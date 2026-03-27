# 07-06 性能監控與告警 (Performance Monitoring & Alerting)

> **版本**: 4.0.0
> **最後更新**: 2026-01-28
> **維護團隊**: DevOps Team & SRE Team

---

## 📋 目錄

- [1. 系統概述](#1-系統概述-system-overview)
- [2. APM 工具選型](#2-apm-工具選型-apm-tool-selection)
- [3. 關鍵指標體系](#3-關鍵指標體系-key-metrics)
- [4. 告警規則設計](#4-告警規則設計-alerting-rules)
- [5. 日誌聚合系統](#5-日誌聚合系統-log-aggregation)
- [6. 監控架構集成](#6-監控架構集成-monitoring-architecture-integration)
- [7. 相關文檔](#7-相關文檔)

---

## 1. 系統概述 (System Overview)

性能監控與告警系統是保障 iGaming 平台穩定運行的關鍵基礎設施,通過**全鏈路監控、實時告警、根因分析**,確保系統 SLA 達標,降低故障影響範圍。

**核心功能**:
- **APM 監控**: 分佈式追蹤、性能分析、依賴拓撲
- **指標採集**: API 延遲、錯誤率、吞吐量、資源使用率
- **智能告警**: 分級告警、告警聚合、自動抑制、根因推薦
- **日誌聚合**: 集中式日誌存儲、全文檢索、日誌關聯分析
- **可視化**: Grafana 儀表板、業務大盤、SLO 儀表板

---

## 2. APM 工具選型 (APM Tool Selection)

### 2.1 工具對比

| APM 工具 | 類型 | 成本 | 易用性 | 功能完整度 | 推薦場景 |
|---------|------|------|--------|-----------|---------|
| **Apache Skywalking** | 開源 | 免費 | ⭐⭐⭐ | ⭐⭐⭐⭐ | 開源優先、Java 生態 ⭐ |
| **New Relic** | 商業 | $$$$ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 企業級、全功能 |
| **Datadog** | 商業 | $$$$ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 多雲環境、DevOps 整合 |
| **Elastic APM** | 開源/商業 | $$ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ELK Stack 整合 |
| **Pinpoint** | 開源 | 免費 | ⭐⭐ | ⭐⭐⭐⭐ | 韓國開源、功能強大 |

**推薦**: **Apache Skywalking** (開源、社區活躍、Java/Spring Boot 原生支持)

---

### 2.2 Skywalking 集成

**依賴配置 (pom.xml)**:
```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-trace</artifactId>
    <version>9.3.0</version>
</dependency>
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-logback-1.x</artifactId>
    <version>9.3.0</version>
</dependency>
```

**啟動參數 (application.yml)**:
```
# Skywalking Agent 配置
JAVA_OPTS: >
  -javaagent:/opt/skywalking-agent/skywalking-agent.jar
  -Dskywalking.agent.service_name=smart-admin-api
  -Dskywalking.collector.backend_service=skywalking-oap:11800
  -Dskywalking.plugin.jdbc.trace_sql_parameters=true
```


---

## 3. 關鍵指標體系 (Key Metrics)

### 3.1 Golden Signals (黃金指標)

**延遲 (Latency)**:
- **P50**: 50% 請求的響應時間 (目標: <200ms)
- **P95**: 95% 請求的響應時間 (目標: <500ms)
- **P99**: 99% 請求的響應時間 (目標: <1000ms)
- **P999**: 99.9% 請求的響應時間 (目標: <3000ms)

**流量 (Traffic)**:
- **QPS (Queries Per Second)**: 每秒請求數
- **Active Users**: 在線用戶數
- **Bet Transactions/sec**: 投注 TPS

**錯誤 (Errors)**:
- **Error Rate**: 錯誤率 (目標: <0.1%)
- **5xx Rate**: 服務端錯誤率 (目標: <0.01%)
- **Timeout Rate**: 超時率 (目標: <0.05%)

**飽和度 (Saturation)**:
- **CPU Usage**: CPU 使用率 (告警閾值: >80%)
- **Memory Usage**: 內存使用率 (告警閾值: >85%)
- **DB Connection Pool**: 連接池使用率 (告警閾值: >90%)
- **Redis Connection Pool**: Redis 連接池使用率 (告警閾值: >90%)

---

### 3.2 業務指標 (Business Metrics)


**Prometheus Metrics 導出**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

---

## 4. 告警規則設計 (Alerting Rules)

### 4.1 告警分級 (Alert Severity)

| 級別 | 定義 | SLA 響應時間 | 通知渠道 | 範例 |
|------|------|------------|---------|------|
| **P0 - Critical** | 核心業務中斷 | 5 分鐘 | PagerDuty (電話) | 存款 API 完全不可用 |
| **P1 - High** | 嚴重性能下降 | 15 分鐘 | Slack (緊急頻道) | API P99 > 3s |
| **P2 - Medium** | 部分功能異常 | 30 分鐘 | Slack (告警頻道) | 錯誤率 > 1% |
| **P3 - Low** | 性能告警 | 1 小時 | Email | CPU > 80% |

---

### 4.2 Prometheus AlertManager 規則

**告警規則配置 (alert-rules.yml)**:
```yaml
groups:
  - name: api_alerts
    interval: 30s
    rules:
      # P0: API 完全不可用
      - alert: APIDownCritical
        expr: up{job="smart-admin-api"} == 0
        for: 1m
        labels:
          severity: critical
          priority: P0
        annotations:
          summary: "API 服務完全不可用"
          description: "{{ $labels.instance }} 已經宕機超過 1 分鐘"
          runbook_url: "https://wiki.internal/runbooks/api-down"

      # P1: P99 延遲過高
      - alert: HighLatencyP99
        expr: histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 3
        for: 5m
        labels:
          severity: high
          priority: P1
        annotations:
          summary: "API P99 延遲超過 3 秒"
          description: "{{ $labels.endpoint }} P99 延遲: {{ $value }}s"

      # P2: 錯誤率超過 1%
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.01
        for: 5m
        labels:
          severity: medium
          priority: P2
        annotations:
          summary: "API 錯誤率超過 1%"
          description: "當前錯誤率: {{ $value | humanizePercentage }}"

      # P3: CPU 使用率過高
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.8
        for: 10m
        labels:
          severity: low
          priority: P3
        annotations:
          summary: "CPU 使用率超過 80%"
          description: "{{ $labels.instance }} CPU: {{ $value | humanizePercentage }}"
```

---

### 4.3 告警通知路由

**AlertManager 配置 (alertmanager.yml)**:
```yaml
route:
  receiver: 'default'
  group_by: ['alertname', 'priority']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    # P0: PagerDuty 電話告警
    - match:
        priority: P0
      receiver: 'pagerduty'
      continue: true

    # P1: Slack 緊急頻道
    - match:
        priority: P1
      receiver: 'slack-critical'
      continue: true

    # P2/P3: Slack 普通告警頻道
    - match_re:
        priority: P2|P3
      receiver: 'slack-warning'

receivers:
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '<PagerDuty_Service_Key>'
        severity: '{{ .GroupLabels.priority }}'

  - name: 'slack-critical'
    slack_configs:
      - api_url: '<Slack_Webhook_URL>'
        channel: '#alerts-critical'
        title: '🔴 {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'slack-warning'
    slack_configs:
      - api_url: '<Slack_Webhook_URL>'
        channel: '#alerts-warning'
        title: '⚠️ {{ .GroupLabels.alertname }}'
```

---

## 5. 日誌聚合系統 (Log Aggregation)

### 5.1 ELK Stack 架構

```
Application Logs (JSON)
    ↓
Filebeat (Log Shipper)
    ↓
Logstash (Log Processing)
    ↓ (Filter, Transform, Enrich)
Elasticsearch (Storage & Indexing)
    ↓
Kibana (Visualization & Search)
```

---

### 5.2 結構化日誌配置

**Logback 配置 (logback-spring.xml)**:
```xml
<configuration>
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/smart-admin.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>
                {"app_name":"smart-admin-api","environment":"${SPRING_PROFILES_ACTIVE}"}
            </customFields>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
                <levelValue>[ignore]</levelValue>
            </fieldNames>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/smart-admin.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_FILE"/>
    </root>
</configuration>
```

**日誌輸出範例 (JSON)**:
```json
{
  "@timestamp": "2026-01-28T10:15:30.123Z",
  "app_name": "smart-admin-api",
  "environment": "production",
  "level": "ERROR",
  "logger": "net.lab1024.sa.admin.module.business.deposit.service.DepositService",
  "thread": "http-nio-1024-exec-5",
  "message": "Deposit processing failed",
  "stack_trace": "...",
  "player_id": 12345,
  "transaction_id": "TXN-2026012810153012345",
  "amount": 100.00,
  "psp": "Stripe",
  "error_code": "PSP_TIMEOUT"
}
```

---

### 5.3 Elasticsearch 索引設計

**索引模板 (index-template.json)**:
```json
{
  "index_patterns": ["smart-admin-logs-*"],
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "index.lifecycle.name": "smart-admin-ilm-policy",
    "index.lifecycle.rollover_alias": "smart-admin-logs"
  },
  "mappings": {
    "properties": {
      "@timestamp": {"type": "date"},
      "level": {"type": "keyword"},
      "logger": {"type": "keyword"},
      "message": {"type": "text"},
      "player_id": {"type": "long"},
      "transaction_id": {"type": "keyword"},
      "amount": {"type": "double"},
      "psp": {"type": "keyword"},
      "error_code": {"type": "keyword"}
    }
  }
}
```

**ILM 策略 (Index Lifecycle Management)**:
```json
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "3d",
        "actions": {
          "forcemerge": {"max_num_segments": 1},
          "shrink": {"number_of_shards": 1}
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {"delete": {}}
      }
    }
  }
}
```

---

## 6. 監控架構集成 (Monitoring Architecture Integration)

### 6.1 全鏈路監控架構

```
┌────────────────────────────────────────────────────────────┐
│  Application Layer (Spring Boot)                           │
│  - Micrometer (Metrics)                                    │
│  - Skywalking Agent (Traces)                               │
│  - Logback (Logs)                                          │
└────────────────┬───────────────────────────────────────────┘
                 │
┌────────────────▼───────────────────────────────────────────┐
│  Metrics Collection Layer                                  │
│  - Prometheus (Scraping)                                   │
│  - Skywalking OAP (Trace Processing)                       │
│  - Filebeat → Logstash → Elasticsearch (Log Pipeline)     │
└────────────────┬───────────────────────────────────────────┘
                 │
┌────────────────▼───────────────────────────────────────────┐
│  Storage Layer                                             │
│  - Prometheus TSDB (Metrics)                               │
│  - Elasticsearch (Traces & Logs)                           │
└────────────────┬───────────────────────────────────────────┘
                 │
┌────────────────▼───────────────────────────────────────────┐
│  Visualization & Alerting Layer                            │
│  - Grafana (Unified Dashboard)                             │
│  - Kibana (Log Analysis)                                   │
│  - Skywalking UI (Trace Analysis)                          │
│  - AlertManager (Alert Routing)                            │
└─────────────────────────────────────────────────────────────┘
```

---

### 6.2 Grafana 統一儀表板

**API 性能儀表板 (JSON 配置)**:
```json
{
  "dashboard": {
    "title": "SmartAdmin API Performance",
    "panels": [
      {
        "title": "Request Rate (QPS)",
        "targets": [{"expr": "rate(http_requests_total[1m])"}]
      },
      {
        "title": "Latency (P50/P95/P99)",
        "targets": [
          {"expr": "histogram_quantile(0.50, rate(http_request_duration_seconds_bucket[5m]))", "legendFormat": "P50"},
          {"expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))", "legendFormat": "P95"},
          {"expr": "histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))", "legendFormat": "P99"}
        ]
      },
      {
        "title": "Error Rate",
        "targets": [{"expr": "rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m])"}]
      }
    ]
  }
}
```

---

## 📚 相關文檔

### 前置知識
- [00-04 技術選型標準](../00_Foundation/concepts/00-04_Technology_Stack.md) - APM 工具選型依據

### 核心依賴
- [12-01 部署架構](./09-01_Deployment.md) - 監控基礎設施部署
- [12-04 維護程序](./09-05_Maintenance.md) - 告警響應流程

### 延伸閱讀
- [09-02 審計日誌系統](../06_Platform_Governance/06-03_Audit_Log.md) - 日誌聚合與審計日誌整合
- [05-01 風控系統](../05_Risk_Control/05-01_Risk_Framework.md) - 風控指標監控

---

**文檔版本**: 4.0.0
**最後更新**: 2026-01-28
**維護團隊**: DevOps Team & SRE Team
