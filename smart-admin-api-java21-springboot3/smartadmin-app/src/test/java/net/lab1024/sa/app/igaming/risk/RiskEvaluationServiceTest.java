package net.lab1024.sa.app.igaming.risk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.risk.dao.RiskRuleParamDao;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import net.lab1024.sa.igaming.risk.domain.RiskEvent;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskRuleParamEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.igaming.risk.manager.RiskScoreManager;
import net.lab1024.sa.igaming.risk.service.RiskEvaluationService;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEvaluationService 測試")
class RiskEvaluationServiceTest {

  @Mock private SmartFlowExecutor smartFlowExecutor;
  @Mock private RiskRuleParamDao riskRuleParamDao;
  @Mock private RiskScoreManager riskScoreManager;
  @Mock private RiskProposalManager riskProposalManager;
  @InjectMocks private RiskEvaluationService riskEvaluationService;

  @Nested
  @DisplayName("evaluateAndDispatch 測試")
  class EvaluateAndDispatchTest {

    @Test
    @DisplayName("低分自動通過 (score < 30)")
    void low_score_auto_approved() {
      RiskEvent event = buildEvent(100L, 1L, "BET_PLACED");
      setupLiteFlowExecution(event, 10); // All components return score 10
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(100L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskScoreManager).saveTransactionScore(any(RiskAssessmentEntity.class));
      verify(riskScoreManager).updateProfileScore(eq(100L), eq(1L), anyInt());
      verify(riskProposalManager, never())
          .createReviewProposal(anyLong(), anyLong(), anyLong(), any());
      verify(riskProposalManager, never()).createUrgentProposal(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("中分待審核 (30 <= score < 70)")
    void medium_score_pending_review() {
      RiskEvent event = buildEvent(200L, 1L, "WITHDRAWAL_REQUESTED");
      setupLiteFlowExecution(event, 45);
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(200L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskScoreManager).saveTransactionScore(any(RiskAssessmentEntity.class));
      verify(riskProposalManager)
          .createReviewProposal(eq(200L), eq(1L), any(), any(RiskLevelEnum.class));
    }

    @Test
    @DisplayName("高分自動拒絕 (score >= 70)")
    void high_score_auto_rejected() {
      RiskEvent event = buildEvent(300L, 1L, "BET_PLACED");
      setupLiteFlowExecution(event, 80);
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(300L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskScoreManager).saveTransactionScore(any(RiskAssessmentEntity.class));
      verify(riskProposalManager).createUrgentProposal(eq(300L), eq(1L), any());
    }

    @Test
    @DisplayName("LiteFlow 失敗不儲存評估")
    void liteflow_failure_no_save() {
      RiskEvent event = buildEvent(400L, 1L, "BET_PLACED");
      when(smartFlowExecutor.execute(eq("player-risk-assessment"), any(RiskContext.class)))
          .thenThrow(new RuntimeException("chain error"));

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskScoreManager, never()).saveTransactionScore(any());
      verify(riskScoreManager, never()).updateProfileScore(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("加權分數計算使用規則參數")
    void weighted_score_uses_rule_params() {
      RiskEvent event = buildEvent(500L, 1L, "BET_PLACED");

      // Set up components to produce specific scores
      doAnswer(
              invocation -> {
                RiskContext ctx = invocation.getArgument(1);
                ctx.addRuleScore("velocityCheck", 50);
                ctx.addRuleScore("amountThreshold", 0);
                return null;
              })
          .when(smartFlowExecutor)
          .execute(eq("player-risk-assessment"), any(RiskContext.class));

      // velocityCheck weight=0.20, amountThreshold weight=0.25
      RiskRuleParamEntity rule1 = new RiskRuleParamEntity();
      rule1.setRuleName("velocityCheck");
      rule1.setWeight(new BigDecimal("0.20"));
      RiskRuleParamEntity rule2 = new RiskRuleParamEntity();
      rule2.setRuleName("amountThreshold");
      rule2.setWeight(new BigDecimal("0.25"));
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of(rule1, rule2));
      when(riskScoreManager.updateProfileScore(eq(500L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      // (50 * 0.20 + 0 * 0.25) / (0.20 + 0.25) = 10 / 0.45 = 22.2 -> 22
      verify(riskScoreManager).saveTransactionScore(any(RiskAssessmentEntity.class));
      verify(riskProposalManager, never())
          .createReviewProposal(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("邊界值 score=30 為 PENDING_REVIEW")
    void boundary_score_30_pending_review() {
      RiskEvent event = buildEvent(600L, 1L, "BET_PLACED");
      setupLiteFlowExecution(event, 30);
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(600L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskProposalManager)
          .createReviewProposal(eq(600L), eq(1L), any(), any(RiskLevelEnum.class));
    }

    @Test
    @DisplayName("邊界值 score=70 為 AUTO_REJECTED")
    void boundary_score_70_auto_rejected() {
      RiskEvent event = buildEvent(700L, 1L, "BET_PLACED");
      setupLiteFlowExecution(event, 70);
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(700L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskProposalManager).createUrgentProposal(eq(700L), eq(1L), any());
    }

    @Test
    @DisplayName("邊界值 score=29 為 AUTO_APPROVED")
    void boundary_score_29_auto_approved() {
      RiskEvent event = buildEvent(800L, 1L, "BET_PLACED");
      setupLiteFlowExecution(event, 29);
      when(riskRuleParamDao.findAllEnabled(1L)).thenReturn(List.of());
      when(riskScoreManager.updateProfileScore(eq(800L), eq(1L), anyInt()))
          .thenReturn(new RiskScoreEntity());

      riskEvaluationService.evaluateAndDispatch(event);

      verify(riskProposalManager, never())
          .createReviewProposal(anyLong(), anyLong(), anyLong(), any());
      verify(riskProposalManager, never()).createUrgentProposal(anyLong(), anyLong(), anyLong());
    }
  }

  private RiskEvent buildEvent(Long playerId, Long tenantId, String eventType) {
    RiskEvent event = new RiskEvent();
    event.setPlayerId(playerId);
    event.setTenantId(tenantId);
    event.setEventType(eventType);
    event.setEventId("evt-" + playerId);
    event.setAmount(new BigDecimal("100.00"));
    event.setPlayerBalance(new BigDecimal("1000.00"));
    event.setDeviceId("dev-001");
    event.setIpAddress("1.2.3.4");
    event.setCountryCode("TW");
    return event;
  }

  /**
   * Set up LiteFlow to populate a single score across all ruleScores entries for a given total.
   * Since no rule weights are configured (empty list), calculateTotalScore returns the average.
   */
  private void setupLiteFlowExecution(RiskEvent event, int scorePerComponent) {
    doAnswer(
            invocation -> {
              RiskContext ctx = invocation.getArgument(1);
              ctx.addRuleScore("velocityCheck", scorePerComponent);
              return null;
            })
        .when(smartFlowExecutor)
        .execute(eq("player-risk-assessment"), any(RiskContext.class));
  }
}
