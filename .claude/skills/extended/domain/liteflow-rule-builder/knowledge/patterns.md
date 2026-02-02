# LiteFlow Patterns - Detailed Guide

**Version**: 1.0.0
**Last Updated**: 2026-02-02

This document provides in-depth explanations of the 7 core LiteFlow orchestration patterns with detailed examples, best practices, and SmartAdmin integration patterns.

---

## Pattern 1: Sequential Execution (THEN)

### Overview

The THEN pattern executes nodes in strict sequential order. Each node completes before the next begins, making it ideal for workflows where later steps depend on earlier results.

### Detailed Execution Flow

```
Step 1 (validateOrder) ✓ Complete
  ↓ Context updated
Step 2 (checkInventory) ✓ Complete
  ↓ Context updated
Step 3 (calculatePrice) ✓ Complete
  ↓ Context updated
Step 4 (createOrder) ✓ Complete
```

### Best Practices

**When to Use**:
- Multi-step processes requiring strict order
- Each step modifies context for next step
- Audit trails needing sequential timestamps

**When to Avoid**:
- Independent operations (use WHEN for parallel)
- Performance-critical sections (consider WHEN for speedup)

### Advanced Example: Order Processing with Validation

**Business Requirements**:
1. Validate order format and data
2. Check inventory availability
3. Calculate price with discounts
4. Reserve inventory
5. Create order record
6. Send confirmation notification

**EL Expression**:
```javascript
THEN(
  validateOrderFormat,
  checkInventoryAvailability,
  calculateFinalPrice,
  reserveInventory,
  createOrderRecord,
  sendConfirmation
)
```

**QLExpress Script: validateOrderFormat**:
```javascript
// Get order from context
order = context.getData("order");

// Validation rules
if (order == null) {
    throw new Exception("Order cannot be null");
}
if (order.productId == null || order.productId <= 0) {
    throw new Exception("Invalid product ID: " + order.productId);
}
if (order.quantity == null || order.quantity <= 0) {
    throw new Exception("Invalid quantity: " + order.quantity);
}
if (order.userId == null) {
    throw new Exception("User ID is required");
}

// Log validation success
log.info("Order validated: productId=" + order.productId + ", quantity=" + order.quantity);
return true;
```

**QLExpress Script: checkInventoryAvailability**:
```javascript
order = context.getData("order");

// Call Spring service to check inventory
inventoryService = context.getBean("inventoryService");
availableStock = inventoryService.getAvailableStock(order.productId);

// Check availability
if (availableStock < order.quantity) {
    throw new Exception("Insufficient inventory: available=" + availableStock + ", required=" + order.quantity);
}

// Pass inventory info to next node
context.setData("availableStock", availableStock);
log.info("Inventory check passed: " + availableStock + " units available");
return true;
```

**SmartAdmin Service Integration**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final OrderDao orderDao;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Prepare input
        Order order = SmartBeanUtil.copy(form, Order.class);
        order.setUserId(RequestContext.getRequestUser().getUserId());
        order.setCreateTime(LocalDateTime.now());

        // Execute LiteFlow chain
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-creation-chain");
        executionForm.setInputParams(Map.of(
            "order", order,
            "requestUser", RequestContext.getRequestUser()
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (!result.getOk()) {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }

        // Extract result
        Order createdOrder = (Order) result.getData().getOutputResult().get("order");

        return ResponseDTO.ok(SmartBeanUtil.copy(createdOrder, OrderVO.class));
    }
}
```

---

## Pattern 2: Parallel Execution (WHEN)

### Overview

The WHEN pattern executes nodes concurrently in separate threads. All nodes complete before proceeding to the next step, making it ideal for independent operations.

### Detailed Execution Flow

```
Start WHEN block
  ↓
  ├─→ sendEmail (Thread 1) ──────┐
  ├─→ sendSMS (Thread 2) ────────┤
  ├─→ sendPush (Thread 3) ───────┤→ All complete → Continue
  └─→ updateCache (Thread 4) ────┘
```

### Performance Characteristics

**Time Savings**:
- Sequential: 2s + 1.5s + 1s + 0.5s = **5 seconds**
- Parallel (WHEN): max(2s, 1.5s, 1s, 0.5s) = **2 seconds**
- **60% time reduction**

### Best Practices

**When to Use**:
- Independent operations (no data dependencies)
- I/O-bound operations (API calls, database queries, email/SMS)
- Broadcasting to multiple channels

**When to Avoid**:
- Operations with dependencies (use THEN)
- CPU-bound operations on limited cores
- Operations requiring strict ordering

### Advanced Example: Multi-Channel Notification

**Business Requirements**:
After order confirmation, simultaneously:
1. Send email notification
2. Send SMS notification
3. Send push notification
4. Update Redis cache
5. Increment user points (async)

**EL Expression**:
```javascript
THEN(
  confirmOrder,
  WHEN(
    sendEmailNotification,
    sendSMSNotification,
    sendPushNotification,
    updateRedisCache,
    incrementUserPoints
  ),
  logNotificationSuccess
)
```

**QLExpress Script: sendEmailNotification**:
```javascript
order = context.getData("order");
user = context.getData("user");

// Get email service
emailService = context.getBean("emailService");

// Prepare email content
subject = "Order Confirmed - #" + order.orderNo;
body = "Thank you for your order. Total: $" + order.totalAmount;

// Send email
try {
    emailService.sendOrderConfirmation(user.email, subject, body);
    log.info("Email sent successfully to: " + user.email);
    context.setData("emailSent", true);
} catch (Exception e) {
    log.error("Email failed: " + e.getMessage());
    context.setData("emailSent", false);
    // Don't throw - allow other notifications to proceed
}

return true;
```

**Thread Safety Note**: Each WHEN node gets its own thread. Ensure QLExpress scripts don't modify shared state without synchronization.

---

## Pattern 3: Conditional Logic (IF)

### Overview

The IF pattern provides binary branching based on a boolean condition. The condition node evaluates to `true` (execute THEN branch) or `false` (execute ELSE branch).

### Detailed Execution Flow

```
Evaluate conditionNode
  ↓
  true? ──→ Execute thenNode(s) ──→ Continue
  ↓
  false? ─→ Execute elseNode(s) ──→ Continue
```

### Best Practices

**When to Use**:
- Binary decisions (VIP vs regular, approved vs rejected)
- Feature flags (enable/disable features)
- A/B testing (variant A vs variant B)

**When to Avoid**:
- 3+ branches (use SWITCH instead)
- Complex boolean logic (extract to separate validation node)

### Advanced Example: VIP Pricing Logic

**Business Requirements**:
- VIP Level 1+: 10% discount
- VIP Level 3+: 20% discount + free shipping
- Regular users: No discount

**Nested IF Structure**:
```javascript
THEN(
  validateOrder,
  IF(isVipMember,
     IF(isVipLevel3Plus,
        THEN(apply20PercentDiscount, addFreeShipping),
        apply10PercentDiscount),
     calculateRegularPrice),
  createOrder
)
```

**QLExpress Script: isVipMember**:
```javascript
user = context.getData("user");

// Get VIP service
memberService = context.getBean("memberService");
vipLevel = memberService.getVipLevel(user.userId);

if (vipLevel != null && vipLevel >= 1) {
    context.setData("vipLevel", vipLevel);
    log.info("VIP member detected: level=" + vipLevel);
    return true;  // Execute THEN branch
} else {
    log.info("Regular user: userId=" + user.userId);
    return false; // Execute ELSE branch
}
```

**QLExpress Script: isVipLevel3Plus**:
```javascript
vipLevel = context.getData("vipLevel");
return vipLevel >= 3;  // true for level 3+, false otherwise
```

---

## Pattern 4: Switch Routing (SWITCH)

### Overview

The SWITCH pattern routes execution to one of multiple nodes based on a router's return value. The router node returns a String node ID, which determines the execution path.

### Detailed Execution Flow

```
Execute routerNode
  ↓
  Returns "level1Approval" → Execute level1Approval → Continue
  Returns "level2Approval" → Execute level2Approval → Continue
  Returns "ceoApproval"    → Execute ceoApproval    → Continue
```

### Best Practices

**When to Use**:
- 3+ distinct execution paths
- Multi-tier workflows (approval levels)
- Strategy pattern (different algorithms based on input)

**When to Avoid**:
- Binary decisions (use IF instead)
- Boolean conditions (use IF, not SWITCH)

### Advanced Example: Multi-Level Approval Workflow

**Business Requirements**:
- Amount < $10,000: Auto-approve
- Amount $10,000-$100,000: Manager approval
- Amount $100,000-$1M: Director approval
- Amount $1M+: CEO approval

**EL Expression**:
```javascript
THEN(
  submitExpenseRequest,
  validateBudget,
  SWITCH(determineApprovalLevel).to(
    autoApprove,
    managerApproval,
    directorApproval,
    ceoApproval
  ),
  notifyRequester
)
```

**QLExpress Script: determineApprovalLevel**:
```javascript
request = context.getData("expenseRequest");
amount = request.amount;

// Determine approval level
if (amount < 10000) {
    log.info("Auto-approval: amount=" + amount);
    return "autoApprove";
} else if (amount < 100000) {
    log.info("Manager approval required: amount=" + amount);
    return "managerApproval";
} else if (amount < 1000000) {
    log.info("Director approval required: amount=" + amount);
    return "directorApproval";
} else {
    log.info("CEO approval required: amount=" + amount);
    return "ceoApproval";
}
```

**QLExpress Script: managerApproval**:
```javascript
request = context.getData("expenseRequest");
approvalService = context.getBean("approvalService");

// Create approval task
approvalTaskId = approvalService.createApprovalTask(
    request.requestId,
    "MANAGER",
    request.managerId
);

// Wait for approval (async or polling)
context.setData("approvalTaskId", approvalTaskId);
context.setData("approvalLevel", "MANAGER");

log.info("Manager approval task created: " + approvalTaskId);
return true;
```

---

## Pattern 5: Loop Iteration (FOR)

### Overview

The FOR pattern iterates over a collection or continues until a break condition is met. The iterator node returns the next item, and the break node returns `true` to stop.

### Detailed Execution Flow

```
Execute iteratorNode → Get next item
  ↓
  Item exists? → Execute DO block → Check BREAK condition
  ↓               ↑                  ↓
  No more items ←─┘                  false → Continue loop
  ↓                                  ↓
  Continue                           true → Exit loop
```

### Best Practices

**When to Use**:
- Batch processing (process multiple records)
- Retry logic (retry until success or max attempts)
- Polling (check status until complete)

**When to Avoid**:
- Very large datasets (1000+ items) - use database pagination
- CPU-intensive operations - consider async processing

### Advanced Example: Batch Invoice Processing

**Business Requirements**:
- Process pending invoices in queue
- For each invoice: validate, calculate tax, generate PDF
- Continue until queue is empty
- Handle errors without stopping batch

**EL Expression**:
```javascript
FOR(getNextInvoice).DO(
  THEN(
    validateInvoice,
    calculateTax,
    generatePDF,
    updateInvoiceStatus
  )
).BREAK(noMoreInvoices)
```

**QLExpress Script: getNextInvoice**:
```javascript
// Get invoice queue from context (initialized in Service layer)
invoiceQueue = context.getData("invoiceQueue");

if (invoiceQueue == null || invoiceQueue.isEmpty()) {
    log.info("No more invoices to process");
    return null;  // Triggers BREAK condition
}

// Get next invoice
nextInvoice = invoiceQueue.poll();
context.setData("currentInvoice", nextInvoice);

log.info("Processing invoice: " + nextInvoice.invoiceNo);
return nextInvoice;
```

**QLExpress Script: noMoreInvoices**:
```javascript
invoiceQueue = context.getData("invoiceQueue");
return invoiceQueue == null || invoiceQueue.isEmpty();
```

**SmartAdmin Service Integration**:
```java
public ResponseDTO<BatchResultVO> processPendingInvoices() {
    // Load pending invoices
    List<Invoice> pendingInvoices = invoiceDao.selectPending();
    Queue<Invoice> invoiceQueue = new LinkedList<>(pendingInvoices);

    // Execute batch chain
    LiteFlowExecutionForm form = new LiteFlowExecutionForm();
    form.setChainCode("batch-invoice-chain");
    form.setInputParams(Map.of(
        "invoiceQueue", invoiceQueue,
        "processedCount", new AtomicInteger(0),
        "failedCount", new AtomicInteger(0)
    ));

    ResponseDTO<LiteFlowExecutionResultVO> result =
        liteFlowExecutionService.execute(form);

    // Extract results
    Map<String, Object> output = result.getData().getOutputResult();
    int processed = ((AtomicInteger) output.get("processedCount")).get();
    int failed = ((AtomicInteger) output.get("failedCount")).get();

    BatchResultVO resultVO = new BatchResultVO();
    resultVO.setProcessed(processed);
    resultVO.setFailed(failed);

    return ResponseDTO.ok(resultVO);
}
```

---

## Pattern 6: Nested Chains (Sub-flows)

### Overview

Nested chains allow reusable sub-workflows to be invoked from multiple parent chains, promoting code reuse and modular design.

### Best Practices

**When to Use**:
- Common operations used in multiple workflows
- Complex workflows requiring decomposition
- Audit trails and cleanup logic

**Example: Reusable Audit + Cleanup**:

**Main Chain**:
```javascript
THEN(
  validateUser,
  processData,
  auditCleanupChain  // Reusable sub-chain
)
```

**Sub-chain Definition**:
```javascript
THEN(auditLog, cleanup, notifyAdmin)
```

---

## Pattern 7: Exception Handling (CATCH)

### Overview

The CATCH pattern provides graceful error recovery by executing a fallback node when exceptions occur in the main chain.

### Best Practices

**When to Use**:
- Critical operations requiring rollback
- Transactional workflows
- Graceful failure handling

**Example: Payment with Rollback**:

**EL Expression**:
```javascript
THEN(
  reserveInventory,
  processPayment
).CATCH(handlePaymentFailure)
```

**QLExpress Script: handlePaymentFailure**:
```javascript
exception = context.getData("exception");
log.error("Payment failed: " + exception.getMessage());

// Rollback inventory reservation
order = context.getData("order");
inventoryService = context.getBean("inventoryService");
inventoryService.releaseReservation(order.productId, order.quantity);

// Notify user
notificationService = context.getBean("notificationService");
notificationService.sendPaymentFailedEmail(order.userId, exception.getMessage());

return false;  // Mark chain as failed
```

---

**See Also**:
- [Quick Reference](quick-reference.md) - Command reference and decision matrix
- [Examples](examples.md) - Real SmartAdmin workflow cases
- [Best Practices](best-practices.md) - Performance optimization
