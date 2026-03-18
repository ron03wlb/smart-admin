package net.lab1024.sa.igaming.integration.payment;

import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.integration.payment.domain.form.DepositCallbackForm;
import net.lab1024.sa.igaming.integration.payment.domain.vo.DepositResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deposit callback controller - PSP webhook endpoint for deposit confirmations.
 *
 * <p>This controller exposes a public webhook endpoint for PSP (Payment Service Providers) to
 * notify the platform of deposit completions. It does not require authentication as it is designed
 * for external PSP systems.
 *
 * <p><b>Security Notes:</b>
 *
 * <ul>
 *   <li>{@code @SaIgnore} - No authentication required (PSP webhook endpoint)
 *   <li>HMAC-SHA256 signature verification is performed by PaymentService
 *   <li>IP whitelisting should be configured at API gateway level
 *   <li>Rate limiting should be applied to prevent abuse
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 * @see FirstDepositBonusIntegrationService
 */
@Tag(name = "iGaming - Deposit Callback Integration")
@RestController
@RequestMapping("/igaming/integration/payment")
@RequiredArgsConstructor
public class DepositCallbackController {

  private final FirstDepositBonusIntegrationService firstDepositBonusIntegrationService;

  /**
   * Process PSP deposit callback with first deposit bonus check.
   *
   * <p>This endpoint is called by PSP systems when a deposit is completed. It orchestrates:
   *
   * <ol>
   *   <li>PSP signature verification (HMAC-SHA256)
   *   <li>CASH wallet credit
   *   <li>First deposit detection
   *   <li>First deposit bonus award (if applicable)
   *   <li>Kafka event publishing
   * </ol>
   *
   * <p><b>Success Response Example:</b>
   *
   * <pre>
   * {
   *   "ok": true,
   *   "code": 1,
   *   "data": {
   *     "orderNo": "DEP-20260318-001",
   *     "playerId": 1001,
   *     "walletId": 2001,
   *     "depositAmount": "100.0000",
   *     "cashBalance": "100.0000",
   *     "isFirstDeposit": true,
   *     "bonusAmount": "50.0000",
   *     "bonusWalletId": 2002,
   *     "bonusBalance": "50.0000",
   *     "promotionCode": "FIRST_DEPOSIT_50"
   *   }
   * }
   * </pre>
   *
   * <p><b>Error Response Example:</b>
   *
   * <pre>
   * {
   *   "ok": false,
   *   "code": 40001,
   *   "msg": "Invalid signature"
   * }
   * </pre>
   *
   * @param form deposit callback form (orderNo, pspTransactionId, signature, payload)
   * @return deposit result with bonus information
   */
  @Operation(summary = "Process deposit callback with first deposit bonus")
  @PostMapping("/deposit-callback")
  @SaIgnore
  public ResponseDTO<DepositResultVO> processDepositCallback(
      @Valid @RequestBody DepositCallbackForm form) {
    return firstDepositBonusIntegrationService.processFirstDeposit(form);
  }
}
