package net.lab1024.sa.admin.module.business.goods;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.goods.constant.GoodsStatusEnum;
import net.lab1024.sa.admin.module.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsImportForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.admin.module.business.goods.domain.vo.GoodsExcelVO;
import net.lab1024.sa.admin.module.business.goods.domain.vo.GoodsVO;

/**
 * Test fixtures for Goods service tests
 *
 * <p>Provides reusable test data builders for Goods entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * GoodsEntity entity = GoodsTestFixture.createEntity();
 *
 * // Create add form
 * GoodsAddForm form = GoodsTestFixture.createAddForm();
 *
 * // Create entity with custom categoryId
 * GoodsEntity entity = GoodsTestFixture.createEntity(100L);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class GoodsTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create GoodsEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static GoodsEntity createEntity() {
    return createEntity(1L);
  }

  /**
   * Create GoodsEntity with specified categoryId
   *
   * @param categoryId Category ID
   * @return Entity with all required fields set
   */
  public static GoodsEntity createEntity(Long categoryId) {
    int id = counter.incrementAndGet();

    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsStatus(GoodsStatusEnum.SELL.getValue()); // 2=售卖中
    entity.setCategoryId(categoryId);
    entity.setGoodsName("商品-" + id);
    entity.setPlace("产地-" + id);
    entity.setPrice(new BigDecimal("99.99").add(new BigDecimal(id)));
    entity.setShelvesFlag(true);
    entity.setDeletedFlag(false);
    entity.setRemark("测试商品备注 " + id);

    // Auto-fields: goodsId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create GoodsAddForm with default test data
   *
   * @return Form ready for service.add()
   */
  public static GoodsAddForm createAddForm() {
    return createAddForm(1L);
  }

  /**
   * Create GoodsAddForm with specified categoryId
   *
   * @param categoryId Category ID
   * @return Form ready for service.add()
   */
  public static GoodsAddForm createAddForm(Long categoryId) {
    int id = counter.incrementAndGet();

    GoodsAddForm form = new GoodsAddForm();
    form.setCategoryId(categoryId);
    form.setGoodsName("新商品-" + id);
    form.setGoodsStatus(GoodsStatusEnum.SELL.getValue());
    form.setPlace("中国");
    form.setPrice(new BigDecimal("199.99"));
    form.setShelvesFlag(true);
    form.setRemark("添加测试商品 " + id);

    return form;
  }

  /**
   * Create GoodsUpdateForm for updating existing goods
   *
   * @param goodsId ID of goods to update (required)
   * @return Form ready for service.update()
   */
  public static GoodsUpdateForm createUpdateForm(Long goodsId) {
    return createUpdateForm(goodsId, 1L);
  }

  /**
   * Create GoodsUpdateForm with specified IDs
   *
   * @param goodsId ID of goods to update
   * @param categoryId Category ID
   * @return Form ready for service.update()
   */
  public static GoodsUpdateForm createUpdateForm(Long goodsId, Long categoryId) {
    int id = counter.incrementAndGet();

    GoodsUpdateForm form = new GoodsUpdateForm();
    form.setGoodsId(goodsId);
    form.setCategoryId(categoryId);
    form.setGoodsName("更新商品-" + id);
    form.setGoodsStatus(GoodsStatusEnum.SELL_OUT.getValue()); // 3=售罄
    form.setPlace("美国");
    form.setPrice(new BigDecimal("299.99"));
    form.setShelvesFlag(false);
    form.setRemark("更新测试商品 " + id);

    return form;
  }

  /**
   * Create GoodsQueryForm with pagination defaults
   *
   * @return Form ready for service.query()
   */
  public static GoodsQueryForm createQueryForm() {
    GoodsQueryForm form = new GoodsQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create GoodsImportForm for import testing
   *
   * @return Form ready for Excel import
   */
  public static GoodsImportForm createImportForm() {
    return createImportForm(1L);
  }

  /**
   * Create GoodsImportForm with specified category name
   *
   * @param categoryId Category ID (converted to category name)
   * @return Form ready for Excel import
   */
  public static GoodsImportForm createImportForm(Long categoryId) {
    int id = counter.incrementAndGet();

    GoodsImportForm form = new GoodsImportForm();
    form.setCategoryName("分类-" + categoryId);
    form.setGoodsName("导入商品-" + id);
    form.setGoodsStatus(GoodsStatusEnum.APPOINTMENT.getDesc()); // "预约中"
    form.setPlace("日本");
    form.setPrice(new BigDecimal("149.99"));

    return form;
  }

  /**
   * Create GoodsVO for query result testing
   *
   * @return VO with all fields set
   */
  public static GoodsVO createVO() {
    return createVO(100L);
  }

  /**
   * Create GoodsVO with specified goodsId
   *
   * @param goodsId Goods ID
   * @return VO with all fields set
   */
  public static GoodsVO createVO(Long goodsId) {
    int id = counter.incrementAndGet();

    GoodsVO vo = new GoodsVO();
    vo.setGoodsId(goodsId);
    vo.setGoodsStatus(GoodsStatusEnum.SELL.getValue());
    vo.setCategoryId(1L);
    vo.setCategoryName("分类-" + id);
    vo.setGoodsName("商品VO-" + id);
    vo.setPlace("产地-" + id);
    vo.setPrice(new BigDecimal("99.99"));
    vo.setShelvesFlag(true);
    vo.setRemark("VO备注 " + id);
    vo.setCreateTime(LocalDateTime.now().minusDays(id));
    vo.setUpdateTime(LocalDateTime.now());

    return vo;
  }

  /**
   * Create GoodsExcelVO for export testing
   *
   * @return ExcelVO with all fields set
   */
  public static GoodsExcelVO createExcelVO() {
    return createExcelVO(100L);
  }

  /**
   * Create GoodsExcelVO for Excel export
   *
   * @param goodsId Goods ID (for identification, not included in Excel)
   * @return ExcelVO with all fields set
   */
  public static GoodsExcelVO createExcelVO(Long goodsId) {
    int id = counter.incrementAndGet();

    GoodsExcelVO vo = new GoodsExcelVO();
    vo.setGoodsStatus(GoodsStatusEnum.SELL.getDesc()); // "售卖中"
    vo.setCategoryName("分类-" + id);
    vo.setGoodsName("导出商品-" + id);
    vo.setPlace("产地-" + id);
    vo.setPrice(new BigDecimal("99.99"));

    return vo;
  }

  /**
   * Create list of GoodsEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<GoodsEntity> createEntityList(int count) {
    List<GoodsEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of GoodsVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<GoodsVO> createVOList(int count) {
    List<GoodsVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createVO((long) (100 + i)));
    }
    return list;
  }

  /**
   * Create list of GoodsExcelVO for export testing
   *
   * @param count Number of ExcelVOs to create
   * @return List of ExcelVOs
   */
  public static List<GoodsExcelVO> createExcelVOList(int count) {
    List<GoodsExcelVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createExcelVO((long) (100 + i)));
    }
    return list;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
