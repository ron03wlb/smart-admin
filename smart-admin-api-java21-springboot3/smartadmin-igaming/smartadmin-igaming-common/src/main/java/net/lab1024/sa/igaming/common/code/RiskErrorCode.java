package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Risk module error codes.
 *
 * <p>Code range 30700-30799 is reserved for risk-related errors.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Getter
@AllArgsConstructor
public enum RiskErrorCode {

  // --- Rule Configuration (30700-30709) ---
  RULE_NOT_FOUND(30700, "Risk rule does not exist"),
  RULE_ALREADY_EXISTS(30701, "Risk rule already exists"),

  // --- Assessment (30710-30719) ---
  ASSESSMENT_FAILED(30710, "Risk assessment failed"),
  ASSESSMENT_NOT_FOUND(30711, "Risk assessment does not exist"),

  // --- Score (30720-30729) ---
  SCORE_NOT_FOUND(30720, "Risk score profile does not exist"),

  // --- Proposal (30730-30739) ---
  PROPOSAL_NOT_FOUND(30730, "Risk proposal does not exist"),
  PROPOSAL_ALREADY_REVIEWED(30731, "Risk proposal already reviewed"),
  PROPOSAL_INVALID_STATUS(30732, "Invalid proposal status transition"),

  // --- Geo Restriction (30740-30749) ---
  GEO_RESTRICTED(30740, "Access restricted from this jurisdiction"),
  ;

  private final int code;
  private final String msg;
}
