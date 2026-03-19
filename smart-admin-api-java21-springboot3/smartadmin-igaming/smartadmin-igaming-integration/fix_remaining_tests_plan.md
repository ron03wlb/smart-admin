# Task Plan: Fix Remaining 6 Integration Test Failures

## Goal
Fix 6 failing integration tests in PlayerRegistrationJourneyIntegrationTest that are unrelated to the "Wallet already exists" issue we just resolved.

## Context
- **Previous fix**: Successfully resolved duplicate CASH wallet creation (commit 2be67a49)
- **Current status**: 1/7 tests passing, 6/7 tests failing
- **Test results**:
  - ✅ shouldRegisterPlayerWithWallets - PASSED
  - ❌ 6 tests failing (pre-existing issues)

## Test Failures Analysis

### Category 1: Payment Order Tests (3 failures) - P0 Priority

**Tests**:
1. `shouldAwardFirstDepositBonusOnFirstDeposit`
2. `shouldHandleFirstDepositWithoutActivePromotion`
3. `shouldNotAwardBonusOnSecondDeposit`

**Error**:
```
org.springframework.dao.DataIntegrityViolationException:
ERROR: null value in column "wallet_id" of relation "t_payment_order" violates not-null constraint
```

**Root Cause Hypothesis**:
- FirstDepositBonusIntegrationService creates PaymentOrder without setting wallet_id
- Need to investigate how wallet_id should be populated

### Category 2: Multi-Tenant Isolation Tests (2 failures) - P1 Priority

**Tests**:
1. `shouldIsolateTenantData`
2. `shouldIsolateWalletAcrossTenants`

**Error**:
```
org.opentest4j.AssertionFailedError:
expected: null
 but was: PlayerEntity(playerId=7, username=tenant1_..., ...)
```

**Root Cause Hypothesis**:
- TenantContext not being set correctly during test execution
- MyBatis tenant interceptor not filtering queries properly
- Possible race condition with @BeforeEach cleanup

### Category 3: Form Validation Test (1 failure) - P2 Priority

**Test**:
1. `shouldValidateRegistrationFormConstraints`

**Error**:
```
org.opentest4j.AssertionFailedError:
Expecting value to be false but was true
```

**Root Cause Hypothesis**:
- Validation logic not being triggered
- Form constraints not properly configured

## Implementation Phases

### Phase 1: Investigate Payment Order Issue ⏳
**Status**: in_progress
**Priority**: P0
**Tasks**:
- [ ] Read FirstDepositBonusIntegrationService.java
- [ ] Read PaymentOrder entity definition
- [ ] Identify where wallet_id should be set
- [ ] Check if processFirstDeposit() method exists
- [ ] Review WalletService integration

**Expected Files**:
- FirstDepositBonusIntegrationService.java
- PaymentOrderEntity.java
- PaymentOrder creation logic

### Phase 2: Fix Payment Order wallet_id
**Status**: pending
**Priority**: P0
**Tasks**:
- [ ] Add wallet_id population logic
- [ ] Ensure CASH wallet ID is used for deposits
- [ ] Add validation to prevent null wallet_id
- [ ] Run affected tests

### Phase 3: Investigate Multi-Tenant Isolation
**Status**: pending
**Priority**: P1
**Tasks**:
- [ ] Read tenant isolation test implementation
- [ ] Check TenantContext lifecycle in tests
- [ ] Verify MyBatis tenant interceptor configuration
- [ ] Review @BeforeEach cleanup timing

### Phase 4: Fix Multi-Tenant Isolation
**Status**: pending
**Priority**: P1
**Tasks**:
- [ ] Add explicit TenantContext.clear() in tests
- [ ] Fix tenant interceptor if needed
- [ ] Add tenant ID assertion before queries
- [ ] Run isolation tests

### Phase 5: Fix Form Validation Test
**Status**: pending
**Priority**: P2
**Tasks**:
- [ ] Read validation test implementation
- [ ] Check PlayerRegistrationIntegrationForm validation annotations
- [ ] Verify validation is triggered
- [ ] Run validation test

### Phase 6: Final Verification
**Status**: pending
**Priority**: P0
**Tasks**:
- [ ] Run all 7 integration tests
- [ ] Verify 7/7 tests passing
- [ ] Check no regressions
- [ ] Update documentation

## Success Criteria
- [ ] All 7 integration tests passing (7/7)
- [ ] No test data conflicts
- [ ] No tenant data leakage
- [ ] Clean test execution (no warnings)

## Errors Encountered
| Error | Phase | Attempt | Resolution |
|-------|-------|---------|------------|
| wallet_id null constraint | Phase 1 | - | Under investigation |

## Notes
- Focus on P0 (payment orders) first - blocks 3 tests
- Multi-tenant isolation (P1) is critical for production readiness
- Form validation (P2) can be deferred if time-constrained

## Files to Investigate
- FirstDepositBonusIntegrationService.java
- PaymentOrderEntity.java
- PlayerRegistrationJourneyIntegrationTest.java (test implementation)
- TenantContext.java (tenant management)
