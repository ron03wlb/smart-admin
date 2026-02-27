package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Risk module error codes.
 *
 * <p>Code range 30500-30599 is reserved for risk-related errors.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum RiskErrorCode {

  // --- Rule Configuration (30500-30509) ---
  RULE_NOT_FOUND(30500, "Risk rule does not exist"),
  RULE_ALREADY_EXISTS(30501, "Risk rule already exists"),

  // --- Assessment (30510-30519) ---
  ASSESSMENT_FAILED(30510, "Risk assessment failed"),
  ASSESSMENT_NOT_FOUND(30511, "Risk assessment does not exist"),

  // --- Score (30520-30529) ---
  SCORE_NOT_FOUND(30520, "Risk score profile does not exist"),

  // --- Proposal (30530-30539) ---
  PROPOSAL_NOT_FOUND(30530, "Risk proposal does not exist"),
  PROPOSAL_ALREADY_REVIEWED(30531, "Risk proposal already reviewed"),
  PROPOSAL_INVALID_STATUS(30532, "Invalid proposal status transition"),

  // --- Geo Restriction (30540-30549) ---
  GEO_RESTRICTED(30540, "Access restricted from this jurisdiction"),
  ;

  private final int code;
  private final String msg;
}
