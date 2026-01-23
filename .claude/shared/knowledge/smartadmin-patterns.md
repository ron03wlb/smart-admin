# SmartAdmin Implementation Patterns

This document provides SmartAdmin-specific implementation patterns and code examples. **All agents must adhere to these patterns when working with this codebase.**

**For detailed architectural rules and enforcement**, see: [.agent/rules/10-architecture-rules.md](../../../.agent/rules/10-architecture-rules.md)

## Layered Architecture Overview

SmartAdmin enforces strict layering rules validated by `ArchitectureTest.java`:

```
Controller → Service → Manager → Dao → Entity
```

**Quick Reference** (see [architecture rules](../../../.agent/rules/10-architecture-rules.md) for complete details):

### Layer Responsibilities

**Controller Layer (`@RestController`):**
- REST API endpoints with proper request mappings
- Request validation using `@Valid`
- Authorization checks with `@SaCheckPermission`
- Calls Service layer ONLY (never Manager or Dao directly)
- Returns `ResponseDTO` objects

**Service Layer (`@Service`):**
- Core business logic and validation
- Orchestrates Manager and Dao calls
- Converts between Forms, Entities, and VOs
- Returns `ResponseDTO` or throws `BusinessException`
- Never accesses Dao directly if Manager layer exists

**Manager Layer (`@Service` with transaction/cache focus):**
- `@Transactional(rollbackFor = Throwable.class)` for transactions
- `@Cacheable` for read-heavy operations
- Complex business logic requiring multiple Dao calls
- Never calls other Managers or Services
- Calls Dao ONLY

**Dao Layer (MyBatis Plus):**
- Extends `BaseMapper<Entity>`
- Type-safe queries using `LambdaQueryWrapper`
- Custom SQL in XML mappers when needed

**Entity Layer:**
- Database mapping with `@TableName`
- Domain objects representing database tables

### Critical Architecture Rules

**MANDATORY** (enforced by ArchitectureTest - see [complete rules](../../../.agent/rules/10-architecture-rules.md)):
- ✅ Controller → Service ONLY
- ✅ `@Transactional` / `@Cacheable`: Manager layer ONLY
- ✅ Constructor injection via `@RequiredArgsConstructor` + `private final`
- ❌ NEVER `@Autowired` field injection

→ **[Complete Architecture Rules & ArchUnit Tests](../../../.agent/rules/10-architecture-rules.md)**
→ **[Manager Layer Constraints](../../../.agent/rules/09-manager-layer.md)**

## ResponseDTO Pattern

All API responses must use the ResponseDTO pattern:

```java
// Success responses
return ResponseDTO.ok(data);              // With data
return ResponseDTO.ok();                  // Without data
return ResponseDTO.okMsg("Created");      // With custom message

// Error responses
return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
return ResponseDTO.userErrorParam("Invalid email format");

// Exception throwing (caught by GlobalExceptionHandler)
throw new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_EXIST);
```

## Domain Object Pattern

### Entity - Database Mapping
```java
@TableName("t_employee")
public class EmployeeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long departmentId;
    // ...
}
```

### Form - Request Input with Validation
```java
public class EmployeeAddForm {
    @NotBlank(message = "Name required")
    @Length(max = 50, message = "Name too long")
    private String name;

    @NotNull(message = "Department required")
    private Long departmentId;
    // ...
}
```

### VO - Response Output
```java
public class EmployeeVO {
    private Long id;
    private String name;
    private String departmentName;
    // ...
}
```

### QueryForm - Paginated Queries
```java
public class EmployeeQueryForm extends PageParam {
    private String name;
    private Long departmentId;
    // ...
}
```

## Bean Conversion

Use `SmartBeanUtil` for object conversion:

```java
// Single object conversion
EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
EmployeeVO vo = SmartBeanUtil.copy(entity, EmployeeVO.class);

// List conversion
List<EmployeeVO> voList = SmartBeanUtil.copyList(entities, EmployeeVO.class);
```

## Pagination Pattern

Use `SmartPageUtil` for paginated queries:

```java
public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
    // Convert form to MyBatis Plus Page object
    Page<?> page = SmartPageUtil.convert2PageQuery(form);

    // Build query wrapper
    LambdaQueryWrapper<EmployeeEntity> wrapper = Wrappers.<EmployeeEntity>lambdaQuery()
        .like(StringUtils.isNotBlank(form.getName()), EmployeeEntity::getName, form.getName())
        .eq(form.getDepartmentId() != null, EmployeeEntity::getDepartmentId, form.getDepartmentId());

    // Execute query
    List<EmployeeEntity> list = employeeDao.selectList(page, wrapper);

    // Convert to PageResult with VO
    return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
}
```

## MyBatis Plus Patterns

**Prefer `LambdaQueryWrapper` for type safety:**

```java
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId)
    .like(StringUtils.isNotBlank(name), Employee::getName, name)
    .orderByDesc(Employee::getCreateTime);

List<Employee> employees = employeeDao.selectList(wrapper);
```

**Conditional queries:**
```java
wrapper.eq(condition, Entity::getField, value)  // Only adds condition if true
```

## Authentication & Authorization (Sa-Token)

```java
// Skip authentication
@NoNeedLogin

// Require specific permission
@SaCheckPermission("employee:add")

// Require all permissions
@SaCheckPermission({"employee:add", "employee:update"})

// Get current user
RequestUser user = AdminRequestUtil.getRequestUser();
Long userId = user.getUserId();
String userName = user.getUserName();
```

## Dependency Injection

**MANDATORY pattern - Constructor Injection:**

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final DepartmentManager deptManager;
    // Constructor injection via Lombok - NEVER use @Autowired fields
}
```

**❌ FORBIDDEN - Field Injection:**
```java
// NEVER DO THIS
@Autowired
private EmployeeDao employeeDao;
```

## Transaction Management

**Transactions ONLY in Manager layer:**

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)  // NOT Exception.class
    public void updateWithRoles(EmployeeEntity entity, List<Long> roleIds) {
        employeeDao.updateById(entity);
        employeeRoleDao.batchInsert(roleIds);
        // Both operations in same transaction
    }
}
```

**Key points:**
- ✅ Use `rollbackFor = Throwable.class` (catches all errors including Error)
- ❌ Never use `rollbackFor = Exception.class` (misses Error subclasses)
- ✅ Only in Manager layer
- ❌ Never in Service or Controller

## Naming Conventions

Follow Alibaba Java naming conventions:

**Classes:**
- Controller: `EmployeeController`
- Service: `EmployeeService`
- Manager: `EmployeeManager`
- Dao: `EmployeeDao`
- Entity: `EmployeeEntity`
- Form: `EmployeeAddForm`, `EmployeeUpdateForm`, `EmployeeQueryForm`
- VO: `EmployeeVO`

**Boolean Fields:**
- ✅ Use: `deleted`, `enabled`, `active`
- ❌ Avoid: `isDeleted`, `isEnabled`, `isActive`

**Methods:**
- Query: `getUserById()`, `listUsers()`, `countUsers()`
- Modify: `saveUser()`, `updateUser()`, `deleteUser()`
- Validation: `validateEmail()`, `checkPermission()`

## Exception Handling

**Use BusinessException with ErrorCode:**

```java
// Check and throw
if (employee == null) {
    throw new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_EXIST);
}

// Custom message
throw new BusinessException("Employee email already exists");
```

**Logging:**
- Use SLF4j with placeholders (never string concatenation)
- Log at appropriate levels (ERROR, WARN, INFO, DEBUG)

```java
// ✅ Correct
log.info("Processing employee, id={}, name={}", id, name);

// ❌ Wrong - string concatenation
log.info("Processing employee, id=" + id + ", name=" + name);
```

## Anti-Patterns to Avoid

| ❌ Anti-Pattern | ✅ Correct Pattern |
|-----------------|-------------------|
| `@Autowired` field injection | `@RequiredArgsConstructor` + `private final` |
| `@Transactional` in Service | Manager layer only |
| `rollbackFor = Exception.class` | `rollbackFor = Throwable.class` |
| Controller → Dao directly | Controller → Service → Dao |
| `return null` for errors | `ResponseDTO.error()` or `throw BusinessException` |
| `QueryWrapper` with strings | `LambdaQueryWrapper` for type safety |
| `private Boolean isDeleted` | `private Boolean deleted` |
| String concat in logs | `log.info("x={}", x)` |
| Empty catch blocks | Log + rethrow as BusinessException |

## Verification

Before committing code, run architecture validation:

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

This test enforces all architectural rules and will fail if patterns are violated.

## References

- See `CLAUDE.md` in project root for complete coding standards
- See `.agent/rules/` directory for detailed rule documentation
