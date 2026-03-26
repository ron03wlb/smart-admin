-- V015__seed_vip_level_config.sql
-- VIP Level Configuration Seed Data (10 Levels)
-- Created: 2026-03-26
-- Author: iGaming Team

-- ============================================================
-- VIP Level System: 10 Tiers
-- Progression: Bronze → Silver → Gold → Platinum → Diamond → Master → Grandmaster → Elite → Legend → Supreme
-- ============================================================

-- Insert VIP Level Configurations for Tenant 1 (default tenant)
INSERT INTO t_vip_level_config (
    tenant_id,
    vip_level,
    level_name,
    level_description,
    upgrade_deposit_requirement,
    upgrade_turnover_requirement,
    upgrade_active_days_requirement,
    daily_withdrawal_limit,
    monthly_withdrawal_limit,
    rebate_rate,
    birthday_bonus,
    level_up_bonus,
    extra_benefits,
    effective_from,
    effective_to,
    status
) VALUES
-- Level 1: Bronze (Entry Level)
(
    1,  -- tenant_id
    1,  -- vip_level
    'Bronze',
    'Entry level VIP - Join the VIP program and start enjoying exclusive benefits',
    0,      -- upgrade_deposit_requirement (no deposit required for Bronze)
    0,      -- upgrade_turnover_requirement (no turnover required for Bronze)
    0,      -- upgrade_active_days_requirement
    10000,  -- daily_withdrawal_limit ($10,000)
    100000, -- monthly_withdrawal_limit ($100,000)
    0.0010, -- rebate_rate (0.10%)
    50,     -- birthday_bonus ($50)
    0,      -- level_up_bonus (no bonus for joining Bronze)
    '{"priority_support": false, "dedicated_account_manager": false, "exclusive_events": false}'::jsonb,
    '2026-01-01',
    NULL,  -- No expiry
    1      -- ACTIVE
),

-- Level 2: Silver
(
    1,  -- tenant_id
    2,  -- vip_level
    'Silver',
    'Silver VIP - Enhanced withdrawal limits and improved rebate rate',
    1000,   -- upgrade_deposit_requirement ($1,000 cumulative deposit)
    5000,   -- upgrade_turnover_requirement ($5,000 cumulative turnover)
    7,      -- upgrade_active_days_requirement (1 week)
    20000,  -- daily_withdrawal_limit ($20,000)
    200000, -- monthly_withdrawal_limit ($200,000)
    0.0015, -- rebate_rate (0.15%)
    100,    -- birthday_bonus ($100)
    50,     -- level_up_bonus ($50 one-time bonus)
    '{"priority_support": false, "dedicated_account_manager": false, "exclusive_events": false}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 3: Gold
(
    1,  -- tenant_id
    3,  -- vip_level
    'Gold',
    'Gold VIP - Priority support and higher withdrawal limits',
    5000,   -- upgrade_deposit_requirement ($5,000 cumulative deposit)
    25000,  -- upgrade_turnover_requirement ($25,000 cumulative turnover)
    14,     -- upgrade_active_days_requirement (2 weeks)
    50000,  -- daily_withdrawal_limit ($50,000)
    500000, -- monthly_withdrawal_limit ($500,000)
    0.0020, -- rebate_rate (0.20%)
    200,    -- birthday_bonus ($200)
    100,    -- level_up_bonus ($100 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": false, "exclusive_events": false}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 4: Platinum
(
    1,  -- tenant_id
    4,  -- vip_level
    'Platinum',
    'Platinum VIP - Dedicated account manager and exclusive event access',
    20000,   -- upgrade_deposit_requirement ($20,000 cumulative deposit)
    100000,  -- upgrade_turnover_requirement ($100,000 cumulative turnover)
    30,      -- upgrade_active_days_requirement (1 month)
    100000,  -- daily_withdrawal_limit ($100,000)
    1000000, -- monthly_withdrawal_limit ($1,000,000)
    0.0030,  -- rebate_rate (0.30%)
    500,     -- birthday_bonus ($500)
    300,     -- level_up_bonus ($300 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 5: Diamond
(
    1,  -- tenant_id
    5,  -- vip_level
    'Diamond',
    'Diamond VIP - Premium benefits and unlimited withdrawal limits',
    50000,   -- upgrade_deposit_requirement ($50,000 cumulative deposit)
    250000,  -- upgrade_turnover_requirement ($250,000 cumulative turnover)
    60,      -- upgrade_active_days_requirement (2 months)
    NULL,    -- daily_withdrawal_limit (UNLIMITED)
    NULL,    -- monthly_withdrawal_limit (UNLIMITED)
    0.0040,  -- rebate_rate (0.40%)
    1000,    -- birthday_bonus ($1,000)
    500,     -- level_up_bonus ($500 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 6: Master
(
    1,  -- tenant_id
    6,  -- vip_level
    'Master',
    'Master VIP - Elite tier with enhanced rebate and exclusive perks',
    100000,  -- upgrade_deposit_requirement ($100,000 cumulative deposit)
    500000,  -- upgrade_turnover_requirement ($500,000 cumulative turnover)
    90,      -- upgrade_active_days_requirement (3 months)
    NULL,    -- daily_withdrawal_limit (UNLIMITED)
    NULL,    -- monthly_withdrawal_limit (UNLIMITED)
    0.0050,  -- rebate_rate (0.50%)
    2000,    -- birthday_bonus ($2,000)
    1000,    -- level_up_bonus ($1,000 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true, "vip_concierge": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 7: Grandmaster
(
    1,  -- tenant_id
    7,  -- vip_level
    'Grandmaster',
    'Grandmaster VIP - Top-tier benefits with VIP concierge service',
    250000,  -- upgrade_deposit_requirement ($250,000 cumulative deposit)
    1250000, -- upgrade_turnover_requirement ($1,250,000 cumulative turnover)
    120,     -- upgrade_active_days_requirement (4 months)
    NULL,    -- daily_withdrawal_limit (UNLIMITED)
    NULL,    -- monthly_withdrawal_limit (UNLIMITED)
    0.0060,  -- rebate_rate (0.60%)
    3000,    -- birthday_bonus ($3,000)
    2000,    -- level_up_bonus ($2,000 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true, "vip_concierge": true, "luxury_gifts": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 8: Elite
(
    1,  -- tenant_id
    8,  -- vip_level
    'Elite',
    'Elite VIP - Invitation-only tier with luxury gifts and premium services',
    500000,  -- upgrade_deposit_requirement ($500,000 cumulative deposit)
    2500000, -- upgrade_turnover_requirement ($2,500,000 cumulative turnover)
    180,     -- upgrade_active_days_requirement (6 months)
    NULL,    -- daily_withdrawal_limit (UNLIMITED)
    NULL,    -- monthly_withdrawal_limit (UNLIMITED)
    0.0070,  -- rebate_rate (0.70%)
    5000,    -- birthday_bonus ($5,000)
    3000,    -- level_up_bonus ($3,000 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true, "vip_concierge": true, "luxury_gifts": true, "private_jets": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 9: Legend
(
    1,  -- tenant_id
    9,  -- vip_level
    'Legend',
    'Legend VIP - Ultimate VIP experience with private jets and luxury travel',
    1000000, -- upgrade_deposit_requirement ($1,000,000 cumulative deposit)
    5000000, -- upgrade_turnover_requirement ($5,000,000 cumulative turnover)
    270,     -- upgrade_active_days_requirement (9 months)
    NULL,    -- daily_withdrawal_limit (UNLIMITED)
    NULL,    -- monthly_withdrawal_limit (UNLIMITED)
    0.0080,  -- rebate_rate (0.80%)
    10000,   -- birthday_bonus ($10,000)
    5000,    -- level_up_bonus ($5,000 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true, "vip_concierge": true, "luxury_gifts": true, "private_jets": true, "yacht_rentals": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
),

-- Level 10: Supreme (Highest Tier)
(
    1,  -- tenant_id
    10, -- vip_level
    'Supreme',
    'Supreme VIP - The pinnacle of VIP experience with fully customized benefits',
    5000000,  -- upgrade_deposit_requirement ($5,000,000 cumulative deposit)
    25000000, -- upgrade_turnover_requirement ($25,000,000 cumulative turnover)
    365,      -- upgrade_active_days_requirement (1 year)
    NULL,     -- daily_withdrawal_limit (UNLIMITED)
    NULL,     -- monthly_withdrawal_limit (UNLIMITED)
    0.0100,   -- rebate_rate (1.00%)
    25000,    -- birthday_bonus ($25,000)
    10000,    -- level_up_bonus ($10,000 one-time bonus)
    '{"priority_support": true, "dedicated_account_manager": true, "exclusive_events": true, "custom_limits": true, "vip_concierge": true, "luxury_gifts": true, "private_jets": true, "yacht_rentals": true, "custom_benefits": true}'::jsonb,
    '2026-01-01',
    NULL,
    1
);

-- ============================================================
-- Verification Query
-- ============================================================
-- SELECT vip_level, level_name, upgrade_deposit_requirement, upgrade_turnover_requirement, rebate_rate, level_up_bonus
-- FROM t_vip_level_config
-- WHERE tenant_id = 1
-- ORDER BY vip_level;
