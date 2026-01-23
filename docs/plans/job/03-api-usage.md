# Snail-Job API 使用示例

> 完整的代码示例，包含定时任务、分布式重试、DAG 工作流等场景

## 📋 目录

- [定时任务（CRON）](#定时任务cron)
- [分布式重试](#分布式重试)
- [DAG 工作流](#dag-工作流)
- [任务监听器与钩子](#任务监听器与钩子)
- [任务管理 API](#任务管理-api)

---

## 定时任务（CRON）

### 基础定时任务

**场景**: 每天早上 8 点生成日报

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.report.service.ReportService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日报生成定时任务
 *
 * CRON: 0 0 8 * * ? (每天早上 8 点)
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportJob {

    private final ReportService reportService;

    @JobExecutor(name = "dailyReportJob")
    public ExecuteResult execute(String params) {
        try {
            LocalDate reportDate = LocalDate.now().minusDays(1);
            log.info("开始生成日报: {}", reportDate);

            // 生成报表
            String reportUrl = reportService.generateDailyReport(reportDate);

            String result = String.format("日报生成成功: %s, URL: %s",
                reportDate.format(DateTimeFormatter.ISO_DATE),
                reportUrl);

            log.info(result);
            return ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("日报生成失败", e);
            return ExecuteResult.fail("生成失败: " + e.getMessage());
        }
    }
}
```

**控制台配置**:
- 任务名称: 日报生成任务
- 执行器: `dailyReportJob`
- CRON: `0 0 8 * * ?`
- 执行模式: 单机执行

---

### 带参数的定时任务

**场景**: 批量发送邮件通知

```java
package net.lab1024.sa.admin.module.business.job;

import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.support.mail.service.MailService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 邮件通知定时任务
 *
 * CRON: 0 0 9 * * ? (每天早上 9 点)
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationJob {

    private final MailService mailService;

    @JobExecutor(name = "emailNotificationJob")
    public ExecuteResult execute(String params) {
        try {
            // 解析 JSON 参数
            EmailParams emailParams = JSONUtil.toBean(params, EmailParams.class);

            log.info("开始发送邮件: 主题={}, 收件人数={}",
                emailParams.getSubject(),
                emailParams.getRecipients().size());

            // 批量发送邮件
            int successCount = 0;
            int failCount = 0;

            for (String recipient : emailParams.getRecipients()) {
                try {
                    mailService.sendMail(
                        recipient,
                        emailParams.getSubject(),
                        emailParams.getContent()
                    );
                    successCount++;
                } catch (Exception e) {
                    log.error("发送邮件失败: {}", recipient, e);
                    failCount++;
                }
            }

            String result = String.format("邮件发送完成: 成功=%d, 失败=%d",
                successCount, failCount);

            log.info(result);
            return ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("邮件任务执行失败", e);
            return ExecuteResult.fail("执行失败: " + e.getMessage());
        }
    }

    @Data
    public static class EmailParams {
        private List<String> recipients;  // 收件人列表
        private String subject;            // 邮件主题
        private String content;            // 邮件内容
        private Boolean highPriority;      // 是否高优先级
    }
}
```

**控制台参数示例**:
```json
{
  "recipients": [
    "user1@example.com",
    "user2@example.com",
    "user3@example.com"
  ],
  "subject": "SmartAdmin 每日数据报告",
  "content": "<h1>今日数据统计</h1><p>详细内容请查看附件...</p>",
  "highPriority": true
}
```

---

### 高级：动态参数任务

**场景**: 数据同步任务，支持增量/全量模式

```java
package net.lab1024.sa.admin.module.business.job;

import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.sync.service.DataSyncService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据同步定时任务
 *
 * 支持增量和全量同步模式
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncJob {

    private final DataSyncService dataSyncService;

    @JobExecutor(name = "dataSyncJob")
    public ExecuteResult execute(String params) {
        try {
            SyncParams syncParams = JSONUtil.toBean(params, SyncParams.class);

            log.info("开始数据同步: 模式={}, 数据源={}",
                syncParams.getMode(),
                syncParams.getSourceType());

            long startTime = System.currentTimeMillis();
            int syncedCount;

            // 根据模式执行不同的同步策略
            if ("full".equalsIgnoreCase(syncParams.getMode())) {
                // 全量同步
                syncedCount = dataSyncService.fullSync(
                    syncParams.getSourceType(),
                    syncParams.getBatchSize()
                );
            } else {
                // 增量同步（默认）
                LocalDateTime lastSyncTime = syncParams.getLastSyncTime() != null
                    ? syncParams.getLastSyncTime()
                    : LocalDateTime.now().minusHours(1);

                syncedCount = dataSyncService.incrementalSync(
                    syncParams.getSourceType(),
                    lastSyncTime,
                    syncParams.getBatchSize()
                );
            }

            long elapsed = System.currentTimeMillis() - startTime;
            String result = String.format("数据同步完成: 模式=%s, 同步数量=%d, 耗时=%dms",
                syncParams.getMode(), syncedCount, elapsed);

            log.info(result);
            return ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("数据同步失败", e);
            return ExecuteResult.fail("同步失败: " + e.getMessage());
        }
    }

    @Data
    public static class SyncParams {
        private String mode = "incremental";  // full: 全量, incremental: 增量
        private String sourceType;            // 数据源类型
        private Integer batchSize = 1000;     // 批次大小
        private LocalDateTime lastSyncTime;   // 最后同步时间
    }
}
```

**增量同步参数**:
```json
{
  "mode": "incremental",
  "sourceType": "user_data",
  "batchSize": 1000
}
```

**全量同步参数**:
```json
{
  "mode": "full",
  "sourceType": "user_data",
  "batchSize": 5000
}
```

---

## 分布式重试

### 场景 1: 支付失败重试

**业务场景**: 调用第三方支付接口，网络抖动时自动重试

```java
package net.lab1024.sa.admin.module.business.payment.service;

import com.aizuda.snailjob.client.job.core.annotation.Retryable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.payment.client.PaymentGatewayClient;
import net.lab1024.sa.admin.module.business.payment.domain.entity.OrderEntity;
import net.lab1024.sa.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 支付服务 - 带分布式重试
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGatewayClient paymentGateway;

    /**
     * 处理支付 - 支持自动重试
     *
     * @param orderId 订单 ID
     * @param amount 支付金额
     */
    @Retryable(
        scene = "payment_processing",           // 重试场景标识
        bizNo = "#orderId",                     // 业务 ID（用于幂等）
        retryStrategy = "LOCAL_REMOTE",         // 本地 + 远程重试
        localTimes = 3,                         // 本地重试 3 次
        localInterval = 2                       // 本地重试间隔 2 秒
    )
    public void processPayment(Long orderId, BigDecimal amount) {
        try {
            log.info("处理支付: orderId={}, amount={}", orderId, amount);

            // 调用第三方支付网关（可能失败）
            PaymentResult result = paymentGateway.charge(orderId, amount);

            if (!result.isSuccess()) {
                throw new BusinessException("支付失败: " + result.getErrorMessage());
            }

            log.info("支付成功: orderId={}, transactionId={}",
                orderId, result.getTransactionId());

        } catch (Exception e) {
            log.error("支付处理异常: orderId={}", orderId, e);
            // 抛出异常触发重试机制
            throw new BusinessException("支付处理失败", e);
        }
    }
}
```

**控制台重试配置**:
1. 导航到 **重试管理** → **重试场景配置**
2. 场景标识: `payment_processing`
3. 退避策略: 指数退避（Exponential Backoff）
4. 最大重试次数: 10
5. 初始延迟: 2 秒
6. 最大延迟: 5 分钟
7. 告警阈值: 连续失败 5 次
8. 死信队列: 启用

**重试时间轴**:
```
第 1 次: 立即执行（失败）
第 2 次: 2 秒后（本地重试）
第 3 次: 4 秒后（本地重试）
第 4 次: 8 秒后（远程重试）
第 5 次: 16 秒后（远程重试）
...
第 10 次: 5 分钟后（最大延迟）
```

---

### 场景 2: API 调用重试

**业务场景**: 调用外部 API，支持自定义重试策略

```java
package net.lab1024.sa.admin.module.business.integration.service;

import com.aizuda.snailjob.client.job.core.annotation.Retryable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方 API 集成服务
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyApiService {

    private final RestTemplate restTemplate;

    /**
     * 调用外部 API - 仅远程重试
     *
     * @param apiUrl API 地址
     * @param requestBody 请求体
     * @return API 响应
     */
    @Retryable(
        scene = "api_call_retry",
        retryStrategy = "ONLY_REMOTE",  // 仅使用远程重试（避免阻塞当前线程）
        bizNo = "#apiUrl"
    )
    public String callExternalApi(String apiUrl, Object requestBody) {
        try {
            log.info("调用外部 API: {}", apiUrl);

            // 发送 HTTP 请求
            String response = restTemplate.postForObject(
                apiUrl,
                requestBody,
                String.class
            );

            log.info("API 调用成功: {}", apiUrl);
            return response;

        } catch (RestClientException e) {
            log.error("API 调用失败: {}", apiUrl, e);
            throw e;  // 触发重试
        }
    }

    /**
     * 批量 API 调用 - 带重试限制
     */
    @Retryable(
        scene = "batch_api_call",
        retryStrategy = "LOCAL_REMOTE",
        localTimes = 2,
        localInterval = 1,
        timeout = 30000  // 超时 30 秒
    )
    public void batchCallApi(List<String> apiUrls) {
        for (String apiUrl : apiUrls) {
            try {
                callExternalApi(apiUrl, null);
            } catch (Exception e) {
                log.error("批量调用失败: {}", apiUrl, e);
                // 单个失败不影响其他，但会记录重试
            }
        }
    }
}
```

---

### 场景 3: 消息发送重试

**业务场景**: 发送通知消息，支持多通道重试

```java
package net.lab1024.sa.admin.module.business.notification.service;

import com.aizuda.snailjob.client.job.core.annotation.Retryable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务 - 多通道重试
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * 发送通知 - 带智能重试
     *
     * @param userId 用户 ID
     * @param message 消息内容
     * @param channel 通道（email/sms/push）
     */
    @Retryable(
        scene = "notification_sending",
        bizNo = "#userId + '_' + #channel",
        retryStrategy = "LOCAL_REMOTE",
        localTimes = 2,
        localInterval = 5,
        callback = "onNotificationRetryFailed"  // 重试失败回调
    )
    public void sendNotification(Long userId, String message, String channel) {
        try {
            log.info("发送通知: userId={}, channel={}", userId, channel);

            switch (channel.toLowerCase()) {
                case "email":
                    sendEmail(userId, message);
                    break;
                case "sms":
                    sendSms(userId, message);
                    break;
                case "push":
                    sendPush(userId, message);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的通道: " + channel);
            }

            log.info("通知发送成功: userId={}, channel={}", userId, channel);

        } catch (Exception e) {
            log.error("通知发送失败: userId={}, channel={}", userId, channel, e);
            throw e;  // 触发重试
        }
    }

    /**
     * 重试失败回调
     */
    public void onNotificationRetryFailed(Long userId, String message, String channel) {
        log.error("通知重试全部失败，转入人工处理: userId={}, channel={}", userId, channel);
        // 记录到死信队列或发送告警
    }

    private void sendEmail(Long userId, String message) {
        // 实现邮件发送
    }

    private void sendSms(Long userId, String message) {
        // 实现短信发送
    }

    private void sendPush(Long userId, String message) {
        // 实现推送通知
    }
}
```

---

## DAG 工作流

### 场景: ETL 数据处理流水线

**工作流设计**:
```
                    ┌─────────────┐
                    │ 数据提取任务  │
                    │ (ExtractJob) │
                    └──────┬──────┘
                           │
                           ↓
                    ┌─────────────┐
                    │ 数据清洗任务  │
                    │ (CleanJob)   │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ↓                         ↓
       ┌─────────────┐          ┌─────────────┐
       │ 数据转换任务  │          │ 数据验证任务  │
       │(TransformJob)│          │(ValidateJob) │
       └──────┬──────┘          └──────┬──────┘
              │                         │
              └────────────┬────────────┘
                           ↓
                    ┌─────────────┐
                    │ 数据加载任务  │
                    │  (LoadJob)   │
                    └──────┬──────┘
                           │
                           ↓
                    ┌─────────────┐
                    │ 完成通知任务  │
                    │ (NotifyJob)  │
                    └─────────────┘
```

**任务实现**:

```java
// 1. 数据提取任务
@Component
public class ExtractJob {
    @JobExecutor(name = "extractJob")
    public ExecuteResult execute(String params) {
        log.info("开始提取数据...");
        // 从数据源提取数据
        int extractedCount = extractDataFromSource();
        return ExecuteResult.success("提取完成: " + extractedCount + " 条");
    }
}

// 2. 数据清洗任务
@Component
public class CleanJob {
    @JobExecutor(name = "cleanJob")
    public ExecuteResult execute(String params) {
        log.info("开始清洗数据...");
        // 清洗提取的数据
        int cleanedCount = cleanExtractedData();
        return ExecuteResult.success("清洗完成: " + cleanedCount + " 条");
    }
}

// 3. 数据转换任务
@Component
public class TransformJob {
    @JobExecutor(name = "transformJob")
    public ExecuteResult execute(String params) {
        log.info("开始转换数据...");
        // 转换数据格式
        int transformedCount = transformData();
        return ExecuteResult.success("转换完成: " + transformedCount + " 条");
    }
}

// 4. 数据验证任务
@Component
public class ValidateJob {
    @JobExecutor(name = "validateJob")
    public ExecuteResult execute(String params) {
        log.info("开始验证数据...");
        // 验证数据完整性
        boolean isValid = validateDataIntegrity();
        if (!isValid) {
            return ExecuteResult.fail("数据验证失败");
        }
        return ExecuteResult.success("验证通过");
    }
}

// 5. 数据加载任务
@Component
public class LoadJob {
    @JobExecutor(name = "loadJob")
    public ExecuteResult execute(String params) {
        log.info("开始加载数据到目标库...");
        // 加载数据到目标数据库
        int loadedCount = loadDataToTarget();
        return ExecuteResult.success("加载完成: " + loadedCount + " 条");
    }
}

// 6. 完成通知任务
@Component
public class NotifyJob {
    @JobExecutor(name = "notifyJob")
    public ExecuteResult execute(String params) {
        log.info("ETL流程完成，发送通知...");
        // 发送完成通知
        sendCompletionNotification();
        return ExecuteResult.success("通知已发送");
    }
}
```

**控制台 DAG 配置**:
1. 导航到 **工作流管理** → **DAG 工作流**
2. 创建工作流: "ETL 数据处理流水线"
3. 添加节点并配置依赖关系
4. 设置 CRON: `0 0 3 * * ?` (每天凌晨 3 点)
5. 启用工作流

---

## 任务监听器与钩子

### 全局任务监听器

```java
package net.lab1024.sa.admin.module.business.job.listener;

import com.aizuda.snailjob.client.job.core.dto.JobContext;
import com.aizuda.snailjob.client.job.core.listener.JobExecutionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全局任务执行监听器
 *
 * @author SmartAdmin Team
 */
@Slf4j
@Component
public class GlobalJobListener implements JobExecutionListener {

    @Override
    public void beforeExecution(JobContext context) {
        log.info("任务开始执行: name={}, params={}",
            context.getJobName(),
            context.getJobParams());

        // 记录开始时间（用于统计）
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    @Override
    public void afterExecution(JobContext context, ExecuteResult result) {
        long startTime = (Long) context.getAttribute("startTime");
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("任务执行完成: name={}, status={}, elapsed={}ms",
            context.getJobName(),
            result.isSuccess() ? "SUCCESS" : "FAIL",
            elapsed);

        // 记录到监控系统
        recordMetrics(context.getJobName(), result.isSuccess(), elapsed);
    }

    @Override
    public void onException(JobContext context, Exception exception) {
        log.error("任务执行异常: name={}, error={}",
            context.getJobName(),
            exception.getMessage(),
            exception);

        // 发送告警
        sendAlert(context.getJobName(), exception);
    }

    private void recordMetrics(String jobName, boolean success, long elapsed) {
        // 实现指标记录
    }

    private void sendAlert(String jobName, Exception exception) {
        // 实现告警发送
    }
}
```

---

## 任务管理 API

### 查询任务执行历史

```java
package net.lab1024.sa.admin.module.business.job.controller;

import com.aizuda.snailjob.client.core.SnailJobClient;
import com.aizuda.snailjob.client.model.JobExecutionRecord;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartPageUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 任务管理控制器
 *
 * @author SmartAdmin Team
 */
@RestController
@RequestMapping("/admin/job")
@RequiredArgsConstructor
public class JobManagementController {

    private final SnailJobClient snailJobClient;

    /**
     * 查询任务执行历史
     */
    @GetMapping("/execution/history")
    public ResponseDTO<PageResult<JobExecutionVO>> queryHistory(
        @RequestParam String jobName,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        // 查询执行记录
        List<JobExecutionRecord> records = snailJobClient.queryExecutionHistory(
            jobName, status, pageNum, pageSize
        );

        // 转换为 VO
        PageResult<JobExecutionVO> result = SmartPageUtil.convert(
            records,
            JobExecutionVO.class
        );

        return ResponseDTO.ok(result);
    }

    /**
     * 手动触发任务
     */
    @PostMapping("/trigger")
    public ResponseDTO<String> triggerJob(
        @RequestParam String jobName,
        @RequestParam(required = false) String params
    ) {
        snailJobClient.triggerJob(jobName, params);
        return ResponseDTO.ok("任务已触发");
    }
}
```

---

## 下一步

- [故障排查手册](./04-troubleshooting.md) - 问题诊断
- [DAG 工作流](./08-dag-workflows.md) - 深入工作流编排
- [监控运维](./06-monitoring.md) - 生产环境监控

---

**API 使用示例完成！** 更多示例请参考 [Snail-Job 官方文档](https://snailjob.opensnail.com/)
