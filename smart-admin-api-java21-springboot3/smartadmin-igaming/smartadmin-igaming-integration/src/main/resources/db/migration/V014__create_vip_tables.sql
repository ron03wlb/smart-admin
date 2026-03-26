-- V014__create_vip_tables.sql
-- VIP Level Auto-Upgrade System Tables
-- Created: 2026-03-26
-- Author: iGaming Team

-- ============================================================
-- Table: t_vip_level_config
-- Purpose: VIP level configuration with upgrade requirements and benefits
-- ============================================================
CREATE TABLE t_vip_level_config (
    config_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    -- VIP Level Definition
    vip_level INTEGER NOT NULL CHECK (vip_level >= 1 AND vip_level <= 10),
    level_name VARCHAR(50) NOT NULL, -- Bronze, Silver, Gold, Platinum, Diamond, etc.
    level_description TEXT,

    -- Upgrade Requirements
    upgrade_deposit_requirement NUMERIC(19, 4) NOT NULL DEFAULT 0,  -- Cumulative deposit amount
    upgrade_turnover_requirement NUMERIC(19, 4) NOT NULL DEFAULT 0, -- Cumulative valid turnover
    upgrade_active_days_requirement INTEGER NOT NULL DEFAULT 0,     -- Active days count

    -- VIP Benefits
    daily_withdrawal_limit NUMERIC(19, 4),                          -- Daily withdrawal limit (NULL = unlimited)
    monthly_withdrawal_limit NUMERIC(19, 4),                        -- Monthly withdrawal limit
    rebate_rate NUMERIC(5, 4) NOT NULL DEFAULT 0,                   -- Rebate rate (e.g., 0.0050 = 0.5%)
    birthday_bonus NUMERIC(19, 4) NOT NULL DEFAULT 0,               -- Birthday bonus amount
    level_up_bonus NUMERIC(19, 4) NOT NULL DEFAULT 0,               -- Level up bonus amount

    -- Advanced Benefits (JSON)
    extra_benefits JSONB,  -- {"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true}

    -- Validity Period
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,  -- NULL = no expiry

    -- Audit Fields
    status INTEGER NOT NULL DEFAULT 1,  -- 1: ACTIVE, 0: INACTIVE
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT uk_vip_level_config_tenant_level UNIQUE (tenant_id, vip_level, effective_from)
);

-- Indexes
CREATE INDEX idx_vip_level_config_tenant ON t_vip_level_config(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_vip_level_config_level ON t_vip_level_config(vip_level) WHERE deleted = FALSE;
CREATE INDEX idx_vip_level_config_effective ON t_vip_level_config(effective_from, effective_to) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE t_vip_level_config IS 'VIP level configuration table - defines upgrade requirements and benefits for each VIP tier';
COMMENT ON COLUMN t_vip_level_config.vip_level IS 'VIP level number (1-10, higher is better)';
COMMENT ON COLUMN t_vip_level_config.level_name IS 'VIP level display name (Bronze, Silver, Gold, Platinum, Diamond, etc.)';
COMMENT ON COLUMN t_vip_level_config.upgrade_deposit_requirement IS 'Minimum cumulative deposit amount required to reach this level';
COMMENT ON COLUMN t_vip_level_config.upgrade_turnover_requirement IS 'Minimum cumulative valid turnover required to reach this level';
COMMENT ON COLUMN t_vip_level_config.upgrade_active_days_requirement IS 'Minimum active days count required to reach this level';
COMMENT ON COLUMN t_vip_level_config.daily_withdrawal_limit IS 'Daily withdrawal limit for this VIP level (NULL = unlimited)';
COMMENT ON COLUMN t_vip_level_config.rebate_rate IS 'Rebate rate for this VIP level (decimal, e.g., 0.0050 = 0.5%)';
COMMENT ON COLUMN t_vip_level_config.birthday_bonus IS 'Birthday bonus amount for this VIP level';
COMMENT ON COLUMN t_vip_level_config.level_up_bonus IS 'One-time bonus when player reaches this VIP level';
COMMENT ON COLUMN t_vip_level_config.extra_benefits IS 'Additional benefits in JSON format (priority support, account manager, etc.)';

-- ============================================================
-- Table: t_player_vip_history
-- Purpose: Player VIP level change history (upgrades and downgrades)
-- ============================================================
CREATE TABLE t_player_vip_history (
    history_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    -- Player Reference
    player_id BIGINT NOT NULL,

    -- VIP Level Change
    old_vip_level INTEGER NOT NULL,
    new_vip_level INTEGER NOT NULL,
    upgrade_type VARCHAR(50) NOT NULL,  -- AUTO_UPGRADE, MANUAL_UPGRADE, DOWNGRADE, MANUAL_DOWNGRADE

    -- Player Stats at Upgrade Time
    total_deposit_at_upgrade NUMERIC(19, 4),
    total_turnover_at_upgrade NUMERIC(19, 4),
    active_days_at_upgrade INTEGER,

    -- Audit Information
    upgrade_reason TEXT,  -- "Auto upgraded based on deposit and turnover", "Manually upgraded by admin", etc.
    upgraded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upgraded_by BIGINT,  -- Employee ID (NULL for auto upgrades)

    -- Audit Fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_player_vip_history_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

-- Indexes
CREATE INDEX idx_player_vip_history_tenant ON t_player_vip_history(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_player_vip_history_player ON t_player_vip_history(player_id, upgraded_at DESC) WHERE deleted = FALSE;
CREATE INDEX idx_player_vip_history_level ON t_player_vip_history(new_vip_level, upgraded_at DESC) WHERE deleted = FALSE;
CREATE INDEX idx_player_vip_history_type ON t_player_vip_history(upgrade_type) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE t_player_vip_history IS 'Player VIP level change history - tracks all VIP upgrades and downgrades';
COMMENT ON COLUMN t_player_vip_history.old_vip_level IS 'VIP level before the change';
COMMENT ON COLUMN t_player_vip_history.new_vip_level IS 'VIP level after the change';
COMMENT ON COLUMN t_player_vip_history.upgrade_type IS 'Type of change: AUTO_UPGRADE (system), MANUAL_UPGRADE (admin), DOWNGRADE (auto), MANUAL_DOWNGRADE (admin)';
COMMENT ON COLUMN t_player_vip_history.total_deposit_at_upgrade IS 'Player total cumulative deposit at the time of upgrade';
COMMENT ON COLUMN t_player_vip_history.total_turnover_at_upgrade IS 'Player total cumulative valid turnover at the time of upgrade';
COMMENT ON COLUMN t_player_vip_history.active_days_at_upgrade IS 'Player active days count at the time of upgrade';
COMMENT ON COLUMN t_player_vip_history.upgraded_by IS 'Employee ID who manually upgraded the player (NULL for auto upgrades)';

-- ============================================================
-- Table: t_vip_reward_record
-- Purpose: VIP-related reward distribution records
-- ============================================================
CREATE TABLE t_vip_reward_record (
    reward_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    -- Player & VIP Reference
    player_id BIGINT NOT NULL,
    vip_level INTEGER NOT NULL,

    -- Reward Details
    reward_type VARCHAR(50) NOT NULL,  -- LEVEL_UP_BONUS, BIRTHDAY_BONUS, MONTHLY_REBATE, WEEKLY_REBATE
    reward_amount NUMERIC(19, 4) NOT NULL,
    reward_description TEXT,

    -- Reward Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING, ISSUED, CANCELLED, EXPIRED
    issued_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,

    -- Audit Information
    issued_by BIGINT,  -- Employee ID (NULL for auto issued)
    cancellation_reason TEXT,

    -- Audit Fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_vip_reward_record_player FOREIGN KEY (player_id) REFERENCES t_player(player_id)
);

-- Indexes
CREATE INDEX idx_vip_reward_record_tenant ON t_vip_reward_record(tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_vip_reward_record_player ON t_vip_reward_record(player_id, create_time DESC) WHERE deleted = FALSE;
CREATE INDEX idx_vip_reward_record_status ON t_vip_reward_record(status, create_time DESC) WHERE deleted = FALSE;
CREATE INDEX idx_vip_reward_record_type ON t_vip_reward_record(reward_type) WHERE deleted = FALSE;
CREATE INDEX idx_vip_reward_record_expires ON t_vip_reward_record(expires_at) WHERE status = 'PENDING' AND deleted = FALSE;

-- Comments
COMMENT ON TABLE t_vip_reward_record IS 'VIP reward distribution records - tracks level-up bonuses, birthday bonuses, and rebates';
COMMENT ON COLUMN t_vip_reward_record.reward_type IS 'Type of reward: LEVEL_UP_BONUS, BIRTHDAY_BONUS, MONTHLY_REBATE, WEEKLY_REBATE';
COMMENT ON COLUMN t_vip_reward_record.reward_amount IS 'Reward amount to be issued to player wallet';
COMMENT ON COLUMN t_vip_reward_record.status IS 'Reward status: PENDING (awaiting issue), ISSUED (credited to wallet), CANCELLED (admin cancelled), EXPIRED (past expiry date)';
COMMENT ON COLUMN t_vip_reward_record.issued_at IS 'Timestamp when reward was issued to player wallet';
COMMENT ON COLUMN t_vip_reward_record.expires_at IS 'Reward expiration timestamp (player must claim before this time)';
COMMENT ON COLUMN t_vip_reward_record.issued_by IS 'Employee ID who manually issued the reward (NULL for auto issued)';
