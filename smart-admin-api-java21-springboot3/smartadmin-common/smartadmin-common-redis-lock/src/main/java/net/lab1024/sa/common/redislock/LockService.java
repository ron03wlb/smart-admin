package net.lab1024.sa.common.redislock;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分佈式鎖服務
 *
 * <p>基於 Lock4j 封裝的分佈式鎖服務，提供統一的鎖操作 API
 *
 * @author ron.chang
 * @since 2026-01-19
 */
@Slf4j
@Service
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class LockService {

  private final LockTemplate lockTemplate;

  public LockService(LockTemplate lockTemplate) {
    this.lockTemplate = lockTemplate;
  }

  /**
   * 獲取鎖並執行程序（有返回值）
   *
   * @param lockKey 鎖的 key
   * @param acquireTimeout 獲取鎖的等待時間（毫秒），0 表示不等待
   * @param expire 鎖的持有時間（毫秒）
   * @param supplier 需要執行的業務邏輯
   * @param <T> 返回值類型
   * @return 業務邏輯的返回值
   * @throws IllegalStateException 當無法獲取鎖時拋出異常
   */
  public <T> T executeWithLock(
      String lockKey, long acquireTimeout, long expire, Supplier<T> supplier) {
    LockInfo lockInfo = tryLock(lockKey, acquireTimeout, expire);
    try {
      return supplier.get();
    } finally {
      releaseLock(lockInfo);
    }
  }

  /**
   * 獲取鎖並執行程序（無返回值）
   *
   * @param lockKey 鎖的 key
   * @param acquireTimeout 獲取鎖的等待時間（毫秒），0 表示不等待
   * @param expire 鎖的持有時間（毫秒）
   * @param runnable 需要執行的業務邏輯
   * @throws IllegalStateException 當無法獲取鎖時拋出異常
   */
  public void executeWithLock(String lockKey, long acquireTimeout, long expire, Runnable runnable) {
    LockInfo lockInfo = tryLock(lockKey, acquireTimeout, expire);
    try {
      runnable.run();
    } finally {
      releaseLock(lockInfo);
    }
  }

  /**
   * 嘗試獲取鎖
   *
   * @param lockKey 鎖的 key
   * @param acquireTimeout 獲取鎖的等待時間（毫秒），0 表示不等待
   * @param expire 鎖的持有時間（毫秒）
   * @return LockInfo 鎖信息，用於後續釋放鎖
   * @throws IllegalStateException 當無法獲取鎖時拋出異常
   */
  public LockInfo tryLock(String lockKey, long acquireTimeout, long expire) {
    LockInfo lockInfo = lockTemplate.lock(lockKey, expire, acquireTimeout);
    if (lockInfo == null) {
      throw new IllegalStateException("业务繁忙,请稍后重试~");
    }
    return lockInfo;
  }

  /**
   * 嘗試獲取鎖（不阻塞，立即返回結果）
   *
   * @param lockKey 鎖的 key
   * @param expire 鎖的持有時間（毫秒）
   * @return LockInfo 如果獲取成功返回鎖信息，否則返回 null
   */
  public LockInfo tryLockNonBlocking(String lockKey, long expire) {
    return lockTemplate.lock(lockKey, expire, 0);
  }

  /**
   * 釋放鎖
   *
   * @param lockInfo 鎖信息
   */
  public void releaseLock(LockInfo lockInfo) {
    if (lockInfo != null) {
      lockTemplate.releaseLock(lockInfo);
    }
  }
}
