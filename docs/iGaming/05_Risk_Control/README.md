# 05_Risk_Control - 風控中心

> **模塊定位**: 風險管理與合規核心
> **三層風控架構**: Layer 1 - 風控驗證（SSOT）
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

風控框架、欺詐檢測、KYC/AML、代理信用風控、提款風控關聯。

**核心功能**：
- 三層風控架構基礎（Layer 1）
- 欺詐檢測與多帳號識別
- KYC/AML 合規驗證
- 代理信用風控
- 流水驗證方案

**職責邊界**：
- ✅ 包含：風控規則、欺詐檢測、合規驗證
- ❌ 不包含：提款審核流程（01_Player_Center）、活動風控策略（04_Activity_Center）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 | 行數 |
|------|---------|------|--------|------|
| 05-01 | [Risk_Framework.md](05-01_Risk_Framework.md) | 風控框架（Layer 1 SSOT）| P0 | ~1200 |
| 05-02 | [Fraud_Detection.md](05-02_Fraud_Detection.md) | 欺詐檢測系統 | P0 | **2200** ⚠️ |
| 05-03 | [KYC_AML.md](05-03_KYC_AML.md) | KYC/AML 合規 | P0 | ~900 |
| 05-04 | [Agent_Credit_Risk.md](05-04_Agent_Credit_Risk.md) | 代理信用風控 | P1 | ~600 |
| 05-05 | [Risk_Proposal_Workflow.md](05-05_Risk_Proposal_Workflow.md) | 風控提案工作流 | P2 | ~400 |
| 05-06 | [Withdrawal_Risk_Correlation.md](05-06_Withdrawal_Risk_Correlation.md) | 提款風控關聯 | P1 | ~500 |
| 05-07 | [Turnover_Validation_Scheme.md](05-07_Turnover_Validation_Scheme.md) | 流水驗證方案 | P0 | ~700 |

> ⚠️ **05-02 超大文檔**：建議未來拆分為 3-4 個子文檔

---

## 🔗 三層風控架構

```text
┌─────────────────────────────────────────────────┐
│  Layer 1: Risk Control (05_Risk_Control)        │  ← 本模塊（SSOT）
│  - 風控驗證：RiskFactor = 0 or 1                │
│  - 對沖檢測、多帳號識別、異常行為               │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│  Layer 2: Finance (02_Finance_Center)           │
│  - 狀態因子：StatusFactor = 0%, 50%, 100%       │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│  Layer 3: Activity (04_Activity_Center)         │
│  - 遊戲權重：GameWeight = 5%-100%               │
└─────────────────────────────────────────────────┘
```

**統一流水計算公式**：
```text
ValidTurnover = BetAmount
                × Layer1_RiskFactor     (本模塊 05-01: 0 or 1)
                × Layer2_StatusFactor   (02-04: 0%, 50%, 100%)
                × Layer3_GameWeight     (04-02: 5%-100%)
```

---

## 🔗 核心依賴

```text
05_Risk_Control (Layer 1)
    ↓
    ├─► 01_Player_Center (01-05) - 提款風控
    ├─► 02_Finance_Center (02-04) - Layer 2 整合
    ├─► 04_Activity_Center (04-02) - Layer 3 整合
    └─► 06_Platform_Governance (06-03) - 審計追蹤
```

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| **風控驗證 API** | 05-01 Risk_Framework | §3.1 |
| 欺詐檢測規則 | 05-02 Fraud_Detection | §4.2 |
| KYC 等級定義 | 05-03 KYC_AML | §2.1 |
| 流水驗證邏輯 | 05-07 Turnover_Validation | §3.1 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Risk Team
