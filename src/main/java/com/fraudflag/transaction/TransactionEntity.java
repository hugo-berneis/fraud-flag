package com.fraudflag.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false, unique = true)
	private String transactionId;

	@Column(name = "account_id", nullable = false)
	private String accountId;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private String merchant;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "device_id")
	private String deviceId;

	@Column(name = "geo_country")
	private String geoCountry;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TransactionEntity() {
		// required by JPA
	}

	public TransactionEntity(
			String transactionId,
			String accountId,
			BigDecimal amount,
			String currency,
			String merchant,
			Instant occurredAt,
			String deviceId,
			String geoCountry) {
		this.transactionId = transactionId;
		this.accountId = accountId;
		this.amount = amount;
		this.currency = currency;
		this.merchant = merchant;
		this.occurredAt = occurredAt;
		this.deviceId = deviceId;
		this.geoCountry = geoCountry;
	}

	public Long getId() {
		return id;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public String getAccountId() {
		return accountId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getMerchant() {
		return merchant;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getGeoCountry() {
		return geoCountry;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
