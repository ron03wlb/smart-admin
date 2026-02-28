package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Player module error codes.
 *
 * <p>Code range 30300-30399 is reserved for player-related errors. This enum does not implement the
 * sealed {@code ErrorCode} interface (which requires permitted types in the same package). Use
 * {@code ResponseDTO.userErrorParam(PlayerErrorCode.XXX.getMsg())} for error responses.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum PlayerErrorCode {

  // --- Resource Not Found (30300-30309) ---
  PLAYER_NOT_FOUND(30300, "Player does not exist"),

  // --- Duplicate Resource (30301-30309) ---
  PLAYER_ALREADY_EXISTS(30301, "Username already exists"),

  // --- Account Status (30302-30309) ---
  PLAYER_LOCKED(30302, "Player account is locked"),
  PLAYER_SUSPENDED(30303, "Player account is suspended"),
  PLAYER_CLOSED(30304, "Player account is closed"),

  // --- Status Transition (30310-30319) ---
  INVALID_STATUS_TRANSITION(30310, "Invalid player status transition"),

  // --- Authentication (30320-30329) ---
  INVALID_CREDENTIALS(30320, "Invalid username or password"),
  EMAIL_ALREADY_EXISTS(30321, "Email already registered"),
  PHONE_ALREADY_EXISTS(30322, "Phone already registered"),

  // --- KYC (30330-30339) ---
  KYC_LEVEL_INSUFFICIENT(30330, "KYC level insufficient for this operation"),
  KYC_DOCUMENT_NOT_FOUND(30331, "KYC document does not exist"),

  // --- Parameter Validation (30340-30349) ---
  INVALID_STATUS_VALUE(30340, "Invalid player status value"),
  INVALID_VIP_LEVEL_VALUE(30341, "Invalid VIP level value"),
  INVALID_DOCUMENT_TYPE(30342, "Invalid KYC document type"),
  ;

  private final int code;
  private final String msg;
}
