package net.lab1024.sa.common.security.service;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码加密服务
 *
 * <p>提供基于 Argon2 的密码加密和验证功能。Argon2 是目前最安全的密码哈希算法之一， 已被选为密码哈希竞赛（Password Hashing Competition）的获胜者。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class PasswordEncryptService {

  private static final Argon2PasswordEncoder ENCODER =
      Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

  /**
   * 加密密码
   *
   * @param rawPassword 原始密码
   * @return 加密后的密码
   */
  public String encrypt(String rawPassword) {
    return ENCODER.encode(rawPassword);
  }

  /**
   * 验证密码是否匹配
   *
   * @param rawPassword 原始密码
   * @param encodedPassword 加密后的密码
   * @return true 匹配, false 不匹配
   */
  public boolean matches(String rawPassword, String encodedPassword) {
    return ENCODER.matches(rawPassword, encodedPassword);
  }
}
