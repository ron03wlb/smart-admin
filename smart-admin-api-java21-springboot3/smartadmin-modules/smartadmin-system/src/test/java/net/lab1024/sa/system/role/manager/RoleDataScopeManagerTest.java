package net.lab1024.sa.system.role.manager;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.system.role.domain.entity.RoleDataScopeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleDataScopeManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>批量更新角色數據範圍事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDataScopeManager 單元測試")
class RoleDataScopeManagerTest {

  @Mock private RoleDataScopeDao roleDataScopeDao;

  @Spy @InjectMocks private RoleDataScopeManager roleDataScopeManager;

  // ==================== updateRoleDataScopeListTransaction 測試 ====================

  @Nested
  @DisplayName("updateRoleDataScopeListTransaction 批量更新事務測試")
  class UpdateRoleDataScopeListTransactionTest {

    @Test
    @DisplayName("正常情況：應該先刪除舊數據再批量保存新數據")
    void shouldDeleteAndSaveNew() {
      // Given
      Long roleId = 1L;
      RoleDataScopeEntity entity1 = createTestEntity(roleId, 1);
      RoleDataScopeEntity entity2 = createTestEntity(roleId, 2);
      List<RoleDataScopeEntity> entityList = Arrays.asList(entity1, entity2);

      // Mock ServiceImpl methods
      doReturn(roleDataScopeDao).when(roleDataScopeManager).getBaseMapper();
      doReturn(true).when(roleDataScopeManager).saveBatch(anyList());

      // When
      roleDataScopeManager.updateRoleDataScopeListTransaction(roleId, entityList);

      // Then
      verify(roleDataScopeDao).deleteByRoleId(roleId);
      verify(roleDataScopeManager).saveBatch(entityList);
    }
  }

  // ==================== Helper Methods ====================

  private RoleDataScopeEntity createTestEntity(Long roleId, Integer dataScopeType) {
    RoleDataScopeEntity entity = new RoleDataScopeEntity();
    entity.setRoleId(roleId);
    entity.setDataScopeType(dataScopeType);
    entity.setViewType(1);
    return entity;
  }
}
