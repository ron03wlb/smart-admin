package net.lab1024.sa.app.igaming.agent.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAgentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionRecordDao;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAgentEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.credit.dao.AgentCreditDao;
import net.lab1024.sa.igaming.agent.credit.dao.SettlementRecordDao;
import net.lab1024.sa.igaming.agent.credit.domain.entity.AgentCreditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.SettlementRecordEntity;
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
 * Agent Dao integration tests — verifies custom queries across 4 agent-related Dao interfaces
 * (AffiliateAgentDao, AffiliateCommissionRecordDao, AgentCreditDao, SettlementRecordDao) against
 * real PostgreSQL 16.
 *
 * @author iGaming Team
 * @since 2026-03-07
 */
@Tag("integration")
@SpringBootTest(
    classes = AgentDaoIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = AgentDaoIntegrationTest.SchemaInitializer.class)
@DisplayName("Agent Dao 整合測試 (Testcontainers + PostgreSQL)")
class AgentDaoIntegrationTest {

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
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.agent.affiliate.dao",
        "net.lab1024.sa.igaming.agent.credit.dao",
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
  }

  private static final Long TEST_TENANT_ID = 1L;
  private static final Long OTHER_TENANT_ID = 2L;

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
            conn, new ClassPathResource("db/migration/V14__agent_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize agent test schema", e);
      }
    }
  }

  @Autowired private AffiliateAgentDao affiliateAgentDao;
  @Autowired private AffiliateCommissionRecordDao commissionRecordDao;
  @Autowired private AgentCreditDao agentCreditDao;
  @Autowired private SettlementRecordDao settlementRecordDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_settlement_record, t_credit_allocation_audit, t_agent_credit,"
            + " t_affiliate_adjustment, t_affiliate_commission_record,"
            + " t_affiliate_commission_plan, t_affiliate_hierarchy, t_affiliate_agent CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== AffiliateAgentDao ====================

  @Nested
  @DisplayName("AffiliateAgentDao 代理主資料查詢")
  class AffiliateAgentDaoTest {

    @Test
    @DisplayName("findByUsernameAndTenantId → 找到匹配代理")
    void shouldFindByUsernameAndTenantId() {
      insertAgent("agent_alpha", null, 1, 1);

      AffiliateAgentEntity result =
          affiliateAgentDao.findByUsernameAndTenantId("agent_alpha", TEST_TENANT_ID);

      assertThat(result).isNotNull();
      assertThat(result.getUsername()).isEqualTo("agent_alpha");
    }

    @Test
    @DisplayName("findByUsernameAndTenantId → 不同 tenant 返回 null")
    void shouldReturnNullForDifferentTenant() {
      insertAgent("agent_alpha", null, 1, 1);

      AffiliateAgentEntity result =
          affiliateAgentDao.findByUsernameAndTenantId("agent_alpha", OTHER_TENANT_ID);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("findByParentAgentIdAndTenantId → 返回下線代理列表")
    void shouldFindDownlineAgents() {
      AffiliateAgentEntity parent = insertAgent("parent_agent", null, 1, 1);
      insertAgent("child_1", parent.getAgentId(), 2, 1);
      insertAgent("child_2", parent.getAgentId(), 2, 1);
      insertAgent("other_child", parent.getAgentId(), 2, 1);

      List<AffiliateAgentEntity> result =
          affiliateAgentDao.findByParentAgentIdAndTenantId(parent.getAgentId(), TEST_TENANT_ID);

      assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("findByParentAgentIdAndTenantId → 無下線返回空列表")
    void shouldReturnEmptyWhenNoDownline() {
      AffiliateAgentEntity agent = insertAgent("solo_agent", null, 1, 1);

      List<AffiliateAgentEntity> result =
          affiliateAgentDao.findByParentAgentIdAndTenantId(agent.getAgentId(), TEST_TENANT_ID);

      assertThat(result).isEmpty();
    }
  }

  // ==================== AffiliateCommissionRecordDao ====================

  @Nested
  @DisplayName("AffiliateCommissionRecordDao 佣金記錄查詢")
  class AffiliateCommissionRecordDaoTest {

    @Test
    @DisplayName("findByAgentAndDate → 找到指定日期的結算記錄")
    void shouldFindByAgentAndDate() {
      AffiliateAgentEntity agent = insertAgent("comm_agent", null, 1, 1);
      LocalDate today = LocalDate.now();
      insertCommissionRecord(agent.getAgentId(), today, "1000.0000", 1);

      AffiliateCommissionRecordEntity result =
          commissionRecordDao.findByAgentAndDate(agent.getAgentId(), today, TEST_TENANT_ID);

      assertThat(result).isNotNull();
      assertThat(result.getGrossAmount()).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("findPendingByTenantId → 返回 status=1 的待處理記錄 (DESC)")
    void shouldReturnPendingRecords() {
      AffiliateAgentEntity agent1 = insertAgent("pending_agent_1", null, 1, 1);
      AffiliateAgentEntity agent2 = insertAgent("pending_agent_2", null, 1, 1);
      insertCommissionRecord(agent1.getAgentId(), LocalDate.now(), "500.0000", 1);
      insertCommissionRecord(agent2.getAgentId(), LocalDate.now().minusDays(1), "300.0000", 1);
      // Approved record — excluded
      insertCommissionRecord(agent1.getAgentId(), LocalDate.now().minusDays(2), "200.0000", 2);

      List<AffiliateCommissionRecordEntity> result =
          commissionRecordDao.findPendingByTenantId(TEST_TENANT_ID);

      assertThat(result).hasSize(2);
      // Ordered by settlement_date DESC
      assertThat(result.get(0).getSettlementDate())
          .isAfterOrEqualTo(result.get(1).getSettlementDate());
    }
  }

  // ==================== AgentCreditDao ====================

  @Nested
  @DisplayName("AgentCreditDao 信用額度查詢")
  class AgentCreditDaoTest {

    @Test
    @DisplayName("findByAgentIdAndTenantId → 找到信用記錄")
    void shouldFindByAgentIdAndTenantId() {
      AffiliateAgentEntity agent = insertAgent("credit_agent", null, 1, 1);
      insertAgentCredit(agent.getAgentId(), null, "50000.0000", "10000.0000");

      AgentCreditEntity result =
          agentCreditDao.findByAgentIdAndTenantId(agent.getAgentId(), TEST_TENANT_ID);

      assertThat(result).isNotNull();
      assertThat(result.getCreditLimit()).isEqualByComparingTo("50000.0000");
      assertThat(result.getUsedCredit()).isEqualByComparingTo("10000.0000");
    }

    @Test
    @DisplayName("findByParentIdAndTenantId → 返回指定 parent 的子信用記錄")
    void shouldFindByParentIdAndTenantId() {
      AffiliateAgentEntity parent = insertAgent("parent_credit", null, 1, 1);
      AffiliateAgentEntity child1 = insertAgent("child_credit_1", parent.getAgentId(), 2, 1);
      AffiliateAgentEntity child2 = insertAgent("child_credit_2", parent.getAgentId(), 2, 1);
      insertAgentCredit(child1.getAgentId(), parent.getAgentId(), "20000.0000", "5000.0000");
      insertAgentCredit(child2.getAgentId(), parent.getAgentId(), "10000.0000", "2000.0000");

      List<AgentCreditEntity> result =
          agentCreditDao.findByParentIdAndTenantId(parent.getAgentId(), TEST_TENANT_ID);

      assertThat(result).hasSize(2);
    }
  }

  // ==================== SettlementRecordDao ====================

  @Nested
  @DisplayName("SettlementRecordDao 結算記錄查詢")
  class SettlementRecordDaoTest {

    @Test
    @DisplayName("findByAgentAndWeek → 找到指定週的結算記錄")
    void shouldFindByAgentAndWeek() {
      AffiliateAgentEntity agent = insertAgent("settle_agent", null, 1, 1);
      insertSettlementRecord(agent.getAgentId(), null, "2026-W10", "5000.0000");

      SettlementRecordEntity result =
          settlementRecordDao.findByAgentAndWeek(agent.getAgentId(), "2026-W10", TEST_TENANT_ID);

      assertThat(result).isNotNull();
      assertThat(result.getSettlementWeek()).isEqualTo("2026-W10");
      assertThat(result.getPlayerLoss()).isEqualByComparingTo("5000.0000");
    }

    @Test
    @DisplayName("findByWeekAndTenantId → 返回該週所有代理結算記錄")
    void shouldFindAllSettlementsForWeek() {
      AffiliateAgentEntity agent1 = insertAgent("settle_agent_1", null, 1, 1);
      AffiliateAgentEntity agent2 = insertAgent("settle_agent_2", null, 1, 1);
      insertSettlementRecord(agent1.getAgentId(), null, "2026-W10", "5000.0000");
      insertSettlementRecord(agent2.getAgentId(), null, "2026-W10", "3000.0000");
      // Different week — excluded
      insertSettlementRecord(agent1.getAgentId(), null, "2026-W09", "2000.0000");

      List<SettlementRecordEntity> result =
          settlementRecordDao.findByWeekAndTenantId("2026-W10", TEST_TENANT_ID);

      assertThat(result).hasSize(2);
    }
  }

  // ==================== Helper Methods ====================

  private AffiliateAgentEntity insertAgent(
      String username, Long parentAgentId, int agentLevel, int status) {
    AffiliateAgentEntity entity = new AffiliateAgentEntity();
    entity.setUsername(username);
    entity.setParentAgentId(parentAgentId);
    entity.setHierarchyPath("/" + username + "/");
    entity.setAgentLevel(agentLevel);
    entity.setTotalPlayers(0);
    entity.setActivePlayers(0);
    entity.setTotalCommission(BigDecimal.ZERO);
    entity.setStatus(status);
    entity.setReferralCode("REF-" + username);
    entity.setDeleted(false);
    entity.setVersion(0);
    affiliateAgentDao.insert(entity);
    return entity;
  }

  private void insertCommissionRecord(
      Long agentId, LocalDate settlementDate, String grossAmount, int status) {
    AffiliateCommissionRecordEntity entity = new AffiliateCommissionRecordEntity();
    entity.setAgentId(agentId);
    entity.setPlanId(1L);
    entity.setSettlementDate(settlementDate);
    entity.setGrossAmount(new BigDecimal(grossAmount));
    entity.setAdjustmentAmount(BigDecimal.ZERO);
    entity.setCarryoverAmount(BigDecimal.ZERO);
    entity.setNetAmount(new BigDecimal(grossAmount));
    entity.setStatus(status);
    commissionRecordDao.insert(entity);
  }

  private void insertAgentCredit(
      Long agentId, Long parentId, String creditLimit, String usedCredit) {
    AgentCreditEntity entity = new AgentCreditEntity();
    entity.setAgentId(agentId);
    entity.setParentId(parentId);
    entity.setCreditLimit(new BigDecimal(creditLimit));
    entity.setUsedCredit(new BigDecimal(usedCredit));
    entity.setAllocatedToChildren(BigDecimal.ZERO);
    entity.setPositionPercent(new BigDecimal("50.0000"));
    entity.setMaxPosition(new BigDecimal("100.0000"));
    entity.setStatus(1);
    entity.setDeleted(false);
    entity.setVersion(0);
    agentCreditDao.insert(entity);
  }

  private void insertSettlementRecord(Long agentId, Long parentId, String week, String playerLoss) {
    SettlementRecordEntity entity = new SettlementRecordEntity();
    entity.setAgentId(agentId);
    entity.setParentId(parentId);
    entity.setSettlementWeek(week);
    entity.setSettlementPhase(1);
    entity.setPlayerLoss(new BigDecimal(playerLoss));
    entity.setOwnShare(BigDecimal.ZERO);
    entity.setToParent(BigDecimal.ZERO);
    entity.setToPlatform(BigDecimal.ZERO);
    entity.setPaymentStatus(1);
    settlementRecordDao.insert(entity);
  }
}
