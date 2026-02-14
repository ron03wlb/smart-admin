package net.lab1024.sa.system.datascope.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.system.datascope.constant.DataScopeTypeEnum;
import net.lab1024.sa.system.datascope.constant.DataScopeViewTypeEnum;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.domain.entity.RoleDataScopeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DataScopeViewManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getCanViewEmployeeId - 根據視圖類型獲取可查看員工ID
 *   <li>getCanViewDepartmentId - 根據視圖類型獲取可查看部門ID
 *   <li>getMeDepartmentIdList - 獲取本人部門ID
 *   <li>getDepartmentAndSubIdList - 獲取部門及下屬部門ID
 *   <li>getEmployeeDataScopeViewType - 獲取員工數據範圍視圖類型
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataScopeViewManager 單元測試")
class DataScopeViewManagerTest {

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleDataScopeDao roleDataScopeDao;

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @InjectMocks private DataScopeViewManager dataScopeViewManager;

  // ==================== getCanViewEmployeeId 測試 ====================

  @Nested
  @DisplayName("getCanViewEmployeeId 獲取可查看員工ID測試")
  class GetCanViewEmployeeIdTest {

    @Test
    @DisplayName("視圖類型為ME時：應該只返回本人ID")
    void shouldReturnOnlySelfIdWhenViewTypeIsMe() {
      // Given
      Long employeeId = 1L;

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewEmployeeId(DataScopeViewTypeEnum.ME, employeeId);

      // Then
      assertThat(result).containsExactly(employeeId);
    }

    @Test
    @DisplayName("視圖類型為DEPARTMENT時：應該返回本部門所有員工ID")
    void shouldReturnDepartmentEmployeeIdsWhenViewTypeIsDepartment() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;
      List<Long> departmentEmployeeIds = Lists.newArrayList(1L, 2L, 3L);

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(employeeDao.getEmployeeIdByDepartmentId(departmentId, false))
          .thenReturn(departmentEmployeeIds);

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewEmployeeId(DataScopeViewTypeEnum.DEPARTMENT, employeeId);

      // Then
      assertThat(result).containsExactlyElementsOf(departmentEmployeeIds);
    }

    @Test
    @DisplayName("視圖類型為DEPARTMENT_AND_SUB時：應該返回本部門及下屬部門所有員工ID")
    void shouldReturnDepartmentAndSubEmployeeIdsWhenViewTypeIsDepartmentAndSub() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;
      List<Long> allDepartmentIds = Lists.newArrayList(100L, 101L, 102L);
      List<Long> allEmployeeIds = Lists.newArrayList(1L, 2L, 3L, 4L, 5L);

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(departmentCacheManager.getDepartmentSelfAndChildren(departmentId))
          .thenReturn(allDepartmentIds);
      when(employeeDao.getEmployeeIdByDepartmentIdList(allDepartmentIds, false))
          .thenReturn(allEmployeeIds);

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewEmployeeId(
              DataScopeViewTypeEnum.DEPARTMENT_AND_SUB, employeeId);

      // Then
      assertThat(result).containsExactlyElementsOf(allEmployeeIds);
    }

    @Test
    @DisplayName("視圖類型為ALL時：應該返回空列表（表示可查看全部）")
    void shouldReturnEmptyListWhenViewTypeIsAll() {
      // Given
      Long employeeId = 1L;

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewEmployeeId(DataScopeViewTypeEnum.ALL, employeeId);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getCanViewDepartmentId 測試 ====================

  @Nested
  @DisplayName("getCanViewDepartmentId 獲取可查看部門ID測試")
  class GetCanViewDepartmentIdTest {

    @Test
    @DisplayName("視圖類型為ME時：應該返回[0]（不可查看任何部門）")
    void shouldReturnZeroWhenViewTypeIsMe() {
      // Given
      Long employeeId = 1L;

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewDepartmentId(DataScopeViewTypeEnum.ME, employeeId);

      // Then
      assertThat(result).containsExactly(0L);
    }

    @Test
    @DisplayName("視圖類型為DEPARTMENT時：應該返回本人部門ID")
    void shouldReturnMeDepartmentIdWhenViewTypeIsDepartment() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewDepartmentId(DataScopeViewTypeEnum.DEPARTMENT, employeeId);

      // Then
      assertThat(result).containsExactly(departmentId);
    }

    @Test
    @DisplayName("視圖類型為DEPARTMENT_AND_SUB時：應該返回本部門及下屬部門ID")
    void shouldReturnDepartmentAndSubIdsWhenViewTypeIsDepartmentAndSub() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;
      List<Long> allDepartmentIds = Lists.newArrayList(100L, 101L, 102L);

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(departmentCacheManager.getDepartmentSelfAndChildren(departmentId))
          .thenReturn(allDepartmentIds);

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewDepartmentId(
              DataScopeViewTypeEnum.DEPARTMENT_AND_SUB, employeeId);

      // Then
      assertThat(result).containsExactlyElementsOf(allDepartmentIds);
    }

    @Test
    @DisplayName("視圖類型為ALL時：應該返回空列表（表示可查看全部）")
    void shouldReturnEmptyListWhenViewTypeIsAll() {
      // Given
      Long employeeId = 1L;

      // When
      List<Long> result =
          dataScopeViewManager.getCanViewDepartmentId(DataScopeViewTypeEnum.ALL, employeeId);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getMeDepartmentIdList 測試 ====================

  @Nested
  @DisplayName("getMeDepartmentIdList 獲取本人部門ID測試")
  class GetMeDepartmentIdListTest {

    @Test
    @DisplayName("正常情況：應該返回員工所屬部門ID")
    void shouldReturnEmployeeDepartmentId() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      List<Long> result = dataScopeViewManager.getMeDepartmentIdList(employeeId);

      // Then
      assertThat(result).containsExactly(departmentId);
    }
  }

  // ==================== getDepartmentAndSubIdList 測試 ====================

  @Nested
  @DisplayName("getDepartmentAndSubIdList 獲取部門及下屬部門ID測試")
  class GetDepartmentAndSubIdListTest {

    @Test
    @DisplayName("正常情況：應該返回本部門及所有下屬部門ID")
    void shouldReturnDepartmentAndAllSubDepartmentIds() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 100L;
      List<Long> allDepartmentIds = Lists.newArrayList(100L, 101L, 102L, 103L);

      EmployeeEntity employee = createTestEmployee(employeeId, departmentId);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(departmentCacheManager.getDepartmentSelfAndChildren(departmentId))
          .thenReturn(allDepartmentIds);

      // When
      List<Long> result = dataScopeViewManager.getDepartmentAndSubIdList(employeeId);

      // Then
      assertThat(result).containsExactlyElementsOf(allDepartmentIds);
    }
  }

  // ==================== getEmployeeDataScopeViewType 測試 ====================

  @Nested
  @DisplayName("getEmployeeDataScopeViewType 獲取員工數據範圍視圖類型測試")
  class GetEmployeeDataScopeViewTypeTest {

    @Test
    @DisplayName("員工不存在時：應該返回ME")
    void shouldReturnMeWhenEmployeeNotExists() {
      // Given
      Long employeeId = 999L;
      when(employeeDao.selectById(employeeId)).thenReturn(null);

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ME);
    }

    @Test
    @DisplayName("員工ID為null時：應該返回ME")
    void shouldReturnMeWhenEmployeeIdIsNull() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = new EmployeeEntity();
      employee.setEmployeeId(null);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ME);
    }

    @Test
    @DisplayName("超級管理員時：應該返回ALL")
    void shouldReturnAllWhenEmployeeIsAdministrator() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(true);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ALL);
    }

    @Test
    @DisplayName("未設置角色時：應該返回ME")
    void shouldReturnMeWhenNoRoleAssigned() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(false);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId))
          .thenReturn(Collections.emptyList());

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ME);
    }

    @Test
    @DisplayName("角色未設置數據範圍時：應該返回ME")
    void shouldReturnMeWhenNoDataScopeConfigured() {
      // Given
      Long employeeId = 1L;
      List<Long> roleIds = Lists.newArrayList(1L, 2L);

      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(false);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId)).thenReturn(roleIds);
      when(roleDataScopeDao.listByRoleIdList(roleIds)).thenReturn(Collections.emptyList());

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ME);
    }

    @Test
    @DisplayName("指定數據類型無配置時：應該返回ME")
    void shouldReturnMeWhenSpecificDataScopeTypeNotConfigured() {
      // Given
      Long employeeId = 1L;
      List<Long> roleIds = Lists.newArrayList(1L);

      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(false);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId)).thenReturn(roleIds);

      // 設置其他類型的數據範圍，但不是 NOTICE 類型
      RoleDataScopeEntity otherScope = new RoleDataScopeEntity();
      otherScope.setDataScopeType(999); // 其他類型
      otherScope.setViewType(DataScopeViewTypeEnum.ALL.getValue());
      when(roleDataScopeDao.listByRoleIdList(roleIds)).thenReturn(Lists.newArrayList(otherScope));

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.ME);
    }

    @Test
    @DisplayName("有多個數據範圍配置時：應該返回最高級別的視圖類型")
    void shouldReturnHighestLevelViewTypeWhenMultipleDataScopesConfigured() {
      // Given
      Long employeeId = 1L;
      List<Long> roleIds = Lists.newArrayList(1L, 2L);

      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(false);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId)).thenReturn(roleIds);

      // 設置多個不同級別的數據範圍
      RoleDataScopeEntity scopeMe =
          createRoleDataScope(DataScopeTypeEnum.NOTICE, DataScopeViewTypeEnum.ME);
      RoleDataScopeEntity scopeDept =
          createRoleDataScope(DataScopeTypeEnum.NOTICE, DataScopeViewTypeEnum.DEPARTMENT);
      RoleDataScopeEntity scopeDeptAndSub =
          createRoleDataScope(DataScopeTypeEnum.NOTICE, DataScopeViewTypeEnum.DEPARTMENT_AND_SUB);

      when(roleDataScopeDao.listByRoleIdList(roleIds))
          .thenReturn(Lists.newArrayList(scopeMe, scopeDept, scopeDeptAndSub));

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.DEPARTMENT_AND_SUB);
    }

    @Test
    @DisplayName("有單個數據範圍配置時：應該返回該視圖類型")
    void shouldReturnConfiguredViewTypeWhenSingleDataScopeConfigured() {
      // Given
      Long employeeId = 1L;
      List<Long> roleIds = Lists.newArrayList(1L);

      EmployeeEntity employee = createTestEmployee(employeeId, 100L);
      employee.setAdministratorFlag(false);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId)).thenReturn(roleIds);

      RoleDataScopeEntity scopeDept =
          createRoleDataScope(DataScopeTypeEnum.NOTICE, DataScopeViewTypeEnum.DEPARTMENT);
      when(roleDataScopeDao.listByRoleIdList(roleIds)).thenReturn(Lists.newArrayList(scopeDept));

      // When
      DataScopeViewTypeEnum result =
          dataScopeViewManager.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, employeeId);

      // Then
      assertThat(result).isEqualTo(DataScopeViewTypeEnum.DEPARTMENT);
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeEntity createTestEmployee(Long employeeId, Long departmentId) {
    EmployeeEntity employee = new EmployeeEntity();
    employee.setEmployeeId(employeeId);
    employee.setDepartmentId(departmentId);
    employee.setAdministratorFlag(false);
    return employee;
  }

  private RoleDataScopeEntity createRoleDataScope(
      DataScopeTypeEnum dataScopeType, DataScopeViewTypeEnum viewType) {
    RoleDataScopeEntity entity = new RoleDataScopeEntity();
    entity.setDataScopeType(dataScopeType.getValue());
    entity.setViewType(viewType.getValue());
    return entity;
  }
}
