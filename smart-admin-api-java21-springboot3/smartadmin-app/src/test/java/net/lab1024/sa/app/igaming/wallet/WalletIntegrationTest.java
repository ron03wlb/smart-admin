package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletLockForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletLockVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.service.WalletService;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Wallet integration tests — verifies wallet operations against real PostgreSQL 16 via
 * Testcontainers.
 *
 * <p>Uses a sliced Spring context ({@link TestConfig}) that only loads wallet beans + MyBatis-Plus
 * infrastructure, avoiding core SmartAdmin beans (ConfigService, EmployeeService, etc.) that
 * require 45+ core tables not present in the test database.
 *
 * <p>Wallet tables are created via {@link SchemaInitializer} (V8 DDL) before the Spring context
 * loads. Only a PostgreSQL container is needed — Kafka/Redis auto-configs are excluded.
 *
 * <p>Validates: idempotency L2 (UNIQUE constraint), optimistic lock (@Version), TOCTOU defense
 * (DuplicateKeyException), balance consistency, and lock/unlock full flow.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = WalletIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = WalletIntegrationTest.SchemaInitializer.class)
@DisplayName("Wallet 整合測試 (Testcontainers + PostgreSQL)")
class WalletIntegrationTest {

  /**
   * Sliced Spring context — loads ONLY wallet + MyBatis-Plus beans, excluding core SmartAdmin
   * infrastructure (ConfigService, EmployeeService, etc.) that require missing core tables.
   *
   * <p>Uses explicit {@code @ImportAutoConfiguration} instead of {@code @EnableAutoConfiguration}
   * to avoid loading DataSourceConfig which conflicts with MybatisPlusAutoConfiguration on the
   * {@code sqlSessionFactory} bean name.
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
    net.lab1024.sa.app.igaming.config.IntegrationTestMockBeans.class,
  })
  @ComponentScan(
      basePackages = {
        "net.lab1024.sa.igaming.wallet.service",
        "net.lab1024.sa.igaming.wallet.manager",
      })
  @MapperScan(value = "net.lab1024.sa.igaming.wallet.dao", annotationClass = Mapper.class)
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
    // PostgreSQL
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    // MyBatis-Plus configuration (explicit — not relying on profile config)
    registry.add("mybatis-plus.mapper-locations", () -> "classpath*:/mapper/**/*.xml");
    registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    registry.add("tenant.enabled", () -> false);
  }

  /**
   * Creates wallet tables (V8 DDL) directly in PostgreSQL BEFORE Spring context loads beans. Runs
   * after Testcontainers start but before bean creation, ensuring wallet tables exist when DAOs
   * initialize.
   */
  static class SchemaInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      try (Connection conn =
          DriverManager.getConnection(
              POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
        // Create smartadmin_app role required by V8 RLS policies and GRANT statements
        try {
          conn.createStatement().execute("CREATE ROLE smartadmin_app LOGIN");
        } catch (SQLException ignored) {
          // Role already exists
        }
        // Execute V8 wallet DDL (4 tables + indexes + RLS + grants)
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize wallet test schema", e);
      }
    }
  }

  @Autowired private WalletService walletService;
  @Autowired private WalletManager walletManager;
  @Autowired private WalletDao walletDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_wallet_lock, t_wallet_bonus_ext, t_wallet_transaction, t_wallet CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== createWallet ====================

  @Nested
  @DisplayName("createWallet 建立錢包整合測試")
  class CreateWalletIntegrationTest {

    @Test
    @DisplayName("建立錢包 → DB 持久化驗證 (tenantId, version, balance)")
    void shouldCreateAndPersistWallet() {
      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(PLAYER_ID);
      form.setWalletType(WalletTypeEnum.CASH.getValue());
      form.setCurrencyCode("USD");

      ResponseDTO<WalletVO> result = walletService.createWallet(form);

      assertThat(result.getSuccess()).isTrue();
      WalletVO vo = result.getData();
      assertThat(vo.getWalletId()).isNotNull();
      assertThat(vo.getBalance()).isEqualByComparingTo("0");
      assertThat(vo.getAvailableBalance()).isEqualByComparingTo("0");

      // Verify DB persistence
      WalletEntity persisted = walletDao.selectById(vo.getWalletId());
      assertThat(persisted).isNotNull();
      assertThat(persisted.getPlayerId()).isEqualTo(PLAYER_ID);
      assertThat(persisted.getTenantId()).isEqualTo(TEST_TENANT_ID);
      assertThat(persisted.getVersion()).isEqualTo(0);
      assertThat(persisted.getDeleted()).isFalse();
      assertThat(persisted.getCurrencyCode()).isEqualTo("USD");
      assertThat(persisted.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("TOCTOU 防禦 → 相同 (playerId, walletType) 第二次建立返回錯誤")
    void shouldRejectDuplicateWalletViaToctou() {
      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(PLAYER_ID);
      form.setWalletType(WalletTypeEnum.CASH.getValue());
      form.setCurrencyCode("USD");

      // First create succeeds
      ResponseDTO<WalletVO> first = walletService.createWallet(form);
      assertThat(first.getSuccess()).isTrue();

      // Second create with same playerId + walletType fails
      ResponseDTO<WalletVO> second = walletService.createWallet(form);
      assertThat(second.getSuccess()).isFalse();

      // Only one wallet in DB
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet WHERE player_id = ? AND wallet_type = ?",
              Long.class,
              PLAYER_ID,
              WalletTypeEnum.CASH.getValue());
      assertThat(count).isEqualTo(1L);
    }
  }

  // ==================== credit ====================

  @Nested
  @DisplayName("credit 加款整合測試")
  class CreditIntegrationTest {

    @Test
    @DisplayName("加款成功 → balance 更新 + transaction 記錄 + version 遞增")
    void shouldCreditAndUpdateBalance() {
      Long walletId = createTestWallet();

      // Credit 1000
      WalletCreditForm form = buildCreditForm(walletId, "1000.0000", "credit-req-001");
      ResponseDTO<WalletTransactionVO> result = walletService.credit(form);

      assertThat(result.getSuccess()).isTrue();
      WalletTransactionVO txn = result.getData();
      assertThat(txn.getBalanceBefore()).isEqualByComparingTo("0");
      assertThat(txn.getBalanceAfter()).isEqualByComparingTo("1000.0000");
      assertThat(txn.getAmount()).isEqualByComparingTo("1000.0000");

      // Verify wallet balance in DB
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("1000.0000");
      assertThat(wallet.getVersion()).isEqualTo(1);

      // Verify transaction record in DB
      Long txnCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet_transaction WHERE wallet_id = ?",
              Long.class,
              walletId);
      assertThat(txnCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("冪等性 L2 → 相同 requestId 加款兩次，餘額只增加一次")
    void shouldBeIdempotentOnDuplicateRequestId() {
      Long walletId = createTestWallet();

      WalletCreditForm form = buildCreditForm(walletId, "500.0000", "idempotent-req-001");

      // First credit
      ResponseDTO<WalletTransactionVO> first = walletService.credit(form);
      assertThat(first.getSuccess()).isTrue();

      // Second credit with same requestId
      ResponseDTO<WalletTransactionVO> second = walletService.credit(form);
      assertThat(second.getSuccess()).isTrue();

      // Balance should be 500, not 1000
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("500.0000");

      // Only one transaction record
      Long txnCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet_transaction WHERE wallet_id = ?",
              Long.class,
              walletId);
      assertThat(txnCount).isEqualTo(1L);
    }
  }

  // ==================== debit ====================

  @Nested
  @DisplayName("debit 扣款整合測試")
  class DebitIntegrationTest {

    @Test
    @DisplayName("扣款成功 + 餘額不足拒絕")
    void shouldDebitAndEnforceBalanceConstraint() {
      Long walletId = createTestWallet();

      // Fund the wallet with 1000
      walletService.credit(buildCreditForm(walletId, "1000.0000", "fund-req-001"));

      // Debit 300 — should succeed
      WalletDebitForm debitForm = buildDebitForm(walletId, "300.0000", "debit-req-001");
      ResponseDTO<WalletTransactionVO> result = walletService.debit(debitForm);

      assertThat(result.getSuccess()).isTrue();
      WalletTransactionVO txn = result.getData();
      assertThat(txn.getBalanceBefore()).isEqualByComparingTo("1000.0000");
      assertThat(txn.getBalanceAfter()).isEqualByComparingTo("700.0000");
      assertThat(txn.getAmount()).isEqualByComparingTo("-300.0000");

      // Verify DB
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("700.0000");

      // Debit 800 — should fail (only 700 available)
      WalletDebitForm overDebit = buildDebitForm(walletId, "800.0000", "debit-req-002");
      ResponseDTO<WalletTransactionVO> failResult = walletService.debit(overDebit);
      assertThat(failResult.getSuccess()).isFalse();

      // Balance unchanged
      WalletEntity unchanged = walletDao.selectById(walletId);
      assertThat(unchanged.getBalance()).isEqualByComparingTo("700.0000");
    }
  }

  // ==================== optimistic lock ====================

  @Nested
  @DisplayName("optimistic lock 樂觀鎖整合測試")
  class OptimisticLockIntegrationTest {

    @Test
    @DisplayName("version 衝突 → OptimisticLockingFailureException")
    void shouldThrowOnOptimisticLockConflict() {
      Long walletId = createTestWallet();

      // Fund the wallet (version becomes 1)
      walletService.credit(buildCreditForm(walletId, "1000.0000", "fund-lock-001"));

      // Read wallet entity (captures version=1)
      WalletEntity staleWallet = walletDao.selectById(walletId);

      // Simulate concurrent modification: bump version directly in DB (now version=2)
      jdbcTemplate.update(
          "UPDATE t_wallet SET version = version + 1 WHERE wallet_id = ?", walletId);

      // Prepare a stale update (entity still has version=1, DB has version=2)
      staleWallet.setBalance(staleWallet.getBalance().add(new BigDecimal("200.0000")));

      WalletTransactionEntity txn = new WalletTransactionEntity();
      txn.setWalletId(walletId);
      txn.setPlayerId(PLAYER_ID);
      txn.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      txn.setAmount(new BigDecimal("200.0000"));
      txn.setBalanceBefore(new BigDecimal("1000.0000"));
      txn.setBalanceAfter(new BigDecimal("1200.0000"));
      txn.setRequestId("conflict-req-001");

      // Manager should throw due to version mismatch (rows=0)
      assertThatThrownBy(() -> walletManager.credit(staleWallet, txn))
          .isInstanceOf(OptimisticLockingFailureException.class);

      // Balance unchanged (transaction rolled back)
      BigDecimal balance =
          jdbcTemplate.queryForObject(
              "SELECT balance FROM t_wallet WHERE wallet_id = ?", BigDecimal.class, walletId);
      assertThat(balance).isEqualByComparingTo("1000.0000");
    }
  }

  // ==================== lock/unlock ====================

  @Nested
  @DisplayName("lock/unlock 鎖定解鎖整合測試")
  class LockUnlockIntegrationTest {

    @Test
    @DisplayName("鎖定 + 解鎖全流程 → lockedAmount 一致性")
    void shouldLockAndUnlockFunds() {
      Long walletId = createTestWallet();

      // Fund the wallet
      walletService.credit(buildCreditForm(walletId, "1000.0000", "fund-lock-002"));

      // Lock 500
      WalletLockForm lockForm = new WalletLockForm();
      lockForm.setWalletId(walletId);
      lockForm.setLockAmount(new BigDecimal("500.0000"));
      lockForm.setLockReason(LockReasonEnum.BET_PENDING.getValue());
      lockForm.setReferenceId("bet-001");

      ResponseDTO<WalletLockVO> lockResult = walletService.lockFunds(lockForm);
      assertThat(lockResult.getSuccess()).isTrue();
      Long lockId = lockResult.getData().getLockId();

      // Verify lockedAmount in DB
      WalletEntity locked = walletDao.selectById(walletId);
      assertThat(locked.getLockedAmount()).isEqualByComparingTo("500.0000");
      assertThat(locked.getBalance()).isEqualByComparingTo("1000.0000");

      // Unlock
      ResponseDTO<String> unlockResult = walletService.unlockFunds(lockId);
      assertThat(unlockResult.getSuccess()).isTrue();

      // Verify lockedAmount back to 0
      WalletEntity unlocked = walletDao.selectById(walletId);
      assertThat(unlocked.getLockedAmount()).isEqualByComparingTo("0");

      // Lock record deleted
      Long lockCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet_lock WHERE wallet_id = ?", Long.class, walletId);
      assertThat(lockCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("鎖定金額超過可用餘額 → 拒絕")
    void shouldEnforceLockedLeBalanceConstraint() {
      Long walletId = createTestWallet();

      // Fund with 500
      walletService.credit(buildCreditForm(walletId, "500.0000", "fund-lock-003"));

      // Try to lock 600 — exceeds balance
      WalletLockForm lockForm = new WalletLockForm();
      lockForm.setWalletId(walletId);
      lockForm.setLockAmount(new BigDecimal("600.0000"));
      lockForm.setLockReason(LockReasonEnum.BET_PENDING.getValue());
      lockForm.setReferenceId("bet-002");

      ResponseDTO<WalletLockVO> result = walletService.lockFunds(lockForm);
      assertThat(result.getSuccess()).isFalse();

      // lockedAmount unchanged
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getLockedAmount()).isEqualByComparingTo("0");
    }
  }

  // ==================== Helper Methods ====================

  private Long createTestWallet() {
    WalletCreateForm form = new WalletCreateForm();
    form.setPlayerId(PLAYER_ID);
    form.setWalletType(WalletTypeEnum.CASH.getValue());
    form.setCurrencyCode("USD");
    ResponseDTO<WalletVO> result = walletService.createWallet(form);
    assertThat(result.getSuccess()).isTrue();
    return result.getData().getWalletId();
  }

  private WalletCreditForm buildCreditForm(Long walletId, String amount, String requestId) {
    WalletCreditForm form = new WalletCreditForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
    form.setRequestId(requestId);
    return form;
  }

  private WalletDebitForm buildDebitForm(Long walletId, String amount, String requestId) {
    WalletDebitForm form = new WalletDebitForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
    form.setRequestId(requestId);
    return form;
  }
}
