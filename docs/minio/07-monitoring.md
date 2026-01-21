# MinIO 监控运维指南

> 生产环境 MinIO 监控、告警和日常运维最佳实践

## 📊 监控体系概览

```
┌────────────────────────────────┐
│      监控数据采集层             │
│  ├─ MinIO Metrics (Prometheus) │
│  ├─ Docker Stats               │
│  └─ Audit Logs                 │
└────────────────────────────────┘
          ↓
┌────────────────────────────────┐
│      指标存储与处理             │
│  ├─ Prometheus                 │
│  └─ ELK Stack                  │
└────────────────────────────────┘
          ↓
┌────────────────────────────────┐
│      可视化与告警               │
│  ├─ Grafana                    │
│  └─ Alertmanager               │
└────────────────────────────────┘
```

## 🔍 关键指标监控

### MinIO 核心指标

**启用 Prometheus 指标**：

```yaml
minio:
  environment:
    MINIO_PROMETHEUS_AUTH_TYPE: public
```

**访问指标端点**：

```bash
curl http://localhost:9000/minio/v2/metrics/cluster
```

### 关键指标列表

| 指标名 | 说明 | 告警阈值 |
|--------|------|----------|
| `minio_cluster_nodes_online` | 在线节点数 | < 预期节点数 |
| `minio_cluster_capacity_usable_free_bytes` | 可用空间 | < 20% 总容量 |
| `minio_s3_requests_errors_total` | API 错误数 | > 1% 请求数 |
| `minio_s3_requests_ttfb_seconds_bucket` | 首字节时间 | P95 > 500ms |

## Prometheus 配置

```yaml
global:
  scrape_interval: 30s

scrape_configs:
  - job_name: 'minio-cluster'
    metrics_path: /minio/v2/metrics/cluster
    static_configs:
      - targets:
          - 'minio:9000'
```

## Grafana 仪表盘

### 导入官方仪表盘

```bash
# 仪表盘 ID: 13502 (MinIO Dashboard)
```

### 自定义面板

#### 存储容量趋势

```promql
(minio_cluster_capacity_usable_free_bytes / minio_cluster_capacity_usable_total_bytes) * 100
```

#### API 请求QPS

```promql
rate(minio_s3_requests_total[5m])
```

#### 错误率

```promql
rate(minio_s3_requests_4xx_errors_total[5m]) / rate(minio_s3_requests_total[5m]) * 100
```

## 告警规则

```yaml
groups:
  - name: minio_alerts
    rules:
      - alert: MinIODiskSpaceLow
        expr: (minio_cluster_capacity_usable_free_bytes / minio_cluster_capacity_usable_total_bytes) * 100 < 20
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MinIO 存储空间不足"
```

## 📝 日志监控

### 启用审计日志

```yaml
minio:
  environment:
    MINIO_AUDIT_LOGGER_ENABLE_console: "on"
```

## 🛠️ 日常运维

### 容量管理

```bash
# 查看存储使用情况
docker exec sa21-minio mc admin info local/

# 按存储桶统计
docker exec sa21-minio mc du --recursive local/dev-smart-admin
```

### 清理策略

```bash
# 临时文件自动清理
docker exec sa21-minio mc ilm add --expiry-days 1 local/dev-smart-admin/temp
```

### 备份与恢复

```bash
# 数据备份
mc mirror local/dev-smart-admin backup/dev-smart-admin

# 快照备份
docker run --rm -v sa21-minio-data:/data -v $(pwd)/backup:/backup \
  alpine tar czf /backup/minio-data-$(date +%Y%m%d).tar.gz /data
```

### 升级维护

```bash
# 单节点升级
docker compose down minio
# 更新镜像版本
docker compose up -d minio
```

## 🎯 SLA 指标

| 指标 | 目标 | 测量周期 |
|------|------|----------|
| 服务可用性 | 99.9% | 月度 |
| API 成功率 | 99.5% | 日度 |
| P95 延迟 | < 500ms | 小时 |
| P99 延迟 | < 1000ms | 小时 |

## 📖 相关文档

- [快速开始](./01-quick-start.md) - 环境搭建
- [故障排查](./05-troubleshooting.md) - 问题诊断
- [架构设计](./06-architecture.md) - 系统架构
- [安全最佳实践](./03-security.md) - 安全加固
