package net.lab1024.sa.system.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.system.dto.MenuDTO;
import net.lab1024.sa.system.menu.dao.MenuDao;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MenuContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 *   <li>角色菜單關聯查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class MenuContractAdapterTest {

  @Mock private MenuDao menuDao;

  @Mock private RoleMenuDao roleMenuDao;

  @InjectMocks private MenuContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long menuId = 1L;
    MenuEntity entity = new MenuEntity();
    entity.setMenuId(menuId);
    entity.setMenuName("系統管理");
    entity.setMenuType(1);
    entity.setPath("/system");

    when(menuDao.selectById(menuId)).thenReturn(entity);

    // When
    Option<MenuDTO> result = adapter.getById(menuId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getMenuId()).isEqualTo(menuId);
    assertThat(result.get().getMenuName()).isEqualTo("系統管理");
    assertThat(result.get().getMenuType()).isEqualTo(1);
    assertThat(result.get().getPath()).isEqualTo("/system");
    verify(menuDao).selectById(menuId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long menuId = 999L;
    when(menuDao.selectById(menuId)).thenReturn(null);

    // When
    Option<MenuDTO> result = adapter.getById(menuId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("menuId cannot be null");
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    MenuEntity entity1 = new MenuEntity();
    entity1.setMenuId(1L);
    entity1.setMenuName("系統管理");
    entity1.setMenuType(1);

    MenuEntity entity2 = new MenuEntity();
    entity2.setMenuId(2L);
    entity2.setMenuName("用戶管理");
    entity2.setMenuType(2);

    MenuEntity entity3 = new MenuEntity();
    entity3.setMenuId(3L);
    entity3.setMenuName("角色管理");
    entity3.setMenuType(2);

    List<MenuEntity> entities = Arrays.asList(entity1, entity2, entity3);
    when(menuDao.selectList(null)).thenReturn(entities);

    // When
    List<MenuDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getMenuId()).isEqualTo(1L);
    assertThat(result.get(0).getMenuName()).isEqualTo("系統管理");
    assertThat(result.get(1).getMenuId()).isEqualTo(2L);
    assertThat(result.get(2).getMenuId()).isEqualTo(3L);
    verify(menuDao).selectList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(menuDao.selectList(null)).thenReturn(Collections.emptyList());

    // When
    List<MenuDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  // ==================== queryByRoleId 測試 ====================

  @Test
  void testQueryByRoleId_Success() {
    // Given
    Long roleId = 1L;

    MenuEntity entity1 = new MenuEntity();
    entity1.setMenuId(1L);
    entity1.setMenuName("系統管理");

    MenuEntity entity2 = new MenuEntity();
    entity2.setMenuId(2L);
    entity2.setMenuName("用戶管理");

    List<MenuEntity> entities = Arrays.asList(entity1, entity2);
    when(roleMenuDao.selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE))
        .thenReturn(entities);

    // When
    List<MenuDTO> result = adapter.queryByRoleId(roleId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getMenuId()).isEqualTo(1L);
    assertThat(result.get(0).getMenuName()).isEqualTo("系統管理");
    assertThat(result.get(1).getMenuId()).isEqualTo(2L);
    verify(roleMenuDao).selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE);
  }

  @Test
  void testQueryByRoleId_EmptyResult() {
    // Given - 角色沒有分配任何菜單
    Long roleId = 999L;
    when(roleMenuDao.selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE))
        .thenReturn(Collections.emptyList());

    // When
    List<MenuDTO> result = adapter.queryByRoleId(roleId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryByRoleId_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByRoleId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roleId cannot be null");
  }

  @Test
  void testQueryByRoleId_ExcludeDeletedMenus() {
    // Given - 測試排除已刪除的菜單（通過參數 deletedFlag=FALSE）
    Long roleId = 1L;

    MenuEntity activeMenu = new MenuEntity();
    activeMenu.setMenuId(1L);
    activeMenu.setMenuName("系統管理");

    List<MenuEntity> entities = Collections.singletonList(activeMenu);
    when(roleMenuDao.selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE))
        .thenReturn(entities);

    // When
    List<MenuDTO> result = adapter.queryByRoleId(roleId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMenuName()).isEqualTo("系統管理");
    verify(roleMenuDao).selectMenuListByRoleIdList(List.of(roleId), Boolean.FALSE);
  }
}
