-- =====================================================================
-- V2: iGaming Domain Tables (Consolidated from V8-V14)
-- =====================================================================
-- Description: SmartAdmin iGaming domain data models including:
--              - Wallet Domain (V8): 4 tables
--              - Payment Domain (V9): 2 tables
--              - Player Domain (V10): 4 tables
--              - Game Domain (V11): 5 tables
--              - Activity Domain (V12): 2 tables
--              - Risk Domain (V13): 5 tables
--              - Agent Domain (V14): 8 tables
--              Total: 30 tables
--
-- Migration History:
--   - Original V8:  Wallet tables (t_wallet, t_wallet_transaction, t_wallet_bonus_ext, t_wallet_lock)
--   - Original V9:  Payment tables (t_payment_order, t_psp)
--   - Original V10: Player tables (t_player, t_kyc_document, t_vip_change_log, t_player_audit_log)
--   - Original V11: Game tables (t_game_provider, t_game, t_game_round, t_reconciliation, t_game_weight_config)
--   - Original V12: Activity tables (t_promotion_rule, t_player_bonus_record)
--   - Original V13: Risk tables (t_risk_rule_param, t_risk_assessment, t_risk_score, t_risk_proposal, t_geo_restriction)
--   - Original V14: Agent tables (8 tables for credit network + affiliate system)
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (already migrated in V1)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id BIGINT NOT NULL on all tables (Multi-Tenant)
--   - PII encryption: AES-256-GCM + HMAC-SHA256 blind index
--
-- Author: iGaming Team
-- Consolidation Date: 2026-03-14
-- Version: 2.0.0 (Consolidated)
-- =====================================================================

-- =====================================================================
-- PART 1: WALLET DOMAIN (from V8) - 4 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_wallet (Master Wallet)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wallet (
    wallet_id       BIGSERIAL       PRIMARY KEY,
    player_id       BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    currency_code   VARCHAR(10)     NOT NULL DEFAULT 'USD',
    wallet_type     SMALLINT        NOT NULL,
    balance         DECIMAL(19,4)   NOT NULL DEFAULT 0,
    locked_amount   DECIMAL(19,4)   NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 0,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_wallet_player_type_tenant UNIQUE (player_id, wallet_type, tenant_id),
    CONSTRAINT ck_wallet_type CHECK (wallet_type IN (1, 2, 3)),
    CONSTRAINT ck_wallet_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_wallet_locked_non_negative CHECK (locked_amount >= 0),
    CONSTRAINT ck_wallet_locked_le_balance CHECK (locked_amount <= balance)
);

COMMENT ON TABLE t_wallet IS '玩家錢包主表';
COMMENT ON COLUMN t_wallet.wallet_type IS '錢包類型: 1=現金, 2=紅利, 3=信用';
COMMENT ON COLUMN t_wallet.balance IS '餘額 DECIMAL(19,4)';
COMMENT ON COLUMN t_wallet.locked_amount IS '鎖定金額';
COMMENT ON COLUMN t_wallet.version IS '樂觀鎖版本號';

CREATE INDEX IF NOT EXISTS idx_wallet_player_tenant ON t_wallet (player_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_wallet_tenant ON t_wallet (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_wallet_transaction (Transaction Log — Append-Only)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wallet_transaction (
    transaction_id    BIGSERIAL       PRIMARY KEY,
    wallet_id         BIGINT          NOT NULL REFERENCES t_wallet(wallet_id),
    player_id         BIGINT          NOT NULL,
    transaction_type  SMALLINT        NOT NULL,
    amount            DECIMAL(19,4)   NOT NULL,
    balance_before    DECIMAL(19,4)   NOT NULL,
    balance_after     DECIMAL(19,4)   NOT NULL,
    request_id        VARCHAR(64)     NOT NULL,
    reference_type    VARCHAR(30),
    reference_id      VARCHAR(64),
    description       VARCHAR(200),
    tenant_id         BIGINT          NOT NULL,
    create_time       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_wallet_transaction_request UNIQUE (request_id),
    CONSTRAINT ck_wallet_transaction_type CHECK (transaction_type IN (1, 2, 3, 4, 5, 6, 7))
);

COMMENT ON TABLE t_wallet_transaction IS '錢包交易記錄 (不可變)';
COMMENT ON COLUMN t_wallet_transaction.transaction_type IS '交易類型: 1=存款, 2=提款, 3=下注, 4=派彩, 5=紅利, 6=調整, 7=回滾';
COMMENT ON COLUMN t_wallet_transaction.request_id IS '冪等鍵 (Idempotency Layer 2)';

CREATE INDEX IF NOT EXISTS idx_wallet_txn_wallet_time ON t_wallet_transaction (wallet_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_player_type ON t_wallet_transaction (player_id, transaction_type, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_tenant ON t_wallet_transaction (tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_txn_ref ON t_wallet_transaction (reference_type, reference_id);

-- ---------------------------------------------------------------------
-- Table: t_wallet_bonus_ext (Bonus Detail Extension)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wallet_bonus_ext (
    id                    BIGSERIAL       PRIMARY KEY,
    wallet_id             BIGINT          NOT NULL REFERENCES t_wallet(wallet_id),
    bonus_id              BIGINT          NOT NULL,
    balance               DECIMAL(19,4)   NOT NULL DEFAULT 0,
    wagering_requirement  DECIMAL(19,4)   NOT NULL,
    wagered_amount        DECIMAL(19,4)   NOT NULL DEFAULT 0,
    expires_at            TIMESTAMPTZ,
    game_restriction      JSONB,
    status                SMALLINT        NOT NULL DEFAULT 1,
    tenant_id             BIGINT          NOT NULL,
    create_time           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_bonus_ext_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT ck_bonus_ext_balance_non_negative CHECK (balance >= 0)
);

COMMENT ON TABLE t_wallet_bonus_ext IS '紅利錢包明細';
COMMENT ON COLUMN t_wallet_bonus_ext.status IS '狀態: 1=有效, 2=過期, 3=完成, 4=沒收';

CREATE INDEX IF NOT EXISTS idx_wallet_bonus_ext_wallet_status ON t_wallet_bonus_ext (wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_bonus_ext_expires ON t_wallet_bonus_ext (expires_at, status) WHERE status = 1;

-- ---------------------------------------------------------------------
-- Table: t_wallet_lock (Lock Detail Records)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_wallet_lock (
    lock_id         BIGSERIAL       PRIMARY KEY,
    wallet_id       BIGINT          NOT NULL REFERENCES t_wallet(wallet_id),
    lock_amount     DECIMAL(19,4)   NOT NULL,
    lock_reason     SMALLINT        NOT NULL,
    reference_id    VARCHAR(64)     NOT NULL,
    expires_at      TIMESTAMPTZ,
    tenant_id       BIGINT          NOT NULL,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_wallet_lock_amount_positive CHECK (lock_amount > 0),
    CONSTRAINT ck_wallet_lock_reason CHECK (lock_reason IN (1, 2, 3))
);

COMMENT ON TABLE t_wallet_lock IS '錢包鎖定明細';
COMMENT ON COLUMN t_wallet_lock.lock_reason IS '鎖定原因: 1=下注待結算, 2=提款待審核, 3=風控凍結';

CREATE INDEX IF NOT EXISTS idx_wallet_lock_wallet_ref ON t_wallet_lock (wallet_id, reference_id);
CREATE INDEX IF NOT EXISTS idx_wallet_lock_expires ON t_wallet_lock (expires_at) WHERE expires_at IS NOT NULL;

-- =====================================================================
-- PART 2: PAYMENT DOMAIN (from V9) - 2 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_payment_order (Unified Deposit/Withdrawal Order)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_payment_order (
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
    reconciliation_status SMALLINT      NOT NULL DEFAULT 1,
    version             INT             NOT NULL DEFAULT 0,
    tenant_id           BIGINT          NOT NULL,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_payment_order_no UNIQUE (order_no),
    CONSTRAINT uk_payment_order_request UNIQUE (request_id),
    CONSTRAINT ck_payment_order_type CHECK (order_type IN (1, 2)),
    CONSTRAINT ck_payment_order_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT ck_payment_order_reconciliation CHECK (reconciliation_status IN (1, 2, 3)),
    CONSTRAINT ck_payment_order_amount_positive CHECK (amount > 0)
);

COMMENT ON TABLE t_payment_order IS '支付訂單 (統一存款/提款)';
COMMENT ON COLUMN t_payment_order.order_type IS '訂單類型: 1=存款, 2=提款';
COMMENT ON COLUMN t_payment_order.status IS '訂單狀態: 1=PENDING, 2=PROCESSING, 3=SUCCESS, 4=FAILED, 5=REJECTED, 6=REFUNDED, 7=CANCELLED';
COMMENT ON COLUMN t_payment_order.reconciliation_status IS '對帳狀態: 1=PENDING (待對帳), 2=MATCHED (已匹配), 3=MISMATCHED (不符)';

CREATE INDEX IF NOT EXISTS idx_payment_order_player_time ON t_payment_order (player_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_status_time ON t_payment_order (status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_psp_code ON t_payment_order (psp_code, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_tenant ON t_payment_order (tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_order_wallet ON t_payment_order (wallet_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_reconciliation ON t_payment_order (reconciliation_status, create_time DESC) WHERE reconciliation_status IN (1, 3);

-- ---------------------------------------------------------------------
-- Table: t_psp (PSP Configuration)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_psp (
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
COMMENT ON COLUMN t_psp.api_key_encrypted IS 'API Key (AES-256-GCM 加密)';

CREATE INDEX IF NOT EXISTS idx_psp_tenant_enabled ON t_psp (tenant_id, enabled);

-- Seed Data: MockPSP for testing
INSERT INTO t_psp (psp_code, psp_name, api_base_url, enabled, priority, supported_currencies, min_deposit, max_deposit, min_withdrawal, max_withdrawal, tenant_id)
VALUES ('mock', 'Mock PSP (Testing)', 'http://localhost:9999', TRUE, 999, 'USD,EUR,GBP', 10.0000, 10000.0000, 20.0000, 50000.0000, 1)
ON CONFLICT DO NOTHING;

-- =====================================================================
-- PART 3: PLAYER DOMAIN (from V10) - 4 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_player (Master Player)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_player (
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
COMMENT ON COLUMN t_player.email_encrypted IS '電子郵件 (AES-256-GCM 加密)';
COMMENT ON COLUMN t_player.email_blind_idx IS '電子郵件盲索引 (HMAC-SHA256)';
COMMENT ON COLUMN t_player.status IS '狀態: 1=ACTIVE, 2=LOCKED, 3=SUSPENDED, 4=PENDING_VERIFICATION, 5=CLOSED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_player_username_tenant ON t_player (tenant_id, username) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_player_email_blind ON t_player (email_blind_idx) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_player_phone_blind ON t_player (phone_blind_idx) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_player_tenant ON t_player (tenant_id);
CREATE INDEX IF NOT EXISTS idx_player_status ON t_player (tenant_id, status);

-- ---------------------------------------------------------------------
-- Table: t_kyc_document (KYC Document Verification)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_kyc_document (
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
COMMENT ON COLUMN t_kyc_document.document_type IS '文件類型: 1=身分證, 2=護照, 3=駕照';

CREATE INDEX IF NOT EXISTS idx_kyc_doc_player ON t_kyc_document (player_id);
CREATE INDEX IF NOT EXISTS idx_kyc_doc_tenant ON t_kyc_document (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_vip_change_log (VIP Level Change Audit)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_vip_change_log (
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

CREATE INDEX IF NOT EXISTS idx_vip_log_player ON t_vip_change_log (player_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_vip_log_tenant ON t_vip_change_log (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_player_audit_log (Player Status Change Audit)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_player_audit_log (
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

CREATE INDEX IF NOT EXISTS idx_player_audit_player ON t_player_audit_log (player_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_player_audit_tenant ON t_player_audit_log (tenant_id);

-- =====================================================================
-- PART 4: GAME DOMAIN (from V11) - 5 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_game_provider (Game Provider Configuration)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_game_provider (
    provider_id         BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    provider_code       VARCHAR(32)     NOT NULL,
    provider_name       VARCHAR(128)    NOT NULL,
    api_url             VARCHAR(512)    NOT NULL,
    encrypted_api_key   VARCHAR(1024),
    callback_url        VARCHAR(512),
    supported_games     JSONB,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    health_status       SMALLINT        NOT NULL DEFAULT 1,
    last_health_check   TIMESTAMPTZ,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_gp_health_status CHECK (health_status IN (1, 2, 3))
);

COMMENT ON TABLE t_game_provider IS '遊戲供應商配置表';
COMMENT ON COLUMN t_game_provider.health_status IS '健康狀態: 1=健康, 2=降級, 3=下線';

CREATE UNIQUE INDEX IF NOT EXISTS uk_gp_code_tenant ON t_game_provider (provider_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_gp_tenant ON t_game_provider (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_game (Game Catalog)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_game (
    game_id             BIGSERIAL       PRIMARY KEY,
    provider_id         BIGINT          NOT NULL REFERENCES t_game_provider(provider_id),
    tenant_id           BIGINT          NOT NULL,
    game_code           VARCHAR(64)     NOT NULL,
    game_name           VARCHAR(128)    NOT NULL,
    category            SMALLINT        NOT NULL,
    thumbnail_url       VARCHAR(512),
    play_count          BIGINT          NOT NULL DEFAULT 0,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             INT             NOT NULL DEFAULT 0,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_game_category CHECK (category IN (1, 2, 3, 4, 5, 6))
);

COMMENT ON TABLE t_game IS '遊戲主表';
COMMENT ON COLUMN t_game.category IS '遊戲類別: 1=老虎機, 2=真人娛樂城, 3=體育, 4=撲克, 5=桌遊, 6=彩票';

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_code_tenant ON t_game (game_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_game_tenant_category ON t_game (tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_game_provider ON t_game (provider_id);

-- ---------------------------------------------------------------------
-- Table: t_game_round (Game Round Records — Seamless Wallet Core)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_game_round (
    round_id                BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    provider_code           VARCHAR(32)     NOT NULL,
    gp_round_id             VARCHAR(128)    NOT NULL,
    game_code               VARCHAR(64)     NOT NULL,
    transaction_id          VARCHAR(128)    NOT NULL,
    bet_amount              DECIMAL(19,4),
    payout_amount           DECIMAL(19,4),
    weighted_turnover       DECIMAL(19,4),
    status                  SMALLINT        NOT NULL DEFAULT 1,
    reconciliation_status   SMALLINT        NOT NULL DEFAULT 1,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_game_round_txn UNIQUE (transaction_id),
    CONSTRAINT ck_round_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT ck_recon_status CHECK (reconciliation_status IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_game_round IS '遊戲局記錄表 (Seamless Wallet 核心)';
COMMENT ON COLUMN t_game_round.status IS '局狀態: 1=進行中, 2=已結算, 3=已取消, 4=已作廢';

CREATE INDEX IF NOT EXISTS idx_round_player ON t_game_round (player_id);
CREATE INDEX IF NOT EXISTS idx_round_provider_date ON t_game_round (provider_code, create_time);
CREATE INDEX IF NOT EXISTS idx_round_status ON t_game_round (status, reconciliation_status);
CREATE INDEX IF NOT EXISTS idx_round_tenant ON t_game_round (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_reconciliation (Daily Reconciliation Summary)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_reconciliation (
    reconciliation_id               BIGSERIAL       PRIMARY KEY,
    tenant_id                       BIGINT          NOT NULL,
    reconciliation_date             DATE            NOT NULL,
    provider_code                   VARCHAR(32)     NOT NULL,
    total_gp_transactions           INT             NOT NULL DEFAULT 0,
    total_platform_transactions     INT             NOT NULL DEFAULT 0,
    matched_count                   INT             NOT NULL DEFAULT 0,
    mismatch_count                  INT             NOT NULL DEFAULT 0,
    missing_count                   INT             NOT NULL DEFAULT 0,
    extra_count                     INT             NOT NULL DEFAULT 0,
    status                          SMALLINT        NOT NULL DEFAULT 1,
    deleted                         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_reconciliation IS '日對帳彙總表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_recon_date_provider_tenant ON t_reconciliation (reconciliation_date, provider_code, tenant_id);
CREATE INDEX IF NOT EXISTS idx_recon_tenant ON t_reconciliation (tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_game_weight_config (Per-Tenant Game Category Weights)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_game_weight_config (
    config_id       BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    game_category   SMALLINT        NOT NULL,
    weight          DECIMAL(5,4)    NOT NULL,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_weight_tenant_category UNIQUE (tenant_id, game_category),
    CONSTRAINT ck_weight_range CHECK (weight >= 0 AND weight <= 1)
);

COMMENT ON TABLE t_game_weight_config IS '遊戲權重配置表';

-- =====================================================================
-- PART 5: ACTIVITY DOMAIN (from V12) - 2 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_promotion_rule (Promotion Rule Configuration)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_promotion_rule (
    rule_id                 BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    promotion_code          VARCHAR(64)     NOT NULL,
    promotion_name          VARCHAR(128)    NOT NULL,
    promotion_type          SMALLINT        NOT NULL,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    start_time              TIMESTAMPTZ     NOT NULL,
    end_time                TIMESTAMPTZ     NOT NULL,
    min_deposit             DECIMAL(19,4)   NOT NULL DEFAULT 0,
    bonus_rate              DECIMAL(8,4),
    max_bonus               DECIMAL(19,4),
    wagering_multiplier     DECIMAL(8,2)    NOT NULL DEFAULT 1.00,
    max_claims_per_player   INT             NOT NULL DEFAULT 1,
    bonus_expiry_days       INT             NOT NULL DEFAULT 30,
    game_restriction        JSONB,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_promo_type CHECK (promotion_type IN (1, 2, 3, 4, 5)),
    CONSTRAINT ck_promo_status CHECK (status IN (1, 2, 3))
);

COMMENT ON TABLE t_promotion_rule IS '促銷活動規則配置表';
COMMENT ON COLUMN t_promotion_rule.promotion_type IS '促銷類型: 1=首存, 2=續存, 3=流水回饋, 4=活動, 5=免費旋轉';

CREATE UNIQUE INDEX IF NOT EXISTS uk_promo_code_tenant ON t_promotion_rule (promotion_code, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_promo_tenant_status ON t_promotion_rule (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_promo_type ON t_promotion_rule (promotion_type);

-- ---------------------------------------------------------------------
-- Table: t_player_bonus_record (Player Bonus Claim Records)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_player_bonus_record (
    record_id               BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    player_id               BIGINT          NOT NULL,
    rule_id                 BIGINT          NOT NULL REFERENCES t_promotion_rule(rule_id),
    claim_id                VARCHAR(64)     NOT NULL,
    bonus_amount            DECIMAL(19,4)   NOT NULL,
    wagering_required       DECIMAL(19,4)   NOT NULL DEFAULT 0,
    wagering_completed      DECIMAL(19,4)   NOT NULL DEFAULT 0,
    status                  SMALLINT        NOT NULL DEFAULT 1,
    claimed_at              TIMESTAMPTZ     NOT NULL,
    completed_at            TIMESTAMPTZ,
    expired_at              TIMESTAMPTZ,
    wallet_bonus_ext_id     BIGINT,
    deleted                 BOOLEAN         NOT NULL DEFAULT FALSE,
    version                 INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_bonus_record_status CHECK (status IN (1, 2, 3, 4, 5))
);

COMMENT ON TABLE t_player_bonus_record IS '玩家獎金領取記錄表';
COMMENT ON COLUMN t_player_bonus_record.status IS '狀態: 1=待啟用, 2=進行中, 3=已完成, 4=已過期, 5=已沒收';

CREATE UNIQUE INDEX IF NOT EXISTS uk_bonus_claim ON t_player_bonus_record (player_id, rule_id, claim_id, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bonus_player ON t_player_bonus_record (player_id);
CREATE INDEX IF NOT EXISTS idx_bonus_tenant_status ON t_player_bonus_record (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_bonus_rule ON t_player_bonus_record (rule_id);
CREATE INDEX IF NOT EXISTS idx_bonus_expired ON t_player_bonus_record (expired_at, status) WHERE status = 2;

-- =====================================================================
-- PART 6: RISK DOMAIN (from V13) - 5 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_risk_rule_param (Risk Rule Parameter Configuration)
-- ---------------------------------------------------------------------
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
COMMENT ON COLUMN t_risk_rule_param.rule_type IS '規則類型: 1=速率, 2=金額, 3=設備, 4=行為, 5=地理';

CREATE INDEX IF NOT EXISTS idx_risk_rule_tenant_type ON t_risk_rule_param (tenant_id, rule_type) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- Table: t_risk_assessment (Per-Transaction Risk Assessment Log)
-- ---------------------------------------------------------------------
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
COMMENT ON COLUMN t_risk_assessment.risk_level IS '風險等級: 1=低, 2=中, 3=高, 4=極高';

CREATE INDEX IF NOT EXISTS idx_assessment_player ON t_risk_assessment (player_id);
CREATE INDEX IF NOT EXISTS idx_assessment_tenant_event ON t_risk_assessment (tenant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_assessment_created ON t_risk_assessment (create_time);

-- ---------------------------------------------------------------------
-- Table: t_risk_score (Player Cumulative Risk Profile)
-- ---------------------------------------------------------------------
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_risk_score_player_tenant ON t_risk_score (player_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_risk_score_level ON t_risk_score (risk_level);

-- ---------------------------------------------------------------------
-- Table: t_risk_proposal (Risk Review Proposals)
-- ---------------------------------------------------------------------
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

CREATE INDEX IF NOT EXISTS idx_proposal_tenant_status ON t_risk_proposal (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_proposal_player ON t_risk_proposal (player_id);
CREATE INDEX IF NOT EXISTS idx_proposal_sla ON t_risk_proposal (sla_deadline, status) WHERE status IN (1, 2);

-- ---------------------------------------------------------------------
-- Table: t_geo_restriction (Restricted Jurisdictions)
-- ---------------------------------------------------------------------
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_geo_country_tenant ON t_geo_restriction (country_code, tenant_id);

-- =====================================================================
-- PART 7: AGENT DOMAIN (from V14) - 8 tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- Table: t_agent_credit (Agent Credit Limit Tracking)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_agent_credit (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_credit_agent_tenant ON t_agent_credit (agent_id, tenant_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_agent_credit_parent ON t_agent_credit (parent_id, tenant_id) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- Table: t_settlement_record (Weekly/Monthly Settlement Records)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_settlement_record (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_agent_week ON t_settlement_record (agent_id, settlement_week, tenant_id);
CREATE INDEX IF NOT EXISTS idx_settlement_week ON t_settlement_record (settlement_week, tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_credit_allocation_audit (Immutable Audit Trail)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_credit_allocation_audit (
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

CREATE INDEX IF NOT EXISTS idx_audit_parent ON t_credit_allocation_audit (parent_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_child ON t_credit_allocation_audit (child_id, tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_affiliate_agent (Agent Master Data)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_affiliate_agent (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_affiliate_username_tenant ON t_affiliate_agent (username, tenant_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_affiliate_referral_code ON t_affiliate_agent (referral_code, tenant_id) WHERE referral_code IS NOT NULL AND deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_affiliate_parent ON t_affiliate_agent (parent_agent_id, tenant_id) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- Table: t_affiliate_hierarchy (Closure Table for Agent Tree)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_affiliate_hierarchy (
    hierarchy_id            BIGSERIAL       PRIMARY KEY,
    tenant_id               BIGINT          NOT NULL,
    ancestor_id             BIGINT          NOT NULL,
    descendant_id           BIGINT          NOT NULL,
    depth                   INT             NOT NULL DEFAULT 0,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE t_affiliate_hierarchy IS '代理層級 Closure Table';

CREATE UNIQUE INDEX IF NOT EXISTS uk_hierarchy_ancestor_descendant ON t_affiliate_hierarchy (ancestor_id, descendant_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_hierarchy_descendant ON t_affiliate_hierarchy (descendant_id, tenant_id);

-- ---------------------------------------------------------------------
-- Table: t_affiliate_commission_plan (Commission Tier Configuration)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_affiliate_commission_plan (
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

CREATE INDEX IF NOT EXISTS idx_plan_tenant_type ON t_affiliate_commission_plan (tenant_id, plan_type) WHERE deleted = FALSE;

-- ---------------------------------------------------------------------
-- Table: t_affiliate_commission_record (Commission Settlement Records)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_affiliate_commission_record (
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_commission_agent_date ON t_affiliate_commission_record (agent_id, settlement_date, tenant_id);
CREATE INDEX IF NOT EXISTS idx_commission_status ON t_affiliate_commission_record (tenant_id, status);

-- ---------------------------------------------------------------------
-- Table: t_affiliate_adjustment (Immutable Adjustment Ledger)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_affiliate_adjustment (
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

CREATE INDEX IF NOT EXISTS idx_adjustment_agent ON t_affiliate_adjustment (agent_id, tenant_id);

-- =====================================================================
-- RLS POLICIES (Row-Level Security) - All 30 tables
-- =====================================================================

-- PART 1: Wallet Domain (4 tables)
ALTER TABLE t_wallet ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_bonus_ext ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_lock ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_wallet;
CREATE POLICY tenant_isolation ON t_wallet
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_wallet_transaction;
CREATE POLICY tenant_isolation ON t_wallet_transaction
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_wallet_bonus_ext;
CREATE POLICY tenant_isolation ON t_wallet_bonus_ext
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_wallet_lock;
CREATE POLICY tenant_isolation ON t_wallet_lock
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 2: Payment Domain (2 tables)
ALTER TABLE t_payment_order ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_psp ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_payment_order;
CREATE POLICY tenant_isolation ON t_payment_order
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_psp;
CREATE POLICY tenant_isolation ON t_psp
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 3: Player Domain (4 tables)
ALTER TABLE t_player ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_kyc_document ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_vip_change_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_player_audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_player;
CREATE POLICY tenant_isolation ON t_player
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_kyc_document;
CREATE POLICY tenant_isolation ON t_kyc_document
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_vip_change_log;
CREATE POLICY tenant_isolation ON t_vip_change_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_player_audit_log;
CREATE POLICY tenant_isolation ON t_player_audit_log
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 4: Game Domain (5 tables)
ALTER TABLE t_game_provider ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game_round ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_reconciliation ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_game_weight_config ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_game_provider;
CREATE POLICY tenant_isolation ON t_game_provider
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_game;
CREATE POLICY tenant_isolation ON t_game
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_game_round;
CREATE POLICY tenant_isolation ON t_game_round
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_reconciliation;
CREATE POLICY tenant_isolation ON t_reconciliation
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_game_weight_config;
CREATE POLICY tenant_isolation ON t_game_weight_config
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 5: Activity Domain (2 tables)
ALTER TABLE t_promotion_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_player_bonus_record ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_promotion_rule;
CREATE POLICY tenant_isolation ON t_promotion_rule
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_player_bonus_record;
CREATE POLICY tenant_isolation ON t_player_bonus_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 6: Risk Domain (5 tables)
ALTER TABLE t_risk_rule_param ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_assessment ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_score ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_risk_proposal ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_geo_restriction ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_risk_rule_param;
CREATE POLICY tenant_isolation ON t_risk_rule_param
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_risk_assessment;
CREATE POLICY tenant_isolation ON t_risk_assessment
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_risk_score;
CREATE POLICY tenant_isolation ON t_risk_score
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_risk_proposal;
CREATE POLICY tenant_isolation ON t_risk_proposal
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_geo_restriction;
CREATE POLICY tenant_isolation ON t_geo_restriction
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- PART 7: Agent Domain (8 tables)
ALTER TABLE t_agent_credit ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_settlement_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_credit_allocation_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_agent ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_hierarchy ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_commission_plan ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_commission_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_affiliate_adjustment ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON t_agent_credit;
CREATE POLICY tenant_isolation ON t_agent_credit
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_settlement_record;
CREATE POLICY tenant_isolation ON t_settlement_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_credit_allocation_audit;
CREATE POLICY tenant_isolation ON t_credit_allocation_audit
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_affiliate_agent;
CREATE POLICY tenant_isolation ON t_affiliate_agent
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_affiliate_hierarchy;
CREATE POLICY tenant_isolation ON t_affiliate_hierarchy
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_affiliate_commission_plan;
CREATE POLICY tenant_isolation ON t_affiliate_commission_plan
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_affiliate_commission_record;
CREATE POLICY tenant_isolation ON t_affiliate_commission_record
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

DROP POLICY IF EXISTS tenant_isolation ON t_affiliate_adjustment;
CREATE POLICY tenant_isolation ON t_affiliate_adjustment
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- GRANT PERMISSIONS - All 30 tables
-- =====================================================================

-- PART 1: Wallet Domain
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_transaction TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_bonus_ext TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_lock TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_wallet_wallet_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_transaction_transaction_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_bonus_ext_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_lock_lock_id_seq TO smartadmin_app;

-- PART 2: Payment Domain
GRANT SELECT, INSERT, UPDATE, DELETE ON t_payment_order TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_psp TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_payment_order_payment_order_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_psp_psp_id_seq TO smartadmin_app;

-- PART 3: Player Domain
GRANT SELECT, INSERT, UPDATE, DELETE ON t_player TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_kyc_document TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_vip_change_log TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_player_audit_log TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_player_player_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_kyc_document_kyc_document_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_vip_change_log_log_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_player_audit_log_audit_id_seq TO smartadmin_app;

-- PART 4: Game Domain
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_provider TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_round TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_reconciliation TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_game_weight_config TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_game_provider_provider_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_game_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_round_round_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_reconciliation_reconciliation_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_game_weight_config_config_id_seq TO smartadmin_app;

-- PART 5: Activity Domain
GRANT SELECT, INSERT, UPDATE, DELETE ON t_promotion_rule TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_player_bonus_record TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_promotion_rule_rule_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_player_bonus_record_record_id_seq TO smartadmin_app;

-- PART 6: Risk Domain
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

-- PART 7: Agent Domain
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

-- =====================================================================
-- End of V2__igaming_domain_tables.sql
-- =====================================================================
