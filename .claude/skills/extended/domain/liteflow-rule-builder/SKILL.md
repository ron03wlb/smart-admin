---
name: liteflow-rule-builder
description: [P1 - Extended] Generate LiteFlow rule DSL (EL expressions and QLExpress scripts) from natural language descriptions for complex business workflows. Use when implementing business rule orchestration, approval workflows, validation chains, or migrating from Evrete rules. Triggers when (1) User requests "create LiteFlow chain/rule", (2) User describes a multi-step business workflow, (3) User mentions flow orchestration, conditional logic, or parallel execution, (4) User wants to implement approval/validation flows, (5) Migrating from Evrete to LiteFlow.
---

# LiteFlow Rule Builder

Generate complete LiteFlow flow orchestration rules from natural language descriptions - automatically creates EL expression chains, QLExpress scripts, database configurations, and integration code following SmartAdmin patterns.

## Quick Start

**Most common usage:**
```
User: "Create an order validation workflow that checks inventory, applies VIP discounts, and sends notifications in parallel"
```

You will:
1. Analyze workflow requirements (sequential steps, conditions, parallel operations)
2. Generate LiteFlow chain EL expression (THEN, IF, WHEN, SWITCH)
3. Generate QLExpress script nodes for business logic
4. Generate database insert statements (t_liteflow_chain, t_liteflow_script)
5. Generate Service layer integration code
6. Provide execution examples and test scenarios

## Why This Skill Exists

**Problem:** Complex business workflows are hard to implement and maintain
- Sequential steps mixed with conditional branching
- Parallel operations (send email + SMS + update cache)
- Dynamic rule changes requiring code redeployment
- Difficult to visualize and debug flow execution

**Solution:**
LiteFlow provides declarative flow orchestration with:
- ✅ EL expressions for clear workflow structure
- ✅ Database-backed storage with hot-reload
- ✅ QLExpress scripts for dynamic business logic
- ✅ Execution logging and monitoring
- ✅ Visual flow representation

## Core LiteFlow Patterns

### Pattern 1: Sequential Execution (THEN)

**Use Case:** Execute nodes in strict order (A → B → C)

**EL Expression:**
```javascript
THEN(nodeA, nodeB, nodeC)
```

**Example: Order Processing**
```javascript
THEN(
  validateOrder,
  checkInventory,
  calculatePrice,
  createOrder,
  sendNotification
)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('订单处理流程', 'order-process-chain', 1,
 'THEN(validateOrder, checkInventory, calculatePrice, createOrder, sendNotification)');
```

**QLExpress Script Example (validateOrder):**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('验证订单', 'validateOrder', 'qlexpress',
'// Get order from context
order = context.getData("order");

// Validation rules
if (order == null) {
    throw new Exception("Order cannot be null");
}
if (order.amount == null || order.amount <= 0) {
    throw new Exception("Invalid order amount: " + order.amount);
}
if (order.productId == null) {
    throw new Exception("Product ID is required");
}

// Log success
log.info("Order validated: orderId=" + order.orderId);
return true;');
```

**When to Use:**
- Multi-step processes requiring strict order
- Dependent operations (B needs result from A)
- Audit trails with sequential logging

**SmartAdmin Integration:**
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;

    public ResponseDTO<OrderVO> processOrder(OrderAddForm form) {
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-process-chain");
        executionForm.setInputParams(Map.of(
            "order", SmartBeanUtil.copy(form, Order.class)
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (!result.getOk()) {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }

        Order order = (Order) result.getData().getOutputResult().get("order");
        return ResponseDTO.ok(SmartBeanUtil.copy(order, OrderVO.class));
    }
}
```

---

### Pattern 2: Parallel Execution (WHEN)

**Use Case:** Execute nodes concurrently (all must complete)

**EL Expression:**
```javascript
WHEN(nodeA, nodeB, nodeC)
```

**Example: Multi-Channel Notification**
```javascript
THEN(
  processOrder,
  WHEN(sendEmail, sendSMS, sendPushNotification, updateCache)
)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('通知发送流程', 'notification-chain', 1,
 'WHEN(sendEmail, sendSMS, sendPushNotification)');
```

**QLExpress Scripts:**
```sql
-- Send Email Script
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('发送邮件', 'sendEmail', 'qlexpress',
'user = context.getData("user");
emailService = context.getBean("emailService");
emailService.send(user.email, "Order Confirmed", "Your order has been confirmed");
log.info("Email sent to: " + user.email);
return true;');

-- Send SMS Script
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('发送短信', 'sendSMS', 'qlexpress',
'user = context.getData("user");
smsService = context.getBean("smsService");
smsService.send(user.phone, "Order confirmed. Track at: example.com/orders");
log.info("SMS sent to: " + user.phone);
return true;');
```

**When to Use:**
- Independent operations that can run concurrently
- Performance optimization (parallel API calls)
- Broadcasting to multiple channels

**Performance Note:**
- All WHEN nodes execute in parallel threads
- Total execution time = max(nodeA, nodeB, nodeC) instead of sum

---

### Pattern 3: Conditional Logic (IF)

**Use Case:** Branch execution based on condition

**EL Expression:**
```javascript
IF(conditionNode, thenNode, elseNode)
```

**Example: VIP Discount Logic**
```javascript
THEN(
  validateOrder,
  checkInventory,
  IF(
    isVipMember,
    THEN(calculateVipDiscount, applyDiscount),
    calculateRegularPrice
  ),
  createOrder
)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('VIP订单处理', 'vip-order-chain', 2,
 'THEN(validateOrder, checkInventory, IF(isVipMember, THEN(calculateVipDiscount, applyDiscount), calculateRegularPrice), createOrder)');
```

**Condition Script (isVipMember):**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('检查VIP会员', 'isVipMember', 'qlexpress',
'user = context.getData("user");
memberService = context.getBean("memberService");
vipLevel = memberService.getVipLevel(user.userId);

// Return boolean for IF condition
if (vipLevel != null && vipLevel >= 1) {
    context.setData("vipLevel", vipLevel);
    return true;  // Execute THEN branch
} else {
    return false; // Execute ELSE branch
}');
```

**When to Use:**
- Business rule branching (different paths for different user types)
- Conditional validations
- Feature flags (A/B testing flows)

**Nested IF Example:**
```javascript
IF(
  isHighValue,
  IF(requiresApproval, manualReview, autoApprove),
  fastTrack
)
```

---

### Pattern 4: Switch Routing (SWITCH)

**Use Case:** Route to different nodes based on value

**EL Expression:**
```javascript
SWITCH(routerNode).to(caseA, caseB, caseC)
```

**Example: Multi-Level Approval**
```javascript
THEN(
  submitRequest,
  SWITCH(getApprovalLevel).to(
    level1Approval,
    level2Approval,
    level3Approval,
    ceoApproval
  )
)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('审批流程', 'approval-chain', 2,
 'THEN(submitRequest, SWITCH(getApprovalLevel).to(level1Approval, level2Approval, level3Approval, ceoApproval))');
```

**Router Script (getApprovalLevel):**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('获取审批级别', 'getApprovalLevel', 'qlexpress',
'request = context.getData("request");
amount = request.amount;

// Return node ID to execute (index-based: 0=level1, 1=level2, etc.)
if (amount < 10000) {
    return "level1Approval";  // Auto approve
} else if (amount < 100000) {
    return "level2Approval";  // Manager approval
} else if (amount < 1000000) {
    return "level3Approval";  // Director approval
} else {
    return "ceoApproval";     // CEO approval
}');
```

**When to Use:**
- Multi-tier workflows (approval levels, escalation)
- Dynamic routing based on runtime values
- Strategy pattern implementation

**SWITCH vs Multiple IF:**
- SWITCH: Cleaner for 3+ branches, value-based routing
- IF: Better for boolean conditions, binary decisions

---

### Pattern 5: Loop Iteration (FOR)

**Use Case:** Execute node for each item in collection

**EL Expression:**
```javascript
FOR(iteratorNode).DO(processingNode).BREAK(breakConditionNode)
```

**Example: Batch Order Processing**
```javascript
FOR(getNextOrder).DO(
  THEN(validateOrder, processPayment, shipOrder)
).BREAK(noMoreOrders)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('批量订单处理', 'batch-order-chain', 3,
 'FOR(getNextOrder).DO(THEN(validateOrder, processPayment, shipOrder)).BREAK(noMoreOrders)');
```

**Iterator Script (getNextOrder):**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('获取下一个订单', 'getNextOrder', 'qlexpress',
'orderQueue = context.getData("orderQueue");

if (orderQueue == null || orderQueue.isEmpty()) {
    return null;  // Trigger BREAK condition
}

// Get next order
nextOrder = orderQueue.poll();
context.setData("currentOrder", nextOrder);
return nextOrder;');
```

**Break Condition Script:**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('检查队列是否为空', 'noMoreOrders', 'qlexpress',
'orderQueue = context.getData("orderQueue");
return orderQueue == null || orderQueue.isEmpty();');
```

**When to Use:**
- Batch processing (multiple records)
- Iterative workflows (retry logic, polling)
- Dynamic loop count (process until condition met)

**Performance Warning:**
- Large loops (1000+ items) should use async execution
- Consider database pagination for very large datasets

---

## Advanced Patterns

### Pattern 6: Nested Chains (Sub-flows)

**Use Case:** Reusable sub-workflows

**Main Chain:**
```javascript
THEN(
  validateInput,
  processData,
  THEN(auditLog, cleanup)  // Reusable sub-chain
)
```

**Database Insert:**
```sql
-- Main chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('主流程', 'main-chain', 1,
 'THEN(validateInput, processData, auditCleanupChain)');

-- Sub-chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('审计清理子流程', 'auditCleanupChain', 1,
 'THEN(auditLog, cleanup)');
```

---

### Pattern 7: Exception Handling (CATCH)

**Use Case:** Handle errors gracefully

**EL Expression:**
```javascript
THEN(
  riskCheck,
  processPayment
).CATCH(handlePaymentError)
```

**Error Handler Script:**
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('支付错误处理', 'handlePaymentError', 'qlexpress',
'exception = context.getData("exception");
log.error("Payment failed: " + exception.getMessage());

// Rollback logic
order = context.getData("order");
orderService = context.getBean("orderService");
orderService.cancelOrder(order.orderId);

// Notify user
notificationService = context.getBean("notificationService");
notificationService.sendPaymentFailedEmail(order.userId);

return false;  // Mark chain as failed
');
```

---

## SmartAdmin Integration Patterns

### Database Storage Pattern

**Step 1: Insert Chain Definition**
```sql
INSERT INTO t_liteflow_chain (
  chain_name,
  chain_code,
  chain_type,
  chain_data,
  status,
  create_user_id,
  create_user_name
) VALUES (
  '员工审批流程',           -- Display name
  'employee-approval-chain', -- Unique code
  2,                         -- Type: 1=普通, 2=条件, 3=循环
  'THEN(validateEmployee, SWITCH(getApprovalLevel).to(hrApproval, managerApproval, ceoApproval))',
  1,                         -- Status: 1=启用, 0=禁用
  1,                         -- Creator user ID
  'admin'                    -- Creator username
);
```

**Step 2: Insert Script Nodes**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status
) VALUES (
  '验证员工信息',
  'validateEmployee',
  'qlexpress',
  'employee = context.getData("employee");
   if (employee == null || employee.name == null) {
       throw new Exception("Invalid employee data");
   }
   return true;',
  1
);
```

**Step 3: Reload Flow Engine**
```bash
POST /liteflow/chain/reloadAll
```

---

### Service Layer Integration

**Standard Pattern:**
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final EmployeeDao employeeDao;

    public ResponseDTO<EmployeeVO> approveEmployee(Long employeeId) {
        // Load employee data
        EmployeeEntity employee = employeeDao.selectById(employeeId);
        if (employee == null) {
            return ResponseDTO.userErrorParam("Employee not found");
        }

        // Execute LiteFlow chain
        LiteFlowExecutionForm form = new LiteFlowExecutionForm();
        form.setChainCode("employee-approval-chain");
        form.setInputParams(Map.of(
            "employee", employee,
            "approver", RequestContext.getRequestUser()
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(form);

        if (!result.getOk()) {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }

        // Extract result
        Map<String, Object> output = result.getData().getOutputResult();
        String approvalStatus = (String) output.get("approvalStatus");

        // Update database
        employee.setApprovalStatus(approvalStatus);
        employeeDao.updateById(employee);

        return ResponseDTO.ok(SmartBeanUtil.copy(employee, EmployeeVO.class));
    }
}
```

---

## QLExpress Script Best Practices

### Accessing Context Data

```javascript
// Get data from context
order = context.getData("order");
user = context.getData("user");

// Set data to context (pass to next node)
context.setData("calculatedPrice", finalPrice);
context.setData("discountApplied", true);
```

### Calling Spring Beans

```javascript
// Get bean from Spring context
orderService = context.getBean("orderService");
emailService = context.getBean("emailService");

// Call bean methods
result = orderService.createOrder(order);
emailService.send(user.email, "Order Confirmed", emailBody);
```

### Logging

```javascript
// Use LiteFlow's logger
log.info("Processing order: " + order.orderId);
log.warn("Low inventory: " + inventory.stock);
log.error("Payment failed: " + exception.getMessage());
```

### Exception Handling

```javascript
// Throw exception to fail chain
if (order.amount <= 0) {
    throw new Exception("Invalid order amount: " + order.amount);
}

// Return boolean for IF conditions
if (user.vipLevel >= 3) {
    return true;   // THEN branch
} else {
    return false;  // ELSE branch
}
```

### Variable Types

```javascript
// Supported types
stringVar = "hello";
intVar = 123;
longVar = 123L;
doubleVar = 123.45;
boolVar = true;
listVar = new ArrayList();
mapVar = new HashMap();

// Type casting
amount = (BigDecimal) context.getData("amount");
userId = (Long) context.getData("userId");
```

---

## Common Mistakes and Fixes

### ❌ Mistake 1: Forgetting to Return Value

**Wrong:**
```javascript
// Condition script without return
user = context.getData("user");
user.vipLevel >= 3;  // No return statement
```

**Fixed:**
```javascript
user = context.getData("user");
return user.vipLevel >= 3;  // Return boolean for IF
```

---

### ❌ Mistake 2: Not Setting Output Data

**Wrong:**
```javascript
// Calculate but don't save to context
finalPrice = order.price * 0.8;
// Next node can't access finalPrice
```

**Fixed:**
```javascript
finalPrice = order.price * 0.8;
context.setData("finalPrice", finalPrice);  // Pass to next node
```

---

### ❌ Mistake 3: Using WHEN for Dependent Operations

**Wrong:**
```javascript
// checkInventory needs result from validateOrder
WHEN(validateOrder, checkInventory)  // Both run in parallel!
```

**Fixed:**
```javascript
THEN(validateOrder, checkInventory)  // Sequential execution
```

---

### ❌ Mistake 4: Missing Exception Handling

**Wrong:**
```javascript
// No null check
user = context.getData("user");
email = user.email;  // NullPointerException if user is null
```

**Fixed:**
```javascript
user = context.getData("user");
if (user == null) {
    throw new Exception("User not found in context");
}
email = user.email;
```

---

### ❌ Mistake 5: Incorrect SWITCH Return Type

**Wrong:**
```javascript
// SWITCH expects node ID (String), not index (int)
return 0;  // Will cause routing error
```

**Fixed:**
```javascript
return "level1Approval";  // Return node ID as String
```

---

## Rationalization Table

| Pattern | Use Case | Complexity | Time Estimate |
|---------|----------|------------|---------------|
| **THEN** | Sequential workflow | Low | 5-10 min |
| **WHEN** | Parallel operations | Medium | 10-15 min |
| **IF** | Conditional branching | Medium | 10-15 min |
| **SWITCH** | Multi-tier routing | High | 15-20 min |
| **FOR** | Batch processing | High | 20-30 min |
| **Nested Chains** | Reusable sub-flows | Medium | 15-20 min |
| **Exception Handling** | Error recovery | Medium | 10-15 min |

**Total Time for Complex Workflow:** 30-60 minutes (including testing)

---

## Migration from Evrete

### Conversion Table

| Evrete Pattern | LiteFlow Equivalent | Notes |
|----------------|---------------------|-------|
| `@Rule(salience=100)` | `THEN(nodeA, nodeB)` | Order by THEN sequence |
| `@Where("$order.amount > 0")` | QLExpress script with `if` | Move condition to script |
| Parallel rule execution | `WHEN(nodeA, nodeB)` | Explicit parallel declaration |
| Rule chaining (fact insertion) | `THEN` with context.setData | Pass data via context |
| Rete algorithm conflict resolution | Manual IF/SWITCH logic | No automatic conflict resolution |

**Migration Steps:**
1. Identify rule dependencies (which rules depend on others)
2. Map to LiteFlow patterns (THEN for sequential, WHEN for parallel)
3. Convert @Where conditions to QLExpress scripts
4. Test functional equivalence (same input → same output)

See [docs/plans/liteflow/migration-guide.md](../../../../docs/plans/liteflow/migration-guide.md) for complete migration guide.

---

## CSO Keywords (Discoverability)

**Trigger phrases:**
- "create flow", "create chain", "LiteFlow rule"
- "business workflow", "approval flow", "validation chain"
- "sequential execution", "parallel processing", "conditional logic"
- "orchestration", "routing", "branching"
- "migrate from Evrete", "rule engine"

**Context signals:**
- User describes multi-step process
- Mentions "if-then-else" or "parallel" operations
- Talks about approval levels or routing
- Wants database-backed rules with hot-reload

---

## Testing Workflow

### RED Phase: Baseline Test

**Scenario:** Employee approval workflow
- Validate employee data
- Route to approval tier based on salary
- Send notification on approval

**Expected LiteFlow Output:**

**Chain:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('员工审批流程', 'employee-approval-chain', 2,
 'THEN(validateEmployee, SWITCH(getApprovalTier).to(hrApproval, managerApproval, ceoApproval), sendNotification)');
```

**Scripts:**
```sql
-- Validate employee
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('验证员工', 'validateEmployee', 'qlexpress',
 'employee = context.getData("employee");
  if (employee == null || employee.name == null) {
      throw new Exception("Invalid employee data");
  }
  return true;');

-- Route to approval tier
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('获取审批层级', 'getApprovalTier', 'qlexpress',
 'employee = context.getData("employee");
  salary = employee.salary;
  if (salary < 50000) {
      return "hrApproval";
  } else if (salary < 150000) {
      return "managerApproval";
  } else {
      return "ceoApproval";
  }');
```

### GREEN Phase: Verification

1. Insert chain and scripts into database
2. Reload flow engine: `POST /liteflow/chain/reloadAll`
3. Execute test: `POST /liteflow/execution/execute`
4. Verify execution log: `GET /liteflow/execution/queryLog`
5. Check output: Correct approval tier selected

### REFACTOR Phase: Optimize

- Extract common validation logic to reusable sub-chain
- Add exception handling for edge cases
- Add execution metrics monitoring

---

## Version Information

**Skill Version:** 1.0.0
**LiteFlow Version:** 2.15.3
**SmartAdmin Compatibility:** v4.0.0+
**Last Updated:** 2026-01-25

**Related Documentation:**
- [LiteFlow Module README](../../../../docs/plans/liteflow/README.md)
- [Architecture Documentation](../../../../docs/plans/liteflow/architecture.md)
- [Migration Guide](../../../../docs/plans/liteflow/migration-guide.md)
- [Database Schema](../../../../docs/plans/liteflow/database-schema.md)

**Maintainer:** SmartAdmin AI Team
