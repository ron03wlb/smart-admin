# Server-Sent Events (SSE) Patterns

**Skill:** websocket-sse-realtime-generator
**Component:** SSE / Real-time Communication / Server Push
**Purpose:** SSE implementation patterns for SmartAdmin applications

---

## SSE vs WebSocket

| Feature | SSE | WebSocket |
|---------|-----|-----------|
| Direction | Server → Client only | Bidirectional |
| Protocol | HTTP (text/event-stream) | WS/WSS |
| Complexity | Simple | Complex |
| Reconnection | Automatic (built-in) | Manual implementation |
| Use Cases | Notifications, live updates, dashboards | Chat, gaming, real-time collaboration |

**When to use SSE:**
- Server-to-client push only (no client-to-server)
- Simpler implementation than WebSocket
- Automatic reconnection needed
- HTTP infrastructure (no special proxy configuration)

---

## Pattern 1: Basic SSE Endpoint

### SSE Controller

```java
package net.lab1024.sa.business.notification.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.domain.vo.NotificationVO;
import net.lab1024.sa.business.notification.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * Subscribe to SSE stream
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        // Get current user ID from Sa-Token
        Long userId = StpUtil.getLoginIdAsLong();

        log.info("SSE subscription from user: {}", userId);

        // Create SSE emitter (timeout: 30 minutes)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Register emitter
        sseEmitterService.addEmitter(userId, emitter);

        // Handle completion and timeout
        emitter.onCompletion(() -> {
            log.info("SSE completed for user: {}", userId);
            sseEmitterService.removeEmitter(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE timeout for user: {}", userId);
            sseEmitterService.removeEmitter(userId);
        });

        emitter.onError(e -> {
            log.error("SSE error for user: {}", userId, e);
            sseEmitterService.removeEmitter(userId);
        });

        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to SSE stream"));
        } catch (Exception e) {
            log.error("Failed to send initial SSE message", e);
        }

        return emitter;
    }
}
```

### SSE Emitter Service

```java
package net.lab1024.sa.business.notification.service;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.domain.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // Map: userId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Add SSE emitter
     */
    public void addEmitter(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.info("Added SSE emitter for user {}, total: {}", userId, emitters.size());
    }

    /**
     * Remove SSE emitter
     */
    public void removeEmitter(Long userId) {
        emitters.remove(userId);
        log.info("Removed SSE emitter for user {}, total: {}", userId, emitters.size());
    }

    /**
     * Send notification to specific user
     */
    public void sendToUser(Long userId, NotificationVO notification) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            log.warn("No SSE emitter found for user {}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(notification));

            log.info("Sent SSE notification to user {}: {}", userId, notification.getTitle());

        } catch (IOException e) {
            log.error("Failed to send SSE to user {}", userId, e);
            removeEmitter(userId);
        }
    }

    /**
     * Broadcast notification to all connected users
     */
    public void broadcast(NotificationVO notification) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));

                log.debug("Broadcasted SSE notification to user {}", userId);

            } catch (IOException e) {
                log.error("Failed to broadcast SSE to user {}", userId, e);
                removeEmitter(userId);
            }
        });

        log.info("Broadcasted SSE notification to {} users", emitters.size());
    }

    /**
     * Get active connection count
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
```

---

## Pattern 2: Heartbeat (Keep-Alive)

### Scheduled Heartbeat

```java
package net.lab1024.sa.business.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseHeartbeatService {

    private final SseEmitterService sseEmitterService;

    /**
     * Send heartbeat to all connected clients every 30 seconds
     * Prevents proxy/firewall timeout
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        sseEmitterService.emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .comment("heartbeat"));  // Comment events don't trigger JS event listeners

                log.debug("Sent SSE heartbeat to user {}", userId);

            } catch (IOException e) {
                log.error("Failed to send SSE heartbeat to user {}", userId, e);
                sseEmitterService.removeEmitter(userId);
            }
        });
    }
}
```

---

## Pattern 3: Event Types

### Multi-Event SSE Stream

```java
/**
 * Send different event types to client
 */
public void sendOrderUpdate(Long userId, OrderStatusVO order) {
    SseEmitter emitter = emitters.get(userId);

    if (emitter != null) {
        try {
            emitter.send(SseEmitter.event()
                .name("order-update")  // Event type
                .id(String.valueOf(order.getOrderId()))  // Event ID for tracking
                .data(order)
                .reconnectTime(3000));  // Reconnect delay if connection lost

        } catch (IOException e) {
            log.error("Failed to send order update", e);
            removeEmitter(userId);
        }
    }
}

public void sendSystemAlert(Long userId, String message) {
    SseEmitter emitter = emitters.get(userId);

    if (emitter != null) {
        try {
            emitter.send(SseEmitter.event()
                .name("system-alert")
                .data(message));

        } catch (IOException e) {
            log.error("Failed to send system alert", e);
            removeEmitter(userId);
        }
    }
}
```

---

## Pattern 4: Real-time Dashboard Updates

### System Metrics SSE

```java
package net.lab1024.sa.system.monitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.monitor.domain.vo.SystemMetricsVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMetricsSseService {

    // Map: sessionId -> SseEmitter
    private final Map<String, SseEmitter> metricsEmitters = new ConcurrentHashMap<>();

    /**
     * Subscribe to system metrics
     */
    public SseEmitter subscribeToMetrics(String sessionId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);  // 1 hour timeout

        metricsEmitters.put(sessionId, emitter);

        emitter.onCompletion(() -> metricsEmitters.remove(sessionId));
        emitter.onTimeout(() -> metricsEmitters.remove(sessionId));
        emitter.onError(e -> metricsEmitters.remove(sessionId));

        return emitter;
    }

    /**
     * Broadcast system metrics every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastSystemMetrics() {
        SystemMetricsVO metrics = collectSystemMetrics();

        metricsEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("system-metrics")
                    .data(metrics));

            } catch (IOException e) {
                log.error("Failed to send metrics to session {}", sessionId, e);
                metricsEmitters.remove(sessionId);
            }
        });
    }

    private SystemMetricsVO collectSystemMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        return SystemMetricsVO.builder()
            .timestamp(System.currentTimeMillis())
            .cpuUsage(getCpuUsage())
            .memoryUsage((double) heapUsed / heapMax * 100)
            .heapUsed(heapUsed / 1024 / 1024)
            .heapMax(heapMax / 1024 / 1024)
            .build();
    }

    private double getCpuUsage() {
        return 0.0;  // Placeholder
    }
}
```

**Controller:**

```java
@GetMapping(value = "/metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribeToMetrics() {
    String sessionId = UUID.randomUUID().toString();
    return systemMetricsSseService.subscribeToMetrics(sessionId);
}
```

---

## Pattern 5: Client-Side JavaScript

### Vanilla JavaScript

```javascript
// Subscribe to SSE stream
const eventSource = new EventSource('/api/sse/subscribe');

// Handle connection
eventSource.addEventListener('connected', (event) => {
  console.log('SSE connected:', event.data);
});

// Handle notifications
eventSource.addEventListener('notification', (event) => {
  const notification = JSON.parse(event.data);
  console.log('Received notification:', notification);

  // Display notification
  showNotification(notification.title, notification.message);
});

// Handle order updates
eventSource.addEventListener('order-update', (event) => {
  const order = JSON.parse(event.data);
  console.log('Order updated:', order);

  // Update UI
  updateOrderStatus(order.orderId, order.status);
});

// Handle errors
eventSource.onerror = (error) => {
  console.error('SSE error:', error);
  // EventSource will automatically reconnect
};

// Close connection (when needed)
function closeConnection() {
  eventSource.close();
}
```

### Vue 3 Composition API

```vue
<template>
  <div class="notification-list">
    <a-card v-for="notification in notifications" :key="notification.id">
      <h4>{{ notification.title }}</h4>
      <p>{{ notification.message }}</p>
      <span>{{ formatTime(notification.timestamp) }}</span>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';

const notifications = ref([]);
let eventSource = null;

onMounted(() => {
  // Subscribe to SSE
  eventSource = new EventSource('/api/sse/subscribe');

  eventSource.addEventListener('connected', (event) => {
    message.success('Connected to notification stream');
  });

  eventSource.addEventListener('notification', (event) => {
    const notification = JSON.parse(event.data);
    notifications.value.unshift(notification);

    // Show toast
    message.info(notification.title);
  });

  eventSource.onerror = (error) => {
    console.error('SSE error:', error);
    message.error('Connection error, reconnecting...');
  };
});

onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
  }
});

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString();
};
</script>
```

---

## Pattern 6: CORS Configuration

```java
@Configuration
public class SseCorsConfiguration {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/sse/**")
                    .allowedOrigins("http://localhost:5173", "https://admin.example.com")
                    .allowedMethods("GET")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

---

## Pattern 7: Performance Optimization

### Connection Pool with Limits

```java
@Service
public class LimitedSseEmitterService {

    private static final int MAX_CONNECTIONS = 1000;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) throws TooManyConnectionsException {
        if (emitters.size() >= MAX_CONNECTIONS) {
            throw new TooManyConnectionsException("Maximum SSE connections reached");
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }
}
```

### Async Message Sending

```java
@Service
@RequiredArgsConstructor
public class AsyncSseService {

    private final SseEmitterService sseEmitterService;
    private final Executor taskExecutor;

    public void sendAsync(Long userId, NotificationVO notification) {
        CompletableFuture.runAsync(() -> {
            sseEmitterService.sendToUser(userId, notification);
        }, taskExecutor);
    }

    public void broadcastAsync(NotificationVO notification) {
        CompletableFuture.runAsync(() -> {
            sseEmitterService.broadcast(notification);
        }, taskExecutor);
    }
}
```

---

## Best Practices

1. **Timeout Configuration:**
   - Set reasonable timeout (30-60 minutes)
   - Client will auto-reconnect on timeout
   - Send heartbeat to keep connection alive

2. **Error Handling:**
   - Always handle onCompletion, onTimeout, onError
   - Remove emitter from map on error
   - Log errors for debugging

3. **Memory Management:**
   - Limit max connections per server
   - Clean up emitters on disconnect
   - Monitor active connection count

4. **Authentication:**
   - Validate user token before subscribing
   - Use Sa-Token integration
   - Support token in query parameter for SSE

5. **Event Naming:**
   - Use descriptive event names
   - Document event types for frontend
   - Use consistent naming convention

6. **Performance:**
   - Use async sending for broadcast
   - Implement message batching if needed
   - Monitor CPU/memory usage

---

**Next:** [Vue Client Integration](vue-client-integration.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
