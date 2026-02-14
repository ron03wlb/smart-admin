package net.lab1024.sa.support.liteflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.dao.LiteFlowExecutionMetricsDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowExecutionMetricsEntity;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowMonitorOverviewVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowMonitorService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getOverview - 獲取監控概覽數據
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowMonitorService 單元測試")
class LiteFlowMonitorServiceTest {

  @Mock private LiteFlowChainDao chainDao;

  @Mock private LiteFlowExecutionMetricsDao metricsDao;

  @InjectMocks private LiteFlowMonitorService monitorService;

  // ==================== getOverview 測試 ====================

  @Nested
  @DisplayName("getOverview 獲取監控概覽測試")
  class GetOverviewTest {

    @Test
    @DisplayName("有執行數據時：應該正確計算成功率")
    void shouldCalculateSuccessRateWithData() {
      // Given
      when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

      LiteFlowExecutionMetricsEntity metrics1 = createTestMetrics(100, 80);
      LiteFlowExecutionMetricsEntity metrics2 = createTestMetrics(50, 45);

      when(metricsDao.selectList(any(LambdaQueryWrapper.class)))
          .thenReturn(List.of(metrics1, metrics2));

      // When
      ResponseDTO<LiteFlowMonitorOverviewVO> result = monitorService.getOverview();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getTotalChains()).isEqualTo(10);
      assertThat(result.getData().getTodayExecutions()).isEqualTo(150); // 100 + 50
      // 成功率: (80 + 45) / 150 * 100 = 83.33%
      assertThat(result.getData().getTodaySuccessRate()).isGreaterThan(80.0);
    }

    @Test
    @DisplayName("無執行數據時：應該返回100%成功率")
    void shouldReturn100PercentWhenNoData() {
      // Given
      when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
      when(metricsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

      // When
      ResponseDTO<LiteFlowMonitorOverviewVO> result = monitorService.getOverview();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getTotalChains()).isEqualTo(5);
      assertThat(result.getData().getTodayExecutions()).isEqualTo(0);
      assertThat(result.getData().getTodaySuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("無流程時：應該返回0個流程")
    void shouldReturnZeroChainsWhenNoChains() {
      // Given
      when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
      when(metricsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

      // When
      ResponseDTO<LiteFlowMonitorOverviewVO> result = monitorService.getOverview();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getTotalChains()).isEqualTo(0);
    }

    @Test
    @DisplayName("全部成功時：應該返回100%成功率")
    void shouldReturn100PercentWhenAllSuccess() {
      // Given
      when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

      LiteFlowExecutionMetricsEntity metrics = createTestMetrics(100, 100);
      when(metricsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metrics));

      // When
      ResponseDTO<LiteFlowMonitorOverviewVO> result = monitorService.getOverview();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTodaySuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("全部失敗時：應該返回0%成功率")
    void shouldReturn0PercentWhenAllFailed() {
      // Given
      when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

      LiteFlowExecutionMetricsEntity metrics = createTestMetrics(100, 0);
      when(metricsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(metrics));

      // When
      ResponseDTO<LiteFlowMonitorOverviewVO> result = monitorService.getOverview();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTodaySuccessRate()).isEqualTo(0.0);
    }
  }

  // ==================== Helper Methods ====================

  private LiteFlowExecutionMetricsEntity createTestMetrics(int totalCount, int successCount) {
    LiteFlowExecutionMetricsEntity entity = new LiteFlowExecutionMetricsEntity();
    entity.setTotalCount(totalCount);
    entity.setSuccessCount(successCount);
    entity.setMetricDate(LocalDate.now());
    return entity;
  }
}
