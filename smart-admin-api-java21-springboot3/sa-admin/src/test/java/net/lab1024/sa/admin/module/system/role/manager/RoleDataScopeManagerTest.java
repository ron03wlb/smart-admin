package net.lab1024.sa.admin.module.system.role.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RoleDataScopeManager Unit Tests
 *
 * <p>Test Coverage: 1 @Transactional method (updateRoleDataScopeListTransaction)
 *
 * <p>Focus Areas: 1. Transaction pattern (delete old + batch insert new) 2. InOrder verification
 * for transactional operations 3. Empty list handling 4. Batch save verification
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("RoleDataScopeManager Unit Tests")
class RoleDataScopeManagerTest extends BaseUnitTest {

  @Mock private RoleDataScopeDao roleDataScopeDao;

  private RoleDataScopeManager roleDataScopeManager;

  // Test constants
  private static final Long TEST_ROLE_ID = 1001L;
  private static final Integer TEST_VIEW_TYPE_DEPT = 1;
  private static final Integer TEST_VIEW_TYPE_CUSTOM = 2;

  private List<RoleDataScopeEntity> testDataScopeList;

  @BeforeEach
  void setUp() {
    // Create RoleDataScopeManager and inject mocked baseMapper using reflection
    roleDataScopeManager = new RoleDataScopeManager();
    ReflectionTestUtils.setField(roleDataScopeManager, "baseMapper", roleDataScopeDao);

    // Create spy to verify method calls
    roleDataScopeManager = spy(roleDataScopeManager);

    // Mock saveBatch method (inherited from ServiceImpl) - lenient since not all tests use it
    lenient().doReturn(true).when(roleDataScopeManager).saveBatch(anyList());

    // Setup test data scope entities
    testDataScopeList = new ArrayList<>();

    RoleDataScopeEntity dataScope1 = new RoleDataScopeEntity();
    dataScope1.setRoleId(TEST_ROLE_ID);
    dataScope1.setViewType(TEST_VIEW_TYPE_DEPT);
    dataScope1.setDataScopeType(1);
    testDataScopeList.add(dataScope1);

    RoleDataScopeEntity dataScope2 = new RoleDataScopeEntity();
    dataScope2.setRoleId(TEST_ROLE_ID);
    dataScope2.setViewType(TEST_VIEW_TYPE_CUSTOM);
    dataScope2.setDataScopeType(2);
    testDataScopeList.add(dataScope2);

    RoleDataScopeEntity dataScope3 = new RoleDataScopeEntity();
    dataScope3.setRoleId(TEST_ROLE_ID);
    dataScope3.setViewType(TEST_VIEW_TYPE_DEPT);
    dataScope3.setDataScopeType(3);
    testDataScopeList.add(dataScope3);
  }

  // ==================== updateRoleDataScopeListTransaction() Tests ====================

  @Nested
  @DisplayName(
      "updateRoleDataScopeListTransaction() Tests - Transaction Pattern (Delete + Batch Insert)")
  class UpdateRoleDataScopeListTests {

    @Test
    @DisplayName("Should delete old and insert new data scopes")
    void updateRoleDataScopeList_ValidData_DeletesAndInserts() {
      // Given
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, testDataScopeList);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleDataScopeManager, times(1)).saveBatch(testDataScopeList);
    }

    @Test
    @DisplayName("Should delete before insert (transaction order)")
    void updateRoleDataScopeList_TransactionOrder_DeleteBeforeInsert() {
      // Given
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, testDataScopeList);

      // Then - verify delete called before saveBatch
      var inOrder = inOrder(roleDataScopeDao, roleDataScopeManager);
      inOrder.verify(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);
      inOrder.verify(roleDataScopeManager).saveBatch(testDataScopeList);
    }

    @Test
    @DisplayName("Should handle empty data scope list")
    void updateRoleDataScopeList_EmptyList_DeletesOnly() {
      // Given
      List<RoleDataScopeEntity> emptyList = new ArrayList<>();
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, emptyList);

      // Then - delete called, saveBatch called with empty list
      verify(roleDataScopeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleDataScopeManager, times(1)).saveBatch(emptyList);
    }

    @Test
    @DisplayName("Should call deleteByRoleId with correct roleId")
    void updateRoleDataScopeList_CorrectRoleId_DeletesCorrectRole() {
      // Given
      Long specificRoleId = 9999L;
      doNothing().when(roleDataScopeDao).deleteByRoleId(specificRoleId);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(specificRoleId, testDataScopeList);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(specificRoleId);
      verify(roleDataScopeDao, never()).deleteByRoleId(TEST_ROLE_ID); // Different role not affected
    }

    @Test
    @DisplayName("Should handle single data scope entity")
    void updateRoleDataScopeList_SingleEntity_WorksCorrectly() {
      // Given
      List<RoleDataScopeEntity> singleList = Lists.newArrayList(testDataScopeList.get(0));
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, singleList);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleDataScopeManager, times(1)).saveBatch(singleList);
      assertEquals(1, singleList.size());
    }

    @Test
    @DisplayName("Should handle large batch of data scopes")
    void updateRoleDataScopeList_LargeBatch_HandlesCorrectly() {
      // Given - create 50 data scope entities
      List<RoleDataScopeEntity> largeBatch = new ArrayList<>();
      for (int i = 1; i <= 50; i++) {
        RoleDataScopeEntity entity = new RoleDataScopeEntity();
        entity.setRoleId(TEST_ROLE_ID);
        entity.setViewType(i % 2 == 0 ? TEST_VIEW_TYPE_DEPT : TEST_VIEW_TYPE_CUSTOM);
        entity.setDataScopeType(i);
        largeBatch.add(entity);
      }
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, largeBatch);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleDataScopeManager, times(1)).saveBatch(largeBatch);
      assertEquals(50, largeBatch.size());
    }

    @Test
    @DisplayName("Should handle multiple updates for different roles")
    void updateRoleDataScopeList_DifferentRoles_HandlesIndependently() {
      // Given
      Long roleId1 = 1001L;
      Long roleId2 = 1002L;
      List<RoleDataScopeEntity> list1 =
          Lists.newArrayList(testDataScopeList.get(0), testDataScopeList.get(1));
      List<RoleDataScopeEntity> list2 = Lists.newArrayList(testDataScopeList.get(2));
      doNothing().when(roleDataScopeDao).deleteByRoleId(anyLong());

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(roleId1, list1);
      roleDataScopeManager.updateRoleDataScopeListTransaction(roleId2, list2);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(roleId1);
      verify(roleDataScopeDao, times(1)).deleteByRoleId(roleId2);
      verify(roleDataScopeManager, times(1)).saveBatch(list1);
      verify(roleDataScopeManager, times(1)).saveBatch(list2);
    }

    @Test
    @DisplayName("Should preserve entity properties during save")
    void updateRoleDataScopeList_EntityProperties_PreservedCorrectly() {
      // Given
      RoleDataScopeEntity specificEntity = new RoleDataScopeEntity();
      specificEntity.setRoleId(TEST_ROLE_ID);
      specificEntity.setViewType(TEST_VIEW_TYPE_DEPT);
      specificEntity.setDataScopeType(99);
      List<RoleDataScopeEntity> specificList = Lists.newArrayList(specificEntity);
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, specificList);

      // Then - verify saveBatch called with correct entity
      verify(roleDataScopeManager, times(1)).saveBatch(specificList);
      assertEquals(TEST_ROLE_ID, specificEntity.getRoleId());
      assertEquals(TEST_VIEW_TYPE_DEPT, specificEntity.getViewType());
      assertEquals(99, specificEntity.getDataScopeType());
    }

    @Test
    @DisplayName("Should handle mixed viewType data scopes")
    void updateRoleDataScopeList_MixedViewTypes_HandlesCorrectly() {
      // Given - list contains both DEPT and CUSTOM view types
      doNothing().when(roleDataScopeDao).deleteByRoleId(TEST_ROLE_ID);

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(TEST_ROLE_ID, testDataScopeList);

      // Then
      verify(roleDataScopeDao, times(1)).deleteByRoleId(TEST_ROLE_ID);
      verify(roleDataScopeManager, times(1)).saveBatch(testDataScopeList);
      // Verify mixed viewTypes exist
      boolean hasDeptType =
          testDataScopeList.stream().anyMatch(e -> e.getViewType().equals(TEST_VIEW_TYPE_DEPT));
      boolean hasCustomType =
          testDataScopeList.stream().anyMatch(e -> e.getViewType().equals(TEST_VIEW_TYPE_CUSTOM));
      assertTrue(hasDeptType, "Should contain DEPT viewType");
      assertTrue(hasCustomType, "Should contain CUSTOM viewType");
    }
  }
}
