package net.lab1024.sa.base.config;

import com.baomidou.lock.LockTemplate;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import net.lab1024.sa.base.common.constant.StringConst;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import net.lab1024.sa.base.module.support.repeatsubmit.RepeatSubmitAspect;
import net.lab1024.sa.base.module.support.repeatsubmit.ticket.RepeatSubmitRedisTicket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 重复提交配置
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021/10/9 18:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Configuration
public class RepeatSubmitConfig {

  @Resource private LockTemplate lockTemplate;

  @Bean
  public RepeatSubmitAspect repeatSubmitAspect() {
    RepeatSubmitRedisTicket ticket = new RepeatSubmitRedisTicket(lockTemplate, this::ticket);
    return new RepeatSubmitAspect(ticket);
  }

  /** 获取指明某个用户的凭证 */
  private String ticket(HttpServletRequest request) {
    Long userId = SmartRequestUtil.getRequestUserId();
    if (null == userId) {
      return StringConst.EMPTY;
    }
    return request.getServletPath() + "_" + userId;
  }
}
