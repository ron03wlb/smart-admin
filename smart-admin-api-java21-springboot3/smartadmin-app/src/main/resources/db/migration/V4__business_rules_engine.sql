-- =====================================================================
-- V4: Business Rules Engine — Consolidation of V24-V27 (+ V30 fix)
-- =====================================================================
-- This migration consolidates the business rules engine setup:
--   V24: Turnover rule tables (5 tables) — WITHOUT enabling RLS
--   V25: LiteFlow workflow tables (2 tables) — WITHOUT enabling RLS
--   V26: (SKIP) Disable LiteFlow RLS — Already optimized in this consolidated version
--   V27: (SKIP) Disable turnover RLS — Already optimized in this consolidated version
--   V30: (INTEGRATED) Fix risk_level constraint to allow 4 levels (not 3)
--
-- Total New Tables: 7 tables
--   - t_turnover_game_weight_rule (V24)
--   - t_turnover_status_factor_rule (V24)
--   - t_turnover_odds_threshold_rule (V24)
--   - t_turnover_risk_action_rule (V24, with V30 fix integrated)
--   - t_turnover_rule_change_log (V24)
--   - t_liteflow_chain (V25)
--   - t_liteflow_script (V25)
--
-- ⚠️ KEY OPTIMIZATION (vs Original V24-V27):
--   - Original V24/V25: Created tables + ENABLED RLS
--   - Original V26/V27: DISABLED RLS (反覆操作)
--   - This V4: Creates tables WITHOUT enabling RLS (避免反覆操作)
--
-- Rationale for NO RLS:
--   - LiteFlow tables: System-level configuration, uses isolated database connection
--   - Turnover tables: System-level configuration, application-layer tenant filtering (WHERE tenant_id = ?)
--   - Comment added to each table explaining RLS disabled reason
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(5,2) for percentage values (0.00 to 100.00)
--   - DECIMAL(10,4) for odds values (high precision)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - BIGINT for tenant_id (Multi-Tenant G1)
--   - All DDL uses IF NOT EXISTS for idempotency
-- =====================================================================

-- =====================================================================
-- PART 1: Turnover Rule Tables (V24) — WITHOUT RLS
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1.1: t_turnover_game_weight_rule (Game Category Weight Rules)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_turnover_game_weight_rule (
    rule_id             BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    rule_code           VARCHAR(64)     NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    game_category       SMALLINT        NOT NULL,
    weight_percentage   DECIMAL(5,2)    NOT NULL,
    priority            INT             NOT NULL DEFAULT 100,
    effective_from      TIMESTAMPTZ,
    effective_to        TIMESTAMPTZ,
    status              SMALLINT        NOT NULL DEFAULT 1,
    remark              VARCHAR(512),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_game_category CHECK (game_category IN (1, 2, 3, 4, 5, 6)),
    CONSTRAINT ck_weight_percentage CHECK (weight_percentage >= 0 AND weight_percentage <= 100),
    CONSTRAINT ck_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_turnover_game_weight_rule IS '流水計算規則 - 遊戲權重配置 (Layer 3) - RLS disabled, app-layer tenant filtering';
COMMENT ON COLUMN t_turnover_game_weight_rule.rule_id IS '規則唯一標識';
COMMENT ON COLUMN t_turnover_game_weight_rule.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_turnover_game_weight_rule.rule_code IS '規則代碼 (租戶內唯一業務鍵)';
COMMENT ON COLUMN t_turnover_game_weight_rule.rule_name IS '規則名稱';
COMMENT ON COLUMN t_turnover_game_weight_rule.game_category IS '遊戲類別: 1=老虎機, 2=真人娛樂城, 3=體育博彩, 4=撲克, 5=桌遊, 6=彩票';
COMMENT ON COLUMN t_turnover_game_weight_rule.weight_percentage IS '權重百分比 (0.00-100.00)';
COMMENT ON COLUMN t_turnover_game_weight_rule.priority IS '優先級 (數字越小優先級越高)';
COMMENT ON COLUMN t_turnover_game_weight_rule.effective_from IS '生效開始時間';
COMMENT ON COLUMN t_turnover_game_weight_rule.effective_to IS '生效結束時間';
COMMENT ON COLUMN t_turnover_game_weight_rule.status IS '狀態: 1=啟用, 2=禁用, 3=已過期';
COMMENT ON COLUMN t_turnover_game_weight_rule.remark IS '備註說明';
COMMENT ON COLUMN t_turnover_game_weight_rule.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_turnover_game_weight_rule.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_weight_code_tenant ON t_turnover_game_weight_rule (rule_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_game_weight_tenant_category ON t_turnover_game_weight_rule (tenant_id, game_category, status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_game_weight_effective ON t_turnover_game_weight_rule (tenant_id, effective_from, effective_to, status) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- 1.2: t_turnover_status_factor_rule (Settlement Status Factor Rules)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_turnover_status_factor_rule (
    rule_id             BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    rule_code           VARCHAR(64)     NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    settlement_status   SMALLINT        NOT NULL,
    factor_percentage   DECIMAL(5,2)    NOT NULL,
    effective_from      TIMESTAMPTZ,
    effective_to        TIMESTAMPTZ,
    status              SMALLINT        NOT NULL DEFAULT 1,
    remark              VARCHAR(512),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_settlement_status CHECK (settlement_status IN (1, 2, 3, 4, 5, 6, 7, 8, 9)),
    CONSTRAINT ck_factor_percentage CHECK (factor_percentage >= 0 AND factor_percentage <= 100),
    CONSTRAINT ck_sf_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_turnover_status_factor_rule IS '流水計算規則 - 結算狀態因子配置 (Layer 2) - RLS disabled, app-layer tenant filtering';
COMMENT ON COLUMN t_turnover_status_factor_rule.rule_id IS '規則唯一標識';
COMMENT ON COLUMN t_turnover_status_factor_rule.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_turnover_status_factor_rule.rule_code IS '規則代碼 (租戶內唯一業務鍵)';
COMMENT ON COLUMN t_turnover_status_factor_rule.rule_name IS '規則名稱';
COMMENT ON COLUMN t_turnover_status_factor_rule.settlement_status IS '結算狀態: 1=贏, 2=輸, 3=半贏, 4=半輸, 5=平局, 6=和局, 7=作廢, 8=取消, 9=進行中';
COMMENT ON COLUMN t_turnover_status_factor_rule.factor_percentage IS '因子百分比 (0.00-100.00)';
COMMENT ON COLUMN t_turnover_status_factor_rule.effective_from IS '生效開始時間';
COMMENT ON COLUMN t_turnover_status_factor_rule.effective_to IS '生效結束時間';
COMMENT ON COLUMN t_turnover_status_factor_rule.status IS '狀態: 1=啟用, 2=禁用, 3=已過期';
COMMENT ON COLUMN t_turnover_status_factor_rule.remark IS '備註說明';
COMMENT ON COLUMN t_turnover_status_factor_rule.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_turnover_status_factor_rule.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_status_factor_code_tenant ON t_turnover_status_factor_rule (rule_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_status_factor_tenant_settlement ON t_turnover_status_factor_rule (tenant_id, settlement_status, status) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- 1.3: t_turnover_odds_threshold_rule (Odds Threshold Rules)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_turnover_odds_threshold_rule (
    rule_id             BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    rule_code           VARCHAR(64)     NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    odds_type           SMALLINT        NOT NULL,
    threshold_value     DECIMAL(10,4)   NOT NULL,
    comparison_operator VARCHAR(8)      NOT NULL,
    effective_from      TIMESTAMPTZ,
    effective_to        TIMESTAMPTZ,
    status              SMALLINT        NOT NULL DEFAULT 1,
    remark              VARCHAR(512),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_odds_type CHECK (odds_type IN (1, 2, 3, 4)),
    CONSTRAINT ck_comparison_operator CHECK (comparison_operator IN ('>=', '>', '<=', '<', '=')),
    CONSTRAINT ck_odds_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_turnover_odds_threshold_rule IS '流水計算規則 - 賠率閾值配置 (Layer 2 Risk Filter) - RLS disabled, app-layer tenant filtering';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.rule_id IS '規則唯一標識';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.rule_code IS '規則代碼 (租戶內唯一業務鍵)';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.rule_name IS '規則名稱';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.odds_type IS '賠率類型: 1=歐洲盤(EUR), 2=香港盤(HK), 3=馬來盤(MY), 4=印尼盤(ID)';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.threshold_value IS '閾值 (DECIMAL(10,4) 高精度)';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.comparison_operator IS '比較運算符: >=, >, <=, <, =';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.effective_from IS '生效開始時間';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.effective_to IS '生效結束時間';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.status IS '狀態: 1=啟用, 2=禁用, 3=已過期';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.remark IS '備註說明';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_odds_threshold_code_tenant ON t_turnover_odds_threshold_rule (rule_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_odds_threshold_tenant_type ON t_turnover_odds_threshold_rule (tenant_id, odds_type, status) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- 1.4: t_turnover_risk_action_rule (Risk Level Action Rules)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_turnover_risk_action_rule (
    rule_id             BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    rule_code           VARCHAR(64)     NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    risk_level          SMALLINT        NOT NULL,
    action_type         SMALLINT        NOT NULL,
    turnover_factor     DECIMAL(5,2)    NOT NULL,
    allow_bet           BOOLEAN         NOT NULL DEFAULT TRUE,
    create_proposal     BOOLEAN         NOT NULL DEFAULT FALSE,
    effective_from      TIMESTAMPTZ,
    effective_to        TIMESTAMPTZ,
    status              SMALLINT        NOT NULL DEFAULT 1,
    remark              VARCHAR(512),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_risk_level CHECK (risk_level IN (1, 2, 3, 4)),  -- ⚠️ V30 FIX: 4 levels (not 3)
    CONSTRAINT ck_action_type CHECK (action_type IN (1, 2, 3)),
    CONSTRAINT ck_turnover_factor CHECK (turnover_factor >= 0 AND turnover_factor <= 100),
    CONSTRAINT ck_risk_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_turnover_risk_action_rule IS '流水計算規則 - 風控動作配置 (Layer 1 Risk Engine) - RLS disabled, app-layer tenant filtering';
COMMENT ON COLUMN t_turnover_risk_action_rule.rule_id IS '規則唯一標識';
COMMENT ON COLUMN t_turnover_risk_action_rule.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_turnover_risk_action_rule.rule_code IS '規則代碼 (租戶內唯一業務鍵)';
COMMENT ON COLUMN t_turnover_risk_action_rule.rule_name IS '規則名稱';
COMMENT ON COLUMN t_turnover_risk_action_rule.risk_level IS '風險等級: 1=低風險, 2=中風險, 3=高風險, 4=極高風險 (V30 FIX)';
COMMENT ON COLUMN t_turnover_risk_action_rule.action_type IS '動作類型: 1=通過(PASS), 2=標記(FLAG), 3=阻擋(BLOCK)';
COMMENT ON COLUMN t_turnover_risk_action_rule.turnover_factor IS '流水因子百分比 (0.00-100.00)';
COMMENT ON COLUMN t_turnover_risk_action_rule.allow_bet IS '是否允許下注';
COMMENT ON COLUMN t_turnover_risk_action_rule.create_proposal IS '是否建立審核提案';
COMMENT ON COLUMN t_turnover_risk_action_rule.effective_from IS '生效開始時間';
COMMENT ON COLUMN t_turnover_risk_action_rule.effective_to IS '生效結束時間';
COMMENT ON COLUMN t_turnover_risk_action_rule.status IS '狀態: 1=啟用, 2=禁用, 3=已過期';
COMMENT ON COLUMN t_turnover_risk_action_rule.remark IS '備註說明';
COMMENT ON COLUMN t_turnover_risk_action_rule.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_turnover_risk_action_rule.version IS '樂觀鎖版本號';

COMMENT ON CONSTRAINT ck_risk_level ON t_turnover_risk_action_rule IS 'Risk level: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL (V30 FIX)';

CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_action_code_tenant ON t_turnover_risk_action_rule (rule_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_risk_action_tenant_level ON t_turnover_risk_action_rule (tenant_id, risk_level, status) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- 1.5: t_turnover_rule_change_log (Rule Change Audit Log)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_turnover_rule_change_log (
    log_id              BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    rule_type           SMALLINT        NOT NULL,
    rule_id             BIGINT          NOT NULL,
    rule_code           VARCHAR(64)     NOT NULL,
    operation_type      SMALLINT        NOT NULL,
    old_value           JSONB,
    new_value           JSONB,
    change_reason       VARCHAR(512),
    operator_id         BIGINT          NOT NULL,
    operator_name       VARCHAR(64)     NOT NULL,
    ip_address          VARCHAR(64),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_rule_type CHECK (rule_type IN (1, 2, 3, 4)),
    CONSTRAINT ck_operation_type CHECK (operation_type IN (1, 2, 3))
);

COMMENT ON TABLE t_turnover_rule_change_log IS '流水計算規則變更歷史表 (JSONB 審計追蹤) - RLS disabled, app-layer tenant filtering';
COMMENT ON COLUMN t_turnover_rule_change_log.log_id IS '日誌唯一標識';
COMMENT ON COLUMN t_turnover_rule_change_log.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_turnover_rule_change_log.rule_type IS '規則類型: 1=遊戲權重, 2=狀態因子, 3=賠率閾值, 4=風控動作';
COMMENT ON COLUMN t_turnover_rule_change_log.rule_id IS '規則 ID';
COMMENT ON COLUMN t_turnover_rule_change_log.rule_code IS '規則代碼';
COMMENT ON COLUMN t_turnover_rule_change_log.operation_type IS '操作類型: 1=新增(CREATE), 2=更新(UPDATE), 3=刪除(DELETE)';
COMMENT ON COLUMN t_turnover_rule_change_log.old_value IS '變更前完整規則 (JSONB 快照)';
COMMENT ON COLUMN t_turnover_rule_change_log.new_value IS '變更後完整規則 (JSONB 快照)';
COMMENT ON COLUMN t_turnover_rule_change_log.change_reason IS '變更原因';
COMMENT ON COLUMN t_turnover_rule_change_log.operator_id IS '操作人員 ID';
COMMENT ON COLUMN t_turnover_rule_change_log.operator_name IS '操作人員姓名';
COMMENT ON COLUMN t_turnover_rule_change_log.ip_address IS 'IP 位址';
COMMENT ON COLUMN t_turnover_rule_change_log.create_time IS '變更時間';

CREATE INDEX IF NOT EXISTS idx_change_log_tenant_rule ON t_turnover_rule_change_log (tenant_id, rule_type, rule_id);
CREATE INDEX IF NOT EXISTS idx_change_log_time ON t_turnover_rule_change_log (create_time DESC);
CREATE INDEX IF NOT EXISTS idx_change_log_operator ON t_turnover_rule_change_log (operator_id);

-- =====================================================================
-- PART 2: LiteFlow Workflow Tables (V25) — WITHOUT RLS
-- =====================================================================

-- ---------------------------------------------------------------------
-- 2.1: t_liteflow_chain (LiteFlow Rule Chain Definitions)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_liteflow_chain (
    chain_id            BIGSERIAL       PRIMARY KEY,
    application_name    VARCHAR(64)     NOT NULL,
    chain_name          VARCHAR(64)     NOT NULL,
    chain_desc          VARCHAR(256),
    el_data             TEXT            NOT NULL,
    enable              BOOLEAN         NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_liteflow_chain_app_name UNIQUE (application_name, chain_name)
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow 規則鏈定義表 (System-level config, isolated DB connection, RLS disabled)';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '規則鏈唯一標識';
COMMENT ON COLUMN t_liteflow_chain.application_name IS '應用標識 (例如: smartadmin-igaming-activity)';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '規則鏈名稱 (應用內唯一)';
COMMENT ON COLUMN t_liteflow_chain.chain_desc IS '規則鏈描述';
COMMENT ON COLUMN t_liteflow_chain.el_data IS 'EL 表達式腳本 (定義規則鏈執行流程)';
COMMENT ON COLUMN t_liteflow_chain.enable IS '是否啟用';

CREATE INDEX IF NOT EXISTS idx_liteflow_chain_app_enable ON t_liteflow_chain (application_name, enable);

-- ---------------------------------------------------------------------
-- 2.2: t_liteflow_script (LiteFlow Script Node Definitions)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_liteflow_script (
    script_id           BIGSERIAL       PRIMARY KEY,
    application_name    VARCHAR(64)     NOT NULL,
    script_id_name      VARCHAR(64)     NOT NULL,
    script_name         VARCHAR(64),
    script_data         TEXT            NOT NULL,
    script_type         VARCHAR(16)     NOT NULL,
    script_language     VARCHAR(16),
    enable              BOOLEAN         NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_liteflow_script_app_id UNIQUE (application_name, script_id_name)
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow 腳本節點定義表 (System-level config, isolated DB connection, RLS disabled)';
COMMENT ON COLUMN t_liteflow_script.script_id IS '腳本節點唯一標識';
COMMENT ON COLUMN t_liteflow_script.application_name IS '應用標識';
COMMENT ON COLUMN t_liteflow_script.script_id_name IS '腳本節點 ID (EL 中引用的節點名稱)';
COMMENT ON COLUMN t_liteflow_script.script_name IS '腳本節點顯示名稱';
COMMENT ON COLUMN t_liteflow_script.script_data IS '腳本內容 (Groovy, Java, JavaScript 等)';
COMMENT ON COLUMN t_liteflow_script.script_type IS '腳本類型 (groovy, java, js 等)';
COMMENT ON COLUMN t_liteflow_script.enable IS '是否啟用';

CREATE INDEX IF NOT EXISTS idx_liteflow_script_app_enable ON t_liteflow_script (application_name, enable);

-- =====================================================================
-- PART 3: LiteFlow Initial Chain Data (V25)
-- =====================================================================

-- Insert the main turnover calculation chain
INSERT INTO t_liteflow_chain (application_name, chain_name, chain_desc, el_data, enable, create_time)
VALUES (
    'smartadmin-igaming-activity',
    'turnover_calculation_main',
    '有效投注額計算主流程 (Turnover Calculation Main Flow)',
    'THEN(fetchGameRound, calculateGameWeight, calculateStatusFactor, calculateOddsFilter, calculateRiskAdjustment, saveTurnoverResult)',
    TRUE,
    NOW()
)
ON CONFLICT (application_name, chain_name) DO NOTHING;

-- =====================================================================
-- PART 4: Grant Permissions to Application Role
-- =====================================================================
-- Note: RLS is NOT enabled, but we still grant standard CRUD permissions

GRANT SELECT, INSERT, UPDATE, DELETE ON t_turnover_game_weight_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_turnover_status_factor_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_turnover_odds_threshold_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_turnover_risk_action_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_turnover_rule_change_log TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_liteflow_chain TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_liteflow_script TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_turnover_game_weight_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_turnover_status_factor_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_turnover_odds_threshold_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_turnover_risk_action_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_turnover_rule_change_log_log_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_liteflow_chain_chain_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_liteflow_script_script_id_seq TO smartadmin_app;

-- =====================================================================
-- Migration Complete: V4 Business Rules Engine
-- =====================================================================
-- Total tables created: 7
-- RLS Status: DISABLED (intentionally, for system-level configuration tables)
-- Optimization: Avoided enable-then-disable RLS cycle from original V24-V27
-- V30 Fix: Integrated risk_level constraint fix (4 levels instead of 3)
-- =====================================================================
