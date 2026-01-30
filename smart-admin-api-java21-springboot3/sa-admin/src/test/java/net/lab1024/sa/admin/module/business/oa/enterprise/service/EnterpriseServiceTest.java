package net.lab1024.sa.admin.module.business.oa.enterprise.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;
import net.lab1024.sa.admin.module.business.oa.enterprise.EnterpriseTestFixture;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseEmployeeDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEmployeeEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseEmployeeVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseExcelVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.manager.EnterpriseEmployeeManager;
import net.lab1024.sa.admin.module.business.oa.enterprise.manager.EnterpriseManager;
import net.lab1024.sa.admin.module.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
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
 * EnterpriseService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>企业CRUD操作（创建、查询、更新、删除）
 *   <li>企业员工关系管理（添加、删除、查询）
 *   <li>业务规则验证（企业名称唯一性、企业存在性、员工重复过滤）
 *   <li>分页查询和列表查询
 *   <li>导出功能
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnterpriseService 单元测试")
class EnterpriseServiceTest {

  @Mock private EnterpriseDao enterpriseDao;

  @Mock private EnterpriseEmployeeDao enterpriseEmployeeDao;

  @Mock private EnterpriseEmployeeManager enterpriseEmployeeManager;

  @Mock private EnterpriseManager enterpriseManager;

  @Mock private DataTracerService dataTracerService;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @InjectMocks private EnterpriseService enterpriseService;

  @BeforeEach
  void setUp() {
    EnterpriseTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("queryByPage() - 分页查询企业")
  class QueryByPageTests {

    @Test
    @DisplayName("正常分页查询 - 应返回分页结果")
    void queryByPage_ValidForm_ShouldReturnPageResult() {
      // Arrange
      EnterpriseQueryForm form = EnterpriseTestFixture.createQueryForm();
      List<EnterpriseVO> voList = EnterpriseTestFixture.createVOList(5);
      Page<EnterpriseVO> page = new Page<>(1, 10);
      page.setRecords(voList);
      page.setTotal(5);

      when(enterpriseDao.queryPage(any(Page.class), eq(form))).thenReturn(voList);

      // Act
      ResponseDTO<PageResult<EnterpriseVO>> response = enterpriseService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(5, response.getData().getList().size());
      assertEquals(Boolean.FALSE, form.getDeletedFlag());
      verify(enterpriseDao, times(1)).queryPage(any(Page.class), eq(form));
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryByPage_NoResults_ShouldReturnEmptyList() {
      // Arrange
      EnterpriseQueryForm form = EnterpriseTestFixture.createQueryForm();
      List<EnterpriseVO> emptyList = Collections.emptyList();

      when(enterpriseDao.queryPage(any(Page.class), eq(form))).thenReturn(emptyList);

      // Act
      ResponseDTO<PageResult<EnterpriseVO>> response = enterpriseService.queryByPage(form);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().getList().isEmpty());
    }
  }

  @Nested
  @DisplayName("getExcelExportData() - 获取导出数据")
  class GetExcelExportDataTests {

    @Test
    @DisplayName("正常导出 - 应返回导出数据")
    void getExcelExportData_ValidForm_ShouldReturnExcelData() {
      // Arrange
      EnterpriseQueryForm form = EnterpriseTestFixture.createQueryForm();
      List<EnterpriseExcelVO> excelData =
          Arrays.asList(
              EnterpriseTestFixture.createExcelVO(),
              EnterpriseTestFixture.createExcelVO(),
              EnterpriseTestFixture.createExcelVO());

      when(enterpriseDao.selectExcelExportData(form)).thenReturn(excelData);

      // Act
      List<EnterpriseExcelVO> result = enterpriseService.getExcelExportData(form);

      // Assert
      assertNotNull(result);
      assertEquals(3, result.size());
      assertEquals(Boolean.FALSE, form.getDeletedFlag());
      verify(enterpriseDao, times(1)).selectExcelExportData(form);
    }
  }

  @Nested
  @DisplayName("getDetail() - 查询企业详情")
  class GetDetailTests {

    @Test
    @DisplayName("正常查询详情 - 应返回企业详情")
    void getDetail_ExistingEnterpriseId_ShouldReturnDetail() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseVO vo = EnterpriseTestFixture.createVO(enterpriseId);

      when(enterpriseDao.getDetail(enterpriseId, Boolean.FALSE)).thenReturn(vo);

      // Act
      EnterpriseVO result = enterpriseService.getDetail(enterpriseId);

      // Assert
      assertNotNull(result);
      assertEquals(enterpriseId, result.getEnterpriseId());
      verify(enterpriseDao, times(1)).getDetail(enterpriseId, Boolean.FALSE);
    }

    @Test
    @DisplayName("企业不存在 - 应返回null")
    void getDetail_NonExistingEnterpriseId_ShouldReturnNull() {
      // Arrange
      Long enterpriseId = 999L;

      when(enterpriseDao.getDetail(enterpriseId, Boolean.FALSE)).thenReturn(null);

      // Act
      EnterpriseVO result = enterpriseService.getDetail(enterpriseId);

      // Assert
      assertNull(result);
    }
  }

  @Nested
  @DisplayName("createEnterprise() - 新建企业")
  class CreateEnterpriseTests {

    @Test
    @DisplayName("正常创建企业 - 应返回成功")
    void createEnterprise_ValidForm_ShouldReturnSuccess() {
      // Arrange
      EnterpriseCreateForm form = EnterpriseTestFixture.createCreateForm();

      when(enterpriseDao.queryByEnterpriseName(form.getEnterpriseName(), null, Boolean.FALSE))
          .thenReturn(null);
      doNothing().when(enterpriseManager).createEnterpriseTransaction(form);

      // Act
      ResponseDTO<String> response = enterpriseService.createEnterprise(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1))
          .queryByEnterpriseName(form.getEnterpriseName(), null, Boolean.FALSE);
      verify(enterpriseManager, times(1)).createEnterpriseTransaction(form);
    }

    @Test
    @DisplayName("企业名称重复 - 应返回错误")
    void createEnterprise_DuplicateName_ShouldReturnError() {
      // Arrange
      EnterpriseCreateForm form = EnterpriseTestFixture.createCreateForm();
      EnterpriseEntity existingEnterprise = EnterpriseTestFixture.createEntity();

      when(enterpriseDao.queryByEnterpriseName(form.getEnterpriseName(), null, Boolean.FALSE))
          .thenReturn(existingEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.createEnterprise(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业名称重复", response.getMsg());
      verify(enterpriseManager, never()).createEnterpriseTransaction(any());
    }
  }

  @Nested
  @DisplayName("updateEnterprise() - 编辑企业")
  class UpdateEnterpriseTests {

    @Test
    @DisplayName("正常更新企业 - 应返回成功")
    void updateEnterprise_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseUpdateForm form = EnterpriseTestFixture.createUpdateForm(enterpriseId);
      EnterpriseEntity existingEnterprise = EnterpriseTestFixture.createEntity();
      existingEnterprise.setEnterpriseId(enterpriseId);
      existingEnterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(existingEnterprise);
      when(enterpriseDao.queryByEnterpriseName(
              form.getEnterpriseName(), enterpriseId, Boolean.FALSE))
          .thenReturn(null);
      doNothing().when(enterpriseManager).updateEnterpriseTransaction(form, existingEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.updateEnterprise(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(enterpriseId);
      verify(enterpriseDao, times(1))
          .queryByEnterpriseName(form.getEnterpriseName(), enterpriseId, Boolean.FALSE);
      verify(enterpriseManager, times(1)).updateEnterpriseTransaction(form, existingEnterprise);
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void updateEnterprise_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 999L;
      EnterpriseUpdateForm form = EnterpriseTestFixture.createUpdateForm(enterpriseId);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = enterpriseService.updateEnterprise(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(enterpriseManager, never()).updateEnterpriseTransaction(any(), any());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void updateEnterprise_EnterpriseDeleted_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseUpdateForm form = EnterpriseTestFixture.createUpdateForm(enterpriseId);
      EnterpriseEntity deletedEnterprise = EnterpriseTestFixture.createEntity();
      deletedEnterprise.setEnterpriseId(enterpriseId);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.updateEnterprise(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(enterpriseManager, never()).updateEnterpriseTransaction(any(), any());
    }

    @Test
    @DisplayName("企业名称重复 - 应返回错误")
    void updateEnterprise_DuplicateName_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseUpdateForm form = EnterpriseTestFixture.createUpdateForm(enterpriseId);
      EnterpriseEntity existingEnterprise = EnterpriseTestFixture.createEntity();
      existingEnterprise.setEnterpriseId(enterpriseId);
      existingEnterprise.setDeletedFlag(false);
      EnterpriseEntity duplicateEnterprise = EnterpriseTestFixture.createEntity();
      duplicateEnterprise.setEnterpriseId(200L); // 不同ID但同名

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(existingEnterprise);
      when(enterpriseDao.queryByEnterpriseName(
              form.getEnterpriseName(), enterpriseId, Boolean.FALSE))
          .thenReturn(duplicateEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.updateEnterprise(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业名称重复", response.getMsg());
      verify(enterpriseManager, never()).updateEnterpriseTransaction(any(), any());
    }
  }

  @Nested
  @DisplayName("deleteEnterprise() - 删除企业")
  class DeleteEnterpriseTests {

    @Test
    @DisplayName("正常删除企业 - 应返回成功")
    void deleteEnterprise_ValidId_ShouldReturnSuccess() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseEntity existingEnterprise = EnterpriseTestFixture.createEntity();
      existingEnterprise.setEnterpriseId(enterpriseId);
      existingEnterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(existingEnterprise);
      doNothing().when(enterpriseManager).deleteEnterpriseTransaction(enterpriseId);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEnterprise(enterpriseId);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(enterpriseId);
      verify(enterpriseManager, times(1)).deleteEnterpriseTransaction(enterpriseId);
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void deleteEnterprise_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 999L;

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEnterprise(enterpriseId);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(enterpriseManager, never()).deleteEnterpriseTransaction(anyLong());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void deleteEnterprise_EnterpriseAlreadyDeleted_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseEntity deletedEnterprise = EnterpriseTestFixture.createEntity();
      deletedEnterprise.setEnterpriseId(enterpriseId);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEnterprise(enterpriseId);

      // Assert
      assertFalse(response.getOk());
      assertEquals("企业不存在", response.getMsg());
      verify(enterpriseManager, never()).deleteEnterpriseTransaction(anyLong());
    }
  }

  @Nested
  @DisplayName("queryList() - 企业列表查询")
  class QueryListTests {

    @Test
    @DisplayName("正常查询列表 - 应返回企业列表")
    void queryList_ValidType_ShouldReturnList() {
      // Arrange
      Integer type = 1;
      List<EnterpriseListVO> voList =
          Arrays.asList(
              EnterpriseTestFixture.createListVO(100L),
              EnterpriseTestFixture.createListVO(101L),
              EnterpriseTestFixture.createListVO(102L));

      when(enterpriseDao.queryList(type, Boolean.FALSE, Boolean.FALSE)).thenReturn(voList);

      // Act
      ResponseDTO<List<EnterpriseListVO>> response = enterpriseService.queryList(type);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(3, response.getData().size());
      verify(enterpriseDao, times(1)).queryList(type, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    @DisplayName("查询所有类型 - type为null应返回所有企业")
    void queryList_NullType_ShouldReturnAllEnterprises() {
      // Arrange
      Integer type = null;
      List<EnterpriseListVO> voList =
          Arrays.asList(
              EnterpriseTestFixture.createListVO(100L), EnterpriseTestFixture.createListVO(101L));

      when(enterpriseDao.queryList(type, Boolean.FALSE, Boolean.FALSE)).thenReturn(voList);

      // Act
      ResponseDTO<List<EnterpriseListVO>> response = enterpriseService.queryList(type);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(2, response.getData().size());
    }
  }

  @Nested
  @DisplayName("addEmployee() - 企业添加员工")
  class AddEmployeeTests {

    @Test
    @DisplayName("正常添加员工 - 应返回成功")
    void addEmployee_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L, 3L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity enterprise = EnterpriseTestFixture.createEntity();
      enterprise.setEnterpriseId(enterpriseId);
      enterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(enterprise);
      when(enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds))
          .thenReturn(Collections.emptyList());
      when(enterpriseEmployeeManager.saveBatch(anyList())).thenReturn(true);

      // Act
      ResponseDTO<String> response = enterpriseService.addEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(enterpriseId);
      verify(enterpriseEmployeeDao, times(1))
          .selectByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds);
      verify(enterpriseEmployeeManager, times(1)).saveBatch(anyList());
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void addEmployee_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 999L;
      List<Long> employeeIds = Arrays.asList(1L, 2L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = enterpriseService.addEmployee(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
      verify(enterpriseEmployeeManager, never()).saveBatch(anyList());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void addEmployee_EnterpriseDeleted_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity deletedEnterprise = EnterpriseTestFixture.createEntity();
      deletedEnterprise.setEnterpriseId(enterpriseId);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.addEmployee(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
      verify(enterpriseEmployeeManager, never()).saveBatch(anyList());
    }

    @Test
    @DisplayName("部分员工已存在 - 应过滤已存在员工并添加新员工")
    void addEmployee_PartialEmployeesExist_ShouldFilterAndAddNew() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L, 3L, 4L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity enterprise = EnterpriseTestFixture.createEntity();
      enterprise.setEnterpriseId(enterpriseId);
      enterprise.setDeletedFlag(false);

      // 假设员工1和2已存在
      List<EnterpriseEmployeeEntity> existingEmployees =
          Arrays.asList(
              EnterpriseTestFixture.createEmployeeEntity(enterpriseId, 1L),
              EnterpriseTestFixture.createEmployeeEntity(enterpriseId, 2L));

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(enterprise);
      when(enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds))
          .thenReturn(existingEmployees);
      when(enterpriseEmployeeManager.saveBatch(anyList())).thenReturn(true);

      // Act
      ResponseDTO<String> response = enterpriseService.addEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseEmployeeManager, times(1))
          .saveBatch(argThat(list -> list.size() == 2)); // 只添加员工3和4
    }

    @Test
    @DisplayName("所有员工已存在 - 应返回成功但不执行添加")
    void addEmployee_AllEmployeesExist_ShouldReturnSuccessWithoutSaving() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity enterprise = EnterpriseTestFixture.createEntity();
      enterprise.setEnterpriseId(enterpriseId);
      enterprise.setDeletedFlag(false);

      List<EnterpriseEmployeeEntity> allExistingEmployees =
          Arrays.asList(
              EnterpriseTestFixture.createEmployeeEntity(enterpriseId, 1L),
              EnterpriseTestFixture.createEmployeeEntity(enterpriseId, 2L));

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(enterprise);
      when(enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds))
          .thenReturn(allExistingEmployees);

      // Act
      ResponseDTO<String> response = enterpriseService.addEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseEmployeeManager, never()).saveBatch(anyList());
    }
  }

  @Nested
  @DisplayName("deleteEmployee() - 企业删除员工")
  class DeleteEmployeeTests {

    @Test
    @DisplayName("正常删除员工 - 应返回成功")
    void deleteEmployee_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L, 3L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity enterprise = EnterpriseTestFixture.createEntity();
      enterprise.setEnterpriseId(enterpriseId);
      enterprise.setDeletedFlag(false);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(enterprise);
      doNothing()
          .when(enterpriseEmployeeDao)
          .deleteByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEmployee(form);

      // Assert
      assertTrue(response.getOk());
      verify(enterpriseDao, times(1)).selectById(enterpriseId);
      verify(enterpriseEmployeeDao, times(1))
          .deleteByEnterpriseAndEmployeeIdList(enterpriseId, employeeIds);
    }

    @Test
    @DisplayName("企业不存在 - 应返回错误")
    void deleteEmployee_EnterpriseNotFound_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 999L;
      List<Long> employeeIds = Arrays.asList(1L, 2L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEmployee(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
      verify(enterpriseEmployeeDao, never())
          .deleteByEnterpriseAndEmployeeIdList(anyLong(), anyList());
    }

    @Test
    @DisplayName("企业已删除 - 应返回错误")
    void deleteEmployee_EnterpriseDeleted_ShouldReturnError() {
      // Arrange
      Long enterpriseId = 100L;
      List<Long> employeeIds = Arrays.asList(1L, 2L);
      EnterpriseEmployeeForm form =
          EnterpriseTestFixture.createEmployeeForm(enterpriseId, employeeIds);
      EnterpriseEntity deletedEnterprise = EnterpriseTestFixture.createEntity();
      deletedEnterprise.setEnterpriseId(enterpriseId);
      deletedEnterprise.setDeletedFlag(true);

      when(enterpriseDao.selectById(enterpriseId)).thenReturn(deletedEnterprise);

      // Act
      ResponseDTO<String> response = enterpriseService.deleteEmployee(form);

      // Assert
      assertFalse(response.getOk());
      assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
      verify(enterpriseEmployeeDao, never())
          .deleteByEnterpriseAndEmployeeIdList(anyLong(), anyList());
    }
  }

  @Nested
  @DisplayName("employeeList() - 企业下员工列表")
  class EmployeeListTests {

    @Test
    @DisplayName("正常查询员工列表 - 应返回员工列表")
    void employeeList_ValidEnterpriseIds_ShouldReturnEmployeeList() {
      // Arrange
      List<Long> enterpriseIds = Arrays.asList(100L, 101L);
      List<EnterpriseEmployeeVO> voList =
          Arrays.asList(
              EnterpriseTestFixture.createEmployeeVO(100L, 1L),
              EnterpriseTestFixture.createEmployeeVO(100L, 2L),
              EnterpriseTestFixture.createEmployeeVO(101L, 3L));

      when(enterpriseEmployeeDao.selectByEnterpriseIdList(enterpriseIds)).thenReturn(voList);

      // Act
      List<EnterpriseEmployeeVO> result = enterpriseService.employeeList(enterpriseIds);

      // Assert
      assertNotNull(result);
      assertEquals(3, result.size());
      verify(enterpriseEmployeeDao, times(1)).selectByEnterpriseIdList(enterpriseIds);
    }

    @Test
    @DisplayName("空企业ID列表 - 应返回空列表")
    void employeeList_EmptyEnterpriseIds_ShouldReturnEmptyList() {
      // Arrange
      List<Long> emptyIds = Collections.emptyList();

      // Act
      List<EnterpriseEmployeeVO> result = enterpriseService.employeeList(emptyIds);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(enterpriseEmployeeDao, never()).selectByEnterpriseIdList(anyList());
    }

    @Test
    @DisplayName("null企业ID列表 - 应返回空列表")
    void employeeList_NullEnterpriseIds_ShouldReturnEmptyList() {
      // Arrange & Act
      List<EnterpriseEmployeeVO> result = enterpriseService.employeeList(null);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(enterpriseEmployeeDao, never()).selectByEnterpriseIdList(anyList());
    }
  }

  @Nested
  @DisplayName("queryPageEmployeeList() - 分页查询企业员工")
  class QueryPageEmployeeListTests {

    @Test
    @DisplayName("正常分页查询员工 - 应返回分页结果")
    void queryPageEmployeeList_ValidForm_ShouldReturnPageResult() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseEmployeeQueryForm form =
          EnterpriseTestFixture.createEmployeeQueryForm(enterpriseId);
      List<EnterpriseEmployeeVO> voList =
          Arrays.asList(
              EnterpriseTestFixture.createEmployeeVO(enterpriseId, 1L),
              EnterpriseTestFixture.createEmployeeVO(enterpriseId, 2L),
              EnterpriseTestFixture.createEmployeeVO(enterpriseId, 3L));
      Map<Long, String> departmentPathMap = new HashMap<>();
      departmentPathMap.put(1L, "部门路径1");
      departmentPathMap.put(2L, "部门路径2");

      when(enterpriseEmployeeDao.queryPageEmployeeList(any(Page.class), eq(form)))
          .thenReturn(voList);
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(departmentPathMap);

      // Act
      PageResult<EnterpriseEmployeeVO> result = enterpriseService.queryPageEmployeeList(form);

      // Assert
      assertNotNull(result);
      assertEquals(3, result.getList().size());
      verify(enterpriseEmployeeDao, times(1)).queryPageEmployeeList(any(Page.class), eq(form));
      verify(departmentCacheManager, times(1)).getDepartmentPathMap();
    }

    @Test
    @DisplayName("空结果查询 - 应返回空分页结果")
    void queryPageEmployeeList_NoResults_ShouldReturnEmptyPageResult() {
      // Arrange
      Long enterpriseId = 100L;
      EnterpriseEmployeeQueryForm form =
          EnterpriseTestFixture.createEmployeeQueryForm(enterpriseId);
      List<EnterpriseEmployeeVO> emptyList = Collections.emptyList();
      Map<Long, String> departmentPathMap = new HashMap<>();

      when(enterpriseEmployeeDao.queryPageEmployeeList(any(Page.class), eq(form)))
          .thenReturn(emptyList);
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(departmentPathMap);

      // Act
      PageResult<EnterpriseEmployeeVO> result = enterpriseService.queryPageEmployeeList(form);

      // Assert
      assertNotNull(result);
      assertTrue(result.getList().isEmpty());
    }
  }
}
