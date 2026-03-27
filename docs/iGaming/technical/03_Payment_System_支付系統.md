---
title: "Ch3: 支付系統技術架構"
part: technical
module: payment-system
version: v2.2
created: 2026-03-24
---

# 第 3 章：支付系統技術架構

## 3.1 模組概述

支付系統採用 PSP adapter pattern，實現智慧路由、多收單方冗餘、以及 SAGA 模式提款流程。

核心特性：
- **多 PSP 支持**：Stripe、Nuvei、Adyen、CoinsPaid、Mock（測試）
- **智慧路由**：多維評分機制，自動選擇最優 PSP
- **SAGA 提款**：LiteFlow 編排，支持風控評估、人工審批、自動賠付
- **Webhook 回調**：簽名驗證、冪等性、指數退避重試、死信隊列
- **三方對帳**：L1 實時、L2 補償、L3 日結

## 3.2 資料模型

### t_payment_order

| 欄位 | 類型 | 說明 |
|------|------|------|
| order_id | UUID | 支付訂單唯一識別碼 |
| tenant_id | UUID | 租戶 ID |
| player_id | UUID | 玩家 ID |
| type | ENUM | DEPOSIT（存款）/ WITHDRAWAL（提款） |
| psp_code | VARCHAR | PSP 代碼（stripe, nuvei, adyen, coinspaid, mock） |
| amount | DECIMAL(15,2) | 訂單金額 |
| currency | VARCHAR(3) | ISO 4217 貨幣代碼（USD, EUR, CNY, etc.） |
| status | ENUM | PENDING / PROCESSING / SUCCESS / FAILED / CANCELLED |
| fee | DECIMAL(15,2) | 手續費 |
| psp_transaction_id | VARCHAR | PSP 的交易 ID |
| callback_payload | JSONB | PSP 回調原始資訊 |
| idempotency_key | VARCHAR(64) | 冪等性鑰匙 |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

### t_psp_config

| 欄位 | 類型 | 說明 |
|------|------|------|
| tenant_id | UUID | 租戶 ID |
| psp_code | VARCHAR | PSP 代碼 |
| enabled | BOOLEAN | 是否啟用 |
| priority | INT | 優先級（1-10）|
| fee_rate | DECIMAL(5,2) | 手續費率（%） |
| min_amount | DECIMAL(15,2) | 最小金額 |
| max_amount | DECIMAL(15,2) | 最大金額 |
| supported_currencies | JSONB | 支持的貨幣列表 |
| health_status | ENUM | HEALTHY / DEGRADED / DOWN |
| ip_whitelist | TEXT | IP 白名單 |

### t_reconciliation_record

| 欄位 | 類型 | 說明 |
|------|------|------|
| reconciliation_id | UUID | 對帳記錄 ID |
| psp_code | VARCHAR | PSP 代碼 |
| level | INT | 對帳級別（1/2/3） |
| reconciliation_date | DATE | 對帳日期 |
| matched_count | INT | 匹配交易數 |
| unmatched_count | INT | 未匹配交易數 |
| discrepancy_amount | DECIMAL(15,2) | 差異金額 |
| status | ENUM | PENDING / COMPLETED / MANUAL_REVIEW |
| created_at | TIMESTAMP | 建立時間 |

## 3.3 PSP 適配器模式

所有外部支付提供商均實現 `PaymentProviderAdapter` 接口，確保系統與 PSP 的解耦。

```java
public interface PaymentProviderAdapter {
    /**
     * 返回 PSP 代碼
     */
    String getPspCode();  // "stripe", "nuvei", "adyen", "coinspaid", "mock"

    /**
     * 處理存款
     */
    Either<String, PspDepositResponse> deposit(PspDepositRequest request);

    /**
     * 處理提款
     */
    Either<String, PspWithdrawResponse> withdraw(PspWithdrawRequest request);

    /**
     * 查詢交易狀態
     */
    Option<PspQueryResponse> queryStatus(String pspTransactionId);

    /**
     * 驗證回調簽名
     */
    boolean verifyCallback(String payload, String signature, long timestamp);
}
```

**PSP 實現示例**：

```java
@Component
public class StripePaymentAdapter implements PaymentProviderAdapter {

    @Override
    public String getPspCode() {
        return "stripe";
    }

    @Override
    public Either<String, PspDepositResponse> deposit(PspDepositRequest request) {
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(this.apiKey)
                .build();

            Map<String, Object> params = new HashMap<>();
            params.put("amount", request.getAmount().multiply(new BigDecimal("100")).longValue());
            params.put("currency", request.getCurrency().toLowerCase());
            params.put("source", request.getTokenId());
            params.put("description", "Deposit for player: " + request.getPlayerId());

            Charge charge = Charge.create(params, requestOptions);

            return Either.right(PspDepositResponse.builder()
                .pspTransactionId(charge.getId())
                .status("PROCESSING")
                .redirectUrl(null)
                .build());
        } catch (StripeException e) {
            return Either.left(e.getMessage());
        }
    }

    @Override
    public boolean verifyCallback(String payload, String signature, long timestamp) {
        // HMAC-SHA256 驗證
        String computed = HmacUtils.hmacSha256Hex(this.webhookSecret, payload);
        return computed.equals(signature);
    }
}
```

**PSP Factory**：Spring Bean 管理，O(1) 時間複雜度查找。

```java
@Component
public class PaymentProviderFactory {

    private final Map<String, PaymentProviderAdapter> adapters;

    public PaymentProviderFactory(List<PaymentProviderAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(PaymentProviderAdapter::getPspCode, Function.identity()));
    }

    public Optional<PaymentProviderAdapter> getAdapter(String pspCode) {
        return Optional.ofNullable(adapters.get(pspCode));
    }
}
```

## 3.4 智慧路由引擎

路由引擎通過多維評分機制為每筆交易選擇最優 PSP。

### 評分維度

1. **租戶配置過濾**：只考慮租戶啟用的 PSP
2. **貨幣 + 地區適配**：檢查支持的貨幣和國家
3. **金額範圍驗證**：min_amount ≤ 金額 ≤ max_amount
4. **健康檢查**：Resilience4j 熔斷器監控 PSP 狀態
5. **加權評分**：
   - 成功率：50%
   - 手續費：30%
   - 交易速度：15%
   - VIP 加成：5%

### 熔斷器配置

```yaml
resilience4j:
  circuitbreaker:
    instances:
      psp-health:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 5000ms
        permitted-calls-in-half-open-state: 10
        sliding-window-size: 10
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 30000ms
```

### 路由核心邏輯

```java
@Service
public class PaymentRouterService {

    private final PspHealthService healthService;
    private final PspConfigRepository configRepo;

    public Either<String, RouteResult> routePayment(
            UUID tenantId,
            PaymentRoutingContext context) {

        // 1. 過濾租戶啟用的 PSP
        List<PspConfig> candidates = configRepo.findByTenantIdAndEnabledTrue(tenantId);

        // 2. 驗證貨幣、地區、金額
        candidates = candidates.stream()
            .filter(cfg -> isCurrencySupported(cfg, context.getCurrency()))
            .filter(cfg -> isCountrySupported(cfg, context.getCountry()))
            .filter(cfg -> isAmountInRange(cfg, context.getAmount()))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return Either.left("No eligible PSP for this payment");
        }

        // 3. 評分
        List<ScoredPsp> scored = candidates.stream()
            .map(cfg -> scorePsp(cfg, context))
            .sorted(Comparator.comparingDouble(ScoredPsp::getScore).reversed())
            .collect(Collectors.toList());

        // 4. 檢查熔斷器狀態
        for (ScoredPsp scored : scored) {
            if (healthService.isHealthy(scored.getPspCode())) {
                return Either.right(new RouteResult(scored.getPspCode(), scored.getScore()));
            }
        }

        return Either.left("All PSP unhealthy or unreachable");
    }

    private ScoredPsp scorePsp(PspConfig cfg, PaymentRoutingContext ctx) {
        double successRate = healthService.getSuccessRate(cfg.getPspCode());
        double feeScore = (1 - cfg.getFeeRate() / 100) * 100;  // 手續費越低越好
        double speedScore = calculateSpeedScore(cfg.getPspCode());
        double vipBonus = ctx.isVip() ? 5.0 : 0.0;

        double totalScore =
            successRate * 0.5 +
            feeScore * 0.3 +
            speedScore * 0.15 +
            vipBonus * 0.05;

        return new ScoredPsp(cfg.getPspCode(), totalScore);
    }
}
```

## 3.5 存款流程

### 流程概覽

```
玩家 → 前端 → API Gateway → PaymentService → PSP Router → PSP Adapter → 外部 PSP
↑                                                               ↓
└─────────────────────────── Callback ←──────────────────────┘
```

### 詳細步驟

1. **玩家選擇支付方式**：前端展示租戶支持的支付方式列表
2. **建立支付訂單**：
   - 建立 t_payment_order，status = PENDING
   - 生成冪等性鑰匙（idempotency_key）防止重複提交
3. **路由到最優 PSP**：調用路由引擎選擇 PSP
4. **PSP 處理**：
   - 若需重定向：PSP 返回重定向 URL（如信用卡支付）
   - 若直接處理：PSP 立即返回結果
5. **異步回調**：PSP 服務 → 系統 Webhook 端點
6. **驗證回調簽名**：HMAC 驗證防止偽造
7. **更新訂單狀態**：status = SUCCESS / FAILED
8. **錢包入帳**：原子性操作，調用 WalletManager.credit()
9. **發佈事件**：發佈 DEPOSIT_COMPLETED，觸發下游業務邏輯

### 代碼實現

```java
@Service
public class DepositService {

    private final PaymentOrderRepository orderRepo;
    private final PaymentRouterService router;
    private final PaymentProviderFactory factory;
    private final WalletManager walletManager;
    private final IdempotencyService idempotency;
    private final EventPublisher eventPublisher;

    @Transactional
    public Either<String, PaymentOrderDto> initiateDeposit(
            DepositRequest request,
            UUID tenantId,
            UUID playerId) {

        // 1. 冪等性檢查
        Optional<PaymentOrder> existing = idempotency.findByKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return Either.right(mapToDto(existing.get()));
        }

        // 2. 建立訂單
        PaymentOrder order = PaymentOrder.builder()
            .orderId(UUID.randomUUID())
            .tenantId(tenantId)
            .playerId(playerId)
            .type(PaymentType.DEPOSIT)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.PENDING)
            .idempotencyKey(request.getIdempotencyKey())
            .createdAt(Instant.now())
            .build();

        orderRepo.save(order);

        // 3. 路由 PSP
        PaymentRoutingContext routingContext = PaymentRoutingContext.builder()
            .currency(request.getCurrency())
            .amount(request.getAmount())
            .country(request.getCountry())
            .vip(false)
            .build();

        Either<String, RouteResult> routeResult = router.routePayment(tenantId, routingContext);
        if (routeResult.isLeft()) {
            order.setStatus(PaymentStatus.FAILED);
            orderRepo.save(order);
            return Either.left(routeResult.getLeft());
        }

        String pspCode = routeResult.get().getPspCode();
        order.setPspCode(pspCode);

        // 4. 調用 PSP
        PaymentProviderAdapter adapter = factory.getAdapter(pspCode)
            .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspCode));

        PspDepositRequest pspRequest = PspDepositRequest.builder()
            .amount(order.getAmount())
            .currency(order.getCurrency())
            .orderId(order.getOrderId().toString())
            .playerId(playerId.toString())
            .tokenId(request.getPaymentTokenId())
            .build();

        Either<String, PspDepositResponse> pspResult = adapter.deposit(pspRequest);

        if (pspResult.isLeft()) {
            order.setStatus(PaymentStatus.FAILED);
            orderRepo.save(order);
            return Either.left(pspResult.getLeft());
        }

        PspDepositResponse pspResponse = pspResult.get();
        order.setPspTransactionId(pspResponse.getPspTransactionId());
        order.setStatus(PaymentStatus.PROCESSING);
        orderRepo.save(order);

        return Either.right(mapToDto(order));
    }

    @Transactional
    public void processDepositCallback(String pspCode, String payload, String signature) {
        PaymentProviderAdapter adapter = factory.getAdapter(pspCode)
            .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspCode));

        // 1. 簽名驗證
        if (!adapter.verifyCallback(payload, signature, System.currentTimeMillis())) {
            throw new SecurityException("Invalid callback signature");
        }

        // 2. 解析回調
        DepositCallbackPayload callback = parseCallback(pspCode, payload);

        // 3. 查找訂單
        PaymentOrder order = orderRepo.findByPspTransactionId(callback.getPspTransactionId())
            .orElseThrow(() -> new EntityNotFoundException("Payment order not found"));

        // 4. 更新狀態
        if ("success".equalsIgnoreCase(callback.getStatus())) {
            order.setStatus(PaymentStatus.SUCCESS);
            order.setCallbackPayload(payload);
            orderRepo.save(order);

            // 5. 入帳
            walletManager.credit(order.getPlayerId(), order.getAmount(),
                "Deposit from " + order.getPspCode());

            // 6. 發佈事件
            eventPublisher.publish(new DepositCompletedEvent(
                order.getOrderId(),
                order.getPlayerId(),
                order.getAmount(),
                order.getCurrency()
            ));
        } else {
            order.setStatus(PaymentStatus.FAILED);
            orderRepo.save(order);
        }
    }
}
```

### 3D Secure 處理

對於 EU 地區的信用卡支付，強制要求 3D Secure 驗證。由 PSP SDK 自動處理，系統側需驗證回調中的 3D Secure 認證狀態。

```java
// 在驗證回調時檢查 3DS 狀態
if ("EU".equalsIgnoreCase(context.getCountry()) &&
    "CARD".equalsIgnoreCase(paymentMethod)) {

    String threeDsStatus = callback.get("three_ds_status");
    if (!"authenticated".equalsIgnoreCase(threeDsStatus)) {
        throw new SecurityException("3DS authentication failed");
    }
}
```

## 3.6 提款流程 (SAGA)

提款流程採用 LiteFlow SAGA 編排，確保複雜業務流程的原子性和可補償性。

### SAGA 鏈定義

```java
@Component
public class WithdrawalSagaFactory {

    private final LiteFlowExecutor executor;

    public WithdrawalSaga createWithdrawalSaga() {
        return executor.chain()
            .then(new FreezeWalletNode())           // 1. 鎖定提款金額
            .then(new RiskEvaluationNode())         // 2. 風控評估
            .then(new ManualApprovalNode())         // 3. 人工審批（如需）
            .then(new SelectPspNode())              // 4. 選擇 PSP
            .then(new ProcessPayoutNode())          // 5. 執行提款
            .then(new CompensationNode())           // 6. 補償邏輯
            .build();
    }
}
```

### 審批層級

| 金額 | 審批流程 | 審批人 | 時限 |
|------|---------|--------|------|
| < $1,000 | 自動（風控評分 < 30） | 無 | 實時 |
| $1,000-$10,000 | 客服經理審批 | CS Manager | 30 分鐘 |
| $10,000-$50,000 | CFO 審批 | CFO | 2 小時 |
| > $50,000 | CFO + CEO 雙審批 | CFO & CEO | 4 小時 |

### Node 實現示例

```java
@Component
public class FreezeWalletNode extends CNodeComponent {

    @Autowired
    private WalletManager walletManager;

    @Override
    public void process(NodeComponent nodeComponent) {
        WithdrawalContext context = (WithdrawalContext) nodeComponent.getContextData("withdrawal");

        try {
            // 鎖定錢包金額
            walletManager.freeze(context.getPlayerId(), context.getAmount(),
                "Withdrawal: " + context.getWithdrawalId());

            context.setFrozen(true);
            nodeComponent.setContextData("withdrawal", context);
            nodeComponent.setIsSuccess(true);
        } catch (Exception e) {
            nodeComponent.setIsSuccess(false);
            nodeComponent.setErrorMsg(e.getMessage());

            // SAGA 將自動觸發補償鏈
            context.addCompensation("unfreeze_wallet",
                new UnfreezeWalletCompensation(context.getPlayerId(), context.getAmount()));
        }
    }
}

@Component
public class RiskEvaluationNode extends CNodeComponent {

    @Autowired
    private RiskScoringService riskService;

    @Override
    public void process(NodeComponent nodeComponent) {
        WithdrawalContext context = (WithdrawalContext) nodeComponent.getContextData("withdrawal");

        try {
            // 風控評分（0-100）
            int riskScore = riskService.evaluateWithdrawal(
                context.getPlayerId(),
                context.getAmount(),
                context.getCountry(),
                context.getIpAddress(),
                context.getDeviceId()
            );

            context.setRiskScore(riskScore);

            // 分值 >= 30 需要人工審批
            if (riskScore >= 30) {
                context.setRequiresApproval(true);

                // 根據金額和分值確定審批人
                String approver = determineApprover(context.getAmount(), riskScore);
                context.setApprover(approver);
            }

            nodeComponent.setContextData("withdrawal", context);
            nodeComponent.setIsSuccess(true);
        } catch (Exception e) {
            nodeComponent.setIsSuccess(false);
            nodeComponent.setErrorMsg(e.getMessage());
        }
    }

    private String determineApprover(BigDecimal amount, int riskScore) {
        if (riskScore >= 70) {
            // 高風險需要 CFO
            return "CFO";
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return "CFO";
        } else {
            return "CS_MANAGER";
        }
    }
}

@Component
public class ManualApprovalNode extends CNodeComponent {

    @Autowired
    private ApprovalService approvalService;

    @Override
    public void process(NodeComponent nodeComponent) {
        WithdrawalContext context = (WithdrawalContext) nodeComponent.getContextData("withdrawal");

        if (!context.isRequiresApproval()) {
            // 跳過人工審批
            nodeComponent.setIsSuccess(true);
            return;
        }

        try {
            // 建立審批任務
            ApprovalTask task = ApprovalTask.builder()
                .taskId(UUID.randomUUID())
                .withdrawalId(context.getWithdrawalId())
                .playerId(context.getPlayerId())
                .amount(context.getAmount())
                .riskScore(context.getRiskScore())
                .approverRole(context.getApprover())
                .status(ApprovalStatus.PENDING)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(4)))
                .build();

            approvalService.createTask(task);

            // 等待批准（異步）
            CompletableFuture<ApprovalResult> approvalFuture = approvalService.waitForApproval(task.getTaskId());

            ApprovalResult result = approvalFuture.get(4, TimeUnit.HOURS);

            if (!result.isApproved()) {
                context.setApprovalRejected(true);
                context.setApprovalReason(result.getReason());
                nodeComponent.setIsSuccess(false);
                return;
            }

            context.setApproved(true);
            context.setApprovedBy(result.getApprovedBy());
            nodeComponent.setContextData("withdrawal", context);
            nodeComponent.setIsSuccess(true);
        } catch (TimeoutException e) {
            nodeComponent.setIsSuccess(false);
            nodeComponent.setErrorMsg("Approval timeout");
        } catch (Exception e) {
            nodeComponent.setIsSuccess(false);
            nodeComponent.setErrorMsg(e.getMessage());
        }
    }
}

@Component
public class ProcessPayoutNode extends CNodeComponent {

    @Autowired
    private PaymentProviderFactory factory;

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Override
    public void process(NodeComponent nodeComponent) {
        WithdrawalContext context = (WithdrawalContext) nodeComponent.getContextData("withdrawal");

        try {
            // 選擇 PSP
            PaymentProviderAdapter adapter = factory.getAdapter(context.getPspCode())
                .orElseThrow(() -> new IllegalArgumentException("PSP not found"));

            // 構建提款請求
            PspWithdrawRequest withdrawRequest = PspWithdrawRequest.builder()
                .amount(context.getAmount())
                .currency(context.getCurrency())
                .orderId(context.getWithdrawalId().toString())
                .playerId(context.getPlayerId().toString())
                .bankAccountToken(context.getBankAccountToken())
                .build();

            // 執行提款
            Either<String, PspWithdrawResponse> result = adapter.withdraw(withdrawRequest);

            if (result.isLeft()) {
                nodeComponent.setIsSuccess(false);
                nodeComponent.setErrorMsg(result.getLeft());
                return;
            }

            PspWithdrawResponse response = result.get();

            // 記錄訂單
            PaymentOrder order = PaymentOrder.builder()
                .orderId(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .playerId(context.getPlayerId())
                .type(PaymentType.WITHDRAWAL)
                .pspCode(context.getPspCode())
                .amount(context.getAmount())
                .currency(context.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .pspTransactionId(response.getPspTransactionId())
                .createdAt(Instant.now())
                .build();

            orderRepo.save(order);

            context.setPaymentOrderId(order.getOrderId());
            context.setPspTransactionId(response.getPspTransactionId());
            nodeComponent.setContextData("withdrawal", context);
            nodeComponent.setIsSuccess(true);
        } catch (Exception e) {
            nodeComponent.setIsSuccess(false);
            nodeComponent.setErrorMsg(e.getMessage());
        }
    }
}

@Component
public class CompensationNode extends CNodeComponent {

    @Autowired
    private WalletManager walletManager;

    @Override
    public void process(NodeComponent nodeComponent) {
        WithdrawalContext context = (WithdrawalContext) nodeComponent.getContextData("withdrawal");

        // 檢查是否需要補償
        if (nodeComponent.getFlow().isFailed()) {
            // SAGA 失敗，執行補償
            if (context.isFrozen()) {
                walletManager.unfreeze(context.getPlayerId(), context.getAmount(),
                    "Withdrawal cancelled");
            }

            nodeComponent.setIsSuccess(true);
        } else {
            // SAGA 成功
            nodeComponent.setIsSuccess(true);
        }
    }
}
```

### 狀態轉移圖

```
INITIATED → FROZEN → RISK_EVALUATED → [APPROVED/REJECTED]
                                            ↓
                                    PAYOUT_PROCESSING
                                            ↓
                                    PAYOUT_SUCCESS/FAILED
                                            ↓
                                    COMPLETED/COMPENSATED
```

## 3.7 Webhook 回調處理

### 回調驗證流程

```java
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private PaymentProviderFactory factory;

    @Autowired
    private CallbackProcessingService callbackService;

    @PostMapping("/{pspCode}")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String pspCode,
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") long timestamp) {

        try {
            // 1. 驗證簽名
            PaymentProviderAdapter adapter = factory.getAdapter(pspCode)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found"));

            if (!adapter.verifyCallback(payload, signature, timestamp)) {
                return ResponseEntity.status(401).build();
            }

            // 2. 校驗時間戳（防重放）
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - timestamp) > 300000) {  // 5 分鐘
                return ResponseEntity.status(400).build();
            }

            // 3. 異步處理回調（防止超時）
            callbackService.enqueueCallback(pspCode, payload);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
```

### 冪等性處理

```java
@Service
public class CallbackProcessingService {

    private final MessageQueue callbackQueue;
    private final PaymentOrderRepository orderRepo;
    private final IdempotencyService idempotency;

    @KafkaListener(topics = "payment-callbacks")
    @Transactional
    public void processCallback(CallbackMessage message) {
        String callbackId = message.getCallbackId();

        // 檢查冪等性
        Optional<CallbackRecord> existing = idempotency.findByCallbackId(callbackId);
        if (existing.isPresent()) {
            // 已處理，直接返回
            return;
        }

        try {
            // 解析並處理回調
            PaymentCallbackPayload payload = parsePayload(message.getPayload());

            PaymentOrder order = orderRepo.findByPspTransactionId(payload.getPspTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

            // 更新訂單
            updateOrderStatus(order, payload);

            // 記錄冪等性
            idempotency.recordCallback(callbackId);
        } catch (Exception e) {
            // 放入死信隊列，人工處理
            deadLetterQueue.enqueue(message);
            throw e;
        }
    }
}
```

### 重試策略

| 重試次數 | 等待時間 | 累計時間 |
|---------|---------|---------|
| 1 | 5s | 5s |
| 2 | 10s | 15s |
| 3 | 20s | 35s |
| 4 | 40s | 75s |
| 5 | 80s | 155s |
| 6 | 160s | 315s (~5 min) |

```java
@Service
public class CallbackRetryService {

    @Autowired
    private CallbackRepository callbackRepo;

    @Scheduled(fixedDelay = 10000)  // 每 10 秒檢查一次
    public void retryFailedCallbacks() {
        List<CallbackRecord> failedRecords = callbackRepo.findByStatusAndRetryLessThan(
            CallbackStatus.FAILED, 6);

        for (CallbackRecord record : failedRecords) {
            int retryCount = record.getRetryCount();
            long[] delays = {5, 10, 20, 40, 80, 160};  // 秒

            long nextRetryTime = record.getLastRetryTime().plus(
                Duration.ofSeconds(delays[retryCount])).toEpochMilli();

            if (System.currentTimeMillis() >= nextRetryTime) {
                try {
                    processCallback(record.getPayload());
                    record.setStatus(CallbackStatus.SUCCESS);
                } catch (Exception e) {
                    record.setRetryCount(retryCount + 1);
                    if (retryCount >= 5) {
                        // 超過 6 次，移至死信隊列
                        record.setStatus(CallbackStatus.DLQ);
                    }
                }
                record.setLastRetryTime(Instant.now());
                callbackRepo.save(record);
            }
        }
    }
}
```

## 3.8 加密貨幣支付

CoinsPaid 集成，支持主流加密貨幣。

### 支持的貨幣

| 貨幣 | 代碼 | 類型 | 確認時間 |
|------|------|------|---------|
| Bitcoin | BTC | Native | 10-30 min |
| Ethereum | ETH | Native | 15-30 sec |
| Tether (ERC20) | USDT | Token | 15-30 sec |
| USD Coin (ERC20) | USDC | Token | 15-30 sec |
| Litecoin | LTC | Native | 2-5 min |

### 費率結構

- 加密轉加密（充幣）：0.8%
- 加密轉法幣（出幣）：1.5%
- 最小金額：$10 USD
- 最大金額：$100,000 USD

### AML 風險評分

```java
@Component
public class CryptoAmlService {

    private final CoinsPaidClient coinsPaidClient;

    public Either<String, AmlRiskResult> evaluateWalletRisk(String walletAddress) {
        try {
            // 調用 CoinsPaid AML API
            WalletRiskResponse response = coinsPaidClient.checkAmlRisk(walletAddress);

            int riskScore = response.getRiskScore();  // 0-100
            String riskLevel = response.getRiskLevel();  // LOW/MEDIUM/HIGH

            if ("HIGH".equals(riskLevel)) {
                return Either.left("Wallet flagged for AML risk");
            }

            return Either.right(new AmlRiskResult(walletAddress, riskScore, riskLevel));
        } catch (Exception e) {
            return Either.left("AML check failed: " + e.getMessage());
        }
    }
}
```

### 結算流程

```java
@Service
public class CryptoSettlementService {

    @Autowired
    private CoinsPaidClient coinsPaidClient;

    @Transactional
    public Either<String, SettlementResult> settleCryptoDeposit(
            PaymentOrder order,
            String walletAddress) {

        try {
            // 1. 檢查 AML 風險
            AmlRiskResult amlResult = evaluateWalletRisk(walletAddress).getOrElseThrow();

            // 2. 等待區塊鏈確認（監聽 Webhook）
            BlockchainConfirmation confirmation = waitForConfirmation(order.getPspTransactionId());

            if (!confirmation.isConfirmed()) {
                return Either.left("Blockchain confirmation failed");
            }

            // 3. 實時結算（無需等待提現期）
            SettlementResult result = SettlementResult.builder()
                .orderId(order.getOrderId())
                .settledAt(Instant.now())
                .confirmations(confirmation.getConfirmationCount())
                .build();

            return Either.right(result);
        } catch (Exception e) {
            return Either.left("Crypto settlement failed: " + e.getMessage());
        }
    }
}
```

## 3.9 三方對帳

三級對帳機制確保交易準確性。

### L1 實時對帳

基於 PSP 回調，自動記錄交易。

```java
@Service
public class L1ReconciliationService {

    @Autowired
    private PaymentOrderRepository orderRepo;

    public void recordL1Reconciliation(PaymentOrder order, String pspTransactionId) {
        L1ReconciliationRecord record = L1ReconciliationRecord.builder()
            .recordId(UUID.randomUUID())
            .orderId(order.getOrderId())
            .pspCode(order.getPspCode())
            .pspTransactionId(pspTransactionId)
            .amount(order.getAmount())
            .status(order.getStatus())
            .recordedAt(Instant.now())
            .build();

        l1ReconciliationRepo.save(record);
    }
}
```

### L2 補償對帳

每 5 分鐘輪詢 PSP API，比對未確認的交易。

```java
@Service
public class L2ReconciliationService {

    @Autowired
    private PaymentProviderFactory factory;

    @Scheduled(fixedRate = 300000)  // 每 5 分鐘
    public void compensatoryReconciliation() {
        // 查找所有處理中的訂單
        List<PaymentOrder> pendingOrders = orderRepo.findByStatusIn(Arrays.asList(
            PaymentStatus.PENDING, PaymentStatus.PROCESSING));

        for (PaymentOrder order : pendingOrders) {
            try {
                PaymentProviderAdapter adapter = factory.getAdapter(order.getPspCode())
                    .orElseThrow();

                Option<PspQueryResponse> result = adapter.queryStatus(order.getPspTransactionId());

                result.forEach(response -> {
                    // 比對狀態，如不一致則告警
                    if (!statusMatches(order.getStatus(), response.getStatus())) {
                        alertingService.alert(AlertLevel.WARNING,
                            "Status mismatch for order: " + order.getOrderId());
                    }
                });
            } catch (Exception e) {
                log.error("L2 reconciliation failed for order: " + order.getOrderId(), e);
            }
        }
    }
}
```

### L3 日結對帳

每日進行完整對帳，對比 PSP 結算報告。

```java
@Service
public class L3ReconciliationService {

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Scheduled(cron = "0 1 * * *")  // 每日 01:00 執行
    public void dailyReconciliation() {
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);  // 對帳前一天

        // 1. 獲取 PSP 結算報告
        Map<String, PspSettlementReport> reports = fetchPspSettlementReports(reconciliationDate);

        for (Map.Entry<String, PspSettlementReport> entry : reports.entrySet()) {
            String pspCode = entry.getKey();
            PspSettlementReport report = entry.getValue();

            // 2. 比對系統記錄
            List<PaymentOrder> systemOrders = orderRepo.findByPspCodeAndDateRange(
                pspCode, reconciliationDate.atStartOfDay(),
                reconciliationDate.plusDays(1).atStartOfDay());

            // 3. 比較
            ReconciliationDifference diff = compareOrdersWithReport(systemOrders, report);

            // 4. 生成報告
            L3ReconciliationRecord record = L3ReconciliationRecord.builder()
                .recordId(UUID.randomUUID())
                .pspCode(pspCode)
                .reconciliationDate(reconciliationDate)
                .matchedCount(diff.getMatchedCount())
                .unmatchedCount(diff.getUnmatchedCount())
                .discrepancyAmount(diff.getDiscrepancyAmount())
                .status(diff.isOk() ? ReconciliationStatus.COMPLETED :
                    ReconciliationStatus.MANUAL_REVIEW)
                .createdAt(Instant.now())
                .build();

            l3ReconciliationRepo.save(record);

            if (!diff.isOk()) {
                // 告警，需要人工審查
                alertingService.alert(AlertLevel.ERROR,
                    "L3 reconciliation failed for PSP: " + pspCode);
            }
        }
    }

    private ReconciliationDifference compareOrdersWithReport(
            List<PaymentOrder> systemOrders,
            PspSettlementReport report) {

        Set<String> reportTransactionIds = report.getTransactions().stream()
            .map(PspTransaction::getTransactionId)
            .collect(Collectors.toSet());

        BigDecimal discrepancy = BigDecimal.ZERO;
        int unmatched = 0;

        for (PaymentOrder order : systemOrders) {
            if (!reportTransactionIds.contains(order.getPspTransactionId())) {
                unmatched++;
                discrepancy = discrepancy.add(order.getAmount());
            }
        }

        return ReconciliationDifference.builder()
            .matchedCount(systemOrders.size() - unmatched)
            .unmatchedCount(unmatched)
            .discrepancyAmount(discrepancy)
            .ok(unmatched == 0 && discrepancy.compareTo(BigDecimal.ZERO) == 0)
            .build();
    }
}
```

## 3.10 Chargeback 爭議處理 (C-01)

### 資料模型

```sql
CREATE TABLE t_chargeback (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    payment_order_id BIGINT NOT NULL,    -- FK to t_payment_order
    psp_code VARCHAR(50) NOT NULL,
    psp_dispute_id VARCHAR(256),         -- PSP's dispute reference
    arn VARCHAR(256),                     -- Acquirer Reference Number
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason_code VARCHAR(50),             -- VISA/MC reason code
    status VARCHAR(30) NOT NULL DEFAULT 'NOTIFIED',
    -- NOTIFIED → EVIDENCE_COLLECTING → REPRESENTMENT_SENT → ARBITRATION → WON / LOST / ACCEPTED
    dispute_opened_at TIMESTAMPTZ NOT NULL,
    dispute_deadline TIMESTAMPTZ NOT NULL, -- Default: opened + 30 days (configurable)
    evidence_submitted_at TIMESTAMPTZ,
    representment_sent_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resolution VARCHAR(20),              -- WON, LOST, ACCEPTED
    player_penalty VARCHAR(30),          -- NONE, FLAG, RESTRICT_PAYMENT, FREEZE_ACCOUNT
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cb_tenant_status ON t_chargeback(tenant_id, status);
CREATE INDEX idx_cb_player ON t_chargeback(player_id);
CREATE INDEX idx_cb_deadline ON t_chargeback(dispute_deadline) WHERE status NOT IN ('WON','LOST','ACCEPTED');

CREATE TABLE t_chargeback_evidence (
    id BIGSERIAL PRIMARY KEY,
    chargeback_id BIGINT NOT NULL REFERENCES t_chargeback(id),
    evidence_type VARCHAR(50) NOT NULL,  -- KYC_DOC, GAME_LOG, IP_DEVICE, TC_ACCEPTANCE, TRANSACTION_PROOF
    file_url TEXT,
    description TEXT,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 狀態機

```
NOTIFIED → EVIDENCE_COLLECTING → REPRESENTMENT_SENT → WON
                                                    → LOST
                                                    → ARBITRATION → WON
                                                                  → LOST
         → ACCEPTED (merchant acknowledges, no contest)
```

### Webhook 接收端點

```java
@PostMapping("/api/v1/payment/webhook/chargeback/{pspCode}")
public ResponseEntity<Void> handleChargebackWebhook(
        @PathVariable String pspCode,
        @RequestBody String rawPayload,
        @RequestHeader("X-Signature") String signature) {

    // 1. Verify PSP signature
    PspAdapter adapter = pspAdapterFactory.getAdapter(pspCode);
    if (!adapter.verifyWebhookSignature(rawPayload, signature)) {
        return ResponseEntity.status(401).build();
    }

    // 2. Parse chargeback notification
    ChargebackNotification notification = adapter.parseChargebackNotification(rawPayload);

    // 3. Idempotency check (24h Redis dedup)
    String idempotencyKey = "cb:webhook:" + notification.getPspDisputeId();
    if (!redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", 24, TimeUnit.HOURS)) {
        return ResponseEntity.ok().build(); // Already processed
    }

    // 4. Create or update chargeback record
    chargebackService.processNotification(notification);

    return ResponseEntity.ok().build();
}
```

### 處理流程

```java
@Service
public class ChargebackService {

    @Transactional
    public void processNotification(ChargebackNotification notification) {
        // Find original payment order
        PaymentOrder order = paymentOrderRepo.findByPspTransactionId(
            notification.getOriginalTransactionId());

        // Create chargeback record
        Chargeback cb = new Chargeback();
        cb.setTenantId(order.getTenantId());
        cb.setPlayerId(order.getPlayerId());
        cb.setPaymentOrderId(order.getId());
        cb.setPspCode(order.getPspCode());
        cb.setPspDisputeId(notification.getDisputeId());
        cb.setArn(notification.getArn());
        cb.setAmount(notification.getAmount());
        cb.setCurrency(notification.getCurrency());
        cb.setReasonCode(notification.getReasonCode());
        cb.setStatus("EVIDENCE_COLLECTING");
        cb.setDisputeOpenedAt(notification.getOpenedAt());
        cb.setDisputeDeadline(notification.getOpenedAt()
            .plusDays(configService.getChargebackWindowDays(order.getTenantId()))); // Default 30

        chargebackRepo.save(cb);

        // Auto-collect evidence
        collectEvidence(cb, order);

        // Player penalty escalation
        applyPlayerPenalty(cb);

        // Update CB rate monitoring
        updateCbRateMetrics(order.getTenantId());
    }

    private void applyPlayerPenalty(Chargeback cb) {
        long cbCount = chargebackRepo.countByPlayerId(cb.getPlayerId());

        String penalty;
        if (cbCount == 1) {
            penalty = "FLAG";        // 1st CB: flag only
        } else if (cbCount == 2) {
            penalty = "RESTRICT_PAYMENT"; // 2nd CB: restrict payment methods
            playerService.restrictPayment(cb.getPlayerId());
        } else {
            penalty = "FREEZE_ACCOUNT";  // 3rd+ CB: freeze account
            playerService.freezeAccount(cb.getPlayerId(), "CHARGEBACK_ABUSE");
        }

        cb.setPlayerPenalty(penalty);
        chargebackRepo.save(cb);
    }
}
```

### CB Rate 監控 (ClickHouse)

```sql
-- Chargeback rate calculation (sliding 30-day window)
SELECT
    tenant_id,
    psp_code,
    countIf(type = 'CHARGEBACK') AS cb_count,
    count() AS total_txns,
    cb_count / total_txns AS cb_rate
FROM payment_events
WHERE event_time >= now() - INTERVAL 30 DAY
GROUP BY tenant_id, psp_code
-- Alert thresholds: 0.5% (yellow) / 0.8% (orange) / 1.0% (red + CFO alert)
```

---

## 3.11 多幣種支付欄位擴充 (C-05)

Deposit/Withdrawal API 新增幣種轉換欄位，配合 Ch2 §2.3.7 FX 服務：

```sql
-- ALTER t_payment_order for multi-currency support
ALTER TABLE t_payment_order ADD COLUMN original_currency VARCHAR(3);
ALTER TABLE t_payment_order ADD COLUMN original_amount DECIMAL(15,2);
ALTER TABLE t_payment_order ADD COLUMN settlement_currency VARCHAR(3);
ALTER TABLE t_payment_order ADD COLUMN settlement_amount DECIMAL(15,2);
ALTER TABLE t_payment_order ADD COLUMN fx_rate DECIMAL(19,8);
ALTER TABLE t_payment_order ADD COLUMN fx_rate_frozen_at TIMESTAMPTZ;
ALTER TABLE t_payment_order ADD COLUMN cross_currency_fee DECIMAL(15,2) DEFAULT 0;
```

**Deposit API Request (updated):**
```json
{
    "playerId": "p_123456",
    "amount": 100.00,
    "currency": "EUR",           // Player's deposit currency
    "settlementCurrency": "USD", // Merchant's fiat (from tenant config)
    "pspCode": "stripe",
    ...
}
```

**Deposit API Response (updated):**
```json
{
    "orderId": "ord_xyz",
    "status": "SUCCESS",
    "originalAmount": 100.00,
    "originalCurrency": "EUR",
    "settlementAmount": 108.50,  // Converted at FX rate
    "settlementCurrency": "USD",
    "fxRate": 1.0850,
    "crossCurrencyFee": 0.00     // Configurable, default 0%
}
```

---

## 3.12 監控指標

### 關鍵績效指標 (KPI)

| 指標 | 目標 | 監控方式 |
|------|------|---------|
| PSP 切換時間 | < 30 秒 | 路由模塊延遲 |
| 存款成功率 | > 95% | 訂單統計 |
| 提款處理時間（自動） | < 15 分鐘 | SAGA 執行時間 |
| 對帳差異率 | < 0.01% | L3 對帳報告 |
| Webhook 處理延遲 | < 5 秒 | 隊列監控 |
| 熔斷器觸發頻率 | < 1/小時 | Resilience4j 指標 |

### Prometheus 指標

```java
@Component
public class PaymentMetricsCollector {

    private final MeterRegistry meterRegistry;

    // 計數器
    private final Counter depositSuccessCounter;
    private final Counter depositFailureCounter;
    private final Counter withdrawalSuccessCounter;
    private final Counter withdrawalFailureCounter;

    // 定時器
    private final Timer routingLatencyTimer;
    private final Timer pspResponseTimer;
    private final Timer callbackProcessingTimer;

    public PaymentMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.depositSuccessCounter = Counter.builder("payment.deposit.success.total")
            .description("Total successful deposits")
            .register(meterRegistry);

        this.depositFailureCounter = Counter.builder("payment.deposit.failure.total")
            .description("Total failed deposits")
            .register(meterRegistry);

        this.routingLatencyTimer = Timer.builder("payment.routing.latency")
            .description("Payment routing latency in milliseconds")
            .register(meterRegistry);

        this.pspResponseTimer = Timer.builder("payment.psp.response")
            .description("PSP response time in milliseconds")
            .register(meterRegistry);
    }

    public void recordDepositSuccess(PaymentOrder order) {
        depositSuccessCounter.increment();
    }

    public void recordDepositFailure(PaymentOrder order) {
        depositFailureCounter.increment();
    }

    public void recordRoutingLatency(long latencyMs) {
        routingLatencyTimer.record(Duration.ofMillis(latencyMs));
    }
}
```

### Grafana 儀表板

監控項：
- 實時交易量（存款、提款）
- PSP 成功率分佈
- 平均手續費成本
- 對帳失配告警
- 系統延遲分佈
- 風控拒絕率

## 3.11 對應業務文檔

詳細業務需求和用例請參考：

> [Requirements: Payment System 支付系統](../requirements/03_Payment_System_支付系統.md)

---

**文檔版本**：v2.0
**最後更新**：2026-03-24
**狀態**：已審核 (Technical Review)
**維護人**：Payment Platform Team
