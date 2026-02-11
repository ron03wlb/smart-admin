# Turnover and Valid Bet Terminology Standards

> **Canonical Source**: [00-08_Terminology_Standards.md](../../source-archive/00_Foundation/guides/00-08_Terminology_Standards.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Architecture**: N/A — Reference/glossary document
> **Last Synced**: 2026-02-08

---

## Business Value

This terminology standard delivers strategic value by:
- **Calculation Accuracy**: Ensures consistent Valid Bet calculation using the Standard Principal Method (industry standard used by Pinnacle, Betfair, Evolution Gaming)
- **Risk Mitigation**: Prevents promotion abuse by clearly defining Wagering Requirement verification timing (at withdrawal, not during betting)
- **System Consistency**: Establishes mandatory API field naming conventions (`validBet`, `wageringProgress.*`) to prevent integration errors
- **Compliance Readiness**: Provides auditable definitions that align with major game provider standards for regulatory inspections

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Terminology Compliance | 100% of new documents use standard terms | Documentation audit for prohibited term usage |
| API Naming Compliance | 100% of endpoints use standard field names | Code review and API schema validation |
| Calculation Accuracy | Zero discrepancies in Valid Bet calculations | Reconciliation between platform and game providers |
| Cross-Team Understanding | All team members can correctly define the 4 core terms | Quarterly terminology quiz (Bet Amount, Turnover, Valid Bet, Wagering Requirement) |

---

## Document Information

- **Version**: 4.0.0
- **Created**: 2026-01-28
- **Scope**: All iGaming business documentation
- **Enforcement**: Mandatory

---

## 1. Bet Amount

### Definition

The original amount a player wagers in a single bet.

### Characteristics

| Property | Description |
|---|---|
| **Scope** | Single bet |
| **Nature** | Raw, unfiltered, unadjusted |
| **Immutability** | Cannot change once bet is confirmed |

### Usage Scenarios

- API parameter passing
- Fund deduction records
- Transaction detail raw data

### Examples

| Scenario | Bet Amount |
|---|---|
| Player bets 100 on a slot machine | 100 |
| Player bets 200 on sports | 200 |

### Position in Three-Layer Architecture

- **Input**: Layer 1 risk engine input
- **Record**: `wagering_details.bet_amount`

---

## 2. Turnover

### Definition

The cumulative sum of bet amounts over a time period.

### Characteristics

| Property | Description |
|---|---|
| **Nature** | Cumulative (multiple bets added together) |
| **Time Scope** | Typically daily / weekly / monthly |
| **Financial Purpose** | Used for calculating GGR (Gross Gaming Revenue) |

### Formula

**Turnover** = Sum of all Bet Amounts

**GGR** = Turnover - Payout

### Usage Scenarios

- Financial reporting and statistics
- GGR calculation
- Revenue analysis

### Example

| Scenario | Calculation | Result |
|---|---|---|
| Player bets 10 times today at 100 each | 10 x 100 | Daily Turnover = 1,000 |
| GGR from above | 1,000 - 800 (payouts) | GGR = 200 (operator revenue) |

### Position in Three-Layer Architecture

- **Position**: Not part of the three-layer validation flow
- **Purpose**: Independent financial reporting metric

---

## 3. Valid Bet

### Definition

The single-bet amount after risk control filtering.

### Characteristics

| Property | Description |
|---|---|
| **Scope** | Single bet |
| **Filtering** | Determined by Layer 1 risk engine |
| **Can be zero** | If rejected by risk control |

### Calculation Rules (Standard Principal Method - Industry Standard)

| Outcome | Valid Bet Rule |
|---|---|
| Risk control passed | Valid Bet = Bet Amount |
| Risk control rejected | Valid Bet = 0 |
| Full Win (WIN) | Valid Bet = Bet Amount |
| Full Loss (LOSS) | Valid Bet = Bet Amount |
| Half Win (HALF_WIN) | Valid Bet = Bet Amount (NOT half) |
| Half Loss (HALF_LOSS) | Valid Bet = Bet Amount (NOT half) |
| Draw (DRAW) | Valid Bet = 0 (no risk assumed) |
| Void (VOID) | Valid Bet = 0 (no risk assumed) |

**Critical Principle**: Valid Bet is NOT affected by settlement outcome.

**Deprecated Method (Actual Risk Method)**:
- Half win/loss at 50% of Bet Amount -- this approach is NO LONGER USED

### Usage Scenarios

- Wagering requirement calculation
- VIP level calculation
- Rebate calculation

### Examples

| Scenario | Result |
|---|---|
| Normal bet: Player bets 100, risk check passes | Valid Bet = 100 |
| Hedge bet: Player bets 100, risk detects hedging | Valid Bet = 0 |
| Sports half-win: Player bets 100, payout 145 | Valid Bet = 100 (NOT 50) |

### Industry Standard References

| Provider | Method Used |
|---|---|
| Pinnacle, Betfair | Standard Principal Method |
| Pragmatic Play | Standard Principal Method |
| Evolution Gaming | Standard Principal Method |

### Position in Three-Layer Architecture

- **Layer 1**: Risk engine determines valid_bet
- **Layer 2**: Unchanged, only records settlement status
- **Layer 3**: Applies game weights

---

## 4. Wagering Requirement

### Definition

The total valid bet amount threshold a player must reach.

### Characteristics

| Property | Description |
|---|---|
| **Nature** | Threshold value |
| **Calculation** | Cumulative: Sum of (Valid Bet x Game Weight) |
| **Scope** | Tied to specific promotion/bonus |

### Formula

**Wagering Requirement** = Deposit Amount x Multiplier

**Progress Calculation**:

| Metric | Formula |
|---|---|
| Completed | Sum of (Valid Bet x Game Weight) |
| Remaining | Requirement - Completed |
| Percentage | (Completed / Requirement) x 100% |

### Game Weights

| Game Type | Weight | Description |
|---------|------|------|
| Slots | 100% | Full contribution |
| Sports Betting | 100% | Full contribution |
| Baccarat | 15% | Only 15% counts |
| Blackjack | 10% | Only 10% counts |
| Roulette | 20% | Only 20% counts |

### Usage Scenarios

- Promotion verification
- Withdrawal restrictions
- Bonus unlock conditions

### Example

**Promotion**: Deposit 100, get 100 bonus, 10x wagering requirement

**Wagering Requirement** = (100 + 100) x 10 = **2,000**

| Player Bet Record | Amount | Weight | Contribution |
|---|---|---|---|
| Slots | 800 | 100% | 800 |
| Baccarat | 600 | 15% | 90 |
| Roulette | 300 | 20% | 60 |
| **Total Completed** | | | **950** |
| **Remaining** | | | **1,050** |
| **Progress** | | | **47.5%** |

### Verification Timing (Industry Standard)

**Correct Approach**: Verify at withdrawal time

| Step | Action |
|---|---|
| During betting | Only accumulate progress |
| At withdrawal | Verify whether requirement is met |
| If met | Unlock bonus wallet |

**Deprecated Approach**: Auto-unlock during betting (risk: player continues playing after reaching target and loses bonus)

### Position in Three-Layer Architecture

- **Layer 3 Output**: Accumulates contributed_amount
- **Withdrawal Verification**: Checks whether Wagering Requirement is met

---

## 5. Key Relationship Flow

### Single Bet Data Flow

| Stage | Term | Example | Description |
|---|---|---|---|
| Player bets | Bet Amount | 100 | Original amount |
| Risk engine check | Valid Bet | 100 (passed) or 0 (rejected) | Not affected by settlement |
| Apply weight | Contributed Amount | 100 x 1.0 = 100 | After game weight |
| Accumulate | Wagering Progress | 950 + 100 = 1,050 | Running total toward target |

### Time-Based Accumulation

| Term | Purpose | Formula |
|---|---|---|
| Turnover | Financial reporting | Sum of all Bet Amounts |
| Wagering Progress | Promotion tracking | Sum of (Valid Bet x Game Weight) |

---

## 6. Terminology Reference Table

| Chinese | English | Abbreviation | Definition | Unit | Usage |
|------|------|------|------|------|------|
| **Bet Amount** | Bet Amount | - | Single original bet amount | Per bet | API interaction, fund deduction |
| **Turnover** | Turnover | - | Cumulative sum of bet amounts over time | Cumulative | GGR calculation, financial reports |
| **Valid Bet** | Valid Bet | VB | Single bet amount after risk filtering | Per bet | Wagering requirements, rebate, VIP |
| **Wagering Requirement** | Wagering Requirement | WR | Total valid bet threshold to reach | Cumulative | Promotion verification, withdrawal restrictions |

---

## 7. Prohibited Terminology

| Incorrect Term | Correct Term | Reason |
|-----------|-----------|------|
| Effective Turnover (mixed) | Valid Bet (single) / Total Valid Bets (cumulative) | Confuses single-bet and cumulative concepts |
| Remaining Turnover Requirement (vague) | Remaining Wagering Requirement | Imprecise terminology |
| Effective Turnover (English) | Valid Bet | Confuses Turnover (cumulative) with Valid Bet (single) |
| RemainingRollover | Remaining Wagering Requirement | Non-standard terminology |

---

## 8. Naming Conventions for Code and APIs

### API Response Field Naming

**Correct naming convention**:

| Field | Meaning |
|---|---|
| `betAmount` | Bet Amount |
| `validBet` | Valid Bet |
| `wageringProgress.totalRequirement` | Wagering Requirement |
| `wageringProgress.completedAmount` | Completed amount |
| `wageringProgress.remainingAmount` | Remaining amount |
| `wageringProgress.percentage` | Progress percentage |

**Deprecated naming (do not use)**:

| Deprecated Field | Replacement |
|---|---|
| `effectiveTurnover` | `validBet` |
| `turnoverRequirement` | `totalRequirement` |

---

## 9. Documentation Citation Standard

### How to Reference These Terms

When mentioning these terms in other documents, the first occurrence should include the full definition or reference to this document.

**Correct citation examples**:
- "Valid Bet (see Terminology Standards, Section 3)"
- "Per the Standard Principal Method, Valid Bet = Bet Amount"

### Enforcement Rules

| Scope | Requirement |
|---|---|
| All new documents | Must follow this terminology standard |
| Existing documents | Replace with standard terms during revision |
| Code reviews | Check naming compliance |

---

## 10. Version History

| Version | Date | Changes | Author |
|------|------|---------|------|
| 1.0.0 | 2026-01-28 | Initial version defining four core terms | Claude Code |

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Product Management Team
