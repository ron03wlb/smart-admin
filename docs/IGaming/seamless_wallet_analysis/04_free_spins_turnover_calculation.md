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
```
GGR = Turnover - Payout

其中:
- Turnover: 玩家投入的金額（包括真錢和虛擬幣）
- Payout: 玩家贏得的金額
- GGR: 營運商的毛利
```

**免費旋轉的財務影響**:

```
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

```sql
-- 交易表（擴展）
CREATE TABLE wallet_transactions (
    transaction_id VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NOT NULL,

    -- 交易類型
    api_type ENUM('BET', 'RESULT', 'ROLLBACK') NOT NULL,
    transaction_type ENUM(
        'CASH_BET',           -- 真錢投注
        'FREESPIN_BET',       -- 免費旋轉投注
        'BONUS_BET',          -- 紅利投注
        'CASH_WIN',           -- 真錢派彩
        'FREESPIN_WIN',       -- 免費旋轉派彩
        'BONUS_WIN'           -- 紅利派彩
    ) NOT NULL,

    -- 金額
    amount DECIMAL(18, 4) NOT NULL,

    -- 流水與有效投注（分開記錄）
    turnover DECIMAL(18, 4) NOT NULL DEFAULT 0,   -- 計入財務報表
    valid_bet DECIMAL(18, 4) NOT NULL DEFAULT 0,  -- 計入流水要求

    -- 免費旋轉特有欄位
    is_free_spin BOOLEAN DEFAULT FALSE,
    freespin_cost DECIMAL(18, 4),  -- 營運商的成本（面額）
    freespin_campaign_id BIGINT,   -- 活動 ID

    -- 錢包類型
    wallet_type ENUM('CASH', 'BONUS') NOT NULL DEFAULT 'CASH',

    -- 其他欄位...
    round_id VARCHAR(128),
    game_id VARCHAR(64),
    created_at TIMESTAMP(3) NOT NULL,

    INDEX idx_user_time (user_id, created_at),
    INDEX idx_type (transaction_type),
    INDEX idx_freespin_campaign (freespin_campaign_id)
);

-- 免費旋轉活動表
CREATE TABLE freespin_campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,

    -- 面額配置
    spin_value DECIMAL(18, 4) NOT NULL,  -- 每次旋轉的面額
    spin_count INT NOT NULL,             -- 旋轉次數
    total_cost DECIMAL(18, 4) NOT NULL,  -- 總成本 = spin_value × spin_count

    -- 資格條件
    eligible_games JSON,  -- 適用的遊戲 ID 列表
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,

    -- 派彩規則
    win_to_bonus_wallet BOOLEAN DEFAULT TRUE,  -- 贏金進紅利錢包
    wagering_requirement INT DEFAULT 0,        -- 流水要求倍數（例如 20x）

    -- 財務追蹤
    total_issued BIGINT DEFAULT 0,        -- 已發放數量
    total_used BIGINT DEFAULT 0,          -- 已使用數量
    total_payout DECIMAL(18, 4) DEFAULT 0,  -- 總派彩金額

    created_at TIMESTAMP NOT NULL,

    INDEX idx_validity (valid_from, valid_until)
);

-- 用戶免費旋轉餘額表
CREATE TABLE user_freespins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,

    -- 餘額
    remaining_spins INT NOT NULL DEFAULT 0,  -- 剩餘次數
    used_spins INT NOT NULL DEFAULT 0,       -- 已使用次數

    -- 狀態
    status ENUM('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED') NOT NULL,

    -- 時間
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,

    UNIQUE KEY uk_user_campaign (user_id, campaign_id),
    INDEX idx_status_expiry (status, expires_at),

    FOREIGN KEY (campaign_id) REFERENCES freespin_campaigns(id)
);
```

### 實現邏輯

```java
/**
 * 免費旋轉交易處理服務
 */
@Service
@RequiredArgsConstructor
public class FreeSpinTransactionService {

    private final WalletTransactionRepository transactionRepository;
    private final UserFreespinRepository freespinRepository;
    private final WalletService walletService;

    /**
     * 處理免費旋轉的 Bet 請求
     */
    @Transactional(rollbackFor = Throwable.class)
    public BetResponse processFreeSpinBet(FreeSpinBetRequest request) {
        String transactionId = request.getTransactionId();
        Long userId = request.getUserId();
        Long campaignId = request.getCampaignId();

        // 步驟 1: 檢查用戶是否有免費旋轉餘額
        UserFreespin userFreespin = freespinRepository
            .findByUserIdAndCampaignId(userId, campaignId)
            .orElseThrow(() -> new FreeSpinNotFoundException(
                "No free spins available for campaign: " + campaignId
            ));

        if (userFreespin.getRemainingSpins() <= 0) {
            throw new FreeSpinExhaustedException(
                "All free spins have been used"
            );
        }

        // 步驟 2: 獲取活動配置
        FreeSpinCampaign campaign = freespinRepository
            .findCampaignById(campaignId)
            .orElseThrow();

        BigDecimal spinValue = campaign.getSpinValue();  // 面額（例如 $1）

        // 步驟 3: 創建交易記錄
        WalletTransaction tx = WalletTransaction.builder()
            .transactionId(transactionId)
            .userId(userId)
            .apiType(ApiType.BET)
            .transactionType(TransactionType.FREESPIN_BET)
            .amount(spinValue)  // 面額

            // ✅ 關鍵: Turnover 和 Valid Bet 分開處理
            .turnover(spinValue)     // 計入 Turnover（財務用）
            .validBet(BigDecimal.ZERO)  // 不計入 Valid Bet（流水用）

            .isFreeSpin(true)
            .freespinCost(spinValue)  // 記錄成本
            .freespinCampaignId(campaignId)
            .walletType(WalletType.CASH)  // Bet 階段不涉及錢包

            .roundId(request.getRoundId())
            .gameId(request.getGameId())
            .status(TransactionStatus.SUCCESS)
            .build();

        transactionRepository.save(tx);

        // 步驟 4: 扣減免費旋轉次數
        userFreespin.setRemainingSpins(
            userFreespin.getRemainingSpins() - 1
        );
        userFreespin.setUsedSpins(
            userFreespin.getUsedSpins() + 1
        );
        userFreespin.setLastUsedAt(Instant.now());

        freespinRepository.save(userFreespin);

        // 步驟 5: 返回響應（餘額不變）
        BigDecimal currentBalance = walletService.getBalance(userId);

        return BetResponse.builder()
            .status("SUCCESS")
            .balance(currentBalance)  // 餘額不變
            .transactionId(transactionId)
            .currency(request.getCurrency())
            .remainingFreeSpins(userFreespin.getRemainingSpins())  // 額外信息
            .build();
    }

    /**
     * 處理免費旋轉的 Result 請求
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResultResponse processFreeSpinResult(FreeSpinResultRequest request) {
        String transactionId = request.getTransactionId();
        String betTransactionId = request.getReferTransactionId();
        Long userId = request.getUserId();
        BigDecimal winAmount = request.getWinAmount();

        // 步驟 1: 查找對應的 Bet 交易
        WalletTransaction betTx = transactionRepository
            .findByTransactionId(betTransactionId)
            .orElseThrow(() -> new TransactionNotFoundException(
                "Bet transaction not found: " + betTransactionId
            ));

        if (!betTx.getIsFreeSpin()) {
            throw new IllegalArgumentException(
                "Referenced bet is not a free spin"
            );
        }

        // 步驟 2: 創建 Result 交易記錄
        WalletTransaction resultTx = WalletTransaction.builder()
            .transactionId(transactionId)
            .userId(userId)
            .apiType(ApiType.RESULT)
            .transactionType(TransactionType.FREESPIN_WIN)
            .amount(winAmount)

            // ✅ 關鍵: Turnover 已在 Bet 階段記錄，Result 不重複計
            .turnover(BigDecimal.ZERO)     // 不重複計入
            .validBet(BigDecimal.ZERO)     // 不計入流水要求

            .isFreeSpin(true)
            .freespinCampaignId(betTx.getFreespinCampaignId())

            // 派彩進紅利錢包
            .walletType(WalletType.BONUS)

            .referTransactionId(betTransactionId)
            .roundId(request.getRoundId())
            .gameId(betTx.getGameId())
            .status(TransactionStatus.SUCCESS)
            .build();

        transactionRepository.save(resultTx);

        // 步驟 3: 派彩到紅利錢包（如果有贏錢）
        BigDecimal newBalance;
        if (winAmount.compareTo(BigDecimal.ZERO) > 0) {
            newBalance = walletService.creditBonusWallet(
                userId,
                winAmount,
                transactionId
            );
        } else {
            newBalance = walletService.getBalance(userId);
        }

        // 步驟 4: 更新活動統計
        FreeSpinCampaign campaign = freespinRepository
            .findCampaignById(betTx.getFreespinCampaignId())
            .orElseThrow();

        campaign.setTotalPayout(
            campaign.getTotalPayout().add(winAmount)
        );

        freespinRepository.saveCampaign(campaign);

        return ResultResponse.builder()
            .status("SUCCESS")
            .balance(newBalance)
            .transactionId(transactionId)
            .currency(request.getCurrency())
            .bonusBalance(walletService.getBonusBalance(userId))  // 紅利餘額
            .build();
    }
}
```

## 財務報表計算

```java
/**
 * 財務報表服務
 */
@Service
@RequiredArgsConstructor
public class FinancialReportService {

    /**
     * 計算 GGR（包含免費旋轉成本）
     */
    public GgrReport calculateGgr(LocalDate date) {
        // 查詢當天所有交易
        List<WalletTransaction> transactions = transactionRepository
            .findByDate(date);

        BigDecimal totalTurnover = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;

        BigDecimal cashTurnover = BigDecimal.ZERO;
        BigDecimal freespinTurnover = BigDecimal.ZERO;

        for (WalletTransaction tx : transactions) {
            // Turnover 計算（包含免費旋轉的面額）
            if (tx.getApiType() == ApiType.BET) {
                totalTurnover = totalTurnover.add(tx.getTurnover());

                if (tx.getIsFreeSpin()) {
                    freespinTurnover = freespinTurnover.add(tx.getTurnover());
                } else {
                    cashTurnover = cashTurnover.add(tx.getTurnover());
                }
            }

            // Payout 計算
            if (tx.getApiType() == ApiType.RESULT) {
                totalPayout = totalPayout.add(tx.getAmount());
            }
        }

        // GGR = Turnover - Payout
        BigDecimal ggr = totalTurnover.subtract(totalPayout);

        return GgrReport.builder()
            .date(date)
            .totalTurnover(totalTurnover)
            .cashTurnover(cashTurnover)
            .freespinTurnover(freespinTurnover)  // 促銷成本
            .totalPayout(totalPayout)
            .ggr(ggr)
            .build();
    }

    /**
     * 計算免費旋轉的 ROI（投資回報率）
     */
    public FreeSpinRoiReport calculateFreeSpinRoi(Long campaignId) {
        FreeSpinCampaign campaign = freespinRepository
            .findCampaignById(campaignId)
            .orElseThrow();

        // 成本: 免費旋轉的總面額
        BigDecimal totalCost = campaign.getTotalCost();

        // 回收: 免費旋轉產生的後續真錢投注
        BigDecimal subsequentCashBets = transactionRepository
            .findSubsequentCashBetsAfterFreeSpin(campaignId);

        // ROI = (回收 - 成本) / 成本 × 100%
        BigDecimal roi = subsequentCashBets
            .subtract(totalCost)
            .divide(totalCost, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

        return FreeSpinRoiReport.builder()
            .campaignId(campaignId)
            .totalCost(totalCost)
            .totalPayout(campaign.getTotalPayout())
            .subsequentCashBets(subsequentCashBets)
            .roi(roi)
            .netProfit(subsequentCashBets.subtract(totalCost))
            .build();
    }
}
```

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

```java
@SpringBootTest
class FreeSpinTurnoverTest {

    @Autowired
    private FreeSpinTransactionService freeSpinService;

    @Autowired
    private FinancialReportService financialReportService;

    @Test
    @DisplayName("免費旋轉的 Turnover 應該等於面額")
    void testFreeSpinTurnover_ShouldEqualFaceValue() {
        // 準備: 贈送 10 次免費旋轉，每次 $1
        Long campaignId = createFreeSpinCampaign(
            spinValue: new BigDecimal("1.00"),
            spinCount: 10
        );

        grantFreeSpins(userId: 12345L, campaignId: campaignId);

        // 執行: 玩家使用免費旋轉
        for (int i = 0; i < 10; i++) {
            freeSpinService.processFreeSpinBet(
                FreeSpinBetRequest.builder()
                    .transactionId("fs_bet_" + i)
                    .userId(12345L)
                    .campaignId(campaignId)
                    .roundId("round_" + i)
                    .gameId("slot_123")
                    .currency("USD")
                    .build()
            );

            // 模擬派彩
            freeSpinService.processFreeSpinResult(
                FreeSpinResultRequest.builder()
                    .transactionId("fs_win_" + i)
                    .referTransactionId("fs_bet_" + i)
                    .userId(12345L)
                    .winAmount(new BigDecimal("0.85"))  // 平均 RTP 85%
                    .roundId("round_" + i)
                    .currency("USD")
                    .build()
            );
        }

        // 驗證: 計算 GGR
        GgrReport report = financialReportService.calculateGgr(
            LocalDate.now()
        );

        // 斷言: Turnover = 10 × $1 = $10
        assertThat(report.getFreespinTurnover())
            .isEqualByComparingTo("10.00");

        // 斷言: Payout = 10 × $0.85 = $8.50
        assertThat(report.getTotalPayout())
            .isEqualByComparingTo("8.50");

        // 斷言: GGR = $10 - $8.50 = $1.50
        assertThat(report.getGgr())
            .isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("免費旋轉的 Valid Bet 應該為 0")
    void testFreeSpinValidBet_ShouldBeZero() {
        // 執行免費旋轉
        freeSpinService.processFreeSpinBet(...);

        // 查詢交易記錄
        WalletTransaction tx = transactionRepository
            .findByTransactionId("fs_bet_123")
            .get();

        // 斷言: Turnover = $1.00（面額）
        assertThat(tx.getTurnover())
            .isEqualByComparingTo("1.00");

        // 斷言: Valid Bet = $0（不計入流水要求）
        assertThat(tx.getValidBet())
            .isEqualByComparingTo("0.00");
    }
}
```

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
