# 05-04 風控提案人工審核工作流 (Risk Proposal Manual Review Workflow)

## 📋 文檔信息

**文檔版本**: 1.0.0 (v2.1.0 簡化版)
**最後更新**: 2026-02-02
**維護團隊**: Risk Team & Compliance Team
**前置依賴**:
- [05-03 配置驅動風控系統](./05-03_Passive_Risk_Control_System.md) - 風控提案服務實現
- [05-01 風控系統架構](./05-01_Risk_Control_System.md) - 配置驅動規則引擎
- [02-01 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md) - SAGA Step 2.5 延遲檢查

---

## 🎯 執行摘要

SmartAdmin iGaming v2.1.0 引入「人工審核為主」的風控提案工作流設計，簡化自動化決策邏輯，將所有可疑案例路由至人工審核隊列，由審核員根據具體情況做出批准、拒絕或部分批准決策。

### 核心理念（v2.1.0 簡化）

| 設計原則 | 說明 | 業務價值 |
|---------|------|---------|
| **人工為主** | 所有可疑金額 > 0 的案例都生成人工審核提案 | 避免過度自動化，降低誤判風險 |
| **無複雜閾值** | ❌ 取消 $500/$5000 自動路由規則 | 簡化決策邏輯，易於維護 |
| **審核員自主** | 審核員可手動升級複雜案例 | 靈活應對各種場景 |
| **透明決策** | 所有審核操作記錄完整日誌 | 合規審計要求 |

### 與傳統工作流的差異

| 維度 | 傳統工作流 (v2.0.0) | v2.1.0 簡化工作流 |
|------|-------------------|-----------------|
| **路由規則** | ❌ 複雜閾值判斷（$500/$5000） | ✅ 統一路由至審核隊列 |
| **自動批准** | ❌ $500 以下自動批准 | ✅ 無自動批准，全部人工審核 |
| **審核分層** | ❌ L1/L2 強制路由 | ✅ 審核員手動升級 |
| **SLA 管理** | ❌ 固定 L1=24h, L2=48h | ✅ 統一 SLA 48 小時 |
| **決策複雜度** | ❌ 多層自動化決策 | ✅ 簡化為人工決策 |

---

## 1. 審核隊列管理

### 1.1 優先級算法（v2.1.0 簡化）

**優先級計算規則**:

```java
/**
 * Service 層 - 提案優先級計算（v2.1.0 簡化）
 */
public class RiskProposalPriorityService {

    /**
     * 計算提案優先級
     * 規則（v2.1.0 簡化）:
     * - URGENT: 金額 > $10,000
     * - HIGH:   金額 > $5,000
     * - MEDIUM: 金額 > $1,000
     * - LOW:    金額 ≤ $1,000
     *
     * @param suspiciousAmount 可疑金額
     * @return String 優先級
     */
    public String calculatePriority(BigDecimal suspiciousAmount) {
        if (suspiciousAmount.compareTo(new BigDecimal("10000")) > 0) {
            return "URGENT";
        } else if (suspiciousAmount.compareTo(new BigDecimal("5000")) > 0) {
            return "HIGH";
        } else if (suspiciousAmount.compareTo(new BigDecimal("1000")) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 更新提案優先級（根據等待時間動態調整）
     * v2.1.0 簡化：僅在超過 SLA 時升級優先級
     *
     * @param proposalId 提案 ID
     */
    @Scheduled(fixedDelay = 3600000)  // 每小時執行一次
    public void updatePriorityBasedOnWaitTime() {
        LocalDateTime slaThreshold = LocalDateTime.now().minusHours(48);

        // 查詢超過 SLA 的待審核提案
        List<RiskProposalEntity> overdueProposals = riskProposalDao.selectList(
            new LambdaQueryWrapper<RiskProposalEntity>()
                .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                .lt(RiskProposalEntity::getCreatedAt, slaThreshold)
                .eq(RiskProposalEntity::getDeleted, false)
        );

        // 升級優先級
        overdueProposals.forEach(proposal -> {
            String newPriority = upgradePriority(proposal.getPriority());
            proposal.setPriority(newPriority);
            riskProposalDao.updateById(proposal);

            log.warn("[Priority Upgrade] Proposal {} priority upgraded to {} (overdue)",
                proposal.getId(), newPriority);
        });
    }

    private String upgradePriority(String currentPriority) {
        return switch (currentPriority) {
            case "LOW" -> "MEDIUM";
            case "MEDIUM" -> "HIGH";
            case "HIGH" -> "URGENT";
            default -> "URGENT";
        };
    }
}
```

### 1.2 SLA 管理機制（v2.1.0 簡化）

**統一 SLA 標準**: 所有提案統一 SLA 48 小時（取消 L1/L2 分層）

**SLA 監控服務**:

```java
/**
 * Service 層 - SLA 監控（v2.1.0 簡化）
 */
@Service
@RequiredArgsConstructor
public class RiskProposalSLAService {

    private final RiskProposalDao riskProposalDao;
    private final NotificationService notificationService;

    /**
     * 統一 SLA: 48 小時（取消 L1/L2 分層）
     */
    private static final int UNIFIED_SLA_HOURS = 48;

    /**
     * 檢查 SLA 違規（定時任務，每小時執行一次）
     */
    @Scheduled(fixedDelay = 3600000)
    public void checkSLAViolations() {
        LocalDateTime slaThreshold = LocalDateTime.now().minusHours(UNIFIED_SLA_HOURS);

        // 查詢超過 SLA 的待審核提案
        List<RiskProposalEntity> overdueProposals = riskProposalDao.selectList(
            new LambdaQueryWrapper<RiskProposalEntity>()
                .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                .lt(RiskProposalEntity::getCreatedAt, slaThreshold)
                .eq(RiskProposalEntity::getDeleted, false)
        );

        if (overdueProposals.isEmpty()) {
            return;
        }

        // 發送告警通知
        notificationService.sendSLAViolationAlert(
            String.format("[SLA Violation] %d proposals overdue (SLA: %d hours)",
                overdueProposals.size(), UNIFIED_SLA_HOURS),
            overdueProposals
        );

        log.error("[SLA Violation] {} proposals overdue, SLA: {} hours",
            overdueProposals.size(), UNIFIED_SLA_HOURS);
    }

    /**
     * 計算提案剩餘時間
     *
     * @param proposalId 提案 ID
     * @return Duration 剩餘時間
     */
    public Duration calculateRemainingTime(Long proposalId) {
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        if (proposal == null) {
            return Duration.ZERO;
        }

        LocalDateTime slaDeadline = proposal.getCreatedAt().plusHours(UNIFIED_SLA_HOURS);
        return Duration.between(LocalDateTime.now(), slaDeadline);
    }

    /**
     * 查詢即將超過 SLA 的提案（未來 6 小時內）
     *
     * @return List<RiskProposalVO> 提案列表
     */
    public List<RiskProposalVO> findApproachingSLA() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.minusHours(UNIFIED_SLA_HOURS - 6);  // 剩餘 6 小時

        List<RiskProposalEntity> entities = riskProposalDao.selectList(
            new LambdaQueryWrapper<RiskProposalEntity>()
                .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                .lt(RiskProposalEntity::getCreatedAt, warningThreshold)
                .ge(RiskProposalEntity::getCreatedAt, now.minusHours(UNIFIED_SLA_HOURS))
                .eq(RiskProposalEntity::getDeleted, false)
                .orderByAsc(RiskProposalEntity::getCreatedAt)
        );

        return entities.stream()
            .map(entity -> SmartBeanUtil.copy(entity, RiskProposalVO.class))
            .collect(Collectors.toList());
    }
}
```

### 1.3 審核隊列查詢

**Controller API**:

```java
/**
 * Controller 層 - 審核隊列查詢
 */
@Tag(name = "風控提案審核隊列")
@RestController
@RequestMapping("/risk/proposal/queue")
@RequiredArgsConstructor
public class RiskProposalQueueController {

    private final RiskProposalService riskProposalService;
    private final RiskProposalSLAService slaService;

    /**
     * 查詢審核隊列（按優先級排序）
     *
     * @param queryForm 查詢表單
     * @return ResponseDTO<PageResult<RiskProposalVO>> 分頁結果
     */
    @Operation(summary = "查詢審核隊列")
    @PostMapping("/query")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<PageResult<RiskProposalVO>> queryReviewQueue(
        @Valid @RequestBody RiskProposalQueryForm queryForm
    ) {
        // 默認按優先級（降序）+ 創建時間（升序）排序
        queryForm.setOrderBy("priority DESC, created_at ASC");

        return riskProposalService.queryProposals(queryForm)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error("查詢失敗"));
    }

    /**
     * 查詢即將超過 SLA 的提案
     *
     * @return ResponseDTO<List<RiskProposalVO>> 提案列表
     */
    @Operation(summary = "查詢即將超過 SLA 的提案")
    @GetMapping("/approaching-sla")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<List<RiskProposalVO>> queryApproachingSLA() {
        List<RiskProposalVO> proposals = slaService.findApproachingSLA();
        return ResponseDTO.ok(proposals);
    }

    /**
     * 領取審核任務（審核員主動領取）
     *
     * @param proposalId 提案 ID
     * @return ResponseDTO<Void> 操作結果
     */
    @Operation(summary = "領取審核任務")
    @PostMapping("/{proposalId}/claim")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> claimReviewTask(@PathVariable Long proposalId) {
        Long reviewerId = StpUtil.getLoginIdAsLong();

        boolean success = riskProposalService.claimReviewTask(proposalId, reviewerId)
            .getOrElse(false);

        return success ? ResponseDTO.ok() : ResponseDTO.error("領取失敗");
    }
}
```

---

## 2. 審核決策矩陣

### 2.1 決策類型

**四種審核決策**（v2.1.0）:

| 決策類型 | 說明 | 後續動作 | 狀態流轉 |
|---------|------|---------|---------|
| **批准 (Approve)** | 可疑金額無問題，全額放行 | 解凍金額，繼續出金流程 | PENDING_REVIEW → APPROVED |
| **拒絕 (Reject)** | 確認為違規行為，拒絕出金 | 保持凍結，扣款處理 | PENDING_REVIEW → REJECTED |
| **部分批准 (Partial Approve)** | 部分金額合規，部分可疑 | 批准金額放行，剩餘凍結 | PENDING_REVIEW → PARTIAL_APPROVED |
| **升級審核 (Escalate)** | 案例複雜，需高級審核 | 路由至高級分析師 | PENDING_REVIEW → ESCALATED |

### 2.2 批准 (Approve) 流程

**Service 層實現**:

```java
/**
 * Service 層 - 批准提案
 */
public class RiskProposalService {

    /**
     * 批准風控提案（全額放行）
     *
     * @param proposalId 提案 ID
     * @param reviewerId 審核員 ID
     * @param reviewNotes 審核備註
     * @return Option<Boolean> 是否成功
     */
    public Option<Boolean> approveProposal(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // 驗證提案狀態
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    log.error("[Approve Failed] Proposal {} not in PENDING_REVIEW status", proposalId);
                    return Option.none();
                }

                // 委託 Manager 層執行事務
                boolean success = riskProposalManager.approveProposalWithTransaction(
                    proposalId,
                    reviewerId,
                    reviewNotes
                );

                return Option.of(success);
            });
    }
}
```

**Manager 層事務實現**:

```java
/**
 * Manager 層 - 批准提案事務處理
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RiskProposalDao riskProposalDao;
    private final WithdrawalService withdrawalService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 批准提案（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean approveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 1. 更新提案狀態
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("APPROVED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(proposal.getSuspiciousAmount());  // 全額批准

        riskProposalDao.updateById(proposal);

        // 2. 解凍金額，繼續出金流程
        if (proposal.getWithdrawalRequestId() != null) {
            withdrawalService.unfreezeAmount(
                proposal.getWithdrawalRequestId(),
                proposal.getSuspiciousAmount()
            );
        }

        // 3. 發送 Kafka 事件
        kafkaTemplate.send("risk.proposal.approved", RiskProposalEvent.builder()
            .proposalId(proposal.getId())
            .playerId(proposal.getPlayerId())
            .reviewerId(reviewerId)
            .approvedAmount(proposal.getSuspiciousAmount())
            .build()
        );

        log.info("[Proposal Approved] proposalId={}, reviewerId={}, amount={}",
            proposalId, reviewerId, proposal.getSuspiciousAmount());

        return true;
    }
}
```

### 2.3 拒絕 (Reject) 流程

**Service 層實現**:

```java
/**
 * Service 層 - 拒絕提案
 */
public class RiskProposalService {

    /**
     * 拒絕風控提案
     *
     * @param proposalId 提案 ID
     * @param reviewerId 審核員 ID
     * @param reviewNotes 審核備註（必填）
     * @return Option<Boolean> 是否成功
     */
    public Option<Boolean> rejectProposal(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 驗證審核備註（拒絕必須提供理由）
        if (StringUtils.isBlank(reviewNotes)) {
            log.error("[Reject Failed] Review notes required for rejection");
            return Option.none();
        }

        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // 驗證提案狀態
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    return Option.none();
                }

                // 委託 Manager 層執行事務
                boolean success = riskProposalManager.rejectProposalWithTransaction(
                    proposalId,
                    reviewerId,
                    reviewNotes
                );

                return Option.of(success);
            });
    }
}
```

**Manager 層事務實現**:

```java
/**
 * Manager 層 - 拒絕提案事務處理
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * 拒絕提案（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean rejectProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 1. 更新提案狀態
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("REJECTED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(BigDecimal.ZERO);  // 拒絕，批准金額 = 0

        riskProposalDao.updateById(proposal);

        // 2. 可疑金額保持凍結，觸發扣款補償
        if (proposal.getWithdrawalRequestId() != null) {
            compensationService.executeDeduction(
                proposal.getPlayerId(),
                proposal.getSuspiciousAmount(),
                String.format("風控提案拒絕: %s (Proposal: %s)", reviewNotes, proposal.getProposalNo())
            );
        }

        // 3. 發送 Kafka 事件
        kafkaTemplate.send("risk.proposal.rejected", RiskProposalEvent.builder()
            .proposalId(proposal.getId())
            .playerId(proposal.getPlayerId())
            .reviewerId(reviewerId)
            .rejectionReason(reviewNotes)
            .build()
        );

        log.info("[Proposal Rejected] proposalId={}, reviewerId={}, reason={}",
            proposalId, reviewerId, reviewNotes);

        return true;
    }
}
```

### 2.4 部分批准 (Partial Approve) 流程

**Service 層實現**:

```java
/**
 * Service 層 - 部分批准提案
 */
public class RiskProposalService {

    /**
     * 部分批准風控提案
     *
     * @param proposalId 提案 ID
     * @param reviewerId 審核員 ID
     * @param approvedAmount 批准金額
     * @param reviewNotes 審核備註（必填）
     * @return Option<Boolean> 是否成功
     */
    public Option<Boolean> partialApproveProposal(
        Long proposalId,
        Long reviewerId,
        BigDecimal approvedAmount,
        String reviewNotes
    ) {
        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // 驗證批准金額
                if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                    approvedAmount.compareTo(proposal.getSuspiciousAmount()) >= 0) {
                    log.error("[Partial Approve Failed] Invalid approved amount: {}", approvedAmount);
                    return Option.none();
                }

                // 委託 Manager 層執行事務
                boolean success = riskProposalManager.partialApproveProposalWithTransaction(
                    proposalId,
                    reviewerId,
                    approvedAmount,
                    reviewNotes
                );

                return Option.of(success);
            });
    }
}
```

**Manager 層事務實現**:

```java
/**
 * Manager 層 - 部分批准提案事務處理
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * 部分批准提案（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean partialApproveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        BigDecimal approvedAmount,
        String reviewNotes
    ) {
        // 1. 更新提案狀態
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        BigDecimal rejectedAmount = proposal.getSuspiciousAmount().subtract(approvedAmount);

        proposal.setStatus("PARTIAL_APPROVED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(approvedAmount);

        riskProposalDao.updateById(proposal);

        // 2. 批准金額解凍，拒絕金額保持凍結
        if (proposal.getWithdrawalRequestId() != null) {
            withdrawalService.unfreezeAmount(proposal.getWithdrawalRequestId(), approvedAmount);

            // 拒絕金額扣款
            compensationService.executeDeduction(
                proposal.getPlayerId(),
                rejectedAmount,
                String.format("部分批准: 批准 $%s, 拒絕 $%s (Proposal: %s)",
                    approvedAmount, rejectedAmount, proposal.getProposalNo())
            );
        }

        // 3. 發送 Kafka 事件
        kafkaTemplate.send("risk.proposal.partial_approved", RiskProposalEvent.builder()
            .proposalId(proposal.getId())
            .playerId(proposal.getPlayerId())
            .reviewerId(reviewerId)
            .approvedAmount(approvedAmount)
            .rejectedAmount(rejectedAmount)
            .build()
        );

        log.info("[Proposal Partial Approved] proposalId={}, approved={}, rejected={}",
            proposalId, approvedAmount, rejectedAmount);

        return true;
    }
}
```

### 2.5 升級審核 (Escalate) 流程

**Service 層實現**:

```java
/**
 * Service 層 - 升級提案至高級審核
 */
public class RiskProposalService {

    /**
     * 升級提案至高級審核（v2.1.0 手動升級）
     *
     * @param proposalId 提案 ID
     * @param reviewerId 當前審核員 ID
     * @param escalationReason 升級原因（必填）
     * @return Option<Boolean> 是否成功
     */
    public Option<Boolean> escalateProposal(
        Long proposalId,
        Long reviewerId,
        String escalationReason
    ) {
        // 驗證升級原因
        if (StringUtils.isBlank(escalationReason)) {
            log.error("[Escalate Failed] Escalation reason required");
            return Option.none();
        }

        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // 驗證提案狀態
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    return Option.none();
                }

                // 委託 Manager 層執行事務
                boolean success = riskProposalManager.escalateProposalWithTransaction(
                    proposalId,
                    reviewerId,
                    escalationReason
                );

                return Option.of(success);
            });
    }
}
```

**Manager 層事務實現**:

```java
/**
 * Manager 層 - 升級提案事務處理
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * 升級提案至高級審核（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean escalateProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String escalationReason
    ) {
        // 1. 更新提案狀態
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("ESCALATED");
        proposal.setPriority("URGENT");  // 升級後優先級設為 URGENT
        proposal.setReviewNotes(String.format("[Escalated by Reviewer %d] %s",
            reviewerId, escalationReason));

        riskProposalDao.updateById(proposal);

        // 2. 發送通知給高級分析師
        notificationService.sendEscalationNotification(
            proposal,
            reviewerId,
            escalationReason
        );

        // 3. 發送 Kafka 事件
        kafkaTemplate.send("risk.proposal.escalated", RiskProposalEvent.builder()
            .proposalId(proposal.getId())
            .playerId(proposal.getPlayerId())
            .escalatedBy(reviewerId)
            .escalationReason(escalationReason)
            .build()
        );

        log.info("[Proposal Escalated] proposalId={}, escalatedBy={}, reason={}",
            proposalId, reviewerId, escalationReason);

        return true;
    }
}
```

---

## 3. 補償機制

### 3.1 補償類型

**三種補償類型**:

| 補償類型 | 場景 | 金額流向 | 示例 |
|---------|------|---------|------|
| **退款 (REFUND)** | 誤判案例，退還凍結金額 | 系統 → 玩家錢包 | 玩家投訴後確認無問題 |
| **調帳 (ADJUSTMENT)** | 流水計算錯誤，補償差額 | 系統 → 玩家錢包 | 流水誤扣，需補回差額 |
| **扣款 (DEDUCTION)** | 確認違規，扣除可疑金額 | 玩家錢包 → 系統 | 拒絕提案後扣除凍結金額 |

### 3.2 補償執行流程

**CompensationService 實現**:

```java
/**
 * Service 層 - 補償服務
 */
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final CompensationManager compensationManager;
    private final WalletService walletService;

    /**
     * 執行退款補償
     *
     * @param playerId 玩家 ID
     * @param amount 退款金額
     * @param reason 退款原因
     * @return Option<String> 補償記錄 ID
     */
    public Option<String> executeRefund(Long playerId, BigDecimal amount, String reason) {
        // 驗證玩家錢包
        return walletService.getPlayerWallet(playerId)
            .flatMap(wallet -> {
                // 委託 Manager 層執行事務
                String compensationId = compensationManager.executeRefundWithTransaction(
                    playerId,
                    amount,
                    reason
                );

                return Option.of(compensationId);
            });
    }

    /**
     * 執行扣款補償
     *
     * @param playerId 玩家 ID
     * @param amount 扣款金額
     * @param reason 扣款原因
     * @return Option<String> 補償記錄 ID
     */
    public Option<String> executeDeduction(Long playerId, BigDecimal amount, String reason) {
        return walletService.getPlayerWallet(playerId)
            .flatMap(wallet -> {
                // 驗證餘額充足
                if (wallet.getBalance().compareTo(amount) < 0) {
                    log.error("[Deduction Failed] Insufficient balance for player {}", playerId);
                    return Option.none();
                }

                // 委託 Manager 層執行事務
                String compensationId = compensationManager.executeDeductionWithTransaction(
                    playerId,
                    amount,
                    reason
                );

                return Option.of(compensationId);
            });
    }
}
```

**Manager 層事務實現**:

```java
/**
 * Manager 層 - 補償事務處理
 */
@Service
@RequiredArgsConstructor
public class CompensationManager {

    private final CompensationDao compensationDao;
    private final WalletDao walletDao;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 執行退款補償（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public String executeRefundWithTransaction(Long playerId, BigDecimal amount, String reason) {
        // 1. 記錄補償記錄
        CompensationEntity compensation = CompensationEntity.builder()
            .compensationNo(generateCompensationNo())
            .playerId(playerId)
            .compensationType("REFUND")
            .amount(amount)
            .reason(reason)
            .status("PENDING")
            .build();

        compensationDao.insert(compensation);

        // 2. 更新玩家錢包（增加餘額）
        WalletEntity wallet = walletDao.selectByPlayerId(playerId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletDao.updateById(wallet);

        // 3. 更新補償狀態
        compensation.setStatus("COMPLETED");
        compensation.setExecutedAt(LocalDateTime.now());
        compensationDao.updateById(compensation);

        // 4. 發送 Kafka 事件
        kafkaTemplate.send("wallet.compensation.refund", CompensationEvent.builder()
            .compensationId(compensation.getId())
            .playerId(playerId)
            .amount(amount)
            .type("REFUND")
            .build()
        );

        log.info("[Refund Executed] playerId={}, amount={}, reason={}",
            playerId, amount, reason);

        return compensation.getId().toString();
    }

    /**
     * 執行扣款補償（含事務）
     */
    @Transactional(rollbackFor = Throwable.class)
    public String executeDeductionWithTransaction(Long playerId, BigDecimal amount, String reason) {
        // 1. 記錄補償記錄
        CompensationEntity compensation = CompensationEntity.builder()
            .compensationNo(generateCompensationNo())
            .playerId(playerId)
            .compensationType("DEDUCTION")
            .amount(amount)
            .reason(reason)
            .status("PENDING")
            .build();

        compensationDao.insert(compensation);

        // 2. 更新玩家錢包（扣除餘額）
        WalletEntity wallet = walletDao.selectByPlayerId(playerId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for deduction");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletDao.updateById(wallet);

        // 3. 更新補償狀態
        compensation.setStatus("COMPLETED");
        compensation.setExecutedAt(LocalDateTime.now());
        compensationDao.updateById(compensation);

        // 4. 發送 Kafka 事件
        kafkaTemplate.send("wallet.compensation.deduction", CompensationEvent.builder()
            .compensationId(compensation.getId())
            .playerId(playerId)
            .amount(amount)
            .type("DEDUCTION")
            .build()
        );

        log.info("[Deduction Executed] playerId={}, amount={}, reason={}",
            playerId, amount, reason);

        return compensation.getId().toString();
    }

    private String generateCompensationNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String random = RandomStringUtils.randomNumeric(6);
        return String.format("COMP-%s-%s", timestamp, random);
    }
}
```

### 3.3 補償失敗處理

**重試機制（指數退避）**:

```java
/**
 * Service 層 - 補償重試服務
 */
@Service
@RequiredArgsConstructor
public class CompensationRetryService {

    private final CompensationDao compensationDao;
    private final CompensationService compensationService;

    /**
     * 重試失敗的補償（定時任務，每 10 分鐘執行一次）
     */
    @Scheduled(fixedDelay = 600000)
    public void retryFailedCompensations() {
        // 查詢失敗的補償記錄
        List<CompensationEntity> failedCompensations = compensationDao.selectList(
            new LambdaQueryWrapper<CompensationEntity>()
                .eq(CompensationEntity::getStatus, "FAILED")
                .lt(CompensationEntity::getRetryCount, 3)  // 最多重試 3 次
                .eq(CompensationEntity::getDeleted, false)
        );

        failedCompensations.forEach(compensation -> {
            try {
                // 指數退避延遲
                long delayMs = (long) Math.pow(2, compensation.getRetryCount()) * 1000;
                Thread.sleep(delayMs);

                // 重試補償
                retryCompensation(compensation);

            } catch (Exception e) {
                log.error("[Retry Failed] compensationId={}, error={}",
                    compensation.getId(), e.getMessage());
            }
        });
    }

    private void retryCompensation(CompensationEntity compensation) {
        try {
            // 根據補償類型重試
            switch (compensation.getCompensationType()) {
                case "REFUND":
                    compensationService.executeRefund(
                        compensation.getPlayerId(),
                        compensation.getAmount(),
                        compensation.getReason()
                    );
                    break;

                case "DEDUCTION":
                    compensationService.executeDeduction(
                        compensation.getPlayerId(),
                        compensation.getAmount(),
                        compensation.getReason()
                    );
                    break;

                case "ADJUSTMENT":
                    compensationService.executeAdjustment(
                        compensation.getPlayerId(),
                        compensation.getAmount(),
                        compensation.getReason()
                    );
                    break;
            }

            // 更新重試計數
            compensation.setRetryCount(compensation.getRetryCount() + 1);
            compensation.setStatus("COMPLETED");
            compensationDao.updateById(compensation);

            log.info("[Retry Success] compensationId={}", compensation.getId());

        } catch (Exception e) {
            // 重試失敗，更新重試計數
            compensation.setRetryCount(compensation.getRetryCount() + 1);

            if (compensation.getRetryCount() >= 3) {
                compensation.setStatus("PERMANENTLY_FAILED");
                log.error("[Retry Exhausted] compensationId={}", compensation.getId());
            }

            compensationDao.updateById(compensation);
        }
    }
}
```

---

## 4. 審核員權限管理（v2.1.0 簡化）

### 4.1 權限角色定義

**三級權限結構**（v2.1.0 簡化）:

| 角色 | 權限 | 職責 | 操作範圍 |
|------|------|------|---------|
| **審核員 (Reviewer)** | `risk:proposal:review` | 處理所有風控提案（批准/拒絕/部分批准/升級） | 全部提案 |
| **高級分析師 (Senior Analyst)** | `risk:proposal:escalate_review` | 處理升級案例，提供諮詢 | ESCALATED 提案 |
| **合規主管 (Compliance Manager)** | `risk:proposal:final_decision` | 最終決策，政策制定 | 全部提案 + 政策配置 |

**關鍵變更（v2.1.0）**:
- ✅ 審核員可處理所有提案（取消 L1/L2 自動路由）
- ✅ 審核員可手動升級複雜案例
- ✅ 無自動分配邏輯（審核員主動領取任務）

### 4.2 權限配置

**Sa-Token 權限註解**:

```java
/**
 * Controller 層 - 權限控制
 */
@RestController
@RequestMapping("/risk/proposal")
public class RiskProposalController {

    /**
     * 審核員權限：查詢審核隊列
     */
    @PostMapping("/queue/query")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<PageResult<RiskProposalVO>> queryReviewQueue(
        @RequestBody RiskProposalQueryForm queryForm
    ) {
        // ...
    }

    /**
     * 審核員權限：批准提案
     */
    @PostMapping("/{proposalId}/approve")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> approveProposal(
        @PathVariable Long proposalId,
        @RequestParam String reviewNotes
    ) {
        // ...
    }

    /**
     * 高級分析師權限：處理升級案例
     */
    @PostMapping("/{proposalId}/escalate-decision")
    @SaCheckPermission("risk:proposal:escalate_review")
    public ResponseDTO<Void> handleEscalatedProposal(
        @PathVariable Long proposalId,
        @RequestBody EscalatedDecisionForm form
    ) {
        // ...
    }

    /**
     * 合規主管權限：配置風控規則
     */
    @PostMapping("/rules/config")
    @SaCheckPermission("risk:proposal:final_decision")
    public ResponseDTO<Void> configureRiskRules(
        @RequestBody RiskRuleConfigForm form
    ) {
        // ...
    }
}
```

### 4.3 審核員工作流

**審核員操作流程**:

```
1. 登入系統 → 查看審核隊列（按優先級排序）
   ↓
2. 領取審核任務（主動領取，非自動分配）
   ↓
3. 查看提案詳情（注單歷史、風控規則匹配、玩家資料）
   ↓
4. 做出決策:
   - 簡單案例 → 直接批准/拒絕/部分批准
   - 複雜案例 → 手動升級至高級分析師
   ↓
5. 填寫審核備註（拒絕/部分批准必填）
   ↓
6. 提交決策 → 系統自動執行補償邏輯
```

**Controller API**:

```java
/**
 * Controller 層 - 審核員操作 API
 */
@Tag(name = "風控提案審核操作")
@RestController
@RequestMapping("/risk/proposal/review")
@RequiredArgsConstructor
public class RiskProposalReviewController {

    private final RiskProposalService riskProposalService;

    /**
     * 審核員領取任務（主動領取）
     */
    @Operation(summary = "領取審核任務")
    @PostMapping("/{proposalId}/claim")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> claimTask(@PathVariable Long proposalId) {
        Long reviewerId = StpUtil.getLoginIdAsLong();

        boolean success = riskProposalService.claimReviewTask(proposalId, reviewerId)
            .getOrElse(false);

        return success ? ResponseDTO.ok() : ResponseDTO.error("領取失敗");
    }

    /**
     * 提交審核決策（統一入口）
     */
    @Operation(summary = "提交審核決策")
    @PostMapping("/{proposalId}/decision")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> submitDecision(
        @PathVariable Long proposalId,
        @Valid @RequestBody ReviewDecisionForm form
    ) {
        Long reviewerId = StpUtil.getLoginIdAsLong();

        boolean success = switch (form.getDecisionType()) {
            case "APPROVE" -> riskProposalService.approveProposal(
                proposalId, reviewerId, form.getReviewNotes()
            ).getOrElse(false);

            case "REJECT" -> riskProposalService.rejectProposal(
                proposalId, reviewerId, form.getReviewNotes()
            ).getOrElse(false);

            case "PARTIAL_APPROVE" -> riskProposalService.partialApproveProposal(
                proposalId, reviewerId, form.getApprovedAmount(), form.getReviewNotes()
            ).getOrElse(false);

            case "ESCALATE" -> riskProposalService.escalateProposal(
                proposalId, reviewerId, form.getEscalationReason()
            ).getOrElse(false);

            default -> false;
        };

        return success ? ResponseDTO.ok() : ResponseDTO.error("決策提交失敗");
    }
}
```

---

## 5. SmartAdmin 工作流集成

### 5.1 Camunda BPMN 流程定義

**風控提案審核 BPMN 流程**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn">

  <bpmn:process id="RiskProposalReviewProcess" name="風控提案審核流程" isExecutable="true">

    <!-- 開始事件 -->
    <bpmn:startEvent id="StartEvent" name="風控提案創建">
      <bpmn:outgoing>Flow1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- 人工任務：審核員審核 -->
    <bpmn:userTask id="ReviewerTask" name="審核員審核"
                   camunda:assignee="${reviewerId}"
                   camunda:candidateGroups="risk_reviewers">
      <bpmn:incoming>Flow1</bpmn:incoming>
      <bpmn:outgoing>Flow2</bpmn:outgoing>
    </bpmn:userTask>

    <!-- 排他網關：決策路由 -->
    <bpmn:exclusiveGateway id="DecisionGateway" name="審核決策">
      <bpmn:incoming>Flow2</bpmn:incoming>
      <bpmn:outgoing>FlowApprove</bpmn:outgoing>
      <bpmn:outgoing>FlowReject</bpmn:outgoing>
      <bpmn:outgoing>FlowPartialApprove</bpmn:outgoing>
      <bpmn:outgoing>FlowEscalate</bpmn:outgoing>
    </bpmn:exclusiveGateway>

    <!-- 服務任務：批准補償 -->
    <bpmn:serviceTask id="ApproveCompensationTask" name="執行批准補償"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowApprove</bpmn:incoming>
      <bpmn:outgoing>FlowEnd1</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 服務任務：拒絕補償 -->
    <bpmn:serviceTask id="RejectCompensationTask" name="執行拒絕補償"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowReject</bpmn:incoming>
      <bpmn:outgoing>FlowEnd2</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 服務任務：部分批准補償 -->
    <bpmn:serviceTask id="PartialApproveCompensationTask" name="執行部分批准補償"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowPartialApprove</bpmn:incoming>
      <bpmn:outgoing>FlowEnd3</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 人工任務：高級分析師審核 -->
    <bpmn:userTask id="SeniorAnalystTask" name="高級分析師審核"
                   camunda:candidateGroups="senior_analysts">
      <bpmn:incoming>FlowEscalate</bpmn:incoming>
      <bpmn:outgoing>Flow2</bpmn:outgoing>  <!-- 回到決策網關 -->
    </bpmn:userTask>

    <!-- 結束事件 -->
    <bpmn:endEvent id="EndEvent" name="審核完成">
      <bpmn:incoming>FlowEnd1</bpmn:incoming>
      <bpmn:incoming>FlowEnd2</bpmn:incoming>
      <bpmn:incoming>FlowEnd3</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 流程邊 -->
    <bpmn:sequenceFlow id="Flow1" sourceRef="StartEvent" targetRef="ReviewerTask"/>
    <bpmn:sequenceFlow id="Flow2" sourceRef="ReviewerTask" targetRef="DecisionGateway"/>

    <bpmn:sequenceFlow id="FlowApprove" sourceRef="DecisionGateway" targetRef="ApproveCompensationTask">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
        ${decisionType == 'APPROVE'}
      </bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <bpmn:sequenceFlow id="FlowReject" sourceRef="DecisionGateway" targetRef="RejectCompensationTask">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
        ${decisionType == 'REJECT'}
      </bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <bpmn:sequenceFlow id="FlowPartialApprove" sourceRef="DecisionGateway" targetRef="PartialApproveCompensationTask">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
        ${decisionType == 'PARTIAL_APPROVE'}
      </bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <bpmn:sequenceFlow id="FlowEscalate" sourceRef="DecisionGateway" targetRef="SeniorAnalystTask">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">
        ${decisionType == 'ESCALATE'}
      </bpmn:conditionExpression>
    </bpmn:sequenceFlow>

    <bpmn:sequenceFlow id="FlowEnd1" sourceRef="ApproveCompensationTask" targetRef="EndEvent"/>
    <bpmn:sequenceFlow id="FlowEnd2" sourceRef="RejectCompensationTask" targetRef="EndEvent"/>
    <bpmn:sequenceFlow id="FlowEnd3" sourceRef="PartialApproveCompensationTask" targetRef="EndEvent"/>

  </bpmn:process>

</bpmn:definitions>
```

### 5.2 Camunda 流程啟動

**Service 層 - 啟動 BPMN 流程**:

```java
/**
 * Service 層 - Camunda 工作流集成
 */
@Service
@RequiredArgsConstructor
public class RiskProposalWorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * 啟動風控提案審核流程
     *
     * @param proposalId 提案 ID
     * @return String 流程實例 ID
     */
    public String startReviewProcess(Long proposalId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("proposalId", proposalId);
        variables.put("startTime", LocalDateTime.now());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "RiskProposalReviewProcess",
            String.valueOf(proposalId),
            variables
        );

        log.info("[Workflow Started] proposalId={}, processInstanceId={}",
            proposalId, processInstance.getId());

        return processInstance.getId();
    }

    /**
     * 完成審核任務（提交決策）
     *
     * @param taskId Camunda 任務 ID
     * @param decisionType 決策類型
     * @param variables 流程變量
     */
    public void completeReviewTask(String taskId, String decisionType, Map<String, Object> variables) {
        variables.put("decisionType", decisionType);
        taskService.complete(taskId, variables);

        log.info("[Workflow Task Completed] taskId={}, decisionType={}", taskId, decisionType);
    }
}
```

---

## 6. 監控指標

### 6.1 關鍵指標定義

**審核效率指標**:

| 指標名稱 | 類型 | 說明 | 告警閾值 |
|---------|------|------|---------|
| `risk_review_duration_seconds` | Histogram | 審核響應時間（秒） | P95 >3600s (1小時) |
| `risk_review_approval_rate` | Gauge | 提案批准率 (%) | <70% 或 >95% |
| `risk_review_sla_violation_count` | Counter | SLA 違規次數 | >10/day |
| `risk_review_pending_count` | Gauge | 待審核提案數量 | >100 |

**補償執行指標**:

| 指標名稱 | 類型 | 說明 | 告警閾值 |
|---------|------|------|---------|
| `risk_compensation_execution_errors` | Counter | 補償執行失敗次數 | >5/hour |
| `risk_compensation_retry_count` | Counter | 補償重試次數 | >20/day |
| `risk_compensation_amount_total` | Counter | 累計補償金額 ($) | - |

### 6.2 Grafana 儀表板

**審核效率儀表板 JSON**:

```json
{
  "dashboard": {
    "title": "風控提案審核效率監控",
    "panels": [
      {
        "id": 1,
        "title": "審核響應時間 (P50/P95/P99)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.50, rate(risk_review_duration_seconds_bucket[5m]))",
            "legendFormat": "P50"
          },
          {
            "expr": "histogram_quantile(0.95, rate(risk_review_duration_seconds_bucket[5m]))",
            "legendFormat": "P95"
          },
          {
            "expr": "histogram_quantile(0.99, rate(risk_review_duration_seconds_bucket[5m]))",
            "legendFormat": "P99"
          }
        ]
      },
      {
        "id": 2,
        "title": "提案批准率",
        "type": "gauge",
        "targets": [
          {
            "expr": "risk_review_approval_rate * 100",
            "legendFormat": "批准率 (%)"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {"value": 0, "color": "red"},
                {"value": 70, "color": "yellow"},
                {"value": 80, "color": "green"},
                {"value": 95, "color": "orange"}
              ]
            }
          }
        }
      },
      {
        "id": 3,
        "title": "待審核提案數量",
        "type": "stat",
        "targets": [
          {
            "expr": "risk_review_pending_count",
            "legendFormat": "待審核"
          }
        ]
      },
      {
        "id": 4,
        "title": "SLA 違規趨勢",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(risk_review_sla_violation_count[1h])",
            "legendFormat": "SLA 違規率 (per hour)"
          }
        ]
      }
    ]
  }
}
```

### 6.3 告警規則

**Prometheus AlertManager 配置**:

```yaml
groups:
  - name: risk_review_alerts
    interval: 30s
    rules:
      # 1. 審核響應時間過長
      - alert: RiskReviewLatencyHigh
        expr: histogram_quantile(0.95, rate(risk_review_duration_seconds_bucket[5m])) > 3600
        for: 10m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "風控審核 P95 響應時間過長"
          description: "P95 響應時間: {{ $value }}s, 超過 1 小時"

      # 2. 待審核提案積壓
      - alert: RiskReviewBacklogHigh
        expr: risk_review_pending_count > 100
        for: 30m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "風控審核積壓過多"
          description: "待審核提案: {{ $value }}, 超過閾值 100"

      # 3. SLA 違規
      - alert: RiskReviewSLAViolation
        expr: increase(risk_review_sla_violation_count[1d]) > 10
        labels:
          severity: critical
          team: risk
        annotations:
          summary: "風控審核 SLA 違規次數過多"
          description: "今日 SLA 違規次數: {{ $value }}, 超過 10 次"

      # 4. 補償執行失敗
      - alert: RiskCompensationExecutionErrors
        expr: rate(risk_compensation_execution_errors[1h]) > 5
        for: 5m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "風控補償執行失敗率過高"
          description: "失敗率: {{ $value }} per hour"
```

---

## 7. 變更日誌 (Change Log)

### v1.0.0 (2026-02-02) - v2.1.0 簡化版

**初始版本**:
1. ✅ 審核隊列管理（優先級算法 + 統一 SLA 48 小時）
2. ✅ 審核決策矩陣（批准/拒絕/部分批准/升級）
3. ✅ 補償機制（退款/調帳/扣款 + 重試機制）
4. ✅ 審核員權限管理（三級權限，無自動路由）
5. ✅ Camunda BPMN 工作流集成
6. ✅ 監控指標（Grafana + Prometheus）

**v2.1.0 簡化變更**:
- ❌ 取消複雜閾值判斷（$500/$5000 自動路由）
- ❌ 取消 L1/L2 強制路由分層
- ✅ 統一 SLA 48 小時（所有提案平等對待）
- ✅ 審核員手動升級（無自動分配）
- ✅ 人工審核為主（所有可疑金額 > 0 都生成提案）

---

**文檔版本**: 1.0.0 (v2.1.0 簡化版)
**最後更新**: 2026-02-02
**維護團隊**: Risk Team & Compliance Team
**相關文檔**:
- [05-03 配置驅動風控系統](./05-03_Passive_Risk_Control_System.md)
- [05-01 風控系統架構](./05-01_Risk_Control_System.md)
- [02-01 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md)
