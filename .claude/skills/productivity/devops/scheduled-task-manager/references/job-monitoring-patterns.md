# Job Monitoring and Alerting Patterns

**Skill:** scheduled-task-manager
**Component:** Monitoring / Alerting / Observability
**Purpose:** Monitor job execution and set up alerts for SmartAdmin scheduled tasks

---

## Why Job Monitoring?

- ✅ Detect job failures immediately
- ✅ Track job execution time and trends
- ✅ Identify performance bottlenecks
- ✅ Ensure SLA compliance
- ✅ Historical analysis and reporting

---

## Pattern 1: Execution Logging

### Structured Logging

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoredJob {

    @JobExecutor(name = "monitoredJob")
    public ExecuteResult execute(String param) {
        long startTime = System.currentTimeMillis();

        log.info("[JOB_START] job=monitoredJob, param={}", param);

        try {
            // Business logic
            someService.process();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[JOB_SUCCESS] job=monitoredJob, duration={}ms", duration);

            return ExecuteResult.success("Processing completed in " + duration + "ms");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[JOB_FAILURE] job=monitoredJob, duration={}ms, error={}",
                duration, e.getMessage(), e);

            return ExecuteResult.failure("Processing failed: " + e.getMessage());
        }
    }
}
```

### Job Execution Record

```java
package net.lab1024.sa.admin.module.business.job.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_job_execution_log")
public class JobExecutionLogEntity {

    @TableId(type = IdType.AUTO)
    private Long logId;

    private String jobName;

    private String jobParam;

    private String executionStatus;  // SUCCESS, FAILURE, RUNNING

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private String resultMessage;

    private String errorMessage;

    private String executorHost;

    private LocalDateTime createdAt;
}
```

### Job Execution Logger Service

```java
package net.lab1024.sa.admin.module.business.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.job.dao.JobExecutionLogDao;
import net.lab1024.sa.admin.module.business.job.domain.entity.JobExecutionLogEntity;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionLoggerService {

    private final JobExecutionLogDao jobExecutionLogDao;

    /**
     * Log job start
     */
    public Long logJobStart(String jobName, String jobParam) {
        JobExecutionLogEntity log = JobExecutionLogEntity.builder()
            .jobName(jobName)
            .jobParam(jobParam)
            .executionStatus("RUNNING")
            .startTime(LocalDateTime.now())
            .executorHost(getHostname())
            .createdAt(LocalDateTime.now())
            .build();

        jobExecutionLogDao.insert(log);
        return log.getLogId();
    }

    /**
     * Log job success
     */
    public void logJobSuccess(Long logId, String resultMessage) {
        JobExecutionLogEntity log = jobExecutionLogDao.selectById(logId);
        if (log != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(log.getStartTime(), endTime).toMillis();

            log.setExecutionStatus("SUCCESS");
            log.setEndTime(endTime);
            log.setDurationMs(duration);
            log.setResultMessage(resultMessage);

            jobExecutionLogDao.updateById(log);
        }
    }

    /**
     * Log job failure
     */
    public void logJobFailure(Long logId, String errorMessage) {
        JobExecutionLogEntity log = jobExecutionLogDao.selectById(logId);
        if (log != null) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(log.getStartTime(), endTime).toMillis();

            log.setExecutionStatus("FAILURE");
            log.setEndTime(endTime);
            log.setDurationMs(duration);
            log.setErrorMessage(errorMessage);

            jobExecutionLogDao.updateById(log);
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
```

### Job with Execution Logging

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggedJob {

    private final JobExecutionLoggerService loggerService;
    private final BusinessService businessService;

    @JobExecutor(name = "loggedJob")
    public ExecuteResult execute(String param) {
        Long logId = loggerService.logJobStart("loggedJob", param);

        try {
            businessService.process();
            loggerService.logJobSuccess(logId, "Processing completed successfully");
            return ExecuteResult.success("Success");

        } catch (Exception e) {
            log.error("Job execution failed", e);
            loggerService.logJobFailure(logId, e.getMessage());
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 2: Metrics Integration

### Micrometer Metrics

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsJob {

    private final MeterRegistry meterRegistry;
    private final BusinessService businessService;

    @JobExecutor(name = "metricsJob")
    public ExecuteResult execute(String param) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            businessService.process();

            // Record success
            sample.stop(Timer.builder("job_execution_duration")
                .tag("job_name", "metricsJob")
                .tag("status", "success")
                .register(meterRegistry));

            meterRegistry.counter("job_execution_count",
                "job_name", "metricsJob",
                "status", "success"
            ).increment();

            return ExecuteResult.success("Processing completed");

        } catch (Exception e) {
            log.error("Job execution failed", e);

            // Record failure
            sample.stop(Timer.builder("job_execution_duration")
                .tag("job_name", "metricsJob")
                .tag("status", "failure")
                .register(meterRegistry));

            meterRegistry.counter("job_execution_count",
                "job_name", "metricsJob",
                "status", "failure"
            ).increment();

            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

### Prometheus Metrics Endpoint

Enable Prometheus metrics in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

Access metrics: `http://localhost:1024/actuator/prometheus`

---

## Pattern 3: Grafana Dashboard

### Prometheus Queries

```promql
# Job execution count
sum(rate(job_execution_count_total[5m])) by (job_name, status)

# Job execution duration (average)
avg(job_execution_duration_seconds) by (job_name)

# Job execution duration (P95)
histogram_quantile(0.95, sum(rate(job_execution_duration_seconds_bucket[5m])) by (job_name, le))

# Job failure rate
sum(rate(job_execution_count_total{status="failure"}[5m])) by (job_name) /
sum(rate(job_execution_count_total[5m])) by (job_name)
```

### Grafana Dashboard JSON

```json
{
  "dashboard": {
    "title": "SmartAdmin Scheduled Jobs",
    "panels": [
      {
        "title": "Job Execution Count",
        "targets": [
          {
            "expr": "sum(rate(job_execution_count_total[5m])) by (job_name, status)"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Job Execution Duration",
        "targets": [
          {
            "expr": "avg(job_execution_duration_seconds) by (job_name)"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Job Failure Rate",
        "targets": [
          {
            "expr": "sum(rate(job_execution_count_total{status=\"failure\"}[5m])) by (job_name) / sum(rate(job_execution_count_total[5m])) by (job_name)"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

## Pattern 4: Alert Rules

### Prometheus Alert Rules

```yaml
# prometheus-alerts.yml
groups:
  - name: job_alerts
    interval: 30s
    rules:
      # Job failure alert
      - alert: JobExecutionFailed
        expr: job_execution_count_total{status="failure"} > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Job {{ $labels.job_name }} failed"
          description: "Job {{ $labels.job_name }} has failed {{ $value }} times in the last minute"

      # Job execution time alert
      - alert: JobExecutionSlow
        expr: job_execution_duration_seconds > 300
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Job {{ $labels.job_name }} is slow"
          description: "Job {{ $labels.job_name }} took {{ $value }} seconds to execute"

      # Job not executing alert
      - alert: JobNotExecuting
        expr: time() - job_last_execution_timestamp_seconds > 3600
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Job {{ $labels.job_name }} not executing"
          description: "Job {{ $labels.job_name }} hasn't executed for {{ $value }} seconds"
```

---

## Pattern 5: Email Alerting

### Email Alert Service

```java
package net.lab1024.sa.admin.module.business.job.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAlertService {

    private final JavaMailSender mailSender;

    /**
     * Send job failure alert email
     */
    public void sendJobFailureAlert(String jobName, String errorMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@smartadmin.com");
            message.setSubject("[Alert] Job Execution Failed: " + jobName);
            message.setText(String.format(
                "Job Name: %s\n" +
                "Status: FAILED\n" +
                "Error Message: %s\n" +
                "Time: %s\n",
                jobName,
                errorMessage,
                java.time.LocalDateTime.now()
            ));

            mailSender.send(message);
            log.info("Job failure alert sent for job: {}", jobName);

        } catch (Exception e) {
            log.error("Failed to send job failure alert", e);
        }
    }

    /**
     * Send job execution timeout alert
     */
    public void sendJobTimeoutAlert(String jobName, long durationMs) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@smartadmin.com");
            message.setSubject("[Alert] Job Execution Timeout: " + jobName);
            message.setText(String.format(
                "Job Name: %s\n" +
                "Status: TIMEOUT\n" +
                "Duration: %d ms\n" +
                "Time: %s\n",
                jobName,
                durationMs,
                java.time.LocalDateTime.now()
            ));

            mailSender.send(message);
            log.info("Job timeout alert sent for job: {}", jobName);

        } catch (Exception e) {
            log.error("Failed to send job timeout alert", e);
        }
    }
}
```

### Job with Alert Integration

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertedJob {

    private final JobAlertService alertService;
    private final BusinessService businessService;

    @JobExecutor(name = "alertedJob")
    public ExecuteResult execute(String param) {
        long startTime = System.currentTimeMillis();

        try {
            businessService.process();

            long duration = System.currentTimeMillis() - startTime;

            // Check for timeout (e.g., > 5 minutes)
            if (duration > 300000) {
                alertService.sendJobTimeoutAlert("alertedJob", duration);
            }

            return ExecuteResult.success("Processing completed");

        } catch (Exception e) {
            log.error("Job execution failed", e);
            alertService.sendJobFailureAlert("alertedJob", e.getMessage());
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 6: Dashboard Query API

### Job Execution Statistics API

```java
package net.lab1024.sa.admin.module.business.job.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.job.dao.JobExecutionLogDao;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job/statistics")
@RequiredArgsConstructor
public class JobStatisticsController {

    private final JobExecutionLogDao jobExecutionLogDao;

    /**
     * Get job execution success rate (last 24 hours)
     */
    @GetMapping("/success-rate")
    public ResponseDTO<Map<String, Double>> getSuccessRate() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Map<String, Double> successRates = jobExecutionLogDao.calculateSuccessRate(since);
        return ResponseDTO.ok(successRates);
    }

    /**
     * Get job execution duration trends
     */
    @GetMapping("/duration-trend")
    public ResponseDTO<List<Object>> getDurationTrend(@RequestParam String jobName) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object> trend = jobExecutionLogDao.getDurationTrend(jobName, since);
        return ResponseDTO.ok(trend);
    }

    /**
     * Get job execution count by hour
     */
    @GetMapping("/execution-count")
    public ResponseDTO<Map<Integer, Long>> getExecutionCountByHour() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Map<Integer, Long> counts = jobExecutionLogDao.getExecutionCountByHour(since);
        return ResponseDTO.ok(counts);
    }

    /**
     * Get recent job failures
     */
    @GetMapping("/recent-failures")
    public ResponseDTO<List<JobExecutionLogEntity>> getRecentFailures() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<JobExecutionLogEntity> failures = jobExecutionLogDao.getRecentFailures(since, 20);
        return ResponseDTO.ok(failures);
    }
}
```

---

## Best Practices

1. **Logging:**
   - Use structured logging format
   - Include job name, parameters, duration
   - Log both success and failure

2. **Metrics:**
   - Track execution count, duration, success rate
   - Use tags for job name and status
   - Export to Prometheus for visualization

3. **Alerting:**
   - Set up alerts for job failures
   - Monitor execution time trends
   - Alert when jobs don't execute as scheduled

4. **Dashboard:**
   - Create Grafana dashboards for visualization
   - Monitor job health in real-time
   - Analyze historical trends

5. **Performance:**
   - Keep execution logs for 30-90 days
   - Archive old logs to cold storage
   - Index frequently queried columns

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
