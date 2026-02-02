# ArchUnit Test Examples - SmartAdmin Real Cases

## Example 1: Manager Cannot Call Service

**Rule**: Manager layer禁止調用業務 Service 層

**Test Code**:
```java
@ArchTest
static final ArchRule managerShouldNotAccessBusinessService =
    noClasses()
        .that().resideInAPackage("..manager..")
        .should().dependOnClassesThat()
        .resideInAPackage("net.lab1024.sa.admin..service..")
        .because("Manager 層禁止調用業務 Service 層 (rule: 09-manager-layer.md)");
```

**Violation Example**:
```java
// ❌ WRONG - Manager calling Service
@Service
public class EmployeeManager {
    @Autowired
    private DepartmentService departmentService;  // VIOLATION!
}
```

**Correct Implementation**:
```java
// ✅ CORRECT - Manager calling Dao
@Service
public class EmployeeManager {
    @Autowired
    private DepartmentDao departmentDao;  // OK
}
```

---

## Example 2: @Transactional Only in Manager

**Rule**: @Transactional must only be used in Manager layer

**Test Code**:
```java
@ArchTest
static final ArchRule transactionalOnlyInManager =
    methods()
        .that().areAnnotatedWith(Transactional.class)
        .should().beDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Manager")
        .because("@Transactional must only be used in Manager layer (rule: 09-manager-layer.md)");
```

**Violation Example**:
```java
// ❌ WRONG - @Transactional in Service
@Service
public class EmployeeService {
    @Transactional
    public void updateEmployee(Long id, String name) {  // VIOLATION!
        // ...
    }
}
```

**Correct Implementation**:
```java
// ✅ CORRECT - @Transactional in Manager
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(Long id, String name) {  // OK
        // ...
    }
}
```

---

## Example 3: No Field Injection

**Rule**: Use constructor injection instead of @Autowired field injection

**Test Code**:
```java
@ArchTest
static final ArchRule noFieldInjection =
    fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..controller..", "..service..", "..manager..")
        .should().notBeAnnotatedWith(Autowired.class)
        .because("Use constructor injection with @RequiredArgsConstructor");
```

**Violation Example**:
```java
// ❌ WRONG - Field injection
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {
    @Autowired  // VIOLATION!
    private EmployeeService employeeService;
}
```

**Correct Implementation**:
```java
// ✅ CORRECT - Constructor injection
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;  // OK
}
```

---

## Running These Tests

```bash
# Compile
./gradlew :sa-admin:compileTestJava

# Run all tests
./gradlew :sa-admin:test --tests ArchitectureTest

# Run specific test
./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection
```

---

See SKILL.md for 7 complete patterns with detailed examples.
