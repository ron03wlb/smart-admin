# Section 02 -- Multi-Tenant Data Isolation

## Overview

This section implements the complete multi-tenant data isolation layer for the Governance & Agent Domain. It covers the hybrid isolation strategy (RLS for shared-tier, dedicated databases for compliance-sensitive brands), the tenant context propagation pipeline from HTTP request through database query, the tenant hierarchy model with materialized paths, and cross-tenant data sharing rules.

**Target stack**: Spring Boot 3.x, Java 21, PostgreSQL (RLS), MyBatis-Plus, HikariCP, Redis

**Depends on**: section-01-foundation (Maven module structure, base entity classes, Flyway migration framework, PostgreSQL schema for `t_tenant`, RLS policy SQL on all tables, `FORCE ROW LEVEL SECURITY`)

**Blocks**: section-03-rbac-auth, section-04-agent-hierarchy, section-07-compliance-flags, section-08-audit-system, section-09-tenant-lifecycle

---

## 1. Tests (Write First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL). Tests are grouped by subsystem.

### 1.1 RLS Policy Tests (Testcontainers -- PostgreSQL)

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/RlsPolicyIntegrationTest.java

// Test: RLS policy blocks queries without tenant context set
//   Connect as app_user (non-superuser), run SELECT on t_agent without SET rls.tenant_id.
//   Expect: zero rows returned (not an error, just empty result set).

// Test: RLS policy returns only matching tenant rows when context is set
//   Insert rows for tenant_id=1 and tenant_id=2.
//   SET rls.tenant_id = '1', SELECT * FROM t_agent.
//   Expect: only tenant_id=1 rows.

// Test: FORCE ROW LEVEL SECURITY blocks table owner from bypassing
//   Connect as the table owner role, run SELECT without setting rls.tenant_id.
//   Expect: zero rows (FORCE prevents owner bypass).

// Test: Non-superuser role used for all application connections
//   Query current_user on an application DataSource connection.
//   Expect: 'app_user', not 'postgres'.

// Test: RLS overhead < 5% (JMH benchmark: query latency with vs without RLS)
//   Benchmark a SELECT with RLS enabled vs a non-RLS table of identical structure.
//   Expect: p99 overhead below 5%.
```

### 1.2 Connection Pool Hooks Tests

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/ConnectionPoolHooksTest.java

// Test: Connection pool checkout hook sets rls.tenant_id
//   Obtain a connection from HikariCP while TenantContext holds tenant_id=42.
//   Execute: SELECT current_setting('rls.tenant_id').
//   Expect: '42'.

// Test: Connection pool return hook resets rls.tenant_id to -1
//   After connection is returned to pool, obtain it again (no TenantContext set).
//   Execute: SELECT current_setting('rls.tenant_id').
//   Expect: '-1' (invalid sentinel).
```

### 1.3 Tenant Context Flow Tests

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/TenantContextFilterTest.java

// Test: TenantContextFilter extracts tenant_id from JWT and sets ThreadLocal
//   Mock HTTP request with JWT containing tenant_id=7 claim.
//   Invoke filter chain. Inside chain, assert TenantContext.getCurrentTenantId() == 7.

// Test: TenantContextFilter clears ThreadLocal on request completion
//   After filter chain completes, assert TenantContext.getCurrentTenantId() is null/empty.
```

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/TenantRoutingDataSourceTest.java

// Test: TenantRoutingDataSource routes shared-tier tenant to shared DB
//   Set TenantContext to a tenant with tier=SHARED.
//   Resolve DataSource. Expect: shared DataSource instance.

// Test: TenantRoutingDataSource routes dedicated-tier tenant to dedicated DB
//   Set TenantContext to a tenant with tier=DEDICATED, dbIdentifier='db_ukgc_brand1'.
//   Resolve DataSource. Expect: dedicated DataSource for that identifier.
```

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/TenantRlsInterceptorTest.java

// Test: TenantRlsInterceptor sets rls.tenant_id before every MyBatis query
//   Configure interceptor with Mockito. Execute a mapper method.
//   Verify that SET LOCAL rls.tenant_id = '<current_tenant>' was executed on the connection.
```

### 1.4 Virtual Thread and Async Propagation Tests

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/TenantContextPropagationTest.java

// Test: ScopedValue propagation works with virtual threads
//   Set tenant context via ScopedValue.
//   Submit task to virtual thread executor.
//   Inside task, assert tenant_id is accessible and correct.

// Test: TenantAwareTaskDecorator propagates context to @Async child threads
//   Set TenantContext.setCurrentTenantId(99).
//   Execute an @Async method decorated by TenantAwareTaskDecorator.
//   Inside async method, assert TenantContext.getCurrentTenantId() == 99.

// Test: Kafka consumer correctly reads tenant_id from message headers
//   Produce a Kafka message with header "X-Tenant-Id: 55".
//   Consume it. Assert TenantContext is set to 55 during processing.
```

### 1.5 Cross-Tenant Leak Test (Integration)

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/CrossTenantLeakTest.java

// Test: Cross-tenant data leak test -- Tenant A data invisible to Tenant B query
//   Insert agent record for tenant_id=1.
//   Set rls.tenant_id='2'. SELECT * FROM t_agent WHERE id = <inserted_id>.
//   Expect: zero rows.
```

### 1.6 Tenant Hierarchy Model Tests

```java
// File: sa-module-governance/src/test/java/com/sa/governance/tenant/TenantHierarchyTest.java

// Test: Materialized path correctly generated on tenant creation
//   Create Platform(id=1), Brand(id=5, parent=1), Tenant(id=12, parent=5).
//   Expect path for Tenant 12: "/1/5/12".

// Test: Subtree query via LIKE returns all descendants
//   Given path "/1/5/", query WHERE materialized_path LIKE '/1/5/%'.
//   Expect: all tenants under Brand 5.

// Test: Direct children query via parent_id is correct
//   Query WHERE parent_id = 5. Expect only direct children, not grandchildren.

// Test: Ancestry extraction from path string is correct
//   Given path "/1/5/12", extract ancestor IDs.
//   Expect: [1, 5, 12].

// Test: Shared data rules -- blacklist visible within brand, invisible across brands
//   Create blacklist entry under Brand 5. Query from Tenant under Brand 5: visible.
//   Query from Tenant under Brand 6: not visible.

// Test: Cross-tenant query audit -- every cross-tenant query creates audit record
//   Perform a cross-tenant query (e.g., brand aggregate report).
//   Assert an audit record was created with actor, timestamp, scope, data volume.
```

---

## 2. Implementation Details

### 2.1 Hybrid Isolation Strategy

The system supports two isolation tiers, selectable per tenant:

**Shared Tier (RLS)**: PostgreSQL Row-Level Security is applied to every tenant-scoped table. A session variable `rls.tenant_id` is set on each database connection before any query executes. RLS policies filter all SELECT/INSERT/UPDATE/DELETE operations to the current tenant's data. Every tenant-scoped table has a non-nullable `tenant_id` column with a B-tree index.

**Dedicated Tier**: Brands requiring physical isolation (e.g., UKGC-licensed operators) receive their own PostgreSQL database. The schema is identical across shared and dedicated databases, allowing tenant migration between tiers. The `TenantRoutingDataSource` selects which database to connect to based on tenant configuration.

RLS hardening rules (all mandatory):
- `ALTER TABLE ... FORCE ROW LEVEL SECURITY` on every tenant-scoped table (prevents table owners from bypassing RLS)
- The application connects as a non-superuser role (`app_user`, never `postgres`)
- `rls.tenant_id` is set at the HikariCP connection pool level (on checkout), not only in MyBatis -- this protects native JDBC, JPA, and any third-party library queries
- On connection return to pool: reset `rls.tenant_id` to `-1` (invalid sentinel) to prevent stale context leakage
- The `TenantRlsInterceptor` (MyBatis) provides a secondary layer for explicit tenant context verification

RLS policy pattern applied per table (section-01-foundation creates the SQL; this section validates it at runtime):

```sql
ALTER TABLE t_agent ENABLE ROW LEVEL SECURITY;
ALTER TABLE t_agent FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON t_agent
  USING (tenant_id = current_setting('rls.tenant_id')::bigint);
```

### 2.2 Tenant Context Propagation Pipeline

The request lifecycle for tenant context is a five-step pipeline.

**Step 1 -- JWT extraction**: The API gateway forwards the request with a JWT containing a `tenant_id` claim (set by Keycloak at login).

**Step 2 -- TenantContextFilter**: A Spring `OncePerRequestFilter` that extracts `tenant_id` from the JWT and stores it in `TenantContext`.

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/tenant/TenantContextFilter.java`

```java
/**
 * OncePerRequestFilter that extracts tenant_id from the JWT claims
 * and stores it in TenantContext (ThreadLocal + ScopedValue).
 * Clears context in the finally block to prevent leakage.
 */
public class TenantContextFilter extends OncePerRequestFilter { ... }
```

**Step 3 -- TenantContext holder**: Dual-mode context holder using both ThreadLocal (for library compatibility) and ScopedValue (for Java 21 virtual threads).

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/tenant/TenantContext.java`

```java
/**
 * Holds current tenant_id for the request scope.
 * Primary: ScopedValue (Java 21) for virtual thread safety.
 * Fallback: ThreadLocal for libraries that do not support ScopedValue.
 *
 * Key methods:
 *   static Long getCurrentTenantId()
 *   static void setCurrentTenantId(Long tenantId)
 *   static void clear()
 */
public final class TenantContext { ... }
```

**Step 4 -- TenantRoutingDataSource**: Extends Spring's `AbstractRoutingDataSource`. Determines the lookup key by reading `TenantContext`, then consulting a tenant config cache to decide shared vs. dedicated routing.

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/tenant/TenantRoutingDataSource.java`

```java
/**
 * Routes to the correct DataSource based on tenant's isolation tier.
 * - SHARED tier -> shared DataSource (RLS handles isolation)
 * - DEDICATED tier -> per-tenant DataSource (looked up by dbIdentifier)
 *
 * Tenant config (tier + dbIdentifier) is cached in a ConcurrentHashMap,
 * refreshed on TenantConfigChangedEvent.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() { ... }
}
```

**Step 5 -- TenantRlsInterceptor**: A MyBatis interceptor that executes `SET LOCAL rls.tenant_id = ?` before every query on a shared-tier connection. This is a secondary defense layer; the HikariCP checkout hook is the primary mechanism.

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/tenant/TenantRlsInterceptor.java`

```java
/**
 * MyBatis Interceptor that sets rls.tenant_id on every statement execution.
 * Acts as defense-in-depth alongside the HikariCP connection pool hook.
 * If TenantContext is empty, throws TenantContextMissingException
 * (prevents accidental unscoped queries).
 */
@Intercepts({@Signature(type = Executor.class, method = "query", ...),
             @Signature(type = Executor.class, method = "update", ...)})
public class TenantRlsInterceptor implements Interceptor { ... }
```

### 2.3 HikariCP Connection Pool Hooks

The connection pool hooks ensure `rls.tenant_id` is always set at the JDBC level, regardless of whether MyBatis or raw JDBC is used.

File: `sa-module-governance/src/main/java/com/sa/governance/config/TenantAwareHikariConfig.java`

The configuration customizes HikariCP with two behaviors:

1. **On connection checkout**: Read `TenantContext.getCurrentTenantId()` and execute `SET LOCAL rls.tenant_id = '<id>'` on the connection. If no tenant context exists (e.g., system-level jobs), set to `-1`.
2. **On connection return**: Execute `RESET rls.tenant_id` (or set to `-1`) to prevent stale tenant context from leaking to the next borrower.

Implementation approach: Use a custom `ConnectionCustomizer` or wrap the DataSource proxy to intercept `getConnection()` and `close()` calls.

### 2.4 Async and Virtual Thread Propagation

**TenantAwareTaskDecorator**: Wraps Spring's `TaskDecorator` interface. Before the `@Async` task runs, it captures the caller's `TenantContext` and restores it on the executor thread.

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/tenant/TenantAwareTaskDecorator.java`

```java
/**
 * TaskDecorator that propagates TenantContext to @Async threads.
 * Captures tenant_id from the calling thread, sets it on the worker thread
 * before execution, and clears it after execution.
 */
public class TenantAwareTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) { ... }
}
```

**Virtual thread safety (Java 21)**: `ThreadLocal` does not auto-propagate to virtual threads. The system uses `ScopedValue` as the primary mechanism for tenant context within request-scoped code paths. `ThreadLocal` is retained as a fallback for libraries that do not support `ScopedValue`.

All propagation scenarios that require explicit handling:
- Virtual thread executors (structured concurrency via `StructuredTaskScope`)
- `CompletableFuture` chains on `ForkJoinPool`
- Parallel stream operations within service methods
- Integration tests must verify tenant isolation under virtual thread execution

**Kafka consumer propagation**: For Kafka message consumers, the `tenant_id` is read from the message header `X-Tenant-Id` and set into `TenantContext` at the start of message processing, cleared at the end.

### 2.5 Tenant Hierarchy Model

The `t_tenant` table (created in section-01-foundation) stores the multi-level tenant hierarchy:

```java
// File: sa-module-governance/src/main/java/com/sa/governance/dao/entity/TenantEntity.java

@Table("t_tenant")
class TenantEntity {
    Long id;
    Long parentId;           // null for Platform
    String materializedPath; // "/1/5/12" -- enables LIKE prefix queries
    TenantLevel level;       // PLATFORM, BRAND, TENANT
    String name;
    String fqdn;             // white-label domain
    IsolationTier tier;      // SHARED, DEDICATED
    String dbIdentifier;     // null for SHARED; DB name for DEDICATED
    TenantStatus status;     // ACTIVE, SUSPENDED, MIGRATING, CLOSED
    JsonNode configJson;     // tenant-level overrides
}
```

The `materializedPath` field enables efficient hierarchy queries without recursive CTEs:
- **All tenants under Brand 5**: `WHERE materialized_path LIKE '/1/5/%'`
- **Direct children of Brand 5**: `WHERE parent_id = 5`
- **Full ancestry of Tenant 12**: Split the path string `"/1/5/12"` into IDs `[1, 5, 12]`

### 2.6 TenantService -- Hierarchy CRUD

File: `sa-module-governance/src/main/java/com/sa/governance/service/tenant/TenantService.java`

```java
/**
 * Service for tenant hierarchy CRUD.
 *
 * Key responsibilities:
 * - Create tenant: validate parent exists, compute materializedPath as
 *   parentPath + "/" + newId, persist, publish TenantCreatedEvent.
 * - Update tenant config: merge configJson, publish TenantConfigChangedEvent.
 * - Query descendants: LIKE query on materializedPath prefix.
 * - Query direct children: parent_id = ?.
 * - Extract ancestry: parse materializedPath into ordered list of ancestor IDs.
 * - Status transitions: ACTIVE -> SUSPENDED -> CLOSED (with validation).
 */
public class TenantService { ... }
```

File: `sa-module-governance/src/main/java/com/sa/governance/dao/mapper/TenantMapper.java`

```java
/**
 * MyBatis-Plus mapper for t_tenant.
 * Custom methods:
 *   List<TenantEntity> selectDescendants(String pathPrefix)
 *   List<TenantEntity> selectDirectChildren(Long parentId)
 */
public interface TenantMapper extends BaseMapper<TenantEntity> { ... }
```

### 2.7 Data Sharing Rules

Cross-tenant data visibility is tightly controlled:

| Data Type | Scope | Controlled By |
|-----------|-------|--------------|
| Player blacklist | Brand-level (opt-in) | Brand Admin |
| Game Provider list | Global | Super Admin |
| Payment providers | Tenant-independent (own merchant ID) | Tenant Admin |
| Risk rules | Hierarchical (global default + tenant override) | Each level |
| Compliance matrix | Global (single source of truth in this domain) | Platform + jurisdiction rules |

Cross-tenant queries are restricted to a whitelist of scenarios:
- Brand aggregate reports (Brand Admin viewing all tenants under their brand)
- Global admin views (Super Admin / Platform-level)
- Tenant migration (during MIGRATING status)

**Every cross-tenant query must be audited** with: actor ID, timestamp, scope of query, and data volume returned. This is enforced at the service layer by requiring a `CrossTenantQueryContext` parameter that triggers audit event emission.

---

## 3. File Summary

All files are within the `sa-module-governance` module:

| File Path | Purpose |
|-----------|---------|
| `src/main/java/.../infrastructure/tenant/TenantContext.java` | ThreadLocal + ScopedValue tenant ID holder |
| `src/main/java/.../infrastructure/tenant/TenantContextFilter.java` | OncePerRequestFilter for JWT tenant extraction |
| `src/main/java/.../infrastructure/tenant/TenantRoutingDataSource.java` | AbstractRoutingDataSource for shared/dedicated routing |
| `src/main/java/.../infrastructure/tenant/TenantRlsInterceptor.java` | MyBatis interceptor for SET rls.tenant_id |
| `src/main/java/.../infrastructure/tenant/TenantAwareTaskDecorator.java` | TaskDecorator for @Async context propagation |
| `src/main/java/.../config/TenantAwareHikariConfig.java` | HikariCP checkout/return hooks |
| `src/main/java/.../dao/entity/TenantEntity.java` | Entity for t_tenant |
| `src/main/java/.../dao/mapper/TenantMapper.java` | MyBatis-Plus mapper for t_tenant |
| `src/main/java/.../service/tenant/TenantService.java` | Tenant hierarchy CRUD and materializedPath management |
| `src/main/java/.../domain/enums/TenantLevel.java` | Enum: PLATFORM, BRAND, TENANT |
| `src/main/java/.../domain/enums/IsolationTier.java` | Enum: SHARED, DEDICATED |
| `src/main/java/.../domain/enums/TenantStatus.java` | Enum: ACTIVE, SUSPENDED, MIGRATING, CLOSED |
| `src/test/java/.../tenant/RlsPolicyIntegrationTest.java` | RLS enforcement tests (Testcontainers) |
| `src/test/java/.../tenant/ConnectionPoolHooksTest.java` | HikariCP tenant context set/reset tests |
| `src/test/java/.../tenant/TenantContextFilterTest.java` | Filter extraction and cleanup tests |
| `src/test/java/.../tenant/TenantRoutingDataSourceTest.java` | Shared/dedicated routing tests |
| `src/test/java/.../tenant/TenantRlsInterceptorTest.java` | MyBatis interceptor tests |
| `src/test/java/.../tenant/TenantContextPropagationTest.java` | Virtual thread, @Async, Kafka propagation tests |
| `src/test/java/.../tenant/CrossTenantLeakTest.java` | Data leak prevention integration test |
| `src/test/java/.../tenant/TenantHierarchyTest.java` | Materialized path and hierarchy query tests |

---

## 4. Implementation Checklist

1. Write all test classes listed in section 1 (they will fail initially).
2. Implement `TenantContext` with dual ThreadLocal/ScopedValue support.
3. Implement `TenantContextFilter` to extract tenant_id from JWT and populate `TenantContext`.
4. Implement `TenantRoutingDataSource` extending `AbstractRoutingDataSource`.
5. Configure `TenantAwareHikariConfig` with connection checkout/return hooks for `rls.tenant_id`.
6. Implement `TenantRlsInterceptor` as a MyBatis interceptor (defense-in-depth layer).
7. Implement `TenantAwareTaskDecorator` for `@Async` propagation.
8. Implement `TenantEntity`, `TenantMapper`, and supporting enums.
9. Implement `TenantService` with hierarchy CRUD and materialized path management.
10. Implement cross-tenant audit enforcement for whitelisted cross-tenant queries.
11. Verify all tests pass, including Testcontainers-based RLS integration tests.
12. Run the JMH benchmark to confirm RLS overhead stays below 5%.
