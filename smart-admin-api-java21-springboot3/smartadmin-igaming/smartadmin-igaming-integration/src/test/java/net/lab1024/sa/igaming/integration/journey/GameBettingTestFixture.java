package net.lab1024.sa.igaming.integration.journey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameWeightConfigEntity;
import net.lab1024.sa.igaming.integration.turnover.domain.event.BetSettlementEvent;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;

/**
 * Game betting test fixture - Builder patterns for Phase 2 test data construction.
 *
 * <p>Provides factory methods to create test entities for game betting, turnover calculation, and
 * bonus conversion flows. Eliminates test data duplication and ensures consistency across test
 * classes.
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * // Create active player
 * PlayerEntity player = GameBettingTestFixture.createActivePlayer("testuser");
 *
 * // Create Slots game with 100% weight
 * GameEntity slotsGame = GameBettingTestFixture.createSlotsGame();
 * GameWeightConfigEntity config = GameBettingTestFixture.createGameWeightConfig(1, new BigDecimal("1.00"));
 *
 * // Create bet settlement event
 * BetSettlementEvent event = GameBettingTestFixture.createBetSettlementEvent(
 *     "BET-001", "SLOT-001", new BigDecimal("100.00"), 1); // WIN
 * </pre>
 *
 * <p><b>Thread Safety:</b> Uses {@link AtomicInteger} for unique ID generation across concurrent
 * tests.
 *
 * @author iGaming Team
 * @since 2026-03-20
 */
public class GameBettingTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  // ===== Player Fixtures =====

  /**
   * Create an active player entity with default values.
   *
   * @param username username (must be unique for each test)
   * @return player entity ready for DAO insertion
   */
  public static PlayerEntity createActivePlayer(String username) {
    PlayerEntity entity = new PlayerEntity();
    entity.setUsername(username);
    entity.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$TEST"); // Argon2id hash
    entity.setEmailEncrypted(username + "@test.com");
    entity.setEmailBlindIdx("BLIND_IDX_" + username); // Simplified blind index for tests
    entity.setPhoneEncrypted("+86-138-0000-" + String.format("%04d", counter.incrementAndGet()));
    entity.setPhoneBlindIdx("BLIND_IDX_PHONE_" + counter.get());
    entity.setStatus(1); // ACTIVE
    entity.setKycLevel(0); // L0 - Basic verification
    entity.setRegistrationIp("127.0.0.1");
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a locked player entity (for negative testing).
   *
   * @param username username (must be unique for each test)
   * @return locked player entity ready for DAO insertion
   */
  public static PlayerEntity createLockedPlayer(String username) {
    PlayerEntity entity = createActivePlayer(username);
    entity.setStatus(2); // LOCKED
    return entity;
  }

  /**
   * Create a suspended player entity (for negative testing).
   *
   * @param username username (must be unique for each test)
   * @return suspended player entity ready for DAO insertion
   */
  public static PlayerEntity createSuspendedPlayer(String username) {
    PlayerEntity entity = createActivePlayer(username);
    entity.setStatus(3); // SUSPENDED
    return entity;
  }

  // ===== Wallet Fixtures =====

  /**
   * Create a CASH wallet entity with specified balance.
   *
   * @param playerId player identifier
   * @param balance wallet balance
   * @return wallet entity ready for DAO insertion
   */
  public static WalletEntity createCashWallet(Long playerId, BigDecimal balance) {
    WalletEntity entity = new WalletEntity();
    entity.setPlayerId(playerId);
    entity.setWalletType(1); // CASH
    entity.setBalance(balance);
    entity.setLockedAmount(BigDecimal.ZERO);
    entity.setCurrencyCode("USD");
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a BONUS wallet entity with specified balance.
   *
   * @param playerId player identifier
   * @param balance wallet balance
   * @return wallet entity ready for DAO insertion
   */
  public static WalletEntity createBonusWallet(Long playerId, BigDecimal balance) {
    WalletEntity entity = new WalletEntity();
    entity.setPlayerId(playerId);
    entity.setWalletType(2); // BONUS
    entity.setBalance(balance);
    entity.setLockedAmount(BigDecimal.ZERO);
    entity.setCurrencyCode("USD");
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a CASH wallet with default $1000 balance.
   *
   * @param playerId player identifier
   * @return wallet entity ready for DAO insertion
   */
  public static WalletEntity createCashWalletWithDefaultBalance(Long playerId) {
    return createCashWallet(playerId, new BigDecimal("1000.00"));
  }

  /**
   * Create a BONUS wallet with default $50 balance.
   *
   * @param playerId player identifier
   * @return wallet entity ready for DAO insertion
   */
  public static WalletEntity createBonusWalletWithDefaultBalance(Long playerId) {
    return createBonusWallet(playerId, new BigDecimal("50.00"));
  }

  // ===== Game Fixtures =====

  /**
   * Create a Slots game entity (100% turnover weight).
   *
   * @return game entity ready for DAO insertion
   */
  public static GameEntity createSlotsGame() {
    int id = counter.incrementAndGet();
    GameEntity entity = new GameEntity();
    entity.setGameCode("SLOT-TEST-" + id);
    entity.setGameName("Test Slots Game " + id);
    entity.setCategory(1); // SLOTS
    entity.setProviderId(1L); // Mock provider ID
    entity.setEnabled(true);
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a Live Casino game entity (15% turnover weight).
   *
   * @return game entity ready for DAO insertion
   */
  public static GameEntity createLiveCasinoGame() {
    int id = counter.incrementAndGet();
    GameEntity entity = new GameEntity();
    entity.setGameCode("LIVE-TEST-" + id);
    entity.setGameName("Test Live Casino Game " + id);
    entity.setCategory(2); // LIVE_CASINO
    entity.setProviderId(1L); // Mock provider ID
    entity.setEnabled(true);
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a Table Games game entity (20% turnover weight).
   *
   * @return game entity ready for DAO insertion
   */
  public static GameEntity createTableGame() {
    int id = counter.incrementAndGet();
    GameEntity entity = new GameEntity();
    entity.setGameCode("TABLE-TEST-" + id);
    entity.setGameName("Test Table Game " + id);
    entity.setCategory(5); // TABLE_GAMES
    entity.setProviderId(1L); // Mock provider ID
    entity.setEnabled(true);
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create a disabled game entity (for negative testing).
   *
   * @return disabled game entity ready for DAO insertion
   */
  public static GameEntity createDisabledGame() {
    GameEntity entity = createSlotsGame();
    entity.setEnabled(false);
    return entity;
  }

  // ===== Game Weight Config Fixtures =====

  /**
   * Create a game weight config entity.
   *
   * @param gameCategory game category (1: Slots, 2: Live Casino, 3: Sports, 4: Poker, 5: Table
   *     Games, 6: Lottery)
   * @param weight weight factor (e.g., 1.00 = 100%, 0.15 = 15%)
   * @return game weight config entity ready for DAO insertion
   */
  public static GameWeightConfigEntity createGameWeightConfig(
      Integer gameCategory, BigDecimal weight) {
    GameWeightConfigEntity entity = new GameWeightConfigEntity();
    entity.setGameCategory(gameCategory);
    entity.setWeight(weight);
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create Slots game weight config (100% weight).
   *
   * @return game weight config entity ready for DAO insertion
   */
  public static GameWeightConfigEntity createSlotsWeightConfig() {
    return createGameWeightConfig(1, new BigDecimal("1.0000")); // 100%
  }

  /**
   * Create Live Casino game weight config (15% weight).
   *
   * @return game weight config entity ready for DAO insertion
   */
  public static GameWeightConfigEntity createLiveCasinoWeightConfig() {
    return createGameWeightConfig(2, new BigDecimal("0.1500")); // 15%
  }

  /**
   * Create Table Games game weight config (20% weight).
   *
   * @return game weight config entity ready for DAO insertion
   */
  public static GameWeightConfigEntity createTableGamesWeightConfig() {
    return createGameWeightConfig(5, new BigDecimal("0.2000")); // 20%
  }

  // ===== Bonus Fixtures =====

  /**
   * Create an active bonus record entity with wagering requirements.
   *
   * @param playerId player identifier
   * @param ruleId promotion rule identifier
   * @param bonusAmount bonus amount
   * @param wageringRequired wagering requirement (e.g., $1000)
   * @param wageringCompleted wagering completed (e.g., $500)
   * @return bonus record entity ready for DAO insertion
   */
  public static PlayerBonusRecordEntity createActiveBonusRecord(
      Long playerId,
      Long ruleId,
      BigDecimal bonusAmount,
      BigDecimal wageringRequired,
      BigDecimal wageringCompleted) {
    int id = counter.incrementAndGet();
    PlayerBonusRecordEntity entity = new PlayerBonusRecordEntity();
    entity.setPlayerId(playerId);
    entity.setRuleId(ruleId);
    entity.setClaimId("CLAIM-TEST-" + id);
    entity.setBonusAmount(bonusAmount);
    entity.setWageringRequired(wageringRequired);
    entity.setWageringCompleted(wageringCompleted);
    entity.setStatus(2); // ACTIVE
    entity.setClaimedAt(OffsetDateTime.now(ZoneOffset.UTC));
    entity.setExpiredAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    entity.setDeleted(false);
    return entity;
  }

  /**
   * Create an active bonus record with default values (50% progress).
   *
   * @param playerId player identifier
   * @param ruleId promotion rule identifier
   * @return bonus record entity ready for DAO insertion
   */
  public static PlayerBonusRecordEntity createActiveBonusRecordWithDefaultProgress(
      Long playerId, Long ruleId) {
    return createActiveBonusRecord(
        playerId,
        ruleId,
        new BigDecimal("50.00"), // Bonus amount
        new BigDecimal("1000.00"), // Wagering required
        new BigDecimal("500.00") // Wagering completed (50%)
        );
  }

  /**
   * Create a completed bonus record (wagering requirement met).
   *
   * @param playerId player identifier
   * @param ruleId promotion rule identifier
   * @return bonus record entity ready for DAO insertion
   */
  public static PlayerBonusRecordEntity createCompletedBonusRecord(Long playerId, Long ruleId) {
    PlayerBonusRecordEntity entity = createActiveBonusRecordWithDefaultProgress(playerId, ruleId);
    entity.setWageringCompleted(new BigDecimal("1000.00")); // 100% progress
    entity.setStatus(3); // COMPLETED
    entity.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return entity;
  }

  // ===== Event Fixtures =====

  /**
   * Create a bet settlement event.
   *
   * @param betId bet identifier
   * @param gameCode game code
   * @param betAmount bet amount
   * @param settlementStatus settlement status (1: WIN, 2: LOSS, 3: DRAW, etc.)
   * @return bet settlement event ready for service consumption
   */
  public static BetSettlementEvent createBetSettlementEvent(
      String betId, String gameCode, BigDecimal betAmount, Integer settlementStatus) {
    BetSettlementEvent event = new BetSettlementEvent();
    event.setBetId(betId);
    event.setGameCode(gameCode);
    event.setBetAmount(betAmount);
    event.setSettlementStatus(settlementStatus);
    event.setOddsValue(new BigDecimal("1.95")); // Default odds (above threshold 1.50)
    event.setOddsType(1); // DECIMAL
    event.setRiskScore(0); // Default LOW risk
    event.setSettledAt(OffsetDateTime.now(ZoneOffset.UTC).toString()); // ISO-8601 format
    return event;
  }

  /**
   * Create a bet settlement event with custom odds and risk score.
   *
   * @param betId bet identifier
   * @param gameCode game code
   * @param betAmount bet amount
   * @param settlementStatus settlement status
   * @param oddsValue odds value (e.g., 1.95)
   * @param riskScore risk score (0-100)
   * @return bet settlement event ready for service consumption
   */
  public static BetSettlementEvent createBetSettlementEventWithRisk(
      String betId,
      String gameCode,
      BigDecimal betAmount,
      Integer settlementStatus,
      BigDecimal oddsValue,
      Integer riskScore) {
    BetSettlementEvent event =
        createBetSettlementEvent(betId, gameCode, betAmount, settlementStatus);
    event.setOddsValue(oddsValue);
    event.setRiskScore(riskScore);
    return event;
  }

  /**
   * Create a WIN bet settlement event.
   *
   * @param betId bet identifier
   * @param gameCode game code
   * @param betAmount bet amount
   * @return bet settlement event with WIN status
   */
  public static BetSettlementEvent createWinBetEvent(
      String betId, String gameCode, BigDecimal betAmount) {
    return createBetSettlementEvent(betId, gameCode, betAmount, 1); // WIN
  }

  /**
   * Create a LOSS bet settlement event.
   *
   * @param betId bet identifier
   * @param gameCode game code
   * @param betAmount bet amount
   * @return bet settlement event with LOSS status
   */
  public static BetSettlementEvent createLossBetEvent(
      String betId, String gameCode, BigDecimal betAmount) {
    return createBetSettlementEvent(betId, gameCode, betAmount, 2); // LOSS
  }

  /**
   * Create a DRAW bet settlement event (0% turnover).
   *
   * @param betId bet identifier
   * @param gameCode game code
   * @param betAmount bet amount
   * @return bet settlement event with DRAW status
   */
  public static BetSettlementEvent createDrawBetEvent(
      String betId, String gameCode, BigDecimal betAmount) {
    return createBetSettlementEvent(betId, gameCode, betAmount, 3); // DRAW
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
   * Generate a unique game code for testing.
   *
   * @param prefix game code prefix (e.g., "SLOT", "LIVE", "TABLE")
   * @return unique game code with timestamp and counter
   */
  public static String generateUniqueGameCode(String prefix) {
    return prefix + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
  }

  /**
   * Generate a unique bet ID for testing.
   *
   * @param prefix bet ID prefix (e.g., "BET", "WAGER")
   * @return unique bet ID with timestamp and counter
   */
  public static String generateUniqueBetId(String prefix) {
    return prefix + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
  }

  /**
   * Generate a unique promotion code for testing.
   *
   * @param prefix promotion code prefix (e.g., "PROMO", "BONUS")
   * @return unique promotion code with timestamp and counter
   */
  public static String generateUniquePromotionCode(String prefix) {
    return prefix + "-" + System.currentTimeMillis() + "-" + counter.incrementAndGet();
  }
}
