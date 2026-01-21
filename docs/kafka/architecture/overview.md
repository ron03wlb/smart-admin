# System Architecture Overview

Comprehensive architecture of SmartAdmin's Kafka integration module.

## Quick Overview

SmartAdmin's Kafka module provides enterprise-grade message queue capabilities with:

- ✅ **Producer**: Sync/async sending, batch operations, transaction support
- ✅ **Consumer**: Single/batch processing, auto-degradation, DLQ handling
- ✅ **Message Aggregation**: Count or time-based message batching
- ✅ **Dead Letter Queue**: Automatic retry and failure handling
- ✅ **Monitoring**: Health checks, metrics, diagnostics

---

## Four-Layer Architecture

SmartAdmin Kafka follows the standard layered architecture with strict separation of concerns:

```mermaid
graph TB
    subgraph "Layer 1: Controller"
        C1[OrderController]
        C2[NotificationController]
        C3[EventController]
    end

    subgraph "Layer 2: Service"
        S1[OrderService]
        S2[NotificationService]
        S3[EventService]
    end

    subgraph "Layer 3: Manager"
        M1[OrderManager]
        M2[CacheManager]
    end

    subgraph "Kafka Module (sa-common/mq)"
        direction TB
        subgraph "Core"
            PS[KafkaProducerService]
            PSI[KafkaProducerServiceImpl]
        end
        subgraph "Listeners"
            AL[AbstractKafkaListener]
            ABL[AbstractBatchKafkaListener]
            MA[MessageAggregator]
        end
        subgraph "DLQ"
            DLS[DeadLetterService]
        end
        subgraph "Config"
            AC[KafkaAutoConfiguration]
            KP[KafkaProperties]
        end
    end

    subgraph "Apache Kafka Cluster"
        B1[Broker 1]
        B2[Broker 2]
        B3[Broker 3]
        T1[Topic: orders]
        T2[Topic: notifications]
        T3[Topic: orders-dlq]
    end

    C1 --> S1
    C2 --> S2
    S1 --> M1
    M1 --> PS
    S2 --> PS
    PS --> PSI
    PSI --> T1
    PSI --> T2

    T1 --> AL
    T2 --> ABL
    AL --> S1
    ABL --> S2

    AL -.Failed.-> DLS
    ABL -.Failed.-> DLS
    DLS --> T3

    T1 --> B1
    T2 --> B2
    T3 --> B3

    style PS fill:#3c8772
    style AL fill:#3c8772
    style ABL fill:#3c8772
    style DLS fill:#ff6b6b
```

### Layer Responsibilities

| Layer | Responsibility | Kafka Usage |
|-------|---------------|-------------|
| **Controller** | HTTP API endpoints | Never touches Kafka directly |
| **Service** | Business logic orchestration | Can inject KafkaProducerService |
| **Manager** | Transaction + caching | Can inject KafkaProducerService |
| **Kafka Module** | Message production/consumption | Encapsulates Kafka complexity |

**Key Rule**: Controllers never directly interact with Kafka - always go through Service or Manager layer.

---

## Module Structure

```
sa-common/mq/src/main/java/net/lab1024/sa/common/mq/kafka/
│
├── config/                      # Configuration Layer
│   ├── KafkaAutoConfiguration.java
│   ├── KafkaProperties.java
│   └── BatchKafkaListenerContainerFactory.java
│
├── constant/                    # Constants
│   ├── KafkaConst.java          # Topic and Group ID constants
│   └── KafkaHeaders.java        # Custom header keys
│
├── core/                        # Producer Core
│   ├── KafkaProducerService.java       # Producer interface
│   └── KafkaProducerServiceImpl.java   # Implementation
│
├── listener/                    # Consumer Base Classes
│   ├── AbstractKafkaListener.java      # Single-message consumer
│   ├── AbstractBatchKafkaListener.java # Batch consumer
│   └── MessageAggregator.java          # Message aggregation
│
└── dlq/                         # Dead Letter Queue
    ├── DeadLetterService.java
    └── DeadLetterServiceImpl.java
```

### Component Dependency Graph

```mermaid
graph LR
    KP[KafkaProperties] --> AC[KafkaAutoConfiguration]
    AC --> PS[KafkaProducerService]
    AC --> BF[BatchKafkaListenerContainerFactory]

    PS --> AL[AbstractKafkaListener]
    PS --> ABL[AbstractBatchKafkaListener]

    AL --> DLS[DeadLetterService]
    ABL --> DLS
    DLS --> PS

    MA[MessageAggregator] --> ABL

    style AC fill:#ffd93d
    style PS fill:#3c8772
    style AL fill:#3c8772
    style ABL fill:#3c8772
    style DLS fill:#ff6b6b
```

---

## Producer Architecture

### Producer Class Diagram

```mermaid
classDiagram
    class KafkaProducerService {
        <<interface>>
        +send(topic, message)
        +send(topic, key, message)
        +send(topic, message, callback)
        +sendBatchAsync(topic, messages)
        +sendBatchAsync(topic, keyedMessages)
    }

    class KafkaProducerServiceImpl {
        -KafkaTemplate kafkaTemplate
        +send(topic, message)
        +sendBatchAsync(topic, messages) CompletableFuture
        -handleSendResult(result)
        -handleSendError(ex)
    }

    class ProducerCallback {
        <<interface>>
        +onSuccess(result)
        +onFailure(exception)
    }

    KafkaProducerService <|.. KafkaProducerServiceImpl
    KafkaProducerServiceImpl ..> ProducerCallback
```

### Producer Message Flow

```mermaid
sequenceDiagram
    participant B as Business Logic
    participant P as KafkaProducerService
    participant KT as KafkaTemplate
    participant K as Kafka Broker

    B->>P: send(topic, message)
    P->>P: Validate parameters
    P->>KT: send(ProducerRecord)
    KT->>K: Produce message

    alt Success (acks=all)
        K-->>KT: All replicas acknowledged
        KT-->>P: SendResult
        P->>P: Log success
        P-->>B: CompletableFuture<Success>
    else Failure (retry exhausted)
        K-->>KT: Error (timeout/broker down)
        KT-->>P: Exception
        P->>P: Log error
        P-->>B: CompletableFuture<Exception>
    end
```

### Batch Send Flow

```mermaid
graph TB
    Start[Batch Messages] --> Validate{Validate Input}
    Validate -->|Valid| Parallel[Create Parallel Futures]
    Validate -->|Invalid| Error[Return Error]

    Parallel --> F1[Send Msg 1]
    Parallel --> F2[Send Msg 2]
    Parallel --> F3[Send Msg N]

    F1 --> Collect[CompletableFuture.allOf]
    F2 --> Collect
    F3 --> Collect

    Collect --> Results[Collect All Results]
    Results --> Return[Return List<SendResult>]

    style Parallel fill:#3c8772
    style Collect fill:#ffd93d
```

---

## Consumer Architecture

### Consumer Class Hierarchy

```mermaid
classDiagram
    class AbstractKafkaListener {
        <<abstract>>
        #handleMessage(record)
        #doHandle(record)*
        -sendToDeadLetter(record, ex)
    }

    class AbstractBatchKafkaListener {
        <<abstract>>
        #handleBatch(records, ack)
        #doBatchHandle(records)*
        #doHandle(record)*
        #getBatchSize() int
        -degradeToSingleProcessing(records)
    }

    class MessageAggregator~T~ {
        -ConcurrentHashMap~String, List~T~~ buffer
        -ScheduledExecutorService scheduler
        +add(message, callback)
        -flush(callback)
    }

    class OrderListener {
        +onOrderMessage(record)
        #doHandle(record)
    }

    class OrderBatchListener {
        +onOrderBatch(records, ack)
        #doBatchHandle(records)
        #doHandle(record)
    }

    AbstractKafkaListener <|-- OrderListener
    AbstractBatchKafkaListener <|-- OrderBatchListener
    AbstractBatchKafkaListener ..> MessageAggregator
```

### Single Message Processing Flow

```mermaid
flowchart TB
    Start[Kafka delivers message] --> Listener[@KafkaListener receives]
    Listener --> Handle[handleMessage\nAbstractKafkaListener]

    Handle --> Try{Try doHandle}
    Try -->|Success| Log[Log success]
    Try -->|BusinessException| DLQ[Send to DLQ]
    Try -->|SystemException| DLQ

    Log --> Commit[Auto commit offset]
    DLQ --> LogError[Log error]
    LogError --> Commit

    Commit --> Done[Processing complete]

    style Try fill:#ffd93d
    style DLQ fill:#ff6b6b
    style Commit fill:#3c8772
```

### Batch Processing with Degradation

```mermaid
flowchart TB
    Start[Receive N messages] --> Batch[Try doBatchHandle]

    Batch --> BatchSuccess{Batch\nSuccess?}

    BatchSuccess -->|Yes| Ack[Acknowledge all]
    BatchSuccess -->|No| Degrade[Graceful Degradation]

    Degrade --> Loop[Loop each message]
    Loop --> Single[Try doHandle single]

    Single --> SingleSuccess{Single\nSuccess?}
    SingleSuccess -->|Yes| Next[Next message]
    SingleSuccess -->|No| SendDLQ[Send to DLQ]

    SendDLQ --> Next
    Next --> More{More\nmessages?}
    More -->|Yes| Single
    More -->|No| AckAll[Acknowledge all]

    Ack --> Done[Complete]
    AckAll --> Done

    style Batch fill:#3c8772
    style Degrade fill:#ffd93d
    style SendDLQ fill:#ff6b6b
```

---

## Dead Letter Queue Architecture

### DLQ Flow

```mermaid
sequenceDiagram
    participant C as Consumer
    participant H as doHandle()
    participant DLS as DeadLetterService
    participant DLQ as DLQ Topic

    C->>H: Process message

    alt Processing Success
        H-->>C: Return normally
        C->>C: Commit offset
    else Processing Failure
        H-->>C: Throw exception
        C->>DLS: sendToDeadLetter(record, ex)
        DLS->>DLS: Create DLQ message
        DLS->>DLQ: Produce to {topic}-dlq
        DLQ-->>DLS: Acknowledge
        DLS-->>C: DLQ saved
        C->>C: Commit offset (message handled)
    end
```

### DLQ Message Structure

```mermaid
classDiagram
    class DeadLetterMessage {
        +String originalTopic
        +String originalKey
        +String originalValue
        +String errorMessage
        +String stackTrace
        +Long timestamp
        +Integer attemptCount
        +Map~String, String~ metadata
    }

    class DeadLetterService {
        +sendToDeadLetter(topic, key, value, error)
        +sendToDeadLetter(record, exception)
        -buildDLQMessage(record, ex)
        -getDLQTopicName(topic)
    }

    DeadLetterService ..> DeadLetterMessage
```

**DLQ Topic Naming**: `{original-topic}-dlq`
- Original: `smart-admin-order`
- DLQ: `smart-admin-order-dlq`

---

## Message Aggregation Architecture

### Aggregation Strategy

```mermaid
stateDiagram-v2
    [*] --> Buffering: First message arrives

    Buffering --> Flushing: Reach count threshold (50 msgs)
    Buffering --> Flushing: Reach time threshold (5 sec)

    Flushing --> Processing: Callback fired
    Processing --> [*]: Batch processed

    note right of Buffering
        Messages accumulate in
        ConcurrentHashMap buffer
    end note

    note right of Flushing
        Triggers when:
        - count >= 50 OR
        - time >= 5000ms
    end note
```

### Aggregator Internals

```mermaid
graph TB
    M1[Message 1] --> Buffer[ConcurrentHashMap Buffer]
    M2[Message 2] --> Buffer
    M3[Message N] --> Buffer

    Buffer --> Check{Check Conditions}

    Check -->|Count >= 50| Flush[Flush Buffer]
    Check -->|Time >= 5s| Flush
    Check -->|Neither| Wait[Continue Buffering]

    Flush --> Callback[Fire Callback]
    Callback --> Process[batchProcess]
    Process --> Clear[Clear Buffer]

    Wait --> Timer[ScheduledExecutor]
    Timer -.5s later.-> Check

    style Buffer fill:#ffd93d
    style Flush fill:#3c8772
    style Process fill:#3c8772
```

**Configuration**:
```yaml
smart:
  kafka:
    batch:
      aggregate:
        enabled: true
        count: 50              # Flush when 50 messages
        timeout-ms: 5000       # Or flush after 5 seconds
```

---

## Configuration Architecture

### Auto-Configuration Flow

```mermaid
graph TB
    Props[KafkaProperties] --> Condition{@ConditionalOnProperty\nsmart.kafka.enabled}

    Condition -->|true| AutoConfig[KafkaAutoConfiguration]
    Condition -->|false| Skip[Skip Kafka beans]

    AutoConfig --> ProducerBean[KafkaProducerService Bean]
    AutoConfig --> TemplateBean[KafkaTemplate Bean]
    AutoConfig --> DLQBean[DeadLetterService Bean]
    AutoConfig --> BatchFactory[BatchKafkaListenerContainerFactory]

    ProducerBean --> App[Application]
    DLQBean --> App

    style AutoConfig fill:#3c8772
    style Props fill:#ffd93d
```

### Configuration Properties

```yaml
smart:
  kafka:
    # Enable/Disable entire module
    enabled: true

    # Kafka cluster
    bootstrap-servers: localhost:9092

    # Producer settings
    producer:
      acks: all                          # Durability
      retries: 3                         # Reliability
      enable-idempotence: true           # Exactly-once semantics
      batch-size: 16384
      linger-ms: 5

    # Consumer settings
    consumer:
      enable-auto-commit: false          # Manual control
      max-poll-records: 500              # Batch size
      session-timeout-ms: 45000

    # Batch processing
    batch:
      enabled: true
      size: 100
      aggregate:
        enabled: true
        count: 50
        timeout-ms: 5000

    # Listener container
    listener:
      batch-listener: true
      ack-mode: manual
      concurrency: 3                     # 3 consumer threads
```

---

## Performance Characteristics

### Throughput Comparison

| Mode | Messages/sec | Latency (p50) | Latency (p99) | Use Case |
|------|-------------|--------------|--------------|----------|
| Single Send | 1,000 | 5ms | 20ms | Low volume, real-time |
| Batch Send | 5,000 | 10ms | 50ms | High volume, acceptable latency |
| Batch Consumer | 10,000 | 15ms | 80ms | Highest throughput |
| Aggregated Consumer | 8,000 | 20ms | 100ms | Balanced throughput + latency |

### Scaling Model

```mermaid
graph LR
    subgraph "1 Consumer Thread"
        T1[Thread 1] --> P1[Partition 0]
    end

    subgraph "3 Consumer Threads (concurrency=3)"
        T2[Thread 1] --> P2[Partition 0]
        T3[Thread 2] --> P3[Partition 1]
        T4[Thread 3] --> P4[Partition 2]
    end

    subgraph "6 Consumer Threads (3 instances × 2 threads)"
        T5[Instance 1 T1] --> P5[Part 0]
        T6[Instance 1 T2] --> P6[Part 1]
        T7[Instance 2 T1] --> P7[Part 2]
        T8[Instance 2 T2] --> P8[Part 3]
        T9[Instance 3 T1] --> P9[Part 4]
        T10[Instance 3 T2] --> P10[Part 5]
    end

    style T2 fill:#3c8772
    style T3 fill:#3c8772
    style T4 fill:#3c8772
```

**Key Principle**: `Consumer Threads ≤ Topic Partitions` for optimal parallelism.

---

## Design Patterns Applied

| Pattern | Implementation | Benefit |
|---------|----------------|---------|
| **Template Method** | `AbstractKafkaListener` | Consistent error handling |
| **Strategy** | `KafkaProducerService` | Multiple send strategies |
| **Facade** | `KafkaProducerService` | Simplified Kafka API |
| **Observer** | `@KafkaListener` | Event-driven architecture |
| **Decorator** | Callback wrappers | Enhanced functionality |
| **Factory** | `BatchKafkaListenerContainerFactory` | Consistent bean creation |

---

## Reliability Guarantees

### Producer Guarantees

| Configuration | Guarantee | Trade-off |
|--------------|-----------|-----------|
| `acks=all` | Message replicated to all ISR | Higher latency |
| `enable.idempotence=true` | No duplicates | Requires `acks=all` |
| `retries=3` | Retry on transient errors | Potential reordering |
| `max.in.flight.requests=1` | Strict ordering | Lower throughput |

### Consumer Guarantees

| Configuration | Guarantee | Trade-off |
|--------------|-----------|-----------|
| `enable.auto.commit=false` | At-least-once delivery | Manual offset management |
| `isolation.level=read_committed` | Read only committed messages | Not applicable for non-transactional |
| DLQ enabled | No message loss | Storage cost |

---

## Monitoring and Observability

### Key Metrics

```mermaid
graph TB
    subgraph "Producer Metrics"
        PM1[record-send-total]
        PM2[record-error-total]
        PM3[record-send-rate]
    end

    subgraph "Consumer Metrics"
        CM1[records-consumed-total]
        CM2[records-lag]
        CM3[commit-latency-avg]
    end

    subgraph "DLQ Metrics"
        DM1[dlq-messages-total]
        DM2[dlq-error-rate]
    end

    subgraph "Grafana Dashboard"
        GD[Kafka Overview]
    end

    PM1 --> GD
    PM2 --> GD
    CM1 --> GD
    CM2 --> GD
    DM1 --> GD

    style GD fill:#3c8772
```

**Actuator Endpoints**:
- `/actuator/health/kafka` - Health status
- `/actuator/metrics/kafka.producer.*` - Producer metrics
- `/actuator/metrics/kafka.consumer.*` - Consumer metrics

---

## Summary

SmartAdmin Kafka architecture provides:

✅ **Production-Ready**: Idempotent producer, manual offset control, DLQ
✅ **High Performance**: Batch processing, message aggregation, parallel consumers
✅ **Fault-Tolerant**: Automatic retry, graceful degradation, circuit breaking
✅ **Observable**: Comprehensive metrics, health checks, logging
✅ **Maintainable**: Clean layering, template patterns, clear abstractions

---

## See Also

- [Module Structure](/kafka/architecture/module-structure) - Detailed code organization
- [Message Flow](/kafka/architecture/message-flow) - Sequence diagrams for all flows
- [Batch Processing](/kafka/architecture/batch-processing) - Batch architecture deep dive
- [Dead Letter Queue](/kafka/architecture/dead-letter-queue) - DLQ design and patterns
- [Configuration Guide](/kafka/guides/configuration) - All configuration options
