package net.lab1024.sa.common.token.admin;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

/**
 * Admin authentication facade delegating to the default StpUtil.
 *
 * <p>Uses the default StpLogic (type="login") so that all existing {@code @SaCheckPermission}
 * annotations continue to work without modification.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public final class StpAdminUtil {

  private StpAdminUtil() {}

  public static void login(Object id) {
    StpUtil.login(id);
  }

  public static void login(Object id, long timeout) {
    StpUtil.login(id, timeout);
  }

  public static void login(Object id, String device) {
    StpUtil.login(id, device);
  }

  public static void logout() {
    StpUtil.logout();
  }

  public static void logout(Object loginId) {
    StpUtil.logout(loginId);
  }

  public static String getTokenValue() {
    return StpUtil.getTokenValue();
  }

  public static Object getLoginIdByToken(String token) {
    return StpUtil.getLoginIdByToken(token);
  }

  public static SaSession getSession() {
    return StpUtil.getSession();
  }

  public static StpLogic getStpLogic() {
    return StpUtil.getStpLogic();
  }

  public static boolean isLogin() {
    return StpUtil.isLogin();
  }
}
