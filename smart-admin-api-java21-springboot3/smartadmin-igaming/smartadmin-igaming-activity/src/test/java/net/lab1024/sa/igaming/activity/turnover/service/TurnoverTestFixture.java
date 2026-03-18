package net.lab1024.sa.igaming.activity.turnover.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.igaming.activity.turnover.constant.OddsTypeEnum;
import net.lab1024.sa.igaming.activity.turnover.constant.TurnoverRuleStatusEnum;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverGameWeightRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverOddsThresholdRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRiskActionRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverStatusFactorRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverGameWeightRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverOddsThresholdRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverStatusFactorRuleEntity;

/**
 * Turnover test fixture - Builder patterns for test data construction.
 *
 * <p>Provides factory methods to create test entities with sensible defaults. Eliminates test data
 * duplication and ensures consistency across test classes.
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * // Create game weight rule
 * TurnoverGameWeightRuleEntity rule = TurnoverTestFixture.createGameWeightRule(2, new BigDecimal("15.00"));
 * gameWeightDao.insert(rule);
 *
 * // Setup default rules for integration tests
 * TurnoverTestFixture.setupDefaultRules(gameWeightDao, oddsThresholdDao, riskActionDao, statusFactorDao);
 * </pre>
 *
 * <p><b>Thread Safety:</b> Uses {@link AtomicInteger} for unique ID generation across concurrent
 * tests.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
public class TurnoverTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  // ===== Entity Builders =====

  /**
   * Create a game weight rule entity with default values.
   *
   * @param gameCategory game category (1-6: Slots, Live Casino, Sports, Poker, Table Games,
   *     Lottery)
   * @param weight weight percentage (0.00 - 100.00)
   * @return game weight rule entity ready for DAO insertion
   */
  public static TurnoverGameWeightRuleEntity createGameWeightRule(
      Integer gameCategory, BigDecimal weight) {
    int id = counter.incrementAndGet();
    TurnoverGameWeightRuleEntity entity = new TurnoverGameWeightRuleEntity();
    entity.setRuleCode("GW-TEST-" + id);
    entity.setRuleName("Test Game Weight Rule " + id);
    entity.setGameCategory(gameCategory);
    entity.setWeightPercentage(weight);
    entity.setPriority(100);
    entity.setStatus(TurnoverRuleStatusEnum.ENABLED.getValue());
    entity.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusYears(1));
    entity.setDeleted(false);
    entity.setRemark("Auto-generated test rule");
    return entity;
  }

  /**
   * Create an odds threshold rule entity with default values.
   *
   * @param oddsType odds type (1-4: EUR, HK, MY, ID)
   * @param operator comparison operator (">=", ">", "<=", "<", "=")
   * @param threshold threshold value (e.g., 1.50)
   * @return odds threshold rule entity ready for DAO insertion
   */
  public static TurnoverOddsThresholdRuleEntity createOddsThresholdRule(
      Integer oddsType, String operator, BigDecimal threshold) {
    int id = counter.incrementAndGet();
    TurnoverOddsThresholdRuleEntity entity = new TurnoverOddsThresholdRuleEntity();
    entity.setRuleCode("OT-TEST-" + id);
    entity.setRuleName("Test Odds Threshold Rule " + id);
    entity.setOddsType(oddsType);
    entity.setComparisonOperator(operator);
    entity.setThresholdValue(threshold);
    entity.setStatus(TurnoverRuleStatusEnum.ENABLED.getValue());
    entity.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusYears(1));
    entity.setDeleted(false);
    entity.setRemark("Auto-generated test rule");
    return entity;
  }

  /**
   * Create a risk action rule entity with default values.
   *
   * @param riskLevel risk level (1-4: LOW, MEDIUM, HIGH, CRITICAL)
   * @param actionType action type (1: PASS, 2: FLAG, 3: BLOCK, 4: MANUAL_REVIEW)
   * @param factor turnover factor percentage (0.00 - 100.00)
   * @return risk action rule entity ready for DAO insertion
   */
  public static TurnoverRiskActionRuleEntity createRiskActionRule(
      Integer riskLevel, Integer actionType, BigDecimal factor) {
    int id = counter.incrementAndGet();
    TurnoverRiskActionRuleEntity entity = new TurnoverRiskActionRuleEntity();
    entity.setRuleCode("RA-TEST-" + id);
    entity.setRuleName("Test Risk Action Rule " + id);
    entity.setRiskLevel(riskLevel);
    entity.setActionType(actionType);
    entity.setTurnoverFactor(factor);
    entity.setStatus(TurnoverRuleStatusEnum.ENABLED.getValue());
    entity.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusYears(1));
    entity.setDeleted(false);
    entity.setRemark("Auto-generated test rule");
    return entity;
  }

  /**
   * Create a status factor rule entity with default values.
   *
   * @param settlementStatus settlement status (1-9: WIN, LOSS, DRAW, TIE, VOID, CANCEL, HALF_WIN,
   *     HALF_LOSS, RUNNING)
   * @param factor factor percentage (0.00 - 100.00)
   * @return status factor rule entity ready for DAO insertion
   */
  public static TurnoverStatusFactorRuleEntity createStatusFactorRule(
      Integer settlementStatus, BigDecimal factor) {
    int id = counter.incrementAndGet();
    TurnoverStatusFactorRuleEntity entity = new TurnoverStatusFactorRuleEntity();
    entity.setRuleCode("SF-TEST-" + id);
    entity.setRuleName("Test Status Factor Rule " + id);
    entity.setSettlementStatus(settlementStatus);
    entity.setFactorPercentage(factor);
    entity.setStatus(TurnoverRuleStatusEnum.ENABLED.getValue());
    entity.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEffectiveTo(OffsetDateTime.now(ZoneOffset.UTC).plusYears(1));
    entity.setDeleted(false);
    entity.setRemark("Auto-generated test rule");
    return entity;
  }

  // ===== TurnoverContext Builder =====

  /**
   * Create a TurnoverContext for testing with default values.
   *
   * @param betAmount bet amount
   * @param gameCategory game category (1-6)
   * @param settlementStatus settlement status (1-9)
   * @return TurnoverContext ready for LiteFlow chain execution
   */
  public static TurnoverContext createContext(
      BigDecimal betAmount, Integer gameCategory, Integer settlementStatus) {
    int id = counter.incrementAndGet();
    TurnoverContext ctx = new TurnoverContext();
    ctx.setBetId("BET-TEST-" + id);
    ctx.setPlayerId(1000L + id);
    ctx.setTenantId(1L);
    ctx.setBetAmount(betAmount);
    ctx.setGameCategory(gameCategory);
    ctx.setSettlementStatus(settlementStatus);
    ctx.setOddsValue(new BigDecimal("1.95"));
    ctx.setOddsType(OddsTypeEnum.EUR.getValue());
    ctx.setRiskScore(0);
    return ctx;
  }

  // ===== Helper Methods =====

  /**
   * Setup default rules for all game categories, statuses, and risk levels.
   *
   * <p>This method pre-populates the database with complete rule configuration for integration
   * tests. It creates:
   *
   * <ul>
   *   <li>6 game weight rules (Slots 100%, Live Casino 15%, Sports 100%, Poker 5%, Table Games 20%,
   *       Lottery 15%)
   *   <li>1 odds threshold rule (oddsValue >= 1.50)
   *   <li>4 risk action rules (LOW=PASS, MEDIUM=FLAG, HIGH=FLAG, CRITICAL=BLOCK)
   *   <li>9 status factor rules (WIN/LOSS=100%, DRAW/VOID/CANCEL=0%, etc.)
   * </ul>
   *
   * @param gameWeightDao game weight rule DAO
   * @param oddsThresholdDao odds threshold rule DAO
   * @param riskActionDao risk action rule DAO
   * @param statusFactorDao status factor rule DAO
   */
  public static void setupDefaultRules(
      TurnoverGameWeightRuleDao gameWeightDao,
      TurnoverOddsThresholdRuleDao oddsThresholdDao,
      TurnoverRiskActionRuleDao riskActionDao,
      TurnoverStatusFactorRuleDao statusFactorDao) {

    // Game weights (6 categories)
    gameWeightDao.insert(createGameWeightRule(1, new BigDecimal("100.00"))); // Slots
    gameWeightDao.insert(createGameWeightRule(2, new BigDecimal("15.00"))); // Live Casino
    gameWeightDao.insert(createGameWeightRule(3, new BigDecimal("100.00"))); // Sports Betting
    gameWeightDao.insert(createGameWeightRule(4, new BigDecimal("5.00"))); // Poker
    gameWeightDao.insert(createGameWeightRule(5, new BigDecimal("20.00"))); // Table Games
    gameWeightDao.insert(createGameWeightRule(6, new BigDecimal("15.00"))); // Lottery

    // Odds threshold (oddsValue >= 1.50)
    oddsThresholdDao.insert(createOddsThresholdRule(1, ">=", new BigDecimal("1.50")));

    // Risk actions (4 levels)
    riskActionDao.insert(createRiskActionRule(1, 1, new BigDecimal("100.00"))); // LOW → PASS
    riskActionDao.insert(createRiskActionRule(2, 2, new BigDecimal("100.00"))); // MEDIUM → FLAG
    riskActionDao.insert(createRiskActionRule(3, 2, new BigDecimal("100.00"))); // HIGH → FLAG
    riskActionDao.insert(createRiskActionRule(4, 3, new BigDecimal("0.00"))); // CRITICAL → BLOCK

    // Status factors (9 statuses)
    statusFactorDao.insert(createStatusFactorRule(1, new BigDecimal("100.00"))); // WIN
    statusFactorDao.insert(createStatusFactorRule(2, new BigDecimal("100.00"))); // LOSS
    statusFactorDao.insert(createStatusFactorRule(3, new BigDecimal("0.00"))); // DRAW
    statusFactorDao.insert(createStatusFactorRule(4, new BigDecimal("0.00"))); // TIE
    statusFactorDao.insert(createStatusFactorRule(5, new BigDecimal("0.00"))); // VOID
    statusFactorDao.insert(createStatusFactorRule(6, new BigDecimal("0.00"))); // CANCEL
    statusFactorDao.insert(createStatusFactorRule(7, new BigDecimal("100.00"))); // HALF_WIN
    statusFactorDao.insert(createStatusFactorRule(8, new BigDecimal("100.00"))); // HALF_LOSS
    statusFactorDao.insert(createStatusFactorRule(9, new BigDecimal("0.00"))); // RUNNING
  }

  /**
   * Reset the counter to 0 for isolated test execution.
   *
   * <p>Call this in {@code @BeforeEach} if tests need consistent IDs. Otherwise, counter
   * accumulates across tests (which is usually fine for unique ID generation).
   */
  public static void resetCounter() {
    counter.set(0);
  }
}
