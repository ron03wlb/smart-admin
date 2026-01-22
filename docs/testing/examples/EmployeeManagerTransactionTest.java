package net.lab1024.sa.admin.module.system.employee.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmployeeManager Transaction Integration Test
 *
 * Demonstrates:
 * - Testing @Transactional Manager methods
 * - Verifying successful commits (all DAOs execute)
 * - Testing rollback scenarios (exceptions trigger rollback)
 * - Using @SpyBean to verify operation order
 *
 * Challenge #2: Testing @Transactional Manager Methods
 *
 * @author SmartAdmin Testing Guide
 * @see <a href="../integration/transaction-testing.md">Transaction Testing Guide</a>
 */
@SpringBootTest
@Transactional  // Auto-rollback after each test
@DisplayName("EmployeeManager Transaction Tests")
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private RoleEmployeeDao roleEmployeeDao;

    /**
     * SpyBean allows partial mocking to simulate failures
     * while keeping real implementation for verification
     */
    @SpyBean
    private RoleEmployeeDao roleEmployeeDaoSpy;

    private Long testEmployeeId;

    @AfterEach
    void cleanup() {
        // Cleanup test data if needed
        if (testEmployeeId != null) {
            // Transaction will rollback automatically due to @Transactional
        }
    }

    // ========================================
    // Test: saveEmployee() - Successful Commits
    // ========================================

    @Nested
    @DisplayName("saveEmployee() - Successful Commits")
    class SaveEmployeeSuccessTests {

        @Test
        @DisplayName("Valid employee with roles - commits employee and roles")
        void testSaveEmployee_ValidEmployeeWithRoles_CommitsBoth() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L, 2L, 3L);

            // When
            employeeManager.saveEmployee(employee, roleIds);

            // Then - verify employee saved
            assertNotNull(employee.getEmployeeId(), "Employee ID should be set after insert");

            EmployeeEntity savedEmployee = employeeDao.selectById(employee.getEmployeeId());
            assertNotNull(savedEmployee, "Employee should exist in database");
            assertEquals(employee.getLoginName(), savedEmployee.getLoginName());
            assertEquals(employee.getActualName(), savedEmployee.getActualName());

            testEmployeeId = employee.getEmployeeId();

            // Verify roles saved
            LambdaQueryWrapper<RoleEmployeeEntity> wrapper = Wrappers.<RoleEmployeeEntity>lambdaQuery()
                .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId());

            List<RoleEmployeeEntity> savedRoles = roleEmployeeDao.selectList(wrapper);
            assertEquals(3, savedRoles.size(), "Should save 3 role assignments");

            // Verify correct role IDs
            List<Long> savedRoleIds = savedRoles.stream()
                .map(RoleEmployeeEntity::getRoleId)
                .collect(Collectors.toList());

            assertTrue(savedRoleIds.containsAll(List.of(1L, 2L, 3L)));
        }

        @Test
        @DisplayName("Valid employee with empty roles - saves employee only")
        void testSaveEmployee_EmptyRoles_SavesEmployeeOnly() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> emptyRoles = List.of();

            // When
            employeeManager.saveEmployee(employee, emptyRoles);

            // Then
            assertNotNull(employee.getEmployeeId());

            EmployeeEntity savedEmployee = employeeDao.selectById(employee.getEmployeeId());
            assertNotNull(savedEmployee);

            testEmployeeId = employee.getEmployeeId();

            // Verify no roles saved
            List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(0, roles.size(), "Should have no role assignments");
        }

        @Test
        @DisplayName("Multiple role saves - all succeed")
        void testSaveEmployee_MultipleRoles_AllSaved() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L, 2L, 3L, 4L, 5L);

            // When
            employeeManager.saveEmployee(employee, roleIds);

            // Then
            assertNotNull(employee.getEmployeeId());
            testEmployeeId = employee.getEmployeeId();

            // Verify all roles saved
            List<RoleEmployeeEntity> savedRoles = roleEmployeeDao.selectList(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(5, savedRoles.size());
        }
    }

    // ========================================
    // Test: saveEmployee() - Rollback Scenarios
    // ========================================

    @Nested
    @DisplayName("saveEmployee() - Rollback Scenarios")
    class SaveEmployeeRollbackTests {

        @Test
        @DisplayName("Role insert fails - rollbacks employee insert")
        void testSaveEmployee_RoleInsertFails_RollbacksEmployee() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L);

            // Mock roleEmployeeDao to throw exception on insert
            doThrow(new RuntimeException("Simulated database constraint violation"))
                .when(roleEmployeeDaoSpy).insert(any(RoleEmployeeEntity.class));

            // When/Then - expect exception
            assertThrows(RuntimeException.class, () -> {
                employeeManager.saveEmployee(employee, roleIds);
            }, "Should throw exception from role insert failure");

            // Then - verify employee NOT saved due to rollback
            // Note: employee.getEmployeeId() may be set (from insert attempt)
            // but database should NOT contain the record

            EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
            assertNull(result, "Employee should not exist in database after rollback");
        }

        @Test
        @DisplayName("Second role insert fails - rollbacks all")
        void testSaveEmployee_SecondRoleFails_RollbacksAll() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            List<Long> roleIds = List.of(1L, 2L);

            // Mock to fail on second insert
            doNothing().doThrow(new RuntimeException("Second insert failed"))
                .when(roleEmployeeDaoSpy).insert(any(RoleEmployeeEntity.class));

            // When/Then
            assertThrows(RuntimeException.class, () -> {
                employeeManager.saveEmployee(employee, roleIds);
            });

            // Verify nothing committed
            EmployeeEntity empResult = employeeDao.selectByLoginName(employee.getLoginName());
            assertNull(empResult, "Employee should be rolled back");

            // If employee ID was set, verify no roles exist
            if (employee.getEmployeeId() != null) {
                List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
                    Wrappers.<RoleEmployeeEntity>lambdaQuery()
                        .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));
                assertEquals(0, roles.size(), "No roles should exist after rollback");
            }
        }
    }

    // ========================================
    // Test: updateEmployee() - Success
    // ========================================

    @Nested
    @DisplayName("updateEmployee() - Successful Updates")
    class UpdateEmployeeSuccessTests {

        @Test
        @DisplayName("Update employee and replace roles - all committed")
        void testUpdateEmployee_ReplacesRoles_Success() {
            // Given - create employee first
            EmployeeEntity employee = createTestEmployee();
            employeeManager.saveEmployee(employee, List.of(1L, 2L));

            testEmployeeId = employee.getEmployeeId();

            // Update employee data
            String newName = "Updated Name";
            employee.setActualName(newName);

            List<Long> newRoleIds = List.of(3L, 4L);  // Different roles

            // When
            employeeManager.updateEmployee(employee, newRoleIds);

            // Then - employee updated
            EmployeeEntity updated = employeeDao.selectById(employee.getEmployeeId());
            assertEquals(newName, updated.getActualName());

            // Roles replaced
            List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(2, roles.size());
            List<Long> savedRoleIds = roles.stream()
                .map(RoleEmployeeEntity::getRoleId)
                .collect(Collectors.toList());

            assertTrue(savedRoleIds.containsAll(List.of(3L, 4L)));
            assertFalse(savedRoleIds.contains(1L), "Old role should be removed");
            assertFalse(savedRoleIds.contains(2L), "Old role should be removed");
        }

        @Test
        @DisplayName("Update with empty roles - deletes all roles")
        void testUpdateEmployee_EmptyRoles_DeletesAllRoles() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            employeeManager.saveEmployee(employee, List.of(1L, 2L));

            testEmployeeId = employee.getEmployeeId();

            // When - update with empty roles
            employeeManager.updateEmployee(employee, List.of());

            // Then - all roles deleted
            List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(0, roles.size(), "All roles should be deleted");
        }
    }

    // ========================================
    // Test: updateEmployeeRole() - Operation Order
    // ========================================

    @Nested
    @DisplayName("updateEmployeeRole() - Operation Order")
    class UpdateEmployeeRoleOrderTests {

        @Test
        @DisplayName("Delete called before insert - correct order")
        void testUpdateEmployeeRole_DeleteThenInsert_CorrectOrder() {
            // Given - create employee with initial roles
            EmployeeEntity employee = createTestEmployee();
            employeeManager.saveEmployee(employee, List.of(1L));

            testEmployeeId = employee.getEmployeeId();

            clearInvocations(roleEmployeeDaoSpy);

            List<RoleEmployeeEntity> newRoles = List.of(
                new RoleEmployeeEntity(3L, employee.getEmployeeId()),
                new RoleEmployeeEntity(4L, employee.getEmployeeId())
            );

            // When
            employeeManager.updateEmployeeRole(employee.getEmployeeId(), newRoles);

            // Then - verify delete called BEFORE insert
            InOrder inOrder = inOrder(roleEmployeeDaoSpy);
            inOrder.verify(roleEmployeeDaoSpy).deleteByEmployeeId(employee.getEmployeeId());
            inOrder.verify(roleEmployeeDaoSpy, times(2)).insert(any(RoleEmployeeEntity.class));
        }

        @Test
        @DisplayName("Delete fails - no inserts performed")
        void testUpdateEmployeeRole_DeleteFails_NoInserts() {
            // Given
            EmployeeEntity employee = createTestEmployee();
            employeeManager.saveEmployee(employee, List.of(1L));

            testEmployeeId = employee.getEmployeeId();

            Long initialCount = roleEmployeeDao.selectCount(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            List<RoleEmployeeEntity> newRoles = List.of(
                new RoleEmployeeEntity(99L, employee.getEmployeeId())
            );

            // Simulate delete failure
            doThrow(new RuntimeException("Delete failed"))
                .when(roleEmployeeDaoSpy).deleteByEmployeeId(employee.getEmployeeId());

            // When/Then
            assertThrows(RuntimeException.class, () -> {
                employeeManager.updateEmployeeRole(employee.getEmployeeId(), newRoles);
            });

            // Verify no changes
            Long finalCount = roleEmployeeDao.selectCount(
                Wrappers.<RoleEmployeeEntity>lambdaQuery()
                    .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId()));

            assertEquals(initialCount, finalCount, "Role count should be unchanged after rollback");
        }
    }

    // ========================================
    // Test: rollbackFor = Throwable.class
    // ========================================

    @Nested
    @DisplayName("rollbackFor = Throwable.class")
    class RollbackForThrowableTests {

        @Test
        @DisplayName("RuntimeException triggers rollback")
        void testSaveEmployee_RuntimeException_Rollbacks() {
            // Given
            EmployeeEntity employee = createTestEmployee();

            doThrow(new RuntimeException("Runtime error"))
                .when(roleEmployeeDaoSpy).insert(any());

            // When/Then
            assertThrows(RuntimeException.class, () -> {
                employeeManager.saveEmployee(employee, List.of(1L));
            });

            // Verify rollback
            EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
            assertNull(result, "Should rollback on RuntimeException");
        }

        @Test
        @DisplayName("Error triggers rollback (rollbackFor = Throwable)")
        void testSaveEmployee_Error_Rollbacks() {
            // Given
            EmployeeEntity employee = createTestEmployee();

            // Mock to throw Error (not Exception)
            doThrow(new OutOfMemoryError("Simulated OOM"))
                .when(roleEmployeeDaoSpy).insert(any());

            // When/Then - Error should trigger rollback due to rollbackFor = Throwable.class
            assertThrows(OutOfMemoryError.class, () -> {
                employeeManager.saveEmployee(employee, List.of(1L));
            });

            // Verify rollback
            EmployeeEntity result = employeeDao.selectByLoginName(employee.getLoginName());
            assertNull(result, "Should rollback even on Error (rollbackFor = Throwable)");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Create test employee entity with unique login name
     */
    private EmployeeEntity createTestEmployee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("test_emp_" + System.currentTimeMillis());
        employee.setActualName("Test Employee");
        employee.setPhone("13800138000");
        employee.setDepartmentId(1L);
        employee.setDisabledFlag(false);
        employee.setDeletedFlag(false);
        employee.setAdministratorFlag(false);
        return employee;
    }
}
