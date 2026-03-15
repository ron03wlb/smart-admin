-- =====================================================================
-- V13: Risk Engine Tables — Phase 5 (Risk Module)
-- =====================================================================
-- Creates 5 tables:
--   - t_risk_rule_param: Risk rule parameter configuration
--   - t_risk_assessment: Per-transaction risk assessment log
--   - t_risk_score: Player cumulative risk profile
--   - t_risk_proposal: Risk review proposals (work orders)
--   - t_geo_restriction: Restricted jurisdiction list
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - BIGINT for tenant_id (Multi-Tenant G1)
-- =====================================================================

-- =====================================================================
-- Part A: t_risk_rule_param (Risk Rule Parameter Configuration)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_risk_rule_param (
    rule_param_id           BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    rule_type               SMALLINT        NOT NULL,
    rule_name               VARCHAR(128)    NOT NULL,
    rule_description        VARCHAR(512),
    threshold_value         DECIMAL(19,4),
    time_window_seconds     INT,
    max_count               INT,
    weight                  DECIMAL(8,4)    NOT NULL DEFAULT 1.0000,
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    params_json             JSONB,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_risk_rule_type CHECK (rule_type IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_risk_rule_param IS '風控規則參數配置表';
COMMENT ON COLUMN t_risk_rule_param.rule_param_id IS '規則參數唯一標識';
COMMENT ON COLUMN t_risk_rule_param.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_risk_rule_param.rule_type IS '規則類型: 1=速率, 2=金額, 3=設備, 4=行為, 5=地理';
COMMENT ON COLUMN t_risk_rule_param.rule_name IS '規則名稱';
COMMENT ON COLUMN t_risk_rule_param.rule_description IS '規則描述';
COMMENT ON COLUMN t_risk_rule_param.threshold_value IS '閾值 DECIMAL(19,4)';
COMMENT ON COLUMN t_risk_rule_param.time_window_seconds IS '時間窗口 (秒)';
COMMENT ON COLUMN t_risk_rule_param.max_count IS '最大次數';
COMMENT ON COLUMN t_risk_rule_param.weight IS '規則權重 DECIMAL(8,4)';
COMMENT ON COLUMN t_risk_rule_param.enabled IS '是否啟用';
COMMENT ON COLUMN t_risk_rule_param.params_json IS '額外參數 (JSONB)';
COMMENT ON COLUMN t_risk_rule_param.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_risk_rule_param.version IS '樂觀鎖版本號';

CREATE INDEX IF NOT EXISTS idx_risk_rule_tenant_type ON t_risk_rule_param (tenant_id, rule_type) WHERE deleted = FALSE;

-- =====================================================================
-- Part B: t_risk_assessment (Per-Transaction Risk Assessment Log)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_risk_assessment (
    assessment_id           BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    event_type              VARCHAR(64)     NOT NULL,
    event_id                VARCHAR(64)     NOT NULL,
    risk_score              DECIMAL(8,4)    NOT NULL DEFAULT 0,
    risk_level              SMALLINT        NOT NULL,
    decision                SMALLINT        NOT NULL,
    rule_results_json       JSONB,
    processing_time_ms      INT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_assessment_risk_level CHECK (risk_level IN (1, 2, 3, 4)),
    CONSTRAINT ck_assessment_decision CHECK (decision IN (1, 2, 3))
);

COMMENT ON TABLE t_risk_assessment IS '交易風險評估日誌表';
COMMENT ON COLUMN t_risk_assessment.assessment_id IS '評估唯一標識';
COMMENT ON COLUMN t_risk_assessment.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_risk_assessment.player_id IS '玩家 ID';
COMMENT ON COLUMN t_risk_assessment.event_type IS '觸發事件類型 (e.g. BET_PLACED, WITHDRAWAL_REQUESTED)';
COMMENT ON COLUMN t_risk_assessment.event_id IS '觸發事件 ID';
COMMENT ON COLUMN t_risk_assessment.risk_score IS '風險分數 (0-100)';
COMMENT ON COLUMN t_risk_assessment.risk_level IS '風險等級: 1=低, 2=中, 3=高, 4=極高';
COMMENT ON COLUMN t_risk_assessment.decision IS '決策: 1=自動通過, 2=待審核, 3=自動拒絕';
COMMENT ON COLUMN t_risk_assessment.rule_results_json IS '各規則執行結果 (JSONB)';
COMMENT ON COLUMN t_risk_assessment.processing_time_ms IS '處理耗時 (毫秒)';

CREATE INDEX IF NOT EXISTS idx_assessment_player ON t_risk_assessment (player_id);
CREATE INDEX IF NOT EXISTS idx_assessment_tenant_event ON t_risk_assessment (tenant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_assessment_created ON t_risk_assessment (create_time);

-- =====================================================================
-- Part C: t_risk_score (Player Cumulative Risk Profile)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_risk_score (
    risk_score_id           BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    cumulative_score        DECIMAL(8,4)    NOT NULL DEFAULT 0,
    risk_level              SMALLINT        NOT NULL DEFAULT 1,
    total_assessments       INT             NOT NULL DEFAULT 0,
    last_assessment_time    TIMESTAMPTZ,
    auto_locked             BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_score_risk_level CHECK (risk_level IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_risk_score IS '玩家累積風險檔案表';
COMMENT ON COLUMN t_risk_score.risk_score_id IS '風險檔案唯一標識';
COMMENT ON COLUMN t_risk_score.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_risk_score.player_id IS '玩家 ID';
COMMENT ON COLUMN t_risk_score.cumulative_score IS '累積風險分數 (加權移動平均)';
COMMENT ON COLUMN t_risk_score.risk_level IS '風險等級: 1=低, 2=中, 3=高, 4=極高';
COMMENT ON COLUMN t_risk_score.total_assessments IS '累計評估次數';
COMMENT ON COLUMN t_risk_score.last_assessment_time IS '最近一次評估時間';
COMMENT ON COLUMN t_risk_score.auto_locked IS '是否因風險自動凍結';
COMMENT ON COLUMN t_risk_score.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_score_player_tenant ON t_risk_score (player_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_risk_score_level ON t_risk_score (risk_level);

-- =====================================================================
-- Part D: t_risk_proposal (Risk Review Proposals)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_risk_proposal (
    proposal_id             BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    assessment_id           BIGINT          NOT NULL,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    priority                SMALLINT        NOT NULL DEFAULT 2,
    assignee                VARCHAR(64),
    review_comment          VARCHAR(1024),
    sla_deadline            TIMESTAMPTZ,
    resolved_at             TIMESTAMPTZ,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_proposal_status CHECK (status IN (1, 2, 3, 4, 5, 6)),
    CONSTRAINT ck_proposal_priority CHECK (priority IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_risk_proposal IS '風控審核工單表';
COMMENT ON COLUMN t_risk_proposal.proposal_id IS '工單唯一標識';
COMMENT ON COLUMN t_risk_proposal.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_risk_proposal.player_id IS '玩家 ID';
COMMENT ON COLUMN t_risk_proposal.assessment_id IS '關聯風險評估 ID';
COMMENT ON COLUMN t_risk_proposal.status IS '狀態: 1=待分配, 2=已分配, 3=已審核, 4=已批准, 5=已拒絕, 6=已升級';
COMMENT ON COLUMN t_risk_proposal.priority IS '優先級: 1=低, 2=中, 3=高, 4=極高';
COMMENT ON COLUMN t_risk_proposal.assignee IS '指派審核人';
COMMENT ON COLUMN t_risk_proposal.review_comment IS '審核意見';
COMMENT ON COLUMN t_risk_proposal.sla_deadline IS 'SLA 截止時間';
COMMENT ON COLUMN t_risk_proposal.resolved_at IS '解決時間';
COMMENT ON COLUMN t_risk_proposal.version IS '樂觀鎖版本號';

CREATE INDEX IF NOT EXISTS idx_proposal_tenant_status ON t_risk_proposal (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_proposal_player ON t_risk_proposal (player_id);
CREATE INDEX IF NOT EXISTS idx_proposal_sla ON t_risk_proposal (sla_deadline, status) WHERE status IN (1, 2);

-- =====================================================================
-- Part E: t_geo_restriction (Restricted Jurisdictions)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_geo_restriction (
    geo_restriction_id      BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    country_code            VARCHAR(3)      NOT NULL,
    country_name            VARCHAR(128)    NOT NULL,
    restriction_type        SMALLINT        NOT NULL DEFAULT 1,
    reason                  VARCHAR(512),
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_geo_restriction IS '受限司法管轄區表';
COMMENT ON COLUMN t_geo_restriction.geo_restriction_id IS '限制記錄唯一標識';
COMMENT ON COLUMN t_geo_restriction.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_geo_restriction.country_code IS 'ISO 3166-1 國家代碼';
COMMENT ON COLUMN t_geo_restriction.country_name IS '國家名稱';
COMMENT ON COLUMN t_geo_restriction.restriction_type IS '限制類型: 1=完全禁止, 2=僅限觀看';
COMMENT ON COLUMN t_geo_restriction.reason IS '限制原因';
COMMENT ON COLUMN t_geo_restriction.enabled IS '是否啟用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_geo_country_tenant ON t_geo_restriction (country_code, tenant_id);

-- =====================================================================
-- Part F: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_risk_rule_param ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_assessment ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_score ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_proposal ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_geo_restriction ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_risk_rule_param
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_risk_assessment
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_risk_score
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_risk_proposal
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_geo_restriction
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part G: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_risk_rule_param TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_risk_assessment TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_risk_score TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_risk_proposal TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_geo_restriction TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_risk_rule_param_rule_param_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_risk_assessment_assessment_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_risk_score_risk_score_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_risk_proposal_proposal_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_geo_restriction_geo_restriction_id_seq TO smartadmin_app;
