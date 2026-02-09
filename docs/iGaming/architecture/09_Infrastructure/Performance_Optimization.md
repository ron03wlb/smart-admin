# 性能優化規範 (Performance Optimization)

**Document Metadata**:
- Version: 1.0.0
- Created: 2026-02-09
- Status: Active
- Priority: P0 (Critical)
- Owner: Backend Team + DBA Team
- Source: [09-07 Performance Optimization](../../source-archive/09_Technical_Infrastructure/09-07_Performance_Optimization.md)

---

## 1. 核心瓶頸分析

iGaming 平台採用零信任統一錢包模型與實時風控，引入三大瓶頸：

| 瓶頸 | 根因 | 影響 |
|------|------|------|
| **「熱點行」並發鎖** | `FOR UPDATE` DB 鎖 | 限制單一玩家 TPS |
| **Redis Lua 腳本阻塞** | 複雜流水/風控邏輯 | 卡死 Redis |
| **鑑識級日誌膨脹** | 1 個動作 = 5-10 次 DB 寫入 | I/O 瓶頸 |

### 延遲預算

| 步驟 | 優化後 | 風險場景 |
|------|--------|---------|
| Token 驗證 | 2ms | 10ms |
| 風控檢查 | 10ms | 100ms |
| DB 事務 | 20ms | 200ms |
| **總計** | **~45ms** | **~340ms** |

> GP 超時閾值: 200-500ms

---

## 2. 下注請求優化

### 2.1 分布式鎖 + 樂觀鎖（取代 FOR UPDATE）

```
1. Redisson 分布式鎖 (wallet:lock:{tenant}:{player})
2. SELECT balance, version FROM player_wallet (無 FOR UPDATE)
3. UPDATE ... SET version = version + 1 WHERE version = ?
4. affected_rows == 0 -> 重試 (最多 3 次)
```

**鎖參數**:

| 參數 | 值 |
|------|-----|
| Wait Time | 3 秒 |
| Lease Time | 5 秒 |
| Watchdog | 啟用 |

**全局規範**: `FOR UPDATE` 禁止使用

### 2.2 player_wallet 分片

**分片鍵**: `hash(tenant_id, player_id) % N`

| 租戶規模 | 日活玩家 | 分片數 |
|---------|---------|-------|
| 小型 | < 10,000 | 8 |
| 中型 | 10,000 - 100,000 | 32 |
| 大型 | > 100,000 | 128 |

**路由**: MyBatis-Plus `DynamicTableNameInterceptor`

---

## 3. Token 安全方案

| 場景 | 驗證方式 | 有效期 | 特殊機制 |
|------|---------|--------|---------|
| **遊戲商 (GP)** | RSA-SHA256 | 單次 | IP 白名單 + 時間窗口 |
| **平台互調** | mTLS + SA Token | 1h | K8s Network Policy |
| **前台玩家** | HS512 JWT | 5min | Refresh Token 7d |
| **後台用戶** | RS256 + Session | 30min | MFA + 二次驗證 |

---

## 4. 實時風控優化

### 4.1 同步 vs 異步邊界

| 類型 | 檢查內容 | 延遲要求 |
|------|---------|---------|
| **同步** (下注路徑) | 玩家狀態、信用額度、全局限額 | < 15ms |
| **異步** (Kafka 事件) | 行為模式分析、鑑識日誌、閾值計算 | 非即時 |

### 4.2 Lua 腳本治理

| 規範 | 要求 |
|------|------|
| 執行時間 | < 1ms |
| Key 數量 | < 5 |
| 禁止操作 | KEYS *, SMEMBERS, HGETALL, 循環 > 100 |

---

## 5. 數據分層存儲

| 層級 | 時間範圍 | 存儲介質 | SLA | 成本/GB/月 |
|------|---------|---------|-----|-----------|
| **熱** | 0-2 個月 | PostgreSQL SSD | < 50ms | $0.115 |
| **溫** | 3-6 個月 | S3 Standard | < 2s | $0.023 |
| **冷** | 7-19 個月 | S3 Glacier | < 30s | $0.004 |
| **歸檔** | 20-84 個月 | S3 Deep Archive | < 12h | $0.00099 |

```
Day 0 -> Day 60 -> Day 180 -> Day 570 -> Day 2520
  |         |          |          |           |
PostgreSQL  S3 Std    S3 Glacier  S3 Deep    Delete
  (Hot)     (Warm)    (Cold)     Archive    /Permanent
```

**合規保留**: 7 年（總計 84 個月）

---

## 6. 性能指標

| 指標 | 優化前 | 優化後 | 提升 |
|------|--------|--------|------|
| TPS | 87 | >= 450 | +418% |
| P99 延遲 | 1,240ms | <= 200ms | -84% |
| 審計日誌延遲 | 350ms | <= 50ms | -87% |
| Cache Hit Rate | 65% | 95% | +30pp |
| Redis QPS | 10,000 | 1,000 | -90% |

---

## 相關文檔

- [Caching Strategy](./Caching_Strategy.md) - JetCache 多級緩存
- [Stream Processing Architecture](./Stream_Processing_Architecture.md) - Flink 流處理
- [Cost Optimization Architecture](./Cost_Optimization_Architecture.md) - 成本優化
