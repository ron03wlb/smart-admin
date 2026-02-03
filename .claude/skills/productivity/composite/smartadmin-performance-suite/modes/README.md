# Performance Suite Modes

This directory contains execution modes for the Performance Suite. Each mode integrates content from previously separate skills.

---

## Mode Overview

| Mode | Purpose | Time | Consolidates |
|------|---------|------|--------------|
| **Mode 1: Diagnose** | Identify performance bottlenecks | ~10 min | java-performance-pro |
| **Mode 2: Optimize** | Implement caching and query optimizations | ~12 min | cache-strategy-generator |
| **Mode 3: Monitor** | Set up APM and observability | ~8 min | apm-integration |
| **Workflow** | Complete pipeline (all 3 modes) | ~30 min | All 3 skills |

---

## Detailed Documentation

### Mode 1: Diagnose

**Purpose**: Profile and identify performance bottlenecks

**Detailed Content**: See [../../java-performance-pro/SKILL.md](../../java-performance-pro/SKILL.md)

**Key Capabilities**:
- N+1 query detection (MyBatis Plus SQL logging)
- JVM profiling (JFR, VisualVM, Arthas)
- CPU hotspot analysis
- Memory leak detection
- SQL slow query analysis
- Database connection pool monitoring

**Typical Output**:
```
Diagnosis Report:
├── Bottleneck: Database (N+1 queries)
├── Root Cause: Loop calling Dao.selectById() in EmployeeService
├── Current: 5.2s avg response time
├── Target: <1.0s response time
└── Recommendations:
    1. Implement JOIN query
    2. Add L1/L2 caching
    3. Enable connection pooling
```

---

### Mode 2: Optimize

**Purpose**: Implement multi-level caching and query optimizations

**Detailed Content**: See [../../cache-strategy-generator/SKILL.md](../../cache-strategy-generator/SKILL.md)

**Key Capabilities**:
- Multi-level cache configuration (Caffeine L1 + Redis L2)
- Cache patterns (cache-aside, write-through, write-behind, read-through)
- Cache invalidation strategies (TTL, event-driven, manual flush)
- Cache warming (startup pre-load)
- Distributed locks (Redisson for cache stampede prevention)
- Cache hit rate monitoring
- Query optimization (JOIN, batch loading, indexing)

**Typical Output**:
```
Generated Files:
├── CacheConfiguration.java
├── {Entity}CacheManager.java
├── {Entity}Service.java (updated with @Cacheable)
└── cache-metrics.yml

Performance:
├── Before: 5.2s
├── After: 0.8s
└── Improvement: 84.6%
```

---

### Mode 3: Monitor

**Purpose**: Set up production APM and observability

**Detailed Content**: See [../../apm-integration/SKILL.md](../../apm-integration/SKILL.md)

**Key Capabilities**:
- Skywalking Java agent configuration (auto-instrumentation)
- Micrometer custom metrics (counters, gauges, timers, histograms)
- Grafana dashboard templates (JVM, HTTP, DB, Redis, Custom panels)
- Alert rule generation (latency P95/P99, error rate, throughput)
- Distributed tracing (TraceId, SpanId propagation)
- MDC log correlation (TraceId in logs)

**Typical Output**:
```
Generated Files:
├── skywalking-agent.config
├── MetricsConfiguration.java
├── {Entity}Metrics.java
├── grafana-dashboard-{entity}.json
└── alert-rules-{entity}.yml

Monitoring:
├── Skywalking: ✅ Enabled
├── Grafana: ✅ http://grafana:3000/d/{entity}-perf
└── Alerts: ✅ 5 rules configured
```

---

## Integrated Workflow

**Default Mode**: --workflow (runs all 3 phases sequentially)

**Execution Sequence**:
1. **Diagnose** (~10 min): Profile and identify bottlenecks
2. **Optimize** (~12 min): Implement caching and query fixes
3. **Monitor** (~8 min): Set up APM and dashboards

**Total Time**: ~30 minutes (vs 60 minutes with separate skills)

**When to Use**:
- New performance issue reported
- Comprehensive performance audit
- Setting up production monitoring for new service

---

## Mode Selection Guide

### User Says → Mode

| User Request | Mode | Reason |
|--------------|------|--------|
| "Find why endpoint is slow" | diagnose | Need bottleneck analysis |
| "Add caching to ProductService" | optimize | Specific optimization task |
| "Set up Grafana dashboard" | monitor | Observability setup |
| "Optimize /api/orders/list endpoint" | workflow | Complete pipeline needed |
| "High CPU usage in production" | diagnose | Performance profiling |
| "Implement Redis cache" | optimize | Caching implementation |
| "Enable distributed tracing" | monitor | APM setup |

---

## References

- **[java-performance-pro](../../java-performance-pro/)**: Complete N+1 detection, JVM tuning, profiling guides
- **[cache-strategy-generator](../../cache-strategy-generator/)**: Detailed caching patterns, invalidation strategies
- **[apm-integration](../../apm-integration/)**: Skywalking, Micrometer, Grafana configuration guides

---

**Version**: 2.0.0 (Performance Suite Consolidation)
**Last Updated**: 2026-01-27
