# LambdaQueryWrapper Usage Guide

Complete guide to MyBatis Plus LambdaQueryWrapper for type-safe queries in SmartAdmin.

## Overview

`LambdaQueryWrapper` provides compile-time type safety for building SQL queries without writing XML mappers.

**When to Use:**
- Single table queries
- Simple conditions (≤ 5 conditions)
- Dynamic WHERE clause
- Simple ORDER BY

**When to Use XML Mapper Instead:**
- Multi-table JOIN
- Complex subqueries
- Custom SQL functions
- Performance-critical queries

## Basic Query Patterns

### Simple Equality Query

```java
// Query by single field
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId);
List<Employee> employees = employeeDao.selectList(wrapper);

// Query by multiple fields
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId)
    .eq(Employee::getDeletedFlag, false);
```

### Conditional Query

```java
// Only add condition if value is not null
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(departmentId != null, Employee::getDepartmentId, departmentId)
    .eq(disabledFlag != null, Employee::getDisabledFlag, disabledFlag);
```

### String Matching

```java
// LIKE query
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .like(StringUtils.isNotBlank(name), Employee::getActualName, name);
// SQL: WHERE actual_name LIKE '%name%'

// LEFT LIKE query
wrapper.likeLeft(Employee::getActualName, name);
// SQL: WHERE actual_name LIKE '%name'

// RIGHT LIKE query
wrapper.likeRight(Employee::getActualName, name);
// SQL: WHERE actual_name LIKE 'name%'
```

### Range Queries

```java
// Greater than / Less than
LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
    .gt(Order::getAmount, minAmount)        // >
    .lt(Order::getAmount, maxAmount);       // <

// Greater/Equal, Less/Equal
wrapper.ge(Order::getCreateTime, startTime) // >=
       .le(Order::getCreateTime, endTime);  // <=

// BETWEEN
wrapper.between(Order::getAmount, minAmount, maxAmount);
// SQL: WHERE amount BETWEEN minAmount AND maxAmount
```

### IN Query

```java
// IN clause
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .in(CollectionUtils.isNotEmpty(deptIds), Employee::getDepartmentId, deptIds);
// SQL: WHERE department_id IN (1, 2, 3)

// NOT IN
wrapper.notIn(Employee::getEmployeeId, excludedIds);
// SQL: WHERE employee_id NOT IN (10, 20, 30)
```

## Sorting

### Single Column Sort

```java
// ORDER BY DESC
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .orderByDesc(Employee::getCreateTime);

// ORDER BY ASC
wrapper.orderByAsc(Employee::getActualName);
```

### Multiple Column Sort

```java
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .orderByDesc(Employee::getDepartmentId)
    .orderByAsc(Employee::getActualName);
// SQL: ORDER BY department_id DESC, actual_name ASC
```

### Dynamic Sort

```java
// Sort based on user input
String sortField = queryForm.getSortField(); // "createTime"
String sortOrder = queryForm.getSortOrder(); // "DESC"

LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .orderBy(StringUtils.isNotBlank(sortField),
             "DESC".equals(sortOrder),
             Employee::getCreateTime);
```

## Pagination

### Basic Pagination

```java
// In Service layer
Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);

LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId)
    .orderByDesc(Employee::getCreateTime);

List<Employee> list = employeeDao.selectList(page, wrapper);
PageResult<EmployeeVO> result = SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
```

## Complex Conditions

### OR Conditions

```java
// (condition1 OR condition2) AND condition3
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .and(w -> w.eq(Employee::getDisabledFlag, false)
               .or()
               .isNull(Employee::getDisabledFlag))
    .eq(Employee::getDeletedFlag, false);
// SQL: WHERE (disabled_flag = false OR disabled_flag IS NULL) AND deleted_flag = false
```

### Nested Conditions

```java
// Complex nested logic
LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
    .nested(w -> w.eq(Order::getStatus, OrderStatus.PENDING)
                  .or()
                  .eq(Order::getStatus, OrderStatus.PROCESSING))
    .ge(Order::getCreateTime, startDate);
// SQL: WHERE (status = 1 OR status = 2) AND create_time >= '2026-01-01'
```

### Multiple OR Groups

```java
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .and(w -> w.like(Employee::getActualName, keyword)
               .or()
               .like(Employee::getPhone, keyword)
               .or()
               .like(Employee::getLoginName, keyword))
    .eq(Employee::getDeletedFlag, false);
// SQL: WHERE (actual_name LIKE '%keyword%' OR phone LIKE '%keyword%' OR login_name LIKE '%keyword%')
//       AND deleted_flag = false
```

## NULL Handling

```java
// IS NULL
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .isNull(Employee::getEmail);

// IS NOT NULL
wrapper.isNotNull(Employee::getPhone);
```

## Select Specific Columns

```java
// Select only specific fields (performance optimization)
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .select(Employee::getEmployeeId, Employee::getActualName, Employee::getPhone)
    .eq(Employee::getDepartmentId, deptId);
// SQL: SELECT employee_id, actual_name, phone FROM t_employee WHERE department_id = ?
```

## Aggregation Queries

### Count

```java
// Count total records
Long count = employeeDao.selectCount(
    Wrappers.<Employee>lambdaQuery()
        .eq(Employee::getDepartmentId, deptId)
        .eq(Employee::getDeletedFlag, false)
);
```

### Exists Check

```java
// Check if record exists
Long count = employeeDao.selectCount(
    Wrappers.<Employee>lambdaQuery()
        .eq(Employee::getLoginName, loginName)
        .last("LIMIT 1")
);
boolean exists = count > 0;
```

## Performance Optimization

### LIMIT Query

```java
// Limit results for performance
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId)
    .last("LIMIT 10");
```

### SELECT Optimization

```java
// ❌ Bad - Fetches all columns
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId);
List<Employee> list = employeeDao.selectList(wrapper);

// ✅ Good - Only fetch needed columns
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .select(Employee::getEmployeeId, Employee::getActualName)
    .eq(Employee::getDepartmentId, deptId);
```

### Index Usage

```java
// ✅ Good - Uses index on employee_id
wrapper.eq(Employee::getEmployeeId, employeeId);

// ❌ Bad - Function on indexed column prevents index usage
wrapper.apply("DATE(create_time) = {0}", targetDate);

// ✅ Good - Range query can use index
wrapper.ge(Employee::getCreateTime, startOfDay)
       .lt(Employee::getCreateTime, endOfDay);
```

## Common Patterns

### Standard Query Pattern

```java
public PageResult<EmployeeVO> queryEmployee(EmployeeQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<EmployeeEntity> wrapper = Wrappers.<EmployeeEntity>lambdaQuery()
        .like(StringUtils.isNotBlank(form.getKeyword()), EmployeeEntity::getActualName, form.getKeyword())
        .eq(form.getDepartmentId() != null, EmployeeEntity::getDepartmentId, form.getDepartmentId())
        .eq(form.getDisabledFlag() != null, EmployeeEntity::getDisabledFlag, form.getDisabledFlag())
        .eq(EmployeeEntity::getDeletedFlag, false)
        .orderByDesc(EmployeeEntity::getCreateTime);

    List<EmployeeEntity> list = employeeDao.selectList(page, wrapper);
    return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
}
```

### Batch Query Pattern

```java
public List<EmployeeVO> getEmployeesByIds(List<Long> employeeIds) {
    if (CollectionUtils.isEmpty(employeeIds)) {
        return Collections.emptyList();
    }

    LambdaQueryWrapper<EmployeeEntity> wrapper = Wrappers.<EmployeeEntity>lambdaQuery()
        .in(EmployeeEntity::getEmployeeId, employeeIds)
        .eq(EmployeeEntity::getDeletedFlag, false);

    List<EmployeeEntity> entities = employeeDao.selectList(wrapper);
    return SmartBeanUtil.copyList(entities, EmployeeVO.class);
}
```

### Dynamic Search Pattern

```java
public List<EmployeeVO> search(EmployeeSearchForm form) {
    LambdaQueryWrapper<EmployeeEntity> wrapper = Wrappers.<EmployeeEntity>lambdaQuery();

    // Keyword search across multiple fields
    if (StringUtils.isNotBlank(form.getKeyword())) {
        wrapper.and(w -> w
            .like(EmployeeEntity::getActualName, form.getKeyword())
            .or()
            .like(EmployeeEntity::getPhone, form.getKeyword())
            .or()
            .like(EmployeeEntity::getLoginName, form.getKeyword())
        );
    }

    // Optional filters
    wrapper.eq(form.getDepartmentId() != null, EmployeeEntity::getDepartmentId, form.getDepartmentId())
           .eq(form.getDisabledFlag() != null, EmployeeEntity::getDisabledFlag, form.getDisabledFlag())
           .eq(EmployeeEntity::getDeletedFlag, false)
           .orderByDesc(EmployeeEntity::getCreateTime);

    List<EmployeeEntity> list = employeeDao.selectList(wrapper);
    return SmartBeanUtil.copyList(list, EmployeeVO.class);
}
```

## Anti-Patterns

### ❌ Anti-Pattern 1: N+1 Query

```java
// ❌ WRONG - N+1 query problem
List<Order> orders = orderDao.selectList(wrapper);
for (Order order : orders) {
    User user = userDao.selectById(order.getUserId());  // ← N queries!
    order.setUserName(user.getUserName());
}

// ✅ FIX - Batch query or use XML Mapper JOIN
List<Order> orders = orderDao.selectList(wrapper);
List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
List<User> users = userDao.selectBatchIds(userIds);
Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, u -> u));
orders.forEach(order -> {
    User user = userMap.get(order.getUserId());
    if (user != null) {
        order.setUserName(user.getUserName());
    }
});
```

### ❌ Anti-Pattern 2: Inefficient String Matching

```java
// ❌ WRONG - Full table scan
wrapper.apply("LOWER(actual_name) = LOWER({0})", name);

// ✅ BETTER - Use database collation
wrapper.eq(Employee::getActualName, name);  // Case-insensitive with utf8mb4_general_ci

// ✅ OR - Create functional index
// CREATE INDEX idx_actual_name_lower ON t_employee (LOWER(actual_name));
```

### ❌ Anti-Pattern 3: Over-fetching Data

```java
// ❌ WRONG - Fetches all columns
List<Employee> employees = employeeDao.selectList(
    Wrappers.<Employee>lambdaQuery()
        .eq(Employee::getDepartmentId, deptId)
);
// Only need employeeId and name but fetching entire entity

// ✅ GOOD - Select only needed columns
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .select(Employee::getEmployeeId, Employee::getActualName)
    .eq(Employee::getDepartmentId, deptId);
List<Employee> employees = employeeDao.selectList(wrapper);
```

## Related Documentation

- [Dao Architecture Guide](dao-architecture.md)
- [XML Mapper Patterns](xml-mapper-patterns.md)
- [MyBatis Plus Core Rules](../../../../.agent/rules/09-mybatis-plus-core.md)
