package net.lab1024.sa.admin.module.system.position;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.system.position.domain.entity.PositionEntity;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionAddForm;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionQueryForm;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionUpdateForm;
import net.lab1024.sa.admin.module.system.position.domain.vo.PositionVO;

/**
 * Test fixtures for Position service tests
 *
 * <p>Provides reusable test data builders for Position entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * PositionEntity entity = PositionTestFixture.createEntity();
 *
 * // Create add form
 * PositionAddForm form = PositionTestFixture.createAddForm();
 *
 * // Create entity with custom positionId
 * PositionEntity entity = PositionTestFixture.createEntity(100L);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class PositionTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create PositionEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static PositionEntity createEntity() {
    int id = counter.incrementAndGet();

    PositionEntity entity = new PositionEntity();
    entity.setPositionName("职务名称-" + id);
    entity.setPositionLevel("P" + id);
    entity.setSort(id * 10);
    entity.setRemark("测试备注-" + id);
    entity.setDeletedFlag(false);

    // Auto-fields: positionId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create PositionEntity with specified positionId
   *
   * @param positionId Position ID
   * @return Entity with all required fields set
   */
  public static PositionEntity createEntity(Long positionId) {
    PositionEntity entity = createEntity();
    entity.setPositionId(positionId);
    return entity;
  }

  /**
   * Create PositionAddForm with default test data
   *
   * @return Form ready for service.add()
   */
  public static PositionAddForm createAddForm() {
    int id = counter.incrementAndGet();

    PositionAddForm form = new PositionAddForm();
    form.setPositionName("新建职务-" + id);
    form.setPositionLevel("P" + id);
    form.setSort(id * 10);
    form.setRemark("新建备注-" + id);

    return form;
  }

  /**
   * Create PositionUpdateForm for updating existing position
   *
   * @param positionId ID of position to update (required)
   * @return Form ready for service.update()
   */
  public static PositionUpdateForm createUpdateForm(Long positionId) {
    int id = counter.incrementAndGet();

    PositionUpdateForm form = new PositionUpdateForm();
    form.setPositionId(positionId);
    form.setPositionName("更新职务-" + id);
    form.setPositionLevel("P" + (id + 100));
    form.setSort(id * 20);
    form.setRemark("更新备注-" + id);

    return form;
  }

  /**
   * Create PositionQueryForm with pagination defaults
   *
   * @return Form ready for service.queryPage()
   */
  public static PositionQueryForm createQueryForm() {
    PositionQueryForm form = new PositionQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create PositionVO for query result testing
   *
   * @return VO with all fields set
   */
  public static PositionVO createVO() {
    return createVO(100L);
  }

  /**
   * Create PositionVO with specified positionId
   *
   * @param positionId Position ID
   * @return VO with all fields set
   */
  public static PositionVO createVO(Long positionId) {
    int id = counter.incrementAndGet();

    PositionVO vo = new PositionVO();
    vo.setPositionId(positionId);
    vo.setPositionName("职务VO-" + id);
    vo.setPositionLevel("P" + id);
    vo.setSort(id * 10);
    vo.setRemark("VO备注-" + id);
    vo.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(id));
    vo.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));

    return vo;
  }

  /**
   * Create list of PositionEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<PositionEntity> createEntityList(int count) {
    List<PositionEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of PositionVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<PositionVO> createVOList(int count) {
    List<PositionVO> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createVO((long) (100 + i)));
    }
    return list;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
