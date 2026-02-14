package net.lab1024.sa.business.sample.kafka;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.KafkaConst;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import net.lab1024.sa.common.mq.kafka.listener.AbstractBatchKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 批量消费者示例
 *
 * <p>展示如何继承 AbstractBatchKafkaListener 实现批量消息消费
 *
 * <p>特点:
 *
 * <ul>
 *   <li>批量消费模式，提高吞吐量
 *   <li>批量处理失败时自动降级为逐条处理
 *   <li>逐条处理失败的消息自动发送到 DLQ
 *   <li>手动确认模式，确保消息不丢失
 * </ul>
 *
 * <p>配置要求:
 *
 * <pre>{@code
 * smart:
 *   kafka:
 *     enabled: true
 *     batch:
 *       enabled: true    # 必须启用批量模式
 *       size: 100        # 批量消费大小
 *       concurrency: 3   # 并发消费者数量
 * }</pre>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaProducerService.class)
@ConditionalOnProperty(prefix = "smart.kafka.batch", name = "enabled", havingValue = "true")
public class KafkaBatchConsumerSample extends AbstractBatchKafkaListener<String> {

  /**
   * 構造函數
   *
   * @param deadLetterService DLQ 服務（可選）
   */
  public KafkaBatchConsumerSample(
      @org.springframework.lang.Nullable
          net.lab1024.sa.common.mq.kafka.dlq.DeadLetterService deadLetterService) {
    super(deadLetterService);
  }

  /**
   * 批量监听通知 Topic
   *
   * <p>使用 batchKafkaListenerContainerFactory 启用批量消费模式
   *
   * @param records 消息记录列表
   * @param ack 确认对象
   */
  @KafkaListener(
      topics = KafkaConst.Topic.NOTIFICATION,
      groupId = KafkaConst.Group.NOTIFICATION,
      containerFactory = "batchKafkaListenerContainerFactory")
  public void onBatchMessage(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    // 调用父类模板方法处理批量消息
    // 处理流程: 批量处理 -> 失败则降级逐条处理 -> 失败消息发送 DLQ -> 统一确认
    handleBatch(records, ack);
  }

  /**
   * 批量处理逻辑
   *
   * <p>实现高效的批量处理，如批量插入数据库、批量调用外部 API 等
   *
   * <p>如果此方法抛出异常，将自动降级为逐条处理（调用 doHandle）
   *
   * @param records 消息记录列表
   * @throws Exception 处理异常时抛出，将触发降级处理
   */
  @Override
  protected void doBatchHandle(List<ConsumerRecord<String, String>> records) throws Exception {
    log.info("开始批量处理通知消息: batchSize={}", records.size());

    // 示例：提取所有消息内容
    List<String> notifications = records.stream().map(ConsumerRecord::value).toList();

    // TODO: 在此实现批量业务逻辑
    // 例如：批量插入数据库
    // notificationService.batchInsert(notifications);

    // 示例：模拟批量处理
    processBatchNotifications(notifications);

    log.info("批量处理通知消息完成: processedCount={}", records.size());
  }

  /**
   * 单条处理逻辑（降级时使用）
   *
   * <p>当批量处理失败时，系统会逐条调用此方法处理每条消息
   *
   * <p>如果此方法也抛出异常，消息将被发送到死信队列（DLQ）
   *
   * @param record 消息记录
   * @throws Exception 处理异常时抛出，消息将发送到 DLQ
   */
  @Override
  protected void doHandle(ConsumerRecord<String, String> record) throws Exception {
    String notification = record.value();
    String key = record.key();

    log.info("降级处理单条通知消息: key={}", key);

    // TODO: 在此实现单条业务逻辑
    // 例如：单条插入数据库
    // notificationService.insert(notification);

    // 示例：模拟单条处理
    processSingleNotification(key, notification);
  }

  /**
   * 批量处理通知的业务逻辑示例
   *
   * @param notifications 通知消息列表
   */
  private void processBatchNotifications(List<String> notifications) {
    // 示例业务处理
    log.debug("处理 {} 条通知消息", notifications.size());

    // 模拟批量处理可能的异常场景
    // if (notifications.size() > 1000) {
    //     throw new RuntimeException("批量处理超过限制，降级为逐条处理");
    // }
  }

  /**
   * 单条处理通知的业务逻辑示例
   *
   * @param key 消息 key
   * @param notification 通知内容
   */
  private void processSingleNotification(String key, String notification) {
    // 示例业务处理
    log.debug("处理通知消息: key={}, notification={}", key, notification);

    // 模拟处理可能的异常场景
    // if (notification.contains("error")) {
    //     throw new RuntimeException("消息处理失败，将发送到 DLQ");
    // }
  }
}
