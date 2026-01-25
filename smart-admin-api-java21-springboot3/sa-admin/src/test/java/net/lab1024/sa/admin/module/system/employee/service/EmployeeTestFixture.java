package net.lab1024.sa.admin.module.system.employee.service;

import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateForm;

/**
 * Test fixtures for Employee integration tests
 *
 * <p>Provides reusable test data builders for Employee entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * EmployeeEntity entity = EmployeeTestFixture.createEntity();
 *
 * // Create entity with specific department
 * EmployeeEntity entity = EmployeeTestFixture.createEntity(departmentId);
 *
 * // Create add form
 * EmployeeAddForm form = EmployeeTestFixture.createAddForm(departmentId);
 * }</pre>
 *
 * @author Claude Code (smartadmin-integration-test skill)
 * @since 2026-01-24
 */
public class EmployeeTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create EmployeeEntity with unique test data
   *
   * @return Entity with all required fields set
   */
  public static EmployeeEntity createEntity() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    EmployeeEntity entity = new EmployeeEntity();
    entity.setLoginName("test_emp_" + timestamp);
    entity.setActualName("测试员工 " + id);
    entity.setPhone(uniquePhone());
    entity.setEmail(uniqueEmail("employee" + id));
    entity.setGender(id % 2 == 0 ? 1 : 2); // Alternate between male/female
    entity.setDepartmentId(1L);
    entity.setDisabledFlag(false);
    entity.setAdministratorFlag(false);
    entity.setDeletedFlag(false);

    return entity;
  }

  /**
   * Create EmployeeEntity with specific department ID
   *
   * @param departmentId Department ID
   * @return Entity with department set
   */
  public static EmployeeEntity createEntity(Long departmentId) {
    EmployeeEntity entity = createEntity();
    entity.setDepartmentId(departmentId);
    return entity;
  }

  /**
   * Create EmployeeAddForm with default test data
   *
   * @param departmentId Department ID (required FK)
   * @return Form ready for service.addEmployee()
   */
  public static EmployeeAddForm createAddForm(Long departmentId) {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    EmployeeAddForm form = new EmployeeAddForm();
    form.setLoginName("test_emp_" + timestamp);
    form.setActualName("测试员工 " + id);
    form.setPhone(uniquePhone());
    form.setEmail(uniqueEmail("employee" + id));
    form.setGender(id % 2 == 0 ? 1 : 2);
    form.setDepartmentId(departmentId);
    form.setDisabledFlag(false);
    form.setRemark("测试备注 " + id);

    return form;
  }

  /**
   * Create EmployeeUpdateForm for updating existing employee
   *
   * @param employeeId ID of employee to update
   * @param departmentId Department ID
   * @return Form ready for service.updateEmployee()
   */
  public static EmployeeUpdateForm createUpdateForm(Long employeeId, Long departmentId) {
    int id = counter.incrementAndGet();

    EmployeeUpdateForm form = new EmployeeUpdateForm();
    form.setEmployeeId(employeeId);
    form.setLoginName("updated_emp_" + System.currentTimeMillis());
    form.setActualName("更新后员工 " + id);
    form.setPhone(uniquePhone());
    form.setEmail(uniqueEmail("updated" + id));
    form.setGender(1);
    form.setDepartmentId(departmentId);
    form.setDisabledFlag(false);
    form.setRemark("更新后备注 " + id);

    return form;
  }

  /**
   * Create EmployeeQueryForm with pagination defaults
   *
   * @return Form ready for service.queryEmployee()
   */
  public static EmployeeQueryForm createQueryForm() {
    EmployeeQueryForm form = new EmployeeQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create DepartmentEntity for FK dependency
   *
   * @param departmentName Department name
   * @return Department entity
   */
  public static DepartmentEntity createDepartment(String departmentName) {
    DepartmentEntity dept = new DepartmentEntity();
    dept.setDepartmentName(departmentName);
    dept.setParentId(0L);
    dept.setSort(1);
    return dept;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }

  /** Generate unique phone number */
  private static String uniquePhone() {
    int id = counter.get();
    return "138" + String.format("%08d", id % 100000000);
  }

  /** Generate unique email */
  private static String uniqueEmail(String name) {
    return name + "_" + System.currentTimeMillis() + "@test.com";
  }
}
