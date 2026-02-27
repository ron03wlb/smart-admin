package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Activity module error codes.
 *
 * <p>Code range 30500-30599 is reserved for activity-related errors.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum ActivityErrorCode {

  // --- Promotion (30500-30509) ---
  PROMOTION_NOT_FOUND(30500, "Promotion rule does not exist"),
  PROMOTION_DISABLED(30501, "Promotion is currently disabled"),
  PROMOTION_EXPIRED(30502, "Promotion has expired"),
  PROMOTION_CODE_EXISTS(30503, "Promotion code already exists"),

  // --- Bonus (30510-30519) ---
  ALREADY_CLAIMED(30510, "Bonus already claimed with this claim ID"),
  MAX_CLAIMS_REACHED(30511, "Maximum claim count reached for this promotion"),
  INELIGIBLE(30512, "Player is not eligible for this promotion"),
  BONUS_NOT_FOUND(30513, "Bonus record does not exist"),

  // --- Wagering (30520-30529) ---
  WAGERING_NOT_MET(30520, "Wagering requirement not met"),
  WAGERING_UPDATE_FAILED(30521, "Failed to update wagering progress"),

  // --- VIP (30530-30539) ---
  VIP_EVALUATION_FAILED(30530, "VIP level evaluation failed"),
  ;

  private final int code;
  private final String msg;
}
