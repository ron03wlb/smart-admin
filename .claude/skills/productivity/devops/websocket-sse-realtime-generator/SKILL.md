---
name: websocket-sse-realtime-generator
description: [P2 - Productivity] Generate WebSocket/SSE (Server-Sent Events) real-time communication for SmartAdmin applications with STOMP, authentication, broadcasting, heartbeat, backpressure, and Redis Pub/Sub for horizontal scaling. Use when implementing real-time dashboards, notifications, chat, or live updates. Triggers when user mentions "WebSocket", "SSE", "real-time", "live updates", "push notifications", "chat", or "dashboard updates".
---

# WebSocket/SSE Real-time Generator

**Priority:** P1 - Roadmap Priority #4
**Sprint:** 5 (Weeks 16-18)
**Status:** ✅ Production Ready

## Purpose

Enable real-time communication by generating WebSocket/SSE integration. Delivers notifications and live updates with < 500ms latency.

## Problem Statement

**User Roadmap Need:** "实时更新 (WebSocket/SSE)" (Real-time updates)

**Current Issues:**
- Admin dashboards need real-time updates
- Manual WebSocket implementation is complex
- No systematic real-time notification system
- Missing horizontal scaling support
- Reconnection logic is error-prone

## Solution Overview

This skill generates:
- ✅ WebSocket endpoint generation (STOMP over WebSocket with Spring)
- ✅ Server-Sent Events (SSE) endpoint generation (simpler alternative for server-to-client)
- ✅ Authentication/authorization for WebSocket (Sa-Token integration)
- ✅ Message broadcasting patterns (topic-based, user-specific, role-based)
- ✅ Heartbeat and reconnection logic (client-side auto-reconnect)
- ✅ Backpressure handling (slow consumer detection, message buffering)
- ✅ Client-side JavaScript/Vue integration (SockJS client, reconnection strategies, message queuing)
- ✅ Scalability with Redis Pub/Sub for multi-instance deployments
- ✅ Real-time dashboard component templates (Vue 3 + Ant Design)

## Quick Start

**Most common usage:**
```
User: "Add WebSocket for real-time order status updates in the dashboard"
```

You will:
1. Generate WebSocket endpoint (STOMP)
2. Add Sa-Token authentication
3. Implement topic-based broadcasting
4. Create Vue dashboard component
5. Add heartbeat and reconnection
6. Set up Redis Pub/Sub (if multi-instance)

## Scope

### Included
- WebSocket endpoint (STOMP over WebSocket)
- SSE endpoint (server-to-client alternative)
- Sa-Token authentication
- Broadcasting patterns (topic, user, role)
- Heartbeat and reconnection
- Backpressure handling
- Vue client integration (SockJS)
- Redis Pub/Sub for scaling
- Dashboard component templates

### Not Included
- Custom protocol implementation (uses standard STOMP)
- Video/audio streaming (use specialized solutions)
- P2P communication (use WebRTC)

## Integration Points

- Uses Spring WebSocket and SockJS
- Integrates with `smartadmin-vue-crud` for real-time UI updates
- Works with `message-queue-pattern-generator` for event broadcasting
- Compatible with `apm-integration-skill` for real-time metrics

## Success Criteria

- ✅ Real-time notifications delivered with < 500ms latency
- ✅ WebSocket connections stable for 24+ hours (with heartbeat)
- ✅ Horizontal scaling validated (3+ instances with Redis Pub/Sub)
- ✅ Client auto-reconnection working seamlessly
- ✅ Dashboard updates in real-time (order status, system metrics, alerts)

## Detailed Documentation

### Implementation Guides

1. **[WebSocket STOMP Patterns](references/websocket-stomp-patterns.md)**
   - WebSocket configuration with STOMP
   - Sa-Token authentication interceptor
   - Message broadcasting (topic-based, user-specific, role-based)
   - Heartbeat and connection management
   - Backpressure handling
   - Real-time order updates example
   - System metrics dashboard
   - **Lines:** ~650+ lines with 6 patterns

2. **[SSE Patterns](references/sse-patterns.md)**
   - SSE vs WebSocket comparison
   - Basic SSE endpoint (SseEmitter)
   - Heartbeat (keep-alive)
   - Event types (multiple event streams)
   - Real-time dashboard updates
   - Client-side JavaScript/Vue integration
   - CORS configuration
   - Performance optimization
   - **Lines:** ~600+ lines with 7 patterns

3. **[Vue Client Integration](references/vue-client-integration.md)**
   - WebSocket composable (SockJS + STOMP)
   - SSE composable
   - Notification component (WebSocket + SSE)
   - Real-time dashboard (system metrics with Chart.js)
   - Order status updates
   - Connection status indicator
   - Auto reconnection with exponential backoff
   - **Lines:** ~700+ lines with 5 patterns

4. **[Redis Pub/Sub Scaling](references/redis-pubsub-scaling.md)**
   - Horizontal scaling architecture
   - Redis configuration
   - Message publisher (broadcast, user-specific)
   - Message subscriber (route to local connections)
   - SSE with Redis integration
   - Session affinity (sticky sessions with Nginx)
   - Monitoring and metrics
   - **Lines:** ~650+ lines with 5 patterns

## Implementation Workflow

### Step 1: Add Dependencies (2 minutes)

```gradle
dependencies {
    // Spring WebSocket
    implementation 'org.springframework.boot:spring-boot-starter-websocket:3.2.2'
}
```

### Step 2: Configure WebSocket (5 minutes)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
```

### Step 3: Add Sa-Token Authentication (10 minutes)

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            StpUtil.checkLogin(token);
            Long userId = StpUtil.getLoginIdAsLong();
            accessor.getSessionAttributes().put("userId", userId);
        }
        return message;
    }
}
```

### Step 4: Create Broadcasting Service (10 minutes)

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastNotification(NotificationVO notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    public void sendToUser(Long userId, NotificationVO notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }
}
```

### Step 5: Create Vue Client (15 minutes)

```javascript
// composables/useWebSocket.js
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export function useWebSocket() {
  const socket = new SockJS('/ws');
  const stompClient = Stomp.over(socket);

  stompClient.connect({ Authorization: getToken() }, (frame) => {
    console.log('Connected:', frame);

    stompClient.subscribe('/topic/notifications', (message) => {
      const notification = JSON.parse(message.body);
      showNotification(notification);
    });
  });

  return { stompClient };
}
```

### Step 6: Set Up Redis Pub/Sub (20 minutes - for multi-instance)

**Configuration:**
```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter adapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(adapter, new PatternTopic("websocket:*"));
    return container;
}
```

**Publisher:**
```java
public void publishToAll(String topic, NotificationVO notification) {
    redisTemplate.convertAndSend("websocket:" + topic, notification);
}
```

**Subscriber:**
```java
public void onMessage(Message message, byte[] pattern) {
    WebSocketMessage wsMessage = deserialize(message);
    messagingTemplate.convertAndSend(wsMessage.getDestination(), wsMessage.getPayload());
}
```

### Step 7: Test Real-time Communication (5 minutes)

```bash
# Connect to WebSocket
wscat -c ws://localhost:1024/ws

# Subscribe to topic
{"command":"SUBSCRIBE","destination":"/topic/notifications"}

# Trigger notification (from API)
curl -X POST "http://localhost:1024/api/notification/broadcast" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","message":"Hello WebSocket"}'
```

### Step 8: Deploy and Monitor (5 minutes)

**Check connections:**
```bash
# Prometheus metrics
curl http://localhost:1024/actuator/prometheus | grep websocket_connections
```

**Load balancer (Nginx):**
```nginx
upstream smartadmin_backend {
    ip_hash;  # Sticky sessions
    server backend1:1024;
    server backend2:1024;
}
```

**Total Time:** ~70 minutes (vs 3-5 days manual implementation)

## Troubleshooting Guide

### Issue: Connection refused

**Solution:** Check WebSocket endpoint is registered
```bash
curl -I http://localhost:1024/ws
# Should return HTTP/1.1 200 OK
```

### Issue: Authentication failed

**Solution:** Verify Sa-Token header is sent
```javascript
stompClient.connect({ Authorization: getToken() }, onConnect, onError);
```

### Issue: Messages not delivered

**Solution:** Check subscription destination matches publish destination
```
Publish: /topic/notifications
Subscribe: /topic/notifications ✅ (match)
Subscribe: /topic/orders ❌ (no match)
```

### Issue: High memory usage

**Solution:** Limit connection count and message buffer
```java
@Value("${websocket.max-connections:1000}")
private int maxConnections;
```

## Performance Impact

**Expected Improvements:**
- Message latency: < 500ms (vs 10+ seconds with polling)
- Development time: 3-5 days → 70 minutes (97% reduction)
- Server resources: 10KB memory per connection
- Scalability: 10k+ concurrent connections per server

**Resource Requirements:**
- WebSocket server: 2GB RAM (10k connections)
- Redis (for scaling): 512MB RAM
- Load balancer: Sticky sessions enabled

---

---

**Version:** 1.0.0
**Created:** 2026-01-26
**Sprint:** 5 (Weeks 16-18)
**Status:** ✅ Production Ready
**Last Updated:** 2026-01-26
