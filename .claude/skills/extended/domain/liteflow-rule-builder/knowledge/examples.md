# LiteFlow Examples - Real SmartAdmin Cases

**Version**: 1.0.0
**Last Updated**: 2026-02-02

This document provides complete, production-ready examples of LiteFlow workflows integrated with SmartAdmin patterns.

---

## Example 1: Order Processing Workflow (THEN Pattern)

### Business Requirements

1. Validate order data format
2. Check inventory availability
3. Calculate price with VIP discounts
4. Reserve inventory
5. Create order record
6. Send confirmation notifications

### Complete Implementation

**Step 1: Database Inserts**

```sql
-- Insert chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, status) VALUES
('Order Processing Workflow', 'order-process-chain', 1,
 'THEN(validateOrder, checkInventory, calculatePrice, reserveInventory, createOrder, sendConfirmation)',
 1);

-- Insert script: validateOrder
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Validate Order', 'validateOrder', 'qlexpress',
 'order = context.getData("order");
  if (order == null) throw new Exception("Order is null");
  if (order.productId == null || order.productId <= 0) throw new Exception("Invalid product ID");
  if (order.quantity == null || order.quantity <= 0) throw new Exception("Invalid quantity");
  log.info("Order validated: productId=" + order.productId);
  return true;',
 1);

-- Insert script: checkInventory
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Check Inventory', 'checkInventory', 'qlexpress',
 'order = context.getData("order");
  inventoryService = context.getBean("inventoryService");
  stock = inventoryService.getAvailableStock(order.productId);
  if (stock < order.quantity) throw new Exception("Insufficient inventory");
  context.setData("availableStock", stock);
  log.info("Inventory check passed: " + stock + " units");
  return true;',
 1);

-- Insert script: calculatePrice
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Calculate Price', 'calculatePrice', 'qlexpress',
 'order = context.getData("order");
  user = context.getData("user");
  priceService = context.getBean("priceService");
  price = priceService.calculatePrice(order.productId, order.quantity, user.vipLevel);
  context.setData("finalPrice", price);
  log.info("Price calculated: $" + price);
  return true;',
 1);

-- Reload engine
-- POST /liteflow/chain/reloadAll
```

**Step 2: Service Layer Integration**

```java
package net.lab1024.sa.admin.module.business.order.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.base.module.support.liteflow.service.LiteFlowExecutionService;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionForm;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionResultVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final OrderDao orderDao;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Prepare order entity
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        order.setUserId(RequestContext.getRequestUser().getUserId());
        order.setCreateTime(LocalDateTime.now());

        // Execute LiteFlow chain
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-process-chain");
        executionForm.setInputParams(Map.of(
            "order", order,
            "user", RequestContext.getRequestUser()
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (!result.getOk()) {
            return ResponseDTO.error(UserErrorCode.ORDER_CREATE_FAILED, result.getMsg());
        }

        // Extract processed order
        Map<String, Object> output = result.getData().getOutputResult();
        BigDecimal finalPrice = (BigDecimal) output.get("finalPrice");

        order.setFinalPrice(finalPrice);
        orderDao.insert(order);

        return ResponseDTO.ok(SmartBeanUtil.copy(order, OrderVO.class));
    }
}
```

**Step 3: Test Execution**

```bash
# Execute chain via API
POST /api/order/create
{
  "productId": 1001,
  "quantity": 2,
  "notes": "Express delivery"
}

# Expected Response:
{
  "ok": true,
  "code": 1,
  "msg": "Success",
  "data": {
    "orderId": 100234,
    "productId": 1001,
    "quantity": 2,
    "finalPrice": 199.98,
    "orderNo": "ORD20260202001"
  }
}
```

---

## Example 2: Multi-Channel Notification (WHEN Pattern)

### Business Requirements

After order confirmation, simultaneously:
- Send email notification
- Send SMS notification
- Send push notification
- Update Redis cache

### Complete Implementation

**Step 1: Database Inserts**

```sql
-- Insert chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, status) VALUES
('Multi-Channel Notification', 'notification-chain', 1,
 'WHEN(sendEmail, sendSMS, sendPush, updateCache)',
 1);

-- Insert script: sendEmail
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Send Email', 'sendEmail', 'qlexpress',
 'order = context.getData("order");
  user = context.getData("user");
  emailService = context.getBean("emailService");
  subject = "Order Confirmed - #" + order.orderNo;
  body = "Thank you! Total: $" + order.finalPrice;
  emailService.send(user.email, subject, body);
  log.info("Email sent to: " + user.email);
  return true;',
 1);

-- Insert script: sendSMS
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Send SMS', 'sendSMS', 'qlexpress',
 'order = context.getData("order");
  user = context.getData("user");
  smsService = context.getBean("smsService");
  message = "Order confirmed. Track at: example.com/orders/" + order.orderNo;
  smsService.send(user.phone, message);
  log.info("SMS sent to: " + user.phone);
  return true;',
 1);

-- Insert script: sendPush
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Send Push Notification', 'sendPush', 'qlexpress',
 'order = context.getData("order");
  user = context.getData("user");
  pushService = context.getBean("pushNotificationService");
  pushService.send(user.userId, "Order Confirmed", "Order #" + order.orderNo);
  log.info("Push sent to userId: " + user.userId);
  return true;',
 1);

-- Insert script: updateCache
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Update Cache', 'updateCache', 'qlexpress',
 'order = context.getData("order");
  cacheService = context.getBean("redisCacheService");
  cacheKey = "order:" + order.orderId;
  cacheService.set(cacheKey, order, 3600);
  log.info("Cache updated: " + cacheKey);
  return true;',
 1);
```

**Step 2: Service Integration**

```java
public ResponseDTO<Void> sendOrderNotifications(Long orderId) {
    OrderEntity order = orderDao.selectById(orderId);
    UserEntity user = userDao.selectById(order.getUserId());

    // Execute parallel notification chain
    LiteFlowExecutionForm form = new LiteFlowExecutionForm();
    form.setChainCode("notification-chain");
    form.setInputParams(Map.of(
        "order", order,
        "user", user
    ));

    ResponseDTO<LiteFlowExecutionResultVO> result =
        liteFlowExecutionService.execute(form);

    if (!result.getOk()) {
        return ResponseDTO.error(UserErrorCode.NOTIFICATION_FAILED, result.getMsg());
    }

    return ResponseDTO.ok();
}
```

**Performance Improvement**:
- Sequential: 2s (email) + 1.5s (SMS) + 1s (push) + 0.5s (cache) = **5 seconds**
- Parallel (WHEN): max(2s, 1.5s, 1s, 0.5s) = **2 seconds**
- **60% time reduction**

---

## Example 3: VIP Discount Logic (IF Pattern)

### Business Requirements

- VIP Level 1-2: 10% discount
- VIP Level 3+: 20% discount + free shipping
- Regular users: No discount

### Complete Implementation

**Step 1: Database Inserts**

```sql
-- Insert chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, status) VALUES
('VIP Pricing Workflow', 'vip-pricing-chain', 2,
 'THEN(validateOrder, IF(isVipMember, IF(isVipLevel3Plus, THEN(apply20PercentDiscount, addFreeShipping), apply10PercentDiscount), calculateRegularPrice), finalizePrice)',
 1);

-- Insert script: isVipMember
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Check VIP Membership', 'isVipMember', 'qlexpress',
 'user = context.getData("user");
  memberService = context.getBean("memberService");
  vipLevel = memberService.getVipLevel(user.userId);
  if (vipLevel != null && vipLevel >= 1) {
      context.setData("vipLevel", vipLevel);
      log.info("VIP member: level=" + vipLevel);
      return true;
  } else {
      log.info("Regular user");
      return false;
  }',
 1);

-- Insert script: isVipLevel3Plus
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Check VIP Level 3+', 'isVipLevel3Plus', 'qlexpress',
 'vipLevel = context.getData("vipLevel");
  return vipLevel >= 3;',
 1);

-- Insert script: apply20PercentDiscount
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Apply 20% Discount', 'apply20PercentDiscount', 'qlexpress',
 'order = context.getData("order");
  originalPrice = order.originalPrice;
  discountedPrice = originalPrice * 0.8;
  context.setData("finalPrice", discountedPrice);
  context.setData("discountApplied", "20%");
  log.info("20% discount applied: $" + originalPrice + " -> $" + discountedPrice);
  return true;',
 1);
```

---

## Example 4: Approval Workflow (SWITCH Pattern)

### Business Requirements

- Amount < $10,000: Auto-approve
- Amount $10,000-$100,000: Manager approval
- Amount $100,000-$1M: Director approval
- Amount $1M+: CEO approval

### Complete Implementation

**Step 1: Database Inserts**

```sql
-- Insert chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, status) VALUES
('Expense Approval Workflow', 'expense-approval-chain', 2,
 'THEN(validateExpense, SWITCH(determineApprovalLevel).to(autoApprove, managerApproval, directorApproval, ceoApproval), notifyRequester)',
 1);

-- Insert script: determineApprovalLevel
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Determine Approval Level', 'determineApprovalLevel', 'qlexpress',
 'expense = context.getData("expense");
  amount = expense.amount;
  if (amount < 10000) {
      log.info("Auto-approval: $" + amount);
      return "autoApprove";
  } else if (amount < 100000) {
      log.info("Manager approval: $" + amount);
      return "managerApproval";
  } else if (amount < 1000000) {
      log.info("Director approval: $" + amount);
      return "directorApproval";
  } else {
      log.info("CEO approval: $" + amount);
      return "ceoApproval";
  }',
 1);

-- Insert script: autoApprove
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Auto Approve', 'autoApprove', 'qlexpress',
 'expense = context.getData("expense");
  expense.status = "APPROVED";
  expense.approvalLevel = "AUTO";
  context.setData("approvalStatus", "APPROVED");
  log.info("Expense auto-approved: " + expense.expenseId);
  return true;',
 1);

-- Insert script: managerApproval
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Manager Approval', 'managerApproval', 'qlexpress',
 'expense = context.getData("expense");
  approvalService = context.getBean("approvalService");
  taskId = approvalService.createTask(expense.expenseId, "MANAGER", expense.managerId);
  context.setData("approvalTaskId", taskId);
  context.setData("approvalStatus", "PENDING_MANAGER");
  log.info("Manager approval task created: " + taskId);
  return true;',
 1);
```

**Step 2: Service Integration**

```java
public ResponseDTO<ExpenseVO> submitExpense(ExpenseAddForm form) {
    ExpenseEntity expense = SmartBeanUtil.copy(form, ExpenseEntity.class);
    expense.setStatus("PENDING");

    // Execute approval chain
    LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
    executionForm.setChainCode("expense-approval-chain");
    executionForm.setInputParams(Map.of(
        "expense", expense,
        "requester", RequestContext.getRequestUser()
    ));

    ResponseDTO<LiteFlowExecutionResultVO> result =
        liteFlowExecutionService.execute(executionForm);

    if (!result.getOk()) {
        return ResponseDTO.error(UserErrorCode.EXPENSE_SUBMIT_FAILED, result.getMsg());
    }

    // Extract approval status
    Map<String, Object> output = result.getData().getOutputResult();
    String approvalStatus = (String) output.get("approvalStatus");

    expense.setStatus(approvalStatus);
    expenseDao.insert(expense);

    return ResponseDTO.ok(SmartBeanUtil.copy(expense, ExpenseVO.class));
}
```

---

## Example 5: Batch Invoice Processing (FOR Pattern)

### Business Requirements

- Process pending invoices from queue
- For each invoice: validate, calculate tax, generate PDF
- Continue until queue is empty
- Track success/failure counts

### Complete Implementation

**Step 1: Database Inserts**

```sql
-- Insert chain
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data, status) VALUES
('Batch Invoice Processing', 'batch-invoice-chain', 3,
 'FOR(getNextInvoice).DO(THEN(validateInvoice, calculateTax, generatePDF, updateStatus)).BREAK(noMoreInvoices)',
 1);

-- Insert script: getNextInvoice
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Get Next Invoice', 'getNextInvoice', 'qlexpress',
 'invoiceQueue = context.getData("invoiceQueue");
  if (invoiceQueue == null || invoiceQueue.isEmpty()) {
      log.info("No more invoices");
      return null;
  }
  nextInvoice = invoiceQueue.poll();
  context.setData("currentInvoice", nextInvoice);
  log.info("Processing invoice: " + nextInvoice.invoiceNo);
  return nextInvoice;',
 1);

-- Insert script: noMoreInvoices
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data, status) VALUES
('Check Empty Queue', 'noMoreInvoices', 'qlexpress',
 'invoiceQueue = context.getData("invoiceQueue");
  return invoiceQueue == null || invoiceQueue.isEmpty();',
 1);
```

**Step 2: Service Integration**

```java
public ResponseDTO<BatchResultVO> processPendingInvoices() {
    // Load pending invoices
    List<InvoiceEntity> pendingInvoices = invoiceDao.selectPending();
    Queue<InvoiceEntity> invoiceQueue = new LinkedList<>(pendingInvoices);

    AtomicInteger processedCount = new AtomicInteger(0);
    AtomicInteger failedCount = new AtomicInteger(0);

    // Execute batch chain
    LiteFlowExecutionForm form = new LiteFlowExecutionForm();
    form.setChainCode("batch-invoice-chain");
    form.setInputParams(Map.of(
        "invoiceQueue", invoiceQueue,
        "processedCount", processedCount,
        "failedCount", failedCount
    ));

    ResponseDTO<LiteFlowExecutionResultVO> result =
        liteFlowExecutionService.execute(form);

    // Extract results
    BatchResultVO resultVO = new BatchResultVO();
    resultVO.setTotal(pendingInvoices.size());
    resultVO.setProcessed(processedCount.get());
    resultVO.setFailed(failedCount.get());

    return ResponseDTO.ok(resultVO);
}
```

---

**See Also**:
- [Quick Reference](quick-reference.md) - Command reference and decision matrix
- [Patterns Guide](patterns.md) - Detailed pattern explanations
- [Best Practices](best-practices.md) - Performance optimization
