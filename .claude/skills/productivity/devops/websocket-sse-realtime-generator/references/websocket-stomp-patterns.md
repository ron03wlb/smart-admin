# WebSocket with STOMP Patterns

**Skill:** websocket-sse-realtime-generator
**Component:** WebSocket / STOMP / Real-time Communication
**Purpose:** WebSocket integration patterns for SmartAdmin applications

---

## Dependencies

```gradle
dependencies {
    // Spring WebSocket
    implementation 'org.springframework.boot:spring-boot-starter-websocket:3.2.2'

    // Sa-Token WebSocket support
    implementation 'cn.dev33:sa-token-spring-boot3-starter:1.44.0'
}
```

---

## Pattern 1: WebSocket Configuration

### STOMP Configuration

```java
package net.lab1024.sa.support.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    /**
     * Configure message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for in-memory message broker
        registry.enableSimpleBroker("/topic", "/queue", "/user");

        // Set application destination prefix for @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();  // Fallback support for browsers without WebSocket
    }

    /**
     * Configure client inbound channel (add authentication interceptor)
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
```

### Authentication Interceptor (Sa-Token)

```java
package net.lab1024.sa.support.websocket;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /**
     * Intercept messages before they are sent to the channel
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Get token from STOMP headers
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Missing authorization token");
            }

            try {
                // Validate token with Sa-Token
                StpUtil.checkLogin(token);

                // Store user ID in session attributes
                Long userId = StpUtil.getLoginIdAsLong();
                accessor.getSessionAttributes().put("userId", userId);

                log.info("WebSocket authenticated: userId={}", userId);

            } catch (Exception e) {
                log.error("WebSocket authentication failed", e);
                throw new IllegalArgumentException("Invalid authorization token");
            }
        }

        return message;
    }
}
```

---

## Pattern 2: Message Broadcasting

### Topic-Based Broadcasting (One-to-Many)

```java
package net.lab1024.sa.business.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.domain.vo.NotificationVO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketNotificationController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * STOMP message mapping
     * Client sends to: /app/notification/subscribe
     * Response broadcast to: /topic/notifications
     */
    @MessageMapping("/notification/subscribe")
    @SendTo("/topic/notifications")
    public NotificationVO handleSubscription() {
        log.info("Client subscribed to notifications");
        return NotificationVO.builder()
            .title("Welcome")
            .message("You are now subscribed to notifications")
            .build();
    }

    /**
     * Server-side broadcast (triggered by business logic)
     */
    public void broadcastNotification(NotificationVO notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.info("Broadcasted notification to topic: {}", notification.getTitle());
    }
}

/**
 * REST API to trigger broadcast (for testing or external systems)
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
class NotificationApiController {

    private final WebSocketNotificationController webSocketController;

    @PostMapping("/broadcast")
    public ResponseDTO<Void> broadcastNotification(@RequestBody NotificationVO notification) {
        webSocketController.broadcastNotification(notification);
        return ResponseDTO.ok();
    }
}
```

### User-Specific Messages (One-to-One)

```java
/**
 * Send message to a specific user
 */
public void sendToUser(Long userId, NotificationVO notification) {
    // Destination: /user/{userId}/queue/notifications
    messagingTemplate.convertAndSendToUser(
        userId.toString(),
        "/queue/notifications",
        notification
    );
    log.info("Sent notification to user {}: {}", userId, notification.getTitle());
}
```

### Role-Based Broadcasting

```java
package net.lab1024.sa.business.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.domain.vo.NotificationVO;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmployeeDao employeeDao;

    /**
     * Broadcast to users with specific role
     */
    public void broadcastToRole(String roleName, NotificationVO notification) {
        // Query users with role
        List<Long> userIds = employeeDao.selectUserIdsByRole(roleName);

        // Send to each user
        userIds.forEach(userId -> {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
        });

        log.info("Broadcasted to role {}: {} users", roleName, userIds.size());
    }
}
```

---

## Pattern 3: Heartbeat and Connection Management

### Heartbeat Configuration

```yaml
# application.yml
spring:
  websocket:
    heartbeat:
      client-inbound: 10000  # Client heartbeat interval (ms)
      server-outbound: 10000 # Server heartbeat interval (ms)
```

### Connection Tracking

```java
package net.lab1024.sa.support.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketConnectionTracker {

    // Track active connections: sessionId -> userId
    private final Map<String, Long> activeConnections = new ConcurrentHashMap<>();

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long userId = (Long) accessor.getSessionAttributes().get("userId");

        activeConnections.put(sessionId, userId);
        log.info("WebSocket connected: sessionId={}, userId={}, total={}",
            sessionId, userId, activeConnections.size());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Long userId = activeConnections.remove(sessionId);

        log.info("WebSocket disconnected: sessionId={}, userId={}, total={}",
            sessionId, userId, activeConnections.size());
    }

    /**
     * Get active connection count
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(Long userId) {
        return activeConnections.containsValue(userId);
    }
}
```

---

## Pattern 4: Backpressure Handling

### Message Queue with Overflow Protection

```java
package net.lab1024.sa.support.websocket;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.notification.domain.vo.NotificationVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class BackpressureWebSocketSender {

    private final SimpMessagingTemplate messagingTemplate;

    // Per-user message queues (bounded)
    private final Map<Long, BlockingQueue<NotificationVO>> userQueues = new ConcurrentHashMap<>();

    // Background thread pool for sending messages
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static final int MAX_QUEUE_SIZE = 100;

    public BackpressureWebSocketSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send message to user with backpressure handling
     */
    public void sendToUserWithBackpressure(Long userId, NotificationVO notification) {
        BlockingQueue<NotificationVO> queue = userQueues.computeIfAbsent(
            userId,
            k -> new LinkedBlockingQueue<>(MAX_QUEUE_SIZE)
        );

        // Try to add message to queue
        boolean added = queue.offer(notification);

        if (!added) {
            log.warn("User {} message queue full, dropping message: {}",
                userId, notification.getTitle());
            return;
        }

        // Process queue in background
        executorService.submit(() -> processUserQueue(userId, queue));
    }

    /**
     * Process user's message queue
     */
    private void processUserQueue(Long userId, BlockingQueue<NotificationVO> queue) {
        while (!queue.isEmpty()) {
            try {
                NotificationVO notification = queue.poll(100, TimeUnit.MILLISECONDS);
                if (notification != null) {
                    messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        notification
                    );

                    // Rate limiting: sleep between messages
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Message processing interrupted for user {}", userId);
                break;
            } catch (Exception e) {
                log.error("Failed to send message to user {}", userId, e);
            }
        }
    }
}
```

---

## Pattern 5: Real-time Order Updates Example

### Order Status WebSocket

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.order.domain.entity.OrderEntity;
import net.lab1024.sa.business.order.domain.vo.OrderStatusVO;
import net.lab1024.sa.foundation.core.util.SmartBeanUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast order status update to all clients
     */
    public void broadcastOrderStatus(OrderEntity order) {
        OrderStatusVO vo = SmartBeanUtil.copy(order, OrderStatusVO.class);
        messagingTemplate.convertAndSend("/topic/orders", vo);
        log.info("Broadcasted order {} status: {}", order.getOrderId(), order.getStatus());
    }

    /**
     * Send order status update to specific user
     */
    public void sendOrderStatusToUser(Long userId, OrderEntity order) {
        OrderStatusVO vo = SmartBeanUtil.copy(order, OrderStatusVO.class);
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/orders",
            vo
        );
        log.info("Sent order {} status to user {}", order.getOrderId(), userId);
    }
}
```

### Manager Layer Integration

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final OrderDao orderDao;
    private final OrderWebSocketService webSocketService;

    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long orderId, String newStatus) {
        // Update database
        OrderEntity order = orderDao.selectById(orderId);
        order.setStatus(newStatus);
        orderDao.updateById(order);

        // Broadcast real-time update
        webSocketService.broadcastOrderStatus(order);
    }
}
```

---

## Pattern 6: System Metrics Dashboard

### Metrics Broadcasting

```java
package net.lab1024.sa.system.monitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.system.monitor.domain.vo.SystemMetricsVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMetricsBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast system metrics every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastSystemMetrics() {
        SystemMetricsVO metrics = collectSystemMetrics();
        messagingTemplate.convertAndSend("/topic/system-metrics", metrics);
        log.debug("Broadcasted system metrics: CPU={}, Memory={}",
            metrics.getCpuUsage(), metrics.getMemoryUsage());
    }

    private SystemMetricsVO collectSystemMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        return SystemMetricsVO.builder()
            .timestamp(System.currentTimeMillis())
            .cpuUsage(getCpuUsage())
            .memoryUsage((double) heapUsed / heapMax * 100)
            .heapUsed(heapUsed / 1024 / 1024)  // MB
            .heapMax(heapMax / 1024 / 1024)    // MB
            .build();
    }

    private double getCpuUsage() {
        // Implementation using OperatingSystemMXBean
        return 0.0;  // Placeholder
    }
}
```

---

## Best Practices

1. **Authentication:**
   - Always validate tokens in WebSocket interceptor
   - Store user ID in session attributes for later use
   - Use Sa-Token for consistent auth across HTTP and WebSocket

2. **Message Broadcasting:**
   - Use `/topic` for one-to-many (all subscribers)
   - Use `/queue` or `/user` for one-to-one (specific user)
   - Implement role-based broadcasting for targeted messages

3. **Connection Management:**
   - Track active connections for monitoring
   - Implement heartbeat to detect disconnections
   - Handle reconnection gracefully on client side

4. **Backpressure:**
   - Use bounded queues to prevent memory overflow
   - Implement rate limiting for high-frequency messages
   - Drop old messages if queue is full (or use eviction policy)

5. **Performance:**
   - Use SimpMessagingTemplate for async message sending
   - Batch messages when possible
   - Monitor connection count and message throughput

---

**Next:** [SSE Patterns](sse-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
