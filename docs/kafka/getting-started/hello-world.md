# Hello World Example

A complete, runnable example to get you started with Kafka in SmartAdmin.

## Overview

This example demonstrates:
- Sending a simple message
- Receiving and processing messages
- Basic error handling
- Testing with Kafka

## Complete Code

### Controller

```java
package com.smartadmin.example.kafka;

import com.smartadmin.base.common.domain.ResponseDTO;
import com.smartadmin.kafka.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/example/kafka")
@RequiredArgsConstructor
public class HelloWorldController {

    private final KafkaProducerService kafkaProducerService;

    @PostMapping("/hello")
    public ResponseDTO<String> sendHello(@RequestParam String name) {
        String message = "Hello, " + name + "!";
        kafkaProducerService.send("hello-world-topic", message);
        return ResponseDTO.ok("Message sent: " + message);
    }
}
```

### Listener

```java
package com.smartadmin.example.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelloWorldListener {

    @KafkaListener(topics = "hello-world-topic", groupId = "hello-world-group")
    public void handleMessage(String message) {
        log.info("📨 Received message: {}", message);

        // Process the message
        processMessage(message);
    }

    private void processMessage(String message) {
        // Your business logic here
        log.info("✅ Processed message: {}", message);
    }
}
```

### Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092

    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3

    consumer:
      group-id: hello-world-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: true
```

## Running the Example

### 1. Start Kafka

```bash
# Using Docker Compose
docker-compose up -d kafka

# Or start Kafka manually
bin/kafka-server-start.sh config/server.properties
```

### 2. Create Topic (Optional)

```bash
kafka-topics.sh --create \
  --topic hello-world-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

### 3. Start Application

```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:bootRun
```

### 4. Send Test Message

```bash
curl -X POST "http://localhost:1024/api/example/kafka/hello?name=World"
```

Expected response:
```json
{
  "code": 1,
  "msg": "success",
  "data": "Message sent: Hello, World!"
}
```

### 5. Check Logs

You should see in the application logs:
```
📨 Received message: Hello, World!
✅ Processed message: Hello, World!
```

## Testing

### Unit Test

```java
@SpringBootTest
class HelloWorldListenerTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void testSendAndReceive() throws Exception {
        // Send message
        kafkaProducerService.send("hello-world-topic", "Test message");

        // Wait for processing
        Thread.sleep(1000);

        // Verify (check logs or use a test listener)
    }
}
```

## Next Steps

Now that you've sent your first message, explore:

- [Batch Processing](/kafka/examples/batch-example) - Send/receive multiple messages efficiently
- [Error Handling](/kafka/examples/dlq-example) - Handle failures with Dead Letter Queue
- [Producer Guide](/kafka/guides/producer-guide) - Advanced producer patterns
- [Consumer Guide](/kafka/guides/consumer-guide) - Advanced consumer patterns

::: warning Work in Progress
This document is being actively developed. More content coming soon!
:::
