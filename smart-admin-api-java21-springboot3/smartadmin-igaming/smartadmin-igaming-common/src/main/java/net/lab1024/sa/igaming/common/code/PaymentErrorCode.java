package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Payment module error codes.
 *
 * <p>Code range 30200-30299 is reserved for payment-related errors. Uses the same standalone enum
 * pattern as {@link WalletErrorCode} (does not implement sealed {@code ErrorCode} interface).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum PaymentErrorCode {

  // --- PSP (30200-30209) ---
  PSP_NOT_FOUND(30200, "PSP does not exist"),
  PSP_UNAVAILABLE(30201, "PSP is currently unavailable"),

  // --- Order Not Found / Duplicate (30210-30219) ---
  PAYMENT_ORDER_NOT_FOUND(30210, "Payment order does not exist"),
  DUPLICATE_REQUEST(30211, "Duplicate payment request"),

  // --- Order Validation (30220-30229) ---
  INVALID_ORDER_TYPE(30220, "Invalid payment order type"),
  INVALID_ORDER_STATUS(30221, "Invalid payment order status transition"),

  // --- Amount Validation (30230-30239) ---
  DEPOSIT_AMOUNT_OUT_OF_RANGE(30230, "Deposit amount out of allowed range"),
  WITHDRAWAL_AMOUNT_OUT_OF_RANGE(30231, "Withdrawal amount out of allowed range"),

  // --- Callback Security (30240-30249) ---
  CALLBACK_SIGNATURE_INVALID(30240, "Callback signature verification failed"),
  CALLBACK_TIMESTAMP_EXPIRED(30241, "Callback timestamp expired"),

  // --- Reconciliation (30250-30259) ---
  RECONCILIATION_NOT_FOUND(30250, "Reconciliation record not found"),
  RECONCILIATION_ALREADY_COMPLETED(30251, "Reconciliation already completed for this date"),
  PSP_REPORT_UNAVAILABLE(30252, "PSP reconciliation report unavailable"),
  ;

  private final int code;
  private final String msg;
}
