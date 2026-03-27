package net.lab1024.sa.igaming.integration.journey;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.integration.player.PlayerRegistrationIntegrationService;
import net.lab1024.sa.igaming.integration.player.domain.form.PlayerRegistrationIntegrationForm;
import net.lab1024.sa.igaming.integration.player.domain.vo.PlayerRegistrationResultVO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionHistoryDao;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionRequestDao;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionHistoryEntity;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionRequestEntity;
import net.lab1024.sa.igaming.player.selfexclusion.service.SelfExclusionEnforcementService;
import net.lab1024.sa.igaming.player.selfexclusion.service.SelfExclusionRequestService;
import net.lab1024.sa.igaming.player.selfexclusion.service.SelfExclusionReviewService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Self-Exclusion Journey Integration Test - End-to-end responsible gaming flow validation.
 *
 * <p>This test class validates the complete self-exclusion (自我排除) system across the player module:
 *
 * <ul>
 *   <li><b>Request Management</b>: Player creates self-exclusion requests with cooling-off periods
 *   <li><b>Restriction Enforcement</b>: Deposit/betting/login blocks enforced correctly
 *   <li><b>Cooling-Off Period</b>: Time-based validation prevents premature removal
 *   <li><b>Removal Review Workflow</b>: Admin approval/rejection/cancellation flows
 *   <li><b>Audit Trail</b>: Complete history tracking for compliance
 * </ul>
 *
 * <p><b>Test Scenarios (10 tests):</b>
 *
 * <ol>
 *   <li>Player creates self-exclusion request (DEPOSIT, 24_HOURS)
 *   <li>Deposit restriction is enforced during active exclusion
 *   <li>Betting restriction is enforced during active exclusion
 *   <li>Login restriction is enforced during active exclusion
 *   <li>Cooling-off period prevents premature removal requests
 *   <li>Player can request removal after cooling-off period ends
 *   <li>Admin approves removal request (status → REMOVED)
 *   <li>Admin rejects removal request (player can try again)
 *   <li>Admin cancels active exclusion request (status → CANCELLED)
 *   <li>Automatic expiry of completed exclusion requests
 * </ol>
 *
 * <p><b>Test Infrastructure:</b>
 *
 * <ul>
 *   <li>Testcontainers: PostgreSQL 16-alpine
 *   <li>Spring Boot Test Context: Full integration module with self-exclusion services
 *   <li>Time Manipulation: Direct database timestamp updates for cooling-off period tests
 * </ul>
 *
 * <p><b>Responsible Gaming Compliance:</b>
 *
 * <ul>
 *   <li>4 exclusion types: DEPOSIT, BETTING, LOGIN, FULL_BLOCK
 *   <li>5 duration types: 24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT
 *   <li>Cooling-off periods: 24H-90D based on duration type
 *   <li>Audit trail: Every state change recorded in t_self_exclusion_history
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@DisplayName("Self-Exclusion Journey Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class SelfExclusionJourneyIntegrationTest extends BaseIntegrationTest {

  @Autowired private PlayerRegistrationIntegrationService playerRegistrationService;
  @Autowired private SelfExclusionRequestService selfExclusionRequestService;
  @Autowired private SelfExclusionEnforcementService selfExclusionEnforcementService;
  @Autowired private SelfExclusionReviewService selfExclusionReviewService;

  // DAOs for verification
  @Autowired private PlayerDao playerDao;
  @Autowired private SelfExclusionRequestDao selfExclusionRequestDao;
  @Autowired private SelfExclusionHistoryDao selfExclusionHistoryDao;

  @BeforeEach
  void setUp() {
    // Reset tenant context before each test
    TenantContext.setTenantId(1L);
  }

  // ========================================================================
  // Test 1: Player Creates Self-Exclusion Request
  // ========================================================================

  @Test
  @Order(1)
  @DisplayName("Test 1: Player should create self-exclusion request successfully")
  void shouldCreateSelfExclusionRequest() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("selfexclusion"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create 24-hour DEPOSIT self-exclusion
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "DEPOSIT", // Block deposits only
            "24_HOURS", // 24-hour exclusion
            "Test self-exclusion for responsible gaming",
            "127.0.0.1",
            "TestAgent/1.0",
            true // Compliance acknowledgement
            );

    // Assertions: Request created successfully
    assertThat(createResult.getOk()).isTrue();
    assertThat(createResult.getData()).isNotNull();
    Long requestId = createResult.getData();

    // Verify request entity in database
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    assertThat(request).isNotNull();
    assertThat(request.getPlayerId()).isEqualTo(playerId);
    assertThat(request.getExclusionType()).isEqualTo("DEPOSIT");
    assertThat(request.getDurationType()).isEqualTo("24_HOURS");
    assertThat(request.getStatus()).isEqualTo("ACTIVE");
    assertThat(request.getStartDate()).isNotNull();
    assertThat(request.getEndDate()).isNotNull();
    assertThat(request.getCoolingOffPeriodEnd()).isNotNull();
    assertThat(request.getComplianceAcknowledgement()).isTrue();

    // Verify end date is 24 hours after start date
    long hoursDiff =
        java.time.Duration.between(request.getStartDate(), request.getEndDate()).toHours();
    assertThat(hoursDiff).isEqualTo(24);

    // Verify cooling-off period is 24 hours (same as duration for 24H exclusions)
    long coolingOffHours =
        java.time.Duration.between(request.getStartDate(), request.getCoolingOffPeriodEnd())
            .toHours();
    assertThat(coolingOffHours).isEqualTo(24);

    // Verify history record created
    LambdaQueryWrapper<SelfExclusionHistoryEntity> historyQuery =
        new LambdaQueryWrapper<SelfExclusionHistoryEntity>()
            .eq(SelfExclusionHistoryEntity::getRequestId, requestId)
            .eq(SelfExclusionHistoryEntity::getActionType, "CREATED");

    SelfExclusionHistoryEntity history = selfExclusionHistoryDao.selectOne(historyQuery);
    assertThat(history).isNotNull();
    assertThat(history.getPlayerId()).isEqualTo(playerId);
    assertThat(history.getActionType()).isEqualTo("CREATED");
    assertThat(history.getPreviousStatus()).isNull(); // First state change
    assertThat(history.getNewStatus()).isEqualTo("ACTIVE");
  }

  // ========================================================================
  // Test 2: Deposit Restriction Enforcement
  // ========================================================================

  @Test
  @Order(2)
  @DisplayName("Test 2: Deposit should be blocked during active DEPOSIT exclusion")
  void shouldBlockDepositDuringActiveExclusion() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("depositblock"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create DEPOSIT self-exclusion
    selfExclusionRequestService.createSelfExclusionRequest(
        playerId,
        "DEPOSIT",
        "7_DAYS",
        "Block deposits for 7 days",
        "127.0.0.1",
        "TestAgent/1.0",
        true);

    // Step 3: Attempt to check if deposit is allowed
    ResponseDTO<Void> checkResult = selfExclusionEnforcementService.checkDepositAllowed(playerId);

    // Assertions: Deposit should be blocked
    assertThat(checkResult.getOk()).isFalse();
    assertThat(checkResult.getMsg()).contains("自我排除");
    assertThat(checkResult.getMsg()).contains("存款操作");
  }

  // ========================================================================
  // Test 3: Betting Restriction Enforcement
  // ========================================================================

  @Test
  @Order(3)
  @DisplayName("Test 3: Betting should be blocked during active BETTING exclusion")
  void shouldBlockBettingDuringActiveExclusion() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("bettingblock"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create BETTING self-exclusion
    selfExclusionRequestService.createSelfExclusionRequest(
        playerId,
        "BETTING",
        "30_DAYS",
        "Block betting for 30 days",
        "127.0.0.1",
        "TestAgent/1.0",
        true);

    // Step 3: Attempt to check if betting is allowed
    ResponseDTO<Void> checkResult = selfExclusionEnforcementService.checkBettingAllowed(playerId);

    // Assertions: Betting should be blocked
    assertThat(checkResult.getOk()).isFalse();
    assertThat(checkResult.getMsg()).contains("自我排除");
    assertThat(checkResult.getMsg()).contains("投注操作");
  }

  // ========================================================================
  // Test 4: Login Restriction Enforcement
  // ========================================================================

  @Test
  @Order(4)
  @DisplayName("Test 4: Login should be blocked during active LOGIN exclusion")
  void shouldBlockLoginDuringActiveExclusion() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("loginblock"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create LOGIN self-exclusion
    selfExclusionRequestService.createSelfExclusionRequest(
        playerId,
        "LOGIN",
        "6_MONTHS",
        "Block login for 6 months",
        "127.0.0.1",
        "TestAgent/1.0",
        true);

    // Step 3: Attempt to check if login is allowed
    ResponseDTO<Void> checkResult = selfExclusionEnforcementService.checkLoginAllowed(playerId);

    // Assertions: Login should be blocked
    assertThat(checkResult.getOk()).isFalse();
    assertThat(checkResult.getMsg()).contains("自我排除");
    assertThat(checkResult.getMsg()).contains("登入");
  }

  // ========================================================================
  // Test 5: Cooling-Off Period Prevents Premature Removal
  // ========================================================================

  @Test
  @Order(5)
  @DisplayName(
      "Test 5: Player should NOT be able to request removal before cooling-off period ends")
  void shouldPreventRemovalBeforeCoolingOffPeriodEnds() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("coolingoff"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create 7-day DEPOSIT self-exclusion (cooling-off: 7 days)
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "DEPOSIT",
            "7_DAYS",
            "Testing cooling-off period",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Step 3: Immediately attempt to request removal (within cooling-off period)
    ResponseDTO<Void> removalResult =
        selfExclusionReviewService.requestRemoval(requestId, playerId);

    // Assertions: Removal should be rejected
    assertThat(removalResult.getOk()).isFalse();
    assertThat(removalResult.getMsg()).contains("冷靜期未結束");
    assertThat(removalResult.getMsg()).contains("最早可申請解除日期");

    // Verify removalRequestDate is NOT set
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    assertThat(request.getRemovalRequestDate()).isNull();
  }

  // ========================================================================
  // Test 6: Player Can Request Removal After Cooling-Off Period
  // ========================================================================

  @Test
  @Order(6)
  @DisplayName("Test 6: Player should be able to request removal after cooling-off period ends")
  void shouldAllowRemovalRequestAfterCoolingOff() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("removalallowed"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create 24-hour DEPOSIT self-exclusion
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "DEPOSIT",
            "24_HOURS",
            "Test removal after cooling-off",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Step 3: Fast-forward cooling-off period by updating database timestamp
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    request.setCoolingOffPeriodEnd(
        OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1)); // Set to 1 hour ago
    selfExclusionRequestDao.updateById(request);

    // Step 4: Request removal (should succeed now)
    ResponseDTO<Void> removalResult =
        selfExclusionReviewService.requestRemoval(requestId, playerId);

    // Assertions: Removal request accepted
    assertThat(removalResult.getOk()).isTrue();

    // Verify removalRequestDate is set
    SelfExclusionRequestEntity updated = selfExclusionRequestDao.selectById(requestId);
    assertThat(updated.getRemovalRequestDate()).isNotNull();
    assertThat(updated.getStatus()).isEqualTo("ACTIVE"); // Still active, pending admin approval

    // Verify REMOVAL_REQUESTED history record created
    LambdaQueryWrapper<SelfExclusionHistoryEntity> historyQuery =
        new LambdaQueryWrapper<SelfExclusionHistoryEntity>()
            .eq(SelfExclusionHistoryEntity::getRequestId, requestId)
            .eq(SelfExclusionHistoryEntity::getActionType, "REMOVAL_REQUESTED");

    SelfExclusionHistoryEntity history = selfExclusionHistoryDao.selectOne(historyQuery);
    assertThat(history).isNotNull();
    assertThat(history.getActionBy()).isNull(); // Player action, not admin
    assertThat(history.getPreviousStatus()).isEqualTo("ACTIVE");
    assertThat(history.getNewStatus()).isEqualTo("ACTIVE");
  }

  // ========================================================================
  // Test 7: Admin Approves Removal Request
  // ========================================================================

  @Test
  @Order(7)
  @DisplayName("Test 7: Admin should be able to approve removal request")
  void shouldApproveRemovalRequest() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("approvaltest"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create self-exclusion and fast-forward cooling-off period
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "DEPOSIT",
            "24_HOURS",
            "Test admin approval",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Fast-forward cooling-off period
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    request.setCoolingOffPeriodEnd(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1));
    selfExclusionRequestDao.updateById(request);

    // Step 3: Player requests removal
    selfExclusionReviewService.requestRemoval(requestId, playerId);

    // Step 4: Admin approves removal
    Long adminId = 100L; // Mock admin employee ID
    String approvalReason = "Player completed cooling-off period and verified identity";

    ResponseDTO<Void> approvalResult =
        selfExclusionReviewService.approveRemoval(requestId, adminId, approvalReason);

    // Assertions: Approval successful
    assertThat(approvalResult.getOk()).isTrue();

    // Verify request status changed to REMOVED
    SelfExclusionRequestEntity approved = selfExclusionRequestDao.selectById(requestId);
    assertThat(approved.getStatus()).isEqualTo("REMOVED");
    assertThat(approved.getRemovalApprovedBy()).isEqualTo(adminId);
    assertThat(approved.getRemovalApprovedAt()).isNotNull();
    assertThat(approved.getRemovalReason()).isEqualTo(approvalReason);

    // Verify REMOVAL_APPROVED history record created
    LambdaQueryWrapper<SelfExclusionHistoryEntity> historyQuery =
        new LambdaQueryWrapper<SelfExclusionHistoryEntity>()
            .eq(SelfExclusionHistoryEntity::getRequestId, requestId)
            .eq(SelfExclusionHistoryEntity::getActionType, "REMOVAL_APPROVED");

    SelfExclusionHistoryEntity history = selfExclusionHistoryDao.selectOne(historyQuery);
    assertThat(history).isNotNull();
    assertThat(history.getActionBy()).isEqualTo(adminId);
    assertThat(history.getPreviousStatus()).isEqualTo("ACTIVE");
    assertThat(history.getNewStatus()).isEqualTo("REMOVED");
    assertThat(history.getMetadata()).isNotNull();
    assertThat(history.getMetadata()).containsKey("admin_id");

    // Verify restriction is lifted (deposit should now be allowed)
    ResponseDTO<Void> checkDeposit = selfExclusionEnforcementService.checkDepositAllowed(playerId);
    assertThat(checkDeposit.getOk()).isTrue();
  }

  // ========================================================================
  // Test 8: Admin Rejects Removal Request
  // ========================================================================

  @Test
  @Order(8)
  @DisplayName("Test 8: Admin should be able to reject removal request")
  void shouldRejectRemovalRequest() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("rejectiontest"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create self-exclusion and fast-forward cooling-off period
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "BETTING",
            "7_DAYS",
            "Test admin rejection",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Fast-forward cooling-off period
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    request.setCoolingOffPeriodEnd(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1));
    selfExclusionRequestDao.updateById(request);

    // Step 3: Player requests removal
    selfExclusionReviewService.requestRemoval(requestId, playerId);

    // Step 4: Admin rejects removal
    Long adminId = 101L;
    String rejectionReason = "Insufficient evidence of recovery from gambling problem";

    ResponseDTO<Void> rejectionResult =
        selfExclusionReviewService.rejectRemoval(requestId, adminId, rejectionReason);

    // Assertions: Rejection successful
    assertThat(rejectionResult.getOk()).isTrue();

    // Verify request status remains ACTIVE
    SelfExclusionRequestEntity rejected = selfExclusionRequestDao.selectById(requestId);
    assertThat(rejected.getStatus()).isEqualTo("ACTIVE");
    assertThat(rejected.getRemovalRequestDate()).isNull(); // Cleared so player can try again later
    assertThat(rejected.getRemovalReason()).contains("Insufficient evidence");

    // Verify REMOVAL_REJECTED history record created
    LambdaQueryWrapper<SelfExclusionHistoryEntity> historyQuery =
        new LambdaQueryWrapper<SelfExclusionHistoryEntity>()
            .eq(SelfExclusionHistoryEntity::getRequestId, requestId)
            .eq(SelfExclusionHistoryEntity::getActionType, "REMOVAL_REJECTED");

    SelfExclusionHistoryEntity history = selfExclusionHistoryDao.selectOne(historyQuery);
    assertThat(history).isNotNull();
    assertThat(history.getActionBy()).isEqualTo(adminId);
    assertThat(history.getPreviousStatus()).isEqualTo("ACTIVE");
    assertThat(history.getNewStatus()).isEqualTo("ACTIVE");

    // Verify restriction is still enforced (betting should still be blocked)
    ResponseDTO<Void> checkBetting = selfExclusionEnforcementService.checkBettingAllowed(playerId);
    assertThat(checkBetting.getOk()).isFalse();
  }

  // ========================================================================
  // Test 9: Admin Cancels Active Exclusion Request
  // ========================================================================

  @Test
  @Order(9)
  @DisplayName("Test 9: Admin should be able to cancel active exclusion request")
  void shouldCancelActiveExclusionRequest() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("cancellationtest"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create PERMANENT self-exclusion
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "FULL_BLOCK",
            "PERMANENT",
            "Block all activities permanently",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Step 3: Admin cancels request (e.g., player created account with stolen identity)
    Long adminId = 102L;
    String cancellationReason = "Player verified identity was fraudulent, account closed";

    ResponseDTO<Void> cancellationResult =
        selfExclusionReviewService.cancelRequest(requestId, adminId, cancellationReason);

    // Assertions: Cancellation successful
    assertThat(cancellationResult.getOk()).isTrue();

    // Verify request status changed to CANCELLED
    SelfExclusionRequestEntity cancelled = selfExclusionRequestDao.selectById(requestId);
    assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    assertThat(cancelled.getRemovalReason()).contains("fraudulent");

    // Verify CANCELLED history record created
    LambdaQueryWrapper<SelfExclusionHistoryEntity> historyQuery =
        new LambdaQueryWrapper<SelfExclusionHistoryEntity>()
            .eq(SelfExclusionHistoryEntity::getRequestId, requestId)
            .eq(SelfExclusionHistoryEntity::getActionType, "CANCELLED");

    SelfExclusionHistoryEntity history = selfExclusionHistoryDao.selectOne(historyQuery);
    assertThat(history).isNotNull();
    assertThat(history.getActionBy()).isEqualTo(adminId);
    assertThat(history.getPreviousStatus()).isEqualTo("ACTIVE");
    assertThat(history.getNewStatus()).isEqualTo("CANCELLED");

    // Verify restriction is lifted (all checks should now pass)
    ResponseDTO<Void> checkDeposit = selfExclusionEnforcementService.checkDepositAllowed(playerId);
    ResponseDTO<Void> checkBetting = selfExclusionEnforcementService.checkBettingAllowed(playerId);
    ResponseDTO<Void> checkLogin = selfExclusionEnforcementService.checkLoginAllowed(playerId);

    assertThat(checkDeposit.getOk()).isTrue();
    assertThat(checkBetting.getOk()).isTrue();
    assertThat(checkLogin.getOk()).isTrue();
  }

  // ========================================================================
  // Test 10: Automatic Expiry of Completed Exclusion Requests
  // ========================================================================

  @Test
  @Order(10)
  @DisplayName("Test 10: System should automatically expire completed exclusion requests")
  void shouldAutomaticallyExpireCompletedRequests() {
    // Step 1: Register a test player
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(
            PlayerRegistrationTestFixture.generateUniqueUsername("expirytest"));
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    assertThat(regResult.getOk()).isTrue();
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Step 2: Create 24-hour DEPOSIT self-exclusion
    ResponseDTO<Long> createResult =
        selfExclusionRequestService.createSelfExclusionRequest(
            playerId,
            "DEPOSIT",
            "24_HOURS",
            "Test auto-expiry",
            "127.0.0.1",
            "TestAgent/1.0",
            true);

    Long requestId = createResult.getData();

    // Step 3: Fast-forward past end date
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);
    request.setEndDate(
        OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1)); // Set to 1 hour ago
    selfExclusionRequestDao.updateById(request);

    // Step 4: Run expiry job
    int expiredCount = selfExclusionEnforcementService.expireOutdatedRequests();

    // Assertions: At least 1 request was expired
    assertThat(expiredCount).isGreaterThanOrEqualTo(1);

    // Verify request status changed to EXPIRED
    SelfExclusionRequestEntity expired = selfExclusionRequestDao.selectById(requestId);
    assertThat(expired.getStatus()).isEqualTo("EXPIRED");

    // Verify restriction is lifted (deposit should now be allowed)
    ResponseDTO<Void> checkDeposit = selfExclusionEnforcementService.checkDepositAllowed(playerId);
    assertThat(checkDeposit.getOk()).isTrue();

    // Verify EXPIRED history record created (optional - may or may not be implemented)
    // Note: Based on the service code, expireOutdatedRequests() may or may not create history
    // records. This is a design decision for the scheduled job.
  }
}
