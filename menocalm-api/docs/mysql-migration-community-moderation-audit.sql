-- Optional: run in MySQL to persist moderation actions (app also logs WARN on each action).
CREATE TABLE IF NOT EXISTS community_moderation_audit (
    id VARCHAR(64) NOT NULL,
    operator_user_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    reason VARCHAR(512) NULL,
    created_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_moderation_audit_time (created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
