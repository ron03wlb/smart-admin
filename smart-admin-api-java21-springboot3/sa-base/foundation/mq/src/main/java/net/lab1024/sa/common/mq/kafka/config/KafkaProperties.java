package net.lab1024.sa.common.mq.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

/**
 * Kafka 配置属性类
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Data
@ConfigurationProperties(prefix = "smart.kafka")
public class KafkaProperties {

  /** 是否启用 Kafka（默认关闭） */
  private boolean enabled = false;

  /** Kafka 集群地址 */
  private String bootstrapServers = "localhost:9092";

  /** 生产者配置 */
  private Producer producer = new Producer();

  /** 消费者配置 */
  private Consumer consumer = new Consumer();

  /** 监听器配置 */
  private Listener listener = new Listener();

  /** 死信队列配置 */
  private DeadLetterQueue deadLetterQueue = new DeadLetterQueue();

  /** 批量处理配置 */
  private Batch batch = new Batch();

  /** 生产者配置 */
  @Data
  public static class Producer {

    /** 确认模式: all 表示所有副本确认 */
    private String acks = "all";

    /** 失败重试次数 */
    private int retries = 3;

    /** 启用幂等性 */
    private boolean enableIdempotence = true;

    /** 单个连接最大未确认请求数（设为1保证顺序） */
    private int maxInFlightRequestsPerConnection = 1;

    /** 批量发送大小（字节） */
    private int batchSize = 16384;

    /** 缓冲区内存大小（字节） */
    private int bufferMemory = 33554432;

    /** 发送延迟（毫秒） */
    private int lingerMs = 5;
  }

  /** 消费者配置 */
  @Data
  public static class Consumer {

    /** 消费者组ID */
    private String groupId = "smart-admin-group";

    /** 是否启用自动提交（建议关闭以保证消息不丢失） */
    private boolean enableAutoCommit = false;

    /** 偏移量重置策略 */
    private String autoOffsetReset = "earliest";

    /** 单次拉取最大记录数 */
    private int maxPollRecords = 500;

    /** 最大拉取间隔（毫秒） */
    private int maxPollIntervalMs = 300000;

    /** 会话超时时间（毫秒） */
    private int sessionTimeoutMs = 45000;

    /** 心跳间隔（毫秒） */
    private int heartbeatIntervalMs = 3000;
  }

  /** 监听器配置 */
  @Data
  public static class Listener {

    /** 确认模式: MANUAL_IMMEDIATE 手动立即确认 */
    private AckMode ackMode = AckMode.MANUAL_IMMEDIATE;

    /** 并发消费者数量 */
    private int concurrency = 3;
  }

  /** 死信队列配置 */
  @Data
  public static class DeadLetterQueue {

    /** 是否启用 DLQ */
    private boolean enabled = true;

    /** DLQ topic 后缀 */
    private String topicSuffix = ".dlq";

    /** 重试配置 */
    private Retry retry = new Retry();
  }

  /** 重试配置 */
  @Data
  public static class Retry {

    /** 最大重试次数 */
    private int maxAttempts = 3;

    /** 初始重试间隔 (ms) */
    private long initialInterval = 1000;

    /** 最大重试间隔 (ms) */
    private long maxInterval = 10000;

    /** 退避乘数 (指数退避) */
    private double multiplier = 2.0;
  }

  /** 批量处理配置 */
  @Data
  public static class Batch {

    /** 是否启用批量消费模式 */
    private boolean enabled = false;

    /** 批量消费的最大消息数量 */
    private int size = 100;

    /** 监听器并发数（批量模式下） */
    private int concurrency = 3;

    /** 消息聚合配置 */
    private Aggregate aggregate = new Aggregate();
  }

  /** 消息聚合配置 */
  @Data
  public static class Aggregate {

    /** 是否启用消息聚合 */
    private boolean enabled = false;

    /** 聚合消息数量阈值（达到此数量触发批量处理） */
    private int count = 50;

    /** 聚合超时时间（毫秒，超时后强制处理已聚合的消息） */
    private long timeoutMs = 5000;
  }
}
