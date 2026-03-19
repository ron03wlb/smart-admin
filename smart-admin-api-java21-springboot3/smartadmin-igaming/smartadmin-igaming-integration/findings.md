# Investigation Findings: Integration Test Failures

## Date: 2026-03-19

## Problem 1: Payment Order wallet_id Null Constraint Violation ✅

### Error Message
```
org.springframework.dao.DataIntegrityViolationException:
ERROR: null value in column "wallet_id" of relation "t_payment_order" violates not-null constraint
```

### Root Cause (CONFIRMED)
**File**: `PlayerRegistrationTestFixture.java`
**Method**: `createPaymentOrder()` (lines 138-150)
**Issue**: Method does NOT set `walletId` field when creating PaymentOrderEntity

**Code Analysis**:
```java
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, String orderNo, BigDecimal amount, Integer status) {
  PaymentOrderEntity entity = new PaymentOrderEntity();
  entity.setPlayerId(playerId);
  entity.setOrderNo(orderNo);
  entity.setOrderType(1); // DEPOSIT
  entity.setStatus(status);
  entity.setAmount(amount);
  entity.setCurrencyCode("USD");
  entity.setPspCode("MOCK_PSP");
  entity.setPspTransactionId("PSP-TX-" + System.currentTimeMillis());
  // ❌ MISSING: entity.setWalletId(???)
  return entity;
}
```

**Database Constraint**:
- Table: `t_payment_order`
- Column: `wallet_id`
- Constraint: `NOT NULL`
- Source: PaymentOrderEntity.java line 29

**Impact**:
- Affects 3 test methods:
  1. `shouldAwardFirstDepositBonusOnFirstDeposit` (line 215-218)
  2. `shouldHandleFirstDepositWithoutActivePromotion`
  3. `shouldNotAwardBonusOnSecondDeposit` (line 277-279)

### Solution Strategy

**Option A: Modify createPaymentOrder() to accept walletId parameter**
```java
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, Long walletId, String orderNo, BigDecimal amount, Integer status) {
  // ... existing code ...
  entity.setWalletId(walletId);
  return entity;
}
```
- ✅ Pros: Explicit, type-safe
- ❌ Cons: Breaks existing test code (need to update 6+ call sites)

**Option B: Query wallet inside createPaymentOrder()** ⭐ RECOMMENDED
```java
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, String orderNo, BigDecimal amount, Integer status, WalletDao walletDao) {
  // Query CASH wallet
  WalletEntity cashWallet = walletDao.selectOne(
      Wrappers.<WalletEntity>lambdaQuery()
          .eq(WalletEntity::getPlayerId, playerId)
          .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
          .eq(WalletEntity::getDeleted, false));

  // ... create entity ...
  entity.setWalletId(cashWallet.getWalletId());
  return entity;
}
```
- ✅ Pros: Automatic wallet lookup, test code remains simple
- ❌ Cons: Requires WalletDao injection into test

**Option C: Create overloaded method** ⭐ BEST COMPROMISE
```java
// Original method - for backward compatibility
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, String orderNo, BigDecimal amount, Integer status) {
  return createPaymentOrder(playerId, null, orderNo, amount, status);
}

// New overloaded method with walletId
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, Long walletId, String orderNo, BigDecimal amount, Integer status) {
  PaymentOrderEntity entity = new PaymentOrderEntity();
  entity.setPlayerId(playerId);
  entity.setWalletId(walletId); // ✅ Set wallet ID
  entity.setOrderNo(orderNo);
  entity.setOrderType(1);
  entity.setStatus(status);
  entity.setAmount(amount);
  entity.setCurrencyCode("USD");
  entity.setPspCode("MOCK_PSP");
  entity.setPspTransactionId("PSP-TX-" + System.currentTimeMillis());
  return entity;
}
```
- ✅ Pros: Backward compatible, flexible
- ⚠️ Cons: Still need to update test call sites to provide walletId

### Implementation Decision
**SELECTED**: Option C (Overloaded method)

**Rationale**:
1. Tests already have access to registered player data (includes wallet info)
2. Explicit wallet ID parameter makes test intent clear
3. No new DAO dependencies in TestFixture

**Test Update Pattern**:
```java
// Before (broken):
PaymentOrderEntity order = PlayerRegistrationTestFixture.createPaymentOrder(
    playerId, orderNo, new BigDecimal("100.00"), PaymentOrderStatusEnum.PENDING.getValue());

// After (fixed):
Long cashWalletId = regResult.getData().getWallets().stream()
    .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
    .findFirst()
    .map(WalletVO::getWalletId)
    .orElseThrow();

PaymentOrderEntity order = PlayerRegistrationTestFixture.createPaymentOrder(
    playerId, cashWalletId, orderNo, new BigDecimal("100.00"), PaymentOrderStatusEnum.PENDING.getValue());
```

---

## Problem 2: Multi-Tenant Isolation Failures (PENDING INVESTIGATION)

### Error Message
```
org.opentest4j.AssertionFailedError:
expected: null
 but was: PlayerEntity(playerId=7, username=tenant1_..., ...)
```

### Hypothesis
- TenantContext not cleared between test phases
- MyBatis tenant interceptor not filtering SELECT queries
- Race condition with @BeforeEach cleanup

### Next Steps
1. Read tenant isolation test implementation
2. Check TenantContext lifecycle in BaseIntegrationTest
3. Verify tenant interceptor configuration

---

## Problem 3: Form Validation Failure (PENDING INVESTIGATION)

### Error Message
```
org.opentest4j.AssertionFailedError:
Expecting value to be false but was true
```

### Next Steps
1. Read validation test implementation
2. Check PlayerRegistrationIntegrationForm validation annotations
3. Verify Spring validation is triggered

---

## Files Analyzed
- ✅ FirstDepositBonusIntegrationService.java (lines 1-352)
- ✅ PlayerRegistrationJourneyIntegrationTest.java (lines 200-279)
- ✅ PlayerRegistrationTestFixture.java (lines 1-184)
- ✅ PaymentOrderEntity.java (lines 0-66)

## Problem 1.2: Payment Order request_id Null Constraint Violation ❌ (NEW)

### Status Update (2026-03-19 14:50)
✅ **wallet_id issue FIXED** - Overloaded method implemented, all 4 call sites updated
❌ **NEW ISSUE DISCOVERED** - request_id constraint violation

### Error Message
```
org.springframework.dao.DataIntegrityViolationException:
ERROR: null value in column "request_id" of relation "t_payment_order" violates not-null constraint
詳細：Failing row contains (1, 1, DEP-1773903029959-10, 4, 7, 1, 100.0000, USD, 1, MOCK_PSP,
                         PSP-TX-1773903029960, null, null, null, null, null, 0, f, ...)
                                                          ^^^^
                                                      request_id is null
```

### Root Cause (CONFIRMED)
**File**: `PlayerRegistrationTestFixture.java`
**Method**: `createPaymentOrder()` (lines 162-175)
**Issue**: Method does NOT set `requestId` field when creating PaymentOrderEntity

**Code Analysis**:
```java
public static PaymentOrderEntity createPaymentOrder(
    Long playerId, Long walletId, String orderNo, BigDecimal amount, Integer status) {
  PaymentOrderEntity entity = new PaymentOrderEntity();
  entity.setPlayerId(playerId);
  entity.setWalletId(walletId); // ✅ Set wallet ID (FIXED)
  entity.setOrderNo(orderNo);
  entity.setOrderType(1);
  entity.setStatus(status);
  entity.setAmount(amount);
  entity.setCurrencyCode("USD");
  entity.setPspCode("MOCK_PSP");
  entity.setPspTransactionId("PSP-TX-" + System.currentTimeMillis());
  // ❌ MISSING: entity.setRequestId(???)
  return entity;
}
```

**Database Constraint**:
- Table: `t_payment_order`
- Column: `request_id`
- Constraint: `NOT NULL`
- Purpose: Idempotency key for payment operations
- Source: PaymentOrderEntity.java line 54

**Impact**:
- Same 3 test methods still failing:
  1. `shouldAwardFirstDepositBonus()`
  2. `shouldHandleFirstDepositWithoutPromotion()`
  3. `shouldNotAwardBonusOnSecondDeposit()`

### Solution Strategy

**Option A: Generate unique requestId in fixture**
```java
entity.setRequestId("REQ-" + orderNo + "-" + System.currentTimeMillis());
```
- ✅ Pros: Simple, unique per order
- ✅ Pros: Follows idempotency key pattern
- ✅ Pros: No test code changes needed

**SELECTED**: Option A

---

## Problem 1.3: Database Table Missing - t_wallet_bonus_ext ❌ (RESOLVED)

### Status Update (2026-03-19 15:03)
✅ **wallet_id issue FIXED** - No more constraint violations
✅ **request_id issue FIXED** - Idempotency key generated
❌ **NEW ISSUE DISCOVERED** - Database table `t_wallet_bonus_ext` does not exist

### Error Message
```
ERROR: relation "t_wallet_bonus_ext" does not exist
```

### Root Cause (CONFIRMED)
**Issue**: Flyway migrations were not being applied correctly due to:
1. Missing migration scripts in `src/test/resources/db/migration`
2. Flyway attempting to load from both main and test resources (version conflict)

**Solution Applied**:
1. ✅ Removed duplicate test migration scripts (V1, V2)
2. ✅ Flyway now uses main resource directory migrations (V001-V006)
3. ✅ `t_wallet_bonus_ext` table created successfully by V004__create_activity_tables.sql

**Result**: Database initialization successful, Flyway migrations applied correctly

---

## Current Test Status (2026-03-19 15:04)

**Progress**: 5/10 tests passing (50% pass rate)

### Passing Tests ✅
1. `shouldRegisterPlayerWithWallets()` - Player registration with CASH and BONUS wallets
2. `shouldRegisterWithReferralCode()` - Player registration with referral code
3. `shouldFailDuplicateUsername()` - Duplicate username validation
4. `shouldNotAwardBonusOnSecondDeposit()` - Second deposit does not trigger bonus
5. `shouldHandleInvalidDepositCallback()` - Invalid deposit callback handling

### Failing Tests ❌
1. `shouldAwardFirstDepositBonus()` - **NEW ISSUE**: Transaction aborted during bonus award
2. `shouldHandleFirstDepositWithoutPromotion()` - **NEW ISSUE**: Related to bonus logic
3. `shouldIsolateTenantData()` - **Original issue**: Tenant data leakage
4. `shouldIsolateWalletAcrossTenants()` - **Original issue**: Wallet tenant isolation
5. `shouldValidateRegistrationFormConstraints()` - **Original issue**: Form validation not triggered

---

## Problem 2: Transaction Abort in First Deposit Bonus ⚠️ (UNDER INVESTIGATION)

### Error Message
```
org.springframework.jdbc.UncategorizedSQLException:
ERROR: current transaction is aborted, commands ignored until end of transaction block
```

### Hypothesis
- Occurs at `FirstDepositBonusIntegrationService.processFirstDeposit()` line 193
- Likely root cause in Step 3-4: Bonus award process
  - Line 282-283: `bonusDistributionManager.distributeBonus()`
  - Line 295: `walletService.creditBonus()`
- Transaction fails but is not properly rolled back
- Subsequent SQL queries fail with "transaction is aborted"

### Investigation Needed
1. Check `bonusDistributionManager.distributeBonus()` implementation
2. Verify `walletService.creditBonus()` error handling
3. Review transaction propagation in `@Transactional` annotations
4. Check if test data setup is complete (e.g., PromotionRuleEntity inserted correctly)

---

## Next Action
1. **Priority 1**: Investigate transaction abort issue in first deposit bonus tests (2 tests)
2. **Priority 2**: Fix multi-tenant isolation tests (2 tests)
3. **Priority 3**: Fix form validation test (1 test)

## Fixes Applied (Summary)

### Fix #1: Payment Order wallet_id (RESOLVED)
- **File**: `PlayerRegistrationTestFixture.java`
- **Change**: Added overloaded `createPaymentOrder()` method with `walletId` parameter
- **Impact**: Fixed 3 payment order constraint violations

### Fix #2: Payment Order request_id (RESOLVED)
- **File**: `PlayerRegistrationTestFixture.java`
- **Change**: Added `requestId` generation in `createPaymentOrder()` method
- **Impact**: Fixed idempotency key constraint violation, 1 test now passing

### Fix #3: Database Table Creation (RESOLVED)
- **Issue**: Flyway migration script conflict (V1 vs V001)
- **Change**: Removed duplicate test migration scripts
- **Impact**: All database tables created successfully, Flyway working correctly
