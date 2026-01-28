# 輪盤覆蓋率檢測算法設計

## 問題來源
文檔第 3.2.2 節「真人視訊」第 159-166 行提供的輪盤覆蓋率檢測邏輯存在可繞過的漏洞。

**原文**:
> 規則： 如果玩家在一局中下注的號碼超過總號碼數的一定比例（例如 > 25 個號碼，或 > 70% 盤面），則該局所有投注的 Valid Bet 為 0。

## 核心問題分析

### 漏洞 1: 使用「投注項數量」而非「實際號碼覆蓋數量」

**錯誤理解**:
```
假設檢測邏輯：if (投注項數量 > 25) → 觸發風控

玩家投注：
- 項目 1: 紅色（18 個號碼）
- 項目 2: 單數（18 個號碼）
- 項目 3: 一打 (1-12)（12 個號碼）

投注項數量 = 3 → 未觸發風控 ✅

實際覆蓋號碼：
紅色 = {1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36}
單數 = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35}
一打 = {1,2,3,4,5,6,7,8,9,10,11,12}

合併 (集合運算):
紅色 ∪ 單數 ∪ 一打 = 27 個號碼
覆蓋率 = 27/37 = 73% → 應該觸發風控但沒有！
```

### 漏洞 2: 區域投注繞過檢測

```
場景: 玩家想覆蓋 97% 的盤面

❌ 容易被檢測的下注方式:
投注 36 個單號 → 系統記錄 36 個投注項 → 觸發風控

✅ 繞過檢測的下注方式:
- 下注 "1-12"（一打） → 1 個投注項
- 下注 "13-24"（二打） → 1 個投注項
- 下注 "25-36"（三打） → 1 個投注項
→ 系統只記錄 3 個投注項 → 風控未觸發
→ 實際覆蓋 36/37 = 97% 盤面
```

### 漏洞 3: 未處理重疊投注

Evolution Gaming API 的 bet_code 範例:
```json
{
  "round_id": "abc123",
  "bets": [
    {
      "bet_code": "RED",
      "amount": 100,
      "covered_numbers": [1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36]
    },
    {
      "bet_code": "DOZEN_1",  // 一打 (1-12)
      "amount": 50,
      "covered_numbers": [1,2,3,4,5,6,7,8,9,10,11,12]
    }
  ]
}
```

**簡單相加的錯誤**:
```
❌ 錯誤算法:
覆蓋數 = 18 (紅色) + 12 (一打) = 30 個號碼
覆蓋率 = 30/37 = 81%

✅ 正確算法（集合運算）:
紅色 ∩ 一打 = {1,3,5,7,9,12} (6 個重疊)
實際覆蓋 = 18 + 12 - 6 = 24 個號碼
覆蓋率 = 24/37 = 65%
```

## 正確的檢測算法設計

### 算法 1: Bet Code 到號碼集合的映射

```java
/**
 * 輪盤投注代碼解析器
 */
@Component
public class RouletteBetCodeParser {

    // 歐洲輪盤配置（37 個號碼：0-36）
    private static final int TOTAL_NUMBERS_EUROPEAN = 37;

    // Bet Code 映射表（完整定義）
    private static final Map<String, Set<Integer>> BET_CODE_MAPPINGS = Map.ofEntries(
        // 單號（Straight Up）
        entry("STRAIGHT_0", Set.of(0)),
        entry("STRAIGHT_1", Set.of(1)),
        // ... 省略其他單號

        // 顏色
        entry("RED", Set.of(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36)),
        entry("BLACK", Set.of(2,4,6,8,10,11,13,15,17,20,22,24,26,28,29,31,33,35)),

        // 奇偶
        entry("ODD", Set.of(1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35)),
        entry("EVEN", Set.of(2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36)),

        // 高低
        entry("LOW_1_18", Set.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18)),
        entry("HIGH_19_36", Set.of(19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36)),

        // 打（Dozen）
        entry("DOZEN_1", Set.of(1,2,3,4,5,6,7,8,9,10,11,12)),
        entry("DOZEN_2", Set.of(13,14,15,16,17,18,19,20,21,22,23,24)),
        entry("DOZEN_3", Set.of(25,26,27,28,29,30,31,32,33,34,35,36)),

        // 列（Column）
        entry("COLUMN_1", Set.of(1,4,7,10,13,16,19,22,25,28,31,34)),
        entry("COLUMN_2", Set.of(2,5,8,11,14,17,20,23,26,29,32,35)),
        entry("COLUMN_3", Set.of(3,6,9,12,15,18,21,24,27,30,33,36)),

        // 分割（Split）- 示例
        entry("SPLIT_1_2", Set.of(1,2)),
        entry("SPLIT_1_4", Set.of(1,4)),
        // ... 省略其他分割

        // 街（Street）- 示例
        entry("STREET_1_2_3", Set.of(1,2,3)),
        entry("STREET_4_5_6", Set.of(4,5,6))
        // ... 省略其他街
    );

    /**
     * 解析 bet_code 到號碼集合
     */
    public Set<Integer> parseBetCode(String betCode) {
        // 1. 嘗試從映射表查找
        Set<Integer> numbers = BET_CODE_MAPPINGS.get(betCode);
        if (numbers != null) {
            return numbers;
        }

        // 2. 處理動態 bet_code（例如 STRAIGHT_25）
        if (betCode.startsWith("STRAIGHT_")) {
            int number = Integer.parseInt(betCode.substring(9));
            if (number >= 0 && number <= 36) {
                return Set.of(number);
            }
        }

        // 3. 處理自定義 bet_code（某些供應商）
        // 例如: "NUMBERS_1_2_3_4" → {1,2,3,4}
        if (betCode.startsWith("NUMBERS_")) {
            String[] parts = betCode.substring(8).split("_");
            return Arrays.stream(parts)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        }

        // 4. 未知 bet_code → 記錄警告並返回空集
        log.warn("Unknown bet code: {}", betCode);
        return Set.of();
    }
}
```

### 算法 2: 覆蓋率計算（集合運算）

```java
/**
 * 輪盤覆蓋率檢測服務
 */
@Service
@RequiredArgsConstructor
public class RouletteCoverageDetectionService {

    private final RouletteBetCodeParser betCodeParser;

    /**
     * 計算實際覆蓋率
     */
    public CoverageAnalysisResult analyzeCoverage(List<RouletteBet> bets) {
        // 步驟 1: 收集所有覆蓋的號碼（集合合併）
        Set<Integer> coveredNumbers = new HashSet<>();

        for (RouletteBet bet : bets) {
            Set<Integer> numbers = betCodeParser.parseBetCode(bet.getBetCode());
            coveredNumbers.addAll(numbers);  // 集合並集
        }

        // 步驟 2: 計算覆蓋率
        double coverageRate = (double) coveredNumbers.size() / 37;

        // 步驟 3: 檢測對沖模式
        boolean hasOpposite = detectOppositeBets(bets);

        return CoverageAnalysisResult.builder()
            .coveredNumbers(coveredNumbers)
            .coverageRate(coverageRate)
            .hasOppositeBets(hasOpposite)
            .build();
    }

    /**
     * 檢測對沖投注（互斥選項）
     */
    private boolean detectOppositeBets(List<RouletteBet> bets) {
        Set<String> betCodes = bets.stream()
            .map(RouletteBet::getBetCode)
            .collect(Collectors.toSet());

        // 檢測常見的對沖組合
        return (betCodes.contains("RED") && betCodes.contains("BLACK")) ||
               (betCodes.contains("ODD") && betCodes.contains("EVEN")) ||
               (betCodes.contains("LOW_1_18") && betCodes.contains("HIGH_19_36"));
    }

    /**
     * 風控決策
     */
    public ValidBetDecision makeValidBetDecision(
        List<RouletteBet> bets,
        BigDecimal totalBetAmount
    ) {
        // 分析覆蓋率
        CoverageAnalysisResult analysis = analyzeCoverage(bets);

        // 規則 1: 對沖投注（紅+黑、單+雙等）
        if (analysis.isHasOppositeBets()) {
            return ValidBetDecision.builder()
                .validBet(BigDecimal.ZERO)
                .reason("OPPOSITE_BETS_DETECTED")
                .details("Hedging detected: RED+BLACK or ODD+EVEN")
                .build();
        }

        // 規則 2: 高覆蓋率（> 70%）
        if (analysis.getCoverageRate() > 0.70) {
            return ValidBetDecision.builder()
                .validBet(BigDecimal.ZERO)
                .reason("HIGH_COVERAGE")
                .details(String.format("Coverage: %.2f%% (threshold: 70%%)",
                    analysis.getCoverageRate() * 100))
                .build();
        }

        // 規則 3: 正常投注
        return ValidBetDecision.builder()
            .validBet(totalBetAmount)
            .reason("NORMAL_BET")
            .coverageRate(analysis.getCoverageRate())
            .build();
    }
}
```

### 算法 3: 測試案例

```java
@SpringBootTest
class RouletteCoverageDetectionTest {

    @Autowired
    private RouletteCoverageDetectionService service;

    @Test
    @DisplayName("場景 1: 區域投注覆蓋 97% 盤面")
    void testHighCoverage_ThreeDozenBets() {
        // 投注三打（1-12, 13-24, 25-36）
        List<RouletteBet> bets = List.of(
            new RouletteBet("DOZEN_1", new BigDecimal("100")),
            new RouletteBet("DOZEN_2", new BigDecimal("100")),
            new RouletteBet("DOZEN_3", new BigDecimal("100"))
        );

        CoverageAnalysisResult analysis = service.analyzeCoverage(bets);

        // 斷言：覆蓋 36/37 = 97%
        assertThat(analysis.getCoveredNumbers().size()).isEqualTo(36);
        assertThat(analysis.getCoverageRate()).isCloseTo(0.973, offset(0.01));

        // 斷言：應該被風控拒絕
        ValidBetDecision decision = service.makeValidBetDecision(
            bets,
            new BigDecimal("300")
        );

        assertThat(decision.getValidBet()).isEqualByComparingTo("0.00");
        assertThat(decision.getReason()).isEqualTo("HIGH_COVERAGE");
    }

    @Test
    @DisplayName("場景 2: 紅色 + 單數重疊投注")
    void testOverlappingBets_RedAndOdd() {
        List<RouletteBet> bets = List.of(
            new RouletteBet("RED", new BigDecimal("100")),
            new RouletteBet("ODD", new BigDecimal("100"))
        );

        CoverageAnalysisResult analysis = service.analyzeCoverage(bets);

        // 紅色: 18 個號碼
        // 單數: 18 個號碼
        // 重疊: {1,3,5,7,9,19,21,23,25,27} = 10 個
        // 實際覆蓋: 18 + 18 - 10 = 26 個

        assertThat(analysis.getCoveredNumbers().size()).isEqualTo(26);
        assertThat(analysis.getCoverageRate()).isCloseTo(0.703, offset(0.01));

        // 斷言：剛好超過 70% 門檻
        ValidBetDecision decision = service.makeValidBetDecision(
            bets,
            new BigDecimal("200")
        );

        assertThat(decision.getValidBet()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("場景 3: 對沖投注檢測")
    void testOppositeBets_RedAndBlack() {
        List<RouletteBet> bets = List.of(
            new RouletteBet("RED", new BigDecimal("100")),
            new RouletteBet("BLACK", new BigDecimal("100"))
        );

        CoverageAnalysisResult analysis = service.analyzeCoverage(bets);

        // 斷言：檢測到對沖
        assertThat(analysis.isHasOppositeBets()).isTrue();

        // 斷言：Valid Bet = 0
        ValidBetDecision decision = service.makeValidBetDecision(
            bets,
            new BigDecimal("200")
        );

        assertThat(decision.getValidBet()).isEqualByComparingTo("0.00");
        assertThat(decision.getReason()).isEqualTo("OPPOSITE_BETS_DETECTED");
    }

    @Test
    @DisplayName("場景 4: 正常投注（單號 + 分割）")
    void testNormalBets_LowCoverage() {
        List<RouletteBet> bets = List.of(
            new RouletteBet("STRAIGHT_7", new BigDecimal("50")),
            new RouletteBet("SPLIT_7_8", new BigDecimal("30")),
            new RouletteBet("STREET_7_8_9", new BigDecimal("20"))
        );

        CoverageAnalysisResult analysis = service.analyzeCoverage(bets);

        // 覆蓋號碼: {7, 8, 9}
        assertThat(analysis.getCoveredNumbers().size()).isEqualTo(3);
        assertThat(analysis.getCoverageRate()).isCloseTo(0.081, offset(0.01));

        // 斷言：正常投注，Valid Bet = 總金額
        ValidBetDecision decision = service.makeValidBetDecision(
            bets,
            new BigDecimal("100")
        );

        assertThat(decision.getValidBet()).isEqualByComparingTo("100.00");
    }
}
```

## 配置管理

```yaml
# application.yml
roulette:
  coverage_detection:
    # 覆蓋率門檻
    coverage_threshold: 0.70  # 70%

    # 對沖檢測開關
    detect_opposite_bets: true

    # 自定義規則
    custom_rules:
      - name: "VIP 放寬"
        user_segments: ["VIP_GOLD", "VIP_PLATINUM"]
        coverage_threshold: 0.85  # VIP 可以 85%

      - name: "新手保護"
        user_segments: ["NEW_PLAYER"]
        coverage_threshold: 0.50  # 新手只能 50%
```

## 決策總結

✅ **推薦方案**: 基於集合運算的覆蓋率檢測

**理由**:
1. **準確性**: 正確計算實際覆蓋的號碼數量
2. **防繞過**: 無法通過區域投注繞過
3. **靈活性**: 支持自定義規則和門檻

❌ **錯誤方案**: 基於投注項數量的檢測

**風險**:
1. 容易被繞過（使用區域投注）
2. 計算不準確（忽略重疊）
3. 玩家可以低風險刷流水

## 需要確認的需求

- [ ] 覆蓋率門檻設為多少？（推薦: 70%）
- [ ] 是否需要區分 VIP 和普通玩家？
- [ ] 對沖投注是否完全禁止？（推薦: 是）
- [ ] 美式輪盤（38 個號碼）是否需要支持？
