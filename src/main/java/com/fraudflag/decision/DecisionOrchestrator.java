package com.fraudflag.decision;

import com.fraudflag.feature.FeatureService;
import com.fraudflag.feature.FeatureSummary;
import com.fraudflag.transaction.Transaction;
import com.fraudflag.transaction.TransactionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * The pipeline every transaction goes through, regardless of whether it arrived over REST or
 * Kafka: compute features from history, run the hard rules, fall back to Jev if no rule fired,
 * write a case summary for uncertain cases, then write exactly one audit row. Features are
 * always computed before the transaction is saved, so "history" never includes the transaction
 * currently being decided.
 */
@Service
public class DecisionOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(DecisionOrchestrator.class);

	private final TransactionService transactionService;
	private final FeatureService featureService;
	private final RulesEngine rulesEngine;
	private final DecisionClient decisionClient;
	private final CaseSummaryClient caseSummaryClient;
	private final DecisionRepository decisionRepository;
	private final RoutingPolicy routingPolicy;
	private final MeterRegistry meterRegistry;
	private final Counter transactionsProcessed;
	private final Timer decisionLatency;

	public DecisionOrchestrator(
			TransactionService transactionService,
			FeatureService featureService,
			RulesEngine rulesEngine,
			DecisionClient decisionClient,
			CaseSummaryClient caseSummaryClient,
			DecisionRepository decisionRepository,
			RoutingPolicy routingPolicy,
			MeterRegistry meterRegistry) {
		this.transactionService = transactionService;
		this.featureService = featureService;
		this.rulesEngine = rulesEngine;
		this.decisionClient = decisionClient;
		this.caseSummaryClient = caseSummaryClient;
		this.decisionRepository = decisionRepository;
		this.routingPolicy = routingPolicy;
		this.meterRegistry = meterRegistry;
		this.transactionsProcessed = Counter.builder("fraudflag.transactions.processed")
				.description("Transactions that went through the decision pipeline")
				.register(meterRegistry);
		this.decisionLatency = Timer.builder("fraudflag.decision.latency")
				.description("Time to compute features, run rules/Jev, and write the audit row")
				.publishPercentileHistogram()
				.register(meterRegistry);
	}

	/** REST path: always saves (throws on duplicate transaction id, same as before Phase 3). */
	public Transaction ingest(Transaction transaction) {
		FeatureSummary features = featureService.computeFeatures(transaction);
		Transaction saved = transactionService.save(transaction);
		decide(transaction, features);
		return saved;
	}

	/** Kafka path: idempotent. Returns false (and skips deciding) for a redelivered transaction id. */
	public boolean ingestIfNew(Transaction transaction) {
		FeatureSummary features = featureService.computeFeatures(transaction);
		boolean isNew = transactionService.saveIfNew(transaction);
		if (isNew) {
			decide(transaction, features);
		}
		return isNew;
	}

	private void decide(Transaction transaction, FeatureSummary features) {
		long startNanos = System.nanoTime();
		RuleResult ruleResult = rulesEngine.evaluate(features);

		Boolean isFraud;
		String fraudType;
		Double confidence;
		Route route;

		if (ruleResult.hasVerdict()) {
			RuleVerdict verdict = ruleResult.verdict();
			isFraud = verdict.isFraud();
			fraudType = verdict.fraudType();
			confidence = verdict.isFraud() ? 1.0 : 0.0;
			route = verdict.route();
		} else {
			NoulAnswer noul = decisionClient.noul(features, ruleResult.hits());
			isFraud = noul.isFraud();
			confidence = noul.confidence();
			route = routingPolicy.decide(confidence);
			fraudType = isFraud ? decisionClient.choice(features, FraudTypes.ALL).fraudType() : null;
		}

		String caseSummary = route == Route.REVIEW
				? caseSummaryClient.summarize(new CaseSummaryRequest(features.categories(), ruleResult.hits(), fraudType, confidence))
				: null;

		long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

		decisionRepository.save(new DecisionEntity(
				transaction.transactionId(),
				features.categories(),
				ruleResult.hits(),
				isFraud,
				fraudType,
				confidence,
				route,
				latencyMs,
				caseSummary));

		transactionsProcessed.increment();
		decisionLatency.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
		meterRegistry.counter("fraudflag.decisions.by.route", "route", route.name()).increment();

		logDecision(transaction.transactionId(), route, isFraud, confidence, latencyMs);
	}

	private void logDecision(String transactionId, Route route, Boolean isFraud, Double confidence, long latencyMs) {
		try (var txnIdScope = MDC.putCloseable("txnId", transactionId);
				var routeScope = MDC.putCloseable("route", route.name());
				var confidenceScope = MDC.putCloseable("confidence", String.valueOf(confidence));
				var latencyScope = MDC.putCloseable("latencyMs", String.valueOf(latencyMs))) {
			log.info("decision recorded isFraud={}", isFraud);
		}
	}
}
