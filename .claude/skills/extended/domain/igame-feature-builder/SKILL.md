---
name: igame-feature-builder
description: [P1 - Extended] Implement iGaming domain features following technical specs (VIP system, Wallet API, Bonus engine, Risk control, Reporting). Use when implementing financial operations, compliance features, player management, VIP tiers, wallet transactions, bonus distribution, KYC/AML validation, or iGaming business logic requiring precision and audit trails.
---

# iGaming Feature Builder

Implement iGaming-specific business features with financial precision, compliance requirements, and audit trails.

## Quick Start

```
User: "Implement VIP tier upgrade logic"
User: "Create wallet deposit/withdrawal API"
User: "Add bonus distribution engine"
```

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "VIP tier" - VIP tier system implementation
- "VIP system" - Complete VIP management features
- "wallet deposit" - Deposit transaction processing
- "wallet withdrawal" - Withdrawal transaction processing
- "bonus distribution" - Bonus engine and distribution logic

**Secondary Keywords** (Medium confidence):
- "financial operations" - Context: iGaming financial transactions
- "compliance features" - Context: iGaming compliance requirements
- "player management" - Context: player account and tier management
- "audit trail" - Context: financial operation logging
- "wagering requirements" - Context: bonus engine rules

**Phrase Patterns**:
- "Implement [iGaming feature]" - Example: "Implement VIP tier upgrade logic"
- "Create [wallet operation] API" - Example: "Create wallet deposit/withdrawal API"
- "Add [bonus feature]" - Example: "Add bonus distribution engine"

**Example User Requests**:
```
User: "Implement VIP tier upgrade logic based on wagering amount"
User: "Create wallet deposit/withdrawal API with SERIALIZABLE isolation"
User: "Add bonus distribution engine with wagering requirements"
User: "Implement player risk scoring for withdrawal approval"
User: "Create financial operations audit trail for compliance"
```

**Note**: This skill can also be manually invoked via `/igame-feature-builder` command.

## Core Capabilities

### 1. VIP System

**Tier Calculation:**
```java
@Service
public class VipService {
    
    public ResponseDTO<VipTierVO> calculateTierUpgrade(Long playerId) {
        // Get player's lifetime points
        BigDecimal lifetimePoints = playerDao.getLifetimePoints(playerId);
        
        // Determine tier based on points
        VipTier newTier = VipTierEnum.getTierByPoints(lifetimePoints);
        
        // Check if upgrade needed
        VipTier currentTier = playerDao.getCurrentTier(playerId);
        if (newTier.getLevel() > currentTier.getLevel()) {
            // Apply upgrade with benefits
            vipManager.upgradeTier(playerId, newTier);
            return ResponseDTO.ok(buildTierVO(newTier));
        }
        
        return ResponseDTO.ok();
    }
}
```

**Point Accumulation:**
```java
@Manager
public class VipManager {
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void accumulatePoints(Long playerId, BigDecimal amount) {
        // Calculate points (e.g., $1 = 10 points)
        BigDecimal points = amount.multiply(new BigDecimal("10"));
        
        // Update player points with row-level locking
        PlayerEntity player = playerDao.selectByIdForUpdate(playerId);
        BigDecimal newPoints = player.getVipPoints().add(points);
        player.setVipPoints(newPoints);
        playerDao.updateById(player);
        
        // Audit trail
        auditLog.log("VIP_POINT_ACCUMULATION", playerId, points);
    }
}
```

### 2. Wallet API

**Deposit Transaction:**
```java
@Service
public class WalletService {
    
    public ResponseDTO<String> deposit(DepositForm form) {
        // Validate amount precision (2 decimal places)
        if (form.getAmount().scale() > 2) {
            return ResponseDTO.userErrorParam("Invalid amount precision");
        }
        
        // Create transaction record
        TransactionEntity txn = new TransactionEntity();
        txn.setPlayerId(form.getPlayerId());
        txn.setAmount(form.getAmount());
        txn.setType(TransactionType.DEPOSIT);
        txn.setStatus(TransactionStatus.PENDING);
        
        // Process deposit with transaction isolation
        walletManager.processDeposit(txn);
        
        return ResponseDTO.ok(txn.getTransactionId());
    }
}

@Manager
public class WalletManager {
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processDeposit(TransactionEntity txn) {
        // Lock player wallet
        WalletEntity wallet = walletDao.selectByPlayerIdForUpdate(txn.getPlayerId());
        
        // Update balance with BigDecimal precision
        BigDecimal newBalance = wallet.getBalance().add(txn.getAmount());
        wallet.setBalance(newBalance);
        walletDao.updateById(wallet);
        
        // Update transaction status
        txn.setStatus(TransactionStatus.COMPLETED);
        transactionDao.insert(txn);
        
        // Audit trail (REQUIRED for compliance)
        auditLog.log("WALLET_DEPOSIT", txn);
    }
}
```

**Withdrawal with KYC:**
```java
public ResponseDTO<String> withdraw(WithdrawalForm form) {
    // KYC validation (REQUIRED)
    if (!kycService.isVerified(form.getPlayerId())) {
        return ResponseDTO.userErrorParam("KYC verification required");
    }
    
    // AML check
    if (amlService.isSuspicious(form.getPlayerId(), form.getAmount())) {
        return ResponseDTO.userErrorParam("Transaction flagged for review");
    }
    
    // Daily limit check
    BigDecimal dailyTotal = walletDao.getDailyWithdrawalTotal(form.getPlayerId());
    if (dailyTotal.add(form.getAmount()).compareTo(DAILY_LIMIT) > 0) {
        return ResponseDTO.userErrorParam("Daily withdrawal limit exceeded");
    }
    
    walletManager.processWithdrawal(form);
    return ResponseDTO.ok();
}
```

### 3. Bonus Engine

**Rule Evaluation:**
```java
@Service
public class BonusService {
    
    public ResponseDTO<String> distributeBonus(Long playerId, BonusType bonusType) {
        // Load bonus rules
        BonusRuleEntity rule = bonusRuleDao.getByType(bonusType);
        
        // Check eligibility
        if (!isBonusEligible(playerId, rule)) {
            return ResponseDTO.userErrorParam("Not eligible for this bonus");
        }
        
        // Calculate bonus amount
        BigDecimal bonusAmount = calculateBonusAmount(playerId, rule);
        
        // Distribute with transaction isolation
        bonusManager.distributeBonus(playerId, bonusAmount, bonusType);
        
        return ResponseDTO.ok("Bonus distributed: " + bonusAmount);
    }
    
    private BigDecimal calculateBonusAmount(Long playerId, BonusRuleEntity rule) {
        BigDecimal depositAmount = walletDao.getDepositTotal(playerId);
        BigDecimal percentage = rule.getBonusPercentage();
        BigDecimal maxBonus = rule.getMaxBonusAmount();
        
        BigDecimal calculated = depositAmount.multiply(percentage)
            .divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
        
        return calculated.min(maxBonus);
    }
}
```

### 4. Risk Control

**Transaction Limit Check:**
```java
@Service
public class RiskControlService {
    
    public boolean validateTransaction(Long playerId, BigDecimal amount, TransactionType type) {
        // Daily limit
        BigDecimal dailyLimit = getDailyLimit(playerId);
        BigDecimal dailyTotal = transactionDao.getDailyTotal(playerId, type);
        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            auditLog.log("RISK_DAILY_LIMIT_EXCEEDED", playerId, amount);
            return false;
        }
        
        // Single transaction limit
        if (amount.compareTo(SINGLE_TXN_LIMIT) > 0) {
            auditLog.log("RISK_SINGLE_TXN_LIMIT_EXCEEDED", playerId, amount);
            return false;
        }
        
        // Fraud detection
        if (fraudDetectionService.isSuspicious(playerId, amount)) {
            auditLog.log("RISK_FRAUD_DETECTED", playerId, amount);
            return false;
        }
        
        return true;
    }
}
```

### 5. Audit Trail (MANDATORY)

**Every Financial Operation:**
```java
@Service
public class AuditLogService {
    
    public void log(String operation, Object... params) {
        AuditLogEntity log = new AuditLogEntity();
        log.setOperation(operation);
        log.setParams(JsonUtil.toJson(params));
        log.setOperatorId(RequestContext.getUserId());
        log.setIpAddress(RequestContext.getIpAddress());
        log.setTimestamp(LocalDateTime.now());
        
        auditLogDao.insert(log);
    }
}
```

## Validation Checklist

**Financial Operations:**
- [ ] Use BigDecimal for money/points (NEVER float/double)
- [ ] Set precision to 2 decimal places
- [ ] Use RoundingMode.DOWN for player-favorable rounding
- [ ] @Transactional(isolation = SERIALIZABLE) for critical ops
- [ ] Row-level locking (selectForUpdate)

**Compliance:**
- [ ] KYC verification before withdrawal
- [ ] AML suspicious activity check
- [ ] Audit log for every financial operation
- [ ] Transaction limits enforced
- [ ] Fraud detection integration

**Data Integrity:**
- [ ] Transaction status workflow (PENDING → COMPLETED/FAILED)
- [ ] Balance consistency checks
- [ ] Rollback support for failed operations
- [ ] Idempotency for duplicate requests

---

## 相關規則

本技能生成的 iGaming 功能代碼必須符合以下 SmartAdmin 規範：

### 強制要求

- **[Architecture Rules - Complete](./../../../.agent/rules/foundation/10-architecture-rules.md)**
  - 錢包 API 嚴格遵循 Controller → Service → Manager → Dao
  - Service 層禁止使用 @Transactional（委託給 Manager 層）
  - Service 層使用 `io.vavr.control.Option`（禁止 `java.util.Optional`）
  - 構造器注入（@RequiredArgsConstructor + private final）

- **[Manager Layer Rules](./../../../.agent/rules/foundation/09-manager-layer.md)**
  - 錢包扣款/加款必須使用 Manager 層
  - @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Throwable.class)
  - Manager 層處理分佈式鎖（Redisson）和樂觀鎖（version 欄位）

- **[Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md)**
  - 實體命名：WalletEntity, VipTierEntity, BonusRuleEntity
  - Service 命名：WalletService, VipService, BonusService
  - Manager 命名：WalletManager（不是 WalletManagerImpl）
  - Form 命名：DepositForm, WithdrawalForm（不是 DepositRequest）

### 參考指引

- **[Exception Handling](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - 餘額不足使用 ResponseDTO.userErrorParam()
  - KYC 驗證失敗使用明確錯誤碼
  - 業務異常不使用 try-catch（Service 層返回 ResponseDTO）

- **[Dependency Injection](./../../../.agent/rules/foundation/07-dependency-injection.md)**
  - 所有 Service/Manager 使用構造器注入
  - 禁止 @Autowired 欄位注入

### 相關技能

- **[fraud-detection-pattern-generator](./../fraud-detection-pattern-generator/SKILL.md)** - 風控檢測整合（支付/提款風控）
- **[igame-pm-analyst](./../igame-pm-analyst/SKILL.md)** - iGaming 功能需求分析
- **[archunit-test-generator](./../../foundation/backend/archunit-test-generator/SKILL.md)** - 生成 ArchUnit 測試驗證架構規則

---

## References

See references/ for detailed patterns:
- wallet-api-patterns.md
- vip-system-implementation.md
- bonus-engine-guide.md
- risk-control-compliance.md

## Time Savings

**Manual Implementation:** Varies by feature (8-20 hours)
**Skill-Guided:** 2-6 hours with patterns
**Time Saved: 60-70% reduction**
