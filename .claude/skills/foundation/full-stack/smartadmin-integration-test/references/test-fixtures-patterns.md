# Test Fixtures Patterns

Advanced patterns for creating reusable test data in SmartAdmin integration tests.

## Table of Contents

- [Basic Fixture Pattern](#basic-fixture-pattern)
- [Builder Pattern](#builder-pattern)
- [Unique Data Generation](#unique-data-generation)
- [Foreign Key Management](#foreign-key-management)
- [Complex Object Graphs](#complex-object-graphs)
- [Fixture Inheritance](#fixture-inheritance)

## Basic Fixture Pattern

Simple static factory methods for creating test objects.

### Entity Fixture

```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_emp");
        entity.setActualName("Test Employee");
        entity.setPhone("13800000001");
        entity.setDepartmentId(1L);
        entity.setGender(1);
        entity.setAdministratorFlag(false);
        entity.setDeletedFlag(false);
        return entity;
    }

    public static EmployeeEntity createEntity(String loginName) {
        EmployeeEntity entity = createEntity();
        entity.setLoginName(loginName);
        return entity;
    }

    public static EmployeeEntity createEntity(Long departmentId) {
        EmployeeEntity entity = createEntity();
        entity.setDepartmentId(departmentId);
        return entity;
    }
}
```

### Form Fixture

```java
public class EmployeeTestFixture {

    public static EmployeeAddForm createAddForm() {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("test_emp");
        form.setActualName("Test Employee");
        form.setPhone("13800000001");
        form.setDepartmentId(1L);
        form.setGender(1);
        return form;
    }

    public static EmployeeAddForm createAddForm(Long departmentId) {
        EmployeeAddForm form = createAddForm();
        form.setDepartmentId(departmentId);
        return form;
    }

    public static EmployeeUpdateForm createUpdateForm(Long employeeId) {
        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setEmployeeId(employeeId);
        form.setActualName("Updated Employee");
        form.setPhone("13800000002");
        form.setDepartmentId(1L);
        form.setGender(1);
        return form;
    }

    public static EmployeeQueryForm createQueryForm() {
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        return form;
    }
}
```

## Builder Pattern

Fluent API for flexible test object creation.

### Basic Builder

```java
public class EmployeeTestFixture {

    public static class Builder {
        private EmployeeEntity entity = new EmployeeEntity();

        public Builder loginName(String loginName) {
            entity.setLoginName(loginName);
            return this;
        }

        public Builder actualName(String actualName) {
            entity.setActualName(actualName);
            return this;
        }

        public Builder phone(String phone) {
            entity.setPhone(phone);
            return this;
        }

        public Builder departmentId(Long departmentId) {
            entity.setDepartmentId(departmentId);
            return this;
        }

        public Builder gender(Integer gender) {
            entity.setGender(gender);
            return this;
        }

        public Builder administrator(boolean isAdmin) {
            entity.setAdministratorFlag(isAdmin);
            return this;
        }

        public Builder deleted(boolean deleted) {
            entity.setDeletedFlag(deleted);
            return this;
        }

        public EmployeeEntity build() {
            // Set defaults if not specified
            if (entity.getLoginName() == null) {
                entity.setLoginName("test_" + System.currentTimeMillis());
            }
            if (entity.getActualName() == null) {
                entity.setActualName("Test Employee");
            }
            if (entity.getDepartmentId() == null) {
                entity.setDepartmentId(1L);
            }
            if (entity.getGender() == null) {
                entity.setGender(1);
            }
            if (entity.getAdministratorFlag() == null) {
                entity.setAdministratorFlag(false);
            }
            if (entity.getDeletedFlag() == null) {
                entity.setDeletedFlag(false);
            }
            return entity;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
```

**Usage:**
```java
EmployeeEntity employee = EmployeeTestFixture.builder()
    .loginName("john_doe")
    .actualName("John Doe")
    .departmentId(testDepartmentId)
    .administrator(true)
    .build();
```

### Form Builder

```java
public class EmployeeTestFixture {

    public static class AddFormBuilder {
        private EmployeeAddForm form = new EmployeeAddForm();

        public AddFormBuilder loginName(String loginName) {
            form.setLoginName(loginName);
            return this;
        }

        public AddFormBuilder actualName(String actualName) {
            form.setActualName(actualName);
            return this;
        }

        public AddFormBuilder departmentId(Long departmentId) {
            form.setDepartmentId(departmentId);
            return this;
        }

        public AddFormBuilder phone(String phone) {
            form.setPhone(phone);
            return this;
        }

        public AddFormBuilder roleIds(List<Long> roleIds) {
            form.setRoleIdList(roleIds);
            return this;
        }

        public EmployeeAddForm build() {
            // Set defaults
            if (form.getLoginName() == null) {
                form.setLoginName("test_" + System.currentTimeMillis());
            }
            if (form.getActualName() == null) {
                form.setActualName("Test Employee");
            }
            if (form.getDepartmentId() == null) {
                form.setDepartmentId(1L);
            }
            if (form.getGender() == null) {
                form.setGender(1);
            }
            return form;
        }
    }

    public static AddFormBuilder addFormBuilder() {
        return new AddFormBuilder();
    }
}
```

## Unique Data Generation

Strategies for generating unique test data to avoid collisions.

### Timestamp-Based Uniqueness

```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        long timestamp = System.currentTimeMillis();
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_" + timestamp);
        entity.setActualName("Test Employee " + timestamp);
        entity.setPhone("138" + (timestamp % 100000000)); // Generate unique phone
        entity.setDepartmentId(1L);
        entity.setDeletedFlag(false);
        return entity;
    }
}
```

### Counter-Based Uniqueness

```java
public class EmployeeTestFixture {

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static EmployeeEntity createEntity() {
        int id = counter.incrementAndGet();
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_emp_" + id);
        entity.setActualName("Test Employee " + id);
        entity.setPhone("138000" + String.format("%05d", id));
        entity.setDepartmentId(1L);
        entity.setDeletedFlag(false);
        return entity;
    }

    public static void resetCounter() {
        counter.set(0);
    }
}
```

**Usage in tests:**
```java
@BeforeEach
void setUp() {
    EmployeeTestFixture.resetCounter(); // Reset for each test
}
```

### UUID-Based Uniqueness

```java
public class EmployeeTestFixture {

    public static EmployeeEntity createEntity() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_" + uuid);
        entity.setActualName("Test Employee " + uuid);
        entity.setPhone("138" + uuid.hashCode() % 100000000);
        entity.setDepartmentId(1L);
        entity.setDeletedFlag(false);
        return entity;
    }
}
```

## Foreign Key Management

Patterns for handling foreign key dependencies in test fixtures.

### Separate FK Fixtures

```java
public class DepartmentTestFixture {

    public static DepartmentEntity createDepartment(String name) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDepartmentName(name);
        dept.setParentId(0L);
        dept.setSort(1);
        dept.setDeletedFlag(false);
        return dept;
    }

    public static DepartmentEntity createDepartment() {
        return createDepartment("Test Department " + System.currentTimeMillis());
    }
}
```

**Usage:**
```java
@BeforeEach
void setUp() {
    // Create department first
    DepartmentEntity dept = DepartmentTestFixture.createDepartment("Engineering");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();

    // Then create employee with FK
    EmployeeEntity employee = EmployeeTestFixture.createEntity(testDepartmentId);
}
```

### Composite Fixtures with FK

```java
public class EmployeeTestFixture {

    /**
     * Create employee with all dependencies
     */
    public static EmployeeWithDependencies createWithDependencies(
        DepartmentDao departmentDao,
        RoleDao roleDao
    ) {
        // Create department
        DepartmentEntity dept = DepartmentTestFixture.createDepartment();
        departmentDao.insert(dept);

        // Create roles
        RoleEntity role1 = RoleTestFixture.createRole("Admin");
        RoleEntity role2 = RoleTestFixture.createRole("User");
        roleDao.insert(role1);
        roleDao.insert(role2);

        // Create employee
        EmployeeEntity employee = createEntity(dept.getDepartmentId());

        return new EmployeeWithDependencies(employee, dept, List.of(role1, role2));
    }

    public static class EmployeeWithDependencies {
        public final EmployeeEntity employee;
        public final DepartmentEntity department;
        public final List<RoleEntity> roles;

        public EmployeeWithDependencies(
            EmployeeEntity employee,
            DepartmentEntity department,
            List<RoleEntity> roles
        ) {
            this.employee = employee;
            this.department = department;
            this.roles = roles;
        }
    }
}
```

**Usage:**
```java
@Test
void testComplexScenario() {
    // Create employee with all dependencies in one call
    EmployeeWithDependencies data = EmployeeTestFixture.createWithDependencies(
        departmentDao, roleDao
    );

    // Use in test
    employeeService.addEmployee(createAddForm(data));
}
```

## Complex Object Graphs

Patterns for creating complex test data structures.

### Nested Object Fixture

```java
public class GoodsTestFixture {

    public static GoodsEntity createEntity() {
        GoodsEntity goods = new GoodsEntity();
        goods.setGoodsName("Test Product " + System.currentTimeMillis());
        goods.setCategoryId(1L);
        goods.setPrice(new BigDecimal("99.99"));
        goods.setStock(100);
        goods.setGoodsStatus(GoodsStatusEnum.SELLING.getValue());
        goods.setDeletedFlag(false);
        return goods;
    }

    public static GoodsEntity createWithCategory(CategoryEntity category) {
        GoodsEntity goods = createEntity();
        goods.setCategoryId(category.getCategoryId());
        goods.setCategoryName(category.getCategoryName());
        return goods;
    }

    public static GoodsVO createVO() {
        GoodsVO vo = new GoodsVO();
        vo.setGoodsId(1L);
        vo.setGoodsName("Test Product");
        vo.setCategoryId(1L);
        vo.setCategoryName("Electronics");
        vo.setPrice(new BigDecimal("99.99"));
        vo.setStock(100);
        vo.setGoodsStatus(GoodsStatusEnum.SELLING.getValue());
        return vo;
    }
}
```

### Collection Fixtures

```java
public class EmployeeTestFixture {

    public static List<EmployeeEntity> createMultiple(int count) {
        List<EmployeeEntity> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(createEntity());
        }
        return employees;
    }

    public static List<EmployeeEntity> createMultiple(int count, Long departmentId) {
        List<EmployeeEntity> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(createEntity(departmentId));
        }
        return employees;
    }

    public static void insertMultiple(EmployeeDao dao, int count) {
        createMultiple(count).forEach(dao::insert);
    }
}
```

**Usage:**
```java
@Test
void testPagination() {
    // Insert 15 employees quickly
    EmployeeTestFixture.insertMultiple(employeeDao, 15);

    // Test pagination
    ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);
    assertTrue(response.getData().getTotal() >= 15);
}
```

## Fixture Inheritance

Share common fixture logic across modules.

### Base Fixture Class

```java
public abstract class BaseTestFixture {

    protected static final AtomicInteger counter = new AtomicInteger(0);

    protected static String uniqueString(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + counter.incrementAndGet();
    }

    protected static String uniquePhone() {
        return "138" + String.format("%08d", counter.incrementAndGet() % 100000000);
    }

    protected static String uniqueEmail(String name) {
        return name + "_" + System.currentTimeMillis() + "@test.com";
    }

    protected static LocalDateTime now() {
        return LocalDateTime.now();
    }

    protected static LocalDate today() {
        return LocalDate.now();
    }
}
```

### Module-Specific Fixtures

```java
public class EmployeeTestFixture extends BaseTestFixture {

    public static EmployeeEntity createEntity() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName(uniqueString("test_emp"));
        entity.setActualName("Test Employee");
        entity.setPhone(uniquePhone());
        entity.setEmail(uniqueEmail("employee"));
        entity.setDepartmentId(1L);
        entity.setDeletedFlag(false);
        entity.setCreateTime(now());
        return entity;
    }
}

public class DepartmentTestFixture extends BaseTestFixture {

    public static DepartmentEntity createDepartment() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDepartmentName(uniqueString("Test Dept"));
        dept.setParentId(0L);
        dept.setSort(1);
        dept.setDeletedFlag(false);
        dept.setCreateTime(now());
        return dept;
    }
}
```

## Best Practices

1. **Keep fixtures simple** - Start with simple static methods, add builders only when needed
2. **Generate unique data** - Use timestamps or counters to avoid collisions
3. **Separate FK creation** - Create dependencies in `@BeforeEach`, not inside fixtures
4. **Use meaningful defaults** - Fixtures should create valid objects by default
5. **Provide overrides** - Allow customization of key fields via parameters
6. **Document special cases** - Comment on non-obvious test data setup
7. **Reuse across tests** - Share fixtures between unit and integration tests
8. **Clean up counters** - Reset static counters in `@BeforeEach` if using counter-based uniqueness

## Anti-Patterns to Avoid

❌ **Don't create dependencies inside fixtures:**
```java
// BAD - DAO dependency in fixture
public static EmployeeEntity createWithDepartment(DepartmentDao dao) {
    DepartmentEntity dept = new DepartmentEntity();
    dao.insert(dept); // Don't persist in fixture
    return createEntity(dept.getDepartmentId());
}
```

✅ **Do create dependencies in tests:**
```java
// GOOD - Persist in test setup
@BeforeEach
void setUp() {
    DepartmentEntity dept = DepartmentTestFixture.createDepartment();
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
}
```

❌ **Don't use hardcoded IDs:**
```java
// BAD - Hardcoded ID can conflict
public static EmployeeEntity createEntity() {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(1L); // Don't set ID manually
    return entity;
}
```

✅ **Do let database generate IDs:**
```java
// GOOD - ID generated on insert
public static EmployeeEntity createEntity() {
    EmployeeEntity entity = new EmployeeEntity();
    // No setEmployeeId() - let database auto-generate
    return entity;
}
```

❌ **Don't use non-unique test data:**
```java
// BAD - Same login name every time
public static EmployeeEntity createEntity() {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setLoginName("test_employee"); // Collides if run multiple times
    return entity;
}
```

✅ **Do make test data unique:**
```java
// GOOD - Unique login name
public static EmployeeEntity createEntity() {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setLoginName("test_" + System.currentTimeMillis()); // Unique
    return entity;
}
```
