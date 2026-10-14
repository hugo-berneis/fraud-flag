package com.fraudflag.decision;

import java.util.List;

/**
 * Everything the case summary writer is allowed to see: computed feature categories and the
 * routing outcome. No raw transaction data (amount, account id, timestamps) ever goes in here.
 */
public record CaseSummaryRequest(
		List<String> featureCategories, List<String> ruleHits, String fraudTypeGuess, double confidence) {
}
