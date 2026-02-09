# iGaming 平台文檔中心

> **版本**: v4.1.0
> **最後更新**: 2026-02-09
> **文檔數量**: 290+ 個 Markdown 文件
> **總行數**: ~150,000 行

---

## 文檔結構 (2026-02-09 Phase 9 Complete)

文檔已按受眾分為兩個主要目錄 + 一個存檔目錄：

| 目錄 | 受眾 | 說明 | 文件數 |
|------|------|------|--------|
| **[requirements/](requirements/)** | 老闆、產品經理、合規人員 | 業務需求 - WHAT & WHY | ~66 |
| **[architecture/](architecture/)** | 架構師、開發人員、DevOps | 技術實現 - HOW | ~117 |
| **[source-archive/](source-archive/)** | 唯讀參考 | 原始 SSOT 文檔（存檔） | 193 |

### For Executives (requirements/) - 66 documents

| Category | Documents | Key Topics |
|----------|-----------|------------|
| [01_Player_Experience](requirements/01_Player_Experience/) | 6 | Player lifecycle, business flows, platform overview, glossary |
| [02_Financial_Operations](requirements/02_Financial_Operations/) | 5 | Payment, reconciliation, wallet rules, turnover |
| [03_Gaming_Operations](requirements/03_Gaming_Operations/) | 4 | Turnover rules, game integration, lobby |
| [04_Promotions_VIP](requirements/04_Promotions_VIP/) | 3 | Bonus rules, activity risk, promotions |
| [05_Risk_Compliance](requirements/05_Risk_Compliance/) | 11 | KYC/AML, fraud, ML, jurisdiction, player protection |
| [06_Governance_Licensing](requirements/06_Governance_Licensing/) | 6 | Multi-tenant, MFA, governance |
| [07_Agent_Operations](requirements/07_Agent_Operations/) | 2 | Credit network, agent system |
| [08_Analytics_Operations](requirements/08_Analytics_Operations/) | 2 | Reporting, BI dashboards |
| [09_Infrastructure_Requirements](requirements/09_Infrastructure_Requirements/) | 2 | QA standards, cost optimization |
| [10_Platform_Operations](requirements/10_Platform_Operations/) | 3 | Tenant config, notifications, data pipeline |
| [11_Frontend_Experience](requirements/11_Frontend_Experience/) | 4 | UX, SEO, mobile, localization |
| [12_Security_Compliance](requirements/12_Security_Compliance/) | 3 | Data protection, compliance, payment security |
| [13_Customer_Service](requirements/13_Customer_Service/) | 2 | CS platform, operations |
| [14_Integration_Standards](requirements/14_Integration_Standards/) | 1 | Third-party integration |
| [15_Responsible_Gambling](requirements/15_Responsible_Gambling/) | 4 | Self-exclusion, deposit limits, session protection |

See full listing: **[requirements/README.md](requirements/README.md)**

### For Developers (architecture/) - 117 documents

| Category | Documents | Key Topics |
|----------|-----------|------------|
| [00_Overview](architecture/00_Overview/) | 5 | Tech stack, data model, platform architecture |
| [01_Player_Service](architecture/01_Player_Service/) | 1 | Player state machine, database design |
| [02_Finance_Service](architecture/02_Finance_Service/) | 8 | Payment API, wallet, turnover calculation |
| [03_Game_Integration](architecture/03_Game_Integration/) | 4 | GP API, turnover logic, lobby system |
| [04_Activity_Engine](architecture/04_Activity_Engine/) | 3 | Bonus engine, activity risk, promotions |
| [05_Risk_Engine](architecture/05_Risk_Engine/) | 10 | Kafka/Flink, ML pipeline, fraud detection |
| [06_Platform_Core](architecture/06_Platform_Core/) | 8 | Multi-tenant, MFA, jurisdiction routing |
| [07_Agent_Service](architecture/07_Agent_Service/) | 2 | Credit network, agent system |
| [08_Analytics_Service](architecture/08_Analytics_Service/) | 2 | Reporting, BI dashboards |
| [09_Infrastructure](architecture/09_Infrastructure/) | 23 | API Gateway, caching, streaming, token validation |
| [10_Platform_Management](architecture/10_Platform_Management/) | 3 | Tenant config, notifications, data pipeline |
| [11_Frontend](architecture/11_Frontend/) | 11 | Layout engine, i18n, mobile, A/B testing |
| [12_Security](architecture/12_Security/) | 10 | Encryption, GDPR, blind index, ISO 27001 |
| [13_Customer_Service](architecture/13_Customer_Service/) | 2 | CS platform, operations |
| [14_Third_Party](architecture/14_Third_Party/) | 1 | Third-party integration |
| [15_Responsible_Gambling](architecture/15_Responsible_Gambling/) | 4 | Self-exclusion, deposit limits, player protection API |
| [adr](architecture/adr/) | 3 | Architecture Decision Records |
| [quality-reports](architecture/quality-reports/) | 2 | Quality gate reports |

See full listing: **[architecture/README.md](architecture/README.md)**

---

## 快速開始

### 新手入門路徑

```text
1. 閱讀本文件 (README.md) - 了解整體架構
2. 進入 source-archive/00_Foundation/ - 實施指南與業務流程
3. 按需查閱各業務模塊
```

### 關鍵文檔

| 文檔 | 位置 | 說明 |
|------|------|------|
| **快速入門** | [00-01_Quickstart](source-archive/00_Foundation/00-01_Quickstart.md) | 10 分鐘快速入門 |
| **業務流程** | [00-02_Business_Flows](source-archive/00_Foundation/00-02_Business_Flows.md) | 核心業務流程圖 |
| **實施指南** | [00-03_Implementation_Guide](source-archive/00_Foundation/00-03_Implementation_Guide.md) | 完整實施路線圖 |

---

## 按角色快速訪問

| 角色 | 核心文檔 |
|------|---------|
| **老闆/PM** | [requirements/](requirements/) — 純業務語言，無代碼 |
| **架構師/開發者** | [architecture/](architecture/) — 技術架構、API、代碼範例 |
| **風控人員** | [requirements/05_Risk_Compliance/](requirements/05_Risk_Compliance/) |
| **支付運營** | [requirements/02_Financial_Operations/](requirements/02_Financial_Operations/) |
| **合規人員** | [requirements/15_Responsible_Gambling/](requirements/15_Responsible_Gambling/), [requirements/12_Security_Compliance/](requirements/12_Security_Compliance/) |
| **客服主管** | [requirements/13_Customer_Service/](requirements/13_Customer_Service/) |

### 核心業務流程

| 流程 | 相關文檔路徑 |
|------|-------------|
| **存款** | 02 Financial → Payment Operations → Reconciliation |
| **出金** | 01 Player → Risk Compliance → Financial Operations |
| **投注** | 03 Gaming → Turnover → Risk Control |
| **Bonus** | 04 Promotions → Activity Risk → Wallet |

---

## 三層風控架構（核心概念）

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

---

## 核心業務指標

| 指標類型 | 關鍵指標 | 說明 |
|---------|---------|------|
| **玩家** | DAU, MAU, Retention | 活躍度與留存 |
| **財務** | NGR, GGR, Deposit Rate | 收入與充值 |
| **遊戲** | Rounds, RTP, House Edge | 遊戲表現 |
| **活動** | Bonus ROI, Conversion | 活動效益 |
| **風控** | Fraud Rate, False Positive | 風控效能 |

---

## 技術棧概覽

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

## 目錄結構

```text
docs/iGaming/
├── README.md                          # 本文件
├── requirements/                      # 業務需求文檔 (給老闆/PM)
│   ├── 01_Player_Experience/          # 玩家體驗
│   ├── 02_Financial_Operations/       # 財務營運
│   ├── 03_Gaming_Operations/          # 遊戲營運
│   ├── 04_Promotions_VIP/             # 活動促銷
│   ├── 05_Risk_Compliance/            # 風控合規
│   ├── 06_Governance_Licensing/       # 治理牌照
│   ├── 07_Agent_Operations/           # 代理營運
│   ├── 08_Analytics_Operations/       # 分析報表
│   ├── 09_Infrastructure_Requirements/ # 基礎設施需求
│   ├── 10_Platform_Operations/        # 平台營運
│   ├── 11_Frontend_Experience/        # 前端體驗
│   ├── 12_Security_Compliance/        # 安全合規
│   ├── 13_Customer_Service/           # 客服
│   ├── 14_Integration_Standards/      # 整合標準
│   └── 15_Responsible_Gambling/       # 負責任博彩
├── architecture/                      # 技術架構文檔 (給架構師/開發者)
│   ├── 00_Overview/                   # 技術概覽
│   ├── 01_Player_Service/             # 玩家服務
│   ├── 02_Finance_Service/            # 財務服務
│   ├── 03_Game_Integration/           # 遊戲整合
│   ├── 04_Activity_Engine/            # 活動引擎
│   ├── 05_Risk_Engine/                # 風控引擎
│   ├── 06_Platform_Core/              # 平台核心
│   ├── 07_Agent_Service/              # 代理服務
│   ├── 08_Analytics_Service/          # 分析服務
│   ├── 09_Infrastructure/             # 技術基礎設施
│   ├── 10_Platform_Management/        # 平台管理
│   ├── 11_Frontend/                   # 前端架構
│   ├── 12_Security/                   # 安全架構
│   ├── 13_Customer_Service/           # 客服架構
│   ├── 14_Third_Party/                # 第三方整合
│   ├── 15_Responsible_Gambling/       # 負責任博彩架構
│   ├── adr/                           # Architecture Decision Records
│   └── quality-reports/               # 品質報告
└── source-archive/                    # 原始 SSOT 文檔（唯讀存檔）
    ├── 00_Foundation/
    ├── 01_Player_Center/
    ├── ...
    └── 15_Responsible_Gambling/
```

---

## 相關資源

- **SmartAdmin 主文檔**: [CLAUDE.md](../../CLAUDE.md)
- **API 文檔**: [architecture/09_Infrastructure](architecture/09_Infrastructure/)
- **安全標準**: [architecture/12_Security](architecture/12_Security/)

---

**索引版本**: 4.0.0
**創建日期**: 2026-02-07
**重組日期**: 2026-02-09
**Phase 9 Complete**: 2026-02-09 (Full 16-module coverage: requirements + architecture)
**維護團隊**: iGaming Platform Team
