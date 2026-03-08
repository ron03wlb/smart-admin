package net.lab1024.sa.app.igaming.game.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
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
 * GameRoundDao integration tests — verifies custom XML queries (selectByTransactionId,
 * selectUnsettledRoundsOlderThan, selectByProviderAndDate, sumWeightedTurnoverByPlayer) against
 * real PostgreSQL 16. Tests PostgreSQL-specific features: interval arithmetic, date casting,
 * COALESCE aggregation.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = GameRoundDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = GameRoundDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("GameRoundDao 整合測試 (Testcontainers + PostgreSQL)")
class GameRoundDaoIntegrationTest {

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
            conn, new ClassPathResource("db/migration/V11__game_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize game test schema", e);
      }
    }
  }

  @Autowired private GameRoundDao gameRoundDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute("TRUNCATE t_game_round CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== selectByTransactionId ====================

  @Nested
  @DisplayName("selectByTransactionId 按交易ID查詢")
  class SelectByTransactionIdTest {

    @Test
    @DisplayName("存在的 transactionId → 返回 round")
    void shouldReturnRoundWhenFound() {
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-001",
          "GAME_A",
          RoundStatusEnum.OPEN.getValue(),
          false,
          "100.00",
          "0.00",
          "80.00");

      GameRoundEntity result = gameRoundDao.selectByTransactionId("txn-001");

      assertThat(result).isNotNull();
      assertThat(result.getTransactionId()).isEqualTo("txn-001");
      assertThat(result.getPlayerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    @DisplayName("已刪除或不存在 → 返回 null")
    void shouldReturnNullWhenDeletedOrNotFound() {
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-deleted",
          "GAME_A",
          RoundStatusEnum.OPEN.getValue(),
          true,
          "100.00",
          "0.00",
          "80.00");

      assertThat(gameRoundDao.selectByTransactionId("txn-deleted")).isNull();
      assertThat(gameRoundDao.selectByTransactionId("txn-nonexistent")).isNull();
    }
  }

  // ==================== selectUnsettledRoundsOlderThan ====================

  @Nested
  @DisplayName("selectUnsettledRoundsOlderThan PostgreSQL interval 查詢")
  class SelectUnsettledRoundsOlderThanTest {

    @Test
    @DisplayName("返回超過指定分鐘的 OPEN 狀態 rounds")
    void shouldReturnOldUnsettledRounds() {
      // Insert an OPEN round and set its create_time to 2 hours ago
      GameRoundEntity round =
          insertRound(
              PLAYER_ID,
              "PROVIDER_A",
              "txn-old",
              "GAME_A",
              RoundStatusEnum.OPEN.getValue(),
              false,
              "100.00",
              "0.00",
              "80.00");
      jdbcTemplate.update(
          "UPDATE t_game_round SET create_time = NOW() - INTERVAL '2 hours' WHERE round_id = ?",
          round.getRoundId());

      List<GameRoundEntity> result = gameRoundDao.selectUnsettledRoundsOlderThan(60);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTransactionId()).isEqualTo("txn-old");
    }

    @Test
    @DisplayName("排除已結算和最近的 rounds")
    void shouldExcludeSettledAndRecentRounds() {
      // SETTLED round (old) — excluded by status
      GameRoundEntity settled =
          insertRound(
              PLAYER_ID,
              "PROVIDER_A",
              "txn-settled",
              "GAME_A",
              RoundStatusEnum.SETTLED.getValue(),
              false,
              "100.00",
              "200.00",
              "80.00");
      jdbcTemplate.update(
          "UPDATE t_game_round SET create_time = NOW() - INTERVAL '2 hours' WHERE round_id = ?",
          settled.getRoundId());

      // OPEN round (recent) — excluded by time
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-recent",
          "GAME_A",
          RoundStatusEnum.OPEN.getValue(),
          false,
          "50.00",
          "0.00",
          "40.00");

      // OPEN round (deleted, old) — excluded by deleted
      GameRoundEntity deleted =
          insertRound(
              PLAYER_ID,
              "PROVIDER_A",
              "txn-del",
              "GAME_A",
              RoundStatusEnum.OPEN.getValue(),
              true,
              "50.00",
              "0.00",
              "40.00");
      jdbcTemplate.update(
          "UPDATE t_game_round SET create_time = NOW() - INTERVAL '2 hours' WHERE round_id = ?",
          deleted.getRoundId());

      List<GameRoundEntity> result = gameRoundDao.selectUnsettledRoundsOlderThan(60);

      assertThat(result).isEmpty();
    }
  }

  // ==================== selectByProviderAndDate ====================

  @Nested
  @DisplayName("selectByProviderAndDate 按供應商和日期查詢")
  class SelectByProviderAndDateTest {

    @Test
    @DisplayName("匹配 providerCode + 今日日期 → 返回 rounds")
    void shouldReturnRoundsForProviderAndDate() {
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-pa-1",
          "GAME_A",
          RoundStatusEnum.SETTLED.getValue(),
          false,
          "100.00",
          "200.00",
          "80.00");
      insertRound(
          PLAYER_ID,
          "PROVIDER_B",
          "txn-pb-1",
          "GAME_B",
          RoundStatusEnum.SETTLED.getValue(),
          false,
          "50.00",
          "100.00",
          "40.00");

      List<GameRoundEntity> result =
          gameRoundDao.selectByProviderAndDate("PROVIDER_A", LocalDate.now());

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getProviderCode()).isEqualTo("PROVIDER_A");
    }

    @Test
    @DisplayName("日期不匹配 → 返回空")
    void shouldReturnEmptyForWrongDate() {
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-pa-2",
          "GAME_A",
          RoundStatusEnum.SETTLED.getValue(),
          false,
          "100.00",
          "200.00",
          "80.00");

      List<GameRoundEntity> result =
          gameRoundDao.selectByProviderAndDate("PROVIDER_A", LocalDate.now().minusDays(1));

      assertThat(result).isEmpty();
    }
  }

  // ==================== sumWeightedTurnoverByPlayer ====================

  @Nested
  @DisplayName("sumWeightedTurnoverByPlayer 加權投注額聚合")
  class SumWeightedTurnoverByPlayerTest {

    @Test
    @DisplayName("只加總 SETTLED (status=2) rounds 的 weighted_turnover")
    void shouldSumSettledRoundsOnly() {
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-s1",
          "GAME_A",
          RoundStatusEnum.SETTLED.getValue(),
          false,
          "100.00",
          "200.00",
          "80.00");
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-s2",
          "GAME_A",
          RoundStatusEnum.SETTLED.getValue(),
          false,
          "200.00",
          "100.00",
          "160.00");
      // OPEN round — excluded
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-open",
          "GAME_A",
          RoundStatusEnum.OPEN.getValue(),
          false,
          "50.00",
          "0.00",
          "40.00");
      // Deleted SETTLED — excluded
      insertRound(
          PLAYER_ID,
          "PROVIDER_A",
          "txn-del",
          "GAME_A",
          RoundStatusEnum.SETTLED.getValue(),
          true,
          "300.00",
          "0.00",
          "240.00");

      BigDecimal sum = gameRoundDao.sumWeightedTurnoverByPlayer(PLAYER_ID, TEST_TENANT_ID);

      // 80.00 + 160.00 = 240.00
      assertThat(sum).isEqualByComparingTo("240.00");
    }

    @Test
    @DisplayName("無匹配記錄 → COALESCE 返回 0")
    void shouldReturnZeroWhenNoSettledRounds() {
      BigDecimal sum = gameRoundDao.sumWeightedTurnoverByPlayer(999L, TEST_TENANT_ID);

      assertThat(sum).isEqualByComparingTo("0");
    }
  }

  // ==================== Helper Methods ====================

  private GameRoundEntity insertRound(
      Long playerId,
      String providerCode,
      String transactionId,
      String gameCode,
      Integer status,
      boolean deleted,
      String betAmount,
      String payoutAmount,
      String weightedTurnover) {
    GameRoundEntity entity = new GameRoundEntity();
    entity.setPlayerId(playerId);
    entity.setProviderCode(providerCode);
    entity.setGpRoundId("gp-" + transactionId);
    entity.setGameCode(gameCode);
    entity.setTransactionId(transactionId);
    entity.setBetAmount(new BigDecimal(betAmount));
    entity.setPayoutAmount(new BigDecimal(payoutAmount));
    entity.setWeightedTurnover(new BigDecimal(weightedTurnover));
    entity.setStatus(status);
    entity.setReconciliationStatus(1);
    entity.setDeleted(deleted);
    entity.setVersion(0);
    gameRoundDao.insert(entity);
    return entity;
  }
}
