# DAG 工作流编排

> 使用 Snail-Job 实现复杂的任务依赖编排

## 📋 目录

- [DAG 概念](#dag-概念)
- [工作流设计模式](#工作流设计模式)
- [实战示例](#实战示例)
- [依赖管理](#依赖管理)
- [错误处理](#错误处理)

---

## DAG 概念

### 什么是 DAG？

**DAG** (Directed Acyclic Graph，有向无环图) 是一种任务编排模式，允许定义任务之间的依赖关系。

```
    A
   / \
  B   C
   \ /
    D
```

**特点**:
- **有向**: 任务有明确的执行顺序
- **无环**: 不会形成循环依赖
- **并行**: 无依赖的任务可以并行执行

### 为什么需要 DAG？

**传统串行执行**:
```
Task A (5s) → Task B (10s) → Task C (8s) → Task D (3s)
总耗时: 26 秒
```

**DAG 并行执行**:
```
         Task A (5s)
            ↓
    ┌───────┴────────┐
Task B (10s)      Task C (8s)
    └───────┬────────┘
         Task D (3s)

总耗时: 5s + 10s + 3s = 18 秒 (节省 31%)
```

---

## 工作流设计模式

### 1. 线性工作流

**适用场景**: 任务必须严格按顺序执行

```
A → B → C → D
```

**示例**: 用户注册流程
```
创建用户 → 发送验证邮件 → 初始化用户设置 → 发送欢迎通知
```

---

### 2. 并行工作流

**适用场景**: 任务之间无依赖，可以同时执行

```
   ┌─→ B ─┐
A ─┼─→ C ─┼→ E
   └─→ D ─┘
```

**示例**: 数据聚合
```
      ┌─→ 查询用户数据 ─┐
开始 ─┼─→ 查询订单数据 ─┼→ 生成报告
      └─→ 查询日志数据 ─┘
```

---

### 3. 分支工作流

**适用场景**: 根据条件选择不同的执行路径

```
        A
        ↓
    [条件判断]
    ↙        ↘
   B          C
```

**示例**: 订单处理
```
        订单创建
           ↓
    [支付方式判断]
    ↙            ↘
支付宝处理    微信支付处理
    ↓            ↓
       订单完成通知
```

---

### 4. 聚合工作流

**适用场景**: 多个并行任务完成后，汇总结果

```
   ┌─→ B ─┐
A ─┼─→ C ─┼→ E (等待 B、C、D 全部完成)
   └─→ D ─┘
```

**示例**: 数据统计
```
      ┌─→ 统计今日订单 ─┐
开始 ─┼─→ 统计今日用户 ─┼→ 生成日报（汇总数据）
      └─→ 统计今日销售 ─┘
```

---

### 5. MapReduce 工作流

**适用场景**: 大数据量分片处理后聚合

```
        Map
    ┌────┼────┐
    A    B    C
    └────┼────┘
       Reduce
```

**示例**: 大文件处理
```
      分片任务调度
    ┌────┼────┐
处理分片1 分片2 分片3
    └────┼────┘
      结果聚合
```

---

## 实战示例

### 示例 1: ETL 数据处理流水线

#### 业务需求

```
每天凌晨 3 点执行 ETL 流程:
1. 从多个数据源提取数据
2. 数据清洗和转换
3. 数据验证
4. 加载到目标数据库
5. 生成处理报告
```

#### DAG 设计

```
                数据提取任务
                   (5 分钟)
                      ↓
                数据清洗任务
                   (10 分钟)
                      ↓
        ┌─────────────┴─────────────┐
        ↓                           ↓
  数据转换任务                  数据验证任务
   (8 分钟)                     (6 分钟)
        └─────────────┬─────────────┘
                      ↓
                数据加载任务
                   (3 分钟)
                      ↓
            发送完成通知任务
                   (1 分钟)

总耗时: 5 + 10 + 8 + 3 + 1 = 27 分钟
```

#### 任务实现

**1. 数据提取任务**

```java
package net.lab1024.sa.admin.module.business.etl.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataExtractJob {

    private final DataSourceService dataSourceService;

    @JobExecutor(name = "dataExtractJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始提取数据...");

            // 从多个数据源提取数据
            int extractedCount = dataSourceService.extractFromSources();

            log.info("数据提取完成，提取 {} 条记录", extractedCount);
            return ExecuteResult.success("提取完成: " + extractedCount + " 条");

        } catch (Exception e) {
            log.error("数据提取失败", e);
            return ExecuteResult.fail("提取失败: " + e.getMessage());
        }
    }
}
```

**2. 数据清洗任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanJob {

    private final DataCleanService dataCleanService;

    @JobExecutor(name = "dataCleanJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始清洗数据...");

            // 数据清洗（去重、补全、格式化）
            int cleanedCount = dataCleanService.cleanExtractedData();

            log.info("数据清洗完成，处理 {} 条记录", cleanedCount);
            return ExecuteResult.success("清洗完成: " + cleanedCount + " 条");

        } catch (Exception e) {
            log.error("数据清洗失败", e);
            return ExecuteResult.fail("清洗失败: " + e.getMessage());
        }
    }
}
```

**3. 数据转换任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DataTransformJob {

    private final DataTransformService dataTransformService;

    @JobExecutor(name = "dataTransformJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始转换数据...");

            // 数据转换（字段映射、类型转换、业务规则应用）
            int transformedCount = dataTransformService.transformData();

            log.info("数据转换完成，处理 {} 条记录", transformedCount);
            return ExecuteResult.success("转换完成: " + transformedCount + " 条");

        } catch (Exception e) {
            log.error("数据转换失败", e);
            return ExecuteResult.fail("转换失败: " + e.getMessage());
        }
    }
}
```

**4. 数据验证任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DataValidateJob {

    private final DataValidationService validationService;

    @JobExecutor(name = "dataValidateJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始验证数据...");

            // 数据验证（完整性检查、业务规则验证）
            ValidationResult result = validationService.validateData();

            if (!result.isValid()) {
                log.error("数据验证失败: {}", result.getErrorMessage());
                return ExecuteResult.fail("验证失败: " + result.getErrorMessage());
            }

            log.info("数据验证通过");
            return ExecuteResult.success("验证通过");

        } catch (Exception e) {
            log.error("数据验证异常", e);
            return ExecuteResult.fail("验证异常: " + e.getMessage());
        }
    }
}
```

**5. 数据加载任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoadJob {

    private final DataLoadService dataLoadService;

    @JobExecutor(name = "dataLoadJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("开始加载数据到目标库...");

            // 批量加载数据
            int loadedCount = dataLoadService.loadToTarget();

            log.info("数据加载完成，加载 {} 条记录", loadedCount);
            return ExecuteResult.success("加载完成: " + loadedCount + " 条");

        } catch (Exception e) {
            log.error("数据加载失败", e);
            return ExecuteResult.fail("加载失败: " + e.getMessage());
        }
    }
}
```

**6. 完成通知任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class EtlNotifyJob {

    private final NotificationService notificationService;

    @JobExecutor(name = "etlNotifyJob")
    public ExecuteResult execute(String params) {
        try {
            log.info("发送 ETL 完成通知...");

            // 发送邮件通知
            notificationService.sendEtlCompletionEmail();

            log.info("通知已发送");
            return ExecuteResult.success("通知已发送");

        } catch (Exception e) {
            log.error("发送通知失败", e);
            // 通知失败不影响整体流程
            return ExecuteResult.success("通知发送失败，但 ETL 流程已完成");
        }
    }
}
```

#### 控制台配置

**步骤 1: 创建工作流**

1. 访问 http://localhost:8082
2. **工作流管理** → **DAG 工作流** → **新增工作流**
3. 工作流名称: `ETL 数据处理流水线`
4. 调度类型: CRON
5. CRON 表达式: `0 0 3 * * ?` (每天凌晨 3 点)

**步骤 2: 配置任务节点**

| 节点 ID | 任务名称 | 执行器 | 依赖节点 |
|---------|---------|--------|----------|
| node-1 | 数据提取 | dataExtractJob | 无 |
| node-2 | 数据清洗 | dataCleanJob | node-1 |
| node-3 | 数据转换 | dataTransformJob | node-2 |
| node-4 | 数据验证 | dataValidateJob | node-2 |
| node-5 | 数据加载 | dataLoadJob | node-3, node-4 |
| node-6 | 完成通知 | etlNotifyJob | node-5 |

**步骤 3: 保存并启用**

---

### 示例 2: 订单处理工作流

#### 业务需求

```
订单创建后:
1. 检查库存
2. 检查用户信用
3. 如果库存和信用都通过，则创建订单
4. 发送确认邮件
5. 更新库存
```

#### DAG 设计

```
         订单创建
            ↓
     ┌──────┴──────┐
     ↓             ↓
 检查库存      检查信用
     └──────┬──────┘
            ↓
       [两者都通过?]
            ↓ Yes
       创建订单
            ↓
     ┌──────┴──────┐
     ↓             ↓
 发送确认邮件   更新库存
```

#### 任务实现

**检查库存任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckInventoryJob {

    private final InventoryService inventoryService;

    @JobExecutor(name = "checkInventoryJob")
    public ExecuteResult execute(String params) {
        try {
            OrderParams order = JSONUtil.toBean(params, OrderParams.class);
            log.info("检查库存: productId={}, quantity={}",
                order.getProductId(), order.getQuantity());

            boolean hasStock = inventoryService.checkStock(
                order.getProductId(),
                order.getQuantity()
            );

            if (!hasStock) {
                return ExecuteResult.fail("库存不足");
            }

            return ExecuteResult.success("库存充足");

        } catch (Exception e) {
            log.error("检查库存失败", e);
            return ExecuteResult.fail("检查失败: " + e.getMessage());
        }
    }
}
```

**检查信用任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckCreditJob {

    private final CreditService creditService;

    @JobExecutor(name = "checkCreditJob")
    public ExecuteResult execute(String params) {
        try {
            OrderParams order = JSONUtil.toBean(params, OrderParams.class);
            log.info("检查用户信用: userId={}, amount={}",
                order.getUserId(), order.getAmount());

            boolean hasCredit = creditService.checkCredit(
                order.getUserId(),
                order.getAmount()
            );

            if (!hasCredit) {
                return ExecuteResult.fail("信用额度不足");
            }

            return ExecuteResult.success("信用额度充足");

        } catch (Exception e) {
            log.error("检查信用失败", e);
            return ExecuteResult.fail("检查失败: " + e.getMessage());
        }
    }
}
```

---

## 依赖管理

### 依赖类型

#### 1. 串行依赖

```
A → B → C
```

**特点**: 任务必须等待前一个任务完成

**配置**:
```
节点 B 依赖: [节点 A]
节点 C 依赖: [节点 B]
```

---

#### 2. 并行依赖

```
   ┌→ B ┐
A ─┼→ C ┼→ D
   └→ E ┘
```

**特点**: 任务 D 必须等待 B、C、E 全部完成

**配置**:
```
节点 B 依赖: [节点 A]
节点 C 依赖: [节点 A]
节点 E 依赖: [节点 A]
节点 D 依赖: [节点 B, 节点 C, 节点 E]
```

---

#### 3. 条件依赖

```
    A
    ↓
[条件判断]
  ↙  ↘
 B    C
```

**特点**: 根据前一个任务的结果选择执行路径

**实现**:
```java
@JobExecutor(name = "conditionJob")
public ExecuteResult execute(String params) {
    // 返回结果中包含下一步信息
    if (condition) {
        return ExecuteResult.success("NEXT:taskB");
    } else {
        return ExecuteResult.success("NEXT:taskC");
    }
}
```

---

### 依赖配置最佳实践

#### 1. 避免循环依赖

```
❌ 错误示例:
A → B → C → A  (形成循环)

✅ 正确示例:
A → B → C → D  (无循环)
```

#### 2. 合理设置并行度

```
考虑因素:
- 系统资源限制（CPU、内存）
- 数据库连接池大小
- 下游服务承受能力

建议:
- 单个工作流并行任务数 ≤ 10
- CPU 密集型任务: 并行数 = CPU 核心数
- IO 密集型任务: 并行数 = CPU 核心数 × 2
```

#### 3. 设置合理的超时时间

```yaml
# 控制台配置
任务超时时间:
- 快速任务（查询）: 30 秒
- 中等任务（数据处理）: 5 分钟
- 长时间任务（ETL）: 30 分钟
```

---

## 错误处理

### 错误处理策略

#### 1. 失败重试

```java
@JobExecutor(
    name = "etlJob",
    maxRetries = 3,           // 最多重试 3 次
    retryInterval = 60000     // 重试间隔 60 秒
)
public ExecuteResult execute(String params) {
    // 任务逻辑
}
```

**控制台配置**:
- 任务编辑 → 高级配置 → 重试策略
- 最大重试次数: 3
- 重试间隔: 60 秒
- 退避策略: 指数退避

---

#### 2. 失败跳过

```
    A
    ↓
    B (失败)
    ↓
    C (跳过 B 的失败，继续执行)
```

**配置**:
- 任务 C 配置 → 依赖节点失败处理: `继续执行`

**适用场景**:
- 非关键任务失败
- 部分数据处理失败不影响整体

---

#### 3. 失败回滚

```
    A (成功)
    ↓
    B (成功)
    ↓
    C (失败)
    ↓
回滚 B、回滚 A
```

**实现**:
```java
@JobExecutor(name = "rollbackableJob")
public ExecuteResult execute(String params) {
    try {
        // 执行业务逻辑
        processData();

        // 记录回滚信息
        saveRollbackInfo();

        return ExecuteResult.success("完成");

    } catch (Exception e) {
        // 执行回滚
        rollback();
        return ExecuteResult.fail("失败，已回滚");
    }
}

private void rollback() {
    // 回滚逻辑
}
```

---

#### 4. 失败告警

**配置告警规则**:
```yaml
alerts:
  - name: ETL 工作流失败
    type: WORKFLOW_FAILED
    conditions:
      - workflow_name: "ETL 数据处理流水线"
      - failure_count >= 1
    channels:
      - email
      - dingtalk
    recipients:
      - ops-team@example.com
```

---

### 异常处理最佳实践

#### 1. 分类异常处理

```java
@JobExecutor(name = "robustJob")
public ExecuteResult execute(String params) {
    try {
        processData();
        return ExecuteResult.success("完成");

    } catch (BusinessException e) {
        // 业务异常：不重试，直接失败
        log.error("业务异常", e);
        return ExecuteResult.fail("业务异常: " + e.getMessage());

    } catch (NetworkException e) {
        // 网络异常：可重试
        log.error("网络异常", e);
        throw e;  // 抛出异常触发重试

    } catch (Exception e) {
        // 未知异常：记录详细日志，不重试
        log.error("未知异常", e);
        return ExecuteResult.fail("系统异常: " + e.getMessage());
    }
}
```

#### 2. 幂等性保证

```java
@JobExecutor(name = "idempotentJob")
public ExecuteResult execute(String params) {
    String bizId = extractBizId(params);

    // 检查是否已处理
    if (isAlreadyProcessed(bizId)) {
        log.info("任务已处理，跳过: {}", bizId);
        return ExecuteResult.success("已处理");
    }

    try {
        // 执行业务逻辑
        processData(bizId);

        // 标记已处理
        markAsProcessed(bizId);

        return ExecuteResult.success("完成");

    } catch (Exception e) {
        log.error("处理失败", e);
        return ExecuteResult.fail(e.getMessage());
    }
}
```

#### 3. 日志与监控

```java
@JobExecutor(name = "monitoredJob")
public ExecuteResult execute(String params) {
    long startTime = System.currentTimeMillis();

    try {
        log.info("任务开始: params={}", params);

        // 执行业务逻辑
        String result = processData();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("任务完成: elapsed={}ms, result={}", elapsed, result);

        // 记录指标
        recordMetrics("job.success", elapsed);

        return ExecuteResult.success(result);

    } catch (Exception e) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.error("任务失败: elapsed={}ms", elapsed, e);

        // 记录指标
        recordMetrics("job.failure", elapsed);

        // 发送告警
        sendAlert("任务失败", e);

        return ExecuteResult.fail(e.getMessage());
    }
}
```

---

## 性能优化

### 1. 减少任务间通信

```
❌ 不推荐:
A (查询数据) → B (接收数据并处理) → C (接收结果并存储)
大量数据在任务间传递

✅ 推荐:
A (查询数据并存储到 Redis) → B (从 Redis 读取并处理) → C (从 Redis 读取结果)
使用共享存储
```

### 2. 合理设置并行度

```
并行度计算:
- CPU 密集型: 核心数
- IO 密集型: 核心数 × 2
- 混合型: 核心数 × 1.5

示例:
8 核 CPU:
- CPU 密集型任务: 8 个并行
- IO 密集型任务: 16 个并行
```

### 3. 使用分片处理大数据

```
MapReduce 模式:
1000 万条数据 → 分 100 片 → 并行处理 → 聚合结果
总耗时: 单机 100 分钟 → 并行 10 分钟（10 倍加速）
```

---

## 下一步

- [API 使用示例](./03-api-usage.md) - 更多代码示例
- [架构设计](./05-architecture.md) - 深入了解原理
- [监控运维](./06-monitoring.md) - 生产环境监控

---

**DAG 工作流编排完成！** 掌握工作流编排，轻松处理复杂业务流程。
