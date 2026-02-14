# SmartAdmin Performance Suite - Examples

## 範例 1: 完整效能優化流程

### Phase 1: Diagnose

**執行診斷**:
```
User: "診斷訂單模塊效能問題，最近 API 響應變慢"
```

**診斷報告**:
```markdown
# 訂單模塊效能診斷報告

## 1. N+1 查詢問題

發現 3 處 N+1:
| 位置 | 查詢次數 | 影響 |
|------|----------|------|
| OrderService.list() | 100+ | 高 |
| OrderService.getDetail() | 50+ | 中 |
| ReportService.generate() | 200+ | 高 |

## 2. JVM 分析

- 堆使用率: 82% (警告)
- GC 頻率: 15次/分鐘 (偏高)
- Full GC: 2次/小時

## 3. CPU 熱點

Top 3 方法:
1. OrderDao.selectList() - 35%
2. JSON 序列化 - 20%
3. 日誌寫入 - 15%

## 4. 資料庫

- 連線池利用率: 75%
- 慢查詢 (>1s): 12 條/小時
```

### Phase 2: Optimize

**基於診斷結果的優化**:

```java
// 1. 修復 N+1 - 使用 JOIN
@Select("""
    SELECT o.*, u.user_name, p.product_name
    FROM t_order o
    LEFT JOIN t_user u ON o.user_id = u.user_id
    LEFT JOIN t_product p ON o.product_id = p.product_id
    WHERE o.deleted_flag = false
    """)
List<OrderDetailVO> selectWithDetails(OrderQueryForm form);

// 2. 添加快取
@Cacheable(value = "orders", key = "#orderId")
public OrderVO getById(Long orderId) {
    return orderDao.selectById(orderId);
}

// 3. 索引優化
-- CREATE INDEX idx_order_user_time ON t_order(user_id, create_time DESC);
```

### Phase 3: Monitor

**配置監控**:
```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;

    public void recordOrderCreation(long durationMs) {
        registry.timer("order.creation.duration").record(durationMs, TimeUnit.MILLISECONDS);
        registry.counter("order.creation.count").increment();
    }
}
```

**Grafana 儀表板**:
- 訂單創建延遲 (P50/P95/P99)
- 訂單量趨勢
- 錯誤率監控
- 資料庫連線池狀態

**告警規則**:
```yaml
- alert: OrderAPIHighLatency
  expr: histogram_quantile(0.95, rate(order_creation_duration_bucket[5m])) > 0.5
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "訂單 API P95 延遲超過 500ms"
```

---

## 範例 2: 僅執行診斷模式

```
User: "用 diagnose 模式分析用戶模塊效能"
```

**輸出**:
```markdown
# 用戶模塊效能診斷

## 執行摘要
- 診斷時間: 2026-02-05 10:30
- 分析範圍: UserService, UserDao
- 整體健康度: ⚠️ 中等

## 關鍵發現

1. **N+1 查詢** - 嚴重
   - UserService.list() 觸發 Department 查詢 N 次
   - 建議: 使用 JOIN 或批量查詢

2. **缺少索引** - 中等
   - t_user.phone 欄位頻繁查詢但無索引
   - 建議: CREATE INDEX idx_user_phone ON t_user(phone)

3. **快取命中率低** - 輕微
   - 當前命中率: 45%
   - 建議: 增加 TTL 至 10 分鐘

## 下一步建議

執行 `optimize` 模式自動生成修復代碼
```
