# P2-18: Scheduled Jobs Management

## Document Control

| Attribute | Value |
|-----------|-------|
| Document ID | P2-18 |
| Title | Scheduled Jobs Management |
| Version | 1.0.0 |
| Status | Draft |
| Author | SmartAdmin Architecture Team |
| Created | 2026-01-23 |
| Last Updated | 2026-01-23 |
| Related Docs | [backend_project.md](../../backend_project.md), [P0-01](../P0-critical/01-double-entry-ledger-schema.md), [P1-11](../P1-important/11-vip-system-design.md) |

---

## 1. Background

### 1.1 Purpose

This document provides comprehensive architecture and implementation guidance for distributed scheduled job management across the iGaming platform using Snail-Job framework.

**Core Objectives**:
1. **Reliable Execution**: Guarantee critical jobs execute exactly once (idempotency)
2. **Distributed Coordination**: Prevent duplicate job execution across multiple pods
3. **Monitoring & Alerting**: Track job execution status and alert on failures
4. **Retry Mechanism**: Automatic retry for failed jobs with exponential backoff
5. **Multi-Tenant Isolation**: Execute tenant-specific jobs independently

### 1.2 Scope

**In Scope**:
- Snail-Job 1.1.0 integration with Spring Boot 3.x
- Critical business jobs (VIP tier evaluation, bonus expiration, daily reconciliation)
- Cron-based and interval-based scheduling
- Job monitoring dashboard and alerting
- Failed job retry with exponential backoff
- Multi-tenant job isolation
- Job execution logs and audit trail

**Out of Scope**:
- Ad-hoc one-time job execution (use Spring @Async instead)
- Real-time event processing (use Kafka consumers instead)
- Complex workflow orchestration (use Saga pattern from P1-05)
- Job scheduling UI (Snail-Job provides built-in admin console)

### 1.3 Strategic Alignment

Aligns with igame_str.md first principles:
- **Automation Leverage**: Replace 70% manual operations with scheduled jobs
- **Zero Marginal Cost**: Automated jobs scale without additional labor
- **Reliability**: Mathematical certainty of job execution (exactly-once semantics)

### 1.4 Critical Scheduled Jobs

| Job Name | Schedule | Purpose | Dependencies |
|----------|----------|---------|--------------|
| VIP Tier Evaluation | Daily 00:00 UTC | Calculate player VIP tiers based on 30-day metrics | P1-11 |
| Bonus Expiration | Hourly | Mark expired bonuses as inactive | P1-12 |
| Daily Ledger Reconciliation | Daily 01:00 UTC | Verify ledger balance matches wallet balance | P0-01 |
| Withdrawal Auto-Approval | Every 5 min | Auto-approve low-risk withdrawals | P0-03 |
| Game Catalog Sync | Daily 02:00 UTC | Sync game metadata from providers | P1-09 |
| Inactive Player Cleanup | Weekly (Sunday 03:00) | Anonymize GDPR-compliant inactive players | P1-16 |
| Monthly GGR Report | Monthly (1st day 04:00) | Generate regulatory GGR reports | P1-16 |
| RTP Verification | Daily 05:00 UTC | Verify game RTP ≥92% (MGA requirement) | P1-09 |

---

## 2. Architecture

### 2.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   Application Pods (Kubernetes)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Pod 1      │  │   Pod 2      │  │   Pod 3      │          │
│  │ (Executor)   │  │ (Executor)   │  │ (Standby)    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                 │                 │                    │
│         └─────────────────┴─────────────────┘                    │
│                           │                                      │
│                           ▼                                      │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                    Snail-Job Server                               │
│  ┌────────────────────────────────────────────────────────┐     │
│  │              Job Scheduler Engine                       │     │
│  │  - Cron expression parser                              │     │
│  │  - Job trigger management                              │     │
│  │  - Leader election (Redis lock)                        │     │
│  │  - Load balancing (round-robin, random, consistent hash)│    │
│  └────────────────────────────────────────────────────────┘     │
│                           │                                      │
│  ┌────────────────────────▼───────────────────────────────┐     │
│  │              Job Execution Coordinator                  │     │
│  │  - Dispatch jobs to executor pods                      │     │
│  │  - Track execution status (running, success, failed)   │     │
│  │  - Retry failed jobs (exponential backoff)             │     │
│  │  - Timeout handling (kill long-running jobs)           │     │
│  └────────────────────────────────────────────────────────┘     │
│                           │                                      │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                      PostgreSQL Database                         │
│  ┌────────────────────────────────────────────────────────┐     │
│  │              Job Metadata Tables                        │     │
│  │  - sj_job: Job definitions (cron, handler)             │     │
│  │  - sj_job_task_batch: Batch metadata                   │     │
│  │  - sj_job_task: Task instances (per execution)         │     │
│  │  - sj_job_log: Execution logs                          │     │
│  │  - sj_retry_task: Failed tasks awaiting retry          │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Redis (Distributed Locks)                   │
│  - Leader election lock (prevent duplicate job triggers)        │
│  - Job execution lock (prevent concurrent execution)            │
│  - Job result cache (reduce database queries)                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Job Execution Flow

```
1. Job Trigger (Cron Schedule Reached)
   ↓
2. Leader Election (Redis Lock Acquisition)
   ↓
3. Create Job Task Batch
   ↓
4. Dispatch to Executor Pod (Load Balancing)
   ↓
5. Executor Pod: Acquire Execution Lock
   ↓
6. Execute Job Handler (@JobHandler method)
   ↓
7. Update Task Status (SUCCESS / FAILED)
   ↓
8. If FAILED → Retry Queue (Exponential Backoff)
   ↓
9. Release Lock
   ↓
10. Log Execution Result
```

### 2.3 Multi-Tenant Job Isolation

**Tenant-Scoped Jobs**:
```
Job: VIP Tier Evaluation
├── Task 1: tenant_id=merchant_a (1000 players)
├── Task 2: tenant_id=merchant_b (500 players)
└── Task 3: tenant_id=merchant_c (2000 players)

Each task executes independently with tenant context propagation
```

---

## 3. Implementation

### 3.1 Snail-Job Integration

#### 3.1.1 Dependencies

**build.gradle** (sa-admin module):
```gradle
dependencies {
    // Snail-Job client
    implementation 'com.aizuda:snail-job-client-starter:1.1.0'

    // Required for distributed locks
    implementation 'org.redisson:redisson-spring-boot-starter:3.50.0'
}
```

#### 3.1.2 Configuration

**application.yml**:
```yaml
snail-job:
  # Snail-Job server configuration
  server:
    host: snail-job-server  # Kubernetes service name
    port: 8888

  # Client configuration
  client:
    namespace: smart-admin  # Namespace for job isolation
    group: default          # Group name (can be per-tenant)

    # Executor configuration
    executor:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 1000
      thread-name-prefix: snail-job-executor-

    # Retry configuration
    retry:
      max-attempts: 3
      backoff-multiplier: 2.0  # Exponential backoff (1s, 2s, 4s)
      max-backoff: 300000      # Max 5 minutes

    # Timeout configuration
    timeout:
      default: 300000  # 5 minutes default timeout
```

#### 3.1.3 Enable Snail-Job

**Application.java**:
```java
package net.lab1024.sa.admin;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartAdmin application entry point
 *
 * @author SmartAdmin Team
 */
@SpringBootApplication
@EnableSnailJob  // Enable Snail-Job client
public class SmartAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAdminApplication.class, args);
    }
}
```

### 3.2 Job Handler Implementation

#### 3.2.1 VIP Tier Evaluation Job

**VipTierEvaluationJobHandler.java**:
```java
package net.lab1024.sa.admin.module.business.vip.job;

import com.aizuda.snailjob.client.job.core.annotation.JobHandler;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.vip.manager.VipTierEvaluationManager;
import net.lab1024.sa.admin.module.business.vip.domain.VipTierEvaluationResult;
import net.lab1024.sa.base.module.support.tenant.dao.TenantDao;
import net.lab1024.sa.base.module.support.tenant.domain.entity.TenantEntity;
import net.lab1024.sa.base.module.support.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VIP tier evaluation scheduled job
 *
 * Schedule: Daily at 00:00 UTC
 * Purpose: Calculate player VIP tiers based on rolling 30-day metrics
 *
 * Related: P1-11 VIP System Design
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipTierEvaluationJobHandler {

    private final TenantDao tenantDao;
    private final VipTierEvaluationManager vipTierEvaluationManager;

    /**
     * Job handler method
     *
     * @JobHandler annotation registers this method as a Snail-Job handler
     * name: Unique job identifier (must match job configuration in database)
     */
    @JobHandler(name = "vipTierEvaluation")
    public ExecuteResult execute(String param) {
        log.info("Starting VIP tier evaluation job");

        try {
            // Get all active tenants
            List<TenantEntity> tenants = tenantDao.selectActiveTeants();

            int totalUpgrades = 0;
            int totalDowngrades = 0;
            int totalMaintained = 0;

            // Process each tenant independently
            for (TenantEntity tenant : tenants) {
                try {
                    // Set tenant context for multi-tenant isolation
                    TenantContextHolder.setTenantId(tenant.getTenantId());

                    // Execute VIP tier evaluation for this tenant
                    VipTierEvaluationResult result = vipTierEvaluationManager.evaluateAllPlayers();

                    totalUpgrades += result.getUpgradeCount();
                    totalDowngrades += result.getDowngradeCount();
                    totalMaintained += result.getMaintainedCount();

                    log.info("VIP tier evaluation completed for tenant={}: upgrades={}, downgrades={}, maintained={}",
                        tenant.getTenantId(), result.getUpgradeCount(), result.getDowngradeCount(), result.getMaintainedCount());

                } catch (Exception e) {
                    log.error("VIP tier evaluation failed for tenant={}", tenant.getTenantId(), e);
                    // Continue processing other tenants (don't fail entire job)
                } finally {
                    TenantContextHolder.clear();
                }
            }

            String resultMessage = String.format(
                "VIP tier evaluation completed: %d tenants processed, %d upgrades, %d downgrades, %d maintained",
                tenants.size(), totalUpgrades, totalDowngrades, totalMaintained
            );

            log.info(resultMessage);

            return ExecuteResult.success(resultMessage);

        } catch (Exception e) {
            log.error("VIP tier evaluation job failed", e);
            return ExecuteResult.failure("VIP tier evaluation failed: " + e.getMessage());
        }
    }
}
```

#### 3.2.2 Bonus Expiration Job

**BonusExpirationJobHandler.java**:
```java
package net.lab1024.sa.admin.module.business.bonus.job;

import com.aizuda.snailjob.client.job.core.annotation.JobHandler;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.bonus.dao.PlayerBonusDao;
import net.lab1024.sa.admin.module.business.bonus.domain.entity.PlayerBonus;
import net.lab1024.sa.admin.module.business.bonus.domain.enums.BonusStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Bonus expiration scheduled job
 *
 * Schedule: Hourly
 * Purpose: Mark expired bonuses as EXPIRED status
 *
 * Related: P1-12 Bonus Engine
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BonusExpirationJobHandler {

    private final PlayerBonusDao playerBonusDao;

    @JobHandler(name = "bonusExpiration")
    public ExecuteResult execute(String param) {
        log.info("Starting bonus expiration job");

        try {
            LocalDateTime now = LocalDateTime.now();

            // Update all active bonuses that have passed expiration date
            int expiredCount = playerBonusDao.update(
                null,
                new LambdaUpdateWrapper<PlayerBonus>()
                    .set(PlayerBonus::getStatus, BonusStatus.EXPIRED)
                    .set(PlayerBonus::getUpdatedAt, now)
                    .eq(PlayerBonus::getStatus, BonusStatus.ACTIVE)
                    .lt(PlayerBonus::getExpiresAt, now)
            );

            String resultMessage = String.format("Bonus expiration completed: %d bonuses expired", expiredCount);
            log.info(resultMessage);

            return ExecuteResult.success(resultMessage);

        } catch (Exception e) {
            log.error("Bonus expiration job failed", e);
            return ExecuteResult.failure("Bonus expiration failed: " + e.getMessage());
        }
    }
}
```

#### 3.2.3 Daily Ledger Reconciliation Job

**DailyLedgerReconciliationJobHandler.java**:
```java
package net.lab1024.sa.admin.module.business.ledger.job;

import com.aizuda.snailjob.client.job.core.annotation.JobHandler;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.ledger.manager.LedgerReconciliationManager;
import net.lab1024.sa.admin.module.business.ledger.domain.ReconciliationResult;
import net.lab1024.sa.base.module.support.tenant.dao.TenantDao;
import net.lab1024.sa.base.module.support.tenant.domain.entity.TenantEntity;
import net.lab1024.sa.base.module.support.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily ledger reconciliation job
 *
 * Schedule: Daily at 01:00 UTC
 * Purpose: Verify ledger balance matches wallet balance (financial integrity)
 *
 * Related: P0-01 Double-Entry Ledger Schema
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyLedgerReconciliationJobHandler {

    private final TenantDao tenantDao;
    private final LedgerReconciliationManager ledgerReconciliationManager;

    @JobHandler(name = "dailyLedgerReconciliation")
    public ExecuteResult execute(String param) {
        log.info("Starting daily ledger reconciliation job");

        try {
            List<TenantEntity> tenants = tenantDao.selectActiveTeants();

            int totalReconciled = 0;
            int totalDiscrepancies = 0;

            for (TenantEntity tenant : tenants) {
                try {
                    TenantContextHolder.setTenantId(tenant.getTenantId());

                    ReconciliationResult result = ledgerReconciliationManager.reconcileAllWallets();

                    totalReconciled += result.getReconciledCount();
                    totalDiscrepancies += result.getDiscrepancyCount();

                    if (result.getDiscrepancyCount() > 0) {
                        log.error("CRITICAL: Ledger discrepancies found for tenant={}: {} wallets with mismatches",
                            tenant.getTenantId(), result.getDiscrepancyCount());

                        // Trigger alert to finance team
                        // alertService.sendCriticalAlert(...);
                    }

                } catch (Exception e) {
                    log.error("Reconciliation failed for tenant={}", tenant.getTenantId(), e);
                } finally {
                    TenantContextHolder.clear();
                }
            }

            if (totalDiscrepancies > 0) {
                String errorMessage = String.format(
                    "CRITICAL: %d wallet discrepancies found across %d tenants",
                    totalDiscrepancies, tenants.size()
                );
                log.error(errorMessage);
                return ExecuteResult.failure(errorMessage);
            }

            String resultMessage = String.format(
                "Daily reconciliation completed: %d wallets reconciled, 0 discrepancies",
                totalReconciled
            );
            log.info(resultMessage);

            return ExecuteResult.success(resultMessage);

        } catch (Exception e) {
            log.error("Daily ledger reconciliation job failed", e);
            return ExecuteResult.failure("Reconciliation failed: " + e.getMessage());
        }
    }
}
```

### 3.3 Job Configuration

#### 3.3.1 Job Registration (SQL)

**Insert Job Definitions**:
```sql
-- VIP Tier Evaluation Job
INSERT INTO sj_job (
    namespace, group_name, job_name, job_handler, cron_expression,
    description, executor_timeout, max_retry_times, retry_interval,
    executor_route_strategy, job_status, create_dt
) VALUES (
    'smart-admin', 'default', 'vipTierEvaluation', 'vipTierEvaluation',
    '0 0 0 * * ?',  -- Daily at 00:00 UTC
    'Calculate player VIP tiers based on 30-day metrics',
    300000,  -- 5 min timeout
    3,       -- Max 3 retries
    60000,   -- 1 min retry interval
    'ROUND',  -- Round-robin load balancing
    1,       -- Enabled
    NOW()
);

-- Bonus Expiration Job
INSERT INTO sj_job (
    namespace, group_name, job_name, job_handler, cron_expression,
    description, executor_timeout, max_retry_times, retry_interval,
    executor_route_strategy, job_status, create_dt
) VALUES (
    'smart-admin', 'default', 'bonusExpiration', 'bonusExpiration',
    '0 0 * * * ?',  -- Hourly
    'Mark expired bonuses as inactive',
    60000,   -- 1 min timeout
    3,       -- Max 3 retries
    30000,   -- 30 sec retry interval
    'ROUND',
    1,
    NOW()
);

-- Daily Ledger Reconciliation Job
INSERT INTO sj_job (
    namespace, group_name, job_name, job_handler, cron_expression,
    description, executor_timeout, max_retry_times, retry_interval,
    executor_route_strategy, job_status, create_dt
) VALUES (
    'smart-admin', 'default', 'dailyLedgerReconciliation', 'dailyLedgerReconciliation',
    '0 0 1 * * ?',  -- Daily at 01:00 UTC
    'Verify ledger balance matches wallet balance',
    600000,  -- 10 min timeout
    1,       -- Single retry only (critical job)
    0,       -- No retry interval
    'FIRST', -- Always run on first executor
    1,
    NOW()
);

-- Withdrawal Auto-Approval Job
INSERT INTO sj_job (
    namespace, group_name, job_name, job_handler, cron_expression,
    description, executor_timeout, max_retry_times, retry_interval,
    executor_route_strategy, job_status, create_dt
) VALUES (
    'smart-admin', 'default', 'withdrawalAutoApproval', 'withdrawalAutoApproval',
    '0 */5 * * * ?',  -- Every 5 minutes
    'Auto-approve low-risk withdrawals',
    120000,  -- 2 min timeout
    3,
    10000,   -- 10 sec retry interval
    'ROUND',
    1,
    NOW()
);
```

#### 3.3.2 Cron Expression Reference

| Expression | Description | Example Use Case |
|------------|-------------|------------------|
| `0 0 0 * * ?` | Daily at midnight UTC | VIP tier evaluation |
| `0 0 * * * ?` | Hourly | Bonus expiration |
| `0 */5 * * * ?` | Every 5 minutes | Withdrawal auto-approval |
| `0 0 1 * * ?` | Daily at 01:00 UTC | Ledger reconciliation |
| `0 0 0 1 * ?` | Monthly (1st day) | Monthly GGR report |
| `0 0 3 ? * SUN` | Weekly Sunday at 03:00 | Inactive player cleanup |

### 3.4 Job Monitoring

#### 3.4.1 Job Execution Logs

**Query Recent Job Executions**:
```sql
-- Get last 10 job executions with status
SELECT
    j.job_name,
    jt.task_batch_id,
    jt.task_status,
    jt.execute_result,
    jt.create_dt AS executed_at,
    (jt.finish_dt - jt.create_dt) AS duration_ms
FROM sj_job_task jt
JOIN sj_job j ON jt.job_id = j.id
WHERE j.namespace = 'smart-admin'
  AND j.group_name = 'default'
ORDER BY jt.create_dt DESC
LIMIT 10;

-- Get failed jobs in last 24 hours
SELECT
    j.job_name,
    jt.task_batch_id,
    jt.execute_result,
    jt.create_dt AS failed_at
FROM sj_job_task jt
JOIN sj_job j ON jt.job_id = j.id
WHERE j.namespace = 'smart-admin'
  AND jt.task_status = 3  -- FAILED
  AND jt.create_dt > NOW() - INTERVAL '24 hours'
ORDER BY jt.create_dt DESC;
```

#### 3.4.2 Prometheus Metrics

**Custom Metrics** (JobMetricsInterceptor.java):
```java
package net.lab1024.sa.admin.module.system.job.interceptor;

import com.aizuda.snailjob.client.job.core.interceptor.JobInterceptor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Job execution metrics interceptor
 *
 * Publishes metrics to Prometheus:
 * - job.execution.count (counter)
 * - job.execution.duration (timer)
 * - job.execution.failure (counter)
 *
 * @author SmartAdmin Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobMetricsInterceptor implements JobInterceptor {

    private final MeterRegistry meterRegistry;

    @Override
    public void beforeExecute(String jobName, String param) {
        // No-op (metrics recorded in afterExecute)
    }

    @Override
    public void afterExecute(String jobName, String param, ExecuteResult result, long executionTimeMs) {
        // Record execution count
        meterRegistry.counter("job.execution.count",
            "job_name", jobName,
            "status", result.isSuccess() ? "success" : "failure"
        ).increment();

        // Record execution duration
        meterRegistry.timer("job.execution.duration",
            "job_name", jobName
        ).record(executionTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Record failure count
        if (!result.isSuccess()) {
            meterRegistry.counter("job.execution.failure",
                "job_name", jobName
            ).increment();
        }
    }
}
```

**Prometheus Queries**:
```promql
# Job execution rate (executions per minute)
sum(rate(job_execution_count[5m])) by (job_name)

# Job success rate
sum(rate(job_execution_count{status="success"}[5m])) by (job_name) /
sum(rate(job_execution_count[5m])) by (job_name)

# Job execution duration (p95)
histogram_quantile(0.95,
  sum(rate(job_execution_duration_seconds_bucket[5m])) by (le, job_name)
)

# Failed job count (last hour)
sum(increase(job_execution_failure[1h])) by (job_name)
```

#### 3.4.3 Alerting Rules

**Prometheus Alerts** (alerts.yml):
```yaml
groups:
  - name: scheduled_jobs_alerts
    interval: 30s
    rules:
      # Job execution failure
      - alert: JobExecutionFailure
        expr: increase(job_execution_failure[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Scheduled job failed: {{ $labels.job_name }}"
          description: "Job {{ $labels.job_name }} has failed in the last 5 minutes"

      # Daily reconciliation discrepancy
      - alert: LedgerReconciliationDiscrepancy
        expr: job_execution_count{job_name="dailyLedgerReconciliation",status="failure"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "CRITICAL: Ledger reconciliation found discrepancies"
          description: "Daily ledger reconciliation detected wallet balance mismatches"

      # Job not executed in expected timeframe
      - alert: JobNotExecuted
        expr: (time() - job_execution_count{job_name="vipTierEvaluation"} > 86400)  # 24 hours
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Job has not executed: {{ $labels.job_name }}"
          description: "Job {{ $labels.job_name }} has not executed in expected timeframe"
```

---

## 4. Testing

### 4.1 Unit Tests

**VipTierEvaluationJobHandlerTest.java**:
```java
@SpringBootTest
class VipTierEvaluationJobHandlerTest {

    @Autowired
    private VipTierEvaluationJobHandler jobHandler;

    @MockBean
    private VipTierEvaluationManager vipTierEvaluationManager;

    @Test
    void testExecute_Success() {
        // Given: 100 players with tier changes
        VipTierEvaluationResult mockResult = VipTierEvaluationResult.builder()
            .upgradeCount(20)
            .downgradeCount(5)
            .maintainedCount(75)
            .build();

        when(vipTierEvaluationManager.evaluateAllPlayers()).thenReturn(mockResult);

        // When: Execute job
        ExecuteResult result = jobHandler.execute(null);

        // Then: Job succeeds
        assertTrue(result.isSuccess());
        assertThat(result.getMessage()).contains("20 upgrades", "5 downgrades", "75 maintained");
    }

    @Test
    void testExecute_PartialFailure() {
        // Given: One tenant fails, others succeed
        when(vipTierEvaluationManager.evaluateAllPlayers())
            .thenThrow(new ServiceException("Database error"))  // First tenant fails
            .thenReturn(VipTierEvaluationResult.builder()      // Second tenant succeeds
                .upgradeCount(10)
                .downgradeCount(2)
                .maintainedCount(50)
                .build());

        // When: Execute job
        ExecuteResult result = jobHandler.execute(null);

        // Then: Job still succeeds (graceful degradation)
        assertTrue(result.isSuccess());
        // Verify error logged for failed tenant
    }
}
```

### 4.2 Integration Tests

**ScheduledJobIntegrationTest.java**:
```java
@SpringBootTest
@Testcontainers
class ScheduledJobIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("smart_admin_test");

    @Autowired
    private BonusExpirationJobHandler bonusExpirationJobHandler;

    @Autowired
    private PlayerBonusDao playerBonusDao;

    @Test
    void testBonusExpiration_IntegrationTest() {
        // Given: 10 active bonuses, 5 expired
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 10; i++) {
            PlayerBonus bonus = new PlayerBonus();
            bonus.setPlayerId((long) i);
            bonus.setStatus(BonusStatus.ACTIVE);
            bonus.setExpiresAt(i < 5 ? now.minusDays(1) : now.plusDays(1));  // 5 expired
            playerBonusDao.insert(bonus);
        }

        // When: Execute job
        ExecuteResult result = bonusExpirationJobHandler.execute(null);

        // Then: Job succeeds and 5 bonuses marked expired
        assertTrue(result.isSuccess());

        long expiredCount = playerBonusDao.selectCount(
            new LambdaQueryWrapper<PlayerBonus>()
                .eq(PlayerBonus::getStatus, BonusStatus.EXPIRED)
        );
        assertEquals(5, expiredCount);
    }
}
```

---

## 5. Operations

### 5.1 Deployment

#### 5.1.1 Snail-Job Server Deployment

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  snail-job-server:
    image: aizuda/snail-job-server:1.1.0
    container_name: snail-job-server
    restart: unless-stopped
    ports:
      - "8888:8888"  # Server API port
      - "9999:9999"  # Admin console port
    environment:
      # Database configuration
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/snail_job
      SPRING_DATASOURCE_USERNAME: snail_job
      SPRING_DATASOURCE_PASSWORD: snail_job_password

      # Redis configuration (for distributed locks)
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379

      # Server configuration
      SERVER_PORT: 8888
      ADMIN_PORT: 9999

    depends_on:
      - postgres
      - redis
    networks:
      - smart-admin-network

  postgres:
    image: postgres:16-alpine
    container_name: postgres-snail-job
    environment:
      POSTGRES_DB: snail_job
      POSTGRES_USER: snail_job
      POSTGRES_PASSWORD: snail_job_password
    volumes:
      - postgres-snail-job-data:/var/lib/postgresql/data
    networks:
      - smart-admin-network

  redis:
    image: redis:7.2-alpine
    container_name: redis-snail-job
    networks:
      - smart-admin-network

volumes:
  postgres-snail-job-data:

networks:
  smart-admin-network:
    external: true
```

**Start Snail-Job Server**:
```bash
# Start containers
docker-compose up -d

# Check logs
docker logs -f snail-job-server

# Access admin console
open http://localhost:9999
# Login: admin / admin123
```

#### 5.1.2 Job Registration via Admin Console

**Steps**:
1. Navigate to http://localhost:9999
2. Login with admin credentials
3. Go to "Job Management" → "Create Job"
4. Fill in job details:
   - Namespace: `smart-admin`
   - Group: `default`
   - Job Name: `vipTierEvaluation`
   - Job Handler: `vipTierEvaluation` (must match @JobHandler name)
   - Cron Expression: `0 0 0 * * ?`
   - Timeout: `300000` (5 min)
   - Max Retry: `3`
   - Route Strategy: `ROUND` (Round-robin)
5. Click "Save" → "Enable"

### 5.2 Monitoring Dashboard

**Grafana Dashboard** (Scheduled Jobs Overview):
```json
{
  "dashboard": {
    "title": "Scheduled Jobs Monitoring",
    "panels": [
      {
        "title": "Job Execution Rate",
        "targets": [
          {
            "expr": "sum(rate(job_execution_count[5m])) by (job_name)",
            "legendFormat": "{{job_name}}"
          }
        ]
      },
      {
        "title": "Job Success Rate",
        "targets": [
          {
            "expr": "sum(rate(job_execution_count{status=\"success\"}[5m])) by (job_name) / sum(rate(job_execution_count[5m])) by (job_name)",
            "legendFormat": "{{job_name}}"
          }
        ],
        "yaxis": { "format": "percentunit" }
      },
      {
        "title": "Job Execution Duration (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(job_execution_duration_seconds_bucket[5m])) by (le, job_name))",
            "legendFormat": "{{job_name}}"
          }
        ],
        "yaxis": { "format": "s" }
      },
      {
        "title": "Failed Jobs (Last Hour)",
        "targets": [
          {
            "expr": "sum(increase(job_execution_failure[1h])) by (job_name)",
            "legendFormat": "{{job_name}}"
          }
        ]
      }
    ]
  }
}
```

### 5.3 Troubleshooting

#### 5.3.1 Job Not Executing

**Symptom**: Job registered but never executes

**Diagnosis**:
```sql
-- Check job status
SELECT job_name, job_status, cron_expression, next_trigger_at
FROM sj_job
WHERE namespace = 'smart-admin'
  AND group_name = 'default';

-- Check if executor is registered
SELECT executor_name, status, last_heartbeat_dt
FROM sj_executor
WHERE namespace = 'smart-admin';
```

**Solution**:
1. Verify job status = 1 (enabled)
2. Check cron expression is valid
3. Ensure executor pods are running and connected
4. Check Redis connectivity (leader election requires Redis)

#### 5.3.2 Job Timeout

**Symptom**: Job execution exceeds timeout and is killed

**Diagnosis**:
```sql
-- Find jobs exceeding timeout
SELECT j.job_name, jt.task_batch_id, (jt.finish_dt - jt.create_dt) AS duration_ms
FROM sj_job_task jt
JOIN sj_job j ON jt.job_id = j.id
WHERE (jt.finish_dt - jt.create_dt) > j.executor_timeout
ORDER BY duration_ms DESC
LIMIT 10;
```

**Solution**:
1. Increase job timeout in job configuration
2. Optimize job logic (add pagination for large datasets)
3. Split job into smaller sub-tasks

#### 5.3.3 Duplicate Job Execution

**Symptom**: Job executes multiple times concurrently

**Diagnosis**:
- Check Redis lock acquisition logs
- Verify only one pod has leader election lock

**Solution**:
1. Ensure Redis is healthy and reachable
2. Check executor route strategy (use `FIRST` for critical jobs)
3. Add distributed lock in job handler if necessary

---

## 6. Appendices

### 6.1 Complete Job Catalog

| Job Name | Schedule | Timeout | Retry | Critical | Related Doc |
|----------|----------|---------|-------|----------|-------------|
| vipTierEvaluation | Daily 00:00 | 5 min | 3 | No | P1-11 |
| bonusExpiration | Hourly | 1 min | 3 | No | P1-12 |
| dailyLedgerReconciliation | Daily 01:00 | 10 min | 1 | Yes | P0-01 |
| withdrawalAutoApproval | Every 5 min | 2 min | 3 | No | P0-03 |
| gameCatalogSync | Daily 02:00 | 15 min | 3 | No | P1-09 |
| inactivePlayerCleanup | Weekly Sun 03:00 | 30 min | 1 | No | P1-16 |
| monthlyGgrReport | Monthly 1st 04:00 | 60 min | 1 | Yes | P1-16 |
| rtpVerification | Daily 05:00 | 10 min | 3 | Yes | P1-09 |

### 6.2 Snail-Job Route Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| ROUND | Round-robin across executors | Balanced load distribution |
| RANDOM | Random executor selection | Simple load balancing |
| FIRST | Always first available executor | Critical jobs (ledger reconciliation) |
| LAST | Always last available executor | Low-priority jobs |
| CONSISTENT_HASH | Consistent hashing by param | Tenant-specific jobs |
| SHARDING | Sharding by ID range | Large batch processing |

### 6.3 Related Documentation

- [P0-01: Double-Entry Ledger Schema](../P0-critical/01-double-entry-ledger-schema.md) - Daily reconciliation job
- [P1-11: VIP System Design](../P1-important/11-vip-system-design.md) - VIP tier evaluation job
- [P1-12: Bonus Engine](../P1-important/12-bonus-engine.md) - Bonus expiration job
- [P1-16: Compliance & Audit](../P1-important/16-compliance-audit.md) - Regulatory reporting jobs
- [backend_project.md](../../backend_project.md) - Overall platform architecture

---

## Document End

**Version**: 1.0.0
**Status**: Draft
**Next Review**: After Snail-Job POC deployment
**Feedback**: Operations team review required for monitoring setup