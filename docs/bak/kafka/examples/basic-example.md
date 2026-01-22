# Basic Example

Complete working example of producer and consumer in SmartAdmin.

## Overview

This example demonstrates:
- Sending a simple message with `KafkaProducerService`
- Consuming messages with `AbstractKafkaListener`
- Automatic DLQ routing on errors
- Spring Boot integration

**Time to complete**: 10 minutes

---

## Prerequisites

- SmartAdmin project running
- Kafka broker accessible on `localhost:9092`
- Module `sa-common/mq/kafka` enabled

---

## Step 1: Configuration

**File**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
smart:
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092

    producer:
      acks: 1
      retries: 3

    consumer:
      group-id: smart-admin-basic-example
      auto-offset-reset: earliest
```

---

## Step 2: Domain Objects

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/sample/domain/form/NotificationForm.java`

```java
package net.lab1024.sa.admin.module.business.sample.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "Notification Form")
public class NotificationForm {

    @Schema(description = "User ID")
    @NotBlank(message = "User ID is required")
    private String userId;

    @Schema(description = "Message content")
    @NotBlank(message = "Message is required")
    private String message;

    @Schema(description = "Notification type")
    private String type = "INFO";
}
```

---

## Step 3: Producer (Sending Messages)

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/sample/controller/KafkaBasicController.java`

```java
package net.lab1024.sa.admin.module.business.sample.controller;

import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.sample.domain.form.NotificationForm;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.service.KafkaProducerService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "Kafka Basic Example")
@RestController
@RequestMapping("/business/sample/kafka/basic")
@RequiredArgsConstructor
@Slf4j
public class KafkaBasicController {

    private final KafkaProducerService kafkaProducerService;

    private static final String TOPIC = "smart-admin-notification";

    @Operation(summary = "Send simple notification")
    @PostMapping("/send")
    public ResponseDTO<String> sendNotification(@Valid @RequestBody NotificationForm form) {
        log.info("Sending notification | UserId: {} | Message: {}", form.getUserId(), form.getMessage());

        // Convert to JSON
        String message = JSON.toJSONString(form);

        // Send with user ID as key (ensures order for same user)
        String key = "USER-" + form.getUserId();
        kafkaProducerService.send(TOPIC, key, message);

        log.info("✅ Notification sent successfully | Key: {}", key);
        return ResponseDTO.ok("Notification sent successfully");
    }
}
```

---

## Step 4: Consumer (Receiving Messages)

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/sample/listener/NotificationListener.java`

```java
package net.lab1024.sa.admin.module.business.sample.listener;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.sample.domain.form.NotificationForm;
import net.lab1024.sa.common.mq.kafka.listener.AbstractKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener extends AbstractKafkaListener {

    @KafkaListener(
        topics = "smart-admin-notification",
        groupId = "notification-processor"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        handleMessage(record);
    }

    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        log.info("📥 Received notification | Key: {} | Partition: {} | Offset: {}",
            record.key(), record.partition(), record.offset());

        // Parse message
        NotificationForm notification = JSON.parseObject(record.value(), NotificationForm.class);

        // Process notification
        processNotification(notification);

        log.info("✅ Notification processed | UserId: {}", notification.getUserId());
    }

    private void processNotification(NotificationForm notification) {
        // Simulate processing
        log.info("Processing notification: Type={}, Message={}",
            notification.getType(), notification.getMessage());

        // In real application:
        // - Save to database
        // - Send email/SMS
        // - Push to mobile app
    }
}
```

---

## Step 5: Run the Example

### Start Kafka

```bash
cd smart-admin-api-java21-springboot3/docker
docker-compose up -d kafka
```

### Create Topic

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic smart-admin-notification \
  --partitions 3 --replication-factor 1
```

### Start Application

```bash
./gradlew :sa-admin:bootRun
```

### Send Test Message

**Using Swagger UI**: `http://localhost:1024/swagger-ui.html`

Navigate to: **Kafka Basic Example** → **Send simple notification**

```json
{
  "userId": "12345",
  "message": "Your order has been shipped",
  "type": "ORDER_UPDATE"
}
```

**Using curl**:
```bash
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "12345",
    "message": "Your order has been shipped",
    "type": "ORDER_UPDATE"
  }'
```

---

## Expected Output

**Producer logs**:
```
INFO  KafkaBasicController - Sending notification | UserId: 12345 | Message: Your order has been shipped
INFO  KafkaBasicController - ✅ Notification sent successfully | Key: USER-12345
```

**Consumer logs**:
```
INFO  NotificationListener - 📥 Received notification | Key: USER-12345 | Partition: 2 | Offset: 0
INFO  NotificationListener - Processing notification: Type=ORDER_UPDATE, Message=Your order has been shipped
INFO  NotificationListener - ✅ Notification processed | UserId: 12345
```

---

## Testing Error Handling

### Simulate Processing Error

Modify `NotificationListener.doHandle()`:

```java
@Override
protected void doHandle(ConsumerRecord<String, String> record) {
    NotificationForm notification = JSON.parseObject(record.value(), NotificationForm.class);

    // Simulate error for testing
    if ("ERROR".equals(notification.getType())) {
        throw new RuntimeException("Simulated processing error");
    }

    processNotification(notification);
}
```

### Send Error Message

```bash
curl -X POST http://localhost:1024/business/sample/kafka/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "99999",
    "message": "This will fail",
    "type": "ERROR"
  }'
```

### Check DLQ

```bash
docker exec smart-admin-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic smart-admin-notification-dlq \
  --from-beginning \
  --max-messages 1
```

**Expected DLQ message**:
```json
{
  "originalTopic": "smart-admin-notification",
  "originalPartition": 2,
  "originalOffset": 1,
  "originalKey": "USER-99999",
  "originalValue": "{\"userId\":\"99999\",\"message\":\"This will fail\",\"type\":\"ERROR\"}",
  "errorMessage": "Simulated processing error",
  "failedAt": "2026-01-22T10:30:45"
}
```

---

## Verification Checklist

- [ ] Topic created successfully
- [ ] Application started without errors
- [ ] Message sent via Swagger UI or curl
- [ ] Consumer logs show message received
- [ ] Consumer logs show processing completed
- [ ] Error message sent to DLQ (if tested)

---

## Next Steps

- [Batch Example](/kafka/examples/batch-example) - Process messages in batches
- [DLQ Example](/kafka/examples/dlq-example) - Handle DLQ messages
- [Producer Guide](/kafka/guides/producer-guide) - Advanced producer patterns
- [Consumer Guide](/kafka/guides/consumer-guide) - Advanced consumer patterns

---

**Last Updated**: 2026-01-22
