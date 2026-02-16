package net.lab1024.sa.common.token.player;

import cn.dev33.satoken.session.SaSession;

/**
 * Player authentication facade using the "player" StpLogic namespace.
 *
 * <p>All operations are independent from admin authentication (StpAdminUtil). Redis keys: {@code
 * sa:player:session:xxx}, {@code sa:player:token:xxx}.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public final class StpPlayerUtil {

  public static StpPlayerLogic stpPlayerLogic = new StpPlayerLogic();

  private StpPlayerUtil() {}

  public static void login(Object id) {
    stpPlayerLogic.login(id);
  }

  public static void login(Object id, long timeout) {
    stpPlayerLogic.login(id, timeout);
  }

  public static void logout() {
    stpPlayerLogic.logout();
  }

  public static void logout(Object loginId) {
    stpPlayerLogic.logout(loginId);
  }

  public static String getTokenValue() {
    return stpPlayerLogic.getTokenValue();
  }

  public static Object getLoginIdByToken(String token) {
    return stpPlayerLogic.getLoginIdByToken(token);
  }

  public static SaSession getSession() {
    return stpPlayerLogic.getSession();
  }

  public static boolean isLogin() {
    return stpPlayerLogic.isLogin();
  }
}
