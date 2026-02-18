-- =====================================================================
-- V9: Payment Gateway Tables — Phase 1.5 (01-payment-design.md)
-- =====================================================================
-- Creates 2 tables:
--   - t_payment_order: Unified deposit/withdrawal order
--   - t_psp: PSP (Payment Service Provider) configuration
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
-- =====================================================================

-- =====================================================================
-- Part A: t_payment_order (Unified Deposit/Withdrawal Order)
-- =====================================================================

CREATE TABLE t_payment_order (
    payment_order_id    BIGSERIAL       PRIMARY KEY,
    order_no            VARCHAR(64)     NOT NULL,
    player_id           BIGINT          NOT NULL,
    wallet_id           BIGINT          NOT NULL REFERENCES t_wallet(wallet_id),
    order_type          SMALLINT        NOT NULL,
    amount              DECIMAL(19,4)   NOT NULL,
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'USD',
    status              SMALLINT        NOT NULL DEFAULT 1,
    psp_code            VARCHAR(30)     NOT NULL,
    psp_transaction_id  VARCHAR(128),
    redirect_url        VARCHAR(500),
    callback_payload    TEXT,
    request_id          VARCHAR(64)     NOT NULL,
    description         VARCHAR(200),
    version             INT             NOT NULL DEFAULT 0,
    tenant_id           BIGINT          NOT NULL,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_payment_order_no UNIQUE (order_no),
    CONSTRAINT uk_payment_order_request UNIQUE (request_id),
    CONSTRAINT ck_payment_order_type CHECK (order_type IN (1, 2)),
    CONSTRAINT ck_payment_order_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT ck_payment_order_amount_positive CHECK (amount > 0)
);

COMMENT ON TABLE t_payment_order IS '支付訂單 (統一存款/提款)';
COMMENT ON COLUMN t_payment_order.payment_order_id IS '支付訂單唯一標識';
COMMENT ON COLUMN t_payment_order.order_no IS '平台訂單號';
COMMENT ON COLUMN t_payment_order.player_id IS '玩家 ID';
COMMENT ON COLUMN t_payment_order.wallet_id IS '關聯錢包 ID';
COMMENT ON COLUMN t_payment_order.order_type IS '訂單類型: 1=存款, 2=提款';
COMMENT ON COLUMN t_payment_order.amount IS '訂單金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_payment_order.currency_code IS '幣種代碼 (ISO 4217)';
COMMENT ON COLUMN t_payment_order.status IS '訂單狀態: 1=PENDING, 2=PROCESSING, 3=SUCCESS, 4=FAILED, 5=REJECTED, 6=REFUNDED, 7=CANCELLED';
COMMENT ON COLUMN t_payment_order.psp_code IS 'PSP 識別碼';
COMMENT ON COLUMN t_payment_order.psp_transaction_id IS 'PSP 端交易 ID';
COMMENT ON COLUMN t_payment_order.redirect_url IS '存款重定向 URL';
COMMENT ON COLUMN t_payment_order.callback_payload IS '回調原始 payload (審計用)';
COMMENT ON COLUMN t_payment_order.request_id IS '冪等鍵';
COMMENT ON COLUMN t_payment_order.version IS '樂觀鎖版本號';

CREATE INDEX idx_payment_order_player_time ON t_payment_order (player_id, create_time DESC);
CREATE INDEX idx_payment_order_status_time ON t_payment_order (status, create_time DESC);
CREATE INDEX idx_payment_order_psp_code ON t_payment_order (psp_code, create_time DESC);
CREATE INDEX idx_payment_order_tenant ON t_payment_order (tenant_id, create_time DESC);
CREATE INDEX idx_payment_order_wallet ON t_payment_order (wallet_id);

-- =====================================================================
-- Part B: t_psp (PSP Configuration)
-- =====================================================================

CREATE TABLE t_psp (
    psp_id                      BIGSERIAL       PRIMARY KEY,
    psp_code                    VARCHAR(30)     NOT NULL,
    psp_name                    VARCHAR(100)    NOT NULL,
    api_base_url                VARCHAR(255),
    api_key_encrypted           TEXT,
    webhook_secret_encrypted    TEXT,
    enabled                     BOOLEAN         NOT NULL DEFAULT TRUE,
    priority                    INT             NOT NULL DEFAULT 100,
    supported_currencies        VARCHAR(200),
    min_deposit                 DECIMAL(19,4),
    max_deposit                 DECIMAL(19,4),
    min_withdrawal              DECIMAL(19,4),
    max_withdrawal              DECIMAL(19,4),
    tenant_id                   BIGINT          NOT NULL,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_psp_code_tenant UNIQUE (psp_code, tenant_id)
);

COMMENT ON TABLE t_psp IS 'PSP 配置表';
COMMENT ON COLUMN t_psp.psp_id IS 'PSP 唯一標識';
COMMENT ON COLUMN t_psp.psp_code IS 'PSP 識別碼 (stripe, nuvei, adyen, mock)';
COMMENT ON COLUMN t_psp.psp_name IS 'PSP 顯示名稱';
COMMENT ON COLUMN t_psp.api_base_url IS 'PSP API 基礎 URL';
COMMENT ON COLUMN t_psp.api_key_encrypted IS 'API Key (AES-256-GCM 加密)';
COMMENT ON COLUMN t_psp.webhook_secret_encrypted IS 'Webhook Secret (AES-256-GCM 加密)';
COMMENT ON COLUMN t_psp.enabled IS '是否啟用';
COMMENT ON COLUMN t_psp.priority IS '優先級 (越小越優先)';
COMMENT ON COLUMN t_psp.supported_currencies IS '支持幣種 (逗號分隔)';
COMMENT ON COLUMN t_psp.min_deposit IS '最低存款金額';
COMMENT ON COLUMN t_psp.max_deposit IS '最高存款金額';
COMMENT ON COLUMN t_psp.min_withdrawal IS '最低提款金額';
COMMENT ON COLUMN t_psp.max_withdrawal IS '最高提款金額';

CREATE INDEX idx_psp_tenant_enabled ON t_psp (tenant_id, enabled);

-- =====================================================================
-- Part C: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_payment_order ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_psp ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_payment_order
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_psp
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part D: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_payment_order TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_psp TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_payment_order_payment_order_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_psp_psp_id_seq TO smartadmin_app;

-- =====================================================================
-- Part E: Seed Data — MockPSP for testing
-- =====================================================================

INSERT INTO t_psp (psp_code, psp_name, api_base_url, enabled, priority, supported_currencies, min_deposit, max_deposit, min_withdrawal, max_withdrawal, tenant_id)
VALUES ('mock', 'Mock PSP (Testing)', 'http://localhost:9999', TRUE, 999, 'USD,EUR,GBP', 10.0000, 10000.0000, 20.0000, 50000.0000, 1);
