# 按任務導航索引

> **⚠️ v5.0 更新 (2026-03-24)**：文檔已重整為 [requirements-v2/](../../requirements-v2/) 和 [technical-v2/](../../technical-v2/)，請優先使用新版文檔。以下為舊版導航，保留供參考。

> 根據常見工作任務快速定位相關文檔。

---

## 開發任務

### 新增一個遊戲供應商 (GP) 整合

| 步驟 | 參考文檔 |
|------|---------|
| 了解整合模式 | `architecture/03_Game_Integration/01_Game_Integration_Protocols.md` |
| 實作 Adapter | `architecture/03_Game_Integration/02_Game_Integration_Implementation.md` |
| 安全四層防禦 | `architecture/03_Game_Integration/03_Game_Integration_Security.md` |
| 投注額計算邏輯 | `architecture/03_Game_Integration/04_Turnover_Calculation_Logic.md` |
| 業務規則 | `requirements/03_Gaming_Operations/01_Turnover_Business_Rules.md` |
| GP API 調研 | `research/game-provider-api-research.md` |

### 新增一個支付供應商 (PSP)

| 步驟 | 參考文檔 |
|------|---------|
| PSP Adapter 模式 | `architecture/02_Finance_Service/05_Payment_Gateway_API.md` |
| 技術實作 | `architecture/02_Finance_Service/06_Payment_Gateway_Technical.md` |
| 對帳機制 | `architecture/02_Finance_Service/07_Reconciliation_Technical.md` |
| 業務需求 | `requirements/02_Financial_Operations/` |

### 實作一個新的風控規則

| 步驟 | 參考文檔 |
|------|---------|
| 風控架構總覽 | `architecture/05_Risk_Engine/01_Risk_System_Architecture.md` |
| LiteFlow 規則實作 | `architecture/05_Risk_Engine/02_Risk_Implementation.md` |
| ML 模型整合 | `architecture/05_Risk_Engine/04_ML_Integration_Architecture.md` |
| 業務需求 | `requirements/05_Risk_Compliance/` |

### 修改錢包相關邏輯

| 步驟 | 參考文檔 |
|------|---------|
| 錢包索引頁 | `architecture/02_Finance_Service/01_Seamless_Wallet_Index.md` |
| 技術實作 | `architecture/02_Finance_Service/03_Seamless_Wallet_Technical.md` |
| 設計文檔 | `implementation/01-wallet-design.md` |
| 併發控制 (三層) | 技術架構總覽 § 四 |
| 冪等防禦 (ADR-015) | 技術架構總覽 § 四.6 |

---

## 營運任務

### 上線一個新租戶/品牌

| 步驟 | 參考文檔 |
|------|---------|
| 多租戶需求 | `requirements/06_Governance_Licensing/01_Multi_Tenant_Requirements.md` |
| 多租戶架構 | `architecture/06_Platform_Core/01_Multi_Tenant_Architecture.md` |
| 租戶配置管理 | `architecture/10_Platform_Management/` |
| 計費模式 | 業務需求總覽 § 七.1 |

### 處理對帳差異

| 步驟 | 參考文檔 |
|------|---------|
| 三層對帳機制 | 業務需求總覽 § 三.3 |
| 對帳技術實作 | `architecture/02_Finance_Service/07_Reconciliation_Technical.md` |
| 差異處理 + 升級路徑 | 業務需求總覽 § 三.3 |

### 處理玩家自我排除

| 步驟 | 參考文檔 |
|------|---------|
| 自我排除需求 | `requirements/15_Responsible_Gambling/01_Self_Exclusion_Requirements.md` |
| 技術實作 | `architecture/15_Responsible_Gambling/` |
| Gamstop/CRUKS 整合 | 需求文檔內各牌照要求章節 |

---

## 合規任務

### 準備 UKGC 牌照申請

| 類別 | 參考文檔 |
|------|---------|
| KYC/AML | `requirements/05_Risk_Compliance/03_KYC_AML_Requirements.md` |
| 負責任博彩 | `requirements/15_Responsible_Gambling/` (全部 4 份) |
| 可負擔性評估 | 業務需求總覽 § 十四.2 |
| 環境分離 | `architecture/12_Security/` |
| 資料保護 | `requirements/12_Security_Compliance/01_Data_Protection_Requirements.md` |

### 準備 PCI-DSS 審計

| 類別 | 參考文檔 |
|------|---------|
| 加密架構 | `architecture/12_Security/` |
| 支付安全 | `requirements/12_Security_Compliance/` |
| 當前覆蓋率 | 技術架構總覽 § 九.4 (67%) |

---

## 品質與測試任務

### 執行 Phase 驗收測試

| 步驟 | 參考文檔 |
|------|---------|
| QA 標準 | `requirements/09_Infrastructure_Requirements/01_QA_Standards_Requirements.md` |
| 測試環境配置 | `Sprint-3-Test-Environment-TODO.md` |
| 過往測試報告 | `testing/turnover-engine-test-coverage-report.md` |
| Phase 1.5 驗收範例 | `reports/PHASE_1.5_COMPLETION_REPORT.md` |
| 品質門報告 | `quality-reports/2026-Q1-quality-gate-report.md` |

---

## 文檔維護任務

### 新增/修改文檔

| 任務 | 參考文檔 |
|------|---------|
| 架構文檔模板 | `TEMPLATE_ARCHITECTURE.md` |
| 需求文檔模板 | `TEMPLATE_REQUIREMENTS.md` |
| ADR 模板 | `TEMPLATE_ADR.md` |
| 術語對照表 | `TRANSLATION_GLOSSARY.md` |
| 品質標準 | `quality-reports/2026-Q1-quality-gate-report.md` |
