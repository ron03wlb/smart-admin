package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.AffiliateRegisterForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionApprovalForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.AffiliateAgentVO;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.manager.AffiliateCommissionManager;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateService;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditAllocateForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditRecallForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.SettlementTriggerForm;
import net.lab1024.sa.igaming.agent.credit.domain.vo.AgentCreditVO;
import net.lab1024.sa.igaming.agent.credit.domain.vo.SettlementRecordVO;
import net.lab1024.sa.igaming.agent.credit.manager.CreditSettlementManager;
import net.lab1024.sa.igaming.agent.credit.service.CreditNetworkService;
import net.lab1024.sa.igaming.common.constant.AgentStatusEnum;
import net.lab1024.sa.igaming.common.constant.CommissionPlanTypeEnum;
import net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.SettlementPeriodEnum;
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
 * Agent module integration tests — verifies credit network and affiliate commission against real
 * PostgreSQL 16 via Testcontainers.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = AgentIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = AgentIntegrationTest.SchemaInitializer.class)
@DisplayName("Agent 整合測試 (Testcontainers + PostgreSQL)")
class AgentIntegrationTest {

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
        "net.lab1024.sa.igaming.agent.credit.service",
        "net.lab1024.sa.igaming.agent.credit.manager",
        "net.lab1024.sa.igaming.agent.affiliate.service",
        "net.lab1024.sa.igaming.agent.affiliate.manager",
      })
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.agent.credit.dao",
        "net.lab1024.sa.igaming.agent.affiliate.dao",
      },
      annotationClass = Mapper.class)
  static class TestConfig {

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

  @Container
  @SuppressWarnings("resource")
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

  @Autowired private CreditNetworkService creditNetworkService;
  @Autowired private CreditSettlementManager creditSettlementManager;
  @Autowired private AffiliateService affiliateService;
  @Autowired private AffiliateCommissionManager affiliateCommissionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_affiliate_adjustment, t_affiliate_commission_record,"
            + " t_affiliate_commission_plan, t_affiliate_hierarchy, t_affiliate_agent,"
            + " t_credit_allocation_audit, t_settlement_record, t_agent_credit CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Credit Network Tests ====================

  @Nested
  @DisplayName("Credit Network 整合測試")
  class CreditNetworkIntegrationTest {

    @Test
    @DisplayName("分配信用額度 — 建立新記錄")
    void allocate_credit_creates_record() {
      // Seed a top-level agent credit
      seedAgentCredit(100L, null, "100000");

      CreditAllocateForm form = new CreditAllocateForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setAmount(new BigDecimal("30000"));
      form.setPositionPercent(new BigDecimal("40"));
      form.setTenantId(TEST_TENANT_ID);
      form.setReason("Branch setup");

      ResponseDTO<String> result = creditNetworkService.allocateCredit(form, "admin");
      assertThat(result.getOk()).isTrue();

      // Verify child credit
      ResponseDTO<AgentCreditVO> childCredit =
          creditNetworkService.getAgentCredit(200L, TEST_TENANT_ID);
      assertThat(childCredit.getOk()).isTrue();
      assertThat(childCredit.getData().getCreditLimit()).isEqualByComparingTo("30000");
      assertThat(childCredit.getData().getPositionPercent()).isEqualByComparingTo("40");

      // Verify parent allocated_to_children updated
      ResponseDTO<AgentCreditVO> parentCredit =
          creditNetworkService.getAgentCredit(100L, TEST_TENANT_ID);
      assertThat(parentCredit.getData().getAllocatedToChildren()).isEqualByComparingTo("30000");
      assertThat(parentCredit.getData().getAvailableCredit()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("額度不足拒絕分配")
    void insufficient_credit_rejected() {
      seedAgentCredit(100L, null, "10000");

      CreditAllocateForm form = new CreditAllocateForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setAmount(new BigDecimal("50000"));
      form.setTenantId(TEST_TENANT_ID);

      ResponseDTO<String> result = creditNetworkService.allocateCredit(form, "admin");
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("回收信用額度")
    void recall_credit() {
      seedAgentCredit(100L, null, "100000");
      seedAgentCredit(200L, 100L, "30000");
      // Update parent's allocated_to_children
      jdbcTemplate.update(
          "UPDATE t_agent_credit SET allocated_to_children = 30000 WHERE agent_id = 100 AND tenant_id = ?",
          TEST_TENANT_ID);

      CreditRecallForm form = new CreditRecallForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setNewLimit(new BigDecimal("15000"));
      form.setTenantId(TEST_TENANT_ID);
      form.setReason("Trim allocation");

      ResponseDTO<String> result = creditNetworkService.reclaimCredit(form, "admin");
      assertThat(result.getOk()).isTrue();

      ResponseDTO<AgentCreditVO> childCredit =
          creditNetworkService.getAgentCredit(200L, TEST_TENANT_ID);
      assertThat(childCredit.getData().getCreditLimit()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("查詢下級代理列表")
    void get_downline_credits() {
      seedAgentCredit(100L, null, "100000");
      seedAgentCredit(200L, 100L, "20000");
      seedAgentCredit(201L, 100L, "30000");

      ResponseDTO<List<AgentCreditVO>> result =
          creditNetworkService.getDownlineCredits(100L, TEST_TENANT_ID);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("週結算建立紀錄")
    void weekly_settlement_creates_records() {
      seedAgentCredit(100L, null, "100000");
      // Set used_credit for settlement calculation
      jdbcTemplate.update(
          "UPDATE t_agent_credit SET used_credit = 10000, position_percent = 40 WHERE agent_id = 100 AND tenant_id = ?",
          TEST_TENANT_ID);

      SettlementTriggerForm form = new SettlementTriggerForm();
      form.setTenantId(TEST_TENANT_ID);
      form.setSettlementWeek("2026-W09");

      ResponseDTO<List<SettlementRecordVO>> result = creditNetworkService.triggerSettlement(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
      assertThat(result.getData().get(0).getSettlementWeek()).isEqualTo("2026-W09");
    }

    @Test
    @DisplayName("驗證結算付款")
    void verify_settlement_payment() {
      seedAgentCredit(100L, null, "100000");
      jdbcTemplate.update(
          "UPDATE t_agent_credit SET used_credit = 5000, position_percent = 30 WHERE agent_id = 100 AND tenant_id = ?",
          TEST_TENANT_ID);

      SettlementTriggerForm triggerForm = new SettlementTriggerForm();
      triggerForm.setTenantId(TEST_TENANT_ID);
      triggerForm.setSettlementWeek("2026-W10");
      ResponseDTO<List<SettlementRecordVO>> triggerResult =
          creditNetworkService.triggerSettlement(triggerForm);

      Long recordId = triggerResult.getData().get(0).getSettlementRecordId();
      ResponseDTO<String> verifyResult =
          creditNetworkService.verifyPayment(recordId, "TXN-PAY-001");
      assertThat(verifyResult.getOk()).isTrue();
    }

    private void seedAgentCredit(Long agentId, Long parentId, String limit) {
      jdbcTemplate.update(
          "INSERT INTO t_agent_credit (tenant_id, agent_id, parent_id, credit_limit, used_credit,"
              + " allocated_to_children, position_percent, max_position, status, deleted, version,"
              + " create_time, update_time)"
              + " VALUES (?, ?, ?, ?, 0, 0, 30, 100, 1, false, 0, NOW(), NOW())",
          TEST_TENANT_ID,
          agentId,
          parentId,
          new BigDecimal(limit));
    }
  }

  // ==================== Affiliate Tests ====================

  @Nested
  @DisplayName("Affiliate 整合測試")
  class AffiliateIntegrationTest {

    @Test
    @DisplayName("註冊頂級代理")
    void register_top_level_agent() {
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("agent01");
      form.setTenantId(TEST_TENANT_ID);
      form.setReferralCode("REF001");

      ResponseDTO<AffiliateAgentVO> result = affiliateService.registerAgent(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getUsername()).isEqualTo("agent01");
      assertThat(result.getData().getAgentLevel()).isEqualTo(1);
      assertThat(result.getData().getStatus()).isEqualTo(AgentStatusEnum.ACTIVE.getValue());
    }

    @Test
    @DisplayName("註冊子代理繼承層級")
    void register_child_agent_inherits_hierarchy() {
      // Register parent first
      AffiliateRegisterForm parentForm = new AffiliateRegisterForm();
      parentForm.setUsername("parent01");
      parentForm.setTenantId(TEST_TENANT_ID);
      ResponseDTO<AffiliateAgentVO> parentResult = affiliateService.registerAgent(parentForm);
      Long parentId = parentResult.getData().getAgentId();

      // Register child
      AffiliateRegisterForm childForm = new AffiliateRegisterForm();
      childForm.setUsername("child01");
      childForm.setParentAgentId(parentId);
      childForm.setTenantId(TEST_TENANT_ID);
      ResponseDTO<AffiliateAgentVO> childResult = affiliateService.registerAgent(childForm);

      assertThat(childResult.getOk()).isTrue();
      assertThat(childResult.getData().getAgentLevel()).isEqualTo(2);
      assertThat(childResult.getData().getParentAgentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("帳號重複拒絕")
    void duplicate_username_rejected() {
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("dup_agent");
      form.setTenantId(TEST_TENANT_ID);
      affiliateService.registerAgent(form);

      AffiliateRegisterForm dup = new AffiliateRegisterForm();
      dup.setUsername("dup_agent");
      dup.setTenantId(TEST_TENANT_ID);
      ResponseDTO<AffiliateAgentVO> result = affiliateService.registerAgent(dup);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("查詢下級代理樹")
    void get_downline_tree() {
      AffiliateRegisterForm parentForm = new AffiliateRegisterForm();
      parentForm.setUsername("tree_parent");
      parentForm.setTenantId(TEST_TENANT_ID);
      Long parentId = affiliateService.registerAgent(parentForm).getData().getAgentId();

      AffiliateRegisterForm childForm = new AffiliateRegisterForm();
      childForm.setUsername("tree_child");
      childForm.setParentAgentId(parentId);
      childForm.setTenantId(TEST_TENANT_ID);
      affiliateService.registerAgent(childForm);

      ResponseDTO<List<AffiliateAgentVO>> result =
          affiliateService.getDownlineTree(parentId, TEST_TENANT_ID);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
      assertThat(result.getData().get(0).getUsername()).isEqualTo("tree_child");
    }

    @Test
    @DisplayName("佣金計算 — Revenue Share")
    void commission_calculation_revenue_share() {
      // Register agent
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("commission_agent");
      form.setTenantId(TEST_TENANT_ID);
      Long agentId = affiliateService.registerAgent(form).getData().getAgentId();

      // Seed a commission plan
      Long planId = seedCommissionPlan(CommissionPlanTypeEnum.REVENUE_SHARE.getValue());
      jdbcTemplate.update(
          "UPDATE t_affiliate_agent SET commission_plan_id = ? WHERE agent_id = ?",
          planId,
          agentId);

      AffiliateCommissionRecordEntity record =
          affiliateCommissionManager.calculateAndIssueCommission(
              agentId, TEST_TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("100000"));

      assertThat(record.getGrossAmount()).isEqualByComparingTo("30000.0000");
      assertThat(record.getNetAmount()).isEqualByComparingTo("30000.0000");
      assertThat(record.getStatus()).isEqualTo(CommissionRecordStatusEnum.PENDING.getValue());
    }

    @Test
    @DisplayName("佣金審核 — 批准後更新累計")
    void commission_approval_updates_total() {
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("approve_agent");
      form.setTenantId(TEST_TENANT_ID);
      Long agentId = affiliateService.registerAgent(form).getData().getAgentId();

      Long planId = seedCommissionPlan(CommissionPlanTypeEnum.REVENUE_SHARE.getValue());
      jdbcTemplate.update(
          "UPDATE t_affiliate_agent SET commission_plan_id = ? WHERE agent_id = ?",
          planId,
          agentId);

      affiliateCommissionManager.calculateAndIssueCommission(
          agentId, TEST_TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("100000"));

      ResponseDTO<List<CommissionRecordVO>> pending =
          affiliateService.getPendingApprovals(TEST_TENANT_ID);
      assertThat(pending.getData()).hasSize(1);

      CommissionApprovalForm approvalForm = new CommissionApprovalForm();
      approvalForm.setRecordId(pending.getData().get(0).getRecordId());
      approvalForm.setApprovedBy("admin");
      ResponseDTO<String> approveResult = affiliateService.approveCommission(approvalForm);
      assertThat(approveResult.getOk()).isTrue();

      // After approval, pending list should be empty
      ResponseDTO<List<CommissionRecordVO>> afterApproval =
          affiliateService.getPendingApprovals(TEST_TENANT_ID);
      assertThat(afterApproval.getData()).isEmpty();
    }

    @Test
    @DisplayName("佣金調整 — 建立調整記錄")
    void create_adjustment_record() {
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("adjust_agent");
      form.setTenantId(TEST_TENANT_ID);
      Long agentId = affiliateService.registerAgent(form).getData().getAgentId();

      var adjustment =
          affiliateCommissionManager.createAdjustment(
              agentId, TEST_TENANT_ID, 1, new BigDecimal("500"), "Late arrival", "admin");

      assertThat(adjustment.getAdjustmentId()).isNotNull();
      assertThat(adjustment.getAmount()).isEqualByComparingTo("500");
    }

    private Long seedCommissionPlan(Integer planType) {
      jdbcTemplate.update(
          "INSERT INTO t_affiliate_commission_plan"
              + " (tenant_id, plan_name, plan_type, tiers_json, settlement_period,"
              + " negative_carryover, enabled, deleted, version, create_time, update_time)"
              + " VALUES (?, 'Test Plan', ?, '[]'::jsonb, ?, false, true, false, 0, NOW(), NOW())",
          TEST_TENANT_ID,
          planType,
          SettlementPeriodEnum.WEEKLY.getValue());

      return jdbcTemplate.queryForObject(
          "SELECT plan_id FROM t_affiliate_commission_plan WHERE tenant_id = ? ORDER BY plan_id DESC LIMIT 1",
          Long.class,
          TEST_TENANT_ID);
    }
  }
}
