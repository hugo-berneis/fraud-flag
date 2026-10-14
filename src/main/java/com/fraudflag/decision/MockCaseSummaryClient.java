package com.fraudflag.decision;

/** Template-based stand-in for the real LLM client -- no API calls, no cost. */
public class MockCaseSummaryClient implements CaseSummaryClient {

	@Override
	public String summarize(CaseSummaryRequest request) {
		String hits = request.ruleHits().isEmpty() ? "no rule fired" : String.join(", ", request.ruleHits());
		return String.format(
				"Uncertain case (confidence %.2f): %s. Rule hits: %s. Best guess type: %s.",
				request.confidence(),
				String.join(", ", request.featureCategories()),
				hits,
				request.fraudTypeGuess() == null ? "unknown" : request.fraudTypeGuess());
	}
}
