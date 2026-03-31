# 02 金融服務（Finance Service）

> **目標讀者（Audience）**: 架構師、後端開發人員、DevOps
> **狀態（Status）**: 10 個架構文件
> **最後更新**: 2026-03-31

---

## 無縫錢包

| 文件 | 描述 |
|------|------|
| [無縫錢包索引](01_Seamless_Wallet_Index.md) | 無縫錢包架構總索引 |
| [無縫錢包分析](02_Seamless_Wallet_Analysis.md) | API 規格、並發控制、狀態機、異常處理 |
| [無縫錢包技術實作](03_Seamless_Wallet_Technical.md) | 6 步原子操作、3 層並發控制、SAGA 補償 |
| [無縫錢包 API 規格](seamless-wallet-api-spec.md) | Seamless Wallet API 完整規格 |

## 金融核心

| 文件 | 描述 |
|------|------|
| [金融實作架構](04_Financial_Implementation.md) | 錢包系統架構、支付閘道 API、風險評分引擎、SAGA 編排器 |
| [支付閘道 API](05_Payment_Gateway_API.md) | PSP API 整合、Webhook、回呼處理 |
| [支付閘道技術](06_Payment_Gateway_Technical.md) | 支付技術實作細節 |
| [對帳技術實現](07_Reconciliation_Technical.md) | 三方比對引擎、定時對帳任務、資料模型 |

## 有效投注額（Valid Turnover）

| 文件 | 描述 |
|------|------|
| [有效投注額架構](08_Turnover_Architecture.md) | 三層驗證架構、業務邏輯公式、狀態因子定義（SSOT） |
| [有效投注額實作](09_Turnover_Implementation.md) | 錢包扣款演算法、LockAmount、流程圖、時序圖 |

> **XREF（交叉引用）**: 有效投注業務規則 → [requirements/03_Gaming_Operations/01_Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/01_Turnover_Business_Rules.md)

---

## 歸檔參考（source-archive）

| 文件 | 描述 |
|------|------|
| [錢包架構](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) | 多幣種錢包設計（唯讀歸檔） |
| [交易處理流程](../../source-archive/02_Finance_Center/02-07_Transaction_Processing_Flow.md) | 端到端交易生命週期（唯讀歸檔） |
