package net.lab1024.sa.igaming.activity.turnover.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.annotation.NoNeedLogin;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService.TurnoverCalculationResult;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Turnover calculation controller — provides REST API for testing turnover calculation logic.
 *
 * <p>This controller is primarily for testing and demonstration purposes. In production, turnover
 * calculation is triggered by bet settlement events (Kafka consumer).
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class TurnoverCalculationController {

  private final TurnoverCalculationService calculationService;

  @Operation(summary = "Calculate turnover for a bet (testing endpoint)")
  @PostMapping("/igaming/activity/turnover/calculate")
  @NoNeedLogin // Temporarily bypass authentication for testing
  // @SaCheckPermission("activity:turnover:calculate")  // Re-enable after testing
  public ResponseDTO<TurnoverCalculationResult> calculate(
      @RequestBody @Valid TurnoverCalculationForm form) {

    return calculationService.calculate(
        form.getBetId(),
        form.getPlayerId(),
        form.getTenantId(),
        form.getBetAmount(),
        form.getGameCategory(),
        form.getSettlementStatus(),
        form.getOddsValue(),
        form.getOddsType(),
        form.getRiskScore());
  }

  /**
   * Turnover calculation request form.
   *
   * <p>Input parameters for turnover calculation testing.
   */
  @Data
  public static class TurnoverCalculationForm {

    @NotBlank(message = "Bet ID cannot be blank")
    private String betId;

    @NotNull(message = "Player ID cannot be null")
    @Min(value = 1, message = "Player ID must be positive")
    private Long playerId;

    @NotNull(message = "Tenant ID cannot be null")
    @Min(value = 1, message = "Tenant ID must be positive")
    private Long tenantId;

    @NotNull(message = "Bet amount cannot be null")
    @DecimalMin(value = "0.01", message = "Bet amount must be at least 0.01")
    private BigDecimal betAmount;

    @NotNull(message = "Game category cannot be null")
    @Min(value = 1, message = "Game category must be between 1 and 6")
    @Max(value = 6, message = "Game category must be between 1 and 6")
    private Integer gameCategory;

    @NotNull(message = "Settlement status cannot be null")
    @Min(value = 1, message = "Settlement status must be between 1 and 9")
    @Max(value = 9, message = "Settlement status must be between 1 and 9")
    private Integer settlementStatus;

    @NotNull(message = "Odds value cannot be null")
    @DecimalMin(value = "0.01", message = "Odds value must be at least 0.01")
    private BigDecimal oddsValue;

    @NotNull(message = "Odds type cannot be null")
    @Min(value = 1, message = "Odds type must be between 1 and 4")
    @Max(value = 4, message = "Odds type must be between 1 and 4")
    private Integer oddsType;

    @Min(value = 0, message = "Risk score must be between 0 and 100")
    @Max(value = 100, message = "Risk score must be between 0 and 100")
    private Integer riskScore;
  }
}
