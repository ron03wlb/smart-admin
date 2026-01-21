# Batch Processing Example

Complete example of batch send and batch consume with graceful degradation.

## Overview

This example demonstrates:
- Batch sending multiple messages efficiently
- Batch consuming with `AbstractBatchKafkaListener`
- Graceful degradation (batch fails → process individually)
- Batch database inserts

**Use case**: Import employee data in bulk

---

## Step 1: Configuration

**File**: `sa-admin/src/main/resources/dev/sa-base.yaml`

```yaml
smart:
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092

    producer:
      batch-size: 16384          # 16KB batch
      linger-ms: 10              # Wait 10ms for batching
      compression-type: lz4      # Compress batches

    consumer:
      max-poll-records: 500      # Fetch up to 500 records
      fetch-min-bytes: 1024      # Wait for 1KB before returning
      fetch-max-wait-ms: 500     # Max wait time
```

---

## Step 2: Domain Objects

**Employee DTO**:
```java
package net.lab1024.sa.admin.module.business.sample.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeImportDTO {
    private String employeeId;
    private String name;
    private String email;
    private String department;
    private BigDecimal salary;
}
```

---

## Step 3: Batch Producer

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/sample/controller/KafkaBatchController.java`

```java
package net.lab1024.sa.admin.module.business.sample.controller;

import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.sample.domain.dto.EmployeeImportDTO;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.service.KafkaProducerService;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Kafka Batch Example")
@RestController
@RequestMapping("/business/sample/kafka/batch")
@RequiredArgsConstructor
@Slf4j
public class KafkaBatchController {

    private final KafkaProducerService kafkaProducerService;

    private static final String TOPIC = "smart-admin-employee-import";

    @Operation(summary = "Import employees in batch")
    @PostMapping("/import")
    public ResponseDTO<String> importEmployees(@RequestParam(defaultValue = "100") int count) {
        log.info("Starting batch import | Count: {}", count);

        long startTime = System.currentTimeMillis();

        // Generate test employees
        List<EmployeeImportDTO> employees = generateEmployees(count);

        // Convert to JSON messages
        List<String> messages = employees.stream()
            .map(JSON::toJSONString)
            .toList();

        // Send batch asynchronously
        CompletableFuture<List<SendResult<String, String>>> future =
            kafkaProducerService.sendBatchAsync(TOPIC, messages);

        // Wait for completion
        future.thenAccept(results -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Batch send completed | Count: {} | Duration: {}ms | Throughput: {}/sec",
                results.size(), duration, (results.size() * 1000 / Math.max(duration, 1)));
        }).exceptionally(ex -> {
            log.error("❌ Batch send failed", ex);
            return null;
        });

        return ResponseDTO.ok("Batch import started: " + count + " employees");
    }

    private List<EmployeeImportDTO> generateEmployees(int count) {
        List<EmployeeImportDTO> employees = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            employees.add(EmployeeImportDTO.builder()
                .employeeId("EMP" + String.format("%05d", i))
                .name("Employee " + i)
                .email("emp" + i + "@smartadmin.com")
                .department("Department " + (i % 5 + 1))
                .salary(new BigDecimal("50000").add(new BigDecimal(i * 100)))
                .build());
        }
        return employees;
    }
}
```

---

## Step 4: Batch Consumer with Graceful Degradation

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/sample/listener/EmployeeImportListener.java`

```java
package net.lab1024.sa.admin.module.business.sample.listener;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.business.employee.manager.EmployeeManager;
import net.lab1024.sa.admin.module.business.sample.domain.dto.EmployeeImportDTO;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.common.mq.kafka.listener.AbstractBatchKafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmployeeImportListener extends AbstractBatchKafkaListener<EmployeeImportDTO> {

    private final EmployeeManager employeeManager;

    @KafkaListener(
        topics = "smart-admin-employee-import",
        groupId = "employee-import-processor",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void onBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        handleBatch(records, ack);
    }

    /**
     * Batch processing - Efficient database batch insert
     */
    @Override
    protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
        log.info("📦 Processing batch | Size: {}", records.size());
        long startTime = System.currentTimeMillis();

        // Parse all records
        List<EmployeeImportDTO> employeeDTOs = records.stream()
            .map(record -> JSON.parseObject(record.value(), EmployeeImportDTO.class))
            .toList();

        // Batch insert to database
        List<EmployeeEntity> entities = SmartBeanUtil.copyList(employeeDTOs, EmployeeEntity.class);
        employeeManager.batchInsert(entities);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Batch processed successfully | Size: {} | Duration: {}ms | Throughput: {}/sec",
            records.size(), duration, (records.size() * 1000 / Math.max(duration, 1)));
    }

    /**
     * Fallback - Individual processing when batch fails
     */
    @Override
    protected void doHandle(ConsumerRecord<String, String> record) {
        log.warn("⚠️ Fallback to individual processing | Key: {}", record.key());

        EmployeeImportDTO employeeDTO = JSON.parseObject(record.value(), EmployeeImportDTO.class);
        EmployeeEntity entity = SmartBeanUtil.copy(employeeDTO, EmployeeEntity.class);

        // Individual insert
        employeeManager.insert(entity);

        log.info("✅ Individual record processed | EmployeeId: {}", employeeDTO.getEmployeeId());
    }
}
```

---

## Step 5: Employee Manager with Batch Operations

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/business/employee/manager/EmployeeManager.java`

```java
package net.lab1024.sa.admin.module.business.employee.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.business.employee.domain.entity.EmployeeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class EmployeeManager extends ServiceImpl<EmployeeDao, EmployeeEntity> {

    /**
     * Batch insert with transaction
     */
    @Transactional(rollbackFor = Throwable.class)
    public void batchInsert(List<EmployeeEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        log.info("Batch inserting employees | Count: {}", entities.size());

        // MyBatis Plus batch insert (uses JDBC batch)
        this.saveBatch(entities, 500);  // Batch size: 500

        log.info("Batch insert completed | Count: {}", entities.size());
    }

    /**
     * Single insert
     */
    @Transactional(rollbackFor = Throwable.class)
    public void insert(EmployeeEntity entity) {
        this.save(entity);
    }
}
```

---

## Step 6: Run the Example

### Create Topic

```bash
docker exec smart-admin-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic smart-admin-employee-import \
  --partitions 3 --replication-factor 1
```

### Start Application

```bash
./gradlew :sa-admin:bootRun
```

### Import Employees

**Import 100 employees**:
```bash
curl -X POST "http://localhost:1024/business/sample/kafka/batch/import?count=100"
```

**Import 1000 employees**:
```bash
curl -X POST "http://localhost:1024/business/sample/kafka/batch/import?count=1000"
```

---

## Expected Output

**Producer logs**:
```
INFO  KafkaBatchController - Starting batch import | Count: 100
INFO  KafkaBatchController - ✅ Batch send completed | Count: 100 | Duration: 45ms | Throughput: 2222/sec
```

**Consumer logs** (batch processing):
```
INFO  EmployeeImportListener - 📦 Processing batch | Size: 100
INFO  EmployeeManager - Batch inserting employees | Count: 100
INFO  EmployeeManager - Batch insert completed | Count: 100
INFO  EmployeeImportListener - ✅ Batch processed successfully | Size: 100 | Duration: 234ms | Throughput: 427/sec
```

---

## Testing Graceful Degradation

### Simulate Batch Failure

Modify `EmployeeImportListener.doBatchHandle()`:

```java
@Override
protected void doBatchHandle(List<ConsumerRecord<String, String>> records) {
    // Simulate batch processing error
    if (records.size() > 50) {
        throw new RuntimeException("Batch too large - simulated error");
    }

    // Normal batch processing
    // ...
}
```

### Send Large Batch

```bash
curl -X POST "http://localhost:1024/business/sample/kafka/batch/import?count=100"
```

**Expected logs**:
```
INFO  EmployeeImportListener - 📦 Processing batch | Size: 100
ERROR EmployeeImportListener - ❌ Batch processing failed, attempting individual processing
WARN  EmployeeImportListener - ⚠️ Fallback to individual processing | Key: null
INFO  EmployeeImportListener - ✅ Individual record processed | EmployeeId: EMP00001
WARN  EmployeeImportListener - ⚠️ Fallback to individual processing | Key: null
INFO  EmployeeImportListener - ✅ Individual record processed | EmployeeId: EMP00002
...
(100 individual records processed)
```

---

## Performance Comparison

| Scenario | Records | Duration | Throughput | Notes |
|----------|---------|----------|------------|-------|
| Batch processing | 100 | 234ms | 427/sec | Efficient |
| Individual processing | 100 | 1,450ms | 69/sec | 6x slower |
| Batch processing | 1,000 | 1,890ms | 529/sec | Scales well |

**Key Insight**: Batch processing is **6x faster** than individual processing.

---

## Monitoring

**Check consumer lag**:
```bash
docker exec smart-admin-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group employee-import-processor
```

**Expected output**:
```
TOPIC                       PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
smart-admin-employee-import 0          334             334             0
smart-admin-employee-import 1          333             333             0
smart-admin-employee-import 2          333             333             0
```

---

## Best Practices Demonstrated

1. ✅ **Async batch sending** - Non-blocking CompletableFuture
2. ✅ **Batch consumer** - Process multiple messages efficiently
3. ✅ **Graceful degradation** - Fallback to individual processing
4. ✅ **Batch database operations** - MyBatis Plus `saveBatch()`
5. ✅ **Transaction management** - `@Transactional` in Manager layer
6. ✅ **Performance metrics** - Log throughput and duration

---

## See Also

- [Batch Operations Guide](/kafka/guides/batch-operations) - Detailed batch patterns
- [Basic Example](/kafka/examples/basic-example) - Simple producer/consumer
- [Performance Tuning](/kafka/operations/performance-tuning) - Optimization strategies

---

**Last Updated**: 2026-01-22
