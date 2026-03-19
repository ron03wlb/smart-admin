-- V005: Create Risk Module Tables
-- Author: iGaming Team
-- Date: 2026-03-19
-- Description: Risk assessment, risk proposals, geo-restrictions, and rule configuration tables

-- ============================================================================
-- Table: t_risk_assessment
-- Description: Append-only per-transaction risk assessment log
-- ============================================================================

CREATE TABLE t_risk_assessment (
    -- Primary Key
    assessment_id       BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Player & Event Reference
    player_id           BIGINT NOT NULL,              -- FK to t_player
    event_type          VARCHAR(50) NOT NULL,         -- Event type: BET_PLACED, WITHDRAWAL_REQUESTED, etc.
    event_id            VARCHAR(100) NOT NULL,        -- Event unique ID (e.g., BET-001, WD-123)

    -- Risk Assessment Results
    risk_score          NUMERIC(8,4) NOT NULL,        -- Risk score: 0.0000 ~ 100.0000
    risk_level          INTEGER NOT NULL,             -- RiskLevelEnum: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
    decision            INTEGER NOT NULL,             -- RiskDecisionEnum: 1=APPROVE, 2=FLAG, 3=REJECT

    -- Rule Execution Details
    rule_results_json   JSONB,                        -- Complete rule execution results as JSONB

    -- Performance Metrics
    processing_time_ms  INTEGER NOT NULL,             -- Processing time in milliseconds

    -- Timestamp (append-only - no update_time)
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Player risk history index
CREATE INDEX idx_risk_assessment_player
    ON t_risk_assessment (tenant_id, player_id, create_time DESC);

-- Event lookup index (for duplicate detection)
CREATE INDEX idx_risk_assessment_event
    ON t_risk_assessment (tenant_id, event_type, event_id);

-- Risk level analysis index
CREATE INDEX idx_risk_assessment_level
    ON t_risk_assessment (tenant_id, risk_level, create_time DESC);

-- Decision analysis index (for monitoring approval/rejection rates)
CREATE INDEX idx_risk_assessment_decision
    ON t_risk_assessment (tenant_id, decision, create_time DESC);

-- Foreign key constraint
ALTER TABLE t_risk_assessment
    ADD CONSTRAINT fk_risk_assessment_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;

COMMENT ON TABLE t_risk_assessment IS
'Append-only per-transaction risk assessment log. Records complete risk evaluation results for each event (bet, withdrawal, etc.) including risk score, level, decision, and rule execution details stored as JSONB. Performance metrics tracked via processing_time_ms.';

-- ============================================================================
-- Table: t_risk_proposal
-- Description: Risk review proposals (work orders for manual review)
-- ============================================================================

CREATE TABLE t_risk_proposal (
    -- Primary Key
    proposal_id         BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Foreign Keys
    player_id           BIGINT NOT NULL,              -- FK to t_player
    assessment_id       BIGINT NOT NULL,              -- FK to t_risk_assessment

    -- Proposal Status & Priority
    status              INTEGER NOT NULL,             -- RiskProposalStatusEnum: 1=PENDING, 2=IN_REVIEW, 3=APPROVED, 4=REJECTED
    priority            INTEGER NOT NULL,             -- RiskLevelEnum: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL

    -- Review Assignment
    assignee            VARCHAR(50),                  -- Assigned reviewer (employee_name or employee_id)
    review_comment      TEXT,                         -- Review comment from operator

    -- SLA Tracking
    sla_deadline        TIMESTAMPTZ,                  -- SLA deadline for review completion
    resolved_at         TIMESTAMPTZ,                  -- Actual resolution timestamp

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Pending proposals query index (for operator worklist)
CREATE INDEX idx_risk_proposal_pending
    ON t_risk_proposal (tenant_id, status, priority DESC, sla_deadline ASC)
    WHERE deleted = FALSE AND status IN (1, 2);  -- 1=PENDING, 2=IN_REVIEW

-- Player proposal history index
CREATE INDEX idx_risk_proposal_player
    ON t_risk_proposal (tenant_id, player_id, create_time DESC)
    WHERE deleted = FALSE;

-- Assessment reference index
CREATE INDEX idx_risk_proposal_assessment
    ON t_risk_proposal (assessment_id)
    WHERE deleted = FALSE;

-- Assignee workload index
CREATE INDEX idx_risk_proposal_assignee
    ON t_risk_proposal (tenant_id, assignee, status)
    WHERE deleted = FALSE AND assignee IS NOT NULL;

-- Foreign key constraints
ALTER TABLE t_risk_proposal
    ADD CONSTRAINT fk_risk_proposal_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;

ALTER TABLE t_risk_proposal
    ADD CONSTRAINT fk_risk_proposal_assessment
    FOREIGN KEY (assessment_id)
    REFERENCES t_risk_assessment (assessment_id)
    ON DELETE RESTRICT;

COMMENT ON TABLE t_risk_proposal IS
'Risk review proposals (work orders) for manual review. Created automatically when risk assessment flags high-risk events. Supports SLA tracking, priority-based work assignment, and resolution workflow.';

-- ============================================================================
-- Table: t_risk_score
-- Description: Player cumulative risk profile (weighted moving average)
-- ============================================================================

CREATE TABLE t_risk_score (
    -- Primary Key
    risk_score_id       BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Player Reference
    player_id           BIGINT NOT NULL,              -- FK to t_player (UNIQUE per tenant)

    -- Cumulative Risk Metrics
    cumulative_score    NUMERIC(8,4) NOT NULL DEFAULT 0,  -- Weighted moving average: 0.0000 ~ 100.0000
    risk_level          INTEGER NOT NULL DEFAULT 1,   -- RiskLevelEnum: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
    total_assessments   INTEGER NOT NULL DEFAULT 0,   -- Total number of assessments processed
    last_assessment_time TIMESTAMPTZ,                 -- Timestamp of last risk assessment

    -- Auto-Lock Flag
    auto_locked         BOOLEAN NOT NULL DEFAULT FALSE,  -- True if player auto-locked due to high risk

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: One risk score per player per tenant
CREATE UNIQUE INDEX uk_risk_score_player
    ON t_risk_score (tenant_id, player_id)
    WHERE deleted = FALSE;

-- Risk level distribution index (for reporting)
CREATE INDEX idx_risk_score_level
    ON t_risk_score (tenant_id, risk_level, cumulative_score DESC)
    WHERE deleted = FALSE;

-- Auto-locked players index (for compliance monitoring)
CREATE INDEX idx_risk_score_auto_locked
    ON t_risk_score (tenant_id, auto_locked, risk_level)
    WHERE deleted = FALSE AND auto_locked = TRUE;

-- Foreign key constraint
ALTER TABLE t_risk_score
    ADD CONSTRAINT fk_risk_score_player
    FOREIGN KEY (player_id)
    REFERENCES t_player (player_id)
    ON DELETE RESTRICT;

COMMENT ON TABLE t_risk_score IS
'Player cumulative risk profile with weighted moving average. Tracks aggregated risk score across all player activities. Auto-lock flag triggers when risk exceeds threshold. Updated incrementally by risk assessment events via weighted moving average algorithm.';

-- ============================================================================
-- Table: t_risk_rule_param
-- Description: Risk rule parameter configuration for LiteFlow components
-- ============================================================================

CREATE TABLE t_risk_rule_param (
    -- Primary Key
    rule_param_id       BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Rule Type & Metadata
    rule_type           INTEGER NOT NULL,             -- RiskRuleTypeEnum: 1=BLACKLIST, 2=HEDGE, 3=BOT, etc.
    rule_name           VARCHAR(100) NOT NULL,        -- Display name
    rule_description    TEXT,                         -- Rule description

    -- Threshold Configuration
    threshold_value     NUMERIC(19,4),                -- Threshold value (e.g., max bet amount, max odds)
    time_window_seconds INTEGER,                      -- Time window for rate limiting (e.g., 3600 = 1 hour)
    max_count           INTEGER,                      -- Max count within time window

    -- Rule Weight (for risk score calculation)
    weight              NUMERIC(8,4),                 -- Rule weight: 0.0000 ~ 100.0000

    -- Enable/Disable Flag
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,

    -- Extra Parameters (JSONB for flexibility)
    params_json         JSONB,                        -- Additional rule parameters as JSONB

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rule type query index
CREATE INDEX idx_risk_rule_param_type
    ON t_risk_rule_param (tenant_id, rule_type, enabled)
    WHERE deleted = FALSE;

-- Enabled rules query index (for hot reload)
CREATE INDEX idx_risk_rule_param_enabled
    ON t_risk_rule_param (tenant_id, enabled)
    WHERE deleted = FALSE AND enabled = TRUE;

COMMENT ON TABLE t_risk_rule_param IS
'Risk rule parameter configuration for LiteFlow rule engine components. Supports dynamic rule configuration with threshold values, time windows, weights, and flexible JSONB parameters. Enables real-time rule updates without code deployment.';

-- ============================================================================
-- Table: t_geo_restriction
-- Description: Geo-restricted jurisdictions (compliance)
-- ============================================================================

CREATE TABLE t_geo_restriction (
    -- Primary Key
    geo_restriction_id  BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Country Information
    country_code        VARCHAR(2) NOT NULL,          -- ISO 3166-1 alpha-2 country code (e.g., US, CN, FR)
    country_name        VARCHAR(100) NOT NULL,        -- Country name (e.g., United States, China)

    -- Restriction Type
    restriction_type    INTEGER NOT NULL,             -- 1=FULL_BAN (no access), 2=VIEW_ONLY (read-only access)

    -- Reason & Enable Flag
    reason              TEXT,                         -- Reason for restriction (compliance requirement)
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,

    -- SmartAdmin Base Entity Fields (no deleted field - hard delete only)
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint on country_code per tenant
CREATE UNIQUE INDEX uk_geo_restriction_country
    ON t_geo_restriction (tenant_id, country_code);

-- Enabled restrictions query index (for runtime access control)
CREATE INDEX idx_geo_restriction_enabled
    ON t_geo_restriction (tenant_id, enabled)
    WHERE enabled = TRUE;

-- Restriction type index (for reporting)
CREATE INDEX idx_geo_restriction_type
    ON t_geo_restriction (tenant_id, restriction_type);

COMMENT ON TABLE t_geo_restriction IS
'Geo-restricted jurisdictions for compliance enforcement. Defines access restrictions by country (ISO 3166-1 alpha-2 codes). Supports FULL_BAN (no access) and VIEW_ONLY (read-only) modes. Hard delete only (no soft delete) to ensure data integrity.';

-- ============================================================================
-- Column Comments for t_risk_assessment
-- ============================================================================

COMMENT ON COLUMN t_risk_assessment.event_type IS 'Event type: BET_PLACED, WITHDRAWAL_REQUESTED, DEPOSIT_APPROVED, etc.';
COMMENT ON COLUMN t_risk_assessment.event_id IS 'Event unique ID for traceability (e.g., BET-001, WD-123, DEP-456)';
COMMENT ON COLUMN t_risk_assessment.risk_score IS 'Risk score (0.0000 ~ 100.0000) - weighted sum of all rule scores';
COMMENT ON COLUMN t_risk_assessment.risk_level IS 'Risk level: 1=LOW (0-29), 2=MEDIUM (30-49), 3=HIGH (50-69), 4=CRITICAL (70-100) (see RiskLevelEnum)';
COMMENT ON COLUMN t_risk_assessment.decision IS 'Decision: 1=APPROVE (allow), 2=FLAG (manual review), 3=REJECT (block) (see RiskDecisionEnum)';
COMMENT ON COLUMN t_risk_assessment.rule_results_json IS 'Complete rule execution results (JSONB): {"rules": [{"name": "BLACKLIST", "matched": false, "score": 0}, ...]}';
COMMENT ON COLUMN t_risk_assessment.processing_time_ms IS 'Processing time in milliseconds (for performance monitoring)';

-- ============================================================================
-- Column Comments for t_risk_proposal
-- ============================================================================

COMMENT ON COLUMN t_risk_proposal.status IS 'Status: 1=PENDING, 2=IN_REVIEW, 3=APPROVED, 4=REJECTED, 5=EXPIRED (see RiskProposalStatusEnum)';
COMMENT ON COLUMN t_risk_proposal.priority IS 'Priority based on risk level: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL (see RiskLevelEnum)';
COMMENT ON COLUMN t_risk_proposal.assignee IS 'Assigned reviewer (employee name or employee ID)';
COMMENT ON COLUMN t_risk_proposal.sla_deadline IS 'SLA deadline: CRITICAL=1h, HIGH=4h, MEDIUM=24h, LOW=72h';
COMMENT ON COLUMN t_risk_proposal.resolved_at IS 'Actual resolution timestamp (NULL if not yet resolved)';

-- ============================================================================
-- Column Comments for t_risk_score
-- ============================================================================

COMMENT ON COLUMN t_risk_score.cumulative_score IS 'Cumulative risk score using weighted moving average (EMA): new_score = 0.2 * current_score + 0.8 * old_score';
COMMENT ON COLUMN t_risk_score.risk_level IS 'Risk level derived from cumulative score: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL';
COMMENT ON COLUMN t_risk_score.total_assessments IS 'Total number of risk assessments processed (for statistical confidence)';
COMMENT ON COLUMN t_risk_score.last_assessment_time IS 'Timestamp of most recent risk assessment';
COMMENT ON COLUMN t_risk_score.auto_locked IS 'True if player auto-locked due to risk score exceeding threshold (requires manual unlock)';

-- ============================================================================
-- Column Comments for t_risk_rule_param
-- ============================================================================

COMMENT ON COLUMN t_risk_rule_param.rule_type IS 'Rule type: 1=BLACKLIST, 2=HEDGE, 3=BOT, 4=LOW_ODDS, 5=HIGH_FREQ, 6=ABNORMAL (see RiskRuleTypeEnum)';
COMMENT ON COLUMN t_risk_rule_param.threshold_value IS 'Threshold value (e.g., 10000.00 for max bet, 1.50 for min odds)';
COMMENT ON COLUMN t_risk_rule_param.time_window_seconds IS 'Time window in seconds (e.g., 3600 = 1 hour for rate limiting)';
COMMENT ON COLUMN t_risk_rule_param.max_count IS 'Max count within time window (e.g., max 10 bets per hour)';
COMMENT ON COLUMN t_risk_rule_param.weight IS 'Rule weight for risk score calculation (0.0000 ~ 100.0000)';
COMMENT ON COLUMN t_risk_rule_param.params_json IS 'Extra rule parameters as JSONB (e.g., {"ip_blacklist": ["192.168.1.1"], "country_whitelist": ["US", "GB"]})';

-- ============================================================================
-- Column Comments for t_geo_restriction
-- ============================================================================

COMMENT ON COLUMN t_geo_restriction.country_code IS 'ISO 3166-1 alpha-2 country code (e.g., US, CN, FR) - 2 characters uppercase';
COMMENT ON COLUMN t_geo_restriction.restriction_type IS 'Restriction type: 1=FULL_BAN (no access), 2=VIEW_ONLY (read-only access, no betting)';
COMMENT ON COLUMN t_geo_restriction.reason IS 'Reason for restriction (e.g., "US federal law prohibits online gambling", "China national regulation")';
