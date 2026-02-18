package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wallet module error codes.
 *
 * <p>Code range 30100-30199 is reserved for wallet-related errors. This enum does not implement the
 * sealed {@code ErrorCode} interface (which requires permitted types in the same package). Use
 * {@code ResponseDTO.userErrorParam(WalletErrorCode.XXX.getMsg())} for error responses.
 *
 * @author iGaming Team
 * @since 4.1.0
 */
@Getter
@AllArgsConstructor
public enum WalletErrorCode {

  // ─── Resource Not Found (30100-30109) ───
  WALLET_NOT_FOUND(30100, "Wallet does not exist"),
  LOCK_NOT_FOUND(30106, "Lock record does not exist"),

  // ─── Duplicate Resource (30110-30119) ───
  WALLET_ALREADY_EXISTS(30110, "Wallet already exists for this player and type"),

  // ─── Transaction Validation (30120-30129) ───
  INVALID_CREDIT_TYPE(30120, "Invalid transaction type for credit operation"),
  INVALID_DEBIT_TYPE(30121, "Invalid transaction type for debit operation"),

  // ─── Balance Validation (30130-30139) ───
  INSUFFICIENT_BALANCE(30130, "Insufficient available balance"),
  INSUFFICIENT_BALANCE_FOR_LOCK(30131, "Insufficient available balance for locking"),

  // ─── Data Consistency (30140-30149) ───
  LOCKED_AMOUNT_INCONSISTENCY(30140, "Locked amount inconsistency detected"),
  ;

  private final int code;
  private final String msg;
}
