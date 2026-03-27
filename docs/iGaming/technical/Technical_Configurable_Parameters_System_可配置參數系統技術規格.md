---
title: "Ch0: 可配置參數系統技術規格"
part: technical
module: platform-configuration
version: v1.0
created: 2026-03-25
---

# 第 0 章：可配置參數系統技術規格

## T-CFG.1 模組概述

The Configurable Parameters System (CPS) is a core platform enabler that abstracts all hardcoded business rules into database-driven, dynamically configurable values. The system consolidates 845+ parameters scattered across 16 chapters of platform requirements into a unified registry supporting three-tier geographic hierarchy (Global → Brand → Jurisdiction) with complete audit trails, hot-reload capabilities, and role-based approval workflows.

**Key Capabilities:**
- **Dynamic Resolution**: Three-tier hierarchy resolution (jurisdiction > brand > global) with strict-rule-applies pattern for compliance-driven parameters
- **Zero-Downtime Updates**: Hot-reload propagation via Redis Pub/Sub achieving ≤5 second cache invalidation across all service instances
- **Complete Audit Trail**: Immutable change log capturing who/what/when/why for all parameter mutations (create, update, delete, approve, reject)
- **Operational Control**: Non-technical staff can modify configurations via admin UI without code deployment
- **Compliance Flexibility**: Support for jurisdiction-specific overrides with mandatory dual approval for sensitive parameters
- **High Performance**: Three-layer caching (L1 local/5s, L2 Redis/30s, L3 DB) with 99.9% cache hit target

**Owned By:** Platform Core Team
**Stakeholders:** Operations, Compliance, Risk, Finance, Product
**Service Name:** `config-service` (Java/Kotlin microservice)
**Port:** 8081 (gRPC), 9081 (HTTP Admin API)

---

## T-CFG.2 系統架構

### 2.1 Component Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                       Admin UI (React)                         │
│  - Parameter Browser (filter by chapter/type/scope)            │
│  - Parameter Editor (inline + bulk import/export)              │
│  - History Viewer & Diff Comparison                            │
│  - Approval Queue (Maker-Checker workflow)                     │
└────────────────┬─────────────────────────────────────────────┘
                 │ HTTP REST API
┌────────────────▼─────────────────────────────────────────────┐
│         Config Service (Java/Kotlin)                          │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ API Layer (Spring Boot)                                  │ │
│  │ - GET /api/v1/config/params                             │ │
│  │ - GET /api/v1/config/params/{key}                       │ │
│  │ - PUT /api/v1/config/params/{key}                       │ │
│  │ - POST /api/v1/config/params/{key}/override             │ │
│  │ - POST /api/v1/config/params/bulk-import                │ │
│  │ - GET /api/v1/config/params/{key}/history               │ │
│  └──────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Resolution Engine                                        │ │
│  │ - resolveConfig(key, brandId, jurisdiction)            │ │
│  │ - resolveComplianceConfig(key, brandId, jurisdiction)  │ │
│  │ - validateParameterValue(key, newValue)                 │ │
│  └──────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Caching Layer (Java HashMap + Redis)                   │ │
│  │ - L1: Local ConcurrentHashMap (5s TTL)                 │ │
│  │ - L2: Redis Distributed Cache (30s TTL)                │ │
│  │ - L3: PostgreSQL Database (authoritative)              │ │
│  │ - Invalidation: Redis Pub/Sub broadcast                │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────┬──────────────────────────┬──────────────────────┘
             │                          │
    ┌────────▼────────┐    ┌────────────▼──────────┐
    │ PostgreSQL 15   │    │  Redis Cluster 7.0    │
    │                 │    │                       │
    │ 3 tables:       │    │ - Config cache        │
    │ - definition    │    │ - Event pub/sub       │
    │ - value         │    │ - Session state       │
    │ - audit_log     │    │ - Rate limits         │
    └─────────────────┘    └───────────────────────┘
             ▲                         ▲
             └─────────────────┬───────┘
                               │
                    Used by all microservices
```

### 2.2 Service Interaction Flow

```
User (Admin) → Admin UI
                   ↓
            [Step 1] PUT /api/v1/config/params/{key}
                   ↓
            Config Service receives request
                   ↓
            [Step 2] Validate: Check RBAC, compliance_driven flag, data constraints
                   ↓
            [Step 3] Create DRAFT change proposal
                   ↓
            [Step 4] If compliance_driven=true, emit PENDING_APPROVAL event
                        Else: Auto-approve and proceed to ACTIVE
                   ↓
            [Step 5] Update config_param_value (is_active=false for old, true for new)
                   ↓
            [Step 6] Write audit_log entry (CREATE/UPDATE)
                   ↓
            [Step 7] Invalidate L1+L2 cache, publish Redis event
                   ↓
            [Step 8] All service instances receive Redis message
                   ↓
            [Step 9] Flush local cache, next read hits Redis/DB
                   ↓
            Clients (game engines, risk service, etc.) fetch fresh config
```

---

## T-CFG.3 資料庫 Schema

### 3.1 Table: config_param_definition

Static parameter metadata — populated by migration scripts, read-only at runtime (updated only during platform releases).

```sql
CREATE TABLE config_param_definition (
    param_key               VARCHAR(128) PRIMARY KEY,
    chapter                 VARCHAR(8) NOT NULL,           -- Ch1, Ch2, ..., Ch16
    section                 VARCHAR(16),                   -- §2.6, §3.1, etc.
    display_name_en         VARCHAR(256) NOT NULL,         -- English name
    display_name_zh         VARCHAR(256) NOT NULL,         -- Chinese name
    description_en          TEXT,                          -- Detailed description (English)
    description_zh          TEXT,                          -- Detailed description (Chinese)
    data_type               VARCHAR(20) NOT NULL,          -- INTEGER, DECIMAL, DURATION, PERCENTAGE, AMOUNT, BOOLEAN, ENUM
    unit                    VARCHAR(32),                   -- minutes, hours, days, USD, %, count, etc.
    default_value           VARCHAR(256) NOT NULL,         -- Global default, must pass validation
    min_value               VARCHAR(256),                  -- Min constraint (null = unbounded)
    max_value               VARCHAR(256),                  -- Max constraint (null = unbounded)
    allowed_enum_values     TEXT,                          -- JSON array for ENUM type: ["DRAFT","ACTIVE","REJECTED"]
    direction               VARCHAR(30) NOT NULL DEFAULT 'NEUTRAL',
                                                           -- LOWER_IS_STRICTER: smaller value = more restrictive
                                                           -- HIGHER_IS_STRICTER: larger value = more restrictive
                                                           -- NEUTRAL: standard override hierarchy
    compliance_driven       BOOLEAN NOT NULL DEFAULT FALSE, -- Trigger dual approval + strictest-rule logic
    is_sensitive            BOOLEAN NOT NULL DEFAULT FALSE, -- Sensitive params (financial thresholds, limits)
    require_deployment      BOOLEAN NOT NULL DEFAULT FALSE, -- If true, change requires service restart
    allow_scope_global      BOOLEAN NOT NULL DEFAULT TRUE,  -- Can be overridden at global level
    allow_scope_brand       BOOLEAN NOT NULL DEFAULT TRUE,  -- Can be overridden at brand level
    allow_scope_jurisdiction BOOLEAN NOT NULL DEFAULT TRUE, -- Can be overridden at jurisdiction level
    category                VARCHAR(20) NOT NULL,          -- PERCENTAGE, AMOUNT, DURATION, COUNT, THRESHOLD
    change_frequency_hint   VARCHAR(20) DEFAULT 'RARELY',  -- RARELY, OCCASIONALLY, FREQUENTLY (for UI sorting)
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_chapter (chapter),
    INDEX idx_category (category),
    INDEX idx_compliance (compliance_driven),
    INDEX idx_sensitive (is_sensitive)
);

-- Example data:
INSERT INTO config_param_definition VALUES (
    'ch1.kyc.l0_daily_withdrawal',
    'Ch1',
    '§1.2.1',
    'KYC Level 0 Daily Withdrawal Limit',
    'KYC 0級日提款限額',
    'Maximum amount a KYC Level 0 user can withdraw per day. Regulatory minimum for unverified accounts.',
    '未驗證帳戶每日提款額度上限。根據監管要求設置。',
    'AMOUNT',
    'USD',
    '0',
    '0',
    '100000',
    NULL,
    'NEUTRAL',
    TRUE,  -- compliance_driven
    TRUE,  -- is_sensitive
    FALSE,
    TRUE,
    FALSE, -- cannot override at brand (regulatory constraint)
    TRUE,
    'AMOUNT',
    'RARELY',
    '2026-03-25',
    '2026-03-25'
);
```

### 3.2 Table: config_param_value

Dynamic parameter values — stores all overrides at global, brand, and jurisdiction scopes. Single active value per (param_key, scope, scope_id) at any point in time.

```sql
CREATE TABLE config_param_value (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_key               VARCHAR(128) NOT NULL,
    scope                   VARCHAR(20) NOT NULL,         -- GLOBAL, BRAND, JURISDICTION
    scope_id                VARCHAR(64),                  -- NULL for GLOBAL, brand_id for BRAND, jurisdiction_code for JURISDICTION
    value                   VARCHAR(256) NOT NULL,        -- Must validate against config_param_definition constraints
    description             TEXT,                         -- Why this override exists (e.g., "MGA liquidity requirement")
    effective_from          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_until         TIMESTAMP,                    -- NULL = permanent; timestamp = scheduled sunset
    is_active               BOOLEAN NOT NULL DEFAULT TRUE, -- Active = currently in use; FALSE = superseded by newer entry
    approval_status         VARCHAR(20) DEFAULT 'APPROVED', -- DRAFT, PENDING_APPROVAL, APPROVED, REJECTED
    created_by              VARCHAR(128) NOT NULL,        -- User ID of creator
    approved_by             VARCHAR(128),                 -- User ID of approver (Maker-Checker)
    approval_comments       TEXT,                         -- Approval reason or rejection reason
    approval_date           TIMESTAMP,                    -- When approved/rejected
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (param_key) REFERENCES config_param_definition(param_key),
    UNIQUE KEY uk_param_scope_active (param_key, scope, scope_id, is_active),
    -- Ensures only one active record per (param_key, scope, scope_id)
    INDEX idx_param_key (param_key),
    INDEX idx_scope (scope, scope_id),
    INDEX idx_active (is_active),
    INDEX idx_approval_status (approval_status),
    INDEX idx_effective (effective_from, effective_until),
    INDEX idx_created_by (created_by)
);

-- Example data:
INSERT INTO config_param_value (
    param_key, scope, scope_id, value, description,
    is_active, approval_status, created_by, approved_by, created_at
) VALUES (
    'ch1.kyc.l0_daily_withdrawal',
    'JURISDICTION',
    'UKGC',
    '0',  -- Stricter than global default
    'UKGC requires £0 limit for unverified accounts',
    TRUE,
    'APPROVED',
    'system_migration',
    'compliance_team',
    '2026-03-25'
);

INSERT INTO config_param_value (
    param_key, scope, scope_id, value, description,
    is_active, approval_status, created_by, approved_by, created_at
) VALUES (
    'ch1.vip.silver_upgrade',
    'BRAND',
    'BRAND_ASIA',
    '5000',  -- Lower threshold for Asian market
    'Localization: Asian market prefers lower VIP thresholds',
    TRUE,
    'APPROVED',
    'brand_ops_asia',
    'vp_brand_asia',
    '2026-03-10'
);
```

### 3.3 Table: config_audit_log

Immutable audit trail — write-once append log. No updates/deletes allowed (enforced via application logic and trigger constraints).

```sql
CREATE TABLE config_audit_log (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_key               VARCHAR(128) NOT NULL,
    scope                   VARCHAR(20) NOT NULL,
    scope_id                VARCHAR(64),
    old_value               VARCHAR(256),                 -- NULL if CREATE operation
    new_value               VARCHAR(256),                 -- NULL if DELETE operation
    change_type             VARCHAR(20) NOT NULL,         -- CREATE, UPDATE, DELETE, APPROVE, REJECT
    changed_by              VARCHAR(128) NOT NULL,        -- User ID
    change_reason           TEXT,                         -- Business reason for change
    approval_reason         TEXT,                         -- Why approved/rejected (for APPROVE/REJECT only)
    ip_address              VARCHAR(45),                  -- IPv4 or IPv6
    user_agent              VARCHAR(512),                 -- Browser/client info
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_param_time (param_key, created_at),
    INDEX idx_changed_by (changed_by, created_at),
    INDEX idx_change_type (change_type, created_at),
    INDEX idx_scope_time (scope, scope_id, created_at)
);

-- Example audit entries:
INSERT INTO config_audit_log (
    param_key, scope, scope_id, old_value, new_value, change_type,
    changed_by, change_reason, ip_address, created_at
) VALUES (
    'ch1.kyc.l0_daily_withdrawal',
    'JURISDICTION',
    'UKGC',
    NULL,
    '0',
    'CREATE',
    'compliance_team',
    'Initial UKGC jurisdiction override per licensing requirement',
    '192.168.1.100',
    '2026-03-25'
);

INSERT INTO config_audit_log (
    param_key, scope, scope_id, old_value, new_value, change_type,
    changed_by, change_reason, approval_reason, created_at
) VALUES (
    'ch1.kyc.l2_daily_deposit',
    'BRAND',
    'BRAND_ASIA',
    '50000',
    '30000',
    'APPROVE',
    'operations_lead',
    'Temporary deposit cap reduction for risk mitigation',
    'Approved pending 30-day review (Risk: high volatile segment)',
    '2026-03-24'
);
```

### 3.4 DDL Summary & Indexes

```sql
-- Create extension for enum support (PostgreSQL 13+)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Primary keys auto-increment
-- Constraints: FK, UNIQUE, CHECK enforced at DB level
-- TTL: No automatic aging; manual archive per retention policy (Ch1)

-- Additional indexes for query performance:
CREATE INDEX idx_config_param_definition_chapter_category
  ON config_param_definition(chapter, category);

CREATE INDEX idx_config_param_value_param_effective
  ON config_param_value(param_key, effective_from DESC, effective_until DESC)
  WHERE is_active = TRUE;

CREATE INDEX idx_config_audit_log_scope_time
  ON config_audit_log(scope, scope_id, created_at DESC);

-- Materialized view for quick override resolution:
CREATE MATERIALIZED VIEW mv_config_active_overrides AS
SELECT
    param_key,
    scope,
    scope_id,
    value,
    effective_from,
    effective_until,
    is_active
FROM config_param_value
WHERE is_active = TRUE
  AND (effective_until IS NULL OR effective_until > CURRENT_TIMESTAMP)
  AND approval_status = 'APPROVED';

CREATE INDEX idx_mv_config_active_param ON mv_config_active_overrides(param_key);
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_config_active_overrides;  -- Refresh every 30s
```

### 3.5 Migration Strategy

**Phase 1: Seed Definition Table**
Bulk load all 845+ parameters into `config_param_definition` from PRD export (CSV).
Validation: Ensure every param_key is unique, default_value passes constraints, required fields populated.

**Phase 2: Populate Initial Values**
Insert current hardcoded defaults into `config_param_value` with scope=GLOBAL, is_active=TRUE.
Source: Extract current production values from codebase constants.

**Phase 3: Add Brand/Jurisdiction Overrides**
For known deviations (UKGC, MGA, brand-specific thresholds), create override entries.
Approval status: APPROVED (pre-approved by compliance before deployment).

**Phase 4: Validation & Dry-Run**
Test resolution engine on 100% of production parameter space.
Compare resolved values with current hardcoded values → must be 100% match.
Load test: Resolution latency P99 ≤ 10ms.

**Phase 5: Deployment**
1. Deploy config-service to production (canary: 10% traffic)
2. Verify Redis cache is warm
3. Gradually increase canary traffic to 100%
4. Monitor: cache hit rates, resolution latency, error rates
5. Once stable, cutover all services to call resolveConfig() instead of hardcoded constants

**Rollback Plan**
If critical issues:
1. Revert all services to cached hardcoded values (safety fallback in code)
2. Pause all config changes via circuit breaker
3. Investigate database state and resolution engine
4. Fix and re-deploy config-service
5. Resume config change propagation once validated

---

## T-CFG.4 三層覆蓋解析引擎

### 4.1 Java/Kotlin Resolution Implementation

```kotlin
// ConfigService.kt - Core resolution engine

interface IConfigResolver {
    fun resolveConfig(
        paramKey: String,
        brandId: String?,
        jurisdictionCode: String?
    ): ConfigValue?

    fun resolveComplianceConfig(
        paramKey: String,
        brandId: String?,
        jurisdictionCode: String?
    ): ConfigValue?
}

data class ConfigValue(
    val paramKey: String,
    val value: String,
    val scope: ConfigScope,
    val scopeId: String?,
    val dataType: DataType,
    val unit: String?,
    val resolvedAt: Instant,
    val cacheLayer: CacheLayer  // L1, L2, or L3 (database)
)

enum class ConfigScope { GLOBAL, BRAND, JURISDICTION }
enum class CacheLayer { L1_LOCAL, L2_REDIS, L3_DATABASE }
enum class DataType { INTEGER, DECIMAL, DURATION, PERCENTAGE, AMOUNT, BOOLEAN, ENUM }

@Service
class ConfigResolver(
    private val repository: ConfigParamRepository,
    private val cache: ConfigCache,
    private val metrics: ConfigMetrics,
    private val logger: Logger
) : IConfigResolver {

    /**
     * Standard resolution: jurisdiction > brand > global (first match wins)
     * Used for non-compliance-driven parameters.
     */
    override fun resolveConfig(
        paramKey: String,
        brandId: String?,
        jurisdictionCode: String?
    ): ConfigValue? {
        val startTime = System.nanoTime()

        // Step 1: Check L1 local cache
        val cachedValue = cache.getFromL1(paramKey, brandId, jurisdictionCode)
        if (cachedValue != null) {
            metrics.recordCacheHit(CacheLayer.L1_LOCAL)
            metrics.recordResolutionLatency(System.nanoTime() - startTime)
            return cachedValue
        }

        // Step 2: Try jurisdiction-level override
        if (jurisdictionCode != null) {
            val jurisdictionValue = cache.getFromL2(
                paramKey,
                ConfigScope.JURISDICTION,
                jurisdictionCode
            ) ?: repository.findActiveValue(
                paramKey,
                ConfigScope.JURISDICTION,
                jurisdictionCode
            )

            if (jurisdictionValue != null) {
                val result = ConfigValue(
                    paramKey = paramKey,
                    value = jurisdictionValue.value,
                    scope = ConfigScope.JURISDICTION,
                    scopeId = jurisdictionCode,
                    dataType = jurisdictionValue.dataType,
                    unit = jurisdictionValue.unit,
                    resolvedAt = Instant.now(),
                    cacheLayer = if (cachedValue != null) CacheLayer.L2_REDIS else CacheLayer.L3_DATABASE
                )
                cache.putToL1(result)
                cache.putToL2(result)
                metrics.recordResolutionLatency(System.nanoTime() - startTime)
                return result
            }
        }

        // Step 3: Try brand-level override
        if (brandId != null) {
            val brandValue = cache.getFromL2(
                paramKey,
                ConfigScope.BRAND,
                brandId
            ) ?: repository.findActiveValue(
                paramKey,
                ConfigScope.BRAND,
                brandId
            )

            if (brandValue != null) {
                val result = ConfigValue(
                    paramKey = paramKey,
                    value = brandValue.value,
                    scope = ConfigScope.BRAND,
                    scopeId = brandId,
                    dataType = brandValue.dataType,
                    unit = brandValue.unit,
                    resolvedAt = Instant.now(),
                    cacheLayer = CacheLayer.L3_DATABASE
                )
                cache.putToL1(result)
                cache.putToL2(result)
                metrics.recordResolutionLatency(System.nanoTime() - startTime)
                return result
            }
        }

        // Step 4: Fall back to global default
        val globalValue = cache.getFromL2(
            paramKey,
            ConfigScope.GLOBAL,
            null
        ) ?: repository.findActiveValue(
            paramKey,
            ConfigScope.GLOBAL,
            null
        )

        if (globalValue != null) {
            val result = ConfigValue(
                paramKey = paramKey,
                value = globalValue.value,
                scope = ConfigScope.GLOBAL,
                scopeId = null,
                dataType = globalValue.dataType,
                unit = globalValue.unit,
                resolvedAt = Instant.now(),
                cacheLayer = CacheLayer.L3_DATABASE
            )
            cache.putToL1(result)
            cache.putToL2(result)
            metrics.recordResolutionLatency(System.nanoTime() - startTime)
            return result
        }

        // Should never reach here in production (all params must have global default)
        logger.error("No value found for param: $paramKey")
        metrics.recordResolutionError(paramKey)
        return null
    }

    /**
     * Compliance resolution: Strictest-rule-applies pattern
     * For compliance_driven = true parameters, resolve all applicable overrides
     * and select the strictest based on the parameter direction.
     */
    override fun resolveComplianceConfig(
        paramKey: String,
        brandId: String?,
        jurisdictionCode: String?
    ): ConfigValue? {
        val startTime = System.nanoTime()

        // Fetch parameter definition to get direction
        val paramDef = repository.getParamDefinition(paramKey)
            ?: throw IllegalArgumentException("Param definition not found: $paramKey")

        if (!paramDef.complianceDriven) {
            logger.warn("resolveComplianceConfig called on non-compliance param: $paramKey")
            return resolveConfig(paramKey, brandId, jurisdictionCode)
        }

        val candidates = mutableListOf<ConfigValue>()

        // Collect global default (always required)
        repository.findActiveValue(paramKey, ConfigScope.GLOBAL, null)?.let {
            candidates.add(
                ConfigValue(
                    paramKey, it.value, ConfigScope.GLOBAL, null,
                    it.dataType, it.unit, Instant.now(), CacheLayer.L3_DATABASE
                )
            )
        }

        // Collect brand override if applicable
        if (brandId != null && paramDef.allowScopeBrand) {
            repository.findActiveValue(paramKey, ConfigScope.BRAND, brandId)?.let {
                candidates.add(
                    ConfigValue(
                        paramKey, it.value, ConfigScope.BRAND, brandId,
                        it.dataType, it.unit, Instant.now(), CacheLayer.L3_DATABASE
                    )
                )
            }
        }

        // Collect jurisdiction override if applicable
        if (jurisdictionCode != null && paramDef.allowScopeJurisdiction) {
            repository.findActiveValue(paramKey, ConfigScope.JURISDICTION, jurisdictionCode)?.let {
                candidates.add(
                    ConfigValue(
                        paramKey, it.value, ConfigScope.JURISDICTION, jurisdictionCode,
                        it.dataType, it.unit, Instant.now(), CacheLayer.L3_DATABASE
                    )
                )
            }
        }

        if (candidates.isEmpty()) {
            logger.error("No values found for compliance param: $paramKey")
            return null
        }

        // Select strictest value based on direction
        val selectedValue = when (paramDef.direction) {
            ComplianceDirection.LOWER_IS_STRICTER -> {
                // Examples: max_bet, daily_withdrawal_limit, etc.
                // Smaller number = stricter (more protective)
                candidates.minByOrNull { it.value.toDoubleOrNull() ?: Double.MAX_VALUE }
                    ?: candidates.first()
            }
            ComplianceDirection.HIGHER_IS_STRICTER -> {
                // Examples: kyc_retention_years, mfa_reset_cooldown_hours, etc.
                // Larger number = stricter (more protective)
                candidates.maxByOrNull { it.value.toDoubleOrNull() ?: Double.MIN_VALUE }
                    ?: candidates.first()
            }
            ComplianceDirection.NEUTRAL -> {
                // Fallback to standard hierarchy (jurisdiction > brand > global)
                candidates.find { it.scope == ConfigScope.JURISDICTION }
                    ?: candidates.find { it.scope == ConfigScope.BRAND }
                    ?: candidates.find { it.scope == ConfigScope.GLOBAL }
                    ?: candidates.first()
            }
        }

        cache.putToL1(selectedValue)
        cache.putToL2(selectedValue)
        metrics.recordResolutionLatency(System.nanoTime() - startTime)
        return selectedValue
    }
}

enum class ComplianceDirection { LOWER_IS_STRICTER, HIGHER_IS_STRICTER, NEUTRAL }
```

### 4.2 Unit Tests

```kotlin
// ConfigResolverTest.kt

@SpringBootTest
class ConfigResolverTest(
    private val resolver: ConfigResolver,
    private val repository: ConfigParamRepository,
    private val cache: ConfigCache
) {

    @Before
    fun setup() {
        cache.clearAll()
        // Seed test data
        seedParamDefinitions()
        seedParamValues()
    }

    @Test
    fun `resolve jurisdiction override wins over brand and global`() {
        // Given: slots_max_bet with global=$10, brand_a=$8, UKGC=£5
        // When: resolve for BrandA in UKGC jurisdiction
        val result = resolver.resolveConfig(
            "ch2.slots.max_bet",
            brandId = "BRAND_A",
            jurisdictionCode = "UKGC"
        )
        // Then: should return UKGC override (£5)
        assertEquals(ConfigScope.JURISDICTION, result?.scope)
        assertEquals("5", result?.value)
    }

    @Test
    fun `resolve brand override wins over global when jurisdiction not set`() {
        // Given: welcome_bonus_cap with global=$500, brand_a=$300
        val result = resolver.resolveConfig(
            "ch1.bonus.welcome_cap",
            brandId = "BRAND_A",
            jurisdictionCode = null
        )
        // Then: should return brand override ($300)
        assertEquals(ConfigScope.BRAND, result?.scope)
        assertEquals("300", result?.value)
    }

    @Test
    fun `resolve compliance param applies stricter rule - LOWER_IS_STRICTER`() {
        // Given: kyc_l0_daily_withdrawal (LOWER_IS_STRICTER)
        //        global=$0, brand_x=$100, UKGC=$0
        // When: resolve for BrandX in UKGC
        val result = resolver.resolveComplianceConfig(
            "ch1.kyc.l0_daily_withdrawal",
            brandId = "BRAND_X",
            jurisdictionCode = "UKGC"
        )
        // Then: should return minimum value ($0, either global or UKGC)
        assertEquals("0", result?.value)
    }

    @Test
    fun `resolve compliance param applies stricter rule - HIGHER_IS_STRICTER`() {
        // Given: kyc_retention_years (HIGHER_IS_STRICTER)
        //        global=5, brand_y=4, MGA=7
        // When: resolve for BrandY in MGA jurisdiction
        val result = resolver.resolveComplianceConfig(
            "ch1.retention.kyc_docs_years",
            brandId = "BRAND_Y",
            jurisdictionCode = "MGA"
        )
        // Then: should return maximum value (7 years from MGA)
        assertEquals("7", result?.value)
    }

    @Test
    fun `cache hit from L1 returns without DB query`() {
        // Given: parameter already in L1 cache
        val expected = ConfigValue(
            "ch2.wallet.round_timeout_hours", "2", ConfigScope.GLOBAL, null,
            DataType.DURATION, "hours", Instant.now(), CacheLayer.L1_LOCAL
        )
        cache.putToL1(expected)

        // When: resolve parameter
        val metrics = mockMetrics()
        val result = resolver.resolveConfig("ch2.wallet.round_timeout_hours", null, null)

        // Then: should return cached value and record L1 hit
        assertEquals(expected.value, result?.value)
        assertTrue(metrics.getCacheHitCount(CacheLayer.L1_LOCAL) > 0)
        verify(repository, never()).findActiveValue(any(), any(), any())
    }

    @Test
    fun `resolve with null brandId and jurisdictionCode returns global default`() {
        // Given: ch1.account.dormant_days with global default 90
        val result = resolver.resolveConfig(
            "ch1.account.dormant_days",
            brandId = null,
            jurisdictionCode = null
        )
        // Then: should return global default
        assertEquals(ConfigScope.GLOBAL, result?.scope)
        assertEquals("90", result?.value)
    }

    @Test
    fun `param validation rejects invalid values before storage`() {
        // Given: parameter with min=0, max=100
        // When: try to set value = 150
        val valid = repository.validateParameterValue(
            "ch1.vip.platinum_cashback_pct",
            "150"  // Exceeds max
        )
        // Then: validation should fail
        assertFalse(valid)
    }

    private fun seedParamDefinitions() {
        // Insert test parameter definitions
        repository.insertParamDefinition(
            ParamDefinition(
                paramKey = "ch2.slots.max_bet",
                chapter = "Ch2",
                displayNameEn = "Slots Max Bet",
                displayNameZh = "老虎機最大投注",
                dataType = DataType.AMOUNT,
                unit = "USD",
                defaultValue = "10",
                minValue = "0.01",
                maxValue = "1000",
                direction = ComplianceDirection.LOWER_IS_STRICTER,
                complianceDriven = true
            )
        )
        // ... more definitions
    }

    private fun seedParamValues() {
        // Insert test parameter values at different scopes
        repository.insertParamValue(
            ParamValue(
                paramKey = "ch2.slots.max_bet",
                scope = ConfigScope.GLOBAL,
                scopeId = null,
                value = "10",
                isActive = true,
                createdBy = "test_user"
            )
        )
        // ... more values at BRAND and JURISDICTION scopes
    }
}
```

---

## T-CFG.5 快取與 Hot-Reload

### 5.1 Three-Layer Caching Architecture

```
┌──────────────────────────────────────────┐
│ L1: Local JVM Cache (in-process)         │
│ - HashMap<String, ConfigValue>           │
│ - TTL: 5 seconds                         │
│ - Capacity: 10,000 entries (~50MB)      │
│ - Invalidation: Redis Pub/Sub broadcast  │
│ - Hit Rate Target: 90%+                  │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│ L2: Redis Distributed Cache              │
│ - Key: "config:{paramKey}:{scope}:{id}"  │
│ - TTL: 30 seconds                        │
│ - Capacity: 100,000 entries (cluster)   │
│ - Invalidation: Pattern UNLINK            │
│ - Hit Rate Target: 95%+                  │
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│ L3: PostgreSQL Database                  │
│ - Authoritative source                   │
│ - Query: SELECT ... WHERE is_active=TRUE │
│ - Latency: ~5-10ms P95                   │
│ - Consistency: ACID guaranteed           │
└──────────────────────────────────────────┘
```

### 5.2 Cache Invalidation Sequence

```
User updates ch1.vip.silver_upgrade for BRAND_ASIA

         Admin UI
            │
            ├─→ PUT /api/v1/config/params/ch1.vip.silver_upgrade
            │       body: { scope: "BRAND", scope_id: "BRAND_ASIA", value: "7500", ... }
            │
            └─→ Config Service
                 │
                 1. Validate parameter & permissions
                 │
                 2. Update config_param_value (is_active=TRUE, approval_status=APPROVED)
                 │
                 3. Write config_audit_log entry
                 │
                 4. Publish Redis event:
                 │   channel: "config:invalidate"
                 │   payload: {
                 │       paramKey: "ch1.vip.silver_upgrade",
                 │       scope: "BRAND",
                 │       scopeId: "BRAND_ASIA",
                 │       timestamp: 1711358400000
                 │   }
                 │
                 └─→ Redis Server
                      │
                      ├─→ Broadcast to ALL subscribed instances
                      │
                      └─→ Instance 1, 2, 3, ... receive message
                           │
                           ├─ Flush L1 cache entry:
                           │  cache.evict("config:ch1.vip.silver_upgrade:BRAND:BRAND_ASIA")
                           │
                           ├─ Flush L2 Redis entry:
                           │  redis.unlink("config:ch1.vip.silver_upgrade:BRAND:BRAND_ASIA")
                           │
                           └─ Next read hits database → reloads to L2 → reloads to L1
                               (Latency: ≤ 5 seconds typical, ≤ 100ms Redis call)
```

### 5.3 Cache Implementation (Java)

```java
// ConfigCache.java

@Component
public class ConfigCache {

    private static final int L1_CAPACITY = 10000;
    private static final long L1_TTL_SECONDS = 5;
    private static final long L2_TTL_SECONDS = 30;

    private final LoadingCache<String, ConfigValue> l1Cache;
    private final RedisTemplate<String, ConfigValue> redisTemplate;
    private final Metrics metrics;
    private final Logger logger;

    public ConfigCache(RedisTemplate<String, ConfigValue> redisTemplate, Metrics metrics) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;

        // L1 cache with TTL eviction
        this.l1Cache = CacheBuilder.newBuilder()
            .maximumSize(L1_CAPACITY)
            .expireAfterWrite(L1_TTL_SECONDS, TimeUnit.SECONDS)
            .removalListener((k, v, reason) -> {
                if (reason == RemovalCause.EXPIRED) {
                    metrics.recordCacheEviction("L1", k.toString());
                }
            })
            .build(new CacheLoader<String, ConfigValue>() {
                @Override
                public ConfigValue load(String key) throws Exception {
                    throw new CacheLoader.InvalidCacheLoadException("Load not allowed");
                }
            });
    }

    /**
     * Get from L1 (fast path)
     */
    public ConfigValue getFromL1(String paramKey, String brandId, String jurisdictionCode) {
        String cacheKey = buildCacheKey(paramKey, brandId, jurisdictionCode);
        ConfigValue cached = l1Cache.getIfPresent(cacheKey);
        if (cached != null) {
            metrics.recordCacheHit(CacheLayer.L1_LOCAL);
            logger.debug("L1 cache hit: {}", cacheKey);
        }
        return cached;
    }

    /**
     * Get from L2 Redis (network call)
     */
    public ConfigValue getFromL2(String paramKey, ConfigScope scope, String scopeId) {
        String redisKey = buildRedisKey(paramKey, scope, scopeId);
        try {
            ConfigValue cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                metrics.recordCacheHit(CacheLayer.L2_REDIS);
                logger.debug("L2 cache hit: {}", redisKey);
                // Promote to L1 automatically
                String l1Key = buildCacheKey(paramKey, null, null);
                l1Cache.put(l1Key, cached);
            }
            return cached;
        } catch (Exception e) {
            logger.warn("Redis read error for key {}: {}", redisKey, e.getMessage());
            metrics.recordCacheError("L2");
            return null; // Fall through to database
        }
    }

    /**
     * Put to L1 cache
     */
    public void putToL1(ConfigValue value) {
        String cacheKey = buildCacheKey(value.getParamKey(), value.getScopeId(), null);
        l1Cache.put(cacheKey, value);
        metrics.recordCachePut("L1");
    }

    /**
     * Put to L2 Redis with TTL
     */
    public void putToL2(ConfigValue value) {
        String redisKey = buildRedisKey(value.getParamKey(), value.getScope(), value.getScopeId());
        try {
            redisTemplate.opsForValue().set(
                redisKey,
                value,
                Duration.ofSeconds(L2_TTL_SECONDS)
            );
            metrics.recordCachePut("L2");
            logger.debug("L2 cache put: {} (TTL={}s)", redisKey, L2_TTL_SECONDS);
        } catch (Exception e) {
            logger.warn("Redis write error for key {}: {}", redisKey, e.getMessage());
            metrics.recordCacheError("L2");
        }
    }

    /**
     * Invalidate L1 cache locally
     */
    public void invalidateL1(String paramKey, String scopeId) {
        String cacheKey = buildCacheKey(paramKey, scopeId, null);
        l1Cache.invalidate(cacheKey);
        metrics.recordCacheInvalidation("L1");
        logger.info("L1 cache invalidated: {}", cacheKey);
    }

    /**
     * Invalidate L2 Redis globally (pattern delete)
     */
    public void invalidateL2(String paramKey, String scopeId) {
        String redisKeyPattern = buildRedisKeyPattern(paramKey, scopeId);
        try {
            Set<String> keys = redisTemplate.keys(redisKeyPattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                metrics.recordCacheInvalidation("L2");
                logger.info("L2 cache invalidated {} keys matching pattern: {}", keys.size(), redisKeyPattern);
            }
        } catch (Exception e) {
            logger.warn("Redis delete error for pattern {}: {}", redisKeyPattern, e.getMessage());
            metrics.recordCacheError("L2");
        }
    }

    /**
     * Clear all caches (emergency reset)
     */
    public void clearAll() {
        l1Cache.invalidateAll();
        try {
            redisTemplate.keys("config:*").forEach(redisTemplate::delete);
            metrics.recordCacheInvalidation("ALL");
            logger.warn("All caches cleared!");
        } catch (Exception e) {
            logger.error("Error clearing caches: {}", e.getMessage());
        }
    }

    private String buildCacheKey(String paramKey, String scopeId, String jurisdiction) {
        // Format: "ch1.vip.silver_upgrade:BRAND_ASIA"
        return paramKey + (scopeId != null ? ":" + scopeId : "");
    }

    private String buildRedisKey(String paramKey, ConfigScope scope, String scopeId) {
        // Format: "config:ch1.vip.silver_upgrade:BRAND:BRAND_ASIA"
        return String.format("config:%s:%s:%s",
            paramKey,
            scope.name(),
            scopeId != null ? scopeId : "GLOBAL"
        );
    }

    private String buildRedisKeyPattern(String paramKey, String scopeId) {
        // Pattern for UNLINK: "config:ch1.vip.silver_upgrade:*"
        return "config:" + paramKey + ":*";
    }
}

// Redis Pub/Sub listener
@Component
public class ConfigInvalidationListener {

    private final ConfigCache cache;
    private final RedisMessageListenerContainer container;
    private final Logger logger;

    @PostConstruct
    public void subscribe() {
        // Subscribe to config invalidation events
        container.addMessageListener((message, pattern) -> {
            try {
                String payload = new String(message.getBody());
                ConfigInvalidationEvent event = JsonUtils.parse(payload, ConfigInvalidationEvent.class);

                logger.info("Received cache invalidation event: {} / {} / {}",
                    event.getParamKey(), event.getScope(), event.getScopeId());

                // Invalidate both L1 and L2
                cache.invalidateL1(event.getParamKey(), event.getScopeId());
                cache.invalidateL2(event.getParamKey(), event.getScopeId());

            } catch (Exception e) {
                logger.error("Error processing invalidation event: {}", e.getMessage());
            }
        }, new PatternTopic("config:invalidate"));
    }
}

// Event model
@Data
class ConfigInvalidationEvent {
    private String paramKey;
    private ConfigScope scope;
    private String scopeId;
    private long timestamp;
}
```

### 5.4 Hot-Reload Sequence Diagram

```
Timeline:  T0                  T1                    T2                  T3
           │                   │                     │                   │
           │ PUT /config/param │                     │                   │
Admin ─────┼──────────────────→ Config Service      │                   │
           │                   │                     │                   │
           │                   ├─ Update DB          │                   │
           │                   ├─ Write audit log    │                   │
           │                   └─ PUBLISH Redis      │                   │
           │                        event            │                   │
           │                        │                │                   │
           │                        ├───────────────→ Redis Pub/Sub ────→ All instances
           │                        │                │                   │
           │                        │                └─ Flush L1/L2 ────→
           │                        │                   cache            │
           │                        │                                    │
           │                        │                   (≤100ms broadcast)
           │                        │                                    │
Instances  │                   Config Instance 1       │                 │
───────────┼─────────────────────────────────────────────────────────────┼─ Cache hit from DB
           │                   Instance 2              │                 │
───────────┼─────────────────────────────────────────────────────────────┼─ Cache miss
           │                   Instance N              │                 │
───────────┼─────────────────────────────────────────────────────────────┼─ Cache miss
           │                                           │                 │
           ↑                   ↑                       ↑                 ↑
      T0: Change         T1: DB updated &       T2: Broadcast      T3: Cache warm
         initiated       Pub/Sub published      to all instances   (≤5 sec total)
```

### 5.5 Monitoring Targets

```
Cache Performance SLOs:

| Metric | Target | Alert |
|--------|--------|-------|
| L1 Hit Rate | >90% | <85% |
| L2 Hit Rate | >95% | <90% |
| Cache Miss Latency P99 | <50ms | >100ms |
| Hot-reload Propagation Time | <5s | >10s |
| Redis Pub/Sub Lag | <100ms | >500ms |
| Cache Eviction Rate | <10 per second | >50 per second |
| Cache Memory Usage | <200MB | >400MB |
```

---

## T-CFG.6 Admin API 規格

### 6.1 REST Endpoints (OpenAPI 3.0)

```yaml
openapi: 3.0.0
info:
  title: Config Service Admin API
  version: v1
  description: Configuration management API for parameter CRUD, overrides, and audit
servers:
  - url: https://config-api.igaming.internal/api/v1

paths:
  /config/params:
    get:
      summary: List all parameters with filtering
      tags: [Parameters]
      parameters:
        - name: chapter
          in: query
          schema: { type: string, example: "Ch1" }
          description: Filter by chapter (Ch1, Ch2, ...)
        - name: category
          in: query
          schema: { type: string, enum: [PERCENTAGE, AMOUNT, DURATION, COUNT, THRESHOLD] }
        - name: compliance
          in: query
          schema: { type: boolean }
          description: Filter by compliance_driven flag
        - name: page
          in: query
          schema: { type: integer, default: 1 }
        - name: limit
          in: query
          schema: { type: integer, default: 100, maximum: 500 }
      responses:
        '200':
          description: Paginated list of parameters
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/ParamDefinition'
                  total:
                    type: integer
                  page:
                    type: integer
                  limit:
                    type: integer
        '400':
          description: Invalid filter parameters
        '403':
          description: Insufficient permissions

  /config/params/{paramKey}:
    get:
      summary: Get parameter definition and current values across all scopes
      tags: [Parameters]
      parameters:
        - name: paramKey
          in: path
          required: true
          schema: { type: string, example: "ch1.vip.silver_upgrade" }
      responses:
        '200':
          description: Parameter definition with current override values
          content:
            application/json:
              schema:
                type: object
                properties:
                  definition:
                    $ref: '#/components/schemas/ParamDefinition'
                  overrides:
                    type: array
                    items:
                      $ref: '#/components/schemas/ParamValue'
                  activeValues:
                    type: object
                    additionalProperties:
                      type: string
                      description: "Map of 'GLOBAL|BRAND_{id}|JURISDICTION_{code}' to resolved value"
        '404':
          description: Parameter not found
        '403':
          description: Insufficient permissions

    put:
      summary: Update or create a parameter override
      tags: [Parameters]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                scope:
                  type: string
                  enum: [GLOBAL, BRAND, JURISDICTION]
                scope_id:
                  type: string
                  nullable: true
                  description: null for GLOBAL, brand_id for BRAND, jurisdiction_code for JURISDICTION
                value:
                  type: string
                  description: Must pass validation constraints (min, max, enum)
                effective_from:
                  type: string
                  format: date-time
                  default: now()
                effective_until:
                  type: string
                  format: date-time
                  nullable: true
                  description: Scheduled sunset date (optional)
                description:
                  type: string
                  example: "MGA liquidity requirement increase"
              required: [scope, value]
      responses:
        '200':
          description: Parameter override created/updated
          content:
            application/json:
              schema:
                type: object
                properties:
                  id:
                    type: integer
                  paramKey:
                    type: string
                  scope:
                    type: string
                  scopeId:
                    type: string
                  value:
                    type: string
                  approvalStatus:
                    type: string
                    enum: [DRAFT, PENDING_APPROVAL, APPROVED, REJECTED]
                  createdAt:
                    type: string
                    format: date-time
        '400':
          description: Validation failed (invalid value, constraint violation)
        '403':
          description: Insufficient permissions
        '409':
          description: Conflict (e.g., active override already exists for this scope)

  /config/params/{paramKey}/history:
    get:
      summary: Get audit history for a parameter
      tags: [Audit]
      parameters:
        - name: paramKey
          in: path
          required: true
          schema: { type: string }
        - name: scope
          in: query
          schema: { type: string, enum: [GLOBAL, BRAND, JURISDICTION] }
        - name: scope_id
          in: query
          schema: { type: string }
        - name: days
          in: query
          schema: { type: integer, default: 30 }
        - name: limit
          in: query
          schema: { type: integer, default: 100, maximum: 1000 }
      responses:
        '200':
          description: Audit log entries
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AuditLogEntry'
        '404':
          description: Parameter not found

  /config/params/{paramKey}/override:
    post:
      summary: Create new override with approval workflow
      tags: [Parameters]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                scope:
                  type: string
                  enum: [GLOBAL, BRAND, JURISDICTION]
                scope_id:
                  type: string
                value:
                  type: string
                reason:
                  type: string
                  description: Business justification
                requires_approval:
                  type: boolean
                  default: true
                  description: Auto-set to true if compliance_driven=true
              required: [scope, scope_id, value, reason]
      responses:
        '201':
          description: Override created (in DRAFT or APPROVED status)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ParamValue'
        '400':
          description: Invalid input
        '403':
          description: Insufficient permissions

  /config/params/bulk-import:
    post:
      summary: Bulk import parameters from CSV/JSON
      tags: [Parameters]
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                  description: CSV or JSON file containing parameter bulk data
                dry_run:
                  type: boolean
                  default: true
                  description: Validate without committing
                on_conflict:
                  type: string
                  enum: [SKIP, OVERWRITE, FAIL]
                  default: SKIP
              required: [file]
      responses:
        '200':
          description: Bulk import results
          content:
            application/json:
              schema:
                type: object
                properties:
                  total:
                    type: integer
                  created:
                    type: integer
                  updated:
                    type: integer
                  skipped:
                    type: integer
                  errors:
                    type: array
                    items:
                      type: object
                      properties:
                        row:
                          type: integer
                        error:
                          type: string
        '400':
          description: Invalid file format or validation errors
        '403':
          description: Insufficient permissions

  /config/params/{paramKey}/diff:
    get:
      summary: Compare parameter values across environments/times
      tags: [Comparison]
      parameters:
        - name: paramKey
          in: path
          required: true
          schema: { type: string }
        - name: from_timestamp
          in: query
          schema: { type: string, format: date-time }
        - name: to_timestamp
          in: query
          schema: { type: string, format: date-time }
      responses:
        '200':
          description: Diff results
          content:
            application/json:
              schema:
                type: object
                properties:
                  from_state:
                    type: array
                    items:
                      $ref: '#/components/schemas/ParamValue'
                  to_state:
                    type: array
                    items:
                      $ref: '#/components/schemas/ParamValue'
                  changes:
                    type: array
                    items:
                      type: object
                      properties:
                        scope:
                          type: string
                        scope_id:
                          type: string
                        old_value:
                          type: string
                        new_value:
                          type: string
                        changed_at:
                          type: string
                          format: date-time

components:
  schemas:
    ParamDefinition:
      type: object
      properties:
        paramKey:
          type: string
          example: "ch1.vip.silver_upgrade"
        chapter:
          type: string
        displayName:
          type: string
        description:
          type: string
        dataType:
          type: string
          enum: [INTEGER, DECIMAL, DURATION, PERCENTAGE, AMOUNT, BOOLEAN, ENUM]
        unit:
          type: string
        defaultValue:
          type: string
        minValue:
          type: string
        maxValue:
          type: string
        complianceDriven:
          type: boolean
        isSensitive:
          type: boolean
        createdAt:
          type: string
          format: date-time

    ParamValue:
      type: object
      properties:
        id:
          type: integer
        paramKey:
          type: string
        scope:
          type: string
          enum: [GLOBAL, BRAND, JURISDICTION]
        scopeId:
          type: string
          nullable: true
        value:
          type: string
        description:
          type: string
        effectiveFrom:
          type: string
          format: date-time
        effectiveUntil:
          type: string
          format: date-time
          nullable: true
        isActive:
          type: boolean
        approvalStatus:
          type: string
          enum: [DRAFT, PENDING_APPROVAL, APPROVED, REJECTED]
        createdBy:
          type: string
        approvedBy:
          type: string
          nullable: true
        approvalComments:
          type: string
        approvalDate:
          type: string
          format: date-time
          nullable: true
        createdAt:
          type: string
          format: date-time

    AuditLogEntry:
      type: object
      properties:
        id:
          type: integer
        paramKey:
          type: string
        scope:
          type: string
        scopeId:
          type: string
          nullable: true
        oldValue:
          type: string
          nullable: true
        newValue:
          type: string
          nullable: true
        changeType:
          type: string
          enum: [CREATE, UPDATE, DELETE, APPROVE, REJECT]
        changedBy:
          type: string
        changeReason:
          type: string
        approvalReason:
          type: string
          nullable: true
        ipAddress:
          type: string
        createdAt:
          type: string
          format: date-time

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: "OAuth 2.0 token with 'config:read' or 'config:write' scope"
```

### 6.2 Error Handling

```kotlin
// Error responses

data class ErrorResponse(
    val code: String,          // E.g., "CONFIG_VALIDATION_ERROR", "CONFIG_UNAUTHORIZED"
    val message: String,       // Human-readable message
    val details: Map<String, Any>? = null,
    val timestamp: Instant = Instant.now(),
    val traceId: String        // For correlation with logs
)

// Common status codes:
// 400 Bad Request: validation failed
// 403 Forbidden: RBAC denied
// 404 Not Found: parameter not found
// 409 Conflict: concurrent modification, active override exists
// 429 Too Many Requests: rate limit exceeded
// 500 Internal Server Error: database or cache failure
// 503 Service Unavailable: circuit breaker open
```

---

## T-CFG.7 Maker-Checker 審批流程

### 7.1 State Machine

```
┌─────────────┐
│   DRAFT     │ ← Initial state after creation
└──────┬──────┘
       │
       ├─ [if compliance_driven=FALSE] → Auto-approve
       │
       └─ [if compliance_driven=TRUE] → Move to PENDING_APPROVAL
          │
          └──────────────────┬──────────────────┐
                             │                  │
                    ┌────────▼────────┐  ┌──────▼─────┐
                    │ PENDING_APPROVAL│  │  REJECTED  │ ← Approver rejects
                    └────────┬────────┘  └────────────┘
                             │
                    ┌────────▼────────┐
                    │    APPROVED     │ ← Approver accepts
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │     ACTIVE      │ ← Change published to systems
                    └────────────────┘

Key State Transitions:
- DRAFT → PENDING_APPROVAL: Created for compliance-driven param
- PENDING_APPROVAL → APPROVED: Approver double-signs
- PENDING_APPROVAL → REJECTED: Approver denies with reason
- APPROVED → ACTIVE: Immediately upon approval (or on scheduled effective_from)
- DRAFT → ACTIVE: Immediately upon creation (non-compliance param)
```

### 7.2 Approval Workflow Implementation

```kotlin
@Service
class ConfigApprovalService(
    private val repository: ConfigParamRepository,
    private val cache: ConfigCache,
    private val eventPublisher: ApplicationEventPublisher,
    private val metrics: Metrics,
    private val logger: Logger
) {

    /**
     * Submit parameter change for approval
     * Returns IMMEDIATELY if compliance_driven=false
     * Queues for manual approval if compliance_driven=true
     */
    fun submitForApproval(
        paramKey: String,
        scope: ConfigScope,
        scopeId: String?,
        newValue: String,
        reason: String,
        requestedBy: String
    ): ConfigApprovalResult {

        // Step 1: Validate parameter exists and value is valid
        val paramDef = repository.getParamDefinition(paramKey)
            ?: throw ParameterNotFoundException(paramKey)

        if (!validateValue(newValue, paramDef)) {
            throw ParameterValidationException("Value $newValue violates constraints for $paramKey")
        }

        // Step 2: Create entry in config_param_value (status=DRAFT)
        val entry = ConfigParamValue(
            paramKey = paramKey,
            scope = scope,
            scopeId = scopeId,
            value = newValue,
            description = reason,
            approvalStatus = ApprovalStatus.DRAFT,
            createdBy = requestedBy,
            createdAt = Instant.now()
        )
        val savedEntry = repository.insertParamValue(entry)

        // Step 3: Check if compliance-driven
        if (!paramDef.complianceDriven) {
            // Non-compliance: Auto-approve immediately
            approveChange(
                savedEntry.id,
                paramDef.paramKey,
                reason = "Auto-approved (non-compliance parameter)",
                approvedBy = "SYSTEM"
            )

            return ConfigApprovalResult(
                paramValueId = savedEntry.id,
                status = ApprovalStatus.APPROVED,
                requiresApprover = false,
                nextStep = "Change is active immediately"
            )
        }

        // Step 4: Compliance-driven: Queue for approval
        val approvalTicket = ApprovalTicket(
            paramValueId = savedEntry.id,
            paramKey = paramKey,
            scope = scope,
            scopeId = scopeId,
            oldValue = findCurrentValue(paramKey, scope, scopeId)?.value,
            newValue = newValue,
            reason = reason,
            submittedBy = requestedBy,
            submittedAt = Instant.now(),
            requiredApprovers = listOf("COMPLIANCE_APPROVER")
        )
        repository.insertApprovalTicket(approvalTicket)

        // Step 5: Notify approvers
        eventPublisher.publishEvent(
            ApprovalRequiredEvent(
                ticketId = approvalTicket.id,
                paramKey = paramKey,
                reason = reason
            )
        )

        metrics.recordApprovalQueued(paramKey)
        logger.info("Change queued for approval: {} (ticket={})", paramKey, approvalTicket.id)

        return ConfigApprovalResult(
            paramValueId = savedEntry.id,
            status = ApprovalStatus.PENDING_APPROVAL,
            requiresApprover = true,
            nextStep = "Awaiting compliance approver signature"
        )
    }

    /**
     * Approve a pending change (Maker-Checker second signature)
     */
    fun approveChange(
        paramValueId: Long,
        paramKey: String,
        reason: String,
        approvedBy: String
    ): ConfigApprovalResult {

        logger.info("Approving change: paramValueId={}, approvedBy={}", paramValueId, approvedBy)

        // Step 1: Load the pending entry
        val entry = repository.getParamValue(paramValueId)
            ?: throw IllegalArgumentException("ParamValue not found: $paramValueId")

        if (entry.approvalStatus != ApprovalStatus.PENDING_APPROVAL) {
            throw IllegalStateException(
                "Can only approve PENDING_APPROVAL entries, current status: ${entry.approvalStatus}"
            )
        }

        // Step 2: Update status to APPROVED
        entry.approvalStatus = ApprovalStatus.APPROVED
        entry.approvedBy = approvedBy
        entry.approvalComments = reason
        entry.approvalDate = Instant.now()
        entry.isActive = true

        repository.updateParamValue(entry)

        // Step 3: Deactivate old entry for this (paramKey, scope, scopeId)
        val oldEntry = repository.findActiveValue(entry.paramKey, entry.scope, entry.scopeId)
        if (oldEntry != null && oldEntry.id != paramValueId) {
            oldEntry.isActive = false
            repository.updateParamValue(oldEntry)

            // Record audit for old value
            repository.insertAuditLog(
                AuditLogEntry(
                    paramKey = entry.paramKey,
                    scope = entry.scope,
                    scopeId = entry.scopeId,
                    oldValue = oldEntry.value,
                    newValue = null,
                    changeType = ChangeType.DELETE,
                    changedBy = approvedBy,
                    changeReason = "Superseded by new active value"
                )
            )
        }

        // Step 4: Write audit log
        repository.insertAuditLog(
            AuditLogEntry(
                paramKey = entry.paramKey,
                scope = entry.scope,
                scopeId = entry.scopeId,
                oldValue = oldEntry?.value,
                newValue = entry.value,
                changeType = ChangeType.APPROVE,
                changedBy = approvedBy,
                changeReason = reason,
                approvalReason = "Approved by $approvedBy"
            )
        )

        // Step 5: Invalidate caches and broadcast change
        cache.invalidateL1(entry.paramKey, entry.scopeId)
        cache.invalidateL2(entry.paramKey, entry.scopeId)

        publishConfigChangeEvent(
            ConfigChangeEvent(
                paramKey = entry.paramKey,
                scope = entry.scope,
                scopeId = entry.scopeId,
                oldValue = oldEntry?.value,
                newValue = entry.value,
                changedAt = Instant.now()
            )
        )

        // Step 6: Record metrics
        metrics.recordApprovalApproved(entry.paramKey)

        logger.info("Change approved and activated: {} / {} / {}",
            entry.paramKey, entry.scope, entry.scopeId)

        return ConfigApprovalResult(
            paramValueId = paramValueId,
            status = ApprovalStatus.APPROVED,
            requiresApprover = false,
            nextStep = "Change is now ACTIVE across all instances"
        )
    }

    /**
     * Reject a pending change with explanation
     */
    fun rejectChange(
        paramValueId: Long,
        paramKey: String,
        reason: String,
        rejectedBy: String
    ): ConfigApprovalResult {

        logger.info("Rejecting change: paramValueId={}, rejectedBy={}", paramValueId, rejectedBy)

        val entry = repository.getParamValue(paramValueId)
            ?: throw IllegalArgumentException("ParamValue not found: $paramValueId")

        if (entry.approvalStatus != ApprovalStatus.PENDING_APPROVAL) {
            throw IllegalStateException(
                "Can only reject PENDING_APPROVAL entries, current status: ${entry.approvalStatus}"
            )
        }

        // Update status to REJECTED
        entry.approvalStatus = ApprovalStatus.REJECTED
        entry.approvalComments = reason
        entry.approvalDate = Instant.now()
        entry.isActive = false

        repository.updateParamValue(entry)

        // Write audit log
        repository.insertAuditLog(
            AuditLogEntry(
                paramKey = entry.paramKey,
                scope = entry.scope,
                scopeId = entry.scopeId,
                oldValue = null,
                newValue = null,
                changeType = ChangeType.REJECT,
                changedBy = rejectedBy,
                changeReason = reason
            )
        )

        metrics.recordApprovalRejected(entry.paramKey)

        logger.info("Change rejected: {} / {} / {}",
            entry.paramKey, entry.scope, entry.scopeId)

        return ConfigApprovalResult(
            paramValueId = paramValueId,
            status = ApprovalStatus.REJECTED,
            requiresApprover = false,
            nextStep = "Change is REJECTED and inactive"
        )
    }

    private fun findCurrentValue(paramKey: String, scope: ConfigScope, scopeId: String?): ConfigParamValue? {
        return repository.findActiveValue(paramKey, scope, scopeId)
    }

    private fun validateValue(value: String, paramDef: ParamDefinition): Boolean {
        // Implement validation based on min/max/enum constraints
        // ...
        return true
    }

    private fun publishConfigChangeEvent(event: ConfigChangeEvent) {
        try {
            val json = ObjectMapper().writeValueAsString(event)
            redisTemplate.convertAndSend("config:invalidate", json)
        } catch (e: Exception) {
            logger.error("Failed to publish config change event: {}", e.message)
        }
    }
}

data class ConfigApprovalResult(
    val paramValueId: Long,
    val status: ApprovalStatus,
    val requiresApprover: Boolean,
    val nextStep: String
)

enum class ApprovalStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED }
```

---

## T-CFG.8 Admin UI 規格

### 8.1 Pages & Components

**Page 1: Parameter Browser**
- Search bar (full-text on paramKey + displayName)
- Filters: Chapter, Category, Compliance flag, Sensitive flag
- Sortable columns: paramKey, chapter, dataType, defaultValue, lastModified
- Details panel (on row click): Shows full definition + current overrides + edit history
- Actions: View History, Create Override, Bulk Edit

**Page 2: Parameter Editor**
- Single parameter focus view
- Current value display (by scope: Global → Brand → Jurisdiction)
- Override form:
  - Scope selector (GLOBAL/BRAND/JURISDICTION)
  - Scope ID selector (brand dropdown, jurisdiction dropdown)
  - Value input (with live validation: shows errors if invalid)
  - Effective date pickers (from/until)
  - Reason/description textarea
  - Submit button (creates DRAFT or APPROVED entry)
- Change history timeline (last 20 changes)
- Approval queue status (if pending)

**Page 3: History Viewer & Diff Comparison**
- Timeline of all changes to a parameter (by param_key, scope, scope_id)
- Each entry shows:
  - Timestamp
  - Who changed it
  - From → To value
  - Reason
  - Approval status
- Diff view: Select two snapshots in time, see side-by-side comparison across all scopes
- Export as CSV/JSON

**Page 4: Approval Queue**
- Table of PENDING_APPROVAL entries
- Columns: paramKey, scope, oldValue → newValue, submittedBy, submittedAt, reason
- Actions: Approve (with comment), Reject (with reason)
- Filters: pending count, priority (compliance vs non-compliance)
- Real-time updates (via WebSocket or polling)

**Page 5: Bulk Import/Export**
- Import: Upload CSV/JSON file
  - Dry-run mode first
  - Show validation results
  - Confirm and apply
- Export: Download all parameters as CSV
  - Include columns: paramKey, scope, scopeId, currentValue, defaultValue

### 8.2 UI Mockup (ASCII)

```
┌─────────────────────────────────────────────────────────────────┐
│ Config Management Dashboard                      [USER] [LOGOUT] │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  [Parameter Browser] [Editor] [History] [Approval Queue] [Admin] │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Search: [____________________________]  [🔍 Search]        │  │
│  │ Filters: [Chapter ▼] [Category ▼] [Compliance ▼] [🔄 Reset] │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ Parameter Key              │ Chapter │ Type      │ Modified   │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ ch1.kyc.l0_daily_withdraw  │ Ch1     │ AMOUNT    │ 2026-03-25 │ │
│  │ ch1.vip.silver_upgrade     │ Ch1     │ AMOUNT    │ 2026-03-10 │ │
│  │ ch2.wallet.round_timeout   │ Ch2     │ DURATION  │ 2026-03-01 │ │
│  │ ch2.bonus.welcome_multiplier│ Ch2     │ INTEGER   │ 2026-02-28 │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  [< Previous] [1] [2] [3] ... [Next >]                          │
└─────────────────────────────────────────────────────────────────┘

Details Panel (on selection):
┌─────────────────────────────────────────────────────────────────┐
│ ch1.kyc.l0_daily_withdrawal                                     │
│ KYC Level 0 Daily Withdrawal Limit (KYC 0級日提款限額)           │
│                                                                   │
│ Definition:                                                      │
│  - Type: AMOUNT (USD)                                           │
│  - Default: $0                                                  │
│  - Compliance-driven: ✓ Yes                                     │
│  - Direction: LOWER_IS_STRICTER                                 │
│                                                                   │
│ Current Overrides:                                              │
│  ┌──────────────┬────────────┬───────────────────────────┐     │
│  │ Scope        │ Value      │ Effective Period          │     │
│  ├──────────────┼────────────┼───────────────────────────┤     │
│  │ GLOBAL       │ $0         │ 2026-03-25 → ∞           │     │
│  │ JURISDICTION │ $0 (UKGC)  │ 2026-03-25 → ∞           │     │
│  │ BRAND        │ $100 (AsiaX)│ 2026-03-20 → ∞           │     │
│  └──────────────┴────────────┴───────────────────────────┘     │
│                                                                   │
│  [Create Override] [View History] [Compare] [Export]            │
└─────────────────────────────────────────────────────────────────┘
```

---

## T-CFG.9 資料遷移策略

### 9.1 Data Migration Execution Plan

**Pre-Migration (Week 1)**
1. Export all 845+ parameters from requirements PRD
2. Deduplicate & validate for consistency
3. Create CSV template: param_key, chapter, display_name_en, display_name_zh, data_type, unit, default_value, min_value, max_value, compliance_driven, direction, category
4. Populate CSV with extracted values
5. Code review with compliance team

**Migration Phase 1: Seed Definition Table (Week 2)**
```sql
-- Step 1: Load ParamDefinition from CSV
LOAD DATA INFILE '/tmp/param_definitions.csv'
INTO TABLE config_param_definition
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
(param_key, chapter, display_name_en, display_name_zh, data_type, unit,
 default_value, min_value, max_value, compliance_driven, direction, category);

-- Step 2: Validate all records
SELECT COUNT(*) FROM config_param_definition;  -- Should = 845
SELECT COUNT(DISTINCT param_key) FROM config_param_definition;  -- Should = 845

-- Step 3: Check for nulls in required fields
SELECT param_key FROM config_param_definition
WHERE param_key IS NULL OR data_type IS NULL OR default_value IS NULL;
```

**Migration Phase 2: Initialize Global Defaults (Week 2)**
```sql
-- Step 1: Insert global values for all 845+ parameters
-- Source: Extract current hardcoded values from codebase
INSERT INTO config_param_value (
    param_key, scope, scope_id, value, is_active,
    approval_status, created_by, created_at
)
SELECT
    param_key,
    'GLOBAL' as scope,
    NULL as scope_id,
    default_value as value,
    TRUE as is_active,
    'APPROVED' as approval_status,
    'system_migration' as created_by,
    NOW() as created_at
FROM config_param_definition;

-- Step 2: Verify all params have global values
SELECT COUNT(*) FROM config_param_value WHERE scope = 'GLOBAL';  -- Should = 845
```

**Migration Phase 3: Add Known Brand/Jurisdiction Overrides (Week 2)**
```sql
-- Insert pre-approved overrides from compliance/ops team
-- Example: UKGC jurisdiction overrides
INSERT INTO config_param_value (
    param_key, scope, scope_id, value, is_active,
    approval_status, created_by, approval_reason, created_at
) VALUES
('ch1.kyc.l0_daily_withdrawal', 'JURISDICTION', 'UKGC', '0', TRUE,
 'APPROVED', 'compliance_team', 'UKGC regulatory requirement', NOW()),
('ch2.slots.max_bet', 'JURISDICTION', 'UKGC', '2', TRUE,
 'APPROVED', 'compliance_team', 'UKGC £2 maximum stake limit', NOW()),
-- ... more UKGC, MGA, PAGCOR, Curaçao overrides
-- ... Brand-specific overrides (VIP thresholds, bonus caps, etc.)
;

-- Verify override counts
SELECT COUNT(*) FROM config_param_value WHERE scope = 'BRAND';
SELECT COUNT(*) FROM config_param_value WHERE scope = 'JURISDICTION';
```

**Migration Phase 4: Validation & Dry-Run Testing (Week 3)**

```kotlin
// ValidationScript.kt
fun validateMigration() {
    // Test 1: All 845 params have global values
    val globalCount = repository.countByScope(ConfigScope.GLOBAL)
    assert(globalCount == 845) { "Expected 845 global params, got $globalCount" }

    // Test 2: No param is missing definition
    val orphanValues = repository.findValuesWithoutDefinition()
    assert(orphanValues.isEmpty()) { "Found orphaned values: $orphanValues" }

    // Test 3: Resolution engine works correctly
    val testCases = listOf(
        Triple("ch2.slots.max_bet", "BRAND_A", "UKGC") to "5",  // UKGC override
        Triple("ch1.vip.silver_upgrade", "BRAND_A", null) to "10000",  // Brand override
        Triple("ch1.account.dormant_days", null, null) to "90"  // Global default
    )

    for ((key, brandId, jurisdiction), expectedValue) in testCases {
        val resolved = resolver.resolveConfig(key, brandId, jurisdiction)
        assert(resolved?.value == expectedValue) {
            "Resolution failed: $key / $brandId / $jurisdiction => " +
            "expected $expectedValue, got ${resolved?.value}"
        }
    }

    // Test 4: Compliance params resolve correctly
    val complianceTestCases = listOf(
        // (paramKey, brandId, jurisdiction) -> expected value
        Triple("ch1.kyc.l0_daily_withdrawal", "BRAND_X", "UKGC") to "0"  // Min of all
    )

    for ((key, brandId, jurisdiction), expectedValue) in complianceTestCases {
        val resolved = resolver.resolveComplianceConfig(key, brandId, jurisdiction)
        assert(resolved?.value == expectedValue) {
            "Compliance resolution failed: $key / $brandId / $jurisdiction"
        }
    }

    // Test 5: Cache performance
    val startTime = System.nanoTime()
    repeat(10000) {
        resolver.resolveConfig("ch1.account.dormant_days", null, null)
    }
    val latencyMs = (System.nanoTime() - startTime) / 10000 / 1_000_000
    assert(latencyMs < 1) { "Average latency ${latencyMs}ms exceeds 1ms target" }

    println("✓ All validation tests passed!")
}
```

**Migration Phase 5: Production Deployment (Week 3-4)**

1. **Pre-Deployment**
   - Backup production database (full backup)
   - Load test with 100% parameter access patterns
   - Monitor: cache hit rates, resolution latency, error rates
   - Setup canary: Deploy config-service to 10% of servers only

2. **Phased Rollout**
   ```
   T=0:     Canary 10%, monitor for 30 minutes
   T=30m:   Canary 25%, monitor for 60 minutes
   T=90m:   Canary 50%, monitor for 120 minutes
   T=150m:  Canary 100%, full production traffic
   ```

3. **Service Cutover** (After ≥4 hours stable)
   ```
   Parallel deployment:
   - All game engines → call resolver.resolveConfig() instead of hardcoded values
   - All audit systems → read from config_audit_log for compliance trails
   - Admin UI → enabled for configuration management
   ```

4. **Verification**
   - Spot-check 50 random parameters across all services
   - Verify old hardcoded constants match resolved values (100% match)
   - Monitor CPU/memory/latency for 24 hours
   - No alert threshold breaches expected

### 9.2 Rollback Plan

**If Critical Issues in First 24 Hours:**

```
Step 1: Circuit Breaker Activation
  - Config Service: Return cached hardcoded fallbacks (safe defaults embedded)
  - Admin UI: Disable parameter changes (read-only mode)
  - All services: Stop calling resolveConfig(), revert to hardcoded values

Step 2: Issue Investigation
  - Check database logs for corruption
  - Review config_audit_log for any suspicious changes
  - Verify Redis state and network connectivity

Step 3: Fix & Redeploy
  - If database issue: Restore from pre-migration backup
  - If cache issue: Clear Redis and warm caches from database
  - If code bug: Hot-fix and re-deploy config-service

Step 4: Resume
  - Enable circuit breaker gradually (10% → 50% → 100%)
  - Re-run validation suite
  - Confirm resolution latency P99 ≤ 10ms
  - Resume parameter change approvals
```

**Validation Checks (Rollback)**
```sql
-- Verify data integrity after rollback
SELECT COUNT(*) FROM config_param_definition WHERE default_value IS NULL;  -- Should = 0
SELECT COUNT(*) FROM config_param_value WHERE value IS NULL;  -- Should = 0

-- Compare pre- and post-migration snapshots
SELECT param_key, COUNT(*) as val_count FROM config_param_value GROUP BY param_key
  HAVING val_count != 1;  -- Each param should have exactly 1 active global value

-- Check audit trail integrity
SELECT COUNT(*) FROM config_audit_log WHERE created_at > '2026-03-25';  -- Migration entries
```

---

## T-CFG.10 監控與告警

### 10.1 Metrics & KPIs

```
Cache Metrics:
├─ L1 Hit Rate (%)
│  └─ Target: >90%, Alert if <85% for 10 minutes
├─ L2 Hit Rate (%)
│  └─ Target: >95%, Alert if <90% for 10 minutes
├─ L3 (DB) Hit Rate (%)
│  └─ Track: Should be <5% (complement of L2)
└─ Cache Eviction Rate (events/sec)
   └─ Alert if >100 evictions/sec (possible memory pressure)

Resolution Performance:
├─ resolveConfig() Latency P99 (ms)
│  └─ Target: <10ms, Alert if >20ms
├─ resolveComplianceConfig() Latency P99 (ms)
│  └─ Target: <15ms, Alert if >30ms
├─ Cache Miss Latency P99 (ms)
│  └─ Target: <50ms, Alert if >100ms
└─ Database Query Latency P95 (ms)
   └─ Target: <5ms, Alert if >10ms

Hot-Reload Performance:
├─ Pub/Sub Broadcast Latency P99 (ms)
│  └─ Target: <100ms, Alert if >500ms
├─ Cache Invalidation Latency P99 (ms)
│  └─ Target: <5 seconds, Alert if >10 seconds
├─ Config Change Propagation Time P99 (sec)
│  └─ Target: <5 seconds from DB update to all instances
└─ Active Config Changes Queue Depth
   └─ Alert if >100 PENDING_APPROVAL for >24 hours

Approval Workflow:
├─ Approval Queue Depth
│  └─ Alert if >100 pending approvals
├─ Approval Mean Time to Approval (MTTA)
│  └─ Target: <4 hours, SLA: <24 hours
├─ Approval Rejection Rate (%)
│  └─ Alert if >30% (possible bad submissions)
└─ Approver Response Time P95 (min)
   └─ Target: <60 minutes

Data Integrity:
├─ Orphaned Values (param_key not in definition)
│  └─ Alert if >0
├─ Null Value Count
│  └─ Alert if >0 in required columns
├─ Validation Errors (%)
│  └─ Alert if >1% of incoming changes fail validation
└─ Audit Log Growth Rate (entries/day)
   └─ Monitor for anomalies (e.g., >10k entries/day)

Error Rate:
├─ Resolution Errors (%)
│  └─ Alert if >0.1%
├─ Cache Read Errors (%)
│  └─ Alert if >0.5%
├─ Cache Write Errors (%)
│  └─ Alert if >0.5%
├─ Approval Errors (%)
│  └─ Alert if >1%
└─ Database Errors (%)
   └─ Alert if >0.1%
```

### 10.2 Monitoring Dashboard (Prometheus/Grafana)

```
┌────────────────────────────────────────────────────────────────┐
│ Config Service Monitoring                                      │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│ [System Health]                 [Cache Performance]             │
│ ✓ 1,247 instances healthy      L1 Hit Rate: 92.3% ✓            │
│ ✓ DB: CONNECTED                L2 Hit Rate: 96.1% ✓            │
│ ✓ Redis: CONNECTED             L3 QPS: 42 ✓                    │
│ ✓ All services responding       Memory L1: 45MB / 50MB         │
│                                 Memory L2: 78MB / 100MB        │
│                                                                  │
│ [Resolution Latency (ms)]       [Hot-Reload Status]            │
│ resolveConfig P99: 8.4ms ✓      Broadcast Lag P99: 42ms ✓      │
│ resolveCompliance P99: 12.1ms✓  Invalidation P99: 1.2s ✓       │
│                                 Last change prop: 1.8s ✓       │
│                                                                  │
│ [Recent Approvals]              [Error Rate]                   │
│ Queue Depth: 3                  Resolution: 0.02% ✓            │
│ MTTA: 42 minutes                Cache: 0.04% ✓                 │
│ Rejection Rate: 8.7%            Approval: 0.1% ✓               │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

### 10.3 Alert Rules

```yaml
# prometheus/config-service-alerts.yml

groups:
  - name: ConfigService
    rules:
      - alert: ConfigResolutionLatencyHigh
        expr: histogram_quantile(0.99, config_resolution_duration_ms) > 20
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Config resolution P99 latency > 20ms"
          runbook: "https://wiki.internal/config-service/latency-runbook"

      - alert: ConfigL1CacheHitRateLow
        expr: config_cache_hit_rate{layer="L1"} < 0.85
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "L1 cache hit rate < 85% (current: {{ $value | humanizePercentage }})"

      - alert: ConfigApprovalQueueFull
        expr: config_approval_queue_depth > 100
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Config approval queue has {{ $value }} pending items"

      - alert: ConfigCacheInvalidationDelayed
        expr: config_invalidation_latency_seconds > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Config invalidation took > 10 seconds"

      - alert: ConfigDatabaseErrors
        expr: rate(config_db_errors_total[5m]) > 0.001
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database error rate > 0.1%"

      - alert: ConfigOrphanedValues
        expr: config_orphaned_values_count > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Found {{ $value }} orphaned parameter values!"
```

---

## T-CFG.11 安全考量

### 11.1 Role-Based Access Control (RBAC)

```
Roles & Permissions:

┌─────────────────┬──────────────┬─────────────┬──────────────┐
│ Role            │ Read Params  │ Create Param│ Approve Param│
├─────────────────┼──────────────┼─────────────┼──────────────┤
│ CONFIG_VIEWER   │ ✓ All        │ ✗           │ ✗            │
│ CONFIG_EDITOR   │ ✓ All        │ ✓ Non-comp  │ ✗            │
│ CONFIG_APPROVER │ ✓ All        │ ✓ All       │ ✓ Compliance │
│ COMPLIANCE      │ ✓ All        │ ✓ Compliance│ ✓ Compliance │
│ ADMIN           │ ✓ All        │ ✓ All       │ ✓ All        │
└─────────────────┴──────────────┴─────────────┴──────────────┘

Additional Scoped Permissions:
- CONFIG_EDITOR_BRAND_ASIA: Can only edit params for BRAND_ASIA
- CONFIG_EDITOR_JURISDICTION_UKGC: Can only edit params for UKGC jurisdiction
- CONFIG_APPROVER_COMPLIANCE: Can approve only compliance_driven=true params
```

### 11.2 Authorization Checks (Kotlin)

```kotlin
@Component
class ConfigAuthorizationService(
    private val rbacService: RbacService,
    private val logger: Logger
) {

    /**
     * Check if user can read a parameter
     */
    fun canRead(paramKey: String, userId: String): Boolean {
        val roles = rbacService.getUserRoles(userId)
        return roles.any { it in listOf("CONFIG_VIEWER", "CONFIG_EDITOR", "CONFIG_APPROVER", "COMPLIANCE", "ADMIN") }
    }

    /**
     * Check if user can create/update a parameter override
     */
    fun canEdit(
        paramKey: String,
        scope: ConfigScope,
        scopeId: String?,
        userId: String
    ): Boolean {
        val roles = rbacService.getUserRoles(userId)

        // Only certain roles can edit
        if (!roles.any { it in listOf("CONFIG_EDITOR", "CONFIG_APPROVER", "COMPLIANCE", "ADMIN") }) {
            logger.warn("User $userId lacks CONFIG_EDITOR role")
            return false
        }

        // Check scope-specific permissions
        if (scope == ConfigScope.BRAND) {
            val brandEditRole = "CONFIG_EDITOR_BRAND_$scopeId"
            if (!rbacService.hasRole(userId, brandEditRole) &&
                !rbacService.hasRole(userId, "ADMIN")) {
                logger.warn("User $userId not authorized for brand $scopeId")
                return false
            }
        }

        if (scope == ConfigScope.JURISDICTION) {
            val jurisdictionEditRole = "CONFIG_EDITOR_JURISDICTION_$scopeId"
            if (!rbacService.hasRole(userId, jurisdictionEditRole) &&
                !rbacService.hasRole(userId, "COMPLIANCE") &&
                !rbacService.hasRole(userId, "ADMIN")) {
                logger.warn("User $userId not authorized for jurisdiction $scopeId")
                return false
            }
        }

        return true
    }

    /**
     * Check if user can approve a parameter change
     */
    fun canApprove(
        paramKey: String,
        complianceDriven: Boolean,
        userId: String
    ): Boolean {
        val roles = rbacService.getUserRoles(userId)

        // Only approvers and admins
        if (!roles.any { it in listOf("CONFIG_APPROVER", "COMPLIANCE", "ADMIN") }) {
            logger.warn("User $userId lacks CONFIG_APPROVER role")
            return false
        }

        // Compliance params require COMPLIANCE or ADMIN role
        if (complianceDriven && !roles.any { it in listOf("COMPLIANCE", "ADMIN") }) {
            logger.warn("User $userId cannot approve compliance params")
            return false
        }

        return true
    }

    /**
     * Enforce maker-checker: Approver cannot be creator
     */
    fun validateMakerChecker(createdBy: String, approverUserId: String): Boolean {
        if (createdBy == approverUserId) {
            logger.warn("Maker-Checker violation: User $approverUserId cannot approve own change")
            return false
        }
        return true
    }
}

// Spring Security interceptor
@Component
class ConfigSecurityInterceptor(
    private val authService: ConfigAuthorizationService,
    private val logger: Logger
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val userId = SecurityContextHolder.getContext().authentication?.name
            ?: return false

        val paramKey = request.getParameter("paramKey") ?: ""
        val method = request.method

        val authorized = when {
            method == "GET" -> authService.canRead(paramKey, userId)
            method in listOf("PUT", "POST") && request.requestURI.contains("override") ->
                authService.canEdit(paramKey, ConfigScope.GLOBAL, null, userId)
            method == "POST" && request.requestURI.contains("approve") ->
                authService.canApprove(paramKey, true, userId)
            else -> false
        }

        if (!authorized) {
            logger.warn("Unauthorized access: $userId → $paramKey [$method]")
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
        }

        return authorized
    }
}
```

### 11.3 Sensitive Parameter Masking

```kotlin
@Component
class SensitiveParameterMasker(
    private val repository: ConfigParamRepository
) {

    fun maskValue(paramKey: String, value: String, userId: String): String {
        val paramDef = repository.getParamDefinition(paramKey) ?: return value

        if (!paramDef.isSensitive) {
            return value
        }

        // Check user has sensitive_read permission
        if (!hasPermission(userId, "config:read:sensitive")) {
            return "[MASKED]"
        }

        return value
    }

    fun maskForLogging(paramKey: String, value: String): String {
        val paramDef = repository.getParamDefinition(paramKey) ?: return value

        if (paramDef.isSensitive) {
            return "[REDACTED]"  // Never log sensitive values in audit
        }

        return value
    }

    private fun hasPermission(userId: String, permission: String): Boolean {
        // Check OAuth scope or RBAC role
        return true
    }
}
```

### 11.4 Encryption at Rest

```kotlin
@Configuration
class EncryptionConfiguration {

    /**
     * Encrypt sensitive parameter values before storage
     */
    @Bean
    fun parameterEncryption(): ParameterEncryption {
        val masterKey = System.getenv("CONFIG_MASTER_KEY")
            ?: throw IllegalStateException("CONFIG_MASTER_KEY not set")

        return ParameterEncryption(masterKey)
    }
}

@Component
class ParameterEncryption(private val masterKey: String) {

    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val spec = GCMParameterSpec(128, SecureRandom().generateSeed(12))

    fun encrypt(plaintext: String): String {
        val key = SecretKeySpec(masterKey.toByteArray(), 0, 32, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        return Base64.getEncoder().encodeToString(ciphertext)
    }

    fun decrypt(ciphertext: String): String {
        val key = SecretKeySpec(masterKey.toByteArray(), 0, 32, "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertext))
        return String(plaintext)
    }
}

// Example: Encrypt sensitive values on INSERT
@Repository
class ConfigParamRepositoryImpl {

    fun insertParamValue(value: ConfigParamValue, encryption: ParameterEncryption): ConfigParamValue {
        val paramDef = getParamDefinition(value.paramKey)

        val encryptedValue = if (paramDef.isSensitive) {
            encryption.encrypt(value.value)
        } else {
            value.value
        }

        // Insert with encrypted value
        // ...

        return value  // Return original plaintext to caller
    }
}
```

---

## T-CFG.12 對應業務文檔

This technical specification implements the requirements defined in:

**Primary Reference:**
[PRD: Configurable Parameters Registry — 可配置參數註冊表](file:///sessions/bold-epic-goldberg/mnt/IGaming/requirements/PRD_Configurable_Parameters_Registry_可配置參數註冊表.md)

**Key Alignments:**
- T-CFG.3 (Database Schema) implements PRD §5 (DB Schema Design)
- T-CFG.4 (Resolution Engine) implements PRD §4 (Three-Tier Override Hierarchy)
- T-CFG.5 (Caching) implements PRD §5.2 (Caching & Hot-Reload)
- T-CFG.7 (Maker-Checker) aligns with PRD compliance approval requirements
- T-CFG.9 (Migration) references all 845+ parameters from PRD §6 (Parameter Registry)

**Related Technical Docs:**
- Ch1: 玩家管理系統 (Player Management) — defines player KYC/AML parameters
- Ch2: 錢包系統 (Wallet System) — defines wallet, bonus, and settlement parameters
- ... (Ch3-16): Each chapter contains business-rule parameters to be migrated

**Approval:** [PENDING RON REVIEW]

---

## Summary Statistics

| Metric | Value |
|--------|-------|
| Total Configurable Parameters | 845+ |
| Database Tables | 3 (definition, value, audit_log) |
| API Endpoints | 7+ (CRUD, history, approval, bulk-import, diff) |
| Cache Layers | 3 (L1 local, L2 Redis, L3 database) |
| Three-Tier Scopes | Global, Brand, Jurisdiction |
| Approval Workflows | 2 (standard, compliance-driven dual approval) |
| Monitoring Metrics | 15+ KPIs with alerts |
| RBAC Roles | 5 primary + scoped variations |
| Documentation | 12 sections (this document) |
| Lines of Code (Est.) | 2,500-3,000 (core service + UI) |
| Test Coverage (Target) | >85% unit + integration tests |
| Deployment Strategy | Canary (10% → 25% → 50% → 100%) |
| Migration Phases | 5 (definition seed, initialization, overrides, validation, deployment) |
| Rollback Time (Est.) | <30 minutes with automated backup restore |

---

**Document Version:** v1.0
**Last Updated:** 2026-03-25
**Status:** DRAFT — Awaiting Architecture Review & Ron Approval
**Next Steps:**
1. Ron review & feedback
2. Tech design review with platform team
3. Capacity planning & sprint allocation
4. Development kickoff (Phase 1: config-service core)
