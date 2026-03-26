package net.lab1024.sa.igaming.integration.journey;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionConfigDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionSettlementDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentRelationshipDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 *
 * <p>Provides shared configuration for PostgreSQL 16, Redis 7, and Spring Boot test context. All
 * integration tests should extend this class to ensure consistent test environment.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>PostgreSQL 16-alpine container with auto-start lifecycle
 *   <li>Redis 7-alpine container for LockService and caching
 *   <li>Dynamic datasource and Redis configuration via {@code @DynamicPropertySource}
 *   <li>Spring Boot test context with {@code test} profile
 *   <li>Container reuse across test classes (via {@code testcontainers.properties})
 *   <li>Full integration module component scanning (Player, Wallet, Activity, Game, Risk modules
 *       loaded)
 * </ul>
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * &#64;DisplayName("Player Registration Journey Integration Test")
 * class PlayerRegistrationJourneyIntegrationTest extends BaseIntegrationTest {
 *
 *     &#64;Autowired private PlayerRegistrationIntegrationService service;
 *
 *     &#64;Test
 *     void shouldRegisterPlayerWithWallets() {
 *         // Test implementation...
 *     }
 * }
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@SpringBootTest(
    classes = {net.lab1024.sa.igaming.integration.config.IntegrationModuleTestConfig.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @Autowired private PlayerDao playerDao;

  @Autowired private WalletDao walletDao;

  @Autowired private PlayerBonusRecordDao bonusRecordDao;

  @Autowired private net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao walletBonusExtDao;

  @Autowired private net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao walletTransactionDao;

  @Autowired private net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao paymentOrderDao;

  @Autowired private AgentRelationshipDao agentRelationshipDao;

  @Autowired private AgentCommissionRecordDao agentCommissionRecordDao;

  @Autowired private AgentCommissionSettlementDao agentCommissionSettlementDao;

  @Autowired private AgentCommissionConfigDao agentCommissionConfigDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * Clean up test data before each test to ensure clean state.
   *
   * <p>This method runs BEFORE each test method to delete any leftover data from previous test
   * runs. This ensures each test starts with a completely clean database state, preventing "Wallet
   * already exists" errors.
   *
   * <p>Note: This runs in addition to {@link #cleanupTestData()} which runs after each test. The
   * before-cleanup handles leftover data from aborted test runs or previous sessions, while the
   * after-cleanup handles data from the current test.
   *
   * <p><b>IMPORTANT:</b> Sets TenantContext to tenant 1 before cleanup to ensure tenant-filtered
   * deletes work correctly with MyBatis tenant interceptor.
   */
  @BeforeEach
  void setupCleanDatabase() {
    // Set tenant context to enable tenant-filtered deletes
    TenantContext.setTenantId(1L);
    System.out.println("[@BeforeEach] Cleaning database for tenant " + TenantContext.getTenantId());

    // Count existing data before cleanup
    long paymentOrderCountBefore =
        paymentOrderDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity>());
    long transactionCountBefore =
        walletTransactionDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity>());
    long bonusExtCountBefore =
        walletBonusExtDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity>());
    long bonusCountBefore =
        bonusRecordDao.selectCount(new LambdaQueryWrapper<PlayerBonusRecordEntity>());
    long walletCountBefore = walletDao.selectCount(new LambdaQueryWrapper<WalletEntity>());
    long playerCountBefore = playerDao.selectCount(new LambdaQueryWrapper<PlayerEntity>());
    System.out.println(
        "[@BeforeEach] Before cleanup: "
            + paymentOrderCountBefore
            + " payment orders, "
            + transactionCountBefore
            + " transactions, "
            + bonusExtCountBefore
            + " bonus ext records, "
            + bonusCountBefore
            + " bonus records, "
            + walletCountBefore
            + " wallets, "
            + playerCountBefore
            + " players");

    // ✅ FIXED: Clean up payment orders first (foreign key to wallets and players)
    int paymentOrdersDeleted =
        paymentOrderDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity>());
    System.out.println("[@BeforeEach] Deleted " + paymentOrdersDeleted + " payment orders");

    // Then clean up wallet transactions (foreign key to wallets)
    int transactionsDeleted =
        walletTransactionDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity>());
    System.out.println("[@BeforeEach] Deleted " + transactionsDeleted + " wallet transactions");

    // Then clean up wallet bonus ext (foreign key to bonus records)
    int bonusExtDeleted =
        walletBonusExtDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity>());
    System.out.println("[@BeforeEach] Deleted " + bonusExtDeleted + " wallet bonus ext records");

    // Then clean up bonus records (foreign key to players)
    int bonusDeleted = bonusRecordDao.delete(new LambdaQueryWrapper<PlayerBonusRecordEntity>());
    System.out.println("[@BeforeEach] Deleted " + bonusDeleted + " bonus records");

    // Then clean up wallets (foreign key constraint to players)
    int walletsDeleted = walletDao.delete(new LambdaQueryWrapper<WalletEntity>());
    System.out.println("[@BeforeEach] Deleted " + walletsDeleted + " wallets");

    // ✅ CRITICAL FIX: Physically delete agent tables (bypass @TableLogic soft delete)
    // Agent entities use @TableLogic, so dao.delete() only soft-deletes records.
    // For test cleanup, we need physical deletion to avoid FK constraint violations.
    int settlementDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_settlement WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + settlementDeleted + " commission settlements");

    int recordDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_record WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + recordDeleted + " commission records");

    int relationshipDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_relationship WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + relationshipDeleted + " agent relationships");

    int configDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_config WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + configDeleted + " commission configs");

    // ✅ CRITICAL FIX: Physically delete VIP tables (bypass @TableLogic soft delete)
    // VIP entities use @TableLogic, so dao.delete() only soft-deletes records.
    // For test cleanup, we need physical deletion to avoid FK constraint violations.
    int vipRewardDeleted =
        jdbcTemplate.update("DELETE FROM t_vip_reward_record WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + vipRewardDeleted + " VIP reward records");

    int vipHistoryDeleted =
        jdbcTemplate.update("DELETE FROM t_player_vip_history WHERE tenant_id = 1");
    System.out.println("[@BeforeEach] Deleted " + vipHistoryDeleted + " VIP history records");

    // Finally clean up players (parent table)
    int playersDeleted = playerDao.delete(new LambdaQueryWrapper<PlayerEntity>());
    System.out.println("[@BeforeEach] Deleted " + playersDeleted + " players");
  }

  /**
   * Clean up test data after each test to prevent conflicts.
   *
   * <p>This method deletes all wallets and players created during the test, ensuring each test
   * starts with a clean database state. This prevents "Wallet already exists" errors when multiple
   * tests create the same player/wallet combinations.
   *
   * <p>Note: This approach is preferred over {@code @Transactional} with rollback because Kafka
   * event consumers need committed data to process events properly.
   *
   * <p><b>IMPORTANT:</b> Sets TenantContext to tenant 1 before cleanup to ensure tenant-filtered
   * deletes work correctly with MyBatis tenant interceptor.
   */
  @AfterEach
  void cleanupTestData() {
    // Set tenant context to enable tenant-filtered deletes
    TenantContext.setTenantId(1L);
    System.out.println("[@AfterEach] Cleaning database for tenant " + TenantContext.getTenantId());

    // Count existing data before cleanup
    long paymentOrderCountBefore =
        paymentOrderDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity>());
    long transactionCountBefore =
        walletTransactionDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity>());
    long bonusExtCountBefore =
        walletBonusExtDao.selectCount(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity>());
    long bonusCountBefore =
        bonusRecordDao.selectCount(new LambdaQueryWrapper<PlayerBonusRecordEntity>());
    long walletCountBefore = walletDao.selectCount(new LambdaQueryWrapper<WalletEntity>());
    long playerCountBefore = playerDao.selectCount(new LambdaQueryWrapper<PlayerEntity>());
    System.out.println(
        "[@AfterEach] Before cleanup: "
            + paymentOrderCountBefore
            + " payment orders, "
            + transactionCountBefore
            + " transactions, "
            + bonusExtCountBefore
            + " bonus ext records, "
            + bonusCountBefore
            + " bonus records, "
            + walletCountBefore
            + " wallets, "
            + playerCountBefore
            + " players");

    // ✅ FIXED: Clean up payment orders first (foreign key to wallets and players)
    int paymentOrdersDeleted =
        paymentOrderDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity>());
    System.out.println("[@AfterEach] Deleted " + paymentOrdersDeleted + " payment orders");

    // Then clean up wallet transactions (foreign key to wallets)
    int transactionsDeleted =
        walletTransactionDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity>());
    System.out.println("[@AfterEach] Deleted " + transactionsDeleted + " wallet transactions");

    // Then clean up wallet bonus ext (foreign key to bonus records)
    int bonusExtDeleted =
        walletBonusExtDao.delete(
            new LambdaQueryWrapper<
                net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity>());
    System.out.println("[@AfterEach] Deleted " + bonusExtDeleted + " wallet bonus ext records");

    // Then clean up bonus records (foreign key to players)
    int bonusDeleted = bonusRecordDao.delete(new LambdaQueryWrapper<PlayerBonusRecordEntity>());
    System.out.println("[@AfterEach] Deleted " + bonusDeleted + " bonus records");

    // Then clean up wallets (foreign key constraint to players)
    int walletsDeleted = walletDao.delete(new LambdaQueryWrapper<WalletEntity>());
    System.out.println("[@AfterEach] Deleted " + walletsDeleted + " wallets");

    // ✅ CRITICAL FIX: Physically delete agent tables (bypass @TableLogic soft delete)
    // Agent entities use @TableLogic, so dao.delete() only soft-deletes records.
    // For test cleanup, we need physical deletion to avoid FK constraint violations.
    int settlementDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_settlement WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + settlementDeleted + " commission settlements");

    int recordDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_record WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + recordDeleted + " commission records");

    int relationshipDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_relationship WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + relationshipDeleted + " agent relationships");

    int configDeleted =
        jdbcTemplate.update("DELETE FROM t_agent_commission_config WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + configDeleted + " commission configs");

    // ✅ CRITICAL FIX: Physically delete VIP tables (bypass @TableLogic soft delete)
    // VIP entities use @TableLogic, so dao.delete() only soft-deletes records.
    // For test cleanup, we need physical deletion to avoid FK constraint violations.
    int vipRewardDeleted =
        jdbcTemplate.update("DELETE FROM t_vip_reward_record WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + vipRewardDeleted + " VIP reward records");

    int vipHistoryDeleted =
        jdbcTemplate.update("DELETE FROM t_player_vip_history WHERE tenant_id = 1");
    System.out.println("[@AfterEach] Deleted " + vipHistoryDeleted + " VIP history records");

    // Finally clean up players (parent table)
    int playersDeleted = playerDao.delete(new LambdaQueryWrapper<PlayerEntity>());
    System.out.println("[@AfterEach] Deleted " + playersDeleted + " players");

    // Clear tenant context after cleanup
    TenantContext.clear();
  }

  /**
   * PostgreSQL 16 database container shared across all integration tests.
   *
   * <p>Container lifecycle:
   *
   * <ul>
   *   <li>Started once before first test execution
   *   <li>Reused across test classes (if {@code testcontainers.reuse.enable=true})
   *   <li>Stopped after JVM shutdown
   * </ul>
   *
   * <p>Database credentials:
   *
   * <ul>
   *   <li>Database name: {@code testdb}
   *   <li>Username: {@code test}
   *   <li>Password: {@code test}
   * </ul>
   */
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test")
          .withReuse(true); // ✅ CRITICAL FIX: Enable container reuse to prevent port changes

  /**
   * Redis 7 container shared across all integration tests.
   *
   * <p>Required for:
   *
   * <ul>
   *   <li>{@code LockService} - Distributed locking (Redisson)
   *   <li>{@code CacheManager} - Spring Cache abstraction
   *   <li>{@code WalletService} - Idempotency checks (requestId)
   * </ul>
   *
   * <p>Container lifecycle:
   *
   * <ul>
   *   <li>Started once before first test execution
   *   <li>Reused across test classes (if {@code testcontainers.reuse.enable=true})
   *   <li>Stopped after JVM shutdown
   * </ul>
   */
  @Container
  @SuppressWarnings("resource")
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379)
          .withReuse(true); // ✅ CRITICAL FIX: Enable container reuse to prevent port changes

  /**
   * Configure Spring Boot datasource and Redis properties dynamically from Testcontainers.
   *
   * <p>Overrides {@code application-test.yml} configuration with actual container connection
   * details.
   *
   * @param registry Spring Boot dynamic property registry
   */
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL datasource
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // Redis connection
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);

    // Redisson configuration (for LockService)
    registry.add(
        "redisson.config",
        () ->
            String.format(
                "singleServerConfig:\n  address: \"redis://%s:%d\"\n  database: 0",
                redis.getHost(), redis.getFirstMappedPort()));

    // LiteFlow database configuration (CRITICAL: Must match Testcontainers PostgreSQL)
    registry.add("liteflow.rule-source-ext-data-map.url", postgres::getJdbcUrl);
    registry.add("liteflow.rule-source-ext-data-map.username", postgres::getUsername);
    registry.add("liteflow.rule-source-ext-data-map.password", postgres::getPassword);
  }
}
