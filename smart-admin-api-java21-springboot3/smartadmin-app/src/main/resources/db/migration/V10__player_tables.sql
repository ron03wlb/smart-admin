-- =====================================================================
-- V10: Player Tables — Phase 2 (02-player-design.md)
-- =====================================================================
-- Creates 4 tables:
--   - t_player: Master player table (PII encrypted)
--   - t_kyc_document: KYC document verification records
--   - t_vip_change_log: VIP level change audit log
--   - t_player_audit_log: Player status change audit log
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
--   - PII fields: AES-256-GCM encrypted + HMAC-SHA256 blind index
-- =====================================================================

-- =====================================================================
-- Part A: t_player (Master Player)
-- =====================================================================

CREATE TABLE t_player (
    player_id           BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    username            VARCHAR(50)     NOT NULL,
    password_hash       TEXT            NOT NULL,
    email_encrypted     TEXT,
    email_blind_idx     VARCHAR(64),
    phone_encrypted     TEXT,
    phone_blind_idx     VARCHAR(64),
    status              SMALLINT        NOT NULL DEFAULT 1,
    kyc_level           SMALLINT        NOT NULL DEFAULT 0,
    vip_level           SMALLINT        NOT NULL DEFAULT 1,
    registration_ip     VARCHAR(45),
    last_login_ip       VARCHAR(45),
    last_login_time     TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_player_status CHECK (status IN (1, 2, 3, 4, 5)),
    CONSTRAINT ck_player_kyc_level CHECK (kyc_level IN (0, 1, 2)),
    CONSTRAINT ck_player_vip_level CHECK (vip_level IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_player IS '玩家主表';
COMMENT ON COLUMN t_player.player_id IS '玩家唯一標識';
COMMENT ON COLUMN t_player.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_player.username IS '用戶名 (租戶內唯一)';
COMMENT ON COLUMN t_player.password_hash IS '密碼雜湊 (Argon2id)';
COMMENT ON COLUMN t_player.email_encrypted IS '電子郵件 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_player.email_blind_idx IS '電子郵件盲索引 (HMAC-SHA256)';
COMMENT ON COLUMN t_player.phone_encrypted IS '手機號碼 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_player.phone_blind_idx IS '手機號碼盲索引 (HMAC-SHA256)';
COMMENT ON COLUMN t_player.status IS '狀態: 1=ACTIVE, 2=LOCKED, 3=SUSPENDED, 4=PENDING_VERIFICATION, 5=CLOSED';
COMMENT ON COLUMN t_player.kyc_level IS 'KYC 等級: 0=L0, 1=L1, 2=L2';
COMMENT ON COLUMN t_player.vip_level IS 'VIP 等級: 1=BRONZE, 2=SILVER, 3=GOLD, 4=PLATINUM, 5=DIAMOND';
COMMENT ON COLUMN t_player.registration_ip IS '註冊 IP';
COMMENT ON COLUMN t_player.last_login_ip IS '最後登入 IP';
COMMENT ON COLUMN t_player.last_login_time IS '最後登入時間';
COMMENT ON COLUMN t_player.deleted IS '軟刪除標記';
COMMENT ON COLUMN t_player.version IS '樂觀鎖版本號';

CREATE UNIQUE INDEX uk_player_username_tenant ON t_player (tenant_id, username) WHERE deleted = FALSE;
CREATE INDEX idx_player_email_blind ON t_player (email_blind_idx) WHERE deleted = FALSE;
CREATE INDEX idx_player_phone_blind ON t_player (phone_blind_idx) WHERE deleted = FALSE;
CREATE INDEX idx_player_tenant ON t_player (tenant_id);
CREATE INDEX idx_player_status ON t_player (tenant_id, status);

-- =====================================================================
-- Part B: t_kyc_document (KYC Document Verification)
-- =====================================================================

CREATE TABLE t_kyc_document (
    kyc_document_id     BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    player_id           BIGINT          NOT NULL REFERENCES t_player(player_id),
    document_type       SMALLINT        NOT NULL,
    document_url        VARCHAR(500),
    verification_status SMALLINT        NOT NULL DEFAULT 1,
    reviewer_comment    VARCHAR(500),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_kyc_doc_type CHECK (document_type IN (1, 2, 3)),
    CONSTRAINT ck_kyc_verification_status CHECK (verification_status IN (1, 2, 3))
);

COMMENT ON TABLE t_kyc_document IS 'KYC 文件審核記錄';
COMMENT ON COLUMN t_kyc_document.kyc_document_id IS 'KYC 文件唯一標識';
COMMENT ON COLUMN t_kyc_document.player_id IS '玩家 ID';
COMMENT ON COLUMN t_kyc_document.document_type IS '文件類型: 1=身分證, 2=護照, 3=駕照';
COMMENT ON COLUMN t_kyc_document.document_url IS '文件 URL';
COMMENT ON COLUMN t_kyc_document.verification_status IS '審核狀態: 1=待審核, 2=通過, 3=拒絕';
COMMENT ON COLUMN t_kyc_document.reviewer_comment IS '審核備註';

CREATE INDEX idx_kyc_doc_player ON t_kyc_document (player_id);
CREATE INDEX idx_kyc_doc_tenant ON t_kyc_document (tenant_id);

-- =====================================================================
-- Part C: t_vip_change_log (VIP Level Change Audit)
-- =====================================================================

CREATE TABLE t_vip_change_log (
    log_id              BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    player_id           BIGINT          NOT NULL,
    old_level           SMALLINT        NOT NULL,
    new_level           SMALLINT        NOT NULL,
    reason              VARCHAR(200),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_vip_change_log IS 'VIP 等級變更日誌';
COMMENT ON COLUMN t_vip_change_log.log_id IS '日誌唯一標識';
COMMENT ON COLUMN t_vip_change_log.player_id IS '玩家 ID';
COMMENT ON COLUMN t_vip_change_log.old_level IS '舊 VIP 等級';
COMMENT ON COLUMN t_vip_change_log.new_level IS '新 VIP 等級';
COMMENT ON COLUMN t_vip_change_log.reason IS '變更原因';

CREATE INDEX idx_vip_log_player ON t_vip_change_log (player_id, create_time DESC);
CREATE INDEX idx_vip_log_tenant ON t_vip_change_log (tenant_id);

-- =====================================================================
-- Part D: t_player_audit_log (Player Status Change Audit)
-- =====================================================================

CREATE TABLE t_player_audit_log (
    audit_id            BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    player_id           BIGINT          NOT NULL,
    old_status          SMALLINT        NOT NULL,
    new_status          SMALLINT        NOT NULL,
    operator            VARCHAR(50),
    reason              VARCHAR(500),
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_player_audit_log IS '玩家狀態變更審計日誌';
COMMENT ON COLUMN t_player_audit_log.audit_id IS '審計唯一標識';
COMMENT ON COLUMN t_player_audit_log.player_id IS '玩家 ID';
COMMENT ON COLUMN t_player_audit_log.old_status IS '舊狀態';
COMMENT ON COLUMN t_player_audit_log.new_status IS '新狀態';
COMMENT ON COLUMN t_player_audit_log.operator IS '操作者 (admin username / system)';
COMMENT ON COLUMN t_player_audit_log.reason IS '變更原因';

CREATE INDEX idx_player_audit_player ON t_player_audit_log (player_id, create_time DESC);
CREATE INDEX idx_player_audit_tenant ON t_player_audit_log (tenant_id);

-- =====================================================================
-- Part E: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_player ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_kyc_document ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_vip_change_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_player_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_player
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_kyc_document
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_vip_change_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_player_audit_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part F: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_player TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_kyc_document TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_vip_change_log TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_player_audit_log TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_player_player_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_kyc_document_kyc_document_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_vip_change_log_log_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_player_audit_log_audit_id_seq TO smartadmin_app;
