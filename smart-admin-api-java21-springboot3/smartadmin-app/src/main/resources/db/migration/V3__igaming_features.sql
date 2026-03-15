-- =====================================================================
-- V3: iGaming Feature Extensions — Consolidation of V15-V20, V23
-- =====================================================================
-- This migration consolidates 7 feature enhancement migrations:
--   V15: Game round status extension
--   V16: Payment reconciliation tables (2 tables)
--   V17: iGaming SmartJob registrations (8 jobs)
--   V18: iGaming menu tree initialization (13 pages + 39 permissions)
--   V19: MFA tables (4 tables)
--   V20: MFA recovery table (1 table)
--   V23: Withdrawal approval workflow (2 tables + t_payment_order alterations)
--
-- Total New Tables: 9 tables
--   - t_payment_reconciliation_summary (V16)
--   - t_payment_reconciliation_detail (V16)
--   - t_mfa_device (V19)
--   - t_mfa_backup_code (V19)
--   - t_mfa_trusted_device (V19)
--   - t_mfa_session (V19)
--   - t_mfa_recovery_request (V20)
--   - t_withdrawal_approval (V23)
--   - t_approval_delegation (V23)
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - BIGINT for tenant_id (Multi-Tenant G1)
--   - All DDL uses IF NOT EXISTS for idempotency
-- =====================================================================

-- =====================================================================
-- PART 1: Game Round Status Extension (V15)
-- =====================================================================
-- Extend t_game_round.status constraint to allow 7 statuses
-- (Original V11 only allowed 4 statuses: 1=IN_PROGRESS, 2=SETTLED, 3=CANCELLED, 4=VOIDED)
-- New statuses: 5=REFUNDED, 6=DISPUTED, 7=COMPENSATED

-- Drop existing constraint
ALTER TABLE t_game_round DROP CONSTRAINT IF EXISTS ck_round_status;

-- Recreate constraint with 7 statuses
ALTER TABLE t_game_round ADD CONSTRAINT ck_round_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7));

COMMENT ON CONSTRAINT ck_round_status ON t_game_round IS '遊戲局狀態: 1=進行中, 2=已結算, 3=已取消, 4=已作廢, 5=已退款, 6=爭議中, 7=已補償';

-- =====================================================================
-- PART 2: Payment Reconciliation Tables (V16)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 2.1: t_payment_reconciliation_summary (Daily Payment Reconciliation Summary)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_payment_reconciliation_summary (
    summary_id                  BIGSERIAL       PRIMARY KEY,
    tenant_id                   BIGINT          NOT NULL,
    reconciliation_date         DATE            NOT NULL,
    psp_code                    VARCHAR(30)     NOT NULL,

    -- Deposit Summary
    total_deposit_count         INT             NOT NULL DEFAULT 0,
    total_deposit_amount        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    psp_deposit_count           INT             NOT NULL DEFAULT 0,
    psp_deposit_amount          DECIMAL(19,4)   NOT NULL DEFAULT 0,
    deposit_matched_count       INT             NOT NULL DEFAULT 0,
    deposit_mismatch_count      INT             NOT NULL DEFAULT 0,

    -- Withdrawal Summary
    total_withdrawal_count      INT             NOT NULL DEFAULT 0,
    total_withdrawal_amount     DECIMAL(19,4)   NOT NULL DEFAULT 0,
    psp_withdrawal_count        INT             NOT NULL DEFAULT 0,
    psp_withdrawal_amount       DECIMAL(19,4)   NOT NULL DEFAULT 0,
    withdrawal_matched_count    INT             NOT NULL DEFAULT 0,
    withdrawal_mismatch_count   INT             NOT NULL DEFAULT 0,

    -- Reconciliation Status
    reconciliation_status       SMALLINT        NOT NULL DEFAULT 1,  -- 1=PENDING, 2=IN_PROGRESS, 3=COMPLETED, 4=FAILED
    discrepancy_amount          DECIMAL(19,4)   NOT NULL DEFAULT 0,
    reconciled_at               TIMESTAMPTZ,

    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_recon_summary_date_psp_tenant UNIQUE (reconciliation_date, psp_code, tenant_id),
    CONSTRAINT ck_recon_summary_status CHECK (reconciliation_status IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_payment_reconciliation_summary IS '支付對帳彙總表 (每日 PSP 對帳結果)';
COMMENT ON COLUMN t_payment_reconciliation_summary.summary_id IS '對帳彙總唯一標識';
COMMENT ON COLUMN t_payment_reconciliation_summary.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_payment_reconciliation_summary.reconciliation_date IS '對帳日期';
COMMENT ON COLUMN t_payment_reconciliation_summary.psp_code IS 'PSP 識別碼';
COMMENT ON COLUMN t_payment_reconciliation_summary.total_deposit_count IS '平台存款筆數';
COMMENT ON COLUMN t_payment_reconciliation_summary.total_deposit_amount IS '平台存款總額 DECIMAL(19,4)';
COMMENT ON COLUMN t_payment_reconciliation_summary.psp_deposit_count IS 'PSP 存款筆數';
COMMENT ON COLUMN t_payment_reconciliation_summary.psp_deposit_amount IS 'PSP 存款總額 DECIMAL(19,4)';
COMMENT ON COLUMN t_payment_reconciliation_summary.deposit_matched_count IS '存款匹配筆數';
COMMENT ON COLUMN t_payment_reconciliation_summary.deposit_mismatch_count IS '存款不符筆數';
COMMENT ON COLUMN t_payment_reconciliation_summary.reconciliation_status IS '對帳狀態: 1=PENDING, 2=IN_PROGRESS, 3=COMPLETED, 4=FAILED';
COMMENT ON COLUMN t_payment_reconciliation_summary.discrepancy_amount IS '差異金額 DECIMAL(19,4)';

CREATE INDEX IF NOT EXISTS idx_recon_summary_tenant_date ON t_payment_reconciliation_summary (tenant_id, reconciliation_date DESC);
CREATE INDEX IF NOT EXISTS idx_recon_summary_status ON t_payment_reconciliation_summary (reconciliation_status, reconciliation_date DESC);

-- ---------------------------------------------------------------------
-- 2.2: t_payment_reconciliation_detail (Reconciliation Mismatch Details)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_payment_reconciliation_detail (
    detail_id                   BIGSERIAL       PRIMARY KEY,
    summary_id                  BIGINT          NOT NULL REFERENCES t_payment_reconciliation_summary(summary_id),
    tenant_id                   BIGINT          NOT NULL,

    -- Order Information
    payment_order_id            BIGINT          REFERENCES t_payment_order(payment_order_id),
    order_no                    VARCHAR(64),
    psp_transaction_id          VARCHAR(128),

    -- Mismatch Information
    mismatch_type               SMALLINT        NOT NULL,  -- 1=AMOUNT_MISMATCH, 2=MISSING_IN_PSP, 3=EXTRA_IN_PSP, 4=STATUS_MISMATCH
    platform_amount             DECIMAL(19,4),
    psp_amount                  DECIMAL(19,4),
    platform_status             SMALLINT,
    psp_status                  VARCHAR(50),

    -- Resolution
    resolution_status           SMALLINT        NOT NULL DEFAULT 1,  -- 1=PENDING, 2=RESOLVED, 3=ESCALATED, 4=IGNORED
    resolution_notes            TEXT,
    resolved_at                 TIMESTAMPTZ,
    resolved_by                 BIGINT,  -- FK to t_employee

    deleted                     BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_recon_detail_mismatch_type CHECK (mismatch_type IN (1, 2, 3, 4)),
    CONSTRAINT ck_recon_detail_resolution_status CHECK (resolution_status IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_payment_reconciliation_detail IS '支付對帳明細表 (記錄不符項目)';
COMMENT ON COLUMN t_payment_reconciliation_detail.detail_id IS '對帳明細唯一標識';
COMMENT ON COLUMN t_payment_reconciliation_detail.summary_id IS '關聯對帳彙總 ID (FK)';
COMMENT ON COLUMN t_payment_reconciliation_detail.payment_order_id IS '關聯支付訂單 ID (FK, 可能為 NULL)';
COMMENT ON COLUMN t_payment_reconciliation_detail.mismatch_type IS '不符類型: 1=金額不符, 2=PSP 缺失, 3=PSP 多餘, 4=狀態不符';
COMMENT ON COLUMN t_payment_reconciliation_detail.platform_amount IS '平台金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_payment_reconciliation_detail.psp_amount IS 'PSP 金額 DECIMAL(19,4)';
COMMENT ON COLUMN t_payment_reconciliation_detail.resolution_status IS '解決狀態: 1=PENDING, 2=RESOLVED, 3=ESCALATED, 4=IGNORED';

CREATE INDEX IF NOT EXISTS idx_recon_detail_summary ON t_payment_reconciliation_detail (summary_id);
CREATE INDEX IF NOT EXISTS idx_recon_detail_order ON t_payment_reconciliation_detail (payment_order_id) WHERE payment_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recon_detail_resolution ON t_payment_reconciliation_detail (resolution_status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_recon_detail_tenant ON t_payment_reconciliation_detail (tenant_id);

-- =====================================================================
-- PART 3: iGaming SmartJob Registrations (V17)
-- =====================================================================
-- Insert 8 scheduled jobs into t_smart_job table
-- Note: t_smart_job table should already exist from SmartAdmin base schema

INSERT INTO t_smart_job (job_name, job_class, trigger_type, trigger_value,
    param, enabled, sort, deleted, update_name, tenant_id, create_time, update_time)
VALUES
-- Activity module
('紅利過期清理', 'net.lab1024.sa.igaming.activity.job.BonusExpirationJob',
 'cron', '0 0/30 * * * ?', NULL, 1, 101, 0, 'System', 1, NOW(), NOW()),

('VIP 等級評估', 'net.lab1024.sa.igaming.activity.job.VipEvaluationJob',
 'cron', '0 0 4 * * ?', NULL, 1, 102, 0, 'System', 1, NOW(), NOW()),

-- Game module
('GP 健康檢查', 'net.lab1024.sa.igaming.game.job.GpHealthCheckJob',
 'fixed_delay', '60000', NULL, 1, 201, 0, 'System', 1, NOW(), NOW()),

('缺失結算輪詢', 'net.lab1024.sa.igaming.game.job.MissingSettlementPollJob',
 'fixed_delay', '300000', NULL, 1, 202, 0, 'System', 1, NOW(), NOW()),

('每日遊戲對帳', 'net.lab1024.sa.igaming.game.job.DailyReconciliationJob',
 'cron', '0 0 2 * * ?', NULL, 1, 203, 0, 'System', 1, NOW(), NOW()),

-- Risk module
('風控提案 SLA 監控', 'net.lab1024.sa.igaming.risk.job.RiskProposalSLAJob',
 'fixed_delay', '180000', NULL, 1, 301, 0, 'System', 1, NOW(), NOW()),

-- Wallet/Payment module
('支付對帳', 'net.lab1024.sa.igaming.wallet.payment.job.PaymentReconciliationJob',
 'cron', '0 0 5 * * ?', NULL, 1, 401, 0, 'System', 1, NOW(), NOW()),

('待處理訂單輪詢', 'net.lab1024.sa.igaming.wallet.payment.job.PendingOrderPollJob',
 'fixed_delay', '120000', NULL, 1, 402, 0, 'System', 1, NOW(), NOW())

ON CONFLICT DO NOTHING;

-- =====================================================================
-- PART 4: iGaming Menu Tree Initialization (V18)
-- =====================================================================
-- Initialize iGaming menu hierarchy: 1 root directory + 13 pages + 39 permission buttons
-- Menu types: 1=directory, 2=page, 3=permission button
-- perms_type: 1=backend permission

-- ==================== Root Directory ====================
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
VALUES ('iGaming 管理', 1, 0, 100, '/igaming', NULL, 0, 0, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ==================== Wallet Module ====================
-- Wallet Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '錢包管理', 2, m.menu_id, 1, '/igaming/wallet', 'igaming/wallet/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

-- Wallet permission buttons
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('餘額查詢', 1, 'wallet:balance:query'),
    ('餘額操作', 2, 'wallet:balance:operate'),
    ('加款操作', 3, 'wallet:credit:operate'),
    ('扣款操作', 4, 'wallet:debit:operate'),
    ('鎖定操作', 5, 'wallet:lock:operate'),
    ('交易查詢', 6, 'wallet:transaction:query')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '錢包管理' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Payment Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '支付管理', 2, m.menu_id, 2, '/igaming/payment', 'igaming/payment/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('存款操作', 1, 'payment:deposit:operate'),
    ('提款操作', 2, 'payment:withdraw:operate'),
    ('訂單查詢', 3, 'payment:order:query')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '支付管理' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Financial Report Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '財務報表', 2, m.menu_id, 3, '/igaming/report', 'igaming/report/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '報表查詢', 3, p.menu_id, 1, NULL, NULL, 0, 0, 1, 0,
    'wallet:report:query', 1, 'wallet:report:query', 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p WHERE p.menu_name = '財務報表' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- ==================== Game Module ====================
-- Game Lobby Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '遊戲大廳', 2, m.menu_id, 4, '/igaming/game/lobby', 'igaming/game/lobby/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('遊戲查詢', 1, 'game:lobby:query'),
    ('遊戲操作', 2, 'game:lobby:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '遊戲大廳' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Game Provider Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '遊戲供應商', 2, m.menu_id, 5, '/igaming/game/provider', 'igaming/game/provider/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('供應商查詢', 1, 'game:provider:query'),
    ('供應商操作', 2, 'game:provider:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '遊戲供應商' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- ==================== Player Module ====================
-- Player Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '玩家管理', 2, m.menu_id, 6, '/igaming/player', 'igaming/player/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('玩家查詢', 1, 'player:info:query'),
    ('玩家操作', 2, 'player:info:operate'),
    ('KYC 查詢', 3, 'player:kyc:query'),
    ('KYC 審核', 4, 'player:kyc:review'),
    ('KYC 操作', 5, 'player:kyc:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '玩家管理' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- ==================== Activity Module ====================
-- Promotion Rules Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '促銷規則', 2, m.menu_id, 7, '/igaming/activity/promotion', 'igaming/activity/promotion/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('規則查詢', 1, 'activity:promotion:query'),
    ('規則新增', 2, 'activity:promotion:add'),
    ('規則修改', 3, 'activity:promotion:update')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '促銷規則' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Bonus Claim Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '紅利管理', 2, m.menu_id, 8, '/igaming/activity/bonus', 'igaming/activity/bonus/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('紅利查詢', 1, 'activity:bonus:query'),
    ('紅利操作', 2, 'activity:bonus:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '紅利管理' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- ==================== Risk Module ====================
-- Risk Rules Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風控規則', 2, m.menu_id, 9, '/igaming/risk/rule', 'igaming/risk/rule/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('規則查詢', 1, 'risk:rule:query'),
    ('規則新增', 2, 'risk:rule:add'),
    ('規則修改', 3, 'risk:rule:update'),
    ('規則刪除', 4, 'risk:rule:delete')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '風控規則' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Risk Proposals Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風控提案', 2, m.menu_id, 10, '/igaming/risk/proposal', 'igaming/risk/proposal/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('提案查詢', 1, 'risk:proposal:query'),
    ('提案審核', 2, 'risk:proposal:review')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '風控提案' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Risk Score Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風險評分', 2, m.menu_id, 11, '/igaming/risk/score', 'igaming/risk/score/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '評分查詢', 3, p.menu_id, 1, NULL, NULL, 0, 0, 1, 0,
    'risk:score:query', 1, 'risk:score:query', 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p WHERE p.menu_name = '風險評分' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- ==================== Agent Module ====================
-- Affiliate Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '聯盟代理', 2, m.menu_id, 12, '/igaming/agent/affiliate', 'igaming/agent/affiliate/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('代理查詢', 1, 'agent:affiliate:query'),
    ('代理註冊', 2, 'agent:affiliate:register'),
    ('佣金管理', 3, 'agent:affiliate:commission')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '聯盟代理' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- Credit Network Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '信用網路', 2, m.menu_id, 13, '/igaming/agent/credit', 'igaming/agent/credit/index', 0, 1, 1, 0,
    NULL, NULL, NULL, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted = 0
ON CONFLICT DO NOTHING;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame,
    cache, visible, disabled, api_perms, perms_type, web_perms, deleted,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, 0, 0, 1, 0,
    v.api_perms, 1, v.api_perms, 0, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('信用查詢', 1, 'agent:credit:query'),
    ('信用分配', 2, 'agent:credit:allocate'),
    ('信用回收', 3, 'agent:credit:recall'),
    ('結算觸發', 4, 'agent:settlement:trigger'),
    ('結算驗證', 5, 'agent:settlement:verify')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '信用網路' AND p.tenant_id = 1 AND p.deleted = 0
ON CONFLICT DO NOTHING;

-- =====================================================================
-- PART 5: MFA (Multi-Factor Authentication) Tables (V19)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 5.1: t_mfa_device (MFA Device Registration)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_mfa_device (
    device_id               BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,  -- FK to t_employee

    -- Device Information
    device_type             SMALLINT        NOT NULL,  -- 1=TOTP_APP, 2=SMS, 3=EMAIL, 4=HARDWARE_TOKEN
    device_name             VARCHAR(100),  -- User-friendly name (e.g., "Google Authenticator")
    encrypted_secret        TEXT            NOT NULL,  -- TOTP secret (AES-256-GCM encrypted)

    -- Verification Status
    verified                BOOLEAN         NOT NULL DEFAULT FALSE,
    verified_at             TIMESTAMPTZ,
    is_primary              BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Usage Statistics
    last_used_at            TIMESTAMPTZ,
    use_count               INT             NOT NULL DEFAULT 0,

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_mfa_device_type CHECK (device_type IN (1, 2, 3, 4))
);

COMMENT ON TABLE t_mfa_device IS 'MFA 裝置註冊表 (TOTP, SMS, Email, Hardware Token)';
COMMENT ON COLUMN t_mfa_device.device_id IS 'MFA 裝置唯一標識';
COMMENT ON COLUMN t_mfa_device.employee_id IS '員工 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_device.device_type IS '裝置類型: 1=TOTP_APP, 2=SMS, 3=EMAIL, 4=HARDWARE_TOKEN';
COMMENT ON COLUMN t_mfa_device.encrypted_secret IS 'TOTP 密鑰 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_mfa_device.verified IS '是否已驗證 (FALSE=待驗證, TRUE=已驗證)';
COMMENT ON COLUMN t_mfa_device.is_primary IS '是否為主要 MFA 裝置';

CREATE INDEX IF NOT EXISTS idx_mfa_device_employee ON t_mfa_device (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_device_tenant ON t_mfa_device (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mfa_device_primary ON t_mfa_device (employee_id, is_primary) WHERE is_primary = TRUE AND deleted = FALSE;

-- ---------------------------------------------------------------------
-- 5.2: t_mfa_backup_code (MFA Backup Codes)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_mfa_backup_code (
    code_id                 BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,  -- FK to t_employee

    -- Backup Code
    code_hash               VARCHAR(128)    NOT NULL,  -- SHA-256 hash of backup code
    used                    BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at                 TIMESTAMPTZ,
    used_from_ip            VARCHAR(45),

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_mfa_backup_code IS 'MFA 備用碼表 (一次性緊急登入碼)';
COMMENT ON COLUMN t_mfa_backup_code.code_id IS '備用碼唯一標識';
COMMENT ON COLUMN t_mfa_backup_code.employee_id IS '員工 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_backup_code.code_hash IS '備用碼雜湊值 (SHA-256)';
COMMENT ON COLUMN t_mfa_backup_code.used IS '是否已使用';
COMMENT ON COLUMN t_mfa_backup_code.used_at IS '使用時間';
COMMENT ON COLUMN t_mfa_backup_code.used_from_ip IS '使用 IP 地址';

CREATE INDEX IF NOT EXISTS idx_mfa_backup_employee ON t_mfa_backup_code (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_backup_tenant ON t_mfa_backup_code (tenant_id);

-- ---------------------------------------------------------------------
-- 5.3: t_mfa_trusted_device (Trusted Device Tracking)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_mfa_trusted_device (
    trusted_device_id       BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,  -- FK to t_employee

    -- Device Fingerprint
    device_fingerprint      VARCHAR(128)    NOT NULL,  -- Hash of User-Agent + Browser features
    device_name             VARCHAR(200),  -- User-friendly device name
    ip_address              VARCHAR(45),

    -- Trust Status
    trusted_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMPTZ,  -- Optional expiry time (30 days default)
    revoked                 BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at              TIMESTAMPTZ,
    revoked_reason          VARCHAR(200),

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_mfa_trusted_device IS 'MFA 信任裝置表 (記住此裝置 30 天免二次驗證)';
COMMENT ON COLUMN t_mfa_trusted_device.trusted_device_id IS '信任裝置唯一標識';
COMMENT ON COLUMN t_mfa_trusted_device.employee_id IS '員工 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_trusted_device.device_fingerprint IS '裝置指紋 (User-Agent + Browser features SHA-256)';
COMMENT ON COLUMN t_mfa_trusted_device.expires_at IS '信任過期時間 (預設 30 天)';
COMMENT ON COLUMN t_mfa_trusted_device.revoked IS '是否已撤銷信任';

CREATE INDEX IF NOT EXISTS idx_mfa_trusted_employee ON t_mfa_trusted_device (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_trusted_fingerprint ON t_mfa_trusted_device (device_fingerprint, employee_id) WHERE revoked = FALSE AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_trusted_tenant ON t_mfa_trusted_device (tenant_id);

-- ---------------------------------------------------------------------
-- 5.4: t_mfa_session (MFA Session Tracking)
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS t_mfa_session (
    session_id              BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,  -- FK to t_employee

    -- Session Information
    session_token           VARCHAR(128)    NOT NULL UNIQUE,
    mfa_verified            BOOLEAN         NOT NULL DEFAULT FALSE,
    mfa_verified_at         TIMESTAMPTZ,
    device_id               BIGINT          REFERENCES t_mfa_device(device_id),  -- Which device was used

    -- Security Context
    ip_address              VARCHAR(45),
    user_agent              TEXT,
    session_expires_at      TIMESTAMPTZ,

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_mfa_session IS 'MFA 會話追蹤表 (記錄 MFA 驗證狀態)';
COMMENT ON COLUMN t_mfa_session.session_id IS 'MFA 會話唯一標識';
COMMENT ON COLUMN t_mfa_session.employee_id IS '員工 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_session.session_token IS '會話令牌 (唯一)';
COMMENT ON COLUMN t_mfa_session.mfa_verified IS 'MFA 是否已驗證';
COMMENT ON COLUMN t_mfa_session.device_id IS '使用的 MFA 裝置 ID (FK)';
COMMENT ON COLUMN t_mfa_session.session_expires_at IS '會話過期時間';

CREATE INDEX IF NOT EXISTS idx_mfa_session_employee ON t_mfa_session (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_session_tenant ON t_mfa_session (tenant_id);
CREATE INDEX IF NOT EXISTS idx_mfa_session_expires ON t_mfa_session (session_expires_at) WHERE mfa_verified = TRUE AND deleted = FALSE;

-- =====================================================================
-- PART 6: MFA Recovery Request Table (V20)
-- =====================================================================

CREATE TABLE IF NOT EXISTS t_mfa_recovery_request (
    request_id              BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,  -- FK to t_employee

    -- Recovery Request
    request_reason          VARCHAR(500)    NOT NULL,  -- Reason for MFA recovery (lost device, etc.)
    request_status          SMALLINT        NOT NULL DEFAULT 1,  -- 1=PENDING, 2=APPROVED, 3=REJECTED
    verification_method     SMALLINT,  -- 1=MANAGER_APPROVAL, 2=EMAIL_VERIFICATION, 3=MANUAL_VERIFICATION

    -- Approval Workflow
    approver_id             BIGINT,  -- FK to t_employee (manager who approved)
    approved_at             TIMESTAMPTZ,
    rejection_reason        VARCHAR(500),

    -- Recovery Actions
    old_device_id           BIGINT          REFERENCES t_mfa_device(device_id),  -- Lost device
    new_device_id           BIGINT          REFERENCES t_mfa_device(device_id),  -- Replacement device
    recovery_completed_at   TIMESTAMPTZ,

    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_mfa_recovery_status CHECK (request_status IN (1, 2, 3)),
    CONSTRAINT ck_mfa_recovery_method CHECK (verification_method IN (1, 2, 3))
);

COMMENT ON TABLE t_mfa_recovery_request IS 'MFA 恢復請求表 (裝置遺失/損壞時的恢復流程)';
COMMENT ON COLUMN t_mfa_recovery_request.request_id IS 'MFA 恢復請求唯一標識';
COMMENT ON COLUMN t_mfa_recovery_request.employee_id IS '員工 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_recovery_request.request_reason IS '恢復原因 (例如: 裝置遺失、裝置損壞)';
COMMENT ON COLUMN t_mfa_recovery_request.request_status IS '請求狀態: 1=PENDING, 2=APPROVED, 3=REJECTED';
COMMENT ON COLUMN t_mfa_recovery_request.verification_method IS '驗證方式: 1=MANAGER_APPROVAL, 2=EMAIL_VERIFICATION, 3=MANUAL_VERIFICATION';
COMMENT ON COLUMN t_mfa_recovery_request.approver_id IS '審批員 ID (FK to t_employee)';
COMMENT ON COLUMN t_mfa_recovery_request.old_device_id IS '舊裝置 ID (FK to t_mfa_device)';
COMMENT ON COLUMN t_mfa_recovery_request.new_device_id IS '新裝置 ID (FK to t_mfa_device)';

CREATE INDEX IF NOT EXISTS idx_mfa_recovery_employee ON t_mfa_recovery_request (employee_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_mfa_recovery_status ON t_mfa_recovery_request (request_status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_mfa_recovery_tenant ON t_mfa_recovery_request (tenant_id);

-- =====================================================================
-- PART 7: Withdrawal Approval Workflow Tables (V23)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 7.1: t_withdrawal_approval (Withdrawal Approval Records)
-- ---------------------------------------------------------------------

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
COMMENT ON COLUMN t_withdrawal_approval.risk_score IS '風險評分 (0-100, 由風險評估服務計算)';
COMMENT ON COLUMN t_withdrawal_approval.saga_state IS 'SAGA 工作流當前狀態: RISK_ASSESSMENT, KYC_VERIFICATION, RISK_PROPOSAL_CHECK, APPROVAL_ROUTING, PAYMENT_EXECUTION, COMPLETED';
COMMENT ON COLUMN t_withdrawal_approval.sla_deadline IS 'SLA 截止時間 (24h 自動審批/拒絕截止時間)';

CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_order ON t_withdrawal_approval (payment_order_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_status_assigned ON t_withdrawal_approval (approval_status, assigned_at DESC) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_approver_status ON t_withdrawal_approval (approver_id, approval_status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_tenant_time ON t_withdrawal_approval (tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_sla ON t_withdrawal_approval (sla_deadline) WHERE sla_breached = FALSE AND approval_status = 1;
CREATE INDEX IF NOT EXISTS idx_withdrawal_approval_saga_state ON t_withdrawal_approval (saga_state, compensation_state) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- 7.2: t_approval_delegation (Approval Delegation Records)
-- ---------------------------------------------------------------------

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

CREATE INDEX IF NOT EXISTS idx_approval_delegation_approval ON t_approval_delegation (approval_id);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_from_approver ON t_approval_delegation (from_approver_id, delegated_at DESC);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_to_approver ON t_approval_delegation (to_approver_id, delegated_at DESC);
CREATE INDEX IF NOT EXISTS idx_approval_delegation_tenant ON t_approval_delegation (tenant_id);

-- ---------------------------------------------------------------------
-- 7.3: Alter t_payment_order to Support Withdrawal Approval Workflow
-- ---------------------------------------------------------------------

-- Add approval_required flag to indicate if this withdrawal needs manual approval
ALTER TABLE t_payment_order ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN t_payment_order.approval_required IS '是否需要人工審批 (TRUE=需要審批, FALSE=自動審批或存款訂單)';

-- Add deleted column for soft delete support (consistent with other iGaming tables)
ALTER TABLE t_payment_order ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN t_payment_order.deleted IS '軟刪除標記 (TRUE=已刪除, FALSE=正常)';

CREATE INDEX IF NOT EXISTS idx_payment_order_approval_required ON t_payment_order (approval_required, status) WHERE order_type = 2 AND deleted = FALSE;

-- =====================================================================
-- PART 8: RLS Policies (Row-Level Security)
-- =====================================================================

-- Enable RLS on new tables
ALTER TABLE t_payment_reconciliation_summary ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_payment_reconciliation_detail ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_backup_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_trusted_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_mfa_recovery_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_withdrawal_approval ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_approval_delegation ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY tenant_isolation ON t_payment_reconciliation_summary
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_payment_reconciliation_detail
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mfa_device
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mfa_backup_code
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mfa_trusted_device
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mfa_session
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_mfa_recovery_request
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_withdrawal_approval
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_approval_delegation
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- PART 9: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_payment_reconciliation_summary TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_payment_reconciliation_detail TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_device TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_backup_code TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_trusted_device TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_session TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_mfa_recovery_request TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_withdrawal_approval TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_approval_delegation TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_payment_reconciliation_summary_summary_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_payment_reconciliation_detail_detail_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_device_device_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_backup_code_code_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_trusted_device_trusted_device_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_session_session_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_mfa_recovery_request_request_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_withdrawal_approval_approval_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_approval_delegation_delegation_id_seq TO smartadmin_app;
