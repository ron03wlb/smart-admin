# Dao Layer Architecture Guide

SmartAdmin Dao layer architecture patterns and conventions for MyBatis Plus integration.

## Overview

The Dao (Data Access Object) layer is responsible for database operations and must strictly adhere to SmartAdmin's layered architecture rules.

## Architecture Rules

### Layer Dependencies

```
Controller → Service → Manager → Dao → Database
                    ↓
                  Entity
```

**Mandatory Rules:**
- ✅ Dao layer is called ONLY by Service or Manager layers
- ❌ NEVER call other Dao methods within Dao layer
- ❌ NEVER call Service/Manager from Dao
- ❌ NEVER add business logic in Dao layer

### Dao Interface Structure

```java
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {

    // 1. Constants (optional)
    String DELETED_FLAG = "deletedFlag";

    // 2. Custom query methods
    List<EmployeeVO> queryEmployee(
        Page<?> page,
        @Param("queryForm") EmployeeQueryForm queryForm,
        @Param("departmentIdList") List<Long> departmentIdList);

    // 3. Single record queries
    EmployeeEntity getByLoginName(
        @Param("loginName") String loginName,
        @Param(DELETED_FLAG) Boolean deletedFlag);

    // 4. Update operations
    void updateDisableFlag(
        @Param("employeeId") Long employeeId,
        @Param("disabledFlag") Boolean disabledFlag);

    // 5. Count/Aggregation
    Integer countByDepartmentId(
        @Param("departmentId") Long departmentId,
        @Param(DELETED_FLAG) Boolean deletedFlag);
}
```

## Naming Conventions

### Interface Naming

| Pattern | Correct | Incorrect |
|---------|---------|-----------|
| **Interface** | `EmployeeDao` | `EmployeeDaoImpl`, `IEmployeeDao` |
| **XML Mapper** | `EmployeeMapper.xml` | `EmployeeDaoMapper.xml` |
| **Package** | `net.lab1024.sa.admin.module.system.employee.dao` | `net.lab1024.sa.admin.dao` |

### Method Naming

| Operation | Prefix | Example | Return Type |
|-----------|--------|---------|-------------|
| **Query List** | `query*`, `list*`, `select*` | `queryEmployee` | `List<VO>` |
| **Query Single** | `get*`, `selectOne*` | `getByLoginName` | `Entity` |
| **Insert** | `insert*`, `save*` | Inherited from `BaseMapper` | `int` |
| **Update** | `update*` | `updateDisableFlag` | `int` / `void` |
| **Delete** | `delete*`, `remove*` | Inherited from `BaseMapper` | `int` |
| **Count** | `count*` | `countByDepartmentId` | `Integer` / `Long` |
| **Exists** | `exists*` | `existsByEmail` | `Boolean` |

## BaseMapper Methods

MyBatis Plus provides built-in CRUD operations through `BaseMapper<T>`:

```java
// Insert
int insert(T entity);
int insertBatchSomeColumn(Collection<T> entityList);

// Delete
int deleteById(Serializable id);
int deleteBatchIds(Collection<? extends Serializable> idList);
int delete(Wrapper<T> wrapper);

// Update
int updateById(T entity);
int update(T entity, Wrapper<T> updateWrapper);

// Select
T selectById(Serializable id);
List<T> selectBatchIds(Collection<? extends Serializable> idList);
List<T> selectList(Wrapper<T> queryWrapper);
T selectOne(Wrapper<T> queryWrapper);
Long selectCount(Wrapper<T> queryWrapper);
```

**Best Practice:** Use inherited methods for simple CRUD, define custom methods for complex queries.

## Custom Methods

### When to Define Custom Methods

**Use Custom Methods for:**
- Multi-table JOIN queries
- Complex dynamic WHERE conditions
- Aggregation with GROUP BY
- Specific column updates
- Batch operations with complex logic

**Use BaseMapper Methods for:**
- Single table CRUD
- Simple WHERE conditions (≤ 3 conditions)
- Primary key lookups
- Batch insert/delete by IDs

### Parameter Binding

**Always use `@Param` annotation:**

```java
// ✅ Correct - Clear parameter names
EmployeeEntity getByLoginName(
    @Param("loginName") String loginName,
    @Param("deletedFlag") Boolean deletedFlag);

// ❌ Incorrect - Unclear what param1, param2 mean in XML
EmployeeEntity getByLoginName(String loginName, Boolean deletedFlag);
```

### Return Types

| Scenario | Return Type | Notes |
|----------|-------------|-------|
| **Query List for Display** | `List<VO>` | Use VO for frontend display |
| **Query for Business Logic** | `List<Entity>` | Use Entity for further processing |
| **Single Record** | `Entity` | Raw entity without joins |
| **Single Record with Joins** | `VO` | Enriched data with related tables |
| **Count/Aggregation** | `Integer` / `Long` | Primitive types |
| **Insert/Update/Delete** | `int` / `void` | Affected row count (optional) |

## Pagination

### Page Object Usage

```java
// In Dao interface
List<EmployeeVO> queryEmployee(
    Page<?> page,  // ← MyBatis Plus Page object
    @Param("queryForm") EmployeeQueryForm queryForm);

// In Service layer
Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
List<EmployeeVO> list = employeeDao.queryEmployee(page, queryForm);
PageResult<EmployeeVO> result = SmartPageUtil.convert2PageResult(page, list);
```

**Important:** Page object is passed to Dao but managed in Service layer.

## Soft Delete Support

### Using @TableLogic

```java
// Entity
@Data
@TableName("t_employee")
public class EmployeeEntity {
    @TableLogic  // ← Automatic soft delete
    private Boolean deletedFlag;
}

// Dao - No special handling needed
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {
    // BaseMapper methods automatically filter deletedFlag = false
}
```

### Manual Soft Delete Filtering

```java
// Custom method that explicitly handles deletedFlag
EmployeeEntity getByLoginName(
    @Param("loginName") String loginName,
    @Param("deletedFlag") Boolean deletedFlag);
```

```xml
<select id="getByLoginName" resultType="EmployeeEntity">
    SELECT * FROM t_employee
    WHERE login_name = #{loginName}
      AND deleted_flag = #{deletedFlag}
</select>
```

## Dependency Injection

### ❌ FORBIDDEN - Dao Should Not Have Dependencies

```java
// ❌ WRONG - Dao should not inject other services
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {
    // Dao interfaces cannot have injected dependencies
}
```

**Rationale:** Dao layer is stateless and should only interact with the database. All business logic and cross-table operations belong in Service/Manager layers.

## Transaction Management

### ❌ No @Transactional in Dao

```java
// ❌ WRONG - Transactions belong in Manager layer
@Mapper
public interface EmployeeDao extends BaseMapper<EmployeeEntity> {
    @Transactional  // ← NEVER do this!
    void saveEmployee(EmployeeEntity entity);
}
```

**Correct Approach:**

```java
// ✅ In Manager Layer
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void saveEmployee(EmployeeEntity entity) {
        employeeDao.insert(entity);
        // Additional transactional operations
    }
}
```

## ArchUnit Validation

All Dao layer rules are enforced by `ArchitectureTest.java`:

```java
@Test
public void daoLayerRules() {
    classes()
        .that().resideInAPackage("..dao..")
        .should().beInterfaces()
        .andShould().haveSimpleNameEndingWith("Dao")
        .andShould().beAnnotatedWith(Mapper.class);
}
```

**Run validation:**
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

## Anti-Patterns

### ❌ Anti-Pattern 1: Business Logic in Dao

```java
// ❌ WRONG - Business validation in Dao
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
    default OrderEntity getValidOrder(Long orderId) {
        OrderEntity order = selectById(orderId);
        if (order == null || order.getDeletedFlag()) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }
}
```

**Fix:** Move validation to Service layer.

### ❌ Anti-Pattern 2: Dao Calling Dao

```java
// ❌ WRONG - Cross-Dao calls
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
    @Autowired
    private UserDao userDao;  // ← Impossible in interface!
}
```

**Fix:** Handle cross-table logic in Service/Manager layer.

### ❌ Anti-Pattern 3: Complex Logic in Dao

```java
// ❌ WRONG - Complex calculation in Dao
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
    default BigDecimal calculateTotalRevenue(Long userId) {
        List<OrderEntity> orders = selectList(
            Wrappers.<OrderEntity>lambdaQuery()
                .eq(OrderEntity::getUserId, userId));
        return orders.stream()
            .map(OrderEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**Fix:** Use SQL aggregation or move calculation to Service layer.

## Best Practices Summary

| ✅ Do | ❌ Don't |
|------|---------|
| Use `@Mapper` annotation | Use `@Repository` |
| Extend `BaseMapper<Entity>` | Implement custom base interfaces |
| Use `@Param` for all parameters | Rely on parameter index |
| Keep methods stateless | Add instance fields |
| Return Entity or VO | Return Map/Object |
| Use XML for complex SQL | Use default methods with complex logic |
| Follow naming conventions | Use generic names like `query()` |

## Related Documentation

- [MyBatis Plus Core Rules](../../../../.agent/rules/technology/database/09-mybatis-plus-core.md)
- [Architecture Rules](../../../../.agent/foundation/10-architecture-rules.md)
- [SmartAdmin Patterns](../../../shared/knowledge/smartadmin-patterns.md)
