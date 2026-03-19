package net.lab1024.sa.igaming.integration.journey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionTypeEnum;
import net.lab1024.sa.igaming.integration.payment.domain.form.DepositCallbackForm;
import net.lab1024.sa.igaming.integration.player.domain.form.PlayerRegistrationIntegrationForm;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;

/**
 * Player registration test fixture - Builder patterns for test data construction.
 *
 * <p>Provides factory methods to create test entities with sensible defaults. Eliminates test data
 * duplication and ensures consistency across test classes.
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * // Create player registration form
 * PlayerRegistrationIntegrationForm form = PlayerRegistrationTestFixture.createRegistrationForm("testuser");
 *
 * // Create deposit callback form
 * DepositCallbackForm depositForm = PlayerRegistrationTestFixture.createDepositCallbackForm("DEP-001", "PSP-TX-001");
 *
 * // Setup first deposit bonus rule
 * PromotionRuleEntity rule = PlayerRegistrationTestFixture.createFirstDepositBonusRule();
 * promotionRuleDao.insert(rule);
 * </pre>
 *
 * <p><b>Thread Safety:</b> Uses {@link AtomicInteger} for unique ID generation across concurrent
 * tests.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
public class PlayerRegistrationTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  // ===== Form Builders =====

  /**
   * Create a player registration form with default values.
   *
   * @param username username (must be unique for each test)
   * @return player registration form ready for service invocation
   */
  public static PlayerRegistrationIntegrationForm createRegistrationForm(String username) {
    PlayerRegistrationIntegrationForm form = new PlayerRegistrationIntegrationForm();
    form.setUsername(username);
    form.setPassword("Test@1234");
    form.setEmail(username + "@test.com");
    form.setPhone("+86-138-0000-" + String.format("%04d", counter.incrementAndGet()));
    form.setRegistrationIp("127.0.0.1");
    form.setCurrencyCode("USD");
    return form;
  }

  /**
   * Create a player registration form with referral code.
   *
   * @param username username (must be unique for each test)
   * @param referralCode referral code (optional)
   * @return player registration form ready for service invocation
   */
  public static PlayerRegistrationIntegrationForm createRegistrationFormWithReferral(
      String username, String referralCode) {
    PlayerRegistrationIntegrationForm form = createRegistrationForm(username);
    form.setReferralCode(referralCode);
    return form;
  }

  /**
   * Create a deposit callback form with default values.
   *
   * @param orderNo platform order number
   * @param pspTransactionId PSP transaction ID
   * @return deposit callback form ready for service invocation
   */
  public static DepositCallbackForm createDepositCallbackForm(
      String orderNo, String pspTransactionId) {
    DepositCallbackForm form = new DepositCallbackForm();
    form.setOrderNo(orderNo);
    form.setPspTransactionId(pspTransactionId);
    form.setSignature("test_signature_" + System.currentTimeMillis());
    form.setCallbackPayload(
        "{\"orderNo\":\""
            + orderNo
            + "\",\"amount\":\"100.00\",\"status\":\"SUCCESS\",\"timestamp\":\""
            + System.currentTimeMillis()
            + "\"}");
    form.setPspCode("MOCK_PSP");
    return form;
  }

  // ===== Entity Builders =====

  /**
   * Create a first deposit bonus promotion rule entity with default values.
   *
   * <p>Rule configuration: 50% bonus (max $500), 20x wagering, 30 days expiry, 1 claim per player.
   *
   * @return promotion rule entity ready for DAO insertion
   */
  public static PromotionRuleEntity createFirstDepositBonusRule() {
    int id = counter.incrementAndGet();
    PromotionRuleEntity entity = new PromotionRuleEntity();
    entity.setPromotionCode("FIRST_DEPOSIT_TEST_" + id);
    entity.setPromotionName("Test First Deposit Bonus " + id);
    entity.setPromotionType(PromotionTypeEnum.FIRST_DEPOSIT.getValue());
    entity.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    entity.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusYears(1));
    entity.setMinDeposit(new BigDecimal("20.00"));
    entity.setBonusRate(new BigDecimal("0.5000")); // 50% bonus
    entity.setMaxBonus(new BigDecimal("500.00"));
    entity.setWageringMultiplier(new BigDecimal("20.00")); // 20x turnover
    entity.setMaxClaimsPerPlayer(1);
    entity.setBonusExpiryDays(30);
    entity.setGameRestriction(null); // No game restrictions
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a payment order entity with default values (without wallet ID).
   *
   * <p><b>NOTE:</b> This method does NOT set wallet_id, which will cause database constraint
   * violation when inserting. Use {@link #createPaymentOrder(Long, Long, String, BigDecimal,
   * Integer)} instead to provide wallet ID.
   *
   * @param playerId player identifier
   * @param orderNo platform order number
   * @param amount deposit amount
   * @param status order status (1: PENDING, 2: PROCESSING, 3: SUCCESS, 4: FAILED)
   * @return payment order entity ready for DAO insertion
   * @deprecated Use {@link #createPaymentOrder(Long, Long, String, BigDecimal, Integer)} with
   *     wallet ID parameter
   */
  @Deprecated
  public static PaymentOrderEntity createPaymentOrder(
      Long playerId, String orderNo, BigDecimal amount, Integer status) {
    return createPaymentOrder(playerId, null, orderNo, amount, status);
  }

  /**
   * Create a payment order entity with wallet ID.
   *
   * <p>This method includes wallet_id to satisfy database NOT NULL constraint.
   *
   * @param playerId player identifier
   * @param walletId wallet identifier (CASH wallet for deposits)
   * @param orderNo platform order number
   * @param amount deposit amount
   * @param status order status (1: PENDING, 2: PROCESSING, 3: SUCCESS, 4: FAILED)
   * @return payment order entity ready for DAO insertion
   */
  public static PaymentOrderEntity createPaymentOrder(
      Long playerId, Long walletId, String orderNo, BigDecimal amount, Integer status) {
    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setPlayerId(playerId);
    entity.setWalletId(walletId); // ✅ Set wallet ID
    entity.setOrderNo(orderNo);
    entity.setOrderType(1); // DEPOSIT
    entity.setStatus(status);
    entity.setAmount(amount);
    entity.setCurrencyCode("USD");
    entity.setPspCode("MOCK_PSP");
    entity.setPspTransactionId("PSP-TX-" + System.currentTimeMillis());
    entity.setRequestId(
        "REQ-" + orderNo + "-" + System.currentTimeMillis()); // ✅ Set request ID for idempotency
    return entity;
  }

  // ===== Helper Methods =====

  /**
   * Reset the counter to 0 for isolated test execution.
   *
   * <p>Call this in {@code @BeforeEach} if tests need consistent IDs. Otherwise, counter
   * accumulates across tests (which is usually fine for unique ID generation).
   */
  public static void resetCounter() {
    counter.set(0);
  }

  /**
   * Generate a unique username for testing.
   *
   * @param prefix username prefix (e.g., "player", "vip")
   * @return unique username with timestamp and counter
   */
  public static String generateUniqueUsername(String prefix) {
    return prefix + "_" + System.currentTimeMillis() + "_" + counter.incrementAndGet();
  }

  /**
   * Generate a unique order number for testing.
   *
   * @param prefix order prefix (e.g., "DEP", "WD")
   * @return unique order number with timestamp
   */
  public static String generateUniqueOrderNo(String prefix) {
    return prefix + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
  }
}
