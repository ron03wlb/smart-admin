---
name: smartadmin-performance-suite
description: [P2 - Productivity] Comprehensive performance suite with integrated diagnose → optimize → monitor workflow. Consolidates java-performance-pro (N+1 detection, JVM tuning), cache-strategy-generator (multi-level caching), and apm-integration-skill (Skywalking, Micrometer, Grafana) into a unified performance optimization orchestrator. Use when experiencing slow endpoints, high CPU usage, memory leaks, or setting up production observability. Triggers when user mentions "slow", "performance", "optimize", "monitoring", "APM", "cache", "N+1", or "Grafana".
---

# SmartAdmin Performance Suite

**Version**: 2.0.0 (Performance Suite Consolidation)
**Consolidates**: java-performance-pro, cache-strategy-generator, apm-integration-skill

Unified performance optimization solution providing integrated diagnose → optimize → monitor workflow for SmartAdmin applications.

---

## Quick Start

**Most common usage:**
```
User: "The employee listing endpoint is slow, taking 5+ seconds"
```

**AI Detection**:
- Mode: --workflow (default, runs all 3 phases)
- Execution: Diagnose → Optimize → Monitor
- Time: ~30 minutes (vs 60 minutes with separate skills)

---

## v2.0.0 Performance Suite Consolidation

**NEW**: This skill consolidates 3 previously separate performance skills into a unified workflow orchestrator.

### Consolidated Skills

This skill **replaces and consolidates**:
- ✅ **java-performance-pro** (Mode 1: Diagnose) → `--mode=diagnose`
- ✅ **cache-strategy-generator** (Mode 2: Optimize) → `--mode=optimize`
- ✅ **apm-integration-skill** (Mode 3: Monitor) → `--mode=monitor`

### Mode-Based Execution

**Complete Workflow (Default)**:
```bash
/performance /api/employees/list --workflow
# Runs: Diagnose → Optimize → Monitor
# Time: ~30 minutes
# Consolidates: All 3 skills in one command
```

**Diagnose Only**:
```bash
/performance /api/orders/query --mode=diagnose
# Detects: N+1 queries, JVM issues, CPU hotspots, SQL slow queries
# Time: ~10 minutes
# Consolidates: former /java-performance-pro
```

**Optimize Only**:
```bash
/performance ProductService.getById --mode=optimize
# Implements: Multi-level caching (Caffeine L1 + Redis L2)
# Time: ~12 minutes
# Consolidates: former /cache-strategy-generator
```

**Monitor Only**:
```bash
/performance OrderService --mode=monitor
# Configures: Skywalking, Micrometer, Grafana dashboards, alerts
# Time: ~8 minutes
# Consolidates: former /apm-integration-skill
```

### Backward Compatibility

**Old commands still work** with deprecation warnings (until 2026-06-30):

```bash
# ⚠️ Deprecated (routes to /performance --mode=diagnose)
/java-performance-pro analyze /api/employees/list

# ⚠️ Deprecated (routes to /performance --mode=optimize)
/cache-strategy ProductService.getById

# ⚠️ Deprecated (routes to /performance --mode=monitor)
/apm-integration UserService
```

**See**: [skill-aliases.json](../skill-aliases.json) for routing configuration

---

## Performance Modes

### Mode 1: Diagnose (--mode=diagnose)

**Purpose**: Identify performance bottlenecks with profiling and analysis

**Input Required**:
- Target endpoint URL or Service/Manager method
- Current performance metrics (response time, CPU usage, memory usage)
- Performance goals (target response time, throughput)

**Execution Steps**:
1. Read [modes/mode-1-diagnose.md](modes/mode-1-diagnose.md)
2. Enable MyBatis SQL logging (detect N+1 queries)
3. Profile with JFR/VisualVM/Arthas (JVM analysis)
4. Analyze slow query logs (PostgreSQL)
5. Identify bottleneck category (DB, JVM, cache, CPU)
6. Generate diagnosis report with root cause

**Output**:
```
Diagnosis Report:
├── Bottleneck Category: Database (N+1 queries)
├── Root Cause: Loop calling Dao.selectById(id) in EmployeeService.queryList()
├── Current Performance: 5.2s avg response time
├── Expected Improvement: ~80% reduction (1.0s target)
└── Recommended Actions:
    1. Implement JOIN query in EmployeeDao.xml
    2. Add Caffeine L1 cache for department lookup
    3. Enable Redis L2 cache for hot data
```

**Time**: ~10 minutes
**Consolidates**: java-performance-pro

---

### Mode 2: Optimize (--mode=optimize)

**Purpose**: Implement caching strategies and query optimizations

**Input Required**:
- Diagnosis report from Mode 1
- Cache requirements (TTL, eviction policy, cache size)
- Query optimization strategy (JOIN, batch loading, indexing)

**Execution Steps**:
1. Read [modes/mode-2-optimize.md](modes/mode-2-optimize.md)
2. Implement multi-level caching:
   - L1 Caffeine (in-memory, fast)
   - L2 Redis (distributed, persistent)
3. Choose cache pattern:
   - Cache-aside (lazy loading)
   - Write-through (synchronous update)
   - Write-behind (asynchronous update)
4. Implement cache invalidation:
   - TTL-based (time expiration)
   - Event-driven (immediate update)
   - Manual flush (on demand)
5. Configure cache warming (startup pre-load)
6. Add distributed locks (prevent cache stampede)
7. Set up cache hit rate monitoring

**Output**:
```
Generated Files:
├── CacheConfiguration.java (Caffeine + Redis config)
├── {Entity}CacheManager.java (cache operations)
├── {Entity}Service.java (updated with @Cacheable, @CacheEvict)
└── cache-metrics.yml (Micrometer metrics configuration)

Performance Improvement:
├── Before: 5.2s avg response time
├── After: 0.8s avg response time
└── Improvement: 84.6% reduction
```

**Time**: ~12 minutes
**Consolidates**: cache-strategy-generator

---

### Mode 3: Monitor (--mode=monitor)

**Purpose**: Set up production observability and alerting

**Input Required**:
- Service/endpoint to monitor
- Alert thresholds (P95/P99 latency, error rate, throughput)
- Grafana dashboard requirements (JVM, HTTP, DB, Redis, Custom metrics)

**Execution Steps**:
1. Read [modes/mode-3-monitor.md](modes/mode-3-monitor.md)
2. Configure Skywalking Java agent:
   - Auto-instrumentation for HTTP, JDBC, Redis
   - Distributed tracing (TraceId, SpanId)
   - Log correlation (MDC integration)
3. Generate Micrometer custom metrics:
   - Counters (login events, order count)
   - Gauges (active users, queue size)
   - Timers (method execution time)
   - Histograms (response size distribution)
4. Create Grafana dashboard JSON:
   - JVM panel (heap, GC, threads)
   - HTTP panel (requests/sec, P95/P99 latency, error rate)
   - Database panel (query time, connection pool)
   - Redis panel (hit rate, operations/sec)
   - Custom business metrics panel
5. Generate alert rules:
   - Latency: P95 > 500ms, P99 > 1000ms
   - Error rate: > 1% over 5 minutes
   - Throughput: < 100 req/sec during peak hours
6. Enable MDC log correlation (TraceId in logs)

**Output**:
```
Generated Files:
├── skywalking-agent.config (agent configuration)
├── MetricsConfiguration.java (Micrometer setup)
├── {Entity}Metrics.java (custom metrics)
├── grafana-dashboard-{entity}.json (dashboard template)
└── alert-rules-{entity}.yml (Prometheus alert rules)

Monitoring Setup:
├── Skywalking: ✅ Distributed tracing enabled
├── Micrometer: ✅ Custom metrics registered
├── Grafana: ✅ Dashboard ready (http://grafana:3000/d/{entity}-perf)
└── Alerts: ✅ 5 rules configured
```

**Time**: ~8 minutes
**Consolidates**: apm-integration-skill

---

## Integrated Workflow (--workflow)

**Purpose**: Complete performance optimization pipeline (Diagnose → Optimize → Monitor)

**When to Use**:
- New performance issue reported
- Setting up production monitoring for new service
- Comprehensive performance audit

**Execution Sequence**:

1. **Phase 1: Diagnose** (~10 minutes)
   - Profile endpoint/service
   - Identify bottlenecks
   - Generate diagnosis report

2. **Phase 2: Optimize** (~12 minutes)
   - Implement caching strategies
   - Optimize queries
   - Validate improvements

3. **Phase 3: Monitor** (~8 minutes)
   - Configure Skywalking
   - Create Grafana dashboard
   - Set up alert rules

**Total Time**: ~30 minutes (vs 60 minutes with separate skills)

**Example**:
```bash
# User request
User: "The employee listing endpoint /api/employees/list is slow, taking 5+ seconds.
      We need it under 1 second for production."

# AI Response
AI: "I'll use the Performance Suite to diagnose, optimize, and set up monitoring.
    Estimated time: ~30 minutes"

# Execution
/performance /api/employees/list --workflow

# Phase 1: Diagnose
- Detected N+1 query: EmployeeService.queryList() calls DepartmentDao.selectById() in loop
- Current: 5.2s avg response time (N=50 employees, 50 department queries)
- Root cause: Missing JOIN, no caching

# Phase 2: Optimize
- Implemented JOIN query in EmployeeDao.xml
- Added Caffeine L1 cache for department lookup (5min TTL)
- Added Redis L2 cache for hot data (30min TTL)
- Configured cache warming on startup
- Result: 0.8s avg response time (84.6% improvement)

# Phase 3: Monitor
- Configured Skywalking distributed tracing
- Created Grafana dashboard: http://grafana:3000/d/employee-perf
- Set up alerts: P95 > 500ms, P99 > 1000ms, Error rate > 1%
- Enabled MDC log correlation (TraceId in logs)

# Final Report
✅ Performance improved: 5.2s → 0.8s (84.6% reduction)
✅ Target achieved: < 1.0s response time
✅ Monitoring active: Dashboard + Alerts configured
```

---

## Mode Detection Logic

When user makes a request, determine performance mode:

**Mode 1: Diagnose (--mode=diagnose)**
- **Triggers**:
  - User says "slow endpoint", "high CPU", "memory leak", "N+1 query"
  - User mentions "profile", "analyze", "find bottleneck"
  - User provides performance metrics (response time, CPU usage)
  - Former `/java-performance-pro` command users
- **Time**: ~10 minutes

**Mode 2: Optimize (--mode=optimize)**
- **Triggers**:
  - User says "add caching", "optimize queries", "improve performance"
  - User mentions "Caffeine", "Redis", "cache-aside", "cache warming"
  - User has diagnosis report with optimization recommendations
  - Former `/cache-strategy-generator` command users
- **Time**: ~12 minutes

**Mode 3: Monitor (--mode=monitor)**
- **Triggers**:
  - User says "add monitoring", "setup APM", "create dashboard", "alerts"
  - User mentions "Skywalking", "Grafana", "Micrometer", "distributed tracing"
  - User wants production observability
  - Former `/apm-integration-skill` command users
- **Time**: ~8 minutes

**Integrated Workflow (--workflow)** [Default]
- **Triggers**:
  - User says "optimize performance", "fix slow endpoint"
  - User mentions "complete performance solution"
  - No specific mode requested
  - New performance issue reported
- **Time**: ~30 minutes

---

## Core Patterns

### Pattern 1: N+1 Query Detection and Fix

**Goal**: Eliminate N+1 queries in MyBatis Plus

**Problem**:
```java
// BAD: N+1 query pattern
List<EmployeeEntity> employees = employeeDao.selectList(query);
for (EmployeeEntity emp : employees) {
    // N+1: Query executed N times in loop
    DepartmentEntity dept = departmentDao.selectById(emp.getDepartmentId());
    emp.setDepartmentName(dept.getDepartmentName());
}
```

**Solution 1: JOIN Query**:
```java
// GOOD: Single query with JOIN
@Select("SELECT e.*, d.department_name " +
        "FROM t_employee e " +
        "LEFT JOIN t_department d ON e.department_id = d.department_id " +
        "WHERE e.deleted_flag = false")
List<EmployeeVO> queryEmployeeWithDepartment();
```

**Solution 2: Batch Loading**:
```java
// GOOD: Batch load departments
List<EmployeeEntity> employees = employeeDao.selectList(query);

// Collect all department IDs
List<Long> deptIds = employees.stream()
    .map(EmployeeEntity::getDepartmentId)
    .distinct()
    .collect(Collectors.toList());

// Single batch query
List<DepartmentEntity> departments = departmentDao.selectBatchIds(deptIds);
Map<Long, DepartmentEntity> deptMap = departments.stream()
    .collect(Collectors.toMap(DepartmentEntity::getDepartmentId, dept -> dept));

// Map results
employees.forEach(emp -> {
    DepartmentEntity dept = deptMap.get(emp.getDepartmentId());
    if (dept != null) {
        emp.setDepartmentName(dept.getDepartmentName());
    }
});
```

### Pattern 2: Multi-Level Caching

**Goal**: Implement L1 (Caffeine) + L2 (Redis) caching

**Configuration**:
```java
@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // L1: Caffeine (in-memory, fast)
        CaffeineCache l1Cache = new CaffeineCache("l1Cache",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build());

        // L2: Redis (distributed, persistent)
        RedisCacheConfiguration redisCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));

        RedisCacheManager l2CacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisCacheConfig)
            .transactionAware()
            .build();

        // Compose L1 + L2
        return new CompositeCacheManager(l1Cache, l2CacheManager);
    }
}
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentManager departmentManager;

    @Cacheable(value = "department", key = "#id")
    public DepartmentVO getById(Long id) {
        return departmentManager.getById(id);
    }

    @CacheEvict(value = "department", key = "#form.departmentId")
    public ResponseDTO<String> updateDepartment(DepartmentUpdateForm form) {
        return departmentManager.updateDepartment(form);
    }
}
```

### Pattern 3: Distributed Tracing with Skywalking

**Goal**: Enable distributed tracing across all layers

**Skywalking Agent Configuration**:
```properties
# skywalking-agent.config
agent.service_name=smartadmin-api
collector.backend_service=skywalking-oap:11800

# Auto-instrumentation plugins
plugin.jdbc.trace_sql_parameters=true
plugin.redis.trace_redis_parameters=true
plugin.http.trace_headers=true
```

**MDC Log Correlation**:
```java
@Component
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            // Get TraceId from Skywalking
            String traceId = TraceContext.traceId();
            MDC.put("traceId", traceId);

            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
```

**Logback Configuration**:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

---

## Validation After Each Mode

### Mode 1 Validation (Diagnose)

- [ ] MyBatis SQL logging enabled
- [ ] N+1 queries detected (if applicable)
- [ ] JFR/VisualVM profile captured
- [ ] Bottleneck category identified (DB, JVM, cache, CPU)
- [ ] Root cause documented
- [ ] Optimization recommendations generated
- [ ] Current performance metrics recorded

### Mode 2 Validation (Optimize)

- [ ] Multi-level cache configured (Caffeine L1 + Redis L2)
- [ ] Cache pattern implemented (cache-aside, write-through, etc.)
- [ ] Cache invalidation strategy configured (TTL, event-driven)
- [ ] Cache warming implemented (if applicable)
- [ ] Distributed locks configured (Redisson)
- [ ] Cache hit rate metrics enabled
- [ ] Performance improvement validated (before/after metrics)
- [ ] Query optimization implemented (JOIN, batch loading)

### Mode 3 Validation (Monitor)

- [ ] Skywalking agent configured
- [ ] Distributed tracing enabled (TraceId propagation)
- [ ] Micrometer custom metrics registered
- [ ] Grafana dashboard created (JVM, HTTP, DB, Redis, Custom panels)
- [ ] Alert rules configured (latency, error rate, throughput)
- [ ] MDC log correlation enabled (TraceId in logs)
- [ ] Dashboard accessible: http://grafana:3000/d/{entity}-perf
- [ ] Alerts firing correctly (test with load)

---

## Troubleshooting

### Common Issues

**Problem**: "MyBatis SQL logging shows duplicate queries but no N+1 pattern"
- **Cause**: Queries are legitimate (different parameters)
- **Solution**: Check if queries can be batched with `IN` clause or JOIN

**Problem**: "Cache hit rate is low (<50%)"
- **Cause**: TTL too short, cache size too small, or cache warming not configured
- **Solution**: Increase TTL, increase cache size, implement cache warming on startup

**Problem**: "Skywalking trace not showing in dashboard"
- **Cause**: Agent not started, collector unreachable, or service name mismatch
- **Solution**: Check agent logs (`skywalking-agent.log`), verify collector URL, ensure service name matches dashboard filter

**Problem**: "Grafana dashboard shows no data"
- **Cause**: Micrometer metrics not registered, Prometheus scraping failed, or time range incorrect
- **Solution**: Verify metrics endpoint (`/actuator/prometheus`), check Prometheus targets, adjust dashboard time range

---

## Time Savings

**Before Consolidation** (separate skills):
- Diagnose (java-performance-pro): 15 minutes
- Optimize (cache-strategy-generator): 20 minutes
- Monitor (apm-integration-skill): 15 minutes
- Manual coordination: 10 minutes
- **Total: 60 minutes**

**After Consolidation** (unified suite):
- One command execution: 30 minutes (auto-runs all 3 phases)
- No coordination overhead
- **Total: 30 minutes**

**Time Savings**: 60 min → 30 min (50% improvement)

---

## Version History

**v2.0.0** (2026-01-27):
- Consolidated 3 performance skills into mode-based workflow
- Added --workflow mode for integrated diagnose → optimize → monitor pipeline
- Reduced performance investigation time: 60 min → 30 min (50% improvement)
- Backward compatibility via skill-aliases.json
- Unified performance orchestration

**v1.0.0** (2025-12-01):
- Initial separate performance skills (java-performance-pro, cache-strategy-generator, apm-integration-skill)
