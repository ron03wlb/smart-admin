# 10-01 報表與商業智能架構 (Reporting & BI Architecture)

## 1. 系統概述

BI 系統為運營、財務、風控團隊提供數據洞察，支持數據驅動決策。本系統整合平台所有業務數據，提供從原始數據到業務報表的完整數據鏈路。

**核心目標**：
- **數據民主化**: 讓非技術人員也能自助查詢數據
- **即時監控**: 關鍵業務指標實時更新
- **歷史追溯**: 支持長期趨勢分析和合規審計
- **多租戶隔離**: 確保各租戶只能查看自己的數據

---

## 2. 數據分層架構 (Data Layering)

### 2.1 四層架構設計

```
┌─────────────────────────────────────────────────────────────┐
│              ADS (Application Data Service)                 │
│          報表就緒層 - 業務特定視圖 (Tableau/Metabase)        │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│           DWS (Data Warehouse Service)                      │
│      數據服務層 - 預聚合指標 (每日/每小時玩家統計)            │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│          DWD (Data Warehouse Detail)                        │
│      數據明細層 - 清洗轉換後的事實表 (DBT 轉換)              │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│          ODS (Operational Data Store)                       │
│    操作數據存儲 - 生產庫原始數據鏡像 (CDC via Debezium)       │
└─────────────────────────────────────────────────────────────┘
```

---

### 2.2 ODS 層 (Operational Data Store)

**數據來源**: 通過 CDC (Change Data Capture) 實時同步生產數據庫

**技術方案**:
- **CDC 工具**: Debezium + Kafka
- **目標存儲**: PostgreSQL (OLAP) / ClickHouse (高性能分析)
- **同步頻率**: 準實時（延遲 < 5秒）
- **保留期限**: 30 天

**表結構範例**:
```sql
-- ODS 層完全鏡像生產表結構
CREATE TABLE ods.transactions (
    transaction_id BIGINT,
    player_id BIGINT,
    type VARCHAR(50),
    amount DECIMAL(18, 4),
    currency CHAR(3),
    status VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    -- CDC metadata
    _kafka_offset BIGINT,
    _synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 2.3 DWD 層 (Data Warehouse Detail)

**數據處理**: 使用 DBT (Data Build Tool) 進行清洗和轉換

**轉換邏輯**:
1. **數據清洗**: 去除測試數據、修正異常值
2. **類型轉換**: 統一時區、幣種標準化
3. **去敏化**: 敏感字段脫敏（如銀行卡號）
4. **關聯補充**: Join 維度表，補充玩家 VIP 等級等屬性

**DBT 模型範例**:
```sql
-- models/dwd/dwd_transactions.sql
{{ config(materialized='incremental') }}

WITH source AS (
    SELECT * FROM {{ source('ods', 'transactions') }}
    {% if is_incremental() %}
    WHERE created_at > (SELECT MAX(created_at) FROM {{ this }})
    {% endif %}
),

cleaned AS (
    SELECT
        transaction_id,
        player_id,
        type,
        -- 統一轉換為 USD
        CASE
            WHEN currency = 'CNY' THEN amount / 7.0
            WHEN currency = 'EUR' THEN amount * 1.1
            ELSE amount
        END AS amount_usd,
        status,
        CONVERT_TIMEZONE('UTC', created_at) AS created_at
    FROM source
    WHERE status != 'test'  -- 排除測試數據
)

SELECT * FROM cleaned;
```

**更新頻率**: 每小時執行一次（Airflow 調度）

---

### 2.4 DWS 層 (Data Warehouse Service)

**數據聚合**: 預計算常用指標，加速報表查詢

**物化視圖範例**:
```sql
-- DWS: 每日玩家統計
CREATE MATERIALIZED VIEW dws.daily_player_stats AS
SELECT
    DATE_TRUNC('day', created_at) AS stat_date,
    player_id,

    -- 存款指標
    COUNT(CASE WHEN type = 'deposit' THEN 1 END) AS deposit_count,
    SUM(CASE WHEN type = 'deposit' THEN amount_usd ELSE 0 END) AS total_deposit,

    -- 提款指標
    COUNT(CASE WHEN type = 'withdrawal' THEN 1 END) AS withdrawal_count,
    SUM(CASE WHEN type = 'withdrawal' THEN amount_usd ELSE 0 END) AS total_withdrawal,

    -- 投注指標
    COUNT(CASE WHEN type = 'bet' THEN 1 END) AS bet_count,
    SUM(CASE WHEN type = 'bet' THEN amount_usd ELSE 0 END) AS total_bet,
    SUM(CASE WHEN type = 'win' THEN amount_usd ELSE 0 END) AS total_win,

    -- 淨充值
    SUM(CASE WHEN type = 'deposit' THEN amount_usd ELSE 0 END) -
    SUM(CASE WHEN type = 'withdrawal' THEN amount_usd ELSE 0 END) AS net_deposit

FROM dwd.transactions
WHERE status = 'success'
GROUP BY DATE_TRUNC('day', created_at), player_id;

-- 定時刷新（每日 03:00 AM）
REFRESH MATERIALIZED VIEW dws.daily_player_stats;
```

---

### 2.5 ADS 層 (Application Data Service)

**業務視圖**: 為特定報表需求設計的視圖

**財務報表視圖範例**:
```sql
-- ADS: 營運商每日損益表視圖
CREATE VIEW ads.operator_daily_pnl AS
SELECT
    stat_date,
    tenant_id,
    tenant_name,

    -- 收入側
    SUM(total_deposit) AS total_deposits,
    SUM(total_bet) - SUM(total_win) AS ggr,  -- Gross Gaming Revenue

    -- 成本側
    SUM(bonus_cost) AS bonus_expenses,
    SUM(psp_fee) AS payment_fees,
    SUM(tax_amount) AS tax_expenses,

    -- 淨收入
    (SUM(total_bet) - SUM(total_win)) -
    SUM(bonus_cost) - SUM(psp_fee) - SUM(tax_amount) AS ngr  -- Net Gaming Revenue

FROM dws.daily_player_stats dps
JOIN dim_players p ON dps.player_id = p.player_id
JOIN dim_tenants t ON p.tenant_id = t.tenant_id
GROUP BY stat_date, tenant_id, tenant_name;
```

---

## 3. 核心報表 (Core Reports)

### 3.1 財務報表

#### 3.1.1 營運商損益表 (Operator P&L)

**報表字段**:
| 指標 | 公式 | 說明 |
|------|------|------|
| 總存款 | SUM(deposits) | 玩家充值總額 |
| 總提款 | SUM(withdrawals) | 玩家提現總額 |
| **GGR** | SUM(bets) - SUM(wins) | 毛利潤 (Gross Gaming Revenue) |
| 獎金成本 | SUM(bonus_issued) | 平台發放的紅利 |
| PSP 手續費 | SUM(transaction_fee) | 支付通道費用 |
| 遊戲提供商抽成 | SUM(gp_commission) | GP 分潤費用 |
| 稅費 | GGR × tax_rate | 博彩稅 (各國稅率不同) |
| **NGR** | GGR - 獎金 - 手續費 - GP抽成 - 稅費 | 淨利潤 (Net Gaming Revenue) |

**SQL 查詢範例**:
```sql
SELECT
    DATE(stat_date) AS 日期,
    tenant_name AS 租戶,
    ROUND(SUM(total_deposits), 2) AS 總存款,
    ROUND(SUM(total_withdrawals), 2) AS 總提款,
    ROUND(SUM(ggr), 2) AS GGR,
    ROUND(SUM(bonus_cost), 2) AS 獎金成本,
    ROUND(SUM(psp_fee), 2) AS 手續費,
    ROUND(SUM(tax_amount), 2) AS 稅費,
    ROUND(SUM(ngr), 2) AS NGR
FROM ads.operator_daily_pnl
WHERE stat_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(stat_date), tenant_name
ORDER BY DATE(stat_date) DESC;
```

---

#### 3.1.2 支付渠道分析報表

**維度分析**:
- 按 PSP 分組：Nuvei、Adyen、Stripe
- 按支付方式：銀行卡、電子錢包、加密貨幣
- 按幣種：USD、CNY、EUR、USDT

**關鍵指標**:
```sql
SELECT
    psp_code AS 支付商,
    payment_method AS 支付方式,
    COUNT(*) AS 交易筆數,
    COUNT(CASE WHEN status = 'success' THEN 1 END) AS 成功筆數,
    ROUND(
        COUNT(CASE WHEN status = 'success' THEN 1 END)::DECIMAL / COUNT(*) * 100,
        2
    ) AS 成功率,
    SUM(fee_amount) AS 手續費總額,
    ROUND(SUM(fee_amount) / NULLIF(SUM(amount), 0) * 100, 2) AS 平均費率
FROM dwd.payment_transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY psp_code, payment_method
ORDER BY 交易筆數 DESC;
```

---

### 3.2 玩家分析報表

#### 3.2.1 玩家分群 (Player Segmentation)

**RFM 模型** (Recency, Frequency, Monetary):
```sql
WITH player_rfm AS (
    SELECT
        player_id,
        MAX(stat_date) AS last_activity_date,
        CURRENT_DATE - MAX(stat_date) AS recency_days,  -- R: 最近活躍天數
        COUNT(DISTINCT stat_date) AS frequency,         -- F: 活躍天數
        SUM(total_deposit) AS monetary                  -- M: 總存款金額
    FROM dws.daily_player_stats
    WHERE stat_date >= CURRENT_DATE - INTERVAL '90 days'
    GROUP BY player_id
),

player_segments AS (
    SELECT
        player_id,
        CASE
            WHEN monetary > 10000 THEN 'Whale'          -- 鯨魚玩家
            WHEN monetary > 5000 THEN 'High Roller'     -- 高額玩家
            WHEN monetary > 1000 THEN 'Regular'         -- 普通玩家
            WHEN monetary > 100 THEN 'Casual'           -- 休閒玩家
            ELSE 'Minnow'                               -- 小額玩家
        END AS monetary_segment,

        CASE
            WHEN recency_days <= 7 THEN 'Active'        -- 活躍
            WHEN recency_days <= 30 THEN 'At Risk'      -- 風險流失
            ELSE 'Churned'                              -- 已流失
        END AS activity_segment
    FROM player_rfm
)

SELECT
    monetary_segment AS 玩家等級,
    activity_segment AS 活躍狀態,
    COUNT(*) AS 玩家數量,
    ROUND(COUNT(*)::DECIMAL / SUM(COUNT(*)) OVER () * 100, 2) AS 佔比
FROM player_segments
GROUP BY monetary_segment, activity_segment
ORDER BY 玩家數量 DESC;
```

---

#### 3.2.2 玩家生命週期價值 (LTV - Lifetime Value)

**LTV 計算公式**:
```
LTV = (平均每月充值 × 平均留存月數) - 獲客成本
```

**SQL 實作**:
```sql
WITH player_monthly_revenue AS (
    SELECT
        player_id,
        DATE_TRUNC('month', stat_date) AS month,
        SUM(total_deposit) AS monthly_deposit
    FROM dws.daily_player_stats
    GROUP BY player_id, DATE_TRUNC('month', stat_date)
),

player_ltv AS (
    SELECT
        player_id,
        AVG(monthly_deposit) AS avg_monthly_deposit,
        COUNT(DISTINCT month) AS retention_months,
        AVG(monthly_deposit) * COUNT(DISTINCT month) AS ltv
    FROM player_monthly_revenue
    GROUP BY player_id
)

SELECT
    registration_channel AS 註冊渠道,
    COUNT(*) AS 玩家數,
    ROUND(AVG(ltv), 2) AS 平均LTV,
    ROUND(MAX(ltv), 2) AS 最高LTV,
    ROUND(AVG(retention_months), 1) AS 平均留存月數
FROM player_ltv
JOIN dim_players p ON player_ltv.player_id = p.player_id
GROUP BY registration_channel
ORDER BY 平均LTV DESC;
```

---

### 3.3 遊戲效能報表

#### 3.3.1 遊戲排行榜

**核心指標**:
```sql
SELECT
    game_id,
    game_name,
    game_provider,

    -- 投注指標
    COUNT(DISTINCT player_id) AS 活躍玩家數,
    COUNT(*) AS 投注筆數,
    SUM(bet_amount_usd) AS 總投注額,

    -- 收益指標
    SUM(bet_amount_usd) - SUM(win_amount_usd) AS GGR,
    ROUND(
        SUM(win_amount_usd) / NULLIF(SUM(bet_amount_usd), 0) * 100,
        2
    ) AS 實際RTP,

    -- 理論RTP對比
    theoretical_rtp,
    ABS(實際RTP - theoretical_rtp) AS RTP偏差

FROM dwd.game_rounds
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
  AND status = 'settled'
GROUP BY game_id, game_name, game_provider, theoretical_rtp
ORDER BY GGR DESC
LIMIT 20;
```

---

#### 3.3.2 RTP 監控警報

**目的**: 檢測遊戲RTP異常，防止GP作弊

```sql
-- RTP 偏差超過 ±2% 的遊戲警報
SELECT
    game_id,
    game_name,
    SUM(bet_amount_usd) AS total_bets,
    SUM(win_amount_usd) AS total_wins,
    ROUND(SUM(win_amount_usd) / NULLIF(SUM(bet_amount_usd), 0) * 100, 2) AS actual_rtp,
    theoretical_rtp,
    ABS(actual_rtp - theoretical_rtp) AS rtp_deviation
FROM dwd.game_rounds
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY game_id, game_name, theoretical_rtp
HAVING ABS(actual_rtp - theoretical_rtp) > 2.0  -- 偏差 > 2%
ORDER BY rtp_deviation DESC;
```

**警報處理**:
- 偏差 > 2%: 發送 Slack 通知給運營團隊
- 偏差 > 5%: 自動暫停該遊戲，啟動調查流程

---

## 4. BI 工具整合

### 4.1 Metabase (自助式 BI)

**目標用戶**: 運營團隊、產品經理

**優勢**:
- 無需 SQL 知識，拖拽式查詢
- 內建儀表板分享功能
- 開源免費

**連接配置**:
```yaml
# metabase-config.yml
database:
  type: postgres
  host: dw.postgres.internal
  port: 5432
  dbname: data_warehouse
  user: metabase_ro  # 只讀賬戶
  password: ${METABASE_DB_PASSWORD}

permissions:
  - role: operator_user
    schemas: [ads, dws]  # 只能訪問聚合層
  - role: finance_user
    schemas: [ads, dws, dwd]  # 可訪問明細層
```

---

### 4.2 Tableau (高級分析)

**目標用戶**: C-level 高管、數據分析師

**使用場景**:
- 複雜趨勢分析（時間序列預測）
- 地理位置分析（玩家分佈地圖）
- 交叉維度對比（VIP等級 × 遊戲類型 × 充值金額）

**Dashboard 範例**:
```
Executive Dashboard:
┌─────────────────────────────────────────┐
│  今日營收: $125,450  ▲ 15% vs 昨日       │
│  活躍玩家: 3,250     ▼ 3% vs 昨日        │
│  新註冊: 125         ▲ 8% vs 昨日        │
└─────────────────────────────────────────┘

[營收趨勢圖 - 最近30天折線圖]
[玩家分佈地圖 - 按國家熱力圖]
[Top 10 遊戲 - 橫向條形圖]
```

---

### 4.3 Redash (開發者友好)

**目標用戶**: 工程師、技術支持

**使用場景**:
- Ad-hoc SQL 查詢
- API 查詢結果（Redash 提供 JSON API）
- 系統健康監控（資料庫性能、CDC 延遲）

**警報配置範例**:
```sql
-- 查詢：CDC 延遲監控
SELECT
    table_name,
    MAX(_synced_at) AS last_sync_time,
    EXTRACT(EPOCH FROM (NOW() - MAX(_synced_at))) AS lag_seconds
FROM ods.transactions
GROUP BY table_name
HAVING EXTRACT(EPOCH FROM (NOW() - MAX(_synced_at))) > 300;  -- 延遲 > 5分鐘

-- 警報規則：若查詢返回任何行，發送 PagerDuty 警報
```

---

## 5. 即時儀表板 (Real-Time Dashboards)

### 5.1 營運即時儀表板

**數據源**: Redis + WebSocket (用於即時推送)

**關鍵指標** (每30秒刷新):
```
┌─────────────────────────────────────────────────────┐
│  當前在線玩家: 1,250                                 │
│  即時投注金額: $8,450 (最近1分鐘)                     │
│  存款成功率: 96.5% (最近1小時)                       │
│  提款隊列長度: 12 筆待審核                           │
└─────────────────────────────────────────────────────┘

[Top 10 熱門遊戲 - 即時更新條形圖]
[存款提款趨勢 - 最近24小時折線圖]
```

**技術實作**:
```python
# Realtime Dashboard Backend (FastAPI + Redis)
from fastapi import FastAPI, WebSocket
import redis
import json

app = FastAPI()
redis_client = redis.Redis(host='localhost', port=6379)

@app.websocket("/ws/dashboard")
async def dashboard_websocket(websocket: WebSocket):
    await websocket.accept()

    while True:
        # 從 Redis 讀取即時指標
        metrics = {
            "online_players": int(redis_client.get("online_players") or 0),
            "recent_bets_amount": float(redis_client.get("recent_bets_1m") or 0),
            "deposit_success_rate": float(redis_client.get("deposit_success_rate_1h") or 0),
            "pending_withdrawals": int(redis_client.get("pending_withdrawals") or 0)
        }

        await websocket.send_text(json.dumps(metrics))
        await asyncio.sleep(30)  # 每30秒推送一次
```

---

### 5.2 風控即時儀表板

**高風險玩家監控**:
```
┌─────────────────────────────────────────────────────┐
│  當前高風險玩家: 8 人                                │
│  24小時內新增風險標記: 3 人                          │
│  異常投注模式檢測: 2 起                              │
└─────────────────────────────────────────────────────┘

[風險分數分佈 - 直方圖]
[異常行為時間線 - 甘特圖]
```

**集成來源**: 引用 [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) 的即時事件流

---

## 6. 數據治理 (Data Governance)

### 6.1 數據質量檢查

**自動化檢查規則**:
```sql
-- DQ Check 1: 空值率檢查
SELECT
    'transactions' AS table_name,
    'player_id' AS column_name,
    COUNT(*) AS total_rows,
    COUNT(CASE WHEN player_id IS NULL THEN 1 END) AS null_count,
    ROUND(
        COUNT(CASE WHEN player_id IS NULL THEN 1 END)::DECIMAL / COUNT(*) * 100,
        2
    ) AS null_rate
FROM dwd.transactions
HAVING null_rate > 1.0;  -- 空值率 > 1% 則警報

-- DQ Check 2: 重複交易檢查
SELECT
    transaction_id,
    COUNT(*) AS duplicate_count
FROM dwd.transactions
GROUP BY transaction_id
HAVING COUNT(*) > 1;  -- 檢測主鍵重複

-- DQ Check 3: 每日對帳檢查
SELECT
    DATE(stat_date) AS 日期,
    SUM(total_deposit) AS DW層總存款,
    (SELECT SUM(amount) FROM ods.transactions WHERE type = 'deposit' AND DATE(created_at) = DATE(stat_date)) AS ODS層總存款,
    ABS(DW層總存款 - ODS層總存款) AS 差異金額
FROM dws.daily_player_stats
GROUP BY DATE(stat_date)
HAVING ABS(差異金額) > 0.01;  -- 差異 > $0.01 則警報
```

**定時執行**: 每日 06:00 AM 運行所有 DQ 檢查，結果發送至 #data-quality Slack 頻道

---

### 6.2 存取控制 (Row-Level Security)

**多租戶數據隔離**:
```sql
-- PostgreSQL RLS Policy: 租戶只能查看自己的數據
ALTER TABLE ads.operator_daily_pnl ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ads.operator_daily_pnl
FOR SELECT
USING (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
);

-- 應用層設置當前租戶（每次查詢前）
SET app.current_tenant_id = 123;
```

**角色權限矩陣**:
| 角色 | 可訪問層級 | 數據範圍 | 工具權限 |
|------|-----------|---------|---------|
| **財務團隊** | ADS, DWS, DWD | 所有租戶財務數據 | Tableau, Metabase |
| **客服團隊** | ADS (Player 360 View) | 僅當前處理的玩家 | Metabase (受限視圖) |
| **運營商** | ADS, DWS | 僅自己租戶數據 | Metabase |
| **數據分析師** | 所有層級 | 所有數據（脫敏後） | Tableau, Redash |

---

### 6.3 審計追蹤 (Audit Trail)

**查詢日誌記錄**:
```sql
CREATE TABLE audit.query_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(50),
    query_text TEXT,
    tables_accessed TEXT[],  -- ['dwd.transactions', 'dws.daily_player_stats']
    rows_returned BIGINT,
    execution_time_ms INT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user (user_id, executed_at DESC),
    INDEX idx_tables (tables_accessed)
);
```

**敏感數據導出監控**:
```python
# 檢測大批量數據導出（可能是數據洩露）
def detect_mass_export(user_id, rows_returned):
    if rows_returned > 10000:
        alert_to_security_team(
            message=f"User {user_id} exported {rows_returned} rows",
            severity="high"
        )

        # 自動標記該用戶查詢為待審核
        db.execute("""
            UPDATE audit.query_logs
            SET review_required = TRUE
            WHERE user_id = %s AND log_id = LAST_INSERT_ID()
        """, (user_id,))
```

---

## 📚 相關文檔

### 數據來源參考
- [00-03 數據模型總覽](../00_Concept_&_Analysis/00-03_Data_Model_Overview.md) - 完整數據庫表設計
- [02-04 流水計算與對帳](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 財務數據邏輯
- [07-04 數據管道架構](../07_Platform_Management/07-04_Data_Pipeline_Architecture.md) - CDC 同步機制

### 安全與合規參考
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 數據脫敏、加密
- [09-02 審計日誌](../09_System_Security/09-02_Audit_Log_&_Approval.md) - 操作審計要求

### 業務邏輯參考
- [01-02 VIP 忠誠系統](../01_Player_Center/01-02_VIP_&_Loyalty_System.md) - 玩家分群邏輯
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 風險指標定義

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: BI Team & Data Engineering Team
