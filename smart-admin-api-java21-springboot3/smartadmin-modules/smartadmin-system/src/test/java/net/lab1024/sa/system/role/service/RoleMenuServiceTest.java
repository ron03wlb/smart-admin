package net.lab1024.sa.system.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.menu.dao.MenuDao;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.system.role.domain.form.RoleMenuUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleMenuTreeVO;
import net.lab1024.sa.system.role.manager.RoleMenuManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleMenuService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>角色菜單權限更新
 *   <li>角色菜單列表查詢
 *   <li>角色已選菜單樹查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleMenuService 單元測試")
class RoleMenuServiceTest {

  @Mock private RoleDao roleDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private RoleMenuManager roleMenuManager;

  @Mock private MenuDao menuDao;

  @InjectMocks private RoleMenuService roleMenuService;

  // ==================== updateRoleMenu 測試 ====================

  @Nested
  @DisplayName("updateRoleMenu 更新角色權限測試")
  class UpdateRoleMenuTest {

    @Test
    @DisplayName("正常情況：角色存在時應該成功更新")
    void shouldUpdateSuccessWhenRoleExists() {
      // Given
      RoleMenuUpdateForm updateForm = new RoleMenuUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setMenuIdList(Arrays.asList(10L, 20L, 30L));

      RoleEntity roleEntity = createTestRoleEntity(1L, "Admin");
      when(roleDao.selectById(1L)).thenReturn(roleEntity);

      // When
      ResponseDTO<String> result = roleMenuService.updateRoleMenu(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleMenuManager).updateRoleMenuTransaction(eq(1L), anyList());
    }

    @Test
    @DisplayName("異常情況：角色不存在時應該返回錯誤")
    void shouldReturnErrorWhenRoleNotExists() {
      // Given
      RoleMenuUpdateForm updateForm = new RoleMenuUpdateForm();
      updateForm.setRoleId(999L);
      updateForm.setMenuIdList(Arrays.asList(10L, 20L));

      when(roleDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleMenuService.updateRoleMenu(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleMenuManager, never()).updateRoleMenuTransaction(any(), any());
    }
  }

  // ==================== getMenuList 測試 ====================

  @Nested
  @DisplayName("getMenuList 獲取菜單列表測試")
  class GetMenuListTest {

    @Test
    @DisplayName("管理員：應該返回所有菜單")
    void shouldReturnAllMenusForAdmin() {
      // Given
      Boolean administratorFlag = true;
      List<Long> roleIdList = Collections.singletonList(1L);

      MenuEntity menu1 = createTestMenuEntity(1L, "System");
      MenuEntity menu2 = createTestMenuEntity(2L, "User");
      List<MenuEntity> allMenus = Arrays.asList(menu1, menu2);

      when(roleMenuDao.selectMenuListByRoleIdList(anyList(), eq(false))).thenReturn(allMenus);

      // When
      List<MenuVO> result = roleMenuService.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("非管理員無角色：應該返回空列表")
    void shouldReturnEmptyListForNonAdminWithoutRole() {
      // Given
      Boolean administratorFlag = false;
      List<Long> roleIdList = Collections.emptyList();

      // When
      List<MenuVO> result = roleMenuService.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).isEmpty();
      verify(roleMenuDao, never()).selectMenuListByRoleIdList(any(), any());
    }

    @Test
    @DisplayName("非管理員有角色：應該返回角色對應的菜單")
    void shouldReturnRoleMenusForNonAdmin() {
      // Given
      Boolean administratorFlag = false;
      List<Long> roleIdList = Arrays.asList(1L, 2L);

      MenuEntity menu = createTestMenuEntity(1L, "Dashboard");
      when(roleMenuDao.selectMenuListByRoleIdList(roleIdList, false))
          .thenReturn(Collections.singletonList(menu));

      // When
      List<MenuVO> result = roleMenuService.getMenuList(roleIdList, administratorFlag);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getMenuName()).isEqualTo("Dashboard");
    }
  }

  // ==================== getRoleSelectedMenu 測試 ====================

  @Nested
  @DisplayName("getRoleSelectedMenu 獲取角色已選菜單測試")
  class GetRoleSelectedMenuTest {

    @Test
    @DisplayName("正常情況：應該返回菜單樹和已選菜單 ID")
    void shouldReturnMenuTreeWithSelectedIds() {
      // Given
      Long roleId = 1L;

      List<Long> selectedMenuIds = Arrays.asList(10L, 20L);
      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(selectedMenuIds);

      MenuVO menu1 = createTestMenuVO(10L, "System", 0L);
      MenuVO menu2 = createTestMenuVO(20L, "User", 10L);
      List<MenuVO> menuList = Arrays.asList(menu1, menu2);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null)).thenReturn(menuList);

      // When
      ResponseDTO<RoleMenuTreeVO> result = roleMenuService.getRoleSelectedMenu(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getRoleId()).isEqualTo(roleId);
      assertThat(result.getData().getSelectedMenuId()).containsExactlyInAnyOrder(10L, 20L);
      assertThat(result.getData().getMenuTreeList()).isNotNull();
    }

    @Test
    @DisplayName("無已選菜單：應該返回空的已選列表")
    void shouldReturnEmptySelectedList() {
      // Given
      Long roleId = 1L;

      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(Collections.emptyList());

      MenuVO menu = createTestMenuVO(10L, "System", 0L);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null))
          .thenReturn(Collections.singletonList(menu));

      // When
      ResponseDTO<RoleMenuTreeVO> result = roleMenuService.getRoleSelectedMenu(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getSelectedMenuId()).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private RoleEntity createTestRoleEntity(Long id, String name) {
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(id);
    entity.setRoleName(name);
    return entity;
  }

  private MenuEntity createTestMenuEntity(Long id, String name) {
    MenuEntity entity = new MenuEntity();
    entity.setMenuId(id);
    entity.setMenuName(name);
    entity.setParentId(0L);
    return entity;
  }

  private MenuVO createTestMenuVO(Long id, String name, Long parentId) {
    MenuVO vo = new MenuVO();
    vo.setMenuId(id);
    vo.setMenuName(name);
    vo.setParentId(parentId);
    return vo;
  }
}
