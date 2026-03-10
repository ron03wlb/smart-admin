# 錢包性能測試執行報告 (Wallet Performance Test Execution Report)

**測試日期**: 2026-03-10
**測試工具**: k6 v0.50.0+
**測試目標**: 驗證三層防護機制（Redisson 鎖 + 樂觀鎖 + Request ID 去重）
**版本**: 1.0.0

---

## 📋 測試執行清單 (Test Execution Checklist)

### 階段 1: 環境準備 (Environment Setup)

#### ✅ 1.1 檢查必要服務

**PostgreSQL**:
```bash
# 檢查 PostgreSQL 是否運行
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "SELECT version();"

# 預期輸出: PostgreSQL 16.x
```

**Redis**:
```bash
# 檢查 Redis 是否運行
redis-cli ping

# 預期輸出: PONG
```

**k6**:
```bash
# 檢查 k6 是否安裝
k6 version

# 預期輸出: k6 v0.50.0 (或更高版本)
```

**結果**:
- [ ] PostgreSQL: ⬜ 未檢查 / ✅ 正常 / ❌ 失敗
- [ ] Redis: ⬜ 未檢查 / ✅ 正常 / ❌ 失敗
- [ ] k6: ⬜ 未檢查 / ✅ 正常 / ❌ 失敗

---

#### ✅ 1.2 啟動 SmartAdmin 應用程序

**方法 1: Gradle 啟動（開發環境）**:
```bash
cd c:/Workspace/open_source/smart-admin/smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun

# 等待啟動完成（約 30-60 秒）
# 預期日誌: "Started SmartAdminApplication in X.XXX seconds"
```

**方法 2: JAR 啟動（生產環境）**:
```bash
cd c:/Workspace/open_source/smart-admin/smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootJar
java -jar smartadmin-app/build/libs/smartadmin-app-*.jar
```

**驗證啟動成功**:
```bash
# 檢查健康狀態
curl http://localhost:1024/actuator/health

# 預期輸出: {"status":"UP"}
```

**結果**:
- [ ] 應用程序啟動: ⬜ 未啟動 / ✅ 成功 / ❌ 失敗
- [ ] 健康檢查: ⬜ 未檢查 / ✅ 正常 / ❌ 失敗

---

### 階段 2: 測試數據準備 (Test Data Preparation)

#### ✅ 2.1 創建測試錢包

**SQL 腳本** (`create-test-wallets.sql`):
```sql
-- =====================================================================
-- Create 100 Test Wallets for k6 Performance Testing
-- =====================================================================
-- Each wallet:
--   - Initial balance: 10,000 CNY
--   - Player: test_player_001 ~ test_player_100
--   - Tenant: 1 (default tenant)
-- =====================================================================

DO $$
DECLARE
    i INT;
    v_player_id BIGINT;
    v_wallet_id BIGINT;
BEGIN
    -- Set tenant context (Multi-Tenant RLS)
    PERFORM set_config('app.current_tenant_id', '1', false);

    FOR i IN 1..100 LOOP
        -- Insert test player
        INSERT INTO t_player (
            username,
            password_hash,
            email_encrypted,
            email_blind_idx,
            phone_encrypted,
            phone_blind_idx,
            status,
            kyc_level,
            vip_level,
            registration_ip,
            deleted,
            version,
            tenant_id,
            create_time,
            update_time
        )
        VALUES (
            'test_player_' || LPAD(i::TEXT, 3, '0'),
            '$argon2id$v=19$m=65536,t=3,p=4$dummyhash',  -- Dummy password hash
            'test' || i || '@k6test.com',  -- Email (will be encrypted by TypeHandler)
            encode(digest('test' || i || '@k6test.com', 'sha256'), 'hex'),  -- Blind index
            '+86138' || LPAD(i::TEXT, 8, '0'),  -- Phone (will be encrypted)
            encode(digest('+86138' || LPAD(i::TEXT, 8, '0'), 'sha256'), 'hex'),  -- Blind index
            1,  -- ACTIVE
            1,  -- KYC L1 (Basic Verification)
            1,  -- VIP Level 1
            '127.0.0.1',
            FALSE,  -- deleted
            0,      -- version
            1,      -- tenant_id
            NOW(),
            NOW()
        )
        RETURNING player_id INTO v_player_id;

        -- Insert CASH wallet
        INSERT INTO t_wallet (
            player_id,
            wallet_type,
            balance,
            locked_amount,
            currency_code,
            status,
            deleted,
            version,
            tenant_id,
            create_time,
            update_time
        )
        VALUES (
            v_player_id,
            1,  -- CASH
            10000.0000,  -- 10,000 CNY
            0.0000,
            'CNY',
            1,  -- ACTIVE
            FALSE,
            0,
            1,
            NOW(),
            NOW()
        )
        RETURNING wallet_id INTO v_wallet_id;

        -- Log progress every 10 wallets
        IF i % 10 = 0 THEN
            RAISE NOTICE 'Created % test wallets...', i;
        END IF;
    END LOOP;

    RAISE NOTICE '✅ Successfully created 100 test wallets with 10,000 CNY each';
END $$;
```

**執行腳本**:
```bash
# 執行測試數據創建腳本
psql -h localhost -U smartadmin_user -d smartadmin_igaming -f create-test-wallets.sql

# 驗證數據創建成功
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "
SELECT COUNT(*) AS wallet_count, SUM(balance) AS total_balance
FROM t_wallet
WHERE currency_code = 'CNY' AND deleted = FALSE;
"

# 預期輸出:
#  wallet_count | total_balance
# --------------+---------------
#           100 |  1000000.0000
```

**結果**:
- [ ] 測試錢包創建: ⬜ 未執行 / ✅ 成功 (100 個錢包) / ❌ 失敗

---

#### ✅ 2.2 獲取 Access Token

**方法 1: cURL 獲取**:
```bash
# 登入並獲取 Access Token
curl -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }' | jq -r '.data.accessToken'

# 保存到環境變量
export ACCESS_TOKEN=$(curl -s -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}' | jq -r '.data.accessToken')

# 驗證 Token
echo $ACCESS_TOKEN
```

**方法 2: PowerShell 獲取**:
```powershell
# 登入並獲取 Access Token
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:1024/api/system/login" `
  -ContentType "application/json" `
  -Body '{"username": "admin", "password": "123456"}'

$env:ACCESS_TOKEN = $response.data.accessToken

# 驗證 Token
Write-Host $env:ACCESS_TOKEN
```

**結果**:
- [ ] Access Token 獲取: ⬜ 未執行 / ✅ 成功 / ❌ 失敗
- [ ] Token 值: `_______________________________________`

---

### 階段 3: 執行 k6 測試 (Run k6 Test)

#### ✅ 3.1 準備測試腳本

**檢查腳本位置**:
```bash
cd c:/Workspace/open_source/smart-admin/docs/testing/performance

# 確認腳本存在
ls -l k6-wallet-concurrency-test.js

# 預期輸出: -rw-r--r-- ... k6-wallet-concurrency-test.js
```

**修改測試配置（可選）**:
```javascript
// 修改 k6-wallet-concurrency-test.js 中的配置
export const options = {
  scenarios: {
    // 場景 1: 漸進負載測試（推薦用於首次測試）
    concurrent_debit_rampup: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 50 },   // 熱身 50 VUs
        { duration: '1m', target: 200 },   // 增加到 200 VUs
        { duration: '2m', target: 500 },   // 增加到 500 VUs
        { duration: '3m', target: 1000 },  // 峰值負載 1000 VUs
        { duration: '2m', target: 1000 },  // 持續 1000 VUs
        { duration: '30s', target: 0 },    // 降載
      ],
    },
  },
};
```

---

#### ✅ 3.2 執行測試

**完整測試（17 分鐘）**:
```bash
cd c:/Workspace/open_source/smart-admin/docs/testing/performance

# 執行完整測試套件
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --out json=results/wallet-concurrency-test-$(date +%Y%m%d-%H%M%S).json \
  k6-wallet-concurrency-test.js

# Windows PowerShell 版本
k6 run `
  --env BASE_URL=http://localhost:1024 `
  --env ACCESS_TOKEN="$env:ACCESS_TOKEN" `
  --out json=results/wallet-concurrency-test-$(Get-Date -Format 'yyyyMMdd-HHmmss').json `
  k6-wallet-concurrency-test.js
```

**快速測試（5 分鐘，用於驗證）**:
```bash
# 僅執行場景 1（漸進負載），跳過場景 2 和 3
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --env SCENARIO=rampup \
  --out json=results/wallet-quick-test.json \
  k6-wallet-concurrency-test.js
```

**測試進度監控**:
```bash
# k6 會實時顯示進度，例如:
#
#   execution: local
#      script: k6-wallet-concurrency-test.js
#      output: json (results/wallet-concurrency-test-20260310-143052.json)
#
#   scenarios: (100.00%) 1 scenario, 1000 max VUs, 17m30s max duration
#                * concurrent_debit_rampup: 1000.00 iters/s (100.00% of max 1000.00)
#
#     ✓ status is 200
#     ✓ response has code=1 (success)
#     ✓ response contains transactionId
#     ✓ response time < 100ms
#
#     checks.........................: 100.00% ✓ 150000 ✗ 0
#     http_req_duration..............: avg=45.23ms  min=12.34ms med=42.56ms max=98.76ms p(90)=67.89ms p(95)=78.45ms
#     http_reqs......................: 37500   625/s
#     successful_debits..............: 37500   625/s
#     duplicate_debits...............: 0       0/s
#     optimistic_lock_failures.......: 123     2.05/s
```

**結果**:
- [ ] 測試執行: ⬜ 未執行 / ✅ 完成 / ❌ 失敗
- [ ] 測試時長: `_______ 分鐘`
- [ ] 總請求數: `_______`
- [ ] 成功率: `_______% `

---

### 階段 4: 結果分析 (Result Analysis)

#### ✅ 4.1 驗收標準檢查

**k6 控制台輸出驗證**:
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 驗收項                  │ 標準          │ 實際值        │ 狀態            │
├─────────────────────────────────────────────────────────────────────────────┤
│ TPS (Throughput)        │ > 500         │ _____ req/s   │ ⬜ PASS / FAIL │
│ P95 響應時間            │ < 100ms       │ _____ ms      │ ⬜ PASS / FAIL │
│ 錯誤率                  │ < 0.1%        │ _____ %       │ ⬜ PASS / FAIL │
│ 重複扣款                │ 0 次          │ _____ 次      │ ⬜ PASS / FAIL │
│ 樂觀鎖衝突率            │ < 1%          │ _____ %       │ ⬜ PASS / FAIL │
└─────────────────────────────────────────────────────────────────────────────┘
```

**從 k6 JSON 輸出提取指標**:
```bash
# 安裝 jq (JSON 處理工具)
# Windows: choco install jq
# macOS: brew install jq
# Linux: apt-get install jq

# 提取關鍵指標
JSON_FILE="results/wallet-concurrency-test-*.json"

# TPS (http_reqs rate)
jq -r '.metrics.http_reqs.values.rate' $JSON_FILE

# P95 響應時間
jq -r '.metrics.http_req_duration.values["p(95)"]' $JSON_FILE

# 錯誤率
jq -r '.metrics.http_req_failed.values.rate' $JSON_FILE

# 重複扣款次數
jq -r '.metrics.duplicateDebits.values.count' $JSON_FILE

# 樂觀鎖衝突次數
jq -r '.metrics.optimisticLockFailures.values.count' $JSON_FILE
```

---

#### ✅ 4.2 餘額一致性驗證

**驗證 SQL 查詢**:
```sql
-- =====================================================================
-- Balance Consistency Verification
-- =====================================================================
-- Verifies that no duplicate debits occurred during k6 test
-- =====================================================================

-- Step 1: Calculate expected balance
-- Initial: 100 wallets × 10,000 CNY = 1,000,000 CNY
-- Expected after test: 1,000,000 CNY - (Total successful debits)

WITH test_summary AS (
    SELECT
        COUNT(DISTINCT wallet_id) AS wallet_count,
        SUM(balance) AS current_total_balance,
        SUM(locked_amount) AS current_locked_amount
    FROM t_wallet
    WHERE currency_code = 'CNY' AND deleted = FALSE
),
transaction_summary AS (
    SELECT
        COUNT(*) AS total_transactions,
        SUM(CASE WHEN transaction_type = 3 THEN amount ELSE 0 END) AS total_debits,
        COUNT(DISTINCT request_id) AS unique_requests,
        COUNT(*) - COUNT(DISTINCT request_id) AS duplicate_requests
    FROM t_wallet_transaction
    WHERE created_time >= NOW() - INTERVAL '1 hour'  -- Adjust based on test start time
)
SELECT
    -- Current State
    ts.wallet_count AS "錢包數量",
    ts.current_total_balance AS "當前總餘額 (CNY)",
    ts.current_locked_amount AS "鎖定金額 (CNY)",

    -- Transaction Summary
    tx.total_transactions AS "總交易筆數",
    tx.total_debits AS "總扣款金額 (CNY)",
    tx.unique_requests AS "唯一請求數",
    tx.duplicate_requests AS "重複請求數 (應為 0)",

    -- Consistency Check
    CASE
        WHEN tx.duplicate_requests = 0 THEN '✅ PASS: 無重複扣款'
        ELSE '❌ FAIL: 發現重複扣款'
    END AS "一致性檢查結果"
FROM test_summary ts, transaction_summary tx;
```

**執行驗證**:
```bash
psql -h localhost -U smartadmin_user -d smartadmin_igaming -f verify-balance-consistency.sql
```

**預期結果** (範例):
```
 錢包數量 | 當前總餘額 (CNY) | 鎖定金額 (CNY) | 總交易筆數 | 總扣款金額 (CNY) | 唯一請求數 | 重複請求數 | 一致性檢查結果
----------+------------------+----------------+------------+------------------+------------+------------+------------------------
      100 |      925000.0000 |         0.0000 |      37500 |       75000.0000 |      37500 |          0 | ✅ PASS: 無重複扣款
```

**驗證清單**:
- [ ] 重複請求數 = 0: ⬜ 未驗證 / ✅ PASS / ❌ FAIL
- [ ] 餘額一致性: ⬜ 未驗證 / ✅ PASS / ❌ FAIL

---

#### ✅ 4.3 三層防護機制驗證

**檢查 1: Redisson 分布式鎖**
```bash
# 檢查 Redis 鎖鍵（測試期間）
redis-cli --scan --pattern "wallet:lock:*"

# 測試結束後應該沒有遺留鎖
redis-cli --scan --pattern "wallet:lock:*" | wc -l
# 預期輸出: 0
```

**檢查 2: MyBatis Plus 樂觀鎖**
```sql
-- 檢查樂觀鎖版本號變化
SELECT
    wallet_id,
    player_id,
    balance,
    version,
    update_time
FROM t_wallet
WHERE currency_code = 'CNY' AND deleted = FALSE
ORDER BY version DESC
LIMIT 10;

-- 預期: version 應該 > 0 (表示發生過更新)
```

**檢查 3: Request ID 去重**
```sql
-- 檢查是否有重複的 request_id
SELECT
    request_id,
    COUNT(*) AS occurrence_count
FROM t_wallet_transaction
WHERE created_time >= NOW() - INTERVAL '1 hour'
GROUP BY request_id
HAVING COUNT(*) > 1;

-- 預期輸出: 0 rows (無重複)
```

**驗證清單**:
- [ ] Redisson 鎖正常釋放: ⬜ 未驗證 / ✅ PASS / ❌ FAIL
- [ ] 樂觀鎖版本遞增: ⬜ 未驗證 / ✅ PASS / ❌ FAIL
- [ ] Request ID 唯一性: ⬜ 未驗證 / ✅ PASS / ❌ FAIL

---

### 階段 5: 生成測試報告 (Generate Test Report)

#### ✅ 5.1 HTML 報告生成

k6 測試腳本已內建 HTML 報告生成功能，測試結束後會自動生成:
```bash
# 報告位置
open results/summary.html  # macOS
start results/summary.html  # Windows
xdg-open results/summary.html  # Linux
```

**HTML 報告內容**:
- ✅ 驗收標準表格（TPS, P95, 錯誤率, 重複扣款）
- ✅ 測試場景詳情
- ✅ 性能指標圖表
- ✅ 三層防護機制驗證結果

---

#### ✅ 5.2 匯出完整報告

**創建測試報告 Markdown**:
```markdown
# 錢包並發扣款性能測試報告

**測試日期**: 2026-03-10
**測試時長**: 17 分鐘
**測試工具**: k6 v0.50.0
**並發用戶**: 1000 VUs (Virtual Users)

## 測試結果摘要

| 指標 | 標準 | 實際值 | 狀態 |
|------|------|--------|------|
| TPS (Throughput) | > 500 | 625 req/s | ✅ PASS |
| P95 響應時間 | < 100ms | 78.45 ms | ✅ PASS |
| 錯誤率 | < 0.1% | 0.02% | ✅ PASS |
| 重複扣款 | 0 次 | 0 次 | ✅ PASS |
| 樂觀鎖衝突率 | < 1% | 0.33% | ✅ PASS |

## 三層防護機制驗證

- ✅ **Layer 1 (Redisson 分布式鎖)**: 無遺留鎖，正常釋放
- ✅ **Layer 2 (MyBatis Plus 樂觀鎖)**: 版本號正常遞增，衝突率 0.33%
- ✅ **Layer 3 (Request ID 去重)**: 無重複 request_id，100% 唯一性

## 餘額一致性驗證

- 初始餘額: 1,000,000 CNY (100 錢包 × 10,000 CNY)
- 總扣款金額: 75,000 CNY (37,500 筆交易)
- 當前餘額: 925,000 CNY
- **一致性檢查**: ✅ PASS (無重複扣款)

## 結論

✅ **測試通過** - 三層防護機制有效防止重複扣款，系統性能符合 iGaming 實施計劃驗收標準。
```

---

## 📊 測試結果記錄 (Test Results Record)

### 實際測試執行記錄

**測試執行時間**: `________________________`
**測試執行人**: `________________________`

**環境配置**:
- SmartAdmin 版本: v4.1.0
- PostgreSQL 版本: `____________`
- Redis 版本: `____________`
- k6 版本: `____________`

**性能指標實際值**:
```
TPS (Throughput):              _______ req/s
P95 響應時間:                  _______ ms
P99 響應時間:                  _______ ms
平均響應時間:                  _______ ms
錯誤率:                        _______ %
重複扣款次數:                  _______ 次
樂觀鎖衝突次數:                _______ 次
樂觀鎖衝突率:                  _______ %
```

**驗收標準通過情況**:
- [ ] TPS > 500: ⬜ PASS / FAIL
- [ ] P95 < 100ms: ⬜ PASS / FAIL
- [ ] 錯誤率 < 0.1%: ⬜ PASS / FAIL
- [ ] 重複扣款 = 0: ⬜ PASS / FAIL
- [ ] 樂觀鎖衝突率 < 1%: ⬜ PASS / FAIL

**三層防護機制驗證**:
- [ ] Redisson 分布式鎖: ⬜ PASS / FAIL
- [ ] MyBatis Plus 樂觀鎖: ⬜ PASS / FAIL
- [ ] Request ID 去重: ⬜ PASS / FAIL

**餘額一致性驗證**:
- [ ] 無重複扣款: ⬜ PASS / FAIL
- [ ] 餘額計算正確: ⬜ PASS / FAIL

---

## 🚨 故障排查 (Troubleshooting)

### 問題 1: 應用程序啟動失敗

**症狀**:
```
Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
```

**可能原因**:
1. PostgreSQL 未運行或連接失敗
2. Redis 未運行或連接失敗
3. 端口 1024 已被占用

**解決方案**:
```bash
# 檢查 PostgreSQL
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "SELECT 1;"

# 檢查 Redis
redis-cli ping

# 檢查端口占用
netstat -ano | findstr :1024  # Windows
lsof -i :1024  # macOS/Linux
```

---

### 問題 2: k6 測試失敗率過高

**症狀**:
```
✗ status is 200
✗ response has code=1 (success)
http_req_failed................: 25.00% ✓ 9375 ✗ 28125
```

**可能原因**:
1. Access Token 過期或無效
2. 應用程序性能瓶頸（CPU/內存不足）
3. PostgreSQL 連接池耗盡

**解決方案**:
```bash
# 重新獲取 Access Token
export ACCESS_TOKEN=$(curl -s -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}' | jq -r '.data.accessToken')

# 檢查應用程序資源使用
top -p $(pgrep -f smartadmin-app)

# 檢查 PostgreSQL 連接數
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "
SELECT count(*) AS connection_count
FROM pg_stat_activity
WHERE datname = 'smartadmin_igaming';
"
```

---

### 問題 3: 發現重複扣款

**症狀**:
```sql
-- 餘額一致性驗證查詢顯示:
重複請求數 (應為 0): 15  -- ❌ FAIL
```

**可能原因**:
1. Request ID 生成邏輯有問題（不唯一）
2. 唯一索引 `uk_wallet_tx_request_id` 未創建成功
3. 冪等性檢查邏輯有 bug

**解決方案**:
```sql
-- 檢查唯一索引是否存在
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 't_wallet_transaction' AND indexname LIKE '%request_id%';

-- 檢查重複的 request_id
SELECT request_id, COUNT(*) AS count
FROM t_wallet_transaction
WHERE created_time >= NOW() - INTERVAL '1 hour'
GROUP BY request_id
HAVING COUNT(*) > 1;

-- 如果發現重複，檢查 k6 腳本的 requestId 生成邏輯
-- 應該是: `k6-test-${__VU}-${__ITER}-${Date.now()}-${randomString(8)}`
```

---

## 📚 相關文檔 (Related Documentation)

- **k6 測試腳本**: [k6-wallet-concurrency-test.js](./k6-wallet-concurrency-test.js)
- **k6 執行指南**: [K6_WALLET_PERFORMANCE_TEST_GUIDE.md](./K6_WALLET_PERFORMANCE_TEST_GUIDE.md)
- **性能測試套件總覽**: [README.md](./README.md)
- **iGaming 實施計劃**: [C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md](C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md)

---

**文檔版本**: 1.0.0
**最後更新**: 2026-03-10
**維護者**: SmartAdmin iGaming Team
