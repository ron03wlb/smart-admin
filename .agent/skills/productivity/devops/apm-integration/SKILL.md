---
name: apm-integration
description: "APM (應用效能監控) 整合生成"
priority: P2
category: devops
---

# APM Integration

生成完整的 APM (Application Performance Monitoring) 整合，包括 Skywalking、Micrometer、Grafana 儀表板和自定義指標，用於 SmartAdmin 應用程式的生產環境監控。

## Usage

```
User: "Add APM monitoring to UserService with custom metrics for login events"
AI: [Configure Skywalking, generate Micrometer metrics, create Grafana dashboard]
```

## When to Use

- 設置生產環境監控系統
- 除錯效能問題需要分散式追蹤
- 實現自定義業務指標
- 創建 Grafana 監控儀表板
- 配置告警規則 (延遲、錯誤率、吞吐量)

## Generated Output

- Skywalking Java agent 配置
- Micrometer 自定義指標 (counters, gauges, timers, histograms)
- Grafana 儀表板 JSON 模板
- 告警規則配置
- MDC 日誌關聯 (TraceId)

## Workflow

1. **配置 Skywalking Agent**
   - 添加 Java agent 啟動參數
   - 配置服務名稱和收集器地址

2. **整合 Micrometer**
   - 添加 Spring Boot Actuator 依賴
   - 配置 Prometheus registry
   - 生成自定義業務指標

3. **創建 Grafana 儀表板**
   - JVM 監控面板 (記憶體、GC、執行緒)
   - HTTP 請求面板 (延遲、QPS、錯誤率)
   - 資料庫連線池面板
   - 自定義業務指標面板

4. **配置告警規則**
   - P95/P99 延遲告警
   - 錯誤率閾值告警
   - 吞吐量異常告警

5. **啟用分散式追蹤**
   - TraceId/SpanId 傳遞
   - MDC 日誌整合

## Related Rules

- [D01-postgresql-basics.md](../../../rules/technology/database/D01-postgresql-basics.md)
- [04-exception-logging.md](../../../rules/technology/patterns/04-exception-logging.md)

## Example Session

**User:** 為 WalletService 添加 APM 監控，追蹤存款和提款交易

**AI Agent Actions:**
1. 配置 Skywalking agent 追蹤 WalletService
2. 創建 Micrometer Counter: `wallet.deposit.count`, `wallet.withdrawal.count`
3. 創建 Micrometer Timer: `wallet.transaction.duration`
4. 生成 Grafana 儀表板顯示交易量和延遲
5. 設置錯誤率 > 1% 的告警規則
