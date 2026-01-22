package net.lab1024.sa.admin.fixtures;

import java.time.LocalDateTime;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleAddForm;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleVO;

/**
 * Test fixture for Role domain objects
 *
 * <p>Provides factory methods to create test data for Role-related tests.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Create default role
 * RoleEntity role = RoleTestFixture.createRole();
 *
 * // Create role with specific ID and name
 * RoleEntity role = RoleTestFixture.createRole(1L, "Admin", "ADMIN");
 *
 * // Create role-employee association
 * RoleEmployeeEntity roleEmp = RoleTestFixture.createRoleEmployee(1L, 1001L);
 * }</pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
public class RoleTestFixture {

  private static final Long DEFAULT_ROLE_ID = 1L;
  private static final String DEFAULT_ROLE_NAME = "Test Role";
  private static final String DEFAULT_ROLE_CODE = "TEST_ROLE";

  /**
   * Create role entity with default values
   *
   * @return RoleEntity with default test data
   */
  public static RoleEntity createRole() {
    return createRole(DEFAULT_ROLE_ID, DEFAULT_ROLE_NAME, DEFAULT_ROLE_CODE);
  }

  /**
   * Create role entity with specific values
   *
   * @param roleId Role ID
   * @param roleName Role name
   * @param roleCode Role code
   * @return RoleEntity with specified values
   */
  public static RoleEntity createRole(Long roleId, String roleName, String roleCode) {
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(roleId);
    entity.setRoleName(roleName);
    entity.setRoleCode(roleCode);
    entity.setRemark("Test role for testing");
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    return entity;
  }

  /**
   * Create role add form
   *
   * @return RoleAddForm with test data
   */
  public static RoleAddForm createAddForm() {
    return createAddForm("New Test Role", "NEW_TEST_ROLE");
  }

  /**
   * Create role add form with specific values
   *
   * @param roleName Role name
   * @param roleCode Role code
   * @return RoleAddForm with specified values
   */
  public static RoleAddForm createAddForm(String roleName, String roleCode) {
    RoleAddForm form = new RoleAddForm();
    form.setRoleName(roleName);
    form.setRoleCode(roleCode);
    form.setRemark("Test role created for testing");
    return form;
  }

  /**
   * Create role update form
   *
   * @param roleId Role ID to update
   * @return RoleUpdateForm with test data
   */
  public static RoleUpdateForm createUpdateForm(Long roleId) {
    return createUpdateForm(roleId, "Updated Test Role", "UPDATED_TEST_ROLE");
  }

  /**
   * Create role update form with specific values
   *
   * @param roleId Role ID to update
   * @param roleName New role name
   * @param roleCode New role code
   * @return RoleUpdateForm with specified values
   */
  public static RoleUpdateForm createUpdateForm(Long roleId, String roleName, String roleCode) {
    RoleUpdateForm form = new RoleUpdateForm();
    form.setRoleId(roleId);
    form.setRoleName(roleName);
    form.setRoleCode(roleCode);
    form.setRemark("Test role updated for testing");
    return form;
  }

  /**
   * Create role VO for response testing
   *
   * @return RoleVO with test data
   */
  public static RoleVO createRoleVO() {
    return createRoleVO(DEFAULT_ROLE_ID, DEFAULT_ROLE_NAME, DEFAULT_ROLE_CODE);
  }

  /**
   * Create role VO with specific values
   *
   * @param roleId Role ID
   * @param roleName Role name
   * @param roleCode Role code
   * @return RoleVO with specified values
   */
  public static RoleVO createRoleVO(Long roleId, String roleName, String roleCode) {
    RoleVO vo = new RoleVO();
    vo.setRoleId(roleId);
    vo.setRoleName(roleName);
    vo.setRoleCode(roleCode);
    vo.setRemark("Test role for testing");
    return vo;
  }

  /**
   * Create role-employee association entity
   *
   * @param roleId Role ID
   * @param employeeId Employee ID
   * @return RoleEmployeeEntity associating role with employee
   */
  public static RoleEmployeeEntity createRoleEmployee(Long roleId, Long employeeId) {
    RoleEmployeeEntity entity = new RoleEmployeeEntity();
    entity.setRoleId(roleId);
    entity.setEmployeeId(employeeId);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    return entity;
  }

  /**
   * Create administrator role
   *
   * @return RoleEntity for administrator
   */
  public static RoleEntity createAdminRole() {
    return createRole(1L, "Administrator", "ADMIN");
  }

  /**
   * Create regular user role
   *
   * @return RoleEntity for regular user
   */
  public static RoleEntity createUserRole() {
    return createRole(2L, "User", "USER");
  }
}
