package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.security.encrypt.AesGcmFieldEncryptService;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.BonusClaimForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleAddForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleQueryForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.domain.vo.BonusClaimResultVO;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager;
import net.lab1024.sa.igaming.activity.manager.VipAutoEvaluationManager;
import net.lab1024.sa.igaming.activity.manager.WageringProgressManager;
import net.lab1024.sa.igaming.activity.service.BonusClaimService;
import net.lab1024.sa.igaming.activity.service.PromotionRuleService;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
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
 * Activity integration tests — verifies promotion rules, bonus claims, wagering progress, and bonus
 * lifecycle against real PostgreSQL 16 via Testcontainers.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@Tag("integration")
@SpringBootTest(
    classes = ActivityIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = ActivityIntegrationTest.SchemaInitializer.class)
@DisplayName("Activity 整合測試 (Testcontainers + PostgreSQL)")
class ActivityIntegrationTest {

  @Configuration
  @EnableCaching
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
        "net.lab1024.sa.igaming.activity.service",
        "net.lab1024.sa.igaming.activity.manager",
        "net.lab1024.sa.igaming.wallet.manager",
      },
      excludeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = VipAutoEvaluationManager.class))
  @MapperScan(
      value = {
        "net.lab1024.sa.igaming.activity.dao",
        "net.lab1024.sa.igaming.wallet.dao",
        "net.lab1024.sa.igaming.game.dao",
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

    @Bean
    public net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher domainEventPublisher() {
      return org.mockito.Mockito.mock(
          net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher.class);
    }

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("promotion:active", "promotion:code");
    }

    @Bean
    public AesGcmFieldEncryptService aesGcmFieldEncryptService() throws Exception {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      SecretKey key = keyGen.generateKey();
      AesGcmFieldEncryptService service = new AesGcmFieldEncryptService(key);
      EncryptedFieldTypeHandler.setEncryptService(service);
      return service;
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
        // V11: game tables (needed for WageringProgressManager → GameDao, GameWeightConfigDao)
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V11__game_tables.sql"));
        // V12: activity tables
        ScriptUtils.executeSqlScript(
            conn, new ClassPathResource("db/migration/V12__activity_tables.sql"));
      } catch (SQLException e) {
        throw new RuntimeException("Failed to initialize activity test schema", e);
      }
    }
  }

  @Autowired private PromotionRuleService promotionRuleService;
  @Autowired private BonusClaimService bonusClaimService;
  @Autowired private WageringProgressManager wageringProgressManager;
  @Autowired private BonusLifecycleManager bonusLifecycleManager;
  @Autowired private PromotionRuleDao promotionRuleDao;
  @Autowired private PlayerBonusRecordDao playerBonusRecordDao;
  @Autowired private WalletDao walletDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(TEST_TENANT_ID);
    clearCaches();
    jdbcTemplate.execute("TRUNCATE t_player_bonus_record CASCADE");
    jdbcTemplate.execute("TRUNCATE t_promotion_rule CASCADE");
    jdbcTemplate.execute(
        "TRUNCATE t_wallet_lock, t_wallet_bonus_ext," + " t_wallet_transaction, t_wallet CASCADE");
    jdbcTemplate.execute("TRUNCATE t_game_weight_config, t_game, t_game_provider CASCADE");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  // ==================== Promotion Rule CRUD ====================

  @Nested
  @DisplayName("promotionRule 促銷規則管理整合測試")
  class PromotionRuleManagementTest {

    @Test
    @DisplayName("新增促銷規則")
    void addRule_success() {
      PromotionRuleAddForm form = buildAddForm("WELCOME100", "歡迎首存100%");

      ResponseDTO<PromotionRuleVO> result = promotionRuleService.addRule(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getPromotionCode()).isEqualTo("WELCOME100");
      assertThat(result.getData().getRuleId()).isNotNull();
    }

    @Test
    @DisplayName("促銷代碼重複 — 拒絕")
    void addRule_duplicateCode_rejected() {
      promotionRuleService.addRule(buildAddForm("DUP001", "第一次"));

      ResponseDTO<PromotionRuleVO> result =
          promotionRuleService.addRule(buildAddForm("DUP001", "第二次"));

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("分頁查詢促銷規則")
    void queryRules_paginated() {
      promotionRuleService.addRule(buildAddForm("Q001", "促銷1"));
      promotionRuleService.addRule(buildAddForm("Q002", "促銷2"));

      PromotionRuleQueryForm form = new PromotionRuleQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      var result = promotionRuleService.queryRules(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(2);
    }

    @Test
    @DisplayName("更新促銷規則")
    void updateRule_success() {
      ResponseDTO<PromotionRuleVO> added =
          promotionRuleService.addRule(buildAddForm("UPD001", "舊名稱"));
      Long ruleId = added.getData().getRuleId();

      PromotionRuleUpdateForm updateForm = new PromotionRuleUpdateForm();
      updateForm.setRuleId(ruleId);
      updateForm.setPromotionCode("UPD001");
      updateForm.setPromotionName("新名稱");
      updateForm.setPromotionType(1);
      updateForm.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
      updateForm.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

      ResponseDTO<Void> result = promotionRuleService.updateRule(updateForm);

      assertThat(result.getOk()).isTrue();
      var updated = promotionRuleService.getRule(ruleId);
      assertThat(updated.isDefined()).isTrue();
      assertThat(updated.get().getPromotionName()).isEqualTo("新名稱");
    }

    @Test
    @DisplayName("切換促銷狀態")
    void toggleStatus() {
      ResponseDTO<PromotionRuleVO> added =
          promotionRuleService.addRule(buildAddForm("TOG001", "切換測試"));
      Long ruleId = added.getData().getRuleId();

      ResponseDTO<Void> result =
          promotionRuleService.toggleStatus(ruleId, PromotionStatusEnum.PAUSED.getValue());

      assertThat(result.getOk()).isTrue();
      clearCaches();
      var rule = promotionRuleService.getRule(ruleId);
      assertThat(rule.get().getStatus()).isEqualTo(PromotionStatusEnum.PAUSED.getValue());
    }

    @Test
    @DisplayName("查詢不存在規則 — 返回 None")
    void getRule_notFound() {
      var result = promotionRuleService.getRule(999999L);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ==================== Bonus Claim ====================

  @Nested
  @DisplayName("bonusClaim 紅利領取整合測試")
  class BonusClaimIntegrationTest {

    @Test
    @DisplayName("領取紅利成功 — 建立記錄 + 入款 BONUS 錢包")
    void claim_success_creates_record_and_credits_wallet() {
      createBonusWallet("0.0000");
      createActiveRule("CLAIM001", new BigDecimal("50.0000"), new BigDecimal("10.00"));

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("CLAIM001");
      form.setClaimId("claim-it-001");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(PLAYER_ID, form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBonusAmount()).isEqualByComparingTo("50.0000");
      // wagering = 50 * 10 = 500
      assertThat(result.getData().getWageringRequired()).isEqualByComparingTo("500.0000");
      assertThat(result.getData().getStatus()).isEqualTo(BonusRecordStatusEnum.ACTIVE.getValue());

      // Verify BONUS wallet balance
      WalletEntity wallet = findBonusWallet();
      assertThat(wallet.getBalance()).isEqualByComparingTo("50.0000");
    }

    @Test
    @DisplayName("冪等重複領取 — 返回已領取錯誤")
    void claim_idempotent_duplicate() {
      createBonusWallet("0.0000");
      createActiveRule("IDEM001", new BigDecimal("50.0000"), new BigDecimal("10.00"));

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("IDEM001");
      form.setClaimId("claim-it-idem");

      ResponseDTO<BonusClaimResultVO> first = bonusClaimService.claimBonus(PLAYER_ID, form);
      assertThat(first.getOk()).isTrue();

      // Second claim with same claimId → rejected
      clearCaches();
      ResponseDTO<BonusClaimResultVO> second = bonusClaimService.claimBonus(PLAYER_ID, form);
      assertThat(second.getOk()).isFalse();
    }

    @Test
    @DisplayName("促銷已暫停 — 拒絕")
    void claim_promotionDisabled_rejected() {
      createBonusWallet("0.0000");
      PromotionRuleEntity rule =
          createActiveRule("DIS001", new BigDecimal("50.0000"), new BigDecimal("10.00"));
      rule.setStatus(PromotionStatusEnum.PAUSED.getValue());
      promotionRuleDao.updateById(rule);
      clearCaches();

      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("DIS001");
      form.setClaimId("claim-it-dis");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(PLAYER_ID, form);
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("促銷不存在 — 拒絕")
    void claim_promotionNotFound_rejected() {
      BonusClaimForm form = new BonusClaimForm();
      form.setPromotionCode("NOTEXIST");
      form.setClaimId("claim-it-notfound");

      ResponseDTO<BonusClaimResultVO> result = bonusClaimService.claimBonus(PLAYER_ID, form);
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Wagering Progress ====================

  @Nested
  @DisplayName("wageringProgress 流水進度整合測試")
  class WageringProgressIntegrationTest {

    @Test
    @DisplayName("更新流水進度 — 未達標仍為 ACTIVE")
    void updateProgress_notCompleted() {
      createBonusWallet("0.0000");
      createActiveRule("WP001", new BigDecimal("100.0000"), new BigDecimal("10.00"));
      // wagering required = 100 * 10 = 1000

      BonusClaimForm claimForm = new BonusClaimForm();
      claimForm.setPromotionCode("WP001");
      claimForm.setClaimId("claim-wp-001");
      ResponseDTO<BonusClaimResultVO> claimed = bonusClaimService.claimBonus(PLAYER_ID, claimForm);
      Long recordId = claimed.getData().getRecordId();

      insertGameProvider();
      insertGame();

      int updated =
          wageringProgressManager.updateWageringProgress(
              PLAYER_ID, "slot-001", new BigDecimal("200.0000"), TEST_TENANT_ID);

      assertThat(updated).isEqualTo(1);

      PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
      assertThat(record.getWageringCompleted()).isEqualByComparingTo("200.0000");
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.ACTIVE.getValue());
    }

    @Test
    @DisplayName("流水完成 — 自動標記 COMPLETED")
    void updateProgress_completed() {
      createBonusWallet("0.0000");
      createActiveRule("WP002", new BigDecimal("10.0000"), new BigDecimal("2.00"));
      // wagering required = 10 * 2 = 20

      BonusClaimForm claimForm = new BonusClaimForm();
      claimForm.setPromotionCode("WP002");
      claimForm.setClaimId("claim-wp-002");
      ResponseDTO<BonusClaimResultVO> claimed = bonusClaimService.claimBonus(PLAYER_ID, claimForm);
      Long recordId = claimed.getData().getRecordId();

      insertGameProvider();
      insertGame();

      // Bet enough to exceed wagering requirement (20)
      wageringProgressManager.updateWageringProgress(
          PLAYER_ID, "slot-001", new BigDecimal("25.0000"), TEST_TENANT_ID);

      PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.COMPLETED.getValue());
      assertThat(record.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("無有效紅利時更新 — 返回 0")
    void updateProgress_noActiveBonus() {
      insertGameProvider();
      insertGame();

      int updated =
          wageringProgressManager.updateWageringProgress(
              PLAYER_ID, "slot-001", new BigDecimal("100.0000"), TEST_TENANT_ID);

      assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("查詢流水進度")
    void getWageringProgress() {
      createBonusWallet("0.0000");
      createActiveRule("WP003", new BigDecimal("100.0000"), new BigDecimal("10.00"));
      // wagering required = 1000

      BonusClaimForm claimForm = new BonusClaimForm();
      claimForm.setPromotionCode("WP003");
      claimForm.setClaimId("claim-wp-003");
      ResponseDTO<BonusClaimResultVO> claimed = bonusClaimService.claimBonus(PLAYER_ID, claimForm);
      Long recordId = claimed.getData().getRecordId();

      insertGameProvider();
      insertGame();

      wageringProgressManager.updateWageringProgress(
          PLAYER_ID, "slot-001", new BigDecimal("250.0000"), TEST_TENANT_ID);

      io.vavr.control.Option<WageringProgressVO> progress =
          bonusClaimService.getWageringProgress(PLAYER_ID, recordId);

      assertThat(progress.isDefined()).isTrue();
      assertThat(progress.get().getWageringRequired()).isEqualByComparingTo("1000.0000");
      assertThat(progress.get().getWageringCompleted()).isEqualByComparingTo("250.0000");
      // 250/1000 * 100 = 25.00%
      assertThat(progress.get().getProgressPercent()).isEqualByComparingTo("25.00");
    }
  }

  // ==================== Bonus Lifecycle ====================

  @Nested
  @DisplayName("bonusLifecycle 紅利生命週期整合測試")
  class BonusLifecycleIntegrationTest {

    @Test
    @DisplayName("沒收紅利 — 扣減 BONUS 錢包")
    void forfeitBonus_deductsWallet() {
      createBonusWallet("0.0000");
      createActiveRule("FORF001", new BigDecimal("100.0000"), new BigDecimal("10.00"));

      BonusClaimForm claimForm = new BonusClaimForm();
      claimForm.setPromotionCode("FORF001");
      claimForm.setClaimId("claim-forf-001");
      ResponseDTO<BonusClaimResultVO> claimed = bonusClaimService.claimBonus(PLAYER_ID, claimForm);
      Long recordId = claimed.getData().getRecordId();

      // Verify wallet was credited
      assertThat(findBonusWallet().getBalance()).isEqualByComparingTo("100.0000");

      // Forfeit the bonus
      bonusLifecycleManager.forfeitBonus(recordId);

      // Verify record status
      PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.FORFEITED.getValue());

      // Verify wallet balance deducted
      WalletEntity wallet = findBonusWallet();
      assertThat(wallet.getBalance()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("過期紅利批次清理")
    void expireActiveBonuses() {
      createBonusWallet("0.0000");
      // Create rule that yields an already-expired bonus
      PromotionRuleEntity rule =
          createActiveRule("EXP001", new BigDecimal("50.0000"), new BigDecimal("5.00"));
      rule.setBonusExpiryDays(0);
      promotionRuleDao.updateById(rule);
      clearCaches();

      BonusClaimForm claimForm = new BonusClaimForm();
      claimForm.setPromotionCode("EXP001");
      claimForm.setClaimId("claim-exp-001");
      ResponseDTO<BonusClaimResultVO> claimed = bonusClaimService.claimBonus(PLAYER_ID, claimForm);
      Long recordId = claimed.getData().getRecordId();

      // Manually set expired_at to past so selectExpiredActive picks it up
      jdbcTemplate.update(
          "UPDATE t_player_bonus_record SET expired_at = NOW() - INTERVAL '1 day'"
              + " WHERE record_id = ?",
          recordId);

      int expired = bonusLifecycleManager.expireActiveBonuses(100);

      assertThat(expired).isEqualTo(1);
      PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.EXPIRED.getValue());
    }
  }

  // ==================== Helpers ====================

  private PromotionRuleAddForm buildAddForm(String code, String name) {
    PromotionRuleAddForm form = new PromotionRuleAddForm();
    form.setPromotionCode(code);
    form.setPromotionName(name);
    form.setPromotionType(1);
    form.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    form.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    form.setMinDeposit(BigDecimal.ZERO);
    form.setBonusRate(new BigDecimal("1.0000"));
    form.setMaxBonus(new BigDecimal("100.0000"));
    form.setWageringMultiplier(new BigDecimal("10.00"));
    form.setMaxClaimsPerPlayer(1);
    form.setBonusExpiryDays(30);
    return form;
  }

  private PromotionRuleEntity createActiveRule(
      String code, BigDecimal maxBonus, BigDecimal wageringMultiplier) {
    PromotionRuleEntity entity = new PromotionRuleEntity();
    entity.setTenantId(TEST_TENANT_ID);
    entity.setPromotionCode(code);
    entity.setPromotionName("Test " + code);
    entity.setPromotionType(1);
    entity.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    entity.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    entity.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
    entity.setMinDeposit(BigDecimal.ZERO);
    entity.setBonusRate(new BigDecimal("1.0000"));
    entity.setMaxBonus(maxBonus);
    entity.setWageringMultiplier(wageringMultiplier);
    entity.setMaxClaimsPerPlayer(1);
    entity.setBonusExpiryDays(30);
    entity.setDeleted(false);
    promotionRuleDao.insert(entity);
    return entity;
  }

  private Long createBonusWallet(String balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setPlayerId(PLAYER_ID);
    wallet.setTenantId(TEST_TENANT_ID);
    wallet.setCurrencyCode("USD");
    wallet.setWalletType(WalletTypeEnum.BONUS.getValue());
    wallet.setBalance(new BigDecimal(balance));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setVersion(0);
    wallet.setDeleted(false);
    walletDao.insert(wallet);
    return wallet.getWalletId();
  }

  private WalletEntity findBonusWallet() {
    return walletDao.selectOne(
        Wrappers.<WalletEntity>lambdaQuery()
            .eq(WalletEntity::getPlayerId, PLAYER_ID)
            .eq(WalletEntity::getWalletType, WalletTypeEnum.BONUS.getValue())
            .eq(WalletEntity::getDeleted, false));
  }

  private void insertGameProvider() {
    jdbcTemplate.execute(
        "INSERT INTO t_game_provider (tenant_id, provider_code, provider_name, api_url,"
            + " encrypted_api_key, enabled, health_status, deleted, version, create_time,"
            + " update_time) VALUES (1, 'mock', 'Mock Provider', 'https://mock.api.com',"
            + " 'test-key', true, 1, false, 0, NOW(), NOW())"
            + " ON CONFLICT DO NOTHING");
  }

  private void insertGame() {
    jdbcTemplate.execute(
        "INSERT INTO t_game (provider_id, tenant_id, game_code, game_name, category,"
            + " thumbnail_url, play_count, enabled, deleted, version, create_time, update_time)"
            + " VALUES ((SELECT provider_id FROM t_game_provider WHERE provider_code='mock'"
            + " LIMIT 1), 1, 'slot-001', 'Test Slot', 1, 'https://img.test/slot.png', 0, true,"
            + " false, 0, NOW(), NOW())"
            + " ON CONFLICT DO NOTHING");
  }

  private void clearCaches() {
    cacheManager
        .getCacheNames()
        .forEach(
            name -> {
              var cache = cacheManager.getCache(name);
              if (cache != null) {
                cache.clear();
              }
            });
  }
}
