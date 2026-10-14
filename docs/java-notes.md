# Java / Spring notes

Concepts as they come up, newest at the bottom.

## Phase 0

- **Record**: `public record Transaction(...)` is a Java class that auto-generates a constructor,
  getters (named after the field, e.g. `amount()` not `getAmount()`), `equals`, `hashCode`, and
  `toString` from the fields you list. Closest Python analogue: a frozen `dataclass`. Closest C#
  analogue: a `record` (C# borrowed the name and the idea).
- **Annotations** (`@RestController`, `@GetMapping`, `@Valid`): metadata attached to a class/method/
  field that a framework reads at runtime (via reflection) or compile time to change behavior.
  Similar to Python decorators (`@app.route(...)` in Flask) but declarative rather than wrapping
  code — Spring reads the annotation and reacts, it doesn't call your code through it.
- **Dependency injection (DI)**: instead of a class constructing its own collaborators (`new Foo()`),
  Spring constructs them and hands them in, usually via the constructor. This project will use
  *constructor injection* everywhere (per the working rules) instead of field injection, because
  constructor injection makes dependencies visible and the class testable without a Spring context.
- **Bean validation** (`@NotBlank`, `@NotNull`, `@Positive` from `jakarta.validation.constraints`):
  declarative input checks. Spring MVC runs these automatically when a `@RequestBody` parameter is
  annotated `@Valid`, and returns HTTP 400 if any constraint fails — no manual `if` checks needed.
- **`@WebMvcTest`**: a Spring Boot test "slice" that boots only the web layer (controllers, JSON
  conversion, validation) with a mocked `MockMvc` client, instead of the whole application context.
  Much faster than `@SpringBootTest` and the right tool when you're only testing a controller.

## Phase 1

- **Flyway migrations**: versioned `.sql` files (`V1__xxx.sql`, `V2__xxx.sql`) in
  `src/main/resources/db/migration`. Flyway runs them in order on startup and records which ones
  ran in a `flyway_schema_history` table, so the schema is defined by SQL files checked into git,
  not by a tool inferring it from your Java classes. We set `spring.jpa.hibernate.ddl-auto: validate`
  so Hibernate checks the schema matches the entities but never changes it itself — Flyway is the
  single source of truth for schema changes.
- **JPA `@Entity` vs. the `Transaction` record**: the record is the API's DTO (what JSON goes in and
  out); `TransactionEntity` is a separate mutable class mapped to the `transactions` table. JPA needs
  a no-arg constructor and mutable fields to build objects via reflection when loading rows, which a
  record can't provide — hence two classes instead of reusing one.
- **`@ServiceConnection` + Testcontainers**: in the integration test, `@Container` starts a real
  throwaway Postgres in Docker, and `@ServiceConnection` automatically points Spring's datasource at
  it — no manual URL/username/password wiring needed. This is "test against the real database,"
  the opposite of mocking the repository.
- **Known gotcha hit this phase**: macOS Docker Desktop's socket moved to `~/.docker/run/docker.sock`
  (not `/var/run/docker.sock`), and the Testcontainers library pinned by the Spring Boot BOM had a bug
  talking to newer Docker Desktop versions. Fixed via `~/.testcontainers.properties` (`docker.host=...`)
  and bumping `testcontainers.version` in `build.gradle.kts`. One-time machine setup, not a code issue.

## Phase 2

- **`@KafkaListener`**: one annotation on a plain method turns it into a Kafka consumer — Spring
  subscribes it to the topic named in the annotation and calls it with each deserialized message.
  No manual poll loop, unlike raw `kafka-python`/`confluent-kafka` clients (which is exactly what
  the Python generator uses on the producer side, since it's a one-shot script, not a long-running
  consumer that benefits from a framework).
  `properties.spring.json.use.type.headers: false` because the producer is Python, so there's no
  Java class to look up — we tell the deserializer which Java type to assume instead.
- **Idempotent consumer via a unique DB constraint**: rather than checking "does this ID exist?"
  before inserting (a race under concurrent consumers), `TransactionService.saveIfNew` just tries
  the insert and catches `DataIntegrityViolationException` if the `transaction_id` unique constraint
  rejects it. The database is the single source of truth for "have I seen this before," not an
  in-memory check.
- **Consumer groups & partitions**: the `transactions.raw` topic has 3 partitions; our one consumer
  instance (group `fraud-flag-service`) gets all 3 assigned. If we ran a second instance of the app
  with the same group id, Kafka would split the partitions between them — that's how Kafka scales
  consumption horizontally, each partition processed by exactly one consumer in the group at a time.
- **Testing a `@KafkaListener` without touching the real broker**: `@EmbeddedKafka` (from
  `spring-kafka-test`) spins up an in-process broker just for the test. Combined with the Testcontainers
  Postgres from Phase 1, `TransactionConsumerIntegrationTest` exercises the real flow — publish JSON,
  consumer picks it up, row lands in Postgres — without needing the docker-compose stack running.

## Phase 3

- **Rules before Jev**: `RulesEngine` runs first on the text categories from `FeatureService`.
  If a rule is confident (e.g. `velocity: extreme` always means card testing), it decides the
  route directly and `DecisionOrchestrator` never calls `DecisionClient` at all — cheaper and
  more explainable than asking a model to re-derive something a simple rule already knows.
- **`DecisionClient` as a seam**: the interface (`noul`/`choice`) and `MockDecisionClient` exist
  so the rest of the pipeline (routing, audit rows, tests) can be built and tested now, with the
  real Jev client swapped in later as a second implementation of the same interface — nothing
  else in the codebase needs to change.
- **`@JdbcTypeCode(SqlTypes.JSON)`**: lets a `List<String>` field map straight to a Postgres
  `jsonb` column — Hibernate handles the (de)serialization, no manual JSON string + converter
  boilerplate needed.
- **Computing features before saving**: `DecisionOrchestrator` calls `FeatureService` *before*
  `TransactionService.save*`, so "this account's history" never accidentally includes the
  transaction currently being decided — a subtle ordering bug that would otherwise make every
  transaction look like it matches its own device/geo/amount.
- **Threshold edges matter**: `RoutingPolicy` uses `>=`/`<=` at `tau_high`/`tau_low`, so a
  confidence of exactly 0.7 holds and exactly 0.3 approves. `RoutingPolicyTest` checks both
  boundaries explicitly instead of only testing "clearly fraud" and "clearly fine" — boundary
  bugs are where off-by-one routing mistakes actually hide.

## Phase 4

- **The LLM never decides, only summarizes**: `CaseSummaryClient.summarize` is only called for
  `REVIEW`-routed transactions, after the route is already decided by rules/Jev. It gets a
  `CaseSummaryRequest` built only from feature categories, rule hits, and confidence — no raw
  transaction data — same "never give it raw numbers" discipline as `DecisionClient`.
  `MockCaseSummaryClient`/`AnthropicCaseSummaryClient` mirror the `DecisionClient`/
  `MockDecisionClient` pattern: one interface, swap implementations based on config, nothing
  else in the codebase changes.
- **Picking the real client without annotation magic**: `CaseSummaryConfig` is a plain `if` in
  a `@Bean` method (empty API key → `MockCaseSummaryClient`, else the real Anthropic one),
  instead of `@ConditionalOnProperty` — that annotation treats an empty string as "present," so
  it would've picked the real client even with a blank key. A plain `if` is also easier to read.
- **Micrometer `Counter` vs `Timer`**: a `Counter` only counts occurrences (`transactionsProcessed`);
  a `Timer` records how long something took and, with `publishPercentileHistogram()`, buckets
  those durations so `/actuator/metrics/fraudflag.decision.latency` can report p50/p95, not just
  an average.
- **MDC for structured logs**: `MDC.putCloseable(key, value)` attaches a field to every log line
  for the current thread until the returned handle is closed (used in a try-with-resources here).
  Combined with `LogstashEncoder` in `logback-spring.xml`, those fields (`txnId`, `route`,
  `confidence`, `latencyMs`) become top-level JSON keys instead of buried in a text message —
  what Project 4 will actually grep/query.
- **DTOs instead of exposing the entity**: `ReviewController` returns `ReviewQueueItem`, a
  `record` built from `DecisionEntity`, rather than the entity itself — keeps the JPA mapping
  free to change without breaking the API, and avoids ever serializing a field nobody asked for.
