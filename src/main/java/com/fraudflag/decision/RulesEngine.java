package com.fraudflag.decision;

import com.fraudflag.feature.FeatureSummary;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Hard rules for the obvious cases, so Jev only has to think about the ambiguous ones.
 * Rules run in order; the first fraud rule to fire wins, but every matching rule name is
 * still recorded in the audit trail.
 */
@Service
public class RulesEngine {

	public RuleResult evaluate(FeatureSummary features) {
		List<String> categories = features.categories();
		List<String> hits = new ArrayList<>();
		RuleVerdict verdict = null;

		if (categories.contains("velocity: extreme")) {
			hits.add("velocity_extreme");
			verdict = new RuleVerdict(Route.HOLD, true, "card_testing");
		}

		if (categories.contains("geo: jump") && categories.contains("device: new")) {
			hits.add("geo_jump_with_new_device");
			if (verdict == null) {
				verdict = new RuleVerdict(Route.HOLD, true, "account_takeover_geo_jump");
			}
		}

		if (categories.contains("amount: extreme")) {
			hits.add("amount_extreme");
			if (verdict == null) {
				verdict = new RuleVerdict(Route.HOLD, true, "sudden_large_purchase");
			}
		}

		if (verdict == null
				&& categories.contains("amount: typical")
				&& categories.contains("velocity: normal")
				&& categories.contains("device: known")
				&& categories.contains("geo: consistent")) {
			hits.add("all_signals_normal");
			verdict = new RuleVerdict(Route.APPROVE, false, null);
		}

		return new RuleResult(hits, verdict);
	}
}
