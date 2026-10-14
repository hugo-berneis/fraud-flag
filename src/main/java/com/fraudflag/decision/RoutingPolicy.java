package com.fraudflag.decision;

import org.springframework.stereotype.Component;

/** Turns a Jev confidence score into a route, using tau_low/tau_high from {@link ThresholdsService}. */
@Component
public class RoutingPolicy {

	private final ThresholdsService thresholdsService;

	public RoutingPolicy(ThresholdsService thresholdsService) {
		this.thresholdsService = thresholdsService;
	}

	public Route decide(double confidence) {
		if (confidence >= thresholdsService.tauHigh()) {
			return Route.HOLD;
		}
		if (confidence <= thresholdsService.tauLow()) {
			return Route.APPROVE;
		}
		return Route.REVIEW;
	}
}
