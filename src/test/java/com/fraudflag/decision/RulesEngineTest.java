package com.fraudflag.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudflag.feature.FeatureSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

class RulesEngineTest {

	private final RulesEngine rulesEngine = new RulesEngine();

	@Test
	void extremeVelocityForcesHoldAsCardTesting() {
		FeatureSummary features =
				new FeatureSummary(List.of("amount: typical", "velocity: extreme", "device: known", "geo: consistent"));

		RuleResult result = rulesEngine.evaluate(features);

		assertThat(result.hasVerdict()).isTrue();
		assertThat(result.verdict().route()).isEqualTo(Route.HOLD);
		assertThat(result.verdict().isFraud()).isTrue();
		assertThat(result.verdict().fraudType()).isEqualTo("card_testing");
		assertThat(result.hits()).contains("velocity_extreme");
	}

	@Test
	void geoJumpWithNewDeviceForcesHoldAsAccountTakeover() {
		FeatureSummary features =
				new FeatureSummary(List.of("amount: typical", "velocity: normal", "device: new", "geo: jump"));

		RuleResult result = rulesEngine.evaluate(features);

		assertThat(result.hasVerdict()).isTrue();
		assertThat(result.verdict().route()).isEqualTo(Route.HOLD);
		assertThat(result.verdict().fraudType()).isEqualTo("account_takeover_geo_jump");
	}

	@Test
	void extremeAmountForcesHoldAsSuddenLargePurchase() {
		FeatureSummary features =
				new FeatureSummary(List.of("amount: extreme", "velocity: normal", "device: known", "geo: consistent"));

		RuleResult result = rulesEngine.evaluate(features);

		assertThat(result.hasVerdict()).isTrue();
		assertThat(result.verdict().route()).isEqualTo(Route.HOLD);
		assertThat(result.verdict().fraudType()).isEqualTo("sudden_large_purchase");
	}

	@Test
	void allNormalSignalsForceApprove() {
		FeatureSummary features =
				new FeatureSummary(List.of("amount: typical", "velocity: normal", "device: known", "geo: consistent"));

		RuleResult result = rulesEngine.evaluate(features);

		assertThat(result.hasVerdict()).isTrue();
		assertThat(result.verdict().route()).isEqualTo(Route.APPROVE);
		assertThat(result.verdict().isFraud()).isFalse();
	}

	@Test
	void mixedAmbiguousSignalsDoNotForceAVerdict() {
		FeatureSummary features =
				new FeatureSummary(List.of("amount: unusually high", "velocity: elevated", "device: new", "geo: consistent"));

		RuleResult result = rulesEngine.evaluate(features);

		assertThat(result.hasVerdict()).isFalse();
		assertThat(result.hits()).isEmpty();
	}
}
