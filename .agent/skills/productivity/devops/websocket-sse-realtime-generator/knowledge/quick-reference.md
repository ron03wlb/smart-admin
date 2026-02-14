# WebSocket/SSE Real-time Generator - Quick Reference

## WebSocket 配置

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
```

## 訊息處理器

```java
@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final SimpMessagingTemplate messagingTemplate;

    // 廣播到所有訂閱者
    public void broadcastNotification(NotificationVO notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    // 發送給特定用戶
    public void sendToUser(Long userId, NotificationVO notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }
}
```

## SSE 端點

```java
@RestController
@RequestMapping("/api/v1/sse")
public class SSEController {

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents() {
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> ServerSentEvent.<String>builder()
                .id(String.valueOf(seq))
                .event("heartbeat")
                .data("ping")
                .build());
    }
}
```

## Vue 3 客戶端

```javascript
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
})

client.onConnect = () => {
  client.subscribe('/topic/notifications', (message) => {
    const notification = JSON.parse(message.body)
    showNotification(notification)
  })
}

client.activate()
```

## 廣播模式

| 模式 | 目的地 | 用途 |
|------|--------|------|
| Topic | `/topic/{name}` | 所有訂閱者 |
| User | `/user/{userId}/queue/{name}` | 特定用戶 |
| Role | 自定義邏輯 | 特定角色群組 |
