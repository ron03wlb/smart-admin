-- V001: Create Player Module Tables
-- Author: iGaming Team
-- Date: 2026-03-19
-- Description: Player master table with PII encryption support

-- ============================================================================
-- Table: t_player
-- Description: Player master table with AES-256-GCM encrypted PII fields
-- ============================================================================

CREATE TABLE t_player (
    -- Primary Key
    player_id           BIGSERIAL PRIMARY KEY,

    -- Multi-Tenant Isolation
    tenant_id           BIGINT NOT NULL,

    -- Authentication
    username            VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,

    -- PII Fields (AES-256-GCM encrypted in application layer)
    email_encrypted     TEXT,
    email_blind_idx     VARCHAR(64),  -- HMAC-SHA256 hex (64 chars)
    phone_encrypted     TEXT,
    phone_blind_idx     VARCHAR(64),  -- HMAC-SHA256 hex (64 chars)

    -- Player Attributes
    status              INTEGER NOT NULL DEFAULT 1,  -- PlayerStatusEnum: 1=ACTIVE, 2=SUSPENDED, 3=BLOCKED
    kyc_level           INTEGER NOT NULL DEFAULT 0,  -- KycLevelEnum: 0=L0, 1=L1, 2=L2, 3=L3
    vip_level           INTEGER NOT NULL DEFAULT 1,  -- VipLevelEnum: 1=BRONZE, 2=SILVER, 3=GOLD, 4=PLATINUM, 5=DIAMOND

    -- Audit Trail
    registration_ip     VARCHAR(45),                 -- IPv4 (15 chars) or IPv6 (45 chars)
    last_login_ip       VARCHAR(45),
    last_login_time     TIMESTAMPTZ,

    -- Soft Delete & Optimistic Lock
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    version             INTEGER NOT NULL DEFAULT 0,

    -- SmartAdmin Base Entity Fields
    create_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- Unique constraint on username per tenant
CREATE UNIQUE INDEX uk_player_username_tenant
    ON t_player (tenant_id, username)
    WHERE deleted = false;

-- Unique constraint on email blind index per tenant (for encrypted email lookup)
CREATE UNIQUE INDEX uk_player_email_idx_tenant
    ON t_player (tenant_id, email_blind_idx)
    WHERE deleted = false AND email_blind_idx IS NOT NULL;

-- Unique constraint on phone blind index per tenant (for encrypted phone lookup)
CREATE UNIQUE INDEX uk_player_phone_idx_tenant
    ON t_player (tenant_id, phone_blind_idx)
    WHERE deleted = false AND phone_blind_idx IS NOT NULL;

-- Tenant isolation index (for multi-tenant queries)
CREATE INDEX idx_player_tenant
    ON t_player (tenant_id)
    WHERE deleted = false;

-- Status filter index (for active/suspended queries)
CREATE INDEX idx_player_status
    ON t_player (tenant_id, status)
    WHERE deleted = false;

-- VIP level index (for VIP-based campaigns)
CREATE INDEX idx_player_vip_level
    ON t_player (tenant_id, vip_level)
    WHERE deleted = false;

-- Registration time index (for cohort analysis)
CREATE INDEX idx_player_create_time
    ON t_player (tenant_id, create_time)
    WHERE deleted = false;

-- ============================================================================
-- Table Comments
-- ============================================================================

COMMENT ON TABLE t_player IS
'Player master table with AES-256-GCM encrypted PII fields. Email and phone use transparent encryption via MyBatis TypeHandler. Blind index fields enable equality lookups without decryption.';

COMMENT ON COLUMN t_player.player_id IS 'Player unique identifier (auto-increment)';
COMMENT ON COLUMN t_player.tenant_id IS 'Tenant ID for multi-tenant isolation (auto-filled by MyBatis MetaObjectHandler)';
COMMENT ON COLUMN t_player.username IS 'Player login username (unique per tenant)';
COMMENT ON COLUMN t_player.password_hash IS 'Argon2id password hash (configurable: m=65536, t=3, p=4 for iGaming compliance)';
COMMENT ON COLUMN t_player.email_encrypted IS 'Email address encrypted with AES-256-GCM (application-layer encryption via EncryptedFieldTypeHandler)';
COMMENT ON COLUMN t_player.email_blind_idx IS 'HMAC-SHA256 blind index for email equality lookups (64-char hex string)';
COMMENT ON COLUMN t_player.phone_encrypted IS 'Phone number encrypted with AES-256-GCM (application-layer encryption via EncryptedFieldTypeHandler)';
COMMENT ON COLUMN t_player.phone_blind_idx IS 'HMAC-SHA256 blind index for phone equality lookups (64-char hex string)';
COMMENT ON COLUMN t_player.status IS 'Player account status: 1=ACTIVE, 2=SUSPENDED, 3=BLOCKED (see PlayerStatusEnum)';
COMMENT ON COLUMN t_player.kyc_level IS 'KYC verification level: 0=L0 (unverified), 1=L1, 2=L2, 3=L3 (see KycLevelEnum)';
COMMENT ON COLUMN t_player.vip_level IS 'VIP tier: 1=BRONZE, 2=SILVER, 3=GOLD, 4=PLATINUM, 5=DIAMOND (see VipLevelEnum)';
COMMENT ON COLUMN t_player.registration_ip IS 'IP address at registration (supports IPv4/IPv6)';
COMMENT ON COLUMN t_player.last_login_ip IS 'IP address of most recent login';
COMMENT ON COLUMN t_player.last_login_time IS 'Timestamp of most recent login (UTC with timezone)';
COMMENT ON COLUMN t_player.deleted IS 'Soft delete flag (1=deleted, 0=active)';
COMMENT ON COLUMN t_player.version IS 'Optimistic lock version for concurrent update protection';
COMMENT ON COLUMN t_player.create_time IS 'Record creation timestamp (auto-filled by MyBatis MetaObjectHandler)';
COMMENT ON COLUMN t_player.update_time IS 'Record last update timestamp (auto-filled by MyBatis MetaObjectHandler)';

-- ============================================================================
-- Index Comments
-- ============================================================================

COMMENT ON INDEX uk_player_username_tenant IS
'Unique constraint: One username per tenant (excludes soft-deleted records)';

COMMENT ON INDEX uk_player_email_idx_tenant IS
'Unique constraint: One email per tenant via blind index (excludes soft-deleted and null email records)';

COMMENT ON INDEX uk_player_phone_idx_tenant IS
'Unique constraint: One phone per tenant via blind index (excludes soft-deleted and null phone records)';

COMMENT ON INDEX idx_player_tenant IS
'Tenant isolation index for multi-tenant query performance';

COMMENT ON INDEX idx_player_status IS
'Composite index for filtering players by status within tenant';

COMMENT ON INDEX idx_player_vip_level IS
'Composite index for VIP-based marketing campaigns and perks';

COMMENT ON INDEX idx_player_create_time IS
'Time-series index for cohort analysis and retention reports';
