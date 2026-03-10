-- =====================================================================
-- Balance Consistency Verification
-- =====================================================================
-- Purpose: Verify that no duplicate debits occurred during k6 performance test
--
-- This query validates the three-layer protection mechanism:
--   Layer 1: Redisson distributed lock
--   Layer 2: MyBatis Plus optimistic lock (@Version)
--   Layer 3: Request ID deduplication (unique index uk_wallet_tx_request_id)
--
-- Expected Result: 重複請求數 (duplicate_requests) = 0
--
-- Usage:
--   psql -h localhost -U smartadmin_user -d smartadmin_igaming -f verify-balance-consistency.sql
-- =====================================================================

\echo '======================================================================'
\echo '錢包餘額一致性驗證報告 (Wallet Balance Consistency Verification Report)'
\echo '======================================================================'
\echo ''

-- Set tenant context for Multi-Tenant Row-Level Security
SELECT set_config('app.current_tenant_id', '1', false);

-- =====================================================================
-- Part 1: Wallet Summary (Current State)
-- =====================================================================

\echo '【Part 1: 錢包當前狀態】'
\echo ''

SELECT
    COUNT(*) AS "錢包數量",
    SUM(balance) AS "當前總餘額 (CNY)",
    SUM(locked_amount) AS "鎖定金額 (CNY)",
    MIN(balance) AS "最小餘額 (CNY)",
    MAX(balance) AS "最大餘額 (CNY)",
    AVG(balance) AS "平均餘額 (CNY)"
FROM t_wallet
WHERE currency_code = 'CNY' AND deleted = FALSE;

\echo ''

-- =====================================================================
-- Part 2: Transaction Summary (Test Period)
-- =====================================================================

\echo '【Part 2: 交易匯總（最近 1 小時）】'
\echo ''

SELECT
    COUNT(*) AS "總交易筆數",
    COUNT(DISTINCT wallet_id) AS "涉及錢包數",
    COUNT(DISTINCT player_id) AS "涉及玩家數",
    SUM(CASE WHEN transaction_type = 3 THEN amount ELSE 0 END) AS "總扣款金額 (CNY)",
    SUM(CASE WHEN transaction_type = 2 THEN amount ELSE 0 END) AS "總存款金額 (CNY)",
    COUNT(DISTINCT request_id) AS "唯一請求數",
    COUNT(*) - COUNT(DISTINCT request_id) AS "重複請求數 (應為 0)",
    MIN(created_time) AS "首筆交易時間",
    MAX(created_time) AS "末筆交易時間"
FROM t_wallet_transaction
WHERE created_time >= NOW() - INTERVAL '1 hour';

\echo ''

-- =====================================================================
-- Part 3: Duplicate Request Detection (Critical Check)
-- =====================================================================

\echo '【Part 3: 重複請求檢測（關鍵檢查）】'
\echo ''

WITH duplicate_check AS (
    SELECT
        request_id,
        COUNT(*) AS occurrence_count,
        SUM(amount) AS total_amount,
        STRING_AGG(transaction_id::TEXT, ', ') AS transaction_ids
    FROM t_wallet_transaction
    WHERE created_time >= NOW() - INTERVAL '1 hour'
    GROUP BY request_id
    HAVING COUNT(*) > 1
)
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN '✅ PASS: 無重複請求'
        ELSE '❌ FAIL: 發現 ' || COUNT(*) || ' 個重複請求'
    END AS "重複請求檢測結果",
    COALESCE(COUNT(*), 0) AS "重複請求數",
    COALESCE(SUM(occurrence_count - 1), 0) AS "重複交易總數",
    COALESCE(SUM(total_amount * (occurrence_count - 1)), 0.0000) AS "重複扣款總金額 (CNY)"
FROM duplicate_check;

-- Show duplicate details (if any)
\echo ''
\echo '【重複請求詳情（如果有）】'
\echo ''

SELECT
    request_id AS "重複的 Request ID",
    COUNT(*) AS "出現次數",
    SUM(amount) AS "累計金額 (CNY)",
    STRING_AGG(transaction_id::TEXT, ', ') AS "交易 ID 列表"
FROM t_wallet_transaction
WHERE created_time >= NOW() - INTERVAL '1 hour'
GROUP BY request_id
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC, SUM(amount) DESC;

\echo ''

-- =====================================================================
-- Part 4: Optimistic Lock Analysis
-- =====================================================================

\echo '【Part 4: 樂觀鎖版本分析】'
\echo ''

SELECT
    MIN(version) AS "最小版本號",
    MAX(version) AS "最大版本號",
    AVG(version) AS "平均版本號",
    COUNT(DISTINCT version) AS "版本數量",
    COUNT(*) FILTER (WHERE version = 0) AS "未更新錢包數 (version=0)"
FROM t_wallet
WHERE currency_code = 'CNY' AND deleted = FALSE;

\echo ''

-- =====================================================================
-- Part 5: Balance Consistency Validation
-- =====================================================================

\echo '【Part 5: 餘額一致性驗證】'
\echo ''

WITH initial_state AS (
    -- Assume initial balance was 100 wallets × 10,000 CNY = 1,000,000 CNY
    SELECT 1000000.0000 AS initial_total_balance
),
transaction_summary AS (
    SELECT
        COALESCE(SUM(CASE WHEN transaction_type = 3 THEN amount ELSE 0 END), 0.0000) AS total_debits,
        COALESCE(SUM(CASE WHEN transaction_type = 2 THEN amount ELSE 0 END), 0.0000) AS total_deposits,
        COUNT(DISTINCT request_id) AS unique_requests,
        COUNT(*) - COUNT(DISTINCT request_id) AS duplicate_requests
    FROM t_wallet_transaction
    WHERE created_time >= NOW() - INTERVAL '1 hour'
),
current_state AS (
    SELECT
        COALESCE(SUM(balance), 0.0000) AS current_total_balance,
        COALESCE(SUM(locked_amount), 0.0000) AS current_locked_amount
    FROM t_wallet
    WHERE currency_code = 'CNY' AND deleted = FALSE
)
SELECT
    -- Initial State
    i.initial_total_balance AS "初始餘額 (CNY)",

    -- Transaction Summary
    tx.total_debits AS "總扣款金額 (CNY)",
    tx.total_deposits AS "總存款金額 (CNY)",
    tx.unique_requests AS "唯一請求數",
    tx.duplicate_requests AS "重複請求數",

    -- Current State
    c.current_total_balance AS "當前總餘額 (CNY)",
    c.current_locked_amount AS "鎖定金額 (CNY)",

    -- Consistency Check
    i.initial_total_balance - tx.total_debits + tx.total_deposits AS "預期餘額 (CNY)",
    c.current_total_balance - (i.initial_total_balance - tx.total_debits + tx.total_deposits) AS "餘額差異 (CNY)",

    -- Pass/Fail Result
    CASE
        WHEN tx.duplicate_requests = 0
             AND ABS(c.current_total_balance - (i.initial_total_balance - tx.total_debits + tx.total_deposits)) < 0.01
        THEN '✅ PASS: 餘額一致性驗證通過'
        WHEN tx.duplicate_requests > 0
        THEN '❌ FAIL: 發現重複扣款'
        ELSE '❌ FAIL: 餘額不一致'
    END AS "一致性檢查結果"
FROM initial_state i, transaction_summary tx, current_state c;

\echo ''

-- =====================================================================
-- Part 6: Top 10 Wallets by Transaction Count
-- =====================================================================

\echo '【Part 6: 交易次數最多的前 10 個錢包】'
\echo ''

SELECT
    w.wallet_id AS "錢包 ID",
    w.player_id AS "玩家 ID",
    w.balance AS "當前餘額 (CNY)",
    w.locked_amount AS "鎖定金額 (CNY)",
    w.version AS "版本號",
    COUNT(wt.transaction_id) AS "交易次數",
    SUM(CASE WHEN wt.transaction_type = 3 THEN wt.amount ELSE 0 END) AS "總扣款 (CNY)"
FROM t_wallet w
LEFT JOIN t_wallet_transaction wt
    ON w.wallet_id = wt.wallet_id
    AND wt.created_time >= NOW() - INTERVAL '1 hour'
WHERE w.currency_code = 'CNY' AND w.deleted = FALSE
GROUP BY w.wallet_id, w.player_id, w.balance, w.locked_amount, w.version
ORDER BY COUNT(wt.transaction_id) DESC
LIMIT 10;

\echo ''
\echo '======================================================================'
\echo '驗證完成 (Verification Complete)'
\echo '======================================================================'
