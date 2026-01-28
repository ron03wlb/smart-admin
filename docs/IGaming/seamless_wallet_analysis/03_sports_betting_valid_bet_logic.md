# 體育博彩「贏半/輸半」Valid Bet 計算邏輯

> **重要更新 (v2.0.0 - 2026-01-28)**:
> - ✅ 明確推薦「標準本金法」(Fixed Principal Method)
> - ✅ 標註「實際風險法」(Actual Risk Method) 為不推薦 (已廢棄)
> - ✅ 提供業界標準參考: Pinnacle, Betfair, Pragmatic Play, Evolution Gaming
> - ✅ 更新配置示例: HALF_WIN/HALF_LOSE 使用 FULL_AMOUNT
> - ✅ 與 SmartAdmin 三層驗證架構對齊 (v2.0.0)
>
> **參考文檔**:
> - [術語標準化定義](../00_Concept_&_Analysis/00-03_Terminology_Standards.md)
> - [三層驗證架構流程圖](../02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md)

## 問題來源
文檔第 3.2.1 節稱：「贏半/輸半時，Valid Bet = 50元（即實際輸贏的金額絕對值）」

**這個邏輯與「實際風險暴露」的定義矛盾，也與業界標準不符。**

> ❌ **已廢棄**: 這是「實際風險法」(Actual Risk Method),與 SmartAdmin Layer 2 的 status_factor 邏輯錯誤相同。
>
> ✅ **正確做法**: 採用「標準本金法」(Fixed Principal Method),valid_bet = bet_amount (不論結果)。

## 核心概念釐清

### 1. 亞洲盤口的運作機制

**範例: 讓球 -0.25（又稱 0/-0.5 盤）**

```
投注金額: 100 元
賠率: 1.90

可能結果:
┌────────────┬──────────────────┬──────────────┐
│ 比賽結果   │ 盤口結算         │ 玩家盈虧     │
├────────────┼──────────────────┼──────────────┤
│ 贏 2 球以上│ 全贏             │ +90 元       │
│ 贏 1 球    │ 全贏             │ +90 元       │
│ 平局       │ 輸一半（退一半） │ -50 元       │
│ 輸球       │ 全輸             │ -100 元      │
└────────────┴──────────────────┴──────────────┘

金額計算（平局時）:
- 本金 100 元拆成兩半: 50 元 + 50 元
- 50 元投注在「讓 0 球」→ 平局退款 → 返還 50 元
- 50 元投注在「讓 -0.5 球」→ 輸球 → 損失 50 元
- 最終: 退 50 - 輸 50 = -50 元
```

### 2. 關鍵概念定義

| 概念 | 定義 | 範例（平局結果） | 用途 |
|------|------|----------------|------|
| **投注本金** | 玩家投入的金額 | 100 元 | 計算賠付金額 |
| **實際風險暴露** | 玩家承擔的最大可能損失 | **100 元** | 風控評估 |
| **實際風險實現** | 玩家實際的盈虧絕對值 | 50 元 | 財務結算 |
| **Valid Bet** | 計入流水要求的金額 | **？** | 優惠計算、返水 |

## 業界標準調查

### 主流營運商的 Valid Bet 計算規則

#### 標準 A: 固定本金法（主流）

> ✅ **推薦方案 - 業界主流標準**
>
> **與 SmartAdmin 三層驗證架構對齊**: Layer 1 風控引擎採用此方法一次性判定 valid_bet

**代表**: Pinnacle, Betfair, Pragmatic Play, Evolution Gaming, 大多數歐洲營運商

```
規則: Valid Bet = 投注本金（不論結果）

邏輯:
- Valid Bet 在下注時就確定 (Layer 1 風控層)
- 與最終輸贏結果無關
- 反映玩家的投注意願和風險承擔

範例:
投注 100 元在「讓 -0.25」
→ Valid Bet = 100 元（不論是全贏/全輸/贏半/輸半）

關鍵原則:
✅ 全贏 (WIN) → Valid Bet = 100 元
✅ 全輸 (LOSS) → Valid Bet = 100 元
✅ 贏半 (HALF_WIN) → Valid Bet = 100 元 (不是 50 元!)
✅ 輸半 (HALF_LOSS) → Valid Bet = 100 元 (不是 50 元!)
```

**理由**:
1. **簡化計算**: 不需要等結算才知道 Valid Bet
2. **公平性**: 同樣的投注行為，同樣的流水貢獻
3. **風控邏輯**: 玩家下注時承擔的風險是 100 元，不是 50 元
4. **與 SmartAdmin 三層驗證架構對齊**: Layer 2 (財務中心) 僅記錄結算狀態,不修改 valid_bet

#### 標準 B: 實際風險法（少數）

> ❌ **不推薦 - 違反公平性原則**
>
> **與 SmartAdmin 三層驗證架構衝突**: 這就是 Layer 2 的 status_factor 邏輯錯誤 (已廢棄)

**代表**: 部分亞洲營運商（如沙巴體育的某些活動規則）

```
規則: Valid Bet = 實際輸贏金額絕對值

邏輯:
- Valid Bet 根據結算結果計算
- 反映「真正發生的資金流動」

範例:
投注 100 元在「讓 -0.25」，結果平局
→ 實際損失 50 元
→ Valid Bet = 50 元 ❌

問題場景:
兩位玩家都投注 100 元在「讓 -0.25」:
- 玩家 A 的比賽結果: 全贏 → valid_bet = 100 元 ✓
- 玩家 B 的比賽結果: 平局(輸半) → valid_bet = 50 元 ❌

矛盾:
- 相同的投注行為
- 相同的風險暴露 (100 元)
- 但 valid_bet 不同 → 違反公平性原則
```

**問題**:
1. **計算複雜**: 需要等結算後才能計算
2. **不公平**: 同樣的投注行為，不同的流水貢獻
3. **風控矛盾**: 鼓勵玩家選擇「可能贏半/輸半」的投注
4. **與 SmartAdmin Layer 2 的 status_factor 邏輯相同** (已在 v2.0.0 廢棄)

#### 標準 C: 賠率調整法（罕見）

**代表**: 極少數營運商的 VIP 計畫

```
規則: Valid Bet = 本金 × 賠率係數

邏輯:
- 考慮賠率對風險的影響
- 高賠率投注有更高的流水貢獻

範例:
投注 100 元，賠率 1.50
→ Valid Bet = 100 × 0.75 = 75 元

投注 100 元，賠率 2.50
→ Valid Bet = 100 × 1.25 = 125 元
```

## 詳細邏輯推導

### 場景分析: 為什麼應該使用「本金法」？

**場景 1: 相同投注，不同結果**

```
玩家 A 和玩家 B 都投注 100 元在「讓 -0.25」

玩家 A 的比賽結果: 全贏
→ 盈虧: +90 元
→ 風險暴露: 100 元
→ Valid Bet (本金法): 100 元
→ Valid Bet (實際風險法): 100 元

玩家 B 的比賽結果: 平局（輸半）
→ 盈虧: -50 元
→ 風險暴露: 100 元
→ Valid Bet (本金法): 100 元
→ Valid Bet (實際風險法): 50 元  ← 不公平！

問題:
- 兩位玩家的投注行為完全相同
- 承擔的風險完全相同
- 但 Valid Bet 不同 → 違反公平性原則
```

**場景 2: 活動套利漏洞**

```
活動: 體育投注流水達 1000 元，送 100 元紅利

策略（如果使用實際風險法）:
1. 專門尋找「可能贏半/輸半」的盤口
2. 投注 2000 元（預期 Valid Bet = 1000 元）
3. 平均損失: 2000 × 50% × 50% = 500 元
4. 獲得紅利: 100 元
5. 淨損失: 400 元

但如果使用本金法:
- 無法透過選擇特定盤口來降低流水成本
- 必須真正投注 1000 元
```

**場景 3: 風控評估矛盾**

```
風控視角:

玩家投注 100 元在「讓 -0.25」時，風控系統評估:
- 最大可能損失: 100 元
- 需要鎖定餘額: 100 元
- 信用額度占用: 100 元

如果 Valid Bet 只計算 50 元（實際風險法）:
→ 風控鎖定 100 元，但流水只貢獻 50 元
→ 不對等的風險和收益
→ 不利於營運商
```

## 推薦實現方案

### 方案 1: 標準本金法（推薦）

```java
/**
 * 體育博彩 Valid Bet 計算（標準本金法）
 */
@Service
public class SportsValidBetCalculator {

    public BigDecimal calculateValidBet(SportsBetSettlement settlement) {
        // 步驟 1: 檢查賠率門檻
        if (!meetsOddsThreshold(settlement.getOdds())) {
            return BigDecimal.ZERO;  // 賠率太低，不計入 Valid Bet
        }

        // 步驟 2: 檢查結算狀態
        SettlementStatus status = settlement.getStatus();

        if (status == SettlementStatus.VOID ||
            status == SettlementStatus.CANCELLED ||
            status == SettlementStatus.PUSH) {
            // 作廢/取消/退款 → 沒有承擔風險
            return BigDecimal.ZERO;
        }

        // 步驟 3: 所有其他狀態（WIN, LOSE, HALF_WIN, HALF_LOSE）
        // Valid Bet = 投注本金
        return settlement.getBetAmount();
    }

    /**
     * 檢查賠率是否滿足門檻
     */
    private boolean meetsOddsThreshold(BigDecimal odds) {
        // 歐洲盤最低賠率: 1.50
        BigDecimal minOddsDecimal = new BigDecimal("1.50");

        // 香港盤最低賠率: 0.50
        BigDecimal minOddsHK = new BigDecimal("0.50");

        // 根據賠率格式判斷
        if (odds.compareTo(BigDecimal.ONE) > 0) {
            // 歐洲盤（> 1.00）
            return odds.compareTo(minOddsDecimal) >= 0;
        } else {
            // 香港盤（0-1 之間）
            return odds.compareTo(minOddsHK) >= 0;
        }
    }
}
```

### 方案 2: 賠率調整法（進階）

```java
/**
 * 體育博彩 Valid Bet 計算（賠率調整法）
 * 適用於 VIP 計畫或精細化的流水計算
 */
@Service
public class AdvancedSportsValidBetCalculator {

    /**
     * 賠率係數表（根據賠率範圍調整 Valid Bet）
     */
    private static final Map<Range<BigDecimal>, BigDecimal> ODDS_MULTIPLIERS =
        Map.of(
            // 超低賠率（1.01 - 1.20）→ 不計入或極低權重
            Range.closed(new BigDecimal("1.01"), new BigDecimal("1.20")),
            BigDecimal.ZERO,

            // 低賠率（1.21 - 1.50）→ 部分計入
            Range.closed(new BigDecimal("1.21"), new BigDecimal("1.50")),
            new BigDecimal("0.50"),

            // 正常賠率（1.51 - 2.00）→ 全額計入
            Range.closed(new BigDecimal("1.51"), new BigDecimal("2.00")),
            BigDecimal.ONE,

            // 高賠率（2.01 - 5.00）→ 超額計入
            Range.closed(new BigDecimal("2.01"), new BigDecimal("5.00")),
            new BigDecimal("1.25"),

            // 超高賠率（> 5.00）→ 封頂
            Range.atLeast(new BigDecimal("5.01")),
            new BigDecimal("1.50")
        );

    public BigDecimal calculateValidBet(SportsBetSettlement settlement) {
        // 檢查結算狀態（VOID/CANCELLED/PUSH → 0）
        if (isInvalidStatus(settlement.getStatus())) {
            return BigDecimal.ZERO;
        }

        BigDecimal betAmount = settlement.getBetAmount();
        BigDecimal odds = settlement.getOdds();

        // 查找賠率係數
        BigDecimal multiplier = ODDS_MULTIPLIERS.entrySet().stream()
            .filter(entry -> entry.getKey().contains(odds))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(BigDecimal.ONE);

        // Valid Bet = 本金 × 賠率係數
        return betAmount.multiply(multiplier);
    }

    private boolean isInvalidStatus(SettlementStatus status) {
        return status == SettlementStatus.VOID ||
               status == SettlementStatus.CANCELLED ||
               status == SettlementStatus.PUSH;
    }
}
```

## 配置化管理

> ✅ **推薦配置 - 採用標準本金法**

```yaml
# application.yml
sports:
  valid-bet:
    # 計算方法: FIXED_PRINCIPAL（本金法 - 推薦）| ACTUAL_RISK（實際風險法 - 已廢棄）| ODDS_ADJUSTED（賠率調整法 - 罕見）
    calculation_method: FIXED_PRINCIPAL  # ✅ 推薦: 與 SmartAdmin 三層驗證架構對齊

    # 賠率門檻配置
    odds_threshold:
      decimal: 1.50    # 歐洲盤最低賠率
      hongkong: 0.50   # 香港盤最低賠率
      american: -200   # 美式盤最低賠率

    # 結算狀態與 Valid Bet 的映射 (標準本金法)
    # ✅ 關鍵: HALF_WIN/HALF_LOSE 使用 FULL_AMOUNT,不是 HALF_AMOUNT!
    settlement_status_rules:
      WIN: FULL_AMOUNT           # 全贏 → 全額計入
      LOSE: FULL_AMOUNT          # 全輸 → 全額計入
      HALF_WIN: FULL_AMOUNT      # 贏半 → 全額計入 ✅ (不是 50%!)
      HALF_LOSE: FULL_AMOUNT     # 輸半 → 全額計入 ✅ (不是 50%!)
      PUSH: ZERO                 # 退款 → 不計入 (無風險承擔)
      VOID: ZERO                 # 作廢 → 不計入 (無風險承擔)
      CANCELLED: ZERO            # 取消 → 不計入 (無風險承擔)

    # 賠率調整係數（僅當 calculation_method = ODDS_ADJUSTED 時生效）
    odds_multipliers:
      - range: [1.01, 1.20]
        multiplier: 0.0
      - range: [1.21, 1.50]
        multiplier: 0.5
      - range: [1.51, 2.00]
        multiplier: 1.0
      - range: [2.01, 5.00]
        multiplier: 1.25
      - range: [5.01, 999.99]
        multiplier: 1.5
```

## 數據庫設計

```sql
-- 體育投注結算表
CREATE TABLE sports_bet_settlements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,

    -- 投注信息
    bet_amount DECIMAL(18, 4) NOT NULL,
    odds DECIMAL(10, 4) NOT NULL,
    odds_format ENUM('DECIMAL', 'HONGKONG', 'AMERICAN') NOT NULL,
    handicap VARCHAR(20),  -- 例如: "-0.25", "+1.5"

    -- 結算信息
    settlement_status ENUM(
        'WIN',           -- 全贏
        'LOSE',          -- 全輸
        'HALF_WIN',      -- 贏半
        'HALF_LOSE',     -- 輸半
        'PUSH',          -- 退款
        'VOID',          -- 作廢
        'CANCELLED'      -- 取消
    ) NOT NULL,
    payout_amount DECIMAL(18, 4),  -- 實際派彩金額

    -- 流水計算
    turnover DECIMAL(18, 4) NOT NULL,  -- 流水（一般 = bet_amount）
    valid_bet DECIMAL(18, 4) NOT NULL,  -- 有效投注

    -- 計算依據
    valid_bet_calculation_method VARCHAR(50),  -- 計算方法
    odds_multiplier DECIMAL(10, 4),  -- 賠率係數（如果使用賠率調整法）

    -- 時間戳
    created_at TIMESTAMP(3) NOT NULL,
    settled_at TIMESTAMP(3),

    INDEX idx_user_time (user_id, settled_at),
    INDEX idx_status (settlement_status)
);
```

## 測試案例

```java
@SpringBootTest
class SportsValidBetCalculatorTest {

    @Autowired
    private SportsValidBetCalculator calculator;

    @Test
    @DisplayName("贏半/輸半應該計入全額 Valid Bet")
    void testHalfWinLose_ShouldCountFullAmount() {
        // 場景: 投注 100 元，讓 -0.25，結果平局（輸半）
        SportsBetSettlement settlement = SportsBetSettlement.builder()
            .betAmount(new BigDecimal("100.00"))
            .odds(new BigDecimal("1.90"))
            .settlementStatus(SettlementStatus.HALF_LOSE)
            .payoutAmount(new BigDecimal("50.00"))  // 退回 50
            .build();

        BigDecimal validBet = calculator.calculateValidBet(settlement);

        // 斷言: Valid Bet = 100（不是 50）
        assertThat(validBet).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("退款應該不計入 Valid Bet")
    void testPush_ShouldCountZero() {
        SportsBetSettlement settlement = SportsBetSettlement.builder()
            .betAmount(new BigDecimal("100.00"))
            .odds(new BigDecimal("1.90"))
            .settlementStatus(SettlementStatus.PUSH)
            .payoutAmount(new BigDecimal("100.00"))  // 全額退回
            .build();

        BigDecimal validBet = calculator.calculateValidBet(settlement);

        // 斷言: Valid Bet = 0
        assertThat(validBet).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("低賠率應該不計入 Valid Bet")
    void testLowOdds_ShouldCountZero() {
        SportsBetSettlement settlement = SportsBetSettlement.builder()
            .betAmount(new BigDecimal("100.00"))
            .odds(new BigDecimal("1.05"))  // 極低賠率
            .settlementStatus(SettlementStatus.WIN)
            .payoutAmount(new BigDecimal("105.00"))
            .build();

        BigDecimal validBet = calculator.calculateValidBet(settlement);

        // 斷言: Valid Bet = 0（賠率低於門檻）
        assertThat(validBet).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("不同結算狀態，相同 Valid Bet（公平性測試）")
    void testDifferentOutcomes_SameValidBet() {
        BigDecimal betAmount = new BigDecimal("100.00");
        BigDecimal odds = new BigDecimal("1.90");

        // 場景 1: 全贏
        SportsBetSettlement win = createSettlement(
            betAmount, odds, SettlementStatus.WIN, new BigDecimal("190.00")
        );

        // 場景 2: 全輸
        SportsBetSettlement lose = createSettlement(
            betAmount, odds, SettlementStatus.LOSE, BigDecimal.ZERO
        );

        // 場景 3: 贏半
        SportsBetSettlement halfWin = createSettlement(
            betAmount, odds, SettlementStatus.HALF_WIN, new BigDecimal("145.00")
        );

        // 場景 4: 輸半
        SportsBetSettlement halfLose = createSettlement(
            betAmount, odds, SettlementStatus.HALF_LOSE, new BigDecimal("50.00")
        );

        // 斷言: 所有場景的 Valid Bet 都應該相同
        assertThat(calculator.calculateValidBet(win))
            .isEqualByComparingTo("100.00");

        assertThat(calculator.calculateValidBet(lose))
            .isEqualByComparingTo("100.00");

        assertThat(calculator.calculateValidBet(halfWin))
            .isEqualByComparingTo("100.00");

        assertThat(calculator.calculateValidBet(halfLose))
            .isEqualByComparingTo("100.00");
    }
}
```

## 決策總結

✅ **推薦方案**: 標準本金法

**理由**:
1. **公平性**: 相同投注行為，相同流水貢獻
2. **簡單性**: 不需要等結算就能確定 Valid Bet
3. **業界標準**: 大多數主流營運商採用
4. **風控一致**: 與風險暴露評估邏輯一致

❌ **不推薦**: 實際風險法

**問題**:
1. 違反公平性原則
2. 存在活動套利漏洞
3. 與風控邏輯矛盾
4. 計算複雜（需要等結算）

## 需要確認的需求

- [ ] 採用哪種 Valid Bet 計算方法？（推薦: 標準本金法）
- [ ] 賠率門檻是多少？（推薦: 歐洲盤 ≥ 1.50）
- [ ] 是否需要支持賠率調整法？（進階功能）
- [ ] 不同結算狀態的 Valid Bet 規則？（推薦: WIN/LOSE/HALF_WIN/HALF_LOSE = 全額，PUSH/VOID/CANCELLED = 0）
- [ ] 串關投注的 Valid Bet 如何計算？
