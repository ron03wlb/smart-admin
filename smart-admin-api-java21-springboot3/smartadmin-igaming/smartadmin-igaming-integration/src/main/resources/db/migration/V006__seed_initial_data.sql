-- ======================================================================================
-- SmartAdmin iGaming - Seed Initial Data (Simplified Version)
-- ======================================================================================
-- Version: V006
-- Description: Insert default configuration data for testing and development
-- Author: iGaming Team
-- Date: 2026-03-19
--
-- Data Categories:
-- 1. Promotion Rules (2 entries)
-- 2. Turnover Rules (20 entries)
-- 3. Risk Rule Parameters (6 entries)
-- 4. Geo-Restrictions (2 entries)
--
-- IMPORTANT: All data uses tenant_id = 1 for default tenant
-- ======================================================================================

-- ======================================================================================
-- 1. Promotion Rules (t_promotion_rule)
-- ======================================================================================

-- First Deposit Bonus: 100% match up to $500, 20x wagering requirement
INSERT INTO t_promotion_rule (
    tenant_id,
    promotion_code,
    promotion_name,
    promotion_type,
    status,
    start_time,
    end_time,
    min_deposit,
    bonus_rate,
    max_bonus,
    wagering_multiplier,
    max_claims_per_player,
    bonus_expiry_days,
    game_restriction,
    deleted,
    version
) VALUES (
    1,
    'FIRST_DEPOSIT_100',
    'First Deposit 100% Bonus',
    1, -- FIRST_DEPOSIT
    1, -- ACTIVE
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 year',
    20.0000,
    1.0000,
    500.0000,
    20.00,
    1,
    30,
    '{"allowed_categories": [1, 3]}'::jsonb,
    FALSE,
    0
);

-- Reload Bonus: 50% match up to $200, 15x wagering requirement
INSERT INTO t_promotion_rule (
    tenant_id,
    promotion_code,
    promotion_name,
    promotion_type,
    status,
    start_time,
    end_time,
    min_deposit,
    bonus_rate,
    max_bonus,
    wagering_multiplier,
    max_claims_per_player,
    bonus_expiry_days,
    game_restriction,
    deleted,
    version
) VALUES (
    1,
    'RELOAD_50',
    'Reload 50% Bonus',
    2, -- RELOAD
    1, -- ACTIVE
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 year',
    50.0000,
    0.5000,
    200.0000,
    15.00,
    NULL,
    14,
    '{"allowed_categories": [1, 2, 3, 5]}'::jsonb,
    FALSE,
    0
);

-- ======================================================================================
-- 2. Turnover Rules
-- ======================================================================================

-- -----------------------------------------------------------------------------
-- 2.1 Game Weight Rules (t_turnover_game_weight_rule) - 6 entries
-- -----------------------------------------------------------------------------

INSERT INTO t_turnover_game_weight_rule (tenant_id, rule_code, rule_name, game_category, weight_percentage, priority, effective_from, effective_to, status, deleted, version) VALUES
(1, 'GW-SLOTS-100', 'Slots Full Weight', 1, 100.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'GW-LIVE-15', 'Live Casino 15% Weight', 2, 15.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'GW-SPORTS-100', 'Sports Betting Full Weight', 3, 100.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'GW-POKER-5', 'Poker 5% Weight', 4, 5.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'GW-TABLE-20', 'Table Games 20% Weight', 5, 20.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'GW-LOTTERY-15', 'Lottery 15% Weight', 6, 15.00, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0);

-- -----------------------------------------------------------------------------
-- 2.2 Status Factor Rules (t_turnover_status_factor_rule) - 9 entries
-- -----------------------------------------------------------------------------

INSERT INTO t_turnover_status_factor_rule (tenant_id, rule_code, rule_name, settlement_status, factor_percentage, effective_from, effective_to, status, deleted, version) VALUES
(1, 'SF-WIN-100', 'Win Status 100% Factor', 1, 100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-LOSS-0', 'Loss Status 0% Factor', 2, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-DRAW-0', 'Draw Status 0% Factor', 3, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-TIE-0', 'Tie Status 0% Factor', 4, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-VOID-0', 'Void Status 0% Factor', 5, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-CANCEL-0', 'Cancelled Status 0% Factor', 6, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-HALFWIN-100', 'Half Win Status 100% Factor', 7, 100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-HALFLOSS-100', 'Half Loss Status 100% Factor', 8, 100.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'SF-RUNNING-0', 'Running Status 0% Factor', 9, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0);

-- -----------------------------------------------------------------------------
-- 2.3 Risk Action Rules (t_turnover_risk_action_rule) - 4 entries
-- -----------------------------------------------------------------------------

INSERT INTO t_turnover_risk_action_rule (tenant_id, rule_code, rule_name, risk_level, action_type, turnover_factor, allow_bet, create_proposal, effective_from, effective_to, status, deleted, version) VALUES
(1, 'RA-LOW-PASS', 'Low Risk Pass Action', 1, 1, 100.00, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'RA-MEDIUM-FLAG', 'Medium Risk Flag Action', 2, 2, 100.00, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'RA-HIGH-FLAG', 'High Risk Flag Action', 3, 2, 100.00, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0),
(1, 'RA-CRITICAL-BLOCK', 'Critical Risk Block Action', 4, 3, 0.00, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0);

-- -----------------------------------------------------------------------------
-- 2.4 Odds Threshold Rule (t_turnover_odds_threshold_rule) - 1 entry
-- -----------------------------------------------------------------------------

INSERT INTO t_turnover_odds_threshold_rule (tenant_id, rule_code, rule_name, odds_type, threshold_value, comparison_operator, effective_from, effective_to, status, deleted, version) VALUES
(1, 'OT-EUR-GE-1.50', 'European Odds >= 1.50 Threshold', 1, 1.5000, '>=', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '10 years', 1, FALSE, 0);

-- ======================================================================================
-- 3. Risk Rule Parameters (t_risk_rule_param)
-- ======================================================================================

INSERT INTO t_risk_rule_param (tenant_id, rule_type, rule_name, rule_description, threshold_value, time_window_seconds, max_count, weight, enabled, params_json, deleted, version) VALUES
(1, 1, 'Blacklist Check', 'Check if player is on fraud blacklist', NULL, NULL, NULL, 50.0000, TRUE, '{"sources": ["internal", "third_party"], "auto_block": true}'::jsonb, FALSE, 0),
(1, 2, 'Arbitrage Detection', 'Detect hedging across multiple accounts', 0.9500, 300, NULL, 30.0000, TRUE, '{"correlation_threshold": 0.95, "check_ip": true}'::jsonb, FALSE, 0),
(1, 3, 'Bot Betting Detection', 'Detect automated/bot betting behavior', NULL, 60, 10, 40.0000, TRUE, '{"min_interval_ms": 500, "pattern_detection": true}'::jsonb, FALSE, 0),
(1, 4, 'Low Odds Abuse', 'Detect consistent betting on low-odds events', 1.1000, 86400, 20, 25.0000, TRUE, '{"consecutive_count": 5, "avg_odds_threshold": 1.15}'::jsonb, FALSE, 0),
(1, 5, 'High Frequency Betting', 'Detect abnormally high betting frequency', NULL, 3600, 50, 20.0000, TRUE, '{"burst_threshold": 10, "burst_window_seconds": 60}'::jsonb, FALSE, 0),
(1, 6, 'Abnormal Bet Size', 'Detect sudden large bets (> 10x average)', 10.0000, NULL, NULL, 35.0000, TRUE, '{"lookback_days": 30, "min_historical_bets": 10}'::jsonb, FALSE, 0);

-- ======================================================================================
-- 4. Geo-Restrictions (t_geo_restriction)
-- ======================================================================================

INSERT INTO t_geo_restriction (tenant_id, country_code, country_name, restriction_type, reason, enabled) VALUES
(1, 'US', 'United States', 1, 'Federal prohibition on online gambling (UIGEA 2006)', TRUE),
(1, 'CN', 'China', 1, 'Strict prohibition on all forms of online gambling', TRUE);

-- ======================================================================================
-- Verification: Total 30 entries
-- ======================================================================================
-- t_promotion_rule: 2 entries
-- t_turnover_game_weight_rule: 6 entries
-- t_turnover_status_factor_rule: 9 entries
-- t_turnover_risk_action_rule: 4 entries
-- t_turnover_odds_threshold_rule: 1 entry
-- t_risk_rule_param: 6 entries
-- t_geo_restriction: 2 entries
-- TOTAL: 30 entries
