package net.lab1024.sa.foundation.mq.kafka.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MessageAggregator 单元测试
 *
 * <p>测试消息聚合器的核心功能：
 *
 * <ul>
 *   <li>数量阈值触发批量处理
 *   <li>超时触发批量处理
 *   <li>手动刷新功能
 *   <li>多线程并发安全性
 *   <li>边界条件和异常处理
 * </ul>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@DisplayName("MessageAggregator 单元测试")
class MessageAggregatorTest {

  /** 测试用的数量阈值 */
  private static final int TEST_COUNT_THRESHOLD = 5;

  /** 测试用的超时时间（毫秒） */
  private static final long TEST_TIMEOUT_MS = 500;

  /** 收集批量处理结果的列表 */
  private List<List<String>> batchResults;

  /** 被测试的聚合器实例 */
  private MessageAggregator<String> aggregator;

  @BeforeEach
  void setUp() {
    batchResults = new CopyOnWriteArrayList<>();
  }

  @AfterEach
  void tearDown() {
    if (aggregator != null) {
      aggregator.shutdown();
    }
  }

  @Nested
  @DisplayName("构造函数参数验证")
  class ConstructorValidation {

    @Test
    @DisplayName("countThreshold 为零时应抛出异常")
    void testConstructor_zeroCountThreshold_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(0, 1000, messages -> {}))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("countThreshold must be positive");
    }

    @Test
    @DisplayName("countThreshold 为负数时应抛出异常")
    void testConstructor_negativeCountThreshold_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(-1, 1000, messages -> {}))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("countThreshold must be positive");
    }

    @Test
    @DisplayName("timeoutMs 为零时应抛出异常")
    void testConstructor_zeroTimeoutMs_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(10, 0, messages -> {}))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("timeoutMs must be positive");
    }

    @Test
    @DisplayName("timeoutMs 为负数时应抛出异常")
    void testConstructor_negativeTimeoutMs_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(10, -100, messages -> {}))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("timeoutMs must be positive");
    }

    @Test
    @DisplayName("batchHandler 为 null 时应抛出异常")
    void testConstructor_nullBatchHandler_throwsException() {
      assertThatThrownBy(() -> new MessageAggregator<>(10, 1000, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("batchHandler cannot be null");
    }

    @Test
    @DisplayName("有效参数应成功创建聚合器")
    void testConstructor_validParameters_success() {
      aggregator = new MessageAggregator<>(10, 1000, messages -> {});
      assertThat(aggregator.getBufferSize()).isZero();
    }
  }

  @Nested
  @DisplayName("数量阈值触发测试")
  class CountThresholdTrigger {

    @Test
    @DisplayName("达到数量阈值时应触发批量处理")
    void testAggregateByCount_reachThreshold_triggersBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When - 添加刚好达到阈值的消息数量
      for (int i = 0; i < TEST_COUNT_THRESHOLD; i++) {
        aggregator.add("message-" + i);
      }

      // Then - 应该触发一次批量处理
      assertThat(batchResults).hasSize(1);
      assertThat(batchResults.get(0)).hasSize(TEST_COUNT_THRESHOLD);
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("未达到数量阈值时不应触发批量处理")
    void testAggregateByCount_belowThreshold_noBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When - 添加少于阈值的消息
      for (int i = 0; i < TEST_COUNT_THRESHOLD - 1; i++) {
        aggregator.add("message-" + i);
      }

      // Then - 不应触发批量处理
      assertThat(batchResults).isEmpty();
      assertThat(aggregator.getBufferSize()).isEqualTo(TEST_COUNT_THRESHOLD - 1);
    }

    @Test
    @DisplayName("多次达到阈值应多次触发批量处理")
    void testAggregateByCount_multipleThresholds_multipleTrigggers() {
      // Given
      aggregator = createAggregator();
      int totalMessages = TEST_COUNT_THRESHOLD * 3;

      // When - 添加三倍阈值的消息
      for (int i = 0; i < totalMessages; i++) {
        aggregator.add("message-" + i);
      }

      // Then - 应该触发三次批量处理
      assertThat(batchResults).hasSize(3);
      for (List<String> batch : batchResults) {
        assertThat(batch).hasSize(TEST_COUNT_THRESHOLD);
      }
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("超过阈值的剩余消息应保留在缓冲区")
    void testAggregateByCount_remainder_staysInBuffer() {
      // Given
      aggregator = createAggregator();
      int totalMessages = TEST_COUNT_THRESHOLD + 2;

      // When
      for (int i = 0; i < totalMessages; i++) {
        aggregator.add("message-" + i);
      }

      // Then
      assertThat(batchResults).hasSize(1);
      assertThat(aggregator.getBufferSize()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("超时触发测试")
  class TimeoutTrigger {

    @Test
    @DisplayName("超时后应触发批量处理")
    void testAggregateByTimeout_triggersBatchHandle() {
      // Given
      aggregator = createAggregator();

      // When - 添加未达阈值的消息
      aggregator.add("message-1");
      aggregator.add("message-2");

      // Then - 等待超时触发
      await()
          .atMost(TEST_TIMEOUT_MS * 3, TimeUnit.MILLISECONDS)
          .untilAsserted(
              () -> {
                assertThat(batchResults).hasSize(1);
                assertThat(batchResults.get(0)).hasSize(2);
              });
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("空缓冲区超时不应触发批量处理")
    void testAggregateByTimeout_emptyBuffer_noTrigger() throws InterruptedException {
      // Given
      aggregator = createAggregator();

      // When - 不添加任何消息，等待超时
      Thread.sleep(TEST_TIMEOUT_MS * 2);

      // Then - 不应触发批量处理
      assertThat(batchResults).isEmpty();
    }

    @Test
    @DisplayName("达到阈值后不应再触发超时处理")
    void testAggregateByTimeout_afterThreshold_noTimeoutTrigger() throws InterruptedException {
      // Given
      aggregator = createAggregator();

      // When - 添加达到阈值的消息
      for (int i = 0; i < TEST_COUNT_THRESHOLD; i++) {
        aggregator.add("message-" + i);
      }

      // 记录当前批次数
      int batchCountAfterThreshold = batchResults.size();

      // 等待超时时间
      Thread.sleep(TEST_TIMEOUT_MS * 2);

      // Then - 批次数应该保持不变（不应有超时触发）
      assertThat(batchResults).hasSize(batchCountAfterThreshold);
    }
  }

  @Nested
  @DisplayName("手动刷新测试")
  class ManualFlush {

    @Test
    @DisplayName("flush 应立即处理缓冲区消息")
    void testFlush_processesBuffer() {
      // Given
      aggregator = createAggregator();
      aggregator.add("message-1");
      aggregator.add("message-2");

      // When
      aggregator.flush();

      // Then
      assertThat(batchResults).hasSize(1);
      assertThat(batchResults.get(0)).hasSize(2);
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("空缓冲区 flush 不应触发批量处理")
    void testFlush_emptyBuffer_noTrigger() {
      // Given
      aggregator = createAggregator();

      // When
      aggregator.flush();

      // Then
      assertThat(batchResults).isEmpty();
    }

    @Test
    @DisplayName("连续 flush 应正确处理")
    void testFlush_consecutive_success() {
      // Given
      aggregator = createAggregator();
      aggregator.add("message-1");

      // When
      aggregator.flush();
      aggregator.flush(); // 第二次 flush 空缓冲区

      // Then
      assertThat(batchResults).hasSize(1);
    }
  }

  @Nested
  @DisplayName("shutdown 测试")
  class ShutdownTests {

    @Test
    @DisplayName("shutdown 应刷新剩余消息")
    void testShutdown_flushesRemainingMessages() {
      // Given
      aggregator = createAggregator();
      aggregator.add("message-1");
      aggregator.add("message-2");

      // When
      aggregator.shutdown();

      // Then
      assertThat(batchResults).hasSize(1);
      assertThat(batchResults.get(0)).hasSize(2);
    }

    @Test
    @DisplayName("shutdown 后添加消息应抛出异常")
    void testShutdown_addAfter_throwsException() {
      // Given
      aggregator = createAggregator();
      aggregator.shutdown();

      // When & Then
      assertThatThrownBy(() -> aggregator.add("message"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("shutdown");
    }
  }

  @Nested
  @DisplayName("addAll 批量添加测试")
  class AddAllTests {

    @Test
    @DisplayName("addAll 应正确添加所有消息")
    void testAddAll_addsAllMessages() {
      // Given
      aggregator = createAggregator();
      List<String> messages = List.of("msg-1", "msg-2", "msg-3");

      // When
      aggregator.addAll(messages);

      // Then
      assertThat(aggregator.getBufferSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("addAll 达到阈值应触发批量处理")
    void testAddAll_reachThreshold_triggersBatchHandle() {
      // Given
      aggregator = createAggregator();
      List<String> messages = new ArrayList<>();
      for (int i = 0; i < TEST_COUNT_THRESHOLD + 2; i++) {
        messages.add("msg-" + i);
      }

      // When
      aggregator.addAll(messages);

      // Then
      assertThat(batchResults).hasSize(1);
      assertThat(aggregator.getBufferSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("addAll null 列表不应抛出异常")
    void testAddAll_nullList_noException() {
      // Given
      aggregator = createAggregator();

      // When & Then - 不应抛出异常
      aggregator.addAll(null);
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("addAll 空列表不应抛出异常")
    void testAddAll_emptyList_noException() {
      // Given
      aggregator = createAggregator();

      // When & Then
      aggregator.addAll(Collections.emptyList());
      assertThat(aggregator.getBufferSize()).isZero();
    }
  }

  @Nested
  @DisplayName("并发安全测试")
  class ConcurrencyTests {

    @Test
    @DisplayName("多线程并发添加消息应线程安全")
    void testConcurrency_multipleThreadsAdd_threadSafe() throws InterruptedException {
      // Given
      int threadCount = 10;
      int messagesPerThread = TEST_COUNT_THRESHOLD * 2;
      int totalMessages = threadCount * messagesPerThread;

      aggregator = createAggregator();
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch endLatch = new CountDownLatch(threadCount);
      AtomicInteger totalAdded = new AtomicInteger(0);

      // When - 多线程并发添加
      for (int t = 0; t < threadCount; t++) {
        int threadId = t;
        executor.submit(
            () -> {
              try {
                startLatch.await(); // 等待所有线程就绪
                for (int i = 0; i < messagesPerThread; i++) {
                  aggregator.add("thread-" + threadId + "-msg-" + i);
                  totalAdded.incrementAndGet();
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                endLatch.countDown();
              }
            });
      }

      startLatch.countDown(); // 启动所有线程
      endLatch.await(10, TimeUnit.SECONDS); // 等待所有线程完成
      executor.shutdown();

      // flush 剩余消息
      aggregator.flush();

      // Then - 验证所有消息都被处理
      int processedCount = batchResults.stream().mapToInt(List::size).sum();
      assertThat(processedCount).isEqualTo(totalMessages);
      assertThat(aggregator.getBufferSize()).isZero();
    }

    @Test
    @DisplayName("并发 add 和 flush 应线程安全")
    void testConcurrency_addAndFlush_threadSafe() throws InterruptedException {
      // Given
      aggregator = createAggregator();
      ExecutorService executor = Executors.newFixedThreadPool(3);
      CountDownLatch latch = new CountDownLatch(3);
      AtomicInteger addCount = new AtomicInteger(0);

      // When - 一个线程持续添加，两个线程随机 flush
      executor.submit(
          () -> {
            try {
              for (int i = 0; i < 100; i++) {
                aggregator.add("msg-" + i);
                addCount.incrementAndGet();
                Thread.sleep(1);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              latch.countDown();
            }
          });

      for (int i = 0; i < 2; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < 20; j++) {
                  Thread.sleep(5);
                  aggregator.flush();
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();
      aggregator.flush();

      // Then - 验证没有消息丢失
      int processedCount = batchResults.stream().mapToInt(List::size).sum();
      assertThat(processedCount).isEqualTo(addCount.get());
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("batchHandler 异常不应影响聚合器状态")
    void testBatchHandler_exception_aggregatorRemainsFunctional() {
      // Given - 创建会抛出异常的处理器
      AtomicInteger callCount = new AtomicInteger(0);
      aggregator =
          new MessageAggregator<>(
              TEST_COUNT_THRESHOLD,
              TEST_TIMEOUT_MS,
              messages -> {
                callCount.incrementAndGet();
                if (callCount.get() == 1) {
                  throw new RuntimeException("Simulated error");
                }
                batchResults.add(new ArrayList<>(messages));
              });

      // When - 第一批触发异常
      for (int i = 0; i < TEST_COUNT_THRESHOLD; i++) {
        aggregator.add("batch1-msg-" + i);
      }

      // 第二批正常处理
      for (int i = 0; i < TEST_COUNT_THRESHOLD; i++) {
        aggregator.add("batch2-msg-" + i);
      }

      // Then - 第一批虽然异常，第二批仍然正常处理
      assertThat(callCount.get()).isEqualTo(2);
      assertThat(batchResults).hasSize(1); // 只有第二批成功
      assertThat(aggregator.getBufferSize()).isZero();
    }
  }

  @Nested
  @DisplayName("getBufferSize 测试")
  class GetBufferSizeTests {

    @Test
    @DisplayName("getBufferSize 应返回正确的缓冲区大小")
    void testGetBufferSize_returnsCorrectSize() {
      // Given
      aggregator = createAggregator();

      // When & Then
      assertThat(aggregator.getBufferSize()).isZero();

      aggregator.add("msg-1");
      assertThat(aggregator.getBufferSize()).isEqualTo(1);

      aggregator.add("msg-2");
      aggregator.add("msg-3");
      assertThat(aggregator.getBufferSize()).isEqualTo(3);

      aggregator.flush();
      assertThat(aggregator.getBufferSize()).isZero();
    }
  }

  /**
   * 创建测试用的聚合器
   *
   * @return MessageAggregator 实例
   */
  private MessageAggregator<String> createAggregator() {
    return new MessageAggregator<>(
        TEST_COUNT_THRESHOLD,
        TEST_TIMEOUT_MS,
        messages -> batchResults.add(new ArrayList<>(messages)));
  }
}
