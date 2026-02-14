# JetCache Testing Guide

> **Challenge #3**: Testing @Cached, @CacheUpdate, and @CacheInvalidate in Manager layer
> **Last Updated**: 2026-01-22

---

## Table of Contents

1. [Introduction](#introduction)
2. [JetCache in SmartAdmin](#jetcache-in-smartadmin)
3. [Unit Test Strategy](#unit-test-strategy)
4. [Integration Test Strategy](#integration-test-strategy)
5. [Real SmartAdmin Examples](#real-smartadmin-examples)
6. [Verification Patterns](#verification-patterns)
7. [Common Issues](#common-issues)

---

## Introduction

SmartAdmin uses **JetCache** for caching with both local and remote (Redis) cache layers:

- `@Cached` - Cache method result
- `@CacheUpdate` - Update cache entry
- `@CacheInvalidate` - Remove cache entry

This guide shows how to test caching behavior effectively.

---

## JetCache in SmartAdmin

### Cache Architecture

```
Request → LoginManager.getRequestEmployee(employeeId)
            ↓
    Check Cache (CacheType.BOTH)
    ├─ Local Cache (Caffeine) - 30 min TTL
    └─ Remote Cache (Redis) - 120 min TTL
            ↓
    Cache Miss → Load from Database
            ↓
    Store in Cache (local + remote)
            ↓
    Return Result
```

### Cache Types

```java
@Cached(
    name = "REQUEST_EMPLOYEE",
    key = "#employeeId",
    cacheType = CacheType.BOTH,      // Local + Remote
    localExpire = 30,                 // 30 minutes local
    expire = 120,                     // 120 minutes remote
    timeUnit = TimeUnit.MINUTES
)
public RequestEmployee getRequestEmployee(Long employeeId) {
    // Load from database
}
```

**CacheType Options:**
- `CacheType.LOCAL` - Caffeine cache only (in-memory)
- `CacheType.REMOTE` - Redis only
- `CacheType.BOTH` - Caffeine + Redis (2-level cache)

---

## Unit Test Strategy

### Option 1: Mock the Manager Method

For Service layer tests, mock the cached Manager method:

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService employeeService;

    @Mock
    private LoginManager loginManager;  // Mock the cached Manager

    @Test
    void testGetEmployeeInfo_UsesCachedData() {
        // Given
        Long employeeId = 1L;
        RequestEmployee cachedEmployee = new RequestEmployee();
        cachedEmployee.setEmployeeId(employeeId);
        cachedEmployee.setActualName("Cached User");

        when(loginManager.getRequestEmployee(employeeId))
            .thenReturn(cachedEmployee);

        // When
        RequestEmployee result = employeeService.getEmployeeInfo(employeeId);

        // Then
        assertNotNull(result);
        assertEquals("Cached User", result.getActualName());

        // Verify called only once (simulates cache behavior)
        verify(loginManager, times(1)).getRequestEmployee(employeeId);
    }
}
```

### Option 2: Don't Test JetCache Directly in Unit Tests

**Recommendation**: Don't test JetCache annotations in unit tests.

**Rationale:**
- JetCache is infrastructure concern
- Unit tests should focus on business logic
- Cache behavior tested in integration tests

---

## Integration Test Strategy

### Step 1: Setup Testcontainers Redis

```java
@SpringBootTest
@Testcontainers
class LoginManagerCacheIntTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);  // Reuse container across tests

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private LoginManager loginManager;

    @SpyBean  // Spy to count DAO method invocations
    private EmployeeDao employeeDao;

    // Tests here
}
```

### Step 2: Test Cache Hit/Miss

**Verify cache MISS (first call hits database):**

```java
@Test
@DisplayName("getRequestEmployee - first call - hits database")
void testGetRequestEmployee_FirstCall_HitsDatabase() {
    // Given
    Long employeeId = 1L;
    clearInvocations(employeeDao);  // Reset spy

    // When - first call (cache miss)
    RequestEmployee result = loginManager.getRequestEmployee(employeeId);

    // Then
    assertNotNull(result);
    assertEquals(employeeId, result.getEmployeeId());

    // Verify DAO was called (cache miss)
    verify(employeeDao, times(1)).selectById(employeeId);
}
```

**Verify cache HIT (second call uses cache):**

```java
@Test
@DisplayName("getRequestEmployee - second call - uses cache")
void testGetRequestEmployee_SecondCall_UsesCache() {
    // Given
    Long employeeId = 1L;

    // First call to warm up cache
    loginManager.getRequestEmployee(employeeId);
    clearInvocations(employeeDao);

    // When - second call (cache hit)
    RequestEmployee result = loginManager.getRequestEmployee(employeeId);

    // Then
    assertNotNull(result);

    // Verify DAO NOT called (cache hit)
    verify(employeeDao, never()).selectById(employeeId);
}
```

### Step 3: Test @CacheInvalidate

```java
@Test
@DisplayName("clearUserLoginInfo - invalidates cache")
void testClearUserLoginInfo_InvalidatesCache() {
    // Given
    Long employeeId = 1L;

    // Warm up cache
    loginManager.getRequestEmployee(employeeId);
    clearInvocations(employeeDao);

    // When - clear cache
    loginManager.clearUserLoginInfo(employeeId);

    // Then - next call should hit database again
    loginManager.getRequestEmployee(employeeId);
    verify(employeeDao, times(1)).selectById(employeeId);
}
```

### Step 4: Test @CacheUpdate

```java
@Test
@DisplayName("loadLoginInfo - updates cache")
void testLoadLoginInfo_UpdatesCache() {
    // Given
    EmployeeEntity employee = new EmployeeEntity();
    employee.setEmployeeId(1L);
    employee.setActualName("Updated Name");
    employee.setDepartmentId(1L);

    when(employeeDao.selectById(1L)).thenReturn(employee);

    // When - call @CacheUpdate method
    RequestEmployee updated = loginManager.loadLoginInfo(employee);
    clearInvocations(employeeDao);

    // Then - getRequestEmployee should use updated cache
    RequestEmployee cached = loginManager.getRequestEmployee(1L);

    // DAO not called (cache was updated)
    verify(employeeDao, never()).selectById(1L);

    // Verify cached data matches updated data
    assertEquals("Updated Name", cached.getActualName());
}
```

---

## Real SmartAdmin Examples

### Example 1: LoginManager.getRequestEmployee()

**Real Implementation:**

```java
@Cached(
    name = CacheKeyConst.Login.REQUEST_EMPLOYEE,
    key = "#requestEmployeeId",
    cacheType = CacheType.BOTH,
    localExpire = 30,
    expire = 120,
    timeUnit = TimeUnit.MINUTES
)
public RequestEmployee getRequestEmployee(Long requestEmployeeId) {
    if (requestEmployeeId == null) {
        return null;
    }

    EmployeeEntity employeeEntity = employeeDao.selectById(requestEmployeeId);
    if (employeeEntity == null) {
        return null;
    }

    return this.loadLoginInfo(employeeEntity);
}
```

**Complete Test:**

```java
@SpringBootTest
@Testcontainers
class LoginManagerGetRequestEmployeeTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private LoginManager loginManager;

    @SpyBean
    private EmployeeDao employeeDao;

    @Nested
    @DisplayName("getRequestEmployee Caching")
    class GetRequestEmployeeCachingTests {

        @Test
        @DisplayName("First call - loads from database")
        void testFirstCall_LoadsFromDatabase() {
            // Given
            Long employeeId = 1L;
            clearInvocations(employeeDao);

            // When
            RequestEmployee result = loginManager.getRequestEmployee(employeeId);

            // Then
            assertNotNull(result);
            verify(employeeDao, times(1)).selectById(employeeId);
        }

        @Test
        @DisplayName("Second call - uses cache")
        void testSecondCall_UsesCache() {
            // Given
            Long employeeId = 1L;
            loginManager.getRequestEmployee(employeeId);  // Warm up
            clearInvocations(employeeDao);

            // When
            RequestEmployee result = loginManager.getRequestEmployee(employeeId);

            // Then
            assertNotNull(result);
            verify(employeeDao, never()).selectById(employeeId);
        }

        @Test
        @DisplayName("Multiple calls - cache is efficient")
        void testMultipleCalls_CacheEfficient() {
            // Given
            Long employeeId = 1L;
            clearInvocations(employeeDao);

            // When - call 10 times
            for (int i = 0; i < 10; i++) {
                RequestEmployee result = loginManager.getRequestEmployee(employeeId);
                assertNotNull(result);
            }

            // Then - DAO called only once
            verify(employeeDao, times(1)).selectById(employeeId);
        }

        @Test
        @DisplayName("Null employee ID - not cached")
        void testNullEmployeeId_NotCached() {
            // When
            RequestEmployee result = loginManager.getRequestEmployee(null);

            // Then
            assertNull(result);
            verify(employeeDao, never()).selectById(any());
        }

        @Test
        @DisplayName("Non-existent employee - null result not cached")
        void testNonExistentEmployee_NullNotCached() {
            // Given
            Long nonExistentId = 999999L;
            when(employeeDao.selectById(nonExistentId)).thenReturn(null);

            // When - first call
            RequestEmployee result1 = loginManager.getRequestEmployee(nonExistentId);
            assertNull(result1);

            clearInvocations(employeeDao);

            // Second call
            RequestEmployee result2 = loginManager.getRequestEmployee(nonExistentId);

            // Then - should hit database again (null not cached by default)
            verify(employeeDao, times(1)).selectById(nonExistentId);
            assertNull(result2);
        }
    }
}
```

### Example 2: LoginManager.getUserPermission()

**Real Implementation:**

```java
@Cached(
    name = CacheKeyConst.Login.USER_PERMISSION,
    key = "#employeeId",
    cacheType = CacheType.BOTH,
    localExpire = 30,
    expire = 120,
    timeUnit = TimeUnit.MINUTES
)
public UserPermission getUserPermission(Long employeeId) {
    if (null == employeeId) {
        return null;
    }
    return this.loadUserPermission(employeeId);
}
```

**Test:**

```java
@Test
@DisplayName("getUserPermission - caches permission data")
void testGetUserPermission_CachesData() {
    // Given
    Long employeeId = 1L;

    // When - first call
    UserPermission permission1 = loginManager.getUserPermission(employeeId);
    clearInvocations(roleEmployeeDao);

    // Second call
    UserPermission permission2 = loginManager.getUserPermission(employeeId);

    // Then - DAO not called second time
    verify(roleEmployeeDao, never()).selectRoleByEmployeeId(employeeId);

    // Verify same permissions returned
    assertEquals(permission1.getPermissionList(), permission2.getPermissionList());
}
```

### Example 3: LoginManager.clearUserPermission()

**Real Implementation:**

```java
@CacheInvalidate(name = CacheKeyConst.Login.USER_PERMISSION, key = "#employeeId")
public void clearUserPermission(Long employeeId) {
    // Method body can be empty - annotation handles cache invalidation
}
```

**Test:**

```java
@Test
@DisplayName("clearUserPermission - invalidates permission cache")
void testClearUserPermission_InvalidatesCache() {
    // Given
    Long employeeId = 1L;

    // Warm up cache
    UserPermission original = loginManager.getUserPermission(employeeId);
    assertNotNull(original);
    clearInvocations(roleEmployeeDao, roleMenuDao);

    // When - clear permission cache
    loginManager.clearUserPermission(employeeId);

    // Then - next call loads from database again
    UserPermission reloaded = loginManager.getUserPermission(employeeId);

    verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(employeeId);
}
```

### Example 4: LoginManager.loadLoginInfo()

**Real Implementation:**

```java
@CacheUpdate(
    name = CacheKeyConst.Login.REQUEST_EMPLOYEE,
    key = "#employeeEntity.employeeId",
    value = "#result"
)
public RequestEmployee loadLoginInfo(EmployeeEntity employeeEntity) {
    // Build RequestEmployee from EmployeeEntity
    RequestEmployee requestEmployee = SmartBeanUtil.copy(employeeEntity, RequestEmployee.class);
    // ... additional logic
    return requestEmployee;
}
```

**Test:**

```java
@Test
@DisplayName("loadLoginInfo - updates cache with new data")
void testLoadLoginInfo_UpdatesCache() {
    // Given
    Long employeeId = 1L;

    // First, get initial cached data
    RequestEmployee initial = loginManager.getRequestEmployee(employeeId);
    assertNotNull(initial);

    // Create updated employee entity
    EmployeeEntity updatedEntity = new EmployeeEntity();
    updatedEntity.setEmployeeId(employeeId);
    updatedEntity.setActualName("Updated Name Via CacheUpdate");
    updatedEntity.setDepartmentId(1L);

    // Mock DAO to return updated entity
    when(employeeDao.selectById(employeeId)).thenReturn(updatedEntity);

    // When - call @CacheUpdate method
    RequestEmployee updated = loginManager.loadLoginInfo(updatedEntity);
    clearInvocations(employeeDao);

    // Then - getRequestEmployee should return updated data from cache
    RequestEmployee fromCache = loginManager.getRequestEmployee(employeeId);

    // DAO not called (using updated cache)
    verify(employeeDao, never()).selectById(employeeId);

    // Verify updated data
    assertEquals("Updated Name Via CacheUpdate", fromCache.getActualName());
}
```

---

## Verification Patterns

### Pattern 1: Count DAO Invocations with @SpyBean

```java
@SpyBean
private EmployeeDao employeeDao;

@Test
void testCacheBehavior() {
    // First call
    loginManager.getRequestEmployee(1L);
    verify(employeeDao, times(1)).selectById(1L);

    clearInvocations(employeeDao);

    // Second call - cache hit
    loginManager.getRequestEmployee(1L);
    verify(employeeDao, never()).selectById(1L);  // Not called!
}
```

### Pattern 2: Measure Response Time (Cache vs No Cache)

```java
@Test
void testCachePerformance() {
    Long employeeId = 1L;

    // First call (cold cache) - slower
    long start1 = System.currentTimeMillis();
    loginManager.getRequestEmployee(employeeId);
    long duration1 = System.currentTimeMillis() - start1;

    // Second call (warm cache) - faster
    long start2 = System.currentTimeMillis();
    loginManager.getRequestEmployee(employeeId);
    long duration2 = System.currentTimeMillis() - start2;

    // Cache should be faster
    assertTrue(duration2 < duration1,
        "Cached call should be faster than database call");
}
```

### Pattern 3: Verify Cache Key Generation

```java
@Test
@DisplayName("Different employee IDs - separate cache entries")
void testDifferentIds_SeparateCacheEntries() {
    // Given
    Long employeeId1 = 1L;
    Long employeeId2 = 2L;

    clearInvocations(employeeDao);

    // When
    loginManager.getRequestEmployee(employeeId1);
    loginManager.getRequestEmployee(employeeId2);

    // Then - each ID loads from database (separate cache keys)
    verify(employeeDao, times(1)).selectById(employeeId1);
    verify(employeeDao, times(1)).selectById(employeeId2);
}
```

### Pattern 4: Verify CacheType.BOTH (Local + Remote)

```java
@Test
@DisplayName("CacheType.BOTH - uses local then remote cache")
void testCacheTypeBoth() {
    // This test is implementation-specific
    // Usually, you trust JetCache's CacheType.BOTH behavior

    // Instead, verify that cache works correctly
    Long employeeId = 1L;

    // First call
    loginManager.getRequestEmployee(employeeId);

    // Second call - should use cache (local or remote)
    clearInvocations(employeeDao);
    loginManager.getRequestEmployee(employeeId);
    verify(employeeDao, never()).selectById(employeeId);
}
```

---

## Common Issues

### Issue 1: Cache Not Working in Tests

**Symptoms**: DAO called every time, even on repeated calls

**Possible Causes:**

1. **Redis not running**

```java
// Solution: Check Testcontainers setup
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);

@DynamicPropertySource
static void configureRedis(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
}
```

2. **JetCache not enabled in test profile**

```yaml
# src/test/resources/application.yaml
jetcache:
  statIntervalMinutes: 15
  areaInCacheName: false
  local:
    default:
      type: caffeine
      limit: 1000
  remote:
    default:
      type: redis
      keyConvertor: fastjson2
```

3. **Cache keys don't match**

```java
// Verify cache key expression
@Cached(key = "#employeeId")  // Must match method parameter name
public RequestEmployee getRequestEmployee(Long employeeId) { }
```

### Issue 2: @SpyBean Doesn't Show Cache Behavior

**Problem**: @SpyBean creates proxy, cache doesn't intercept

**Solution**: Use @SpyBean correctly with Spring:

```java
@SpringBootTest
class CacheIntTest {
    @SpyBean  // ✅ Spring-managed spy
    private EmployeeDao employeeDao;

    @Autowired  // ✅ Get real cached manager
    private LoginManager loginManager;

    // NOT @InjectMocks - that bypasses Spring!
}
```

### Issue 3: clearInvocations() Not Working

**Problem**: Spy invocation count keeps accumulating

**Solution**: Call `clearInvocations()` after warmup:

```java
@Test
void testCache() {
    // Warm up cache
    loginManager.getRequestEmployee(1L);

    // IMPORTANT: Clear invocation count
    clearInvocations(employeeDao);

    // Now test cache hit
    loginManager.getRequestEmployee(1L);
    verify(employeeDao, never()).selectById(1L);  // ✅ Correct count
}
```

### Issue 4: Cache Pollution Between Tests

**Problem**: Cache from one test affects another test

**Solution 1**: Clear cache in @BeforeEach

```java
@Autowired
private CacheManager cacheManager;

@BeforeEach
void clearCache() {
    cacheManager.getCache("REQUEST_EMPLOYEE").clear();
}
```

**Solution 2**: Use @DirtiesContext (slower)

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CacheIntTest {
    // Spring context rebuilt after each test
}
```

### Issue 5: Can't Test Local vs Remote Cache Separately

**Problem**: CacheType.BOTH uses both caches - can't isolate

**Solution**: Don't test implementation details

```java
// ❌ Bad - testing JetCache internals
@Test
void testLocalCacheUsedFirst() {
    // Can't reliably test this without JetCache internals
}

// ✅ Good - test observable behavior
@Test
void testCacheWorks() {
    // First call hits database
    loginManager.getRequestEmployee(1L);
    verify(employeeDao, times(1)).selectById(1L);

    clearInvocations(employeeDao);

    // Second call doesn't hit database
    loginManager.getRequestEmployee(1L);
    verify(employeeDao, never()).selectById(1L);
}
```

---

## Best Practices

### 1. Use @SpyBean, Not @Mock

```java
// ✅ Good - real DAO with spy
@SpyBean
private EmployeeDao employeeDao;

// ❌ Bad - mocked DAO won't show real behavior
@MockBean
private EmployeeDao employeeDao;
```

### 2. Test Cache Behavior, Not Cache Implementation

```java
// ✅ Good - test observable behavior
@Test
void testCache_ReducesDatabaseCalls() {
    // Multiple calls, single DB hit
}

// ❌ Bad - testing JetCache internals
@Test
void testCache_StoresInRedisWithCorrectTTL() {
    // Don't test framework internals
}
```

### 3. Clear Invocations After Warmup

```java
@Test
void testCacheHit() {
    loginManager.getRequestEmployee(1L);  // Warmup
    clearInvocations(employeeDao);         // ✅ Reset counter

    loginManager.getRequestEmployee(1L);  // Test
    verify(employeeDao, never()).selectById(1L);
}
```

### 4. Test All Cache Annotations

```java
@Nested
@DisplayName("Cache Operations")
class CacheOperationsTests {

    @Test
    void testCached_LoadsOnce() { }

    @Test
    void testCacheUpdate_UpdatesCache() { }

    @Test
    void testCacheInvalidate_ClearsCache() { }
}
```

---

## Summary

| Annotation | Purpose | How to Test |
|------------|---------|-------------|
| **@Cached** | Cache method result | Verify DAO called once, not on subsequent calls |
| **@CacheUpdate** | Update cache entry | Call method, verify next @Cached uses new data |
| **@CacheInvalidate** | Remove cache entry | Clear cache, verify next @Cached hits database |

**Key Testing Pattern**: Use `@SpyBean` to count DAO invocations and verify cache behavior.

---

## Related Documentation

- [Integration Testing Quick Reference](../integration-testing-quick-reference.md) - Quick cache patterns
- [Testing Strategy](../testing-strategy.md) - Overall testing philosophy
- [Manager Layer Rules](../../../.agent/rules/09-manager-layer.md) - Cache in Manager only

---

**Happy Caching Testing! 🧪**
