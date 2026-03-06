-- =====================================================================
-- V7: Idempotent key table for event consumer deduplication (ADR-015)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_idempotent_key (
    id              BIGSERIAL       PRIMARY KEY,
    event_id        VARCHAR(64)     NOT NULL,
    event_type      VARCHAR(128)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_event_id UNIQUE (event_id)
);

COMMENT ON TABLE t_idempotent_key IS '冪等鍵值表 — 事件消費去重';
COMMENT ON COLUMN t_idempotent_key.event_id IS '事件唯一識別碼 (UUID)';
COMMENT ON COLUMN t_idempotent_key.event_type IS '事件類型（用於審計追蹤）';
COMMENT ON COLUMN t_idempotent_key.created_at IS '建立時間';

-- Index for cleanup job (remove records older than 7 days)
CREATE INDEX IF NOT EXISTS idx_idempotent_key_created_at ON t_idempotent_key (created_at);
