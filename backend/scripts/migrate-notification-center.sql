-- Run against an existing smart-education database before deploying notification-center code.
-- The statements are idempotent for environments where phase one was already applied.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS edu_notification (
    id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    announcement_id BIGINT NULL,
    course_id BIGINT NULL,
    assignment_id BIGINT NULL,
    exam_id BIGINT NULL,
    warning_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_idempotency (idempotency_key),
    KEY idx_notification_recipient (recipient_user_id, status, category, deleted, created_at, id),
    KEY idx_notification_announcement (announcement_id, status, deleted, id)
);

CREATE TABLE IF NOT EXISTS edu_notification_read (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    read_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_read_user (user_id, notification_id),
    KEY idx_notification_read_user (user_id, deleted, notification_id)
);

CREATE TABLE IF NOT EXISTS edu_notification_preference (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    updated_by BIGINT NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_preference_user (user_id, category),
    KEY idx_notification_preference_user (user_id, deleted, category)
);
