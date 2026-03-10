-- =====================================================================
-- V23: Withdrawal Approval Workflow Tables — Phase 1 (V23 Evaluation)
-- =====================================================================
-- Creates withdrawal approval workflow infrastructure:
--   - t_withdrawal_approval: Multi-level approval records (L1/L2/L3)
--   - t_approval_delegation: Approval delegation tracking (optional)
--
-- Business Requirements:
--   - Global Compliance: UKGC (96.3% auto-approve), MGA (€2,000 threshold), PAGCOR (3-day KYC)
--   - Multi-Level Approval: Risk-based routing (0-30=Auto, 31-50=L1, 51-70=L1+L2, 71-100=L1+L2+L3)
--   - SAGA Workflow: Risk Assessment → KYC Verification → Approval Routing → Payment Execution
--   - Compensation Flow: Step failure compensation (fund release, retry, manual review)
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
--
-- Reference:
--   - Evaluation Report: docs/iGaming/implementation/V23-withdrawal-approval-evaluation.md
--   - Business Requirements: docs/iGaming/source-archive/01_Player_Center/01-05_Withdrawal_Risk.md
--   - Payment Design: docs/iGaming/implementation/01-payment-design.md
-- =====================================================================

-- =====================================================================
-- Part A: t_withdrawal_approval (Withdrawal Approval Records)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_withdrawal_approval (
    approval_id             BIGSERIAL       PRIMARY KEY,
    payment_order_id        BIGINT          NOT NULL REFERENCES t_payment_order(payment_order_id),
    tenant_id               BIGINT          NOT NULL,

    -- Approval Stage & Status
    approval_stage          SMALLINT        NOT NULL,  -- 1=L1, 2=L2, 3=L3, 4=AUTO_APPROVED
    approval_status         SMALLINT        NOT NULL DEFAULT 1,  -- 1=PENDING, 2=APPROVED, 3=REJECTED, 4=TIMEOUT, 5=DELEGATED

    -- Approver Information
    approver_id             BIGINT,  -- FK to t_employee (SmartAdmin user table)
    assigned_at             TIMESTAMPTZ,
    reviewed_at             TIMESTAMPTZ,
    approval_notes          TEXT,
    rejection_reason        VARCHAR(500),

    -- Risk Assessment Results
    risk_score              INT,  -- 0-100
    risk_category           VARCHAR(20),  -- LOW, MEDIUM, MEDIUM_HIGH, HIGH
    routing_reason          VARCHAR(200),  -- Reason for routing to this approval level

    -- KYC/AML Check Results
    kyc_status              SMALLINT,  -- KycVerificationStatusEnum: 1=PENDING, 2=APPROVED, 3=REJECTED
    kyc_level               SMALLINT,  -- KycLevelEnum: 0=L0, 1=L1, 2=L2
    aml_check_result        VARCHAR(50),  -- PASS, SUSPICIOUS, BLOCKED
    sanctions_screening     VARCHAR(50),  -- CLEAR, MATCH, PENDING
    pep_check_result        VARCHAR(50),  -- NOT_PEP, PEP_DOMESTIC, PEP_FOREIGN

    -- SAGA Workflow State
    saga_state              VARCHAR(50),  -- RISK_ASSESSMENT, KYC_VERIFICATION, RISK_PROPOSAL_CHECK, APPROVAL_ROUTING, PAYMENT_EXECUTION, COMPLETED
    compensation_state      VARCHAR(50),  -- NONE, COMPENSATING, COMPENSATED, COMPENSATION_FAILED
    retry_count             INT             NOT NULL DEFAULT 0,
    last_retry_at           TIMESTAMPTZ,

    -- SLA Tracking
    sla_deadline            TIMESTAMPTZ,  -- 24h auto-approve/reject deadline
    sla_breached            BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Extended Metadata (JSON)
    approval_metadata       JSONB,  -- Stores device fingerprint, IP, risk model version, etc.

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_approval_order_stage UNIQUE (payment_order_id, approval_stage),
    CONSTRAINT ck_approval_stage CHECK (approval_stage IN (1, 2, 3, 4)),
    CONSTRAINT ck_approval_status CHECK (approval_status IN (1, 2, 3, 4, 5)),
    CONSTRAINT ck_risk_score_range CHECK (risk_score >= 0 AND risk_score <= 100)
);

COMMENT ON TABLE t_withdrawal_approval IS '提款審批記錄 (支持多級審批鏈 L1/L2/L3)';
COMMENT ON COLUMN t_withdrawal_approval.approval_id IS '審批記錄唯一標識';
COMMENT ON COLUMN t_withdrawal_approval.payment_order_id IS '關聯支付訂單 ID (FK to t_payment_order)';
COMMENT ON COLUMN t_withdrawal_approval.approval_stage IS '審批階段: 1=L1 (Junior Analyst), 2=L2 (Senior Analyst), 3=L3 (Manager), 4=AUTO_APPROVED';
COMMENT ON COLUMN t_withdrawal_approval.approval_status IS '審批狀態: 1=PENDING (待審批), 2=APPROVED (已批准), 3=REJECTED (已拒絕), 4=TIMEOUT (超時自動決策), 5=DELEGATED (已委派)';
COMMENT ON COLUMN t_withdrawal_approval.approver_id IS '審批員 ID (FK to t_employee)';
COMMENT ON COLUMN t_withdrawal_approval.assigned_at IS '分配時間 (審批任務分配給審批員的時間)';
COMMENT ON COLUMN t_withdrawal_approval.reviewed_at IS '審核完成時間';
COMMENT ON COLUMN t_withdrawal_approval.approval_notes IS '審批備註 (審批員填寫的決策理由)';
COMMENT ON COLUMN t_withdrawal_approval.rejection_reason IS '拒絕原因 (審批員拒絕時必填)';
COMMENT ON COLUMN t_withdrawal_approval.risk_score IS '風險評分 (0-100, 由風險評估服務計算)';
COMMENT ON COLUMN t_withdrawal_approval.risk_category IS '風險類別: LOW (0-30), MEDIUM (31-50), MEDIUM_HIGH (51-70), HIGH (71-100)';
COMMENT ON COLUMN t_withdrawal_approval.routing_reason IS '路由原因 (為什麼路由到此審批級別)';
COMMENT ON COLUMN t_withdrawal_approval.kyc_status IS 'KYC 驗證狀態: 1=PENDING, 2=APPROVED, 3=REJECTED';
COMMENT ON COLUMN t_withdrawal_approval.kyc_level IS 'KYC 等級: 0=L0 (未驗證), 1=L1 (基礎驗證), 2=L2 (強化驗證)';
COMMENT ON COLUMN t_withdrawal_approval.aml_check_result IS 'AML 檢查結果: PASS (通過), SUSPICIOUS (可疑), BLOCKED (封鎖)';
COMMENT ON COLUMN t_withdrawal_approval.sanctions_screening IS '制裁名單篩查: CLEAR (清白), MATCH (匹配), PENDING (待審核)';
COMMENT ON COLUMN t_withdrawal_approval.pep_check_result IS 'PEP 檢查結果: NOT_PEP (非政治人物), PEP_DOMESTIC (國內政治人物), PEP_FOREIGN (外國政治人物)';
COMMENT ON COLUMN t_withdrawal_approval.saga_state IS 'SAGA 工作流當前狀態: RISK_ASSESSMENT, KYC_VERIFICATION, RISK_PROPOSAL_CHECK, APPROVAL_ROUTING, PAYMENT_EXECUTION, COMPLETED';
COMMENT ON COLUMN t_withdrawal_approval.compensation_state IS '補償事務狀態: NONE (無補償), COMPENSATING (補償中), COMPENSATED (已補償), COMPENSATION_FAILED (補償失敗)';
COMMENT ON COLUMN t_withdrawal_approval.retry_count IS '重試次數 (SAGA 步驟失敗重試計數)';
COMMENT ON COLUMN t_withdrawal_approval.last_retry_at IS '最後重試時間';
COMMENT ON COLUMN t_withdrawal_approval.sla_deadline IS 'SLA 截止時間 (24h 自動審批/拒絕截止時間)';
COMMENT ON COLUMN t_withdrawal_approval.sla_breached IS 'SLA 是否違規 (TRUE=已超過 24h 截止時間)';
COMMENT ON COLUMN t_withdrawal_approval.approval_metadata IS '審批元數據 (JSON): 設備指紋、IP 地址、風險模型版本、額外風險因子等';

-- Indexes for Performance Optimization
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_order ON t_withdrawal_approval (payment_order_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_status_assigned ON t_withdrawal_approval (approval_status, assigned_at DESC) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_approver_status ON t_withdrawal_approval (approver_id, approval_status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_tenant_time ON t_withdrawal_approval (tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_sla ON t_withdrawal_approval (sla_deadline) WHERE sla_breached = FALSE AND approval_status = 1;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_saga_state ON t_withdrawal_approval (saga_state, compensation_state) WHERE deleted = FALSE;

-- =====================================================================
-- Part B: t_approval_delegation (Approval Delegation Records)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_approval_delegation (
    delegation_id       BIGSERIAL       PRIMARY KEY,
    approval_id         BIGINT          NOT NULL REFERENCES t_withdrawal_approval(approval_id),
    tenant_id           BIGINT          NOT NULL,

    -- Delegation Participants
    from_approver_id    BIGINT          NOT NULL,  -- Original approver
    to_approver_id      BIGINT          NOT NULL,  -- Delegate approver
    delegation_reason   VARCHAR(200),

    -- Delegation Timeline
    delegated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    effective_until     TIMESTAMPTZ,  -- Optional expiry time for temporary delegation

    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_approval_delegation IS '審批委派記錄 (審批員臨時授權/轉移審批任務)';
COMMENT ON COLUMN t_approval_delegation.delegation_id IS '委派記錄唯一標識';
COMMENT ON COLUMN t_approval_delegation.approval_id IS '關聯審批記錄 ID (FK to t_withdrawal_approval)';
COMMENT ON COLUMN t_approval_delegation.from_approver_id IS '原審批員 ID';
COMMENT ON COLUMN t_approval_delegation.to_approver_id IS '委派對象 ID';
COMMENT ON COLUMN t_approval_delegation.delegation_reason IS '委派原因 (例如: 休假、負載過高、專業領域不符)';
COMMENT ON COLUMN t_approval_delegation.delegated_at IS '委派時間';
COMMENT ON COLUMN t_approval_delegation.effective_until IS '委派有效期 (NULL=永久，否則為臨時委派)';

CREATE INDEX IF NOT EXISTS idx_approval_delegation_approval ON t_approval_delegation (approval_id);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_from_approver ON t_approval_delegation (from_approver_id, delegated_at DESC);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_to_approver ON t_approval_delegation (to_approver_id, delegated_at DESC);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_tenant ON t_approval_delegation (tenant_id);

-- =====================================================================
-- Part C: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_withdrawal_approval ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_approval_delegation ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_withdrawal_approval
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_approval_delegation
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part D: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_withdrawal_approval TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_approval_delegation TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_withdrawal_approval_approval_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_approval_delegation_delegation_id_seq TO smartadmin_app;

-- =====================================================================
-- Part E: Alter t_payment_order to Support Withdrawal Approval Workflow
-- =====================================================================
-- Add approval_required flag to indicate if this withdrawal needs manual approval

ALTER TABLE t_payment_order ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN t_payment_order.approval_required IS '是否需要人工審批 (TRUE=需要審批, FALSE=自動審批或存款訂單)';

CREATE INDEX IF NOT EXISTS idx_payment_order_approval_required ON t_payment_order (approval_required, status) WHERE order_type = 2 AND deleted = FALSE;

-- =====================================================================
-- Part F: Seed Data — Example Approval Configurations (Optional)
-- =====================================================================
-- This section is commented out as approval configurations are typically
-- managed through Admin UI or external configuration files.
-- Uncomment if you want to insert sample data for testing.

/*
-- Example: Auto-approve configuration for low-risk withdrawals
-- (This would typically be stored in a separate t_approval_config table)
INSERT INTO t_approval_config (tenant_id, config_key, config_value, description)
VALUES
    (1, 'withdrawal.auto_approve.max_amount', '1000.0000', 'Max withdrawal amount for auto-approval (USD)'),
    (1, 'withdrawal.auto_approve.max_risk_score', '30', 'Max risk score for auto-approval (0-100)'),
    (1, 'withdrawal.sla.timeout_hours', '24', 'SLA timeout for manual approval (hours)')
ON CONFLICT DO NOTHING;
*/
