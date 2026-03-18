package net.lab1024.sa.igaming.integration.risk;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.common.constant.RiskDecisionEnum;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.integration.risk.domain.WithdrawalRiskContext;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import org.springframework.stereotype.Service;

/**
 * Withdrawal risk check service — orchestrates LiteFlow withdrawal risk assessment chain.
 *
 * <p>This service coordinates the complete withdrawal risk evaluation workflow:
 *
 * <ol>
 *   <li>Build {@link WithdrawalRiskContext} from withdrawal order, player, wallet data
 *   <li>Execute LiteFlow risk chain: THEN(kycLevelCheck, amlTurnoverCheck, withdrawalVelocityCheck)
 *   <li>Calculate weighted total risk score
 *   <li>Make final decision: AUTO_APPROVED / PENDING_REVIEW / AUTO_REJECTED
 *   <li>Save {@link RiskAssessmentEntity} (audit log)
 *   <li>Create {@link net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity} if manual
 *       review required
 *   <li>Publish WITHDRAWAL_RISK_CHECKED event to Kafka
 * </ol>
 *
 * <p><b>Risk Score Weights:</b>
 *
 * <ul>
 *   <li>KYC Level Check: 3.0 (highest priority)
 *   <li>AML Turnover Check: 2.0 (medium priority)
 *   <li>Withdrawal Velocity Check: 1.0 (lowest priority)
 * </ul>
 *
 * <p><b>Decision Thresholds:</b>
 *
 * <ul>
 *   <li>0-29: AUTO_APPROVED (low risk, proceed with withdrawal)
 *   <li>30-69: PENDING_REVIEW (medium/high risk, manual approval required)
 *   <li>70-100: AUTO_REJECTED (critical risk, block withdrawal)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRiskCheckService {

  private static final String LITEFLOW_CHAIN_NAME = "withdrawal_risk_check_main";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Component score weights (KYC=3, AML=2, Velocity=1). */
  private static final Map<String, BigDecimal> SCORE_WEIGHTS = new HashMap<>();

  static {
    SCORE_WEIGHTS.put("kycLevelCheck", new BigDecimal("3.0"));
    SCORE_WEIGHTS.put("amlTurnoverCheck", new BigDecimal("2.0"));
    SCORE_WEIGHTS.put("withdrawalVelocityCheck", new BigDecimal("1.0"));
  }

  private final FlowExecutor flowExecutor;
  private final PlayerDao playerDao;
  private final PaymentOrderDao paymentOrderDao;
  private final WalletDao walletDao;
  private final RiskAssessmentDao riskAssessmentDao;
  private final RiskProposalManager riskProposalManager;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Execute withdrawal risk check for a given payment order.
   *
   * <p>This method orchestrates the complete risk assessment workflow and returns the final risk
   * decision.
   *
   * @param orderNo payment order number (withdrawal)
   * @return risk check result with decision
   */
  public ResponseDTO<String> executeRiskCheck(String orderNo) {
    long startTime = System.currentTimeMillis();

    log.info("[WITHDRAWAL_RISK_CHECK] Starting risk check for orderNo={}", orderNo);

    // Step 1: Load withdrawal order
    PaymentOrderEntity order = loadPaymentOrder(orderNo);
    if (order == null) {
      return ResponseDTO.userErrorParam("Payment order not found: " + orderNo);
    }

    Long playerId = order.getPlayerId();
    Long tenantId = order.getTenantId();

    // Step 2: Load player
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null) {
      return ResponseDTO.userErrorParam("Player not found: " + playerId);
    }

    // Step 3: Load wallet
    WalletEntity wallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, playerId)
                .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                .eq(WalletEntity::getDeleted, false));

    if (wallet == null) {
      return ResponseDTO.userErrorParam("Wallet not found for player: " + playerId);
    }

    // Step 4: Calculate cumulative turnover and total deposits
    BigDecimal cumulativeTurnover = calculateCumulativeTurnover(playerId, tenantId);
    BigDecimal totalDeposits = calculateTotalDeposits(playerId, tenantId);

    // Step 5: Build WithdrawalRiskContext
    WithdrawalRiskContext context =
        buildRiskContext(order, player, wallet, cumulativeTurnover, totalDeposits);

    // Step 6: Execute LiteFlow risk assessment chain
    LiteflowResponse response = flowExecutor.execute2Resp(LITEFLOW_CHAIN_NAME, null, context);

    if (!response.isSuccess()) {
      log.error(
          "[WITHDRAWAL_RISK_CHECK] LiteFlow chain execution failed for orderNo={}. Cause: {}",
          orderNo,
          response.getCause());
      return ResponseDTO.userErrorParam("Risk check chain execution failed");
    }

    // Step 7: Calculate weighted total score
    int totalScore = context.calculateTotalScore(SCORE_WEIGHTS);
    String riskLevel = context.getRiskLevel(totalScore);
    String decision = context.getDecision(totalScore);

    long processingTime = System.currentTimeMillis() - startTime;

    log.info(
        "[WITHDRAWAL_RISK_CHECK] Risk check completed for orderNo={}, playerId={}. Score={},"
            + " Level={}, Decision={}, ProcessingTime={}ms",
        orderNo,
        playerId,
        totalScore,
        riskLevel,
        decision,
        processingTime);

    // Step 8: Save risk assessment (audit log)
    RiskAssessmentEntity assessment =
        saveRiskAssessment(
            tenantId,
            playerId,
            orderNo,
            totalScore,
            riskLevel,
            decision,
            context,
            (int) processingTime);

    // Step 9: Create risk proposal if manual review required
    if ("PENDING_REVIEW".equals(decision)) {
      RiskLevelEnum riskLevelEnum = RiskLevelEnum.valueOf(riskLevel);
      riskProposalManager.createReviewProposal(
          playerId, tenantId, assessment.getAssessmentId(), riskLevelEnum);

      log.info(
          "[WITHDRAWAL_RISK_CHECK] Created review proposal for orderNo={}, assessmentId={}",
          orderNo,
          assessment.getAssessmentId());
    }

    // Step 10: Publish WITHDRAWAL_RISK_CHECKED event
    publishRiskCheckedEvent(order, totalScore, riskLevel, decision);

    return ResponseDTO.ok(
        String.format(
            "Risk check completed. Decision: %s, Score: %d, Level: %s",
            decision, totalScore, riskLevel));
  }

  /**
   * Load payment order by order number.
   *
   * @param orderNo order number
   * @return payment order entity
   */
  private PaymentOrderEntity loadPaymentOrder(String orderNo) {
    return paymentOrderDao.selectOne(
        Wrappers.<PaymentOrderEntity>lambdaQuery()
            .eq(PaymentOrderEntity::getOrderNo, orderNo)
            .eq(PaymentOrderEntity::getOrderType, PaymentOrderTypeEnum.WITHDRAWAL.getValue()));
  }

  /**
   * Calculate player's cumulative turnover (lifetime).
   *
   * <p>This is a simplified implementation. In production, turnover should be queried from a
   * pre-aggregated summary table or calculated via a database query summing all valid turnover
   * records.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @return cumulative turnover
   */
  private BigDecimal calculateCumulativeTurnover(Long playerId, Long tenantId) {
    // TODO: Implement actual turnover calculation
    // Option 1: Query from t_wagering_progress (aggregate all activityValidTurnover)
    // Option 2: Query from t_player_stats (pre-aggregated summary table)
    // Option 3: Query from game betting history and apply turnover rules

    // Placeholder: Return 0 for now
    log.warn(
        "[WITHDRAWAL_RISK_CHECK] cumulativeTurnover calculation not implemented. Returning 0 for"
            + " playerId={}",
        playerId);
    return BigDecimal.ZERO;
  }

  /**
   * Calculate player's total deposits (lifetime).
   *
   * <p>Sum all SUCCESS deposits for this player.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @return total deposits
   */
  private BigDecimal calculateTotalDeposits(Long playerId, Long tenantId) {
    // Query all SUCCESS deposits
    // SELECT SUM(amount) FROM t_payment_order WHERE player_id = ? AND order_type = 1 AND status = 3

    // Placeholder: Return 0 for now
    log.warn(
        "[WITHDRAWAL_RISK_CHECK] totalDeposits calculation not implemented. Returning 0 for"
            + " playerId={}",
        playerId);
    return BigDecimal.ZERO;
  }

  /**
   * Build withdrawal risk context from data.
   *
   * @param order payment order entity
   * @param player player entity
   * @param wallet wallet entity
   * @param cumulativeTurnover cumulative turnover
   * @param totalDeposits total deposits
   * @return risk context
   */
  private WithdrawalRiskContext buildRiskContext(
      PaymentOrderEntity order,
      PlayerEntity player,
      WalletEntity wallet,
      BigDecimal cumulativeTurnover,
      BigDecimal totalDeposits) {
    WithdrawalRiskContext context = new WithdrawalRiskContext();

    // Withdrawal metadata
    context.setOrderNo(order.getOrderNo());
    context.setPlayerId(player.getPlayerId());
    context.setTenantId(order.getTenantId());
    context.setWithdrawalAmount(order.getAmount());
    context.setCurrencyCode(order.getCurrencyCode());
    context.setCurrentBalance(wallet.getBalance());
    context.setLockedAmount(wallet.getLockedAmount());

    // Player risk profile
    context.setKycLevel(player.getKycLevel());
    context.setCumulativeTurnover(cumulativeTurnover);
    context.setTotalDeposits(totalDeposits);

    // Note: totalWithdrawals is not required by current risk rules, so we skip it
    // context.setTotalWithdrawals(calculateTotalWithdrawals(player.getPlayerId(),
    // order.getTenantId()));

    return context;
  }

  /**
   * Save risk assessment to database (audit log).
   *
   * @param tenantId tenant ID
   * @param playerId player ID
   * @param orderNo order number
   * @param totalScore total risk score
   * @param riskLevel risk level (LOW, MEDIUM, HIGH, CRITICAL)
   * @param decision decision (AUTO_APPROVED, PENDING_REVIEW, AUTO_REJECTED)
   * @param context risk context
   * @param processingTimeMs processing time in milliseconds
   * @return saved assessment entity
   */
  private RiskAssessmentEntity saveRiskAssessment(
      Long tenantId,
      Long playerId,
      String orderNo,
      int totalScore,
      String riskLevel,
      String decision,
      WithdrawalRiskContext context,
      int processingTimeMs) {
    RiskAssessmentEntity assessment = new RiskAssessmentEntity();
    assessment.setTenantId(tenantId);
    assessment.setPlayerId(playerId);
    assessment.setEventType("WITHDRAWAL_REQUESTED");
    assessment.setEventId(orderNo);
    assessment.setRiskScore(new BigDecimal(totalScore));
    assessment.setRiskLevel(RiskLevelEnum.valueOf(riskLevel).getValue());
    assessment.setDecision(RiskDecisionEnum.valueOf(decision).getValue());
    assessment.setProcessingTimeMs(processingTimeMs);
    assessment.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    // Serialize rule results to JSON
    try {
      ObjectNode ruleResults = JsonNodeFactory.instance.objectNode();
      ruleResults.put("blocked", context.isBlocked());
      ruleResults.put("flagged", context.isFlagged());
      ruleResults.put("blockReason", context.getBlockReason());
      ruleResults.put("flagReason", context.getFlagReason());

      ObjectNode scores = JsonNodeFactory.instance.objectNode();
      context.getRuleScores().forEach(scores::put);
      ruleResults.set("ruleScores", scores);

      assessment.setRuleResultsJson(OBJECT_MAPPER.writeValueAsString(ruleResults));
    } catch (Exception e) {
      log.error("[WITHDRAWAL_RISK_CHECK] Failed to serialize rule results", e);
      assessment.setRuleResultsJson("{}");
    }

    riskAssessmentDao.insert(assessment);
    return assessment;
  }

  /**
   * Publish WITHDRAWAL_RISK_CHECKED event to Kafka.
   *
   * @param order payment order
   * @param totalScore total risk score
   * @param riskLevel risk level
   * @param decision decision
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishRiskCheckedEvent(
      PaymentOrderEntity order, int totalScore, String riskLevel, String decision) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("orderNo", order.getOrderNo());
    payload.put("playerId", order.getPlayerId());
    payload.put("amount", order.getAmount().toPlainString());
    payload.put("totalScore", totalScore);
    payload.put("riskLevel", riskLevel);
    payload.put("decision", decision);

    DomainEvent event =
        DomainEvent.builder()
            .eventType("WITHDRAWAL_RISK_CHECKED")
            .tenantId(order.getTenantId())
            .aggregateType("WITHDRAWAL")
            .aggregateId(order.getOrderNo())
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.RISK_EVENTS, event);

    log.debug(
        "[WITHDRAWAL_RISK_CHECK] Published WITHDRAWAL_RISK_CHECKED event: orderNo={}, decision={}",
        order.getOrderNo(),
        decision);
  }
}
