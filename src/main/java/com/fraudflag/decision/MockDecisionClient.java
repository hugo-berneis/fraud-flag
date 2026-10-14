package com.fraudflag.decision;

import com.fraudflag.feature.FeatureSummary;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Stands in for the real Jev client. Scores how many feature categories look suspicious and
 * guesses a fraud type from which categories fired -- deliberately simple, just enough to
 * exercise the routing logic until the real Jev SDK is available.
 */
@Component
public class MockDecisionClient implements DecisionClient {

	private static final List<String> SUSPICIOUS_MARKERS =
			List.of("extreme", "unusually high", "elevated", "new", "jump");

	@Override
	public NoulAnswer noul(FeatureSummary features, List<String> ruleHits) {
		long suspiciousCount = features.categories().stream()
				.filter(category -> SUSPICIOUS_MARKERS.stream().anyMatch(category::contains))
				.count();

		double confidence = Math.min(1.0, suspiciousCount / 4.0 + (ruleHits.isEmpty() ? 0.0 : 0.2));
		return new NoulAnswer(confidence >= 0.5, confidence);
	}

	@Override
	public ChoiceAnswer choice(FeatureSummary features, List<String> options) {
		Set<String> categories = Set.copyOf(features.categories());
		String fraudType;
		if (categories.contains("velocity: extreme")) {
			fraudType = "card_testing";
		} else if (categories.contains("geo: jump")) {
			fraudType = "account_takeover_geo_jump";
		} else if (categories.contains("amount: extreme")) {
			fraudType = "sudden_large_purchase";
		} else if (categories.contains("velocity: elevated")) {
			fraudType = "velocity_abuse";
		} else {
			fraudType = options.isEmpty() ? "unknown" : options.get(0);
		}
		return new ChoiceAnswer(fraudType, 0.6);
	}
}
