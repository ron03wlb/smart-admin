-- =====================================================
-- LiteFlow Turnover Calculation Chain Seed Data
-- Migrates chain from dynamic @PostConstruct creation to database-driven rules
-- =====================================================
--
-- PURPOSE:
-- This migration ensures the LiteFlow turnover calculation chain exists in the database
-- BEFORE the application starts, allowing LiteFlow to load rules from the database.
--
-- BACKGROUND:
-- Previously, the chain was created dynamically in TurnoverCalculationService.@PostConstruct,
-- but LiteFlow's execute2Resp() was returning null because rules were not properly loaded
-- from the database. This migration solves that issue by pre-seeding the chain.
--
-- CHAIN DETAILS:
-- - chain_code: turnover_calculation_main (unique business key)
-- - chain_data: THEN(riskFilterNode, statusFactorNode, gameWeightNode, turnoverAggregateNode)
-- - All 4 nodes are Java @LiteflowComponent classes (NOT script nodes)
-- - No t_liteflow_script inserts needed
--
-- IDEMPOTENCY:
-- Uses ON CONFLICT DO NOTHING to allow safe re-execution
-- =====================================================

-- Insert Turnover Calculation Main Chain
INSERT INTO t_liteflow_chain (
    tenant_id,
    chain_name,
    chain_code,
    chain_type,
    chain_data,
    version,
    status,
    deleted_flag,
    remark,
    create_user_id,
    create_user_name,
    update_user_id,
    update_user_name,
    create_time,
    update_time
) VALUES (
    1,                                  -- tenant_id: Test tenant
    '流水計算主流程',                    -- chain_name: Display name
    'turnover_calculation_main',        -- chain_code: Unique business key
    1,                                  -- chain_type: 1=串行流程 (sequential)
    'THEN(riskFilterNode, statusFactorNode, gameWeightNode, turnoverAggregateNode)',  -- chain_data: EL expression
    1,                                  -- version: Initial version
    1,                                  -- status: 1=啟用 (enabled)
    0,                                  -- deleted_flag: 0=未刪除 (not deleted)
    'Turnover calculation chain for bet settlement - 3-layer verification',  -- remark
    0,                                  -- create_user_id: SYSTEM
    'SYSTEM',                           -- create_user_name
    0,                                  -- update_user_id: SYSTEM
    'SYSTEM',                           -- update_user_name
    NOW(),                              -- create_time
    NOW()                               -- update_time
)
ON CONFLICT (tenant_id, chain_code) WHERE deleted_flag = 0 DO NOTHING;

-- =====================================================
-- VERIFICATION:
-- Run FlywayMigrationIntegrationTest to verify:
-- 1. Migration executes successfully (flywayMigrationCount = 10)
-- 2. Schema version is "010"
-- 3. Chain exists in database with correct EL expression
-- =====================================================
