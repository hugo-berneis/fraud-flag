package com.fraudflag.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudflag.decision.DecisionEntity;
import com.fraudflag.decision.DecisionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@Testcontainers
class TransactionPersistenceIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TransactionRepository repository;

	@Autowired
	private DecisionRepository decisionRepository;

	@Test
	void postedTransactionIsPersistedInPostgres() {
		Transaction transaction = new Transaction(
				"txn-integration-1",
				"acct-1",
				new BigDecimal("42.00"),
				"USD",
				"Bookstore",
				Instant.parse("2026-01-01T10:00:00Z"),
				"device-1",
				"US");

		ResponseEntity<Transaction> response = restTemplate.postForEntity("/transactions", transaction, Transaction.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		Optional<TransactionEntity> saved = repository.findByTransactionId("txn-integration-1");
		assertThat(saved).isPresent();
		assertThat(saved.get().getMerchant()).isEqualTo("Bookstore");
		assertThat(saved.get().getAmount()).isEqualByComparingTo("42.00");

		List<DecisionEntity> decisions = decisionRepository.findByTransactionId("txn-integration-1");
		assertThat(decisions).hasSize(1);
	}
}
