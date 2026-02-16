package net.lab1024.sa.common.token.config;

import cn.dev33.satoken.config.SaTokenConfig;
import net.lab1024.sa.common.token.player.StpPlayerLogic;
import net.lab1024.sa.common.token.player.StpPlayerUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the player StpLogic bean with its own token configuration.
 *
 * <p>Player sessions use a separate {@code sa-player-token} header, independent from the admin
 * {@code Authorization} header.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Configuration
public class StpLogicConfig {

  @Bean
  public StpPlayerLogic stpPlayerLogic() {
    StpPlayerLogic logic = StpPlayerUtil.stpPlayerLogic;
    SaTokenConfig config = new SaTokenConfig();
    config.setTokenName("sa-player-token");
    config.setTokenPrefix("Bearer");
    config.setIsConcurrent(true);
    config.setIsShare(false);
    config.setTokenStyle("simple-uuid");
    config.setAutoRenew(true);
    config.setIsReadCookie(false);
    logic.setConfig(config);
    return logic;
  }
}
