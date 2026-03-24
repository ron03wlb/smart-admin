package net.lab1024.sa.igaming.integration.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import jakarta.annotation.PostConstruct;
import net.lab1024.sa.common.core.domain.SystemEnvironment;
import net.lab1024.sa.common.core.enumeration.SystemEnvironmentEnum;
import net.lab1024.sa.common.token.player.StpPlayerLogic;
import net.lab1024.sa.common.token.player.StpPlayerUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for security-related beans.
 *
 * <p>Provides mock implementations of security beans when running integration tests. This allows
 * tests to run without real encryption keys while still verifying business logic.
 *
 * <p><b>Provided Mock Beans:</b>
 *
 * <ul>
 *   <li>{@link BlindIndexService} - Mock blind index service using SHA-256 instead of HMAC-SHA256
 *       (no secret key required in tests)
 * </ul>
 *
 * <p><b>Real Beans (auto-configured):</b>
 *
 * <ul>
 *   <li>{@code PasswordEncryptService} - Uses real Argon2id implementation with test configuration
 *       from {@code application-test.yml} ({@code smart.security.argon2.*})
 * </ul>
 *
 * <p><b>Usage:</b> This configuration is automatically loaded by {@link
 * IntegrationModuleTestConfig} via {@code @Import(TestSecurityConfig.class)}.
 *
 * <p><b>Mock Strategy:</b> The mock BlindIndexService produces deterministic but non-cryptographic
 * hashes (SHA-256 of plaintext) that are sufficient for testing blind index lookups. Real
 * production deployments use HMAC-SHA256 with a secret key.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@TestConfiguration
public class TestSecurityConfig {

  /**
   * Initialize mock Sa-Token player authentication after Spring context is ready.
   *
   * <p>This method replaces the static {@code StpPlayerUtil.stpPlayerLogic} with a mock
   * implementation that bypasses actual Sa-Token context requirements. This allows integration
   * tests to call {@code PlayerAuthService.register()} which internally calls {@code
   * StpPlayerUtil.login()} without needing a fully initialized Sa-Token web environment.
   *
   * <p>Mock behavior:
   *
   * <ul>
   *   <li>{@code login(playerId)} - No-op (does not store session)
   *   <li>{@code getTokenValue()} - Returns mock token "test-player-token-{playerId}"
   *   <li>{@code isLogin()} - Always returns {@code true}
   *   <li>{@code getLoginId()} - Returns the mocked player ID
   * </ul>
   *
   * <p>This approach is necessary because integration tests directly invoke service methods rather
   * than making HTTP requests through controllers, so Sa-Token's filter chain never executes to
   * initialize the context.
   */
  @PostConstruct
  public void initializeMockStpPlayerUtil() {
    // Create a mock StpPlayerLogic for player authentication
    StpPlayerLogic mockStpPlayerLogic = mock(StpPlayerLogic.class);

    // Mock login() to do nothing (bypass context initialization)
    doNothing().when(mockStpPlayerLogic).login(any());
    doNothing().when(mockStpPlayerLogic).login(any(), any(Long.class));

    // Mock getTokenValue() to return a test token
    when(mockStpPlayerLogic.getTokenValue()).thenReturn("test-player-token-12345");

    // Mock isLogin() to always return true
    when(mockStpPlayerLogic.isLogin()).thenReturn(true);

    // Mock getLoginId() to return a test player ID
    when(mockStpPlayerLogic.getLoginId()).thenReturn(12345L);
    when(mockStpPlayerLogic.getLoginIdAsLong()).thenReturn(12345L);

    // Replace the static stpPlayerLogic field with our mock
    StpPlayerUtil.stpPlayerLogic = mockStpPlayerLogic;
  }

  /**
   * Provides Sa-Token context for tests.
   *
   * <p>Sa-Token requires a SaTokenContext to be initialized before any login operations. In test
   * environment, we use ThreadLocal-based context.
   *
   * <p>This bean is created early in the Spring context lifecycle to ensure it's available before
   * any service methods that use Sa-Token authentication.
   */
  @Bean
  @Primary
  public SaTokenContext saTokenContext() {
    SaTokenContext context = new SaTokenContextForThreadLocal();
    SaManager.setSaTokenContext(context);
    return context;
  }

  /**
   * Provides a mock SystemEnvironment for tests.
   *
   * <p>This avoids the need for {@code spring.profiles.active} and {@code project.name} properties
   * in test configuration, which can conflict with {@code @ActiveProfiles("test")}.
   *
   * <p>Sets environment to TEST mode with a test project name.
   *
   * @return mock SystemEnvironment instance for tests
   */
  @Bean("systemEnvironment")
  @Primary
  public SystemEnvironment mockSystemEnvironment() {
    return new SystemEnvironment(
        false, // isProduction = false (test environment)
        "smartadmin-igaming-integration-test", // projectName
        SystemEnvironmentEnum.TEST // currentEnvironment
        );
  }

  /**
   * Provides a mock FlowExecutor for tests.
   *
   * <p>This mock implementation bypasses LiteFlow initialization when smart.liteflow.enabled=false.
   * Tests that need actual LiteFlow functionality should enable it in their test profile.
   *
   * <p><b>Why Mock Instead of Real?</b>
   *
   * <ul>
   *   <li>Avoids LiteFlow SQL parser initialization errors when smart.liteflow.enabled=false
   *   <li>Integration tests focus on Player/Wallet/Bonus flow, not LiteFlow rules
   *   <li>LiteFlow functionality is tested separately in TurnoverCalculationIntegrationTest
   * </ul>
   *
   * @return mock FlowExecutor instance using Mockito
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "smart.liteflow",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = false)
  public FlowExecutor mockFlowExecutor() {
    FlowExecutor mockExecutor = mock(FlowExecutor.class);

    // Setup default behavior for execute2Resp
    LiteflowResponse mockResponse = new LiteflowResponse();
    mockResponse.setSuccess(true);
    mockResponse.setMessage("Mock FlowExecutor - LiteFlow disabled for tests");

    when(mockExecutor.execute2Resp(anyString(), any(), any())).thenReturn(mockResponse);

    return mockExecutor;
  }

  /**
   * Provides a mock SecurityConfigProvider for tests.
   *
   * <p>This mock implementation:
   *
   * <ul>
   *   <li>Provides permissive test-friendly security settings
   *   <li>Disables two-factor authentication (for test simplicity)
   *   <li>Allows unlimited login attempts (no lockout in tests)
   *   <li>Disables password complexity requirements
   *   <li>Sets reasonable timeout values for test execution
   * </ul>
   *
   * <p><b>Why Mock Instead of Real?</b>
   *
   * <ul>
   *   <li>Tests don't need production-level security enforcement
   *   <li>Simplifies test setup (no login failures, no password expiry)
   *   <li>Consistent across all test environments
   * </ul>
   *
   * <p><b>Production Difference:</b> Production uses real SecurityConfigProvider with strict
   * three-level security settings (三级等保). Test environment uses permissive mock settings.
   *
   * @return mock SecurityConfigProvider instance
   */
  @Bean
  @Primary
  public net.lab1024.sa.common.security.service.SecurityConfigProvider
      mockSecurityConfigProvider() {
    return new net.lab1024.sa.common.security.service.SecurityConfigProvider() {
      @Override
      public boolean isTwoFactorLoginEnabled() {
        return false; // Disabled for tests
      }

      @Override
      public int getLoginFailMaxTimes() {
        return -1; // Unlimited attempts (no lockout in tests)
      }

      @Override
      public int getLoginFailLockSeconds() {
        return -1; // No lockout
      }

      @Override
      public boolean isPasswordComplexityEnabled() {
        return false; // Disabled for tests
      }

      @Override
      public int getRegularChangePasswordDays() {
        return 365; // 1 year (effectively disabled for tests)
      }

      @Override
      public int getRegularChangePasswordNotAllowRepeatTimes() {
        return 0; // No password history check
      }

      @Override
      public boolean isFileDetectEnabled() {
        return false; // Disabled for tests
      }

      @Override
      public long getMaxUploadFileSizeMb() {
        return 100; // 100MB (generous limit for tests)
      }

      @Override
      public int getLoginActiveTimeoutSeconds() {
        return 7200; // 2 hours (long timeout for test debugging)
      }
    };
  }
}
