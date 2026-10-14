package com.fraudflag.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

	private final TransactionRepository repository;

	public TransactionService(TransactionRepository repository) {
		this.repository = repository;
	}

	public Transaction save(Transaction transaction) {
		repository.save(toEntity(transaction));
		return transaction;
	}

	/**
	 * Idempotent save for the Kafka consumer: relies on the unique constraint on
	 * transaction_id, so a redelivered message is skipped instead of causing a duplicate row.
	 */
	public boolean saveIfNew(Transaction transaction) {
		try {
			repository.save(toEntity(transaction));
			return true;
		} catch (DataIntegrityViolationException e) {
			log.info("Skipping duplicate transaction {}", transaction.transactionId());
			return false;
		}
	}

	private TransactionEntity toEntity(Transaction transaction) {
		return new TransactionEntity(
				transaction.transactionId(),
				transaction.accountId(),
				transaction.amount(),
				transaction.currency(),
				transaction.merchant(),
				transaction.occurredAt(),
				transaction.deviceId(),
				transaction.geoCountry());
	}
}
