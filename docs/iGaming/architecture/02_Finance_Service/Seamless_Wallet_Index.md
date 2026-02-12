# 無縫錢包架構

> **目標讀者**: 架構師、後端開發者
> **業務需求**: [無縫錢包需求](../../requirements/02_Financial_Operations/Seamless_Wallet_Requirements.md)
> **狀態**: 索引已建立 - 連結至 source-archive/

---

## 技術架構文檔

| 文檔 | 說明 | 來源 |
|----------|-------------|--------|
| [安全性](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-01_Security.md) | 認證、授權與 API 安全 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-01_Security.md) |
| [併發控制](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) | 樂觀鎖、競爭條件處理與分佈式鎖 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-02_Concurrency.md) |
| [恢復機制](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) | 交易回滾、補償與失敗恢復 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-03_Recovery.md) |
| [會計整合](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) | 複式記帳與 GL 整合 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-04_Accounting.md) |
| [對帳](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-05_Reconciliation.md) | 餘額驗證與差異解決 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-05_Reconciliation.md) |

## 遊戲專屬整合

| 文檔 | 說明 | 來源 |
|----------|-------------|--------|
| [免費旋轉](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-06_Free_Spins.md) | 免費旋轉錢包整合模式 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-06_Free_Spins.md) |
| [輪盤對沖](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-07_Roulette_Hedge.md) | 輪盤對沖投注偵測 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-07_Roulette_Hedge.md) |
| [百家樂和局](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-08_Baccarat_Tie.md) | 百家樂和局投注處理 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-08_Baccarat_Tie.md) |
| [體育投注](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-09_Sports_Betting.md) | 體育投注錢包整合 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-09_Sports_Betting.md) |

## 對帳擴展

| 文檔 | 說明 | 來源 |
|----------|-------------|--------|
| [體育結算](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-10_Sports_Settlement_Reconciliation.md) | 體育投注結算對帳 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-10_Sports_Settlement_Reconciliation.md) |
| [提前兌現對帳](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-11_Cashout_Reconciliation.md) | 提前兌現交易對帳 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-11_Cashout_Reconciliation.md) |
| [投注失敗對帳](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-12_Bet_Failure_Reconciliation.md) | 失敗投注處理與恢復 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-12_Bet_Failure_Reconciliation.md) |
| [回滾鏈](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-13_Rollback_Chain_Reconciliation.md) | 多步驟回滾鏈對帳 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-13_Rollback_Chain_Reconciliation.md) |
| [GP 逾時框架](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-14_GP_Timeout_Framework.md) | 遊戲供應商逾時處理框架 | [source](../../source-archive/02_Finance_Center/seamless-wallet/02-SW-14_GP_Timeout_Framework.md) |

---

**最後更新**: 2026-02-08
