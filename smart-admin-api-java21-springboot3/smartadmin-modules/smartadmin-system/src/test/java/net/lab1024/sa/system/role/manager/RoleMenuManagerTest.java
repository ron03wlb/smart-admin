package net.lab1024.sa.system.role.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.entity.RoleMenuEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleMenuManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>更新角色菜單權限事務
 *   <li>根據角色查詢菜單列表
 *   <li>管理員菜單權限特殊處理
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleMenuManager 單元測試")
class RoleMenuManagerTest {

  @Mock private RoleMenuDao roleMenuDao;

  @Spy @InjectMocks private RoleMenuManager roleMenuManager;

  // ==================== updateRoleMenuTransaction 測試 ====================

  @Nested
  @DisplayName("updateRoleMenuTransaction 更新角色菜單事務測試")
  class UpdateRoleMenuTransactionTest {

    @Test
    @DisplayName("正常情況：應該先刪除舊權限再批量保存新權限")
    void shouldDeleteAndSaveNew() {
      // Given
      Long roleId = 1L;
      RoleMenuEntity entity1 = createTestRoleMenuEntity(roleId, 100L);
      RoleMenuEntity entity2 = createTestRoleMenuEntity(roleId, 101L);
      List<RoleMenuEntity> entityList = Arrays.asList(entity1, entity2);

      // Mock ServiceImpl methods
      doReturn(true).when(roleMenuManager).saveBatch(anyList());

      // When
      roleMenuManager.updateRoleMenuTransaction(roleId, entityList);

      // Then
      verify(roleMenuDao).deleteByRoleId(roleId);
      verify(roleMenuManager).saveBatch(entityList);
    }
  }

  // ==================== getMenuList 測試 ====================

  @Nested
  @DisplayName("getMenuList 查詢菜單列表測試")
  class GetMenuListTest {

    @Test
    @DisplayName("正常情況：管理員應返回所有菜單")
    void shouldReturnAllMenusForAdmin() {
      // Given
      List<Long> roleIdList = Collections.singletonList(1L);
      Boolean administratorFlag = true;

      MenuEntity menu1 = createTestMenuEntity(1L, "菜單1");
      MenuEntity menu2 = createTestMenuEntity(2L, "菜單2");

      when(roleMenuDao.selectMenuListByRoleIdList(eq(Lists.newArrayList()), eq(false)))
          .thenReturn(Arrays.asList(menu1, menu2));

      // When
      List<MenuVO> result = roleMenuManager.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("正常情況：非管理員應返回對應角色的菜單")
    void shouldReturnMenusForRoles() {
      // Given
      List<Long> roleIdList = Arrays.asList(1L, 2L);
      Boolean administratorFlag = false;

      MenuEntity menu1 = createTestMenuEntity(1L, "菜單1");

      when(roleMenuDao.selectMenuListByRoleIdList(roleIdList, false))
          .thenReturn(Collections.singletonList(menu1));

      // When
      List<MenuVO> result = roleMenuManager.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("邊界情況：非管理員無角色時返回空列表")
    void shouldReturnEmptyWhenNoRoles() {
      // Given
      List<Long> roleIdList = Collections.emptyList();
      Boolean administratorFlag = false;

      // When
      List<MenuVO> result = roleMenuManager.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("邊界情況：非管理員角色列表為 null 時返回空列表")
    void shouldReturnEmptyWhenRolesNull() {
      // Given
      Boolean administratorFlag = false;

      // When
      List<MenuVO> result = roleMenuManager.getMenuList(null, administratorFlag);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private RoleMenuEntity createTestRoleMenuEntity(Long roleId, Long menuId) {
    RoleMenuEntity entity = new RoleMenuEntity();
    entity.setRoleId(roleId);
    entity.setMenuId(menuId);
    return entity;
  }

  private MenuEntity createTestMenuEntity(Long id, String name) {
    MenuEntity entity = new MenuEntity();
    entity.setMenuId(id);
    entity.setMenuName(name);
    entity.setDeletedFlag(false);
    return entity;
  }
}
