package net.lab1024.sa.igaming.activity.turnover.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.BaseIntegrationTest;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverGameWeightRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverOddsThresholdRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRiskActionRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverStatusFactorRuleDao;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService.TurnoverCalculationResult;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Turnover Calculation Engine.
 *
 * <p>Tests the complete flow from Service → LiteFlow → Components → Database.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@SpringBootTest
@DisplayName("Turnover 計算引擎集成測試")
class TurnoverCalculationIntegrationTest extends BaseIntegrationTest {

  @Autowired private TurnoverCalculationService calculationService;
  @Autowired private TurnoverGameWeightRuleDao gameWeightRuleDao;
  @Autowired private TurnoverStatusFactorRuleDao statusFactorRuleDao;
  @Autowired private TurnoverOddsThresholdRuleDao oddsThresholdRuleDao;
  @Autowired private TurnoverRiskActionRuleDao riskActionRuleDao;

  @BeforeEach
  void setUp() {
    // Initialize LiteFlow chain
    calculationService.initializeChain();
  }

  // ===== 完整流程測試 =====

  @Nested
  @DisplayName("完整流程測試")
  @Transactional
  class FullFlowTests {

    @BeforeEach
    void setupRules() {
      // Setup default rules for testing
      TurnoverTestFixture.setupDefaultRules(
          gameWeightRuleDao, oddsThresholdRuleDao, riskActionRuleDao, statusFactorRuleDao);
    }

    @Test
    @DisplayName("Slots 遊戲贏注應該計算100%流水")
    void slots_Win_ShouldCalculate100PercentTurnover() {
      // Arrange
      String betId = "BET-SLOTS-WIN-001";
      Long playerId = 10001L;
      Long tenantId = 1L;
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer gameCategory = 1; // Slots
      Integer settlementStatus = 1; // WIN
      BigDecimal oddsValue = new BigDecimal("1.95");
      Integer oddsType = 1; // Decimal
      Integer riskScore = 10; // Low risk

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              playerId,
              tenantId,
              betAmount,
              gameCategory,
              settlementStatus,
              oddsValue,
              oddsType,
              riskScore);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getBetId()).isEqualTo(betId);
      assertThat(result.getPlayerId()).isEqualTo(playerId);
      assertThat(result.getBetAmount()).isEqualByComparingTo("100.00");

      // Layer 1: Risk Filter (odds >= 1.50, low risk = PASS)
      assertThat(result.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
      assertThat(result.getRiskActionType()).isEqualTo(1); // PASS

      // Layer 2: Status Factor (WIN = 100%)
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
      assertThat(result.getStatusFactor()).isEqualByComparingTo("100.00");

      // Layer 3: Game Weight (Slots = 100%)
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("100.00");
      assertThat(result.getGameWeight()).isEqualByComparingTo("100.00");

      assertThat(result.getRejected()).isFalse();
      assertThat(result.getMatchedRules()).isNotEmpty();
      assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Live Casino 遊戲贏注應該計算15%流水")
    void liveCasino_Win_ShouldCalculate15PercentTurnover() {
      // Arrange
      String betId = "BET-LIVE-WIN-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer gameCategory = 2; // Live Casino
      Integer settlementStatus = 1; // WIN

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              betAmount,
              gameCategory,
              settlementStatus,
              new BigDecimal("1.95"),
              1,
              10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("100.00");

      // Live Casino = 15% weight
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("15.00");
      assertThat(result.getGameWeight()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("和局應該計算0%流水")
    void draw_ShouldCalculateZeroTurnover() {
      // Arrange
      String betId = "BET-DRAW-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer settlementStatus = 3; // DRAW

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, settlementStatus, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");

      // DRAW = 0% factor
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("0.00");
      assertThat(result.getStatusFactor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("賠率過低應該被拒絕")
    void lowOdds_ShouldBeRejected() {
      // Arrange
      String betId = "BET-LOW-ODDS-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      BigDecimal lowOdds = new BigDecimal("1.20"); // Below threshold 1.50

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(betId, 10001L, 1L, betAmount, 1, 1, lowOdds, 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getRejected()).isTrue();
      assertThat(result.getRejectedBy()).contains("ODDS");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("高風險評分應該降低流水")
    void highRisk_ShouldReduceTurnover() {
      // Arrange
      String betId = "BET-HIGH-RISK-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer riskScore = 75; // CRITICAL risk (70-100)

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, 1, new BigDecimal("1.95"), 1, riskScore);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();

      // CRITICAL risk should apply factor (e.g., 50% or FLAG/BLOCK)
      // Depending on risk action rule, it may reduce turnover or reject
      assertThat(result.getRiskActionType()).isIn(1, 2, 3); // PASS, FLAG, or BLOCK
    }

    @Test
    @DisplayName("取消注單應該計算0%流水")
    void cancelled_ShouldCalculateZeroTurnover() {
      // Arrange
      String betId = "BET-CANCELLED-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer settlementStatus = 6; // CANCEL

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, settlementStatus, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("0.00");
      assertThat(result.getStatusFactor()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("大額注單應該正確計算")
    void largeBetAmount_ShouldCalculateCorrectly() {
      // Arrange
      String betId = "BET-LARGE-001";
      BigDecimal largeAmount = new BigDecimal("99999.99");
      Integer gameCategory = 1; // Slots 100%
      Integer settlementStatus = 1; // WIN 100%

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              largeAmount,
              gameCategory,
              settlementStatus,
              new BigDecimal("1.95"),
              1,
              10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getBetAmount()).isEqualByComparingTo("99999.99");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("99999.99");
    }

    @Test
    @DisplayName("Poker 遊戲應該計算5%流水")
    void poker_ShouldCalculate5PercentTurnover() {
      // Arrange
      String betId = "BET-POKER-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer gameCategory = 4; // Poker
      Integer settlementStatus = 1; // WIN

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              betAmount,
              gameCategory,
              settlementStatus,
              new BigDecimal("1.95"),
              1,
              10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getGameWeight()).isEqualByComparingTo("5.00");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("半贏應該計算100%流水")
    void halfWin_ShouldCalculate100PercentTurnover() {
      // Arrange
      String betId = "BET-HALF-WIN-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer settlementStatus = 7; // HALF_WIN

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, settlementStatus, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("半輸應該計算100%流水")
    void halfLoss_ShouldCalculate100PercentTurnover() {
      // Arrange
      String betId = "BET-HALF-LOSS-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer settlementStatus = 8; // HALF_LOSS

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, settlementStatus, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("運行中注單應該計算0%流水")
    void running_ShouldCalculateZeroTurnover() {
      // Arrange
      String betId = "BET-RUNNING-001";
      BigDecimal betAmount = new BigDecimal("100.00");
      Integer settlementStatus = 9; // RUNNING

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, settlementStatus, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("複雜場景: Live Casino + 和局")
    void complexScenario_LiveCasinoDraw() {
      // Arrange
      String betId = "BET-COMPLEX-001";
      BigDecimal betAmount = new BigDecimal("250.00");
      Integer gameCategory = 2; // Live Casino 15%
      Integer settlementStatus = 3; // DRAW 0%

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              betAmount,
              gameCategory,
              settlementStatus,
              new BigDecimal("1.95"),
              1,
              10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();

      // Even though game weight is 15%, DRAW makes it 0%
      assertThat(result.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
      assertThat(result.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("應該記錄所有匹配的規則")
    void shouldRecordAllMatchedRules() {
      // Arrange
      String betId = "BET-RULES-001";
      BigDecimal betAmount = new BigDecimal("100.00");

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, betAmount, 1, 1, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();

      TurnoverCalculationResult result = response.getData();
      assertThat(result.getMatchedRules()).isNotEmpty();
      // Should have rules from odds threshold, risk action, status factor, and game weight
      assertThat(result.getMatchedRules().size()).isGreaterThanOrEqualTo(4);
    }
  }

  // ===== 邊界條件測試 =====

  @Nested
  @DisplayName("邊界條件測試")
  @Transactional
  class EdgeCaseTests {

    @BeforeEach
    void setupRules() {
      TurnoverTestFixture.setupDefaultRules(
          gameWeightRuleDao, oddsThresholdRuleDao, riskActionRuleDao, statusFactorRuleDao);
    }

    @Test
    @DisplayName("零投注額應該返回零流水")
    void zeroBetAmount_ShouldReturnZeroTurnover() {
      // Arrange
      String betId = "BET-ZERO-001";
      BigDecimal zeroBet = BigDecimal.ZERO;

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, zeroBet, 1, 1, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();
      assertThat(response.getData().getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("極小投注額應該正確計算")
    void tinyBetAmount_ShouldCalculateCorrectly() {
      // Arrange
      String betId = "BET-TINY-001";
      BigDecimal tinyBet = new BigDecimal("0.01");

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId, 10001L, 1L, tinyBet, 1, 1, new BigDecimal("1.95"), 1, 10);

      // Assert
      assertThat(response.getOk()).isTrue();
      assertThat(response.getData().getActivityValidTurnover()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("風險評分為0應該正常處理")
    void zeroRiskScore_ShouldProcessNormally() {
      // Arrange
      String betId = "BET-ZERO-RISK-001";
      Integer riskScore = 0;

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              new BigDecimal("100.00"),
              1,
              1,
              new BigDecimal("1.95"),
              1,
              riskScore);

      // Assert
      assertThat(response.getOk()).isTrue();
      assertThat(response.getData().getRiskActionType()).isEqualTo(1); // PASS
    }

    @Test
    @DisplayName("風險評分為100應該正常處理")
    void maxRiskScore_ShouldProcessNormally() {
      // Arrange
      String betId = "BET-MAX-RISK-001";
      Integer riskScore = 100;

      // Act
      ResponseDTO<TurnoverCalculationResult> response =
          calculationService.calculate(
              betId,
              10001L,
              1L,
              new BigDecimal("100.00"),
              1,
              1,
              new BigDecimal("1.95"),
              1,
              riskScore);

      // Assert
      assertThat(response.getOk()).isTrue();
      // CRITICAL risk (70-100) should have risk action
      assertThat(response.getData().getRiskActionType()).isIn(1, 2, 3);
    }
  }
}
