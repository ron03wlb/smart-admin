# Code Quality Standards

This document provides a quality checklist and quick reference for SmartAdmin code standards. **All agents must enforce these standards when writing or reviewing code.**

**For detailed technical rules**, see:
- [Architecture Rules](../../../.agent/rules/10-architecture-rules.md) - Layered architecture enforcement
- [Naming Conventions](../../../.agent/rules/01-naming-conventions.md) - Complete Alibaba guidelines
- [Exception & Logging](../../../.agent/rules/04-exception-logging.md) - Error handling standards
- [Quality Tool Rules](../../../.agent/rules/) - PMD, SpotBugs, Checkstyle, etc.

## Code Quality Checklist

Before completing any implementation, verify:

### Architectural Compliance
- [ ] Correct layer invocation (Controller → Service → Manager → Dao)
- [ ] Constructor injection used (not field injection with `@Autowired`)
- [ ] `@Transactional` only in Manager with `rollbackFor = Throwable.class`
- [ ] No circular dependencies between modules
- [ ] ArchitectureTest passes

### Exception Handling
- [ ] Proper exception handling (BusinessException with ErrorCode)
- [ ] ResponseDTO pattern used correctly
- [ ] No empty catch blocks
- [ ] Exceptions logged before rethrowing
- [ ] Stack traces preserved when wrapping exceptions

### Bean Conversion & Pagination
- [ ] Bean conversion via SmartBeanUtil (not manual)
- [ ] Pagination using SmartPageUtil
- [ ] LambdaQueryWrapper for type safety (not string-based QueryWrapper)

### Validation & Security
- [ ] Validation annotations on Form classes (`@NotBlank`, `@NotNull`, etc.)
- [ ] `@SaCheckPermission` on protected endpoints
- [ ] Input sanitization for user-provided data
- [ ] SQL injection prevention via parameterized queries

### Naming & Style
- [ ] Naming conventions followed (Alibaba guidelines)
- [ ] Boolean fields named correctly (`deleted`, not `isDeleted`)
- [ ] Meaningful variable names (avoid single letters except loops)
- [ ] Consistent method naming patterns

### Logging & Monitoring
- [ ] Logging with SLF4j placeholders (not string concatenation)
- [ ] Appropriate log levels (ERROR, WARN, INFO, DEBUG)
- [ ] Sensitive data not logged
- [ ] Performance metrics logged where appropriate

### Testing
- [ ] Unit tests written for business logic
- [ ] Test coverage > 85%
- [ ] Integration tests for API endpoints
- [ ] Edge cases covered
- [ ] Mock external dependencies

## Naming Conventions

→ **[Complete Naming Conventions (Alibaba Guidelines)](../../../.agent/rules/01-naming-conventions.md)**

**Quick Reference:**

### Class Naming

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `{Entity}Controller` | `EmployeeController` |
| Service | `{Entity}Service` | `EmployeeService` |
| Manager | `{Entity}Manager` | `EmployeeManager` |
| Dao | `{Entity}Dao` | `EmployeeDao` |
| Entity | `{Entity}Entity` | `EmployeeEntity` |
| Form | `{Entity}{Action}Form` | `EmployeeAddForm`, `EmployeeUpdateForm`, `EmployeeQueryForm` |
| VO | `{Entity}VO` | `EmployeeVO` |
| Enum | `{Name}Enum` | `UserStatusEnum`, `OrderTypeEnum` |
| Constant | `{Name}Constant` | `RedisKeyConstant`, `SystemConstant` |
| Exception | `{Name}Exception` | `BusinessException`, `ValidationException` |
| Util | `{Name}Util` | `DateUtil`, `StringUtil` |

### Variable Naming

**Boolean Fields:**
- ✅ `deleted`, `enabled`, `active`, `visible`
- ❌ `isDeleted`, `isEnabled`, `isActive`, `isVisible`

**Collections:**
- ✅ `employeeList`, `userMap`, `roleSet`
- ❌ `employees`, `users`, `roles` (use explicit collection type)

**Constants:**
- ✅ `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`, `REDIS_KEY_PREFIX`
- Use UPPER_SNAKE_CASE for constants

**Method Names:**
- Query: `get{Entity}ById()`, `list{Entity}s()`, `count{Entity}s()`
- Modify: `save{Entity}()`, `update{Entity}()`, `delete{Entity}()`
- Check: `validate{Something}()`, `check{Condition}()`, `is{Condition}()`
- Convert: `convert{Source}To{Target}()`, `to{Target}()`

## Exception Handling Standards

### Using BusinessException

```java
// With ErrorCode enum
if (user == null) {
    throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
}

// With custom message
if (email.exists()) {
    throw new BusinessException("Email already registered");
}

// With formatted message
throw new BusinessException(String.format("Invalid status: %s", status));
```

### Proper Exception Logging

```java
// ✅ Correct - Log then throw
try {
    riskyOperation();
} catch (IOException e) {
    log.error("Failed to read file, path={}", filePath, e);
    throw new BusinessException("File read failed", e);
}

// ❌ Wrong - Empty catch block
try {
    riskyOperation();
} catch (IOException e) {
    // Silent failure - NEVER do this
}

// ❌ Wrong - Lost stack trace
try {
    riskyOperation();
} catch (IOException e) {
    throw new BusinessException("File read failed");  // Original exception lost!
}
```

### Exception Handling Hierarchy

1. **Business Exceptions**: Expected errors, use `BusinessException`
2. **Validation Errors**: Use `@Valid` annotations, caught by exception handler
3. **System Exceptions**: Unexpected errors, log full stack trace
4. **External API Failures**: Wrap in `BusinessException` with context

## Logging Standards

### Use SLF4j with Placeholders

```java
// ✅ Correct - Placeholders
log.info("Processing order, orderId={}, userId={}", orderId, userId);
log.error("Payment failed, orderId={}, amount={}", orderId, amount, exception);

// ❌ Wrong - String concatenation
log.info("Processing order, orderId=" + orderId + ", userId=" + userId);

// ❌ Wrong - String.format (unnecessary overhead)
log.info(String.format("Processing order, orderId=%s, userId=%s", orderId, userId));
```

### Log Levels

| Level | Use Case | Example |
|-------|----------|---------|
| **ERROR** | System errors, exceptions, failed operations | `log.error("Database connection failed", ex)` |
| **WARN** | Recoverable errors, deprecated API usage | `log.warn("Retry attempt {}, max retries {}", attempt, maxRetries)` |
| **INFO** | Business events, lifecycle events | `log.info("User logged in, userId={}", userId)` |
| **DEBUG** | Detailed diagnostic info, development debugging | `log.debug("Query executed, sql={}, params={}", sql, params)` |

### What NOT to Log

- ❌ Passwords, tokens, API keys
- ❌ Credit card numbers, SSNs, PII
- ❌ Excessive data in loops (use aggregation)
- ❌ Same message repeatedly (use rate limiting)

## Testing Standards

### Unit Test Structure

```java
@SpringBootTest
public class EmployeeServiceTest {

    @Autowired
    private EmployeeService employeeService;

    @MockBean
    private EmployeeDao employeeDao;

    @Test
    public void testAddEmployee_Success() {
        // Given - Setup test data
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName("John Doe");

        when(employeeDao.insert(any())).thenReturn(1);

        // When - Execute method
        ResponseDTO<Long> result = employeeService.addEmployee(form);

        // Then - Verify results
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        verify(employeeDao, times(1)).insert(any());
    }

    @Test
    public void testAddEmployee_DuplicateEmail() {
        // Test error case
        EmployeeAddForm form = new EmployeeAddForm();
        form.setEmail("duplicate@example.com");

        when(employeeDao.selectByEmail(form.getEmail()))
            .thenReturn(existingEmployee);

        // Should throw BusinessException
        assertThrows(BusinessException.class, () -> {
            employeeService.addEmployee(form);
        });
    }
}
```

### Test Coverage Requirements

- **Minimum**: 85% line coverage
- **Service Layer**: 100% coverage (all business logic paths)
- **Manager Layer**: 100% coverage (transaction scenarios)
- **Controller Layer**: Integration tests for all endpoints
- **Edge Cases**: Null inputs, empty lists, boundary values

### Test Naming

Pattern: `test{MethodName}_{Scenario}`

Examples:
- `testAddEmployee_Success`
- `testAddEmployee_DuplicateEmail`
- `testQueryEmployees_WithPagination`
- `testDeleteEmployee_NotFound`

## Performance Standards

### Query Optimization

- Avoid N+1 query problems
- Use batch operations for multiple records
- Implement pagination for large result sets
- Create appropriate indexes
- Use `@Cacheable` in Manager layer for read-heavy operations

### Transaction Optimization

- Keep transactions short
- Avoid long-running transactions
- Don't call external APIs within transactions
- Use `@Transactional` only where needed

### Code Efficiency

- Avoid unnecessary object creation in loops
- Use appropriate data structures (HashMap vs List)
- Prefer streams for readability, loops for performance-critical code
- Cache expensive computations

## Anti-Patterns to Avoid

### Critical Anti-Patterns

| ❌ Never Do This | ✅ Do This Instead | Why |
|------------------|-------------------|-----|
| `@Autowired` field injection | Constructor injection with `@RequiredArgsConstructor` | Testability, immutability |
| `@Transactional` in Service | `@Transactional` in Manager only | Architecture rules |
| `rollbackFor = Exception.class` | `rollbackFor = Throwable.class` | Catches Error subclasses |
| Controller → Dao | Controller → Service → Dao | Layering rules |
| Empty catch blocks | Log + rethrow | Error visibility |
| String concat in logs | SLF4j placeholders | Performance |
| `QueryWrapper` with strings | `LambdaQueryWrapper` | Type safety |
| `isDeleted` field name | `deleted` | Naming convention |
| Return `null` for errors | `ResponseDTO.error()` or throw | Explicit error handling |

### Code Smells

Watch for these warning signs:
- Methods longer than 50 lines
- Classes with more than 10 dependencies
- Cyclomatic complexity > 10
- Duplicate code blocks
- God classes (>500 lines)
- Long parameter lists (>5 parameters)

## Code Review Focus Areas

When reviewing code, prioritize:

1. **Architecture Violations** - Must be fixed (run ArchitectureTest)
2. **Security Issues** - SQL injection, XSS, authentication bypass
3. **Data Loss Risks** - Transaction handling, error scenarios
4. **Performance Problems** - N+1 queries, missing indexes
5. **Maintainability** - Code clarity, naming, documentation
6. **Test Coverage** - Critical paths tested

## Commit Quality

### Before Committing

1. Run architecture tests: `./gradlew :sa-admin:test --tests ArchitectureTest`
2. Run all tests: `./gradlew :sa-admin:test`
3. Verify build succeeds: `./gradlew build`
4. Review your own changes
5. Ensure commit message follows convention

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `perf`, `chore`
Scopes: `sa-admin`, `sa-base`, `sa-common`

## Documentation Requirements

### Code Documentation

- JavaDoc for public APIs
- Inline comments for complex logic only
- README.md for new modules
- Architecture decisions documented

### What to Document

- ✅ Public API methods (JavaDoc)
- ✅ Complex algorithms (inline comments)
- ✅ Business rules (inline comments)
- ✅ Workarounds and TODOs
- ❌ Obvious code (self-documenting)
- ❌ Redundant comments

## Discovered Rules (Hook-Generated)

This section is automatically maintained by the hooks system when issues are discovered and fixed. These rules represent real issues found in the codebase and their solutions, serving as a knowledge base for the team.

**How This Works**:
1. code-reviewer and architect-reviewer find issues
2. java-architect fixes them
3. Hooks system records successful fixes here
4. Rules accumulate over time, improving code quality

**Format**: Each rule follows this template:

### Rule: [Rule Name]
- **Discovered**: [Date]
- **Severity**: [Critical/Major/Minor]
- **Category**: [Architecture/Security/Performance/Code Quality]
- **Issue ID**: [CR-XXX or AR-XXX]
- **Anti-Pattern**:
  ```java
  // Bad example that was found
  ```
- **Correct Pattern**:
  ```java
  // Good example after fix
  ```
- **Rationale**: Why this rule exists
- **References**: Related sections in other docs

---

**Note**: Rules below this line are automatically generated. Manual edits may be overwritten by hooks system.

---

<!-- AUTO-GENERATED RULES START -->

_No rules have been auto-generated yet. Rules will appear here after the first java-architect auto-fix cycle._

<!-- AUTO-GENERATED RULES END -->

---

## References

- **Alibaba Java Coding Guidelines**: See `.agent/rules/01-naming-conventions.md`
- **Exception Handling**: See `.agent/rules/04-exception-logging.md`
- **Architecture Rules**: See `.agent/rules/10-architecture-rules.md`
- **SmartAdmin Patterns**: See `smartadmin-patterns.md` in this directory
