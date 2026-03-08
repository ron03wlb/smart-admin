---
name: liteflow-rule-builder
description: [P1 - Extended] Generate LiteFlow rule DSL (EL expressions and QLExpress scripts) from natural language descriptions for complex business workflows. Use when implementing business rule orchestration, approval workflows, validation chains, or migrating from Evrete rules. Triggers when (1) User requests "create LiteFlow chain/rule", (2) User describes a multi-step business workflow, (3) User mentions flow orchestration, conditional logic, or parallel execution, (4) User wants to implement approval/validation flows, (5) Migrating from Evrete to LiteFlow.
---

# LiteFlow Rule Builder

Generate complete LiteFlow flow orchestration rules from natural language descriptions -- automatically creates EL expression chains, QLExpress scripts, database configurations, and integration code following SmartAdmin patterns.

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

**Problem:** Complex business workflows are hard to implement and maintain -- sequential steps mixed with conditional branching, parallel operations, dynamic rule changes requiring redeployment.

**Solution:** LiteFlow provides declarative flow orchestration with EL expressions, database-backed hot-reload, QLExpress scripts for dynamic logic, and execution logging/monitoring.

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "LiteFlow" - LiteFlow rule or chain generation
- "create LiteFlow chain/rule" - Explicit creation request
- "flow orchestration" - Workflow orchestration design
- "business workflow" - Business process workflow

**Secondary Keywords** (Medium confidence):
- "approval flow" - Multi-level approval workflows
- "validation chain" - Sequential validation logic
- "conditional logic" - IF/SWITCH/CASE patterns
- "parallel execution" / "parallel processing" - WHEN pattern
- "migrate from Evrete" - Evrete to LiteFlow migration
- "rule engine" - Business rule engine implementation

**Note**: This skill can also be manually invoked via `/liteflow-rule-builder` command.

---

## Core LiteFlow Patterns

### Pattern 1: Sequential Execution (THEN)

Execute nodes in strict order (A -> B -> C). Use when operations are dependent or require strict ordering.

**EL Expression:** `THEN(nodeA, nodeB, nodeC)`

See [dsl-examples.md](examples/dsl-examples.md#pattern-1-sequential-execution-then) for complete example with order processing chain, QLExpress script, database insert, and SmartAdmin Service integration.

### Pattern 2: Parallel Execution (WHEN)

Execute nodes concurrently (all must complete). Total time = max(nodeA, nodeB, nodeC) instead of sum.

**EL Expression:** `WHEN(nodeA, nodeB, nodeC)`

See [dsl-examples.md](examples/dsl-examples.md#pattern-2-parallel-execution-when) for multi-channel notification example with QLExpress scripts.

### Pattern 3: Conditional Logic (IF)

Branch execution based on condition. Condition script returns boolean: true = THEN branch, false = ELSE branch.

**EL Expression:** `IF(conditionNode, thenNode, elseNode)`

See [dsl-examples.md](examples/dsl-examples.md#pattern-3-conditional-logic-if) for VIP discount logic with nested IF example.

### Pattern 4: Switch Routing (SWITCH)

Route to different nodes based on value. Cleaner than multiple IF for 3+ branches.

**EL Expression:** `SWITCH(routerNode).to(caseA, caseB, caseC)`

**IMPORTANT**: Router script must return node ID as String, NOT integer index.

See [dsl-examples.md](examples/dsl-examples.md#pattern-4-switch-routing-switch) for multi-level approval example.

### Pattern 5: Loop Iteration (FOR)

Execute node for each item in collection. Large loops (1000+ items) should use async execution.

**EL Expression:** `FOR(iteratorNode).DO(processingNode).BREAK(breakConditionNode)`

See [dsl-examples.md](examples/dsl-examples.md#pattern-5-loop-iteration-for) for batch order processing example.

### Pattern 6: Nested Chains (Sub-flows)

Reusable sub-workflows referenced by chain code. Separate chain definitions in database.

See [dsl-examples.md](examples/dsl-examples.md#pattern-6-nested-chains-sub-flows) for main chain + sub-chain example.

### Pattern 7: Exception Handling (CATCH)

**EL Expression:** `THEN(nodeA, nodeB).CATCH(errorHandler)`

See [dsl-examples.md](examples/dsl-examples.md#pattern-7-exception-handling-catch) for payment error handling with rollback and notification.

---

## SmartAdmin Integration

### Database Storage Pattern

1. Insert chain definition into `t_liteflow_chain`
2. Insert script nodes into `t_liteflow_script`
3. Reload flow engine: `POST /liteflow/chain/reloadAll`

### Service Layer Integration

Standard pattern: Load data -> Create `LiteFlowExecutionForm` -> Call `liteFlowExecutionService.execute()` -> Extract result -> Update database.

See [dsl-examples.md](examples/dsl-examples.md#smartadmin-service-layer-integration) for complete EmployeeService approval example.

### QLExpress Script Best Practices

- **Context data**: `context.getData("key")` / `context.setData("key", value)`
- **Spring beans**: `context.getBean("serviceName")`
- **Logging**: `log.info()`, `log.warn()`, `log.error()`
- **Conditions**: Return `true`/`false` for IF, return node ID String for SWITCH

See [dsl-examples.md](examples/dsl-examples.md#qlexpress-script-best-practices) for detailed examples.

---

## Common Mistakes and Fixes

| Mistake | Problem | Fix |
|---------|---------|-----|
| No return value | Condition script without `return` | Always `return true/false` for IF |
| Not setting output data | Next node can't access calculated value | Use `context.setData("key", value)` |
| WHEN for dependent ops | Both run in parallel but B needs A's result | Use THEN for sequential dependencies |
| Missing null check | NullPointerException on `context.getData()` | Always check `if (data == null)` |
| Wrong SWITCH return | Return integer index instead of node ID | Return `"nodeName"` as String |

---

## Pattern Complexity Table

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
4. Test functional equivalence (same input -> same output)

See [docs/plans/liteflow/migration-guide.md](../../../../docs/plans/liteflow/migration-guide.md) for complete migration guide.

---

## Testing Workflow

### RED Phase: Baseline Test

**Scenario:** Employee approval workflow -- validate data, route to approval tier by salary, send notification.

See [dsl-examples.md](examples/dsl-examples.md#red-phase-baseline-test) for expected chain, scripts, and verification steps.

### GREEN Phase: Verification
1. Insert chain and scripts into database
2. Reload: `POST /liteflow/chain/reloadAll`
3. Execute: `POST /liteflow/execution/execute`
4. Verify log: `GET /liteflow/execution/queryLog`
5. Check output: Correct approval tier selected

### REFACTOR Phase: Optimize
- Extract common validation to reusable sub-chain
- Add exception handling for edge cases
- Add execution metrics monitoring

---

## Related Rules

### Mandatory Requirements

- **[Architecture Rules](../../../.agent/rules/foundation/F04-architecture-rules.md)** - LiteFlow chain calls in Service layer, complex execution (multi-table transactions) in Manager layer, constructor injection
- **[Dependency Injection](../../../.agent/rules/foundation/F04-architecture-rules.md)** - LiteFlow nodes (@LiteflowComponent) use constructor injection (no @Autowired field injection)

### Reference Guidelines

- **[Exception Handling](../../../.agent/rules/technology/patterns/04-exception-logging.md)** - CATCH node exception handling, execution failure rollback
- **[Naming Conventions](../../../.agent/rules/foundation/F01-naming-conventions.md)** - Node: ApprovalValidationNode; Chain: approvalChain

### Related Skills

- **[scheduled-task-manager](../../productivity/devops/scheduled-task-manager/SKILL.md)** - XXL-Job scheduled tasks triggering LiteFlow chains
- **[smartadmin-crud-generator](../../foundation/full-stack/smartadmin-crud-generator/SKILL.md)** - Controller calling LiteFlow Service for business chains

---

## Version Information

**Skill Version:** 1.0.0
**LiteFlow Version:** 2.15.3
**SmartAdmin Compatibility:** v4.0.0+
**Last Updated:** 2026-02-06

**Related Documentation:**
- [LiteFlow Module README](../../../../docs/plans/liteflow/README.md)
- [Architecture Documentation](../../../../docs/plans/liteflow/architecture.md)
- [Migration Guide](../../../../docs/plans/liteflow/migration-guide.md)
- [Database Schema](../../../../docs/plans/liteflow/database-schema.md)

**Maintainer:** SmartAdmin AI Team
