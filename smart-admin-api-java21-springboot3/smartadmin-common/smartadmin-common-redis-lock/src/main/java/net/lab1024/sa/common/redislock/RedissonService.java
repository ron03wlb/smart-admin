package net.lab1024.sa.common.redislock;

import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redisson 业务
 *
 * @author huke
 * @since 2024/6/19 20:39
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonService {

  private final RedissonClient redissonClient;

  private final LockService lockService;

  public RedissonClient getRedissonClient() {
    return redissonClient;
  }

  /**
   * 获取锁 并 执行程序
   *
   * @param lockKey 锁的 key
   * @param waitTime 等待时间（毫秒）
   * @param lockTime 锁持有时间（毫秒）
   * @param supplier 业务逻辑
   * @param <T> 返回值类型
   * @return 业务逻辑返回值
   * @deprecated 请使用 {@link LockService#executeWithLock(String, long, long, Supplier)} 替代
   */
  @Deprecated(since = "3.1.0")
  public <T> T executeWithLock(String lockKey, long waitTime, long lockTime, Supplier<T> supplier) {
    return lockService.executeWithLock(lockKey, waitTime, lockTime, supplier);
  }

  /**
   * 获取锁 并 执行程序
   *
   * @param lockKey 锁的 key
   * @param waitTime 等待时间（毫秒）
   * @param lockTime 锁持有时间（毫秒）
   * @param runnable 业务逻辑
   * @deprecated 请使用 {@link LockService#executeWithLock(String, long, long, Runnable)} 替代
   */
  @Deprecated(since = "3.1.0")
  public void executeWithLock(String lockKey, long waitTime, long lockTime, Runnable runnable) {
    lockService.executeWithLock(lockKey, waitTime, lockTime, runnable);
  }

  /**
   * 获取 id 生成器 nextId 可生成连续不重复的id
   *
   * @param key
   * @return
   */
  public RIdGenerator idGenerator(String key) {
    return redissonClient.getIdGenerator(key);
  }

  /**
   * 存放任意数据类型
   *
   * @param key
   * @param v
   * @param duration
   * @param <T>
   */
  public <T> void putObj(String key, T v, Duration duration) {
    redissonClient.getBucket(key).set(v, duration);
  }

  /**
   * 获取任意数据类型
   *
   * @param key
   * @param clazz
   * @param <T>
   * @return 如果没有找到则返回null
   */
  public <T> T getObj(String key, Class<T> clazz) {
    RBucket<T> bucket = redissonClient.getBucket(key);
    return bucket.get();
  }
}
