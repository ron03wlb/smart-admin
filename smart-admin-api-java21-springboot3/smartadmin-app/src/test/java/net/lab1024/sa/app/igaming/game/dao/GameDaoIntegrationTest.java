package net.lab1024.sa.app.igaming.game.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.GameCategoryEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
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
 * GameDao integration tests — verifies queryPage (keyword ILIKE, category filter, JOIN with
 * t_game_provider), selectEnabledGamesByTenant, and selectGamesByTenantAndCategory against real
 * PostgreSQL 16.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = GameDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = GameDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("GameDao 整合測試 (Testcontainers + PostgreSQL)")
class GameDaoIntegrationTest {

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
  @MapperScan(value = "net.lab1024.sa.igaming.game.dao", annotationClass = Mapper.class)
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
            conn, new ClassPathResource("db/migration/V11__game_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize game test schema", e);
      }
    }
  }

  @Autowired private GameDao gameDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute("TRUNCATE t_game_round, t_game, t_game_provider CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== queryPage ====================

  @Nested
  @DisplayName("queryPage 分頁查詢 (JOIN t_game_provider)")
  class QueryPageTest {

    @Test
    @DisplayName("無過濾條件 → 返回所有非刪除遊戲")
    void shouldReturnAllNonDeletedGames() {
      Long providerId = insertProvider("PROV_A", "Provider A", true);
      insertGame(
          providerId,
          "SLOTS_001",
          "Lucky Slots",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          false,
          1000L);
      insertGame(
          providerId,
          "POKER_001",
          "Texas Poker",
          GameCategoryEnum.POKER.getValue(),
          true,
          false,
          500L);
      insertGame(
          providerId,
          "SLOTS_DEL",
          "Deleted Game",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          true,
          0L);

      GameQueryForm query = new GameQueryForm();
      Page<GameVO> page = new Page<>(1, 10);
      List<GameVO> result = gameDao.queryPage(page, query);

      assertThat(result).hasSize(2);
      assertThat(page.getTotal()).isEqualTo(2L);
    }

    @Test
    @DisplayName("按 keyword ILIKE 搜尋 (遊戲名稱或代碼)")
    void shouldFilterByKeyword() {
      Long providerId = insertProvider("PROV_A", "Provider A", true);
      insertGame(
          providerId,
          "SLOTS_001",
          "Lucky Slots",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          false,
          1000L);
      insertGame(
          providerId,
          "POKER_001",
          "Texas Poker",
          GameCategoryEnum.POKER.getValue(),
          true,
          false,
          500L);

      GameQueryForm query = new GameQueryForm();
      query.setKeyword("lucky");
      Page<GameVO> page = new Page<>(1, 10);
      List<GameVO> result = gameDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getGameName()).isEqualTo("Lucky Slots");
    }

    @Test
    @DisplayName("按 category 過濾")
    void shouldFilterByCategory() {
      Long providerId = insertProvider("PROV_A", "Provider A", true);
      insertGame(
          providerId,
          "SLOTS_001",
          "Lucky Slots",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          false,
          1000L);
      insertGame(
          providerId,
          "POKER_001",
          "Texas Poker",
          GameCategoryEnum.POKER.getValue(),
          true,
          false,
          500L);

      GameQueryForm query = new GameQueryForm();
      query.setCategory(GameCategoryEnum.POKER.getValue());
      Page<GameVO> page = new Page<>(1, 10);
      List<GameVO> result = gameDao.queryPage(page, query);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCategory()).isEqualTo(GameCategoryEnum.POKER.getValue());
    }
  }

  // ==================== selectEnabledGamesByTenant ====================

  @Nested
  @DisplayName("selectEnabledGamesByTenant 啟用遊戲查詢")
  class SelectEnabledGamesByTenantTest {

    @Test
    @DisplayName("只返回 enabled=true 且 provider enabled=true 的遊戲")
    void shouldReturnOnlyEnabledGamesWithEnabledProviders() {
      Long enabledProv = insertProvider("PROV_A", "Enabled Provider", true);
      Long disabledProv = insertProvider("PROV_B", "Disabled Provider", false);

      insertGame(
          enabledProv,
          "SLOTS_001",
          "Active Slots",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          false,
          1000L);
      insertGame(
          enabledProv,
          "SLOTS_OFF",
          "Disabled Slots",
          GameCategoryEnum.SLOTS.getValue(),
          false,
          false,
          500L);
      insertGame(
          disabledProv,
          "POKER_001",
          "Poker on Disabled",
          GameCategoryEnum.POKER.getValue(),
          true,
          false,
          200L);

      List<GameVO> result = gameDao.selectEnabledGamesByTenant(TEST_TENANT_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getGameCode()).isEqualTo("SLOTS_001");
    }
  }

  // ==================== selectGamesByTenantAndCategory ====================

  @Nested
  @DisplayName("selectGamesByTenantAndCategory 按分類查詢啟用遊戲")
  class SelectGamesByTenantAndCategoryTest {

    @Test
    @DisplayName("按 category 過濾 + 排除 disabled")
    void shouldFilterByCategoryAndEnabled() {
      Long providerId = insertProvider("PROV_A", "Provider A", true);
      insertGame(
          providerId,
          "SLOTS_001",
          "Slots A",
          GameCategoryEnum.SLOTS.getValue(),
          true,
          false,
          1000L);
      insertGame(
          providerId, "SLOTS_002", "Slots B", GameCategoryEnum.SLOTS.getValue(), true, false, 500L);
      insertGame(
          providerId, "POKER_001", "Poker A", GameCategoryEnum.POKER.getValue(), true, false, 200L);
      insertGame(
          providerId,
          "SLOTS_OFF",
          "Disabled Slots",
          GameCategoryEnum.SLOTS.getValue(),
          false,
          false,
          0L);

      List<GameVO> result =
          gameDao.selectGamesByTenantAndCategory(TEST_TENANT_ID, GameCategoryEnum.SLOTS.getValue());

      assertThat(result).hasSize(2);
      assertThat(result)
          .allSatisfy(
              vo -> {
                assertThat(vo.getCategory()).isEqualTo(GameCategoryEnum.SLOTS.getValue());
                assertThat(vo.getEnabled()).isTrue();
              });
    }
  }

  // ==================== Helper Methods ====================

  /** Insert game provider via JdbcTemplate (avoids EncryptedFieldTypeHandler dependency). */
  private Long insertProvider(String providerCode, String providerName, boolean enabled) {
    jdbcTemplate.update(
        "INSERT INTO t_game_provider "
            + "(provider_code, provider_name, api_url, enabled, deleted, tenant_id, "
            + "health_status, version, create_time, update_time) "
            + "VALUES (?, ?, 'http://test.com', ?, FALSE, ?, 1, 0, NOW(), NOW())",
        providerCode,
        providerName,
        enabled,
        TEST_TENANT_ID);
    return jdbcTemplate.queryForObject(
        "SELECT provider_id FROM t_game_provider WHERE provider_code = ?",
        Long.class,
        providerCode);
  }

  private void insertGame(
      Long providerId,
      String gameCode,
      String gameName,
      Integer category,
      boolean enabled,
      boolean deleted,
      Long playCount) {
    GameEntity entity = new GameEntity();
    entity.setProviderId(providerId);
    entity.setGameCode(gameCode);
    entity.setGameName(gameName);
    entity.setCategory(category);
    entity.setThumbnailUrl("http://test.com/" + gameCode + ".png");
    entity.setPlayCount(playCount);
    entity.setEnabled(enabled);
    entity.setDeleted(deleted);
    entity.setVersion(0);
    gameDao.insert(entity);
  }
}
