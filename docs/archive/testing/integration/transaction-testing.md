# Transaction Testing Guide

> **Challenge #2**: Testing @Transactional Manager methods and rollback scenarios
> **Last Updated**: 2026-01-22

---

## Table of Contents

1. [Introduction](#introduction)
2. [SmartAdmin Transaction Architecture](#smartadmin-transaction-architecture)
3. [Testing Successful Commits](#testing-successful-commits)
4. [Testing Rollback Scenarios](#testing-rollback-scenarios)
5. [Real SmartAdmin Examples](#real-smartadmin-examples)
6. [Advanced Patterns](#advanced-patterns)
7. [Common Issues](#common-issues)

---

## Introduction

SmartAdmin follows a strict architectural pattern:
- **@Transactional ONLY in Manager layer** (not Service)
- **rollbackFor = Throwable.class** (not Exception.class)
- **Manager layer coordinates multiple DAO operations**

This guide shows how to test transactional behavior correctly.

---

## SmartAdmin Transaction Architecture

### The Manager Layer Pattern

```
Controller → Service → Manager → Dao
                         ↓
                  @Transactional
```

**EmployeeManager Example:**

```java
@Service
public class EmployeeManager extends ServiceImpl<EmployeeDao, EmployeeEntity> {

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
        // Save employee (INSERT)
        employeeDao.insert(employee);

        // Save employee-role relationships (multiple INSERTs)
        if (CollectionUtils.isNotEmpty(roleIdList)) {
            roleIdList.forEach(roleId ->
                roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getEmployeeId())));
        }
    }
}
```

**What to Test:**
1. ✅ Both employee AND roles are saved (commit scenario)
2. ✅ If role insert fails, employee is NOT saved (rollback scenario)
3. ✅ Operations execute in correct order

---

## Testing Successful Commits

### Pattern 1: Verify Multi-DAO Operations

Test that ALL DAO operations in a transaction complete successfully.

```java
@SpringBootTest
@Transactional  // Auto-rollback after each test
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("saveEmployee - success - commits employee and roles")
    void testSaveEmployee_Success_CommitsBothTables() {
        // Given
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("test_employee_" + System.currentTimeMillis());
        employee.setActualName("Test User");
        employee.setPhone("13800138000");
        employee.setDepartmentId(1L);
        employee.setDisabledFlag(false);
        employee.setDeletedFlag(false);
        employee.setAdministratorFlag(false);

        List<Long> roleIds = List.of(1L, 2L);

        // When
        employeeManager.saveEmployee(employee, roleIds);

        // Then - verify employee saved
        assertNotNull(employee.getEmployeeId(), "Employee ID should be set after insert");

        EmployeeEntity savedEmployee = employeeDao.selectById(employee.getEmployeeId());
        assertNotNull(savedEmployee);
        assertEquals("Test User", savedEmployee.getActualName());

        // Verify roles saved
        List<RoleEmployeeEntity> savedRoles =
            roleEmployeeDao.selectList(Wrappers.<RoleEmployeeEntity>lambdaQuery()
                .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

        assertEquals(2, savedRoles.size(), "Should save 2 role assignments");

        List<Long> savedRoleIds = savedRoles.stream()
            .map(RoleEmployeeEntity::getRoleId)
            .collect(Collectors.toList());
        assertTrue(savedRoleIds.containsAll(List.of(1L, 2L)));
    }
}
```

### Pattern 2: Verify Operation Sequence

Test that DAO operations execute in the correct order.

```java
@SpringBootTest
@Transactional
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @SpyBean  // Use Spy to verify method calls
    private EmployeeDao employeeDao;

    @SpyBean
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("updateEmployeeRole - deletes old roles then inserts new ones")
    void testUpdateEmployeeRole_DeletesThenInserts() {
        // Given
        Long employeeId = 1L;
        List<RoleEmployeeEntity> newRoles = List.of(
            new RoleEmployeeEntity(3L, employeeId),
            new RoleEmployeeEntity(4L, employeeId)
        );

        // When
        employeeManager.updateEmployeeRole(employeeId, newRoles);

        // Then - verify delete called BEFORE insert
        InOrder inOrder = inOrder(roleEmployeeDao);
        inOrder.verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
        inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }
}
```

---

## Testing Rollback Scenarios

### Pattern 1: Simulate Exception in Second DAO Call

Test that if the second operation fails, the first is rolled back.

```java
@SpringBootTest
@Transactional
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @SpyBean  // Use SpyBean to partially mock
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("saveEmployee - role insert fails - rollbacks employee")
    void testSaveEmployee_RoleInsertFails_RollbacksEmployee() {
        // Given
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("rollback_test_" + System.currentTimeMillis());
        employee.setActualName("Rollback Test");
        employee.setPhone("13800138001");
        employee.setDepartmentId(1L);
        employee.setDisabledFlag(false);
        employee.setDeletedFlag(false);
        employee.setAdministratorFlag(false);

        List<Long> roleIds = List.of(1L);

        // Mock roleEmployeeDao to throw exception on insert
        doThrow(new RuntimeException("Simulated database constraint violation"))
            .when(roleEmployeeDao).insert(any(RoleEmployeeEntity.class));

        // When/Then - expect exception
        assertThrows(RuntimeException.class, () -> {
            employeeManager.saveEmployee(employee, roleIds);
        });

        // Then - verify employee NOT saved due to rollback
        // Note: employee.getEmployeeId() may be set (from insert attempt)
        // but the database should NOT contain the record
        if (employee.getEmployeeId() != null) {
            EmployeeEntity result = employeeDao.selectById(employee.getEmployeeId());
            assertNull(result, "Employee should be rolled back and not exist in database");
        }

        // Verify by login name as well
        EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
        assertNull(result, "Employee should not exist in database after rollback");
    }
}
```

### Pattern 2: Test rollbackFor = Throwable.class

Verify that ALL throwables (including Error) trigger rollback.

```java
@Test
@DisplayName("saveEmployee - throws Error - still rollbacks")
void testSaveEmployee_Error_Rollbacks() {
    // Given
    EmployeeEntity employee = new EmployeeEntity();
    employee.setLoginName("error_test");
    employee.setActualName("Error Test");
    employee.setDepartmentId(1L);

    // Mock to throw Error (not Exception)
    doThrow(new OutOfMemoryError("Simulated OOM"))
        .when(roleEmployeeDao).insert(any());

    // When/Then - Error should still trigger rollback
    assertThrows(OutOfMemoryError.class, () -> {
        employeeManager.saveEmployee(employee, List.of(1L));
    });

    // Verify rollback
    EmployeeEntity result = employeeDao.selectByLoginName("error_test");
    assertNull(result, "Transaction should rollback even for Error");
}
```

### Pattern 3: Test Without Transaction (Anti-Pattern)

Demonstrate why @Transactional is needed.

```java
@Test
@DisplayName("Without @Transactional - partial commit occurs (anti-pattern)")
void demonstrateWithoutTransaction_PartialCommit() {
    // This test shows what WOULD happen without @Transactional
    // Note: EmployeeManager DOES have @Transactional, so this is hypothetical

    // Given
    EmployeeEntity employee = new EmployeeEntity();
    employee.setLoginName("partial_commit_test");

    // Imagine saveEmployee WITHOUT @Transactional:
    // employeeDao.insert(employee);  // ✅ Commits immediately
    // roleEmployeeDao.insert(role);   // ❌ Fails
    // Result: Employee saved, roles NOT saved = data inconsistency!

    // SmartAdmin prevents this by requiring @Transactional in Manager
}
```

---

## Real SmartAdmin Examples

### Example 1: EmployeeManager.saveEmployee()

**Real Implementation:**

```java
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    // Save employee first
    employeeDao.insert(employee);

    // Then save roles
    if (CollectionUtils.isNotEmpty(roleIdList)) {
        roleIdList.forEach(roleId ->
            roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getEmployeeId())));
    }
}
```

**Test Coverage:**

```java
@SpringBootTest
@Transactional
class EmployeeManagerSaveEmployeeTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private RoleEmployeeDao roleEmployeeDao;

    @SpyBean
    private RoleEmployeeDao roleEmployeeDaoSpy;

    @Nested
    @DisplayName("saveEmployee Method")
    class SaveEmployeeTests {

        @Test
        @DisplayName("Valid employee with roles - saves both")
        void testSaveEmployee_ValidEmployeeWithRoles_SavesBoth() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L, 2L, 3L);

            // When
            employeeManager.saveEmployee(employee, roleIds);

            // Then
            assertNotNull(employee.getEmployeeId());

            // Verify employee in DB
            EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
            assertNotNull(saved);
            assertEquals(employee.getLoginName(), saved.getLoginName());

            // Verify roles in DB
            List<RoleEmployeeEntity> savedRoles =
                roleEmployeeDao.selectList(Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(3, savedRoles.size());
        }

        @Test
        @DisplayName("Valid employee with empty roles - saves employee only")
        void testSaveEmployee_EmptyRoles_SavesEmployeeOnly() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> emptyRoles = List.of();

            // When
            employeeManager.saveEmployee(employee, emptyRoles);

            // Then
            assertNotNull(employee.getEmployeeId());
            EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
            assertNotNull(saved);

            // Verify no roles
            List<RoleEmployeeEntity> roles =
                roleEmployeeDao.selectList(Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(0, roles.size());
        }

        @Test
        @DisplayName("Role insert fails - rollbacks employee insert")
        void testSaveEmployee_RoleFails_RollbacksAll() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L);

            // Simulate role insert failure
            doThrow(new RuntimeException("Foreign key violation"))
                .when(roleEmployeeDaoSpy).insert(any(RoleEmployeeEntity.class));

            // When/Then
            assertThrows(RuntimeException.class, () -> {
                employeeManager.saveEmployee(employee, roleIds);
            });

            // Verify rollback
            EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
            assertNull(result, "Employee should not exist after rollback");
        }
    }

    private EmployeeEntity createTestEmployee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("test_" + System.currentTimeMillis());
        employee.setActualName("Test User");
        employee.setPhone("13800138000");
        employee.setDepartmentId(1L);
        employee.setDisabledFlag(false);
        employee.setDeletedFlag(false);
        employee.setAdministratorFlag(false);
        return employee;
    }
}
```

### Example 2: EmployeeManager.updateEmployee()

**Real Implementation:**

```java
@Transactional(rollbackFor = Throwable.class)
public void updateEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    // Update employee
    employeeDao.updateById(employee);

    // If roles empty, delete all
    if (CollectionUtils.isEmpty(roleIdList)) {
        roleEmployeeDao.deleteByEmployeeId(employee.getEmployeeId());
        return;
    }

    // Otherwise, update roles (delete + insert)
    List<RoleEmployeeEntity> roleEmployeeList = roleIdList.stream()
        .map(e -> new RoleEmployeeEntity(e, employee.getEmployeeId()))
        .collect(Collectors.toList());

    this.updateEmployeeRole(employee.getEmployeeId(), roleEmployeeList);
}
```

**Test:**

```java
@Test
@DisplayName("updateEmployee - updates employee and replaces roles")
void testUpdateEmployee_ReplacesRoles() {
    // Given - employee with existing roles
    EmployeeEntity employee = employeeDao.selectById(1L);
    String newName = "Updated Name";
    employee.setActualName(newName);

    List<Long> newRoleIds = List.of(5L, 6L);  // Different roles

    // When
    employeeManager.updateEmployee(employee, newRoleIds);

    // Then - employee updated
    EmployeeEntity updated = employeeDao.selectById(1L);
    assertEquals(newName, updated.getActualName());

    // Roles replaced
    List<RoleEmployeeEntity> roles =
        roleEmployeeDao.selectList(Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getEmployeeId, 1L));

    assertEquals(2, roles.size());
    assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(5L)));
    assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(6L)));
}
```

### Example 3: EmployeeManager.updateEmployeeRole()

**Real Implementation:**

```java
@Transactional(rollbackFor = Throwable.class)
public void updateEmployeeRole(Long employeeId, List<RoleEmployeeEntity> roleEmployeeList) {
    // Delete existing roles
    roleEmployeeDao.deleteByEmployeeId(employeeId);

    // Insert new roles
    if (CollectionUtils.isNotEmpty(roleEmployeeList)) {
        roleEmployeeList.forEach(roleEmployeeDao::insert);
    }
}
```

**Test:**

```java
@Test
@DisplayName("updateEmployeeRole - delete fails - no changes committed")
void testUpdateEmployeeRole_DeleteFails_NoChanges() {
    // Given
    Long employeeId = 1L;

    // Get current role count
    long initialCount = roleEmployeeDao.selectCount(
        Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getEmployeeId, employeeId));

    List<RoleEmployeeEntity> newRoles = List.of(
        new RoleEmployeeEntity(99L, employeeId)
    );

    // Simulate delete failure
    doThrow(new RuntimeException("Delete failed"))
        .when(roleEmployeeDaoSpy).deleteByEmployeeId(employeeId);

    // When/Then
    assertThrows(RuntimeException.class, () -> {
        employeeManager.updateEmployeeRole(employeeId, newRoles);
    });

    // Verify no changes
    long finalCount = roleEmployeeDao.selectCount(
        Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getEmployeeId, employeeId));

    assertEquals(initialCount, finalCount, "Role count should be unchanged");
}
```

---

## Advanced Patterns

### Pattern 1: Testing Transaction Propagation

```java
@Service
public class OuterService {
    @Resource
    private InnerService innerService;

    @Transactional(rollbackFor = Throwable.class)
    public void outerMethod() {
        // Outer transaction
        employeeDao.insert(employee);

        // Calls inner transaction
        innerService.innerMethod();  // REQUIRES_NEW propagation?
    }
}

@Test
@DisplayName("Nested transactions - inner commits independently")
void testNestedTransactions() {
    // Test transaction propagation behavior
    // SmartAdmin typically doesn't use nested transactions
    // but this shows how to test if needed
}
```

### Pattern 2: Testing Transaction Isolation Levels

```java
@Test
@DisplayName("Read committed isolation - can read committed data")
void testReadCommittedIsolation() throws Exception {
    // Test concurrent access patterns
    // Usually not needed for SmartAdmin's use cases
}
```

### Pattern 3: Testing Long-Running Transactions

```java
@Test
@DisplayName("Long transaction - no timeout error")
void testLongTransaction() {
    // Verify transaction doesn't timeout
    // Useful for batch operations
}
```

---

## Common Issues

### Issue 1: Test @Transactional Interferes with Manager @Transactional

**Problem**: Test class has `@Transactional` which auto-rolls back, making it hard to verify commits.

**Solution**: Remove `@Transactional` from test class when testing commits:

```java
// ❌ Problem - auto-rollback prevents verification
@SpringBootTest
@Transactional  // This rolls back everything!
class EmployeeManagerTest {
    @Test
    void testSaveEmployee_Commits() {
        employeeManager.saveEmployee(employee, roles);
        // Can't verify in DB - test transaction will rollback!
    }
}

// ✅ Solution - remove @Transactional from class
@SpringBootTest
class EmployeeManagerTest {
    @Autowired
    private EmployeeDao employeeDao;

    @Test
    void testSaveEmployee_Commits() {
        employeeManager.saveEmployee(employee, roles);

        // Now we can verify
        EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
        assertNotNull(saved);
    }

    @AfterEach
    void cleanup() {
        // Manually clean up test data
        employeeDao.deleteById(employee.getEmployeeId());
    }
}
```

### Issue 2: SpyBean Doesn't Work with @Transactional

**Problem**: Spy creates proxy, transaction manager creates another proxy - conflicts.

**Solution**: Use `@SpyBean` at class level, not field level:

```java
@SpringBootTest
@Transactional
class EmployeeManagerTest {

    @SpyBean  // ✅ Correct - class level
    private RoleEmployeeDao roleEmployeeDao;

    // Tests...
}
```

### Issue 3: Can't Verify Rollback

**Problem**: How to verify data was rolled back?

**Solution**: Check database state after exception:

```java
@Test
void testRollback() {
    // Get initial state
    Long initialCount = employeeDao.selectCount(null);

    // Trigger rollback
    assertThrows(Exception.class, () -> {
        employeeManager.saveEmployee(employee, roles);
    });

    // Verify no change
    Long finalCount = employeeDao.selectCount(null);
    assertEquals(initialCount, finalCount, "Count should be unchanged");

    // Or verify specific record doesn't exist
    EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
    assertNull(result);
}
```

### Issue 4: Testing Requires Real Database

**Problem**: Transaction tests need real database behavior (H2 doesn't fully match PostgreSQL).

**Solution**: Use Testcontainers with real PostgreSQL:

```java
@SpringBootTest
@Testcontainers
class EmployeeManagerTransactionTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Tests with real PostgreSQL transaction behavior
}
```

---

## Best Practices

### 1. Test Both Success and Failure Paths

```java
@Nested
@DisplayName("saveEmployee")
class SaveEmployeeTests {

    @Test
    @DisplayName("Success - commits all")
    void testSuccess() { }

    @Test
    @DisplayName("Failure - rollbacks all")
    void testFailure() { }
}
```

### 2. Verify Order of Operations

```java
@Test
void testOperationOrder() {
    InOrder inOrder = inOrder(dao1, dao2, dao3);
    inOrder.verify(dao1).operation1();
    inOrder.verify(dao2).operation2();
    inOrder.verify(dao3).operation3();
}
```

### 3. Use Descriptive Test Names

```java
// ✅ Good
@Test
@DisplayName("saveEmployee - role insert fails - rollbacks employee insert")
void testSaveEmployee_RoleInsertFails_RollbacksEmployee() { }

// ❌ Poor
@Test
void test1() { }
```

### 4. Clean Up Test Data

```java
@AfterEach
void cleanup() {
    // Clean up in reverse order (due to foreign keys)
    roleEmployeeDao.deleteByEmployeeId(testEmployeeId);
    employeeDao.deleteById(testEmployeeId);
}
```

---

## Summary

| Scenario | Pattern | Verification |
|----------|---------|--------------|
| **Successful commit** | Call method, verify all DAOs | Check all tables have data |
| **Rollback on exception** | Mock DAO to throw, call method | Verify no data committed |
| **Operation order** | Use `InOrder` | Verify sequence of DAO calls |
| **Multi-step transaction** | Test all steps | Verify all-or-nothing behavior |

**Key Architecture Rule**: `@Transactional(rollbackFor = Throwable.class)` ONLY in Manager layer!

---

## Related Documentation

- [Integration Testing Quick Reference](../integration-testing-quick-reference.md) - Quick transaction patterns
- [Architecture Overview](../architecture/overview.md) - Manager layer rules
- [Manager Layer Rules](../../../.agent/rules/09-manager-layer.md) - Detailed constraints

---

**Happy Testing! 🧪**
