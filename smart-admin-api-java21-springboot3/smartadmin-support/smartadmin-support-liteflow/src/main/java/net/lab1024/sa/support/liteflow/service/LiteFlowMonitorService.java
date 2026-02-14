package net.lab1024.sa.support.liteflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.dao.LiteFlowExecutionMetricsDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowExecutionMetricsEntity;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowMonitorOverviewVO;
import org.springframework.stereotype.Service;

/**
 * LiteFlow 監控服務
 *
 * <p>提供監控數據查詢功能
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Service
@RequiredArgsConstructor
public class LiteFlowMonitorService {

  private final LiteFlowChainDao chainDao;
  private final LiteFlowExecutionMetricsDao metricsDao;

  /**
   * 獲取監控概覽
   *
   * @return 監控概覽數據
   */
  public ResponseDTO<LiteFlowMonitorOverviewVO> getOverview() {
    LiteFlowMonitorOverviewVO overview = new LiteFlowMonitorOverviewVO();

    // 1. 統計總流程數
    LambdaQueryWrapper<LiteFlowChainEntity> chainQuery = new LambdaQueryWrapper<>();
    chainQuery.eq(LiteFlowChainEntity::getDeletedFlag, 0);
    overview.setTotalChains(chainDao.selectCount(chainQuery).intValue());

    // 2. 統計今日執行數
    LocalDate today = LocalDate.now();
    LambdaQueryWrapper<LiteFlowExecutionMetricsEntity> todayQuery = new LambdaQueryWrapper<>();
    todayQuery.eq(LiteFlowExecutionMetricsEntity::getMetricDate, today);
    List<LiteFlowExecutionMetricsEntity> todayMetrics = metricsDao.selectList(todayQuery);

    int todayExecutions =
        todayMetrics.stream().mapToInt(LiteFlowExecutionMetricsEntity::getTotalCount).sum();
    int todaySuccess =
        todayMetrics.stream().mapToInt(LiteFlowExecutionMetricsEntity::getSuccessCount).sum();

    overview.setTodayExecutions(todayExecutions);

    // 3. 計算今日成功率
    if (todayExecutions > 0) {
      BigDecimal successRate =
          new BigDecimal(todaySuccess)
              .divide(new BigDecimal(todayExecutions), 4, RoundingMode.HALF_UP)
              .multiply(new BigDecimal(100));
      overview.setTodaySuccessRate(successRate.doubleValue());
    } else {
      overview.setTodaySuccessRate(100.0);
    }

    return ResponseDTO.ok(overview);
  }
}
