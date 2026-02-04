# SmartAdmin Testing Suite - Quick Reference

## 測試模式

| 模式 | 命令 | 用途 |
|------|------|------|
| integration | `./gradlew integrationTest` | Testcontainers 整合測試 |
| fixtures | 使用 test-fixture-generator | 測試數據建構器 |
| unit | `./gradlew test` | 單元測試 |
| e2e | (future) | 端對端 API 測試 |

## 測試結構

```
sa-admin/src/test/java/
├── net/lab1024/sa/admin/module/
│   └── {module}/
│       ├── {Entity}ServiceTest.java       # 單元測試
│       ├── {Entity}IntegrationTest.java   # 整合測試
│       └── {Entity}TestFixture.java       # 測試數據
│
└── resources/
    └── application-test.yml               # 測試配置
```

## 整合測試基類

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("smartadmin_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## 測試數據建構器模式

```java
public class EmployeeTestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static EmployeeEntity createEntity() {
        int id = COUNTER.incrementAndGet();
        EmployeeEntity entity = new EmployeeEntity();
        entity.setFirstName("Test" + id);
        entity.setLastName("Employee" + id);
        entity.setEmail("test" + id + "@example.com");
        entity.setDeleted(false);
        return entity;
    }

    public static EmployeeAddForm createAddForm() {
        int id = COUNTER.incrementAndGet();
        EmployeeAddForm form = new EmployeeAddForm();
        form.setFirstName("Test" + id);
        form.setLastName("Employee" + id);
        form.setEmail("test" + id + "@example.com");
        return form;
    }
}
```

## 覆蓋率目標

| 類型 | 目標 |
|------|------|
| 整體 | >= 80% |
| Service | >= 85% |
| Manager | >= 90% |
| Controller | >= 70% |

## 命令速查

```bash
# 運行所有測試
./gradlew test

# 運行整合測試
./gradlew integrationTest

# 運行特定測試
./gradlew test --tests EmployeeServiceTest

# 生成覆蓋率報告
./gradlew jacocoTestReport

# 檢視報告
open build/reports/jacoco/test/html/index.html
```

## 相關規則

- [Q06-jacoco-coverage-rules.md](../../../../rules/quality-tools/Q06-jacoco-coverage-rules.md)
