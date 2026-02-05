# Java Performance Pro - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: java-performance-pro (P2 - Productivity/Analysis)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| CPU Profiling | Analyze CPU hotspots with async-profiler | ~5 min |
| Memory Analysis | Detect memory leaks with heap dump | ~10 min |
| N+1 Detection | Identify N+1 query patterns | ~3 min |
| JVM Tuning | Optimize GC and heap settings | ~15 min |
| SQL Optimization | Analyze and optimize slow queries | ~8 min |

### Rapid Development Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Profile | Collect performance data (CPU, Memory, SQL) | ~10 min |
| 2. Analyze | Identify bottlenecks and hotspots | ~15 min |
| 3. Optimize | Apply fixes (code, JVM, SQL) | ~30-60 min |
| 4. Verify | Re-profile to confirm improvements | ~10 min |

**Total**: ~65-95 minutes per optimization cycle

---

## Performance Analysis Pattern Matrix

### Pattern 1: CPU Profiling (async-profiler)

**Trigger Keywords**: "high CPU", "performance profiling", "hotspot analysis"

**Use When**: CPU usage >70% or slow response times

**async-profiler Setup**:
```bash
# Download async-profiler
wget https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz

# Profile for 60 seconds (CPU + allocation)
./profiler.sh -d 60 -e cpu,alloc -f flamegraph.html <PID>
```

**Flame Graph Analysis**:
```
Hotspots to look for:
1. Wide bars = High CPU consumption
2. Tall stacks = Deep call chains
3. Flat tops = Leaf methods (optimization targets)
```

**Common Hotspots**:
| Pattern | Fix | Impact |
|---------|-----|--------|
| Regex in loop | Compile Pattern once | 80% faster |
| String concatenation | Use StringBuilder | 70% faster |
| Reflection | Cache Method/Field | 90% faster |
| Unnecessary object creation | Reuse objects | 50% memory |

**Example Fix (Regex)**:
```java
// ❌ BAD - Compiles regex every iteration
for (String line : lines) {
    if (line.matches("\\d{3}-\\d{4}")) {  // Slow!
        process(line);
    }
}

// ✅ GOOD - Compile once
private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{3}-\\d{4}");

for (String line : lines) {
    if (PHONE_PATTERN.matcher(line).matches()) {  // Fast!
        process(line);
    }
}
```

**Time to Profile**: 5-10 minutes
**Time to Fix**: 10-30 minutes per hotspot

---

### Pattern 2: Memory Leak Detection (Heap Dump Analysis)

**Trigger Keywords**: "OutOfMemoryError", "memory leak", "heap analysis"

**Use When**: Memory usage grows continuously or OOM errors

**Heap Dump Generation**:
```bash
# Manual heap dump
jmap -dump:live,format=b,file=heap.hprof <PID>

# Auto heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -jar app.jar
```

**Eclipse MAT Analysis**:
```bash
# Open heap dump in Eclipse MAT
./mat heap.hprof

# Look for:
1. Leak Suspects Report (auto-generated)
2. Dominator Tree (largest objects)
3. Histogram (object count by class)
```

**Common Memory Leaks**:

**Leak 1: Static Collections**
```java
// ❌ BAD - Never cleared
public class CacheService {
    private static final Map<String, Object> CACHE = new HashMap<>();

    public void cache(String key, Object value) {
        CACHE.put(key, value);  // Memory leak!
    }
}

// ✅ GOOD - Use Caffeine with expiry
@Service
public class CacheService {
    private final Cache<String, Object> cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();
}
```

**Leak 2: Unclosed Resources**
```java
// ❌ BAD - Connection not closed
public void query() {
    Connection conn = dataSource.getConnection();
    PreparedStatement ps = conn.prepareStatement(sql);
    ResultSet rs = ps.executeQuery();
    // Forgot to close!
}

// ✅ GOOD - Try-with-resources
public void query() {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        // Auto-closed
    }
}
```

**Leak 3: ThreadLocal Not Removed**
```java
// ❌ BAD - ThreadLocal leak in thread pool
public class RequestContext {
    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static void setUser(User user) {
        USER_CONTEXT.set(user);  // Never removed!
    }
}

// ✅ GOOD - Always remove after use
public class RequestContext {
    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static void setUser(User user) {
        USER_CONTEXT.set(user);
    }

    public static void clear() {
        USER_CONTEXT.remove();  // Must call after request!
    }
}
```

**Time to Analyze**: 10-15 minutes
**Time to Fix**: 20-40 minutes per leak

---

### Pattern 3: N+1 Query Detection

**Trigger Keywords**: "N+1 query", "slow database", "excessive queries"

**Use When**: Endpoint makes hundreds of DB queries

**Detection Methods**:

**Method 1: P6Spy Log Count**
```properties
# Enable P6Spy query logging
spring.datasource.url=jdbc:p6spy:postgresql://localhost:5432/smartadmin
```

**Method 2: Code Inspection**
```java
// ❌ N+1 Pattern (BAD)
@Service
public class EmployeeService {
    public List<EmployeeVO> listEmployees() {
        List<Employee> employees = employeeDao.selectAll();  // 1 query

        return employees.stream()
            .map(emp -> {
                Department dept = departmentDao.selectById(emp.getDeptId());  // N queries!
                return toVO(emp, dept);
            })
            .collect(Collectors.toList());
    }
}

// ✅ Fixed with JOIN (GOOD)
@Service
public class EmployeeService {
    public List<EmployeeVO> listEmployees() {
        return employeeDao.selectAllWithDepartment();  // 1 query with JOIN
    }
}
```

**MyBatis Optimization**:
```xml
<!-- ❌ BAD - Lazy loading causes N+1 -->
<resultMap id="EmployeeMap" type="Employee">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <association property="department" column="dept_id"
                 select="selectDepartmentById"/>  <!-- N+1! -->
</resultMap>

<!-- ✅ GOOD - Eager loading with JOIN -->
<resultMap id="EmployeeMap" type="Employee">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <association property="department" javaType="Department">
        <id property="id" column="dept_id"/>
        <result property="name" column="dept_name"/>
    </association>
</resultMap>

<select id="selectAllWithDepartment" resultMap="EmployeeMap">
    SELECT e.*, d.name as dept_name
    FROM t_employee e
    LEFT JOIN t_department d ON e.dept_id = d.id
</select>
```

**Time to Detect**: 3-5 minutes
**Time to Fix**: 10-20 minutes per query

---

### Pattern 4: JVM Tuning (GC Optimization)

**Trigger Keywords**: "GC pause", "JVM tuning", "heap optimization"

**Use When**: GC pauses >100ms or frequent Full GC

**GC Analysis**:
```bash
# Enable GC logging (Java 11+)
java -Xlog:gc*:file=gc.log:time,uptime,level,tags \
     -XX:+UseG1GC \
     -Xms2g -Xmx4g \
     -jar app.jar

# Analyze GC log
gceasy.io  # Upload gc.log for analysis
```

**Recommended GC Settings (SmartAdmin)**:

**For Web Applications (Low Latency)**:
```bash
# G1GC (Recommended for most cases)
-XX:+UseG1GC
-Xms2g -Xmx4g                          # Heap: 2-4GB
-XX:MaxGCPauseMillis=200               # Target: 200ms pause
-XX:G1HeapRegionSize=16m               # Region size: 16MB
-XX:InitiatingHeapOccupancyPercent=45  # Start concurrent GC at 45%
-XX:+ParallelRefProcEnabled            # Parallel reference processing
```

**For Batch Processing (High Throughput)**:
```bash
# Parallel GC (Maximize throughput)
-XX:+UseParallelGC
-Xms4g -Xmx8g                          # Heap: 4-8GB
-XX:ParallelGCThreads=8                # GC threads: 8
-XX:MaxGCPauseMillis=500               # Accept longer pauses
```

**For Microservices (Small Heap)**:
```bash
# Serial GC or G1GC (Small heap)
-XX:+UseG1GC
-Xms512m -Xmx1g                        # Heap: 512MB-1GB
-XX:MaxGCPauseMillis=100               # Low latency target
```

**Heap Sizing Formula**:
```
Initial Heap (Xms) = 50% of max heap
Max Heap (Xmx) = Physical RAM / 4 (leave room for OS + other processes)

Example:
- Server RAM: 16GB
- Max Heap: 16GB / 4 = 4GB
- Initial Heap: 4GB / 2 = 2GB
- JVM Flags: -Xms2g -Xmx4g
```

**GC Troubleshooting**:
| Symptom | Cause | Fix |
|---------|-------|-----|
| Frequent Full GC | Heap too small | Increase -Xmx |
| Long GC pauses | Large heap + bad GC | Switch to G1GC |
| High CPU (GC overhead) | Heap too small | Increase heap or reduce allocation rate |
| OutOfMemoryError | Memory leak | Fix leak (see Pattern 2) |

**Time to Tune**: 15-30 minutes
**Time to Verify**: 10-20 minutes (monitor production)

---

### Pattern 5: SQL Query Optimization

**Trigger Keywords**: "slow query", "SQL optimization", "query performance"

**Use When**: Queries taking >1 second

**EXPLAIN ANALYZE Workflow**:
```sql
-- 1. Identify slow query
SELECT e.*, d.name
FROM t_employee e
LEFT JOIN t_department d ON e.dept_id = d.id
WHERE e.status = 'ACTIVE'
  AND e.created_at > '2025-01-01';

-- 2. Analyze execution plan
EXPLAIN ANALYZE
SELECT e.*, d.name
FROM t_employee e
LEFT JOIN t_department d ON e.dept_id = d.id
WHERE e.status = 'ACTIVE'
  AND e.created_at > '2025-01-01';

-- Output:
-- Seq Scan on t_employee  (cost=0.00..1520.00 rows=10000)  ← BAD!
--   Filter: (status = 'ACTIVE' AND created_at > '2025-01-01')

-- 3. Add index
CREATE INDEX idx_employee_status_created ON t_employee(status, created_at);

-- 4. Verify improvement
EXPLAIN ANALYZE
SELECT e.*, d.name
FROM t_employee e
LEFT JOIN t_department d ON e.dept_id = d.id
WHERE e.status = 'ACTIVE'
  AND e.created_at > '2025-01-01';

-- Output:
-- Index Scan using idx_employee_status_created  (cost=0.29..8.31 rows=1)  ← GOOD!
```

**Index Optimization Rules**:
1. **WHERE clause columns** → Create index
2. **JOIN columns** → Create index on both sides
3. **ORDER BY columns** → Include in index
4. **Composite index order** → Most selective column first

**Example Composite Index**:
```sql
-- Query: WHERE status = 'ACTIVE' AND dept_id = 1 ORDER BY created_at DESC
CREATE INDEX idx_employee_status_dept_created ON t_employee(status, dept_id, created_at DESC);

-- Rule: Index columns should match query order
```

**Time to Optimize**: 8-15 minutes per query

---

## Common Performance Errors and Quick Fixes

### Error 1: High CPU (String Concatenation in Loop)

**Symptom**: CPU usage 90%+, slow response

**Cause**: `String` concatenation creates new objects

**Quick Fix**:
```java
// ❌ BAD - O(n²) time complexity
String result = "";
for (String item : items) {
    result += item + ",";  // Creates new String every iteration!
}

// ✅ GOOD - O(n) time complexity
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(",");
}
String result = sb.toString();
```

**Improvement**: 70-80% faster

---

### Error 2: OutOfMemoryError (Heap Space)

**Symptom**: `java.lang.OutOfMemoryError: Java heap space`

**Cause**: Heap too small or memory leak

**Quick Fix**:
```bash
# Increase heap size
java -Xms2g -Xmx4g -jar app.jar

# Enable heap dump on OOM for diagnosis
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.hprof -jar app.jar
```

**Analyze heap dump** with Eclipse MAT to find leak

---

### Error 3: Excessive GC (Allocation Rate Too High)

**Symptom**: GC runs constantly, high CPU

**Cause**: Creating too many temporary objects

**Quick Fix**:
```java
// ❌ BAD - Creates new BigDecimal every call
public BigDecimal calculate(BigDecimal value) {
    return value.multiply(new BigDecimal("0.1"));  // New object!
}

// ✅ GOOD - Reuse constant
private static final BigDecimal RATE = new BigDecimal("0.1");

public BigDecimal calculate(BigDecimal value) {
    return value.multiply(RATE);  // Reuse!
}
```

**Improvement**: 50% reduction in GC pressure

---

## Time Estimates (Production Data)

| Task | Initial | Optimized | Improvement |
|------|---------|-----------|-------------|
| CPU Profiling + Fix | 60 min | 15 min | 75% |
| Memory Leak Detection | 90 min | 30 min | 67% |
| N+1 Query Fix | 30 min | 10 min | 67% |
| JVM Tuning | 45 min | 20 min | 56% |
| SQL Optimization | 60 min | 15 min | 75% |

**Full Performance Audit**: 2-3 hours (all 5 patterns)

---

## Validation Checklist

Before deploying performance optimizations:

- [ ] CPU profiling completed (hotspots identified)
- [ ] Memory leaks detected and fixed
- [ ] N+1 queries eliminated (verified with P6Spy)
- [ ] JVM tuning applied (GC pauses <200ms)
- [ ] SQL queries optimized (all <1s execution)
- [ ] Load testing completed (performance improved)
- [ ] Production monitoring enabled (Grafana/Prometheus)

---

**See Also**:
- [N+1 Detection Guide](../references/n-plus-one-detection.md) - Detailed N+1 patterns
- [JVM Tuning Guide](../references/jvm-tuning-guide.md) - Complete JVM configuration
- [SQL Optimization Patterns](../references/sql-optimization-patterns.md) - Query optimization techniques
- [PostgreSQL Best Practices](../../integration/postgresql-best-practices/) - Database optimization
