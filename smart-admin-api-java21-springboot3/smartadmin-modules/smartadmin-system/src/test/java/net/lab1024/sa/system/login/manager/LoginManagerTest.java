package net.lab1024.sa.system.login.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.UserPermission;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.file.service.IFileStorageService;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.login.domain.RequestEmployee;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * LoginManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>獲取請求用戶信息（帶緩存）
 *   <li>加載登錄信息
 *   <li>獲取用戶權限
 *   <li>加載用戶權限
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginManager 單元測試")
class LoginManagerTest {

  @Mock private DepartmentDao departmentDao;

  @Mock private IFileStorageService fileStorageService;

  @Mock private EmployeeDao employeeDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleMenuDao roleMenuDao;

  @InjectMocks private LoginManager loginManager;

  // ==================== getRequestEmployee 測試 ====================

  @Nested
  @DisplayName("getRequestEmployee 獲取請求用戶信息測試")
  class GetRequestEmployeeTest {

    @Test
    @DisplayName("邊界情況：employeeId為null時應返回null")
    void shouldReturnNullWhenEmployeeIdNull() {
      // When
      RequestEmployee result = loginManager.getRequestEmployee(null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("邊界情況：員工不存在時應返回null")
    void shouldReturnNullWhenEmployeeNotFound() {
      // Given
      Long employeeId = 999L;
      when(employeeDao.selectById(employeeId)).thenReturn(null);

      // When
      RequestEmployee result = loginManager.getRequestEmployee(employeeId);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("正常情況：應該返回員工請求信息")
    void shouldReturnRequestEmployee() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "admin", 1L);
      DepartmentVO department = createTestDepartment(1L, "IT部門");

      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(departmentDao.selectDepartmentVO(1L)).thenReturn(department);

      // When
      RequestEmployee result = loginManager.getRequestEmployee(employeeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEmployeeId()).isEqualTo(employeeId);
      assertThat(result.getLoginName()).isEqualTo("admin");
      assertThat(result.getDepartmentName()).isEqualTo("IT部門");
    }
  }

  // ==================== loadLoginInfo 測試 ====================

  @Nested
  @DisplayName("loadLoginInfo 加載登錄信息測試")
  class LoadLoginInfoTest {

    @Test
    @DisplayName("正常情況：應該正確加載登錄信息和部門名稱")
    void shouldLoadLoginInfoWithDepartment() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin", 1L);
      DepartmentVO department = createTestDepartment(1L, "研發部");

      when(departmentDao.selectDepartmentVO(1L)).thenReturn(department);

      // When
      RequestEmployee result = loginManager.loadLoginInfo(employee);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getUserType()).isEqualTo(UserTypeEnum.ADMIN_EMPLOYEE);
      assertThat(result.getDepartmentName()).isEqualTo("研發部");
    }

    @Test
    @DisplayName("邊界情況：部門不存在時應該設置空部門名稱")
    void shouldSetEmptyDepartmentNameWhenNoDepartment() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin", 999L);

      when(departmentDao.selectDepartmentVO(999L)).thenReturn(null);

      // When
      RequestEmployee result = loginManager.loadLoginInfo(employee);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDepartmentName()).isEmpty();
    }

    @Test
    @DisplayName("正常情況：有頭像時應該獲取頭像URL")
    void shouldGetAvatarUrlWhenHasAvatar() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin", 1L);
      employee.setAvatar("avatar-key");

      when(departmentDao.selectDepartmentVO(1L)).thenReturn(null);
      when(fileStorageService.getFileUrl("avatar-key"))
          .thenReturn(ResponseDTO.ok("https://example.com/avatar.jpg"));

      // When
      RequestEmployee result = loginManager.loadLoginInfo(employee);

      // Then
      assertThat(result.getAvatar()).isEqualTo("https://example.com/avatar.jpg");
    }
  }

  // ==================== getUserPermission 測試 ====================

  @Nested
  @DisplayName("getUserPermission 獲取用戶權限測試")
  class GetUserPermissionTest {

    @Test
    @DisplayName("邊界情況：employeeId為null時應返回null")
    void shouldReturnNullWhenEmployeeIdNull() {
      // When
      UserPermission result = loginManager.getUserPermission(null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("正常情況：應該返回用戶權限")
    void shouldReturnUserPermission() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "admin", 1L);
      employee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("ROLE_USER");

      MenuEntity menu = createTestMenu(1L, "user:add");

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId))
          .thenReturn(Collections.singletonList(role));
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), anyBoolean()))
          .thenReturn(Collections.singletonList(menu));

      // When
      UserPermission result = loginManager.getUserPermission(employeeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRoleList()).contains("ROLE_USER");
      assertThat(result.getPermissionList()).contains("user:add");
    }
  }

  // ==================== loadUserPermission 測試 ====================

  @Nested
  @DisplayName("loadUserPermission 加載用戶權限測試")
  class LoadUserPermissionTest {

    @Test
    @DisplayName("正常情況：管理員應該獲取所有菜單權限")
    void shouldGetAllMenusForAdmin() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity admin = createTestEmployee(employeeId, "admin", 1L);
      admin.setAdministratorFlag(true);

      MenuEntity menu1 = createTestMenu(1L, "user:add");
      MenuEntity menu2 = createTestMenu(2L, "user:edit");

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId)).thenReturn(Collections.emptyList());
      when(employeeDao.selectById(employeeId)).thenReturn(admin);
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), anyBoolean()))
          .thenReturn(List.of(menu1, menu2));

      // When
      UserPermission result = loginManager.loadUserPermission(employeeId);

      // Then
      assertThat(result.getPermissionList()).containsExactlyInAnyOrder("user:add", "user:edit");
    }

    @Test
    @DisplayName("邊界情況：非管理員無角色應返回空權限")
    void shouldReturnEmptyPermissionsForNonAdminWithNoRoles() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "user", 1L);
      employee.setAdministratorFlag(false);

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId)).thenReturn(Collections.emptyList());
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      UserPermission result = loginManager.loadUserPermission(employeeId);

      // Then
      assertThat(result.getPermissionList()).isEmpty();
      assertThat(result.getRoleList()).isEmpty();
    }

    @Test
    @DisplayName("正常情況：應該正確解析多個權限（逗號分隔）")
    void shouldParseMultiplePermissions() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "user", 1L);
      employee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("ROLE_USER");

      MenuEntity menu = createTestMenu(1L, "user:add,user:edit,user:delete");

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId))
          .thenReturn(Collections.singletonList(role));
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), anyBoolean()))
          .thenReturn(Collections.singletonList(menu));

      // When
      UserPermission result = loginManager.loadUserPermission(employeeId);

      // Then
      assertThat(result.getPermissionList())
          .containsExactlyInAnyOrder("user:add", "user:edit", "user:delete");
    }

    @Test
    @DisplayName("邊界情況：菜單無權限類型時應該跳過")
    void shouldSkipMenuWithNoPermsType() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "user", 1L);
      employee.setAdministratorFlag(false);

      RoleVO role = new RoleVO();
      role.setRoleId(1L);
      role.setRoleCode("ROLE_USER");

      MenuEntity menu = new MenuEntity();
      menu.setMenuId(1L);
      menu.setPermsType(null); // 無權限類型

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId))
          .thenReturn(Collections.singletonList(role));
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), anyBoolean()))
          .thenReturn(Collections.singletonList(menu));

      // When
      UserPermission result = loginManager.loadUserPermission(employeeId);

      // Then
      assertThat(result.getPermissionList()).isEmpty();
    }
  }

  // ==================== clearUserPermission / clearUserLoginInfo 測試 ====================

  @Nested
  @DisplayName("clearCache 清除緩存測試")
  class ClearCacheTest {

    @Test
    @DisplayName("正常情況：clearUserPermission 應該正常執行")
    void shouldClearUserPermission() {
      // When & Then - 這些方法是空方法，由緩存註解處理
      loginManager.clearUserPermission(1L);
      // 不會拋出異常
    }

    @Test
    @DisplayName("正常情況：clearUserLoginInfo 應該正常執行")
    void shouldClearUserLoginInfo() {
      // When & Then - 這些方法是空方法，由緩存註解處理
      loginManager.clearUserLoginInfo(1L);
      // 不會拋出異常
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeEntity createTestEmployee(Long id, String loginName, Long departmentId) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setLoginName(loginName);
    entity.setActualName("員工" + id);
    entity.setDepartmentId(departmentId);
    entity.setAdministratorFlag(false);
    return entity;
  }

  private DepartmentVO createTestDepartment(Long id, String name) {
    DepartmentVO vo = new DepartmentVO();
    vo.setDepartmentId(id);
    vo.setDepartmentName(name);
    return vo;
  }

  private MenuEntity createTestMenu(Long id, String apiPerms) {
    MenuEntity entity = new MenuEntity();
    entity.setMenuId(id);
    entity.setPermsType(1); // 有權限類型
    entity.setApiPerms(apiPerms);
    return entity;
  }
}
