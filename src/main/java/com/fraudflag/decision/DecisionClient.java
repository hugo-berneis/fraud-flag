package com.fraudflag.decision;

import com.fraudflag.feature.FeatureSummary;
import java.util.List;

/**
 * Jev's two primitives, as named in the project spec: "noul" answers yes/no with a
 * confidence, "choice" picks among a list of options. Only {@link MockDecisionClient} exists
 * until the real Jev docs and SDK calls are provided -- never invent the real API shape here.
 */
public interface DecisionClient {

	NoulAnswer noul(FeatureSummary features, List<String> ruleHits);

	ChoiceAnswer choice(FeatureSummary features, List<String> options);
}
