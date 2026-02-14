package net.lab1024.sa.oa.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import net.lab1024.sa.oa.notice.dao.NoticeDao;
import net.lab1024.sa.oa.notice.dao.NoticeTypeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.oa.notice.domain.entity.NoticeTypeEntity;
import net.lab1024.sa.oa.notice.domain.form.NoticeAddForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeQueryForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeUpdateForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeVisibleRangeForm;
import net.lab1024.sa.oa.notice.domain.vo.NoticeUpdateFormVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeVO;
import net.lab1024.sa.oa.notice.manager.NoticeManager;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * NoticeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>通知公告 CRUD 操作
 *   <li>可見範圍校驗
 *   <li>分類校驗
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NoticeService 單元測試")
class NoticeServiceTest {

  @Mock private NoticeDao noticeDao;

  @Mock private NoticeManager noticeManager;

  @Mock private EmployeeDao employeeDao;

  @Mock private DepartmentDao departmentDao;

  @Mock private NoticeTypeDao noticeTypeDao;

  @Mock private DataTracerService dataTracerService;

  @InjectMocks private NoticeService noticeService;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      NoticeQueryForm queryForm = new NoticeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      NoticeVO noticeVO = createTestNoticeVO(1L, "測試通知");
      noticeVO.setPublishTime(LocalDateTime.now().minusDays(1));
      when(noticeDao.query(any(), any())).thenReturn(Collections.singletonList(noticeVO));

      // When
      PageResult<NoticeVO> result = noticeService.query(queryForm);

      // Then
      assertThat(result.getList()).hasSize(1);
      assertThat(result.getList().get(0).getPublishFlag()).isTrue();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增通知測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：全部可見時應該成功新增")
    void shouldAddNoticeWithAllVisible() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 1L, true);
      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");

      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeManager).saveTransaction(any(NoticeEntity.class), anyList());
    }

    @Test
    @DisplayName("異常情況：分類不存在時應返回錯誤")
    void shouldReturnErrorWhenTypeNotFound() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 999L, true);
      when(noticeTypeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("分类不存在");
      verify(noticeManager, never()).saveTransaction(any(), any());
    }

    @Test
    @DisplayName("異常情況：非全部可見但未設置可見範圍時應返回錯誤")
    void shouldReturnErrorWhenNoVisibleRange() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 1L, false);
      addForm.setVisibleRangeList(Collections.emptyList());
      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");

      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("未设置可见范围");
    }

    @Test
    @DisplayName("正常情況：有可見範圍時應該校驗員工並成功")
    void shouldValidateEmployeeAndSuccess() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 1L, false);
      NoticeVisibleRangeForm visibleRange = new NoticeVisibleRangeForm();
      visibleRange.setDataType(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue());
      visibleRange.setDataId(1L);
      addForm.setVisibleRangeList(Collections.singletonList(visibleRange));

      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");
      EmployeeEntity employee = createTestEmployee(1L);

      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);
      when(employeeDao.selectBatchIds(anyList())).thenReturn(Collections.singletonList(employee));

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("異常情況：員工ID不存在時應返回錯誤")
    void shouldReturnErrorWhenEmployeeNotFound() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 1L, false);
      NoticeVisibleRangeForm visibleRange = new NoticeVisibleRangeForm();
      visibleRange.setDataType(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue());
      visibleRange.setDataId(999L);
      addForm.setVisibleRangeList(Collections.singletonList(visibleRange));

      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");

      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);
      when(employeeDao.selectBatchIds(anyList())).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("员工id不存在");
    }

    @Test
    @DisplayName("正常情況：有部門可見範圍時應該校驗並成功")
    void shouldValidateDepartmentAndSuccess() {
      // Given
      NoticeAddForm addForm = createTestAddForm("測試通知", 1L, false);
      NoticeVisibleRangeForm visibleRange = new NoticeVisibleRangeForm();
      visibleRange.setDataType(NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue());
      visibleRange.setDataId(1L);
      addForm.setVisibleRangeList(Collections.singletonList(visibleRange));

      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");
      DepartmentEntity department = createTestDepartment(1L);

      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);
      when(departmentDao.selectBatchIds(anyList()))
          .thenReturn(Collections.singletonList(department));

      // When
      ResponseDTO<String> result = noticeService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新通知測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新通知")
    void shouldUpdateNoticeSuccess() {
      // Given
      NoticeUpdateForm updateForm = createTestUpdateForm(1L, "更新通知", 1L, true);
      NoticeEntity existingNotice = createTestNoticeEntity(1L, "原通知");
      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");

      when(noticeDao.selectById(1L)).thenReturn(existingNotice);
      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);

      // When
      ResponseDTO<String> result = noticeService.update(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeManager).updateTransaction(any(), any(), anyList());
    }

    @Test
    @DisplayName("異常情況：通知不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      NoticeUpdateForm updateForm = createTestUpdateForm(999L, "更新通知", 1L, true);
      when(noticeDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = noticeService.update(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("通知不存在");
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除通知測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除通知")
    void shouldDeleteNoticeSuccess() {
      // Given
      Long noticeId = 1L;
      NoticeEntity existingNotice = createTestNoticeEntity(noticeId, "通知");

      when(noticeDao.selectById(noticeId)).thenReturn(existingNotice);

      // When
      ResponseDTO<String> result = noticeService.delete(noticeId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeDao).updateDeletedFlag(noticeId);
    }

    @Test
    @DisplayName("異常情況：通知不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long noticeId = 999L;
      when(noticeDao.selectById(noticeId)).thenReturn(null);

      // When
      ResponseDTO<String> result = noticeService.delete(noticeId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("通知公告不存在");
    }

    @Test
    @DisplayName("異常情況：通知已刪除時應返回錯誤")
    void shouldReturnErrorWhenDeleted() {
      // Given
      Long noticeId = 1L;
      NoticeEntity deletedNotice = createTestNoticeEntity(noticeId, "已刪除通知");
      deletedNotice.setDeletedFlag(true);

      when(noticeDao.selectById(noticeId)).thenReturn(deletedNotice);

      // When
      ResponseDTO<String> result = noticeService.delete(noticeId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("通知公告不存在");
    }
  }

  // ==================== getUpdateFormVO 測試 ====================

  @Nested
  @DisplayName("getUpdateFormVO 獲取更新表單測試")
  class GetUpdateFormVOTest {

    @Test
    @DisplayName("正常情況：應該返回更新表單VO")
    void shouldReturnUpdateFormVO() {
      // Given
      Long noticeId = 1L;
      NoticeEntity noticeEntity = createTestNoticeEntity(noticeId, "通知");
      noticeEntity.setNoticeTypeId(1L);
      noticeEntity.setAllVisibleFlag(true);
      noticeEntity.setPublishTime(LocalDateTime.now().minusDays(1));

      NoticeTypeEntity noticeType = createTestNoticeType(1L, "通知類型");

      when(noticeDao.selectById(noticeId)).thenReturn(noticeEntity);
      when(noticeTypeDao.selectById(1L)).thenReturn(noticeType);

      // When
      NoticeUpdateFormVO result = noticeService.getUpdateFormVO(noticeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getNoticeTypeName()).isEqualTo("通知類型");
    }

    @Test
    @DisplayName("異常情況：通知不存在時應返回null")
    void shouldReturnNullWhenNotFound() {
      // Given
      Long noticeId = 999L;
      when(noticeDao.selectById(noticeId)).thenReturn(null);

      // When
      NoticeUpdateFormVO result = noticeService.getUpdateFormVO(noticeId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private NoticeVO createTestNoticeVO(Long id, String title) {
    NoticeVO vo = new NoticeVO();
    vo.setNoticeId(id);
    vo.setTitle(title);
    return vo;
  }

  private NoticeEntity createTestNoticeEntity(Long id, String title) {
    NoticeEntity entity = new NoticeEntity();
    entity.setNoticeId(id);
    entity.setTitle(title);
    entity.setDeletedFlag(false);
    return entity;
  }

  private NoticeAddForm createTestAddForm(String title, Long noticeTypeId, Boolean allVisible) {
    NoticeAddForm form = new NoticeAddForm();
    form.setTitle(title);
    form.setNoticeTypeId(noticeTypeId);
    form.setAllVisibleFlag(allVisible);
    form.setScheduledPublishFlag(false);
    form.setVisibleRangeList(Collections.emptyList());
    return form;
  }

  private NoticeUpdateForm createTestUpdateForm(
      Long noticeId, String title, Long noticeTypeId, Boolean allVisible) {
    NoticeUpdateForm form = new NoticeUpdateForm();
    form.setNoticeId(noticeId);
    form.setTitle(title);
    form.setNoticeTypeId(noticeTypeId);
    form.setAllVisibleFlag(allVisible);
    form.setVisibleRangeList(Collections.emptyList());
    return form;
  }

  private NoticeTypeEntity createTestNoticeType(Long id, String name) {
    NoticeTypeEntity entity = new NoticeTypeEntity();
    entity.setNoticeTypeId(id);
    entity.setNoticeTypeName(name);
    return entity;
  }

  private EmployeeEntity createTestEmployee(Long id) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setActualName("員工" + id);
    return entity;
  }

  private DepartmentEntity createTestDepartment(Long id) {
    DepartmentEntity entity = new DepartmentEntity();
    entity.setDepartmentId(id);
    entity.setDepartmentName("部門" + id);
    return entity;
  }
}
