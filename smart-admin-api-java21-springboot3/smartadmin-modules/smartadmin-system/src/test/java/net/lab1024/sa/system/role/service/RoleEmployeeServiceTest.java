package net.lab1024.sa.system.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.system.role.domain.form.RoleEmployeeQueryForm;
import net.lab1024.sa.system.role.domain.form.RoleEmployeeUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleSelectedVO;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleEmployeeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>角色-員工關係查詢
 *   <li>批量添加/移除角色成員
 *   <li>員工角色查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleEmployeeService 單元測試")
class RoleEmployeeServiceTest {

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleDao roleDao;

  @Mock private DepartmentDao departmentDao;

  @InjectMocks private RoleEmployeeService roleEmployeeService;

  // ==================== batchInsert 測試 ====================

  @Nested
  @DisplayName("batchInsert 批量插入測試")
  class BatchInsertTest {

    @Test
    @DisplayName("正常情況：應該逐條插入")
    void shouldInsertEachRecord() {
      // Given
      RoleEmployeeEntity entity1 = new RoleEmployeeEntity(1L, 100L);
      RoleEmployeeEntity entity2 = new RoleEmployeeEntity(1L, 101L);
      List<RoleEmployeeEntity> list = Arrays.asList(entity1, entity2);

      // When
      roleEmployeeService.batchInsert(list);

      // Then
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }
  }

  // ==================== queryEmployee 測試 ====================

  @Nested
  @DisplayName("queryEmployee 查詢角色員工測試")
  class QueryEmployeeTest {

    @Test
    @DisplayName("正常情況：應該返回員工列表並填充部門名稱")
    void shouldReturnEmployeeListWithDepartmentName() {
      // Given
      RoleEmployeeQueryForm queryForm = new RoleEmployeeQueryForm();
      queryForm.setRoleId("1");
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeVO emp1 = createTestEmployeeVO(100L, "John", 10L);
      EmployeeVO emp2 = createTestEmployeeVO(101L, "Jane", 10L);
      List<EmployeeVO> empList = Arrays.asList(emp1, emp2);

      DepartmentEntity dept = new DepartmentEntity();
      dept.setDepartmentId(10L);
      dept.setDepartmentName("Engineering");

      when(roleEmployeeDao.selectRoleEmployeeByName(any(Page.class), eq(queryForm)))
          .thenReturn(empList);
      when(departmentDao.selectBatchIds(anyList())).thenReturn(Collections.singletonList(dept));

      // When
      ResponseDTO<PageResult<EmployeeVO>> result = roleEmployeeService.queryEmployee(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(2);
      assertThat(result.getData().getList().get(0).getDepartmentName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyList() {
      // Given
      RoleEmployeeQueryForm queryForm = new RoleEmployeeQueryForm();
      queryForm.setRoleId("1");
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(roleEmployeeDao.selectRoleEmployeeByName(any(Page.class), eq(queryForm)))
          .thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<EmployeeVO>> result = roleEmployeeService.queryEmployee(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).isEmpty();
    }
  }

  // ==================== getAllEmployeeByRoleId 測試 ====================

  @Nested
  @DisplayName("getAllEmployeeByRoleId 測試")
  class GetAllEmployeeByRoleIdTest {

    @Test
    @DisplayName("正常情況：應該返回角色下所有員工")
    void shouldReturnAllEmployees() {
      // Given
      Long roleId = 1L;
      EmployeeVO emp = createTestEmployeeVO(100L, "John", 10L);
      when(roleEmployeeDao.selectEmployeeByRoleId(roleId))
          .thenReturn(Collections.singletonList(emp));

      // When
      List<EmployeeVO> result = roleEmployeeService.getAllEmployeeByRoleId(roleId);

      // Then
      assertThat(result).hasSize(1);
      verify(roleEmployeeDao).selectEmployeeByRoleId(roleId);
    }
  }

  // ==================== removeRoleEmployee 測試 ====================

  @Nested
  @DisplayName("removeRoleEmployee 移除角色員工測試")
  class RemoveRoleEmployeeTest {

    @Test
    @DisplayName("正常情況：應該成功移除")
    void shouldRemoveSuccess() {
      // Given
      Long employeeId = 100L;
      Long roleId = 1L;

      // When
      ResponseDTO<String> result = roleEmployeeService.removeRoleEmployee(employeeId, roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleEmployeeDao).deleteByEmployeeIdRoleId(employeeId, roleId);
    }

    @Test
    @DisplayName("異常情況：employeeId 為 null 時應該返回錯誤")
    void shouldReturnErrorWhenEmployeeIdNull() {
      // When
      ResponseDTO<String> result = roleEmployeeService.removeRoleEmployee(null, 1L);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleEmployeeDao, never()).deleteByEmployeeIdRoleId(any(), any());
    }

    @Test
    @DisplayName("異常情況：roleId 為 null 時應該返回錯誤")
    void shouldReturnErrorWhenRoleIdNull() {
      // When
      ResponseDTO<String> result = roleEmployeeService.removeRoleEmployee(100L, null);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleEmployeeDao, never()).deleteByEmployeeIdRoleId(any(), any());
    }
  }

  // ==================== batchRemoveRoleEmployee 測試 ====================

  @Nested
  @DisplayName("batchRemoveRoleEmployee 批量移除測試")
  class BatchRemoveRoleEmployeeTest {

    @Test
    @DisplayName("正常情況：應該成功批量移除")
    void shouldBatchRemoveSuccess() {
      // Given
      RoleEmployeeUpdateForm updateForm = new RoleEmployeeUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setEmployeeIdList(new HashSet<>(Arrays.asList(100L, 101L)));

      // When
      ResponseDTO<String> result = roleEmployeeService.batchRemoveRoleEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleEmployeeDao).batchDeleteEmployeeRole(eq(1L), any());
    }
  }

  // ==================== batchAddRoleEmployee 測試 ====================

  @Nested
  @DisplayName("batchAddRoleEmployee 批量添加測試")
  class BatchAddRoleEmployeeTest {

    @Test
    @DisplayName("正常情況：應該添加不存在的員工")
    void shouldAddNewEmployees() {
      // Given
      RoleEmployeeUpdateForm updateForm = new RoleEmployeeUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setEmployeeIdList(new HashSet<>(Arrays.asList(100L, 101L, 102L)));

      // 模擬數據庫已有員工 100L
      Set<Long> existingIds = new HashSet<>(Collections.singletonList(100L));
      when(roleEmployeeDao.selectEmployeeIdByRoleIdList(anyList())).thenReturn(existingIds);

      // When
      ResponseDTO<String> result = roleEmployeeService.batchAddRoleEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      // 應該插入 101L 和 102L（2 條記錄）
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("全部已存在：不應該插入任何記錄")
    void shouldNotInsertWhenAllExist() {
      // Given
      RoleEmployeeUpdateForm updateForm = new RoleEmployeeUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setEmployeeIdList(new HashSet<>(Arrays.asList(100L, 101L)));

      Set<Long> existingIds = new HashSet<>(Arrays.asList(100L, 101L));
      when(roleEmployeeDao.selectEmployeeIdByRoleIdList(anyList())).thenReturn(existingIds);

      // When
      ResponseDTO<String> result = roleEmployeeService.batchAddRoleEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }
  }

  // ==================== getRoleInfoListByEmployeeId 測試 ====================

  @Nested
  @DisplayName("getRoleInfoListByEmployeeId 測試")
  class GetRoleInfoListByEmployeeIdTest {

    @Test
    @DisplayName("正常情況：應該返回角色列表並標記已選")
    void shouldReturnRoleListWithSelection() {
      // Given
      Long employeeId = 100L;

      RoleEntity role1 = createTestRoleEntity(1L, "Admin");
      RoleEntity role2 = createTestRoleEntity(2L, "User");
      List<RoleEntity> allRoles = Arrays.asList(role1, role2);

      when(roleEmployeeDao.selectRoleIdByEmployeeId(employeeId))
          .thenReturn(Collections.singletonList(1L));
      when(roleDao.selectList(null)).thenReturn(allRoles);

      // When
      List<RoleSelectedVO> result = roleEmployeeService.getRoleInfoListByEmployeeId(employeeId);

      // Then
      assertThat(result).hasSize(2);
      // Admin 應該被標記為已選
      assertThat(
              result.stream().filter(r -> r.getRoleId().equals(1L)).findFirst().get().getSelected())
          .isTrue();
      // User 不應該被標記
      assertThat(
              result.stream().filter(r -> r.getRoleId().equals(2L)).findFirst().get().getSelected())
          .isFalse();
    }
  }

  // ==================== getRoleIdList 測試 ====================

  @Nested
  @DisplayName("getRoleIdList 測試")
  class GetRoleIdListTest {

    @Test
    @DisplayName("正常情況：應該返回員工的角色列表")
    void shouldReturnRoleList() {
      // Given
      Long employeeId = 100L;
      RoleVO roleVO = new RoleVO();
      roleVO.setRoleId(1L);
      roleVO.setRoleName("Admin");

      when(roleEmployeeDao.selectRoleByEmployeeId(employeeId))
          .thenReturn(Collections.singletonList(roleVO));

      // When
      List<RoleVO> result = roleEmployeeService.getRoleIdList(employeeId);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getRoleName()).isEqualTo("Admin");
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeVO createTestEmployeeVO(Long id, String name, Long deptId) {
    EmployeeVO vo = new EmployeeVO();
    vo.setEmployeeId(id);
    vo.setActualName(name);
    vo.setDepartmentId(deptId);
    return vo;
  }

  private RoleEntity createTestRoleEntity(Long id, String name) {
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(id);
    entity.setRoleName(name);
    return entity;
  }
}
