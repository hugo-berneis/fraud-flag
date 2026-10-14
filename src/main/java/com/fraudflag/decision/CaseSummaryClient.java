package com.fraudflag.decision;

/**
 * Writes a short case summary for a human reviewer on uncertain (REVIEW) decisions. Never makes
 * the fraud decision itself -- that's Jev's and the rules engine's job.
 */
public interface CaseSummaryClient {

	String summarize(CaseSummaryRequest request);
}
