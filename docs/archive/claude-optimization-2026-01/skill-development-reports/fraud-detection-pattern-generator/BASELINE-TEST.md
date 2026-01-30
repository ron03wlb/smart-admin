# Fraud Detection Pattern Generator - Baseline Test

## Test Scenario: Multi-Account Bonus Abuse Detection

### Background

This test validates the fraud-detection-pattern-generator skill's ability to detect and prevent multi-account bonus abuse, one of the most common fraud patterns in iGaming platforms.

### Scenario Description

**Fraudster Profile:**
- Name: Alex Chen (fraudster)
- Objective: Exploit welcome bonus by creating multiple accounts
- Method: Register 3 accounts using same device, claim $100 welcome bonus on each
- Expected gain: $300 (3 x $100 bonus)

**Timeline:**
```
10:00 AM - Register Account 1 (email: alex1@example.com)
10:05 AM - Deposit $100 on Account 1, claim welcome bonus
10:15 AM - Register Account 2 (email: alex2@example.com)
10:20 AM - Deposit $100 on Account 2, claim welcome bonus
10:30 AM - Register Account 3 (email: alex3@example.com)
10:35 AM - Deposit $100 on Account 3, claim welcome bonus
10:45 AM - Attempt to withdraw from all 3 accounts
```

**Shared Fraud Indicators:**
- Same device fingerprint: `d8f3a9e7b2c4...` (SHA-256 hash)
- Same IP address: `192.168.1.100` (home broadband)
- Similar user agents: Chrome 131 on macOS
- Similar betting patterns: All accounts bet on same games

---

## Expected Detection Logic

### Phase 1: Device Fingerprint Detection (10:15 AM)

**Trigger:** Account 2 registration

**Detection Query:**
```sql
-- Find accounts sharing device fingerprint
SELECT
    df.player_id,
    p.email,
    df.fingerprint_hash,
    df.first_seen_at
FROM device_fingerprints df
JOIN players p ON df.player_id = p.id
WHERE df.fingerprint_hash = 'd8f3a9e7b2c4...'
ORDER BY df.first_seen_at;
```

**Expected Result:**
```
player_id | email                | fingerprint_hash     | first_seen_at
----------------------------------------------------------------------
1001      | alex1@example.com    | d8f3a9e7b2c4...      | 2026-01-25 10:00:00
1002      | alex2@example.com    | d8f3a9e7b2c4...      | 2026-01-25 10:15:00
```

**Action:**
- Create `LinkedAccountGroup` with detection_method = 'DEVICE_FINGERPRINT'
- Risk score: 30 (base) + 20 (coordinated deposits) = 50
- Status: PENDING (requires confirmation)
- Alert compliance team

---

### Phase 2: Bonus Abuse Detection (10:20 AM)

**Trigger:** Account 2 bonus claim

**Detection Query:**
```sql
-- Find rapid bonus claiming
SELECT
    player_id,
    COUNT(*) as bonus_count,
    MAX(claimed_at) as last_claim
FROM bonus_claims
WHERE claimed_at > NOW() - INTERVAL '24 hours'
GROUP BY player_id
HAVING COUNT(*) > 1;
```

**Expected Result:**
```
player_id | bonus_count | last_claim
-------------------------------------
1001      | 1           | 10:05:00
1002      | 1           | 10:20:00
```

**Velocity Check:**
- Linked accounts claimed 2 bonuses in 15 minutes
- Risk score increased: +25 for RAPID_BONUS_CLAIMING
- Total risk score: 50 + 25 = 75 (HIGH)

---

### Phase 3: Account Freeze (10:30 AM)

**Trigger:** Risk score reaches 75 (HIGH threshold)

**Automated Actions:**
1. Flag Account 1 and Account 2 for manual review
2. Block bonus claims on both accounts
3. Send alert to compliance team
4. Prevent Account 3 from claiming bonus (proactive)

**Database Updates:**
```sql
-- Update linked account group status
UPDATE linked_account_groups
SET status = 'CONFIRMED', risk_score = 75
WHERE id = 12345;

-- Block bonus claims
UPDATE players
SET bonus_abuse_flag = TRUE
WHERE id IN (1001, 1002);

-- Add risk events
INSERT INTO risk_events (player_id, event_type, points, reason)
VALUES
  (1001, 'MULTI_ACCOUNT', 30, 'Device fingerprint match'),
  (1002, 'MULTI_ACCOUNT', 30, 'Device fingerprint match'),
  (1001, 'BONUS_ABUSE', 25, 'Rapid bonus claiming'),
  (1002, 'BONUS_ABUSE', 25, 'Rapid bonus claiming');
```

---

### Phase 4: Withdrawal Block (10:45 AM)

**Trigger:** Fraudster attempts withdrawal on all 3 accounts

**Detection Logic:**
```java
// Withdrawal validation in WithdrawalService
if (playerService.hasBonusAbuseFlag(playerId)) {
    throw new ServiceException("Withdrawal blocked: Account under review for bonus abuse");
}

if (riskScoreManager.getRiskScore(playerId) >= 70) {
    throw new ServiceException("Withdrawal blocked: High risk score");
}
```

**Expected Outcome:**
- Withdrawal requests rejected
- Funds remain in player accounts (pending investigation)
- Compliance team reviews case within 24 hours

---

## Expected Detection Metrics

### Detection Performance

| Metric | Target | Expected Result |
|--------|--------|-----------------|
| **Detection Rate** | >90% | ✅ 100% (all 3 accounts detected) |
| **Detection Time** | <5 minutes | ✅ 15 minutes (after 2nd account) |
| **False Positive Rate** | <5% | ✅ 0% (confirmed fraud) |
| **Processing Time** | <500ms | ✅ 312ms (device fingerprint check) |

### Risk Scoring Breakdown

| Component | Points | Justification |
|-----------|--------|---------------|
| Multi-Account (Device) | 30 | Same device fingerprint |
| Coordinated Deposits | 20 | Both accounts deposited $100 |
| Rapid Bonus Claiming | 25 | 2 bonuses in 15 minutes |
| **Total Risk Score** | **75** | **HIGH RISK** |

---

## Manual Review Checklist

**Compliance Team Actions:**

1. **Verify Device Fingerprint:**
   - [ ] Check device fingerprint components (canvas, WebGL, screen resolution)
   - [ ] Confirm match is not false positive (e.g., shared household device)

2. **Review Betting Patterns:**
   - [ ] Analyze betting history for all linked accounts
   - [ ] Check for low-risk wagering (odds < 1.1)
   - [ ] Identify opposite betting (hedging strategy)

3. **Payment Method Check:**
   - [ ] Verify if same credit card used for deposits
   - [ ] Check billing address consistency

4. **KYC Verification:**
   - [ ] Request identity documents (passport/ID card)
   - [ ] Verify each account belongs to different person
   - [ ] Check for photoshopped or stolen documents

5. **Decision:**
   - [ ] **CONFIRMED FRAUD:** Permanently ban all accounts, forfeit balances
   - [ ] **FALSE POSITIVE:** Unblock accounts, refund withheld withdrawals
   - [ ] **INCONCLUSIVE:** Request additional verification

---

## Fraud Prevention Outcomes

### Scenario A: Detection Successful (Current Test)

**Result:**
- Fraudster loses: $300 (deposit) + $0 (no bonus payout)
- Platform saves: $300 (prevented bonus theft)
- Detection cost: ~5 minutes manual review

### Scenario B: No Detection (Baseline Without Skill)

**Result:**
- Fraudster gains: $300 (3 x $100 bonus)
- Platform loses: $300 (bonus theft)
- Additional risk: Fraudster repeats with more accounts

### ROI Calculation

**Assumptions:**
- 100 fraud attempts per month
- Average fraud value: $300 per attempt
- Detection rate: 90%

**Monthly Savings:**
```
100 attempts × $300 × 90% = $27,000 saved per month
Annual savings: $324,000
```

**Implementation Cost:**
- Development: 2 weeks (~$10,000)
- Maintenance: $2,000/year

**ROI:** 3,140% annual return

---

## Test Validation Criteria

### ✅ PASS Criteria

1. **Detection Accuracy:**
   - System detects linked accounts within 5 minutes of 2nd account registration
   - Risk score calculation is accurate (75 points)
   - All 3 accounts flagged before any bonus payout

2. **Automated Actions:**
   - Bonus claims blocked on linked accounts
   - Withdrawal requests rejected
   - Compliance team receives alert within 30 seconds

3. **Performance:**
   - Device fingerprint check: <500ms
   - Risk score calculation: <200ms
   - Database queries: <100ms

4. **Data Integrity:**
   - All fraud events logged in `risk_events` table
   - Linked account group created with correct members
   - Audit trail complete (who, what, when)

### ❌ FAIL Criteria

1. **False Negative:** System misses linked accounts (detection rate <90%)
2. **False Positive:** Legitimate multi-user household flagged as fraud
3. **Performance Degradation:** Detection takes >5 seconds
4. **Data Loss:** Fraud events not logged or incomplete

---

## Alternative Test Scenarios

### Test 2: Bonus Arbitrage (Low-Risk Wagering)

**Fraudster Strategy:**
- Claim welcome bonus with 10x wagering requirement
- Bet on near-certain outcomes (odds 1.05)
- Clear wagering with minimal risk

**Expected Detection:**
- `BonusAbuseDetectionService.detectLowRiskWagering()` triggers
- Risk score: +30 for LOW_RISK_WAGERING
- Bonus cancelled, wagering progress reset

### Test 3: Payment Fraud (Card Testing)

**Fraudster Strategy:**
- Test 10 stolen credit cards with $1 deposits
- Use cards that pass for larger deposits

**Expected Detection:**
- `PaymentFraudDetectionService.detectCardTesting()` triggers after 3 failures
- Account blocked from making payments for 24 hours
- Risk score: +40 for CARD_TESTING

### Test 4: Syndicate Betting

**Fraudster Strategy:**
- Coordinate 5 accounts to place large bets on same outcome
- Manipulate odds or exploit insider information

**Expected Detection:**
- `SuspiciousBettingDetectionService.detectSyndicateBetting()` triggers
- All 5 accounts flagged for manual review
- Risk score: +50 for SYNDICATE_BETTING

---

## Success Indicators

After implementing this skill, the system should:

1. **Reduce Fraud Losses by 80%+**
   - Multi-account bonus abuse: Near 0%
   - Payment fraud: <1% of transactions
   - Betting pattern fraud: <2% of bets

2. **Maintain Low False Positive Rate (<5%)**
   - Legitimate players not impacted
   - Manual review queue manageable (<10 cases/day)

3. **Fast Detection (<5 minutes)**
   - Real-time risk scoring
   - Automated alerts to compliance team

4. **Audit Compliance**
   - Complete fraud event logging
   - 7-year data retention for regulatory compliance
   - GDPR-compliant data handling

---

## Implementation Checklist

- [ ] Create database tables (device_fingerprints, linked_account_groups, risk_scores, risk_events)
- [ ] Implement MultiAccountDetectionService
- [ ] Implement BonusAbuseDetectionService
- [ ] Implement SuspiciousBettingDetectionService
- [ ] Implement PaymentFraudDetectionService
- [ ] Implement RiskScoreManager
- [ ] Configure event listeners for real-time detection
- [ ] Set up Redis caching for risk scores
- [ ] Create compliance dashboard for manual review
- [ ] Add Prometheus metrics for monitoring
- [ ] Write integration tests with this baseline scenario
- [ ] Deploy to staging and run smoke tests
- [ ] Train compliance team on new fraud detection workflow

---

**Document Status:** Ready for RED phase testing
**Expected Test Duration:** 30 minutes
**Required Resources:** Test database, 3 test player accounts, compliance team representative
