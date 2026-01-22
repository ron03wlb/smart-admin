package net.lab1024.sa.admin.module.system.login.manager;

import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.base.core.domain.UserPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LoginManager Cache Integration Test
 *
 * Demonstrates:
 * - Testing JetCache @Cached annotations
 * - Testing @CacheUpdate and @CacheInvalidate
 * - Using Testcontainers Redis for real cache behavior
 * - Using @SpyBean to count DAO invocations
 *
 * Challenge #3: Testing Cache with JetCache
 *
 * @author SmartAdmin Testing Guide
 * @see <a href="../integration/caching-testing.md">Caching Testing Guide</a>
 */
@SpringBootTest
@Testcontainers
@DisplayName("LoginManager Cache Tests")
class LoginManagerCacheIntTest {

    /**
     * Redis container for cache testing
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);  // Reuse container across test runs

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private LoginManager loginManager;

    /**
     * SpyBean allows us to count method invocations
     * while keeping real implementation
     */
    @SpyBean
    private EmployeeDao employeeDao;

    @SpyBean
    private RoleEmployeeDao roleEmployeeDao;

    @SpyBean
    private RoleMenuDao roleMenuDao;

    @SpyBean
    private DepartmentDao departmentDao;

    @BeforeEach
    void setUp() {
        // Clear invocation counts before each test
        clearInvocations(employeeDao, roleEmployeeDao, roleMenuDao, departmentDao);
    }

    // ========================================
    // Test: @Cached - getRequestEmployee()
    // ========================================

    @Nested
    @DisplayName("@Cached - getRequestEmployee()")
    class GetRequestEmployeeCachingTests {

        @Test
        @DisplayName("First call - loads from database (cache miss)")
        void testGetRequestEmployee_FirstCall_HitsDatabase() {
            // Given
            Long employeeId = 1L;

            // When - first call (cache miss)
            RequestEmployee result = loginManager.getRequestEmployee(employeeId);

            // Then
            assertNotNull(result, "Should return employee data");
            assertEquals(employeeId, result.getEmployeeId());

            // Verify DAO was called (cache miss)
            verify(employeeDao, times(1)).selectById(employeeId);
        }

        @Test
        @DisplayName("Second call - uses cache (cache hit)")
        void testGetRequestEmployee_SecondCall_UsesCache() {
            // Given
            Long employeeId = 1L;

            // Warm up cache with first call
            RequestEmployee first = loginManager.getRequestEmployee(employeeId);
            assertNotNull(first);

            // Clear invocation counter
            clearInvocations(employeeDao);

            // When - second call (cache hit)
            RequestEmployee second = loginManager.getRequestEmployee(employeeId);

            // Then
            assertNotNull(second);
            assertEquals(first.getEmployeeId(), second.getEmployeeId());

            // Verify DAO NOT called (cache hit)
            verify(employeeDao, never()).selectById(employeeId);
        }

        @Test
        @DisplayName("Multiple calls - cache is efficient")
        void testGetRequestEmployee_MultipleCalls_CacheEfficient() {
            // Given
            Long employeeId = 1L;

            clearInvocations(employeeDao);

            // When - call 10 times
            for (int i = 0; i < 10; i++) {
                RequestEmployee result = loginManager.getRequestEmployee(employeeId);
                assertNotNull(result, "Iteration " + i + " should return data");
            }

            // Then - DAO called only once (first call)
            verify(employeeDao, times(1)).selectById(employeeId);
        }

        @Test
        @DisplayName("Different employee IDs - separate cache entries")
        void testGetRequestEmployee_DifferentIds_SeparateCacheEntries() {
            // Given
            Long employeeId1 = 1L;
            Long employeeId2 = 2L;

            clearInvocations(employeeDao);

            // When
            RequestEmployee emp1 = loginManager.getRequestEmployee(employeeId1);
            RequestEmployee emp2 = loginManager.getRequestEmployee(employeeId2);

            // Then - each ID loads from database (separate cache keys)
            assertNotNull(emp1);
            assertNotNull(emp2);
            assertNotEquals(emp1.getEmployeeId(), emp2.getEmployeeId());

            verify(employeeDao, times(1)).selectById(employeeId1);
            verify(employeeDao, times(1)).selectById(employeeId2);
        }

        @Test
        @DisplayName("Null employee ID - not cached")
        void testGetRequestEmployee_NullId_NotCached() {
            // When
            RequestEmployee result = loginManager.getRequestEmployee(null);

            // Then
            assertNull(result, "Null ID should return null");

            // Verify DAO not called for null ID
            verify(employeeDao, never()).selectById(any());
        }
    }

    // ========================================
    // Test: @Cached - getUserPermission()
    // ========================================

    @Nested
    @DisplayName("@Cached - getUserPermission()")
    class GetUserPermissionCachingTests {

        @Test
        @DisplayName("First call - loads from database")
        void testGetUserPermission_FirstCall_HitsDatabase() {
            // Given
            Long employeeId = 1L;

            clearInvocations(roleEmployeeDao, roleMenuDao);

            // When
            UserPermission result = loginManager.getUserPermission(employeeId);

            // Then
            assertNotNull(result);
            assertNotNull(result.getPermissionList());
            assertNotNull(result.getRoleList());

            // Verify DAOs called to build permission
            verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(employeeId);
            // roleMenuDao called based on roles
        }

        @Test
        @DisplayName("Second call - uses cache")
        void testGetUserPermission_SecondCall_UsesCache() {
            // Given
            Long employeeId = 1L;

            // Warm up cache
            UserPermission first = loginManager.getUserPermission(employeeId);
            assertNotNull(first);

            clearInvocations(roleEmployeeDao, roleMenuDao);

            // When - second call
            UserPermission second = loginManager.getUserPermission(employeeId);

            // Then
            assertNotNull(second);
            assertEquals(first.getPermissionList().size(), second.getPermissionList().size());

            // Verify DAOs NOT called (cache hit)
            verify(roleEmployeeDao, never()).selectRoleByEmployeeId(employeeId);
            verify(roleMenuDao, never()).selectMenuListByRoleIdList(any(), anyBoolean());
        }
    }

    // ========================================
    // Test: @CacheInvalidate
    // ========================================

    @Nested
    @DisplayName("@CacheInvalidate - clearUserLoginInfo()")
    class ClearUserLoginInfoTests {

        @Test
        @DisplayName("After clearUserLoginInfo - cache invalidated")
        void testClearUserLoginInfo_InvalidatesCache() {
            // Given
            Long employeeId = 1L;

            // Warm up cache
            RequestEmployee initial = loginManager.getRequestEmployee(employeeId);
            assertNotNull(initial);

            clearInvocations(employeeDao);

            // When - clear cache
            loginManager.clearUserLoginInfo(employeeId);

            // Then - next call should hit database again
            RequestEmployee reloaded = loginManager.getRequestEmployee(employeeId);
            assertNotNull(reloaded);

            // Verify DAO called again (cache was invalidated)
            verify(employeeDao, times(1)).selectById(employeeId);
        }
    }

    @Nested
    @DisplayName("@CacheInvalidate - clearUserPermission()")
    class ClearUserPermissionTests {

        @Test
        @DisplayName("After clearUserPermission - permission cache invalidated")
        void testClearUserPermission_InvalidatesPermissionCache() {
            // Given
            Long employeeId = 1L;

            // Warm up permission cache
            UserPermission initial = loginManager.getUserPermission(employeeId);
            assertNotNull(initial);

            clearInvocations(roleEmployeeDao, roleMenuDao);

            // When - clear permission cache
            loginManager.clearUserPermission(employeeId);

            // Then - next call should reload from database
            UserPermission reloaded = loginManager.getUserPermission(employeeId);
            assertNotNull(reloaded);

            // Verify DAOs called again
            verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(employeeId);
        }
    }

    // ========================================
    // Test: @CacheUpdate - loadLoginInfo()
    // ========================================

    @Nested
    @DisplayName("@CacheUpdate - loadLoginInfo()")
    class LoadLoginInfoCacheUpdateTests {

        @Test
        @DisplayName("loadLoginInfo - updates cache with new data")
        void testLoadLoginInfo_UpdatesCache() {
            // Given
            Long employeeId = 1L;

            // Get initial cached data
            RequestEmployee initial = loginManager.getRequestEmployee(employeeId);
            assertNotNull(initial);

            // Create updated employee entity
            EmployeeEntity updatedEntity = new EmployeeEntity();
            updatedEntity.setEmployeeId(employeeId);
            updatedEntity.setActualName("Updated Name Via CacheUpdate");
            updatedEntity.setDepartmentId(1L);

            // Mock DAO to return updated entity
            when(employeeDao.selectById(employeeId)).thenReturn(updatedEntity);

            clearInvocations(employeeDao);

            // When - call @CacheUpdate method
            RequestEmployee updated = loginManager.loadLoginInfo(updatedEntity);
            assertNotNull(updated);
            assertEquals("Updated Name Via CacheUpdate", updated.getActualName());

            clearInvocations(employeeDao);

            // Then - getRequestEmployee should return updated data from cache
            RequestEmployee fromCache = loginManager.getRequestEmployee(employeeId);

            // Verify DAO NOT called (using updated cache)
            verify(employeeDao, never()).selectById(employeeId);

            // Verify cached data was updated
            assertEquals("Updated Name Via CacheUpdate", fromCache.getActualName());
        }
    }

    @Nested
    @DisplayName("@CacheUpdate - loadUserPermission()")
    class LoadUserPermissionCacheUpdateTests {

        @Test
        @DisplayName("loadUserPermission - updates permission cache")
        void testLoadUserPermission_UpdatesCache() {
            // Given
            Long employeeId = 1L;

            // Get initial permissions
            UserPermission initial = loginManager.getUserPermission(employeeId);
            assertNotNull(initial);

            clearInvocations(roleEmployeeDao, roleMenuDao);

            // When - call @CacheUpdate method
            UserPermission updated = loginManager.loadUserPermission(employeeId);
            assertNotNull(updated);

            clearInvocations(roleEmployeeDao, roleMenuDao);

            // Then - getUserPermission should use updated cache
            UserPermission fromCache = loginManager.getUserPermission(employeeId);
            assertNotNull(fromCache);

            // Verify DAOs NOT called (cache was updated)
            verify(roleEmployeeDao, never()).selectRoleByEmployeeId(employeeId);
        }
    }

    // ========================================
    // Test: Cache Performance
    // ========================================

    @Nested
    @DisplayName("Cache Performance")
    class CachePerformanceTests {

        @Test
        @DisplayName("Cache reduces response time")
        void testCache_ReducesResponseTime() {
            // Given
            Long employeeId = 1L;

            // When - measure first call (cold cache)
            long start1 = System.currentTimeMillis();
            RequestEmployee result1 = loginManager.getRequestEmployee(employeeId);
            long duration1 = System.currentTimeMillis() - start1;

            assertNotNull(result1);

            // Measure second call (warm cache)
            long start2 = System.currentTimeMillis();
            RequestEmployee result2 = loginManager.getRequestEmployee(employeeId);
            long duration2 = System.currentTimeMillis() - start2;

            assertNotNull(result2);

            // Then - cached call should be faster
            // Note: This is a soft assertion as timing can vary
            System.out.println("First call (cache miss): " + duration1 + "ms");
            System.out.println("Second call (cache hit): " + duration2 + "ms");

            // Generally, cache hit should be faster, but we don't assert hard timing
            // as test execution speed can vary
        }
    }
}
