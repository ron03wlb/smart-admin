package net.lab1024.sa.admin.module.business.sample.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.batch.MessageAggregator;
import net.lab1024.sa.common.mq.kafka.constant.KafkaConst;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 消息聚合器示例
 *
 * <p>展示如何使用 MessageAggregator 实现消息聚合后批量发送
 *
 * <p>适用场景:
 *
 * <ul>
 *   <li>高频小消息场景（如日志、埋点、监控数据）
 *   <li>需要减少网络开销的场景
 *   <li>对实时性要求不高，但需要高吞吐的场景
 * </ul>
 *
 * <p>工作原理:
 *
 * <ul>
 *   <li>消息累积到指定数量后触发批量发送
 *   <li>或超过指定时间后强制发送（防止消息堆积）
 *   <li>两个条件满足任一即触发发送
 * </ul>
 *
 * <p>配置要求:
 *
 * <pre>{@code
 * smart:
 *   kafka:
 *     enabled: true
 *     batch:
 *       aggregate:
 *         enabled: true      # 启用聚合功能
 *         count: 50          # 聚合数量阈值
 *         timeout-ms: 5000   # 聚合超时时间
 * }</pre>
 *
 * <p>使用方式:
 *
 * <pre>{@code
 * @Resource
 * private KafkaMessageAggregatorSample aggregatorSample;
 *
 * // 添加消息到聚合器（达到阈值或超时后自动批量发送）
 * aggregatorSample.addMessage("日志消息1");
 * aggregatorSample.addMessage("日志消息2");
 * // ...
 *
 * // 查看当前缓冲区大小
 * int bufferSize = aggregatorSample.getBufferSize();
 *
 * // 强制刷新（立即发送所有已聚合的消息）
 * aggregatorSample.flush();
 * }</pre>
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaProducerService.class)
@ConditionalOnProperty(
    prefix = "smart.kafka.batch.aggregate",
    name = "enabled",
    havingValue = "true")
public class KafkaMessageAggregatorSample {

  /** 聚合数量阈值（累积多少条消息后触发批量发送） */
  private static final int AGGREGATE_COUNT = 50;

  /** 聚合超时时间（毫秒，超时后强制发送已聚合的消息） */
  private static final long AGGREGATE_TIMEOUT_MS = 5000;

  @Resource private KafkaProducerService kafkaProducerService;

  /** 消息聚合器 */
  private MessageAggregator<String> messageAggregator;

  /** 初始化聚合器 */
  @PostConstruct
  public void init() {
    messageAggregator =
        new MessageAggregator<>(
            AGGREGATE_COUNT,
            AGGREGATE_TIMEOUT_MS,
            messages -> {
              // 批量发送回调
              log.info("聚合器触发批量发送: messageCount={}", messages.size());
              kafkaProducerService
                  .sendBatchAsync(KafkaConst.Topic.SAMPLE, messages)
                  .whenComplete(
                      (result, ex) -> {
                        if (ex != null) {
                          log.error("聚合消息批量发送异常: {}", ex.getMessage(), ex);
                          return;
                        }
                        if (result.isAllSuccess()) {
                          log.info("聚合消息批量发送成功: total={}", result.getTotal());
                        } else {
                          log.warn(
                              "聚合消息批量发送部分失败: success={}, failed={}",
                              result.getSuccessCount(),
                              result.getFailureCount());
                        }
                      });
            });

    log.info("消息聚合器初始化完成: countThreshold={}, timeoutMs={}", AGGREGATE_COUNT, AGGREGATE_TIMEOUT_MS);
  }

  /** 关闭时刷新剩余消息 */
  @PreDestroy
  public void destroy() {
    if (messageAggregator != null) {
      log.info("关闭消息聚合器，刷新剩余消息...");
      messageAggregator.shutdown();
    }
  }

  /**
   * 添加消息到聚合器
   *
   * <p>消息会被缓存，达到数量阈值或超时后自动触发批量发送
   *
   * @param message 消息内容
   */
  public void addMessage(String message) {
    messageAggregator.add(message);
    log.debug("消息已添加到聚合器: currentBufferSize={}", messageAggregator.getBufferSize());
  }

  /**
   * 强制刷新缓冲区
   *
   * <p>立即发送所有已聚合的消息，不等待阈值或超时
   */
  public void flush() {
    log.info("手动刷新聚合器缓冲区");
    messageAggregator.flush();
  }

  /**
   * 获取当前缓冲区大小
   *
   * @return 缓冲区中的消息数量
   */
  public int getBufferSize() {
    return messageAggregator.getBufferSize();
  }

  /**
   * 批量添加消息示例
   *
   * <p>演示高频场景下的消息聚合
   *
   * @param count 消息数量
   */
  public void addTestMessages(int count) {
    log.info("开始添加 {} 条测试消息到聚合器", count);
    for (int i = 0; i < count; i++) {
      String message = String.format("{\"id\":%d,\"timestamp\":%d}", i, System.currentTimeMillis());
      addMessage(message);
    }
    log.info("测试消息添加完成: addedCount={}, currentBufferSize={}", count, getBufferSize());
  }
}
