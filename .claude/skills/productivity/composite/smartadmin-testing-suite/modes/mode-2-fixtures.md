# Mode 2: Test Fixtures

**Purpose**: Generate reusable test data builders for complex domain objects with guaranteed uniqueness

**Time**: ~5 minutes per entity

**Consolidates**: test-fixture-generator skill patterns

---

## When to Use This Mode

Use Mode 2 when:
- ✅ Creating test data for integration tests
- ✅ Avoiding unique constraint violations (email, phone, loginName)
- ✅ Setting up FK dependencies in tests
- ✅ Building complex domain objects (Entity, Form, VO)
- ✅ Need reusable test data across multiple test classes
- ✅ Implementing test data isolation (unique values per test run)

**Command**:
```bash
/test Employee --mode=fixtures
```

---

## Core Pattern: Test Fixture Builder with AtomicInteger

### Test Fixture Class Structure

```java
package net.lab1024.sa.admin.module.{module}.domain;

import net.lab1024.sa.admin.module.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.{module}.domain.form.{Entity}AddForm;
import net.lab1024.sa.admin.module.{module}.domain.form.{Entity}UpdateForm;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Fixture for {Entity}
 *
 * Provides reusable test data builders with guaranteed uniqueness via AtomicInteger.
 * Use in integration tests to avoid hard-coded values and unique constraint violations.
 */
public class {Entity}TestFixture {

    /**
     * Sequential counter for generating unique values
     * Increments on each factory method call
     */
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Create {Entity}Entity with default values
     * All required fields populated with unique values
     *
     * @return new {Entity}Entity instance
     */
    public static {Entity}Entity createEntity() {
        int id = counter.incrementAndGet();

        {Entity}Entity entity = new {Entity}Entity();
        entity.setActualName("Test Employee " + id);
        entity.setLoginName("employee" + id);
        entity.setEmail("employee" + id + "@test.com");
        entity.setPhone("138" + String.format("%08d", id));  // Format: 13800000001
        entity.setGender(id % 2 == 0 ? 1 : 2);  // Alternating gender
        entity.setDepartmentId(1L);  // Default FK
        entity.setIsDisabled(false);
        entity.setIsLeave(false);
        entity.setRemark("Test employee " + id);
        entity.setAdministratorFlag(false);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    /**
     * Create {Entity}Entity with custom FK dependencies
     * Override default departmentId
     *
     * @param departmentId custom department FK
     * @return new {Entity}Entity instance
     */
    public static {Entity}Entity createEntity(Long departmentId) {
        {Entity}Entity entity = createEntity();
        entity.setDepartmentId(departmentId);
        return entity;
    }

    /**
     * Create {Entity}Entity with multiple custom FKs
     *
     * @param departmentId custom department FK
     * @param roleId custom role FK (if applicable)
     * @return new {Entity}Entity instance
     */
    public static {Entity}Entity createEntity(Long departmentId, Long roleId) {
        {Entity}Entity entity = createEntity(departmentId);
        // If entity has roleId field:
        // entity.setRoleId(roleId);
        return entity;
    }

    /**
     * Create {Entity}AddForm for Service layer tests
     * All required fields populated
     *
     * @param departmentId department FK
     * @return new {Entity}AddForm instance
     */
    public static {Entity}AddForm createAddForm(Long departmentId) {
        int id = counter.incrementAndGet();

        {Entity}AddForm form = new {Entity}AddForm();
        form.setActualName("Test Employee " + id);
        form.setLoginName("employee" + id);
        form.setPassword("password123");  // Default password
        form.setEmail("employee" + id + "@test.com");
        form.setPhone("138" + String.format("%08d", id));
        form.setGender(id % 2 == 0 ? 1 : 2);
        form.setDepartmentId(departmentId);
        form.setIsDisabled(false);
        form.setIsLeave(false);
        form.setRemark("Test employee " + id);
        form.setAdministratorFlag(false);
        return form;
    }

    /**
     * Create {Entity}AddForm with custom role
     *
     * @param departmentId department FK
     * @param roleId role FK
     * @return new {Entity}AddForm instance
     */
    public static {Entity}AddForm createAddForm(Long departmentId, Long roleId) {
        {Entity}AddForm form = createAddForm(departmentId);
        // If form has roleId field:
        // form.setRoleId(roleId);
        return form;
    }

    /**
     * Create {Entity}UpdateForm for update tests
     *
     * @param entityId entity ID to update
     * @param departmentId department FK
     * @return new {Entity}UpdateForm instance
     */
    public static {Entity}UpdateForm createUpdateForm(Long entityId, Long departmentId) {
        int id = counter.incrementAndGet();

        {Entity}UpdateForm form = new {Entity}UpdateForm();
        form.set{Entity}Id(entityId);
        form.setActualName("Updated Employee " + id);
        form.setLoginName("employee" + id);  // Unique loginName for update
        form.setEmail("updated" + id + "@test.com");
        form.setPhone("138" + String.format("%08d", id));
        form.setGender(1);
        form.setDepartmentId(departmentId);
        form.setIsDisabled(false);
        form.setIsLeave(false);
        form.setRemark("Updated employee " + id);
        return form;
    }

    /**
     * Create related entity: Department
     * Used for FK dependency setup in @BeforeEach
     *
     * @param name department name
     * @return new DepartmentEntity instance
     */
    public static DepartmentEntity createDepartment(String name) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDepartmentName(name);
        dept.setParentId(0L);  // Root department
        dept.setSort(1);
        dept.setManagerId(1L);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        return dept;
    }

    /**
     * Create related entity: Role
     * Used for FK dependency setup in @BeforeEach
     *
     * @param name role name
     * @return new RoleEntity instance
     */
    public static RoleEntity createRole(String name) {
        RoleEntity role = new RoleEntity();
        role.setRoleName(name);
        role.setRoleCode("ROLE_" + name.toUpperCase().replace(" ", "_"));
        role.setRemark("Test role: " + name);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return role;
    }

    /**
     * Reset counter for test isolation
     * Call in @BeforeEach if needed
     */
    public static void resetCounter() {
        counter.set(0);
    }

    /**
     * Get current counter value (for debugging)
     *
     * @return current counter value
     */
    public static int getCounterValue() {
        return counter.get();
    }

    /**
     * Generate unique email
     *
     * @param prefix email prefix
     * @return unique email address
     */
    public static String uniqueEmail(String prefix) {
        int id = counter.incrementAndGet();
        return prefix + id + "@test.com";
    }

    /**
     * Generate unique phone number
     *
     * @return unique phone number (format: 138XXXXXXXX)
     */
    public static String uniquePhone() {
        int id = counter.incrementAndGet();
        return "138" + String.format("%08d", id);
    }

    /**
     * Generate unique login name
     *
     * @param prefix login name prefix
     * @return unique login name
     */
    public static String uniqueLoginName(String prefix) {
        int id = counter.incrementAndGet();
        return prefix + id;
    }
}
```

---

## Key Features

### 1. AtomicInteger for Sequential IDs

```java
private static final AtomicInteger counter = new AtomicInteger(0);

public static {Entity}Entity createEntity() {
    int id = counter.incrementAndGet();  // Thread-safe increment
    entity.setLoginName("employee" + id);  // Unique: employee1, employee2, ...
}
```

**Benefits**:
- ✅ Guaranteed uniqueness across test runs
- ✅ Thread-safe (parallel test execution)
- ✅ No database sequence dependency
- ✅ Predictable values for debugging

### 2. Static Factory Methods (No Constructors)

```java
// ✅ GOOD: Static factory method
{Entity}Entity entity = {Entity}TestFixture.createEntity();

// ❌ BAD: Direct instantiation
{Entity}Entity entity = new {Entity}Entity();
entity.setLoginName("test");  // Hard-coded, not unique
```

**Benefits**:
- Centralized test data creation
- No need to instantiate fixture class
- Clear method names (createEntity, createAddForm)

### 3. Override Variants for Customization

```java
// Default FK
{Entity}Entity entity1 = {Entity}TestFixture.createEntity();
entity1.getDepartmentId();  // Returns: 1L (default)

// Custom FK
{Entity}Entity entity2 = {Entity}TestFixture.createEntity(departmentId);
entity2.getDepartmentId();  // Returns: departmentId
```

**Pattern**: Base method + Override variants

```java
// Base method (all defaults)
public static {Entity}Entity createEntity() { /* ... */ }

// Override variant 1 (custom departmentId)
public static {Entity}Entity createEntity(Long departmentId) {
    {Entity}Entity entity = createEntity();  // Reuse base
    entity.setDepartmentId(departmentId);  // Override
    return entity;
}

// Override variant 2 (multiple custom FKs)
public static {Entity}Entity createEntity(Long departmentId, Long roleId) {
    {Entity}Entity entity = createEntity(departmentId);  // Chain overrides
    entity.setRoleId(roleId);
    return entity;
}
```

### 4. Related Entity Factories for FK Setup

```java
@BeforeEach
void setUp() {
    // Create FK dependencies using fixture
    DepartmentEntity dept = {Entity}TestFixture.createDepartment("Test Dept");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();

    RoleEntity role = {Entity}TestFixture.createRole("Test Role");
    roleDao.insert(role);
    testRoleId = role.getRoleId();
}
```

**Benefits**:
- No need for separate DepartmentTestFixture class
- All related factories in one place
- Simplified test setup

### 5. Unique Value Helpers

```java
// Generate unique values on demand
String email = {Entity}TestFixture.uniqueEmail("user");  // user1@test.com
String phone = {Entity}TestFixture.uniquePhone();  // 13800000001
String loginName = {Entity}TestFixture.uniqueLoginName("emp");  // emp1
```

**Use Case**: When you need specific unique values outside of entity creation

---

## Usage Examples

### Example 1: Basic Integration Test Setup

```java
@SpringBootTest
@Transactional
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeDao employeeDao;
    @Autowired private DepartmentDao departmentDao;

    private Long testDepartmentId;

    @BeforeEach
    void setUp() {
        // Create FK dependency
        DepartmentEntity dept = EmployeeTestFixture.createDepartment("Test Dept");
        departmentDao.insert(dept);
        testDepartmentId = dept.getDepartmentId();
    }

    @Test
    void testAddEmployee() {
        // Given - Use fixture to create unique test data
        EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);

        // When
        ResponseDTO<String> response = employeeService.addEmployee(form);

        // Then
        assertTrue(response.getOk());
    }
}
```

### Example 2: Multiple Test Entities

```java
@Test
void testQueryEmployees() {
    // Given - Insert 3 unique employees
    for (int i = 0; i < 3; i++) {
        EmployeeEntity entity = EmployeeTestFixture.createEntity(testDepartmentId);
        employeeDao.insert(entity);
        // Each entity has unique: loginName, email, phone
    }

    // When - Query all
    List<EmployeeEntity> employees = employeeDao.selectList(null);

    // Then
    assertTrue(employees.size() >= 3);
}
```

### Example 3: Custom Field Values

```java
@Test
void testUpdateEmployee() {
    // Given - Create entity with custom values
    EmployeeEntity entity = EmployeeTestFixture.createEntity(testDepartmentId);
    entity.setActualName("Custom Name");  // Override after creation
    employeeDao.insert(entity);

    // When - Update
    EmployeeUpdateForm form = EmployeeTestFixture.createUpdateForm(
        entity.getEmployeeId(),
        testDepartmentId
    );
    employeeService.updateEmployee(form);

    // Then - Verify update
    EmployeeEntity updated = employeeDao.selectById(entity.getEmployeeId());
    assertNotEquals("Custom Name", updated.getActualName());
}
```

### Example 4: Counter Reset for Test Isolation

```java
@BeforeEach
void setUp() {
    // Reset counter for predictable IDs in each test
    EmployeeTestFixture.resetCounter();
    // First entity will be: employee1, employee1@test.com, 13800000001
}

@Test
void test1() {
    EmployeeEntity entity = EmployeeTestFixture.createEntity();
    assertEquals("employee1", entity.getLoginName());
}

@Test
void test2() {
    EmployeeEntity entity = EmployeeTestFixture.createEntity();
    assertEquals("employee1", entity.getLoginName());  // Same ID (counter reset)
}
```

**Note**: Counter reset is optional. Usually NOT needed because @Transactional rollback prevents duplicate data issues.

---

## Field Uniqueness Strategies

### Unique Constraint Fields

Fields with unique constraints MUST use sequential IDs:

```java
// ✅ GOOD: Sequential ID ensures uniqueness
entity.setLoginName("employee" + id);  // employee1, employee2, ...
entity.setEmail("employee" + id + "@test.com");
entity.setPhone("138" + String.format("%08d", id));  // 13800000001, 13800000002

// ❌ BAD: Hard-coded values cause constraint violations
entity.setLoginName("test");  // Fails on second insert
entity.setEmail("test@example.com");  // Fails on second insert
```

### Non-Unique Fields

Non-unique fields can use:

1. **Same value** (if uniqueness not required):
```java
entity.setDepartmentName("Test Department");  // Same for all
```

2. **Sequential values** (for better debugging):
```java
entity.setDepartmentName("Test Department " + id);
```

3. **Alternating values** (for variety):
```java
entity.setGender(id % 2 == 0 ? 1 : 2);  // Alternate: Male, Female
entity.setIsDisabled(id % 3 == 0);  // Every 3rd is disabled
```

### FK Fields

FK fields should allow override:

```java
// Default FK
entity.setDepartmentId(1L);

// Override method
public static EmployeeEntity createEntity(Long departmentId) {
    EmployeeEntity entity = createEntity();
    entity.setDepartmentId(departmentId);  // Override
    return entity;
}
```

---

## Validation Checklist

After generating test fixtures, verify:

- [ ] AtomicInteger counter declared: `private static final AtomicInteger counter = new AtomicInteger(0);`
- [ ] Static factory methods (createEntity, createAddForm, createUpdateForm)
- [ ] All required fields populated with unique values
- [ ] Unique constraint fields use sequential IDs (loginName, email, phone)
- [ ] Override variants for FK customization
- [ ] Related entity factories (createDepartment, createRole) if applicable
- [ ] Counter reset method: `resetCounter()`
- [ ] Unique value helpers (uniqueEmail, uniquePhone, uniqueLoginName)
- [ ] Can be used across multiple test classes without modification
- [ ] No hard-coded magic numbers (use descriptive constants)

---

## Troubleshooting

### Problem: "Duplicate key violation"

**Cause**: Not using sequential IDs for unique fields

**Solution**:
```java
// ❌ BAD
entity.setEmail("test@example.com");

// ✅ GOOD
int id = counter.incrementAndGet();
entity.setEmail("test" + id + "@example.com");
```

### Problem: "Counter not incrementing"

**Cause**: Forgot to call `counter.incrementAndGet()`

**Solution**:
```java
public static EmployeeEntity createEntity() {
    int id = counter.incrementAndGet();  // ← Must call first
    // ...
}
```

### Problem: "NullPointerException on FK fields"

**Cause**: FK dependency not created in @BeforeEach

**Solution**:
```java
@BeforeEach
void setUp() {
    // Create FK dependencies BEFORE test
    DepartmentEntity dept = EmployeeTestFixture.createDepartment("Test");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
}
```

### Problem: "Phone number too long"

**Cause**: Incorrect format string

**Solution**:
```java
// ❌ BAD: 138 + 9 digits = 11 digits (too long if id > 999,999,999)
entity.setPhone("138" + id);

// ✅ GOOD: 138 + 8 digits = 11 digits (max id: 99,999,999)
entity.setPhone("138" + String.format("%08d", id));
```

### Problem: "Test fixture polluting other tests"

**Cause**: Static counter shared across test classes

**Solution**:
```java
@BeforeEach
void setUp() {
    // Reset counter for test isolation (optional)
    EmployeeTestFixture.resetCounter();
}
```

**Note**: Usually NOT needed. @Transactional rollback prevents pollution.

---

## Advanced Patterns

### Pattern 1: Builder-Style Fixture (Optional)

```java
public static class EmployeeBuilder {
    private final EmployeeEntity entity;

    private EmployeeBuilder() {
        int id = counter.incrementAndGet();
        this.entity = createDefaultEntity(id);
    }

    public static EmployeeBuilder builder() {
        return new EmployeeBuilder();
    }

    public EmployeeBuilder withActualName(String name) {
        entity.setActualName(name);
        return this;
    }

    public EmployeeBuilder withDepartmentId(Long deptId) {
        entity.setDepartmentId(deptId);
        return this;
    }

    public EmployeeEntity build() {
        return entity;
    }
}

// Usage
EmployeeEntity entity = EmployeeTestFixture.builder()
    .withActualName("Custom Name")
    .withDepartmentId(deptId)
    .build();
```

### Pattern 2: Parameterized Fixture Factory

```java
public static EmployeeEntity createEntity(
    Long departmentId,
    String actualName,
    String email,
    Boolean isDisabled
) {
    EmployeeEntity entity = createEntity(departmentId);
    if (actualName != null) entity.setActualName(actualName);
    if (email != null) entity.setEmail(email);
    if (isDisabled != null) entity.setIsDisabled(isDisabled);
    return entity;
}
```

---

## Time Estimates

| Entity Complexity | Time |
|-------------------|------|
| Simple entity (5-10 fields) | ~3 minutes |
| Complex entity (10-20 fields) | ~5 minutes |
| Entity with 2+ FK dependencies | ~7 minutes |

---

## Related Documentation

- **[SKILL.md](../SKILL.md)** - Testing suite overview
- **[mode-1-integration.md](mode-1-integration.md)** - Integration test patterns
- **[SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)** - Domain object patterns

---

**Version**: 2.0.0 (Testing Suite Consolidation)
**Last Updated**: 2026-01-27
