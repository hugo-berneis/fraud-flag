package com.fraudflag.transaction;

import com.fraudflag.decision.DecisionOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

	private final DecisionOrchestrator decisionOrchestrator;

	public TransactionController(DecisionOrchestrator decisionOrchestrator) {
		this.decisionOrchestrator = decisionOrchestrator;
	}

	@PostMapping("/transactions")
	public Transaction receive(@Valid @RequestBody Transaction transaction) {
		return decisionOrchestrator.ingest(transaction);
	}
}
