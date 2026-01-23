package net.lab1024.sa.foundation.repeatsubmit.ticket;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 防重复提交凭证接口
 *
 * <p>定义防重复提交凭证的基本操作，包括凭证生成、加锁和解锁。 实现类可以基于内存或 Redis 等存储方式。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface RepeatSubmitTicket {

  /**
   * 生成加锁的凭证
   *
   * @param request HTTP请求对象
   * @return 凭证字符串
   */
  String generateTicket(HttpServletRequest request);

  /**
   * 尝试加锁
   *
   * @param ticket 凭证
   * @param currentTimestamp 当前时间戳
   * @param intervalMilliSecond 间隔时间（毫秒）
   * @return 是否加锁成功
   */
  boolean tryLock(String ticket, Long currentTimestamp, Long intervalMilliSecond);

  /**
   * 释放锁
   *
   * @param ticket 凭证
   * @param intervalMilliSecond 间隔时间（毫秒）
   */
  void unLock(String ticket, Long intervalMilliSecond);
}
