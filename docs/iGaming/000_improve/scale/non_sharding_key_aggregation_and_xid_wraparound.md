# 非分片鍵維度聚合查詢加速 與 XID Wraparound 風險解決方案

**系統背景**：iGame 多商戶對帳系統 | PostgreSQL + Citus 128 分片 | `user_id` 分片鍵 | 日均 1.5 億筆交易 | 月分區策略

---

## 第一部分：非分片鍵維度聚合查詢加速方案

### 問題本質：為什麼按 merchant_id 聚合這麼慢？

在 Citus 架構中，我們選擇 `user_id` 作為分片鍵，這意味著同一個商戶的交易數據**被打散到所有 128 個分片中**。當執行 `GROUP BY merchant_id` 聚合時：

```
Coordinator 發起查詢
    │
    ├──→ Worker 1: 掃描本地分片，找出 merchant_001 的部分數據 → 局部 SUM
    ├──→ Worker 2: 掃描本地分片，找出 merchant_001 的部分數據 → 局部 SUM
    ├──→ ...
    └──→ Worker 128: 掃描本地分片，找出 merchant_001 的部分數據 → 局部 SUM
    │
    └──→ Coordinator: 合併 128 份局部結果 → 全局 SUM
```

**核心瓶頸分析**：

| 瓶頸層面 | 具體問題 | 量化影響 |
|---------|---------|---------|
| **網路 I/O** | 128 個 Worker 都要把局部結果傳回 Coordinator | 100+ 商戶 × 128 分片 = 12,800 次局部聚合結果傳輸 |
| **無法 Partition Pruning** | merchant_id 不是分片鍵，無法跳過任何分片 | 每次查詢必須掃描全部 128 個分片 |
| **超大商戶長尾效應** | 佔 30% 交易量的超大商戶數據分散在 128 個分片中 | Coordinator 合併階段成為瓶頸，單一商戶的聚合結果可能就有數千萬筆 |
| **Coordinator 記憶體壓力** | 所有局部結果要在 Coordinator 上做最終 Merge | 月度結算可能涉及 45 億筆交易的聚合 |

**Citus 在這種場景下的執行計畫**：

```sql
-- 這個查詢會觸發 "Multi-Shard" 執行路徑
SELECT merchant_id, 
       SUM(amount) as total_amount, 
       COUNT(*) as txn_count
FROM transaction_ledger
WHERE created_at BETWEEN '2026-01-01' AND '2026-02-01'
GROUP BY merchant_id;

-- Citus 實際執行：
-- 1. 發送子查詢到所有 128 個 Worker
-- 2. 每個 Worker 執行 Partial Aggregate（利用本地 Parallel Query）
-- 3. Coordinator 執行 Finalize Aggregate
```

---

### 方案一：Flink 側流式預聚合（推薦首選）

**核心思想**：將聚合計算從 PostgreSQL 移到 Flink 串流處理層，在數據寫入的同時完成聚合，而不是事後再去聚合海量原始數據。

```
交易數據流入
    │
    ├──→ Kafka Topic: transactions
    │         │
    │         ▼
    │    Flink Job: merchant_settlement_aggregator
    │         │
    │         ├── KeyBy(merchant_id)  ← 關鍵：按商戶分組
    │         ├── TumblingWindow(1 day)
    │         ├── AggregateFunction(SUM, COUNT, ...)
    │         │
    │         ▼
    │    Sink: merchant_daily_summary 表（PostgreSQL 本地表）
    │
    └──→ 原始數據照常寫入 Citus 分片表（user_id 分片）
```

**Flink 聚合作業核心邏輯**：

```java
// Flink DataStream API 實現商戶級預聚合
DataStream<Transaction> txnStream = env
    .addSource(new FlinkKafkaConsumer<>("transactions", schema, props));

txnStream
    // Step 1: 按 merchant_id 分組（Flink 內部 Hash 分區）
    .keyBy(Transaction::getMerchantId)
    
    // Step 2: 按天滾動窗口
    .window(TumblingEventTimeWindows.of(Time.days(1)))
    
    // Step 3: 聚合函式
    .aggregate(new MerchantDailyAggregator())
    
    // Step 4: 寫入結算彙總表
    .addSink(new JdbcSink<>(
        "INSERT INTO merchant_daily_summary " +
        "(merchant_id, settle_date, total_amount, txn_count, ...) " +
        "VALUES (?, ?, ?, ?) " +
        "ON CONFLICT (merchant_id, settle_date) DO UPDATE SET ...",
        statementBuilder, executionOptions, connectionOptions
    ));
```

**為什麼 Flink 側聚合是最優解？**

| 維度 | Flink 側聚合 | DB 側聚合 |
|------|-------------|----------|
| **數據掃描量** | 僅處理增量（流式） | 每次掃描全月 45 億筆 |
| **計算時機** | 實時/準實時 | 事後批量 |
| **DB 壓力** | 接近零（只寫彙總結果） | 極大（跨 128 分片聚合） |
| **結算查詢** | `SELECT * FROM merchant_daily_summary WHERE merchant_id = ?`，毫秒級 | 跨分片 GROUP BY，分鐘級 |
| **容錯** | Flink Checkpoint + Exactly-Once | 查詢失敗需重試 |

**彙總表設計**（PostgreSQL 本地表，不走 Citus 分片）：

```sql
-- 這是一張「本地表」，存在 Coordinator 上
-- 數據量極小：100 商戶 × 365 天 = 36,500 行/年
CREATE TABLE merchant_daily_summary (
    merchant_id     INTEGER NOT NULL,
    settle_date     DATE NOT NULL,
    total_bet_amount    NUMERIC(18,2) DEFAULT 0,
    total_win_amount    NUMERIC(18,2) DEFAULT 0,
    total_fee_amount    NUMERIC(18,2) DEFAULT 0,
    net_settlement      NUMERIC(18,2) DEFAULT 0,
    txn_count           BIGINT DEFAULT 0,
    unique_users        INTEGER DEFAULT 0,
    -- 對帳用欄位
    flink_checksum      VARCHAR(64),    -- Flink 側計算的校驗和
    db_verified         BOOLEAN DEFAULT FALSE,
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    
    PRIMARY KEY (merchant_id, settle_date)
);

-- 月度結算只需要簡單聚合這張小表
SELECT merchant_id, 
       SUM(total_bet_amount) as monthly_bet,
       SUM(net_settlement) as monthly_net
FROM merchant_daily_summary
WHERE settle_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY merchant_id;
-- 掃描 ~3,100 行，< 1ms
```

**關鍵陷阱與防護**：

1. **數據一致性校驗**：Flink 聚合結果可能因為 Late Event、Exactly-Once 語義問題與 DB 原始數據不一致。必須定期（如 T+1）用 DB 側聚合做校驗。
2. **Late Event 處理**：設置合理的 Watermark 延遲（如 5 分鐘），並使用 Allowed Lateness + Side Output 捕獲遲到事件，異步修正彙總表。
3. **Flink Job 故障恢復**：利用 Savepoint/Checkpoint 機制，確保重啟後不丟失也不重複聚合。

---

### 方案二：物化中間表（Materialized Intermediate Table）

**核心思想**：在對帳或結算窗口前，由排程任務將跨分片數據「拉」到 Coordinator 的本地物化表中。

```sql
-- Step 1: 在 Coordinator 上建立本地物化表
CREATE UNLOGGED TABLE merchant_monthly_agg AS
SELECT 
    merchant_id,
    DATE_TRUNC('month', created_at) as settle_month,
    txn_type,
    SUM(amount) as total_amount,
    COUNT(*) as txn_count,
    COUNT(DISTINCT user_id) as unique_users
FROM transaction_ledger
WHERE created_at BETWEEN '2026-01-01' AND '2026-02-01'
GROUP BY merchant_id, DATE_TRUNC('month', created_at), txn_type;

-- ⚠️ 這個 CREATE TABLE AS 會觸發跨 128 分片的並行聚合
-- 但只需要跑一次，結果存在本地

-- Step 2: 後續所有結算查詢都走本地表
SELECT * FROM merchant_monthly_agg 
WHERE merchant_id = 'M001';
-- 掃描本地表，毫秒級

-- Step 3: 結算完成後清理
DROP TABLE merchant_monthly_agg;
```

**為什麼用 UNLOGGED TABLE？**

UNLOGGED 表不寫 WAL，寫入速度比普通表快 2~5 倍。因為物化中間表是可重建的臨時數據，不需要崩潰恢復保護。但要注意：PostgreSQL 崩潰後 UNLOGGED 表會被自動清空。

**Citus 的 Parallel 執行加速**：

```sql
-- 確保每個 Worker 節點啟用 Parallel Query
-- Citus 會在每個 Worker 上利用 PostgreSQL 原生的並行查詢
ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
ALTER SYSTEM SET max_parallel_workers = 32;  -- 每個 Worker 節點

-- Worker 節點的執行計畫：
-- Gather Merge
--   └── Partial HashAggregate  (worker 1)
--   └── Partial HashAggregate  (worker 2)
--   └── Partial HashAggregate  (worker 3)
--   └── Partial HashAggregate  (worker 4)
--       └── Parallel Seq Scan on transaction_ledger_202601
--           Filter: created_at BETWEEN ... AND ...
```

**二級並行效應**：Citus 分片 × PostgreSQL 原生 Parallel = 雙層並行

- 第一層：Citus 將查詢分發到 128 個 Worker 分片（跨節點並行）
- 第二層：每個 Worker 內部啟用 4 個 Parallel Worker（節點內並行）
- 實際並行度：128 × 4 = 512 個並行執行單元

---

### 方案三：Citus Materialized View + 定時刷新

```sql
-- 在分布式表上建立物化視圖
-- ⚠️ Citus 對 Materialized View 的支援有限制
-- 替代方案：使用 Reference Table + 定時任務

-- 方案 A: 在每個 Worker 上建立局部物化視圖（需要 Citus Enterprise 或手動管理）
-- 方案 B: 使用 pg_cron + 定時聚合到本地表

-- pg_cron 排程（在 Coordinator 上）
SELECT cron.schedule('merchant_daily_agg', '0 3 * * *', $$
    INSERT INTO merchant_daily_summary 
    (merchant_id, settle_date, total_bet_amount, txn_count)
    SELECT merchant_id, 
           CURRENT_DATE - 1,
           SUM(CASE WHEN txn_type = 'BET' THEN amount ELSE 0 END),
           COUNT(*)
    FROM transaction_ledger
    WHERE created_at >= CURRENT_DATE - 1 
      AND created_at < CURRENT_DATE
    GROUP BY merchant_id
    ON CONFLICT (merchant_id, settle_date) DO UPDATE SET
        total_bet_amount = EXCLUDED.total_bet_amount,
        txn_count = EXCLUDED.txn_count;
$$);
```

---

### 方案四：CQRS 讀寫分離模型

**核心思想**：為商戶結算場景建立專用的讀取模型，與寫入模型完全解耦。

```
寫入路徑（OLTP）                    讀取路徑（OLAP）
    │                                    │
    ▼                                    ▼
Citus 分片表                      商戶結算讀取模型
(user_id 分片)                   (merchant_id 為主鍵)
    │                                    ▲
    │                                    │
    └──── CDC (Debezium) ──→ Kafka ──→ Flink ──→ 結算專用 DB
                                                  │
                                            可選方案：
                                            ├── PostgreSQL 單機（本地表）
                                            ├── ClickHouse / StarRocks
                                            └── Apache Doris
```

**CDC + Flink 架構的數據流**：

```
Citus Worker 1 ──→ Debezium Connector ──┐
Citus Worker 2 ──→ Debezium Connector ──┤
...                                     ├──→ Kafka Topic ──→ Flink ──→ 結算 DB
Citus Worker N ──→ Debezium Connector ──┘
```

**適用場景**：當商戶數量增長到數千家，報表維度從 `merchant_id` 擴展到 `merchant_id + game_type + currency + region` 等多維度時，CQRS 的收益最大。

---

### 方案五：Citus Reference Table + 本地 JOIN

**適用條件**：商戶維度表（merchant_config）體量小（< 200 條）。

```sql
-- 將商戶配置表設為 Reference Table（全量複製到每個 Worker）
SELECT create_reference_table('merchant_config');

-- 現在每個 Worker 上都有完整的 merchant_config
-- 但 transaction_ledger 的 GROUP BY merchant_id 依然需要跨分片
-- Reference Table 解決的是 JOIN 問題，不是聚合問題

-- 真正有用的場景：需要 JOIN 商戶配置做過濾
SELECT mc.merchant_name, 
       SUM(tl.amount) as total
FROM transaction_ledger tl
JOIN merchant_config mc ON tl.merchant_id = mc.id
WHERE mc.settlement_cycle = 'MONTHLY'
  AND tl.created_at BETWEEN '2026-01-01' AND '2026-02-01'
GROUP BY mc.merchant_name;
-- Reference Table 讓 JOIN 在每個 Worker 本地完成
-- 但 GROUP BY 的最終合併仍需 Coordinator
```

---

### 六種方案的權衡比較

| 方案 | 查詢延遲 | 數據新鮮度 | 實現複雜度 | DB 壓力 | 推薦場景 |
|------|---------|-----------|-----------|--------|---------|
| **Flink 流式預聚合** | < 1ms | 準實時（秒級） | 高 | 極低 | ✅ 首選：已有 Flink 基礎設施 |
| **物化中間表** | < 10ms | T+1 | 低 | 高（建表時） | 週/月結算，無 Flink 時 |
| **pg_cron 定時聚合** | < 1ms | T+1 | 低 | 中（凌晨跑） | 簡單場景，100 以下商戶 |
| **CQRS + CDC** | < 5ms | 近實時 | 極高 | 低 | 多維度報表，千家商戶 |
| **Reference Table** | 分鐘級 | 實時 | 低 | 高 | 需要 JOIN 維度表時 |
| **OLAP 引擎** | 秒級 | 近實時 | 高 | 零 | 自助式分析、Ad-hoc 查詢 |

**iGame 系統推薦組合策略**：

```
日常運營報表 ──→ Flink 流式預聚合 → merchant_daily_summary 本地表
月度商戶結算 ──→ Flink 聚合結果 + T+1 DB 側校驗（物化中間表做 Reconciliation）
Ad-hoc 分析  ──→ 未來考慮 OLAP 引擎（StarRocks/Doris），目前用物化中間表應急
```

---

## 第二部分：XID Wraparound 風險的系統性解決方案

### 問題量化：為什麼這是「生死問題」？

PostgreSQL 的 Transaction ID（XID）是 32 位無符號整數，採用**環形數字空間**：

```
XID 空間（環形）：0 ──→ 2^31 ──→ 2^32 ──→ 0（回繞）
                  │                │
                  └── 未來 ────── 過去
                  
可用 XID 數量 = 2^31 ≈ 21.47 億（一半視為「過去」，一半視為「未來」）
```

**iGame 系統的 XID 消耗速度**：

```
日均交易：1.5 億筆
每筆交易消耗 XID：至少 1 個（INSERT），對帳回寫再 +1（UPDATE）
實際日均 XID 消耗：~3 億（含子交易、SAVEPOINT、內部操作）

21.47 億 ÷ 3 億/天 ≈ 71.6 天

也就是說，如果不做任何 FREEZE，大約 2.5 個月就會耗盡 XID 空間。
```

**XID Wraparound 的災難場景**：

```
正常運行中...
    │
    ▼ 剩餘 XID < 1,000,000
    │
    ▼ PostgreSQL 進入 anti-wraparound VACUUM 模式
    │  ──→ autovacuum_freeze_max_age 觸發強制 VACUUM
    │  ──→ 這個 VACUUM 會嘗試掃描所有未凍結的 Tuple
    │  ──→ 在 50~100 GB/天的大表上，可能需要數小時
    │
    ▼ 如果 VACUUM 來不及完成...
    │
    ▼ 剩餘 XID < 1,000,000（硬性閾值）
    │
    ▼ ⚠️ PostgreSQL 拒絕分配新的 XID
    │  ──→ 所有寫入操作失敗
    │  ──→ ERROR: database is not accepting commands to avoid wraparound data loss
    │  ──→ 必須進入單用戶模式手動執行 VACUUM FREEZE
    │
    ▼ 服務完全中斷，需要人工介入
```

---

### 解決方案一：積極的分區 DETACH + FREEZE 策略（核心方案）

**核心思想**：利用時間分區的天然特性——歷史分區是只讀的，可以提前 DETACH 並 FREEZE，讓這些已凍結的分區不再消耗 XID 空間。

```
月分區生命週期與 FREEZE 策略：

[M+0] 活躍寫入分區    → 正常 autovacuum，不做 FREEZE
[M+1] 對帳回寫中      → 密集 autovacuum（scale_factor=0.01）
[M+2] 對帳完成，只讀  → 執行 VACUUM FREEZE
[M+3] DETACH 分區     → 獨立表，已完全凍結，XID 安全
[M+6] 歸檔到 S3       → 離線存儲
```

**Step-by-Step 實施流程**：

```sql
-- ============================================
-- Step 1: 確認分區已完成所有業務寫入
-- ============================================
-- 驗證沒有活躍交易正在寫入 2026-01 分區
SELECT count(*) 
FROM pg_stat_activity 
WHERE query LIKE '%transaction_ledger_202601%' 
  AND state = 'active';

-- ============================================
-- Step 2: 在分區上執行 VACUUM FREEZE
-- ============================================
-- ⚠️ 關鍵：先 FREEZE 再 DETACH
-- 因為 FREEZE 需要在分區仍為父表一部分時執行
-- 這樣 Citus 才能正確路由到對應 Worker
VACUUM (FREEZE, VERBOSE) transaction_ledger_202601;

-- 監控 FREEZE 進度
SELECT relname, 
       age(relfrozenxid) as xid_age,
       pg_size_pretty(pg_relation_size(oid)) as size
FROM pg_class 
WHERE relname LIKE 'transaction_ledger_202601%';

-- ============================================
-- Step 3: DETACH 分區（非阻塞方式）
-- ============================================
-- PostgreSQL 14+ 支持 CONCURRENTLY 選項
ALTER TABLE transaction_ledger 
    DETACH PARTITION transaction_ledger_202601 CONCURRENTLY;

-- ⚠️ CONCURRENTLY 的行為：
-- 1. 第一階段：取得 ACCESS SHARE 鎖，標記分區為 "detach pending"
-- 2. 等待所有正在使用此分區的事務完成
-- 3. 第二階段：完成 DETACH

-- ============================================
-- Step 4: 驗證凍結狀態
-- ============================================
SELECT relname, 
       age(relfrozenxid) as xid_age,
       relfrozenxid
FROM pg_class 
WHERE relname = 'transaction_ledger_202601';
-- xid_age 應該接近 0（剛被凍結）
```

**DETACH 策略的潛在陷阱與解決方案**：

| 陷阱 | 詳細描述 | 解決方案 |
|------|---------|---------|
| **正在執行的跨分區查詢** | DETACH 時若有查詢正在掃描包含此分區的父表，會導致查詢失敗或被阻塞 | 使用 `DETACH PARTITION ... CONCURRENTLY`（PG 14+），它會等待現有查詢完成 |
| **DETACH 後的查詢路由** | 應用層若仍有 `SELECT FROM transaction_ledger WHERE created_at < '2026-02-01'`，DETACH 後 202601 分區的數據不再被掃描 | 對帳/審計查詢需要感知分區狀態，使用 UNION ALL 顯式查詢已 DETACH 的分區 |
| **Citus 分片表的 DETACH 複雜性** | Citus 分片表的分區 DETACH 需要在 Coordinator 和所有 Worker 上同步執行 | 使用 Citus 提供的 DDL 傳播機制，確保 DDL 原子性 |
| **FREEZE 期間的 I/O 風暴** | 月分區 15~30 GB/分片 × 128 分片 = 1.9~3.8 TB 的 FREEZE 操作 | 分批在不同 Worker 上執行，錯開 I/O 高峰；使用 `vacuum_cost_delay` 限流 |
| **對帳查詢的完整性** | T+1 對帳可能跨越月邊界（如 1 月 31 日的對帳查詢 2 月 1 日才跑），DETACH 1 月分區前必須確保對帳已完成 | 建立明確的分區生命週期狀態機，對帳完成後才標記可 DETACH |

**審計查詢兼容方案**：

```sql
-- 建立視圖，自動包含已 DETACH 的分區
CREATE OR REPLACE VIEW transaction_ledger_all AS
    SELECT * FROM transaction_ledger           -- 活躍分區
    UNION ALL
    SELECT * FROM transaction_ledger_202601    -- 已 DETACH 的歷史分區
    UNION ALL
    SELECT * FROM transaction_ledger_202512;   -- 更早的歷史分區

-- 或者更好的方案：使用函式動態生成查詢
CREATE OR REPLACE FUNCTION query_historical_ledger(
    p_start_date TIMESTAMPTZ,
    p_end_date TIMESTAMPTZ
) RETURNS SETOF transaction_ledger AS $$
DECLARE
    v_partition_name TEXT;
BEGIN
    -- 動態查詢涵蓋時間範圍的所有分區（含已 DETACH 的）
    FOR v_partition_name IN
        SELECT tablename FROM pg_tables 
        WHERE tablename LIKE 'transaction_ledger_2026%'
    LOOP
        RETURN QUERY EXECUTE format(
            'SELECT * FROM %I WHERE created_at BETWEEN $1 AND $2',
            v_partition_name
        ) USING p_start_date, p_end_date;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

---

### 解決方案二：分層 VACUUM 策略（日常防線）

**核心原則**：不同分區、不同表採用不同的 VACUUM 策略，確保 FREEZE 進度始終領先於 XID 消耗速度。

```sql
-- ============================================
-- 第一層：活躍寫入分區（當月）
-- 目標：控制 dead tuples，不強制 FREEZE
-- ============================================
ALTER TABLE transaction_ledger_202603 SET (
    autovacuum_vacuum_scale_factor = 0.01,      -- 1% 行數變更即觸發
    autovacuum_vacuum_threshold = 100000,        -- 10 萬筆 dead tuples
    autovacuum_vacuum_cost_delay = 2,            -- 2ms（更激進）
    autovacuum_vacuum_cost_limit = 2000,         -- 高 I/O 預算
    autovacuum_freeze_min_age = 5000000,         -- 500 萬（提前 FREEZE）
    autovacuum_freeze_max_age = 100000000        -- 1 億（比預設 2 億更激進）
);

-- ============================================
-- 第二層：對帳回寫分區（上月）
-- 目標：對帳回寫產生大量 dead tuples，需要高頻 VACUUM
-- ============================================
ALTER TABLE transaction_ledger_202602 SET (
    autovacuum_vacuum_scale_factor = 0.005,      -- 0.5%
    autovacuum_vacuum_threshold = 50000,
    autovacuum_vacuum_cost_delay = 0,            -- 無限速！
    autovacuum_vacuum_cost_limit = 10000,        -- 全力 VACUUM
    autovacuum_freeze_min_age = 1000000,         -- 100 萬就開始 FREEZE
    autovacuum_freeze_max_age = 50000000         -- 5000 萬就強制
);

-- ============================================  
-- 第三層：只讀歷史分區（2 個月前）
-- 目標：徹底 FREEZE，準備 DETACH
-- ============================================
-- 手動執行（不依賴 autovacuum）
VACUUM (FREEZE, VERBOSE) transaction_ledger_202601;
```

---

### 解決方案三：避免不必要的 XID 消耗

**根本性減少 XID 使用量**，從源頭降低 Wraparound 壓力。

**3.1 消除對帳回寫（最高收益）**

```sql
-- ❌ 原方案：UPDATE 1.5 億筆記錄的 recon_status
UPDATE transaction_ledger 
SET recon_status = 1, recon_batch_id = 'BATCH_20260201'
WHERE created_at BETWEEN '2026-01-31' AND '2026-02-01'
  AND recon_status = 0;
-- 產生 1.5 億個 dead tuples + 消耗 1.5 億個 XID

-- ✅ 改進方案 A：獨立對帳狀態表
CREATE TABLE recon_status_map (
    txn_id      VARCHAR(64) PRIMARY KEY,
    recon_status SMALLINT DEFAULT 0,
    batch_id    VARCHAR(64),
    verified_at TIMESTAMPTZ
);
-- transaction_ledger 完全不需要 UPDATE
-- XID 消耗降低 50%

-- ✅ 改進方案 B：完全不做回寫
-- 對帳結果只記錄在 recon_batch + recon_discrepancy 中
-- 「已對帳」= 存在於某個 batch 中且不在 discrepancy 中
-- XID 消耗降低 50%，VACUUM 壓力降低 90%
```

**3.2 批量操作合併 XID**

```sql
-- ❌ 逐筆 INSERT（每筆消耗一個 XID）
FOR each_txn IN transactions LOOP
    INSERT INTO transaction_ledger VALUES (...);  -- 1.5 億個 XID
END LOOP;

-- ✅ 批量 INSERT（一個事務一個 XID）
INSERT INTO transaction_ledger (...)
VALUES (...), (...), (...), ...  -- 批量 1000 筆
-- 1.5 億 ÷ 1000 = 15 萬個 XID

-- ✅ COPY 命令（最高效）
COPY transaction_ledger FROM STDIN WITH (FORMAT binary);
-- 整個 COPY 操作只消耗 1 個 XID
```

**3.3 只讀事務不消耗 XID**

```sql
-- PostgreSQL 的只讀事務（SELECT）使用虛擬 XID（Virtual XID）
-- 虛擬 XID 不消耗真實的 32-bit XID 空間
BEGIN TRANSACTION READ ONLY;
SELECT * FROM transaction_ledger WHERE ...;
COMMIT;
-- 消耗 0 個真實 XID

-- 確保只讀 Replica 不消耗 XID
-- pg_stat_activity 中的 backend_xid 為 NULL 表示未分配真實 XID
SELECT pid, backend_xid, query 
FROM pg_stat_activity 
WHERE backend_xid IS NOT NULL;
```

---

### 解決方案四：監控體系（早期預警）

```sql
-- ============================================
-- 核心監控指標
-- ============================================

-- 1. 全局 XID 年齡（最重要的指標）
SELECT datname, 
       age(datfrozenxid) as db_xid_age,
       -- 距離強制 VACUUM 還有多少 XID
       (current_setting('autovacuum_freeze_max_age')::bigint 
        - age(datfrozenxid)) as until_force_vacuum,
       -- 距離災難性 Wraparound 還有多少 XID  
       (2147483647 - age(datfrozenxid)) as until_wraparound
FROM pg_database 
ORDER BY age(datfrozenxid) DESC;

-- 2. 各表的 XID 年齡（找出最危險的表）
SELECT schemaname, relname, 
       age(relfrozenxid) as xid_age,
       pg_size_pretty(pg_total_relation_size(relid)) as total_size,
       n_dead_tup,
       last_autovacuum,
       last_autoanalyze
FROM pg_stat_user_tables
ORDER BY age(relfrozenxid) DESC
LIMIT 20;

-- 3. 當前 VACUUM 進度（PostgreSQL 9.6+）
SELECT p.pid,
       p.datname,
       p.relid::regclass as table_name,
       p.phase,
       p.heap_blks_total,
       p.heap_blks_scanned,
       ROUND(100.0 * p.heap_blks_scanned / NULLIF(p.heap_blks_total, 0), 1) 
           as pct_complete,
       a.query_start,
       NOW() - a.query_start as duration
FROM pg_stat_progress_vacuum p
JOIN pg_stat_activity a ON p.pid = a.pid;

-- 4. XID 消耗速率（需要定期採樣計算）
-- 建議用 pg_cron 每小時記錄一次
CREATE TABLE xid_consumption_log (
    sampled_at  TIMESTAMPTZ DEFAULT NOW(),
    current_xid BIGINT,
    db_xid_age  BIGINT
);

SELECT cron.schedule('xid_monitor', '0 * * * *', $$
    INSERT INTO xid_consumption_log (current_xid, db_xid_age)
    SELECT txid_current(), 
           age(datfrozenxid) 
    FROM pg_database WHERE datname = current_database();
$$);

-- 計算每小時消耗速率
SELECT sampled_at,
       current_xid - LAG(current_xid) OVER (ORDER BY sampled_at) as xid_per_hour,
       db_xid_age
FROM xid_consumption_log
ORDER BY sampled_at DESC
LIMIT 24;
```

**告警閾值設計**：

```
╔══════════════════════════════════════════════════════════════════╗
║                    XID Wraparound 告警分級                       ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  🟢 NORMAL:  age(datfrozenxid) < 5 億                           ║
║              → 一切正常，autovacuum 在正常工作                     ║
║                                                                  ║
║  🟡 WARNING: age(datfrozenxid) > 5 億                           ║
║              → 檢查 autovacuum 是否被阻塞                        ║
║              → 檢查是否有長時間運行的事務阻止 VACUUM                ║
║              → 考慮手動觸發 VACUUM FREEZE                        ║
║                                                                  ║
║  🟠 CRITICAL: age(datfrozenxid) > 10 億                         ║
║              → 立即手動執行 VACUUM FREEZE                         ║
║              → 取消所有非必要的長事務                              ║
║              → 準備緊急運維窗口                                   ║
║                                                                  ║
║  🔴 EMERGENCY: age(datfrozenxid) > 15 億                        ║
║              → 距離 Wraparound 僅剩 ~6 億 XID（約 2 天）         ║
║              → 停止所有非關鍵寫入                                 ║
║              → 全力執行 VACUUM FREEZE                            ║
║              → 準備降級方案（唯讀模式）                            ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
```

---

### 解決方案五：長事務管控（VACUUM 的隱形殺手）

**為什麼長事務會阻止 FREEZE？**

PostgreSQL 的 VACUUM 只能凍結比所有活躍事務的最小快照 XID 更老的 Tuple。如果有一個長達數小時的事務持有古老的快照，VACUUM 就無法凍結該快照之後的任何 Tuple。

```sql
-- 找出阻止 VACUUM 的長事務
SELECT pid, 
       usename,
       state,
       backend_xmin,       -- 這個事務持有的最老快照
       age(backend_xmin) as snapshot_age,
       query_start,
       NOW() - query_start as duration,
       LEFT(query, 100) as query_preview
FROM pg_stat_activity
WHERE backend_xmin IS NOT NULL
ORDER BY age(backend_xmin) DESC
LIMIT 10;

-- ⚠️ 如果看到 snapshot_age > 1 億，這個事務正在阻止 FREEZE
-- 考慮終止它：
-- SELECT pg_terminate_backend(pid);
```

**防護措施**：

```sql
-- 設置事務超時（全局）
ALTER SYSTEM SET idle_in_transaction_session_timeout = '30min';

-- 設置語句超時
ALTER SYSTEM SET statement_timeout = '1h';

-- 對帳任務的特殊處理：
-- 對帳的 Flink JDBC Source 應該使用短事務 + 分段讀取
-- 而不是一個長事務掃描整天的數據
SET idle_in_transaction_session_timeout = '5min';
BEGIN;
SELECT * FROM transaction_ledger 
WHERE created_at BETWEEN '2026-01-31 00:00:00' AND '2026-01-31 01:00:00';
COMMIT;
-- 然後開新事務讀下一段
```

---

### 綜合方案：iGame 系統的 XID Wraparound 防禦體系

```
                    XID Wraparound 防禦架構
                    
第一道防線：源頭減少 XID 消耗
├── 消除對帳回寫（獨立狀態表）           → XID 消耗降低 50%
├── 批量 INSERT/COPY 替代逐筆寫入       → XID 消耗降低 99%
└── 確保只讀操作使用 READ ONLY 事務     → 避免無效 XID 分配

第二道防線：分層 VACUUM 策略
├── 活躍分區：激進 autovacuum（scale_factor=0.01）
├── 對帳分區：超激進（cost_delay=0，全力 VACUUM）
└── 只讀分區：手動 VACUUM FREEZE → DETACH

第三道防線：分區生命週期管理
├── [M+2] 對帳完成 → VACUUM FREEZE
├── [M+3] DETACH PARTITION CONCURRENTLY
└── [M+6] 歸檔到 S3 + DROP

第四道防線：長事務管控
├── idle_in_transaction_session_timeout = 30min
├── 監控 backend_xmin 年齡
└── 自動告警 + 自動終止異常事務

第五道防線：監控告警體系
├── 每小時採樣 XID 消耗速率
├── 四級告警閾值（5/10/15 億）
└── Grafana 面板即時展示
```

---

## 深度反思提問

**問題一**：我們在方案一中提出「先 FREEZE 再 DETACH」的策略。但考慮一個極端場景：如果 VACUUM FREEZE 在月分區（15~30 GB/分片 × 128 分片）上執行時間超過 24 小時，而此時新的交易持續消耗 XID，FREEZE 還沒跑完 XID 就快耗盡了——這時候你會怎麼做？是否可以考慮「先 DETACH 再分批 FREEZE」的反向策略？這樣做的風險是什麼？（提示：DETACH 後的獨立表仍然在同一個資料庫中，它的 `relfrozenxid` 仍然影響 `datfrozenxid`）

**問題二**：在非分片鍵聚合的方案中，我們推薦了 Flink 流式預聚合。但如果 Flink Job 因為故障重啟，從 Checkpoint 恢復後會重新處理一段時間窗口的數據。這時候 `merchant_daily_summary` 表中的聚合結果可能會被「重複加總」。你會如何設計 Flink 的聚合邏輯和 Sink 的冪等性，確保在故障恢復後聚合結果依然正確？（提示：想想 Flink 的 Exactly-Once 語義是「端到端」的嗎？還是只在 Flink 內部保證？JDBC Sink 如何參與 Two-Phase Commit？）
