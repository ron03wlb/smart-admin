package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.RiskDecisionEnum;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.common.constant.RiskRuleTypeEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalReviewForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleAddForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskRuleVO;
import net.lab1024.sa.igaming.risk.domain.vo.RiskScoreVO;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.igaming.risk.manager.RiskScoreManager;
import net.lab1024.sa.igaming.risk.service.RiskProposalService;
import net.lab1024.sa.igaming.risk.service.RiskRuleService;
import net.lab1024.sa.igaming.risk.service.RiskScoreService;
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
 * Risk module integration tests — verifies risk scoring, proposal workflow, and rule CRUD against
 * real PostgreSQL 16 via Testcontainers.
 *
 * <p>Uses a sliced Spring context ({@link TestConfig}) that only loads risk beans + MyBatis-Plus
 * infrastructure. Risk tables (V13 DDL) are created via {@link SchemaInitializer}.
 *
 * <p>Validates: weighted moving average scoring, auto-lock threshold, proposal lifecycle
 * (create/approve/reject/escalate), rule CRUD with soft delete, and pagination queries.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@SpringBootTest(
    classes = RiskIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = RiskIntegrationTest.SchemaInitializer.class)
@DisplayName("Risk 整合測試 (Testcontainers + PostgreSQL)")
class RiskIntegrationTest {

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
        "net.lab1024.sa.igaming.risk.service",
        "net.lab1024.sa.igaming.risk.manager",
      },
      excludeFilters =
          @ComponentScan.Filter(
              type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
              classes = {
                net.lab1024.sa.igaming.risk.service.RiskEvaluationService.class,
              }))
  @MapperScan(value = "net.lab1024.sa.igaming.risk.dao", annotationClass = Mapper.class)
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
            conn, new ClassPathResource("db/migration/V13__risk_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize risk test schema", e);
      }
    }
  }

  @Autowired private RiskRuleService riskRuleService;
  @Autowired private RiskScoreService riskScoreService;
  @Autowired private RiskScoreManager riskScoreManager;
  @Autowired private RiskProposalService riskProposalService;
  @Autowired private RiskProposalManager riskProposalManager;
  @Autowired private RiskScoreDao riskScoreDao;
  @Autowired private RiskProposalDao riskProposalDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    jdbcTemplate.execute(
        "TRUNCATE t_risk_proposal, t_risk_assessment, t_risk_score,"
            + " t_risk_rule_param, t_geo_restriction CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Risk Rule CRUD ====================

  @Nested
  @DisplayName("Risk Rule CRUD 整合測試")
  class RiskRuleCrudTest {

    @Test
    @DisplayName("新增規則 → DB 持久化 + 查詢驗證")
    void shouldAddAndQueryRule() {
      RiskRuleAddForm form = buildRuleAddForm("Velocity Check", RiskRuleTypeEnum.VELOCITY);
      ResponseDTO<String> addResult = riskRuleService.add(form);
      assertThat(addResult.getSuccess()).isTrue();

      // Query page
      RiskRuleQueryForm queryForm = new RiskRuleQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      ResponseDTO<PageResult<RiskRuleVO>> pageResult = riskRuleService.queryPage(queryForm);
      assertThat(pageResult.getSuccess()).isTrue();
      assertThat(pageResult.getData().getList()).hasSize(1);

      RiskRuleVO rule = pageResult.getData().getList().get(0);
      assertThat(rule.getRuleName()).isEqualTo("Velocity Check");
      assertThat(rule.getRuleType()).isEqualTo(RiskRuleTypeEnum.VELOCITY.getValue());
      assertThat(rule.getWeight()).isEqualByComparingTo("1.5000");
    }

    @Test
    @DisplayName("軟刪除規則 → 查詢不可見")
    void shouldSoftDeleteRule() {
      RiskRuleAddForm form = buildRuleAddForm("Amount Check", RiskRuleTypeEnum.AMOUNT);
      riskRuleService.add(form);

      // Find the rule ID
      RiskRuleQueryForm queryForm = new RiskRuleQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      Long ruleId =
          riskRuleService.queryPage(queryForm).getData().getList().get(0).getRuleParamId();

      // Soft delete
      ResponseDTO<String> deleteResult = riskRuleService.delete(ruleId);
      assertThat(deleteResult.getSuccess()).isTrue();

      // Query returns empty
      ResponseDTO<PageResult<RiskRuleVO>> afterDelete = riskRuleService.queryPage(queryForm);
      assertThat(afterDelete.getData().getList()).isEmpty();

      // getById also returns error
      ResponseDTO<RiskRuleVO> getResult = riskRuleService.getById(ruleId);
      assertThat(getResult.getSuccess()).isFalse();

      // But record still exists in DB (soft deleted)
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_risk_rule_param WHERE rule_param_id = ?", Long.class, ruleId);
      assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("多規則分頁查詢 + 類型篩選")
    void shouldFilterByRuleType() {
      riskRuleService.add(buildRuleAddForm("Velocity 1", RiskRuleTypeEnum.VELOCITY));
      riskRuleService.add(buildRuleAddForm("Amount 1", RiskRuleTypeEnum.AMOUNT));
      riskRuleService.add(buildRuleAddForm("Velocity 2", RiskRuleTypeEnum.VELOCITY));

      // Filter by VELOCITY type
      RiskRuleQueryForm queryForm = new RiskRuleQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setRuleType(RiskRuleTypeEnum.VELOCITY.getValue());

      ResponseDTO<PageResult<RiskRuleVO>> result = riskRuleService.queryPage(queryForm);
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getList()).hasSize(2);
      assertThat(result.getData().getList())
          .allMatch(r -> r.getRuleType().equals(RiskRuleTypeEnum.VELOCITY.getValue()));
    }
  }

  // ==================== Risk Score Manager ====================

  @Nested
  @DisplayName("RiskScoreManager 整合測試")
  class RiskScoreManagerTest {

    @Test
    @DisplayName("首次評估 → 建立新玩家風控檔案")
    void shouldCreateNewProfileOnFirstAssessment() {
      RiskScoreEntity profile = riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 25);

      assertThat(profile.getPlayerId()).isEqualTo(PLAYER_ID);
      assertThat(profile.getTenantId()).isEqualTo(TEST_TENANT_ID);
      assertThat(profile.getCumulativeScore()).isEqualByComparingTo("25");
      assertThat(profile.getRiskLevel()).isEqualTo(RiskLevelEnum.LOW.getValue());
      assertThat(profile.getAutoLocked()).isFalse();
      assertThat(profile.getTotalAssessments()).isEqualTo(1);

      // Verify DB persistence
      RiskScoreEntity persisted = riskScoreDao.findByPlayerIdAndTenantId(PLAYER_ID, TEST_TENANT_ID);
      assertThat(persisted).isNotNull();
      assertThat(persisted.getRiskScoreId()).isNotNull();
    }

    @Test
    @DisplayName("多次評估 → 加權移動平均 (70% old + 30% new)")
    void shouldApplyWeightedMovingAverage() {
      // First: score = 40
      riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 40);

      // Second: newScore = 40 * 0.70 + 60 * 0.30 = 28 + 18 = 46
      RiskScoreEntity updated = riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 60);
      assertThat(updated.getCumulativeScore()).isEqualByComparingTo("46.0000");
      assertThat(updated.getTotalAssessments()).isEqualTo(2);
      assertThat(updated.getRiskLevel()).isEqualTo(RiskLevelEnum.MEDIUM.getValue());

      // Verify DB
      RiskScoreEntity persisted = riskScoreDao.findByPlayerIdAndTenantId(PLAYER_ID, TEST_TENANT_ID);
      assertThat(persisted.getCumulativeScore()).isEqualByComparingTo("46.0000");
    }

    @Test
    @DisplayName("累積分數 >= 80 → 自動凍結")
    void shouldAutoLockWhenScoreReachesThreshold() {
      // First: score = 90 (high risk — already above threshold)
      RiskScoreEntity profile = riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 90);
      assertThat(profile.getAutoLocked()).isTrue();
      assertThat(profile.getRiskLevel()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());

      // Verify in DB
      RiskScoreEntity persisted = riskScoreDao.findByPlayerIdAndTenantId(PLAYER_ID, TEST_TENANT_ID);
      assertThat(persisted.getAutoLocked()).isTrue();
    }

    @Test
    @DisplayName("saveTransactionScore → 評估記錄持久化")
    void shouldPersistAssessmentRecord() {
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "BET_PLACED", 45);
      riskScoreManager.saveTransactionScore(assessment);

      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_risk_assessment WHERE player_id = ?", Long.class, PLAYER_ID);
      assertThat(count).isEqualTo(1L);
    }
  }

  // ==================== Risk Score Query Service ====================

  @Nested
  @DisplayName("RiskScoreService 整合測試")
  class RiskScoreQueryTest {

    @Test
    @DisplayName("查詢玩家風控分數 → ResponseDTO + VO 映射")
    void shouldQueryPlayerRiskScore() {
      // Create profile first
      riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 35);

      ResponseDTO<RiskScoreVO> result = riskScoreService.getByPlayerId(PLAYER_ID);
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getPlayerId()).isEqualTo(PLAYER_ID);
      assertThat(result.getData().getCumulativeScore()).isEqualByComparingTo("35");
    }

    @Test
    @DisplayName("查詢不存在的玩家 → 返回錯誤")
    void shouldReturnErrorForNonexistentPlayer() {
      ResponseDTO<RiskScoreVO> result = riskScoreService.getByPlayerId(999L);
      assertThat(result.getSuccess()).isFalse();
    }
  }

  // ==================== Risk Proposal Lifecycle ====================

  @Nested
  @DisplayName("Risk Proposal 生命週期整合測試")
  class RiskProposalLifecycleTest {

    @Test
    @DisplayName("建立標準審核工單 → 批准 → DB 狀態驗證")
    void shouldCreateAndApproveProposal() {
      // Create assessment first (FK reference)
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "WITHDRAWAL_REQUESTED", 55);
      riskScoreManager.saveTransactionScore(assessment);
      Long assessmentId = assessment.getAssessmentId();

      // Create proposal
      RiskProposalEntity proposal =
          riskProposalManager.createReviewProposal(
              PLAYER_ID, TEST_TENANT_ID, assessmentId, RiskLevelEnum.HIGH);
      assertThat(proposal.getProposalId()).isNotNull();
      assertThat(proposal.getStatus()).isEqualTo(RiskProposalStatusEnum.PENDING.getValue());
      assertThat(proposal.getSlaDeadline()).isNotNull();

      // Approve
      RiskProposalReviewForm reviewForm = new RiskProposalReviewForm();
      reviewForm.setProposalId(proposal.getProposalId());
      reviewForm.setReviewComment("Verified — legitimate withdrawal");

      ResponseDTO<String> approveResult = riskProposalService.approve(reviewForm);
      assertThat(approveResult.getSuccess()).isTrue();

      // Verify DB
      RiskProposalEntity persisted = riskProposalDao.selectById(proposal.getProposalId());
      assertThat(persisted.getStatus()).isEqualTo(RiskProposalStatusEnum.APPROVED.getValue());
      assertThat(persisted.getReviewComment()).isEqualTo("Verified — legitimate withdrawal");
      assertThat(persisted.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("建立緊急工單 → 駁回 → DB 狀態驗證")
    void shouldCreateUrgentAndRejectProposal() {
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "BET_PLACED", 85);
      riskScoreManager.saveTransactionScore(assessment);

      RiskProposalEntity proposal =
          riskProposalManager.createUrgentProposal(
              PLAYER_ID, TEST_TENANT_ID, assessment.getAssessmentId());
      assertThat(proposal.getPriority()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());

      // Reject
      RiskProposalReviewForm reviewForm = new RiskProposalReviewForm();
      reviewForm.setProposalId(proposal.getProposalId());
      reviewForm.setReviewComment("Confirmed fraud pattern");

      ResponseDTO<String> rejectResult = riskProposalService.reject(reviewForm);
      assertThat(rejectResult.getSuccess()).isTrue();

      // Verify DB
      RiskProposalEntity persisted = riskProposalDao.selectById(proposal.getProposalId());
      assertThat(persisted.getStatus()).isEqualTo(RiskProposalStatusEnum.REJECTED.getValue());
    }

    @Test
    @DisplayName("審核已完成的工單 → 返回錯誤")
    void shouldRejectReviewOfCompletedProposal() {
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "BET_PLACED", 50);
      riskScoreManager.saveTransactionScore(assessment);

      RiskProposalEntity proposal =
          riskProposalManager.createReviewProposal(
              PLAYER_ID, TEST_TENANT_ID, assessment.getAssessmentId(), RiskLevelEnum.MEDIUM);

      // Approve first
      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(proposal.getProposalId());
      riskProposalService.approve(form);

      // Try to approve again
      ResponseDTO<String> secondApprove = riskProposalService.approve(form);
      assertThat(secondApprove.getSuccess()).isFalse();

      // Try to reject
      ResponseDTO<String> rejectResult = riskProposalService.reject(form);
      assertThat(rejectResult.getSuccess()).isFalse();
    }

    @Test
    @DisplayName("SLA 逾期升級 — 過期工單自動升級為 ESCALATED")
    void shouldEscalateOverdueProposals() {
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "BET_PLACED", 50);
      riskScoreManager.saveTransactionScore(assessment);

      // Create proposal with past SLA deadline directly via DB
      RiskProposalEntity proposal =
          riskProposalManager.createReviewProposal(
              PLAYER_ID, TEST_TENANT_ID, assessment.getAssessmentId(), RiskLevelEnum.MEDIUM);

      // Force SLA deadline to past
      jdbcTemplate.update(
          "UPDATE t_risk_proposal SET sla_deadline = NOW() - INTERVAL '1 hour'"
              + " WHERE proposal_id = ?",
          proposal.getProposalId());

      // Escalate
      int escalated = riskProposalManager.escalateOverdueProposals();
      assertThat(escalated).isEqualTo(1);

      // Verify DB
      RiskProposalEntity persisted = riskProposalDao.selectById(proposal.getProposalId());
      assertThat(persisted.getStatus()).isEqualTo(RiskProposalStatusEnum.ESCALATED.getValue());
      assertThat(persisted.getPriority()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
    }
  }

  // ==================== End-to-End Scoring + Proposal Flow ====================

  @Nested
  @DisplayName("端到端風控評估流程")
  class EndToEndFlowTest {

    @Test
    @DisplayName("評估 → 風控檔案更新 → 工單建立 → 全鏈路 DB 驗證")
    void shouldCompleteFullRiskAssessmentFlow() {
      // Step 1: Save assessment
      RiskAssessmentEntity assessment = buildAssessment(PLAYER_ID, "WITHDRAWAL_REQUESTED", 65);
      riskScoreManager.saveTransactionScore(assessment);

      // Step 2: Update player risk profile
      RiskScoreEntity score = riskScoreManager.updateProfileScore(PLAYER_ID, TEST_TENANT_ID, 65);
      assertThat(score.getRiskLevel()).isEqualTo(RiskLevelEnum.HIGH.getValue());

      // Step 3: Create proposal based on score
      RiskProposalEntity proposal =
          riskProposalManager.createReviewProposal(
              PLAYER_ID, TEST_TENANT_ID, assessment.getAssessmentId(), RiskLevelEnum.HIGH);

      // Step 4: Verify all 3 tables have data
      Long assessmentCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM t_risk_assessment WHERE player_id = ?", Long.class, PLAYER_ID);
      assertThat(assessmentCount).isEqualTo(1L);

      RiskScoreEntity persistedScore =
          riskScoreDao.findByPlayerIdAndTenantId(PLAYER_ID, TEST_TENANT_ID);
      assertThat(persistedScore).isNotNull();
      assertThat(persistedScore.getCumulativeScore()).isEqualByComparingTo("65");

      RiskProposalEntity persistedProposal = riskProposalDao.selectById(proposal.getProposalId());
      assertThat(persistedProposal).isNotNull();
      assertThat(persistedProposal.getAssessmentId()).isEqualTo(assessment.getAssessmentId());

      // Step 5: Query via service
      ResponseDTO<RiskScoreVO> scoreResult = riskScoreService.getByPlayerId(PLAYER_ID);
      assertThat(scoreResult.getSuccess()).isTrue();
      assertThat(scoreResult.getData().getCumulativeScore()).isEqualByComparingTo("65");
    }
  }

  // ==================== Helper Methods ====================

  private RiskRuleAddForm buildRuleAddForm(String name, RiskRuleTypeEnum type) {
    RiskRuleAddForm form = new RiskRuleAddForm();
    form.setRuleName(name);
    form.setRuleType(type.getValue());
    form.setWeight(new BigDecimal("1.5000"));
    form.setThresholdValue(new BigDecimal("100.0000"));
    form.setTimeWindowSeconds(3600);
    form.setMaxCount(10);
    return form;
  }

  private RiskAssessmentEntity buildAssessment(Long playerId, String eventType, int riskScore) {
    RiskAssessmentEntity entity = new RiskAssessmentEntity();
    entity.setTenantId(TEST_TENANT_ID);
    entity.setPlayerId(playerId);
    entity.setEventType(eventType);
    entity.setEventId("evt-" + System.currentTimeMillis());
    entity.setRiskScore(BigDecimal.valueOf(riskScore));
    entity.setRiskLevel(
        riskScore >= 70
            ? RiskLevelEnum.CRITICAL.getValue()
            : riskScore >= 50
                ? RiskLevelEnum.HIGH.getValue()
                : riskScore >= 30 ? RiskLevelEnum.MEDIUM.getValue() : RiskLevelEnum.LOW.getValue());
    entity.setDecision(
        riskScore >= 70
            ? RiskDecisionEnum.AUTO_REJECTED.getValue()
            : riskScore >= 30
                ? RiskDecisionEnum.PENDING_REVIEW.getValue()
                : RiskDecisionEnum.AUTO_APPROVED.getValue());
    return entity;
  }
}
