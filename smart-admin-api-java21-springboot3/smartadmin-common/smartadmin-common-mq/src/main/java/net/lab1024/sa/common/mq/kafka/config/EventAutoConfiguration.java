package net.lab1024.sa.common.mq.kafka.config;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.core.KafkaProducerService;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for domain event infrastructure
 *
 * <p>Registers DomainEventPublisher when Kafka is enabled.
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "smart.kafka", name = "enabled", havingValue = "true")
public class EventAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(KafkaProducerService.class)
  public DomainEventPublisher domainEventPublisher(KafkaProducerService kafkaProducerService) {
    log.info("DomainEventPublisher initialized");
    return new DomainEventPublisher(kafkaProducerService);
  }
}
