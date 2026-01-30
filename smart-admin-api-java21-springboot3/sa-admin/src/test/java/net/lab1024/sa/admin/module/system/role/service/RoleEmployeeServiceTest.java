package net.lab1024.sa.admin.module.system.role.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.lab1024.sa.admin.fixtures.RoleTestFixture;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.admin.module.system.role.RoleEmployeeTestFixture;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleEmployeeQueryForm;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleEmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleSelectedVO;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleEmployeeService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>批量插入角色员工
 *   <li>分页查询角色员工（含部门名称关联）
 *   <li>获取角色所有员工
 *   <li>移除员工角色（参数校验）
 *   <li>批量移除角色员工
 *   <li>批量添加角色员工（过滤已存在）
 *   <li>获取员工角色信息列表（含选中状态）
 *   <li>根据员工ID查询角色列表
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleEmployeeService 单元测试")
class RoleEmployeeServiceTest {

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleDao roleDao;

  @Mock private DepartmentDao departmentDao;

  @InjectMocks private RoleEmployeeService roleEmployeeService;

  @BeforeEach
  void setUp() {
    RoleEmployeeTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("batchInsert() - 批量插入角色员工")
  class BatchInsertTests {

    @Test
    @DisplayName("正常批量插入 - 应调用Dao插入所有实体")
    void batchInsert_ValidList_ShouldInsertAll() {
      // Arrange
      Long roleId = 1L;
      List<Long> employeeIds = Arrays.asList(1L, 2L, 3L);
      List<RoleEmployeeEntity> entities =
          RoleEmployeeTestFixture.createEntityList(roleId, employeeIds);

      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // Act
      roleEmployeeService.batchInsert(entities);

      // Assert
      verify(roleEmployeeDao, times(3)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("空列表插入 - 应不调用Dao")
    void batchInsert_EmptyList_ShouldNotInsert() {
      // Arrange
      List<RoleEmployeeEntity> emptyList = Collections.emptyList();

      // Act
      roleEmployeeService.batchInsert(emptyList);

      // Assert
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("queryEmployee() - 分页查询角色员工")
  class QueryEmployeeTests {

    @Test
    @DisplayName("正常分页查询 - 应返回员工列表含部门名称")
    void queryEmployee_ValidForm_ShouldReturnEmployeesWithDepartmentName() {
      // Arrange
      Long roleId = 1L;
      Long departmentId = 100L;
      RoleEmployeeQueryForm form = RoleEmployeeTestFixture.createQueryForm(String.valueOf(roleId));

      List<EmployeeVO> employeeList = RoleEmployeeTestFixture.createEmployeeVOList(3, departmentId);

      DepartmentEntity department = new DepartmentEntity();
      department.setDepartmentId(departmentId);
      department.setDepartmentName("测试部门");

      when(roleEmployeeDao.selectRoleEmployeeByName(any(Page.class), eq(form)))
          .thenReturn(employeeList);
      when(departmentDao.selectBatchIds(anyList())).thenReturn(Arrays.asList(department));

      // Act
      ResponseDTO<PageResult<EmployeeVO>> response = roleEmployeeService.queryEmployee(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      PageResult<EmployeeVO> result = response.getData();
      assertEquals(3, result.getList().size());

      // Verify department name is set
      result.getList().forEach(emp -> assertEquals("测试部门", emp.getDepartmentName()));

      verify(roleEmployeeDao, times(1)).selectRoleEmployeeByName(any(Page.class), eq(form));
      verify(departmentDao, times(1)).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("查询结果为空 - 应返回空列表")
    void queryEmployee_NoResults_ShouldReturnEmptyList() {
      // Arrange
      Long roleId = 1L;
      RoleEmployeeQueryForm form = RoleEmployeeTestFixture.createQueryForm(String.valueOf(roleId));

      when(roleEmployeeDao.selectRoleEmployeeByName(any(Page.class), eq(form)))
          .thenReturn(Collections.emptyList());

      // Act
      ResponseDTO<PageResult<EmployeeVO>> response = roleEmployeeService.queryEmployee(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().getList().isEmpty());
      verify(departmentDao, never()).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("员工无部门 - 应返回空部门名称")
    void queryEmployee_EmployeesWithoutDepartment_ShouldReturnEmptyDepartmentName() {
      // Arrange
      Long roleId = 1L;
      RoleEmployeeQueryForm form = RoleEmployeeTestFixture.createQueryForm(String.valueOf(roleId));

      List<EmployeeVO> employeeList = RoleEmployeeTestFixture.createEmployeeVOList(2, null);

      when(roleEmployeeDao.selectRoleEmployeeByName(any(Page.class), eq(form)))
          .thenReturn(employeeList);

      // Act
      ResponseDTO<PageResult<EmployeeVO>> response = roleEmployeeService.queryEmployee(form);

      // Assert
      assertTrue(response.getOk());
      assertEquals(2, response.getData().getList().size());
      verify(departmentDao, never()).selectBatchIds(anyList());
    }
  }

  @Nested
  @DisplayName("getAllEmployeeByRoleId() - 获取角色所有员工")
  class GetAllEmployeeByRoleIdTests {

    @Test
    @DisplayName("正常获取所有员工 - 应返回员工列表")
    void getAllEmployeeByRoleId_ValidRoleId_ShouldReturnEmployeeList() {
      // Arrange
      Long roleId = 1L;
      List<EmployeeVO> employeeList = RoleEmployeeTestFixture.createEmployeeVOList(5, 100L);

      when(roleEmployeeDao.selectEmployeeByRoleId(roleId)).thenReturn(employeeList);

      // Act
      List<EmployeeVO> result = roleEmployeeService.getAllEmployeeByRoleId(roleId);

      // Assert
      assertNotNull(result);
      assertEquals(5, result.size());
      verify(roleEmployeeDao, times(1)).selectEmployeeByRoleId(roleId);
    }

    @Test
    @DisplayName("角色无员工 - 应返回空列表")
    void getAllEmployeeByRoleId_NoEmployees_ShouldReturnEmptyList() {
      // Arrange
      Long roleId = 2L;

      when(roleEmployeeDao.selectEmployeeByRoleId(roleId)).thenReturn(Collections.emptyList());

      // Act
      List<EmployeeVO> result = roleEmployeeService.getAllEmployeeByRoleId(roleId);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("removeRoleEmployee() - 移除员工角色")
  class RemoveRoleEmployeeTests {

    @Test
    @DisplayName("正常移除员工角色 - 应返回成功")
    void removeRoleEmployee_ValidIds_ShouldReturnSuccess() {
      // Arrange
      Long employeeId = 1L;
      Long roleId = 2L;

      doNothing().when(roleEmployeeDao).deleteByEmployeeIdRoleId(employeeId, roleId);

      // Act
      ResponseDTO<String> response = roleEmployeeService.removeRoleEmployee(employeeId, roleId);

      // Assert
      assertTrue(response.getOk());
      verify(roleEmployeeDao, times(1)).deleteByEmployeeIdRoleId(employeeId, roleId);
    }

    @Test
    @DisplayName("employeeId为null - 应返回参数错误")
    void removeRoleEmployee_NullEmployeeId_ShouldReturnError() {
      // Arrange
      Long employeeId = null;
      Long roleId = 2L;

      // Act
      ResponseDTO<String> response = roleEmployeeService.removeRoleEmployee(employeeId, roleId);

      // Assert
      assertFalse(response.getOk());
      verify(roleEmployeeDao, never()).deleteByEmployeeIdRoleId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("roleId为null - 应返回参数错误")
    void removeRoleEmployee_NullRoleId_ShouldReturnError() {
      // Arrange
      Long employeeId = 1L;
      Long roleId = null;

      // Act
      ResponseDTO<String> response = roleEmployeeService.removeRoleEmployee(employeeId, roleId);

      // Assert
      assertFalse(response.getOk());
      verify(roleEmployeeDao, never()).deleteByEmployeeIdRoleId(anyLong(), anyLong());
    }
  }

  @Nested
  @DisplayName("batchRemoveRoleEmployee() - 批量移除角色员工")
  class BatchRemoveRoleEmployeeTests {

    @Test
    @DisplayName("正常批量移除 - 应返回成功")
    void batchRemoveRoleEmployee_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 1L;
      Set<Long> employeeIds = RoleEmployeeTestFixture.createEmployeeIdSet(1L, 2L, 3L);
      RoleEmployeeUpdateForm form = RoleEmployeeTestFixture.createUpdateForm(roleId, employeeIds);

      doNothing().when(roleEmployeeDao).batchDeleteEmployeeRole(roleId, employeeIds);

      // Act
      ResponseDTO<String> response = roleEmployeeService.batchRemoveRoleEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleEmployeeDao, times(1)).batchDeleteEmployeeRole(roleId, employeeIds);
    }

    @Test
    @DisplayName("空员工列表 - 应返回成功")
    void batchRemoveRoleEmployee_EmptyEmployeeList_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 1L;
      Set<Long> emptySet = Collections.emptySet();
      RoleEmployeeUpdateForm form = RoleEmployeeTestFixture.createUpdateForm(roleId, emptySet);

      doNothing().when(roleEmployeeDao).batchDeleteEmployeeRole(roleId, emptySet);

      // Act
      ResponseDTO<String> response = roleEmployeeService.batchRemoveRoleEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleEmployeeDao, times(1)).batchDeleteEmployeeRole(roleId, emptySet);
    }
  }

  @Nested
  @DisplayName("batchAddRoleEmployee() - 批量添加角色员工")
  class BatchAddRoleEmployeeTests {

    @Test
    @DisplayName("正常批量添加 - 应过滤已存在员工")
    void batchAddRoleEmployee_ValidForm_ShouldFilterExistingEmployees() {
      // Arrange
      Long roleId = 1L;
      Set<Long> selectedEmployeeIds = RoleEmployeeTestFixture.createEmployeeIdSet(1L, 2L, 3L, 4L);
      Set<Long> existingEmployeeIds = RoleEmployeeTestFixture.createEmployeeIdSet(1L, 2L);

      RoleEmployeeUpdateForm form =
          RoleEmployeeTestFixture.createUpdateForm(roleId, selectedEmployeeIds);

      when(roleEmployeeDao.selectEmployeeIdByRoleIdList(Lists.newArrayList(roleId)))
          .thenReturn(existingEmployeeIds);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // Act
      ResponseDTO<String> response = roleEmployeeService.batchAddRoleEmployee(form);

      // Assert
      assertTrue(response.getOk());

      // Verify only new employees (3 and 4) are inserted
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("所有员工已存在 - 应不插入")
    void batchAddRoleEmployee_AllEmployeesExist_ShouldNotInsert() {
      // Arrange
      Long roleId = 1L;
      Set<Long> employeeIds = RoleEmployeeTestFixture.createEmployeeIdSet(1L, 2L);
      RoleEmployeeUpdateForm form = RoleEmployeeTestFixture.createUpdateForm(roleId, employeeIds);

      when(roleEmployeeDao.selectEmployeeIdByRoleIdList(Lists.newArrayList(roleId)))
          .thenReturn(employeeIds);

      // Act
      ResponseDTO<String> response = roleEmployeeService.batchAddRoleEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("全新员工添加 - 应插入所有员工")
    void batchAddRoleEmployee_AllNewEmployees_ShouldInsertAll() {
      // Arrange
      Long roleId = 1L;
      Set<Long> employeeIds = RoleEmployeeTestFixture.createEmployeeIdSet(1L, 2L, 3L);
      RoleEmployeeUpdateForm form = RoleEmployeeTestFixture.createUpdateForm(roleId, employeeIds);

      when(roleEmployeeDao.selectEmployeeIdByRoleIdList(Lists.newArrayList(roleId)))
          .thenReturn(Collections.emptySet());
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // Act
      ResponseDTO<String> response = roleEmployeeService.batchAddRoleEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleEmployeeDao, times(3)).insert(any(RoleEmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("getRoleInfoListByEmployeeId() - 获取员工角色信息列表")
  class GetRoleInfoListByEmployeeIdTests {

    @Test
    @DisplayName("正常获取角色信息 - 应标记选中状态")
    void getRoleInfoListByEmployeeId_ValidEmployeeId_ShouldMarkSelected() {
      // Arrange
      Long employeeId = 1L;
      List<Long> selectedRoleIds = Arrays.asList(1L, 3L);

      RoleEntity role1 = RoleTestFixture.createRole(1L, "角色1", "ROLE_1");
      RoleEntity role2 = RoleTestFixture.createRole(2L, "角色2", "ROLE_2");
      RoleEntity role3 = RoleTestFixture.createRole(3L, "角色3", "ROLE_3");

      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId)).thenReturn(selectedRoleIds);
      when(roleDao.selectList(null)).thenReturn(Arrays.asList(role1, role2, role3));

      // Act
      List<RoleSelectedVO> result = roleEmployeeService.getRoleInfoListByEmployeeId(employeeId);

      // Assert
      assertNotNull(result);
      assertEquals(3, result.size());

      // Verify role1 and role3 are selected
      RoleSelectedVO roleVO1 =
          result.stream().filter(r -> r.getRoleId().equals(1L)).findFirst().orElse(null);
      assertNotNull(roleVO1);
      assertTrue(roleVO1.getSelected());

      RoleSelectedVO roleVO2 =
          result.stream().filter(r -> r.getRoleId().equals(2L)).findFirst().orElse(null);
      assertNotNull(roleVO2);
      assertFalse(roleVO2.getSelected());

      RoleSelectedVO roleVO3 =
          result.stream().filter(r -> r.getRoleId().equals(3L)).findFirst().orElse(null);
      assertNotNull(roleVO3);
      assertTrue(roleVO3.getSelected());
    }

    @Test
    @DisplayName("员工无角色 - 应返回所有角色未选中")
    void getRoleInfoListByEmployeeId_NoRoles_ShouldReturnAllUnselected() {
      // Arrange
      Long employeeId = 2L;

      RoleEntity role1 = RoleTestFixture.createRole(1L, "角色1", "ROLE_1");
      RoleEntity role2 = RoleTestFixture.createRole(2L, "角色2", "ROLE_2");

      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId))
          .thenReturn(Collections.emptyList());
      when(roleDao.selectList(null)).thenReturn(Arrays.asList(role1, role2));

      // Act
      List<RoleSelectedVO> result = roleEmployeeService.getRoleInfoListByEmployeeId(employeeId);

      // Assert
      assertNotNull(result);
      assertEquals(2, result.size());
      result.forEach(role -> assertFalse(role.getSelected()));
    }
  }

  @Nested
  @DisplayName("getRoleIdList() - 根据员工ID查询角色列表")
  class GetRoleIdListTests {

    @Test
    @DisplayName("正常查询角色列表 - 应返回角色VO列表")
    void getRoleIdList_ValidEmployeeId_ShouldReturnRoleList() {
      // Arrange
      Long employeeId = 1L;
      List<RoleVO> roleList = RoleEmployeeTestFixture.createRoleVOList(3);

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId)).thenReturn(roleList);

      // Act
      List<RoleVO> result = roleEmployeeService.getRoleIdList(employeeId);

      // Assert
      assertNotNull(result);
      assertEquals(3, result.size());
      verify(roleEmployeeDao, times(1)).selectRoleByEmployeeId(employeeId);
    }

    @Test
    @DisplayName("员工无角色 - 应返回空列表")
    void getRoleIdList_NoRoles_ShouldReturnEmptyList() {
      // Arrange
      Long employeeId = 2L;

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId)).thenReturn(Collections.emptyList());

      // Act
      List<RoleVO> result = roleEmployeeService.getRoleIdList(employeeId);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }
}
