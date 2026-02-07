package net.lab1024.sa.system.menu.service;

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
import net.lab1024.sa.common.core.domain.RequestUrlVO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.menu.dao.MenuDao;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.menu.domain.form.MenuAddForm;
import net.lab1024.sa.system.menu.domain.form.MenuUpdateForm;
import net.lab1024.sa.system.menu.domain.vo.MenuTreeVO;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * MenuService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>菜單 CRUD 操作
 *   <li>菜單名稱/權限字符串校驗
 *   <li>菜單樹查詢
 *   <li>批量刪除及遞歸刪除
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MenuService 單元測試")
class MenuServiceTest {

  @Mock private MenuDao menuDao;

  @Mock private List<RequestUrlVO> authUrl;

  @InjectMocks private MenuService menuService;

  // ==================== addMenu 測試 ====================

  @Nested
  @DisplayName("addMenu 新增菜單測試")
  class AddMenuTest {

    @Test
    @DisplayName("正常情況：應該成功新增菜單")
    void shouldAddMenuSuccess() {
      // Given
      MenuAddForm addForm = createTestAddForm("用戶管理", 0L);
      when(menuDao.getByMenuName("用戶管理", 0L, Boolean.FALSE)).thenReturn(null);
      when(menuDao.getByWebPerms(null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.addMenu(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(menuDao).insert(any(MenuEntity.class));
    }

    @Test
    @DisplayName("異常情況：菜單名稱已存在時應返回錯誤")
    void shouldReturnErrorWhenNameExists() {
      // Given
      MenuAddForm addForm = createTestAddForm("用戶管理", 0L);
      MenuEntity existingMenu = createTestMenuEntity(1L, "用戶管理");

      when(menuDao.getByMenuName("用戶管理", 0L, Boolean.FALSE)).thenReturn(existingMenu);

      // When
      ResponseDTO<String> result = menuService.addMenu(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("菜单名称已存在");
      verify(menuDao, never()).insert(any(MenuEntity.class));
    }

    @Test
    @DisplayName("異常情況：權限字符串已存在時應返回錯誤")
    void shouldReturnErrorWhenWebPermsExists() {
      // Given
      MenuAddForm addForm = createTestAddForm("用戶管理", 0L);
      addForm.setWebPerms("user:view");

      MenuEntity existingMenu = createTestMenuEntity(1L, "其他菜單");

      when(menuDao.getByMenuName("用戶管理", 0L, Boolean.FALSE)).thenReturn(null);
      when(menuDao.getByWebPerms("user:view", Boolean.FALSE)).thenReturn(existingMenu);

      // When
      ResponseDTO<String> result = menuService.addMenu(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("前端权限字符串已存在");
    }
  }

  // ==================== updateMenu 測試 ====================

  @Nested
  @DisplayName("updateMenu 更新菜單測試")
  class UpdateMenuTest {

    @Test
    @DisplayName("正常情況：應該成功更新菜單")
    void shouldUpdateMenuSuccess() {
      // Given
      MenuUpdateForm updateForm = createTestUpdateForm(1L, "更新後菜單", 0L);

      MenuEntity existingMenu = createTestMenuEntity(1L, "原菜單");
      existingMenu.setDeletedFlag(false);

      when(menuDao.selectById(1L)).thenReturn(existingMenu);
      when(menuDao.getByMenuName("更新後菜單", 0L, Boolean.FALSE)).thenReturn(null);
      when(menuDao.getByWebPerms(null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.updateMenu(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(menuDao).updateById(any(MenuEntity.class));
    }

    @Test
    @DisplayName("異常情況：菜單不存在時應返回錯誤")
    void shouldReturnErrorWhenMenuNotFound() {
      // Given
      MenuUpdateForm updateForm = createTestUpdateForm(999L, "菜單", 0L);
      when(menuDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.updateMenu(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("菜单不存在");
    }

    @Test
    @DisplayName("異常情況：菜單已刪除時應返回錯誤")
    void shouldReturnErrorWhenMenuDeleted() {
      // Given
      MenuUpdateForm updateForm = createTestUpdateForm(1L, "菜單", 0L);

      MenuEntity deletedMenu = createTestMenuEntity(1L, "已刪除");
      deletedMenu.setDeletedFlag(true);

      when(menuDao.selectById(1L)).thenReturn(deletedMenu);

      // When
      ResponseDTO<String> result = menuService.updateMenu(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("菜单已被删除");
    }

    @Test
    @DisplayName("異常情況：上級菜單為自己時應返回錯誤")
    void shouldReturnErrorWhenParentIsSelf() {
      // Given
      MenuUpdateForm updateForm = createTestUpdateForm(1L, "菜單", 1L);

      MenuEntity existingMenu = createTestMenuEntity(1L, "菜單");
      existingMenu.setDeletedFlag(false);

      when(menuDao.selectById(1L)).thenReturn(existingMenu);
      when(menuDao.getByMenuName("菜單", 1L, Boolean.FALSE)).thenReturn(null);
      when(menuDao.getByWebPerms(null, Boolean.FALSE)).thenReturn(null);

      // When
      ResponseDTO<String> result = menuService.updateMenu(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("上级菜单不能为自己");
    }
  }

  // ==================== batchDeleteMenu 測試 ====================

  @Nested
  @DisplayName("batchDeleteMenu 批量刪除測試")
  class BatchDeleteMenuTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    void shouldBatchDeleteSuccess() {
      // Given
      List<Long> menuIdList = Arrays.asList(1L, 2L);
      Long employeeId = 100L;

      when(menuDao.selectMenuIdByParentIdList(menuIdList)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(menuIdList, employeeId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(menuDao).deleteByMenuIdList(menuIdList, employeeId, Boolean.TRUE);
    }

    @Test
    @DisplayName("異常情況：菜單列表為空時應返回錯誤")
    void shouldReturnErrorWhenListEmpty() {
      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(Collections.emptyList(), 100L);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("所选菜单不能为空");
    }

    @Test
    @DisplayName("正常情況：應該遞歸刪除子菜單")
    void shouldDeleteChildrenRecursively() {
      // Given
      List<Long> menuIdList = Collections.singletonList(1L);
      Long employeeId = 100L;
      List<Long> childrenIds = Arrays.asList(2L, 3L);

      when(menuDao.selectMenuIdByParentIdList(menuIdList)).thenReturn(childrenIds);
      when(menuDao.selectMenuIdByParentIdList(childrenIds)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> result = menuService.batchDeleteMenu(menuIdList, employeeId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(menuDao).deleteByMenuIdList(menuIdList, employeeId, Boolean.TRUE);
      verify(menuDao).deleteByMenuIdList(childrenIds, employeeId, Boolean.TRUE);
    }
  }

  // ==================== queryMenuList 測試 ====================

  @Nested
  @DisplayName("queryMenuList 查詢菜單列表測試")
  class QueryMenuListTest {

    @Test
    @DisplayName("正常情況：應該返回菜單列表")
    void shouldReturnMenuList() {
      // Given
      MenuVO menu1 = createTestMenuVO(1L, "菜單1", 0L);
      MenuVO menu2 = createTestMenuVO(2L, "菜單2", 0L);

      when(menuDao.queryMenuList(Boolean.FALSE, null, null))
          .thenReturn(Arrays.asList(menu1, menu2));

      // When
      List<MenuVO> result = menuService.queryMenuList(null);

      // Then
      assertThat(result).hasSize(2);
    }
  }

  // ==================== queryMenuTree 測試 ====================

  @Nested
  @DisplayName("queryMenuTree 查詢菜單樹測試")
  class QueryMenuTreeTest {

    @Test
    @DisplayName("正常情況：應該返回菜單樹結構")
    void shouldReturnMenuTree() {
      // Given
      MenuVO root = createTestMenuVO(1L, "Root", 0L);
      MenuVO child = createTestMenuVO(2L, "Child", 1L);

      when(menuDao.queryMenuList(eq(Boolean.FALSE), any(), anyList()))
          .thenReturn(Arrays.asList(root, child));

      // When
      ResponseDTO<List<MenuTreeVO>> result = menuService.queryMenuTree(true);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
      assertThat(result.getData().get(0).getChildren()).hasSize(1);
    }
  }

  // ==================== getMenuDetail 測試 ====================

  @Nested
  @DisplayName("getMenuDetail 查詢菜單詳情測試")
  class GetMenuDetailTest {

    @Test
    @DisplayName("正常情況：應該返回菜單詳情")
    void shouldReturnMenuDetail() {
      // Given
      Long menuId = 1L;
      MenuEntity entity = createTestMenuEntity(menuId, "菜單");
      entity.setDeletedFlag(false);

      when(menuDao.selectById(menuId)).thenReturn(entity);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(menuId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getMenuName()).isEqualTo("菜單");
    }

    @Test
    @DisplayName("異常情況：菜單不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      when(menuDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(999L);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("菜单不存在");
    }

    @Test
    @DisplayName("異常情況：菜單已刪除時應返回錯誤")
    void shouldReturnErrorWhenDeleted() {
      // Given
      MenuEntity deletedMenu = createTestMenuEntity(1L, "已刪除");
      deletedMenu.setDeletedFlag(true);

      when(menuDao.selectById(1L)).thenReturn(deletedMenu);

      // When
      ResponseDTO<MenuVO> result = menuService.getMenuDetail(1L);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("菜单已被删除");
    }
  }

  // ==================== getAuthUrl 測試 ====================

  @Nested
  @DisplayName("getAuthUrl 獲取系統請求路徑測試")
  class GetAuthUrlTest {

    @Test
    @DisplayName("正常情況：應該返回請求路徑列表")
    void shouldReturnAuthUrlList() {
      // When
      ResponseDTO<List<RequestUrlVO>> result = menuService.getAuthUrl();

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== Helper Methods ====================

  private MenuAddForm createTestAddForm(String name, Long parentId) {
    MenuAddForm form = new MenuAddForm();
    form.setMenuName(name);
    form.setParentId(parentId);
    return form;
  }

  private MenuUpdateForm createTestUpdateForm(Long id, String name, Long parentId) {
    MenuUpdateForm form = new MenuUpdateForm();
    form.setMenuId(id);
    form.setMenuName(name);
    form.setParentId(parentId);
    return form;
  }

  private MenuEntity createTestMenuEntity(Long id, String name) {
    MenuEntity entity = new MenuEntity();
    entity.setMenuId(id);
    entity.setMenuName(name);
    entity.setParentId(0L);
    entity.setDeletedFlag(false);
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
