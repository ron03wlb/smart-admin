package net.lab1024.sa.common.mq.kafka.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息聚合器
 *
 * <p>将消息累积到一定数量或超时后触发批量处理
 *
 * <p>使用示例:
 *
 * <pre>{@code
 * MessageAggregator<String> aggregator = new MessageAggregator<>(
 *     50,           // 累积 50 条消息后触发
 *     5000,         // 或超时 5 秒后触发
 *     messages -> { // 批量处理回调
 *         kafkaProducerService.sendBatchAsync(topic, messages);
 *     }
 * );
 *
 * // 添加消息
 * aggregator.add("message1");
 * aggregator.add("message2");
 * // ...
 *
 * // 关闭时刷新剩余消息
 * aggregator.shutdown();
 * }</pre>
 *
 * @param <T> 消息类型
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@SuppressWarnings("PMD.GuardLogStatement") // SLF4J 占位符已优化性能
public class MessageAggregator<T> {

  /** 聚合数量阈值 */
  private final int countThreshold;

  /** 超时时间（毫秒） */
  private final long timeoutMs;

  /** 批量处理回调 */
  private final Consumer<List<T>> batchHandler;

  /** 消息缓冲区 */
  private List<T> buffer;

  /** 缓冲区锁 */
  private final ReentrantLock lock = new ReentrantLock();

  /** 调度器 */
  private final ScheduledExecutorService scheduler;

  /** 当前调度的超时任务 */
  private ScheduledFuture<?> scheduledFlush;

  /** 是否已关闭 */
  private volatile boolean isShutdown = false;

  /** 首条消息标记 */
  private static final int FIRST_MESSAGE_SIZE = 1;

  /**
   * 创建消息聚合器
   *
   * @param countThreshold 数量阈值（达到此数量触发批量处理）
   * @param timeoutMs 超时时间（毫秒，超时后强制处理已聚合的消息）
   * @param batchHandler 批量处理回调
   */
  public MessageAggregator(int countThreshold, long timeoutMs, Consumer<List<T>> batchHandler) {
    if (countThreshold <= 0) {
      throw new IllegalArgumentException("countThreshold must be positive");
    }
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("timeoutMs must be positive");
    }
    if (batchHandler == null) {
      throw new IllegalArgumentException("batchHandler cannot be null");
    }

    this.countThreshold = countThreshold;
    this.timeoutMs = timeoutMs;
    this.batchHandler = batchHandler;
    this.buffer = new ArrayList<>(countThreshold);
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "kafka-message-aggregator");
              t.setDaemon(true);
              return t;
            });
  }

  /**
   * 添加消息到聚合器
   *
   * <p>达到数量阈值时自动触发批量处理
   *
   * @param message 消息
   */
  public void add(T message) {
    if (isShutdown) {
      throw new IllegalStateException("Aggregator has been shutdown");
    }

    List<T> toFlush = null;

    lock.lock();
    try {
      buffer.add(message);

      // 第一条消息时启动超时调度
      if (buffer.size() == FIRST_MESSAGE_SIZE) {
        scheduleTimeoutFlush();
      }

      // 达到阈值时触发刷新
      if (buffer.size() >= countThreshold) {
        toFlush = flushBufferLocked();
      }
    } finally {
      lock.unlock();
    }

    // 在锁外执行批量处理
    if (toFlush != null && !toFlush.isEmpty()) {
      executeBatchHandler(toFlush);
    }
  }

  /**
   * 批量添加消息
   *
   * @param messages 消息列表
   */
  public void addAll(List<T> messages) {
    if (messages == null || messages.isEmpty()) {
      return;
    }
    for (T message : messages) {
      add(message);
    }
  }

  /**
   * 强制刷新缓冲区
   *
   * <p>立即处理所有已聚合的消息，不等待阈值或超时
   */
  public void flush() {
    List<T> toFlush = null;

    lock.lock();
    try {
      if (!buffer.isEmpty()) {
        toFlush = flushBufferLocked();
      }
    } finally {
      lock.unlock();
    }

    if (toFlush != null && !toFlush.isEmpty()) {
      executeBatchHandler(toFlush);
    }
  }

  /**
   * 关闭聚合器
   *
   * <p>刷新剩余消息并关闭调度器
   */
  public void shutdown() {
    isShutdown = true;
    flush();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      scheduler.shutdownNow();
    }
  }

  /**
   * 获取当前缓冲区大小
   *
   * @return 缓冲区中的消息数量
   */
  public int getBufferSize() {
    lock.lock();
    try {
      return buffer.size();
    } finally {
      lock.unlock();
    }
  }

  /** 调度超时刷新任务 */
  private void scheduleTimeoutFlush() {
    if (scheduledFlush != null) {
      scheduledFlush.cancel(false);
    }
    scheduledFlush = scheduler.schedule(this::timeoutFlush, timeoutMs, TimeUnit.MILLISECONDS);
  }

  /** 超时触发的刷新 */
  private void timeoutFlush() {
    List<T> toFlush = null;

    lock.lock();
    try {
      if (!buffer.isEmpty()) {
        log.debug("消息聚合器超时触发刷新: bufferSize={}", buffer.size());
        toFlush = flushBufferLocked();
      }
    } finally {
      lock.unlock();
    }

    if (toFlush != null && !toFlush.isEmpty()) {
      executeBatchHandler(toFlush);
    }
  }

  /**
   * 刷新缓冲区（需要在锁内调用）
   *
   * @return 被刷新的消息列表
   */
  @SuppressWarnings("PMD.NullAssignment") // 主动释放 ScheduledFuture 引用
  private List<T> flushBufferLocked() {
    // 取消超时调度
    if (scheduledFlush != null) {
      scheduledFlush.cancel(false);
      scheduledFlush = null;
    }

    List<T> flushed = buffer;
    buffer = new ArrayList<>(countThreshold);
    return flushed;
  }

  /**
   * 执行批量处理回调
   *
   * @param messages 消息列表
   */
  private void executeBatchHandler(List<T> messages) {
    try {
      log.debug("消息聚合器执行批量处理: messageCount={}", messages.size());
      batchHandler.accept(messages);
    } catch (Exception ex) {
      log.error("消息聚合器批量处理异常: messageCount={}, error={}", messages.size(), ex.getMessage(), ex);
    }
  }
}
