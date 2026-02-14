# CQRS and Saga Patterns Guide

**Skill:** message-queue-pattern-generator
**Component:** CQRS / Saga / Distributed Transactions
**Purpose:** Implement command-query separation and distributed transaction patterns

---

## Part 1: CQRS (Command Query Responsibility Segregation)

### What is CQRS?

**Problem:** Single database model serves both reads and writes poorly
- Writes need consistency, validation, business logic
- Reads need speed, denormalization, different views

**Solution:** Separate read and write models

```
Commands (Write) → Event Store → Events → Projections (Read Models)
```

---

## Pattern 1: Basic CQRS Implementation

### Step 1: Command Side (Write Model)

```java
package net.lab1024.sa.business.order.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.command.CreateOrderCommand;
import net.lab1024.sa.business.order.domain.command.PayOrderCommand;
import net.lab1024.sa.business.order.domain.event.OrderCreatedEvent;
import net.lab1024.sa.business.order.domain.event.OrderPaidEvent;
import net.lab1024.sa.business.eventsourcing.service.EventStoreService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final EventStoreService eventStoreService;
    private final OrderEventProducer eventProducer;

    /**
     * Handle CreateOrder command
     */
    public ResponseDTO<Long> createOrder(CreateOrderCommand command) {
        // 1. Validate command
        validateCreateOrder(command);

        // 2. Generate order ID
        Long orderId = IdGenerator.nextId();

        // 3. Create event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(orderId)
            .userId(command.getUserId())
            .totalAmount(command.getTotalAmount())
            .items(command.getItems())
            .createdAt(LocalDateTime.now())
            .eventId(UUID.randomUUID().toString())
            .eventVersion(1)
            .build();

        // 4. Persist event
        eventStoreService.appendEvent("Order", orderId.toString(), event);

        // 5. Publish event
        eventProducer.publishOrderCreated(event);

        log.info("Order created: orderId={}, userId={}", orderId, command.getUserId());
        return ResponseDTO.ok(orderId);
    }

    /**
     * Handle PayOrder command
     */
    public ResponseDTO<Void> payOrder(PayOrderCommand command) {
        // 1. Validate command
        validatePayOrder(command);

        // 2. Load aggregate to check current state
        OrderAggregate aggregate = snapshotService.loadAggregate("Order", command.getOrderId().toString());
        if (!"CREATED".equals(aggregate.getOrderStatus())) {
            return ResponseDTO.error("Order cannot be paid in current status");
        }

        // 3. Create event
        OrderPaidEvent event = OrderPaidEvent.builder()
            .orderId(command.getOrderId())
            .paymentMethod(command.getPaymentMethod())
            .paidAmount(command.getPaidAmount())
            .paidAt(LocalDateTime.now())
            .eventId(UUID.randomUUID().toString())
            .eventVersion(aggregate.getVersion() + 1)
            .build();

        // 4. Persist event
        eventStoreService.appendEvent("Order", command.getOrderId().toString(), event);

        // 5. Publish event
        eventProducer.publishOrderPaid(event);

        log.info("Order paid: orderId={}, amount={}", command.getOrderId(), command.getPaidAmount());
        return ResponseDTO.ok();
    }

    private void validateCreateOrder(CreateOrderCommand command) {
        if (command.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total amount must be positive");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException("Order must have at least one item");
        }
    }

    private void validatePayOrder(PayOrderCommand command) {
        if (command.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }
    }
}
```

### Step 2: Query Side (Read Model)

```java
package net.lab1024.sa.business.order.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.dao.OrderReadModelDao;
import net.lab1024.sa.business.order.domain.entity.OrderReadModelEntity;
import net.lab1024.sa.business.order.domain.form.OrderQueryForm;
import net.lab1024.sa.business.order.domain.vo.OrderVO;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadModelDao orderReadModelDao;

    /**
     * Query order by ID (optimized read model)
     */
    public ResponseDTO<OrderVO> getOrderById(Long orderId) {
        OrderReadModelEntity readModel = orderReadModelDao.selectById(orderId);
        if (readModel == null) {
            return ResponseDTO.error("Order not found");
        }

        OrderVO vo = SmartBeanUtil.copy(readModel, OrderVO.class);
        return ResponseDTO.ok(vo);
    }

    /**
     * Query orders with pagination (optimized for listing)
     */
    public ResponseDTO<PageResult<OrderVO>> queryOrders(OrderQueryForm form) {
        Page<OrderReadModelEntity> page = orderReadModelDao.selectPage(
            SmartPageUtil.convert2PageQuery(form),
            buildQueryWrapper(form)
        );

        List<OrderVO> voList = page.getRecords().stream()
            .map(entity -> SmartBeanUtil.copy(entity, OrderVO.class))
            .collect(Collectors.toList());

        PageResult<OrderVO> pageResult = SmartPageUtil.convert2PageResult(page, voList);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * Complex query: Orders by user with status aggregation
     */
    public ResponseDTO<UserOrderSummaryVO> getUserOrderSummary(Long userId) {
        // Use denormalized read model for complex queries
        UserOrderSummaryVO summary = orderReadModelDao.selectUserOrderSummary(userId);
        return ResponseDTO.ok(summary);
    }

    private QueryWrapper<OrderReadModelEntity> buildQueryWrapper(OrderQueryForm form) {
        QueryWrapper<OrderReadModelEntity> wrapper = new QueryWrapper<>();

        if (form.getUserId() != null) {
            wrapper.eq("user_id", form.getUserId());
        }
        if (form.getOrderStatus() != null) {
            wrapper.eq("order_status", form.getOrderStatus());
        }
        if (form.getStartDate() != null) {
            wrapper.ge("created_at", form.getStartDate());
        }
        if (form.getEndDate() != null) {
            wrapper.le("created_at", form.getEndDate());
        }

        wrapper.orderByDesc("created_at");
        return wrapper;
    }
}
```

### Step 3: Read Model Schema (Denormalized)

```sql
-- Optimized read model (denormalized for query performance)
CREATE TABLE order_read_model (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100),           -- Denormalized from User table
    order_status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    item_count INT,                  -- Pre-calculated
    payment_method VARCHAR(50),
    tracking_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (order_status),
    INDEX idx_created_at (created_at)
);

-- Summary table for analytics (materialized view pattern)
CREATE TABLE user_order_summary (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(100),
    total_orders INT DEFAULT 0,
    total_spent DECIMAL(15,2) DEFAULT 0,
    orders_created INT DEFAULT 0,
    orders_paid INT DEFAULT 0,
    orders_shipped INT DEFAULT 0,
    orders_delivered INT DEFAULT 0,
    orders_cancelled INT DEFAULT 0,
    last_order_date TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_total_orders (total_orders),
    INDEX idx_total_spent (total_spent)
);
```

### Step 4: Controller with CQRS Separation

```java
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService commandService;  // Write side
    private final OrderQueryService queryService;      // Read side

    /**
     * Command: Create order
     */
    @PostMapping("/create")
    public ResponseDTO<Long> createOrder(
        @RequestBody @Valid CreateOrderCommand command,
        @LoginUser RequestUser requestUser
    ) {
        command.setUserId(requestUser.getUserId());
        return commandService.createOrder(command);
    }

    /**
     * Command: Pay order
     */
    @PostMapping("/pay")
    public ResponseDTO<Void> payOrder(@RequestBody @Valid PayOrderCommand command) {
        return commandService.payOrder(command);
    }

    /**
     * Query: Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseDTO<OrderVO> getOrder(@PathVariable Long orderId) {
        return queryService.getOrderById(orderId);
    }

    /**
     * Query: List orders
     */
    @PostMapping("/query")
    public ResponseDTO<PageResult<OrderVO>> queryOrders(@RequestBody OrderQueryForm form) {
        return queryService.queryOrders(form);
    }

    /**
     * Query: Get user order summary
     */
    @GetMapping("/summary/{userId}")
    public ResponseDTO<UserOrderSummaryVO> getUserSummary(@PathVariable Long userId) {
        return queryService.getUserOrderSummary(userId);
    }
}
```

---

## Part 2: Saga Pattern (Distributed Transactions)

### What is Saga?

**Problem:** Distributed transactions across multiple services are complex
- Traditional 2PC (two-phase commit) doesn't scale
- Need to maintain consistency without distributed locks

**Solution:** Saga pattern - sequence of local transactions with compensations

---

## Pattern 2: Choreography-based Saga

**Order Payment Saga:**
```
OrderService → PaymentService → InventoryService → ShippingService
     ↓              ↓                  ↓                 ↓
OrderCreated → PaymentProcessed → InventoryReserved → OrderShipped
```

### Step 1: Saga Orchestrator (Choreography)

```java
package net.lab1024.sa.business.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.event.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentSagaChoreography {

    private final PaymentEventProducer paymentProducer;
    private final InventoryEventProducer inventoryProducer;
    private final ShippingEventProducer shippingProducer;
    private final OrderEventProducer orderProducer;

    /**
     * Step 1: Order created → Request payment
     */
    @KafkaListener(topics = "smartadmin.order.created", groupId = "order-saga-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Saga Step 1: Order created, requesting payment: orderId={}", event.getOrderId());

        PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .amount(event.getTotalAmount())
            .sagaId(event.getEventId())  // Link events in saga
            .build();

        paymentProducer.publishPaymentRequested(paymentEvent);
    }

    /**
     * Step 2: Payment processed → Reserve inventory
     */
    @KafkaListener(topics = "smartadmin.payment.processed", groupId = "order-saga-group")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Saga Step 2: Payment processed, reserving inventory: orderId={}", event.getOrderId());

        InventoryReservationRequestedEvent inventoryEvent = InventoryReservationRequestedEvent.builder()
            .orderId(event.getOrderId())
            .items(event.getOrderItems())
            .sagaId(event.getSagaId())
            .build();

        inventoryProducer.publishInventoryReservationRequested(inventoryEvent);
    }

    /**
     * Step 3: Inventory reserved → Create shipment
     */
    @KafkaListener(topics = "smartadmin.inventory.reserved", groupId = "order-saga-group")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Saga Step 3: Inventory reserved, creating shipment: orderId={}", event.getOrderId());

        ShipmentCreatedEvent shipmentEvent = ShipmentCreatedEvent.builder()
            .orderId(event.getOrderId())
            .shippingAddress(event.getShippingAddress())
            .sagaId(event.getSagaId())
            .build();

        shippingProducer.publishShipmentCreated(shipmentEvent);
    }

    /**
     * Step 4: Shipment created → Complete order
     */
    @KafkaListener(topics = "smartadmin.shipment.created", groupId = "order-saga-group")
    public void handleShipmentCreated(ShipmentCreatedEvent event) {
        log.info("Saga Step 4: Shipment created, completing order: orderId={}", event.getOrderId());

        OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
            .orderId(event.getOrderId())
            .trackingNumber(event.getTrackingNumber())
            .sagaId(event.getSagaId())
            .build();

        orderProducer.publishOrderCompleted(completedEvent);
    }

    /**
     * Compensation: Payment failed → Cancel order
     */
    @KafkaListener(topics = "smartadmin.payment.failed", groupId = "order-saga-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Saga Compensation: Payment failed, canceling order: orderId={}", event.getOrderId());

        OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
            .orderId(event.getOrderId())
            .reason("Payment failed: " + event.getFailureReason())
            .sagaId(event.getSagaId())
            .build();

        orderProducer.publishOrderCancelled(cancelEvent);
    }

    /**
     * Compensation: Inventory reservation failed → Refund payment
     */
    @KafkaListener(topics = "smartadmin.inventory.reservation-failed", groupId = "order-saga-group")
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.warn("Saga Compensation: Inventory failed, refunding payment: orderId={}", event.getOrderId());

        PaymentRefundRequestedEvent refundEvent = PaymentRefundRequestedEvent.builder()
            .orderId(event.getOrderId())
            .reason("Inventory reservation failed")
            .sagaId(event.getSagaId())
            .build();

        paymentProducer.publishPaymentRefundRequested(refundEvent);

        // Also cancel order
        OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
            .orderId(event.getOrderId())
            .reason("Inventory reservation failed")
            .sagaId(event.getSagaId())
            .build();

        orderProducer.publishOrderCancelled(cancelEvent);
    }
}
```

---

## Pattern 3: Orchestration-based Saga

**Centralized Saga Orchestrator:**

```java
package net.lab1024.sa.business.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.entity.SagaStateEntity;
import net.lab1024.sa.business.order.dao.SagaStateDao;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentSagaOrchestrator {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final SagaStateDao sagaStateDao;

    /**
     * Execute saga with centralized orchestration
     */
    public void executeOrderPaymentSaga(Long orderId, OrderCreatedEvent orderEvent) {
        String sagaId = UUID.randomUUID().toString();

        // Create saga state
        SagaStateEntity sagaState = SagaStateEntity.builder()
            .sagaId(sagaId)
            .sagaType("ORDER_PAYMENT")
            .currentStep("PAYMENT")
            .orderId(orderId)
            .status("RUNNING")
            .build();
        sagaStateDao.insert(sagaState);

        try {
            // Step 1: Process payment
            log.info("Saga step 1: Processing payment: orderId={}, sagaId={}", orderId, sagaId);
            paymentService.processPayment(orderId, orderEvent.getTotalAmount());
            updateSagaStep(sagaId, "INVENTORY");

            // Step 2: Reserve inventory
            log.info("Saga step 2: Reserving inventory: orderId={}, sagaId={}", orderId, sagaId);
            inventoryService.reserveInventory(orderId, orderEvent.getItems());
            updateSagaStep(sagaId, "SHIPPING");

            // Step 3: Create shipment
            log.info("Saga step 3: Creating shipment: orderId={}, sagaId={}", orderId, sagaId);
            shippingService.createShipment(orderId, orderEvent.getShippingAddress());
            updateSagaStep(sagaId, "COMPLETED");

            // Saga completed successfully
            sagaState.setStatus("COMPLETED");
            sagaStateDao.updateById(sagaState);
            log.info("Saga completed successfully: orderId={}, sagaId={}", orderId, sagaId);

        } catch (PaymentException e) {
            log.error("Saga failed at PAYMENT step, compensating: orderId={}", orderId, e);
            compensatePaymentFailure(sagaId, orderId);

        } catch (InventoryException e) {
            log.error("Saga failed at INVENTORY step, compensating: orderId={}", orderId, e);
            compensateInventoryFailure(sagaId, orderId);

        } catch (ShippingException e) {
            log.error("Saga failed at SHIPPING step, compensating: orderId={}", orderId, e);
            compensateShippingFailure(sagaId, orderId);
        }
    }

    private void updateSagaStep(String sagaId, String newStep) {
        SagaStateEntity sagaState = sagaStateDao.selectBySagaId(sagaId);
        sagaState.setCurrentStep(newStep);
        sagaStateDao.updateById(sagaState);
    }

    /**
     * Compensation: Payment failed
     */
    private void compensatePaymentFailure(String sagaId, Long orderId) {
        log.info("Compensation: Canceling order due to payment failure: orderId={}", orderId);
        // Cancel order
        orderService.cancelOrder(orderId, "Payment failed");

        // Update saga state
        SagaStateEntity sagaState = sagaStateDao.selectBySagaId(sagaId);
        sagaState.setStatus("COMPENSATED");
        sagaState.setCompensationStep("ORDER_CANCELLED");
        sagaStateDao.updateById(sagaState);
    }

    /**
     * Compensation: Inventory reservation failed
     */
    private void compensateInventoryFailure(String sagaId, Long orderId) {
        log.info("Compensation: Refunding payment due to inventory failure: orderId={}", orderId);

        // Step 1: Refund payment
        paymentService.refundPayment(orderId);

        // Step 2: Cancel order
        orderService.cancelOrder(orderId, "Inventory reservation failed");

        // Update saga state
        SagaStateEntity sagaState = sagaStateDao.selectBySagaId(sagaId);
        sagaState.setStatus("COMPENSATED");
        sagaState.setCompensationStep("PAYMENT_REFUNDED,ORDER_CANCELLED");
        sagaStateDao.updateById(sagaState);
    }

    /**
     * Compensation: Shipping creation failed
     */
    private void compensateShippingFailure(String sagaId, Long orderId) {
        log.info("Compensation: Rolling back due to shipping failure: orderId={}", orderId);

        // Step 1: Release inventory
        inventoryService.releaseInventory(orderId);

        // Step 2: Refund payment
        paymentService.refundPayment(orderId);

        // Step 3: Cancel order
        orderService.cancelOrder(orderId, "Shipping creation failed");

        // Update saga state
        SagaStateEntity sagaState = sagaStateDao.selectBySagaId(sagaId);
        sagaState.setStatus("COMPENSATED");
        sagaState.setCompensationStep("INVENTORY_RELEASED,PAYMENT_REFUNDED,ORDER_CANCELLED");
        sagaStateDao.updateById(sagaState);
    }
}
```

### Saga State Table

```sql
CREATE TABLE saga_state (
    saga_id VARCHAR(50) PRIMARY KEY,
    saga_type VARCHAR(50) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,  -- RUNNING, COMPLETED, COMPENSATING, COMPENSATED, FAILED
    compensation_step VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
);
```

---

## Best Practices

### CQRS Best Practices:
1. ✅ Separate databases for read and write (optional but recommended)
2. ✅ Use eventual consistency for read models
3. ✅ Denormalize read models for query performance
4. ✅ Version commands and events for schema evolution
5. ✅ Validate commands before executing

### Saga Best Practices:
1. ✅ Design idempotent saga steps (handle duplicate events)
2. ✅ Implement compensating transactions for each step
3. ✅ Use saga state persistence for recovery
4. ✅ Add timeouts and retries for saga steps
5. ✅ Monitor saga completion rates and compensation triggers

---

**Next:** [Reliability Patterns](reliability-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
