# P1-07: 取款風控關聯系統

**優先級**: P1 重要
**預估工作量**: 3 人天
**風險等級**: 🟡 中
**相關 ADR**: [ADR 012: 異步風控提案系統](../../architecture-decisions/012-async-risk-proposal-system.md)

---

## 功能概述

在玩家發起取款請求時，系統自動抓取「自上次取款以來」投注時段內的所有風控提案，並根據提案風險評分決定是否批准取款。

### 核心目標

1. ✅ **防止資金流出**：高風險提案未審核前，阻斷取款
2. ✅ **完整覆蓋**：關聯投注時段的所有風控單（不遺漏）
3. ✅ **性能優化**：查詢延遲 < 200ms（P95）
4. ✅ **審計追溯**：記錄所有取款風控關聯決策

### 業務流程

```
玩家發起取款
    ↓
查詢上次取款時間
    ↓
計算時間範圍（since → now）
    ↓
查詢該時段所有風控提案
    ↓
┌──────────────────┐
│ 存在高風險提案？   │ (score ≥ 80)
└──────────────────┘
    YES │      │ NO
        ↓      ↓
    阻斷取款  批准取款
        ↓
    人工審核
        ↓
    決策執行
```

---

## 時間範圍邏輯

### 規則定義

#### 規則 1: 正常用戶（非首次取款）

**條件**：`lastWithdrawalTime != null`

**時間範圍**：`lastWithdrawalTime → currentTime`

**示例**：
```
上次取款時間：2026-01-15 10:00
當前取款時間：2026-02-03 15:00
時間範圍：2026-01-15 10:00 → 2026-02-03 15:00（19 天）
```

---

#### 規則 2: 首次取款用戶（新註冊）

**條件**：`lastWithdrawalTime == null && 註冊時間 < 30 天`

**時間範圍**：`registrationTime → currentTime`

**示例**：
```
註冊時間：2026-01-20 08:00
當前取款時間：2026-02-03 15:00
時間範圍：2026-01-20 08:00 → 2026-02-03 15:00（14 天）
```

---

#### 規則 3: 首次取款用戶（老用戶）

**條件**：`lastWithdrawalTime == null && 註冊時間 ≥ 30 天`

**時間範圍**：`currentTime.minusDays(30) → currentTime`

**理由**：避免查詢過長時間範圍，影響性能

**示例**：
```
註冊時間：2025-10-01 08:00（4 個月前）
當前取款時間：2026-02-03 15:00
時間範圍：2026-01-04 15:00 → 2026-02-03 15:00（30 天）
```

---

### 實現代碼

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalRiskCorrelationService {

    private final RiskProposalDao riskProposalDao;
    private final WithdrawalDao withdrawalDao;
    private final PlayerDao playerDao;

    /**
     * 計算取款風控時間範圍
     */
    public LocalDateTime calculateCorrelationStartTime(Long playerId) {
        // 查詢上次取款時間
        LocalDateTime lastWithdrawalTime = withdrawalDao.getLastWithdrawalTime(playerId);

        // 規則 1: 正常用戶（非首次取款）
        if (lastWithdrawalTime != null) {
            log.info("[Withdrawal] Regular user: playerId={}, lastWithdrawal={}",
                    playerId, lastWithdrawalTime);
            return lastWithdrawalTime;
        }

        // 首次取款用戶：查詢註冊時間
        LocalDateTime registrationTime = playerDao.getRegistrationTime(playerId);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 規則 2: 首次取款用戶（新註冊 < 30 天）
        if (registrationTime.isAfter(thirtyDaysAgo)) {
            log.info("[Withdrawal] First-time user (new): playerId={}, registration={}",
                    playerId, registrationTime);
            return registrationTime;
        }

        // 規則 3: 首次取款用戶（老用戶 ≥ 30 天）
        log.info("[Withdrawal] First-time user (old): playerId={}, fallback=30days",
                playerId);
        return thirtyDaysAgo;
    }

    /**
     * 取款時關聯風控提案
     */
    public List<RiskProposal> getRelatedProposals(Long playerId, WithdrawalRequest request) {
        LocalDateTime startTime = calculateCorrelationStartTime(playerId);
        LocalDateTime endTime = LocalDateTime.now();

        List<RiskProposal> proposals = riskProposalDao.findPendingProposals(
                playerId,
                startTime,
                endTime
        );

        log.info("[Withdrawal] Correlated proposals: playerId={}, count={}, period={} to {}",
                playerId, proposals.size(), startTime, endTime);

        return proposals;
    }
}
```

---

## 數據庫設計

### 表結構設計

#### 風控提案表（risk_proposals）

已在 [ADR 012](../../architecture-decisions/012-async-risk-proposal-system.md) 定義。

---

#### 取款風控關聯表（withdrawal_risk_correlations）

```sql
CREATE TABLE withdrawal_risk_correlations (
    id BIGSERIAL PRIMARY KEY,
    withdrawal_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    correlated_proposals JSONB NOT NULL,  -- Array of proposal IDs
    high_risk_count INTEGER NOT NULL,     -- 高風險提案數量
    medium_risk_count INTEGER NOT NULL,   -- 中風險提案數量
    total_risk_score INTEGER NOT NULL,    -- 總風險評分
    decision VARCHAR(20) NOT NULL,        -- APPROVED, BLOCKED, MANUAL_REVIEW
    correlation_start_time TIMESTAMP NOT NULL,  -- 關聯時間範圍起點
    correlation_end_time TIMESTAMP NOT NULL,    -- 關聯時間範圍終點
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,                -- 人工審核時間（如果需要）
    reviewed_by BIGINT,                   -- 審核人（如果需要）
    decision_notes TEXT,                  -- 決策備註

    CONSTRAINT fk_withdrawal
        FOREIGN KEY (withdrawal_id) REFERENCES withdrawals(id),
    CONSTRAINT fk_player
        FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT chk_decision CHECK (
        decision IN ('APPROVED', 'BLOCKED', 'MANUAL_REVIEW')
    ),
    CONSTRAINT chk_correlation_time CHECK (
        correlation_end_time > correlation_start_time
    )
);

-- 索引設計
CREATE INDEX idx_withdrawal_correlations_player
    ON withdrawal_risk_correlations(player_id, evaluated_at DESC);

CREATE INDEX idx_withdrawal_correlations_withdrawal
    ON withdrawal_risk_correlations(withdrawal_id);

CREATE INDEX idx_withdrawal_correlations_decision
    ON withdrawal_risk_correlations(decision, evaluated_at DESC);

CREATE INDEX idx_withdrawal_correlations_review_pending
    ON withdrawal_risk_correlations(decision, reviewed_at)
    WHERE decision = 'MANUAL_REVIEW' AND reviewed_at IS NULL;

-- 分區表（可選，提升查詢性能）
-- 按月分區
CREATE TABLE withdrawal_risk_correlations_y2026m02
PARTITION OF withdrawal_risk_correlations
FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

---

### 查詢優化

#### 關鍵查詢 SQL

```sql
-- 查詢待審核風控提案
SELECT *
FROM risk_proposals
WHERE player_id = :playerId
  AND status = 'PENDING_REVIEW'
  AND created_at >= :startTime
  AND created_at <= :endTime
ORDER BY risk_score DESC, created_at DESC;

-- 執行計劃分析
EXPLAIN ANALYZE
SELECT *
FROM risk_proposals
WHERE player_id = 123456
  AND status = 'PENDING_REVIEW'
  AND created_at >= '2026-01-15 10:00'
  AND created_at <= '2026-02-03 15:00'
ORDER BY risk_score DESC, created_at DESC;
```

#### 索引優化建議

```sql
-- 複合索引（覆蓋查詢）
CREATE INDEX idx_risk_proposals_correlation_query
    ON risk_proposals(player_id, status, created_at DESC, risk_score DESC)
    INCLUDE (proposal_type, game_type, details);

-- 部分索引（僅索引 PENDING_REVIEW）
CREATE INDEX idx_risk_proposals_pending_review
    ON risk_proposals(player_id, created_at DESC)
    WHERE status = 'PENDING_REVIEW';
```

---

## API 設計

### RESTful API 接口

#### 1. 取款風控評估（內部 API）

```java
/**
 * POST /api/withdrawal/risk-evaluation
 * 評估取款請求的風控狀態
 */
@PostMapping("/risk-evaluation")
public ResponseDTO<WithdrawalRiskEvaluationVO> evaluateWithdrawalRisk(
        @RequestBody @Valid WithdrawalRiskEvaluationForm form) {

    WithdrawalRiskEvaluationVO result = withdrawalRiskService.evaluate(form);
    return ResponseDTO.ok(result);
}
```

**請求體**（WithdrawalRiskEvaluationForm）：
```json
{
  "playerId": 123456,
  "withdrawalId": 789012,
  "amount": 5000.00,
  "currency": "USD"
}
```

**響應體**（WithdrawalRiskEvaluationVO）：
```json
{
  "decision": "BLOCKED",
  "correlatedProposals": [
    {
      "proposalId": 12345,
      "riskScore": 85,
      "proposalType": "LATE_BETTING",
      "gameType": "SPORTS_BETTING",
      "createdAt": "2026-01-20T14:30:00"
    },
    {
      "proposalId": 12346,
      "riskScore": 75,
      "proposalType": "MULTI_ACCOUNT",
      "gameType": "SLOTS",
      "createdAt": "2026-01-28T09:15:00"
    }
  ],
  "highRiskCount": 1,
  "mediumRiskCount": 1,
  "totalRiskScore": 160,
  "correlationStartTime": "2026-01-15T10:00:00",
  "correlationEndTime": "2026-02-03T15:00:00",
  "requiresManualReview": true,
  "reason": "HIGH_RISK_PROPOSALS_PENDING"
}
```

---

#### 2. 查詢取款風控關聯記錄

```java
/**
 * GET /api/withdrawal/{withdrawalId}/risk-correlation
 * 查詢取款的風控關聯記錄
 */
@GetMapping("/{withdrawalId}/risk-correlation")
public ResponseDTO<WithdrawalRiskCorrelationVO> getWithdrawalRiskCorrelation(
        @PathVariable Long withdrawalId) {

    WithdrawalRiskCorrelationVO result = withdrawalRiskService.getCorrelation(withdrawalId);
    return ResponseDTO.ok(result);
}
```

**響應體**（WithdrawalRiskCorrelationVO）：
```json
{
  "correlationId": 567890,
  "withdrawalId": 789012,
  "playerId": 123456,
  "decision": "BLOCKED",
  "correlatedProposals": [...],
  "highRiskCount": 1,
  "mediumRiskCount": 1,
  "totalRiskScore": 160,
  "evaluatedAt": "2026-02-03T15:00:00",
  "reviewedAt": null,
  "reviewedBy": null,
  "decisionNotes": null
}
```

---

#### 3. 人工審核決策（內部 API）

```java
/**
 * POST /api/withdrawal/risk-correlation/{correlationId}/review
 * 人工審核取款風控關聯
 */
@PostMapping("/risk-correlation/{correlationId}/review")
public ResponseDTO<Void> reviewWithdrawalRisk(
        @PathVariable Long correlationId,
        @RequestBody @Valid WithdrawalRiskReviewForm form) {

    withdrawalRiskService.review(correlationId, form);
    return ResponseDTO.ok();
}
```

**請求體**（WithdrawalRiskReviewForm）：
```json
{
  "decision": "APPROVED",  // APPROVED, REJECTED
  "decisionNotes": "經審核，風險可控，批准取款。玩家投注行為正常，套利評分為誤判。",
  "reviewedBy": 999
}
```

---

### Service 層實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalRiskService {

    private final WithdrawalRiskCorrelationService correlationService;
    private final RiskProposalDao riskProposalDao;
    private final WithdrawalRiskCorrelationManager manager;

    /**
     * 評估取款風控狀態
     */
    public WithdrawalRiskEvaluationVO evaluate(WithdrawalRiskEvaluationForm form) {
        // 1. 取得關聯風控提案
        List<RiskProposal> proposals = correlationService.getRelatedProposals(
                form.getPlayerId(),
                new WithdrawalRequest(form.getWithdrawalId(), form.getAmount())
        );

        // 2. 統計風險提案
        long highRiskCount = proposals.stream()
                .filter(p -> p.getRiskScore() >= 80)
                .count();

        long mediumRiskCount = proposals.stream()
                .filter(p -> p.getRiskScore() >= 50 && p.getRiskScore() < 80)
                .count();

        int totalRiskScore = proposals.stream()
                .mapToInt(RiskProposal::getRiskScore)
                .sum();

        // 3. 決策邏輯
        String decision;
        boolean requiresManualReview = false;
        String reason = null;

        if (highRiskCount > 0) {
            decision = "BLOCKED";
            requiresManualReview = true;
            reason = "HIGH_RISK_PROPOSALS_PENDING";
        } else if (mediumRiskCount > 0) {
            decision = "MANUAL_REVIEW";
            requiresManualReview = true;
            reason = "MEDIUM_RISK_PROPOSALS_PENDING";
        } else {
            decision = "APPROVED";
        }

        // 4. 持久化關聯記錄
        WithdrawalRiskCorrelation correlation = manager.saveCorrelation(
                form, proposals, decision, highRiskCount, mediumRiskCount, totalRiskScore
        );

        // 5. 構建響應
        return buildEvaluationVO(correlation, proposals, requiresManualReview, reason);
    }
}
```

---

## LiteFlow 規則配置

### 取款風控關聯規則（EL 表達式）

```java
// config/liteflow/withdrawal-risk-correlation.el
THEN(
    // 1. 計算時間範圍
    calculateCorrelationTimeRange,

    // 2. 查詢風控提案
    queryPendingProposals,

    // 3. 統計風險級別
    classifyProposalsByRiskLevel,

    // 4. 決策判定
    SWITCH(decisionRule).to(
        IF(highRiskCount > 0, THEN(blockWithdrawal)),
        IF(mediumRiskCount > 0, THEN(requireManualReview)),
        DEFAULT(approveWithdrawal)
    ),

    // 5. 持久化關聯記錄
    saveCorrelationRecord,

    // 6. 發送通知
    notifyRiskTeam
);
```

### 規則組件實現

```java
@Component("calculateCorrelationTimeRange")
@RequiredArgsConstructor
@Slf4j
public class CalculateCorrelationTimeRangeComponent extends NodeComponent {

    private final WithdrawalRiskCorrelationService correlationService;

    @Override
    public void process() {
        Long playerId = this.getRequestData();
        LocalDateTime startTime = correlationService.calculateCorrelationStartTime(playerId);

        this.setContextBean("correlationStartTime", startTime);
        this.setContextBean("correlationEndTime", LocalDateTime.now());

        log.info("[LiteFlow] Calculated correlation time range: player={}, start={}, end={}",
                playerId, startTime, LocalDateTime.now());
    }
}
```

---

## 性能要求

### 目標指標

| 指標 | 目標值 | 測量方法 |
|------|--------|---------|
| 取款風控查詢延遲 | < 200ms (P95) | Prometheus + Grafana |
| 關聯提案數量 | ≤ 100 個 | 資料庫查詢統計 |
| 併發支持 | 1,000 TPS | JMeter 壓測 |
| 資料庫查詢時間 | < 100ms (P95) | PostgreSQL EXPLAIN ANALYZE |

---

### 性能優化策略

#### 1. 資料庫索引優化

```sql
-- 複合索引（覆蓋查詢）
CREATE INDEX idx_risk_proposals_correlation_query
    ON risk_proposals(player_id, status, created_at DESC, risk_score DESC)
    INCLUDE (proposal_type, game_type, details);
```

#### 2. Redis 緩存

```java
@Cacheable(value = "withdrawal:risk:proposals", key = "#playerId + ':' + #startTime + ':' + #endTime")
public List<RiskProposal> getRelatedProposals(Long playerId, LocalDateTime startTime, LocalDateTime endTime) {
    return riskProposalDao.findPendingProposals(playerId, startTime, endTime);
}
```

#### 3. 異步處理（可選）

```java
@Async("withdrawalExecutor")
public CompletableFuture<WithdrawalRiskEvaluationVO> evaluateAsync(WithdrawalRiskEvaluationForm form) {
    WithdrawalRiskEvaluationVO result = evaluate(form);
    return CompletableFuture.completedFuture(result);
}
```

---

## 測試用例

### 單元測試

#### 測試用例 1: 正常用戶時間範圍計算

```java
@Test
@DisplayName("正常用戶（非首次取款）應返回上次取款時間")
void testCalculateTimeRange_RegularUser() {
    // Given
    Long playerId = 123456L;
    LocalDateTime lastWithdrawal = LocalDateTime.of(2026, 1, 15, 10, 0);
    when(withdrawalDao.getLastWithdrawalTime(playerId)).thenReturn(lastWithdrawal);

    // When
    LocalDateTime result = correlationService.calculateCorrelationStartTime(playerId);

    // Then
    assertThat(result).isEqualTo(lastWithdrawal);
    verify(playerDao, never()).getRegistrationTime(any());  // 不應查詢註冊時間
}
```

#### 測試用例 2: 首次取款用戶（新註冊）

```java
@Test
@DisplayName("首次取款用戶（註冊 < 30 天）應返回註冊時間")
void testCalculateTimeRange_FirstTimeNewUser() {
    // Given
    Long playerId = 123456L;
    LocalDateTime registrationTime = LocalDateTime.now().minusDays(14);
    when(withdrawalDao.getLastWithdrawalTime(playerId)).thenReturn(null);
    when(playerDao.getRegistrationTime(playerId)).thenReturn(registrationTime);

    // When
    LocalDateTime result = correlationService.calculateCorrelationStartTime(playerId);

    // Then
    assertThat(result).isEqualTo(registrationTime);
}
```

#### 測試用例 3: 首次取款用戶（老用戶）

```java
@Test
@DisplayName("首次取款用戶（註冊 ≥ 30 天）應返回 30 天前")
void testCalculateTimeRange_FirstTimeOldUser() {
    // Given
    Long playerId = 123456L;
    LocalDateTime registrationTime = LocalDateTime.now().minusDays(120);  // 4 個月前
    when(withdrawalDao.getLastWithdrawalTime(playerId)).thenReturn(null);
    when(playerDao.getRegistrationTime(playerId)).thenReturn(registrationTime);

    // When
    LocalDateTime result = correlationService.calculateCorrelationStartTime(playerId);

    // Then
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    assertThat(result).isAfterOrEqualTo(thirtyDaysAgo.minusSeconds(5));  // 容忍誤差
    assertThat(result).isBeforeOrEqualTo(thirtyDaysAgo.plusSeconds(5));
}
```

#### 測試用例 4: 高風險提案阻斷取款

```java
@Test
@DisplayName("存在高風險提案（score ≥ 80）應阻斷取款")
void testEvaluate_HighRiskProposals_ShouldBlock() {
    // Given
    WithdrawalRiskEvaluationForm form = createForm();
    List<RiskProposal> proposals = List.of(
            createProposal(85, ProposalType.LATE_BETTING),
            createProposal(75, ProposalType.MULTI_ACCOUNT)
    );
    when(correlationService.getRelatedProposals(any(), any())).thenReturn(proposals);

    // When
    WithdrawalRiskEvaluationVO result = withdrawalRiskService.evaluate(form);

    // Then
    assertThat(result.getDecision()).isEqualTo("BLOCKED");
    assertThat(result.getHighRiskCount()).isEqualTo(1);
    assertThat(result.getMediumRiskCount()).isEqualTo(1);
    assertThat(result.isRequiresManualReview()).isTrue();
}
```

---

### 集成測試

#### 測試用例 5: 端到端取款風控流程

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class WithdrawalRiskCorrelationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WithdrawalRiskService withdrawalRiskService;

    @Autowired
    private RiskProposalDao riskProposalDao;

    @Test
    @DisplayName("端到端：取款時應成功關聯風控提案並阻斷取款")
    void testEndToEnd_WithdrawalRiskCorrelation() {
        // 1. 準備測試數據
        Long playerId = 123456L;
        createPlayer(playerId, LocalDateTime.now().minusDays(14));  // 註冊 14 天前

        // 創建風控提案
        createRiskProposal(playerId, 85, LocalDateTime.now().minusDays(5));  // 高風險
        createRiskProposal(playerId, 70, LocalDateTime.now().minusDays(2));  // 中風險

        // 2. 執行取款風控評估
        WithdrawalRiskEvaluationForm form = WithdrawalRiskEvaluationForm.builder()
                .playerId(playerId)
                .withdrawalId(789012L)
                .amount(BigDecimal.valueOf(5000))
                .currency("USD")
                .build();

        WithdrawalRiskEvaluationVO result = withdrawalRiskService.evaluate(form);

        // 3. 驗證結果
        assertThat(result.getDecision()).isEqualTo("BLOCKED");
        assertThat(result.getCorrelatedProposals()).hasSize(2);
        assertThat(result.getHighRiskCount()).isEqualTo(1);
        assertThat(result.getMediumRiskCount()).isEqualTo(1);
        assertThat(result.getTotalRiskScore()).isEqualTo(155);

        // 4. 驗證資料庫記錄
        WithdrawalRiskCorrelation correlation = withdrawalRiskCorrelationDao
                .findByWithdrawalId(789012L);

        assertThat(correlation).isNotNull();
        assertThat(correlation.getDecision()).isEqualTo("BLOCKED");
    }
}
```

---

### 性能測試

#### JMeter 壓測配置

```xml
<!-- jmeter/withdrawal-risk-correlation.jmx -->
<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="取款風控併發測試">
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>
  <longProp name="ThreadGroup.duration">60</longProp>
</ThreadGroup>

<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">1024</stringProp>
  <stringProp name="HTTPSampler.path">/api/withdrawal/risk-evaluation</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
  <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
</HTTPSamplerProxy>
```

**目標**：
- 併發用戶：100
- 測試時長：60 秒
- 預期 TPS：1,000+
- P95 延遲：< 200ms

---

## 驗收標準

### 功能驗收

- [ ] 正常用戶時間範圍計算正確（自上次取款以來）
- [ ] 首次取款用戶時間範圍計算正確（註冊 < 30 天）
- [ ] 首次取款用戶時間範圍計算正確（註冊 ≥ 30 天，回溯 30 天）
- [ ] 高風險提案（score ≥ 80）成功阻斷取款
- [ ] 中風險提案（50 ≤ score < 80）標記為人工審核
- [ ] 無風險提案成功批准取款
- [ ] 關聯記錄成功持久化到 `withdrawal_risk_correlations`

### 性能驗收

- [ ] 取款風控查詢延遲 < 200ms（P95）
- [ ] 資料庫查詢時間 < 100ms（P95）
- [ ] 併發支持 1,000 TPS
- [ ] Redis 緩存命中率 > 70%

### 質量驗收

- [ ] 單元測試覆蓋率 > 80%
- [ ] 集成測試覆蓋核心流程
- [ ] ArchitectureTest 通過
- [ ] SonarQube 無 critical 問題

---

## 部署清單

### 資料庫遷移

```sql
-- migration/V1.7.0__withdrawal_risk_correlation.sql
CREATE TABLE withdrawal_risk_correlations (...);
CREATE INDEX idx_withdrawal_correlations_player (...);
...
```

### 配置項

```yaml
# application-prod.yml
withdrawal:
  risk:
    correlation:
      max-time-range-days: 30  # 最大時間範圍（天）
      cache-ttl-minutes: 5     # Redis 緩存 TTL
      async-enabled: false     # 是否異步處理
```

### 監控告警

```yaml
# prometheus/alerts.yml
groups:
  - name: withdrawal-risk-correlation
    rules:
      - alert: WithdrawalRiskQuerySlow
        expr: histogram_quantile(0.95, rate(withdrawal_risk_query_duration_seconds_bucket[5m])) > 0.2
        for: 5m
        annotations:
          summary: "取款風控查詢延遲過高（P95 > 200ms）"
```

---

## 參考資料

### 相關文檔
- [ADR 012: 異步風控提案系統](../../architecture-decisions/012-async-risk-proposal-system.md)
- [SmartAdmin 分層架構](./../../../.agent/rules/foundation/10-architecture-rules.md)

### 業界標準
- [Analysis of Casino Online Gambling Data](https://www.researchgate.net/publication/228225597)
- [Using AI to predict problem gambling](https://pmc.ncbi.nlm.nih.gov/articles/PMC10397135/)

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-03
**下一步行動**: 開始實施取款風控關聯系統（預估 3 人天）
