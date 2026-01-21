package net.lab1024.sa.common.mq.kafka.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Kafka 消费者抽象基类
 *
 * <p>提供消息处理的模板方法，包含异常处理和手动确认逻辑
 *
 * <p>使用示例:
 *
 * <pre>{@code
 * @Component
 * public class OrderKafkaListener extends AbstractKafkaListener<OrderMessage> {
 *
 *     @KafkaListener(topics = KafkaConst.Topic.ORDER, groupId = KafkaConst.Group.ORDER)
 *     public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
 *         handleMessage(record, ack);
 *     }
 *
 *     @Override
 *     protected void doHandle(ConsumerRecord<String, String> record) {
 *         // 业务处理逻辑
 *         String message = record.value();
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @param <T> 消息类型
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
public abstract class AbstractKafkaListener<T> {

  /**
   * 处理消息的模板方法
   *
   * <p>包含完整的异常处理和手动确认逻辑
   *
   * @param record 消息记录
   * @param ack 确认对象
   */
  protected void handleMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();
    String key = record.key();

    try {
      if (log.isDebugEnabled()) {
        log.debug(
            "Kafka 开始处理消息: topic={}, partition={}, offset={}, key={}",
            topic,
            partition,
            offset,
            key);
      }

      // 调用子类实现的业务处理方法
      doHandle(record);

      // 处理成功，手动确认
      ack.acknowledge();

      if (log.isDebugEnabled()) {
        log.debug(
            "Kafka 消息处理成功并已确认: topic={}, partition={}, offset={}, key={}",
            topic,
            partition,
            offset,
            key);
      }
    } catch (Exception ex) {
      handleException(record, ex);
      // 异常处理后也确认消息，避免消息堆积
      // 如需重试，应在 doHandle 中实现或使用死信队列
      ack.acknowledge();
    }
  }

  /**
   * 业务处理方法（子类实现）
   *
   * @param record 消息记录
   */
  protected abstract void doHandle(ConsumerRecord<String, String> record);

  /**
   * 异常处理方法（可重写）
   *
   * <p>默认记录错误日志，子类可重写实现自定义异常处理（如发送到死信队列）
   *
   * @param record 消息记录
   * @param ex 异常
   */
  protected void handleException(ConsumerRecord<String, String> record, Exception ex) {
    log.error(
        "Kafka 消息处理异常: topic={}, partition={}, offset={}, key={}, value={}, error={}",
        record.topic(),
        record.partition(),
        record.offset(),
        record.key(),
        record.value(),
        ex.getMessage(),
        ex);
  }

  /**
   * 获取当前消费者名称（用于日志）
   *
   * @return 消费者名称
   */
  protected String getListenerName() {
    return this.getClass().getSimpleName();
  }
}
