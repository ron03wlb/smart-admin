package net.lab1024.sa.admin.module.business.goods.service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.admin.module.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsUpdateForm;

/**
 * Test fixtures for Goods integration tests
 *
 * <p>Provides reusable test data builders for Goods entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * GoodsEntity entity = GoodsTestFixture.createGoods();
 *
 * // Create entity with specific category
 * GoodsEntity entity = GoodsTestFixture.createGoods(categoryId);
 *
 * // Create add form
 * GoodsAddForm form = GoodsTestFixture.createAddForm(categoryId);
 *
 * // Create category for FK dependency
 * CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
 * categoryDao.insert(category);
 * }</pre>
 *
 * <p><b>Pattern Highlights:</b>
 *
 * <ul>
 *   <li>AtomicInteger counter ensures uniqueness across tests
 *   <li>Static factory methods (no constructors, no @Builder)
 *   <li>Parameter-based overrides (not Lombok builder)
 *   <li>BigDecimal uses String constructor for precision
 *   <li>Related entity factories for FK dependencies
 *   <li>resetCounter() for test isolation (optional with @Transactional)
 * </ul>
 *
 * @author Claude Code (test-fixture-generator skill)
 * @since 2026-01-25
 */
public class GoodsTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create GoodsEntity with unique test data
   *
   * <p>All required fields are populated with sensible defaults. Use parameter variants to override
   * FK fields.
   *
   * <p><b>Default Values:</b>
   *
   * <ul>
   *   <li>goodsName: "商品-{id}" (unique, readable)
   *   <li>goodsStatus: 2 (售卖中)
   *   <li>price: "99.99" + id (unique prices)
   *   <li>categoryId: 1L (override via createGoods(categoryId))
   *   <li>shelvesFlag: true (active)
   *   <li>deletedFlag: false (not deleted)
   * </ul>
   *
   * @return Entity with all required fields set (NOT persisted)
   */
  public static GoodsEntity createGoods() {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsName("商品-" + id);
    entity.setGoodsStatus(2); // 售卖中
    entity.setPrice(new BigDecimal("99.99").add(BigDecimal.valueOf(id))); // Unique: 100.99, 101.99
    entity.setPlace("测试产地-" + id);
    entity.setShelvesFlag(true);
    entity.setDeletedFlag(false);
    entity.setCategoryId(1L); // Default category
    entity.setRemark("测试备注-" + timestamp);

    return entity;
  }

  /**
   * Create GoodsEntity with specific category ID
   *
   * @param categoryId Category ID (required FK)
   * @return Entity with category set
   */
  public static GoodsEntity createGoods(Long categoryId) {
    GoodsEntity entity = createGoods();
    entity.setCategoryId(categoryId);
    return entity;
  }

  /**
   * Create GoodsEntity with custom status
   *
   * <p>Useful for testing different goods states:
   *
   * <ul>
   *   <li>1 = 预约中 (Pre-order)
   *   <li>2 = 售卖中 (On sale)
   *   <li>3 = 售罄 (Sold out)
   * </ul>
   *
   * @param categoryId Category ID
   * @param goodsStatus Goods status (1-3)
   * @return Entity with custom status
   */
  public static GoodsEntity createGoods(Long categoryId, Integer goodsStatus) {
    GoodsEntity entity = createGoods(categoryId);
    entity.setGoodsStatus(goodsStatus);
    return entity;
  }

  /**
   * Create GoodsAddForm with default test data
   *
   * <p>Ready for service.addGoods() call. Uses same defaults as createGoods().
   *
   * @param categoryId Category ID (required FK)
   * @return Form ready for service.addGoods()
   */
  public static GoodsAddForm createAddForm(Long categoryId) {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    GoodsAddForm form = new GoodsAddForm();
    form.setGoodsName("商品-" + id);
    form.setGoodsStatus(2);
    form.setPrice(new BigDecimal("99.99").add(BigDecimal.valueOf(id)));
    form.setPlace("测试产地-" + id);
    form.setShelvesFlag(true);
    form.setCategoryId(categoryId);
    form.setRemark("测试备注-" + timestamp);

    return form;
  }

  /**
   * Create GoodsUpdateForm for updating existing goods
   *
   * <p>Contains new values for update operation. Caller must set goodsId before calling
   * service.updateGoods().
   *
   * @param goodsId ID of goods to update
   * @param categoryId Category ID
   * @return Form ready for service.updateGoods()
   */
  public static GoodsUpdateForm createUpdateForm(Long goodsId, Long categoryId) {
    int id = counter.incrementAndGet();
    long timestamp = System.currentTimeMillis();

    GoodsUpdateForm form = new GoodsUpdateForm();
    form.setGoodsId(goodsId);
    form.setGoodsName("更新后商品-" + id);
    form.setGoodsStatus(2);
    form.setPrice(new BigDecimal("199.99"));
    form.setPlace("更新后产地-" + id);
    form.setShelvesFlag(true);
    form.setCategoryId(categoryId);
    form.setRemark("更新后备注-" + timestamp);

    return form;
  }

  /**
   * Create GoodsQueryForm with pagination defaults
   *
   * <p>Default pagination: pageNum=1, pageSize=10. Override via setters for custom pagination.
   *
   * @return Form ready for service.queryGoods()
   */
  public static GoodsQueryForm createQueryForm() {
    GoodsQueryForm form = new GoodsQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create CategoryEntity for FK dependency
   *
   * <p><b>Important:</b> This method returns an unpersisted entity. Caller must insert via Dao:
   *
   * <pre>{@code
   * CategoryEntity category = GoodsTestFixture.createCategory("测试分类");
   * categoryDao.insert(category); // ← Caller controls persistence
   * Long categoryId = category.getCategoryId();
   * }</pre>
   *
   * @param categoryName Category name
   * @return Category entity (NOT persisted)
   */
  public static CategoryEntity createCategory(String categoryName) {
    CategoryEntity category = new CategoryEntity();
    category.setCategoryName(categoryName);
    category.setCategoryType(1); // Default type
    category.setParentId(0L);
    category.setSort(1);
    category.setDisabledFlag(false);
    category.setDeletedFlag(false);
    return category;
  }

  /**
   * Reset counter for test isolation
   *
   * <p>Optional with {@code @Transactional} test class (auto-rollback handles isolation). Useful
   * when:
   *
   * <ul>
   *   <li>Running tests without transaction rollback
   *   <li>Need predictable counter values for assertions
   *   <li>Debugging counter-related issues
   * </ul>
   *
   * <pre>{@code
   * @BeforeEach
   * void setUp() {
   *     GoodsTestFixture.resetCounter(); // Optional
   * }
   * }</pre>
   */
  public static void resetCounter() {
    counter.set(0);
  }

  /**
   * Get current counter value (for debugging)
   *
   * <p><b>Internal use only.</b> Not recommended for production tests.
   *
   * @return Current counter value
   */
  static int getCurrentCounter() {
    return counter.get();
  }
}
