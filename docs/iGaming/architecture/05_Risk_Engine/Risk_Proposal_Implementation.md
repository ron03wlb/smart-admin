# Risk Proposal Workflow Implementation

> **Canonical Source**: [source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md)
> **Audience**: Architects, Backend Developers, DevOps Engineers
> **Business Requirements**: [Risk_Proposal_Requirements.md](../../requirements/05_Risk_Compliance/Risk_Proposal_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. Document Information

**Version**: 4.0.0 (v2.1.0 Simplified)
**Dependencies**:
- [05-03 KYC/AML Configuration-Driven Risk](../05_Risk_Engine/) - Risk proposal service implementation
- [05-01 Risk Framework](../05_Risk_Engine/) - Configuration-driven rule engine
- [01-05 Withdrawal Risk](../../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md) - SAGA Step 2.5 delayed check

---

## 2. Review Queue Management

### 2.1 Priority Calculation Service

```java
/**
 * Service Layer - Proposal priority calculation (v2.1.0 simplified)
 */
public class RiskProposalPriorityService {

    /**
     * Calculate proposal priority
     * Rules (v2.1.0 simplified):
     * - URGENT: amount > $10,000
     * - HIGH:   amount > $5,000
     * - MEDIUM: amount > $1,000
     * - LOW:    amount <= $1,000
     *
     * @param suspiciousAmount Suspicious amount
     * @return String Priority level
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
     * Dynamic priority upgrade based on wait time
     * v2.1.0: Upgrades priority only when SLA is exceeded
     */
    @Scheduled(fixedDelay = 3600000)  // Execute every hour
    public void updatePriorityBasedOnWaitTime() {
        LocalDateTime slaThreshold = LocalDateTime.now().minusHours(48);

        // Query overdue pending proposals
        List<RiskProposalEntity> overdueProposals = riskProposalDao.selectList(
            new LambdaQueryWrapper<RiskProposalEntity>()
                .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                .lt(RiskProposalEntity::getCreatedAt, slaThreshold)
                .eq(RiskProposalEntity::getDeleted, false)
        );

        // Upgrade priority
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

### 2.2 SLA Monitoring and Timeout Processing (v3.0.0)

```java
/**
 * Service Layer - SLA monitoring and timeout processing (v3.0.0 priority-based)
 */
@Service
@RequiredArgsConstructor
public class RiskProposalSLAService {

    private final RiskProposalDao riskProposalDao;
    private final NotificationService notificationService;
    private final WithdrawalService withdrawalService;

    /**
     * Per-priority SLA configuration (unit: hours)
     */
    private static final Map<String, Integer> PRIORITY_SLA_HOURS = Map.of(
        "URGENT", 1,
        "HIGH", 2,
        "MEDIUM", 24,
        "LOW", 48
    );

    /**
     * Process expired proposals (scheduled task, every 5 minutes)
     * URGENT/HIGH/MEDIUM timeout -> auto-reject withdrawal
     * LOW timeout -> auto-approve
     */
    @Scheduled(fixedDelay = 300_000)
    public void processExpiredProposals() {
        for (Map.Entry<String, Integer> entry : PRIORITY_SLA_HOURS.entrySet()) {
            String priority = entry.getKey();
            int slaHours = entry.getValue();
            LocalDateTime slaThreshold = LocalDateTime.now().minusHours(slaHours);

            List<RiskProposalEntity> expired = riskProposalDao.selectList(
                new LambdaQueryWrapper<RiskProposalEntity>()
                    .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                    .eq(RiskProposalEntity::getPriority, priority)
                    .lt(RiskProposalEntity::getCreatedAt, slaThreshold)
                    .eq(RiskProposalEntity::getDeleted, false)
            );

            for (RiskProposalEntity p : expired) {
                if ("LOW".equals(p.getPriority())) {
                    // LOW: auto-approve on timeout
                    withdrawalService.approveWithdrawal(p.getWithdrawalRequestId());
                    p.setStatus("APPROVED");
                    log.info("[SLA Auto-Approve] Proposal {} (LOW) auto-approved after {} hours",
                        p.getId(), slaHours);
                } else {
                    // URGENT/HIGH/MEDIUM: auto-reject on timeout
                    withdrawalService.rejectWithdrawal(p.getWithdrawalRequestId());
                    p.setStatus("REJECTED");
                    log.warn("[SLA Auto-Reject] Proposal {} ({}) auto-rejected after {} hours",
                        p.getId(), p.getPriority(), slaHours);
                }
                riskProposalDao.updateById(p);
            }
        }
    }

    /**
     * Calculate remaining time for a proposal (based on priority SLA)
     */
    public Duration calculateRemainingTime(Long proposalId) {
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        if (proposal == null) {
            return Duration.ZERO;
        }

        int slaHours = PRIORITY_SLA_HOURS.getOrDefault(proposal.getPriority(), 48);
        LocalDateTime slaDeadline = proposal.getCreatedAt().plusHours(slaHours);
        return Duration.between(LocalDateTime.now(), slaDeadline);
    }

    /**
     * Find proposals approaching SLA deadline (remaining time < warning threshold)
     */
    public List<RiskProposalVO> findApproachingSLA() {
        LocalDateTime now = LocalDateTime.now();
        List<RiskProposalEntity> results = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : PRIORITY_SLA_HOURS.entrySet()) {
            String priority = entry.getKey();
            int slaHours = entry.getValue();
            int warningHours = Math.max(1, slaHours / 4);  // Warning threshold = SLA / 4

            LocalDateTime warningThreshold = now.minusHours(slaHours - warningHours);

            results.addAll(riskProposalDao.selectList(
                new LambdaQueryWrapper<RiskProposalEntity>()
                    .eq(RiskProposalEntity::getStatus, "PENDING_REVIEW")
                    .eq(RiskProposalEntity::getPriority, priority)
                    .lt(RiskProposalEntity::getCreatedAt, warningThreshold)
                    .ge(RiskProposalEntity::getCreatedAt, now.minusHours(slaHours))
                    .eq(RiskProposalEntity::getDeleted, false)
            ));
        }

        return results.stream()
            .sorted(Comparator.comparing(RiskProposalEntity::getCreatedAt))
            .map(entity -> SmartBeanUtil.copy(entity, RiskProposalVO.class))
            .collect(Collectors.toList());
    }
}
```

### 2.3 Review Queue Controller

```java
/**
 * Controller Layer - Review queue queries
 */
@Tag(name = "Risk Proposal Review Queue")
@RestController
@RequestMapping("/risk/proposal/queue")
@RequiredArgsConstructor
public class RiskProposalQueueController {

    private final RiskProposalService riskProposalService;
    private final RiskProposalSLAService slaService;

    /**
     * Query review queue (sorted by priority)
     *
     * @param queryForm Query form
     * @return ResponseDTO<PageResult<RiskProposalVO>> Paginated result
     */
    @Operation(summary = "Query review queue")
    @PostMapping("/query")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<PageResult<RiskProposalVO>> queryReviewQueue(
        @Valid @RequestBody RiskProposalQueryForm queryForm
    ) {
        // Default sort: priority DESC, created_at ASC
        queryForm.setOrderBy("priority DESC, created_at ASC");

        return riskProposalService.queryProposals(queryForm)
            .map(ResponseDTO::ok)
            .getOrElse(ResponseDTO.error("Query failed"));
    }

    /**
     * Query proposals approaching SLA deadline
     *
     * @return ResponseDTO<List<RiskProposalVO>> Proposal list
     */
    @Operation(summary = "Query proposals approaching SLA")
    @GetMapping("/approaching-sla")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<List<RiskProposalVO>> queryApproachingSLA() {
        List<RiskProposalVO> proposals = slaService.findApproachingSLA();
        return ResponseDTO.ok(proposals);
    }

    /**
     * Claim review task (reviewer actively claims)
     *
     * @param proposalId Proposal ID
     * @return ResponseDTO<Void> Operation result
     */
    @Operation(summary = "Claim review task")
    @PostMapping("/{proposalId}/claim")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> claimReviewTask(@PathVariable Long proposalId) {
        Long reviewerId = StpUtil.getLoginIdAsLong();

        boolean success = riskProposalService.claimReviewTask(proposalId, reviewerId)
            .getOrElse(false);

        return success ? ResponseDTO.ok() : ResponseDTO.error("Claim failed");
    }
}
```

---

## 3. Review Decision Implementation

### 3.1 Approve Flow

**Service Layer**:

```java
/**
 * Service Layer - Approve proposal
 */
public class RiskProposalService {

    /**
     * Approve risk proposal (full release)
     *
     * @param proposalId Proposal ID
     * @param reviewerId Reviewer ID
     * @param reviewNotes Review notes
     * @return Option<Boolean> Success flag
     */
    public Option<Boolean> approveProposal(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // Validate proposal status
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    log.error("[Approve Failed] Proposal {} not in PENDING_REVIEW status", proposalId);
                    return Option.none();
                }

                // Delegate to Manager layer for transaction
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

**Manager Layer (transactional)**:

```java
/**
 * Manager Layer - Approve proposal transaction
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    private final RiskProposalDao riskProposalDao;
    private final WithdrawalService withdrawalService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Approve proposal (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean approveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 1. Update proposal status
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("APPROVED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(proposal.getSuspiciousAmount());  // Full approval

        riskProposalDao.updateById(proposal);

        // 2. Unfreeze amount, continue withdrawal flow
        if (proposal.getWithdrawalRequestId() != null) {
            withdrawalService.unfreezeAmount(
                proposal.getWithdrawalRequestId(),
                proposal.getSuspiciousAmount()
            );
        }

        // 3. Publish Kafka event
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

### 3.2 Reject Flow

**Service Layer**:

```java
/**
 * Service Layer - Reject proposal
 */
public class RiskProposalService {

    /**
     * Reject risk proposal
     *
     * @param proposalId Proposal ID
     * @param reviewerId Reviewer ID
     * @param reviewNotes Review notes (REQUIRED)
     * @return Option<Boolean> Success flag
     */
    public Option<Boolean> rejectProposal(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // Validate review notes (rejection must provide reason)
        if (StringUtils.isBlank(reviewNotes)) {
            log.error("[Reject Failed] Review notes required for rejection");
            return Option.none();
        }

        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    return Option.none();
                }

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

**Manager Layer (transactional)**:

```java
/**
 * Manager Layer - Reject proposal transaction
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * Reject proposal (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean rejectProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String reviewNotes
    ) {
        // 1. Update proposal status
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("REJECTED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(BigDecimal.ZERO);  // Rejected, approved = 0

        riskProposalDao.updateById(proposal);

        // 2. Suspicious amount stays frozen, trigger deduction compensation
        if (proposal.getWithdrawalRequestId() != null) {
            compensationService.executeDeduction(
                proposal.getPlayerId(),
                proposal.getSuspiciousAmount(),
                String.format("Risk proposal rejected: %s (Proposal: %s)",
                    reviewNotes, proposal.getProposalNo())
            );
        }

        // 3. Publish Kafka event
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

### 3.3 Partial Approve Flow

**Service Layer**:

```java
/**
 * Service Layer - Partial approve proposal
 */
public class RiskProposalService {

    /**
     * Partially approve risk proposal
     *
     * @param proposalId Proposal ID
     * @param reviewerId Reviewer ID
     * @param approvedAmount Approved amount
     * @param reviewNotes Review notes (REQUIRED)
     * @return Option<Boolean> Success flag
     */
    public Option<Boolean> partialApproveProposal(
        Long proposalId,
        Long reviewerId,
        BigDecimal approvedAmount,
        String reviewNotes
    ) {
        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                // Validate approved amount
                if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                    approvedAmount.compareTo(proposal.getSuspiciousAmount()) >= 0) {
                    log.error("[Partial Approve Failed] Invalid approved amount: {}", approvedAmount);
                    return Option.none();
                }

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

**Manager Layer (transactional)**:

```java
/**
 * Manager Layer - Partial approve proposal transaction
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * Partial approve proposal (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean partialApproveProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        BigDecimal approvedAmount,
        String reviewNotes
    ) {
        // 1. Update proposal status
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        BigDecimal rejectedAmount = proposal.getSuspiciousAmount().subtract(approvedAmount);

        proposal.setStatus("PARTIAL_APPROVED");
        proposal.setReviewerId(reviewerId);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNotes(reviewNotes);
        proposal.setApprovedAmount(approvedAmount);

        riskProposalDao.updateById(proposal);

        // 2. Unfreeze approved amount, deduct rejected amount
        if (proposal.getWithdrawalRequestId() != null) {
            withdrawalService.unfreezeAmount(proposal.getWithdrawalRequestId(), approvedAmount);

            // Deduct rejected portion
            compensationService.executeDeduction(
                proposal.getPlayerId(),
                rejectedAmount,
                String.format("Partial approve: approved $%s, rejected $%s (Proposal: %s)",
                    approvedAmount, rejectedAmount, proposal.getProposalNo())
            );
        }

        // 3. Publish Kafka event
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

### 3.4 Escalate Flow

**Service Layer**:

```java
/**
 * Service Layer - Escalate proposal to senior review
 */
public class RiskProposalService {

    /**
     * Escalate proposal to senior analyst (v2.1.0 manual escalation)
     *
     * @param proposalId Proposal ID
     * @param reviewerId Current reviewer ID
     * @param escalationReason Escalation reason (REQUIRED)
     * @return Option<Boolean> Success flag
     */
    public Option<Boolean> escalateProposal(
        Long proposalId,
        Long reviewerId,
        String escalationReason
    ) {
        if (StringUtils.isBlank(escalationReason)) {
            log.error("[Escalate Failed] Escalation reason required");
            return Option.none();
        }

        return Option.of(riskProposalDao.selectById(proposalId))
            .flatMap(proposal -> {
                if (!"PENDING_REVIEW".equals(proposal.getStatus())) {
                    return Option.none();
                }

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

**Manager Layer (transactional)**:

```java
/**
 * Manager Layer - Escalate proposal transaction
 */
@Service
@RequiredArgsConstructor
public class RiskProposalManager {

    /**
     * Escalate proposal to senior review (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean escalateProposalWithTransaction(
        Long proposalId,
        Long reviewerId,
        String escalationReason
    ) {
        // 1. Update proposal status
        RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
        proposal.setStatus("ESCALATED");
        proposal.setPriority("URGENT");  // Escalated -> URGENT priority
        proposal.setReviewNotes(String.format("[Escalated by Reviewer %d] %s",
            reviewerId, escalationReason));

        riskProposalDao.updateById(proposal);

        // 2. Notify senior analysts
        notificationService.sendEscalationNotification(
            proposal,
            reviewerId,
            escalationReason
        );

        // 3. Publish Kafka event
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

## 4. Compensation Mechanism

### 4.1 Compensation Service

```java
/**
 * Service Layer - Compensation service
 */
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final CompensationManager compensationManager;
    private final WalletService walletService;

    /**
     * Execute refund compensation
     *
     * @param playerId Player ID
     * @param amount Refund amount
     * @param reason Refund reason
     * @return Option<String> Compensation record ID
     */
    public Option<String> executeRefund(Long playerId, BigDecimal amount, String reason) {
        return walletService.getPlayerWallet(playerId)
            .flatMap(wallet -> {
                String compensationId = compensationManager.executeRefundWithTransaction(
                    playerId,
                    amount,
                    reason
                );

                return Option.of(compensationId);
            });
    }

    /**
     * Execute deduction compensation
     *
     * @param playerId Player ID
     * @param amount Deduction amount
     * @param reason Deduction reason
     * @return Option<String> Compensation record ID
     */
    public Option<String> executeDeduction(Long playerId, BigDecimal amount, String reason) {
        return walletService.getPlayerWallet(playerId)
            .flatMap(wallet -> {
                if (wallet.getBalance().compareTo(amount) < 0) {
                    log.error("[Deduction Failed] Insufficient balance for player {}", playerId);
                    return Option.none();
                }

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

### 4.2 Compensation Manager (Transactional)

```java
/**
 * Manager Layer - Compensation transaction processing
 */
@Service
@RequiredArgsConstructor
public class CompensationManager {

    private final CompensationDao compensationDao;
    private final WalletDao walletDao;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Execute refund compensation (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public String executeRefundWithTransaction(Long playerId, BigDecimal amount, String reason) {
        // 1. Create compensation record
        CompensationEntity compensation = CompensationEntity.builder()
            .compensationNo(generateCompensationNo())
            .playerId(playerId)
            .compensationType("REFUND")
            .amount(amount)
            .reason(reason)
            .status("PENDING")
            .build();

        compensationDao.insert(compensation);

        // 2. Update player wallet (add balance)
        WalletEntity wallet = walletDao.selectByPlayerId(playerId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletDao.updateById(wallet);

        // 3. Update compensation status
        compensation.setStatus("COMPLETED");
        compensation.setExecutedAt(LocalDateTime.now());
        compensationDao.updateById(compensation);

        // 4. Publish Kafka event
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
     * Execute deduction compensation (with transaction)
     */
    @Transactional(rollbackFor = Throwable.class)
    public String executeDeductionWithTransaction(Long playerId, BigDecimal amount, String reason) {
        // 1. Create compensation record
        CompensationEntity compensation = CompensationEntity.builder()
            .compensationNo(generateCompensationNo())
            .playerId(playerId)
            .compensationType("DEDUCTION")
            .amount(amount)
            .reason(reason)
            .status("PENDING")
            .build();

        compensationDao.insert(compensation);

        // 2. Update player wallet (subtract balance)
        WalletEntity wallet = walletDao.selectByPlayerId(playerId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for deduction");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletDao.updateById(wallet);

        // 3. Update compensation status
        compensation.setStatus("COMPLETED");
        compensation.setExecutedAt(LocalDateTime.now());
        compensationDao.updateById(compensation);

        // 4. Publish Kafka event
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

### 4.3 Compensation Retry Service

```java
/**
 * Service Layer - Compensation retry with exponential backoff
 */
@Service
@RequiredArgsConstructor
public class CompensationRetryService {

    private final CompensationDao compensationDao;
    private final CompensationService compensationService;

    /**
     * Retry failed compensations (scheduled task, every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000)
    public void retryFailedCompensations() {
        List<CompensationEntity> failedCompensations = compensationDao.selectList(
            new LambdaQueryWrapper<CompensationEntity>()
                .eq(CompensationEntity::getStatus, "FAILED")
                .lt(CompensationEntity::getRetryCount, 3)  // Max 3 retries
                .eq(CompensationEntity::getDeleted, false)
        );

        failedCompensations.forEach(compensation -> {
            try {
                // Exponential backoff delay
                long delayMs = (long) Math.pow(2, compensation.getRetryCount()) * 1000;
                Thread.sleep(delayMs);

                // Retry compensation
                retryCompensation(compensation);

            } catch (Exception e) {
                log.error("[Retry Failed] compensationId={}, error={}",
                    compensation.getId(), e.getMessage());
            }
        });
    }

    private void retryCompensation(CompensationEntity compensation) {
        try {
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

            compensation.setRetryCount(compensation.getRetryCount() + 1);
            compensation.setStatus("COMPLETED");
            compensationDao.updateById(compensation);

            log.info("[Retry Success] compensationId={}", compensation.getId());

        } catch (Exception e) {
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

## 5. Reviewer API and Permission Configuration

### 5.1 Permission Annotations (Sa-Token)

```java
/**
 * Controller Layer - Permission configuration
 */
@RestController
@RequestMapping("/risk/proposal")
public class RiskProposalController {

    /**
     * Reviewer permission: Query review queue
     */
    @PostMapping("/queue/query")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<PageResult<RiskProposalVO>> queryReviewQueue(
        @RequestBody RiskProposalQueryForm queryForm
    ) {
        // ...
    }

    /**
     * Reviewer permission: Approve proposal
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
     * Senior analyst permission: Handle escalated cases
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
     * Compliance manager permission: Configure risk rules
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

### 5.2 Unified Review Decision Controller

```java
/**
 * Controller Layer - Reviewer operation APIs
 */
@Tag(name = "Risk Proposal Review Operations")
@RestController
@RequestMapping("/risk/proposal/review")
@RequiredArgsConstructor
public class RiskProposalReviewController {

    private final RiskProposalService riskProposalService;

    /**
     * Claim review task (reviewer actively claims)
     */
    @Operation(summary = "Claim review task")
    @PostMapping("/{proposalId}/claim")
    @SaCheckPermission("risk:proposal:review")
    public ResponseDTO<Void> claimTask(@PathVariable Long proposalId) {
        Long reviewerId = StpUtil.getLoginIdAsLong();

        boolean success = riskProposalService.claimReviewTask(proposalId, reviewerId)
            .getOrElse(false);

        return success ? ResponseDTO.ok() : ResponseDTO.error("Claim failed");
    }

    /**
     * Submit review decision (unified entry point)
     */
    @Operation(summary = "Submit review decision")
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

        return success ? ResponseDTO.ok() : ResponseDTO.error("Decision submission failed");
    }
}
```

---

## 6. Camunda BPMN Workflow Integration

### 6.1 BPMN Process Definition

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn">

  <bpmn:process id="RiskProposalReviewProcess" name="Risk Proposal Review Process" isExecutable="true">

    <!-- Start Event -->
    <bpmn:startEvent id="StartEvent" name="Risk Proposal Created">
      <bpmn:outgoing>Flow1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- User Task: Reviewer Review -->
    <bpmn:userTask id="ReviewerTask" name="Reviewer Review"
                   camunda:assignee="${reviewerId}"
                   camunda:candidateGroups="risk_reviewers">
      <bpmn:incoming>Flow1</bpmn:incoming>
      <bpmn:outgoing>Flow2</bpmn:outgoing>
    </bpmn:userTask>

    <!-- Exclusive Gateway: Decision Routing -->
    <bpmn:exclusiveGateway id="DecisionGateway" name="Review Decision">
      <bpmn:incoming>Flow2</bpmn:incoming>
      <bpmn:outgoing>FlowApprove</bpmn:outgoing>
      <bpmn:outgoing>FlowReject</bpmn:outgoing>
      <bpmn:outgoing>FlowPartialApprove</bpmn:outgoing>
      <bpmn:outgoing>FlowEscalate</bpmn:outgoing>
    </bpmn:exclusiveGateway>

    <!-- Service Task: Approve Compensation -->
    <bpmn:serviceTask id="ApproveCompensationTask" name="Execute Approve Compensation"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowApprove</bpmn:incoming>
      <bpmn:outgoing>FlowEnd1</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- Service Task: Reject Compensation -->
    <bpmn:serviceTask id="RejectCompensationTask" name="Execute Reject Compensation"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowReject</bpmn:incoming>
      <bpmn:outgoing>FlowEnd2</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- Service Task: Partial Approve Compensation -->
    <bpmn:serviceTask id="PartialApproveCompensationTask" name="Execute Partial Approve Compensation"
                      camunda:delegateExpression="${compensationService}">
      <bpmn:incoming>FlowPartialApprove</bpmn:incoming>
      <bpmn:outgoing>FlowEnd3</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- User Task: Senior Analyst Review -->
    <bpmn:userTask id="SeniorAnalystTask" name="Senior Analyst Review"
                   camunda:candidateGroups="senior_analysts">
      <bpmn:incoming>FlowEscalate</bpmn:incoming>
      <bpmn:outgoing>Flow2</bpmn:outgoing>  <!-- Back to decision gateway -->
    </bpmn:userTask>

    <!-- End Event -->
    <bpmn:endEvent id="EndEvent" name="Review Complete">
      <bpmn:incoming>FlowEnd1</bpmn:incoming>
      <bpmn:incoming>FlowEnd2</bpmn:incoming>
      <bpmn:incoming>FlowEnd3</bpmn:incoming>
    </bpmn:endEvent>

    <!-- Sequence Flows -->
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

### 6.2 Workflow Service

```java
/**
 * Service Layer - Camunda workflow integration
 */
@Service
@RequiredArgsConstructor
public class RiskProposalWorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * Start risk proposal review process
     *
     * @param proposalId Proposal ID
     * @return String Process instance ID
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
     * Complete review task (submit decision)
     *
     * @param taskId Camunda task ID
     * @param decisionType Decision type
     * @param variables Process variables
     */
    public void completeReviewTask(String taskId, String decisionType, Map<String, Object> variables) {
        variables.put("decisionType", decisionType);
        taskService.complete(taskId, variables);

        log.info("[Workflow Task Completed] taskId={}, decisionType={}", taskId, decisionType);
    }
}
```

---

## 7. Monitoring and Alerting

### 7.1 Prometheus Metrics

**Review efficiency metrics**:

| Metric | Type | Description | Alert Threshold |
|--------|------|-------------|-----------------|
| `risk_review_duration_seconds` | Histogram | Review response time (seconds) | P95 > 3600s (1 hour) |
| `risk_review_approval_rate` | Gauge | Proposal approval rate (%) | < 70% or > 95% |
| `risk_review_sla_violation_count` | Counter | SLA violation count | > 10/day |
| `risk_review_pending_count` | Gauge | Pending proposal count | > 100 |

**Compensation metrics**:

| Metric | Type | Description | Alert Threshold |
|--------|------|-------------|-----------------|
| `risk_compensation_execution_errors` | Counter | Compensation execution failures | > 5/hour |
| `risk_compensation_retry_count` | Counter | Compensation retry count | > 20/day |
| `risk_compensation_amount_total` | Counter | Total compensation amount ($) | - |

### 7.2 Grafana Dashboard Configuration

```json
{
  "dashboard": {
    "title": "Risk Proposal Review Efficiency Monitor",
    "panels": [
      {
        "id": 1,
        "title": "Review Response Time (P50/P95/P99)",
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
        "title": "Proposal Approval Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "risk_review_approval_rate * 100",
            "legendFormat": "Approval Rate (%)"
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
        "title": "Pending Proposal Count",
        "type": "stat",
        "targets": [
          {
            "expr": "risk_review_pending_count",
            "legendFormat": "Pending"
          }
        ]
      },
      {
        "id": 4,
        "title": "SLA Violation Trend",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(risk_review_sla_violation_count[1h])",
            "legendFormat": "SLA Violations (per hour)"
          }
        ]
      }
    ]
  }
}
```

### 7.3 Prometheus AlertManager Rules

```yaml
groups:
  - name: risk_review_alerts
    interval: 30s
    rules:
      # 1. Review latency too high
      - alert: RiskReviewLatencyHigh
        expr: histogram_quantile(0.95, rate(risk_review_duration_seconds_bucket[5m])) > 3600
        for: 10m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "Risk review P95 response time too high"
          description: "P95 response time: {{ $value }}s, exceeds 1 hour"

      # 2. Pending proposal backlog
      - alert: RiskReviewBacklogHigh
        expr: risk_review_pending_count > 100
        for: 30m
        labels:
          severity: warning
          team: risk
        annotations:
          summary: "Risk review backlog too high"
          description: "Pending proposals: {{ $value }}, exceeds threshold 100"

      # 3. SLA violations
      - alert: RiskReviewSLAViolation
        expr: increase(risk_review_sla_violation_count[1d]) > 10
        labels:
          severity: critical
          team: risk
        annotations:
          summary: "Risk review SLA violations too high"
          description: "Today's SLA violations: {{ $value }}, exceeds 10"

      # 4. Compensation execution failures
      - alert: RiskCompensationExecutionErrors
        expr: rate(risk_compensation_execution_errors[1h]) > 5
        for: 5m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "Risk compensation execution failure rate too high"
          description: "Failure rate: {{ $value }} per hour"
```

---

## 8. Architecture Patterns Summary

### 8.1 SmartAdmin Layered Architecture Compliance

| Layer | Responsibility | Key Pattern |
|-------|---------------|-------------|
| **Controller** | API routing, permission check, parameter validation | `@SaCheckPermission`, `ResponseDTO.ok()` |
| **Service** | Business logic, Vavr `Option` for null safety | `Option.of(...).flatMap(...)` |
| **Manager** | Transaction management, cross-service coordination | `@Transactional(rollbackFor = Throwable.class)` |
| **Dao** | Data access via MyBatis Plus | `LambdaQueryWrapper`, `selectList`, `updateById` |

### 8.2 Event-Driven Architecture

All state changes publish Kafka events for downstream consumers:
- `risk.proposal.approved` - Proposal approved
- `risk.proposal.rejected` - Proposal rejected
- `risk.proposal.partial_approved` - Proposal partially approved
- `risk.proposal.escalated` - Proposal escalated
- `wallet.compensation.refund` - Refund executed
- `wallet.compensation.deduction` - Deduction executed

### 8.3 Key Design Decisions

- **Service uses Vavr Option** (NOT java.util.Optional) per ArchUnit enforcement
- **@Transactional ONLY in Manager layer** (NEVER in Service/Controller)
- **Constructor injection** via `@RequiredArgsConstructor` + `private final`
- **Boolean field naming**: `deleted` (NOT `isDeleted`)
