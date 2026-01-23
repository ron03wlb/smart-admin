# Snail-Job 快速开始

> 5 分钟快速上手 Snail-Job 分布式任务调度

## 📋 前置条件

在开始之前，请确保您的开发环境已安装：

- ✅ **Docker & Docker Compose** (20.10+)
- ✅ **Java 21** (SmartAdmin 要求)
- ✅ **SmartAdmin** v3.1.0+
- ✅ **Git** (克隆项目代码)

---

## 🚀 5 分钟快速上手

### 步骤 1: 启动 Snail-Job 服务器

打开终端，进入项目的 Docker 目录：

```bash
cd docker
```

启动 Snail-Job 服务及数据库初始化：

```bash
docker compose up -d snail-job-server snail-job-init
```

**预期输出**:
```
[+] Running 3/3
 ✔ Network sa21-network           Created
 ✔ Container sa21-snail-job-init   Started
 ✔ Container sa21-snail-job-server Started
```

**验证服务状态**:
```bash
# 查看服务状态
docker compose ps snail-job-server

# 查看启动日志
docker logs sa21-snail-job-server

# 等待看到以下日志表示启动成功：
# "Started SnailJobServerApplication in X.XXX seconds"
```

---

### 步骤 2: 访问管理控制台

**打开浏览器访问**：http://localhost:8082

**登录凭据**:
- 用户名：`admin`
- 密码：`SmartAdmin@2024`

**首次登录后**:
1. 导航到 **系统管理** → **命名空间管理**
2. 确认 `dev` 命名空间已创建
3. 导航到 **执行器管理** → **分组管理**
4. 确认 `smart-admin` 分组已创建

---

### 步骤 3: 配置应用连接

编辑开发环境配置文件：

**文件路径**: `smart-admin-api-java21-springboot3/sa-admin/src/main/resources/dev/sa-base.yaml`

**添加以下配置**（在文件末尾添加）:

```yaml
# ===========================================
# Snail-Job Task Scheduler Configuration
# ===========================================
snail-job:
  # 是否启用 Snail-Job
  enabled: true

  # 服务器连接配置
  server:
    host: localhost           # Snail-Job 服务器地址
    port: 1788                # RPC 通信端口（非 Web 控制台端口）
    namespace: dev            # 命名空间（隔离不同环境）
    group-name: smart-admin   # 分组名称（对应控制台中的分组）
    token: ""                 # 认证 Token（开发环境可留空）

  # 客户端配置
  client:
    host: localhost           # 客户端地址（自动检测可省略）
    port: 1789                # 客户端 RPC 端口

  # 日志配置
  logging:
    config: classpath:logback-spring.xml
```

**配置说明**:
- `enabled: true` - 启用 Snail-Job 集成
- `server.port: 1788` - RPC 通信端口，**不是** Web 控制台端口（8082）
- `namespace: dev` - 对应控制台中的命名空间 unique_id
- `token: ""` - 开发环境无需 Token，生产环境必须配置

---

### 步骤 4: 创建第一个定时任务

在 SmartAdmin 项目中创建任务执行器类：

**文件路径**: `smart-admin-api-java21-springboot3/sa-admin/src/main/java/net/lab1024/sa/admin/module/business/job/DataCleanupJob.java`

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据清理定时任务示例
 *
 * CRON: 0 0 2 * * ? (每天凌晨 2 点执行)
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupJob {

    /**
     * 任务执行方法
     *
     * @param params 控制台传入的参数（JSON 格式）
     * @return 执行结果
     */
    @JobExecutor(name = "dataCleanupJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始执行数据清理任务，参数: {}", params);

            // 业务逻辑：清理 30 天前的日志
            int deletedCount = cleanupOldLogs(30);

            String result = String.format("数据清理完成，清理了 %d 条记录", deletedCount);
            log.info(result);

            // 返回成功结果
            return ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("数据清理任务执行失败", e);
            // 返回失败结果（会触发重试机制）
            return ExecuteResult.fail("清理失败: " + e.getMessage());
        }
    }

    /**
     * 清理旧日志（示例实现）
     */
    private int cleanupOldLogs(int retentionDays) {
        // 实际实现中，这里应该调用 DAO 层删除旧数据
        // 示例中直接返回模拟数据

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        log.info("清理截止日期: {}", cutoffDate.format(DateTimeFormatter.ISO_DATE_TIME));

        // 模拟删除操作
        return 150; // 返回模拟的删除记录数
    }
}
```

**关键注解说明**:
- `@Component` - Spring Bean，自动注册到容器
- `@JobExecutor(name = "dataCleanupJob")` - Snail-Job 任务执行器标记
- `ExecuteResult.success(String)` - 返回成功结果
- `ExecuteResult.fail(String)` - 返回失败结果（触发重试）

---

### 步骤 5: 启动应用

在 IDE 中启动 SmartAdmin 应用，或使用命令行：

```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:bootRun
```

**观察启动日志**，确认以下信息：

```
INFO  n.l.s.b.f.taskscheduler.config.SnailJobAutoConfiguration - Initializing Snail-Job: namespace=dev, group=smart-admin
INFO  c.a.snailjob.client.starter.SnailJobClient - Snail-Job client started successfully
INFO  c.a.snailjob.client.starter.SnailJobClient - Registered executor: dataCleanupJob
```

**如果看到以上日志，说明应用已成功连接 Snail-Job 服务器！**

---

### 步骤 6: 在控制台配置任务

**回到 Snail-Job 控制台**（http://localhost:8082）

#### 6.1 创建定时任务

1. 导航到 **任务管理** → **定时任务**
2. 点击右上角 **新增任务** 按钮
3. 填写任务配置：

| 字段 | 值 | 说明 |
|------|-----|------|
| 任务名称 | 数据清理任务 | 任务显示名称 |
| 执行器名称 | dataCleanupJob | 对应 @JobExecutor(name) |
| 命名空间 | dev | 选择开发环境 |
| 分组名称 | smart-admin | 选择应用分组 |
| 调度类型 | CRON | CRON 表达式调度 |
| CRON 表达式 | `0 0 2 * * ?` | 每天凌晨 2 点执行 |
| 执行模式 | 单机执行 | 集群中只有一个节点执行 |
| 阻塞策略 | 丢弃后续任务 | 如果上次任务未完成，丢弃本次 |
| 任务参数 | `{}` | JSON 格式参数（可选） |
| 任务描述 | 每天凌晨清理 30 天前的日志数据 | 备注信息 |

#### 6.2 CRON 表达式快速参考

| 表达式 | 说明 |
|--------|------|
| `0 0 2 * * ?` | 每天凌晨 2 点 |
| `0 0 8 * * ?` | 每天早上 8 点 |
| `0 0 * * * ?` | 每小时整点 |
| `0 */5 * * * ?` | 每 5 分钟 |
| `0 0 0 * * MON` | 每周一凌晨 |

#### 6.3 保存并启用任务

1. 点击 **保存**
2. 在任务列表中找到刚创建的任务
3. 点击 **启用** 按钮

---

### 步骤 7: 测试任务执行

#### 手动触发任务

为了快速验证，不必等到凌晨 2 点：

1. 在任务列表找到 "数据清理任务"
2. 点击 **操作** → **立即执行**
3. 观察任务状态变为 **执行中**

#### 查看执行日志

1. 点击 **操作** → **执行日志**
2. 在日志列表中可以看到：
   - 执行时间
   - 执行状态（成功/失败）
   - 执行耗时
   - 返回结果：`数据清理完成，清理了 150 条记录`

#### 查看应用日志

在 SmartAdmin 应用的控制台日志中，您会看到：

```
INFO  n.l.s.a.a.m.b.job.DataCleanupJob - 开始执行数据清理任务，参数: {}
INFO  n.l.s.a.a.m.b.job.DataCleanupJob - 清理截止日期: 2025-12-23T02:00:00
INFO  n.l.s.a.a.m.b.job.DataCleanupJob - 数据清理完成，清理了 150 条记录
```

---

## ✅ 验证成功

如果您看到以下内容，说明集成成功：

✅ **Snail-Job 服务器启动**
```bash
docker compose ps snail-job-server
# 状态应为 "running"
```

✅ **应用成功连接**
```
应用日志显示: "Snail-Job client started successfully"
```

✅ **任务执行成功**
```
控制台执行日志状态: 成功
应用日志显示: "数据清理完成，清理了 150 条记录"
```

✅ **控制台可访问**
```
浏览器访问 http://localhost:8082 正常显示
```

---

## 🎉 恭喜！

您已成功完成 Snail-Job 的快速集成！

---

## 📚 下一步

### 🔧 配置优化
- [配置参考手册](./02-configuration.md) - 详细配置说明
- [监控运维指南](./06-monitoring.md) - 生产环境监控

### 💻 深入使用
- [API 使用示例](./03-api-usage.md) - 更多代码示例
  - 带参数的定时任务
  - 分布式重试机制
  - DAG 工作流编排

### 🔗 高级特性
- [DAG 工作流](./08-dag-workflows.md) - 复杂任务编排
- [架构设计](./05-architecture.md) - 深入了解原理

### 🐛 遇到问题？
- [故障排查手册](./04-troubleshooting.md) - 常见问题解决

---

## 💡 小贴士

### 开发环境最佳实践

1. **使用短周期任务测试**
   ```
   CRON: 0 */1 * * * ?  （每分钟执行）
   测试验证后再改为实际周期
   ```

2. **查看实时日志**
   ```bash
   # Snail-Job 服务器日志
   docker logs -f sa21-snail-job-server

   # SmartAdmin 应用日志
   tail -f logs/smart-admin.log
   ```

3. **任务参数使用 JSON 格式**
   ```json
   {
     "retentionDays": 30,
     "batchSize": 1000,
     "enableNotification": true
   }
   ```

4. **启用任务前先手动执行**
   - 确保任务逻辑正确
   - 验证参数解析正常
   - 检查异常处理

---

## 🆘 常见问题

### Q1: 应用启动时连接失败

**现象**:
```
ERROR - Failed to connect to Snail-Job server: Connection refused
```

**解决方案**:
1. 检查 Snail-Job 服务器是否启动：`docker compose ps snail-job-server`
2. 检查配置中的 `server.port` 是否为 `1788`（不是 8082）
3. 检查防火墙是否阻止端口 1788

### Q2: 任务未在控制台中显示

**现象**: 执行器列表为空

**解决方案**:
1. 确认应用已启动并成功连接
2. 检查 `@JobExecutor` 注解的 name 属性
3. 确认类已添加 `@Component` 注解
4. 在控制台刷新页面

### Q3: 任务执行失败

**现象**: 执行日志显示失败状态

**解决方案**:
1. 查看应用日志中的异常堆栈
2. 检查业务逻辑中的异常处理
3. 验证任务参数格式是否正确
4. 确认数据库连接等依赖服务正常

---

**更多问题？** 请参阅 [故障排查手册](./04-troubleshooting.md)
