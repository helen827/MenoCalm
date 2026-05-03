CREATE TABLE IF NOT EXISTS journal_entries (
    user_id VARCHAR(64) NOT NULL,
    entry_id VARCHAR(64) NOT NULL,
    date_text VARCHAR(32) NULL,
    raw_text TEXT NULL,
    created_at_ms DOUBLE NOT NULL,
    revision INT NULL,
    extracted_json JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, entry_id),
    KEY idx_journal_user_created (user_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
