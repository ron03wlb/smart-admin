# LiteFlow Module Architecture

## Overview

The LiteFlow module is designed as a SmartAdmin support module (`sa-base/support/liteflow`) that provides comprehensive flow orchestration and rule engine capabilities. It strictly follows SmartAdmin's layered architecture pattern with database-backed storage, two-level caching, and hot-reload support.

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Framework** | Spring Boot | 3.5.4 | Application framework |
| **Rule Engine** | LiteFlow | 2.15.3 | Flow orchestration and execution |
| **Script Engine** | QLExpress | 2.15.3 | Dynamic business logic execution |
| **Database** | PostgreSQL | 16 | Persistent storage for flows and logs |
| **Cache** | JetCache (Caffeine + Redis) | 2.7.7 | Two-level caching |
| **Validation** | Jakarta Validation | 3.x | Request validation |
| **ORM** | MyBatis-Plus | 3.5.12 | Data access layer |
| **Security** | Sa-Token | 1.44.0 | Authentication and authorization |
| **API Docs** | Knife4j | 4.6.0 | API documentation |

## Layered Architecture

Following SmartAdmin's strict layered architecture:

```
┌─────────────────────────────────────────────┐
│          Controller Layer                   │  REST APIs, Request validation
│  (LiteFlowChainController,                  │  Permission control (@SaCheckPermission)
│   LiteFlowExecutionController, etc.)        │
└─────────────────┬───────────────────────────┘
                  │ Calls (ONLY Service layer)
                  ▼
┌─────────────────────────────────────────────┐
│          Service Layer                       │  Business logic orchestration
│  (LiteFlowChainService,                     │  Coordinates Manager/Dao calls
│   LiteFlowExecutionService, etc.)           │
└─────────────────┬───────────────────────────┘
                  │ Calls (Manager OR Dao)
                  ▼
┌─────────────────────────────────────────────┐
│          Manager Layer                       │  Transactions, Cache, Complex logic
│  (LiteFlowChainManager,                     │  @Transactional(rollbackFor = Throwable.class)
│   LiteFlowCacheManager,                     │  Cache eviction
│   LiteFlowMetricsManager)                   │
└─────────────────┬───────────────────────────┘
                  │ Calls (ONLY Dao layer)
                  ▼
┌─────────────────────────────────────────────┐
│          Dao Layer                           │  Data access (MyBatis-Plus)
│  (LiteFlowChainDao,                         │  Type-safe LambdaQueryWrapper
│   LiteFlowScriptDao,                        │  BaseMapper extensions
│   LiteFlowExecutionLogDao, etc.)            │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│          Entity Layer                        │  Database entities
│  (LiteFlowChainEntity,                      │  @TableName, @TableId
│   LiteFlowScriptEntity, etc.)               │  MyBatis-Plus annotations
└─────────────────────────────────────────────┘
```

## Module Structure

```
sa-base/support/liteflow/
├── build.gradle.kts                    # Dependency configuration
├── src/main/
│   ├── java/net/lab1024/sa/base/module/support/liteflow/
│   │   ├── config/                     # Auto-configuration
│   │   │   ├── LiteFlowAutoConfiguration.java
│   │   │   ├── LiteFlowProperties.java
│   │   │   └── LiteFlowDataSourceConfiguration.java
│   │   ├── controller/                 # REST API layer
│   │   │   ├── LiteFlowChainController.java
│   │   │   ├── LiteFlowScriptController.java
│   │   │   ├── LiteFlowExecutionController.java
│   │   │   └── LiteFlowMonitorController.java
│   │   ├── service/                    # Business logic layer
│   │   │   ├── LiteFlowChainService.java
│   │   │   ├── LiteFlowScriptService.java
│   │   │   ├── LiteFlowExecutionService.java
│   │   │   └── LiteFlowMonitorService.java
│   │   ├── manager/                    # Transaction & cache layer
│   │   │   ├── LiteFlowChainManager.java
│   │   │   ├── LiteFlowCacheManager.java
│   │   │   └── LiteFlowMetricsManager.java
│   │   ├── dao/                        # Data access layer
│   │   │   ├── LiteFlowChainDao.java
│   │   │   ├── LiteFlowScriptDao.java
│   │   │   ├── LiteFlowExecutionLogDao.java
│   │   │   └── LiteFlowExecutionMetricsDao.java
│   │   ├── domain/                     # Domain objects
│   │   │   ├── entity/
│   │   │   │   ├── LiteFlowChainEntity.java
│   │   │   │   ├── LiteFlowScriptEntity.java
│   │   │   │   ├── LiteFlowExecutionLogEntity.java
│   │   │   │   └── LiteFlowExecutionMetricsEntity.java
│   │   │   ├── form/
│   │   │   │   ├── LiteFlowChainAddForm.java
│   │   │   │   ├── LiteFlowChainUpdateForm.java
│   │   │   │   ├── LiteFlowChainQueryForm.java
│   │   │   │   ├── LiteFlowScriptAddForm.java
│   │   │   │   ├── LiteFlowScriptUpdateForm.java
│   │   │   │   ├── LiteFlowScriptQueryForm.java
│   │   │   │   ├── LiteFlowExecutionForm.java
│   │   │   │   └── LiteFlowMonitorQueryForm.java
│   │   │   └── vo/
│   │   │       ├── LiteFlowChainVO.java
│   │   │       ├── LiteFlowScriptVO.java
│   │   │       ├── LiteFlowExecutionLogVO.java
│   │   │       ├── LiteFlowExecutionResultVO.java
│   │   │       └── LiteFlowMetricsVO.java
│   │   ├── constant/                   # Constants & enums
│   │   │   ├── LiteFlowConst.java
│   │   │   ├── LiteFlowChainTypeEnum.java
│   │   │   ├── LiteFlowScriptTypeEnum.java
│   │   │   └── LiteFlowExecutionStatusEnum.java
│   │   ├── core/                       # LiteFlow integration
│   │   │   ├── datasource/
│   │   │   │   └── SmartLiteFlowDataSource.java
│   │   │   ├── executor/
│   │   │   │   └── SmartFlowExecutor.java
│   │   │   ├── listener/
│   │   │   │   └── LiteFlowExecutionListener.java
│   │   │   └── reload/
│   │   │       └── LiteFlowReloadCommand.java
│   │   └── exception/
│   │       └── LiteFlowException.java
│   └── resources/
│       ├── META-INF/spring/
│       │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       └── mapper/
│           ├── LiteFlowChainMapper.xml
│           ├── LiteFlowScriptMapper.xml
│           ├── LiteFlowExecutionLogMapper.xml
│           └── LiteFlowExecutionMetricsMapper.xml
```

## Core Components

### 1. Auto-Configuration

**LiteFlowAutoConfiguration.java**
- Spring Boot auto-configuration class
- Conditionally enabled via `smart.liteflow.enabled=true`
- Registers core beans: SmartFlowExecutor, LiteFlowExecutionListener
- Imports LiteFlowDataSourceConfiguration

```java
@AutoConfiguration
@AutoConfigureAfter({WebAutoConfiguration.class, MybatisAutoConfiguration.class})
@ConditionalOnProperty(prefix = "smart.liteflow", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LiteFlowProperties.class)
```

**LiteFlowProperties.java**
- Configuration properties for `smart.liteflow.*` namespace
- Enables database-backed storage, execution logging, metrics
- Configures script engine type, execution timeout, reload interval

### 2. Database-Backed Flow Storage

**SmartLiteFlowDataSource.java**
- Implements LiteFlow's `RuleSource` SPI
- Loads flow/script definitions from PostgreSQL
- Enables hot-reload from database
- Integrates with SmartAdmin's DAO layer

**LiteFlowDataSourceConfiguration.java**
- Configures custom data source bean
- Creates LiteflowConfig bean with database rule source
- Supports hot-reload interval configuration

### 3. Flow Execution Engine

**SmartFlowExecutor.java**
- Wraps LiteFlow's FlowExecutor
- Adds SmartAdmin-specific logging and monitoring
- Handles execution timeout and error handling
- Integrates with execution logging system

**LiteFlowExecutionListener.java**
- Listens to flow execution events
- Captures execution metrics (duration, success/failure)
- Writes execution logs to database
- Triggers metrics aggregation

### 4. Cache Management

**LiteFlowCacheManager.java**
- JetCache two-level caching (local Caffeine + remote Redis)
- Caches flow/script definitions
- TTL: 120 min (remote), 30 min (local)
- Cache eviction on CRUD operations

```java
@CreateCache(
    name = "liteflow:chain",
    cacheType = CacheType.BOTH,
    expire = 120,
    timeUnit = TimeUnit.MINUTES,
    localExpire = 30
)
```

### 5. Transaction Management

**LiteFlowChainManager.java**
- Transactional CRUD operations for chains
- Version increment on updates
- Cache invalidation after modifications
- Hot-reload coordination

```java
@Transactional(rollbackFor = Throwable.class)
public ResponseDTO<String> update(LiteFlowChainUpdateForm updateForm) {
    // Update chain
    // Increment version
    // Evict cache
    // Reload flow engine
}
```

## Integration Patterns

### 1. Spring Boot Auto-Configuration

Registration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
net.lab1024.sa.base.module.support.liteflow.config.LiteFlowAutoConfiguration
```

### 2. Configuration Properties

```yaml
smart:
  liteflow:
    enabled: true                    # Enable module
    database-enabled: true           # Database-backed storage
    execution-log-enabled: true      # Execution logging
    metrics-enabled: true            # Metrics collection
    log-retention-days: 30           # Log retention (days)
    reload-interval: 0               # Auto-reload (0=disabled)
    default-script-type: qlexpress   # Default script engine
    execution-timeout: 30000         # Execution timeout (ms)
```

### 3. Permission Control

All APIs protected with Sa-Token permissions:
```java
@SaCheckPermission("liteflow:chain:add")
@SaCheckPermission("liteflow:chain:update")
@SaCheckPermission("liteflow:execution:execute")
@SaCheckPermission("liteflow:monitor:query")
```

### 4. Response Pattern

All APIs return SmartAdmin's ResponseDTO:
```java
// Success
return ResponseDTO.ok(data);
return ResponseDTO.ok();

// Error
return ResponseDTO.userErrorParam("Invalid parameter");
throw new BusinessException(ErrorCode.BUSINESS_ERROR);
```

### 5. Hot-Reload Integration

Integrates with SmartAdmin's Reload module:
```java
@SmartReload("liteflow")
@Component
public class LiteFlowReloadCommand extends AbstractSmartReloadCommand {
    @Override
    public ResponseDTO<String> reload(String args) {
        return liteFlowChainManager.reloadAll();
    }
}
```

## Data Flow

### Flow Execution Flow

```
1. Client Request
   ↓
2. LiteFlowExecutionController.execute()
   ↓
3. LiteFlowExecutionService.execute()
   ↓
4. SmartFlowExecutor.execute()
   ├─→ Load chain from cache (if exists)
   ├─→ Load chain from DB (if cache miss)
   ├─→ Load scripts from cache/DB
   ├─→ Execute flow (LiteFlow engine)
   ├─→ Capture execution metrics
   └─→ Write execution log
   ↓
5. Return LiteFlowExecutionResultVO
```

### CRUD Operations Flow

```
1. Client Request (Add/Update/Delete chain)
   ↓
2. LiteFlowChainController
   ↓
3. LiteFlowChainService
   ↓
4. LiteFlowChainManager (Transactional)
   ├─→ Validate request
   ├─→ Update database (DAO layer)
   ├─→ Increment version
   ├─→ Evict cache (LiteFlowCacheManager)
   └─→ Reload flow engine (flowExecutor.reloadRule())
   ↓
5. Return ResponseDTO
```

## Scalability Considerations

### 1. Horizontal Scalability
- **Current**: Single-node execution
- **Future**: Distributed execution with Redis coordination
- **Approach**: Use Redis locks for concurrent execution control

### 2. Performance Optimization
- **Two-level caching**: Reduces database queries (90%+ cache hit rate)
- **Connection pooling**: HikariCP for database connections
- **Async execution**: Optional async mode for long-running flows

### 3. High Availability
- **Database**: PostgreSQL with replication
- **Cache**: Redis with sentinel/cluster mode
- **Application**: Multiple instances behind load balancer

## Security Architecture

### 1. Authentication & Authorization
- Sa-Token for session management
- Permission-based access control (@SaCheckPermission)
- Role-based menu visibility

### 2. Input Validation
- JSR-303 validation annotations (@Valid, @NotBlank, @NotNull)
- Custom validators for business rules
- SQL injection prevention (MyBatis-Plus parameterized queries)

### 3. Script Sandboxing
- QLExpress built-in sandboxing
- Restricted API access in scripts
- Execution timeout limits

### 4. Audit Logging
- All CRUD operations logged (create_user_id, create_time)
- Execution logs with input/output parameters
- Integration with OperateLog module

## Monitoring & Observability

### 1. Execution Logging
- Real-time execution logs (t_liteflow_execution_log)
- Request tracing with request_id
- Error tracking with stack traces

### 2. Metrics Collection
- Daily aggregated metrics (t_liteflow_execution_metrics)
- Success/failure rates
- Execution time statistics (avg, max, min)

### 3. Dashboard Support
- Monitoring APIs for metrics queries
- Execution trend analysis
- Performance overview endpoints

## Migration Path

### From Evrete to LiteFlow

**Key Differences**:
| Aspect | Evrete | LiteFlow |
|--------|--------|----------|
| **Type** | Pure rule engine (Rete) | Flow orchestration + rules |
| **Configuration** | Java DSL | EL expressions (declarative) |
| **Hot Reload** | Custom implementation | Built-in support |
| **Database Storage** | No native support | Custom data source (this module) |
| **Script Support** | Limited | QLExpress, Groovy, JS, Python |

**Migration Strategy**:
1. **Parallel deployment**: Deploy LiteFlow alongside Evrete
2. **Rule conversion**: Evrete rules → LiteFlow chains + QLExpress scripts
3. **Testing & validation**: Compare execution results
4. **Gradual migration**: Migrate flows incrementally
5. **Evrete deprecation**: Remove after full migration

See [migration-guide.md](migration-guide.md) for detailed migration steps.

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Maintainer**: 1024创新实验室
