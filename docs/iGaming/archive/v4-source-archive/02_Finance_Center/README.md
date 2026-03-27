# 02_Finance_Center - 財務中心

> **模塊定位**: 資金流轉與對帳核心
> **三層風控架構**: Layer 2 - 狀態因子計算
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

支付閘道、對帳系統、流水計算、帳務處理、錢包架構。

**核心功能**：
- 支付閘道整合（存款/提款）
- 財務對帳與差異處理
- 流水計算與遊戲對帳（Layer 2）
- 統一錢包架構

**職責邊界**：
- ✅ 包含：資金流轉、對帳、流水計算
- ❌ 不包含：活動獎金發放（04_Activity_Center）、遊戲整合（03_Game_Center）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 |
|------|---------|------|--------|
| 02-02 | [Payment_Gateway_Integration.md](02-02_Payment_Gateway_Integration.md) | 支付閘道整合 | P0 |
| 02-03 | [Reconciliation_System.md](02-03_Reconciliation_System.md) | 財務對帳系統 | P0 |
| 02-04 | [Turnover_and_Game_Reconciliation_Analysis.md](02-04_Turnover_and_Game_Reconciliation_Analysis.md) | 流水計算與遊戲對帳（Layer 2）| P0 |
| 02-05 | [Billing_&_Invoicing.md](02-05_Billing_&_Invoicing.md) | 帳單與發票 | P2 |
| 02-06 | [Wallet_Architecture.md](02-06_Wallet_Architecture.md) | 統一錢包架構 | P0 |
| 02-07 | [Transaction_Processing_Flow.md](02-07_Transaction_Processing_Flow.md) | 交易處理流程 | P1 |
| 02-08 | [Player_Funds_Segregation.md](02-08_Player_Funds_Segregation.md) | 玩家資金隔離（UKGC/MGA 合規）| P0 |
| 02-09 | [Refund_Dispute_Management.md](02-09_Refund_Dispute_Management.md) | 退款與爭議管理 | P1 |
| 02-10 | [Crypto_Travel_Rule.md](02-10_Crypto_Travel_Rule.md) | 加密貨幣旅行規則 | P1 |
| 02-11 | [GGR_Tax_Reconciliation.md](02-11_GGR_Tax_Reconciliation.md) | GGR 稅務對帳 | P0 |
| 02-12 | [Affiliate_Commission_Reconciliation.md](02-12_Affiliate_Commission_Reconciliation.md) | 代理佣金對帳 | P1 |
| 02-13 | [Service_Fee_Reconciliation.md](02-13_Service_Fee_Reconciliation.md) | 第三方服務費對帳 | P1 |
| 02-14 | [Currency_Reconciliation.md](02-14_Currency_Reconciliation.md) | 多幣種匯率對帳 | P2 | 🆕

---

## 🔗 核心依賴

```text
02_Finance_Center (Layer 2)
    ↓
    ├─► 05_Risk_Control (05-01) - Layer 1 風控驗證
    ├─► 04_Activity_Center (04-02) - Layer 3 遊戲權重
    ├─► 03_Game_Center (03-03) - Seamless Wallet 整合
    └─► 06_Platform_Governance (06-03) - 審計日誌
```

---

## 🔑 三層風控架構位置

```text
ValidTurnover = BetAmount
                × Layer1_RiskFactor     (05-01: 0 or 1)
                × Layer2_StatusFactor   (本模塊 02-04: 0%, 50%, 100%)
                × Layer3_GameWeight     (04-02: 5%-100%)
```

**Layer 2 狀態因子定義**：
| 結果狀態 | 狀態因子 | 說明 |
|---------|---------|------|
| WIN/LOSS | 100% | 正常結算 |
| VOID/CANCEL | 0% | 無效投注 |
| CASHOUT | 50% | 提前結算 |

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 流水計算規則 | 02-04 Turnover_Analysis | §3.1 |
| 錢包類型定義 | 02-06 Wallet_Architecture | §2.1 |
| 對帳差異處理 | 02-03 Reconciliation | §4.2 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Finance Team
