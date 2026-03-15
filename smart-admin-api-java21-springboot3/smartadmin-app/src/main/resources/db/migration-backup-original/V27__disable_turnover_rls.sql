-- V27: Disable RLS on Turnover Rule Tables
-- Description: Turnover rule tables (game weight, status factor, odds threshold, risk action)
--              are system-level configuration tables, not business data.
--              RLS policies block queries when app.current_tenant_id is not set.
--              Disable RLS and rely on application-layer tenant_id filtering instead.
-- Author: iGaming Team
-- Date: 2026-03-13

-- ============================================================================
-- Disable RLS on Turnover Rule Tables
-- ============================================================================

ALTER TABLE t_turnover_game_weight_rule DISABLE ROW LEVEL SECURITY;
ALTER TABLE t_turnover_status_factor_rule DISABLE ROW LEVEL SECURITY;
ALTER TABLE t_turnover_odds_threshold_rule DISABLE ROW LEVEL SECURITY;
ALTER TABLE t_turnover_risk_action_rule DISABLE ROW LEVEL SECURITY;
ALTER TABLE t_turnover_rule_change_log DISABLE ROW LEVEL SECURITY;

-- ============================================================================
-- Drop RLS Policies
-- ============================================================================

DROP POLICY IF EXISTS tenant_isolation_game_weight ON t_turnover_game_weight_rule;
DROP POLICY IF EXISTS tenant_isolation_status_factor ON t_turnover_status_factor_rule;
DROP POLICY IF EXISTS tenant_isolation_odds_threshold ON t_turnover_odds_threshold_rule;
DROP POLICY IF EXISTS tenant_isolation_risk_action ON t_turnover_risk_action_rule;
DROP POLICY IF EXISTS tenant_isolation_change_log ON t_turnover_rule_change_log;

-- ============================================================================
-- Verification
-- ============================================================================

-- Note: tenant_id column still exists in all tables.
--       Application layer (Manager/Service) will filter by tenant_id using WHERE clauses.
--       Example: WHERE tenant_id = #{tenantId} AND deleted = FALSE AND status = 1

COMMENT ON TABLE t_turnover_game_weight_rule IS 'Game weight rules (RLS disabled - system config table)';
COMMENT ON TABLE t_turnover_status_factor_rule IS 'Status factor rules (RLS disabled - system config table)';
COMMENT ON TABLE t_turnover_odds_threshold_rule IS 'Odds threshold rules (RLS disabled - system config table)';
COMMENT ON TABLE t_turnover_risk_action_rule IS 'Risk action rules (RLS disabled - system config table)';
COMMENT ON TABLE t_turnover_rule_change_log IS 'Rule change log (RLS disabled - audit table)';
