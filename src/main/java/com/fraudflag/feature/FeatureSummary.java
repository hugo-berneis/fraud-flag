package com.fraudflag.feature;

import java.util.List;

/**
 * The text categories Jev is allowed to see. Never raw numbers -- the whole point of
 * computing features in code is that the amount, timestamps, etc. never leave this service.
 */
public record FeatureSummary(List<String> categories) {
}
