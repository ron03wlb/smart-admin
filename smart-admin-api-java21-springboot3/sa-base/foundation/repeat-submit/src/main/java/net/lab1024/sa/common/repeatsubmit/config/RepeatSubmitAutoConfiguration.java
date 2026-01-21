package net.lab1024.sa.common.repeatsubmit.config;

import com.baomidou.lock.LockTemplate;
import net.lab1024.sa.common.repeatsubmit.aspect.RepeatSubmitAspect;
import net.lab1024.sa.common.repeatsubmit.generator.TicketGenerator;
import net.lab1024.sa.common.repeatsubmit.ticket.RepeatSubmitMemoryTicket;
import net.lab1024.sa.common.repeatsubmit.ticket.RepeatSubmitRedisTicket;
import net.lab1024.sa.common.repeatsubmit.ticket.RepeatSubmitTicket;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 防重复提交自动配置类
 *
 * <p>提供防重复提交功能的自动装配，根据是否存在 LockTemplate 自动选择 Redis 或内存实现。
 *
 * <p>使用方式：
 *
 * <pre>
 * 1. 在项目中引入 sa-common:repeat-submit 依赖
 * 2. 提供 TicketGenerator Bean（必须）
 * 3. Spring Boot 会自动装配 RepeatSubmitAspect
 * 4. 在需要防重复提交的方法上添加 @RepeatSubmit 注解
 * </pre>
 *
 * <p>配置示例（在 sa-base 中提供 TicketGenerator）：
 *
 * <pre>{@code
 * @Configuration
 * public class RepeatSubmitConfig {
 *
 *     @Bean
 *     public TicketGenerator ticketGenerator() {
 *         return request -> {
 *             Long userId = SmartRequestUtil.getRequestUserId();
 *             if (userId == null) {
 *                 return "";
 *             }
 *             return request.getServletPath() + "_" + userId;
 *         };
 *     }
 * }
 * }</pre>
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AutoConfiguration
@ConditionalOnBean(TicketGenerator.class)
public class RepeatSubmitAutoConfiguration {

  /**
   * 配置 Redis 实现的防重复提交凭证（优先）
   *
   * <p>当存在 LockTemplate Bean 时使用 Redis 实现，支持分布式场景。
   *
   * @param lockTemplate Lock4j 锁模板
   * @param ticketGenerator 凭证生成器
   * @return Redis 实现的防重复提交凭证
   */
  @Bean
  @ConditionalOnBean(LockTemplate.class)
  @ConditionalOnMissingBean(RepeatSubmitTicket.class)
  public RepeatSubmitTicket repeatSubmitRedisTicket(
      LockTemplate lockTemplate, TicketGenerator ticketGenerator) {
    return new RepeatSubmitRedisTicket(lockTemplate, ticketGenerator);
  }

  /**
   * 配置内存实现的防重复提交凭证（备用）
   *
   * <p>当不存在 LockTemplate Bean 时使用内存实现，仅适用于单机部署。
   *
   * @param ticketGenerator 凭证生成器
   * @return 内存实现的防重复提交凭证
   */
  @Bean
  @ConditionalOnMissingBean({RepeatSubmitTicket.class, LockTemplate.class})
  public RepeatSubmitTicket repeatSubmitMemoryTicket(TicketGenerator ticketGenerator) {
    return new RepeatSubmitMemoryTicket(ticketGenerator);
  }

  /**
   * 配置防重复提交切面
   *
   * @param repeatSubmitTicket 防重复提交凭证
   * @return 防重复提交切面
   */
  @Bean
  @ConditionalOnMissingBean(RepeatSubmitAspect.class)
  public RepeatSubmitAspect repeatSubmitAspect(RepeatSubmitTicket repeatSubmitTicket) {
    return new RepeatSubmitAspect(repeatSubmitTicket);
  }
}
