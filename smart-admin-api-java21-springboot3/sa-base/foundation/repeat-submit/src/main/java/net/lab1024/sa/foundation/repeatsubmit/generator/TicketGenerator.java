package net.lab1024.sa.foundation.repeatsubmit.generator;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 防重复提交凭证生成器函数式接口
 *
 * <p>用于生成防重复提交的唯一凭证，通常基于请求路径和用户ID组合生成。 此接口由业务层（如 sa-base）实现，以便注入用户ID等业务相关信息。
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * @Bean
 * public TicketGenerator ticketGenerator() {
 *     return request -> {
 *         Long userId = SmartRequestUtil.getRequestUserId();
 *         if (userId == null) {
 *             return "";
 *         }
 *         return request.getServletPath() + "_" + userId;
 *     };
 * }
 * }</pre>
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@FunctionalInterface
public interface TicketGenerator {

  /**
   * 生成防重复提交的唯一凭证
   *
   * @param request HTTP请求对象
   * @return 凭证字符串，返回空字符串表示不需要防重复提交检查
   */
  String generate(HttpServletRequest request);
}
