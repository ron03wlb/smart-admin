# P2-21: A/B Testing Framework

**Version**: 1.0
**Last Updated**: 2026-01-23
**Status**: Draft
**Priority**: P2 (Enhancement)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Requirements](#2-requirements)
3. [Architecture Design](#3-architecture-design)
4. [Feature Flag Management](#4-feature-flag-management)
5. [A/B Test Design](#5-ab-test-design)
6. [Traffic Splitting Algorithm](#6-traffic-splitting-algorithm)
7. [Variant Allocation & Sticky Sessions](#7-variant-allocation--sticky-sessions)
8. [Database Schema Design](#8-database-schema-design)
9. [Metrics Collection & Analysis](#9-metrics-collection--analysis)
10. [SmartAdmin Implementation](#10-smartadmin-implementation)
11. [Statistical Significance](#11-statistical-significance)
12. [Testing Strategy](#12-testing-strategy)
13. [Operations & Monitoring](#13-operations--monitoring)
14. [Appendices](#14-appendices)

---

## 1. Background & Strategic Context

### 1.1 Business Context

**From [igame_str.md](../../igame_str.md) - Data Leverage**:
> "Data leverage enables intelligent decisions. Every player interaction generates data that compounds into competitive moats."

**From [backend_project.md](../../backend_project.md) Section 3.10**:
> "Performance optimization and profiling ensure 10K TPS throughput with <200ms p95 latency. Data-driven decisions guide architectural improvements."

### 1.2 Problem Statement

**Current Limitation**:
- Product changes deployed to 100% of players immediately (binary: all-or-nothing)
- No mechanism to test different bonus amounts, UI variations, or feature toggles
- Cannot measure incremental revenue impact of changes before full rollout
- Risk of revenue loss if untested change negatively impacts conversion

**Business Impact**:
- **Revenue Risk**: $50K revenue loss from poorly tested bonus change (actual incident)
- **Missed Optimization**: 15% conversion uplift discovered after full rollout (too late to iterate)
- **Slow Iteration**: 2-week release cycle prevents rapid experimentation

### 1.3 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Experiment Setup Time** | <30 minutes | From idea to live experiment |
| **Traffic Allocation Accuracy** | ±1% of target split | 50/50 split = 49-51% actual distribution |
| **Variant Consistency** | 100% sticky sessions | Player sees same variant across sessions |
| **Statistical Confidence** | 95% confidence level | Chi-square test p-value < 0.05 |
| **Metrics Latency** | <5 seconds | Real-time metrics dashboard update |

---

## 2. Requirements

### 2.1 Functional Requirements

**FR-ABT-001: Feature Flag Management**
- Enable/disable features without code deployment
- Percentage-based rollouts (e.g., enable for 10% of players, gradually increase)
- Tenant-specific feature flags (merchant A has feature, merchant B doesn't)
- User segment targeting (e.g., only VIP players, only mobile users)

**FR-ABT-002: A/B Test Configuration**
- Define experiment with multiple variants (A/B/C/... up to 10 variants)
- Configure traffic allocation per variant (e.g., 40% A, 30% B, 30% C)
- Set experiment start/end dates
- Define success metrics (conversion rate, revenue, retention)

**FR-ABT-003: Variant Assignment**
- Deterministic assignment: Same player always gets same variant
- Randomized assignment: Evenly distribute players across variants
- Segment-based assignment: VIP players get variant A, others get B
- Override mechanism: Force specific players into specific variants (QA testing)

**FR-ABT-004: Metrics Tracking**
- Track conversion events (registration, deposit, first bet)
- Track revenue metrics (GGR, deposits, withdrawals)
- Track engagement metrics (session duration, game rounds played)
- Real-time metrics aggregation (not batch, updated every 5 seconds)

**FR-ABT-005: Statistical Analysis**
- Calculate statistical significance (Chi-square test, t-test)
- Detect winner with 95% confidence level
- Sample size calculator (how many players needed for significance?)
- Multi-armed bandit optimization (optional: auto-allocate traffic to winning variant)

### 2.2 Non-Functional Requirements

**NFR-ABT-001: Performance**
- Variant assignment: <5ms p95 latency (in-memory lookup)
- Metrics ingestion: 10,000 events/second
- Dashboard query: <2 seconds for 30-day experiment data

**NFR-ABT-002: Scalability**
- Support 100+ concurrent experiments across all tenants
- Handle 1M+ players per experiment

**NFR-ABT-003: Consistency**
- Variant assignment is sticky (player sees same variant across devices)
- No variant bleeding (player never switches variants mid-experiment)

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Player Request                              │
│  GET /api/game/launch?game_id=123                               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│               ABTestInterceptor (Spring MVC)                     │
│  - Check active experiments for this tenant                     │
│  - Assign player to variant (deterministic hash)                │
│  - Set variant in ABTestContextHolder                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Business Logic (Service)                        │
│                                                                  │
│  if (ABTestService.isVariantActive("new_bonus_flow", "variant_b")) {  │
│      // New bonus flow with 200% bonus                          │
│      applyBonus(playerId, 2.00);                                │
│  } else {                                                        │
│      // Control: 100% bonus                                     │
│      applyBonus(playerId, 1.00);                                │
│  }                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Metrics Collection                             │
│  ABTestMetricsService.trackConversion(playerId, "deposit", amount); │
│                                                                  │
│  Kafka Topic: ab-test-events                                    │
│  {                                                               │
│    "experiment_id": "exp_001",                                   │
│    "variant": "variant_b",                                       │
│    "player_id": 12345,                                           │
│    "event": "deposit",                                           │
│    "value": 100.00,                                              │
│    "timestamp": "2026-01-23T14:30:00Z"                          │
│  }                                                               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Flink Stream Processing                          │
│  - Aggregate metrics by (experiment_id, variant)                │
│  - Calculate running totals (conversions, revenue)               │
│  - Compute statistical significance (Chi-square, t-test)         │
│  - Update metrics in Redis (real-time dashboard)                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Admin Dashboard (Vue 3)                        │
│  - Real-time metrics visualization (Chart.js)                   │
│  - Confidence intervals and p-values                            │
│  - Winner declaration with statistical badge                    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Key Design Decisions

**Decision 1: Deterministic Hash-Based Assignment**
- **Pattern**: `variant = hash(player_id + experiment_id) % num_variants`
- **Rationale**: Ensures same player always gets same variant, no database lookup needed
- **Trade-off**: Cannot dynamically re-balance traffic (must create new experiment)

**Decision 2: Kafka + Flink for Real-Time Metrics**
- **Pattern**: Events → Kafka → Flink → Redis (metrics cache)
- **Rationale**: <5s latency for dashboard updates, handles 10K events/sec
- **Alternative Rejected**: Batch processing (30-minute delay unacceptable)

**Decision 3: Multi-Tenancy via Experiment Scope**
- **Pattern**: Each experiment scoped to tenant_id
- **Rationale**: Merchant A can run different experiments than Merchant B
- **Implementation**: `WHERE tenant_id = :tenantId AND experiment_id = :expId`

**Decision 4: Feature Flags as Degenerate A/B Tests**
- **Pattern**: Feature flag = A/B test with 1 variant (on/off)
- **Rationale**: Unified codebase, same infrastructure
- **Benefit**: Easy to convert feature flag to A/B test later

---

## 4. Feature Flag Management

### 4.1 Feature Flag Configuration

**Example**: Gradual Rollout of New VIP Dashboard

```java
@Service
@RequiredArgsConstructor
public class VipDashboardService {

    private final ABTestService abTestService;

    public DashboardVO getDashboard(Long playerId) {
        // Check feature flag "new_vip_dashboard" (percentage-based rollout)
        if (abTestService.isFeatureEnabled("new_vip_dashboard", playerId)) {
            return getNewVipDashboard(playerId);  // New UI with gamification
        } else {
            return getOldVipDashboard(playerId);  // Legacy UI
        }
    }
}
```

**Admin UI**: Feature Flag Configuration

```json
{
  "feature_flag_id": "new_vip_dashboard",
  "tenant_id": "merchant_001",
  "enabled": true,
  "rollout_percentage": 25,  // Enable for 25% of players
  "targeting_rules": {
    "vip_tier": ["GOLD", "PLATINUM", "DIAMOND"],  // Only VIP players
    "country": ["BR", "MX"],  // Only Brazil and Mexico
    "platform": ["mobile"]  // Only mobile users
  },
  "start_date": "2026-01-20T00:00:00Z",
  "end_date": null  // No end date (permanent feature)
}
```

### 4.2 Feature Flag Evaluation Logic

```java
@Service
@RequiredArgsConstructor
public class ABTestService {

    private final RedisService redisService;
    private final PlayerDao playerDao;

    /**
     * Check if feature flag is enabled for given player
     *
     * @param featureFlagId Feature flag ID
     * @param playerId Player ID
     * @return true if feature enabled for this player
     */
    public boolean isFeatureEnabled(String featureFlagId, Long playerId) {
        String tenantId = TenantContextHolder.getTenantId();

        // Fetch feature flag config from Redis cache
        String cacheKey = String.format("feature_flag:%s:%s", tenantId, featureFlagId);
        FeatureFlagConfig config = redisService.get(cacheKey);

        if (config == null || !config.isEnabled()) {
            return false;  // Feature disabled
        }

        // Check if player matches targeting rules
        if (!matchesTargetingRules(playerId, config.getTargetingRules())) {
            return false;
        }

        // Check percentage rollout (deterministic hash)
        int hash = Math.abs((playerId.toString() + featureFlagId).hashCode());
        int bucket = hash % 100;  // 0-99

        return bucket < config.getRolloutPercentage();
    }

    private boolean matchesTargetingRules(Long playerId, TargetingRules rules) {
        if (rules == null) {
            return true;  // No targeting rules = all players
        }

        PlayerEntity player = playerDao.selectById(playerId);

        // VIP tier filter
        if (rules.getVipTiers() != null &&
            !rules.getVipTiers().contains(player.getVipTier())) {
            return false;
        }

        // Country filter
        if (rules.getCountries() != null &&
            !rules.getCountries().contains(player.getCountry())) {
            return false;
        }

        // Platform filter
        if (rules.getPlatforms() != null &&
            !rules.getPlatforms().contains(player.getPlatform())) {
            return false;
        }

        return true;
    }
}
```

---

## 5. A/B Test Design

### 5.1 Experiment Configuration

**Example**: Test 3 Different Welcome Bonus Amounts

```json
{
  "experiment_id": "exp_welcome_bonus_2026_q1",
  "tenant_id": "merchant_001",
  "name": "Welcome Bonus Amount Test",
  "description": "Test which welcome bonus amount maximizes first deposit conversion",
  "status": "RUNNING",
  "start_date": "2026-01-20T00:00:00Z",
  "end_date": "2026-02-20T00:00:00Z",
  "variants": [
    {
      "variant_id": "control",
      "name": "100% Bonus",
      "traffic_allocation": 0.33,
      "config": { "bonus_percentage": 1.00 }
    },
    {
      "variant_id": "variant_a",
      "name": "150% Bonus",
      "traffic_allocation": 0.33,
      "config": { "bonus_percentage": 1.50 }
    },
    {
      "variant_id": "variant_b",
      "name": "200% Bonus",
      "traffic_allocation": 0.34,
      "config": { "bonus_percentage": 2.00 }
    }
  ],
  "success_metrics": [
    {
      "metric_id": "first_deposit_conversion",
      "type": "CONVERSION",
      "goal": "MAXIMIZE"
    },
    {
      "metric_id": "revenue_per_player",
      "type": "REVENUE",
      "goal": "MAXIMIZE"
    }
  ]
}
```

### 5.2 Business Logic Integration

```java
@Service
@RequiredArgsConstructor
public class BonusManager {

    private final ABTestService abTestService;

    @Transactional(rollbackFor = Exception.class)
    public void grantWelcomeBonus(Long playerId, BigDecimal depositAmount) {
        // Get variant for player
        ABTestVariant variant = abTestService.getVariant("exp_welcome_bonus_2026_q1", playerId);

        // Extract bonus percentage from variant config
        double bonusPercentage = variant != null
            ? variant.getConfig().getDouble("bonus_percentage")
            : 1.00;  // Default 100% if no experiment running

        BigDecimal bonusAmount = depositAmount.multiply(BigDecimal.valueOf(bonusPercentage));

        // Grant bonus
        walletManager.credit(playerId, bonusAmount, "USD", "Welcome Bonus");

        // Track conversion event for A/B test
        if (variant != null) {
            abTestMetricsService.trackConversion(
                "exp_welcome_bonus_2026_q1",
                variant.getVariantId(),
                playerId,
                "first_deposit",
                depositAmount
            );
        }
    }
}
```

---

## 6. Traffic Splitting Algorithm

### 6.1 Deterministic Hash-Based Assignment

**Algorithm**:

```
variant_index = hash(player_id + experiment_id + salt) % num_variants
variant = variants[variant_index]
```

**Properties**:
- ✅ Deterministic: Same player always gets same variant
- ✅ Uniform: Evenly distributes players across variants
- ✅ Fast: O(1) lookup, no database query
- ❌ Not re-balanceable: Cannot change traffic allocation mid-experiment

**Java Implementation**:

```java
public ABTestVariant assignVariant(String experimentId, Long playerId) {
    ABTestExperiment experiment = getExperiment(experimentId);

    if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
        return null;  // No experiment or not running
    }

    // Deterministic hash
    String hashInput = playerId.toString() + experimentId + experiment.getSalt();
    int hash = Math.abs(hashInput.hashCode());

    // Cumulative distribution function (CDF) for traffic allocation
    List<ABTestVariant> variants = experiment.getVariants();
    double[] cumulativeAllocation = new double[variants.size()];
    double sum = 0.0;

    for (int i = 0; i < variants.size(); i++) {
        sum += variants.get(i).getTrafficAllocation();
        cumulativeAllocation[i] = sum;
    }

    // Normalize hash to [0.0, 1.0)
    double normalizedHash = (hash % 10000) / 10000.0;

    // Find variant based on CDF
    for (int i = 0; i < cumulativeAllocation.length; i++) {
        if (normalizedHash < cumulativeAllocation[i]) {
            return variants.get(i);
        }
    }

    // Fallback to last variant (should never happen)
    return variants.get(variants.size() - 1);
}
```

**Example**:

Experiment with 3 variants:
- Control: 40% traffic
- Variant A: 30% traffic
- Variant B: 30% traffic

CDF: `[0.40, 0.70, 1.00]`

Player with hash 0.25 → Control (0.25 < 0.40)
Player with hash 0.55 → Variant A (0.40 ≤ 0.55 < 0.70)
Player with hash 0.85 → Variant B (0.70 ≤ 0.85 < 1.00)

---

## 7. Variant Allocation & Sticky Sessions

### 7.1 Variant Assignment Table

**Database Storage**:

```sql
CREATE TABLE t_ab_test_assignment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    experiment_id VARCHAR(128) NOT NULL,
    player_id BIGINT NOT NULL,
    variant_id VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, experiment_id, player_id)
);

CREATE INDEX idx_ab_test_assignment_lookup
    ON t_ab_test_assignment(tenant_id, experiment_id, player_id);
```

### 7.2 Sticky Session Guarantees

**Problem**: Player sees variant A on mobile, then variant B on desktop (inconsistent experience)

**Solution**: Store assignment in database + Redis cache

```java
@Service
@RequiredArgsConstructor
public class ABTestAssignmentManager {

    private final ABTestAssignmentDao assignmentDao;
    private final RedisService redisService;

    @Transactional(rollbackFor = Exception.class)
    public ABTestVariant getOrAssignVariant(String experimentId, Long playerId) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Check Redis cache first (fast path)
        String cacheKey = String.format("ab_test:%s:%s:%d", tenantId, experimentId, playerId);
        String variantId = redisService.get(cacheKey);

        if (variantId != null) {
            return getVariantById(experimentId, variantId);
        }

        // 2. Check database (player already assigned?)
        ABTestAssignment assignment = assignmentDao.selectOne(
            new LambdaQueryWrapper<ABTestAssignment>()
                .eq(ABTestAssignment::getTenantId, tenantId)
                .eq(ABTestAssignment::getExperimentId, experimentId)
                .eq(ABTestAssignment::getPlayerId, playerId)
        );

        if (assignment != null) {
            // Cache for 24 hours
            redisService.set(cacheKey, assignment.getVariantId(), Duration.ofHours(24));
            return getVariantById(experimentId, assignment.getVariantId());
        }

        // 3. New player - assign variant
        ABTestVariant variant = assignVariant(experimentId, playerId);

        // Store assignment in database
        assignment = new ABTestAssignment();
        assignment.setTenantId(tenantId);
        assignment.setExperimentId(experimentId);
        assignment.setPlayerId(playerId);
        assignment.setVariantId(variant.getVariantId());
        assignmentDao.insert(assignment);

        // Cache for 24 hours
        redisService.set(cacheKey, variant.getVariantId(), Duration.ofHours(24));

        return variant;
    }
}
```

---

## 8. Database Schema Design

### 8.1 Experiment Table

```sql
CREATE TABLE t_ab_test_experiment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    experiment_id VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,  -- DRAFT, RUNNING, PAUSED, COMPLETED
    salt VARCHAR(64) NOT NULL,  -- Random salt for hash function
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, experiment_id)
);

CREATE INDEX idx_ab_test_experiment_status
    ON t_ab_test_experiment(tenant_id, status);
```

### 8.2 Variant Table

```sql
CREATE TABLE t_ab_test_variant (
    id BIGSERIAL PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES t_ab_test_experiment(id),
    variant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    traffic_allocation DECIMAL(5, 4) NOT NULL,  -- 0.0000 to 1.0000
    config JSONB,  -- Variant-specific configuration
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(experiment_id, variant_id)
);

CREATE INDEX idx_ab_test_variant_experiment
    ON t_ab_test_variant(experiment_id);
```

### 8.3 Metrics Table (Materialized View)

```sql
CREATE TABLE t_ab_test_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    experiment_id VARCHAR(128) NOT NULL,
    variant_id VARCHAR(64) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,  -- IMPRESSION, CONVERSION, REVENUE
    total_count BIGINT DEFAULT 0,
    total_value DECIMAL(20, 4) DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, experiment_id, variant_id, metric_type)
);

CREATE INDEX idx_ab_test_metrics_lookup
    ON t_ab_test_metrics(tenant_id, experiment_id);
```

---

## 9. Metrics Collection & Analysis

### 9.1 Event Tracking

```java
@Service
@RequiredArgsConstructor
public class ABTestMetricsService {

    private final KafkaTemplate<String, ABTestEvent> kafkaTemplate;

    /**
     * Track conversion event for A/B test
     *
     * @param experimentId Experiment ID
     * @param variantId Variant ID
     * @param playerId Player ID
     * @param eventType Event type (impression, click, conversion)
     * @param value Event value (revenue, count)
     */
    public void trackEvent(String experimentId, String variantId, Long playerId,
                          String eventType, BigDecimal value) {
        ABTestEvent event = new ABTestEvent();
        event.setTenantId(TenantContextHolder.getTenantId());
        event.setExperimentId(experimentId);
        event.setVariantId(variantId);
        event.setPlayerId(playerId);
        event.setEventType(eventType);
        event.setValue(value);
        event.setTimestamp(Instant.now());

        // Send to Kafka topic: ab-test-events
        kafkaTemplate.send("ab-test-events", experimentId, event);
    }

    /**
     * Convenience method for conversion tracking
     */
    public void trackConversion(String experimentId, String variantId,
                               Long playerId, String conversionType, BigDecimal value) {
        trackEvent(experimentId, variantId, playerId, "conversion:" + conversionType, value);
    }
}
```

### 9.2 Flink Stream Aggregation

**Flink Job**: Real-time metrics aggregation

```java
public class ABTestMetricsAggregationJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka source
        FlinkKafkaConsumer<ABTestEvent> source = new FlinkKafkaConsumer<>(
            "ab-test-events",
            new ABTestEventDeserializer(),
            kafkaProperties
        );

        DataStream<ABTestEvent> events = env.addSource(source);

        // Aggregate by (experiment_id, variant_id, metric_type)
        DataStream<ABTestMetrics> metrics = events
            .keyBy(event -> Tuple3.of(
                event.getExperimentId(),
                event.getVariantId(),
                event.getEventType()
            ))
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new ABTestMetricsAggregator());

        // Sink to Redis for real-time dashboard
        metrics.addSink(new RedisSink<>(redisConfig));

        // Also sink to PostgreSQL for historical analysis
        metrics.addSink(new PostgreSQLSink<>(postgresConfig));

        env.execute("AB Test Metrics Aggregation");
    }

    static class ABTestMetricsAggregator
        implements AggregateFunction<ABTestEvent, ABTestMetrics, ABTestMetrics> {

        @Override
        public ABTestMetrics createAccumulator() {
            return new ABTestMetrics();
        }

        @Override
        public ABTestMetrics add(ABTestEvent event, ABTestMetrics acc) {
            acc.setExperimentId(event.getExperimentId());
            acc.setVariantId(event.getVariantId());
            acc.setMetricType(event.getEventType());
            acc.setTotalCount(acc.getTotalCount() + 1);
            acc.setTotalValue(acc.getTotalValue().add(event.getValue()));
            return acc;
        }

        @Override
        public ABTestMetrics getResult(ABTestMetrics acc) {
            return acc;
        }

        @Override
        public ABTestMetrics merge(ABTestMetrics a, ABTestMetrics b) {
            a.setTotalCount(a.getTotalCount() + b.getTotalCount());
            a.setTotalValue(a.getTotalValue().add(b.getTotalValue()));
            return a;
        }
    }
}
```

---

## 10. SmartAdmin Implementation

### 10.1 Controller Layer

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/abtest/controller/ABTestController.java`

```java
@RestController
@RequestMapping("/abtest")
@RequiredArgsConstructor
@Tag(name = "A/B Test Management")
public class ABTestController {

    private final ABTestService abTestService;

    @PostMapping("/experiment/create")
    @SaCheckPermission("abtest:experiment:create")
    public ResponseDTO<ABTestExperiment> createExperiment(@RequestBody @Valid ABTestExperimentForm form) {
        ABTestExperiment experiment = abTestService.createExperiment(form);
        return ResponseDTO.ok(experiment);
    }

    @GetMapping("/experiment/{experimentId}/metrics")
    @SaCheckPermission("abtest:experiment:view")
    public ResponseDTO<ABTestMetricsVO> getMetrics(@PathVariable String experimentId) {
        ABTestMetricsVO metrics = abTestService.getExperimentMetrics(experimentId);
        return ResponseDTO.ok(metrics);
    }

    @PostMapping("/experiment/{experimentId}/stop")
    @SaCheckPermission("abtest:experiment:manage")
    public ResponseDTO<Void> stopExperiment(@PathVariable String experimentId,
                                           @RequestBody @Valid StopExperimentForm form) {
        abTestService.stopExperiment(experimentId, form.getWinningVariantId());
        return ResponseDTO.ok();
    }
}
```

### 10.2 Service Layer

```java
@Service
@RequiredArgsConstructor
public class ABTestService {

    private final ABTestExperimentDao experimentDao;
    private final ABTestAssignmentManager assignmentManager;

    public ABTestVariant getVariant(String experimentId, Long playerId) {
        return assignmentManager.getOrAssignVariant(experimentId, playerId);
    }

    public boolean isVariantActive(String experimentId, String variantId) {
        ABTestVariant variant = ABTestContextHolder.getVariant(experimentId);
        return variant != null && variant.getVariantId().equals(variantId);
    }
}
```

---

## 11. Statistical Significance

### 11.1 Chi-Square Test for Conversion Rate

**Formula**:

```
χ² = Σ [(Observed - Expected)² / Expected]

p-value = P(χ² > observed_chi_square)

If p-value < 0.05, reject null hypothesis (variants are different with 95% confidence)
```

**Java Implementation**:

```java
@Service
@RequiredArgsConstructor
public class ABTestStatisticsService {

    /**
     * Calculate statistical significance using Chi-square test
     *
     * @return p-value (if < 0.05, result is statistically significant)
     */
    public double calculatePValue(ABTestMetrics controlMetrics, ABTestMetrics variantMetrics) {
        long controlConversions = controlMetrics.getConversionCount();
        long controlTotal = controlMetrics.getTotalCount();
        long variantConversions = variantMetrics.getConversionCount();
        long variantTotal = variantMetrics.getTotalCount();

        // Expected conversions
        double totalConversions = controlConversions + variantConversions;
        double totalSamples = controlTotal + variantTotal;
        double expectedRate = totalConversions / totalSamples;

        double expectedControl = controlTotal * expectedRate;
        double expectedVariant = variantTotal * expectedRate;

        // Chi-square statistic
        double chiSquare =
            Math.pow(controlConversions - expectedControl, 2) / expectedControl +
            Math.pow(variantConversions - expectedVariant, 2) / expectedVariant;

        // P-value from chi-square distribution (df=1)
        ChiSquaredDistribution distribution = new ChiSquaredDistribution(1);
        double pValue = 1 - distribution.cumulativeProbability(chiSquare);

        return pValue;
    }

    /**
     * Calculate required sample size for given parameters
     *
     * @param baselineRate Control conversion rate (e.g., 0.10 = 10%)
     * @param minimumDetectableEffect Minimum effect to detect (e.g., 0.02 = 2% absolute increase)
     * @param alpha Significance level (typically 0.05)
     * @param power Statistical power (typically 0.80)
     * @return Required sample size per variant
     */
    public int calculateSampleSize(double baselineRate, double minimumDetectableEffect,
                                  double alpha, double power) {
        NormalDistribution normal = new NormalDistribution();
        double zAlpha = normal.inverseCumulativeProbability(1 - alpha / 2);
        double zBeta = normal.inverseCumulativeProbability(power);

        double p1 = baselineRate;
        double p2 = baselineRate + minimumDetectableEffect;
        double pBar = (p1 + p2) / 2;

        double n = Math.pow(zAlpha + zBeta, 2) *
                  (p1 * (1 - p1) + p2 * (1 - p2)) /
                  Math.pow(p1 - p2, 2);

        return (int) Math.ceil(n);
    }
}
```

---

## 12. Testing Strategy

### 12.1 Unit Tests

```java
@SpringBootTest
class ABTestAssignmentTest {

    @Autowired
    private ABTestAssignmentManager assignmentManager;

    @Test
    void testDeterministicAssignment() {
        String experimentId = "exp_test_001";
        Long playerId = 12345L;

        // Assign variant
        ABTestVariant variant1 = assignmentManager.getOrAssignVariant(experimentId, playerId);
        ABTestVariant variant2 = assignmentManager.getOrAssignVariant(experimentId, playerId);

        // Should get same variant every time (deterministic)
        assertThat(variant1.getVariantId()).isEqualTo(variant2.getVariantId());
    }

    @Test
    void testTrafficAllocation() {
        String experimentId = "exp_test_002";

        // Assign 10,000 players
        Map<String, Integer> distribution = new HashMap<>();
        for (long playerId = 1; playerId <= 10000; playerId++) {
            ABTestVariant variant = assignmentManager.getOrAssignVariant(experimentId, playerId);
            distribution.merge(variant.getVariantId(), 1, Integer::sum);
        }

        // Expected: 50% control, 50% variant_a (±1% tolerance)
        int controlCount = distribution.get("control");
        int variantCount = distribution.get("variant_a");

        assertThat(controlCount).isBetween(4900, 5100);  // 49-51%
        assertThat(variantCount).isBetween(4900, 5100);  // 49-51%
    }
}
```

---

## 13. Operations & Monitoring

### 13.1 Metrics

```promql
# Experiment participation rate
rate(ab_test_assignments_total[5m])

# Variant distribution
sum by (variant_id) (ab_test_assignments_total)

# Conversion rate by variant
sum(ab_test_conversions_total) / sum(ab_test_impressions_total) by (variant_id)

# Statistical significance
ab_test_p_value{experiment_id="exp_001"} < 0.05
```

### 13.2 Alerts

```yaml
- alert: ABTestLowParticipation
  expr: rate(ab_test_assignments_total[1h]) < 100
  annotations:
    summary: "A/B test {{ $labels.experiment_id }} has low participation rate"

- alert: ABTestImbalancedTraffic
  expr: abs(ab_test_variant_ratio - 0.5) > 0.05
  annotations:
    summary: "A/B test {{ $labels.experiment_id }} has imbalanced traffic split"
```

---

## 14. Appendices

### 14.1 Sample Size Calculator (UI)

**Input**:
- Baseline conversion rate: 10%
- Minimum detectable effect: 2% (absolute)
- Significance level: 95%
- Statistical power: 80%

**Output**:
- Required sample size: 3,842 players **per variant**
- Estimated duration: 12 days (at 640 players/day)

### 14.2 Cross-References

- [P1-13: Reporting & Analytics](../P1-important/13-reporting-analytics.md) - Metrics collection
- [backend_project.md](../../backend_project.md) - Overall architecture

---

**Document End**
