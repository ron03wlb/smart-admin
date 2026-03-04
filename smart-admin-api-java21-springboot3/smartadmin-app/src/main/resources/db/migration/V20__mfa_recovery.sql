-- =====================================================================
-- V20: MFA Recovery Tables — Phase 3 Week 3 Day 3 (Device Loss Recovery)
-- =====================================================================
-- Creates 1 table:
--   - t_mfa_recovery_request: MFA device loss recovery requests
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
--   - Sensitive fields: Argon2id hashed (recovery_code_hash)
--
-- Business Rules:
--   - One PENDING request per employee (enforced by unique constraint)
--   - Recovery code expires after 24 hours
--   - Recovery code can only be used once
--   - Admin approval required (Super Admin role)
--   - CRITICAL audit logs for all recovery operations
-- =====================================================================

-- =====================================================================
-- Part A: t_mfa_recovery_request (MFA Recovery Requests)
-- =====================================================================

CREATE TABLE t_mfa_recovery_request (
    recovery_id         BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL,
    tenant_id           BIGINT,
    request_email       VARCHAR(255)    NOT NULL,
    request_reason      TEXT,
    request_ip          VARCHAR(50),
    status              SMALLINT        NOT NULL DEFAULT 1,
    approver_id         BIGINT,
    approved_at         TIMESTAMPTZ,
    rejection_reason    TEXT,
    recovery_code       VARCHAR(64),
    recovery_code_hash  VARCHAR(255),
    expires_at          TIMESTAMPTZ,
    used                BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at             TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_mfa_recovery_status CHECK (status IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_mfa_recovery_request IS 'MFA 恢復請求表';
COMMENT ON COLUMN t_mfa_recovery_request.recovery_id IS '恢復請求唯一標識';
COMMENT ON COLUMN t_mfa_recovery_request.employee_id IS '員工 ID';
COMMENT ON COLUMN t_mfa_recovery_request.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_mfa_recovery_request.request_email IS '請求郵箱';
COMMENT ON COLUMN t_mfa_recovery_request.request_reason IS '請求原因';
COMMENT ON COLUMN t_mfa_recovery_request.request_ip IS '請求 IP 地址';
COMMENT ON COLUMN t_mfa_recovery_request.status IS '狀態: 1=PENDING, 2=APPROVED, 3=REJECTED, 4=EXPIRED';
COMMENT ON COLUMN t_mfa_recovery_request.approver_id IS '審批人 ID';
COMMENT ON COLUMN t_mfa_recovery_request.approved_at IS '審批時間';
COMMENT ON COLUMN t_mfa_recovery_request.rejection_reason IS '拒絕原因';
COMMENT ON COLUMN t_mfa_recovery_request.recovery_code IS '臨時恢復碼 (明文, 僅用於 Email)';
COMMENT ON COLUMN t_mfa_recovery_request.recovery_code_hash IS '恢復碼雜湊 (Argon2id)';
COMMENT ON COLUMN t_mfa_recovery_request.expires_at IS '恢復碼過期時間 (審批後 24 小時)';
COMMENT ON COLUMN t_mfa_recovery_request.used IS '恢復碼是否已使用';
COMMENT ON COLUMN t_mfa_recovery_request.used_at IS '恢復碼使用時間';
COMMENT ON COLUMN t_mfa_recovery_request.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_mfa_recovery_request.version IS '樂觀鎖版本號';

CREATE INDEX idx_mfa_recovery_employee ON t_mfa_recovery_request (employee_id) WHERE deleted = FALSE;
CREATE INDEX idx_mfa_recovery_tenant ON t_mfa_recovery_request (tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_mfa_recovery_status ON t_mfa_recovery_request (status, create_time DESC) WHERE deleted = FALSE;
CREATE INDEX idx_mfa_recovery_expires ON t_mfa_recovery_request (expires_at) WHERE deleted = FALSE AND used = FALSE;
CREATE INDEX idx_mfa_recovery_approver ON t_mfa_recovery_request (approver_id, approved_at DESC) WHERE deleted = FALSE;

-- Partial unique index: One PENDING request per employee (active records only)
CREATE UNIQUE INDEX uk_mfa_recovery_employee_pending ON t_mfa_recovery_request (employee_id)
    WHERE deleted = FALSE AND status = 1;

-- =====================================================================
-- Part B: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_mfa_recovery_request ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_mfa_recovery_request
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL);

-- =====================================================================
-- Part C: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_recovery_request TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_recovery_request_recovery_id_seq TO smartadmin_app;
