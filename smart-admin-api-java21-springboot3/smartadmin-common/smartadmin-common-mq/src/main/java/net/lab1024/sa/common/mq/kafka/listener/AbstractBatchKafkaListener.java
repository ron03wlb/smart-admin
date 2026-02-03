package net.lab1024.sa.common.mq.kafka.listener;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.dlq.DeadLetterService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Kafka 批量消费者抽象基类
 *
 * <p>提供批量消息处理的模板方法，包含批量处理、降级单条处理、DLQ 发送等逻辑
 *
 * <p>使用示例:
 *
 * <pre>{@code
 * @Component
 * public class OrderBatchListener extends AbstractBatchKafkaListener<Order> {
 *
 *     @KafkaListener(
 *         topics = KafkaConst.Topic.ORDER,
 *         groupId = KafkaConst.Group.ORDER,
 *         containerFactory = "batchKafkaListenerContainerFactory")
 *     public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
 *         handleBatch(records, ack);
 *     }
 *
 *     @Override
 *     protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
 *         // 批量处理逻辑（如批量插入数据库）
 *         List<Order> orders = records.stream()
 *             .map(r -> parseOrder(r.value()))
 *             .toList();
 *         orderService.batchInsert(orders);
 *     }
 *
 *     @Override
 *     protected void doHandle(ConsumerRecord<String, String> record) {
 *         // 单条处理逻辑（降级时使用）
 *         Order order = parseOrder(record.value());
 *         orderService.insert(order);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 消息类型
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@SuppressWarnings("PMD.GuardLogStatement") // SLF4J 占位符已优化性能
public abstract class AbstractBatchKafkaListener<T> {

  @Autowired(required = false)
  private DeadLetterService deadLetterService;

  /**
   * 处理批量消息的模板方法
   *
   * <p>处理流程: 1. 尝试批量处理 {@link #doBatchHandle} 2. 批量处理失败时降级为逐条处理 {@link #doHandle} 3. 逐条处理失败的消息发送到
   * DLQ 4. 统一确认
   *
   * @param records 消息记录列表
   * @param ack 确认对象
   */
  protected void handleBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    if (records == null || records.isEmpty()) {
      ack.acknowledge();
      return;
    }

    String topic = records.getFirst().topic();
    int batchSize = records.size();

    try {
      if (log.isDebugEnabled()) {
        log.debug("Kafka 批量处理开始: topic={}, batchSize={}", topic, batchSize);
      }

      // 尝试批量处理
      doBatchHandle(records);

      if (log.isDebugEnabled()) {
        log.debug("Kafka 批量处理成功: topic={}, batchSize={}", topic, batchSize);
      }
    } catch (Exception batchEx) {
      log.warn(
          "Kafka 批量处理失败，降级为逐条处理: topic={}, batchSize={}, error={}",
          topic,
          batchSize,
          batchEx.getMessage());

      // 降级为逐条处理
      handleOneByOne(records);
    } finally {
      // 统一确认
      ack.acknowledge();
    }
  }

  /**
   * 逐条处理消息（批量处理失败时的降级策略）
   *
   * @param records 消息记录列表
   */
  private void handleOneByOne(List<ConsumerRecord<String, String>> records) {
    List<ConsumerRecord<String, String>> failedRecords = new ArrayList<>();

    for (ConsumerRecord<String, String> record : records) {
      try {
        doHandle(record);
      } catch (Exception ex) {
        log.error(
            "Kafka 单条消息处理失败: topic={}, partition={}, offset={}, error={}",
            record.topic(),
            record.partition(),
            record.offset(),
            ex.getMessage(),
            ex);
        failedRecords.add(record);

        // 发送到 DLQ
        sendToDeadLetterQueue(record, ex);
      }
    }

    if (!failedRecords.isEmpty()) {
      log.warn("Kafka 批量降级处理完成: total={}, failed={}", records.size(), failedRecords.size());
    }
  }

  /**
   * 发送消息到死信队列
   *
   * @param record 消息记录
   * @param ex 异常
   */
  private void sendToDeadLetterQueue(ConsumerRecord<String, String> record, Exception ex) {
    if (deadLetterService == null) {
      log.warn("DLQ 服务未启用，无法发送失败消息到死信队列: topic={}, offset={}", record.topic(), record.offset());
      return;
    }

    try {
      deadLetterService.sendToDeadLetter(record, ex);
    } catch (Exception dlqEx) {
      log.error(
          "发送消息到 DLQ 失败: topic={}, partition={}, offset={}, error={}",
          record.topic(),
          record.partition(),
          record.offset(),
          dlqEx.getMessage(),
          dlqEx);
    }
  }

  /**
   * 批量处理消息（子类实现）
   *
   * <p>实现此方法进行高效的批量处理，如批量插入数据库
   *
   * @param records 消息记录列表
   * @throws Exception 处理异常时抛出，将触发降级为逐条处理
   */
  protected abstract void doBatchHandle(List<ConsumerRecord<String, String>> records)
      throws Exception;

  /**
   * 单条处理消息（子类实现）
   *
   * <p>批量处理失败时降级调用此方法逐条处理
   *
   * @param record 消息记录
   * @throws Exception 处理异常时抛出，消息将发送到 DLQ
   */
  protected abstract void doHandle(ConsumerRecord<String, String> record) throws Exception;

  /**
   * 获取当前监听器名称（用于日志）
   *
   * @return 监听器名称
   */
  protected String getListenerName() {
    return this.getClass().getSimpleName();
  }
}
