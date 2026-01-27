# 07-04 數據管道與 BI 架構 (Data Pipeline & BI Architecture)

## 1. 系統概述
為解決傳統 OLTP 資料庫在處理報表查詢時的效能瓶頸，本模組定義了 **OLAP (Online Analytical Processing)** 的數據流架構。
目標是支撐 **T+1 經營報表** 與 **準實時 (Near Real-time) 風控儀表板**。

## 2. 核心架構 (Data Flow)

```
[Source: MySQL/Mongo] 
    | (CDC - Debezium)
    v
[Stream: Kafka] 
    | (Flink / Kafka Connect)
    v
[Data Lake: S3 (Parquet)] -> [Spark Batch Job] -> [Data Warehouse: ClickHouse/Doris] -> [BI Tool: Superset/Tableau]
```

### 2.1 數據分層 (Layering)
1.  **ODS (Operational Data Store)**: 原始資料層，與業務庫保持一致 (MySQL Replica)。
2.  **DWD (Data Warehouse Detail)**: 明細層，進行清洗、脫敏 (Masking)、標準化。
3.  **DWS (Data Warehouse Summary)**: 匯總層，按日/按商戶聚合 (e.g. `daily_merchant_revenue`)。
4.  **ADS (Application Data Service)**: 應用層，直接供前端 API 查詢的結果集。

---

## 3. 報表類型與實現策略

### 3.1 實時儀表板 (Real-time Dashboard)
*   **場景**：在線人數、今日存款總額、風控警報。
*   **技術**：
    *   **Source**: Redis (Counter) 或 Flink Streaming SQL。
    *   **Latency**: < 5 秒。
    *   **限制**：僅提供簡易聚合，不支援複雜 Join。

### 3.2 T+1 經營報表 (Historical Report)
*   **場景**：月度損益表、代理業績結算、遊戲商對帳。
*   **技術**：
    *   **Source**: ClickHouse / StarRocks。
    *   **排程**：每日凌晨 02:00 執行 Spark/Airflow 任務，計算前一日數據。
    *   **優勢**：支援海量數據 (億級) 的秒級查詢。

---

## 4. 數據治理 (Data Governance)

### 4.1 脫敏規範 (Data Masking)
*   進入 Data Warehouse 前，所有 **PII (Personal Identifiable Information)** 必須脫敏。
*   規則：
    *   Name -> `R** C**`
    *   Phone -> `0912***789`
    *   ID -> `A12****89`

### 4.2 數據保留策略 (Retention)
*   **Hot Data (SSD)**: 最近 3 個月 (高頻查詢)。
*   **Warm Data (HDD)**: 3個月 - 1年。
*   **Cold Data (S3 Glacier)**: 1年以上 (僅供審計歸檔)。

## 6. 特殊業務處理 (Advanced Operations)

### 6.1 數據修正與冪等覆蓋 (Data Correction)
當上游產生 "歷史帳務作廢" (Void Transaction) 時：
1.  **策略**: **Idempotent Overwrite (冪等覆蓋)**。
2.  **執行**: Airflow 接收 `date` 參數，刪除該日期的 ClickHouse Partition，並重新從 ODS 執行 ETL。
3.  **Command**: `ALTER TABLE dws_revenue DROP PARTITION '2023-10-01';`

### 6.2 多商戶隔離策略 (OLAP Multi-Tenancy)
*   **模式**: **Logical Isolation (邏輯隔離)**。
*   **實作**: 所有商戶共用同一張大表，但在 Partition Key 中包含 `tenant_id`。
*   **優勢**: 避免維護數千個 DB 實例，且支援跨商戶的平台級報表 (如：全站總 GGR)。
*   **查詢**: 必須強制帶入 `WHERE tenant_id = ?`，否則拒絕執行。

---

## 5. 技術選型建議
*   **CDC**: Debezium (穩定、支援 MySQL Binlog)。
*   **Message Queue**: Kafka (高吞吐)。
*   **OLAP Engine**: 
    *   **ClickHouse**: 單表查詢極快，適合日誌分析。
    *   **StarRocks / Doris**: 支援 Join 較好，適合複雜報表。
*   **Scheduler**: Apache Airflow (DAG 管理)。
