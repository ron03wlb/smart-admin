# Testing Quick Reference Card

> **Version**: 1.0 | **Last Updated**: 2026-01-21
> **Print-friendly single-page cheatsheet**

---

## Architecture Rules (Cheatsheet)

| Rule | Correct ✅ | Wrong ❌ |
|------|-----------|---------|
| **Transactions** | `@Transactional` in Manager | `@Transactional` in Service |
| **rollbackFor** | `Throwable.class` | `Exception.class` |
| **Injection** | Constructor injection with `@RequiredArgsConstructor` | `@Autowired` field injection |
| **Layer calls** | Controller → Service → Manager → Dao | Controller → Dao directly |

**Layer Architecture**:
```
Controller -> Service -> Manager -> Dao -> Entity
```

---

## Commands

### Run Tests

```bash
# All tests
./gradlew :sa-admin:test

# Specific test class
./gradlew :sa-admin:test --tests SecurityLoginServiceTest

# Test method pattern
./gradlew :sa-admin:test --tests "*Login*"

# Single test method
./gradlew :sa-admin:test --tests "LoginServiceTest.testLogin_ValidCredentials_ReturnsToken"

# Architecture validation (MUST pass before writing tests)
./gradlew :sa-admin:test --tests ArchitectureTest
```

### Run Integration Tests

```bash
# Integration tests with Testcontainers
./gradlew :sa-admin:test --tests *IntTest

# Specific integration test
./gradlew :sa-admin:test --tests EmployeeControllerIntTest

# With container reuse (faster)
./gradlew :sa-admin:test -Dtestcontainers.reuse.enable=true

# See: Integration Testing Quick Reference for more
```

### Coverage Reports

```bash
# Generate JaCoCo report
./gradlew :sa-admin:jacocoTestReport

# View HTML report
open sa-admin/build/reports/jacoco/test/html/index.html

# Verify coverage thresholds
./gradlew :sa-admin:jacocoTestCoverageVerification

# Full quality check (tests + coverage + architecture)
./gradlew :sa-admin:check
```

### Detect Violations

```bash
# Find @Transactional in Service classes
grep -r "@Transactional" --include="*Service.java" sa-admin/src/main/java

# Find wrong rollbackFor usage
grep -r "rollbackFor = Exception.class" --include="*.java" sa-admin/src/main/java

# Find @Autowired field injection
grep -r "@Autowired" --include="*.java" sa-admin/src/main/java | grep -v "constructor"
```

---

## Mock Strategy Decision Tree

```
What am I testing?
│
├─ Service Layer
│  └─ Use: @Mock (Mockito) - Fast ~10ms
│     @ExtendWith(MockitoExtension.class)
│     @InjectMocks private MyService service;
│     @Mock private MyDao dao;
│
├─ Manager Layer
│  └─ Use: @Mock (Mockito) - Fast ~10ms
│     Verify DAO call sequences with InOrder
│
├─ Controller Layer
│  └─ Use: @WebMvcTest + @MockBean - Medium ~500ms
│     @WebMvcTest(MyController.class)
│     @MockBean private MyService service;
│
├─ SA-Token Static Methods
│  └─ Use: MockedStatic<StpUtil>
│     try (MockedStatic<StpUtil> mock = mockStatic(StpUtil.class)) {
│         mock.when(StpUtil::getTokenValue).thenReturn("token");
│     }
│
└─ Cache Operations
   └─ Mock Manager layer (don't test JetCache directly)
```

---

## Test Naming Convention

**Pattern**: `test{Method}_{Scenario}_{ExpectedResult}`

**Examples**:
```java
✅ testLogin_ValidCredentials_ReturnsToken
✅ testAddEmployee_DuplicateLoginName_ReturnsError
✅ testCheckLogin_AccountLocked_ReturnsLockMessage
✅ testUpdatePassword_WrongOldPassword_ReturnsError

❌ testLogin1
❌ testEmployeeService
❌ test_add_employee
```

**DisplayName** (optional, for readability):
```java
@Test
@DisplayName("Valid credentials should return token and user info")
void testLogin_ValidCredentials_ReturnsToken() { }
```

---

## Coverage Targets

| Layer | Line Coverage | Branch Coverage |
|-------|---------------|-----------------|
| **Service** | ≥ 85% | ≥ 75% |
| **Manager** | ≥ 80% | ≥ 70% |
| **Controller** | ≥ 60% | ≥ 50% |
| **Overall** | ≥ 80% | ≥ 70% |

**Excluded from coverage**:
- DTO/VO/Form classes (data containers)
- Entity classes
- Constants/Enums

---

## Common Test Patterns

### Service Test Template

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest extends BaseServiceTest {

    @InjectMocks
    private EmployeeService employeeService;

    @Mock
    private EmployeeDao employeeDao;

    @Mock
    private EmployeeManager employeeManager;

    @Test
    void testAddEmployee_ValidForm_ReturnsSuccess() {
        // Given
        EmployeeAddForm form = EmployeeFixture.defaultAddForm();
        when(employeeDao.selectByLoginName(anyString())).thenReturn(null);

        // When
        ResponseDTO<Long> result = employeeService.addEmployee(form);

        // Then
        assertSuccess(result);
        verify(employeeManager).saveEmployee(any(), anyList());
    }
}
```

### Manager Test Template

```java
@ExtendWith(MockitoExtension.class)
class EmployeeManagerTest extends BaseManagerTest {

    @InjectMocks
    private EmployeeManager employeeManager;

    @Mock
    private EmployeeDao employeeDao;

    @Mock
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    void testSaveEmployee_InsertsEmployeeAndRoles() {
        // Given
        EmployeeEntity employee = EmployeeFixture.defaultEmployee().build();
        List<Long> roleIds = List.of(1L, 2L);

        // When
        employeeManager.saveEmployee(employee, roleIds);

        // Then - verify order
        InOrder inOrder = inOrder(employeeDao, roleEmployeeDao);
        inOrder.verify(employeeDao).insert(employee);
        inOrder.verify(roleEmployeeDao, times(2)).insert(any());
    }
}
```

---

## Common Fixes

### Fix 1: @Transactional in Service

```java
// ❌ Wrong - Service layer
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> updatePassword(form) { }
}

// ✅ Correct - Manager layer
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)
    public void updatePassword(Long id, String pwd, PasswordLogEntity log) {
        employeeDao.updateById(updateEntity);
        if (log != null) passwordLogDao.insert(log);
    }
}

// ✅ Service calls Manager
@Service
public class EmployeeService {
    public ResponseDTO<String> updatePassword(form) {
        // ... validation logic ...
        employeeManager.updatePassword(id, encrypted, log);
        return ResponseDTO.ok();
    }
}
```

### Fix 2: Field Injection

```java
// ❌ Wrong
@Service
public class EmployeeService {
    @Autowired
    private EmployeeDao employeeDao;
}

// ✅ Correct
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
}
```

### Fix 3: rollbackFor

```java
// ❌ Wrong
@Transactional(rollbackFor = Exception.class)

// ✅ Correct
@Transactional(rollbackFor = Throwable.class)
```

---

## ResponseDTO Assertions

**Use BaseServiceTest helpers**:

```java
// ✅ Good - clear failure messages
assertSuccess(response);
assertError(response);
assertErrorContains(response, "already exists");
assertErrorCode(response, UserErrorCode.DATA_NOT_EXIST);

// ❌ Avoid - less informative
assertTrue(response.getOk());
assertFalse(response.getOk());
```

---

## Quick Links

| Topic | Link |
|-------|------|
| **Integration Testing** | [integration-testing-quick-reference.md](./integration-testing-quick-reference.md) |
| **Full Testing Strategy** | [testing-strategy.md](./testing-strategy.md) |
| **Implementation Roadmap** | [unit-test-implementation-plan.md](./unit-test-implementation-plan.md) |
| **Architecture Fixes** | [architecture/overview.md](./architecture/overview.md) |
| **Employee Fix Guide** | [architecture/fix-employee-transactional.md](./architecture/fix-employee-transactional.md) |
| **Role Fix Guide** | [architecture/fix-role-transactional.md](./architecture/fix-role-transactional.md) |
| **Project Conventions** | [../../CLAUDE.md](../../CLAUDE.md) |

---

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | Correct Approach |
|--------------|---------|------------------|
| `@SpringBootTest` for unit tests | Slow, 2s+ startup | `@ExtendWith(MockitoExtension.class)` |
| Random test data | Flaky tests | Use deterministic fixtures |
| `Time.now()` in assertions | Race conditions | Use fixed time references |
| Testing private methods | Fragile tests | Test through public API |
| Multiple focuses per test | Hard to diagnose | One logical assertion per test |

---

## Troubleshooting

### ArchitectureTest Fails

```bash
# 1. Identify violation
./gradlew :sa-admin:test --tests ArchitectureTest

# 2. Find violation type in output
# → "@Transactional in Service" → See architecture/fix-employee-transactional.md
# → "Wrong rollbackFor" → Change to Throwable.class

# 3. Apply fix

# 4. Verify
./gradlew :sa-admin:test --tests ArchitectureTest
```

### Coverage Below Threshold

```bash
# 1. Generate detailed report
./gradlew :sa-admin:jacocoTestReport
open sa-admin/build/reports/jacoco/test/html/index.html

# 2. Find uncovered branches (red/yellow lines)

# 3. Add test cases for uncovered scenarios

# 4. Verify
./gradlew :sa-admin:jacocoTestCoverageVerification
```

### Flaky Tests

**Common causes**:
- Time-based assertions → Use fixed time references
- Random data → Use fixture constants
- Shared mutable state → Reset in `@BeforeEach`
- Order-dependent tests → Ensure tests are independent

---

**Print this page for quick reference during development!**
