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

    /**
     * Bet Code 映射表（完整定義）
     *
     * v2.0.0 更新: 補充完整映射表，包含所有常見投注類型
     * 參考: Evolution Gaming Roulette API v3.2
     */
    private static final Map<String, BetCodeInfo> BET_CODE_MAPPINGS = Map.ofEntries(
        // ==================== 內註 (Inside Bets) ====================

        // 單號 (Straight Up) - 37 個號碼 (0-36)
        // 覆蓋率: 1/37 = 2.7%, 賠率: 35:1
        entry("STRAIGHT_0", new BetCodeInfo(Set.of(0), 2.7, 35, "LOW")),
        entry("STRAIGHT_1", new BetCodeInfo(Set.of(1), 2.7, 35, "LOW")),
        // ... 可通過動態解析處理 STRAIGHT_{0-36}

        // 分割 (Split) - 2 個相鄰號碼
        // 覆蓋率: 2/37 = 5.4%, 賠率: 17:1
        entry("SPLIT_1_2", new BetCodeInfo(Set.of(1,2), 5.4, 17, "LOW")),
        entry("SPLIT_1_4", new BetCodeInfo(Set.of(1,4), 5.4, 17, "LOW")),
        entry("SPLIT_2_3", new BetCodeInfo(Set.of(2,3), 5.4, 17, "LOW")),
        // ... 共 60 種 Split 組合 (完整列表參見附錄 A)

        // 街 (Street) - 3 個橫排號碼
        // 覆蓋率: 3/37 = 8.1%, 賠率: 11:1
        entry("STREET_1_2_3", new BetCodeInfo(Set.of(1,2,3), 8.1, 11, "LOW")),
        entry("STREET_4_5_6", new BetCodeInfo(Set.of(4,5,6), 8.1, 11, "LOW")),
        entry("STREET_7_8_9", new BetCodeInfo(Set.of(7,8,9), 8.1, 11, "LOW")),
        // ... 共 12 種 Street 組合

        // 角 (Corner) - 4 個號碼的方塊
        // 覆蓋率: 4/37 = 10.8%, 賠率: 8:1
        entry("CORNER_1_2_4_5", new BetCodeInfo(Set.of(1,2,4,5), 10.8, 8, "MEDIUM")),
        entry("CORNER_2_3_5_6", new BetCodeInfo(Set.of(2,3,5,6), 10.8, 8, "MEDIUM")),
        // ... 共 22 種 Corner 組合 (完整列表參見附錄 B)

        // 線 (Line) - 6 個號碼 (兩個街)
        // 覆蓋率: 6/37 = 16.2%, 賠率: 5:1
        entry("LINE_1_2_3_4_5_6", new BetCodeInfo(Set.of(1,2,3,4,5,6), 16.2, 5, "MEDIUM")),
        entry("LINE_4_5_6_7_8_9", new BetCodeInfo(Set.of(4,5,6,7,8,9), 16.2, 5, "MEDIUM")),
        // ... 共 11 種 Line 組合

        // ==================== 外註 (Outside Bets) ====================

        // 顏色 (Color)
        // 覆蓋率: 18/37 = 48.6%, 賠率: 1:1
        entry("RED", new BetCodeInfo(
            Set.of(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36),
            48.6, 1, "HIGH"
        )),
        entry("BLACK", new BetCodeInfo(
            Set.of(2,4,6,8,10,11,13,15,17,20,22,24,26,28,29,31,33,35),
            48.6, 1, "HIGH"
        )),

        // 奇偶 (Odd/Even)
        // 覆蓋率: 18/37 = 48.6%, 賠率: 1:1
        entry("ODD", new BetCodeInfo(
            Set.of(1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35),
            48.6, 1, "HIGH"
        )),
        entry("EVEN", new BetCodeInfo(
            Set.of(2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36),
            48.6, 1, "HIGH"
        )),

        // 高低 (High/Low)
        // 覆蓋率: 18/37 = 48.6%, 賠率: 1:1
        entry("LOW_1_18", new BetCodeInfo(
            Set.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18),
            48.6, 1, "HIGH"
        )),
        entry("HIGH_19_36", new BetCodeInfo(
            Set.of(19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36),
            48.6, 1, "HIGH"
        )),

        // 打 (Dozen)
        // 覆蓋率: 12/37 = 32.4%, 賠率: 2:1
        entry("DOZEN_1", new BetCodeInfo(
            Set.of(1,2,3,4,5,6,7,8,9,10,11,12),
            32.4, 2, "MEDIUM"
        )),
        entry("DOZEN_2", new BetCodeInfo(
            Set.of(13,14,15,16,17,18,19,20,21,22,23,24),
            32.4, 2, "MEDIUM"
        )),
        entry("DOZEN_3", new BetCodeInfo(
            Set.of(25,26,27,28,29,30,31,32,33,34,35,36),
            32.4, 2, "MEDIUM"
        )),

        // 列 (Column)
        // 覆蓋率: 12/37 = 32.4%, 賠率: 2:1
        entry("COLUMN_1", new BetCodeInfo(
            Set.of(1,4,7,10,13,16,19,22,25,28,31,34),
            32.4, 2, "MEDIUM"
        )),
        entry("COLUMN_2", new BetCodeInfo(
            Set.of(2,5,8,11,14,17,20,23,26,29,32,35),
            32.4, 2, "MEDIUM"
        )),
        entry("COLUMN_3", new BetCodeInfo(
            Set.of(3,6,9,12,15,18,21,24,27,30,33,36),
            32.4, 2, "MEDIUM"
        )),

        // ==================== 特殊組合 (Special Bets) ====================

        // 鄰居投注 (Neighbours) - 法式輪盤常見
        entry("ORPHELINS", new BetCodeInfo(
            Set.of(1,6,9,14,17,20,31,34),
            21.6, -1, "MEDIUM"  // 賠率變動
        )),
        entry("VOISINS_DU_ZERO", new BetCodeInfo(
            Set.of(22,18,29,7,28,12,35,3,26,0,32,15,19,4,21,2,25),
            45.9, -1, "HIGH"
        )),
        entry("TIERS_DU_CYLINDRE", new BetCodeInfo(
            Set.of(27,13,36,11,30,8,23,10,5,24,16,33),
            32.4, -1, "MEDIUM"
        ))
    );

    /**
     * Bet Code 信息數據類
     */
    @Value
    static class BetCodeInfo {
        Set<Integer> coveredNumbers;  // 覆蓋的號碼集合
        double coverageRate;           // 覆蓋率 (%)
        int payoutRatio;               // 賠率 (-1 表示變動)
        String riskLevel;              // 風險級別: LOW/MEDIUM/HIGH
    }

    /**
     * 解析 bet_code 到號碼集合 (v2.0.0 增強版)
     *
     * @return BetCodeInfo 包含覆蓋號碼、覆蓋率、賠率、風險級別
     */
    public BetCodeInfo parseBetCode(String betCode) {
        // 1. 嘗試從映射表查找
        BetCodeInfo info = BET_CODE_MAPPINGS.get(betCode);
        if (info != null) {
            return info;
        }

        // 2. 處理動態 bet_code (STRAIGHT_0 到 STRAIGHT_36)
        if (betCode.startsWith("STRAIGHT_")) {
            int number = Integer.parseInt(betCode.substring(9));
            if (number >= 0 && number <= 36) {
                return new BetCodeInfo(Set.of(number), 2.7, 35, "LOW");
            }
        }

        // 3. 處理自定義 bet_code (某些供應商允許自定義組合)
        // 例如: "NUMBERS_1_2_3_4" → {1,2,3,4}
        if (betCode.startsWith("NUMBERS_")) {
            String[] parts = betCode.substring(8).split("_");
            Set<Integer> numbers = Arrays.stream(parts)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

            double coverage = (double) numbers.size() / 37 * 100;
            String risk = coverage > 40 ? "HIGH" : coverage > 20 ? "MEDIUM" : "LOW";

            return new BetCodeInfo(numbers, coverage, -1, risk);
        }

        // 4. 未知 bet_code → 記錄警告並返回空集
        log.warn("Unknown bet code: {}, treating as zero coverage", betCode);
        return new BetCodeInfo(Set.of(), 0, 0, "UNKNOWN");
    }

    /**
     * 獲取所有支持的 Bet Code (用於驗證)
     */
    public Set<String> getSupportedBetCodes() {
        return BET_CODE_MAPPINGS.keySet();
    }

    /**
     * 檢查 Bet Code 有效性
     */
    public boolean isValidBetCode(String betCode) {
        return BET_CODE_MAPPINGS.containsKey(betCode)
            || betCode.startsWith("STRAIGHT_")
            || betCode.startsWith("NUMBERS_");
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
     * 計算實際覆蓋率 (v2.0.0 增強版 - 使用 BetCodeInfo)
     */
    public CoverageAnalysisResult analyzeCoverage(List<RouletteBet> bets) {
        // 步驟 1: 收集所有覆蓋的號碼（集合合併）
        Set<Integer> coveredNumbers = new HashSet<>();
        Map<String, BetCodeInfo> betDetails = new HashMap<>();

        for (RouletteBet bet : bets) {
            BetCodeInfo info = betCodeParser.parseBetCode(bet.getBetCode());
            coveredNumbers.addAll(info.getCoveredNumbers());  // 集合並集
            betDetails.put(bet.getBetCode(), info);  // 保存詳細信息用於風控分析
        }

        // 步驟 2: 計算覆蓋率
        double coverageRate = (double) coveredNumbers.size() / 37;

        // 步驟 3: 檢測對沖模式
        boolean hasOpposite = detectOppositeBets(bets);

        // 步驟 4: 計算綜合風險級別
        String overallRiskLevel = calculateOverallRiskLevel(betDetails, coverageRate);

        return CoverageAnalysisResult.builder()
            .coveredNumbers(coveredNumbers)
            .coverageRate(coverageRate)
            .hasOppositeBets(hasOpposite)
            .overallRiskLevel(overallRiskLevel)
            .betDetails(betDetails)
            .build();
    }

    /**
     * 計算綜合風險級別
     */
    private String calculateOverallRiskLevel(
        Map<String, BetCodeInfo> betDetails,
        double coverageRate
    ) {
        // 如果覆蓋率超過 70%，強制 HIGH 風險
        if (coverageRate > 0.70) {
            return "HIGH";
        }

        // 否則根據投注組合判斷
        long highRiskCount = betDetails.values().stream()
            .filter(info -> "HIGH".equals(info.getRiskLevel()))
            .count();

        if (highRiskCount > 0) {
            return "HIGH";
        }

        long mediumRiskCount = betDetails.values().stream()
            .filter(info -> "MEDIUM".equals(info.getRiskLevel()))
            .count();

        return mediumRiskCount > 0 ? "MEDIUM" : "LOW";
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
            .overallRiskLevel(analysis.getOverallRiskLevel())
            .build();
    }

    /**
     * 覆蓋率分析結果 (v2.0.0 增強版)
     */
    @Value
    @Builder
    static class CoverageAnalysisResult {
        Set<Integer> coveredNumbers;              // 覆蓋的號碼集合
        double coverageRate;                       // 覆蓋率 (0.0 - 1.0)
        boolean hasOppositeBets;                   // 是否檢測到對沖投注
        String overallRiskLevel;                   // 綜合風險級別: LOW/MEDIUM/HIGH
        Map<String, BetCodeInfo> betDetails;      // 每個投注的詳細信息
    }

    /**
     * Valid Bet 決策結果
     */
    @Value
    @Builder
    static class ValidBetDecision {
        BigDecimal validBet;        // 實際有效投注金額
        String reason;              // 決策原因
        String details;             // 詳細信息（可選）
        Double coverageRate;        // 覆蓋率（可選）
        String overallRiskLevel;    // 風險級別（可選）
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
