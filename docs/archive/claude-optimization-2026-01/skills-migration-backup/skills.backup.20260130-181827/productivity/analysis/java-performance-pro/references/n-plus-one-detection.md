# N+1 Query Detection and Resolution Patterns

## Detection Strategies

### 1. Enable MyBatis SQL Logging

```yaml
# application.yml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    
logging:
  level:
    net.lab1024.sa: DEBUG
```

### 2. Look for Repeated Query Patterns

```
==>  Preparing: SELECT * FROM t_employee WHERE department_id = ?
==> Parameters: 1(Long)
<==    Total: 5
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

**Red flags:**
- Same query with different parameters repeated
- Queries inside loops
- Total queries = 1 + N (where N = number of parent records)

## Resolution Strategies

### Strategy 1: JOIN Query (Best for small datasets)

```java
// DAO method with explicit JOIN
@Select("SELECT e.employee_id, e.actual_name, e.phone, " +
        "       d.department_id, d.department_name " +
        "FROM t_employee e " +
        "LEFT JOIN t_department d ON e.department_id = d.department_id " +
        "WHERE e.deleted_flag = false")
List<EmployeeVO> queryEmployeeWithDepartment();
```

**Pros:**
- Single database round-trip
- Optimal for small datasets (< 1000 rows)
- Database optimized JOINs

**Cons:**
- Can be slow for large result sets
- Duplication if one-to-many relationships

### Strategy 2: Batch Loading (Best for large datasets)

```java
public List<EmployeeVO> queryEmployeeWithDepartment() {
    // Step 1: Query employees
    List<EmployeeEntity> employees = employeeDao.selectList(
        Wrappers.<EmployeeEntity>lambdaQuery()
            .eq(EmployeeEntity::getDeletedFlag, false)
    );
    
    // Step 2: Collect department IDs
    List<Long> deptIds = employees.stream()
        .map(EmployeeEntity::getDepartmentId)
        .distinct()
        .collect(Collectors.toList());
    
    // Step 3: Batch query departments
    List<DepartmentEntity> departments = departmentDao.selectBatchIds(deptIds);
    Map<Long, DepartmentEntity> deptMap = departments.stream()
        .collect(Collectors.toMap(DepartmentEntity::getDepartmentId, d -> d));
    
    // Step 4: Map results
    return employees.stream()
        .map(emp -> {
            EmployeeVO vo = SmartBeanUtil.copy(emp, EmployeeVO.class);
            DepartmentEntity dept = deptMap.get(emp.getDepartmentId());
            if (dept != null) {
                vo.setDepartmentName(dept.getDepartmentName());
            }
            return vo;
        })
        .collect(Collectors.toList());
}
```

**Pros:**
- Scales well to large datasets
- Only 2 database queries (not 1 + N)
- Avoids JOIN overhead

**Cons:**
- More application code
- Two round-trips to database

### Strategy 3: Caching (Best for frequently accessed reference data)

```java
@Manager
public class DepartmentCacheManager {
    private final DepartmentDao departmentDao;
    
    @Cacheable(value = "department", key = "#deptId")
    public DepartmentEntity getDepartment(Long deptId) {
        return departmentDao.selectById(deptId);
    }
}

@Service
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final DepartmentCacheManager departmentCacheManager;
    
    public List<EmployeeVO> queryEmployee() {
        List<EmployeeEntity> employees = employeeDao.selectList(null);
        
        return employees.stream()
            .map(emp -> {
                EmployeeVO vo = SmartBeanUtil.copy(emp, EmployeeVO.class);
                // First call: cache miss, second+: cache hit
                DepartmentEntity dept = departmentCacheManager.getDepartment(emp.getDepartmentId());
                if (dept != null) {
                    vo.setDepartmentName(dept.getDepartmentName());
                }
                return vo;
            })
            .collect(Collectors.toList());
    }
}
```

**Pros:**
- Subsequent queries very fast
- Good for reference data (departments, roles, dictionaries)
- Reduces database load

**Cons:**
- First call still queries database N times
- Requires cache invalidation strategy

## Decision Matrix

| Dataset Size | Data Type | Recommended Strategy |
|--------------|-----------|---------------------|
| < 100 rows | Any | JOIN Query |
| 100-10,000 rows | Frequently changing | Batch Loading |
| 100-10,000 rows | Reference data | Caching |
| > 10,000 rows | Any | Batch Loading + Pagination |

## Testing for N+1 Queries

```java
@Test
void queryEmployee_ShouldNotHaveNPlusOneQueries() {
    // Given: 10 employees with different departments
    for (int i = 0; i < 10; i++) {
        employeeDao.insert(createEmployee(i));
    }
    
    // When: Query all employees
    int queryCountBefore = getQueryCount(); // Use P6Spy or similar
    List<EmployeeVO> results = employeeService.queryEmployee();
    int queryCountAfter = getQueryCount();
    
    // Then: Should execute maximum 2 queries (employees + batch departments)
    int totalQueries = queryCountAfter - queryCountBefore;
    assertTrue(totalQueries <= 2, 
        "Expected ≤ 2 queries, but got " + totalQueries + " (N+1 detected)");
}
```
