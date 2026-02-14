# iGaming 需求文檔

> **受眾（Audience）**: 高階主管、產品經理、合規官、業務分析師
> **重點（Focus）**: 做什麼和為什麼 - 業務需求、政策和合規
> **最後更新（Last Updated）**: 2026-02-09
> **階段（Phase）**: 9 完成（15 個類別共 66 份文檔）

---

## 類別導航（Category Navigation）

| 類別 | 說明 | 文件數量 |
|----------|-------------|-----------|
| [01_Player_Experience](01_Player_Experience/) | 玩家生命週期、VIP、細分 | 6 |
| [02_Financial_Operations](02_Financial_Operations/) | 支付、對帳、錢包規則 | 5 |
| [03_Gaming_Operations](03_Gaming_Operations/) | 遊戲整合、投注額規則 | 4 |
| [04_Promotions_VIP](04_Promotions_VIP/) | 紅利規則、活動政策 | 3 |
| [05_Risk_Compliance](05_Risk_Compliance/) | KYC/AML、欺詐檢測、授權 | 11 |
| [06_Governance_Licensing](06_Governance_Licensing/) | RBAC、審計、多司法管轄區、MFA | 6 |
| [07_Agent_Operations](07_Agent_Operations/) | 信用網絡、代理系統 | 2 |
| [08_Analytics_Operations](08_Analytics_Operations/) | 報告、BI 儀表板 | 2 |
| [09_Infrastructure_Requirements](09_Infrastructure_Requirements/) | QA 標準、成本優化 | 2 |
| [10_Platform_Operations](10_Platform_Operations/) | 租戶配置、通知、數據管道 | 3 |
| [11_Frontend_Experience](11_Frontend_Experience/) | UX、SEO、移動端、本地化 | 4 |
| [12_Security_Compliance](12_Security_Compliance/) | 數據保護、合規、支付安全 | 3 |
| [13_Customer_Service](13_Customer_Service/) | 客服平台、運營 | 2 |
| [14_Integration_Standards](14_Integration_Standards/) | 第三方整合 | 1 |
| [15_Responsible_Gambling](15_Responsible_Gambling/) | 自我排除、存款限額、會話保護 | 4 |

---

## 所有文檔（All Documents）

### 01 玩家體驗（Player Experience）（6 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [玩家生命週期（Player Lifecycle）](01_Player_Experience/04_Player_Lifecycle.md) | 玩家階段、KYC 等級、狀態轉換 |
| [業務流程（Business Flows）](01_Player_Experience/05_Business_Flows.md) | 核心業務流程、玩家旅程圖 |
| [平台概覽（Platform Overview）](01_Player_Experience/01_Platform_Overview.md) | 平台功能、5 個核心業務概念 |
| [解決方案概覽（Solution Overview）](01_Player_Experience/02_Solution_Overview.md) | 平台定位、模塊關係 |
| [行業術語表（Industry Glossary）](01_Player_Experience/06_Industry_Glossary.md) | iGaming 術語參考 |
| [術語標準（Terminology Standards）](01_Player_Experience/03_Terminology_Standards.md) | 命名慣例、術語標準化 |

### 02 財務運營（Financial Operations）（5 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [支付運營（Payment Operations）](02_Financial_Operations/03_Payment_Operations.md) | 支付方式、成本、SLA、合規 |
| [對帳需求（Reconciliation Requirements）](02_Financial_Operations/04_Reconciliation_Requirements.md) | 三層匹配、差異處理 |
| [財務實施需求（Financial Implementation Requirements）](02_Financial_Operations/02_Financial_Implementation_Requirements.md) | 錢包業務規則、支付處理政策 |
| [投注額對帳需求（Turnover Reconciliation Requirements）](02_Financial_Operations/05_Turnover_Reconciliation_Requirements.md) | 投注額驗證政策、遊戲對帳 |
| [無縫錢包需求（Seamless Wallet Requirements）](02_Financial_Operations/01_Seamless_Wallet_Requirements.md) | 無縫錢包業務規則、狀態轉換 |

### 03 遊戲運營（Gaming Operations）（4 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [投注額業務規則（Turnover Business Rules）](03_Gaming_Operations/01_Turnover_Business_Rules.md) | 流水要求、遊戲權重 |
| [遊戲整合需求（Game Integration Requirements）](03_Gaming_Operations/02_Game_Integration_Requirements.md) | 供應商標準、認證檢查表 |
| [遊戲整合標準（Game Integration Standards）](03_Gaming_Operations/03_Game_Integration_Standards.md) | 供應商 API 標準、整合政策 |
| [遊戲大廳需求（Game Lobby Requirements）](03_Gaming_Operations/04_Game_Lobby_Requirements.md) | 大廳組織、分類、推薦 |

### 04 促銷與 VIP（Promotions & VIP）（3 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [紅利計算需求（Bonus Calculation Requirements）](04_Promotions_VIP/01_Bonus_Calculation_Requirements.md) | 紅利類型、資格、條款 |
| [活動風險需求（Activity Risk Requirements）](04_Promotions_VIP/02_Activity_Risk_Requirements.md) | 紅利濫用政策、配對投注檢測 |
| [促銷需求（Promotion Requirements）](04_Promotions_VIP/03_Promotion_Requirements.md) | 流水遊戲權重、VIP 等級規則 |

### 05 風險與合規（Risk & Compliance）（11 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [風險策略概覽（Risk Strategy Overview）](05_Risk_Compliance/01_Risk_Strategy_Overview.md) | 欺詐類型、KPI、行業背景 |
| [KYC/AML 需求](05_Risk_Compliance/03_KYC_AML_Requirements.md) | 驗證等級、法規 |
| [欺詐檢測需求（Fraud Detection Requirements）](05_Risk_Compliance/04_Fraud_Detection_Requirements.md) | 檢測政策、優先級 |
| [風險提案需求（Risk Proposal Requirements）](05_Risk_Compliance/07_Risk_Proposal_Requirements.md) | 審批工作流程、SLA 政策 |
| [負擔能力需求（Affordability Requirements）](05_Risk_Compliance/09_Affordability_Requirements.md) | 負擔能力評分、收入評估 |
| [玩家保護需求（Player Protection Requirements）](05_Risk_Compliance/08_Player_Protection_Requirements.md) | 自我排除、限額管理、GAMSTOP |
| [ML 需求](05_Risk_Compliance/06_ML_Requirements.md) | ML 模型政策、漂移監控 |
| [司法管轄區框架需求（Jurisdiction Framework Requirements）](05_Risk_Compliance/11_Jurisdiction_Framework_Requirements.md) | 多司法管轄區政策、監管路由 |
| [檢測模型規範（Detection Model Spec）](05_Risk_Compliance/05_Detection_Model_Spec.md) | 檢測類別、規則閾值 |
| [風險需求摘要（Risk Requirements Summary）](05_Risk_Compliance/02_Risk_Requirements_Summary.md) | 規則引擎政策、升級程序 |
| [投注額驗證需求（Turnover Validation Requirements）](05_Risk_Compliance/10_Turnover_Validation_Requirements.md) | 檢查點快照規則、雙層保護 |

### 06 治理與授權（Governance & Licensing）（6 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [多租戶需求（Multi-Tenant Requirements）](06_Governance_Licensing/01_Multi_Tenant_Requirements.md) | 層級結構、數據隔離、計費 |
| [MFA 需求](06_Governance_Licensing/04_MFA_Requirements.md) | 驗證政策、合規 |
| [MFA 恢復需求（MFA Recovery Requirements）](06_Governance_Licensing/06_MFA_Recovery_Requirements.md) | 恢復程序、備份驗證 |
| [MFA 合規需求（MFA Compliance Requirements）](06_Governance_Licensing/05_MFA_Compliance_Requirements.md) | 審計追蹤、合規測試 |
| [MFA 架構規範（MFA Architecture Spec）](06_Governance_Licensing/03_MFA_Architecture_Spec.md) | MFA 設計需求、驗證器政策 |
| [治理需求（Governance Requirements）](06_Governance_Licensing/02_Governance_Requirements.md) | RBAC 政策、審計日誌、多租戶治理 |

### 07 代理運營（Agent Operations）（2 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [信用網絡需求（Credit Network Requirements）](07_Agent_Operations/01_Credit_Network_Requirements.md) | 信用額度、結算週期、業務規則 |
| [代理系統需求（Agent System Requirements）](07_Agent_Operations/02_Agent_System_Requirements.md) | 代理層級、佣金規則、招募 |

### 08 分析運營（Analytics Operations）（2 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [報告需求（Reporting Requirements）](08_Analytics_Operations/01_Reporting_Requirements.md) | 業務報告、KPI 定義、數據維度 |
| [BI 儀表板需求（BI Dashboard Requirements）](08_Analytics_Operations/02_BI_Dashboard_Requirements.md) | 儀表板需求、指標定義 |

### 09 基礎設施需求（Infrastructure Requirements）（2 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [QA 標準需求（QA Standards Requirements）](09_Infrastructure_Requirements/01_QA_Standards_Requirements.md) | QA 驗收標準、測試覆蓋率 |
| [成本優化需求（Cost Optimization Requirements）](09_Infrastructure_Requirements/02_Cost_Optimization_Requirements.md) | 成本控制目標、預算約束 |

### 10 平台運營（Platform Operations）（3 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [租戶配置需求（Tenant Configuration Requirements）](10_Platform_Operations/01_Tenant_Configuration_Requirements.md) | 白標配置、租戶自定義 |
| [通知需求（Notification Requirements）](10_Platform_Operations/02_Notification_Requirements.md) | 通知場景、渠道優先級 |
| [數據管道需求（Data Pipeline Requirements）](10_Platform_Operations/03_Data_Pipeline_Requirements.md) | 數據報告需求、ETL 規則、SLA |

### 11 前端體驗（Frontend Experience）（4 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [前端 UX 需求（Frontend UX Requirements）](11_Frontend_Experience/01_Frontend_UX_Requirements.md) | 頁面佈局、橫幅規則 |
| [SEO 性能需求（SEO Performance Requirements）](11_Frontend_Experience/03_SEO_Performance_Requirements.md) | SEO 目標、性能 SLA |
| [移動應用需求（Mobile App Requirements）](11_Frontend_Experience/02_Mobile_App_Requirements.md) | 移動功能、平台支援 |
| [本地化需求（Localization Requirements）](11_Frontend_Experience/04_Localization_Requirements.md) | 多語言、翻譯工作流程 |

### 12 安全合規（Security Compliance）（3 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [數據保護需求（Data Protection Requirements）](12_Security_Compliance/01_Data_Protection_Requirements.md) | GDPR 合規、數據刪除政策 |
| [合規標準需求（Compliance Standards Requirements）](12_Security_Compliance/02_Compliance_Standards_Requirements.md) | ISO 27001、UK RTS 合規 |
| [支付安全需求（Payment Security Requirements）](12_Security_Compliance/03_Payment_Security_Requirements.md) | 支付限制、數據可攜性 |

### 13 客戶服務（Customer Service）（2 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [客服平台需求（CS Platform Requirements）](13_Customer_Service/01_CS_Platform_Requirements.md) | 客服平台、玩家 360 視圖、工單分類 |
| [客服運營需求（CS Operations Requirements）](13_Customer_Service/02_CS_Operations_Requirements.md) | 客服運營、SLA 指標、排班 |

### 14 整合標準（Integration Standards）（1 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [第三方整合需求（Third Party Integration Requirements）](14_Integration_Standards/01_Third_Party_Integration_Requirements.md) | 第三方商業條件、SLA |

### 15 負責任博彩（Responsible Gambling）（4 份文檔）

| 文件 | 關鍵主題 |
|----------|------------|
| [自我排除需求（Self-Exclusion Requirements）](15_Responsible_Gambling/01_Self_Exclusion_Requirements.md) | 自我排除類型、監管要求 |
| [存款限額需求（Deposit Limits Requirements）](15_Responsible_Gambling/02_Deposit_Limits_Requirements.md) | 存款限額、虧損限額、業務規則 |
| [會話保護需求（Session Protection Requirements）](15_Responsible_Gambling/03_Session_Protection_Requirements.md) | 冷靜期、會話管理、現實檢查 |
| [負擔能力需求（Affordability Requirements）](15_Responsible_Gambling/04_Affordability_Requirements.md) | 玩家保護 API、負擔能力評估 |

---

## 按角色快速連結（Quick Links by Role）

### 高階主管（For Executives）
- [風險策略概覽（Risk Strategy Overview）](05_Risk_Compliance/01_Risk_Strategy_Overview.md) - 欺詐成本、KPI、供應商比較
- [玩家生命週期（Player Lifecycle）](01_Player_Experience/04_Player_Lifecycle.md) - 玩家旅程和互動
- [平台概覽（Platform Overview）](01_Player_Experience/01_Platform_Overview.md) - 平台功能概覽

### 合規官（For Compliance Officers）
- [自我排除需求（Self-Exclusion Requirements）](15_Responsible_Gambling/01_Self_Exclusion_Requirements.md) - 自我排除類型
- [KYC/AML 需求](05_Risk_Compliance/03_KYC_AML_Requirements.md) - 驗證程序
- [合規標準需求（Compliance Standards Requirements）](12_Security_Compliance/02_Compliance_Standards_Requirements.md) - ISO 27001、UK RTS
- [數據保護需求（Data Protection Requirements）](12_Security_Compliance/01_Data_Protection_Requirements.md) - GDPR 合規

### 產品經理（For Product Managers）
- [紅利計算需求（Bonus Calculation Requirements）](04_Promotions_VIP/01_Bonus_Calculation_Requirements.md) - 紅利規則
- [支付運營（Payment Operations）](02_Financial_Operations/03_Payment_Operations.md) - 支付方式
- [信用網絡需求（Credit Network Requirements）](07_Agent_Operations/01_Credit_Network_Requirements.md) - 代理信用系統
- [BI 儀表板需求（BI Dashboard Requirements）](08_Analytics_Operations/02_BI_Dashboard_Requirements.md) - 分析儀表板

---

## 相關文檔（Related Documentation）

- **技術實施（Technical Implementation）**: [../architecture/](../architecture/) - 供架構師和開發者使用
- **已歸檔來源（Archived Source）**: [../source-archive/](../source-archive/) - 原始 SSOT 文檔（只讀）
- **主索引（Main Index）**: [../README.md](../README.md) - 導航中心

---

**狀態（Status）**: Phase 9 完成 - 15 個類別共 66 份文檔
**驗證（Validation）**: 需求文檔中無代碼區塊（已透過 `validate-requirements-purity.sh` 驗證）
