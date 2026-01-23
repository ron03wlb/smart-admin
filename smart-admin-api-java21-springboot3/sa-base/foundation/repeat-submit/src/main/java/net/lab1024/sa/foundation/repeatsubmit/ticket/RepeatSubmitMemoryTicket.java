package net.lab1024.sa.foundation.repeatsubmit.ticket;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Maps;
import java.util.concurrent.ConcurrentMap;
import net.lab1024.sa.foundation.repeatsubmit.generator.TicketGenerator;

/**
 * 凭证（内存实现）
 *
 * <p>基于内存的防重复提交实现，使用 Guava 的 Interner 实现字符串锁定。 适用于单机部署场景，分布式场景请使用 Redis 实现。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class RepeatSubmitMemoryTicket extends AbstractRepeatSubmitTicket {

  private final Interner<String> pool = Interners.newStrongInterner();

  private final ConcurrentMap<String, Long> ticketMap = Maps.newConcurrentMap();

  /**
   * 构造函数
   *
   * @param ticketGenerator 凭证生成器
   */
  public RepeatSubmitMemoryTicket(TicketGenerator ticketGenerator) {
    super(ticketGenerator);
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
  public boolean tryLock(String ticket, Long currentTimestamp, Long intervalMilliSecond) {
    synchronized (pool.intern(ticket)) {
      Long lastTime = ticketMap.putIfAbsent(ticket, currentTimestamp);
      if (lastTime == null) {
        return true;
      }

      if (intervalMilliSecond <= 0) {
        return false;
      }

      if (currentTimestamp - lastTime < intervalMilliSecond) {
        return false;
      }
      ticketMap.put(ticket, currentTimestamp);
      return true;
    }
  }

  /**
   * 释放锁
   *
   * @param ticket 凭证
   * @param intervalMilliSecond 间隔时间（毫秒）
   */
  @Override
  public void unLock(String ticket, Long intervalMilliSecond) {
    if (intervalMilliSecond > 0) {
      return;
    }
    ticketMap.remove(ticket);
  }
}
