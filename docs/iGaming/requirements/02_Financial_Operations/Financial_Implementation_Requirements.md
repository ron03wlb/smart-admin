# Financial Implementation Requirements

> **Canonical Source**: [source-archive/00_Foundation/guides/00-11_Financial_Implementation.md](../../source-archive/00_Foundation/guides/00-11_Financial_Implementation.md)
> **Audience**: Executives, Compliance Officers, Product Managers
> **Related Doc**: [Financial_Implementation.md](../../architecture/02_Finance_Service/Financial_Implementation.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (atomicity, idempotency, HMAC-SHA256 algorithms, SAGA flow diagrams) moved to Architecture layer. This document focuses on business requirements only.

---

## Business Value

This financial system delivers value by:
- **Fund safety**: Prevents player fund loss through concurrency control, idempotency protection, and SAGA compensation mechanisms, ensuring zero overdrafts and zero fund leakage
- **Regulatory compliance**: Ensures financial accuracy required by Tier-1 regulators (UKGC, MGA) through comprehensive audit trails, multi-tenant isolation, and 4-decimal precision standards (DECIMAL(19,4))
- **Fraud prevention**: Reduces fraudulent withdrawal risk through 7-dimension risk scoring model (0-100 scale) and automated approval workflows, protecting operator revenue

## Acceptance Criteria

- [ ] Wallet system passes concurrency tests at 1,000 TPS without overdrafts or balance inconsistencies
- [ ] Available balance formula calculates correctly across all scenarios (cash, bonus, locked funds)
- [ ] Payment gateway signature verification achieves 100% accuracy using time-safe comparison
- [ ] Deposit callback idempotency handles duplicate requests gracefully (no double credits)
- [ ] Withdrawal risk control responds within 500ms at 1,000 TPS
- [ ] Risk scoring model calculates accurately across all 7 dimensions (credit score, KYC, deposit ratio, frequency, turnover, IP/device, multi-account)
- [ ] Auto-approve (0-29), manual review (30-69), and auto-reject (70-100) thresholds work correctly
- [ ] SAGA compensation unlocks wallet amounts upon payment gateway failures
- [ ] Daily reconciliation achieves 99.99% balance accuracy across wallet cache/database comparison
- [ ] Three-way bet reconciliation (OLTP, Game Provider, OLAP) identifies discrepancies with 0.01 precision
- [ ] All financial operations generate complete audit trail (operator, timestamp, before/after values)
- [ ] Multi-tenant isolation enforced via tenant_id filtering on all financial queries

---

## 1. Document Purpose

This document defines the **business requirements** for the iGaming platform's core financial processes, including the wallet system, payment gateway integration, withdrawal risk control, and reconciliation system.

**Target Audience**:
- Product Managers (financial module)
- Compliance Officers
- Operations Managers
- Business Analysts

---

## 2. Wallet System Requirements

### 2.1 Multi-Wallet Support

The platform MUST support multiple wallet types per player:

| Wallet Type | Purpose | Business Rule |
|-------------|---------|---------------|
| **CASH** | Real-money balance for betting and withdrawals | Primary wallet for all financial operations |
| **BONUS** | Promotional funds subject to wagering requirements | Cannot be withdrawn until turnover conditions are met |
| **LOCKED** | Temporarily frozen funds (pending withdrawal, etc.) | Released upon settlement or cancellation |

### 2.2 Available Balance Formula

The system MUST calculate a player's available (bettable) balance using the following formula:

**Available Balance = Cash Wallet Balance - Locked Amount - Pending Bets**

- The result MUST never be negative (floor at zero)
- The calculation MUST be performed in real-time for every bet placement request

### 2.3 Wallet Lock/Unlock Rules

| Scenario | Lock Trigger | Unlock Trigger |
|----------|-------------|----------------|
| Bet placed | Lock bet amount when bet is submitted | Unlock when bet is settled (win/lose) |
| Withdrawal requested | Lock withdrawal amount at request time | Unlock upon approval, rejection, or timeout |
| Fraud investigation | Lock suspicious amount upon risk detection | Unlock upon investigation resolution |

**Business Rules**:
- Wallet lock MUST be recorded with a clear reason and reference ID
- All lock/unlock operations MUST generate audit trail events
- Expired lock records MUST be cleaned up periodically

### 2.4 Concurrency and Consistency Requirements

- Concurrent balance deductions MUST be handled safely to prevent overdraft
- Every debit operation MUST handle duplicate requests correctly
- The system MUST guarantee data consistency between all system components

→ **[Technical Implementation](../../architecture/02_Finance_Service/Financial_Implementation.md#concurrency-control)** - Atomicity, idempotency, eventual consistency mechanisms

### 2.5 Transaction Event Publishing

All wallet state changes MUST be published as events for downstream consumers:
- Wallet credited (deposit, win)
- Wallet debited (bet, withdrawal)
- Wallet locked / unlocked
- Events MUST be guaranteed to be delivered (using transactional outbox pattern)

### 2.6 Verification Criteria

- Available balance formula computes correctly across all scenarios
- Concurrent deduction tests pass at 1,000 TPS
- Insufficient balance requests are correctly rejected
- Wallet lock/unlock mechanism operates normally
- Outbox events achieve 100% delivery success rate
- Idempotency tests pass (duplicate requests return identical results)
- Cache and database balances remain eventually consistent

### 2.7 Common Pitfalls

1. **Optimistic lock failures without retry**: Under high concurrency, exponential backoff retry is required
2. **Expired lock records not cleaned**: A scheduled cleanup process is mandatory
3. **Cache-DB balance inconsistency**: Must use transactional outbox or periodic reconciliation
4. **Decimal precision issues**: All monetary values MUST use 4-decimal precision (e.g., DECIMAL(19,4))

---

## 3. Payment Gateway Integration Requirements

### 3.1 Supported Operations

The platform MUST support integration with third-party payment gateways (e.g., Stripe, PayPal, local payment providers) for:

| Operation | Description | Key Requirement |
|-----------|-------------|-----------------|
| **Deposit** | Player adds funds to wallet | Signature verification, idempotent callbacks |
| **Withdrawal** | Player withdraws funds from wallet | Risk control review, async status polling |
| **Callback Handling** | Gateway notifies payment result | HMAC signature verification, idempotent processing |
| **Status Query** | Poll for pending withdrawal status | Scheduled polling every 30 seconds, 24-hour timeout |

### 3.2 Deposit Flow Requirements

1. System creates a deposit order with PENDING status
2. System calls payment gateway to obtain a payment URL or QR code
3. Player completes payment on the gateway
4. Gateway sends a callback notification to the system
5. System verifies signature on the callback
6. System performs duplicate check (already-processed orders are skipped)
7. System credits the player's wallet
8. System sends a deposit success notification to the player

**Business Rules**:
- Callback signature verification MUST use time-safe comparison (prevent timing attacks)
- Duplicate callbacks for the same order MUST be handled gracefully (idempotent)
- All payment events MUST be recorded in the audit log

### 3.3 Withdrawal Flow Requirements

1. Player submits a withdrawal request
2. System performs risk control evaluation
3. If risk decision is REJECT, the withdrawal is denied immediately
4. If risk decision is MANUAL_REVIEW, the order enters the review queue; the risk control team is notified
5. If risk decision is AUTO_APPROVE, the order is submitted to the payment gateway
6. System initiates async status polling for the withdrawal
7. Upon success: wallet is debited, player is notified
8. Upon failure: locked amount is released, player is notified

**Business Rules**:
- Any failure path MUST unlock the player's wallet amount
- Orders not completed within 24 hours MUST be automatically cancelled
- All withdrawal operations MUST be recorded in the audit log

### 3.4 Signature Verification Requirements

- All payment gateway callbacks MUST be verified using cryptographic signatures
- Signature comparison MUST use secure comparison methods to prevent timing attacks

→ **[Signature Algorithm Details](../../architecture/02_Finance_Service/Financial_Implementation.md#signature-verification)** - HMAC-SHA256 generation process, parameter sorting, Base64 encoding

### 3.5 Verification Criteria

- Signature verification works correctly
- Deposit callback duplicate prevention tests pass
- Withdrawal risk control review flow is complete
- Wallet amount is correctly unlocked upon withdrawal failure
- Payment status query scheduled task runs normally
- Timed-out orders are automatically cancelled
- All payment events are recorded in the audit log

---

## 4. Withdrawal Risk Control Requirements

### 4.1 Risk Scoring Model

The system MUST evaluate every withdrawal request across 7 risk dimensions:

| Dimension | Weight | Score Range | Description |
|-----------|--------|-------------|-------------|
| Player Credit Score | 20% | 0-20 | Inverse of credit score (low credit = high risk) |
| KYC Completeness | 15% | 0-15 | VERIFIED=0, PARTIALLY_VERIFIED=8, NOT_VERIFIED=15 |
| Deposit/Withdrawal Ratio | 15% | 0-15 | Ratio > 2x triggers maximum risk score |
| Recent Withdrawal Frequency | 15% | 0-15 | Number of withdrawals in last 7 days, capped at 15 |
| Turnover Completion | 15% | 0-15 | Progress toward required wagering, inversely scored |
| IP/Device Anomaly | 10% | 0-10 | Device fingerprint and IP anomaly detection |
| Multi-Account Correlation | 10% | 0-10 | Related accounts detection |

**Total Score: 0-100**

### 4.2 Decision Thresholds

| Score Range | Decision | Action |
|-------------|----------|--------|
| 0-29 | AUTO_APPROVE | Withdrawal proceeds automatically |
| 30-69 | MANUAL_REVIEW | Sent to review queue for human decision |
| 70-100 | REJECT | Withdrawal is automatically rejected |

### 4.3 Rule Engine Requirements

The risk control chain MUST execute the following steps in sequence:
1. **Basic Validation**: Player status check (blocked players cannot withdraw), balance check, minimum withdrawal amount check (minimum: 100)
2. **Risk Scoring**: Multi-dimensional risk score calculation
3. **Decision**: Route based on score thresholds
4. **Compensation**: If any step fails, previously locked wallet amounts MUST be unlocked

### 4.4 Review Workflow State Machine

The withdrawal review workflow MUST follow this state machine:

- PENDING -> REVIEWING -> APPROVED -> PROCESSING -> SUCCESS
- REVIEWING -> REJECTED (if reviewer rejects)

**Business Rules**:
- Reviewer approval triggers submission to the payment gateway
- Reviewer rejection unlocks the player's wallet amount and sends a notification
- All review operations MUST be recorded in the audit log with reviewer identity and comments

### 4.5 Compensation Requirements

The withdrawal process MUST implement compensation mechanisms to ensure data consistency when failures occur:

**Business Rules**:
- If wallet locking succeeds but payment gateway submission fails, the wallet amount MUST be unlocked
- If payment gateway submission succeeds but wallet debit fails, the gateway order MUST be cancelled
- Compensations MUST be executed in reverse order of the original operations
- If a compensation itself fails, the case MUST be routed to a manual processing queue
- All compensation actions MUST be logged

→ **[SAGA Pattern Implementation](../../architecture/02_Finance_Service/Financial_Implementation.md#saga-compensation)** - Technical flow diagram, compensation steps, distributed transaction handling

### 4.6 Verification Criteria

- Risk scoring model calculates correctly across all dimensions
- Auto-approve and auto-reject decisions are accurate
- Manual review workflow is complete end-to-end
- Rule engine chain executes successfully
- Compensation mechanism operates correctly
- All review operations are recorded in the audit log
- Stress test: risk control system responds within 500ms at 1,000 TPS

### 4.7 Common Pitfalls

1. **Compensation not executed**: Must use proper error handling and compensation framework
2. **State machine concurrency issues**: Use locking mechanisms to prevent race conditions
3. **Rule engine misconfiguration**: Thorough testing of rule chain expressions is required
4. **Audit log gaps**: Every state change MUST be recorded

---

## 5. Reconciliation System Requirements

### 5.1 Scope of Reconciliation

The system MUST perform daily reconciliation covering:

| Category | Internal Source | External Source | Comparison |
|----------|----------------|-----------------|------------|
| **Wallet** | System balance (cache) | Database balance | Balance difference |
| **Deposits** | Deposit order records | Payment gateway daily report | Count and amount differences |
| **Withdrawals** | Withdrawal order records | Payment gateway daily report | Count and amount differences |
| **Bets** | Bet order records (OLTP) | Game provider daily report | Count and amount differences |

### 5.2 Three-Way Bet Reconciliation

For bet reconciliation, the system MUST perform three-way comparison:

1. **OLTP System** (real-time bet records)
2. **Game Provider Report** (provider's daily settlement data)
3. **OLAP Data Warehouse** (T+1 ETL data)

Any bet appearing in one source but not the others MUST be flagged as a discrepancy.

### 5.3 Discrepancy Detection and Alerting

- Any difference exceeding 0.01 MUST generate an alert
- Discrepancy records MUST capture: type, reference ID, system amount, external amount, difference amount
- All unresolved discrepancies MUST be visible in the operations dashboard

### 5.4 Discrepancy Resolution Process

Three resolution actions are available:

| Action | Description | Use Case |
|--------|-------------|----------|
| **ADJUST_SYSTEM** | Correct internal records (e.g., missed order) | System data error |
| **ADJUST_EXTERNAL** | Request external party to correct their records | Gateway/provider data error |
| **IGNORE** | Mark as acceptable (small rounding differences) | Minor discrepancies below threshold |

**Auto-Resolution Rule**: Differences below 0.01 that appear consecutively for 3 or more days are automatically marked as IGNORE.

### 5.5 Reconciliation Report

- The system MUST generate a daily reconciliation report per tenant
- The report MUST include: wallet balance comparison, deposit comparison, withdrawal comparison, bet comparison, and discrepancy details
- Reports MUST be generated automatically after reconciliation completes (scheduled at 02:00 AM daily)
- All resolution actions MUST be recorded in the audit log

### 5.6 Verification Criteria

- Daily reconciliation scheduled task runs successfully
- Wallet balance accuracy verified to 99.99% across all data sources
- Payment gateway reconciliation difference is below 0.01%

→ **[Cache/Database Synchronization Strategy](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#cache-strategy)**
- Three-way bet reconciliation achieves 100% accuracy
- Discrepancy alerts are sent in a timely manner
- Reconciliation reports are generated automatically
- Discrepancy resolution process is fully recorded in the audit log

### 5.7 Common Pitfalls

1. **Timezone misalignment**: System, payment gateway, and game providers MUST use a unified timezone
2. **Precision loss**: All monetary values MUST use DECIMAL(19,4) and BigDecimal
3. **ETL delays**: OLAP data may have T+1 delay; reconciliation logic must account for this
4. **Accumulated small differences**: 0.01 differences may appear harmless individually but can accumulate significantly over time

---

## 6. Cross-Cutting Business Requirements

### 6.1 Audit Trail

All financial operations MUST generate complete audit records including:
- Operator identity (system or human)
- Timestamp
- Operation type
- Affected entities (wallet ID, order ID, etc.)
- Before/after values where applicable

### 6.2 Multi-Tenant Isolation

All financial data MUST be tenant-isolated:
- Every table MUST include a `tenant_id` column
- Queries MUST always filter by tenant
- Reconciliation runs per tenant independently

### 6.3 Decimal Precision Standard

- All monetary values: DECIMAL(19,4) in database, BigDecimal in application code
- No floating-point types (float/double) for monetary calculations
- Consistent rounding mode: HALF_UP

---

## 7. Implementation Sequence

The recommended implementation order is:

1. **Wallet System** -- Foundation for all financial operations
2. **Payment Gateway Integration** -- Enables deposits and withdrawals
3. **Withdrawal Risk Control** -- Adds fraud prevention layer
4. **Reconciliation System** -- Ensures financial accuracy

Each phase should be fully verified before proceeding to the next.
