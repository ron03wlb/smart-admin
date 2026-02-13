# 05 風險與合規（Risk Compliance）

> **受眾（Audience）**: 高階主管、產品經理、合規官
> **狀態（Status）**: Phase 6 完成 - 11 份拆分文件 + 來源索引

---

## 拆分文件（Split Documents）

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| [風險策略概覽（Risk Strategy Overview）](01_Risk_Strategy_Overview.md) | 欺詐類型、行業 KPI、供應商比較、合規概覽 | [source](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) |
| [KYC/AML 需求](03_KYC_AML_Requirements.md) | 驗證等級、監管要求、程序 | [source](../../source-archive/05_Risk_Control/05-03_KYC_AML.md) |
| [欺詐檢測需求（Fraud Detection Requirements）](04_Fraud_Detection_Requirements.md) | 檢測政策、優先級、規則定義 | [source](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) |
| [風險提案需求（Risk Proposal Requirements）](07_Risk_Proposal_Requirements.md) | 優先級分類、SLA 要求、決策類型、補償規則 | [source](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| [負擔能力需求（Affordability Requirements）](09_Affordability_Requirements.md) | 負擔能力檢查觸發條件、收入閾值、UKGC/MGA 強制要求 | [source](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| [玩家保護需求（Player Protection Requirements）](08_Player_Protection_Requirements.md) | 自我排除（Self-Exclusion）、存款/虧損限額、會話管理、負責任博彩 API | [source](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| [ML 需求](06_ML_Requirements.md) | ML 模型治理、訓練資料要求、A/B 測試政策 | [source](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) |
| [司法管轄區框架需求（Jurisdiction Framework Requirements）](11_Jurisdiction_Framework_Requirements.md) | 多司法管轄區合規、按司法管轄區的 KYC/支付/遊戲限制 | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |
| [檢測模型規範（Detection Model Spec）](05_Detection_Model_Spec.md) | 檢測政策、模型類型、風險閾值、行業基準 | [source](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md) |
| [有效投注額驗證需求（Turnover Validation Requirements）](10_Turnover_Validation_Requirements.md) | 檢查點快照規則、雙層保護、無效投注額規則 | [source](../../source-archive/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |
| [風險需求摘要（Risk Requirements Summary）](02_Risk_Requirements_Summary.md) | 檢測類別、升級程序、欺詐類別、信用監控 | [source](../../source-archive/00_Foundation/guides/00-14_Risk_Implementation.md) |

## 來源索引（Source Index）

### KYC/AML 合規

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| KYC/AML | 身份驗證 (KYC)、反洗錢 (AML) 程序 | [source](../../source-archive/05_Risk_Control/05-03_KYC_AML.md) |
| 制裁篩查 (Sanctions Screening) | 政治敏感人物和制裁名單篩選 | [source](../../source-archive/05_Risk_Control/05-02-08_Sanctions_Screening.md) |
| AML 報告對帳 (AML Report Reconciliation) | AML 報告和對帳 | [source](../../source-archive/05_Risk_Control/05-04_AML_Report_Reconciliation.md) |
| SAR 財務整合 (SAR Finance Integration) | 可疑活動報告 (SAR) 財務整合 | [source](../../source-archive/05_Risk_Control/05-05_SAR_Finance_Integration.md) |

### 監管合規

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| UKGC 合規 (UKGC Compliance) | 英國博彩委員會監管要求 | [source](../../source-archive/06_Platform_Governance/06-08_UKGC_Compliance.md) |
| MGA 合規 (MGA Compliance) | 馬爾他博彩管理局監管要求 | [source](../../source-archive/06_Platform_Governance/06-09_MGA_Compliance.md) |
| 多司法管轄區框架 (Multi-Jurisdiction Framework) | 跨司法管轄區合規管理 | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |

### 負責任博彩

| 文件 | 說明 | 來源 |
|----------|-------------|--------|
| 自我排除 (Self-Exclusion) | 玩家自我排除計畫、GAMSTOP 整合 | [source](../../source-archive/15_Responsible_Gambling/15-01_Self_Exclusion.md) |
| 存款限額 (Deposit Limits) | 存款限額設定和執行 | [source](../../source-archive/15_Responsible_Gambling/15-02_Deposit_Limits.md) |
| 冷靜期 (Cooling Off Period) | 臨時帳戶限制 | [source](../../source-archive/15_Responsible_Gambling/15-03_Cooling_Off_Period.md) |
| 會話管理 (Session Management) | 遊戲會話時間限制 | [source](../../source-archive/15_Responsible_Gambling/15-04_Session_Management.md) |
| 現實檢查 (Reality Checks) | 遊戲中現實檢查通知 | [source](../../source-archive/15_Responsible_Gambling/15-05_Reality_Checks.md) |
| 虧損限額 (Loss Limits) | 虧損限額配置和執行 | [source](../../source-archive/15_Responsible_Gambling/15-06_Loss_Limits.md) |
| 玩家保護 API (Player Protection API) | 負責任博彩 API 整合 | [source](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| 負擔能力評估 (Affordability Assessment) | 玩家負擔能力檢查 | [source](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| 自我排除對帳 (Self-Exclusion Reconciliation) | 自我排除名單同步 | [source](../../source-archive/15_Responsible_Gambling/15-09_Self_Exclusion_Reconciliation.md) |

---

**最後更新（Last Updated）**: 2026-02-12
