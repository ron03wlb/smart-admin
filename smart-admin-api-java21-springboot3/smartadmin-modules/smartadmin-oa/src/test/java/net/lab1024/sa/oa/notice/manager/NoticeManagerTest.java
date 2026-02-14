package net.lab1024.sa.oa.notice.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.oa.notice.dao.NoticeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.oa.notice.domain.form.NoticeVisibleRangeForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NoticeManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>保存通知事務（含可見範圍）
 *   <li>更新通知事務（含可見範圍）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeManager 單元測試")
class NoticeManagerTest {

  @Mock private NoticeDao noticeDao;

  @InjectMocks private NoticeManager noticeManager;

  // ==================== saveTransaction 測試 ====================

  @Nested
  @DisplayName("saveTransaction 保存通知事務測試")
  class SaveTransactionTest {

    @Test
    @DisplayName("正常情況：有可見範圍時應該保存通知和可見範圍")
    void shouldSaveNoticeAndVisibleRange() {
      // Given
      NoticeEntity noticeEntity = createTestNoticeEntity(null, "測試通知");
      NoticeVisibleRangeForm visibleRange = createTestVisibleRangeForm(1, 1L);
      List<NoticeVisibleRangeForm> visibleRangeList = Collections.singletonList(visibleRange);

      // When
      noticeManager.saveTransaction(noticeEntity, visibleRangeList);

      // Then
      verify(noticeDao).insert(noticeEntity);
      verify(noticeDao).insertVisibleRange(any(), eq(visibleRangeList));
    }

    @Test
    @DisplayName("邊界情況：無可見範圍時只保存通知")
    void shouldOnlySaveNoticeWhenNoVisibleRange() {
      // Given
      NoticeEntity noticeEntity = createTestNoticeEntity(null, "測試通知");
      List<NoticeVisibleRangeForm> visibleRangeList = Collections.emptyList();

      // When
      noticeManager.saveTransaction(noticeEntity, visibleRangeList);

      // Then
      verify(noticeDao).insert(noticeEntity);
      verify(noticeDao, never()).insertVisibleRange(any(), anyList());
    }
  }

  // ==================== updateTransaction 測試 ====================

  @Nested
  @DisplayName("updateTransaction 更新通知事務測試")
  class UpdateTransactionTest {

    @Test
    @DisplayName("正常情況：有可見範圍時應該更新通知並重建可見範圍")
    void shouldUpdateNoticeAndRebuildVisibleRange() {
      // Given
      NoticeEntity oldEntity = createTestNoticeEntity(1L, "舊通知");
      NoticeEntity newEntity = createTestNoticeEntity(1L, "新通知");
      NoticeVisibleRangeForm visibleRange = createTestVisibleRangeForm(1, 1L);
      List<NoticeVisibleRangeForm> visibleRangeList = Collections.singletonList(visibleRange);

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then
      verify(noticeDao).updateById(newEntity);
      verify(noticeDao).deleteVisibleRange(1L);
      verify(noticeDao).insertVisibleRange(1L, visibleRangeList);
    }

    @Test
    @DisplayName("邊界情況：無可見範圍時只更新通知")
    void shouldOnlyUpdateNoticeWhenNoVisibleRange() {
      // Given
      NoticeEntity oldEntity = createTestNoticeEntity(1L, "舊通知");
      NoticeEntity newEntity = createTestNoticeEntity(1L, "新通知");
      List<NoticeVisibleRangeForm> visibleRangeList = Collections.emptyList();

      // When
      noticeManager.updateTransaction(oldEntity, newEntity, visibleRangeList);

      // Then
      verify(noticeDao).updateById(newEntity);
      verify(noticeDao, never()).deleteVisibleRange(any());
      verify(noticeDao, never()).insertVisibleRange(any(), anyList());
    }
  }

  // ==================== Helper Methods ====================

  private NoticeEntity createTestNoticeEntity(Long id, String title) {
    NoticeEntity entity = new NoticeEntity();
    entity.setNoticeId(id);
    entity.setTitle(title);
    return entity;
  }

  private NoticeVisibleRangeForm createTestVisibleRangeForm(Integer dataType, Long dataId) {
    NoticeVisibleRangeForm form = new NoticeVisibleRangeForm();
    form.setDataType(dataType);
    form.setDataId(dataId);
    return form;
  }
}
