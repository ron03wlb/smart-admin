# 从 SmartJob 迁移到 Snail-Job

> SmartJob 到 Snail-Job 的完整迁移指南

## 📋 目录

- [迁移概述](#迁移概述)
- [功能对比](#功能对比)
- [API 迁移指南](#api-迁移指南)
- [数据迁移方案](#数据迁移方案)
- [迁移最佳实践](#迁移最佳实践)

---

## 迁移概述

### 为什么迁移？

**SmartJob (Quartz-based)** → **Snail-Job**

| 迁移理由 | 说明 |
|---------|------|
| ✅ 分布式重试 | SmartJob 无内置重试，Snail-Job 提供完整的分布式重试系统 |
| ✅ DAG 工作流 | Snail-Job 支持复杂任务依赖编排 |
| ✅ 现代化 UI | 实时监控、可视化配置、执行日志查看 |
| ✅ 性能优化 | 更高效的调度算法，支持更大规模并发 |
| ✅ 社区活跃 | 持续更新，Spring Boot 3 原生支持 |

### 迁移策略

#### 策略 A: 完全替换（推荐）

```
适用场景: SmartJob 未被使用或使用较少

步骤:
1. 移除 SmartJob 模块
2. 安装 Snail-Job
3. 重写任务逻辑
4. 配置任务调度

优点: 架构统一，维护成本低
缺点: 需要重写代码
```

#### 策略 B: 并行运行（过渡期）

```
适用场景: SmartJob 已有大量任务，需要逐步迁移

步骤:
1. 保留 SmartJob
2. 新增 Snail-Job
3. 新任务使用 Snail-Job
4. 逐步迁移旧任务
5. 最终移除 SmartJob

优点: 风险低，渐进式迁移
缺点: 维护两套系统
```

---

## 功能对比

### SmartJob vs Snail-Job

| 功能 | SmartJob | Snail-Job |
|------|----------|-----------|
| **调度引擎** | Quartz | 自研高性能引擎 |
| **CRON 调度** | ✅ 支持 | ✅ 支持 |
| **固定频率调度** | ✅ 支持 | ✅ 支持 |
| **分布式锁** | ✅ Redis | ✅ Redis |
| **执行模式** | 单机 | 单机/广播/分片/MapReduce |
| **分布式重试** | ❌ 无 | ✅ 完整支持 |
| **DAG 工作流** | ❌ 无 | ✅ 支持 |
| **管理界面** | 基础 CRUD API | 现代化 Web 控制台 |
| **执行日志** | 数据库记录 | 实时日志 + 数据库 |
| **任务参数** | 数据库配置 | 控制台配置 + JSON |
| **多租户** | ❌ 无 | ✅ 命名空间隔离 |
| **告警通知** | ❌ 无 | ✅ 邮件/钉钉/企业微信 |

---

## API 迁移指南

### 1. 任务定义迁移

#### SmartJob 写法

```java
package net.lab1024.sa.base.module.support.job.sample;

import net.lab1024.sa.base.module.support.job.core.SmartJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmartJobSample1 implements SmartJob {

    @Override
    public String run(String param) {
        log.info("SmartJobSample1 executing, param: {}", param);

        try {
            // 业务逻辑
            processData();
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Job failed", e);
            return "FAILED: " + e.getMessage();
        }
    }

    private void processData() {
        // 实现业务逻辑
    }
}
```

#### Snail-Job 写法

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component  // ✅ 使用 @Component 替代 @Service
public class DataProcessingJob {

    @JobExecutor(name = "dataProcessingJob")  // ✅ 添加注解
    public ExecuteResult execute(String params) {  // ✅ 返回类型改变
        log.info("DataProcessingJob executing, params: {}", params);

        try {
            // 业务逻辑（无需改变）
            processData();

            return ExecuteResult.success("SUCCESS");  // ✅ 使用 ExecuteResult

        } catch (Exception e) {
            log.error("Job failed", e);
            return ExecuteResult.fail("FAILED: " + e.getMessage());
        }
    }

    private void processData() {
        // 实现业务逻辑（无需改变）
    }
}
```

**迁移要点**:
1. ✅ 移除 `implements SmartJob`
2. ✅ 添加 `@JobExecutor(name = "xxx")` 注解
3. ✅ 方法签名改为 `ExecuteResult execute(String params)`
4. ✅ 返回 `ExecuteResult.success()` 或 `ExecuteResult.fail()`

---

### 2. 配置迁移

#### SmartJob 配置

```yaml
# SmartJob 配置（sa-base.yaml）
smart:
  job:
    enabled: true
    corePoolSize: 2
    initDelay: 30
    dbRefreshEnabled: true
    dbRefreshInterval: 120
```

#### Snail-Job 配置

```yaml
# Snail-Job 配置（sa-base.yaml）
snail-job:
  enabled: true
  server:
    host: localhost
    port: 1788
    namespace: dev
    group-name: smart-admin
    token: ""
  client:
    port: 1789
  executor:
    core-pool-size: 10  # ✅ 更强大的线程池配置
    max-pool-size: 50
    queue-capacity: 200
```

---

### 3. 任务调度迁移

#### SmartJob 调度配置

**数据库配置** (`t_smart_job` 表):
```sql
INSERT INTO t_smart_job (
  job_name,
  job_class,
  trigger_type,
  trigger_value,
  param,
  enabled_flag
) VALUES (
  '数据清理任务',
  'net.lab1024.sa.base.module.support.job.sample.SmartJobSample1',
  'CRON',
  '0 0 2 * * ?',
  '{"retentionDays": 30}',
  1
);
```

#### Snail-Job 调度配置

**Web 控制台配置**:
1. 访问 http://localhost:8082
2. **任务管理** → **定时任务** → **新增任务**
3. 填写表单:
   - 任务名称: 数据清理任务
   - 执行器: `dataCleanupJob`
   - CRON: `0 0 2 * * ?`
   - 参数: `{"retentionDays": 30}`
   - 执行模式: 单机执行

**优势**:
- ✅ 无需写 SQL，直接在界面配置
- ✅ 实时生效，无需重启应用
- ✅ 可视化 CRON 表达式验证
- ✅ 支持任务启用/禁用开关

---

### 4. 依赖注入迁移

#### SmartJob 写法

```java
@Service
public class UserSyncJob implements SmartJob {

    @Autowired  // ❌ SmartJob 使用字段注入
    private UserService userService;

    @Override
    public String run(String param) {
        List<User> users = userService.findAll();
        // ...
        return "SUCCESS";
    }
}
```

#### Snail-Job 写法

```java
@Component
@RequiredArgsConstructor  // ✅ 使用构造器注入（推荐）
public class UserSyncJob {

    private final UserService userService;  // ✅ final 字段

    @JobExecutor(name = "userSyncJob")
    public ExecuteResult execute(String params) {
        List<User> users = userService.findAll();
        // ...
        return ExecuteResult.success("SUCCESS");
    }
}
```

**迁移要点**:
- ✅ 改用构造器注入（SmartAdmin 标准）
- ✅ 使用 `@RequiredArgsConstructor` + `private final`
- ✅ 移除 `@Autowired` 字段注入

---

### 5. 异常处理迁移

#### SmartJob 写法

```java
@Override
public String run(String param) {
    try {
        processData();
        return "SUCCESS";
    } catch (Exception e) {
        log.error("Job failed", e);
        return "FAILED";  // ❌ 异常被吞掉，无法触发重试
    }
}
```

#### Snail-Job 写法（支持重试）

```java
@JobExecutor(name = "myJob")
public ExecuteResult execute(String params) {
    try {
        processData();
        return ExecuteResult.success("SUCCESS");

    } catch (Exception e) {
        log.error("Job failed", e);
        // ✅ 返回失败结果，会触发重试机制
        return ExecuteResult.fail("FAILED: " + e.getMessage());
    }
}
```

**或使用分布式重试注解**:

```java
@Retryable(
    scene = "data_processing",
    retryStrategy = "LOCAL_REMOTE",
    localTimes = 3,
    localInterval = 2
)
public void processData() {
    // 业务逻辑
    // 如果抛出异常，会自动触发重试
}
```

---

## 数据迁移方案

### 方案 A: 软弃用（推荐）

**保留 SmartJob 数据，不影响现有系统**

```sql
-- 1. 添加注释标记弃用
COMMENT ON TABLE t_smart_job IS '已弃用：SmartJob 替换为 Snail-Job (v3.1.0)';
COMMENT ON TABLE t_smart_job_log IS '已弃用：SmartJob 替换为 Snail-Job (v3.1.0)';

-- 2. 可选：导出历史数据用于分析
SELECT * INTO smart_job_backup_20260122
FROM t_smart_job;

SELECT * INTO smart_job_log_backup_20260122
FROM t_smart_job_log;
```

**优点**:
- ✅ 零风险，不破坏现有数据
- ✅ 保留历史记录用于审计
- ✅ 可随时回滚

**缺点**:
- ❌ 占用存储空间

---

### 方案 B: 数据映射迁移

**将 SmartJob 任务配置转换为 Snail-Job 格式**

```sql
-- 生成 Snail-Job 导入脚本
SELECT
  CONCAT(
    'curl -X POST http://localhost:8082/api/job/create ',
    '-H "Content-Type: application/json" ',
    '-d ''{"jobName":"', job_name, '",',
    '"executorName":"', REPLACE(job_class, 'SmartJobSample', 'Job'), '",',
    '"cronExpression":"', trigger_value, '",',
    '"params":"', param, '"}'''
  ) AS import_command
FROM t_smart_job
WHERE enabled_flag = 1;
```

**执行生成的命令**:
```bash
# 示例输出
curl -X POST http://localhost:8082/api/job/create \
  -H "Content-Type: application/json" \
  -d '{"jobName":"数据清理任务","executorName":"DataCleanupJob","cronExpression":"0 0 2 * * ?","params":"{}"}'
```

---

### 方案 C: 硬删除（生产环境升级后）

**在确认 Snail-Job 稳定运行 1-2 周后执行**

```sql
-- 创建迁移脚本
-- 文件: v3.1.0_snail_job_migration.sql

-- 1. 创建备份
CREATE TABLE t_smart_job_backup AS SELECT * FROM t_smart_job;
CREATE TABLE t_smart_job_log_backup AS SELECT * FROM t_smart_job_log;

-- 2. 删除原表
DROP TABLE IF EXISTS t_smart_job_log CASCADE;
DROP TABLE IF EXISTS t_smart_job CASCADE;

-- 3. 记录迁移
INSERT INTO t_changelog (
  version,
  description,
  executed_at
) VALUES (
  'v3.1.0',
  'Migrated from SmartJob to Snail-Job',
  NOW()
);
```

---

## 迁移最佳实践

### 迁移前准备

#### 1. 评估现有任务

```bash
# 统计现有任务数量
SELECT COUNT(*) FROM t_smart_job WHERE enabled_flag = 1;

# 查看任务执行频率
SELECT
  job_name,
  COUNT(*) AS exec_count
FROM t_smart_job_log
WHERE DATE(create_time) >= CURRENT_DATE - 7
GROUP BY job_name
ORDER BY exec_count DESC;
```

#### 2. 制定迁移计划

| 阶段 | 任务 | 时间 | 负责人 |
|------|------|------|--------|
| 准备期 | 安装 Snail-Job、测试环境验证 | Week 1 | 开发团队 |
| 迁移期 | 逐步迁移任务、代码重构 | Week 2-3 | 开发团队 |
| 并行期 | SmartJob + Snail-Job 并行运行 | Week 4-5 | 运维团队 |
| 验证期 | 监控、性能测试、稳定性验证 | Week 6 | 测试团队 |
| 清理期 | 移除 SmartJob 模块 | Week 7 | 开发团队 |

#### 3. 创建任务清单

```markdown
## 任务迁移清单

### 高优先级（每日执行）
- [ ] 数据清理任务（每日凌晨 2 点）
- [ ] 日报生成任务（每日早上 8 点）
- [ ] 缓存刷新任务（每小时）

### 中优先级（每周执行）
- [ ] 数据统计任务（每周一）
- [ ] 备份任务（每周日）

### 低优先级（按需执行）
- [ ] 数据导出任务
- [ ] 手动触发任务
```

---

### 迁移步骤

#### Step 1: 测试环境验证

```bash
# 1. 部署 Snail-Job 到测试环境
cd docker
docker compose -f docker-compose.test.yml up -d snail-job-server snail-job-init

# 2. 配置测试应用
# 编辑 test/sa-base.yaml
snail-job:
  enabled: true
  server:
    namespace: test

# 3. 迁移一个简单任务进行验证
# 4. 观察运行 24 小时，确认稳定
```

#### Step 2: 生产环境部署

```bash
# 1. 备份现有数据
./backup-smart-job.sh

# 2. 部署 Snail-Job 服务器
docker compose up -d snail-job-server snail-job-init

# 3. 验证服务健康
curl http://localhost:8082/actuator/health

# 4. 配置生产应用（保留 SmartJob 配置）
snail-job:
  enabled: true
smart:
  job:
    enabled: true  # 暂时保留
```

#### Step 3: 逐步迁移任务

```
Week 1: 迁移非关键任务（10%）
  - 测试任务
  - 低频率任务

Week 2: 迁移重要任务（40%）
  - 数据清理
  - 缓存刷新

Week 3: 迁移核心任务（50%）
  - 日报生成
  - 数据同步

验证 2 周稳定运行
```

#### Step 4: 移除 SmartJob

```bash
# 1. 确认所有任务已迁移
# 2. 禁用 SmartJob
# 编辑所有环境的 sa-base.yaml
smart:
  job:
    enabled: false

# 3. 重启应用验证
# 4. 观察 1 周无问题

# 5. 移除 SmartJob 模块
# 删除代码
rm -rf sa-base/support/job/

# 修改 settings.gradle.kts
# 删除: "sa-base:support:job",

# 6. 清理数据库（可选）
# 执行 v3.1.0_snail_job_migration.sql
```

---

### 回滚计划

**如果迁移过程中出现问题**:

```bash
# 1. 立即禁用 Snail-Job
snail-job:
  enabled: false

# 2. 启用 SmartJob
smart:
  job:
    enabled: true

# 3. 重启应用
./gradlew :sa-admin:bootRun

# 4. 验证 SmartJob 任务正常执行
# 5. 分析问题原因
# 6. 修复后重新迁移
```

---

### 迁移验证

#### 功能验证清单

```markdown
## 验证清单

### 任务调度
- [ ] CRON 调度正常
- [ ] 固定频率调度正常
- [ ] 手动触发正常

### 任务执行
- [ ] 单机执行模式正常
- [ ] 任务参数传递正确
- [ ] 执行日志记录完整
- [ ] 异常处理正确

### 分布式特性
- [ ] 多节点负载均衡
- [ ] 故障转移正常
- [ ] 分布式锁生效

### 监控告警
- [ ] 执行失败告警正常
- [ ] 执行超时告警正常
- [ ] 控制台监控数据正常

### 性能验证
- [ ] 任务执行性能不下降
- [ ] 系统资源使用正常
- [ ] 数据库连接正常
```

---

## 常见问题

### Q1: 迁移期间如何避免任务重复执行？

**方案**: 使用命名空间隔离

```yaml
# SmartJob 配置
smart:
  job:
    enabled: true
    # 任务名称: oldDataCleanup

# Snail-Job 配置
snail-job:
  enabled: true
  server:
    namespace: migration  # 使用独立命名空间
    # 任务名称: newDataCleanup
```

### Q2: 如何处理正在执行的长时间任务？

**方案**: 优雅停机

```java
@JobExecutor(name = "longRunningJob")
public ExecuteResult execute(String params) {
    // 检查是否应该停止
    while (hasMoreData() && !shouldStop()) {
        processData();
    }

    if (shouldStop()) {
        return ExecuteResult.fail("任务被中断");
    }
    return ExecuteResult.success("完成");
}

private boolean shouldStop() {
    // 检查停止信号（Redis 标志位等）
    return redisTemplate.hasKey("job:stop:longRunningJob");
}
```

### Q3: 迁移后性能下降怎么办？

**排查步骤**:
1. 检查线程池配置是否合理
2. 检查数据库连接池大小
3. 查看慢任务执行日志
4. 对比 SmartJob vs Snail-Job 执行耗时

**优化建议**: 见 [性能优化](./05-architecture.md#性能优化)

---

## 下一步

- [快速开始](./01-quick-start.md) - Snail-Job 快速上手
- [API 使用](./03-api-usage.md) - 代码示例
- [故障排查](./04-troubleshooting.md) - 问题诊断

---

**迁移指南完成！** 按照本指南逐步迁移，确保平滑过渡。
