# 02 金融服務（Finance Service）

> **目標讀者（Audience）**: 架構師、後端開發人員、DevOps
> **狀態（Status）**: Phase 6 完成 - 8 個拆分文檔 + 來源索引

---

## 拆分文檔

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| [支付閘道 API](05_Payment_Gateway_API.md) | 支付服務提供商 API 整合、Webhook、回呼處理 | [來源](../../source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md) |
| [對帳技術實現](07_Reconciliation_Technical.md) | 三方比對引擎、定時對帳任務、資料模型 | [來源](../../source-archive/02_Finance_Center/02-03_Reconciliation_System.md) |
| [金融實作架構](04_Financial_Implementation.md) | 錢包系統架構、支付閘道 API、風險評分引擎、SAGA 編排器 | [來源](../../source-archive/00_Foundation/guides/00-11_Financial_Implementation.md) |
| [有效投注流程圖](11_Turnover_Flowcharts.md) | 有效投注驗證流程圖、遊戲權重應用、投注生命週期狀態機 | [來源](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md) |
| [有效投注計算架構](08_Turnover_Calculation_Architecture.md) | 三層驗證實作、事件驅動資料交換、SmartAdmin 映射 | [來源](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) |
| [有效投注實作](10_Turnover_Implementation.md) | 錢包扣款演算法、有效投注金額計算、鎖定金額狀態轉換 | [來源](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-03_Implementation_Details.md) |
| [無縫錢包分析](02_Seamless_Wallet_Analysis.md) | 無縫錢包 API 規格、並發控制、狀態機、異常處理 | [來源](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) |
| [有效投注計算邏輯詳解](09_Turnover_Calculation_Logic_Detail.md) | 有效投注金額公式、鎖定金額生命週期、返水計算、提款流水 | [來源](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-02_Calculation_Logic.md) |

## 核心架構文檔（來源索引）

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| [錢包架構](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) | 多幣種錢包設計、餘額管理、帳本結構 | [來源](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) |
| [交易處理流程](../../source-archive/02_Finance_Center/02-07_Transaction_Processing_Flow.md) | 端到端交易生命週期與狀態機 | [來源](../../source-archive/02_Finance_Center/02-07_Transaction_Processing_Flow.md) |

## 無縫錢包技術文檔

| 文檔 | 描述 | 來源 |
|----------|-------------|--------|
| [並發控制](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) | 樂觀鎖定、競態條件處理、分佈式鎖 | [來源](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) |
| [恢復機制](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) | 交易回滾、補償、故障恢復 | [來源](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) |
| [會計整合](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) | 複式記帳與總帳整合 | [來源](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) |

## 附加索引

| 文檔 | 描述 |
|----------|-------------|
| [無縫錢包索引](./01_Seamless_Wallet_Index.md) | 無縫錢包架構詳細文檔 |

---

**最後更新（Last Updated）**: 2026-02-12
