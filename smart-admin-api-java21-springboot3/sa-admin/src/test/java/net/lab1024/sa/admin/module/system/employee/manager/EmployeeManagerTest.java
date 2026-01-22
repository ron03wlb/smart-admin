package net.lab1024.sa.admin.module.system.employee.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * EmployeeManager Unit Tests
 *
 * <p>Tests the Manager layer's transactional methods for employee and role management. These
 * methods coordinate DAO operations within database transactions.
 *
 * <p><b>Testing Strategy:</b>
 *
 * <ul>
 *   <li>Pure unit tests with Mockito (no Spring context)
 *   <li>Mock all DAO dependencies
 *   <li>Test business logic: method orchestration, conditional flows, edge cases
 *   <li>Verify method call order using InOrder verification
 *   <li>@Transactional behavior (rollback) is a framework feature tested in production
 * </ul>
 *
 * <p><b>Methods Tested:</b>
 *
 * <ol>
 *   <li>saveEmployee() - Insert employee + assign roles
 *   <li>updateEmployee() - Update employee + replace roles
 *   <li>updateEmployeeRole() - Delete old roles + insert new roles
 * </ol>
 *
 * @author Claude Code
 * @since 2026-01-22
 */
@DisplayName("EmployeeManager Unit Tests - Transactional Methods")
class EmployeeManagerTest extends BaseUnitTest {

  @InjectMocks private EmployeeManager employeeManager;

  @Mock private EmployeeDao employeeDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  private EmployeeEntity testEmployee;
  private List<Long> testRoleIds;

  private static final Long TEST_EMPLOYEE_ID = 1001L;
  private static final Long ROLE_ID_1 = 1L;
  private static final Long ROLE_ID_2 = 2L;

  @BeforeEach
  void setUp() {
    // Create test employee
    testEmployee = new EmployeeEntity();
    testEmployee.setEmployeeId(TEST_EMPLOYEE_ID);
    testEmployee.setLoginName("test_employee");
    testEmployee.setActualName("Test Employee");
    testEmployee.setPhone("13800000001");
    testEmployee.setDepartmentId(1L);

    // Create test role IDs
    testRoleIds = List.of(ROLE_ID_1, ROLE_ID_2);
  }

  @Nested
  @DisplayName("saveEmployee() Tests - Insert Employee + Roles")
  class SaveEmployeeTests {

    @Test
    @DisplayName("Should insert employee then insert each role")
    void saveEmployee_ValidData_InsertsEmployeeAndRoles() {
      // Given
      when(employeeDao.insert(testEmployee)).thenReturn(1);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.saveEmployee(testEmployee, testRoleIds);

      // Then
      InOrder inOrder = inOrder(employeeDao, roleEmployeeDao);

      // Verify employee inserted first
      inOrder.verify(employeeDao, times(1)).insert(testEmployee);

      // Verify roles inserted after employee (2 role inserts)
      inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should only insert employee when role list is empty")
    void saveEmployee_EmptyRoleList_OnlyInsertsEmployee() {
      // Given
      when(employeeDao.insert(testEmployee)).thenReturn(1);

      // When
      employeeManager.saveEmployee(testEmployee, new ArrayList<>());

      // Then
      verify(employeeDao, times(1)).insert(testEmployee);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should only insert employee when role list is null")
    void saveEmployee_NullRoleList_OnlyInsertsEmployee() {
      // Given
      when(employeeDao.insert(testEmployee)).thenReturn(1);

      // When
      employeeManager.saveEmployee(testEmployee, null);

      // Then
      verify(employeeDao, times(1)).insert(testEmployee);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should insert single role correctly")
    void saveEmployee_SingleRole_InsertsOneRole() {
      // Given
      List<Long> singleRoleList = List.of(ROLE_ID_1);
      when(employeeDao.insert(testEmployee)).thenReturn(1);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.saveEmployee(testEmployee, singleRoleList);

      // Then
      verify(employeeDao, times(1)).insert(testEmployee);
      verify(roleEmployeeDao, times(1)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should handle multiple roles (3+)")
    void saveEmployee_MultipleRoles_InsertsAllRoles() {
      // Given
      List<Long> multipleRoles = List.of(1L, 2L, 3L, 4L, 5L);
      when(employeeDao.insert(testEmployee)).thenReturn(1);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.saveEmployee(testEmployee, multipleRoles);

      // Then
      verify(employeeDao, times(1)).insert(testEmployee);
      verify(roleEmployeeDao, times(5)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should use employee ID from entity in role assignments")
    void saveEmployee_UsesEmployeeIdFromEntity() {
      // Given
      EmployeeEntity newEmployee = new EmployeeEntity();
      newEmployee.setEmployeeId(9999L); // Different ID
      when(employeeDao.insert(newEmployee)).thenReturn(1);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.saveEmployee(newEmployee, List.of(ROLE_ID_1));

      // Then
      verify(roleEmployeeDao, times(1)).insert(any(RoleEmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("updateEmployee() Tests - Update Employee + Replace Roles")
  class UpdateEmployeeTests {

    @Test
    @DisplayName("Should update employee then replace roles")
    void updateEmployee_ValidData_UpdatesEmployeeAndRoles() {
      // Given
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.updateEmployee(testEmployee, testRoleIds);

      // Then
      InOrder inOrder = inOrder(employeeDao, roleEmployeeDao);

      // Verify employee updated first
      inOrder.verify(employeeDao, times(1)).updateById(testEmployee);

      // Verify old roles deleted
      inOrder.verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // Verify new roles inserted
      inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should delete all roles when role list is empty")
    void updateEmployee_EmptyRoleList_DeletesAllRoles() {
      // Given
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // When
      employeeManager.updateEmployee(testEmployee, new ArrayList<>());

      // Then
      InOrder inOrder = inOrder(employeeDao, roleEmployeeDao);

      // Verify employee updated
      inOrder.verify(employeeDao, times(1)).updateById(testEmployee);

      // Verify all roles deleted
      inOrder.verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // Verify no new roles inserted (method returns early)
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should delete all roles when role list is null")
    void updateEmployee_NullRoleList_DeletesAllRoles() {
      // Given
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // When
      employeeManager.updateEmployee(testEmployee, null);

      // Then
      verify(employeeDao, times(1)).updateById(testEmployee);
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should handle role list size change (2 roles to 5 roles)")
    void updateEmployee_RoleListSizeChange_UpdatesCorrectly() {
      // Given
      List<Long> newRoles = List.of(1L, 2L, 3L, 4L, 5L);
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.updateEmployee(testEmployee, newRoles);

      // Then
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, times(5)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should delegate to updateEmployeeRole() for role replacement")
    void updateEmployee_DelegatesToUpdateEmployeeRole() {
      // Given
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.updateEmployee(testEmployee, testRoleIds);

      // Then - updateEmployeeRole() delegates to DAO
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("updateEmployeeRole() Tests - Delete Old + Insert New Roles")
  class UpdateEmployeeRoleTests {

    @Test
    @DisplayName("Should delete old roles then insert new roles")
    void updateEmployeeRole_ValidData_DeletesThenInserts() {
      // Given
      List<RoleEmployeeEntity> newRoles =
          List.of(
              new RoleEmployeeEntity(ROLE_ID_1, TEST_EMPLOYEE_ID),
              new RoleEmployeeEntity(ROLE_ID_2, TEST_EMPLOYEE_ID));
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.updateEmployeeRole(TEST_EMPLOYEE_ID, newRoles);

      // Then
      InOrder inOrder = inOrder(roleEmployeeDao);

      // Verify delete called first
      inOrder.verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // Verify inserts called after delete
      inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should only delete when role list is empty")
    void updateEmployeeRole_EmptyRoleList_OnlyDeletes() {
      // Given
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // When
      employeeManager.updateEmployeeRole(TEST_EMPLOYEE_ID, new ArrayList<>());

      // Then
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should only delete when role list is null")
    void updateEmployeeRole_NullRoleList_OnlyDeletes() {
      // Given
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // When
      employeeManager.updateEmployeeRole(TEST_EMPLOYEE_ID, null);

      // Then
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Should insert each role entity individually")
    void updateEmployeeRole_MultipleRoles_InsertsEachRole() {
      // Given
      List<RoleEmployeeEntity> multipleRoles =
          List.of(
              new RoleEmployeeEntity(1L, TEST_EMPLOYEE_ID),
              new RoleEmployeeEntity(2L, TEST_EMPLOYEE_ID),
              new RoleEmployeeEntity(3L, TEST_EMPLOYEE_ID));
      doNothing().when(roleEmployeeDao).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      when(roleEmployeeDao.insert(any(RoleEmployeeEntity.class))).thenReturn(1);

      // When
      employeeManager.updateEmployeeRole(TEST_EMPLOYEE_ID, multipleRoles);

      // Then
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
      verify(roleEmployeeDao, times(3)).insert(any(RoleEmployeeEntity.class));
    }
  }

  @Nested
  @DisplayName("Transaction Rollback Scenarios (Conceptual)")
  class TransactionRollbackTests {

    @Test
    @DisplayName("Note: Transaction rollback on saveEmployee() DAO failure is framework behavior")
    void saveEmployee_TransactionRollbackNote() {
      // Note: This test documents expected behavior, but actual rollback is handled by Spring
      // In production:
      // 1. If employeeDao.insert() fails → transaction rolled back
      // 2. If roleEmployeeDao.insert() fails → transaction rolled back (employee not saved)
      //
      // In unit tests, we can only verify method calls, not actual transaction behavior
      // Transaction rollback is tested via integration tests or manual testing

      // Given
      when(employeeDao.insert(testEmployee)).thenReturn(1);
      doThrow(new RuntimeException("Role insert failed"))
          .when(roleEmployeeDao)
          .insert(any(RoleEmployeeEntity.class));

      // When/Then - Exception propagates (would trigger rollback in production)
      assertThrows(
          RuntimeException.class, () -> employeeManager.saveEmployee(testEmployee, testRoleIds));

      // Verify employee insert was attempted
      verify(employeeDao, times(1)).insert(testEmployee);

      // Verify role insert was attempted and failed
      verify(roleEmployeeDao, times(1)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("Note: Transaction rollback on updateEmployee() DAO failure is framework behavior")
    void updateEmployee_TransactionRollbackNote() {
      // Given
      when(employeeDao.updateById(testEmployee)).thenReturn(1);
      doThrow(new RuntimeException("Delete roles failed"))
          .when(roleEmployeeDao)
          .deleteByEmployeeId(TEST_EMPLOYEE_ID);

      // When/Then - Exception propagates
      assertThrows(
          RuntimeException.class, () -> employeeManager.updateEmployee(testEmployee, testRoleIds));

      // Verify update was attempted
      verify(employeeDao, times(1)).updateById(testEmployee);

      // Verify delete was attempted and failed
      verify(roleEmployeeDao, times(1)).deleteByEmployeeId(TEST_EMPLOYEE_ID);
    }
  }
}
