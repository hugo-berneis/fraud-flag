package com.fraudflag.kafka;

import com.fraudflag.decision.DecisionOrchestrator;
import com.fraudflag.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

	private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

	private final DecisionOrchestrator decisionOrchestrator;

	public TransactionConsumer(DecisionOrchestrator decisionOrchestrator) {
		this.decisionOrchestrator = decisionOrchestrator;
	}

	@KafkaListener(topics = KafkaTopicConfig.TRANSACTIONS_RAW_TOPIC, groupId = "fraud-flag-service")
	public void consume(Transaction transaction) {
		boolean saved = decisionOrchestrator.ingestIfNew(transaction);
		if (saved) {
			log.info("Persisted transaction {}", transaction.transactionId());
		}
	}
}
