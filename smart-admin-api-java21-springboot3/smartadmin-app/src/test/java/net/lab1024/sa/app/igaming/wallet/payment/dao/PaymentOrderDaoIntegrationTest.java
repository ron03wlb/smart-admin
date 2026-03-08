package net.lab1024.sa.app.igaming.wallet.payment.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.PaymentOrderQueryForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
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
 * PaymentOrderDao integration tests — verifies queryPage (pagination + status/playerId filter)
 * against real PostgreSQL 16.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = PaymentOrderDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = PaymentOrderDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("PaymentOrderDao 整合測試 (Testcontainers + PostgreSQL)")
class PaymentOrderDaoIntegrationTest {

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
  @MapperScan(value = "net.lab1024.sa.igaming.wallet.payment.dao", annotationClass = Mapper.class)
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
        // V8 for wallet tables (FK dependency), V9 for payment tables
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V9__payment_tables.sql"));
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V16__payment_reconciliation.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize payment test schema", e);
      }
    }
  }

  @Autowired private PaymentOrderDao paymentOrderDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute("TRUNCATE t_payment_order CASCADE");
    jdbcTemplate.execute("TRUNCATE t_wallet CASCADE");
    // Pre-populate wallets for FK satisfaction (walletId 1, 2, 3)
    for (long wid = 1; wid <= 3; wid++) {
      jdbcTemplate.update(
          "INSERT INTO t_wallet (wallet_id, player_id, wallet_type, currency_code, balance,"
              + " locked_amount, version, deleted, tenant_id, create_time, update_time)"
              + " VALUES (?, ?, 1, 'USD', 0, 0, 0, false, ?, NOW(), NOW())",
          wid,
          99L + wid,
          TEST_TENANT_ID);
    }
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== queryPage ====================

  @Nested
  @DisplayName("queryPage 支付訂單分頁查詢")
  class QueryPageTest {

    @Test
    @DisplayName("無過濾條件 → 返回所有訂單")
    void shouldReturnAllOrders() {
      insertPaymentOrder(
          100L, 1L, 1, PaymentOrderStatusEnum.SUCCESS.getValue(), "1000.0000", "USD", "PSP_A");
      insertPaymentOrder(
          101L, 2L, 2, PaymentOrderStatusEnum.PENDING.getValue(), "500.0000", "USD", "PSP_B");

      PaymentOrderQueryForm query = new PaymentOrderQueryForm();
      Page<PaymentOrderVO> page = new Page<>(1, 10);
      List<PaymentOrderVO> result = paymentOrderDao.queryPage(page, query);

      assertThat(result).hasSize(2);
      assertThat(page.getTotal()).isEqualTo(2L);
    }

    @Test
    @DisplayName("按 status 過濾")
    void shouldFilterByStatus() {
      insertPaymentOrder(
          100L, 1L, 1, PaymentOrderStatusEnum.SUCCESS.getValue(), "1000.0000", "USD", "PSP_A");
      insertPaymentOrder(
          101L, 2L, 1, PaymentOrderStatusEnum.PENDING.getValue(), "500.0000", "USD", "PSP_A");
      insertPaymentOrder(
          102L, 3L, 1, PaymentOrderStatusEnum.FAILED.getValue(), "300.0000", "USD", "PSP_B");

      PaymentOrderQueryForm query = new PaymentOrderQueryForm();
      query.setStatus(PaymentOrderStatusEnum.SUCCESS.getValue());
      Page<PaymentOrderVO> page = new Page<>(1, 10);
      List<PaymentOrderVO> result = paymentOrderDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getValue());
    }

    @Test
    @DisplayName("按 playerId 過濾")
    void shouldFilterByPlayerId() {
      insertPaymentOrder(
          100L, 1L, 1, PaymentOrderStatusEnum.SUCCESS.getValue(), "1000.0000", "USD", "PSP_A");
      insertPaymentOrder(
          100L, 1L, 2, PaymentOrderStatusEnum.SUCCESS.getValue(), "500.0000", "USD", "PSP_A");
      insertPaymentOrder(
          101L, 2L, 1, PaymentOrderStatusEnum.PENDING.getValue(), "300.0000", "USD", "PSP_B");

      PaymentOrderQueryForm query = new PaymentOrderQueryForm();
      query.setPlayerId(100L);
      Page<PaymentOrderVO> page = new Page<>(1, 10);
      List<PaymentOrderVO> result = paymentOrderDao.queryPage(page, query);

      assertThat(result).hasSize(2);
      assertThat(result).allSatisfy(vo -> assertThat(vo.getPlayerId()).isEqualTo(100L));
    }
  }

  // ==================== Helper Methods ====================

  private static int orderCounter = 0;

  private void insertPaymentOrder(
      Long playerId,
      Long walletId,
      Integer orderType,
      Integer status,
      String amount,
      String currencyCode,
      String pspCode) {
    orderCounter++;
    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setOrderNo("ORD-" + System.currentTimeMillis() + "-" + orderCounter);
    entity.setPlayerId(playerId);
    entity.setWalletId(walletId);
    entity.setOrderType(orderType);
    entity.setStatus(status);
    entity.setAmount(new BigDecimal(amount));
    entity.setCurrencyCode(currencyCode);
    entity.setPspCode(pspCode);
    entity.setRequestId("req-" + orderCounter);
    entity.setReconciliationStatus(0);
    entity.setVersion(0);
    paymentOrderDao.insert(entity);
  }
}
