-- =====================================================================
-- V8: iGaming Wallet Tables — Phase 1 Data Model (ADR-001, ADR-006)
-- =====================================================================
-- Creates 4 core wallet tables:
--   - t_wallet: Master wallet (CASH/BONUS/CREDIT)
--   - t_wallet_transaction: Immutable transaction log (append-only)
--   - t_wallet_bonus_ext: Bonus detail extension per activity
--   - t_wallet_lock: Lock detail records (dual-track with t_wallet.locked_amount)
--
-- Conventions:
--   - Singular table names (ADR-001)
--   - DECIMAL(19,4) for all monetary amounts (ADR-006)
--   - TIMESTAMPTZ for all timestamps (G2 OffsetDateTime migration)
--   - SMALLINT for enum fields (BaseEnum Integer pattern)
--   - tenant_id on all tables (Multi-Tenant G1)
-- =====================================================================

-- =====================================================================
-- Part A: t_wallet (Master Wallet)
-- =====================================================================

CREATE TABLE t_wallet (
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
COMMENT ON COLUMN t_wallet.wallet_id IS '錢包唯一標識';
COMMENT ON COLUMN t_wallet.player_id IS '玩家 ID';
COMMENT ON COLUMN t_wallet.tenant_id IS '租戶 ID';
COMMENT ON COLUMN t_wallet.currency_code IS '幣種代碼 (ISO 4217)';
COMMENT ON COLUMN t_wallet.wallet_type IS '錢包類型: 1=現金, 2=紅利, 3=信用';
COMMENT ON COLUMN t_wallet.balance IS '餘額 DECIMAL(19,4)';
COMMENT ON COLUMN t_wallet.locked_amount IS '鎖定金額 (= SUM(t_wallet_lock.lock_amount))';
COMMENT ON COLUMN t_wallet.version IS '樂觀鎖版本號';
COMMENT ON COLUMN t_wallet.deleted IS '軟刪除標記';

CREATE INDEX idx_wallet_player_tenant ON t_wallet (player_id, tenant_id);
CREATE INDEX idx_wallet_tenant ON t_wallet (tenant_id);

-- =====================================================================
-- Part B: t_wallet_transaction (Transaction Log — Append-Only)
-- =====================================================================

CREATE TABLE t_wallet_transaction (
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
    CONSTRAINT ck_wallet_transaction_type CHECK (transaction_type IN (1, 2, 3, 4, 5, 6))
);

COMMENT ON TABLE t_wallet_transaction IS '錢包交易記錄 (不可變)';
COMMENT ON COLUMN t_wallet_transaction.transaction_id IS '交易唯一標識';
COMMENT ON COLUMN t_wallet_transaction.wallet_id IS '所屬錢包 ID';
COMMENT ON COLUMN t_wallet_transaction.player_id IS '玩家 ID';
COMMENT ON COLUMN t_wallet_transaction.transaction_type IS '交易類型: 1=存款, 2=提款, 3=下注, 4=派彩, 5=紅利, 6=調整';
COMMENT ON COLUMN t_wallet_transaction.amount IS '交易金額';
COMMENT ON COLUMN t_wallet_transaction.balance_before IS '交易前餘額';
COMMENT ON COLUMN t_wallet_transaction.balance_after IS '交易後餘額';
COMMENT ON COLUMN t_wallet_transaction.request_id IS '冪等鍵 (Idempotency Layer 2)';
COMMENT ON COLUMN t_wallet_transaction.reference_type IS '關聯類型 (DEPOSIT_ORDER, ROUND, BONUS 等)';
COMMENT ON COLUMN t_wallet_transaction.reference_id IS '關聯 ID';

CREATE INDEX idx_wallet_txn_wallet_time ON t_wallet_transaction (wallet_id, create_time DESC);
CREATE INDEX idx_wallet_txn_player_type ON t_wallet_transaction (player_id, transaction_type, create_time DESC);
CREATE INDEX idx_wallet_txn_tenant ON t_wallet_transaction (tenant_id, create_time DESC);
CREATE INDEX idx_wallet_txn_ref ON t_wallet_transaction (reference_type, reference_id);

-- =====================================================================
-- Part C: t_wallet_bonus_ext (Bonus Detail Extension)
-- =====================================================================

CREATE TABLE t_wallet_bonus_ext (
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

COMMENT ON TABLE t_wallet_bonus_ext IS '紅利錢包明細 (每筆活動紅利)';
COMMENT ON COLUMN t_wallet_bonus_ext.bonus_id IS '紅利活動 ID';
COMMENT ON COLUMN t_wallet_bonus_ext.balance IS '該筆紅利剩餘金額';
COMMENT ON COLUMN t_wallet_bonus_ext.wagering_requirement IS '流水要求 (有效投注額)';
COMMENT ON COLUMN t_wallet_bonus_ext.wagered_amount IS '已完成流水';
COMMENT ON COLUMN t_wallet_bonus_ext.expires_at IS '紅利過期時間';
COMMENT ON COLUMN t_wallet_bonus_ext.game_restriction IS '遊戲限制 (JSONB)';
COMMENT ON COLUMN t_wallet_bonus_ext.status IS '狀態: 1=有效, 2=過期, 3=完成, 4=沒收';

CREATE INDEX idx_wallet_bonus_ext_wallet_status ON t_wallet_bonus_ext (wallet_id, status);
CREATE INDEX idx_wallet_bonus_ext_expires ON t_wallet_bonus_ext (expires_at, status) WHERE status = 1;

-- =====================================================================
-- Part D: t_wallet_lock (Lock Detail Records)
-- =====================================================================

CREATE TABLE t_wallet_lock (
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

COMMENT ON TABLE t_wallet_lock IS '錢包鎖定明細 (雙軌設計)';
COMMENT ON COLUMN t_wallet_lock.lock_amount IS '鎖定金額';
COMMENT ON COLUMN t_wallet_lock.lock_reason IS '鎖定原因: 1=下注待結算, 2=提款待審核, 3=風控凍結';
COMMENT ON COLUMN t_wallet_lock.reference_id IS '關聯訂單/注單 ID';
COMMENT ON COLUMN t_wallet_lock.expires_at IS '鎖定過期時間';

CREATE INDEX idx_wallet_lock_wallet_ref ON t_wallet_lock (wallet_id, reference_id);
CREATE INDEX idx_wallet_lock_expires ON t_wallet_lock (expires_at) WHERE expires_at IS NOT NULL;

-- =====================================================================
-- Part E: RLS Policies (Row-Level Security)
-- =====================================================================

ALTER TABLE t_wallet ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_transaction ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_bonus_ext ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_wallet_lock ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON t_wallet
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_wallet_transaction
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_wallet_bonus_ext
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

CREATE POLICY tenant_isolation ON t_wallet_lock
    FOR ALL TO smartadmin_app
    USING (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::BIGINT);

-- =====================================================================
-- Part F: Grant Permissions to Application Role
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_transaction TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_bonus_ext TO smartadmin_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON t_wallet_lock TO smartadmin_app;

GRANT USAGE, SELECT ON SEQUENCE t_wallet_wallet_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_transaction_transaction_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_bonus_ext_id_seq TO smartadmin_app;
GRANT USAGE, SELECT ON SEQUENCE t_wallet_lock_lock_id_seq TO smartadmin_app;
