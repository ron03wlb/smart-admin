---
name: postgresql-best-practices
description: "PostgreSQL 效能分析與優化"
priority: P2
category: integration
---

# PostgreSQL Best Practices

PostgreSQL 效能分析和優化技能，提供全面的資料庫效能診斷和優化建議。

## Usage

```
User: "Analyze database performance issues causing timeouts"
AI: [Run HikariCP analysis, N+1 detection, EXPLAIN ANALYZE]
```

## When to Use

- 資料庫連線超時問題
- HikariCP 連線池調優
- 慢查詢分析優化
- N+1 查詢檢測
- 索引優化建議

## Generated Output

- HikariCP 調優建議 (min-idle, max-pool-size)
- N+1 查詢模式清單
- EXPLAIN ANALYZE 執行計劃分析
- 索引建議 (CREATE INDEX 語句)
- 效能優化報告

## Workflow

1. **HikariCP 分析**
   - 連線池利用率計算
   - 最佳配置公式: `connections = (core_count × 2) + effective_spindle_count`

2. **N+1 查詢檢測**
   - 解析 P6Spy 日誌
   - 識別重複查詢模式

3. **EXPLAIN ANALYZE**
   - 分析慢查詢執行計劃
   - 識別 Seq Scan、Sort 瓶頸

4. **索引建議**
   - 基於 pg_stat_user_tables 分析
   - 生成 CREATE INDEX 語句

5. **產出報告**
   - Markdown 格式優化報告
   - 優先級排序建議

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)
- [D02-postgresql-advanced.md](../../../rules/technology/database/D02-postgresql-advanced.md)

## Example Session

**User:** 分析資料庫連線超時問題

**AI Agent Actions:**
1. 檢查 HikariCP 配置和指標
2. 分析連線池利用率
3. 解析 P6Spy 日誌檢測 N+1
4. 執行 EXPLAIN ANALYZE 分析慢查詢
5. 查詢 pg_stat_user_tables 建議索引
6. 生成優化報告和 SQL 腳本
