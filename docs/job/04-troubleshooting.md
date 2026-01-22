# Snail-Job 故障排查手册

> 常见问题诊断与解决方案

## 📋 快速诊断清单

遇到问题时，请按以下顺序检查：

✅ **服务健康检查**
```bash
# 1. 检查 Snail-Job 服务器状态
docker compose ps snail-job-server

# 2. 查看服务器日志
docker logs -f sa21-snail-job-server

# 3. 健康检查端点
curl http://localhost:8082/actuator/health
```

✅ **网络连通性**
```bash
# 4. 测试 RPC 端口
telnet localhost 1788

# 5. 测试 Web 控制台
curl http://localhost:8082
```

✅ **应用配置检查**
```bash
# 6. 检查应用日志
tail -f smart-admin-api-java21-springboot3/logs/smart-admin.log | grep "snail-job"

# 7. 验证配置加载
grep -A 10 "snail-job:" sa-admin/src/main/resources/dev/sa-base.yaml
```

---

## 1. 服务器启动失败

### 现象

```bash
docker compose up -d snail-job-server
# 服务持续重启或状态为 "Restarting"
```

### 排查步骤

#### 1.1 查看启动日志

```bash
docker logs sa21-snail-job-server
```

#### 1.2 常见错误及解决方案

**错误 A: 数据库连接失败**

```
Caused by: org.postgresql.util.PSQLException: Connection refused
```

**原因**: PostgreSQL 服务未启动或连接配置错误

**解决方案**:
```bash
# 检查 PostgreSQL 服务状态
docker compose ps postgres

# 启动 PostgreSQL
docker compose up -d postgres

# 检查连接配置（docker-compose.yml）
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/smart_admin
```

---

**错误 B: Redis 连接失败**

```
Unable to connect to Redis: Connection refused
```

**原因**: Redis 服务未启动

**解决方案**:
```bash
# 启动 Redis
docker compose up -d redis

# 验证 Redis 可访问
docker exec -it sa21-redis redis-cli ping
# 预期输出: PONG
```

---

**错误 C: 端口被占用**

```
Address already in use: 0.0.0.0:1788
```

**原因**: 端口 1788 或 8082 被其他进程占用

**解决方案**:
```bash
# 查找占用进程
lsof -i :1788
lsof -i :8082

# 方法 1: 停止占用进程
kill -9 <PID>

# 方法 2: 修改端口配置
# 编辑 docker/.env
SNAIL_JOB_SERVER_PORT=1789
SNAIL_JOB_ADMIN_PORT=8083
```

---

**错误 D: 数据库表不存在**

```
ERROR: relation "sj_namespace" does not exist
```

**原因**: 数据库未初始化

**解决方案**:
```bash
# 重新运行初始化脚本
docker compose up snail-job-init

# 手动执行初始化（如果自动初始化失败）
docker exec -i sa21-postgres psql -U smartadmin -d smart_admin < docker/snail-job/init-schema.sql
```

---

## 2. 客户端连接失败

### 现象

应用启动日志显示：

```
ERROR - Failed to connect to Snail-Job server: Connection refused
ERROR - Snail-Job client registration failed
```

### 排查步骤

#### 2.1 检查服务器是否启动

```bash
# 验证服务器状态
docker compose ps snail-job-server
# 状态应为 "running"

# 检查 RPC 端口是否监听
netstat -an | grep 1788
```

#### 2.2 检查应用配置

**检查配置文件**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
snail-job:
  enabled: true  # ✅ 确认已启用
  server:
    host: localhost  # ✅ 确认主机地址正确
    port: 1788       # ✅ 确认是 RPC 端口，不是 8082
    namespace: dev   # ✅ 确认命名空间存在
    group-name: smart-admin  # ✅ 确认分组已创建
```

#### 2.3 验证命名空间和分组

1. 访问 Snail-Job 控制台: http://localhost:8082
2. **系统管理** → **命名空间管理**
   - 确认 `dev` 命名空间存在
3. **执行器管理** → **分组管理**
   - 确认 `smart-admin` 分组存在

#### 2.4 检查防火墙/网络

**Docker 网络检查**:
```bash
# 检查网络配置
docker network inspect sa21-network

# 验证容器间通信
docker exec sa21-snail-job-server ping redis
```

**防火墙检查** (生产环境):
```bash
# Linux 检查端口是否开放
sudo iptables -L -n | grep 1788

# 临时开放端口测试
sudo iptables -A INPUT -p tcp --dport 1788 -j ACCEPT
```

---

## 3. 任务执行失败

### 现象

控制台执行日志显示任务状态为 **失败**

### 排查步骤

#### 3.1 查看应用日志

```bash
# 查看详细错误堆栈
tail -f logs/smart-admin.log | grep -A 20 "ERROR"
```

#### 3.2 常见原因

**原因 A: 业务逻辑异常**

```java
@JobExecutor(name = "myJob")
public ExecuteResult execute(String params) {
    try {
        // 业务逻辑
        processData();
        return ExecuteResult.success("完成");
    } catch (Exception e) {
        log.error("执行失败", e);  // ✅ 检查这里的日志
        return ExecuteResult.fail(e.getMessage());
    }
}
```

**解决方案**: 修复业务逻辑错误

---

**原因 B: 参数解析失败**

```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException:
Unrecognized field "xxx"
```

**解决方案**: 检查控制台传入的 JSON 参数格式

```json
// ❌ 错误格式
{
  "userName": "test"  // 字段名不匹配
}

// ✅ 正确格式
{
  "username": "test"  // 与 Java 类字段一致
}
```

---

**原因 C: 数据库连接耗尽**

```
SQLTransientConnectionException: HikariPool - Connection is not available
```

**解决方案**: 调整连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 增加连接池大小
      connection-timeout: 30000
```

---

**原因 D: 任务超时**

```
Task execution timeout after 30000ms
```

**解决方案**:
1. 优化任务执行逻辑，减少耗时
2. 增加超时配置（控制台任务配置）

---

## 4. 任务未按预期触发

### 现象

任务已启用，但未在预期时间执行

### 排查步骤

#### 4.1 检查 CRON 表达式

**验证 CRON 表达式**:
```
在线工具: https://crontab.guru/

示例:
0 0 2 * * ?   ✅ 每天凌晨 2 点
0 0 2 * * *   ❌ 错误格式（Quartz 需要 6-7 位）
```

#### 4.2 检查任务状态

1. 控制台 → **任务管理** → **定时任务**
2. 确认任务状态为 **已启用** ✅
3. 查看 **下次执行时间**

#### 4.3 检查执行器状态

1. 控制台 → **执行器管理** → **执行器列表**
2. 确认应用已注册，状态为 **在线** ✅
3. 确认执行器名称与任务配置一致

#### 4.4 检查服务器时间

```bash
# 检查容器时间
docker exec sa21-snail-job-server date

# 检查宿主机时间
date

# 时区不一致可能导致任务不触发
```

**解决方案**: 统一时区

```yaml
# docker-compose.yml
snail-job-server:
  environment:
    TZ: Asia/Shanghai  # 设置时区
```

---

## 5. 分布式重试不生效

### 现象

任务执行失败，但未触发重试机制

### 排查步骤

#### 5.1 检查 @Retryable 注解

```java
@Retryable(
    scene = "payment_processing",  // ✅ 场景标识
    bizNo = "#orderId",            // ✅ 业务 ID
    retryStrategy = "LOCAL_REMOTE" // ✅ 重试策略
)
public void processPayment(Long orderId, BigDecimal amount) {
    // ...
}
```

#### 5.2 检查控制台重试配置

1. 控制台 → **重试管理** → **重试场景配置**
2. 确认场景 `payment_processing` 已配置
3. 检查重试策略、最大次数等配置

#### 5.3 检查异常类型

**重试仅对 RuntimeException 生效**:

```java
// ✅ 会触发重试
throw new BusinessException("支付失败");
throw new RuntimeException("网络错误");

// ❌ 不会触发重试（受检异常需要配置）
throw new Exception("错误");
throw new IOException("IO 错误");
```

#### 5.4 查看重试日志

```bash
# 查看重试记录
# 控制台 → 重试管理 → 重试记录

# 查看应用日志
grep "Retrying task" logs/smart-admin.log
```

---

## 6. 性能问题

### 现象

任务执行缓慢，系统响应延迟

### 排查步骤

#### 6.1 监控关键指标

**控制台查看**:
1. **任务管理** → 查看任务执行耗时
2. **系统监控** → 查看 CPU、内存使用率

**命令行查看**:
```bash
# 查看容器资源使用
docker stats sa21-snail-job-server

# 查看日志中的耗时
grep "elapsed=" logs/smart-admin.log | tail -20
```

#### 6.2 常见性能瓶颈

**瓶颈 A: 数据库慢查询**

**解决方案**:
```sql
-- 开启慢查询日志
SET log_min_duration_statement = 1000;  -- 记录超过 1 秒的查询

-- 查看慢查询
SELECT * FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 优化方案:
-- 1. 添加索引
-- 2. 优化查询语句
-- 3. 使用缓存
```

---

**瓶颈 B: 线程池配置不当**

**解决方案**: 调整线程池配置

```yaml
snail-job:
  executor:
    core-pool-size: 20      # CPU 密集型: CPU核心数
    max-pool-size: 100      # IO 密集型: CPU核心数 * 10
    queue-capacity: 500     # 增加队列容量
```

---

**瓶颈 C: 网络延迟**

**诊断**:
```bash
# 测试网络延迟
ping snail-job-server

# 测试 RPC 响应时间
time curl http://localhost:1788/health
```

**解决方案**:
- 使用内网地址
- 优化网络配置
- 考虑部署到同一可用区

---

**瓶颈 D: 任务并发过高**

**解决方案**: 控制并发数

```java
// 控制台配置
// 任务管理 → 编辑任务 → 高级配置
执行超时时间: 30000  // 30 秒
并发数限制: 10       // 最多 10 个并发实例
```

---

## 7. Web 控制台访问问题

### 现象

无法访问 http://localhost:8082

### 排查步骤

```bash
# 1. 检查端口映射
docker compose ps snail-job-server | grep 8082

# 2. 检查服务是否监听
docker exec sa21-snail-job-server netstat -an | grep 8080

# 3. 测试容器内访问
docker exec sa21-snail-job-server curl http://localhost:8080

# 4. 测试宿主机访问
curl http://localhost:8082
```

### 解决方案

**问题 A: 端口映射错误**

检查 `docker-compose.yml`:
```yaml
ports:
  - "8082:8080"  # ✅ 正确：宿主机:容器
  - "8080:8082"  # ❌ 错误
```

**问题 B: 防火墙阻止**

```bash
# 临时开放端口
sudo iptables -A INPUT -p tcp --dport 8082 -j ACCEPT

# 或关闭防火墙测试
sudo systemctl stop firewalld
```

---

## 8. 日志问题

### 查看日志的多种方式

#### 8.1 Docker 日志

```bash
# 实时查看
docker logs -f sa21-snail-job-server

# 查看最近 100 行
docker logs --tail 100 sa21-snail-job-server

# 查看特定时间段
docker logs --since "2026-01-22T08:00:00" sa21-snail-job-server
```

#### 8.2 应用日志

```bash
# SmartAdmin 应用日志
tail -f smart-admin-api-java21-springboot3/logs/smart-admin.log

# 过滤错误日志
grep "ERROR" logs/smart-admin.log | tail -50

# 过滤任务执行日志
grep "Job" logs/smart-admin.log | tail -50
```

#### 8.3 控制台日志查看

1. 登录 http://localhost:8082
2. **任务管理** → **定时任务** → **操作** → **执行日志**
3. 查看详细执行记录

---

## 9. 数据一致性问题

### 现象

任务执行多次或执行状态不一致

### 排查步骤

#### 9.1 检查幂等性

```java
@Retryable(
    scene = "payment",
    bizNo = "#orderId"  // ✅ 使用业务 ID 保证幂等
)
public void processPayment(Long orderId, BigDecimal amount) {
    // 业务逻辑应支持幂等
    if (isAlreadyProcessed(orderId)) {
        return;  // 已处理，直接返回
    }
    // 执行支付
}
```

#### 9.2 检查分布式锁

```java
@JobExecutor(name = "dataCleanupJob")
public ExecuteResult execute(String params) {
    // 使用 Redis 分布式锁
    RLock lock = redissonClient.getLock("job:dataCleanup");
    try {
        if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
            // 执行任务
            cleanupData();
        } else {
            log.warn("获取锁失败，任务已在其他节点执行");
        }
    } finally {
        lock.unlock();
    }
}
```

---

## 🆘 获取帮助

### 诊断信息收集

提交问题时，请提供以下信息：

```bash
# 1. 版本信息
docker exec sa21-snail-job-server java -version
./gradlew --version

# 2. 服务状态
docker compose ps

# 3. 配置信息（脱敏）
cat sa-admin/src/main/resources/dev/sa-base.yaml | grep -A 20 "snail-job"

# 4. 最近日志
docker logs --tail 200 sa21-snail-job-server > snail-job-server.log
tail -200 logs/smart-admin.log > smart-admin.log

# 5. 网络信息
netstat -an | grep "1788\|8082"
```

### 社区支持

- [Snail-Job GitHub Issues](https://github.com/aizuda/snail-job/issues)
- [SmartAdmin GitHub Issues](https://github.com/1024-lab/smart-admin)
- [Snail-Job 官方文档](https://snailjob.opensnail.com/)

---

## 下一步

- [架构设计](./05-architecture.md) - 深入了解原理
- [监控运维](./06-monitoring.md) - 生产环境监控
- [配置参考](./02-configuration.md) - 详细配置说明

---

**故障排查完成！** 如问题仍未解决，请提交 Issue 并附上诊断信息。
