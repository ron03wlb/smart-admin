package net.lab1024.sa.support.datatracer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.datatracer.dao.DataTracerDao;
import net.lab1024.sa.support.datatracer.domain.form.DataTracerQueryForm;
import net.lab1024.sa.support.datatracer.domain.vo.DataTracerVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DataTracerService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 分頁查詢數據變動記錄
 * </ul>
 *
 * <p>注意: 其他方法 (addTrace, insert, update, delete, batchDelete) 依賴 SmartRequestUtil.getRequestUser()
 * 需要 Mock 靜態方法，這裡只測試 query 方法
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataTracerService 單元測試")
class DataTracerServiceTest {

  @Mock private DataTracerDao dataTracerDao;

  @InjectMocks private DataTracerService dataTracerService;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      DataTracerQueryForm form = new DataTracerQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      DataTracerVO vo = createTestDataTracerVO();
      when(dataTracerDao.query(any(Page.class), any(DataTracerQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<DataTracerVO>> result = dataTracerService.query(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      DataTracerQueryForm form = new DataTracerQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(dataTracerDao.query(any(Page.class), any(DataTracerQueryForm.class)))
          .thenReturn(List.of());

      // When
      ResponseDTO<PageResult<DataTracerVO>> result = dataTracerService.query(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== Helper Methods ====================

  private DataTracerVO createTestDataTracerVO() {
    DataTracerVO vo = new DataTracerVO();
    vo.setDataTracerId(1L);
    vo.setDataId(100L);
    vo.setType(1);
    vo.setContent("變更內容");
    vo.setUserId(1L);
    vo.setUserName("admin");
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }
}
