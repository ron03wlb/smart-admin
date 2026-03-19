-- V002: Create Wallet Module Tables
-- Author: iGaming Team
-- Date: 2026-03-19
-- Description: Wallet, transaction log, and fund lock tables for seamless wallet architecture

-- ============================================================================
-- Table: t_wallet
-- Description: Wallet master table (one wallet per player per type per tenant)
-- ============================================================================

CREATE TABLE t_wallet (
    -- Primary Key
    wallet_id           BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Player Association
    player_id           BIGINT NOT NULL,

    -- Wallet Configuration
    currency_code       VARCHAR(3) NOT NULL,         -- ISO 4217: USD, EUR, etc.
    wallet_type         INTEGER NOT NULL,            -- WalletTypeEnum: 1=CASH, 2=BONUS, 3=CREDIT

    -- Balance Tracking (DECIMAL(19,4) for precision to 0.0001)
    balance             NUMERIC(19,4) NOT NULL DEFAULT 0,
    locked_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,  -- SUM of t_wallet_lock.lock_amount

    -- Optimistic Lock & Soft Delete
    version             INTEGER NOT NULL DEFAULT 0,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraint
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_wallet_locked_non_negative CHECK (locked_amount >= 0),
    CONSTRAINT chk_wallet_locked_not_exceed_balance CHECK (locked_amount <= balance)
);

-- ============================================================================
-- Table: t_wallet_transaction
-- Description: Immutable append-only transaction log (double-entry style)
-- ============================================================================

CREATE TABLE t_wallet_transaction (
    -- Primary Key
    transaction_id      BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Player & Wallet Association
    player_id           BIGINT NOT NULL,
    wallet_id           BIGINT NOT NULL,

    -- Transaction Details
    transaction_type    INTEGER NOT NULL,            -- TransactionTypeEnum: 1=DEPOSIT, 2=WITHDRAW, 3=BET, 4=WIN, etc.
    amount              NUMERIC(19,4) NOT NULL,      -- Positive for credit, negative for debit
    balance_before      NUMERIC(19,4) NOT NULL,
    balance_after       NUMERIC(19,4) NOT NULL,

    -- Idempotency & Audit Trail
    request_id          VARCHAR(100) NOT NULL,       -- Unique idempotency key
    reference_type      VARCHAR(50),                 -- E.g., "DEPOSIT_ORDER", "BET_ORDER", "WITHDRAWAL_ORDER"
    reference_id        VARCHAR(100),                -- External system reference ID

    -- Description
    description         TEXT,

    -- SmartAdmin Base Entity Fields (NO update_time - transactions are immutable)
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Table: t_wallet_lock
-- Description: Fund lock detail records (dual-track design with t_wallet.locked_amount)
-- ============================================================================

CREATE TABLE t_wallet_lock (
    -- Primary Key
    lock_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Wallet Association
    wallet_id           BIGINT NOT NULL,

    -- Lock Details
    lock_amount         NUMERIC(19,4) NOT NULL,
    lock_reason         INTEGER NOT NULL,            -- LockReasonEnum: 1=PENDING_WITHDRAWAL, 2=WAGERING_REQUIREMENT, 3=RISK_HOLD
    reference_id        VARCHAR(100),                -- Reference to order/bonus ID
    expires_at          TIMESTAMPTZ,                 -- Optional expiration time

    -- SmartAdmin Base Entity Fields (NO update_time - locks are created/deleted, not updated)
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraint
    CONSTRAINT chk_wallet_lock_amount_positive CHECK (lock_amount > 0)
);

-- ============================================================================
-- Indexes for t_wallet
-- ============================================================================

-- Unique constraint: One wallet per (tenant, player, wallet_type, currency)
CREATE UNIQUE INDEX uk_wallet_player_type_currency
    ON t_wallet (tenant_id, player_id, wallet_type, currency_code)
    WHERE deleted = FALSE;

-- Player lookup index (for all wallets of a player)
CREATE INDEX idx_wallet_player
    ON t_wallet (tenant_id, player_id)
    WHERE deleted = FALSE;

-- Tenant isolation index
CREATE INDEX idx_wallet_tenant
    ON t_wallet (tenant_id)
    WHERE deleted = FALSE;

-- ============================================================================
-- Indexes for t_wallet_transaction
-- ============================================================================

-- Unique constraint for idempotency (prevent duplicate transactions)
CREATE UNIQUE INDEX uk_wallet_transaction_request_id
    ON t_wallet_transaction (tenant_id, request_id);

-- Wallet transaction history index (for wallet statement queries)
CREATE INDEX idx_wallet_transaction_wallet_time
    ON t_wallet_transaction (wallet_id, create_time DESC);

-- Player transaction history index (for player audit trail)
CREATE INDEX idx_wallet_transaction_player_time
    ON t_wallet_transaction (tenant_id, player_id, create_time DESC);

-- Reference lookup index (for transaction reconciliation)
CREATE INDEX idx_wallet_transaction_reference
    ON t_wallet_transaction (reference_type, reference_id);

-- Transaction type analysis index
CREATE INDEX idx_wallet_transaction_type
    ON t_wallet_transaction (tenant_id, transaction_type, create_time DESC);

-- ============================================================================
-- Indexes for t_wallet_lock
-- ============================================================================

-- Wallet lock aggregation index (for SUM calculation)
CREATE INDEX idx_wallet_lock_wallet
    ON t_wallet_lock (wallet_id);

-- Reference lookup index (for unlock operations)
CREATE INDEX idx_wallet_lock_reference
    ON t_wallet_lock (reference_id);

-- Expiration cleanup index (for scheduled jobs)
CREATE INDEX idx_wallet_lock_expires
    ON t_wallet_lock (expires_at)
    WHERE expires_at IS NOT NULL;

-- Lock reason analysis index
CREATE INDEX idx_wallet_lock_reason
    ON t_wallet_lock (tenant_id, lock_reason);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================

-- Foreign key: t_wallet.player_id -> t_player.player_id
ALTER TABLE t_wallet
    ADD CONSTRAINT fk_wallet_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;  -- Cannot delete player if wallet exists

-- Foreign key: t_wallet_transaction.wallet_id -> t_wallet.wallet_id
ALTER TABLE t_wallet_transaction
    ADD CONSTRAINT fk_wallet_transaction_wallet
    FOREIGN KEY (wallet_id)
    REFERENCES t_wallet (wallet_id)
    ON DELETE RESTRICT;  -- Cannot delete wallet if transactions exist

-- Foreign key: t_wallet_transaction.player_id -> t_player.player_id
ALTER TABLE t_wallet_transaction
    ADD CONSTRAINT fk_wallet_transaction_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;

-- Foreign key: t_wallet_lock.wallet_id -> t_wallet.wallet_id
ALTER TABLE t_wallet_lock
    ADD CONSTRAINT fk_wallet_lock_wallet
    FOREIGN KEY (wallet_id)
    REFERENCES t_wallet (wallet_id)
    ON DELETE RESTRICT;  -- Cannot delete wallet if locks exist

-- ============================================================================
-- Table Comments
-- ============================================================================

COMMENT ON TABLE t_wallet IS
'Wallet master table for seamless wallet architecture. Each player has one wallet per type (CASH/BONUS/CREDIT) per currency. Balance and locked_amount use DECIMAL(19,4) precision. Dual-track design: t_wallet.locked_amount = SUM(t_wallet_lock.lock_amount).';

COMMENT ON TABLE t_wallet_transaction IS
'Immutable append-only transaction log with double-entry style tracking (balance_before + amount = balance_after). NO update_time column as transactions are never modified. request_id provides Layer 2 idempotency defense.';

COMMENT ON TABLE t_wallet_lock IS
'Fund lock detail records for pending withdrawals, wagering requirements, or risk holds. Dual-track consistency: SUM(lock_amount) must equal t_wallet.locked_amount. NO update_time as locks are created/deleted, not updated.';

-- ============================================================================
-- Column Comments for t_wallet
-- ============================================================================

COMMENT ON COLUMN t_wallet.wallet_id IS 'Wallet unique identifier (auto-increment)';
COMMENT ON COLUMN t_wallet.tenant_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN t_wallet.player_id IS 'Player ID (foreign key to t_player)';
COMMENT ON COLUMN t_wallet.currency_code IS 'ISO 4217 currency code (USD, EUR, CNY, etc.)';
COMMENT ON COLUMN t_wallet.wallet_type IS 'Wallet type: 1=CASH, 2=BONUS, 3=CREDIT (see WalletTypeEnum)';
COMMENT ON COLUMN t_wallet.balance IS 'Current wallet balance (DECIMAL(19,4) for 0.0001 precision)';
COMMENT ON COLUMN t_wallet.locked_amount IS 'Sum of all active locks in t_wallet_lock (for dual-track consistency)';
COMMENT ON COLUMN t_wallet.version IS 'Optimistic lock version for concurrent transaction protection';
COMMENT ON COLUMN t_wallet.deleted IS 'Soft delete flag';

-- ============================================================================
-- Column Comments for t_wallet_transaction
-- ============================================================================

COMMENT ON COLUMN t_wallet_transaction.transaction_id IS 'Transaction unique identifier (auto-increment)';
COMMENT ON COLUMN t_wallet_transaction.tenant_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN t_wallet_transaction.player_id IS 'Player ID (foreign key to t_player)';
COMMENT ON COLUMN t_wallet_transaction.wallet_id IS 'Wallet ID (foreign key to t_wallet)';
COMMENT ON COLUMN t_wallet_transaction.transaction_type IS 'Transaction type: 1=DEPOSIT, 2=WITHDRAW, 3=BET, 4=WIN, 5=REFUND, etc. (see TransactionTypeEnum)';
COMMENT ON COLUMN t_wallet_transaction.amount IS 'Transaction amount (positive for credit, negative for debit)';
COMMENT ON COLUMN t_wallet_transaction.balance_before IS 'Wallet balance before transaction';
COMMENT ON COLUMN t_wallet_transaction.balance_after IS 'Wallet balance after transaction (balance_before + amount = balance_after)';
COMMENT ON COLUMN t_wallet_transaction.request_id IS 'Unique idempotency key for Layer 2 duplicate prevention';
COMMENT ON COLUMN t_wallet_transaction.reference_type IS 'Reference type (e.g., DEPOSIT_ORDER, BET_ORDER, WITHDRAWAL_ORDER)';
COMMENT ON COLUMN t_wallet_transaction.reference_id IS 'External system reference ID for reconciliation';
COMMENT ON COLUMN t_wallet_transaction.description IS 'Human-readable transaction description';
COMMENT ON COLUMN t_wallet_transaction.create_time IS 'Transaction timestamp (immutable - no update_time column)';

-- ============================================================================
-- Column Comments for t_wallet_lock
-- ============================================================================

COMMENT ON COLUMN t_wallet_lock.lock_id IS 'Lock unique identifier (auto-increment)';
COMMENT ON COLUMN t_wallet_lock.tenant_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN t_wallet_lock.wallet_id IS 'Wallet ID (foreign key to t_wallet)';
COMMENT ON COLUMN t_wallet_lock.lock_amount IS 'Locked amount (must be positive)';
COMMENT ON COLUMN t_wallet_lock.lock_reason IS 'Lock reason: 1=PENDING_WITHDRAWAL, 2=WAGERING_REQUIREMENT, 3=RISK_HOLD (see LockReasonEnum)';
COMMENT ON COLUMN t_wallet_lock.reference_id IS 'Reference to order/bonus ID for tracking';
COMMENT ON COLUMN t_wallet_lock.expires_at IS 'Optional lock expiration time (NULL = no expiration)';
COMMENT ON COLUMN t_wallet_lock.create_time IS 'Lock creation timestamp (immutable - no update_time column)';
