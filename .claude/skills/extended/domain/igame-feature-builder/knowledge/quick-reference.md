# iGaming Feature Builder - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: igame-feature-builder (P1 - Extended/Domain)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| VIP System | Implement tier system with auto-upgrade | ~20 min |
| Wallet Deposit | Create deposit API with SERIALIZABLE isolation | ~15 min |
| Wallet Withdrawal | Create withdrawal API with KYC/AML checks | ~18 min |
| Bonus Distribution | Implement bonus engine with wagering rules | ~20 min |
| Risk Control | Add transaction limits and fraud detection | ~12 min |
| Audit Trail | Setup financial operation logging | ~8 min |

### Rapid Development Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Select Feature | Choose iGaming feature type | ~2 min |
| 2. Generate Service | Create SmartAdmin Service layer | ~10 min |
| 3. Generate Manager | Create Manager with @Transactional | ~8 min |
| 4. Database Schema | Generate tables with proper indexes | ~6 min |
| 5. Add Compliance | Integrate KYC/AML/Audit checks | ~10 min |
| 6. Testing | Verify with financial precision tests | ~15 min |

**Total**: ~50 minutes per iGaming feature

---

## Feature Pattern Matrix (Decision Guide)

### Pattern 1: VIP System (5-Tier Auto-Upgrade)

**Trigger Keywords**: "VIP tier", "VIP upgrade", "loyalty points", "tier benefits"

**Use When**: Implement player retention system with automatic tier management

**5-Tier Configuration**:

| Tier | Monthly GGR | Total Deposits (30d) | Min Sessions | Grace Period | Benefits |
|------|-------------|----------------------|--------------|--------------|----------|
| **Bronze** | $0 - $499 | $0 - $999 | 0 | N/A | 5% cashback, 25% reload bonus |
| **Silver** | $500 - $1,999 | $1,000 - $4,999 | 10 | 30 days | 7% cashback, 35% reload bonus |
| **Gold** | $2,000 - $4,999 | $5,000 - $14,999 | 20 | 60 days | 10% cashback, 50% reload bonus |
| **Platinum** | $5,000 - $14,999 | $15,000 - $49,999 | 30 | 90 days | 15% cashback, 75% reload bonus |
| **Diamond** | $15,000+ | $50,000+ | 40 | 180 days | 20% cashback, 100% reload bonus |

**Upgrade Logic**: Must meet ALL THREE criteria (GGR + Deposits + Sessions)
**Downgrade Logic**: Fails ANY ONE criterion AND grace period expires

**Implementation**:

```java
@Service
@RequiredArgsConstructor
public class VipService {

    private final PlayerDao playerDao;
    private final VipTierConfigDao tierConfigDao;
    private final PlayerStatsDao statsDao;
    private final VipManager vipManager;

    /**
     * Calculate tier upgrade based on player metrics
     * Time: ~12 min to implement
     */
    public ResponseDTO<VipTierVO> calculateTierUpgrade(Long playerId) {
        // Get player's 30-day metrics
        PlayerStatsEntity stats = statsDao.getMonthlyStats(playerId);

        // Get current tier
        VipTierEntity currentTier = playerDao.getCurrentVipTier(playerId);

        // Find eligible tier based on ALL THREE criteria
        VipTierConfigEntity newTierConfig = tierConfigDao.findEligibleTier(
            stats.getMonthlyGgr(),
            stats.getTotalDeposits30d(),
            stats.getSessionCount()
        );

        // Check if upgrade needed
        if (newTierConfig.getLevel() > currentTier.getLevel()) {
            // Delegate to Manager for transaction
            VipTierVO upgraded = vipManager.upgradeTier(
                playerId,
                newTierConfig.getTier(),
                "Met " + newTierConfig.getTier() + " criteria"
            );

            log.info("[VIP] Tier upgraded: player={}, {} → {}",
                playerId, currentTier.getTier(), newTierConfig.getTier());

            return ResponseDTO.ok(upgraded);
        }

        return ResponseDTO.ok(buildTierVO(currentTier));
    }

    /**
     * Check downgrade with grace period protection
     * Time: ~10 min to implement
     */
    public ResponseDTO<Void> checkTierDowngrade(Long playerId) {
        VipTierEntity currentTier = playerDao.getCurrentVipTier(playerId);
        PlayerStatsEntity stats = statsDao.getMonthlyStats(playerId);

        VipTierConfigEntity tierConfig = tierConfigDao.getByTier(currentTier.getTier());

        // Check if player fails ANY criterion
        boolean failsGgr = stats.getMonthlyGgr().compareTo(tierConfig.getMinMonthlyGgr()) < 0;
        boolean failsDeposits = stats.getTotalDeposits30d().compareTo(tierConfig.getMinDeposits30d()) < 0;
        boolean failsSessions = stats.getSessionCount() < tierConfig.getMinSessions();

        if (failsGgr || failsDeposits || failsSessions) {
            if (!currentTier.getInGracePeriod()) {
                // Enter grace period
                vipManager.enterGracePeriod(playerId, tierConfig.getGracePeriodDays());
                log.warn("[VIP] Entered grace period: player={}, tier={}",
                    playerId, currentTier.getTier());
            } else if (isGracePeriodExpired(currentTier)) {
                // Downgrade to lower tier
                VipTierConfigEntity lowerTier = tierConfigDao.getLowerTier(currentTier.getTier());
                vipManager.downgradeTier(
                    playerId,
                    lowerTier.getTier(),
                    "Failed to maintain " + currentTier.getTier() + " criteria"
                );
                log.warn("[VIP] Tier downgraded: player={}, {} → {}",
                    playerId, currentTier.getTier(), lowerTier.getTier());
            }
        } else {
            // Player recovered, exit grace period
            if (currentTier.getInGracePeriod()) {
                vipManager.exitGracePeriod(playerId);
            }
        }

        return ResponseDTO.ok();
    }
}

@Service
@RequiredArgsConstructor
public class VipManager {

    private final VipTierDao vipTierDao;
    private final AuditLogService auditLog;

    /**
     * Upgrade tier with benefits distribution
     * Time: ~8 min to implement
     */
    @Transactional(rollbackFor = Throwable.class)
    public VipTierVO upgradeTier(Long playerId, String newTier, String reason) {
        // Lock player row
        PlayerEntity player = playerDao.selectByIdForUpdate(playerId);

        // Create new tier record
        VipTierEntity tierEntity = new VipTierEntity();
        tierEntity.setPlayerId(playerId);
        tierEntity.setTier(newTier);
        tierEntity.setEffectiveDate(LocalDateTime.now());
        tierEntity.setExpiryDate(null);  // No grace period on upgrade
        tierEntity.setInGracePeriod(false);
        tierEntity.setUpgradeReason(reason);

        vipTierDao.insert(tierEntity);

        // Distribute welcome bonus for new tier
        distributeTierWelcomeBonus(playerId, newTier);

        // Audit trail
        auditLog.log("VIP_TIER_UPGRADE", playerId, newTier, reason);

        return buildTierVO(tierEntity);
    }

    /**
     * Enter grace period (protection mechanism)
     */
    @Transactional(rollbackFor = Throwable.class)
    public void enterGracePeriod(Long playerId, Integer gracePeriodDays) {
        VipTierEntity tier = vipTierDao.getCurrentTier(playerId);

        tier.setInGracePeriod(true);
        tier.setExpiryDate(LocalDateTime.now().plusDays(gracePeriodDays));

        vipTierDao.updateById(tier);

        auditLog.log("VIP_GRACE_PERIOD_START", playerId, gracePeriodDays);
    }
}
```

**Database Schema**:

```sql
CREATE TABLE t_vip_tier (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,  -- BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
    effective_date TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP,  -- Null if no grace period
    grace_period_days INTEGER,
    in_grace_period BOOLEAN DEFAULT FALSE,

    -- Metrics at tier assignment
    monthly_ggr NUMERIC(15, 2),
    total_deposits NUMERIC(15, 2),
    session_count INTEGER,

    upgrade_reason TEXT,
    downgrade_reason TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    operator_id BIGINT,  -- Null for automatic

    version INTEGER DEFAULT 0,  -- Optimistic locking

    CONSTRAINT fk_vip_player FOREIGN KEY (player_id) REFERENCES t_player(id)
);

CREATE INDEX idx_vip_tier_player ON t_vip_tier(player_id, effective_date DESC);
CREATE INDEX idx_vip_tier_expiry ON t_vip_tier(expiry_date) WHERE in_grace_period = TRUE;

CREATE TABLE t_vip_tier_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tier VARCHAR(20) NOT NULL,

    -- Thresholds (ALL must be met for upgrade)
    min_monthly_ggr NUMERIC(15, 2) NOT NULL,
    max_monthly_ggr NUMERIC(15, 2),
    min_deposits_30d NUMERIC(15, 2) NOT NULL,
    max_deposits_30d NUMERIC(15, 2),
    min_sessions INTEGER NOT NULL,

    -- Retention
    grace_period_days INTEGER NOT NULL,

    -- Benefits
    cashback_rate NUMERIC(5, 4),  -- 0.0500 = 5%
    reload_bonus_rate NUMERIC(5, 4),
    daily_withdrawal_limit NUMERIC(15, 2),
    monthly_withdrawal_limit NUMERIC(15, 2),

    color_code VARCHAR(7),
    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX idx_vip_config_tenant_tier ON t_vip_tier_config(tenant_id, tier);
```

**Time to Implement**: 20-25 minutes

---

### Pattern 2: Wallet API (Deposit/Withdrawal with SERIALIZABLE Isolation)

**Trigger Keywords**: "wallet deposit", "wallet withdrawal", "balance update", "financial transaction"

**Use When**: Implement player wallet with financial precision and compliance

**Critical Requirements**:
- ✅ Use `BigDecimal` for all amounts (NEVER `float` or `double`)
- ✅ Precision: 2 decimal places
- ✅ Rounding: `RoundingMode.DOWN` (player-favorable)
- ✅ Transaction isolation: `SERIALIZABLE` for critical operations
- ✅ Row-level locking: `selectForUpdate`

**Deposit Implementation**:

```java
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletManager walletManager;
    private final PaymentGatewayService paymentGateway;

    /**
     * Process deposit transaction
     * Time: ~10 min to implement
     */
    public ResponseDTO<String> deposit(DepositForm form) {
        // Validate amount precision (2 decimal places)
        if (form.getAmount().scale() > 2) {
            return ResponseDTO.userErrorParam("Amount must have max 2 decimal places");
        }

        // Minimum deposit check
        if (form.getAmount().compareTo(new BigDecimal("10.00")) < 0) {
            return ResponseDTO.userErrorParam("Minimum deposit is $10.00");
        }

        // Create transaction record
        TransactionEntity txn = TransactionEntity.builder()
            .playerId(form.getPlayerId())
            .amount(form.getAmount())
            .type(TransactionType.DEPOSIT)
            .status(TransactionStatus.PENDING)
            .paymentMethod(form.getPaymentMethod())
            .transactionId(generateTransactionId())
            .build();

        // Process with payment gateway
        PaymentResult result = paymentGateway.processDeposit(form);

        if (result.isSuccess()) {
            // Update wallet balance in transaction
            walletManager.processDeposit(txn);
            return ResponseDTO.ok(txn.getTransactionId());
        } else {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setErrorMessage(result.getErrorMessage());
            transactionDao.insert(txn);
            return ResponseDTO.userErrorParam(result.getErrorMessage());
        }
    }
}

@Service
@RequiredArgsConstructor
public class WalletManager {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;
    private final AuditLogService auditLog;

    /**
     * Process deposit with SERIALIZABLE isolation
     * Time: ~12 min to implement
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
    public void processDeposit(TransactionEntity txn) {
        // Lock player wallet row
        WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());

        // Update balance with BigDecimal precision
        BigDecimal newBalance = wallet.getBalance().add(txn.getAmount());
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletDao.updateById(wallet);

        // Update transaction status
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        transactionDao.insert(txn);

        // Audit trail (REQUIRED for compliance)
        auditLog.log("WALLET_DEPOSIT", txn.getPlayerId(), txn.getAmount(), txn.getTransactionId());

        log.info("[Wallet] Deposit completed: player={}, amount={}, newBalance={}",
            txn.getPlayerId(), txn.getAmount(), newBalance);
    }
}
```

**Withdrawal Implementation with KYC/AML**:

```java
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletManager walletManager;
    private final KycService kycService;
    private final AmlService amlService;

    /**
     * Process withdrawal with KYC/AML checks
     * Time: ~15 min to implement
     */
    public ResponseDTO<String> withdraw(WithdrawalForm form) {
        // 1. KYC verification (REQUIRED)
        if (!kycService.isVerified(form.getPlayerId())) {
            return ResponseDTO.userErrorParam("KYC verification required for withdrawal");
        }

        // 2. AML suspicious activity check
        if (amlService.isSuspicious(form.getPlayerId(), form.getAmount())) {
            auditLog.log("AML_WITHDRAWAL_FLAGGED", form.getPlayerId(), form.getAmount());
            return ResponseDTO.userErrorParam("Transaction flagged for compliance review");
        }

        // 3. Daily withdrawal limit
        BigDecimal dailyTotal = walletDao.getDailyWithdrawalTotal(form.getPlayerId());
        BigDecimal dailyLimit = getDailyWithdrawalLimit(form.getPlayerId());

        if (dailyTotal.add(form.getAmount()).compareTo(dailyLimit) > 0) {
            return ResponseDTO.userErrorParam(
                String.format("Daily withdrawal limit: $%s (used: $%s)",
                    dailyLimit, dailyTotal)
            );
        }

        // 4. Sufficient balance check
        WalletEntity wallet = walletDao.selectByPlayerId(form.getPlayerId());
        if (wallet.getBalance().compareTo(form.getAmount()) < 0) {
            return ResponseDTO.userErrorParam("Insufficient balance");
        }

        // 5. Process withdrawal in transaction
        TransactionEntity txn = TransactionEntity.builder()
            .playerId(form.getPlayerId())
            .amount(form.getAmount())
            .type(TransactionType.WITHDRAWAL)
            .status(TransactionStatus.PENDING)
            .paymentMethod(form.getPaymentMethod())
            .transactionId(generateTransactionId())
            .build();

        walletManager.processWithdrawal(txn);

        return ResponseDTO.ok(txn.getTransactionId());
    }
}

@Service
@RequiredArgsConstructor
public class WalletManager {

    /**
     * Process withdrawal with SERIALIZABLE isolation
     * Time: ~12 min to implement
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
    public void processWithdrawal(TransactionEntity txn) {
        // Lock player wallet row
        WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());

        // Validate sufficient balance (double-check in transaction)
        if (wallet.getBalance().compareTo(txn.getAmount()) < 0) {
            throw new BusinessException("Insufficient balance");
        }

        // Deduct balance
        BigDecimal newBalance = wallet.getBalance().subtract(txn.getAmount());
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletDao.updateById(wallet);

        // Update transaction status
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        transactionDao.insert(txn);

        // Audit trail (REQUIRED)
        auditLog.log("WALLET_WITHDRAWAL", txn.getPlayerId(), txn.getAmount(), txn.getTransactionId());

        log.info("[Wallet] Withdrawal completed: player={}, amount={}, newBalance={}",
            txn.getPlayerId(), txn.getAmount(), newBalance);
    }
}
```

**Database Schema**:

```sql
CREATE TABLE t_wallet (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL UNIQUE,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    frozen_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    version INTEGER DEFAULT 0,  -- Optimistic locking

    CONSTRAINT fk_wallet_player FOREIGN KEY (player_id) REFERENCES t_player(id),
    CONSTRAINT chk_balance_positive CHECK (balance >= 0),
    CONSTRAINT chk_frozen_positive CHECK (frozen_balance >= 0)
);

CREATE INDEX idx_wallet_player ON t_wallet(player_id);

CREATE TABLE t_transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,  -- DEPOSIT, WITHDRAWAL, BONUS, ADJUSTMENT
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, COMPLETED, FAILED, CANCELLED
    payment_method VARCHAR(50),

    error_message TEXT,
    completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_txn_player FOREIGN KEY (player_id) REFERENCES t_player(id)
);

CREATE INDEX idx_txn_player_created ON t_transaction(player_id, created_at DESC);
CREATE INDEX idx_txn_status ON t_transaction(status);
CREATE INDEX idx_txn_type_created ON t_transaction(type, created_at DESC);
```

**Time to Implement**: 25-30 minutes (deposit + withdrawal)

---

### Pattern 3: Bonus Engine (Rule-Based Distribution)

**Trigger Keywords**: "bonus distribution", "wagering requirement", "bonus rule", "promotion"

**Use When**: Implement automated bonus system with eligibility rules and wagering requirements

**Implementation**:

```java
@Service
@RequiredArgsConstructor
public class BonusService {

    private final BonusRuleDao bonusRuleDao;
    private final BonusManager bonusManager;
    private final PlayerDao playerDao;

    /**
     * Distribute bonus to player
     * Time: ~15 min to implement
     */
    public ResponseDTO<String> distributeBonus(Long playerId, BonusType bonusType) {
        // Load bonus rules
        BonusRuleEntity rule = bonusRuleDao.getByType(bonusType);

        if (rule == null || !rule.getActive()) {
            return ResponseDTO.userErrorParam("Bonus not available");
        }

        // Check eligibility
        if (!isBonusEligible(playerId, rule)) {
            return ResponseDTO.userErrorParam("Not eligible for this bonus");
        }

        // Calculate bonus amount
        BigDecimal bonusAmount = calculateBonusAmount(playerId, rule);

        // Distribute with transaction
        bonusManager.distributeBonus(playerId, bonusAmount, bonusType, rule);

        return ResponseDTO.ok("Bonus distributed: $" + bonusAmount);
    }

    /**
     * Check bonus eligibility
     * Time: ~8 min to implement
     */
    private boolean isBonusEligible(Long playerId, BonusRuleEntity rule) {
        PlayerEntity player = playerDao.selectById(playerId);

        // VIP tier requirement
        if (rule.getMinVipTier() != null) {
            VipTier playerTier = VipTier.valueOf(player.getVipTier());
            VipTier minTier = VipTier.valueOf(rule.getMinVipTier());
            if (playerTier.getLevel() < minTier.getLevel()) {
                return false;
            }
        }

        // Deposit requirement (last 7 days)
        if (rule.getMinDepositAmount() != null) {
            BigDecimal deposits7d = walletDao.getDepositsLast7Days(playerId);
            if (deposits7d.compareTo(rule.getMinDepositAmount()) < 0) {
                return false;
            }
        }

        // Frequency limit (e.g., once per week)
        if (rule.getFrequencyDays() != null) {
            LocalDateTime lastClaim = bonusClaimDao.getLastClaimDate(playerId, rule.getBonusType());
            if (lastClaim != null) {
                long daysSince = ChronoUnit.DAYS.between(lastClaim, LocalDateTime.now());
                if (daysSince < rule.getFrequencyDays()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculate bonus amount with percentage and cap
     * Time: ~6 min to implement
     */
    private BigDecimal calculateBonusAmount(Long playerId, BonusRuleEntity rule) {
        BigDecimal depositAmount = walletDao.getDepositTotal(playerId);
        BigDecimal percentage = rule.getBonusPercentage();
        BigDecimal maxBonus = rule.getMaxBonusAmount();

        // Calculate: deposit * (percentage / 100)
        BigDecimal calculated = depositAmount
            .multiply(percentage)
            .divide(new BigDecimal("100"), 2, RoundingMode.DOWN);

        // Cap at max bonus
        return calculated.min(maxBonus);
    }
}

@Service
@RequiredArgsConstructor
public class BonusManager {

    private final WalletDao walletDao;
    private final BonusClaimDao bonusClaimDao;
    private final AuditLogService auditLog;

    /**
     * Distribute bonus with wagering requirement
     * Time: ~12 min to implement
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
    public void distributeBonus(Long playerId, BigDecimal bonusAmount, BonusType bonusType, BonusRuleEntity rule) {
        // Lock wallet
        WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(playerId);

        // Add bonus to balance
        BigDecimal newBalance = wallet.getBalance().add(bonusAmount);
        wallet.setBalance(newBalance);
        walletDao.updateById(wallet);

        // Create bonus claim record with wagering requirement
        BonusClaimEntity claim = BonusClaimEntity.builder()
            .playerId(playerId)
            .bonusType(bonusType)
            .bonusAmount(bonusAmount)
            .wageringRequirement(bonusAmount.multiply(rule.getWageringMultiplier()))
            .wageringProgress(BigDecimal.ZERO)
            .status(BonusStatus.ACTIVE)
            .expiryDate(LocalDateTime.now().plusDays(rule.getExpiryDays()))
            .build();

        bonusClaimDao.insert(claim);

        // Audit trail
        auditLog.log("BONUS_DISTRIBUTED", playerId, bonusType, bonusAmount);

        log.info("[Bonus] Distributed: player={}, type={}, amount={}, wagering={}x",
            playerId, bonusType, bonusAmount, rule.getWageringMultiplier());
    }
}
```

**Database Schema**:

```sql
CREATE TABLE t_bonus_rule (
    id BIGSERIAL PRIMARY KEY,
    bonus_type VARCHAR(50) NOT NULL UNIQUE,
    bonus_percentage NUMERIC(5, 2) NOT NULL,
    max_bonus_amount NUMERIC(15, 2) NOT NULL,

    -- Eligibility
    min_vip_tier VARCHAR(20),
    min_deposit_amount NUMERIC(15, 2),
    frequency_days INTEGER,  -- e.g., 7 for once per week

    -- Wagering
    wagering_multiplier NUMERIC(5, 2) NOT NULL,  -- e.g., 30 for 30x
    expiry_days INTEGER NOT NULL,

    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE t_bonus_claim (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    bonus_type VARCHAR(50) NOT NULL,
    bonus_amount NUMERIC(15, 2) NOT NULL,

    wagering_requirement NUMERIC(15, 2) NOT NULL,
    wagering_progress NUMERIC(15, 2) NOT NULL DEFAULT 0.00,

    status VARCHAR(20) NOT NULL,  -- ACTIVE, COMPLETED, EXPIRED, FORFEITED
    expiry_date TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_bonus_player FOREIGN KEY (player_id) REFERENCES t_player(id)
);

CREATE INDEX idx_bonus_claim_player_status ON t_bonus_claim(player_id, status);
CREATE INDEX idx_bonus_claim_expiry ON t_bonus_claim(expiry_date) WHERE status = 'ACTIVE';
```

**Time to Implement**: 20-25 minutes

---

### Pattern 4: Risk Control (Transaction Limits + Fraud Detection)

**Trigger Keywords**: "transaction limit", "fraud detection", "risk control", "suspicious activity"

**Use When**: Enforce compliance limits and detect fraudulent transactions

**Implementation**:

```java
@Service
@RequiredArgsConstructor
public class RiskControlService {

    private final TransactionDao transactionDao;
    private final FraudDetectionService fraudDetectionService;
    private final AuditLogService auditLog;

    /**
     * Validate transaction against risk rules
     * Time: ~10 min to implement
     */
    public boolean validateTransaction(Long playerId, BigDecimal amount, TransactionType type) {
        // 1. Daily limit check
        BigDecimal dailyLimit = getDailyLimit(playerId, type);
        BigDecimal dailyTotal = transactionDao.getDailyTotal(playerId, type);

        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            auditLog.log("RISK_DAILY_LIMIT_EXCEEDED", playerId, type, amount);
            return false;
        }

        // 2. Single transaction limit
        BigDecimal singleTxnLimit = getSingleTransactionLimit(playerId, type);
        if (amount.compareTo(singleTxnLimit) > 0) {
            auditLog.log("RISK_SINGLE_TXN_LIMIT_EXCEEDED", playerId, type, amount);
            return false;
        }

        // 3. Fraud detection integration
        if (fraudDetectionService.isSuspicious(playerId, amount, type)) {
            auditLog.log("RISK_FRAUD_DETECTED", playerId, type, amount);
            return false;
        }

        // 4. Velocity check (transactions per hour)
        int txnCount1h = transactionDao.countTransactionsLastHour(playerId, type);
        if (txnCount1h > MAX_TXN_PER_HOUR) {
            auditLog.log("RISK_VELOCITY_EXCEEDED", playerId, type, txnCount1h);
            return false;
        }

        return true;
    }

    /**
     * Get daily limit based on VIP tier
     */
    private BigDecimal getDailyLimit(Long playerId, TransactionType type) {
        PlayerEntity player = playerDao.selectById(playerId);
        VipTierConfigEntity tierConfig = tierConfigDao.getByTier(player.getVipTier());

        if (type == TransactionType.WITHDRAWAL) {
            return tierConfig.getDailyWithdrawalLimit();
        } else {
            return new BigDecimal("50000.00");  // Default deposit limit
        }
    }
}
```

**Time to Implement**: 10-15 minutes

---

### Pattern 5: Audit Trail (Compliance Logging)

**Trigger Keywords**: "audit log", "compliance trail", "financial operation log"

**Use When**: Log all financial operations for regulatory compliance

**Implementation**:

```java
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogDao auditLogDao;

    /**
     * Log financial operation for compliance
     * Time: ~6 min to implement
     */
    public void log(String operation, Object... params) {
        AuditLogEntity log = AuditLogEntity.builder()
            .operation(operation)
            .params(JsonUtil.toJson(params))
            .operatorId(RequestContext.getUserId())
            .ipAddress(RequestContext.getIpAddress())
            .userAgent(RequestContext.getUserAgent())
            .timestamp(LocalDateTime.now())
            .build();

        auditLogDao.insert(log);
    }
}
```

**Database Schema**:

```sql
CREATE TABLE t_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(100) NOT NULL,
    params JSONB NOT NULL,
    operator_id BIGINT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_operation ON t_audit_log(operation, timestamp DESC);
CREATE INDEX idx_audit_operator ON t_audit_log(operator_id, timestamp DESC);
```

**Time to Implement**: 8-10 minutes

---

## Common Errors and Quick Fixes

### Error 1: Float/Double Used for Money

**Symptom**: Precision loss in financial calculations (e.g., $10.10 becomes $10.099999)

**Cause**: Using `float` or `double` instead of `BigDecimal`

**Quick Fix**:
```java
// ❌ WRONG
double balance = 10.50;
double newBalance = balance + 0.10;  // May result in 10.599999

// ✅ CORRECT
BigDecimal balance = new BigDecimal("10.50");
BigDecimal newBalance = balance.add(new BigDecimal("0.10"));  // Exact: 10.60
```

**Time to Fix**: 5-10 minutes (refactor all money fields)

---

### Error 2: Missing Transaction Isolation

**Symptom**: Race condition causing duplicate deposits or negative balances

**Cause**: Missing `@Transactional(isolation = SERIALIZABLE)` or `selectForUpdate`

**Quick Fix**:
```java
// ❌ WRONG - Race condition possible
@Transactional
public void deposit(Long playerId, BigDecimal amount) {
    WalletEntity wallet = walletDao.selectByPlayerId(playerId);
    wallet.setBalance(wallet.getBalance().add(amount));
    walletDao.updateById(wallet);
}

// ✅ CORRECT - SERIALIZABLE + row-level lock
@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
public void deposit(Long playerId, BigDecimal amount) {
    WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(playerId);  // Lock row
    wallet.setBalance(wallet.getBalance().add(amount));
    walletDao.updateById(wallet);
}
```

**Time to Fix**: 2-5 minutes per method

---

### Error 3: Missing KYC Check on Withdrawal

**Symptom**: Unverified players can withdraw funds

**Cause**: No KYC validation in withdrawal flow

**Quick Fix**:
```java
// ❌ WRONG - No KYC check
public ResponseDTO<String> withdraw(WithdrawalForm form) {
    walletManager.processWithdrawal(form);
    return ResponseDTO.ok();
}

// ✅ CORRECT - KYC required
public ResponseDTO<String> withdraw(WithdrawalForm form) {
    if (!kycService.isVerified(form.getPlayerId())) {
        return ResponseDTO.userErrorParam("KYC verification required");
    }

    walletManager.processWithdrawal(form);
    return ResponseDTO.ok();
}
```

**Time to Fix**: 5 minutes

---

### Error 4: Missing Audit Trail

**Symptom**: No compliance logs for financial operations

**Cause**: Forgot to call `auditLog.log()` after transaction

**Quick Fix**:
```java
// ❌ WRONG - No audit log
@Transactional
public void deposit(TransactionEntity txn) {
    WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());
    wallet.setBalance(wallet.getBalance().add(txn.getAmount()));
    walletDao.updateById(wallet);
}

// ✅ CORRECT - Audit trail added
@Transactional
public void deposit(TransactionEntity txn) {
    WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());
    wallet.setBalance(wallet.getBalance().add(txn.getAmount()));
    walletDao.updateById(wallet);

    auditLog.log("WALLET_DEPOSIT", txn.getPlayerId(), txn.getAmount(), txn.getTransactionId());
}
```

**Time to Fix**: 2-3 minutes per operation

---

### Error 5: VIP Tier Downgrade Without Grace Period

**Symptom**: Players downgraded immediately when temporarily inactive

**Cause**: Missing grace period logic

**Quick Fix**:
```java
// ❌ WRONG - Immediate downgrade
if (stats.getMonthlyGgr().compareTo(tierConfig.getMinMonthlyGgr()) < 0) {
    downgradeTier(playerId);
}

// ✅ CORRECT - Grace period protection
if (stats.getMonthlyGgr().compareTo(tierConfig.getMinMonthlyGgr()) < 0) {
    if (!currentTier.getInGracePeriod()) {
        enterGracePeriod(playerId, tierConfig.getGracePeriodDays());
    } else if (isGracePeriodExpired(currentTier)) {
        downgradeTier(playerId);
    }
}
```

**Time to Fix**: 10-15 minutes

---

## Time Estimates (Production Data)

| Feature | Implementation | Testing | Total | Complexity |
|---------|---------------|---------|-------|------------|
| VIP System (5-Tier) | 20-25 min | 15 min | 35-40 min | Medium |
| Wallet Deposit | 10-12 min | 8 min | 18-20 min | Low |
| Wallet Withdrawal (KYC) | 15-18 min | 10 min | 25-28 min | Medium |
| Bonus Engine | 20-25 min | 12 min | 32-37 min | Medium |
| Risk Control | 10-15 min | 8 min | 18-23 min | Low |
| Audit Trail | 8-10 min | 5 min | 13-15 min | Low |

**Full iGaming Feature Set**: 2.5-3 hours (all 6 patterns)

---

## Validation Checklist

Before deploying iGaming features:

**Financial Precision:**
- [ ] All money fields use `BigDecimal` (NEVER `float`/`double`)
- [ ] Precision set to 2 decimal places
- [ ] Rounding mode: `RoundingMode.DOWN` (player-favorable)
- [ ] `@Transactional(isolation = SERIALIZABLE)` for critical ops
- [ ] Row-level locking with `selectForUpdate`

**Compliance:**
- [ ] KYC verification required before withdrawal
- [ ] AML suspicious activity check integrated
- [ ] Audit log for every financial operation
- [ ] Transaction limits enforced (daily + single txn)
- [ ] Fraud detection system connected

**VIP System:**
- [ ] 5-tier configuration complete (Bronze → Diamond)
- [ ] Auto-upgrade logic (ALL THREE criteria)
- [ ] Auto-downgrade logic (ANY ONE criterion fails)
- [ ] Grace period protection (prevent temporary downgrade)
- [ ] Tier benefits distributed on upgrade

**Wallet:**
- [ ] Deposit flow with payment gateway integration
- [ ] Withdrawal flow with KYC/AML checks
- [ ] Balance consistency checks
- [ ] Transaction status workflow (PENDING → COMPLETED/FAILED)
- [ ] Idempotency for duplicate requests

**Bonus Engine:**
- [ ] Rule-based eligibility checks
- [ ] Wagering requirement calculation
- [ ] Bonus expiry enforcement
- [ ] VIP tier benefits integration

---

**See Also**:
- [VIP System Implementation](../references/vip-system-implementation.md) - Detailed VIP tier guide
- [Fraud Detection Pattern Generator](../../fraud-detection-pattern-generator/) - Risk control integration
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - ResponseDTO, Transaction patterns
- [Manager Layer Rules](./../../../.agent/rules/foundation/09-manager-layer.md) - Transaction management
