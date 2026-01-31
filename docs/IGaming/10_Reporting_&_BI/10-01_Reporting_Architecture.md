# 10-01 報表與 BI 架構 (Reporting & BI Architecture)

> **版本**: 1.0.0
> **最後更新**: 2026-01-28
> **維護團隊**: Data Team & BI Team

---

## 📋 目錄

- [1. 系統概述](#1-系統概述-system-overview)
- [2. 報表類型體系](#2-報表類型體系-report-classification)
- [3. BI 工具選型](#3-bi-工具選型-bi-tool-selection)
- [4. 數據分層架構](#4-數據分層架構-data-layering-architecture)
- [5. 報表調度系統](#5-報表調度系統-report-scheduling)
- [6. 報表導出機制](#6-報表導出機制-report-export-mechanism)
- [7. 性能優化策略](#7-性能優化策略-performance-optimization)
- [8. 相關文檔](#8-相關文檔)

---

## 1. 系統概述 (System Overview)

IGaming 平台的報表與 BI 系統負責為不同角色提供**數據洞察**與**決策支持**。系統支持以下核心功能：

- **多維度分析**：玩家行為、遊戲表現、財務指標、風控事件
- **即時性**：T+0 即時報表（5 分鐘延遲）、T+1 日報、T+7 週報、T+30 月報
- **可視化**：Grafana 儀表板、Metabase 自助分析、Excel/PDF 導出
- **合規性**：MGA/Curacao 監管報表、審計追蹤
- **可擴展性**：支持自定義報表、API 集成、數據導出

---

## 2. 報表類型體系 (Report Classification)

### 2.1 財務報表 (Financial Reports)

**2.1.1 GGR/NGR 報表 (Gross Gaming Revenue / Net Gaming Revenue)**

**目的**: 核心營收指標，監管合規必備

**計算公式**:
```text
GGR = Total Bets - Total Wins
NGR = GGR - Bonuses - Chargebacks - Refunds
```

**刷新頻率**: 每小時更新（T+0）

**數據表**: `dws.fact_ggr_ngr_hourly`

**關鍵指標**:
- GGR by Game Type (Slot, Live, Sports)
- NGR by Player Segment (VIP, Regular, New)
- GGR Margin (GGR / Total Bets)
- NGR Margin (NGR / GGR)

---

**2.1.2 出入金報表 (Deposit & Withdrawal Report)**

**目的**: 資金流轉監控、PSP 對賬

**關鍵指標**:
- Total Deposits (按 PSP、幣種、地區)
- Total Withdrawals (按審核狀態、風險等級)
- Net Deposit (Deposit - Withdrawal)
- PSP Success Rate (成功率、平均手續費)
- Deposit/Withdrawal Ratio (健康指標)

**刷新頻率**: 實時（5 分鐘延遲）

**數據表**: `dws.fact_deposit_withdrawal_daily`

---

**2.1.3 佣金報表 (Commission Report)**

**目的**: 代理佣金計算與發放


**刷新頻率**: 每日（T+1）

---

### 2.2 運營報表 (Operational Reports)

**2.2.1 玩家活躍度報表 (Player Activity Report)**

**關鍵指標**:
- DAU (Daily Active Users)
- MAU (Monthly Active Users)
- Stickiness (DAU / MAU)
- New Registrations
- Churn Rate (流失率)

**刷新頻率**: 每日（T+1）

**數據表**: `dws.dim_player_activity_daily`

---

**2.2.2 遊戲表現報表 (Game Performance Report)**

**關鍵指標**:
- Total Bets / Total Wins (按遊戲)
- Actual RTP (實際 RTP)
- Avg Bet Size
- Top Performing Games (GGR 排名)
- Game Popularity (活躍玩家數)

**刷新頻率**: 每小時（T+0）

**數據表**: `dws.fact_game_performance_hourly`

---

**2.2.3 活動效果報表 (Campaign Performance Report)**

**關鍵指標**:
- Bonus Issued / Bonus Redeemed
- Wagering Completion Rate (流水完成率)
- CPA (Cost Per Acquisition)
- ROI (Return on Investment)
- Bonus Abuse Detection (紅利濫用率)

**刷新頻率**: 每日（T+1）

**數據表**: `dws.fact_campaign_performance_daily`

---

### 2.3 風控報表 (Risk Control Reports)

**2.3.1 異常投注報表 (Suspicious Betting Report)**

**觸發條件**:
- 對沖檢測 (Hedging)
- 套利檢測 (Arbitrage)
- 異常贏率 (Win Rate > 55%)
- 流水異常 (短時間內完成大額流水)

**刷新頻率**: 實時（5 分鐘延遲）

**數據表**: `dws.fact_risk_events`

---

**2.3.2 多賬戶關聯報表 (Multi-Account Detection Report)**

**檢測維度**:
- 同一設備指紋 (Device Fingerprint)
- 同一 IP 地址
- 同一支付方式
- 投注行為相似度 (ML 模型)

**刷新頻率**: 每日（T+1）

**數據表**: `dws.dim_multi_account_detection`

---

**2.3.3 提款風控報表 (Withdrawal Risk Report)**

**關鍵指標**:
- High Risk Withdrawals (金額 > $10K)
- KYC Unverified Withdrawals
- First Deposit Bonus Abuse (首存後立即提款)
- Avg Approval Time (審核時長)
- Rejection Rate by Reason

**刷新頻率**: 實時（5 分鐘延遲）

**數據表**: `dws.fact_withdrawal_risk`

---

### 2.4 合規報表 (Compliance Reports)

**2.4.1 MGA 監管報表 (Malta Gaming Authority Compliance Report)**

**必須包含**:
- Total GGR (按遊戲類型)
- Total Bets / Total Wins
- Player Count (by Jurisdiction)
- RTP Verification (實際 RTP vs 聲明 RTP)
- Responsible Gaming Metrics (自我排除人數、存款限額設置)

**提交頻率**: 每月（T+30）

**數據表**: `ads.report_mga_compliance_monthly`

---

**2.4.2 AML/KYC 報表 (Anti-Money Laundering / Know Your Customer Report)**

**關鍵指標**:
- High-Value Transactions (單筆 > $10K)
- Suspicious Activity Reports (SAR)
- KYC Verification Rate
- EDD Required Cases (增強盡職調查)
- SOF Verification Rate (資金來源驗證)

**刷新頻率**: 每日（T+1）

**數據表**: `dws.fact_aml_kyc_daily`

---

## 3. BI 工具選型 (BI Tool Selection)

### 3.1 工具對比矩陣

| 工具 | 開源 | 易用性 | 性能 | 成本 | 推薦場景 |
|------|------|-------|------|------|---------|
| **Metabase** | ✅ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 免費 | 自助分析、輕量級 BI |
| **Apache Superset** | ✅ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 免費 | 開發者友好、SQL 自由度高 |
| **Tableau** | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | $$$$ | 企業級 BI、深度分析 |
| **Power BI** | ❌ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | $$$ | Microsoft 生態 |
| **Grafana** | ✅ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 免費 | 實時監控、時序數據 |

---

### 3.2 推薦架構

**分層使用策略**:

```text
┌─────────────────────────────────────────────────────┐
│  Grafana (實時監控)                                  │
│  - GGR/NGR 實時儀表板                                │
│  - 風控事件監控                                      │
│  - 系統性能監控                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  Metabase (自助分析)                                 │
│  - 業務團隊自助查詢                                  │
│  - 玩家分群分析                                      │
│  - 活動效果分析                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  Apache Superset (高級分析)                          │
│  - 數據科學家深度分析                                │
│  - 複雜 SQL 查詢                                     │
│  - 自定義可視化                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  ClickHouse / PostgreSQL (數據倉庫)                  │
│  - DWS (Data Warehouse Summary) 層                   │
│  - ADS (Application Data Service) 層                 │
└─────────────────────────────────────────────────────┘
```

---

## 4. 數據分層架構 (Data Layering Architecture)

### 4.1 四層架構

**ODS (Operational Data Store) → DWD (Data Warehouse Detail) → DWS (Data Warehouse Summary) → ADS (Application Data Service)**

```text
┌─────────────────────────────────────────────────────┐
│  ADS (Application Data Service)                      │
│  - 即用報表 (Pre-aggregated)                         │
│  - API 接口數據                                      │
│  - Grafana / Metabase 數據源                         │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  DWS (Data Warehouse Summary)                        │
│  - 日/週/月聚合表                                    │
│  - 維度彙總 (by Game, by Player Segment, by Date)   │
│  - 預計算指標 (GGR, NGR, LTV, ARPU)                 │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  DWD (Data Warehouse Detail)                         │
│  - 清洗後的事實表 (Fact Tables)                      │
│  - 標準化維度表 (Dimension Tables)                   │
│  - 數據質量校驗                                      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│  ODS (Operational Data Store)                        │
│  - PostgreSQL 主庫 (OLTP)                            │
│  - Kafka CDC 實時流                                  │
│  - MongoDB 審計日誌                                  │
└─────────────────────────────────────────────────────┘
```

---

### 4.2 數據流轉

**實時流 (Kafka CDC)**:
```text
PostgreSQL (ODS)
    → Debezium CDC
    → Kafka Topic (bet_events, transaction_events)
    → Kafka Streams / Flink
    → DWD (ClickHouse)
```

**批次 ETL (Apache Airflow)**:
```text
ODS (PostgreSQL)
    → (每小時 ETL)
    → DWD (ClickHouse)
    → (每日 ETL)
    → DWS (ClickHouse Materialized View)
    → (每日 ETL)
    → ADS (Redis Cache + ClickHouse)
```

---

## 5. 報表調度系統 (Report Scheduling)

### 5.1 Snail-Job 集成

**調度策略**:

```yaml
# GGR/NGR 每小時報表
job_name: generate_ggr_ngr_hourly
cron: 0 */1 * * * ?  # 每小時執行
handler: GgrNgrReportHandler
params:
  source_table: dwd.fact_bets
  target_table: dws.fact_ggr_ngr_hourly
  aggregation_level: hourly
retry: 3
timeout: 600  # 10 minutes
```

```yaml
# 玩家活躍度每日報表
job_name: generate_player_activity_daily
cron: 0 0 2 * * ?  # 每日 02:00 執行
handler: PlayerActivityReportHandler
params:
  source_table: dwd.fact_player_sessions
  target_table: dws.dim_player_activity_daily
  date: yesterday
retry: 3
timeout: 1800  # 30 minutes
```text

---

### 5.2 報表生成流程

```
1. 調度觸發 (Snail-Job Cron)
    ↓
2. 數據抽取 (Extract from DWD)
    ↓
3. 數據轉換 (Transform: Aggregation, Calculation)
    ↓
4. 數據加載 (Load to DWS/ADS)
    ↓
5. 緩存預熱 (Redis Cache Warm-up)
    ↓
6. 通知推送 (Slack/Email Notification)
```yaml

---

## 6. 報表導出機制 (Report Export Mechanism)

### 6.1 支持格式

**Excel (XLSX)**:
- 使用 Apache POI / EasyExcel
- 支持多 Sheet (多維度)
- 支持樣式 (標題、邊框、顏色)
- 最大行數: 1,048,576 行

**PDF**:
- 使用 iText / Flying Saucer
- 支持圖表嵌入 (Chart.js 生成圖片)
- 支持頁眉/頁尾、頁碼
- 適合打印歸檔

**CSV**:
- 流式寫入 (避免 OOM)
- 支持大數據導出 (億級)
- 適合 ETL 傳輸

---

### 6.2 異步導出架構

**大批量導出 (>10萬行)**:

```markdown
1. 用戶發起導出請求
    ↓
2. 創建導出任務 (export_tasks 表)
    ↓
3. Kafka 消息隊列 (export_task_queue)
    ↓
4. 後台 Worker 消費任務 (分頁查詢 ClickHouse)
    ↓
5. 流式寫入文件 (S3 / MinIO)
    ↓
6. 生成下載鏈接 (Presigned URL, 24 小時有效)
    ↓
7. 通知用戶 (郵件 / 站內信)
```


---

## 7. 性能優化策略 (Performance Optimization)

### 7.1 查詢優化


---

### 7.2 緩存策略

**Redis 緩存**:
```markdown
- 熱門報表 TTL: 5 分鐘
- 歷史報表 TTL: 1 小時
- 自定義查詢: 不緩存
```

**緩存鍵設計**:
```text
report:{report_type}:{date}:{filters_hash}
例: report:ggr_hourly:2026-01-28:abc123
```

---

### 7.3 併發控制

**限流策略**:
- 每用戶每分鐘: 10 次查詢
- 每租戶每分鐘: 100 次查詢
- 導出任務隊列: 最多 5 個並發

---

## 📚 相關文檔

### 前置知識
- [00-03 數據模型總覽](../00_Concept_&_Analysis/00-03_Data_Model_Overview.md) - 數據分層架構、表結構設計
- [00-04 技術選型標準](../00_Concept_&_Analysis/00-04_Technology_Stack.md) - ClickHouse、Kafka、Airflow 技術棧

### 核心依賴
- [02-04 流水計算與對賬](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - GGR/NGR 計算邏輯
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 風控報表數據來源

### 延伸閱讀
- [07-04 數據管道架構](../07_Platform_Management/07-04_Data_Pipeline_Architecture.md) - CDC、ETL 流程
- [12-06 性能監控](../12_Technical_Operations/12-06_Performance_Monitoring.md) - Grafana 儀表板設計

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Data Team & BI Team
