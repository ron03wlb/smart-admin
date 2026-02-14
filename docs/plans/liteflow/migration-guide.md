# Migration Guide: Evrete → LiteFlow

## Overview

This guide provides a step-by-step approach to migrating from Evrete rule engine to LiteFlow flow orchestration in SmartAdmin. The migration supports a gradual, risk-managed transition with parallel deployment and comprehensive testing.

## Decision Matrix

### When to Use LiteFlow (vs Evrete)

| Use Case | Evrete | LiteFlow | Recommendation |
|----------|--------|----------|----------------|
| **Complex multi-step workflows** | ❌ Not ideal | ✅ Excellent | Use LiteFlow |
| **Fact-based rule inference** | ✅ Excellent | ⚠️ Possible | Use Evrete |
| **Hot-reload from database** | ❌ Custom impl | ✅ Built-in | Use LiteFlow |
| **Visual flow design** | ❌ No support | ✅ Designer plugin | Use LiteFlow |
| **Script-based rules** | ❌ Limited | ✅ Multiple engines | Use LiteFlow |
| **Pure rule conflict resolution** | ✅ Rete algorithm | ❌ Manual | Use Evrete |
| **SmartAdmin integration** | ⚠️ Custom | ✅ Native support | Use LiteFlow |

## Evrete vs LiteFlow Comparison

### Architecture Differences

| Aspect | Evrete | LiteFlow |
|--------|--------|----------|
| **Type** | Pure rule engine (Rete algorithm) | Flow orchestration + conditional execution |
| **Paradigm** | Forward/backward chaining | Directed acyclic graph (DAG) |
| **Configuration** | Java DSL, annotations | EL expressions (XML/JSON/YAML) |
| **Execution** | Fact insertion → rule matching → action | Chain execution → node execution |
| **State Management** | Working memory | Context-based |
| **Hot Reload** | Requires custom implementation | Built-in support |
| **Database Storage** | No native support | Custom data source (this module) |
| **Script Support** | Limited | QLExpress, Groovy, JS, Python |
| **Community** | Smaller | Dromara foundation (active) |

### Code Pattern Comparison

**Evrete Pattern**:
```java
// Define rules with annotations
@Rule(value = "check-inventory", salience = 100)
@Where("$order.quantity > $inventory.stock")
public void checkInventory(@Fact("$order") Order order,
                           @Fact("$inventory") Inventory inventory) {
    throw new BusinessException("Insufficient stock");
}

// Execute rules
Knowledge knowledge = knowledgeService.newKnowledge();
knowledge.insert(order);
knowledge.insert(inventory);
ActivationMode mode = knowledge.createActivationMode();
mode.fire();
```

**LiteFlow Pattern**:
```java
// Define chain (EL expression stored in DB)
THEN(validateOrder, checkInventory, createOrder, sendNotification)

// Define script node (QLExpress stored in DB)
if (order.quantity > inventory.stock) {
    throw new Exception("Insufficient stock");
}
return true;

// Execute flow
LiteFlowExecutionForm form = new LiteFlowExecutionForm();
form.setChainCode("order-process-chain");
form.setInputParams(Map.of("order", order, "inventory", inventory));
liteFlowExecutionService.execute(form);
```

---

## Migration Strategy

### Phase 1: Parallel Deployment (Week 1-2)

**Goal**: Deploy LiteFlow alongside Evrete without disrupting existing functionality.

**Steps**:

1. **Install LiteFlow Module**
   ```bash
   # 1. Add dependencies to libs.versions.toml
   # 2. Create sa-base/support/liteflow module
   # 3. Run database migration
   psql -U postgres -d smartadmin -f V1.x__liteflow.sql
   ```

2. **Enable LiteFlow in Configuration**
   ```yaml
   smart:
     liteflow:
       enabled: true
       database-enabled: true
   ```

3. **Keep Evrete Operational**
   - Do NOT remove Evrete dependencies
   - Existing Evrete rules continue to function
   - Monitor both systems in parallel

4. **Select Pilot Flows**
   - Choose 1-2 low-risk, simple flows
   - Criteria:
     - Low transaction volume
     - Simple rule logic (sequential, no complex conflict resolution)
     - Easy rollback
   - Examples: Email notification flow, simple validation flow

**Deliverables**:
- LiteFlow module deployed and operational
- Pilot flows identified
- Both systems running in parallel

---

### Phase 2: Rule Migration (Week 3-4)

**Goal**: Convert Evrete rules to LiteFlow chains and scripts.

#### Rule Pattern Conversion Table

| Evrete Pattern | LiteFlow Equivalent | Example |
|----------------|---------------------|---------|
| **Sequential rules** (salience ordering) | `THEN(a, b, c)` | `THEN(validate, process, notify)` |
| **Parallel rules** (independent facts) | `WHEN(a, b, c)` | `WHEN(sendEmail, sendSMS, updateDB)` |
| **Conditional rules** (@Where condition) | `IF(condition, then, else)` | `IF(isVip, THEN(applyDiscount), THEN(regularPrice))` |
| **Rule chaining** (fact insertion in action) | `THEN(...).THEN(...)` | Sequential node execution |
| **No-op rules** (guard clauses) | Boolean script nodes | Return true/false in QLExpress |

#### Step-by-Step Conversion

**Step 1: Analyze Evrete Rule Dependencies**
```java
// Identify rule dependencies
// Example: Rule A inserts fact used by Rule B → Sequential execution needed
```

**Step 2: Create LiteFlow Chain**
```sql
-- Create chain in database
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('Order Validation', 'order-validation-chain', 1,
 'THEN(checkOrderValidity, checkInventory, checkPayment)');
```

**Step 3: Convert Rule Logic to Scripts**

**Evrete Rule**:
```java
@Rule("validate-order")
@Where("$order.amount > 0 && $order.productId != null")
public void validateOrder(@Fact("$order") Order order) {
    if (order.userId == null) {
        throw new BusinessException("User ID required");
    }
}
```

**LiteFlow Script (QLExpress)**:
```sql
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('Validate Order', 'checkOrderValidity', 'qlexpress',
'if (order.amount <= 0) {
    throw new Exception("Invalid amount");
}
if (order.productId == null) {
    throw new Exception("Product ID required");
}
if (order.userId == null) {
    throw new Exception("User ID required");
}
return true;');
```

**Step 4: Test Conversion**
```bash
# 1. Reload LiteFlow engine
POST /liteflow/chain/reloadAll

# 2. Execute test
POST /liteflow/execution/execute
{
  "chainCode": "order-validation-chain",
  "inputParams": {
    "order": { "amount": 100, "productId": "PROD-001", "userId": 123 }
  }
}

# 3. Compare results with Evrete execution
# 4. Verify execution logs
GET /liteflow/execution/queryLog
```

---

### Phase 3: Code Migration (Week 5-6)

**Goal**: Update service layer to use LiteFlow instead of Evrete.

#### Service Layer Refactoring

**Before (Evrete)**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KnowledgeService evreteKnowledgeService;
    private final OrderMapper orderMapper;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Prepare facts
        Order order = SmartBeanUtil.copy(form, Order.class);
        Inventory inventory = inventoryMapper.selectByProductId(form.getProductId());

        // Execute Evrete rules
        Knowledge knowledge = evreteKnowledgeService.newKnowledge(
            "JAVA-CLASS", OrderValidationRules.class
        );

        try {
            knowledge.insert(order);
            knowledge.insert(inventory);
            ActivationMode mode = knowledge.createActivationMode();
            mode.fire();
        } catch (Exception e) {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, e.getMessage());
        }

        // Business logic
        orderMapper.insert(order);
        return ResponseDTO.ok(SmartBeanUtil.copy(order, OrderVO.class));
    }
}
```

**After (LiteFlow)**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final OrderMapper orderMapper;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Execute LiteFlow chain
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-process-chain");
        executionForm.setInputParams(Map.of(
            "orderForm", form,
            "productId", form.getProductId()
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (!result.getOk()) {
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }

        // Extract result
        LiteFlowExecutionResultVO executionResult = result.getData();
        Order order = (Order) executionResult.getOutputResult().get("order");

        return ResponseDTO.ok(SmartBeanUtil.copy(order, OrderVO.class));
    }
}
```

#### Manager Layer Pattern

**LiteFlow Integration Pattern**:
```java
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final OrderMapper orderMapper;
    private final InventoryMapper inventoryMapper;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<OrderVO> processOrder(OrderAddForm form) {
        // Step 1: Execute validation flow
        ResponseDTO<LiteFlowExecutionResultVO> validationResult =
            executeFlow("order-validation-chain", Map.of("orderForm", form));

        if (!validationResult.getOk()) {
            return ResponseDTO.error(UserErrorCode.VALIDATION_ERROR,
                validationResult.getMsg());
        }

        // Step 2: Execute business logic flow
        ResponseDTO<LiteFlowExecutionResultVO> processResult =
            executeFlow("order-process-chain", Map.of("orderForm", form));

        if (!processResult.getOk()) {
            throw new BusinessException(UserErrorCode.BUSINESS_ERROR,
                processResult.getMsg());
        }

        // Step 3: Persist to database
        Order order = extractOrder(processResult);
        orderMapper.insert(order);

        return ResponseDTO.ok(SmartBeanUtil.copy(order, OrderVO.class));
    }

    private ResponseDTO<LiteFlowExecutionResultVO> executeFlow(
        String chainCode, Map<String, Object> params) {

        LiteFlowExecutionForm form = new LiteFlowExecutionForm();
        form.setChainCode(chainCode);
        form.setInputParams(params);
        return liteFlowExecutionService.execute(form);
    }

    private Order extractOrder(ResponseDTO<LiteFlowExecutionResultVO> result) {
        return (Order) result.getData().getOutputResult().get("order");
    }
}
```

---

### Phase 4: Testing & Validation (Week 7)

**Goal**: Comprehensive testing to ensure functional equivalence.

#### Test Strategy

**1. Unit Testing**
```java
@SpringBootTest
class OrderServiceLiteFlowTest {

    @Autowired
    private OrderService orderService;

    @Test
    void testCreateOrder_Success() {
        // Arrange
        OrderAddForm form = new OrderAddForm();
        form.setProductId("PROD-001");
        form.setAmount(new BigDecimal("99.99"));
        form.setUserId(123L);

        // Act
        ResponseDTO<OrderVO> response = orderService.createOrder(form);

        // Assert
        Assertions.assertTrue(response.getOk());
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals("created", response.getData().getStatus());
    }

    @Test
    void testCreateOrder_ValidationFailed() {
        // Arrange
        OrderAddForm form = new OrderAddForm();
        form.setAmount(new BigDecimal("-10")); // Invalid amount

        // Act
        ResponseDTO<OrderVO> response = orderService.createOrder(form);

        // Assert
        Assertions.assertFalse(response.getOk());
        Assertions.assertTrue(response.getMsg().contains("Invalid amount"));
    }
}
```

**2. Integration Testing**
```java
@SpringBootTest
@Transactional
class OrderFlowIntegrationTest {

    @Autowired
    private LiteFlowExecutionService liteFlowExecutionService;

    @Test
    void testOrderProcessFlow_EndToEnd() {
        // Execute full order process flow
        LiteFlowExecutionForm form = new LiteFlowExecutionForm();
        form.setChainCode("order-process-chain");
        form.setInputParams(Map.of(
            "orderId", 12345,
            "userId", 678,
            "amount", 99.99
        ));

        ResponseDTO<LiteFlowExecutionResultVO> response =
            liteFlowExecutionService.execute(form);

        // Verify execution
        Assertions.assertTrue(response.getOk());
        Assertions.assertEquals(1, response.getData().getExecutionStatus());

        // Verify execution log created
        // Verify metrics updated
    }
}
```

**3. Performance Testing**
```bash
# JMeter test plan
# - 100 concurrent users
# - 1000 executions total
# - Compare: Evrete vs LiteFlow execution time

# Expected results:
# - Throughput: > 500 executions/sec
# - P95 latency: < 100ms
# - Error rate: < 0.1%
```

**4. Comparison Testing**
```java
// Run same input through both Evrete and LiteFlow
// Compare outputs for functional equivalence

@Test
void testEvreteVsLiteFlow_FunctionalEquivalence() {
    // Execute with Evrete
    OrderVO evreteResult = executeWithEvrete(testOrder);

    // Execute with LiteFlow
    OrderVO liteFlowResult = executeWithLiteFlow(testOrder);

    // Assert equivalence
    Assertions.assertEquals(evreteResult.getStatus(), liteFlowResult.getStatus());
    Assertions.assertEquals(evreteResult.getAmount(), liteFlowResult.getAmount());
}
```

---

### Phase 5: Gradual Rollout (Week 8-10)

**Goal**: Incrementally migrate production traffic to LiteFlow.

#### Rollout Strategy

**Week 8: 10% Traffic**
- Route 10% of production traffic to LiteFlow
- Monitor for 3-5 days
- Compare metrics: error rates, latency, throughput

**Week 9: 50% Traffic**
- If Week 8 successful, increase to 50%
- Monitor for 5-7 days
- Perform load testing during peak hours

**Week 10: 100% Traffic**
- Full migration to LiteFlow
- Evrete remains as fallback (hot standby)
- Monitor for 7-10 days before decommissioning Evrete

#### Feature Flag Pattern
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final LiteFlowExecutionService liteFlowExecutionService;
    private final EvreteKnowledgeService evreteKnowledgeService;

    @Value("${smart.liteflow.migration.percentage:0}")
    private int liteFlowTrafficPercentage;

    public ResponseDTO<OrderVO> createOrder(OrderAddForm form) {
        // Determine execution engine based on traffic percentage
        boolean useLiteFlow = ThreadLocalRandom.current()
            .nextInt(100) < liteFlowTrafficPercentage;

        if (useLiteFlow) {
            return createOrderWithLiteFlow(form);
        } else {
            return createOrderWithEvrete(form);
        }
    }

    private ResponseDTO<OrderVO> createOrderWithLiteFlow(OrderAddForm form) {
        // LiteFlow implementation
    }

    private ResponseDTO<OrderVO> createOrderWithEvrete(OrderAddForm form) {
        // Evrete implementation (fallback)
    }
}
```

**Configuration**:
```yaml
smart:
  liteflow:
    migration:
      percentage: 10  # Week 8: 10%, Week 9: 50%, Week 10: 100%
```

---

### Phase 6: Evrete Decommissioning (Week 11-12)

**Goal**: Remove Evrete dependencies and finalize migration.

**Steps**:

1. **Verify Complete Migration**
   ```bash
   # Check for remaining Evrete references
   grep -r "evreteKnowledgeService" src/
   grep -r "Knowledge knowledge" src/
   grep -r "@Rule" src/
   ```

2. **Remove Evrete Dependencies**
   ```kotlin
   // build.gradle.kts - Remove Evrete dependency
   // dependencies {
   //     implementation("org.evrete:evrete-core:4.0.3")
   // }
   ```

3. **Archive Evrete Rule Configurations**
   ```bash
   mkdir -p docs/legacy/evrete-rules
   mv src/main/java/rules/* docs/legacy/evrete-rules/
   ```

4. **Update Documentation**
   - Update CLAUDE.md to reference LiteFlow
   - Remove Evrete usage examples
   - Add LiteFlow best practices

5. **Final Cleanup**
   ```bash
   # Remove Evrete service classes
   rm -rf src/main/java/**/evrete/

   # Remove Evrete configuration
   rm -rf src/main/resources/evrete-config.xml
   ```

---

## Rollback Plan

### Emergency Rollback Procedure

If critical issues arise during migration:

**Step 1: Immediate Rollback**
```yaml
# Set traffic percentage to 0
smart:
  liteflow:
    migration:
      percentage: 0  # Routes all traffic back to Evrete
```

**Step 2: Identify Root Cause**
- Check LiteFlow execution logs: `GET /liteflow/execution/queryLog?executionStatus=0`
- Review error messages and stack traces
- Compare with Evrete execution for same inputs

**Step 3: Fix or Disable**
```yaml
# Disable LiteFlow module entirely
smart:
  liteflow:
    enabled: false  # Disables LiteFlow, fallback to Evrete
```

---

## Migration Checklist

### Pre-Migration
- [ ] LiteFlow module installed and configured
- [ ] Database tables created
- [ ] Pilot flows identified
- [ ] Test environment validated

### During Migration
- [ ] Rules converted to LiteFlow chains/scripts
- [ ] Unit tests written and passing
- [ ] Integration tests passing
- [ ] Performance tests meet targets
- [ ] Code refactored (service/manager layers)

### Post-Migration
- [ ] 100% traffic on LiteFlow
- [ ] Evrete dependencies removed
- [ ] Documentation updated
- [ ] Team trained on LiteFlow
- [ ] Monitoring dashboards updated

---

## Troubleshooting

### Common Issues

**Issue 1: Script Syntax Errors**
```
Error: "QLExpress syntax error: unexpected token"
Solution: Validate script syntax using test endpoint
POST /liteflow/script/test
```

**Issue 2: Chain Not Found**
```
Error: "Chain code 'xxx' not found"
Solution:
1. Check chain exists: GET /liteflow/chain/queryPage
2. Reload engine: POST /liteflow/chain/reloadAll
3. Verify cache: Check Redis keys "liteflow:chain:*"
```

**Issue 3: Performance Degradation**
```
Symptom: Execution time > Evrete
Solution:
1. Check cache hit rate (should be > 90%)
2. Review execution logs for bottlenecks
3. Optimize script logic
4. Enable async execution for long-running flows
```

---

## Support Resources

- **LiteFlow Documentation**: https://liteflow.cc/
- **LiteFlow GitHub**: https://github.com/dromara/liteflow
- **SmartAdmin LiteFlow Module**: See [README.md](README.md)
- **API Reference**: See [api-specification.md](api-specification.md)

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Maintainer**: 1024创新实验室
