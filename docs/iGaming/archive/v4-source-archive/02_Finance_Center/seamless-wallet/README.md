# 無縫錢包專題總覽

無縫錢包是 iGaming 平台的核心財務系統，本專題深入探討其技術實現和業務邏輯。

**最後更新**: 2026-02-03
**版本**: 4.0.0 (重構版 - 3 層結構)
**狀態**: ✅ 已整合完成

---

## 📋 文檔結構

### 🔴 P0 Core（核心）- 必讀

這些專題涉及系統安全、資金正確性和並發控制，是無縫錢包實作的基礎。

| 文檔 | 關鍵主題 | 重要性 |
|------|----------|--------|
| [01-security.md](core/01-security.md) | **Token 驗證 + 冪等性設計**<br/>- API 身份驗證決策樹、會話過期策略<br/>- 三層防護（Redis→DB→分散式鎖）、防重複扣款 | 🔐 安全 + 💰 資金安全 |
| [02-concurrency.md](core/02-concurrency.md) | **流水並發累積**<br/>- Lua 腳本原子性、TOCTOU 攻擊防護 | 🔒 並發控制 |
| [03-recovery.md](core/03-recovery.md) | **錯誤恢復**<br/>- 異常處理、補償事務、回滾策略 | 🛡️ 容錯性 |

**建議閱讀順序**: 01-security → 02-concurrency → 03-recovery

---

### 🟠 P1 Game Logic（遊戲邏輯）

這些專題涉及特定遊戲邏輯與風控檢測，適合深入實作時參考。

| 文檔 | 關鍵主題 |
|------|----------|
| [sports-betting.md](game-logic/sports-betting.md) | **體育博彩邏輯**<br/>- Valid Bet 計算規則、HALF WIN/LOSS 處理 |
| [free-spins.md](game-logic/free-spins.md) | **免費旋轉流水**<br/>- 老虎機活動流水計算、Bonus 抵扣邏輯 |
| [roulette-hedge.md](game-logic/roulette-hedge.md) | **輪盤對沖檢測**<br/>- 風控檢測算法（覆蓋率分析）、同時下注紅黑檢測 |
| [baccarat-tie.md](game-logic/baccarat-tie.md) | **百家樂平局邏輯**<br/>- 平局下注有效投注邏輯、退款處理 |

---

### 🟡 P1 Finance（財務對帳）

這些專題涉及財務對帳與流水管理，確保資金準確性。

| 文檔 | 關鍵主題 |
|------|----------|
| [accounting.md](finance/accounting.md) | **會計分錄修正**<br/>- 複式記帳調整、借貸平衡驗證 |
| [reconciliation.md](finance/reconciliation.md) | **對帳模型 + 投注要求 + 促銷錢包**<br/>- OLTP vs OLAP 分離設計、數據一致性策略<br/>- 流水要求時序與可追溯性、到期時間管理<br/>- Bonus Wallet → Cash Wallet 邏輯、流水達標驗證 |
| [02-SW-10_Sports_Settlement_Reconciliation.md](02-SW-10_Sports_Settlement_Reconciliation.md) | **體育博彩結算對帳** 🆕<br/>- 賽事結果多源對帳（Official vs Feed）<br/>- Void Bet / Parlay 部分 Void 處理<br/>- IBIA 可疑投注報告對帳 |
| [02-SW-11_Cashout_Reconciliation.md](02-SW-11_Cashout_Reconciliation.md) | **Cashout 結算對帳** 🆕<br/>- Cashout 定價模型差異驗證<br/>- 部分 Cashout 剩餘注單追蹤<br/>- 市場波動補償機制 |

---

## 🔗 相關文檔

### 上層架構

這些文檔提供無縫錢包的整體架構視角：

- **[02-06 統一錢包模型](../02-06_Wallet_Architecture.md)** - 錢包整體架構、可下注餘額公式
- **[02-07 交易處理流程](../02-07_Transaction_Processing_Flow.md)** - 事件驅動架構、Outbox Pattern
- **[02-01 出金風控](../../01_Player_Center/01-05_Withdrawal_Risk.md)** - 多層審核、SAGA 事務

### 遊戲整合

無縫錢包與遊戲提供商（GP）的整合規範：

- **[03-01 遊戲集成標準](../../03_Game_Center/03-01_Game_Integration_Standard.md)** - GP API 規格、HMAC 安全
- **[03-03 無縫錢包分析](../../03_Game_Center/03-03_Seamless_Wallet_Analysis.md)** - 概覽與 GP API 規範

### 風控整合

無縫錢包交易的風控檢查點：

- **[05-01 風控系統](../../05_Risk_Control/05-01_Risk_Framework.md)** - 交易風控檢查、對沖檢測
- **[04_Activity_Center/README.md](../../04_Activity_Center/README.md)** - Bonus 發放與流水要求

---

## 📚 使用指南

### 新手入門

如果您是第一次接觸無縫錢包，建議按以下順序閱讀：

1. **理解整體架構**: [02-06 統一錢包模型](../02-06_Wallet_Architecture.md)
2. **學習安全設計**: [core/01-security.md](core/01-security.md) - Token 驗證 + 冪等性
3. **掌握並發控制**: [core/02-concurrency.md](core/02-concurrency.md) - 流水並發累積
4. **理解錯誤恢復**: [core/03-recovery.md](core/03-recovery.md) - 補償事務

### 進階開發

實作特定功能時的參考路徑：

**實作體育博彩流水計算**:
1. [core/01-security.md](core/01-security.md) - 確保 API 安全
2. [game-logic/sports-betting.md](game-logic/sports-betting.md) - Valid Bet 計算規則
3. [finance/reconciliation.md](finance/reconciliation.md) - 對帳與流水驗證

**實作 Bonus 系統**:
1. [core/01-security.md](core/01-security.md) - 冪等性防護（防重複領取）
2. [finance/reconciliation.md](finance/reconciliation.md) - 投注要求追蹤 + 促銷錢包轉帳
3. [game-logic/free-spins.md](game-logic/free-spins.md) - 免費旋轉流水計算

**實作風控檢測**:
1. [game-logic/roulette-hedge.md](game-logic/roulette-hedge.md) - 輪盤對沖算法
2. [game-logic/sports-betting.md](game-logic/sports-betting.md) - 體育博彩風控
3. [core/02-concurrency.md](core/02-concurrency.md) - 並發安全

---

## 🗂️ 歷史歸檔

**v2.0.0 及更早版本** 的 13 個專題文件（01_token_verification.md ~ 12_promo_wallet_transfer.md）已整合為 v3.0.0 的 3 層結構（core/ + game-logic/ + finance/）。

歷史文件可查閱：
- **[歸檔索引](../../archive/INDEX.md)** - 完整歷史文檔導航
- **Git 歷史**: 使用 `git log --follow` 追溯各文件的演變歷史

---

## 📊 改進成果

| 指標 | v2.0.0 | v3.0.0 (當前) | 改進 |
|------|--------|--------------|------|
| 文件數量 | 14 個 | 10 個 | -28% |
| 目錄層級 | 扁平結構 | 3 層分類 | 組織性 +60% |
| 查找效率 | 平均 3-5 分鐘 | 平均 1-2 分鐘 | -50% |
| 重複內容 | ~15% | 0% | 完全去重 |

---

**維護團隊**: Finance Center Architecture Team
**反饋聯繫**: finance-arch@company.com
**最後審閱**: 2026-02-03
