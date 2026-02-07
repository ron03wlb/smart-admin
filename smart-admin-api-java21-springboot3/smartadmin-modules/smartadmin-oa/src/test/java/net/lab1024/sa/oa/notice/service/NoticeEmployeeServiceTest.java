package net.lab1024.sa.oa.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import net.lab1024.sa.oa.notice.dao.NoticeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.oa.notice.domain.form.NoticeEmployeeQueryForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeViewRecordQueryForm;
import net.lab1024.sa.oa.notice.domain.vo.NoticeDetailVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeEmployeeVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeViewRecordVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeVisibleRangeVO;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
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
 * NoticeEmployeeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>員工查看通知列表
 *   <li>查看通知詳情
 *   <li>可見範圍校驗
 *   <li>查看記錄
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NoticeEmployeeService 單元測試")
class NoticeEmployeeServiceTest {

  @Mock private NoticeDao noticeDao;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @Mock private EmployeeDao employeeDao;

  @InjectMocks private NoticeEmployeeService noticeEmployeeService;

  // ==================== queryList 測試 ====================

  @Nested
  @DisplayName("queryList 查詢通知列表測試")
  class QueryListTest {

    @Test
    @DisplayName("正常情況：管理員應該返回通知列表")
    void shouldReturnNoticeListForAdmin() {
      // Given
      Long employeeId = 1L;
      NoticeEmployeeQueryForm queryForm = new NoticeEmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeEntity employee = createTestEmployee(employeeId, true, null);
      NoticeEmployeeVO noticeVO = createTestNoticeEmployeeVO(1L, "測試通知");

      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(noticeDao.queryEmployeeNotice(
              any(), anyLong(), any(), anyList(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
          .thenReturn(Collections.singletonList(noticeVO));

      // When
      ResponseDTO<PageResult<NoticeEmployeeVO>> result =
          noticeEmployeeService.queryList(employeeId, queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(1);
    }

    @Test
    @DisplayName("正常情況：非管理員應該根據部門過濾通知")
    void shouldFilterNoticeByDepartmentForNonAdmin() {
      // Given
      Long employeeId = 1L;
      NoticeEmployeeQueryForm queryForm = new NoticeEmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      EmployeeEntity employee = createTestEmployee(employeeId, false, 1L);
      NoticeEmployeeVO noticeVO = createTestNoticeEmployeeVO(1L, "測試通知");

      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(departmentCacheManager.getDepartmentSelfAndChildren(1L))
          .thenReturn(Arrays.asList(1L, 2L));
      when(noticeDao.queryEmployeeNotice(
              any(), anyLong(), any(), anyList(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
          .thenReturn(Collections.singletonList(noticeVO));

      // When
      ResponseDTO<PageResult<NoticeEmployeeVO>> result =
          noticeEmployeeService.queryList(employeeId, queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("正常情況：只查詢未讀通知")
    void shouldQueryNotViewNotice() {
      // Given
      Long employeeId = 1L;
      NoticeEmployeeQueryForm queryForm = new NoticeEmployeeQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setNotViewFlag(true);

      EmployeeEntity employee = createTestEmployee(employeeId, true, null);
      NoticeEmployeeVO noticeVO = createTestNoticeEmployeeVO(1L, "未讀通知");

      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(noticeDao.queryEmployeeNotViewNotice(
              any(), anyLong(), any(), anyList(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
          .thenReturn(Collections.singletonList(noticeVO));

      // When
      ResponseDTO<PageResult<NoticeEmployeeVO>> result =
          noticeEmployeeService.queryList(employeeId, queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== view 測試 ====================

  @Nested
  @DisplayName("view 查看通知詳情測試")
  class ViewTest {

    @Test
    @DisplayName("正常情況：全部可見時應該成功查看")
    void shouldViewNoticeWhenAllVisible() {
      // Given
      Long employeeId = 1L;
      Long noticeId = 1L;
      String ip = "127.0.0.1";
      String userAgent = "Mozilla";

      NoticeEntity noticeEntity = createTestNoticeEntity(noticeId, "通知", true);
      EmployeeEntity employee = createTestEmployee(employeeId, false, 1L);

      when(noticeDao.selectById(noticeId)).thenReturn(noticeEntity);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(noticeDao.viewRecordCount(noticeId, employeeId)).thenReturn(0L);

      // When
      ResponseDTO<NoticeDetailVO> result =
          noticeEmployeeService.view(employeeId, noticeId, ip, userAgent);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeDao).insertViewRecord(noticeId, employeeId, ip, userAgent, 1);
      verify(noticeDao).updateViewCount(noticeId, 1, 1);
    }

    @Test
    @DisplayName("異常情況：通知不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long employeeId = 1L;
      Long noticeId = 999L;

      when(noticeDao.selectById(noticeId)).thenReturn(null);

      // When
      ResponseDTO<NoticeDetailVO> result =
          noticeEmployeeService.view(employeeId, noticeId, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("通知公告不存在");
    }

    @Test
    @DisplayName("異常情況：通知已刪除時應返回錯誤")
    void shouldReturnErrorWhenDeleted() {
      // Given
      Long employeeId = 1L;
      Long noticeId = 1L;

      NoticeEntity deletedNotice = createTestNoticeEntity(noticeId, "已刪除", true);
      deletedNotice.setDeletedFlag(true);

      when(noticeDao.selectById(noticeId)).thenReturn(deletedNotice);

      // When
      ResponseDTO<NoticeDetailVO> result =
          noticeEmployeeService.view(employeeId, noticeId, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("通知公告不存在");
    }

    @Test
    @DisplayName("正常情況：再次查看時只更新頁面瀏覽量")
    void shouldUpdatePageViewWhenViewAgain() {
      // Given
      Long employeeId = 1L;
      Long noticeId = 1L;
      String ip = "127.0.0.1";
      String userAgent = "Mozilla";

      NoticeEntity noticeEntity = createTestNoticeEntity(noticeId, "通知", true);
      EmployeeEntity employee = createTestEmployee(employeeId, false, 1L);

      when(noticeDao.selectById(noticeId)).thenReturn(noticeEntity);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(noticeDao.viewRecordCount(noticeId, employeeId)).thenReturn(1L);

      // When
      ResponseDTO<NoticeDetailVO> result =
          noticeEmployeeService.view(employeeId, noticeId, ip, userAgent);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeDao).updateViewRecord(noticeId, employeeId, ip, userAgent);
      verify(noticeDao).updateViewCount(noticeId, 1, 0);
    }

    @Test
    @DisplayName("異常情況：無權限查看時應返回錯誤")
    void shouldReturnErrorWhenNoPermission() {
      // Given
      Long employeeId = 1L;
      Long noticeId = 1L;

      NoticeEntity noticeEntity = createTestNoticeEntity(noticeId, "限制通知", false);
      EmployeeEntity employee = createTestEmployee(employeeId, false, 1L);

      when(noticeDao.selectById(noticeId)).thenReturn(noticeEntity);
      when(employeeDao.selectById(employeeId)).thenReturn(employee);
      when(noticeDao.queryVisibleRange(noticeId)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<NoticeDetailVO> result =
          noticeEmployeeService.view(employeeId, noticeId, "127.0.0.1", "Mozilla");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("没有权限查看");
    }
  }

  // ==================== checkVisibleRange 測試 ====================

  @Nested
  @DisplayName("checkVisibleRange 校驗可見範圍測試")
  class CheckVisibleRangeTest {

    @Test
    @DisplayName("正常情況：員工在可見範圍內應返回true")
    void shouldReturnTrueWhenEmployeeInRange() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 1L;

      NoticeVisibleRangeVO range =
          createVisibleRangeVO(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue(), employeeId);

      // When
      boolean result =
          noticeEmployeeService.checkVisibleRange(
              Collections.singletonList(range), employeeId, departmentId);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("正常情況：部門在可見範圍內應返回true")
    void shouldReturnTrueWhenDepartmentInRange() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 1L;

      NoticeVisibleRangeVO range =
          createVisibleRangeVO(NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue(), 1L);

      when(departmentCacheManager.getDepartmentSelfAndChildren(1L))
          .thenReturn(Arrays.asList(1L, 2L));

      // When
      boolean result =
          noticeEmployeeService.checkVisibleRange(
              Collections.singletonList(range), employeeId, departmentId);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("邊界情況：不在任何範圍內應返回false")
    void shouldReturnFalseWhenNotInRange() {
      // Given
      Long employeeId = 1L;
      Long departmentId = 1L;

      NoticeVisibleRangeVO range =
          createVisibleRangeVO(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue(), 999L);

      // When
      boolean result =
          noticeEmployeeService.checkVisibleRange(
              Collections.singletonList(range), employeeId, departmentId);

      // Then
      assertThat(result).isFalse();
    }
  }

  // ==================== queryViewRecord 測試 ====================

  @Nested
  @DisplayName("queryViewRecord 查詢查看記錄測試")
  class QueryViewRecordTest {

    @Test
    @DisplayName("正常情況：應該返回查看記錄列表")
    void shouldReturnViewRecordList() {
      // Given
      NoticeViewRecordQueryForm queryForm = new NoticeViewRecordQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setNoticeId(1L);

      NoticeViewRecordVO recordVO = new NoticeViewRecordVO();
      recordVO.setEmployeeId(1L);

      when(noticeDao.queryNoticeViewRecordList(any(), any()))
          .thenReturn(Collections.singletonList(recordVO));

      // When
      PageResult<NoticeViewRecordVO> result = noticeEmployeeService.queryViewRecord(queryForm);

      // Then
      assertThat(result.getList()).hasSize(1);
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeEntity createTestEmployee(Long id, Boolean adminFlag, Long departmentId) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setAdministratorFlag(adminFlag);
    entity.setDepartmentId(departmentId);
    entity.setActualName("員工" + id);
    return entity;
  }

  private NoticeEmployeeVO createTestNoticeEmployeeVO(Long id, String title) {
    NoticeEmployeeVO vo = new NoticeEmployeeVO();
    vo.setNoticeId(id);
    vo.setTitle(title);
    vo.setPublishTime(LocalDateTime.now().minusDays(1));
    return vo;
  }

  private NoticeEntity createTestNoticeEntity(Long id, String title, Boolean allVisible) {
    NoticeEntity entity = new NoticeEntity();
    entity.setNoticeId(id);
    entity.setTitle(title);
    entity.setAllVisibleFlag(allVisible);
    entity.setDeletedFlag(false);
    entity.setPageViewCount(0);
    entity.setUserViewCount(0);
    return entity;
  }

  private NoticeVisibleRangeVO createVisibleRangeVO(Integer dataType, Long dataId) {
    NoticeVisibleRangeVO vo = new NoticeVisibleRangeVO();
    vo.setDataType(dataType);
    vo.setDataId(dataId);
    return vo;
  }
}
