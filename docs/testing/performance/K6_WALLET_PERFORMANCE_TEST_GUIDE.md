# k6 錢包並發扣款性能測試指南

**版本**: 1.0.0
**測試工具**: k6 v0.49.0
**目標**: 驗證三層防護機制 + 性能驗收標準
**生成日期**: 2026-03-10

---

## 📋 測試目標

驗證 SmartAdmin iGaming 錢包模塊在高並發場景下的性能和正確性：

**功能目標**:
- ✅ 驗證三層防護機制（Redisson 鎖 + 樂觀鎖 + Request ID 去重）
- ✅ 確保錢包餘額一致性（無重複扣款）
- ✅ 測試冪等性保證（相同 requestId 返回原結果）

**性能目標**（來自 iGaming 實施計劃）:
- ✅ **TPS** (Throughput): > 500 requests/second
- ✅ **P95 響應時間**: < 100ms
- ✅ **錯誤率**: < 0.1%
- ✅ **重複扣款**: 0 次（🔴 關鍵驗收標準）

---

## 🚀 快速開始

### 前置要求

**1. 安裝 k6**

```bash
# macOS (Homebrew)
brew install k6

# Windows (Chocolatey)
choco install k6

# Windows (Scoop)
scoop install k6

# Linux (Debian/Ubuntu)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# 驗證安裝
k6 version
```

**預期輸出**: `k6 v0.49.0 (go1.21.6, linux/amd64)`

**2. 準備測試環境**

確保以下服務正在運行：

```bash
# 檢查 SmartAdmin 應用程序
curl http://localhost:1024/actuator/health
# 預期響應: {"status":"UP"}

# 檢查 PostgreSQL
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "SELECT 1;"

# 檢查 Redis
redis-cli ping
# 預期響應: PONG
```

**3. 準備測試數據**

運行以下 SQL 腳本創建 100 個測試錢包：

```sql
-- Create test wallets with initial balance of 10,000.0000
DO $$
BEGIN
  FOR i IN 1..100 LOOP
    INSERT INTO t_wallet (wallet_id, player_id, wallet_type, currency_code, balance, locked_amount, version, deleted)
    VALUES (
      1000 + i,           -- wallet_id: 1001 ~ 1100
      100 + i,            -- player_id: 101 ~ 200
      1,                  -- wallet_type: CASH = 1
      'CNY',              -- currency_code
      10000.0000,         -- balance: 10,000 CNY
      0.0000,             -- locked_amount
      1,                  -- version (for optimistic lock)
      false               -- deleted
    )
    ON CONFLICT (wallet_id) DO UPDATE SET
      balance = 10000.0000,
      locked_amount = 0.0000,
      version = 1;
  END LOOP;
END $$;

-- Verify test wallets
SELECT wallet_id, player_id, balance, locked_amount, version
FROM t_wallet
WHERE wallet_id BETWEEN 1001 AND 1100
ORDER BY wallet_id
LIMIT 10;
```

**4. 獲取測試用 Access Token**

```bash
# 登入並獲取 token
curl -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }' | jq -r '.data.accessToken'
```

**保存 token 到環境變量**:
```bash
export ACCESS_TOKEN="<your-access-token-here>"
```

---

## 📝 運行測試

### 測試場景 1: 漸進負載測試（推薦）

**目的**: 逐步增加負載，觀察系統行為變化。

```bash
cd docs/testing/performance

# 運行測試（使用環境變量）
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --out json=results/wallet-concurrency-test-result.json \
  k6-wallet-concurrency-test.js
```

**測試流程**:
1. **Warm-up** (2 分鐘): 0 → 100 VUs
2. **Ramp-up** (3 分鐘): 100 → 500 VUs
3. **Peak Load** (5 分鐘): 500 → 1000 VUs
4. **Sustain** (5 分鐘): 保持 1000 VUs
5. **Ramp-down** (2 分鐘): 1000 → 0 VUs

**總時長**: ~17 分鐘

---

### 測試場景 2: 壓力測試

**目的**: 持續高負載測試系統穩定性。

```bash
# 僅運行壓力測試場景
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --scenarios concurrent_debit_stress \
  --out json=results/wallet-stress-test-result.json \
  k6-wallet-concurrency-test.js
```

**測試參數**:
- 並發用戶: 1000 VUs
- 持續時間: 10 分鐘
- 預期 TPS: > 500

---

### 測試場景 3: 尖峰測試

**目的**: 測試系統對突發流量的應對能力。

```bash
# 僅運行尖峰測試場景
k6 run \
  --env BASE_URL=http://localhost:1024 \
  --env ACCESS_TOKEN="$ACCESS_TOKEN" \
  --scenarios concurrent_debit_spike \
  --out json=results/wallet-spike-test-result.json \
  k6-wallet-concurrency-test.js
```

**測試參數**:
- 突發流量: 0 → 2000 VUs（10 秒內）
- 保持時間: 1 分鐘
- 下降時間: 10 秒

---

## 📊 查看測試結果

### 實時控制台輸出

k6 會在控制台實時顯示測試進度：

```
          /\      |‾‾| /‾‾/   /‾‾/
     /\  /  \     |  |/  /   /  /
    /  \/    \    |     (   /   ‾‾\
   /          \   |  |\  \ |  (‾)  |
  / __________ \  |__| \__\ \_____/ .io

  execution: local
     script: k6-wallet-concurrency-test.js
     output: json (results/wallet-concurrency-test-result.json)

  scenarios: (100.00%) 3 scenarios, 2000 max VUs, 40m30s max duration (incl. graceful stop):
           * concurrent_debit_rampup: Up to 1000 looping VUs for 17m0s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)
           * concurrent_debit_stress: 1000 looping VUs for 10m0s (startTime: 20m0s, gracefulStop: 30s)
           * concurrent_debit_spike: Up to 2000 looping VUs for 1m20s over 3 stages (startTime: 35m0s, gracefulStop: 30s)


running (17m00.5s), 0000/1000 VUs, 125000 complete and 0 interrupted iterations
concurrent_debit_rampup ✓ [======================================] 1000 VUs  17m0s

     ✓ status is 200
     ✓ response has code=1 (success)
     ✓ response contains transactionId
     ✓ response time < 100ms

     checks.........................: 100.00% ✓ 500000      ✗ 0
     data_received..................: 125 MB  123 kB/s
     data_sent......................: 50 MB   49 kB/s
     duplicateDebits................: 0       0/s      ⭐ CRITICAL: PASS
     errorRate......................: 0.00%   ✓ 0           ✗ 125000
     http_req_blocked...............: avg=1.2ms    min=0µs      med=1µs      max=250ms   p(95)=5ms     p(99)=10ms
     http_req_connecting............: avg=800µs    min=0µs      med=0µs      max=150ms   p(95)=3ms     p(99)=8ms
   ✓ http_req_duration..............: avg=45ms     min=5ms      med=40ms     max=300ms   p(95)=85ms    p(99)=120ms
     http_req_failed................: 0.00%   ✓ 0           ✗ 125000
     http_req_receiving.............: avg=500µs    min=50µs     med=400µs    max=20ms    p(95)=1ms     p(99)=3ms
     http_req_sending...............: avg=300µs    min=20µs     med=250µs    max=10ms    p(95)=800µs   p(99)=2ms
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s       max=0s      p(95)=0s      p(99)=0s
     http_req_waiting...............: avg=44ms     min=4ms      med=39ms     max=290ms   p(95)=84ms    p(99)=118ms
   ✓ http_reqs......................: 125000  735.2/s  ⭐ TPS: PASS (> 500)
     iteration_duration.............: avg=1.3s     min=500ms    med=1.2s     max=5s      p(95)=2s      p(99)=3s
     iterations.....................: 125000  735.2/s
     optimisticLockFailures.........: 125     0.73/s   ℹ️ < 0.1% (acceptable)
     successfulDebits...............: 124875  734.5/s
     vus............................: 1000    min=0         max=1000
     vus_max........................: 1000    min=1000      max=1000
   ✓ wallet_debit_duration..........: avg=45ms     min=5ms      med=40ms     max=300ms   p(95)=85ms    p(99)=120ms

PASS: All thresholds met ✅
```

### HTML 報告

測試完成後會自動生成 HTML 報告：

```bash
# 報告位置
ls -lh results/summary.html

# 在瀏覽器中打開
open results/summary.html  # macOS
start results/summary.html  # Windows
xdg-open results/summary.html  # Linux
```

**HTML 報告包含**:
- ✅ 關鍵指標匯總表（TPS、P95、錯誤率、重複扣款）
- ✅ 響應時間分佈（P50/P90/P95/P99/Max）
- ✅ 三層防護機制驗證結果
- ✅ 驗收標準對比表
- ✅ 優化建議

---

### JSON 結果文件

```bash
# 查看 JSON 結果
cat results/wallet-concurrency-test-result.json | jq '.'

# 提取關鍵指標
cat results/wallet-concurrency-test-result.json | jq '.metrics.http_req_duration.values."p(95)"'
cat results/wallet-concurrency-test-result.json | jq '.metrics.http_reqs.values.rate'
cat results/wallet-concurrency-test-result.json | jq '.metrics.duplicateDebits.values.count'
```

---

## ✅ 驗收標準檢查清單

測試完成後，根據以下清單驗證結果：

| # | 驗收項 | 標準 | 驗證方法 | 狀態 |
|---|--------|------|---------|------|
| 1 | **TPS (Throughput)** | > 500 requests/s | 查看 `http_reqs.values.rate` | ⬜ |
| 2 | **P95 響應時間** | < 100ms | 查看 `http_req_duration.values.p(95)` | ⬜ |
| 3 | **錯誤率** | < 0.1% | 查看 `http_req_failed.values.rate` | ⬜ |
| 4 | **重複扣款** | 0 次 | 查看 `duplicateDebits.values.count` | ⬜ |
| 5 | **樂觀鎖衝突率** | < 1% | 查看 `optimisticLockFailures.values.count` / `http_reqs.values.count` | ⬜ |
| 6 | **餘額一致性** | 100% | 執行餘額驗證 SQL | ⬜ |

---

## 🔍 餘額一致性驗證

### 步驟 1: 計算預期總扣款金額

```bash
# 從 k6 JSON 結果提取成功扣款次數和金額
cat results/wallet-concurrency-test-result.json | jq '.metrics.successfulDebits.values.count'

# 計算預期總扣款金額（需要日誌中的金額記錄）
# 方法 1: 解析應用程序日誌
grep "Wallet debit successful" smart-admin-api-java21-springboot3/logs/app.log | \
  awk '{sum += $NF} END {print "Expected total debit:", sum}'

# 方法 2: 查詢數據庫交易記錄
psql -h localhost -U smartadmin_user -d smartadmin_igaming -c "
  SELECT
    COUNT(*) as total_transactions,
    SUM(amount) as total_debit_amount
  FROM t_wallet_transaction
  WHERE transaction_type = 3  -- BET
    AND reference_id LIKE 'bet-%'
    AND created_at > NOW() - INTERVAL '1 hour';
"
```

### 步驟 2: 驗證錢包最終餘額

```sql
-- 計算所有測試錢包的實際總扣款金額
WITH initial_balances AS (
  SELECT 1000 + generate_series(1, 100) AS wallet_id, 10000.0000 AS initial_balance
),
final_balances AS (
  SELECT wallet_id, balance FROM t_wallet WHERE wallet_id BETWEEN 1001 AND 1100
),
debit_summary AS (
  SELECT
    ib.wallet_id,
    ib.initial_balance,
    COALESCE(fb.balance, ib.initial_balance) AS final_balance,
    ib.initial_balance - COALESCE(fb.balance, ib.initial_balance) AS actual_debit
  FROM initial_balances ib
  LEFT JOIN final_balances fb ON ib.wallet_id = fb.wallet_id
)
SELECT
  COUNT(*) as wallet_count,
  SUM(initial_balance) as total_initial_balance,
  SUM(final_balance) as total_final_balance,
  SUM(actual_debit) as total_actual_debit
FROM debit_summary;
```

**預期結果**:
- `total_actual_debit` 應等於 k6 成功扣款金額總和
- 無任何錢包餘額為負數（`final_balance >= 0`）

### 步驟 3: 檢查重複交易

```sql
-- 檢查是否有重複的 requestId（關鍵驗證）
SELECT
  request_id,
  COUNT(*) as duplicate_count
FROM t_wallet_transaction
WHERE transaction_type = 3  -- BET
  AND created_at > NOW() - INTERVAL '1 hour'
GROUP BY request_id
HAVING COUNT(*) > 1;
```

**預期結果**: 0 rows（無重複交易）

---

## 📈 性能優化建議

### 場景 1: TPS < 500

**可能原因**:
- 數據庫查詢慢
- Redis 連接池不足
- 應用程序 CPU/Memory 瓶頸

**優化措施**:
1. **數據庫優化**:
   ```sql
   -- 檢查慢查詢
   SELECT query, mean_exec_time, calls
   FROM pg_stat_statements
   WHERE mean_exec_time > 100
   ORDER BY mean_exec_time DESC
   LIMIT 10;

   -- 添加索引（如果缺失）
   CREATE INDEX CONCURRENTLY idx_wallet_transaction_request_id
   ON t_wallet_transaction (request_id)
   WHERE NOT deleted;
   ```

2. **Redis 連接池調優**:
   ```yaml
   # application.yaml
   spring:
     redis:
       lettuce:
         pool:
           max-active: 100  # 增加最大連接數
           max-idle: 50
           min-idle: 10
   ```

3. **應用程序擴展**:
   - 增加應用程序實例（水平擴展）
   - 增加 JVM 堆內存（`-Xmx4g -Xms4g`）

---

### 場景 2: P95 響應時間 > 100ms

**可能原因**:
- 數據庫連接池耗盡
- Redisson 鎖等待時間過長
- 網絡延遲

**優化措施**:
1. **數據庫連接池調優**:
   ```yaml
   # application.yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 50  # 增加連接池大小
         minimum-idle: 10
         connection-timeout: 30000
   ```

2. **Redisson 鎖配置調優**:
   ```yaml
   # application.yaml
   igaming:
     lock:
       wait-ms: 1000   # 減少等待時間（從 5000ms 減至 1000ms）
       lease-ms: 3000  # 減少鎖持有時間（從 10000ms 減至 3000ms）
   ```

3. **啟用數據庫讀寫分離**:
   - 配置 PostgreSQL 主從複製
   - 讀操作路由到從庫

---

### 場景 3: 樂觀鎖衝突率 > 1%

**可能原因**:
- 熱點錢包（少數錢包被高頻訪問）
- 並發用戶過多訪問相同錢包

**優化措施**:
1. **增加測試錢包數量**:
   ```javascript
   // k6-wallet-concurrency-test.js
   // 從 100 個錢包增加到 1000 個錢包
   for (let i = 6; i <= 1000; i++) {
     TEST_WALLETS.push({ walletId: 1000 + i, ... });
   }
   ```

2. **調整 Redisson 鎖策略**:
   - 增加鎖等待時間（`wait-ms`）
   - 實現 Exponential Backoff 重試

---

## 🛠️ 故障排查

### 問題 1: k6 報錯 "connection refused"

**原因**: SmartAdmin 應用程序未啟動或端口錯誤。

**解決方案**:
```bash
# 檢查應用程序狀態
curl http://localhost:1024/actuator/health

# 檢查端口占用
netstat -ano | findstr :1024  # Windows
lsof -i :1024                 # macOS/Linux

# 重啟應用程序
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:bootRun
```

---

### 問題 2: 所有請求返回 401 Unauthorized

**原因**: Access Token 過期或無效。

**解決方案**:
```bash
# 重新獲取 Token
curl -X POST http://localhost:1024/api/system/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}' | jq -r '.data.accessToken'

# 更新環境變量
export ACCESS_TOKEN="<new-token>"

# 重新運行測試
k6 run --env ACCESS_TOKEN="$ACCESS_TOKEN" k6-wallet-concurrency-test.js
```

---

### 問題 3: 檢測到重複扣款（duplicateDebits > 0）

**原因**: 🔴 **CRITICAL** - Request ID 去重機制失效！

**緊急處理**:
1. **立即停止測試**
2. **檢查數據庫唯一索引**:
   ```sql
   SELECT indexname, indexdef
   FROM pg_indexes
   WHERE tablename = 't_wallet_transaction'
     AND indexname LIKE '%request_id%';

   -- 預期結果: uk_wallet_tx_request_id (UNIQUE)
   ```

3. **檢查代碼邏輯**:
   - 確認 `WalletManager.java` 中的 `try-catch (DuplicateKeyException)` 邏輯
   - 驗證 MyBatis `@TableField(value = "request_id")` 映射正確

4. **回滾測試數據**:
   ```sql
   DELETE FROM t_wallet_transaction
   WHERE reference_id LIKE 'bet-%'
     AND created_at > NOW() - INTERVAL '1 hour';

   UPDATE t_wallet
   SET balance = 10000.0000, locked_amount = 0.0000, version = 1
   WHERE wallet_id BETWEEN 1001 AND 1100;
   ```

---

## 📚 相關文檔

- **iGaming 實施計劃**: [C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md](C:\Users\ron.chang\.claude\plans\memoized-beaming-pretzel.md)
- **Wallet API 文檔**: [WalletController.java](../../../smart-admin-api-java21-springboot3/smartadmin-igaming/smartadmin-igaming-wallet/src/main/java/net/lab1024/sa/igaming/wallet/controller/WalletController.java)
- **三層防護機制文檔**: Phase 1 - 錢包系統三層防護
- **k6 官方文檔**: [https://k6.io/docs/](https://k6.io/docs/)

---

## 📞 技術支持

**問題反饋**: [GitHub Issues](https://github.com/lab1024/smart-admin/issues)
**文檔版本**: 1.0.0
**最後更新**: 2026-03-10

---

**文檔維護者**: SmartAdmin iGaming Team
**審閱者**: SmartAdmin QA Team
**批准者**: Performance Test Lead
