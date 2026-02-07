package net.lab1024.sa.system.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.department.domain.form.DepartmentAddForm;
import net.lab1024.sa.system.department.domain.form.DepartmentUpdateForm;
import net.lab1024.sa.system.department.domain.vo.DepartmentTreeVO;
import net.lab1024.sa.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DepartmentService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>部門 CRUD 操作
 *   <li>刪除時子部門/員工檢查
 *   <li>部門樹查詢
 *   <li>部門路徑查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService 單元測試")
class DepartmentServiceTest {

  @Mock private DepartmentDao departmentDao;

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @InjectMocks private DepartmentService departmentService;

  // ==================== addDepartment 測試 ====================

  @Nested
  @DisplayName("addDepartment 新增部門測試")
  class AddDepartmentTest {

    @Test
    @DisplayName("正常情況：應該成功新增部門")
    void shouldAddDepartmentSuccess() {
      // Given
      DepartmentAddForm addForm = createTestAddForm("Engineering", 0L);

      // When
      ResponseDTO<String> result = departmentService.addDepartment(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(departmentDao).insert(any(DepartmentEntity.class));
      verify(departmentCacheManager).clearCache();
    }
  }

  // ==================== updateDepartment 測試 ====================

  @Nested
  @DisplayName("updateDepartment 更新部門測試")
  class UpdateDepartmentTest {

    @Test
    @DisplayName("正常情況：應該成功更新部門")
    void shouldUpdateDepartmentSuccess() {
      // Given
      DepartmentUpdateForm updateForm = createTestUpdateForm(1L, "Updated Dept", 0L);
      DepartmentEntity existingEntity = createTestDepartmentEntity(1L, "Old Dept");

      when(departmentDao.selectById(1L)).thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(departmentDao).updateById(any(DepartmentEntity.class));
      verify(departmentCacheManager).clearCache();
    }

    @Test
    @DisplayName("異常情況：父級部門 ID 為空時應返回錯誤")
    void shouldReturnErrorWhenParentIdNull() {
      // Given
      DepartmentUpdateForm updateForm = createTestUpdateForm(1L, "Dept", null);

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("父级部门id不能为空");
      verify(departmentDao, never()).updateById(any(DepartmentEntity.class));
    }

    @Test
    @DisplayName("異常情況：部門不存在時應返回錯誤")
    void shouldReturnErrorWhenDepartmentNotFound() {
      // Given
      DepartmentUpdateForm updateForm = createTestUpdateForm(999L, "Dept", 0L);
      when(departmentDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = departmentService.updateDepartment(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(departmentDao, never()).updateById(any(DepartmentEntity.class));
    }
  }

  // ==================== deleteDepartment 測試 ====================

  @Nested
  @DisplayName("deleteDepartment 刪除部門測試")
  class DeleteDepartmentTest {

    @Test
    @DisplayName("正常情況：無子部門無員工時應成功刪除")
    void shouldDeleteDepartmentSuccess() {
      // Given
      Long departmentId = 1L;
      DepartmentEntity entity = createTestDepartmentEntity(departmentId, "Dept");

      when(departmentDao.selectById(departmentId)).thenReturn(entity);
      when(departmentDao.countSubDepartment(departmentId)).thenReturn(0);
      when(employeeDao.countByDepartmentId(departmentId, Boolean.FALSE)).thenReturn(0);

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(departmentId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(departmentDao).deleteById(departmentId);
      verify(departmentCacheManager).clearCache();
    }

    @Test
    @DisplayName("異常情況：部門不存在時應返回錯誤")
    void shouldReturnErrorWhenDepartmentNotFound() {
      // Given
      when(departmentDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(999L);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(departmentDao, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("異常情況：有子部門時應返回錯誤")
    void shouldReturnErrorWhenHasSubDepartment() {
      // Given
      Long departmentId = 1L;
      DepartmentEntity entity = createTestDepartmentEntity(departmentId, "Dept");

      when(departmentDao.selectById(departmentId)).thenReturn(entity);
      when(departmentDao.countSubDepartment(departmentId)).thenReturn(2);

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(departmentId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("请先删除子级部门");
      verify(departmentDao, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("異常情況：有員工時應返回錯誤")
    void shouldReturnErrorWhenHasEmployees() {
      // Given
      Long departmentId = 1L;
      DepartmentEntity entity = createTestDepartmentEntity(departmentId, "Dept");

      when(departmentDao.selectById(departmentId)).thenReturn(entity);
      when(departmentDao.countSubDepartment(departmentId)).thenReturn(0);
      when(employeeDao.countByDepartmentId(departmentId, Boolean.FALSE)).thenReturn(5);

      // When
      ResponseDTO<String> result = departmentService.deleteDepartment(departmentId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("请先删除部门员工");
      verify(departmentDao, never()).deleteById(anyLong());
    }
  }

  // ==================== departmentTree 測試 ====================

  @Nested
  @DisplayName("departmentTree 查詢部門樹測試")
  class DepartmentTreeTest {

    @Test
    @DisplayName("正常情況：應該返回部門樹結構")
    void shouldReturnDepartmentTree() {
      // Given
      DepartmentTreeVO treeVO = new DepartmentTreeVO();
      treeVO.setDepartmentId(1L);
      treeVO.setDepartmentName("Root");

      when(departmentCacheManager.getDepartmentTree())
          .thenReturn(Collections.singletonList(treeVO));

      // When
      ResponseDTO<List<DepartmentTreeVO>> result = departmentService.departmentTree();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ==================== selfAndChildrenIdList 測試 ====================

  @Nested
  @DisplayName("selfAndChildrenIdList 查詢子部門 ID 測試")
  class SelfAndChildrenIdListTest {

    @Test
    @DisplayName("正常情況：應該返回自身及子部門 ID 列表")
    void shouldReturnSelfAndChildrenIds() {
      // Given
      Long departmentId = 1L;
      List<Long> idList = Arrays.asList(1L, 2L, 3L);

      when(departmentCacheManager.getDepartmentSelfAndChildren(departmentId)).thenReturn(idList);

      // When
      List<Long> result = departmentService.selfAndChildrenIdList(departmentId);

      // Then
      assertThat(result).containsExactlyElementsOf(idList);
    }
  }

  // ==================== listAll 測試 ====================

  @Nested
  @DisplayName("listAll 查詢所有部門測試")
  class ListAllTest {

    @Test
    @DisplayName("正常情況：應該返回所有部門列表")
    void shouldReturnAllDepartments() {
      // Given
      DepartmentVO vo1 = createTestDepartmentVO(1L, "Dept1");
      DepartmentVO vo2 = createTestDepartmentVO(2L, "Dept2");

      when(departmentCacheManager.getDepartmentList()).thenReturn(Arrays.asList(vo1, vo2));

      // When
      List<DepartmentVO> result = departmentService.listAll();

      // Then
      assertThat(result).hasSize(2);
    }
  }

  // ==================== getDepartmentById 測試 ====================

  @Nested
  @DisplayName("getDepartmentById 查詢部門測試")
  class GetDepartmentByIdTest {

    @Test
    @DisplayName("正常情況：應該返回部門詳情")
    void shouldReturnDepartmentDetail() {
      // Given
      Long departmentId = 1L;
      DepartmentVO vo = createTestDepartmentVO(departmentId, "Dept");

      when(departmentDao.selectDepartmentVO(departmentId)).thenReturn(vo);

      // When
      DepartmentVO result = departmentService.getDepartmentById(departmentId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDepartmentName()).isEqualTo("Dept");
    }
  }

  // ==================== getDepartmentPath 測試 ====================

  @Nested
  @DisplayName("getDepartmentPath 查詢部門路徑測試")
  class GetDepartmentPathTest {

    @Test
    @DisplayName("正常情況：應該返回部門路徑")
    void shouldReturnDepartmentPath() {
      // Given
      Long departmentId = 1L;
      Map<Long, String> pathMap = new HashMap<>();
      pathMap.put(1L, "/公司/研發部");

      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(pathMap);

      // When
      String result = departmentService.getDepartmentPath(departmentId);

      // Then
      assertThat(result).isEqualTo("/公司/研發部");
    }
  }

  // ==================== Helper Methods ====================

  private DepartmentAddForm createTestAddForm(String name, Long parentId) {
    DepartmentAddForm form = new DepartmentAddForm();
    form.setDepartmentName(name);
    form.setParentId(parentId);
    return form;
  }

  private DepartmentUpdateForm createTestUpdateForm(Long id, String name, Long parentId) {
    DepartmentUpdateForm form = new DepartmentUpdateForm();
    form.setDepartmentId(id);
    form.setDepartmentName(name);
    form.setParentId(parentId);
    form.setSort(0);
    return form;
  }

  private DepartmentEntity createTestDepartmentEntity(Long id, String name) {
    DepartmentEntity entity = new DepartmentEntity();
    entity.setDepartmentId(id);
    entity.setDepartmentName(name);
    entity.setParentId(0L);
    return entity;
  }

  private DepartmentVO createTestDepartmentVO(Long id, String name) {
    DepartmentVO vo = new DepartmentVO();
    vo.setDepartmentId(id);
    vo.setDepartmentName(name);
    return vo;
  }
}
