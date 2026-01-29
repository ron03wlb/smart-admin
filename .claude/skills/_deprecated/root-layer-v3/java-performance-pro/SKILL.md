---
name: java-performance-pro
description: Profile and optimize Java application performance including N+1 query detection, JVM memory tuning, CPU hotspot analysis, Redis cache optimization, and SQL query performance. Use when experiencing performance issues, slow endpoints, memory leaks, high CPU usage, or when optimizing for production scalability. Automatically triggered when user mentions "slow", "performance", "optimize", "N+1", "memory leak", "CPU usage", "cache miss", or "query optimization".
---

# Java Performance Profiling & Optimization

Profile Java applications and optimize performance bottlenecks with focus on MyBatis Plus N+1 queries, JVM tuning, cache optimization, and SQL performance.

## Quick Start

**Most common usage:**
```
User: "The employee listing endpoint is very slow, taking 5+ seconds"
User: "We're getting OutOfMemoryError in production"
User: "CPU usage is at 100% during peak hours"
User: "How do I optimize this N+1 query?"
```

You will:
1. Identify performance bottleneck category (DB, JVM, cache, CPU)
2. Profile using appropriate tool (JFR, VisualVM, Arthas, MyBatis logging)
3. Analyze metrics and identify root cause
4. Recommend optimizations with code examples
5. Measure improvement with before/after metrics

## Core Capabilities

### 1. N+1 Query Detection (MyBatis Plus)

**Problem**: Loop calling DAO methods triggers N+1 queries

**Detection Pattern:**
```java
// BAD: N+1 query pattern
List<EmployeeEntity> employees = employeeDao.selectList(query);
for (EmployeeEntity emp : employees) {
    // N+1: Query executed N times in loop
    DepartmentEntity dept = departmentDao.selectById(emp.getDepartmentId());
    emp.setDepartmentName(dept.getDepartmentName());
}
```

**Enable MyBatis SQL Logging:**
```yaml
# application.yml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**Look for repeated queries:**
```
==>  Preparing: SELECT * FROM t_department WHERE department_id = ?
==> Parameters: 1(Long)
<==    Total: 1
==>  Preparing: SELECT * FROM t_department WHERE department_id = ?
==> Parameters: 2(Long)
<==    Total: 1
==>  Preparing: SELECT * FROM t_department WHERE department_id = ?
==> Parameters: 3(Long)
<==    Total: 1
```

**Solution 1: JOIN Query**
```java
// GOOD: Single query with JOIN
@Select("SELECT e.*, d.department_name " +
        "FROM t_employee e " +
        "LEFT JOIN t_department d ON e.department_id = d.department_id " +
        "WHERE e.deleted_flag = false")
List<EmployeeVO> queryEmployeeWithDepartment();
```

**Solution 2: Batch Loading (MyBatis Plus)**
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

**Solution 3: Caching**
```java
@Manager
public class DepartmentCacheManager {
    @Cacheable(value = "department", key = "#deptId")
    public DepartmentEntity getDepartment(Long deptId) {
        return departmentDao.selectById(deptId);
    }
}

// Usage: Cache hit after first query
for (EmployeeEntity emp : employees) {
    DepartmentEntity dept = departmentCacheManager.getDepartment(emp.getDepartmentId());
}
```

---

### 2. JVM Memory Profiling

**Symptoms:**
- OutOfMemoryError: Java heap space
- OutOfMemoryError: GC overhead limit exceeded
- OutOfMemoryError: Metaspace
- Slow response times with high GC pauses

**Enable JFR (Java Flight Recorder):**
```bash
# Start application with JFR
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar app.jar

# Analyze with JDK Mission Control
jmc recording.jfr
```

**Heap Dump Analysis:**
```bash
# Generate heap dump (when OOM occurs)
jmap -dump:live,format=b,file=heap.bin <pid>

# Analyze with VisualVM or Eclipse MAT
visualvm --openfile heap.bin
```

**Common Issues & Fixes:**

**Issue 1: Large result sets in memory**
```java
// BAD: Loads all 1M records into memory
List<EmployeeEntity> all = employeeDao.selectList(null);

// GOOD: Use pagination
Page<EmployeeEntity> page = employeeDao.selectPage(
    new Page<>(1, 1000),
    Wrappers.<EmployeeEntity>lambdaQuery()
);
```

**Issue 2: Unbounded caches**
```java
// BAD: Unlimited cache growth
@Cacheable("employees")
public EmployeeEntity getEmployee(Long id) { }

// GOOD: Configure cache eviction
@Bean
public CacheManager cacheManager() {
    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
        ).build();
}
```

**Issue 3: String concatenation in loops**
```java
// BAD: Creates N temporary String objects
String result = "";
for (String s : list) {
    result += s; // New String object each iteration
}

// GOOD: Use StringBuilder
StringBuilder sb = new StringBuilder();
for (String s : list) {
    sb.append(s);
}
String result = sb.toString();
```

**JVM Tuning Recommendations:**
```bash
# For 8GB RAM server
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -jar app.jar
```

---

### 3. CPU Hotspot Analysis

**Enable CPU profiling with Arthas:**
```bash
# Attach to running JVM
java -jar arthas-boot.jar

# Profile CPU for 30 seconds
profiler start
# Wait 30 seconds
profiler stop --format html --file /tmp/profile.html
```

**Common CPU Hotspots:**

**Issue 1: Excessive regex compilation**
```java
// BAD: Compiles pattern on every call
public boolean validate(String input) {
    return input.matches("[a-z]+"); // Compiles regex each time
}

// GOOD: Compile once
private static final Pattern PATTERN = Pattern.compile("[a-z]+");

public boolean validate(String input) {
    return PATTERN.matcher(input).matches();
}
```

**Issue 2: Inefficient collection operations**
```java
// BAD: O(n²) complexity
List<Long> ids = new ArrayList<>();
for (EmployeeEntity emp : employees) {
    if (!ids.contains(emp.getDepartmentId())) { // O(n) lookup
        ids.add(emp.getDepartmentId());
    }
}

// GOOD: O(n) with Set
Set<Long> ids = employees.stream()
    .map(EmployeeEntity::getDepartmentId)
    .collect(Collectors.toSet());
```

**Issue 3: Synchronization bottlenecks**
```java
// BAD: Synchronized on every read
private Map<Long, String> cache = new HashMap<>();

public synchronized String get(Long key) {
    return cache.get(key);
}

// GOOD: Use ConcurrentHashMap
private Map<Long, String> cache = new ConcurrentHashMap<>();

public String get(Long key) {
    return cache.get(key);
}
```

---

### 4. Redis Cache Optimization

**Monitor cache hit ratio:**
```java
@Component
public class CacheMetrics {
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public void recordHit() { hits.incrementAndGet(); }
    public void recordMiss() { misses.incrementAndGet(); }

    public double getHitRatio() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0 : (double) hits.get() / total;
    }
}
```

**Cache key design:**
```java
// BAD: Cache key too broad (low hit ratio)
@Cacheable(value = "employees", key = "'all'")
public List<EmployeeEntity> queryAll() { }

// GOOD: Granular cache keys
@Cacheable(value = "employee", key = "#id")
public EmployeeEntity getById(Long id) { }

@Cacheable(value = "employee:dept", key = "#deptId")
public List<EmployeeEntity> getByDepartment(Long deptId) { }
```

**Cache eviction strategies:**
```java
@CacheEvict(value = "employee", key = "#entity.employeeId")
public void updateEmployee(EmployeeEntity entity) {
    employeeDao.updateById(entity);
}

@CacheEvict(value = "employee:dept", key = "#entity.departmentId")
public void addEmployee(EmployeeEntity entity) {
    employeeDao.insert(entity);
}
```

**Monitor Redis metrics:**
```bash
# Redis CLI
redis-cli

# Get cache stats
INFO stats
# Look for: keyspace_hits, keyspace_misses

# Monitor slow commands
SLOWLOG GET 10
```

---

### 5. SQL Query Optimization

**Enable SQL execution time logging:**
```yaml
# application.yml
logging:
  level:
    net.lab1024.sa: DEBUG
    org.springframework.jdbc.core: DEBUG
```

**Analyze slow queries:**
```sql
-- PostgreSQL: Enable slow query log
ALTER DATABASE smart_admin SET log_min_duration_statement = 1000; -- Log queries > 1s

-- MySQL: Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
```

**Common optimizations:**

**Issue 1: Missing indexes**
```sql
-- BAD: Full table scan
EXPLAIN SELECT * FROM t_employee WHERE login_name = 'admin';
-- Seq Scan on t_employee (cost=0.00..1000.00 rows=1)

-- GOOD: Add index
CREATE INDEX idx_employee_login_name ON t_employee(login_name);
-- Index Scan using idx_employee_login_name (cost=0.00..8.27 rows=1)
```

**Issue 2: Inefficient pagination**
```java
// BAD: OFFSET causes full scan of skipped rows
SELECT * FROM t_employee ORDER BY employee_id OFFSET 10000 LIMIT 10;

// GOOD: Use keyset pagination
SELECT * FROM t_employee
WHERE employee_id > :lastSeenId
ORDER BY employee_id
LIMIT 10;
```

**Issue 3: SELECT * wasteful**
```java
// BAD: Returns all columns
@Select("SELECT * FROM t_employee WHERE deleted_flag = false")
List<EmployeeEntity> queryAll();

// GOOD: Select only needed columns
@Select("SELECT employee_id, actual_name, phone FROM t_employee WHERE deleted_flag = false")
List<EmployeeSimpleVO> querySimple();
```

---

### 6. Connection Pool Tuning (HikariCP)

**Monitor connection pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000 # Enable leak detection
```

**Diagnose connection leaks:**
```bash
# Look for warnings in logs
WARN HikariPool - Connection leak detection triggered
```

**Fix connection leaks:**
```java
// BAD: Connection not closed
Connection conn = dataSource.getConnection();
Statement stmt = conn.createStatement();
// Missing close()

// GOOD: Try-with-resources
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement()) {
    // Auto-closes resources
}
```

---

## Performance Profiling Workflow

### Step 1: Identify Bottleneck

**Symptoms → Category:**
- Slow API responses → Database or N+1 queries
- OutOfMemoryError → JVM heap issues
- High CPU usage → Algorithm or synchronization
- Cache misses → Redis configuration
- Connection timeouts → Connection pool exhaustion

### Step 2: Profile with Appropriate Tool

| Category | Tool | Command |
|----------|------|---------|
| N+1 Queries | MyBatis SQL logging | Set `log-impl: StdOutImpl` |
| JVM Memory | JFR / VisualVM | `java -XX:StartFlightRecording` |
| CPU Hotspots | Arthas | `profiler start` |
| SQL Performance | EXPLAIN | `EXPLAIN SELECT ...` |
| Redis | redis-cli | `INFO stats` |

### Step 3: Analyze Metrics

**Look for:**
- Repeated SQL queries (N+1 pattern)
- High GC pause times (> 200ms)
- CPU-intensive methods (> 10% total time)
- Low cache hit ratio (< 80%)
- Slow SQL queries (> 1 second)

### Step 4: Apply Optimizations

**Priority order:**
1. Fix N+1 queries (biggest impact)
2. Add missing indexes
3. Optimize cache strategy
4. Tune JVM parameters
5. Refactor CPU-intensive code

### Step 5: Measure Improvement

**Before/After Metrics:**
```
Metric                | Before  | After   | Improvement
---------------------|---------|---------|------------
Response Time        | 5000ms  | 200ms   | 96% faster
Database Queries     | 101     | 1       | 99% reduction
Memory Usage         | 4GB     | 1GB     | 75% reduction
CPU Usage            | 100%    | 30%     | 70% reduction
Cache Hit Ratio      | 20%     | 95%     | 375% improvement
```

---

## Validation Checklist

**N+1 Query Detection:**
- [ ] MyBatis SQL logging enabled
- [ ] No repeated SELECT queries in logs
- [ ] JOINs used for related data OR batch loading
- [ ] Cache applied where appropriate

**JVM Memory:**
- [ ] Heap dump analyzed for large objects
- [ ] No unbounded caches or collections
- [ ] GC pause times < 200ms
- [ ] Max heap size appropriate for workload

**CPU Optimization:**
- [ ] No regex compilation in loops
- [ ] Efficient collection operations (avoid O(n²))
- [ ] No synchronized blocks in hot paths
- [ ] ConcurrentHashMap for concurrent reads

**Redis Cache:**
- [ ] Cache hit ratio > 80%
- [ ] Granular cache keys (not "all")
- [ ] Cache eviction configured
- [ ] TTL set appropriately

**SQL Performance:**
- [ ] Indexes created for WHERE/JOIN columns
- [ ] EXPLAIN shows Index Scan (not Seq Scan)
- [ ] Keyset pagination for large offsets
- [ ] SELECT specific columns (not SELECT *)

---

## References

Detailed profiling guides:
- [references/n-plus-one-detection.md](references/n-plus-one-detection.md) - MyBatis N+1 patterns and detection
- [references/jvm-tuning-guide.md](references/jvm-tuning-guide.md) - JVM memory and GC optimization
- [references/sql-optimization-patterns.md](references/sql-optimization-patterns.md) - SQL query optimization techniques

## Time Savings

**Manual Profiling:** 4-8 hours per performance issue
**Skill-Guided Profiling:** 1-2 hours (systematic approach)
**Time Saved: 60-75% reduction**

**Quality Improvements:**
- ✅ Systematic bottleneck identification
- ✅ Proven optimization patterns
- ✅ Before/after metrics for validation
- ✅ Production-ready tuning parameters
