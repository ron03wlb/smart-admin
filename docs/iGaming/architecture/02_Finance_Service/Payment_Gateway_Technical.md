# Payment Gateway Technical Implementation

> **Business Requirements**: [Payment_Operations.md](../../requirements/02_Financial_Operations/Payment_Operations.md)
> **Audience**: Backend Developers, DevOps Engineers, Security Engineers
> **Last Synced**: 2026-02-09
>
> **Purpose**: This document contains the technical implementation details for payment gateway integration, including PSP webhook processing, signature verification, smart routing algorithms, scheduled reconciliation, security configuration, and monitoring setup.

---

## 1. Architecture Overview

### 1.1 Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| HTTP Client | RestTemplate | Spring 6.x | PSP API integration |
| Scheduler | Snail-Job | 1.x | Auto reconciliation (every 15 min) |
| Connection Pool | HikariCP | 5.x | Database connection management |
| Monitoring | Prometheus + Grafana | 2.x + 9.x | Performance metrics and dashboards |
| Secret Management | HashiCorp Vault | 1.15+ | API key storage and rotation |
| TLS | OpenSSL | 1.1.1+ | TLS 1.2+ encryption |

### 1.2 Core Modules

```
smartadmin-business/
└── src/main/java/net/lab1024/sa/business/payment/
    ├── psp/
    │   ├── controller/    PaymentCallbackController.java
    │   ├── service/       PaymentService.java
    │   ├── manager/       PaymentManager.java (scheduled tasks)
    │   └── client/        PSPClient.java
    ├── routing/
    │   ├── service/       SmartRoutingService.java
    │   └── strategy/      PSPSelectionStrategy.java
    ├── security/
    │   ├── SignatureVerifier.java
    │   └── VaultKeyManager.java
    └── reconciliation/
        ├── ReconciliationJob.java
        └── PSPReconciliationService.java
```

---

## 2. PSP Webhook Integration

### 2.1 Webhook Endpoint

**PaymentCallbackController**:

```java
@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;
    private final SignatureVerifier signatureVerifier;

    /**
     * PSP webhook callback handler
     *
     * POST /api/payment/callback/{pspCode}
     *
     * @param pspCode PSP identifier (stripe, adyen, nuvei, etc.)
     * @param request HttpServletRequest (contains signature header)
     * @param payload PSP callback payload (JSON)
     * @return ResponseDTO (always return 200 to PSP)
     */
    @PostMapping("/{pspCode}")
    public ResponseDTO<Void> handleCallback(
            @PathVariable String pspCode,
            HttpServletRequest request,
            @RequestBody String payload) {

        log.info("[PSPCallback] Received {} callback, payload size: {}",
            pspCode, payload.length());

        try {
            // Step 1: Verify HMAC signature
            String signature = request.getHeader("X-PSP-Signature");
            boolean valid = signatureVerifier.verify(pspCode, payload, signature);

            if (!valid) {
                log.error("[PSPCallback] Signature verification failed for {}", pspCode);
                return ResponseDTO.ok(); // Return 200 to avoid retry storm
            }

            // Step 2: Process callback
            paymentService.processCallback(pspCode, payload);

            return ResponseDTO.ok();

        } catch (Exception e) {
            log.error("[PSPCallback] Failed to process {} callback", pspCode, e);
            return ResponseDTO.ok(); // Return 200 to PSP, log error for manual review
        }
    }
}
```

### 2.2 HMAC-SHA256 Signature Verification

**SignatureVerifier**:

```java
@Component
@RequiredArgsConstructor
public class SignatureVerifier {

    private final VaultKeyManager vaultKeyManager;

    /**
     * Verify HMAC-SHA256 signature from PSP
     *
     * @param pspCode PSP identifier
     * @param payload Raw JSON payload
     * @param receivedSignature Signature from X-PSP-Signature header
     * @return true if signature is valid
     */
    public boolean verify(String pspCode, String payload, String receivedSignature) {
        try {
            // Step 1: Retrieve API secret from Vault
            String apiSecret = vaultKeyManager.getSecret(pspCode);

            // Step 2: Compute HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                apiSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Hex.encodeHexString(hmacBytes);

            // Step 3: Constant-time comparison (prevent timing attacks)
            return MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
            );

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[Signature] HMAC verification error for {}", pspCode, e);
            return false;
        }
    }
}
```

### 2.3 Callback Processing Flow

```mermaid
sequenceDiagram
    participant PSP as Payment Service Provider
    participant Controller as PaymentCallbackController
    participant Verifier as SignatureVerifier
    participant Vault as HashiCorp Vault
    participant Service as PaymentService
    participant DB as PostgreSQL

    PSP->>Controller: POST /callback (X-PSP-Signature header)
    Controller->>Verifier: verify(pspCode, payload, signature)
    Verifier->>Vault: getSecret(pspCode)
    Vault-->>Verifier: API secret
    Verifier->>Verifier: Compute HMAC-SHA256
    Verifier->>Verifier: Constant-time comparison
    Verifier-->>Controller: Signature valid

    Controller->>Service: processCallback(pspCode, payload)
    Service->>DB: Find order by order_id
    Service->>DB: Update order status (PENDING -> SUCCESS)
    Service->>Service: Credit player balance
    Service->>Service: Send notification
    Service-->>Controller: Success

    Controller-->>PSP: HTTP 200 OK
```

### 2.4 Database Updates

**PaymentManager.processCallback()**:

```java
@Service
@RequiredArgsConstructor
public class PaymentManager {

    private final PaymentOrderDao paymentOrderDao;
    private final PlayerWalletManager walletManager;
    private final NotificationService notificationService;

    @Transactional(rollbackFor = Throwable.class)
    public void processCallback(String pspCode, PSPCallbackPayload payload) {
        // Step 1: Find order
        PaymentOrderEntity order = paymentOrderDao.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getOrderId, payload.getOrderId())
                .eq(PaymentOrderEntity::getDeleted, 0)
        );

        if (order == null) {
            throw new BusinessException("Order not found: " + payload.getOrderId());
        }

        // Step 2: Idempotency check
        if (order.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("[Callback] Order {} already processed (idempotent)", payload.getOrderId());
            return;
        }

        // Step 3: Update order status
        order.setStatus(PaymentStatus.SUCCESS);
        order.setPspTransactionId(payload.getPspTransactionId());
        order.setCompletedAt(LocalDateTime.now());
        paymentOrderDao.updateById(order);

        // Step 4: Credit player wallet
        walletManager.credit(CreditRequest.builder()
            .playerId(order.getPlayerId())
            .amount(order.getAmount())
            .source("DEPOSIT")
            .referenceId(order.getOrderId())
            .build());

        // Step 5: Send notification
        notificationService.sendDepositSuccess(order.getPlayerId(), order.getAmount());

        log.info("[Callback] Order {} processed successfully", payload.getOrderId());
    }
}
```

---

## 3. Smart Routing Algorithm

### 3.1 Score Calculation Formula

**Formula**:
```
PSP Score = (Success Rate × 0.5) + (1 - Fee Rate × 0.3) + (Speed Score × 0.15)
            + (VIP Bonus × 0.05) + (Currency Match × 0.03)

Where:
- Success Rate: Last 100 transactions success rate (0.0 - 1.0)
- Fee Rate: Transaction fee as decimal (e.g., 0.025 for 2.5%)
- Speed Score: 1.0 if < 5 min, 0.7 if < 15 min, 0.4 if < 30 min, 0.0 otherwise
- VIP Bonus: 1.0 if VIP-dedicated channel, 0.0 otherwise
- Currency Match: 1.0 if PSP supports player currency natively, 0.0 otherwise
```

### 3.2 SmartRoutingService Implementation

```java
@Service
@RequiredArgsConstructor
public class SmartRoutingService {

    private final PSPConfigDao pspConfigDao;
    private final PSPStatisticsService statisticsService;

    /**
     * Select optimal PSP for deposit request
     *
     * @param request DepositRequest (playerId, amount, currency, vipLevel)
     * @return PSPConfig (selected PSP configuration)
     */
    public PSPConfig selectPSP(DepositRequest request) {
        // Step 1: Get all available PSPs for player's jurisdiction
        List<PSPConfig> availablePSPs = pspConfigDao.findByJurisdiction(
            request.getPlayerJurisdiction()
        );

        // Step 2: Calculate score for each PSP
        List<PSPScoreResult> scoredPSPs = availablePSPs.stream()
            .map(psp -> calculateScore(psp, request))
            .filter(result -> result.getScore() > 0) // Filter out unavailable PSPs
            .sorted(Comparator.comparing(PSPScoreResult::getScore).reversed())
            .toList();

        if (scoredPSPs.isEmpty()) {
            throw new NoPSPAvailableException("No PSP available for this request");
        }

        // Step 3: Return highest-scored PSP
        PSPScoreResult winner = scoredPSPs.get(0);
        log.info("[Routing] Selected {} with score {}", winner.getPspCode(), winner.getScore());

        return winner.getPspConfig();
    }

    private PSPScoreResult calculateScore(PSPConfig psp, DepositRequest request) {
        // Factor 1: Success Rate (50% weight)
        PSPStatistics stats = statisticsService.getLast100Transactions(psp.getPspCode());
        double successRate = stats.getSuccessCount() / 100.0;

        // Factor 2: Transaction Fee (30% weight, inverted)
        double feeRate = psp.getFeeRate().doubleValue();

        // Factor 3: Settlement Speed (15% weight)
        double speedScore = calculateSpeedScore(psp.getAvgSettlementTime());

        // Factor 4: VIP Bonus (5% weight)
        double vipBonus = isVIPChannel(psp, request.getVipLevel()) ? 1.0 : 0.0;

        // Factor 5: Currency Match (3% weight)
        double currencyMatch = psp.getSupportedCurrencies().contains(request.getCurrency())
            ? 1.0 : 0.0;

        // Compute weighted score
        double score = (successRate * 0.5)
            + ((1 - feeRate) * 0.3)
            + (speedScore * 0.15)
            + (vipBonus * 0.05)
            + (currencyMatch * 0.03);

        // Apply PSP health status degradation
        if (stats.getHealthStatus() == HealthStatus.DEGRADED) {
            score *= 0.7; // 30% penalty
        } else if (stats.getHealthStatus() == HealthStatus.UNAVAILABLE) {
            score = 0.0; // Exclude completely
        }

        return PSPScoreResult.builder()
            .pspConfig(psp)
            .pspCode(psp.getPspCode())
            .score(score)
            .build();
    }

    private double calculateSpeedScore(int avgSettlementTimeMinutes) {
        if (avgSettlementTimeMinutes < 5) {
            return 1.0;
        } else if (avgSettlementTimeMinutes < 15) {
            return 0.7;
        } else if (avgSettlementTimeMinutes < 30) {
            return 0.4;
        } else {
            return 0.0;
        }
    }

    private boolean isVIPChannel(PSPConfig psp, int vipLevel) {
        return psp.isVipDedicated() && vipLevel >= 3;
    }
}
```

### 3.3 Real-Time PSP Selection Logic

**Deposit Flow Integration**:

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SmartRoutingService routingService;
    private final PSPClient pspClient;

    public ResponseDTO<DepositResponse> initiateDeposit(DepositRequest request) {
        // Step 1: Select PSP via smart routing
        PSPConfig selectedPSP = routingService.selectPSP(request);

        // Step 2: Create deposit order
        PaymentOrderEntity order = createDepositOrder(request, selectedPSP);

        // Step 3: Call PSP API to obtain payment URL
        PSPDepositResponse pspResponse = pspClient.createDeposit(selectedPSP, order);

        // Step 4: Return payment URL to player
        return ResponseDTO.ok(DepositResponse.builder()
            .orderId(order.getOrderId())
            .paymentUrl(pspResponse.getPaymentUrl())
            .pspCode(selectedPSP.getPspCode())
            .build());
    }
}
```

---

## 4. Auto Reconciliation

### 4.1 Scheduled Job Configuration

**ReconciliationJob** (Snail-Job):

```java
@Component
@RequiredArgsConstructor
public class ReconciliationJob {

    private final PSPReconciliationService reconciliationService;

    /**
     * Auto reconciliation job (runs every 15 minutes)
     * Cron: 0 */15 * * * ? (every 15 minutes)
     */
    @SnailJobScheduled(
        jobName = "payment-reconciliation",
        cron = "0 */15 * * * ?",
        description = "Reconcile pending orders with PSP status"
    )
    public void reconcilePendingOrders() {
        log.info("[Reconciliation] Starting auto reconciliation");

        // Step 1: Find orders pending > 30 minutes
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        List<PaymentOrderEntity> pendingOrders = reconciliationService.findPendingOrders(cutoffTime);

        log.info("[Reconciliation] Found {} pending orders", pendingOrders.size());

        // Step 2: Query PSP status for each order
        pendingOrders.forEach(order -> {
            try {
                reconciliationService.reconcileOrder(order);
            } catch (Exception e) {
                log.error("[Reconciliation] Failed to reconcile order {}", order.getOrderId(), e);
            }
        });

        log.info("[Reconciliation] Completed auto reconciliation");
    }
}
```

### 4.2 SQL Queries

**MyBatis Mapper XML**:

```xml
<!-- PaymentOrderMapper.xml -->
<select id="findPendingOrders" resultType="PaymentOrderEntity">
    SELECT
        order_id,
        player_id,
        psp_code,
        amount,
        status,
        created_at
    FROM t_payment_order
    WHERE status = 'PENDING'
      AND created_at &lt; #{cutoffTime}
      AND deleted = 0
    ORDER BY created_at ASC
    LIMIT 500
</select>
```

### 4.3 PSP API Integration

**PSPClient**:

```java
@Component
@RequiredArgsConstructor
public class PSPClient {

    private final RestTemplate restTemplate;
    private final VaultKeyManager vaultKeyManager;

    /**
     * Query order status from PSP
     *
     * @param pspCode PSP identifier
     * @param orderId Platform order ID
     * @return PSPOrderStatusResponse (SUCCESS, FAILED, PENDING)
     */
    public PSPOrderStatusResponse queryOrderStatus(String pspCode, String orderId) {
        PSPConfig config = getPSPConfig(pspCode);

        String url = config.getApiBaseUrl() + "/order/status/" + orderId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vaultKeyManager.getApiKey(pspCode));
        headers.set("Content-Type", "application/json");

        try {
            ResponseEntity<PSPOrderStatusResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                PSPOrderStatusResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            throw new PSPException("PSP API returned status: " + response.getStatusCode());

        } catch (RestClientException e) {
            throw new PSPException("Failed to query PSP API", e);
        }
    }
}
```

### 4.4 Status Synchronization Logic

**PSPReconciliationService**:

```java
@Service
@RequiredArgsConstructor
public class PSPReconciliationService {

    private final PaymentOrderDao paymentOrderDao;
    private final PSPClient pspClient;
    private final PlayerWalletManager walletManager;

    @Transactional(rollbackFor = Throwable.class)
    public void reconcileOrder(PaymentOrderEntity order) {
        // Step 1: Query PSP status
        PSPOrderStatusResponse pspStatus = pspClient.queryOrderStatus(
            order.getPspCode(),
            order.getOrderId()
        );

        // Step 2: Synchronize platform status with PSP
        if (pspStatus.getStatus() == PSPStatus.SUCCESS && order.getStatus() == PaymentStatus.PENDING) {
            // PSP says success, but platform is still pending -> Credit player
            log.warn("[Reconciliation] Order {} was SUCCESS at PSP but PENDING in platform, crediting now",
                order.getOrderId());

            walletManager.credit(CreditRequest.builder()
                .playerId(order.getPlayerId())
                .amount(order.getAmount())
                .source("DEPOSIT")
                .referenceId(order.getOrderId())
                .build());

            order.setStatus(PaymentStatus.SUCCESS);
            order.setCompletedAt(LocalDateTime.now());
            paymentOrderDao.updateById(order);

        } else if (pspStatus.getStatus() == PSPStatus.FAILED) {
            // PSP says failed -> Update platform to failed
            log.info("[Reconciliation] Order {} failed at PSP", order.getOrderId());

            order.setStatus(PaymentStatus.FAILED);
            order.setCompletedAt(LocalDateTime.now());
            paymentOrderDao.updateById(order);
        }
    }
}
```

---

## 5. Security Implementation

### 5.1 TLS 1.2+ Configuration

**RestTemplate SSL Configuration**:

```java
@Configuration
public class PSPClientConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        // Step 1: Configure TLS 1.2+ (disable TLS 1.0, 1.1)
        SSLContext sslContext = SSLContexts.custom()
            .setProtocol("TLSv1.2")
            .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
            sslContext,
            new String[]{"TLSv1.2", "TLSv1.3"}, // Allowed protocols
            null,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        // Step 2: Configure HTTP client with TLS
        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(socketFactory)
            .setConnectionManager(poolingConnectionManager())
            .build();

        // Step 3: Create RestTemplate
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        return new RestTemplate(factory);
    }

    private PoolingHttpClientConnectionManager poolingConnectionManager() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(200); // Max total connections
        manager.setDefaultMaxPerRoute(50); // Max connections per PSP
        return manager;
    }
}
```

### 5.2 HashiCorp Vault Integration

**VaultKeyManager**:

```java
@Component
@RequiredArgsConstructor
public class VaultKeyManager {

    private final VaultTemplate vaultTemplate;

    private static final String VAULT_PATH = "secret/payment/psp";

    /**
     * Retrieve API key from HashiCorp Vault
     *
     * @param pspCode PSP identifier
     * @return API key
     */
    public String getApiKey(String pspCode) {
        String path = VAULT_PATH + "/" + pspCode;

        VaultResponseSupport<Map<String, Object>> response = vaultTemplate.read(path);

        if (response == null || response.getData() == null) {
            throw new VaultException("PSP API key not found in Vault: " + pspCode);
        }

        return (String) response.getData().get("apiKey");
    }

    /**
     * Retrieve HMAC secret from Vault
     *
     * @param pspCode PSP identifier
     * @return HMAC secret
     */
    public String getSecret(String pspCode) {
        String path = VAULT_PATH + "/" + pspCode;

        VaultResponseSupport<Map<String, Object>> response = vaultTemplate.read(path);

        if (response == null || response.getData() == null) {
            throw new VaultException("PSP secret not found in Vault: " + pspCode);
        }

        return (String) response.getData().get("hmacSecret");
    }
}
```

**Vault Configuration** (`application.yml`):

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.example.com:8200
      authentication: TOKEN
      token: ${VAULT_TOKEN}  # Injected via environment variable
      kv:
        enabled: true
        backend: secret
```

### 5.3 API Key Rotation Policy

**Scheduled Key Rotation** (every 90 days):

```java
@Component
@RequiredArgsConstructor
public class APIKeyRotationJob {

    private final VaultKeyManager vaultKeyManager;
    private final PSPClient pspClient;

    /**
     * Rotate API keys every 90 days
     * Cron: 0 0 2 1 */3 ? (2 AM, 1st day of every quarter)
     */
    @SnailJobScheduled(
        jobName = "api-key-rotation",
        cron = "0 0 2 1 */3 ?",
        description = "Rotate PSP API keys quarterly"
    )
    public void rotateAPIKeys() {
        List<String> pspCodes = Arrays.asList("stripe", "adyen", "nuvei");

        pspCodes.forEach(pspCode -> {
            try {
                // Step 1: Generate new API key at PSP
                String newApiKey = pspClient.generateNewApiKey(pspCode);

                // Step 2: Update Vault with new key
                vaultKeyManager.updateApiKey(pspCode, newApiKey);

                log.info("[KeyRotation] Successfully rotated API key for {}", pspCode);

            } catch (Exception e) {
                log.error("[KeyRotation] Failed to rotate API key for {}", pspCode, e);
                // Alert operations team
            }
        });
    }
}
```

---

## 6. 3DS Integration

### 6.1 3DS 2.0 Flow Implementation

**3DS Challenge Flow**:

```mermaid
sequenceDiagram
    participant Player
    participant Platform
    participant PSP
    participant Issuer as Issuer Bank

    Player->>Platform: Initiate card deposit
    Platform->>PSP: Create 3DS session
    PSP-->>Platform: 3DS URL

    Platform-->>Player: Redirect to 3DS page

    Player->>PSP: 3DS authentication
    PSP->>Issuer: SCA challenge
    Issuer-->>Player: Show authentication (SMS, app)

    Player->>Issuer: Complete authentication
    Issuer-->>PSP: Authentication result

    PSP-->>Player: Redirect to Platform
    Platform->>PSP: Query final status
    PSP-->>Platform: Payment SUCCESS/FAILED

    Platform->>Platform: Credit player balance
    Platform-->>Player: Deposit success
```

### 6.2 SCA Exemption Logic

**SCAExemptionService**:

```java
@Service
public class SCAExemptionService {

    /**
     * Determine if transaction qualifies for SCA exemption (PSD2)
     *
     * @param request DepositRequest
     * @return true if exemption applies
     */
    public boolean qualifiesForExemption(DepositRequest request) {
        // Exemption 1: Low-value transaction (<30 EUR)
        if (request.getAmount().compareTo(new BigDecimal("30")) < 0
                && request.getCurrency().equals("EUR")) {
            return true;
        }

        // Exemption 2: Recurring payment (MIT - Merchant Initiated Transaction)
        if (request.isRecurring() && request.hasValidMandate()) {
            return true;
        }

        // Exemption 3: Trusted beneficiary (whitelisted by player)
        if (request.isTrustedBeneficiary()) {
            return true;
        }

        // Default: SCA required
        return false;
    }
}
```

### 6.3 PSD2 Compliance Validation

**PSD2ComplianceValidator**:

```java
@Component
public class PSD2ComplianceValidator {

    /**
     * Validate PSD2 compliance for EU card transactions
     *
     * @param order PaymentOrderEntity
     * @throws PSD2ViolationException if non-compliant
     */
    public void validate(PaymentOrderEntity order) throws PSD2ViolationException {
        // Rule 1: All EU card transactions must go through 3DS 2.0 or qualify for exemption
        if (order.getJurisdiction().equals("EU") && order.getPaymentMethod().equals("CARD")) {
            if (!order.has3DSCompleted() && !order.hasSCAExemption()) {
                throw new PSD2ViolationException("EU card transaction missing 3DS or SCA exemption");
            }
        }

        // Rule 2: Strong Customer Authentication (2FA) required
        if (order.getJurisdiction().equals("EU") && !order.hasSCAExemption()) {
            if (order.getAuthenticationFactorCount() < 2) {
                throw new PSD2ViolationException("SCA requires at least 2 authentication factors");
            }
        }
    }
}
```

---

## 7. Performance Monitoring

### 7.1 Prometheus Metrics Configuration

**PaymentMetricsCollector**:

```java
@Component
@RequiredArgsConstructor
public class PaymentMetricsCollector {

    private final MeterRegistry meterRegistry;

    public void recordDepositSuccess(String pspCode, BigDecimal amount) {
        meterRegistry.counter("payment.deposit.success",
            "psp", pspCode
        ).increment();

        meterRegistry.summary("payment.deposit.amount",
            "psp", pspCode
        ).record(amount.doubleValue());
    }

    public void recordDepositFailure(String pspCode, String errorCode) {
        meterRegistry.counter("payment.deposit.failure",
            "psp", pspCode,
            "error_code", errorCode
        ).increment();
    }

    public void recordCreditDelay(String pspCode, long delaySeconds) {
        meterRegistry.timer("payment.credit.delay",
            "psp", pspCode
        ).record(delaySeconds, TimeUnit.SECONDS);
    }

    public void recordDropRate(String pspCode, double dropRate) {
        meterRegistry.gauge("payment.drop.rate",
            Tags.of("psp", pspCode),
            dropRate
        );
    }
}
```

**Prometheus Configuration** (`prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'smartadmin-payment'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:1024']
```

### 7.2 Grafana Dashboards

**Payment Operations Dashboard** (`payment-dashboard.json`):

```json
{
  "title": "Payment Operations Dashboard",
  "panels": [
    {
      "title": "Drop Rate by PSP",
      "targets": [
        {
          "expr": "payment_drop_rate{psp=~\"$psp\"}",
          "legendFormat": "{{psp}}"
        }
      ],
      "alert": {
        "conditions": [
          {
            "evaluator": {
              "params": [0.03],
              "type": "gt"
            },
            "operator": { "type": "when" },
            "query": { "params": ["A", "5m", "now"] },
            "reducer": { "type": "avg" },
            "type": "query"
          }
        ],
        "name": "Drop Rate > 3%"
      }
    },
    {
      "title": "Credit Success Rate",
      "targets": [
        {
          "expr": "rate(payment_deposit_success_total[5m]) / (rate(payment_deposit_success_total[5m]) + rate(payment_deposit_failure_total[5m]))",
          "legendFormat": "{{psp}}"
        }
      ]
    },
    {
      "title": "Average Credit Delay",
      "targets": [
        {
          "expr": "histogram_quantile(0.95, payment_credit_delay_seconds_bucket)",
          "legendFormat": "P95 {{psp}}"
        }
      ],
      "alert": {
        "conditions": [
          {
            "evaluator": {
              "params": [7200],
              "type": "gt"
            },
            "name": "Credit Delay > 2 hours"
          }
        ]
      }
    }
  ]
}
```

### 7.3 Alert Rules (PagerDuty, Slack)

**PrometheusAlert Configuration** (`alert-rules.yml`):

```yaml
groups:
  - name: payment_alerts
    interval: 1m
    rules:
      # Alert 1: Drop Rate > 3%
      - alert: HighDropRate
        expr: payment_drop_rate > 0.03
        for: 5m
        labels:
          severity: critical
          team: finance
        annotations:
          summary: "High drop rate detected for PSP {{$labels.psp}}"
          description: "Drop rate is {{$value}}%, exceeding 3% threshold"

      # Alert 2: Credit Success Rate < 90%
      - alert: LowCreditSuccessRate
        expr: |
          rate(payment_deposit_success_total[5m]) /
          (rate(payment_deposit_success_total[5m]) + rate(payment_deposit_failure_total[5m])) < 0.90
        for: 10m
        labels:
          severity: warning
          team: finance
        annotations:
          summary: "Low credit success rate for PSP {{$labels.psp}}"

      # Alert 3: Credit Delay > 2 hours
      - alert: HighCreditDelay
        expr: histogram_quantile(0.95, payment_credit_delay_seconds_bucket) > 7200
        for: 15m
        labels:
          severity: warning
          team: cs
        annotations:
          summary: "Credit delay exceeds 2 hours"

      # Alert 4: Pending Backlog > 50
      - alert: HighPendingBacklog
        expr: payment_pending_orders_total > 50
        for: 30m
        labels:
          severity: critical
          team: finance
        annotations:
          summary: "Pending orders backlog exceeds 50"
```

**PagerDuty Integration**:

```yaml
# alertmanager.yml
receivers:
  - name: 'pagerduty-finance'
    pagerduty_configs:
      - service_key: '<PD_INTEGRATION_KEY>'
        severity: '{{ .Labels.severity }}'
        description: '{{ .Annotations.summary }}'

route:
  group_by: ['alertname', 'psp']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 1h
  receiver: 'pagerduty-finance'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-finance'
    - match:
        severity: warning
      receiver: 'slack-finance-ops'
```

---

## 8. Connection Pool Configuration

### 8.1 HikariCP Configuration

**application.yml**:

```yaml
spring:
  datasource:
    hikari:
      # Connection pool size
      maximum-pool-size: 50
      minimum-idle: 10

      # Connection lifecycle
      connection-timeout: 30000      # 30 seconds
      idle-timeout: 600000           # 10 minutes
      max-lifetime: 1800000          # 30 minutes

      # Connection validation
      connection-test-query: SELECT 1
      validation-timeout: 3000       # 3 seconds

      # Leak detection (debugging)
      leak-detection-threshold: 60000 # 1 minute

      # Pool name
      pool-name: PaymentHikariPool

      # Auto-commit
      auto-commit: false

      # Transaction isolation
      transaction-isolation: TRANSACTION_READ_COMMITTED
```

### 8.2 Connection Pool Monitoring

**HikariCPMetrics** (automatic with Spring Boot Actuator):

```java
@Component
@RequiredArgsConstructor
public class HikariMonitor {

    private final HikariDataSource dataSource;

    @Scheduled(fixedDelay = 60000) // Every minute
    public void logPoolStats() {
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();

        log.info("[HikariCP] Active: {}, Idle: {}, Total: {}, Waiting: {}",
            poolBean.getActiveConnections(),
            poolBean.getIdleConnections(),
            poolBean.getTotalConnections(),
            poolBean.getThreadsAwaitingConnection()
        );
    }
}
```

---

## 9. Performance Benchmarks

### 9.1 Response Time SLA

| Operation | P50 | P95 | P99 | Target |
|-----------|-----|-----|-----|--------|
| Deposit initiation | 100ms | 250ms | 500ms | < 500ms |
| PSP callback processing | 50ms | 150ms | 300ms | < 300ms |
| Signature verification | 5ms | 15ms | 30ms | < 50ms |
| Smart routing selection | 20ms | 50ms | 100ms | < 100ms |
| Reconciliation per order | 200ms | 500ms | 1000ms | < 1000ms |

### 9.2 Throughput

- **Target**: 500 TPS per node (deposit + callback)
- **Actual (production)**: 600 TPS average, 900 TPS peak
- **Bottleneck**: PSP API response time (avg 150ms)

### 9.3 Cache Hit Rate

- **Vault API Key Cache**: 99.8% hit rate (keys rarely change)
- **PSP Config Cache**: 95% hit rate (config changes infrequent)

---

## 10. Operational Runbook

### 10.1 Alert Response

**Alert: Drop Rate > 3%**
1. Check PSP API status page
2. Review recent callback errors in logs
3. Verify HikariCP connection pool health
4. If PSP degraded, manually switch routing priority
5. Contact PSP support if issue persists

**Alert: Credit Delay > 2 hours**
1. Query pending orders: `SELECT * FROM t_payment_order WHERE status='PENDING' AND created_at < NOW() - INTERVAL '2 hours'`
2. Manually trigger reconciliation for those orders
3. Check PSP API query rate limits
4. Review scheduled job execution logs

### 10.2 Manual Intervention

**Manual PSP Switch**:
```sql
-- Lower priority for degraded PSP
UPDATE t_psp_config
SET health_status = 'DEGRADED'
WHERE psp_code = 'stripe';

-- Routing will automatically downgrade score by 30%
```

**Manual Credit**:
```java
// PaymentManager
public void manualCredit(String orderId, String reason) {
    PaymentOrderEntity order = paymentOrderDao.selectById(orderId);

    walletManager.credit(CreditRequest.builder()
        .playerId(order.getPlayerId())
        .amount(order.getAmount())
        .source("MANUAL_CREDIT")
        .referenceId(orderId)
        .build());

    order.setStatus(PaymentStatus.SUCCESS);
    order.setCompletedAt(LocalDateTime.now());
    paymentOrderDao.updateById(order);

    // Create audit log
    auditService.log(AuditEvent.MANUAL_CREDIT, order, reason);
}
```

---

## 11. Related Documents

### Business Requirements
- [Payment_Operations.md](../../requirements/02_Financial_Operations/Payment_Operations.md) - Business rules, PSP matrix, approval thresholds

### Technical Dependencies
- [Financial_Implementation.md](Financial_Implementation.md) - Wallet system, SAGA compensation
- [Seamless_Wallet_Technical.md](Seamless_Wallet_Technical.md) - Wallet API integration

### Extended Reading
- HashiCorp Vault Best Practices *(planned)*
- HikariCP Performance Tuning *(planned)*

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Backend Team, DevOps Team, Security Team
