# Section 03 -- RBAC & Authorization / Keycloak Authentication

## Overview

This section implements role-based access control (RBAC) with a `resource:action` permission model, cached in Redis Sets for sub-10ms lookups, plus Keycloak-backed authentication with MFA enforcement, IP whitelisting, session management, impersonation tracking, and a maker-checker workflow for all sensitive operations.

**Plan sections covered**: 4.1--4.4 (RBAC & Authorization), 5.1--5.4 (Authentication & Security / Keycloak Integration)

**Dependencies**:
- **section-01-foundation**: Maven module (`sa-module-governance`), four-layer package structure, base entity classes, Flyway migration framework, database schema for tables `t_role_template`, `t_tenant_role`, `t_user_role_mapping`, `t_pending_change`, `t_impersonation_session`, `t_agent_ip_whitelist`
- **section-02-tenant-isolation**: `TenantContext` (ThreadLocal/ScopedValue), `TenantContextFilter`, RLS enforcement on all tenant-scoped tables, `TenantRlsInterceptor`

**Blocks**: section-04-agent-hierarchy (uses permission checks, maker-checker), section-09-tenant-lifecycle (uses role templates, Keycloak provisioning)

---

## 1. Tests (Write First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL, Redis, Keycloak). Write every test stub below before any implementation code.

### 1.1 Permission Check Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/rbac/PermissionCheckServiceTest.java`

```java
// Test: Permission check returns true for granted resource:action
// Test: Permission check returns false for missing permission
// Test: Redis cache hit returns correct result (SISMEMBER)
// Test: Cache miss triggers DB load and Redis Set population
// Test: Cache invalidation on role change (via Pub/Sub)
// Test: Permission check latency < 10ms p99 (JMH benchmark with Redis)
// Test: Custom role creation respects max 20 per tenant limit
// Test: Permission bundle expansion (billing:manage expands to billing:view, billing:create, billing:refund)
// Test: Three-question auth check -- resource belongs to tenant, user in tenant, role grants permission
```

### 1.2 Impersonation Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/rbac/ImpersonationServiceTest.java`

```java
// Test: Super Admin can impersonate any Brand/Tenant/Agent
// Test: Brand Admin can impersonate only their own tenants/agents
// Test: Tenant Admin cannot impersonate other tenants
// Test: Impersonation session creates audit record with both actor and target
// Test: Actions during impersonation cannot exceed target's permissions
// Test: JWT contains impersonating=true, actorId, targetId claims
```

### 1.3 Maker-Checker Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/rbac/MakerCheckerServiceTest.java`

```java
// Test: PendingChange created with PENDING status on maker initiation
// Test: Different user with approve permission can approve
// Test: Maker cannot approve their own change
// Test: Double approval prevented (atomic CAS on status=PENDING)
// Test: Expired changes (>72h) swept to EXPIRED by job
// Test: Approved change applied atomically within transaction
// Test: Rejected change not applied, checker comment recorded
// Test: Hierarchy level comparison -- checker at higher level can approve
```

### 1.4 Keycloak / Session Security Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/rbac/AuthenticationSecurityTest.java`

```java
// Test: MFA required for all forced-role accounts (cannot login without TOTP)
// Test: First login forces MFA setup before any operation
// Test: IP whitelist blocks non-whitelisted IPs
// Test: Session timeout after 30min idle
// Test: Single session enforcement -- new login kicks old session
// Test: Lockout after 5 failures (15min), 10 failures (1hr), 20 failures (permanent)
// Test: @RequireReAuth annotation blocks operation without fresh TOTP code
// Test: Valid X-MFA-Code header passes re-auth check
// Test: Keycloak HA -- failover to standby node on primary failure
```

---

## 2. Permission Architecture

### 2.1 Permission Model

Permissions follow a `resource:action` string pattern.

File: `sa-module-governance/src/main/java/com/sa/governance/domain/rbac/Permission.java`

```java
class Permission {
    String code;        // "agents:create", "settlements:approve", "config:override"
    String resource;    // "agents"
    String action;      // "create"
    String description;
}
```

**Permission bundles**: Some high-level permissions expand to a set of granular ones. For example, `billing:manage` expands to `billing:view`, `billing:create`, `billing:refund`. Bundle expansion is resolved at cache-load time so the Redis Set always contains the fully-expanded permission codes.

### 2.2 Role Templates and Tenant Roles

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/RoleTemplateEntity.java`

```java
class RoleTemplateEntity {
    Long id;
    String name;              // "brand_admin", "tenant_finance", "agent_manager"
    TenantLevel applicableAt; // Which hierarchy level can use this template
    Set<String> permissions;  // Default permission set (stored as permissions_json)
    boolean isSystem;         // true = cannot delete, only extend
}
```

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/TenantRoleEntity.java`

```java
class TenantRoleEntity {
    Long id;
    Long tenantId;
    Long templateId;          // null if fully custom
    String name;
    Set<String> permissions;  // Tenant's customized permission set (stored as permissions_json)
}
```

Role templates are defined globally and are cloneable per tenant. Each tenant may create up to **20 custom roles** (configurable via platform settings). System roles (`isSystem = true`) cannot be deleted but can be extended with additional permissions.

### 2.3 Database Tables

These tables are created by Flyway migrations in section-01-foundation. This section provides the CRUD services and caching layer on top of them.

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `t_role_template` | Global role templates | id, name, applicable_level, permissions_json, is_system |
| `t_tenant_role` | Tenant-customized roles | id, tenant_id, template_id, name, permissions_json |
| `t_user_role_mapping` | User-role assignments | user_id, tenant_id, role_id |
| `t_pending_change` | Maker-Checker queue | id, entity_type, entity_id, diff_json, maker_id, checker_id, status |
| `t_impersonation_session` | Impersonation tracking | session_id, actor_id, target_tenant_id, start_time, end_time, reason |
| `t_agent_ip_whitelist` | IP restrictions | agent_id, ip_address, approved_by, approved_at |

All tenant-scoped tables (`t_tenant_role`, `t_user_role_mapping`, `t_pending_change`, `t_impersonation_session`, `t_agent_ip_whitelist`) have RLS policies enforced by section-02.

---

## 3. Permission Check Flow (PermissionCheckService)

File: `sa-module-governance/src/main/java/com/sa/governance/service/rbac/PermissionCheckService.java`

The three-question authorization check is:
1. Does the resource belong to this tenant?
2. Is the user assigned to this tenant?
3. Does any of the user's roles grant the required permission?

**Lookup flow**:

1. Request arrives with JWT containing `userId`, `tenantId`, `roles[]`.
2. `PermissionCheckService.hasPermission(userId, tenantId, requiredPermission)`:
   - Check Redis: `SISMEMBER perm:{tenantId}:{userId} "agents:create"` -- O(1) lookup.
   - On cache miss: load all roles for the user in this tenant from the DB, compute the fully-expanded permission set (resolving bundles), store in a Redis Set with key `perm:{tenantId}:{userId}` and **300-second TTL**.
3. Return `false` (which maps to HTTP 403) if permission is not found.

**Cache invalidation**: When a role is modified or a user's role assignment changes, publish a `PermissionInvalidateEvent` to the Redis Pub/Sub channel `permission:invalidate`. All application nodes subscribe and delete the affected cache keys (`DEL perm:{tenantId}:{userId}`).

**Performance target**: p99 latency under 10ms for permission checks against Redis.

---

## 4. Impersonation (ImpersonationService)

File: `sa-module-governance/src/main/java/com/sa/governance/service/rbac/ImpersonationService.java`

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/ImpersonationSessionEntity.java`

```java
class ImpersonationSessionEntity {
    Long sessionId;
    Long actorId;           // The admin performing impersonation
    Long targetTenantId;    // The tenant being impersonated
    Long targetUserId;      // Optional: specific user being impersonated
    Instant startTime;
    Instant endTime;        // null while active
    String reason;          // Mandatory field
}
```

**Scope rules** (enforced in `ImpersonationService`):
- **Super Admin**: can impersonate any Brand Admin, Tenant Admin, or Agent.
- **Brand Admin**: can impersonate only tenants and agents within their own brand.
- **Tenant Admin**: cannot impersonate other tenants.

**During an impersonation session**:
- A secondary JWT is issued containing claims: `impersonating=true`, `actorId`, `targetId`.
- All audit events created during the session include both `actorId` and `impersonatorId`.
- The frontend renders a non-dismissable banner: "You are impersonating [target] as [actor]".
- Actions during impersonation **cannot exceed** the target user's normal permissions. This is enforced server-side: `PermissionCheckService` checks against the target's permissions when `impersonating=true` is set in the JWT.

Every impersonation session is recorded in `t_impersonation_session` for audit purposes.

---

## 5. Maker-Checker Workflow (MakerCheckerService)

File: `sa-module-governance/src/main/java/com/sa/governance/service/rbac/MakerCheckerService.java`

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/PendingChangeEntity.java`

```java
class PendingChangeEntity {
    Long id;
    String entityType;       // "TENANT", "AGENT_CREDIT", "COMMISSION_MODEL", "CONFIG_OVERRIDE"
    String entityId;
    JsonNode changeDiff;     // Before/after JSON diff
    Long makerId;
    Long checkerId;          // null until reviewed
    ChangeStatus status;     // PENDING, APPROVED, REJECTED, EXPIRED
    String makerComment;
    String checkerComment;
    Instant createdAt;
    Instant resolvedAt;
    Instant expiresAt;       // Auto-expire after 72h if not reviewed
}
```

### 5.1 Operations Requiring Maker-Checker

- **Agent**: credit adjustments, creation/deactivation, commission model changes, hierarchy changes
- **Settlement**: approval at all tiers
- **Governance**: tenant creation, billing model changes
- **Configuration**: ALL 3-layer config overrides

### 5.2 State Machine

```
PENDING --> APPROVED  (by checker with {entity}:approve at same/higher level)
PENDING --> REJECTED  (by checker)
PENDING --> EXPIRED   (by sweep job after 72h)
```

**Atomic transition guard**: Approval uses `UPDATE t_pending_change SET status = 'APPROVED', checker_id = ? WHERE id = ? AND status = 'PENDING'`. The `WHERE status = 'PENDING'` clause prevents double-approval. If the affected-row count is zero, the operation returns a conflict error.

### 5.3 Checker Eligibility

The checker must satisfy all three conditions:
1. Must be a **different user** from the maker.
2. Must hold `{entityType}:approve` permission for the relevant entity type.
3. Must be at the **same or higher hierarchy level** than the maker. Hierarchy comparison uses `materializedPath` -- the checker's path must be a prefix of (or equal to) the maker's path.

### 5.4 Expiry Sweep Job

File: `sa-module-governance/src/main/java/com/sa/governance/service/rbac/MakerCheckerExpiryJob.java`

`MakerCheckerExpiryJob` is a scheduled task that runs **every 15 minutes**. It:
1. Queries for all `PendingChange` records where `status = 'PENDING'` and `expiresAt < NOW()`.
2. Updates them to `status = 'EXPIRED'`.
3. Publishes `PendingChangeExpiredEvent` for each expired entry (to trigger notifications).

A reminder notification is sent when 50% of the SLA has elapsed (i.e., at the 36-hour mark for a 72-hour window).

---

## 6. Keycloak Integration

### 6.1 Realm and Group Configuration

File: `sa-module-governance/src/main/java/com/sa/governance/config/KeycloakConfig.java`

A self-hosted Keycloak instance manages identity for all admin and agent users. The mapping is:

| Scope | Keycloak Concept |
|-------|-----------------|
| Brand | Keycloak **Realm** (provides SSO isolation between brands) |
| Tenant | Keycloak **Group** within a realm (tenants in the same brand share a realm) |

**SSO modes** (configured per brand):

| Mode | Keycloak Config |
|------|----------------|
| Fully isolated | Separate realm per tenant |
| Brand SSO | Shared realm, tenant groups |
| Federated | External IdP via Keycloak Identity Brokering (SAML/OIDC) |

**Invariant**: Regardless of SSO mode, wallets and data remain tenant-isolated through RLS.

### 6.2 MFA Enforcement

Keycloak's authentication flow is configured to **require** TOTP for these roles: Super Admin, Brand Admin, Tenant Admin, all Agent accounts, finance, risk control, DBA, DevOps.

**First login flow**:
1. User authenticates with credentials.
2. Forced TOTP setup screen (cannot skip).
3. 10 one-time backup codes generated and displayed once.
4. No operations are permitted until MFA setup is complete.

**Device loss recovery**: SMS or email OTP with a 24--48 hour SLA, requiring upper-level manager approval.

### 6.3 Agent Session Security

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/security/IpWhitelistFilter.java`

| Control | Implementation |
|---------|---------------|
| IP whitelist (max 5 per agent) | Custom Keycloak SPI checks source IP against `t_agent_ip_whitelist` table |
| Session timeout | Keycloak realm setting: 30min idle timeout |
| Single session | Keycloak `max-sessions: 1` with "terminate oldest" policy |
| Lockout | Keycloak brute-force detection: 5 failures = 15min lock, 10 failures = 1hr lock + notification, 20 failures = permanent lock |

### 6.4 Sensitive Operation Re-Authentication

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/security/RequireReAuth.java`

File: `sa-module-governance/src/main/java/com/sa/governance/infrastructure/security/ReAuthInterceptor.java`

`@RequireReAuth` is a custom annotation placed on controller methods that handle sensitive operations. The `ReAuthInterceptor` (Spring `HandlerInterceptor`) intercepts these requests and:

1. Reads the `X-MFA-Code` header from the request.
2. Validates the TOTP code against Keycloak's TOTP verification endpoint.
3. If the header is missing or the code is invalid, returns HTTP 403 with error code `REAUTH_REQUIRED`.

**Operations requiring re-authentication**: credit adjustments, settlement approvals, agent creation/deactivation, commission model changes, hierarchy changes.

---

## 7. File Inventory

All paths are relative to the `sa-module-governance` module root.

### Source Files to Create

| Layer | Path | Purpose |
|-------|------|---------|
| domain | `domain/rbac/Permission.java` | Permission value object (`resource:action`) |
| domain | `domain/rbac/ChangeStatus.java` | Enum: PENDING, APPROVED, REJECTED, EXPIRED |
| domain | `domain/rbac/TenantLevel.java` | Enum for hierarchy levels |
| entity | `dao/entity/RoleTemplateEntity.java` | Role template entity |
| entity | `dao/entity/TenantRoleEntity.java` | Tenant-customized role entity |
| entity | `dao/entity/PendingChangeEntity.java` | Maker-checker pending change entity |
| entity | `dao/entity/ImpersonationSessionEntity.java` | Impersonation session entity |
| mapper | `dao/mapper/RoleTemplateMapper.java` | MyBatis-Plus mapper for role templates |
| mapper | `dao/mapper/TenantRoleMapper.java` | MyBatis-Plus mapper for tenant roles |
| mapper | `dao/mapper/UserRoleMappingMapper.java` | MyBatis-Plus mapper for user-role assignments |
| mapper | `dao/mapper/PendingChangeMapper.java` | MyBatis-Plus mapper for pending changes |
| mapper | `dao/mapper/ImpersonationSessionMapper.java` | MyBatis-Plus mapper for impersonation sessions |
| cache | `dao/cache/PermissionCacheOps.java` | Redis Set operations for permission caching |
| service | `service/rbac/PermissionCheckService.java` | Permission check with Redis caching |
| service | `service/rbac/RoleTemplateService.java` | Role template CRUD |
| service | `service/rbac/TenantRoleService.java` | Tenant-scoped role CRUD (max 20 custom) |
| service | `service/rbac/ImpersonationService.java` | Impersonation session management |
| service | `service/rbac/MakerCheckerService.java` | Maker-checker workflow and state machine |
| service | `service/rbac/MakerCheckerExpiryJob.java` | Scheduled expiry sweep (every 15min) |
| infra | `infrastructure/security/RequireReAuth.java` | Custom annotation for re-auth |
| infra | `infrastructure/security/ReAuthInterceptor.java` | HandlerInterceptor validating TOTP |
| infra | `infrastructure/security/IpWhitelistFilter.java` | IP whitelist enforcement filter |
| config | `config/KeycloakConfig.java` | Keycloak realm/client configuration |
| controller | `controller/rbac/RbacController.java` | REST endpoints: permission check, role CRUD |
| controller | `controller/rbac/ImpersonationController.java` | REST endpoints: start/stop impersonation |
| controller | `controller/rbac/MakerCheckerController.java` | REST endpoints: submit/approve/reject changes |
| event | `event/PermissionInvalidateEvent.java` | Published on role/assignment change |
| event | `event/PendingChangeExpiredEvent.java` | Published when sweep job expires a change |

### Test Files to Create

| Path | Purpose |
|------|---------|
| `src/test/java/.../service/rbac/PermissionCheckServiceTest.java` | Permission check + caching tests |
| `src/test/java/.../service/rbac/ImpersonationServiceTest.java` | Impersonation scope + audit tests |
| `src/test/java/.../service/rbac/MakerCheckerServiceTest.java` | State machine + double-approval tests |
| `src/test/java/.../service/rbac/AuthenticationSecurityTest.java` | MFA, IP whitelist, session, lockout tests |

---

## 8. Implementation Checklist

1. Write all test stubs from section 1 above (tests compile but fail).
2. Create domain value objects: `Permission`, `ChangeStatus` enum, `TenantLevel` enum.
3. Create entity classes: `RoleTemplateEntity`, `TenantRoleEntity`, `PendingChangeEntity`, `ImpersonationSessionEntity`.
4. Create MyBatis-Plus mappers for each entity.
5. Implement `PermissionCacheOps` -- Redis Set operations (`SADD`, `SISMEMBER`, `DEL`) for the `perm:{tenantId}:{userId}` key pattern with 300-second TTL.
6. Implement `PermissionCheckService` -- the three-question check, cache-miss DB load with bundle expansion, Redis Pub/Sub subscription on `permission:invalidate` channel.
7. Implement `RoleTemplateService` and `TenantRoleService` -- CRUD with the 20-custom-role limit enforcement.
8. Implement `ImpersonationService` -- scope validation (Super Admin > Brand Admin > Tenant Admin), JWT claims injection, audit record creation.
9. Implement `MakerCheckerService` -- `submit()`, `approve()`, `reject()` with the atomic `WHERE status = 'PENDING'` guard, checker eligibility validation (different user, correct permission, hierarchy level comparison via `materializedPath`).
10. Implement `MakerCheckerExpiryJob` -- scheduled every 15 minutes, sweeps PENDING records past `expiresAt`, sends 50%-SLA reminder notifications.
11. Create `@RequireReAuth` annotation and `ReAuthInterceptor` -- reads `X-MFA-Code` header, validates against Keycloak TOTP service, returns 403 `REAUTH_REQUIRED` on failure.
12. Create `IpWhitelistFilter` -- checks request IP against `t_agent_ip_whitelist`.
13. Configure `KeycloakConfig` -- realm-per-brand mapping, MFA enforcement flow, session policies (`max-sessions: 1`, 30min idle, brute-force thresholds).
14. Create REST controllers: `RbacController`, `ImpersonationController`, `MakerCheckerController`.
15. Run full test suite -- all stubs should pass.
