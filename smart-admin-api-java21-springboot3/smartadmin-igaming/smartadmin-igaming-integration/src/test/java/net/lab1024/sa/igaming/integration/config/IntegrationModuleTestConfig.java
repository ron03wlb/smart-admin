package net.lab1024.sa.igaming.integration.config;

import net.lab1024.sa.common.security.config.Argon2Properties;
import net.lab1024.sa.common.security.config.FieldEncryptAutoConfiguration;
import net.lab1024.sa.common.security.encrypt.FieldEncryptProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Test configuration for iGaming Integration module integration tests.
 *
 * <p>This configuration class enables component scanning for the integration module and all
 * dependent modules (Player, Wallet, Activity, Game, Risk) to provide a complete test context for
 * end-to-end player journey tests.
 *
 * <p><b>Scanned Modules:</b>
 *
 * <ul>
 *   <li>{@code integration} - Integration orchestration services (PlayerRegistration,
 *       FirstDepositBonus, WithdrawalRiskCheck, WithdrawalApproval)
 *   <li>{@code player} - Player registration, authentication, KYC services
 *   <li>{@code wallet} - Wallet management, payment processing services
 *   <li>{@code activity} - Bonus distribution, promotion rule services
 *   <li>{@code game} - Game provider adapter services
 *   <li>{@code risk} - Risk assessment, risk proposal services
 *   <li>{@code common} - Shared constants, domain events
 * </ul>
 *
 * <p><b>MyBatis Mappers:</b> Scans all Dao interfaces across iGaming modules (player, wallet,
 * activity, game, risk).
 *
 * <p><b>Test Beans:</b> Imports {@link TestKafkaConfig} to provide mock Kafka beans
 * (DomainEventPublisher) when Kafka is disabled in test profile.
 *
 * <p><b>Usage:</b> Used by {@link net.lab1024.sa.igaming.integration.journey.BaseIntegrationTest}
 * to bootstrap Spring context for integration tests with Testcontainers.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@SpringBootApplication(
    scanBasePackages = {
      "net.lab1024.sa.igaming.integration",
      "net.lab1024.sa.igaming.player",
      "net.lab1024.sa.igaming.wallet",
      "net.lab1024.sa.igaming.activity",
      "net.lab1024.sa.igaming.game",
      "net.lab1024.sa.igaming.risk",
      "net.lab1024.sa.igaming.common",
      "net.lab1024.sa.common.mybatis",
      "net.lab1024.sa.common.mq",
      "net.lab1024.sa.common.tenant",
      "net.lab1024.sa.common.redislock",
      "net.lab1024.sa.common.redis",
      "net.lab1024.sa.common.cache",
      "net.lab1024.sa.common.security",
      "net.lab1024.sa.common.token",
      "net.lab1024.sa.common.json",
      "net.lab1024.sa.support.liteflow"
    })
@EnableConfigurationProperties({Argon2Properties.class, FieldEncryptProperties.class})
@Import({TestKafkaConfig.class, TestSecurityConfig.class, FieldEncryptAutoConfiguration.class})
@MapperScan(
    basePackages = {
      "net.lab1024.sa.igaming.player.dao",
      "net.lab1024.sa.igaming.wallet.dao",
      "net.lab1024.sa.igaming.wallet.payment.dao",
      "net.lab1024.sa.igaming.activity.dao",
      "net.lab1024.sa.igaming.activity.turnover.dao",
      "net.lab1024.sa.igaming.game.dao",
      "net.lab1024.sa.igaming.risk.dao",
      "net.lab1024.sa.support.liteflow.dao",
      "net.lab1024.sa.common.tenant.dao"
    })
public class IntegrationModuleTestConfig {
  // Empty configuration class - Spring Boot will auto-configure based on classpath
}
