# SmartAdmin Performance Suite - Quick Reference

## 模式選擇指南

| 情境 | 推薦模式 | 主要輸出 |
|------|----------|----------|
| 效能問題初診 | diagnose | 診斷報告 |
| 已知瓶頸優化 | optimize | 優化配置 |
| 上線後監控 | monitor | 儀表板 + 告警 |
| 完整改善流程 | diagnose → optimize → monitor | 全套 |

## Diagnose Mode 工具

```bash
# N+1 檢測 (P6Spy 日誌)
./gradlew :smartadmin-app:test --tests *IntegrationTest -Pspy=true
grep -E "SELECT.*WHERE" logs/spy.log | sort | uniq -c | sort -rn

# JVM 堆分析
jcmd <pid> GC.heap_info
jmap -histo:live <pid> | head -20

# CPU 熱點 (async-profiler)
./profiler.sh -d 30 -f profile.html <pid>
```

## Optimize Mode 速查

**快取配置**:
```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES));
    return manager;
}
```

**N+1 修復**:
```java
// Before: N+1
employees.forEach(e -> e.setDept(deptDao.selectById(e.getDeptId())));

// After: JOIN
@Select("SELECT e.*, d.dept_name FROM t_employee e LEFT JOIN t_department d ...")
List<EmployeeVO> selectWithDept();
```

## Monitor Mode 配置

**Micrometer 指標**:
```java
@Timed(value = "order.create", description = "Order creation time")
public OrderVO createOrder(OrderForm form) { ... }
```

**Grafana 面板 PromQL**:
```promql
# P95 延遲
histogram_quantile(0.95, rate(order_create_seconds_bucket[5m]))

# 錯誤率
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
  / rate(http_server_requests_seconds_count[5m])
```

## 效能指標基準

| 指標 | 良好 | 警告 | 危險 |
|------|------|------|------|
| API P95 延遲 | < 200ms | 200-500ms | > 500ms |
| 錯誤率 | < 0.1% | 0.1-1% | > 1% |
| DB 連線利用率 | < 50% | 50-80% | > 80% |
| JVM 堆使用率 | < 70% | 70-85% | > 85% |
