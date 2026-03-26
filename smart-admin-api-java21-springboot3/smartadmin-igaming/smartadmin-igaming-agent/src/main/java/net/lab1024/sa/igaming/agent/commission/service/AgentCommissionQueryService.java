package net.lab1024.sa.igaming.agent.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentPerformanceSnapshotDao;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentPerformanceSnapshotEntity;
import org.springframework.stereotype.Service;

/**
 * Agent Commission Query Service - Commission query and statistics.
 *
 * <p>Provides query methods for commission records, performance snapshots, and statistics.
 *
 * <p>Query Types: - Commission records by agent, date range, status - Performance snapshots
 * (weekly/monthly) - Commission summary statistics
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionQueryService {

  private final AgentCommissionRecordDao commissionRecordDao;
  private final AgentPerformanceSnapshotDao performanceSnapshotDao;

  /**
   * Get commission records for an agent in a date range.
   *
   * @param agentId agent ID
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @return list of commission records
   */
  public List<AgentCommissionRecordEntity> getCommissionRecords(
      Long agentId, LocalDate startDate, LocalDate endDate) {

    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .ge(AgentCommissionRecordEntity::getSettlementDate, startDate)
            .le(AgentCommissionRecordEntity::getSettlementDate, endDate)
            .orderByDesc(AgentCommissionRecordEntity::getSettlementDate);

    return commissionRecordDao.selectList(wrapper);
  }

  /**
   * Get commission records by status.
   *
   * @param agentId agent ID
   * @param status status (PENDING, SETTLED, FROZEN, CANCELLED)
   * @return list of commission records
   */
  public List<AgentCommissionRecordEntity> getCommissionRecordsByStatus(
      Long agentId, String status) {

    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .eq(AgentCommissionRecordEntity::getStatus, status)
            .orderByDesc(AgentCommissionRecordEntity::getSettlementDate);

    return commissionRecordDao.selectList(wrapper);
  }

  /**
   * Calculate total commission for an agent in a date range.
   *
   * @param agentId agent ID
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @param status optional status filter (null = all statuses)
   * @return total net commission
   */
  public BigDecimal calculateTotalCommission(
      Long agentId, LocalDate startDate, LocalDate endDate, String status) {

    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .ge(AgentCommissionRecordEntity::getSettlementDate, startDate)
            .le(AgentCommissionRecordEntity::getSettlementDate, endDate);

    if (status != null) {
      wrapper.eq(AgentCommissionRecordEntity::getStatus, status);
    }

    List<AgentCommissionRecordEntity> records = commissionRecordDao.selectList(wrapper);

    return records.stream()
        .map(AgentCommissionRecordEntity::getNetCommission)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Get performance snapshot for an agent.
   *
   * @param agentId agent ID
   * @param snapshotType snapshot type (WEEKLY, MONTHLY)
   * @param snapshotDate snapshot date
   * @return Option containing snapshot if found
   */
  public Option<AgentPerformanceSnapshotEntity> getPerformanceSnapshot(
      Long agentId, String snapshotType, LocalDate snapshotDate) {

    LambdaQueryWrapper<AgentPerformanceSnapshotEntity> wrapper =
        new LambdaQueryWrapper<AgentPerformanceSnapshotEntity>()
            .eq(AgentPerformanceSnapshotEntity::getAgentId, agentId)
            .eq(AgentPerformanceSnapshotEntity::getSnapshotType, snapshotType)
            .eq(AgentPerformanceSnapshotEntity::getSnapshotDate, snapshotDate);

    return Option.of(performanceSnapshotDao.selectOne(wrapper));
  }

  /**
   * Get performance snapshots for an agent in a date range.
   *
   * @param agentId agent ID
   * @param snapshotType snapshot type (WEEKLY, MONTHLY)
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @return list of performance snapshots
   */
  public List<AgentPerformanceSnapshotEntity> getPerformanceSnapshots(
      Long agentId, String snapshotType, LocalDate startDate, LocalDate endDate) {

    LambdaQueryWrapper<AgentPerformanceSnapshotEntity> wrapper =
        new LambdaQueryWrapper<AgentPerformanceSnapshotEntity>()
            .eq(AgentPerformanceSnapshotEntity::getAgentId, agentId)
            .eq(AgentPerformanceSnapshotEntity::getSnapshotType, snapshotType)
            .ge(AgentPerformanceSnapshotEntity::getSnapshotDate, startDate)
            .le(AgentPerformanceSnapshotEntity::getSnapshotDate, endDate)
            .orderByDesc(AgentPerformanceSnapshotEntity::getSnapshotDate);

    return performanceSnapshotDao.selectList(wrapper);
  }

  /**
   * Get commission records by product type.
   *
   * @param agentId agent ID
   * @param productType product type (SPORTS, CASINO, LIVE, POKER, LOTTERY)
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @return list of commission records
   */
  public List<AgentCommissionRecordEntity> getCommissionRecordsByProduct(
      Long agentId, String productType, LocalDate startDate, LocalDate endDate) {

    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .eq(AgentCommissionRecordEntity::getProductType, productType)
            .ge(AgentCommissionRecordEntity::getSettlementDate, startDate)
            .le(AgentCommissionRecordEntity::getSettlementDate, endDate)
            .orderByDesc(AgentCommissionRecordEntity::getSettlementDate);

    return commissionRecordDao.selectList(wrapper);
  }

  /**
   * Calculate commission breakdown by product type.
   *
   * @param agentId agent ID
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @return total commission for each product type
   */
  public BigDecimal calculateCommissionByProduct(
      Long agentId, String productType, LocalDate startDate, LocalDate endDate) {

    List<AgentCommissionRecordEntity> records =
        getCommissionRecordsByProduct(agentId, productType, startDate, endDate);

    return records.stream()
        .map(AgentCommissionRecordEntity::getNetCommission)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Get latest performance snapshot for an agent.
   *
   * @param agentId agent ID
   * @param snapshotType snapshot type (WEEKLY, MONTHLY)
   * @return Option containing latest snapshot if found
   */
  public Option<AgentPerformanceSnapshotEntity> getLatestSnapshot(
      Long agentId, String snapshotType) {

    LambdaQueryWrapper<AgentPerformanceSnapshotEntity> wrapper =
        new LambdaQueryWrapper<AgentPerformanceSnapshotEntity>()
            .eq(AgentPerformanceSnapshotEntity::getAgentId, agentId)
            .eq(AgentPerformanceSnapshotEntity::getSnapshotType, snapshotType)
            .orderByDesc(AgentPerformanceSnapshotEntity::getSnapshotDate)
            .last("LIMIT 1");

    return Option.of(performanceSnapshotDao.selectOne(wrapper));
  }

  /**
   * Count commission records by status.
   *
   * @param agentId agent ID
   * @param status status (PENDING, SETTLED, FROZEN, CANCELLED)
   * @return count of records
   */
  public long countRecordsByStatus(Long agentId, String status) {
    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .eq(AgentCommissionRecordEntity::getStatus, status);

    return commissionRecordDao.selectCount(wrapper);
  }
}
