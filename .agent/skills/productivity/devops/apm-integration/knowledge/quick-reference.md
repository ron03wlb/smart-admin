# APM Integration - Quick Reference

## Skywalking Agent 配置

```bash
# JVM 啟動參數
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=smartadmin
-Dskywalking.collector.backend_service=oap-server:11800
```

## Micrometer 指標類型

| 類型 | 用途 | 範例 |
|------|------|------|
| Counter | 累計計數 | 請求總數、錯誤數 |
| Gauge | 當前值 | 連線池大小、隊列長度 |
| Timer | 延遲分佈 | 請求延遲、方法執行時間 |
| Histogram | 數值分佈 | 響應大小、批次處理量 |

## 自定義指標範例

```java
@RequiredArgsConstructor
public class WalletMetrics {
    private final MeterRegistry registry;

    public void recordDeposit(BigDecimal amount) {
        registry.counter("wallet.deposit.count").increment();
        registry.counter("wallet.deposit.amount").increment(amount.doubleValue());
    }

    public void recordWithdrawal(BigDecimal amount, long durationMs) {
        registry.counter("wallet.withdrawal.count").increment();
        registry.timer("wallet.withdrawal.duration")
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

## Grafana 儀表板 PromQL

```promql
# QPS
rate(http_server_requests_seconds_count{uri="/api/v1/wallet/deposit"}[5m])

# P95 延遲
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 錯誤率
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
  / rate(http_server_requests_seconds_count[5m])
```

## 告警規則範例

```yaml
groups:
  - name: smartadmin-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
```
