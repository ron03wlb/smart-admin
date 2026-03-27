# Section 05: Commission Engine

## Overview

This section implements the Commission Engine for the Governance & Agent Domain. The commission engine calculates agent commissions using a Strategy pattern that supports four distinct models. Each agent has a `CommissionAgreement` specifying which model(s) to use, and the engine can execute multiple strategies concurrently and sum the results.

**Platform context**: Spring Boot 3.x, Java 21, PostgreSQL with RLS, Redis, MyBatis-Plus. The module lives under `sa-module-governance/` following SmartAdmin V3.0 four-layer architecture (controller / service / manager / dao).

**Depends on**: section-04-agent-hierarchy (agent tree model, `AgentEntity`, materialized path, credit structures must exist before commission logic is built on top of agents).

**Blocks**: section-06-settlement (settlement consumes the commission engine to calculate totals and process payouts).

---

## Database Tables

Three tables support the commission engine. Their Flyway migrations are created in section-01-foundation. All three are tenant-scoped and require RLS policies.

### t_commission_agreement

Stores the commission configuration for each agent.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `tenant_id` | BIGINT NOT NULL | RLS tenant scope |
| `agent_id` | BIGINT NOT NULL | FK to `t_agent.id` |
| `models_json` | JSONB NOT NULL | Array of active commission model configs |
| `carry_policy` | VARCHAR(20) NOT NULL | One of: `CARRY`, `RESET_MONTHLY`, `RESET_WEEKLY`, `CAP_AT_AMOUNT` |
| `cap_amount` | NUMERIC(18,2) | Only used when `carry_policy = CAP_AT_AMOUNT` |
| `settlement_cycle` | VARCHAR(10) NOT NULL | `WEEKLY` or `MONTHLY` |
| `effective_from` | TIMESTAMP NOT NULL | Start of agreement validity |
| `effective_to` | TIMESTAMP | Null means indefinite |

### t_commission_ledger

Double-entry bookkeeping for commission tracking. Each row is an immutable entry; running balance is maintained per agent per period.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `tenant_id` | BIGINT NOT NULL | RLS tenant scope |
| `agent_id` | BIGINT NOT NULL | FK to `t_agent.id` |
| `period_key` | VARCHAR(10) NOT NULL | e.g., `2026-W13` (weekly) or `2026-03` (monthly) |
| `type` | VARCHAR(20) NOT NULL | One of: `EARNED`, `DEDUCTED`, `CARRY_FORWARD`, `RESET` |
| `amount` | NUMERIC(18,2) NOT NULL | Signed value |
| `running_balance` | NUMERIC(18,2) NOT NULL | Balance after this entry |
| `description` | TEXT | Human-readable explanation |

### t_agent_position

Records position-holding risk distribution per bet per agent.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-generated |
| `tenant_id` | BIGINT NOT NULL | RLS tenant scope |
| `agent_id` | BIGINT NOT NULL | FK to `t_agent.id` |
| `bet_id` | BIGINT NOT NULL | FK to bet in gaming domain |
| `position_pct` | NUMERIC(5,4) NOT NULL | Percentage of risk (0.0000 to 1.0000) |
| `potential_payout` | NUMERIC(18,2) NOT NULL | Potential payout amount for this position |

---

## Tests (Write First)

All tests go under `sa-module-governance/src/test/java/`. The testing stack is JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis). Write these test stubs before implementation.

### 1.1 Strategy Pattern Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/commission/CommissionStrategyTest.java`

```java
// Test: RevenueShareStrategy calculates correct commission for each tier
// Test: TurnoverRebateStrategy applies correct game-type rates
// Test: CpaStrategy counts only first-time depositing players
// Test: HybridStrategy sums RevShare + Turnover correctly
// Test: Single model agent -- only one strategy executed
// Test: Multi-model agent -- all active strategies executed and summed
```

### 1.2 Revenue Share Tier Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/commission/RevenueShareTierTest.java`

```java
// Test: GGR < $10K -> 30% share
// Test: GGR $10K-$50K -> 35% share
// Test: GGR $50K-$200K -> 40% share
// Test: GGR > $200K -> 45% share
// Test: Boundary values ($9,999.99, $10,000.00, $50,000.00)
```

### 1.3 Position Holding Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/commission/PositionHoldingTest.java`

```java
// Test: Position distribution calculated correctly across agent chain
// Test: Sub-agent positions > 100% normalized proportionally
// Test: Player loss -> agent receives position percentage of profit
// Test: Player win -> agent bears position percentage of loss
```

### 1.4 Negative Carry-Forward Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/commission/NegativeCarryForwardTest.java`

```java
// Test: Negative balance carries to next period (CARRY policy)
// Test: RESET_MONTHLY resets balance to 0 at month boundary
// Test: CAP_AT_AMOUNT caps carry-forward at configured amount
// Test: Double-entry bookkeeping -- all entries balance
```

---

## Package Structure

```
sa-module-governance/
  src/main/java/com/sa/governance/
    controller/commission/
      CommissionController.java
    service/commission/
      CommissionEngineService.java
      CommissionStrategy.java           (interface)
      RevenueShareStrategy.java
      TurnoverRebateStrategy.java
      CpaStrategy.java
      HybridStrategy.java
      PositionHoldingService.java
      CommissionLedgerService.java
    dao/entity/
      CommissionAgreementEntity.java
      CommissionLedgerEntryEntity.java
      AgentPositionEntity.java
    dao/mapper/
      CommissionAgreementMapper.java
      CommissionLedgerMapper.java
      AgentPositionMapper.java
    domain/
      NegativeCarryPolicy.java          (enum)
      CommissionModelConfig.java         (value object)
      EntryType.java                     (enum)
```

---

## Implementation Details

### CommissionStrategy Interface

```java
/**
 * Strategy interface for commission calculation models.
 * Each implementation handles one type of commission model.
 */
interface CommissionStrategy {
    BigDecimal calculate(Long agentId, String periodKey, CommissionModelConfig config);
}
```

### Strategy Implementations

**RevenueShareStrategy**: Computes commission as a tiered percentage of net Gross Gaming Revenue (GGR). Default tiers (DB-configurable per agreement):

| Monthly Net P&L (GGR) | Share Rate |
|------------------------|-----------|
| < $10,000 | 30% |
| $10,000 - $50,000 | 35% |
| $50,000 - $200,000 | 40% |
| > $200,000 | 45% |

Tier boundary at $10,000.00 is inclusive on the lower end (falls into the 35% tier).

**TurnoverRebateStrategy**: Calculates a rebate based on total valid bets multiplied by game-type-specific rates. The `CommissionModelConfig` contains a map of game category to rebate rate (e.g., `{"slots": 0.005, "live_casino": 0.003, "sports": 0.004}`).

**CpaStrategy**: Counts first-time depositing (FTD) players acquired during the period and multiplies by a fixed CPA amount.

**HybridStrategy**: Delegates to both `RevenueShareStrategy` and `TurnoverRebateStrategy`, sums the results.

### CommissionEngineService

```java
/**
 * Orchestrates commission calculation by dispatching to the correct strategies
 * based on the agent's CommissionAgreement.
 */
class CommissionEngineService {
    BigDecimal calculateTotal(Long agentId, String periodKey);
}
```

Flow:
1. Load the `CommissionAgreement` for the given agent (must be effective for the period).
2. For each entry in `activeModels`, resolve the corresponding `CommissionStrategy` bean.
3. Call `strategy.calculate(agentId, periodKey, modelConfig)` for each.
4. Sum all results into a total commission amount.
5. Return the total (may be negative if losses exceed gains).

### PositionHoldingService

```java
/**
 * Calculates and records risk distribution for position-holding agents.
 */
class PositionHoldingService {
    void calculateRiskDistribution(Long betId, String agentPath);
}
```

Implementation:
1. Parse `agentPath` to identify all agents in the chain.
2. Read `risk_position_percentage` for each agent.
3. Sum all percentages. If > 100%, normalize proportionally.
4. Create `t_agent_position` records for each agent.
5. On bet settlement: profit/loss distributed by position percentage.

### CommissionLedgerService

```java
/**
 * Manages the commission ledger with double-entry bookkeeping.
 * All balance changes are recorded as immutable ledger entries.
 */
class CommissionLedgerService {
    void recordEarned(Long agentId, String periodKey, BigDecimal amount, String description);
    void recordDeducted(Long agentId, String periodKey, BigDecimal amount, String description);
    BigDecimal getRunningBalance(Long agentId, String periodKey);
    void processCarryForward(Long agentId, String closingPeriodKey, String nextPeriodKey);
}
```

Period-end carry-forward logic:
- `CARRY` policy: insert `CARRY_FORWARD` entry in next period with same negative amount.
- `RESET_MONTHLY` policy: insert `RESET` entry zeroing the balance.
- `CAP_AT_AMOUNT` policy: carry forward up to `capAmount`, reset the remainder.
- If `runningBalance > 0`: agent is eligible for settlement payout.

---

## Key Design Considerations

1. **All monetary calculations use `BigDecimal`** with explicit scale and `RoundingMode.HALF_UP`. Never use `double` or `float`.
2. **Tier boundary behavior**: $10,000.00 falls into the 35% tier (lower-inclusive).
3. **Position normalization**: When sub-agent positions exceed 100%, proportionally reduce all to sum to exactly 100%.
4. **Ledger immutability**: Entries are append-only. Corrections are made by inserting offsetting entries.
5. **Period key format**: Weekly `YYYY-Www` (e.g., `2026-W13`), Monthly `YYYY-MM` (e.g., `2026-03`).
6. **Strategy registry**: `Map<String, CommissionStrategy>` bean for extensible model types.
7. **Tenant isolation**: All queries go through RLS via `tenant_id` columns.

---

## Integration Points

- **Consumed by section-06-settlement**: `CommissionEngineService.calculateTotal()` is called by `SettlementOrchestrator`. `CommissionLedgerService.processCarryForward()` is called when a period closes.
- **Depends on section-04-agent-hierarchy**: Uses `AgentEntity` and its materialized path for position holding chain walks.
- **Data sources**: Revenue share and turnover calculations require bet/GGR data from the gaming domain. These are stubbed as service interfaces backed by cross-domain API calls (defined in section-10-integration).
