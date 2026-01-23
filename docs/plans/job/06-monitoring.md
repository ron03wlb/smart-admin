# Snail-Job 监控运维指南

> 生产环境监控、告警配置与运维最佳实践

## 📋 目录

- [监控指标](#监控指标)
- [告警配置](#告警配置)
- [性能分析](#性能分析)
- [日志管理](#日志管理)
- [运维操作](#运维操作)

---

## 监控指标

### 核心指标

| 指标分类 | 指标名称 | 说明 | 告警阈值 |
|---------|---------|------|----------|
| **任务执行** | 执行成功率 | 成功次数 / 总次数 | < 95% |
| | 平均执行时间 | 任务平均耗时 | > 5 秒 |
| | 执行队列长度 | 待执行任务数 | > 1000 |
| **重试** | 重试率 | 重试次数 / 总次数 | > 10% |
| | 死信队列数量 | 重试失败任务数 | > 100 |
| **系统** | CPU 使用率 | 服务器 CPU 占用 | > 80% |
| | 内存使用率 | JVM 堆内存占用 | > 85% |
| | 数据库连接数 | 活跃连接数 | > 80% 最大值 |
| **网络** | RPC 响应时间 | 客户端到服务器延迟 | > 500ms |
| | 心跳失败率 | 心跳超时次数 / 总次数 | > 5% |

### Web 控制台监控

#### 实时监控面板

**访问**: http://localhost:8082 → **系统监控** → **实时监控**

**监控内容**:
- 📊 任务执行统计（成功/失败/运行中）
- 📈 执行趋势图（24 小时）
- 🖥️ 服务器资源使用率
- 👥 在线执行器列表
- ⏱️ 平均执行时间
- 🔄 重试统计

#### 任务执行日志

**访问**: **任务管理** → **定时任务** → **执行日志**

**查看内容**:
- 执行时间
- 执行状态（成功/失败）
- 执行耗时
- 返回结果
- 异常堆栈（如果失败）
- 执行节点 IP

---

## 告警配置

### 告警通道配置

#### 1. 邮件告警

**控制台配置**:
1. **系统管理** → **告警配置** → **邮件配置**
2. 填写 SMTP 配置

```yaml
# SMTP 配置示例
smtp:
  host: smtp.example.com
  port: 465
  username: alert@example.com
  password: ${SMTP_PASSWORD}
  from: Snail-Job Alert <alert@example.com>
  ssl: true
```

**告警模板**:
```
主题: [Snail-Job 告警] {taskName} 执行失败

内容:
- 任务名称: {taskName}
- 执行时间: {executeTime}
- 失败原因: {errorMessage}
- 执行节点: {nodeIp}
- 重试次数: {retryCount}

请及时处理！

查看详情: http://snail-job.example.com/log/{logId}
```

#### 2. 钉钉告警

**Webhook 配置**:
```yaml
dingtalk:
  webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
  secret: YOUR_SECRET
```

**告警消息格式**:
```json
{
  "msgtype": "markdown",
  "markdown": {
    "title": "任务执行失败告警",
    "text": "### [告警] 任务执行失败\n\n" +
            "- **任务名称**: 数据清理任务\n" +
            "- **执行时间**: 2026-01-22 02:00:00\n" +
            "- **失败原因**: 数据库连接超时\n" +
            "- **执行节点**: 10.0.1.100\n\n" +
            "[查看详情](http://snail-job.example.com/log/12345)"
  },
  "at": {
    "atMobiles": ["13800138000"],
    "isAtAll": false
  }
}
```

#### 3. 企业微信告警

**配置示例**:
```yaml
wechat:
  corpid: ww1234567890abcdef
  corpsecret: YOUR_SECRET
  agentid: 1000001
```

### 告警规则配置

#### 任务执行失败告警

**触发条件**:
- 单次执行失败
- 连续失败 N 次
- 失败率超过阈值

**配置示例**:
```yaml
alerts:
  - name: 任务执行失败
    type: TASK_FAILED
    conditions:
      - consecutive_failures >= 3  # 连续失败 3 次
    channels:
      - email
      - dingtalk
    recipients:
      - admin@example.com
      - ops-team@example.com
```

#### 任务执行超时告警

**触发条件**:
- 执行时间超过预期 2 倍
- 执行时间超过绝对阈值

**配置示例**:
```yaml
alerts:
  - name: 任务执行超时
    type: TASK_TIMEOUT
    conditions:
      - execute_time > expected_time * 2
      - execute_time > 300000  # 5 分钟
    channels:
      - dingtalk
```

#### 重试率告警

**触发条件**:
- 最近 1 小时重试率 > 10%
- 死信队列数量 > 100

**配置示例**:
```yaml
alerts:
  - name: 重试率异常
    type: RETRY_RATE_HIGH
    conditions:
      - retry_rate_1h > 0.1  # 10%
      - dead_letter_count > 100
    channels:
      - email
      - wechat
```

#### 系统资源告警

**触发条件**:
- CPU 使用率 > 80%
- 内存使用率 > 85%
- 数据库连接数 > 80

**配置示例**:
```yaml
alerts:
  - name: 系统资源告警
    type: SYSTEM_RESOURCE
    conditions:
      - cpu_usage > 0.8
      - memory_usage > 0.85
      - db_connections > 80
    channels:
      - email
      - dingtalk
```

---

## 性能分析

### 任务执行分析

#### 慢任务分析

**查询慢任务**:
```sql
-- 查找执行时间超过 5 秒的任务
SELECT
  job_name,
  AVG(execute_time_millis) AS avg_time,
  MAX(execute_time_millis) AS max_time,
  COUNT(*) AS exec_count
FROM sj_job_log
WHERE
  execute_time_millis > 5000
  AND created_at > NOW() - INTERVAL '7 days'
GROUP BY job_name
ORDER BY avg_time DESC
LIMIT 20;
```

**优化建议**:
1. 分析慢查询
2. 添加数据库索引
3. 使用分页/批处理
4. 考虑异步执行

#### 任务执行趋势

**查询执行趋势**:
```sql
-- 任务执行趋势（按小时）
SELECT
  DATE_TRUNC('hour', created_at) AS hour,
  COUNT(*) AS total_count,
  SUM(CASE WHEN success_flag = 1 THEN 1 ELSE 0 END) AS success_count,
  AVG(execute_time_millis) AS avg_time
FROM sj_job_log
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY hour
ORDER BY hour;
```

### 系统性能分析

#### JVM 监控

**使用 JVisualVM**:
```bash
# 启动 JVisualVM
jvisualvm

# 连接到 Snail-Job Server
# 主机: localhost
# 端口: 9010 (JMX 端口)
```

**关键指标**:
- 堆内存使用率
- GC 频率和耗时
- 线程数量
- CPU 使用率

**优化 JVM 参数**:
```yaml
snail-job-server:
  environment:
    JAVA_OPTS: >
      -Xms2g
      -Xmx4g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/opt/snail-job/dumps
```

#### 数据库性能分析

**慢查询日志**:
```sql
-- 开启慢查询日志
ALTER DATABASE smart_admin
SET log_min_duration_statement = 1000;  -- 记录超过 1 秒的查询

-- 查看慢查询
SELECT
  query,
  mean_exec_time,
  calls,
  total_exec_time
FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 20;
```

**优化建议**:
1. 添加索引
2. 优化查询语句
3. 使用连接池
4. 定期清理历史数据

#### Redis 性能监控

**监控命令**:
```bash
# 连接 Redis
docker exec -it sa21-redis redis-cli

# 查看信息
INFO stats
INFO memory

# 查看慢日志
SLOWLOG GET 10

# 查看热点 Key
redis-cli --bigkeys
```

---

## 日志管理

### 日志级别配置

**应用日志配置**:
```yaml
logging:
  level:
    root: INFO
    net.lab1024.sa: DEBUG
    com.aizuda.snailjob: INFO

  # 生产环境推荐配置
  file:
    name: logs/smart-admin.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 10GB
```

### 日志分析

#### 使用 grep 分析

```bash
# 查找错误日志
grep "ERROR" logs/smart-admin.log | tail -50

# 查找特定任务的日志
grep "dataCleanupJob" logs/smart-admin.log

# 统计错误类型
grep "ERROR" logs/smart-admin.log | awk -F'|' '{print $3}' | sort | uniq -c | sort -rn

# 查找执行时间超过 5 秒的任务
grep "elapsed=" logs/smart-admin.log | awk '$NF ~ /elapsed=[5-9][0-9]{3}|elapsed=[1-9][0-9]{4}/'
```

#### 日志聚合（推荐）

**使用 ELK Stack**:

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /opt/smart-admin/logs/*.log
    fields:
      service: smart-admin
      environment: production

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "smart-admin-%{+yyyy.MM.dd}"
```

**Kibana 查询示例**:
```
service: smart-admin AND level: ERROR
taskName: "dataCleanupJob" AND status: "failed"
executeTime > 5000
```

### 日志清理策略

#### 自动清理

**Logback 配置**:
```xml
<configuration>
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/smart-admin.log</file>

    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <!-- 每天归档 -->
      <fileNamePattern>logs/smart-admin.%d{yyyy-MM-dd}.log.gz</fileNamePattern>

      <!-- 保留 30 天 -->
      <maxHistory>30</maxHistory>

      <!-- 总大小限制 10GB -->
      <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>

    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
</configuration>
```

#### 数据库日志清理

**定期清理执行日志**:
```sql
-- 创建清理任务（在 Snail-Job 中配置）
-- 删除 90 天前的执行日志
DELETE FROM sj_job_log
WHERE created_at < NOW() - INTERVAL '90 days'
LIMIT 10000;  -- 分批删除

-- 删除重试成功的记录（保留 30 天）
DELETE FROM sj_retry_task
WHERE
  retry_status = 'SUCCESS'
  AND created_at < NOW() - INTERVAL '30 days'
LIMIT 10000;
```

---

## 运维操作

### 日常运维

#### 健康检查

```bash
#!/bin/bash
# health-check.sh

# 检查服务器状态
echo "=== Snail-Job Server Status ==="
docker compose ps snail-job-server

# 检查健康端点
echo -e "\n=== Health Endpoint ==="
curl -s http://localhost:8082/actuator/health | jq '.'

# 检查数据库连接
echo -e "\n=== Database Connection ==="
docker exec sa21-postgres pg_isready -U smartadmin

# 检查 Redis 连接
echo -e "\n=== Redis Connection ==="
docker exec sa21-redis redis-cli ping

# 检查在线执行器数量
echo -e "\n=== Active Executors ==="
# 通过 API 查询（需要配置认证）
# curl -s http://localhost:8082/api/executor/count
```

#### 服务重启

```bash
# 优雅重启（等待任务执行完成）
docker compose restart snail-job-server

# 查看重启日志
docker logs -f sa21-snail-job-server
```

#### 数据备份

```bash
#!/bin/bash
# backup-snail-job.sh

BACKUP_DIR="/backup/snail-job"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份数据库
docker exec sa21-postgres pg_dump -U smartadmin smart_admin \
  | gzip > "${BACKUP_DIR}/snail_job_db_${DATE}.sql.gz"

# 备份配置文件
tar -czf "${BACKUP_DIR}/snail_job_config_${DATE}.tar.gz" \
  docker/snail-job/ \
  sa-admin/src/main/resources/*/sa-base.yaml

# 清理 7 天前的备份
find "${BACKUP_DIR}" -name "*.gz" -mtime +7 -delete

echo "Backup completed: ${DATE}"
```

### 故障恢复

#### 服务器宕机恢复

```bash
# 1. 检查服务状态
docker compose ps

# 2. 查看错误日志
docker logs sa21-snail-job-server | tail -100

# 3. 重启服务
docker compose up -d snail-job-server

# 4. 验证恢复
curl http://localhost:8082/actuator/health
```

#### 数据恢复

```bash
# 从备份恢复数据库
gunzip < snail_job_db_20260122.sql.gz | \
  docker exec -i sa21-postgres psql -U smartadmin smart_admin
```

### 升级操作

#### 滚动升级

```bash
# 1. 备份数据
./backup-snail-job.sh

# 2. 下载新版本镜像
docker pull aizuda/snail-job-server:1.9.0

# 3. 更新 docker-compose.yml
# image: aizuda/snail-job-server:1.9.0

# 4. 滚动重启
docker compose up -d --no-deps snail-job-server

# 5. 验证升级
docker logs sa21-snail-job-server | grep "Started"
curl http://localhost:8082/actuator/info
```

---

## 监控最佳实践

### 1. 建立监控基线

**收集 7 天的正常数据**:
- 任务执行成功率
- 平均执行时间
- 系统资源使用率
- 重试率

**基于基线设置告警阈值**:
```
告警阈值 = 基线均值 + 2 * 标准差
```

### 2. 分级告警

| 级别 | 严重程度 | 响应时间 | 通道 |
|------|---------|---------|------|
| P0 | 严重 | 立即 | 电话 + 短信 + 邮件 |
| P1 | 高 | 15 分钟 | 短信 + 邮件 |
| P2 | 中 | 1 小时 | 邮件 + 钉钉 |
| P3 | 低 | 工作日处理 | 邮件 |

### 3. 监控大盘

**关键指标展示**:
- 实时任务执行状态
- 24 小时执行趋势
- Top 10 慢任务
- 系统资源使用率
- 告警数量统计

**推荐工具**: Grafana

---

## 下一步

- [故障排查手册](./04-troubleshooting.md) - 问题诊断
- [架构设计说明](./05-architecture.md) - 深入了解原理
- [配置参考](./02-configuration.md) - 详细配置

---

**监控运维完成！** 建立完善的监控体系是保障系统稳定运行的关键。
