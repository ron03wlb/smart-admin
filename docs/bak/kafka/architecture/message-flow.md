# Message Flow

Comprehensive sequence diagrams showing how messages flow through SmartAdmin's Kafka integration.

## Overview

This document provides detailed sequence diagrams for all message flows in Smart Admin's Kafka module, including:

- **Producer Flows**: Single send, batch send, callbacks
- **Consumer Flows**: Single processing, batch processing, degradation
- **DLQ Flows**: Error handling and dead letter queue routing
- **End-to-End Flows**: Complete request-response cycles

---

## Producer Message Flows

### Single Message Send (Async)

```mermaid
sequenceDiagram
    participant Controller
    participant Service
    participant Producer as KafkaProducerService
    participant KT as KafkaTemplate
    participant Broker as Kafka Broker

    Controller->>Service: POST /api/order/create
    Service->>Service: Create order entity
    Service->>Producer: send(topic, message)

    Producer->>Producer: Validate parameters
    Producer->>KT: send(topic, key, message)

    KT->>Broker: ProducerRecord
    activate Broker

    Note over Broker: Write to leader<br/>Replicate to followers

    Broker-->>KT: RecordMetadata
    deactivate Broker

    KT->>Producer: ListenableFuture<SendResult>
    Producer->>Producer: Log success callback

    Producer-->>Service: void (async)
    Service-->>Controller: ResponseDTO.ok()
```

**Key Points**:
1. **Async by default**: `send()` returns immediately, doesn't block
2. **Callback logging**: Success/failure logged automatically
3. **Fire-and-forget**: Controller doesn't wait for Kafka ack

---

### Single Message Send with Callback

```mermaid
sequenceDiagram
    participant Service
    participant Producer as KafkaProducerService
    participant Callback as ProducerCallback
    participant KT as KafkaTemplate
    participant Broker as Kafka Broker

    Service->>Producer: send(topic, msg, callback)
    Producer->>KT: send(topic, key, message)

    KT->>Broker: ProducerRecord
    activate Broker

    alt Success (acks=all)
        Broker-->>KT: RecordMetadata
        deactivate Broker
        KT->>Callback: onSuccess(SendResult)
        Callback->>Callback: Business logic<br/>(update DB, metrics, etc.)
    else Failure (retries exhausted)
        Broker-->>KT: Exception
        deactivate Broker
        KT->>Callback: onFailure(Throwable)
        Callback->>Callback: Handle error<br/>(alert, compensation, etc.)
    end

    Producer-->>Service: void
```

**Key Points**:
1. **Custom callback**: Handle success/failure in business logic
2. **Non-blocking**: Callback executed asynchronously
3. **Use cases**: Metrics collection, DB updates, alerting

---

### Batch Send (Parallel)

```mermaid
sequenceDiagram
    participant Service
    participant Producer as KafkaProducerService
    participant KT as KafkaTemplate
    participant Broker as Kafka Broker

    Service->>Producer: sendBatchAsync(topic, [msg1, msg2, msg3])
    activate Producer

    Producer->>Producer: Create 3 CompletableFutures

    par Send msg1
        Producer->>KT: send(topic, msg1)
        KT->>Broker: ProducerRecord 1
        Broker-->>KT: RecordMetadata 1
        KT-->>Producer: Future1
    and Send msg2
        Producer->>KT: send(topic, msg2)
        KT->>Broker: ProducerRecord 2
        Broker-->>KT: RecordMetadata 2
        KT-->>Producer: Future2
    and Send msg3
        Producer->>KT: send(topic, msg3)
        KT->>Broker: ProducerRecord 3
        Broker-->>KT: RecordMetadata 3
        KT-->>Producer: Future3
    end

    Producer->>Producer: CompletableFuture.allOf(futures)
    Producer->>Producer: Collect all results

    deactivate Producer
    Producer-->>Service: CompletableFuture<List<SendResult>>

    Service->>Service: .thenAccept(results -> log)
```

**Key Points**:
1. **Parallel execution**: All messages sent concurrently
2. **CompletableFuture.allOf()**: Waits for all to complete
3. **Preserves order**: Results match input order
4. **Performance**: 3-5x faster than sequential

---

## Consumer Message Flows

### Single Message Processing (AbstractKafkaListener)

```mermaid
sequenceDiagram
    participant Broker as Kafka Broker
    participant Listener as @KafkaListener
    participant Abstract as AbstractKafkaListener
    participant Impl as doHandle()
    participant DLQ as DeadLetterService
    participant Business as Business Logic

    Broker->>Listener: Poll ConsumerRecord
    Listener->>Abstract: handleMessage(record)
    activate Abstract

    Abstract->>Abstract: Log: Processing message

    Abstract->>Impl: doHandle(record)
    activate Impl

    alt Success Path
        Impl->>Business: Process message
        Business-->>Impl: Success
        Impl-->>Abstract: Return normally
        Abstract->>Abstract: Log: Successfully processed
        deactivate Impl
        Abstract->>Broker: Auto-commit offset
    else Failure Path (Exception)
        Impl->>Business: Process message
        Business-->>Impl: Throw exception
        Impl-->>Abstract: Throw exception
        deactivate Impl
        Abstract->>Abstract: Log error with stack trace
        Abstract->>DLQ: sendToDeadLetter(record, ex)
        DLQ->>Broker: Produce to {topic}-dlq
        Abstract->>Broker: Auto-commit offset<br/>(message handled)
    end

    deactivate Abstract
```

**Key Points**:
1. **Template method**: `handleMessage()` wraps `doHandle()`
2. **Auto-DLQ**: Exceptions automatically route to DLQ
3. **Always commits**: Offset committed even on failure (DLQ handles it)
4. **No message loss**: Failed messages preserved in DLQ

---

### Batch Processing with Degradation

```mermaid
sequenceDiagram
    participant Broker as Kafka Broker
    participant Listener as @KafkaListener
    participant Abstract as AbstractBatchKafkaListener
    participant Batch as doBatchHandle()
    participant Single as doHandle()
    participant DLQ as DeadLetterService

    Broker->>Listener: Poll List<ConsumerRecord> (N=100)
    Listener->>Abstract: handleBatch(records, ack)
    activate Abstract

    Abstract->>Abstract: Log: Processing batch of 100

    Abstract->>Batch: doBatchHandle(records)
    activate Batch

    alt Batch Success
        Batch->>Batch: Batch DB operation
        Batch-->>Abstract: Return normally
        deactivate Batch
        Abstract->>Abstract: ack.acknowledge()
        Abstract->>Abstract: Log: Successfully processed 100
    else Batch Failure
        Batch->>Batch: Batch DB operation fails
        Batch-->>Abstract: Throw exception
        deactivate Batch
        Abstract->>Abstract: Log: Degrading to single mode

        Abstract->>Abstract: degradeToSingleProcessing()

        loop Each of 100 messages
            Abstract->>Single: doHandle(record)
            activate Single
            alt Single Success
                Single-->>Abstract: Return
                deactivate Single
                Abstract->>Abstract: successCount++
            else Single Failure
                Single-->>Abstract: Throw exception
                deactivate Single
                Abstract->>DLQ: sendToDeadLetter(record, ex)
                Abstract->>Abstract: failureCount++
            end
        end

        Abstract->>Abstract: ack.acknowledge()
        Abstract->>Abstract: Log: 95 success, 5 failed
    end

    deactivate Abstract
```

**Key Points**:
1. **Try batch first**: Optimal for performance
2. **Graceful degradation**: Falls back to single-message mode
3. **Partial success**: Some messages succeed, failures go to DLQ
4. **Always acknowledges**: Manual ack after all processing complete

---

## Dead Letter Queue Flows

### DLQ Message Creation and Routing

```mermaid
sequenceDiagram
    participant Consumer as Consumer Logic
    participant DLQSvc as DeadLetterService
    participant Builder as DeadLetterMessage
    participant Producer as KafkaProducerService
    participant Broker as Kafka Broker

    Consumer->>DLQSvc: sendToDeadLetter(topic, key, value, error)
    activate DLQSvc

    DLQSvc->>DLQSvc: Calculate DLQ topic<br/>{topic}-dlq

    DLQSvc->>Builder: Build DeadLetterMessage
    activate Builder
    Builder->>Builder: Set originalTopic
    Builder->>Builder: Set originalKey
    Builder->>Builder: Set originalValue
    Builder->>Builder: Set errorMessage
    Builder->>Builder: Set stackTrace
    Builder->>Builder: Set timestamp
    Builder->>Builder: Set attemptCount=1
    Builder-->>DLQSvc: DeadLetterMessage
    deactivate Builder

    DLQSvc->>DLQSvc: Serialize to JSON

    DLQSvc->>Producer: send(dlqTopic, key, json)
    Producer->>Broker: ProducerRecord

    Broker-->>Producer: Ack
    Producer-->>DLQSvc: Success

    DLQSvc->>DLQSvc: Log DLQ routing

    deactivate DLQSvc
```

**DLQ Message Structure**:
```json
{
  "originalTopic": "smart-admin-order",
  "originalKey": "ORDER-123",
  "originalValue": "{\"orderId\": 123, \"amount\": 100}",
  "errorMessage": "Database connection failed",
  "stackTrace": "java.sql.SQLException: ...",
  "timestamp": 1706745600000,
  "attemptCount": 1,
  "metadata": {
    "consumerGroup": "order-group",
    "partition": "2",
    "offset": "12345"
  }
}
```

---

### DLQ Manual Processing Flow

```mermaid
sequenceDiagram
    participant Admin as Admin/Operator
    participant DLQConsumer as DLQ Consumer
    participant Broker as Kafka Broker (DLQ Topic)
    participant Analysis as Error Analysis
    participant Fix as Fix & Retry
    participant Original as Original Topic

    Admin->>DLQConsumer: Start manual DLQ processing
    DLQConsumer->>Broker: Poll from {topic}-dlq

    loop For each DLQ message
        Broker->>DLQConsumer: DeadLetterMessage (JSON)
        DLQConsumer->>Analysis: Analyze error

        alt Transient Error (DB down, network)
            Analysis->>Fix: Wait for recovery
            Fix->>Fix: Verify service healthy
            Fix->>Original: Republish to original topic
            Original-->>Fix: Success
        else Data Error (invalid format)
            Analysis->>Fix: Transform/correct data
            Fix->>Original: Republish corrected message
        else Business Rule Violation
            Analysis->>Admin: Manual intervention required
            Admin->>Admin: Decide: fix, discard, or escalate
        end

        DLQConsumer->>Broker: Commit DLQ offset
    end
```

**Key Points**:
1. **Manual inspection**: Operator reviews each failure
2. **Error categorization**: Transient vs permanent errors
3. **Selective retry**: Only valid messages replayed
4. **Audit trail**: DLQ messages preserved for analysis

---

## End-to-End Flows

### Complete Request-Response Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Producer as KafkaProducerService
    participant Broker as Kafka Broker
    participant Consumer as OrderListener
    participant OrderService

    Client->>Controller: POST /api/order/create
    Controller->>Service: createOrder(form)

    Service->>Service: Validate form
    Service->>Service: Create OrderEntity
    Service->>Service: Save to database

    Service->>Producer: send("orders", orderJson)
    Producer->>Broker: ProducerRecord

    Service-->>Controller: ResponseDTO.ok("Order created")
    Controller-->>Client: 200 OK {"code": 1}

    Note over Broker: Message stored<br/>in Kafka cluster

    Broker->>Consumer: Poll messages
    Consumer->>Consumer: handleMessage(record)
    Consumer->>Consumer: doHandle(record)

    Consumer->>OrderService: processOrder(order)
    OrderService->>OrderService: Send email notification
    OrderService->>OrderService: Update inventory
    OrderService->>OrderService: Generate invoice

    OrderService-->>Consumer: Complete
    Consumer->>Broker: Commit offset
```

**Timing**:
- Client receives response: ~50ms (database write time)
- Kafka receives message: ~5ms (async)
- Consumer processes message: ~200ms (email, inventory, invoice)
- **Total**: Client unblocked at 50ms, background work continues

---

### Error Recovery Flow

```mermaid
sequenceDiagram
    participant Broker as Kafka Broker
    participant Consumer
    participant Service
    participant Database
    participant DLQ

    Broker->>Consumer: Message (attempt 1)
    Consumer->>Service: Process message
    Service->>Database: INSERT order
    Database-->>Service: Connection timeout
    Service-->>Consumer: Throw SQLException

    Consumer->>DLQ: Send to DLQ
    Consumer->>Broker: Commit offset

    Note over DLQ: Message preserved<br/>for manual retry

    rect rgb(255, 240, 240)
        Note over Database: Database issue fixed
    end

    Admin->>DLQ: Inspect failed messages
    DLQ->>Admin: Show: "SQLException: Connection timeout"

    Admin->>Admin: Verify DB healthy
    Admin->>Broker: Republish to original topic

    Broker->>Consumer: Message (attempt 2)
    Consumer->>Service: Process message
    Service->>Database: INSERT order
    Database-->>Service: Success
    Service-->>Consumer: Complete

    Consumer->>Broker: Commit offset
```

**Key Points**:
1. **No automatic retry**: Avoids retry storms
2. **DLQ preservation**: Failed messages never lost
3. **Manual intervention**: Operator decides when to retry
4. **Root cause fix**: Address underlying issue before retry

---

## Message Aggregation Flow

### Time or Count-Based Aggregation

```mermaid
sequenceDiagram
    participant Broker
    participant Consumer as OrderListener
    participant Aggregator as MessageAggregator
    participant Timer as ScheduledExecutor
    participant Callback
    participant Service as OrderService

    loop Messages arrive
        Broker->>Consumer: Message 1
        Consumer->>Aggregator: add(order1, callback)
        Aggregator->>Aggregator: buffer.add(order1)<br/>count=1

        Broker->>Consumer: Message 2
        Consumer->>Aggregator: add(order2, callback)
        Aggregator->>Aggregator: buffer.add(order2)<br/>count=2

        Note over Aggregator: ... 48 more messages

        Broker->>Consumer: Message 50
        Consumer->>Aggregator: add(order50, callback)
        Aggregator->>Aggregator: buffer.add(order50)<br/>count=50

        Aggregator->>Aggregator: Check: count >= 50?
        Aggregator->>Callback: Fire callback(50 orders)
        Callback->>Service: batchProcess(orders)
        Service->>Service: Bulk DB insert
        Service-->>Callback: Complete
        Aggregator->>Aggregator: Clear buffer
    end

    alt Timeout scenario (count < 50)
        Note over Timer: 5 seconds elapsed
        Timer->>Aggregator: Timeout trigger
        Aggregator->>Aggregator: Check buffer: count=25
        Aggregator->>Callback: Fire callback(25 orders)
        Callback->>Service: batchProcess(orders)
        Aggregator->>Aggregator: Clear buffer
    end
```

**Configuration**:
```yaml
smart:
  kafka:
    batch:
      aggregate:
        enabled: true
        count: 50       # Flush when 50 messages
        timeout-ms: 5000 # Or flush after 5 seconds
```

**Trigger Conditions** (whichever comes first):
1. **Count threshold**: Buffer reaches 50 messages
2. **Time threshold**: 5 seconds elapsed since first message

---

## Summary

### Flow Comparison

| Flow Type | Latency | Throughput | Error Handling | Use Case |
|-----------|---------|------------|----------------|----------|
| **Single Send** | ~5ms | 1,000 msg/s | Callback logging | Real-time events |
| **Batch Send** | ~10ms | 5,000 msg/s | Per-message callback | Bulk operations |
| **Single Consumer** | ~10ms | 2,000 msg/s | Auto-DLQ | Standard processing |
| **Batch Consumer** | ~20ms | 10,000 msg/s | Degradation + DLQ | High-throughput |
| **Aggregated Consumer** | ~50ms | 8,000 msg/s | Buffered + DLQ | DB batch inserts |

### Best Practices

1. **Producer**:
   - Use batch send for bulk operations (>10 messages)
   - Implement callbacks for critical messages
   - Monitor send failures via metrics

2. **Consumer**:
   - Extend `AbstractKafkaListener` for reliability
   - Use batch mode for high-throughput scenarios
   - Implement `doHandle()` for degradation fallback

3. **DLQ**:
   - Monitor DLQ topics daily
   - Categorize errors (transient vs permanent)
   - Implement automated retry for known transients

4. **Performance**:
   - Use message aggregation for database writes
   - Tune `max-poll-records` based on processing time
   - Scale consumers via `listener.concurrency`

---

## See Also

- [Architecture Overview](/kafka/architecture/overview) - System architecture
- [Module Structure](/kafka/architecture/module-structure) - Code organization
- [Batch Processing](/kafka/architecture/batch-processing) - Batch design details
- [Dead Letter Queue](/kafka/architecture/dead-letter-queue) - DLQ architecture
- [Producer Guide](/kafka/guides/producer-guide) - Producer patterns
- [Consumer Guide](/kafka/guides/consumer-guide) - Consumer patterns
