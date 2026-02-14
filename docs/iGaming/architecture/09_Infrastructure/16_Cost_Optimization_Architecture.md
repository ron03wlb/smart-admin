# 成本優化架構（Cost Optimization Architecture）

> **業務需求**: [Cost Optimization Requirements](../../requirements/09_Infrastructure_Requirements/02_Cost_Optimization_Requirements.md)
> **規範來源**: [09-10 Cost Optimization](../../source-archive/09_Technical_Infrastructure/09-10_Cost_Optimization.md)
> **視角**: Technical Architecture (Development & DevOps)

---

## 1. 成本優化總覽（Cost Optimization Overview）

| 項目 | 優化前 | 優化後 | 節省 | 節省率 |
|------|--------|--------|------|--------|
| 存儲成本 | $793.85 | $45.36 | $748.49 | -94% |
| 計算成本 | $0 | $552 | -$552 | N/A (新增) |
| 基礎設施 | $1,780.8 | $1,190.4 | $590.4 | -33% |
| 監控成本 | $245 | $0 | $245 | -100% |
| **月度總計** | **$2,819.65** | **$1,787.76** | **$1,031.89** | **-37%** |

**年度淨節省**: $12,383 | **ROI**: 87% (第一年) | **回收期**: 6.2 個月

---

## 2. 存儲成本優化（Storage Cost Optimization, -94%）

### 2.1 日誌分層存儲（Tiered Log Storage）

```mermaid
flowchart LR
    HOT[Hot Data<br/>PostgreSQL SSD<br/>0-2 months<br/>$6.90/mo] --> WARM[Warm Data<br/>S3 Standard<br/>3-6 months<br/>$2.76/mo]
    WARM --> COLD[Cold Data<br/>S3 Glacier<br/>7-19 months<br/>$1.56/mo]
    COLD --> ARCHIVE[Archive<br/>S3 Deep Archive<br/>20-84 months<br/>$1.93/mo]

    style HOT fill:#FF6B6B
    style WARM fill:#FFC107
    style COLD fill:#4FC3F7
    style ARCHIVE fill:#90A4AE
```

| 數據層 | 查詢延遲 | 查詢成本 | 適用場景 |
|--------|---------|---------|---------|
| Hot (PostgreSQL) | < 50ms | 無額外成本 | 實時業務查詢 |
| Warm (S3 Standard) | 2-5s | $0.002/GB | 客服查詢 |
| Cold (S3 Glacier) | 1-5min | $0.03/GB | 運營分析 |
| Archive (Deep Archive) | 12h | $0.02/GB | 審計法務 |

### 2.2 Kafka 日誌保留優化（Kafka Log Retention Optimization）

```properties
# server.properties
log.retention.hours=168  # 7 days (was 30 days)
log.segment.bytes=1073741824  # 1GB
log.retention.check.interval.ms=300000
log.cleanup.policy=delete
```

| 項目 | 優化前 | 優化後 | 節省 |
|------|--------|--------|------|
| 保留期 | 30 天 | 7 天 | - |
| 磁盤 | 500 GB | 120 GB | - |
| 成本 | $57.50/月 | $13.80/月 | -76% |

---

## 3. 計算成本（Compute Cost, Flink Cluster）

```yaml
# Resource Configuration
JobManager (2 replicas):
  Memory: 4GB x 2 = 8GB
  CPU: 2 cores x 2 = 4 cores
  Cost: c5.xlarge x 2 = $80/mo

TaskManager (4 replicas):
  Memory: 8GB x 4 = 32GB
  CPU: 4 cores x 4 = 16 cores
  Cost: c5.2xlarge x 4 = $472/mo

Total: $552/mo ($6,624/yr)
```

### 3.1 HPA 自動擴縮（HPA Auto-Scaling）

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: flink-taskmanager-hpa
spec:
  minReplicas: 4
  maxReplicas: 12
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 300
    scaleDown:
      stabilizationWindowSeconds: 600
```

---

## 4. 基礎設施優化（Infrastructure Optimization, -33%）

### 4.1 RDS 實例降級（RDS Instance Downgrade）

| 項目 | 優化前 | 優化後 | 節省 |
|------|--------|--------|------|
| 規格 | db.r5.4xlarge | db.r5.2xlarge | -50% |
| CPU | 16 vCPU | 8 vCPU | - |
| RAM | 128 GB | 64 GB | - |
| 成本 | $1,200/月 | $600/月 | $600/月 |

**優化原理**: Flink SQL 分流 OLAP 查詢，PostgreSQL 僅處理 OLTP

### 4.2 Redis 實例降級（Redis Instance Downgrade）

| 項目 | 優化前 | 優化後 | 節省 |
|------|--------|--------|------|
| 規格 | r5.2xlarge x 6 | r5.xlarge x 6 | -50% |
| 成本 | $580.8/月 | $290.4/月 | $290.4/月 |

**優化原理**: Caffeine L1 緩存命中率 95%，Redis QPS 降低 90%

---

## 5. 監控成本優化（Monitoring Cost Optimization, -100%）

### 5.1 遷移至開源方案（Migration to Open-Source Stack）

```mermaid
flowchart TD
    A[Application] -->|Metrics| B[Prometheus]
    A -->|Logs| C[Loki]
    A -->|Traces| D[Jaeger]
    B --> E[Grafana<br/>Dashboard]
    C --> E
    D --> E
    B --> F[Alertmanager]
    F -->|Slack/Email| G[On-Call Team]
    B -->|Long-term<br/>Storage| H[S3]
    C -->|Long-term<br/>Storage| H
```

**技術棧**:

| 組件 | 版本 | 用途 |
|------|------|------|
| Prometheus | 2.48 | 指標監控 |
| Grafana | 10.2 | 可視化 |
| Loki | 2.9 | 日誌管理 |
| Jaeger | 1.52 | 鏈路追蹤 |
| Alertmanager | 0.26 | 告警管理 |

### 5.2 Prometheus 配置（Prometheus Configuration）

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'flink'
    static_configs:
      - targets: ['flink-jobmanager:9250', 'flink-taskmanager:9250']
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

remote_write:
  - url: https://s3.amazonaws.com/prometheus-storage
```

---

## 6. 成本監控與告警（Cost Monitoring & Alerting）

### 6.1 AWS Budgets

```yaml
Budget:
  Name: igaming-monthly-budget
  BudgetType: COST
  TimeUnit: MONTHLY
  BudgetLimit:
    Amount: 2000
    Unit: USD
  NotificationsWithSubscribers:
    - Notification:
        NotificationType: ACTUAL
        ComparisonOperator: GREATER_THAN
        Threshold: 90
      Subscribers:
        - SubscriptionType: EMAIL
          Address: devops@company.com
```

### 6.2 Kubecost

```yaml
kubecostToken: "your-token"
prometheus:
  enabled: true
grafana:
  enabled: true
  ingress:
    hosts:
      - kubecost.company.com
```

---

## 7. 實施路線圖（Implementation Roadmap）

| Phase | 時程 | 內容 | 預期節省 |
|-------|------|------|---------|
| **Phase 1** | Week 1-2 | Kafka 保留策略 + S3 Lifecycle + Redis 降級 | $1,126/月 |
| **Phase 2** | Week 3-6 | Flink Cluster + JetCache + RDS 降級 | -$48/月 |
| **Phase 3** | Week 7-8 | Prometheus + Grafana + Datadog 下線 | $245/月 |

---

## 8. SmartAdmin 實作範例（SmartAdmin Implementation）

### 8.1 Cost Monitoring Service

```java
@Service
@RequiredArgsConstructor
public class CostMonitoringService {

    private final CostAllocationDao costAllocationDao;
    private final CostAlertManager alertManager;

    /**
     * Query cost allocation by tenant using Vavr Option.
     */
    public Option<CostAllocationVO> getCostByTenant(Long tenantId, LocalDate month) {
        return Option.of(costAllocationDao.selectByTenantAndMonth(tenantId, month))
            .map(entity -> SmartBeanUtil.copy(entity, CostAllocationVO.class));
    }
}
```

### 8.2 Cost Alert Manager

```java
@Component
@RequiredArgsConstructor
public class CostAlertManager {

    private final CostAllocationDao costAllocationDao;
    private final CostBudgetDao budgetDao;

    /**
     * Calculate costs and trigger alerts if over budget.
     * @Transactional only allowed in Manager layer per SmartAdmin architecture.
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> checkBudgetAlerts(LocalDate month) {
        List<CostAllocationEntity> allocations = costAllocationDao.selectByMonth(month);
        for (CostAllocationEntity allocation : allocations) {
            CostBudgetEntity budget = budgetDao.selectByTenantId(allocation.getTenantId());
            if (budget != null && allocation.getTotalCost()
                    .compareTo(budget.getMonthlyBudget().multiply(new BigDecimal("0.9"))) > 0) {
                // Trigger alert
            }
        }
        return ResponseDTO.ok();
    }
}
```

### 8.3 資料庫結構（Database Schema）

```sql
-- Cost allocation tracking table
CREATE TABLE t_cost_allocation (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    month           DATE NOT NULL,
    compute_cost    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    storage_cost    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    network_cost    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_cost      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cost_tenant_month UNIQUE (tenant_id, month)
);

CREATE INDEX idx_cost_tenant ON t_cost_allocation(tenant_id, month DESC);
```

---

## 相關文檔（Related Documents）

- [Performance Optimization](./13_Performance_Optimization.md) - 性能優化
- [Stream Processing Architecture](./15_Stream_Processing_Architecture.md) - Flink 流處理
- [Caching Strategy](./14_Caching_Strategy.md) - 緩存策略
- [Performance Monitoring](./12_Performance_Monitoring.md) - 監控告警
