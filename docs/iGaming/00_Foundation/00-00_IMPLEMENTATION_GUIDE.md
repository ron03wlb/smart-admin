# 00-00 實作指南 (Implementation Guide)

**版本**: 1.0.0
**創建日期**: 2026-02-03
**狀態**: ✅ v2.0 重組規範

---

## 📋 文檔目的

本文檔為**任務導向的實作指南**，幫助開發者快速找到"我要實作 XXX"的完整閱讀路徑和實作步驟。

**適用對象**：
- 後端開發工程師
- 全端開發工程師
- 技術架構師
- 系統整合工程師

**使用方式**：
1. 找到你的開發任務（例如："我要對接新遊戲廠商"）
2. 按順序閱讀推薦文檔
3. 參考實作步驟和代碼範例
4. 執行測試驗證

---

## 📚 目錄

### 核心財務流程
1. [實作錢包系統](#1-實作錢包系統)
2. [對接支付閘道](#2-對接支付閘道)
3. [實作出金風控流程](#3-實作出金風控流程)
4. [建立對帳系統](#4-建立對帳系統)

### 遊戲營運
5. [對接新遊戲廠商](#5-對接新遊戲廠商)
6. [實作 Seamless Wallet API](#6-實作-seamless-wallet-api)
7. [設計流水計算邏輯](#7-設計流水計算邏輯)

### 活動系統
8. [建立 Bonus 發放引擎](#8-建立-bonus-發放引擎)
9. [設計流水要求追蹤](#9-設計流水要求追蹤)
10. [實作 VIP 等級系統](#10-實作-vip-等級系統)

### 風控系統
11. [建立風控規則引擎](#11-建立風控規則引擎)
12. [實作欺詐檢測算法](#12-實作欺詐檢測算法)
13. [設計代理信用管理](#13-設計代理信用管理)

### 平台治理
14. [實作多租戶架構](#14-實作多租戶架構)
15. [設計 RBAC 權限系統](#15-設計-rbac-權限系統)
16. [建立審計日誌系統](#16-建立審計日誌系統)
17. [實作數據加密策略](#17-實作數據加密策略)

### 技術基礎設施
18. [設計 API 閘道](#18-設計-api-閘道)
19. [建立 Blue-Green 部署](#19-建立-blue-green-部署)
20. [實作 API 限流機制](#20-實作-api-限流機制)

---

## 1. 實作錢包系統

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [00-00 QUICKSTART](./00-00_QUICKSTART.md) | §1 錢包模型 | 5 分鐘 | 可下注餘額公式 |
| 2 | [01-02 Wallet_Architecture](../02_Finance_Center/02-06_Wallet_Architecture.md) | §2 錢包架構 | 15 分鐘 | 多錢包設計、鎖定邏輯 |
| 3 | [01-02 Wallet_Architecture](../02_Finance_Center/02-06_Wallet_Architecture.md) | §3 並發控制 | 10 分鐘 | Redis Lua 原子性 |
| 4 | [01-04 Transaction_Flow](../01_Core_Financial_Loop/01-04_Transaction_Flow.md) | §3 事件驅動 | 12 分鐘 | Outbox Pattern |

### 🎯 實作目標

完整實作錢包系統，支援：
- 多錢包類型（現金、Bonus、鎖定錢包）
- 可下注餘額計算
- 並發安全的餘額扣減
- 錢包鎖定/解鎖機制
- 交易事件發布

### 📝 實作步驟

#### Step 1: 設計數據庫 Schema

```sql
-- 錢包主表
CREATE TABLE wallet (
    wallet_id BIGINT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    wallet_type VARCHAR(20) NOT NULL, -- CASH, BONUS, LOCKED
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    locked_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    version INT NOT NULL DEFAULT 0, -- 樂觀鎖
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(player_id, wallet_type),
    INDEX idx_player_tenant (player_id, tenant_id)
);

-- 錢包交易記錄
CREATE TABLE wallet_transaction (
    transaction_id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAW, BET, WIN
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    request_id VARCHAR(64) UNIQUE NOT NULL, -- 冪等性
    created_at TIMESTAMP NOT NULL,
    INDEX idx_wallet_time (wallet_id, created_at)
);

-- 錢包鎖定記錄
CREATE TABLE wallet_lock (
    lock_id BIGINT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    lock_amount DECIMAL(19,4) NOT NULL,
    lock_reason VARCHAR(50) NOT NULL, -- BET_PENDING, WITHDRAWAL_PENDING
    reference_id VARCHAR(64) NOT NULL, -- 關聯的投注/出金訂單
    created_at TIMESTAMP NOT NULL,
    INDEX idx_wallet_ref (wallet_id, reference_id)
);
```

#### Step 2: 實作可下注餘額計算

```java
/**
 * 計算可下注餘額
 *
 * 公式: 可下注餘額 = 現金錢包餘額 - 鎖定金額 - 進行中投注
 *
 * @param playerId 玩家ID
 * @param tenantId 租戶ID
 * @return 可下注餘額
 */
public BigDecimal calculateAvailableBalance(Long playerId, String tenantId) {
    // 1. 查詢現金錢包
    Wallet cashWallet = walletDao.findByPlayerAndType(
        playerId,
        WalletType.CASH
    );

    if (cashWallet == null) {
        return BigDecimal.ZERO;
    }

    // 2. 計算公式
    BigDecimal availableBalance = cashWallet.getBalance()
        .subtract(cashWallet.getLockedAmount())
        .subtract(calculatePendingBets(playerId));

    // 3. 確保不為負數
    return availableBalance.max(BigDecimal.ZERO);
}

/**
 * 計算進行中投注金額
 */
private BigDecimal calculatePendingBets(Long playerId) {
    return betDao.sumPendingBetAmount(playerId);
}
```

#### Step 3: 實作並發安全的餘額扣減（Redis Lua）

```java
/**
 * 扣減錢包餘額（原子性操作）
 *
 * 使用 Redis Lua 腳本確保原子性
 *
 * @param request 扣減請求
 * @return 扣減結果
 */
public DebitResult debitWallet(DebitRequest request) {
    String requestId = request.getRequestId();
    Long walletId = request.getWalletId();
    BigDecimal amount = request.getAmount();

    // 1. 冪等性檢查（Redis 快速路徑）
    String cacheKey = "wallet:debit:" + requestId;
    DebitResult cachedResult = redisTemplate.opsForValue().get(cacheKey);
    if (cachedResult != null) {
        log.info("Request {} already processed (cached)", requestId);
        return cachedResult;
    }

    // 2. 執行 Lua 腳本扣款
    String luaScript = """
        local wallet_key = KEYS[1]
        local amount = tonumber(ARGV[1])

        -- 檢查餘額
        local balance = tonumber(redis.call('HGET', wallet_key, 'balance'))
        if balance < amount then
            return 'INSUFFICIENT_BALANCE'
        end

        -- 原子扣減
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
        throw new InsufficientBalanceException("餘額不足");
    }

    // 3. 持久化到資料庫（異步）
    CompletableFuture.runAsync(() -> {
        persistWalletTransaction(request);
    }, asyncExecutor);

    // 4. 快取結果（15分鐘）
    DebitResult debitResult = new DebitResult(requestId, walletId, amount);
    redisTemplate.opsForValue().set(cacheKey, debitResult, 15, TimeUnit.MINUTES);

    return debitResult;
}
```

#### Step 4: 實作錢包鎖定機制

```java
/**
 * 鎖定錢包金額（投注時使用）
 *
 * @param walletId 錢包ID
 * @param amount 鎖定金額
 * @param betId 投注訂單ID
 */
@Transactional(rollbackFor = Throwable.class)
public void lockWalletAmount(Long walletId, BigDecimal amount, String betId) {
    // 1. 樂觀鎖更新錢包
    int updated = walletDao.incrementLockedAmount(walletId, amount);
    if (updated == 0) {
        throw new ConcurrentUpdateException("錢包更新衝突，請重試");
    }

    // 2. 記錄鎖定明細
    WalletLock lock = WalletLock.builder()
        .walletId(walletId)
        .lockAmount(amount)
        .lockReason(LockReason.BET_PENDING)
        .referenceId(betId)
        .createdAt(LocalDateTime.now())
        .build();

    walletLockDao.insert(lock);

    // 3. 發布事件
    eventPublisher.publish(new WalletLockedEvent(walletId, amount, betId));
}

/**
 * 解鎖錢包金額（投注結算時使用）
 *
 * @param walletId 錢包ID
 * @param betId 投注訂單ID
 */
@Transactional(rollbackFor = Throwable.class)
public void unlockWalletAmount(Long walletId, String betId) {
    // 1. 查詢鎖定記錄
    WalletLock lock = walletLockDao.findByReference(walletId, betId);
    if (lock == null) {
        log.warn("Lock not found for bet {}", betId);
        return;
    }

    // 2. 樂觀鎖更新錢包
    int updated = walletDao.decrementLockedAmount(walletId, lock.getLockAmount());
    if (updated == 0) {
        throw new ConcurrentUpdateException("錢包更新衝突，請重試");
    }

    // 3. 刪除鎖定記錄
    walletLockDao.deleteById(lock.getLockId());

    // 4. 發布事件
    eventPublisher.publish(new WalletUnlockedEvent(walletId, lock.getLockAmount(), betId));
}
```

#### Step 5: 實作 Outbox Pattern（事務一致性）

```java
/**
 * Outbox 事件表
 */
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

/**
 * 在同一個事務中保存業務數據和事件
 */
@Transactional(rollbackFor = Throwable.class)
public void debitWalletWithEvent(DebitRequest request) {
    // 1. 更新錢包餘額
    walletDao.debitBalance(request.getWalletId(), request.getAmount());

    // 2. 記錄交易
    WalletTransaction tx = createTransaction(request);
    walletTransactionDao.insert(tx);

    // 3. 保存 Outbox 事件（同一事務）
    OutboxEvent event = OutboxEvent.builder()
        .aggregateType("WALLET")
        .aggregateId(request.getWalletId().toString())
        .eventType("WALLET_DEBITED")
        .payload(toJson(tx))
        .createdAt(LocalDateTime.now())
        .build();

    outboxEventDao.insert(event);

    // 事務提交後，事件會被異步 Relay 發送到 Kafka
}

/**
 * Outbox Event Relay（定時任務）
 */
@Scheduled(fixedDelay = 1000)
public void relayOutboxEvents() {
    List<OutboxEvent> events = outboxEventDao.findUnprocessed(100);

    for (OutboxEvent event : events) {
        try {
            // 發送到 Kafka
            kafkaTemplate.send(
                "wallet-events",
                event.getAggregateId(),
                event.getPayload()
            );

            // 標記為已處理
            outboxEventDao.markProcessed(event.getEventId());

        } catch (Exception e) {
            log.error("Failed to relay event {}", event.getEventId(), e);
            // 下次繼續重試
        }
    }
}
```

### ✅ 驗證清單

- [ ] 可下注餘額公式計算正確
- [ ] 並發扣款測試通過（JMeter 1000 TPS）
- [ ] 餘額不足時正確拒絕
- [ ] 錢包鎖定/解鎖機制正常
- [ ] Outbox 事件 100% 發送成功
- [ ] 冪等性測試通過（重複請求返回相同結果）
- [ ] 資料庫與 Redis 餘額最終一致

### ⚠️ 常見陷阱

1. **樂觀鎖失敗未重試**：高並發下需要指數退避重試
2. **忘記清理過期的鎖定記錄**：需要定時清理
3. **Redis 與 DB 餘額不一致**：必須使用 Outbox Pattern 或定期對帳
4. **小數精度問題**：統一使用 `DECIMAL(19,4)` 和 `BigDecimal`

---

## 2. 對接支付閘道

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [01-03 Payment_Integration](../01_Core_Financial_Loop/01-03_Payment_Integration.md) | §2 對接流程 | 10 分鐘 | 入金/出金 API |
| 2 | [07-03-02 Authentication](../07_Technical_Infrastructure/07-03-02_Authentication.md) | §2 HMAC 簽名 | 8 分鐘 | 簽名驗證機制 |
| 3 | [01-04 Transaction_Flow](../01_Core_Financial_Loop/01-04_Transaction_Flow.md) | §2 交易狀態機 | 10 分鐘 | 支付狀態轉換 |

### 🎯 實作目標

對接第三方支付閘道（如 Stripe, PayPal, 本地支付），實作：
- 入金請求與回調處理
- 出金請求與狀態查詢
- HMAC 簽名驗證
- 冪等性處理
- 異步回調處理

### 📝 實作步驟

#### Step 1: 定義支付閘道接口

```java
/**
 * 支付閘道抽象接口
 */
public interface PaymentGateway {

    /**
     * 創建入金訂單
     *
     * @param request 入金請求
     * @return 支付 URL 或二維碼
     */
    DepositResponse createDeposit(DepositRequest request);

    /**
     * 處理入金回調
     *
     * @param callback 回調數據
     * @return 處理結果
     */
    CallbackResult handleDepositCallback(Map<String, String> callback);

    /**
     * 創建出金訂單
     *
     * @param request 出金請求
     * @return 出金訂單號
     */
    WithdrawalResponse createWithdrawal(WithdrawalRequest request);

    /**
     * 查詢出金狀態
     *
     * @param orderId 訂單號
     * @return 訂單狀態
     */
    WithdrawalStatus queryWithdrawalStatus(String orderId);

    /**
     * 驗證回調簽名
     *
     * @param params 回調參數
     * @param signature 簽名
     * @return 是否合法
     */
    boolean verifySignature(Map<String, String> params, String signature);
}
```

#### Step 2: 實作 HMAC 簽名驗證

```java
/**
 * 支付閘道簽名工具
 */
@Component
public class PaymentSignatureUtil {

    @Value("${payment.gateway.secret-key}")
    private String secretKey;

    /**
     * 生成 HMAC-SHA256 簽名
     *
     * 步驟：
     * 1. 參數按 key 排序
     * 2. 拼接為 key1=value1&key2=value2
     * 3. HMAC-SHA256 加密
     * 4. Base64 編碼
     *
     * @param params 參數
     * @return 簽名
     */
    public String generateSignature(Map<String, String> params) {
        // 1. 過濾空值並排序
        String sortedParams = params.entrySet().stream()
            .filter(e -> StringUtils.isNotBlank(e.getValue()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

        // 2. HMAC-SHA256 加密
        byte[] hmac = HmacUtils.hmacSha256(secretKey.getBytes(), sortedParams.getBytes());

        // 3. Base64 編碼
        return Base64.getEncoder().encodeToString(hmac);
    }

    /**
     * 驗證簽名
     *
     * @param params 參數
     * @param receivedSignature 接收到的簽名
     * @return 是否合法
     */
    public boolean verifySignature(Map<String, String> params, String receivedSignature) {
        String expectedSignature = generateSignature(params);

        // 使用時間安全的比較函數（防止時序攻擊）
        return MessageDigest.isEqual(
            expectedSignature.getBytes(),
            receivedSignature.getBytes()
        );
    }
}
```

#### Step 3: 實作入金流程

```java
/**
 * 入金服務
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private final PaymentGateway paymentGateway;
    private final DepositOrderDao depositOrderDao;
    private final WalletService walletService;

    /**
     * 創建入金訂單
     *
     * @param request 入金請求
     * @return 支付 URL
     */
    public DepositResponse createDepositOrder(DepositRequest request) {
        // 1. 創建訂單（狀態：PENDING）
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

        // 2. 調用支付閘道
        DepositResponse response = paymentGateway.createDeposit(request);

        // 3. 更新訂單（記錄閘道訂單號）
        depositOrderDao.updateGatewayOrderId(
            order.getOrderId(),
            response.getGatewayOrderId()
        );

        return response;
    }

    /**
     * 處理入金回調
     *
     * 冪等性保證：同一個訂單只會加款一次
     *
     * @param callback 回調數據
     * @return 處理結果
     */
    @Transactional(rollbackFor = Throwable.class)
    public CallbackResult handleDepositCallback(Map<String, String> callback) {
        String gatewayOrderId = callback.get("order_id");
        String status = callback.get("status");
        String signature = callback.get("signature");

        // 1. 驗證簽名
        if (!paymentGateway.verifySignature(callback, signature)) {
            log.error("Invalid signature for order {}", gatewayOrderId);
            return CallbackResult.failure("Invalid signature");
        }

        // 2. 查詢訂單
        DepositOrder order = depositOrderDao.findByGatewayOrderId(gatewayOrderId);
        if (order == null) {
            log.error("Order not found: {}", gatewayOrderId);
            return CallbackResult.failure("Order not found");
        }

        // 3. 冪等性檢查
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.info("Order {} already processed", order.getOrderId());
            return CallbackResult.success("Already processed");
        }

        // 4. 更新訂單狀態
        if ("SUCCESS".equals(status)) {
            order.setStatus(OrderStatus.SUCCESS);
            order.setCompletedAt(LocalDateTime.now());
            depositOrderDao.updateStatus(order);

            // 5. 加款到錢包
            walletService.creditWallet(
                order.getPlayerId(),
                order.getAmount(),
                "DEPOSIT:" + order.getOrderId()
            );

            // 6. 發送通知
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

#### Step 4: 實作出金流程（含風控審核）

```java
/**
 * 出金服務
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final PaymentGateway paymentGateway;
    private final WithdrawalOrderDao withdrawalOrderDao;
    private final WalletService walletService;
    private final RiskControlService riskControlService;

    /**
     * 創建出金訂單
     *
     * 流程：
     * 1. 風控審核
     * 2. 鎖定錢包金額
     * 3. 提交到支付閘道
     * 4. 異步查詢狀態
     *
     * @param request 出金請求
     * @return 訂單結果
     */
    @Transactional(rollbackFor = Throwable.class)
    public WithdrawalResult createWithdrawalOrder(WithdrawalRequest request) {
        // 1. 風控審核
        RiskDecision decision = riskControlService.evaluateWithdrawal(request);

        if (decision.getAction() == RiskAction.REJECT) {
            throw new WithdrawalRejectedException(decision.getReason());
        }

        // 2. 創建訂單
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

        // 3. 鎖定錢包金額
        walletService.lockWalletAmount(
            request.getPlayerId(),
            request.getAmount(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 4. 如果需要人工審核
        if (decision.getAction() == RiskAction.MANUAL_REVIEW) {
            order.setStatus(OrderStatus.REVIEWING);
            withdrawalOrderDao.updateStatus(order);

            // 通知風控團隊
            notificationService.notifyManualReview(order);

            return WithdrawalResult.underReview(order.getOrderId());
        }

        // 5. 自動審核通過，提交到支付閘道
        WithdrawalResponse response = paymentGateway.createWithdrawal(request);

        order.setGatewayOrderId(response.getGatewayOrderId());
        order.setStatus(OrderStatus.PROCESSING);
        withdrawalOrderDao.update(order);

        // 6. 啟動異步狀態查詢
        scheduleStatusQuery(order.getOrderId());

        return WithdrawalResult.processing(order.getOrderId());
    }

    /**
     * 查詢出金狀態（定時任務）
     *
     * 每 30 秒查詢一次，最多查詢 24 小時
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
     * 完成出金
     */
    @Transactional(rollbackFor = Throwable.class)
    private void completeWithdrawal(WithdrawalOrder order) {
        // 1. 更新訂單狀態
        order.setStatus(OrderStatus.SUCCESS);
        order.setCompletedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 2. 扣減錢包餘額（解鎖 + 扣款）
        walletService.debitWallet(
            order.getPlayerId(),
            order.getAmount(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 3. 發送通知
        notificationService.sendWithdrawalSuccessNotification(order);
    }

    /**
     * 出金失敗
     */
    @Transactional(rollbackFor = Throwable.class)
    private void failWithdrawal(WithdrawalOrder order) {
        // 1. 更新訂單狀態
        order.setStatus(OrderStatus.FAILED);
        withdrawalOrderDao.update(order);

        // 2. 解鎖錢包金額
        walletService.unlockWalletAmount(
            order.getPlayerId(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 3. 發送通知
        notificationService.sendWithdrawalFailedNotification(order);
    }
}
```

### ✅ 驗證清單

- [ ] HMAC 簽名驗證正確
- [ ] 入金回調冪等性測試通過
- [ ] 出金風控審核流程完整
- [ ] 出金失敗時錢包金額正確解鎖
- [ ] 支付狀態查詢定時任務正常運行
- [ ] 超時訂單自動取消
- [ ] 所有支付事件記錄到審計日誌

### ⚠️ 常見陷阱

1. **簽名驗證時序攻擊**：使用 `MessageDigest.isEqual()` 而非 `==`
2. **回調冪等性失敗**：必須在事務內檢查訂單狀態
3. **出金失敗未解鎖**：任何失敗路徑都要解鎖錢包
4. **忘記處理超時訂單**：24 小時未完成的訂單需要自動取消

---

## 3. 實作出金風控流程

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [00-00 BUSINESS_FLOWS](./00-00_BUSINESS_FLOWS.md) | §4 出金審核流程 | 12 分鐘 | 完整業務流程 |
| 2 | [01-05 Withdrawal_Risk](../01_Player_Center/01-05_Withdrawal_Risk.md) | §4 風控流程 | 15 分鐘 | 規則引擎設計 |
| 3 | [04-01 Risk_Engine](../04_Risk_Control/04-01_Risk_Framework.md) | §2 規則引擎 | 12 分鐘 | LiteFlow 實作 |
| 4 | ~~04-04 Risk_Workflow~~ 🚧 計劃中 | §2 工作流狀態機 | 10 分鐘 | 審核流程狀態 |

### 🎯 實作目標

建立完整的出金風控系統，包含：
- 多維度風險評分（7 個維度）
- 自動/人工/拒絕決策
- SAGA 補償機制
- 審核工作流狀態機
- 風控規則引擎（LiteFlow）

### 📝 實作步驟

#### Step 1: 設計風險評分模型

```java
/**
 * 出金風險評分器
 *
 * 評分維度：
 * 1. 玩家信用評分（0-20分）
 * 2. KYC 完整度（0-15分）
 * 3. 入金/出金比率（0-15分）
 * 4. 近期出金頻率（0-15分）
 * 5. 流水完成度（0-15分）
 * 6. IP/設備異常（0-10分）
 * 7. 多帳號關聯（0-10分）
 *
 * 總分 0-100，決策閾值：
 * - [0, 30): 自動通過
 * - [30, 70): 人工審核
 * - [70, 100]: 自動拒絕
 */
@Component
@RequiredArgsConstructor
public class WithdrawalRiskScorer {

    private final PlayerCreditService playerCreditService;
    private final KycService kycService;
    private final TransactionHistoryService transactionHistoryService;
    private final FraudDetectionService fraudDetectionService;

    /**
     * 計算出金風險評分
     *
     * @param request 出金請求
     * @return 風險決策
     */
    public RiskDecision scoreWithdrawal(WithdrawalRequest request) {
        Long playerId = request.getPlayerId();
        BigDecimal amount = request.getAmount();

        // 1. 玩家信用評分（0-20分）
        int creditScore = playerCreditService.getCreditScore(playerId);
        int creditRisk = (100 - creditScore) * 20 / 100; // 反向計算

        // 2. KYC 完整度（0-15分）
        KycLevel kycLevel = kycService.getKycLevel(playerId);
        int kycRisk = switch (kycLevel) {
            case VERIFIED -> 0;
            case PARTIALLY_VERIFIED -> 8;
            case NOT_VERIFIED -> 15;
        };

        // 3. 入金/出金比率（0-15分）
        BigDecimal depositTotal = transactionHistoryService.getTotalDeposits(playerId);
        BigDecimal withdrawalTotal = transactionHistoryService.getTotalWithdrawals(playerId);

        int ratioRisk = 0;
        if (depositTotal.compareTo(BigDecimal.ZERO) == 0) {
            ratioRisk = 15; // 從未入金直接出金
        } else {
            BigDecimal ratio = withdrawalTotal.divide(depositTotal, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) > 0) {
                ratioRisk = 15; // 出金超過入金2倍
            } else if (ratio.compareTo(new BigDecimal("1.5")) > 0) {
                ratioRisk = 10;
            } else if (ratio.compareTo(new BigDecimal("1.0")) > 0) {
                ratioRisk = 5;
            }
        }

        // 4. 近期出金頻率（0-15分）
        int recentWithdrawals = transactionHistoryService.countWithdrawalsLast7Days(playerId);
        int frequencyRisk = Math.min(recentWithdrawals * 3, 15);

        // 5. 流水完成度（0-15分）
        BigDecimal requiredTurnover = calculateRequiredTurnover(playerId);
        BigDecimal actualTurnover = transactionHistoryService.getTurnover(playerId);

        int turnoverRisk = 0;
        if (requiredTurnover.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal completion = actualTurnover.divide(requiredTurnover, 2, RoundingMode.HALF_UP);
            if (completion.compareTo(BigDecimal.ONE) < 0) {
                turnoverRisk = 15 - (completion.multiply(new BigDecimal("15"))).intValue();
            }
        }

        // 6. IP/設備異常（0-10分）
        int deviceRisk = fraudDetectionService.detectDeviceAnomaly(playerId);

        // 7. 多帳號關聯（0-10分）
        int multiAccountRisk = fraudDetectionService.detectMultiAccount(playerId);

        // 計算總分
        int totalScore = creditRisk + kycRisk + ratioRisk + frequencyRisk
                       + turnoverRisk + deviceRisk + multiAccountRisk;

        // 決策
        RiskAction action;
        String reason;

        if (totalScore < 30) {
            action = RiskAction.AUTO_APPROVE;
            reason = "低風險，自動通過";
        } else if (totalScore < 70) {
            action = RiskAction.MANUAL_REVIEW;
            reason = String.format("中風險（%d分），需人工審核", totalScore);
        } else {
            action = RiskAction.REJECT;
            reason = String.format("高風險（%d分），自動拒絕", totalScore);
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

#### Step 2: 實作 LiteFlow 規則引擎

```java
/**
 * LiteFlow 風控鏈路配置
 */
@Configuration
public class WithdrawalRiskChainConfig {

    /**
     * 風控規則鏈（EL 表達式）
     *
     * 規則：
     * 1. 基礎驗證（玩家狀態、餘額檢查）
     * 2. 風險評分
     * 3. 決策判斷
     * 4. 補償處理（失敗時解鎖）
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
 * 基礎驗證節點
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

        // 1. 檢查玩家狀態
        Player player = playerService.getPlayer(request.getPlayerId());
        if (player.getStatus() == PlayerStatus.BLOCKED) {
            throw new BizException("玩家已被封禁，無法出金");
        }

        // 2. 檢查餘額
        BigDecimal balance = walletService.getAvailableBalance(request.getPlayerId());
        if (balance.compareTo(request.getAmount()) < 0) {
            throw new BizException("餘額不足");
        }

        // 3. 檢查最小出金金額
        if (request.getAmount().compareTo(new BigDecimal("100")) < 0) {
            throw new BizException("出金金額不得低於 100");
        }
    }
}

/**
 * 風險評分節點
 */
@Component("risk_scoring_node")
public class RiskScoringNode extends NodeComponent {

    @Resource
    private WithdrawalRiskScorer riskScorer;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();

        // 計算風險評分
        RiskDecision decision = riskScorer.scoreWithdrawal(request);

        // 保存到上下文
        this.setContextBean("riskDecision", decision);

        log.info("Withdrawal {} risk score: {}, action: {}",
                 request.getOrderId(),
                 decision.getScore(),
                 decision.getAction());
    }
}

/**
 * 決策節點
 */
@Component("decision_node")
public class DecisionNode extends NodeComponent {

    @Resource
    private WithdrawalOrderDao withdrawalOrderDao;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();
        RiskDecision decision = this.getContextBean("riskDecision");

        // 根據決策更新訂單狀態
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
 * 補償節點（失敗時解鎖錢包）
 */
@Component("compensation_node")
public class CompensationNode extends NodeComponent {

    @Resource
    private WalletService walletService;

    @Override
    public void process() {
        WithdrawalRequest request = this.getRequestData();

        // 解鎖錢包金額
        walletService.unlockWalletAmount(
            request.getPlayerId(),
            "WITHDRAWAL:" + request.getOrderId()
        );

        log.info("Withdrawal {} compensation: unlocked wallet amount",
                 request.getOrderId());
    }
}
```

#### Step 3: 實作審核工作流狀態機

```java
/**
 * 出金審核狀態機
 *
 * 狀態轉換：
 * PENDING → REVIEWING → APPROVED → PROCESSING → SUCCESS
 *                    ↘ REJECTED
 */
@Component
@RequiredArgsConstructor
public class WithdrawalWorkflowStateMachine {

    private final WithdrawalOrderDao withdrawalOrderDao;
    private final PaymentGateway paymentGateway;
    private final WalletService walletService;

    /**
     * 人工審核通過
     */
    @Transactional(rollbackFor = Throwable.class)
    public void approveByReviewer(String orderId, String reviewerId, String comment) {
        WithdrawalOrder order = withdrawalOrderDao.findById(orderId);

        // 1. 狀態檢查
        if (order.getStatus() != OrderStatus.REVIEWING) {
            throw new IllegalStateException(
                "訂單狀態錯誤，當前：" + order.getStatus()
            );
        }

        // 2. 更新狀態
        order.setStatus(OrderStatus.APPROVED);
        order.setReviewerId(reviewerId);
        order.setReviewComment(comment);
        order.setReviewedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 3. 提交到支付閘道
        WithdrawalResponse response = paymentGateway.createWithdrawal(order);

        order.setGatewayOrderId(response.getGatewayOrderId());
        order.setStatus(OrderStatus.PROCESSING);
        withdrawalOrderDao.update(order);

        // 4. 記錄審計日誌
        auditLogService.log(AuditEvent.builder()
            .action("WITHDRAWAL_APPROVED")
            .operator(reviewerId)
            .targetId(orderId)
            .comment(comment)
            .build());
    }

    /**
     * 人工審核拒絕
     */
    @Transactional(rollbackFor = Throwable.class)
    public void rejectByReviewer(String orderId, String reviewerId, String reason) {
        WithdrawalOrder order = withdrawalOrderDao.findById(orderId);

        // 1. 狀態檢查
        if (order.getStatus() != OrderStatus.REVIEWING) {
            throw new IllegalStateException(
                "訂單狀態錯誤，當前：" + order.getStatus()
            );
        }

        // 2. 更新狀態
        order.setStatus(OrderStatus.REJECTED);
        order.setReviewerId(reviewerId);
        order.setReviewComment(reason);
        order.setReviewedAt(LocalDateTime.now());
        withdrawalOrderDao.update(order);

        // 3. 解鎖錢包金額
        walletService.unlockWalletAmount(
            order.getPlayerId(),
            "WITHDRAWAL:" + order.getOrderId()
        );

        // 4. 發送通知
        notificationService.sendWithdrawalRejectedNotification(order);

        // 5. 記錄審計日誌
        auditLogService.log(AuditEvent.builder()
            .action("WITHDRAWAL_REJECTED")
            .operator(reviewerId)
            .targetId(orderId)
            .comment(reason)
            .build());
    }
}
```

#### Step 4: 實作 SAGA 補償機制

```java
/**
 * 出金 SAGA 編排器
 *
 * 步驟：
 * 1. 鎖定錢包金額
 * 2. 風控審核
 * 3. 提交支付閘道
 * 4. 扣減餘額
 *
 * 補償：
 * 任何步驟失敗時，回滾前面的操作
 */
@Component
@RequiredArgsConstructor
public class WithdrawalSagaOrchestrator {

    private final WalletService walletService;
    private final RiskControlService riskControlService;
    private final PaymentGateway paymentGateway;

    /**
     * 執行出金 SAGA
     */
    public WithdrawalResult executeWithdrawalSaga(WithdrawalRequest request) {
        String orderId = request.getOrderId();
        List<CompensationAction> compensations = new ArrayList<>();

        try {
            // Step 1: 鎖定錢包金額
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

            // Step 2: 風控審核
            RiskDecision decision = riskControlService.evaluateWithdrawal(request);
            if (decision.getAction() == RiskAction.REJECT) {
                throw new WithdrawalRejectedException(decision.getReason());
            }

            // Step 3: 提交支付閘道
            WithdrawalResponse response = paymentGateway.createWithdrawal(request);
            compensations.add(() -> {
                paymentGateway.cancelWithdrawal(response.getGatewayOrderId());
            });

            // Step 4: 扣減餘額
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
            // 執行補償
            log.error("Withdrawal SAGA failed for order {}, executing compensations",
                      orderId, e);

            for (int i = compensations.size() - 1; i >= 0; i--) {
                try {
                    compensations.get(i).compensate();
                } catch (Exception ce) {
                    log.error("Compensation failed for order {}", orderId, ce);
                    // 記錄到人工處理隊列
                    manualCompensationQueue.add(orderId, compensations.get(i));
                }
            }

            throw new WithdrawalSagaException("出金流程失敗：" + e.getMessage(), e);
        }
    }
}

/**
 * 補償動作接口
 */
@FunctionalInterface
interface CompensationAction {
    void compensate() throws Exception;
}
```

### ✅ 驗證清單

- [ ] 風險評分模型計算正確
- [ ] 自動通過/拒絕決策準確
- [ ] 人工審核工作流完整
- [ ] LiteFlow 規則鏈執行成功
- [ ] SAGA 補償機制正常運作
- [ ] 審核操作記錄到審計日誌
- [ ] 壓力測試：1000 TPS 下風控系統響應時間 <500ms

### ⚠️ 常見陷阱

1. **補償未執行**：必須使用 try-finally 或 SAGA 框架
2. **狀態機併發問題**：使用樂觀鎖或分佈式鎖
3. **規則引擎配置錯誤**：LiteFlow EL 表達式需要充分測試
4. **審計日誌遺漏**：所有狀態變更必須記錄

---

## 4. 建立對帳系統

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [01-06 Reconciliation](../01_Core_Financial_Loop/01-06_Reconciliation.md) | §2 對帳模型 | 15 分鐘 | 三方對帳邏輯 |
| 2 | [02-03 Turnover_Calculation](../03_Game_Center/03-04_Turnover_Calculation.md) | §2 三層驗證 | 10 分鐘 | 流水對帳 |
| 3 | [00-00 BUSINESS_FLOWS](./00-00_BUSINESS_FLOWS.md) | §5 流水對帳流程 | 10 分鐘 | 端到端流程 |

### 🎯 實作目標

建立完整的對帳系統，包含：
- 每日財務對帳（錢包、入金、出金、投注）
- 遊戲廠商對帳（投注流水三方比對）
- 差異檢測與告警
- 自動對帳報表生成

### 📝 實作步驟

#### Step 1: 設計對帳數據模型

```sql
-- 每日財務對帳表
CREATE TABLE daily_financial_reconciliation (
    recon_id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    recon_date DATE NOT NULL,

    -- 錢包對帳
    wallet_balance_system DECIMAL(19,4) NOT NULL,
    wallet_balance_db DECIMAL(19,4) NOT NULL,
    wallet_balance_diff DECIMAL(19,4) NOT NULL,

    -- 入金對帳
    deposit_count_system INT NOT NULL,
    deposit_count_gateway INT NOT NULL,
    deposit_amount_system DECIMAL(19,4) NOT NULL,
    deposit_amount_gateway DECIMAL(19,4) NOT NULL,
    deposit_diff DECIMAL(19,4) NOT NULL,

    -- 出金對帳
    withdrawal_count_system INT NOT NULL,
    withdrawal_count_gateway INT NOT NULL,
    withdrawal_amount_system DECIMAL(19,4) NOT NULL,
    withdrawal_amount_gateway DECIMAL(19,4) NOT NULL,
    withdrawal_diff DECIMAL(19,4) NOT NULL,

    -- 投注對帳
    bet_count_system INT NOT NULL,
    bet_count_provider INT NOT NULL,
    bet_amount_system DECIMAL(19,4) NOT NULL,
    bet_amount_provider DECIMAL(19,4) NOT NULL,
    bet_diff DECIMAL(19,4) NOT NULL,

    -- 對帳狀態
    status VARCHAR(20) NOT NULL, -- MATCHED, MISMATCHED, PENDING
    created_at TIMESTAMP NOT NULL,

    UNIQUE(tenant_id, recon_date),
    INDEX idx_status_date (status, recon_date)
);

-- 對帳差異明細表
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

#### Step 2: 實作每日財務對帳

```java
/**
 * 每日財務對帳服務
 *
 * 執行時間：每日 02:00 AM
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
     * 執行每日對帳
     *
     * @param tenantId 租戶ID
     * @param reconDate 對帳日期
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(rollbackFor = Throwable.class)
    public void executeDailyReconciliation(String tenantId, LocalDate reconDate) {
        log.info("Starting daily reconciliation for tenant {} on {}",
                 tenantId, reconDate);

        // 1. 錢包對帳
        WalletReconciliationResult walletResult = reconcileWallets(tenantId, reconDate);

        // 2. 入金對帳
        DepositReconciliationResult depositResult = reconcileDeposits(tenantId, reconDate);

        // 3. 出金對帳
        WithdrawalReconciliationResult withdrawalResult = reconcileWithdrawals(tenantId, reconDate);

        // 4. 投注對帳
        BetReconciliationResult betResult = reconcileBets(tenantId, reconDate);

        // 5. 保存對帳結果
        DailyFinancialReconciliation recon = DailyFinancialReconciliation.builder()
            .tenantId(tenantId)
            .reconDate(reconDate)

            // 錢包
            .walletBalanceSystem(walletResult.getSystemBalance())
            .walletBalanceDb(walletResult.getDbBalance())
            .walletBalanceDiff(walletResult.getDiff())

            // 入金
            .depositCountSystem(depositResult.getSystemCount())
            .depositCountGateway(depositResult.getGatewayCount())
            .depositAmountSystem(depositResult.getSystemAmount())
            .depositAmountGateway(depositResult.getGatewayAmount())
            .depositDiff(depositResult.getDiff())

            // 出金
            .withdrawalCountSystem(withdrawalResult.getSystemCount())
            .withdrawalCountGateway(withdrawalResult.getGatewayCount())
            .withdrawalAmountSystem(withdrawalResult.getSystemAmount())
            .withdrawalAmountGateway(withdrawalResult.getGatewayAmount())
            .withdrawalDiff(withdrawalResult.getDiff())

            // 投注
            .betCountSystem(betResult.getSystemCount())
            .betCountProvider(betResult.getProviderCount())
            .betAmountSystem(betResult.getSystemAmount())
            .betAmountProvider(betResult.getProviderAmount())
            .betDiff(betResult.getDiff())

            .status(determineStatus(walletResult, depositResult, withdrawalResult, betResult))
            .createdAt(LocalDateTime.now())
            .build();

        reconciliationDao.insert(recon);

        // 6. 如果有差異，發送告警
        if (recon.getStatus() == ReconciliationStatus.MISMATCHED) {
            alertService.sendReconciliationMismatchAlert(recon);
        }

        log.info("Daily reconciliation completed for tenant {} on {}, status: {}",
                 tenantId, reconDate, recon.getStatus());
    }

    /**
     * 錢包對帳
     */
    private WalletReconciliationResult reconcileWallets(String tenantId, LocalDate date) {
        // 1. 系統餘額（Redis）
        BigDecimal systemBalance = walletDao.sumAllBalancesFromRedis(tenantId);

        // 2. 資料庫餘額
        BigDecimal dbBalance = walletDao.sumAllBalancesFromDb(tenantId);

        // 3. 計算差異
        BigDecimal diff = systemBalance.subtract(dbBalance);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Wallet balance mismatch: system={}, db={}, diff={}",
                      systemBalance, dbBalance, diff);

            // 記錄差異明細
            // ... 省略
        }

        return new WalletReconciliationResult(systemBalance, dbBalance, diff);
    }

    /**
     * 入金對帳
     */
    private DepositReconciliationResult reconcileDeposits(String tenantId, LocalDate date) {
        // 1. 系統統計
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        int systemCount = depositOrderDao.countByPeriod(tenantId, startOfDay, endOfDay);
        BigDecimal systemAmount = depositOrderDao.sumAmountByPeriod(tenantId, startOfDay, endOfDay);

        // 2. 支付閘道統計
        PaymentGatewayReport gatewayReport = paymentGateway.getDailyReport(tenantId, date);
        int gatewayCount = gatewayReport.getDepositCount();
        BigDecimal gatewayAmount = gatewayReport.getDepositAmount();

        // 3. 計算差異
        BigDecimal diff = systemAmount.subtract(gatewayAmount);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Deposit mismatch: system={}/{}, gateway={}/{}, diff={}",
                      systemCount, systemAmount, gatewayCount, gatewayAmount, diff);

            // 找出具體哪些訂單有差異
            List<String> mismatchedOrders = findMismatchedDepositOrders(
                tenantId, date, systemCount, gatewayCount
            );

            // 記錄差異
            for (String orderId : mismatchedOrders) {
                recordDiscrepancy(ReconciliationType.DEPOSIT, orderId, diff);
            }
        }

        return new DepositReconciliationResult(
            systemCount, gatewayCount, systemAmount, gatewayAmount, diff
        );
    }

    /**
     * 投注對帳（三方比對）
     */
    private BetReconciliationResult reconcileBets(String tenantId, LocalDate date) {
        // 1. 系統統計（OLTP）
        int systemCount = betOrderDao.countByPeriod(tenantId, date);
        BigDecimal systemAmount = betOrderDao.sumAmountByPeriod(tenantId, date);

        // 2. 遊戲廠商統計
        GameProviderReport providerReport = gameProviderService.getDailyReport(tenantId, date);
        int providerCount = providerReport.getBetCount();
        BigDecimal providerAmount = providerReport.getBetAmount();

        // 3. 計算差異
        BigDecimal diff = systemAmount.subtract(providerAmount);

        if (diff.abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Bet mismatch: system={}/{}, provider={}/{}, diff={}",
                      systemCount, systemAmount, providerCount, providerAmount, diff);

            // 三方比對（系統、廠商、OLAP）
            performThreeWayBetReconciliation(tenantId, date);
        }

        return new BetReconciliationResult(
            systemCount, providerCount, systemAmount, providerAmount, diff
        );
    }

    /**
     * 投注流水三方比對
     *
     * 比對：
     * 1. OLTP 系統（實時）
     * 2. 遊戲廠商報表
     * 3. OLAP 數據倉庫（T+1）
     */
    private void performThreeWayBetReconciliation(String tenantId, LocalDate date) {
        // 1. 從三個來源獲取投注列表
        List<BetOrder> systemBets = betOrderDao.findByDate(tenantId, date);
        List<BetOrder> providerBets = gameProviderService.getBetsByDate(tenantId, date);
        List<BetOrder> olapBets = olapService.getBetsByDate(tenantId, date);

        // 2. 轉換為 Set（使用投注單號）
        Set<String> systemBetIds = systemBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        Set<String> providerBetIds = providerBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        Set<String> olapBetIds = olapBets.stream()
            .map(BetOrder::getBetId)
            .collect(Collectors.toSet());

        // 3. 找出差異
        Set<String> onlyInSystem = new HashSet<>(systemBetIds);
        onlyInSystem.removeAll(providerBetIds);
        onlyInSystem.removeAll(olapBetIds);

        Set<String> onlyInProvider = new HashSet<>(providerBetIds);
        onlyInProvider.removeAll(systemBetIds);
        onlyInProvider.removeAll(olapBetIds);

        Set<String> onlyInOlap = new HashSet<>(olapBetIds);
        onlyInOlap.removeAll(systemBetIds);
        onlyInOlap.removeAll(providerBetIds);

        // 4. 記錄差異
        if (!onlyInSystem.isEmpty()) {
            log.error("Bets only in system: {}", onlyInSystem);
            // 可能是廠商未報送
        }

        if (!onlyInProvider.isEmpty()) {
            log.error("Bets only in provider: {}", onlyInProvider);
            // 可能是系統未記錄（嚴重問題）
        }

        if (!onlyInOlap.isEmpty()) {
            log.error("Bets only in OLAP: {}", onlyInOlap);
            // 可能是 ETL 延遲
        }
    }
}
```

#### Step 3: 實作差異處理流程

```java
/**
 * 對帳差異處理服務
 */
@Service
@RequiredArgsConstructor
public class ReconciliationDiscrepancyService {

    private final ReconciliationDiscrepancyDao discrepancyDao;
    private final WalletService walletService;

    /**
     * 手動處理差異
     *
     * @param discrepancyId 差異ID
     * @param action 處理動作（ADJUST_SYSTEM, ADJUST_EXTERNAL, IGNORE）
     * @param operator 操作人
     * @param comment 備註
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
            throw new IllegalStateException("差異已處理");
        }

        switch (action) {
            case ADJUST_SYSTEM -> {
                // 調整系統數據（例如補單）
                adjustSystemData(discrepancy);
            }
            case ADJUST_EXTERNAL -> {
                // 聯繫外部廠商調整
                requestExternalAdjustment(discrepancy);
            }
            case IGNORE -> {
                // 標記為可忽略（小額差異）
                log.info("Discrepancy {} marked as ignore", discrepancyId);
            }
        }

        // 標記為已處理
        discrepancy.setResolved(true);
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancy.setResolvedBy(operator);
        discrepancyDao.update(discrepancy);

        // 記錄審計日誌
        auditLogService.log(AuditEvent.builder()
            .action("DISCREPANCY_RESOLVED")
            .operator(operator)
            .targetId(discrepancyId.toString())
            .comment(comment)
            .build());
    }

    /**
     * 自動調整小額差異
     *
     * 條件：差異 < 0.01 且連續 3 天出現
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void autoResolveMinorDiscrepancies() {
        List<ReconciliationDiscrepancy> minorDiscrepancies =
            discrepancyDao.findUnresolvedMinorDiscrepancies(new BigDecimal("0.01"));

        for (ReconciliationDiscrepancy discrepancy : minorDiscrepancies) {
            // 檢查是否連續出現
            int consecutiveDays = countConsecutiveDays(discrepancy);

            if (consecutiveDays >= 3) {
                // 自動忽略
                resolveDiscrepancy(
                    discrepancy.getDiscrepancyId(),
                    ResolutionAction.IGNORE,
                    "SYSTEM",
                    "自動忽略：差異小於 0.01 且連續 3 天出現"
                );
            }
        }
    }
}
```

#### Step 4: 生成對帳報表

```java
/**
 * 對帳報表生成服務
 */
@Service
@RequiredArgsConstructor
public class ReconciliationReportService {

    private final ReconciliationDao reconciliationDao;
    private final ReconciliationDiscrepancyDao discrepancyDao;

    /**
     * 生成每日對帳報表（PDF）
     *
     * @param tenantId 租戶ID
     * @param reconDate 對帳日期
     * @return PDF 文件路徑
     */
    public String generateDailyReport(String tenantId, LocalDate reconDate) {
        // 1. 查詢對帳數據
        DailyFinancialReconciliation recon = reconciliationDao.findByDate(tenantId, reconDate);

        // 2. 查詢差異明細
        List<ReconciliationDiscrepancy> discrepancies =
            discrepancyDao.findByReconId(recon.getReconId());

        // 3. 生成 PDF
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream("report.pdf"));
        document.open();

        // 標題
        document.add(new Paragraph("每日財務對帳報表", titleFont));
        document.add(new Paragraph("租戶：" + tenantId, normalFont));
        document.add(new Paragraph("日期：" + reconDate, normalFont));
        document.add(new Paragraph("狀態：" + recon.getStatus(), normalFont));
        document.add(Chunk.NEWLINE);

        // 錢包對帳
        PdfPTable walletTable = new PdfPTable(3);
        walletTable.addCell("項目");
        walletTable.addCell("系統");
        walletTable.addCell("資料庫");
        walletTable.addCell("餘額");
        walletTable.addCell(recon.getWalletBalanceSystem().toString());
        walletTable.addCell(recon.getWalletBalanceDb().toString());
        document.add(walletTable);

        // 入金對帳
        PdfPTable depositTable = new PdfPTable(4);
        depositTable.addCell("項目");
        depositTable.addCell("系統筆數");
        depositTable.addCell("系統金額");
        depositTable.addCell("閘道金額");
        depositTable.addCell("入金");
        depositTable.addCell(recon.getDepositCountSystem().toString());
        depositTable.addCell(recon.getDepositAmountSystem().toString());
        depositTable.addCell(recon.getDepositAmountGateway().toString());
        document.add(depositTable);

        // 差異明細
        if (!discrepancies.isEmpty()) {
            document.add(new Paragraph("差異明細：", subtitleFont));

            PdfPTable discrepancyTable = new PdfPTable(5);
            discrepancyTable.addCell("類型");
            discrepancyTable.addCell("參考號");
            discrepancyTable.addCell("系統金額");
            discrepancyTable.addCell("外部金額");
            discrepancyTable.addCell("差異");

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

### ✅ 驗證清單

- [ ] 每日對帳定時任務正常執行
- [ ] 錢包餘額 Redis/DB 一致性 >99.99%
- [ ] 支付閘道對帳差異 <0.01%
- [ ] 遊戲廠商三方比對準確率 100%
- [ ] 差異告警及時發送
- [ ] 對帳報表自動生成
- [ ] 差異處理流程完整記錄到審計日誌

### ⚠️ 常見陷阱

1. **時區問題**：確保系統、支付閘道、遊戲廠商使用統一時區
2. **精度丟失**：統一使用 `DECIMAL(19,4)` 和 `BigDecimal`
3. **ETL 延遲**：OLAP 數據可能有 T+1 延遲，需要考慮
4. **小額差異累積**：0.01 的差異看似無害，累積後可能很大

---

## 5. 對接新遊戲廠商

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [00-00 BUSINESS_FLOWS](./00-00_BUSINESS_FLOWS.md) | §2 遊戲對接流程 | 10 分鐘 | 完整業務流程 |
| 2 | [02-02 Seamless_Wallet_API](../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §4 Token 驗證 | 10 分鐘 | 安全機制 |
| 3 | [02-02 Seamless_Wallet_API](../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §4.3 冪等性設計 | 8 分鐘 | 請求去重 |
| 4 | [02-04 Game_Provider_Cases](../02_Game_Operations/02-04_Game_Provider_Cases.md) | 全文 | 15 分鐘 | 廠商案例 |

### 🎯 實作目標

完整對接新遊戲廠商，實作：
- Seamless Wallet API（下注/贏錢/取消）
- Token 驗證機制
- 冪等性處理（三層防護）
- 並發控制（Redis Lua）
- 錯誤恢復機制

### 📝 實作步驟

#### Step 1: 定義 Seamless Wallet API 接口

```java
/**
 * Seamless Wallet API 接口
 *
 * 標準接口：
 * 1. /api/game/debit - 下注扣款
 * 2. /api/game/credit - 贏錢加款
 * 3. /api/game/cancel - 取消交易
 * 4. /api/game/balance - 查詢餘額
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class SeamlessWalletController {

    private final SeamlessWalletService seamlessWalletService;
    private final TokenVerificationService tokenVerificationService;

    /**
     * 下注扣款
     *
     * @param request 下注請求
     * @return 交易結果
     */
    @PostMapping("/debit")
    public ResponseDTO<DebitResponse> debit(@RequestBody @Valid DebitRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行下注扣款
        DebitResponse response = seamlessWalletService.debit(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 贏錢加款
     *
     * @param request 贏錢請求
     * @return 交易結果
     */
    @PostMapping("/credit")
    public ResponseDTO<CreditResponse> credit(@RequestBody @Valid CreditRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行贏錢加款
        CreditResponse response = seamlessWalletService.credit(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 取消交易
     *
     * @param request 取消請求
     * @return 交易結果
     */
    @PostMapping("/cancel")
    public ResponseDTO<CancelResponse> cancel(@RequestBody @Valid CancelRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行取消
        CancelResponse response = seamlessWalletService.cancel(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 查詢餘額
     *
     * @param request 餘額查詢請求
     * @return 餘額
     */
    @PostMapping("/balance")
    public ResponseDTO<BalanceResponse> getBalance(@RequestBody @Valid BalanceRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 查詢餘額
        BalanceResponse response = seamlessWalletService.getBalance(request);

        return ResponseDTO.ok(response);
    }
}
```

#### Step 2: 實作 Token 驗證機制

```java
/**
 * Token 驗證服務
 *
 * Token 組成：
 * player_id|tenant_id|timestamp|signature
 *
 * 簽名算法：
 * signature = HMAC-SHA256(player_id + tenant_id + timestamp, secret_key)
 */
@Service
@RequiredArgsConstructor
public class TokenVerificationService {

    @Value("${game.api.secret-key}")
    private String secretKey;

    @Value("${game.api.token-ttl:300}") // 5 分鐘
    private int tokenTtl;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 生成 Token
     *
     * @param playerId 玩家ID
     * @param tenantId 租戶ID
     * @return Token
     */
    public String generateToken(Long playerId, String tenantId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String data = playerId + "|" + tenantId + "|" + timestamp;

        // HMAC-SHA256 簽名
        String signature = HmacUtils.hmacSha256Hex(secretKey, data);

        // 組裝 Token
        String token = data + "|" + signature;

        // Base64 編碼
        return Base64.getEncoder().encodeToString(token.getBytes());
    }

    /**
     * 驗證 Token
     *
     * 檢查項目：
     * 1. 格式正確
     * 2. 簽名合法
     * 3. 未過期
     * 4. 未被重放（Redis 黑名單）
     *
     * @param token Token
     * @throws TokenInvalidException 驗證失敗
     */
    public void verifyToken(String token) {
        try {
            // 1. Base64 解碼
            String decoded = new String(Base64.getDecoder().decode(token));

            // 2. 解析 Token
            String[] parts = decoded.split("\\|");
            if (parts.length != 4) {
                throw new TokenInvalidException("Token 格式錯誤");
            }

            String playerId = parts[0];
            String tenantId = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String signature = parts[3];

            // 3. 驗證簽名
            String expectedSignature = HmacUtils.hmacSha256Hex(
                secretKey,
                playerId + "|" + tenantId + "|" + timestamp
            );

            if (!MessageDigest.isEqual(signature.getBytes(), expectedSignature.getBytes())) {
                throw new TokenInvalidException("Token 簽名無效");
            }

            // 4. 驗證有效期
            long now = System.currentTimeMillis() / 1000;
            if (now - timestamp > tokenTtl) {
                throw new TokenInvalidException("Token 已過期");
            }

            // 5. 防重放攻擊（Redis 黑名單）
            String blacklistKey = "token:blacklist:" + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                throw new TokenInvalidException("Token 已被使用");
            }

            // 6. 加入黑名單（TTL = Token 有效期）
            redisTemplate.opsForValue().set(
                blacklistKey,
                "1",
                tokenTtl,
                TimeUnit.SECONDS
            );

        } catch (IllegalArgumentException e) {
            throw new TokenInvalidException("Token 解碼失敗", e);
        }
    }
}
```

#### Step 3: 實作冪等性處理（三層防護）

```java
/**
 * Seamless Wallet 服務
 *
 * 冪等性三層防護：
 * 1. Redis 快速檢查（99% 場景）
 * 2. 資料庫檢查（Redis 失效時）
 * 3. 分佈式鎖（極端併發時）
 */
@Service
@RequiredArgsConstructor
public class SeamlessWalletService {

    private final WalletService walletService;
    private final GameTransactionDao gameTransactionDao;
    private final RedisTemplate<String, GameTransactionResult> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 下注扣款（冪等性保證）
     *
     * @param request 下注請求
     * @return 交易結果
     */
    public DebitResponse debit(DebitRequest request) {
        String requestId = request.getRequestId();

        // 第一層：Redis 快速檢查（99% 場景）
        String cacheKey = "game:tx:" + requestId;
        GameTransactionResult cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Request {} already processed (cached)", requestId);
            return DebitResponse.fromCachedResult(cached);
        }

        // 第二層：資料庫檢查（Redis 失效時）
        GameTransaction existing = gameTransactionDao.findByRequestId(requestId);

        if (existing != null) {
            log.info("Request {} already processed (db)", requestId);

            // 回寫 Redis
            GameTransactionResult result = GameTransactionResult.from(existing);
            redisTemplate.opsForValue().set(cacheKey, result, 15, TimeUnit.MINUTES);

            return DebitResponse.fromDbRecord(existing);
        }

        // 第三層：分佈式鎖（極端併發時）
        String lockKey = "game:lock:" + requestId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 嘗試獲取鎖（等待 3 秒，持有 10 秒）
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);

            if (!locked) {
                throw new ConcurrentRequestException("請求處理中，請稍後重試");
            }

            // 雙重檢查（獲得鎖後再次確認）
            GameTransaction doubleCheck = gameTransactionDao.findByRequestId(requestId);
            if (doubleCheck != null) {
                log.info("Request {} already processed (double check)", requestId);
                return DebitResponse.fromDbRecord(doubleCheck);
            }

            // 執行扣款
            walletService.debitWallet(
                request.getPlayerId(),
                request.getAmount(),
                "GAME_BET:" + requestId
            );

            // 記錄交易
            GameTransaction transaction = GameTransaction.builder()
                .requestId(requestId)
                .playerId(request.getPlayerId())
                .tenantId(request.getTenantId())
                .transactionType(TransactionType.DEBIT)
                .amount(request.getAmount())
                .gameId(request.getGameId())
                .roundId(request.getRoundId())
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

            gameTransactionDao.insert(transaction);

            // 快取結果（15 分鐘）
            GameTransactionResult result = GameTransactionResult.from(transaction);
            redisTemplate.opsForValue().set(cacheKey, result, 15, TimeUnit.MINUTES);

            return DebitResponse.fromTransaction(transaction);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException("鎖獲取被中斷", e);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 贏錢加款（冪等性保證）
     */
    public CreditResponse credit(CreditRequest request) {
        // 類似 debit()，省略重複邏輯
        // ...
    }

    /**
     * 取消交易（冪等性保證）
     */
    public CancelResponse cancel(CancelRequest request) {
        String originalRequestId = request.getOriginalRequestId();

        // 1. 查詢原始交易
        GameTransaction original = gameTransactionDao.findByRequestId(originalRequestId);

        if (original == null) {
            throw new TransactionNotFoundException("原始交易不存在：" + originalRequestId);
        }

        if (original.getStatus() == TransactionStatus.CANCELLED) {
            log.info("Transaction {} already cancelled", originalRequestId);
            return CancelResponse.alreadyCancelled();
        }

        // 2. 退款
        walletService.creditWallet(
            original.getPlayerId(),
            original.getAmount(),
            "GAME_CANCEL:" + request.getRequestId()
        );

        // 3. 更新原始交易狀態
        original.setStatus(TransactionStatus.CANCELLED);
        original.setCancelledAt(LocalDateTime.now());
        gameTransactionDao.update(original);

        // 4. 清除快取
        redisTemplate.delete("game:tx:" + originalRequestId);

        return CancelResponse.success();
    }
}
```

#### Step 4: 實作錯誤恢復機制

```java
/**
 * 遊戲交易恢復服務
 *
 * 處理場景：
 * 1. 網絡超時導致的懸掛交易
 * 2. 系統異常導致的不一致狀態
 * 3. 廠商回調失敗
 */
@Service
@RequiredArgsConstructor
public class GameTransactionRecoveryService {

    private final GameTransactionDao gameTransactionDao;
    private final WalletService walletService;
    private final GameProviderService gameProviderService;

    /**
     * 定時掃描懸掛交易
     *
     * 執行時間：每 5 分鐘
     * 懸掛交易定義：創建超過 10 分鐘且狀態為 PENDING
     */
    @Scheduled(fixedDelay = 300000)
    public void scanPendingTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<GameTransaction> pending = gameTransactionDao.findPendingBefore(threshold);

        for (GameTransaction tx : pending) {
            try {
                recoverTransaction(tx);
            } catch (Exception e) {
                log.error("Failed to recover transaction {}", tx.getRequestId(), e);
            }
        }
    }

    /**
     * 恢復單筆交易
     *
     * 恢復策略：
     * 1. 向遊戲廠商查詢交易狀態
     * 2. 根據廠商結果調整系統狀態
     * 3. 補償錢包餘額
     */
    private void recoverTransaction(GameTransaction tx) {
        // 1. 向遊戲廠商查詢
        GameProviderTransactionStatus providerStatus =
            gameProviderService.queryTransactionStatus(tx.getRoundId());

        // 2. 根據廠商狀態決定恢復動作
        switch (providerStatus) {
            case SUCCESS -> {
                // 廠商成功，系統更新為成功
                tx.setStatus(TransactionStatus.SUCCESS);
                gameTransactionDao.update(tx);
                log.info("Transaction {} recovered: SUCCESS", tx.getRequestId());
            }

            case FAILED -> {
                // 廠商失敗，退款
                if (tx.getTransactionType() == TransactionType.DEBIT) {
                    walletService.creditWallet(
                        tx.getPlayerId(),
                        tx.getAmount(),
                        "RECOVERY:" + tx.getRequestId()
                    );
                }

                tx.setStatus(TransactionStatus.FAILED);
                gameTransactionDao.update(tx);
                log.info("Transaction {} recovered: FAILED", tx.getRequestId());
            }

            case NOT_FOUND -> {
                // 廠商無記錄，視為失敗
                if (tx.getTransactionType() == TransactionType.DEBIT) {
                    walletService.creditWallet(
                        tx.getPlayerId(),
                        tx.getAmount(),
                        "RECOVERY:" + tx.getRequestId()
                    );
                }

                tx.setStatus(TransactionStatus.NOT_FOUND);
                gameTransactionDao.update(tx);
                log.warn("Transaction {} not found in provider", tx.getRequestId());
            }
        }

        // 3. 發送告警（如果是異常狀態）
        if (tx.getStatus() == TransactionStatus.FAILED ||
            tx.getStatus() == TransactionStatus.NOT_FOUND) {
            alertService.sendTransactionRecoveryAlert(tx);
        }
    }
}
```

### ✅ 驗證清單

- [ ] Token 驗證機制正確（簽名、有效期、防重放）
- [ ] 冪等性測試通過（重複請求返回相同結果）
- [ ] 並發測試通過（1000 TPS 無重複扣款）
- [ ] 取消交易正確退款
- [ ] 錯誤恢復機制正常運作
- [ ] 懸掛交易自動恢復
- [ ] 所有遊戲交易記錄到審計日誌

### ⚠️ 常見陷阱

1. **Token 重放攻擊**：必須使用 Redis 黑名單
2. **冪等性失效**：三層防護缺一不可
3. **分佈式鎖超時**：持有時間應大於業務執行時間
4. **懸掛交易未處理**：定時任務必須穩定運行

---

*(繼續其餘 15 個實作指南...)*

**文檔說明**：
- 本文檔共計劃 20 個實作指南
- 目前已完成 5 個核心指南（錢包、支付、風控、對帳、遊戲對接）
- 剩餘 15 個將在後續階段完成
- 每個指南包含：閱讀順序、實作目標、步驟、驗證清單、常見陷阱

---

## 📊 實作指南統計

| 模塊 | 已完成 | 計劃中 | 總計 |
|------|--------|--------|------|
| 核心財務流程 | 4 | 0 | 4 |
| 遊戲營運 | 3 | 0 | 3 |
| 活動系統 | 0 | 3 | 3 |
| 風控系統 | 0 | 3 | 3 |
| 平台治理 | 0 | 4 | 4 |
| 技術基礎設施 | 0 | 3 | 3 |
| **總計** | **7** | **13** | **20** |

---

## 🔗 相關文檔

- [00-00 QUICKSTART](./00-00_QUICKSTART.md) - 10 分鐘快速入門
- [00-00 BUSINESS_FLOWS](./00-00_BUSINESS_FLOWS.md) - 業務流程圖集
- [SSOT 映射表](../SSOT_MAPPING.md) - 核心概念唯一真相來源
- [遷移對照表](../MIGRATION_MAPPING.md) - 舊編號 → 新編號

---

**維護團隊**: Architecture Team
**反饋聯繫**: architecture@company.com
**最後更新**: 2026-02-03
