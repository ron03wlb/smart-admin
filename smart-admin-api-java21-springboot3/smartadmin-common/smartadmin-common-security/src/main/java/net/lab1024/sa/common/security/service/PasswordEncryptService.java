package net.lab1024.sa.common.security.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lab1024.sa.common.security.config.Argon2Properties;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码加密服务
 *
 * <p>提供基于 Argon2id 的密码加密和验证功能。Argon2 是目前最安全的密码哈希算法之一， 已被选为密码哈希竞赛（Password Hashing Competition）的获胜者。
 *
 * <p>Parameters are configurable via {@code smart.security.argon2.*} in application.yaml. Defaults
 * match Spring Security 5.8 ({@code m=16384, t=2, p=1}). For iGaming compliance (OWASP 2024),
 * configure {@code m=65536, t=3, p=4}.
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class PasswordEncryptService {

  /** Pattern to extract Argon2 params from hash header: $argon2id$v=19$m=16384,t=2,p=1$... */
  private static final Pattern ARGON2_PARAMS_PATTERN =
      Pattern.compile("\\$argon2[id]{1,2}\\$v=\\d+\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$");

  private final Argon2PasswordEncoder encoder;
  private final int configuredMemory;
  private final int configuredIterations;
  private final int configuredParallelism;

  public PasswordEncryptService(Argon2Properties argon2Properties) {
    this.configuredMemory = argon2Properties.getMemory();
    this.configuredIterations = argon2Properties.getIterations();
    this.configuredParallelism = argon2Properties.getParallelism();
    this.encoder =
        new Argon2PasswordEncoder(
            argon2Properties.getSaltLength(),
            argon2Properties.getHashLength(),
            argon2Properties.getParallelism(),
            argon2Properties.getMemory(),
            argon2Properties.getIterations());
  }

  /**
   * 加密密码
   *
   * @param rawPassword 原始密码
   * @return 加密后的密码
   */
  public String encrypt(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  /**
   * 验证密码是否匹配
   *
   * @param rawPassword 原始密码
   * @param encodedPassword 加密后的密码
   * @return true 匹配, false 不匹配
   */
  public boolean matches(String rawPassword, String encodedPassword) {
    return encoder.matches(rawPassword, encodedPassword);
  }

  /**
   * Check if an encoded password was hashed with different Argon2 parameters than the current
   * configuration, indicating it should be re-hashed (lazy migration).
   *
   * <p>Parses the Argon2 hash header format: {@code $argon2id$v=19$m=16384,t=2,p=1$<salt>$<hash>}
   *
   * @param encodedPassword the Argon2 encoded password hash
   * @return true if the hash uses different parameters than the current config
   */
  public boolean needsUpgrade(String encodedPassword) {
    if (encodedPassword == null || !encodedPassword.startsWith("$argon2")) {
      return false;
    }

    Matcher matcher = ARGON2_PARAMS_PATTERN.matcher(encodedPassword);
    if (!matcher.find()) {
      return false;
    }

    int hashMemory = Integer.parseInt(matcher.group(1));
    int hashIterations = Integer.parseInt(matcher.group(2));
    int hashParallelism = Integer.parseInt(matcher.group(3));

    return hashMemory != configuredMemory
        || hashIterations != configuredIterations
        || hashParallelism != configuredParallelism;
  }
}
