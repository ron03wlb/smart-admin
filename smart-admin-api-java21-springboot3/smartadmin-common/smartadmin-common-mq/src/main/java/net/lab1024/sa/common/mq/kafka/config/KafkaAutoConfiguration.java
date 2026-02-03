package net.lab1024.sa.common.mq.kafka.config;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerServiceImpl;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka 自动配置类
 *
 * <p>通过 smart.kafka.enabled=true 启用
 *
 * @author 1024创新实验室
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(prefix = "smart.kafka", name = "enabled", havingValue = "true")
@SuppressWarnings("PMD.GuardLogStatement") // SLF4J 占位符已优化性能
public class KafkaAutoConfiguration {

  /**
   * 创建生产者工厂
   *
   * @param properties Kafka配置属性
   * @return ProducerFactory
   */
  @Bean
  @ConditionalOnMissingBean
  public ProducerFactory<String, String> kafkaProducerFactory(KafkaProperties properties) {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    // 消息不丢失配置
    KafkaProperties.Producer producerConfig = properties.getProducer();
    configProps.put(ProducerConfig.ACKS_CONFIG, producerConfig.getAcks());
    configProps.put(ProducerConfig.RETRIES_CONFIG, producerConfig.getRetries());
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerConfig.isEnableIdempotence());

    // 顺序保证配置
    configProps.put(
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
        producerConfig.getMaxInFlightRequestsPerConnection());

    // 性能配置
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerConfig.getBatchSize());
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producerConfig.getBufferMemory());
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerConfig.getLingerMs());

    log.info("Kafka Producer 配置完成: bootstrapServers={}", properties.getBootstrapServers());
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * 创建 KafkaTemplate
   *
   * @param producerFactory 生产者工厂
   * @return KafkaTemplate
   */
  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, String> kafkaTemplate(
      ProducerFactory<String, String> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  /**
   * 创建消费者工厂
   *
   * @param properties Kafka配置属性
   * @return ConsumerFactory
   */
  @Bean
  @ConditionalOnMissingBean
  public ConsumerFactory<String, String> kafkaConsumerFactory(KafkaProperties properties) {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
    configProps.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    configProps.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    // 消费者配置
    KafkaProperties.Consumer consumerConfig = properties.getConsumer();
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerConfig.getGroupId());
    configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerConfig.isEnableAutoCommit());
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getAutoOffsetReset());
    configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerConfig.getMaxPollRecords());
    configProps.put(
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerConfig.getMaxPollIntervalMs());
    configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerConfig.getSessionTimeoutMs());
    configProps.put(
        ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerConfig.getHeartbeatIntervalMs());

    log.info(
        "Kafka Consumer 配置完成: bootstrapServers={}, groupId={}",
        properties.getBootstrapServers(),
        consumerConfig.getGroupId());
    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  /**
   * 创建死信队列错误处理器
   *
   * @param kafkaTemplate KafkaTemplate
   * @param properties Kafka配置属性
   * @return DefaultErrorHandler
   */
  @Bean
  @ConditionalOnProperty(
      prefix = "smart.kafka.dead-letter-queue",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties) {

    KafkaProperties.DeadLetterQueue dlqConfig = properties.getDeadLetterQueue();
    KafkaProperties.Retry retryConfig = dlqConfig.getRetry();

    // DeadLetterPublishingRecoverer - 发送到 DLQ topic
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(record.topic() + dlqConfig.getTopicSuffix(), -1));

    // 指数退避重试策略
    ExponentialBackOff backOff = new ExponentialBackOff();
    backOff.setInitialInterval(retryConfig.getInitialInterval());
    backOff.setMaxInterval(retryConfig.getMaxInterval());
    backOff.setMultiplier(retryConfig.getMultiplier());
    backOff.setMaxElapsedTime(calculateMaxElapsedTime(retryConfig));

    // 创建 ErrorHandler
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 不重试的异常类型（直接进 DLQ）
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class, NullPointerException.class);

    log.info(
        "Kafka DLQ ErrorHandler 配置完成: maxAttempts={}, initialInterval={}ms, multiplier={}",
        retryConfig.getMaxAttempts(),
        retryConfig.getInitialInterval(),
        retryConfig.getMultiplier());

    return errorHandler;
  }

  /**
   * 计算最大重试时间
   *
   * @param retryConfig 重试配置
   * @return 最大重试时间（毫秒）
   */
  private long calculateMaxElapsedTime(KafkaProperties.Retry retryConfig) {
    long totalTime = 0;
    long interval = retryConfig.getInitialInterval();
    for (int i = 0; i < retryConfig.getMaxAttempts(); i++) {
      totalTime += Math.min(interval, retryConfig.getMaxInterval());
      interval = (long) (interval * retryConfig.getMultiplier());
    }
    return totalTime + 1000; // 额外留 1 秒缓冲
  }

  /**
   * 创建监听器容器工厂
   *
   * @param consumerFactory 消费者工厂
   * @param properties Kafka配置属性
   * @param errorHandler 错误处理器（可选）
   * @return ConcurrentKafkaListenerContainerFactory
   */
  @Bean
  @ConditionalOnMissingBean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      KafkaProperties properties,
      @Autowired(required = false) CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);

    // 监听器配置
    KafkaProperties.Listener listenerConfig = properties.getListener();
    factory.setConcurrency(listenerConfig.getConcurrency());
    factory.getContainerProperties().setAckMode(listenerConfig.getAckMode());

    // 设置错误处理器
    if (errorHandler != null) {
      factory.setCommonErrorHandler(errorHandler);
      log.info("Kafka Listener 已配置 DLQ 错误处理器");
    }

    log.info(
        "Kafka Listener 配置完成: ackMode={}, concurrency={}",
        listenerConfig.getAckMode(),
        listenerConfig.getConcurrency());
    return factory;
  }

  /**
   * 创建批量监听器容器工厂
   *
   * <p>启用批量消费模式，需要配置 smart.kafka.batch.enabled=true
   *
   * @param consumerFactory 消费者工厂
   * @param properties Kafka配置属性
   * @param errorHandler 错误处理器（可选）
   * @return ConcurrentKafkaListenerContainerFactory 批量监听器工厂
   */
  @Bean
  @ConditionalOnProperty(prefix = "smart.kafka.batch", name = "enabled", havingValue = "true")
  public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      KafkaProperties properties,
      @Autowired(required = false) CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);

    // 启用批量消费模式
    factory.setBatchListener(true);

    // 批量配置
    KafkaProperties.Batch batchConfig = properties.getBatch();
    factory.setConcurrency(batchConfig.getConcurrency());

    // 使用 MANUAL_IMMEDIATE 确认模式，由业务代码控制确认时机
    factory
        .getContainerProperties()
        .setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

    // 设置错误处理器
    if (errorHandler != null) {
      factory.setCommonErrorHandler(errorHandler);
      log.info("Kafka Batch Listener 已配置 DLQ 错误处理器");
    }

    log.info(
        "Kafka Batch Listener 配置完成: batchSize={}, concurrency={}",
        batchConfig.getSize(),
        batchConfig.getConcurrency());
    return factory;
  }

  /**
   * 创建生产者服务
   *
   * @param kafkaTemplate KafkaTemplate
   * @return KafkaProducerService
   */
  @Bean
  @ConditionalOnMissingBean
  public KafkaProducerService kafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
    log.info("Kafka ProducerService 初始化完成");
    return new KafkaProducerServiceImpl(kafkaTemplate);
  }
}
