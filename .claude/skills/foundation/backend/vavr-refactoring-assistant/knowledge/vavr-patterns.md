# Vavr Refactoring Patterns

> Extracted from SKILL.md to keep the main file focused on AI agent instructions.
> This file contains complete code examples for all 9 Vavr refactoring patterns and common mistakes.

---

## Pattern 1: Optional -> Option

**BEFORE:**
```java
// ArchUnit violation: Service uses Optional
public Optional<Employee> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));
}
```

**AFTER:**
```java
// Correct: Service uses Option
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

## Pattern 2: try-catch -> Try.of()

**BEFORE:**
```java
// Anti-pattern: try-catch in Service
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
// Correct: Try.of() returns Try<String>
public Try<String> readConfig(String path) {
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("Config read failed", e));
}

// Controller handles default value
// configService.readConfig(path).getOrElse("default-config")
```

**Import updates:**
```java
import io.vavr.control.Try;
```

---

## Pattern 3: Null checks -> Option chaining

**BEFORE:**
```java
// Anti-pattern: Explicit null checks
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
// Correct: Option chaining with flatMap
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

## Pattern 4: MyBatis null handling

**AFTER (MyBatis returns Optional):**
```java
// Correct: Convert Optional to Option
public Option<Employee> findById(Long id) {
    return Option.ofOptional(employeeDao.findById(id));
}
```

**AFTER (MyBatis returns nullable):**
```java
// Correct: Wrap nullable in Option
public Option<Employee> findById(Long id) {
    return Option.of(employeeDao.selectById(id));
}
```

---

## Pattern 5: Stream -> Vavr List (Optional)

**BEFORE:**
```java
// Traditional: Java Stream API
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
// Ideal: Vavr immutable List
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

---

## Pattern 6: Business validation -> Either

**BEFORE:**
```java
// Anti-pattern: Early returns with errors
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
// Correct: Either with flatMap chaining
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

---

## Pattern 7: Multiple try-catch -> Try.flatMap() chain

**BEFORE:**
```java
// Anti-pattern: Nested try-catch blocks
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
// Correct: Try.flatMap() chains exception handling
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

---

## Pattern 8: @Transactional with Try return type

**BEFORE:**
```java
// Manager layer with checked exceptions
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
// Correct: Try is fully compatible with @Transactional
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
- Try.Success -> Transaction commits
- Try.Failure -> Transaction rolls back (exception thrown internally)
- No special handling needed

---

## Common Mistakes & Fixes

### Mistake 1: Forgetting to update imports

**Symptom:** Compilation error "cannot find symbol Option"

**Fix:**
```java
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;
```

### Mistake 2: Converting return type but not method body

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

### Mistake 3: Not handling MyBatis null correctly

**Symptom:** NullPointerException when DAO returns null. `Option.of()` handles null safely, so use it directly.

### Mistake 4: Missing .toOption() after Try

**Wrong:** `public Option<String> readConfig(String path) { return Try.of(() -> ...); }` -- Returns Try, not Option!

**Fix:** Return `Try<String>` or add `.toOption()` at the end.

### Mistake 5: Handling Option in Service instead of Controller

**Wrong:** Service returns concrete type after `.getOrElseThrow()` -- wrong layer!

**Correct:** Service returns `Option<T>`, Controller handles with `.fold()` or `.getOrElseThrow()`.

### Mistake 6: Mixing .map() and .flatMap() incorrectly

**Symptom:** Type error: Option<Option<T>> instead of Option<T>

**Rule:** Use `.flatMap()` when the mapping function returns Option/Try/Either.

```java
// Wrong: .map() returns Option<Option<Department>>
Option.of(employee).map(e -> Option.of(e.getDepartment()));

// Correct: .flatMap() returns Option<Department>
Option.of(employee).flatMap(e -> Option.of(e.getDepartment()));
```

### Mistake 7: Nested Try.of() instead of flatMap chain

**Rule:** Multiple try-catch blocks -> Try.flatMap() chain, not nested Try.of()

### Mistake 8: String errors when custom error types are better

**Rule:** Use `Either<String, T>` for simple cases, `Either<CustomError, T>` for complex domains.

```java
public enum EmployeeError {
    INVALID_EMAIL("Invalid email format"),
    NAME_EXISTS("Employee name already exists"),
    EMAIL_EXISTS("Email already registered");
    // ...
}

private Either<EmployeeError, String> validateEmail(String email) {
    return isValidEmail(email)
        ? Either.right(email)
        : Either.left(EmployeeError.INVALID_EMAIL);
}
```

### Mistake 9: Worrying about @Transactional compatibility with Try

**Reality:** Try.Failure still throws exception internally (triggers rollback). @Transactional works correctly with Try return type. No special handling needed.

---

## RED Phase - Test Results (Baseline Without Skill)

**Scenario:** "Refactor this Service method to use Vavr Option instead of Optional"

**Test Code:**
```java
public Optional<EmployeeEntity> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));
}
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

---

**Version**: 1.0.0
**Extracted From**: SKILL.md v1.2
**Last Updated**: 2026-02-06
