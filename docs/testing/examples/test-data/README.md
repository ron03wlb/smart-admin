# Test Data SQL Scripts

This directory contains SQL scripts for loading test data in integration tests.

## Files

### employees-setup.sql

Sets up test data before integration tests:
- Test department (ID: 999)
- Test roles (IDs: 90, 91)
- Test employees (IDs: 1001-1003)
- Test role assignments

### cleanup.sql

Cleans up test data after integration tests:
- Removes all test employees
- Removes test role assignments
- Removes test roles and department

## Usage

### Option 1: @Sql Annotation (Recommended)

```java
@SpringBootTest
@Transactional
@Sql(scripts = "/test-data/employees-setup.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/cleanup.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EmployeeServiceIntTest {

    @Test
    void testQueryEmployee_WithTestData_ReturnsResults() {
        // Test data already loaded
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageSize(10);
        form.setPageNum(1);

        PageResult<EmployeeVO> result = employeeService.queryEmployee(form);

        assertTrue(result.getTotal() > 0);
    }
}
```

### Option 2: Manual Execution

```java
@SpringBootTest
class EmployeeServiceIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Load SQL script
        Resource resource = new ClassPathResource("test-data/employees-setup.sql");
        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), resource);
    }

    @AfterEach
    void tearDown() {
        Resource resource = new ClassPathResource("test-data/cleanup.sql");
        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), resource);
    }
}
```

### Option 3: @Transactional Auto-Rollback (Easiest)

```java
@SpringBootTest
@Transactional  // Automatically rolls back after each test
@Sql(scripts = "/test-data/employees-setup.sql")
class EmployeeServiceIntTest {
    // No cleanup needed - transaction rolls back automatically
}
```

## Test Data IDs

To avoid conflicts with production data, test data uses specific ID ranges:

| Entity | ID Range | Purpose |
|--------|----------|---------|
| **Departments** | 900-999 | Test departments |
| **Roles** | 90-99 | Test roles |
| **Employees** | 1001-1099 | Test employees |

## Customization

### Adding More Test Data

1. Edit `employees-setup.sql`
2. Follow the ID range conventions
3. Add corresponding DELETE statements to `cleanup.sql`
4. Use `ON CONFLICT DO NOTHING` for idempotency

### Example: Add Test Menu

```sql
-- In employees-setup.sql
INSERT INTO t_menu (menu_id, menu_name, ...)
VALUES (9001, 'Test Menu', ...)
ON CONFLICT (menu_id) DO NOTHING;

-- In cleanup.sql
DELETE FROM t_menu WHERE menu_id = 9001;
```

## Best Practices

### 1. Use @Transactional for Automatic Cleanup

```java
@SpringBootTest
@Transactional  // ✅ Easiest - auto-rollback
@Sql("/test-data/employees-setup.sql")
class MyTest { }
```

### 2. Idempotent Scripts

All scripts use `ON CONFLICT DO NOTHING` so they can run multiple times safely.

### 3. Avoid Hard-Coded Passwords

```sql
-- ❌ Bad
login_pwd = 'password123'

-- ✅ Good
login_pwd = '$2a$10$...'  -- BCrypt hash
```

### 4. Clean Up in Reverse Order

Delete child records before parent records due to foreign keys:

```sql
DELETE FROM t_role_employee;   -- Child first
DELETE FROM t_employee;        -- Parent second
```

### 5. Descriptive Test Data

Use clear naming for test data:

```sql
-- ✅ Good
login_name = 'test_user_admin'
actual_name = 'Test Admin User'

-- ❌ Bad
login_name = 'user1'
```

## Troubleshooting

### Issue: Foreign Key Constraint Violations

**Cause**: Cleanup order is wrong

**Solution**: Delete child records first

```sql
-- Correct order
DELETE FROM t_role_employee;  -- Junction table
DELETE FROM t_employee;       -- Foreign key holder
DELETE FROM t_role;           -- Referenced table
DELETE FROM t_department;     -- Referenced table
```

### Issue: Duplicate Key Errors

**Cause**: Test data IDs conflict with existing data

**Solution**: Use higher ID ranges (1000+, 9000+)

### Issue: Script Doesn't Execute

**Cause**: File path incorrect

**Solution**: Use leading slash

```java
// ✅ Correct
@Sql("/test-data/employees-setup.sql")

// ❌ Wrong
@Sql("test-data/employees-setup.sql")
```

## Related Documentation

- [Integration Testing Quick Reference](../../integration-testing-quick-reference.md)
- [Transaction Testing Guide](../../integration/transaction-testing.md)
- [Sa-Token Testing Guide](../../integration/sa-token-testing.md)

---

**These scripts are examples - customize them for your specific test needs!**
