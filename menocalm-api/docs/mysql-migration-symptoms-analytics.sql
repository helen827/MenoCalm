-- Apply on existing databases that already have conversation_insights.
-- Safe to run multiple times (IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS conversation_insight_snapshots (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    insight_json JSON NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_insight_snap_user_time (user_id, created_at_ms),
    KEY idx_insight_snap_conv_time (user_id, conversation_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS symptoms_log (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NULL,
    user_message_excerpt VARCHAR(512) NULL,
    payload_json JSON NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_symptoms_log_user_time (user_id, created_at_ms),
    KEY idx_symptoms_log_conv_time (user_id, conversation_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
