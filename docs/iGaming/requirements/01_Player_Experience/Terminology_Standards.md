# 流水與有效投注術語標準

> **權威來源**: [00-08_Terminology_Standards.md](../../source-archive/00_Foundation/guides/00-08_Terminology_Standards.md)
> **目標讀者**: 高階主管、產品經理、合規專員
> **相關架構**: N/A — 參考/術語表文檔
> **最後同步**: 2026-02-08

---

## 業務價值（Business Value）

本術語標準提供以下戰略價值：
- **計算準確性**: 使用標準本金法 (Standard Principal Method)（Pinnacle、Betfair、Evolution Gaming 使用的行業標準）確保有效投注 (Valid Bet) 計算一致
- **風險緩解**: 通過明確定義流水要求 (Wagering Requirement) 驗證時機（在提款時而非投注期間），防止促銷濫用
- **系統一致性**: 建立強制性 API 欄位命名規範（`validBet`、`wageringProgress.*`），防止整合錯誤
- **合規就緒**: 提供符合主要遊戲供應商標準的可審計定義，適用於監管檢查

---

## 成功指標（Success Metrics）

| 指標 | 目標 | 衡量方式 |
|-----|------|---------|
| 術語合規性 | 100% 新文檔使用標準術語 | 文檔審計禁用術語使用情況 |
| API 命名合規性 | 100% 端點使用標準欄位名稱 | 代碼審查和 API Schema 驗證 |
| 計算準確性 | 有效投注計算零差異 | 平台與遊戲供應商之間的對帳 |
| 跨團隊理解 | 所有團隊成員能正確定義 4 個核心術語 | 季度術語測驗（Bet Amount、Turnover、Valid Bet、Wagering Requirement） |

---

## 文檔資訊

- **版本**: 4.0.0
- **創建日期**: 2026-01-28
- **適用範圍**: 所有 iGaming 業務文檔
- **執行力度**: 強制

---

## 1. Bet Amount（投注金額）

### 定義

玩家 (Player) 在單次投注中下注的原始金額。

### 特性

| 屬性 | 描述 |
|-----|-----|
| **範圍** | 單次投注 |
| **性質** | 原始、未過濾、未調整 |
| **不可變性** | 投注確認後不可更改 |

### 使用場景

- API 參數傳遞
- 資金扣除記錄
- 交易詳情原始數據

### 範例

| 場景 | Bet Amount |
|-----|----------|
| 玩家在老虎機上投注 100 | 100 |
| 玩家在體育上投注 200 | 200 |

### 在三層架構中的位置

- **輸入**: 第 1 層風控引擎輸入
- **記錄**: `wagering_details.bet_amount`

---

## 2. Turnover（流水總額）

### 定義

一段時間內投注金額的累計總和。

### 特性

| 屬性 | 描述 |
|-----|-----|
| **性質** | 累計（多次投注相加） |
| **時間範圍** | 通常為每日/每週/每月 |
| **財務用途** | 用於計算 GGR（總博彩收入） |

### 公式

**Turnover** = 所有 Bet Amount 的總和

**GGR** = Turnover - Payout（派彩）

### 使用場景

- 財務報告和統計
- GGR 計算
- 收入分析

### 範例

| 場景 | 計算 | 結果 |
|-----|-----|-----|
| 玩家今天投注 10 次，每次 100 | 10 x 100 | Daily Turnover = 1,000 |
| 上述情況的 GGR | 1,000 - 800（派彩） | GGR = 200（運營商收入） |

### 在三層架構中的位置

- **位置**: 不屬於三層驗證流程
- **用途**: 獨立的財務報告指標

---

## 3. Valid Bet（有效投注）

### 定義

經過風控過濾後的單次投注金額。

### 特性

| 屬性 | 描述 |
|-----|-----|
| **範圍** | 單次投注 |
| **過濾** | 由第 1 層風控引擎決定 |
| **可為零** | 如被風控拒絕 |

### 計算規則（標準本金法 - 行業標準）

| 結果 | Valid Bet 規則 |
|-----|---------------|
| 風控通過 | Valid Bet = Bet Amount |
| 風控拒絕 | Valid Bet = 0 |
| 全贏 (WIN) | Valid Bet = Bet Amount |
| 全輸 (LOSS) | Valid Bet = Bet Amount |
| 半贏 (HALF_WIN) | Valid Bet = Bet Amount（不是一半） |
| 半輸 (HALF_LOSS) | Valid Bet = Bet Amount（不是一半） |
| 和局 (DRAW) | Valid Bet = 0（無風險承擔） |
| 作廢 (VOID) | Valid Bet = 0（無風險承擔） |

**關鍵原則**: Valid Bet 不受結算結果影響。

**已棄用方法（實際風險法）**:
- 半贏/半輸按 Bet Amount 的 50% 計算 — 此方法已不再使用

### 使用場景

- 流水要求計算
- VIP 等級計算
- 返水計算

### 範例

| 場景 | 結果 |
|-----|-----|
| 正常投注：玩家投注 100，風控檢查通過 | Valid Bet = 100 |
| 對沖投注：玩家投注 100，風控檢測到對沖 | Valid Bet = 0 |
| 體育半贏：玩家投注 100，派彩 145 | Valid Bet = 100（不是 50） |

### 行業標準參考

| 供應商 | 使用方法 |
|-------|---------|
| Pinnacle, Betfair | 標準本金法 (Standard Principal Method) |
| Pragmatic Play | 標準本金法 (Standard Principal Method) |
| Evolution Gaming | 標準本金法 (Standard Principal Method) |

### 在三層架構中的位置

- **第 1 層**: 風控引擎決定 valid_bet
- **第 2 層**: 不變，僅記錄結算狀態
- **第 3 層**: 應用遊戲權重

---

## 4. Wagering Requirement（流水要求）

### 定義

玩家必須達到的有效投注總額閾值。

### 特性

| 屬性 | 描述 |
|-----|-----|
| **性質** | 閾值 |
| **計算方式** | 累計：Sum of (Valid Bet x Game Weight) |
| **範圍** | 綁定特定促銷/獎金 |

### 公式

**Wagering Requirement** = 存款金額 x 倍數

**進度計算**：

| 指標 | 公式 |
|-----|-----|
| 已完成 | Sum of (Valid Bet x Game Weight) |
| 剩餘 | Requirement - Completed |
| 百分比 | (Completed / Requirement) x 100% |

### 遊戲權重 (Game Weights)

| 遊戲類型 | 權重 | 說明 |
|---------|-----|-----|
| 老虎機 (Slots) | 100% | 全額貢獻 |
| 體育投注 (Sports Betting) | 100% | 全額貢獻 |
| 百家樂 (Baccarat) | 15% | 僅 15% 計入 |
| 21 點 (Blackjack) | 10% | 僅 10% 計入 |
| 輪盤 (Roulette) | 20% | 僅 20% 計入 |

### 使用場景

- 促銷驗證
- 提款限制
- 獎金解鎖條件

### 範例

**促銷**: 存款 100，獲得 100 獎金，10 倍流水要求

**Wagering Requirement** = (100 + 100) x 10 = **2,000**

| 玩家投注記錄 | 金額 | 權重 | 貢獻 |
|------------|-----|-----|-----|
| 老虎機 | 800 | 100% | 800 |
| 百家樂 | 600 | 15% | 90 |
| 輪盤 | 300 | 20% | 60 |
| **已完成總計** | | | **950** |
| **剩餘** | | | **1,050** |
| **進度** | | | **47.5%** |

### 驗證時機（行業標準）

**正確做法**: 在提款時驗證

| 步驟 | 動作 |
|-----|-----|
| 投注期間 | 僅累計進度 |
| 提款時 | 驗證是否達標 |
| 如達標 | 解鎖獎金錢包 |

**已棄用做法**: 投注期間自動解鎖（風險：玩家達標後繼續遊戲並輸掉獎金）

### 在三層架構中的位置

- **第 3 層輸出**: 累計 contributed_amount
- **提款驗證**: 檢查 Wagering Requirement 是否達標

---

## 5. 關鍵關係流程

### 單次投注數據流

| 階段 | 術語 | 範例 | 說明 |
|-----|-----|-----|-----|
| 玩家下注 | Bet Amount | 100 | 原始金額 |
| 風控引擎檢查 | Valid Bet | 100（通過）或 0（拒絕） | 不受結算影響 |
| 應用權重 | Contributed Amount | 100 x 1.0 = 100 | 遊戲權重後 |
| 累計 | Wagering Progress | 950 + 100 = 1,050 | 朝向目標的累計總額 |

### 基於時間的累計

| 術語 | 用途 | 公式 |
|-----|-----|-----|
| Turnover | 財務報告 | 所有 Bet Amount 的總和 |
| Wagering Progress | 促銷追蹤 | Sum of (Valid Bet x Game Weight) |

---

## 6. 術語參考表

| 中文 | 英文 | 縮寫 | 定義 | 單位 | 使用場景 |
|-----|-----|-----|-----|-----|---------|
| **投注金額** | Bet Amount | - | 單次原始投注金額 | 每次 | API 互動、資金扣除 |
| **流水總額** | Turnover | - | 一段時間內投注金額的累計總和 | 累計 | GGR 計算、財務報告 |
| **有效投注** | Valid Bet | VB | 風控過濾後的單次投注金額 | 每次 | 流水要求、返水、VIP |
| **流水要求** | Wagering Requirement | WR | 需達到的有效投注總額閾值 | 累計 | 促銷驗證、提款限制 |

---

## 7. 禁用術語

| 錯誤術語 | 正確術語 | 原因 |
|---------|---------|-----|
| 有效流水（混合） | Valid Bet（單次）/ Total Valid Bets（累計） | 混淆單次和累計概念 |
| 剩餘流水要求（模糊） | Remaining Wagering Requirement | 術語不精確 |
| Effective Turnover（英文） | Valid Bet | 混淆 Turnover（累計）和 Valid Bet（單次） |
| RemainingRollover | Remaining Wagering Requirement | 非標準術語 |

---

## 8. 代碼和 API 命名規範

### API 響應欄位命名

**正確命名規範**：

| 欄位 | 含義 |
|-----|-----|
| `betAmount` | 投注金額 (Bet Amount) |
| `validBet` | 有效投注 (Valid Bet) |
| `wageringProgress.totalRequirement` | 流水要求 (Wagering Requirement) |
| `wageringProgress.completedAmount` | 已完成金額 |
| `wageringProgress.remainingAmount` | 剩餘金額 |
| `wageringProgress.percentage` | 進度百分比 |

**已棄用命名（請勿使用）**：

| 棄用欄位 | 替代 |
|---------|-----|
| `effectiveTurnover` | `validBet` |
| `turnoverRequirement` | `totalRequirement` |

---

## 9. 文檔引用標準

### 如何引用這些術語

在其他文檔中提及這些術語時，首次出現應包含完整定義或對本文檔的引用。

**正確引用範例**：
- "Valid Bet（見術語標準，第 3 節）"
- "根據標準本金法 (Standard Principal Method)，Valid Bet = Bet Amount"

### 執行規則

| 範圍 | 要求 |
|-----|-----|
| 所有新文檔 | 必須遵循本術語標準 |
| 現有文檔 | 修訂時替換為標準術語 |
| 代碼審查 | 檢查命名合規性 |

---

## 10. 版本歷史

| 版本 | 日期 | 變更 | 作者 |
|-----|-----|-----|-----|
| 1.0.0 | 2026-01-28 | 初始版本，定義四個核心術語 | Claude Code |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-08
**維護者**: 產品管理團隊
