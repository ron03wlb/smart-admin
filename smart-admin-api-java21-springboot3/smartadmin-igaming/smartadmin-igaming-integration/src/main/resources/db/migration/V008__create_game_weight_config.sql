-- =====================================================
-- Game Weight Configuration Table
-- 從主應用 V2__igaming_domain_tables.sql 提取
-- ⚠️ CRITICAL: 解決 "relation t_game_weight_config does not exist" 錯誤
-- =====================================================

-- 1. Game Weight Config Table (遊戲權重配置表)
CREATE TABLE IF NOT EXISTS t_game_weight_config (
    config_id       BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    game_category   SMALLINT        NOT NULL,  -- 1=Slots, 2=Live, 3=Sports, 4=Poker, 5=Table, 6=Lottery
    weight          DECIMAL(5,4)    NOT NULL,  -- 0.0000-1.0000 (0%-100%)
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_weight_range CHECK (weight >= 0 AND weight <= 1)
);

COMMENT ON TABLE t_game_weight_config IS '遊戲權重配置表 - 用於流水計算';
COMMENT ON COLUMN t_game_weight_config.game_category IS '遊戲類別: 1=Slots, 2=Live, 3=Sports, 4=Poker, 5=Table, 6=Lottery';
COMMENT ON COLUMN t_game_weight_config.weight IS '流水權重: 0.0000-1.0000 (1.0000 = 100% 計入有效流水)';

-- Indexes
CREATE INDEX idx_weight_tenant_category ON t_game_weight_config(tenant_id, game_category);

-- Partial unique index: Allows soft-deleted records to not interfere with uniqueness
CREATE UNIQUE INDEX uk_weight_tenant_category ON t_game_weight_config(tenant_id, game_category) WHERE deleted = false;

-- Note: Seed data removed to allow test code to control test data insertion
