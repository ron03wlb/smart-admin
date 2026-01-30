package net.lab1024.sa.admin.module.business.oa.invoice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceQueryForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceVO;

/**
 * Test fixtures for Invoice service tests
 *
 * <p>Provides reusable test data builders for Invoice entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * InvoiceEntity entity = InvoiceTestFixture.createEntity();
 *
 * // Create add form
 * InvoiceAddForm form = InvoiceTestFixture.createAddForm();
 *
 * // Create entity with custom enterpriseId
 * InvoiceEntity entity = InvoiceTestFixture.createEntity(100L);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class InvoiceTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create InvoiceEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static InvoiceEntity createEntity() {
    return createEntity(1L);
  }

  /**
   * Create InvoiceEntity with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return Entity with all required fields set
   */
  public static InvoiceEntity createEntity(Long enterpriseId) {
    int id = counter.incrementAndGet();

    InvoiceEntity entity = new InvoiceEntity();
    entity.setInvoiceHeads("发票抬头-" + id);
    entity.setTaxpayerIdentificationNumber("91" + String.format("%016d", id));
    entity.setAccountNumber("6222" + String.format("%012d", id));
    entity.setBankName("中国银行" + id + "支行");
    entity.setRemark("测试发票备注 " + id);
    entity.setEnterpriseId(enterpriseId);
    entity.setDisabledFlag(false);
    entity.setDeletedFlag(false);

    // Auto-fields: invoiceId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create InvoiceAddForm with default test data
   *
   * @return Form ready for service.createInvoice()
   */
  public static InvoiceAddForm createAddForm() {
    return createAddForm(1L);
  }

  /**
   * Create InvoiceAddForm with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.createInvoice()
   */
  public static InvoiceAddForm createAddForm(Long enterpriseId) {
    int id = counter.incrementAndGet();

    InvoiceAddForm form = new InvoiceAddForm();
    form.setInvoiceHeads("新发票抬头-" + id);
    form.setTaxpayerIdentificationNumber("91" + String.format("%016d", id));
    form.setAccountNumber("6222" + String.format("%012d", id));
    form.setBankName("招商银行" + id + "支行");
    form.setRemark("添加测试发票 " + id);
    form.setEnterpriseId(enterpriseId);

    return form;
  }

  /**
   * Create InvoiceUpdateForm for updating existing invoice
   *
   * @param invoiceId ID of invoice to update (required)
   * @return Form ready for service.updateInvoice()
   */
  public static InvoiceUpdateForm createUpdateForm(Long invoiceId) {
    return createUpdateForm(invoiceId, 1L);
  }

  /**
   * Create InvoiceUpdateForm with specified IDs
   *
   * @param invoiceId ID of invoice to update
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.updateInvoice()
   */
  public static InvoiceUpdateForm createUpdateForm(Long invoiceId, Long enterpriseId) {
    int id = counter.incrementAndGet();

    InvoiceUpdateForm form = new InvoiceUpdateForm();
    form.setInvoiceId(invoiceId);
    form.setInvoiceHeads("更新发票抬头-" + id);
    form.setTaxpayerIdentificationNumber("91" + String.format("%016d", id + 1000));
    form.setAccountNumber("6222" + String.format("%012d", id + 1000));
    form.setBankName("工商银行" + id + "支行");
    form.setRemark("更新测试发票 " + id);
    form.setEnterpriseId(enterpriseId);
    form.setDisabledFlag(false);

    return form;
  }

  /**
   * Create InvoiceQueryForm with pagination defaults
   *
   * @return Form ready for service.queryByPage()
   */
  public static InvoiceQueryForm createQueryForm() {
    InvoiceQueryForm form = new InvoiceQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create InvoiceVO for query result testing
   *
   * @return VO with all fields set
   */
  public static InvoiceVO createVO() {
    return createVO(100L);
  }

  /**
   * Create InvoiceVO with specified invoiceId
   *
   * @param invoiceId Invoice ID
   * @return VO with all fields set
   */
  public static InvoiceVO createVO(Long invoiceId) {
    int id = counter.incrementAndGet();

    InvoiceVO vo = new InvoiceVO();
    vo.setInvoiceId(invoiceId);
    vo.setInvoiceHeads("发票抬头VO-" + id);
    vo.setTaxpayerIdentificationNumber("91" + String.format("%016d", id));
    vo.setAccountNumber("6222" + String.format("%012d", id));
    vo.setBankName("银行-" + id);
    vo.setRemark("VO备注 " + id);
    vo.setEnterpriseId(1L);
    vo.setEnterpriseName("企业-" + id);
    vo.setDisabledFlag(false);
    vo.setCreateTime(LocalDateTime.now().minusDays(id));
    vo.setUpdateTime(LocalDateTime.now());

    return vo;
  }

  /**
   * Create list of InvoiceEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<InvoiceEntity> createEntityList(int count) {
    List<InvoiceEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of InvoiceVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<InvoiceVO> createVOList(int count) {
    List<InvoiceVO> list = new ArrayList<>();
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
