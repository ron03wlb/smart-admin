-- V012: Add optimized indexes for Turnover Rule tables
-- Author: iGaming Team
-- Date: 2026-03-24
-- Description: Create composite indexes for fast Turnover Rule queries (used by LiteFlow + Redis cache)

-- ============================================
-- Game Weight Rule Indexes
-- ============================================

-- Primary lookup: tenant_id + game_category + status + deleted + effective dates + priority
CREATE INDEX IF NOT EXISTS idx_turnover_game_weight_lookup
  ON t_turnover_game_weight_rule(tenant_id, game_category, status, deleted, effective_from, effective_to, priority DESC)
  WHERE deleted = false AND status = 1;

COMMENT ON INDEX idx_turnover_game_weight_lookup IS 'Optimized index for TurnoverGameWeightRuleManager.getActiveRule() - filters by tenant, category, status, effective dates, then orders by priority DESC';

-- ============================================
-- Status Factor Rule Indexes
-- ============================================

-- Primary lookup: tenant_id + settlement_status + status + deleted + effective dates
CREATE INDEX IF NOT EXISTS idx_turnover_status_factor_lookup
  ON t_turnover_status_factor_rule(tenant_id, settlement_status, status, deleted, effective_from, effective_to)
  WHERE deleted = false AND status = 1;

COMMENT ON INDEX idx_turnover_status_factor_lookup IS 'Optimized index for TurnoverStatusFactorRuleManager.getActiveRule() - filters by tenant, settlement_status, status, effective dates';

-- ============================================
-- Risk Action Rule Indexes
-- ============================================

-- Primary lookup: tenant_id + risk_level + status + deleted + effective dates
CREATE INDEX IF NOT EXISTS idx_turnover_risk_action_lookup
  ON t_turnover_risk_action_rule(tenant_id, risk_level, status, deleted, effective_from, effective_to)
  WHERE deleted = false AND status = 1;

COMMENT ON INDEX idx_turnover_risk_action_lookup IS 'Optimized index for TurnoverRiskActionRuleManager.getActiveRule() - filters by tenant, risk_level, status, effective dates';

-- ============================================
-- Odds Threshold Rule Indexes
-- ============================================

-- Primary lookup: tenant_id + odds_type + status + deleted + effective dates
CREATE INDEX IF NOT EXISTS idx_turnover_odds_threshold_lookup
  ON t_turnover_odds_threshold_rule(tenant_id, odds_type, status, deleted, effective_from, effective_to)
  WHERE deleted = false AND status = 1;

COMMENT ON INDEX idx_turnover_odds_threshold_lookup IS 'Optimized index for TurnoverOddsThresholdRuleManager.getActiveRule() - filters by tenant, odds_type, status, effective dates';

-- ============================================
-- Performance Notes
-- ============================================

-- All indexes use partial indexing (WHERE deleted = false AND status = 1) to reduce index size
-- Priority DESC is included only for t_turnover_game_weight_rule (other tables don't have priority column)
-- Effective date range filtering is covered (effective_from, effective_to)
-- Redis caching will reduce database hits to ~5% (95% cache hit rate expected)
