package net.lab1024.sa.common.securityprotect.constant;

/**
 * 安全相关常量
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public final class SecurityConst {

  private SecurityConst() {
    // Prevent instantiation
  }

  /** 密码最小长度 */
  public static final int PASSWORD_MIN_LENGTH = 8;

  /** 密码最大长度 */
  public static final int PASSWORD_MAX_LENGTH = 20;

  /** 最小允许重复次数 */
  public static final int MIN_ALLOWED_REPEAT_TIMES = 1;

  /** 最小修改密码天数 */
  public static final int MIN_CHANGE_PASSWORD_DAYS = 1;

  /** 最小登录失败次数阈值 */
  public static final int MIN_FAIL_TIMES_THRESHOLD = 1;
}
