# LiteFlow Rule Builder Skill

Generate complete LiteFlow flow orchestration rules from natural language descriptions.

## Overview

**Skill Name:** liteflow-rule-builder
**Category:** Business Rule Orchestration
**Complexity:** P1 (High Priority)
**Version:** 1.0.0

## What This Skill Does

Transforms natural language workflow descriptions into complete LiteFlow implementations:
- EL expression chains (THEN, IF, WHEN, SWITCH, FOR)
- QLExpress scripts for business logic
- Database SQL insert statements
- Service layer integration code
- Test scenarios and execution examples

## When to Use

**Trigger Scenarios:**
1. User requests "create LiteFlow chain/rule"
2. User describes multi-step business workflow
3. User mentions flow orchestration, conditional logic, or parallel execution
4. User wants approval/validation flows
5. Migrating from Evrete to LiteFlow

**Example User Requests:**
```
"Create an order validation workflow with inventory check and VIP discounts"
"Generate employee approval flow with multi-tier routing"
"Build a payment processing chain with fraud detection"
"Migrate this Evrete rule to LiteFlow"
```

## Core Capabilities

### 1. Flow Patterns
- **Sequential Execution (THEN)**: A → B → C
- **Parallel Execution (WHEN)**: A + B + C (concurrent)
- **Conditional Logic (IF)**: If condition then A else B
- **Switch Routing (SWITCH)**: Route to different nodes based on value
- **Loop Iteration (FOR)**: Batch processing with break conditions
- **Nested Chains**: Reusable sub-workflows
- **Exception Handling (CATCH)**: Error recovery

### 2. QLExpress Script Generation
- Context data access and manipulation
- Spring bean integration
- Validation logic
- Business rule implementation
- Logging and monitoring

### 3. SmartAdmin Integration
- Database schema compliance (t_liteflow_chain, t_liteflow_script)
- Service layer patterns
- ResponseDTO usage
- Permission control
- Hot-reload integration

## Quick Start Example

**User Input:**
```
"Create an order processing workflow:
1. Validate order data
2. Check inventory
3. If VIP member, apply 20% discount
4. Create order
5. Send email and SMS notifications in parallel"
```

**Generated Output:**

**EL Chain:**
```javascript
THEN(
  validateOrder,
  checkInventory,
  IF(isVipMember, applyVipDiscount, calculateRegularPrice),
  createOrder,
  WHEN(sendEmail, sendSMS)
)
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_chain (chain_name, chain_code, chain_type, chain_data) VALUES
('订单处理流程', 'order-process-chain', 2,
 'THEN(validateOrder, checkInventory, IF(isVipMember, applyVipDiscount, calculateRegularPrice), createOrder, WHEN(sendEmail, sendSMS))');
```

**QLExpress Scripts:** (11 scripts with complete business logic)

**Integration Code:**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final LiteFlowExecutionService liteFlowExecutionService;

    public ResponseDTO<OrderVO> processOrder(OrderAddForm form) {
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("order-process-chain");
        executionForm.setInputParams(Map.of("order", form));

        return liteFlowExecutionService.execute(executionForm);
    }
}
```

## Files in This Skill

| File | Purpose |
|------|---------|
| `SKILL.md` | Complete skill documentation with 7 core patterns |
| `BASELINE-TEST.md` | Validation test scenario (Employee Approval Workflow) |
| `README.md` | This file - Quick reference and navigation |

## Pattern Complexity Estimates

| Pattern | Complexity | Time Estimate |
|---------|-----------|---------------|
| THEN (Sequential) | Low | 5-10 min |
| WHEN (Parallel) | Medium | 10-15 min |
| IF (Conditional) | Medium | 10-15 min |
| SWITCH (Routing) | High | 15-20 min |
| FOR (Loop) | High | 20-30 min |
| Nested Chains | Medium | 15-20 min |
| Exception Handling | Medium | 10-15 min |

**Total for Complex Workflow:** 30-60 minutes (including testing)

## Testing

### Baseline Test Scenario
**Employee Approval Workflow** with:
- Multi-field validation
- Three-tier approval routing (HR/Manager/CEO)
- Conditional branching (approved/rejected)
- Parallel operations (email + status update)
- Audit logging

**Test Coverage:**
- 1 chain definition
- 11 script nodes
- 4 test cases (3 approval tiers + 1 validation failure)
- Expected execution time < 100ms

See `BASELINE-TEST.md` for complete test specification.

## SmartAdmin Integration Points

### Database Tables Used
- `t_liteflow_chain` - Chain definitions
- `t_liteflow_script` - Script nodes
- `t_liteflow_execution_log` - Execution history
- `t_liteflow_execution_metrics` - Performance metrics

### Service Dependencies
- `LiteFlowExecutionService` - Flow execution
- Spring bean access from QLExpress scripts
- ResponseDTO for API responses
- Hot-reload support

### Architecture Compliance
- ✅ Layered architecture (Service → LiteFlowExecutionService)
- ✅ Constructor injection (@RequiredArgsConstructor)
- ✅ ResponseDTO pattern
- ✅ Database-backed configuration
- ✅ Audit logging

## Common Mistakes to Avoid

1. **No return statement in condition scripts** - IF/SWITCH require boolean/String return
2. **Using WHEN for dependent operations** - Use THEN for sequential dependencies
3. **Not setting context data** - Next nodes can't access previous results
4. **Missing null checks** - Always validate context.getData() results
5. **Wrong SWITCH return type** - Return node ID (String), not index (int)

See `SKILL.md` section "Common Mistakes and Fixes" for detailed examples.

## Migration from Evrete

**Conversion Table:**

| Evrete | LiteFlow |
|--------|----------|
| @Rule(salience=100) | THEN(a, b, c) |
| @Where("condition") | QLExpress if statement |
| Parallel rules | WHEN(a, b, c) |
| Rule chaining | context.setData() |

**Migration Steps:**
1. Identify rule dependencies
2. Map to LiteFlow patterns
3. Convert @Where to QLExpress
4. Test functional equivalence

See `docs/plans/liteflow/migration-guide.md` for complete migration guide.

## Related Documentation

### SmartAdmin LiteFlow Module
- [LiteFlow Module README](../../../docs/plans/liteflow/README.md)
- [Architecture Documentation](../../../docs/plans/liteflow/architecture.md)
- [Database Schema](../../../docs/plans/liteflow/database-schema.md)
- [API Specification](../../../docs/plans/liteflow/api-specification.md)
- [Migration Guide](../../../docs/plans/liteflow/migration-guide.md)

### Architecture Decision
- [ADR-011: Evrete → LiteFlow Migration](../../../docs/iGame/architecture-decisions/011-liteflow-migration.md)

### Technical Specifications
- [Bonus Engine (P1-12)](../../../docs/iGame/technical-specs/P1-important/12-bonus-engine.md) - Uses LiteFlow
- [VIP System (P1-11)](../../../docs/iGame/technical-specs/P1-important/11-vip-system-design.md) - Uses LiteFlow
- [Risk Control (P1-06)](../../../docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md) - Uses LiteFlow

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-25 | Initial release with 7 core patterns |

## Maintainer

**Team:** SmartAdmin AI Team
**Contact:** See CLAUDE.md for AI assistant guidelines
**LiteFlow Version:** 2.15.3
**SmartAdmin Version:** v4.0.0+

---

**Quick Links:**
- [SKILL.md](SKILL.md) - Full skill documentation
- [BASELINE-TEST.md](BASELINE-TEST.md) - Validation test scenario
- [LiteFlow Official Docs](https://liteflow.cc/)
