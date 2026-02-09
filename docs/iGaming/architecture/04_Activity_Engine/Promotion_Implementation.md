# Promotion Implementation

> **Canonical Source**: [00-13_Promotion_Implementation.md](../../source-archive/00_Foundation/guides/00-13_Promotion_Implementation.md)
> **Audience**: Architects, Backend Engineers, Product Engineers
> **Business Requirements**: [Promotion_Requirements.md](../../requirements/04_Promotions_VIP/Promotion_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

This document covers the technical implementation of the iGaming platform promotion system, including the bonus distribution engine, wagering requirement tracking, and VIP tier system. All implementations follow SmartAdmin layered architecture patterns.

---

## 2. Bonus Distribution Engine

**Status**: PLANNED (Phase 5+)
**Modules**: 04_Activity_Center, 01_Core_Financial_Loop

### Implementation Goal

Build configurable bonus type definitions, a rule engine for distribution logic, trigger condition management, and wallet integration for bonus fund allocation.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) | S3 Rule Engine | Bonus distribution logic |
| 2 | [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) | Full doc | Calculation formulas |
| 3 | [02-06 Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) | S2 Bonus Wallet | Wallet integration |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | Bonus claim API, campaign query API (`@SaCheckPermission` for admin, `@NoNeedLogin` for public campaign listing) |
| Service | Bonus eligibility validation, claim processing (Vavr Option for player/campaign lookups) |
| Manager | @Transactional bonus distribution + wallet credit (atomic operation), @Cacheable campaign config |
| Dao | Bonus record CRUD, campaign configuration queries via MyBatis Plus |

### Key Technical Considerations

- **Distributed Lock**: Use Redisson `RLock` to prevent concurrent duplicate claims
- **Idempotency**: Claim operations must be idempotent (use claim_id as dedup key)
- **Wallet Integration**: Bonus credit to wallet must be atomic with bonus record creation (Manager @Transactional)
- **Rule Engine**: Consider LiteFlow for complex bonus rule evaluation chains

### Verification Checklist

- [ ] Bonus types configured correctly (first deposit, wagering, activity, etc.)
- [ ] Distribution rule engine operates normally
- [ ] Anti-duplicate claim mechanism is effective
- [ ] Bonus wallet balance is correct

### Common Pitfalls

1. **Duplicate Claims**: Concurrent requests bypass validation -- use Redisson distributed lock
2. **Incomplete Wagering**: New bonus on unchecked wagering -- query outstanding wagering before distribution
3. **Expired Bonuses**: Expired entries not cleaned -- schedule cleanup via Snail-Job

---

## 3. Wagering Requirement Tracking

**Status**: PLANNED (Phase 5+)
**Modules**: 04_Activity_Center, 02_Game_Operations

### Implementation Goal

Implement wagering calculation logic, real-time progress tracking, and completion notifications with game-weight-aware contribution accounting.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [03-04 Turnover Calculation](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md) | S1 Three-Layer Validation | Valid bet algorithm |
| 2 | [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) | S5 Wagering Requirements | Wagering calculation |
| 3 | [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) | S4 Wagering Tracking | Progress recording |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | Wagering progress query API, completion status endpoint |
| Service | Wagering calculation logic, game weight resolution (Vavr Option for optional game configs) |
| Manager | @Transactional wagering progress update (bet settlement event), @Cacheable game weight configs |
| Dao | Wagering progress records, game weight configuration queries |

### Wagering Calculation Logic

```
effective_wagering = bet_amount * game_weight_percentage
total_progress = SUM(effective_wagering) across all qualifying bets
requirement_met = total_progress >= required_wagering_amount
```

### Game Weight Configuration

| Game Category | Weight | Contribution Formula |
|--------------|--------|---------------------|
| Slots | 100% | `bet_amount * 1.0` |
| Table Games | 50% | `bet_amount * 0.5` |
| Live Casino | 25% | `bet_amount * 0.25` |
| Sports | Variable | `bet_amount * configured_weight` |

### Event-Driven Architecture

- **Bet Settlement Event**: Triggers wagering progress recalculation
- **Bet Cancellation Event**: Triggers deduction from accumulated progress
- **Completion Event**: Triggers notification and bonus release

### Verification Checklist

- [ ] Valid bet calculation is accurate
- [ ] Wagering progress updates in real time
- [ ] Completion notification is sent promptly
- [ ] Historical wagering records are traceable

### Common Pitfalls

1. **Game Weight Errors**: Incorrect weight for different games -- centralize weight config with @Cacheable in Manager
2. **Cancelled Bet Handling**: Must deduct calculated wagering on cancellation events
3. **Cross-Day Accumulation**: Wagering must accumulate across days until completion

---

## 4. VIP Tier System

**Status**: PLANNED (Phase 5+)
**Modules**: 01_Player_Center, 04_Activity_Center

### Implementation Goal

Implement tier definitions, upgrade/downgrade rules, points calculation, and exclusive benefit management.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [01-06 VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) | S2 Tier System | VIP definition |
| 2 | [01-06 VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) | S3 Upgrade Rules | Points calculation |
| 3 | [01-06 VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) | S4 Benefits Config | Exclusive benefits |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | VIP status query, tier benefits endpoint |
| Service | Tier calculation, upgrade/downgrade evaluation (Vavr Option for player tier lookup) |
| Manager | @Transactional tier change operations (upgrade/downgrade + benefit activation), @Cacheable tier definitions |
| Dao | VIP tier records, points history, benefit configuration |

### Tier Change Logic

```
// Upgrade check (triggered on points accumulation)
if (player.totalPoints >= nextTier.requiredPoints) {
    // Manager: @Transactional upgrade + benefit activation
    vipManager.upgradeTier(playerId, nextTier);
}

// Downgrade check (scheduled task via Snail-Job)
if (player.periodPoints < currentTier.retentionPoints
    && gracePeriod.isExpired()) {
    // Manager: @Transactional downgrade + benefit removal
    vipManager.downgradeTier(playerId, lowerTier);
}
```

### Points Lifecycle

| Event | Points Action |
|-------|--------------|
| Bet Settlement | Award points based on wagering amount |
| Deposit | Award deposit bonus points |
| Points Expiration | Scheduled cleanup of expired points |
| Tier Downgrade | Points reset for retention calculation |

### Verification Checklist

- [ ] VIP tier is correctly calculated
- [ ] Upgrade triggers accurately
- [ ] Downgrade mechanism operates normally
- [ ] Exclusive benefits take effect

### Common Pitfalls

1. **Demotion Rules**: Must define retention conditions and grace period buffer
2. **Benefit Deactivation**: Remove exclusive benefits on demotion -- trigger via Manager @Transactional
3. **Points Expiration**: Schedule periodic points cleanup via Snail-Job

---

## 5. Reference Documents

| Area | Document |
|------|----------|
| Activity Bonus | [04-04 Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) |
| Turnover Calculation | [03-04 Turnover Calculation](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md) |
| VIP & Loyalty | [01-06 VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) |
| Wallet Architecture | [02-06 Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) |

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Source Version**: 4.0.0
