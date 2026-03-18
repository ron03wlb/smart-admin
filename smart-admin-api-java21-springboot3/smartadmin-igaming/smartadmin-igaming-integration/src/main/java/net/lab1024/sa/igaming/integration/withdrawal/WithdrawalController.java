package net.lab1024.sa.igaming.integration.withdrawal;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Option;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.integration.withdrawal.domain.form.WithdrawalRequestForm;
import net.lab1024.sa.igaming.integration.withdrawal.domain.vo.WithdrawalResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Withdrawal integration controller — player withdrawal API.
 *
 * <p>This controller provides player-facing withdrawal APIs. It uses Sa-Token authentication to
 * ensure the player can only withdraw from their own account.
 *
 * <p><b>Security:</b> All endpoints require Sa-Token authentication. The playerId in the request
 * must match the authenticated player's ID.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Tag(name = "Withdrawal Integration")
@RestController
@RequiredArgsConstructor
public class WithdrawalController {

  private final WithdrawalIntegrationService withdrawalIntegrationService;

  /**
   * Request withdrawal — player applies for cash withdrawal.
   *
   * <p>This endpoint orchestrates the withdrawal application flow:
   *
   * <ol>
   *   <li>Validate player status (ACTIVE required)
   *   <li>Load CASH wallet
   *   <li>Check for active bonuses (block withdrawal if any)
   *   <li>Calculate available balance (balance - lockedAmount)
   *   <li>Create withdrawal order and lock funds
   *   <li>Publish WITHDRAWAL_REQUESTED event (triggers risk check)
   *   <li>Return withdrawal result VO
   * </ol>
   *
   * <p><b>Security:</b> The playerId in the form must match the authenticated player's ID.
   *
   * <p><b>Business Rules:</b>
   *
   * <ul>
   *   <li>Player must be in ACTIVE status
   *   <li>Cannot withdraw while active bonuses exist
   *   <li>Withdrawal amount must not exceed available balance
   *   <li>Minimum withdrawal amount: 0.0001
   * </ul>
   *
   * @param form withdrawal request form
   * @return withdrawal result VO
   */
  @Operation(summary = "Request withdrawal")
  @PostMapping("/igaming/integration/withdrawal/request")
  public ResponseDTO<WithdrawalResultVO> requestWithdrawal(
      @Valid @RequestBody WithdrawalRequestForm form) {
    // Security: verify playerId matches authenticated player
    Long authenticatedPlayerId = StpUtil.getLoginIdAsLong();
    if (!form.getPlayerId().equals(authenticatedPlayerId)) {
      return ResponseDTO.userErrorParam("You can only withdraw from your own account");
    }

    return withdrawalIntegrationService.requestWithdrawal(form);
  }

  /**
   * Get available withdrawal balance — query available balance for withdrawal.
   *
   * <p>This is a convenience endpoint for frontend to display available balance before the player
   * submits a withdrawal request.
   *
   * <p>Available balance = CASH wallet balance - locked amount - active bonus requirements
   *
   * <p><b>Security:</b> The playerId in the path must match the authenticated player's ID.
   *
   * @param playerId player ID
   * @return available balance
   */
  @Operation(summary = "Get available withdrawal balance")
  @GetMapping("/igaming/integration/withdrawal/available-balance/{playerId}")
  public ResponseDTO<BigDecimal> getAvailableBalance(@PathVariable Long playerId) {
    // Security: verify playerId matches authenticated player
    Long authenticatedPlayerId = StpUtil.getLoginIdAsLong();
    if (!playerId.equals(authenticatedPlayerId)) {
      return ResponseDTO.userErrorParam("You can only query your own balance");
    }

    Option<BigDecimal> balanceOpt =
        withdrawalIntegrationService.getAvailableWithdrawalBalance(playerId);

    return balanceOpt
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Wallet not found"));
  }
}
