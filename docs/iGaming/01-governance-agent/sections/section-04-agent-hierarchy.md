# Section 04: Agent Hierarchy and Credit Network

## Overview

This section implements the **Agent Hierarchy and Credit Network** -- the core subsystem that models the multi-level agent tree (up to 10 levels deep), manages real-time credit allocation with a write-ahead durability pattern, monitors risk thresholds, integrates with player self-exclusion events, and provides concurrency-safe hierarchy change operations with advisory locking.

**Module**: `sa-module-governance` within `smart-admin-api`
**Target stack**: Spring Boot 3.x, Java 21, PostgreSQL (with RLS), Redis (with Lua scripts and AOF), MyBatis-Plus

### Dependencies on Prior Sections

- **section-01-foundation**: Provides the Maven module skeleton, SmartAdmin V3.0 four-layer package structure, base entity classes, Flyway migration framework, and the PostgreSQL schema for `t_agent` and `t_agent_closure` tables (DDL + RLS policies).
- **section-02-tenant-isolation**: Provides `TenantContext`, `TenantContextFilter`, `TenantRlsInterceptor` (MyBatis), and HikariCP connection pool hooks that set/reset `rls.tenant_id`. All agent queries flow through RLS.
- **section-03-rbac-auth**: Provides `PermissionCheckService`, Maker-Checker workflow (`MakerCheckerService`), and Keycloak integration. Agent creation requires Maker-Checker approval. `@RequireReAuth` is used on sensitive credit operations.

### What This Section Blocks

- **section-05-commission-engine**: The commission engine operates on the agent tree produced here and references `AgentEntity`, credit state, and the agent hierarchy path.

---

## Tests (Write These First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis). Write the test classes and stub methods before any implementation code.

### 1.1 Agent Tree Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/agent/AgentServiceTest.java`

```java
// Test: Max 10 levels enforced on agent creation
//   - Create agents at levels 1 through 10 (succeeds)
//   - Attempt to create agent at level 11 (throws AgentHierarchyException)

// Test: Credit allocation is top-down only (parent -> child)
//   - Parent allocates credit to child (succeeds)
//   - Child attempts to allocate credit to parent (throws InvalidCreditFlowException)

// Test: Child credit limit cannot exceed parent's available allocation
//   - Parent has 10000 limit, 6000 used; child requests 5000 (throws InsufficientCreditException)
//   - Parent has 10000 limit, 6000 used; child requests 4000 (succeeds)

// Test: Agent creation requires Maker-Checker approval
//   - Creating a new agent produces a PendingChange record with status PENDING
//   - Agent is not active until the PendingChange is approved
```

### 1.2 Credit Management (Write-Ahead Pattern) Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/agent/AgentCreditServiceTest.java`

```java
// Test: Credit deduction writes WAL entry BEFORE Redis update
//   - Call checkAndDeductCredit; verify CreditTransactionLog INSERT occurs
//     before Redis Lua script execution (use ordered verification)

// Test: Successful deduction: WAL entry PENDING -> COMMITTED
//   - Deduct 100 from agent with 500 available
//   - Assert log entry transitions from PENDING to COMMITTED
//   - Assert Redis available = 400, used increased by 100

// Test: Insufficient credit: WAL entry marked REJECTED, Redis unchanged
//   - Deduct 600 from agent with 500 available
//   - Assert log entry status = REJECTED
//   - Assert Redis state unchanged

// Test: Redis Lua script atomically deducts (no race condition under concurrent calls)
//   - Launch 10 concurrent deductions of 100 each against agent with 500 available
//   - Assert exactly 5 succeed and 5 fail with INSUFFICIENT_CREDIT
//   - Assert final available = 0 (no over-deduction)

// Test: Redis failure fallback: DB with optimistic lock succeeds
//   - Simulate Redis unavailable; deduction falls through to DB path
//   - Assert t_agent.credit_used updated with optimistic lock (credit_version incremented)

// Test: Recovery process replays PENDING WAL entries to rebuild Redis state
//   - Insert 3 PENDING CreditTransactionLog entries; corrupt Redis state
//   - Run CreditRecoveryService
//   - Assert Redis state matches DB truth after recovery

// Test: Credit reconciliation detects and resolves DB/Redis divergence
//   - Manually set Redis available to wrong value
//   - Run reconciliation; assert Redis corrected to match DB

// Test: Credit deduction latency < 50ms p99 (JMH benchmark)
//   - Benchmark the happy-path deduction flow
```

### 1.3 Risk Threshold Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/agent/CreditMonitorJobTest.java`

```java
// Test: CreditMonitorJob detects 81-90% usage -> publishes CreditWarningEvent
//   - Agent with limit 10000, used 8500 -> CreditWarningEvent published

// Test: CreditMonitorJob detects 91-100% -> publishes CreditHighRiskEvent
//   - Agent with limit 10000, used 9500 -> CreditHighRiskEvent published

// Test: CreditMonitorJob detects >100% -> publishes CreditCriticalEvent (freeze)
//   - Agent with limit 10000, used 10500 -> CreditCriticalEvent published
//   - Agent status set to FROZEN

// Test: Thresholds configurable per brand via 3-layer config
//   - Override warning threshold to 70% for a specific brand
//   - Agent at 75% usage triggers CreditWarningEvent for that brand
//   - Agent at 75% usage does NOT trigger event for default-config brand
```

### 1.4 Self-Exclusion Integration Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/agent/AgentCreditListenerTest.java`

```java
// Test: PlayerSelfExclusionEvent triggers credit wallet freeze
//   - Publish event; assert player's credit_status = FROZEN

// Test: Credit operation rejected when player is self-excluded
//   - Freeze player; attempt credit grant -> rejected with PLAYER_EXCLUDED

// Test: Agent notified on player exclusion
//   - Publish event; verify notification dispatched to responsible agent
```

### 1.5 Hierarchy Change Locking Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/agent/HierarchyChangeServiceTest.java`

```java
// Test: Advisory lock prevents concurrent hierarchy changes on same subtree
//   - Two concurrent move requests on overlapping subtrees
//   - One succeeds; the other blocks until the first completes or fails

// Test: Hierarchy change rejected during active settlement
//   - Create a PENDING_REVIEW settlement record for an agent in the subtree
//   - Attempt hierarchy change -> SETTLEMENT_IN_PROGRESS error

// Test: Materialized path update is atomic for all descendants
//   - Move agent with 5 descendants; assert all 5 paths updated in single transaction
//   - No intermediate state visible to concurrent readers

// Test: Closure table maintained correctly on agent move
//   - Move agent from parent A to parent B
//   - Assert t_agent_closure rows updated: old ancestor links removed, new ones added

// Test: Redis caches invalidated for affected agents after hierarchy change
//   - Move agent; verify Redis keys for affected agent IDs are deleted
```

---

## Implementation Details

### Package Structure

All code lives under `sa-module-governance` following the SmartAdmin V3.0 four-layer convention:

```
sa-module-governance/
  src/main/java/com/sa/governance/
    controller/agent/
      AgentController.java           # REST endpoints for agent CRUD + hierarchy
      AgentCreditController.java     # REST endpoints for credit operations
    service/agent/
      AgentService.java              # Agent CRUD, hierarchy validation
      AgentCreditService.java        # Credit deduction/release with WAL pattern
      CreditRecoveryService.java     # WAL replay and Redis rebuild
      CreditMonitorJob.java          # Scheduled risk threshold scanner
      AgentCreditListener.java       # Listens for PlayerSelfExclusionEvent
      HierarchyChangeService.java    # Advisory-locked agent moves
    manager/
      AgentCreditManager.java        # Orchestrates credit flow across services
    dao/
      entity/
        AgentEntity.java             # Maps to t_agent
        AgentClosureEntity.java      # Maps to t_agent_closure
        CreditTransactionLog.java    # Maps to t_credit_transaction_log
      mapper/
        AgentMapper.java             # MyBatis-Plus mapper for t_agent
        AgentClosureMapper.java      # MyBatis-Plus mapper for t_agent_closure
        CreditTransactionLogMapper.java
      cache/
        CreditRedisCache.java        # Redis operations + Lua scripts for credit
    domain/
      enums/
        AgentType.java               # MASTER, INTERMEDIATE, TERMINAL
        AgentStatus.java             # ACTIVE, SUSPENDED, FROZEN, CLOSED
        CreditTransactionStatus.java # PENDING, COMMITTED, REJECTED, ROLLED_BACK
      event/
        CreditDeductedEvent.java
        CreditWarningEvent.java
        CreditHighRiskEvent.java
        CreditCriticalEvent.java
    config/
      CreditRedisConfig.java         # Redis connection + AOF settings
```

### Database Tables

These tables are created via Flyway migrations in section-01-foundation. This section references them:

**`t_agent`** -- Agent hierarchy and credit state

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT PK | Agent ID |
| `tenant_id` | BIGINT NOT NULL | Tenant foreign key (RLS column) |
| `parent_agent_id` | BIGINT | Null for Master Agent (tenant-direct) |
| `materialized_path` | VARCHAR(1024) | e.g. `/tenant_5/agent_1/agent_7/agent_15` |
| `level` | INT | 1 = Master, 2-10 = intermediate/terminal |
| `type` | VARCHAR(20) | MASTER, INTERMEDIATE, TERMINAL |
| `status` | VARCHAR(20) | ACTIVE, SUSPENDED, FROZEN, CLOSED |
| `credit_limit` | DECIMAL(18,2) | Maximum credit |
| `credit_used` | DECIMAL(18,2) | Currently consumed credit |
| `credit_version` | BIGINT | Optimistic lock version for DB fallback |
| `commission_agreement_id` | BIGINT | FK to `t_commission_agreement` |
| `mfa_secret` | VARCHAR(256) | Agent's TOTP secret |

**`t_agent_closure`** -- Closure table for subtree queries

| Column | Type | Description |
|--------|------|-------------|
| `ancestor_id` | BIGINT | Ancestor agent ID |
| `descendant_id` | BIGINT | Descendant agent ID |
| `depth` | INT | Distance between ancestor and descendant |

**`t_credit_transaction_log`** -- Write-ahead log for credit durability

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT PK | Log entry ID |
| `agent_id` | BIGINT NOT NULL | Agent being debited/credited |
| `tenant_id` | BIGINT NOT NULL | RLS column |
| `amount` | DECIMAL(18,2) | Transaction amount |
| `direction` | VARCHAR(10) | DEDUCT or RELEASE |
| `status` | VARCHAR(20) | PENDING, COMMITTED, REJECTED, ROLLED_BACK |
| `idempotency_key` | VARCHAR(64) UNIQUE | Prevents duplicate processing |
| `created_at` | TIMESTAMPTZ | When the WAL entry was written |
| `committed_at` | TIMESTAMPTZ | When COMMITTED status was set |

### AgentCreditService (Write-Ahead Pattern)

File: `sa-module-governance/src/main/java/com/sa/governance/service/agent/AgentCreditService.java`

This is the central credit management service. It implements a **write-ahead log (WAL) pattern** to guarantee durability even if Redis fails mid-operation.

**Credit deduction flow** (`checkAndDeductCredit(agentId, amount, idempotencyKey)`):

1. **Write-ahead**: Insert a `CreditTransactionLog` row with `status=PENDING` into PostgreSQL. This is the durable source of truth. The insert happens BEFORE any Redis interaction.

2. **Redis check**: Read `credit:{agentId}` hash from Redis (`{limit, used, available}`).

3. **If `available >= amount`**: Execute the Redis Lua script to atomically decrement `available` and increment `used`. The Lua script references the log entry ID.

4. **If insufficient**: Mark the log entry as `REJECTED` immediately. Return `INSUFFICIENT_CREDIT`.

5. **DB commit**: Update `t_agent.credit_used` with optimistic lock (`WHERE credit_version = :currentVersion`). Mark the log entry as `COMMITTED`.

6. **Publish event**: Emit `CreditDeductedEvent` for parent agent dashboard updates.

**Fallback on Redis failure**: If Redis is unavailable, fall back to direct DB updates with optimistic locking on `t_agent.credit_version`. Higher latency but correct behavior.

### CreditRedisCache (Lua Script)

File: `sa-module-governance/src/main/java/com/sa/governance/dao/cache/CreditRedisCache.java`

Manages the Redis hash `credit:{agentId}` with keys `limit`, `used`, `available`.

The atomic deduction Lua script:
```lua
-- KEYS[1] = credit:{agentId}
-- ARGV[1] = deduction amount
local available = tonumber(redis.call('HGET', KEYS[1], 'available'))
if available == nil then return -1 end  -- cache miss
local amount = tonumber(ARGV[1])
if available >= amount then
    redis.call('HINCRBY', KEYS[1], 'used', amount)
    redis.call('HSET', KEYS[1], 'available', available - amount)
    return 1  -- success
else
    return 0  -- insufficient
end
```

**Redis persistence**: The credit data Redis instance must have AOF enabled with at minimum `appendfsync everysec`.

### CreditRecoveryService

File: `sa-module-governance/src/main/java/com/sa/governance/service/agent/CreditRecoveryService.java`

Runs on two triggers:
1. **Application startup** (via `@PostConstruct` or `ApplicationReadyEvent`)
2. **Redis reconnect** (via Redis connection listener)

Recovery algorithm:
1. Query: `SELECT * FROM t_credit_transaction_log WHERE status = 'PENDING' AND created_at > now() - interval '1 hour'`
2. For each PENDING entry: check `t_agent.credit_used` to see if the deduction was actually applied to DB
3. If DB reflects the deduction: update Redis to match, mark log as `COMMITTED`
4. If DB does not reflect it: mark log as `ROLLED_BACK`
5. After replay: compare every active agent's Redis `{used, available}` against DB `{credit_used, credit_limit - credit_used}`
6. If divergence detected: rebuild the Redis hash from DB values

### HierarchyChangeService (Advisory Locking)

File: `sa-module-governance/src/main/java/com/sa/governance/service/agent/HierarchyChangeService.java`

**Move agent flow** (`moveAgent(agentId, newParentId)`):

1. **Acquire advisory lock**: `SELECT pg_advisory_lock(:subtreeRootAgentId)`
2. **Check settlement state**: Query `t_settlement_record` for any agent in the subtree with status `IN ('PENDING_REVIEW', 'APPROVED')`. If any exist, reject with `SETTLEMENT_IN_PROGRESS`.
3. **Validate**: New parent must be in the same tenant. New depth must not exceed 10 levels for any descendant.
4. **Execute path update**: Within a single transaction -- compute new materialized paths, update all descendants atomically, update closure table
5. **Invalidate caches**: Delete Redis keys for all affected agent IDs
6. **Release advisory lock**: `SELECT pg_advisory_unlock(:subtreeRootAgentId)`

### REST API Endpoints (Cross-Domain)

| Method | Path | Consumer | Description |
|--------|------|----------|-------------|
| GET | `/api/v1/agents/{id}/credit` | 02-funding | Returns `{limit, used, available}` |
| POST | `/api/v1/agents/credit/deduct` | 02-funding | Deducts credit for bet placement |
| POST | `/api/v1/agents/credit/release` | 02-funding | Releases credit on bet settlement |
| GET | `/api/v1/agents/player/{playerId}` | 02-funding, 04-gaming | Agent-player relationship lookup |

### Domain Events Published

| Event | Trigger | Payload |
|-------|---------|---------|
| `CreditDeductedEvent` | Successful credit deduction | agentId, amount, newAvailable, idempotencyKey |
| `CreditReleasedEvent` | Successful credit release | agentId, amount, newAvailable, idempotencyKey |
| `CreditWarningEvent` | Monitor detects 81-90% usage | agentId, usagePct, threshold |
| `CreditHighRiskEvent` | Monitor detects 91-100% usage | agentId, usagePct |
| `CreditCriticalEvent` | Monitor detects >100% usage | agentId, usagePct |

### Domain Events Consumed

| Event | Source | Handler |
|-------|--------|---------|
| `PlayerSelfExclusionEvent` | 03-player domain | `AgentCreditListener` |

---

## Implementation Checklist

1. Create enums: `AgentType`, `AgentStatus`, `CreditTransactionStatus`
2. Create entities: `AgentEntity`, `AgentClosureEntity`, `CreditTransactionLog`
3. Create MyBatis-Plus mappers: `AgentMapper`, `AgentClosureMapper`, `CreditTransactionLogMapper`
4. Implement `CreditRedisCache` with Lua scripts (deduct + release + rebuild)
5. Implement `AgentService` with hierarchy validation (max depth, path generation, closure table queries)
6. Implement `AgentCreditService` with the full WAL pattern (steps 1-6 of the deduction flow)
7. Implement `CreditRecoveryService` with startup and reconnect triggers
8. Implement `CreditMonitorJob` with configurable thresholds and event publishing
9. Implement `AgentCreditListener` for self-exclusion event handling
10. Implement `HierarchyChangeService` with advisory locks and atomic path updates
11. Implement `AgentCreditController` REST endpoints
12. Implement `AgentController` for agent CRUD endpoints
13. Configure Redis AOF persistence in `CreditRedisConfig`
