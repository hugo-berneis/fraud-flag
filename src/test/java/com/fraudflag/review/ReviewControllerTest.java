package com.fraudflag.review;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fraudflag.decision.DecisionEntity;
import com.fraudflag.decision.HumanVerdict;
import com.fraudflag.decision.Route;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ReviewService reviewService;

	@Test
	void reviewQueueListsPendingDecisions() throws Exception {
		DecisionEntity decision = new DecisionEntity(
				"txn-1", List.of("amount: typical"), List.of(), true, "velocity_abuse", 0.5, Route.REVIEW, 12, "uncertain case");
		when(reviewService.pendingReviews()).thenReturn(List.of(decision));

		mockMvc.perform(get("/review-queue"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].transactionId", is("txn-1")));
	}

	@Test
	void postingVerdictRecordsIt() throws Exception {
		DecisionEntity decision = new DecisionEntity(
				"txn-1", List.of("amount: typical"), List.of(), true, "velocity_abuse", 0.5, Route.REVIEW, 12, "uncertain case");
		decision.recordHumanVerdict(HumanVerdict.APPROVED);
		when(reviewService.recordVerdict(1L, HumanVerdict.APPROVED)).thenReturn(decision);

		mockMvc.perform(post("/review/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"verdict\":\"APPROVED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.humanVerdict", is("APPROVED")));

		verify(reviewService).recordVerdict(1L, HumanVerdict.APPROVED);
	}

	@Test
	void reviewingUnknownDecisionIsNotFound() throws Exception {
		when(reviewService.recordVerdict(anyLong(), org.mockito.ArgumentMatchers.any()))
				.thenThrow(new NoSuchElementException("not found"));

		mockMvc.perform(post("/review/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"verdict\":\"APPROVED\"}"))
				.andExpect(status().isNotFound());
	}
}
