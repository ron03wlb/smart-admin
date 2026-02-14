package net.lab1024.sa.system.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.securityprotect.service.SecurityPasswordService;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.system.employee.domain.form.EmployeeBatchUpdateDepartmentForm;
import net.lab1024.sa.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.system.employee.domain.form.EmployeeUpdateForm;
import net.lab1024.sa.system.employee.domain.form.EmployeeUpdatePasswordForm;
import net.lab1024.sa.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.system.employee.manager.EmployeeManager;
import net.lab1024.sa.system.login.domain.RequestEmployee;
import net.lab1024.sa.system.login.manager.LoginManager;
import net.lab1024.sa.system.position.dao.PositionDao;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
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
 * EmployeeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>員工查詢（分頁、全部）
 *   <li>員工新增（校驗重複）
 *   <li>員工更新（基本信息、頭像、密碼）
 *   <li>員工禁用/啟用
 *   <li>批量操作（刪除、更新部門）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmployeeService 單元測試")
class EmployeeServiceTest {

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentDao departmentDao;

  @Mock private EmployeeManager employeeManager;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @Mock private SecurityPasswordService securityPasswordService;

  @Mock private LoginManager loginManager;

  @Mock private PositionDao positionDao;

  @InjectMocks private EmployeeService employeeService;

  // ==================== getById 測試 ====================

  @Nested
  @DisplayName("getById 根據ID查詢測試")
  class GetByIdTest {

    @Test
    @DisplayName("正常情況：應該返回員工實體")
    void shouldReturnEmployee() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "admin");
      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      EmployeeEntity result = employeeService.getById(employeeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getEmployeeId()).isEqualTo(employeeId);
    }
  }

  // ==================== queryEmployee 測試 ====================

  @Nested
  @DisplayName("queryEmployee 分頁查詢測試")
  class QueryEmployeeTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeVO employeeVO = new EmployeeVO();
      employeeVO.setEmployeeId(1L);
      employeeVO.setActualName("員工1");

      when(employeeDao.queryEmployee(any(), any(), anyList()))
          .thenReturn(Collections.singletonList(employeeVO));
      when(roleEmployeeDao.selectRoleByEmployeeIdList(anyList()))
          .thenReturn(Collections.emptyList());
      when(departmentCacheManager.getDepartmentPathMap()).thenReturn(Collections.emptyMap());

      // When
      ResponseDTO<PageResult<EmployeeVO>> result = employeeService.queryEmployee(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(1);
    }

    @Test
    @DisplayName("正常情況：指定部門ID時應該查詢部門及子部門")
    void shouldQueryWithDepartmentHierarchy() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setDepartmentId(1L);

      when(departmentCacheManager.getDepartmentSelfAndChildren(1L)).thenReturn(List.of(1L, 2L, 3L));
      when(employeeDao.queryEmployee(any(), any(), anyList())).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<EmployeeVO>> result = employeeService.queryEmployee(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(departmentCacheManager).getDepartmentSelfAndChildren(1L);
    }

    @Test
    @DisplayName("邊界情況：無結果時應返回空列表")
    void shouldReturnEmptyListWhenNoResult() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(employeeDao.queryEmployee(any(), any(), anyList())).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<EmployeeVO>> result = employeeService.queryEmployee(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).isEmpty();
    }
  }

  // ==================== addEmployee 測試 ====================

  @Nested
  @DisplayName("addEmployee 新增員工測試")
  class AddEmployeeTest {

    @Test
    @DisplayName("異常情況：登錄名重複時應返回錯誤")
    void shouldReturnErrorWhenLoginNameDuplicate() {
      // Given
      EmployeeAddForm addForm = createTestAddForm("admin", "13800138000");
      EmployeeEntity existingEmployee = createTestEmployee(999L, "admin");

      when(employeeDao.getByLoginName("admin", null)).thenReturn(existingEmployee);

      // When
      ResponseDTO<String> result = employeeService.addEmployee(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("登录名重复");
    }

    @Test
    @DisplayName("異常情況：手機號重複時應返回錯誤")
    void shouldReturnErrorWhenPhoneDuplicate() {
      // Given
      EmployeeAddForm addForm = createTestAddForm("newuser", "13800138000");
      EmployeeEntity existingEmployee = createTestEmployee(999L, "other");

      when(employeeDao.getByLoginName("newuser", null)).thenReturn(null);
      when(employeeDao.getByPhone("13800138000", null)).thenReturn(existingEmployee);

      // When
      ResponseDTO<String> result = employeeService.addEmployee(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("手机号已存在");
    }

    @Test
    @DisplayName("異常情況：部門不存在時應返回錯誤")
    void shouldReturnErrorWhenDepartmentNotFound() {
      // Given
      EmployeeAddForm addForm = createTestAddForm("newuser", "13800138000");
      addForm.setDepartmentId(999L);

      when(employeeDao.getByLoginName("newuser", null)).thenReturn(null);
      when(employeeDao.getByPhone("13800138000", null)).thenReturn(null);
      when(departmentDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.addEmployee(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("部门不存在");
    }

    @Test
    @DisplayName("正常情況：應該成功新增員工並返回密碼")
    void shouldAddEmployeeSuccess() {
      // Given
      EmployeeAddForm addForm = createTestAddForm("newuser", "13800138000");
      addForm.setDepartmentId(1L);

      DepartmentEntity department = new DepartmentEntity();
      department.setDepartmentId(1L);

      when(employeeDao.getByLoginName("newuser", null)).thenReturn(null);
      when(employeeDao.getByPhone("13800138000", null)).thenReturn(null);
      when(departmentDao.selectById(1L)).thenReturn(department);
      when(securityPasswordService.randomPassword()).thenReturn("RandomPwd123");
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encryptedPwd");

      // When
      ResponseDTO<String> result = employeeService.addEmployee(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEqualTo("RandomPwd123");
      verify(employeeManager).saveEmployeeTransaction(any(), anyList());
    }
  }

  // ==================== updateEmployee 測試 ====================

  @Nested
  @DisplayName("updateEmployee 更新員工測試")
  class UpdateEmployeeTest {

    @Test
    @DisplayName("異常情況：員工不存在時應返回錯誤")
    void shouldReturnErrorWhenEmployeeNotFound() {
      // Given
      EmployeeUpdateForm updateForm = createTestUpdateForm(999L, "admin");
      when(employeeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.updateEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("異常情況：部門不存在時應返回錯誤")
    void shouldReturnErrorWhenDepartmentNotFoundOnUpdate() {
      // Given
      EmployeeUpdateForm updateForm = createTestUpdateForm(1L, "admin");
      updateForm.setDepartmentId(999L);

      EmployeeEntity employee = createTestEmployee(1L, "admin");
      when(employeeDao.selectById(1L)).thenReturn(employee);
      when(departmentDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.updateEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("部门不存在");
    }

    @Test
    @DisplayName("正常情況：應該成功更新員工")
    void shouldUpdateEmployeeSuccess() {
      // Given
      EmployeeUpdateForm updateForm = createTestUpdateForm(1L, "admin");
      updateForm.setDepartmentId(1L);
      updateForm.setPhone("13800138001");
      updateForm.setEmail("admin@example.com");

      EmployeeEntity employee = createTestEmployee(1L, "admin");
      DepartmentEntity department = new DepartmentEntity();
      department.setDepartmentId(1L);

      when(employeeDao.selectById(1L)).thenReturn(employee);
      when(departmentDao.selectById(1L)).thenReturn(department);
      when(employeeDao.getByLoginName(anyString(), any())).thenReturn(null);
      when(employeeDao.getByPhone(anyString(), any())).thenReturn(null);
      when(employeeDao.getByEmail(anyString(), any())).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.updateEmployee(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(employeeManager).updateEmployeeTransaction(any(), anyList());
      verify(loginManager).clearUserPermission(1L);
      verify(loginManager).clearUserLoginInfo(1L);
    }
  }

  // ==================== updateDisableFlag 測試 ====================

  @Nested
  @DisplayName("updateDisableFlag 禁用/啟用測試")
  class UpdateDisableFlagTest {

    @Test
    @DisplayName("異常情況：員工ID為null時應返回錯誤")
    void shouldReturnErrorWhenEmployeeIdNull() {
      // When
      ResponseDTO<String> result = employeeService.updateDisableFlag(null);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("異常情況：員工不存在時應返回錯誤")
    void shouldReturnErrorWhenEmployeeNotFoundOnDisable() {
      // Given
      when(employeeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.updateDisableFlag(999L);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("正常情況：應該成功切換禁用狀態")
    void shouldToggleDisableFlag() {
      // Given
      Long employeeId = 1L;
      EmployeeEntity employee = createTestEmployee(employeeId, "admin");
      employee.setDisabledFlag(false);

      when(employeeDao.selectById(employeeId)).thenReturn(employee);

      // When
      ResponseDTO<String> result = employeeService.updateDisableFlag(employeeId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(employeeDao).updateDisableFlag(employeeId, true);
    }
  }

  // ==================== batchUpdateDeleteFlag 測試 ====================

  @Nested
  @DisplayName("batchUpdateDeleteFlag 批量刪除測試")
  class BatchUpdateDeleteFlagTest {

    @Test
    @DisplayName("邊界情況：空列表時應直接返回成功")
    void shouldReturnOkWhenEmptyList() {
      // When
      ResponseDTO<String> result = employeeService.batchUpdateDeleteFlag(Collections.emptyList());

      // Then
      assertThat(result.getOk()).isTrue();
      verify(employeeManager, never()).updateBatchById(anyList());
    }

    @Test
    @DisplayName("正常情況：應該批量刪除員工")
    void shouldBatchDeleteEmployees() {
      // Given
      List<Long> employeeIdList = List.of(1L, 2L);
      List<EmployeeEntity> employees =
          List.of(createTestEmployee(1L, "admin"), createTestEmployee(2L, "user"));

      when(employeeManager.listByIds(employeeIdList)).thenReturn(employees);

      // When
      ResponseDTO<String> result = employeeService.batchUpdateDeleteFlag(employeeIdList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(employeeManager).updateBatchById(anyList());
    }
  }

  // ==================== batchUpdateDepartment 測試 ====================

  @Nested
  @DisplayName("batchUpdateDepartment 批量更新部門測試")
  class BatchUpdateDepartmentTest {

    @Test
    @DisplayName("異常情況：員工數量不匹配時應返回錯誤")
    void shouldReturnErrorWhenEmployeeCountMismatch() {
      // Given
      EmployeeBatchUpdateDepartmentForm form = new EmployeeBatchUpdateDepartmentForm();
      form.setEmployeeIdList(List.of(1L, 2L, 3L));
      form.setDepartmentId(1L);

      when(employeeDao.selectBatchIds(anyList()))
          .thenReturn(List.of(createTestEmployee(1L, "admin")));

      // When
      ResponseDTO<String> result = employeeService.batchUpdateDepartment(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("正常情況：應該批量更新部門")
    void shouldBatchUpdateDepartment() {
      // Given
      EmployeeBatchUpdateDepartmentForm form = new EmployeeBatchUpdateDepartmentForm();
      form.setEmployeeIdList(List.of(1L, 2L));
      form.setDepartmentId(1L);

      when(employeeDao.selectBatchIds(anyList()))
          .thenReturn(List.of(createTestEmployee(1L, "admin"), createTestEmployee(2L, "user")));

      // When
      ResponseDTO<String> result = employeeService.batchUpdateDepartment(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(employeeManager).updateBatchById(anyList());
    }
  }

  // ==================== updatePassword 測試 ====================

  @Nested
  @DisplayName("updatePassword 更新密碼測試")
  class UpdatePasswordTest {

    @Test
    @DisplayName("異常情況：員工不存在時應返回錯誤")
    void shouldReturnErrorWhenEmployeeNotFoundOnPasswordUpdate() {
      // Given
      EmployeeUpdatePasswordForm form = createTestPasswordForm(999L, "oldPwd", "newPwd");
      RequestEmployee requestUser = createTestRequestEmployee(999L);

      when(employeeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.updatePassword(requestUser, form);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("異常情況：原密碼錯誤時應返回錯誤")
    void shouldReturnErrorWhenOldPasswordWrong() {
      // Given
      EmployeeUpdatePasswordForm form = createTestPasswordForm(1L, "wrongPwd", "newPwd");
      RequestEmployee requestUser = createTestRequestEmployee(1L);

      EmployeeEntity employee = createTestEmployee(1L, "admin");
      employee.setEmployeeUid("UID123");
      employee.setLoginPwd("encryptedOldPwd");

      when(employeeDao.selectById(1L)).thenReturn(employee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(false);

      // When
      ResponseDTO<String> result = employeeService.updatePassword(requestUser, form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("原密码有误");
    }

    @Test
    @DisplayName("異常情況：新舊密碼相同時應返回錯誤")
    void shouldReturnErrorWhenNewPasswordSameAsOld() {
      // Given
      EmployeeUpdatePasswordForm form = createTestPasswordForm(1L, "samePwd", "samePwd");
      RequestEmployee requestUser = createTestRequestEmployee(1L);

      EmployeeEntity employee = createTestEmployee(1L, "admin");
      employee.setEmployeeUid("UID123");
      employee.setLoginPwd("hashedPassword");

      when(employeeDao.selectById(1L)).thenReturn(employee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(true);

      // When
      ResponseDTO<String> result = employeeService.updatePassword(requestUser, form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("新密码与原始密码相同");
    }
  }

  // ==================== resetPassword 測試 ====================

  @Nested
  @DisplayName("resetPassword 重置密碼測試")
  class ResetPasswordTest {

    @Test
    @DisplayName("異常情況：員工不存在時應返回錯誤")
    void shouldReturnErrorWhenEmployeeNotFoundOnReset() {
      // Given
      when(employeeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = employeeService.resetPassword(999L);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("正常情況：應該成功重置密碼")
    void shouldResetPasswordSuccess() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin");
      employee.setEmployeeUid("UID123");

      when(employeeDao.selectById(1L)).thenReturn(employee);
      when(securityPasswordService.randomPassword()).thenReturn("NewPwd123");
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted");

      // When
      ResponseDTO<String> result = employeeService.resetPassword(1L);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEqualTo("NewPwd123");
      verify(employeeDao).updatePassword(eq(1L), anyString());
    }
  }

  // ==================== queryAllEmployee 測試 ====================

  @Nested
  @DisplayName("queryAllEmployee 查詢全部員工測試")
  class QueryAllEmployeeTest {

    @Test
    @DisplayName("正常情況：應該返回員工列表")
    void shouldReturnAllEmployees() {
      // Given
      EmployeeVO employeeVO = new EmployeeVO();
      employeeVO.setEmployeeId(1L);

      when(employeeDao.selectEmployeeByDisabledAndDeleted(anyBoolean(), eq(Boolean.FALSE)))
          .thenReturn(Collections.singletonList(employeeVO));

      // When
      ResponseDTO<List<EmployeeVO>> result = employeeService.queryAllEmployee(false);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ==================== generateSaltPassword 測試 ====================

  @Nested
  @DisplayName("generateSaltPassword 生成加鹽密碼測試")
  class GenerateSaltPasswordTest {

    @Test
    @DisplayName("正常情況：應該正確生成加鹽密碼格式")
    void shouldGenerateCorrectFormat() {
      // Given
      String password = "myPassword";
      String employeeUid = "uid123";

      // When
      String result = employeeService.generateSaltPassword(password, employeeUid);

      // Then
      assertThat(result).isEqualTo("myPassword_UID123_uid123");
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeEntity createTestEmployee(Long id, String loginName) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setLoginName(loginName);
    entity.setActualName("員工" + id);
    entity.setEmployeeUid("UID" + id);
    entity.setDeletedFlag(false);
    entity.setDisabledFlag(false);
    return entity;
  }

  private EmployeeAddForm createTestAddForm(String loginName, String phone) {
    EmployeeAddForm form = new EmployeeAddForm();
    form.setLoginName(loginName);
    form.setPhone(phone);
    form.setActualName("新員工");
    form.setDepartmentId(1L);
    form.setRoleIdList(Collections.emptyList());
    return form;
  }

  private EmployeeUpdateForm createTestUpdateForm(Long employeeId, String loginName) {
    EmployeeUpdateForm form = new EmployeeUpdateForm();
    form.setEmployeeId(employeeId);
    form.setLoginName(loginName);
    form.setActualName("更新後員工");
    form.setRoleIdList(Collections.emptyList());
    return form;
  }

  private EmployeeUpdatePasswordForm createTestPasswordForm(
      Long employeeId, String oldPwd, String newPwd) {
    EmployeeUpdatePasswordForm form = new EmployeeUpdatePasswordForm();
    form.setEmployeeId(employeeId);
    form.setOldPassword(oldPwd);
    form.setNewPassword(newPwd);
    return form;
  }

  private RequestEmployee createTestRequestEmployee(Long employeeId) {
    RequestEmployee requestEmployee = new RequestEmployee();
    requestEmployee.setEmployeeId(employeeId);
    requestEmployee.setActualName("員工" + employeeId);
    requestEmployee.setUserType(UserTypeEnum.ADMIN_EMPLOYEE);
    return requestEmployee;
  }
}
