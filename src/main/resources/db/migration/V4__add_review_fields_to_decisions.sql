ALTER TABLE decisions
    ADD COLUMN case_summary TEXT,
    ADD COLUMN human_verdict VARCHAR(32),
    ADD COLUMN reviewed_at TIMESTAMPTZ;
