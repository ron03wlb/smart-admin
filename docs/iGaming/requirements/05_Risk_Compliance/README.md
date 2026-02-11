# 05 風險與合規（Risk Compliance）

> **受眾（Audience）**: 高階主管、產品經理、合規官
> **狀態（Status）**: Phase 6 完成 - 11 份拆分文檔 + 來源索引

---

## 拆分文檔（Split Documents）

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| [風險策略概覽（Risk Strategy Overview）](Risk_Strategy_Overview.md) | 欺詐類型、行業 KPI、供應商比較、合規概覽 | [source](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) |
| [KYC/AML 需求](KYC_AML_Requirements.md) | 驗證等級、監管要求、程序 | [source](../../source-archive/05_Risk_Control/05-03_KYC_AML.md) |
| [欺詐檢測需求（Fraud Detection Requirements）](Fraud_Detection_Requirements.md) | 檢測政策、優先級、規則定義 | [source](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) |
| [風險提案需求（Risk Proposal Requirements）](Risk_Proposal_Requirements.md) | 優先級分類、SLA 要求、決策類型、補償規則 | [source](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| [負擔能力需求（Affordability Requirements）](Affordability_Requirements.md) | 負擔能力檢查觸發條件、收入閾值、UKGC/MGA 強制要求 | [source](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| [玩家保護需求（Player Protection Requirements）](Player_Protection_Requirements.md) | 自我排除、存款/虧損限額、會話管理、負責任博彩 API | [source](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| [ML 需求](ML_Requirements.md) | ML 模型治理、訓練資料要求、A/B 測試政策 | [source](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) |
| [司法管轄區框架需求（Jurisdiction Framework Requirements）](Jurisdiction_Framework_Requirements.md) | 多司法管轄區合規、按司法管轄區的 KYC/支付/遊戲限制 | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |
| [檢測模型規範（Detection Model Spec）](Detection_Model_Spec.md) | 檢測政策、模型類型、風險閾值、行業基準 | [source](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md) |
| [投注額驗證需求（Turnover Validation Requirements）](Turnover_Validation_Requirements.md) | 檢查點快照規則、雙層保護、無效投注額規則 | [source](../../source-archive/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |
| [風險需求摘要（Risk Requirements Summary）](Risk_Requirements_Summary.md) | 檢測類別、升級程序、欺詐類別、信用監控 | [source](../../source-archive/00_Foundation/guides/00-14_Risk_Implementation.md) |

## 來源索引（Source Index）

### KYC/AML 合規

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| KYC AML | 了解你的客戶、反洗錢程序 | [source](../../source-archive/05_Risk_Control/05-03_KYC_AML.md) |
| Sanctions Screening | 政治敏感人物和制裁名單篩選 | [source](../../source-archive/05_Risk_Control/05-02-08_Sanctions_Screening.md) |
| AML Report Reconciliation | AML 報告和對帳 | [source](../../source-archive/05_Risk_Control/05-04_AML_Report_Reconciliation.md) |
| SAR Finance Integration | 可疑活動報告財務整合 | [source](../../source-archive/05_Risk_Control/05-05_SAR_Finance_Integration.md) |

### 監管合規

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| UKGC Compliance | 英國博彩委員會監管要求 | [source](../../source-archive/06_Platform_Governance/06-08_UKGC_Compliance.md) |
| MGA Compliance | 馬爾他博彩管理局監管要求 | [source](../../source-archive/06_Platform_Governance/06-09_MGA_Compliance.md) |
| Multi-Jurisdiction Framework | 跨司法管轄區合規管理 | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |

### 負責任博彩

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| Self-Exclusion | 玩家自我排除計畫、GAMSTOP 整合 | [source](../../source-archive/15_Responsible_Gambling/15-01_Self_Exclusion.md) |
| Deposit Limits | 存款限額設定和執行 | [source](../../source-archive/15_Responsible_Gambling/15-02_Deposit_Limits.md) |
| Cooling Off Period | 臨時帳戶限制 | [source](../../source-archive/15_Responsible_Gambling/15-03_Cooling_Off_Period.md) |
| Session Management | 遊戲會話時間限制 | [source](../../source-archive/15_Responsible_Gambling/15-04_Session_Management.md) |
| Reality Checks | 遊戲中現實檢查通知 | [source](../../source-archive/15_Responsible_Gambling/15-05_Reality_Checks.md) |
| Loss Limits | 虧損限額配置和執行 | [source](../../source-archive/15_Responsible_Gambling/15-06_Loss_Limits.md) |
| Player Protection API | 負責任博彩 API 整合 | [source](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| Affordability Assessment | 玩家負擔能力檢查 | [source](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| Self-Exclusion Reconciliation | 自我排除名單同步 | [source](../../source-archive/15_Responsible_Gambling/15-09_Self_Exclusion_Reconciliation.md) |

---

**最後更新（Last Updated）**: 2026-02-08
