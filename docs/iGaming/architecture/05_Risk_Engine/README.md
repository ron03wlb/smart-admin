# 05 風控引擎（Risk Engine）

> **目標讀者**: 架構師、後端開發、DevOps
> **狀態**: Phase 6 完成 — 10 份拆分文檔 + 來源索引
> **業務需求**: [風控合規需求](../../requirements/05_Risk_Compliance/README.md)

---

## 拆分文檔

| 文件 | 說明 | 來源 |
|------|------|------|
| [風控系統架構](01_Risk_System_Architecture.md) | 事件驅動風控管線、Kafka/Flink 整合、ML 模型服務 | [來源](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) |
| [KYC 驗證 API](06_KYC_Verification_API.md) | 身份驗證服務、資料庫結構、API 端點 | [來源](../../source-archive/05_Risk_Control/05-03_KYC_AML.md) |
| [詐騙偵測系統](05_Fraud_Detection_System.md) | ML 偵測模型、規則引擎、SpotBugs 整合 | [來源](../../source-archive/05_Risk_Control/05-02_Fraud_Detection.md) |
| [風控提案實作](07_Risk_Proposal_Implementation.md) | 優先級計算服務、SLA 監控、Camunda BPMN 工作流程、Sa-Token 權限 | [來源](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| [可負擔性評估實作](09_Affordability_Implementation.md) | 可負擔性評分演算法、API 端點、數據整合、監控 | [來源](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| [玩家保護 API](08_Player_Protection_API.md) | 自我排除服務、限額管理 API、會話追蹤、GAMSTOP 整合 | [來源](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| [ML 整合架構](04_ML_Integration_Architecture.md) | ML 訓練管線、模型部署、A/B 測試、漂移監控 | [來源](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) |
| [偵測模型實作](03_Detection_Model_Implementation.md) | 五層偵測架構、Kafka 管線、TCC 交易、規則執行 | [來源](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md) |
| [有效投注額驗證架構](10_Turnover_Validation_Architecture.md) | 快照 SQL 結構、驗證服務、雙層架構、索引策略 | [來源](../../source-archive/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |
| [風控實作指南](02_Risk_Implementation.md) | 規則引擎架構（Drools/LiteFlow）、風險評分計算、ML 整合 | [來源](../../source-archive/00_Foundation/guides/00-14_Risk_Implementation.md) |

## 來源索引

| 文件 | 說明 | 來源 |
|------|------|------|
| 偵測模型 | ML 偵測模型架構 | [來源](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md) |
| 規則配置 | 風控規則引擎配置 | [來源](../../source-archive/05_Risk_Control/05-02-02_Rule_Configuration.md) |
| ML 整合 | ML 模型訓練與部署管線 | [來源](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) |
| 營運工具 | 風控營運工具 | [來源](../../source-archive/05_Risk_Control/05-02-04_Operations_Tools.md) |
| 風控提案工作流程 | 風控提案審批系統 | [來源](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| 有效投注額驗證 | 有效投注額驗證架構 | [來源](../../source-archive/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |

---

**最後更新**: 2026-02-08
