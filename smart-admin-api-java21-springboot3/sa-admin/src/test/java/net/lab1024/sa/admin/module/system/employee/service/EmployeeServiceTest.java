package net.lab1024.sa.admin.module.system.employee.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.fixtures.EmployeeTestFixture;
import net.lab1024.sa.admin.module.support.securityprotect.service.SecurityPasswordService;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.department.domain.vo.DepartmentVO;
import net.lab1024.sa.admin.module.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeBatchUpdateDepartmentForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateAvatarForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateCenterForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdatePasswordForm;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.admin.module.system.employee.manager.EmployeeManager;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.login.manager.LoginManager;
import net.lab1024.sa.admin.module.system.position.dao.PositionDao;
import net.lab1024.sa.admin.module.system.position.domain.entity.PositionEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleEmployeeVO;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.constant.StringConst;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

/**
 * EmployeeService Tests - TIER 1 Critical Business Logic
 *
 * <p>Test Coverage: 14 methods, 50+ test cases
 *
 * <p>Focus Areas: - Synchronized methods (addEmployee, updateEmployee) - Uniqueness validation
 * (loginName, phone, email) - Batch operations with Sa-Token logout - Password security (Level 3
 * protection) - Cache invalidation - Department hierarchy resolution
 *
 * @author Claude Code - Phase 2.2
 * @since 2026-01-22
 */
@DisplayName("EmployeeService Tests")
class EmployeeServiceTest extends BaseUnitTest {

  @InjectMocks private EmployeeService employeeService;

  // DAO Mocks
  @Mock private EmployeeDao employeeDao;
  @Mock private DepartmentDao departmentDao;
  @Mock private RoleEmployeeDao roleEmployeeDao;
  @Mock private PositionDao positionDao;

  // Manager Mocks
  @Mock private EmployeeManager employeeManager;
  @Mock private DepartmentCacheManager departmentCacheManager;
  @Mock private LoginManager loginManager;

  // Service Mocks
  @Mock private SecurityPasswordService securityPasswordService;

  // Test Data
  private EmployeeEntity testEmployee;
  private DepartmentEntity testDepartment;
  private static final Long TEST_EMPLOYEE_ID = 1001L;
  private static final Long TEST_DEPARTMENT_ID = 100L;
  private static final String TEST_LOGIN_NAME = "test_employee";
  private static final String TEST_PHONE = "13800138000";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_EMPLOYEE_UID = "test-uid-12345";

  @BeforeEach
  void setUp() {
    testEmployee = EmployeeTestFixture.createEmployee(TEST_EMPLOYEE_ID, TEST_LOGIN_NAME);
    testEmployee.setPhone(TEST_PHONE);
    testEmployee.setEmail(TEST_EMAIL);
    testEmployee.setEmployeeUid(TEST_EMPLOYEE_UID);
    testEmployee.setDepartmentId(TEST_DEPARTMENT_ID);

    testDepartment = new DepartmentEntity();
    testDepartment.setDepartmentId(TEST_DEPARTMENT_ID);
    testDepartment.setDepartmentName("Test Department");
  }

  @Nested
  @DisplayName("queryEmployee() Tests - Pagination with Complex Mapping")
  class QueryEmployeeTests {

    @Test
    @DisplayName("Should return paginated employees with role and position mapping")
    void queryEmployee_WithFilters_ReturnsPaginatedResults() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setDepartmentId(TEST_DEPARTMENT_ID);
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      // Mock department hierarchy
      when(departmentCacheManager.getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID))
          .thenReturn(List.of(TEST_DEPARTMENT_ID, 101L, 102L));

      // Mock employee query result
      EmployeeVO employeeVO = new EmployeeVO();
      employeeVO.setEmployeeId(TEST_EMPLOYEE_ID);
      employeeVO.setLoginName(TEST_LOGIN_NAME);
      employeeVO.setDepartmentId(TEST_DEPARTMENT_ID);
      employeeVO.setPositionId(1L);
      when(employeeDao.queryEmployee(any(Page.class), eq(queryForm), anyList()))
          .thenReturn(List.of(employeeVO));

      // Mock role mapping
      RoleEmployeeVO roleEmployeeVO = new RoleEmployeeVO();
      roleEmployeeVO.setEmployeeId(TEST_EMPLOYEE_ID);
      roleEmployeeVO.setRoleId(1L);
      roleEmployeeVO.setRoleName("Admin");
      when(roleEmployeeDao.selectRoleByEmployeeIdList(anyList()))
          .thenReturn(List.of(roleEmployeeVO));

      // Mock position mapping
      PositionEntity positionEntity = new PositionEntity();
      positionEntity.setPositionId(1L);
      positionEntity.setPositionName("Manager");
      when(positionDao.selectBatchIds(anyList())).thenReturn(List.of(positionEntity));

      // Mock department path
      when(departmentCacheManager.getDepartmentPathMap())
          .thenReturn(Map.of(TEST_DEPARTMENT_ID, "Company/IT Department"));

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertOk(response);
      PageResult<EmployeeVO> pageResult = response.getData();
      assertNotNull(pageResult);
      assertEquals(1, pageResult.getList().size());

      EmployeeVO result = pageResult.getList().get(0);
      assertEquals(TEST_EMPLOYEE_ID, result.getEmployeeId());
      assertEquals(List.of(1L), result.getRoleIdList());
      assertEquals(List.of("Admin"), result.getRoleNameList());
      assertEquals("Manager", result.getPositionName());
      assertEquals("Company/IT Department", result.getDepartmentName());

      verify(departmentCacheManager, times(1)).getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID);
      verify(roleEmployeeDao, times(1)).selectRoleByEmployeeIdList(anyList());
    }

    @Test
    @DisplayName("Should return empty result when no employees found")
    void queryEmployee_NoResults_ReturnsEmptyPage() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(employeeDao.queryEmployee(any(Page.class), eq(queryForm), anyList()))
          .thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertOk(response);
      assertTrue(response.getData().getList().isEmpty());
      verify(roleEmployeeDao, never()).selectRoleByEmployeeIdList(anyList());
    }

    @Test
    @DisplayName("Should handle employees with no roles")
    void queryEmployee_EmployeeWithNoRoles_ReturnsEmptyRoleList() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeVO employeeVO = new EmployeeVO();
      employeeVO.setEmployeeId(TEST_EMPLOYEE_ID);
      employeeVO.setDepartmentId(TEST_DEPARTMENT_ID);
      when(employeeDao.queryEmployee(any(Page.class), eq(queryForm), anyList()))
          .thenReturn(List.of(employeeVO));

      when(roleEmployeeDao.selectRoleByEmployeeIdList(anyList()))
          .thenReturn(Collections.emptyList());
      when(departmentCacheManager.getDepartmentPathMap())
          .thenReturn(Map.of(TEST_DEPARTMENT_ID, "Test Department"));

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertOk(response);
      EmployeeVO result = response.getData().getList().get(0);
      assertTrue(result.getRoleIdList().isEmpty());
      assertTrue(result.getRoleNameList().isEmpty());
    }

    @Test
    @DisplayName("Should handle employees with no position")
    void queryEmployee_EmployeeWithNoPosition_HandlesNull() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeVO employeeVO = new EmployeeVO();
      employeeVO.setEmployeeId(TEST_EMPLOYEE_ID);
      employeeVO.setDepartmentId(TEST_DEPARTMENT_ID);
      employeeVO.setPositionId(null); // No position
      when(employeeDao.queryEmployee(any(Page.class), eq(queryForm), anyList()))
          .thenReturn(List.of(employeeVO));

      when(roleEmployeeDao.selectRoleByEmployeeIdList(anyList()))
          .thenReturn(Collections.emptyList());
      // 使用 lenient() 因為當 positionId 為 null 時，可能不會調用 selectBatchIds
      lenient().when(positionDao.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
      when(departmentCacheManager.getDepartmentPathMap())
          .thenReturn(Map.of(TEST_DEPARTMENT_ID, "Test Department"));

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertOk(response);
      EmployeeVO result = response.getData().getList().get(0);
      assertNull(result.getPositionName());
    }

    @Test
    @DisplayName("Should query department hierarchy when departmentId is provided")
    void queryEmployee_WithDepartmentId_QueriesHierarchy() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setDepartmentId(TEST_DEPARTMENT_ID);
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      List<Long> hierarchyIds = List.of(TEST_DEPARTMENT_ID, 101L, 102L, 103L);
      when(departmentCacheManager.getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID))
          .thenReturn(hierarchyIds);

      when(employeeDao.queryEmployee(any(Page.class), eq(queryForm), eq(hierarchyIds)))
          .thenReturn(Collections.emptyList());

      // When
      employeeService.queryEmployee(queryForm);

      // Then
      verify(departmentCacheManager, times(1)).getDepartmentSelfAndChildren(TEST_DEPARTMENT_ID);
      verify(employeeDao, times(1)).queryEmployee(any(Page.class), eq(queryForm), eq(hierarchyIds));
    }

    @Test
    @DisplayName("Should set deletedFlag to false in query form")
    void queryEmployee_SetsDeletedFlagToFalse() {
      // Given
      EmployeeQueryForm queryForm = new EmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(employeeDao.queryEmployee(any(Page.class), any(), anyList()))
          .thenReturn(Collections.emptyList());

      // When
      employeeService.queryEmployee(queryForm);

      // Then
      assertFalse(queryForm.getDeletedFlag());
    }
  }

  @Nested
  @DisplayName("addEmployee() Tests - Synchronized with Uniqueness Validation")
  class AddEmployeeTests {

    private EmployeeAddForm employeeAddForm;

    @BeforeEach
    void setUpAddForm() {
      employeeAddForm = EmployeeTestFixture.createAddForm();
      employeeAddForm.setLoginName("new_employee");
      employeeAddForm.setPhone("13900139000");
      employeeAddForm.setDepartmentId(TEST_DEPARTMENT_ID);
      employeeAddForm.setRoleIdList(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Should add employee successfully and return random password")
    void addEmployee_ValidForm_ReturnsRandomPassword() {
      // Given
      when(employeeDao.getByLoginName(employeeAddForm.getLoginName(), null)).thenReturn(null);
      when(employeeDao.getByPhone(employeeAddForm.getPhone(), null)).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      String randomPassword = "Random@123";
      when(securityPasswordService.randomPassword()).thenReturn(randomPassword);
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted_password");

      doNothing()
          .when(employeeManager)
          .saveEmployeeTransaction(any(EmployeeEntity.class), anyList());

      // When
      ResponseDTO<String> response = employeeService.addEmployee(employeeAddForm);

      // Then
      assertOk(response);
      assertEquals(randomPassword, response.getData());
      verify(employeeManager, times(1))
          .saveEmployeeTransaction(
              argThat(
                  entity ->
                      entity.getLoginName().equals(employeeAddForm.getLoginName())
                          && entity.getEmployeeUid() != null
                          && entity.getDeletedFlag().equals(Boolean.FALSE)),
              eq(employeeAddForm.getRoleIdList()));
    }

    @Test
    @DisplayName("Should return error when login name already exists")
    void addEmployee_DuplicateLoginName_ReturnsError() {
      // Given
      when(employeeDao.getByLoginName(employeeAddForm.getLoginName(), null))
          .thenReturn(testEmployee);

      // When
      ResponseDTO<String> response = employeeService.addEmployee(employeeAddForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "登录名重复");
      verify(employeeManager, never()).saveEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when phone already exists")
    void addEmployee_DuplicatePhone_ReturnsError() {
      // Given
      when(employeeDao.getByLoginName(employeeAddForm.getLoginName(), null)).thenReturn(null);
      when(employeeDao.getByPhone(employeeAddForm.getPhone(), null)).thenReturn(testEmployee);

      // When
      ResponseDTO<String> response = employeeService.addEmployee(employeeAddForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "手机号已存在");
      verify(employeeManager, never()).saveEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when department does not exist")
    void addEmployee_NonExistentDepartment_ReturnsError() {
      // Given
      when(employeeDao.getByLoginName(employeeAddForm.getLoginName(), null)).thenReturn(null);
      when(employeeDao.getByPhone(employeeAddForm.getPhone(), null)).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.addEmployee(employeeAddForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "部门不存在");
      verify(employeeManager, never()).saveEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should generate random password and encrypt it")
    void addEmployee_GeneratesAndEncryptsPassword() {
      // Given
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);
      when(employeeDao.getByPhone(anyString(), isNull())).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      String randomPassword = "Test@1234";
      when(securityPasswordService.randomPassword()).thenReturn(randomPassword);
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted_test_pwd");

      doNothing().when(employeeManager).saveEmployeeTransaction(any(), anyList());

      // When
      ResponseDTO<String> response = employeeService.addEmployee(employeeAddForm);

      // Then
      assertOk(response);
      assertEquals(randomPassword, response.getData());
      verify(securityPasswordService, times(1)).randomPassword();
      verify(securityPasswordService, times(1)).getEncryptPwd(anyString());
    }

    @Test
    @DisplayName("Should pass role list to EmployeeManager")
    void addEmployee_PassesRoleListToManager() {
      // Given
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);
      when(employeeDao.getByPhone(anyString(), isNull())).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(securityPasswordService.randomPassword()).thenReturn("Random@123");
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted");

      List<Long> roleIds = List.of(1L, 2L, 3L);
      employeeAddForm.setRoleIdList(roleIds);

      doNothing().when(employeeManager).saveEmployeeTransaction(any(), anyList());

      // When
      employeeService.addEmployee(employeeAddForm);

      // Then
      verify(employeeManager, times(1))
          .saveEmployeeTransaction(any(EmployeeEntity.class), eq(roleIds));
    }

    @Test
    @DisplayName("Should set deletedFlag to false for new employee")
    void addEmployee_SetsDeletedFlagFalse() {
      // Given
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);
      when(employeeDao.getByPhone(anyString(), isNull())).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(securityPasswordService.randomPassword()).thenReturn("Random@123");
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted");

      doNothing().when(employeeManager).saveEmployeeTransaction(any(), anyList());

      // When
      employeeService.addEmployee(employeeAddForm);

      // Then
      verify(employeeManager, times(1))
          .saveEmployeeTransaction(
              argThat(entity -> Boolean.FALSE.equals(entity.getDeletedFlag())), anyList());
    }

    @Test
    @DisplayName("Should generate unique employeeUid for new employee")
    void addEmployee_GeneratesUniqueEmployeeUid() {
      // Given
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);
      when(employeeDao.getByPhone(anyString(), isNull())).thenReturn(null);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(securityPasswordService.randomPassword()).thenReturn("Random@123");
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted");

      doNothing().when(employeeManager).saveEmployeeTransaction(any(), anyList());

      // When
      employeeService.addEmployee(employeeAddForm);

      // Then
      verify(employeeManager, times(1))
          .saveEmployeeTransaction(
              argThat(
                  entity ->
                      entity.getEmployeeUid() != null && entity.getEmployeeUid().length() == 32),
              anyList());
    }
  }

  @Nested
  @DisplayName("updateEmployee() Tests - Synchronized with Uniqueness Validation")
  class UpdateEmployeeTests {

    private EmployeeUpdateForm updateForm;

    @BeforeEach
    void setUpUpdateForm() {
      updateForm = new EmployeeUpdateForm();
      updateForm.setEmployeeId(TEST_EMPLOYEE_ID);
      updateForm.setLoginName("updated_name");
      updateForm.setPhone("13911111111");
      updateForm.setEmail("updated@example.com");
      updateForm.setDepartmentId(TEST_DEPARTMENT_ID);
      updateForm.setRoleIdList(List.of(1L));
    }

    @Test
    @DisplayName("Should update employee successfully")
    void updateEmployee_ValidForm_UpdatesSuccessfully() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(employeeDao.getByLoginName(updateForm.getLoginName(), null)).thenReturn(null);
      when(employeeDao.getByPhone(updateForm.getPhone(), null)).thenReturn(null);
      when(employeeDao.getByEmail(updateForm.getEmail(), null)).thenReturn(null);

      doNothing()
          .when(employeeManager)
          .updateEmployeeTransaction(any(EmployeeEntity.class), anyList());
      doNothing().when(loginManager).clearUserPermission(TEST_EMPLOYEE_ID);
      doNothing().when(loginManager).clearUserLoginInfo(TEST_EMPLOYEE_ID);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertOk(response);
      verify(employeeManager, times(1))
          .updateEmployeeTransaction(
              argThat(
                  entity ->
                      entity.getEmployeeId().equals(TEST_EMPLOYEE_ID)
                          && entity.getLoginPwd() == null),
              eq(updateForm.getRoleIdList()));
      verify(loginManager, times(1)).clearUserPermission(TEST_EMPLOYEE_ID);
      verify(loginManager, times(1)).clearUserLoginInfo(TEST_EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void updateEmployee_NonExistentEmployee_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeManager, never()).updateEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when department does not exist")
    void updateEmployee_NonExistentDepartment_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "部门不存在");
      verify(employeeManager, never()).updateEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when login name conflicts with another employee")
    void updateEmployee_DuplicateLoginName_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      EmployeeEntity conflictEmployee = new EmployeeEntity();
      conflictEmployee.setEmployeeId(9999L); // Different ID
      conflictEmployee.setLoginName(updateForm.getLoginName());
      when(employeeDao.getByLoginName(updateForm.getLoginName(), null))
          .thenReturn(conflictEmployee);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "登录名重复");
      verify(employeeManager, never()).updateEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when phone conflicts with another employee")
    void updateEmployee_DuplicatePhone_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(employeeDao.getByLoginName(updateForm.getLoginName(), null)).thenReturn(null);

      EmployeeEntity conflictEmployee = new EmployeeEntity();
      conflictEmployee.setEmployeeId(9999L); // Different ID
      conflictEmployee.setPhone(updateForm.getPhone());
      when(employeeDao.getByPhone(updateForm.getPhone(), null)).thenReturn(conflictEmployee);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "手机号已存在");
      verify(employeeManager, never()).updateEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should return error when email conflicts with another employee")
    void updateEmployee_DuplicateEmail_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);
      when(employeeDao.getByLoginName(updateForm.getLoginName(), null)).thenReturn(null);
      when(employeeDao.getByPhone(updateForm.getPhone(), null)).thenReturn(null);

      EmployeeEntity conflictEmployee = new EmployeeEntity();
      conflictEmployee.setEmployeeId(9999L); // Different ID
      conflictEmployee.setEmail(updateForm.getEmail());
      when(employeeDao.getByEmail(updateForm.getEmail(), null)).thenReturn(conflictEmployee);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "邮箱账号已存在");
      verify(employeeManager, never()).updateEmployeeTransaction(any(), anyList());
    }

    @Test
    @DisplayName("Should allow update when same employee ID has same login name")
    void updateEmployee_SameEmployeeId_AllowsSameLoginName() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(departmentDao.selectById(TEST_DEPARTMENT_ID)).thenReturn(testDepartment);

      EmployeeEntity sameEmployee = new EmployeeEntity();
      sameEmployee.setEmployeeId(TEST_EMPLOYEE_ID); // Same ID
      sameEmployee.setLoginName(updateForm.getLoginName());
      when(employeeDao.getByLoginName(updateForm.getLoginName(), null)).thenReturn(sameEmployee);
      when(employeeDao.getByPhone(updateForm.getPhone(), null)).thenReturn(null);
      when(employeeDao.getByEmail(updateForm.getEmail(), null)).thenReturn(null);

      doNothing().when(employeeManager).updateEmployeeTransaction(any(), anyList());
      doNothing().when(loginManager).clearUserPermission(anyLong());
      doNothing().when(loginManager).clearUserLoginInfo(anyLong());

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertOk(response);
      verify(employeeManager, times(1)).updateEmployeeTransaction(any(), anyList());
    }
  }

  @Nested
  @DisplayName("updateCenter() Tests - Employee Self-Update")
  class UpdateCenterTests {

    private EmployeeUpdateCenterForm updateCenterForm;

    @BeforeEach
    void setUpCenterForm() {
      updateCenterForm = new EmployeeUpdateCenterForm();
      updateCenterForm.setEmployeeId(TEST_EMPLOYEE_ID);
      updateCenterForm.setActualName("Updated Name");
      updateCenterForm.setPhone("13922222222");
      updateCenterForm.setEmail("newemail@example.com");
    }

    @Test
    @DisplayName("Should update employee center info successfully")
    void updateCenter_ValidForm_UpdatesSuccessfully() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);
      when(employeeDao.getByPhone(updateCenterForm.getPhone(), null)).thenReturn(null);
      when(employeeDao.getByEmail(updateCenterForm.getEmail(), null)).thenReturn(null);
      when(employeeDao.updateById(any(EmployeeEntity.class))).thenReturn(1);

      doNothing().when(loginManager).clearUserPermission(TEST_EMPLOYEE_ID);
      doNothing().when(loginManager).clearUserLoginInfo(TEST_EMPLOYEE_ID);

      // When
      ResponseDTO<String> response = employeeService.updateCenter(updateCenterForm);

      // Then
      assertOk(response);
      verify(employeeDao, times(1))
          .updateById(
              argThat(
                  (EmployeeEntity entity) ->
                      entity.getEmployeeId().equals(TEST_EMPLOYEE_ID)
                          && entity.getLoginPwd() == null));
      verify(loginManager, times(1)).clearUserPermission(TEST_EMPLOYEE_ID);
      verify(loginManager, times(1)).clearUserLoginInfo(TEST_EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void updateCenter_NonExistentEmployee_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.updateCenter(updateCenterForm);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should validate phone uniqueness but NOT login name")
    void updateCenter_ValidatesPhoneNotLoginName() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(employeeDao.getByLoginName("", null)).thenReturn(null); // Empty string, should not check
      when(employeeDao.getByPhone(updateCenterForm.getPhone(), null)).thenReturn(null);
      when(employeeDao.getByEmail(updateCenterForm.getEmail(), null)).thenReturn(null);
      when(employeeDao.updateById(any(EmployeeEntity.class))).thenReturn(1);

      doNothing().when(loginManager).clearUserPermission(anyLong());
      doNothing().when(loginManager).clearUserLoginInfo(anyLong());

      // When
      employeeService.updateCenter(updateCenterForm);

      // Then - Verify empty string passed for loginName (no validation)
      verify(employeeDao, times(1)).getByLoginName(eq(""), isNull());
      verify(employeeDao, times(1)).getByPhone(updateCenterForm.getPhone(), null);
    }

    @Test
    @DisplayName("Should return error when phone conflicts")
    void updateCenter_DuplicatePhone_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(employeeDao.getByLoginName(anyString(), isNull())).thenReturn(null);

      EmployeeEntity conflictEmployee = new EmployeeEntity();
      conflictEmployee.setEmployeeId(9999L);
      conflictEmployee.setPhone(updateCenterForm.getPhone());
      when(employeeDao.getByPhone(updateCenterForm.getPhone(), null)).thenReturn(conflictEmployee);

      // When
      ResponseDTO<String> response = employeeService.updateCenter(updateCenterForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "手机号已存在");
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("updateAvatar() Tests")
  class UpdateAvatarTests {

    @Test
    @DisplayName("Should update avatar successfully")
    void updateAvatar_ValidForm_UpdatesAvatar() {
      // Given
      EmployeeUpdateAvatarForm avatarForm = new EmployeeUpdateAvatarForm();
      avatarForm.setEmployeeId(TEST_EMPLOYEE_ID);
      avatarForm.setAvatar("https://example.com/avatar.jpg");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(employeeDao.updateById(any(EmployeeEntity.class))).thenReturn(1);
      doNothing().when(loginManager).clearUserPermission(TEST_EMPLOYEE_ID);
      doNothing().when(loginManager).clearUserLoginInfo(TEST_EMPLOYEE_ID);

      // When
      ResponseDTO<String> response = employeeService.updateAvatar(avatarForm);

      // Then
      assertOk(response);
      verify(employeeDao, times(1))
          .updateById(
              argThat(
                  (EmployeeEntity entity) ->
                      entity.getEmployeeId().equals(TEST_EMPLOYEE_ID)
                          && entity.getAvatar().equals(avatarForm.getAvatar())));
      verify(loginManager, times(1)).clearUserPermission(TEST_EMPLOYEE_ID);
      verify(loginManager, times(1)).clearUserLoginInfo(TEST_EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void updateAvatar_NonExistentEmployee_ReturnsError() {
      // Given
      EmployeeUpdateAvatarForm avatarForm = new EmployeeUpdateAvatarForm();
      avatarForm.setEmployeeId(TEST_EMPLOYEE_ID);
      avatarForm.setAvatar("https://example.com/avatar.jpg");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.updateAvatar(avatarForm);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should clear employee cache after avatar update")
    void updateAvatar_ClearsCacheAfterUpdate() {
      // Given
      EmployeeUpdateAvatarForm avatarForm = new EmployeeUpdateAvatarForm();
      avatarForm.setEmployeeId(TEST_EMPLOYEE_ID);
      avatarForm.setAvatar("new_avatar.png");

      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(employeeDao.updateById(any(EmployeeEntity.class))).thenReturn(1);
      doNothing().when(loginManager).clearUserPermission(TEST_EMPLOYEE_ID);
      doNothing().when(loginManager).clearUserLoginInfo(TEST_EMPLOYEE_ID);

      // When
      employeeService.updateAvatar(avatarForm);

      // Then
      verify(loginManager, times(1)).clearUserPermission(TEST_EMPLOYEE_ID);
      verify(loginManager, times(1)).clearUserLoginInfo(TEST_EMPLOYEE_ID);
    }
  }

  @Nested
  @DisplayName("updateDisableFlag() Tests - Sa-Token Logout Integration")
  class UpdateDisableFlagTests {

    @Test
    @DisplayName("Should disable employee and force logout")
    void updateDisableFlag_DisableEmployee_ForcesLogout() {
      // Given
      testEmployee.setDisabledFlag(false); // Currently enabled
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      doNothing().when(employeeDao).updateDisableFlag(TEST_EMPLOYEE_ID, true);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock
            .when(
                () ->
                    StpUtil.logout(
                        UserTypeEnum.ADMIN_EMPLOYEE.getValue()
                            + StringConst.COLON
                            + TEST_EMPLOYEE_ID))
            .thenAnswer(invocation -> null);

        ResponseDTO<String> response = employeeService.updateDisableFlag(TEST_EMPLOYEE_ID);

        // Then
        assertOk(response);
        verify(employeeDao, times(1)).updateDisableFlag(TEST_EMPLOYEE_ID, true);
        stpUtilMock.verify(
            () ->
                StpUtil.logout(
                    UserTypeEnum.ADMIN_EMPLOYEE.getValue() + StringConst.COLON + TEST_EMPLOYEE_ID),
            times(1));
      }
    }

    @Test
    @DisplayName("Should enable employee without logout")
    void updateDisableFlag_EnableEmployee_NoLogout() {
      // Given
      testEmployee.setDisabledFlag(true); // Currently disabled
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      doNothing().when(employeeDao).updateDisableFlag(TEST_EMPLOYEE_ID, false);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        ResponseDTO<String> response = employeeService.updateDisableFlag(TEST_EMPLOYEE_ID);

        // Then
        assertOk(response);
        verify(employeeDao, times(1)).updateDisableFlag(TEST_EMPLOYEE_ID, false);
        stpUtilMock.verify(() -> StpUtil.logout(anyString()), never());
      }
    }

    @Test
    @DisplayName("Should return error when employee ID is null")
    void updateDisableFlag_NullEmployeeId_ReturnsError() {
      // When
      ResponseDTO<String> response = employeeService.updateDisableFlag(null);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeDao, never()).updateDisableFlag(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void updateDisableFlag_NonExistentEmployee_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.updateDisableFlag(TEST_EMPLOYEE_ID);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeDao, never()).updateDisableFlag(anyLong(), anyBoolean());
    }
  }

  @Nested
  @DisplayName("batchUpdateDeleteFlag() Tests - Batch Soft Delete with Force Logout")
  class BatchUpdateDeleteFlagTests {

    @Test
    @DisplayName("Should batch soft delete employees and force logout all")
    void batchUpdateDeleteFlag_ValidList_SoftDeletesAndLogsOut() {
      // Given
      List<Long> employeeIds = List.of(1001L, 1002L, 1003L);
      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");
      EmployeeEntity emp3 = EmployeeTestFixture.createEmployee(1003L, "emp3");

      when(employeeManager.listByIds(employeeIds)).thenReturn(List.of(emp1, emp2, emp3));
      when(employeeManager.updateBatchById(anyList())).thenReturn(true);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.logout(anyString())).thenAnswer(invocation -> null);

        ResponseDTO<String> response = employeeService.batchUpdateDeleteFlag(employeeIds);

        // Then
        assertOk(response);
        verify(employeeManager, times(1))
            .updateBatchById(
                argThat(
                    list ->
                        list.size() == 3
                            && list.stream()
                                .allMatch(e -> Boolean.TRUE.equals(e.getDeletedFlag()))));
        stpUtilMock.verify(() -> StpUtil.logout(anyString()), times(3));
      }
    }

    @Test
    @DisplayName("Should return OK when employee list is empty")
    void batchUpdateDeleteFlag_EmptyList_ReturnsOk() {
      // When
      ResponseDTO<String> response = employeeService.batchUpdateDeleteFlag(Collections.emptyList());

      // Then
      assertOk(response);
      verify(employeeManager, never()).listByIds(anyList());
      verify(employeeManager, never()).updateBatchById(anyList());
    }

    @Test
    @DisplayName("Should return OK when no matching employees found")
    void batchUpdateDeleteFlag_NoMatchingEmployees_ReturnsOk() {
      // Given
      List<Long> employeeIds = List.of(9999L, 8888L);
      when(employeeManager.listByIds(employeeIds)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> response = employeeService.batchUpdateDeleteFlag(employeeIds);

      // Then
      assertOk(response);
      verify(employeeManager, never()).updateBatchById(anyList());
    }

    @Test
    @DisplayName("Should force logout each deleted employee")
    void batchUpdateDeleteFlag_ForcesLogoutForEachEmployee() {
      // Given
      List<Long> employeeIds = List.of(1001L, 1002L);
      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");

      when(employeeManager.listByIds(employeeIds)).thenReturn(List.of(emp1, emp2));
      when(employeeManager.updateBatchById(anyList())).thenReturn(true);

      // When
      try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
        stpUtilMock.when(() -> StpUtil.logout(anyString())).thenAnswer(invocation -> null);

        employeeService.batchUpdateDeleteFlag(employeeIds);

        // Then - Verify logout called for each employee ID
        stpUtilMock.verify(
            () -> StpUtil.logout(UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":1001"), times(1));
        stpUtilMock.verify(
            () -> StpUtil.logout(UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":1002"), times(1));
      }
    }
  }

  @Nested
  @DisplayName("batchUpdateDepartment() Tests")
  class BatchUpdateDepartmentTests {

    @Test
    @DisplayName("Should batch update department successfully")
    void batchUpdateDepartment_ValidForm_UpdatesSuccessfully() {
      // Given
      EmployeeBatchUpdateDepartmentForm form = new EmployeeBatchUpdateDepartmentForm();
      form.setEmployeeIdList(List.of(1001L, 1002L, 1003L));
      form.setDepartmentId(200L);

      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");
      EmployeeEntity emp3 = EmployeeTestFixture.createEmployee(1003L, "emp3");

      when(employeeDao.selectBatchIds(form.getEmployeeIdList()))
          .thenReturn(List.of(emp1, emp2, emp3));
      when(employeeManager.updateBatchById(anyList())).thenReturn(true);

      // When
      ResponseDTO<String> response = employeeService.batchUpdateDepartment(form);

      // Then
      assertOk(response);
      verify(employeeManager, times(1))
          .updateBatchById(
              argThat(
                  list ->
                      list.size() == 3
                          && list.stream().allMatch(e -> e.getDepartmentId().equals(200L))));
    }

    @Test
    @DisplayName("Should return error when employee count mismatch")
    void batchUpdateDepartment_CountMismatch_ReturnsError() {
      // Given - Request 3 employees, but only 2 exist
      EmployeeBatchUpdateDepartmentForm form = new EmployeeBatchUpdateDepartmentForm();
      form.setEmployeeIdList(List.of(1001L, 1002L, 9999L));
      form.setDepartmentId(200L);

      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");

      when(employeeDao.selectBatchIds(form.getEmployeeIdList()))
          .thenReturn(List.of(emp1, emp2)); // Only 2 found

      // When
      ResponseDTO<String> response = employeeService.batchUpdateDepartment(form);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeManager, never()).updateBatchById(anyList());
    }

    @Test
    @DisplayName("Should set correct department ID for all employees")
    void batchUpdateDepartment_SetsCorrectDepartmentId() {
      // Given
      EmployeeBatchUpdateDepartmentForm form = new EmployeeBatchUpdateDepartmentForm();
      form.setEmployeeIdList(List.of(1001L, 1002L));
      form.setDepartmentId(300L);

      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");

      when(employeeDao.selectBatchIds(form.getEmployeeIdList())).thenReturn(List.of(emp1, emp2));
      when(employeeManager.updateBatchById(anyList())).thenReturn(true);

      // When
      employeeService.batchUpdateDepartment(form);

      // Then
      verify(employeeManager, times(1))
          .updateBatchById(
              argThat(
                  list -> {
                    List<EmployeeEntity> entities = new java.util.ArrayList<>(list);
                    return list.size() == 2
                        && entities.get(0).getEmployeeId().equals(1001L)
                        && entities.get(0).getDepartmentId().equals(300L)
                        && entities.get(1).getEmployeeId().equals(1002L)
                        && entities.get(1).getDepartmentId().equals(300L);
                  }));
    }
  }

  @Nested
  @DisplayName("updatePassword() Tests - SECURITY CRITICAL with @Transactional")
  class UpdatePasswordTests {

    private EmployeeUpdatePasswordForm updatePasswordForm;
    private RequestEmployee requestUser;

    @BeforeEach
    void setUpPasswordForm() {
      updatePasswordForm = new EmployeeUpdatePasswordForm();
      updatePasswordForm.setEmployeeId(TEST_EMPLOYEE_ID);
      updatePasswordForm.setOldPassword("OldPass@123");
      updatePasswordForm.setNewPassword("NewPass@456");

      requestUser = new RequestEmployee();
      requestUser.setEmployeeId(TEST_EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Should update password successfully")
    void updatePassword_ValidForm_UpdatesPassword() {
      // Given
      testEmployee.setLoginPwd("encrypted_old_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      String oldSaltPassword =
          updatePasswordForm.getOldPassword()
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.matchesPwd(oldSaltPassword, testEmployee.getLoginPwd()))
          .thenReturn(true);

      when(securityPasswordService.validatePasswordComplexity(updatePasswordForm.getNewPassword()))
          .thenReturn(ResponseDTO.ok());
      when(securityPasswordService.validatePasswordRepeatTimes(eq(requestUser), anyString()))
          .thenReturn(ResponseDTO.ok());
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("encrypted_new_password");

      doNothing()
          .when(employeeManager)
          .updatePasswordTransaction(eq(TEST_EMPLOYEE_ID), eq("encrypted_new_password"));
      doNothing()
          .when(securityPasswordService)
          .saveUserChangePasswordLog(eq(requestUser), anyString(), anyString());

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertOk(response);
      verify(employeeManager, times(1))
          .updatePasswordTransaction(eq(TEST_EMPLOYEE_ID), eq("encrypted_new_password"));
      verify(securityPasswordService, times(1))
          .saveUserChangePasswordLog(
              eq(requestUser), eq("encrypted_new_password"), eq(testEmployee.getLoginPwd()));
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void updatePassword_NonExistentEmployee_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should return error when old password is incorrect")
    void updatePassword_WrongOldPassword_ReturnsError() {
      // Given
      testEmployee.setLoginPwd("encrypted_old_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      String oldSaltPassword =
          updatePasswordForm.getOldPassword()
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.matchesPwd(oldSaltPassword, testEmployee.getLoginPwd()))
          .thenReturn(false);

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "原密码有误");
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should return error when new password same as old password")
    void updatePassword_NewPasswordSameAsOld_ReturnsError() {
      // Given
      updatePasswordForm.setOldPassword("SamePass@123");
      updatePasswordForm.setNewPassword("SamePass@123");

      testEmployee.setLoginPwd("encrypted_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(true);

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "新密码与原始密码相同");
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should return error when password complexity validation fails")
    void updatePassword_ComplexityValidationFails_ReturnsError() {
      // Given
      testEmployee.setLoginPwd("encrypted_old_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(true);

      when(securityPasswordService.validatePasswordComplexity(updatePasswordForm.getNewPassword()))
          .thenReturn(ResponseDTO.userErrorParam("密码复杂度不符合要求"));

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "密码复杂度不符合要求");
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName(
        "Should return error when password repeat times validation fails (Level 3 Protection)")
    void updatePassword_RepeatTimesValidationFails_ReturnsError() {
      // Given
      testEmployee.setLoginPwd("encrypted_old_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(true);
      when(securityPasswordService.validatePasswordComplexity(anyString()))
          .thenReturn(ResponseDTO.ok());

      when(securityPasswordService.validatePasswordRepeatTimes(eq(requestUser), anyString()))
          .thenReturn(ResponseDTO.userErrorParam("密码不能与最近5次使用的密码相同"));

      // When
      ResponseDTO<String> response =
          employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      assertError(response, UserErrorCode.PARAM_ERROR);
      assertErrorContains(response, "密码不能与最近");
      verify(employeeDao, never()).updateById(any(EmployeeEntity.class));
    }

    @Test
    @DisplayName("Should use correct salt password format for validation")
    void updatePassword_UsesCorrectSaltPasswordFormat() {
      // Given
      testEmployee.setLoginPwd("encrypted_old_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      String expectedOldSaltPwd =
          updatePasswordForm.getOldPassword()
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.matchesPwd(expectedOldSaltPwd, testEmployee.getLoginPwd()))
          .thenReturn(true);

      when(securityPasswordService.validatePasswordComplexity(anyString()))
          .thenReturn(ResponseDTO.ok());

      String expectedNewSaltPwd =
          updatePasswordForm.getNewPassword()
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.validatePasswordRepeatTimes(
              eq(requestUser), eq(expectedNewSaltPwd)))
          .thenReturn(ResponseDTO.ok());

      when(securityPasswordService.getEncryptPwd(expectedNewSaltPwd))
          .thenReturn("encrypted_new_password");
      doNothing()
          .when(employeeManager)
          .updatePasswordTransaction(eq(TEST_EMPLOYEE_ID), eq("encrypted_new_password"));
      doNothing().when(securityPasswordService).saveUserChangePasswordLog(any(), any(), any());

      // When
      employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      verify(securityPasswordService, times(1))
          .matchesPwd(eq(expectedOldSaltPwd), eq(testEmployee.getLoginPwd()));
      verify(securityPasswordService, times(1))
          .validatePasswordRepeatTimes(eq(requestUser), eq(expectedNewSaltPwd));
      verify(securityPasswordService, times(1)).getEncryptPwd(eq(expectedNewSaltPwd));
    }

    @Test
    @DisplayName("Should save password change log after successful update")
    void updatePassword_SavesPasswordChangeLog() {
      // Given
      testEmployee.setLoginPwd("old_encrypted_password");
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(securityPasswordService.matchesPwd(anyString(), anyString())).thenReturn(true);
      when(securityPasswordService.validatePasswordComplexity(anyString()))
          .thenReturn(ResponseDTO.ok());
      when(securityPasswordService.validatePasswordRepeatTimes(any(), anyString()))
          .thenReturn(ResponseDTO.ok());
      when(securityPasswordService.getEncryptPwd(anyString())).thenReturn("new_encrypted_password");
      doNothing()
          .when(employeeManager)
          .updatePasswordTransaction(eq(TEST_EMPLOYEE_ID), eq("new_encrypted_password"));

      doNothing()
          .when(securityPasswordService)
          .saveUserChangePasswordLog(eq(requestUser), anyString(), anyString());

      // When
      employeeService.updatePassword(requestUser, updatePasswordForm);

      // Then
      verify(securityPasswordService, times(1))
          .saveUserChangePasswordLog(
              eq(requestUser), eq("new_encrypted_password"), eq("old_encrypted_password"));
    }
  }

  @Nested
  @DisplayName("getAllEmployeeByDepartmentId() Tests")
  class GetAllEmployeeByDepartmentIdTests {

    @Test
    @DisplayName("Should return employees with department info")
    void getAllEmployeeByDepartmentId_ValidDepartmentId_ReturnsEmployees() {
      // Given
      EmployeeEntity emp1 = EmployeeTestFixture.createEmployee(1001L, "emp1");
      EmployeeEntity emp2 = EmployeeTestFixture.createEmployee(1002L, "emp2");
      emp1.setDepartmentId(TEST_DEPARTMENT_ID);
      emp2.setDepartmentId(TEST_DEPARTMENT_ID);

      when(employeeDao.selectByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE))
          .thenReturn(List.of(emp1, emp2));

      DepartmentVO departmentVO = new DepartmentVO();
      departmentVO.setDepartmentId(TEST_DEPARTMENT_ID);
      departmentVO.setDepartmentName("Test Department");
      when(departmentDao.selectDepartmentVO(TEST_DEPARTMENT_ID)).thenReturn(departmentVO);

      // When
      ResponseDTO<List<EmployeeVO>> response =
          employeeService.getAllEmployeeByDepartmentId(TEST_DEPARTMENT_ID);

      // Then
      assertOk(response);
      assertEquals(2, response.getData().size());
      response.getData().forEach(vo -> assertEquals("Test Department", vo.getDepartmentName()));
    }

    @Test
    @DisplayName("Should return empty list when no employees in department")
    void getAllEmployeeByDepartmentId_NoEmployees_ReturnsEmptyList() {
      // Given
      when(employeeDao.selectByDepartmentId(TEST_DEPARTMENT_ID, Boolean.FALSE))
          .thenReturn(Collections.emptyList());

      // When
      ResponseDTO<List<EmployeeVO>> response =
          employeeService.getAllEmployeeByDepartmentId(TEST_DEPARTMENT_ID);

      // Then
      assertOk(response);
      assertTrue(response.getData().isEmpty());
      verify(departmentDao, never()).selectDepartmentVO(anyLong());
    }
  }

  @Nested
  @DisplayName("resetPassword() Tests")
  class ResetPasswordTests {

    @Test
    @DisplayName("Should reset password successfully and return random password")
    void resetPassword_ValidEmployeeId_ReturnsRandomPassword() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      String randomPassword = "Random@789";
      when(securityPasswordService.randomPassword()).thenReturn(randomPassword);

      String expectedSaltPassword =
          randomPassword
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.getEncryptPwd(expectedSaltPassword))
          .thenReturn("encrypted_random_password");

      when(employeeDao.updatePassword(TEST_EMPLOYEE_ID, "encrypted_random_password")).thenReturn(1);

      // When
      ResponseDTO<String> response = employeeService.resetPassword(TEST_EMPLOYEE_ID);

      // Then
      assertOk(response);
      assertEquals(randomPassword, response.getData());
      verify(securityPasswordService, times(1)).randomPassword();
      verify(securityPasswordService, times(1)).getEncryptPwd(expectedSaltPassword);
      verify(employeeDao, times(1)).updatePassword(TEST_EMPLOYEE_ID, "encrypted_random_password");
    }

    @Test
    @DisplayName("Should return error when employee does not exist")
    void resetPassword_NonExistentEmployee_ReturnsError() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(null);

      // When
      ResponseDTO<String> response = employeeService.resetPassword(TEST_EMPLOYEE_ID);

      // Then
      assertError(response, UserErrorCode.DATA_NOT_EXIST);
      verify(securityPasswordService, never()).randomPassword();
      verify(employeeDao, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should use generateSaltPassword() for password encryption")
    void resetPassword_UsesGenerateSaltPassword() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);
      when(securityPasswordService.randomPassword()).thenReturn("TempPass@111");

      String expectedSaltPassword =
          "TempPass@111"
              + "_"
              + TEST_EMPLOYEE_UID.toUpperCase()
              + "_"
              + TEST_EMPLOYEE_UID.toLowerCase();
      when(securityPasswordService.getEncryptPwd(expectedSaltPassword))
          .thenReturn("encrypted_temp_password");
      when(employeeDao.updatePassword(anyLong(), anyString())).thenReturn(1);

      // When
      employeeService.resetPassword(TEST_EMPLOYEE_ID);

      // Then
      verify(securityPasswordService, times(1)).getEncryptPwd(eq(expectedSaltPassword));
    }
  }

  @Nested
  @DisplayName("queryAllEmployee() Tests")
  class QueryAllEmployeeTests {

    @Test
    @DisplayName("Should return all enabled employees when disabledFlag is false")
    void queryAllEmployee_DisabledFlagFalse_ReturnsEnabledEmployees() {
      // Given
      EmployeeVO emp1 = new EmployeeVO();
      emp1.setEmployeeId(1001L);
      emp1.setDisabledFlag(false);

      EmployeeVO emp2 = new EmployeeVO();
      emp2.setEmployeeId(1002L);
      emp2.setDisabledFlag(false);

      when(employeeDao.selectEmployeeByDisabledAndDeleted(false, Boolean.FALSE))
          .thenReturn(List.of(emp1, emp2));

      // When
      ResponseDTO<List<EmployeeVO>> response = employeeService.queryAllEmployee(false);

      // Then
      assertOk(response);
      assertEquals(2, response.getData().size());
      verify(employeeDao, times(1)).selectEmployeeByDisabledAndDeleted(false, Boolean.FALSE);
    }

    @Test
    @DisplayName("Should return all disabled employees when disabledFlag is true")
    void queryAllEmployee_DisabledFlagTrue_ReturnsDisabledEmployees() {
      // Given
      EmployeeVO emp1 = new EmployeeVO();
      emp1.setEmployeeId(1001L);
      emp1.setDisabledFlag(true);

      when(employeeDao.selectEmployeeByDisabledAndDeleted(true, Boolean.FALSE))
          .thenReturn(List.of(emp1));

      // When
      ResponseDTO<List<EmployeeVO>> response = employeeService.queryAllEmployee(true);

      // Then
      assertOk(response);
      assertEquals(1, response.getData().size());
      verify(employeeDao, times(1)).selectEmployeeByDisabledAndDeleted(true, Boolean.FALSE);
    }
  }

  @Nested
  @DisplayName("generateSaltPassword() Tests - Public Utility Method")
  class GenerateSaltPasswordTests {

    @Test
    @DisplayName("Should generate correct salt password format")
    void generateSaltPassword_ValidInputs_ReturnsCorrectFormat() {
      // Given
      String password = "Test@123";
      String employeeUid = "abc-123-def";

      // When
      String saltPassword = employeeService.generateSaltPassword(password, employeeUid);

      // Then
      String expected =
          password + "_" + employeeUid.toUpperCase() + "_" + employeeUid.toLowerCase();
      assertEquals(expected, saltPassword);
      assertTrue(saltPassword.contains("ABC-123-DEF")); // Uppercase
      assertTrue(saltPassword.contains("abc-123-def")); // Lowercase
    }

    @Test
    @DisplayName("Should handle different UID formats correctly")
    void generateSaltPassword_DifferentUidFormats_HandlesCorrectly() {
      // Given
      String password = "Pass@456";
      String employeeUid = "XyZ-999";

      // When
      String saltPassword = employeeService.generateSaltPassword(password, employeeUid);

      // Then
      assertEquals("Pass@456_XYZ-999_xyz-999", saltPassword);
    }
  }

  @Nested
  @DisplayName("getById() Tests - Simple DAO Delegation")
  class GetByIdTests {

    @Test
    @DisplayName("Should return employee entity by ID")
    void getById_ExistingEmployee_ReturnsEntity() {
      // Given
      when(employeeDao.selectById(TEST_EMPLOYEE_ID)).thenReturn(testEmployee);

      // When
      EmployeeEntity result = employeeService.getById(TEST_EMPLOYEE_ID);

      // Then
      assertNotNull(result);
      assertEquals(TEST_EMPLOYEE_ID, result.getEmployeeId());
      verify(employeeDao, times(1)).selectById(TEST_EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Should return null when employee does not exist")
    void getById_NonExistentEmployee_ReturnsNull() {
      // Given
      when(employeeDao.selectById(9999L)).thenReturn(null);

      // When
      EmployeeEntity result = employeeService.getById(9999L);

      // Then
      assertNull(result);
    }
  }

  @Nested
  @DisplayName("getByLoginName() Tests - Simple DAO Delegation")
  class GetByLoginNameTests {

    @Test
    @DisplayName("Should return employee entity by login name")
    void getByLoginName_ExistingEmployee_ReturnsEntity() {
      // Given
      when(employeeDao.getByLoginName(TEST_LOGIN_NAME, false)).thenReturn(testEmployee);

      // When
      EmployeeEntity result = employeeService.getByLoginName(TEST_LOGIN_NAME);

      // Then
      assertNotNull(result);
      assertEquals(TEST_LOGIN_NAME, result.getLoginName());
      verify(employeeDao, times(1)).getByLoginName(TEST_LOGIN_NAME, false);
    }

    @Test
    @DisplayName("Should return null when login name does not exist")
    void getByLoginName_NonExistentEmployee_ReturnsNull() {
      // Given
      when(employeeDao.getByLoginName("nonexistent", false)).thenReturn(null);

      // When
      EmployeeEntity result = employeeService.getByLoginName("nonexistent");

      // Then
      assertNull(result);
    }
  }
}
