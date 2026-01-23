# Snail-Job 架构设计说明

> 深入理解 Snail-Job 的架构设计、任务调度原理与最佳实践

## 📋 目录

- [系统架构](#系统架构)
- [任务调度原理](#任务调度原理)
- [分布式重试机制](#分布式重试机制)
- [高可用部署](#高可用部署)
- [性能优化](#性能优化)

---

## 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                   SmartAdmin Applications                   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  App Node 1  │  │  App Node 2  │  │  App Node 3  │    │
│  │              │  │              │  │              │    │
│  │ Job Executor │  │ Job Executor │  │ Job Executor │    │
│  │ @Component   │  │ @Component   │  │ @Component   │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
│         │                  │                  │            │
│         └──────────────────┴──────────────────┘            │
│                            │                                │
│                 Snail-Job Client                            │
│                    (Netty RPC)                              │
└───────────────────────────┼─────────────────────────────────┘
                            │
                            │ RPC: Port 1788
                            │ Heartbeat: 30s
                            │
┌───────────────────────────┼─────────────────────────────────┐
│                           ↓                                  │
│              Snail-Job Server Cluster                        │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Server 1    │  │  Server 2    │  │  Server 3    │    │
│  │  (Master)    │  │  (Standby)   │  │  (Standby)   │    │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤    │
│  │ Scheduler    │  │ Scheduler    │  │ Scheduler    │    │
│  │ Engine       │  │ Engine       │  │ Engine       │    │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤    │
│  │ Retry        │  │ Retry        │  │ Retry        │    │
│  │ Engine       │  │ Engine       │  │ Engine       │    │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤    │
│  │ DAG Workflow │  │ DAG Workflow │  │ DAG Workflow │    │
│  │ Engine       │  │ Engine       │  │ Engine       │    │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤    │
│  │ Web Console  │  │ Web Console  │  │ Web Console  │    │
│  │ (Port 8082)  │  │ (Port 8082)  │  │ (Port 8082)  │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│         │                  │                  │            │
│         └──────────────────┴──────────────────┘            │
│                            │                                │
└────────────────────────────┼────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
        ┌───────┴────────┐      ┌────────┴────────┐
        │  PostgreSQL    │      │     Redis       │
        │                │      │                 │
        │ - 任务配置      │      │ - 分布式锁      │
        │ - 执行记录      │      │ - 任务队列      │
        │ - 重试记录      │      │ - 客户端心跳    │
        └────────────────┘      └─────────────────┘
```

### 核心组件

#### 1. Snail-Job Client (客户端)

**职责**:
- 任务执行器注册与发现
- 接收任务调度指令
- 执行任务并返回结果
- 心跳保持与健康检查

**关键类**:
```java
// SmartAdmin 集成
@Configuration
@ConditionalOnProperty(prefix = "snail-job", name = "enabled", havingValue = "true")
public class SnailJobAutoConfiguration {

    @Bean
    public SnailJobClient snailJobClient(SnailJobConfig config) {
        return SnailJobClient.builder()
            .serverHost(config.getServer().getHost())
            .serverPort(config.getServer().getPort())
            .namespace(config.getServer().getNamespace())
            .groupName(config.getServer().getGroupName())
            .token(config.getServer().getToken())
            .build();
    }
}
```

#### 2. Snail-Job Server (服务端)

**职责**:
- 任务调度与分发
- 执行器管理
- 重试机制实现
- DAG 工作流编排
- Web 控制台

**核心引擎**:

| 引擎 | 功能 |
|------|------|
| Scheduler Engine | CRON 调度、固定频率调度 |
| Retry Engine | 分布式重试、退避策略 |
| DAG Engine | 工作流编排、依赖管理 |
| Dispatcher | 任务分发、负载均衡 |

---

## 任务调度原理

### 调度流程

```
1. 任务配置
   ↓
2. Scheduler Engine 扫描任务表 (每秒)
   ↓
3. 判断任务是否到期执行
   ↓
4. 获取可用执行器列表
   ↓
5. 根据执行模式选择执行器
   │
   ├─→ 单机执行: 选择一个执行器
   ├─→ 广播执行: 所有执行器
   ├─→ 分片执行: 多个执行器（分片参数）
   └─→ MapReduce: 分片 + 聚合
   ↓
6. 通过 Netty RPC 发送执行指令
   ↓
7. 执行器接收指令并执行任务
   ↓
8. 返回执行结果
   ↓
9. 记录执行日志
   ↓
10. 更新任务状态 & 计算下次执行时间
```

### CRON 调度实现

**时间轮算法** (Time Wheel):

```
┌─────────────────────────────────────────────┐
│            Time Wheel (60 slots)            │
│                                             │
│  0   1   2   3  ...  58  59                │
│  │   │   │   │       │   │                 │
│  ↓   ↓   ↓   ↓       ↓   ↓                 │
│ Task Task     Task       Task              │
│ List List     List       List              │
└─────────────────────────────────────────────┘
         │
         ↓ 每秒移动一个槽位
    检查当前槽位的任务列表
         │
         ↓ 如果 CRON 匹配
    加入执行队列
```

**优势**:
- O(1) 时间复杂度
- 低 CPU 占用
- 高并发支持

### 执行模式详解

#### 1. 单机执行 (Standalone)

```java
// 仅在集群中的一个节点执行
// 适用场景: 数据统计、报表生成

@JobExecutor(name = "dailyReportJob")
public ExecuteResult execute(String params) {
    // 生成报表（只需执行一次）
    return ExecuteResult.success("报表已生成");
}
```

**选择策略**:
- 随机选择
- 轮询选择
- 一致性哈希

#### 2. 广播执行 (Broadcast)

```java
// 在所有节点上执行
// 适用场景: 缓存刷新、配置更新

@JobExecutor(name = "cacheRefreshJob")
public ExecuteResult execute(String params) {
    // 每个节点刷新本地缓存
    localCache.refresh();
    return ExecuteResult.success("缓存已刷新");
}
```

#### 3. 分片执行 (Sharding)

```java
// 数据分片并行处理
// 适用场景: 大数据量处理

@JobExecutor(name = "userDataProcessJob")
public ExecuteResult execute(String params) {
    // 获取分片参数
    int shardIndex = JobContext.getShardIndex();    // 当前分片索引
    int shardTotal = JobContext.getShardTotal();    // 总分片数

    // 按分片处理数据
    List<User> users = userDao.selectBySharding(shardIndex, shardTotal);
    processUsers(users);

    return ExecuteResult.success("分片 " + shardIndex + " 处理完成");
}
```

**分片策略**:
```
总数据: 10000 条
分片数: 4
节点 1: 处理 0, 4, 8, 12, ... (shardIndex=0)
节点 2: 处理 1, 5, 9, 13, ... (shardIndex=1)
节点 3: 处理 2, 6, 10, 14, ... (shardIndex=2)
节点 4: 处理 3, 7, 11, 15, ... (shardIndex=3)
```

#### 4. MapReduce 执行

```java
// Map 阶段
@JobExecutor(name = "mapJob")
public ExecuteResult map(String params) {
    int shardIndex = JobContext.getShardIndex();
    // 分片处理数据
    List<Result> results = processShardData(shardIndex);
    // 返回中间结果
    return ExecuteResult.success(JSONUtil.toJsonStr(results));
}

// Reduce 阶段
@JobExecutor(name = "reduceJob")
public ExecuteResult reduce(String params) {
    // 聚合所有 Map 的结果
    List<Result> allResults = getAllMapResults();
    Result finalResult = aggregate(allResults);
    return ExecuteResult.success(JSONUtil.toJsonStr(finalResult));
}
```

---

## 分布式重试机制

### 重试架构

```
┌─────────────────────────────────────────────┐
│          Application Thread                 │
│                                             │
│  @Retryable Method Invoked                  │
│         │                                   │
│         ↓                                   │
│  AOP Interceptor                            │
│         │                                   │
│    ┌────┴─────┐                             │
│    │ Try      │                             │
│    │ Execute  │                             │
│    └────┬─────┘                             │
│         │                                   │
│    Success? ───────→ Yes → Return           │
│         │                                   │
│        No                                   │
│         │                                   │
│    Local Retry? ───→ Yes ─┐                │
│         │                  │                │
│        No                  ↓                │
│         │          ┌──────────────┐         │
│         │          │ Local Retry  │         │
│         │          │ (Blocking)   │         │
│         │          └──────┬───────┘         │
│         │                 │                 │
│         │            Success? → Return      │
│         │                 │                 │
│         │                No                 │
│         │                 │                 │
│         └─────────────────┘                 │
│         │                                   │
│         ↓                                   │
│  Save to Retry Queue                        │
│  (PostgreSQL + Redis)                       │
└─────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────┐
│         Retry Engine (Server)               │
│                                             │
│  ┌──────────────────────────────────┐      │
│  │  Retry Queue Polling             │      │
│  │  (每 5 秒扫描一次)                 │      │
│  └──────────┬───────────────────────┘      │
│             │                               │
│             ↓                               │
│      Task Due to Retry?                     │
│             │                               │
│            Yes                              │
│             │                               │
│             ↓                               │
│   ┌──────────────────┐                      │
│   │ Select Executor  │                      │
│   └────────┬─────────┘                      │
│            │                                │
│            ↓                                │
│   ┌──────────────────┐                      │
│   │  Send RPC Call   │                      │
│   └────────┬─────────┘                      │
│            │                                │
│            ↓                                │
│       Success?                              │
│         │    │                              │
│        Yes  No                              │
│         │    │                              │
│     Remove  Update                          │
│      from  Next Retry                       │
│     Queue  Time                             │
│             │                               │
│             ↓                               │
│    Max Retries Exceeded?                    │
│         │         │                         │
│        Yes       No                         │
│         │         │                         │
│    Dead Letter  Continue                    │
│       Queue    Retrying                     │
└─────────────────────────────────────────────┘
```

### 退避策略

#### 1. 固定延迟 (Fixed)

```
重试间隔固定不变
第 1 次: 2 秒后
第 2 次: 2 秒后
第 3 次: 2 秒后
...
```

**适用场景**: 瞬时故障（网络抖动）

#### 2. 指数退避 (Exponential)

```
重试间隔指数增长
第 1 次: 2 秒后
第 2 次: 4 秒后 (2 × 2)
第 3 次: 8 秒后 (4 × 2)
第 4 次: 16 秒后 (8 × 2)
...
最大延迟: 5 分钟
```

**适用场景**: 下游服务过载、限流

#### 3. 抖动退避 (Jittered)

```
在指数退避基础上加入随机抖动
第 1 次: 2 ± 0.5 秒后
第 2 次: 4 ± 1 秒后
第 3 次: 8 ± 2 秒后
...
```

**优势**: 避免重试风暴，分散请求

### 幂等性保证

**业务 ID (bizNo)**:

```java
@Retryable(
    scene = "payment",
    bizNo = "#orderId"  // 使用订单 ID 作为幂等键
)
public void processPayment(Long orderId, BigDecimal amount) {
    // Snail-Job 自动去重
    // 相同 bizNo 的重试只会执行一次
}
```

**实现原理**:
```sql
-- 重试记录表包含唯一约束
CREATE UNIQUE INDEX idx_retry_biz
ON sj_retry_task (scene, biz_no, namespace);

-- 插入时自动去重
INSERT INTO sj_retry_task (...)
ON CONFLICT (scene, biz_no, namespace)
DO UPDATE SET retry_count = retry_count + 1;
```

---

## 高可用部署

### 服务器集群部署

#### 架构图

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (反向代理)  │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────┴─────┐      ┌────┴─────┐      ┌────┴─────┐
   │ Server 1 │      │ Server 2 │      │ Server 3 │
   │ (Master) │      │(Standby) │      │(Standby) │
   └────┬─────┘      └────┬─────┘      └────┬─────┘
        │                  │                  │
        └──────────────────┴──────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
     ┌────────┴────────┐      ┌────────┴────────┐
     │   PostgreSQL    │      │   Redis         │
     │   (主从复制)     │      │   (Sentinel)    │
     └─────────────────┘      └─────────────────┘
```

#### Docker Compose 集群配置

```yaml
version: '3.8'

services:
  # Snail-Job Server 1 (Master)
  snail-job-server-1:
    image: aizuda/snail-job-server:1.8.1
    container_name: snail-job-server-1
    environment:
      SERVER_ID: 1
      CLUSTER_MODE: true
      CLUSTER_NODES: snail-job-server-1:1788,snail-job-server-2:1788,snail-job-server-3:1788
    ports:
      - "1788:1788"
      - "8082:8080"
    networks:
      - snail-job-cluster

  # Snail-Job Server 2 (Standby)
  snail-job-server-2:
    image: aizuda/snail-job-server:1.8.1
    container_name: snail-job-server-2
    environment:
      SERVER_ID: 2
      CLUSTER_MODE: true
      CLUSTER_NODES: snail-job-server-1:1788,snail-job-server-2:1788,snail-job-server-3:1788
    ports:
      - "1789:1788"
      - "8083:8080"
    networks:
      - snail-job-cluster

  # Snail-Job Server 3 (Standby)
  snail-job-server-3:
    image: aizuda/snail-job-server:1.8.1
    container_name: snail-job-server-3
    environment:
      SERVER_ID: 3
      CLUSTER_MODE: true
      CLUSTER_NODES: snail-job-server-1:1788,snail-job-server-2:1788,snail-job-server-3:1788
    ports:
      - "1790:1788"
      - "8084:8080"
    networks:
      - snail-job-cluster
```

### 故障转移机制

**心跳检测**:
```
客户端 → 服务器: 心跳包 (每 30 秒)
服务器 → 客户端: 心跳响应

如果 3 次心跳失败:
  标记执行器为离线
  将任务重新分配到其他执行器
```

**主备切换**:
```
1. Master 服务器宕机
   ↓
2. Standby 服务器通过心跳检测发现
   ↓
3. Standby 竞选 Master（基于 Redis 分布式锁）
   ↓
4. 新 Master 接管任务调度
   ↓
5. 客户端自动重连新 Master
```

---

## 性能优化

### 调度性能优化

#### 1. 批量处理

```java
// 批量获取待调度任务
List<Task> tasks = taskDao.selectPendingTasks(limit=1000);

// 并行调度
tasks.parallelStream().forEach(task -> {
    dispatch(task);
});
```

#### 2. 任务分桶

```
将任务按时间分桶存储
Bucket 0: 0-5 秒内到期的任务
Bucket 1: 5-10 秒内到期的任务
Bucket 2: 10-30 秒内到期的任务
...

仅扫描最近的桶，减少查询范围
```

#### 3. 预加载机制

```
提前 5 秒加载即将到期的任务到内存
减少数据库查询压力
```

### 执行性能优化

#### 1. 线程池隔离

```yaml
snail-job:
  executor:
    # CPU 密集型任务线程池
    cpu-pool:
      core-size: 8
      max-size: 16

    # IO 密集型任务线程池
    io-pool:
      core-size: 50
      max-size: 200
```

#### 2. 异步执行

```java
@JobExecutor(name = "asyncJob", async = true)
public ExecuteResult execute(String params) {
    // 异步执行，立即返回
    CompletableFuture.runAsync(() -> {
        // 长时间任务
        processLargeData();
    });

    return ExecuteResult.success("已提交异步执行");
}
```

### 数据库优化

#### 1. 索引优化

```sql
-- 任务查询索引
CREATE INDEX idx_task_next_time
ON sj_job (next_exec_time, status)
WHERE deleted = 0;

-- 重试任务索引
CREATE INDEX idx_retry_next_time
ON sj_retry_task (next_retry_time, retry_status);

-- 执行日志分区
CREATE TABLE sj_job_log_202601
PARTITION OF sj_job_log
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

#### 2. 连接池配置

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 20
      maximum-pool-size: 100
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
```

### 网络优化

#### 1. Netty 调优

```yaml
snail-job:
  netty:
    boss-threads: 1
    worker-threads: 8  # CPU 核心数
    max-frame-length: 10485760  # 10MB
    connect-timeout: 5000
    keepalive: true
```

#### 2. 序列化优化

```
使用高效序列化协议:
- Protobuf: 性能最优
- Kryo: 平衡性能和易用性
- JSON: 易于调试
```

---

## 下一步

- [监控运维指南](./06-monitoring.md) - 生产环境监控
- [配置参考](./02-configuration.md) - 详细配置说明
- [故障排查](./04-troubleshooting.md) - 问题诊断

---

**架构设计完成！** 深入理解原理有助于更好地使用和优化系统。
