# Multi-Tenant Architecture

> **Business Requirements**: [Multi_Tenant_Requirements.md](../../requirements/06_Governance_Licensing/Multi_Tenant_Requirements.md)
> **Canonical Source**: [`docs/iGaming/source-archive/06_Platform_Governance/06-01_Multi_Tenant.md`](../../source-archive/06_Platform_Governance/06-01_Multi_Tenant.md)
> **Audience**: Architects, Backend Developers, DevOps Engineers
> **Last Synced**: 2026-02-09

---

## 1. Architecture Overview

```mermaid
graph TB
    subgraph Platform["Platform Layer"]
        SA[Super Admin]
    end

    subgraph Brand_A["Brand A"]
        BA[Brand Admin]
        subgraph Tenants_A["Tenants"]
            T1[Tenant 1<br/>site1.com]
            T2[Tenant 2<br/>site2.com]
        end
    end

    subgraph Brand_B["Brand B"]
        BB[Brand Admin]
        subgraph Tenants_B["Tenants"]
            T3[Tenant 3<br/>site3.com]
        end
    end

    SA --> BA
    SA --> BB
    BA --> T1
    BA --> T2
    BB --> T3

    T1 --> A1[Agents]
    T2 --> A2[Agents]
    T3 --> A3[Agents]
```

**Hierarchy**: `Super Admin -> Brand -> Tenant -> Agent`

---

## 2. Database Partitioning Strategy

### 2.1 Data Isolation Approaches

```mermaid
graph LR
    subgraph Option_1["Option 1: Database per Tenant"]
        DB1[(Tenant 1 DB)]
        DB2[(Tenant 2 DB)]
        DB3[(Tenant 3 DB)]
    end

    subgraph Option_2["Option 2: Schema per Tenant"]
        DB_Main[(Main DB)]
        S1[Schema: tenant_1]
        S2[Schema: tenant_2]
        S3[Schema: tenant_3]
        DB_Main --> S1
        DB_Main --> S2
        DB_Main --> S3
    end

    subgraph Option_3["Option 3: Row-Level Security"]
        DB_Shared[(Shared DB)]
        RLS[PostgreSQL RLS]
        DB_Shared --> RLS
    end
```

| Strategy | Isolation Level | Cost | Cross-Tenant Query | Recommendation |
|----------|-----------------|------|-------------------|----------------|
| Database per Tenant | Highest | High | Complex | Enterprise customers |
| Schema per Tenant | Medium | Medium | Moderate | Mid-size deployments |
| Row-Level (RLS) | Adequate | Low | Simple | **Recommended (SmartAdmin)** |

### 2.2 Recommended: Row-Level Security with PostgreSQL RLS

**PostgreSQL RLS Policy Implementation**:

```sql
-- Enable RLS on player table
ALTER TABLE t_player ENABLE ROW LEVEL SECURITY;

-- Create policy for tenant isolation
CREATE POLICY tenant_isolation_policy ON t_player
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

-- Create policy for brand-level access
CREATE POLICY brand_access_policy ON t_player
    FOR SELECT
    USING (
        brand_id = current_setting('app.current_brand_id')::BIGINT
        OR current_setting('app.is_super_admin')::BOOLEAN = TRUE
    );
```

**Session Context Setup**:

```sql
-- Set tenant context at connection start
SET app.current_tenant_id = '789';
SET app.current_brand_id = '10';
SET app.is_super_admin = 'FALSE';
```

---

## 3. API Tenant Context Propagation

### 3.1 JWT Token Structure

```json
{
  "user_id": 12345,
  "role": "tenant_admin",
  "tenant_id": 789,
  "brand_id": 10,
  "permissions": ["player:read", "player:write", "report:read"],
  "exp": 1706356800,
  "iat": 1706270400
}
```

### 3.2 Tenant Context Holder (ThreadLocal)

```java
/**
 * SmartAdmin Multi-Tenant Context Holder
 * Thread-safe tenant context propagation
 */
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    public static TenantContext getContext() {
        return CONTEXT.get();
    }

    public static Long getTenantId() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    public static Long getBrandId() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getBrandId() : null;
    }

    public static boolean isSuperAdmin() {
        TenantContext ctx = CONTEXT.get();
        return ctx != null && ctx.isSuperAdmin();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

@Data
@Builder
public class TenantContext {
    private Long tenantId;
    private Long brandId;
    private boolean superAdmin;
    private Long impersonatedTenantId; // For admin impersonation
}
```

### 3.3 Tenant Filter Interceptor

```java
/**
 * SmartAdmin Tenant Interceptor
 * Extracts tenant context from JWT and sets ThreadLocal
 */
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        String token = extractToken(request);
        if (token != null) {
            JwtPayload payload = jwtService.parseToken(token);

            TenantContext context = TenantContext.builder()
                .tenantId(payload.getTenantId())
                .brandId(payload.getBrandId())
                .superAdmin("super_admin".equals(payload.getRole()))
                .build();

            // Check for impersonation header
            String impersonatedId = request.getHeader("X-Impersonate-Tenant");
            if (impersonatedId != null && context.isSuperAdmin()) {
                context.setImpersonatedTenantId(Long.parseLong(impersonatedId));
            }

            TenantContextHolder.setContext(context);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler, Exception ex) {
        TenantContextHolder.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

---

## 4. MyBatis-Plus Tenant Plugin

### 4.1 Tenant Line Handler Configuration

```java
/**
 * SmartAdmin MyBatis-Plus Multi-Tenant Configuration
 */
@Configuration
public class MybatisPlusTenantConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Tenant Line Interceptor - auto-inject tenant_id
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
            new TenantLineHandler() {

                @Override
                public Expression getTenantId() {
                    Long tenantId = TenantContextHolder.getTenantId();
                    if (tenantId == null) {
                        throw new TenantNotFoundException("Tenant context not set");
                    }
                    return new LongValue(tenantId);
                }

                @Override
                public String getTenantIdColumn() {
                    return "tenant_id";
                }

                @Override
                public boolean ignoreTable(String tableName) {
                    // Global tables that don't need tenant filtering
                    return GLOBAL_TABLES.contains(tableName);
                }
            }
        ));

        // Pagination interceptor
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }

    private static final Set<String> GLOBAL_TABLES = Set.of(
        "t_game_provider",      // Global game provider list
        "t_currency",           // Currency reference
        "t_country",            // Country reference
        "t_system_config"       // Global system configuration
    );
}
```

### 4.2 Entity Base Class

```java
/**
 * Base entity with tenant_id for multi-tenant tables
 */
@Data
public abstract class TenantBaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Tenant identifier - auto-filled by MyBatis-Plus interceptor
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * Brand identifier for aggregation queries
     */
    @TableField(fill = FieldFill.INSERT)
    private Long brandId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
```

---

## 5. Impersonation Implementation

### 5.1 Admin Impersonation Flow

```mermaid
sequenceDiagram
    participant SA as Super Admin
    participant API as API Gateway
    participant Auth as Auth Service
    participant Backend as Backend Service
    participant DB as Database

    SA->>API: POST /admin/impersonate<br/>{targetTenantId: 789}
    API->>Auth: Validate super admin role
    Auth-->>API: Authorized
    API->>Auth: Generate impersonation token
    Auth-->>API: JWT with impersonated_tenant_id
    API-->>SA: Set-Cookie: impersonation_token

    SA->>API: GET /tenant/players<br/>Cookie: impersonation_token
    API->>Backend: Forward with X-Impersonate-Tenant: 789
    Backend->>DB: Query with tenant_id = 789
    DB-->>Backend: Tenant 789 data
    Backend-->>SA: Player list (Tenant 789)
```

### 5.2 Impersonation Service

```java
/**
 * SmartAdmin Impersonation Service
 * Allows Super Admin to operate as any Tenant Admin
 */
@Service
@RequiredArgsConstructor
public class ImpersonationService {

    private final TenantDao tenantDao;
    private final AuditLogManager auditLogManager;

    /**
     * Start impersonation session
     * @param targetTenantId Tenant to impersonate
     * @return Impersonation session token
     */
    public String startImpersonation(Long targetTenantId) {
        TenantContext ctx = TenantContextHolder.getContext();

        if (!ctx.isSuperAdmin()) {
            throw new UnauthorizedException("Only Super Admin can impersonate");
        }

        // Verify target tenant exists
        TenantEntity tenant = tenantDao.selectById(targetTenantId);
        if (tenant == null) {
            throw new TenantNotFoundException("Tenant not found: " + targetTenantId);
        }

        // Log impersonation start
        auditLogManager.log(AuditLog.builder()
            .action("IMPERSONATION_START")
            .userId(ctx.getUserId())
            .targetTenantId(targetTenantId)
            .build());

        // Update context
        ctx.setImpersonatedTenantId(targetTenantId);
        TenantContextHolder.setContext(ctx);

        return generateImpersonationToken(ctx);
    }

    /**
     * End impersonation session
     */
    public void stopImpersonation() {
        TenantContext ctx = TenantContextHolder.getContext();

        if (ctx.getImpersonatedTenantId() != null) {
            auditLogManager.log(AuditLog.builder()
                .action("IMPERSONATION_END")
                .userId(ctx.getUserId())
                .targetTenantId(ctx.getImpersonatedTenantId())
                .build());

            ctx.setImpersonatedTenantId(null);
            TenantContextHolder.setContext(ctx);
        }
    }
}
```

---

## 6. Brand-Level Aggregation Queries

### 6.1 Cross-Tenant Report Query

```java
/**
 * Brand-level financial report service
 */
@Service
@RequiredArgsConstructor
public class BrandReportService {

    private final TransactionDao transactionDao;

    /**
     * Get aggregated financial report for all tenants under a brand
     * Bypasses tenant filter for aggregation
     */
    @TenantIgnore // Custom annotation to bypass tenant filter
    public BrandFinancialReportVO getBrandReport(Long brandId,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        // Verify brand access
        TenantContext ctx = TenantContextHolder.getContext();
        if (!ctx.isSuperAdmin() && !brandId.equals(ctx.getBrandId())) {
            throw new UnauthorizedException("No access to brand: " + brandId);
        }

        // Aggregate across all tenants in brand
        List<TenantFinancialSummary> summaries = transactionDao.selectBrandSummary(
            brandId, startDate, endDate);

        return BrandFinancialReportVO.builder()
            .brandId(brandId)
            .startDate(startDate)
            .endDate(endDate)
            .tenantSummaries(summaries)
            .totalDeposit(summaries.stream()
                .map(TenantFinancialSummary::getTotalDeposit)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalWithdrawal(summaries.stream()
                .map(TenantFinancialSummary::getTotalWithdrawal)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalGGR(summaries.stream()
                .map(TenantFinancialSummary::getGgr)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .build();
    }
}
```

### 6.2 SQL for Brand Aggregation

```sql
-- Brand-level financial summary
SELECT
    t.tenant_id,
    t.tenant_name,
    COALESCE(SUM(CASE WHEN tx.type = 'DEPOSIT' THEN tx.amount END), 0) as total_deposit,
    COALESCE(SUM(CASE WHEN tx.type = 'WITHDRAWAL' THEN tx.amount END), 0) as total_withdrawal,
    COALESCE(SUM(CASE WHEN tx.type = 'BET' THEN tx.amount END), 0) as total_bet,
    COALESCE(SUM(CASE WHEN tx.type = 'WIN' THEN tx.amount END), 0) as total_win,
    COALESCE(SUM(CASE WHEN tx.type = 'BET' THEN tx.amount END), 0)
        - COALESCE(SUM(CASE WHEN tx.type = 'WIN' THEN tx.amount END), 0) as ggr
FROM t_tenant t
LEFT JOIN t_transaction tx ON t.id = tx.tenant_id
    AND tx.create_time BETWEEN :startDate AND :endDate
    AND tx.deleted = FALSE
WHERE t.brand_id = :brandId
    AND t.deleted = FALSE
GROUP BY t.tenant_id, t.tenant_name
ORDER BY t.tenant_name;
```

---

## 7. Tenant Data Migration

### 7.1 Cross-Brand Transfer Process

```mermaid
stateDiagram-v2
    [*] --> ValidationPhase

    ValidationPhase --> PreparationPhase: All checks passed
    ValidationPhase --> [*]: Validation failed

    PreparationPhase --> MigrationPhase: Backups complete

    MigrationPhase --> VerificationPhase: Data transferred

    VerificationPhase --> CompletionPhase: Integrity verified
    VerificationPhase --> RollbackPhase: Verification failed

    RollbackPhase --> [*]: Rollback complete
    CompletionPhase --> [*]: Migration complete

    note right of ValidationPhase
        Check data integrity
        Verify target brand capacity
        Confirm regulatory compliance
    end note

    note right of MigrationPhase
        Update brand_id references
        Transfer configuration
        Migrate player data
    end note
```

### 7.2 Migration Service

```java
/**
 * Tenant migration service for cross-brand transfers
 */
@Service
@RequiredArgsConstructor
public class TenantMigrationManager {

    private final TenantDao tenantDao;
    private final PlayerDao playerDao;
    private final TransactionDao transactionDao;
    private final AuditLogManager auditLogManager;

    /**
     * Migrate tenant from one brand to another
     * Transaction spans multiple tables
     */
    @Transactional(rollbackFor = Throwable.class)
    public MigrationResultVO migrateTenant(Long tenantId,
                                           Long sourceBrandId,
                                           Long targetBrandId) {
        // 1. Validate migration
        validateMigration(tenantId, sourceBrandId, targetBrandId);

        // 2. Create backup checkpoint
        String checkpointId = createBackupCheckpoint(tenantId);

        try {
            // 3. Update tenant brand reference
            int tenantUpdated = tenantDao.updateBrandId(tenantId, targetBrandId);

            // 4. Update all related records
            int playersUpdated = playerDao.updateBrandIdByTenant(tenantId, targetBrandId);
            int transactionsUpdated = transactionDao.updateBrandIdByTenant(tenantId, targetBrandId);

            // 5. Log migration
            auditLogManager.log(AuditLog.builder()
                .action("TENANT_MIGRATION")
                .tenantId(tenantId)
                .details(Map.of(
                    "sourceBrandId", sourceBrandId,
                    "targetBrandId", targetBrandId,
                    "playersUpdated", playersUpdated,
                    "transactionsUpdated", transactionsUpdated
                ))
                .build());

            return MigrationResultVO.builder()
                .success(true)
                .tenantId(tenantId)
                .targetBrandId(targetBrandId)
                .recordsUpdated(playersUpdated + transactionsUpdated)
                .build();

        } catch (Exception e) {
            // Rollback will be automatic due to @Transactional
            throw new MigrationException("Migration failed: " + e.getMessage(), e);
        }
    }
}
```

---

## 8. SmartAdmin Multi-Tenant Implementation

### 8.1 Module Structure

```
smartadmin-modules/
├── smartadmin-system/
│   └── src/main/java/net/lab1024/sa/system/
│       └── tenant/
│           ├── controller/
│           │   └── TenantController.java
│           ├── service/
│           │   └── TenantService.java
│           ├── manager/
│           │   ├── TenantManager.java          # @Transactional here
│           │   └── TenantMigrationManager.java
│           ├── dao/
│           │   └── TenantDao.java
│           └── domain/
│               ├── entity/TenantEntity.java
│               ├── form/TenantCreateForm.java
│               └── vo/TenantVO.java
```

### 8.2 Controller Layer

```java
/**
 * Tenant management API endpoints
 */
@RestController
@RequestMapping("/admin/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant Management")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/create")
    @SaCheckPermission("tenant:create")
    @Operation(summary = "Create new tenant")
    public ResponseDTO<Long> createTenant(@Valid @RequestBody TenantCreateForm form) {
        return tenantService.createTenant(form);
    }

    @GetMapping("/list")
    @SaCheckPermission("tenant:read")
    @Operation(summary = "List tenants with pagination")
    public ResponseDTO<PageResult<TenantVO>> listTenants(TenantQueryForm form) {
        return tenantService.queryTenants(form);
    }

    @PostMapping("/impersonate/{tenantId}")
    @SaCheckPermission("tenant:impersonate")
    @Operation(summary = "Start impersonation session")
    public ResponseDTO<String> impersonate(@PathVariable Long tenantId) {
        return tenantService.startImpersonation(tenantId);
    }
}
```

### 8.3 Service Layer (with Vavr Option)

```java
/**
 * Tenant service - uses io.vavr.control.Option (SmartAdmin standard)
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantDao tenantDao;
    private final TenantManager tenantManager;
    private final ImpersonationService impersonationService;

    public ResponseDTO<Long> createTenant(TenantCreateForm form) {
        // Validate unique domain
        Option<TenantEntity> existing = Option.of(
            tenantDao.selectByDomain(form.getDomain()));

        if (existing.isDefined()) {
            return ResponseDTO.userErrorParam("Domain already exists: " + form.getDomain());
        }

        // Delegate to Manager for @Transactional operation
        Long tenantId = tenantManager.createTenant(form);
        return ResponseDTO.ok(tenantId);
    }

    public ResponseDTO<PageResult<TenantVO>> queryTenants(TenantQueryForm form) {
        Page<TenantEntity> page = SmartPageUtil.convert2PageQuery(form);

        // Apply brand filter based on context
        TenantContext ctx = TenantContextHolder.getContext();
        if (!ctx.isSuperAdmin()) {
            form.setBrandId(ctx.getBrandId());
        }

        Page<TenantEntity> result = tenantDao.selectPage(page,
            new LambdaQueryWrapper<TenantEntity>()
                .eq(form.getBrandId() != null, TenantEntity::getBrandId, form.getBrandId())
                .like(form.getName() != null, TenantEntity::getName, form.getName())
                .orderByDesc(TenantEntity::getCreateTime));

        PageResult<TenantVO> pageResult = SmartPageUtil.convert2PageResult(
            result, TenantVO.class);

        return ResponseDTO.ok(pageResult);
    }

    public ResponseDTO<String> startImpersonation(Long tenantId) {
        String token = impersonationService.startImpersonation(tenantId);
        return ResponseDTO.ok(token);
    }
}
```

---

## Related Documents

### Architecture References
- RBAC Architecture *(planned)* - Permission system implementation
- Audit Log Architecture *(planned)* - Tenant operation auditing

### Business Requirements
- [Multi-Tenant Requirements](../../requirements/06_Governance_Licensing/Multi_Tenant_Requirements.md) - Business requirements view

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Backend Team
