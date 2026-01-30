package net.lab1024.sa.admin.module.business.category;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryAddForm;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryTreeQueryForm;
import net.lab1024.sa.admin.module.business.category.domain.form.CategoryUpdateForm;
import net.lab1024.sa.admin.module.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.admin.module.business.category.domain.vo.CategoryVO;

/**
 * Test fixtures for Category service tests
 *
 * <p>Provides reusable test data builders for Category entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create root category entity
 * CategoryEntity root = CategoryTestFixture.createRootEntity(CategoryTypeEnum.GOODS.getValue());
 *
 * // Create child category entity
 * CategoryEntity child = CategoryTestFixture.createEntity(parentId, CategoryTypeEnum.GOODS.getValue());
 *
 * // Create add form
 * CategoryAddForm form = CategoryTestFixture.createAddForm(parentId, CategoryTypeEnum.GOODS.getValue());
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class CategoryTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create root CategoryEntity (parentId = 0) with unique test data
   *
   * @param categoryType Category type (1=GOODS, 2=CUSTOM)
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static CategoryEntity createRootEntity(Integer categoryType) {
    return createEntity(0L, categoryType);
  }

  /**
   * Create CategoryEntity with unique test data
   *
   * @param parentId Parent category ID (0 for root)
   * @param categoryType Category type (1=GOODS, 2=CUSTOM)
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static CategoryEntity createEntity(Long parentId, Integer categoryType) {
    int id = counter.incrementAndGet();

    CategoryEntity entity = new CategoryEntity();
    entity.setCategoryName("类目-" + id);
    entity.setCategoryType(categoryType);
    entity.setParentId(parentId);
    entity.setDisabledFlag(false);
    entity.setSort(id * 10);
    entity.setRemark("测试备注-" + id);
    entity.setDeletedFlag(false);

    // Auto-fields: categoryId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create CategoryEntity with specified categoryId
   *
   * @param categoryId Category ID
   * @param parentId Parent category ID
   * @param categoryType Category type
   * @return Entity with all required fields set
   */
  public static CategoryEntity createEntity(Long categoryId, Long parentId, Integer categoryType) {
    CategoryEntity entity = createEntity(parentId, categoryType);
    entity.setCategoryId(categoryId);
    entity.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
    entity.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
    return entity;
  }

  /**
   * Create CategoryAddForm with default test data
   *
   * @param parentId Parent category ID (null for root)
   * @param categoryType Category type (1=GOODS, 2=CUSTOM)
   * @return Form ready for service.add()
   */
  public static CategoryAddForm createAddForm(Long parentId, Integer categoryType) {
    int id = counter.incrementAndGet();

    CategoryAddForm form = new CategoryAddForm();
    form.setCategoryName("新建类目-" + id);
    form.setCategoryType(categoryType);
    form.setParentId(parentId);
    form.setSort(id * 10);
    form.setRemark("新建备注-" + id);
    form.setDisabledFlag(false);

    return form;
  }

  /**
   * Create CategoryUpdateForm for updating existing category
   *
   * @param categoryId ID of category to update (required)
   * @return Form ready for service.update()
   */
  public static CategoryUpdateForm createUpdateForm(Long categoryId) {
    int id = counter.incrementAndGet();

    CategoryUpdateForm form = new CategoryUpdateForm();
    form.setCategoryId(categoryId);
    form.setCategoryName("更新类目-" + id);
    form.setSort(id * 20);
    form.setRemark("更新备注-" + id);
    form.setDisabledFlag(false);

    return form;
  }

  /**
   * Create CategoryTreeQueryForm with pagination defaults
   *
   * @param parentId Parent category ID (null to query from root)
   * @param categoryType Category type (required if parentId is null)
   * @return Form ready for service.queryTree()
   */
  public static CategoryTreeQueryForm createTreeQueryForm(Long parentId, Integer categoryType) {
    CategoryTreeQueryForm form = new CategoryTreeQueryForm();
    form.setParentId(parentId);
    form.setCategoryType(categoryType);
    return form;
  }

  /**
   * Create CategoryVO for query result testing
   *
   * @param categoryId Category ID
   * @param parentId Parent category ID
   * @param categoryType Category type
   * @return VO with all fields set
   */
  public static CategoryVO createVO(Long categoryId, Long parentId, Integer categoryType) {
    int id = counter.incrementAndGet();

    CategoryVO vo = new CategoryVO();
    vo.setCategoryId(categoryId);
    vo.setCategoryName("类目VO-" + id);
    vo.setCategoryType(categoryType);
    vo.setParentId(parentId);
    vo.setDisabledFlag(false);
    vo.setSort(id * 10);
    vo.setRemark("VO备注-" + id);
    vo.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(id));
    vo.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));

    return vo;
  }

  /**
   * Create CategoryTreeVO for tree query result testing
   *
   * @param categoryId Category ID
   * @param parentId Parent category ID
   * @return TreeVO with all fields set
   */
  public static CategoryTreeVO createTreeVO(Long categoryId, Long parentId) {
    int id = counter.incrementAndGet();

    CategoryTreeVO vo = new CategoryTreeVO();
    vo.setCategoryId(categoryId);
    vo.setCategoryName("树节点-" + id);
    vo.setCategoryFullName("父级/树节点-" + id);
    vo.setParentId(parentId);
    vo.setValue(categoryId);
    vo.setLabel("树节点-" + id);
    vo.setChildren(new ArrayList<>());

    return vo;
  }

  /**
   * Create list of CategoryEntity for batch testing
   *
   * @param count Number of entities to create
   * @param parentId Parent category ID
   * @param categoryType Category type
   * @return List of entities
   */
  public static List<CategoryEntity> createEntityList(
      int count, Long parentId, Integer categoryType) {
    List<CategoryEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity(parentId, categoryType));
    }
    return list;
  }

  /**
   * Create list of CategoryTreeVO for tree query result testing
   *
   * @param count Number of tree nodes to create
   * @param parentId Parent category ID
   * @return List of tree VOs
   */
  public static List<CategoryTreeVO> createTreeVOList(int count, Long parentId) {
    List<CategoryTreeVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createTreeVO((long) (100 + i), parentId));
    }
    return list;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
