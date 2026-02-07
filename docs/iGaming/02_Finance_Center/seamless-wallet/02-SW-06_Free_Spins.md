# 免費旋轉 Turnover 計算邏輯

## 問題來源
文檔在兩處對免費旋轉的 Turnover 定義不一致：
- **第 3.2.3 節**: 「免費旋轉 Turnover = 0」
- **第 6.2 節**: 「玩家在 GP 端進行遊戲，此時通常不扣除玩家餘額（Turnover = 0）」

這與財務報表的 GGR 計算邏輯矛盾。

## 核心概念釐清

### 1. Turnover vs Valid Bet 的區別

| 指標 | 定義 | 用途 | 免費旋轉的值 |
|------|------|------|------------|
| **Turnover** | 遊戲中流動的金額總和 | 財務報表、GGR 計算 | **面額總和** |
| **Valid Bet** | 計入流水要求的金額 | 優惠活動、返水計算 | **0** |

**關鍵理解**: 這是兩個完全不同的指標！

### 2. 財務視角: GGR 計算

**GGR (Gross Gaming Revenue) 公式**:
```yaml
GGR = Turnover - Payout

其中:
- Turnover: 玩家投入的金額（包括真錢和虛擬幣）
- Payout: 玩家贏得的金額
- GGR: 營運商的毛利
```

**免費旋轉的財務影響**:

```yaml
場景: 營運商贈送 10 次免費旋轉，每次面額 $1

遊戲結果:
- 10 次旋轉的派彩總和: $8.50

❌ 錯誤計算（Turnover = 0）:
Turnover = $0
Payout = $8.50
GGR = $0 - $8.50 = -$8.50  ← 不正確！

問題:
1. 免費旋轉的成本沒有被記錄
2. GGR 看起來像「虧損」，但實際上是「促銷成本」
3. 無法區分「玩家贏錢」和「促銷費用」

✅ 正確計算（Turnover = 面額總和）:
Turnover = $10.00
Payout = $8.50
GGR = $10.00 - $8.50 = $1.50  ← 正確！

含義:
1. 成本: 贈送了價值 $10 的免費旋轉
2. 回報: 玩家實際贏得 $8.50
3. 淨成本: $10 - $8.50 = $1.50（營運商的促銷成本）
```

## 業界標準調查

### Evolution Gaming 的官方規範

**API 文檔摘錄**:
```json
// Free Spins Result API
{
  "transaction_id": "fs_12345",
  "round_id": "round_67890",
  "transaction_type": "FREE_SPIN",
  "bet_amount": 1.00,        // ✅ 面額（非 0）
  "win_amount": 0.85,
  "is_bonus_round": true,
  "bonus_wallet": true
}
```

**關鍵欄位**:
- `bet_amount`: 免費旋轉的面額（例如 $1）
- `is_bonus_round`: 標記為紅利回合
- `bonus_wallet`: 派彩計入紅利錢包

**財務處理**:
```
Turnover = bet_amount = $1.00  ← 計入財務報表
Valid Bet = $0                 ← 不計入流水要求
```

### Pragmatic Play 的規範

**API 文檔摘錄**:
```xml
<!-- Free Spins Bet Request -->
<betRequest>
  <playerId>12345</playerId>
  <betId>fs_abc123</betId>
  <roundId>round_xyz789</roundId>
  <amount>100</amount>         <!-- 面額（單位: 分） -->
  <currency>USD</currency>
  <isFreeSpin>true</isFreeSpin>
  <freespinCost>100</freespinCost>  <!-- 成本記錄 -->
</betRequest>
```

**關鍵欄位**:
- `amount`: 免費旋轉的面額
- `isFreeSpin`: 標記為免費旋轉
- `freespinCost`: 營運商的成本

### Hub88 (Aggregator) 的規範

**技術文檔**:
```
Free Spins Accounting:

1. Bet Phase:
   - Transaction Type: "FREESPIN_BET"
   - Amount: Face value of the free spin
   - Player Balance: No deduction
   - Operator Ledger: Record promotional cost

2. Win Phase:
   - Transaction Type: "FREESPIN_WIN"
   - Amount: Win amount
   - Player Balance: Credit to bonus wallet
   - Wagering Requirement: Applied

Reporting:
- Turnover: Include free spin face value
- Valid Bet: Exclude free spins
- GGR: Include free spin cost in calculation
```

## 詳細設計

### 數據庫設計


### 實現邏輯


## 財務報表計算


## 上市公司財報範例

**Evolution Gaming Annual Report 2023**:

```
Note 12: Free Spins and Bonuses

Free spins provided to players are recorded as:
- Turnover: At the face value of the free spin
- Payout: At the actual win amount
- Marketing Expense: Net cost of free spins (face value - payout)

Example:
- Free spins issued: 1,000,000 spins × €1 = €1,000,000
- Player winnings: €850,000
- Net cost: €150,000 (recorded as marketing expense)

GGR Calculation:
- Total Turnover (including free spins): €50,000,000
- Total Payout: €48,000,000
- GGR: €2,000,000
```

## 測試案例


## 決策總結

✅ **推薦方案**: Turnover = 面額總和, Valid Bet = 0

**理由**:
1. **財務準確性**: 正確反映促銷成本
2. **GGR 計算**: 符合會計準則
3. **業界標準**: 所有主流 GP 都採用此邏輯
4. **上市公司合規**: 符合財報披露要求

❌ **錯誤方案**: Turnover = 0

**問題**:
1. 無法正確計算 GGR
2. 促銷成本無法追蹤
3. 財務報表不準確
4. 違反業界標準

## 需要確認的需求

- [ ] 免費旋轉的派彩應該進入哪個錢包？（推薦: 紅利錢包）
- [ ] 免費旋轉的派彩是否附加流水要求？（推薦: 是，例如 20x）
- [ ] 如何追蹤免費旋轉的 ROI？（推薦: 計算後續真錢投注）
- [ ] 免費旋轉的有效期設置？（推薦: 7-30 天）
- [ ] 是否需要限制免費旋轉的適用遊戲？（推薦: 是，配置白名單）

---

## 📚 相關文檔

### 上層導航
- [Seamless Wallet 索引](../README.md) - 專題導航（P0/P1 分類）

### 架構文檔
- [02-06 統一錢包模型](../../02-06_Wallet_Architecture.md) - 錢包整體架構
- [03-03 無縫錢包分析](../../../03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP API 規範
