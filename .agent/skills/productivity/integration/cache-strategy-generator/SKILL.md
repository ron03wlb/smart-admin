---
name: cache-strategy-generator
description: "多層快取策略生成"
priority: P2
category: integration
---

# Cache Strategy Generator

生成系統化的多層快取策略 (Caffeine L1 + Redis L2)，包括 cache-aside、write-through、write-behind 模式、失效策略和快取預熱。

## Usage

```
User: "Add two-level caching to ProductService.getById with 5-minute TTL"
AI: [Generate L1+L2 cache config, cache-aside pattern, invalidation]
```

## When to Use

- 優化慢查詢效能
- 實現多層快取架構
- 解決快取穿透/雪崩問題
- 需要快取失效策略
- 快取預熱需求

## Generated Output

- 多層快取配置 (L1: Caffeine, L2: Redis)
- 快取模式實現 (cache-aside, write-through)
- 失效策略 (TTL, event-driven, manual)
- 快取預熱邏輯
- 分散式鎖整合 (Redisson)

## Workflow

1. **分析快取需求**
   - 確認快取目標 (Entity, DTO)
   - 評估資料一致性需求

2. **配置 L1 快取 (Caffeine)**
   - 本地記憶體快取
   - 設定容量和過期時間

3. **配置 L2 快取 (Redis)**
   - 分散式快取
   - 序列化策略

4. **實現快取模式**
   - Cache-aside (讀優先)
   - Write-through (寫同步)

5. **設置失效策略**
   - TTL 過期
   - 事件驅動失效
   - 快取標籤分組

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)

## Example Session

**User:** 為 ProductService.getById 添加兩層快取，TTL 5 分鐘

**AI Agent Actions:**
1. 創建 `CacheConfig` 配置 Caffeine + Redis
2. 實現 `@TwoLevelCacheable` 自定義註解
3. 在 Service 方法添加快取邏輯
4. 實現快取失效觸發 (UPDATE/DELETE 時)
5. 添加快取命中率監控
