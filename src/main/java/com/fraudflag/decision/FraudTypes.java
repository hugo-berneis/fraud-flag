package com.fraudflag.decision;

import java.util.List;

/** The fraud type labels the generator injects, matching tools/generator/generator.py. */
public final class FraudTypes {

	public static final List<String> ALL =
			List.of("card_testing", "account_takeover_geo_jump", "sudden_large_purchase", "velocity_abuse");

	private FraudTypes() {
	}
}
