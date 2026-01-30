# Phase 4: Test Generation

**Purpose**: Generate comprehensive tests for CRUD functionality.

**Integration**: Uses patterns from `smartadmin-integration-test` skill.

---

## Test Types

1. **Integration Tests** (Spring Boot + Testcontainers)
2. **Architecture Tests** (ArchUnit validation)
3. **Unit Tests** (Service layer - optional)

---

## 1. Generate Integration Test

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/{module}/{Entity}IntegrationTest.java`

**Pattern**:
```java
package net.lab1024.sa.admin.module.business.{module};

import net.lab1024.sa.admin.BaseIntegrationTest;
import net.lab1024.sa.admin.module.business.{module}.dao.{Entity}Dao;
import net.lab1024.sa.admin.module.business.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.business.{module}.domain.form.*;
import net.lab1024.sa.admin.module.business.{module}.domain.vo.{Entity}VO;
import net.lab1024.sa.admin.module.business.{module}.manager.{Entity}Manager;
import net.lab1024.sa.admin.module.business.{module}.service.{Entity}Service;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * {EntityName} Integration Test
 *
 * @author SmartAdmin Generator
 * @date 2024-01-01
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class {Entity}IntegrationTest extends BaseIntegrationTest {

    @Autowired
    private {Entity}Service {entity}Service;

    @Autowired
    private {Entity}Manager {entity}Manager;

    @Autowired
    private {Entity}Dao {entity}Dao;

    private Long test{Entity}Id;

    // Test Add
    @Test
    @Order(1)
    @DisplayName("Add {entity} - should succeed")
    void testAdd() {
        // Given
        {Entity}AddForm addForm = new {Entity}AddForm();
        // Set test data
        addForm.set{Field}("Test Value");

        // When
        ResponseDTO<Void> response = {entity}Service.add(addForm);

        // Then
        assertThat(response.isOk()).isTrue();

        // Verify in database
        List<{Entity}Entity> entities = {entity}Dao.selectList(null);
        assertThat(entities).isNotEmpty();
        test{Entity}Id = entities.get(0).get{Entity}Id();
    }

    @Test
    @Order(2)
    @DisplayName("Query {entity} with pagination - should return results")
    void testQuery() {
        // Given
        {Entity}QueryForm queryForm = new {Entity}QueryForm();
        queryForm.setPageNum(1);
        queryForm.setPageSize(10);
        queryForm.setKeyword("Test");

        // When
        ResponseDTO<PageResult<{Entity}VO>> response = {entity}Service.query(queryForm);

        // Then
        assertThat(response.isOk()).isTrue();
        PageResult<{Entity}VO> pageResult = response.getData();
        assertThat(pageResult.getList()).isNotEmpty();
        assertThat(pageResult.getTotal()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    @DisplayName("Get {entity} by ID - should return entity")
    void testGetById() {
        // Given
        assertThat(test{Entity}Id).isNotNull();

        // When
        ResponseDTO<{Entity}VO> response = {entity}Service.getById(test{Entity}Id);

        // Then
        assertThat(response.isOk()).isTrue();
        {Entity}VO vo = response.getData();
        assertThat(vo).isNotNull();
        assertThat(vo.get{Entity}Id()).isEqualTo(test{Entity}Id);
    }

    @Test
    @Order(4)
    @DisplayName("Update {entity} - should succeed")
    void testUpdate() {
        // Given
        {Entity}UpdateForm updateForm = new {Entity}UpdateForm();
        updateForm.set{Entity}Id(test{Entity}Id);
        updateForm.set{Field}("Updated Value");

        // When
        ResponseDTO<Void> response = {entity}Service.update(updateForm);

        // Then
        assertThat(response.isOk()).isTrue();

        // Verify update
        {Entity}Entity updated = {entity}Dao.selectById(test{Entity}Id);
        assertThat(updated.get{Field}()).isEqualTo("Updated Value");
    }

    @Test
    @Order(5)
    @DisplayName("Delete {entity} - should succeed")
    void testDelete() {
        // Given
        assertThat(test{Entity}Id).isNotNull();

        // When
        ResponseDTO<Void> response = {entity}Service.delete(test{Entity}Id);

        // Then
        assertThat(response.isOk()).isTrue();

        // Verify soft delete
        {Entity}Entity deleted = {entity}Dao.selectById(test{Entity}Id);
        assertThat(deleted).isNull(); // Soft deleted, so returns null
    }

    @Test
    @Order(6)
    @DisplayName("Batch delete {entity}s - should succeed")
    @Transactional
    void testBatchDelete() {
        // Given - Create test entities
        {Entity}Entity entity1 = new {Entity}Entity();
        entity1.set{Field}("Batch Test 1");
        {entity}Dao.insert(entity1);

        {Entity}Entity entity2 = new {Entity}Entity();
        entity2.set{Field}("Batch Test 2");
        {entity}Dao.insert(entity2);

        List<Long> ids = List.of(entity1.get{Entity}Id(), entity2.get{Entity}Id());

        // When
        {Entity}BatchDeleteForm batchDeleteForm = new {Entity}BatchDeleteForm();
        batchDeleteForm.set{Entity}IdList(ids);
        ResponseDTO<Void> response = {entity}Service.batchDelete(batchDeleteForm);

        // Then
        assertThat(response.isOk()).isTrue();

        // Verify all deleted
        for (Long id : ids) {
            {Entity}Entity deleted = {entity}Dao.selectById(id);
            assertThat(deleted).isNull();
        }
    }

    @Test
    @DisplayName("Get non-existent {entity} - should return error")
    void testGetByIdNotFound() {
        // Given
        Long nonExistentId = 999999L;

        // When
        ResponseDTO<{Entity}VO> response = {entity}Service.getById(nonExistentId);

        // Then
        assertThat(response.isOk()).isFalse();
        assertThat(response.getMsg()).contains("not found");
    }

    @Test
    @DisplayName("Add {entity} with invalid data - should fail validation")
    void testAddInvalid() {
        // Given
        {Entity}AddForm addForm = new {Entity}AddForm();
        // Missing required fields

        // When/Then
        assertThatThrownBy(() -> {entity}Service.add(addForm))
            .isInstanceOf(Exception.class);
    }
}
```

**Test Coverage Goals**:
- ✅ CRUD operations (Create, Read, Update, Delete)
- ✅ Batch operations
- ✅ Query with pagination
- ✅ Validation (invalid data handling)
- ✅ Error cases (not found scenarios)
- ✅ Database persistence verification

---

## 2. Architecture Test Validation

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/ArchitectureTest.java` (Already exists)

**Verify Generated Code Compliance**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected Validations** (for newly generated code):
- ✅ Service uses Vavr `Option` (NOT `Optional`)
- ✅ Controller → Service dependency (no direct Dao access)
- ✅ Manager has `@Transactional(rollbackFor = Throwable.class)`
- ✅ Constructor injection (no field injection)
- ✅ Boolean field is `deletedFlag` (NOT `isDeleted`)
- ✅ Correct package naming (v4.0.0+)

**If ArchitectureTest fails**, fix the generated code before proceeding.

---

## 3. Test Data Fixtures (Optional)

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/{module}/fixture/{Entity}Fixture.java`

**Pattern** (if complex test data needed):
```java
package net.lab1024.sa.admin.module.business.{module}.fixture;

import net.lab1024.sa.admin.module.business.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.business.{module}.domain.form.*;

import java.time.LocalDateTime;

/**
 * {EntityName} Test Fixture
 *
 * @author SmartAdmin Generator
 * @date 2024-01-01
 */
public class {Entity}Fixture {

    public static {Entity}Entity createEntity() {
        {Entity}Entity entity = new {Entity}Entity();
        entity.set{Field}("Test Value");
        // Set all fields with realistic test data
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    public static {Entity}AddForm createAddForm() {
        {Entity}AddForm form = new {Entity}AddForm();
        form.set{Field}("Test Value");
        // Set all required fields
        return form;
    }

    public static {Entity}UpdateForm createUpdateForm(Long id) {
        {Entity}UpdateForm form = new {Entity}UpdateForm();
        form.set{Entity}Id(id);
        form.set{Field}("Updated Value");
        // Set all fields
        return form;
    }

    public static {Entity}QueryForm createQueryForm() {
        {Entity}QueryForm form = new {Entity}QueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        form.setKeyword("Test");
        return form;
    }
}
```

---

## 4. Controller Test (Optional - if REST layer testing needed)

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/{module}/{Entity}ControllerTest.java`

**Pattern** (using MockMvc):
```java
package net.lab1024.sa.admin.module.business.{module};

import com.fasterxml.jackson.databind.ObjectMapper;
import net.lab1024.sa.admin.BaseIntegrationTest;
import net.lab1024.sa.admin.module.business.{module}.domain.form.{Entity}AddForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {EntityName} Controller Test
 *
 * @author SmartAdmin Generator
 * @date 2024-01-01
 */
@SpringBootTest
@AutoConfigureMockMvc
class {Entity}ControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testQuery() throws Exception {
        mockMvc.perform(post("/{module}/{entity}/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pageNum\":1,\"pageSize\":10}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void testAdd() throws Exception {
        {Entity}AddForm addForm = new {Entity}AddForm();
        addForm.set{Field}("Test Value");

        mockMvc.perform(post("/{module}/{entity}/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addForm)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1));
    }
}
```

---

## Test Execution

**Run All Tests**:
```bash
./gradlew :sa-admin:test
```

**Run Specific Test**:
```bash
./gradlew :sa-admin:test --tests {Entity}IntegrationTest
```

**Run with Coverage**:
```bash
./gradlew :sa-admin:test jacocoTestReport
# View: sa-admin/build/reports/jacoco/test/html/index.html
```

---

## Test Structure

```
sa-admin/src/test/java/net/lab1024/sa/admin/
├── BaseIntegrationTest.java (base class, already exists)
└── module/
    └── business/
        └── {module}/
            ├── {Entity}IntegrationTest.java    (Integration tests)
            ├── {Entity}ControllerTest.java     (Controller tests - optional)
            └── fixture/
                └── {Entity}Fixture.java         (Test data - optional)
```

---

## Testcontainers Setup

Integration tests use Testcontainers for PostgreSQL + Redis.

**Configuration** (in `BaseIntegrationTest.java`):
```java
@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
}
```

**Requirements**:
- Docker must be running
- First test run will download containers (slow)
- Subsequent runs reuse containers (fast)

---

## Validation Checklist

After generation, verify:
- [ ] Integration test exists for CRUD operations
- [ ] All test methods pass: `./gradlew :sa-admin:test --tests {Entity}IntegrationTest`
- [ ] ArchitectureTest passes: `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] Test coverage > 80% for Service layer
- [ ] Tests use realistic data (not just "test" or "string")
- [ ] Tests verify database persistence
- [ ] Tests check error cases (not found, validation failures)
- [ ] Testcontainers start correctly (Docker running)

---

## Common Issues & Solutions

**Issue 1**: Testcontainers timeout
- **Solution**: Ensure Docker is running
- **Solution**: Check Docker has network access
- **Solution**: Increase timeout in test configuration

**Issue 2**: Tests fail due to data conflicts
- **Solution**: Use `@Transactional` on test methods for auto-rollback
- **Solution**: Use unique test data (timestamps, UUIDs)
- **Solution**: Clean up test data in `@AfterEach` method

**Issue 3**: ArchitectureTest fails for generated code
- **Solution**: Review Phase 1 generation (backend) for compliance
- **Solution**: Check Service uses Vavr `Option` (NOT `Optional`)
- **Solution**: Verify Manager has correct `@Transactional` annotation

---

## Integration Notes

This phase uses:
- ✅ BaseIntegrationTest with Testcontainers from smartadmin-integration-test
- ✅ AssertJ fluent assertions for readable tests
- ✅ JUnit 5 with `@Order` for test execution order
- ✅ `@Transactional` for test isolation
- ✅ Realistic test data patterns
- ✅ ArchitectureTest validation for generated code compliance

**Testing Best Practices**:
- Test realistic scenarios (not just happy paths)
- Verify database state after operations
- Use descriptive test method names
- Keep tests independent (no shared state)
- Mock external dependencies if needed
