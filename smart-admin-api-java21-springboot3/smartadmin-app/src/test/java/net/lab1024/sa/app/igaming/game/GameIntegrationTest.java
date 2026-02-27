package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameWeightConfigEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameCallbackService;
import net.lab1024.sa.igaming.game.service.GameLobbyService;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
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
 * Game integration tests — verifies game operations against real PostgreSQL 16 via Testcontainers.
 *
 * <p>Tests the full callback flow: debit (bet) → credit (win) → rollback (cancel), idempotency,
 * weighted turnover, and game search.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Tag("integration")
@SpringBootTest(
    classes = GameIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = GameIntegrationTest.SchemaInitializer.class)
@DisplayName("Game 整合測試 (Testcontainers + PostgreSQL)")
class GameIntegrationTest {

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
        "net.lab1024.sa.igaming.game.service",
        "net.lab1024.sa.igaming.game.manager",
        "net.lab1024.sa.igaming.game.adapter",
        "net.lab1024.sa.igaming.wallet.service",
        "net.lab1024.sa.igaming.wallet.manager",
      })
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.game.dao",
        "net.lab1024.sa.igaming.wallet.dao",
      },
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
  private static final Long PLAYER_ID = 100L;

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
        // V8: wallet tables
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
        // V11: game tables
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V11__game_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize game test schema", e);
      }
    }
  }

  @Autowired private GameCallbackService gameCallbackService;
  @Autowired private GameLobbyService gameLobbyService;
  @Autowired private GameRoundDao gameRoundDao;
  @Autowired private GameWeightConfigDao gameWeightConfigDao;
  @Autowired private WalletDao walletDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_game_weight_config, t_reconciliation, t_game_round,"
            + " t_game, t_game_provider CASCADE");
    jdbcTemplate.execute(
        "TRUNCATE t_wallet_lock, t_wallet_bonus_ext," + " t_wallet_transaction, t_wallet CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Debit (Bet) ====================

  @Nested
  @DisplayName("debit 下注整合測試")
  class DebitIntegrationTest {

    @Test
    @DisplayName("下注成功 — 建立 round + 扣減錢包")
    void debit_success_creates_round_and_debits_wallet() {
      Long walletId = createFundedWallet("100.0000");
      insertGameProvider();
      insertGame();

      CallbackDebitForm form = buildDebitForm("tx-it-001", "10.0000");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTransactionId()).isEqualTo("tx-it-001");
      assertThat(result.getData().getBalance()).isEqualByComparingTo("90.0000");

      // Verify round in DB
      GameRoundEntity round = gameRoundDao.selectByTransactionId("tx-it-001");
      assertThat(round).isNotNull();
      assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.OPEN.getValue());
      assertThat(round.getBetAmount()).isEqualByComparingTo("10.0000");

      // Verify wallet balance
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("90.0000");
    }

    @Test
    @DisplayName("冪等重複下注 — 返回已有結果")
    void debit_idempotent_duplicate() {
      createFundedWallet("100.0000");
      insertGameProvider();
      insertGame();

      CallbackDebitForm form = buildDebitForm("tx-it-idem", "10.0000");
      ResponseDTO<CallbackResponseVO> first =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);
      assertThat(first.getOk()).isTrue();

      ResponseDTO<CallbackResponseVO> second =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);
      assertThat(second.getOk()).isTrue();
      assertThat(second.getData().getTransactionId()).isEqualTo("tx-it-idem");
    }

    @Test
    @DisplayName("餘額不足 — 拒絕")
    void debit_insufficient_balance() {
      createFundedWallet("5.0000");
      insertGameProvider();
      insertGame();

      CallbackDebitForm form = buildDebitForm("tx-it-insuf", "10.0000");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Credit (Win) ====================

  @Nested
  @DisplayName("credit 結算整合測試")
  class CreditIntegrationTest {

    @Test
    @DisplayName("結算成功 — 更新 round + 入款")
    void credit_success_settles_round_and_credits_wallet() {
      Long walletId = createFundedWallet("100.0000");
      insertGameProvider();
      insertGame();

      // First: debit
      CallbackDebitForm debitForm = buildDebitForm("tx-it-credit-001", "10.0000");
      gameCallbackService.processDebit(debitForm, TEST_TENANT_ID);

      // Then: credit
      CallbackCreditForm creditForm = buildCreditForm("tx-it-credit-002", "round-001", "25.0000");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processCredit(creditForm, TEST_TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());

      // Verify wallet balance: 100 - 10 + 25 = 115
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("115.0000");
    }
  }

  // ==================== Rollback (Cancel) ====================

  @Nested
  @DisplayName("rollback 取消整合測試")
  class RollbackIntegrationTest {

    @Test
    @DisplayName("取消成功 — 退款 + CANCELLED")
    void rollback_success_refunds_and_cancels() {
      Long walletId = createFundedWallet("100.0000");
      insertGameProvider();
      insertGame();

      // First: debit
      CallbackDebitForm debitForm = buildDebitForm("tx-it-rb-001", "10.0000");
      gameCallbackService.processDebit(debitForm, TEST_TENANT_ID);

      // Then: rollback
      CallbackRollbackForm rollbackForm = buildRollbackForm("tx-it-rb-001");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processRollback(rollbackForm, TEST_TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.CANCELLED.getValue());

      // Verify wallet restored: 100 - 10 + 10 = 100
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("100.0000");

      // Verify round status
      GameRoundEntity round = gameRoundDao.selectByTransactionId("tx-it-rb-001");
      assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.CANCELLED.getValue());
    }
  }

  // ==================== Search ====================

  @Nested
  @DisplayName("search 遊戲搜索整合測試")
  class SearchIntegrationTest {

    @Test
    @DisplayName("分頁搜索遊戲")
    void search_games_paginated() {
      insertGameProvider();
      insertGame();

      GameQueryForm form = new GameQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      var result = gameLobbyService.searchGames(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).isNotEmpty();
    }
  }

  // ==================== Weighted Turnover ====================

  @Nested
  @DisplayName("weightedTurnover 有效投注額整合測試")
  class WeightedTurnoverIntegrationTest {

    @Test
    @DisplayName("使用配置權重計算有效投注額 — 透過下注回呼驗證")
    void weighted_turnover_with_config() {
      createFundedWallet("200.0000");
      insertGameProvider();
      insertGame();
      insertWeightConfig(1, new BigDecimal("0.5000"));

      CallbackDebitForm form = buildDebitForm("tx-it-wt-001", "100.0000");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);

      assertThat(result.getOk()).isTrue();
      GameRoundEntity round = gameRoundDao.selectByTransactionId("tx-it-wt-001");
      assertThat(round.getWeightedTurnover()).isEqualByComparingTo("50.0000");
    }

    @Test
    @DisplayName("無配置使用預設權重 1.0 — 透過下注回呼驗證")
    void weighted_turnover_default_weight() {
      createFundedWallet("200.0000");
      insertGameProvider();
      insertGame();

      CallbackDebitForm form = buildDebitForm("tx-it-wt-002", "100.0000");
      ResponseDTO<CallbackResponseVO> result =
          gameCallbackService.processDebit(form, TEST_TENANT_ID);

      assertThat(result.getOk()).isTrue();
      GameRoundEntity round = gameRoundDao.selectByTransactionId("tx-it-wt-002");
      assertThat(round.getWeightedTurnover()).isEqualByComparingTo("100.0000");
    }
  }

  // ==================== Helpers ====================

  private Long createFundedWallet(String balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setPlayerId(PLAYER_ID);
    wallet.setTenantId(TEST_TENANT_ID);
    wallet.setCurrencyCode("USD");
    wallet.setWalletType(WalletTypeEnum.CASH.getValue());
    wallet.setBalance(new BigDecimal(balance));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setVersion(0);
    wallet.setDeleted(false);
    walletDao.insert(wallet);
    return wallet.getWalletId();
  }

  private void insertGameProvider() {
    jdbcTemplate.execute(
        "INSERT INTO t_game_provider (tenant_id, provider_code, provider_name, api_url,"
            + " encrypted_api_key, enabled, health_status, deleted, version, create_time,"
            + " update_time) VALUES (1, 'mock', 'Mock Provider', 'https://mock.api.com',"
            + " 'test-key', true, 1, false, 0, NOW(), NOW())"
            + " ON CONFLICT DO NOTHING");
  }

  private void insertGame() {
    jdbcTemplate.execute(
        "INSERT INTO t_game (provider_id, tenant_id, game_code, game_name, category,"
            + " thumbnail_url, play_count, enabled, deleted, version, create_time, update_time)"
            + " VALUES ((SELECT provider_id FROM t_game_provider WHERE provider_code='mock'"
            + " LIMIT 1), 1, 'slot-001', 'Test Slot', 1, 'https://img.test/slot.png', 0, true,"
            + " false, 0, NOW(), NOW())"
            + " ON CONFLICT DO NOTHING");
  }

  private void insertWeightConfig(int category, BigDecimal weight) {
    GameWeightConfigEntity config = new GameWeightConfigEntity();
    config.setTenantId(TEST_TENANT_ID);
    config.setGameCategory(category);
    config.setWeight(weight);
    config.setDeleted(false);
    gameWeightConfigDao.insert(config);
  }

  private CallbackDebitForm buildDebitForm(String transactionId, String amount) {
    CallbackDebitForm form = new CallbackDebitForm();
    form.setProviderCode("mock");
    form.setPlayerId(PLAYER_ID);
    form.setTransactionId(transactionId);
    form.setRoundId("round-001");
    form.setGameCode("slot-001");
    form.setAmount(new BigDecimal(amount));
    return form;
  }

  private CallbackCreditForm buildCreditForm(String transactionId, String roundId, String amount) {
    CallbackCreditForm form = new CallbackCreditForm();
    form.setProviderCode("mock");
    form.setPlayerId(PLAYER_ID);
    form.setTransactionId(transactionId);
    form.setRoundId(roundId);
    form.setPayoutAmount(new BigDecimal(amount));
    return form;
  }

  private CallbackRollbackForm buildRollbackForm(String originalTransactionId) {
    CallbackRollbackForm form = new CallbackRollbackForm();
    form.setProviderCode("mock");
    form.setPlayerId(PLAYER_ID);
    form.setOriginalTransactionId(originalTransactionId);
    return form;
  }
}
