# Kafka & Async Operations Testing Guide

> **Challenge #4**: Testing asynchronous operations and Kafka message handling
> **Last Updated**: 2026-01-22

---

## Table of Contents

1. [Introduction](#introduction)
2. [Testing @Async Methods](#testing-async-methods)
3. [Testing Kafka Producers](#testing-kafka-producers)
4. [Testing Kafka Consumers](#testing-kafka-consumers)
5. [Common Patterns](#common-patterns)
6. [Common Issues](#common-issues)

---

## Introduction

Asynchronous operations complete after the test method returns, creating timing challenges:

- **@Async methods** - Execute in separate thread
- **Kafka producers** - Send messages asynchronously
- **Kafka consumers** - Process messages in background

This guide shows how to test async code reliably.

---

## Testing @Async Methods

### The Async Challenge

```java
@Service
public class NotificationService {

    @Async
    public void sendNotificationAsync(Long userId, String message) {
        // Executes in separate thread
        emailService.send(userId, message);
    }
}
```

**Problem**: Test finishes before async method completes.

```java
// ❌ This test will pass even if sendNotificationAsync fails!
@Test
void testSendNotification() {
    notificationService.sendNotificationAsync(1L, "Hello");
    // Test ends here, but async method still running!
}
```

### Solution 1: Return CompletableFuture

Modify method to return `CompletableFuture`:

```java
@Async
public CompletableFuture<Void> sendNotificationAsync(Long userId, String message) {
    emailService.send(userId, message);
    return CompletableFuture.completedFuture(null);
}
```

**Test:**

```java
@Test
void testSendNotificationAsync_CompletesSuccessfully() throws Exception {
    // When
    CompletableFuture<Void> future = notificationService.sendNotificationAsync(1L, "Hello");

    // Then - wait for completion
    future.get(5, TimeUnit.SECONDS);  // Timeout after 5 seconds

    // Verify
    verify(emailService).send(1L, "Hello");
}
```

### Solution 2: Use CountDownLatch

Use `CountDownLatch` to wait for async completion:

```java
@Test
void testSendNotificationAsync_WithLatch() throws Exception {
    // Given
    CountDownLatch latch = new CountDownLatch(1);

    doAnswer(invocation -> {
        latch.countDown();  // Signal completion
        return null;
    }).when(emailService).send(anyLong(), anyString());

    // When
    notificationService.sendNotificationAsync(1L, "Hello");

    // Then - wait for async to complete
    boolean completed = latch.await(5, TimeUnit.SECONDS);
    assertTrue(completed, "Async method should complete within 5 seconds");

    verify(emailService).send(1L, "Hello");
}
```

### Solution 3: Make @Async Synchronous in Tests

Configure test to run @Async methods synchronously:

```java
@TestConfiguration
static class AsyncTestConfig {

    @Bean
    @Primary  // Override production executor
    public Executor taskExecutor() {
        return new SyncTaskExecutor();  // Executes synchronously
    }
}

@SpringBootTest
@Import(AsyncTestConfig.class)
class NotificationServiceTest {

    @Test
    void testSendNotificationAsync_RunsSynchronously() {
        // When - executes synchronously in test
        notificationService.sendNotificationAsync(1L, "Hello");

        // Then - can verify immediately
        verify(emailService).send(1L, "Hello");
    }
}
```

### Solution 4: Use Awaitility

Use Awaitility library for polling:

```java
@Test
void testAsyncMethod_WithAwaitility() {
    // When
    notificationService.sendNotificationAsync(1L, "Hello");

    // Then - poll until condition met
    await()
        .atMost(5, SECONDS)
        .untilAsserted(() -> {
            verify(emailService).send(1L, "Hello");
        });
}
```

---

## Testing Kafka Producers

### Option 1: Use @EmbeddedKafka

Spring provides embedded Kafka for testing:

```java
@SpringBootTest
@EmbeddedKafka(
    topics = {"smart-admin-sample", "smart-admin-sample.dlq"},
    partitions = 3,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
class KafkaProducerIntTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("sendAsync - sends message successfully")
    void testSendAsync_Success() throws Exception {
        // Given
        String topic = "smart-admin-sample";
        String key = "test-key";
        String message = "Hello Kafka";

        CountDownLatch latch = new CountDownLatch(1);

        // Listen for message
        kafkaTemplate.receive(topic, 0, 0);  // Setup consumer

        // When
        kafkaProducerService.sendAsync(topic, key, message);

        // Then - wait for message to be sent
        boolean sent = latch.await(10, TimeUnit.SECONDS);
        assertTrue(sent, "Message should be sent within 10 seconds");
    }
}
```

### Option 2: Mock KafkaTemplate

For unit tests, mock the Kafka infrastructure:

```java
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void testSendAsync_CallsKafkaTemplate() {
        // Given
        String topic = "test-topic";
        String key = "key1";
        String message = "test message";

        CompletableFuture<SendResult<String, String>> future =
            CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(topic, key, message)).thenReturn(future);

        // When
        kafkaProducerService.sendAsync(topic, key, message);

        // Then
        verify(kafkaTemplate).send(topic, key, message);
    }
}
```

---

## Testing Kafka Consumers

### Testing @KafkaListener

```java
@Service
public class OrderEventListener {

    @KafkaListener(topics = "order-events", groupId = "order-group")
    public void handleOrderEvent(String message) {
        // Process order event
        orderService.processOrder(message);
    }
}
```

**Integration Test:**

```java
@SpringBootTest
@EmbeddedKafka(topics = "order-events")
class OrderEventListenerIntTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("Listener processes message")
    void testHandleOrderEvent_ProcessesMessage() throws Exception {
        // Given
        String message = "{\"orderId\": 123}";
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(orderService).processOrder(anyString());

        // When - send message to topic
        kafkaTemplate.send("order-events", message);

        // Then - wait for listener to process
        boolean processed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(processed, "Listener should process message within 10 seconds");

        verify(orderService).processOrder(message);
    }
}
```

### Testing Error Handling

```java
@Test
@DisplayName("Listener handles error and sends to DLQ")
void testHandleOrderEvent_ErrorSendsToDLQ() throws Exception {
    // Given
    String invalidMessage = "invalid json";

    // Mock to throw exception
    doThrow(new RuntimeException("Parse error"))
        .when(orderService).processOrder(invalidMessage);

    CountDownLatch latch = new CountDownLatch(1);

    // Listen to DLQ
    @KafkaListener(topics = "order-events.dlq", groupId = "test-dlq")
    public void dlqListener(String message) {
        assertEquals(invalidMessage, message);
        latch.countDown();
    }

    // When - send invalid message
    kafkaTemplate.send("order-events", invalidMessage);

    // Then - should appear in DLQ
    boolean sentToDLQ = latch.await(10, TimeUnit.SECONDS);
    assertTrue(sentToDLQ, "Error should send message to DLQ");
}
```

---

## Common Patterns

### Pattern 1: CountDownLatch for Async Verification

```java
@Test
void testAsyncOperation() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    // Setup callback
    doAnswer(invocation -> {
        latch.countDown();
        return null;
    }).when(mockService).callback();

    // Trigger async operation
    asyncService.doSomething();

    // Wait for completion
    assertTrue(latch.await(5, SECONDS));

    // Verify
    verify(mockService).callback();
}
```

### Pattern 2: CompletableFuture.get() with Timeout

```java
@Test
void testAsyncWithFuture() throws Exception {
    // When
    CompletableFuture<String> future = asyncService.processAsync(data);

    // Then
    String result = future.get(3, TimeUnit.SECONDS);
    assertEquals("expected", result);
}
```

### Pattern 3: @Async with Synchronous Executor in Tests

```java
@TestConfiguration
static class TestAsyncConfig {
    @Bean
    @Primary
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
```

### Pattern 4: Awaitility for Polling

```java
@Test
void testAsyncWithPolling() {
    // When
    asyncService.doSomething();

    // Then - poll until condition met
    await()
        .atMost(5, SECONDS)
        .pollInterval(100, MILLISECONDS)
        .untilAsserted(() -> {
            verify(mockService).callback();
        });
}
```

---

## Common Issues

### Issue 1: Test Passes But Async Code Fails

**Cause**: Test completes before async operation finishes

**Solution**: Always wait for async completion

```java
// ❌ Bad - test finishes too early
@Test
void testAsync() {
    asyncService.doSomething();
    // Test ends here!
}

// ✅ Good - wait for completion
@Test
void testAsync() throws Exception {
    CompletableFuture<Void> future = asyncService.doSomething();
    future.get(5, TimeUnit.SECONDS);
}
```

### Issue 2: Flaky Tests (Sometimes Pass, Sometimes Fail)

**Cause**: Race condition - timing varies

**Solution**: Increase timeout and use proper waiting

```java
// ❌ Bad - arbitrary sleep
@Test
void testAsync() {
    asyncService.doSomething();
    Thread.sleep(1000);  // Might not be enough!
}

// ✅ Good - explicit wait with timeout
@Test
void testAsync() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    // ... setup callback
    assertTrue(latch.await(10, SECONDS));  // Generous timeout
}
```

### Issue 3: @EmbeddedKafka Port Conflict

**Cause**: Kafka broker port already in use

**Solution**: Use random port

```java
@EmbeddedKafka(
    topics = "test-topic",
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0"  // Random port
    }
)
```

### Issue 4: Kafka Messages Not Consumed in Test

**Cause**: Consumer not started in time

**Solution**: Wait for consumer to be ready

```java
@BeforeEach
void waitForConsumerReady() throws Exception {
    // Poll until consumer is ready
    await()
        .atMost(30, SECONDS)
        .until(() -> {
            // Check consumer assignment
            return !kafkaListenerEndpointRegistry
                .getListenerContainer("listener-id")
                .getAssignedPartitions()
                .isEmpty();
        });
}
```

---

## Best Practices

### 1. Always Set Timeouts

```java
// ✅ Good
future.get(5, TimeUnit.SECONDS);
latch.await(10, TimeUnit.SECONDS);

// ❌ Bad - could hang forever
future.get();
latch.await();
```

### 2. Use Generous Timeouts in CI

```java
// Local: 5 seconds might be enough
// CI: Use 10-30 seconds (slower environment)

boolean completed = latch.await(30, TimeUnit.SECONDS);
```

### 3. Clean Up Async Resources

```java
@AfterEach
void cleanup() {
    // Shut down thread pools
    asyncExecutor.shutdown();
    asyncExecutor.awaitTermination(5, SECONDS);
}
```

### 4. Test Error Scenarios

```java
@Test
void testAsync_HandlesError() {
    // Verify async error handling
    CompletableFuture<Void> future = asyncService.doSomethingThatFails();

    assertThrows(ExecutionException.class, () -> {
        future.get(5, SECONDS);
    });
}
```

---

## Summary

| Pattern | Use Case | Example |
|---------|----------|---------|
| **CompletableFuture.get()** | Method returns future | `future.get(5, SECONDS)` |
| **CountDownLatch** | Callback-based async | `latch.await(10, SECONDS)` |
| **SyncTaskExecutor** | Make @Async sync in tests | Test configuration bean |
| **Awaitility** | Polling until condition | `await().atMost(5, SECONDS).until(...)` |
| **@EmbeddedKafka** | Kafka integration test | Spring test annotation |

**Key Principle**: Always wait for async operations to complete with proper timeout!

---

## Related Documentation

### SmartAdmin Kafka Testing (Comprehensive)

SmartAdmin has extensive Kafka testing documentation:

- **[Kafka Testing Strategy](../../kafka/testing/testing-strategy.md)** - 6-dimension quality framework
- **[Kafka Unit Testing](../../kafka/testing/unit-testing.md)** - Producer/consumer/listener patterns
- **[Kafka Integration Testing](../../kafka/testing/integration-testing.md)** - End-to-end testing
- **[Kafka Verification Framework](../../kafka/testing/verification-framework.md)** - Complete verification guide

### Other Documentation

- [Integration Testing Quick Reference](../integration-testing-quick-reference.md) - Quick async patterns
- [Testing Strategy](../testing-strategy.md) - Overall testing philosophy

---

**Happy Async Testing! 🧪**
