package net.lab1024.sa.common.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Argon2id password hashing configuration properties.
 *
 * <p>Defaults match Spring Security 5.8 ({@code m=16384, t=2, p=1}). Override via {@code
 * smart.security.argon2.*} in application.yaml for stronger parameters (e.g., iGaming compliance:
 * {@code m=65536, t=3, p=4}).
 *
 * @author SmartAdmin
 * @since 2026-02-14
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "smart.security.argon2")
public class Argon2Properties {

  /** Salt length in bytes (default: 16) */
  private int saltLength = 16;

  /** Hash length in bytes (default: 32) */
  private int hashLength = 32;

  /** Parallelism / number of threads (default: 1, iGaming: 4) */
  private int parallelism = 1;

  /** Memory cost in KiB (default: 16384 = 16 MB, iGaming: 65536 = 64 MB) */
  private int memory = 16384;

  /** Number of iterations / time cost (default: 2, iGaming: 3) */
  private int iterations = 2;
}
