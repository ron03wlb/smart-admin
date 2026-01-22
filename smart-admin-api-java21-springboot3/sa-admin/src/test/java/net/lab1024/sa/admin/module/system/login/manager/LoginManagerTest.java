package net.lab1024.sa.admin.module.system.login.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.base.core.domain.UserPermission;
import net.lab1024.sa.base.module.support.file.service.IFileStorageService;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * LoginManager Unit Tests
 *
 * <p>Tests LoginManager business logic with mocked dependencies. While LoginManager uses @Cached
 * annotations in production, these unit tests focus on business logic verification without testing
 * actual cache behavior (which is handled by JetCache framework).
 *
 * <p><b>Testing Strategy:</b>
 *
 * <ul>
 *   <li>Pure unit tests with Mockito (no Spring context)
 *   <li>Mock all DAO and service dependencies
 *   <li>Test business logic: data loading, transformation, permission extraction
 *   <li>Cache annotations (@Cached, @CacheUpdate, @CacheInvalidate) are framework features tested
 *       in production
 * </ul>
 *
 * <p><b>Methods Tested:</b>
 *
 * <ol>
 *   <li>getRequestEmployee() - Loads employee with department and avatar info
 *   <li>loadLoginInfo() - Transforms EmployeeEntity to RequestEmployee with enrichments
 *   <li>getUserPermission() - Loads user roles and permissions from menus
 *   <li>loadUserPermission() - Builds UserPermission from roles and menus (administrator vs normal
 *       user logic)
 *   <li>clearUserPermission() - Cache invalidation (no-op in unit test)
 *   <li>clearUserLoginInfo() - Cache invalidation (no-op in unit test)
 * </ol>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@DisplayName("LoginManager Unit Tests")
class LoginManagerTest extends BaseUnitTest {

  @InjectMocks private LoginManager loginManager;

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentDao departmentDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private IFileStorageService fileStorageService;

  // Test data constants
  private static final Long TEST_EMPLOYEE_ID = 1001L;
  private static final String TEST_LOGIN_NAME = "test_employee";
  private static final String TEST_ACTUAL_NAME = "Test Employee";
  private static final String TEST_AVATAR_KEY = "avatar/test.png";
  private static final String TEST_AVATAR_URL = "https://cdn.example.com/avatar/test.png";
  private static final Long TEST_DEPARTMENT_ID = 100L;
  private static final String TEST_DEPARTMENT_NAME = "Test Department";

  private EmployeeEntity testEmployee;
  private DepartmentVO testDepartment;

  @BeforeEach
  void setUp() {
    // Create test employee
    testEmployee = new EmployeeEntity();
    testEmployee.setEmployeeId(TEST_EMPLOYEE_ID);
    testEmployee.setLoginName(TEST_LOGIN_NAME);
    testEmployee.setActualName(TEST_ACTUAL_NAME);
    testEmployee.setDepartmentId(TEST_DEPARTMENT_ID);
    testEmployee.setAvatar(TEST_AVATAR_KEY);
    testEmployee.setAdministratorFlag(false);
    testEmployee.setDisabledFlag(false);
    testEmployee.setDeletedFlag(false);

    // Create test department
    testDepartment = new DepartmentVO();
    testDepartment.setDepartmentId(TEST_DEPARTMENT_ID);
    testDepartment.setDepartmentName(TEST_DEPARTMENT_NAME);
  }

  @Nested
  @DisplayName("getRequestEmployee() Tests")
  class GetRequestEmployeeTests {

    @Test
    @DisplayName("Should load employee with department and avatar")
    void getRequestEmployee_ValidEmployee_LoadsCompleteInfo() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(fileStorageService.getFileUrl(TEST_AVATAR_KEY))
          .thenReturn(ResponseDTO.ok(TEST_AVATAR_URL));

      // When
      RequestEmployee result = loginManager.getRequestEmployee(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(TEST_EMPLOYEE_ID, result.getEmployeeId());
      assertEquals(TEST_LOGIN_NAME, result.getLoginName());
      assertEquals(TEST_ACTUAL_NAME, result.getActualName());
      assertEquals(TEST_DEPARTMENT_NAME, result.getDepartmentName());
      assertEquals(TEST_AVATAR_URL, result.getAvatar());
      assertEquals(UserTypeEnum.ADMIN_EMPLOYEE, result.getUserType());

      verify(employeeDao, times(1)).selectById(TEST_EMPLOYEE_ID);
      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should return null when employee ID is null")
    void getRequestEmployee_NullEmployeeId_ReturnsNull() {
      // When
      RequestEmployee result = loginManager.getRequestEmployee(null);

      // Then
      assertNull(result);
      verify(employeeDao, never()).selectById(any());
    }

    @Test
    @DisplayName("Should return null when employee does not exist")
    void getRequestEmployee_NonExistentEmployee_ReturnsNull() {
      // Given
      when(employeeDao.selectById(9999L)).thenReturn(null);

      // When
      RequestEmployee result = loginManager.getRequestEmployee(9999L);

      // Then
      assertNull(result);
      verify(employeeDao, times(1)).selectById(9999L);
    }

    @Test
    @DisplayName("Should delegate to loadLoginInfo() for data transformation")
    void getRequestEmployee_DelegatesToLoadLoginInfo() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(fileStorageService.getFileUrl(TEST_AVATAR_KEY))
          .thenReturn(ResponseDTO.ok(TEST_AVATAR_URL));

      // When
      RequestEmployee result = loginManager.getRequestEmployee(TEST_EMPLOYEE_ID);

      // Then - Verify loadLoginInfo() was called (by checking department and file service were
      // called)
      assertNotNull(result);
      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
      verify(fileStorageService, times(1)).getFileUrl(TEST_AVATAR_KEY);
    }
  }

  @Nested
  @DisplayName("loadLoginInfo() Tests")
  class LoadLoginInfoTests {

    @Test
    @DisplayName("Should load employee info with department and avatar")
    void loadLoginInfo_ValidEmployee_LoadsCompleteInfo() {
      // Given
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(fileStorageService.getFileUrl(TEST_AVATAR_KEY))
          .thenReturn(ResponseDTO.ok(TEST_AVATAR_URL));

      // When
      RequestEmployee result = loginManager.loadLoginInfo(testEmployee);

      // Then
      assertNotNull(result);
      assertEquals(TEST_EMPLOYEE_ID, result.getEmployeeId());
      assertEquals(TEST_LOGIN_NAME, result.getLoginName());
      assertEquals(TEST_ACTUAL_NAME, result.getActualName());
      assertEquals(TEST_DEPARTMENT_NAME, result.getDepartmentName());
      assertEquals(TEST_AVATAR_URL, result.getAvatar());
      assertEquals(UserTypeEnum.ADMIN_EMPLOYEE, result.getUserType());

      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
      verify(fileStorageService, times(1)).getFileUrl(TEST_AVATAR_KEY);
    }

    @Test
    @DisplayName("Should handle missing department gracefully (set empty string)")
    void loadLoginInfo_NoDepartment_SetsEmptyDepartmentName() {
      // Given
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(null);
      when(fileStorageService.getFileUrl(TEST_AVATAR_KEY))
          .thenReturn(ResponseDTO.ok(TEST_AVATAR_URL));

      // When
      RequestEmployee result = loginManager.loadLoginInfo(testEmployee);

      // Then
      assertNotNull(result);
      assertEquals("", result.getDepartmentName()); // Empty string for missing department

      verify(departmentDao, times(1)).selectDepartmentVO(TEST_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should handle missing avatar (null avatar key)")
    void loadLoginInfo_NoAvatar_SkipsFileService() {
      // Given
      testEmployee.setAvatar(null);
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      // When
      RequestEmployee result = loginManager.loadLoginInfo(testEmployee);

      // Then
      assertNotNull(result);
      assertNull(result.getAvatar()); // Avatar should remain null

      // File storage service should NOT be called
      verify(fileStorageService, never()).getFileUrl(anyString());
    }

    @Test
    @DisplayName("Should handle file storage service failure (avatar URL not returned)")
    void loadLoginInfo_FileServiceFails_AvatarRemainsAsKey() {
      // Given
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(fileStorageService.getFileUrl(TEST_AVATAR_KEY))
          .thenReturn(ResponseDTO.userErrorParam("File service failed")); // Simulate failure

      // When
      RequestEmployee result = loginManager.loadLoginInfo(testEmployee);

      // Then
      assertNotNull(result);
      // When file service fails, avatar remains as the key (not converted to URL)
      assertEquals(TEST_AVATAR_KEY, result.getAvatar());

      verify(fileStorageService, times(1)).getFileUrl(TEST_AVATAR_KEY);
    }

    @Test
    @DisplayName("Should handle blank avatar key (empty string)")
    void loadLoginInfo_BlankAvatar_SkipsFileService() {
      // Given
      testEmployee.setAvatar("   "); // Blank string
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      // When
      RequestEmployee result = loginManager.loadLoginInfo(testEmployee);

      // Then
      assertNotNull(result);

      // File storage service should NOT be called for blank string
      verify(fileStorageService, never()).getFileUrl(anyString());
    }
  }

  @Nested
  @DisplayName("getUserPermission() Tests")
  class GetUserPermissionTests {

    @Test
    @DisplayName("Should load permissions with roles and menus")
    void getUserPermission_ValidEmployee_LoadsPermissions() {
      // Given
      RoleVO testRole = new RoleVO();
      testRole.setRoleId(1L);
      testRole.setRoleCode("ADMIN");

      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(List.of(testRole));

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      MenuEntity menu = new MenuEntity();
      menu.setMenuId(1L);
      menu.setMenuName("Employee Management");
      menu.setPermsType(1);
      menu.setApiPerms("employee:add,employee:update");

      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), eq(false))).thenReturn(List.of(menu));

      // When
      UserPermission result = loginManager.getUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertNotNull(result.getRoleList());
      assertNotNull(result.getPermissionList());

      assertTrue(result.getRoleList().contains("ADMIN"));
      assertTrue(result.getPermissionList().contains("employee:add"));
      assertTrue(result.getPermissionList().contains("employee:update"));

      verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleMenuDao, times(1)).selectMenuListByRoleIdList(anyList(), eq(false));
    }

    @Test
    @DisplayName("Should return null when employee ID is null")
    void getUserPermission_NullEmployeeId_ReturnsNull() {
      // When
      UserPermission result = loginManager.getUserPermission(null);

      // Then
      assertNull(result);
      verify(roleEmployeeDao, never()).selectRoleByEmployeeId(any());
    }

    @Test
    @DisplayName("Should handle empty role list")
    void getUserPermission_NoRoles_ReturnsEmptyPermissions() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(new ArrayList<>());
      // Note: roleMenuDao is NOT called when employee has no roles (non-administrator)

      // When
      UserPermission result = loginManager.getUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertTrue(result.getRoleList().isEmpty());
      assertTrue(result.getPermissionList().isEmpty());
    }

    @Test
    @DisplayName("Should delegate to loadUserPermission() for permission building")
    void getUserPermission_DelegatesToLoadUserPermission() {
      // Given
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(new ArrayList<>());
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      // Note: roleMenuDao is NOT called when employee has no roles (non-administrator)

      // When
      UserPermission result = loginManager.getUserPermission(TEST_EMPLOYEE_ID);

      // Then - Verify loadUserPermission() was called (by checking DAO interactions)
      assertNotNull(result);
      verify(employeeDao, times(1)).selectById(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(TEST_EMPLOYEE_ID);
    }
  }

  @Nested
  @DisplayName("loadUserPermission() Tests - Administrator vs Normal Employee Logic")
  class LoadUserPermissionTests {

    @Test
    @DisplayName("Should load all menus for administrator (administratorFlag = true)")
    void loadUserPermission_Administrator_LoadsAllMenus() {
      // Given
      testEmployee.setAdministratorFlag(true);

      MenuEntity menu1 = new MenuEntity();
      menu1.setMenuId(1L);
      menu1.setPermsType(1);
      menu1.setApiPerms("employee:add,employee:delete");

      MenuEntity menu2 = new MenuEntity();
      menu2.setMenuId(2L);
      menu2.setPermsType(1);
      menu2.setApiPerms("department:view");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(new ArrayList<>());
      when(roleMenuDao.selectMenuListByRoleIdList(eq(List.of()), eq(false)))
          .thenReturn(List.of(menu1, menu2));

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(3, result.getPermissionList().size()); // 3 permissions
      assertTrue(result.getPermissionList().contains("employee:add"));
      assertTrue(result.getPermissionList().contains("employee:delete"));
      assertTrue(result.getPermissionList().contains("department:view"));

      // Verify called with empty roleIdList (administrator gets all menus)
      verify(roleMenuDao, times(1)).selectMenuListByRoleIdList(eq(List.of()), eq(false));
    }

    @Test
    @DisplayName("Should load role-based menus for normal employee")
    void loadUserPermission_NormalEmployee_LoadsRoleBasedMenus() {
      // Given
      testEmployee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("USER");

      MenuEntity menu = new MenuEntity();
      menu.setMenuId(1L);
      menu.setPermsType(1);
      menu.setApiPerms("employee:view");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(List.of(role));
      when(roleMenuDao.selectMenuListByRoleIdList(eq(List.of(1L)), eq(false)))
          .thenReturn(List.of(menu));

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(1, result.getRoleList().size());
      assertTrue(result.getRoleList().contains("USER"));
      assertTrue(result.getPermissionList().contains("employee:view"));

      // Verify called with roleIdList
      verify(roleMenuDao, times(1)).selectMenuListByRoleIdList(eq(List.of(1L)), eq(false));
    }

    @Test
    @DisplayName("Should return empty permissions when employee has no roles (non-administrator)")
    void loadUserPermission_NoRoles_ReturnsEmptyPermissions() {
      // Given
      testEmployee.setAdministratorFlag(false);

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(new ArrayList<>());
      // Note: roleMenuDao is NOT called when employee has no roles (non-administrator path)

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertTrue(result.getRoleList().isEmpty());
      assertTrue(result.getPermissionList().isEmpty());
    }

    @Test
    @DisplayName("Should extract permissions from menu apiPerms (comma-separated)")
    void loadUserPermission_ParsesApiPermsCorrectly() {
      // Given
      testEmployee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("ADMIN");

      MenuEntity menu = new MenuEntity();
      menu.setMenuId(1L);
      menu.setPermsType(1);
      menu.setApiPerms("user:add,user:update,user:delete,user:view");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(List.of(role));
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), eq(false))).thenReturn(List.of(menu));

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(4, result.getPermissionList().size());
      assertTrue(result.getPermissionList().contains("user:add"));
      assertTrue(result.getPermissionList().contains("user:update"));
      assertTrue(result.getPermissionList().contains("user:delete"));
      assertTrue(result.getPermissionList().contains("user:view"));
    }

    @Test
    @DisplayName("Should skip menus with null permsType or empty apiPerms")
    void loadUserPermission_SkipsInvalidMenus() {
      // Given
      testEmployee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("USER");

      MenuEntity menu1 = new MenuEntity();
      menu1.setMenuId(1L);
      menu1.setPermsType(null); // No permission type
      menu1.setApiPerms("should:be:skipped");

      MenuEntity menu2 = new MenuEntity();
      menu2.setMenuId(2L);
      menu2.setPermsType(1);
      menu2.setApiPerms(""); // Empty apiPerms

      MenuEntity menu3 = new MenuEntity();
      menu3.setMenuId(3L);
      menu3.setPermsType(1);
      menu3.setApiPerms("valid:permission");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID)).thenReturn(List.of(role));
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), eq(false)))
          .thenReturn(List.of(menu1, menu2, menu3));

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(1, result.getPermissionList().size());
      assertTrue(result.getPermissionList().contains("valid:permission"));
      assertFalse(result.getPermissionList().contains("should:be:skipped"));
    }

    @Test
    @DisplayName("Should handle multiple roles with combined permissions")
    void loadUserPermission_MultipleRoles_CombinesPermissions() {
      // Given
      testEmployee.setAdministratorFlag(false);

      RoleVO role1 = new RoleVO();
      role1.setRoleId(1L);
      role1.setRoleCode("MANAGER");

      RoleVO role2 = new RoleVO();
      role2.setRoleId(2L);
      role2.setRoleCode("USER");

      MenuEntity menu1 = new MenuEntity();
      menu1.setMenuId(1L);
      menu1.setPermsType(1);
      menu1.setApiPerms("employee:add");

      MenuEntity menu2 = new MenuEntity();
      menu2.setMenuId(2L);
      menu2.setPermsType(1);
      menu2.setApiPerms("employee:view");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(roleEmployeeDao.selectRoleByEmployeeId(TEST_EMPLOYEE_ID))
          .thenReturn(List.of(role1, role2));
      when(roleMenuDao.selectMenuListByRoleIdList(eq(List.of(1L, 2L)), eq(false)))
          .thenReturn(List.of(menu1, menu2));

      // When
      UserPermission result = loginManager.loadUserPermission(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(2, result.getRoleList().size());
      assertTrue(result.getRoleList().contains("MANAGER"));
      assertTrue(result.getRoleList().contains("USER"));

      assertEquals(2, result.getPermissionList().size());
      assertTrue(result.getPermissionList().contains("employee:add"));
      assertTrue(result.getPermissionList().contains("employee:view"));
    }
  }

  @Nested
  @DisplayName("clearUserPermission() and clearUserLoginInfo() Tests - Cache Invalidation")
  class ClearCacheTests {

    @Test
    @DisplayName("clearUserPermission() should not throw exception")
    void clearUserPermission_DoesNotThrowException() {
      // When/Then - Should not throw
      assertDoesNotThrow(() -> loginManager.clearUserPermission(TEST_EMPLOYEE_ID));
      assertDoesNotThrow(() -> loginManager.clearUserPermission(null));
    }

    @Test
    @DisplayName("clearUserLoginInfo() should not throw exception")
    void clearUserLoginInfo_DoesNotThrowException() {
      // When/Then - Should not throw
      assertDoesNotThrow(() -> loginManager.clearUserLoginInfo(TEST_EMPLOYEE_ID));
      assertDoesNotThrow(() -> loginManager.clearUserLoginInfo(null));
    }

    @Test
    @DisplayName(
        "Cache clear methods are no-op in unit tests (cache behavior tested in production)")
    void clearCache_NoOpInUnitTests() {
      // Note: @CacheInvalidate is a JetCache framework annotation
      // In unit tests, these methods are no-ops
      // In production, JetCache intercepts and clears the cache

      // When
      loginManager.clearUserPermission(TEST_EMPLOYEE_ID);
      loginManager.clearUserLoginInfo(TEST_EMPLOYEE_ID);

      // Then - Methods execute without error
      // Actual cache invalidation is tested in production/integration environment
    }
  }
}
