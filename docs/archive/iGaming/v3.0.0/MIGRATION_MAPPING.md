# 文檔遷移對照表

**版本**: 1.0.0
**創建日期**: 2026-02-03
**狀態**: 📋 規劃階段

---

## 📖 使用說明

本對照表記錄了從舊結構（14 個模塊）到新結構（8 個模塊）的完整映射關係，幫助用戶快速找到遷移後的文檔位置。

**遷移時間線**：
- **Week 1-2**: 架構規劃（當前階段）
- **Week 3-4**: 內容合併與去重
- **Week 5**: 歷史記錄清理
- **Week 6-7**: 模塊合併與文件重組
- **Week 7-8**: 導航系統更新

**過渡期**：
- 舊文檔保留 30 天（標記 DEPRECATED）
- 舊文檔添加重定向註釋，指向新位置

---

## 🔄 完整遷移對照表

### 00_Concept_&_Analysis → 00_Foundation

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 00-00 | Document_Map.md | 00-00 | Document_Map.md | **簡化** | 移除冗長導航，新增 3 個核心路徑文檔 |
| 00-01 | Solution_Overview.md | 00-01 | Solution_Overview.md | 直接遷移 | 無變更 |
| 00-02 | Industry_Terminology.md | 00-02 | Industry_Terminology.md | 直接遷移 | 無變更 |
| 03-data-model | **00-05_Data_Model.md** ⚠️ | 00-03 | Data_Model.md | 重命名 | 修正編號規範 |
| 00-04 | Technology_Stack.md | 00-04 | Technology_Stack.md | 直接遷移 | 無變更 |
| - | - | **00-00** | **QUICKSTART.md** | **新建** | 10 分鐘快速入門 |
| - | - | **00-00** | **BUSINESS_FLOWS.md** | **新建** | 業務流程圖集 |
| - | - | **00-00** | **IMPLEMENTATION_GUIDE.md** | **新建** | 實作指南索引 |

### 00_Navigation → **刪除**

| 舊路徑 | 新位置 | 操作類型 | 原因 |
|--------|--------|---------|------|
| 00_Navigation/README.md | 00_Foundation/00-00_QUICKSTART.md | **功能合併** | 導航功能移至核心路徑文檔 |
| 00_Navigation/by-module/ | 00_Foundation/00-00_Document_Map.md | **功能合併** | 模塊導航整合到文檔地圖 |
| 00_Navigation/by-role/ | 00_Foundation/00-00_QUICKSTART.md | **功能合併** | 角色導航整合到快速入門 |
| 00_Navigation/by-task/ | 00_Foundation/00-00_IMPLEMENTATION_GUIDE.md | **功能合併** | 任務導航移至實作指南 |

### 01_Player_Center + 11_Customer_Service → 01_Core_Financial_Loop

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 01-01 | Player_Account_System.md | 01-01 | Player_Lifecycle.md | **合併** | 整合部分 11-01 客服內容 |
| 01-02 | VIP_&_Loyalty_System.md | 03-04 | VIP_Loyalty.md | **移動** | 移至活動系統模塊 |
| 01-03 | Player_Segmentation.md | 06-02 | Customer_Service.md | **合併** | 整合至客服平台 |
| 11-01 | CS_Platform_Design.md | 06-02 | Customer_Service.md | **擴充合併** | 擴充至 500 行 |

### 02_Finance_Center → 01_Core_Financial_Loop

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 02-01 | Withdrawal_Risk_Control.md | 01-05 | Withdrawal_Risk.md | 直接遷移 | 無變更 |
| 02-02 | Payment_Gateway_Integration.md | 01-03 | Payment_Integration.md | 直接遷移 | 簡短文件（44 行）|
| 02-03 | Reconciliation_System.md | 01-06 | Reconciliation.md | **合併** | 整合 02-04 對帳部分 |
| **02-04** | **Turnover_and_Game_Reconciliation_Analysis.md** | **02-03** | **Turnover_Calculation.md** | **關鍵合併** | 合併 02-04-diagrams/（1150→1800 行）|
| 02-05 | Billing_&_Invoicing.md | 06-01 | Reporting_BI.md | **合併** | 計費邏輯整合至報表模塊 |
| **02-06** | **Unified_Wallet_Model.md** | **01-02** | **Wallet_Architecture.md** | **關鍵合併** | 合併 seamless-wallet/core/（615→900 行）|
| 02-07 | Transaction_Processing_Flow.md | 01-04 | Transaction_Flow.md | 直接遷移 | 無變更 |

### 02_Finance_Center/seamless-wallet → 多處拆分

| 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|---------|------|
| seamless-wallet/README.md | - | - | **刪除** | 功能被新 README 取代 |
| seamless-wallet/core/01-security.md | 01-02 | Wallet_Architecture.md §3 | **合併** | API 安全設計 |
| seamless-wallet/core/02-concurrency.md | 01-02 | Wallet_Architecture.md §4 | **合併** | 並發控制 |
| seamless-wallet/core/03-recovery.md | 01-02 | Wallet_Architecture.md §5 | **合併** | 錯誤恢復 |
| seamless-wallet/finance/accounting.md | 01-06 | Reconciliation.md §4 | **合併** | 會計分錄 |
| seamless-wallet/finance/reconciliation.md | 01-06 | Reconciliation.md §3 | **合併** | 對帳模型 |
| seamless-wallet/game-logic/sports-betting.md | 02-04 | Game_Provider_Cases.md §1 | **合併** | 體育博彩案例 |
| seamless-wallet/game-logic/free-spins.md | 02-04 | Game_Provider_Cases.md §2 | **合併** | 免費旋轉案例 |
| seamless-wallet/game-logic/roulette-hedge.md | 02-04 | Game_Provider_Cases.md §3 | **合併** | 輪盤對沖案例 |
| seamless-wallet/game-logic/baccarat-tie.md | 02-04 | Game_Provider_Cases.md §4 | **合併** | 百家樂平局案例 |

### 02_Finance_Center/02-04-diagrams → 02_Game_Operations

| 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|---------|------|
| 02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md | 02-03 | Turnover_Calculation.md §5 | **合併** | 流程圖 |
| 02-04-diagrams/02-04-02_Calculation_Logic.md | 02-03 | Turnover_Calculation.md §3 | **合併** | 計算邏輯 |
| 02-04-diagrams/02-04-03_Implementation_Details.md | 02-03 | Turnover_Calculation.md §4 | **合併** | 實作細節 |

### 03_Game_Center → 02_Game_Operations

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 03-01 | Game_Integration_Standard.md | 02-01 | Game_Integration.md | **合併** | 合併 03-02 |
| 03-02 | Game_Lobby_Management.md | 02-01 | Game_Integration.md §6 | **合併** | 簡短文件（51 行）|
| **03-03** | **Seamless_Wallet_Analysis.md** | **02-02** | **Seamless_Wallet_API.md** | **關鍵合併** | 合併 seamless-wallet/core/ |

### 04_Activity_Center → 03_Promotion_System

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 01-system-design | **01-system-design.md** ⚠️ | 03-01 | Bonus_Engine.md | **重命名+拆分** | 修正編號，拆分流水部分 |
| 04-02 | Bonus_Calculation_Engine.md | 03-02 | Campaign_Management.md | 直接遷移 | 無變更 |
| 04-03 | Activity_Risk_Control.md | 04-02 | Fraud_Detection.md §5 | **合併** | 整合至反欺詐模塊 |
| - | (流水要求邏輯) | 03-03 | Wagering_Rules.md | **拆分** | 從 04-01 拆分出來 |

### 05_Risk_Management → 04_Risk_Control

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 05-01 | Risk_Control_System.md | 04-01 | Risk_Engine.md | 直接遷移 | 無變更 |
| **05-02** | **Agent_Credit_Risk.md** | **04-03** | **Agent_Credit_Risk.md** | **合併** | 合併 06-02 信用網絡邏輯 |
| 05-03 | Passive_Risk_Control_System.md | 04-02 | Fraud_Detection.md | 直接遷移 | 無變更 |
| 05-04 | Risk_Proposal_Workflow.md | 04-04 | Risk_Workflow.md | 直接遷移 | 無變更 |

### 06_Agent_Center → 05_Platform_Governance + 04_Risk_Control

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 06-01 | Affiliate_System_Design.md | 05-02 | Agent_System.md | 直接遷移 | 無變更 |
| 06-02 | Credit_Network_Logic.md | 04-03 | Agent_Credit_Risk.md §3 | **合併** | 整合至代理風控 |

### 07_Platform_Management → 05_Platform_Governance + 06_Analytics_Operations

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 07-01 | Hierarchy_Architecture.md | 05-01 | Multi_Tenant_Arch.md | 直接遷移 | 簡短文件（39 行）→ 擴充 |
| 07-02 | Tenant_Configuration.md | 05-01 | Multi_Tenant_Arch.md §4 | **合併** | 整合至多租戶架構 |
| 07-03 | Notification_Architecture.md | 06-02 | Customer_Service.md §6 | **合併** | 整合至客服平台 |
| 07-04 | Data_Pipeline_Architecture.md | 06-01 | Reporting_BI.md §5 | **合併** | 整合至報表模塊 |

### 08_Frontend_CMS → 08_Frontend_CMS (保留)

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 08-01 | Frontend_Layout_Engine.md | 08-01 | Frontend_Layout_Engine.md | 直接遷移 | 無變更 |
| 08-02 | Banner_&_Announcement.md | 08-02 | Banner_Announcement.md | 直接遷移 | 移除 & 符號 |
| 08-03 | SEO_&_Performance.md | 08-03 | SEO_Performance.md | 直接遷移 | 移除 & 符號 |
| 08-04 | Mobile_App_Architecture.md | 08-04 | Mobile_App_Architecture.md | 直接遷移 | 無變更 |
| - | **缺失 08-05** ⚠️ | - | - | - | 編號跳號問題 |
| 08-06 | AB_Testing_Framework.md | 08-06 | AB_Testing_Framework.md | 直接遷移 | 無變更 |
| 01-localization | **01-localization/** ⚠️ | 08-05-localization | localization/ | **重命名+修復** | 修正編號，修復損壞的 README |

### 09_System_Security → 05_Platform_Governance

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 09-01 | Admin_RBAC.md | 05-03 | RBAC_Security.md | 直接遷移 | 無變更 |
| 09-02 | Audit_Log_System.md | 05-04 | Audit_System.md | 直接遷移 | 無變更 |
| **09-03** | **Data_Security_Standard.md** | **05-05** | **Data_Security.md** | **關鍵去重** | **合併 4 個文件（3079→1800 行）** |
| **09-03-01** | **Encryption_Strategy.md** | ↑ | Data_Security.md §2.1 | **合併** | 加密策略 |
| **09-03-02** | **Blind_Index_Architecture.md** | ↑ | Data_Security.md §2.2 | **合併** | 盲索引 |
| **09-03-03** | **GDPR_Data_Deletion.md** | ↑ | Data_Security.md §2.3 | **合併** | GDPR 合規 |
| 09-04 | Approval_Workflow_System.md | 05-06 | Approval_Workflow.md | 直接遷移 | 無變更 |

### 10_Reporting_&_BI → 06_Analytics_Operations

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| **10-01** | **Reporting_Architecture.md** | **06-01** | **Reporting_BI.md** | **合併+擴充** | 合併 07-04，新增即時報表（500 行）|

### 12_Technical_Operations → 07_Technical_Infrastructure

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| 12-01 | Deployment_Architecture.md | 07-01 | Deployment.md | 直接遷移 | 無變更 |
| 12-02 | QA_Testing_Standard.md | 07-04 | QA_Standards.md | 直接遷移 | 無變更 |
| **12-03** | **Gateway_Architecture.md** | **07-02/** | **Gateway_Architecture/** | **拆分** | **拆分為 3 個子文件（1131 行）** |
| - | - | 07-02-01 | Gateway_Core.md | **拆分** | 核心架構（500 行）|
| - | - | 07-02-02 | Rate_Limiting.md | **拆分** | 限流算法（300 行）|
| - | - | 07-02-03 | Security.md | **拆分** | 安全防護（350 行）|
| 12-04 | Maintenance_Procedure.md | 07-05 | Maintenance.md | 直接遷移 | 無變更 |
| 12-05 | API_Design_Standard.md | 07-03 | API_Design/ (目錄) | **拆分** | 保留為索引 |
| **12-05-01** | **API_Design_Examples.md** | **07-03/** | **API_Design/** | **拆分** | **拆分為 4 個子文件（1933 行）** |
| - | - | 07-03-01 | Design_Principles.md | **拆分** | 設計原則（400 行）|
| - | - | 07-03-02 | Authentication.md | **拆分** | 認證機制（300 行）|
| - | - | 07-03-03 | Common_Patterns.md | **拆分** | 通用模式（500 行）|
| - | - | 07-03-04 | Domain_APIs.md | **拆分** | 領域 API（700 行）|
| 12-06 | Performance_Monitoring.md | 07-05 | Maintenance.md §6 | **合併** | 整合至維護程序 |

### 13_Third_Party_Integration → 06_Analytics_Operations

| 舊編號 | 舊路徑 | 新編號 | 新路徑 | 操作類型 | 備註 |
|--------|--------|--------|--------|---------|------|
| **13-01** | **Third_Party_Integration_Standard.md** | **06-03** | **Third_Party_Integration.md** | **擴充** | 擴充至 350 行，新增 Webhook 管理 |

---

## 📊 遷移統計

### 操作類型分布

| 操作類型 | 數量 | 說明 |
|---------|------|------|
| **直接遷移** | 28 | 文件直接移動，無內容變更 |
| **合併** | 23 | 多個文件合併為一個 |
| **拆分** | 2 | 一個超長文件拆分為多個 |
| **重命名** | 5 | 修正編號規範或命名 |
| **擴充** | 4 | 簡短文件擴充內容 |
| **刪除** | 1 | 整個目錄刪除（00_Navigation）|
| **新建** | 3 | 新增核心路徑文檔 |

### 模塊級別變更

| 舊模塊 | 新模塊 | 文件數變化 | 類型 |
|--------|--------|-----------|------|
| 00_Concept_&_Analysis | 00_Foundation | 5 → 8 | 擴充（+3 核心路徑文檔）|
| 00_Navigation | - | 4 → 0 | **完全刪除** |
| 01_Player_Center | 01_Core_Financial_Loop | 3 → 6 | 合併 |
| 02_Finance_Center | 01_Core_Financial_Loop + 02_Game_Operations | 18 → 10 | **大規模重組** |
| 03_Game_Center | 02_Game_Operations | 3 → 4 | 合併 |
| 04_Activity_Center | 03_Promotion_System | 4 → 4 | 重組 |
| 05_Risk_Management | 04_Risk_Control | 4 → 4 | 保留 |
| 06_Agent_Center | 05_Platform_Governance | 2 → 合併 | 合併至治理模塊 |
| 07_Platform_Management | 05_Platform_Governance + 06_Analytics_Operations | 4 → 分散 | 拆分 |
| 08_Frontend_CMS | 08_Frontend_CMS | 7 → 7 | **保留**（修復 i18n）|
| 09_System_Security | 05_Platform_Governance | 7 → 6 | **去重合併** |
| 10_Reporting_&_BI | 06_Analytics_Operations | 1 → 合併 | 合併 |
| 11_Customer_Service | 06_Analytics_Operations | 1 → 合併 | 合併 |
| 12_Technical_Operations | 07_Technical_Infrastructure | 7 → 5（+4 子文件）| **拆分** |
| 13_Third_Party_Integration | 06_Analytics_Operations | 1 → 合併 | 合併 |

**總計**：115 個文件 → 預估 65-70 個文件（**-40%**）

---

## 🔍 快速查找

### 按舊編號查找

**範例：我想找舊的 02-06 Unified_Wallet_Model.md**

→ 查找「02-06」
→ 新位置：`01_Core_Financial_Loop/01-02_Wallet_Architecture.md`
→ 操作：關鍵合併（合併了 seamless-wallet/core/）

### 按概念查找

**範例：我想找可下注餘額公式**

→ 查閱 [SSOT_MAPPING.md](./SSOT_MAPPING.md)
→ 概念 ID：F-01
→ 權威文檔：`01_Core_Financial_Loop/01-02_Wallet_Architecture.md §2.3`

---

## ⚠️ 重要提醒

### 關鍵合併任務（Week 3-4 優先）

1. **09-03 系列 → 05-05 Data_Security.md**
   - 最大去重任務（3079 → 1800 行）
   - 需要仔細驗證概念定義無遺漏

2. **02-06 + seamless-wallet → 01-02 Wallet_Architecture.md**
   - 財務系統 SSOT
   - 需要保留所有核心公式

3. **02-04 + 02-04-diagrams → 02-03 Turnover_Calculation.md**
   - 流水計算權威定義
   - 需要整合補充圖表

### 編號規範問題需修正

| 文件 | 問題 | 修正 |
|------|------|------|
| 00_Concept_&_Analysis/00-05_Data_Model.md | 編號不規範 | 重命名為 00-03_Data_Model.md |
| 04_Activity_Center/01-system-design.md | 編號不規範 | 重命名為 03-01_Bonus_Engine.md |
| 08_Frontend_CMS/01-localization/ | 編號不規範 | 重命名為 08-05-localization/ |

### 損壞文件需修復

| 文件 | 問題 | 修正 |
|------|------|------|
| 08_Frontend_CMS/01-localization/README.md | 充滿 Bonus 佔位符 | 完全重寫 |

---

## 📝 更新日誌

| 日期 | 版本 | 變更說明 |
|------|------|---------|
| 2026-02-03 | 1.0.0 | 初始版本，完整映射 115 個文件 |

---

## 🔗 相關文檔

- [SSOT 映射表](./SSOT_MAPPING.md)
- [重組計劃](../../../.claude/plans/mutable-kindling-dijkstra.md)
- [文檔更新規範](../.github/DOCUMENTATION_STANDARDS.md)

---

**維護團隊**: Architecture Team
**使用反饋**: architecture@company.com
