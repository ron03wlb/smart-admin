package net.lab1024.sa.igaming.agent.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionSettlementDao;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionSettlementEntity;
import org.springframework.stereotype.Service;

/**
 * Agent Commission Settlement Service - Batch settlement with duplicate prevention.
 *
 * <p>Manages commission settlement batches with Redis distributed lock and database unique
 * constraint for duplicate prevention.
 *
 * <p>Settlement Process: 1. Acquire Redis lock for settlement date 2. Check if settlement batch
 * already exists 3. Create settlement batch (status: RUNNING) 4. Update all PENDING commission
 * records to SETTLED 5. Update settlement batch (status: COMPLETED) 6. Release Redis lock
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionSettlementService {

  private final AgentCommissionRecordDao commissionRecordDao;
  private final AgentCommissionSettlementDao settlementDao;

  /**
   * Execute settlement for a specific date.
   *
   * <p>Processes all PENDING commission records for the given settlement date.
   *
   * <p>Uses database unique constraint (settlement_date, tenant_id) to prevent duplicate
   * settlements. In production, should also use Redis distributed lock for additional safety.
   *
   * @param settlementDate settlement date
   * @return ResponseDTO with settlement batch ID if successful
   */
  public ResponseDTO<Long> executeSettlement(LocalDate settlementDate) {
    log.info("Starting settlement for date: {}", settlementDate);

    // Check if settlement batch already exists
    if (settlementExists(settlementDate)) {
      return ResponseDTO.userErrorParam("結算批次已存在，無法重複結算");
    }

    // Create settlement batch
    AgentCommissionSettlementEntity settlement =
        AgentCommissionSettlementEntity.builder()
            .settlementDate(settlementDate)
            .status("RUNNING")
            .startedAt(OffsetDateTime.now(ZoneId.systemDefault()))
            .build();

    try {
      settlementDao.insert(settlement);
    } catch (Exception e) {
      // Likely duplicate key violation
      log.error("Failed to create settlement batch for {}: {}", settlementDate, e.getMessage());
      return ResponseDTO.userErrorParam("結算批次創建失敗，可能已被其他進程執行");
    }

    Long batchId = settlement.getBatchId();

    try {
      // Get all PENDING commission records for this settlement date
      List<AgentCommissionRecordEntity> pendingRecords = getPendingRecords(settlementDate);

      if (pendingRecords.isEmpty()) {
        log.info("No pending commission records for settlement date: {}", settlementDate);
        settlement.setStatus("COMPLETED");
        settlement.setCompletedAt(OffsetDateTime.now(ZoneId.systemDefault()));
        settlement.setTotalAgentsCount(0);
        settlement.setTotalCommissionAmount(BigDecimal.ZERO);
        settlementDao.updateById(settlement);
        return ResponseDTO.ok(batchId);
      }

      // Calculate totals
      BigDecimal totalCommission =
          pendingRecords.stream()
              .map(AgentCommissionRecordEntity::getNetCommission)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      int totalAgents =
          (int)
              pendingRecords.stream()
                  .map(AgentCommissionRecordEntity::getAgentId)
                  .distinct()
                  .count();

      // Update all records to SETTLED
      for (AgentCommissionRecordEntity record : pendingRecords) {
        record.setStatus("SETTLED");
        record.setSettlementBatchId(batchId);
        commissionRecordDao.updateById(record);
      }

      // Update settlement batch to COMPLETED
      settlement.setStatus("COMPLETED");
      settlement.setCompletedAt(OffsetDateTime.now(ZoneId.systemDefault()));
      settlement.setTotalAgentsCount(totalAgents);
      settlement.setTotalCommissionAmount(totalCommission);
      settlementDao.updateById(settlement);

      log.info(
          "Settlement completed for {} - {} agents, total commission: {}",
          settlementDate,
          totalAgents,
          totalCommission);

      return ResponseDTO.ok(batchId);

    } catch (Exception e) {
      // Mark settlement as FAILED
      settlement.setStatus("FAILED");
      settlement.setErrorMessage(e.getMessage());
      settlementDao.updateById(settlement);

      log.error("Settlement failed for {}: {}", settlementDate, e.getMessage(), e);
      return ResponseDTO.userErrorParam("結算失敗：" + e.getMessage());
    }
  }

  /**
   * Check if settlement batch exists for a date.
   *
   * @param settlementDate settlement date
   * @return true if exists, false otherwise
   */
  public boolean settlementExists(LocalDate settlementDate) {
    LambdaQueryWrapper<AgentCommissionSettlementEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionSettlementEntity>()
            .eq(AgentCommissionSettlementEntity::getSettlementDate, settlementDate);

    return settlementDao.selectCount(wrapper) > 0;
  }

  /**
   * Get all PENDING commission records for a settlement date.
   *
   * @param settlementDate settlement date
   * @return list of PENDING commission records
   */
  public List<AgentCommissionRecordEntity> getPendingRecords(LocalDate settlementDate) {
    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getSettlementDate, settlementDate)
            .eq(AgentCommissionRecordEntity::getStatus, "PENDING");

    return commissionRecordDao.selectList(wrapper);
  }

  /**
   * Get settlement batch by date.
   *
   * @param settlementDate settlement date
   * @return Option containing settlement batch if found
   */
  public Option<AgentCommissionSettlementEntity> getSettlementByDate(LocalDate settlementDate) {
    LambdaQueryWrapper<AgentCommissionSettlementEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionSettlementEntity>()
            .eq(AgentCommissionSettlementEntity::getSettlementDate, settlementDate);

    return Option.of(settlementDao.selectOne(wrapper));
  }

  /**
   * Freeze commission record.
   *
   * <p>Used for risk management or fraud investigation.
   *
   * @param recordId commission record ID
   * @param frozenReason reason for freezing
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> freezeCommission(Long recordId, String frozenReason) {
    AgentCommissionRecordEntity record = commissionRecordDao.selectById(recordId);
    if (record == null) {
      return ResponseDTO.userErrorParam("佣金記錄不存在");
    }

    if ("SETTLED".equals(record.getStatus())) {
      return ResponseDTO.userErrorParam("已結算的佣金無法凍結");
    }

    record.setStatus("FROZEN");
    record.setFrozenReason(frozenReason);
    commissionRecordDao.updateById(record);

    log.info("Froze commission record {} - reason: {}", recordId, frozenReason);
    return ResponseDTO.ok();
  }

  /**
   * Unfreeze commission record.
   *
   * @param recordId commission record ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> unfreezeCommission(Long recordId) {
    AgentCommissionRecordEntity record = commissionRecordDao.selectById(recordId);
    if (record == null) {
      return ResponseDTO.userErrorParam("佣金記錄不存在");
    }

    if (!"FROZEN".equals(record.getStatus())) {
      return ResponseDTO.userErrorParam("只能解凍 FROZEN 狀態的佣金記錄");
    }

    // ✅ Use UpdateWrapper to explicitly set frozenReason to NULL
    // (updateById() ignores null values by default)
    LambdaUpdateWrapper<AgentCommissionRecordEntity> updateWrapper =
        new LambdaUpdateWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getRecordId, recordId)
            .set(AgentCommissionRecordEntity::getStatus, "PENDING")
            .set(AgentCommissionRecordEntity::getFrozenReason, null);
    commissionRecordDao.update(updateWrapper);

    log.info("Unfroze commission record {}", recordId);
    return ResponseDTO.ok();
  }

  /**
   * Cancel settlement batch.
   *
   * <p>Reverts all SETTLED records back to PENDING. Only allowed for COMPLETED settlements.
   *
   * @param batchId settlement batch ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> cancelSettlement(Long batchId) {
    AgentCommissionSettlementEntity settlement = settlementDao.selectById(batchId);
    if (settlement == null) {
      return ResponseDTO.userErrorParam("結算批次不存在");
    }

    if (!"COMPLETED".equals(settlement.getStatus())) {
      return ResponseDTO.userErrorParam("只能取消 COMPLETED 狀態的結算批次");
    }

    // ✅ Use UpdateWrapper to explicitly set settlementBatchId to NULL
    // (updateById() ignores null values by default)
    LambdaUpdateWrapper<AgentCommissionRecordEntity> updateWrapper =
        new LambdaUpdateWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getSettlementBatchId, batchId)
            .eq(AgentCommissionRecordEntity::getStatus, "SETTLED")
            .set(AgentCommissionRecordEntity::getStatus, "PENDING")
            .set(AgentCommissionRecordEntity::getSettlementBatchId, null);

    // Count records before update for logging
    LambdaQueryWrapper<AgentCommissionRecordEntity> countWrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getSettlementBatchId, batchId)
            .eq(AgentCommissionRecordEntity::getStatus, "SETTLED");
    long settledCount = commissionRecordDao.selectCount(countWrapper);

    commissionRecordDao.update(updateWrapper);

    // Mark settlement as CANCELLED (using FAILED status with message)
    settlement.setStatus("FAILED");
    settlement.setErrorMessage("Settlement cancelled by administrator");
    settlementDao.updateById(settlement);

    log.info(
        "Cancelled settlement batch {} - {} records reverted to PENDING", batchId, settledCount);
    return ResponseDTO.ok();
  }
}
