package com.fraudflag.feature;

import com.fraudflag.transaction.Transaction;
import com.fraudflag.transaction.TransactionEntity;
import com.fraudflag.transaction.TransactionRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Turns a transaction plus the account's prior history into the text categories Jev sees.
 * Nothing here ever returns a raw amount, timestamp, or count -- only categorical descriptions.
 */
@Service
public class FeatureService {

	private static final Duration VELOCITY_WINDOW = Duration.ofSeconds(60);

	private final TransactionRepository repository;

	public FeatureService(TransactionRepository repository) {
		this.repository = repository;
	}

	public FeatureSummary computeFeatures(Transaction transaction) {
		List<TransactionEntity> history = repository.findByAccountIdOrderByOccurredAtDesc(transaction.accountId());

		return new FeatureSummary(List.of(
				amountCategory(transaction, history),
				velocityCategory(transaction, history),
				deviceCategory(transaction, history),
				geoCategory(transaction, history)));
	}

	private String amountCategory(Transaction transaction, List<TransactionEntity> history) {
		if (history.isEmpty()) {
			return "amount: typical";
		}

		double average = history.stream()
				.mapToDouble(h -> h.getAmount().doubleValue())
				.average()
				.orElse(transaction.amount().doubleValue());
		double ratio = transaction.amount().doubleValue() / Math.max(average, 0.01);

		if (ratio >= 10) {
			return "amount: extreme";
		}
		if (ratio >= 3) {
			return "amount: unusually high";
		}
		return "amount: typical";
	}

	private String velocityCategory(Transaction transaction, List<TransactionEntity> history) {
		long recentCount = history.stream()
				.filter(h -> !h.getOccurredAt().isBefore(transaction.occurredAt().minus(VELOCITY_WINDOW)))
				.filter(h -> h.getOccurredAt().isBefore(transaction.occurredAt()))
				.count();

		if (recentCount >= 5) {
			return "velocity: extreme";
		}
		if (recentCount >= 2) {
			return "velocity: elevated";
		}
		return "velocity: normal";
	}

	private String deviceCategory(Transaction transaction, List<TransactionEntity> history) {
		if (transaction.deviceId() == null) {
			return "device: unknown";
		}

		boolean seenBefore = history.stream().anyMatch(h -> transaction.deviceId().equals(h.getDeviceId()));
		return seenBefore ? "device: known" : "device: new";
	}

	private String geoCategory(Transaction transaction, List<TransactionEntity> history) {
		if (history.isEmpty()) {
			return "geo: first transaction";
		}
		if (transaction.geoCountry() == null) {
			return "geo: unknown";
		}

		String mostCommonCountry = history.stream()
				.filter(h -> h.getGeoCountry() != null)
				.collect(Collectors.groupingBy(TransactionEntity::getGeoCountry, Collectors.counting()))
				.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);

		if (mostCommonCountry == null || mostCommonCountry.equals(transaction.geoCountry())) {
			return "geo: consistent";
		}
		return "geo: jump";
	}
}
