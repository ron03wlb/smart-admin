# LiteFlow DSL Examples

> Extracted from SKILL.md to keep the main file focused on AI agent instructions.
> This file contains complete EL expression examples, QLExpress scripts, SQL inserts, and SmartAdmin integration code.

---

## Pattern 1: Sequential Execution (THEN)

### EL Expression
```javascript
THEN(
  validateOrder,
  checkInventory,
  calculatePrice,
  createOrder,
  sendNotification
)
```

### Database Insert
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Order Processing Flow', 'order-process-chain', 1,
 'THEN(validateOrder, checkInventory, calculatePrice, createOrder, sendNotification)');
```

### QLExpress Script (validateOrder)
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Validate Order', 'validateOrder', 'qlexpress',
'order = context.getData("order");

if (order == null) {
    throw new Exception("Order cannot be null");
}
if (order.amount == null || order.amount <= 0) {
    throw new Exception("Invalid order amount: " + order.amount);
}
if (order.productId == null) {
    throw new Exception("Product ID is required");
}

log.info("Order validated: orderId=" + order.orderId);
return true;');
```

### SmartAdmin Service Integration
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

## Pattern 2: Parallel Execution (WHEN)

### EL Expression
```javascript
THEN(
  processOrder,
  WHEN(sendEmail, sendSMS, sendPushNotification, updateCache)
)
```

### Database Insert
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Notification Flow', 'notification-chain', 1,
 'WHEN(sendEmail, sendSMS, sendPushNotification)');
```

### QLExpress Scripts
```sql
-- Send Email Script
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Send Email', 'sendEmail', 'qlexpress',
'user = context.getData("user");
emailService = context.getBean("emailService");
emailService.send(user.email, "Order Confirmed", "Your order has been confirmed");
log.info("Email sent to: " + user.email);
return true;');

-- Send SMS Script
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Send SMS', 'sendSMS', 'qlexpress',
'user = context.getData("user");
smsService = context.getBean("smsService");
smsService.send(user.phone, "Order confirmed. Track at: example.com/orders");
log.info("SMS sent to: " + user.phone);
return true;');
```

---

## Pattern 3: Conditional Logic (IF)

### EL Expression
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

### Database Insert
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('VIP Order Processing', 'vip-order-chain', 2,
 'THEN(validateOrder, checkInventory, IF(isVipMember, THEN(calculateVipDiscount, applyDiscount), calculateRegularPrice), createOrder)');
```

### Condition Script (isVipMember)
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Check VIP Member', 'isVipMember', 'qlexpress',
'user = context.getData("user");
memberService = context.getBean("memberService");
vipLevel = memberService.getVipLevel(user.userId);

if (vipLevel != null && vipLevel >= 1) {
    context.setData("vipLevel", vipLevel);
    return true;
} else {
    return false;
}');
```

### Nested IF Example
```javascript
IF(
  isHighValue,
  IF(requiresApproval, manualReview, autoApprove),
  fastTrack
)
```

---

## Pattern 4: Switch Routing (SWITCH)

### EL Expression
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

### Database Insert
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Approval Flow', 'approval-chain', 2,
 'THEN(submitRequest, SWITCH(getApprovalLevel).to(level1Approval, level2Approval, level3Approval, ceoApproval))');
```

### Router Script (getApprovalLevel)
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Get Approval Level', 'getApprovalLevel', 'qlexpress',
'request = context.getData("request");
amount = request.amount;

if (amount < 10000) {
    return "level1Approval";
} else if (amount < 100000) {
    return "level2Approval";
} else if (amount < 1000000) {
    return "level3Approval";
} else {
    return "ceoApproval";
}');
```

---

## Pattern 5: Loop Iteration (FOR)

### EL Expression
```javascript
FOR(getNextOrder).DO(
  THEN(validateOrder, processPayment, shipOrder)
).BREAK(noMoreOrders)
```

### Database Insert
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Batch Order Processing', 'batch-order-chain', 3,
 'FOR(getNextOrder).DO(THEN(validateOrder, processPayment, shipOrder)).BREAK(noMoreOrders)');
```

### Iterator Script (getNextOrder)
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Get Next Order', 'getNextOrder', 'qlexpress',
'orderQueue = context.getData("orderQueue");

if (orderQueue == null || orderQueue.isEmpty()) {
    return null;
}

nextOrder = orderQueue.poll();
context.setData("currentOrder", nextOrder);
return nextOrder;');
```

### Break Condition Script
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Check Queue Empty', 'noMoreOrders', 'qlexpress',
'orderQueue = context.getData("orderQueue");
return orderQueue == null || orderQueue.isEmpty();');
```

---

## Pattern 6: Nested Chains (Sub-flows)

```sql
-- Main chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Main Flow', 'main-chain', 1,
 'THEN(validateInput, processData, auditCleanupChain)');

-- Sub-chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Audit Cleanup Sub-flow', 'auditCleanupChain', 1,
 'THEN(auditLog, cleanup)');
```

---

## Pattern 7: Exception Handling (CATCH)

### EL Expression
```javascript
THEN(
  riskCheck,
  processPayment
).CATCH(handlePaymentError)
```

### Error Handler Script
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Payment Error Handler', 'handlePaymentError', 'qlexpress',
'exception = context.getData("exception");
log.error("Payment failed: " + exception.getMessage());

order = context.getData("order");
orderService = context.getBean("orderService");
orderService.cancelOrder(order.orderId);

notificationService = context.getBean("notificationService");
notificationService.sendPaymentFailedEmail(order.userId);

return false;
');
```

---

## SmartAdmin Database Storage Pattern

### Step 1: Insert Chain Definition
```sql
INSERT INTO t_liteflow_chain (
  chain_name, chain_code, chain_type, chain_data, status, create_user_id, create_user_name
) VALUES (
  'Employee Approval Flow',
  'employee-approval-chain',
  2,
  'THEN(validateEmployee, SWITCH(getApprovalLevel).to(hrApproval, managerApproval, ceoApproval))',
  1,
  1,
  'admin'
);
```

### Step 2: Insert Script Nodes
```sql
INSERT INTO t_liteflow_script (
  script_name, script_code, script_type, script_data, status
) VALUES (
  'Validate Employee',
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

### Step 3: Reload Flow Engine
```bash
POST /liteflow/chain/reloadAll
```

---

## SmartAdmin Service Layer Integration

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final EmployeeDao employeeDao;

    public ResponseDTO<EmployeeVO> approveEmployee(Long employeeId) {
        EmployeeEntity employee = employeeDao.selectById(employeeId);
        if (employee == null) {
            return ResponseDTO.userErrorParam("Employee not found");
        }

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

        Map<String, Object> output = result.getData().getOutputResult();
        String approvalStatus = (String) output.get("approvalStatus");

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
order = context.getData("order");
user = context.getData("user");
context.setData("calculatedPrice", finalPrice);
context.setData("discountApplied", true);
```

### Calling Spring Beans
```javascript
orderService = context.getBean("orderService");
emailService = context.getBean("emailService");
result = orderService.createOrder(order);
emailService.send(user.email, "Order Confirmed", emailBody);
```

### Logging
```javascript
log.info("Processing order: " + order.orderId);
log.warn("Low inventory: " + inventory.stock);
log.error("Payment failed: " + exception.getMessage());
```

### Variable Types
```javascript
stringVar = "hello";
intVar = 123;
longVar = 123L;
doubleVar = 123.45;
boolVar = true;
listVar = new ArrayList();
mapVar = new HashMap();
amount = (BigDecimal) context.getData("amount");
userId = (Long) context.getData("userId");
```

---

## RED Phase Baseline Test

### Expected Chain
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Employee Approval Flow', 'employee-approval-chain', 2,
 'THEN(validateEmployee, SWITCH(getApprovalTier).to(hrApproval, managerApproval, ceoApproval), sendNotification)');
```

### Expected Scripts
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Validate Employee', 'validateEmployee', 'qlexpress',
 'employee = context.getData("employee");
  if (employee == null || employee.name == null) {
      throw new Exception("Invalid employee data");
  }
  return true;');

INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Get Approval Tier', 'getApprovalTier', 'qlexpress',
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

### Verification Steps
1. Insert chain and scripts into database
2. Reload flow engine: `POST /liteflow/chain/reloadAll`
3. Execute test: `POST /liteflow/execution/execute`
4. Verify execution log: `GET /liteflow/execution/queryLog`
5. Check output: Correct approval tier selected

---

**Version**: 1.0.0
**Extracted From**: SKILL.md v1.0.0
**Last Updated**: 2026-02-06
