---
name: scheduled-task-manager
description: [P2 - Productivity] Generate XXL-Job/Snail-Job integration for SmartAdmin applications with job handlers, cron expressions, job parameters, monitoring, retry logic, and job dependency management. Use when implementing scheduled tasks, batch processing, or automated workflows. Triggers when user mentions "scheduled task", "cron job", "XXL-Job", "Snail-Job", "batch processing", "scheduled report", or "periodic task".
---

# Scheduled Task Manager

**Priority:** P1 - Roadmap Priority #3
**Sprint:** 3 (Weeks 9-11)
**Status:** ✅ Production Ready

## Purpose

Enable automated workflows by generating XXL-Job/Snail-Job integration. Replaces manual cron/Timer tasks with distributed scheduled jobs.

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "scheduled task" - Scheduled job creation
- "cron job" - Cron-based job scheduling
- "XXL-Job" - XXL-Job distributed scheduling
- "Snail-Job" - Snail-Job distributed scheduling
- "batch processing" - Batch job processing

**Secondary Keywords** (Medium confidence):
- "distributed job" - Context: distributed job scheduling
- "job monitoring" - Context: scheduled job monitoring
- "retry handling" - Context: job failure retry
- "job dependency" - Context: job dependency management

**Phrase Patterns**:
- "Create scheduled task for [operation]" - Example: "Create scheduled task for daily report generation"
- "Add cron job to [action]" - Example: "Add cron job to clean up expired data"
- "Setup [XXL-Job/Snail-Job] for [task]" - Example: "Setup XXL-Job for user synchronization"

**Example User Requests**:
```
User: "Create scheduled task for daily report generation"
User: "Add cron job to clean up expired sessions every hour"
User: "Setup XXL-Job for user data synchronization"
User: "Implement batch processing for invoice generation"
```

**Note**: This skill can also be manually invoked via `/scheduled-task-manager` command.

## Problem Statement

**User Roadmap Need:** "调度自动化 (XXL-Job/Snail-Job)" (Scheduled automation)

**Current Issues:**
- Manual task scheduling is error-prone
- No distributed job scheduling
- Missing job monitoring and alerting
- No retry and failure handling
- Job dependency management is complex

## Solution Overview

This skill generates:
- ✅ XXL-Job executor configuration (embedded, distributed modes)
- ✅ Snail-Job integration (if preferred over XXL-Job)
- ✅ Job handler code generation (simple, sharding, broadcast, MapReduce)
- ✅ Cron expression builder with human-readable descriptions
- ✅ Job parameter configuration (JSON, key-value)
- ✅ Job monitoring and alerting (failure notifications, SLA tracking)
- ✅ Retry and failure handling (exponential backoff, circuit breaker)
- ✅ Job dependency management (chain execution, conditional triggers)
- ✅ Dynamic job registration (runtime job creation)
- ✅ Job execution logs and audit trail

## Quick Start

**Most common usage:**
```
User: "Create daily user statistics calculation job with retry logic"
```

You will:
1. Generate XXL-Job handler code
2. Configure cron expression (e.g., "0 2 * * *" for 2 AM daily)
3. Add job parameters
4. Implement retry logic
5. Set up monitoring and alerts
6. Add execution logs

## Scope

### Included
- XXL-Job/Snail-Job executor configuration
- Job handler generation (simple, sharding, broadcast)
- Cron expression builder
- Job parameter configuration
- Monitoring and alerting
- Retry and failure handling
- Job dependency chains
- Dynamic job registration
- Execution logs

### Not Included
- XXL-Job admin server deployment
- Custom executor plugins
- Complex workflow orchestration (use LiteFlow for that)

## Integration Points

- Works with `report-generator` for scheduled reports
- Integrates with `db-migration-manager` for scheduled data cleanup
- Uses SmartAdmin's logging infrastructure
- Compatible with all SmartAdmin modules

## Success Criteria

- ✅ Scheduled jobs replace all manual cron/Timer tasks
- ✅ Job execution reliability > 99.9% (with retry mechanisms)
- ✅ Job monitoring and alerting operational
- ✅ Job dependency chains working correctly
- ✅ Execution logs complete and searchable

## Detailed Documentation

### Configuration Guides

1. **[Snail-Job Configuration](references/snail-job-configuration.md)**
   - Snail-Job vs XXL-Job comparison
   - Basic configuration (application.yml)
   - Job handler registration (simple, sharding, broadcast)
   - Workflow jobs (job chains)
   - Retry configuration (global + job-specific)
   - Docker deployment
   - **Lines:** ~650+ lines with 9 patterns

2. **[Cron Expression Guide](references/cron-expression-guide.md)**
   - Cron expression format and syntax
   - Common patterns (daily, weekly, monthly)
   - Special characters (*, ?, -, /, L, W, #)
   - SmartAdmin use cases
   - Cron expression builder (Java)
   - Testing and validation
   - **Lines:** ~500+ lines

3. **[Job Monitoring Patterns](references/job-monitoring-patterns.md)**
   - Execution logging (structured logging)
   - Metrics integration (Micrometer + Prometheus)
   - Grafana dashboard setup
   - Alert rules (Prometheus alerts)
   - Email alerting
   - Dashboard query API
   - **Lines:** ~550+ lines

## Implementation Workflow

### Step 1: Add Dependency (2 minutes)

```gradle
dependencies {
    // Snail-Job client
    implementation 'com.aizuda:snail-job-client-starter:2.1.0'
}
```

### Step 2: Configure Snail-Job (5 minutes)

```yaml
# application.yml
snail-job:
  server:
    host: ${SNAIL_JOB_SERVER:localhost}
    port: ${SNAIL_JOB_PORT:8081}
  namespace: smartadmin-prod
  group: smartadmin-executor-group
  app-name: smartadmin-service
  executor:
    port: 1789
    core-thread-size: 10
    max-thread-size: 50
  retry:
    max-count: 3
    interval: 1000
  log:
    path: ./logs/snail-job
    retention-days: 30
```

### Step 3: Enable Snail-Job (1 minute)

```java
@EnableSnailJob
@SpringBootApplication
public class SmartAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartAdminApplication.class, args);
    }
}
```

### Step 4: Create Job Handler (10 minutes)

```java
package net.lab1024.sa.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatisticsJob {

    private final UserStatisticsService userStatisticsService;

    @JobExecutor(name = "userStatisticsJob")
    public ExecuteResult execute(String param) {
        try {
            log.info("Starting user statistics calculation");
            userStatisticsService.calculateDailyStatistics();
            log.info("User statistics calculation completed");
            return ExecuteResult.success("Statistics calculated successfully");
        } catch (Exception e) {
            log.error("User statistics calculation failed", e);
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

### Step 5: Configure Job in Admin Console (5 minutes)

1. Login to Snail-Job Admin: `http://localhost:8081`
2. Navigate to: **Job Management** → **Add Job**
3. Fill in:
   - **Job Name:** `userStatisticsJob`
   - **Job Handler:** `userStatisticsJob`
   - **Cron Expression:** `0 0 2 * * ?` (2 AM daily)
   - **Description:** "Calculate daily user statistics"
   - **Executor Group:** `smartadmin-executor-group`
4. Save and enable the job

### Step 6: Add Monitoring (10 minutes)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoredJob {

    private final MeterRegistry meterRegistry;
    private final JobExecutionLoggerService loggerService;
    private final BusinessService businessService;

    @JobExecutor(name = "monitoredJob")
    public ExecuteResult execute(String param) {
        Long logId = loggerService.logJobStart("monitoredJob", param);
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            businessService.process();

            sample.stop(Timer.builder("job_execution_duration")
                .tag("job_name", "monitoredJob")
                .tag("status", "success")
                .register(meterRegistry));

            loggerService.logJobSuccess(logId, "Processing completed");
            return ExecuteResult.success("Success");

        } catch (Exception e) {
            sample.stop(Timer.builder("job_execution_duration")
                .tag("job_name", "monitoredJob")
                .tag("status", "failure")
                .register(meterRegistry));

            loggerService.logJobFailure(logId, e.getMessage());
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

### Step 7: Test Job Execution (5 minutes)

```bash
# Check executor registration
curl http://localhost:8081/api/executor/list

# Trigger job manually (admin console)
# Navigate to: Job Management → Select job → Execute

# Check execution logs
tail -f logs/snail-job/executor.log
```

### Step 8: Set Up Alerts (5 minutes)

```java
@Service
@RequiredArgsConstructor
public class AlertedJob {

    private final JobAlertService alertService;

    @JobExecutor(name = "alertedJob")
    public ExecuteResult execute(String param) {
        try {
            businessService.process();
            return ExecuteResult.success("Success");
        } catch (Exception e) {
            alertService.sendJobFailureAlert("alertedJob", e.getMessage());
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

**Total Time:** ~45 minutes (vs 2-3 days manual implementation)

## Troubleshooting Guide

### Issue: Job not executing

**Solution:** Check job configuration in admin console
1. Verify cron expression is correct
2. Check if job is enabled
3. Verify executor is online

### Issue: Job handler not found

**Solution:** Ensure handler name matches
```java
@JobExecutor(name = "userStatisticsJob")  // Must match admin console
```

### Issue: Job execution timeout

**Solution:** Increase timeout or optimize job logic
```yaml
snail-job:
  executor:
    timeout: 300  # 5 minutes
```

### Issue: High memory usage

**Solution:** Adjust thread pool size
```yaml
snail-job:
  executor:
    core-thread-size: 5
    max-thread-size: 20
```

## Performance Impact

**Expected Improvements:**
- Development time: 2-3 days → 45 minutes (96% reduction)
- Job execution reliability: > 99.9% (with retry mechanisms)
- Job monitoring: Real-time execution tracking
- Distributed scheduling: Horizontal scaling support

**Resource Requirements:**
- Snail-Job Server: 2GB RAM, 2 CPU cores
- Executor: 512MB RAM per executor
- Database: MySQL 8.0+ (for job metadata)

---

## 相關規則

本技能直接關聯以下 SmartAdmin 規範與 Snail-Job 排程任務實踐：

### 強制要求

- **[Architecture Rules - Manager Layer](./../../../.agent/rules/foundation/10-architecture-rules.md)**
  - 排程任務邏輯放置在 Manager 層（跨表事務、複雜業務邏輯）
  - XXL-Job Handler 調用 Manager.executeTask() 方法
  - 構造器注入（@RequiredArgsConstructor + private final）

- **[Manager Layer Rules](./../../../.agent/rules/foundation/09-manager-layer.md)**
  - 排程任務 Manager 方法必須包含 @Transactional(rollbackFor = Throwable.class)
  - 任務執行失敗使用異常回滾（不使用返回碼）
  - 任務執行歷史記錄（審計日誌）

### 參考指引

- **[Exception Handling](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - 排程任務異常處理（捕獲異常 + 日誌記錄 + 重試策略）
  - XXL-Job Handler 返回值：XxlJobHelper.SUCCESS / XxlJobHelper.FAIL
  - 異常通知機制（郵件、Webhook）

- **[Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md)**
  - Handler 命名：DataSyncJobHandler, ReportGenerationJobHandler
  - Manager 命名：DataSyncManager, ReportManager

### 相關技能

- **[liteflow-rule-builder](./../../../extended/domain/liteflow-rule-builder/SKILL.md)** - 定時執行 LiteFlow 規則鏈（XXL-Job 觸發工作流）
- **[report-generator](./../../../productivity/integration/report-generator/SKILL.md)** - 定時生成報表任務（排程導出）
- **[smartadmin-integration-test](./../../../foundation/full-stack/smartadmin-integration-test/SKILL.md)** - 排程任務整合測試（Testcontainers）

---

**Version:** 1.0.0
**Created:** 2026-01-26
**Sprint:** 3 (Weeks 9-11)
**Status:** ✅ Production Ready
**Last Updated:** 2026-01-26
