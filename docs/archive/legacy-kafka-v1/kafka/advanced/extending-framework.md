# Extending Framework

Framework extension points and customization guide for SmartAdmin's Kafka integration.

## Overview

**SmartAdmin Kafka Extension Points**:
- Custom serializers/deserializers
- Custom partitioners
- Interceptors (producer/consumer)
- Custom error handlers
- Message transformers
- Listener container customization

**When to extend**:
- Project-specific serialization formats (Avro, Protobuf)
- Custom routing logic
- Cross-cutting concerns (auditing, encryption)
- Integration with existing infrastructure

---

## Extension Point 1: Custom Serializers

### Use Case

Custom message format (e.g., Protobuf, Avro):

**Protobuf Serializer**:
```java
package net.lab1024.sa.common.mq.kafka.serializer;

import com.google.protobuf.Message;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ProtobufSerializer implements Serializer<Message> {

    private static final Logger log = LoggerFactory.getLogger(ProtobufSerializer.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Configuration if needed
    }

    @Override
    public byte[] serialize(String topic, Message data) {
        if (data == null) {
            return null;
        }

        try {
            byte[] bytes = data.toByteArray();
            log.debug("Serialized Protobuf message | Topic: {} | Size: {} bytes", topic, bytes.length);
            return bytes;
        } catch (Exception e) {
            log.error("Failed to serialize Protobuf message | Topic: {}", topic, e);
            throw new SerializationException("Protobuf serialization failed", e);
        }
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}
```

**Protobuf Deserializer**:
```java
package net.lab1024.sa.common.mq.kafka.serializer;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ProtobufDeserializer<T extends Message> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(ProtobufDeserializer.class);

    private Parser<T> parser;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String parserClassName = (String) configs.get("protobuf.parser.class");
        if (parserClassName != null) {
            try {
                Class<?> parserClass = Class.forName(parserClassName);
                this.parser = (Parser<T>) parserClass.getMethod("parser").invoke(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to initialize Protobuf parser", e);
            }
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            T message = parser.parseFrom(data);
            log.debug("Deserialized Protobuf message | Topic: {} | Size: {} bytes", topic, data.length);
            return message;
        } catch (Exception e) {
            log.error("Failed to deserialize Protobuf message | Topic: {}", topic, e);
            throw new SerializationException("Protobuf deserialization failed", e);
        }
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}
```

**Configuration**:
```yaml
smart:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: net.lab1024.sa.common.mq.kafka.serializer.ProtobufSerializer

    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: net.lab1024.sa.common.mq.kafka.serializer.ProtobufDeserializer
      properties:
        protobuf.parser.class: com.example.proto.OrderProto.Order
```

---

## Extension Point 2: Custom Partitioners

### Use Case

Custom routing logic (e.g., route VIP customers to dedicated partitions):

**Implementation**:
```java
package net.lab1024.sa.common.mq.kafka.partitioner;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

@Slf4j
public class VipCustomerPartitioner implements Partitioner {

    private static final int VIP_PARTITION = 0;  // Dedicated partition for VIP

    @Override
    public void configure(Map<String, ?> configs) {
        // Configuration if needed
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {

        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (numPartitions == 0) {
            throw new IllegalArgumentException("Topic has no partitions: " + topic);
        }

        // Check if customer is VIP
        if (isVipCustomer(value, valueBytes)) {
            log.debug("Routing VIP customer to partition {} | Topic: {}", VIP_PARTITION, topic);
            return VIP_PARTITION;
        }

        // Regular customers: Hash-based partitioning
        if (keyBytes == null) {
            return Utils.toPositive(Utils.murmur2(valueBytes)) % numPartitions;
        }

        int partition = Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
        log.debug("Routing regular customer to partition {} | Topic: {}", partition, topic);
        return partition;
    }

    private boolean isVipCustomer(Object value, byte[] valueBytes) {
        try {
            String jsonStr = value != null ? value.toString() : new String(valueBytes);
            JSONObject json = JSON.parseObject(jsonStr);
            Boolean isVip = json.getBoolean("isVip");
            return Boolean.TRUE.equals(isVip);
        } catch (Exception e) {
            log.warn("Failed to parse VIP status, routing to regular partition", e);
            return false;
        }
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}
```

**Configuration**:
```yaml
smart:
  kafka:
    producer:
      partitioner-class: net.lab1024.sa.common.mq.kafka.partitioner.VipCustomerPartitioner
```

---

## Extension Point 3: Producer Interceptors

### Use Case

Add metadata, encryption, or auditing to all messages:

**Implementation**:
```java
package net.lab1024.sa.common.mq.kafka.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class AuditProducerInterceptor implements ProducerInterceptor<String, String> {

    private String applicationName;

    @Override
    public void configure(Map<String, ?> configs) {
        this.applicationName = (String) configs.getOrDefault("application.name", "smart-admin");
    }

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        // Add audit headers
        Headers headers = record.headers();
        headers.add("x-source-app", applicationName.getBytes(StandardCharsets.UTF_8));
        headers.add("x-sent-at", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        headers.add("x-correlation-id", generateCorrelationId().getBytes(StandardCharsets.UTF_8));

        log.debug("📤 Producer interceptor | Topic: {} | Key: {} | Headers added",
            record.topic(), record.key());

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.error("❌ Message send failed | Topic: {} | Partition: {} | Offset: {}",
                metadata.topic(), metadata.partition(), metadata.offset(), exception);
        } else {
            log.debug("✅ Message acknowledged | Topic: {} | Partition: {} | Offset: {}",
                metadata.topic(), metadata.partition(), metadata.offset());
        }
    }

    @Override
    public void close() {
        log.info("Audit producer interceptor closed");
    }

    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
}
```

**Configuration**:
```yaml
smart:
  kafka:
    producer:
      properties:
        interceptor.classes: net.lab1024.sa.common.mq.kafka.interceptor.AuditProducerInterceptor
        application.name: ${spring.application.name}
```

---

## Extension Point 4: Consumer Interceptors

### Use Case

Decrypt messages, validate signatures, extract metadata:

**Implementation**:
```java
package net.lab1024.sa.common.mq.kafka.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class DecryptionConsumerInterceptor implements ConsumerInterceptor<String, String> {

    private EncryptionService encryptionService;

    @Override
    public void configure(Map<String, ?> configs) {
        // Initialize encryption service
        String encryptionKey = (String) configs.get("encryption.key");
        this.encryptionService = new EncryptionService(encryptionKey);
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        log.debug("📥 Consumer interceptor | Records: {}", records.count());

        // Check if decryption is needed
        records.forEach(record -> {
            Header encryptedHeader = record.headers().lastHeader("x-encrypted");
            if (encryptedHeader != null && "true".equals(new String(encryptedHeader.value()))) {
                log.debug("🔓 Decrypting message | Topic: {} | Key: {}", record.topic(), record.key());
                // Note: Cannot modify record in-place, decryption must happen in listener
            }
        });

        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        log.debug("✅ Offsets committed | Partitions: {}", offsets.size());
    }

    @Override
    public void close() {
        log.info("Decryption consumer interceptor closed");
    }
}
```

---

## Extension Point 5: Custom Error Handler

### Use Case

Organization-specific error handling strategy:

**Implementation**:
```java
package net.lab1024.sa.common.mq.kafka.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.service.KafkaProducerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("smartAdminKafkaErrorHandler")
@Slf4j
@RequiredArgsConstructor
public class SmartAdminKafkaErrorHandler implements KafkaListenerErrorHandler {

    private final KafkaProducerService kafkaProducerService;
    private final AlertService alertService;

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
        log.error("❌ Kafka listener error | Message: {}", message, exception);

        ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) message.getPayload();

        // Analyze exception type
        Throwable cause = exception.getCause();

        // Critical errors: Alert team
        if (isCriticalError(cause)) {
            alertService.sendAlert("Critical Kafka error: " + cause.getMessage());
        }

        // Send to DLQ
        sendToDLQ(record, exception);

        // Return null to acknowledge message
        return null;
    }

    private boolean isCriticalError(Throwable cause) {
        return cause instanceof OutOfMemoryError
            || cause instanceof StackOverflowError
            || cause.getMessage().contains("CRITICAL");
    }

    private void sendToDLQ(ConsumerRecord<?, ?> record, Exception e) {
        String dlqTopic = record.topic() + "-dlq";
        String key = record.key() != null ? record.key().toString() : null;
        String value = record.value() != null ? record.value().toString() : null;

        kafkaProducerService.send(dlqTopic, key, value);
        log.warn("⚠️ Message sent to DLQ | Topic: {} | Key: {}", dlqTopic, key);
    }
}
```

**Usage**:
```java
@KafkaListener(
    topics = KafkaConst.Topic.ORDER,
    groupId = KafkaConst.Group.ORDER,
    errorHandler = "smartAdminKafkaErrorHandler"  // Reference custom error handler
)
public void onMessage(ConsumerRecord<String, String> record) {
    // Processing logic
}
```

---

## Extension Point 6: Listener Container Customization

### Use Case

Custom concurrency, error handling, or retry configuration per listener:

**Implementation**:
```java
@Configuration
public class CustomKafkaListenerConfig {

    @Bean("highThroughputContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> highThroughputFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(10);  // 10 concurrent consumers
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        // Custom error handler
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L)  // 3 retries with 1s delay
        ));

        return factory;
    }

    @Bean("lowLatencyContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> lowLatencyFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);  // Single consumer for ordering
        factory.getContainerProperties().setPollTimeout(100);  // Low poll timeout
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
```

**Usage**:
```java
// High throughput listener
@KafkaListener(
    topics = KafkaConst.Topic.ORDER,
    groupId = "order-high-throughput",
    containerFactory = "highThroughputContainerFactory"
)
public void processHighVolume(ConsumerRecord<String, String> record) {
    // Process with high concurrency
}

// Low latency listener
@KafkaListener(
    topics = KafkaConst.Topic.PAYMENT,
    groupId = "payment-low-latency",
    containerFactory = "lowLatencyContainerFactory"
)
public void processLowLatency(ConsumerRecord<String, String> record) {
    // Process with low latency
}
```

---

## Plugin Architecture Pattern

### Pluggable Message Processor

**Interface**:
```java
package net.lab1024.sa.common.mq.kafka.plugin;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface MessageProcessor {

    /**
     * Check if this processor can handle the message
     */
    boolean canProcess(ConsumerRecord<String, String> record);

    /**
     * Process the message
     */
    void process(ConsumerRecord<String, String> record);

    /**
     * Plugin priority (lower = higher priority)
     */
    default int getPriority() {
        return 100;
    }
}
```

**Plugin Manager**:
```java
@Component
@Slf4j
public class MessageProcessorManager {

    private final List<MessageProcessor> processors;

    public MessageProcessorManager(List<MessageProcessor> processors) {
        this.processors = processors.stream()
            .sorted(Comparator.comparingInt(MessageProcessor::getPriority))
            .toList();

        log.info("Registered {} message processors", processors.size());
    }

    public void processMessage(ConsumerRecord<String, String> record) {
        for (MessageProcessor processor : processors) {
            if (processor.canProcess(record)) {
                log.debug("Processing with: {}", processor.getClass().getSimpleName());
                processor.process(record);
                return;
            }
        }

        log.warn("No processor found for message | Topic: {} | Key: {}",
            record.topic(), record.key());
    }
}
```

**Example Plugins**:
```java
@Component
public class OrderMessageProcessor implements MessageProcessor {

    @Override
    public boolean canProcess(ConsumerRecord<String, String> record) {
        return KafkaConst.Topic.ORDER.equals(record.topic());
    }

    @Override
    public void process(ConsumerRecord<String, String> record) {
        log.info("Processing order: {}", record.key());
        // Order processing logic
    }

    @Override
    public int getPriority() {
        return 10;  // High priority
    }
}

@Component
public class PaymentMessageProcessor implements MessageProcessor {

    @Override
    public boolean canProcess(ConsumerRecord<String, String> record) {
        return KafkaConst.Topic.PAYMENT.equals(record.topic());
    }

    @Override
    public void process(ConsumerRecord<String, String> record) {
        log.info("Processing payment: {}", record.key());
        // Payment processing logic
    }

    @Override
    public int getPriority() {
        return 5;  // Higher priority than orders
    }
}
```

---

## Best Practices

### 1. Document Extension Points

```java
/**
 * Extension point for custom partitioning logic.
 *
 * To implement:
 * 1. Implement org.apache.kafka.clients.producer.Partitioner
 * 2. Configure: smart.kafka.producer.partitioner-class
 *
 * Example: Route VIP customers to dedicated partitions
 */
```

### 2. Provide Configuration Defaults

```java
@Override
public void configure(Map<String, ?> configs) {
    this.timeout = (Integer) configs.getOrDefault("custom.timeout", 5000);
    this.retries = (Integer) configs.getOrDefault("custom.retries", 3);
}
```

### 3. Add Metrics

```java
@Override
public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
    meterRegistry.counter("kafka.interceptor.messages.sent",
        "topic", record.topic()
    ).increment();
    return record;
}
```

### 4. Handle Errors Gracefully

```java
@Override
public int partition(String topic, Object key, byte[] keyBytes,
                     Object value, byte[] valueBytes, Cluster cluster) {
    try {
        return customPartitionLogic(value);
    } catch (Exception e) {
        log.warn("Custom partition logic failed, falling back to default", e);
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }
}
```

---

## See Also

- [Custom Listeners](/kafka/advanced/custom-listeners) - Custom listener patterns
- [Configuration](/kafka/guides/configuration) - Configuration reference
- [Best Practices](/kafka/guides/best-practices) - Production recommendations

---

**Last Updated**: 2026-01-22
