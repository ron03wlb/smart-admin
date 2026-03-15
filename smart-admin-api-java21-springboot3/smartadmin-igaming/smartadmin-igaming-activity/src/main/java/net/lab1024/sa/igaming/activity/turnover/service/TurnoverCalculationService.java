package net.lab1024.sa.igaming.activity.turnover.service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.manager.LiteFlowChainManager;
import org.springframework.stereotype.Service;

/**
 * Turnover calculation service — orchestrates LiteFlow chain execution for bet turnover
 * calculation.
 *
 * <p>Calculation flow (3-layer verification):
 *
 * <ol>
 *   <li><b>Layer 1 (Risk Filter)</b>: Validates odds threshold and applies risk-based adjustments
 *   <li><b>Layer 2 (Finance Center)</b>: Applies settlement status factor (win/loss/draw)
 *   <li><b>Layer 3 (Activity Center)</b>: Applies game-specific weight multiplier
 * </ol>
 *
 * <p>Chain definition:
 *
 * <pre>
 * THEN(riskFilterNode, statusFactorNode, gameWeightNode, turnoverAggregateNode)
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoverCalculationService {

  private static final String CHAIN_CODE = "turnover_calculation_main";
  private static final String CHAIN_NAME = "流水計算主流程";
  private static final String CHAIN_EL =
      "THEN(riskFilterNode, statusFactorNode, gameWeightNode, turnoverAggregateNode)";

  private final SmartFlowExecutor flowExecutor;
  private final LiteFlowChainManager chainManager;

  /**
   * Initialize LiteFlow chain on application startup.
   *
   * <p>This method ensures the turnover calculation chain exists in the database. If the chain
   * already exists, it will be skipped.
   */
  @PostConstruct
  public void initializeChain() {
    try {
      // Check if chain already exists
      if (chainExists()) {
        log.info("LiteFlow chain already exists: chainCode={}", CHAIN_CODE);
        return;
      }

      // Create chain definition
      LiteFlowChainAddForm form = new LiteFlowChainAddForm();
      form.setChainName(CHAIN_NAME);
      form.setChainCode(CHAIN_CODE);
      form.setChainType(1); // Normal serial flow
      form.setChainData(CHAIN_EL);
      form.setRemark("Turnover calculation chain for bet settlement - 3-layer verification");

      // Add chain via manager (transaction + reload)
      ResponseDTO<String> response = chainManager.add(form, 0L, "SYSTEM");

      if (response.getOk()) {
        log.info(
            "LiteFlow chain initialized successfully: chainCode={}, chainEL={}",
            CHAIN_CODE,
            CHAIN_EL);
      } else {
        log.error("Failed to initialize LiteFlow chain: {}", response.getMsg());
      }
    } catch (Exception e) {
      log.error("Error initializing LiteFlow chain", e);
      // Don't throw - allow application to start even if chain initialization fails
    }
  }

  /**
   * Check if chain already exists in database.
   *
   * @return true if chain exists, false otherwise
   */
  private boolean chainExists() {
    try {
      // Try to reload the chain - if it exists, this will succeed
      flowExecutor.reloadRule();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Calculate turnover for a bet settlement.
   *
   * <p>Executes the LiteFlow chain with the provided context.
   *
   * @param betId bet ID
   * @param playerId player ID
   * @param tenantId tenant ID
   * @param betAmount bet amount
   * @param gameCategory game category (1-6)
   * @param settlementStatus settlement status (1-9)
   * @param oddsValue odds value
   * @param oddsType odds type (1-4)
   * @param riskScore risk score (0-100, optional)
   * @return calculation result
   */
  public ResponseDTO<TurnoverCalculationResult> calculate(
      String betId,
      Long playerId,
      Long tenantId,
      BigDecimal betAmount,
      Integer gameCategory,
      Integer settlementStatus,
      BigDecimal oddsValue,
      Integer oddsType,
      Integer riskScore) {

    // Build context
    TurnoverContext ctx = new TurnoverContext();
    ctx.setBetId(betId);
    ctx.setPlayerId(playerId);
    ctx.setTenantId(tenantId);
    ctx.setBetAmount(betAmount);
    ctx.setGameCategory(gameCategory);
    ctx.setSettlementStatus(settlementStatus);
    ctx.setOddsValue(oddsValue);
    ctx.setOddsType(oddsType);
    ctx.setRiskScore(riskScore != null ? riskScore : 0);

    // Execute LiteFlow chain
    try {
      flowExecutor.execute(CHAIN_CODE, ctx);
    } catch (Exception e) {
      log.error("Turnover calculation failed: betId={}", betId, e);
      return ResponseDTO.error(
          net.lab1024.sa.common.core.domain.code.SystemErrorCode.SYSTEM_ERROR,
          "Turnover calculation failed: " + e.getMessage());
    }

    // Extract result
    TurnoverCalculationResult result = buildResult(ctx);

    return ResponseDTO.ok(result);
  }

  /**
   * Build calculation result from context.
   *
   * @param ctx turnover context
   * @return calculation result
   */
  private TurnoverCalculationResult buildResult(TurnoverContext ctx) {
    TurnoverCalculationResult result = new TurnoverCalculationResult();
    result.setBetId(ctx.getBetId());
    result.setPlayerId(ctx.getPlayerId());
    result.setBetAmount(ctx.getBetAmount());
    result.setEffectiveTurnoverBase(ctx.getEffectiveTurnoverBase());
    result.setValidTurnoverFinance(ctx.getValidTurnoverFinance());
    result.setActivityValidTurnover(ctx.getActivityValidTurnover());
    result.setRiskActionType(ctx.getRiskActionType());
    result.setStatusFactor(ctx.getStatusFactor());
    result.setGameWeight(ctx.getGameWeight());
    result.setRejected(ctx.isRejected());
    result.setRejectedBy(ctx.getRejectedBy());
    result.setMatchedRules(ctx.getMatchedRules());
    result.setCalculatedAt(ctx.getCalculatedAt());
    return result;
  }

  /**
   * Turnover calculation result DTO.
   *
   * <p>Contains all intermediate and final calculation values for audit and reporting.
   */
  @lombok.Data
  public static class TurnoverCalculationResult {
    private String betId;
    private Long playerId;
    private BigDecimal betAmount;
    private BigDecimal effectiveTurnoverBase;
    private BigDecimal validTurnoverFinance;
    private BigDecimal activityValidTurnover;
    private Integer riskActionType;
    private BigDecimal statusFactor;
    private BigDecimal gameWeight;
    private Boolean rejected;
    private String rejectedBy;
    private java.util.List<String> matchedRules;
    private java.time.OffsetDateTime calculatedAt;
  }
}
