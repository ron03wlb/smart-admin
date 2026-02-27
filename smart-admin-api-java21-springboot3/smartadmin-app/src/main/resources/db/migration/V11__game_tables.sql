-- =====================================================================
-- V11: Game Integration Tables — Phase 3 (02-game-integration.md)
-- =====================================================================
-- Creates 5 tables:
--   - t_game_provider: Game provider (GP) configuration
--   - t_game: Game catalog
--   - t_game_round: Game round records (seamless wallet core)
--   - t_reconciliation: Daily reconciliation summary
--   - t_game_weight_config: Per-tenant game category weights
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - BIGINT for tenant_id (Multi-Tenant G1)
--   - PII fields: AES-256-GCM encrypted (GP API keys)
-- =====================================================================

-- =====================================================================
-- Part A: t_game_provider (Game Provider Configuration)
-- =====================================================================

CREATE TABLE t_game_provider (
    provider_id         BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    provider_code       VARCHAR(32)     NOT NULL,
    provider_name       VARCHAR(128)    NOT NULL,
    api_url             VARCHAR(512)    NOT NULL,
    encrypted_api_key   VARCHAR(1024),
    callback_url        VARCHAR(512),
    supported_games     JSONB,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    health_status       SMALLINT        NOT NULL DEFAULT 1,
    last_health_check   TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_gp_health_status CHECK (health_status IN (1, 2, 3))
);

COMMENT ON TABLE t_game_provider IS '遊戲供應商配置表';
COMMENT ON COLUMN t_game_provider.provider_id IS '供應商唯一標識';
COMMENT ON COLUMN t_game_provider.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_game_provider.provider_code IS '供應商代碼 (如 pgsoft, pragmatic, mock)';
COMMENT ON COLUMN t_game_provider.provider_name IS '供應商名稱';
COMMENT ON COLUMN t_game_provider.api_url IS 'API 位址';
COMMENT ON COLUMN t_game_provider.encrypted_api_key IS 'API 金鑰 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_game_provider.callback_url IS '回呼位址';
COMMENT ON COLUMN t_game_provider.supported_games IS '支援的遊戲列表 (JSONB)';
COMMENT ON COLUMN t_game_provider.enabled IS '是否啟用';
COMMENT ON COLUMN t_game_provider.health_status IS '健康狀態: 1=健康, 2=降級, 3=下線';
COMMENT ON COLUMN t_game_provider.last_health_check IS '最後健康檢查時間';
COMMENT ON COLUMN t_game_provider.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_game_provider.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX uk_gp_code_tenant ON t_game_provider (provider_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_gp_tenant ON t_game_provider (tenant_id);

-- =====================================================================
-- Part B: t_game (Game Catalog)
-- =====================================================================

CREATE TABLE t_game (
    game_id             BIGSERIAL       PRIMARY KEY,
    provider_id         BIGINT          NOT NULL REFERENCES t_game_provider(provider_id),
    tenant_id           BIGINT          NOT NULL,
    game_code           VARCHAR(64)     NOT NULL,
    game_name           VARCHAR(128)    NOT NULL,
    category            SMALLINT        NOT NULL,
    thumbnail_url       VARCHAR(512),
    play_count          BIGINT          NOT NULL DEFAULT 0,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_game_category CHECK (category IN (1, 2, 3, 4, 5, 6))
);

COMMENT ON TABLE t_game IS '遊戲主表';
COMMENT ON COLUMN t_game.game_id IS '遊戲唯一標識';
COMMENT ON COLUMN t_game.provider_id IS '供應商 ID';
COMMENT ON COLUMN t_game.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_game.game_code IS '遊戲代碼 (租戶內唯一)';
COMMENT ON COLUMN t_game.game_name IS '遊戲名稱';
COMMENT ON COLUMN t_game.category IS '遊戲類別: 1=老虎機, 2=真人娛樂城, 3=體育, 4=撲克, 5=桌遊, 6=彩票';
COMMENT ON COLUMN t_game.thumbnail_url IS '縮圖 URL';
COMMENT ON COLUMN t_game.play_count IS '遊玩次數';
COMMENT ON COLUMN t_game.enabled IS '是否啟用';
COMMENT ON COLUMN t_game.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_game.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX uk_game_code_tenant ON t_game (game_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_game_tenant_category ON t_game (tenant_id, category);
CREATE INDEX idx_game_provider ON t_game (provider_id);

-- =====================================================================
-- Part C: t_game_round (Game Round Records — Seamless Wallet Core)
-- =====================================================================

CREATE TABLE t_game_round (
    round_id                BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    provider_code           VARCHAR(32)     NOT NULL,
    gp_round_id             VARCHAR(128)    NOT NULL,
    game_code               VARCHAR(64)     NOT NULL,
    transaction_id          VARCHAR(128)    NOT NULL,
    bet_amount              DECIMAL(19,4),
    payout_amount           DECIMAL(19,4),
    weighted_turnover       DECIMAL(19,4),
    status                  SMALLINT        NOT NULL DEFAULT 1,
    reconciliation_status   SMALLINT        NOT NULL DEFAULT 1,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_game_round_txn UNIQUE (transaction_id),
    CONSTRAINT ck_round_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT ck_recon_status CHECK (reconciliation_status IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_game_round IS '遊戲局記錄表 (Seamless Wallet 核心)';
COMMENT ON COLUMN t_game_round.round_id IS '局唯一標識';
COMMENT ON COLUMN t_game_round.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_game_round.player_id IS '玩家 ID';
COMMENT ON COLUMN t_game_round.provider_code IS '供應商代碼';
COMMENT ON COLUMN t_game_round.gp_round_id IS 'GP 局號';
COMMENT ON COLUMN t_game_round.game_code IS '遊戲代碼';
COMMENT ON COLUMN t_game_round.transaction_id IS '交易唯一鍵 (冪等性)';
COMMENT ON COLUMN t_game_round.bet_amount IS '下注金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_game_round.payout_amount IS '派彩金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_game_round.weighted_turnover IS '加權有效投注額 DECIMAL(19,4)';
COMMENT ON COLUMN t_game_round.status IS '局狀態: 1=進行中, 2=已結算, 3=已取消, 4=已作廢';
COMMENT ON COLUMN t_game_round.reconciliation_status IS '對帳狀態: 1=待核對, 2=已驗證, 3=不符, 4=已補償, 5=已調和';
COMMENT ON COLUMN t_game_round.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_game_round.version IS '樂觀鎖版本號';

CREATE INDEX idx_round_player ON t_game_round (player_id);
CREATE INDEX idx_round_provider_date ON t_game_round (provider_code, create_time);
CREATE INDEX idx_round_status ON t_game_round (status, reconciliation_status);
CREATE INDEX idx_round_tenant ON t_game_round (tenant_id);

-- =====================================================================
-- Part D: t_reconciliation (Daily Reconciliation Summary)
-- =====================================================================

CREATE TABLE t_reconciliation (
    reconciliation_id               BIGSERIAL       PRIMARY KEY,
    tenant_id                       BIGINT          NOT NULL,
    reconciliation_date             DATE            NOT NULL,
    provider_code                   VARCHAR(32)     NOT NULL,
    total_gp_transactions           INT             NOT NULL DEFAULT 0,
    total_platform_transactions     INT             NOT NULL DEFAULT 0,
    matched_count                   INT             NOT NULL DEFAULT 0,
    mismatch_count                  INT             NOT NULL DEFAULT 0,
    missing_count                   INT             NOT NULL DEFAULT 0,
    extra_count                     INT             NOT NULL DEFAULT 0,
    status                          SMALLINT        NOT NULL DEFAULT 1,
    deleted                         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_reconciliation IS '日對帳彙總表';
COMMENT ON COLUMN t_reconciliation.reconciliation_id IS '對帳唯一標識';
COMMENT ON COLUMN t_reconciliation.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_reconciliation.reconciliation_date IS '對帳日期';
COMMENT ON COLUMN t_reconciliation.provider_code IS '供應商代碼';
COMMENT ON COLUMN t_reconciliation.total_gp_transactions IS 'GP 交易筆數';
COMMENT ON COLUMN t_reconciliation.total_platform_transactions IS '平台交易筆數';
COMMENT ON COLUMN t_reconciliation.matched_count IS '匹配筆數';
COMMENT ON COLUMN t_reconciliation.mismatch_count IS '不符筆數';
COMMENT ON COLUMN t_reconciliation.missing_count IS '缺失筆數 (GP 有、平台無)';
COMMENT ON COLUMN t_reconciliation.extra_count IS '多餘筆數 (平台有、GP 無)';
COMMENT ON COLUMN t_reconciliation.status IS '對帳狀態: 1=待核對, 2=已驗證, 3=不符, 4=已補償, 5=已調和';

CREATE UNIQUE INDEX uk_recon_date_provider_tenant ON t_reconciliation (reconciliation_date, provider_code, tenant_id);
CREATE INDEX idx_recon_tenant ON t_reconciliation (tenant_id);

-- =====================================================================
-- Part E: t_game_weight_config (Per-Tenant Game Category Weights)
-- =====================================================================

CREATE TABLE t_game_weight_config (
    config_id       BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    game_category   SMALLINT        NOT NULL,
    weight          DECIMAL(5,4)    NOT NULL,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_weight_tenant_category UNIQUE (tenant_id, game_category),
    CONSTRAINT ck_weight_range CHECK (weight >= 0 AND weight <= 1)
);

COMMENT ON TABLE t_game_weight_config IS '遊戲權重配置表 (按租戶)';
COMMENT ON COLUMN t_game_weight_config.config_id IS '配置唯一標識';
COMMENT ON COLUMN t_game_weight_config.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_game_weight_config.game_category IS '遊戲類別 (對應 GameCategoryEnum)';
COMMENT ON COLUMN t_game_weight_config.weight IS '權重 0.0000~1.0000';

-- =====================================================================
-- Part F: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_game_provider ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game_round ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_reconciliation ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game_weight_config ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_game_provider
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_game
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_game_round
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_reconciliation
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_game_weight_config
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part G: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_provider TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_round TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_reconciliation TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_weight_config TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_game_provider_provider_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_game_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_round_round_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_reconciliation_reconciliation_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_weight_config_config_id_seq TO smartadmin_app;
