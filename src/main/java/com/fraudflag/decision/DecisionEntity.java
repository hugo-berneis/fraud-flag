package com.fraudflag.decision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "decisions")
public class DecisionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false)
	private String transactionId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private List<String> features;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rule_hits", nullable = false)
	private List<String> ruleHits;

	@Column(name = "is_fraud")
	private Boolean isFraud;

	@Column(name = "fraud_type")
	private String fraudType;

	private Double confidence;

	@Column(nullable = false)
	private String route;

	@Column(name = "latency_ms", nullable = false)
	private long latencyMs;

	@Column(name = "case_summary")
	private String caseSummary;

	@Column(name = "human_verdict")
	private String humanVerdict;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DecisionEntity() {
		// required by JPA
	}

	public DecisionEntity(
			String transactionId,
			List<String> features,
			List<String> ruleHits,
			Boolean isFraud,
			String fraudType,
			Double confidence,
			Route route,
			long latencyMs,
			String caseSummary) {
		this.transactionId = transactionId;
		this.features = features;
		this.ruleHits = ruleHits;
		this.isFraud = isFraud;
		this.fraudType = fraudType;
		this.confidence = confidence;
		this.route = route.name();
		this.latencyMs = latencyMs;
		this.caseSummary = caseSummary;
	}

	public void recordHumanVerdict(HumanVerdict verdict) {
		this.humanVerdict = verdict.name();
		this.reviewedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public List<String> getFeatures() {
		return features;
	}

	public List<String> getRuleHits() {
		return ruleHits;
	}

	public Boolean getIsFraud() {
		return isFraud;
	}

	public String getFraudType() {
		return fraudType;
	}

	public Double getConfidence() {
		return confidence;
	}

	public Route getRoute() {
		return Route.valueOf(route);
	}

	public long getLatencyMs() {
		return latencyMs;
	}

	public String getCaseSummary() {
		return caseSummary;
	}

	public HumanVerdict getHumanVerdict() {
		return humanVerdict == null ? null : HumanVerdict.valueOf(humanVerdict);
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
