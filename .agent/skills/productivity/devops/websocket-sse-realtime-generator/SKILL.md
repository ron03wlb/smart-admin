---
name: websocket-sse-realtime-generator
description: "即時通訊生成 (WebSocket/SSE)"
priority: P2
category: devops
---

# WebSocket/SSE Real-time Generator

為 SmartAdmin 應用程式生成 WebSocket/SSE (Server-Sent Events) 即時通訊功能，支援 STOMP 協議、身份驗證、廣播和水平擴展。

## Usage

```
User: "Add WebSocket for real-time order status updates"
AI: [Generate WebSocket endpoint, authentication, Vue client]
```

## When to Use

- 實現即時儀表板更新
- 推送通知系統
- 即時聊天功能
- 訂單狀態即時更新
- 庫存變動推送

## Generated Output

- WebSocket 端點 (STOMP over WebSocket)
- SSE 端點 (Server-Sent Events)
- Sa-Token 身份驗證整合
- Vue 3 前端組件
- Redis Pub/Sub 水平擴展配置

## Workflow

1. **選擇通訊方式**
   - WebSocket (雙向通訊)
   - SSE (伺服器單向推送)

2. **生成後端端點**
   - STOMP 配置
   - 訊息處理器
   - 身份驗證攔截器

3. **實現廣播模式**
   - Topic 廣播 (所有訂閱者)
   - User 點對點 (特定用戶)
   - Role 角色群發

4. **整合 Vue 前端**
   - SockJS 客戶端
   - 自動重連邏輯
   - 訊息佇列處理

5. **配置水平擴展**
   - Redis Pub/Sub
   - 多實例部署支援

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)

## Example Session

**User:** 為管理後台添加即時通知功能，支援多實例部署

**AI Agent Actions:**
1. 創建 WebSocket 配置類 `WebSocketConfig`
2. 實現 `NotificationWebSocketHandler`
3. 整合 Sa-Token 驗證用戶身份
4. 創建 Vue 通知組件 `NotificationWebSocket.vue`
5. 配置 Redis Pub/Sub 支援多實例
6. 添加心跳檢測和自動重連
