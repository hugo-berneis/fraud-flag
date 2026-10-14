package com.fraudflag.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RoutingPolicyTest {

	// Points at a file that doesn't exist, so ThresholdsService always falls back to these
	// defaults -- keeping this test deterministic regardless of what's on disk.
	private final ThresholdsService thresholdsService =
			new ThresholdsService(new ThresholdsProperties(0.3, 0.7, "does-not-exist.json"), new ObjectMapper());

	private final RoutingPolicy routingPolicy = new RoutingPolicy(thresholdsService);

	@Test
	void belowTauLowApproves() {
		assertThat(routingPolicy.decide(0.1)).isEqualTo(Route.APPROVE);
	}

	@Test
	void exactlyAtTauLowApproves() {
		assertThat(routingPolicy.decide(0.3)).isEqualTo(Route.APPROVE);
	}

	@Test
	void justAboveTauLowReviews() {
		assertThat(routingPolicy.decide(0.31)).isEqualTo(Route.REVIEW);
	}

	@Test
	void justBelowTauHighReviews() {
		assertThat(routingPolicy.decide(0.69)).isEqualTo(Route.REVIEW);
	}

	@Test
	void exactlyAtTauHighHolds() {
		assertThat(routingPolicy.decide(0.7)).isEqualTo(Route.HOLD);
	}

	@Test
	void aboveTauHighHolds() {
		assertThat(routingPolicy.decide(0.9)).isEqualTo(Route.HOLD);
	}
}
