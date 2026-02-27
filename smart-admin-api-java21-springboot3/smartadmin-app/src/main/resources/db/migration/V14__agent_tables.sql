-- =====================================================================
-- V14: Agent Module Tables — Phase 6 (Agent + Credit Network)
-- =====================================================================
-- Creates 8 tables:
--   Credit Network:
--     - t_agent_credit: Agent credit limit tracking
--     - t_settlement_record: Weekly/monthly settlement records
--     - t_credit_allocation_audit: Immutable credit allocation audit trail
--   Affiliate System:
--     - t_affiliate_agent: Agent master data
--     - t_affiliate_hierarchy: Closure Table for agent tree
--     - t_affiliate_commission_plan: Commission tier configuration
--     - t_affiliate_commission_record: Commission settlement records
--     - t_affiliate_adjustment: Immutable adjustment ledger
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - BIGINT for tenant_id (Multi-Tenant G1)
--   - BIGSERIAL for PKs (matching existing iGaming tables)
-- =====================================================================

-- =====================================================================
-- Part A: t_agent_credit (Agent Credit Limit Tracking)
-- =====================================================================

CREATE TABLE t_agent_credit (
    agent_credit_id         BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    agent_id                BIGINT          NOT NULL,
    parent_id               BIGINT,
    credit_limit            DECIMAL(19,4)   NOT NULL DEFAULT 0,
    used_credit             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    allocated_to_children   DECIMAL(19,4)   NOT NULL DEFAULT 0,
    position_percent        DECIMAL(8,4)    NOT NULL DEFAULT 0,
    max_position            DECIMAL(8,4)    NOT NULL DEFAULT 100.0000,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    frozen_at               TIMESTAMPTZ,
    last_settlement_time    TIMESTAMPTZ,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_agent_credit_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_agent_credit IS '代理信用額度追蹤表';
COMMENT ON COLUMN t_agent_credit.agent_credit_id IS '信用記錄唯一標識';
COMMENT ON COLUMN t_agent_credit.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_agent_credit.agent_id IS '代理 ID';
COMMENT ON COLUMN t_agent_credit.parent_id IS '上級代理 ID (NULL = 頂級代理)';
COMMENT ON COLUMN t_agent_credit.credit_limit IS '信用額度上限 DECIMAL(19,4)';
COMMENT ON COLUMN t_agent_credit.used_credit IS '已使用信用額度 DECIMAL(19,4)';
COMMENT ON COLUMN t_agent_credit.allocated_to_children IS '已分配給下級的額度 DECIMAL(19,4)';
COMMENT ON COLUMN t_agent_credit.position_percent IS '持倉百分比 DECIMAL(8,4)';
COMMENT ON COLUMN t_agent_credit.max_position IS '最大持倉限制 DECIMAL(8,4)';
COMMENT ON COLUMN t_agent_credit.status IS '狀態: 1=活躍, 2=凍結, 3=停用';
COMMENT ON COLUMN t_agent_credit.frozen_at IS '凍結時間';
COMMENT ON COLUMN t_agent_credit.last_settlement_time IS '最近結算時間';
COMMENT ON COLUMN t_agent_credit.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_agent_credit.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX uk_agent_credit_agent_tenant ON t_agent_credit (agent_id, tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_agent_credit_parent ON t_agent_credit (parent_id, tenant_id) WHERE deleted = FALSE;

-- =====================================================================
-- Part B: t_settlement_record (Weekly/Monthly Settlement Records)
-- =====================================================================

CREATE TABLE t_settlement_record (
    settlement_record_id    BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    agent_id                BIGINT          NOT NULL,
    parent_id               BIGINT,
    settlement_week         VARCHAR(16)     NOT NULL,
    settlement_phase        SMALLINT        NOT NULL DEFAULT 1,
    player_loss             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    own_share               DECIMAL(19,4)   NOT NULL DEFAULT 0,
    to_parent               DECIMAL(19,4)   NOT NULL DEFAULT 0,
    to_platform             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    payment_status          SMALLINT        NOT NULL DEFAULT 1,
    payment_txn_id          VARCHAR(128),
    verified_at             TIMESTAMPTZ,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_settlement_phase CHECK (settlement_phase IN (1, 2, 3, 4)),
    CONSTRAINT ck_payment_status CHECK (payment_status IN (1, 2, 3))
);

COMMENT ON TABLE t_settlement_record IS '代理結算紀錄表';
COMMENT ON COLUMN t_settlement_record.settlement_record_id IS '結算紀錄唯一標識';
COMMENT ON COLUMN t_settlement_record.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_settlement_record.agent_id IS '代理 ID';
COMMENT ON COLUMN t_settlement_record.parent_id IS '上級代理 ID';
COMMENT ON COLUMN t_settlement_record.settlement_week IS '結算週期 (e.g. 2026-W09)';
COMMENT ON COLUMN t_settlement_record.settlement_phase IS '結算階段: 1=凍結計算, 2=收款, 3=驗證, 4=報表';
COMMENT ON COLUMN t_settlement_record.player_loss IS '玩家虧損 DECIMAL(19,4)';
COMMENT ON COLUMN t_settlement_record.own_share IS '自留份額 DECIMAL(19,4)';
COMMENT ON COLUMN t_settlement_record.to_parent IS '上繳上級 DECIMAL(19,4)';
COMMENT ON COLUMN t_settlement_record.to_platform IS '上繳平台 DECIMAL(19,4)';
COMMENT ON COLUMN t_settlement_record.payment_status IS '付款狀態: 1=待付, 2=已驗證, 3=逾期';
COMMENT ON COLUMN t_settlement_record.payment_txn_id IS '付款交易 ID';
COMMENT ON COLUMN t_settlement_record.verified_at IS '驗證時間';

CREATE UNIQUE INDEX uk_settlement_agent_week ON t_settlement_record (agent_id, settlement_week, tenant_id);
CREATE INDEX idx_settlement_week ON t_settlement_record (settlement_week, tenant_id);

-- =====================================================================
-- Part C: t_credit_allocation_audit (Immutable Audit Trail)
-- =====================================================================

CREATE TABLE t_credit_allocation_audit (
    audit_id                BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    parent_id               BIGINT          NOT NULL,
    child_id                BIGINT          NOT NULL,
    old_limit               DECIMAL(19,4)   NOT NULL,
    new_limit               DECIMAL(19,4)   NOT NULL,
    delta                   DECIMAL(19,4)   NOT NULL,
    old_position            DECIMAL(8,4),
    new_position            DECIMAL(8,4),
    reason                  VARCHAR(512),
    operator                VARCHAR(64)     NOT NULL,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_credit_allocation_audit IS '信用分配審計日誌表 (不可變)';
COMMENT ON COLUMN t_credit_allocation_audit.audit_id IS '審計記錄唯一標識';
COMMENT ON COLUMN t_credit_allocation_audit.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_credit_allocation_audit.parent_id IS '分配方 (上級代理) ID';
COMMENT ON COLUMN t_credit_allocation_audit.child_id IS '接收方 (下級代理) ID';
COMMENT ON COLUMN t_credit_allocation_audit.old_limit IS '原額度 DECIMAL(19,4)';
COMMENT ON COLUMN t_credit_allocation_audit.new_limit IS '新額度 DECIMAL(19,4)';
COMMENT ON COLUMN t_credit_allocation_audit.delta IS '變動量 DECIMAL(19,4)';
COMMENT ON COLUMN t_credit_allocation_audit.old_position IS '原持倉 %';
COMMENT ON COLUMN t_credit_allocation_audit.new_position IS '新持倉 %';
COMMENT ON COLUMN t_credit_allocation_audit.reason IS '操作原因';
COMMENT ON COLUMN t_credit_allocation_audit.operator IS '操作人';

CREATE INDEX idx_audit_parent ON t_credit_allocation_audit (parent_id, tenant_id);
CREATE INDEX idx_audit_child ON t_credit_allocation_audit (child_id, tenant_id);

-- =====================================================================
-- Part D: t_affiliate_agent (Agent Master Data)
-- =====================================================================

CREATE TABLE t_affiliate_agent (
    agent_id                BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    username                VARCHAR(64)     NOT NULL,
    parent_agent_id         BIGINT,
    hierarchy_path          VARCHAR(1024),
    agent_level             INT             NOT NULL DEFAULT 1,
    commission_plan_id      BIGINT,
    total_players           INT             NOT NULL DEFAULT 0,
    active_players          INT             NOT NULL DEFAULT 0,
    total_commission        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    referral_code           VARCHAR(32),
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_affiliate_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_affiliate_agent IS '代理主檔表';
COMMENT ON COLUMN t_affiliate_agent.agent_id IS '代理唯一標識';
COMMENT ON COLUMN t_affiliate_agent.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_affiliate_agent.username IS '代理帳號';
COMMENT ON COLUMN t_affiliate_agent.parent_agent_id IS '上級代理 ID (NULL = 頂級代理)';
COMMENT ON COLUMN t_affiliate_agent.hierarchy_path IS '層級路徑 (e.g. /1/5/12/)';
COMMENT ON COLUMN t_affiliate_agent.agent_level IS '代理層級 (1=一級代理)';
COMMENT ON COLUMN t_affiliate_agent.commission_plan_id IS '佣金計畫 ID';
COMMENT ON COLUMN t_affiliate_agent.total_players IS '累計綁定玩家數';
COMMENT ON COLUMN t_affiliate_agent.active_players IS '活躍玩家數';
COMMENT ON COLUMN t_affiliate_agent.total_commission IS '累計佣金 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_agent.status IS '狀態: 1=活躍, 2=凍結, 3=停用';
COMMENT ON COLUMN t_affiliate_agent.referral_code IS '推薦碼 (唯一)';
COMMENT ON COLUMN t_affiliate_agent.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_affiliate_agent.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX uk_affiliate_username_tenant ON t_affiliate_agent (username, tenant_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX uk_affiliate_referral_code ON t_affiliate_agent (referral_code, tenant_id) WHERE referral_code IS NOT NULL AND deleted = FALSE;
CREATE INDEX idx_affiliate_parent ON t_affiliate_agent (parent_agent_id, tenant_id) WHERE deleted = FALSE;

-- =====================================================================
-- Part E: t_affiliate_hierarchy (Closure Table for Agent Tree)
-- =====================================================================

CREATE TABLE t_affiliate_hierarchy (
    hierarchy_id            BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    ancestor_id             BIGINT          NOT NULL,
    descendant_id           BIGINT          NOT NULL,
    depth                   INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_affiliate_hierarchy IS '代理層級 Closure Table';
COMMENT ON COLUMN t_affiliate_hierarchy.hierarchy_id IS '層級記錄唯一標識';
COMMENT ON COLUMN t_affiliate_hierarchy.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_affiliate_hierarchy.ancestor_id IS '祖先代理 ID';
COMMENT ON COLUMN t_affiliate_hierarchy.descendant_id IS '後代代理 ID';
COMMENT ON COLUMN t_affiliate_hierarchy.depth IS '深度 (0=自身)';

CREATE UNIQUE INDEX uk_hierarchy_ancestor_descendant ON t_affiliate_hierarchy (ancestor_id, descendant_id, tenant_id);
CREATE INDEX idx_hierarchy_descendant ON t_affiliate_hierarchy (descendant_id, tenant_id);

-- =====================================================================
-- Part F: t_affiliate_commission_plan (Commission Tier Configuration)
-- =====================================================================

CREATE TABLE t_affiliate_commission_plan (
    plan_id                 BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    plan_name               VARCHAR(128)    NOT NULL,
    plan_type               SMALLINT        NOT NULL,
    tiers_json              JSONB           NOT NULL DEFAULT '[]'::jsonb,
    settlement_period       SMALLINT        NOT NULL DEFAULT 1,
    negative_carryover      BOOLEAN         NOT NULL DEFAULT FALSE,
    reset_threshold         DECIMAL(19,4),
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_plan_type CHECK (plan_type IN (1, 2, 3)),
    CONSTRAINT ck_settlement_period CHECK (settlement_period IN (1, 2))
);

COMMENT ON TABLE t_affiliate_commission_plan IS '佣金計畫配置表';
COMMENT ON COLUMN t_affiliate_commission_plan.plan_id IS '計畫唯一標識';
COMMENT ON COLUMN t_affiliate_commission_plan.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_affiliate_commission_plan.plan_name IS '計畫名稱';
COMMENT ON COLUMN t_affiliate_commission_plan.plan_type IS '計畫類型: 1=營收分成, 2=流水返佣, 3=CPA';
COMMENT ON COLUMN t_affiliate_commission_plan.tiers_json IS '階梯配置 (JSONB) e.g. [{"min":0,"max":100000,"rate":0.30}]';
COMMENT ON COLUMN t_affiliate_commission_plan.settlement_period IS '結算週期: 1=週結, 2=月結';
COMMENT ON COLUMN t_affiliate_commission_plan.negative_carryover IS '是否啟用負數結轉';
COMMENT ON COLUMN t_affiliate_commission_plan.reset_threshold IS '重置閾值 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_commission_plan.enabled IS '是否啟用';
COMMENT ON COLUMN t_affiliate_commission_plan.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_affiliate_commission_plan.version IS '樂觀鎖版本號';

CREATE INDEX idx_plan_tenant_type ON t_affiliate_commission_plan (tenant_id, plan_type) WHERE deleted = FALSE;

-- =====================================================================
-- Part G: t_affiliate_commission_record (Commission Settlement Records)
-- =====================================================================

CREATE TABLE t_affiliate_commission_record (
    record_id               BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    agent_id                BIGINT          NOT NULL,
    plan_id                 BIGINT,
    settlement_date         DATE            NOT NULL,
    gross_amount            DECIMAL(19,4)   NOT NULL DEFAULT 0,
    adjustment_amount       DECIMAL(19,4)   NOT NULL DEFAULT 0,
    carryover_amount        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    net_amount              DECIMAL(19,4)   NOT NULL DEFAULT 0,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    approved_by             VARCHAR(64),
    approved_at             TIMESTAMPTZ,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_commission_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_affiliate_commission_record IS '佣金結算紀錄表';
COMMENT ON COLUMN t_affiliate_commission_record.record_id IS '紀錄唯一標識';
COMMENT ON COLUMN t_affiliate_commission_record.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_affiliate_commission_record.agent_id IS '代理 ID';
COMMENT ON COLUMN t_affiliate_commission_record.plan_id IS '佣金計畫 ID';
COMMENT ON COLUMN t_affiliate_commission_record.settlement_date IS '結算日期';
COMMENT ON COLUMN t_affiliate_commission_record.gross_amount IS '毛佣金 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_commission_record.adjustment_amount IS '調整金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_commission_record.carryover_amount IS '結轉金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_commission_record.net_amount IS '淨佣金 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_commission_record.status IS '狀態: 1=待審核, 2=已批准, 3=已拒絕';
COMMENT ON COLUMN t_affiliate_commission_record.approved_by IS '審核人';
COMMENT ON COLUMN t_affiliate_commission_record.approved_at IS '審核時間';

CREATE UNIQUE INDEX uk_commission_agent_date ON t_affiliate_commission_record (agent_id, settlement_date, tenant_id);
CREATE INDEX idx_commission_status ON t_affiliate_commission_record (tenant_id, status);

-- =====================================================================
-- Part H: t_affiliate_adjustment (Immutable Adjustment Ledger)
-- =====================================================================

CREATE TABLE t_affiliate_adjustment (
    adjustment_id           BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    agent_id                BIGINT          NOT NULL,
    adjustment_type         SMALLINT        NOT NULL,
    amount                  DECIMAL(19,4)   NOT NULL,
    original_settlement_date DATE,
    target_settlement_date  DATE,
    reason                  VARCHAR(512),
    created_by              VARCHAR(64)     NOT NULL,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_adjustment_type CHECK (adjustment_type IN (1, 2, 3))
);

COMMENT ON TABLE t_affiliate_adjustment IS '佣金調整帳本 (不可變)';
COMMENT ON COLUMN t_affiliate_adjustment.adjustment_id IS '調整記錄唯一標識';
COMMENT ON COLUMN t_affiliate_adjustment.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_affiliate_adjustment.agent_id IS '代理 ID';
COMMENT ON COLUMN t_affiliate_adjustment.adjustment_type IS '調整類型: 1=遲到入帳, 2=回滾, 3=手動調整';
COMMENT ON COLUMN t_affiliate_adjustment.amount IS '調整金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_affiliate_adjustment.original_settlement_date IS '原結算日期';
COMMENT ON COLUMN t_affiliate_adjustment.target_settlement_date IS '目標結算日期';
COMMENT ON COLUMN t_affiliate_adjustment.reason IS '調整原因';
COMMENT ON COLUMN t_affiliate_adjustment.created_by IS '建立人';

CREATE INDEX idx_adjustment_agent ON t_affiliate_adjustment (agent_id, tenant_id);

-- =====================================================================
-- Part I: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_agent_credit ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_settlement_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_credit_allocation_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_agent ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_hierarchy ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_commission_plan ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_commission_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_adjustment ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_agent_credit
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_settlement_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_credit_allocation_audit
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_affiliate_agent
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_affiliate_hierarchy
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_affiliate_commission_plan
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_affiliate_commission_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_affiliate_adjustment
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part J: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_agent_credit TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_settlement_record TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_credit_allocation_audit TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_affiliate_agent TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_affiliate_hierarchy TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_affiliate_commission_plan TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_affiliate_commission_record TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_affiliate_adjustment TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_agent_credit_agent_credit_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_settlement_record_settlement_record_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_credit_allocation_audit_audit_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_affiliate_agent_agent_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_affiliate_hierarchy_hierarchy_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_affiliate_commission_plan_plan_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_affiliate_commission_record_record_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_affiliate_adjustment_adjustment_id_seq TO smartadmin_app;
