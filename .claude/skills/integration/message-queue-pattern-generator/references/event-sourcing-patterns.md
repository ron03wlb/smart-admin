# Event Sourcing Patterns Guide

**Skill:** message-queue-pattern-generator
**Component:** Event Sourcing / Event Store
**Purpose:** Implement event sourcing for audit trails, temporal queries, and rebuilding state

---

## What is Event Sourcing?

**Core Principle:** Store all changes to application state as a sequence of events

**Traditional Approach:**
```
UPDATE users SET balance = 150 WHERE user_id = 1;
```
❌ Lost information: previous balance, why changed, when changed

**Event Sourcing Approach:**
```
Event 1: UserRegistered(userId=1, initialBalance=100)
Event 2: DepositMade(userId=1, amount=50, balance=150)
```
✅ Complete history, can replay, can query "what was balance on date X?"

---

## Pattern 1: Event Store Implementation

### Step 1: Event Store Schema

```sql
-- Event store table
CREATE TABLE event_store (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,  -- e.g., 'Order', 'User'
    aggregate_id VARCHAR(100) NOT NULL,   -- Business entity ID
    event_type VARCHAR(100) NOT NULL,     -- e.g., 'OrderCreated', 'OrderPaid'
    event_version INT NOT NULL,           -- Version of event schema
    event_data JSON NOT NULL,             -- Event payload
    metadata JSON,                        -- User ID, timestamp, correlation ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
);

-- Snapshot table (for performance)
CREATE TABLE event_snapshots (
    snapshot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    snapshot_version INT NOT NULL,        -- Last event version included
    snapshot_data JSON NOT NULL,          -- Aggregated state
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_aggregate (aggregate_type, aggregate_id, snapshot_version)
);
```

### Step 2: Event Store Entity

```java
package net.lab1024.sa.admin.module.business.eventsourcing.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("event_store")
public class EventStoreEntity {

    @TableId(type = IdType.AUTO)
    private Long eventId;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private Integer eventVersion;

    private String eventData;  // JSON string

    private String metadata;   // JSON string

    private LocalDateTime createdAt;
}
```

### Step 3: Event Store Dao

```java
package net.lab1024.sa.admin.module.business.eventsourcing.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventStoreEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventStoreDao extends BaseMapper<EventStoreEntity> {

    /**
     * Get all events for an aggregate (for rebuilding state)
     */
    List<EventStoreEntity> selectByAggregate(
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId
    );

    /**
     * Get events since a specific version (for incremental updates)
     */
    List<EventStoreEntity> selectByAggregateAfterVersion(
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId,
        @Param("afterVersion") Integer afterVersion
    );
}
```

```xml
<!-- EventStoreDao.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatisPlus.org//DTD Mapper 3.0//EN"
        "http://mybatisPlus.org/dtd/mybatisPlus-mapper.dtd">
<mapper namespace="net.lab1024.sa.admin.module.business.eventsourcing.dao.EventStoreDao">

    <select id="selectByAggregate" resultType="net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventStoreEntity">
        SELECT * FROM event_store
        WHERE aggregate_type = #{aggregateType}
          AND aggregate_id = #{aggregateId}
        ORDER BY event_id ASC
    </select>

    <select id="selectByAggregateAfterVersion" resultType="net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventStoreEntity">
        SELECT * FROM event_store
        WHERE aggregate_type = #{aggregateType}
          AND aggregate_id = #{aggregateId}
          AND event_version > #{afterVersion}
        ORDER BY event_id ASC
    </select>

</mapper>
```

### Step 4: Event Store Service

```java
package net.lab1024.sa.admin.module.business.eventsourcing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.eventsourcing.dao.EventStoreDao;
import net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventStoreEntity;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStoreService {

    private final EventStoreDao eventStoreDao;
    private final ObjectMapper objectMapper;

    /**
     * Append event to event store
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendEvent(String aggregateType, String aggregateId, OrderEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", event.getUserId());
            metadata.put("timestamp", LocalDateTime.now());
            metadata.put("correlationId", event.getEventId());

            EventStoreEntity entity = EventStoreEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(event.getClass().getSimpleName())
                .eventVersion(event.getEventVersion())
                .eventData(eventData)
                .metadata(objectMapper.writeValueAsString(metadata))
                .createdAt(LocalDateTime.now())
                .build();

            eventStoreDao.insert(entity);

            log.info("Event appended to store: aggregateId={}, eventType={}",
                aggregateId, event.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Failed to append event: aggregateId={}", aggregateId, e);
            throw new RuntimeException("Event store append failed", e);
        }
    }

    /**
     * Load all events for an aggregate
     */
    public List<EventStoreEntity> loadEvents(String aggregateType, String aggregateId) {
        return eventStoreDao.selectByAggregate(aggregateType, aggregateId);
    }

    /**
     * Load events since a specific version
     */
    public List<EventStoreEntity> loadEventsSinceVersion(
        String aggregateType,
        String aggregateId,
        Integer afterVersion
    ) {
        return eventStoreDao.selectByAggregateAfterVersion(aggregateType, aggregateId, afterVersion);
    }
}
```

---

## Pattern 2: Aggregate Rebuilding from Events

```java
package net.lab1024.sa.admin.module.business.order.aggregate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.domain.event.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class OrderAggregate {

    private Long orderId;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalAmount;
    private List<OrderItemAggregate> items = new ArrayList<>();
    private Integer version = 0;

    /**
     * Apply event to rebuild state
     */
    public void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent) {
            applyOrderCreated((OrderCreatedEvent) event);
        } else if (event instanceof OrderPaidEvent) {
            applyOrderPaid((OrderPaidEvent) event);
        } else if (event instanceof OrderShippedEvent) {
            applyOrderShipped((OrderShippedEvent) event);
        } else if (event instanceof OrderCancelledEvent) {
            applyOrderCancelled((OrderCancelledEvent) event);
        }
        version++;
    }

    private void applyOrderCreated(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.userId = event.getUserId();
        this.orderStatus = "CREATED";
        this.totalAmount = event.getTotalAmount();
        log.debug("Applied OrderCreated: orderId={}", orderId);
    }

    private void applyOrderPaid(OrderPaidEvent event) {
        this.orderStatus = "PAID";
        log.debug("Applied OrderPaid: orderId={}", orderId);
    }

    private void applyOrderShipped(OrderShippedEvent event) {
        this.orderStatus = "SHIPPED";
        log.debug("Applied OrderShipped: orderId={}", orderId);
    }

    private void applyOrderCancelled(OrderCancelledEvent event) {
        this.orderStatus = "CANCELLED";
        log.debug("Applied OrderCancelled: orderId={}", orderId);
    }

    /**
     * Rebuild aggregate from event history
     */
    public static OrderAggregate fromEvents(List<OrderEvent> events) {
        OrderAggregate aggregate = new OrderAggregate();
        events.forEach(aggregate::apply);
        return aggregate;
    }
}
```

---

## Pattern 3: Projections (Read Models)

### Projection Handler

```java
package net.lab1024.sa.admin.module.business.order.projection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.order.dao.OrderReadModelDao;
import net.lab1024.sa.admin.module.business.order.domain.entity.OrderReadModelEntity;
import net.lab1024.sa.admin.module.business.order.domain.event.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProjectionHandler {

    private final OrderReadModelDao orderReadModelDao;

    /**
     * Update read model when OrderCreated event is received
     */
    @KafkaListener(topics = "smartadmin.order.created", groupId = "order-projection-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        OrderReadModelEntity readModel = OrderReadModelEntity.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .orderStatus("CREATED")
            .totalAmount(event.getTotalAmount())
            .createdAt(event.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();

        orderReadModelDao.insert(readModel);
        log.info("Order read model created: orderId={}", event.getOrderId());
    }

    /**
     * Update read model when OrderPaid event is received
     */
    @KafkaListener(topics = "smartadmin.order.paid", groupId = "order-projection-group")
    public void handleOrderPaid(OrderPaidEvent event) {
        OrderReadModelEntity readModel = orderReadModelDao.selectById(event.getOrderId());
        if (readModel != null) {
            readModel.setOrderStatus("PAID");
            readModel.setPaidAt(event.getPaidAt());
            readModel.setUpdatedAt(LocalDateTime.now());
            orderReadModelDao.updateById(readModel);
            log.info("Order read model updated (paid): orderId={}", event.getOrderId());
        }
    }

    /**
     * Update read model when OrderShipped event is received
     */
    @KafkaListener(topics = "smartadmin.order.shipped", groupId = "order-projection-group")
    public void handleOrderShipped(OrderShippedEvent event) {
        OrderReadModelEntity readModel = orderReadModelDao.selectById(event.getOrderId());
        if (readModel != null) {
            readModel.setOrderStatus("SHIPPED");
            readModel.setShippedAt(event.getShippedAt());
            readModel.setTrackingNumber(event.getTrackingNumber());
            readModel.setUpdatedAt(LocalDateTime.now());
            orderReadModelDao.updateById(readModel);
            log.info("Order read model updated (shipped): orderId={}", event.getOrderId());
        }
    }
}
```

---

## Pattern 4: Snapshots for Performance

```java
package net.lab1024.sa.admin.module.business.eventsourcing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.eventsourcing.dao.EventSnapshotDao;
import net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventSnapshotEntity;
import net.lab1024.sa.admin.module.business.order.aggregate.OrderAggregate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final EventSnapshotDao snapshotDao;
    private final EventStoreService eventStoreService;
    private final ObjectMapper objectMapper;

    private static final int SNAPSHOT_INTERVAL = 50;  // Snapshot every 50 events

    /**
     * Save snapshot of aggregate state
     */
    public void saveSnapshot(String aggregateType, String aggregateId, OrderAggregate aggregate) {
        try {
            String snapshotData = objectMapper.writeValueAsString(aggregate);

            EventSnapshotEntity snapshot = EventSnapshotEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .snapshotVersion(aggregate.getVersion())
                .snapshotData(snapshotData)
                .createdAt(LocalDateTime.now())
                .build();

            snapshotDao.insert(snapshot);

            log.info("Snapshot saved: aggregateId={}, version={}",
                aggregateId, aggregate.getVersion());

        } catch (Exception e) {
            log.error("Failed to save snapshot: aggregateId={}", aggregateId, e);
        }
    }

    /**
     * Load aggregate with snapshot optimization
     */
    public OrderAggregate loadAggregate(String aggregateType, String aggregateId) {
        // 1. Try to load latest snapshot
        EventSnapshotEntity snapshot = snapshotDao.selectLatestSnapshot(aggregateType, aggregateId);

        OrderAggregate aggregate;
        int startVersion = 0;

        if (snapshot != null) {
            try {
                // Start from snapshot
                aggregate = objectMapper.readValue(snapshot.getSnapshotData(), OrderAggregate.class);
                startVersion = snapshot.getSnapshotVersion();
                log.info("Loaded from snapshot: aggregateId={}, version={}",
                    aggregateId, startVersion);
            } catch (Exception e) {
                log.warn("Failed to load snapshot, will rebuild from scratch", e);
                aggregate = new OrderAggregate();
            }
        } else {
            aggregate = new OrderAggregate();
        }

        // 2. Replay events since snapshot
        var events = eventStoreService.loadEventsSinceVersion(aggregateType, aggregateId, startVersion);
        events.forEach(eventEntity -> {
            try {
                OrderEvent event = objectMapper.readValue(
                    eventEntity.getEventData(),
                    Class.forName("net.lab1024.sa.admin.module.business.order.domain.event." + eventEntity.getEventType())
                );
                aggregate.apply(event);
            } catch (Exception e) {
                log.error("Failed to apply event: eventId={}", eventEntity.getEventId(), e);
            }
        });

        // 3. Create new snapshot if needed
        if (aggregate.getVersion() - startVersion >= SNAPSHOT_INTERVAL) {
            saveSnapshot(aggregateType, aggregateId, aggregate);
        }

        return aggregate;
    }
}
```

---

## Pattern 5: Temporal Queries

```java
package net.lab1024.sa.admin.module.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.eventsourcing.dao.EventStoreDao;
import net.lab1024.sa.admin.module.business.eventsourcing.domain.entity.EventStoreEntity;
import net.lab1024.sa.admin.module.business.order.aggregate.OrderAggregate;
import net.lab1024.sa.admin.module.business.order.domain.event.OrderEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemporalQueryService {

    private final EventStoreDao eventStoreDao;

    /**
     * Query: What was the order status at a specific point in time?
     */
    public OrderAggregate getOrderStateAt(Long orderId, LocalDateTime pointInTime) {
        // Load all events up to the point in time
        List<EventStoreEntity> events = eventStoreDao.selectByAggregateBeforeTime(
            "Order",
            orderId.toString(),
            pointInTime
        );

        // Rebuild aggregate from historical events
        OrderAggregate aggregate = new OrderAggregate();
        events.forEach(eventEntity -> {
            try {
                OrderEvent event = parseEvent(eventEntity);
                aggregate.apply(event);
            } catch (Exception e) {
                log.error("Failed to apply historical event", e);
            }
        });

        log.info("Temporal query: orderId={}, pointInTime={}, status={}",
            orderId, pointInTime, aggregate.getOrderStatus());

        return aggregate;
    }

    /**
     * Query: Find all orders that were in "PAID" status on a specific date
     */
    public List<Long> findOrdersPaidOn(LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        // Find all OrderPaid events within date range
        List<EventStoreEntity> paidEvents = eventStoreDao.selectByEventTypeInDateRange(
            "OrderPaidEvent",
            startOfDay,
            endOfDay
        );

        return paidEvents.stream()
            .map(EventStoreEntity::getAggregateId)
            .map(Long::valueOf)
            .collect(Collectors.toList());
    }

    private OrderEvent parseEvent(EventStoreEntity entity) throws Exception {
        // Parse JSON to event object
        // Implementation depends on your JSON library
        return null;
    }
}
```

---

## Complete SmartAdmin Integration

```java
// Order Service with Event Sourcing
@Service
@RequiredArgsConstructor
public class EventSourcedOrderService {

    private final EventStoreService eventStoreService;
    private final OrderEventProducer eventProducer;

    /**
     * Create order (command)
     */
    public ResponseDTO<Long> createOrder(OrderAddForm form) {
        // 1. Generate order ID
        Long orderId = IdGenerator.nextId();

        // 2. Create event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(orderId)
            .userId(form.getUserId())
            .totalAmount(form.getTotalAmount())
            .createdAt(LocalDateTime.now())
            .eventId(UUID.randomUUID().toString())
            .eventVersion(1)
            .build();

        // 3. Append to event store
        eventStoreService.appendEvent("Order", orderId.toString(), event);

        // 4. Publish to Kafka (for projections and downstream consumers)
        eventProducer.publishOrderCreated(event);

        return ResponseDTO.ok(orderId);
    }

    /**
     * Query order (from read model)
     */
    public ResponseDTO<OrderVO> getOrder(Long orderId) {
        // Read from projection (optimized read model)
        OrderReadModelEntity readModel = orderReadModelDao.selectById(orderId);
        OrderVO vo = SmartBeanUtil.copy(readModel, OrderVO.class);
        return ResponseDTO.ok(vo);
    }

    /**
     * Query order history (from event store)
     */
    public ResponseDTO<List<OrderEvent>> getOrderHistory(Long orderId) {
        List<EventStoreEntity> events = eventStoreService.loadEvents("Order", orderId.toString());
        // Convert to OrderEvent objects
        return ResponseDTO.ok(parsedEvents);
    }
}
```

---

## Best Practices

### 1. Event Design
- ✅ Events are immutable (past tense names: OrderCreated, not CreateOrder)
- ✅ Include all data needed to rebuild state
- ✅ Version events for schema evolution
- ✅ Include correlation IDs for tracing

### 2. Performance
- ✅ Use snapshots for aggregates with many events (> 50 events)
- ✅ Build read models (projections) for queries
- ✅ Cache frequently accessed aggregates
- ✅ Use async event processing for projections

### 3. Consistency
- ✅ Event store writes are transactional
- ✅ Projections are eventually consistent
- ✅ Handle duplicate events (idempotency)
- ✅ Order events by aggregate ID for consistency

---

**Next:** [CQRS and Saga Patterns](cqrs-saga-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
