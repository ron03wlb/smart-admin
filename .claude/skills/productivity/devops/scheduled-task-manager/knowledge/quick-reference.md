# Scheduled Task Manager - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: scheduled-task-manager (P2 - Productivity/DevOps)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Simple Scheduled Task | @Scheduled annotation | ~3 min |
| Snail Job Integration | Enterprise job scheduling | ~15 min |
| Cron Expression | Configure complex schedules | ~5 min |
| Dynamic Scheduling | Runtime schedule modification | ~12 min |
| Task Monitoring | Track execution status | ~8 min |

---

## Scheduling Solution Matrix

### Option 1: Spring @Scheduled (Simple Tasks)

**Use When**: Simple periodic tasks (<10 tasks)

**Pros**:
- ✅ Zero dependencies
- ✅ Simple annotation-based
- ✅ Good for small projects

**Cons**:
- ⚠️ No distributed coordination
- ⚠️ No task management UI
- ⚠️ No failure retry

**Setup**:
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enable @Scheduled support
}

@Component
@Slf4j
public class DailyReportTask {

    /**
     * Run every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        log.info("Generating daily report...");
        // Task logic
    }

    /**
     * Run every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        log.info("Cleaning up expired sessions...");
    }

    /**
     * Run 10 seconds after previous completion
     */
    @Scheduled(fixedDelay = 10000)
    public void healthCheck() {
        log.info("Performing health check...");
    }
}
```

**Time to Implement**: 3-5 minutes

---

### Option 2: Snail Job (Enterprise Scheduling) - Recommended

**Use When**: Need distributed scheduling, task management, failure retry

**Pros**:
- ✅ Web UI for task management
- ✅ Distributed execution
- ✅ Automatic retry on failure
- ✅ Execution history tracking
- ✅ Supports cron, fixed rate, workflow

**Setup**:

**1. Add Dependency**:
```gradle
dependencies {
    implementation 'com.aizuda:snail-job-client-starter:1.0.0'
}
```

**2. Configuration**:
```yaml
snail-job:
  namespace: smartadmin
  app-name: sa-admin
  server:
    host: localhost
    port: 1788
  retry:
    enabled: true
    max-count: 3
    interval: 60000  # 1 minute
```

**3. Task Implementation**:
```java
@Component
@Slf4j
public class SnailJobTasks {

    /**
     * Snail Job task (managed via Web UI)
     */
    @SnailJobHandler(name = "dailyReportTask")
    public ReturnT<String> dailyReport(String params) {
        try {
            log.info("Executing daily report with params: {}", params);

            // Task logic here
            generateReport();

            return ReturnT.success("Report generated successfully");

        } catch (Exception e) {
            log.error("Failed to generate report", e);
            return ReturnT.fail("Report generation failed: " + e.getMessage());
        }
    }

    /**
     * Cleanup task with retry
     */
    @SnailJobHandler(name = "cleanupExpiredData")
    public ReturnT<String> cleanupExpiredData(String params) {
        try {
            int deletedCount = deleteExpiredRecords();
            return ReturnT.success("Deleted " + deletedCount + " records");
        } catch (Exception e) {
            log.error("Cleanup failed", e);
            return ReturnT.fail(e.getMessage());
        }
    }
}
```

**4. Configure via Web UI**:
- URL: http://localhost:1788
- Add Job → Select Handler → Set Cron → Save

**Time to Setup**: 15-20 minutes

---

### Option 3: Quartz Scheduler (Complex Workflows)

**Use When**: Need complex scheduling with dependencies

**Setup**:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-quartz'
}
```

```java
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail dailyReportJobDetail() {
        return JobBuilder.newJob(DailyReportJob.class)
                .withIdentity("dailyReportJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyReportTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(dailyReportJobDetail())
                .withIdentity("dailyReportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
                .build();
    }
}

public class DailyReportJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Executing daily report job");
        // Task logic
    }
}
```

**Time to Setup**: 10-15 minutes

---

## Cron Expression Guide

### Common Patterns

| Schedule | Cron Expression | Description |
|----------|-----------------|-------------|
| Every minute | `0 * * * * ?` | Every minute on the minute |
| Every 5 minutes | `0 */5 * * * ?` | Every 5 minutes |
| Every hour | `0 0 * * * ?` | Top of every hour |
| Daily at 2 AM | `0 0 2 * * ?` | 2:00 AM every day |
| Weekly (Monday) | `0 0 0 ? * MON` | Monday at midnight |
| Monthly (1st) | `0 0 0 1 * ?` | 1st of month at midnight |
| Weekdays only | `0 0 9 ? * MON-FRI` | 9 AM Monday-Friday |
| Business hours | `0 0 9-17 ? * MON-FRI` | Every hour 9-5, Mon-Fri |

### Cron Expression Format

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-7 or MON-SUN, 0 and 7 = Sunday)
│ │ │ │ │ │
* * * * * ?
```

**Special Characters**:
- `*` = Any value
- `?` = No specific value (day fields)
- `-` = Range (1-5 = 1,2,3,4,5)
- `,` = List (1,3,5 = 1 or 3 or 5)
- `/` = Increment (*/5 = every 5)
- `L` = Last (L = last day of month)
- `W` = Weekday (15W = nearest weekday to 15th)

**Time to Configure**: 5 minutes

---

## Dynamic Scheduling Pattern

**Use When**: Need to modify schedules at runtime

```java
@Service
@RequiredArgsConstructor
public class DynamicSchedulingService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Schedule task dynamically
     */
    public void scheduleTask(String taskId, String cronExpression, Runnable task) {
        // Cancel existing task if any
        cancelTask(taskId);

        // Schedule new task
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
            task,
            new CronTrigger(cronExpression)
        );

        scheduledTasks.put(taskId, scheduledTask);
        log.info("Scheduled task: {} with cron: {}", taskId, cronExpression);
    }

    /**
     * Cancel scheduled task
     */
    public void cancelTask(String taskId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(taskId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(taskId);
            log.info("Cancelled task: {}", taskId);
        }
    }

    /**
     * Update task schedule
     */
    public void updateSchedule(String taskId, String newCronExpression, Runnable task) {
        scheduleTask(taskId, newCronExpression, task);
    }
}

// Usage
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final DynamicSchedulingService schedulingService;

    @PostMapping("/schedule")
    public ResponseDTO<Void> scheduleTask(@RequestBody ScheduleTaskForm form) {
        schedulingService.scheduleTask(
            form.getTaskId(),
            form.getCronExpression(),
            () -> executeTask(form.getTaskId())
        );

        return ResponseDTO.ok();
    }

    @DeleteMapping("/{taskId}")
    public ResponseDTO<Void> cancelTask(@PathVariable String taskId) {
        schedulingService.cancelTask(taskId);
        return ResponseDTO.ok();
    }
}
```

**Time to Implement**: 12-15 minutes

---

## Task Monitoring Pattern

**Use When**: Need to track execution status and failures

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class MonitoredScheduledTask {

    private final TaskExecutionLogDao taskLogDao;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReport() {
        String taskId = "daily-report-" + System.currentTimeMillis();
        TaskExecutionLog log = new TaskExecutionLog();
        log.setTaskId(taskId);
        log.setTaskName("Daily Report");
        log.setStartTime(LocalDateTime.now());

        try {
            // Execute task
            generateReport();

            // Log success
            log.setStatus("SUCCESS");
            log.setEndTime(LocalDateTime.now());
            log.setDuration(Duration.between(log.getStartTime(), log.getEndTime()).toMillis());

        } catch (Exception e) {
            // Log failure
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setEndTime(LocalDateTime.now());

            log.error("Task failed: {}", taskId, e);

        } finally {
            taskLogDao.insert(log);
        }
    }
}

// Database schema
CREATE TABLE t_task_execution_log (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    task_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, RUNNING
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration BIGINT,  -- milliseconds
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_log_name_start ON t_task_execution_log(task_name, start_time DESC);
CREATE INDEX idx_task_log_status ON t_task_execution_log(status);
```

**Time to Implement**: 8-10 minutes

---

## Distributed Lock Pattern

**Use When**: Multiple instances, need task to run only once

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedScheduledTask {

    private final RedissonClient redisson;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReportWithLock() {
        RLock lock = redisson.getLock("scheduled-task:daily-report");

        try {
            // Try to acquire lock (wait up to 5s, auto-release after 60s)
            boolean acquired = lock.tryLock(5, 60, TimeUnit.SECONDS);

            if (acquired) {
                try {
                    log.info("Lock acquired, executing daily report");
                    generateReport();
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("Lock not acquired, skipping (another instance running)");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted", e);
        }
    }
}
```

**Time to Implement**: 5-8 minutes

---

## Common Use Cases

### Use Case 1: Data Cleanup

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class DataCleanupTask {

    private final LogDao logDao;

    /**
     * Delete logs older than 90 days (daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = logDao.deleteOlderThan(cutoff);
        log.info("Deleted {} old log records", deleted);
    }
}
```

---

### Use Case 2: Cache Warming

```java
@Component
@RequiredArgsConstructor
public class CacheWarmingTask {

    private final CacheManager cacheManager;
    private final ProductDao productDao;

    /**
     * Warm cache every hour
     */
    @Scheduled(fixedRate = 3600000)
    public void warmProductCache() {
        List<Product> products = productDao.selectHotProducts();
        Cache cache = cacheManager.getCache("products");

        products.forEach(p -> cache.put(p.getId(), p));

        log.info("Warmed cache with {} hot products", products.size());
    }
}
```

---

### Use Case 3: Report Generation

```java
@Component
@RequiredArgsConstructor
public class ReportGenerationTask {

    private final ReportService reportService;
    private final EmailService emailService;

    /**
     * Generate and email monthly report (1st of month at 9 AM)
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void monthlyReport() {
        byte[] reportPdf = reportService.generateMonthlyReport();
        emailService.sendReport("admin@example.com", reportPdf);
    }
}
```

---

## Time Estimates

| Task | Implementation | Configuration | Testing | Total |
|------|---------------|---------------|---------|-------|
| @Scheduled Task | 3 min | 2 min | 2 min | 7 min |
| Snail Job Integration | 10 min | 8 min | 7 min | 25 min |
| Dynamic Scheduling | 12 min | 5 min | 8 min | 25 min |
| Distributed Lock | 5 min | 3 min | 5 min | 13 min |
| Task Monitoring | 8 min | 4 min | 3 min | 15 min |

**Full Scheduling System**: 30-45 minutes

---

**See Also**:
- [APM Integration](../apm-integration-skill/) - Monitor scheduled tasks
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - Async task patterns
