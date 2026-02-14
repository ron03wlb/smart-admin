package net.lab1024.sa.support.operatelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.operatelog.domain.OperateLogEntity;
import net.lab1024.sa.support.operatelog.domain.OperateLogQueryForm;
import net.lab1024.sa.support.operatelog.domain.OperateLogVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * OperateLogService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryByPage - 分頁查詢操作日誌
 *   <li>detail - 查詢操作日誌詳情
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperateLogService 單元測試")
class OperateLogServiceTest {

  @Mock private OperateLogDao operateLogDao;

  @InjectMocks private OperateLogService operateLogService;

  // ==================== queryByPage 測試 ====================

  @Nested
  @DisplayName("queryByPage 分頁查詢測試")
  class QueryByPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      OperateLogQueryForm form = new OperateLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      OperateLogEntity entity = createTestOperateLogEntity();
      when(operateLogDao.queryByPage(any(Page.class), any(OperateLogQueryForm.class)))
          .thenReturn(List.of(entity));

      // When
      ResponseDTO<PageResult<OperateLogVO>> result = operateLogService.queryByPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      OperateLogQueryForm form = new OperateLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(operateLogDao.queryByPage(any(Page.class), any(OperateLogQueryForm.class)))
          .thenReturn(List.of());

      // When
      ResponseDTO<PageResult<OperateLogVO>> result = operateLogService.queryByPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }
  }

  // ==================== detail 測試 ====================

  @Nested
  @DisplayName("detail 查詢詳情測試")
  class DetailTest {

    @Test
    @DisplayName("正常情況：應該返回操作日誌詳情")
    void shouldReturnOperateLogDetail() {
      // Given
      Long operateLogId = 1L;
      OperateLogEntity entity = createTestOperateLogEntity();
      when(operateLogDao.selectById(operateLogId)).thenReturn(entity);

      // When
      ResponseDTO<OperateLogVO> result = operateLogService.detail(operateLogId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getOperateLogId()).isEqualTo(operateLogId);
    }

    @Test
    @DisplayName("數據不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      Long operateLogId = 999L;
      when(operateLogDao.selectById(operateLogId)).thenReturn(null);

      // When
      ResponseDTO<OperateLogVO> result = operateLogService.detail(operateLogId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Helper Methods ====================

  private OperateLogEntity createTestOperateLogEntity() {
    OperateLogEntity entity =
        OperateLogEntity.builder()
            .operateLogId(1L)
            .operateUserId(1L)
            .operateUserType(1)
            .operateUserName("admin")
            .module("系統管理")
            .content("查詢用戶列表")
            .url("/api/system/employee/list")
            .method("GET")
            .param("{}")
            .response("{\"code\":0}")
            .ip("127.0.0.1")
            .ipRegion("本機")
            .userAgent("Chrome")
            .successFlag(1)
            .build();
    entity.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return entity;
  }
}
