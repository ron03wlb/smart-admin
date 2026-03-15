-- =====================================================================
-- V16: Payment Reconciliation Tables — Sprint 3 Wallet Production
-- =====================================================================
-- Adds reconciliation infrastructure for payment module:
--   - reconciliation_status column on t_payment_order
--   - t_payment_reconciliation: daily per-PSP reconciliation summary
--   - t_reconciliation_exception: discrepancy records
-- =====================================================================

-- =====================================================================
-- Part A: Add reconciliation_status to existing t_payment_order
-- =====================================================================

ALTER TABLE t_payment_order ADD COLUMN IF NOT EXISTS reconciliation_status SMALLINT NOT NULL DEFAULT 1;
COMMENT ON COLUMN t_payment_order.reconciliation_status IS '對帳狀態: 1=PENDING, 2=VERIFIED, 3=MISMATCH, 4=COMPENSATED, 5=RECONCILED';

CREATE INDEX IF NOT EXISTS idx_payment_order_recon_status ON t_payment_order (reconciliation_status);

-- =====================================================================
-- Part B: t_payment_reconciliation (Daily per-PSP Summary)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_payment_reconciliation (
    reconciliation_id           BIGSERIAL       PRIMARY KEY,
    tenant_id                   BIGINT          NOT NULL,
    reconciliation_date         DATE            NOT NULL,
    psp_code                    VARCHAR(50)     NOT NULL,
    total_psp_transactions      INT             NOT NULL DEFAULT 0,
    total_platform_transactions INT             NOT NULL DEFAULT 0,
    matched_count               INT             NOT NULL DEFAULT 0,
    mismatch_count              INT             NOT NULL DEFAULT 0,
    missing_count               INT             NOT NULL DEFAULT 0,
    extra_count                 INT             NOT NULL DEFAULT 0,
    total_psp_amount            DECIMAL(19,4),
    total_platform_amount       DECIMAL(19,4),
    amount_difference           DECIMAL(19,4),
    status                      SMALLINT        NOT NULL DEFAULT 1,
    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_pay_recon_date_psp_tenant UNIQUE (reconciliation_date, psp_code, tenant_id)
);

COMMENT ON TABLE t_payment_reconciliation IS '支付對帳匯總 (每日/每 PSP)';
COMMENT ON COLUMN t_payment_reconciliation.reconciliation_id IS '對帳記錄唯一標識';
COMMENT ON COLUMN t_payment_reconciliation.reconciliation_date IS '對帳日期';
COMMENT ON COLUMN t_payment_reconciliation.psp_code IS 'PSP 識別碼';
COMMENT ON COLUMN t_payment_reconciliation.total_psp_transactions IS 'PSP 端交易筆數';
COMMENT ON COLUMN t_payment_reconciliation.total_platform_transactions IS '平台端交易筆數';
COMMENT ON COLUMN t_payment_reconciliation.matched_count IS '匹配筆數';
COMMENT ON COLUMN t_payment_reconciliation.mismatch_count IS '不符筆數';
COMMENT ON COLUMN t_payment_reconciliation.missing_count IS '缺失筆數';
COMMENT ON COLUMN t_payment_reconciliation.extra_count IS '多餘筆數';
COMMENT ON COLUMN t_payment_reconciliation.total_psp_amount IS 'PSP 端總金額';
COMMENT ON COLUMN t_payment_reconciliation.total_platform_amount IS '平台端總金額';
COMMENT ON COLUMN t_payment_reconciliation.amount_difference IS '金額差異';
COMMENT ON COLUMN t_payment_reconciliation.status IS '對帳狀態: ReconciliationStatusEnum';

CREATE INDEX IF NOT EXISTS idx_pay_recon_tenant ON t_payment_reconciliation (tenant_id);
CREATE INDEX IF NOT EXISTS idx_pay_recon_date ON t_payment_reconciliation (reconciliation_date DESC);

-- =====================================================================
-- Part C: t_reconciliation_exception (Discrepancy Records)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_reconciliation_exception (
    exception_id        BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    reconciliation_id   BIGINT          REFERENCES t_payment_reconciliation(reconciliation_id),
    order_no            VARCHAR(100)    NOT NULL,
    exception_type      SMALLINT        NOT NULL,
    platform_amount     DECIMAL(19,4),
    psp_amount          DECIMAL(19,4),
    difference_amount   DECIMAL(19,4),
    resolution_status   SMALLINT        NOT NULL DEFAULT 1,
    notes               TEXT,
    resolved_at         TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_reconciliation_exception IS '對帳異常記錄';
COMMENT ON COLUMN t_reconciliation_exception.exception_id IS '異常記錄唯一標識';
COMMENT ON COLUMN t_reconciliation_exception.reconciliation_id IS '關聯對帳匯總 ID';
COMMENT ON COLUMN t_reconciliation_exception.order_no IS '訂單號';
COMMENT ON COLUMN t_reconciliation_exception.exception_type IS '異常類型: 1=金額不符, 2=平台缺失, 3=PSP缺失, 4=狀態不符';
COMMENT ON COLUMN t_reconciliation_exception.platform_amount IS '平台端金額';
COMMENT ON COLUMN t_reconciliation_exception.psp_amount IS 'PSP 端金額';
COMMENT ON COLUMN t_reconciliation_exception.difference_amount IS '差異金額';
COMMENT ON COLUMN t_reconciliation_exception.resolution_status IS '處理狀態: 1=PENDING, 2=INVESTIGATING, 3=RESOLVED, 4=WRITE_OFF';
COMMENT ON COLUMN t_reconciliation_exception.notes IS '備註';
COMMENT ON COLUMN t_reconciliation_exception.resolved_at IS '解決時間';

CREATE INDEX IF NOT EXISTS idx_recon_exc_order ON t_reconciliation_exception (order_no);
CREATE INDEX IF NOT EXISTS idx_recon_exc_tenant ON t_reconciliation_exception (tenant_id);
CREATE INDEX IF NOT EXISTS idx_recon_exc_recon_id ON t_reconciliation_exception (reconciliation_id);

-- =====================================================================
-- Part D: RLS Policies
-- =====================================================================

ALTER TABLE t_payment_reconciliation ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_reconciliation_exception ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_payment_reconciliation
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_reconciliation_exception
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part E: Grant Permissions
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_payment_reconciliation TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_reconciliation_exception TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_payment_reconciliation_reconciliation_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_reconciliation_exception_exception_id_seq TO smartadmin_app;
