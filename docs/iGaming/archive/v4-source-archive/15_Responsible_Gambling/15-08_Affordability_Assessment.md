# 15-08 Affordability Assessment (可負擔性評估)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

可負擔性評估 (Affordability Assessment) 是 UK Gambling Commission 2025 年新規的核心要求，用於評估玩家的經濟能力，確保博彩活動不超出其可負擔範圍。

### 監管要求

| 監管機構 | 條款 | 生效日期 | 主要要求 |
|---------|------|---------|---------|
| **UKGC** | 2025 新規 | 2025-01 | 強制性財務評估 |
| **Netherlands** | KOA Remote Gambling | 2024 | 年度可負擔性聲明 |
| **Germany** | GlüStV 2021 | 2021 | €1,000/月上限 |

---

## UK 2025 新規詳解

### 觸發條件

UK Gambling Commission 降低了強制財務評估的閾值：

| 觸發條件 | 舊規（2024 前） | 新規（2025） |
|---------|---------------|-------------|
| 淨虧損閾值 | £2,000/年 | £500/年 |
| 存款速度 | 無 | £500/24h 內多次存款 |
| 高風險標記 | 營運商自行決定 | 強制規則 |

### 評估等級

| 等級 | 觸發條件 | 要求動作 |
|------|---------|---------|
| **Basic** | 淨虧損 £125-£500 | 顯示警告訊息 |
| **Enhanced** | 淨虧損 £500-£2,000 | 玩家自我聲明 |
| **Full** | 淨虧損 > £2,000 | 第三方數據驗證 |

### 評估內容

**玩家自我聲明**:
- 年收入範圍
- 住房狀態（租/買）
- 家庭人口數
- 每月可支配收入

**第三方數據驗證**:
- Open Banking 數據
- 信用參考機構數據
- 公開財務記錄

---

## 財務脆弱性月淨存款觸發規則 (UKGC 2025-02-28) 🆕

> **監管要求**: 從 2025 年 2 月 28 日起，運營商必須對月淨存款 ≥£150 的玩家進行財務脆弱性檢查。

### 觸發條件對比

本規則與上述年度淨虧損規則**並行生效**，是獨立的觸發條件：

| 規則類型 | 計算週期 | 計算公式 | 閾值 | 生效日期 |
|---------|---------|---------|------|---------|
| 年度淨虧損 | 12 個月滾動 | 虧損金額 - 盈利金額 | £125/£500/£2,000 | 2025-01 |
| **月淨存款** 🆕 | 30 天滾動 | 存款金額 - 提款金額 | **≥£150** | **2025-02-28** |

### 月淨存款計算

```sql
-- 計算玩家過去 30 天的月淨存款
SELECT
    player_id,
    SUM(CASE WHEN transaction_type = 'DEPOSIT' THEN amount ELSE 0 END) AS total_deposits,
    SUM(CASE WHEN transaction_type = 'WITHDRAWAL' THEN amount ELSE 0 END) AS total_withdrawals,
    SUM(CASE WHEN transaction_type = 'DEPOSIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN transaction_type = 'WITHDRAWAL' THEN amount ELSE 0 END) AS monthly_net_deposit
FROM t_transaction
WHERE jurisdiction = 'UKGC'
  AND transaction_date >= CURRENT_DATE - INTERVAL 30 DAY
GROUP BY player_id
HAVING monthly_net_deposit >= 150;
```

### 排程任務實現

```java
/**
 * 月淨存款 £150 財務脆弱性檢查 (UKGC 2025-02-28)
 *
 * 每日執行，識別達到閾值但尚未完成檢查的玩家
 */
@Scheduled(cron = "0 0 2 * * ?") // 每日凌晨 2:00 執行
public void checkMonthlyNetDepositThreshold() {
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    BigDecimal threshold = new BigDecimal("150");

    // 查詢達到閾值的 UK 玩家
    List<PlayerNetDepositDTO> triggeredPlayers = transactionDao.findPlayersExceedingMonthlyNetDeposit(
        "UKGC",
        thirtyDaysAgo,
        threshold
    );

    for (PlayerNetDepositDTO player : triggeredPlayers) {
        // 檢查是否已有 30 天內的有效評估
        boolean hasRecentCheck = vulnerabilityCheckDao.hasRecentCheck(
            player.getPlayerId(),
            VulnerabilityCheckType.MONTHLY_NET_DEPOSIT,
            thirtyDaysAgo
        );

        if (!hasRecentCheck) {
            // 創建財務脆弱性檢查任務
            createVulnerabilityCheckTask(player, TriggerReason.MONTHLY_NET_DEPOSIT_150);

            // 發送通知給玩家
            notificationService.sendFinancialCheckReminder(player.getPlayerId());

            log.info("Monthly net deposit threshold triggered: playerId={}, amount=£{}",
                player.getPlayerId(), player.getMonthlyNetDeposit());
        }
    }
}
```

### 資料庫擴展

```sql
-- 擴展財務脆弱性檢查表，支持新的觸發類型
ALTER TABLE t_financial_vulnerability_check
ADD COLUMN trigger_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL_NET_LOSS'
COMMENT '觸發類型: ANNUAL_NET_LOSS, MONTHLY_NET_DEPOSIT, DEPOSIT_VELOCITY, MANUAL';

-- 新增索引支持月淨存款查詢
CREATE INDEX idx_transaction_jurisdiction_date
ON t_transaction (jurisdiction, transaction_date, player_id);
```

### 與現有評估的關係

```
玩家活動
    │
    ├── 年度淨虧損 ≥ £125 ──► Basic 評估（顯示警告）
    ├── 年度淨虧損 ≥ £500 ──► Enhanced 評估（自我聲明）
    ├── 年度淨虧損 ≥ £2,000 ──► Full 評估（第三方驗證）
    │
    └── 月淨存款 ≥ £150 ──► 財務脆弱性基礎檢查 🆕
                              │
                              └── 結合風險指標判斷是否升級
```

### 合規狀態追蹤

此規則的實現狀態追蹤於 [06-12 合規時間線](../06_Platform_Governance/06-12_Compliance_Timeline.md)。

---

## 技術實現

### 資料庫設計

```sql
-- 可負擔性評估記錄
CREATE TABLE t_affordability_assessment (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    assessment_type     VARCHAR(20) NOT NULL,  -- BASIC, ENHANCED, FULL
    trigger_reason      VARCHAR(50) NOT NULL,  -- NET_LOSS, DEPOSIT_VELOCITY, MANUAL
    trigger_value       DECIMAL(18,2),         -- 觸發時的數值

    -- 玩家聲明數據
    declared_income_range       VARCHAR(20),   -- 10K-20K, 20K-30K, etc.
    declared_housing_status     VARCHAR(20),   -- OWNER, RENTER, LIVING_WITH_FAMILY
    declared_household_size     INT,
    declared_monthly_disposable DECIMAL(18,2),

    -- 第三方驗證
    third_party_verified    BOOLEAN DEFAULT FALSE,
    third_party_provider    VARCHAR(50),       -- OPEN_BANKING, EXPERIAN, EQUIFAX
    third_party_result      JSON,
    verified_at             DATETIME,

    -- 評估結果
    assessment_result       VARCHAR(20),       -- PASSED, FAILED, PENDING, EXPIRED
    recommended_limit       DECIMAL(18,2),
    applied_limit           DECIMAL(18,2),
    risk_tier               VARCHAR(20),       -- LOW, MEDIUM, HIGH, CRITICAL

    -- 有效期
    valid_from              DATETIME NOT NULL,
    valid_until             DATETIME NOT NULL,

    -- 審計
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_valid_until (valid_until),
    INDEX idx_assessment_result (assessment_result)
);

-- 財務脆弱性指標
CREATE TABLE t_financial_vulnerability_indicator (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    indicator_type      VARCHAR(50) NOT NULL,  -- CHASING_LOSSES, DEPOSIT_VELOCITY, UNUSUAL_PATTERN
    indicator_value     VARCHAR(255),
    severity            VARCHAR(20) NOT NULL,  -- LOW, MEDIUM, HIGH
    detected_at         DATETIME NOT NULL,
    resolved            BOOLEAN DEFAULT FALSE,
    resolved_at         DATETIME,
    resolution_action   VARCHAR(100),

    INDEX idx_player_id (player_id),
    INDEX idx_detected_at (detected_at)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AffordabilityAssessmentService {

    private final AffordabilityAssessmentDao assessmentDao;
    private final FinancialVulnerabilityDao vulnerabilityDao;
    private final DepositLimitService depositLimitService;
    private final OpenBankingClient openBankingClient;
    private final CreditReferenceClient creditClient;

    /**
     * 檢查是否需要可負擔性評估
     */
    public Option<AssessmentRequirement> checkAssessmentRequired(Long playerId) {
        // 獲取當前評估狀態
        Option<AffordabilityAssessment> currentAssessmentOpt =
            assessmentDao.findLatestValid(playerId);

        // 計算年度淨虧損
        BigDecimal annualNetLoss = calculateAnnualNetLoss(playerId);

        // 根據 UK 2025 規則判斷
        AssessmentType requiredType = determineAssessmentType(annualNetLoss);

        if (requiredType == null) {
            return Option.none(); // 不需要評估
        }

        // 檢查是否已有有效評估
        if (currentAssessmentOpt.isDefined()) {
            AffordabilityAssessment current = currentAssessmentOpt.get();
            if (current.getAssessmentType().ordinal() >= requiredType.ordinal() &&
                current.getValidUntil().isAfter(LocalDateTime.now())) {
                return Option.none(); // 已有足夠級別的有效評估
            }
        }

        return Option.some(AssessmentRequirement.builder()
            .type(requiredType)
            .reason(determineTriggerReason(annualNetLoss))
            .triggerValue(annualNetLoss)
            .build());
    }

    /**
     * 判斷所需評估類型
     */
    private AssessmentType determineAssessmentType(BigDecimal annualNetLoss) {
        if (annualNetLoss.compareTo(new BigDecimal("2000")) > 0) {
            return AssessmentType.FULL;
        } else if (annualNetLoss.compareTo(new BigDecimal("500")) > 0) {
            return AssessmentType.ENHANCED;
        } else if (annualNetLoss.compareTo(new BigDecimal("125")) > 0) {
            return AssessmentType.BASIC;
        }
        return null;
    }

    /**
     * 提交玩家自我聲明
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<AssessmentResultVO> submitSelfDeclaration(
            Long playerId,
            SelfDeclarationForm form) {

        // 驗證聲明數據合理性
        if (!validateDeclaration(form)) {
            return ResponseDTO.error(UserErrorCode.INVALID_DECLARATION);
        }

        // 計算建議限額
        BigDecimal recommendedLimit = calculateRecommendedLimit(form);

        // 創建評估記錄
        AffordabilityAssessment assessment = AffordabilityAssessment.builder()
            .playerId(playerId)
            .assessmentType(AssessmentType.ENHANCED)
            .triggerReason(TriggerReason.NET_LOSS)
            .declaredIncomeRange(form.getIncomeRange())
            .declaredHousingStatus(form.getHousingStatus())
            .declaredHouseholdSize(form.getHouseholdSize())
            .declaredMonthlyDisposable(form.getMonthlyDisposable())
            .assessmentResult(AssessmentResult.PASSED)
            .recommendedLimit(recommendedLimit)
            .validFrom(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusMonths(3)) // 3 個月有效
            .build();

        assessmentDao.insert(assessment);

        // 詢問是否應用建議限額
        return ResponseDTO.ok(AssessmentResultVO.builder()
            .assessmentId(assessment.getId())
            .result(AssessmentResult.PASSED)
            .recommendedMonthlyLimit(recommendedLimit)
            .message("評估完成，建議月存款限額為 " + recommendedLimit)
            .build());
    }

    /**
     * 執行完整評估（第三方驗證）
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<AssessmentResultVO> performFullAssessment(
            Long playerId,
            FullAssessmentForm form) {

        // 1. 調用 Open Banking API 獲取財務數據
        OpenBankingResult obResult = null;
        if (form.isOpenBankingConsent()) {
            obResult = openBankingClient.getFinancialData(
                form.getBankAccountId(),
                form.getConsentToken()
            );
        }

        // 2. 調用信用參考機構
        CreditReferenceResult crResult = creditClient.checkAffordability(
            form.getFirstName(),
            form.getLastName(),
            form.getDateOfBirth(),
            form.getPostcode()
        );

        // 3. 綜合評估
        FullAssessmentAnalysis analysis = analyzeFullAssessment(obResult, crResult);

        // 4. 確定風險等級和限額
        RiskTier riskTier = determineRiskTier(analysis);
        BigDecimal appliedLimit = calculateAppliedLimit(analysis, riskTier);

        // 5. 創建評估記錄
        AffordabilityAssessment assessment = AffordabilityAssessment.builder()
            .playerId(playerId)
            .assessmentType(AssessmentType.FULL)
            .triggerReason(TriggerReason.NET_LOSS)
            .thirdPartyVerified(true)
            .thirdPartyProvider("OPEN_BANKING,EXPERIAN")
            .thirdPartyResult(JsonUtil.toJson(analysis))
            .verifiedAt(LocalDateTime.now())
            .assessmentResult(analysis.isPassed() ? AssessmentResult.PASSED : AssessmentResult.FAILED)
            .recommendedLimit(appliedLimit)
            .appliedLimit(appliedLimit)
            .riskTier(riskTier)
            .validFrom(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusMonths(6)) // 6 個月有效
            .build();

        assessmentDao.insert(assessment);

        // 6. 如果評估失敗，強制應用限額
        if (!analysis.isPassed()) {
            depositLimitService.forceApplyLimit(playerId, appliedLimit, "AFFORDABILITY_ASSESSMENT");
        }

        log.info("Full affordability assessment completed: playerId={}, result={}, limit={}",
            playerId, assessment.getAssessmentResult(), appliedLimit);

        return ResponseDTO.ok(AssessmentResultVO.builder()
            .assessmentId(assessment.getId())
            .result(assessment.getAssessmentResult())
            .riskTier(riskTier)
            .appliedMonthlyLimit(appliedLimit)
            .message(buildResultMessage(assessment))
            .build());
    }

    /**
     * 計算建議限額（基於自我聲明）
     */
    private BigDecimal calculateRecommendedLimit(SelfDeclarationForm form) {
        BigDecimal monthlyDisposable = form.getMonthlyDisposable();

        // 建議博彩支出不超過可支配收入的 10%
        BigDecimal maxGambling = monthlyDisposable.multiply(new BigDecimal("0.10"));

        // 根據家庭規模調整
        if (form.getHouseholdSize() > 2) {
            maxGambling = maxGambling.multiply(new BigDecimal("0.8")); // 減少 20%
        }

        // 最低限額 £50，最高限額 £2,000
        return maxGambling.max(new BigDecimal("50")).min(new BigDecimal("2000"));
    }

    /**
     * 檢測財務脆弱性指標
     */
    @Scheduled(fixedRate = 300000) // 每 5 分鐘執行
    public void detectFinancialVulnerability() {
        List<Long> playerIds = getActivePlayerIds();

        for (Long playerId : playerIds) {
            List<VulnerabilityIndicator> indicators = new ArrayList<>();

            // 檢測追趕虧損行為
            if (detectChasingLosses(playerId)) {
                indicators.add(VulnerabilityIndicator.builder()
                    .type(IndicatorType.CHASING_LOSSES)
                    .severity(Severity.HIGH)
                    .build());
            }

            // 檢測異常存款速度
            if (detectDepositVelocity(playerId)) {
                indicators.add(VulnerabilityIndicator.builder()
                    .type(IndicatorType.DEPOSIT_VELOCITY)
                    .severity(Severity.MEDIUM)
                    .build());
            }

            // 檢測異常投注模式
            if (detectUnusualPattern(playerId)) {
                indicators.add(VulnerabilityIndicator.builder()
                    .type(IndicatorType.UNUSUAL_PATTERN)
                    .severity(Severity.MEDIUM)
                    .build());
            }

            // 記錄並處理指標
            for (VulnerabilityIndicator indicator : indicators) {
                recordAndHandleIndicator(playerId, indicator);
            }
        }
    }

    /**
     * 檢測追趕虧損行為
     *
     * 模式：虧損後立即增加投注金額
     */
    private boolean detectChasingLosses(Long playerId) {
        List<BetRecord> recentBets = betHistoryService.getRecentBets(playerId, 20);

        int chasingCount = 0;
        BigDecimal previousStake = null;
        boolean previousWon = true;

        for (BetRecord bet : recentBets) {
            if (!previousWon && previousStake != null) {
                // 上一注輸了，且本注金額增加 > 50%
                if (bet.getStake().compareTo(previousStake.multiply(new BigDecimal("1.5"))) > 0) {
                    chasingCount++;
                }
            }
            previousStake = bet.getStake();
            previousWon = bet.getWinAmount().compareTo(bet.getStake()) > 0;
        }

        return chasingCount >= 3; // 20 注內有 3 次追趕行為
    }

    /**
     * 檢測異常存款速度
     *
     * 模式：24 小時內多次小額存款
     */
    private boolean detectDepositVelocity(Long playerId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<DepositRecord> deposits = depositService.getDepositsSince(playerId, since);

        if (deposits.size() >= 5) {
            BigDecimal totalAmount = deposits.stream()
                .map(DepositRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 5 次以上存款且總額 > £500
            return totalAmount.compareTo(new BigDecimal("500")) > 0;
        }

        return false;
    }

    /**
     * 記錄並處理脆弱性指標
     */
    private void recordAndHandleIndicator(Long playerId, VulnerabilityIndicator indicator) {
        // 記錄指標
        FinancialVulnerabilityIndicator record = FinancialVulnerabilityIndicator.builder()
            .playerId(playerId)
            .indicatorType(indicator.getType().name())
            .severity(indicator.getSeverity().name())
            .detectedAt(LocalDateTime.now())
            .build();

        vulnerabilityDao.insert(record);

        // 根據嚴重程度採取行動
        switch (indicator.getSeverity()) {
            case HIGH -> {
                // 觸發完整評估
                triggerFullAssessment(playerId, TriggerReason.VULNERABILITY_DETECTED);
                // 發送關懷訊息
                notificationService.sendConcernMessage(playerId);
            }
            case MEDIUM -> {
                // 顯示警告
                notificationService.sendWarningMessage(playerId,
                    "我們注意到您最近的博彩活動有所增加。請確保您的博彩活動在可負擔範圍內。");
            }
            case LOW -> {
                // 僅記錄，不採取行動
                log.info("Low severity indicator detected: playerId={}, type={}",
                    playerId, indicator.getType());
            }
        }
    }
}
```

### Open Banking 整合

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenBankingClient {

    private final RestTemplate restTemplate;

    @Value("${openbanking.api.url}")
    private String apiUrl;

    /**
     * 獲取財務數據（需要玩家授權）
     */
    public OpenBankingResult getFinancialData(String accountId, String consentToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + consentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // 獲取帳戶餘額
            ResponseEntity<AccountBalanceResponse> balanceResponse = restTemplate.exchange(
                apiUrl + "/accounts/" + accountId + "/balances",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                AccountBalanceResponse.class
            );

            // 獲取交易記錄（最近 90 天）
            ResponseEntity<TransactionListResponse> transactionResponse = restTemplate.exchange(
                apiUrl + "/accounts/" + accountId + "/transactions?fromDate=" +
                    LocalDate.now().minusDays(90),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                TransactionListResponse.class
            );

            // 分析財務狀況
            return analyzeFinancialData(
                balanceResponse.getBody(),
                transactionResponse.getBody()
            );
        } catch (Exception e) {
            log.error("Open Banking API call failed", e);
            throw new OpenBankingException("無法獲取財務數據", e);
        }
    }

    private OpenBankingResult analyzeFinancialData(
            AccountBalanceResponse balance,
            TransactionListResponse transactions) {

        // 計算月平均收入
        BigDecimal monthlyIncome = calculateMonthlyIncome(transactions);

        // 計算月平均支出
        BigDecimal monthlyExpenses = calculateMonthlyExpenses(transactions);

        // 計算可支配收入
        BigDecimal disposableIncome = monthlyIncome.subtract(monthlyExpenses);

        // 檢測博彩相關支出
        BigDecimal gamblingSpend = detectGamblingTransactions(transactions);

        return OpenBankingResult.builder()
            .averageBalance(balance.getAvailableBalance())
            .monthlyIncome(monthlyIncome)
            .monthlyExpenses(monthlyExpenses)
            .disposableIncome(disposableIncome)
            .gamblingSpend(gamblingSpend)
            .gamblingSpendRatio(gamblingSpend.divide(monthlyIncome, 4, RoundingMode.HALF_UP))
            .build();
    }
}
```

---

## 前端整合

### 可負擔性評估流程

```vue
<template>
  <a-modal
    v-model:visible="visible"
    title="財務評估"
    :closable="false"
    :maskClosable="false"
    width="600px"
  >
    <!-- 步驟指示器 -->
    <a-steps :current="currentStep" class="mb-6">
      <a-step title="說明" />
      <a-step title="財務聲明" />
      <a-step title="完成" />
    </a-steps>

    <!-- 步驟 1: 說明 -->
    <div v-if="currentStep === 0">
      <a-alert
        type="info"
        show-icon
        class="mb-4"
      >
        <template #message>為什麼需要財務評估？</template>
        <template #description>
          根據 UK Gambling Commission 規定，當您的博彩活動達到特定閾值時，
          我們需要確認您的博彩活動在可負擔範圍內。這是為了保護您的利益。
        </template>
      </a-alert>

      <p>我們將詢問一些基本的財務問題，所有資料將嚴格保密。</p>

      <a-button type="primary" @click="currentStep = 1">
        開始評估
      </a-button>
    </div>

    <!-- 步驟 2: 財務聲明 -->
    <div v-if="currentStep === 1">
      <a-form :model="form" :rules="rules" @finish="handleSubmit">
        <a-form-item label="年收入範圍" name="incomeRange">
          <a-select v-model:value="form.incomeRange" placeholder="請選擇">
            <a-select-option value="0-15000">£0 - £15,000</a-select-option>
            <a-select-option value="15000-25000">£15,000 - £25,000</a-select-option>
            <a-select-option value="25000-40000">£25,000 - £40,000</a-select-option>
            <a-select-option value="40000-60000">£40,000 - £60,000</a-select-option>
            <a-select-option value="60000-100000">£60,000 - £100,000</a-select-option>
            <a-select-option value="100000+">£100,000+</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="住房狀態" name="housingStatus">
          <a-radio-group v-model:value="form.housingStatus">
            <a-radio value="OWNER">自有房屋</a-radio>
            <a-radio value="RENTER">租房</a-radio>
            <a-radio value="FAMILY">與家人同住</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="家庭人口數" name="householdSize">
          <a-input-number
            v-model:value="form.householdSize"
            :min="1"
            :max="10"
          />
        </a-form-item>

        <a-form-item label="每月可支配收入（扣除必要支出後）" name="monthlyDisposable">
          <a-input-number
            v-model:value="form.monthlyDisposable"
            :min="0"
            :step="100"
            style="width: 200px"
          >
            <template #addonBefore>£</template>
          </a-input-number>
        </a-form-item>

        <a-divider />

        <a-checkbox v-model:checked="form.confirmAccuracy" class="mb-4">
          我確認以上資訊真實準確
        </a-checkbox>

        <a-form-item>
          <a-button @click="currentStep = 0">上一步</a-button>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            :disabled="!form.confirmAccuracy"
            class="ml-2"
          >
            提交評估
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 步驟 3: 完成 -->
    <div v-if="currentStep === 2">
      <a-result
        :status="result.passed ? 'success' : 'warning'"
        :title="result.passed ? '評估完成' : '需要調整'"
      >
        <template #subTitle>
          <p v-if="result.recommendedLimit">
            根據您的財務狀況，我們建議您的月存款限額為
            <strong>£{{ result.recommendedLimit }}</strong>
          </p>
        </template>

        <template #extra>
          <a-button
            v-if="result.recommendedLimit"
            type="primary"
            @click="applyRecommendedLimit"
          >
            應用建議限額
          </a-button>
          <a-button @click="close">關閉</a-button>
        </template>
      </a-result>
    </div>
  </a-modal>
</template>
```

---

## 監控與報告

### 關鍵指標

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 評估觸發數 | `affordability_assessment_triggered_total` | 按類型分類 |
| 評估通過率 | `affordability_assessment_pass_rate` | PASSED / TOTAL |
| 平均建議限額 | `affordability_recommended_limit_avg` | 建議限額平均值 |
| 脆弱性指標檢測 | `vulnerability_indicator_detected_total` | 按類型分類 |

### 合規報告

| 報告 | 頻率 | 內容 |
|------|------|------|
| 評估統計 | 月度 | 觸發數、通過率、限額分布 |
| 脆弱性檢測 | 月度 | 檢測數量、處理結果 |
| 限額應用 | 月度 | 強制限額數、玩家反應 |

---

## 相關文檔

- [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) - 存款限額
- [15-06_Loss_Limits.md](15-06_Loss_Limits.md) - 虧損限額
- [05-03_KYC_AML.md](../05_Risk_Control/05-03_KYC_AML.md) - KYC/AML
- [06-08_UKGC_Compliance.md](../06_Platform_Governance/06-08_UKGC_Compliance.md) - UK 合規

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
