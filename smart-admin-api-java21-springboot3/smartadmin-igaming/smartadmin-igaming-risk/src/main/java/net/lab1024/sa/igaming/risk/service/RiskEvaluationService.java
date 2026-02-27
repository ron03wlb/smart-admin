package net.lab1024.sa.igaming.risk.service;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.igaming.common.constant.RiskDecisionEnum;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.risk.dao.RiskRuleParamDao;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import net.lab1024.sa.igaming.risk.domain.RiskEvent;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskRuleParamEntity;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.igaming.risk.manager.RiskScoreManager;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import org.springframework.stereotype.Service;

/**
 * Risk evaluation service — orchestrates LiteFlow chain execution and dispatches outcomes.
 *
 * <p>Decision thresholds:
 *
 * <ul>
 *   <li>0-29: AUTO_APPROVED (audit logging only)
 *   <li>30-69: PENDING_REVIEW (create proposal for manual review)
 *   <li>70-100: AUTO_REJECTED (freeze account / block operation)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

  private static final String CHAIN_CODE = "player-risk-assessment";
  private static final int AUTO_APPROVE_THRESHOLD = 30;
  private static final int AUTO_REJECT_THRESHOLD = 70;

  private final SmartFlowExecutor smartFlowExecutor;
  private final RiskRuleParamDao riskRuleParamDao;
  private final RiskScoreManager riskScoreManager;
  private final RiskProposalManager riskProposalManager;

  /**
   * Evaluate risk for an event and dispatch the appropriate action.
   *
   * @param event the risk event from Kafka
   */
  public void evaluateAndDispatch(RiskEvent event) {
    long start = System.currentTimeMillis();

    // 1. Build context
    RiskContext ctx = buildContext(event);

    // 2. Execute LiteFlow chain
    try {
      smartFlowExecutor.execute(CHAIN_CODE, ctx);
    } catch (Exception e) {
      log.error(
          "LiteFlow chain execution failed: playerId={}, eventType={}",
          event.getPlayerId(),
          event.getEventType(),
          e);
      return;
    }

    // 3. Load rule weights and calculate total score
    Map<String, BigDecimal> weights = loadRuleWeights(event.getTenantId());
    int totalScore = ctx.calculateTotalScore(weights);

    // 4. Determine decision
    RiskDecisionEnum decision = determineDecision(totalScore);
    RiskLevelEnum riskLevel = determineRiskLevel(totalScore);

    long processingTime = System.currentTimeMillis() - start;

    // 5. Save assessment record
    RiskAssessmentEntity assessment =
        buildAssessment(event, totalScore, riskLevel, decision, ctx, (int) processingTime);
    riskScoreManager.saveTransactionScore(assessment);

    // 6. Update player profile
    riskScoreManager.updateProfileScore(event.getPlayerId(), event.getTenantId(), totalScore);

    // 7. Dispatch based on decision
    dispatchDecision(decision, riskLevel, event, assessment.getAssessmentId());

    log.info(
        "Risk evaluation complete: playerId={}, eventType={}, score={}, decision={}, time={}ms",
        event.getPlayerId(),
        event.getEventType(),
        totalScore,
        decision.getDesc(),
        processingTime);
  }

  private RiskContext buildContext(RiskEvent event) {
    RiskContext ctx = new RiskContext();
    ctx.setPlayerId(event.getPlayerId());
    ctx.setTenantId(event.getTenantId());
    ctx.setEventType(event.getEventType());
    ctx.setEventId(event.getEventId());
    ctx.setAmount(event.getAmount());
    ctx.setPlayerBalance(event.getPlayerBalance());
    ctx.setDeviceId(event.getDeviceId());
    ctx.setIpAddress(event.getIpAddress());
    ctx.setCountryCode(event.getCountryCode());
    ctx.setGameType(event.getGameType());
    ctx.setGameCode(event.getGameCode());
    return ctx;
  }

  private Map<String, BigDecimal> loadRuleWeights(Long tenantId) {
    return Option.of(riskRuleParamDao.findAllEnabled(tenantId))
        .map(
            rules ->
                rules.stream()
                    .collect(
                        Collectors.toMap(
                            RiskRuleParamEntity::getRuleName,
                            RiskRuleParamEntity::getWeight,
                            (a, b) -> a)))
        .getOrElse(Map.of());
  }

  private RiskDecisionEnum determineDecision(int score) {
    if (score >= AUTO_REJECT_THRESHOLD) {
      return RiskDecisionEnum.AUTO_REJECTED;
    } else if (score >= AUTO_APPROVE_THRESHOLD) {
      return RiskDecisionEnum.PENDING_REVIEW;
    }
    return RiskDecisionEnum.AUTO_APPROVED;
  }

  private RiskLevelEnum determineRiskLevel(int score) {
    if (score >= 70) {
      return RiskLevelEnum.CRITICAL;
    } else if (score >= 50) {
      return RiskLevelEnum.HIGH;
    } else if (score >= 30) {
      return RiskLevelEnum.MEDIUM;
    }
    return RiskLevelEnum.LOW;
  }

  private RiskAssessmentEntity buildAssessment(
      RiskEvent event,
      int totalScore,
      RiskLevelEnum riskLevel,
      RiskDecisionEnum decision,
      RiskContext ctx,
      int processingTimeMs) {
    RiskAssessmentEntity assessment = new RiskAssessmentEntity();
    assessment.setTenantId(event.getTenantId());
    assessment.setPlayerId(event.getPlayerId());
    assessment.setEventType(event.getEventType());
    assessment.setEventId(event.getEventId());
    assessment.setRiskScore(BigDecimal.valueOf(totalScore));
    assessment.setRiskLevel(riskLevel.getValue());
    assessment.setDecision(decision.getValue());
    assessment.setRuleResultsJson(JsonUtil.toJson(ctx.getRuleScores()));
    assessment.setProcessingTimeMs(processingTimeMs);
    assessment.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return assessment;
  }

  private void dispatchDecision(
      RiskDecisionEnum decision, RiskLevelEnum riskLevel, RiskEvent event, Long assessmentId) {
    switch (decision) {
      case AUTO_APPROVED ->
          log.debug(
              "Auto-approved: playerId={}, eventType={}",
              event.getPlayerId(),
              event.getEventType());
      case PENDING_REVIEW ->
          riskProposalManager.createReviewProposal(
              event.getPlayerId(), event.getTenantId(), assessmentId, riskLevel);
      case AUTO_REJECTED -> {
        riskProposalManager.createUrgentProposal(
            event.getPlayerId(), event.getTenantId(), assessmentId);
        log.warn(
            "Auto-rejected: playerId={}, eventType={}", event.getPlayerId(), event.getEventType());
      }
    }
  }
}
