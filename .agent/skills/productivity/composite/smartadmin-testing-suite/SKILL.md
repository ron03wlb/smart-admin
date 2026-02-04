---
name: smartadmin-testing-suite
description: Generates integration tests, test fixtures, and test data builders for SmartAdmin modules with Testcontainers support
---

# SmartAdmin Testing Suite

Generates comprehensive test suites following SmartAdmin testing patterns.

## Usage

```bash
/test EmployeeService --mode=integration   # Integration tests with Testcontainers
/test Employee --mode=fixtures             # Test fixture generators
/test EmployeeController --mode=api        # API endpoint tests
```

## Modes

### 1. Integration Tests (`--mode=integration`)

Generates integration tests with:
- Testcontainers for PostgreSQL
- Real database operations
- Transaction rollback

**Generated Files:**
```
src/test/java/
├── net/lab1024/sa/admin/
│   ├── base/
│   │   └── BaseIntegrationTest.java    # Shared test config
│   └── module/{module}/
│       └── {Entity}ServiceIntegrationTest.java
```

### 2. Test Fixtures (`--mode=fixtures`)

Generates test data builders:
- Unique ID generation with AtomicInteger
- Randomized field values
- Builder pattern

**Generated Files:**
```
src/test/java/
└── net/lab1024/sa/admin/module/{module}/
    └── fixture/
        └── {Entity}Fixture.java
```

### 3. API Tests (`--mode=api`)

Generates MockMvc-based API tests:
- Request/Response validation
- Authentication headers
- Error scenarios

**Generated Files:**
```
src/test/java/
└── net/lab1024/sa/admin/module/{module}/
    └── {Entity}ControllerTest.java
```

## Test Patterns

### Base Integration Test

```java
@SpringBootTest
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("smartadmin_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Test Fixture Pattern

```java
public class EmployeeFixture {
    
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    
    public static EmployeeEntity create() {
        return EmployeeEntity.builder()
            .employeeId((long) ID_COUNTER.getAndIncrement())
            .loginName("emp_" + RandomStringUtils.randomAlphanumeric(8))
            .actualName("Test Employee " + ID_COUNTER.get())
            .email("emp" + ID_COUNTER.get() + "@test.com")
            .build();
    }
    
    public static EmployeeEntity createWithDepartment(Long departmentId) {
        return create().toBuilder()
            .departmentId(departmentId)
            .build();
    }
}
```

## Coverage Requirements

| Metric            | Minimum |
| ----------------- | ------- |
| Line Coverage     | 80%     |
| Branch Coverage   | 70%     |
| New Code Coverage | 80%     |

## Workflow

```
1. Parse user request for entity/service name
2. Determine test mode
3. Check if BaseIntegrationTest exists, create if not
4. Generate appropriate test class
5. If fixtures mode, generate fixture class
6. Run tests: ./gradlew test --tests "*{Entity}*"
7. Check coverage: ./gradlew jacocoTestReport
8. Report results
```

## Related Rules

- [quality-tools/Q06-jacoco-coverage-rules.md](../../rules/quality-tools/Q06-jacoco-coverage-rules.md)
- [technology/functional/P03-vavr-mybatis-integration.md](../../rules/technology/functional/P03-vavr-mybatis-integration.md)

## Example Session

**User:** `/test EmployeeService --mode=integration`

**AI Agent Actions:**
1. Check for BaseIntegrationTest, create if missing
2. Create EmployeeFixture for test data
3. Create EmployeeServiceIntegrationTest with:
   - @SpringBootTest, @Testcontainers
   - Test cases for CRUD operations
   - Vavr Option assertions
4. Run `./gradlew test --tests "EmployeeServiceIntegrationTest"`
5. Run `./gradlew jacocoTestReport`
6. Report coverage results
