package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Game module error codes.
 *
 * <p>Code range 30400-30499 is reserved for game-related errors.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum GameErrorCode {

  // --- Provider (30400-30409) ---
  PROVIDER_NOT_FOUND(30400, "Game provider does not exist"),
  PROVIDER_DISABLED(30401, "Game provider is currently disabled"),
  PROVIDER_NOT_SUPPORTED(30402, "Unsupported game provider"),

  // --- Game (30410-30419) ---
  GAME_NOT_FOUND(30410, "Game does not exist"),
  GAME_DISABLED(30411, "Game is currently disabled"),

  // --- Round (30420-30429) ---
  ROUND_NOT_FOUND(30420, "Game round does not exist"),
  ROUND_ALREADY_SETTLED(30421, "Game round already settled"),
  DUPLICATE_TRANSACTION(30422, "Duplicate transaction ID"),

  // --- Callback (30430-30439) ---
  INVALID_SIGNATURE(30430, "Invalid GP callback signature"),
  DEBIT_FAILED(30431, "Debit operation failed"),
  CREDIT_FAILED(30432, "Credit operation failed"),
  ROLLBACK_FAILED(30433, "Rollback operation failed"),

  // --- Balance (30440-30449) ---
  INSUFFICIENT_BALANCE(30440, "Insufficient balance for bet"),

  // --- Reconciliation (30450-30459) ---
  RECONCILIATION_FAILED(30450, "Reconciliation failed"),
  AMOUNT_MISMATCH(30451, "Transaction amount mismatch"),
  ;

  private final int code;
  private final String msg;
}
