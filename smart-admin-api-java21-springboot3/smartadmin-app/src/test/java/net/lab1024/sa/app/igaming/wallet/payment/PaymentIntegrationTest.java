package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.DepositRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.form.WithdrawRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.DepositResponseVO;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
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
 * Payment integration tests — verifies payment operations against real PostgreSQL 16 via
 * Testcontainers.
 *
 * <p>Uses a sliced Spring context that loads wallet + payment beans + MyBatis-Plus infrastructure.
 * Validates: deposit flow, withdrawal flow with balance lock, callback idempotency, and
 * reconciliation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Tag("integration")
@SpringBootTest(
    classes = PaymentIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = PaymentIntegrationTest.SchemaInitializer.class)
@DisplayName("Payment 整合測試 (Testcontainers + PostgreSQL)")
class PaymentIntegrationTest {

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
        "net.lab1024.sa.igaming.wallet.payment.service",
        "net.lab1024.sa.igaming.wallet.payment.manager",
        "net.lab1024.sa.igaming.wallet.payment.psp",
      })
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.wallet.dao",
        "net.lab1024.sa.igaming.wallet.payment.dao",
      },
      annotationClass = Mapper.class)
  static class TestConfig {

    @org.springframework.context.annotation.Bean
    public net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher domainEventPublisher() {
      return org.mockito.Mockito.mock(
          net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher.class);
    }

    @org.springframework.context.annotation.Bean
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
        // V9: payment tables
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V9__payment_tables.sql"));
        // V16: payment reconciliation (adds reconciliation_status column)
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V16__payment_reconciliation.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize payment test schema", e);
      }
    }
  }

  @Autowired private WalletService walletService;
  @Autowired private PaymentService paymentService;
  @Autowired private PaymentOrderDao paymentOrderDao;
  @Autowired private WalletDao walletDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_payment_order, t_wallet_lock, t_wallet_bonus_ext,"
            + " t_wallet_transaction, t_wallet CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Deposit ====================

  @Nested
  @DisplayName("deposit 存款整合測試")
  class DepositIntegrationTest {

    @Test
    @DisplayName("存款成功建立 PROCESSING 訂單")
    void deposit_success_creates_processing_order() {
      Long walletId = createFundedWallet("0");

      DepositRequestForm form = buildDepositForm(walletId, "200.0000", "dep_req_001");
      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isTrue();
      DepositResponseVO vo = result.getData();
      assertThat(vo.getOrderNo()).isNotNull();
      assertThat(vo.getRedirectUrl()).isNotNull();
      assertThat(vo.getStatus()).isEqualTo(PaymentOrderStatusEnum.PROCESSING.getValue());

      // Verify DB
      PaymentOrderEntity order =
          paymentOrderDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PaymentOrderEntity>lambdaQuery()
                  .eq(PaymentOrderEntity::getOrderNo, vo.getOrderNo()));
      assertThat(order).isNotNull();
      assertThat(order.getOrderType()).isEqualTo(PaymentOrderTypeEnum.DEPOSIT.getValue());
      assertThat(order.getAmount()).isEqualByComparingTo("200.0000");
      assertThat(order.getPspTransactionId()).startsWith("mock_dep_");
    }

    @Test
    @DisplayName("冪等存款 → 重複 requestId 返回已有訂單")
    void deposit_idempotency_returns_existing() {
      Long walletId = createFundedWallet("0");

      DepositRequestForm form = buildDepositForm(walletId, "100.0000", "dep_idem_001");

      ResponseDTO<DepositResponseVO> first = paymentService.createDeposit(form);
      assertThat(first.getOk()).isTrue();
      String firstOrderNo = first.getData().getOrderNo();

      ResponseDTO<DepositResponseVO> second = paymentService.createDeposit(form);
      assertThat(second.getOk()).isTrue();
      assertThat(second.getData().getOrderNo()).isEqualTo(firstOrderNo);

      // Only one order in DB
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_payment_order WHERE request_id = ?",
              Long.class,
              "dep_idem_001");
      assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("存款回調成功入賬")
    void deposit_callback_credits_wallet() {
      Long walletId = createFundedWallet("0");

      DepositRequestForm form = buildDepositForm(walletId, "500.0000", "dep_cb_001");
      ResponseDTO<DepositResponseVO> depositResult = paymentService.createDeposit(form);
      assertThat(depositResult.getOk()).isTrue();
      String orderNo = depositResult.getData().getOrderNo();

      // Simulate PSP callback (SUCCESS)
      ResponseDTO<String> callbackResult =
          paymentService.processDepositCallback(orderNo, "psp_dep_txn_001", "{}");
      assertThat(callbackResult.getOk()).isTrue();

      // Verify wallet balance credited
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("500.0000");

      // Verify order status = SUCCESS
      PaymentOrderEntity order =
          paymentOrderDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PaymentOrderEntity>lambdaQuery()
                  .eq(PaymentOrderEntity::getOrderNo, orderNo));
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getValue());
    }

    @Test
    @DisplayName("存款回調冪等 → 重複回調不重複入賬")
    void deposit_callback_idempotency() {
      Long walletId = createFundedWallet("0");

      DepositRequestForm form = buildDepositForm(walletId, "300.0000", "dep_cb_idem_001");
      ResponseDTO<DepositResponseVO> depositResult = paymentService.createDeposit(form);
      String orderNo = depositResult.getData().getOrderNo();

      // First callback
      paymentService.processDepositCallback(orderNo, "psp_dep_txn_002", "{}");

      // Second callback (duplicate)
      ResponseDTO<String> duplicateResult =
          paymentService.processDepositCallback(orderNo, "psp_dep_txn_002", "{}");
      assertThat(duplicateResult.getOk()).isTrue();
      assertThat(duplicateResult.getData()).contains("Already processed");

      // Balance is 300, not 600
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("300.0000");
    }
  }

  // ==================== Withdrawal ====================

  @Nested
  @DisplayName("withdrawal 提款整合測試")
  class WithdrawalIntegrationTest {

    @Test
    @DisplayName("提款成功凍結餘額")
    void withdrawal_success_locks_funds() {
      Long walletId = createFundedWallet("1000.0000");

      WithdrawRequestForm form = buildWithdrawForm(walletId, "200.0000", "wd_req_001");
      ResponseDTO<PaymentOrderVO> result = paymentService.createWithdrawal(form);

      assertThat(result.getOk()).isTrue();

      // Verify funds are locked
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getLockedAmount()).isEqualByComparingTo("200.0000");
      assertThat(wallet.getBalance()).isEqualByComparingTo("1000.0000"); // balance unchanged

      // Verify lock record exists
      Long lockCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet_lock WHERE wallet_id = ?", Long.class, walletId);
      assertThat(lockCount).isEqualTo(1L);

      // Verify order in DB
      PaymentOrderEntity order =
          paymentOrderDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PaymentOrderEntity>lambdaQuery()
                  .eq(PaymentOrderEntity::getRequestId, "wd_req_001"));
      assertThat(order.getOrderType()).isEqualTo(PaymentOrderTypeEnum.WITHDRAWAL.getValue());
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.PROCESSING.getValue());
    }

    @Test
    @DisplayName("提款回調成功完成 → 扣款 + 解鎖")
    void withdrawal_callback_success_completes() {
      Long walletId = createFundedWallet("1000.0000");

      WithdrawRequestForm form = buildWithdrawForm(walletId, "300.0000", "wd_cb_succ_001");
      ResponseDTO<PaymentOrderVO> wdResult = paymentService.createWithdrawal(form);
      assertThat(wdResult.getOk()).isTrue();
      String orderNo = wdResult.getData().getOrderNo();

      // Simulate PSP callback (SUCCESS)
      ResponseDTO<String> callbackResult =
          paymentService.processWithdrawalCallback(orderNo, "psp_wd_txn_001", "{}", true);
      assertThat(callbackResult.getOk()).isTrue();

      // Verify balance deducted and lock released
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("700.0000");
      assertThat(wallet.getLockedAmount()).isEqualByComparingTo("0");

      // Verify order status = SUCCESS
      PaymentOrderEntity order =
          paymentOrderDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PaymentOrderEntity>lambdaQuery()
                  .eq(PaymentOrderEntity::getOrderNo, orderNo));
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getValue());

      // Verify lock record deleted
      Long lockCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_wallet_lock WHERE wallet_id = ?", Long.class, walletId);
      assertThat(lockCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("提款失敗回調 → 解凍餘額")
    void withdrawal_callback_failure_refunds() {
      Long walletId = createFundedWallet("1000.0000");

      WithdrawRequestForm form = buildWithdrawForm(walletId, "400.0000", "wd_cb_fail_001");
      ResponseDTO<PaymentOrderVO> wdResult = paymentService.createWithdrawal(form);
      assertThat(wdResult.getOk()).isTrue();
      String orderNo = wdResult.getData().getOrderNo();

      // Verify locked before callback
      WalletEntity locked = walletDao.selectById(walletId);
      assertThat(locked.getLockedAmount()).isEqualByComparingTo("400.0000");

      // Simulate PSP callback (FAILURE)
      ResponseDTO<String> callbackResult =
          paymentService.processWithdrawalCallback(orderNo, "psp_wd_txn_002", "{}", false);
      assertThat(callbackResult.getOk()).isTrue();

      // Verify balance unchanged and lock released
      WalletEntity wallet = walletDao.selectById(walletId);
      assertThat(wallet.getBalance()).isEqualByComparingTo("1000.0000");
      assertThat(wallet.getLockedAmount()).isEqualByComparingTo("0");

      // Verify order status = FAILED
      PaymentOrderEntity order =
          paymentOrderDao.selectOne(
              com.baomidou.mybatisplus.core.toolkit.Wrappers.<PaymentOrderEntity>lambdaQuery()
                  .eq(PaymentOrderEntity::getOrderNo, orderNo));
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.FAILED.getValue());
    }
  }

  // ==================== Helper Methods ====================

  private Long createFundedWallet(String initialBalance) {
    WalletCreateForm form = new WalletCreateForm();
    form.setPlayerId(PLAYER_ID);
    form.setWalletType(WalletTypeEnum.CASH.getValue());
    form.setCurrencyCode("USD");
    ResponseDTO<WalletVO> result = walletService.createWallet(form);
    assertThat(result.getSuccess()).isTrue();
    Long walletId = result.getData().getWalletId();

    if (new BigDecimal(initialBalance).compareTo(BigDecimal.ZERO) > 0) {
      WalletCreditForm creditForm = new WalletCreditForm();
      creditForm.setWalletId(walletId);
      creditForm.setAmount(new BigDecimal(initialBalance));
      creditForm.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      creditForm.setRequestId("init_fund_" + walletId);
      walletService.credit(creditForm);
    }

    return walletId;
  }

  private DepositRequestForm buildDepositForm(Long walletId, String amount, String requestId) {
    DepositRequestForm form = new DepositRequestForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setCurrencyCode("USD");
    form.setPspCode("mock");
    form.setRequestId(requestId);
    return form;
  }

  private WithdrawRequestForm buildWithdrawForm(Long walletId, String amount, String requestId) {
    WithdrawRequestForm form = new WithdrawRequestForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setCurrencyCode("USD");
    form.setPspCode("mock");
    form.setRequestId(requestId);
    return form;
  }
}
