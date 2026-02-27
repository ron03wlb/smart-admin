package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.security.config.Argon2Properties;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.BlindIndexService;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;
import net.lab1024.sa.common.security.service.PasswordEncryptService;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.PlayerRegistrationManager;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import net.lab1024.sa.igaming.player.service.PlayerService;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Player integration tests — verifies player lifecycle operations against real PostgreSQL 16 via
 * Testcontainers.
 *
 * <p>Uses a sliced Spring context ({@link TestConfig}) that loads player + wallet manager/service
 * beans + MyBatis-Plus + security services. PlayerAuthService is excluded (requires Sa-Token web
 * context); player creation uses {@link PlayerRegistrationManager} directly.
 *
 * <p>Validates: registration (player + wallet atomic creation), state machine transitions with
 * audit log, KYC level checks and document submission, and VIP level changes with change log.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Tag("integration")
@SpringBootTest(
    classes = PlayerIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = PlayerIntegrationTest.SchemaInitializer.class)
@DisplayName("Player 整合測試 (Testcontainers + PostgreSQL)")
class PlayerIntegrationTest {

  /**
   * Sliced Spring context — loads player + wallet beans + MyBatis-Plus + security services.
   * Excludes PlayerAuthService (requires Sa-Token web context).
   */
  @Configuration
  @ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
  })
  @Import({
    net.lab1024.sa.common.mybatis.handler.MybatisPlusFillHandler.class,
  })
  @ComponentScan(
      basePackages = {
        "net.lab1024.sa.igaming.player.service",
        "net.lab1024.sa.igaming.player.manager",
        "net.lab1024.sa.igaming.wallet.service",
        "net.lab1024.sa.igaming.wallet.manager",
      },
      excludeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = net.lab1024.sa.igaming.player.service.PlayerAuthService.class))
  @MapperScan(
      value = {"net.lab1024.sa.igaming.player.dao", "net.lab1024.sa.igaming.wallet.dao"},
      annotationClass = Mapper.class)
  static class TestConfig {

    @Bean
    public com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
        mybatisPlusInterceptor() {
      var interceptor = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
      interceptor.addInnerInterceptor(
          new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
      interceptor.addInnerInterceptor(
          new com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor(
              com.baomidou.mybatisplus.annotation.DbType.POSTGRE_SQL));
      return interceptor;
    }

    @Bean
    public net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher domainEventPublisher() {
      return org.mockito.Mockito.mock(
          net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher.class);
    }

    @Bean
    public PasswordEncryptService passwordEncryptService() {
      return new PasswordEncryptService(new Argon2Properties());
    }

    @Bean
    public BlindIndexService blindIndexService() throws Exception {
      KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
      keyGen.init(256);
      SecretKey key = keyGen.generateKey();
      return new BlindIndexService(key);
    }

    @Bean
    public AesGcmFieldEncryptService aesGcmFieldEncryptService() throws Exception {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      SecretKey key = keyGen.generateKey();
      AesGcmFieldEncryptService service = new AesGcmFieldEncryptService(key);
      EncryptedFieldTypeHandler.setEncryptService(service);
      return service;
    }
  }

  private static final Long TEST_TENANT_ID = 1L;

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smart_admin_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("mybatis-plus.mapper-locations", () -> "classpath*:/mapper/**/*.xml");
    registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    registry.add("tenant.enabled", () -> false);
  }

  static class SchemaInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      try (Connection conn =
          DriverManager.getConnection(
              POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
        try {
          conn.createStatement().execute("CREATE ROLE smartadmin_app LOGIN");
        } catch (SQLException ignored) {
          // Role already exists
        }
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V10__player_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize player test schema", e);
      }
    }
  }

  @Autowired private PlayerRegistrationManager playerRegistrationManager;
  @Autowired private PlayerService playerService;
  @Autowired private KycVerificationService kycVerificationService;
  @Autowired private PlayerDao playerDao;
  @Autowired private BlindIndexService blindIndexService;
  @Autowired private PasswordEncryptService passwordEncryptService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_player_audit_log, t_vip_change_log, t_kyc_document, t_wallet_lock,"
            + " t_wallet_bonus_ext, t_wallet_transaction, t_wallet, t_player CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Registration ====================

  @Nested
  @DisplayName("register 玩家註冊整合測試")
  class RegisterIntegrationTest {

    @Test
    @DisplayName("註冊成功 → 同時建立玩家和錢包")
    void register_success_creates_player_and_wallet() {
      Long playerId = createPlayer("player1", "p1@test.com", "13800000001");

      // Verify player in DB
      PlayerEntity player = playerDao.selectById(playerId);
      assertThat(player).isNotNull();
      assertThat(player.getUsername()).isEqualTo("player1");
      assertThat(player.getStatus()).isEqualTo(PlayerStatusEnum.ACTIVE.getValue());
      assertThat(player.getKycLevel()).isEqualTo(KycLevelEnum.L0.getValue());
      assertThat(player.getVipLevel()).isEqualTo(VipLevelEnum.BRONZE.getValue());
      assertThat(player.getTenantId()).isEqualTo(TEST_TENANT_ID);
      assertThat(player.getCreateTime()).isNotNull();

      // Verify wallet created atomically
      Long walletCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet WHERE player_id = ?", Long.class, playerId);
      assertThat(walletCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("重複 username 拒絕 (UNIQUE constraint)")
    void register_duplicate_username_rejected() {
      createPlayer("dupuser", "dup1@test.com", "13800000010");

      // Second registration with same username fails via DuplicateKeyException
      try {
        createPlayer("dupuser", "dup2@test.com", "13800000011");
      } catch (org.springframework.dao.DuplicateKeyException ignored) {
        // Expected
      }

      // Only one player in DB
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_player WHERE username = ?", Long.class, "dupuser");
      assertThat(count).isEqualTo(1L);
    }
  }

  // ==================== State Machine ====================

  @Nested
  @DisplayName("status transition 狀態轉換整合測試")
  class StatusTransitionIntegrationTest {

    @Test
    @DisplayName("ACTIVE → LOCKED → ACTIVE 成功 + 審計日誌")
    void status_transition_valid() {
      Long playerId = createPlayer("stateuser", "state@test.com", "13800000020");

      // ACTIVE → LOCKED
      var lockResult =
          playerService.changePlayerStatus(
              playerId, PlayerStatusEnum.LOCKED, "admin", "suspicious");
      assertThat(lockResult.getSuccess()).isTrue();

      PlayerEntity locked = playerDao.selectById(playerId);
      assertThat(locked.getStatus()).isEqualTo(PlayerStatusEnum.LOCKED.getValue());

      // Verify audit log
      Long auditCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_player_audit_log WHERE player_id = ?", Long.class, playerId);
      assertThat(auditCount).isEqualTo(1L);

      // LOCKED → ACTIVE
      var unlockResult =
          playerService.changePlayerStatus(playerId, PlayerStatusEnum.ACTIVE, "admin", "resolved");
      assertThat(unlockResult.getSuccess()).isTrue();

      PlayerEntity active = playerDao.selectById(playerId);
      assertThat(active.getStatus()).isEqualTo(PlayerStatusEnum.ACTIVE.getValue());

      // 2 audit log entries total
      Long auditCount2 =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_player_audit_log WHERE player_id = ?", Long.class, playerId);
      assertThat(auditCount2).isEqualTo(2L);
    }

    @Test
    @DisplayName("非法轉換拒絕 (CLOSED → ACTIVE)")
    void status_transition_invalid_rejected() {
      Long playerId = createPlayer("closeduser", "closed@test.com", "13800000021");

      // ACTIVE → CLOSED
      playerService.changePlayerStatus(playerId, PlayerStatusEnum.CLOSED, "admin", "closure");

      // CLOSED → ACTIVE (invalid)
      var result =
          playerService.changePlayerStatus(playerId, PlayerStatusEnum.ACTIVE, "admin", "reopen");
      assertThat(result.getSuccess()).isFalse();

      // Still CLOSED
      PlayerEntity player = playerDao.selectById(playerId);
      assertThat(player.getStatus()).isEqualTo(PlayerStatusEnum.CLOSED.getValue());
    }
  }

  // ==================== KYC ====================

  @Nested
  @DisplayName("KYC 身份驗證整合測試")
  class KycIntegrationTest {

    @Test
    @DisplayName("L1 文件提交 → 自動審核通過 → KYC 升級")
    void kyc_l1_submit_auto_approves() {
      Long playerId = createPlayer("kycuser", "kyc@test.com", "13800000030");

      // Before: L0
      PlayerEntity before = playerDao.selectById(playerId);
      assertThat(before.getKycLevel()).isEqualTo(KycLevelEnum.L0.getValue());

      // Submit L1 document
      var result =
          kycVerificationService.submitL1Document(
              playerId, KycDocumentTypeEnum.ID_CARD, "https://example.com/id.jpg");
      assertThat(result.getSuccess()).isTrue();

      // After: L1
      PlayerEntity after = playerDao.selectById(playerId);
      assertThat(after.getKycLevel()).isEqualTo(KycLevelEnum.L1.getValue());

      // Verify document record
      Long docCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_kyc_document WHERE player_id = ?", Long.class, playerId);
      assertThat(docCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("L0 提款額度限制 → $500 以內允許, 超過拒絕")
    void kyc_withdrawal_eligibility() {
      Long playerId = createPlayer("withdrawuser", "wd@test.com", "13800000031");

      // L0: $400 OK
      assertThat(
              kycVerificationService
                  .checkWithdrawalEligibility(playerId, new BigDecimal("400"))
                  .getSuccess())
          .isTrue();

      // L0: $600 rejected
      assertThat(
              kycVerificationService
                  .checkWithdrawalEligibility(playerId, new BigDecimal("600"))
                  .getSuccess())
          .isFalse();
    }
  }

  // ==================== VIP ====================

  @Nested
  @DisplayName("VIP 等級整合測試")
  class VipIntegrationTest {

    @Test
    @DisplayName("VIP 等級變更 + 變更日誌")
    void vip_level_change_with_log() {
      Long playerId = createPlayer("vipuser", "vip@test.com", "13800000040");

      // BRONZE → SILVER
      var result = playerService.changeVipLevel(playerId, VipLevelEnum.SILVER, "manual upgrade");
      assertThat(result.getSuccess()).isTrue();

      PlayerEntity player = playerDao.selectById(playerId);
      assertThat(player.getVipLevel()).isEqualTo(VipLevelEnum.SILVER.getValue());

      // Verify VIP change log
      Long logCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_vip_change_log WHERE player_id = ?", Long.class, playerId);
      assertThat(logCount).isEqualTo(1L);
    }
  }

  // ==================== PII Encryption ====================

  @Nested
  @DisplayName("PII 加密整合測試")
  class PiiEncryptionIntegrationTest {

    @Test
    @DisplayName("Email/Phone 加密儲存 + 盲索引可查詢")
    void pii_fields_encrypted_in_db() {
      Long playerId = createPlayer("piiuser", "secret@email.com", "13899998888");

      // Verify encrypted storage in DB (raw query, not via TypeHandler)
      String rawEmail =
          jdbcTemplate.queryForObject(
              "SELECT email_encrypted FROM t_player WHERE player_id = ?", String.class, playerId);
      assertThat(rawEmail).isNotNull();
      assertThat(rawEmail).startsWith("v1:"); // AES-256-GCM format
      assertThat(rawEmail).isNotEqualTo("secret@email.com"); // not plaintext

      // Verify blind index is set
      String emailIdx =
          jdbcTemplate.queryForObject(
              "SELECT email_blind_idx FROM t_player WHERE player_id = ?", String.class, playerId);
      assertThat(emailIdx).isNotNull();
      assertThat(emailIdx).hasSize(64); // HMAC-SHA256 hex = 64 chars

      // Verify TypeHandler decrypts on read
      PlayerEntity player = playerDao.selectById(playerId);
      assertThat(player.getEmailEncrypted()).isEqualTo("secret@email.com");
    }
  }

  // ==================== Helpers ====================

  private Long createPlayer(String username, String email, String phone) {
    PlayerEntity player = new PlayerEntity();
    player.setUsername(username);
    player.setPasswordHash(passwordEncryptService.encrypt("TestPass123"));
    player.setEmailEncrypted(email);
    player.setEmailBlindIdx(blindIndexService.computeIndex(email));
    player.setPhoneEncrypted(phone);
    player.setPhoneBlindIdx(blindIndexService.computeIndex(phone));
    player.setStatus(PlayerStatusEnum.ACTIVE.getValue());
    player.setKycLevel(KycLevelEnum.L0.getValue());
    player.setVipLevel(VipLevelEnum.BRONZE.getValue());
    player.setRegistrationIp("127.0.0.1");
    player.setDeleted(false);

    WalletEntity wallet = new WalletEntity();
    wallet.setCurrencyCode("USD");
    wallet.setWalletType(WalletTypeEnum.CASH.getValue());
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);

    playerRegistrationManager.registerPlayer(player, wallet);
    return player.getPlayerId();
  }
}
