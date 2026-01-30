package net.lab1024.sa.admin.module.business.oa.notice.manager;

import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.oa.notice.dao.NoticeDao;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeVisibleRangeForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * NoticeManager Unit Tests
 *
 * <p>Test Coverage: 2 @Transactional methods (save, update)
 *
 * <p>Focus Areas: 1. Transaction pattern without DataTracer 2. Insert + insertVisibleRange pattern
 * 3. Update + deleteVisibleRange + insertVisibleRange pattern 4. Empty list handling
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("NoticeManager Unit Tests")
class NoticeManagerTest extends BaseUnitTest {

  @Mock private NoticeDao noticeDao;

  private NoticeManager noticeManager;

  // Test constants
  private static final Long TEST_NOTICE_ID = 1001L;
  private static final String TEST_NOTICE_TITLE = "測試公告";

  @BeforeEach
  void setUp() {
    noticeManager = new NoticeManager(noticeDao);
  }

  // ==================== saveTransaction() Tests ====================

  @Nested
  @DisplayName("saveTransaction() Tests - Insert + InsertVisibleRange Pattern")
  class SaveTransactionTests {

    @Test
    @DisplayName("Should insert notice and insert visible range")
    void saveNotice_ValidEntity_InsertsNoticeAndVisibleRange() {
      // Given
      NoticeEntity noticeEntity = new NoticeEntity();
      noticeEntity.setTitle(TEST_NOTICE_TITLE);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm1 = new NoticeVisibleRangeForm();
      rangeForm1.setDataType(1);
      rangeForm1.setDataId(1001L);
      visibleRangeList.add(rangeForm1);

      when(noticeDao.insert(any(NoticeEntity.class)))
          .thenAnswer(
              invocation -> {
                NoticeEntity entity = invocation.getArgument(0);
                entity.setNoticeId(TEST_NOTICE_ID);
                return 1;
              });
      doNothing().when(noticeDao).insertVisibleRange(eq(TEST_NOTICE_ID), eq(visibleRangeList));

      // When
      noticeManager.saveTransaction(noticeEntity, visibleRangeList);

      // Then
      verify(noticeDao, times(1)).insert(any(NoticeEntity.class));
      verify(noticeDao, times(1)).insertVisibleRange(eq(TEST_NOTICE_ID), eq(visibleRangeList));
    }

    @Test
    @DisplayName("Should insert before insertVisibleRange (transaction order)")
    void saveNotice_TransactionOrder_InsertBeforeVisibleRange() {
      // Given
      NoticeEntity noticeEntity = new NoticeEntity();
      noticeEntity.setTitle(TEST_NOTICE_TITLE);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm = new NoticeVisibleRangeForm();
      rangeForm.setDataType(1);
      visibleRangeList.add(rangeForm);

      when(noticeDao.insert(any(NoticeEntity.class)))
          .thenAnswer(
              invocation -> {
                NoticeEntity entity = invocation.getArgument(0);
                entity.setNoticeId(TEST_NOTICE_ID);
                return 1;
              });
      doNothing().when(noticeDao).insertVisibleRange(anyLong(), anyList());

      // When
      noticeManager.saveTransaction(noticeEntity, visibleRangeList);

      // Then - verify insert called before insertVisibleRange
      var inOrder = inOrder(noticeDao);
      inOrder.verify(noticeDao).insert(any(NoticeEntity.class));
      inOrder.verify(noticeDao).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should handle empty visible range list")
    void saveNotice_EmptyVisibleRange_InsertsNoticeOnly() {
      // Given
      NoticeEntity noticeEntity = new NoticeEntity();
      noticeEntity.setTitle(TEST_NOTICE_TITLE);

      List<NoticeVisibleRangeForm> emptyList = Collections.emptyList();

      when(noticeDao.insert(any(NoticeEntity.class)))
          .thenAnswer(
              invocation -> {
                NoticeEntity entity = invocation.getArgument(0);
                entity.setNoticeId(TEST_NOTICE_ID);
                return 1;
              });

      // When
      noticeManager.saveTransaction(noticeEntity, emptyList);

      // Then - only insert notice, do not insertVisibleRange
      verify(noticeDao, times(1)).insert(any(NoticeEntity.class));
      verify(noticeDao, never()).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should handle null visible range list")
    void saveNotice_NullVisibleRange_InsertsNoticeOnly() {
      // Given
      NoticeEntity noticeEntity = new NoticeEntity();
      noticeEntity.setTitle(TEST_NOTICE_TITLE);

      when(noticeDao.insert(any(NoticeEntity.class)))
          .thenAnswer(
              invocation -> {
                NoticeEntity entity = invocation.getArgument(0);
                entity.setNoticeId(TEST_NOTICE_ID);
                return 1;
              });

      // When
      noticeManager.saveTransaction(noticeEntity, null);

      // Then - only insert notice, do not insertVisibleRange
      verify(noticeDao, times(1)).insert(any(NoticeEntity.class));
      verify(noticeDao, never()).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should use generated notice ID in insertVisibleRange")
    void saveNotice_GeneratedId_UsedInVisibleRange() {
      // Given
      Long generatedId = 9999L;
      NoticeEntity noticeEntity = new NoticeEntity();
      noticeEntity.setTitle(TEST_NOTICE_TITLE);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm = new NoticeVisibleRangeForm();
      rangeForm.setDataType(1);
      visibleRangeList.add(rangeForm);

      when(noticeDao.insert(any(NoticeEntity.class)))
          .thenAnswer(
              invocation -> {
                NoticeEntity entity = invocation.getArgument(0);
                entity.setNoticeId(generatedId);
                return 1;
              });
      doNothing().when(noticeDao).insertVisibleRange(anyLong(), anyList());

      // When
      noticeManager.saveTransaction(noticeEntity, visibleRangeList);

      // Then
      verify(noticeDao, times(1)).insertVisibleRange(eq(generatedId), eq(visibleRangeList));
    }
  }

  // ==================== updateTransaction() Tests ====================

  @Nested
  @DisplayName(
      "updateTransaction() Tests - Update + DeleteVisibleRange + InsertVisibleRange Pattern")
  class UpdateTransactionTests {

    @Test
    @DisplayName("Should update notice and replace visible range")
    void updateNotice_ValidEntity_UpdatesAndReplacesVisibleRange() {
      // Given
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(TEST_NOTICE_ID);
      oldEntity.setTitle(TEST_NOTICE_TITLE);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(TEST_NOTICE_ID);
      newEntity.setTitle("Updated Notice Title");

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm = new NoticeVisibleRangeForm();
      rangeForm.setDataType(1);
      visibleRangeList.add(rangeForm);

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);
      doNothing().when(noticeDao).deleteVisibleRange(eq(TEST_NOTICE_ID));
      doNothing().when(noticeDao).insertVisibleRange(eq(TEST_NOTICE_ID), eq(visibleRangeList));

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then
      verify(noticeDao, times(1)).updateById(any(NoticeEntity.class));
      verify(noticeDao, times(1)).deleteVisibleRange(eq(TEST_NOTICE_ID));
      verify(noticeDao, times(1)).insertVisibleRange(eq(TEST_NOTICE_ID), eq(visibleRangeList));
    }

    @Test
    @DisplayName(
        "Should execute in correct order: update → deleteVisibleRange → insertVisibleRange")
    void updateNotice_TransactionOrder_CorrectSequence() {
      // Given
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(TEST_NOTICE_ID);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(TEST_NOTICE_ID);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm = new NoticeVisibleRangeForm();
      visibleRangeList.add(rangeForm);

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);
      doNothing().when(noticeDao).deleteVisibleRange(anyLong());
      doNothing().when(noticeDao).insertVisibleRange(anyLong(), anyList());

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then - verify execution order
      var inOrder = inOrder(noticeDao);
      inOrder.verify(noticeDao).updateById(any(NoticeEntity.class));
      inOrder.verify(noticeDao).deleteVisibleRange(anyLong());
      inOrder.verify(noticeDao).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should handle empty visible range list")
    void updateNotice_EmptyVisibleRange_UpdatesOnly() {
      // Given
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(TEST_NOTICE_ID);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(TEST_NOTICE_ID);

      List<NoticeVisibleRangeForm> emptyList = Collections.emptyList();

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, emptyList);

      // Then - only update notice, do not delete or insert visible range
      verify(noticeDao, times(1)).updateById(any(NoticeEntity.class));
      verify(noticeDao, never()).deleteVisibleRange(anyLong());
      verify(noticeDao, never()).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should handle null visible range list")
    void updateNotice_NullVisibleRange_UpdatesOnly() {
      // Given
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(TEST_NOTICE_ID);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(TEST_NOTICE_ID);

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, null);

      // Then - only update notice, do not delete or insert visible range
      verify(noticeDao, times(1)).updateById(any(NoticeEntity.class));
      verify(noticeDao, never()).deleteVisibleRange(anyLong());
      verify(noticeDao, never()).insertVisibleRange(anyLong(), anyList());
    }

    @Test
    @DisplayName("Should use correct notice ID in all operations")
    void updateNotice_NoticeId_UsedInAllOperations() {
      // Given
      Long specificNoticeId = 8888L;
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(specificNoticeId);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(specificNoticeId);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm = new NoticeVisibleRangeForm();
      visibleRangeList.add(rangeForm);

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);
      doNothing().when(noticeDao).deleteVisibleRange(anyLong());
      doNothing().when(noticeDao).insertVisibleRange(anyLong(), anyList());

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then
      verify(noticeDao, times(1)).deleteVisibleRange(eq(specificNoticeId));
      verify(noticeDao, times(1)).insertVisibleRange(eq(specificNoticeId), eq(visibleRangeList));
    }

    @Test
    @DisplayName("Should handle multiple visible range forms")
    void updateNotice_MultipleVisibleRanges_HandlesCorrectly() {
      // Given
      NoticeEntity oldEntity = new NoticeEntity();
      oldEntity.setNoticeId(TEST_NOTICE_ID);

      NoticeEntity newEntity = new NoticeEntity();
      newEntity.setNoticeId(TEST_NOTICE_ID);

      List<NoticeVisibleRangeForm> visibleRangeList = Lists.newArrayList();
      NoticeVisibleRangeForm rangeForm1 = new NoticeVisibleRangeForm();
      rangeForm1.setDataType(1);
      rangeForm1.setDataId(1001L);
      NoticeVisibleRangeForm rangeForm2 = new NoticeVisibleRangeForm();
      rangeForm2.setDataType(2);
      rangeForm2.setDataId(2001L);
      NoticeVisibleRangeForm rangeForm3 = new NoticeVisibleRangeForm();
      rangeForm3.setDataType(1);
      rangeForm3.setDataId(1002L);
      visibleRangeList.add(rangeForm1);
      visibleRangeList.add(rangeForm2);
      visibleRangeList.add(rangeForm3);

      when(noticeDao.updateById(any(NoticeEntity.class))).thenReturn(1);
      doNothing().when(noticeDao).deleteVisibleRange(anyLong());
      doNothing().when(noticeDao).insertVisibleRange(anyLong(), anyList());

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then
      verify(noticeDao, times(1)).deleteVisibleRange(eq(TEST_NOTICE_ID));
      verify(noticeDao, times(1)).insertVisibleRange(eq(TEST_NOTICE_ID), eq(visibleRangeList));
    }
  }
}
