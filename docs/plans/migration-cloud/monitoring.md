# SmartAdmin 微服務監控運維指南

**文檔類型**: 監控運維參考手冊
**目標讀者**: DevOps 工程師、運維團隊、SRE
**文檔版本**: 1.0.0
**創建日期**: 2026-02-03

---

## 📋 文檔目的

本文檔提供 **可直接使用的監控配置和告警規則**：
1. ✅ Prometheus 指標採集配置
2. ✅ Grafana 儀表盤 JSON
3. ✅ 告警規則示例（P0/P1/P2）
4. ✅ 容量管理與性能調優

---

## 📊 關鍵指標定義

### 1. RED 指標（推薦）⭐

**適用於**: 所有微服務

| 指標 | 定義 | 閾值 | 告警級別 |
|------|------|------|---------|
| **Rate（請求速率）** | Requests per second (RPS) | - | - |
| **Errors（錯誤率）** | Error rate (%) | > 5% | P1 |
| **Duration（持續時間）** | P50, P95, P99 latency | P99 > 500ms | P2 |

**Prometheus 查詢**:

```promql
# Rate - 每秒請求數
rate(http_server_requests_seconds_count[5m])

# Errors - 錯誤率 (%)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m])) * 100

# Duration - P99 延遲
histogram_quantile(0.99,
  rate(http_server_requests_seconds_bucket[5m])
)
```

---

### 2. USE 指標（基礎設施）

**適用於**: 物理機、虛擬機、容器

| 指標 | 定義 | 閾值 | 告警級別 |
|------|------|------|---------|
| **Utilization（利用率）** | CPU, Memory, Disk | CPU > 80% | P2 |
| **Saturation（飽和度）** | Queue length, Thread pool | Thread pool > 90% | P1 |
| **Errors（錯誤）** | Network errors, Disk errors | Disk error > 0 | P0 |

**Prometheus 查詢**:

```promql
# CPU 利用率 (%)
100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 記憶體利用率 (%)
100 - ((node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100)

# 磁盤利用率 (%)
100 - ((node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100)
```

---

## 🔧 Prometheus 指標採集

### 1. Nacos Server 指標

**prometheus.yml 配置**:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'nacos'
    static_configs:
      - targets: ['localhost:8848']
    metrics_path: '/nacos/actuator/prometheus'
    scrape_interval: 15s
    scrape_timeout: 10s
```

**關鍵指標**:

```promql
# Nacos 服務註冊數量
nacos_service_count

# Nacos 配置推送成功率
rate(nacos_config_push_success_total[5m])
/
rate(nacos_config_push_total[5m]) * 100
```

---

### 2. API Gateway 指標

**prometheus.yml 配置**:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'gateway'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

**關鍵指標**:

```promql
# Gateway 請求速率
rate(spring_cloud_gateway_requests_seconds_count[5m])

# Gateway 錯誤率
sum(rate(spring_cloud_gateway_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))
/
sum(rate(spring_cloud_gateway_requests_seconds_count[5m])) * 100

# Gateway P99 延遲
histogram_quantile(0.99,
  rate(spring_cloud_gateway_requests_seconds_bucket[5m])
)
```

---

### 3. 微服務指標

**prometheus.yml 配置**:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'microservices'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
```

**關鍵指標**:

```promql
# 服務請求速率
rate(http_server_requests_seconds_count{job="microservices"}[5m])

# 服務錯誤率
sum by (application) (
  rate(http_server_requests_seconds_count{status=~"5..",job="microservices"}[5m])
)
/
sum by (application) (
  rate(http_server_requests_seconds_count{job="microservices"}[5m])
) * 100

# 服務 P99 延遲
histogram_quantile(0.99,
  sum by (application, le) (
    rate(http_server_requests_seconds_bucket{job="microservices"}[5m])
  )
)
```

---

## 📈 Grafana 儀表盤

### Dashboard 1: 服務健康概覽

**用途**: 實時監控所有服務的健康狀態

**關鍵面板**:
1. 服務註冊數量（Nacos）
2. API 請求速率（Gateway）
3. 錯誤率趨勢（所有服務）
4. P99 延遲（所有服務）

**JSON 配置**（部分）:

```json
{
  "dashboard": {
    "title": "SmartAdmin 微服務健康概覽",
    "panels": [
      {
        "id": 1,
        "title": "服務註冊數量",
        "targets": [
          {
            "expr": "nacos_service_count"
          }
        ],
        "type": "stat"
      },
      {
        "id": 2,
        "title": "API 請求速率 (RPS)",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[5m]))"
          }
        ],
        "type": "graph"
      },
      {
        "id": 3,
        "title": "錯誤率 (%)",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100"
          }
        ],
        "type": "graph",
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [5],
                "type": "gt"
              }
            }
          ]
        }
      }
    ]
  }
}
```

**導入方式**:
1. 登入 Grafana: http://localhost:3000
2. 點擊 "+" → "Import"
3. 上傳 JSON 文件或貼上 JSON 內容

---

### Dashboard 2: 微服務詳細監控

**用途**: 針對單個微服務的深度監控

**關鍵面板**:
1. 請求速率（按 API 路徑分組）
2. 錯誤率（按狀態碼分組）
3. P50/P95/P99 延遲
4. JVM 記憶體使用
5. 線程池狀態
6. 數據庫連接池狀態

**變量配置**:

```json
{
  "templating": {
    "list": [
      {
        "name": "service",
        "type": "query",
        "query": "label_values(http_server_requests_seconds_count, application)",
        "refresh": 1
      }
    ]
  }
}
```

---

## 🚨 告警規則

### 1. P0 嚴重告警（Critical）

**特點**: 立即響應，影響業務正常運行

#### 1.1 服務不可用

```yaml
# prometheus/rules/p0-critical.yml
groups:
  - name: p0-critical
    rules:
      - alert: ServiceDown
        expr: up{job="microservices"} == 0
        for: 1m
        labels:
          severity: critical
          priority: P0
        annotations:
          summary: "服務 {{ $labels.instance }} 不可用"
          description: "服務已下線超過 1 分鐘，請立即檢查！"
```

#### 1.2 錯誤率過高

```yaml
      - alert: HighErrorRate
        expr: |
          sum by (application) (
            rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          )
          /
          sum by (application) (
            rate(http_server_requests_seconds_count[5m])
          ) * 100 > 10
        for: 5m
        labels:
          severity: critical
          priority: P0
        annotations:
          summary: "服務 {{ $labels.application }} 錯誤率過高"
          description: "錯誤率為 {{ $value }}%，超過閾值 10%"
```

#### 1.3 Nacos 不可用

```yaml
      - alert: NacosDown
        expr: up{job="nacos"} == 0
        for: 1m
        labels:
          severity: critical
          priority: P0
        annotations:
          summary: "Nacos Server 不可用"
          description: "Nacos 已下線超過 1 分鐘，所有服務註冊發現將失敗！"
```

---

### 2. P1 高優先級告警（High）

**特點**: 2 小時內響應，可能影響部分功能

#### 2.1 P99 延遲過高

```yaml
# prometheus/rules/p1-high.yml
groups:
  - name: p1-high
    rules:
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99,
            sum by (application, le) (
              rate(http_server_requests_seconds_bucket[5m])
            )
          ) > 0.5
        for: 10m
        labels:
          severity: high
          priority: P1
        annotations:
          summary: "服務 {{ $labels.application }} 延遲過高"
          description: "P99 延遲為 {{ $value }}s，超過閾值 500ms"
```

#### 2.2 線程池飽和

```yaml
      - alert: ThreadPoolSaturation
        expr: |
          (tomcat_threads_current_threads / tomcat_threads_config_max_threads) > 0.9
        for: 5m
        labels:
          severity: high
          priority: P1
        annotations:
          summary: "服務 {{ $labels.application }} 線程池飽和"
          description: "線程池使用率為 {{ $value }}%，接近上限"
```

#### 2.3 數據庫連接池飽和

```yaml
      - alert: DBConnectionPoolSaturation
        expr: |
          (hikaricp_connections_active / hikaricp_connections_max) > 0.9
        for: 5m
        labels:
          severity: high
          priority: P1
        annotations:
          summary: "服務 {{ $labels.application }} 數據庫連接池飽和"
          description: "連接池使用率為 {{ $value }}%，接近上限"
```

---

### 3. P2 中等優先級告警（Medium）

**特點**: 24 小時內響應，不影響核心功能

#### 3.1 CPU 使用率過高

```yaml
# prometheus/rules/p2-medium.yml
groups:
  - name: p2-medium
    rules:
      - alert: HighCPUUsage
        expr: |
          100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 15m
        labels:
          severity: medium
          priority: P2
        annotations:
          summary: "主機 {{ $labels.instance }} CPU 使用率過高"
          description: "CPU 使用率為 {{ $value }}%，超過閾值 80%"
```

#### 3.2 記憶體使用率過高

```yaml
      - alert: HighMemoryUsage
        expr: |
          100 - ((node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100) > 85
        for: 15m
        labels:
          severity: medium
          priority: P2
        annotations:
          summary: "主機 {{ $labels.instance }} 記憶體使用率過高"
          description: "記憶體使用率為 {{ $value }}%，超過閾值 85%"
```

#### 3.3 磁盤使用率過高

```yaml
      - alert: HighDiskUsage
        expr: |
          100 - ((node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100) > 85
        for: 15m
        labels:
          severity: medium
          priority: P2
        annotations:
          summary: "主機 {{ $labels.instance }} 磁盤使用率過高"
          description: "磁盤使用率為 {{ $value }}%，超過閾值 85%"
```

---

## 📧 告警通知配置

### 1. Alertmanager 配置

**alertmanager.yml**:

```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@example.com'
  smtp_auth_username: 'alert@example.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical-team'
    - match:
        severity: high
      receiver: 'high-team'
    - match:
        severity: medium
      receiver: 'medium-team'

receivers:
  - name: 'default'
    email_configs:
      - to: 'team@example.com'

  - name: 'critical-team'
    email_configs:
      - to: 'oncall@example.com'
    webhook_configs:
      - url: 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN'

  - name: 'high-team'
    email_configs:
      - to: 'devops@example.com'

  - name: 'medium-team'
    email_configs:
      - to: 'dev@example.com'
```

---

### 2. 釘釘告警配置

**DingTalk Webhook**:

```yaml
webhook_configs:
  - url: 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN'
    send_resolved: true
```

**訊息格式**:

```json
{
  "msgtype": "markdown",
  "markdown": {
    "title": "SmartAdmin 微服務告警",
    "text": "## 🚨 服務告警\n\n- **告警級別**: P0 Critical\n- **服務名稱**: job-service\n- **告警內容**: 服務不可用\n- **觸發時間**: 2026-02-03 10:00:00\n\n請立即處理！"
  }
}
```

---

## 📊 容量管理

### 1. 資源使用趨勢分析

**Grafana 查詢**:

```promql
# CPU 使用趨勢（過去 7 天）
avg_over_time(
  (100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100))[7d:1h]
)

# 記憶體使用趨勢（過去 7 天）
avg_over_time(
  (100 - ((node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100))[7d:1h]
)
```

---

### 2. 擴容建議

| 指標 | 閾值 | 擴容建議 |
|------|------|---------|
| CPU 使用率 | > 70% 持續 24 小時 | 增加 2 個 Pod |
| 記憶體使用率 | > 80% 持續 24 小時 | 增加記憶體 50% |
| 請求速率 | > 1000 RPS 持續 1 小時 | 增加 2-4 個 Pod |
| P99 延遲 | > 500ms 持續 1 小時 | 檢查瓶頸，優化或擴容 |

**Kubernetes 自動擴容配置**:

```yaml
# hpa.yml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: job-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: job-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## 🔧 性能調優

### 1. JVM 調優

**推薦 JVM 參數**:

```bash
# application.yml 或啟動腳本
JAVA_OPTS="
  -Xms2g -Xmx2g           # 堆大小
  -XX:+UseG1GC            # G1 垃圾回收器
  -XX:MaxGCPauseMillis=200 # GC 暫停時間目標
  -XX:+HeapDumpOnOutOfMemoryError  # OOM 時生成堆轉儲
  -XX:HeapDumpPath=/dumps  # 堆轉儲路徑
  -XX:+PrintGCDetails      # 打印 GC 詳情
  -XX:+PrintGCDateStamps   # 打印 GC 時間戳
"
```

---

### 2. 數據庫連接池調優

**HikariCP 配置**:

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大連接數
      minimum-idle: 5            # 最小空閒連接數
      connection-timeout: 30000  # 連接超時 (30s)
      idle-timeout: 600000       # 空閒超時 (10min)
      max-lifetime: 1800000      # 最大生命週期 (30min)
      leak-detection-threshold: 60000  # 洩漏檢測閾值 (60s)
```

**連接數計算公式**:

```
connections = (core_count × 2) + effective_spindle_count
```

示例：
- 4 核 CPU + 1 個 SSD（忽略） = 4 × 2 + 0 = **8 個連接**

---

### 3. Feign 連接池調優

**配置**:

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      httpclient:
        enabled: true
        max-connections: 500           # 最大連接數
        max-connections-per-route: 100 # 每個路由最大連接數
        connection-timeout: 5000       # 連接超時 (5s)
        connection-timer-repeat: 3000  # 連接池清理間隔 (3s)
```

---

## 📝 日誌管理

### 1. ELK Stack 集成

**Logstash 配置**:

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:loglevel} \[%{DATA:service}\] %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "smartadmin-logs-%{+YYYY.MM.dd}"
  }
}
```

---

### 2. 日誌查詢示例

**Kibana 查詢**:

```
# 查詢錯誤日誌
loglevel:ERROR

# 查詢特定服務日誌
service:job-service AND loglevel:ERROR

# 查詢特定時間範圍
@timestamp:[2026-02-03T00:00:00 TO 2026-02-03T23:59:59] AND loglevel:ERROR
```

---

## 📚 參考資源

- **Prometheus 官方文檔**: https://prometheus.io/docs/
- **Grafana 官方文檔**: https://grafana.com/docs/
- **Micrometer 官方文檔**: https://micrometer.io/docs/
- **HikariCP 官方文檔**: https://github.com/brettwooldridge/HikariCP

---

## 📝 文檔維護

**文檔版本**: 1.0.0
**創建日期**: 2026-02-03
**最後更新**: 2026-02-03
**維護團隊**: SmartAdmin 架構組

**更新歷史**:
- v1.0.0 (2026-02-03): 初始版本，完整監控運維配置

---

**Happy Monitoring! 📊**

希望本指南能幫助您建立完善的監控體系！
