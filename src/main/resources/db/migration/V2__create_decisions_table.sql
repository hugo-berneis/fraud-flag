CREATE TABLE decisions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL REFERENCES transactions (transaction_id),
    features JSONB NOT NULL,
    rule_hits JSONB NOT NULL DEFAULT '[]'::JSONB,
    is_fraud BOOLEAN,
    fraud_type VARCHAR(64),
    confidence DOUBLE PRECISION,
    route VARCHAR(32) NOT NULL,
    latency_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_decisions_transaction_id ON decisions (transaction_id);
