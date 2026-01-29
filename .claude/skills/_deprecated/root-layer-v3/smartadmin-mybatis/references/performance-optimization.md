# MyBatis Performance Optimization Guide

Comprehensive guide to optimizing MyBatis queries and avoiding common performance pitfalls in SmartAdmin.

## Common Performance Issues

### 1. N+1 Query Problem

**Problem:** Loop calling single-record query results in N+1 database round-trips.

```java
// ❌ BAD - N+1 queries (1 order query + N user queries)
List<OrderEntity> orders = orderDao.selectList(wrapper);
for (OrderEntity order : orders) {
    UserEntity user = userDao.selectById(order.getUserId());  // ← N queries!
    order.setUserName(user.getUserName());
}
```

**Solution 1: Batch Query**
```java
// ✅ GOOD - 2 queries total
List<OrderEntity> orders = orderDao.selectList(wrapper);

// Extract unique user IDs
List<Long> userIds = orders.stream()
    .map(OrderEntity::getUserId)
    .distinct()
    .collect(Collectors.toList());

// Batch fetch users
List<UserEntity> users = userDao.selectBatchIds(userIds);
Map<Long, UserEntity> userMap = users.stream()
    .collect(Collectors.toMap(UserEntity::getUserId, u -> u));

// Map users to orders
orders.forEach(order -> {
    UserEntity user = userMap.get(order.getUserId());
    if (user != null) {
        order.setUserName(user.getUserName());
    }
});
```

**Solution 2: JOIN Query (XML Mapper)**
```xml
<!-- ✅ GOOD - 1 query with JOIN -->
<select id="queryOrderWithUser" resultType="OrderVO">
    SELECT
        o.order_id,
        o.order_no,
        o.amount,
        u.user_id,
        u.user_name
    FROM t_order o
    LEFT JOIN t_user u ON u.user_id = o.user_id
    WHERE o.status = #{status}
</select>
```

**Detection:**
- Enable SQL logging: `mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl`
- Look for repeated similar queries in logs
- Use database profiling tools (MySQL slow query log)

---

### 2. Large Offset Pagination

**Problem:** `LIMIT offset, size` performance degrades with large offsets.

```sql
-- ❌ SLOW - MySQL must scan and skip 100,000 rows
SELECT * FROM t_order
ORDER BY create_time DESC
LIMIT 100000, 20;
```

**Why it's slow:**
- Database must scan `offset + size` rows
- Large offsets cause full index scans
- Performance degrades linearly with offset size

**Solution 1: Cursor-Based Pagination**
```java
// ✅ FAST - Use last seen ID as cursor
LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
    .gt(lastOrderId != null, Order::getOrderId, lastOrderId)
    .orderByAsc(Order::getOrderId)
    .last("LIMIT 20");
```

```xml
<!-- XML version -->
<select id="queryOrdersAfter" resultType="OrderVO">
    SELECT *
    FROM t_order
    <where>
        <if test="lastOrderId != null">
            AND order_id > #{lastOrderId}
        </if>
    </where>
    ORDER BY order_id ASC
    LIMIT #{pageSize}
</select>
```

**Solution 2: Subquery Optimization**
```sql
-- ✅ FASTER - Use covering index
SELECT o.*
FROM t_order o
INNER JOIN (
    SELECT order_id
    FROM t_order
    ORDER BY create_time DESC
    LIMIT 100000, 20
) AS temp ON temp.order_id = o.order_id;
```

**Solution 3: Scroll API (for internal use)**
```java
// For admin dashboards only (not for public APIs)
// Use MyBatis Plus scroll
IPage<OrderEntity> page = orderDao.selectPage(
    new Page<>(pageNum, pageSize),
    wrapper
);
```

**Best Practice:**
- Limit max page number (e.g., pageNum ≤ 100)
- Use cursor pagination for "infinite scroll" features
- Cache frequently accessed pages (Redis)

---

### 3. SELECT * Over-fetching

**Problem:** Fetching unnecessary columns wastes bandwidth and memory.

```java
// ❌ BAD - Fetches all 30 columns including large TEXT fields
List<OrderEntity> orders = orderDao.selectList(wrapper);
// Only need order_id and amount
```

**Solution: Select Specific Columns**
```java
// ✅ GOOD - Only fetch needed columns
LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
    .select(Order::getOrderId, Order::getAmount, Order::getStatus)
    .eq(Order::getUserId, userId);
List<Order> orders = orderDao.selectList(wrapper);
```

```xml
<!-- XML version -->
<select id="getOrderSummary" resultType="OrderSummaryVO">
    SELECT order_id, amount, status
    FROM t_order
    WHERE user_id = #{userId}
</select>
```

**Impact:**
- Reduces memory usage (50-90% for large objects)
- Faster network transfer
- Better cache efficiency
- Reduces database I/O

---

### 4. Missing or Unused Indexes

**Problem:** Queries scan entire table instead of using indexes.

```sql
-- ❌ SLOW - Full table scan if no index on email
SELECT * FROM t_employee
WHERE email = 'user@example.com';

-- Check with EXPLAIN
EXPLAIN SELECT * FROM t_employee WHERE email = 'user@example.com';
-- type: ALL ← Full table scan!
```

**Solution: Add Appropriate Indexes**
```sql
-- ✅ Add index on frequently queried columns
CREATE INDEX idx_employee_email ON t_employee(email);

-- Composite index for multiple conditions
CREATE INDEX idx_employee_dept_status ON t_employee(department_id, disabled_flag);

-- Covering index (includes SELECT columns)
CREATE INDEX idx_employee_dept_name ON t_employee(department_id, actual_name);
```

**Index Design Rules:**
1. **Selectivity**: High cardinality columns first
2. **Equality before Range**: `WHERE a = ? AND b > ?` → `INDEX(a, b)`
3. **Covering Indexes**: Include SELECT columns to avoid table lookup
4. **Avoid Over-indexing**: Each index slows INSERT/UPDATE

**Check Index Usage:**
```sql
-- MySQL
EXPLAIN SELECT * FROM t_employee
WHERE department_id = 1 AND disabled_flag = false;

-- Look for:
-- - type: ref (good) or ALL (bad)
-- - key: index_name (which index used)
-- - rows: estimated rows scanned
```

---

### 5. Inefficient LIKE Queries

**Problem:** Leading wildcard prevents index usage.

```sql
-- ❌ SLOW - Cannot use index
SELECT * FROM t_employee
WHERE actual_name LIKE '%zhang%';

-- ❌ SLOW - Leading wildcard
WHERE actual_name LIKE '%zhang';

-- ✅ FAST - Can use index (prefix match)
WHERE actual_name LIKE 'zhang%';
```

**Solutions:**

**Option 1: Full-Text Search**
```sql
-- Create full-text index
CREATE FULLTEXT INDEX ft_employee_name ON t_employee(actual_name);

-- Use MATCH AGAINST
SELECT * FROM t_employee
WHERE MATCH(actual_name) AGAINST('zhang' IN NATURAL LANGUAGE MODE);
```

**Option 2: Elasticsearch (for large datasets)**
- Index documents to Elasticsearch
- Use fuzzy/wildcard queries
- Return IDs and fetch full records from MySQL

**Option 3: Accept Limitation**
- For small tables (< 10K rows), full scan is acceptable
- Add pagination to limit result size
- Cache results for common searches

---

### 6. Subquery Performance

**Problem:** Correlated subqueries execute for every row.

```sql
-- ❌ SLOW - Subquery executes N times
SELECT
    e.employee_id,
    e.actual_name,
    (SELECT COUNT(*) FROM t_order WHERE user_id = e.employee_id) AS order_count
FROM t_employee e;
```

**Solution: JOIN with Aggregation**
```sql
-- ✅ FAST - Single aggregation
SELECT
    e.employee_id,
    e.actual_name,
    COALESCE(COUNT(o.order_id), 0) AS order_count
FROM t_employee e
LEFT JOIN t_order o ON o.user_id = e.employee_id
GROUP BY e.employee_id, e.actual_name;
```

---

### 7. OR Condition Performance

**Problem:** OR conditions can prevent index usage.

```sql
-- ❌ SLOW - May not use indexes efficiently
SELECT * FROM t_employee
WHERE department_id = 1 OR position_id = 5;
```

**Solution: UNION**
```sql
-- ✅ FASTER - Each query uses its index
SELECT * FROM t_employee WHERE department_id = 1
UNION
SELECT * FROM t_employee WHERE position_id = 5;
```

**In MyBatis:**
```xml
<select id="findByDepartmentOrPosition" resultType="EmployeeVO">
    SELECT * FROM t_employee WHERE department_id = #{deptId}
    UNION
    SELECT * FROM t_employee WHERE position_id = #{positionId}
</select>
```

---

## Optimization Strategies

### 1. Query Result Caching

**Use Case:** Frequently accessed, rarely changed data.

```java
@Service
@RequiredArgsConstructor
public class DepartmentCacheManager {

    private final DepartmentDao departmentDao;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "department:";

    public DepartmentEntity getById(Long deptId) {
        String key = CACHE_KEY + deptId;

        // Try cache first
        DepartmentEntity cached = (DepartmentEntity) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // Cache miss - query database
        DepartmentEntity dept = departmentDao.selectById(deptId);
        if (dept != null) {
            redisTemplate.opsForValue().set(key, dept, 1, TimeUnit.HOURS);
        }
        return dept;
    }

    public void invalidateCache(Long deptId) {
        redisTemplate.delete(CACHE_KEY + deptId);
    }
}
```

**Cache Strategies:**
- **Read-through**: Check cache → miss → query DB → update cache
- **Write-through**: Update DB → update cache
- **Cache-aside**: Application manages cache explicitly

---

### 2. Batch Operations

**Problem:** Multiple single-record operations.

```java
// ❌ BAD - N database round-trips
for (EmployeeEntity employee : employees) {
    employeeDao.insert(employee);  // ← N queries
}
```

**Solution: Batch Insert**
```java
// ✅ GOOD - Single batch operation
employeeDao.insertBatchSomeColumn(employees);
```

**XML Batch Insert:**
```xml
<insert id="batchInsert">
    INSERT INTO t_employee (actual_name, department_id, phone)
    VALUES
    <foreach collection="employees" separator="," item="item">
        (#{item.actualName}, #{item.departmentId}, #{item.phone})
    </foreach>
</insert>
```

**Batch Update:**
```java
// MyBatis Plus batch update
employeeDao.updateBatchById(employees, 1000);  // Batch size: 1000
```

---

### 3. Connection Pooling

**HikariCP Configuration (SmartAdmin default):**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20          # Max connections
      minimum-idle: 10               # Min idle connections
      connection-timeout: 30000      # 30s connection timeout
      idle-timeout: 600000           # 10min idle timeout
      max-lifetime: 1800000          # 30min max lifetime
      leak-detection-threshold: 60000  # Detect leaks after 60s
```

**Best Practices:**
- Set `maximum-pool-size` based on CPU cores: `cores * 2 + disk_spindles`
- Monitor connection usage with `/actuator/metrics`
- Enable leak detection in development

---

### 4. Lazy Loading

**Problem:** Fetching related entities eagerly wastes resources.

```java
// Avoid fetching nested objects unless needed
@Data
@TableName("t_employee")
public class EmployeeEntity {
    private Long employeeId;
    // Don't eagerly load department
    @TableField(exist = false)
    private DepartmentEntity department;  // ← Load only when needed
}
```

**Explicit Loading:**
```java
// Service layer
public EmployeeVO getEmployeeDetail(Long employeeId) {
    EmployeeEntity employee = employeeDao.selectById(employeeId);
    EmployeeVO vo = SmartBeanUtil.copy(employee, EmployeeVO.class);

    // Load department only if needed
    if (employee.getDepartmentId() != null) {
        DepartmentEntity dept = deptDao.selectById(employee.getDepartmentId());
        vo.setDepartmentName(dept.getDepartmentName());
    }

    return vo;
}
```

---

## Monitoring and Profiling

### 1. Enable SQL Logging

**application.yml:**
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

logging:
  level:
    net.lab1024.sa.admin.module.system.employee.dao: DEBUG
```

**Output:**
```
==> Preparing: SELECT * FROM t_employee WHERE department_id = ?
==> Parameters: 1(Long)
<== Total: 25
```

---

### 2. MySQL Slow Query Log

**Enable slow query logging:**
```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;  -- Log queries > 2 seconds
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

**Analyze slow queries:**
```bash
mysqldumpslow -s t -t 10 /var/log/mysql/slow-query.log
```

---

### 3. EXPLAIN Query Plans

```sql
EXPLAIN SELECT e.*, d.department_name
FROM t_employee e
LEFT JOIN t_department d ON d.department_id = e.department_id
WHERE e.actual_name LIKE 'zhang%';
```

**Key Columns:**
- `type`: ALL (bad), index, ref (good), const (best)
- `key`: Which index used (NULL means no index)
- `rows`: Estimated rows scanned
- `Extra`: Using filesort (bad), Using index (good)

---

## Performance Checklist

### Before Deploying to Production

- [ ] No N+1 queries (check logs for repeated patterns)
- [ ] All frequently queried columns have indexes
- [ ] Large offset pagination replaced with cursor pagination
- [ ] SELECT only required columns (no SELECT *)
- [ ] Batch operations used for bulk inserts/updates
- [ ] Connection pool properly configured
- [ ] Slow query logging enabled
- [ ] Cache strategy implemented for hot data
- [ ] EXPLAIN shows efficient query plans
- [ ] No correlated subqueries

---

## Anti-Patterns Summary

| ❌ Anti-Pattern | ✅ Solution |
|----------------|------------|
| N+1 queries in loop | Batch query or JOIN |
| `LIMIT 100000, 20` | Cursor pagination |
| `SELECT *` | Select specific columns |
| Missing indexes | Add indexes on WHERE/JOIN columns |
| `LIKE '%keyword%'` | Full-text search or prefix match |
| Correlated subquery | JOIN with aggregation |
| Single-record inserts in loop | Batch insert |
| No connection pooling | Configure HikariCP |
| No query logging | Enable SQL logging |

---

## Related Documentation

- [Dao Architecture Guide](dao-architecture.md)
- [LambdaQueryWrapper Guide](lambda-wrapper-guide.md)
- [XML Mapper Patterns](xml-mapper-patterns.md)
- [PostgreSQL Advanced Rules](../../../../.agent/technology/database/05-postgresql-advanced.md)
- [MyBatis Plus Core](../../../../.agent/rules/technology/database/09-mybatis-plus-core.md)
