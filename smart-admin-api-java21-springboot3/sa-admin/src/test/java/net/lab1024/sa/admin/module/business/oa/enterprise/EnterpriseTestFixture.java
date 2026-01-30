package net.lab1024.sa.admin.module.business.oa.enterprise;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEmployeeEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseEmployeeVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseExcelVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseVO;

/**
 * Test fixtures for Enterprise service tests
 *
 * <p>Provides reusable test data builders for Enterprise entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * EnterpriseEntity entity = EnterpriseTestFixture.createEntity();
 *
 * // Create create form
 * EnterpriseCreateForm form = EnterpriseTestFixture.createCreateForm();
 *
 * // Create employee form
 * EnterpriseEmployeeForm form = EnterpriseTestFixture.createEmployeeForm(1L, Arrays.asList(100L, 101L));
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class EnterpriseTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create EnterpriseEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static EnterpriseEntity createEntity() {
    int id = counter.incrementAndGet();

    EnterpriseEntity entity = new EnterpriseEntity();
    entity.setEnterpriseName("企业名称-" + id);
    entity.setEnterpriseLogo("logo-" + id + ".png");
    entity.setUnifiedSocialCreditCode("91" + String.format("%016d", id));
    entity.setType(1); // 假设类型1为普通企业
    entity.setContact("联系人-" + id);
    entity.setContactPhone("13800138" + String.format("%03d", id % 1000));
    entity.setEmail("enterprise" + id + "@test.com");
    entity.setProvince(110000);
    entity.setProvinceName("北京市");
    entity.setCity(110100);
    entity.setCityName("北京市");
    entity.setDistrict(110101);
    entity.setDistrictName("东城区");
    entity.setAddress("详细地址-" + id);
    entity.setBusinessLicense("license-" + id + ".pdf");
    entity.setDisabledFlag(false);
    entity.setDeletedFlag(false);
    entity.setCreateUserId(1L);
    entity.setCreateUserName("测试用户");

    // Auto-fields: enterpriseId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create EnterpriseCreateForm with default test data
   *
   * @return Form ready for service.createEnterprise()
   */
  public static EnterpriseCreateForm createCreateForm() {
    int id = counter.incrementAndGet();

    EnterpriseCreateForm form = new EnterpriseCreateForm();
    form.setEnterpriseName("新企业-" + id);
    form.setEnterpriseLogo("new-logo-" + id + ".png");
    form.setUnifiedSocialCreditCode("91" + String.format("%016d", id));
    form.setType(1);
    form.setContact("新联系人-" + id);
    form.setContactPhone("13900139" + String.format("%03d", id % 1000));
    form.setEmail("new" + id + "@test.com");
    form.setProvince(310000);
    form.setProvinceName("上海市");
    form.setCity(310100);
    form.setCityName("上海市");
    form.setDistrict(310101);
    form.setDistrictName("黄浦区");
    form.setAddress("新地址-" + id);
    form.setBusinessLicense("new-license-" + id + ".pdf");
    form.setDisabledFlag(false);
    form.setCreateUserId(1L);
    form.setCreateUserName("创建用户");

    return form;
  }

  /**
   * Create EnterpriseUpdateForm for updating existing enterprise
   *
   * @param enterpriseId ID of enterprise to update (required)
   * @return Form ready for service.updateEnterprise()
   */
  public static EnterpriseUpdateForm createUpdateForm(Long enterpriseId) {
    int id = counter.incrementAndGet();

    EnterpriseUpdateForm form = new EnterpriseUpdateForm();
    form.setEnterpriseId(enterpriseId);
    form.setEnterpriseName("更新企业-" + id);
    form.setEnterpriseLogo("updated-logo-" + id + ".png");
    form.setUnifiedSocialCreditCode("91" + String.format("%016d", id + 1000));
    form.setType(2);
    form.setContact("更新联系人-" + id);
    form.setContactPhone("13700137" + String.format("%03d", id % 1000));
    form.setEmail("updated" + id + "@test.com");
    form.setProvince(440000);
    form.setProvinceName("广东省");
    form.setCity(440100);
    form.setCityName("广州市");
    form.setDistrict(440103);
    form.setDistrictName("荔湾区");
    form.setAddress("更新地址-" + id);
    form.setBusinessLicense("updated-license-" + id + ".pdf");
    form.setDisabledFlag(false);

    return form;
  }

  /**
   * Create EnterpriseQueryForm with pagination defaults
   *
   * @return Form ready for service.queryByPage()
   */
  public static EnterpriseQueryForm createQueryForm() {
    EnterpriseQueryForm form = new EnterpriseQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create EnterpriseVO for query result testing
   *
   * @return VO with all fields set
   */
  public static EnterpriseVO createVO() {
    return createVO(100L);
  }

  /**
   * Create EnterpriseVO with specified enterpriseId
   *
   * @param enterpriseId Enterprise ID
   * @return VO with all fields set
   */
  public static EnterpriseVO createVO(Long enterpriseId) {
    int id = counter.incrementAndGet();

    EnterpriseVO vo = new EnterpriseVO();
    vo.setEnterpriseId(enterpriseId);
    vo.setEnterpriseName("企业VO-" + id);
    vo.setEnterpriseLogo("vo-logo-" + id + ".png");
    vo.setUnifiedSocialCreditCode("91" + String.format("%016d", id));
    vo.setType(1);
    vo.setContact("VO联系人-" + id);
    vo.setContactPhone("13600136" + String.format("%03d", id % 1000));
    vo.setEmail("vo" + id + "@test.com");
    vo.setProvince(110000);
    vo.setProvinceName("北京市");
    vo.setCity(110100);
    vo.setCityName("北京市");
    vo.setDistrict(110101);
    vo.setDistrictName("东城区");
    vo.setAddress("VO地址-" + id);
    vo.setBusinessLicense("vo-license-" + id + ".pdf");
    vo.setDisabledFlag(false);
    vo.setCreateTime(LocalDateTime.now().minusDays(id));
    vo.setUpdateTime(LocalDateTime.now());

    return vo;
  }

  /**
   * Create EnterpriseListVO for list query result testing
   *
   * @param enterpriseId Enterprise ID
   * @return ListVO with minimal fields
   */
  public static EnterpriseListVO createListVO(Long enterpriseId) {
    int id = counter.incrementAndGet();

    EnterpriseListVO vo = new EnterpriseListVO();
    vo.setEnterpriseId(enterpriseId);
    vo.setEnterpriseName("企业列表-" + id);

    return vo;
  }

  /**
   * Create EnterpriseExcelVO for export testing
   *
   * @return ExcelVO with all export fields
   */
  public static EnterpriseExcelVO createExcelVO() {
    int id = counter.incrementAndGet();

    EnterpriseExcelVO vo = new EnterpriseExcelVO();
    vo.setEnterpriseName("导出企业-" + id);
    vo.setUnifiedSocialCreditCode("91" + String.format("%016d", id));
    vo.setContact("导出联系人-" + id);
    vo.setContactPhone("13500135" + String.format("%03d", id % 1000));

    return vo;
  }

  /**
   * Create EnterpriseEmployeeForm for adding/deleting employees
   *
   * @param enterpriseId Enterprise ID
   * @param employeeIdList List of employee IDs
   * @return Form ready for service.addEmployee() or service.deleteEmployee()
   */
  public static EnterpriseEmployeeForm createEmployeeForm(
      Long enterpriseId, List<Long> employeeIdList) {
    EnterpriseEmployeeForm form = new EnterpriseEmployeeForm();
    form.setEnterpriseId(enterpriseId);
    form.setEmployeeIdList(employeeIdList);
    return form;
  }

  /**
   * Create EnterpriseEmployeeEntity for employee relationship
   *
   * @param enterpriseId Enterprise ID
   * @param employeeId Employee ID
   * @return Entity ready to be saved
   */
  public static EnterpriseEmployeeEntity createEmployeeEntity(Long enterpriseId, Long employeeId) {
    EnterpriseEmployeeEntity entity = new EnterpriseEmployeeEntity();
    entity.setEnterpriseId(enterpriseId);
    entity.setEmployeeId(employeeId);
    return entity;
  }

  /**
   * Create EnterpriseEmployeeVO for employee query result testing
   *
   * @param enterpriseId Enterprise ID
   * @param employeeId Employee ID
   * @return VO with employee and enterprise fields
   */
  public static EnterpriseEmployeeVO createEmployeeVO(Long enterpriseId, Long employeeId) {
    int id = counter.incrementAndGet();

    EnterpriseEmployeeVO vo = new EnterpriseEmployeeVO();
    vo.setEnterpriseId(enterpriseId);
    vo.setEmployeeId(employeeId);
    vo.setActualName("员工-" + id);
    vo.setLoginName("employee" + id);
    vo.setPhone("13800138" + String.format("%03d", id % 1000));
    vo.setDepartmentId(1L);
    vo.setDepartmentName("测试部门");
    vo.setDisabledFlag(false);

    return vo;
  }

  /**
   * Create EnterpriseEmployeeQueryForm with pagination defaults
   *
   * @param enterpriseId Enterprise ID
   * @return Form ready for service.queryPageEmployeeList()
   */
  public static EnterpriseEmployeeQueryForm createEmployeeQueryForm(Long enterpriseId) {
    EnterpriseEmployeeQueryForm form = new EnterpriseEmployeeQueryForm();
    form.setEnterpriseId(enterpriseId);
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create list of EnterpriseEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<EnterpriseEntity> createEntityList(int count) {
    List<EnterpriseEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of EnterpriseVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<EnterpriseVO> createVOList(int count) {
    List<EnterpriseVO> list = new ArrayList<>();
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
