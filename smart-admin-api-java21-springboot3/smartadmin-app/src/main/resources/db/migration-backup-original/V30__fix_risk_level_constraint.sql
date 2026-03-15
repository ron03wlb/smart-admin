-- V30: Fix Risk Level Constraint
-- Description: Extend ck_risk_level to allow 4 levels (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)
-- Author: iGaming Team
-- Date: 2026-03-13

-- ============================================================================
-- Drop and Recreate Risk Level Constraint
-- ============================================================================

ALTER TABLE t_turnover_risk_action_rule DROP CONSTRAINT IF EXISTS ck_risk_level;

ALTER TABLE t_turnover_risk_action_rule
  ADD CONSTRAINT ck_risk_level CHECK (risk_level IN (1, 2, 3, 4));

COMMENT ON CONSTRAINT ck_risk_level ON t_turnover_risk_action_rule IS 'Risk level: 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL';
