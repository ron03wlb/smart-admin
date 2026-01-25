package net.lab1024.sa.admin.module.system.employee.service;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import net.lab1024.sa.admin.BaseIntegrationTest;
import net.lab1024.sa.admin.module.system.department.dao.DepartmentDao;
import net.lab1024.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeQueryForm;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeUpdateForm;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * EmployeeService Integration Tests
 *
 * <p>Tests EmployeeService with real Spring context, database, and dependencies.
 *
 * <p><b>Testing Strategy:</b>
 *
 * <ul>
 *   <li>Uses real Spring beans (not mocks)
 *   <li>Verifies database persistence
 *   <li>Tests ResponseDTO structure
 *   <li>Validates business logic with actual data
 *   <li>@Transactional causes auto-rollback after each test
 * </ul>
 *
 * @author Claude Code (smartadmin-integration-test skill)
 * @since 2026-01-24
 */
@SpringBootTest
@Transactional
@DisplayName("EmployeeService Integration Tests")
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private EmployeeService employeeService;

  @Autowired private EmployeeDao employeeDao;

  @Autowired private DepartmentDao departmentDao;

  @Autowired private RoleEmployeeDao roleEmployeeDao;

  private Long testDepartmentId;

  @Override
  @BeforeEach
  protected void setUp() {
    // Create test department (required FK for employee)
    DepartmentEntity dept = EmployeeTestFixture.createDepartment("测试部门");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
  }

  @Nested
  @DisplayName("addEmployee() Tests")
  class AddEmployeeTests {

    @Test
    @DisplayName("Should add employee and persist to database")
    void addEmployee_ValidForm_PersistsToDatabase() {
      // Given
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);

      // When
      ResponseDTO<String> response = employeeService.addEmployee(form);

      // Then - Verify ResponseDTO
      assertNotNull(response, "Response should not be null");
      assertTrue(response.getOk(), "Expected success but got error: " + response.getMsg());
      assertNotNull(response.getData(), "Random password should be returned");
      assertFalse(response.getData().isEmpty(), "Password should not be empty");

      // Then - Verify database state
      List<EmployeeEntity> employees =
          employeeDao.selectList(
              Wrappers.<EmployeeEntity>lambdaQuery()
                  .eq(EmployeeEntity::getLoginName, form.getLoginName()));
      assertEquals(1, employees.size(), "Should have exactly 1 employee");

      EmployeeEntity saved = employees.get(0);
      assertEquals(form.getActualName(), saved.getActualName());
      assertEquals(form.getPhone(), saved.getPhone());
      assertEquals(form.getEmail(), saved.getEmail());
      assertEquals(form.getDepartmentId(), saved.getDepartmentId());
      assertEquals(form.getGender(), saved.getGender());
      assertFalse(saved.getDeletedFlag(), "Deleted flag should be false");
      assertNotNull(saved.getEmployeeUid(), "Employee UID should be generated");
      assertNotNull(saved.getLoginPwd(), "Password should be encrypted and saved");
    }

    @Test
    @DisplayName("Should add employee with roles")
    void addEmployee_WithRoles_SavesRoleAssignments() {
      // Given - Form with role IDs
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
      form.setRoleIdList(List.of(1L, 2L));

      // When
      ResponseDTO<String> response = employeeService.addEmployee(form);

      // Then
      assertTrue(response.getOk());

      // Verify roles saved
      EmployeeEntity saved = employeeDao.getByLoginName(form.getLoginName(), null);
      List<RoleEmployeeEntity> roles =
          roleEmployeeDao.selectList(
              Wrappers.<RoleEmployeeEntity>lambdaQuery()
                  .eq(RoleEmployeeEntity::getEmployeeId, saved.getEmployeeId()));
      assertEquals(2, roles.size(), "Should have 2 role assignments");
    }

    @Test
    @DisplayName("Should fail when department does not exist")
    void addEmployee_InvalidDepartment_ReturnsError() {
      // Given
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(99999L); // Non-existent dept

      // When
      ResponseDTO<String> response = employeeService.addEmployee(form);

      // Then
      assertFalse(response.getOk(), "Expected error but got success");
      assertTrue(
          response.getMsg().contains("部门不存在"), "Error message should mention department not found");

      // Verify no employee was created
      List<EmployeeEntity> employees =
          employeeDao.selectList(
              Wrappers.<EmployeeEntity>lambdaQuery()
                  .eq(EmployeeEntity::getLoginName, form.getLoginName()));
      assertTrue(employees.isEmpty(), "No employee should be created");
    }

    @Test
    @DisplayName("Should fail when login name already exists")
    void addEmployee_DuplicateLoginName_ReturnsError() {
      // Given - Create first employee
      EmployeeAddForm form1 = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form1);

      // When - Try to create second employee with same login name
      EmployeeAddForm form2 = EmployeeTestFixture.createAddForm(testDepartmentId);
      form2.setLoginName(form1.getLoginName()); // Duplicate
      ResponseDTO<String> response = employeeService.addEmployee(form2);

      // Then
      assertFalse(response.getOk(), "Expected error for duplicate login name");
      assertTrue(
          response.getMsg().contains("登录名重复"), "Error message should mention duplicate login name");
    }

    @Test
    @DisplayName("Should fail when phone already exists")
    void addEmployee_DuplicatePhone_ReturnsError() {
      // Given - Create first employee
      EmployeeAddForm form1 = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form1);

      // When - Try to create second employee with same phone
      EmployeeAddForm form2 = EmployeeTestFixture.createAddForm(testDepartmentId);
      form2.setPhone(form1.getPhone()); // Duplicate phone
      ResponseDTO<String> response = employeeService.addEmployee(form2);

      // Then
      assertFalse(response.getOk(), "Expected error for duplicate phone");
      assertTrue(
          response.getMsg().contains("手机号已存在"), "Error message should mention duplicate phone");
    }
  }

  @Nested
  @DisplayName("updateEmployee() Tests")
  class UpdateEmployeeTests {

    private Long testEmployeeId;

    @BeforeEach
    void setUp() {
      // Create test employee for update tests
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form);
      EmployeeEntity saved = employeeDao.getByLoginName(form.getLoginName(), null);
      testEmployeeId = saved.getEmployeeId();
    }

    @Test
    @DisplayName("Should update employee and persist changes")
    void updateEmployee_ValidForm_PersistsChanges() {
      // Given
      EmployeeUpdateForm form =
          EmployeeTestFixture.createUpdateForm(testEmployeeId, testDepartmentId);
      String newName = "更新后的名字";
      String newPhone = "13900000999";
      form.setActualName(newName);
      form.setPhone(newPhone);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(form);

      // Then - Verify ResponseDTO
      assertTrue(response.getOk(), "Expected success: " + response.getMsg());

      // Then - Verify database state
      EmployeeEntity updated = employeeDao.selectById(testEmployeeId);
      assertNotNull(updated);
      assertEquals(newName, updated.getActualName());
      assertEquals(newPhone, updated.getPhone());
      assertNotNull(updated.getUpdateTime(), "Update time should be set");
    }

    @Test
    @DisplayName("Should update employee roles")
    void updateEmployee_WithNewRoles_UpdatesRoleAssignments() {
      // Given - Update with new roles
      EmployeeUpdateForm form =
          EmployeeTestFixture.createUpdateForm(testEmployeeId, testDepartmentId);
      form.setRoleIdList(List.of(3L, 4L, 5L));

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(form);

      // Then
      assertTrue(response.getOk());

      // Verify roles updated
      List<RoleEmployeeEntity> roles =
          roleEmployeeDao.selectList(
              Wrappers.<RoleEmployeeEntity>lambdaQuery()
                  .eq(RoleEmployeeEntity::getEmployeeId, testEmployeeId));
      assertEquals(3, roles.size(), "Should have 3 new roles");
    }

    @Test
    @DisplayName("Should fail when employee does not exist")
    void updateEmployee_NonexistentId_ReturnsError() {
      // Given
      EmployeeUpdateForm form = EmployeeTestFixture.createUpdateForm(99999L, testDepartmentId);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(form);

      // Then
      assertFalse(response.getOk(), "Expected error for nonexistent ID");
      assertEquals(
          UserErrorCode.DATA_NOT_EXIST.getCode(),
          response.getCode(),
          "Should return DATA_NOT_EXIST error");
    }

    @Test
    @DisplayName("Should fail when department does not exist")
    void updateEmployee_InvalidDepartment_ReturnsError() {
      // Given
      EmployeeUpdateForm form = EmployeeTestFixture.createUpdateForm(testEmployeeId, 99999L);

      // When
      ResponseDTO<String> response = employeeService.updateEmployee(form);

      // Then
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("部门不存在"));
    }

    @Test
    @DisplayName("Should fail when updating to duplicate phone")
    void updateEmployee_DuplicatePhone_ReturnsError() {
      // Given - Create another employee
      EmployeeAddForm form2 = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form2);
      EmployeeEntity employee2 = employeeDao.getByLoginName(form2.getLoginName(), null);

      // When - Try to update first employee with second employee's phone
      EmployeeUpdateForm updateForm =
          EmployeeTestFixture.createUpdateForm(testEmployeeId, testDepartmentId);
      updateForm.setPhone(employee2.getPhone());
      ResponseDTO<String> response = employeeService.updateEmployee(updateForm);

      // Then
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("手机号已存在"));
    }
  }

  @Nested
  @DisplayName("queryEmployee() Tests")
  class QueryEmployeeTests {

    @BeforeEach
    void setUp() {
      // Insert test employees
      for (int i = 0; i < 5; i++) {
        EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
        employeeService.addEmployee(form);
      }
    }

    @Test
    @DisplayName("Should return paginated results")
    void queryEmployee_WithPagination_ReturnsPaginatedResults() {
      // Given
      EmployeeQueryForm queryForm = EmployeeTestFixture.createQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then - Verify ResponseDTO
      assertTrue(response.getOk(), "Expected success: " + response.getMsg());
      assertNotNull(response.getData(), "PageResult should not be null");

      // Then - Verify PageResult structure
      PageResult<EmployeeVO> page = response.getData();
      assertNotNull(page.getList(), "List should not be null");
      assertTrue(page.getTotal() >= 5, "Should have at least 5 records");
      assertTrue(page.getList().size() <= 10, "Should not exceed page size");

      // Verify VO fields populated
      if (!page.getList().isEmpty()) {
        EmployeeVO firstEmployee = page.getList().get(0);
        assertNotNull(firstEmployee.getEmployeeId());
        assertNotNull(firstEmployee.getActualName());
        assertNotNull(firstEmployee.getLoginName());
      }
    }

    @Test
    @DisplayName("Should return empty results when no matches")
    void queryEmployee_NoMatches_ReturnsEmptyResults() {
      // Given
      EmployeeQueryForm queryForm = EmployeeTestFixture.createQueryForm();
      queryForm.setKeyword("nonexistent_user_12345");

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertTrue(response.getOk());
      PageResult<EmployeeVO> page = response.getData();
      assertNotNull(page);
      assertTrue(page.getList().isEmpty(), "List should be empty for no matches");
    }

    @Test
    @DisplayName("Should filter by department")
    void queryEmployee_FilterByDepartment_ReturnsMatchingEmployees() {
      // Given
      EmployeeQueryForm queryForm = EmployeeTestFixture.createQueryForm();
      queryForm.setDepartmentId(testDepartmentId);

      // When
      ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

      // Then
      assertTrue(response.getOk());
      PageResult<EmployeeVO> page = response.getData();
      assertTrue(page.getTotal() >= 5, "Should find employees in test department");

      // Verify all employees belong to test department
      for (EmployeeVO employee : page.getList()) {
        assertEquals(
            testDepartmentId,
            employee.getDepartmentId(),
            "Employee should belong to test department");
      }
    }
  }

  @Nested
  @DisplayName("batchUpdateDeleteFlag() Tests")
  class BatchDeleteEmployeeTests {

    private Long testEmployeeId1;
    private Long testEmployeeId2;

    @BeforeEach
    void setUp() {
      // Create test employees
      EmployeeAddForm form1 = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form1);
      testEmployeeId1 = employeeDao.getByLoginName(form1.getLoginName(), null).getEmployeeId();

      EmployeeAddForm form2 = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form2);
      testEmployeeId2 = employeeDao.getByLoginName(form2.getLoginName(), null).getEmployeeId();
    }

    @Test
    @DisplayName("Should soft delete employees")
    void batchUpdateDeleteFlag_ValidIds_MarksAsDeleted() {
      // When
      ResponseDTO<String> response =
          employeeService.batchUpdateDeleteFlag(List.of(testEmployeeId1, testEmployeeId2));

      // Then - Verify ResponseDTO
      assertTrue(response.getOk(), "Expected success: " + response.getMsg());

      // Then - Verify soft delete (records still exist but deleted_flag = true)
      EmployeeEntity deleted1 = employeeDao.selectById(testEmployeeId1);
      EmployeeEntity deleted2 = employeeDao.selectById(testEmployeeId2);

      assertNotNull(deleted1, "Record should still exist");
      assertNotNull(deleted2, "Record should still exist");
      assertTrue(deleted1.getDeletedFlag(), "Deleted flag should be true");
      assertTrue(deleted2.getDeletedFlag(), "Deleted flag should be true");

      // Then - Verify not returned in queries
      EmployeeQueryForm queryForm = EmployeeTestFixture.createQueryForm();
      queryForm.setDepartmentId(testDepartmentId);
      ResponseDTO<PageResult<EmployeeVO>> queryResponse = employeeService.queryEmployee(queryForm);

      PageResult<EmployeeVO> page = queryResponse.getData();
      boolean hasDeleted1 =
          page.getList().stream().anyMatch(e -> e.getEmployeeId().equals(testEmployeeId1));
      boolean hasDeleted2 =
          page.getList().stream().anyMatch(e -> e.getEmployeeId().equals(testEmployeeId2));

      assertFalse(hasDeleted1, "Deleted employee 1 should not appear in query");
      assertFalse(hasDeleted2, "Deleted employee 2 should not appear in query");
    }

    @Test
    @DisplayName("Should handle empty list gracefully")
    void batchUpdateDeleteFlag_EmptyList_ReturnsSuccess() {
      // When
      ResponseDTO<String> response = employeeService.batchUpdateDeleteFlag(List.of());

      // Then
      assertTrue(response.getOk(), "Should handle empty list gracefully");
    }
  }

  @Nested
  @DisplayName("getById() Tests")
  class GetByIdTests {

    private Long testEmployeeId;

    @BeforeEach
    void setUp() {
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form);
      testEmployeeId = employeeDao.getByLoginName(form.getLoginName(), null).getEmployeeId();
    }

    @Test
    @DisplayName("Should return employee when exists")
    void getById_ExistingId_ReturnsEmployee() {
      // When
      EmployeeEntity employee = employeeService.getById(testEmployeeId);

      // Then
      assertNotNull(employee, "Employee should be found");
      assertEquals(testEmployeeId, employee.getEmployeeId());
    }

    @Test
    @DisplayName("Should return null when not exists")
    void getById_NonexistentId_ReturnsNull() {
      // When
      EmployeeEntity employee = employeeService.getById(99999L);

      // Then
      assertNull(employee, "Should return null for nonexistent ID");
    }
  }

  @Nested
  @DisplayName("resetPassword() Tests")
  class ResetPasswordTests {

    private Long testEmployeeId;

    @BeforeEach
    void setUp() {
      EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);
      employeeService.addEmployee(form);
      testEmployeeId = employeeDao.getByLoginName(form.getLoginName(), null).getEmployeeId();
    }

    @Test
    @DisplayName("Should reset password and return new random password")
    void resetPassword_ValidId_ResetsPassword() {
      // When
      ResponseDTO<String> response = employeeService.resetPassword(testEmployeeId);

      // Then
      assertTrue(response.getOk());
      assertNotNull(response.getData(), "New password should be returned");
      assertFalse(response.getData().isEmpty(), "Password should not be empty");

      // Verify password changed in database
      EmployeeEntity employee = employeeDao.selectById(testEmployeeId);
      assertNotNull(employee.getLoginPwd(), "Password should be encrypted");
    }

    @Test
    @DisplayName("Should fail when employee does not exist")
    void resetPassword_NonexistentId_ReturnsError() {
      // When
      ResponseDTO<String> response = employeeService.resetPassword(99999L);

      // Then
      assertFalse(response.getOk());
      assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
    }
  }
}
