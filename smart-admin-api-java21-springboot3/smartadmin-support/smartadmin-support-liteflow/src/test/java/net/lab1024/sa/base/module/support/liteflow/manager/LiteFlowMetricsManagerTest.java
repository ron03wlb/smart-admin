package net.lab1024.sa.support.liteflow.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.lab1024.sa.support.liteflow.dao.LiteFlowExecutionMetricsDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowExecutionMetricsEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowMetricsManager 單元測試
 *
 * <p>測試指標聚合邏輯的正確性
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowMetricsManager 單元測試")
class LiteFlowMetricsManagerTest {

  @Mock private LiteFlowExecutionMetricsDao metricsDao;

  @InjectMocks private LiteFlowMetricsManager metricsManager;

  private static final String TEST_CHAIN_CODE = "test-chain";

  @BeforeEach
  void setUp() {
    reset(metricsDao);
  }

  @Test
  @DisplayName("更新指標 - 首次執行（創建新記錄）")
  void testUpdateMetrics_FirstExecution_Success() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 100L;
    boolean success = true;
    LocalDate today = LocalDate.now();

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(metricsDao.insert(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, success);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).selectOne(any(LambdaQueryWrapper.class));
    verify(metricsDao, times(1)).insert(captor.capture());

    LiteFlowExecutionMetricsEntity inserted = captor.getValue();
    assertEquals(chainCode, inserted.getChainCode());
    assertEquals(today, inserted.getMetricDate());
    assertEquals(1, inserted.getTotalCount());
    assertEquals(1, inserted.getSuccessCount());
    assertEquals(0, inserted.getFailureCount());
    assertEquals(100, inserted.getAvgExecutionTime());
    assertEquals(100, inserted.getMaxExecutionTime());
    assertEquals(100, inserted.getMinExecutionTime());
    assertNotNull(inserted.getCreateTime());
  }

  @Test
  @DisplayName("更新指標 - 首次執行失敗")
  void testUpdateMetrics_FirstExecution_Failure() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 150L;
    boolean success = false;

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(metricsDao.insert(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, success);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).insert(captor.capture());

    LiteFlowExecutionMetricsEntity inserted = captor.getValue();
    assertEquals(1, inserted.getTotalCount());
    assertEquals(0, inserted.getSuccessCount(), "成功數應為 0");
    assertEquals(1, inserted.getFailureCount(), "失敗數應為 1");
  }

  @Test
  @DisplayName("更新指標 - 增量更新（成功）")
  void testUpdateMetrics_IncrementalUpdate_Success() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 200L;
    boolean success = true;

    LiteFlowExecutionMetricsEntity existingMetrics = new LiteFlowExecutionMetricsEntity();
    existingMetrics.setMetricId(1L);
    existingMetrics.setChainCode(chainCode);
    existingMetrics.setMetricDate(LocalDate.now());
    existingMetrics.setTotalCount(5);
    existingMetrics.setSuccessCount(4);
    existingMetrics.setFailureCount(1);
    existingMetrics.setAvgExecutionTime(100);
    existingMetrics.setMaxExecutionTime(150);
    existingMetrics.setMinExecutionTime(50);
    existingMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, success);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).selectOne(any(LambdaQueryWrapper.class));
    verify(metricsDao, times(1)).updateById(captor.capture());

    LiteFlowExecutionMetricsEntity updated = captor.getValue();
    assertEquals(6, updated.getTotalCount(), "總數應遞增為 6");
    assertEquals(5, updated.getSuccessCount(), "成功數應遞增為 5");
    assertEquals(1, updated.getFailureCount(), "失敗數保持不變");

    // 驗證平均值計算：(100 * 5 + 200) / 6 = 116
    int expectedAvg = (100 * 5 + 200) / 6;
    assertEquals(expectedAvg, updated.getAvgExecutionTime(), "平均時間應正確計算");

    assertEquals(200, updated.getMaxExecutionTime(), "最大時間應更新為 200");
    assertEquals(50, updated.getMinExecutionTime(), "最小時間保持不變");
    assertNotNull(updated.getUpdateTime());
  }

  @Test
  @DisplayName("更新指標 - 增量更新（失敗）")
  void testUpdateMetrics_IncrementalUpdate_Failure() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 120L;
    boolean success = false;

    LiteFlowExecutionMetricsEntity existingMetrics = new LiteFlowExecutionMetricsEntity();
    existingMetrics.setMetricId(1L);
    existingMetrics.setChainCode(chainCode);
    existingMetrics.setMetricDate(LocalDate.now());
    existingMetrics.setTotalCount(10);
    existingMetrics.setSuccessCount(9);
    existingMetrics.setFailureCount(1);
    existingMetrics.setAvgExecutionTime(100);
    existingMetrics.setMaxExecutionTime(150);
    existingMetrics.setMinExecutionTime(50);
    existingMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, success);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).updateById(captor.capture());

    LiteFlowExecutionMetricsEntity updated = captor.getValue();
    assertEquals(11, updated.getTotalCount());
    assertEquals(9, updated.getSuccessCount(), "成功數保持不變");
    assertEquals(2, updated.getFailureCount(), "失敗數應遞增為 2");
  }

  @Test
  @DisplayName("更新指標 - 驗證最小執行時間更新")
  void testUpdateMetrics_MinExecutionTimeUpdate() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 30L; // 比現有最小值還小

    LiteFlowExecutionMetricsEntity existingMetrics = new LiteFlowExecutionMetricsEntity();
    existingMetrics.setMetricId(1L);
    existingMetrics.setChainCode(chainCode);
    existingMetrics.setMetricDate(LocalDate.now());
    existingMetrics.setTotalCount(5);
    existingMetrics.setSuccessCount(5);
    existingMetrics.setFailureCount(0);
    existingMetrics.setAvgExecutionTime(100);
    existingMetrics.setMaxExecutionTime(150);
    existingMetrics.setMinExecutionTime(80); // 現有最小值
    existingMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, true);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).updateById(captor.capture());

    LiteFlowExecutionMetricsEntity updated = captor.getValue();
    assertEquals(30, updated.getMinExecutionTime(), "最小時間應更新為 30");
    assertEquals(150, updated.getMaxExecutionTime(), "最大時間保持不變");
  }

  @Test
  @DisplayName("更新指標 - 驗證最大執行時間保持不變")
  void testUpdateMetrics_MaxExecutionTimeUnchanged() {
    // Given
    String chainCode = TEST_CHAIN_CODE;
    long executionTime = 120L; // 比現有最大值小

    LiteFlowExecutionMetricsEntity existingMetrics = new LiteFlowExecutionMetricsEntity();
    existingMetrics.setMetricId(1L);
    existingMetrics.setChainCode(chainCode);
    existingMetrics.setMetricDate(LocalDate.now());
    existingMetrics.setTotalCount(5);
    existingMetrics.setSuccessCount(5);
    existingMetrics.setFailureCount(0);
    existingMetrics.setAvgExecutionTime(100);
    existingMetrics.setMaxExecutionTime(200);
    existingMetrics.setMinExecutionTime(50);
    existingMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, executionTime, true);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).updateById(captor.capture());

    LiteFlowExecutionMetricsEntity updated = captor.getValue();
    assertEquals(200, updated.getMaxExecutionTime(), "最大時間應保持不變");
    assertEquals(50, updated.getMinExecutionTime(), "最小時間應保持不變");
  }

  @Test
  @DisplayName("更新指標 - 驗證平均時間計算精度")
  void testUpdateMetrics_AverageCalculationAccuracy() {
    // Given
    String chainCode = TEST_CHAIN_CODE;

    LiteFlowExecutionMetricsEntity existingMetrics = new LiteFlowExecutionMetricsEntity();
    existingMetrics.setMetricId(1L);
    existingMetrics.setChainCode(chainCode);
    existingMetrics.setMetricDate(LocalDate.now());
    existingMetrics.setTotalCount(3);
    existingMetrics.setSuccessCount(3);
    existingMetrics.setFailureCount(0);
    existingMetrics.setAvgExecutionTime(100); // (100 + 100 + 100) / 3 = 100
    existingMetrics.setMaxExecutionTime(100);
    existingMetrics.setMinExecutionTime(100);
    existingMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When
    metricsManager.updateMetrics(chainCode, 200L, true);

    // Then
    ArgumentCaptor<LiteFlowExecutionMetricsEntity> captor =
        ArgumentCaptor.forClass(LiteFlowExecutionMetricsEntity.class);
    verify(metricsDao, times(1)).updateById(captor.capture());

    LiteFlowExecutionMetricsEntity updated = captor.getValue();

    // 驗證平均值：(100 * 3 + 200) / 4 = 500 / 4 = 125
    assertEquals(125, updated.getAvgExecutionTime(), "平均時間應為 125");
    assertEquals(4, updated.getTotalCount());
  }

  @Test
  @DisplayName("更新指標 - 併發場景（多次更新）")
  void testUpdateMetrics_ConcurrentUpdates() {
    // Given
    String chainCode = TEST_CHAIN_CODE;

    LiteFlowExecutionMetricsEntity initialMetrics = new LiteFlowExecutionMetricsEntity();
    initialMetrics.setMetricId(1L);
    initialMetrics.setChainCode(chainCode);
    initialMetrics.setMetricDate(LocalDate.now());
    initialMetrics.setTotalCount(0);
    initialMetrics.setSuccessCount(0);
    initialMetrics.setFailureCount(0);
    initialMetrics.setAvgExecutionTime(0);
    initialMetrics.setMaxExecutionTime(0);
    initialMetrics.setMinExecutionTime(Integer.MAX_VALUE);
    initialMetrics.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    when(metricsDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(initialMetrics);
    when(metricsDao.updateById(any(LiteFlowExecutionMetricsEntity.class))).thenReturn(1);

    // When - 模擬多次執行
    metricsManager.updateMetrics(chainCode, 100L, true);
    metricsManager.updateMetrics(chainCode, 200L, true);
    metricsManager.updateMetrics(chainCode, 150L, false);

    // Then
    verify(metricsDao, times(3)).selectOne(any(LambdaQueryWrapper.class));
    verify(metricsDao, times(3)).updateById(any(LiteFlowExecutionMetricsEntity.class));
  }
}
