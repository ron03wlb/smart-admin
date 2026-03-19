-- V004: Create Activity Module Tables
-- Author: iGaming Team
-- Date: 2026-03-19
-- Description: Promotion, bonus, and turnover calculation rule tables

-- ============================================================================
-- Section 1: Promotion & Bonus Tables
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: t_promotion_rule
-- Description: Promotion rule master table for deposit bonuses and campaigns
-- ----------------------------------------------------------------------------

CREATE TABLE t_promotion_rule (
    -- Primary Key
    rule_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Promotion Information
    promotion_code      VARCHAR(50) NOT NULL,         -- Unique promotion code
    promotion_name      VARCHAR(100) NOT NULL,        -- Display name
    promotion_type      INTEGER NOT NULL,             -- PromotionTypeEnum: 1=FIRST_DEPOSIT, 2=RELOAD, etc.
    status              INTEGER NOT NULL,             -- PromotionStatusEnum: 1=ACTIVE, 2=SUSPENDED, 3=EXPIRED

    -- Time Range
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ NOT NULL,

    -- Bonus Configuration
    min_deposit         NUMERIC(19,4),                -- Minimum deposit required (nullable = no minimum)
    bonus_rate          NUMERIC(5,4) NOT NULL,        -- Bonus rate: 1.0000 = 100%
    max_bonus           NUMERIC(19,4),                -- Maximum bonus amount (nullable = unlimited)
    wagering_multiplier NUMERIC(5,2) NOT NULL,        -- Wagering requirement: 20.00 = 20x turnover

    -- Restriction & Limits
    max_claims_per_player INTEGER,                    -- Max claims per player (nullable = unlimited)
    bonus_expiry_days   INTEGER,                      -- Bonus validity days (nullable = never expires)
    game_restriction    JSONB,                        -- Game restriction rules as JSONB

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_promotion_bonus_rate_valid CHECK (bonus_rate >= 0 AND bonus_rate <= 10),
    CONSTRAINT chk_promotion_wagering_multiplier_valid CHECK (wagering_multiplier > 0),
    CONSTRAINT chk_promotion_time_range_valid CHECK (start_time < end_time)
);

-- Unique constraint on promotion_code per tenant
CREATE UNIQUE INDEX uk_promotion_code_tenant
    ON t_promotion_rule (tenant_id, promotion_code)
    WHERE deleted = FALSE;

-- Active promotions query index
CREATE INDEX idx_promotion_status_time
    ON t_promotion_rule (tenant_id, status, start_time, end_time)
    WHERE deleted = FALSE;

COMMENT ON TABLE t_promotion_rule IS
'Promotion rule master table for deposit bonuses, reload bonuses, and campaigns. Supports wagering requirements, time windows, and game restrictions via JSONB fields.';

-- ----------------------------------------------------------------------------
-- Table: t_player_bonus_record
-- Description: Player bonus claim records with wagering progress tracking
-- ----------------------------------------------------------------------------

CREATE TABLE t_player_bonus_record (
    -- Primary Key
    record_id           BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Foreign Keys
    player_id           BIGINT NOT NULL,              -- FK to t_player
    rule_id             BIGINT NOT NULL,              -- FK to t_promotion_rule

    -- Idempotency & Amounts
    claim_id            VARCHAR(100) NOT NULL,        -- Unique idempotency key
    bonus_amount        NUMERIC(19,4) NOT NULL,       -- Actual bonus amount awarded
    wagering_required   NUMERIC(19,4) NOT NULL,       -- Total wagering required (bonus * multiplier)
    wagering_completed  NUMERIC(19,4) NOT NULL DEFAULT 0,  -- Wagering progress

    -- Status & Timestamps
    status              INTEGER NOT NULL,             -- BonusRecordStatusEnum: 1=ACTIVE, 2=COMPLETED, etc.
    claimed_at          TIMESTAMPTZ NOT NULL,
    completed_at        TIMESTAMPTZ,                  -- NULL if not yet completed
    expired_at          TIMESTAMPTZ,                  -- NULL if never expires

    -- Wallet Extension Reference
    wallet_bonus_ext_id BIGINT,                       -- FK to t_wallet_bonus_ext (wallet module)

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_bonus_record_amounts_positive CHECK (bonus_amount > 0 AND wagering_required > 0),
    CONSTRAINT chk_bonus_record_wagering_progress CHECK (wagering_completed >= 0 AND wagering_completed <= wagering_required)
);

-- Unique constraint on claim_id (idempotency)
CREATE UNIQUE INDEX uk_bonus_record_claim_id
    ON t_player_bonus_record (tenant_id, claim_id)
    WHERE deleted = FALSE;

-- Player bonus history index
CREATE INDEX idx_bonus_record_player
    ON t_player_bonus_record (tenant_id, player_id, status, claimed_at DESC)
    WHERE deleted = FALSE;

-- Active bonuses query index (for wagering progress updates)
CREATE INDEX idx_bonus_record_active
    ON t_player_bonus_record (player_id, status)
    WHERE deleted = FALSE AND status = 1;  -- 1 = ACTIVE

-- Foreign key constraints
ALTER TABLE t_player_bonus_record
    ADD CONSTRAINT fk_bonus_record_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;

ALTER TABLE t_player_bonus_record
    ADD CONSTRAINT fk_bonus_record_rule
    FOREIGN KEY (rule_id)
    REFERENCES t_promotion_rule (rule_id)
    ON DELETE RESTRICT;

COMMENT ON TABLE t_player_bonus_record IS
'Player bonus claim records tracking wagering progress. Each record represents a single bonus claim with real-time wagering completion tracking. Idempotency enforced via claim_id UNIQUE constraint.';

-- ============================================================================
-- Section 2: Turnover Calculation Engine Rule Tables
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: t_turnover_game_weight_rule
-- Description: Game category weight configuration (Layer 3: Activity System)
-- ----------------------------------------------------------------------------

CREATE TABLE t_turnover_game_weight_rule (
    -- Primary Key
    rule_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Identification
    rule_code           VARCHAR(50) NOT NULL,         -- Unique rule code (e.g., GW_SLOTS_100)
    rule_name           VARCHAR(100) NOT NULL,        -- Display name

    -- Game Weight Configuration
    game_category       INTEGER NOT NULL,             -- GameCategoryEnum: 1=SLOTS, 2=LIVE_CASINO, etc.
    weight_percentage   NUMERIC(5,2) NOT NULL,        -- Weight: 0.00 ~ 100.00

    -- Priority & Effective Period
    priority            INTEGER NOT NULL DEFAULT 100, -- Higher number = higher priority
    effective_from      TIMESTAMPTZ,                  -- NULL = applies immediately
    effective_to        TIMESTAMPTZ,                  -- NULL = never expires

    -- Status & Metadata
    status              INTEGER NOT NULL,             -- TurnoverRuleStatusEnum: 1=ENABLED, 2=DISABLED
    remark              TEXT,

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_game_weight_percentage_valid CHECK (weight_percentage >= 0 AND weight_percentage <= 100)
);

-- Unique constraint on rule_code per tenant
CREATE UNIQUE INDEX uk_game_weight_rule_code
    ON t_turnover_game_weight_rule (tenant_id, rule_code)
    WHERE deleted = FALSE;

-- Game category lookup index (for turnover calculation)
CREATE INDEX idx_game_weight_category
    ON t_turnover_game_weight_rule (tenant_id, game_category, status, priority DESC)
    WHERE deleted = FALSE;

COMMENT ON TABLE t_turnover_game_weight_rule IS
'Game category weight rules for turnover calculation (Layer 3: Activity System). Defines contribution of each game category (Slots 100%, Live Casino 15%, etc.) to valid turnover. Supports effective date ranges and priority-based rule selection.';

-- ----------------------------------------------------------------------------
-- Table: t_turnover_odds_threshold_rule
-- Description: Odds threshold configuration (Layer 2: Finance Center)
-- ----------------------------------------------------------------------------

CREATE TABLE t_turnover_odds_threshold_rule (
    -- Primary Key
    rule_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Identification
    rule_code           VARCHAR(50) NOT NULL,         -- Unique rule code (e.g., OT_EUR_1_5)
    rule_name           VARCHAR(100) NOT NULL,        -- Display name

    -- Odds Threshold Configuration
    odds_type           INTEGER NOT NULL,             -- OddsTypeEnum: 1=EUROPEAN, 2=HONG_KONG, etc.
    threshold_value     NUMERIC(10,4) NOT NULL,       -- Threshold value (e.g., 1.5000)
    comparison_operator VARCHAR(2) NOT NULL,          -- Operator: '>=', '>', '<=', '<', '='

    -- Effective Period
    effective_from      TIMESTAMPTZ,                  -- NULL = applies immediately
    effective_to        TIMESTAMPTZ,                  -- NULL = never expires

    -- Status & Metadata
    status              INTEGER NOT NULL,             -- TurnoverRuleStatusEnum: 1=ENABLED, 2=DISABLED
    remark              TEXT,

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_odds_threshold_operator_valid CHECK (comparison_operator IN ('>=', '>', '<=', '<', '='))
);

-- Unique constraint on rule_code per tenant
CREATE UNIQUE INDEX uk_odds_threshold_rule_code
    ON t_turnover_odds_threshold_rule (tenant_id, rule_code)
    WHERE deleted = FALSE;

-- Odds type lookup index
CREATE INDEX idx_odds_threshold_type
    ON t_turnover_odds_threshold_rule (tenant_id, odds_type, status)
    WHERE deleted = FALSE;

COMMENT ON TABLE t_turnover_odds_threshold_rule IS
'Odds threshold rules for anti-arbitrage protection (Layer 2: Finance Center). Defines minimum odds requirements (e.g., European >= 1.5) for bets to qualify for turnover. Prevents bonus abuse via low-odds hedging strategies.';

-- ----------------------------------------------------------------------------
-- Table: t_turnover_risk_action_rule
-- Description: Risk engine action configuration (Layer 1: Risk Engine)
-- ----------------------------------------------------------------------------

CREATE TABLE t_turnover_risk_action_rule (
    -- Primary Key
    rule_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Identification
    rule_code           VARCHAR(50) NOT NULL,         -- Unique rule code (e.g., RA_PASS, RA_FLAG, RA_BLOCK)
    rule_name           VARCHAR(100) NOT NULL,        -- Display name

    -- Risk Action Configuration
    risk_level          INTEGER NOT NULL,             -- Risk level: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
    action_type         INTEGER NOT NULL,             -- RiskActionTypeEnum: 1=PASS, 2=FLAG, 3=BLOCK
    turnover_factor     NUMERIC(5,2) NOT NULL,        -- Turnover factor: 0.00 ~ 100.00
    allow_bet           BOOLEAN NOT NULL,             -- Allow betting flag
    create_proposal     BOOLEAN NOT NULL,             -- Create risk proposal flag

    -- Effective Period
    effective_from      TIMESTAMPTZ,                  -- NULL = applies immediately
    effective_to        TIMESTAMPTZ,                  -- NULL = never expires

    -- Status & Metadata
    status              INTEGER NOT NULL,             -- TurnoverRuleStatusEnum: 1=ENABLED, 2=DISABLED
    remark              TEXT,

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_risk_action_turnover_factor_valid CHECK (turnover_factor >= 0 AND turnover_factor <= 100)
);

-- Unique constraint on rule_code per tenant
CREATE UNIQUE INDEX uk_risk_action_rule_code
    ON t_turnover_risk_action_rule (tenant_id, rule_code)
    WHERE deleted = FALSE;

-- Risk level lookup index
CREATE INDEX idx_risk_action_level
    ON t_turnover_risk_action_rule (tenant_id, risk_level, status)
    WHERE deleted = FALSE;

COMMENT ON TABLE t_turnover_risk_action_rule IS
'Risk engine action rules for automated risk management (Layer 1: Risk Engine). Defines actions (PASS/FLAG/BLOCK) based on player risk level. Integrates with risk scoring system (t_risk_assessment) to make real-time decisions on betting and turnover calculation.';

-- ----------------------------------------------------------------------------
-- Table: t_turnover_status_factor_rule
-- Description: Settlement status factor configuration (Layer 2: Finance Center)
-- ----------------------------------------------------------------------------

CREATE TABLE t_turnover_status_factor_rule (
    -- Primary Key
    rule_id             BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Identification
    rule_code           VARCHAR(50) NOT NULL,         -- Unique rule code (e.g., SF_WIN_100, SF_DRAW_0)
    rule_name           VARCHAR(100) NOT NULL,        -- Display name

    -- Status Factor Configuration
    settlement_status   INTEGER NOT NULL,             -- SettlementStatusEnum: 1=WIN, 2=LOSS, 3=DRAW, etc.
    factor_percentage   NUMERIC(5,2) NOT NULL,        -- Factor: 0.00 ~ 100.00

    -- Effective Period
    effective_from      TIMESTAMPTZ,                  -- NULL = applies immediately
    effective_to        TIMESTAMPTZ,                  -- NULL = never expires

    -- Status & Metadata
    status              INTEGER NOT NULL,             -- TurnoverRuleStatusEnum: 1=ENABLED, 2=DISABLED
    remark              TEXT,

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Consistency Check Constraints
    CONSTRAINT chk_status_factor_percentage_valid CHECK (factor_percentage >= 0 AND factor_percentage <= 100)
);

-- Unique constraint on rule_code per tenant
CREATE UNIQUE INDEX uk_status_factor_rule_code
    ON t_turnover_status_factor_rule (tenant_id, rule_code)
    WHERE deleted = FALSE;

-- Settlement status lookup index
CREATE INDEX idx_status_factor_settlement
    ON t_turnover_status_factor_rule (tenant_id, settlement_status, status)
    WHERE deleted = FALSE;

COMMENT ON TABLE t_turnover_status_factor_rule IS
'Settlement status factor rules for turnover calculation (Layer 2: Finance Center). Defines how each settlement outcome (WIN/LOSS/DRAW/VOID) affects turnover. Uses "Standard Principal Method" where HALF_WIN/HALF_LOSS count as 100% (not 50%).';

-- ----------------------------------------------------------------------------
-- Table: t_turnover_rule_change_log
-- Description: Audit trail for all turnover rule modifications
-- ----------------------------------------------------------------------------

CREATE TABLE t_turnover_rule_change_log (
    -- Primary Key
    log_id              BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Reference
    rule_type           INTEGER NOT NULL,             -- 1=GameWeight, 2=StatusFactor, 3=OddsThreshold, 4=RiskAction
    rule_id             BIGINT NOT NULL,              -- FK to corresponding rule table
    rule_code           VARCHAR(50) NOT NULL,         -- Denormalized rule code

    -- Change Details
    operation_type      INTEGER NOT NULL,             -- 1=INSERT, 2=UPDATE, 3=DELETE, 4=RELOAD
    old_value           JSONB,                        -- NULL for INSERT
    new_value           JSONB,                        -- NULL for DELETE
    change_reason       TEXT,                         -- Required for UPDATE/DELETE

    -- Operator Information
    operator_id         BIGINT NOT NULL,              -- Employee ID who made the change
    operator_name       VARCHAR(50) NOT NULL,         -- Denormalized operator name

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rule change history query index
CREATE INDEX idx_rule_change_log_rule
    ON t_turnover_rule_change_log (tenant_id, rule_type, rule_id, create_time DESC);

-- Operator audit trail index
CREATE INDEX idx_rule_change_log_operator
    ON t_turnover_rule_change_log (tenant_id, operator_id, create_time DESC);

-- Change time range query index
CREATE INDEX idx_rule_change_log_time
    ON t_turnover_rule_change_log (tenant_id, create_time DESC);

COMMENT ON TABLE t_turnover_rule_change_log IS
'Audit trail for all turnover rule modifications. Records WHO (operator_id/name), WHAT (old_value vs new_value as JSONB), WHEN (create_time), and WHY (change_reason). Supports compliance requirements and debugging of rule changes.';

-- ============================================================================
-- Column Comments for t_promotion_rule
-- ============================================================================

COMMENT ON COLUMN t_promotion_rule.promotion_type IS 'Promotion type: 1=FIRST_DEPOSIT, 2=RELOAD, 3=CASHBACK, 4=REFERRAL, etc. (see PromotionTypeEnum)';
COMMENT ON COLUMN t_promotion_rule.status IS 'Status: 1=ACTIVE, 2=SUSPENDED, 3=EXPIRED (see PromotionStatusEnum)';
COMMENT ON COLUMN t_promotion_rule.bonus_rate IS 'Bonus rate: 0.5000 = 50%, 1.0000 = 100%, 2.0000 = 200%';
COMMENT ON COLUMN t_promotion_rule.wagering_multiplier IS 'Wagering multiplier: 20.00 = player must wager 20x (deposit + bonus) to unlock';
COMMENT ON COLUMN t_promotion_rule.game_restriction IS 'Game restriction rules as JSONB (e.g., {"allowed_categories": [1, 3], "excluded_games": [123, 456]})';

-- ============================================================================
-- Column Comments for t_player_bonus_record
-- ============================================================================

COMMENT ON COLUMN t_player_bonus_record.status IS 'Bonus status: 1=ACTIVE, 2=COMPLETED, 3=EXPIRED, 4=FORFEITED (see BonusRecordStatusEnum)';
COMMENT ON COLUMN t_player_bonus_record.wagering_required IS 'Total wagering required = bonus_amount * wagering_multiplier';
COMMENT ON COLUMN t_player_bonus_record.wagering_completed IS 'Current wagering progress (updated in real-time via turnover calculation)';
COMMENT ON COLUMN t_player_bonus_record.wallet_bonus_ext_id IS 'Reference to t_wallet_bonus_ext.id in wallet module (for bonus account tracking)';

-- ============================================================================
-- Column Comments for Turnover Rule Tables
-- ============================================================================

COMMENT ON COLUMN t_turnover_game_weight_rule.game_category IS 'Game category: 1=SLOTS, 2=LIVE_CASINO, 3=SPORTS, 4=POKER, 5=TABLE_GAMES, 6=LOTTERY (see GameCategoryEnum)';
COMMENT ON COLUMN t_turnover_game_weight_rule.weight_percentage IS 'Weight percentage: 100.00 = full weight (slots), 15.00 = 15% weight (live casino)';

COMMENT ON COLUMN t_turnover_odds_threshold_rule.odds_type IS 'Odds type: 1=EUROPEAN, 2=HONG_KONG, 3=MALAY, 4=INDONESIAN (see OddsTypeEnum)';
COMMENT ON COLUMN t_turnover_odds_threshold_rule.threshold_value IS 'Threshold value: 1.5000 for European odds, 0.5000 for HK odds, etc.';

COMMENT ON COLUMN t_turnover_risk_action_rule.risk_level IS 'Risk level: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL';
COMMENT ON COLUMN t_turnover_risk_action_rule.action_type IS 'Action type: 1=PASS (allow), 2=FLAG (review), 3=BLOCK (reject) (see RiskActionTypeEnum)';
COMMENT ON COLUMN t_turnover_risk_action_rule.turnover_factor IS 'Turnover factor: 100.00 = full turnover, 0.00 = no turnover (for blocked bets)';

COMMENT ON COLUMN t_turnover_status_factor_rule.settlement_status IS 'Settlement status: 1=WIN, 2=LOSS, 3=DRAW, 4=TIE, 5=VOID, 6=CANCEL, 7=HALF_WIN, 8=HALF_LOSS, 9=RUNNING (see SettlementStatusEnum)';
COMMENT ON COLUMN t_turnover_status_factor_rule.factor_percentage IS 'Status factor: 100.00 = full turnover (WIN/LOSS), 0.00 = no turnover (DRAW/VOID)';

COMMENT ON COLUMN t_turnover_rule_change_log.rule_type IS 'Rule type: 1=GameWeight, 2=StatusFactor, 3=OddsThreshold, 4=RiskAction';
COMMENT ON COLUMN t_turnover_rule_change_log.operation_type IS 'Operation: 1=INSERT, 2=UPDATE, 3=DELETE, 4=RELOAD (rule hot-reload)';
COMMENT ON COLUMN t_turnover_rule_change_log.old_value IS 'Complete rule snapshot before change (PostgreSQL JSONB format)';
COMMENT ON COLUMN t_turnover_rule_change_log.new_value IS 'Complete rule snapshot after change (PostgreSQL JSONB format)';
