# 投注與有效投注術語標準

> **Canonical Source**: [00-08_Terminology_Standards.md](../../source-archive/00_Foundation/guides/00-08_Terminology_Standards.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Architecture**: N/A — Reference/glossary document
> **Last Synced**: 2026-02-08

---

## 業務價值（Business Value）

此標準化提供以下價值：
- **財務準確性**：消除導致關鍵業務計算錯誤的模糊術語（例如，Valid Bet 與 Turnover 混淆導致 GGR 報告錯誤、紅利解鎖錯誤）
- **合規保證**：通過標準化 GGR 計算方法和流水要求定義，確保向監管機構（UKGC, MGA）提供準確的財務報告
- **跨團隊效率**：在產品、工程、QA 和合規團隊之間建立統一術語，減少溝通成本

## 成功指標（Success Metrics）

| 指標 | 目標 | 測量方式 |
|------|------|----------|
| API 命名合規率 | 100% | 所有新 API 端點使用標準化字段名稱（`betAmount`, `validBet`, `wageringProgress`），通過代碼審查檢查表驗證 |
| 文檔合規率 | 100% | 所有新業務文檔遵循禁用術語規則（grep 審計 "Effective Turnover", "RemainingRollover" 等） |
| 代碼審查覆蓋率 | 100% | 術語合規性檢查整合到代碼審查流程（驗證無已棄用字段名稱，如 `effectiveTurnover`, `turnoverRequirement`） |
| 交叉引用準確性 | 100% | 所有引用這些術語的文檔包含對本標準文檔的引用（Section 9 合規） |

---

## 文檔資訊（Document Information）

- **版本**: 4.0.0
- **創建日期**: 2026-01-28
- **範圍**: 所有 iGaming 業務文檔
- **執行力度**: 強制

---

## 1. 投注金額（Bet Amount）

### 定義

玩家在單次投注中下注的原始金額。

### 特性

| 屬性 | 說明 |
|------|------|
| **範圍** | 單次投注 |
| **性質** | 原始的、未過濾的、未調整的 |
| **不可變性** | 投注確認後不可更改 |

### 使用場景

- API 參數傳遞
- 資金扣除記錄
- 交易明細原始數據

### 範例

| 場景 | 投注金額（Bet Amount） |
|------|----------------------|
| 玩家在老虎機上投注 100 | 100 |
| 玩家在體育博彩上投注 200 | 200 |

### 在三層架構中的位置

- **輸入**: Layer 1 風控引擎輸入
- **記錄**: `wagering_details.bet_amount`

---

## 2. 流水（Turnover）

### 定義

一段時間內投注金額的累計總和。

### 特性

| 屬性 | 說明 |
|------|------|
| **性質** | 累計（多次投注相加） |
| **時間範圍** | 通常為每日 / 每週 / 每月 |
| **財務用途** | 用於計算 GGR（毛利） |

### 公式

**流水（Turnover）** = 所有投注金額之和

**GGR（毛利）** = 流水 - 派彩

### 使用場景

- 財務報表與統計
- GGR 計算
- 收入分析

### 範例

| 場景 | 計算 | 結果 |
|------|------|------|
| 玩家今天投注 10 次，每次 100 | 10 x 100 | 當日流水 = 1,000 |
| 上述情境的 GGR | 1,000 - 800（派彩） | GGR = 200（運營商收入） |

### 在三層架構中的位置

- **位置**: 不屬於三層驗證流程
- **用途**: 獨立的財務報告指標

---

## 3. 有效投注（Valid Bet）

### 定義

經過風控過濾後的單次投注金額。

### 特性

| 屬性 | 說明 |
|------|------|
| **範圍** | 單次投注 |
| **過濾** | 由 Layer 1 風控引擎決定 |
| **可為零** | 如被風控拒絕 |

### 計算規則（標準本金法 - 行業標準）

| 結算結果 | 有效投注規則（Valid Bet Rule） |
|---------|------------------------------|
| 風控通過 | Valid Bet = Bet Amount |
| 風控拒絕 | Valid Bet = 0 |
| 全贏（WIN） | Valid Bet = Bet Amount |
| 全輸（LOSS） | Valid Bet = Bet Amount |
| 半贏（HALF_WIN） | Valid Bet = Bet Amount（不是一半） |
| 半輸（HALF_LOSS） | Valid Bet = Bet Amount（不是一半） |
| 和局（DRAW） | Valid Bet = 0（無風險承擔） |
| 作廢（VOID） | Valid Bet = 0（無風險承擔） |

**關鍵原則**：有效投注（Valid Bet）不受結算結果影響。

**已棄用方法（實際風險法）**：
- 半贏/半輸為投注金額的 50% — 此方法已不再使用

### 使用場景

- 流水要求計算
- VIP 等級計算
- 返水計算

### 範例

| 場景 | 結果 |
|------|------|
| 正常投注：玩家投注 100，風控通過 | Valid Bet = 100 |
| 對沖投注：玩家投注 100，風控檢測到對沖 | Valid Bet = 0 |
| 體育半贏：玩家投注 100，派彩 145 | Valid Bet = 100（不是 50） |

### 行業標準參考

| 供應商 | 使用方法 |
|--------|---------|
| Pinnacle, Betfair | 標準本金法（Standard Principal Method） |
| Pragmatic Play | 標準本金法（Standard Principal Method） |
| Evolution Gaming | 標準本金法（Standard Principal Method） |

### 在三層架構中的位置

- **Layer 1**: 風控引擎決定 valid_bet
- **Layer 2**: 不變，僅記錄結算狀態
- **Layer 3**: 應用遊戲權重

---

## 4. 流水要求（Wagering Requirement）

### 定義

玩家必須達到的有效投注總額門檻。

### 特性

| 屬性 | 說明 |
|------|------|
| **性質** | 門檻值 |
| **計算** | 累計：Sum of (Valid Bet x Game Weight) |
| **範圍** | 綁定特定促銷活動/紅利 |

### 公式

**流水要求（Wagering Requirement）** = 存款金額 x 倍數

**進度計算**：

| 指標 | 公式 |
|------|------|
| 已完成 | Sum of (Valid Bet x Game Weight) |
| 剩餘 | Requirement - Completed |
| 百分比 | (Completed / Requirement) x 100% |

### 遊戲權重（Game Weights）

| 遊戲類型 | 權重 | 說明 |
|---------|------|------|
| 老虎機（Slots） | 100% | 完全貢獻 |
| 體育博彩（Sports Betting） | 100% | 完全貢獻 |
| 百家樂（Baccarat） | 15% | 僅計算 15% |
| 二十一點（Blackjack） | 10% | 僅計算 10% |
| 輪盤（Roulette） | 20% | 僅計算 20% |

### 使用場景

- 促銷驗證
- 提款限制
- 紅利解鎖條件

### 範例

**促銷活動**：存款 100，獲得 100 紅利，10 倍流水要求

**流水要求（Wagering Requirement）** = (100 + 100) x 10 = **2,000**

| 玩家投注記錄 | 金額 | 權重 | 貢獻 |
|------------|------|------|------|
| 老虎機（Slots） | 800 | 100% | 800 |
| 百家樂（Baccarat） | 600 | 15% | 90 |
| 輪盤（Roulette） | 300 | 20% | 60 |
| **總計已完成** | | | **950** |
| **剩餘** | | | **1,050** |
| **進度** | | | **47.5%** |

### 驗證時機（行業標準）

**正確做法**：在提款時驗證

| 步驟 | 動作 |
|------|------|
| 投注期間 | 僅累計進度 |
| 提款時 | 驗證是否達到要求 |
| 達到要求 | 解鎖紅利錢包 |

**已棄用做法**：在投注期間自動解鎖（風險：玩家達到目標後繼續遊戲並輸掉紅利）

### 在三層架構中的位置

- **Layer 3 輸出**: 累計 contributed_amount
- **提款驗證**: 檢查是否達到流水要求（Wagering Requirement）

---

## 5. 關鍵關係流程（Key Relationship Flow）

### 單次投注數據流

| 階段 | 術語 | 範例 | 說明 |
|------|------|------|------|
| 玩家投注 | 投注金額（Bet Amount） | 100 | 原始金額 |
| 風控檢查 | 有效投注（Valid Bet） | 100（通過）或 0（拒絕） | 不受結算影響 |
| 應用權重 | 貢獻金額（Contributed Amount） | 100 x 1.0 = 100 | 遊戲權重後 |
| 累計 | 流水進度（Wagering Progress） | 950 + 100 = 1,050 | 累計至目標 |

### 基於時間的累計

| 術語 | 用途 | 公式 |
|------|------|------|
| 流水（Turnover） | 財務報告 | Sum of all Bet Amounts |
| 流水進度（Wagering Progress） | 促銷追蹤 | Sum of (Valid Bet x Game Weight) |

---

## 6. 術語對照表（Terminology Reference Table）

| 中文 | 英文 | 縮寫 | 定義 | 單位 | 使用場景 |
|------|------|------|------|------|---------|
| **投注金額** | Bet Amount | - | 單次原始投注金額 | 單次 | API 交互、資金扣除 |
| **流水** | Turnover | - | 一段時間內投注金額的累計總和 | 累計 | GGR 計算、財務報告 |
| **有效投注** | Valid Bet | VB | 經過風控過濾後的單次投注金額 | 單次 | 流水要求、返水、VIP |
| **流水要求** | Wagering Requirement | WR | 必須達到的有效投注總額門檻 | 累計 | 促銷驗證、提款限制 |

---

## 7. 禁用術語（Prohibited Terminology）

| 錯誤術語 | 正確術語 | 原因 |
|---------|---------|------|
| Effective Turnover（混合） | Valid Bet（單次）/ Total Valid Bets（累計） | 混淆單次投注與累計概念 |
| Remaining Turnover Requirement（模糊） | Remaining Wagering Requirement | 術語不精確 |
| Effective Turnover（英文） | Valid Bet | 混淆 Turnover（累計）與 Valid Bet（單次） |
| RemainingRollover | Remaining Wagering Requirement | 非標準術語 |

---

## 8. 代碼與 API 命名規範（Naming Conventions for Code and APIs）

### API 響應字段命名

**正確命名規範**：

| 字段 | 含義 |
|------|------|
| `betAmount` | 投注金額（Bet Amount） |
| `validBet` | 有效投注（Valid Bet） |
| `wageringProgress.totalRequirement` | 流水要求（Wagering Requirement） |
| `wageringProgress.completedAmount` | 已完成金額 |
| `wageringProgress.remainingAmount` | 剩餘金額 |
| `wageringProgress.percentage` | 進度百分比 |

**已棄用命名（請勿使用）**：

| 已棄用字段 | 替換為 |
|-----------|--------|
| `effectiveTurnover` | `validBet` |
| `turnoverRequirement` | `totalRequirement` |

---

## 9. 文檔引用標準（Documentation Citation Standard）

### 如何引用這些術語

在其他文檔中提及這些術語時，首次出現應包含完整定義或引用本文檔。

**正確引用範例**：
- "有效投注（Valid Bet，見術語標準，Section 3）"
- "根據標準本金法（Standard Principal Method），Valid Bet = Bet Amount"

### 執行規則

| 範圍 | 要求 |
|------|------|
| 所有新文檔 | 必須遵循本術語標準 |
| 現有文檔 | 在修訂期間替換為標準術語 |
| 代碼審查 | 檢查命名合規性 |

---

## 10. 版本歷史（Version History）

| 版本 | 日期 | 變更 | 作者 |
|------|------|------|------|
| 1.0.0 | 2026-01-28 | 初始版本，定義四個核心術語 | Claude Code |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-08
**維護者**: 產品管理團隊（Product Management Team）
