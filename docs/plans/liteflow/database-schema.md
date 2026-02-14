# LiteFlow Database Schema

## Overview

The LiteFlow module uses four core PostgreSQL tables to store flow definitions, script nodes, execution logs, and aggregated metrics. All tables follow SmartAdmin naming conventions with soft delete support, audit fields, and optimized indexes.

## Schema Design Principles

1. **Soft Delete**: Use `deleted_flag` instead of physical deletion
2. **Audit Trail**: All tables have `create_user_id`, `create_user_name`, `create_time`, `update_time`
3. **Versioning**: Chain and script tables include `version` field for tracking changes
4. **Indexing**: Strategic indexes on frequently queried columns
5. **BIGSERIAL**: Auto-incrementing primary keys for scalability
6. **Constraints**: Unique constraints with `deleted_flag` to allow soft-deleted records

## Entity-Relationship Diagram

```
┌─────────────────────┐         ┌─────────────────────┐
│ t_liteflow_chain    │         │ t_liteflow_script   │
├─────────────────────┤         ├─────────────────────┤
│ chain_id (PK)       │         │ script_id (PK)      │
│ chain_code (UK)     │────┐    │ script_code (UK)    │
│ chain_name          │    │    │ script_name         │
│ chain_type          │    │    │ script_type         │
│ chain_data (EL)     │    │    │ script_data         │
│ version             │    │    │ version             │
│ status              │    │    │ status              │
│ deleted_flag        │    │    │ deleted_flag        │
└─────────────────────┘    │    └─────────────────────┘
                           │
                           │ (referenced by)
                           │
        ┌──────────────────┴────────────────────┐
        │                                        │
        ▼                                        ▼
┌───────────────────────────┐   ┌───────────────────────────┐
│ t_liteflow_execution_log  │   │ t_liteflow_execution_metrics│
├───────────────────────────┤   ├───────────────────────────┤
│ log_id (PK)               │   │ metric_id (PK)            │
│ chain_code                │   │ chain_code                │
│ request_id                │   │ metric_date               │
│ execution_status          │   │ total_count               │
│ execution_time            │   │ success_count             │
│ input_params (JSON)       │   │ failure_count             │
│ output_result (JSON)      │   │ avg_execution_time        │
│ error_message             │   │ max_execution_time        │
│ error_stack               │   │ min_execution_time        │
└───────────────────────────┘   └───────────────────────────┘
```

## Table Definitions

### 1. t_liteflow_chain

Stores flow/chain definitions with EL expressions.

**Columns**:

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| chain_id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| chain_name | VARCHAR(255) | NOT NULL | Flow display name |
| chain_code | VARCHAR(100) | NOT NULL, UNIQUE | Flow unique code (maps to LiteFlow chainName) |
| chain_type | SMALLINT | NOT NULL, DEFAULT 1 | Flow type: 1=普通;2=条件;3=循环 |
| chain_data | TEXT | NOT NULL | EL expression or flow definition |
| version | INTEGER | NOT NULL, DEFAULT 1 | Version number (incremented on update) |
| status | SMALLINT | NOT NULL, DEFAULT 1 | Status: 0=禁用;1=启用 |
| remark | VARCHAR(500) | NULL | Optional description |
| deleted_flag | SMALLINT | NOT NULL, DEFAULT 0 | Soft delete: 0=active;1=deleted |
| create_user_id | BIGINT | NULL | Creator user ID |
| create_user_name | VARCHAR(100) | NULL | Creator username |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update time |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time |

**Constraints**:
```sql
PRIMARY KEY (chain_id)
CONSTRAINT chain_code_unique UNIQUE (chain_code, deleted_flag)
```

**Indexes**:
```sql
CREATE INDEX idx_chain_code ON t_liteflow_chain(chain_code) WHERE deleted_flag = 0;
CREATE INDEX idx_status ON t_liteflow_chain(status) WHERE deleted_flag = 0;
```

**DDL**:
```sql
DROP TABLE IF EXISTS t_liteflow_chain;
CREATE TABLE t_liteflow_chain (
  chain_id BIGSERIAL,
  chain_name VARCHAR(255) NOT NULL,
  chain_code VARCHAR(100) NOT NULL,
  chain_type SMALLINT NOT NULL DEFAULT 1,
  chain_data TEXT NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  status SMALLINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL DEFAULT NULL,
  deleted_flag SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NULL DEFAULT NULL,
  create_user_name VARCHAR(100) NULL DEFAULT NULL,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chain_id),
  CONSTRAINT chain_code_unique UNIQUE (chain_code, deleted_flag)
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow流程定义表';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '主键ID';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '流程名称';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS '流程编码(唯一标识)';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '流程类型:1-普通;2-条件;3-循环';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'EL表达式定义';
COMMENT ON COLUMN t_liteflow_chain.version IS '版本号';
COMMENT ON COLUMN t_liteflow_chain.status IS '状态:0-禁用;1-启用';
COMMENT ON COLUMN t_liteflow_chain.deleted_flag IS '删除标记:0-未删除;1-已删除';

CREATE INDEX idx_chain_code ON t_liteflow_chain(chain_code) WHERE deleted_flag = 0;
CREATE INDEX idx_status ON t_liteflow_chain(status) WHERE deleted_flag = 0;
```

**Example Data**:
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, version, status) VALUES
('订单处理流程', 'order-process-chain', 1, 'THEN(validateOrder, checkInventory, createOrder, sendNotification)', 1, 1),
('用户注册流程', 'user-register-chain', 1, 'THEN(validateUser, createUser, sendWelcomeEmail)', 1, 1),
('支付审核流程', 'payment-approval-chain', 2, 'IF(isHighValue, THEN(manualReview, approve), autoApprove)', 1, 1);
```

---

### 2. t_liteflow_script

Stores script node definitions (QLExpress/Groovy/JavaScript).

**Columns**:

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| script_id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| script_name | VARCHAR(255) | NOT NULL | Script display name |
| script_code | VARCHAR(100) | NOT NULL, UNIQUE | Script code (maps to LiteFlow nodeId) |
| script_type | VARCHAR(20) | NOT NULL, DEFAULT 'qlexpress' | Script engine type |
| script_data | TEXT | NOT NULL | Script content/code |
| script_language | VARCHAR(20) | NOT NULL, DEFAULT 'qlexpress' | Script language identifier |
| version | INTEGER | NOT NULL, DEFAULT 1 | Version number |
| status | SMALLINT | NOT NULL, DEFAULT 1 | Status: 0=禁用;1=启用 |
| remark | VARCHAR(500) | NULL | Optional description |
| deleted_flag | SMALLINT | NOT NULL, DEFAULT 0 | Soft delete flag |
| create_user_id | BIGINT | NULL | Creator user ID |
| create_user_name | VARCHAR(100) | NULL | Creator username |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update time |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time |

**Constraints**:
```sql
PRIMARY KEY (script_id)
CONSTRAINT script_code_unique UNIQUE (script_code, deleted_flag)
```

**Indexes**:
```sql
CREATE INDEX idx_script_code ON t_liteflow_script(script_code) WHERE deleted_flag = 0;
CREATE INDEX idx_script_type ON t_liteflow_script(script_type);
```

**DDL**:
```sql
DROP TABLE IF EXISTS t_liteflow_script;
CREATE TABLE t_liteflow_script (
  script_id BIGSERIAL,
  script_name VARCHAR(255) NOT NULL,
  script_code VARCHAR(100) NOT NULL,
  script_type VARCHAR(20) NOT NULL DEFAULT 'qlexpress',
  script_data TEXT NOT NULL,
  script_language VARCHAR(20) NOT NULL DEFAULT 'qlexpress',
  version INTEGER NOT NULL DEFAULT 1,
  status SMALLINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL DEFAULT NULL,
  deleted_flag SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NULL DEFAULT NULL,
  create_user_name VARCHAR(100) NULL DEFAULT NULL,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (script_id),
  CONSTRAINT script_code_unique UNIQUE (script_code, deleted_flag)
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow脚本定义表';
COMMENT ON COLUMN t_liteflow_script.script_code IS '脚本编码(对应nodeId)';
COMMENT ON COLUMN t_liteflow_script.script_type IS '脚本类型:qlexpress/groovy/javascript';
COMMENT ON COLUMN t_liteflow_script.script_data IS '脚本内容';

CREATE INDEX idx_script_code ON t_liteflow_script(script_code) WHERE deleted_flag = 0;
CREATE INDEX idx_script_type ON t_liteflow_script(script_type);
```

**Example Data**:
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, version, status) VALUES
('验证订单', 'validateOrder', 'qlexpress', 'if(order.amount <= 0) { throw new Exception("Invalid amount"); } return true;', 1, 1),
('检查库存', 'checkInventory', 'qlexpress', 'inventory = inventoryService.getStock(order.productId); if(inventory < order.quantity) { throw new Exception("Insufficient stock"); } return true;', 1, 1),
('创建订单', 'createOrder', 'qlexpress', 'orderId = orderService.create(order); return orderId;', 1, 1);
```

---

### 3. t_liteflow_execution_log

Stores detailed execution logs for debugging and audit.

**Columns**:

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| log_id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| chain_code | VARCHAR(100) | NOT NULL | Executed chain code |
| chain_name | VARCHAR(255) | NOT NULL | Chain display name |
| request_id | VARCHAR(100) | NULL | Request trace ID |
| execution_status | SMALLINT | NOT NULL | Status: 0=失败;1=成功 |
| execution_time | INTEGER | NOT NULL | Execution duration (ms) |
| start_time | TIMESTAMP | NOT NULL | Execution start time |
| end_time | TIMESTAMP | NOT NULL | Execution end time |
| input_params | TEXT | NULL | Input parameters (JSON) |
| output_result | TEXT | NULL | Output result (JSON) |
| error_message | TEXT | NULL | Error message (if failed) |
| error_stack | TEXT | NULL | Exception stack trace |
| step_count | INTEGER | NOT NULL, DEFAULT 0 | Number of executed steps |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Log creation time |

**Indexes**:
```sql
CREATE INDEX idx_chain_code_log ON t_liteflow_execution_log(chain_code);
CREATE INDEX idx_execution_status ON t_liteflow_execution_log(execution_status);
CREATE INDEX idx_create_time ON t_liteflow_execution_log(create_time);
CREATE INDEX idx_request_id ON t_liteflow_execution_log(request_id);
```

**DDL**:
```sql
DROP TABLE IF EXISTS t_liteflow_execution_log;
CREATE TABLE t_liteflow_execution_log (
  log_id BIGSERIAL,
  chain_code VARCHAR(100) NOT NULL,
  chain_name VARCHAR(255) NOT NULL,
  request_id VARCHAR(100) NULL DEFAULT NULL,
  execution_status SMALLINT NOT NULL,
  execution_time INTEGER NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  input_params TEXT NULL DEFAULT NULL,
  output_result TEXT NULL DEFAULT NULL,
  error_message TEXT NULL DEFAULT NULL,
  error_stack TEXT NULL DEFAULT NULL,
  step_count INTEGER NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id)
);

COMMENT ON TABLE t_liteflow_execution_log IS 'LiteFlow执行日志表';
CREATE INDEX idx_chain_code_log ON t_liteflow_execution_log(chain_code);
CREATE INDEX idx_execution_status ON t_liteflow_execution_log(execution_status);
CREATE INDEX idx_create_time ON t_liteflow_execution_log(create_time);
CREATE INDEX idx_request_id ON t_liteflow_execution_log(request_id);
```

**Retention Policy**:
- Configurable via `smart.liteflow.log-retention-days` (default: 30 days)
- Scheduled cleanup job (to be implemented): Deletes logs older than retention period

---

### 4. t_liteflow_execution_metrics

Stores aggregated execution metrics for monitoring dashboards.

**Columns**:

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| metric_id | BIGSERIAL | PRIMARY KEY | Auto-incrementing ID |
| chain_code | VARCHAR(100) | NOT NULL | Chain code |
| metric_date | DATE | NOT NULL | Metric date (daily aggregation) |
| total_count | INTEGER | NOT NULL, DEFAULT 0 | Total executions |
| success_count | INTEGER | NOT NULL, DEFAULT 0 | Successful executions |
| failure_count | INTEGER | NOT NULL, DEFAULT 0 | Failed executions |
| avg_execution_time | INTEGER | NOT NULL, DEFAULT 0 | Average execution time (ms) |
| max_execution_time | INTEGER | NOT NULL, DEFAULT 0 | Maximum execution time (ms) |
| min_execution_time | INTEGER | NOT NULL, DEFAULT 0 | Minimum execution time (ms) |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update time |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time |

**Constraints**:
```sql
PRIMARY KEY (metric_id)
CONSTRAINT metric_unique UNIQUE (chain_code, metric_date)
```

**Indexes**:
```sql
CREATE INDEX idx_metric_date ON t_liteflow_execution_metrics(metric_date);
```

**DDL**:
```sql
DROP TABLE IF EXISTS t_liteflow_execution_metrics;
CREATE TABLE t_liteflow_execution_metrics (
  metric_id BIGSERIAL,
  chain_code VARCHAR(100) NOT NULL,
  metric_date DATE NOT NULL,
  total_count INTEGER NOT NULL DEFAULT 0,
  success_count INTEGER NOT NULL DEFAULT 0,
  failure_count INTEGER NOT NULL DEFAULT 0,
  avg_execution_time INTEGER NOT NULL DEFAULT 0,
  max_execution_time INTEGER NOT NULL DEFAULT 0,
  min_execution_time INTEGER NOT NULL DEFAULT 0,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (metric_id),
  CONSTRAINT metric_unique UNIQUE (chain_code, metric_date)
);

COMMENT ON TABLE t_liteflow_execution_metrics IS 'LiteFlow执行指标统计表';
CREATE INDEX idx_metric_date ON t_liteflow_execution_metrics(metric_date);
```

**Aggregation Strategy**:
- Updated on each execution (upsert operation)
- Calculate metrics incrementally:
  - `total_count += 1`
  - `success_count += (status == 1 ? 1 : 0)`
  - `failure_count += (status == 0 ? 1 : 0)`
  - `avg_execution_time = (avg * count + new_time) / (count + 1)`

---

## Complete Migration SQL

**File Location**: `sa-admin/src/main/resources/db/migration/V1.x__liteflow.sql`

```sql
-- ========================================
-- LiteFlow Module Database Migration
-- Version: 1.0.0
-- Date: 2026-01-23
-- ========================================

-- Table 1: Flow/Chain Definitions
DROP TABLE IF EXISTS t_liteflow_chain;
CREATE TABLE t_liteflow_chain (
  chain_id BIGSERIAL,
  chain_name VARCHAR(255) NOT NULL,
  chain_code VARCHAR(100) NOT NULL,
  chain_type SMALLINT NOT NULL DEFAULT 1,
  chain_data TEXT NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  status SMALLINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL DEFAULT NULL,
  deleted_flag SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NULL DEFAULT NULL,
  create_user_name VARCHAR(100) NULL DEFAULT NULL,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chain_id),
  CONSTRAINT chain_code_unique UNIQUE (chain_code, deleted_flag)
);

COMMENT ON TABLE t_liteflow_chain IS 'LiteFlow流程定义表';
COMMENT ON COLUMN t_liteflow_chain.chain_id IS '主键ID';
COMMENT ON COLUMN t_liteflow_chain.chain_name IS '流程名称';
COMMENT ON COLUMN t_liteflow_chain.chain_code IS '流程编码(唯一标识)';
COMMENT ON COLUMN t_liteflow_chain.chain_type IS '流程类型:1-普通;2-条件;3-循环';
COMMENT ON COLUMN t_liteflow_chain.chain_data IS 'EL表达式定义';
COMMENT ON COLUMN t_liteflow_chain.version IS '版本号';
COMMENT ON COLUMN t_liteflow_chain.status IS '状态:0-禁用;1-启用';
COMMENT ON COLUMN t_liteflow_chain.deleted_flag IS '删除标记:0-未删除;1-已删除';

CREATE INDEX idx_chain_code ON t_liteflow_chain(chain_code) WHERE deleted_flag = 0;
CREATE INDEX idx_status ON t_liteflow_chain(status) WHERE deleted_flag = 0;

-- Table 2: Script Node Definitions
DROP TABLE IF EXISTS t_liteflow_script;
CREATE TABLE t_liteflow_script (
  script_id BIGSERIAL,
  script_name VARCHAR(255) NOT NULL,
  script_code VARCHAR(100) NOT NULL,
  script_type VARCHAR(20) NOT NULL DEFAULT 'qlexpress',
  script_data TEXT NOT NULL,
  script_language VARCHAR(20) NOT NULL DEFAULT 'qlexpress',
  version INTEGER NOT NULL DEFAULT 1,
  status SMALLINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL DEFAULT NULL,
  deleted_flag SMALLINT NOT NULL DEFAULT 0,
  create_user_id BIGINT NULL DEFAULT NULL,
  create_user_name VARCHAR(100) NULL DEFAULT NULL,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (script_id),
  CONSTRAINT script_code_unique UNIQUE (script_code, deleted_flag)
);

COMMENT ON TABLE t_liteflow_script IS 'LiteFlow脚本定义表';
COMMENT ON COLUMN t_liteflow_script.script_code IS '脚本编码(对应nodeId)';
COMMENT ON COLUMN t_liteflow_script.script_type IS '脚本类型:qlexpress/groovy/javascript';
COMMENT ON COLUMN t_liteflow_script.script_data IS '脚本内容';

CREATE INDEX idx_script_code ON t_liteflow_script(script_code) WHERE deleted_flag = 0;
CREATE INDEX idx_script_type ON t_liteflow_script(script_type);

-- Table 3: Execution Logs
DROP TABLE IF EXISTS t_liteflow_execution_log;
CREATE TABLE t_liteflow_execution_log (
  log_id BIGSERIAL,
  chain_code VARCHAR(100) NOT NULL,
  chain_name VARCHAR(255) NOT NULL,
  request_id VARCHAR(100) NULL DEFAULT NULL,
  execution_status SMALLINT NOT NULL,
  execution_time INTEGER NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  input_params TEXT NULL DEFAULT NULL,
  output_result TEXT NULL DEFAULT NULL,
  error_message TEXT NULL DEFAULT NULL,
  error_stack TEXT NULL DEFAULT NULL,
  step_count INTEGER NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id)
);

COMMENT ON TABLE t_liteflow_execution_log IS 'LiteFlow执行日志表';
CREATE INDEX idx_chain_code_log ON t_liteflow_execution_log(chain_code);
CREATE INDEX idx_execution_status ON t_liteflow_execution_log(execution_status);
CREATE INDEX idx_create_time ON t_liteflow_execution_log(create_time);
CREATE INDEX idx_request_id ON t_liteflow_execution_log(request_id);

-- Table 4: Aggregated Metrics
DROP TABLE IF EXISTS t_liteflow_execution_metrics;
CREATE TABLE t_liteflow_execution_metrics (
  metric_id BIGSERIAL,
  chain_code VARCHAR(100) NOT NULL,
  metric_date DATE NOT NULL,
  total_count INTEGER NOT NULL DEFAULT 0,
  success_count INTEGER NOT NULL DEFAULT 0,
  failure_count INTEGER NOT NULL DEFAULT 0,
  avg_execution_time INTEGER NOT NULL DEFAULT 0,
  max_execution_time INTEGER NOT NULL DEFAULT 0,
  min_execution_time INTEGER NOT NULL DEFAULT 0,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (metric_id),
  CONSTRAINT metric_unique UNIQUE (chain_code, metric_date)
);

COMMENT ON TABLE t_liteflow_execution_metrics IS 'LiteFlow执行指标统计表';
CREATE INDEX idx_metric_date ON t_liteflow_execution_metrics(metric_date);
```

---

## Performance Considerations

### 1. Indexes
- **chain_code, script_code**: Frequent lookups by code (partial index with `deleted_flag = 0`)
- **execution_status**: Filter by success/failure
- **create_time**: Time-range queries for logs
- **request_id**: Distributed tracing

### 2. Soft Delete
- Unique constraints include `deleted_flag` to allow same code after deletion
- Partial indexes (`WHERE deleted_flag = 0`) reduce index size

### 3. JSON Storage
- `input_params`, `output_result` stored as TEXT (JSON)
- Consider PostgreSQL JSONB for advanced querying (future enhancement)

### 4. Partitioning (Future)
- **t_liteflow_execution_log**: Partition by `create_time` (monthly/yearly)
- **t_liteflow_execution_metrics**: Partition by `metric_date`

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Maintainer**: 1024创新实验室
