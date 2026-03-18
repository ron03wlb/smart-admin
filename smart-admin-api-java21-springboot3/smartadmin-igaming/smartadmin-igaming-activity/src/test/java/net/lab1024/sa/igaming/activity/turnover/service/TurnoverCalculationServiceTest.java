package net.lab1024.sa.igaming.activity.turnover.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.code.SystemErrorCode;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService.TurnoverCalculationResult;
import net.lab1024.sa.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.manager.LiteFlowChainManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TurnoverCalculationService}.
 *
 * <p>Tests the service layer orchestration including:
 *
 * <ul>
 *   <li>LiteFlow chain initialization on application startup
 *   <li>Turnover calculation flow with mocked LiteFlow execution
 *   <li>Result building and error handling
 *   <li>Null value handling and edge cases
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoverCalculationService 單元測試")
class TurnoverCalculationServiceTest {

  @Mock private SmartFlowExecutor flowExecutor;
  @Mock private LiteFlowChainManager chainManager;

  private TurnoverCalculationService service;

  @BeforeEach
  void setUp() {
    service = new TurnoverCalculationService(flowExecutor, chainManager);
  }

  // ===== 鏈初始化測試 =====

  @Nested
  @DisplayName("鏈初始化")
  class ChainInitializationTests {

    @Test
    @DisplayName("當應用啟動時應該初始化鏈")
    void whenAppStarts_ShouldInitializeChain() {
      // Arrange
      doThrow(new RuntimeException("Chain not found")).when(flowExecutor).reloadRule();
      when(chainManager.add(any(LiteFlowChainAddForm.class), anyLong(), anyString()))
          .thenReturn(ResponseDTO.ok("Chain added successfully"));

      // Act
      service.initializeChain();

      // Assert
      verify(flowExecutor, times(1)).reloadRule();
      verify(chainManager, times(1)).add(any(LiteFlowChainAddForm.class), eq(0L), eq("SYSTEM"));
    }

    @Test
    @DisplayName("當鏈已存在時應該跳過初始化")
    void whenChainAlreadyExists_ShouldSkipInitialization() {
      // Arrange
      doNothing().when(flowExecutor).reloadRule(); // Simulate chain exists

      // Act
      service.initializeChain();

      // Assert
      verify(flowExecutor, times(1)).reloadRule();
      verify(chainManager, never()).add(any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("當初始化失敗時應該記錄錯誤但不拋出異常")
    void whenInitializationFails_ShouldLogErrorButNotThrow() {
      // Arrange
      doThrow(new RuntimeException("Chain not found")).when(flowExecutor).reloadRule();
      when(chainManager.add(any(LiteFlowChainAddForm.class), anyLong(), anyString()))
          .thenReturn(ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "Failed to add chain"));

      // Act & Assert - Should not throw exception
      service.initializeChain();

      // Verify manager was called even though it returned error
      verify(chainManager, times(1)).add(any(LiteFlowChainAddForm.class), eq(0L), eq("SYSTEM"));
    }
  }

  // ===== 流水計算測試 =====

  @Nested
  @DisplayName("流水計算")
  class TurnoverCalculationTests {

    @Test
    @DisplayName("當輸入有效時應該執行鏈並返回結果")
    void whenValidInput_ShouldExecuteChainAndReturnResult() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                // Simulate chain execution setting values
                ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
                ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
                ctx.setActivityValidTurnover(new BigDecimal("100.00"));
                ctx.setRiskActionType(1);
                ctx.setStatusFactor(new BigDecimal("100.00"));
                ctx.setGameWeight(new BigDecimal("100.00"));
                return null;
              })
          .when(flowExecutor)
          .execute(eq("turnover_calculation_main"), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-001", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getBetId()).isEqualTo("BET-001");
      assertThat(result.getData().getPlayerId()).isEqualTo(1001L);
      assertThat(result.getData().getBetAmount()).isEqualByComparingTo("100.00");
      assertThat(result.getData().getActivityValidTurnover()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當風險評分為null時應該默認為0")
    void whenRiskScoreNull_ShouldDefaultToZero() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                assertThat(ctx.getRiskScore()).isEqualTo(0);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      service.calculate(
          "BET-002", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, null);

      // Assert
      verify(flowExecutor, times(1)).execute(anyString(), any(TurnoverContext.class));
    }

    @Test
    @DisplayName("當鏈執行失敗時應該返回錯誤")
    void whenChainExecutionFails_ShouldReturnError() {
      // Arrange
      doThrow(new RuntimeException("LiteFlow execution failed"))
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-003", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getOk()).isFalse();
      assertThat(result.getCode()).isEqualTo(SystemErrorCode.SYSTEM_ERROR.getCode());
      assertThat(result.getMsg()).contains("Turnover calculation failed");
    }

    @Test
    @DisplayName("應該從上下文構建結果")
    void shouldBuildResultFromContext() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
                ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
                ctx.setActivityValidTurnover(new BigDecimal("100.00"));
                ctx.setRiskActionType(1);
                ctx.setStatusFactor(new BigDecimal("100.00"));
                ctx.setGameWeight(new BigDecimal("100.00"));
                ctx.addMatchedRule("RULE-001");
                ctx.addMatchedRule("RULE-002");
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-004", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      TurnoverCalculationResult data = result.getData();
      assertThat(data.getBetId()).isEqualTo("BET-004");
      assertThat(data.getPlayerId()).isEqualTo(1001L);
      assertThat(data.getBetAmount()).isEqualByComparingTo("100.00");
      assertThat(data.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
      assertThat(data.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
      assertThat(data.getActivityValidTurnover()).isEqualByComparingTo("100.00");
      assertThat(data.getRiskActionType()).isEqualTo(1);
      assertThat(data.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(data.getGameWeight()).isEqualByComparingTo("100.00");
      assertThat(data.getMatchedRules()).hasSize(2);
      assertThat(data.getRejected()).isFalse();
    }

    @Test
    @DisplayName("當拒絕時結果應該反映拒絕狀態")
    void whenRejected_ResultShouldReflectRejection() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.markRejected("ODDS_TOO_LOW");
                ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
                ctx.setValidTurnoverFinance(BigDecimal.ZERO);
                ctx.setActivityValidTurnover(BigDecimal.ZERO);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-005", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.20"), 1, 0);

      // Assert
      TurnoverCalculationResult data = result.getData();
      assertThat(data.getRejected()).isTrue();
      assertThat(data.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
      assertThat(data.getActivityValidTurnover()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("應該填充匹配的規則")
    void shouldPopulateMatchedRules() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.addMatchedRule("ODDS-THRESHOLD-001");
                ctx.addMatchedRule("RISK-ACTION-001");
                ctx.addMatchedRule("STATUS-FACTOR-001");
                ctx.addMatchedRule("GAME-WEIGHT-001");
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-006", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getData().getMatchedRules()).hasSize(4);
      assertThat(result.getData().getMatchedRules())
          .containsExactly(
              "ODDS-THRESHOLD-001", "RISK-ACTION-001", "STATUS-FACTOR-001", "GAME-WEIGHT-001");
    }

    @Test
    @DisplayName("應該處理大數值")
    void shouldHandleLargeDecimalValues() {
      // Arrange
      BigDecimal largeAmount = new BigDecimal("999999999.99");
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.setEffectiveTurnoverBase(largeAmount);
                ctx.setValidTurnoverFinance(largeAmount);
                ctx.setActivityValidTurnover(largeAmount);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate("BET-007", 1001L, 1L, largeAmount, 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBetAmount()).isEqualByComparingTo(largeAmount);
      assertThat(result.getData().getActivityValidTurnover()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("應該處理零投注額")
    void shouldHandleZeroBetAmount() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
                ctx.setValidTurnoverFinance(BigDecimal.ZERO);
                ctx.setActivityValidTurnover(BigDecimal.ZERO);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-008", 1001L, 1L, BigDecimal.ZERO, 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBetAmount()).isEqualByComparingTo("0");
      assertThat(result.getData().getActivityValidTurnover()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("應該正確設置所有上下文字段")
    void shouldSetAllContextFieldsCorrectly() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                // Verify all fields are set correctly
                assertThat(ctx.getBetId()).isEqualTo("BET-009");
                assertThat(ctx.getPlayerId()).isEqualTo(1001L);
                assertThat(ctx.getTenantId()).isEqualTo(1L);
                assertThat(ctx.getBetAmount()).isEqualByComparingTo("250.50");
                assertThat(ctx.getGameCategory()).isEqualTo(2);
                assertThat(ctx.getSettlementStatus()).isEqualTo(1);
                assertThat(ctx.getOddsValue()).isEqualByComparingTo("1.95");
                assertThat(ctx.getOddsType()).isEqualTo(1);
                assertThat(ctx.getRiskScore()).isEqualTo(25);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      service.calculate(
          "BET-009", 1001L, 1L, new BigDecimal("250.50"), 2, 1, new BigDecimal("1.95"), 1, 25);

      // Assert
      verify(flowExecutor, times(1)).execute(anyString(), any(TurnoverContext.class));
    }

    @Test
    @DisplayName("應該處理高風險評分")
    void shouldHandleHighRiskScore() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                assertThat(ctx.getRiskScore()).isEqualTo(95);
                ctx.markRejected("RISK_SCORE_HIGH");
                ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-010", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 95);

      // Assert
      assertThat(result.getData().getRejected()).isTrue();
      assertThat(result.getData().getRejectedBy()).isEqualTo("RISK_SCORE_HIGH");
    }

    @Test
    @DisplayName("應該處理和局狀態")
    void shouldHandleDrawStatus() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                assertThat(ctx.getSettlementStatus()).isEqualTo(3); // DRAW
                ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
                ctx.setValidTurnoverFinance(BigDecimal.ZERO); // DRAW = 0% factor
                ctx.setActivityValidTurnover(BigDecimal.ZERO);
                ctx.setStatusFactor(BigDecimal.ZERO);
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-011",
              1001L,
              1L,
              new BigDecimal("100.00"),
              1,
              3, // DRAW
              new BigDecimal("1.95"),
              1,
              0);

      // Assert
      assertThat(result.getData().getValidTurnoverFinance()).isEqualByComparingTo("0");
      assertThat(result.getData().getActivityValidTurnover()).isEqualByComparingTo("0");
      assertThat(result.getData().getStatusFactor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("應該處理多個遊戲類別")
    void shouldHandleMultipleGameCategories() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                assertThat(ctx.getGameCategory()).isEqualTo(2); // Live Casino
                ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
                ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
                ctx.setActivityValidTurnover(new BigDecimal("15.00")); // 15% weight
                ctx.setGameWeight(new BigDecimal("15.00"));
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-012",
              1001L,
              1L,
              new BigDecimal("100.00"),
              2, // Live Casino
              1,
              new BigDecimal("1.95"),
              1,
              0);

      // Assert
      assertThat(result.getData().getGameWeight()).isEqualByComparingTo("15.00");
      assertThat(result.getData().getActivityValidTurnover()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("應該設置計算時間戳")
    void shouldSetCalculationTimestamp() {
      // Arrange
      doAnswer(
              invocation -> {
                TurnoverContext ctx = invocation.getArgument(1);
                ctx.setCalculatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
                return null;
              })
          .when(flowExecutor)
          .execute(anyString(), any(TurnoverContext.class));

      // Act
      ResponseDTO<TurnoverCalculationResult> result =
          service.calculate(
              "BET-013", 1001L, 1L, new BigDecimal("100.00"), 1, 1, new BigDecimal("1.95"), 1, 0);

      // Assert
      assertThat(result.getData().getCalculatedAt()).isNotNull();
    }
  }
}
