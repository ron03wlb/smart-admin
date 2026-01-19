package net.lab1024.sa.base.module.support.repeatsubmit.ticket;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 凭证（使用 Lock4j 实现）
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-07-26 23:56:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RepeatSubmitRedisTicket extends AbstractRepeatSubmitTicket {

  private final LockTemplate lockTemplate;

  /** 存储 intervalMilliSecond == 0 时的锁信息，用于手动释放 */
  private final Map<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();

  public RepeatSubmitRedisTicket(
      LockTemplate lockTemplate, Function<HttpServletRequest, String> ticketFunction) {
    super(ticketFunction);
    this.lockTemplate = lockTemplate;
  }

  @Override
  public boolean tryLock(String ticket, Long currentTimestamp, Long intervalMilliSecond) {
    // 使用 acquireTimeout=0 表示不等待，立即返回
    // expire 为锁的持有时间
    long expire = intervalMilliSecond > 0 ? intervalMilliSecond : 30000L; // 默认30秒超时
    LockInfo lockInfo = lockTemplate.lock(ticket, expire, 0);

    if (lockInfo == null) {
      return false;
    }

    // 如果 intervalMilliSecond == 0，需要存储锁信息以便后续手动释放
    if (intervalMilliSecond <= 0) {
      lockInfoMap.put(ticket, lockInfo);
    }

    return true;
  }

  @Override
  public void unLock(String ticket, Long intervalMilliSecond) {
    // intervalMilliSecond > 0 时，锁会自动过期，无需手动释放
    if (intervalMilliSecond > 0) {
      return;
    }

    // intervalMilliSecond == 0 时，需要手动释放锁
    LockInfo lockInfo = lockInfoMap.remove(ticket);
    if (lockInfo != null) {
      lockTemplate.releaseLock(lockInfo);
    }
  }
}
