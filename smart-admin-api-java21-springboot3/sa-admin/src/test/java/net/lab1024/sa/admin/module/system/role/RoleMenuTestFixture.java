package net.lab1024.sa.admin.module.system.role;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.lab1024.sa.admin.module.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuSimpleTreeVO;
import net.lab1024.sa.admin.module.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleMenuEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleMenuTreeVO;

/**
 * Test fixtures for RoleMenu service tests
 *
 * <p>Provides reusable test data builders for RoleMenu entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create role-menu entity
 * RoleMenuEntity roleMenu = RoleMenuTestFixture.createRoleMenuEntity(roleId, menuId);
 *
 * // Create update form
 * RoleMenuUpdateForm form = RoleMenuTestFixture.createUpdateForm(roleId, Arrays.asList(1L, 2L, 3L));
 *
 * // Create menu VO
 * MenuVO menu = RoleMenuTestFixture.createMenuVO(menuId, parentId);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class RoleMenuTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create RoleMenuEntity with unique test data
   *
   * @param roleId Role ID
   * @param menuId Menu ID
   * @return Entity with all required fields set
   */
  public static RoleMenuEntity createRoleMenuEntity(Long roleId, Long menuId) {
    RoleMenuEntity entity = new RoleMenuEntity();
    entity.setRoleMenuId((long) counter.incrementAndGet());
    entity.setRoleId(roleId);
    entity.setMenuId(menuId);
    entity.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
    entity.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
    return entity;
  }

  /**
   * Create RoleMenuUpdateForm with test data
   *
   * @param roleId Role ID
   * @param menuIdList List of menu IDs
   * @return Form ready for service.updateRoleMenu()
   */
  public static RoleMenuUpdateForm createUpdateForm(Long roleId, List<Long> menuIdList) {
    RoleMenuUpdateForm form = new RoleMenuUpdateForm();
    form.setRoleId(roleId);
    form.setMenuIdList(menuIdList);
    return form;
  }

  /**
   * Create MenuEntity with unique test data
   *
   * @param menuId Menu ID
   * @param parentId Parent menu ID (0 for root)
   * @return Entity with all required fields set
   */
  public static MenuEntity createMenuEntity(Long menuId, Long parentId) {
    int id = counter.incrementAndGet();

    MenuEntity entity = new MenuEntity();
    entity.setMenuId(menuId);
    entity.setMenuName("菜单-" + id);
    entity.setMenuType(1); // 目录
    entity.setParentId(parentId);
    entity.setSort(id * 10);
    entity.setPermsType(1);
    entity.setVisibleFlag(true);
    entity.setDisabledFlag(false);
    entity.setDeletedFlag(false);
    entity.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
    entity.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));

    return entity;
  }

  /**
   * Create MenuVO for query result testing
   *
   * @param menuId Menu ID
   * @param parentId Parent menu ID
   * @return VO with all fields set
   */
  public static MenuVO createMenuVO(Long menuId, Long parentId) {
    int id = counter.incrementAndGet();

    MenuVO vo = new MenuVO();
    vo.setMenuId(menuId);
    vo.setMenuName("菜单VO-" + id);
    vo.setMenuType(1);
    vo.setParentId(parentId);
    vo.setSort(id * 10);
    vo.setPermsType(1);
    vo.setVisibleFlag(true);
    vo.setDisabledFlag(false);
    vo.setFrameFlag(false);
    vo.setCacheFlag(true);

    return vo;
  }

  /**
   * Create MenuSimpleTreeVO for tree result testing
   *
   * @param menuId Menu ID
   * @param parentId Parent menu ID
   * @return TreeVO with all fields set
   */
  public static MenuSimpleTreeVO createMenuSimpleTreeVO(Long menuId, Long parentId) {
    int id = counter.incrementAndGet();

    MenuSimpleTreeVO vo = new MenuSimpleTreeVO();
    vo.setMenuId(menuId);
    vo.setMenuName("树节点-" + id);
    vo.setMenuType(1);
    vo.setParentId(parentId);
    vo.setChildren(new ArrayList<>());

    return vo;
  }

  /**
   * Create RoleMenuTreeVO for getRoleSelectedMenu result testing
   *
   * @param roleId Role ID
   * @param selectedMenuIds List of selected menu IDs
   * @param menuTreeList Menu tree list
   * @return RoleMenuTreeVO with all fields set
   */
  public static RoleMenuTreeVO createRoleMenuTreeVO(
      Long roleId, List<Long> selectedMenuIds, List<MenuSimpleTreeVO> menuTreeList) {
    RoleMenuTreeVO vo = new RoleMenuTreeVO();
    vo.setRoleId(roleId);
    vo.setSelectedMenuId(selectedMenuIds);
    vo.setMenuTreeList(menuTreeList);
    return vo;
  }

  /**
   * Create list of RoleMenuEntity for batch testing
   *
   * @param roleId Role ID
   * @param menuIds List of menu IDs
   * @return List of role-menu entities
   */
  public static List<RoleMenuEntity> createRoleMenuEntityList(Long roleId, List<Long> menuIds) {
    return menuIds.stream()
        .map(menuId -> createRoleMenuEntity(roleId, menuId))
        .collect(Collectors.toList());
  }

  /**
   * Create list of MenuEntity for batch testing
   *
   * @param count Number of menu entities to create
   * @param parentId Parent menu ID
   * @return List of menu entities
   */
  public static List<MenuEntity> createMenuEntityList(int count, Long parentId) {
    List<MenuEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createMenuEntity((long) (100 + i), parentId));
    }
    return list;
  }

  /**
   * Create list of MenuVO for query result testing
   *
   * @param count Number of menu VOs to create
   * @param parentId Parent menu ID
   * @return List of menu VOs
   */
  public static List<MenuVO> createMenuVOList(int count, Long parentId) {
    List<MenuVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createMenuVO((long) (100 + i), parentId));
    }
    return list;
  }

  /**
   * Create hierarchical menu structure (root + children)
   *
   * @return List of MenuVO with root and child menus
   */
  public static List<MenuVO> createHierarchicalMenuVOList() {
    List<MenuVO> list = new ArrayList<>();

    // Root menus (parentId = 0)
    MenuVO root1 = createMenuVO(1L, 0L);
    root1.setMenuName("系统管理");
    list.add(root1);

    MenuVO root2 = createMenuVO(2L, 0L);
    root2.setMenuName("业务管理");
    list.add(root2);

    // Child menus under root1
    MenuVO child1 = createMenuVO(11L, 1L);
    child1.setMenuName("用户管理");
    list.add(child1);

    MenuVO child2 = createMenuVO(12L, 1L);
    child2.setMenuName("角色管理");
    list.add(child2);

    // Child menus under root2
    MenuVO child3 = createMenuVO(21L, 2L);
    child3.setMenuName("订单管理");
    list.add(child3);

    return list;
  }

  /**
   * Create list of selected menu IDs for testing
   *
   * @param menuIds Menu IDs
   * @return List of menu IDs
   */
  public static List<Long> createSelectedMenuIdList(Long... menuIds) {
    return Arrays.asList(menuIds);
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
