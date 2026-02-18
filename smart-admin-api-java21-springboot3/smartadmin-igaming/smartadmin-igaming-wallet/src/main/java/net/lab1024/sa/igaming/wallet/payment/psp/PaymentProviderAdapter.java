package net.lab1024.sa.igaming.wallet.payment.psp;

import io.vavr.control.Either;
import io.vavr.control.Option;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspQueryResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspWithdrawResponse;

/**
 * PSP adapter interface — Strategy Pattern.
 *
 * <p>Each PSP implementation provides deposit, withdraw, query, and callback verification. All PSP
 * communication goes through Resilience4j circuit breaker.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
public interface PaymentProviderAdapter {

  /** PSP identifier code (e.g., "stripe", "nuvei", "adyen", "mock"). */
  String getPspCode();

  /**
   * Initiate deposit request to PSP, returns redirect URL.
   *
   * @param request deposit parameters
   * @return Either.left(errorMsg) or Either.right(response with redirectUrl)
   */
  Either<String, PspDepositResponse> deposit(PspDepositRequest request);

  /**
   * Submit withdrawal (payout) request to PSP.
   *
   * @param request withdrawal parameters
   * @return Either.left(errorMsg) or Either.right(response with pspTransactionId)
   */
  Either<String, PspWithdrawResponse> withdraw(PspWithdrawRequest request);

  /**
   * Query transaction status at PSP (for reconciliation).
   *
   * @param pspTransactionId PSP-side transaction identifier
   * @return Option.some(status) or Option.none() if not found
   */
  Option<PspQueryResponse> queryStatus(String pspTransactionId);

  /**
   * Verify PSP callback signature.
   *
   * @param payload raw callback request body
   * @param signature value from X-PSP-Signature header
   * @param timestamp callback timestamp for replay attack prevention
   * @return true if signature is valid and timestamp is within tolerance
   */
  boolean verifyCallback(String payload, String signature, long timestamp);
}
