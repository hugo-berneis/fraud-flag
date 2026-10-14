package com.fraudflag.review;

import com.fraudflag.decision.DecisionEntity;
import com.fraudflag.decision.HumanVerdict;
import java.time.Instant;
import java.util.List;

/** REST projection of a decision -- never expose the JPA entity directly. */
public record ReviewQueueItem(
		Long decisionId,
		String transactionId,
		List<String> features,
		List<String> ruleHits,
		String fraudType,
		Double confidence,
		String caseSummary,
		HumanVerdict humanVerdict,
		Instant reviewedAt) {

	public static ReviewQueueItem from(DecisionEntity entity) {
		return new ReviewQueueItem(
				entity.getId(),
				entity.getTransactionId(),
				entity.getFeatures(),
				entity.getRuleHits(),
				entity.getFraudType(),
				entity.getConfidence(),
				entity.getCaseSummary(),
				entity.getHumanVerdict(),
				entity.getReviewedAt());
	}
}
