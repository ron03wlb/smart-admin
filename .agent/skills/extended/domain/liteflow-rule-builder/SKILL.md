---
name: liteflow-rule-builder
description: Generate LiteFlow rule DSL (EL expressions, QLExpress scripts)
priority: P1
category: domain
---

# LiteFlow Rule Builder

Generate LiteFlow rule DSL (EL expressions and QLExpress scripts) for business workflows.

## When to Use

- Building business workflow rules
- Need approval flow or validation chains
- Creating conditional business logic
- Implementing complex rule engines

## LiteFlow Concepts

- **Chain**: Workflow definition
- **Node**: Processing unit
- **EL Expression**: Orchestration syntax (THEN, WHEN, SWITCH)
- **QLExpress**: Embedded scripting

## Example Output

```xml
<chain name="orderValidation">
    THEN(
        validateUser,
        WHEN(checkInventory, checkCredit).any(),
        IF(isVip, applyDiscount, THEN(checkLimit, applyNormalPrice))
    )
</chain>
```

## Workflow

1. Analyze business requirements
2. Design node components
3. Generate EL expressions
4. Create QLExpress scripts if needed
5. Generate SQL INSERT statements
6. Output integration code
