package com.fraudflag.decision;

/**
 * A hard rule's final answer: when a rule fires with enough confidence to skip Jev entirely.
 */
public record RuleVerdict(Route route, boolean isFraud, String fraudType) {
}
