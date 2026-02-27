package net.lab1024.sa.igaming.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent module error codes.
 *
 * <p>Code range 30600-30699 is reserved for agent-related errors.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Getter
@AllArgsConstructor
public enum AgentErrorCode {

  // --- Credit Network (30600-30619) ---
  CREDIT_NOT_FOUND(30600, "Agent credit record does not exist"),
  CREDIT_INSUFFICIENT(30601, "Insufficient credit available for allocation"),
  CREDIT_RECALL_EXCEEDS_USED(30602, "Cannot recall below used credit amount"),
  CREDIT_ALREADY_FROZEN(30603, "Agent credit is already frozen"),
  CREDIT_VERSION_CONFLICT(30604, "Credit update conflict, please retry"),

  // --- Settlement (30620-30639) ---
  SETTLEMENT_NOT_FOUND(30620, "Settlement record does not exist"),
  SETTLEMENT_ALREADY_EXISTS(30621, "Settlement record already exists for this period"),
  SETTLEMENT_ALREADY_VERIFIED(30622, "Settlement payment already verified"),
  SETTLEMENT_INVALID_PHASE(30623, "Invalid settlement phase transition"),

  // --- Affiliate Agent (30640-30659) ---
  AGENT_NOT_FOUND(30640, "Agent does not exist"),
  AGENT_USERNAME_DUPLICATE(30641, "Agent username already exists"),
  AGENT_REFERRAL_CODE_DUPLICATE(30642, "Referral code already exists"),
  AGENT_ALREADY_FROZEN(30643, "Agent is already frozen"),
  AGENT_HIERARCHY_TOO_DEEP(30644, "Agent hierarchy exceeds maximum depth"),

  // --- Commission (30660-30679) ---
  COMMISSION_PLAN_NOT_FOUND(30660, "Commission plan does not exist"),
  COMMISSION_RECORD_NOT_FOUND(30661, "Commission record does not exist"),
  COMMISSION_ALREADY_APPROVED(30662, "Commission already approved"),
  COMMISSION_ALREADY_REJECTED(30663, "Commission already rejected"),
  COMMISSION_INVALID_STATUS(30664, "Invalid commission status transition"),
  ;

  private final int code;
  private final String msg;
}
