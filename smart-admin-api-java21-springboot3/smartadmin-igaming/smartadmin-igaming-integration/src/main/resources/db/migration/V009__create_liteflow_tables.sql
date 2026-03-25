-- =====================================================
-- LiteFlow Tables for Integration Testing
-- Aligned with LiteFlowChainEntity.java and LiteFlowScriptEntity.java
-- ⚠️ CRITICAL: Must match SmartAdmin entity field mappings
-- =====================================================

-- 1. LiteFlow Chain Table (規則鏈定義表)
-- Matches: LiteFlowChainEntity extends SmartAdminBaseEntity
CREATE TABLE IF NOT EXISTS t_liteflow_chain (
    chain_id            BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT,                         -- From SmartAdminBaseEntity
    chain_name          VARCHAR(64)     NOT NULL,
    chain_code          VARCHAR(64)     NOT NULL,       -- ⭐ CRITICAL: Used in WHERE clause
    chain_type          INTEGER         NOT NULL DEFAULT 1,  -- 1=普通 2=條件 3=循環
    chain_data          TEXT            NOT NULL,       -- EL expression
    version             INTEGER         NOT NULL DEFAULT 0,  -- Optimistic lock
    status              INTEGER         NOT NULL DEFAULT 1,  -- 0=禁用 1=啟用
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,  -- ⭐ CRITICAL: false=未刪除 true=已刪除
    remark              VARCHAR(512),
    create_user_id      BIGINT,
    create_user_name    VARCHAR(64),
    update_user_id      BIGINT,
    update_user_name    VARCHAR(64),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),  -- From SmartAdminBaseEntity
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()   -- From SmartAdminBaseEntity
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow 規則鏈定義表 (aligned with LiteFlowChainEntity)';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '規則鏈唯一標識';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '規則鏈名稱';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS '規則鏈編碼 (唯一業務鍵, used in DAO WHERE clause)';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '規則鏈類型: 1=普通 2=條件 3=循環';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'EL 表達式定義: THEN(a, b, c)';
COMMENT ON COLUMN t_liteflow_chain.deleted IS '刪除標記: 0=未刪除 1=已刪除';

-- Unique constraint on chain_code (business key)
CREATE UNIQUE INDEX IF NOT EXISTS uk_liteflow_chain_code ON t_liteflow_chain(tenant_id, chain_code) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_liteflow_chain_status ON t_liteflow_chain(tenant_id, status);

-- 2. LiteFlow Script Table (腳本節點定義表)
-- Matches: LiteFlowScriptEntity extends SmartAdminBaseEntity
CREATE TABLE IF NOT EXISTS t_liteflow_script (
    script_id           BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT,                         -- From SmartAdminBaseEntity
    script_id_name      VARCHAR(64)     NOT NULL,       -- Node ID referenced in EL
    script_name         VARCHAR(64),
    script_data         TEXT            NOT NULL,       -- Script content
    script_type         VARCHAR(16)     NOT NULL,       -- groovy, java, js, etc.
    script_language     VARCHAR(16),
    version             INTEGER         NOT NULL DEFAULT 0,  -- Optimistic lock
    status              INTEGER         NOT NULL DEFAULT 1,  -- 0=禁用 1=啟用
    deleted             BOOLEAN        NOT NULL DEFAULT FALSE,  -- false=未刪除 true=已刪除
    remark              VARCHAR(512),
    create_user_id      BIGINT,
    create_user_name    VARCHAR(64),
    update_user_id      BIGINT,
    update_user_name    VARCHAR(64),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),  -- From SmartAdminBaseEntity
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()   -- From SmartAdminBaseEntity
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow 腳本節點定義表 (aligned with LiteFlowScriptEntity)';
COMMENT ON COLUMN t_liteflow_script.script_id IS '腳本節點唯一標識';
COMMENT ON COLUMN t_liteflow_script.script_id_name IS '腳本節點 ID (EL 中引用的節點名稱)';
COMMENT ON COLUMN t_liteflow_script.script_data IS '腳本內容 (Groovy, Java, JavaScript 等)';
COMMENT ON COLUMN t_liteflow_script.deleted IS '刪除標記: 0=未刪除 1=已刪除';

-- Unique constraint on script_id_name (node ID)
CREATE UNIQUE INDEX IF NOT EXISTS uk_liteflow_script_id_name ON t_liteflow_script(tenant_id, script_id_name) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_liteflow_script_status ON t_liteflow_script(tenant_id, status);

-- Note: No seed data inserted here - TurnoverCalculationService creates chains dynamically
-- via chainManager.add() in @PostConstruct method with correct EL expressions
