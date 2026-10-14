package com.fraudflag.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fraudflag.transaction.Transaction;
import com.fraudflag.transaction.TransactionEntity;
import com.fraudflag.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

	@Mock
	private TransactionRepository repository;

	@Test
	void firstTransactionForAccountGetsNeutralCategories() {
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(List.of());
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "50.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "US"));

		assertThat(features.categories())
				.containsExactly("amount: typical", "velocity: normal", "device: new", "geo: first transaction");
	}

	@Test
	void amountFarAboveHistoricalAverageIsExtreme() {
		List<TransactionEntity> history =
				List.of(entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-1", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "500.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "US"));

		assertThat(features.categories()).contains("amount: extreme");
	}

	@Test
	void amountModeratelyAboveAverageIsUnusuallyHigh() {
		List<TransactionEntity> history =
				List.of(entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-1", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "40.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "US"));

		assertThat(features.categories()).contains("amount: unusually high");
	}

	@Test
	void fiveTransactionsInLastMinuteIsExtremeVelocity() {
		Instant now = Instant.parse("2026-01-01T10:00:00Z");
		List<TransactionEntity> history = IntStream.range(1, 6)
				.mapToObj(i -> entity("acct-1", "10.00", now.minusSeconds(i * 5), "device-1", "US"))
				.toList();
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(txn("acct-1", "10.00", now, "device-1", "US"));

		assertThat(features.categories()).contains("velocity: extreme");
	}

	@Test
	void unseenDeviceIsNew() {
		List<TransactionEntity> history =
				List.of(entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-old", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "10.00", Instant.parse("2026-01-01T10:00:00Z"), "device-new", "US"));

		assertThat(features.categories()).contains("device: new");
	}

	@Test
	void previouslySeenDeviceIsKnown() {
		List<TransactionEntity> history =
				List.of(entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-1", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "10.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "US"));

		assertThat(features.categories()).contains("device: known");
	}

	@Test
	void differentCountryThanHistoryIsGeoJump() {
		List<TransactionEntity> history = List.of(
				entity("acct-1", "10.00", Instant.parse("2026-01-01T08:00:00Z"), "device-1", "US"),
				entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-1", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "10.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "RU"));

		assertThat(features.categories()).contains("geo: jump");
	}

	@Test
	void sameCountryAsHistoryIsConsistent() {
		List<TransactionEntity> history =
				List.of(entity("acct-1", "10.00", Instant.parse("2026-01-01T09:00:00Z"), "device-1", "US"));
		when(repository.findByAccountIdOrderByOccurredAtDesc("acct-1")).thenReturn(history);
		FeatureService service = new FeatureService(repository);

		FeatureSummary features = service.computeFeatures(
				txn("acct-1", "10.00", Instant.parse("2026-01-01T10:00:00Z"), "device-1", "US"));

		assertThat(features.categories()).contains("geo: consistent");
	}

	private Transaction txn(String accountId, String amount, Instant occurredAt, String deviceId, String geoCountry) {
		return new Transaction(
				"txn-" + occurredAt, accountId, new BigDecimal(amount), "USD", "Coffee Shop", occurredAt, deviceId, geoCountry);
	}

	private TransactionEntity entity(
			String accountId, String amount, Instant occurredAt, String deviceId, String geoCountry) {
		return new TransactionEntity(
				"txn-" + occurredAt + "-" + deviceId,
				accountId,
				new BigDecimal(amount),
				"USD",
				"Coffee Shop",
				occurredAt,
				deviceId,
				geoCountry);
	}
}
