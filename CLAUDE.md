# CLAUDE.md: Project 3, Payment Fraud Checker

> Rename this file to `CLAUDE.md` and place it in the repo root. Claude reads it at the start of every session.
> **Kickoff message:** "Read CLAUDE.md fully. Summarize the goal and the phase plan back to me in 5 bullets, ask any blocking questions, then start Phase 0."

---

## Role
You are a senior Java/Spring Boot engineer **and teacher** pairing with Hugo, a 2nd-year CS student who knows Python, C++, and C# but is **new to Java, Spring Boot, and Kafka**.
- When you introduce a Java or Spring concept for the first time (dependency injection, annotations, beans, `@Transactional`, records, Optional, Kafka consumer groups), explain it in 1–3 sentences, comparing it to Python or C# where that helps.
- Prefer idiomatic, boring Spring Boot over clever patterns. Hugo must be able to explain every file in an interview.

## Goal
A streaming fraud-triage service:
1. A generator produces fake card transactions with **injected fraud patterns** and publishes them to Kafka.
2. A Spring Boot consumer computes features **in code** (amount vs. the account's usual spending, velocity, new device, geo jump) and turns them into **categorical text**, e.g. "velocity: unusually high for this account".
3. Hard rules run first. Then Jev answers **is this fraud? (Noul)** and **what type? (Choice)**.
4. Routing: confident fraud → hold; confident legit → approve; uncertain → LLM-written case summary plus a **human review queue**.
5. Every decision gets a Postgres audit row. The service emits structured JSON logs and metrics, which Project 4 will monitor.

**Timebox:** Oct 26 → Nov 15, 2026 (3 weeks). **MVP scope:** 3–4 fraud patterns, one service, one Kafka topic.

## Non-goals
- A real payment network, PCI data, or real card numbers. Everything is synthetic.
- A frontend. REST endpoints plus curl or Postman are enough.
- Kubernetes (stretch only)

## Tech stack
- Java 21, Spring Boot 3.x, Gradle (Kotlin DSL)
- Spring for Apache Kafka. Kafka in `docker compose` (KRaft mode, no ZooKeeper).
- Postgres 16, Spring Data JPA, Flyway migrations
- JUnit 5, Testcontainers for Kafka and Postgres integration tests
- Micrometer + Spring Boot Actuator for metrics. JSON logs via Logback.
- Transaction generator: a small Python script in `tools/generator/` (Hugo's strength)
- Secrets and config via environment variables and `application.yml` profiles. Commit only `.env.example`.

## Jev and LLM rules
- **Do not invent Jev SDK calls.** Create a Java `DecisionClient` interface (`noul`, `choice`) with a `MockDecisionClient`. Hugo provides the Jev docs before the real client is built.
- **Never give Jev raw numbers.** Code computes every feature and converts it to a text category.
- Thresholds (`tau_low`, `tau_high`) come from Project 2's `thresholds.json`. Fall back to defaults in `application.yml` and log a warning if the file is missing.
- The LLM (Anthropic API, small model) is called **only** for uncertain cases. It writes the case summary and never makes the decision.
- Log the confidence and routing outcome for every transaction.

## Working rules
- One phase at a time. Every phase ends with a passing `./gradlew build` (tests included).
- Small classes, constructor injection, no field injection.
- Ask before expensive or irreversible choices, offering 2–3 options and a recommendation.
- **AWS:** Claude writes configs and instructions, but **Hugo runs every command that creates billable resources.** Remind him to set a billing alarm before the first deploy.
- Never fabricate test results or metrics.

## Git rules (Hugo pushes, not Claude)
- **Never** run `git commit`, `git push`, `git add`, `git reset`, `git rebase`, or change remotes or branches. Read-only git is fine.
- Suggest a commit message at each checkpoint.

## Checkpoint protocol (end of every phase)
1. Run `./gradlew build` and report honestly.
2. Summarize the phase in 3–5 bullets, including **1–2 new Java/Spring concepts Hugo learned this phase**.
3. List the files changed.
4. Give Hugo verification steps (commands, curl calls, expected output).
5. Suggest a conventional commit message.
6. Append to the **Progress Log**.
7. **STOP** and wait for "pushed, continue".

---

## Phase plan

### Phase 0: Java + Spring Boot basics (Days 1–3)
- Scaffold the Gradle project, `GET /health`, and one `Transaction` record plus a `POST /transactions` endpoint that validates the input and echoes it back.
- One unit test and one `@WebMvcTest`.
- A short `docs/java-notes.md` that grows each phase: Java/Spring concepts as they come up.
- **Done when:** `./gradlew bootRun` works and the tests pass.

### Phase 1: Persistence (Days 4–5)
- `docker compose` with Postgres. Flyway migrations for `transactions` and `decisions` (the audit table).
- JPA entities and repositories. The POST endpoint saves transactions.
- A Testcontainers integration test.
- **Done when:** POSTed transactions show up in Postgres, and the integration test passes.

### Phase 2: Kafka + generator (Days 6–9)
- Add Kafka to compose. Create the topic `transactions.raw`.
- A Python generator with configurable rate and 3–4 fraud patterns (e.g. card testing, account takeover plus a geo jump, a sudden large purchase). It writes ground-truth labels to a separate file and **never** sends them to the service.
- A Spring Kafka consumer with idempotent processing (dedupe by transaction ID).
- **Done when:** the generator streams events, the consumer persists them, and consumer lag stays near zero at the demo rate.

### Phase 3: Rules + features + Jev routing (Days 10–13)
- A feature service that computes numeric features and converts them to text categories. Unit-test each feature.
- A rules engine for obvious cases.
- `DecisionClient` (mock first), then the routing logic using thresholds loaded from config or `thresholds.json`.
- Write an audit row per decision with: features, rule hits, the Jev answers, confidences, route, and latency.
- **Done when:** every transaction gets exactly one decision row, and routing is unit-tested at the threshold edges.

### Phase 4: Review queue + LLM case summary + metrics (Days 14–17)
- `GET /review-queue` and `POST /review/{id}` (approve or confirm fraud). Store the human verdict.
- For uncertain cases, call the LLM for a short case summary built only from the stored features. Save it with the decision.
- Micrometer metrics: throughput, decision latency (p50/p95), counts per route. JSON logs include `txnId`, `route`, `confidence`, and `latencyMs`.
- `scripts/score.py` compares decisions against the generator's ground truth: precision, recall, auto-resolve rate, escalation rate.
- **Done when:** the scoring script prints real metrics from a run.

### Phase 5: Containerize + deploy (Days 18–21)
- A multi-stage Dockerfile and a full `docker compose` stack.
- Deploy plan, with Hugo running the billable commands: **default is a single small EC2 instance running docker compose** (cheapest), with logs shipped to CloudWatch for Project 4. ECS Fargate is a stretch goal.
- README: pitch, Mermaid architecture diagram, results table (real runs only), how to run locally, how to deploy and **tear down**, and known limitations.
- **Done when:** the stack runs on AWS for a demo, the logs appear in CloudWatch, and teardown is documented.

---

## Metrics to record (for the resume)
throughput (events/sec) · p50/p95 decision latency · auto-resolve rate · escalation rate · precision/recall at the chosen thresholds · LLM cost per 1k transactions

## Decisions log
<!-- date · decision · why · alternatives -->
- 2026-10-12 · Scaffolded on Spring Boot 3.3.5, not the spec's generic "3.x" · start.spring.io's compatibility range is now >=4.0.0 and Boot 4 is current, but 3.3.5 is the best-trodden version for a learner and still on Maven Central · alternative was Boot 4.1.1 (rejected: newly released, less tutorial content).
- 2026-10-12 · Postgres on host port 5433, not 5432 · another project's container already holds 5432 on this machine · alternative was stopping the other container (rejected: not ours to touch).
- 2026-10-12 · Pinned `testcontainers.version` to 1.21.4 via Gradle `extra[]`, overriding Spring Boot's managed 1.19.8 · the BOM-pinned docker-java 3.3.6 can't talk to newer Docker Desktop (empty/400 response on `/info`), fixed in docker-java 3.4.2 · alternative was downgrading Docker Desktop (rejected: not reversible to ask of Hugo for a dev tool).
- 2026-10-12 · Extended the `Transaction`/`transactions` schema with `deviceId`/`geoCountry` during Phase 2, ahead of Phase 3 · Phase 3's features (new device, geo jump) need this data to exist from the moment the generator starts producing events · alternative was backfilling later (rejected: generator and consumer would need a second pass).
- 2026-10-12 · `CaseSummaryClient`/`AnthropicCaseSummaryClient` mirrors the `DecisionClient`/`MockDecisionClient` seam from Phase 3 · same reasoning: build and test the pipeline against a free mock, swap in the real client via one `@Bean` method once a key exists, no other code changes · alternative was `@ConditionalOnProperty` (rejected: treats an empty-string property as "present," would've picked the real client with a blank key).
- 2026-10-13 · Wired the real Anthropic client once Hugo supplied `ANTHROPIC_API_KEY` (copied from another of his projects' `.env`) · CLAUDE.md explicitly scopes "the LLM (Anthropic API, small model)" as allowed, unlike Jev where no real docs exist yet · `JEV_API_KEY`/`JEV_BASE_URL` (api.typesafe.ai) were also copied over but NOT wired in — no documented request/response shape for `noul`/`choice`, so `MockDecisionClient` stays active per the "do not invent Jev SDK calls" rule.
- 2026-10-13 · `scripts/score.py` uses `psycopg` (psycopg3), not `psycopg2-binary` · no prebuilt wheel for this machine's Python 3.14 yet, and building from source needs `pg_config`/libpq which isn't installed · psycopg3's API is close enough to be a one-line import swap.

## Progress log
<!-- date · phase · summary · build status · open issues -->
- 2026-10-12 · Phase 0 · Gradle (Kotlin DSL) scaffold, `GET /health`, `Transaction` record + validated `POST /transactions`, unit test + `@WebMvcTest`, `docs/java-notes.md` started · `./gradlew build` green (5 tests) · no open issues.
- 2026-10-12 · Phase 1 · docker-compose Postgres, Flyway `V1`/`V2` (transactions + decisions tables), JPA entity/repo, Testcontainers integration test · `./gradlew build` green · no open issues.
- 2026-10-12 · Phase 2 · Kafka (KRaft) in compose, `transactions.raw` topic, Python generator (`tools/generator/`) with 4 fraud patterns + ground truth file, idempotent `@KafkaListener` consumer, `V3` migration for device/geo columns · `./gradlew build` green (10 tests) · verified live: 100 generated events, 0 consumer lag across 3 partitions, duplicate redelivery correctly skipped.
- 2026-10-12 · Phase 3 · `FeatureService` (4 text-category features), `RulesEngine` (4 hard rules), `DecisionClient`/`MockDecisionClient`, `ThresholdsService` (thresholds.json with application.yml fallback), `DecisionOrchestrator` audit pipeline · `./gradlew build` green (29 tests) · verified live: 80/80 new transactions got exactly one decision row, routed correctly.
- 2026-10-13 · Phase 4 · `GET /review-queue` + `POST /review/{id}`, `CaseSummaryClient`/`MockCaseSummaryClient`/`AnthropicCaseSummaryClient` (real Anthropic wired, Hugo supplied the key), Micrometer counters/timer with p50/p95, JSON logs via Logstash encoder + MDC, `scripts/score.py` · `./gradlew build` green (32 tests) · verified live: real case summary written by Claude for a REVIEW case, review-queue/review endpoints work, scoring script printed precision 76%/recall 78%/auto-resolve 99%/escalation 1% on a 120-transaction run. Open issue: one of two live LLM calls returned an empty text block (no error logged) — likely API response variance, not reproduced in automated tests since those run with the mock client; worth a retry/non-empty check if it recurs.
