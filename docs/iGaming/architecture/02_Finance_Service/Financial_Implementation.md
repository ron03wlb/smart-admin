# Financial Implementation Architecture

> **Canonical Source**: [source-archive/00_Foundation/guides/00-11_Financial_Implementation.md](../../source-archive/00_Foundation/guides/00-11_Financial_Implementation.md)
> **Audience**: Architects, Backend Developers, System Integration Engineers
> **Business Requirements**: [Financial_Implementation_Requirements.md](../../requirements/02_Financial_Operations/Financial_Implementation_Requirements.md)
> **Last Synced**: 2026-02-09
>
> **Technical Focus**: This document contains implementation details (atomicity, idempotency, HMAC-SHA256 algorithms, SAGA compensation flows) extracted from Requirements layer.

---

## 1. Document Purpose

This document provides the **technical architecture and implementation guide** for the iGaming platform's core financial processes, including wallet system, payment gateway integration, withdrawal risk control, and reconciliation system.

**Target Audience**:
- Backend Developers (financial module)
- Full-Stack Developers
- System Integration Engineers

**Implementation Sequence**:
1. Wallet System -> Payment Gateway -> Withdrawal Risk Control -> Reconciliation System
2. Each task includes: reading order, implementation steps, verification checklist, common pitfalls
3. Follow SmartAdmin architecture patterns (Entity, Manager, Service)

---

## 2. Wallet System Implementation

### 2.1 Database Schema

```sql
-- Wallet master table
CREATE TABLE wallet (
    wallet_id BIGINT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    wallet_type VARCHAR(20) NOT NULL, -- CASH, BONUS, LOCKED
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    locked_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    version INT NOT NULL DEFAULT 0, -- Optimistic lock
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(player_id, wallet_type),
    INDEX idx_player_tenant (player_id, tenant_id)
);

-- Wallet transaction records
CREATE TABLE wallet_transaction (
    transaction_id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAW, BET, WIN
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    request_id VARCHAR(64) UNIQUE NOT NULL, -- Idempotency
    created_at TIMESTAMP NOT NULL,
    INDEX idx_wallet_time (wallet_id, created_at)
);

-- Wallet lock records
CREATE TABLE wallet_lock (
    lock_id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    lock_amount DECIMAL(19,4) NOT NULL,
    lock_reason VARCHAR(50) NOT NULL, -- BET_PENDING, WITHDRAWAL_PENDING
    reference_id VARCHAR(64) NOT NULL, -- Associated bet/withdrawal order
    created_at TIMESTAMP NOT NULL,
    INDEX idx_wallet_ref (wallet_id, reference_id)
);
```

### 2.2 Available Balance Calculation

```java
/**
 * Calculate available (bettable) balance
 *
 * Formula: Available Balance = Cash Wallet Balance - Locked Amount - Pending Bets
 *
 * @param playerId Player ID
 * @param tenantId Tenant ID
 * @return Available balance
 */
public BigDecimal calculateAvailableBalance(Long playerId, String tenantId) {
    // 1. Query cash wallet
    Wallet cashWallet = walletDao.findByPlayerAndType(
        playerId,
        WalletType.CASH
    );

    if (cashWallet == null) {
        return BigDecimal.ZERO;
    }

    // 2. Apply formula
    BigDecimal availableBalance = cashWallet.getBalance()
        .subtract(cashWallet.getLockedAmount())
        .subtract(calculatePendingBets(playerId));

    // 3. Floor at zero
    return availableBalance.max(BigDecimal.ZERO);
}

/**
 * Calculate pending bet amount
 */
private BigDecimal calculatePendingBets(Long playerId) {
    return betDao.sumPendingBetAmount(playerId);
}
```

### 2.3 Concurrent-Safe Balance Deduction (Redis Lua)

```java
/**
 * Debit wallet balance (atomic operation)
 *
 * Uses Redis Lua script for atomicity
 *
 * @param request Debit request
 * @return Debit result
 */
public DebitResult debitWallet(DebitRequest request) {
    String requestId = request.getRequestId();
    Long walletId = request.getWalletId();
    BigDecimal amount = request.getAmount();

    // 1. Idempotency check (Redis fast path)
    String cacheKey = "wallet:debit:" + requestId;
    DebitResult cachedResult = redisTemplate.opsForValue().get(cacheKey);
    if (cachedResult != null) {
        log.info("Request {} already processed (cached)", requestId);
        return cachedResult;
    }

    // 2. Execute Lua script for deduction
    String luaScript = """
        local wallet_key = KEYS[1]
        local amount = tonumber(ARGV[1])

        -- Check balance
        local balance = tonumber(redis.call('HGET', wallet_key, 'balance'))
        if balance < amount then
            return 'INSUFFICIENT_BALANCE'
        end

        -- Atomic deduction
        redis.call('HINCRBYFLOAT', wallet_key, 'balance', -amount)
        return 'SUCCESS'
        """;

    String walletKey = "wallet:" + walletId;
    String result = redisTemplate.execute(
        RedisScript.of(luaScript, String.class),
        Collections.singletonList(walletKey),
        amount.toString()
    );

    if (!"SUCCESS".equals(result)) {
        throw new InsufficientBalanceException("Insufficient balance");
    }

    // 3. Persist to database (async)
    CompletableFuture.runAsync(() -> {
        persistWalletTransaction(request);
    }, asyncExecutor);

    // 4. Cache result (15 minutes)
    DebitResult debitResult = new DebitResult(requestId, walletId, amount);
    redisTemplate.opsForValue().set(cacheKey, debitResult, 15, TimeUnit.MINUTES);

    return debitResult;
}
```

### 2.4 Wallet Lock/Unlock Mechanism

```java
/**
 * Lock wallet amount (used during bet placement)
 *
 * @param walletId Wallet ID
 * @param amount Lock amount
 * @param betId Bet order ID
 */
@Transactional(rollbackFor = Throwable.class)
public void lockWalletAmount(Long walletId, BigDecimal amount, String betId) {
    // 1. Optimistic lock update
    int updated = walletDao.incrementLockedAmount(walletId, amount);
    if (updated == 0) {
        throw new ConcurrentUpdateException("Wallet update conflict, please retry");
    }

    // 2. Record lock detail
    WalletLock lock = WalletLock.builder()
        .walletId(walletId)
        .lockAmount(amount)
        .lockReason(LockReason.BET_PENDING)
        .referenceId(betId)
        .createdAt(LocalDateTime.now())
        .build();

    walletLockDao.insert(lock);

    // 3. Publish event
    eventPublisher.publish(new WalletLockedEvent(walletId, amount, betId));
}

/**
 * Unlock wallet amount (used during bet settlement)
 *
 * @param walletId Wallet ID
 * @param betId Bet order ID
 */
@Transactional(rollbackFor = Throwable.class)
public void unlockWalletAmount(Long walletId, String betId) {
    // 1. Query lock record
    WalletLock lock = walletLockDao.findByReference(walletId, betId);
    if (lock == null) {
        log.warn("Lock not found for bet {}", betId);
        return;
    }

    // 2. Optimistic lock update
    int updated = walletDao.decrementLockedAmount(walletId, lock.getLockAmount());
    if (updated == 0) {
        throw new ConcurrentUpdateException("Wallet update conflict, please retry");
    }

    // 3. Delete lock record
    walletLockDao.deleteById(lock.getLockId());

    // 4. Publish event
    eventPublisher.publish(new WalletUnlockedEvent(walletId, lock.getLockAmount(), betId));
}
```

### 2.5 Outbox Pattern (Transactional Consistency)

```sql
-- Outbox event table
CREATE TABLE outbox_event (
    event_id BIGINT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL, -- WALLET, BET, WITHDRAWAL
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    INDEX idx_unprocessed (processed_at, created_at)
);
```

```java
/**
 * Save business data and event in the same transaction
 */
@Transactional(rollbackFor = Throwable.class)
public void debitWalletWithEvent(DebitRequest request) {
    // 1. Update wallet balance
    walletDao.debitBalance(request.getWalletId(), request.getAmount());

    // 2. Record transaction
    WalletTransaction tx = createTransaction(request);
    walletTransactionDao.insert(tx);

    // 3. Save Outbox event (same transaction)
    OutboxEvent event = OutboxEvent.builder()
        .aggregateType("WALLET")
        .aggregateId(request.getWalletId().toString())
        .eventType("WALLET_DEBITED")
        .payload(toJson(tx))
        .createdAt(LocalDateTime.now())
        .build();

    outboxEventDao.insert(event);

    // After transaction commits, event will be relayed to Kafka asynchronously
}

/**
 * Outbox Event Relay (scheduled task)
 */
@Scheduled(fixedDelay = 1000)
public void relayOutboxEvents() {
    List<OutboxEvent> events = outboxEventDao.findUnprocessed(100);

    for (OutboxEvent event : events) {
        try {
            // Send to Kafka
            kafkaTemplate.send(
                "wallet-events",
                event.getAggregateId(),
                event.getPayload()
            );

            // Mark as processed
            outboxEventDao.markProcessed(event.getEventId());

        } catch (Exception e) {
            log.error("Failed to relay event {}", event.getEventId(), e);
            // Will retry on next cycle
        }
    }
}
```

---

## 3. Payment Gateway Integration

### 3.1 Gateway Interface Definition

```java
/**
 * Payment gateway abstract interface
 */
public interface PaymentGateway {

    /**
     * Create deposit order
     *
     * @param request Deposit request
     * @return Payment URL or QR code
     */
    DepositResponse createDeposit(DepositRequest request);

    /**
     * Handle deposit callback
     *
     * @param callback Callback data
     * @return Processing result
     */
    CallbackResult handleDepositCallback(Map<String, String> callback);

    /**
     * Create withdrawal order
     *
     * @param request Withdrawal request
     * @return Withdrawal order number
     */
    WithdrawalResponse createWithdrawal(WithdrawalRequest request);

    /**
     * Query withdrawal status
     *
     * @param orderId Order ID
     * @return Order status
     */
    WithdrawalStatus queryWithdrawalStatus(String orderId);

    /**
     * Verify callback signature
     *
     * @param params Callback parameters
     * @param signature Signature
     * @return Whether valid
     */
    boolean verifySignature(Map<String, String> params, String signature);
}
```

### 3.2 HMAC Signature Verification

```java
/**
 * Payment gateway signature utility
 */
@Component
public class PaymentSignatureUtil {

    @Value("${payment.gateway.secret-key}")
    private String secretKey;

    /**
     * Generate HMAC-SHA256 signature
     *
     * Steps:
     * 1. Sort parameters by key
     * 2. Concatenate as key1=value1&key2=value2
     * 3. HMAC-SHA256 encrypt
     * 4. Base64 encode
     *
     * @param params Parameters
     * @return Signature
     */
    public String generateSignature(Map<String, String> params) {
        // 1. Filter empty values and sort
        String sortedParams = params.entrySet().stream()
            .filter(e -> StringUtils.isNotBlank(e.getValue()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        // 2. HMAC-SHA256 encrypt
        byte[] hmac = HmacUtils.hmacSha256(secretKey.getBytes(), sortedParams.getBytes());

        // 3. Base64 encode
        return Base64.getEncoder().encodeToString(hmac);
    }

    /**
     * Verify signature
     *
     * @param params Parameters
     * @param receivedSignature Received signature
     * @return Whether valid
     */
    public boolean verifySignature(Map<String, String> params, String receivedSignature) {
        String expectedSignature = generateSignature(params);

        // Use time-safe comparison function (prevent timing attacks)
        return MessageDigest.isEqual(
            expectedSignature.getBytes(),
            receivedSignature.getBytes()
        );
    }
}
```

### 3.3 Deposit Service Implementation

```java
/**
 * Deposit service
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private final PaymentGateway paymentGateway;
    private final DepositOrderDao depositOrderDao;
    private final WalletService walletService;

    /**
     * Create deposit order
     *
     * @param request Deposit request
     * @return Payment URL
     */
    public DepositResponse createDepositOrder(DepositRequest request) {
        // 1. Create order (status: PENDING)
        DepositOrder order = DepositOrder.builder()
            .orderId(generateOrderId())
            .playerId(request.getPlayerId())
            .tenantId(request.getTenantId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .paymentMethod(request.getPaymentMethod())
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();

        depositOrderDao.insert(order);

        // 2. Call payment gateway
        DepositResponse response = paymentGateway.createDeposit(request);

        // 3. Update order (record gateway order ID)
        depositOrderDao.updateGatewayOrderId(
            order.getOrderId(),
            response.getGatewayOrderId()
        );

        return response;
    }

    /**
     * Handle deposit callback
     *
     * Idempotency guarantee: each order is credited only once
     *
     * @param callback Callback data
     * @return Processing result
     */
    @Transactional(rollbackFor = Throwable.class)
    public CallbackResult handleDepositCallback(Map<String, String> callback) {
        String gatewayOrderId = callback.get("order_id");
        String status = callback.get("status");
        String signature = callback.get("signature");

        // 1. Verify signature
        if (!paymentGateway.verifySignature(callback, signature)) {
            log.error("Invalid signature for order {}", gatewayOrderId);
            return CallbackResult.failure("Invalid signature");
        }

        // 2. Query order
        DepositOrder order = depositOrderDao.findByGatewayOrderId(gatewayOrderId);
        if (order == null) {
            log.error("Order not found: {}", gatewayOrderId);
            return CallbackResult.failure("Order not found");
        }

        // 3. Idempotency check
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.info("Order {} already processed", order.getOrderId());
            return CallbackResult.success("Already processed");
        }

        // 4. Update order status
        if ("SUCCESS".equals(status)) {
            order.setStatus(OrderStatus.SUCCESS);
            order.setCompletedAt(LocalDateTime.now());
            depositOrderDao.updateStatus(order);

            // 5. Credit wallet
            walletService.creditWallet(
                order.getPlayerId(),
                order.getAmount(),
                "DEPOSIT:" + order.getOrderId()
            );

            // 6. Send notification
            notificationService.sendDepositSuccessNotification(order);

            return CallbackResult.success("Deposit processed");

        } else {
            order.setStatus(OrderStatus.FAILED);
            depositOrderDao.updateStatus(order);

            return CallbackResult.success("Deposit failed");
        }
    }
}
```

### 3.4 Withdrawal Service Implementation

```java
/**
 * Withdrawal service
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final PaymentGateway paymentGateway;
    private final WithdrawalOrderDao withdrawalOrderDao;
    private final WalletService walletService;
    private final RiskControlService riskControlService;

    /**
     * Create withdrawal order
     *
     * Flow:
     * 1. Risk control evaluation
     * 2. Lock wallet amount
     * 3. Submit to payment gateway
     * 4. Async status polling
     *
     * @param request Withdrawal request
     * @return Order result
     */
    @Transactional(rollbackFor = Throwable.class)
    public WithdrawalResult createWithdrawalOrder(WithdrawalRequest request) {
        // 1. Risk control evaluation
        RiskDecision decision = riskControlService.evaluateWithdrawal(request);

        if (decision.getAction() == RiskAction.REJECT) {
            throw new WithdrawalRejectedException(decision.getReason());
        }

        // 2. Create order
        WithdrawalOrder order = WithdrawalOrder.builder()
            .orderId(generateOrderId())
            .playerId(request.getPlayerId())
            .tenantId(request.getTenantId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .bankAccount(request.getBankAccount())
            .status(OrderStatus.PENDING)
            .riskScore(decision.getScore())
            .createdAt(LocalDateTime.now())
            .build();

        withdrawalOrderDao.insert(order);

        // 3. Lock wallet amount
        walletService.lockWalletAmount(
            request.getPlayerId(),
            request.getAmount(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 4. If manual review required
        if (decision.getAction() == RiskAction.MANUAL_REVIEW) {
            order.setStatus(OrderStatus.REVIEWING);
            withdrawalOrderDao.updateStatus(order);

            // Notify risk control team
            notificationService.notifyManualReview(order);

            return WithdrawalResult.underReview(order.getOrderId());
        }

        // 5. Auto-approved, submit to payment gateway
        WithdrawalResponse response = paymentGateway.createWithdrawal(request);

        order.setGatewayOrderId(response.getGatewayOrderId());
        order.setStatus(OrderStatus.PROCESSING);
        withdrawalOrderDao.update(order);

        // 6. Start async status query
        scheduleStatusQuery(order.getOrderId());

        return WithdrawalResult.processing(order.getOrderId());
    }

    /**
     * Query withdrawal status (scheduled task)
     *
     * Every 30 seconds, max 24 hours
     */
    @Scheduled(fixedDelay = 30000)
    public void queryPendingWithdrawals() {
        List<WithdrawalOrder> orders = withdrawalOrderDao.findProcessing();

        for (WithdrawalOrder order : orders) {
            try {
                WithdrawalStatus status = paymentGateway.queryWithdrawalStatus(
                    order.getGatewayOrderId()
                );

                if (status == WithdrawalStatus.SUCCESS) {
                    completeWithdrawal(order);
                } else if (status == WithdrawalStatus.FAILED) {
                    failWithdrawal(order);
                } else if (isTimeout(order)) {
                    timeoutWithdrawal(order);
                }

            } catch (Exception e) {
                log.error("Failed to query withdrawal {}", order.getOrderId(), e);
            }
        }
    }

    /**
     * Complete withdrawal
     */
    @Transactional(rollbackFor = Throwable.class)
    private void completeWithdrawal(WithdrawalOrder order) {
        // 1. Update order status
        order.setStatus(OrderStatus.SUCCESS);
        order.setCompletedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 2. Debit wallet balance (unlock + debit)
        walletService.debitWallet(
            order.getPlayerId(),
            order.getAmount(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 3. Send notification
        notificationService.sendWithdrawalSuccessNotification(order);
    }

    /**
     * Withdrawal failure
     */
    @Transactional(rollbackFor = Throwable.class)
    private void failWithdrawal(WithdrawalOrder order) {
        // 1. Update order status
        order.setStatus(OrderStatus.FAILED);
        withdrawalOrderDao.update(order);

        // 2. Unlock wallet amount
        walletService.unlockWalletAmount(
            order.getPlayerId(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 3. Send notification
        notificationService.sendWithdrawalFailedNotification(order);
    }
}
```

---

## 4. Withdrawal Risk Control Implementation

### 4.1 Risk Scoring Model

```java
/**
 * Withdrawal risk scorer
 *
 * Scoring dimensions:
 * 1. Player credit score (0-20 points)
 * 2. KYC completeness (0-15 points)
 * 3. Deposit/withdrawal ratio (0-15 points)
 * 4. Recent withdrawal frequency (0-15 points)
 * 5. Turnover completion (0-15 points)
 * 6. IP/device anomaly (0-10 points)
 * 7. Multi-account correlation (0-10 points)
 *
 * Total 0-100, decision thresholds:
 * - [0, 30): Auto-approve
 * - [30, 70): Manual review
 * - [70, 100]: Auto-reject
 */
@Component
@RequiredArgsConstructor
public class WithdrawalRiskScorer {

    private final PlayerCreditService playerCreditService;
    private final KycService kycService;
    private final TransactionHistoryService transactionHistoryService;
    private final FraudDetectionService fraudDetectionService;

    /**
     * Calculate withdrawal risk score
     *
     * @param request Withdrawal request
     * @return Risk decision
     */
    public RiskDecision scoreWithdrawal(WithdrawalRequest request) {
        Long playerId = request.getPlayerId();
        BigDecimal amount = request.getAmount();

        // 1. Player credit score (0-20 points)
        int creditScore = playerCreditService.getCreditScore(playerId);
        int creditRisk = (100 - creditScore) * 20 / 100; // Inverse calculation

        // 2. KYC completeness (0-15 points)
        KycLevel kycLevel = kycService.getKycLevel(playerId);
        int kycRisk = switch (kycLevel) {
            case VERIFIED -> 0;
            case PARTIALLY_VERIFIED -> 8;
            case NOT_VERIFIED -> 15;
        };

        // 3. Deposit/withdrawal ratio (0-15 points)
        BigDecimal depositTotal = transactionHistoryService.getTotalDeposits(playerId);
        BigDecimal withdrawalTotal = transactionHistoryService.getTotalWithdrawals(playerId);

        int ratioRisk = 0;
        if (depositTotal.compareTo(BigDecimal.ZERO) == 0) {
            ratioRisk = 15; // Never deposited, direct withdrawal
        } else {
            BigDecimal ratio = withdrawalTotal.divide(depositTotal, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) > 0) {
                ratioRisk = 15; // Withdrawal exceeds 2x deposits
            } else if (ratio.compareTo(new BigDecimal("1.5")) > 0) {
                ratioRisk = 10;
            } else if (ratio.compareTo(new BigDecimal("1.0")) > 0) {
                ratioRisk = 5;
            }
        }

        // 4. Recent withdrawal frequency (0-15 points)
        int recentWithdrawals = transactionHistoryService.countWithdrawalsLast7Days(playerId);
        int frequencyRisk = Math.min(recentWithdrawals * 3, 15);

        // 5. Turnover completion (0-15 points)
        BigDecimal requiredTurnover = calculateRequiredTurnover(playerId);
        BigDecimal actualTurnover = transactionHistoryService.getTurnover(playerId);

        int turnoverRisk = 0;
        if (requiredTurnover.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal completion = actualTurnover.divide(requiredTurnover, 2, RoundingMode.HALF_UP);
            if (completion.compareTo(BigDecimal.ONE) < 0) {
                turnoverRisk = 15 - (completion.multiply(new BigDecimal("15"))).intValue();
            }
        }

        // 6. IP/device anomaly (0-10 points)
        int deviceRisk = fraudDetectionService.detectDeviceAnomaly(playerId);

        // 7. Multi-account correlation (0-10 points)
        int multiAccountRisk = fraudDetectionService.detectMultiAccount(playerId);

        // Calculate total score
        int totalScore = creditRisk + kycRisk + ratioRisk + frequencyRisk
                       + turnoverRisk + deviceRisk + multiAccountRisk;

        // Decision
        RiskAction action;
        String reason;

        if (totalScore < 30) {
            action = RiskAction.AUTO_APPROVE;
            reason = "Low risk, auto-approved";
        } else if (totalScore < 70) {
            action = RiskAction.MANUAL_REVIEW;
            reason = String.format("Medium risk (%d points), manual review required", totalScore);
        } else {
            action = RiskAction.REJECT;
            reason = String.format("High risk (%d points), auto-rejected", totalScore);
        }

        return RiskDecision.builder()
            .score(totalScore)
            .action(action)
            .reason(reason)
            .scoreBreakdown(Map.of(
                "credit", creditRisk,
                "kyc", kycRisk,
                "ratio", ratioRisk,
                "frequency", frequencyRisk,
                "turnover", turnoverRisk,
                "device", deviceRisk,
                "multiAccount", multiAccountRisk
            ))
            .build();
    }
}
```

### 4.2 LiteFlow Rule Engine Configuration

```java
/**
 * LiteFlow risk control chain configuration
 */
@Configuration
public class WithdrawalRiskChainConfig {

    /**
     * Risk control rule chain (EL expression)
     *
     * Rules:
     * 1. Basic validation (player status, balance check)
     * 2. Risk scoring
     * 3. Decision routing
     * 4. Compensation handling (unlock on failure)
     */
    @Bean
    public String withdrawalRiskChainEL() {
        return """
            THEN(
                validation_node,
                risk_scoring_node,
                decision_node,
                CATCH(
                    notification_node
                ).DO(
                    compensation_node
                )
            )
            """;
    }
}

/**
 * Basic validation node
 */
@Component("validation_node")
public class ValidationNode extends NodeComponent {

    @Resource
    private PlayerService playerService;

    @Resource
    private WalletService walletService;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();

        // 1. Check player status
        Player player = playerService.getPlayer(request.getPlayerId());
        if (player.getStatus() == PlayerStatus.BLOCKED) {
            throw new BizException("Player is blocked, withdrawal denied");
        }

        // 2. Check balance
        BigDecimal balance = walletService.getAvailableBalance(request.getPlayerId());
        if (balance.compareTo(request.getAmount()) < 0) {
            throw new BizException("Insufficient balance");
        }

        // 3. Check minimum withdrawal amount
        if (request.getAmount().compareTo(new BigDecimal("100")) < 0) {
            throw new BizException("Withdrawal amount must be at least 100");
        }
    }
}

/**
 * Risk scoring node
 */
@Component("risk_scoring_node")
public class RiskScoringNode extends NodeComponent {

    @Resource
    private WithdrawalRiskScorer riskScorer;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();

        // Calculate risk score
        RiskDecision decision = riskScorer.scoreWithdrawal(request);

        // Save to context
        this.setContextBean("riskDecision", decision);

        log.info("Withdrawal {} risk score: {}, action: {}",
                 request.getOrderId(),
                 decision.getScore(),
                 decision.getAction());
    }
}

/**
 * Decision node
 */
@Component("decision_node")
public class DecisionNode extends NodeComponent {

    @Resource
    private WithdrawalOrderDao withdrawalOrderDao;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();
        RiskDecision decision = this.getContextBean("riskDecision");

        // Route based on decision
        switch (decision.getAction()) {
            case AUTO_APPROVE -> {
                withdrawalOrderDao.updateStatus(
                    request.getOrderId(),
                    OrderStatus.APPROVED
                );
            }
            case MANUAL_REVIEW -> {
                withdrawalOrderDao.updateStatus(
                    request.getOrderId(),
                    OrderStatus.REVIEWING
                );
            }
            case REJECT -> {
                withdrawalOrderDao.updateStatus(
                    request.getOrderId(),
                    OrderStatus.REJECTED
                );
                throw new WithdrawalRejectedException(decision.getReason());
            }
        }
    }
}

/**
 * Compensation node (unlock wallet on failure)
 */
@Component("compensation_node")
public class CompensationNode extends NodeComponent {

    @Resource
    private WalletService walletService;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();

        // Unlock wallet amount
        walletService.unlockWalletAmount(
            request.getPlayerId(),
            "WITHDRAWAL:" + request.getOrderId()
        );

        log.info("Withdrawal {} compensation: unlocked wallet amount",
                 request.getOrderId());
    }
}
```

### 4.3 Review Workflow State Machine

```java
/**
 * Withdrawal review state machine
 *
 * State transitions:
 * PENDING -> REVIEWING -> APPROVED -> PROCESSING -> SUCCESS
 *                      -> REJECTED
 */
@Component
@RequiredArgsConstructor
public class WithdrawalWorkflowStateMachine {

    private final WithdrawalOrderDao withdrawalOrderDao;
    private final PaymentGateway paymentGateway;
    private final WalletService walletService;

    /**
     * Reviewer approval
     */
    @Transactional(rollbackFor = Throwable.class)
    public void approveByReviewer(String orderId, String reviewerId, String comment) {
        WithdrawalOrder order = withdrawalOrderDao.findById(orderId);

        // 1. State check
        if (order.getStatus() != OrderStatus.REVIEWING) {
            throw new IllegalStateException(
                "Invalid order status, current: " + order.getStatus()
            );
        }

        // 2. Update status
        order.setStatus(OrderStatus.APPROVED);
        order.setReviewerId(reviewerId);
        order.setReviewComment(comment);
        order.setReviewedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 3. Submit to payment gateway
        WithdrawalResponse response = paymentGateway.createWithdrawal(order);

        order.setGatewayOrderId(response.getGatewayOrderId());
        order.setStatus(OrderStatus.PROCESSING);
        withdrawalOrderDao.update(order);

        // 4. Record audit log
        auditLogService.log(AuditEvent.builder()
            .action("WITHDRAWAL_APPROVED")
            .operator(reviewerId)
            .targetId(orderId)
            .comment(comment)
            .build());
    }

    /**
     * Reviewer rejection
     */
    @Transactional(rollbackFor = Throwable.class)
    public void rejectByReviewer(String orderId, String reviewerId, String reason) {
        WithdrawalOrder order = withdrawalOrderDao.findById(orderId);

        // 1. State check
        if (order.getStatus() != OrderStatus.REVIEWING) {
            throw new IllegalStateException(
                "Invalid order status, current: " + order.getStatus()
            );
        }

        // 2. Update status
        order.setStatus(OrderStatus.REJECTED);
        order.setReviewerId(reviewerId);
        order.setReviewComment(reason);
        order.setReviewedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 3. Unlock wallet amount
        walletService.unlockWalletAmount(
            order.getPlayerId(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 4. Send notification
        notificationService.sendWithdrawalRejectedNotification(order);

        // 5. Record audit log
        auditLogService.log(AuditEvent.builder()
            .action("WITHDRAWAL_REJECTED")
            .operator(reviewerId)
            .targetId(orderId)
            .comment(reason)
            .build());
    }
}
```

### 4.4 SAGA Compensation Orchestrator

```java
/**
 * Withdrawal SAGA Orchestrator
 *
 * Steps:
 * 1. Lock wallet amount
 * 2. Risk control evaluation
 * 3. Submit to payment gateway
 * 4. Debit balance
 *
 * Compensation:
 * On any failure, rollback previous steps in reverse order
 */
@Component
@RequiredArgsConstructor
public class WithdrawalSagaOrchestrator {

    private final WalletService walletService;
    private final RiskControlService riskControlService;
    private final PaymentGateway paymentGateway;

    /**
     * Execute withdrawal SAGA
     */
    public WithdrawalResult executeWithdrawalSaga(WithdrawalRequest request) {
        String orderId = request.getOrderId();
        List<CompensationAction> compensations = new ArrayList<>();

        try {
            // Step 1: Lock wallet amount
            walletService.lockWalletAmount(
                request.getPlayerId(),
                request.getAmount(),
                "WITHDRAWAL:" + orderId
            );
            compensations.add(() -> {
                walletService.unlockWalletAmount(
                    request.getPlayerId(),
                    "WITHDRAWAL:" + orderId
                );
            });

            // Step 2: Risk control evaluation
            RiskDecision decision = riskControlService.evaluateWithdrawal(request);
            if (decision.getAction() == RiskAction.REJECT) {
                throw new WithdrawalRejectedException(decision.getReason());
            }

            // Step 3: Submit to payment gateway
            WithdrawalResponse response = paymentGateway.createWithdrawal(request);
            compensations.add(() -> {
                paymentGateway.cancelWithdrawal(response.getGatewayOrderId());
            });

            // Step 4: Debit balance
            walletService.debitWallet(
                request.getPlayerId(),
                request.getAmount(),
                "WITHDRAWAL:" + orderId
            );
            compensations.add(() -> {
                walletService.creditWallet(
                    request.getPlayerId(),
                    request.getAmount(),
                    "WITHDRAWAL_REFUND:" + orderId
                );
            });

            return WithdrawalResult.success(orderId);

        } catch (Exception e) {
            // Execute compensations in reverse order
            log.error("Withdrawal SAGA failed for order {}, executing compensations",
                      orderId, e);

            for (int i = compensations.size() - 1; i >= 0; i--) {
                try {
                    compensations.get(i).compensate();
                } catch (Exception ce) {
                    log.error("Compensation failed for order {}", orderId, ce);
                    // Route to manual processing queue
                    manualCompensationQueue.add(orderId, compensations.get(i));
                }
            }

            throw new WithdrawalSagaException("Withdrawal process failed: " + e.getMessage(), e);
        }
    }
}

/**
 * Compensation action interface
 */
@FunctionalInterface
interface CompensationAction {
    void compensate() throws Exception;
}
```

---

## 5. Reconciliation System Implementation

### 5.1 Database Schema

```sql
-- Daily financial reconciliation table
CREATE TABLE daily_financial_reconciliation (
    recon_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    recon_date DATE NOT NULL,

    -- Wallet reconciliation
    wallet_balance_system DECIMAL(19,4) NOT NULL,
    wallet_balance_db DECIMAL(19,4) NOT NULL,
    wallet_balance_diff DECIMAL(19,4) NOT NULL,

    -- Deposit reconciliation
    deposit_count_system INT NOT NULL,
    deposit_count_gateway INT NOT NULL,
    deposit_amount_system DECIMAL(19,4) NOT NULL,
    deposit_amount_gateway DECIMAL(19,4) NOT NULL,
    deposit_diff DECIMAL(19,4) NOT NULL,

    -- Withdrawal reconciliation
    withdrawal_count_system INT NOT NULL,
    withdrawal_count_gateway INT NOT NULL,
    withdrawal_amount_system DECIMAL(19,4) NOT NULL,
    withdrawal_amount_gateway DECIMAL(19,4) NOT NULL,
    withdrawal_diff DECIMAL(19,4) NOT NULL,

    -- Bet reconciliation
    bet_count_system INT NOT NULL,
    bet_count_provider INT NOT NULL,
    bet_amount_system DECIMAL(19,4) NOT NULL,
    bet_amount_provider DECIMAL(19,4) NOT NULL,
    bet_diff DECIMAL(19,4) NOT NULL,

    -- Reconciliation status
    status VARCHAR(20) NOT NULL, -- MATCHED, MISMATCHED, PENDING
    created_at TIMESTAMP NOT NULL,

    UNIQUE(tenant_id, recon_date),
    INDEX idx_status_date (status, recon_date)
);

-- Reconciliation discrepancy detail table
CREATE TABLE reconciliation_discrepancy (
    discrepancy_id BIGINT PRIMARY KEY,
    recon_id BIGINT NOT NULL,
    discrepancy_type VARCHAR(30) NOT NULL, -- WALLET, DEPOSIT, WITHDRAWAL, BET
    reference_id VARCHAR(64) NOT NULL,
    system_amount DECIMAL(19,4) NOT NULL,
    external_amount DECIMAL(19,4) NOT NULL,
    diff_amount DECIMAL(19,4) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    resolved_by VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL,

    INDEX idx_recon_type (recon_id, discrepancy_type),
    INDEX idx_unresolved (resolved, created_at)
);
```

### 5.2 Daily Financial Reconciliation Service

```java
/**
 * Daily financial reconciliation service
 *
 * Execution time: 02:00 AM daily
 */
@Service
@RequiredArgsConstructor
public class DailyFinancialReconciliationService {

    private final WalletDao walletDao;
    private final DepositOrderDao depositOrderDao;
    private final WithdrawalOrderDao withdrawalOrderDao;
    private final BetOrderDao betOrderDao;
    private final PaymentGateway paymentGateway;
    private final GameProviderService gameProviderService;
    private final ReconciliationDao reconciliationDao;

    /**
     * Execute daily reconciliation
     *
     * @param tenantId Tenant ID
     * @param reconDate Reconciliation date
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(rollbackFor = Throwable.class)
    public void executeDailyReconciliation(String tenantId, LocalDate reconDate) {
        log.info("Starting daily reconciliation for tenant {} on {}",
                 tenantId, reconDate);

        // 1. Wallet reconciliation
        WalletReconciliationResult walletResult = reconcileWallets(tenantId, reconDate);

        // 2. Deposit reconciliation
        DepositReconciliationResult depositResult = reconcileDeposits(tenantId, reconDate);

        // 3. Withdrawal reconciliation
        WithdrawalReconciliationResult withdrawalResult = reconcileWithdrawals(tenantId, reconDate);

        // 4. Bet reconciliation
        BetReconciliationResult betResult = reconcileBets(tenantId, reconDate);

        // 5. Save reconciliation results
        DailyFinancialReconciliation recon = DailyFinancialReconciliation.builder()
            .tenantId(tenantId)
            .reconDate(reconDate)
            .walletBalanceSystem(walletResult.getSystemBalance())
            .walletBalanceDb(walletResult.getDbBalance())
            .walletBalanceDiff(walletResult.getDiff())
            .depositCountSystem(depositResult.getSystemCount())
            .depositCountGateway(depositResult.getGatewayCount())
            .depositAmountSystem(depositResult.getSystemAmount())
            .depositAmountGateway(depositResult.getGatewayAmount())
            .depositDiff(depositResult.getDiff())
            .withdrawalCountSystem(withdrawalResult.getSystemCount())
            .withdrawalCountGateway(withdrawalResult.getGatewayCount())
            .withdrawalAmountSystem(withdrawalResult.getSystemAmount())
            .withdrawalAmountGateway(withdrawalResult.getGatewayAmount())
            .withdrawalDiff(withdrawalResult.getDiff())
            .betCountSystem(betResult.getSystemCount())
            .betCountProvider(betResult.getProviderCount())
            .betAmountSystem(betResult.getSystemAmount())
            .betAmountProvider(betResult.getProviderAmount())
            .betDiff(betResult.getDiff())
            .status(determineStatus(walletResult, depositResult, withdrawalResult, betResult))
            .createdAt(LocalDateTime.now())
            .build();

        reconciliationDao.insert(recon);

        // 6. Send alert if discrepancies found
        if (recon.getStatus() == ReconciliationStatus.MISMATCHED) {
            alertService.sendReconciliationMismatchAlert(recon);
        }

        log.info("Daily reconciliation completed for tenant {} on {}, status: {}",
                 tenantId, reconDate, recon.getStatus());
    }

    /**
     * Wallet reconciliation
     */
    private WalletReconciliationResult reconcileWallets(String tenantId, LocalDate date) {
        // 1. System balance (Redis)
        BigDecimal systemBalance = walletDao.sumAllBalancesFromRedis(tenantId);

        // 2. Database balance
        BigDecimal dbBalance = walletDao.sumAllBalancesFromDb(tenantId);

        // 3. Calculate difference
        BigDecimal diff = systemBalance.subtract(dbBalance);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Wallet balance mismatch: system={}, db={}, diff={}",
                      systemBalance, dbBalance, diff);
        }

        return new WalletReconciliationResult(systemBalance, dbBalance, diff);
    }

    /**
     * Deposit reconciliation
     */
    private DepositReconciliationResult reconcileDeposits(String tenantId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        int systemCount = depositOrderDao.countByPeriod(tenantId, startOfDay, endOfDay);
        BigDecimal systemAmount = depositOrderDao.sumAmountByPeriod(tenantId, startOfDay, endOfDay);

        PaymentGatewayReport gatewayReport = paymentGateway.getDailyReport(tenantId, date);
        int gatewayCount = gatewayReport.getDepositCount();
        BigDecimal gatewayAmount = gatewayReport.getDepositAmount();

        BigDecimal diff = systemAmount.subtract(gatewayAmount);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Deposit mismatch: system={}/{}, gateway={}/{}, diff={}",
                      systemCount, systemAmount, gatewayCount, gatewayAmount, diff);

            List<String> mismatchedOrders = findMismatchedDepositOrders(
                tenantId, date, systemCount, gatewayCount
            );

            for (String orderId : mismatchedOrders) {
                recordDiscrepancy(ReconciliationType.DEPOSIT, orderId, diff);
            }
        }

        return new DepositReconciliationResult(
            systemCount, gatewayCount, systemAmount, gatewayAmount, diff
        );
    }

    /**
     * Bet reconciliation (three-way comparison)
     */
    private BetReconciliationResult reconcileBets(String tenantId, LocalDate date) {
        // 1. System statistics (OLTP)
        int systemCount = betOrderDao.countByPeriod(tenantId, date);
        BigDecimal systemAmount = betOrderDao.sumAmountByPeriod(tenantId, date);

        // 2. Game provider statistics
        GameProviderReport providerReport = gameProviderService.getDailyReport(tenantId, date);
        int providerCount = providerReport.getBetCount();
        BigDecimal providerAmount = providerReport.getBetAmount();

        // 3. Calculate difference
        BigDecimal diff = systemAmount.subtract(providerAmount);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Bet mismatch: system={}/{}, provider={}/{}, diff={}",
                      systemCount, systemAmount, providerCount, providerAmount, diff);

            // Three-way comparison (system, provider, OLAP)
            performThreeWayBetReconciliation(tenantId, date);
        }

        return new BetReconciliationResult(
            systemCount, providerCount, systemAmount, providerAmount, diff
        );
    }

    /**
     * Three-way bet reconciliation
     *
     * Compare:
     * 1. OLTP system (real-time)
     * 2. Game provider report
     * 3. OLAP data warehouse (T+1)
     */
    private void performThreeWayBetReconciliation(String tenantId, LocalDate date) {
        // 1. Fetch bet lists from three sources
        List<BetOrder> systemBets = betOrderDao.findByDate(tenantId, date);
        List<BetOrder> providerBets = gameProviderService.getBetsByDate(tenantId, date);
        List<BetOrder> olapBets = olapService.getBetsByDate(tenantId, date);

        // 2. Convert to Sets (using bet ID)
        Set<String> systemBetIds = systemBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        Set<String> providerBetIds = providerBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        Set<String> olapBetIds = olapBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        // 3. Find discrepancies
        Set<String> onlyInSystem = new HashSet<>(systemBetIds);
        onlyInSystem.removeAll(providerBetIds);
        onlyInSystem.removeAll(olapBetIds);

        Set<String> onlyInProvider = new HashSet<>(providerBetIds);
        onlyInProvider.removeAll(systemBetIds);
        onlyInProvider.removeAll(olapBetIds);

        Set<String> onlyInOlap = new HashSet<>(olapBetIds);
        onlyInOlap.removeAll(systemBetIds);
        onlyInOlap.removeAll(providerBetIds);

        // 4. Log discrepancies
        if (!onlyInSystem.isEmpty()) {
            log.error("Bets only in system: {}", onlyInSystem);
            // Possible: provider did not report
        }

        if (!onlyInProvider.isEmpty()) {
            log.error("Bets only in provider: {}", onlyInProvider);
            // Possible: system did not record (critical issue)
        }

        if (!onlyInOlap.isEmpty()) {
            log.error("Bets only in OLAP: {}", onlyInOlap);
            // Possible: ETL delay
        }
    }
}
```

### 5.3 Discrepancy Resolution Service

```java
/**
 * Reconciliation discrepancy resolution service
 */
@Service
@RequiredArgsConstructor
public class ReconciliationDiscrepancyService {

    private final ReconciliationDiscrepancyDao discrepancyDao;
    private final WalletService walletService;

    /**
     * Manually resolve discrepancy
     *
     * @param discrepancyId Discrepancy ID
     * @param action Resolution action (ADJUST_SYSTEM, ADJUST_EXTERNAL, IGNORE)
     * @param operator Operator
     * @param comment Notes
     */
    @Transactional(rollbackFor = Throwable.class)
    public void resolveDiscrepancy(
        Long discrepancyId,
        ResolutionAction action,
        String operator,
        String comment
    ) {
        ReconciliationDiscrepancy discrepancy = discrepancyDao.findById(discrepancyId);

        if (discrepancy.getResolved()) {
            throw new IllegalStateException("Discrepancy already resolved");
        }

        switch (action) {
            case ADJUST_SYSTEM -> {
                // Adjust system data (e.g., create missing order)
                adjustSystemData(discrepancy);
            }
            case ADJUST_EXTERNAL -> {
                // Contact external party for adjustment
                requestExternalAdjustment(discrepancy);
            }
            case IGNORE -> {
                // Mark as ignorable (small amount difference)
                log.info("Discrepancy {} marked as ignore", discrepancyId);
            }
        }

        // Mark as resolved
        discrepancy.setResolved(true);
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancy.setResolvedBy(operator);
        discrepancyDao.update(discrepancy);

        // Record audit log
        auditLogService.log(AuditEvent.builder()
            .action("DISCREPANCY_RESOLVED")
            .operator(operator)
            .targetId(discrepancyId.toString())
            .comment(comment)
            .build());
    }

    /**
     * Auto-resolve minor discrepancies
     *
     * Condition: difference < 0.01 AND appears for 3+ consecutive days
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void autoResolveMinorDiscrepancies() {
        List<ReconciliationDiscrepancy> minorDiscrepancies =
            discrepancyDao.findUnresolvedMinorDiscrepancies(new BigDecimal("0.01"));

        for (ReconciliationDiscrepancy discrepancy : minorDiscrepancies) {
            int consecutiveDays = countConsecutiveDays(discrepancy);

            if (consecutiveDays >= 3) {
                resolveDiscrepancy(
                    discrepancy.getDiscrepancyId(),
                    ResolutionAction.IGNORE,
                    "SYSTEM",
                    "Auto-ignored: difference < 0.01 for 3+ consecutive days"
                );
            }
        }
    }
}
```

### 5.4 Report Generation Service

```java
/**
 * Reconciliation report generation service
 */
@Service
@RequiredArgsConstructor
public class ReconciliationReportService {

    private final ReconciliationDao reconciliationDao;
    private final ReconciliationDiscrepancyDao discrepancyDao;

    /**
     * Generate daily reconciliation report (PDF)
     *
     * @param tenantId Tenant ID
     * @param reconDate Reconciliation date
     * @return PDF file path
     */
    public String generateDailyReport(String tenantId, LocalDate reconDate) {
        // 1. Query reconciliation data
        DailyFinancialReconciliation recon = reconciliationDao.findByDate(tenantId, reconDate);

        // 2. Query discrepancy details
        List<ReconciliationDiscrepancy> discrepancies =
            discrepancyDao.findByReconId(recon.getReconId());

        // 3. Generate PDF
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream("report.pdf"));
        document.open();

        // Title
        document.add(new Paragraph("Daily Financial Reconciliation Report", titleFont));
        document.add(new Paragraph("Tenant: " + tenantId, normalFont));
        document.add(new Paragraph("Date: " + reconDate, normalFont));
        document.add(new Paragraph("Status: " + recon.getStatus(), normalFont));
        document.add(Chunk.NEWLINE);

        // Wallet reconciliation table
        PdfPTable walletTable = new PdfPTable(3);
        walletTable.addCell("Item");
        walletTable.addCell("System");
        walletTable.addCell("Database");
        walletTable.addCell("Balance");
        walletTable.addCell(recon.getWalletBalanceSystem().toString());
        walletTable.addCell(recon.getWalletBalanceDb().toString());
        document.add(walletTable);

        // Deposit reconciliation table
        PdfPTable depositTable = new PdfPTable(4);
        depositTable.addCell("Item");
        depositTable.addCell("System Count");
        depositTable.addCell("System Amount");
        depositTable.addCell("Gateway Amount");
        depositTable.addCell("Deposits");
        depositTable.addCell(recon.getDepositCountSystem().toString());
        depositTable.addCell(recon.getDepositAmountSystem().toString());
        depositTable.addCell(recon.getDepositAmountGateway().toString());
        document.add(depositTable);

        // Discrepancy details
        if (!discrepancies.isEmpty()) {
            document.add(new Paragraph("Discrepancy Details:", subtitleFont));

            PdfPTable discrepancyTable = new PdfPTable(5);
            discrepancyTable.addCell("Type");
            discrepancyTable.addCell("Reference");
            discrepancyTable.addCell("System Amount");
            discrepancyTable.addCell("External Amount");
            discrepancyTable.addCell("Difference");

            for (ReconciliationDiscrepancy d : discrepancies) {
                discrepancyTable.addCell(d.getDiscrepancyType());
                discrepancyTable.addCell(d.getReferenceId());
                discrepancyTable.addCell(d.getSystemAmount().toString());
                discrepancyTable.addCell(d.getExternalAmount().toString());
                discrepancyTable.addCell(d.getDiffAmount().toString());
            }

            document.add(discrepancyTable);
        }

        document.close();

        return "report.pdf";
    }
}
```

---

## 6. Reference Documentation

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | Wallet Architecture (02-06) | Concurrency Control | Redis Lua atomicity |
| 2 | Transaction Processing Flow (02-07) | Event-Driven | Outbox Pattern |
| 3 | Payment Gateway Integration (02-02) | Integration Flow | Deposit/Withdrawal API |
| 4 | Withdrawal Risk (01-05) | Risk Control | Rule engine design |
| 5 | Risk Framework (05-01) | Rule Engine | LiteFlow implementation |
| 6 | Reconciliation System (02-03) | Reconciliation Model | Three-way reconciliation |
| 7 | Turnover Calculation (03-04) | Three-Layer Validation | Turnover reconciliation |
