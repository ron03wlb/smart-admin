package net.lab1024.sa.igaming.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * iGaming configuration properties — externalized from hardcoded constants.
 *
 * <p>Prefix: {@code igaming}. Example:
 *
 * <pre>
 * igaming:
 *   lock:
 *     wait-ms: 3000
 *     lease-ms: 10000
 *   wallet:
 *     default-currency: USD
 * </pre>
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "igaming")
public class IgamingProperties {

  private Lock lock = new Lock();
  private Wallet wallet = new Wallet();

  /** Distributed lock configuration. */
  @Data
  public static class Lock {
    /** Lock wait (acquire) timeout in milliseconds. */
    private long waitMs = 3000;

    /** Lock lease (hold) time in milliseconds. */
    private long leaseMs = 10000;
  }

  /** Wallet module configuration. */
  @Data
  public static class Wallet {
    /** Default currency code for new wallets (ISO 4217). */
    private String defaultCurrency = "USD";
  }
}
