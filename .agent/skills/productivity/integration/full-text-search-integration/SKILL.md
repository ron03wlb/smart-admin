---
name: full-text-search-integration
description: "Elasticsearch 全文搜索整合"
priority: P2
category: integration
---

# Full-Text Search Integration (Elasticsearch)

為 SmartAdmin 應用程式生成 Elasticsearch 整合，包括文檔映射、搜索 API、聚合查詢和索引生命週期管理。

## Usage

```
User: "Add Elasticsearch search to ProductEntity with autocomplete"
AI: [Generate document mapping, search API, sync strategy]
```

## When to Use

- 實現進階全文搜索
- 超越 MyBatis LIKE 查詢性能
- 商品/文章模糊搜索
- 自動完成 (autocomplete)
- 業務分析和聚合報表

## Generated Output

- Elasticsearch 文檔映射
- 搜索 API (fuzzy, phrase, autocomplete)
- 聚合查詢 (terms, histogram, metrics)
- 資料同步策略 (dual-write, CDC)
- 搜索結果高亮

## Workflow

1. **設計文檔結構**
   - 從 Entity 生成 ES 映射
   - 配置分析器 (analyzer)

2. **實現搜索 API**
   - 全文搜索
   - 模糊搜索 (fuzzy)
   - 自動完成 (completion suggester)

3. **配置聚合查詢**
   - 分組統計 (terms aggregation)
   - 數值分析 (sum, avg, percentiles)

4. **設置資料同步**
   - 雙寫策略
   - CDC 變更捕獲 (Debezium)

5. **優化搜索性能**
   - 路由策略
   - 分片配置

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)

## Example Session

**User:** 為商品模塊添加 Elasticsearch 搜索，支援模糊搜索和自動完成

**AI Agent Actions:**
1. 創建 `ProductDocument` 映射類
2. 配置 IK 分詞器 (中文支援)
3. 實現 `ProductSearchService` 搜索服務
4. 添加 completion suggester 自動完成
5. 配置雙寫同步策略
