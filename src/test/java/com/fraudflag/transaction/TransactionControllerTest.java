package com.fraudflag.transaction;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fraudflag.decision.DecisionOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DecisionOrchestrator decisionOrchestrator;

	@Test
	void postingValidTransactionEchoesItBack() throws Exception {
		String body = """
				{
				  "transactionId": "txn-1",
				  "accountId": "acct-1",
				  "amount": 12.50,
				  "currency": "USD",
				  "merchant": "Coffee Shop",
				  "occurredAt": "2026-01-01T10:00:00Z"
				}
				""";

		when(decisionOrchestrator.ingest(any())).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transactionId", is("txn-1")))
				.andExpect(jsonPath("$.merchant", is("Coffee Shop")));
	}

	@Test
	void postingBlankMerchantIsRejected() throws Exception {
		String body = """
				{
				  "transactionId": "txn-1",
				  "accountId": "acct-1",
				  "amount": 12.50,
				  "currency": "USD",
				  "merchant": "",
				  "occurredAt": "2026-01-01T10:00:00Z"
				}
				""";

		mockMvc.perform(post("/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}
}
