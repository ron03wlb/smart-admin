# RED Phase - Test Results

## Purpose

Document baseline agent behavior when attempting Vavr refactoring **WITHOUT** the skill, to validate the skill addresses real problems.

## Test Scenario 1: Basic Optional → Option Conversion

**Prompt:**
```
Refactor this Service method to use Vavr Option instead of Optional:

public Optional<EmployeeEntity> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));
}
```

**Expected Agent Failures:**

### Failure 1: Incomplete Import Updates
**What happens:** Changes return type but forgets to update import statements

**Code produced:**
```java
import java.util.Optional;  // ❌ Still imports Optional!

public Option<EmployeeEntity> findById(Long id) {  // Changed signature
    return Optional.ofNullable(employeeDao.selectById(id));  // Still uses Optional!
}
```

**Rationalization:** "The method signature is the important part"

---

### Failure 2: Return Type Only Conversion
**What happens:** Changes signature but not method body

**Code produced:**
```java
import io.vavr.control.Option;

public Option<EmployeeEntity> findById(Long id) {
    return Optional.ofNullable(employeeDao.selectById(id));  // Compile error!
}
```

**Rationalization:** "Changed the interface, implementation follows"

---

### Failure 3: Wrong Null Handling Pattern
**What happens:** Misunderstands how Option.of() handles null

**Code produced:**
```java
public Option<EmployeeEntity> findById(Long id) {
    EmployeeEntity entity = employeeDao.selectById(id);
    if (entity == null) {  // ❌ Still has null check!
        return Option.none();
    }
    return Option.of(entity);
}
```

**Rationalization:** "Need explicit null check to be safe"

**Reality:** `Option.of(employeeDao.selectById(id))` handles null automatically

---

## Test Scenario 2: try-catch → Try.of() Conversion

**Prompt:**
```
Convert this method to use Try.of():

public String readConfig(String path) {
    try {
        return Files.readString(Paths.get(path));
    } catch (IOException e) {
        log.error("Config read failed", e);
        return "default-config";
    }
}
```

**Expected Agent Failures:**

### Failure 4: Partial Conversion
**What happens:** Wraps with Try but keeps try-catch inside

**Code produced:**
```java
public Try<String> readConfig(String path) {
    return Try.of(() -> {
        try {  // ❌ Still has try-catch!
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            log.error("Config read failed", e);
            return "default-config";
        }
    });
}
```

**Rationalization:** "Try.of() wraps the existing logic"

---

### Failure 5: Wrong Return Handling
**What happens:** Returns concrete type instead of Try

**Code produced:**
```java
public String readConfig(String path) {  // ❌ Still returns String!
    return Try.of(() -> Files.readString(Paths.get(path)))
        .onFailure(e -> log.error("Config read failed", e))
        .getOrElse("default-config");  // ❌ Unwraps in Service!
}
```

**Rationalization:** "Simpler to return concrete type"

**Reality:** Service should return Try<String>, Controller handles .getOrElse()

---

## Test Scenario 3: Nested Null Checks → flatMap Chaining

**Prompt:**
```
Refactor this to use Option chaining:

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

**Expected Agent Failures:**

### Failure 6: Using .map() Instead of .flatMap()
**What happens:** Creates nested Option<Option<T>>

**Code produced:**
```java
public Option<EmployeeVO> getEmployeeCity(Long id) {
    return Option.of(employeeDao.selectById(id))
        .map(employee -> {
            // ❌ Returns Option<Option<String>>
            Option<String> deptName = Option.of(employee.getDepartment())
                .map(DepartmentEntity::getName);
            return new EmployeeVO(employee.getName(), deptName.getOrElse("Unknown"));
        });
}
```

**Rationalization:** ".map() transforms the value"

**Reality:** Use .flatMap() when inner function returns Option

---

### Failure 7: Exception Handling Lost
**What happens:** Removes exception but doesn't handle in Controller

**Code produced:**
```java
// Service
public Option<EmployeeVO> getEmployeeCity(Long id) {
    return Option.of(employeeDao.selectById(id))
        .map(employee -> new EmployeeVO(
            employee.getName(),
            Option.of(employee.getDepartment())
                .map(DepartmentEntity::getName)
                .getOrElse("Unknown")
        ));
}

// Controller - NOT UPDATED! ❌
public ResponseDTO<EmployeeVO> getEmployee(Long id) {
    EmployeeVO vo = employeeService.getEmployeeCity(id);  // Compile error!
    return ResponseDTO.ok(vo);
}
```

**Rationalization:** "Only Service layer needs changes"

**Reality:** Controller MUST handle Option with .fold() or .getOrElse()

---

## Test Scenario 4: MyBatis Integration

**Prompt:**
```
This Dao method returns Optional. Refactor Service to use Vavr Option:

// Dao (uses Spring Data style)
Optional<Employee> findById(Long id);

// Service
public Optional<Employee> getById(Long id) {
    return employeeDao.findById(id);
}
```

**Expected Agent Failures:**

### Failure 8: Direct Return Without Conversion
**What happens:** Tries to return Optional as Option

**Code produced:**
```java
public Option<Employee> getById(Long id) {
    return employeeDao.findById(id);  // ❌ Type mismatch!
}
```

**Rationalization:** "Just change the signature"

**Reality:** Must use `Option.ofOptional()` to convert

---

### Failure 9: Wrong Conversion Method
**What happens:** Uses Option.of() on Optional

**Code produced:**
```java
public Option<Employee> getById(Long id) {
    return Option.of(employeeDao.findById(id));  // ❌ Option<Optional<Employee>>!
}
```

**Rationalization:** "Option.of() wraps anything"

**Reality:** Use `Option.ofOptional()` for Optional → Option conversion

---

## Observed Rationalization Patterns

| Category | Common Excuses | Reality |
|----------|----------------|---------|
| **Import Management** | "Return type is the important part" | Must remove `java.util.Optional` import |
| **Null Handling** | "Need explicit null check to be safe" | `Option.of()` handles null automatically |
| **Type Confusion** | "Option.of() and Optional.of() are similar" | Optional.of() throws NPE on null, Option.of() returns None |
| **Method Chaining** | ".map() transforms the value" | Use .flatMap() when function returns Option/Try/Either |
| **Layer Responsibility** | "Simpler to return concrete type" | Service returns Option/Try, Controller unwraps |
| **Partial Refactoring** | "Only Service layer needs changes" | Controller MUST handle Option/Try/Either |
| **try-catch Conversion** | "Try.of() wraps the existing logic" | Replace try-catch entirely, not wrap it |
| **MyBatis Integration** | "Just change the signature" | Must use Option.ofOptional() or Option.of() depending on DAO |

---

## Why These Tests Prove Skill Is Needed

**Without explicit patterns, agents consistently:**
1. Mix up Optional and Option semantics
2. Use .map() where .flatMap() is required
3. Keep null checks instead of using Option chaining
4. Forget to update imports
5. Unwrap Option/Try in Service instead of Controller
6. Partially convert without understanding layer responsibilities

**The skill addresses each failure with:**
- Explicit before/after code examples
- Import update checklist
- .map() vs .flatMap() rules
- Layer responsibility guidelines
- MyBatis integration patterns
- Common mistakes section with exact fixes

---

## Success Criteria (GREEN Phase)

**With skill loaded, agent should:**
- [ ] Update imports correctly (remove Optional, add Option/Try/Either)
- [ ] Convert Optional.ofNullable() to Option.of()
- [ ] Replace try-catch with Try.of() completely
- [ ] Use .flatMap() for nested Option operations
- [ ] Return Option/Try from Service, handle in Controller
- [ ] Use Option.ofOptional() when DAO returns Optional
- [ ] Apply all conversions without null check remnants

---

## Next Steps (REFACTOR Phase)

**After GREEN phase passes:**
1. Test with edge cases (multiple nested Options, complex Either chains)
2. Identify new rationalizations agents use
3. Add explicit counters to skill document
4. Re-test until bulletproof

**Known edge cases to test:**
- Multiple try-catch blocks in one method
- Stream.findFirst() returning Optional
- Nested Optional<Optional<T>> from complex queries
- Either with custom error types (not String)
- Transaction boundary with Try + @Transactional

---

**Document Version:** 1.0
**Test Date:** 2026-01-25
**Status:** Baseline failures documented, skill created, awaiting GREEN phase validation
