-- =====================================================
-- Game Tables for Integration Testing
-- 從主應用 V2__igaming_domain_tables.sql 提取
-- =====================================================

-- 1. Game Provider Table (遊戲供應商表)
CREATE TABLE IF NOT EXISTS t_game_provider (
    provider_id        BIGSERIAL       PRIMARY KEY,
    tenant_id          BIGINT          NOT NULL,
    provider_code      VARCHAR(50)     NOT NULL,
    provider_name      VARCHAR(100)    NOT NULL,
    api_url            VARCHAR(255),
    encrypted_api_key  TEXT,
    callback_url       VARCHAR(255),
    supported_games    TEXT,
    enabled            BOOLEAN         NOT NULL DEFAULT TRUE,
    health_status      INTEGER         DEFAULT 1,  -- 1=HEALTHY, 2=DEGRADED, 3=DOWN
    last_health_check  TIMESTAMPTZ,
    deleted            BOOLEAN         NOT NULL DEFAULT FALSE,
    version            INTEGER         NOT NULL DEFAULT 0,
    create_time        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_game_provider IS '遊戲供應商配置表';
COMMENT ON COLUMN t_game_provider.provider_code IS '供應商代碼 (e.g., PRAGMATIC_PLAY, EVOLUTION)';
COMMENT ON COLUMN t_game_provider.api_url IS 'Game Provider API endpoint';
COMMENT ON COLUMN t_game_provider.encrypted_api_key IS 'API key (AES-256-GCM encrypted)';
COMMENT ON COLUMN t_game_provider.enabled IS '啟用狀態: TRUE=啟用, FALSE=禁用';
COMMENT ON COLUMN t_game_provider.health_status IS '健康狀態: 1=HEALTHY, 2=DEGRADED, 3=DOWN';

-- 2. Game Catalog Table (遊戲目錄表)
CREATE TABLE IF NOT EXISTS t_game (
    game_id           BIGSERIAL       PRIMARY KEY,
    tenant_id         BIGINT          NOT NULL,
    provider_id       BIGINT          NOT NULL REFERENCES t_game_provider(provider_id),
    game_code         VARCHAR(100)    NOT NULL,
    game_name         VARCHAR(200)    NOT NULL,
    category          INTEGER         NOT NULL,  -- 1=Slots, 2=Live, 3=Sports, 4=Poker, 5=Table, 6=Lottery
    thumbnail_url     VARCHAR(500),
    play_count        BIGINT          NOT NULL DEFAULT 0,
    enabled           BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted           BOOLEAN         NOT NULL DEFAULT FALSE,
    version           INTEGER         NOT NULL DEFAULT 0,
    create_time       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_game_provider FOREIGN KEY (provider_id) REFERENCES t_game_provider(provider_id)
);

-- Partial unique index for t_game_provider: MUST be created BEFORE seed data INSERT
CREATE UNIQUE INDEX uk_provider_tenant_code ON t_game_provider(tenant_id, provider_code) WHERE deleted = false;

COMMENT ON TABLE t_game IS '遊戲目錄表';
COMMENT ON COLUMN t_game.category IS '遊戲類別: 1=Slots, 2=Live, 3=Sports, 4=Poker, 5=Table, 6=Lottery';
COMMENT ON COLUMN t_game.enabled IS '遊戲啟用狀態: TRUE=啟用, FALSE=禁用';

-- Indexes
CREATE INDEX idx_game_tenant_category ON t_game(tenant_id, category);
CREATE INDEX idx_game_provider ON t_game(provider_id);

-- Partial unique index for t_game: Allows soft-deleted records to not interfere
CREATE UNIQUE INDEX uk_game_tenant_code ON t_game(tenant_id, game_code) WHERE deleted = false;

-- 3. Seed Data: Mock Game Provider (required by test code with providerId=1L)
-- Note: No ON CONFLICT clause because partial unique index doesn't support it
-- Testcontainers creates fresh database each time, so INSERT will always succeed
INSERT INTO t_game_provider (tenant_id, provider_code, provider_name, api_url, enabled, deleted)
VALUES (1, 'MOCK_PROVIDER', 'Mock Game Provider', 'http://localhost:8080/mock-game-api', true, false);

-- Note: Game seed data removed to allow test code to control game data insertion
