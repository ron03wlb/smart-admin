---
name: spring-pattern-checker
description: Validate Spring patterns (@Transactional placement, DI, layering)
priority: P1
category: quality
---

# Spring Pattern Checker

Validate Spring patterns including @Transactional placement, dependency injection, and layered architecture compliance.

## When to Use

- Validating @Transactional usage
- Checking dependency injection patterns
- Ensuring layered architecture compliance
- Pre-commit validation

## Checks Performed

1. **@Transactional Placement**
   - Only in Manager layer (not Service/Controller)
   - Must include `rollbackFor = Throwable.class`

2. **Dependency Injection**
   - Constructor injection with @RequiredArgsConstructor
   - No @Autowired field injection

3. **Layer Dependencies**
   - Controller → Service only
   - Service → Manager/Dao
   - Manager → Dao only

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)
- [F03-manager-layer.md](../../../rules/foundation/F03-manager-layer.md)

## Example Session

**User:** Check Spring patterns in EmployeeService

**AI Actions:**
1. Scan for @Transactional annotations
2. Check DI patterns
3. Validate layer dependencies
4. Report violations with fixes
