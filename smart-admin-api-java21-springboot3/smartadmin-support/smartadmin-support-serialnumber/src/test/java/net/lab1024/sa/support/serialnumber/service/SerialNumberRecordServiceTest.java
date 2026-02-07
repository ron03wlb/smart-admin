package net.lab1024.sa.support.serialnumber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.support.serialnumber.dao.SerialNumberRecordDao;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberRecordEntity;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberRecordQueryForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SerialNumberRecordService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 分頁查詢序列號記錄
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SerialNumberRecordService 單元測試")
class SerialNumberRecordServiceTest {

  @Mock private SerialNumberRecordDao serialNumberRecordDao;

  @InjectMocks private SerialNumberRecordService serialNumberRecordService;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      SerialNumberRecordQueryForm form = new SerialNumberRecordQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      SerialNumberRecordEntity entity = createTestEntity();
      when(serialNumberRecordDao.query(any(Page.class), any(SerialNumberRecordQueryForm.class)))
          .thenReturn(List.of(entity));

      // When
      PageResult<SerialNumberRecordEntity> result = serialNumberRecordService.query(form);

      // Then
      assertThat(result).isNotNull();
      verify(serialNumberRecordDao).query(any(Page.class), any(SerialNumberRecordQueryForm.class));
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      SerialNumberRecordQueryForm form = new SerialNumberRecordQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(serialNumberRecordDao.query(any(Page.class), any(SerialNumberRecordQueryForm.class)))
          .thenReturn(List.of());

      // When
      PageResult<SerialNumberRecordEntity> result = serialNumberRecordService.query(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== Helper Methods ====================

  private SerialNumberRecordEntity createTestEntity() {
    SerialNumberRecordEntity entity = new SerialNumberRecordEntity();
    entity.setSerialNumberId(1);
    entity.setRecordDate(LocalDate.of(2026, 2, 7));
    entity.setLastNumber(100L);
    entity.setCount(10L);
    return entity;
  }
}
