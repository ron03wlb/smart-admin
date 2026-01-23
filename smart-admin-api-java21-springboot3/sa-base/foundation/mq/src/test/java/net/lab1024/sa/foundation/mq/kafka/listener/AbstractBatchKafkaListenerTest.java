package net.lab1024.sa.foundation.mq.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.foundation.mq.kafka.dlq.DeadLetterService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

/**
 * AbstractBatchKafkaListener 单元测试
 *
 * <p>测试批量消费者基类的核心功能：
 *
 * <ul>
 *   <li>批量消费成功场景
 *   <li>批量消费失败降级为单条处理
 *   <li>单条处理失败发送到 DLQ
 *   <li>手动确认机制
 * </ul>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractBatchKafkaListener 单元测试")
class AbstractBatchKafkaListenerTest {

  @Mock private Acknowledgment acknowledgment;

  @Mock private DeadLetterService deadLetterService;

  private TestBatchListener listener;

  @BeforeEach
  void setUp() {
    listener = new TestBatchListener();
  }

  @Nested
  @DisplayName("批量处理成功测试")
  class BatchHandleSuccessTests {

    @Test
    @DisplayName("批量处理成功时应调用 doBatchHandle 并确认消息")
    void testBatchConsume_success() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(3);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isTrue();
      assertThat(listener.singleHandleCallCount.get()).isZero();
      assertThat(listener.processedBatchRecords).hasSize(3);
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("空消息列表应直接确认")
    void testBatchConsume_emptyList_acknowledgesImmediately() {
      // Given
      List<ConsumerRecord<String, String>> records = List.of();

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isFalse();
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("null 消息列表应直接确认")
    void testBatchConsume_nullList_acknowledgesImmediately() {
      // Given & When
      listener.handleBatch(null, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isFalse();
      verify(acknowledgment, times(1)).acknowledge();
    }
  }

  @Nested
  @DisplayName("降级处理测试")
  class DegradationTests {

    @Test
    @DisplayName("批量处理失败时应降级为逐条处理")
    void testBatchConsume_batchFails_degradesToSingle() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(3);
      listener.batchHandleShouldFail = true;

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.batchHandleCalled).isTrue();
      assertThat(listener.singleHandleCallCount.get()).isEqualTo(3);
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("单条处理部分失败时应继续处理其他消息")
    void testBatchConsume_singlePartialFail_continuesProcessing() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(5);
      listener.batchHandleShouldFail = true;
      listener.singleHandleFailIndices.add(1);
      listener.singleHandleFailIndices.add(3);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.singleHandleCallCount.get()).isEqualTo(5);
      assertThat(listener.processedSingleRecords).hasSize(3); // 5 - 2 failed = 3 successful
      verify(acknowledgment, times(1)).acknowledge();
    }
  }

  @Nested
  @DisplayName("DLQ 集成测试")
  class DlqIntegrationTests {

    @Test
    @DisplayName("单条处理失败时应发送到 DLQ")
    void testBatchConsume_singleFail_sendsToDlq() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(3);
      listener.batchHandleShouldFail = true;
      listener.singleHandleFailIndices.add(1); // 第二条消息失败
      listener.setDeadLetterService(deadLetterService);
      doNothing().when(deadLetterService).sendToDeadLetter(any(), any());

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      verify(deadLetterService, times(1)).sendToDeadLetter(any(), any());
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("DLQ 服务未启用时应记录警告但不抛异常")
    void testBatchConsume_dlqNotEnabled_logsWarning() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(2);
      listener.batchHandleShouldFail = true;
      listener.singleHandleFailIndices.add(0);
      // 不设置 deadLetterService

      // When - 不应抛出异常
      listener.handleBatch(records, acknowledgment);

      // Then
      assertThat(listener.singleHandleCallCount.get()).isEqualTo(2);
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("DLQ 发送失败时不应影响主流程")
    void testBatchConsume_dlqSendFails_continuesProcessing() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(3);
      listener.batchHandleShouldFail = true;
      listener.singleHandleFailIndices.add(0);
      listener.singleHandleFailIndices.add(2);
      listener.setDeadLetterService(deadLetterService);
      doThrow(new RuntimeException("DLQ send failed"))
          .when(deadLetterService)
          .sendToDeadLetter(any(), any());

      // When - 不应抛出异常
      listener.handleBatch(records, acknowledgment);

      // Then - 所有记录都被处理尝试
      assertThat(listener.singleHandleCallCount.get()).isEqualTo(3);
      verify(acknowledgment, times(1)).acknowledge();
    }
  }

  @Nested
  @DisplayName("确认机制测试")
  class AcknowledgmentTests {

    @Test
    @DisplayName("正常处理后应确认消息")
    void testAcknowledgment_normalProcessing_acknowledges() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(2);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("批量处理异常后应确认消息")
    void testAcknowledgment_batchException_stillAcknowledges() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(2);
      listener.batchHandleShouldFail = true;

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    @DisplayName("所有单条处理失败后应确认消息")
    void testAcknowledgment_allSingleFail_stillAcknowledges() {
      // Given
      List<ConsumerRecord<String, String>> records = createRecords(2);
      listener.batchHandleShouldFail = true;
      listener.singleHandleFailIndices.add(0);
      listener.singleHandleFailIndices.add(1);

      // When
      listener.handleBatch(records, acknowledgment);

      // Then
      verify(acknowledgment, times(1)).acknowledge();
    }
  }

  @Nested
  @DisplayName("getListenerName 测试")
  class ListenerNameTests {

    @Test
    @DisplayName("getListenerName 应返回类名")
    void testGetListenerName_returnsClassName() {
      // When
      String name = listener.getListenerName();

      // Then
      assertThat(name).isEqualTo("TestBatchListener");
    }
  }

  /**
   * 创建测试用的 ConsumerRecord 列表
   *
   * @param count 记录数量
   * @return ConsumerRecord 列表
   */
  private List<ConsumerRecord<String, String>> createRecords(int count) {
    List<ConsumerRecord<String, String>> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      records.add(
          new ConsumerRecord<>(
              "test-topic", // topic
              0, // partition
              i, // offset
              "key-" + i, // key
              "value-" + i // value
              ));
    }
    return records;
  }

  /**
   * 测试用的批量监听器实现
   *
   * <p>用于验证 AbstractBatchKafkaListener 的行为
   */
  static class TestBatchListener extends AbstractBatchKafkaListener<String> {

    /** 标记 doBatchHandle 是否被调用 */
    boolean batchHandleCalled = false;

    /** 标记 doBatchHandle 是否应该失败 */
    boolean batchHandleShouldFail = false;

    /** 单条处理调用次数 */
    AtomicInteger singleHandleCallCount = new AtomicInteger(0);

    /** 指定哪些索引的单条处理应该失败 */
    List<Integer> singleHandleFailIndices = new CopyOnWriteArrayList<>();

    /** 已处理的批量记录 */
    List<ConsumerRecord<String, String>> processedBatchRecords = new CopyOnWriteArrayList<>();

    /** 已处理的单条记录 */
    List<ConsumerRecord<String, String>> processedSingleRecords = new CopyOnWriteArrayList<>();

    /** DLQ 服务 */
    private DeadLetterService dlqService;

    void setDeadLetterService(DeadLetterService dlqService) {
      this.dlqService = dlqService;
      // 使用反射设置父类的 deadLetterService 字段
      try {
        java.lang.reflect.Field field =
            AbstractBatchKafkaListener.class.getDeclaredField("deadLetterService");
        field.setAccessible(true);
        field.set(this, dlqService);
      } catch (Exception e) {
        throw new RuntimeException("Failed to set deadLetterService", e);
      }
    }

    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
      batchHandleCalled = true;
      if (batchHandleShouldFail) {
        throw new RuntimeException("Batch handle failed");
      }
      processedBatchRecords.addAll(records);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
      int index = singleHandleCallCount.getAndIncrement();
      if (singleHandleFailIndices.contains(index)) {
        throw new RuntimeException("Single handle failed for index " + index);
      }
      processedSingleRecords.add(record);
    }
  }
}
