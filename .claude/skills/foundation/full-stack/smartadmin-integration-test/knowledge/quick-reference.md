# SmartAdmin Integration Test - Quick Reference

**Version**: 1.0.0  
**Last Updated**: 2026-02-02  
**Skill**: smartadmin-integration-test (P0 - Critical)

---

## Quick Start

### Basic Integration Test

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
public class EmployeeControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCreateEmployee() {
        EmployeeForm form = new EmployeeForm();
        form.setName("John Doe");
        
        ResponseEntity<ResponseDTO> response = restTemplate.postForEntity(
            "/api/employee",
            form,
            ResponseDTO.class
        );
        
        assertEquals(200, response.getStatusCodeValue());
    }
}

---

## 3 Core Components

### 1. Testcontainers Setup
- PostgreSQL: Database isolation
- Redis: Cache testing
- MinIO: File storage testing

### 2. DB Fixtures
- @Sql: Load test data
- @Transactional: Auto rollback

### 3. API Testing
- TestRestTemplate: REST calls
- MockMvc: Controller mocking

---

## Commands

### Run Integration Tests
./gradlew :sa-admin:integrationTest

### Run Specific Test
./gradlew :sa-admin:integrationTest --tests EmployeeControllerIT

### With Coverage
./gradlew :sa-admin:integrationTest jacocoTestReport

---

## Test Patterns

### Pattern 1: Controller + Service + DB

@SpringBootTest
@Testcontainers
class EmployeeIntegrationTest {
    @Container
    static PostgreSQLContainer<?> db = ...;
    
    @Test
    void testFullCRUD() {
        // Create, Read, Update, Delete with real DB
    }
}

### Pattern 2: API Contract Test

@Test
void testResponseFormat() {
    ResponseDTO response = restTemplate.getForObject(
        "/api/employee/1",
        ResponseDTO.class
    );
    
    assertTrue(response.isOk());
    assertNotNull(response.getData());
}

### Pattern 3: DB Fixture Test

@Test
@Sql("/test-data/employees.sql")
void testWithFixtures() {
    // Test with pre-loaded data
}

---

## Configuration

application-test.yml:
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

---

## Dependencies

testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:postgresql'
testImplementation 'org.testcontainers:junit-jupiter'

---

See SKILL.md for complete integration test patterns.
