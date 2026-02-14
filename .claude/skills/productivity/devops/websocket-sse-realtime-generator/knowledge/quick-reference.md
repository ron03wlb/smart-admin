# WebSocket/SSE Realtime Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: websocket-sse-realtime-generator (P2 - Productivity/DevOps)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| WebSocket Server | Setup WebSocket endpoint | ~10 min |
| SSE Endpoint | Create Server-Sent Events | ~8 min |
| Client Integration | Frontend WebSocket/SSE client | ~12 min |
| Authentication | Add JWT/Token auth | ~8 min |
| Load Testing | Test concurrent connections | ~15 min |

---

## Technology Selection Matrix

### Pattern 1: WebSocket (Bi-directional)

**Use When**: Need real-time bi-directional communication

**Pros**:
- ✅ Full-duplex (client ↔ server)
- ✅ Lower latency than HTTP polling
- ✅ Binary data support

**Spring Boot Setup**:
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/ws/chat")
                .setAllowedOrigins("*")
                .withSockJS();  // Fallback for old browsers
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
}

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Broadcast to all sessions
        sessions.forEach(s -> {
            try {
                s.sendMessage(message);
            } catch (IOException e) {
                log.error("Failed to send message", e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}
```

**Time to Implement**: 10-15 minutes

---

### Pattern 2: Server-Sent Events (Uni-directional)

**Use When**: Only need server → client push (simpler than WebSocket)

**Pros**:
- ✅ Simpler than WebSocket
- ✅ Automatic reconnection
- ✅ Works over HTTP (easier firewall traversal)

**Cons**:
- ⚠️ Only server → client (not bi-directional)

**Spring Boot Setup**:
```java
@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SseEmitterManager emitterManager;

    /**
     * SSE endpoint for real-time notifications
     */
    @GetMapping("/notifications")
    public SseEmitter streamNotifications() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitterManager.addEmitter(emitter);

        emitter.onCompletion(() -> emitterManager.removeEmitter(emitter));
        emitter.onTimeout(() -> emitterManager.removeEmitter(emitter));
        emitter.onError(e -> emitterManager.removeEmitter(emitter));

        return emitter;
    }
}

@Service
public class SseEmitterManager {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
    }

    public void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    /**
     * Broadcast event to all connected clients
     */
    public void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove dead emitters
        deadEmitters.forEach(this::removeEmitter);
    }
}
```

**Frontend Client**:
```javascript
const eventSource = new EventSource('/api/sse/notifications');

eventSource.addEventListener('notification', (event) => {
    const data = JSON.parse(event.data);
    console.log('Notification:', data);
});

eventSource.onerror = () => {
    console.error('SSE connection error');
    eventSource.close();
};
```

**Time to Implement**: 8-12 minutes

---

### Pattern 3: STOMP over WebSocket (Messaging Protocol)

**Use When**: Need publish/subscribe pattern with message queuing

**Pros**:
- ✅ Topic-based subscriptions
- ✅ Message queuing support
- ✅ Works with RabbitMQ/ActiveMQ

**Spring Boot Setup**:
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketMessageConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}

@Controller
public class ChatController {

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage message) {
        message.setType(MessageType.JOIN);
        return message;
    }
}
```

**Frontend Client (SockJS + STOMP)**:
```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // Subscribe to topic
    stompClient.subscribe('/topic/public', (message) => {
        const data = JSON.parse(message.body);
        console.log('Message:', data);
    });

    // Send message
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        sender: 'User1',
        content: 'Hello!'
    }));
});
```

**Time to Implement**: 15-20 minutes

---

## Authentication Pattern

**JWT Authentication for WebSocket/SSE**:

```java
@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public WebSocketHandler authenticatedHandler(JwtService jwtService) {
        return new AuthenticatedWebSocketHandler(jwtService);
    }
}

public class AuthenticatedWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final Map<String, WebSocketSession> authenticatedSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Extract JWT from query params or headers
        String token = extractToken(session);

        if (token != null && jwtService.validateToken(token)) {
            Long userId = jwtService.getUserIdFromToken(token);
            authenticatedSessions.put(userId.toString(), session);
            log.info("Authenticated WebSocket: userId={}", userId);
        } else {
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            } catch (IOException e) {
                log.error("Failed to close session", e);
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        // From query params: /ws/chat?token=xxx
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return null;
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = authenticatedSessions.get(userId.toString());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("Failed to send to user: {}", userId, e);
            }
        }
    }
}
```

**Time to Implement**: 8-10 minutes

---

## Use Case Patterns

### Use Case 1: Real-time Notifications

**Scenario**: Notify users of new orders, messages, etc.

**Implementation (SSE)**:
```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseEmitterManager emitterManager;

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        NotificationVO notification = NotificationVO.builder()
            .type("ORDER_CREATED")
            .message("New order #" + event.getOrderId())
            .timestamp(LocalDateTime.now())
            .build();

        emitterManager.broadcast("notification", notification);
    }
}
```

**Time to Implement**: 5-8 minutes

---

### Use Case 2: Live Chat

**Scenario**: Real-time chat between users

**Implementation (WebSocket + STOMP)**:
```java
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId,
                                   ChatMessage message) {
        // Save to database
        chatService.saveMessage(roomId, message);

        // Broadcast to room
        return message;
    }
}
```

**Time to Implement**: 10-15 minutes

---

### Use Case 3: Live Dashboard Updates

**Scenario**: Update dashboard metrics in real-time

**Implementation (SSE)**:
```java
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SseEmitterManager emitterManager;

    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void broadcastMetrics() {
        DashboardMetrics metrics = DashboardMetrics.builder()
            .activeUsers(getActiveUserCount())
            .totalOrders(getTotalOrders())
            .revenue(getTotalRevenue())
            .timestamp(LocalDateTime.now())
            .build();

        emitterManager.broadcast("dashboard.metrics", metrics);
    }
}
```

**Time to Implement**: 8-12 minutes

---

## Performance Optimization

### Pattern: Connection Pooling

**Use When**: High concurrent connections (>1000)

```java
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/ws/chat")
                .setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                     WebSocketHandler handler,
                                                     Map<String, Object> attributes) {
                        // Use connection pooling for user sessions
                        return new CustomPrincipal(UUID.randomUUID().toString());
                    }
                });
    }
}
```

**Connection Limits**:
```yaml
server:
  tomcat:
    threads:
      max: 200  # Max threads for WebSocket connections
    max-connections: 10000  # Max concurrent connections
```

**Time to Configure**: 5 minutes

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| WebSocket Server | 10 min | 5 min | 15 min |
| SSE Endpoint | 8 min | 4 min | 12 min |
| STOMP over WebSocket | 15 min | 8 min | 23 min |
| JWT Authentication | 8 min | 5 min | 13 min |
| Frontend Client | 12 min | 8 min | 20 min |

**Full Real-time System**: 45-60 minutes

---

**See Also**:
- [APM Integration](../apm-integration/) - Monitor WebSocket connections
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - Real-time patterns
