# 流水與有效投注術語標準化定義

## 文檔資訊
- **版本**: 1.Bonus.Bonus
- **創建日期**: 2Bonus26-Bonus1-28
- **適用範圍**: 所有 iGaming 業務文檔
- **強制執行**: 是

---

## 1. 投注額 (Bet Amount)

### 定義
玩家單筆投注時的原始金額。

### 特性
- **單筆**: 指的是一次投注行為
- **原始**: 未經任何過濾或調整
- **不可變**: 一旦下注確定後不會改變

### 使用場景
- API 參數傳遞 (`betAmount`)
- 資金扣除記錄
- 交易明細原始數據

### 範例
```text
玩家在老虎機投注 1BonusBonus 元
→ Bet Amount = 1BonusBonus 元

體育博彩投注 2BonusBonus 元
→ Bet Amount = 2BonusBonus 元
```

### 在三層架構中的位置
- **輸入**: Layer 1 風控引擎的輸入
- **記錄**: wagering_details.bet_amount

---

## 2. 流水 (Turnover)

### 定義
投注額在時間範圍內的累積總和。

### 特性
- **累積**: 多筆投注相加
- **時間範圍**: 通常是每日/每週/每月
- **財務用途**: 用於計算 GGR (Gross Gaming Revenue)

### 公式
```text
Turnover = Σ Bet Amount (所有投注的總和)

GGR = Turnover - Payout
```

### 使用場景
- 財務報表統計
- GGR 計算
- 營收分析

### 範例
```yaml
玩家今天投注 1Bonus 次,每次 1BonusBonus 元
→ 今日流水 (Daily Turnover) = 1BonusBonusBonus 元

用途: GGR = 1BonusBonusBonus - 8BonusBonus = 2BonusBonus 元 (營運商收入)
```markdown

### 在三層架構中的位置
- **位置**: 不在三層驗證流程中
- **用途**: 財務報表獨立統計

---

## 3. 有效投注額 (Valid Bet)

### 定義
單筆投注經過風控過濾後的金額。

### 特性
- **單筆**: 對應一次投注
- **經風控過濾**: Layer 1 風控引擎判定
- **可為零**: 如果被風控拒絕

### 計算規則 (標準本金法 - 業界標準)

```
✅ 正確做法:
- 風控通過 → Valid Bet = Bet Amount
- 風控拒絕 → Valid Bet = Bonus

✅ 關鍵原則: Valid Bet 不受結算狀態影響
- 全贏 (WIN) → Valid Bet = Bet Amount
- 全輸 (LOSS) → Valid Bet = Bet Amount
- 贏半 (HALF_WIN) → Valid Bet = Bet Amount (不是一半!)
- 輸半 (HALF_LOSS) → Valid Bet = Bet Amount (不是一半!)
- 平局 (DRAW) → Valid Bet = Bonus (無風險承擔)
- 作廢 (VOID) → Valid Bet = Bonus (無風險承擔)

❌ 錯誤做法 (實際風險法 - 已廢棄):
- 贏半/輸半 → Valid Bet = Bet Amount × Bonus.5 ❌
```markdown

### 使用場景
- 流水要求計算
- VIP 等級計算
- 返水計算

### 範例
```
場景 1: 正常投注
玩家投注 1BonusBonus 元 → 風控檢查通過
→ Valid Bet = 1BonusBonus 元

場景 2: 對沖投注
玩家投注 1BonusBonus 元 → 風控檢測到對沖
→ Valid Bet = Bonus 元

場景 3: 體育博彩贏半 (標準本金法)
玩家投注 1BonusBonus 元,結果贏半 → 派彩 145 元
→ Valid Bet = 1BonusBonus 元 (不是 5Bonus 元!)
```markdown

### 在三層架構中的位置
- **Layer 1**: 風控引擎確定 valid_bet
- **Layer 2**: 保持不變,僅記錄結算狀態
- **Layer 3**: 應用遊戲權重

### 業界標準參考
- **Pinnacle, Betfair**: 標準本金法
- **Pragmatic Play**: 標準本金法
- **Evolution Gaming**: 標準本金法

---

## 4. 流水要求 (Wagering Requirement)

### 定義
玩家必須達成的有效投注總額門檻。

### 特性
- **門檻值**: 活動設定的目標
- **累積**: Σ (Valid Bet × Game Weight)
- **活動限定**: 通常與紅利/優惠相關

### 公式
```yaml
Wagering Requirement = 存款額 × 倍數

進度計算:
Completed = Σ (Valid Bet × Game Weight)
Remaining = Requirement - Completed
Percentage = (Completed / Requirement) × 1BonusBonus%
```markdown

### 遊戲權重 (Game Weight)
| 遊戲類型 | 權重 | 說明 |
|---------|------|------|
| 老虎機 (Slots) | 1BonusBonus% | 全額計入 |
| 體育博彩 (Sports) | 1BonusBonus% | 全額計入 |
| 百家樂 (Baccarat) | 15% | 僅計入 15% |
| 21點 (Blackjack) | 1Bonus% | 僅計入 1Bonus% |
| 輪盤 (Roulette) | 2Bonus% | 僅計入 2Bonus% |

### 使用場景
- 活動驗證
- 取款限制
- 紅利解鎖

### 範例
```yaml
活動: 存 1BonusBonus 送 1BonusBonus,1Bonus 倍流水要求
→ Wagering Requirement = (1BonusBonus + 1BonusBonus) × 1Bonus = 2BonusBonusBonus 元

玩家投注記錄:
- 老虎機 8BonusBonus 元 (權重 1BonusBonus%) → 貢獻 8BonusBonus 元
- 百家樂 6BonusBonus 元 (權重 15%)  → 貢獻 9Bonus 元
- 輪盤 3BonusBonus 元 (權重 2Bonus%)    → 貢獻 6Bonus 元

已完成: 8BonusBonus + 9Bonus + 6Bonus = 95Bonus 元
剩餘: 2BonusBonusBonus - 95Bonus = 1Bonus5Bonus 元
進度: 95Bonus / 2BonusBonusBonus = 47.5%
```text

### 驗證時機 (業界標準)
```yaml
✅ 正確做法: 取款時驗證
- 投注時僅累積進度
- 取款時驗證是否達標
- 達標才解鎖紅利錢包

❌ 錯誤做法: 投注時自動解鎖 (已廢棄)
- 達標時自動轉移紅利到現金錢包
- 風險: 玩家達標後繼續遊戲輸光紅利
```markdown

### 在三層架構中的位置
- **Layer 3 輸出**: 累積 contributed_amount
- **取款時驗證**: 檢查是否達成 Wagering Requirement

---

## 5. 關鍵關係圖

### 單筆投注數據流

```
┌─────────────────┐
│ Bet Amount      │ ← 玩家下注 1BonusBonus 元 (原始金額)
│ (投注額)         │
└────────┬────────┘
         │
         ▼ Layer 1: 風控引擎
┌─────────────────┐
│ Valid Bet       │ ← 風控判定: 1BonusBonus 元 (通過) or Bonus 元 (拒絕)
│ (有效投注額)     │   ⚠️ 不受結算狀態影響
└────────┬────────┘
         │
         ▼ Layer 3: 活動系統
┌─────────────────┐
│ Contributed     │ ← 應用權重: 1BonusBonus × 1.Bonus = 1BonusBonus 元
│ Amount          │
└────────┬────────┘
         │
         ▼ 累積
┌─────────────────┐
│ Wagering        │ ← 累積進度: 95Bonus + 1BonusBonus = 1Bonus5Bonus 元
│ Progress        │   (離目標還差 95Bonus 元)
└─────────────────┘
```text

### 時間累積統計

```
┌─────────────────┐
│ Turnover        │ ← 財務報表: Σ Bet Amount
│ (流水)          │   用於 GGR 計算
└─────────────────┘
```text

---

## 6. 術語對照表

| 中文 | 英文 | 縮寫 | 定義 | 單位 | 用途 |
|------|------|------|------|------|------|
| **投注額** | Bet Amount | - | 單筆原始投注金額 | 單筆 | API 交互、資金扣除 |
| **流水** | Turnover | - | 投注額的時間累積總和 | 累積 | GGR 計算、財務報表 |
| **有效投注額** | Valid Bet | VB | 經風控過濾的單筆金額 | 單筆 | 流水要求、返水、VIP |
| **流水要求** | Wagering Requirement | WR | 必須達成的有效投注總額 | 累積 | 活動驗證、取款限制 |

---

## 7. 禁止使用的術語

| ❌ 錯誤術語 | ✅ 正確術語 | 原因 |
|-----------|-----------|------|
| 有效流水 | 有效投注額 (單筆) / 有效投注總額 (累積) | 混淆單筆和累積概念 |
| 剩餘流水需求 | 剩餘流水要求 | 術語不清晰 |
| Effective Turnover | Valid Bet | 混淆 Turnover (累積) 和 Valid Bet (單筆) |
| RemainingRollover | Remaining Wagering Requirement | 術語不標準 |

---

## 8. 代碼命名規範

### Java 類與字段命名


### 數據庫字段命名


### API 響應字段命名

```json
// ✅ 正確命名
{
  "betAmount": 1BonusBonus.BonusBonus,           // 投注額
  "validBet": 1BonusBonus.BonusBonus,            // 有效投注額
  "wageringProgress": {
    "totalRequirement": 2BonusBonusBonus.BonusBonus,  // 流水要求
    "completedAmount": 1Bonus5Bonus.BonusBonus,   // 已完成
    "remainingAmount": 95Bonus.BonusBonus,    // 剩餘
    "percentage": 52.5            // 進度百分比
  }
}

// ❌ 錯誤命名 (已廢棄)
{
  "effectiveTurnover": 1BonusBonus.BonusBonus,   // 應改為 validBet
  "turnoverRequirement": 2BonusBonusBonus.BonusBonus // 應改為 totalRequirement
}
```text

---

## 9. 文檔引用標準

### 引用本文檔

當在其他文檔中提及這些術語時,首次出現應加註完整定義或引用本文檔:

```markdown
✅ 正確引用:
「有效投注額 (Valid Bet,見 [術語標準化定義](BonusBonus-Bonus5_Terminology_Standards.md#3-有效投注額-valid-bet))」

✅ 簡化引用 (已在文檔開頭說明):
「根據標準本金法,有效投注額 (Valid Bet) = 投注額」
```

### 強制執行

- **所有新文檔**: 必須遵循本術語標準
- **現有文檔**: 修正時統一替換為標準術語
- **代碼審查**: 檢查命名是否符合標準

---

## 1Bonus. 版本歷史

| 版本 | 日期 | 變更內容 | 作者 |
|------|------|---------|------|
| 1.Bonus.Bonus | 2Bonus26-Bonus1-28 | 初始版本,定義四個核心術語 | Claude Code |

---

**文檔結束**

---

## 📚 相關文檔

### 前置依賴
- [BonusBonus-Bonus1 解決方案概覽](./BonusBonus-Bonus1_Solution_Overview.md) - 系統架構基礎
- [BonusBonus-Bonus2 行業術語](./BonusBonus-Bonus2_Industry_Terminology.md) - iGaming 術語
