package net.lab1024.sa.base.module.support.liteflow.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.liteflow.dao.LiteFlowExecutionMetricsDao;
import net.lab1024.sa.base.module.support.liteflow.domain.entity.LiteFlowExecutionMetricsEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LiteFlow 指標管理器
 *
 * <p>負責執行指標的聚合和更新
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteFlowMetricsManager {

  private final LiteFlowExecutionMetricsDao metricsDao;

  /**
   * 更新執行指標
   *
   * @param chainCode 流程編碼
   * @param executionTime 執行時長（毫秒）
   * @param success 是否成功
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateMetrics(String chainCode, long executionTime, boolean success) {
    LocalDate today = LocalDate.now();

    LambdaQueryWrapper<LiteFlowExecutionMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(LiteFlowExecutionMetricsEntity::getChainCode, chainCode)
        .eq(LiteFlowExecutionMetricsEntity::getMetricDate, today);

    LiteFlowExecutionMetricsEntity metrics = metricsDao.selectOne(queryWrapper);

    if (metrics == null) {
      // 創建新記錄
      metrics = new LiteFlowExecutionMetricsEntity();
      metrics.setChainCode(chainCode);
      metrics.setMetricDate(today);
      metrics.setTotalCount(1);
      metrics.setSuccessCount(success ? 1 : 0);
      metrics.setFailureCount(success ? 0 : 1);
      metrics.setAvgExecutionTime((int) executionTime);
      metrics.setMaxExecutionTime((int) executionTime);
      metrics.setMinExecutionTime((int) executionTime);
      metrics.setCreateTime(LocalDateTime.now());

      metricsDao.insert(metrics);

    } else {
      // 增量更新
      int newTotal = metrics.getTotalCount() + 1;
      int currentAvg = metrics.getAvgExecutionTime();
      int currentTotal = metrics.getTotalCount();
      int newAvg = (currentAvg * currentTotal + (int) executionTime) / newTotal;

      // 優化裝箱/拆箱操作
      int currentSuccess = metrics.getSuccessCount();
      int currentFailure = metrics.getFailureCount();

      metrics.setTotalCount(newTotal);
      metrics.setSuccessCount(success ? currentSuccess + 1 : currentSuccess);
      metrics.setFailureCount(success ? currentFailure : currentFailure + 1);
      metrics.setAvgExecutionTime(newAvg);
      metrics.setMaxExecutionTime(Math.max(metrics.getMaxExecutionTime(), (int) executionTime));
      metrics.setMinExecutionTime(Math.min(metrics.getMinExecutionTime(), (int) executionTime));
      metrics.setUpdateTime(LocalDateTime.now());

      metricsDao.updateById(metrics);
    }

    if (log.isDebugEnabled()) {
      log.debug("LiteFlow 指標已更新: chainCode={}, totalCount={}", chainCode, metrics.getTotalCount());
    }
  }
}
