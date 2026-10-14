package com.fraudflag.decision;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Defaults from application.yml, used when Project 2's thresholds.json isn't present. */
@ConfigurationProperties(prefix = "fraudflag.thresholds")
public record ThresholdsProperties(double tauLow, double tauHigh, String file) {
}
