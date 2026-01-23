package net.lab1024.sa.foundation.repeatsubmit.ticket;

import jakarta.servlet.http.HttpServletRequest;
import net.lab1024.sa.foundation.repeatsubmit.generator.TicketGenerator;

/**
 * 凭证抽象基类（用于校验重复提交的凭证）
 *
 * <p>封装凭证生成逻辑，子类只需实现具体的加锁和解锁操作。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public abstract class AbstractRepeatSubmitTicket implements RepeatSubmitTicket {

  private final TicketGenerator ticketGenerator;

  /**
   * 构造函数
   *
   * @param ticketGenerator 凭证生成器
   */
  protected AbstractRepeatSubmitTicket(TicketGenerator ticketGenerator) {
    this.ticketGenerator = ticketGenerator;
  }

  /**
   * 生成加锁的凭证
   *
   * @param request HTTP请求对象
   * @return 凭证字符串
   */
  @Override
  public String generateTicket(HttpServletRequest request) {
    return this.ticketGenerator.generate(request);
  }

  /**
   * 尝试加锁
   *
   * @param ticket 凭证
   * @param currentTimestamp 当前时间戳
   * @param intervalMilliSecond 间隔时间（毫秒）
   * @return 是否加锁成功
   */
  @Override
  public abstract boolean tryLock(String ticket, Long currentTimestamp, Long intervalMilliSecond);

  /**
   * 释放锁
   *
   * @param ticket 凭证
   * @param intervalMilliSecond 间隔时间（毫秒）
   */
  @Override
  public abstract void unLock(String ticket, Long intervalMilliSecond);
}
