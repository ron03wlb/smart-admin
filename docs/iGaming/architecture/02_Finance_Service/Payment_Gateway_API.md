# Payment Gateway API

> **Canonical Source**: [source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md](../../source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: [Payment_Operations.md](../../requirements/02_Financial_Operations/Payment_Operations.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## 1. System Architecture Overview

The Payment Gateway integrates with multiple external PSPs (Payment Service Providers) to handle deposit and withdrawal transactions. Key architectural goals:

- Multi-PSP support with intelligent routing
- Fault-tolerant failover mechanism
- Idempotent transaction processing
- PCI-DSS compliant data handling

---

## 2. Deposit Flow Sequence

### 2.1 Complete Deposit Flow

The following sequence diagram shows the complete flow from player deposit initiation to balance crediting, including PSP routing, signature verification, callback handling, and idempotency protection:

```mermaid
sequenceDiagram
    participant Player
    participant Frontend
    participant API Gateway
    participant PaymentService
    participant PSP Router
    participant PSP (Nuvei)
    participant Webhook Handler
    participant WalletService
    participant Redis
    participant DB
    participant Notification

    Note over Player,Notification: Deposit Flow - From Request to Credit

    Player->>Frontend: Click "Deposit" (Amount: $100, Method: Credit Card)
    Frontend->>API Gateway: POST /api/v1/payments/deposit<br/>{amount: 100, currency: USD, payment_method: credit_card}

    API Gateway->>PaymentService: createDepositOrder(playerId, amount, method)
    activate PaymentService

    PaymentService->>PSP Router: selectBestPSP(player, amount, method, country)
    activate PSP Router

    Note over PSP Router: Smart Routing Algorithm (See Section 3)

    PSP Router->>PSP Router: Calculate PSP Scores:<br/>Nuvei: 85, Adyen: 78, Stripe: 72
    PSP Router-->>PaymentService: Selected PSP: Nuvei (Score: 85)
    deactivate PSP Router

    PaymentService->>DB: BEGIN TRANSACTION
    PaymentService->>DB: INSERT INTO transactions<br/>(id, player_id, amount, status, psp_code, created_at)<br/>VALUES (txn_20260127_001, player_123, 100, 'PENDING', 'nuvei', NOW())

    PaymentService->>PaymentService: Generate Checksum:<br/>SHA256(merchantId + amount + currency + timestamp + secret)

    PaymentService->>PSP (Nuvei): POST /ppp/api/v1/payment.do<br/>{merchantId, amount, currency, checksum, returnUrl}
    PSP (Nuvei)-->>PaymentService: {status: OK, redirect_url, payment_token, expires_at}

    PaymentService->>DB: UPDATE transactions SET<br/>psp_order_id = payment_token,<br/>redirect_url = redirect_url,<br/>expires_at = NOW() + 15min

    PaymentService->>DB: COMMIT
    PaymentService-->>API Gateway: {transaction_id, redirect_url, expires_at}
    deactivate PaymentService

    API Gateway-->>Frontend: 200 OK {redirect_url}
    Frontend->>PSP (Nuvei): 302 Redirect to Payment Page

    Note over Player,PSP (Nuvei): Player fills in card details & completes 3DS verification

    Player->>PSP (Nuvei): Submit Card Details (Card: 4111****1111, CVV: 123)
    PSP (Nuvei)->>PSP (Nuvei): Process Payment (3-30 seconds)

    alt Payment Success
        PSP (Nuvei)->>Webhook Handler: POST /webhook/deposit<br/>X-PSP-Signature: HMAC-SHA256<br/>Body: {transaction_id, status: APPROVED, psp_txn_id, amount}

        activate Webhook Handler

        Webhook Handler->>Webhook Handler: Step 1: Verify IP Whitelist<br/>(Request IP in PSP_IPS?)

        alt IP Not Whitelisted
            Webhook Handler-->>PSP (Nuvei): 403 Forbidden
            Webhook Handler->>Notification: Alert Security Team (Suspicious IP)
        end

        Webhook Handler->>Webhook Handler: Step 2: Verify HMAC Signature<br/>Expected = HMAC(body, webhook_secret)<br/>Received = X-PSP-Signature

        alt Signature Mismatch
            Webhook Handler-->>PSP (Nuvei): 403 Forbidden (Invalid Signature)
            Webhook Handler->>Notification: Alert Security Team (Signature Forgery)
        end

        Webhook Handler->>Webhook Handler: Step 3: Verify Timestamp<br/>(NOW() - request_timestamp < 5min?)

        alt Timestamp Expired
            Webhook Handler-->>PSP (Nuvei): 400 Bad Request (Replay Attack)
        end

        Webhook Handler->>Redis: SET NX callback:txn_20260127_001 1 EX 60
        Redis-->>Webhook Handler: OK (Lock Acquired)

        alt Lock Failed (Duplicate Callback)
            Redis-->>Webhook Handler: NIL (Already Processing)
            Webhook Handler-->>PSP (Nuvei): 200 OK {status: ALREADY_PROCESSED}
        end

        Webhook Handler->>DB: SELECT * FROM transactions<br/>WHERE id = txn_20260127_001 FOR UPDATE
        DB-->>Webhook Handler: {status: PENDING, player_id: player_123, amount: 100}

        alt Status != PENDING (Idempotent Check)
            Webhook Handler-->>PSP (Nuvei): 200 OK {status: ALREADY_PROCESSED}
        else Status = PENDING (Normal Path)
            Webhook Handler->>DB: BEGIN TRANSACTION

            Webhook Handler->>DB: UPDATE transactions SET<br/>status = 'SUCCESS',<br/>psp_transaction_id = psp_txn_id,<br/>completed_at = NOW()

            Webhook Handler->>WalletService: creditBalance(player_id: player_123, amount: 100, ref_id: txn_20260127_001)
            activate WalletService
            WalletService->>DB: UPDATE player_wallet SET<br/>balance = balance + 100,<br/>version = version + 1<br/>WHERE player_id = player_123 AND version = current_version
            WalletService->>DB: INSERT INTO wallet_transactions<br/>(player_id, type, amount, ref_id, created_at)<br/>VALUES (player_123, 'DEPOSIT', 100, txn_20260127_001, NOW())
            WalletService-->>Webhook Handler: {status: OK, new_balance: 1100}
            deactivate WalletService

            Webhook Handler->>DB: INSERT INTO payment_audit_log<br/>(transaction_id, event, details, created_at)<br/>VALUES (txn_20260127_001, 'WEBHOOK_RECEIVED', {...}, NOW())

            Webhook Handler->>DB: COMMIT

            Webhook Handler->>Notification: sendDepositSuccessNotification(player_123, amount: 100)
            Notification->>Player: Email + SMS: "Deposit $100 Successful. New Balance: $1,100"

            Webhook Handler-->>PSP (Nuvei): 200 OK {status: SUCCESS}
        end

        Webhook Handler->>Redis: DEL callback:txn_20260127_001
        deactivate Webhook Handler

        PSP (Nuvei)->>Frontend: 302 Redirect to Return URL (success_page)
        Frontend->>Player: Display "Deposit Successful" + New Balance

    else Payment Failed
        PSP (Nuvei)->>Webhook Handler: POST /webhook/deposit<br/>Body: {transaction_id, status: DECLINED, error_code: INSUFFICIENT_FUNDS}

        Webhook Handler->>DB: UPDATE transactions SET<br/>status = 'FAILED',<br/>error_code = 'INSUFFICIENT_FUNDS',<br/>completed_at = NOW()

        Webhook Handler->>Notification: sendDepositFailedNotification(player_123, reason: INSUFFICIENT_FUNDS)
        Notification->>Player: Email: "Deposit Failed. Please try another card."

        Webhook Handler-->>PSP (Nuvei): 200 OK {status: RECEIVED}

        PSP (Nuvei)->>Frontend: 302 Redirect to Return URL (failure_page)
        Frontend->>Player: Display "Payment Failed. Reason: Insufficient Funds"

    else Payment Timeout (No Callback Received)
        Note over PaymentService,DB: Scheduled Job - Reconciliation (Every 15 min)

        PaymentService->>DB: SELECT * FROM transactions<br/>WHERE status = 'PENDING'<br/>AND created_at < NOW() - INTERVAL '30 minutes'
        DB-->>PaymentService: [txn_20260127_001, ...]

        loop For each Pending Transaction
            PaymentService->>PSP (Nuvei): GET /api/v1/query?transaction_id=txn_20260127_001<br/>Authorization: HMAC-SHA256

            alt PSP Status: APPROVED
                PSP (Nuvei)-->>PaymentService: {status: APPROVED, psp_txn_id, amount: 100}
                PaymentService->>PaymentService: Trigger Manual Credit Flow (Same as Webhook Handler)
                PaymentService->>WalletService: creditBalance(player_123, 100)
                PaymentService->>DB: UPDATE transactions SET status = 'SUCCESS'
                PaymentService->>Notification: Send Manual Credit Notification
            else PSP Status: DECLINED
                PSP (Nuvei)-->>PaymentService: {status: DECLINED}
                PaymentService->>DB: UPDATE transactions SET status = 'FAILED'
            else PSP Status: PENDING
                PSP (Nuvei)-->>PaymentService: {status: PENDING}
                PaymentService->>PaymentService: Keep PENDING, retry later
            end
        end
    end
```

### 2.2 Key Design Points

| Phase | Key Step | Security Mechanism | Performance Target |
|-------|----------|-------------------|-------------------|
| **1. PSP Routing** | Smart PSP selection | Multi-dimensional scoring (success rate, cost, speed) | < 100ms |
| **2. Order Creation** | Generate unique transaction_id | DB unique constraint prevents duplicates | < 50ms |
| **3. Signature Generation** | SHA256 checksum | Prevents request tampering | < 10ms |
| **4. PSP Request** | Obtain redirect_url | HTTPS + TLS 1.2+ | < 500ms |
| **5. Callback Verification** | 3-layer validation (IP, signature, timestamp) | Prevents forgery, replay attacks | < 50ms |
| **6. Idempotency** | Redis SET NX lock | Prevents duplicate callbacks | < 10ms |
| **7. Balance Update** | Optimistic locking (version) | Prevents concurrent conflicts | < 100ms |
| **8. Reconciliation** | Scheduled PSP status query | Prevents dropped transactions | Every 15 min |

---

## 3. Smart Routing Algorithm

### 3.1 Routing Architecture Overview

```mermaid
flowchart LR
    START[Player Deposit Request] --> INPUT["Input Parameters<br/>-----<br/>Amount: $1000<br/>Currency: USD<br/>Country: US<br/>Payment Method: Credit Card<br/>VIP Level: 2"]

    INPUT --> STEP1["Step 1<br/>Country & Payment<br/>Method Filtering<br/>-----<br/>Filter by Geography<br/>& Payment Type"]

    STEP1 --> CANDIDATES["Candidate PSPs<br/>-----<br/>Nuvei<br/>Adyen<br/>Stripe<br/>(3 candidates)"]

    CANDIDATES --> STEP2["Step 2<br/>Health Status Check<br/>-----<br/>Filter Unavailable PSPs"]

    STEP2 --> AVAILABLE["Available PSPs<br/>-----<br/>Nuvei: Healthy<br/>Adyen: Healthy<br/>Stripe: Degraded<br/>(2 healthy)"]

    AVAILABLE --> STEP3["Step 3<br/>Multi-Dimensional<br/>Scoring Algorithm<br/>-----<br/>5 Dimensions Evaluation"]

    STEP3 --> RANKED["Ranked PSPs<br/>-----<br/>1. Nuvei: 92.25<br/>2. Adyen: 78.0<br/>3. Stripe: 72.0"]

    RANKED --> STEP4["Step 4<br/>Final Health Check<br/>-----<br/>Verify Top PSP Status"]

    STEP4 --> DECISION{Top PSP Status?}

    DECISION -->|Healthy| ROUTE["Route to Nuvei<br/>-----<br/>Payment URL Generated<br/>Settlement ETA: 10 min"]
    DECISION -->|Degraded/Down| FALLBACK["Fallback to Adyen<br/>-----<br/>Retry with 2nd PSP"]

    FALLBACK --> DECISION

    ROUTE --> LOG["Log Routing Decision<br/>-----<br/>Selected: Nuvei<br/>Score: 92.25<br/>Fallback Queue: Adyen, Stripe"]

    LOG --> RETURN["Return to Gateway<br/>-----<br/>Redirect URL + ETA"]

    RETURN --> END1[End - Success]
```

### 3.2 Country & Payment Method Filtering

```mermaid
flowchart TD
    START[Step 1: Filter PSPs] --> INPUT[Input: Country, Payment Method]

    INPUT --> COUNTRY{Country?}

    COUNTRY -->|US| US["US PSPs<br/>-----<br/>Stripe<br/>Nuvei<br/>PayPal<br/>Coinbase"]
    COUNTRY -->|EU| EU["EU PSPs<br/>-----<br/>Adyen<br/>Trustly<br/>Klarna<br/>Skrill"]
    COUNTRY -->|CN| CN["CN PSPs<br/>-----<br/>Alipay<br/>WeChat Pay<br/>UnionPay"]
    COUNTRY -->|PH| PH["PH PSPs<br/>-----<br/>GCash<br/>PayMaya<br/>GrabPay"]
    COUNTRY -->|BR| BR["BR PSPs<br/>-----<br/>PagSeguro<br/>MercadoPago<br/>Pix"]
    COUNTRY -->|JP| JP["JP PSPs<br/>-----<br/>PayPay<br/>Line Pay<br/>Rakuten Pay"]
    COUNTRY -->|Other| GLOBAL["Global PSPs<br/>-----<br/>Stripe<br/>Adyen<br/>Nuvei"]

    US --> METHOD
    EU --> METHOD
    CN --> METHOD
    PH --> METHOD
    BR --> METHOD
    JP --> METHOD
    GLOBAL --> METHOD

    METHOD{Payment Method?}

    METHOD -->|Credit Card| CARD["PSPs Supporting Cards<br/>-----<br/>Example: Stripe, Adyen, Nuvei<br/>Card Networks: VISA, MC, AMEX"]
    METHOD -->|E-Wallet| EWALLET["PSPs Supporting E-Wallets<br/>-----<br/>Example: PayPal, Skrill, Neteller<br/>Local: Alipay, WeChat, GCash"]
    METHOD -->|Bank Transfer| BANK["PSPs Supporting Bank Transfers<br/>-----<br/>Example: Trustly, Klarna, Pix<br/>Settlement: T+1 to T+3"]
    METHOD -->|Crypto| CRYPTO["PSPs Supporting Crypto<br/>-----<br/>Example: Coinbase, BitPay<br/>Currencies: USDT, BTC, ETH"]

    CARD --> STATUS_FILTER
    EWALLET --> STATUS_FILTER
    BANK --> STATUS_FILTER
    CRYPTO --> STATUS_FILTER

    STATUS_FILTER[Health Status Filtering] --> HEALTH{PSP Status?}

    HEALTH -->|Healthy<br/>Success >= 80%| HEALTHY["Available for Scoring<br/>-----<br/>Pass to Step 3"]
    HEALTH -->|Degraded<br/>Success 50-79%| DEGRADED["Lower Priority<br/>-----<br/>Score Penalty: -20"]
    HEALTH -->|Unavailable<br/>Success < 50%| UNAVAILABLE["Skip<br/>-----<br/>Use Fallback"]

    HEALTHY --> RETURN1[Return Filtered PSPs]
    DEGRADED --> RETURN1
    UNAVAILABLE --> FALLBACK_OPTION["Manual Bank Transfer<br/>-----<br/>Settlement: T+2"]

    RETURN1 --> END1[Proceed to Step 3: Scoring]
    FALLBACK_OPTION --> END2[Notify Ops Team]
```

### 3.3 Multi-Dimensional Scoring Algorithm

```mermaid
flowchart TD
    START[Step 3: Calculate Score for Each PSP] --> INIT[Initialize Score = 0]

    INIT --> DIM1{"Dimension 1<br/>Success Rate (24h)<br/>-----<br/>Weight: 50%"}

    DIM1 -->|>= 95%| S1A["Score += 50 x 0.95<br/>= 47.5"]
    DIM1 -->|90-94%| S1B["Score += 50 x 0.92<br/>= 46.0"]
    DIM1 -->|85-89%| S1C["Score += 50 x 0.88<br/>= 44.0"]
    DIM1 -->|80-84%| S1D["Score += 50 x 0.85<br/>= 42.5"]
    DIM1 -->|< 80%| S1E["Score += 50 x 0.75<br/>= 37.5"]

    S1A --> DIM2
    S1B --> DIM2
    S1C --> DIM2
    S1D --> DIM2
    S1E --> DIM2

    DIM2{"Dimension 2<br/>Fee Rate<br/>-----<br/>Weight: 30%"}

    DIM2 -->|< 2%| S2A["Score += 30 x 0.98<br/>= 29.4"]
    DIM2 -->|2-3%| S2B["Score += 30 x 0.95<br/>= 28.5"]
    DIM2 -->|3-4%| S2C["Score += 30 x 0.92<br/>= 27.6"]
    DIM2 -->|4-5%| S2D["Score += 30 x 0.90<br/>= 27.0"]
    DIM2 -->|> 5%| S2E["Score += 30 x 0.80<br/>= 24.0"]

    S2A --> DIM3
    S2B --> DIM3
    S2C --> DIM3
    S2D --> DIM3
    S2E --> DIM3

    DIM3{"Dimension 3<br/>Settlement Speed<br/>-----<br/>Weight: 15%"}

    DIM3 -->|< 5 min| S3A["Score += 15 x 1.0<br/>= 15.0"]
    DIM3 -->|5-15 min| S3B["Score += 15 x 0.8<br/>= 12.0"]
    DIM3 -->|15-30 min| S3C["Score += 15 x 0.6<br/>= 9.0"]
    DIM3 -->|30-60 min| S3D["Score += 15 x 0.5<br/>= 7.5"]
    DIM3 -->|> 60 min| S3E["Score += 15 x 0.2<br/>= 3.0"]

    S3A --> DIM4
    S3B --> DIM4
    S3C --> DIM4
    S3D --> DIM4
    S3E --> DIM4

    DIM4{"Dimension 4<br/>VIP Channel<br/>-----<br/>Weight: 5%"}

    DIM4 -->|VIP >= 3<br/>AND<br/>PSP has VIP channel| S4A["Score += 5.0<br/>VIP Bonus"]
    DIM4 -->|Otherwise| S4B[Score += 0]

    S4A --> DIM5
    S4B --> DIM5

    DIM5{"Dimension 5<br/>Currency Match<br/>-----<br/>Weight: 3%"}

    DIM5 -->|Exact Match<br/>No FX Fee| S5A[Score += 3.0]
    DIM5 -->|Need Conversion<br/>FX Fee Applied| S5B[Score += 0]

    S5A --> TOTAL
    S5B --> TOTAL

    TOTAL["Calculate Total Score<br/>-----<br/>Range: 0-103<br/>Typical: 70-95"] --> EXAMPLE["Scoring Example:<br/>-----<br/>Nuvei<br/>Success 95%: 47.5<br/>Fee 2.5%: 28.5<br/>Speed 10min: 12.0<br/>VIP: 0<br/>Currency Match: 3.0<br/>-----<br/>Total: 91.0"]

    EXAMPLE --> RANK[Rank All PSPs by Score DESC]

    RANK --> SELECT[Select Top 1 PSP]

    SELECT --> HEALTH_CHECK{Final Health Check}

    HEALTH_CHECK -->|Healthy| ROUTE[Route to Selected PSP]
    HEALTH_CHECK -->|Degraded/Down| RETRY[Retry with 2nd Ranked PSP]

    RETRY --> HEALTH_CHECK

    ROUTE --> LOG[Log Decision + Fallback Queue]
    LOG --> END[Return PSP Details]
```

### 3.4 Score Calculation Example

For a US player depositing $1000 via Credit Card:

```
Nuvei:
  Success Rate: 95% -> 95% x 50 = 47.5
  Fee: 2.5% -> (1 - 0.025) x 30 = 29.25
  Settlement: 10 min -> (1 - 10/60) x 15 = 12.5
  VIP Bonus: No -> 0
  Currency Match: USD -> +3
  Total Score: 92.25 (Selected)

Adyen:
  Success Rate: 92% -> 46.0
  Fee: 3.0% -> 29.1
  Settlement: 5 min -> 13.75
  VIP Bonus: No -> 0
  Currency Match: EUR (need convert) -> 0
  Total Score: 88.85

Stripe:
  Success Rate: 90% -> 45.0
  Fee: 2.9% -> 29.13
  Settlement: 15 min -> 11.25
  VIP Bonus: No -> 0
  Currency Match: USD -> +3
  Total Score: 88.38
```

**Decision**: Nuvei selected (92.25). Fallback queue: [Adyen, Stripe]

---

## 4. Reconciliation & Manual Credit Flow

### 4.1 Reconciliation Task Overview

```mermaid
flowchart TD
    START["Cron Job<br/>-----<br/>Trigger: Every 15 minutes<br/>Target: Pending > 30 min"] --> QUERY["Query Pending Transactions<br/>-----<br/>SELECT * FROM transactions<br/>WHERE status = 'PENDING'<br/>AND created_at < NOW() - 30min"]

    QUERY --> CHECK{"Found Pending<br/>Transactions?"}

    CHECK -->|No| END1["End<br/>-----<br/>No Action Needed<br/>Next Run: 15 min"]

    CHECK -->|Yes| COUNT["Pending Count: 25<br/>-----<br/>Begin Reconciliation Loop"]

    COUNT --> LOOP["For Each Transaction<br/>-----<br/>Query PSP Status via API"]

    LOOP --> PSP_API["PSP Query Result<br/>-----<br/>GET /api/v1/query<br/>HMAC Signature Auth"]

    PSP_API --> ROUTE{PSP Status?}

    ROUTE -->|SUCCESS| PATH_SUCCESS["Auto Credit Flow<br/>-----<br/>PSP confirmed but not credited<br/>Proceed to Credit Process"]

    ROUTE -->|FAILED| PATH_FAILED["Update Status<br/>-----<br/>Mark as FAILED<br/>Notify Player<br/>No Credit Needed"]

    ROUTE -->|PENDING| PATH_PENDING["Continue Waiting<br/>-----<br/>Check Duration:<br/>< 2h: Wait<br/>>= 2h: Alert CS Team"]

    ROUTE -->|NOT_FOUND| PATH_NOT_FOUND["Manual Review Flow<br/>-----<br/>PSP has no record<br/>Proceed to Appeal Process"]

    ROUTE -->|API ERROR| PATH_ERROR["Retry Logic<br/>-----<br/>Retry < 3: Wait 5min<br/>Retry >= 3: Escalate"]

    PATH_SUCCESS --> AUTO_CREDIT[See: Auto Credit Flow<br/>-----<br/>Idempotency + Lock + Credit]
    PATH_NOT_FOUND --> MANUAL_REVIEW["See: Manual Review Flow<br/>-----<br/>Appeal + Verify + Approval"]

    AUTO_CREDIT --> RESULT1[Result: Credit Success/Failed]
    MANUAL_REVIEW --> RESULT2[Result: Approved/Rejected]
    PATH_FAILED --> RESULT3[Result: Marked Failed]
    PATH_PENDING --> RESULT4[Result: Still Pending]
    PATH_ERROR --> RESULT5[Result: Retry/Escalated]

    RESULT1 --> NEXT
    RESULT2 --> NEXT
    RESULT3 --> NEXT
    RESULT4 --> NEXT
    RESULT5 --> NEXT

    NEXT{"More Pending<br/>Transactions?"}

    NEXT -->|Yes| LOOP
    NEXT -->|No| SUMMARY["Generate Report<br/>-----<br/>Total Checked: 25<br/>Credit Success: 5<br/>Failed: 3<br/>Still Pending: 15<br/>Manual Review: 2"]

    SUMMARY --> REPORT["Send to Finance Team<br/>-----<br/>Daily Report @ 08:00 AM<br/>Email + Dashboard"]

    REPORT --> END2["End<br/>-----<br/>Reconciliation Complete<br/>Next Run: 15 min"]
```

### 4.2 Auto Credit Flow

```mermaid
flowchart TD
    START["Auto Credit Trigger<br/>-----<br/>Condition: PSP Status = SUCCESS<br/>Platform Status = PENDING"] --> IDEMPOTENT{"Idempotent Check<br/>-----<br/>Already Credited?"}

    IDEMPOTENT -->|Yes| SKIP["Skip Credit<br/>-----<br/>Log: Duplicate Attempt<br/>Reason: Already Processed<br/>Action: None"]

    IDEMPOTENT -->|No| LOCK["Acquire Redis Lock<br/>-----<br/>Key: reconcile:txn_{id}<br/>Command: SET NX<br/>TTL: 300 seconds"]

    LOCK --> LOCK_CHECK{Lock Acquired?}

    LOCK_CHECK -->|No| SKIP2["Skip Credit<br/>-----<br/>Reason: Another Job Processing<br/>Action: Wait Next Cycle"]

    LOCK_CHECK -->|Yes| DB_TXN["BEGIN DB Transaction<br/>-----<br/>Isolation: READ_COMMITTED"]

    DB_TXN --> UPDATE_TXN["UPDATE transactions SET<br/>-----<br/>status = 'SUCCESS',<br/>psp_transaction_id = ?,<br/>credit_flag = TRUE,<br/>credit_at = NOW(),<br/>completed_at = NOW()<br/>WHERE id = ? AND status = 'PENDING'"]

    UPDATE_TXN --> AFFECTED{Affected Rows?}

    AFFECTED -->|0 rows| ROLLBACK["ROLLBACK Transaction<br/>-----<br/>Reason: Already Updated<br/>Release Lock"]

    AFFECTED -->|1 row| CREDIT["Credit Player Balance<br/>-----<br/>wallet_service.credit(<br/>  player_id,<br/>  amount,<br/>  source: 'RECONCILIATION'<br/>)"]

    CREDIT --> CREDIT_CHECK{Credit Success?}

    CREDIT_CHECK -->|Failed| ROLLBACK2["ROLLBACK Transaction<br/>-----<br/>Reason: Wallet Service Error<br/>Action: Retry Later"]

    CREDIT_CHECK -->|Success| AUDIT["Insert Audit Log<br/>-----<br/>INSERT INTO payment_audit_log<br/>(transaction_id, event, operator, details)<br/>VALUES (?, 'RECONCILIATION_CREDITED',<br/>'SYSTEM', JSON)"]

    AUDIT --> COMMIT["COMMIT Transaction<br/>-----<br/>Status: Success<br/>Balance Updated"]

    COMMIT --> NOTIFY_PLAYER["Notify Player<br/>-----<br/>Channel: Email + SMS<br/>Subject: Deposit Credited (Delayed)<br/>Content: Your $100 deposit has been credited"]

    NOTIFY_PLAYER --> NOTIFY_FINANCE["Alert Finance Team<br/>-----<br/>Channel: Slack + Email<br/>Info: Credit Success<br/>Transaction ID: ?<br/>Reason: Webhook Not Received"]

    NOTIFY_FINANCE --> RELEASE["Release Redis Lock<br/>-----<br/>Command: DEL reconcile:txn_{id}"]

    RELEASE --> SUCCESS["Credit Successful<br/>-----<br/>Balance: +$100<br/>Status: SUCCESS<br/>Flag: credit_flag = TRUE"]

    ROLLBACK --> ERROR1["Credit Failed<br/>-----<br/>Reason: Already Updated<br/>Action: Skip"]

    ROLLBACK2 --> ERROR2["Credit Failed<br/>-----<br/>Reason: Wallet Error<br/>Action: Retry Next Cycle"]

    SKIP --> END1[End - Skipped]
    SKIP2 --> END1
    SUCCESS --> END2[End - Success]
    ERROR1 --> END3[End - Error]
    ERROR2 --> END3
```

### 4.3 Manual Review & Appeal Flow

```mermaid
flowchart TD
    START["Manual Review Trigger<br/>-----<br/>Condition: PSP Status = NOT_FOUND<br/>PSP has no transaction record"] --> SEVERITY{Amount Severity?}

    SEVERITY -->|>= $1000<br/>High Value| CRITICAL["CRITICAL Alert<br/>-----<br/>Notify: Finance + Security + CTO<br/>Priority: HIGH<br/>SLA: 2 hours"]

    SEVERITY -->|< $1000<br/>Low Value| STANDARD["STANDARD Alert<br/>-----<br/>Notify: CS Team<br/>Priority: MEDIUM<br/>SLA: 24 hours"]

    CRITICAL --> TICKET["Create Review Ticket<br/>-----<br/>System: Jira<br/>Type: Payment Investigation<br/>Assignee: Finance Team<br/>Fields: {txn_id, amount, player_id, psp}"]

    STANDARD --> TICKET

    TICKET --> APPEAL{"Player Appeals?<br/>-----<br/>Timeout: 7 days"}

    APPEAL -->|No - Timeout| TIMEOUT["Close Ticket<br/>-----<br/>Status: EXPIRED<br/>Reason: No Player Response<br/>Action: Mark as FAILED"]

    APPEAL -->|Yes - Upload Receipt| VERIFY["CS Agent Verifies Receipt<br/>-----<br/>Check:<br/>Bank Reference Number<br/>Transaction Amount<br/>Transaction Date<br/>Payment Method"]

    VERIFY --> CONTACT_PSP["Contact PSP Support<br/>-----<br/>Action: Submit Ticket to PSP<br/>Evidence: Bank Receipt<br/>Wait: 1-3 business days"]

    CONTACT_PSP --> PSP_CONFIRM{"PSP Confirms<br/>Payment?"}

    PSP_CONFIRM -->|No - Not Found| REJECT["Reject Appeal<br/>-----<br/>Status: REJECTED<br/>Reason: No Payment Proof from PSP<br/>Notify Player: Email"]

    PSP_CONFIRM -->|Yes - Confirmed| MANUAL_CREDIT["Manual Credit Request<br/>-----<br/>Operator: CS Agent<br/>Evidence: PSP Confirmation Email<br/>Audit Trail: Logged"]

    MANUAL_CREDIT --> APPROVAL{"Approval Required?<br/>-----<br/>Threshold: $1000"}

    APPROVAL -->|No<br/>(Amount < $1000)| EXECUTE["Execute Credit<br/>-----<br/>Same as Auto Credit Flow<br/>Operator: CS Agent"]

    APPROVAL -->|Yes<br/>(Amount >= $1000)| AWAIT["Await CFO Approval<br/>-----<br/>Approval System: Workflow<br/>Approver: CFO<br/>SLA: 24 hours"]

    AWAIT --> APPROVED{Approved?}

    APPROVED -->|No - Rejected| REJECT2["Reject Appeal<br/>-----<br/>Status: CFO_REJECTED<br/>Reason: Insufficient Evidence<br/>Notify Player + CS"]

    APPROVED -->|Yes - Approved| EXECUTE

    EXECUTE --> CREDIT_EXEC["Credit Player Balance<br/>-----<br/>Source: MANUAL_CREDIT<br/>Operator: {cs_agent_id}<br/>Approver: {cfo_id if required}"]

    CREDIT_EXEC --> CREDIT_CHECK{Credit Success?}

    CREDIT_CHECK -->|Failed| ERROR["Credit Failed<br/>-----<br/>Reason: Wallet Service Error<br/>Action: Escalate to Tech Team"]

    CREDIT_CHECK -->|Success| AUDIT_LOG["Insert Audit Log<br/>-----<br/>Event: MANUAL_CREDIT<br/>Evidence: {psp_confirmation, bank_receipt}<br/>Approver: {cfo_id}<br/>Compliance: 7-year retention"]

    AUDIT_LOG --> NOTIFY["Notify Stakeholders<br/>-----<br/>Player: Email + SMS<br/>Finance: Slack Alert<br/>Audit: Log to SIEM"]

    NOTIFY --> SUCCESS["Manual Credit Complete<br/>-----<br/>Status: SUCCESS<br/>Flag: manual_credit = TRUE<br/>Audit Trail: Complete"]

    TIMEOUT --> END1[End - Expired]
    REJECT --> END2[End - Rejected]
    REJECT2 --> END2
    SUCCESS --> END3[End - Success]
    ERROR --> END4[End - Error]
```

---

## 5. Security Implementation

### 5.1 Three-Layer Callback Verification

```
Layer 1: IP Whitelist Verification
  - Only accept requests from PSP designated IPs
  - Reject other IPs -> 403 Forbidden + Alert

Layer 2: HMAC Signature Verification
  - Expected = HMAC-SHA256(request_body, webhook_secret)
  - Received = X-PSP-Signature Header
  - Mismatch -> 403 Forbidden + Security Alert

Layer 3: Timestamp Verification (Replay Attack Prevention)
  - NOW() - request_timestamp < 5 minutes
  - Expired -> 400 Bad Request
```

### 5.2 Idempotency Protection

| Mechanism | Implementation | TTL | Purpose |
|-----------|---------------|-----|---------|
| **Redis Lock** | `SET NX callback:{txn_id} 1 EX 60` | 60 sec | Prevent concurrent callback processing |
| **Database Status** | `WHERE status = 'PENDING' AND UPDATE status = 'SUCCESS'` | N/A | Ensure unique state transition |
| **Unique Constraint** | `UNIQUE (transaction_id, psp_transaction_id)` | N/A | Prevent duplicate credits |

### 5.3 3D Secure Implementation

| Feature | 3DS 1.0 | 3DS 2.0 (EMV 3DS) |
|---------|---------|-------------------|
| User Experience | Redirect to bank page (high abandonment) | Native in-app verification |
| Data Transmission | Basic card info only | Device fingerprint, behavioral data |
| Risk Assessment | Issuer-only decision | Multi-party collaboration (Issuer+PSP+Merchant) |
| Applicable Scenario | Desktop | Mobile-first |

**PSD2 Exemptions**:
- Transaction amount < EUR 30
- Merchant has high risk score (low-risk merchant)
- Recurring payments (Subscription)

---

## 6. PSP Integration Examples

### 6.1 Nuvei (Formerly SafeCharge)

**API Endpoints**:
```
Production: https://ppp.nuvei.com/ppp/api/v1/payment.do
Sandbox: https://ppp-test.nuvei.com/ppp/api/v1/payment.do
```

**Deposit API Request**:
```json
POST /ppp/api/v1/payment.do
{
  "merchantId": "123456789",
  "merchantSiteId": "987654",
  "clientRequestId": "txn_202601271234",
  "amount": "100.00",
  "currency": "USD",
  "userId": "player_12345",
  "timeStamp": "2026-01-27 10:30:00",
  "checksum": "e7f8a1b2c3d4e5f6..."
}
```

**Checksum Generation**:
```
SHA256(merchantId + merchantSiteId + clientRequestId + amount + currency + timeStamp + secret)
```

### 6.2 Adyen

**3DS 2.0 Integration Flow**:
1. Frontend collects card number, CVV, cardholder name
2. Call Adyen `/payments` API, returns `action.type = "threeDS2"`
3. Frontend loads Adyen 3DS Component (iframe)
4. Player completes bank verification, call `/payments/details` for final result

**Payout API Request**:
```json
POST /pal/servlet/Payout/v68/payout
{
  "merchantAccount": "IGamingPlatformEU",
  "amount": {
    "currency": "EUR",
    "value": 5000
  },
  "reference": "withdrawal_98765",
  "shopperEmail": "player@example.com",
  "card": {
    "number": "4111111111111111",
    "expiryMonth": "03",
    "expiryYear": "2030",
    "holderName": "John Doe"
  }
}
```

**Note**: Adyen uses smallest currency unit (5000 = EUR 50.00)

---

## 7. API Specifications

### 7.1 Unified Deposit API

**Request**:
```http
POST /api/v1/payments/deposit
Content-Type: application/json
Authorization: Bearer <player_jwt_token>

{
  "amount": 100.00,
  "currency": "USD",
  "payment_method": "credit_card",
  "psp_code": "nuvei",
  "return_url": "https://platform.com/deposit/callback"
}
```

**Response**:
```json
{
  "code": 1000,
  "message": "Success",
  "data": {
    "transaction_id": "txn_202601271234",
    "redirect_url": "https://psp.com/payment?token=abc123",
    "expires_at": "2026-01-27T11:00:00Z"
  }
}
```

### 7.2 Webhook Callback Handler

**Security Requirements**:
1. IP Whitelist validation
2. HMAC-SHA256 signature verification
3. Timestamp validation (< 5 min)
4. Idempotency via Redis lock: `SET callback:{txn_id} 1 EX 60 NX`

---

## 8. Failover Strategy

### 8.1 Health Check Mechanism

**Periodic Health Probe** (every 5 minutes):
- Test small transaction capability
- Monitor success rate trends
- Update PSP status in cache

### 8.2 Automatic Switching

```
Primary PSP: Nuvei (Status: Unavailable)
    ↓ Automatic switch
Backup PSP: Adyen (Status: Healthy)
    ↓ If also fails
Fallback PSP: Manual Bank Transfer (Notify Finance Team)
```

### 8.3 Recovery Detection

- Test degraded PSP every 10 minutes
- Restore to `available` status after 3 consecutive successes

---

## 9. Error Handling Matrix

| Exception Type | Trigger Condition | Handling Strategy | Notification |
|---------------|-------------------|-------------------|--------------|
| **IP Not Whitelisted** | Request IP not in PSP_IPS | Reject + Security Alert | Security Team |
| **Signature Mismatch** | HMAC Mismatch | Reject + Security Alert | Security + CTO |
| **Duplicate Callback** | Redis Lock Failed | Return 200 OK (ALREADY_PROCESSED) | None (normal) |
| **Status Changed** | Status != PENDING | Return 200 OK (Idempotent) | None (normal) |
| **Callback Timeout** | No callback for 30 min | Query PSP status proactively | Tech Team |
| **Credit Failed** | PSP returns NOT_FOUND | Manual review + Player appeal | CS + Finance |

---

## 10. Related Technical Documentation

- [Data Security Standard](../../source-archive/12_System_Security/12-03_Data_Security_Standard.md) - Payment data encryption
- [API Design Principles](../../source-archive/09_Technical_Infrastructure/09-03-01_Design_Principles.md) - API specifications
- [Audit Log System](../../source-archive/06_Platform_Governance/06-03_Audit_Log.md) - Configuration change audit
- [Approval Workflow](../../source-archive/06_Platform_Governance/06-04_Approval_Workflow.md) - Configuration change approval

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Backend Team
