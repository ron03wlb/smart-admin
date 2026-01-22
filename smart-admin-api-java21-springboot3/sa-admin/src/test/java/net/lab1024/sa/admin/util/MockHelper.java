package net.lab1024.sa.admin.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;

/**
 * Helper class for common mocking patterns
 *
 * <p>Provides utility methods to set up common mock behaviors for DAO layers, reducing boilerplate
 * code in unit tests.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * @Mock private EmployeeDao employeeDao;
 *
 * @Test
 * void testQueryEmployee() {
 *     EmployeeEntity employee = EmployeeTestFixture.createEmployee();
 *     MockHelper.mockEmployeeDaoSelectById(employeeDao, employee);
 *
 *     // Now employeeDao.selectById(employee.getEmployeeId()) returns employee
 *     // And employeeDao.selectById(anyOtherId) returns null
 * }
 * }</pre>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
public class MockHelper {

  /**
   * Mock EmployeeDao.selectById() to return specific employee
   *
   * <p>Configures:
   *
   * <ul>
   *   <li>selectById(employee.getEmployeeId()) → returns employee
   *   <li>selectById(anyOtherId) → returns null
   * </ul>
   *
   * @param dao EmployeeDao mock
   * @param entity Employee entity to return
   */
  public static void mockEmployeeDaoSelectById(EmployeeDao dao, EmployeeEntity entity) {
    when(dao.selectById(entity.getEmployeeId())).thenReturn(entity);
    when(dao.selectById(argThat(id -> !id.equals(entity.getEmployeeId())))).thenReturn(null);
  }

  /**
   * Mock EmployeeDao.getByLoginName() to return specific employee
   *
   * @param dao EmployeeDao mock
   * @param entity Employee entity to return
   */
  public static void mockEmployeeDaoGetByLoginName(EmployeeDao dao, EmployeeEntity entity) {
    when(dao.getByLoginName(entity.getLoginName(), false)).thenReturn(entity);
    when(dao.getByLoginName(argThat(name -> !name.equals(entity.getLoginName())), any()))
        .thenReturn(null);
  }

  /**
   * Mock EmployeeDao to return null for non-existent employee ID
   *
   * @param dao EmployeeDao mock
   * @param nonExistentId ID that should return null
   */
  public static void mockEmployeeDaoNotFound(EmployeeDao dao, Long nonExistentId) {
    when(dao.selectById(nonExistentId)).thenReturn(null);
  }

  /**
   * Mock DepartmentDao.selectById() to return specific department
   *
   * @param dao DepartmentDao mock
   * @param entity Department entity to return
   */
  public static void mockDepartmentDaoSelectById(DepartmentDao dao, DepartmentEntity entity) {
    when(dao.selectById(entity.getDepartmentId())).thenReturn(entity);
    when(dao.selectById(argThat(id -> !id.equals(entity.getDepartmentId())))).thenReturn(null);
  }

  /**
   * Mock DepartmentDao to indicate department exists
   *
   * <p>Creates a simple department entity and configures selectById to return it.
   *
   * @param dao DepartmentDao mock
   * @param deptId Department ID that should exist
   */
  public static void mockDepartmentExists(DepartmentDao dao, Long deptId) {
    DepartmentEntity dept = new DepartmentEntity();
    dept.setDepartmentId(deptId);
    dept.setDepartmentName("Test Department " + deptId);
    dept.setParentId(0L);
    when(dao.selectById(deptId)).thenReturn(dept);
  }

  /**
   * Mock DepartmentDao to indicate department does not exist
   *
   * @param dao DepartmentDao mock
   * @param deptId Department ID that should not exist
   */
  public static void mockDepartmentNotFound(DepartmentDao dao, Long deptId) {
    when(dao.selectById(deptId)).thenReturn(null);
  }

  /**
   * Mock RoleDao.selectById() to return specific role
   *
   * @param dao RoleDao mock
   * @param entity Role entity to return
   */
  public static void mockRoleDaoSelectById(RoleDao dao, RoleEntity entity) {
    when(dao.selectById(entity.getRoleId())).thenReturn(entity);
    when(dao.selectById(argThat(id -> !id.equals(entity.getRoleId())))).thenReturn(null);
  }

  /**
   * Mock RoleDao to return null for duplicate name check
   *
   * <p>Used when testing role add/update with unique name validation.
   *
   * @param dao RoleDao mock
   */
  public static void mockRoleNameNotDuplicate(RoleDao dao) {
    when(dao.getByRoleName(any())).thenReturn(null);
  }

  /**
   * Mock RoleDao to return existing role for duplicate name check
   *
   * @param dao RoleDao mock
   * @param existingRole Existing role with conflicting name
   */
  public static void mockRoleNameDuplicate(RoleDao dao, RoleEntity existingRole) {
    when(dao.getByRoleName(existingRole.getRoleName())).thenReturn(existingRole);
  }

  /**
   * Mock RoleDao to return null for duplicate code check
   *
   * @param dao RoleDao mock
   */
  public static void mockRoleCodeNotDuplicate(RoleDao dao) {
    when(dao.getByRoleCode(any())).thenReturn(null);
  }

  /**
   * Mock RoleDao to return existing role for duplicate code check
   *
   * @param dao RoleDao mock
   * @param existingRole Existing role with conflicting code
   */
  public static void mockRoleCodeDuplicate(RoleDao dao, RoleEntity existingRole) {
    when(dao.getByRoleCode(existingRole.getRoleCode())).thenReturn(existingRole);
  }

  /**
   * Mock DAO insert to return success (affected rows = 1)
   *
   * <p>Generic helper for any DAO insert operation.
   *
   * @param mockResult Mock result to configure
   */
  public static void mockInsertSuccess(Object mockResult) {
    // MyBatis Plus insert returns int (affected rows)
    // This is typically handled inline in tests with when().thenReturn(1)
  }

  /**
   * Mock DAO update to return success (affected rows = 1)
   *
   * @param mockResult Mock result to configure
   */
  public static void mockUpdateSuccess(Object mockResult) {
    // MyBatis Plus updateById returns int (affected rows)
    // This is typically handled inline in tests with when().thenReturn(1)
  }
}
