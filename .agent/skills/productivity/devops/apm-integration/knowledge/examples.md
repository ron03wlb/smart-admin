# APM Integration - Examples

## 範例 1: 電商訂單監控

**需求**: 監控訂單服務的效能和業務指標

**生成配置**:

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;

    public void recordOrderCreated(String orderType) {
        registry.counter("order.created", "type", orderType).increment();
    }

    public void recordOrderPayment(BigDecimal amount, long durationMs) {
        registry.counter("order.payment.total").increment();
        registry.summary("order.payment.amount").record(amount.doubleValue());
        registry.timer("order.payment.duration").record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

**Grafana 面板**:
- 訂單創建趨勢圖 (按類型分組)
- 支付金額統計
- 支付延遲 P50/P95/P99

---

## 範例 2: 分散式追蹤

**需求**: 追蹤跨服務的 API 調用鏈路

**MDC 日誌整合**:

```java
@Component
public class TraceIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = TraceContext.traceId();
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

**logback.xml 配置**:
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

---

## 範例 3: 資料庫連線池監控

**Grafana PromQL**:
```promql
# 活躍連線數
hikaricp_connections_active{pool="HikariPool-1"}

# 等待連線數
hikaricp_connections_pending{pool="HikariPool-1"}

# 連線獲取耗時
histogram_quantile(0.95, rate(hikaricp_connections_acquire_seconds_bucket[5m]))
```

**告警**: 當等待連線數 > 10 持續 2 分鐘時告警
