package net.lab1024.sa.admin.module.system.role.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.fixtures.RoleTestFixture;
import net.lab1024.sa.admin.module.system.menu.dao.MenuDao;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.admin.module.system.role.RoleMenuTestFixture;
import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleMenuTreeVO;
import net.lab1024.sa.admin.module.system.role.manager.RoleMenuManager;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleMenuService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>角色菜单权限更新（角色存在性校验、Manager事务调用）
 *   <li>获取菜单列表（管理员vs非管理员权限逻辑）
 *   <li>获取角色已选菜单（菜单树构建）
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleMenuService 单元测试")
class RoleMenuServiceTest {

  @Mock private RoleDao roleDao;

  @Mock private RoleMenuDao roleMenuDao;

  @Mock private RoleMenuManager roleMenuManager;

  @Mock private MenuDao menuDao;

  @InjectMocks private RoleMenuService roleMenuService;

  @BeforeEach
  void setUp() {
    RoleMenuTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("updateRoleMenu() - 更新角色菜单权限")
  class UpdateRoleMenuTests {

    @Test
    @DisplayName("正常更新角色菜单 - 应返回成功")
    void updateRoleMenu_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 1L;
      List<Long> menuIdList = Arrays.asList(1L, 2L, 3L);
      RoleMenuUpdateForm form = RoleMenuTestFixture.createUpdateForm(roleId, menuIdList);

      RoleEntity roleEntity = RoleTestFixture.createRole(roleId, "Test Role", "TEST_ROLE");

      when(roleDao.selectById(roleId)).thenReturn(roleEntity);
      doNothing().when(roleMenuManager).updateRoleMenuTransaction(eq(roleId), anyList());

      // Act
      ResponseDTO<String> response = roleMenuService.updateRoleMenu(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleDao, times(1)).selectById(roleId);
      verify(roleMenuManager, times(1)).updateRoleMenuTransaction(eq(roleId), anyList());
    }

    @Test
    @DisplayName("角色不存在 - 应返回错误")
    void updateRoleMenu_RoleNotFound_ShouldReturnError() {
      // Arrange
      Long roleId = 999L;
      List<Long> menuIdList = Arrays.asList(1L, 2L);
      RoleMenuUpdateForm form = RoleMenuTestFixture.createUpdateForm(roleId, menuIdList);

      when(roleDao.selectById(roleId)).thenReturn(null);

      // Act
      ResponseDTO<String> response = roleMenuService.updateRoleMenu(form);

      // Assert
      assertFalse(response.getOk());
      verify(roleDao, times(1)).selectById(roleId);
      verify(roleMenuManager, never()).updateRoleMenuTransaction(anyLong(), anyList());
    }

    @Test
    @DisplayName("空菜单列表 - 应返回成功")
    void updateRoleMenu_EmptyMenuList_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 1L;
      List<Long> emptyMenuList = Collections.emptyList();
      RoleMenuUpdateForm form = RoleMenuTestFixture.createUpdateForm(roleId, emptyMenuList);

      RoleEntity roleEntity = RoleTestFixture.createRole(roleId, "Test Role", "TEST_ROLE");

      when(roleDao.selectById(roleId)).thenReturn(roleEntity);
      doNothing().when(roleMenuManager).updateRoleMenuTransaction(eq(roleId), anyList());

      // Act
      ResponseDTO<String> response = roleMenuService.updateRoleMenu(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleMenuManager, times(1)).updateRoleMenuTransaction(eq(roleId), anyList());
    }
  }

  @Nested
  @DisplayName("getMenuList() - 获取菜单列表")
  class GetMenuListTests {

    @Test
    @DisplayName("管理员获取菜单 - 应返回所有菜单")
    void getMenuList_Administrator_ShouldReturnAllMenus() {
      // Arrange
      List<Long> roleIdList = Arrays.asList(1L);
      Boolean administratorFlag = true;

      List<MenuEntity> allMenus = RoleMenuTestFixture.createMenuEntityList(10, 0L);

      when(roleMenuDao.selectMenuListByRoleIdList(Lists.newArrayList(), false))
          .thenReturn(allMenus);

      // Act
      List<MenuVO> result = roleMenuService.getMenuList(roleIdList, administratorFlag);

      // Assert
      assertNotNull(result);
      assertEquals(10, result.size());
      verify(roleMenuDao, times(1)).selectMenuListByRoleIdList(Lists.newArrayList(), false);
    }

    @Test
    @DisplayName("非管理员无角色 - 应返回空列表")
    void getMenuList_NonAdminWithoutRoles_ShouldReturnEmptyList() {
      // Arrange
      List<Long> emptyRoleList = Collections.emptyList();
      Boolean administratorFlag = false;

      // Act
      List<MenuVO> result = roleMenuService.getMenuList(emptyRoleList, administratorFlag);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(roleMenuDao, never()).selectMenuListByRoleIdList(anyList(), anyBoolean());
    }

    @Test
    @DisplayName("非管理员有角色 - 应返回角色关联菜单")
    void getMenuList_NonAdminWithRoles_ShouldReturnRoleMenus() {
      // Arrange
      List<Long> roleIdList = Arrays.asList(2L, 3L);
      Boolean administratorFlag = false;

      List<MenuEntity> roleMenus = RoleMenuTestFixture.createMenuEntityList(5, 0L);

      when(roleMenuDao.selectMenuListByRoleIdList(roleIdList, false)).thenReturn(roleMenus);

      // Act
      List<MenuVO> result = roleMenuService.getMenuList(roleIdList, administratorFlag);

      // Assert
      assertNotNull(result);
      assertEquals(5, result.size());
      verify(roleMenuDao, times(1)).selectMenuListByRoleIdList(roleIdList, false);
    }

    @Test
    @DisplayName("角色列表为null - 应返回空列表")
    void getMenuList_RoleListNull_ShouldReturnEmptyList() {
      // Arrange
      List<Long> nullRoleList = null;
      Boolean administratorFlag = false;

      // Act
      List<MenuVO> result = roleMenuService.getMenuList(nullRoleList, administratorFlag);

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
      verify(roleMenuDao, never()).selectMenuListByRoleIdList(anyList(), anyBoolean());
    }
  }

  @Nested
  @DisplayName("getRoleSelectedMenu() - 获取角色已选菜单")
  class GetRoleSelectedMenuTests {

    @Test
    @DisplayName("正常获取角色已选菜单 - 应返回菜单树")
    void getRoleSelectedMenu_ValidRoleId_ShouldReturnMenuTree() {
      // Arrange
      Long roleId = 1L;
      List<Long> selectedMenuIds = Arrays.asList(1L, 11L, 12L);
      List<MenuVO> allMenus = RoleMenuTestFixture.createHierarchicalMenuVOList();

      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(selectedMenuIds);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null)).thenReturn(allMenus);

      // Act
      ResponseDTO<RoleMenuTreeVO> response = roleMenuService.getRoleSelectedMenu(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(roleId, response.getData().getRoleId());
      assertEquals(selectedMenuIds, response.getData().getSelectedMenuId());
      assertNotNull(response.getData().getMenuTreeList());

      verify(roleMenuDao, times(1)).queryMenuIdByRoleId(roleId);
      verify(menuDao, times(1)).queryMenuList(Boolean.FALSE, Boolean.FALSE, null);
    }

    @Test
    @DisplayName("角色无已选菜单 - 应返回空选择列表")
    void getRoleSelectedMenu_NoSelectedMenus_ShouldReturnEmptySelection() {
      // Arrange
      Long roleId = 2L;
      List<Long> emptySelectedMenuIds = Collections.emptyList();
      List<MenuVO> allMenus = RoleMenuTestFixture.createHierarchicalMenuVOList();

      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(emptySelectedMenuIds);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null)).thenReturn(allMenus);

      // Act
      ResponseDTO<RoleMenuTreeVO> response = roleMenuService.getRoleSelectedMenu(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(roleId, response.getData().getRoleId());
      assertTrue(response.getData().getSelectedMenuId().isEmpty());
      assertNotNull(response.getData().getMenuTreeList());
    }

    @Test
    @DisplayName("系统无菜单 - 应返回空菜单树")
    void getRoleSelectedMenu_NoMenusInSystem_ShouldReturnEmptyTree() {
      // Arrange
      Long roleId = 3L;
      List<Long> selectedMenuIds = Arrays.asList(1L);
      List<MenuVO> emptyMenuList = Collections.emptyList();

      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(selectedMenuIds);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null)).thenReturn(emptyMenuList);

      // Act
      ResponseDTO<RoleMenuTreeVO> response = roleMenuService.getRoleSelectedMenu(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(roleId, response.getData().getRoleId());
      assertTrue(response.getData().getMenuTreeList().isEmpty());
    }

    @Test
    @DisplayName("菜单树递归构建 - 应正确构建多层级结构")
    void getRoleSelectedMenu_MenuTree_ShouldBuildCorrectHierarchy() {
      // Arrange
      Long roleId = 1L;
      List<Long> selectedMenuIds = Arrays.asList(1L, 2L);
      List<MenuVO> hierarchicalMenus = RoleMenuTestFixture.createHierarchicalMenuVOList();

      when(roleMenuDao.queryMenuIdByRoleId(roleId)).thenReturn(selectedMenuIds);
      when(menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null)).thenReturn(hierarchicalMenus);

      // Act
      ResponseDTO<RoleMenuTreeVO> response = roleMenuService.getRoleSelectedMenu(roleId);

      // Assert
      assertTrue(response.getOk());
      RoleMenuTreeVO result = response.getData();

      // Verify root level menus (parentId = 0)
      assertNotNull(result.getMenuTreeList());
      assertTrue(result.getMenuTreeList().size() > 0);

      // Verify tree structure has children
      boolean hasChildren =
          result.getMenuTreeList().stream().anyMatch(menu -> !menu.getChildren().isEmpty());
      assertTrue(hasChildren, "Menu tree should have child menus");
    }
  }
}
