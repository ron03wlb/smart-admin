package net.lab1024.sa.admin.fixtures;

import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;

/**
 * Test fixture for Employee domain objects
 *
 * <p>Provides factory methods to create test data for Employee-related tests. All methods return
 * new instances with default values that can be customized.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Create default employee entity
 * EmployeeEntity employee = EmployeeTestFixture.createEmployee();
 *
 * // Create employee with specific ID
 * EmployeeEntity employee = EmployeeTestFixture.createEmployee(1001L, "test_user");
 *
 * // Create employee add form with unique login name
 * EmployeeAddForm form = EmployeeTestFixture.createAddForm();
 * form.setActualName("Custom Name");
 * }</pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
public class EmployeeTestFixture {

  private static final Long DEFAULT_EMPLOYEE_ID = 1001L;
  private static final String DEFAULT_LOGIN_NAME = "test_employee";
  private static final String DEFAULT_ACTUAL_NAME = "Test Employee";
  private static final Long DEFAULT_DEPARTMENT_ID = 1L;
  private static final Integer DEFAULT_GENDER = 1; // Male

  /**
   * Create employee entity with default values
   *
   * @return EmployeeEntity with default test data
   */
  public static EmployeeEntity createEmployee() {
    return createEmployee(DEFAULT_EMPLOYEE_ID, DEFAULT_LOGIN_NAME);
  }

  /**
   * Create employee entity with specific ID and login name
   *
   * @param employeeId Employee ID
   * @param loginName Login name (unique)
   * @return EmployeeEntity with specified values
   */
  public static EmployeeEntity createEmployee(Long employeeId, String loginName) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(employeeId);
    entity.setLoginName(loginName);
    entity.setLoginPwd("$2a$10$encryptedPasswordHash"); // BCrypt hash placeholder
    entity.setActualName(DEFAULT_ACTUAL_NAME + " " + employeeId);
    entity.setGender(DEFAULT_GENDER);
    entity.setPhone("138" + String.format("%08d", employeeId));
    entity.setEmail(loginName + "@test.com");
    entity.setDepartmentId(DEFAULT_DEPARTMENT_ID);
    entity.setPositionId(null);
    entity.setDisabledFlag(false);
    entity.setDeletedFlag(false);
    entity.setAdministratorFlag(false);
    entity.setRemark("Test employee created for testing");
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    return entity;
  }

  /**
   * Create employee add form with unique login name (timestamp-based)
   *
   * @return EmployeeAddForm with valid test data
   */
  public static EmployeeAddForm createAddForm() {
    String uniqueLoginName = "test_emp_" + System.currentTimeMillis();
    return createAddForm(uniqueLoginName);
  }

  /**
   * Create employee add form with specific login name
   *
   * @param loginName Login name (should be unique)
   * @return EmployeeAddForm with specified login name
   */
  public static EmployeeAddForm createAddForm(String loginName) {
    EmployeeAddForm form = new EmployeeAddForm();
    form.setLoginName(loginName);
    form.setActualName("New Test Employee");
    form.setGender(DEFAULT_GENDER);
    form.setDepartmentId(DEFAULT_DEPARTMENT_ID);
    form.setDisabledFlag(false);
    form.setPhone("13800000001");
    form.setEmail(loginName + "@test.com");
    form.setPositionId(null);
    form.setRoleIdList(List.of(1L)); // Default role
    form.setRemark("Created for testing");
    return form;
  }

  /**
   * Create employee update form for existing employee
   *
   * @param employeeId Employee ID to update
   * @return EmployeeUpdateForm with test data
   */
  public static EmployeeUpdateForm createUpdateForm(Long employeeId) {
    return createUpdateForm(employeeId, "updated_user_" + employeeId);
  }

  /**
   * Create employee update form with specific values
   *
   * @param employeeId Employee ID to update
   * @param loginName New login name
   * @return EmployeeUpdateForm with specified values
   */
  public static EmployeeUpdateForm createUpdateForm(Long employeeId, String loginName) {
    EmployeeUpdateForm form = new EmployeeUpdateForm();
    form.setEmployeeId(employeeId);
    form.setLoginName(loginName);
    form.setActualName("Updated Test Employee");
    form.setGender(DEFAULT_GENDER);
    form.setDepartmentId(DEFAULT_DEPARTMENT_ID);
    form.setDisabledFlag(false);
    form.setPhone("13900000001");
    form.setEmail(loginName + "@test.com");
    form.setPositionId(null);
    form.setRoleIdList(List.of(1L, 2L)); // Multiple roles
    form.setRemark("Updated for testing");
    return form;
  }

  /**
   * Create employee VO for response testing
   *
   * @return EmployeeVO with test data
   */
  public static EmployeeVO createEmployeeVO() {
    return createEmployeeVO(DEFAULT_EMPLOYEE_ID, DEFAULT_LOGIN_NAME);
  }

  /**
   * Create employee VO with specific values
   *
   * @param employeeId Employee ID
   * @param loginName Login name
   * @return EmployeeVO with specified values
   */
  public static EmployeeVO createEmployeeVO(Long employeeId, String loginName) {
    EmployeeVO vo = new EmployeeVO();
    vo.setEmployeeId(employeeId);
    vo.setLoginName(loginName);
    vo.setActualName(DEFAULT_ACTUAL_NAME + " " + employeeId);
    vo.setGender(DEFAULT_GENDER);
    vo.setPhone("138" + String.format("%08d", employeeId));
    vo.setEmail(loginName + "@test.com");
    vo.setDepartmentId(DEFAULT_DEPARTMENT_ID);
    vo.setDisabledFlag(false);
    vo.setAdministratorFlag(false);
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }

  /**
   * Create disabled employee entity
   *
   * @return EmployeeEntity with disabledFlag = true
   */
  public static EmployeeEntity createDisabledEmployee() {
    EmployeeEntity entity = createEmployee(1003L, "disabled_user");
    entity.setDisabledFlag(true);
    return entity;
  }

  /**
   * Create administrator employee entity
   *
   * @return EmployeeEntity with administratorFlag = true
   */
  public static EmployeeEntity createAdministrator() {
    EmployeeEntity entity = createEmployee(1L, "admin");
    entity.setAdministratorFlag(true);
    return entity;
  }

  /**
   * Create employee with specific department
   *
   * @param departmentId Department ID
   * @return EmployeeEntity in specified department
   */
  public static EmployeeEntity createEmployeeInDepartment(Long departmentId) {
    EmployeeEntity entity = createEmployee();
    entity.setDepartmentId(departmentId);
    return entity;
  }
}
