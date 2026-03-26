package net.lab1024.sa.igaming.agent.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionConfigDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionConfigEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentRelationshipEntity;
import org.springframework.stereotype.Service;

/**
 * Agent Commission Calculation Service - Commission calculation engine with multi-level support.
 *
 * <p>Calculates commission for agents based on player negative profit (losses). Supports up to 5
 * levels of agent hierarchy with configurable commission rates.
 *
 * <p>Calculation formula: - Negative Profit = Total Bet Amount - Total Payout Amount - Commission =
 * Negative Profit × Commission Rate - Net Commission = Commission - Platform Cost
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionCalculationService {

  private final AgentRelationshipService agentRelationshipService;
  private final AgentCommissionConfigDao commissionConfigDao;
  private final AgentCommissionRecordDao commissionRecordDao;

  /**
   * Calculate commission for a player's betting activity.
   *
   * <p>Calculates commission for all agents in the player's upline based on negative profit.
   *
   * @param playerId player ID
   * @param productType product type (SPORTS, CASINO, LIVE, POKER, LOTTERY)
   * @param settlementDate settlement date
   * @param totalBetAmount total bet amount
   * @param totalPayoutAmount total payout amount
   * @param totalValidTurnover total valid turnover
   * @return List of commission records created
   */
  public List<AgentCommissionRecordEntity> calculateCommission(
      Long playerId,
      String productType,
      LocalDate settlementDate,
      BigDecimal totalBetAmount,
      BigDecimal totalPayoutAmount,
      BigDecimal totalValidTurnover) {

    // Calculate negative profit (player losses)
    BigDecimal negativeProfit = totalBetAmount.subtract(totalPayoutAmount);

    // If player won (negative profit <= 0), no commission
    if (negativeProfit.compareTo(BigDecimal.ZERO) <= 0) {
      log.info("No commission for player {} - negative profit: {}", playerId, negativeProfit);
      return List.of();
    }

    // Get player's agent binding
    Option<AgentRelationshipEntity> bindingOpt =
        agentRelationshipService.getActiveBinding(playerId);
    if (bindingOpt.isEmpty()) {
      log.info("No active agent for player {}, skipping commission", playerId);
      return List.of();
    }

    AgentRelationshipEntity binding = bindingOpt.get();
    Long agentId = binding.getAgentId();
    Integer agentLevel = binding.getLevel();

    // Get commission config for this agent level and product type
    Option<AgentCommissionConfigEntity> configOpt =
        getActiveCommissionConfig(agentLevel, productType, settlementDate);

    if (configOpt.isEmpty()) {
      log.warn(
          "No commission config found for agent {} (level {}, product {})",
          agentId,
          agentLevel,
          productType);
      return List.of();
    }

    AgentCommissionConfigEntity config = configOpt.get();

    // Check minimum negative profit threshold
    if (negativeProfit.compareTo(config.getMinNegativeProfit()) < 0) {
      log.info(
          "Negative profit {} below threshold {} for agent {}, skipping commission",
          negativeProfit,
          config.getMinNegativeProfit(),
          agentId);
      return List.of();
    }

    // Calculate commission
    BigDecimal commissionRate = config.getCommissionRate();
    BigDecimal commissionAmount =
        negativeProfit.multiply(commissionRate).setScale(4, RoundingMode.HALF_UP);

    // Calculate platform cost (5% of commission)
    BigDecimal platformCost =
        commissionAmount.multiply(new BigDecimal("0.05")).setScale(4, RoundingMode.HALF_UP);

    // Calculate net commission
    BigDecimal netCommission = commissionAmount.subtract(platformCost);

    // Create commission record
    AgentCommissionRecordEntity record =
        AgentCommissionRecordEntity.builder()
            .agentId(agentId)
            .settlementDate(settlementDate)
            .productType(productType)
            .totalValidTurnover(totalValidTurnover)
            .totalBetAmount(totalBetAmount)
            .totalPayoutAmount(totalPayoutAmount)
            .totalNegativeProfit(negativeProfit)
            .commissionRate(commissionRate)
            .commissionAmount(commissionAmount)
            .platformCost(platformCost)
            .netCommission(netCommission)
            .status("PENDING")
            .build();

    commissionRecordDao.insert(record);

    log.info(
        "Created commission record {} for agent {} - negative profit: {}, rate: {}%, commission: {}",
        record.getRecordId(),
        agentId,
        negativeProfit,
        commissionRate.multiply(new BigDecimal("100")),
        netCommission);

    List<AgentCommissionRecordEntity> records = new ArrayList<>();
    records.add(record);

    // TODO: Extend to multi-level commission calculation
    // This will require recursively processing upstream agents
    // For now, only calculating for direct agent (Level 1)

    return records;
  }

  /**
   * Get active commission config for agent level and product type.
   *
   * <p>Finds the config effective on the given settlement date.
   *
   * @param agentLevel agent level (1-5)
   * @param productType product type
   * @param settlementDate settlement date
   * @return Option containing config if found, None otherwise
   */
  public Option<AgentCommissionConfigEntity> getActiveCommissionConfig(
      Integer agentLevel, String productType, LocalDate settlementDate) {

    LambdaQueryWrapper<AgentCommissionConfigEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionConfigEntity>()
            .eq(AgentCommissionConfigEntity::getAgentLevel, agentLevel)
            .eq(AgentCommissionConfigEntity::getProductType, productType)
            .eq(AgentCommissionConfigEntity::getStatus, 1) // ENABLED
            .le(AgentCommissionConfigEntity::getEffectiveFrom, settlementDate)
            .and(
                w ->
                    w.isNull(AgentCommissionConfigEntity::getEffectiveTo)
                        .or()
                        .ge(AgentCommissionConfigEntity::getEffectiveTo, settlementDate));

    return Option.of(commissionConfigDao.selectOne(wrapper));
  }

  /**
   * Recalculate commission for a settlement period.
   *
   * <p>Used for manual recalculation or corrections.
   *
   * @param agentId agent ID
   * @param settlementDate settlement date
   * @param productType product type
   * @return updated commission record
   */
  public Option<AgentCommissionRecordEntity> recalculateCommission(
      Long agentId, LocalDate settlementDate, String productType) {

    // Find existing record
    LambdaQueryWrapper<AgentCommissionRecordEntity> wrapper =
        new LambdaQueryWrapper<AgentCommissionRecordEntity>()
            .eq(AgentCommissionRecordEntity::getAgentId, agentId)
            .eq(AgentCommissionRecordEntity::getSettlementDate, settlementDate)
            .eq(AgentCommissionRecordEntity::getProductType, productType);

    AgentCommissionRecordEntity existingRecord = commissionRecordDao.selectOne(wrapper);
    if (existingRecord == null) {
      log.warn(
          "No commission record found for agent {} on {} ({})",
          agentId,
          settlementDate,
          productType);
      return Option.none();
    }

    // Recalculate with current data
    BigDecimal negativeProfit = existingRecord.getTotalNegativeProfit();

    // Get current agent level
    Option<AgentRelationshipEntity> bindingOpt = agentRelationshipService.getActiveBinding(agentId);
    if (bindingOpt.isEmpty()) {
      log.warn("Agent {} no longer has active binding, cannot recalculate", agentId);
      return Option.none();
    }

    Integer agentLevel = bindingOpt.get().getLevel();

    // Get current commission config
    Option<AgentCommissionConfigEntity> configOpt =
        getActiveCommissionConfig(agentLevel, productType, settlementDate);
    if (configOpt.isEmpty()) {
      log.warn("No commission config found for recalculation");
      return Option.none();
    }

    AgentCommissionConfigEntity config = configOpt.get();

    // Recalculate commission
    BigDecimal commissionRate = config.getCommissionRate();
    BigDecimal commissionAmount =
        negativeProfit.multiply(commissionRate).setScale(4, RoundingMode.HALF_UP);
    BigDecimal platformCost =
        commissionAmount.multiply(new BigDecimal("0.05")).setScale(4, RoundingMode.HALF_UP);
    BigDecimal netCommission = commissionAmount.subtract(platformCost);

    // Update record
    existingRecord.setCommissionRate(commissionRate);
    existingRecord.setCommissionAmount(commissionAmount);
    existingRecord.setPlatformCost(platformCost);
    existingRecord.setNetCommission(netCommission);

    commissionRecordDao.updateById(existingRecord);

    log.info(
        "Recalculated commission record {} - new rate: {}%, new net commission: {}",
        existingRecord.getRecordId(),
        commissionRate.multiply(new BigDecimal("100")),
        netCommission);

    return Option.of(existingRecord);
  }
}
