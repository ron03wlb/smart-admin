package net.lab1024.sa.igaming.integration.journey;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.integration.payment.FirstDepositBonusIntegrationService;
import net.lab1024.sa.igaming.integration.payment.domain.form.DepositCallbackForm;
import net.lab1024.sa.igaming.integration.payment.domain.vo.DepositResultVO;
import net.lab1024.sa.igaming.integration.player.PlayerRegistrationIntegrationService;
import net.lab1024.sa.igaming.integration.player.domain.form.PlayerRegistrationIntegrationForm;
import net.lab1024.sa.igaming.integration.player.domain.vo.PlayerRegistrationResultVO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Player registration journey integration test - end-to-end flow validation.
 *
 * <p>This test class validates the complete player registration and first deposit bonus flow across
 * multiple modules:
 *
 * <ul>
 *   <li><b>Task 1.1</b>: Player Registration Integration (Player + Wallet modules)
 *   <li><b>Task 1.2</b>: First Deposit Bonus Flow (Payment + Activity modules)
 * </ul>
 *
 * <p><b>Test Scenarios:</b>
 *
 * <ol>
 *   <li>Complete registration flow (KYC L0 → CASH + BONUS wallet creation → Event publishing)
 *   <li>First deposit bonus flow (Deposit → First deposit check → Bonus award → Wallet credit)
 *   <li>Multi-tenant isolation (Tenant 1 data not visible to Tenant 2)
 *   <li>Idempotency validation (Duplicate registration requests handled correctly)
 * </ol>
 *
 * <p><b>Test Infrastructure:</b>
 *
 * <ul>
 *   <li>Testcontainers: PostgreSQL 16-alpine
 *   <li>Spring Boot Test Context: Full integration module loaded
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@DisplayName("Player Registration Journey Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class PlayerRegistrationJourneyIntegrationTest extends BaseIntegrationTest {

  @Autowired private PlayerRegistrationIntegrationService playerRegistrationService;
  @Autowired private FirstDepositBonusIntegrationService firstDepositBonusService;

  // DAOs for verification
  @Autowired private PlayerDao playerDao;
  @Autowired private WalletDao walletDao;
  @Autowired private PaymentOrderDao paymentOrderDao;
  @Autowired private PromotionRuleDao promotionRuleDao;
  @Autowired private PlayerBonusRecordDao bonusRecordDao;

  @BeforeEach
  void setUp() {
    // Reset tenant context before each test
    TenantContext.setTenantId(1L);

    // Clean up promotion rules and bonus records from previous tests
    // Note: BaseIntegrationTest already handles cleanup for players, wallets, and payment orders
    // This additional cleanup is only for test-specific data
    bonusRecordDao.delete(Wrappers.lambdaQuery());
    promotionRuleDao.delete(Wrappers.lambdaQuery());
  }

  @AfterEach
  void tearDown() {
    // Clear tenant context after each test
    TenantContext.clear();
  }

  // ==================================================================================
  // Test Scenario 1: Complete Player Registration Flow
  // ==================================================================================

  @Test
  @Order(1)
  @DisplayName("Should register player with CASH and BONUS wallets")
  void shouldRegisterPlayerWithWallets() {
    // Arrange
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("player");
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(username);

    // Act
    ResponseDTO<PlayerRegistrationResultVO> result =
        playerRegistrationService.registerPlayerWithWallet(form);

    // Assert - Response validation
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getPlayer()).isNotNull();
    assertThat(result.getData().getPlayer().getPlayerId()).isNotNull();
    assertThat(result.getData().getPlayer().getUsername()).isEqualTo(username);
    assertThat(result.getData().getWallets()).hasSize(2); // CASH + BONUS

    Long playerId = result.getData().getPlayer().getPlayerId();

    // Assert - Player entity created
    PlayerEntity player = playerDao.selectById(playerId);
    assertThat(player).isNotNull();
    assertThat(player.getUsername()).isEqualTo(username);
    assertThat(player.getEmailEncrypted()).isEqualTo(username + "@test.com");

    // Assert - CASH wallet created
    WalletVO cashWallet =
        result.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
            .findFirst()
            .orElse(null);
    assertThat(cashWallet).isNotNull();
    assertThat(cashWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(cashWallet.getCurrencyCode()).isEqualTo("USD");

    // Assert - BONUS wallet created
    WalletVO bonusWallet =
        result.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.BONUS.getValue()))
            .findFirst()
            .orElse(null);
    assertThat(bonusWallet).isNotNull();
    assertThat(bonusWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @Order(2)
  @DisplayName("Should register player with referral code")
  void shouldRegisterPlayerWithReferralCode() {
    // Arrange
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("vip");
    String referralCode = "REF123";
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationFormWithReferral(username, referralCode);

    // Act
    ResponseDTO<PlayerRegistrationResultVO> result =
        playerRegistrationService.registerPlayerWithWallet(form);

    // Assert
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getPlayer().getPlayerId()).isNotNull();
    assertThat(result.getData().getReferralCode()).isEqualTo(referralCode);

    // Note: Referral processing is handled by Player module internally
    // This test verifies that referral code does not break registration flow
  }

  @Test
  @Order(3)
  @DisplayName("Should fail registration with duplicate username")
  void shouldFailRegistrationWithDuplicateUsername() {
    // Arrange
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("duplicate");
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(username);

    // Act - First registration (should succeed)
    ResponseDTO<PlayerRegistrationResultVO> firstResult =
        playerRegistrationService.registerPlayerWithWallet(form);
    assertThat(firstResult.getOk()).isTrue();

    // Act - Second registration with same username (should fail)
    ResponseDTO<PlayerRegistrationResultVO> secondResult =
        playerRegistrationService.registerPlayerWithWallet(form);

    // Assert - Duplicate registration rejected
    assertThat(secondResult.getOk()).isFalse();
    // Error code depends on Player module's duplicate handling logic
  }

  // ==================================================================================
  // Test Scenario 2: First Deposit Bonus Flow
  // ==================================================================================

  @Test
  @Order(4)
  @DisplayName("Should award first deposit bonus on first deposit")
  void shouldAwardFirstDepositBonus() {
    // Arrange - Setup promotion rule
    PromotionRuleEntity bonusRule = PlayerRegistrationTestFixture.createFirstDepositBonusRule();
    promotionRuleDao.insert(bonusRule);

    // Arrange - Register player
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("firstdeposit");
    PlayerRegistrationIntegrationForm regForm =
        PlayerRegistrationTestFixture.createRegistrationForm(username);
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(regForm);
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Arrange - Get CASH wallet ID
    Long cashWalletId =
        regResult.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
            .findFirst()
            .map(w -> w.getWalletId())
            .orElseThrow(() -> new IllegalStateException("CASH wallet not found"));

    // Arrange - Create payment order
    String orderNo = PlayerRegistrationTestFixture.generateUniqueOrderNo("DEP");
    PaymentOrderEntity order =
        PlayerRegistrationTestFixture.createPaymentOrder(
            playerId,
            cashWalletId,
            orderNo,
            new BigDecimal("100.00"),
            PaymentOrderStatusEnum.PENDING.getValue());
    paymentOrderDao.insert(order);

    // Arrange - Deposit callback form
    DepositCallbackForm depositForm =
        PlayerRegistrationTestFixture.createDepositCallbackForm(
            orderNo, "PSP-TX-" + System.currentTimeMillis());

    // Act - Process first deposit
    ResponseDTO<DepositResultVO> depositResult =
        firstDepositBonusService.processFirstDeposit(depositForm);

    // Assert - Deposit processed successfully
    assertThat(depositResult.getOk()).isTrue();
    assertThat(depositResult.getData()).isNotNull();
    assertThat(depositResult.getData().getPlayerId()).isEqualTo(playerId);
    assertThat(depositResult.getData().getIsFirstDeposit()).isTrue();

    // Assert - CASH wallet credited
    assertThat(depositResult.getData().getDepositAmount()).isEqualByComparingTo("100.00");
    assertThat(depositResult.getData().getCashBalance()).isEqualByComparingTo("100.00");

    // Assert - Bonus awarded
    assertThat(depositResult.getData().getBonusAmount()).isNotNull();
    assertThat(depositResult.getData().getBonusAmount())
        .isEqualByComparingTo("50.00"); // 50% of 100 = 50
    assertThat(depositResult.getData().getBonusBalance()).isEqualByComparingTo("50.00");

    // Assert - Bonus record created
    PlayerBonusRecordEntity bonusRecord =
        bonusRecordDao.selectOne(
            Wrappers.<PlayerBonusRecordEntity>lambdaQuery()
                .eq(PlayerBonusRecordEntity::getPlayerId, playerId)
                .eq(PlayerBonusRecordEntity::getRuleId, bonusRule.getRuleId()));
    assertThat(bonusRecord).isNotNull();
    assertThat(bonusRecord.getBonusAmount()).isEqualByComparingTo("50.00");
    assertThat(bonusRecord.getWageringRequired())
        .isEqualByComparingTo("1000.00"); // 50 * 20x = 1000
    assertThat(bonusRecord.getStatus()).isEqualTo(BonusRecordStatusEnum.ACTIVE.getValue());
  }

  @Test
  @Order(5)
  @DisplayName("Should NOT award bonus on second deposit")
  void shouldNotAwardBonusOnSecondDeposit() {
    // Arrange - Setup promotion rule
    PromotionRuleEntity bonusRule = PlayerRegistrationTestFixture.createFirstDepositBonusRule();
    promotionRuleDao.insert(bonusRule);

    // Arrange - Register player
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("seconddeposit");
    PlayerRegistrationIntegrationForm regForm =
        PlayerRegistrationTestFixture.createRegistrationForm(username);
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(regForm);
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Arrange - Get CASH wallet ID
    Long cashWalletId =
        regResult.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
            .findFirst()
            .map(w -> w.getWalletId())
            .orElseThrow(() -> new IllegalStateException("CASH wallet not found"));

    // Arrange - First deposit (SUCCESS)
    String firstOrderNo = PlayerRegistrationTestFixture.generateUniqueOrderNo("DEP");
    PaymentOrderEntity firstOrder =
        PlayerRegistrationTestFixture.createPaymentOrder(
            playerId,
            cashWalletId,
            firstOrderNo,
            new BigDecimal("100.00"),
            PaymentOrderStatusEnum.SUCCESS.getValue());
    paymentOrderDao.insert(firstOrder);

    // Arrange - Second deposit
    String secondOrderNo = PlayerRegistrationTestFixture.generateUniqueOrderNo("DEP");
    PaymentOrderEntity secondOrder =
        PlayerRegistrationTestFixture.createPaymentOrder(
            playerId,
            cashWalletId,
            secondOrderNo,
            new BigDecimal("200.00"),
            PaymentOrderStatusEnum.PENDING.getValue());
    paymentOrderDao.insert(secondOrder);

    // Act - Process second deposit
    DepositCallbackForm depositForm =
        PlayerRegistrationTestFixture.createDepositCallbackForm(
            secondOrderNo, "PSP-TX-" + System.currentTimeMillis());
    ResponseDTO<DepositResultVO> depositResult =
        firstDepositBonusService.processFirstDeposit(depositForm);

    // Assert - Deposit processed successfully but NO bonus
    assertThat(depositResult.getOk()).isTrue();
    assertThat(depositResult.getData().getIsFirstDeposit()).isFalse();
    assertThat(depositResult.getData().getBonusAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @Order(6)
  @DisplayName("Should handle first deposit without active promotion")
  void shouldHandleFirstDepositWithoutPromotion() {
    // Arrange - Register player (NO promotion rule setup)
    String username = PlayerRegistrationTestFixture.generateUniqueUsername("nopromo");
    PlayerRegistrationIntegrationForm regForm =
        PlayerRegistrationTestFixture.createRegistrationForm(username);
    ResponseDTO<PlayerRegistrationResultVO> regResult =
        playerRegistrationService.registerPlayerWithWallet(regForm);
    Long playerId = regResult.getData().getPlayer().getPlayerId();

    // Arrange - Get CASH wallet ID
    Long cashWalletId =
        regResult.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
            .findFirst()
            .map(w -> w.getWalletId())
            .orElseThrow(() -> new IllegalStateException("CASH wallet not found"));

    // Arrange - Payment order
    String orderNo = PlayerRegistrationTestFixture.generateUniqueOrderNo("DEP");
    PaymentOrderEntity order =
        PlayerRegistrationTestFixture.createPaymentOrder(
            playerId,
            cashWalletId,
            orderNo,
            new BigDecimal("100.00"),
            PaymentOrderStatusEnum.PENDING.getValue());
    paymentOrderDao.insert(order);

    // Act - Process first deposit
    DepositCallbackForm depositForm =
        PlayerRegistrationTestFixture.createDepositCallbackForm(
            orderNo, "PSP-TX-" + System.currentTimeMillis());
    ResponseDTO<DepositResultVO> depositResult =
        firstDepositBonusService.processFirstDeposit(depositForm);

    // Assert - Deposit processed successfully but NO bonus (no promotion found)
    assertThat(depositResult.getOk()).isTrue();
    assertThat(depositResult.getData().getIsFirstDeposit()).isTrue();
    assertThat(depositResult.getData().getBonusAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // ==================================================================================
  // Test Scenario 3: Multi-Tenant Isolation
  // ==================================================================================

  @Test
  @Order(7)
  @DisplayName("Should isolate player data across tenants")
  void shouldIsolateTenantData() {
    // Arrange - Tenant 1 player registration
    TenantContext.setTenantId(1L);
    String tenant1Username = PlayerRegistrationTestFixture.generateUniqueUsername("tenant1");
    PlayerRegistrationIntegrationForm form1 =
        PlayerRegistrationTestFixture.createRegistrationForm(tenant1Username);
    ResponseDTO<PlayerRegistrationResultVO> tenant1Result =
        playerRegistrationService.registerPlayerWithWallet(form1);
    Long tenant1PlayerId = tenant1Result.getData().getPlayer().getPlayerId();

    // Arrange - Tenant 2 player registration
    TenantContext.setTenantId(2L);
    String tenant2Username = PlayerRegistrationTestFixture.generateUniqueUsername("tenant2");
    PlayerRegistrationIntegrationForm form2 =
        PlayerRegistrationTestFixture.createRegistrationForm(tenant2Username);
    ResponseDTO<PlayerRegistrationResultVO> tenant2Result =
        playerRegistrationService.registerPlayerWithWallet(form2);
    Long tenant2PlayerId = tenant2Result.getData().getPlayer().getPlayerId();

    // Assert - Both registrations succeeded
    assertThat(tenant1Result.getOk()).isTrue();
    assertThat(tenant2Result.getOk()).isTrue();

    // Assert - Tenant 1 player not visible to Tenant 2
    TenantContext.setTenantId(2L);
    PlayerEntity tenant1PlayerFromTenant2 = playerDao.selectById(tenant1PlayerId);
    // Note: MyBatis Plus interceptor should filter by tenant_id
    // If tenant isolation is working, this should return null or be filtered
    assertThat(tenant1PlayerFromTenant2).isNull();

    // Assert - Tenant 2 player not visible to Tenant 1
    TenantContext.setTenantId(1L);
    PlayerEntity tenant2PlayerFromTenant1 = playerDao.selectById(tenant2PlayerId);
    assertThat(tenant2PlayerFromTenant1).isNull();
  }

  @Test
  @Order(8)
  @DisplayName("Should isolate wallet data across tenants")
  void shouldIsolateWalletAcrossTenants() {
    // Arrange - Tenant 1 player with wallets
    TenantContext.setTenantId(1L);
    String tenant1Username = PlayerRegistrationTestFixture.generateUniqueUsername("wallet1");
    PlayerRegistrationIntegrationForm form =
        PlayerRegistrationTestFixture.createRegistrationForm(tenant1Username);
    ResponseDTO<PlayerRegistrationResultVO> result =
        playerRegistrationService.registerPlayerWithWallet(form);
    Long playerId = result.getData().getPlayer().getPlayerId();

    WalletVO cashWallet =
        result.getData().getWallets().stream()
            .filter(w -> w.getWalletType().equals(WalletTypeEnum.CASH.getValue()))
            .findFirst()
            .orElse(null);
    assertThat(cashWallet).isNotNull();
    Long cashWalletId = cashWallet.getWalletId();

    // Act - Switch to Tenant 2 and try to query Tenant 1 wallet
    TenantContext.setTenantId(2L);
    WalletEntity walletFromWrongTenant = walletDao.selectById(cashWalletId);

    // Assert - Wallet not visible across tenants
    assertThat(walletFromWrongTenant).isNull();
  }

  // ==================================================================================
  // Test Scenario 4: Edge Cases & Error Handling
  // ==================================================================================

  @Test
  @Order(9)
  @DisplayName("Should handle invalid deposit callback gracefully")
  void shouldHandleInvalidDepositCallback() {
    // Arrange - Deposit callback for non-existent order
    String invalidOrderNo = "INVALID-ORDER-999";
    DepositCallbackForm invalidForm =
        PlayerRegistrationTestFixture.createDepositCallbackForm(invalidOrderNo, "PSP-TX-INVALID");

    // Act - Process invalid deposit
    ResponseDTO<DepositResultVO> result = firstDepositBonusService.processFirstDeposit(invalidForm);

    // Assert - Error response returned (order not found)
    assertThat(result.getOk()).isFalse();
    // Error code/message depends on PaymentService implementation
  }

  @Test
  @Order(10)
  @DisplayName("Should validate registration form constraints")
  void shouldValidateRegistrationFormConstraints() {
    // Arrange - Invalid form (username too short)
    PlayerRegistrationIntegrationForm invalidForm = new PlayerRegistrationIntegrationForm();
    invalidForm.setUsername("ab"); // Too short (min 3 chars)
    invalidForm.setPassword("Test@1234");

    // Act & Assert - Jakarta Validation should throw ConstraintViolationException
    // Note: Validation occurs at method entry (before service logic executes)
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> playerRegistrationService.registerPlayerWithWallet(invalidForm))
        .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
        .hasMessageContaining("username must be 3-50 characters");
  }
}
