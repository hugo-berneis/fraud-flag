package com.fraudflag.review;

import com.fraudflag.decision.HumanVerdict;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull HumanVerdict verdict) {
}
