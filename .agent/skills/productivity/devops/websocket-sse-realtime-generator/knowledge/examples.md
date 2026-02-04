# WebSocket/SSE Real-time Generator - Examples

## 範例 1: 即時通知系統

**後端 - 通知服務**:
```java
@Service
@RequiredArgsConstructor
public class NotificationPushService {
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    // 廣播系統通知
    public void broadcastSystemNotification(NotificationVO notification) {
        // 本地廣播
        messagingTemplate.convertAndSend("/topic/system-notifications", notification);

        // Redis Pub/Sub (多實例同步)
        redisTemplate.convertAndSend("channel:notifications",
            JsonUtil.toJson(notification));
    }

    // 發送給特定用戶
    public void sendToUser(Long userId, NotificationVO notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/personal-notifications",
            notification
        );
    }
}
```

**前端 - Vue 組件**:
```vue
<template>
  <a-badge :count="unreadCount">
    <a-dropdown :trigger="['click']">
      <bell-outlined />
      <template #overlay>
        <a-list :data-source="notifications">
          <template #renderItem="{ item }">
            <a-list-item>{{ item.content }}</a-list-item>
          </template>
        </a-list>
      </template>
    </a-dropdown>
  </a-badge>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'

const notifications = ref([])
const unreadCount = ref(0)

const { connect, subscribe, disconnect } = useWebSocket()

onMounted(async () => {
  await connect()

  subscribe('/topic/system-notifications', (message) => {
    notifications.value.unshift(JSON.parse(message.body))
    unreadCount.value++
  })

  subscribe('/user/queue/personal-notifications', (message) => {
    notifications.value.unshift(JSON.parse(message.body))
    unreadCount.value++
  })
})

onUnmounted(() => {
  disconnect()
})
</script>
```

---

## 範例 2: 訂單狀態即時更新

```java
@Service
@RequiredArgsConstructor
public class OrderStatusPushService {
    private final SimpMessagingTemplate messagingTemplate;

    public void pushOrderStatusUpdate(Long orderId, String status) {
        OrderStatusVO statusVO = new OrderStatusVO(orderId, status, LocalDateTime.now());

        // 推送給訂單擁有者
        Long userId = orderDao.findUserIdByOrderId(orderId);
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/order-status",
            statusVO
        );

        // 推送給管理員儀表板
        messagingTemplate.convertAndSend("/topic/admin/order-updates", statusVO);
    }
}
```

---

## 範例 3: SSE 即時庫存更新

```java
@RestController
@RequestMapping("/api/v1/inventory")
public class InventorySSEController {
    private final InventoryService inventoryService;

    @GetMapping(value = "/stream/{productId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<InventoryVO>> streamInventory(@PathVariable Long productId) {
        return Flux.interval(Duration.ofSeconds(5))
            .map(seq -> {
                InventoryVO inventory = inventoryService.getInventory(productId);
                return ServerSentEvent.<InventoryVO>builder()
                    .id(String.valueOf(seq))
                    .event("inventory-update")
                    .data(inventory)
                    .build();
            });
    }
}
```

**前端**:
```javascript
const eventSource = new EventSource('/api/v1/inventory/stream/12345')

eventSource.addEventListener('inventory-update', (event) => {
  const inventory = JSON.parse(event.data)
  updateInventoryDisplay(inventory)
})

eventSource.onerror = () => {
  // 自動重連由瀏覽器處理
  console.log('SSE connection error, reconnecting...')
}
```

---

## 範例 4: Redis Pub/Sub 多實例支援

```java
@Component
@RequiredArgsConstructor
public class RedisMessageListener implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationVO notification = objectMapper.readValue(
                message.getBody(), NotificationVO.class);

            // 轉發到本地 WebSocket 連線
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        } catch (Exception e) {
            log.error("處理 Redis 訊息失敗", e);
        }
    }
}
```

**Redis 配置**:
```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        RedisMessageListener listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listener, new ChannelTopic("channel:notifications"));
    return container;
}
```
