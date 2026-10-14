package com.fraudflag.review;

import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/review-queue")
	public List<ReviewQueueItem> reviewQueue() {
		return reviewService.pendingReviews().stream().map(ReviewQueueItem::from).toList();
	}

	@PostMapping("/review/{id}")
	public ReviewQueueItem review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
		return ReviewQueueItem.from(reviewService.recordVerdict(id, request.verdict()));
	}

	@ExceptionHandler(NoSuchElementException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public void handleNotFound() {
	}
}
