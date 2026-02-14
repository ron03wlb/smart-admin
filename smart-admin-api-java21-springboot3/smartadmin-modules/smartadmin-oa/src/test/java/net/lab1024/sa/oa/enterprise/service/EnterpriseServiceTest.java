package net.lab1024.sa.oa.enterprise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseEmployeeDao;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEmployeeEntity;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseEmployeeForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseEmployeeQueryForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseQueryForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseEmployeeVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.oa.enterprise.manager.EnterpriseEmployeeManager;
import net.lab1024.sa.oa.enterprise.manager.EnterpriseManager;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EnterpriseService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>企業 CRUD 操作
 *   <li>企業名稱重複校驗
 *   <li>企業員工管理
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseService 單元測試")
class EnterpriseServiceTest {

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private EnterpriseEmployeeDao enterpriseEmployeeDao;

  @Mock private EnterpriseEmployeeManager enterpriseEmployeeManager;

  @Mock private EnterpriseManager enterpriseManager;

  @Mock private DataTracerService dataTracerService;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @InjectMocks private EnterpriseService enterpriseService;

  // ==================== queryByPage 測試 ====================

  @Nested
  @DisplayName("queryByPage 分頁查詢測試")
  class QueryByPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      EnterpriseQueryForm queryForm = new EnterpriseQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EnterpriseVO enterpriseVO = createTestEnterpriseVO(1L, "測試企業");
      when(enterpriseDao.queryPage(any(), any()))
          .thenReturn(Collections.singletonList(enterpriseVO));

      // When
      ResponseDTO<PageResult<EnterpriseVO>> result = enterpriseService.queryByPage(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(1);
    }
  }

  // ==================== getDetail 測試 ====================

  @Nested
  @DisplayName("getDetail 查詢詳情測試")
  class GetDetailTest {

    @Test
    @DisplayName("正常情況：應該返回企業詳情")
    void shouldReturnEnterpriseDetail() {
      // Given
      Long enterpriseId = 1L;
      EnterpriseVO enterpriseVO = createTestEnterpriseVO(enterpriseId, "測試企業");
      when(enterpriseDao.getDetail(enterpriseId, Boolean.FALSE)).thenReturn(enterpriseVO);

      // When
      EnterpriseVO result = enterpriseService.getDetail(enterpriseId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEnterpriseName()).isEqualTo("測試企業");
    }
  }

  // ==================== createEnterprise 測試 ====================

  @Nested
  @DisplayName("createEnterprise 新增企業測試")
  class CreateEnterpriseTest {

    @Test
    @DisplayName("正常情況：應該成功新增企業")
    void shouldCreateEnterpriseSuccess() {
      // Given
      EnterpriseCreateForm createForm = createTestCreateForm("新企業");
      when(enterpriseDao.queryByEnterpriseName("新企業", null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = enterpriseService.createEnterprise(createForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(enterpriseManager).createEnterpriseTransaction(createForm);
    }

    @Test
    @DisplayName("異常情況：企業名稱重複時應返回錯誤")
    void shouldReturnErrorWhenNameDuplicate() {
      // Given
      EnterpriseCreateForm createForm = createTestCreateForm("重複企業");
      EnterpriseEntity existingEnterprise = createTestEnterpriseEntity(1L, "重複企業");

      when(enterpriseDao.queryByEnterpriseName("重複企業", null, Boolean.FALSE))
          .thenReturn(existingEnterprise);

      // When
      ResponseDTO<String> result = enterpriseService.createEnterprise(createForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业名称重复");
      verify(enterpriseManager, never()).createEnterpriseTransaction(any());
    }
  }

  // ==================== updateEnterprise 測試 ====================

  @Nested
  @DisplayName("updateEnterprise 更新企業測試")
  class UpdateEnterpriseTest {

    @Test
    @DisplayName("正常情況：應該成功更新企業")
    void shouldUpdateEnterpriseSuccess() {
      // Given
      EnterpriseUpdateForm updateForm = createTestUpdateForm(1L, "更新後企業");
      EnterpriseEntity existingEntity = createTestEnterpriseEntity(1L, "原企業");

      when(enterpriseDao.selectById(1L)).thenReturn(existingEntity);
      when(enterpriseDao.queryByEnterpriseName("更新後企業", 1L, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = enterpriseService.updateEnterprise(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(enterpriseManager).updateEnterpriseTransaction(updateForm, existingEntity);
    }

    @Test
    @DisplayName("異常情況：企業不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      EnterpriseUpdateForm updateForm = createTestUpdateForm(999L, "企業");
      when(enterpriseDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = enterpriseService.updateEnterprise(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业不存在");
    }

    @Test
    @DisplayName("異常情況：企業已刪除時應返回錯誤")
    void shouldReturnErrorWhenDeleted() {
      // Given
      EnterpriseUpdateForm updateForm = createTestUpdateForm(1L, "企業");
      EnterpriseEntity deletedEntity = createTestEnterpriseEntity(1L, "已刪除");
      deletedEntity.setDeletedFlag(true);

      when(enterpriseDao.selectById(1L)).thenReturn(deletedEntity);

      // When
      ResponseDTO<String> result = enterpriseService.updateEnterprise(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("企业不存在");
    }
  }

  // ==================== deleteEnterprise 測試 ====================

  @Nested
  @DisplayName("deleteEnterprise 刪除企業測試")
  class DeleteEnterpriseTest {

    @Test
    @DisplayName("正常情況：應該成功刪除企業")
    void shouldDeleteEnterpriseSuccess() {
      // Given
      Long enterpriseId = 1L;
      EnterpriseEntity existingEntity = createTestEnterpriseEntity(enterpriseId, "企業");

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = enterpriseService.deleteEnterprise(enterpriseId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(enterpriseManager).deleteEnterpriseTransaction(enterpriseId);
    }

    @Test
    @DisplayName("異常情況：企業不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long enterpriseId = 999L;
      when(enterpriseDao.selectById(enterpriseId)).thenReturn(null);

      // When
      ResponseDTO<String> result = enterpriseService.deleteEnterprise(enterpriseId);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(enterpriseManager, never()).deleteEnterpriseTransaction(any());
    }
  }

  // ==================== queryList 測試 ====================

  @Nested
  @DisplayName("queryList 列表查詢測試")
  class QueryListTest {

    @Test
    @DisplayName("正常情況：應該返回企業列表")
    void shouldReturnEnterpriseList() {
      // Given
      Integer type = 1;
      EnterpriseListVO listVO = new EnterpriseListVO();
      listVO.setEnterpriseId(1L);
      listVO.setEnterpriseName("企業1");

      when(enterpriseDao.queryList(type, Boolean.FALSE, Boolean.FALSE))
          .thenReturn(Collections.singletonList(listVO));

      // When
      ResponseDTO<List<EnterpriseListVO>> result = enterpriseService.queryList(type);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ==================== addEmployee 測試 ====================

  @Nested
  @DisplayName("addEmployee 添加員工測試")
  class AddEmployeeTest {

    @Test
    @DisplayName("正常情況：應該成功添加員工")
    void shouldAddEmployeeSuccess() {
      // Given
      EnterpriseEmployeeForm form = createTestEmployeeForm(1L, Arrays.asList(1L, 2L));
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "企業");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(eq(1L), anyList()))
          .thenReturn(Collections.emptyList());
      when(enterpriseEmployeeManager.saveBatch(anyList())).thenReturn(true);

      // When
      ResponseDTO<String> result = enterpriseService.addEmployee(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(enterpriseEmployeeManager).saveBatch(anyList());
    }

    @Test
    @DisplayName("異常情況：企業不存在時應返回錯誤")
    void shouldReturnErrorWhenEnterpriseNotFound() {
      // Given
      EnterpriseEmployeeForm form = createTestEmployeeForm(999L, Arrays.asList(1L));
      when(enterpriseDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = enterpriseService.addEmployee(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("邊界情況：員工已存在時應過濾重複")
    void shouldFilterExistingEmployees() {
      // Given
      EnterpriseEmployeeForm form = createTestEmployeeForm(1L, Arrays.asList(1L, 2L));
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "企業");

      EnterpriseEmployeeEntity existingEmployee = new EnterpriseEmployeeEntity();
      existingEmployee.setEnterpriseId(1L);
      existingEmployee.setEmployeeId(1L);

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);
      when(enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(eq(1L), anyList()))
          .thenReturn(Collections.singletonList(existingEmployee));
      when(enterpriseEmployeeManager.saveBatch(anyList())).thenReturn(true);

      // When
      ResponseDTO<String> result = enterpriseService.addEmployee(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== deleteEmployee 測試 ====================

  @Nested
  @DisplayName("deleteEmployee 刪除員工測試")
  class DeleteEmployeeTest {

    @Test
    @DisplayName("正常情況：應該成功刪除員工")
    void shouldDeleteEmployeeSuccess() {
      // Given
      EnterpriseEmployeeForm form = createTestEmployeeForm(1L, Arrays.asList(1L, 2L));
      EnterpriseEntity enterprise = createTestEnterpriseEntity(1L, "企業");

      when(enterpriseDao.selectById(1L)).thenReturn(enterprise);

      // When
      ResponseDTO<String> result = enterpriseService.deleteEmployee(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(enterpriseEmployeeDao).deleteByEnterpriseAndEmployeeIdList(eq(1L), anyList());
    }
  }

  // ==================== employeeList 測試 ====================

  @Nested
  @DisplayName("employeeList 員工列表測試")
  class EmployeeListTest {

    @Test
    @DisplayName("正常情況：應該返回員工列表")
    void shouldReturnEmployeeList() {
      // Given
      List<Long> enterpriseIdList = Arrays.asList(1L, 2L);
      EnterpriseEmployeeVO employeeVO = new EnterpriseEmployeeVO();
      employeeVO.setEmployeeId(1L);

      when(enterpriseEmployeeDao.selectByEnterpriseIdList(enterpriseIdList))
          .thenReturn(Collections.singletonList(employeeVO));

      // When
      List<EnterpriseEmployeeVO> result = enterpriseService.employeeList(enterpriseIdList);

      // Then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("邊界情況：空列表應返回空結果")
    void shouldReturnEmptyWhenListEmpty() {
      // When
      List<EnterpriseEmployeeVO> result = enterpriseService.employeeList(Collections.emptyList());

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== queryPageEmployeeList 測試 ====================

  @Nested
  @DisplayName("queryPageEmployeeList 分頁查詢員工測試")
  class QueryPageEmployeeListTest {

    @Test
    @DisplayName("正常情況：應該返回分頁員工列表")
    void shouldReturnPagedEmployeeList() {
      // Given
      EnterpriseEmployeeQueryForm queryForm = new EnterpriseEmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setEnterpriseId(1L);

      EnterpriseEmployeeVO employeeVO = new EnterpriseEmployeeVO();
      employeeVO.setEmployeeId(1L);
      employeeVO.setDepartmentId(1L);

      Map<Long, String> pathMap = new HashMap<>();
      pathMap.put(1L, "/公司/研發部");

      when(enterpriseEmployeeDao.queryPageEmployeeList(any(), any()))
          .thenReturn(Collections.singletonList(employeeVO));
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(pathMap);

      // When
      PageResult<EnterpriseEmployeeVO> result = enterpriseService.queryPageEmployeeList(queryForm);

      // Then
      assertThat(result.getList()).hasSize(1);
      assertThat(result.getList().get(0).getDepartmentName()).isEqualTo("/公司/研發部");
    }
  }

  // ==================== Helper Methods ====================

  private EnterpriseVO createTestEnterpriseVO(Long id, String name) {
    EnterpriseVO vo = new EnterpriseVO();
    vo.setEnterpriseId(id);
    vo.setEnterpriseName(name);
    return vo;
  }

  private EnterpriseEntity createTestEnterpriseEntity(Long id, String name) {
    EnterpriseEntity entity = new EnterpriseEntity();
    entity.setEnterpriseId(id);
    entity.setEnterpriseName(name);
    entity.setDeletedFlag(false);
    return entity;
  }

  private EnterpriseCreateForm createTestCreateForm(String name) {
    EnterpriseCreateForm form = new EnterpriseCreateForm();
    form.setEnterpriseName(name);
    return form;
  }

  private EnterpriseUpdateForm createTestUpdateForm(Long id, String name) {
    EnterpriseUpdateForm form = new EnterpriseUpdateForm();
    form.setEnterpriseId(id);
    form.setEnterpriseName(name);
    return form;
  }

  private EnterpriseEmployeeForm createTestEmployeeForm(Long enterpriseId, List<Long> employeeIds) {
    EnterpriseEmployeeForm form = new EnterpriseEmployeeForm();
    form.setEnterpriseId(enterpriseId);
    form.setEmployeeIdList(employeeIds);
    return form;
  }
}
