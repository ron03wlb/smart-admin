package net.lab1024.sa.admin.module.system.role.manager;

import static org.mockito.Mockito.*;

import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * RoleManager Unit Tests
 *
 * <p>Test Coverage: 2 @Transactional methods (deleteRoleWithCascadeTransaction,
 * updateRoleTransaction)
 *
 * <p>Focus Areas: 1. Cascade deletion pattern (role → roleMenu → roleEmployee) 2. Transaction
 * ordering (InOrder verification) 3. Update transaction verification
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("RoleManager Unit Tests")
class RoleManagerTest extends BaseUnitTest {

  @Mock private RoleDao roleDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  private RoleManager roleManager;

  // Test constants
  private static final Long TEST_ROLE_ID = 1001L;
  private static final String TEST_ROLE_NAME = "測試角色";
  private static final String TEST_ROLE_CODE = "TEST_ROLE";

  @BeforeEach
  void setUp() {
    roleManager = new RoleManager(roleDao, roleMenuDao, roleEmployeeDao);
  }

  // ==================== deleteRoleWithCascadeTransaction() Tests ====================

  @Nested
  @DisplayName("deleteRoleWithCascadeTransaction() Tests - Cascade Deletion Pattern")
  class DeleteRoleWithCascadeTests {

    @Test
    @DisplayName("Should delete role, role menus, and role employees in correct order")
    void deleteRoleWithCascade_ValidRoleId_DeletesAllRelatedData() {
      // Given - deleteById returns int (number of rows deleted)
      when(roleDao.deleteById(TEST_ROLE_ID)).thenReturn(1);
      doNothing().when(roleMenuDao).deleteByRoleId(TEST_ROLE_ID);
      doNothing().when(roleEmployeeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleManager.deleteRoleWithCascadeTransaction(TEST_ROLE_ID);

      // Then - verify all three deletions occurred
      verify(roleDao, times(1)).deleteById(TEST_ROLE_ID);
      verify(roleMenuDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleEmployeeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should delete in correct order: role → roleMenu → roleEmployee")
    void deleteRoleWithCascade_TransactionOrder_CorrectSequence() {
      // Given
      when(roleDao.deleteById(TEST_ROLE_ID)).thenReturn(1);
      doNothing().when(roleMenuDao).deleteByRoleId(TEST_ROLE_ID);
      doNothing().when(roleEmployeeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleManager.deleteRoleWithCascadeTransaction(TEST_ROLE_ID);

      // Then - verify deletion order
      var inOrder = inOrder(roleDao, roleMenuDao, roleEmployeeDao);
      inOrder.verify(roleDao).deleteById(TEST_ROLE_ID);
      inOrder.verify(roleMenuDao).deleteByRoleId(TEST_ROLE_ID);
      inOrder.verify(roleEmployeeDao).deleteByRoleId(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should call deleteById with correct roleId")
    void deleteRoleWithCascade_CorrectRoleId_DeletesCorrectRole() {
      // Given
      Long specificRoleId = 9999L;
      when(roleDao.deleteById(specificRoleId)).thenReturn(1);
      doNothing().when(roleMenuDao).deleteByRoleId(specificRoleId);
      doNothing().when(roleEmployeeDao).deleteByRoleId(specificRoleId);

      // When
      roleManager.deleteRoleWithCascadeTransaction(specificRoleId);

      // Then
      verify(roleDao, times(1)).deleteById(specificRoleId);
      verify(roleMenuDao, times(1)).deleteByRoleId(specificRoleId);
      verify(roleEmployeeDao, times(1)).deleteByRoleId(specificRoleId);
      // Verify other role not affected
      verify(roleDao, never()).deleteById(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should not skip cascade deletions even if role not found")
    void deleteRoleWithCascade_RoleNotFound_StillDeletesCascadeData() {
      // Given - simulate role not found (returns 0), but still delete related data
      when(roleDao.deleteById(TEST_ROLE_ID)).thenReturn(0);
      doNothing().when(roleMenuDao).deleteByRoleId(TEST_ROLE_ID);
      doNothing().when(roleEmployeeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleManager.deleteRoleWithCascadeTransaction(TEST_ROLE_ID);

      // Then - cascade deletions should still occur
      verify(roleMenuDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleEmployeeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
    }

    @Test
    @DisplayName("Should handle multiple cascade deletes for different roles")
    void deleteRoleWithCascade_MultipleCalls_HandlesSeparately() {
      // Given
      Long roleId1 = 1001L;
      Long roleId2 = 1002L;
      when(roleDao.deleteById(anyLong())).thenReturn(1);
      doNothing().when(roleMenuDao).deleteByRoleId(anyLong());
      doNothing().when(roleEmployeeDao).deleteByRoleId(anyLong());

      // When
      roleManager.deleteRoleWithCascadeTransaction(roleId1);
      roleManager.deleteRoleWithCascadeTransaction(roleId2);

      // Then
      verify(roleDao, times(1)).deleteById(roleId1);
      verify(roleDao, times(1)).deleteById(roleId2);
      verify(roleMenuDao, times(1)).deleteByRoleId(roleId1);
      verify(roleMenuDao, times(1)).deleteByRoleId(roleId2);
      verify(roleEmployeeDao, times(1)).deleteByRoleId(roleId1);
      verify(roleEmployeeDao, times(1)).deleteByRoleId(roleId2);
    }
  }

  // ==================== updateRoleTransaction() Tests ====================

  @Nested
  @DisplayName("updateRoleTransaction() Tests - Role Update Transaction")
  class UpdateRoleTransactionTests {

    private RoleEntity testRoleEntity;

    @BeforeEach
    void setUp() {
      testRoleEntity = new RoleEntity();
      testRoleEntity.setRoleId(TEST_ROLE_ID);
      testRoleEntity.setRoleName(TEST_ROLE_NAME);
      testRoleEntity.setRoleCode(TEST_ROLE_CODE);
    }

    @Test
    @DisplayName("Should update role with correct entity")
    void updateRoleTransaction_ValidEntity_UpdatesRole() {
      // Given
      when(roleDao.updateById(testRoleEntity)).thenReturn(1);

      // When
      roleManager.updateRoleTransaction(testRoleEntity);

      // Then
      verify(roleDao, times(1)).updateById(testRoleEntity);
    }

    @Test
    @DisplayName("Should call updateById with correct roleEntity")
    void updateRoleTransaction_CorrectEntity_PassesToDao() {
      // Given
      RoleEntity anotherEntity = new RoleEntity();
      anotherEntity.setRoleId(2002L);
      anotherEntity.setRoleName("Another Role");
      when(roleDao.updateById(anotherEntity)).thenReturn(1);

      // When
      roleManager.updateRoleTransaction(anotherEntity);

      // Then
      verify(roleDao, times(1)).updateById(anotherEntity);
      verify(roleDao, never()).updateById(testRoleEntity);
    }

    @Test
    @DisplayName("Should handle role entity with updated fields")
    void updateRoleTransaction_UpdatedFields_UpdatesCorrectly() {
      // Given
      testRoleEntity.setRoleName("Updated Role Name");
      testRoleEntity.setRoleCode("UPDATED_CODE");
      when(roleDao.updateById(testRoleEntity)).thenReturn(1);

      // When
      roleManager.updateRoleTransaction(testRoleEntity);

      // Then
      verify(roleDao, times(1)).updateById(testRoleEntity);
    }

    @Test
    @DisplayName("Should handle multiple update transactions")
    void updateRoleTransaction_MultipleCalls_HandlesSequentially() {
      // Given
      RoleEntity entity1 = new RoleEntity();
      entity1.setRoleId(1001L);
      RoleEntity entity2 = new RoleEntity();
      entity2.setRoleId(1002L);
      when(roleDao.updateById(any(RoleEntity.class))).thenReturn(1);

      // When
      roleManager.updateRoleTransaction(entity1);
      roleManager.updateRoleTransaction(entity2);

      // Then
      verify(roleDao, times(2)).updateById(any(RoleEntity.class));
      verify(roleDao, times(1)).updateById(entity1);
      verify(roleDao, times(1)).updateById(entity2);
    }

    @Test
    @DisplayName("Should not affect other operations when updating")
    void updateRoleTransaction_Update_DoesNotTriggerDelete() {
      // Given
      when(roleDao.updateById(testRoleEntity)).thenReturn(1);

      // When
      roleManager.updateRoleTransaction(testRoleEntity);

      // Then - only update called, no deletes
      verify(roleDao, times(1)).updateById(testRoleEntity);
      verify(roleDao, never()).deleteById(anyLong());
      verify(roleMenuDao, never()).deleteByRoleId(anyLong());
      verify(roleEmployeeDao, never()).deleteByRoleId(anyLong());
    }
  }
}
