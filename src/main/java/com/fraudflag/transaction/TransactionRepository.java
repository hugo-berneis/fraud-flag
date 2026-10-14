package com.fraudflag.transaction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

	Optional<TransactionEntity> findByTransactionId(String transactionId);

	List<TransactionEntity> findByAccountIdOrderByOccurredAtDesc(String accountId);
}
