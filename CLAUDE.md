# CLAUDE.md

**Quick Reference Card** for SmartAdmin development. This document provides essential patterns, commands, and conventions for rapid development.

**For detailed guidance**, see:
- [.claude/ Directory](.claude/README.md) - AI agent system, multi-agent workflows, and orchestration
- [.agent/rules/](.agent/rules/) - Comprehensive coding standards and technical rules
- [Architecture Documentation](docs/) - High-level architecture and design decisions

---

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

**See also**:
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md) - Detailed implementation patterns
- [Project Architecture](.claude/shared/knowledge/project-architecture.md) - Module structure and build configuration
- [Quality Standards](.claude/shared/knowledge/quality-standards.md) - Code quality checklist

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

**Most critical anti-patterns:**

| Anti-Pattern | Correct Pattern |
|--------------|-----------------|
| `@Transactional` in Service | Manager layer only |
| `@Autowired` field injection | `@RequiredArgsConstructor` + `private final` |
| Controller → Dao directly | Controller → Service → Dao |

**Complete list**: See [Quality Standards](.claude/shared/knowledge/quality-standards.md#anti-patterns-to-avoid) for all anti-patterns and detailed explanations.

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.4 |
| MyBatis Plus | 3.5.12 |
| Sa-Token | 1.44.0 |
| Redisson | 3.50.0 |
| Knife4j | 4.6.0 |

**See also**: [Project Architecture](.claude/shared/knowledge/project-architecture.md) for detailed version compatibility and configuration.

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

**See also**:
- [Quality Standards](.claude/shared/knowledge/quality-standards.md) - Code quality checklist
- [.claude/ Directory](.claude/README.md) - Agent system and orchestration

## Quality Tool Patterns

Common quality tool violations and approved solutions:

### PMD Suppressions
- **CallSuperInConstructor**: Empty constructors (default behavior is acceptable)
- **AvoidReassigningParameters**: Create local variable copy instead
- **ShortClassName**: Use `@SuppressWarnings` for utility inner classes (Dict, Expire)
- **MissingStaticMethodInNonInstantiatableClass**: Constant-only classes are valid

### SpotBugs Exclusions
- **EI_EXPOSE_REP/EI_EXPOSE_REP2**: DTO/VO/Form/Config classes don't need defensive copying
- **NP_NULL_ON_SOME_PATH**: CompletableFuture.getNow(null) and Kafka null keys are valid
- **ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD**: @PostConstruct static field initialization pattern
- **CT_CONSTRUCTOR_THROW**: Constructor validation pattern is safe for internal classes

Detailed rules: See [.agent/rules/12-pmd-rules.md](.agent/rules/12-pmd-rules.md) and [.agent/rules/13-spotbugs-rules.md](.agent/rules/13-spotbugs-rules.md)

---

## Version

**Document Version**: 1.0.0
**Last Updated**: 2026-01-22
**Aligned with**: .claude/ v2.5.0, .agent/rules/ (latest)

**Change History**:
- 1.0.0 (2026-01-22): Initial versioned release with cross-references to .claude/ and improved navigation
