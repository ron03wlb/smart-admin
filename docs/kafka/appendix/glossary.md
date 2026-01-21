# Glossary

Comprehensive glossary of terms used in SmartAdmin Kafka integration documentation.

## A

### Acknowledgment (Ack)
Confirmation from Kafka broker that a message has been received and stored. Producer can configure acknowledgment level: `0` (none), `1` (leader), `all` (all replicas).

**Example**: `acks=all` ensures message is replicated to all in-sync replicas before acknowledging.

### At-Least-Once Delivery
Delivery semantics where each message is delivered one or more times. Duplicates are possible if producer retries. Default behavior without idempotence.

### At-Most-Once Delivery
Delivery semantics where each message is delivered zero or one time. Message loss is possible if producer doesn't retry on failure.

### Auto-Offset-Reset
Consumer configuration determining what to do when no initial offset exists or current offset is invalid. Values: `earliest` (read from beginning), `latest` (skip to end), `none` (throw exception).

---

## B

### Batch Processing
Processing multiple messages together in a single operation, improving throughput. In SmartAdmin, implemented via `AbstractBatchKafkaListener`.

**Example**: Processing 100 employee records in single database transaction.

### Bootstrap Servers
Comma-separated list of Kafka broker addresses used for initial connection. Format: `host1:port1,host2:port2`.

**Example**: `localhost:9092` (dev) or `kafka-1:9092,kafka-2:9092,kafka-3:9092` (prod)

### Broker
Kafka server that stores messages and serves client requests. A Kafka cluster consists of multiple brokers for high availability.

---

## C

### Commit
Saving the consumer's current position (offset) in a partition. Can be automatic (periodic) or manual (application-controlled).

### Consumer
Application that reads messages from Kafka topics. Belongs to a consumer group for load balancing.

### Consumer Group
Set of consumers that cooperate to consume messages from topics. Each partition is consumed by exactly one consumer in the group.

**Example**: `order-processor` group with 3 consumers processing `smart-admin-order` topic with 3 partitions.

### Consumer Lag
Number of messages a consumer is behind the latest message in a partition. Calculated as: `log-end-offset - current-offset`.

**Monitoring**: `kafka-consumer-groups --describe --group order-processor`

---

## D

### Dead Letter Queue (DLQ)
Special topic where failed messages are sent for later analysis and recovery. In SmartAdmin, DLQ topic is named `{original-topic}-dlq`.

**Example**: `smart-admin-order-dlq` for failed `smart-admin-order` messages.

### Deserialization
Converting byte array from Kafka into Java object. Default: `StringDeserializer` for text messages.

---

## E

### Exactly-Once Semantics (EOS)
Delivery guarantee where each message is processed exactly once, eliminating duplicates. Requires idempotent producer and transactions.

**Configuration**: `enable.idempotence=true` + `transactional.id`

---

## F

### Fetch
Consumer operation to retrieve messages from broker. Configured by `fetch.min.bytes` and `fetch.max.wait.ms`.

---

## G

### Graceful Degradation
Fallback mechanism in `AbstractBatchKafkaListener` that switches from batch processing to single-message processing on batch failure.

**Flow**: Batch processing → (error) → Individual processing → (error) → DLQ

### Group ID
Unique identifier for a consumer group. Multiple consumers with same group ID share partition assignments.

---

## H

### High Water Mark (HWM)
Offset of the last message that was successfully replicated to all in-sync replicas. Only messages up to HWM are visible to consumers.

---

## I

### Idempotence
Property where producing the same message multiple times has the same effect as producing it once. Prevents duplicates.

**Producer config**: `enable.idempotence=true`

**Implementation strategies**:
- Redis cache with TTL
- Database unique constraints
- Kafka transactions
- Message versioning

### In-Sync Replica (ISR)
Set of replicas that are fully caught up with the leader partition. Required for `acks=all` acknowledgments.

---

## K

### Kafka Streams
Stream processing library built on Kafka. Not currently used in SmartAdmin (uses Spring Kafka instead).

### KafkaListener
Spring annotation that marks a method as a Kafka message listener.

**Example**:
```java
@KafkaListener(topics = "smart-admin-order", groupId = "order-processor")
public void onMessage(ConsumerRecord<String, String> record) { }
```

### KafkaProducerService
SmartAdmin service class wrapping `KafkaTemplate` with convenience methods: `send()`, `sendAsync()`, `sendBatchAsync()`.

### KafkaTemplate
Spring Kafka class for sending messages to Kafka topics. Thread-safe and reusable.

### KRaft (Kafka Raft)
Kafka's new consensus protocol that removes ZooKeeper dependency. Used in Kafka 3.3+.

**Mode**: `process.roles=broker,controller`

---

## L

### Leader
Partition replica that handles all reads and writes. Other replicas are followers that replicate from leader.

### Linger
Time producer waits before sending a batch, allowing more messages to accumulate. Configured by `linger.ms`.

**Trade-off**: Higher linger → better batching → higher latency

---

## M

### Message
Unit of data in Kafka consisting of key, value, timestamp, and headers. Immutable once written.

**Structure**:
- Key: Optional, used for partitioning
- Value: Message payload (usually JSON)
- Timestamp: When message was created
- Headers: Metadata key-value pairs

### Message Aggregation
Pattern of collecting multiple messages before processing them together. Implemented in SmartAdmin for analytics use cases.

**Triggers**: Count-based (N messages) or time-based (T seconds)

---

## O

### Offset
Sequential ID assigned to each message in a partition. Used by consumers to track position.

**Types**:
- **Current offset**: Consumer's current position
- **Committed offset**: Last saved position
- **Log-end offset**: Latest message in partition

---

## P

### Partition
Ordered, immutable sequence of messages within a topic. Unit of parallelism in Kafka.

**Key properties**:
- Messages in partition are ordered
- Each partition has one leader replica
- Consumers process partitions independently

### Partition Key
Message key used to determine which partition to send message to. Messages with same key go to same partition, preserving order.

**Example**: Use `customerId` as key to ensure all orders for a customer are ordered.

### Producer
Application that publishes messages to Kafka topics.

---

## R

### Rebalancing
Process where Kafka redistributes partition assignments among consumers in a group. Triggered by consumer joins, leaves, or crashes.

**Impact**: Brief pause in message consumption during rebalancing.

### Replication
Copying partition data across multiple brokers for fault tolerance. Configured by `replication.factor`.

**Best practice**: `replication-factor=3` for production

### Replication Factor
Number of copies of each partition across brokers. Minimum replicas for fault tolerance.

**Example**: `replication-factor=3` means each partition has 3 copies on 3 different brokers.

---

## S

### Serialization
Converting Java object into byte array for storage in Kafka. Default: `StringSerializer` for text messages.

### SmartAdmin Kafka
Custom Kafka integration framework for SmartAdmin providing:
- `KafkaProducerService` for sending messages
- `AbstractKafkaListener` base class with DLQ routing
- `AbstractBatchKafkaListener` for batch processing
- Automatic configuration via `smart.kafka.*` properties

---

## T

### Topic
Named stream of messages in Kafka. Logical channel for publishing and subscribing.

**Naming convention**: `smart-admin-{feature-name}`

**Example**: `smart-admin-order`, `smart-admin-notification`

### Transaction
Atomic operation spanning multiple Kafka operations. Enables exactly-once semantics across topics.

**Configuration**: `transactional.id` prefix

---

## Z

### ZooKeeper
Centralized service for maintaining configuration in older Kafka versions. Being replaced by KRaft in Kafka 3.x+.

**Status**: Deprecated in favor of KRaft mode

---

## SmartAdmin-Specific Terms

### AbstractKafkaListener
Base class for single-message Kafka listeners in SmartAdmin. Provides:
- Automatic error handling
- DLQ routing on failures
- Lifecycle hooks (`beforeHandle`, `afterHandle`)
- Structured logging

**Usage**: All consumer listeners must extend this class.

### AbstractBatchKafkaListener
Base class for batch message processing with graceful degradation. Extends `AbstractKafkaListener`.

**Features**:
- Batch processing via `doBatchHandle()`
- Automatic fallback to single-message processing
- 6-10x performance improvement

### DeadLetterMessage
Domain object representing a failed message in DLQ. Contains original message plus error metadata.

**Fields**: `originalTopic`, `originalOffset`, `errorMessage`, `errorStackTrace`, `failedAt`

---

## Configuration Properties

### smart.kafka.enabled
Boolean flag to enable/disable Kafka integration. Must be `true` for Kafka components to load.

**Default**: `false`

### smart.kafka.bootstrap-servers
Kafka broker addresses for SmartAdmin. Environment-specific.

**Example**: `localhost:9092` (dev), `kafka-1:9092,kafka-2:9092` (prod)

---

## Acronyms

| Acronym | Full Name | Description |
|---------|-----------|-------------|
| **ACL** | Access Control List | Kafka security permissions |
| **CDC** | Change Data Capture | Capturing database changes |
| **DLQ** | Dead Letter Queue | Failed message storage |
| **EOS** | Exactly-Once Semantics | Delivery guarantee |
| **HWM** | High Water Mark | Replication threshold |
| **ISR** | In-Sync Replica | Replicas caught up with leader |
| **JMX** | Java Management Extensions | Monitoring interface |
| **KRaft** | Kafka Raft | ZooKeeper replacement |
| **LSO** | Last Stable Offset | Transaction boundary |
| **SASL** | Simple Authentication and Security Layer | Authentication protocol |
| **SSL** | Secure Sockets Layer | Encryption protocol |
| **TLS** | Transport Layer Security | Encryption protocol |

---

## See Also

- [Quick Reference](/kafka/getting-started/quick-reference) - Quick API lookup
- [Configuration Reference](/kafka/reference/configuration-reference) - All properties
- [Architecture Overview](/kafka/architecture/overview) - System design

---

**Last Updated**: 2026-01-22
