---
name: vavr-refactoring-assistant
description: Refactor Service layer to use Vavr Option/Try/Either patterns
priority: P0
category: backend
---

# Vavr Refactoring Assistant

Refactor Service layer methods to use Vavr functional patterns (Option, Try, Either) instead of java.util.Optional and checked exceptions.

## Usage

```
User: "Refactor EmployeeService to use Vavr patterns"
AI: [Analyzes current code and applies Vavr refactoring]
```

## When to Use

- Service uses java.util.Optional (ArchUnit violation)
- Want to adopt functional error handling
- Refactoring to Vavr patterns
- Need better null handling in Service layer

## Vavr Patterns

| Pattern | Java Equivalent | Use Case |
|---------|-----------------|----------|
| `Option<T>` | `Optional<T>` | Nullable results |
| `Try<T>` | try-catch | Operations that may fail |
| `Either<L, R>` | Exception or result | Typed error handling |

## Workflow

1. Identify methods using java.util.Optional
2. Replace with io.vavr.control.Option
3. Update method chains (.map, .flatMap, .getOrElse)
4. Add proper null handling
5. Run ArchUnit tests to verify

## Related Rules

- [P01-vavr-fundamentals.md](../../../rules/technology/functional/P01-vavr-fundamentals.md)
- [P02-vavr-advanced.md](../../../rules/technology/functional/P02-vavr-advanced.md)
- [P03-vavr-mybatis-integration.md](../../../rules/technology/functional/P03-vavr-mybatis-integration.md)

## Example Session

**User:** ArchUnit fails: Service uses java.util.Optional

**AI Agent Actions:**
1. Find all java.util.Optional usages in Service
2. Replace imports: `java.util.Optional` → `io.vavr.control.Option`
3. Update method signatures
4. Refactor method chains
5. Run `./gradlew test --tests ArchitectureTest`
