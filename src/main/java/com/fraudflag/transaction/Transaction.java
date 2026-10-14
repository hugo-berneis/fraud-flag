package com.fraudflag.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
		@NotBlank String transactionId,
		@NotBlank String accountId,
		@NotNull @Positive BigDecimal amount,
		@NotBlank String currency,
		@NotBlank String merchant,
		@NotNull Instant occurredAt,
		String deviceId,
		String geoCountry) {
}
