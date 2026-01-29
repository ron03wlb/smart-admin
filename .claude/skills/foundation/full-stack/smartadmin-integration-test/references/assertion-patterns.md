# Assertion Patterns for SmartAdmin Integration Tests

Comprehensive assertion patterns for verifying SmartAdmin responses, database state, and cache behavior.

## Table of Contents

- [ResponseDTO Assertions](#responsedto-assertions)
- [PageResult Assertions](#pageresult-assertions)
- [Database State Assertions](#database-state-assertions)
- [Cache Behavior Assertions](#cache-behavior-assertions)
- [Transaction Assertions](#transaction-assertions)
- [MockMvc Assertions](#mockmvc-assertions)
- [Custom Assertion Helpers](#custom-assertion-helpers)

## ResponseDTO Assertions

### Success Response

```java
// Basic success assertion
ResponseDTO<String> response = service.addEmployee(form);
assertNotNull(response, "Response should not be null");
assertTrue(response.getOk(), "Expected success but got error: " + response.getMsg());

// Success with data
ResponseDTO<Long> response = service.addEmployee(form);
assertTrue(response.getOk());
assertNotNull(response.getData(), "Response data should not be null");
assertTrue(response.getData() > 0, "Employee ID should be positive");

// Success with specific data value
ResponseDTO<String> response = service.updateEmployee(form);
assertTrue(response.getOk());
assertEquals("更新成功", response.getData());
```

### Error Response

```java
// Basic error assertion
ResponseDTO<String> response = service.addEmployee(invalidForm);
assertNotNull(response);
assertFalse(response.getOk(), "Expected error but got success");

// Error with specific error code
ResponseDTO<String> response = service.addEmployee(invalidForm);
assertFalse(response.getOk());
assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode(),
    "Expected DATA_NOT_EXIST error code");

// Error with message content check
ResponseDTO<String> response = service.addEmployee(form);
assertFalse(response.getOk());
assertNotNull(response.getMsg(), "Error message should not be null");
assertTrue(response.getMsg().contains("部门不存在"),
    "Error message should mention department not found");

// Error with message pattern matching
ResponseDTO<String> response = service.addEmployee(duplicateForm);
assertFalse(response.getOk());
assertTrue(response.getMsg().matches(".*登录名.*已存在.*"),
    "Error message should indicate duplicate login name");
```

### Helper Methods (from BaseUnitTest)

```java
// Using BaseIntegrationTest assertion helpers
ResponseDTO<String> response = service.addEmployee(form);

// Success assertions
assertOk(response); // Basic success check
assertOkWithData(response, "添加成功"); // Success with specific data
Long employeeId = assertOkAndGetData(response); // Success, returns data

// Error assertions
assertError(response); // Basic error check
assertError(response, UserErrorCode.DATA_NOT_EXIST); // Error with code
assertErrorContains(response, "部门"); // Error message contains text
```

## PageResult Assertions

### Basic Pagination

```java
ResponseDTO<PageResult<EmployeeVO>> response = service.queryEmployee(queryForm);

// Verify ResponseDTO
assertTrue(response.getOk());
assertNotNull(response.getData(), "PageResult should not be null");

// Verify PageResult structure
PageResult<EmployeeVO> page = response.getData();
assertNotNull(page.getList(), "Result list should not be null");
assertNotNull(page.getTotal(), "Total count should not be null");
assertNotNull(page.getPageSize(), "Page size should not be null");
assertNotNull(page.getPageNum(), "Page number should not be null");

// Verify pagination logic
assertTrue(page.getTotal() >= 0, "Total should be non-negative");
assertTrue(page.getList().size() <= page.getPageSize(),
    "Result size should not exceed page size");
```

### Page Size Validation

```java
// Request page 1, size 10
queryForm.setPageNum(1);
queryForm.setPageSize(10);
ResponseDTO<PageResult<EmployeeVO>> response = service.queryEmployee(queryForm);

PageResult<EmployeeVO> page = response.getData();

// Verify page size respected
assertTrue(page.getList().size() <= 10,
    "Should return at most 10 results");

// If total > 10, should return exactly 10
if (page.getTotal() > 10) {
    assertEquals(10, page.getList().size(),
        "Should return exactly 10 results when more data available");
}
```

### Page Number Validation

```java
// Insert 25 employees
EmployeeTestFixture.insertMultiple(employeeDao, 25);

// Query page 1
queryForm.setPageNum(1);
queryForm.setPageSize(10);
PageResult<EmployeeVO> page1 = service.queryEmployee(queryForm).getData();
assertEquals(10, page1.getList().size());

// Query page 2
queryForm.setPageNum(2);
PageResult<EmployeeVO> page2 = service.queryEmployee(queryForm).getData();
assertEquals(10, page2.getList().size());

// Query page 3
queryForm.setPageNum(3);
PageResult<EmployeeVO> page3 = service.queryEmployee(queryForm).getData();
assertEquals(5, page3.getList().size(), "Page 3 should have remaining 5 records");

// Verify no duplicate IDs across pages
Set<Long> allIds = new HashSet<>();
page1.getList().forEach(emp -> allIds.add(emp.getEmployeeId()));
page2.getList().forEach(emp -> allIds.add(emp.getEmployeeId()));
page3.getList().forEach(emp -> allIds.add(emp.getEmployeeId()));
assertEquals(25, allIds.size(), "Should have 25 unique employee IDs");
```

### Empty Results

```java
// Query with filter that matches nothing
queryForm.setLoginName("nonexistent_user");
ResponseDTO<PageResult<EmployeeVO>> response = service.queryEmployee(queryForm);

assertTrue(response.getOk());
PageResult<EmployeeVO> page = response.getData();
assertNotNull(page);
assertEquals(0, page.getTotal(), "Total should be 0 for no matches");
assertTrue(page.getList().isEmpty(), "List should be empty");
```

### Sort Order Validation

```java
// Query with sort
queryForm.setSortField("createTime");
queryForm.setSortOrder("DESC");
ResponseDTO<PageResult<EmployeeVO>> response = service.queryEmployee(queryForm);

PageResult<EmployeeVO> page = response.getData();
assertTrue(page.getList().size() >= 2, "Need at least 2 records to verify sort");

// Verify descending order
for (int i = 0; i < page.getList().size() - 1; i++) {
    LocalDateTime current = page.getList().get(i).getCreateTime();
    LocalDateTime next = page.getList().get(i + 1).getCreateTime();
    assertTrue(current.isAfter(next) || current.isEqual(next),
        "Results should be sorted by createTime DESC");
}
```

## Database State Assertions

### Record Existence

```java
// Verify record was inserted
EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
ResponseDTO<Long> response = employeeService.addEmployee(form);
assertTrue(response.getOk());

Long employeeId = response.getData();
EmployeeEntity saved = employeeDao.selectById(employeeId);
assertNotNull(saved, "Employee should exist in database");

// Verify with custom query
List<EmployeeEntity> employees = employeeDao.selectList(
    Wrappers.<EmployeeEntity>lambdaQuery()
        .eq(EmployeeEntity::getLoginName, form.getLoginName())
);
assertEquals(1, employees.size(), "Should find exactly 1 employee with login name");
```

### Field Values

```java
// Verify all fields were saved correctly
EmployeeEntity saved = employeeDao.selectById(employeeId);
assertEquals(form.getLoginName(), saved.getLoginName());
assertEquals(form.getActualName(), saved.getActualName());
assertEquals(form.getPhone(), saved.getPhone());
assertEquals(form.getDepartmentId(), saved.getDepartmentId());
assertEquals(form.getGender(), saved.getGender());
assertFalse(saved.getDeletedFlag(), "Deleted flag should be false");
assertNotNull(saved.getCreateTime(), "Create time should be set");
```

### Record Update

```java
// Verify record was updated
EmployeeUpdateForm updateForm = EmployeeTestFixture.createUpdateForm(employeeId);
updateForm.setActualName("Updated Name");
updateForm.setPhone("13900000002");

employeeService.updateEmployee(updateForm);

EmployeeEntity updated = employeeDao.selectById(employeeId);
assertEquals("Updated Name", updated.getActualName());
assertEquals("13900000002", updated.getPhone());
assertNotNull(updated.getUpdateTime(), "Update time should be set");
```

### Soft Delete

```java
// Verify soft delete (deleted_flag = true)
employeeService.deleteEmployee(employeeId);

EmployeeEntity deleted = employeeDao.selectById(employeeId);
assertNotNull(deleted, "Record should still exist");
assertTrue(deleted.getDeletedFlag(), "Deleted flag should be true");

// Verify not returned in queries
List<EmployeeEntity> active = employeeDao.selectList(
    Wrappers.<EmployeeEntity>lambdaQuery()
        .eq(EmployeeEntity::getDeletedFlag, false)
);
assertFalse(active.stream().anyMatch(e -> e.getEmployeeId().equals(employeeId)),
    "Deleted employee should not appear in active list");
```

### Relationship Assertions

```java
// Verify employee-role relationships
employeeManager.saveEmployee(employee, List.of(1L, 2L));

// Verify employee saved
EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
assertNotNull(saved);

// Verify roles saved
List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
    Wrappers.<RoleEmployeeEntity>lambdaQuery()
        .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId())
);
assertEquals(2, roles.size(), "Should have 2 role assignments");

Set<Long> roleIds = roles.stream()
    .map(RoleEmployeeEntity::getRoleId)
    .collect(Collectors.toSet());
assertTrue(roleIds.contains(1L), "Should have role 1");
assertTrue(roleIds.contains(2L), "Should have role 2");
```

### Count Assertions

```java
// Verify record count
long countBefore = employeeDao.selectCount(null);

employeeService.addEmployee(form1);
employeeService.addEmployee(form2);

long countAfter = employeeDao.selectCount(null);
assertEquals(countBefore + 2, countAfter, "Should have 2 more employees");

// Verify count with condition
long activeCount = employeeDao.selectCount(
    Wrappers.<EmployeeEntity>lambdaQuery()
        .eq(EmployeeEntity::getDeletedFlag, false)
);
assertTrue(activeCount >= 2, "Should have at least 2 active employees");
```

## Cache Behavior Assertions

### Cache Hit/Miss

```java
// Verify cache miss on first call
DepartmentEntity dept = DepartmentTestFixture.createDepartment();
departmentDao.insert(dept);
Long deptId = dept.getDepartmentId();

// First call - cache miss (should query database)
DepartmentEntity result1 = departmentCacheManager.queryDepartment(deptId);
assertNotNull(result1);

// Verify cache populated
Cache cache = cacheManager.getCache("department");
assertNotNull(cache, "Department cache should exist");
Cache.ValueWrapper cached = cache.get(deptId);
assertNotNull(cached, "Department should be cached");

// Second call - cache hit (should NOT query database)
DepartmentEntity result2 = departmentCacheManager.queryDepartment(deptId);
assertEquals(result1.getDepartmentName(), result2.getDepartmentName(),
    "Should return same data from cache");
```

### Cache Eviction

```java
// Populate cache
DepartmentEntity dept = DepartmentTestFixture.createDepartment();
departmentDao.insert(dept);
departmentCacheManager.queryDepartment(dept.getDepartmentId()); // Cache it

// Verify cached
Cache cache = cacheManager.getCache("department");
assertNotNull(cache.get(dept.getDepartmentId()));

// Update department (should evict cache)
dept.setDepartmentName("Updated Name");
departmentCacheManager.updateDepartment(dept);

// Verify cache evicted
assertNull(cache.get(dept.getDepartmentId()),
    "Cache should be evicted after update");

// Query again - should hit database and re-cache
DepartmentEntity updated = departmentCacheManager.queryDepartment(dept.getDepartmentId());
assertEquals("Updated Name", updated.getDepartmentName());
assertNotNull(cache.get(dept.getDepartmentId()), "Should be re-cached");
```

### Cache Key Assertions

```java
// Verify cache key format
Long deptId = 123L;
departmentCacheManager.queryDepartment(deptId);

Cache cache = cacheManager.getCache("department");
assertNotNull(cache);

// Check cache key (depends on caching strategy)
Cache.ValueWrapper cached = cache.get(deptId);
assertNotNull(cached, "Should find cached value with key: " + deptId);
```

## Transaction Assertions

### Rollback on Exception

```java
@Test
void saveEmployee_DaoFailure_RollsBackTransaction() {
    // Get count before
    long countBefore = employeeDao.selectCount(null);

    // Create employee with invalid FK (should fail)
    EmployeeEntity employee = EmployeeTestFixture.createEntity(99999L); // Non-existent dept
    List<Long> roleIds = List.of(1L, 2L);

    // Should throw exception and rollback
    assertThrows(Exception.class, () -> {
        employeeManager.saveEmployee(employee, roleIds);
    });

    // Verify rollback - count unchanged
    long countAfter = employeeDao.selectCount(null);
    assertEquals(countBefore, countAfter,
        "Employee count should be unchanged after rollback");

    // Verify no roles inserted
    List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
        Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId())
    );
    assertTrue(roles.isEmpty(), "No roles should be inserted after rollback");
}
```

### Multi-DAO Coordination

```java
@Test
void updateEmployee_UpdatesEmployeeAndRoles_InSameTransaction() {
    // Setup - Create employee with roles
    EmployeeEntity employee = EmployeeTestFixture.createEntity();
    employeeDao.insert(employee);
    employeeManager.updateEmployeeRole(employee.getEmployeeId(), List.of(1L, 2L));

    // Update - Change employee data and roles
    EmployeeUpdateForm form = EmployeeTestFixture.createUpdateForm(employee.getEmployeeId());
    form.setActualName("Updated Name");
    List<Long> newRoleIds = List.of(3L, 4L, 5L);

    employeeManager.updateEmployee(employee, newRoleIds);

    // Verify both updates succeeded
    EmployeeEntity updated = employeeDao.selectById(employee.getEmployeeId());
    assertEquals("Updated Name", updated.getActualName());

    List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
        Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId())
    );
    assertEquals(3, roles.size(), "Should have 3 new roles");
}
```

## MockMvc Assertions

### Status Code

```java
mockMvc.perform(
    post("/employee/query")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"pageNum\":1,\"pageSize\":10}")
).andExpect(status().isOk());

// Other status codes
.andExpect(status().isBadRequest());
.andExpect(status().isUnauthorized());
.andExpect(status().isForbidden());
.andExpect(status().isNotFound());
```

### Response Body

```java
MvcResult result = mockMvc.perform(
    post("/employee/query")
        .contentType(MediaType.APPLICATION_JSON)
        .content(JsonUtil.toJsonString(queryForm))
).andExpect(status().isOk()).andReturn();

// Parse ResponseDTO
String json = result.getResponse().getContentAsString();
ResponseDTO<PageResult<EmployeeVO>> response =
    JsonUtil.parseObject(json, new TypeReference<>() {});

// Assert ResponseDTO
assertTrue(response.getOk());
assertNotNull(response.getData());
```

### JSON Path Assertions

```java
mockMvc.perform(
    post("/employee/query")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"pageNum\":1,\"pageSize\":10}")
)
.andExpect(status().isOk())
.andExpect(jsonPath("$.ok").value(true))
.andExpect(jsonPath("$.data.total").exists())
.andExpect(jsonPath("$.data.list").isArray())
.andExpect(jsonPath("$.data.list.length()").value(greaterThanOrEqualTo(0)));
```

### Header Assertions

```java
// Verify authentication header required
mockMvc.perform(
    post("/employee/query")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}")
).andExpect(status().isUnauthorized());

// With auth token
mockMvc.perform(
    post("/employee/query")
        .header("x-access-token", authToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}")
).andExpect(status().isOk());
```

## Custom Assertion Helpers

### Domain-Specific Assertions

```java
/**
 * Assert employee entity matches add form
 */
protected void assertEmployeeMatchesAddForm(EmployeeEntity entity, EmployeeAddForm form) {
    assertEquals(form.getLoginName(), entity.getLoginName());
    assertEquals(form.getActualName(), entity.getActualName());
    assertEquals(form.getPhone(), entity.getPhone());
    assertEquals(form.getDepartmentId(), entity.getDepartmentId());
    assertEquals(form.getGender(), entity.getGender());
}

/**
 * Assert employee VO matches entity
 */
protected void assertEmployeeVOMatchesEntity(EmployeeVO vo, EmployeeEntity entity) {
    assertEquals(entity.getEmployeeId(), vo.getEmployeeId());
    assertEquals(entity.getLoginName(), vo.getLoginName());
    assertEquals(entity.getActualName(), vo.getActualName());
    assertEquals(entity.getPhone(), vo.getPhone());
    assertEquals(entity.getDepartmentId(), vo.getDepartmentId());
}

/**
 * Assert page result is valid
 */
protected <T> void assertValidPageResult(PageResult<T> page, int expectedMinTotal) {
    assertNotNull(page);
    assertNotNull(page.getList());
    assertNotNull(page.getTotal());
    assertTrue(page.getTotal() >= expectedMinTotal);
    assertTrue(page.getList().size() <= page.getPageSize());
}
```

### Soft Assertions (AssertJ)

```java
// Multiple assertions that all execute
SoftAssertions softly = new SoftAssertions();
softly.assertThat(employee.getLoginName()).isEqualTo(form.getLoginName());
softly.assertThat(employee.getActualName()).isEqualTo(form.getActualName());
softly.assertThat(employee.getPhone()).isEqualTo(form.getPhone());
softly.assertThat(employee.getDepartmentId()).isEqualTo(form.getDepartmentId());
softly.assertAll(); // Throws if any assertion failed
```

## Best Practices

1. **Always verify both ResponseDTO and database state** - Don't trust ResponseDTO alone
2. **Use descriptive assertion messages** - Include context when assertion fails
3. **Check null values explicitly** - Don't assume non-null
4. **Verify side effects** - Check cache updates, relationship changes
5. **Use helper methods** - Extract common assertions into helper methods
6. **Test negative cases** - Verify error responses work correctly
7. **Check all PageResult fields** - Verify total, pageNum, pageSize, list
8. **Use AssertJ for complex assertions** - Cleaner syntax for collections and conditions
