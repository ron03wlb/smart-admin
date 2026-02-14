package net.lab1024.sa.system.role.manager;

import static org.mockito.Mockito.verify;

import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>角色級聯刪除事務
 *   <li>角色更新事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleManager 單元測試")
class RoleManagerTest {

  @Mock private RoleDao roleDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @InjectMocks private RoleManager roleManager;

  // ==================== deleteRoleWithCascadeTransaction 測試 ====================

  @Nested
  @DisplayName("deleteRoleWithCascadeTransaction 級聯刪除事務測試")
  class DeleteRoleWithCascadeTransactionTest {

    @Test
    @DisplayName("正常情況：應該刪除角色及所有關聯數據")
    void shouldDeleteRoleAndAllRelations() {
      // Given
      Long roleId = 1L;

      // When
      roleManager.deleteRoleWithCascadeTransaction(roleId);

      // Then
      verify(roleDao).deleteById(roleId);
      verify(roleMenuDao).deleteByRoleId(roleId);
      verify(roleEmployeeDao).deleteByRoleId(roleId);
    }
  }

  // ==================== updateRoleTransaction 測試 ====================

  @Nested
  @DisplayName("updateRoleTransaction 更新事務測試")
  class UpdateRoleTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新角色信息")
    void shouldUpdateRole() {
      // Given
      RoleEntity roleEntity = new RoleEntity();
      roleEntity.setRoleId(1L);
      roleEntity.setRoleName("UpdatedRole");
      roleEntity.setRoleCode("UPDATED");

      // When
      roleManager.updateRoleTransaction(roleEntity);

      // Then
      verify(roleDao).updateById(roleEntity);
    }
  }
}
