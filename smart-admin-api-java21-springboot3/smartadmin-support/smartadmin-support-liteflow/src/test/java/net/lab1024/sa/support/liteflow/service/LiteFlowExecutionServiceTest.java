package net.lab1024.sa.support.liteflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yomahub.liteflow.flow.LiteflowResponse;
import java.util.List;
import java.util.Map;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.constant.LiteFlowConst;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.support.liteflow.core.listener.LiteFlowExecutionListener;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.dao.LiteFlowExecutionLogDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowExecutionLogEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowExecutionForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowExecutionLogQueryForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowExecutionLogVO;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowExecutionResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowExecutionService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>execute - 執行流程
 *   <li>queryLog - 查詢執行日誌
 *   <li>getLogDetail - 獲取日誌詳情
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowExecutionService 單元測試")
class LiteFlowExecutionServiceTest {

  @Mock private SmartFlowExecutor flowExecutor;

  @Mock private LiteFlowChainDao chainDao;

  @Mock private LiteFlowExecutionLogDao logDao;

  @Mock private LiteFlowExecutionListener executionListener;

  @InjectMocks private LiteFlowExecutionService executionService;

  // ==================== execute 測試 ====================

  @Nested
  @DisplayName("execute 執行流程測試")
  class ExecuteTest {

    @Test
    @DisplayName("流程不存在時：應該返回錯誤")
    void shouldReturnErrorWhenChainNotExists() {
      // Given
      LiteFlowExecutionForm form = new LiteFlowExecutionForm();
      form.setChainCode("non-existent-chain");
      form.setInputParams(Map.of("key", "value"));

      when(chainDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      // When
      ResponseDTO<LiteFlowExecutionResultVO> result = executionService.execute(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(flowExecutor, never()).execute(anyString(), any());
    }

    @Test
    @DisplayName("執行成功時：應該返回成功結果")
    void shouldReturnSuccessResultWhenExecutionSucceeds() {
      // Given
      LiteFlowExecutionForm form = new LiteFlowExecutionForm();
      form.setChainCode("test-chain");
      form.setInputParams(Map.of("key", "value"));

      LiteFlowChainEntity chainEntity = createTestChainEntity();
      when(chainDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chainEntity);

      LiteflowResponse mockResponse = mock(LiteflowResponse.class);
      when(mockResponse.isSuccess()).thenReturn(true);
      when(flowExecutor.execute(eq("test-chain"), any())).thenReturn(mockResponse);

      doNothing().when(executionListener).beforeExecution(anyString(), any());
      doNothing().when(executionListener).afterExecution(anyString(), any());

      // When
      ResponseDTO<LiteFlowExecutionResultVO> result = executionService.execute(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getSuccess()).isTrue();
      assertThat(result.getData().getChainCode()).isEqualTo("test-chain");
      verify(executionListener).beforeExecution(eq("test-chain"), any());
      verify(executionListener).afterExecution(eq("test-chain"), any());
    }

    @Test
    @DisplayName("執行失敗時：應該返回失敗結果")
    void shouldReturnFailureResultWhenExecutionFails() {
      // Given
      LiteFlowExecutionForm form = new LiteFlowExecutionForm();
      form.setChainCode("test-chain");
      form.setInputParams(Map.of("key", "value"));

      LiteFlowChainEntity chainEntity = createTestChainEntity();
      when(chainDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chainEntity);

      LiteflowResponse mockResponse = mock(LiteflowResponse.class);
      when(mockResponse.isSuccess()).thenReturn(false);
      when(mockResponse.getCause()).thenReturn(new RuntimeException("Execution failed"));
      when(flowExecutor.execute(eq("test-chain"), any())).thenReturn(mockResponse);

      doNothing().when(executionListener).beforeExecution(anyString(), any());
      doNothing().when(executionListener).afterExecution(anyString(), any());

      // When
      ResponseDTO<LiteFlowExecutionResultVO> result = executionService.execute(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getSuccess()).isFalse();
      assertThat(result.getData().getErrorMessage()).contains("Execution failed");
    }

    @Test
    @DisplayName("執行異常時：應該返回錯誤")
    void shouldReturnErrorWhenExceptionOccurs() {
      // Given
      LiteFlowExecutionForm form = new LiteFlowExecutionForm();
      form.setChainCode("test-chain");
      form.setInputParams(Map.of("key", "value"));

      LiteFlowChainEntity chainEntity = createTestChainEntity();
      when(chainDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(chainEntity);

      doNothing().when(executionListener).beforeExecution(anyString(), any());
      when(flowExecutor.execute(eq("test-chain"), any()))
          .thenThrow(new RuntimeException("Unexpected error"));

      // When
      ResponseDTO<LiteFlowExecutionResultVO> result = executionService.execute(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryLog 測試 ====================

  @Nested
  @DisplayName("queryLog 查詢日誌測試")
  class QueryLogTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      LiteFlowExecutionLogQueryForm form = new LiteFlowExecutionLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      LiteFlowExecutionLogEntity entity = createTestLogEntity(1L);
      Page<LiteFlowExecutionLogEntity> page = new Page<>(1, 10);
      page.setRecords(List.of(entity));
      page.setTotal(1);

      when(logDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowExecutionLogVO>> result = executionService.queryLog(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("帶查詢條件：應該正確過濾")
    void shouldFilterByConditions() {
      // Given
      LiteFlowExecutionLogQueryForm form = new LiteFlowExecutionLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      form.setChainCode("test-chain");
      form.setExecutionStatus(1);
      form.setRequestId("req-123");

      Page<LiteFlowExecutionLogEntity> page = new Page<>(1, 10);
      page.setRecords(List.of());
      page.setTotal(0);

      when(logDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowExecutionLogVO>> result = executionService.queryLog(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(logDao).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
  }

  // ==================== getLogDetail 測試 ====================

  @Nested
  @DisplayName("getLogDetail 獲取日誌詳情測試")
  class GetLogDetailTest {

    @Test
    @DisplayName("正常情況：應該返回日誌詳情")
    void shouldReturnLogDetail() {
      // Given
      Long logId = 1L;
      LiteFlowExecutionLogEntity entity = createTestLogEntity(logId);

      when(logDao.selectById(logId)).thenReturn(entity);

      // When
      ResponseDTO<LiteFlowExecutionLogVO> result = executionService.getLogDetail(logId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("日誌不存在時：應該返回錯誤")
    void shouldReturnErrorWhenLogNotExists() {
      // Given
      Long logId = 999L;
      when(logDao.selectById(logId)).thenReturn(null);

      // When
      ResponseDTO<LiteFlowExecutionLogVO> result = executionService.getLogDetail(logId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Helper Methods ====================

  private LiteFlowChainEntity createTestChainEntity() {
    LiteFlowChainEntity entity = new LiteFlowChainEntity();
    entity.setChainId(1L);
    entity.setChainCode("test-chain");
    entity.setChainName("Test Chain");
    entity.setStatus(LiteFlowConst.STATUS_ENABLED);
    entity.setDeletedFlag(0);
    return entity;
  }

  private LiteFlowExecutionLogEntity createTestLogEntity(Long logId) {
    LiteFlowExecutionLogEntity entity = new LiteFlowExecutionLogEntity();
    entity.setLogId(logId);
    entity.setChainCode("test-chain");
    entity.setExecutionStatus(1);
    entity.setRequestId("req-123");
    return entity;
  }
}
