package net.lab1024.sa.admin.module.business.oa.notice;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeTypeEntity;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeAddForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeQueryForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeUpdateForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeVisibleRangeForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeUpdateFormVO;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeVO;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeVisibleRangeVO;

/**
 * Test fixtures for Notice service tests
 *
 * <p>Provides reusable test data builders for Notice entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * NoticeEntity entity = NoticeTestFixture.createEntity();
 *
 * // Create add form
 * NoticeAddForm form = NoticeTestFixture.createAddForm();
 *
 * // Create visible range form for employee
 * NoticeVisibleRangeForm rangeForm = NoticeTestFixture.createVisibleRangeForm(
 *     NoticeVisibleRangeDataTypeEnum.EMPLOYEE, 1L);
 * }</pre>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
public class NoticeTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create NoticeEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static NoticeEntity createEntity() {
    return createEntity(1L);
  }

  /**
   * Create NoticeEntity with specified noticeTypeId
   *
   * @param noticeTypeId Notice type ID
   * @return Entity with all required fields set
   */
  public static NoticeEntity createEntity(Long noticeTypeId) {
    int id = counter.incrementAndGet();

    NoticeEntity entity = new NoticeEntity();
    entity.setNoticeTypeId(noticeTypeId);
    entity.setTitle("通知标题-" + id);
    entity.setAllVisibleFlag(true);
    entity.setScheduledPublishFlag(false);
    entity.setPublishTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
    entity.setContentText("纯文本内容-" + id);
    entity.setContentHtml("<p>HTML内容-" + id + "</p>");
    entity.setAttachment("file-" + id + ".pdf");
    entity.setPageViewCount(100);
    entity.setUserViewCount(50);
    entity.setSource("测试来源-" + id);
    entity.setAuthor("测试作者-" + id);
    entity.setDocumentNumber("DOC-" + id);
    entity.setDeletedFlag(false);
    entity.setCreateUserId(1L);

    // Auto-fields: noticeId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create NoticeAddForm with default test data (all visible)
   *
   * @return Form ready for service.add()
   */
  public static NoticeAddForm createAddForm() {
    return createAddForm(1L, true);
  }

  /**
   * Create NoticeAddForm with specified noticeTypeId and visibility
   *
   * @param noticeTypeId Notice type ID
   * @param allVisibleFlag Whether all visible
   * @return Form ready for service.add()
   */
  public static NoticeAddForm createAddForm(Long noticeTypeId, Boolean allVisibleFlag) {
    int id = counter.incrementAndGet();

    NoticeAddForm form = new NoticeAddForm();
    form.setTitle("新通知-" + id);
    form.setNoticeTypeId(noticeTypeId);
    form.setAllVisibleFlag(allVisibleFlag);
    form.setScheduledPublishFlag(false);
    form.setPublishTime(LocalDateTime.now(ZoneId.systemDefault()));
    form.setContentText("新文本内容-" + id);
    form.setContentHtml("<p>新HTML内容-" + id + "</p>");
    form.setAttachment("new-file-" + id + ".pdf");
    form.setAuthor("新作者-" + id);
    form.setSource("新来源-" + id);
    form.setDocumentNumber("NEW-DOC-" + id);
    form.setCreateUserId(1L);

    if (!allVisibleFlag) {
      // Add default visible range (one employee)
      form.setVisibleRangeList(
          Arrays.asList(createVisibleRangeForm(NoticeVisibleRangeDataTypeEnum.EMPLOYEE, 1L)));
    }

    return form;
  }

  /**
   * Create NoticeUpdateForm for updating existing notice
   *
   * @param noticeId ID of notice to update (required)
   * @return Form ready for service.update()
   */
  public static NoticeUpdateForm createUpdateForm(Long noticeId) {
    return createUpdateForm(noticeId, 1L, true);
  }

  /**
   * Create NoticeUpdateForm with specified IDs and visibility
   *
   * @param noticeId ID of notice to update
   * @param noticeTypeId Notice type ID
   * @param allVisibleFlag Whether all visible
   * @return Form ready for service.update()
   */
  public static NoticeUpdateForm createUpdateForm(
      Long noticeId, Long noticeTypeId, Boolean allVisibleFlag) {
    int id = counter.incrementAndGet();

    NoticeUpdateForm form = new NoticeUpdateForm();
    form.setNoticeId(noticeId);
    form.setTitle("更新通知-" + id);
    form.setNoticeTypeId(noticeTypeId);
    form.setAllVisibleFlag(allVisibleFlag);
    form.setScheduledPublishFlag(false);
    form.setPublishTime(LocalDateTime.now(ZoneId.systemDefault()));
    form.setContentText("更新文本内容-" + id);
    form.setContentHtml("<p>更新HTML内容-" + id + "</p>");
    form.setAttachment("updated-file-" + id + ".pdf");
    form.setAuthor("更新作者-" + id);
    form.setSource("更新来源-" + id);
    form.setDocumentNumber("UPD-DOC-" + id);

    if (!allVisibleFlag) {
      form.setVisibleRangeList(
          Arrays.asList(createVisibleRangeForm(NoticeVisibleRangeDataTypeEnum.EMPLOYEE, 1L)));
    }

    return form;
  }

  /**
   * Create NoticeQueryForm with pagination defaults
   *
   * @return Form ready for service.query()
   */
  public static NoticeQueryForm createQueryForm() {
    NoticeQueryForm form = new NoticeQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /**
   * Create NoticeVO for query result testing
   *
   * @return VO with all fields set
   */
  public static NoticeVO createVO() {
    return createVO(100L);
  }

  /**
   * Create NoticeVO with specified noticeId
   *
   * @param noticeId Notice ID
   * @return VO with all fields set
   */
  public static NoticeVO createVO(Long noticeId) {
    int id = counter.incrementAndGet();

    NoticeVO vo = new NoticeVO();
    vo.setNoticeId(noticeId);
    vo.setNoticeTypeId(1L);
    vo.setNoticeTypeName("通知类型-" + id);
    vo.setTitle("通知VO-" + id);
    vo.setAllVisibleFlag(true);
    vo.setScheduledPublishFlag(false);
    vo.setPublishTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(2));
    vo.setPageViewCount(200);
    vo.setUserViewCount(100);
    vo.setSource("VO来源-" + id);
    vo.setAuthor("VO作者-" + id);
    vo.setDocumentNumber("VO-DOC-" + id);
    vo.setCreateUserName("VO创建者");
    vo.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(10));
    vo.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
    vo.setPublishFlag(true);

    return vo;
  }

  /**
   * Create NoticeUpdateFormVO for getUpdateFormVO result testing
   *
   * @param noticeId Notice ID
   * @return UpdateFormVO with all fields set
   */
  public static NoticeUpdateFormVO createUpdateFormVO(Long noticeId) {
    int id = counter.incrementAndGet();

    NoticeUpdateFormVO vo = new NoticeUpdateFormVO();
    vo.setNoticeId(noticeId);
    vo.setNoticeTypeId(1L);
    vo.setNoticeTypeName("通知类型名称");
    vo.setTitle("表单VO-" + id);
    vo.setAllVisibleFlag(false);
    vo.setScheduledPublishFlag(false);
    vo.setPublishTime(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));
    vo.setContentText("表单文本-" + id);
    vo.setContentHtml("<p>表单HTML-" + id + "</p>");
    vo.setAttachment("form-file-" + id + ".pdf");
    vo.setPageViewCount(150);
    vo.setUserViewCount(75);
    vo.setSource("表单来源-" + id);
    vo.setAuthor("表单作者-" + id);
    vo.setDocumentNumber("FORM-DOC-" + id);
    vo.setPublishFlag(true);
    vo.setVisibleRangeList(
        Arrays.asList(createVisibleRangeVO(NoticeVisibleRangeDataTypeEnum.EMPLOYEE, 1L, "员工名称")));

    return vo;
  }

  /**
   * Create NoticeTypeEntity for notice type testing
   *
   * @param noticeTypeId Notice type ID
   * @return NoticeTypeEntity with basic fields
   */
  public static NoticeTypeEntity createNoticeTypeEntity(Long noticeTypeId) {
    int id = counter.incrementAndGet();

    NoticeTypeEntity entity = new NoticeTypeEntity();
    entity.setNoticeTypeId(noticeTypeId);
    entity.setNoticeTypeName("通知类型-" + id);

    return entity;
  }

  /**
   * Create NoticeVisibleRangeForm for visible range testing
   *
   * @param dataType Data type (EMPLOYEE or DEPARTMENT)
   * @param dataId Employee or Department ID
   * @return NoticeVisibleRangeForm ready to use
   */
  public static NoticeVisibleRangeForm createVisibleRangeForm(
      NoticeVisibleRangeDataTypeEnum dataType, Long dataId) {
    return new NoticeVisibleRangeForm(dataType.getValue(), dataId);
  }

  /**
   * Create NoticeVisibleRangeVO for visible range query result testing
   *
   * @param dataType Data type (EMPLOYEE or DEPARTMENT)
   * @param dataId Employee or Department ID
   * @param dataName Employee or Department name
   * @return NoticeVisibleRangeVO with all fields set
   */
  public static NoticeVisibleRangeVO createVisibleRangeVO(
      NoticeVisibleRangeDataTypeEnum dataType, Long dataId, String dataName) {
    NoticeVisibleRangeVO vo = new NoticeVisibleRangeVO();
    vo.setDataType(dataType.getValue());
    vo.setDataId(dataId);
    vo.setDataName(dataName);
    return vo;
  }

  /**
   * Create list of NoticeEntity for batch testing
   *
   * @param count Number of entities to create
   * @return List of entities
   */
  public static List<NoticeEntity> createEntityList(int count) {
    List<NoticeEntity> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(createEntity());
    }
    return list;
  }

  /**
   * Create list of NoticeVO for query result testing
   *
   * @param count Number of VOs to create
   * @return List of VOs
   */
  public static List<NoticeVO> createVOList(int count) {
    List<NoticeVO> list = new ArrayList<>();
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
