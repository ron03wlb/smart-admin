-- V002.5: Create Wallet Bonus Extension Table
-- Author: iGaming Team
-- Date: 2026-03-19
-- Description: Per-activity bonus detail tracking for wagering progress

-- ============================================================================
-- Table: t_wallet_bonus_ext
-- Description: Wallet bonus extension — per-activity bonus detail tracking
-- ============================================================================

CREATE TABLE t_wallet_bonus_ext (
    -- Primary Key
    id                  BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    wallet_id           BIGINT NOT NULL,
    bonus_id            BIGINT NOT NULL,              -- FK to t_player_bonus_record.record_id

    -- Bonus Tracking
    balance             NUMERIC(19,4) NOT NULL DEFAULT 0,
    wagering_requirement NUMERIC(19,4) NOT NULL,
    wagered_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,

    -- Expiration & Restrictions
    expires_at          TIMESTAMPTZ,
    game_restriction    TEXT,                          -- JSONB string
    status              INTEGER NOT NULL,              -- BonusStatusEnum

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Soft Delete
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_wallet_bonus_ext_wallet
        FOREIGN KEY (wallet_id) REFERENCES t_wallet (wallet_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_bonus_ext_bonus
        FOREIGN KEY (bonus_id) REFERENCES t_player_bonus_record (record_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_bonus_ext_balance_non_negative
        CHECK (balance >= 0),
    CONSTRAINT chk_bonus_ext_wagered_valid
        CHECK (wagered_amount >= 0 AND wagered_amount <= wagering_requirement)
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- Wallet lookup index (for player bonus balance queries)
CREATE INDEX idx_wallet_bonus_ext_wallet ON t_wallet_bonus_ext (wallet_id);

-- Bonus lookup index (for bonus record tracking)
CREATE INDEX idx_wallet_bonus_ext_bonus ON t_wallet_bonus_ext (bonus_id);

-- Status query index (for active bonus filtering)
CREATE INDEX idx_wallet_bonus_ext_status ON t_wallet_bonus_ext (tenant_id, status);

-- Tenant isolation index
CREATE INDEX idx_wallet_bonus_ext_tenant ON t_wallet_bonus_ext (tenant_id) WHERE deleted = false;

-- ============================================================================
-- Table Comments
-- ============================================================================

COMMENT ON TABLE t_wallet_bonus_ext IS
'Wallet bonus extension — per-activity bonus detail tracking. Each bonus activity creates a separate record tracking its own balance and wagering progress. Consistency rule: t_wallet.balance(BONUS) = SUM(t_wallet_bonus_ext.balance WHERE status=ACTIVE).';

-- ============================================================================
-- Column Comments
-- ============================================================================

COMMENT ON COLUMN t_wallet_bonus_ext.id IS 'Bonus extension unique identifier (auto-increment)';
COMMENT ON COLUMN t_wallet_bonus_ext.wallet_id IS 'Wallet ID (foreign key to t_wallet)';
COMMENT ON COLUMN t_wallet_bonus_ext.bonus_id IS 'Bonus record ID (foreign key to t_player_bonus_record)';
COMMENT ON COLUMN t_wallet_bonus_ext.balance IS 'Remaining bonus balance for this activity (DECIMAL(19,4))';
COMMENT ON COLUMN t_wallet_bonus_ext.wagering_requirement IS 'Required valid turnover to unlock bonus (DECIMAL(19,4))';
COMMENT ON COLUMN t_wallet_bonus_ext.wagered_amount IS 'Accumulated valid turnover progress (DECIMAL(19,4))';
COMMENT ON COLUMN t_wallet_bonus_ext.expires_at IS 'Bonus expiration time (NULL = never expires)';
COMMENT ON COLUMN t_wallet_bonus_ext.game_restriction IS 'Game restriction rules as JSONB string (e.g., {"allowed_categories": [1, 3]})';
COMMENT ON COLUMN t_wallet_bonus_ext.status IS 'Bonus status: 1=ACTIVE, 2=COMPLETED, 3=EXPIRED, 4=FORFEITED (see BonusStatusEnum)';
COMMENT ON COLUMN t_wallet_bonus_ext.tenant_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN t_wallet_bonus_ext.deleted IS 'Soft delete flag';
COMMENT ON COLUMN t_wallet_bonus_ext.create_time IS 'Record creation timestamp';
COMMENT ON COLUMN t_wallet_bonus_ext.update_time IS 'Record last update timestamp';
