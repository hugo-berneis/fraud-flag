package com.fraudflag.review;

import com.fraudflag.decision.DecisionEntity;
import com.fraudflag.decision.DecisionRepository;
import com.fraudflag.decision.HumanVerdict;
import com.fraudflag.decision.Route;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

	private final DecisionRepository decisionRepository;

	public ReviewService(DecisionRepository decisionRepository) {
		this.decisionRepository = decisionRepository;
	}

	public List<DecisionEntity> pendingReviews() {
		return decisionRepository.findByRouteAndHumanVerdictIsNull(Route.REVIEW.name());
	}

	public DecisionEntity recordVerdict(Long decisionId, HumanVerdict verdict) {
		DecisionEntity decision = decisionRepository
				.findById(decisionId)
				.orElseThrow(() -> new NoSuchElementException("No decision with id " + decisionId));
		decision.recordHumanVerdict(verdict);
		return decisionRepository.save(decision);
	}
}
