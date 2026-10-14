ALTER TABLE transactions
    ADD COLUMN device_id VARCHAR(64),
    ADD COLUMN geo_country VARCHAR(2);
