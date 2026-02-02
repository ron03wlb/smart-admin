# SmartAdmin Performance Suite - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: smartadmin-performance-suite (P2 - Productivity/Composite)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Full Performance Audit | Run all performance checks | ~30 min |
| Database Analysis | PostgreSQL + N+1 detection | ~10 min |
| JVM Profiling | CPU + Memory analysis | ~15 min |
| Load Testing | Stress test endpoints | ~20 min |
| Report Generation | Comprehensive report | ~5 min |

---

## Performance Suite Overview

**Composite Skill** - Integrates multiple performance analysis skills:

1. **[PostgreSQL Best Practices](../../integration/postgresql-best-practices/)** - Database optimization
2. **[Java Performance Pro](../../analysis/java-performance-pro/)** - JVM profiling
3. **[APM Integration](../../devops/apm-integration-skill/)** - Real-time monitoring

**Selection Matrix**:

| Scenario | Use PostgreSQL | Use Java Perf | Use APM | Time |
|----------|---------------|---------------|---------|------|
| Database timeout | ✅ Primary | ⚠️ Check N+1 | ✅ Monitor | 15 min |
| High CPU usage | ❌ | ✅ Primary | ✅ Profile | 20 min |
| Memory leak | ❌ | ✅ Primary | ✅ Track | 25 min |
| Slow endpoints | ✅ Check SQL | ✅ Profile code | ✅ Trace | 30 min |
| Full audit | ✅ All checks | ✅ All checks | ✅ Setup | 60 min |

---

## Integrated Workflow

### Step 1: Initial Diagnosis (5 minutes)

```bash
# Quick health check
curl http://localhost:1024/actuator/health
curl http://localhost:1024/actuator/metrics

# Check current load
top -p $(pgrep -f sa-admin)
```

**Decision Tree**:
- High CPU (>80%) → **Java Performance Pro** (CPU profiling)
- High Memory (>85%) → **Java Performance Pro** (heap dump)
- Database errors → **PostgreSQL Best Practices** (connection pool + N+1)
- All metrics normal → **APM Integration** (long-term monitoring)

---

### Step 2: Database Analysis (10 minutes)

**Use**: [PostgreSQL Best Practices](../../integration/postgresql-best-practices/)

```bash
# 1. HikariCP utilization
./gradlew :sa-admin:test --tests HikariCPAnalyzerTest

# Expected output:
# - Utilization: 45% ✅ (target: <80%)
# - Active connections: 9/20
# - Waiting threads: 0

# 2. N+1 query detection
grep "SELECT.*FROM" logs/spy.log | sort | uniq -c | sort -rn | head -10

# 3. Slow query analysis
psql -c "SELECT query, calls, total_time/calls as avg_time
         FROM pg_stat_statements
         WHERE total_time/calls > 1000
         ORDER BY avg_time DESC LIMIT 10;"
```

**Fix Priority**:
1. N+1 queries (>50 similar queries)
2. Missing indexes (Seq Scan in EXPLAIN)
3. Pool exhaustion (utilization >80%)

**Time**: 10-15 minutes

---

### Step 3: JVM Profiling (15 minutes)

**Use**: [Java Performance Pro](../../analysis/java-performance-pro/)

**CPU Profiling**:
```bash
# Profile CPU for 60 seconds
./profiler.sh -d 60 -e cpu -f flamegraph.html $(pgrep -f sa-admin)

# Analyze flame graph
open flamegraph.html

# Look for:
# 1. Wide bars = CPU hotspots
# 2. Regex compilation in loops
# 3. Reflection calls
```

**Memory Analysis**:
```bash
# Heap dump
jmap -dump:live,format=b,file=heap.hprof $(pgrep -f sa-admin)

# Analyze with Eclipse MAT
./mat heap.hprof

# Check for:
# 1. Large collections (>10MB)
# 2. ThreadLocal leaks
# 3. Static caches
```

**Time**: 15-20 minutes

---

### Step 4: Load Testing (20 minutes)

**Use**: Apache JMeter or Gatling

**JMeter Test Plan**:
```xml
<!-- SmartAdmin Load Test -->
<ThreadGroup>
  <numThreads>100</numThreads>
  <rampTime>60</rampTime>
  <duration>300</duration>

  <HTTPSampler>
    <path>/api/employee/query</path>
    <method>POST</method>
    <body>{"pageNum":1,"pageSize":20}</body>
  </HTTPSampler>
</ThreadGroup>
```

**Gatling Scenario**:
```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SmartAdminLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:1024")
    .acceptHeader("application/json")

  val scn = scenario("Employee Query")
    .exec(http("Query Employees")
      .post("/api/employee/query")
      .body(StringBody("""{"pageNum":1,"pageSize":20}"""))
      .check(status.is(200))
      .check(jsonPath("$.code").is("0"))
    )

  setUp(
    scn.inject(
      rampUsers(100) during (60 seconds)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.max.lt(2000),
    global.successfulRequests.percent.gt(99)
  )
}
```

**Run Test**:
```bash
# JMeter
jmeter -n -t smartadmin-load-test.jmx -l results.jtl

# Gatling
./gradlew gatlingRun
```

**Success Criteria**:
- Response time p99 < 2s
- Success rate > 99%
- No errors under 100 concurrent users

**Time**: 20-30 minutes

---

### Step 5: APM Setup (Long-term Monitoring)

**Use**: [APM Integration](../../devops/apm-integration-skill/)

**SkyWalking + Prometheus + Grafana**:

```bash
# 1. Start SkyWalking
docker-compose up -d skywalking-oap skywalking-ui

# 2. Run app with agent
java -javaagent:/opt/skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=smartadmin-api \
     -jar sa-admin.jar

# 3. Setup Grafana dashboard
# Import dashboard ID: 12900 (Spring Boot 2.1 System Monitor)
```

**Key Metrics to Monitor**:
- Request rate (requests/sec)
- Response time (p50, p95, p99)
- Error rate (%)
- JVM heap usage (%)
- Database connection pool (active/idle)
- GC pause time (ms)

**Time**: 15-20 minutes

---

## Performance Optimization Checklist

### Database (Priority: HIGH)

- [ ] HikariCP utilization < 80%
- [ ] No N+1 queries detected
- [ ] All queries < 1s execution time
- [ ] Critical indexes created (Seq Scan eliminated)
- [ ] Connection leaks fixed (try-with-resources)

**Tools**: PostgreSQL Best Practices skill

---

### JVM (Priority: MEDIUM)

- [ ] CPU hotspots identified and fixed
- [ ] No memory leaks (heap stable over time)
- [ ] GC pauses < 200ms
- [ ] No regex compilation in loops
- [ ] ThreadLocal cleaned up properly

**Tools**: Java Performance Pro skill

---

### Application (Priority: MEDIUM)

- [ ] Response time p99 < 2s
- [ ] Success rate > 99% under load
- [ ] No synchronization bottlenecks
- [ ] Caching implemented for hot data
- [ ] Async processing for heavy tasks

**Tools**: Load testing + APM

---

### Infrastructure (Priority: LOW)

- [ ] Monitoring dashboards created
- [ ] Alerts configured (CPU, Memory, Errors)
- [ ] Log aggregation setup (ELK/Loki)
- [ ] Distributed tracing enabled
- [ ] Resource limits configured (CPU/Memory)

**Tools**: APM Integration skill

---

## Common Performance Issues & Solutions

### Issue 1: Database Timeout (97.6% case from production)

**Symptoms**:
- `HikariPool-1 - Connection is not available`
- Request timeout after 30s

**Root Causes** (from PostgreSQL skill):
1. HikariCP pool too small
2. N+1 query in `EmployeeService.listEmployees()`
3. Missing index on `t_employee(dept_id, status)`

**Solution**:
```yaml
# Increase pool size
spring.datasource.hikari.maximum-pool-size: 20  # Was: 10

# Fix N+1 (replace loop with JOIN)
# Add index
CREATE INDEX idx_employee_dept_status ON t_employee(dept_id, status);
```

**Result**: 97.6% reduction (1,234 → 30 errors/day)

---

### Issue 2: High CPU Usage

**Symptoms**:
- CPU constantly >80%
- Slow response times

**Root Causes** (from Java Perf skill):
1. Regex compilation in loop
2. Excessive String concatenation
3. Unnecessary reflection

**Solution**:
```java
// Fix regex
private static final Pattern PATTERN = Pattern.compile("\\d+");

// Fix String concat
StringBuilder sb = new StringBuilder();
for (String s : list) {
    sb.append(s);
}

// Cache reflection
private static final Field FIELD = Employee.class.getDeclaredField("name");
```

**Result**: 70% CPU reduction

---

### Issue 3: Memory Leak

**Symptoms**:
- Memory usage grows continuously
- Eventually OutOfMemoryError

**Root Causes** (from Java Perf skill):
1. Static collections never cleared
2. ThreadLocal not removed
3. Connection/ResultSet not closed

**Solution**:
```java
// Fix static collection
private final Cache<String, Object> cache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .maximumSize(1000)
    .build();

// Fix ThreadLocal
RequestContext.clear();  // Always call in finally block

// Fix resources
try (Connection conn = dataSource.getConnection()) {
    // Auto-closed
}
```

**Result**: Memory stable, no OOM

---

## Integrated Performance Report

**Example Report Structure**:

```markdown
# SmartAdmin Performance Audit Report

**Date**: 2026-02-02
**Duration**: 30 minutes
**Status**: ⚠️ ISSUES FOUND

---

## Executive Summary

- **Overall Health**: MEDIUM (3 critical issues)
- **Database**: ⚠️ N+1 detected, pool utilization 85%
- **JVM**: ⚠️ CPU hotspot in regex, memory leak potential
- **Application**: ✅ Response times acceptable (<2s)

---

## Findings

### 1. Database Issues (CRITICAL)

**Issue**: N+1 query in `EmployeeService.listEmployees()`
- **Impact**: 150 queries per request
- **Fix**: Add JOIN in `employeeDao.selectAllWithDepartment()`
- **Priority**: P0 (Fix within 24h)

**Issue**: HikariCP utilization 85%
- **Impact**: Connection timeouts under load
- **Fix**: Increase `maximum-pool-size` to 20
- **Priority**: P1 (Fix within 1 week)

### 2. JVM Issues (HIGH)

**Issue**: Regex compilation in loop
- **Location**: `ValidatorUtil.java:45`
- **Impact**: 30% CPU usage
- **Fix**: Compile Pattern once (static final)
- **Priority**: P1 (Fix within 1 week)

### 3. Recommendations

1. Implement caching for hot data (department list)
2. Enable APM monitoring (SkyWalking)
3. Schedule monthly performance audits

---

## Performance Metrics

| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| Response Time (p99) | 1.8s | <2s | ✅ |
| Database Pool Utilization | 85% | <80% | ⚠️ |
| CPU Usage | 75% | <70% | ⚠️ |
| Memory Usage | 65% | <70% | ✅ |
| Error Rate | 0.1% | <1% | ✅ |

---

## Next Steps

1. Fix N+1 query (2 hours)
2. Increase HikariCP pool size (15 minutes)
3. Fix regex hotspot (30 minutes)
4. Re-run audit (30 minutes)
```

---

## Time Estimates

| Task | Duration |
|------|----------|
| Initial Diagnosis | 5 min |
| Database Analysis | 10 min |
| JVM Profiling | 15 min |
| Load Testing | 20 min |
| APM Setup | 15 min |
| Report Generation | 5 min |
| **Total Audit** | **70 min** |

**Fix Implementation**: 2-6 hours (depends on issues found)

---

**See Also**:
- [PostgreSQL Best Practices](../../integration/postgresql-best-practices/) - Database optimization
- [Java Performance Pro](../../analysis/java-performance-pro/) - JVM profiling
- [APM Integration](../../devops/apm-integration-skill/) - Monitoring setup
