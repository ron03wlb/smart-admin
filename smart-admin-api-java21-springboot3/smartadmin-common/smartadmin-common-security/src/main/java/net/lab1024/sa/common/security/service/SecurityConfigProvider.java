package net.lab1024.sa.common.security.service;

/**
 * 安全配置提供者接口
 *
 * <p>由具体应用模块实现，提供安全配置参数。此接口定义了三级等保所需的各项安全配置。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface SecurityConfigProvider {

  /**
   * 是否开启双因子登录
   *
   * @return true 开启, false 关闭
   */
  boolean isTwoFactorLoginEnabled();

  /**
   * 获取连续登录失败最大次数
   *
   * <p>-1 表示不受限制，可以一直尝试登录
   *
   * @return 最大失败次数
   */
  int getLoginFailMaxTimes();

  /**
   * 获取连续登录失败锁定时间（单位：秒）
   *
   * <p>-1 表示不锁定
   *
   * @return 锁定时间（秒）
   */
  int getLoginFailLockSeconds();

  /**
   * 是否开启密码复杂度校验
   *
   * @return true 开启, false 关闭
   */
  boolean isPasswordComplexityEnabled();

  /**
   * 获取定期修改密码天数
   *
   * <p>超过此天数未修改密码需要强制修改
   *
   * @return 天数
   */
  int getRegularChangePasswordDays();

  /**
   * 获取密码不允许重复次数
   *
   * <p>新密码不能与最近N次使用的密码相同
   *
   * @return 不允许重复次数
   */
  int getRegularChangePasswordNotAllowRepeatTimes();

  /**
   * 是否开启文件检测
   *
   * @return true 开启, false 关闭
   */
  boolean isFileDetectEnabled();

  /**
   * 获取最大上传文件大小（单位：MB）
   *
   * @return 最大文件大小（MB）
   */
  long getMaxUploadFileSizeMb();

  /**
   * 获取登录活跃超时时间（单位：秒）
   *
   * <p>用户在此时间内无操作将被自动登出
   *
   * @return 超时时间（秒）
   */
  int getLoginActiveTimeoutSeconds();
}
