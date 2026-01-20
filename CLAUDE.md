# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference Card

| Task | Pattern |
|------|---------|
| Return success | `ResponseDTO.ok(data)` |
| Return error | `ResponseDTO.error(ErrorCode)` |
| Throw exception | `throw new BusinessException(ErrorCode)` |
| Get current user | `AdminRequestUtil.getRequestUser()` |
| Skip auth | `@NoNeedLogin` |
| Check permission | `@SaCheckPermission("module:action")` |
| Paginated query | `SmartPageUtil.convert2PageQuery(form)` |
| Bean copy | `SmartBeanUtil.copy(source, Target.class)` |
| Transaction | `@Transactional(rollbackFor = Throwable.class)` in Manager only |

## Build Commands

Backend location: `smart-admin-api-java21-springboot3/`

```bash
# Build
./gradlew clean build
./gradlew build -x test                    # Skip tests
./gradlew build -Penv=dev|test|pre|prod    # Environment-specific

# Run application
./gradlew :sa-admin:bootRun
# Access: http://localhost:1024/swagger-ui.html
```

## Test Commands

```bash
# All tests
./gradlew :sa-admin:test

# Single test class
./gradlew :sa-admin:test --tests ArchitectureTest
./gradlew :sa-admin:test --tests AdminApplicationTest
```

## Architecture

Modular monolith with strict layered architecture enforced by ArchUnit:

```
Controller → Service → Manager → Dao → Entity
     ↓          ↓          ↓        ↓
@RestController  Business   Cache   BaseMapper
 @Valid         Logic      @Transactional
```

**Module structure:**
- `sa-base/` - Infrastructure, utilities, support modules
- `sa-admin/` - Business logic, system modules
- `sa-common/` - Shared services: api-encrypt, cache, mq, redis-lock

**Layer rules (enforced via ArchitectureTest.java):**
- Controller → Service ONLY (never directly to Manager/Dao)
- Service → Manager OR Dao
- Manager → Dao ONLY (never to Service or other Managers)
- `@Transactional` / `@Cacheable`: ONLY in Manager layer
- `@Autowired` field injection: FORBIDDEN

**Package pattern per module:**
```
module/
├── controller/     # REST endpoints
├── service/        # Business logic (@Service)
├── manager/        # Caching, transactions (@Transactional)
├── dao/            # MyBatis Plus mappers
└── domain/
    ├── entity/     # Database entities (@TableName)
    ├── form/       # Request DTOs (@Valid)
    └── vo/         # Response DTOs
```

## SmartAdmin Patterns

### ResponseDTO Pattern
```java
// Success responses
return ResponseDTO.ok(data);           // With data
return ResponseDTO.ok();               // Without data
return ResponseDTO.okMsg("Created");   // With message

// Error responses
return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
return ResponseDTO.userErrorParam("Invalid email format");

// Exception throwing (caught by GlobalExceptionHandler)
throw new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_EXIST);
```

### Domain Objects
```java
// Entity - Database mapping
@TableName("t_employee")
public class EmployeeEntity { ... }

// Form - Request input with validation
public class EmployeeAddForm {
    @NotBlank(message = "Name required")
    @Length(max = 50)
    private String name;
}

// VO - Response output
public class EmployeeVO { ... }

// QueryForm - Paginated queries
public class EmployeeQueryForm extends PageParam { ... }
```

### Pagination
```java
// In Service
public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<EmployeeEntity> list = employeeDao.selectList(page, wrapper);
    return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
}
```

### Bean Conversion
```java
EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
List<EmployeeVO> voList = SmartBeanUtil.copyList(entities, EmployeeVO.class);
```

## Authentication (Sa-Token)

```java
@NoNeedLogin                              // Skip authentication
@SaCheckPermission("employee:add")        // Require specific permission
@SaCheckPermission({"a:b", "c:d"})        // Require all permissions

// Get current user
RequestUser user = AdminRequestUtil.getRequestUser();
Long userId = user.getUserId();
```

## Key Conventions

**Dependency Injection (MANDATORY):**
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;      // Constructor injection
    private final DepartmentManager deptManager;
}
// NEVER use @Autowired field injection
```

**Transaction Management (Manager layer ONLY):**
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // NOT Exception.class
    public void updateWithRoles(EmployeeEntity entity, List<Long> roleIds) { }
}
```

**Naming:**
- Classes: `UserController`, `UserService`, `UserManager`, `UserDao`, `UserEntity`
- Forms: `UserAddForm`, `UserUpdateForm`, `UserQueryForm`
- Boolean fields: `deleted` NOT `isDeleted`
- Methods: `getUserById()`, `listUsers()`, `countUsers()`, `saveUser()`, `deleteUser()`

**MyBatis Plus (prefer LambdaQueryWrapper):**
```java
LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
    .eq(Employee::getDepartmentId, deptId)
    .like(StringUtils.isNotBlank(name), Employee::getName, name)
    .orderByDesc(Employee::getCreateTime);
```

**Commit messages:** Conventional Commits format
```
<type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
Scopes: sa-admin, sa-base, sa-common, smart-admin-web, smart-app, docker, docs
```

## Anti-Patterns to Avoid

| Anti-Pattern | Correct Pattern |
|--------------|-----------------|
| `@Transactional` in Service | Manager layer only |
| `@Autowired` field injection | `@RequiredArgsConstructor` + `private final` |
| `return null` for missing entity | `ResponseDTO.error(ErrorCode)` |
| `private Boolean isDeleted` | `private Boolean deleted` |
| Controller → Dao directly | Controller → Service → Dao |
| `rollbackFor = Exception.class` | `rollbackFor = Throwable.class` |
| Empty catch blocks | Log + rethrow as BusinessException |
| String concat in logs | `log.info("x={}", x)` placeholder |
| `QueryWrapper` with strings | `LambdaQueryWrapper` for type safety |

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.4 |
| MyBatis Plus | 3.5.12 |
| Sa-Token | 1.44.0 |
| Redisson | 3.50.0 |
| Knife4j | 4.6.0 |

## Development Guidelines

Detailed coding standards in `.agent/rules/`:
- `10-architecture-rules.md` - Layered architecture
- `09-manager-layer.md` - Manager layer constraints
- `01-naming-conventions.md` - Naming standards (Alibaba guidelines)
- `17-commit-message-conventions.md` - Git conventions
- `04-exception-logging.md` - Exception & logging standards

Run architecture validation before commits:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```
