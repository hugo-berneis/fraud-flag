package com.fraudflag.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private TransactionRepository repository;

	@Test
	void saveIfNewPersistsAFreshTransaction() {
		TransactionService service = new TransactionService(repository);
		Transaction transaction = sampleTransaction();

		boolean saved = service.saveIfNew(transaction);

		assertThat(saved).isTrue();
		verify(repository).save(any());
		verifyNoMoreInteractions(repository);
	}

	@Test
	void saveIfNewSkipsADuplicateTransaction() {
		TransactionService service = new TransactionService(repository);
		Transaction transaction = sampleTransaction();
		doThrow(new DataIntegrityViolationException("duplicate")).when(repository).save(any());

		boolean saved = service.saveIfNew(transaction);

		assertThat(saved).isFalse();
	}

	private Transaction sampleTransaction() {
		return new Transaction(
				"txn-1", "acct-1", new BigDecimal("12.50"), "USD", "Coffee Shop", Instant.now(), "device-1", "US");
	}
}
