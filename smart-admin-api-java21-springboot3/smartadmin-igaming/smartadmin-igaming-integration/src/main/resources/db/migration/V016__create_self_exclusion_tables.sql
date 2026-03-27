-- V016__create_self_exclusion_tables.sql
-- Self-Exclusion System Tables (Responsible Gaming Compliance)
-- Created: 2026-03-26
-- Author: iGaming Team

-- ============================================================
-- Table: t_self_exclusion_request
-- Purpose: Player self-exclusion requests with cooling-off periods
-- ============================================================
CREATE TABLE t_self_exclusion_request (
    request_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    -- Player Reference
    player_id BIGINT NOT NULL,

    -- Exclusion Configuration
    exclusion_type VARCHAR(50) NOT NULL,  -- DEPOSIT, BETTING, LOGIN, FULL_BLOCK
    duration_type VARCHAR(50) NOT NULL,   -- 24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT

    -- Time Configuration
    start_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP WITH TIME ZONE,  -- NULL for PERMANENT exclusions
    cooling_off_period_end TIMESTAMP WITH TIME ZONE NOT NULL,  -- Earliest date player can request removal

    -- Request Status
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRED, REMOVED, CANCELLED
    request_reason TEXT,  -- Player's reason for self-exclusion (optional)

    -- Removal/Cancellation Information
    removal_request_date TIMESTAMP WITH TIME ZONE,  -- When player requested removal
    removal_approved_by BIGINT,  -- Employee ID who approved removal (NULL if not approved yet)
    removal_approved_at TIMESTAMP WITH TIME ZONE,  -- When removal was approved
    removal_reason TEXT,  -- Admin's reason for approval/rejection or cancellation reason

    -- Compliance Audit Fields
    ip_address VARCHAR(45),  -- IPv4/IPv6 address when request was created
    user_agent TEXT,  -- Browser/device information
    compliance_acknowledgement BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit Fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,  -- Usually NULL (player self-created), or employee ID if admin-created

    -- Foreign Keys
    CONSTRAINT fk_self_exclusion_request_player FOREIGN KEY (player_id) REFERENCES t_player(player_id),

    -- Constraints
    CONSTRAINT chk_exclusion_type CHECK (exclusion_type IN ('DEPOSIT', 'BETTING', 'LOGIN', 'FULL_BLOCK')),
    CONSTRAINT chk_duration_type CHECK (duration_type IN ('24_HOURS', '7_DAYS', '30_DAYS', '6_MONTHS', 'PERMANENT')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REMOVED', 'CANCELLED')),
    CONSTRAINT chk_end_date_for_permanent CHECK (
        (duration_type = 'PERMANENT' AND end_date IS NULL) OR
        (duration_type != 'PERMANENT' AND end_date IS NOT NULL)
    )
);

-- Indexes
CREATE INDEX idx_self_exclusion_request_tenant ON t_self_exclusion_request(tenant_id) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_request_player ON t_self_exclusion_request(player_id, create_time DESC) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_request_status ON t_self_exclusion_request(status, create_time DESC) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_request_active ON t_self_exclusion_request(player_id, exclusion_type)
    WHERE status = 'ACTIVE' AND deleted = false;
CREATE INDEX idx_self_exclusion_request_cooling_off ON t_self_exclusion_request(cooling_off_period_end)
    WHERE status = 'ACTIVE' AND removal_request_date IS NOT NULL AND deleted = false;
CREATE INDEX idx_self_exclusion_request_end_date ON t_self_exclusion_request(end_date)
    WHERE status = 'ACTIVE' AND end_date IS NOT NULL AND deleted = false;

-- Comments
COMMENT ON TABLE t_self_exclusion_request IS 'Self-exclusion requests for responsible gaming compliance - allows players to restrict their own gaming activities';
COMMENT ON COLUMN t_self_exclusion_request.exclusion_type IS 'Type of restriction: DEPOSIT (block deposits only), BETTING (block bets only), LOGIN (block login), FULL_BLOCK (block all activities)';
COMMENT ON COLUMN t_self_exclusion_request.duration_type IS 'Duration of exclusion: 24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT';
COMMENT ON COLUMN t_self_exclusion_request.start_date IS 'When the exclusion takes effect (usually immediate)';
COMMENT ON COLUMN t_self_exclusion_request.end_date IS 'When the exclusion expires (NULL for PERMANENT exclusions)';
COMMENT ON COLUMN t_self_exclusion_request.cooling_off_period_end IS 'Earliest date when player can request removal (prevents impulsive removal)';
COMMENT ON COLUMN t_self_exclusion_request.status IS 'Status: ACTIVE (currently enforced), EXPIRED (past end_date), REMOVED (admin approved removal), CANCELLED (admin cancelled before activation)';
COMMENT ON COLUMN t_self_exclusion_request.request_reason IS 'Optional reason provided by player for self-exclusion';
COMMENT ON COLUMN t_self_exclusion_request.removal_request_date IS 'When player requested early removal (must be after cooling_off_period_end)';
COMMENT ON COLUMN t_self_exclusion_request.removal_approved_by IS 'Employee ID who approved the removal request';
COMMENT ON COLUMN t_self_exclusion_request.compliance_acknowledgement IS 'Player acknowledged responsible gaming policy when creating request';

-- ============================================================
-- Table: t_self_exclusion_history
-- Purpose: Audit trail of all self-exclusion state changes
-- ============================================================
CREATE TABLE t_self_exclusion_history (
    history_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,

    -- Request Reference
    request_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,

    -- Action Details
    action_type VARCHAR(50) NOT NULL,  -- CREATED, ACTIVATED, EXPIRED, REMOVAL_REQUESTED, REMOVAL_APPROVED, REMOVAL_REJECTED, CANCELLED
    action_by BIGINT,  -- Employee ID (NULL for system actions like auto-expiry)
    action_reason TEXT,  -- Reason for the action
    action_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- State Change
    previous_status VARCHAR(50),  -- NULL if CREATED
    new_status VARCHAR(50) NOT NULL,

    -- Additional Context (JSON)
    metadata JSONB,  -- {"ip_address": "1.2.3.4", "user_agent": "...", "notes": "..."}

    -- Audit Fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_self_exclusion_history_request FOREIGN KEY (request_id) REFERENCES t_self_exclusion_request(request_id),
    CONSTRAINT fk_self_exclusion_history_player FOREIGN KEY (player_id) REFERENCES t_player(player_id),

    -- Constraints
    CONSTRAINT chk_action_type CHECK (action_type IN (
        'CREATED', 'ACTIVATED', 'EXPIRED', 'REMOVAL_REQUESTED',
        'REMOVAL_APPROVED', 'REMOVAL_REJECTED', 'CANCELLED'
    ))
);

-- Indexes
CREATE INDEX idx_self_exclusion_history_tenant ON t_self_exclusion_history(tenant_id) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_request ON t_self_exclusion_history(request_id, action_at DESC) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_player ON t_self_exclusion_history(player_id, action_at DESC) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_action_type ON t_self_exclusion_history(action_type, action_at DESC) WHERE deleted = false;
CREATE INDEX idx_self_exclusion_history_action_by ON t_self_exclusion_history(action_by, action_at DESC)
    WHERE action_by IS NOT NULL AND deleted = false;

-- Comments
COMMENT ON TABLE t_self_exclusion_history IS 'Audit trail for all self-exclusion request state changes - required for regulatory compliance';
COMMENT ON COLUMN t_self_exclusion_history.action_type IS 'Type of action: CREATED (player created), ACTIVATED (took effect), EXPIRED (auto-expired), REMOVAL_REQUESTED (player requested removal), REMOVAL_APPROVED (admin approved), REMOVAL_REJECTED (admin rejected), CANCELLED (admin cancelled)';
COMMENT ON COLUMN t_self_exclusion_history.action_by IS 'Employee ID who performed the action (NULL for system actions like auto-expiry)';
COMMENT ON COLUMN t_self_exclusion_history.action_reason IS 'Reason for the action (required for REMOVAL_APPROVED, REMOVAL_REJECTED, CANCELLED)';
COMMENT ON COLUMN t_self_exclusion_history.previous_status IS 'Status before the action (NULL for CREATED action)';
COMMENT ON COLUMN t_self_exclusion_history.new_status IS 'Status after the action';
COMMENT ON COLUMN t_self_exclusion_history.metadata IS 'Additional context in JSON format (IP address, user agent, notes, etc.)';
