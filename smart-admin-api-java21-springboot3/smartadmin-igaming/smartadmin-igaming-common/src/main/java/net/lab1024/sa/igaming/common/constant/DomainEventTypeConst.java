package net.lab1024.sa.igaming.common.constant;

/**
 * Domain event type constants — shared across producers and consumers.
 *
 * <p>Centralizes all event type strings to prevent magic string duplication. Producers (Manager
 * layer) and consumers (Consumer layer) should both reference these constants.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
public final class DomainEventTypeConst {

  private DomainEventTypeConst() {}

  // --- Game Events (published by GameTransactionManager) ---
  public static final String BET_PLACED = "BET_PLACED";
  public static final String ROUND_SETTLED = "ROUND_SETTLED";
  public static final String BET_CANCELLED = "BET_CANCELLED";
  public static final String ROUND_TIMEOUT = "ROUND_TIMEOUT";
  public static final String ROUND_ADJUSTED = "ROUND_ADJUSTED";
  public static final String ROUND_PENDING_REVIEW = "ROUND_PENDING_REVIEW";

  // --- Wallet Events (published by WalletManager) ---
  public static final String WALLET_CREDITED = "WALLET_CREDITED";
  public static final String WALLET_DEBITED = "WALLET_DEBITED";
  public static final String BONUS_CREDITED = "BONUS_CREDITED";
  public static final String BONUS_DEBITED = "BONUS_DEBITED";
  public static final String BONUS_CONVERTED = "BONUS_CONVERTED";

  // --- Player Events (published by PlayerRegistrationManager, PlayerStateManager) ---
  public static final String PLAYER_REGISTERED = "PLAYER_REGISTERED";
  public static final String PLAYER_STATUS_CHANGED = "PLAYER_STATUS_CHANGED";
  public static final String PLAYER_SUSPENDED = "PLAYER_SUSPENDED";
  public static final String PLAYER_SELF_EXCLUDED = "PLAYER_SELF_EXCLUDED";
  public static final String PLAYER_LOGIN = "PLAYER_LOGIN";
  public static final String KYC_UPDATED = "KYC_UPDATED";

  // --- Activity Events (published by WageringProgressManager) ---
  public static final String WAGERING_COMPLETED = "WAGERING_COMPLETED";

  // --- Agent Events (published by CreditSettlementManager, AffiliateCommissionManager) ---
  public static final String CREDIT_ALLOCATED = "CREDIT_ALLOCATED";
  public static final String CREDIT_RECALLED = "CREDIT_RECALLED";
  public static final String SETTLEMENT_COMPLETED = "SETTLEMENT_COMPLETED";
  public static final String PAYMENT_VERIFIED = "PAYMENT_VERIFIED";
  public static final String COMMISSION_ISSUED = "COMMISSION_ISSUED";
  public static final String COMMISSION_APPROVED = "COMMISSION_APPROVED";
  public static final String COMMISSION_REJECTED = "COMMISSION_REJECTED";
  public static final String AGENT_REGISTERED = "AGENT_REGISTERED";

  // --- Risk Events (consumed by RiskEventConsumer) ---
  public static final String WITHDRAWAL_REQUESTED = "WITHDRAWAL_REQUESTED";
}
