CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    account_id VARCHAR(64) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
