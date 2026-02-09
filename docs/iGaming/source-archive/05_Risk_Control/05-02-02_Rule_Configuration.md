# 05-02-02 規則配置 (Rule Configuration)

## 文檔信息

| 屬性 | 值 |
|------|-----|
| **文檔版本** | 4.0.0 |
| **最後更新** | 2026-02-07 |
| **父文檔** | [05-02 欺詐檢測](./05-02_Fraud_Detection.md) |
| **維護團隊** | Risk Team & Backend Team |

---

## 1. 風控提案服務 (Risk Proposal Service)

### 1.1 提案數據模型

**t_risk_proposal 表結構**:

```sql
CREATE TABLE t_risk_proposal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提案 ID',
    proposal_no VARCHAR(50) NOT NULL UNIQUE COMMENT '提案編號 (如 RP-20260202-123456)',

    -- 關聯資訊
    bet_id VARCHAR(50) NOT NULL COMMENT '注單 ID',
    player_id BIGINT NOT NULL COMMENT '玩家 ID',
    withdrawal_request_id BIGINT COMMENT '取款請求 ID (若關聯取款)',

    -- 風控資訊
    matched_rules JSON NOT NULL COMMENT '匹配的風控規則列表 (如 ["LOW_ODDS_WAGERING", "CROSS_MATCH_HEDGE"])',
    flagged_reasons JSON NOT NULL COMMENT '標記原因列表',
    suspicious_amount DECIMAL(15, 2) NOT NULL COMMENT '可疑金額',

    -- 狀態管理
    status VARCHAR(20) NOT NULL COMMENT '提案狀態 (PENDING_REVIEW/APPROVED/REJECTED/PARTIAL_APPROVED)',
    priority VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '優先級 (LOW/MEDIUM/HIGH/URGENT)',

    -- 審核資訊
    reviewer_id BIGINT COMMENT '審核員 ID',
    reviewed_at DATETIME COMMENT '審核時間',
    review_notes TEXT COMMENT '審核備註',
    approved_amount DECIMAL(15, 2) COMMENT '批准金額（部分批准場景）',

    -- 補償資訊
    compensation_type VARCHAR(20) COMMENT '補償類型 (REFUND/ADJUSTMENT/DEDUCTION)',
    compensation_amount DECIMAL(15, 2) COMMENT '補償金額',
    compensation_executed_at DATETIME COMMENT '補償執行時間',

    -- 審計欄位
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE COMMENT '軟刪除標記',

    -- 索引優化
    INDEX idx_bet_id (bet_id),
    INDEX idx_player_id_status (player_id, status, deleted),
    INDEX idx_created_at (created_at),
    INDEX idx_status_priority (status, priority, deleted),
    INDEX idx_withdrawal_request (withdrawal_request_id)
) COMMENT='風控提案表';
```

**提案狀態流轉**:

```
PENDING_REVIEW (待審核)
    ↓
    ├─→ APPROVED (批准) → 執行補償 (若需要)
    ├─→ REJECTED (拒絕) → 凍結金額保持
    └─→ PARTIAL_APPROVED (部分批准) → 部分補償
```

### 1.2 提案創建流程

**投注成功後異步創建提案 (Kafka Consumer 調用)**:

> **重要說明**：此方法由 Kafka Consumer（風控引擎）在投注成功後異步調用。
> - **觸發時機**：投注 TCC Confirm 完成後，WALLET_DEBITED 事件發布至 Kafka
> - **觸發條件**：風控規則（BLOCK/FLAG）匹配時
> - **非同步調用**：不影響投注流程，投注已經成功

```java
/**
 * Service 層 - 創建風控提案
 *
 * 此方法由 Kafka Consumer（風控引擎）異步調用
 * 觸發時機：投注成功後，WALLET_DEBITED 事件發布至 Kafka Topic: wallet.debited
 * 調用者：RiskAnalysisConsumer (Kafka Consumer)
 */
public class RiskProposalService {

    private final RiskProposalManager riskProposalManager;
    private final PlayerDao playerDao;

    /**
     * 創建風控提案 (投注成功後異步觸發，FLAG/BLOCK 規則匹配時調用)
     *
     * @param createDTO 提案創建 DTO (包含 matched_rules, suspicious_amount 等)
     * @return Option<String> 提案 ID
     */
    public Option<String> createProposal(RiskProposalCreateDTO createDTO) {
        // 驗證玩家存在性
        return Option.of(playerDao.selectById(createDTO.getPlayerId()))
            .flatMap(player -> {
                // 委託給 Manager 層執行事務操作
                String proposalId = riskProposalManager.createProposalWithTransaction(
                    RiskProposalEntity.builder()
                        .proposalNo(generateProposalNo())
                        .betId(createDTO.getBetId())
                        .playerId(createDTO.getPlayerId())
                        .matchedRules(createDTO.getFlaggedRules())
                        .flaggedReasons(createDTO.getFlaggedReasons())
                        .suspiciousAmount(createDTO.getSuspiciousAmount())
                        .status(ProposalStatus.PENDING_REVIEW)
                        .priority(calculatePriority(createDTO))
                        .build()
                );

                return Option.of(proposalId);
            });
    }

    /**
     * 生成提案編號 (格式: RP-YYYYMMDD-HHMMSS-RANDOM)
     */
    private String generateProposalNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String random = RandomStringUtils.randomNumeric(6);
        return String.format("RP-%s-%s", timestamp, random);
    }

    /**
     * 計算提案優先級
     */
    private String calculatePriority(RiskProposalCreateDTO createDTO) {
        BigDecimal amount = createDTO.getSuspiciousAmount();

        // 優先級規則
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return "URGENT";  // 金額 > $10,000
        } else if (amount.compareTo(new BigDecimal("5000")) > 0) {
            return "HIGH";    // 金額 > $5,000
        } else if (amount.compareTo(new BigDecimal("1000")) > 0) {
            return "MEDIUM";  // 金額 > $1,000
        } else {
            return "LOW";     // 金額 ≤ $1,000
        }
    }
}
```

**Manager 層 - 事務處理**:

```java
/**
 * Manager 層 - 風控提案事務管理
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RiskProposalDao riskProposalDao;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 創建風控提案（含事務）
     *
     * @param entity 提案實體
     * @return String 提案 ID
     */
    @Transactional(rollbackFor = Throwable.class)
    public String createProposalWithTransaction(RiskProposalEntity entity) {
        // 1. 插入提案記錄
        riskProposalDao.insert(entity);

        // 2. 發送 Kafka 事件（異步處理）
        kafkaTemplate.send("risk.proposal.created", RiskProposalEvent.builder()
            .proposalId(entity.getId())
            .playerId(entity.getPlayerId())
            .suspiciousAmount(entity.getSuspiciousAmount())
            .priority(entity.getPriority())
            .createdAt(entity.getCreatedAt())
            .build()
        );

        // 3. 返回提案 ID
        return entity.getId().toString();
    }
}
```

### 1.3 提案查詢與關聯

**查詢歷史提案 (取款時使用)**:

```java
/**
 * Service 層 - 查詢歷史風控提案
 */
public class RiskProposalService {

    private final RiskProposalDao riskProposalDao;
    private final RedissonClient redissonClient;

    /**
     * 查詢玩家的待審核提案 (近 30 天)
     * 使用 Redis 緩存提升性能
     *
     * @param playerId 玩家 ID
     * @param days 查詢天數
     * @return Option<List<RiskProposalVO>> 提案列表
     */
    public Option<List<RiskProposalVO>> findPendingProposals(Long playerId, int days) {
        String cacheKey = String.format("risk:proposal:player:%d:days:%d", playerId, days);

        // 1. 嘗試從 Redis 緩存讀取 (TTL 5 分鐘)
        RMap<String, List<RiskProposalVO>> cache = redissonClient.getMap(cacheKey);
        if (cache.isExists()) {
            return Option.of(cache.get("proposals"));
        }

        // 2. 緩存未命中,查詢數據庫
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RiskProposalEntity> entities = riskProposalDao.selectByPlayerIdAndStatus(
            playerId,
            ProposalStatus.PENDING_REVIEW,
            startDate
        );

        // 3. Entity → VO 轉換
        List<RiskProposalVO> vos = entities.stream()
            .map(entity -> SmartBeanUtil.copy(entity, RiskProposalVO.class))
            .collect(Collectors.toList());

        // 4. 寫入 Redis 緩存 (TTL 5 分鐘)
        cache.put("proposals", vos);
        cache.expire(Duration.ofMinutes(5));

        return Option.of(vos);
    }
}
```

**Dao 層查詢實現**:

```java
/**
 * Dao 層 - 風控提案查詢
 */
@Mapper
public interface RiskProposalDao extends BaseMapper<RiskProposalEntity> {

    /**
     * 查詢玩家的待審核提案 (MyBatis-Plus 動態查詢)
     *
     * @param playerId 玩家 ID
     * @param status 提案狀態
     * @param startDate 開始日期
     * @return List<RiskProposalEntity> 提案列表
     */
    default List<RiskProposalEntity> selectByPlayerIdAndStatus(
        Long playerId,
        ProposalStatus status,
        LocalDateTime startDate
    ) {
        return this.selectList(new LambdaQueryWrapper<RiskProposalEntity>()
            .eq(RiskProposalEntity::getPlayerId, playerId)
            .eq(RiskProposalEntity::getStatus, status)
            .ge(RiskProposalEntity::getCreatedAt, startDate)
            .eq(RiskProposalEntity::getDeleted, false)
            .orderByDesc(RiskProposalEntity::getCreatedAt)
        );
    }
}
```

---

## 2. 延遲檢查機制 (Deferred Check Mechanism)

### 2.1 取款時觸發邏輯

**SAGA Step 2.5 實現** (參考 01-05_Withdrawal_Risk.md §5):

```java
/**
 * Service 層 - 出金 SAGA 編排器
 */
public class WithdrawalSagaService {

    private final RiskProposalService riskProposalService;

    /**
     * SAGA Step 2.5: 延遲風控檢查
     *
     * @param withdrawalRequest 取款請求
     * @return Option<StepResult> 步驟結果
     */
    public Option<StepResult> performDeferredRiskCheck(WithdrawalRequest withdrawalRequest) {
        // 1. 查詢歷史風控提案 (近 30 天)
        return riskProposalService
            .findPendingProposals(withdrawalRequest.getPlayerId(), 30)
            .flatMap(proposals -> {
                // 2. 按優先級分類計數（v3.0.0 規則獨立觸發模式）
                long urgentCount = proposals.stream()
                    .filter(p -> "URGENT".equals(p.getPriority()))
                    .count();

                long highCount = proposals.stream()
                    .filter(p -> "HIGH".equals(p.getPriority()))
                    .count();

                long mediumCount = proposals.stream()
                    .filter(p -> "MEDIUM".equals(p.getPriority()))
                    .count();

                // 3. 按優先級決策（不累加分數）
                String decision;
                String reason;
                BigDecimal suspiciousAmount = BigDecimal.ZERO;

                if (urgentCount > 0) {
                    decision = "BLOCKED";
                    reason = "URGENT_PRIORITY_PROPOSALS_PENDING (黑名單/IP封禁)";
                    // 計算 URGENT 優先級提案的總金額
                    suspiciousAmount = proposals.stream()
                        .filter(p -> "URGENT".equals(p.getPriority()))
                        .map(RiskProposalVO::getSuspiciousAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    log.error("[Step 2.5] BLOCKED due to URGENT proposals: count={}", urgentCount);
                } else if (highCount > 0) {
                    decision = "BLOCKED";
                    reason = "HIGH_PRIORITY_PROPOSALS_PENDING (機器人檢測)";
                    // 計算 HIGH 優先級提案的總金額
                    suspiciousAmount = proposals.stream()
                        .filter(p -> "HIGH".equals(p.getPriority()))
                        .map(RiskProposalVO::getSuspiciousAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    log.warn("[Step 2.5] BLOCKED due to HIGH proposals: count={}", highCount);
                } else if (mediumCount > 0) {
                    decision = "MANUAL_REVIEW";
                    reason = "MEDIUM_PRIORITY_PROPOSALS_PENDING (異常投注)";
                    // 計算 MEDIUM 優先級提案的總金額
                    suspiciousAmount = proposals.stream()
                        .filter(p -> "MEDIUM".equals(p.getPriority()))
                        .map(RiskProposalVO::getSuspiciousAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    log.info("[Step 2.5] MANUAL_REVIEW due to MEDIUM proposals: count={}", mediumCount);
                } else {
                    // 無待審核提案 → 繼續 Step 3
                    return Option.of(StepResult.proceed());
                }

                // 4. 生成人工審核提案
                log.info("[Step 2.5] Decision: {}, Reason: {}, Amount: {}", decision, reason, suspiciousAmount);

                String proposalId = riskProposalService.createManualReviewProposal(
                    RiskProposalCreateDTO.builder()
                        .playerId(withdrawalRequest.getPlayerId())
                        .suspiciousAmount(suspiciousAmount)
                        .historicalProposals(proposals)
                        .withdrawalRequestId(withdrawalRequest.getId())
                        .decision(decision)
                        .reason(reason)
                        .build()
                ).getOrNull();

                // 5. 凍結可疑金額,路由至審核隊列
                return Option.of(StepResult.freeze(suspiciousAmount, proposalId, decision, reason));
            });
    }
}
```

### 2.2 歷史投注查詢優化

**性能優化策略**:

| 優化方式 | 說明 | 預期收益 |
|---------|------|---------|
| **Redis 緩存** | TTL 5 分鐘，減少數據庫查詢 | 緩存命中率 >90% |
| **數據庫索引** | `(player_id, status, created_at, deleted)` 複合索引 | 查詢時間 <200ms |
| **分頁限制** | 最多查詢近 30 天數據 | 避免全表掃描 |
| **異步加載** | 使用 `CompletableFuture` 並行查詢 | 響應時間減少 50% |

**複合索引設計**:

```sql
-- 優化歷史提案查詢性能 (覆蓋索引)
CREATE INDEX idx_proposal_query ON t_risk_proposal (
    player_id,
    status,
    created_at DESC,
    deleted
) INCLUDE (id, suspicious_amount, matched_rules);
```

**並行查詢實現**:

```java
/**
 * Service 層 - 並行查詢優化
 */
public class RiskProposalService {

    /**
     * 並行查詢玩家風控提案與注單詳情
     *
     * @param playerId 玩家 ID
     * @return CompletableFuture<CombinedResult> 組合結果
     */
    public CompletableFuture<CombinedResult> queryProposalsWithBets(Long playerId) {
        // 並行查詢
        CompletableFuture<List<RiskProposalVO>> proposalsFuture = CompletableFuture.supplyAsync(
            () -> findPendingProposals(playerId, 30).getOrElse(Collections.emptyList()),
            virtualThreadExecutor
        );

        CompletableFuture<List<BetVO>> betsFuture = CompletableFuture.supplyAsync(
            () -> betService.findByPlayerId(playerId, 30).getOrElse(Collections.emptyList()),
            virtualThreadExecutor
        );

        // 組合結果
        return proposalsFuture.thenCombine(betsFuture, CombinedResult::new);
    }
}
```

### 2.3 按優先級決策規則（v3.0.0）

**核心概念**：
- **不再累加分數**：移除所有提案的分數累加邏輯
- **按優先級判斷**：URGENT > HIGH > MEDIUM > LOW
- **明確決策原因**：記錄具體哪個優先級的提案觸發了阻斷

**決策邏輯**:

```java
/**
 * Service 層 - 按優先級決策（v3.0.0 規則獨立觸發模式）
 */
public class WithdrawalRiskEvaluationService {

    /**
     * 評估取款風險並決策
     *
     * @param proposals 待審核提案列表
     * @return WithdrawalDecision 決策結果
     */
    public WithdrawalDecision evaluateWithdrawalRisk(List<RiskProposalVO> proposals) {
        // 1. 按優先級分類計數
        long urgentCount = proposals.stream()
            .filter(p -> "URGENT".equals(p.getPriority()))
            .count();

        long highCount = proposals.stream()
            .filter(p -> "HIGH".equals(p.getPriority()))
            .count();

        long mediumCount = proposals.stream()
            .filter(p -> "MEDIUM".equals(p.getPriority()))
            .count();

        // 2. 按優先級決策（不累加分數）
        String decision;
        String reason;
        List<String> matchedRules = new ArrayList<>();

        if (urgentCount > 0) {
            decision = "BLOCKED";
            reason = "URGENT_PRIORITY_PROPOSALS_PENDING (黑名單/IP封禁)";
            // 記錄 URGENT 優先級提案觸發的規則
            matchedRules = proposals.stream()
                .filter(p -> "URGENT".equals(p.getPriority()))
                .flatMap(p -> p.getMatchedRules().stream())
                .distinct()
                .collect(Collectors.toList());
            log.error("[Withdrawal] BLOCKED due to URGENT proposals: count={}, rules={}", urgentCount, matchedRules);
        } else if (highCount > 0) {
            decision = "BLOCKED";
            reason = "HIGH_PRIORITY_PROPOSALS_PENDING (機器人檢測)";
            matchedRules = proposals.stream()
                .filter(p -> "HIGH".equals(p.getPriority()))
                .flatMap(p -> p.getMatchedRules().stream())
                .distinct()
                .collect(Collectors.toList());
            log.warn("[Withdrawal] BLOCKED due to HIGH proposals: count={}, rules={}", highCount, matchedRules);
        } else if (mediumCount > 0) {
            decision = "MANUAL_REVIEW";
            reason = "MEDIUM_PRIORITY_PROPOSALS_PENDING (異常投注)";
            matchedRules = proposals.stream()
                .filter(p -> "MEDIUM".equals(p.getPriority()))
                .flatMap(p -> p.getMatchedRules().stream())
                .distinct()
                .collect(Collectors.toList());
            log.info("[Withdrawal] MANUAL_REVIEW due to MEDIUM proposals: count={}, rules={}", mediumCount, matchedRules);
        } else {
            decision = "APPROVED";
            reason = "NO_RISK_PROPOSALS";
            log.info("[Withdrawal] APPROVED, no risk proposals");
        }

        // 3. 返回決策結果（包含明確的觸發規則）
        return WithdrawalDecision.builder()
            .decision(decision)
            .reason(reason)
            .matchedRules(matchedRules)
            .urgentCount(urgentCount)
            .highCount(highCount)
            .mediumCount(mediumCount)
            .build();
    }

    /**
     * 計算指定優先級的可疑金額總和
     *
     * @param proposals 風控提案列表
     * @param priority 優先級 (URGENT/HIGH/MEDIUM/LOW)
     * @return BigDecimal 該優先級的可疑金額總和
     */
    public BigDecimal calculateSuspiciousAmountByPriority(
        List<RiskProposalVO> proposals,
        String priority
    ) {
        return proposals.stream()
            .filter(p -> priority.equals(p.getPriority()))
            .filter(p -> "PENDING_REVIEW".equals(p.getStatus()))
            .map(RiskProposalVO::getSuspiciousAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**審計追溯性提升**:

```
舊模式（分數累加）：
玩家取款被阻斷
審計日誌：總風險分數 = 85 分
問題：無法追溯具體哪條規則導致阻斷

新模式（規則獨立觸發）：
玩家取款被阻斷
審計日誌：
  - 決策: BLOCKED
  - 原因: URGENT_PRIORITY_PROPOSALS_PENDING
  - URGENT 提案數量: 2
  - 觸發規則: [BLACKLIST_PLAYER, IP_BLOCKED]
  - 可疑金額: $3,500 (URGENT 優先級提案總和)

明確追溯到具體規則，審計清晰
```

---

## 相關文檔

- [05-02-01 檢測模型](./05-02-01_Detection_Model.md) - 系統概述與架構
- [05-02-03 ML 整合](./05-02-03_ML_Integration.md) - 多維度風控規則
- [05-02-04 運營工具](./05-02-04_Operations_Tools.md) - SmartAdmin 架構映射與監控
