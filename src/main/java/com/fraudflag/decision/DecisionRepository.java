package com.fraudflag.decision;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<DecisionEntity, Long> {

	List<DecisionEntity> findByTransactionId(String transactionId);

	List<DecisionEntity> findByRouteAndHumanVerdictIsNull(String route);
}
