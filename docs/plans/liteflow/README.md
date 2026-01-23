# LiteFlow Module - SmartAdmin Integration

> Comprehensive flow orchestration and rule engine module for SmartAdmin, providing database-backed storage, hot-reload, and monitoring capabilities.

## Overview

The LiteFlow module integrates [LiteFlow](https://liteflow.cc/) (Dromara foundation project) into SmartAdmin as a support module, enabling:
- **Complex Business Process Orchestration** - Multi-step workflows with conditional branching and parallel execution
- **Rule Engine** - Script-based dynamic business rules (QLExpress, Groovy, JavaScript)
- **Database-Backed Storage** - PostgreSQL persistence with hot-reload support
- **Monitoring & Metrics** - Execution logging and performance analytics
- **Web UI Management** - Full CRUD APIs with permission control

## Quick Start

### 1. Enable Module

Add to `application.yml`:
```yaml
smart:
  liteflow:
    enabled: true                    # Enable LiteFlow module
    database-enabled: true           # Use database storage
    execution-log-enabled: true      # Enable execution logging
    metrics-enabled: true            # Enable metrics collection
```

### 2. Run Database Migration

Execute the migration SQL to create 4 tables:
```bash
psql -U postgres -d smartadmin -f sa-admin/src/main/resources/db/migration/V1.x__liteflow.sql
```

Tables created:
- `t_liteflow_chain` - Flow/chain definitions
- `t_liteflow_script` - Script node definitions
- `t_liteflow_execution_log` - Execution history
- `t_liteflow_execution_metrics` - Aggregated metrics

### 3. Create Your First Flow

**Step 1: Create a chain**
```bash
POST /liteflow/chain/add
```
```json
{
  "chainName": "订单处理流程",
  "chainCode": "order-process-chain",
  "chainType": 1,
  "chainData": "THEN(validateOrder, checkInventory, createOrder, sendNotification)"
}
```

**Step 2: Create script nodes**
```bash
POST /liteflow/script/add
```
```json
{
  "scriptName": "验证订单",
  "scriptCode": "validateOrder",
  "scriptType": "qlexpress",
  "scriptData": "if(order.amount <= 0) { throw new Exception('Invalid amount'); } return true;"
}
```

**Step 3: Reload flow engine**
```bash
POST /liteflow/chain/reloadAll
```

**Step 4: Execute the flow**
```bash
POST /liteflow/execution/execute
```
```json
{
  "chainCode": "order-process-chain",
  "inputParams": {
    "orderId": 12345,
    "userId": 678,
    "amount": 99.99
  }
}
```

## Architecture

### Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| LiteFlow | 2.15.3 | Flow orchestration engine |
| QLExpress | 2.15.3 | Script execution (Alibaba) |
| PostgreSQL | 16 | Persistent storage |
| JetCache | 2.7.7 | Two-level caching |
| Spring Boot | 3.5.4 | Application framework |

### Module Structure

```
sa-base/support/liteflow/
├── config/           # Auto-configuration & properties
├── controller/       # REST API controllers
├── service/          # Business logic layer
├── manager/          # Transaction & cache management
├── dao/              # Data access layer (MyBatis-Plus)
├── domain/           # Entities, Forms, VOs
├── constant/         # Constants & enums
├── core/             # LiteFlow integration
│   ├── datasource/   # Custom database data source
│   ├── executor/     # Flow executor wrapper
│   ├── listener/     # Execution event listener
│   └── reload/       # Hot-reload integration
└── exception/        # Custom exceptions
```

See [architecture.md](architecture.md) for detailed architecture documentation.

## Key Features

### 1. Database-Backed Flow Storage
- Store flow definitions in PostgreSQL
- Version control for flows (auto-increment)
- Hot-reload from database (manual or auto)
- Soft delete support

### 2. Two-Level Caching
- **Local Cache** (Caffeine): 30-minute TTL
- **Remote Cache** (Redis): 120-minute TTL
- Auto cache invalidation on CRUD operations
- 90%+ cache hit rate expected

### 3. Execution Logging & Metrics
- **Execution Logs**: Detailed logs with input/output, errors, duration
- **Aggregated Metrics**: Daily metrics by chain (success/failure rates, avg execution time)
- **Request Tracing**: request_id for distributed tracing
- **Retention Policy**: Configurable (default 30 days)

### 4. Permission Control
All APIs protected with Sa-Token permissions:
- `liteflow:chain:add` - Create flows
- `liteflow:chain:update` - Update flows
- `liteflow:chain:delete` - Delete flows
- `liteflow:execution:execute` - Execute flows
- `liteflow:monitor:query` - View monitoring data

### 5. Script Engine Support
- **QLExpress** (Primary): Alibaba's expression engine, battle-tested
- **Groovy** (Optional): Dynamic scripting
- **JavaScript** (Optional): JS engine support

## API Documentation

### Chain Management

**Create Chain**
```http
POST /liteflow/chain/add
```

**Update Chain**
```http
POST /liteflow/chain/update
```

**Delete Chain**
```http
GET /liteflow/chain/delete/{chainId}
```

**Query Chains (Paginated)**
```http
POST /liteflow/chain/queryPage
```

**Reload Flow Engine**
```http
POST /liteflow/chain/reloadAll
```

### Script Management

**Create Script**
```http
POST /liteflow/script/add
```

**Update Script**
```http
POST /liteflow/script/update
```

**Delete Script**
```http
GET /liteflow/script/delete/{scriptId}
```

### Execution

**Execute Flow**
```http
POST /liteflow/execution/execute
```

**Query Execution Logs**
```http
POST /liteflow/execution/queryLog
```

**Get Log Detail**
```http
GET /liteflow/execution/logDetail/{logId}
```

### Monitoring

**Get Metrics**
```http
POST /liteflow/monitor/metrics
```

**Get Execution Trend**
```http
GET /liteflow/monitor/trend/{chainCode}?days=7
```

**Get Overview**
```http
GET /liteflow/monitor/overview
```

See [api-specification.md](api-specification.md) for complete API documentation.

## Configuration

### application.yml

```yaml
smart:
  liteflow:
    enabled: true                    # Enable module (default: false)
    database-enabled: true           # Database-backed storage (default: true)
    execution-log-enabled: true      # Execution logging (default: true)
    metrics-enabled: true            # Metrics collection (default: true)
    log-retention-days: 30           # Log retention days (0 = forever)
    reload-interval: 0               # Auto-reload seconds (0 = disabled)
    default-script-type: qlexpress   # Default script engine
    async-enabled: false             # Async execution mode
    max-thread-pool-size: 10         # Async thread pool size
    execution-timeout: 30000         # Execution timeout (ms)
```

## Usage Examples

### Example 1: Order Processing Flow

**Chain Definition (EL Expression)**:
```
THEN(
  validateOrder,
  checkInventory,
  IF(isVipUser, THEN(applyVipDiscount, processPayment), processPayment),
  createOrder,
  WHEN(sendEmail, sendSMS, updateInventory)
)
```

**QLExpress Script (validateOrder)**:
```java
// Check order validity
if (order.amount == null || order.amount <= 0) {
    throw new Exception("Invalid order amount");
}

if (order.productId == null) {
    throw new Exception("Product ID is required");
}

// Log validation
log.info("Order validated: orderId=" + order.id);
return true;
```

### Example 2: Approval Workflow

**Chain Definition**:
```
IF(
  requiresApproval,
  THEN(
    submitForApproval,
    SWITCH(approvalLevel).to(
      level1Approval,
      level2Approval,
      level3Approval
    )
  ),
  autoApprove
)
```

### Example 3: Using LiteFlow in Business Code

**Service Layer**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;

    public ResponseDTO<OrderVO> processOrder(OrderAddForm orderForm) {
        // Execute LiteFlow chain
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-process-chain");
        executionForm.setInputParams(Map.of(
            "orderId", orderForm.getOrderId(),
            "userId", orderForm.getUserId(),
            "amount", orderForm.getAmount()
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (result.getOk()) {
            OrderVO orderVO = (OrderVO) result.getData().getOutputResult();
            return ResponseDTO.ok(orderVO);
        } else {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }
    }
}
```

## Testing

### Unit Tests
```bash
./gradlew :sa-base:support:liteflow:test
```

### Integration Tests
```bash
./gradlew :sa-admin:test --tests LiteFlowIntegrationTest
```

### Architecture Tests (ArchUnit)
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

## Migration from Evrete

If you're migrating from Evrete, see [migration-guide.md](migration-guide.md) for:
- Pattern comparison (Evrete vs LiteFlow)
- Rule conversion strategies
- Step-by-step migration guide
- Code examples

## Documentation

- **[Implementation Plan](implementation-plan.md)** - Complete implementation plan with phases
- **[Architecture](architecture.md)** - Detailed architecture and design decisions
- **[Database Schema](database-schema.md)** - Database DDL and schema design
- **[API Specification](api-specification.md)** - REST API documentation
- **[Migration Guide](migration-guide.md)** - Migration from Evrete to LiteFlow

## Performance Benchmarks

Expected performance metrics:
- **Throughput**: > 500 executions/sec
- **Response Time**: P95 < 100ms
- **Cache Hit Rate**: > 90%
- **Error Rate**: < 0.1%

## Support

- **LiteFlow Official Site**: https://liteflow.cc/
- **LiteFlow GitHub**: https://github.com/dromara/liteflow
- **SmartAdmin Documentation**: See CLAUDE.md

## Version

**Module Version**: 1.0.0
**LiteFlow Version**: 2.15.3
**Last Updated**: 2026-01-23
**Maintainer**: 1024创新实验室

---

**Next Steps**:
1. Review [implementation-plan.md](implementation-plan.md) for development roadmap
2. Check [database-schema.md](database-schema.md) for database setup
3. See [api-specification.md](api-specification.md) for API usage
4. Start with Phase 1: Foundation Setup
