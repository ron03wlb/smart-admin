# HikariCP Tuning Example

本文檔展示 SmartAdmin 項目中 HikariCP 連接池的完整調優過程和效果對比。

---

## 背景

**項目**: SmartAdmin Employee Management Module
**數據庫**: PostgreSQL 16.1 (Supabase 託管)
**問題**: 高峰期頻繁出現 "Connection timeout" 錯誤

---

## 調優前狀態

### 配置（application-prod.yml）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://supabase.example.com:5432/smartadmin
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
      pool-name: SmartAdminHikariPool
```

### 性能指標（高峰期：10:00-11:00）

| 指標 | 數值 | 狀態 |
|------|------|------|
| 平均活躍連接 | 9.2 / 10 | ❌ 危險 |
| 平均空閒連接 | 0.3 | ❌ 不足 |
| 利用率 | 92% | ❌ 過高 |
| 等待線程數 | 15-25 | ❌ 嚴重 |
| 超時錯誤 | 127 次/小時 | ❌ 嚴重 |
| P95 響應時間 | 3,200ms | ❌ 不可接受 |

### 問題分析（由 postgresql-best-practices skill 生成）

```
🔍 HikariCP Analysis Results:

問題 1: 連接池利用率過高（92%）
- 原因：maximum-pool-size 設置過低（10）
- 影響：高峰期連接不足，導致線程等待
- 證據：等待線程數高達 15-25

問題 2: 空閒連接不足（0.3）
- 原因：minimum-idle 設置過低（2）
- 影響：突發流量無法快速響應
- 證據：連接創建延遲 > 500ms

問題 3: 超時時間過短（30s）
- 原因：connection-timeout 30 秒不足以應對高峰期
- 影響：頻繁超時錯誤（127 次/小時）
- 建議：增加到 60 秒
```

---

## 調優方案

### 推薦配置（由 skill 生成）

```yaml
spring:
  datasource:
    url: jdbc:p6spy:postgresql://supabase.example.com:5432/smartadmin
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver  # 啟用 P6Spy 監控
    hikari:
      minimum-idle: 5              # 2 → 5 (保持更多空閒連接)
      maximum-pool-size: 20        # 10 → 20 (增加連接數)
      connection-timeout: 60000    # 30s → 60s (增加超時時間)
      idle-timeout: 600000         # 維持 10 分鐘
      max-lifetime: 1800000        # 維持 30 分鐘
      pool-name: SmartAdminHikariPool
      register-mbeans: true        # 啟用 JMX 監控

      # 額外優化
      connection-test-query: SELECT 1
      validation-timeout: 5000
      leak-detection-threshold: 60000  # 檢測連接洩漏
```

### 調整依據

#### 1. maximum-pool-size: 10 → 20

**計算公式**（HikariCP 官方推薦）:
```
connections = ((core_count × 2) + effective_spindle_count)
```

**SmartAdmin 生產環境**:
- CPU 核心數：4
- 硬碟類型：SSD (effective_spindle_count = 1)
- 計算：(4 × 2) + 1 = 9

**實際配置**:
- 理論值：9
- 考慮峰值：9 × 1.5 = 13.5 ≈ 15
- 保留冗餘：15 × 1.3 = 19.5 ≈ 20
- **最終配置：20**

#### 2. minimum-idle: 2 → 5

**依據**:
- 平均併發請求：30-50
- 平均查詢時間：200ms
- 所需連接：50 × 0.2s / 60s ≈ 0.16（理論最小值）
- 考慮突發：0.16 × 5 = 0.8 ≈ 1
- 實際設置：1 × 5 = 5（保留 5 倍冗餘）

#### 3. connection-timeout: 30s → 60s

**依據**:
- P95 連接獲取時間：2.3s（高峰期）
- P99 連接獲取時間：8.7s（高峰期）
- 安全閾值：P99 × 5 = 43.5s
- 推薦配置：60s（留 30% 冗餘）

---

## 調優實施

### 步驟 1: 驗證當前狀態

```bash
# 1. 運行性能分析
./gradlew runPostgresqlAnalysis

# 2. 查看報告
cat docs/performance/postgresql-performance-report.md

# 3. 確認問題
# ✅ HikariCP 利用率：92%（過高）
# ✅ 等待線程數：15-25（嚴重）
# ✅ 超時錯誤：127 次/小時
```

### 步驟 2: 更新配置

```bash
# 1. 備份現有配置
cp src/main/resources/application-prod.yml src/main/resources/application-prod.yml.backup

# 2. 更新配置（使用推薦值）
vi src/main/resources/application-prod.yml
# 修改 hikari 配置（如上）

# 3. 提交更改
git add src/main/resources/application-prod.yml
git commit -m "perf(hikaricp): tune connection pool settings

- maximum-pool-size: 10 → 20
- minimum-idle: 2 → 5
- connection-timeout: 30s → 60s

Expected improvement:
- Reduce timeout errors by 90%
- Reduce P95 response time by 60%

Refs: docs/performance/postgresql-performance-report.md"
```

### 步驟 3: 部署驗證

```bash
# 1. 部署到測試環境
kubectl apply -f k8s/smartadmin-deployment-test.yml

# 2. 壓力測試（使用 JMeter）
jmeter -n -t tests/load-test.jmx -l results/after-tuning.jtl

# 3. 監控 HikariCP 指標（Grafana）
# Dashboard: SmartAdmin > Database > HikariCP Metrics
```

---

## 調優後效果

### 性能指標對比（高峰期：10:00-11:00）

| 指標 | 調優前 | 調優後 | 改善 |
|------|--------|--------|------|
| 平均活躍連接 | 9.2 / 10 | 8.5 / 20 | ✅ 利用率降至 42.5% |
| 平均空閒連接 | 0.3 | 4.2 | ✅ 增加 1300% |
| 利用率 | 92% | 42.5% | ✅ 降低 53.8% |
| 等待線程數 | 15-25 | 0-2 | ✅ 減少 95% |
| 超時錯誤 | 127 次/小時 | 3 次/小時 | ✅ 減少 97.6% |
| P95 響應時間 | 3,200ms | 520ms | ✅ 改善 83.8% |
| P99 響應時間 | 8,500ms | 1,200ms | ✅ 改善 85.9% |

### 業務影響

**用戶體驗**:
- ✅ 員工列表頁面加載時間：5s → 0.8s（改善 84%）
- ✅ 部門查詢響應時間：2.3s → 0.4s（改善 82.6%）
- ✅ 高峰期無超時錯誤（從 127 次降至 3 次）

**系統穩定性**:
- ✅ 連接池利用率穩定在 40-50%（安全範圍）
- ✅ 無連接洩漏警告
- ✅ 數據庫 CPU 使用率：78% → 45%（改善 42.3%）

---

## Grafana 監控面板

### 調優前（利用率 92%）

```
HikariCP Pool Utilization (10:00-11:00)
┌─────────────────────────────────────┐
│ Active Connections (9.2 avg)       │
│ ████████████████████████████████▓▓  │ 92%
│                                     │
│ Idle Connections (0.3 avg)         │
│ ▓                                   │ 3%
│                                     │
│ Waiting Threads (20 avg)           │
│ ████████████████████                │
└─────────────────────────────────────┘
⚠️  連接池幾乎耗盡！
```

### 調優後（利用率 42.5%）

```
HikariCP Pool Utilization (10:00-11:00)
┌─────────────────────────────────────┐
│ Active Connections (8.5 avg)       │
│ █████████████████                   │ 42.5%
│                                     │
│ Idle Connections (4.2 avg)         │
│ ████████                            │ 21%
│                                     │
│ Waiting Threads (0.5 avg)          │
│ ▓                                   │
└─────────────────────────────────────┘
✅ 連接池健康！
```

---

## JMeter 壓力測試結果

### 測試場景

- **併發用戶**: 100
- **Ramp-up 時間**: 10 秒
- **測試持續時間**: 5 分鐘
- **測試接口**: GET /api/employee/list

### 結果對比

| 指標 | 調優前 | 調優後 | 改善 |
|------|--------|--------|------|
| 平均響應時間 | 1,850ms | 320ms | ✅ 82.7% |
| P95 響應時間 | 3,200ms | 520ms | ✅ 83.8% |
| P99 響應時間 | 8,500ms | 1,200ms | ✅ 85.9% |
| 錯誤率 | 12.3% | 0.3% | ✅ 97.6% |
| 吞吐量 | 45 req/s | 180 req/s | ✅ 300% |

### JMeter 報告截圖

```
Aggregate Report (Before Tuning):
Label               Samples  Average  Min  Max    Std.Dev  Error%  Throughput
/api/employee/list  5000     1850     120  12500  2340     12.3%   45.2/sec

Aggregate Report (After Tuning):
Label               Samples  Average  Min  Max    Std.Dev  Error%  Throughput
/api/employee/list  5000     320      80   1800   185      0.3%    180.5/sec
```

---

## 成本分析

### 資源消耗變化

| 資源 | 調優前 | 調優後 | 變化 |
|------|--------|--------|------|
| 數據庫連接數 | 10 | 20 | ⬆️ +10 |
| 內存使用 | 450MB | 520MB | ⬆️ +70MB (+15.6%) |
| 數據庫 CPU | 78% | 45% | ⬇️ -42.3% |
| 應用 CPU | 65% | 58% | ⬇️ -10.8% |

### 成本效益分析

**額外成本**:
- 數據庫連接數 +10: 忽略不計（PostgreSQL 支持 > 100 連接）
- 內存 +70MB: $0（在現有資源範圍內）

**收益**:
- 減少超時錯誤 97.6%: 減少客戶投訴和支持成本
- 提升吞吐量 300%: 可支持 3 倍用戶增長，無需擴容
- 降低數據庫 CPU 42%: 延長數據庫擴容時間 6-12 個月

**ROI**: 無限（零額外成本，顯著收益）

---

## 經驗總結

### ✅ 最佳實踐

1. **使用數據驅動調優**
   - 依賴 postgresql-best-practices skill 的分析報告
   - 不要盲目增加連接數

2. **遵循 HikariCP 公式**
   - `connections = ((core_count × 2) + effective_spindle_count)`
   - 為峰值流量保留 30-50% 冗餘

3. **啟用監控**
   - `register-mbeans: true`（JMX 監控）
   - `leak-detection-threshold`（連接洩漏檢測）
   - P6Spy（SQL 監控）

4. **漸進式調優**
   - 先在測試環境驗證
   - 灰度發布到生產環境
   - 監控 7 天確認穩定性

### ❌ 常見錯誤

1. **過度配置**
   - ❌ 不要設置 `maximum-pool-size: 100`（浪費資源）
   - ✅ 根據公式計算，保留適當冗餘

2. **忽略 minimum-idle**
   - ❌ 不要使用默認值（0）
   - ✅ 設置為 `maximum-pool-size` 的 25-50%

3. **超時時間過長**
   - ❌ 不要設置 `connection-timeout: 300000`（5 分鐘）
   - ✅ 60 秒已足夠，過長會延遲錯誤發現

---

## 參考資料

- [HikariCP About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [PostgreSQL Connection Management](https://www.postgresql.org/docs/current/runtime-config-connection.html)
- [SmartAdmin Performance Guide](../references/postgres-best-practices.md)

---

**作者**: SmartAdmin Skills Team
**最後更新**: 2026-01-29
**適用版本**: SmartAdmin v3.0.0+, HikariCP 5.0.1+, PostgreSQL 16+
