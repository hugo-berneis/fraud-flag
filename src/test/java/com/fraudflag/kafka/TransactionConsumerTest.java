package com.fraudflag.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudflag.decision.DecisionOrchestrator;
import com.fraudflag.transaction.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

	@Mock
	private DecisionOrchestrator decisionOrchestrator;

	@Test
	void consumeDelegatesToDecisionOrchestratorIngestIfNew() {
		TransactionConsumer consumer = new TransactionConsumer(decisionOrchestrator);
		Transaction transaction = new Transaction(
				"txn-1", "acct-1", new BigDecimal("12.50"), "USD", "Coffee Shop", Instant.now(), "device-1", "US");
		when(decisionOrchestrator.ingestIfNew(any())).thenReturn(true);

		consumer.consume(transaction);

		verify(decisionOrchestrator).ingestIfNew(transaction);
	}
}
