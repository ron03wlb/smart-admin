package net.lab1024.sa.admin.module.system.role;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleDataScopeUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleDataScopeVO;

/**
 * Test fixtures for RoleDataScope service tests
 *
 * <p>Provides reusable test data builders for RoleDataScope entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create data scope entity
 * RoleDataScopeEntity entity = RoleDataScopeTestFixture.createEntity(roleId, dataScopeType, viewType);
 *
 * // Create update form
 * RoleDataScopeUpdateForm form = RoleDataScopeTestFixture.createUpdateForm(roleId, itemList);
 *
 * // Create VO
 * RoleDataScopeVO vo = RoleDataScopeTestFixture.createVO(dataScopeType, viewType);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class RoleDataScopeTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create RoleDataScopeEntity with unique test data
   *
   * @param roleId Role ID
   * @param dataScopeType Data scope type (1=部门, 2=商品类目, etc.)
   * @param viewType View type (0=不可见, 1=可见, 2=仅可见自己)
   * @return Entity with all required fields set
   */
  public static RoleDataScopeEntity createEntity(
      Long roleId, Integer dataScopeType, Integer viewType) {
    RoleDataScopeEntity entity = new RoleDataScopeEntity();
    entity.setId((long) counter.incrementAndGet());
    entity.setRoleId(roleId);
    entity.setDataScopeType(dataScopeType);
    entity.setViewType(viewType);
    entity.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
    entity.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
    return entity;
  }

  /**
   * Create RoleDataScopeUpdateForm with test data
   *
   * @param roleId Role ID
   * @param dataScopeItems List of data scope items
   * @return Form ready for service.updateRoleDataScopeList()
   */
  public static RoleDataScopeUpdateForm createUpdateForm(
      Long roleId, List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> dataScopeItems) {
    RoleDataScopeUpdateForm form = new RoleDataScopeUpdateForm();
    form.setRoleId(roleId);
    form.setDataScopeItemList(dataScopeItems);
    return form;
  }

  /**
   * Create RoleUpdateDataScopeListFormItem with test data
   *
   * @param dataScopeType Data scope type
   * @param viewType View type
   * @return Form item ready for inclusion in update form
   */
  public static RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem createFormItem(
      Integer dataScopeType, Integer viewType) {
    RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem item =
        new RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem();
    item.setDataScopeType(dataScopeType);
    item.setViewType(viewType);
    return item;
  }

  /**
   * Create RoleDataScopeVO for query result testing
   *
   * @param dataScopeType Data scope type
   * @param viewType View type
   * @return VO with all fields set
   */
  public static RoleDataScopeVO createVO(Integer dataScopeType, Integer viewType) {
    RoleDataScopeVO vo = new RoleDataScopeVO();
    vo.setDataScopeType(dataScopeType);
    vo.setViewType(viewType);
    return vo;
  }

  /**
   * Create list of RoleDataScopeEntity for batch testing
   *
   * @param roleId Role ID
   * @param count Number of entities to create
   * @return List of entities with sequential data scope types
   */
  public static List<RoleDataScopeEntity> createEntityList(Long roleId, int count) {
    List<RoleDataScopeEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity(roleId, i + 1, 1)); // dataScopeType: 1,2,3..., viewType: 1 (可见)
    }
    return list;
  }

  /**
   * Create list of RoleUpdateDataScopeListFormItem for batch update testing
   *
   * @param count Number of form items to create
   * @return List of form items with sequential data scope types
   */
  public static List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> createFormItemList(
      int count) {
    List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createFormItem(i + 1, 1)); // dataScopeType: 1,2,3..., viewType: 1 (可见)
    }
    return list;
  }

  /**
   * Create list of RoleDataScopeVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs with sequential data scope types
   */
  public static List<RoleDataScopeVO> createVOList(int count) {
    List<RoleDataScopeVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createVO(i + 1, 1)); // dataScopeType: 1,2,3..., viewType: 1 (可见)
    }
    return list;
  }

  /**
   * Create typical department data scope entity (dataScopeType=1, viewType=1)
   *
   * @param roleId Role ID
   * @return Entity representing department data scope
   */
  public static RoleDataScopeEntity createDepartmentDataScopeEntity(Long roleId) {
    return createEntity(roleId, 1, 1); // 1=部门, 1=可见
  }

  /**
   * Create typical goods category data scope entity (dataScopeType=2, viewType=2)
   *
   * @param roleId Role ID
   * @return Entity representing goods category data scope
   */
  public static RoleDataScopeEntity createGoodsCategoryDataScopeEntity(Long roleId) {
    return createEntity(roleId, 2, 2); // 2=商品类目, 2=仅可见自己
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
