# 百家樂和局投注 Valid Bet 計算邏輯

## 問題來源
文檔第 3.2.2 節第 174 行對和局投注的 Valid Bet 描述不明確且矛盾。

**原文**:
> 和局（Tie）： 許多營運商在計算「返水」時會排除和局的投注，或僅計算和局的輸贏金額。

## 核心問題分析

### 矛盾 1: 「和局投注」vs「莊閒投注遇和局」

文檔混淆了兩個完全不同的概念：

| 投注類型 | 和局時的結果 | 應有的 Valid Bet | 文檔描述 |
|---------|------------|----------------|---------|
| **莊** | 退款（Push） | **0** | ✅ 正確（退還莊閒的注金） |
| **閒** | 退款（Push） | **0** | ✅ 正確 |
| **和** | 贏（賠率 8:1） | **？** | ❌ 不明確（「僅計算輸贏金額」） |

### 矛盾 2: 「僅計算輸贏金額」的歧義

```
場景: 玩家投注 100 元在「和」，結果為和局，贏了 800 元

「僅計算輸贏金額」可能的理解:
A. Valid Bet = 800（贏金）？
B. Valid Bet = 100（投注本金）？
C. Valid Bet = 0（排除和局投注）？

文檔沒有明確說明！
```

## Evolution Gaming 標準規則

```java
/**
 * Evolution Gaming 的百家樂 Valid Bet 計算標準
 */
public BigDecimal calculateBaccaratValidBet(
    BaccaratBetType betType,
    BaccaratOutcome outcome,
    BigDecimal betAmount
) {
    // 莊閒投注遇到和局 → 退款 → Valid Bet = 0
    if ((betType == BANKER || betType == PLAYER) && outcome == TIE) {
        return BigDecimal.ZERO;  // ✅ Push 狀態
    }

    // 和局投注
    if (betType == TIE) {
        // 不論輸贏，Valid Bet = 投注本金
        return betAmount;  // ✅ 承擔風險
    }

    // 對子投注
    if (betType == BANKER_PAIR || betType == PLAYER_PAIR) {
        return betAmount;  // ✅ 承擔風險
    }

    // 其他情況：莊贏/閒贏
    return betAmount;
}
```

## 正確邏輯總結

### 規則矩陣

| 投注類型 | 遊戲結果 | 結算狀態 | Valid Bet | 理由 |
|---------|---------|---------|-----------|------|
| **莊** | 莊贏 | WIN | 本金 | 承擔風險 |
| **莊** | 閒贏 | LOSE | 本金 | 承擔風險 |
| **莊** | 和局 | **PUSH** | **0** | 沒有風險 |
| **閒** | 莊贏 | LOSE | 本金 | 承擔風險 |
| **閒** | 閒贏 | WIN | 本金 | 承擔風險 |
| **閒** | 和局 | **PUSH** | **0** | 沒有風險 |
| **和** | 和局 | **WIN** | **本金** | 承擔風險 |
| **和** | 莊贏/閒贏 | **LOSE** | **本金** | 承擔風險 |

### 關鍵原則

```
✅ 核心原則: Valid Bet 與「是否承擔風險」有關，與輸贏結果無關

Push 狀態（退款）:
→ 沒有承擔風險
→ Valid Bet = 0

Win/Lose 狀態:
→ 承擔了風險
→ Valid Bet = 投注本金
```

## 實現代碼

```java
@Service
@RequiredArgsConstructor
public class BaccaratValidBetCalculator {

    public BigDecimal calculateValidBet(BaccaratSettlement settlement) {
        BaccaratBetType betType = settlement.getBetType();
        BaccaratOutcome outcome = settlement.getOutcome();
        BigDecimal betAmount = settlement.getBetAmount();

        // 莊閒投注遇和局 → Push → Valid Bet = 0
        if (isPushState(betType, outcome)) {
            return BigDecimal.ZERO;
        }

        // 所有其他情況 → Valid Bet = 本金
        return betAmount;
    }

    private boolean isPushState(BaccaratBetType betType, BaccaratOutcome outcome) {
        return (betType == BaccaratBetType.BANKER || betType == BaccaratBetType.PLAYER)
            && outcome == BaccaratOutcome.TIE;
    }
}
```

## 測試案例

```java
@Test
void testTieBet_ShouldCountFullAmount() {
    // 場景: 投注和局 100 元，結果和局（贏 800 元）
    BaccaratSettlement settlement = BaccaratSettlement.builder()
        .betType(BaccaratBetType.TIE)
        .betAmount(new BigDecimal("100.00"))
        .outcome(BaccaratOutcome.TIE)
        .payout(new BigDecimal("900.00"))  // 本金 100 + 贏金 800
        .build();

    BigDecimal validBet = calculator.calculateValidBet(settlement);

    // 斷言: Valid Bet = 100（不是 800，不是 0）
    assertThat(validBet).isEqualByComparingTo("100.00");
}

@Test
void testBankerBet_TieOutcome_ShouldCountZero() {
    // 場景: 投注莊 100 元，結果和局（退款）
    BaccaratSettlement settlement = BaccaratSettlement.builder()
        .betType(BaccaratBetType.BANKER)
        .betAmount(new BigDecimal("100.00"))
        .outcome(BaccaratOutcome.TIE)
        .payout(new BigDecimal("100.00"))  // 退款
        .build();

    BigDecimal validBet = calculator.calculateValidBet(settlement);

    // 斷言: Valid Bet = 0（Push 狀態）
    assertThat(validBet).isEqualByComparingTo("0.00");
}
```

## 決策總結

✅ **推薦邏輯**:
- 和局投注（Tie Bet）: Valid Bet = 本金（不論輸贏）
- 莊閒投注遇和局: Valid Bet = 0（Push 狀態）

❌ **錯誤理解**:
- 「僅計算輸贏金額」→ 會導致混淆和錯誤實現

## 需要確認的需求

- [ ] 和局投注是否計入返水？（推薦: 是，按本金計）
- [ ] 對子投注（Pair Bet）的 Valid Bet？（推薦: 本金）
- [ ] 是否需要檢測莊閒對打？（推薦: 是）
