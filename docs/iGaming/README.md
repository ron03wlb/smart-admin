# iGaming 平台文檔中心

> **版本**: v4.0.0
> **最後更新**: 2026-02-07
> **文檔數量**: 133+ 個 Markdown 文件
> **總行數**: ~82,000 行

---

## 🚀 快速開始

### 新手入門路徑

```text
1. 閱讀本文件 (README.md) - 了解整體架構
2. 進入 00_Foundation/ - 實施指南與業務流程
3. 按需查閱各業務模塊
```

### 關鍵文檔

| 文檔 | 位置 | 說明 |
|------|------|------|
| **快速入門** | [00-01_Quickstart](00_Foundation/00-01_Quickstart.md) | 10 分鐘快速入門 |
| **業務流程** | [00-02_Business_Flows](00_Foundation/00-02_Business_Flows.md) | 核心業務流程圖 |
| **實施指南** | [00-03_Implementation_Guide](00_Foundation/00-03_Implementation_Guide.md) | 完整實施路線圖 |

---

## 📂 模塊導航（15 個主模塊）

### 🏗️ 基礎層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **00** | [Foundation](00_Foundation/) | 實施指南、業務流程、快速入門 | 4 |

### 👤 業務中心層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **01** | [Player_Center](01_Player_Center/) | 玩家生命週期、VIP、提款風控 | 4 |
| **02** | [Finance_Center](02_Finance_Center/) | 支付、對帳、錢包、流水計算（Layer 2）| 6 |
| **03** | [Game_Center](03_Game_Center/) | 遊戲整合、大廳、Seamless Wallet | 4 |
| **04** | [Activity_Center](04_Activity_Center/) | 活動系統、獎金計算（Layer 3）、風控 | 4 |

### ⚖️ 風控與治理層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **05** | [Risk_Control](05_Risk_Control/) | 風控框架（Layer 1）、欺詐檢測、KYC | 7 |
| **06** | [Platform_Governance](06_Platform_Governance/) | 多租戶、RBAC、審計、審批 | 6 |

### 👥 代理與分析層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **07** | [Agent_Center](07_Agent_Center/) | 代理網絡、信用體系 | 2 |
| **08** | [Analytics_BI](08_Analytics_BI/) | 報表、商業智能 | 2 |

### ⚙️ 技術基礎設施層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **09** | [Technical_Infrastructure](09_Technical_Infrastructure/) | 部署、API 閘道、性能、緩存 | 15 |
| **10** | [Platform_Management](10_Platform_Management/) | 租戶配置、通知、數據管道 | 3 |

### 🎨 前端與安全層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **11** | [Frontend_CMS](11_Frontend_CMS/) | 前端架構、CMS、i18n | 9 |
| **12** | [System_Security](12_System_Security/) | 數據安全、加密、GDPR | 4 |

### 🤝 服務與整合層

| 編號 | 模塊名稱 | 說明 | 文檔數 |
|------|---------|------|--------|
| **13** | [Customer_Service](13_Customer_Service/) | 客服平台、運營流程 | 2 |
| **14** | [Third_Party_Integration](14_Third_Party_Integration/) | 第三方整合標準 | 1 |

---

## 👤 按角色快速訪問

| 角色 | 核心文檔 |
|------|---------|
| **風控人員** | [05-01 風控框架](05_Risk_Control/05-01_Risk_Framework.md), [05-02 欺詐檢測](05_Risk_Control/05-02_Fraud_Detection.md) |
| **支付運營** | [02-02 支付閘道](02_Finance_Center/02-02_Payment_Gateway_Integration.md), [02-03 對帳系統](02_Finance_Center/02-03_Reconciliation_System.md) |
| **活動運營** | [04-02 獎金引擎](04_Activity_Center/04-02_Bonus_Calculation_Engine.md), [04-03 活動風控](04_Activity_Center/04-03_Activity_Risk_Control.md) |
| **開發人員** | [09-02 API 閘道](09_Technical_Infrastructure/09-02_API_Gateway.md), [00-03 實施指南](00_Foundation/00-03_Implementation_Guide.md) |

### 核心業務流程

| 流程 | 相關文檔路徑 |
|------|-------------|
| **存款** | 02-02 支付閘道 → 02-03 對帳系統 → 02-06 錢包架構 |
| **出金** | 01-05 提款風控 → 06-04 審批工作流 → 02-02 支付閘道 |
| **投注** | 03-01 遊戲整合 → 03-04 流水計算 → 05-01 風控框架 |
| **Bonus** | 04-02 獎金引擎 → 04-03 活動風控 → 02-06 錢包架構 |

---

## 🔗 三層風控架構（核心概念）

iGaming 平台採用三層風控架構進行流水驗證：

```text
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: Risk Control (05_Risk_Control)                    │
│  ├── 風控驗證：RiskFactor = 0 (無效) or 1 (有效)            │
│  └── 對沖檢測、多帳號識別、異常行為偵測                      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Layer 2: Finance (02_Finance_Center)                       │
│  ├── 狀態因子：StatusFactor = 0%, 50%, 100%                 │
│  └── WIN/LOSS=100%, VOID=0%, CASHOUT=50%                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: Activity (04_Activity_Center)                     │
│  ├── 遊戲權重：GameWeight = 5%-100%                         │
│  └── Slots=100%, Blackjack=10%, Poker=0%                    │
└─────────────────────────────────────────────────────────────┘
```

**統一流水計算公式**：

```text
ValidTurnover = BetAmount
                × Layer1_RiskFactor     (05-01: 0 or 1)
                × Layer2_StatusFactor   (02-04: 0%, 50%, 100%)
                × Layer3_GameWeight     (04-02: 5%-100%)
```

**詳細文檔**：
- Layer 1: [05-01_Risk_Framework.md](05_Risk_Control/05-01_Risk_Framework.md)
- Layer 2: [02-04_Turnover_and_Game_Reconciliation_Analysis.md](02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md)
- Layer 3: [04-02_Bonus_Calculation_Engine.md](04_Activity_Center/04-02_Bonus_Calculation_Engine.md)

---

## 📊 核心業務指標

| 指標類型 | 關鍵指標 | 說明 |
|---------|---------|------|
| **玩家** | DAU, MAU, Retention | 活躍度與留存 |
| **財務** | NGR, GGR, Deposit Rate | 收入與充值 |
| **遊戲** | Rounds, RTP, House Edge | 遊戲表現 |
| **活動** | Bonus ROI, Conversion | 活動效益 |
| **風控** | Fraud Rate, False Positive | 風控效能 |

---

## 🛠️ 技術棧概覽

| 層級 | 技術選型 | 用途 |
|------|---------|------|
| API 閘道 | Kong / AWS API Gateway | 限流、認證、路由 |
| 事件串流 | Apache Kafka | 即時事件處理 |
| 流處理 | Apache Flink | 實時計算 |
| 主數據庫 | PostgreSQL | ACID 事務 |
| 高併發 | ScyllaDB / CockroachDB | 流水記錄 |
| 緩存 | Redis / JetCache | 狀態快取 |
| 搜索 | Elasticsearch | 日誌分析 |

---

## 📋 文檔命名規範

### 文件命名格式

```text
XX-YY[-ZZ]_Document_Name.md

XX = 模塊編號 (00-14)
YY = 文檔編號 (01-99)
ZZ = 子文檔編號（可選）
```

**範例**：
- `05-01_Risk_Framework.md` - 風控框架
- `09-03-02_Authentication.md` - API 認證（子文檔）
- `12-03-01_Encryption_Strategy.md` - 加密策略（子文檔）

### 優先級標記

| 標記 | 說明 |
|------|------|
| P0 | 核心必讀 |
| P1 | 進階主題 |
| P2 | 參考文檔 |

---

## ⚠️ 超大文檔標記

以下文檔超過 1600 行，建議優先閱讀索引或考慮拆分：

| 文檔 | 行數 | 模塊 |
|------|------|------|
| 00-00_IMPLEMENTATION_GUIDE.md | 2,379 | Foundation |
| 03-04_Turnover_Calculation.md | 2,332 | Game_Center |
| 06-06_MFA_Implementation.md | 2,311 | Platform_Governance |
| 05-02_Fraud_Detection.md | 2,200 | Risk_Control |
| 09-13_Token_Validation_Service.md | 199 (索引) | Technical_Infrastructure |

---

## 📁 目錄結構

```text
docs/iGaming/
├── README.md                          # 本文件
├── 00_Foundation/                     # 基礎文檔
├── 01_Player_Center/                  # 玩家中心
├── 02_Finance_Center/                 # 財務中心
│   └── seamless-wallet/               # Seamless Wallet 子模塊
├── 03_Game_Center/                    # 遊戲中心
│   └── seamless-wallet/               # Seamless Wallet 詳細設計
├── 04_Activity_Center/                # 活動中心
│   └── archive/                       # 歷史版本
├── 05_Risk_Control/                   # 風控中心
├── 06_Platform_Governance/            # 平台治理
├── 07_Agent_Center/                   # 代理中心
├── 08_Analytics_BI/                   # 數據分析
├── 09_Technical_Infrastructure/       # 技術基礎設施
├── 10_Platform_Management/            # 平台管理
├── 11_Frontend_CMS/                   # 前端 CMS
├── 12_System_Security/                # 系統安全
├── 13_Customer_Service/               # 客服中心
├── 14_Third_Party_Integration/        # 第三方整合
└── 000_improve/                       # 改進計劃
```

---

## 📖 相關資源

- **SmartAdmin 主文檔**: [CLAUDE.md](../../CLAUDE.md)
- **API 文檔**: [09_Technical_Infrastructure](09_Technical_Infrastructure/)
- **安全標準**: [12_System_Security](12_System_Security/)

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: iGaming Platform Team
