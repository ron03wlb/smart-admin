# SQL Query Optimization Patterns

## Index Optimization

### 1. Identify Missing Indexes

**Use EXPLAIN to analyze query execution:**

```sql
-- PostgreSQL
EXPLAIN ANALYZE SELECT * FROM t_employee WHERE login_name = 'admin';

-- Look for:
-- Seq Scan (BAD - full table scan)
-- Index Scan (GOOD - using index)
```

**Example output:**
```
Seq Scan on t_employee  (cost=0.00..1000.00 rows=1 width=100)
  Filter: (login_name = 'admin'::text)
```

**Fix: Add index**
```sql
CREATE INDEX idx_employee_login_name ON t_employee(login_name);

-- After index:
Index Scan using idx_employee_login_name on t_employee  (cost=0.00..8.27 rows=1)
  Index Cond: (login_name = 'admin'::text)
```

### 2. Composite Indexes

**When to use:**
- Queries filter on multiple columns
- Queries with ORDER BY on multiple columns

```sql
-- Query filters on department_id AND deleted_flag
SELECT * FROM t_employee 
WHERE department_id = 1 AND deleted_flag = false
ORDER BY create_time DESC;

-- Create composite index (order matters!)
CREATE INDEX idx_employee_dept_deleted_time 
ON t_employee(department_id, deleted_flag, create_time DESC);
```

**Index column order guidelines:**
1. Equality conditions first (`=`)
2. Range conditions next (`>`, `<`, `BETWEEN`)
3. Sort columns last (`ORDER BY`)

### 3. Covering Indexes

**Include all columns needed by query in index:**

```sql
-- Query needs: employee_id, actual_name, phone
SELECT employee_id, actual_name, phone 
FROM t_employee 
WHERE department_id = 1;

-- Covering index (PostgreSQL)
CREATE INDEX idx_employee_dept_covering 
ON t_employee(department_id) 
INCLUDE (employee_id, actual_name, phone);

-- MySQL equivalent
CREATE INDEX idx_employee_dept_covering 
ON t_employee(department_id, employee_id, actual_name, phone);
```

**Benefit:** Index-only scan (no table access needed)

## Pagination Optimization

### 1. OFFSET Pagination (Slow for large offsets)

**Problem:**
```sql
-- BAD: Scans and discards first 10,000 rows
SELECT * FROM t_employee 
ORDER BY employee_id 
OFFSET 10000 LIMIT 10;
```

**Execution:** Database must read 10,010 rows to return 10

### 2. Keyset Pagination (Fast)

**Solution:**
```sql
-- GOOD: Uses index to skip directly to position
SELECT * FROM t_employee 
WHERE employee_id > :lastSeenId
ORDER BY employee_id 
LIMIT 10;
```

**Java implementation:**
```java
public PageResult<EmployeeVO> queryEmployeeKeyset(Long lastSeenId, int pageSize) {
    LambdaQueryWrapper<EmployeeEntity> query = Wrappers.<EmployeeEntity>lambdaQuery()
        .gt(lastSeenId != null, EmployeeEntity::getEmployeeId, lastSeenId)
        .orderByAsc(EmployeeEntity::getEmployeeId)
        .last("LIMIT " + pageSize);
    
    List<EmployeeEntity> employees = employeeDao.selectList(query);
    List<EmployeeVO> voList = SmartBeanUtil.copyList(employees, EmployeeVO.class);
    
    return new PageResult<>(voList, employees.size());
}
```

### 3. When to Use Each

| Pagination Type | Use Case | Performance |
|----------------|----------|-------------|
| OFFSET/LIMIT | Small datasets (< 1000 rows) | Good |
| OFFSET/LIMIT | Random page access needed | Acceptable |
| Keyset | Large datasets (> 10,000 rows) | Excellent |
| Keyset | Sequential scrolling (infinite scroll) | Excellent |

## JOIN Optimization

### 1. Use Appropriate JOIN Type

```sql
-- INNER JOIN: Only matching rows
SELECT e.*, d.department_name
FROM t_employee e
INNER JOIN t_department d ON e.department_id = d.department_id;

-- LEFT JOIN: All employees, even without department
SELECT e.*, d.department_name
FROM t_employee e
LEFT JOIN t_department d ON e.department_id = d.department_id;
```

### 2. JOIN Order Matters

**Database optimizers are smart, but help them:**

```sql
-- BAD: Large table first
SELECT * FROM t_employee e
INNER JOIN t_department d ON e.department_id = d.department_id
WHERE d.department_id = 1;

-- BETTER: Filter first, then JOIN
SELECT * FROM t_department d
INNER JOIN t_employee e ON e.department_id = d.department_id
WHERE d.department_id = 1;

-- BEST: Use WHERE to filter (optimizer handles order)
SELECT * FROM t_employee e
INNER JOIN t_department d ON e.department_id = d.department_id
WHERE e.department_id = 1;
```

### 3. Avoid JOIN on Function Results

```sql
-- BAD: Function prevents index usage
SELECT * FROM t_employee e
INNER JOIN t_department d ON LOWER(e.department_id) = LOWER(d.department_id);

-- GOOD: Join on indexed column directly
SELECT * FROM t_employee e
INNER JOIN t_department d ON e.department_id = d.department_id;
```

## SELECT Optimization

### 1. Select Only Needed Columns

```java
// BAD: Returns all columns (wasteful)
@Select("SELECT * FROM t_employee WHERE deleted_flag = false")
List<EmployeeEntity> queryAll();

// GOOD: Select only needed columns
@Select("SELECT employee_id, actual_name, phone FROM t_employee WHERE deleted_flag = false")
List<EmployeeSimpleVO> querySimple();
```

**Benefit:**
- Reduced network transfer
- Smaller result set in memory
- Possible index-only scans

### 2. Avoid Functions in WHERE Clause

```sql
-- BAD: Function on column prevents index usage
SELECT * FROM t_employee WHERE YEAR(create_time) = 2024;

-- GOOD: Use range query
SELECT * FROM t_employee 
WHERE create_time >= '2024-01-01' 
  AND create_time < '2025-01-01';
```

## Subquery Optimization

### 1. Use JOIN Instead of IN (for large lists)

```sql
-- BAD: Subquery may be inefficient
SELECT * FROM t_employee 
WHERE department_id IN (
    SELECT department_id FROM t_department WHERE region = 'North'
);

-- GOOD: Use JOIN
SELECT e.* FROM t_employee e
INNER JOIN t_department d ON e.department_id = d.department_id
WHERE d.region = 'North';
```

### 2. Use EXISTS Instead of COUNT

```sql
-- BAD: Counts all rows (wasteful)
SELECT department_id FROM t_department
WHERE (SELECT COUNT(*) FROM t_employee WHERE department_id = d.department_id) > 0;

-- GOOD: EXISTS stops after first match
SELECT department_id FROM t_department d
WHERE EXISTS (
    SELECT 1 FROM t_employee e WHERE e.department_id = d.department_id
);
```

## Batch Operations

### 1. Batch INSERT

```java
// BAD: N individual inserts
for (EmployeeEntity emp : employees) {
    employeeDao.insert(emp);
}

// GOOD: Batch insert
employeeDao.insertBatch(employees);
```

**MyBatis Plus batch insert:**
```java
@Service
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final SqlSessionFactory sqlSessionFactory;
    
    public void batchInsert(List<EmployeeEntity> employees) {
        // Use batch session for better performance
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            EmployeeDao mapper = sqlSession.getMapper(EmployeeDao.class);
            for (EmployeeEntity emp : employees) {
                mapper.insert(emp);
            }
            sqlSession.commit();
        }
    }
}
```

### 2. Batch UPDATE

```sql
-- PostgreSQL: Use CASE for batch updates
UPDATE t_employee SET 
    actual_name = CASE employee_id
        WHEN 1 THEN 'Name1'
        WHEN 2 THEN 'Name2'
        WHEN 3 THEN 'Name3'
    END
WHERE employee_id IN (1, 2, 3);
```

## Query Result Caching

### 1. Application-Level Caching

```java
@Manager
public class EmployeeCacheManager {
    
    @Cacheable(value = "employee", key = "#id")
    public EmployeeEntity getById(Long id) {
        return employeeDao.selectById(id);
    }
    
    @CacheEvict(value = "employee", key = "#entity.employeeId")
    public void update(EmployeeEntity entity) {
        employeeDao.updateById(entity);
    }
}
```

### 2. Database Query Cache (MySQL)

```sql
-- Enable query cache (MySQL 5.x only, deprecated in 8.0)
SET GLOBAL query_cache_type = ON;
SET GLOBAL query_cache_size = 67108864; -- 64MB
```

**Note:** MySQL 8.0+ removed query cache. Use application-level caching instead.

## EXPLAIN Analysis Checklist

**Run EXPLAIN on all queries:**

```sql
EXPLAIN ANALYZE SELECT ...;
```

**Look for warning signs:**
- ❌ Seq Scan (full table scan) - Add index
- ❌ Rows estimate >> actual rows - Update statistics
- ❌ Nested Loop with large tables - Consider hash join
- ❌ Subquery in SELECT - Use JOIN instead
- ✅ Index Scan - Good
- ✅ Index Only Scan - Best (covering index)

**PostgreSQL EXPLAIN output:**
```
QUERY PLAN
---------------------------------------------------------------------------
Index Scan using idx_employee_login_name on t_employee  (cost=0.29..8.31 rows=1 width=100)
  Index Cond: (login_name = 'admin'::text)
```

**Key metrics:**
- `cost=0.29..8.31` - Estimated cost (lower is better)
- `rows=1` - Estimated rows returned
- `width=100` - Average row size in bytes

## Common Anti-Patterns

### 1. SELECT COUNT(*) for Existence Check

```sql
-- BAD: Counts all rows
SELECT COUNT(*) FROM t_employee WHERE login_name = 'admin';
-- Then check: if (count > 0)

-- GOOD: Use LIMIT 1
SELECT 1 FROM t_employee WHERE login_name = 'admin' LIMIT 1;
-- Then check: if (result != null)
```

### 2. OR in WHERE Clause

```sql
-- BAD: OR prevents index usage
SELECT * FROM t_employee WHERE department_id = 1 OR department_id = 2;

-- GOOD: Use IN
SELECT * FROM t_employee WHERE department_id IN (1, 2);
```

### 3. LIKE with Leading Wildcard

```sql
-- BAD: Cannot use index
SELECT * FROM t_employee WHERE login_name LIKE '%admin';

-- BETTER: Use full-text search
CREATE INDEX idx_employee_login_name_gin ON t_employee 
USING GIN(to_tsvector('english', login_name));

SELECT * FROM t_employee 
WHERE to_tsvector('english', login_name) @@ to_tsquery('admin');
```
