package com.fraudflag.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransactionValidationTest {

	private final Validator validator;

	TransactionValidationTest() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();
	}

	@Test
	void validTransactionHasNoViolations() {
		Transaction transaction = new Transaction(
				"txn-1", "acct-1", new BigDecimal("12.50"), "USD", "Coffee Shop", Instant.now(), "device-1", "US");

		Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

		assertThat(violations).isEmpty();
	}

	@Test
	void blankMerchantIsRejected() {
		Transaction transaction = new Transaction(
				"txn-1", "acct-1", new BigDecimal("12.50"), "USD", "", Instant.now(), "device-1", "US");

		Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

		assertThat(violations).isNotEmpty();
	}

	@Test
	void negativeAmountIsRejected() {
		Transaction transaction = new Transaction(
				"txn-1", "acct-1", new BigDecimal("-5.00"), "USD", "Coffee Shop", Instant.now(), "device-1", "US");

		Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

		assertThat(violations).isNotEmpty();
	}
}
