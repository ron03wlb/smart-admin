---
name: smartadmin-performance-suite
description: "效能優化套件 (診斷 → 優化 → 監控)"
priority: P2
category: composite
---

# SmartAdmin Performance Suite

綜合效能優化套件，整合診斷、優化和監控三個階段，提供端到端的效能改善方案。

## Usage

```
User: "Diagnose performance issues in Order module"
AI: [Run N+1 detection, JVM profiling, cache analysis, then optimize]
```

## When to Use

- 效能問題診斷
- 系統瓶頸分析
- 快取策略優化
- APM 監控設置
- 端到端效能改善

## Generated Output

根據選擇的模式生成不同輸出：
- **diagnose**: N+1 報告、JVM 分析、熱點方法
- **optimize**: 快取配置、查詢優化、索引建議
- **monitor**: APM 配置、Grafana 儀表板、告警規則

## Modes

### 1. Diagnose Mode (診斷)
- N+1 查詢檢測
- JVM 效能分析
- CPU 熱點識別
- 記憶體洩漏檢查

### 2. Optimize Mode (優化)
- 多層快取實現 (Caffeine L1 + Redis L2)
- 查詢優化
- 連線池調優

### 3. Monitor Mode (監控)
- Skywalking/Micrometer 整合
- Grafana 儀表板
- 告警規則配置

## Workflow

1. **診斷階段**
   - 收集效能指標
   - 識別瓶頸點
   - 生成診斷報告

2. **優化階段**
   - 根據診斷結果制定優化方案
   - 實施快取策略
   - 優化資料庫查詢

3. **監控階段**
   - 部署 APM 整合
   - 創建監控儀表板
   - 設置告警規則

## Consolidates

此技能整合以下獨立技能的功能：
- [cache-strategy-generator](../integration/cache-strategy-generator/)
- [postgresql-best-practices](../integration/postgresql-best-practices/)
- [apm-integration](../devops/apm-integration/)

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)

## Example Session

**User:** 診斷訂單模塊的效能問題

**AI Agent Actions:**

**Phase 1 - Diagnose:**
1. 解析 P6Spy 日誌檢測 N+1 查詢
2. 分析 JVM 堆使用情況
3. 識別 CPU 熱點方法
4. 生成診斷報告

**Phase 2 - Optimize (基於診斷結果):**
5. 為高頻查詢添加 L1+L2 快取
6. 優化 N+1 查詢 (JOIN/批量查詢)
7. 建議索引優化

**Phase 3 - Monitor:**
8. 配置 Micrometer 自定義指標
9. 創建效能監控儀表板
10. 設置延遲和錯誤率告警
