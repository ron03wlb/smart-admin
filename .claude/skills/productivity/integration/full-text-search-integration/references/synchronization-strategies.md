# Elasticsearch Synchronization Strategies

**Skill:** full-text-search-integration
**Component:** Elasticsearch / Data Synchronization / CDC
**Purpose:** Keep MySQL and Elasticsearch in sync for SmartAdmin

---

## Synchronization Strategies Overview

| Strategy | Latency | Complexity | Reliability | Use Case |
|----------|---------|------------|-------------|----------|
| **Dual-Write** | Low (< 100ms) | Low | Medium | Simple applications, write-heavy |
| **CDC (Debezium/Canal)** | Low (< 1s) | High | High | Production systems, reliability critical |
| **Logstash ETL** | High (minutes) | Medium | High | Batch processing, legacy systems |
| **Spring Events** | Low (< 100ms) | Low | Medium | Monolithic applications |

---

## Pattern 1: Dual-Write (Application-Level)

### Synchronous Dual-Write

```java
package net.lab1024.sa.business.product.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.dao.ProductDao;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import net.lab1024.sa.business.product.domain.entity.ProductEntity;
import net.lab1024.sa.business.product.repository.ProductSearchRepository;
import net.lab1024.sa.foundation.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductManager {

    private final ProductDao productDao;
    private final ProductSearchRepository productSearchRepository;

    /**
     * Insert product to MySQL and Elasticsearch (synchronous)
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertProduct(ProductEntity product) {
        // 1. Insert to MySQL
        productDao.insert(product);

        // 2. Insert to Elasticsearch
        try {
            ProductDocument document = SmartBeanUtil.copy(product, ProductDocument.class);
            productSearchRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to sync product to Elasticsearch: {}", product.getProductId(), e);
            // WARNING: MySQL committed, ES failed - data inconsistency!
            // Options:
            // - Throw exception (rollback MySQL)
            // - Log to dead letter table for retry
            // - Send to message queue for async retry
            throw e;  // Rollback MySQL transaction
        }
    }

    /**
     * Update product in MySQL and Elasticsearch
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductEntity product) {
        // 1. Update MySQL
        productDao.updateById(product);

        // 2. Update Elasticsearch
        try {
            ProductDocument document = SmartBeanUtil.copy(product, ProductDocument.class);
            productSearchRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to sync product update to Elasticsearch: {}", product.getProductId(), e);
            throw e;  // Rollback MySQL transaction
        }
    }

    /**
     * Delete product from MySQL and Elasticsearch
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        // 1. Delete from MySQL
        productDao.deleteById(productId);

        // 2. Delete from Elasticsearch
        try {
            productSearchRepository.deleteById(productId);
        } catch (Exception e) {
            log.error("Failed to delete product from Elasticsearch: {}", productId, e);
            throw e;  // Rollback MySQL transaction
        }
    }
}
```

### Asynchronous Dual-Write (Event-Driven)

```java
package net.lab1024.sa.business.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lab1024.sa.business.product.domain.entity.ProductEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangedEvent {
    private ProductEntity product;
    private EventType eventType;

    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}
```

```java
package net.lab1024.sa.business.product.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.dao.ProductDao;
import net.lab1024.sa.business.product.domain.entity.ProductEntity;
import net.lab1024.sa.business.product.event.ProductChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductManager {

    private final ProductDao productDao;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Insert product to MySQL, then publish event for ES sync
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertProduct(ProductEntity product) {
        productDao.insert(product);

        // Publish event (processed after transaction commit)
        eventPublisher.publishEvent(new ProductChangedEvent(
            product, ProductChangedEvent.EventType.CREATED
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductEntity product) {
        productDao.updateById(product);
        eventPublisher.publishEvent(new ProductChangedEvent(
            product, ProductChangedEvent.EventType.UPDATED
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        ProductEntity product = productDao.selectById(productId);
        productDao.deleteById(productId);
        eventPublisher.publishEvent(new ProductChangedEvent(
            product, ProductChangedEvent.EventType.DELETED
        ));
    }
}
```

```java
package net.lab1024.sa.business.product.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import net.lab1024.sa.business.product.event.ProductChangedEvent;
import net.lab1024.sa.business.product.repository.ProductSearchRepository;
import net.lab1024.sa.foundation.core.util.SmartBeanUtil;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductSearchRepository productSearchRepository;

    /**
     * Listen to product changed events and sync to Elasticsearch
     * @TransactionalEventListener ensures event is processed after MySQL transaction commits
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductChanged(ProductChangedEvent event) {
        try {
            switch (event.getEventType()) {
                case CREATED:
                case UPDATED:
                    ProductDocument document = SmartBeanUtil.copy(
                        event.getProduct(), ProductDocument.class
                    );
                    productSearchRepository.save(document);
                    log.info("Synced product to Elasticsearch: {}", event.getProduct().getProductId());
                    break;

                case DELETED:
                    productSearchRepository.deleteById(event.getProduct().getProductId());
                    log.info("Deleted product from Elasticsearch: {}", event.getProduct().getProductId());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to sync product to Elasticsearch", e);
            // TODO: Send to dead letter queue for manual retry
        }
    }
}
```

**Configuration (enable async processing):**

```java
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("es-sync-");
        executor.initialize();
        return executor;
    }
}
```

---

## Pattern 2: Change Data Capture (CDC) with Debezium

### Debezium Configuration

```yaml
# docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: smartadmin
    command:
      - --server-id=1
      - --log-bin=mysql-bin
      - --binlog_format=ROW
      - --binlog_row_image=FULL
    ports:
      - "3306:3306"

  debezium:
    image: debezium/connect:2.5
    depends_on:
      - kafka
      - mysql
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: debezium_configs
      OFFSET_STORAGE_TOPIC: debezium_offsets
      STATUS_STORAGE_TOPIC: debezium_statuses
    ports:
      - "8083:8083"
```

### Debezium Connector Configuration

```json
// POST http://localhost:8083/connectors
{
  "name": "smartadmin-mysql-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "root",
    "database.server.id": "184054",
    "database.server.name": "smartadmin",
    "database.include.list": "smartadmin",
    "table.include.list": "smartadmin.t_product",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "schema-changes.smartadmin",
    "include.schema.changes": "false",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
    "transforms.route.replacement": "$3"
  }
}
```

### Kafka Consumer for ES Sync

```java
package net.lab1024.sa.business.product.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import net.lab1024.sa.business.product.repository.ProductSearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCDCConsumer {

    private final ProductSearchRepository productSearchRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consume Debezium CDC events from Kafka
     */
    @KafkaListener(topics = "t_product", groupId = "elasticsearch-sync-group")
    public void consumeProductChange(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String op = event.get("op").asText();  // c=create, u=update, d=delete

            switch (op) {
                case "c":  // CREATE
                case "u":  // UPDATE
                    JsonNode after = event.get("after");
                    ProductDocument document = objectMapper.treeToValue(after, ProductDocument.class);
                    productSearchRepository.save(document);
                    log.info("Synced product {} to Elasticsearch (op: {})",
                        document.getProductId(), op);
                    break;

                case "d":  // DELETE
                    JsonNode before = event.get("before");
                    Long productId = before.get("product_id").asLong();
                    productSearchRepository.deleteById(productId);
                    log.info("Deleted product {} from Elasticsearch", productId);
                    break;

                case "r":  // READ (snapshot)
                    // Handle initial snapshot
                    JsonNode snapshot = event.get("after");
                    ProductDocument snapshotDoc = objectMapper.treeToValue(snapshot, ProductDocument.class);
                    productSearchRepository.save(snapshotDoc);
                    log.info("Snapshot: Synced product {} to Elasticsearch",
                        snapshotDoc.getProductId());
                    break;

                default:
                    log.warn("Unknown operation type: {}", op);
            }

        } catch (Exception e) {
            log.error("Failed to process CDC event", e);
            // TODO: Send to dead letter queue
        }
    }
}
```

---

## Pattern 3: Logstash ETL Pipeline

### Logstash Configuration

```conf
# logstash/product-sync.conf
input {
  jdbc {
    jdbc_driver_library => "/usr/share/logstash/mysql-connector-java.jar"
    jdbc_driver_class => "com.mysql.cj.jdbc.Driver"
    jdbc_connection_string => "jdbc:mysql://localhost:3306/smartadmin"
    jdbc_user => "root"
    jdbc_password => "root"
    statement => "SELECT * FROM t_product WHERE updated_at > :sql_last_value ORDER BY updated_at ASC"
    use_column_value => true
    tracking_column => "updated_at"
    tracking_column_type => "timestamp"
    schedule => "*/5 * * * *"  # Run every 5 minutes
    last_run_metadata_path => "/usr/share/logstash/.logstash_jdbc_last_run"
  }
}

filter {
  # Transform snake_case to camelCase
  mutate {
    rename => {
      "product_id" => "productId"
      "product_name" => "productName"
      "created_at" => "createdAt"
      "updated_at" => "updatedAt"
    }
  }

  # Convert price to double
  mutate {
    convert => {
      "price" => "float"
    }
  }
}

output {
  elasticsearch {
    hosts => ["http://localhost:9200"]
    index => "products"
    document_id => "%{productId}"
    action => "index"
  }

  stdout {
    codec => rubydebug
  }
}
```

**Docker Compose for Logstash:**

```yaml
# docker-compose.yml
version: '3.8'
services:
  logstash:
    image: docker.elastic.co/logstash/logstash:8.12.0
    volumes:
      - ./logstash/product-sync.conf:/usr/share/logstash/pipeline/product-sync.conf
      - ./mysql-connector-java.jar:/usr/share/logstash/mysql-connector-java.jar
    environment:
      LS_JAVA_OPTS: "-Xmx512m -Xms512m"
    ports:
      - "5044:5044"
      - "9600:9600"
    depends_on:
      - elasticsearch
```

---

## Pattern 4: Full Data Resync

### Batch Resync Service

```java
package net.lab1024.sa.business.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.dao.ProductDao;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import net.lab1024.sa.business.product.domain.entity.ProductEntity;
import net.lab1024.sa.business.product.repository.ProductSearchRepository;
import net.lab1024.sa.foundation.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductResyncService {

    private final ProductDao productDao;
    private final ProductSearchRepository productSearchRepository;

    /**
     * Full resync from MySQL to Elasticsearch
     * WARNING: This is a heavy operation, run during off-peak hours
     */
    public void fullResync() {
        log.info("Starting full product resync from MySQL to Elasticsearch");

        long totalCount = productDao.selectCount(new LambdaQueryWrapper<>());
        log.info("Total products to sync: {}", totalCount);

        int pageSize = 1000;
        long totalPages = (totalCount + pageSize - 1) / pageSize;

        for (long pageNum = 1; pageNum <= totalPages; pageNum++) {
            try {
                // Fetch batch from MySQL
                Page<ProductEntity> page = productDao.selectPage(
                    new Page<>(pageNum, pageSize),
                    new LambdaQueryWrapper<>()
                );

                // Convert to documents
                List<ProductDocument> documents = page.getRecords().stream()
                    .map(entity -> SmartBeanUtil.copy(entity, ProductDocument.class))
                    .collect(Collectors.toList());

                // Batch insert to Elasticsearch
                productSearchRepository.saveAll(documents);

                log.info("Synced batch {}/{} ({} products)",
                    pageNum, totalPages, documents.size());

                // Sleep to avoid overwhelming ES
                Thread.sleep(500);

            } catch (Exception e) {
                log.error("Failed to sync batch {}/{}", pageNum, totalPages, e);
                // Continue with next batch
            }
        }

        log.info("Full product resync completed");
    }

    /**
     * Incremental resync (only updated records since timestamp)
     */
    public void incrementalResync(LocalDateTime since) {
        log.info("Starting incremental resync since: {}", since);

        List<ProductEntity> updatedProducts = productDao.selectList(
            new LambdaQueryWrapper<ProductEntity>()
                .ge(ProductEntity::getUpdatedAt, since)
        );

        List<ProductDocument> documents = updatedProducts.stream()
            .map(entity -> SmartBeanUtil.copy(entity, ProductDocument.class))
            .collect(Collectors.toList());

        productSearchRepository.saveAll(documents);

        log.info("Incremental resync completed: {} products updated", documents.size());
    }
}
```

### Scheduled Resync Job

```java
package net.lab1024.sa.business.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.service.ProductResyncService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductResyncJob {

    private final ProductResyncService resyncService;

    /**
     * Daily incremental resync (last 24 hours)
     * Cron: 0 0 3 * * ? (3 AM daily)
     */
    @JobExecutor(name = "productIncrementalResync")
    public ExecuteResult execute(String param) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            resyncService.incrementalResync(since);
            return ExecuteResult.success("Incremental resync completed");
        } catch (Exception e) {
            log.error("Incremental resync failed", e);
            return ExecuteResult.failure("Failed: " + e.getMessage());
        }
    }
}
```

---

## Best Practices

1. **Dual-Write:**
   - ✅ Simple and low latency
   - ✅ Good for write-heavy applications
   - ⚠️ Risk of inconsistency (MySQL succeeds, ES fails)
   - ⚠️ No support for bulk operations outside application

2. **CDC (Debezium/Canal):**
   - ✅ Guaranteed consistency (captures binlog)
   - ✅ Low latency (< 1 second)
   - ✅ Supports all MySQL operations (including bulk updates)
   - ⚠️ Complex setup (Kafka, Debezium, consumers)
   - ⚠️ Requires MySQL binlog enabled

3. **Logstash ETL:**
   - ✅ Simple configuration
   - ✅ Works with any SQL database
   - ⚠️ High latency (batch processing)
   - ⚠️ Inefficient for real-time sync

4. **Spring Events:**
   - ✅ Simple implementation
   - ✅ Low latency
   - ⚠️ Same process only (not suitable for microservices)
   - ⚠️ Risk of inconsistency (async processing)

5. **General Recommendations:**
   - Use **Dual-Write** for simple applications
   - Use **CDC (Debezium)** for production systems requiring reliability
   - Use **Logstash** for legacy systems or batch processing
   - Always implement **full resync** as fallback mechanism
   - Monitor sync lag and failures

---

**Next:** [Performance Optimization](performance-optimization.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
