# LiteFlow API Specification

## Overview

This document provides complete API specifications for the LiteFlow module REST endpoints. All APIs follow SmartAdmin conventions with ResponseDTO pattern, permission control, and standardized error handling.

## Base Information

- **Base URL**: `/liteflow`
- **Authentication**: Sa-Token (session-based)
- **Authorization**: Permission-based (@SaCheckPermission)
- **Response Format**: JSON (ResponseDTO wrapper)
- **API Documentation**: Knife4j/Swagger UI

## Response Format

All APIs return responses wrapped in `ResponseDTO`:

**Success Response**:
```json
{
  "ok": true,
  "code": 1,
  "msg": "success",
  "data": { /* response data */ }
}
```

**Error Response**:
```json
{
  "ok": false,
  "code": 10001,
  "msg": "Parameter validation failed",
  "data": null
}
```

---

## Chain Management APIs

### 1. Query Chains (Paginated)

**Endpoint**: `POST /liteflow/chain/queryPage`

**Permission**: `liteflow:chain:query`

**Request Body**:
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "chainCode": "order",         // Optional: filter by chain code
  "chainName": "订单",          // Optional: filter by chain name
  "chainType": 1,               // Optional: filter by type
  "status": 1                   // Optional: filter by status
}
```

**Response**:
```json
{
  "ok": true,
  "data": {
    "total": 25,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "chainId": 1,
        "chainName": "订单处理流程",
        "chainCode": "order-process-chain",
        "chainType": 1,
        "chainData": "THEN(validateOrder, checkInventory, createOrder)",
        "version": 3,
        "status": 1,
        "remark": "订单业务流程",
        "createUserName": "admin",
        "createTime": "2026-01-20 10:30:00",
        "updateTime": "2026-01-22 14:20:00"
      }
    ]
  }
}
```

---

### 2. Create Chain

**Endpoint**: `POST /liteflow/chain/add`

**Permission**: `liteflow:chain:add`

**Request Body**:
```json
{
  "chainName": "订单处理流程",
  "chainCode": "order-process-chain",
  "chainType": 1,
  "chainData": "THEN(validateOrder, checkInventory, createOrder, sendNotification)",
  "remark": "订单创建到通知的完整流程"
}
```

**Validation Rules**:
- `chainName`: Required, max 255 characters
- `chainCode`: Required, max 100 characters, unique, alphanumeric + dash/underscore
- `chainType`: Required, 1=普通, 2=条件, 3=循环
- `chainData`: Required, valid EL expression
- `remark`: Optional, max 500 characters

**Response**:
```json
{
  "ok": true,
  "msg": "创建成功",
  "data": null
}
```

**Error Cases**:
- `chain_code already exists`: Code 10001, "流程编码已存在"
- `Invalid EL syntax`: Code 10002, "EL表达式语法错误"

---

### 3. Update Chain

**Endpoint**: `POST /liteflow/chain/update`

**Permission**: `liteflow:chain:update`

**Request Body**:
```json
{
  "chainId": 1,
  "chainName": "订单处理流程-V2",
  "chainCode": "order-process-chain",
  "chainType": 1,
  "chainData": "THEN(validateOrder, checkInventory, IF(isVip, applyDiscount, processPayment), createOrder)",
  "remark": "增加VIP折扣逻辑"
}
```

**Behavior**:
- Version auto-increments: `version = version + 1`
- Cache invalidated for updated chain
- Flow engine reloaded automatically

**Response**:
```json
{
  "ok": true,
  "msg": "更新成功",
  "data": null
}
```

---

### 4. Delete Chain (Soft Delete)

**Endpoint**: `GET /liteflow/chain/delete/{chainId}`

**Permission**: `liteflow:chain:delete`

**Path Parameters**:
- `chainId` (Long): Chain ID to delete

**Response**:
```json
{
  "ok": true,
  "msg": "删除成功",
  "data": null
}
```

**Behavior**:
- Sets `deleted_flag = 1`
- Cache invalidated
- Flow engine reloaded

---

### 5. Get Chain Detail

**Endpoint**: `GET /liteflow/chain/detail/{chainId}`

**Permission**: `liteflow:chain:query`

**Path Parameters**:
- `chainId` (Long): Chain ID

**Response**:
```json
{
  "ok": true,
  "data": {
    "chainId": 1,
    "chainName": "订单处理流程",
    "chainCode": "order-process-chain",
    "chainType": 1,
    "chainData": "THEN(validateOrder, checkInventory, createOrder)",
    "version": 5,
    "status": 1,
    "remark": "订单业务流程",
    "createUserId": 100,
    "createUserName": "admin",
    "createTime": "2026-01-20 10:30:00",
    "updateTime": "2026-01-23 09:15:00"
  }
}
```

---

### 6. Reload Chain

**Endpoint**: `POST /liteflow/chain/reload`

**Permission**: `liteflow:chain:reload`

**Query Parameters**:
- `chainCode` (String): Chain code to reload

**Response**:
```json
{
  "ok": true,
  "msg": "重新加载成功",
  "data": null
}
```

**Behavior**:
- Evicts cache for specified chain
- Calls `flowExecutor.reloadRule()`

---

### 7. Reload All Chains

**Endpoint**: `POST /liteflow/chain/reloadAll`

**Permission**: `liteflow:chain:reload`

**Response**:
```json
{
  "ok": true,
  "msg": "重新加载所有流程成功",
  "data": null
}
```

**Behavior**:
- Evicts all chain/script caches
- Reloads entire flow engine

---

## Script Management APIs

### 8. Query Scripts (Paginated)

**Endpoint**: `POST /liteflow/script/queryPage`

**Permission**: `liteflow:script:query`

**Request Body**:
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "scriptCode": "validate",     // Optional: filter by code
  "scriptName": "验证",         // Optional: filter by name
  "scriptType": "qlexpress",    // Optional: filter by type
  "status": 1                   // Optional: filter by status
}
```

**Response**:
```json
{
  "ok": true,
  "data": {
    "total": 15,
    "list": [
      {
        "scriptId": 1,
        "scriptName": "验证订单",
        "scriptCode": "validateOrder",
        "scriptType": "qlexpress",
        "scriptData": "if(order.amount <= 0) { throw new Exception('Invalid amount'); } return true;",
        "version": 2,
        "status": 1,
        "createTime": "2026-01-20 11:00:00"
      }
    ]
  }
}
```

---

### 9. Create Script

**Endpoint**: `POST /liteflow/script/add`

**Permission**: `liteflow:script:add`

**Request Body**:
```json
{
  "scriptName": "验证订单",
  "scriptCode": "validateOrder",
  "scriptType": "qlexpress",
  "scriptData": "if (order == null || order.amount <= 0) {\n  throw new Exception('Invalid order');\n}\nreturn true;",
  "remark": "订单基本验证逻辑"
}
```

**Validation Rules**:
- `scriptName`: Required, max 255 characters
- `scriptCode`: Required, max 100 characters, unique, alphanumeric
- `scriptType`: Required, enum: qlexpress|groovy|javascript
- `scriptData`: Required, valid script syntax

**Response**:
```json
{
  "ok": true,
  "msg": "创建成功",
  "data": null
}
```

---

### 10. Update Script

**Endpoint**: `POST /liteflow/script/update`

**Permission**: `liteflow:script:update`

**Request Body**:
```json
{
  "scriptId": 1,
  "scriptName": "验证订单-增强版",
  "scriptCode": "validateOrder",
  "scriptType": "qlexpress",
  "scriptData": "// Enhanced validation\nif (order == null) return false;\nif (order.amount <= 0 || order.amount > 10000) return false;\nif (order.productId == null) return false;\nreturn true;",
  "remark": "增加金额上限和产品ID验证"
}
```

**Behavior**:
- Version increments: `version = version + 1`
- Cache invalidated
- Flow engine reloaded

---

### 11. Delete Script

**Endpoint**: `GET /liteflow/script/delete/{scriptId}`

**Permission**: `liteflow:script:delete`

**Path Parameters**:
- `scriptId` (Long): Script ID

**Response**:
```json
{
  "ok": true,
  "msg": "删除成功",
  "data": null
}
```

---

### 12. Test Script

**Endpoint**: `POST /liteflow/script/test`

**Permission**: `liteflow:script:test`

**Request Body**:
```json
{
  "scriptType": "qlexpress",
  "scriptData": "a = 10; b = 20; return a + b;",
  "testParams": {
    "a": 5,
    "b": 15
  }
}
```

**Response**:
```json
{
  "ok": true,
  "data": {
    "success": true,
    "result": "20",
    "executionTime": 5,
    "errorMessage": null
  }
}
```

---

## Execution APIs

### 13. Execute Flow

**Endpoint**: `POST /liteflow/execution/execute`

**Permission**: `liteflow:execution:execute`

**Request Body**:
```json
{
  "chainCode": "order-process-chain",
  "inputParams": {
    "orderId": 12345,
    "userId": 678,
    "amount": 99.99,
    "productId": "PROD-001"
  },
  "async": false,               // Optional: async execution (default: false)
  "requestId": "REQ-20260123-001" // Optional: custom request ID for tracing
}
```

**Response (Sync)**:
```json
{
  "ok": true,
  "data": {
    "success": true,
    "chainCode": "order-process-chain",
    "requestId": "REQ-20260123-001",
    "executionTime": 125,
    "outputResult": {
      "orderId": 12345,
      "status": "created",
      "confirmationCode": "ORD-20260123-12345"
    },
    "message": "Order processed successfully"
  }
}
```

**Response (Async)**:
```json
{
  "ok": true,
  "data": {
    "async": true,
    "requestId": "REQ-20260123-001",
    "message": "Flow execution started in background"
  }
}
```

**Error Response**:
```json
{
  "ok": false,
  "code": 20001,
  "msg": "Flow execution failed: Insufficient stock",
  "data": {
    "chainCode": "order-process-chain",
    "requestId": "REQ-20260123-001",
    "errorMessage": "Insufficient stock for product PROD-001",
    "executionTime": 87
  }
}
```

---

### 14. Query Execution Logs

**Endpoint**: `POST /liteflow/execution/queryLog`

**Permission**: `liteflow:execution:query`

**Request Body**:
```json
{
  "pageNum": 1,
  "pageSize": 20,
  "chainCode": "order-process-chain",  // Optional
  "executionStatus": 1,                // Optional: 0=失败, 1=成功
  "requestId": "REQ-20260123",         // Optional: partial match
  "startTime": "2026-01-23 00:00:00",  // Optional
  "endTime": "2026-01-23 23:59:59"     // Optional
}
```

**Response**:
```json
{
  "ok": true,
  "data": {
    "total": 150,
    "list": [
      {
        "logId": 1001,
        "chainCode": "order-process-chain",
        "chainName": "订单处理流程",
        "requestId": "REQ-20260123-001",
        "executionStatus": 1,
        "executionTime": 125,
        "startTime": "2026-01-23 10:30:00",
        "endTime": "2026-01-23 10:30:00.125",
        "stepCount": 4,
        "createTime": "2026-01-23 10:30:00"
      }
    ]
  }
}
```

---

### 15. Get Execution Log Detail

**Endpoint**: `GET /liteflow/execution/logDetail/{logId}`

**Permission**: `liteflow:execution:query`

**Path Parameters**:
- `logId` (Long): Execution log ID

**Response**:
```json
{
  "ok": true,
  "data": {
    "logId": 1001,
    "chainCode": "order-process-chain",
    "chainName": "订单处理流程",
    "requestId": "REQ-20260123-001",
    "executionStatus": 1,
    "executionTime": 125,
    "startTime": "2026-01-23 10:30:00.000",
    "endTime": "2026-01-23 10:30:00.125",
    "inputParams": "{\"orderId\":12345,\"userId\":678,\"amount\":99.99}",
    "outputResult": "{\"orderId\":12345,\"status\":\"created\"}",
    "errorMessage": null,
    "errorStack": null,
    "stepCount": 4,
    "createTime": "2026-01-23 10:30:00"
  }
}
```

---

## Monitoring APIs

### 16. Query Metrics

**Endpoint**: `POST /liteflow/monitor/metrics`

**Permission**: `liteflow:monitor:query`

**Request Body**:
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "chainCode": "order",              // Optional: filter by chain code
  "startDate": "2026-01-01",         // Optional
  "endDate": "2026-01-23"            // Optional
}
```

**Response**:
```json
{
  "ok": true,
  "data": {
    "total": 23,
    "list": [
      {
        "chainCode": "order-process-chain",
        "metricDate": "2026-01-23",
        "totalCount": 1250,
        "successCount": 1200,
        "failureCount": 50,
        "avgExecutionTime": 105,
        "maxExecutionTime": 850,
        "minExecutionTime": 45,
        "successRate": 96.0
      }
    ]
  }
}
```

---

### 17. Get Execution Trend

**Endpoint**: `GET /liteflow/monitor/trend/{chainCode}`

**Permission**: `liteflow:monitor:query`

**Path Parameters**:
- `chainCode` (String): Chain code

**Query Parameters**:
- `days` (Integer): Number of days (default: 7, max: 90)

**Response**:
```json
{
  "ok": true,
  "data": {
    "chainCode": "order-process-chain",
    "chainName": "订单处理流程",
    "days": 7,
    "trend": [
      {
        "date": "2026-01-17",
        "totalCount": 980,
        "successCount": 950,
        "failureCount": 30,
        "successRate": 96.9,
        "avgExecutionTime": 98
      },
      {
        "date": "2026-01-18",
        "totalCount": 1100,
        "successCount": 1070,
        "failureCount": 30,
        "successRate": 97.3,
        "avgExecutionTime": 102
      }
      // ... 5 more days
    ]
  }
}
```

---

### 18. Get Monitoring Overview

**Endpoint**: `GET /liteflow/monitor/overview`

**Permission**: `liteflow:monitor:query`

**Response**:
```json
{
  "ok": true,
  "data": {
    "totalChains": 25,
    "activeChains": 20,
    "todayExecutions": 5680,
    "todaySuccesses": 5520,
    "todayFailures": 160,
    "todaySuccessRate": 97.2,
    "avgExecutionTime": 110,
    "topSlowChains": [
      {
        "chainCode": "complex-approval-chain",
        "chainName": "复杂审批流程",
        "avgExecutionTime": 850
      }
    ],
    "topFrequentChains": [
      {
        "chainCode": "order-process-chain",
        "chainName": "订单处理流程",
        "todayCount": 1250
      }
    ],
    "recentErrors": [
      {
        "chainCode": "payment-chain",
        "errorMessage": "Payment gateway timeout",
        "count": 15,
        "lastOccurrence": "2026-01-23 14:20:00"
      }
    ]
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1 | success | Success |
| 10001 | 参数验证失败 | Parameter validation failed |
| 10002 | EL表达式语法错误 | Invalid EL expression syntax |
| 10003 | 流程编码已存在 | Chain code already exists |
| 10004 | 脚本编码已存在 | Script code already exists |
| 10005 | 流程不存在 | Chain not found |
| 10006 | 脚本不存在 | Script not found |
| 20001 | 流程执行失败 | Flow execution failed |
| 20002 | 执行超时 | Execution timeout |
| 20003 | 脚本执行错误 | Script execution error |
| 30001 | 权限不足 | Insufficient permission |
| 30002 | 未登录 | Not authenticated |

---

## Testing with Swagger/Knife4j

Access API documentation at:
```
http://localhost:1024/doc.html
```

Navigate to: **SmartAdmin Base Support - LiteFlow**

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Maintainer**: 1024创新实验室
