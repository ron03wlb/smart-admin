package net.lab1024.sa.base.config;

import net.lab1024.sa.base.core.util.SmartRequestUtil;
import net.lab1024.sa.common.repeatsubmit.generator.TicketGenerator;
import net.lab1024.sa.foundation.domain.constant.StringConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 重复提交配置
 *
 * <p>为 repeat-submit 模块提供 TicketGenerator Bean，生成包含用户ID的防重复提交凭证。
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021/10/9 18:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Configuration
public class RepeatSubmitConfig {

  /**
   * 提供凭证生成器
   *
   * <p>凭证格式为：请求路径_用户ID。 如果用户未登录（userId为null），则返回空字符串，不进行防重复提交检查。
   *
   * @return 凭证生成器
   */
  @Bean
  public TicketGenerator ticketGenerator() {
    return request -> {
      Long userId = SmartRequestUtil.getRequestUserId();
      if (null == userId) {
        return StringConst.EMPTY;
      }
      return request.getServletPath() + "_" + userId;
    };
  }
}
