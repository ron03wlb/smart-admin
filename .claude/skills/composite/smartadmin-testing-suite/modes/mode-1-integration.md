# Mode 1: Integration Tests

**Purpose**: Test Service/Manager/Controller layers with real database and Spring Boot context

**Time**: ~10 minutes per service

**Consolidates**: smartadmin-integration-test skill patterns

---

## When to Use This Mode

Use Mode 1 when:
- ✅ Testing business logic with database persistence
- ✅ Validating @Transactional rollback behavior
- ✅ Testing Service/Manager layer interactions
- ✅ Verifying ResponseDTO responses AND database state
- ✅ Testing complex queries with JOIN operations
- ✅ Validating FK constraints and cascade operations

**Command**:
```bash
/test EmployeeService --mode=integration
# Or simply:
/test EmployeeService  # (integration is default mode)
```

---

## Core Pattern: Service Layer Integration Test

### Test Class Structure

```java
package net.lab1024.sa.admin.module.{module}.service;

import net.lab1024.sa.admin.BaseIntegrationTest;
import net.lab1024.sa.admin.module.{module}.domain.entity.{Entity}Entity;
import net.lab1024.sa.admin.module.{module}.domain.form.{Entity}AddForm;
import net.lab1024.sa.admin.module.{module}.domain.form.{Entity}UpdateForm;
import net.lab1024.sa.admin.module.{module}.domain.form.{Entity}QueryForm;
import net.lab1024.sa.admin.module.{module}.domain.vo.{Entity}VO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {Entity}Service Integration Test
 *
 * Tests business logic with real database using Testcontainers.
 * Auto-rollback after each test via @Transactional.
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class {Entity}ServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private {Entity}Service {entity}Service;

    @Autowired
    private {Entity}Dao {entity}Dao;

    // FK dependency IDs (setup in @BeforeEach)
    private Long testDepartmentId;
    private Long testRoleId;

    @BeforeEach
    void setUp() {
        // Create FK dependencies using test fixtures
        DepartmentEntity dept = {Entity}TestFixture.createDepartment("Test Department");
        departmentDao.insert(dept);
        testDepartmentId = dept.getDepartmentId();

        RoleEntity role = {Entity}TestFixture.createRole("Test Role");
        roleDao.insert(role);
        testRoleId = role.getRoleId();
    }

    @Test
    @Order(1)
    @DisplayName("Add {entity} - should succeed and persist to database")
    void addEntity_ValidForm_PersistsToDatabase() {
        // Given
        {Entity}AddForm form = {Entity}TestFixture.createAddForm(testDepartmentId, testRoleId);

        // When
        ResponseDTO<String> response = {entity}Service.add{Entity}(form);

        // Then - Verify ResponseDTO
        assertTrue(response.getOk(), "Expected success response: " + response.getMsg());

        // Then - Verify database state
        List<{Entity}Entity> entities = {entity}Dao.selectList(
            Wrappers.<{Entity}Entity>lambdaQuery()
                .eq({Entity}Entity::getLoginName, form.getLoginName())
        );
        assertEquals(1, entities.size(), "Expected exactly 1 entity in database");
        {Entity}Entity savedEntity = entities.get(0);
        assertEquals(form.getActualName(), savedEntity.getActualName());
        assertEquals(testDepartmentId, savedEntity.getDepartmentId());
        assertNotNull(savedEntity.getCreateTime());
    }

    @Test
    @Order(2)
    @DisplayName("Query {entity}s - should return paginated results")
    void queryEntities_ValidForm_ReturnsPaginatedResults() {
        // Given - Insert 3 test entities
        for (int i = 0; i < 3; i++) {
            {Entity}Entity entity = {Entity}TestFixture.createEntity(testDepartmentId);
            {entity}Dao.insert(entity);
        }

        {Entity}QueryForm queryForm = new {Entity}QueryForm();
        queryForm.setPageNum(1);
        queryForm.setPageSize(10);

        // When
        ResponseDTO<PageResult<{Entity}VO>> response = {entity}Service.query{Entity}(queryForm);

        // Then
        assertTrue(response.getOk());
        PageResult<{Entity}VO> pageResult = response.getData();
        assertTrue(pageResult.getTotal() >= 3, "Expected at least 3 entities");
        assertFalse(pageResult.getList().isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Update {entity} - should succeed and update database")
    void updateEntity_ValidForm_UpdatesDatabase() {
        // Given - Insert entity
        {Entity}Entity entity = {Entity}TestFixture.createEntity(testDepartmentId);
        {entity}Dao.insert(entity);
        Long entityId = entity.get{Entity}Id();

        {Entity}UpdateForm form = new {Entity}UpdateForm();
        form.set{Entity}Id(entityId);
        form.setActualName("Updated Name");
        form.setEmail("updated@example.com");
        form.setDepartmentId(testDepartmentId);

        // When
        ResponseDTO<String> response = {entity}Service.update{Entity}(form);

        // Then - Verify ResponseDTO
        assertTrue(response.getOk());

        // Then - Verify database state
        {Entity}Entity updatedEntity = {entity}Dao.selectById(entityId);
        assertEquals("Updated Name", updatedEntity.getActualName());
        assertEquals("updated@example.com", updatedEntity.getEmail());
        assertNotNull(updatedEntity.getUpdateTime());
    }

    @Test
    @Order(4)
    @DisplayName("Delete {entity} - should soft delete (set deletedFlag)")
    void deleteEntity_ValidId_SoftDeletes() {
        // Given - Insert entity
        {Entity}Entity entity = {Entity}TestFixture.createEntity(testDepartmentId);
        {entity}Dao.insert(entity);
        Long entityId = entity.get{Entity}Id();

        // When
        ResponseDTO<String> response = {entity}Service.delete{Entity}(entityId);

        // Then - Verify ResponseDTO
        assertTrue(response.getOk());

        // Then - Verify soft delete (deletedFlag = 1, MyBatis Plus @TableLogic)
        {Entity}Entity deletedEntity = {entity}Dao.selectById(entityId);
        assertNull(deletedEntity, "Expected entity to be soft deleted (not visible in queries)");

        // Verify raw database state (bypass @TableLogic)
        Long count = {entity}Dao.selectCount(
            Wrappers.<{Entity}Entity>lambdaQuery()
                .eq({Entity}Entity::get{Entity}Id, entityId)
                .last("AND deleted_flag = 1")  // Check soft delete flag
        );
        assertEquals(1L, count, "Expected entity to exist with deletedFlag = 1");
    }

    @Test
    @Order(5)
    @DisplayName("Batch delete {entity}s - should soft delete multiple")
    void batchDeleteEntities_ValidIds_SoftDeletesAll() {
        // Given - Insert 3 entities
        List<Long> entityIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            {Entity}Entity entity = {Entity}TestFixture.createEntity(testDepartmentId);
            {entity}Dao.insert(entity);
            entityIds.add(entity.get{Entity}Id());
        }

        // When
        ResponseDTO<String> response = {entity}Service.batchDelete{Entity}(entityIds);

        // Then - Verify ResponseDTO
        assertTrue(response.getOk());

        // Then - Verify all soft deleted
        entityIds.forEach(id -> {
            {Entity}Entity entity = {entity}Dao.selectById(id);
            assertNull(entity, "Expected entity " + id + " to be soft deleted");
        });
    }

    @Test
    @Order(6)
    @DisplayName("Add {entity} with duplicate unique field - should fail")
    void addEntity_DuplicateUniqueField_Fails() {
        // Given - Insert entity with unique loginName
        {Entity}Entity existingEntity = {Entity}TestFixture.createEntity(testDepartmentId);
        {entity}Dao.insert(existingEntity);

        // Try to insert duplicate
        {Entity}AddForm form = {Entity}TestFixture.createAddForm(testDepartmentId, testRoleId);
        form.setLoginName(existingEntity.getLoginName());  // Duplicate

        // When
        ResponseDTO<String> response = {entity}Service.add{Entity}(form);

        // Then - Verify failure
        assertFalse(response.getOk(), "Expected failure due to duplicate unique field");
        assertTrue(response.getMsg().contains("already exists") ||
                   response.getMsg().contains("duplicate"),
                   "Expected error message about duplication");
    }

    @Test
    @Order(7)
    @DisplayName("Transaction rollback - failed operation should not persist")
    void transactionRollback_FailedOperation_DoesNotPersist() {
        // Given
        {Entity}AddForm form = {Entity}TestFixture.createAddForm(testDepartmentId, testRoleId);
        form.setEmail("invalid-email");  // Assume validation fails

        // When
        try {
            {entity}Service.add{Entity}(form);
        } catch (Exception e) {
            // Expected to fail validation
        }

        // Then - Verify nothing persisted
        Long count = {entity}Dao.selectCount(
            Wrappers.<{Entity}Entity>lambdaQuery()
                .eq({Entity}Entity::getLoginName, form.getLoginName())
        );
        assertEquals(0L, count, "Expected no entity to persist after failed transaction");
    }
}
```

---

## Test Execution Order

Tests are executed in order using `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`:

1. **@Order(1): Add** - Basic insert operation
2. **@Order(2): Query** - Pagination and filtering
3. **@Order(3): Update** - Modification operation
4. **@Order(4): Delete** - Soft delete verification
5. **@Order(5): Batch Delete** - Multiple deletions
6. **@Order(6): Duplicate Validation** - Unique constraint
7. **@Order(7): Transaction Rollback** - Rollback verification

---

## Key Features

### 1. Extends BaseIntegrationTest

```java
@SpringBootTest
@Transactional
class {Entity}ServiceIntegrationTest extends BaseIntegrationTest {
    // ...
}
```

**BaseIntegrationTest provides**:
- ✅ Testcontainers setup (PostgreSQL, Redis)
- ✅ Spring Boot application context
- ✅ @Transactional auto-rollback after each test
- ✅ Common test utilities

### 2. Real Spring Beans (NOT Mocks)

```java
@Autowired
private {Entity}Service {entity}Service;  // Real service with full dependency injection

@Autowired
private {Entity}Dao {entity}Dao;  // Real Dao with database access
```

**Benefits**:
- Tests real business logic
- Validates @Transactional behavior
- Tests cache operations
- Verifies FK constraints

### 3. Two-Level Verification

**Always verify both**:

1. **ResponseDTO** (API contract):
```java
ResponseDTO<String> response = {entity}Service.add{Entity}(form);
assertTrue(response.getOk(), "Expected success: " + response.getMsg());
```

2. **Database State** (persistence):
```java
{Entity}Entity savedEntity = {entity}Dao.selectById(entityId);
assertEquals(form.getActualName(), savedEntity.getActualName());
```

### 4. Test Fixtures for Data Setup

```java
@BeforeEach
void setUp() {
    // Use fixtures to create FK dependencies
    DepartmentEntity dept = {Entity}TestFixture.createDepartment("Test Dept");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
}
```

**Benefits**:
- ✅ Unique test data (AtomicInteger)
- ✅ No hard-coded values
- ✅ Reusable across tests
- ✅ No unique constraint violations

### 5. Soft Delete Verification

```java
// Verify soft delete (MyBatis Plus @TableLogic hides deleted records)
{Entity}Entity deletedEntity = {entity}Dao.selectById(entityId);
assertNull(deletedEntity, "Expected entity to be soft deleted");

// Verify raw database state
Long count = {entity}Dao.selectCount(
    Wrappers.<{Entity}Entity>lambdaQuery()
        .eq({Entity}Entity::get{Entity}Id, entityId)
        .last("AND deleted_flag = 1")  // Bypass @TableLogic
);
assertEquals(1L, count, "Expected deletedFlag = 1");
```

---

## Manager Layer Integration Test

When testing Manager layer (transaction boundaries):

```java
@SpringBootTest
@Transactional
class {Entity}ManagerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private {Entity}Manager {entity}Manager;

    @Autowired
    private {Entity}Dao {entity}Dao;

    @Test
    @DisplayName("Transaction rollback - exception should rollback all changes")
    void transactionRollback_Exception_RollsBackAllChanges() {
        // Given - Initial count
        Long initialCount = {entity}Dao.selectCount(null);

        // When - Manager method throws exception mid-transaction
        assertThrows(BusinessException.class, () -> {
            {entity}Manager.performComplexOperation();  // Inserts 3 entities, then throws
        });

        // Then - Verify all changes rolled back
        Long finalCount = {entity}Dao.selectCount(null);
        assertEquals(initialCount, finalCount, "Expected all changes to be rolled back");
    }

    @Test
    @DisplayName("Cacheable annotation - second call should use cache")
    void cacheableAnnotation_SecondCall_UsesCache() {
        // Given
        Long entityId = createTestEntity();

        // First call - database hit
        {Entity}VO result1 = {entity}Manager.get{Entity}ById(entityId);

        // Clear Dao mock (if tracking calls)
        // reset({entity}Dao);  // NOT applicable - real Dao, not mock

        // Second call - cache hit (verify via logs or metrics)
        {Entity}VO result2 = {entity}Manager.get{Entity}ById(entityId);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        // In real scenario, check Redis or cache metrics
    }
}
```

---

## Controller Layer Integration Test

When testing Controller layer (HTTP endpoints):

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class {Entity}ControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private {Entity}Dao {entity}Dao;

    @Test
    @DisplayName("POST /api/{module}/{entity}/add - should create entity")
    void addEntity_ValidRequest_Returns200() throws Exception {
        // Given
        {Entity}AddForm form = {Entity}TestFixture.createAddForm(1L, 1L);
        String requestBody = objectMapper.writeValueAsString(form);

        // When
        MvcResult result = mockMvc.perform(post("/api/{module}/{entity}/add")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-access-token", getAdminToken())  // Mock authentication
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.ok").value(true))
            .andReturn();

        // Then - Verify database state
        String loginName = form.getLoginName();
        {Entity}Entity savedEntity = {entity}Dao.selectOne(
            Wrappers.<{Entity}Entity>lambdaQuery()
                .eq({Entity}Entity::getLoginName, loginName)
        );
        assertNotNull(savedEntity);
    }

    @Test
    @DisplayName("POST /api/{module}/{entity}/add - no permission should return 403")
    void addEntity_NoPermission_Returns403() throws Exception {
        // Given
        {Entity}AddForm form = {Entity}TestFixture.createAddForm(1L, 1L);
        String requestBody = objectMapper.writeValueAsString(form);

        // When - No token or invalid permission
        mockMvc.perform(post("/api/{module}/{entity}/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }
}
```

---

## Validation Checklist

After generating integration tests, verify:

- [ ] Test class extends `BaseIntegrationTest`
- [ ] Uses `@SpringBootTest` and `@Transactional`
- [ ] Real Spring beans (`@Autowired`, NOT `@Mock`)
- [ ] Verifies **both** ResponseDTO AND database state
- [ ] Test fixtures used for test data (no hardcoded values)
- [ ] FK dependencies set up in `@BeforeEach`
- [ ] Tests cover: Add, Query, Update, Delete, Batch Delete, Validation
- [ ] Soft delete verification includes `deletedFlag` check
- [ ] Transaction rollback test included
- [ ] Tests pass: `./gradlew :sa-admin:test --tests {Entity}ServiceIntegrationTest`

---

## Troubleshooting

### Problem: "Testcontainers failed to start"

**Cause**: Docker not running

**Solution**:
```bash
# Start Docker Desktop
# Ensure 4GB+ memory allocated to Docker

# Verify Docker is running
docker ps
```

### Problem: "Unique constraint violation in tests"

**Cause**: Not using test fixtures with AtomicInteger

**Solution**:
```java
// ❌ BAD: Hardcoded values
{Entity}Entity entity = new {Entity}Entity();
entity.setLoginName("test");  // Fails on second test run

// ✅ GOOD: Use test fixture
{Entity}Entity entity = {Entity}TestFixture.createEntity(departmentId);
// Generates unique: "test1", "test2", "test3", ...
```

### Problem: "Test data not rolled back"

**Cause**: Missing `@Transactional` on test class

**Solution**:
```java
@SpringBootTest
@Transactional  // ← Add this
class {Entity}ServiceIntegrationTest extends BaseIntegrationTest {
    // ...
}
```

### Problem: "FK constraint violation"

**Cause**: Related entity not created in `@BeforeEach`

**Solution**:
```java
@BeforeEach
void setUp() {
    // Create FK dependencies BEFORE test
    DepartmentEntity dept = {Entity}TestFixture.createDepartment("Test");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
}
```

### Problem: "NullPointerException when accessing service"

**Cause**: Service not autowired correctly

**Solution**:
```java
@Autowired
private {Entity}Service {entity}Service;  // Ensure @Autowired is present
```

---

## Time Estimates

| Test Scope | Time |
|------------|------|
| Service layer (7 tests) | ~10 minutes |
| Manager layer (3 tests) | ~5 minutes |
| Controller layer (5 tests) | ~8 minutes |

**Total**: ~23 minutes for complete integration test coverage

---

## Related Documentation

- **[SKILL.md](../SKILL.md)** - Testing suite overview
- **[mode-2-fixtures.md](mode-2-fixtures.md)** - Test fixture patterns
- **[BaseIntegrationTest](../../../sa-admin/src/test/java/net/lab1024/sa/admin/BaseIntegrationTest.java)** - Base test class

---

**Version**: 2.0.0 (Testing Suite Consolidation)
**Last Updated**: 2026-01-27
