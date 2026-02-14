---
name: message-queue-pattern-generator
description: "消息隊列整合模式生成"
priority: P2
category: integration
---

# Message Queue Pattern Generator

為 SmartAdmin 應用程式生成 Kafka/RocketMQ 消息隊列整合模式，包括 Event Sourcing、CQRS 和 Saga 模式。

## Usage

```
User: "Add Kafka event publishing for order status changes"
AI: [Generate producer, consumer, event schema, idempotency]
```

## When to Use

- 實現異步消息處理
- 服務間解耦通訊
- Event Sourcing 架構
- CQRS 讀寫分離
- 分散式事務 (Saga)

## Generated Output

- Kafka/RocketMQ 配置
- Producer 生產者
- Consumer 消費者
- 事件 Schema
- 冪等性處理
- 死信隊列配置

## Workflow

1. **設計事件結構**
   - 定義事件類別
   - JSON Schema 設計

2. **配置消息隊列**
   - Kafka/RocketMQ 連接配置
   - Topic 設定

3. **實現生產者**
   - KafkaTemplate 發送
   - 事務性發送

4. **實現消費者**
   - @KafkaListener 監聽
   - 冪等性保證

5. **錯誤處理**
   - 重試機制
   - 死信隊列

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)

## Example Session

**User:** 為訂單狀態變更添加 Kafka 事件發布

**AI Agent Actions:**
1. 創建 `OrderStatusChangedEvent` 事件類
2. 配置 Kafka producer 和 topic
3. 實現 `OrderEventPublisher` 發布服務
4. 創建 `OrderEventConsumer` 消費者
5. 添加冪等性檢查 (event_id + Redis)
6. 配置死信隊列處理失敗消息
