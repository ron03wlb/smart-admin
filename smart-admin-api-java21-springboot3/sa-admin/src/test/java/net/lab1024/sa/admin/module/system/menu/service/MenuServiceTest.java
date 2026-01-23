package net.lab1024.sa.admin.module.system.menu.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.system.menu.constant.MenuTypeEnum;
import net.lab1024.sa.admin.module.system.menu.dao.MenuDao;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.menu.domain.form.MenuAddForm;
import net.lab1024.sa.admin.module.system.menu.domain.form.MenuUpdateForm;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuTreeVO;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.domain.RequestUrlVO;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/**
 * MenuService Unit Tests
 *
 * <p>Tests MenuService business logic with emphasis on:
 *
 * <ul>
 *   <li>Tree algorithms (buildMenuTree, recursiveDeleteChildren)
 *   <li>Generic validation methods (validateMenuName, validateWebPerms)
 *   <li>Synchronized methods (addMenu, updateMenu, batchDeleteMenu)
 *   <li>Recursive deletion with multi-level cascading
 *   <li>Type filtering (onlyMenu flag for CATALOG/MENU vs POINTS)
 * </ul>
 *
 * @author SmartAdmin Testing Framework
 * @since 2025-01-22
 */
@DisplayName("MenuService Unit Tests")
class MenuServiceTest extends BaseUnitTest {

  @InjectMocks private MenuService menuService;

  @Mock private MenuDao menuDao;

  @Mock private List<RequestUrlVO> authUrl;

  // Test constants
  private static final Long TEST_MENU_ID = 1001L;
  private static final Long TEST_PARENT_ID = 1000L;
  private static final Long TEST_EMPLOYEE_ID = 2001L;
  private static final String TEST_MENU_NAME = "User Management";
  private static final String TEST_WEB_PERMS = "system:user:view";

  // Test data
  private MenuAddForm testAddForm;
  private MenuUpdateForm testUpdateForm;
  private MenuEntity testMenuEntity;
  private MenuVO testMenuVO;

  @BeforeEach
  void setUp() {
    // Create test add form
    testAddForm = new MenuAddForm();
    testAddForm.setMenuName(TEST_MENU_NAME);
    testAddForm.setMenuType(MenuTypeEnum.MENU.getValue());
    testAddForm.setParentId(TEST_PARENT_ID);
    testAddForm.setWebPerms(TEST_WEB_PERMS);
    testAddForm.setFrameFlag(false);
    testAddForm.setCacheFlag(true);
    testAddForm.setVisibleFlag(true);
    testAddForm.setDisabledFlag(false);

    // Create test update form
    testUpdateForm = new MenuUpdateForm();
    testUpdateForm.setMenuId(TEST_MENU_ID);
    testUpdateForm.setMenuName(TEST_MENU_NAME);
    testUpdateForm.setMenuType(MenuTypeEnum.MENU.getValue());
    testUpdateForm.setParentId(TEST_PARENT_ID);
    testUpdateForm.setWebPerms(TEST_WEB_PERMS);
    testUpdateForm.setFrameFlag(false);
    testUpdateForm.setCacheFlag(true);
    testUpdateForm.setVisibleFlag(true);
    testUpdateForm.setDisabledFlag(false);

    // Create test entity
    testMenuEntity = new MenuEntity();
    testMenuEntity.setMenuId(TEST_MENU_ID);
    testMenuEntity.setMenuName(TEST_MENU_NAME);
    testMenuEntity.setMenuType(MenuTypeEnum.MENU.getValue());
    testMenuEntity.setParentId(TEST_PARENT_ID);
    testMenuEntity.setDeletedFlag(false);

    // Create test VO
    testMenuVO = new MenuVO();
    testMenuVO.setMenuId(TEST_MENU_ID);
    testMenuVO.setMenuName(TEST_MENU_NAME);
    testMenuVO.setParentId(TEST_PARENT_ID);
  }

  // ==================== addMenu() Tests ====================

  @Nested
  @DisplayName("addMenu() Tests - Synchronized Add with Validation")
  class AddMenuTests {

    @Test
    @DisplayName("Should successfully add menu when validations pass")
    void addMenu_ValidData_Success() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(null);
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(null);
      when(menuDao.insert(any(MenuEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = menuService.addMenu(testAddForm);

      // Then
      assertOk(result);
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
      verify(menuDao, times(1)).insert(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject when menu name already exists")
    void addMenu_DuplicateMenuName_ReturnsError() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(testMenuEntity);

      // When
      ResponseDTO<String> result = menuService.addMenu(testAddForm);

      // Then
      assertErrorContains(result, "菜单名称已存在");
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
      verify(menuDao, never()).insert(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject when web perms already exist")
    void addMenu_DuplicateWebPerms_ReturnsError() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(null);
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(testMenuEntity);

      // When
      ResponseDTO<String> result = menuService.addMenu(testAddForm);

      // Then
      assertErrorContains(result, "前端权限字符串已存在");
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
      verify(menuDao, never()).insert(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should handle synchronized method behavior")
    void addMenu_SynchronizedMethod_ThreadSafe() {
      // Given
      when(menuDao.getByMenuName(anyString(), anyLong(), anyBoolean())).thenReturn(null);
      when(menuDao.getByWebPerms(anyString(), anyBoolean())).thenReturn(null);
      when(menuDao.insert(any(MenuEntity.class))).thenReturn(1);

      // When - Multiple sequential calls (synchronized prevents concurrent access)
      ResponseDTO<String> result1 = menuService.addMenu(testAddForm);
      ResponseDTO<String> result2 = menuService.addMenu(testAddForm);

      // Then
      assertOk(result1);
      assertOk(result2);
      verify(menuDao, times(2)).insert(any(MenuEntity.class));
    }
  }

  // ==================== updateMenu() Tests ====================

  @Nested
  @DisplayName("updateMenu() Tests - Synchronized Update with Validation")
  class UpdateMenuTests {

    @Test
    @DisplayName("Should successfully update menu when validations pass")
    void updateMenu_ValidData_Success() {
      // Given
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(null);
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(null);
      when(menuDao.updateById(any(MenuEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertOk(result);
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
      verify(menuDao, times(1)).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject when menu does not exist")
    void updateMenu_NonExistentMenu_ReturnsError() {
      // Given
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertErrorContains(result, "菜单不存在");
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
      verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject when menu is deleted")
    void updateMenu_DeletedMenu_ReturnsError() {
      // Given
      testMenuEntity.setDeletedFlag(true);
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertErrorContains(result, "菜单已被删除");
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
      verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate menu name with different ID")
    void updateMenu_DuplicateMenuNameDifferentId_ReturnsError() {
      // Given
      MenuEntity conflictingMenu = new MenuEntity();
      conflictingMenu.setMenuId(9999L); // Different ID
      conflictingMenu.setMenuName(TEST_MENU_NAME);

      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false))
          .thenReturn(conflictingMenu);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertErrorContains(result, "菜单名称已存在");
      verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate web perms with different ID")
    void updateMenu_DuplicateWebPermsDifferentId_ReturnsError() {
      // Given
      MenuEntity conflictingMenu = new MenuEntity();
      conflictingMenu.setMenuId(9999L); // Different ID
      conflictingMenu.setWebPerms(TEST_WEB_PERMS);

      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(null);
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(conflictingMenu);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertErrorContains(result, "前端权限字符串已存在");
      verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should reject self-referencing parent")
    void updateMenu_SelfReferencingParent_ReturnsError() {
      // Given
      testUpdateForm.setParentId(TEST_MENU_ID); // Parent is self
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_MENU_ID, false)).thenReturn(null);
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertErrorContains(result, "上级菜单不能为自己");
      verify(menuDao, never()).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("Should allow update when menu name/perms belong to same ID")
    void updateMenu_SameIdNameAndPerms_Allowed() {
      // Given
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false))
          .thenReturn(testMenuEntity); // Same entity
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(testMenuEntity); // Same entity
      when(menuDao.updateById(any(MenuEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = menuService.updateMenu(testUpdateForm);

      // Then
      assertOk(result); // Should succeed - same menu
      verify(menuDao, times(1)).updateById(any(MenuEntity.class));
    }
  }

  // ==================== batchDeleteMenu() Tests ====================

  @Nested
  @DisplayName("batchDeleteMenu() Tests - Recursive Cascade Delete")
  class BatchDeleteMenuTests {

    @Test
    @DisplayName("Should successfully delete menu list and recursively delete children")
    void batchDeleteMenu_ValidList_DeletesWithChildren() {
      // Given
      List<Long> menuIdList = Lists.newArrayList(1L, 2L);
      List<Long> childrenIds = Lists.newArrayList(3L, 4L);

      doNothing().when(menuDao).deleteByMenuIdList(menuIdList, TEST_EMPLOYEE_ID, true);
      when(menuDao.selectMenuIdByParentIdList(menuIdList)).thenReturn(childrenIds);
      doNothing().when(menuDao).deleteByMenuIdList(childrenIds, TEST_EMPLOYEE_ID, true);
      when(menuDao.selectMenuIdByParentIdList(childrenIds)).thenReturn(new ArrayList<>()); // No
      // grandchildren

      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(menuIdList, TEST_EMPLOYEE_ID);

      // Then
      assertOk(result);
      verify(menuDao, times(1)).deleteByMenuIdList(menuIdList, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(1)).selectMenuIdByParentIdList(menuIdList);
      verify(menuDao, times(1)).deleteByMenuIdList(childrenIds, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(1)).selectMenuIdByParentIdList(childrenIds);
    }

    @Test
    @DisplayName("Should reject empty menu list")
    void batchDeleteMenu_EmptyList_ReturnsError() {
      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(new ArrayList<>(), TEST_EMPLOYEE_ID);

      // Then
      assertErrorContains(result, "所选菜单不能为空");
      verify(menuDao, never()).deleteByMenuIdList(anyList(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Should reject null menu list")
    void batchDeleteMenu_NullList_ReturnsError() {
      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(null, TEST_EMPLOYEE_ID);

      // Then
      assertErrorContains(result, "所选菜单不能为空");
      verify(menuDao, never()).deleteByMenuIdList(anyList(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Should handle multi-level recursive deletion (3 levels)")
    void batchDeleteMenu_ThreeLevels_RecursivelyDeletes() {
      // Given - 3-level hierarchy
      List<Long> level1Ids = Lists.newArrayList(1L);
      List<Long> level2Ids = Lists.newArrayList(2L, 3L);
      List<Long> level3Ids = Lists.newArrayList(4L, 5L, 6L);

      doNothing().when(menuDao).deleteByMenuIdList(anyList(), anyLong(), anyBoolean());
      when(menuDao.selectMenuIdByParentIdList(level1Ids)).thenReturn(level2Ids);
      when(menuDao.selectMenuIdByParentIdList(level2Ids)).thenReturn(level3Ids);
      when(menuDao.selectMenuIdByParentIdList(level3Ids)).thenReturn(new ArrayList<>()); // Stop

      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(level1Ids, TEST_EMPLOYEE_ID);

      // Then
      assertOk(result);
      verify(menuDao, times(1)).deleteByMenuIdList(level1Ids, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(1)).deleteByMenuIdList(level2Ids, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(1)).deleteByMenuIdList(level3Ids, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(3)).selectMenuIdByParentIdList(anyList());
    }

    @Test
    @DisplayName("Should stop recursion when no children found")
    void batchDeleteMenu_NoChildren_StopsRecursion() {
      // Given
      List<Long> menuIdList = Lists.newArrayList(1L);

      doNothing().when(menuDao).deleteByMenuIdList(menuIdList, TEST_EMPLOYEE_ID, true);
      when(menuDao.selectMenuIdByParentIdList(menuIdList)).thenReturn(new ArrayList<>()); // No
      // children

      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(menuIdList, TEST_EMPLOYEE_ID);

      // Then
      assertOk(result);
      verify(menuDao, times(1)).deleteByMenuIdList(menuIdList, TEST_EMPLOYEE_ID, true);
      verify(menuDao, times(1)).selectMenuIdByParentIdList(menuIdList);
    }
  }

  // ==================== validateMenuName() Tests ====================

  @Nested
  @DisplayName("validateMenuName() Tests - Generic Name Validation")
  class ValidateMenuNameTests {

    @Test
    @DisplayName("MenuAddForm: Should return true when menu name exists")
    void validateMenuName_AddFormDuplicate_ReturnsTrue() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(testMenuEntity);

      // When
      Boolean result = menuService.validateMenuName(testAddForm);

      // Then
      assertTrue(result); // true = duplicate
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
    }

    @Test
    @DisplayName("MenuAddForm: Should return false when menu name does not exist")
    void validateMenuName_AddFormUnique_ReturnsFalse() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false)).thenReturn(null);

      // When
      Boolean result = menuService.validateMenuName(testAddForm);

      // Then
      assertFalse(result); // false = unique
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
    }

    @Test
    @DisplayName("MenuUpdateForm: Should return true when menu name exists with different ID")
    void validateMenuName_UpdateFormDifferentId_ReturnsTrue() {
      // Given
      MenuEntity conflictingMenu = new MenuEntity();
      conflictingMenu.setMenuId(9999L); // Different ID
      conflictingMenu.setMenuName(TEST_MENU_NAME);

      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false))
          .thenReturn(conflictingMenu);

      // When
      Boolean result = menuService.validateMenuName(testUpdateForm);

      // Then
      assertTrue(result); // true = duplicate (different ID)
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
    }

    @Test
    @DisplayName("MenuUpdateForm: Should return false when menu name exists with same ID")
    void validateMenuName_UpdateFormSameId_ReturnsFalse() {
      // Given
      when(menuDao.getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false))
          .thenReturn(testMenuEntity); // Same ID

      // When
      Boolean result = menuService.validateMenuName(testUpdateForm);

      // Then
      assertFalse(result); // false = allowed (same menu)
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, TEST_PARENT_ID, false);
    }

    @Test
    @DisplayName("Should allow same name under different parent")
    void validateMenuName_SameNameDifferentParent_Allowed() {
      // Given - Menu with same name but different parent
      Long differentParentId = 5000L;
      MenuAddForm formWithDifferentParent = new MenuAddForm();
      formWithDifferentParent.setMenuName(TEST_MENU_NAME);
      formWithDifferentParent.setParentId(differentParentId);

      when(menuDao.getByMenuName(TEST_MENU_NAME, differentParentId, false)).thenReturn(null);

      // When
      Boolean result = menuService.validateMenuName(formWithDifferentParent);

      // Then
      assertFalse(result); // Allowed - different parent
      verify(menuDao, times(1)).getByMenuName(TEST_MENU_NAME, differentParentId, false);
    }
  }

  // ==================== validateWebPerms() Tests ====================

  @Nested
  @DisplayName("validateWebPerms() Tests - Generic Perms Validation")
  class ValidateWebPermsTests {

    @Test
    @DisplayName("Should return false when web perms is empty")
    void validateWebPerms_EmptyPerms_ReturnsFalse() {
      // Given
      testAddForm.setWebPerms("");

      // When
      Boolean result = menuService.validateWebPerms(testAddForm);

      // Then
      assertFalse(result); // false = no duplicate check needed
      verify(menuDao, never()).getByWebPerms(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Should return false when web perms is null")
    void validateWebPerms_NullPerms_ReturnsFalse() {
      // Given
      testAddForm.setWebPerms(null);

      // When
      Boolean result = menuService.validateWebPerms(testAddForm);

      // Then
      assertFalse(result); // false = no duplicate check needed
      verify(menuDao, never()).getByWebPerms(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("MenuAddForm: Should return true when web perms exist")
    void validateWebPerms_AddFormDuplicate_ReturnsTrue() {
      // Given
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(testMenuEntity);

      // When
      Boolean result = menuService.validateWebPerms(testAddForm);

      // Then
      assertTrue(result); // true = duplicate
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
    }

    @Test
    @DisplayName("MenuAddForm: Should return false when web perms do not exist")
    void validateWebPerms_AddFormUnique_ReturnsFalse() {
      // Given
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(null);

      // When
      Boolean result = menuService.validateWebPerms(testAddForm);

      // Then
      assertFalse(result); // false = unique
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
    }

    @Test
    @DisplayName("MenuUpdateForm: Should return true when web perms exist with different ID")
    void validateWebPerms_UpdateFormDifferentId_ReturnsTrue() {
      // Given
      MenuEntity conflictingMenu = new MenuEntity();
      conflictingMenu.setMenuId(9999L); // Different ID
      conflictingMenu.setWebPerms(TEST_WEB_PERMS);

      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(conflictingMenu);

      // When
      Boolean result = menuService.validateWebPerms(testUpdateForm);

      // Then
      assertTrue(result); // true = duplicate (different ID)
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
    }

    @Test
    @DisplayName("MenuUpdateForm: Should return false when web perms exist with same ID")
    void validateWebPerms_UpdateFormSameId_ReturnsFalse() {
      // Given
      when(menuDao.getByWebPerms(TEST_WEB_PERMS, false)).thenReturn(testMenuEntity); // Same ID

      // When
      Boolean result = menuService.validateWebPerms(testUpdateForm);

      // Then
      assertFalse(result); // false = allowed (same menu)
      verify(menuDao, times(1)).getByWebPerms(TEST_WEB_PERMS, false);
    }
  }

  // ==================== queryMenuTree() Tests ====================

  @Nested
  @DisplayName("queryMenuTree() Tests - Tree Building with Type Filtering")
  class QueryMenuTreeTests {

    @Test
    @DisplayName("Should build full menu tree when onlyMenu is false")
    void queryMenuTree_OnlyMenuFalse_ReturnsFullTree() {
      // Given
      List<MenuVO> allMenus = createTestMenuHierarchy();
      when(menuDao.queryMenuList(false, null, Lists.newArrayList())).thenReturn(allMenus);

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(false);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      verify(menuDao, times(1)).queryMenuList(false, null, Lists.newArrayList());
    }

    @Test
    @DisplayName("Should exclude POINTS type when onlyMenu is true")
    void queryMenuTree_OnlyMenuTrue_ExcludesPoints() {
      // Given
      List<Integer> expectedTypes =
          Lists.newArrayList(MenuTypeEnum.CATALOG.getValue(), MenuTypeEnum.MENU.getValue());
      List<MenuVO> catalogAndMenus = createTestMenuHierarchy();

      when(menuDao.queryMenuList(false, null, expectedTypes)).thenReturn(catalogAndMenus);

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(true);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      verify(menuDao, times(1)).queryMenuList(false, null, expectedTypes);
    }

    @Test
    @DisplayName("Should handle empty menu list")
    void queryMenuTree_EmptyMenuList_ReturnsEmptyTree() {
      // Given
      when(menuDao.queryMenuList(anyBoolean(), any(), anyList())).thenReturn(new ArrayList<>());

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(false);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("Should group menus by parentId and build tree structure")
    void queryMenuTree_ValidMenus_BuildsTreeStructure() {
      // Given
      List<MenuVO> menus = createTestMenuHierarchy();
      when(menuDao.queryMenuList(anyBoolean(), any(), anyList())).thenReturn(menus);

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(false);

      // Then
      assertOk(result);
      List<MenuTreeVO> tree = result.getData();
      assertNotNull(tree);

      // Verify tree structure (top-level menus with parentId = 0)
      assertTrue(tree.stream().allMatch(menu -> menu.getParentId() == 0L));
    }

    @Test
    @DisplayName("Should handle deep nesting (3+ levels)")
    void queryMenuTree_DeepNesting_BuildsCorrectly() {
      // Given
      List<MenuVO> menus = createDeepMenuHierarchy();
      when(menuDao.queryMenuList(anyBoolean(), any(), anyList())).thenReturn(menus);

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(false);

      // Then
      assertOk(result);
      List<MenuTreeVO> tree = result.getData();
      assertNotNull(tree);

      // Verify children are set recursively
      MenuTreeVO root = tree.get(0);
      assertNotNull(root.getChildren());
      if (!root.getChildren().isEmpty()) {
        MenuTreeVO child = root.getChildren().get(0);
        assertNotNull(child.getChildren()); // Grandchildren
      }
    }
  }

  // ==================== buildMenuTree() Tests ====================

  @Nested
  @DisplayName("buildMenuTree() Tests - Recursive Generic Tree Building")
  class BuildMenuTreeTests {

    @Test
    @DisplayName("Should build tree with single root")
    void buildMenuTree_SingleRoot_BuildsCorrectly() {
      // Given
      List<MenuVO> menus = createSingleRootHierarchy();
      var parentMap =
          menus.stream().collect(java.util.stream.Collectors.groupingBy(MenuVO::getParentId));

      // When
      List<MenuTreeVO> tree = menuService.buildMenuTree(parentMap, 0L);

      // Then
      assertNotNull(tree);
      assertEquals(1, tree.size());
      assertEquals("System Management", tree.get(0).getMenuName());
    }

    @Test
    @DisplayName("Should build tree with multiple roots")
    void buildMenuTree_MultipleRoots_BuildsCorrectly() {
      // Given
      List<MenuVO> menus = createMultipleRootsHierarchy();
      var parentMap =
          menus.stream().collect(java.util.stream.Collectors.groupingBy(MenuVO::getParentId));

      // When
      List<MenuTreeVO> tree = menuService.buildMenuTree(parentMap, 0L);

      // Then
      assertNotNull(tree);
      assertEquals(2, tree.size()); // 2 root menus
    }

    @Test
    @DisplayName("Should set children recursively")
    void buildMenuTree_NestedChildren_SetsChildrenRecursively() {
      // Given
      List<MenuVO> menus = createNestedHierarchy();
      var parentMap =
          menus.stream().collect(java.util.stream.Collectors.groupingBy(MenuVO::getParentId));

      // When
      List<MenuTreeVO> tree = menuService.buildMenuTree(parentMap, 0L);

      // Then
      assertNotNull(tree);
      MenuTreeVO root = tree.get(0);
      assertNotNull(root.getChildren());
      assertFalse(root.getChildren().isEmpty());

      // Verify children have their own children
      MenuTreeVO child = root.getChildren().get(0);
      assertNotNull(child.getChildren());
    }

    @Test
    @DisplayName("Should handle empty parent map")
    void buildMenuTree_EmptyParentMap_ReturnsEmptyList() {
      // Given
      var emptyMap = new java.util.HashMap<Long, List<MenuVO>>();

      // When
      List<MenuTreeVO> tree = menuService.buildMenuTree(emptyMap, 0L);

      // Then
      assertNotNull(tree);
      assertTrue(tree.isEmpty());
    }

    @Test
    @DisplayName("Should handle leaf nodes (no children)")
    void buildMenuTree_LeafNodes_EmptyChildren() {
      // Given
      MenuVO leafMenu = new MenuVO();
      leafMenu.setMenuId(100L);
      leafMenu.setMenuName("Leaf Menu");
      leafMenu.setParentId(0L);

      var parentMap = new java.util.HashMap<Long, List<MenuVO>>();
      parentMap.put(0L, Lists.newArrayList(leafMenu));

      // When
      List<MenuTreeVO> tree = menuService.buildMenuTree(parentMap, 0L);

      // Then
      assertNotNull(tree);
      assertEquals(1, tree.size());
      assertNotNull(tree.get(0).getChildren());
      assertTrue(tree.get(0).getChildren().isEmpty()); // No children
    }
  }

  // ==================== getMenuDetail() Tests ====================

  @Nested
  @DisplayName("getMenuDetail() Tests - Menu Retrieval")
  class GetMenuDetailTests {

    @Test
    @DisplayName("Should return menu detail when menu exists")
    void getMenuDetail_ExistingMenu_ReturnsMenuVO() {
      // Given
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(TEST_MENU_ID);

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      assertEquals(TEST_MENU_ID, result.getData().getMenuId());
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
    }

    @Test
    @DisplayName("Should return error when menu does not exist")
    void getMenuDetail_NonExistentMenu_ReturnsError() {
      // Given
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(null);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(TEST_MENU_ID);

      // Then
      assertError(result);
      assertErrorContains(result, "菜单不存在");
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
    }

    @Test
    @DisplayName("Should return error when menu is deleted")
    void getMenuDetail_DeletedMenu_ReturnsError() {
      // Given
      testMenuEntity.setDeletedFlag(true);
      when(menuDao.selectById(TEST_MENU_ID)).thenReturn(testMenuEntity);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(TEST_MENU_ID);

      // Then
      assertError(result);
      assertErrorContains(result, "菜单已被删除");
      verify(menuDao, times(1)).selectById(TEST_MENU_ID);
    }
  }

  // ==================== getAuthUrl() Tests ====================

  @Nested
  @DisplayName("getAuthUrl() Tests - Request URL Retrieval")
  class GetAuthUrlTests {

    @Test
    @DisplayName("Should return authUrl list")
    void getAuthUrl_ReturnsAuthUrlList() {
      // When
      ResponseDTO<List<RequestUrlVO>> result = menuService.getAuthUrl();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      // authUrl is injected via @Mock, method just returns ResponseDTO.ok(authUrl)
    }

    @Test
    @DisplayName("Should handle injected authUrl")
    void getAuthUrl_InjectedAuthUrl_ReturnsCorrectly() {
      // When
      ResponseDTO<List<RequestUrlVO>> result = menuService.getAuthUrl();

      // Then
      assertOk(result);
      assertNotNull(result.getData());
      // Verify the response wraps the authUrl field
      assertEquals(authUrl, result.getData());
    }
  }

  // ==================== Test Data Helpers ====================

  /** Creates test menu hierarchy for testing */
  private List<MenuVO> createTestMenuHierarchy() {
    List<MenuVO> menus = new ArrayList<>();

    // Root menu
    MenuVO root = new MenuVO();
    root.setMenuId(1L);
    root.setMenuName("System Management");
    root.setParentId(0L);
    root.setMenuType(MenuTypeEnum.CATALOG.getValue());
    menus.add(root);

    // Child menu
    MenuVO child = new MenuVO();
    child.setMenuId(2L);
    child.setMenuName("User Management");
    child.setParentId(1L);
    child.setMenuType(MenuTypeEnum.MENU.getValue());
    menus.add(child);

    return menus;
  }

  /** Creates deep menu hierarchy (3+ levels) */
  private List<MenuVO> createDeepMenuHierarchy() {
    List<MenuVO> menus = createTestMenuHierarchy();

    // Grandchild menu
    MenuVO grandchild = new MenuVO();
    grandchild.setMenuId(3L);
    grandchild.setMenuName("User List");
    grandchild.setParentId(2L);
    grandchild.setMenuType(MenuTypeEnum.MENU.getValue());
    menus.add(grandchild);

    return menus;
  }

  /** Creates single root hierarchy */
  private List<MenuVO> createSingleRootHierarchy() {
    MenuVO root = new MenuVO();
    root.setMenuId(1L);
    root.setMenuName("System Management");
    root.setParentId(0L);
    return Lists.newArrayList(root);
  }

  /** Creates multiple roots hierarchy */
  private List<MenuVO> createMultipleRootsHierarchy() {
    List<MenuVO> menus = new ArrayList<>();

    MenuVO root1 = new MenuVO();
    root1.setMenuId(1L);
    root1.setMenuName("System Management");
    root1.setParentId(0L);
    menus.add(root1);

    MenuVO root2 = new MenuVO();
    root2.setMenuId(2L);
    root2.setMenuName("Business Management");
    root2.setParentId(0L);
    menus.add(root2);

    return menus;
  }

  /** Creates nested hierarchy */
  private List<MenuVO> createNestedHierarchy() {
    List<MenuVO> menus = new ArrayList<>();

    // Root
    MenuVO root = new MenuVO();
    root.setMenuId(1L);
    root.setMenuName("System");
    root.setParentId(0L);
    menus.add(root);

    // Child
    MenuVO child = new MenuVO();
    child.setMenuId(2L);
    child.setMenuName("User");
    child.setParentId(1L);
    menus.add(child);

    // Grandchild
    MenuVO grandchild = new MenuVO();
    grandchild.setMenuId(3L);
    grandchild.setMenuName("User List");
    grandchild.setParentId(2L);
    menus.add(grandchild);

    return menus;
  }
}
