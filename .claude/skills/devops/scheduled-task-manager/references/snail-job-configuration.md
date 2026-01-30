# Snail-Job Configuration Guide

**Skill:** scheduled-task-manager
**Component:** Snail-Job / Distributed Task Scheduling
**Purpose:** Configure Snail-Job for SmartAdmin applications

---

## Why Snail-Job?

- ✅ Modern distributed task scheduling (next-gen XXL-Job)
- ✅ Better performance and scalability
- ✅ Enhanced monitoring and observability
- ✅ Improved retry mechanisms
- ✅ Native Spring Boot 3 support
- ✅ More flexible job routing strategies

**Comparison with XXL-Job:**
| Feature | Snail-Job | XXL-Job |
|---------|-----------|---------|
| Spring Boot 3 | ✅ Native | ⚠️ Compatible |
| Performance | Higher | Good |
| Job Routing | More flexible | Standard |
| Monitoring | Enhanced | Good |
| Community | Growing | Mature |

---

## Dependencies

```gradle
dependencies {
    // Snail-Job client
    implementation 'com.aizuda:snail-job-client-starter:2.1.0'

    // Optional: Monitoring integration
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

---

## Pattern 1: Basic Configuration

### Step 1: application.yml Configuration

```yaml
# application.yml
snail-job:
  # Server configuration
  server:
    host: ${SNAIL_JOB_SERVER:localhost}
    port: ${SNAIL_JOB_PORT:8081}

  # Namespace (multi-tenant isolation)
  namespace: smartadmin-prod

  # Group name (executor group)
  group: smartadmin-executor-group

  # Application name
  app-name: smartadmin-service

  # Executor configuration
  executor:
    # Executor port
    port: 1789

    # Max thread pool size
    core-thread-size: 10
    max-thread-size: 50

    # Thread keep alive time (seconds)
    keep-alive-time: 60

  # Retry configuration
  retry:
    # Max retry count
    max-count: 3

    # Retry interval (ms)
    interval: 1000

  # Log configuration
  log:
    # Log path
    path: ./logs/snail-job

    # Log retention days
    retention-days: 30
```

### Step 2: Enable Snail-Job

```java
package net.lab1024.sa;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableSnailJob  // Enable Snail-Job
@SpringBootApplication
public class SmartAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAdminApplication.class, args);
    }
}
```

---

## Pattern 2: Job Handler Registration

### Simple Job Handler

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
public class UserStatisticsJob {

    private final UserStatisticsService userStatisticsService;

    /**
     * Daily user statistics calculation job
     * Execute at 2 AM every day
     */
    @JobExecutor(name = "userStatisticsJob")
    public ExecuteResult execute(String param) {
        try {
            log.info("Starting user statistics calculation, param: {}", param);

            // Execute business logic
            userStatisticsService.calculateDailyStatistics();

            log.info("User statistics calculation completed successfully");
            return ExecuteResult.success("Statistics calculation completed");

        } catch (Exception e) {
            log.error("User statistics calculation failed", e);
            return ExecuteResult.failure("Statistics calculation failed: " + e.getMessage());
        }
    }
}
```

### Job with Parameters

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupJob {

    private final DataCleanupService dataCleanupService;
    private final ObjectMapper objectMapper;

    @JobExecutor(name = "dataCleanupJob")
    public ExecuteResult execute(String param) {
        try {
            // Parse job parameters
            CleanupParams params = objectMapper.readValue(param, CleanupParams.class);

            log.info("Starting data cleanup: retentionDays={}, tableName={}",
                params.getRetentionDays(), params.getTableName());

            // Execute cleanup
            int deletedCount = dataCleanupService.cleanup(
                params.getTableName(),
                params.getRetentionDays()
            );

            log.info("Data cleanup completed: deleted {} records", deletedCount);
            return ExecuteResult.success("Deleted " + deletedCount + " records");

        } catch (Exception e) {
            log.error("Data cleanup failed", e);
            return ExecuteResult.failure("Cleanup failed: " + e.getMessage());
        }
    }

    @Data
    public static class CleanupParams {
        private String tableName;
        private Integer retentionDays;
    }
}
```

---

## Pattern 3: Sharding Job

```java
package net.lab1024.sa.admin.module.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.client.model.ShardingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingShardingJob {

    private final OrderProcessingService orderProcessingService;

    /**
     * Sharding job for processing large order dataset
     * Multiple executors process different shards in parallel
     */
    @JobExecutor(name = "orderProcessingShardingJob", enableSharding = true)
    public ExecuteResult execute(String param) {
        try {
            // Get sharding context
            ShardingContext context = ShardingContext.get();
            int shardIndex = context.getShardIndex();      // Current shard index (0-based)
            int shardTotal = context.getShardTotal();      // Total shard count

            log.info("Processing shard {}/{}, param: {}", shardIndex + 1, shardTotal, param);

            // Calculate shard range (example: process orders by ID mod)
            List<Long> orderIds = orderProcessingService.getOrderIdsByShard(shardIndex, shardTotal);

            log.info("Shard {}/{}: found {} orders to process", shardIndex + 1, shardTotal, orderIds.size());

            int processedCount = 0;
            for (Long orderId : orderIds) {
                orderProcessingService.processOrder(orderId);
                processedCount++;
            }

            log.info("Shard {}/{}: processed {} orders successfully", shardIndex + 1, shardTotal, processedCount);
            return ExecuteResult.success("Shard " + (shardIndex + 1) + " processed " + processedCount + " orders");

        } catch (Exception e) {
            log.error("Sharding job failed", e);
            return ExecuteResult.failure("Shard processing failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 4: Broadcast Job

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
public class CacheClearJob {

    private final CacheManager cacheManager;

    /**
     * Broadcast job - executes on ALL executors
     * Use case: Clear local cache on all application instances
     */
    @JobExecutor(name = "cacheClearJob", broadcast = true)
    public ExecuteResult execute(String param) {
        try {
            log.info("Clearing local cache on this executor instance, param: {}", param);

            // Clear local cache
            cacheManager.clearAllCaches();

            log.info("Local cache cleared successfully");
            return ExecuteResult.success("Cache cleared");

        } catch (Exception e) {
            log.error("Cache clear failed", e);
            return ExecuteResult.failure("Cache clear failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 5: Workflow Job (Job Chain)

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
public class ReportGenerationWorkflowJob {

    private final DataExtractionService dataExtractionService;
    private final DataTransformationService dataTransformationService;
    private final ReportGenerationService reportGenerationService;
    private final EmailService emailService;

    /**
     * Workflow job with multiple steps
     * Step 1 → Step 2 → Step 3 → Step 4
     */
    @JobExecutor(name = "reportGenerationWorkflow")
    public ExecuteResult execute(String param) {
        try {
            log.info("Starting report generation workflow, param: {}", param);

            // Step 1: Extract data
            log.info("Step 1: Extracting data...");
            String dataFile = dataExtractionService.extractData();

            // Step 2: Transform data
            log.info("Step 2: Transforming data...");
            String transformedFile = dataTransformationService.transform(dataFile);

            // Step 3: Generate report
            log.info("Step 3: Generating report...");
            String reportFile = reportGenerationService.generate(transformedFile);

            // Step 4: Send email
            log.info("Step 4: Sending email...");
            emailService.sendReportEmail(reportFile);

            log.info("Report generation workflow completed successfully");
            return ExecuteResult.success("Report generated and sent: " + reportFile);

        } catch (Exception e) {
            log.error("Report generation workflow failed", e);
            return ExecuteResult.failure("Workflow failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 6: Retry Configuration

### Global Retry Configuration

Already configured in `application.yml`:
```yaml
snail-job:
  retry:
    max-count: 3
    interval: 1000  # 1 second
```

### Job-Specific Retry Configuration

```java
@JobExecutor(
    name = "importantJob",
    retryCount = 5,         // Override global max retry count
    retryInterval = 2000    // Override global retry interval (ms)
)
public ExecuteResult execute(String param) {
    // Job logic
}
```

### Custom Retry Logic

```java
@JobExecutor(name = "customRetryJob")
public ExecuteResult execute(String param) {
    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        try {
            // Execute business logic
            someService.process();
            return ExecuteResult.success("Processing completed");

        } catch (TemporaryException e) {
            retryCount++;
            log.warn("Temporary error, retry {}/{}", retryCount, maxRetries, e);

            if (retryCount >= maxRetries) {
                return ExecuteResult.failure("Max retries exceeded: " + e.getMessage());
            }

            // Exponential backoff
            try {
                Thread.sleep((long) (1000 * Math.pow(2, retryCount - 1)));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (PermanentException e) {
            // Don't retry permanent errors
            log.error("Permanent error, skipping retry", e);
            return ExecuteResult.failure("Permanent error: " + e.getMessage());
        }
    }

    return ExecuteResult.failure("Max retries exceeded");
}
```

---

## Pattern 7: Job Monitoring

### Custom Metrics

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
public class MonitoredJob {

    private final MeterRegistry meterRegistry;
    private final BusinessService businessService;

    @JobExecutor(name = "monitoredJob")
    public ExecuteResult execute(String param) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            businessService.process();

            sample.stop(Timer.builder("snail_job_execution")
                .tag("job_name", "monitoredJob")
                .tag("status", "success")
                .register(meterRegistry));

            return ExecuteResult.success("Processing completed");

        } catch (Exception e) {
            sample.stop(Timer.builder("snail_job_execution")
                .tag("job_name", "monitoredJob")
                .tag("status", "failure")
                .register(meterRegistry));

            meterRegistry.counter("snail_job_failures",
                "job_name", "monitoredJob").increment();

            return ExecuteResult.failure("Processing failed: " + e.getMessage());
        }
    }
}
```

---

## Pattern 8: Docker Deployment

### docker-compose.yml

```yaml
version: '3.8'

services:
  snail-job-server:
    image: aizuda/snail-job-server:2.1.0
    container_name: snail-job-server
    ports:
      - "8081:8081"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/snail_job?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=root123
    volumes:
      - ./logs/snail-job:/app/logs
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    container_name: snail-job-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: snail_job
    volumes:
      - ./data/mysql:/var/lib/mysql

  smartadmin-executor:
    image: smartadmin:latest
    container_name: smartadmin-executor
    ports:
      - "1024:1024"
      - "1789:1789"  # Snail-Job executor port
    environment:
      SNAIL_JOB_SERVER: snail-job-server
      SNAIL_JOB_PORT: 8081
    depends_on:
      - snail-job-server
```

---

## Pattern 9: Health Check

```java
package net.lab1024.sa.admin.module.system.health;

import com.aizuda.snailjob.client.core.SnailJobClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnailJobHealthIndicator implements HealthIndicator {

    private final SnailJobClient snailJobClient;

    @Override
    public Health health() {
        try {
            boolean isConnected = snailJobClient.isConnected();

            if (isConnected) {
                return Health.up()
                    .withDetail("status", "connected")
                    .withDetail("namespace", snailJobClient.getNamespace())
                    .withDetail("group", snailJobClient.getGroup())
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "disconnected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## Best Practices

1. **Job Naming:**
   - Use descriptive names: `userStatisticsJob`, `dataCleanupJob`
   - Follow naming convention: `{module}{Action}Job`

2. **Error Handling:**
   - Return `ExecuteResult.success()` on success
   - Return `ExecuteResult.failure()` with detailed error message
   - Log all errors for troubleshooting

3. **Parameters:**
   - Use JSON format for complex parameters
   - Validate parameters before execution
   - Document parameter schema

4. **Idempotency:**
   - Design jobs to be idempotent (safe to re-execute)
   - Check processing status before execution
   - Use distributed locks for critical sections

5. **Performance:**
   - Use sharding for large datasets
   - Implement timeout for long-running jobs
   - Monitor job execution time

---

**Next:** [Cron Expression Guide](cron-expression-guide.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
