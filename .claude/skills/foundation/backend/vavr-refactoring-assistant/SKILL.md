---
name: vavr-refactoring-assistant
description: [P0 - Critical] Use when refactoring Service layer methods to use Vavr Option/Try/Either instead of java.util.Optional/checked exceptions, when code review detects Optional usage violations in Service classes, when ArchitectureTest serviceUsesVavrOption fails, or when user mentions "refactor to Vavr", "convert Optional", "use Try", "functional exceptions"
---

# Vavr Refactoring Assistant

Refactor Java code from java.util.Optional/try-catch/null checks to io.vavr Option/Try/Either patterns, enforcing SmartAdmin architectural standards.

## Quick Start

**Most common usage:**
```
User: "Refactor this Service to use Vavr Option"
User: "Convert Optional to Option"
User: "Replace try-catch with Try.of()"
User: "Fix ArchitectureTest serviceUsesVavrOption violation"
```

You will:
1. Identify anti-patterns (Optional, null checks, try-catch)
2. Apply Vavr patterns (Option, Try, Either)
3. Update imports and method signatures
4. Preserve business logic correctness
5. Verify ArchUnit compliance

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "Vavr" - Vavr library usage or refactoring
- "refactor to Vavr" - Convert code to use Vavr patterns
- "Option pattern" - io.vavr.control.Option usage
- "Try pattern" - io.vavr.control.Try usage
- "Either pattern" - io.vavr.control.Either usage
- "serviceUsesVavrOption" - ArchitectureTest violation fix

**Secondary Keywords** (Medium confidence):
- "convert Optional" - Context: from java.util.Optional to io.vavr.control.Option
- "functional exceptions" - Context: replacing try-catch with Try.of()
- "ArchitectureTest violation" - Context: Service layer Optional usage
- "Vavr refactoring" - General Vavr refactoring request

**Phrase Patterns**:
- "Refactor [Service] to use Vavr Option" - Example: "Refactor UserService to use Vavr Option"
- "Convert Optional to Option in [class]" - Example: "Convert Optional to Option in EmployeeService"
- "Replace try-catch with Try.of() in [method]" - Example: "Replace try-catch with Try.of() in findById"

**Example User Requests**:
```
User: "Refactor this Service to use Vavr Option"
User: "Convert Optional to Option in EmployeeService"
User: "Fix ArchitectureTest serviceUsesVavrOption violation"
User: "Replace try-catch with Try.of() for error handling"
```

**Note**: This skill can also be manually invoked via `/vavr-refactoring-assistant` command.

## Prerequisites

**IMPORTANT**: Before using this skill, verify Vavr dependency is in your project:

### 1. Check build.gradle.kts

Vavr must be added to `sa-base/foundation/core/build.gradle.kts`:

```kotlin
dependencies {
    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)

    // Vavr - Functional programming library
    api("io.vavr:vavr:0.10.4")  // ← Required for this skill
}
```

**Verification command:**
```bash
./gradlew :sa-base:foundation:core:dependencies --configuration api | grep vavr
```

**Expected output:**
```
+--- io.vavr:vavr:0.10.4
```

### 2. If Vavr dependency is missing

**Symptom**: Compilation error `package io.vavr.control does not exist`

**Fix**: Add dependency to `sa-base/foundation/core/build.gradle.kts` (see above), then:
```bash
./gradlew :sa-admin:compileJava
```

### 3. Create verification test (optional)

Create `sa-admin/src/test/java/net/lab1024/sa/admin/VavrDependencyTest.java`:
```java
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VavrDependencyTest {
  @Test
  void testVavrOptionAvailable() {
    Option<String> some = Option.of("test");
    assertTrue(some.isDefined());
  }
}
```

**Once Vavr is confirmed available**, proceed with refactoring.

---

## Critical Rules

**MUST enforce:**
- Service layer returns `Option<T>` NOT `Optional<T>` (ArchUnit violation)
- Use `Try.of()` for exceptions NOT `try-catch`
- No explicit null checks (`if (obj == null)`)
- Controller layer handles Option/Try (NOT Service)
- MyBatis integration: `Option.of(dao.selectById(id))` NOT `dao.selectById(id)`

## Refactoring Patterns

### Pattern 1: Optional → Option

**BEFORE:**
```java
// ❌ ArchUnit violation: Service uses Optional
public Optional<Employee> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));
}
```

**AFTER:**
```java
// ✅ Correct: Service uses Option
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id));
}
```

**Import updates:**
```java
// Remove
import java.util.Optional;

// Add
import io.vavr.control.Option;
```

---

### Pattern 2: try-catch → Try.of()

**BEFORE:**
```java
// ❌ Anti-pattern: try-catch in Service
public String readConfig(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("Config read failed", e);
        return "default-config";
    }
}
```

**AFTER:**
```java
// ✅ Correct: Try.of() returns Try<String>
public Try<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("Config read failed", e));
}

// Controller handles default value
// configService.readConfig(path).getOrElse("default-config")
```

**Import updates:**
```java
// Add
import io.vavr.control.Try;
```

---

### Pattern 3: Null checks → Option chaining

**BEFORE:**
```java
// ❌ Anti-pattern: Explicit null checks
public EmployeeVO getEmployeeCity(Long id) {
    EmployeeEntity employee = employeeDao.selectById(id);
    if (employee == null) {
        throw new NotFoundException("Employee not found");
    }
    DepartmentEntity department = employee.getDepartment();
    if (department == null) {
        return new EmployeeVO(employee.getName(), "Unknown");
    }
    return new EmployeeVO(employee.getName(), department.getName());
}
```

**AFTER:**
```java
// ✅ Correct: Option chaining with flatMap
public Option<EmployeeVO> getEmployeeCity(Long id) {
    return Option.of(employeeDao.selectById(id))
        .map(employee -> new EmployeeVO(
            employee.getName(),
            Option.of(employee.getDepartment())
                .map(DepartmentEntity::getName)
                .getOrElse("Unknown")
        ));
}

// Controller handles not found case
// service.getEmployeeCity(id)
//     .getOrElseThrow(() -> new NotFoundException("Employee not found"))
```

---

### Pattern 4: MyBatis null handling

**BEFORE:**
```java
// ❌ Wrong: Doesn't handle null from MyBatis
public Option<Employee> findById(Long id) {
    return employeeDao.findById(id); // Returns Optional<Employee>
}
```

**AFTER (MyBatis returns Optional):**
```java
// ✅ Correct: Convert Optional to Option
public Option<Employee> findById(Long id) {
    return Option.ofOptional(employeeDao.findById(id));
}
```

**AFTER (MyBatis returns nullable):**
```java
// ✅ Correct: Wrap nullable in Option
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id));
}
```

---

### Pattern 5: Stream → Vavr List (Optional)

**BEFORE:**
```java
// ❌ Traditional: Java Stream API
public List<EmployeeVO> getActiveEmployees() {
    return employeeDao.selectList(
            Wrappers.<EmployeeEntity>lambdaQuery()
                .eq(EmployeeEntity::getDeleted, false)
        )
        .stream()
        .filter(e -> e.getDisabled() == false)
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class))
        .collect(Collectors.toList());
}
```

**AFTER:**
```java
// ✅ Ideal: Vavr immutable List
public io.vavr.collection.List<EmployeeVO> getActiveEmployees() {
    return io.vavr.collection.List.ofAll(
            employeeDao.selectList(
                Wrappers.<EmployeeEntity>lambdaQuery()
                    .eq(EmployeeEntity::getDeleted, false)
            )
        )
        .filter(e -> e.getDisabled() == false)
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class));
}
```

**Import updates:**
```java
// Add
import io.vavr.collection.List;
```

---

### Pattern 6: Business validation → Either

**BEFORE:**
```java
// ❌ Anti-pattern: Early returns with errors
public ResponseDTO<Employee> createEmployee(EmployeeAddForm form) {
    if (form.getEmail() == null || !EMAIL_PATTERN.matcher(form.getEmail()).matches()) {
        return ResponseDTO.userErrorParam("Invalid email format");
    }

    if (form.getPassword().length() < 8) {
        return ResponseDTO.userErrorParam("Password too short");
    }

    EmployeeEntity existing = employeeDao.getByEmail(form.getEmail());
    if (existing != null) {
        return ResponseDTO.userErrorParam("Email already exists");
    }

    try {
        Employee employee = buildEmployee(form);
        employeeDao.insert(employee);
        return ResponseDTO.ok(employee);
    } catch (Exception e) {
        log.error("Employee creation failed", e);
        return ResponseDTO.error("Creation failed");
    }
}
```

**AFTER:**
```java
// ✅ Correct: Either with flatMap chaining
public Either<String, Employee> createEmployee(EmployeeAddForm form) {
    return validateEmail(form.getEmail())
        .flatMap(email -> validatePassword(form.getPassword()))
        .flatMap(pwd -> checkEmailNotExists(form.getEmail()))
        .flatMap(email -> saveEmployee(form));
}

private Either<String, String> validateEmail(String email) {
    return email != null && EMAIL_PATTERN.matcher(email).matches()
        ? Either.right(email)
        : Either.left("Invalid email format");
}

private Either<String, String> validatePassword(String password) {
    return password.length() >= 8
        ? Either.right(password)
        : Either.left("Password too short");
}

private Either<String, String> checkEmailNotExists(String email) {
    return Option.of(employeeDao.getByEmail(email)).isEmpty()
        ? Either.right(email)
        : Either.left("Email already exists");
}

private Either<String, Employee> saveEmployee(EmployeeAddForm form) {
    return Try.of(() -> {
            Employee employee = buildEmployee(form);
            employeeDao.insert(employee);
            return employee;
        })
        .toEither()
        .mapLeft(ex -> "Creation failed: " + ex.getMessage());
}

// Controller handles Either
// service.createEmployee(form).fold(
//     error -> ResponseDTO.userErrorParam(error),
//     employee -> ResponseDTO.ok(employee)
// )
```

**Import updates:**
```java
// Add
import io.vavr.control.Either;
```

---

### Pattern 7: Multiple try-catch → Try.flatMap() chain

**BEFORE:**
```java
// ❌ Anti-pattern: Nested try-catch blocks
public Optional<Report> generateReport(Long id) {
    try {
        Data data = fetchData(id);
        try {
            String formatted = formatData(data);
            try {
                Report report = saveReport(formatted);
                return Optional.of(report);
            } catch (IOException e) {
                log.error("Save failed", e);
                return Optional.empty();
            }
        } catch (FormatException e) {
            log.error("Format failed", e);
            return Optional.empty();
        }
    } catch (DataException e) {
        log.error("Fetch failed", e);
        return Optional.empty();
    }
}
```

**AFTER:**
```java
// ✅ Correct: Try.flatMap() chains exception handling
public Option<Report> generateReport(Long id) {
    return Try.of(() -> fetchData(id))
        .flatMap(data -> Try.of(() -> formatData(data)))
        .flatMap(formatted -> Try.of(() -> saveReport(formatted)))
        .onFailure(e -> log.error("Report generation failed", e))
        .toOption();
}
```

**Key points:**
- Each operation wrapped in separate Try.of()
- .flatMap() chains them (not nested lambdas)
- Single .onFailure() logs all exception types
- .toOption() converts Try<Report> to Option<Report>

**Import updates:**
```java
// Add
import io.vavr.control.Try;
import io.vavr.control.Option;
```

---

### Pattern 8: @Transactional with Try return type

**BEFORE:**
```java
// ❌ Manager layer with checked exceptions
@Transactional(rollbackFor = Throwable.class)
public Employee updateEmployee(EmployeeUpdateForm form) throws SQLException {
    EmployeeEntity entity = employeeDao.selectById(form.getId());
    if (entity == null) {
        throw new NotFoundException("Employee not found");
    }
    entity.setName(form.getName());
    employeeDao.updateById(entity);
    return SmartBeanUtil.copy(entity, Employee.class);
}
```

**AFTER:**
```java
// ✅ Correct: Try is fully compatible with @Transactional
@Transactional(rollbackFor = Throwable.class)
public Try<Employee> updateEmployee(EmployeeUpdateForm form) {
    return Try.of(() -> {
        EmployeeEntity entity = Option.of(employeeDao.selectById(form.getId()))
            .getOrElseThrow(() -> new NotFoundException("Employee not found"));

        entity.setName(form.getName());
        employeeDao.updateById(entity);
        return SmartBeanUtil.copy(entity, Employee.class);
    });
}
```

**Transaction behavior:**
- Try.Success → Transaction commits
- Try.Failure → Transaction rolls back (exception thrown internally)
- No special handling needed

**Import updates:**
```java
// Add
import io.vavr.control.Try;
import io.vavr.control.Option;
```

---

## Refactoring Checklist

**Phase 1: Identify Anti-Patterns**
- [ ] Search for `java.util.Optional` in Service classes
- [ ] Find `try-catch` blocks in Service methods
- [ ] Locate explicit null checks (`if (obj == null)`)
- [ ] Identify Stream API that can be replaced with Vavr collections

**Phase 2: Update Imports**
- [ ] Remove `import java.util.Optional;`
- [ ] Add `import io.vavr.control.Option;`
- [ ] Add `import io.vavr.control.Try;` (if exceptions)
- [ ] Add `import io.vavr.control.Either;` (if business validation)

**Phase 3: Refactor Method Signatures**
- [ ] Change `Optional<T>` to `Option<T>`
- [ ] Change `T throws XException` to `Try<T>`
- [ ] Change validation methods to return `Either<Error, T>`

**Phase 4: Refactor Method Bodies**
- [ ] Replace `Optional.ofNullable()` with `Option.of()`
- [ ] Replace `Optional.of()` with `Option.of()`
- [ ] Replace `try-catch` with `Try.of(() -> ...)`
- [ ] Replace `if (obj == null)` with `Option.of(obj).map(...)`
- [ ] Replace nested null checks with `.flatMap()` chaining

**Phase 5: Update Controller Layer**
- [ ] Add `.getOrElse()` for default values
- [ ] Add `.getOrElseThrow()` for required values
- [ ] Add `.fold()` for Either handling
- [ ] Ensure Controller handles all Option/Try/Either cases

**Phase 6: Verify**
- [ ] Run ArchitectureTest to verify no Optional violations
- [ ] Ensure all Service methods return Option/Try/Either
- [ ] Verify business logic correctness preserved
- [ ] Check imports are correct

---

## Common Mistakes & Fixes

### Mistake 1: Forgetting to update imports

**Symptom:** Compilation error "cannot find symbol Option"

**Fix:**
```java
// Add at top of file
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;
```

---

### Mistake 2: Converting return type but not method body

**Symptom:** Returns `Optional` wrapped in `Option`

**Wrong:**
```java
public Option<Employee> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id)); // Still Optional!
}
```

**Correct:**
```java
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id));
}
```

---

### Mistake 3: Not handling MyBatis null correctly

**Symptom:** NullPointerException when DAO returns null

**Wrong:**
```java
public Option<Employee> findById(Long id) {
    Employee entity = employeeDao.selectById(id);
    return Option.of(entity); // NPE if entity is null!
}
```

**Correct:**
```java
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id)); // Option.of handles null
}
```

---

### Mistake 4: Missing .toOption() after Try

**Symptom:** Type mismatch: Try<T> cannot be converted to Option<T>

**Wrong:**
```java
public Option<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path))); // Returns Try<String>!
}
```

**Correct:**
```java
public Try<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)));
}

// Or if you really need Option:
public Option<String> readConfigSafe(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("Config read failed", e))
        .toOption(); // Converts Try<T> to Option<T>
}
```

---

### Mistake 5: Handling Option in Service instead of Controller

**Symptom:** Service returns concrete type after .getOrElse()

**Wrong:**
```java
// Service layer
public Employee findById(Long id) {
    return Option.of(employeeDao.selectById(id))
        .getOrElseThrow(() -> new NotFoundException()); // Wrong layer!
}
```

**Correct:**
```java
// Service layer - return Option
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id));
}

// Controller layer - handle Option
public ResponseDTO<EmployeeVO> getEmployee(Long id) {
    return employeeService.findById(id)
        .map(e -> SmartBeanUtil.copy(e, EmployeeVO.class))
        .fold(
            () -> ResponseDTO.userErrorParam("Employee not found"),
            employee -> ResponseDTO.ok(employee)
        );
}
```

---

### Mistake 6: Mixing .map() and .flatMap() incorrectly

**Symptom:** Type error: Option<Option<T>> instead of Option<T>

**Wrong:**
```java
public Option<String> getDepartmentName(Long employeeId) {
    return Option.of(employeeDao.selectById(employeeId))
        .map(e -> Option.of(e.getDepartment()))  // Returns Option<Option<Department>>
        .map(d -> d.getName());  // Compile error!
}
```

**Correct:**
```java
public Option<String> getDepartmentName(Long employeeId) {
    return Option.of(employeeDao.selectById(employeeId))
        .flatMap(e -> Option.of(e.getDepartment()))  // Flattens to Option<Department>
        .map(d -> d.getName());  // Returns Option<String>
}
```

**Rule:** Use `.flatMap()` when the mapping function returns Option/Try/Either

---

### Mistake 7: Multiple nested try-catch → Nested Try.of()

**Symptom:** Nested Try.of() blocks instead of flatMap chain

**Wrong:**
```java
public Try<Report> generateReport(Long id) {
    return Try.of(() -> {
        try {
            Data data = fetchData(id);
            try {
                String formatted = formatData(data);
                return saveReport(formatted);  // Still has nested try-catch!
            } catch (FormatException e) {
                throw e;
            }
        } catch (DataException e) {
            throw e;
        }
    });
}
```

**Correct:**
```java
public Option<Report> generateReport(Long id) {
    return Try.of(() -> fetchData(id))
        .flatMap(data -> Try.of(() -> formatData(data)))
        .flatMap(formatted -> Try.of(() -> saveReport(formatted)))
        .onFailure(e -> log.error("Report generation failed", e))
        .toOption();
}
```

**Rule:** Multiple try-catch blocks → Try.flatMap() chain, not nested Try.of()

---

### Mistake 8: Using Either<String, T> when custom error types are better

**Symptom:** Error messages hardcoded as strings, poor type safety

**Acceptable (simple cases):**
```java
// OK for simple validations
private Either<String, String> validateEmail(String email) {
    return isValidEmail(email)
        ? Either.right(email)
        : Either.left("Invalid email format");
}
```

**Better (complex domain logic):**
```java
// Better: Type-safe error handling
public enum EmployeeError {
    INVALID_EMAIL("Invalid email format"),
    NAME_EXISTS("Employee name already exists"),
    EMAIL_EXISTS("Email already registered");

    private final String message;
    EmployeeError(String message) { this.message = message; }
    public String getMessage() { return message; }
}

private Either<EmployeeError, String> validateEmail(String email) {
    return isValidEmail(email)
        ? Either.right(email)
        : Either.left(EmployeeError.INVALID_EMAIL);
}

// Controller handles typed errors
return service.addEmployee(form).fold(
    error -> ResponseDTO.userErrorParam(error.getMessage()),
    employee -> ResponseDTO.ok(employee)
);
```

**When to use custom error types:**
- Multiple related error cases
- Need error categorization (validation vs system errors)
- Want type-safe error handling
- Errors need metadata (error codes, i18n keys)

**Rule:** Use `Either<String, T>` for simple cases, `Either<CustomError, T>` for complex domains

---

### Mistake 9: Worrying about @Transactional compatibility with Try

**Symptom:** Keeps throws clause or unwraps Try in Manager layer

**Wrong (unnecessary worry):**
```java
@Transactional(rollbackFor = Throwable.class)
public Employee updateEmployee(EmployeeUpdateForm form) throws SQLException {
    // Keeps checked exception instead of using Try
}
```

**Correct:**
```java
@Transactional(rollbackFor = Throwable.class)
public Try<Employee> updateEmployee(EmployeeUpdateForm form) {
    return Try.of(() -> {
        // All operations here
        // Transaction rolls back on Try.Failure automatically
    });
}
```

**Reality:**
- Try.Failure still throws exception internally (triggers rollback)
- @Transactional works correctly with Try return type
- Transaction commits only on Try.Success
- No special handling needed

**Rule:** @Transactional + Try is fully compatible, no workarounds needed

---

## Migration Strategy

### Phase 1: Setup and Planning (5 minutes)
1. Verify Vavr dependency (see Prerequisites section above)
2. Identify target Service class
3. List all methods with Optional/try-catch patterns
4. Create integration test for verification (optional)

### Phase 2: Refactoring (2-5 minutes per method)
1. Update imports (remove Optional, add Option/Try/Either)
2. Change method signature (Optional → Option)
3. Refactor method body (apply patterns from this skill)
4. Update Controller layer to handle Option/Try
5. Verify compilation: `./gradlew :sa-admin:compileJava`

### Phase 3: Validation (3-5 minutes)
1. Run integration tests: `./gradlew :sa-admin:test --tests {ServiceIntegrationTest}`
2. Run ArchUnit test: `./gradlew :sa-admin:test --tests ArchitectureTest#serviceUsesVavrOption`
3. Verify business logic correctness (manually test endpoints if needed)

**Total Time Estimate**: 10-15 minutes per Service class

### Incremental Migration

**Recommended approach**: Migrate one Service class at a time
- ✅ Allows incremental testing
- ✅ Easy to rollback if issues found
- ✅ No "big bang" migration risk

**Avoid**: Refactoring all Services simultaneously
- ❌ Hard to identify source of failures
- ❌ Large PR difficult to review
- ❌ Rollback affects entire codebase

---

## Validation

**Verify refactoring correctness:**

```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest#serviceUsesVavrOption
```

**Expected output:**
```
ArchitectureTest > serviceUsesVavrOption PASSED
```

If test fails, check error message for violating classes.

---

## Reference Files

**Vavr rules:**
- `.agent/technology/functional/08-vavr-fundamentals.md` - Option/Try basics
- `.agent/technology/functional/08-vavr-advanced.md` - Either/Collections/Patterns
- `.agent/rules/technology/functional/08-vavr-mybatis-integration.md` - MyBatis + Vavr patterns

**Architecture:**
- `.agent/foundation/10-architecture-rules.md` - Service layer constraints
- `ArchitectureTest.java` - ArchUnit validation

---

## Quick Reference Table

| Anti-Pattern | Vavr Pattern | When to Use |
|--------------|-------------|-------------|
| `Optional<T>` | `Option<T>` | Service return types (MANDATORY) |
| `Optional.ofNullable(x)` | `Option.of(x)` | Wrapping nullable values |
| `Optional.of(x)` | `Option.of(x)` | Wrapping non-null values |
| `try-catch` | `Try.of(() -> ...)` | Single exception handling |
| Nested try-catch | `Try.flatMap()` chain | Multiple exception steps |
| `if (x == null)` | `Option.of(x).map(...)` | Null checks |
| Nested null checks | `.flatMap()` chaining | Chained nullable access |
| Multiple validations (strings) | `Either<String, T>.flatMap()` | Simple business rules |
| Multiple validations (typed) | `Either<CustomError, T>.flatMap()` | Complex domain logic |
| Stream API | `io.vavr.collection.List` | Immutable collections (optional) |
| `@Transactional + throws` | `@Transactional + Try<T>` | Manager layer with exceptions |

---

## RED Phase - Test Results (Baseline Without Skill)

**Scenario:** "Refactor this Service method to use Vavr Option instead of Optional"

**Test Code:**
```java
public Optional<EmployeeEntity> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));
}

// Also convert any try-catch blocks to Try.of()
```

**Observed Failures (Agent without skill):**

| Failure Type | What Happened | Rationalization |
|--------------|---------------|-----------------|
| Import not updated | Left `import java.util.Optional;` | "Forgot to remove old import" |
| Method body unchanged | Changed signature but kept `Optional.ofNullable()` | "Return type is enough" |
| Wrong null handling | Used `Option.of(null)` directly | "Didn't understand Option.of() handles null" |
| Missed chaining | Converted Optional but didn't apply `.flatMap()` | "Just did literal conversion" |
| Exception handling ignored | Forgot to convert `try-catch` to `Try.of()` | "Focused only on Optional" |
| Controller not updated | Service returns Option but Controller doesn't handle it | "Only changed Service layer" |

**Why this proves skill is needed:** Agents make systematic mistakes without explicit patterns, especially mixing up .map()/.flatMap() and forgetting to handle MyBatis null results.

---

## Rationalization Table

| Excuse | Reality | Frequency |
|--------|---------|-----------|
| "Just change the return type" | Must also change imports, method body, null handling | Very High |
| "Option.of() is same as Optional.of()" | Option.of() handles null, Optional.of() throws NPE | High |
| "Don't need to update Controller" | Controller MUST handle Option with .getOrElse()/.fold() | High |
| ".map() works for all cases" | Use .flatMap() when function returns Option/Try/Either | Very High |
| "try-catch is clearer than Try.of()" | ArchUnit enforces functional patterns in Service | Medium |
| "Conversion is optional" | ArchitectureTest FAILS if Service uses Optional | Medium |
| "Just wrap with Try.of(), keep nested try-catch" | Multiple try-catch → Try.flatMap() chain, not nested | High |
| "String errors are good enough" | Use Either<CustomError, T> for complex domain logic | Medium |
| "Try might break @Transactional" | Try.Failure triggers rollback correctly, fully compatible | Low |
| "Wrap Optional in Option.of()" | Use Option.ofOptional() to convert Optional → Option | Medium |

---

## 相關規則

本技能直接關聯以下 SmartAdmin 架構規則：

### 強制要求

- **[Architecture Rules - Service Layer Vavr Option](./../../../.agent/rules/foundation/10-architecture-rules.md#serviceusesvavroption)**
  - Service 層必須使用 `io.vavr.control.Option`（禁止 `java.util.Optional`）
  - ArchUnit 測試驗證: `serviceUsesVavrOption()`
  - 違規時本技能自動觸發重構

- **[Manager Layer Transaction Rules](./../../../.agent/rules/foundation/09-manager-layer.md)**
  - 當 Service 需要 @Transactional 時，應提取至 Manager 層
  - 本技能不處理事務重構（請使用 smartadmin-manager-extractor）
  - Manager 層同樣必須使用 Vavr Option

### 參考指引

- **[Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md)**
  - 布林欄位命名: `deleted` 不是 `isDeleted`
  - 類別命名: `UserService` 不是 `UserServiceImpl`
  - Vavr 重構時確保遵循命名規範

- **[Exception and Logging Rules](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - Try.Failure 應記錄到日誌
  - Either.Left 錯誤應包含完整堆棧跟蹤
  - 使用 Vavr 後異常處理模式的最佳實踐

---

## 參考資料

- [Vavr Option 官方文檔](https://docs.vavr.io/#_option)
- [Vavr Try 官方文檔](https://docs.vavr.io/#_try)
- [Vavr Either 官方文檔](https://docs.vavr.io/#_either)
- [SmartAdmin Patterns - Domain Objects](./../../../.claude/shared/knowledge/smartadmin-patterns.md#domain-object-pattern)

---

**Last Updated:** 2026-02-03 (v1.2 - Added cross-references to .agent/rules)
