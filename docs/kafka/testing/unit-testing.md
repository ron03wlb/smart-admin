# Unit Testing

Comprehensive unit testing patterns for Kafka producers, consumers, and listeners in SmartAdmin.

## Overview

**Unit testing goals**:
- Test components in isolation
- Fast execution (< 1 second per test)
- No external dependencies (mocked Kafka)
- High coverage (≥ 80%)

**Testing tools**:
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions
- **Spring Boot Test** - Spring context support

---

## Testing Producer Service

### Test Setup

**File**: `sa-admin/src/test/java/net/lab1024/sa/common/mq/kafka/service/KafkaProducerServiceTest.java`

```java
package net.lab1024.sa.common.mq.kafka.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        kafkaProducerService = new KafkaProducerService(kafkaTemplate);
    }

    @Test
    void testSendWithKeyAndValue() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        String value = "test-value";

        SendResult<String, String> mockResult = mockSendResult(topic, 0, 100L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockResult);

        when(kafkaTemplate.send(eq(topic), eq(key), eq(value))).thenReturn(future);

        // Act
        kafkaProducerService.send(topic, key, value);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(topic);
        assertThat(keyCaptor.getValue()).isEqualTo(key);
        assertThat(valueCaptor.getValue()).isEqualTo(value);
    }

    @Test
    void testSendWithoutKey() {
        // Arrange
        String topic = "test-topic";
        String value = "test-value";

        SendResult<String, String> mockResult = mockSendResult(topic, 0, 100L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockResult);

        when(kafkaTemplate.send(eq(topic), eq(value))).thenReturn(future);

        // Act
        kafkaProducerService.send(topic, value);

        // Assert
        verify(kafkaTemplate).send(eq(topic), eq(value));
    }

    @Test
    void testSendBatchAsync() {
        // Arrange
        String topic = "test-topic";
        List<String> messages = List.of("msg1", "msg2", "msg3");

        SendResult<String, String> mockResult = mockSendResult(topic, 0, 100L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockResult);

        when(kafkaTemplate.send(eq(topic), anyString())).thenReturn(future);

        // Act
        CompletableFuture<List<SendResult<String, String>>> resultFuture =
            kafkaProducerService.sendBatchAsync(topic, messages);

        // Assert
        assertThat(resultFuture).isNotNull();
        assertThat(resultFuture).isCompleted();

        verify(kafkaTemplate, times(3)).send(eq(topic), anyString());
    }

    @Test
    void testSendWithCallback() {
        // Arrange
        String topic = "test-topic";
        String key = "test-key";
        String value = "test-value";

        SendResult<String, String> mockResult = mockSendResult(topic, 0, 100L);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mockResult);

        when(kafkaTemplate.send(eq(topic), eq(key), eq(value))).thenReturn(future);

        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        // Act
        kafkaProducerService.send(topic, key, value, new ProducerCallback<>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                callbackInvoked.set(true);
            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Should not fail");
            }
        });

        // Assert
        await().atMost(1, TimeUnit.SECONDS).until(callbackInvoked::get);
    }

    @Test
    void testSendFailure() {
        // Arrange
        String topic = "test-topic";
        String value = "test-value";

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Send failed"));

        when(kafkaTemplate.send(eq(topic), eq(value))).thenReturn(future);

        AtomicBoolean errorHandled = new AtomicBoolean(false);

        // Act
        kafkaProducerService.send(topic, value, new ProducerCallback<>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable ex) {
                errorHandled.set(true);
                assertThat(ex).isInstanceOf(RuntimeException.class);
                assertThat(ex.getMessage()).isEqualTo("Send failed");
            }
        });

        // Assert
        await().atMost(1, TimeUnit.SECONDS).until(errorHandled::get);
    }

    private SendResult<String, String> mockSendResult(String topic, int partition, long offset) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        RecordMetadata metadata = new RecordMetadata(topicPartition, offset, 0, 0L, 0, 0);
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "key", "value");
        return new SendResult<>(producerRecord, metadata);
    }
}
```

---

## Testing Consumer Listeners

### Test Setup with Mock Consumer Record

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/sample/listener/OrderListenerTest.java`

```java
package net.lab1024.sa.admin.module.business.sample.listener;

import com.alibaba.fastjson2.JSON;
import net.lab1024.sa.admin.module.business.sample.domain.dto.OrderDTO;
import net.lab1024.sa.admin.module.business.sample.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderListenerTest {

    @Mock
    private OrderService orderService;

    private OrderListener orderListener;

    @BeforeEach
    void setUp() {
        orderListener = new OrderListener(orderService);
    }

    @Test
    void testDoHandle_ValidOrder() {
        // Arrange
        OrderDTO order = OrderDTO.builder()
            .orderId("ORD-001")
            .customerId("CUST-123")
            .amount(new BigDecimal("99.99"))
            .quantity(2)
            .status("PENDING")
            .build();

        String orderJson = JSON.toJSONString(order);
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 100L, "ORD-001", orderJson);

        // Act
        orderListener.doHandle(record);

        // Assert
        ArgumentCaptor<OrderDTO> orderCaptor = ArgumentCaptor.forClass(OrderDTO.class);
        verify(orderService).processOrder(orderCaptor.capture());

        OrderDTO capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getOrderId()).isEqualTo("ORD-001");
        assertThat(capturedOrder.getAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    void testDoHandle_InvalidQuantity_ThrowsException() {
        // Arrange
        OrderDTO order = OrderDTO.builder()
            .orderId("ORD-002")
            .quantity(-1)  // Invalid
            .amount(new BigDecimal("50.00"))
            .build();

        String orderJson = JSON.toJSONString(order);
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 100L, "ORD-002", orderJson);

        // Act & Assert
        assertThatThrownBy(() -> orderListener.doHandle(record))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid quantity");

        verify(orderService, never()).processOrder(any());
    }

    @Test
    void testDoHandle_InvalidAmount_ThrowsException() {
        // Arrange
        OrderDTO order = OrderDTO.builder()
            .orderId("ORD-003")
            .quantity(1)
            .amount(BigDecimal.ZERO)  // Invalid
            .build();

        String orderJson = JSON.toJSONString(order);
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 100L, "ORD-003", orderJson);

        // Act & Assert
        assertThatThrownBy(() -> orderListener.doHandle(record))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid amount");
    }

    @Test
    void testDoHandle_ServiceThrowsException_PropagatesException() {
        // Arrange
        OrderDTO order = OrderDTO.builder()
            .orderId("ORD-004")
            .quantity(1)
            .amount(new BigDecimal("100.00"))
            .status("PENDING")
            .build();

        String orderJson = JSON.toJSONString(order);
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("smart-admin-order", 0, 100L, "ORD-004", orderJson);

        doThrow(new RuntimeException("Database error"))
            .when(orderService).processOrder(any());

        // Act & Assert
        assertThatThrownBy(() -> orderListener.doHandle(record))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database error");
    }
}
```

---

## Testing Batch Listeners

### Test Batch Processing and Graceful Degradation

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/sample/listener/EmployeeImportListenerTest.java`

```java
package net.lab1024.sa.admin.module.business.sample.listener;

import com.alibaba.fastjson2.JSON;
import net.lab1024.sa.admin.module.business.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.business.employee.manager.EmployeeManager;
import net.lab1024.sa.admin.module.business.sample.domain.dto.EmployeeImportDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeImportListenerTest {

    @Mock
    private EmployeeManager employeeManager;

    private EmployeeImportListener listener;

    @BeforeEach
    void setUp() {
        listener = new EmployeeImportListener(employeeManager);
    }

    @Test
    void testDoBatchHandle_Success() {
        // Arrange
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            EmployeeImportDTO emp = EmployeeImportDTO.builder()
                .employeeId("EMP" + i)
                .name("Employee " + i)
                .salary(new BigDecimal("50000"))
                .build();

            String json = JSON.toJSONString(emp);
            records.add(new ConsumerRecord<>("test-topic", 0, i, "key" + i, json));
        }

        // Act
        listener.doBatchHandle(records);

        // Assert
        ArgumentCaptor<List<EmployeeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(employeeManager).batchInsert(captor.capture());

        List<EmployeeEntity> capturedEmployees = captor.getValue();
        assertThat(capturedEmployees).hasSize(10);
        assertThat(capturedEmployees.get(0).getEmployeeId()).isEqualTo("EMP1");
    }

    @Test
    void testDoHandle_IndividualFallback() {
        // Arrange
        EmployeeImportDTO emp = EmployeeImportDTO.builder()
            .employeeId("EMP001")
            .name("John Doe")
            .salary(new BigDecimal("60000"))
            .build();

        String json = JSON.toJSONString(emp);
        ConsumerRecord<String, String> record =
            new ConsumerRecord<>("test-topic", 0, 1L, "key1", json);

        // Act
        listener.doHandle(record);

        // Assert
        ArgumentCaptor<EmployeeEntity> captor = ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeManager).insert(captor.capture());

        EmployeeEntity capturedEmployee = captor.getValue();
        assertThat(capturedEmployee.getEmployeeId()).isEqualTo("EMP001");
        assertThat(capturedEmployee.getName()).isEqualTo("John Doe");
    }

    @Test
    void testDoBatchHandle_Failure_FallsBackToIndividual() {
        // Arrange
        List<ConsumerRecord<String, String>> records = createTestRecords(5);

        doThrow(new RuntimeException("Batch insert failed"))
            .when(employeeManager).batchInsert(anyList());

        // Act
        listener.handleBatch(records, null);

        // Assert
        // Should fall back to individual processing
        verify(employeeManager).batchInsert(anyList());
        verify(employeeManager, times(5)).insert(any(EmployeeEntity.class));
    }

    private List<ConsumerRecord<String, String>> createTestRecords(int count) {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            EmployeeImportDTO emp = EmployeeImportDTO.builder()
                .employeeId("EMP" + i)
                .name("Employee " + i)
                .build();

            String json = JSON.toJSONString(emp);
            records.add(new ConsumerRecord<>("test-topic", 0, i, "key" + i, json));
        }
        return records;
    }
}
```

---

## Testing Service Layer

### Test Business Logic

**File**: `sa-admin/src/test/java/net/lab1024/sa/admin/module/business/sample/service/OrderServiceTest.java`

```java
package net.lab1024.sa.admin.module.business.sample.service;

import net.lab1024.sa.admin.module.business.sample.dao.OrderDao;
import net.lab1024.sa.admin.module.business.sample.domain.dto.OrderDTO;
import net.lab1024.sa.admin.module.business.sample.domain.entity.OrderEntity;
import net.lab1024.sa.admin.module.business.sample.manager.OrderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderManager orderManager;

    @Mock
    private OrderDao orderDao;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderManager, orderDao);
    }

    @Test
    void testProcessOrder_Success() {
        // Arrange
        OrderDTO orderDTO = OrderDTO.builder()
            .orderId("ORD-001")
            .customerId("CUST-123")
            .amount(new BigDecimal("100.00"))
            .quantity(2)
            .status("PENDING")
            .build();

        when(orderManager.createOrder(any())).thenReturn(1L);

        // Act
        orderService.processOrder(orderDTO);

        // Assert
        verify(orderManager).createOrder(any(OrderEntity.class));
    }

    @Test
    void testProcessOrder_DuplicateOrder_ThrowsException() {
        // Arrange
        OrderDTO orderDTO = OrderDTO.builder()
            .orderId("ORD-001")
            .build();

        when(orderDao.selectById("ORD-001")).thenReturn(new OrderEntity());

        // Act & Assert
        assertThatThrownBy(() -> orderService.processOrder(orderDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Duplicate order");
    }
}
```

---

## Best Practices

### 1. Use Descriptive Test Names

```java
// ✅ Good: Clear what is being tested
@Test
void testSendMessage_WithValidKeyAndValue_SendsSuccessfully()

// ❌ Bad: Unclear intent
@Test
void testSend()
```

### 2. Follow AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testProcessOrder() {
    // Arrange - Setup test data
    OrderDTO order = createTestOrder();
    when(orderService.save(any())).thenReturn(1L);

    // Act - Execute the method
    Long result = orderService.processOrder(order);

    // Assert - Verify expectations
    assertThat(result).isEqualTo(1L);
    verify(orderService).save(any());
}
```

### 3. Test Edge Cases

```java
@Test
void testSend_NullKey_SendsWithoutKey()

@Test
void testSend_EmptyValue_ThrowsException()

@Test
void testSend_NullTopic_ThrowsNullPointerException()
```

### 4. Use Test Fixtures

```java
@TestConfiguration
public class KafkaTestConfig {

    @Bean
    public OrderDTOFixture orderFixture() {
        return new OrderDTOFixture();
    }
}

public class OrderDTOFixture {
    public OrderDTO valid() {
        return OrderDTO.builder()
            .orderId("ORD-001")
            .amount(new BigDecimal("100.00"))
            .build();
    }

    public OrderDTO invalidQuantity() {
        return OrderDTO.builder()
            .orderId("ORD-002")
            .quantity(-1)
            .build();
    }
}
```

---

## See Also

- [Integration Testing](/kafka/testing/integration-testing) - Integration tests with real Kafka
- [Testing Strategy](/kafka/testing/testing-strategy) - Overall testing approach
- [Verification Framework](/kafka/testing/verification-framework) - Quality verification

---

**Last Updated**: 2026-01-22
