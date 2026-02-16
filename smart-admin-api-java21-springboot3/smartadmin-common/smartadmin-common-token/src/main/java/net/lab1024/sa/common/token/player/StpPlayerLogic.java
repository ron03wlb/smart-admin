package net.lab1024.sa.common.token.player;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Player-specific StpLogic with independent "player" namespace.
 *
 * <p>Redis keys use prefix {@code sa:player:session:xxx}, fully separated from admin sessions
 * ({@code sa:login:session:xxx}).
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public class StpPlayerLogic extends StpLogic {

  public StpPlayerLogic() {
    super("player");
  }
}
