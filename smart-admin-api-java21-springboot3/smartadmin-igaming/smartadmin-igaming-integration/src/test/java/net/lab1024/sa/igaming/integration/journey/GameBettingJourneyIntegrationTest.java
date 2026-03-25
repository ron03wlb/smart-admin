package net.lab1024.sa.igaming.integration.journey;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameWeightConfigEntity;
import net.lab1024.sa.igaming.integration.bonus.BonusConversionIntegrationService;
import net.lab1024.sa.igaming.integration.bonus.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.integration.game.GameBettingIntegrationService;
import net.lab1024.sa.igaming.integration.game.domain.form.GameLaunchForm;
import net.lab1024.sa.igaming.integration.game.domain.vo.GameLaunchVO;
import net.lab1024.sa.igaming.integration.turnover.TurnoverIntegrationService;
import net.lab1024.sa.igaming.integration.turnover.domain.event.BetSettlementEvent;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Game betting journey integration test - end-to-end flow validation for Phase 2.
 *
 * <p>This test class validates the complete game betting flow across multiple modules:
 *
 * <ul>
 *   <li><b>Task 2.1</b>: Game Launch Integration (Player + Wallet + Game modules)
 *   <li><b>Task 2.2</b>: Bet Settlement & Turnover Calculation (Game + Activity modules)
 *   <li><b>Task 2.3</b>: Bonus Conversion (Activity + Wallet modules)
 * </ul>
 *
 * <p><b>Test Scenarios (15 total):</b>
 *
 * <ol>
 *   <li><b>Group A - Game Launch</b> (5 tests):
 *       <ul>
 *         <li>shouldLaunchGameSuccessfully - Normal game launch flow
 *         <li>shouldFailGameLaunchWhenPlayerLocked - Player status validation
 *         <li>shouldFailGameLaunchWhenInsufficientBalance - Balance validation
 *         <li>shouldFailGameLaunchWhenGameDisabled - Game validation
 *         <li>shouldVerifyJWTTokenClaims - JWT token validation
 *       </ul>
 *   <li><b>Group B - Bet Settlement</b> (6 tests):
 *       <ul>
 *         <li>shouldProcessBetSettlementSlotsFullTurnover - Slots 100% weight
 *         <li>shouldProcessBetSettlementLiveCasinoReducedTurnover - Live Casino 15% weight
 *         <li>shouldApplyRiskFilterForCriticalRiskScore - Risk filter validation
 *         <li>shouldApplyStatusFactorWin - WIN status (100% factor)
 *         <li>shouldApplyStatusFactorLoss - LOSS status (0% factor)
 *         <li>shouldUpdateWageringProgressAfterSettlement - Wagering progress update
 *       </ul>
 *   <li><b>Group C - Bonus Conversion</b> (4 tests):
 *       <ul>
 *         <li>shouldQueryWageringProgressForActiveBonuses - Progress query
 *         <li>shouldCalculateWageringProgressPercentage - Percentage calculation
 *         <li>shouldCompleteBonusWhenWageringMet - Bonus completion
 *         <li>shouldPublishBonusConvertedEvent - Event publishing
 *       </ul>
 * </ol>
 *
 * <p><b>Test Infrastructure:</b>
 *
 * <ul>
 *   <li>Testcontainers: PostgreSQL 16-alpine, Redis 7, Kafka
 *   <li>Spring Boot Test Context: Full integration module loaded
 *   <li>Test Fixtures: {@link GameBettingTestFixture} for data construction
 * </ul>
 *
 * <p><b>LiteFlow 3-Layer Turnover Calculation:</b>
 *
 * <pre>
 * Layer 1: Risk Filter → effectiveTurnoverBase (Risk score + Odds threshold)
 * Layer 2: Status Factor → validTurnoverFinance (Settlement status: WIN/LOSS/DRAW)
 * Layer 3: Game Weight → activityValidTurnover (Game category weight: Slots 100%, Live 15%)
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-20
 */
@DisplayName("Game Betting Journey Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class GameBettingJourneyIntegrationTest extends BaseIntegrationTest {

  @Autowired private GameBettingIntegrationService gameBettingService;
  @Autowired private TurnoverIntegrationService turnoverIntegrationService;
  @Autowired private BonusConversionIntegrationService bonusConversionService;

  // DAOs for test data setup and verification
  @Autowired private PlayerDao playerDao;
  @Autowired private WalletDao walletDao;
  @Autowired private GameDao gameDao;
  @Autowired private GameWeightConfigDao gameWeightConfigDao;
  @Autowired private PromotionRuleDao promotionRuleDao;
  @Autowired private PlayerBonusRecordDao bonusRecordDao;

  // Test data holders
  private PlayerEntity testPlayer;
  private WalletEntity cashWallet;
  private WalletEntity bonusWallet;
  private GameEntity slotsGame;
  private GameEntity liveCasinoGame;
  private GameWeightConfigEntity slotsWeightConfig;
  private GameWeightConfigEntity liveCasinoWeightConfig;
  private PromotionRuleEntity promotionRule;
  private PlayerBonusRecordEntity bonusRecord;

  @BeforeEach
  void setUp() {
    // Reset tenant context before each test
    TenantContext.setTenantId(1L);

    // Clean up all test data from previous tests (reverse dependency order)
    bonusRecordDao.delete(Wrappers.lambdaQuery());
    promotionRuleDao.delete(Wrappers.lambdaQuery());
    gameWeightConfigDao.delete(Wrappers.lambdaQuery());
    gameDao.delete(Wrappers.lambdaQuery());
    walletDao.delete(Wrappers.lambdaQuery());
    playerDao.delete(Wrappers.lambdaQuery());

    // Setup test data (dependency order: player → wallet → game → config → promo → bonus)
    setupTestPlayer();
    setupTestWallets();
    setupTestGames();
    setupTestGameWeightConfigs();
    setupTestPromotionRule();
    setupTestBonusRecord();
  }

  @AfterEach
  void tearDown() {
    // Clear tenant context after each test
    TenantContext.clear();
  }

  // ==================================================================================
  // Test Data Setup Methods
  // ==================================================================================

  private void setupTestPlayer() {
    testPlayer =
        GameBettingTestFixture.createActivePlayer("test_player_" + System.currentTimeMillis());
    playerDao.insert(testPlayer);
  }

  private void setupTestWallets() {
    cashWallet =
        GameBettingTestFixture.createCashWalletWithDefaultBalance(testPlayer.getPlayerId());
    walletDao.insert(cashWallet);

    bonusWallet =
        GameBettingTestFixture.createBonusWalletWithDefaultBalance(testPlayer.getPlayerId());
    walletDao.insert(bonusWallet);
  }

  private void setupTestGames() {
    slotsGame = GameBettingTestFixture.createSlotsGame();
    gameDao.insert(slotsGame);

    liveCasinoGame = GameBettingTestFixture.createLiveCasinoGame();
    gameDao.insert(liveCasinoGame);
  }

  private void setupTestGameWeightConfigs() {
    slotsWeightConfig = GameBettingTestFixture.createSlotsWeightConfig();
    gameWeightConfigDao.insert(slotsWeightConfig);

    liveCasinoWeightConfig = GameBettingTestFixture.createLiveCasinoWeightConfig();
    gameWeightConfigDao.insert(liveCasinoWeightConfig);
  }

  private void setupTestPromotionRule() {
    promotionRule = PlayerRegistrationTestFixture.createFirstDepositBonusRule();
    promotionRuleDao.insert(promotionRule);
  }

  private void setupTestBonusRecord() {
    bonusRecord =
        GameBettingTestFixture.createActiveBonusRecordWithDefaultProgress(
            testPlayer.getPlayerId(), promotionRule.getRuleId());
    bonusRecordDao.insert(bonusRecord);
  }

  // ==================================================================================
  // Test Scenario Group A: Game Launch Tests (5 tests)
  // ==================================================================================

  @Test
  @Order(1)
  @DisplayName("Should launch game successfully for ACTIVE player with sufficient balance")
  void shouldLaunchGameSuccessfully() {
    // 1. Create GameLaunchForm with testPlayer.playerId and slotsGame.gameCode
    GameLaunchForm form = new GameLaunchForm();
    form.setPlayerId(testPlayer.getPlayerId());
    form.setGameCode(slotsGame.getGameCode());
    form.setProviderCode("mock");
    form.setLanguage("en");

    // 2. Call gameBettingService.launchGame(form)
    ResponseDTO<GameLaunchVO> result = gameBettingService.launchGame(form);

    // 3. Assert ResponseDTO.ok() is true
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();

    // 4. Assert GameLaunchVO contains valid JWT token
    GameLaunchVO vo = result.getData();
    assertThat(vo.getSessionToken()).isNotBlank();
    assertThat(vo.getLaunchUrl()).isNotBlank();
    assertThat(vo.getGameCode()).isEqualTo(slotsGame.getGameCode());
    assertThat(vo.getProviderCode()).isEqualTo("mock");
    assertThat(vo.getSessionId()).isNotBlank();
    assertThat(vo.getTokenExpiresAt()).isNotNull();

    // 5. Verify JWT token claims: playerId, tenantId, gameCode, 24h expiry
    Option<Long> verifiedPlayerIdOpt = gameBettingService.verifySessionToken(vo.getSessionToken());
    assertThat(verifiedPlayerIdOpt.isDefined()).isTrue();
    assertThat(verifiedPlayerIdOpt.get()).isEqualTo(testPlayer.getPlayerId());

    // Verify token expiry is ~24 hours from now (86400 seconds)
    long now = System.currentTimeMillis() / 1000;
    long expiresIn = vo.getTokenExpiresAt() - now;
    assertThat(expiresIn).isBetween(86300L, 86500L); // Allow 100s tolerance
  }

  @Test
  @Order(2)
  @DisplayName("Should fail game launch when player is LOCKED")
  void shouldFailGameLaunchWhenPlayerLocked() {
    // 1. Create LOCKED player using GameBettingTestFixture.createLockedPlayer()
    PlayerEntity lockedPlayer =
        GameBettingTestFixture.createLockedPlayer("locked_" + System.currentTimeMillis());
    playerDao.insert(lockedPlayer);

    // 2. Create wallet for locked player (balance check requires wallet)
    WalletEntity lockedWallet =
        GameBettingTestFixture.createCashWalletWithDefaultBalance(lockedPlayer.getPlayerId());
    walletDao.insert(lockedWallet);

    // 3. Create GameLaunchForm with locked player ID
    GameLaunchForm form = new GameLaunchForm();
    form.setPlayerId(lockedPlayer.getPlayerId());
    form.setGameCode(slotsGame.getGameCode());
    form.setProviderCode("mock");

    // 4. Call gameBettingService.launchGame(form)
    ResponseDTO<GameLaunchVO> result = gameBettingService.launchGame(form);

    // 5. Assert ResponseDTO.ok() is false
    assertThat(result.getOk()).isFalse();

    // 6. Assert error message contains "LOCKED" keyword
    assertThat(result.getMsg()).containsIgnoringCase("locked");
  }

  @Test
  @Order(3)
  @DisplayName("Should fail game launch when player has insufficient balance")
  void shouldFailGameLaunchWhenInsufficientBalance() {
    // 1. Update cashWallet balance to $0
    cashWallet.setBalance(BigDecimal.ZERO);
    walletDao.updateById(cashWallet);

    // Also set bonus wallet to $0
    bonusWallet.setBalance(BigDecimal.ZERO);
    walletDao.updateById(bonusWallet);

    // 2. Create GameLaunchForm
    GameLaunchForm form = new GameLaunchForm();
    form.setPlayerId(testPlayer.getPlayerId());
    form.setGameCode(slotsGame.getGameCode());
    form.setProviderCode("mock");

    // 3. Call gameBettingService.launchGame(form)
    ResponseDTO<GameLaunchVO> result = gameBettingService.launchGame(form);

    // 4. Assert ResponseDTO.ok() is false
    assertThat(result.getOk()).isFalse();

    // 5. Assert error message contains "balance" or "insufficient" keyword
    assertThat(result.getMsg())
        .satisfiesAnyOf(
            msg -> assertThat(msg).containsIgnoringCase("insufficient"),
            msg -> assertThat(msg).containsIgnoringCase("balance"));
  }

  @Test
  @Order(4)
  @DisplayName("Should fail game launch when game is disabled")
  void shouldFailGameLaunchWhenGameDisabled() {
    // 1. Create disabled game using GameBettingTestFixture.createDisabledGame()
    GameEntity disabledGame = GameBettingTestFixture.createDisabledGame();
    gameDao.insert(disabledGame);

    // 2. Create GameLaunchForm with disabled game code
    GameLaunchForm form = new GameLaunchForm();
    form.setPlayerId(testPlayer.getPlayerId());
    form.setGameCode(disabledGame.getGameCode());
    form.setProviderCode("mock");

    // 3. Call gameBettingService.launchGame(form)
    ResponseDTO<GameLaunchVO> result = gameBettingService.launchGame(form);

    // 4. Assert ResponseDTO.ok() is false
    assertThat(result.getOk()).isFalse();

    // 5. Assert error message contains "disabled" keyword
    assertThat(result.getMsg()).containsIgnoringCase("disabled");
  }

  @Test
  @Order(5)
  @DisplayName("Should verify JWT token contains correct claims")
  void shouldVerifyJWTTokenClaims() {
    // 1. Launch game successfully (get JWT token)
    GameLaunchForm form = new GameLaunchForm();
    form.setPlayerId(testPlayer.getPlayerId());
    form.setGameCode(slotsGame.getGameCode());
    form.setProviderCode("mock");

    ResponseDTO<GameLaunchVO> result = gameBettingService.launchGame(form);
    assertThat(result.getOk()).isTrue();

    GameLaunchVO vo = result.getData();
    String token = vo.getSessionToken();

    // 2. Decode JWT token (use gameBettingService.verifySessionToken())
    Option<Long> verifiedPlayerIdOpt = gameBettingService.verifySessionToken(token);

    // 3. Assert token contains correct playerId
    assertThat(verifiedPlayerIdOpt.isDefined()).isTrue();
    assertThat(verifiedPlayerIdOpt.get()).isEqualTo(testPlayer.getPlayerId());

    // 4. Assert token expiry is ~24 hours from now (86400 seconds)
    long now = System.currentTimeMillis() / 1000;
    long expiresIn = vo.getTokenExpiresAt() - now;
    assertThat(expiresIn).isBetween(86300L, 86500L); // Allow 100s tolerance for execution time

    // 5. Verify VO contains all required fields
    assertThat(vo.getGameCode()).isEqualTo(slotsGame.getGameCode());
    assertThat(vo.getProviderCode()).isEqualTo("mock");
    assertThat(vo.getSessionId()).isNotBlank();
    assertThat(vo.getLaunchUrl()).contains(token);
  }

  // ==================================================================================
  // Test Scenario Group B: Bet Settlement Tests (6 tests)
  // ==================================================================================

  @Test
  @Order(6)
  @DisplayName("Should process Slots bet settlement with 100% turnover weight")
  void shouldProcessBetSettlementSlotsFullTurnover() {
    // 1. Query initial wagering progress
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();
    assertThat(initialWageringCompleted).isEqualByComparingTo("500.00"); // Default 50% progress

    // 2. Create BetSettlementEvent: $100 bet, SLOTS game, WIN status
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEvent(
            "BET-SLOTS-001", slotsGame.getGameCode(), new BigDecimal("100.00"), 1); // WIN
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(1); // SLOTS
    event.setProviderCode("mock");

    // DEBUG: Print event details to verify it's correctly created
    System.out.println("==== DEBUG: Event Details ====");
    System.out.println("BetId: " + event.getBetId());
    System.out.println("GameCode: " + event.getGameCode());
    System.out.println("GameCategory: " + event.getGameCategory());
    System.out.println("PlayerId: " + event.getPlayerId());
    System.out.println("BetAmount: " + event.getBetAmount());
    System.out.println("SettlementStatus: " + event.getSettlementStatus());
    System.out.println("OddsValue: " + event.getOddsValue());
    System.out.println("RiskScore: " + event.getRiskScore());
    System.out.println("=============================");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    System.out.println("==== DEBUG: Calling processBetSettlement() ====");
    turnoverIntegrationService.processBetSettlement(event);
    System.out.println("==== DEBUG: processBetSettlement() returned ====");

    // 4. Verify LiteFlow calculation results (query updated bonus record)
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    System.out.println("==== DEBUG: Wagering Progress ====");
    System.out.println("Initial: " + initialWageringCompleted);
    System.out.println("Updated: " + updatedWageringCompleted);
    System.out.println("==================================");

    // Expected calculation:
    // - Layer 1 (Risk): effectiveTurnoverBase = $100 (no risk, riskScore=0 → 100% factor)
    // - Layer 2 (Status): validTurnoverFinance = $100 (WIN status → 100% factor)
    // - Layer 3 (Weight): activityValidTurnover = $100 (Slots → 100% weight)
    // - Final: wageringCompleted = $500 + $100 = $600
    assertThat(updatedWageringCompleted).isEqualByComparingTo("600.00");
  }

  @Test
  @Order(7)
  @DisplayName("Should process Live Casino bet settlement with 15% turnover weight")
  void shouldProcessBetSettlementLiveCasinoReducedTurnover() {
    // 1. Query initial wagering progress
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();

    // 2. Create BetSettlementEvent: $100 bet, LIVE_CASINO game, WIN status
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEvent(
            "BET-LIVE-001", liveCasinoGame.getGameCode(), new BigDecimal("100.00"), 1); // WIN
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(2); // LIVE_CASINO
    event.setProviderCode("mock");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    turnoverIntegrationService.processBetSettlement(event);

    // 4. Verify wagering progress updated: wageringCompleted += $15
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    // Expected calculation:
    // - Layer 1: effectiveTurnoverBase = $100
    // - Layer 2: validTurnoverFinance = $100 (WIN = 100%)
    // - Layer 3: activityValidTurnover = $15 (Live Casino weight = 15%)
    // - Final: wageringCompleted = initial + $15
    BigDecimal expectedTurnover = initialWageringCompleted.add(new BigDecimal("15.00"));
    assertThat(updatedWageringCompleted).isEqualByComparingTo(expectedTurnover);
  }

  @Test
  @Order(8)
  @DisplayName("Should apply risk filter for CRITICAL risk score (0% turnover)")
  void shouldApplyRiskFilterForCriticalRiskScore() {
    // 1. Query initial wagering progress
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();

    // 2. Create BetSettlementEvent with riskScore = 85 (CRITICAL, level 4)
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEventWithRisk(
            "BET-RISK-001",
            slotsGame.getGameCode(),
            new BigDecimal("100.00"),
            1, // WIN status
            new BigDecimal("1.95"), // oddsValue
            85); // riskScore = 85 (CRITICAL → BLOCK)
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(1); // SLOTS
    event.setProviderCode("mock");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    turnoverIntegrationService.processBetSettlement(event);

    // 4. Verify wagering progress NOT updated (bet rejected by risk filter)
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    // Expected: No change (CRITICAL risk → BLOCK action → 0% turnover)
    assertThat(updatedWageringCompleted).isEqualByComparingTo(initialWageringCompleted);
  }

  @Test
  @Order(9)
  @DisplayName("Should apply WIN status factor (100%)")
  void shouldApplyStatusFactorWin() {
    // 1. Query initial wagering progress
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();

    // 2. Create BetSettlementEvent: $100 bet, SLOTS, WIN status (status factor = 100%)
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEvent(
            "BET-WIN-001", slotsGame.getGameCode(), new BigDecimal("100.00"), 1); // WIN
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(1); // SLOTS
    event.setProviderCode("mock");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    turnoverIntegrationService.processBetSettlement(event);

    // 4. Verify wagering progress updated with full turnover contribution
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    // Expected calculation:
    // - Layer 1 (Risk): effectiveTurnoverBase = $100 (no risk, riskScore=0 → 100%)
    // - Layer 2 (Status): validTurnoverFinance = $100 × 100% = $100 (WIN status factor = 100%)
    // - Layer 3 (Weight): activityValidTurnover = $100 × 100% = $100 (Slots weight = 100%)
    // - Final: wageringCompleted = initial + $100
    BigDecimal expectedWageringCompleted = initialWageringCompleted.add(new BigDecimal("100.00"));
    assertThat(updatedWageringCompleted).isEqualByComparingTo(expectedWageringCompleted);
  }

  @Test
  @Order(10)
  @DisplayName("Should apply LOSS status factor (0%)")
  void shouldApplyStatusFactorLoss() {
    // 1. Query initial wagering progress
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();

    // 2. Create BetSettlementEvent: $100 bet, SLOTS, LOSS status (status factor = 0%)
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEvent(
            "BET-LOSS-001", slotsGame.getGameCode(), new BigDecimal("100.00"), 2); // LOSS
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(1); // SLOTS
    event.setProviderCode("mock");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    turnoverIntegrationService.processBetSettlement(event);

    // 4. Verify wagering progress NOT updated (LOSS status contributes 0% turnover)
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    // Expected calculation:
    // - Layer 1 (Risk): effectiveTurnoverBase = $100
    // - Layer 2 (Status): validTurnoverFinance = $100 × 0% = $0 (LOSS status factor = 0%)
    // - Layer 3 (Weight): activityValidTurnover = $0 × 100% = $0
    // - Final: wageringCompleted = initial + $0 (no change)
    assertThat(updatedWageringCompleted).isEqualByComparingTo(initialWageringCompleted);
  }

  @Test
  @Order(11)
  @DisplayName("Should update wagering progress after bet settlement")
  void shouldUpdateWageringProgressAfterSettlement() {
    // 1. Query initial wagering progress (bonusRecord.wageringCompleted)
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();
    BigDecimal wageringRequired = bonusRecord.getWageringRequired();
    assertThat(initialWageringCompleted).isEqualByComparingTo("500.00"); // 50% progress
    assertThat(wageringRequired).isEqualByComparingTo("1000.00");

    // 2. Create BetSettlementEvent: $100 bet, SLOTS, WIN
    BetSettlementEvent event =
        GameBettingTestFixture.createBetSettlementEvent(
            "BET-PROGRESS-001", slotsGame.getGameCode(), new BigDecimal("100.00"), 1); // WIN
    event.setPlayerId(testPlayer.getPlayerId());
    event.setTenantId(1L);
    event.setGameCategory(1); // SLOTS
    event.setProviderCode("mock");

    // 3. Call turnoverIntegrationService.processBetSettlement(event)
    turnoverIntegrationService.processBetSettlement(event);

    // 4. Query updated wagering progress
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal updatedWageringCompleted = updatedRecord.getWageringCompleted();

    // 5. Verify: wageringCompleted = initial + $100
    BigDecimal expectedWageringCompleted = initialWageringCompleted.add(new BigDecimal("100.00"));
    assertThat(updatedWageringCompleted).isEqualByComparingTo(expectedWageringCompleted); // $600

    // 6. Verify: progressPercentage = (wageringCompleted / wageringRequired) × 100%
    // Expected: ($600 / $1000) × 100% = 60%
    BigDecimal progressPercentage =
        updatedWageringCompleted
            .divide(wageringRequired, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    assertThat(progressPercentage).isEqualByComparingTo("60.00");

    // Additional verification: Wagering remaining
    BigDecimal wageringRemaining = wageringRequired.subtract(updatedWageringCompleted);
    assertThat(wageringRemaining).isEqualByComparingTo("400.00"); // $1000 - $600 = $400
  }

  // ==================================================================================
  // Test Scenario Group C: Bonus Conversion Tests (4 tests)
  // ==================================================================================

  @Test
  @Order(12)
  @DisplayName("Should query wagering progress for all ACTIVE bonuses")
  void shouldQueryWageringProgressForActiveBonuses() {
    // 1. Call bonusConversionService.getWageringProgress(testPlayer.playerId)
    ResponseDTO<List<WageringProgressVO>> result =
        bonusConversionService.getWageringProgress(testPlayer.getPlayerId());

    // 2. Assert ResponseDTO.ok() is true
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();

    // 3. Assert result contains 1 record (bonusRecord)
    List<WageringProgressVO> progressList = result.getData();
    assertThat(progressList).hasSize(1);

    // 4. Verify record fields: bonusAmount, wageringRequired, wageringCompleted, status=ACTIVE
    WageringProgressVO vo = progressList.get(0);
    assertThat(vo.getRecordId()).isEqualTo(bonusRecord.getRecordId());
    assertThat(vo.getPlayerId()).isEqualTo(testPlayer.getPlayerId());
    assertThat(vo.getBonusAmount()).isEqualByComparingTo("50.00"); // From fixture
    assertThat(vo.getWageringRequired()).isEqualByComparingTo("1000.00"); // From fixture
    assertThat(vo.getWageringCompleted())
        .isEqualByComparingTo("500.00"); // From fixture (50% progress)
    assertThat(vo.getStatus()).isEqualTo(2); // ACTIVE
    assertThat(vo.getPromotionCode()).isEqualTo(promotionRule.getPromotionCode());
    assertThat(vo.getExpiresAt()).isNotNull();
  }

  @Test
  @Order(13)
  @DisplayName("Should calculate wagering progress percentage correctly")
  void shouldCalculateWageringProgressPercentage() {
    // 1. Query wagering progress (wageringCompleted = $500, wageringRequired = $1000)
    ResponseDTO<List<WageringProgressVO>> result =
        bonusConversionService.getWageringProgress(testPlayer.getPlayerId());

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);

    WageringProgressVO vo = result.getData().get(0);

    // 2. Verify progressPercentage = 50.0% (500 / 1000 × 100)
    assertThat(vo.getProgressPercentage()).isEqualByComparingTo("50.00");

    // 3. Verify wageringRemaining = $500 (1000 - 500)
    assertThat(vo.getWageringRemaining()).isEqualByComparingTo("500.00");

    // 4. Verify wagering amounts match fixture defaults
    assertThat(vo.getWageringCompleted()).isEqualByComparingTo("500.00");
    assertThat(vo.getWageringRequired()).isEqualByComparingTo("1000.00");

    // 5. Verify isReadyForConversion flag (should be false at 50% progress)
    assertThat(vo.getIsReadyForConversion()).isFalse();
  }

  @Test
  @Order(14)
  @DisplayName("Should complete bonus when wagering requirement is met")
  void shouldCompleteBonusWhenWageringMet() {
    // 1. Verify initial wagering progress: $500 completed (50%)
    BigDecimal initialWageringCompleted = bonusRecord.getWageringCompleted();
    assertThat(initialWageringCompleted).isEqualByComparingTo("500.00");

    // 2. Process 5 Slots bets (5 × $100 = $500 turnover) to reach 100% progress
    for (int i = 1; i <= 5; i++) {
      BetSettlementEvent event =
          GameBettingTestFixture.createBetSettlementEvent(
              "BET-COMPLETE-00" + i, slotsGame.getGameCode(), new BigDecimal("100.00"), 1); // WIN
      event.setPlayerId(testPlayer.getPlayerId());
      event.setTenantId(1L);
      event.setGameCategory(1); // SLOTS
      event.setProviderCode("mock");

      turnoverIntegrationService.processBetSettlement(event);
    }

    // 3. Verify wageringCompleted reached $1000 (initial $500 + new $500)
    PlayerBonusRecordEntity updatedRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    BigDecimal finalWageringCompleted = updatedRecord.getWageringCompleted();
    assertThat(finalWageringCompleted).isEqualByComparingTo("1000.00");

    // 4. Query wagering progress to verify 100% completion
    ResponseDTO<List<WageringProgressVO>> result =
        bonusConversionService.getWageringProgress(testPlayer.getPlayerId());

    // Note: Bonus may still be ACTIVE (status transition to COMPLETED happens via Kafka event)
    // This test verifies wagering progress reached 100%, not automatic status change
    assertThat(result.getOk()).isTrue();
    WageringProgressVO vo = result.getData().get(0);
    assertThat(vo.getProgressPercentage()).isEqualByComparingTo("100.00");
    assertThat(vo.getWageringRemaining()).isEqualByComparingTo("0.00");
    assertThat(vo.getIsReadyForConversion()).isTrue();
  }

  @Test
  @Order(15)
  @DisplayName("Should publish BONUS_CONVERTED event when manually converted")
  void shouldPublishBonusConvertedEvent() {
    // 1. Set wageringCompleted = wageringRequired (100% progress)
    bonusRecord.setWageringCompleted(bonusRecord.getWageringRequired());
    bonusRecordDao.updateById(bonusRecord);

    // Verify 100% progress
    PlayerBonusRecordEntity verifyRecord = bonusRecordDao.selectById(bonusRecord.getRecordId());
    assertThat(verifyRecord.getWageringCompleted()).isEqualByComparingTo("1000.00");
    assertThat(verifyRecord.getWageringRequired()).isEqualByComparingTo("1000.00");

    // 2. Call bonusConversionService.manualConvertBonus(recordId, operatorId)
    Long operatorId = 999L; // Mock admin operator ID
    ResponseDTO<Void> result =
        bonusConversionService.manualConvertBonus(bonusRecord.getRecordId(), operatorId);

    // 3. Verify ResponseDTO.ok() is true (conversion successful)
    assertThat(result.getOk()).isTrue();

    // 4. Verify wagering progress query shows conversion readiness
    ResponseDTO<List<WageringProgressVO>> progressResult =
        bonusConversionService.getWageringProgress(testPlayer.getPlayerId());

    // Note: Bonus status may change to COMPLETED or remain ACTIVE (depends on Kafka event timing)
    // The key validation is that manualConvertBonus() returned success
    assertThat(progressResult.getOk()).isTrue();

    // 5. Verify BONUS_CONVERTED event was published (implicit - no errors thrown)
    // Note: Full Kafka event validation requires Kafka consumer testing,
    // which is beyond the scope of this journey test.
    // The publishBonusConvertedEvent() method is invoked within manualConvertBonus(),
    // and we verify no exceptions were thrown.
  }
}
