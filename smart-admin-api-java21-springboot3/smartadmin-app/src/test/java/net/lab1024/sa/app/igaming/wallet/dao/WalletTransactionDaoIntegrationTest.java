package net.lab1024.sa.app.igaming.wallet.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletTransactionQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
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
 * WalletTransactionDao integration tests — verifies queryPage (time range filter) and
 * sumAmountByType (COALESCE aggregation) against real PostgreSQL 16.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = WalletTransactionDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = WalletTransactionDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("WalletTransactionDao 整合測試 (Testcontainers + PostgreSQL)")
class WalletTransactionDaoIntegrationTest {

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
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V8__wallet_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize wallet test schema", e);
      }
    }
  }

  @Autowired private WalletDao walletDao;
  @Autowired private WalletTransactionDao walletTransactionDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long testWalletId;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_wallet_lock, t_wallet_bonus_ext, t_wallet_transaction, t_wallet CASCADE");
    testWalletId = createTestWallet();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== queryPage ====================

  @Nested
  @DisplayName("queryPage 交易分頁查詢")
  class QueryPageTest {

    @Test
    @DisplayName("按 walletId 查詢所有交易")
    void shouldReturnTransactionsByWalletId() {
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "1000.0000", "req-001");
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.BET.getValue(), "-100.0000", "req-002");

      WalletTransactionQueryForm query = new WalletTransactionQueryForm();
      query.setWalletId(testWalletId);
      Page<WalletTransactionVO> page = new Page<>(1, 10);
      List<WalletTransactionVO> result = walletTransactionDao.queryPage(page, query);

      assertThat(result).hasSize(2);
      assertThat(page.getTotal()).isEqualTo(2L);
    }

    @Test
    @DisplayName("按 transactionType 過濾")
    void shouldFilterByTransactionType() {
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "1000.0000", "req-001");
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.BET.getValue(), "-100.0000", "req-002");

      WalletTransactionQueryForm query = new WalletTransactionQueryForm();
      query.setWalletId(testWalletId);
      query.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      Page<WalletTransactionVO> page = new Page<>(1, 10);
      List<WalletTransactionVO> result = walletTransactionDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTransactionType())
          .isEqualTo(TransactionTypeEnum.DEPOSIT.getValue());
    }

    @Test
    @DisplayName("按時間範圍過濾")
    void shouldFilterByTimeRange() {
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "500.0000", "req-010");
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "300.0000", "req-011");

      // Set one transaction's create_time to yesterday
      jdbcTemplate.update(
          "UPDATE t_wallet_transaction SET create_time = NOW() - INTERVAL '1 day'"
              + " WHERE request_id = 'req-010'");

      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      WalletTransactionQueryForm query = new WalletTransactionQueryForm();
      query.setWalletId(testWalletId);
      query.setStartTime(now.minusHours(1));
      query.setEndTime(now.plusHours(1));
      Page<WalletTransactionVO> page = new Page<>(1, 10);
      List<WalletTransactionVO> result = walletTransactionDao.queryPage(page, query);

      // Only req-011 (today) should be returned
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getAmount()).isEqualByComparingTo("300.0000");
    }
  }

  // ==================== sumAmountByType ====================

  @Nested
  @DisplayName("sumAmountByType 聚合查詢")
  class SumAmountByTypeTest {

    @Test
    @DisplayName("加總指定類型的交易絕對金額")
    void shouldSumAbsoluteAmounts() {
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "1000.0000", "req-001");
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.DEPOSIT.getValue(), "500.0000", "req-002");
      insertTransaction(
          testWalletId, PLAYER_ID, TransactionTypeEnum.BET.getValue(), "-200.0000", "req-003");

      OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
      OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);

      BigDecimal sum =
          walletTransactionDao.sumAmountByType(TransactionTypeEnum.DEPOSIT.getValue(), start, end);

      assertThat(sum).isEqualByComparingTo("1500.0000");
    }

    @Test
    @DisplayName("無匹配記錄 → COALESCE 返回 0")
    void shouldReturnZeroWhenNoRecords() {
      OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
      OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);

      BigDecimal sum =
          walletTransactionDao.sumAmountByType(TransactionTypeEnum.DEPOSIT.getValue(), start, end);

      assertThat(sum).isEqualByComparingTo("0");
    }
  }

  // ==================== Helper Methods ====================

  private Long createTestWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setPlayerId(PLAYER_ID);
    wallet.setWalletType(WalletTypeEnum.CASH.getValue());
    wallet.setCurrencyCode("USD");
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setVersion(0);
    wallet.setDeleted(false);
    walletDao.insert(wallet);
    return wallet.getWalletId();
  }

  private void insertTransaction(
      Long walletId, Long playerId, Integer type, String amount, String requestId) {
    WalletTransactionEntity txn = new WalletTransactionEntity();
    txn.setWalletId(walletId);
    txn.setPlayerId(playerId);
    txn.setTransactionType(type);
    txn.setAmount(new BigDecimal(amount));
    txn.setBalanceBefore(BigDecimal.ZERO);
    txn.setBalanceAfter(new BigDecimal(amount));
    txn.setRequestId(requestId);
    walletTransactionDao.insert(txn);
  }
}
