CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) NOT NULL,
    phone_digits VARCHAR(32) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_phone_digits (phone_digits)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_identities (
    user_id VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    provider_raw_id VARCHAR(128) NULL,
    display_name VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provider, provider_user_id),
    KEY idx_user_identities_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(64) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_id (user_id),
    KEY idx_refresh_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_media (
    id VARCHAR(64) NOT NULL,
    owner_user_id VARCHAR(64) NOT NULL,
    bucket VARCHAR(128) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NULL,
    visibility VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_bucket_object (bucket, object_key),
    KEY idx_media_owner (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_feed_snapshot (
    snapshot_id VARCHAR(32) NOT NULL,
    official_json JSON NOT NULL,
    user_json JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS conversation_messages (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at_ms BIGINT NOT NULL,
    metadata_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conv_messages_user_conv_time (user_id, conversation_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS conversation_insights (
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    insight_json JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Append-only: each successful / fallback insight generation (for longitudinal analytics; differs from upserted conversation_insights)
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

-- Structured features per user message (symptom / lifestyle / mood); supports future LLM extraction by changing source + payload shape
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

CREATE TABLE IF NOT EXISTS community_posts (
    id VARCHAR(64) NOT NULL,
    author_user_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    tags_json JSON NOT NULL,
    created_at_ms BIGINT NOT NULL,
    comment_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at_ms BIGINT NULL,
    deleted_by_user_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_community_posts_created (created_at_ms),
    KEY idx_community_posts_created_id (created_at_ms, id),
    KEY idx_community_posts_author (author_user_id),
    KEY idx_community_posts_deleted (is_deleted, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_post_media (
    post_id VARCHAR(64) NOT NULL,
    media_id VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, media_id),
    KEY idx_community_post_media_media_id (media_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_comments (
    id VARCHAR(64) NOT NULL,
    post_id VARCHAR(64) NOT NULL,
    author_user_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    created_at_ms BIGINT NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at_ms BIGINT NULL,
    deleted_by_user_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_community_comments_post_time (post_id, created_at_ms),
    KEY idx_community_comments_post_deleted_time (post_id, is_deleted, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_post_likes (
    post_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    KEY idx_community_post_likes_user (user_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS community_post_favorites (
    post_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    KEY idx_community_post_favorites_user (user_id, created_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS knowledge_docs (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    tags_json JSON NOT NULL,
    source VARCHAR(512) NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_knowledge_docs_updated (updated_at_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
