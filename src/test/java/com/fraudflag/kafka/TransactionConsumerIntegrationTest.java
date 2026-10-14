package com.fraudflag.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudflag.decision.DecisionRepository;
import com.fraudflag.transaction.TransactionEntity;
import com.fraudflag.transaction.TransactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Publishes a raw JSON message directly to an embedded broker, the same way the Python
 * generator will (plain JSON bytes, no Java type headers), and checks the real
 * {@link TransactionConsumer} picks it up, persists it, and writes exactly one decision row --
 * including on a redelivered (duplicate) message.
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {KafkaTopicConfig.TRANSACTIONS_RAW_TOPIC})
class TransactionConsumerIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@Autowired
	private TransactionRepository repository;

	@Autowired
	private DecisionRepository decisionRepository;

	@Test
	void publishedTransactionIsConsumedPersistedAndDecidedExactlyOnce() throws Exception {
		String transactionId = "txn-kafka-1";
		String json = transactionJson(transactionId);

		Properties producerProps = new Properties();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
			producer.send(new ProducerRecord<>(KafkaTopicConfig.TRANSACTIONS_RAW_TOPIC, transactionId, json))
					.get();

			Optional<TransactionEntity> found = awaitTransaction(transactionId);
			assertThat(found).isPresent();
			assertThat(found.get().getMerchant()).isEqualTo("Gas Station");
			assertThat(decisionRepository.findByTransactionId(transactionId)).hasSize(1);

			// Redeliver the same message (same transaction id) -- must not create a second decision row.
			producer.send(new ProducerRecord<>(KafkaTopicConfig.TRANSACTIONS_RAW_TOPIC, transactionId, json))
					.get();
		}

		Thread.sleep(1000);
		List<TransactionEntity> rows = repository.findByAccountIdOrderByOccurredAtDesc("acct-1");
		assertThat(rows).hasSize(1);
		assertThat(decisionRepository.findByTransactionId(transactionId)).hasSize(1);
	}

	private Optional<TransactionEntity> awaitTransaction(String transactionId) throws InterruptedException {
		Optional<TransactionEntity> found = Optional.empty();
		for (int i = 0; i < 40 && found.isEmpty(); i++) {
			Thread.sleep(250);
			found = repository.findByTransactionId(transactionId);
		}
		return found;
	}

	private String transactionJson(String transactionId) {
		return """
				{
				  "transactionId": "%s",
				  "accountId": "acct-1",
				  "amount": 15.00,
				  "currency": "USD",
				  "merchant": "Gas Station",
				  "occurredAt": "2026-01-01T10:00:00Z",
				  "deviceId": "device-1",
				  "geoCountry": "US"
				}
				"""
				.formatted(transactionId);
	}
}
