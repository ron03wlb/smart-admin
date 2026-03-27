---
title: "Ch2: 錢包系統技術架構"
part: technical
module: wallet-system
version: v2.2
created: 2026-03-24
---

# 第 2 章：錢包系統技術架構

## 2.1 模組概述

Seamless Wallet implementation with triple-layer concurrency control and triple-layer idempotency (ADR-015). The wallet system is the core financial engine of the iGaming platform, handling real-time balance management, transaction settlement, bonus tracking, and reconciliation across game providers with zero tolerance for balance drift or duplicate transactions.

## 2.2 資料模型

### t_wallet

Primary wallet storage with strict constraints and concurrency control:

```sql
CREATE TABLE t_wallet (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    wallet_type VARCHAR(20) NOT NULL, -- CASH, BONUS, CREDIT
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    locked_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_wallet_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_wallet_locked_le_balance CHECK (locked_amount <= balance),
    CONSTRAINT uk_wallet_player_type_tenant UNIQUE (player_id, wallet_type, tenant_id)
);

CREATE INDEX idx_wallet_tenant_player ON t_wallet(tenant_id, player_id);
CREATE INDEX idx_wallet_player_type ON t_wallet(player_id, wallet_type);
```

**Columns:**
- `id`: Unique wallet identifier (BIGSERIAL, immutable)
- `tenant_id`: Multi-tenancy isolation
- `player_id`: Player reference (immutable, linked to t_player)
- `wallet_type`: CASH (playable funds), BONUS (restricted promotion funds), CREDIT (agent credit mode)
- `currency`: ISO 4217 code (e.g., USD, CNY, EUR)
- `balance`: Current balance after all transactions
- `locked_amount`: Amount in transit (pending bets, pending withdrawal) ≤ balance
- `version`: Optimistic lock counter, incremented on every update
- `created_at`: Immutable creation timestamp
- `updated_at`: Updated on every transaction

**Constraints:**
- `ck_wallet_balance_non_negative`: Prevents negative balance (hardstop)
- `ck_wallet_locked_le_balance`: Locked amount never exceeds balance
- `uk_wallet_player_type_tenant`: One wallet per (player, type, tenant) combination

### t_wallet_transaction

Full audit trail with complete transaction history:

```sql
CREATE TABLE t_wallet_transaction (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    tx_type VARCHAR(30) NOT NULL, -- DEBIT, CREDIT, LOCK, UNLOCK, ROLLBACK, ADJUSTMENT
    tx_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, REVERSED
    event_id VARCHAR(256) NOT NULL UNIQUE, -- Idempotency key from game provider
    event_type VARCHAR(50) NOT NULL, -- BET_PLACED, WIN_SETTLED, BONUS_GRANT, FREE_SPIN, etc.
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    locked_before DECIMAL(19,4) NOT NULL DEFAULT 0,
    locked_after DECIMAL(19,4) NOT NULL DEFAULT 0,
    round_id VARCHAR(256),
    game_id VARCHAR(100),
    game_provider VARCHAR(50),
    request_reference VARCHAR(256),
    idempotency_key VARCHAR(256) NOT NULL,
    correlation_id VARCHAR(256),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB,
    CONSTRAINT fk_tx_wallet_id FOREIGN KEY (wallet_id) REFERENCES t_wallet(id),
    CONSTRAINT uk_event_id_tenant UNIQUE (event_id, tenant_id)
);

CREATE INDEX idx_tx_player_created ON t_wallet_transaction(player_id, created_at DESC);
CREATE INDEX idx_tx_event_id ON t_wallet_transaction(event_id);
CREATE INDEX idx_tx_idempotency_key ON t_wallet_transaction(idempotency_key);
CREATE INDEX idx_tx_round_id ON t_wallet_transaction(round_id);
```

**Key Fields:**
- `event_id`: Game provider's transaction ID (UNIQUE per tenant, primary idempotency)
- `idempotency_key`: Request UUID for distributed replay detection
- `tx_status`: PENDING (awaiting confirmation), COMPLETED (settled), FAILED (rejected), REVERSED (rollback)
- `balance_before/after`: Pre/post balance snapshots for audit
- `locked_before/after`: Lock state snapshots for lock tracking
- `round_id`: Game round reference (can have multiple txs per round)
- `metadata`: JSONB for provider-specific fields, bonus conditions, game state

### t_wallet_bonus_ext

Per-bonus wagering progress and restrictions:

```sql
CREATE TABLE t_wallet_bonus_ext (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    bonus_id BIGINT NOT NULL,
    activity_id VARCHAR(100),
    bonus_amount DECIMAL(19,4) NOT NULL,
    wagered_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    required_amount DECIMAL(19,4) NOT NULL,
    wagering_progress_pct DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN required_amount > 0 THEN (wagered_amount * 100 / required_amount) ELSE 100 END
    ) STORED,
    game_restrictions JSONB, -- {"allowed_games": ["SLOT_001", "SLOT_002"], "excluded_games": [...]}
    contribution_rates JSONB, -- {"SLOT": 100, "TABLE": 50, "LIVE": 0}
    expiry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FORFEITED, CONVERTED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bonus_ext_wallet FOREIGN KEY (wallet_id) REFERENCES t_wallet(id),
    CONSTRAINT ck_bonus_wagered_le_required CHECK (wagered_amount <= required_amount * 1.1)
);

CREATE INDEX idx_bonus_ext_player_active ON t_wallet_bonus_ext(player_id, status, expiry_at);
CREATE INDEX idx_bonus_ext_bonus_id ON t_wallet_bonus_ext(bonus_id);
```

**Fields:**
- `bonus_amount`: Bonus credit issued
- `wagered_amount`: Cumulative wagered on this bonus
- `required_amount`: Wagering requirement (e.g., 35x for deposit bonus)
- `wagering_progress_pct`: Auto-calculated percentage
- `game_restrictions`: List of allowed/excluded game IDs and game type contributions
- `contribution_rates`: Percentage of bet that counts toward wagering (slots 100%, tables 50%, live 0%)
- `expiry_at`: Bonus expiration date (forfeiture if not met)

### t_wallet_lock

Lock reason tracking for debugging and reporting:

```sql
CREATE TABLE t_wallet_lock (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    lock_reason VARCHAR(50) NOT NULL, -- BET_PENDING, WITHDRAWAL_PENDING, FRAUD_HOLD, BONUS_LOCK, MANUAL_HOLD
    locked_amount DECIMAL(19,4) NOT NULL,
    reference_id VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP,
    metadata JSONB,
    CONSTRAINT fk_lock_wallet FOREIGN KEY (wallet_id) REFERENCES t_wallet(id),
    CONSTRAINT ck_lock_positive CHECK (locked_amount > 0)
);

CREATE INDEX idx_wallet_lock_active ON t_wallet_lock(wallet_id, released_at);
```

### Row-Level Security (RLS)

All wallet tables enforce RLS:

```sql
ALTER TABLE t_wallet ENABLE ROW LEVEL SECURITY;

CREATE POLICY wallet_tenant_isolation ON t_wallet
    USING (tenant_id = current_setting('app.tenant_id')::BIGINT);

ALTER TABLE t_wallet_transaction ENABLE ROW LEVEL SECURITY;

CREATE POLICY transaction_tenant_isolation ON t_wallet_transaction
    USING (tenant_id = current_setting('app.tenant_id')::BIGINT);
```

## 2.3 Seamless Wallet API (5 端點)

### POST /igaming/seamless/authenticate

Player balance query with game provider authentication.

**Request:**
```json
{
    "playerId": "p_123456",
    "sessionToken": "sess_abc123xyz",
    "timestamp": 1711270800,
    "signature": "sha256(sessionToken+timestamp+apiSecret)"
}
```

**Response (200 OK):**
```json
{
    "playerId": "p_123456",
    "cash": {
        "balance": 1000.00,
        "currency": "USD",
        "locked": 50.00
    },
    "bonus": {
        "balance": 200.00,
        "locked": 0.00,
        "wageredAmount": 70.00,
        "requiredAmount": 280.00,
        "expiryAt": "2026-04-24T00:00:00Z"
    },
    "playable": 1150.00,
    "totalBalance": 1200.00,
    "requestId": "req_xyz789"
}
```

**Implementation:**
```java
@PostMapping("/igaming/seamless/authenticate")
@RateLimit(rps = 100, burst = 200)
public ResponseEntity<AuthenticateVO> authenticate(@RequestBody AuthenticateForm form) {
    // 1. Signature verification (HMAC-SHA256)
    if (!signatureService.verify(form.getSessionToken(), form.getTimestamp(), form.getSignature())) {
        return ResponseEntity.status(401).build();
    }

    // 2. Timestamp within 5 minutes
    long clockSkew = Math.abs(System.currentTimeMillis() - form.getTimestamp() * 1000);
    if (clockSkew > 5 * 60 * 1000) {
        return ResponseEntity.status(401).build();
    }

    // 3. Fetch cached balance (JetCache L1/L2)
    WalletBalanceVO balance = walletService.getBalance(form.getPlayerId());

    return ResponseEntity.ok(new AuthenticateVO(balance));
}
```

### POST /igaming/seamless/debit

Deduct amount from player wallet (bet placement).

**Request:**
```json
{
    "eventId": "event_2024032401_12345_001",
    "playerId": "p_123456",
    "amount": 50.00,
    "currency": "USD",
    "roundId": "round_game123_1711270800",
    "gameId": "SLOT_DEMO_001",
    "gameProviderId": "PRAGMATIC",
    "transactionType": "BET_PLACED",
    "timestamp": 1711270800,
    "signature": "...",
    "metadata": {
        "lineCount": 20,
        "coinValue": 0.10,
        "betMultiplier": 25
    }
}
```

**Response (200 OK):**
```json
{
    "transactionId": "tx_gp_12345",
    "status": "COMPLETED",
    "newBalance": 950.00,
    "currency": "USD",
    "timestamp": 1711270800123,
    "requestId": "req_xyz789"
}
```

**Response (409 Conflict - Insufficient Funds):**
```json
{
    "errorCode": "INSUFFICIENT_FUNDS",
    "message": "Available balance 800.00 is less than requested 50.00",
    "availableBalance": 800.00,
    "requestId": "req_xyz789"
}
```

**Implementation with Triple-Layer Concurrency & Idempotency:**

```java
@PostMapping("/igaming/seamless/debit")
@RateLimit(rps = 500, burst = 1000)
public ResponseEntity<DebitResponseVO> debit(@RequestBody DebitForm form) {
    String requestId = UUID.randomUUID().toString();

    try {
        // === LAYER 1: IDEMPOTENCY - Redis Fast Check ===
        IdempotencyResult cached = idempotencyService.getCached(form.getEventId());
        if (cached != null && cached.isCompleted()) {
            return ResponseEntity.ok(cached.getResponse());
        }

        // === STRICT TOKEN VALIDATION (C-03) ===
        // Debit is money operation — expired tokens are rejected with no grace period
        if (!signatureService.verifyStrict(form)) {
            return ResponseEntity.status(401).body(
                new ErrorVO("TOKEN_EXPIRED", "Expired token rejected. See §2.3.6")
            );
        }

        return walletTransactionService.debit(form);

    } catch (Exception e) {
        logger.error("Debit failed: " + e.getMessage(), e);
        return ResponseEntity.status(500).build();
    }
}

@Component
@RequiredArgsConstructor
public class WalletTransactionService {
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;
    private final RedissonClient redissonClient;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<DebitResponseVO> debit(DebitForm form) {
        String eventId = form.getEventId();
        Long playerId = form.getPlayerId();
        String lockKey = "wallet:debit:" + playerId;

        // === LAYER 2: DISTRIBUTED LOCK (Redisson) ===
        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            return ResponseEntity.status(503).build(); // Service Unavailable
        }

        try {
            // === LAYER 1.5: IDEMPOTENCY - DB Check (after lock acquired) ===
            WalletTransaction existingTx = txRepo.findByEventId(eventId);
            if (existingTx != null && existingTx.isCompleted()) {
                return ResponseEntity.ok(buildDebitResponse(existingTx));
            }

            // === LAYER 3: PESSIMISTIC LOCK (SELECT FOR UPDATE) ===
            WalletEntity wallet = walletRepo.findByPlayerIdAndTypeForUpdate(
                playerId,
                WalletType.CASH
            );

            // === ROUND STATE MACHINE VALIDATION ===
            GameRound round = gameRoundRepo.findById(form.getRoundId());
            if (!round.isOpen()) {
                return ResponseEntity.status(409).body(
                    new ErrorVO("ROUND_NOT_OPEN", "Round is not in OPEN state")
                );
            }

            // === BALANCE VALIDATION ===
            BigDecimal playableBalance = wallet.getBalance().subtract(wallet.getLockedAmount());
            if (playableBalance.compareTo(form.getAmount()) < 0) {
                return ResponseEntity.status(409).body(
                    new ErrorVO("INSUFFICIENT_FUNDS",
                        "Available " + playableBalance + " < requested " + form.getAmount())
                );
            }

            // === DEDUCTION PRIORITY (Configurable): BONUS → CASH → CREDIT ===
            BigDecimal amountRemaining = form.getAmount();
            WalletTransaction debitTx = new WalletTransaction();
            debitTx.setEventId(eventId);
            debitTx.setPlayerId(playerId);
            debitTx.setTxType(TransactionType.DEBIT);
            debitTx.setEventType(form.getTransactionType());
            debitTx.setRoundId(form.getRoundId());
            debitTx.setGameId(form.getGameId());
            debitTx.setGameProvider(form.getGameProviderId());
            debitTx.setAmount(form.getAmount());
            debitTx.setIdempotencyKey(form.getEventId());
            debitTx.setBalanceBefore(wallet.getBalance());

            // Debit from BONUS first if available
            WalletEntity bonusWallet = walletRepo.findByPlayerIdAndTypeForUpdate(
                playerId,
                WalletType.BONUS
            );
            if (bonusWallet.getBalance().compareTo(amountRemaining) >= 0) {
                bonusWallet.setBalance(bonusWallet.getBalance().subtract(amountRemaining));
                bonusWallet.setVersion(bonusWallet.getVersion() + 1);
                walletRepo.save(bonusWallet);

                debitTx.setWalletId(bonusWallet.getId());
                amountRemaining = BigDecimal.ZERO;
            } else if (bonusWallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bonusDebit = bonusWallet.getBalance();
                bonusWallet.setBalance(BigDecimal.ZERO);
                bonusWallet.setVersion(bonusWallet.getVersion() + 1);
                walletRepo.save(bonusWallet);

                amountRemaining = amountRemaining.subtract(bonusDebit);
            }

            // Debit remaining from CASH
            if (amountRemaining.compareTo(BigDecimal.ZERO) > 0) {
                if (wallet.getBalance().compareTo(amountRemaining) < 0) {
                    return ResponseEntity.status(409).body(
                        new ErrorVO("INSUFFICIENT_FUNDS", "Not enough CASH wallet")
                    );
                }
                wallet.setBalance(wallet.getBalance().subtract(amountRemaining));
                wallet.setVersion(wallet.getVersion() + 1);
                debitTx.setWalletId(wallet.getId());
            }

            debitTx.setBalanceAfter(wallet.getBalance());
            debitTx.setTxStatus(TransactionStatus.COMPLETED);

            // === OPTIMISTIC LOCK CHECK (version field) ===
            int updated = walletRepo.updateBalanceWithVersion(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getVersion()
            );
            if (updated == 0) {
                throw new OptimisticLockException("Wallet version mismatch");
            }

            // === PERSIST TRANSACTION ===
            txRepo.save(debitTx);

            // === CACHE IDEMPOTENCY RESULT ===
            DebitResponseVO response = buildDebitResponse(debitTx, wallet);
            idempotencyService.cache(eventId, response);

            // === PUBLISH EVENT ===
            eventPublisher.publish("igaming.wallet.events",
                new WalletDebitedEvent(debitTx, wallet));

            return ResponseEntity.ok(response);

        } catch (OptimisticLockException e) {
            logger.warn("Optimistic lock failed, retrying: " + e.getMessage());
            return debit(form); // Recursive retry (max 3 times)
        } finally {
            lock.unlock();
        }
    }
}
```

**Concurrency Flow Diagram:**
```
Request arrives
    ↓
[LAYER 1] Redis idempotency SETNX check (fast path, <5ms)
    ↓ (cache miss)
Acquire request lock
    ↓
[LAYER 1.5] DB event_id uniqueness check
    ↓
[LAYER 2] Redisson distributed lock (player-level, 30s timeout)
    ↓
[LAYER 3] SELECT FOR UPDATE (row-level PostgreSQL pessimistic lock)
    ↓
Round state machine validation
    ↓
Balance sufficiency check
    ↓
Calculate deductions (BONUS → CASH → CREDIT priority)
    ↓
Version check (optimistic lock on update)
    ↓
DB write (UPDATE with WHERE version = X)
    ↓
Save transaction record (event_id UNIQUE constraint triggers if duplicate)
    ↓
Cache response in Redis (1h TTL)
    ↓
Publish WALLET_DEBITED event
    ↓
Release locks
    ↓
Return response
```

### POST /igaming/seamless/credit

Credit amount to player wallet (win settlement).

**Request:**
```json
{
    "eventId": "event_2024032401_12345_002",
    "playerId": "p_123456",
    "amount": 150.00,
    "currency": "USD",
    "roundId": "round_game123_1711270800",
    "gameId": "SLOT_DEMO_001",
    "gameProviderId": "PRAGMATIC",
    "transactionType": "WIN_SETTLED",
    "bonusApplicable": true,
    "timestamp": 1711270801,
    "signature": "..."
}
```

**Response (200 OK):**
```json
{
    "transactionId": "tx_gp_12346",
    "status": "COMPLETED",
    "creditedWallet": "CASH",
    "newBalance": 1100.00,
    "currency": "USD",
    "timestamp": 1711270801123,
    "requestId": "req_xyz790"
}
```

**Implementation:**

```java
@PostMapping("/igaming/seamless/credit")
@RateLimit(rps = 500, burst = 1000)
public ResponseEntity<CreditResponseVO> credit(@RequestBody CreditForm form) {
    try {
        // Idempotency check (same as debit)
        IdempotencyResult cached = idempotencyService.getCached(form.getEventId());
        if (cached != null && cached.isCompleted()) {
            return ResponseEntity.ok(cached.getResponse());
        }

        // STRICT Token validation — ALL money operations reject expired tokens (C-03)
        // No grace period. If GP sends credit after token expiry, reject and
        // route to Resettlement flow (customer service manually credits via Adjust endpoint)
        if (!signatureService.verifyStrict(form)) {
            return ResponseEntity.status(401).body(
                new ErrorVO("TOKEN_EXPIRED",
                    "Token expired. Use Resettlement flow for late settlements. See §2.3.6")
            );
        }

        return walletTransactionService.credit(form);

    } catch (Exception e) {
        logger.error("Credit failed: " + e.getMessage(), e);
        return ResponseEntity.status(500).build();
    }
}

@Transactional(rollbackFor = Throwable.class)
public ResponseEntity<CreditResponseVO> credit(CreditForm form) {
    String lockKey = "wallet:credit:" + form.getPlayerId();
    RLock lock = redissonClient.getLock(lockKey);

    if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        return ResponseEntity.status(503).build();
    }

    try {
        // Idempotency DB check
        WalletTransaction existingTx = txRepo.findByEventId(form.getEventId());
        if (existingTx != null && existingTx.isCompleted()) {
            return ResponseEntity.ok(buildCreditResponse(existingTx));
        }

        // Pessimistic lock on CASH wallet (credit always to CASH)
        WalletEntity cashWallet = walletRepo.findByPlayerIdAndTypeForUpdate(
            form.getPlayerId(),
            WalletType.CASH
        );

        // Round state validation
        GameRound round = gameRoundRepo.findById(form.getRoundId());
        if (!round.isClosedOrPending()) {
            return ResponseEntity.status(409).body(
                new ErrorVO("ROUND_INVALID_STATE", "Round not ready for credit")
            );
        }

        // Create credit transaction
        WalletTransaction creditTx = new WalletTransaction();
        creditTx.setEventId(form.getEventId());
        creditTx.setPlayerId(form.getPlayerId());
        creditTx.setWalletId(cashWallet.getId());
        creditTx.setTxType(TransactionType.CREDIT);
        creditTx.setEventType(form.getTransactionType());
        creditTx.setAmount(form.getAmount());
        creditTx.setRoundId(form.getRoundId());
        creditTx.setBalanceBefore(cashWallet.getBalance());

        // Credit to CASH (bonus winnings go to CASH after wagering requirement met)
        cashWallet.setBalance(cashWallet.getBalance().add(form.getAmount()));
        cashWallet.setVersion(cashWallet.getVersion() + 1);

        creditTx.setBalanceAfter(cashWallet.getBalance());
        creditTx.setTxStatus(TransactionStatus.COMPLETED);

        // Optimistic lock update
        int updated = walletRepo.updateBalanceWithVersion(
            cashWallet.getId(),
            cashWallet.getBalance(),
            cashWallet.getVersion()
        );
        if (updated == 0) {
            throw new OptimisticLockException("Wallet version mismatch on credit");
        }

        // Persist and publish
        txRepo.save(creditTx);
        CreditResponseVO response = buildCreditResponse(creditTx, cashWallet);
        idempotencyService.cache(form.getEventId(), response);
        eventPublisher.publish("igaming.wallet.events",
            new WalletCreditedEvent(creditTx, cashWallet));

        return ResponseEntity.ok(response);

    } finally {
        lock.unlock();
    }
}
```

### POST /igaming/seamless/rollback

Reverse a debit transaction (e.g., bet cancellation, server error recovery).

**Request:**
```json
{
    "eventId": "event_2024032401_12345_003",
    "originalEventId": "event_2024032401_12345_001",
    "playerId": "p_123456",
    "amount": 50.00,
    "roundId": "round_game123_1711270800",
    "timestamp": 1711270802,
    "signature": "..."
}
```

**Response (200 OK):**
```json
{
    "transactionId": "tx_gp_12347",
    "status": "COMPLETED",
    "reversedTransactionId": "tx_gp_12345",
    "newBalance": 1000.00,
    "timestamp": 1711270802123,
    "requestId": "req_xyz791"
}
```

**Implementation:**

```java
@PostMapping("/igaming/seamless/rollback")
@RateLimit(rps = 100, burst = 200)
public ResponseEntity<RollbackResponseVO> rollback(@RequestBody RollbackForm form) {
    try {
        // Idempotency check
        IdempotencyResult cached = idempotencyService.getCached(form.getEventId());
        if (cached != null && cached.isCompleted()) {
            return ResponseEntity.ok(cached.getResponse());
        }

        // STRICT Token validation — Rollback is money operation (C-03)
        if (!signatureService.verifyStrict(form)) {
            return ResponseEntity.status(401).body(
                new ErrorVO("TOKEN_EXPIRED", "Expired token rejected. See §2.3.6")
            );
        }

        return walletTransactionService.rollback(form);

    } catch (Exception e) {
        logger.error("Rollback failed: " + e.getMessage(), e);
        return ResponseEntity.status(500).build();
    }
}

@Transactional(rollbackFor = Throwable.class)
public ResponseEntity<RollbackResponseVO> rollback(RollbackForm form) {
    String lockKey = "wallet:rollback:" + form.getPlayerId();
    RLock lock = redissonClient.getLock(lockKey);

    if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        return ResponseEntity.status(503).build();
    }

    try {
        // Find original debit transaction
        WalletTransaction originalTx = txRepo.findByEventId(form.getOriginalEventId());
        if (originalTx == null) {
            return ResponseEntity.status(404).body(
                new ErrorVO("TRANSACTION_NOT_FOUND", "Original transaction not found")
            );
        }

        // If already rolled back, return success (idempotent)
        WalletTransaction existingRollback = txRepo.findByReferencedTxId(originalTx.getId());
        if (existingRollback != null && existingRollback.isCompleted()) {
            return ResponseEntity.ok(buildRollbackResponse(existingRollback));
        }

        // Pessimistic lock original wallet
        WalletEntity wallet = walletRepo.findByIdForUpdate(originalTx.getWalletId());

        // Create reverse transaction
        WalletTransaction rollbackTx = new WalletTransaction();
        rollbackTx.setEventId(form.getEventId());
        rollbackTx.setPlayerId(form.getPlayerId());
        rollbackTx.setWalletId(wallet.getId());
        rollbackTx.setTxType(TransactionType.ROLLBACK);
        rollbackTx.setEventType("BET_CANCELLED");
        rollbackTx.setAmount(originalTx.getAmount().negate()); // Negative amount
        rollbackTx.setReferencedTxId(originalTx.getId());
        rollbackTx.setBalanceBefore(wallet.getBalance());

        // Reverse the debit (restore amount)
        wallet.setBalance(wallet.getBalance().add(originalTx.getAmount()));
        wallet.setVersion(wallet.getVersion() + 1);

        rollbackTx.setBalanceAfter(wallet.getBalance());
        rollbackTx.setTxStatus(TransactionStatus.COMPLETED);

        // Optimistic lock update
        int updated = walletRepo.updateBalanceWithVersion(
            wallet.getId(),
            wallet.getBalance(),
            wallet.getVersion()
        );
        if (updated == 0) {
            throw new OptimisticLockException("Wallet version mismatch on rollback");
        }

        // Mark original transaction as reversed
        originalTx.setTxStatus(TransactionStatus.REVERSED);
        txRepo.save(originalTx);

        // Persist rollback transaction
        txRepo.save(rollbackTx);

        // Update round status (if applicable)
        if (originalTx.getRoundId() != null) {
            GameRound round = gameRoundRepo.findById(originalTx.getRoundId());
            if (round != null && round.getStatus().equals("CLOSED")) {
                round.setStatus("CANCELLED");
                gameRoundRepo.save(round);
            }
        }

        RollbackResponseVO response = buildRollbackResponse(rollbackTx);
        idempotencyService.cache(form.getEventId(), response);
        eventPublisher.publish("igaming.wallet.events",
            new WalletRolledBackEvent(rollbackTx, originalTx, wallet));

        return ResponseEntity.ok(response);

    } finally {
        lock.unlock();
    }
}
```

### POST /igaming/seamless/getBalance

Simple balance query endpoint (cached).

**Request:**
```json
{
    "playerId": "p_123456",
    "timestamp": 1711270803,
    "signature": "..."
}
```

**Response (200 OK):**
```json
{
    "playerId": "p_123456",
    "cash": {
        "balance": 1000.00,
        "currency": "USD",
        "locked": 50.00
    },
    "bonus": {
        "balance": 200.00,
        "locked": 0.00
    },
    "credit": {
        "balance": 0.00,
        "locked": 0.00
    },
    "playable": 1150.00,
    "totalBalance": 1200.00,
    "lastUpdated": 1711270803000
}
```

**Implementation:**

```java
@PostMapping("/igaming/seamless/getBalance")
@RateLimit(rps = 1000, burst = 2000)
public ResponseEntity<BalanceResponseVO> getBalance(@RequestBody BalanceForm form) {
    // LENIENT Token validation — GetBalance is read-only, allows expired tokens (C-03)
    if (!signatureService.verifyLenient(form)) {
        return ResponseEntity.status(401).build();
    }

    WalletBalanceVO balance = walletService.getBalance(form.getPlayerId());
    return ResponseEntity.ok(new BalanceResponseVO(balance));
}

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepo;
    private final JetCache jetCache;

    @Cached(name = "wallet:balance:",
            key = "#playerId",
            cacheType = CacheType.BOTH,
            localExpire = 100,    // L1: 100 seconds
            expire = 3600)        // L2: 1 hour
    public WalletBalanceVO getBalance(Long playerId) {
        List<WalletEntity> wallets = walletRepo.findByPlayerId(playerId);

        WalletBalanceVO balance = new WalletBalanceVO();
        for (WalletEntity wallet : wallets) {
            switch (wallet.getWalletType()) {
                case CASH:
                    balance.setCash(new WalletDetailVO(
                        wallet.getBalance(),
                        wallet.getLockedAmount(),
                        wallet.getCurrency()
                    ));
                    break;
                case BONUS:
                    balance.setBonus(new WalletDetailVO(
                        wallet.getBalance(),
                        wallet.getLockedAmount(),
                        wallet.getCurrency()
                    ));
                    break;
                case CREDIT:
                    balance.setCredit(new WalletDetailVO(
                        wallet.getBalance(),
                        wallet.getLockedAmount(),
                        wallet.getCurrency()
                    ));
                    break;
            }
        }

        // Calculate playable balance
        BigDecimal playable = balance.getCash().getBalance()
            .subtract(balance.getCash().getLocked())
            .add(balance.getBonus().getBalance())
            .subtract(balance.getBonus().getLocked());
        balance.setPlayable(playable);

        return balance;
    }
}
```

### 2.3.6 Token 驗證策略 (Strict Policy — C-03)

All Seamless Wallet endpoints enforce Token validation. Per business decision, **ALL money-related operations strictly reject expired tokens**. Only GetBalance allows lenient validation.

**Endpoint Token Policy Matrix:**

| Endpoint | Expired Token | Invalid Signature | Replay (one-time used) |
|----------|:------------:|:-----------------:|:---------------------:|
| `/authenticate` | ✅ Allow (lenient) | ❌ Reject 401 | ✅ Allow |
| `/getBalance` | ✅ Allow (lenient) | ❌ Reject 401 | ✅ Allow |
| `/debit` | ❌ **Reject 401** | ❌ Reject 401 | ❌ Reject 409 |
| `/credit` | ❌ **Reject 401** | ❌ Reject 401 | ❌ Reject 409 |
| `/rollback` | ❌ **Reject 401** | ❌ Reject 401 | ❌ Reject 409 |
| `/adjust` | ❌ **Reject 401** | ❌ Reject 401 | ❌ Reject 409 |

**SignatureService Implementation:**

```java
@Service
public class SignatureService {

    private static final long TOKEN_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Strict verification — rejects expired tokens.
     * Used by: debit, credit, rollback, adjust
     */
    public boolean verifyStrict(BaseForm form) {
        long clockSkew = Math.abs(System.currentTimeMillis() - form.getTimestamp() * 1000);
        if (clockSkew > TOKEN_TTL_MS) {
            return false; // Expired — NO grace period
        }
        return verifyHmac(form);
    }

    /**
     * Lenient verification — allows expired tokens (only checks signature).
     * Used by: authenticate, getBalance
     */
    public boolean verifyLenient(BaseForm form) {
        return verifyHmac(form); // Signature must still be valid
    }

    private boolean verifyHmac(BaseForm form) {
        String expected = HmacUtils.hmacSha256Hex(apiSecret,
            form.getSessionToken() + form.getTimestamp());
        return MessageDigest.isEqual(
            expected.getBytes(), form.getSignature().getBytes());
    }
}
```

**Resettlement Flow (for rejected expired-token credits):**

When GP sends a Credit with an expired token and it is rejected:
1. System returns `401 TOKEN_EXPIRED` with Resettlement instructions
2. GP or platform support creates a **Resettlement ticket** (manual review)
3. After verification (matching round_id, game records, amount), CS agent approves
4. Platform issues an **Adjust** endpoint call using a fresh platform-signed token
5. Amount is credited to player's CASH wallet
6. Audit log records: original GP event_id, resettlement ticket_id, approver

```sql
CREATE TABLE t_resettlement (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    original_event_id VARCHAR(256) NOT NULL,
    round_id VARCHAR(256),
    game_provider VARCHAR(50) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, COMPLETED
    approved_by VARCHAR(100),
    completed_tx_id BIGINT, -- FK to t_wallet_transaction after Adjust
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    CONSTRAINT uk_resettlement_event UNIQUE (original_event_id, tenant_id)
);
```

---

### 2.3.7 多幣種外匯核算 (Multi-Currency FX — C-05)

**Strategy**: 存款時轉換 (Convert on Deposit) — per business decision, all deposits are converted to the merchant's settlement fiat at deposit time. Betting is unified in settlement currency. Withdrawal converts back.

**FX Rate Provider Schema:**

```sql
CREATE TABLE t_fx_rate (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(10) NOT NULL,       -- e.g., EUR
    quote_currency VARCHAR(10) NOT NULL,      -- e.g., USD (settlement currency)
    rate DECIMAL(19,8) NOT NULL,              -- e.g., 1.0850
    inverse_rate DECIMAL(19,8) NOT NULL,      -- e.g., 0.9217
    provider VARCHAR(50) NOT NULL,            -- e.g., 'CURRENCY_LAYER', 'BINANCE'
    fetched_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_crypto BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_fx_rate UNIQUE (base_currency, quote_currency, fetched_at)
);

CREATE INDEX idx_fx_rate_pair_time ON t_fx_rate(base_currency, quote_currency, fetched_at DESC);
```

**FX Rate Cache (Redis):**

```
Key:    fx:{base}:{quote}           e.g., fx:EUR:USD
Value:  { rate: 1.0850, fetchedAt: ..., expiresAt: ... }
TTL:    300s (fiat) / 30s (crypto)
```

**Deposit-Time Conversion Flow:**

```java
@Service
public class FxConversionService {

    private static final long FIAT_RATE_TTL_MS = 5 * 60 * 1000;    // 5 min
    private static final long CRYPTO_RATE_TTL_MS = 30 * 1000;       // 30 sec
    private static final long RATE_FREEZE_MS = 15 * 60 * 1000;      // 15 min freeze
    private static final BigDecimal CRYPTO_SLIPPAGE_LIMIT = new BigDecimal("0.02"); // 2%

    /**
     * Convert deposit from player's currency to merchant's settlement currency.
     * Rate is frozen for 15 minutes from quote time.
     */
    public FxConversionResult convertDeposit(
            BigDecimal amount,
            String playerCurrency,
            String settlementCurrency,
            Long tenantId) {

        if (playerCurrency.equals(settlementCurrency)) {
            return FxConversionResult.noConversion(amount);
        }

        // Get latest rate
        FxRate rate = getLatestRate(playerCurrency, settlementCurrency);

        // Freeze rate for 15 minutes
        String freezeKey = "fx:freeze:" + tenantId + ":" + UUID.randomUUID();
        redisTemplate.opsForValue().set(freezeKey,
            objectMapper.writeValueAsString(rate), RATE_FREEZE_MS, TimeUnit.MILLISECONDS);

        // Calculate converted amount
        BigDecimal convertedAmount = amount.multiply(rate.getRate())
            .setScale(4, RoundingMode.HALF_UP);

        // Crypto slippage protection
        if (rate.isCrypto()) {
            BigDecimal currentRate = getLatestRate(playerCurrency, settlementCurrency).getRate();
            BigDecimal slippage = currentRate.subtract(rate.getRate())
                .divide(rate.getRate(), 6, RoundingMode.HALF_UP).abs();
            if (slippage.compareTo(CRYPTO_SLIPPAGE_LIMIT) > 0) {
                throw new FxSlippageExceededException(
                    "Slippage " + slippage + " exceeds limit " + CRYPTO_SLIPPAGE_LIMIT);
            }
        }

        return new FxConversionResult(
            amount, playerCurrency,
            convertedAmount, settlementCurrency,
            rate.getRate(), rate.getFetchedAt(), freezeKey
        );
    }

    /**
     * Convert withdrawal from settlement currency back to player's requested currency.
     */
    public FxConversionResult convertWithdrawal(
            BigDecimal settlementAmount,
            String settlementCurrency,
            String targetCurrency) {

        if (settlementCurrency.equals(targetCurrency)) {
            return FxConversionResult.noConversion(settlementAmount);
        }

        FxRate rate = getLatestRate(settlementCurrency, targetCurrency);
        BigDecimal targetAmount = settlementAmount.multiply(rate.getRate())
            .setScale(4, RoundingMode.HALF_UP);

        return new FxConversionResult(
            settlementAmount, settlementCurrency,
            targetAmount, targetCurrency,
            rate.getRate(), rate.getFetchedAt(), null
        );
    }
}
```

**Wallet Transaction with FX Fields:**

```sql
-- Additional columns for t_wallet_transaction (ALTER TABLE)
ALTER TABLE t_wallet_transaction ADD COLUMN original_currency VARCHAR(10);
ALTER TABLE t_wallet_transaction ADD COLUMN original_amount DECIMAL(19,4);
ALTER TABLE t_wallet_transaction ADD COLUMN fx_rate DECIMAL(19,8);
ALTER TABLE t_wallet_transaction ADD COLUMN fx_rate_frozen_at TIMESTAMPTZ;
```

**Report Currency Unification:**
- Player-facing reports: settlement fiat (商戶定義法幣)
- Merchant-facing reports: settlement currency
- Platform-level reports: USD (unified)

---

### 2.3.7a FX Absorption Thresholds — Configurable Parameters (F-04)

**Overview**: FX volatility absorption thresholds are **database-configurable** (not hardcoded), enabling runtime adjustments without code deployment. Confirmed by Ron (2026-03-25).

**Configuration Parameters:**

| Parameter | Key | Default | Type | Description |
|-----------|-----|---------|------|-------------|
| Fiat Absorption % | `ch2.fx.fiat_absorption_pct` | ≤0.5% | Decimal | Fiat pairs (USD/EUR, etc.): platform absorbs up to this threshold; above splits 50/50 player |
| Crypto Absorption % | `ch2.fx.crypto_absorption_pct` | ≤2% | Decimal | Crypto pairs (BTC/USD, etc.): platform absorbs up to this threshold; above shifts to player |
| Rate Lock Window | `ch2.fx.rate_lock_window_seconds` | 900 (15min) | Integer | Quote-to-settlement freeze duration in seconds; prevents re-quoting within window |

**Retrieval (Java Example):**

```java
// Load from t_parameter table
BigDecimal fiatAbsorptionPct = parameterService.getAsDecimal("ch2.fx.fiat_absorption_pct");
BigDecimal cryptoAbsorptionPct = parameterService.getAsDecimal("ch2.fx.crypto_absorption_pct");
Integer rateLockWindow = parameterService.getAsInteger("ch2.fx.rate_lock_window_seconds");

// Applied in FxConversionService.calculateVolatility()
if (rate.isCrypto()) {
    if (volatility.compareTo(cryptoAbsorptionPct) > 0) {
        playerBearsCost = true;
    }
} else {
    if (volatility.compareTo(fiatAbsorptionPct) > 0) {
        splitCost = true; // 50/50 split above threshold
    }
}
```

**Cross-Module References:**
- **F-04 Risk Absorption Rules**: Technical_Sprint_v2.2_Implementation_Guide §F-04 — complete formula, API contracts, audit trails
- **BS-10 Multi-Currency × FX Boundary**: Technical_Cross_Module_Boundary_Scenarios §BS-10 — multi-currency withdrawal × FX volatility interaction, rate-lock failure handling, async queue fallback

---

### 2.3.8 負餘額容忍與自動補償 (Negative Balance Tolerance — H-01)

In extreme concurrent scenarios (multiple GPs debit simultaneously), a momentary negative balance may occur before DB constraints catch it.

**Tolerance Policy:**
- Window: ≤ 5 seconds
- If balance < 0 for > 5 seconds → **auto-compensation** triggered
- Compensation order: BONUS → CASH → manual intervention

**Compensation Job:**

```java
@Scheduled(fixedDelay = 5000) // Every 5 seconds
public void compensateNegativeBalances() {
    List<WalletEntity> negativeWallets = walletRepo.findNegativeBalances();

    for (WalletEntity wallet : negativeWallets) {
        Duration negDuration = Duration.between(wallet.getUpdatedAt(), Instant.now());
        if (negDuration.getSeconds() < 5) {
            continue; // Within tolerance window, skip
        }

        BigDecimal deficit = wallet.getBalance().negate(); // positive amount needed

        // Step 1: Try BONUS wallet
        WalletEntity bonusWallet = walletRepo.findByPlayerIdAndType(
            wallet.getPlayerId(), WalletType.BONUS);
        if (bonusWallet != null && bonusWallet.getBalance().compareTo(deficit) >= 0) {
            transferCompensation(bonusWallet, wallet, deficit, "AUTO_BONUS_COMPENSATE");
            continue;
        }

        // Step 2: Zero out and create manual review ticket
        wallet.setBalance(BigDecimal.ZERO);
        walletRepo.save(wallet);

        // Create incident for manual resolution
        incidentService.createNegativeBalanceIncident(wallet, deficit);
        alertService.sendAlert("NEGATIVE_BALANCE",
            "Player " + wallet.getPlayerId() + " deficit: " + deficit);
    }
}
```

**Alert Thresholds:**

| Condition | Alert Level | Action |
|-----------|------------|--------|
| Any negative balance > 5s | WARNING | Auto-compensation attempt |
| Negative balance > $100 | HIGH | Notify risk team |
| Negative balance > $1,000 | CRITICAL | Freeze player + CFO alert |
| 3+ negative events/hour (same player) | CRITICAL | Fraud investigation trigger |

---

## 2.4 三層冪等防護 (ADR-015)

Comprehensive idempotency mechanism to prevent duplicate transaction processing:

### Layer 1: Redis Fast Check

```java
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final long CACHE_TTL = 3600; // 1 hour

    /**
     * Check if request already processed (fast path ~5ms)
     */
    public Option<WalletResponseVO> checkCached(String eventId) {
        String key = KEY_PREFIX + eventId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return Option.of(JSON.parseObject(cached, WalletResponseVO.class));
            } catch (Exception e) {
                logger.warn("Failed to parse cached response: " + e.getMessage());
                return Option.none();
            }
        }
        return Option.none();
    }

    /**
     * Cache successful response for future retries
     */
    public void cache(String eventId, WalletResponseVO response) {
        String key = KEY_PREFIX + eventId;
        redisTemplate.opsForValue().set(
            key,
            JSON.toJSONString(response),
            Duration.ofSeconds(CACHE_TTL)
        );
    }

    /**
     * Mark request as in-flight (optimistic SETNX)
     */
    public boolean markInFlight(String eventId) {
        String key = KEY_PREFIX + eventId + ":inflight";
        Boolean result = redisTemplate.opsForValue().setIfAbsent(
            key,
            "1",
            Duration.ofSeconds(30) // 30 second processing window
        );
        return Boolean.TRUE.equals(result);
    }

    public void clearInFlight(String eventId) {
        String key = KEY_PREFIX + eventId + ":inflight";
        redisTemplate.delete(key);
    }
}
```

### Layer 2: Database UNIQUE Constraint

The `event_id` column in `t_wallet_transaction` has `UNIQUE` constraint per tenant. If duplicate `event_id` is inserted, PostgreSQL throws `IntegrityConstraintViolationException` which is caught and converts to "already processed" response.

```java
@Transactional(rollbackFor = Throwable.class)
public ResponseEntity<DebitResponseVO> debit(DebitForm form) {
    try {
        WalletTransaction tx = new WalletTransaction();
        tx.setEventId(form.getEventId()); // UNIQUE constraint enforced
        // ... populate transaction ...
        txRepo.save(tx);

    } catch (DataIntegrityViolationException e) {
        // Duplicate event_id: return cached result
        WalletTransaction existing = txRepo.findByEventId(form.getEventId());
        if (existing != null && existing.isCompleted()) {
            return ResponseEntity.ok(buildDebitResponse(existing));
        }
        throw new WalletException("Failed to save transaction: " + e.getMessage());
    }
}
```

### Layer 3: SELECT WHERE Fallback

Fallback query if Redis cache miss and DB insert fails:

```java
public Option<WalletTransaction> findByTransactionId(String txId) {
    return Option.of(
        walletTransactionRepo.findByTransactionId(txId)
    );
}

/**
 * Idempotency check with all three layers
 */
public WalletResponseVO ensureIdempotency(String eventId, Supplier<WalletResponseVO> processor) {
    // Layer 1: Redis cache
    Option<WalletResponseVO> cached = idempotencyGuard.checkCached(eventId);
    if (cached.isDefined()) {
        return cached.get();
    }

    // Layer 2: DB UNIQUE constraint will catch duplicates during insert
    // Layer 3: SELECT fallback for safety
    WalletTransaction existing = walletTransactionRepo.findByEventId(eventId);
    if (existing != null && existing.isCompleted()) {
        WalletResponseVO response = buildResponse(existing);
        idempotencyGuard.cache(eventId, response);
        return response;
    }

    // Process new request
    WalletResponseVO response = processor.get();
    idempotencyGuard.cache(eventId, response);
    return response;
}
```

### Idempotency Flow Diagram

```
Debit Request (eventId=X)
    ↓
[Layer 1] Redis GET idempotency:X
    ↓ (miss)
[Layer 2] Mark in-flight: SETNX idempotency:X:inflight (30s TTL)
    ↓ (success)
Acquire distributed lock
    ↓
[Layer 3] SELECT FROM t_wallet_transaction WHERE event_id = X
    ↓ (not found)
Process transaction
    ↓
Try INSERT into t_wallet_transaction (UNIQUE event_id constraint)
    ↓ (success)
[Layer 1] SETEX idempotency:X (1h TTL) with response
    ↓
Clear in-flight flag
    ↓
Return response
```

## 2.5 三層並發控制

Three-layer concurrency control ensuring serialization and atomicity:

```java
@Component
@RequiredArgsConstructor
public class WalletManager {
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;
    private final RedissonClient redissonClient;
    private final EventPublisher eventPublisher;

    /**
     * Triple-layer concurrency control:
     * 1. Distributed lock (Redisson)
     * 2. Row-level pessimistic lock (SELECT FOR UPDATE)
     * 3. Version-based optimistic lock (on UPDATE)
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Throwable.class
    )
    public WalletResponseVO debit(DebitForm form) throws LockAcquisitionException {
        Long playerId = form.getPlayerId();
        String lockKey = "wallet:debit:" + playerId;

        // === LAYER 1: DISTRIBUTED LOCK (Redisson) ===
        // Prevents concurrent requests from different app instances
        RLock distributedLock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;

        try {
            lockAcquired = distributedLock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                logger.warn("Failed to acquire distributed lock for player " + playerId);
                throw new LockAcquisitionException("Cannot acquire lock (timeout 5s)");
            }

            // === LAYER 2: PESSIMISTIC ROW LOCK (PostgreSQL SELECT FOR UPDATE) ===
            // Prevents concurrent transactions on same wallet within same database
            WalletEntity wallet = walletRepo.findByPlayerIdAndTypeForUpdate(
                playerId,
                WalletType.CASH
            );

            if (wallet == null) {
                throw new WalletNotFoundException("Wallet not found");
            }

            // Load bonuses
            List<WalletBonusExt> bonuses = walletRepo.findActiveBonus(playerId);

            // Perform business logic
            BigDecimal amountToDebit = form.getAmount();
            walletRepo.validateBalance(wallet, amountToDebit);

            // Deduct from bonus first
            for (WalletBonusExt bonus : bonuses) {
                if (amountToDebit.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal deductFromBonus = min(
                    bonus.getBalance(),
                    amountToDebit
                );
                bonus.setBalance(bonus.getBalance().subtract(deductFromBonus));
                amountToDebit = amountToDebit.subtract(deductFromBonus);
            }

            // Deduct remaining from cash
            wallet.setBalance(wallet.getBalance().subtract(amountToDebit));
            wallet.setUpdatedAt(Instant.now());

            // === LAYER 3: OPTIMISTIC LOCK (Version field) ===
            // Version is incremented before UPDATE, old version in WHERE clause
            long oldVersion = wallet.getVersion();
            wallet.setVersion(oldVersion + 1);

            // This UPDATE will fail if version != oldVersion (another thread modified)
            int rowsUpdated = walletRepo.updateBalanceWithVersionCheck(
                wallet.getId(),
                wallet.getBalance(),
                oldVersion,      // WHERE version = ?
                wallet.getVersion() // SET version = ?
            );

            if (rowsUpdated == 0) {
                // Version mismatch: retry or fail
                logger.warn("Version mismatch for wallet " + wallet.getId() +
                    ", expected " + oldVersion);
                throw new OptimisticLockException("Wallet was modified by another transaction");
            }

            // === TRANSACTION LOGGING ===
            WalletTransaction tx = new WalletTransaction();
            tx.setWalletId(wallet.getId());
            tx.setPlayerId(playerId);
            tx.setTxType(TransactionType.DEBIT);
            tx.setAmount(form.getAmount());
            tx.setBalanceBefore(wallet.getBalance().add(form.getAmount()));
            tx.setBalanceAfter(wallet.getBalance());
            tx.setEventId(form.getEventId());
            tx.setTxStatus(TransactionStatus.COMPLETED);
            txRepo.save(tx);

            // === EVENT PUBLISHING (for cache invalidation) ===
            eventPublisher.publish("wallet.events", new WalletDebitedEvent(
                playerId,
                wallet.getBalance(),
                form.getAmount()
            ));

            return new WalletResponseVO(wallet.getId(), wallet.getBalance());

        } finally {
            if (lockAcquired) {
                distributedLock.unlock();
            }
        }
    }
}
```

### Concurrency Control Lock Ordering

```
Player A: Debit Request          Player B: Credit Request (same player)
    ↓                                    ↓
[LAYER 1] Redisson Lock                [LAYER 1] Redisson Lock (blocked)
(player:123 acquired)                  (player:123 waiting)
    ↓
[LAYER 2] SELECT FOR UPDATE wallet
    ↓
[LAYER 3] Optimistic lock version=1
    ↓
Business logic (validate, calculate)
    ↓
UPDATE wallet SET balance=..., version=2 WHERE id=X AND version=1
    ↓ (success)
Commit transaction
    ↓
Release Redisson lock
    ↓                                    ↓
                            [LAYER 1] Redisson Lock acquired
                                    ↓
                            [LAYER 2] SELECT FOR UPDATE wallet (version now 2)
                                    ↓
                            [LAYER 3] Optimistic lock version=2
                                    ↓
                            UPDATE wallet SET balance=..., version=3 WHERE id=X AND version=2
                                    ↓ (success)
                            Release Redisson lock
```

## 2.6 回合生命週期 (Round Lifecycle)

Game round state machine:

```
┌─────────────────────────────────────────────────────────┐
│                   ROUND_STATE_MACHINE                   │
└─────────────────────────────────────────────────────────┘

[OPEN]
  │ (Initial state, player places bets)
  │
  ├─→ [CLOSED]
  │    (All bets settled, wait for final win settlement)
  │    │
  │    ├─→ [PENDING_REVIEW]
  │    │    (Anomaly detected, manual review required)
  │    │    │
  │    │    └─→ [ADJUSTED]
  │    │         (Resettlement applied)
  │    │
  │    └─→ [COMPLETED]
  │         (Final state, no further changes allowed)
  │
  ├─→ [TIMEOUT]
  │    (No activity within TTL, round abandoned)
  │
  └─→ [CANCELLED]
       (Game provider cancelled, all bets rolled back)

State Transitions:
OPEN → CLOSED: All bets debited + win applied
OPEN → TIMEOUT: TTL expired (configurable, e.g., 30 mins)
OPEN → CANCELLED: Game provider sends cancel event
CLOSED → PENDING_REVIEW: Anomaly flag (large bet, suspected fraud, etc.)
PENDING_REVIEW → ADJUSTED: Manual/automated adjustment applied
CLOSED → COMPLETED: Reconciliation passed, locked
CANCELLED: (terminal)
TIMEOUT: (terminal, but can be overridden by late win within grace period)

Round entity:
{
  round_id: string (unique per game_provider + game_id)
  player_id: bigint
  game_id: string
  game_provider: string
  status: enum (OPEN, CLOSED, PENDING_REVIEW, ADJUSTED, COMPLETED, TIMEOUT, CANCELLED)
  total_bet: decimal
  total_win: decimal
  net_win: decimal (win - bet)
  opened_at: timestamp
  closed_at: timestamp
  timeout_at: timestamp (now + TTL)
  metadata: jsonb (game-specific data)
}
```

```java
@Entity
@Table(name = "t_game_round")
public class GameRound {
    @Id
    private String roundId;

    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    private Instant openedAt;
    private Instant closedAt;
    private Instant timeoutAt;

    @Version
    private Long version;

    public void closeRound() {
        if (!this.status.equals(RoundStatus.OPEN)) {
            throw new InvalidStateException("Can only close OPEN rounds");
        }
        this.status = RoundStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void markForReview(String reason) {
        if (!this.status.equals(RoundStatus.CLOSED)) {
            throw new InvalidStateException("Can only review CLOSED rounds");
        }
        this.status = RoundStatus.PENDING_REVIEW;
        this.metadata.put("review_reason", reason);
    }

    public void adjust(BigDecimal adjustment) {
        if (!this.status.equals(RoundStatus.PENDING_REVIEW)) {
            throw new InvalidStateException("Can only adjust PENDING_REVIEW rounds");
        }
        this.status = RoundStatus.ADJUSTED;
        this.netWin = this.netWin.add(adjustment);
    }
}
```

## 2.7 可下注餘額計算 (Playable Balance)

Formula for available balance (excludable):

```
Playable Balance = (CASH.balance - CASH.locked) + (BONUS.balance - BONUS.locked)
```

If CREDIT wallet enabled (agent mode):
```
Playable Balance = ... + CREDIT.balance - CREDIT.locked
```

Only CASH + BONUS + CREDIT balances count toward playable. Locked amounts (pending bets, pending withdrawals) are excluded.

```java
public class BalanceCalculator {

    public BigDecimal getPlayableBalance(Long playerId, List<WalletEntity> wallets) {
        BigDecimal playable = BigDecimal.ZERO;

        for (WalletEntity wallet : wallets) {
            // Only include unlocked portions
            BigDecimal unlockedAmount = wallet.getBalance()
                .subtract(wallet.getLockedAmount());

            // Only include active wallet types
            if (wallet.getWalletType().isPlayable()) {
                playable = playable.add(unlockedAmount);
            }
        }

        return playable.max(BigDecimal.ZERO); // No negative
    }

    public BigDecimal getTotalBalance(Long playerId, List<WalletEntity> wallets) {
        return wallets.stream()
            .filter(w -> w.getWalletType().isPlayable())
            .map(WalletEntity::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getLockedAmount(Long playerId, List<WalletEntity> wallets) {
        return wallets.stream()
            .filter(w -> w.getWalletType().isPlayable())
            .map(WalletEntity::getLockedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## 2.8 快取策略 (Caching Strategy)

Two-level caching with JetCache (L1 local + L2 distributed):

```java
@Service
@RequiredArgsConstructor
public class WalletCacheService {
    private final WalletRepository walletRepo;
    private final Cache<String, WalletBalanceVO> walletCache;

    /**
     * L1: Local cache (Caffeine, 100 second TTL)
     * L2: Distributed cache (Redis, 1 hour TTL)
     * Invalidation: On every debit/credit/rollback
     */
    @Cached(
        name = "wallet:balance",
        key = "'balance:' + #tenantId + ':' + #playerId",
        cacheType = CacheType.BOTH,  // BOTH = L1 + L2
        localExpire = 100,            // L1 TTL: 100 seconds
        expire = 3600,                // L2 TTL: 1 hour
        condition = "#result != null"
    )
    public WalletBalanceVO getBalance(Long tenantId, Long playerId) {
        List<WalletEntity> wallets = walletRepo.findByTenantIdAndPlayerId(
            tenantId,
            playerId
        );
        return buildBalanceVO(wallets);
    }

    /**
     * Cache invalidation on transaction
     */
    @CacheInvalidate(
        name = "wallet:balance",
        key = "'balance:' + #tenantId + ':' + #playerId"
    )
    public void invalidateCache(Long tenantId, Long playerId) {
        // Annotation handles invalidation
    }

    /**
     * Explicit cache refresh (after debit/credit)
     */
    public void refreshBalance(Long tenantId, Long playerId) {
        invalidateCache(tenantId, playerId);
        // Next getBalance call will hit DB
    }

    /**
     * Cache statistics
     */
    public CacheStats getCacheStats() {
        return walletCache.getCacheStats();
    }
}
```

Cache invalidation event listener:

```java
@Component
@RequiredArgsConstructor
public class WalletCacheInvalidationListener {
    private final WalletCacheService cacheService;

    @EventListener(WalletDebitedEvent.class)
    public void onWalletDebited(WalletDebitedEvent event) {
        cacheService.invalidateCache(
            event.getTenantId(),
            event.getPlayerId()
        );
        logger.debug("Cache invalidated for wallet debit: " + event.getPlayerId());
    }

    @EventListener(WalletCreditedEvent.class)
    public void onWalletCredited(WalletCreditedEvent event) {
        cacheService.invalidateCache(
            event.getTenantId(),
            event.getPlayerId()
        );
        logger.debug("Cache invalidated for wallet credit: " + event.getPlayerId());
    }

    @EventListener(WalletRolledBackEvent.class)
    public void onWalletRolledBack(WalletRolledBackEvent event) {
        cacheService.invalidateCache(
            event.getTenantId(),
            event.getPlayerId()
        );
        logger.debug("Cache invalidated for wallet rollback: " + event.getPlayerId());
    }
}
```

## 2.9 對帳機制 (Reconciliation)

Three-layer reconciliation approach:

### Layer 1: Real-Time (0ms)

Every game provider callback immediately records transaction:

```java
@PostMapping("/igaming/seamless/debit")
public ResponseEntity<DebitResponseVO> debit(@RequestBody DebitForm form) {
    // Immediate record in t_wallet_transaction
    WalletTransaction tx = new WalletTransaction();
    tx.setEventId(form.getEventId());
    tx.setGameProvider(form.getGameProviderId());
    tx.setTxStatus(TransactionStatus.COMPLETED);
    txRepo.save(tx); // Now in DB

    return ResponseEntity.ok(response);
}
```

### Layer 2: Compensatory (5 minutes)

Poll game provider API every 5 minutes, compare with our records, patch missing:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CompensatoryReconciliationService {
    private final WalletTransactionRepository txRepo;
    private final GameProviderApiClient gpClient;
    private final ReconciliationService reconciliationService;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void compensatoryReconciliation() {
        List<String> providers = gameProviderConfig.getActiveProviders();

        for (String provider : providers) {
            try {
                // Poll GP API for last 10 minutes of transactions
                List<GameProviderTransaction> gpTxs = gpClient.getTransactions(
                    provider,
                    Duration.ofMinutes(10)
                );

                // Compare with our records
                for (GameProviderTransaction gpTx : gpTxs) {
                    WalletTransaction ourTx = txRepo.findByEventId(gpTx.getEventId());

                    if (ourTx == null) {
                        // Missing transaction: create compensatory entry
                        logger.warn("Missing transaction " + gpTx.getEventId() +
                            " from " + provider + ", creating compensatory record");
                        reconciliationService.createCompensatoryTransaction(gpTx);
                    } else if (!ourTx.getAmount().equals(gpTx.getAmount())) {
                        // Amount mismatch
                        logger.error("Amount mismatch for " + gpTx.getEventId() +
                            ": GP=" + gpTx.getAmount() + ", Ours=" + ourTx.getAmount());
                        reconciliationService.flagForManualReview(gpTx, ourTx);
                    }
                }
            } catch (Exception e) {
                logger.error("Compensatory reconciliation failed for " + provider, e);
                alertService.sendAlert("Reconciliation failure: " + provider);
            }
        }
    }
}
```

### Layer 3: Daily Settlement

Import game provider settlement report, final diff, manual review report:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DailySettlementService {
    private final WalletTransactionRepository txRepo;
    private final SettlementReportRepository settlementRepo;
    private final GameProviderApiClient gpClient;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 2 * * *") // Every day at 2 AM
    public void dailySettlement() {
        LocalDate settlementDate = LocalDate.now().minusDays(1);

        for (String provider : gameProviderConfig.getActiveProviders()) {
            try {
                // Import settlement report from GP
                SettlementReport gpReport = gpClient.getSettlementReport(
                    provider,
                    settlementDate
                );

                // Sum our transactions for same date
                BigDecimal ourTotal = txRepo.sumAmountByDate(
                    provider,
                    settlementDate
                );

                // Compare
                BigDecimal diff = gpReport.getTotal().subtract(ourTotal);

                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                    logger.warn("Settlement diff for " + provider + " on " + settlementDate +
                        ": " + diff);

                    SettlementReconciliation reconciliation = new SettlementReconciliation();
                    reconciliation.setProvider(provider);
                    reconciliation.setSettlementDate(settlementDate);
                    reconciliation.setGpTotal(gpReport.getTotal());
                    reconciliation.setOurTotal(ourTotal);
                    reconciliation.setDiff(diff);
                    reconciliation.setStatus(ReconciliationStatus.PENDING_REVIEW);
                    settlementRepo.save(reconciliation);

                    // Notify reconciliation team
                    notificationService.notify(
                        "RECONCILIATION_ALERT",
                        "Settlement discrepancy: " + provider + " diff=" + diff
                    );
                }
            } catch (Exception e) {
                logger.error("Daily settlement failed for " + provider, e);
                alertService.sendAlert("Daily settlement failure: " + provider);
            }
        }
    }
}
```

## 2.10 九大交易情境處理 (9 Transaction Scenarios)

### Scenario A: Insufficient Funds

When player tries to bet more than playable balance.

```
Request: Debit 100 USD
Current: CASH=50, BONUS=30, locked=0
Playable: 80 USD
Result: REJECTED (409 Conflict)
Reason: 100 > 80
```

**Handling:**
```java
if (playableBalance.compareTo(form.getAmount()) < 0) {
    return ResponseEntity.status(409).body(new ErrorVO(
        "INSUFFICIENT_FUNDS",
        "Playable " + playableBalance + " < requested " + form.getAmount()
    ));
}
```

No wallet state changes, no transaction recorded.

### Scenario B: Concurrent Requests

Two requests simultaneously trying to debit from same player wallet.

```
Request A: Debit 50 at T0
Request B: Debit 50 at T0 (same player)
Balance: 80
Result: A succeeds (30 remaining), B fails (not enough)
```

**Handling (Serialization via Locks):**
- Request A acquires distributed lock → pessimistic lock → processes
- Request B waits for A's lock → acquires lock → checks balance → finds 30 remaining → fails
- No race condition, serialized processing

### Scenario C: Timeout & Retry

Game provider doesn't receive response, retries after 2 seconds.

```
Request 1 (eventId=X): Debit 50
  → Processing (slow)
  → Timeout (30s)
Request 2 (eventId=X): Debit 50 (RETRY)
  → [Layer 1] Redis check: FOUND (cached response)
  → Return immediately (idempotent)
```

**Result:** Exact same response, no duplicate debit.

### Scenario D: Out-of-Order Events

Win arrives before final bet settlement.

```
Request 1: Bet Placed (roundId=R1) - Balance: 100
Request 2: Win 200 (roundId=R1) - Arrives before Bet settled
  → Round state validation: round NOT CLOSED yet
  → REJECTED (409)
  → Wait for bet settlement callback
Request 3: Bet Settled
Request 2 (RETRY): Win 200
  → Round NOW CLOSED
  → ACCEPTED
  → New balance: 300
```

**Handling:**
```java
GameRound round = gameRoundRepo.findById(form.getRoundId());
if (!round.isClosedOrPending()) {
    return ResponseEntity.status(409).body(
        new ErrorVO("ROUND_NOT_CLOSED", "Round state: " + round.getStatus())
    );
}
```

### Scenario E: Rollback (Bet Cancellation)

Game provider cancels bet due to technical issue.

```
Request 1: Debit 50 (eventId=X)
  → Balance: 50, Transaction recorded (tx_id=1)
Request 2: Rollback (originalEventId=X)
  → Find original transaction (tx_id=1)
  → Check if already reversed: No
  → Create reverse transaction (amount=-50)
  → Mark tx_id=1 as REVERSED
  → Update round status: CLOSED → CANCELLED
  → New balance: 100
```

**Handling:**
```java
WalletTransaction rollbackTx = new WalletTransaction();
rollbackTx.setEventId(form.getEventId());
rollbackTx.setReferencedTxId(originalTx.getId());
rollbackTx.setTxType(TransactionType.ROLLBACK);
rollbackTx.setAmount(originalTx.getAmount().negate());

wallet.setBalance(wallet.getBalance().add(originalTx.getAmount()));
txRepo.save(rollbackTx);
originalTx.setTxStatus(TransactionStatus.REVERSED);
txRepo.save(originalTx);
```

Idempotent: Rollback of already-rolled-back = success (returns cached response).

### Scenario F: Resettlement (Dispute/Anomaly)

Large bet flagged for manual review, then adjusted.

```
Round: R1, Status: CLOSED
Bet: 500, Win: 1000
Net: +500

Anomaly Detection:
  → Bet size unusual (flagged as max bet possible)
  → Auto-mark: PENDING_REVIEW

Manual Review:
  → Investigation shows fair play
  → Adjustment: NONE (keep as is)
  → Status: ADJUSTED → COMPLETED

OR

Manual Review:
  → Suspected collusion detected
  → Adjustment: Win reduced to 600
  → Net adjustment: -400
  → Debit player 400
  → Status: ADJUSTED → COMPLETED
```

**Handling:**
```java
public void adjustRound(String roundId, BigDecimal adjustment) {
    GameRound round = gameRoundRepo.findByIdForUpdate(roundId);
    if (!round.getStatus().equals(RoundStatus.PENDING_REVIEW)) {
        throw new InvalidStateException();
    }

    if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
        // Debit player
        WalletEntity wallet = walletRepo.findByPlayerIdForUpdate(round.getPlayerId());
        wallet.setBalance(wallet.getBalance().add(adjustment)); // negative
        walletRepo.save(wallet);
    }

    round.setStatus(RoundStatus.ADJUSTED);
    gameRoundRepo.save(round);
}
```

### Scenario G: Free Spins

Free spin bonus granted (no monetary deduction).

```
Request: Free Spin Granted
  → Amount: 0 USD
  → Create transaction with amount=0
  → Update BONUS wallet balance
  → Create bonus_ext entry with wagering requirement
  → No cash movement
```

**Handling:**
```java
if (form.getAmount().equals(BigDecimal.ZERO)) {
    // Free spin: no balance change, just log
    WalletTransaction freeTx = new WalletTransaction();
    freeTx.setAmount(BigDecimal.ZERO);
    freeTx.setEventType("FREE_SPIN");
    txRepo.save(freeTx);

    // Record bonus
    WalletBonusExt bonus = new WalletBonusExt();
    bonus.setBonusAmount(form.getFreeSpinValue());
    bonus.setRequiredAmount(form.getFreeSpinValue().multiply(new BigDecimal(40))); // 40x
    bonusRepo.save(bonus);
}
```

### Scenario H: Bonus Wallet

Bonus funds can only be used for gambling, not withdrawal.

```
Player: CASH=100, BONUS=50, Playable=150

Bet 60 (from BONUS):
  → Deduct 50 from BONUS (all)
  → Deduct 10 from CASH
  → Wagering tracked: 60 toward bonus requirement

Win 100 (winnings from bonus bet):
  → Credit 100 to CASH (never to BONUS)
  → Player: CASH=190, BONUS=0

Withdrawal attempt:
  → Only CASH can be withdrawn
  → BONUS used for play only
```

**Handling:**
```java
// Wagering requirement tracking
WalletBonusExt bonusRecord = new WalletBonusExt();
bonusRecord.setWageredAmount(bonusRecord.getWageredAmount().add(debitAmount));

// Bonus conversion check
if (bonusRecord.getWageredAmount().compareTo(bonusRecord.getRequiredAmount()) >= 0) {
    // Bonus met: convert to CASH
    bonusWallet.setBalance(BigDecimal.ZERO);
    cashWallet.setBalance(cashWallet.getBalance().add(remainingBonus));
    bonusRecord.setStatus(BonusStatus.CONVERTED);
}

// Win always to CASH
cashWallet.setBalance(cashWallet.getBalance().add(winAmount));
```

### Scenario I: Jackpot

Large win (jackpot) with special handling and audit flag.

```
Request: Jackpot Win 10,000 USD
  → Amount exceeds normal win limit (e.g., 5,000)
  → Special audit flag set
  → Separate audit trail
  → Manual verification required
  → Credit held pending approval

Status: PENDING_AUDIT
After approval: Status → COMPLETED, funds released
```

**Handling:**
```java
if (form.getAmount().compareTo(jackpotThreshold) > 0) {
    WalletTransaction jackpotTx = new WalletTransaction();
    jackpotTx.setEventType("JACKPOT_WIN");
    jackpotTx.setMetadata(Map.of(
        "audit_required", true,
        "jackpot_amount", form.getAmount(),
        "flagged_at", Instant.now()
    ));

    // Hold credit pending verification
    WalletLock lock = new WalletLock();
    lock.setLockReason("JACKPOT_PENDING_APPROVAL");
    lock.setLockedAmount(form.getAmount());
    walletLockRepo.save(lock);

    // Notify compliance team
    complianceService.notifyJackpot(form.getPlayerId(), form.getAmount());

    return ResponseEntity.accepted().body(response);
}
```

## 2.11 監控指標 (Monitoring Metrics)

Key performance indicators and SLOs:

### Latency Metrics

```
wallet.api.debit.p99 < 100ms
wallet.api.credit.p99 < 100ms
wallet.api.rollback.p99 < 50ms
wallet.api.getBalance.p99 < 20ms

wallet.lock.acquisition.p99 < 10ms
wallet.db.query.p99 < 30ms
```

### Idempotency Metrics

```
idempotency.cache.hit_rate > 95%
  (Successful fast-path returns without lock)

idempotency.db_constraint_violations = near-zero
  (Indicates malformed duplicate detection)

idempotency.inflight_timeout_rate < 0.1%
  (Transactions marked in-flight but never completed)
```

### Reconciliation Metrics

```
reconciliation.discrepancy_rate < 0.01%
  (Transactions with amount mismatch between us & GP)

reconciliation.missing_tx_rate < 0.001%
  (Transactions in GP report but not in our DB)

reconciliation.daily_settlement_accuracy = 100%
  (Daily settlement should balance exactly)

reconciliation.manual_review_rate < 5%
  (Percentage of transactions flagged for manual review)
```

### Balance Integrity Metrics

```
wallet.balance_drift = 0
  (sum(cash.balance) + sum(locked) must equal total allocated)

wallet.negative_balance_incidents = 0
  (Hardstop: negative balance never allowed)

wallet.constraint_violation_rate = 0
  (locked_amount > balance, or other constraint violations)
```

### Error Metrics

```
wallet.api.error_rate < 0.1%
wallet.api.insufficient_funds_rate < 5% (normal)
wallet.api.lock_timeout_rate < 0.01%
wallet.api.optimistic_lock_retry_rate < 2%
```

### Database Metrics

```
postgres.lock.deadlock_rate = 0
  (No deadlocks due to careful lock ordering)

postgres.transaction.rollback_rate < 0.5%
postgres.constraint.violation_rate = 0
```

### Alerting Thresholds

```
CRITICAL:
  - wallet.balance_drift > 0 (any amount)
  - wallet.negative_balance_incidents > 0
  - reconciliation.discrepancy_rate > 1%
  - wallet.api.error_rate > 1%

WARNING:
  - wallet.api.p99 > 200ms
  - idempotency.cache.hit_rate < 90%
  - wallet.api.lock_timeout_rate > 0.1%
```

### Observability

```java
@Component
@RequiredArgsConstructor
public class WalletMetricsService {
    private final MeterRegistry meterRegistry;

    public void recordDebitSuccess(Long duration, BigDecimal amount) {
        Timer.builder("wallet.debit.success")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);

        Counter.builder("wallet.debit.amount")
            .register(meterRegistry)
            .increment(amount.doubleValue());
    }

    public void recordIdempotencyCacheHit() {
        Counter.builder("idempotency.cache.hit")
            .register(meterRegistry)
            .increment();
    }

    public void recordBalanceDrift(BigDecimal drift) {
        Gauge.builder("wallet.balance.drift",
            () -> drift.doubleValue())
            .register(meterRegistry);

        if (drift.compareTo(BigDecimal.ZERO) != 0) {
            logger.error("Balance drift detected: " + drift);
            alertService.sendCriticalAlert("Balance drift: " + drift);
        }
    }
}
```

## 2.12 對應業務文檔 (Business Documentation)

Link to requirements document:

> See: `/sessions/confident-determined-bohr/mnt/IGaming/requirements/02_Wallet_System_錢包系統.md`

This technical implementation realizes all business requirements specified in the requirements document, including:
- Multi-tenancy wallet isolation
- Triple-layer concurrency & idempotency protection
- Real-time balance management
- Bonus tracking with wagering requirements
- Game provider seamless wallet API
- Three-layer reconciliation (real-time, compensatory, daily settlement)
- Nine transaction scenarios handling
- Comprehensive monitoring and alerting
- **[v2.1]** Strict Token validation policy (§2.3.6) — all money ops reject expired tokens
- **[v2.1]** Multi-currency FX conversion (§2.3.7) — deposit-time conversion, rate freeze, slippage protection
- **[v2.1]** Negative balance tolerance & auto-compensation (§2.3.8)
- **[v2.1]** Resettlement flow for late GP settlements (§2.3.6)
- **[v2.2]** FX absorption thresholds configurable parameters (§2.3.7a, F-04) — param_keys for fiat/crypto absorption %, rate lock window; BS-10 cross-reference for multi-currency × FX scenarios

All endpoints are rate-limited, logged, and integrated with event-driven cache invalidation for consistency.

---

**Document Control:**
- Version: v2.2
- Last Updated: 2026-03-25
- Owner: Platform Engineering / Wallet Team
- Status: APPROVED
- Review Cycle: Quarterly
