package net.lab1024.sa.app.igaming.wallet.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
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
 * WalletDao integration tests — verifies custom SQL queries (queryPage, selectForUpdate) against
 * real PostgreSQL 16 via Testcontainers.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = WalletDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = WalletDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("WalletDao 整合測試 (Testcontainers + PostgreSQL)")
class WalletDaoIntegrationTest {

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

  // ==================== queryPage ====================

  @Nested
  @DisplayName("queryPage 分頁查詢")
  class QueryPageTest {

    @Test
    @DisplayName("無過濾條件 → 返回所有非刪除錢包")
    void shouldReturnAllNonDeletedWallets() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(101L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(102L, WalletTypeEnum.BONUS.getValue(), "CNY", true);

      WalletQueryForm query = new WalletQueryForm();
      Page<WalletVO> page = new Page<>(1, 10);
      List<WalletVO> result = walletDao.queryPage(page, query);

      assertThat(result).hasSize(2);
      assertThat(page.getTotal()).isEqualTo(2L);
    }

    @Test
    @DisplayName("按 playerId 過濾")
    void shouldFilterByPlayerId() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(101L, WalletTypeEnum.CASH.getValue(), "USD", false);

      WalletQueryForm query = new WalletQueryForm();
      query.setPlayerId(100L);
      Page<WalletVO> page = new Page<>(1, 10);
      List<WalletVO> result = walletDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getPlayerId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("按 walletType 過濾")
    void shouldFilterByWalletType() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(101L, WalletTypeEnum.BONUS.getValue(), "USD", false);

      WalletQueryForm query = new WalletQueryForm();
      query.setWalletType(WalletTypeEnum.BONUS.getValue());
      Page<WalletVO> page = new Page<>(1, 10);
      List<WalletVO> result = walletDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getWalletType()).isEqualTo(WalletTypeEnum.BONUS.getValue());
    }

    @Test
    @DisplayName("分頁 → 第1頁 size=1，total=3")
    void shouldPaginateResults() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(101L, WalletTypeEnum.CASH.getValue(), "USD", false);
      insertWallet(102L, WalletTypeEnum.BONUS.getValue(), "CNY", false);

      WalletQueryForm query = new WalletQueryForm();
      Page<WalletVO> page = new Page<>(1, 1);
      List<WalletVO> result = walletDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(page.getTotal()).isEqualTo(3L);
    }
  }

  // ==================== selectForUpdate ====================

  @Nested
  @DisplayName("selectForUpdate 悲觀鎖查詢")
  class SelectForUpdateTest {

    @Test
    @DisplayName("存在的錢包 → 返回實體")
    void shouldReturnWalletWhenExists() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", false);

      WalletEntity result = walletDao.selectForUpdate(100L, WalletTypeEnum.CASH.getValue());

      assertThat(result).isNotNull();
      assertThat(result.getPlayerId()).isEqualTo(100L);
      assertThat(result.getWalletType()).isEqualTo(WalletTypeEnum.CASH.getValue());
      assertThat(result.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("已刪除或不存在 → 返回 null")
    void shouldReturnNullWhenDeletedOrNotFound() {
      insertWallet(100L, WalletTypeEnum.CASH.getValue(), "USD", true);

      WalletEntity deleted = walletDao.selectForUpdate(100L, WalletTypeEnum.CASH.getValue());
      assertThat(deleted).isNull();

      WalletEntity notFound = walletDao.selectForUpdate(999L, WalletTypeEnum.CASH.getValue());
      assertThat(notFound).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private void insertWallet(
      Long playerId, Integer walletType, String currencyCode, boolean deleted) {
    WalletEntity entity = new WalletEntity();
    entity.setPlayerId(playerId);
    entity.setWalletType(walletType);
    entity.setCurrencyCode(currencyCode);
    entity.setBalance(BigDecimal.ZERO);
    entity.setLockedAmount(BigDecimal.ZERO);
    entity.setVersion(0);
    entity.setDeleted(deleted);
    walletDao.insert(entity);
  }
}
