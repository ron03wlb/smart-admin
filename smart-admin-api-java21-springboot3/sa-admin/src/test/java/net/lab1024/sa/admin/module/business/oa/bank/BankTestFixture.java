package net.lab1024.sa.admin.module.business.oa.bank;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankEntity;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankQueryForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankVO;

/**
 * Test fixtures for Bank service tests
 *
 * <p>Provides reusable test data builders for Bank entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * BankEntity entity = BankTestFixture.createEntity();
 *
 * // Create create form
 * BankCreateForm form = BankTestFixture.createCreateForm();
 *
 * // Create entity with custom enterpriseId
 * BankEntity entity = BankTestFixture.createEntity(100L);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class BankTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create BankEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static BankEntity createEntity() {
    return createEntity(1L);
  }

  /**
   * Create BankEntity with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return Entity with all required fields set
   */
  public static BankEntity createEntity(Long enterpriseId) {
    int id = counter.incrementAndGet();

    BankEntity entity = new BankEntity();
    entity.setBankName("测试银行-" + id);
    entity.setAccountName("账户名称-" + id);
    entity.setAccountNumber("6222" + String.format("%015d", id));
    entity.setRemark("测试备注-" + id);
    entity.setBusinessFlag(true);
    entity.setEnterpriseId(enterpriseId);
    entity.setDisabledFlag(false);
    entity.setDeletedFlag(false);
    entity.setCreateUserId(1L);
    entity.setCreateUserName("测试用户");

    // Auto-fields: bankId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create BankCreateForm with default test data
   *
   * @return Form ready for service.createBank()
   */
  public static BankCreateForm createCreateForm() {
    return createCreateForm(1L);
  }

  /**
   * Create BankCreateForm with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.createBank()
   */
  public static BankCreateForm createCreateForm(Long enterpriseId) {
    int id = counter.incrementAndGet();

    BankCreateForm form = new BankCreateForm();
    form.setBankName("新建银行-" + id);
    form.setAccountName("新账户-" + id);
    form.setAccountNumber("6228" + String.format("%015d", id));
    form.setRemark("新建备注-" + id);
    form.setBusinessFlag(true);
    form.setEnterpriseId(enterpriseId);
    form.setDisabledFlag(false);
    form.setCreateUserId(1L);
    form.setCreateUserName("创建用户");

    return form;
  }

  /**
   * Create BankUpdateForm for updating existing bank
   *
   * @param bankId ID of bank to update (required)
   * @return Form ready for service.updateBank()
   */
  public static BankUpdateForm createUpdateForm(Long bankId) {
    return createUpdateForm(bankId, 1L);
  }

  /**
   * Create BankUpdateForm with specified IDs
   *
   * @param bankId ID of bank to update
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.updateBank()
   */
  public static BankUpdateForm createUpdateForm(Long bankId, Long enterpriseId) {
    int id = counter.incrementAndGet();

    BankUpdateForm form = new BankUpdateForm();
    form.setBankId(bankId);
    form.setBankName("更新银行-" + id);
    form.setAccountName("更新账户-" + id);
    form.setAccountNumber("6229" + String.format("%015d", id));
    form.setRemark("更新备注-" + id);
    form.setBusinessFlag(false);
    form.setEnterpriseId(enterpriseId);
    form.setDisabledFlag(false);

    return form;
  }

  /**
   * Create BankQueryForm with pagination defaults
   *
   * @return Form ready for service.queryByPage()
   */
  public static BankQueryForm createQueryForm() {
    BankQueryForm form = new BankQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create BankQueryForm with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.queryList()
   */
  public static BankQueryForm createQueryForm(Long enterpriseId) {
    BankQueryForm form = createQueryForm();
    form.setEnterpriseId(enterpriseId);
    return form;
  }

  /**
   * Create BankVO for query result testing
   *
   * @return VO with all fields set
   */
  public static BankVO createVO() {
    return createVO(100L);
  }

  /**
   * Create BankVO with specified bankId
   *
   * @param bankId Bank ID
   * @return VO with all fields set
   */
  public static BankVO createVO(Long bankId) {
    int id = counter.incrementAndGet();

    BankVO vo = new BankVO();
    vo.setBankId(bankId);
    vo.setBankName("银行VO-" + id);
    vo.setAccountName("账户VO-" + id);
    vo.setAccountNumber("6222" + String.format("%015d", id));
    vo.setRemark("VO备注-" + id);
    vo.setBusinessFlag(true);
    vo.setEnterpriseId(1L);
    vo.setEnterpriseName("企业名称-" + id);
    vo.setDisabledFlag(false);
    vo.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(id));
    vo.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));

    return vo;
  }

  /**
   * Create list of BankEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<BankEntity> createEntityList(int count) {
    List<BankEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of BankVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<BankVO> createVOList(int count) {
    List<BankVO> list = new ArrayList<>();
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
