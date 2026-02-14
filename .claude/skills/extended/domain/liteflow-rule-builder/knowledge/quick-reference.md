# LiteFlow Rule Builder - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: liteflow-rule-builder (P1 - Extended/Domain)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Example |
|---------|---------|---------|
| Reload Engine | Hot-reload chains after DB update | POST /liteflow/chain/reloadAll |
| Execute Chain | Run workflow with input params | POST /liteflow/execution/execute |
| Query Log | Check execution history | GET /liteflow/execution/queryLog |
| List Chains | View all chains | GET /liteflow/chain/query |
| Test Script | Validate QLExpress syntax | QLExpress online validator |

### Rapid Development Workflow

Time estimate: 15-30 minutes per workflow

| Step | Action | Time |
|------|--------|------|
| 1. Analyze Requirements | Identify sequential/parallel/conditional logic | ~3 min |
| 2. Select Pattern | Choose from 7 patterns (see Decision Matrix) | ~2 min |
| 3. Generate EL Expression | Write THEN/WHEN/IF/SWITCH chain | ~5 min |
| 4. Generate Scripts | Write QLExpress scripts for logic nodes | ~10 min |
| 5. Insert DB | Execute SQL inserts | ~2 min |
| 6. Reload & Test | POST /liteflow/chain/reloadAll + execute | ~8 min |

---

## LiteFlow Pattern Selection (Decision Matrix)

### Pattern 1: Sequential Execution (THEN)

**Trigger Keywords**: "then", "after", "sequential", "step-by-step", "order processing"

**Use When**:
- Operations must execute in strict order (A → B → C)
- Step B depends on result from step A
- Audit trails requiring sequential logging

**Quick Template**:
```javascript
THEN(nodeA, nodeB, nodeC)
```

**Database Insert**:
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Order Processing', 'order-process-chain', 1,
 'THEN(validateOrder, checkInventory, calculatePrice, createOrder)');
```

**SmartAdmin Integration**:
```java
LiteFlowExecutionForm form = new LiteFlowExecutionForm();
form.setChainCode("order-process-chain");
form.setInputParams(Map.of("order", order));
ResponseDTO<LiteFlowExecutionResultVO> result = liteFlowExecutionService.execute(form);
```

**Time Estimate**: 5-10 min

---

### Pattern 2: Parallel Execution (WHEN)

**Trigger Keywords**: "parallel", "concurrently", "simultaneously", "broadcast", "multi-channel"

**Use When**:
- Independent operations that can run concurrently
- Performance optimization (parallel API calls)
- Broadcasting to multiple channels (email + SMS + push)

**Quick Template**:
```javascript
WHEN(nodeA, nodeB, nodeC)
```

**Nested Example**:
```javascript
THEN(
  processOrder,
  WHEN(sendEmail, sendSMS, sendPush, updateCache)
)
```

**Performance Note**: Total time = max(nodeA, nodeB, nodeC), not sum

**Time Estimate**: 10-15 min

---

### Pattern 3: Conditional Logic (IF)

**Trigger Keywords**: "if", "condition", "branch", "VIP", "feature flag", "A/B testing"

**Use When**:
- Business rule branching (different paths for user types)
- Conditional validations
- Feature toggles

**Quick Template**:
```javascript
IF(conditionNode, thenNode, elseNode)
```

**VIP Discount Example**:
```javascript
THEN(
  validateOrder,
  IF(isVipMember,
     THEN(calculateVipDiscount, applyDiscount),
     calculateRegularPrice),
  createOrder
)
```

**Condition Script Returns**: `true` (THEN branch) or `false` (ELSE branch)

**Time Estimate**: 10-15 min

---

### Pattern 4: Switch Routing (SWITCH)

**Trigger Keywords**: "approval level", "routing", "multi-tier", "escalation", "strategy pattern"

**Use When**:
- Multi-tier workflows (approval levels, escalation)
- Dynamic routing based on runtime values
- 3+ distinct execution paths

**Quick Template**:
```javascript
SWITCH(routerNode).to(caseA, caseB, caseC)
```

**Approval Example**:
```javascript
THEN(
  submitRequest,
  SWITCH(getApprovalLevel).to(
    level1Approval,
    level2Approval,
    ceoApproval
  )
)
```

**Router Script Returns**: Node ID as String (e.g., "level1Approval")

**SWITCH vs IF**:
- SWITCH: Cleaner for 3+ branches, value-based routing
- IF: Better for boolean conditions, binary decisions

**Time Estimate**: 15-20 min

---

### Pattern 5: Loop Iteration (FOR)

**Trigger Keywords**: "batch", "loop", "iterate", "for each", "retry", "polling"

**Use When**:
- Batch processing (multiple records)
- Iterative workflows (retry logic)
- Dynamic loop count

**Quick Template**:
```javascript
FOR(iteratorNode).DO(processingNode).BREAK(breakConditionNode)
```

**Batch Order Example**:
```javascript
FOR(getNextOrder).DO(
  THEN(validateOrder, processPayment, shipOrder)
).BREAK(noMoreOrders)
```

**Performance Warning**: Large loops (1000+ items) should use async execution

**Time Estimate**: 20-30 min

---

### Pattern 6: Nested Chains (Sub-flows)

**Trigger Keywords**: "reusable", "sub-flow", "common logic", "audit + cleanup"

**Use When**:
- Reusable sub-workflows
- Common operations used in multiple chains
- Complex workflows requiring decomposition

**Quick Template**:
```javascript
THEN(
  validateInput,
  processData,
  auditCleanupChain  // Reusable sub-chain
)
```

**Time Estimate**: 15-20 min

---

### Pattern 7: Exception Handling (CATCH)

**Trigger Keywords**: "error handling", "rollback", "failure recovery", "graceful degradation"

**Use When**:
- Critical operations requiring error recovery
- Transactions needing rollback
- Graceful failure handling

**Quick Template**:
```javascript
THEN(riskCheck, processPayment).CATCH(handlePaymentError)
```

**Error Handler Script**:
```javascript
exception = context.getData("exception");
log.error("Payment failed: " + exception.getMessage());
// Rollback logic
orderService.cancelOrder(order.orderId);
return false;  // Mark chain as failed
```

**Time Estimate**: 10-15 min

---

## QLExpress Script Quick Patterns

### Context Operations

| Operation | Code Example | Use Case |
|-----------|--------------|----------|
| Get data | `order = context.getData("order");` | Retrieve input |
| Set data | `context.setData("price", 99.99);` | Pass to next node |
| Get bean | `service = context.getBean("orderService");` | Call Spring service |

### Logging

```javascript
log.info("Processing order: " + order.orderId);
log.warn("Low inventory: " + inventory.stock);
log.error("Payment failed: " + exception.getMessage());
```

### Return Values

| Node Type | Return Value | Example |
|-----------|--------------|---------|
| Condition (IF) | `true` or `false` | `return user.vipLevel >= 3;` |
| Router (SWITCH) | Node ID as String | `return "level2Approval";` |
| Iterator (FOR) | Next item or null | `return orderQueue.poll();` |
| Processing (THEN/WHEN) | `true` (success) or throw | `return true;` |

---

## Common Errors and Quick Fixes

### Error 1: Chain Not Found

**Error Message**: "Chain [order-chain] not found in database"

**Cause**: Forgot to reload engine after DB insert

**Fix**:
```bash
POST /liteflow/chain/reloadAll
```

---

### Error 2: NullPointerException in Script

**Error Message**: "Cannot invoke method on null object"

**Cause**: Missing null check for context data

**Fix**:
```javascript
// ❌ BAD
user = context.getData("user");
email = user.email;  // NPE if user is null

// ✅ GOOD
user = context.getData("user");
if (user == null) {
    throw new Exception("User not found in context");
}
email = user.email;
```

---

### Error 3: SWITCH Routing Failed

**Error Message**: "Cannot route to node [0]"

**Cause**: Router script returns int instead of String

**Fix**:
```javascript
// ❌ BAD
return 0;  // Index-based routing fails

// ✅ GOOD
return "level1Approval";  // Return node ID
```

---

### Error 4: Parallel Nodes Not Executing

**Error Message**: "Only first node executed in WHEN block"

**Cause**: Used THEN instead of WHEN

**Fix**:
```javascript
// ❌ BAD (sequential)
THEN(sendEmail, sendSMS)

// ✅ GOOD (parallel)
WHEN(sendEmail, sendSMS)
```

---

### Error 5: Condition Script Returns Wrong Type

**Error Message**: "IF condition expects boolean, got String"

**Cause**: Condition script doesn't return boolean

**Fix**:
```javascript
// ❌ BAD
user.vipLevel >= 3;  // No return statement

// ✅ GOOD
return user.vipLevel >= 3;  // Explicit return
```

---

## Database Quick Templates

### Insert Chain

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
  'Order Processing',           -- Display name
  'order-process-chain',        -- Unique code
  1,                            -- Type: 1=普通, 2=条件, 3=循环
  'THEN(validateOrder, checkInventory, createOrder)',
  1,                            -- Status: 1=启用, 0=禁用
  1,                            -- Creator user ID
  'admin'                       -- Creator username
);
```

### Insert Script

```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status
) VALUES (
  'Validate Order',
  'validateOrder',
  'qlexpress',
  'order = context.getData("order");
   if (order == null) {
       throw new Exception("Order is null");
   }
   return true;',
  1
);
```

---

## SmartAdmin Integration Checklist

Before integrating LiteFlow chain in Service layer:

- [ ] Chain and scripts inserted in database
- [ ] Engine reloaded: `POST /liteflow/chain/reloadAll`
- [ ] Test execution: `POST /liteflow/execution/execute`
- [ ] Verify logs: `GET /liteflow/execution/queryLog`
- [ ] Service uses `LiteFlowExecutionService.execute()`
- [ ] Input params use `SmartBeanUtil.copy()` for DTOs
- [ ] Error handling with `ResponseDTO.error()`
- [ ] Constructor injection (`@RequiredArgsConstructor`)

---

## Time Estimates (Production Data)

| Workflow Complexity | Pattern Count | Time Estimate |
|---------------------|---------------|---------------|
| Simple (sequential) | 1 (THEN) | 5-10 min |
| Medium (conditional) | 2-3 (THEN + IF/WHEN) | 15-20 min |
| Complex (multi-tier) | 4+ (THEN + SWITCH + WHEN + CATCH) | 30-45 min |
| Very Complex (batch) | 5+ (FOR + nested chains) | 45-60 min |

**Breakdown**:
- Pattern selection: 10%
- EL expression: 20%
- QLExpress scripts: 50%
- Testing & debugging: 20%

---

## Evrete Migration Quick Reference

| Evrete Pattern | LiteFlow Equivalent | Notes |
|----------------|---------------------|-------|
| `@Rule(salience=100)` | `THEN(nodeA, nodeB)` | Order by THEN sequence |
| `@Where("$order.amount > 0")` | QLExpress if statement | Move condition to script |
| Parallel rule execution | `WHEN(nodeA, nodeB)` | Explicit parallel |
| Fact insertion chaining | `context.setData()` | Pass data via context |
| Rete conflict resolution | Manual IF/SWITCH | No auto resolution |

**Migration Steps**:
1. Identify rule dependencies
2. Map to LiteFlow patterns (THEN for sequential, WHEN for parallel)
3. Convert @Where conditions to QLExpress
4. Test functional equivalence

**See**: [Migration Guide](../../../../docs/plans/liteflow/migration-guide.md)

---

**See Also**:
- [Patterns Guide](patterns.md) - Detailed pattern explanations with examples
- [Examples Guide](examples.md) - Real SmartAdmin workflow cases
- [Best Practices](best-practices.md) - Performance optimization and debugging
- [LiteFlow Architecture](../../../../docs/plans/liteflow/architecture.md) - System design
- [Database Schema](../../../../docs/plans/liteflow/database-schema.md) - Table structures
