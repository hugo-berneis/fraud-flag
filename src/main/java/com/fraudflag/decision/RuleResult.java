package com.fraudflag.decision;

import java.util.List;

/**
 * Every rule name that matched (for the audit trail), plus a verdict if any rule was
 * confident enough to decide the route on its own without calling Jev.
 */
public record RuleResult(List<String> hits, RuleVerdict verdict) {

	public boolean hasVerdict() {
		return verdict != null;
	}
}
