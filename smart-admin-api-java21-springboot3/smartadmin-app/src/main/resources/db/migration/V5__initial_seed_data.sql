-- =====================================================================
-- V5: Initial Seed Data — Consolidation of V29-V31
-- =====================================================================
-- This migration consolidates initial rule data:
--   V29: Insert Turnover Rules Initial Data (6+9+4+4 = 23 rules)
--   V30: (SKIPPED) Fix risk_level constraint — Already integrated in V4
--   V31: (REMOVED) Duplicate of V29 — V29 and V31 are identical
--
-- Total Rules Inserted: 23 rules
--   - 6 Game Weight Rules (game_category 1-6)
--   - 9 Status Factor Rules (settlement_status 1-9)
--   - 4 Odds Threshold Rules (odds_type 1-4)
--   - 4 Risk Action Rules (risk_level 1-4, with V30 fix)
--
-- ⚠️ IMPORTANT NOTES:
--   - V29 and V31 files were identical (both labeled as "V28" in comments)
--   - V30 constraint fix already integrated in V4 table creation
--   - All INSERT statements use WHERE NOT EXISTS for idempotency
--   - Uses tenant_id = 1 for initial seed data
--
-- Conventions:
--   - WHERE NOT EXISTS pattern for idempotency (partial unique index compatible)
--   - DECIMAL(5,2) for percentage values (0.00 to 100.00)
--   - DECIMAL(10,4) for odds values (high precision)
--   - All rules have status=1 (enabled) by default
-- =====================================================================

-- =====================================================================
-- PART 0: System Configuration (t_config)
-- =====================================================================
-- SmartAdmin requires these configurations to be present at startup:
--   - config_id=1: Super Password (admin emergency access)
--   - config_id=2: Level 3 Protect Config (security compliance)
--
-- ⚠️ CRITICAL: Level3ProtectConfigService.init() will FAIL if config_id=2 is missing
-- =====================================================================

INSERT INTO t_config (config_id, tenant_id, config_name, config_key, config_value, remark, create_time, update_time)
VALUES
    (1, 1, '万能密码', 'super_password', '1024ok',
     '一路春光啊一路荆棘呀惊鸿一般短暂如夏花一样绚烂这是一个不能停留太久的世界，一路春光啊一路荆棘呀惊鸿一般短暂如夏花一样绚烂这是一个不能停留太久的世界啊',
     NOW(), NOW()),

    (2, 1, '三级等保', 'level3_protect_config',
     '{"fileDetectFlag":true,"loginActiveTimeoutMinutes":30,"loginFailLockMinutes":30,"loginFailMaxTimes":3,"maxUploadFileSizeMb":30,"passwordComplexityEnabled":true,"regularChangePasswordMonths":3,"regularChangePasswordNotAllowRepeatTimes":3,"twoFactorLoginEnabled":false}',
     'Level 3 security protection configuration (required for startup)',
     NOW(), NOW())

ON CONFLICT (config_id) DO UPDATE
SET config_key = EXCLUDED.config_key,
    config_value = EXCLUDED.config_value,
    update_time = NOW();

-- =====================================================================
-- PART 1: Game Weight Rules (6 categories)
-- =====================================================================

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_SLOTS', 'Slots Game Weight', 1, 100.00, 1, 1, 'Slots contribute 100% to turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_SLOTS' AND tenant_id = 1);

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_LIVE_CASINO', 'Live Casino Weight', 2, 15.00, 2, 1, 'Live Casino (Baccarat, Roulette) contributes 15%'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_LIVE_CASINO' AND tenant_id = 1);

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_SPORTS', 'Sports Betting Weight', 3, 100.00, 3, 1, 'Sports betting contributes 100%'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_SPORTS' AND tenant_id = 1);

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_POKER', 'Poker Weight', 4, 5.00, 4, 1, 'Poker contributes 5%'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_POKER' AND tenant_id = 1);

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_TABLE_GAMES', 'Table Games Weight', 5, 20.00, 5, 1, 'Table Games (Blackjack, Craps) contribute 20%'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_TABLE_GAMES' AND tenant_id = 1);

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, status, remark)
SELECT 1, 'GAME_WEIGHT_LOTTERY', 'Lottery Weight', 6, 15.00, 6, 1, 'Lottery contributes 15%'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_game_weight_rule WHERE rule_code = 'GAME_WEIGHT_LOTTERY' AND tenant_id = 1);

-- =====================================================================
-- PART 2: Status Factor Rules (9 settlement statuses)
-- =====================================================================

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_WIN', 'Win Status Factor', 1, 100.00, 1, 'Win: 100% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_WIN' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_LOSS', 'Loss Status Factor', 2, 100.00, 1, 'Loss: 100% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_LOSS' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_DRAW', 'Draw Status Factor', 3, 0.00, 1, 'Draw/Tie: 0% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_DRAW' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_HALF_WIN', 'Half Win Factor', 4, 100.00, 1, 'Half Win: 100% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_HALF_WIN' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_HALF_LOSS', 'Half Loss Factor', 5, 100.00, 1, 'Half Loss: 100% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_HALF_LOSS' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_VOID', 'Void Status Factor', 6, 0.00, 1, 'Void: 0% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_VOID' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_CANCEL', 'Cancel Status Factor', 7, 0.00, 1, 'Cancel: 0% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_CANCEL' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_RUNNING', 'Running Status Factor', 8, 0.00, 1, 'Running/Pending: 0% until settled'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_RUNNING' AND tenant_id = 1);

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, status, remark)
SELECT 1, 'STATUS_FACTOR_CASHOUT', 'Cashout Status Factor', 9, 100.00, 1, 'Cashout: 100% valid turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_status_factor_rule WHERE rule_code = 'STATUS_FACTOR_CASHOUT' AND tenant_id = 1);

-- =====================================================================
-- PART 3: Odds Threshold Rules (4 odds types)
-- =====================================================================

INSERT INTO t_turnover_odds_threshold_rule (tenant_id, rule_code, rule_name, odds_type, threshold_value, comparison_operator, status, remark)
SELECT 1, 'ODDS_THRESHOLD_DECIMAL', 'Decimal Odds Threshold', 1, 1.50, '>=', 1, 'Decimal odds must be >= 1.5'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_odds_threshold_rule WHERE rule_code = 'ODDS_THRESHOLD_DECIMAL' AND tenant_id = 1);

INSERT INTO t_turnover_odds_threshold_rule (tenant_id, rule_code, rule_name, odds_type, threshold_value, comparison_operator, status, remark)
SELECT 1, 'ODDS_THRESHOLD_HONGKONG', 'Hong Kong Odds Threshold', 2, 0.50, '>=', 1, 'HK odds must be >= 0.5'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_odds_threshold_rule WHERE rule_code = 'ODDS_THRESHOLD_HONGKONG' AND tenant_id = 1);

INSERT INTO t_turnover_odds_threshold_rule (tenant_id, rule_code, rule_name, odds_type, threshold_value, comparison_operator, status, remark)
SELECT 1, 'ODDS_THRESHOLD_MALAY', 'Malay Odds Threshold', 3, 0.50, '>=', 1, 'Malay odds must be >= 0.5'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_odds_threshold_rule WHERE rule_code = 'ODDS_THRESHOLD_MALAY' AND tenant_id = 1);

INSERT INTO t_turnover_odds_threshold_rule (tenant_id, rule_code, rule_name, odds_type, threshold_value, comparison_operator, status, remark)
SELECT 1, 'ODDS_THRESHOLD_INDO', 'Indo Odds Threshold', 4, -2.00, '<=', 1, 'Indo odds must be <= -2.0 (absolute value >= 2.0)'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_odds_threshold_rule WHERE rule_code = 'ODDS_THRESHOLD_INDO' AND tenant_id = 1);

-- =====================================================================
-- PART 4: Risk Action Rules (4 risk levels, with V30 fix)
-- =====================================================================

INSERT INTO t_turnover_risk_action_rule (tenant_id, rule_code, rule_name, risk_level, action_type, turnover_factor, allow_bet, create_proposal, status, remark)
SELECT 1, 'RISK_ACTION_LOW', 'Low Risk Action', 1, 1, 100.00, TRUE, FALSE, 1, 'Risk 0-29: PASS, 100% turnover factor'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_risk_action_rule WHERE rule_code = 'RISK_ACTION_LOW' AND tenant_id = 1);

INSERT INTO t_turnover_risk_action_rule (tenant_id, rule_code, rule_name, risk_level, action_type, turnover_factor, allow_bet, create_proposal, status, remark)
SELECT 1, 'RISK_ACTION_MEDIUM', 'Medium Risk Action', 2, 2, 100.00, TRUE, TRUE, 1, 'Risk 30-49: FLAG, 100% factor, create proposal'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_risk_action_rule WHERE rule_code = 'RISK_ACTION_MEDIUM' AND tenant_id = 1);

INSERT INTO t_turnover_risk_action_rule (tenant_id, rule_code, rule_name, risk_level, action_type, turnover_factor, allow_bet, create_proposal, status, remark)
SELECT 1, 'RISK_ACTION_HIGH', 'High Risk Action', 3, 2, 100.00, TRUE, TRUE, 1, 'Risk 50-69: FLAG, 100% factor, create proposal'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_risk_action_rule WHERE rule_code = 'RISK_ACTION_HIGH' AND tenant_id = 1);

INSERT INTO t_turnover_risk_action_rule (tenant_id, rule_code, rule_name, risk_level, action_type, turnover_factor, allow_bet, create_proposal, status, remark)
SELECT 1, 'RISK_ACTION_CRITICAL', 'Critical Risk Action', 4, 3, 0.00, FALSE, TRUE, 1, 'Risk 70+: BLOCK, 0% factor, reject turnover'
WHERE NOT EXISTS (SELECT 1 FROM t_turnover_risk_action_rule WHERE rule_code = 'RISK_ACTION_CRITICAL' AND tenant_id = 1);

-- =====================================================================
-- PART 5: Verification Query (Optional - Commented Out)
-- =====================================================================
-- Uncomment this section to verify all rules were inserted successfully

/*
SELECT 'Game Weight Rules' AS category, COUNT(*) AS count FROM t_turnover_game_weight_rule WHERE tenant_id = 1 AND deleted = FALSE
UNION ALL
SELECT 'Status Factor Rules', COUNT(*) FROM t_turnover_status_factor_rule WHERE tenant_id = 1 AND deleted = FALSE
UNION ALL
SELECT 'Odds Threshold Rules', COUNT(*) FROM t_turnover_odds_threshold_rule WHERE tenant_id = 1 AND deleted = FALSE
UNION ALL
SELECT 'Risk Action Rules', COUNT(*) FROM t_turnover_risk_action_rule WHERE tenant_id = 1 AND deleted = FALSE;
*/

-- =====================================================================
-- Migration Complete: V5 Initial Seed Data
-- =====================================================================
-- Total rules inserted: 23
--   - Game Weight: 6 rules (categories 1-6)
--   - Status Factor: 9 rules (statuses 1-9)
--   - Odds Threshold: 4 rules (odds types 1-4)
--   - Risk Action: 4 rules (risk levels 1-4, including V30 CRITICAL level)
--
-- Idempotency: All INSERTs use WHERE NOT EXISTS pattern
-- V30 Fix: Risk level 4 (CRITICAL) included in Risk Action Rules
-- V31 Duplicate: Removed (V29 and V31 were identical)
-- =====================================================================
