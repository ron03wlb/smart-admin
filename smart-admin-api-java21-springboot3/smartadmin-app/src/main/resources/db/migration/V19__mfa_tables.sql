-- =====================================================================
-- V19: MFA Tables — Phase 3 (MFA Implementation Plan)
-- =====================================================================
-- Creates 4 tables:
--   - t_mfa_config: MFA configuration with encrypted TOTP secret
--   - t_mfa_backup_code: Backup codes for account recovery
--   - t_mfa_trusted_device: Trusted devices (30-day bypass)
--   - t_mfa_audit_log: MFA event audit logging
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
--   - Sensitive fields: AES-256-GCM encrypted (secret), Argon2id hashed (backup codes)
-- =====================================================================

-- =====================================================================
-- Part A: t_mfa_config (MFA Configuration)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_mfa_config (
    mfa_config_id       BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL,
    tenant_id           BIGINT,
    mfa_enabled         BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_type            VARCHAR(20)     NOT NULL DEFAULT 'TOTP',
    secret_encrypted    TEXT            NOT NULL,
    backup_codes_generated BOOLEAN      NOT NULL DEFAULT FALSE,
    qr_code_confirmed   BOOLEAN         NOT NULL DEFAULT FALSE,
    enforced_by_role    BOOLEAN         NOT NULL DEFAULT FALSE,
    last_verified_at    TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_mfa_config_employee_tenant UNIQUE (employee_id, tenant_id)
);

COMMENT ON TABLE t_mfa_config IS 'MFA 配置表';
COMMENT ON COLUMN t_mfa_config.mfa_config_id IS 'MFA 配置唯一標識';
COMMENT ON COLUMN t_mfa_config.employee_id IS '員工 ID';
COMMENT ON COLUMN t_mfa_config.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_mfa_config.mfa_enabled IS 'MFA 是否啟用';
COMMENT ON COLUMN t_mfa_config.mfa_type IS 'MFA 類型: TOTP, SMS, EMAIL';
COMMENT ON COLUMN t_mfa_config.secret_encrypted IS 'TOTP 密鑰 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_mfa_config.backup_codes_generated IS '備份碼是否已生成';
COMMENT ON COLUMN t_mfa_config.qr_code_confirmed IS 'QR 碼是否已確認';
COMMENT ON COLUMN t_mfa_config.enforced_by_role IS '是否由角色強制啟用';
COMMENT ON COLUMN t_mfa_config.last_verified_at IS '最後驗證時間';
COMMENT ON COLUMN t_mfa_config.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_mfa_config.version IS '樂觀鎖版本號';

CREATE INDEX IF NOT EXISTS idx_mfa_config_employee ON t_mfa_config (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_config_tenant ON t_mfa_config (tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_config_enabled ON t_mfa_config (employee_id, mfa_enabled) WHERE deleted = FALSE;

-- =====================================================================
-- Part B: t_mfa_backup_code (MFA Backup Codes)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_mfa_backup_code (
    backup_code_id      BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL,
    tenant_id           BIGINT,
    code_hash           VARCHAR(255)    NOT NULL,
    used                BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at             TIMESTAMPTZ,
    used_ip             VARCHAR(50),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_mfa_backup_code IS 'MFA 備份碼表';
COMMENT ON COLUMN t_mfa_backup_code.backup_code_id IS '備份碼唯一標識';
COMMENT ON COLUMN t_mfa_backup_code.employee_id IS '員工 ID';
COMMENT ON COLUMN t_mfa_backup_code.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_mfa_backup_code.code_hash IS '備份碼雜湊 (Argon2id)';
COMMENT ON COLUMN t_mfa_backup_code.used IS '是否已使用';
COMMENT ON COLUMN t_mfa_backup_code.used_at IS '使用時間';
COMMENT ON COLUMN t_mfa_backup_code.used_ip IS '使用 IP';
COMMENT ON COLUMN t_mfa_backup_code.deleted IS '軟刪除標記';

CREATE INDEX IF NOT EXISTS idx_mfa_backup_code_employee ON t_mfa_backup_code (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_backup_code_tenant ON t_mfa_backup_code (tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_backup_code_used ON t_mfa_backup_code (employee_id, used) WHERE deleted = FALSE;

-- =====================================================================
-- Part C: t_mfa_trusted_device (MFA Trusted Devices)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_mfa_trusted_device (
    device_id           BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL,
    tenant_id           BIGINT,
    device_fingerprint  VARCHAR(255)    NOT NULL,
    device_name         VARCHAR(100),
    ip_address          VARCHAR(50),
    user_agent          TEXT,
    trusted_until       TIMESTAMPTZ     NOT NULL,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_mfa_trusted_device IS 'MFA 信任設備表';
COMMENT ON COLUMN t_mfa_trusted_device.device_id IS '設備唯一標識';
COMMENT ON COLUMN t_mfa_trusted_device.employee_id IS '員工 ID';
COMMENT ON COLUMN t_mfa_trusted_device.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_mfa_trusted_device.device_fingerprint IS '設備指紋 (SHA256)';
COMMENT ON COLUMN t_mfa_trusted_device.device_name IS '設備名稱 (例如：我的 iPhone 15)';
COMMENT ON COLUMN t_mfa_trusted_device.ip_address IS 'IP 地址';
COMMENT ON COLUMN t_mfa_trusted_device.user_agent IS '用戶代理字串';
COMMENT ON COLUMN t_mfa_trusted_device.trusted_until IS '信任截止時間 (30天後)';
COMMENT ON COLUMN t_mfa_trusted_device.deleted IS '軟刪除標記';

CREATE INDEX IF NOT EXISTS idx_mfa_trusted_device_employee ON t_mfa_trusted_device (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_trusted_device_tenant ON t_mfa_trusted_device (tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_trusted_device_fingerprint ON t_mfa_trusted_device (employee_id, device_fingerprint) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_trusted_device_expiry ON t_mfa_trusted_device (trusted_until) WHERE deleted = FALSE;

-- =====================================================================
-- Part D: t_mfa_audit_log (MFA Audit Logging)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_mfa_audit_log (
    log_id              BIGSERIAL       PRIMARY KEY,
    employee_id         BIGINT          NOT NULL,
    tenant_id           BIGINT,
    event_type          VARCHAR(50)     NOT NULL,
    event_result        VARCHAR(20)     NOT NULL,
    severity            VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    ip_address          VARCHAR(50),
    user_agent          TEXT,
    error_message       VARCHAR(500),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_mfa_audit_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

COMMENT ON TABLE t_mfa_audit_log IS 'MFA 審計日誌表';
COMMENT ON COLUMN t_mfa_audit_log.log_id IS '日誌唯一標識';
COMMENT ON COLUMN t_mfa_audit_log.employee_id IS '員工 ID';
COMMENT ON COLUMN t_mfa_audit_log.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_mfa_audit_log.event_type IS '事件類型: MFA_ENABLED, MFA_DISABLED, MFA_VERIFY_SUCCESS, MFA_VERIFY_FAIL, BACKUP_CODE_USED, TRUSTED_DEVICE_ADDED, TRUSTED_DEVICE_REMOVED, ANOMALY_DETECTED, ACCOUNT_LOCKED, RECOVERY_INITIATED';
COMMENT ON COLUMN t_mfa_audit_log.event_result IS '事件結果: SUCCESS, FAILURE';
COMMENT ON COLUMN t_mfa_audit_log.severity IS '嚴重性: INFO, WARNING, CRITICAL';
COMMENT ON COLUMN t_mfa_audit_log.ip_address IS 'IP 地址';
COMMENT ON COLUMN t_mfa_audit_log.user_agent IS '用戶代理字串';
COMMENT ON COLUMN t_mfa_audit_log.error_message IS '錯誤訊息';
COMMENT ON COLUMN t_mfa_audit_log.deleted IS '軟刪除標記';

CREATE INDEX IF NOT EXISTS idx_mfa_audit_log_employee ON t_mfa_audit_log (employee_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_mfa_audit_log_tenant ON t_mfa_audit_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mfa_audit_log_severity ON t_mfa_audit_log (severity, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_mfa_audit_log_event_type ON t_mfa_audit_log (event_type, create_time DESC);

-- =====================================================================
-- Part E: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_mfa_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_backup_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_trusted_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_mfa_config
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL);

CREATE POLICY tenant_isolation ON t_mfa_backup_code
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL);

CREATE POLICY tenant_isolation ON t_mfa_trusted_device
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL);

CREATE POLICY tenant_isolation ON t_mfa_audit_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT OR tenant_id IS NULL);

-- =====================================================================
-- Part F: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_config TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_backup_code TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_trusted_device TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_audit_log TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_mfa_config_mfa_config_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_backup_code_backup_code_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_trusted_device_device_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_audit_log_log_id_seq TO smartadmin_app;
