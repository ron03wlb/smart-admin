---
name: vavr-refactoring-assistant
description: [P0 - Critical] Use when refactoring Service layer methods to use Vavr Option/Try/Either instead of java.util.Optional/checked exceptions, when code review detects Optional usage violations in Service classes, when ArchitectureTest serviceUsesVavrOption fails, or when user mentions "refactor to Vavr", "convert Optional", "use Try", "functional exceptions"
---

# Vavr Refactoring Assistant

Refactor Java code from java.util.Optional/try-catch/null checks to io.vavr Option/Try/Either patterns, enforcing SmartAdmin architectural standards.

## Quick Start

**Most common usage:**
```
User: "Refactor this Service to use Vavr Option"
User: "Convert Optional to Option"
User: "Replace try-catch with Try.of()"
User: "Fix ArchitectureTest serviceUsesVavrOption violation"
```

You will:
1. Identify anti-patterns (Optional, null checks, try-catch)
2. Apply Vavr patterns (Option, Try, Either)
3. Update imports and method signatures
4. Preserve business logic correctness
5. Verify ArchUnit compliance

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "Vavr" - Vavr library usage or refactoring
- "refactor to Vavr" - Convert code to use Vavr patterns
- "Option pattern" / "Try pattern" / "Either pattern" - Vavr type usage
- "serviceUsesVavrOption" - ArchitectureTest violation fix

**Secondary Keywords** (Medium confidence):
- "convert Optional" - From java.util.Optional to io.vavr.control.Option
- "functional exceptions" - Replacing try-catch with Try.of()
- "ArchitectureTest violation" - Service layer Optional usage

**Note**: This skill can also be manually invoked via `/vavr-refactoring-assistant` command.

---

## Prerequisites

**IMPORTANT**: Verify Vavr dependency is in your project:

```kotlin
// smartadmin-common/foundation/core/build.gradle.kts
dependencies {
    api("io.vavr:vavr:0.10.4")  // Required for this skill
}
```

**Verification:**
```bash
./gradlew :smartadmin-common:foundation:core:dependencies --configuration api | grep vavr
```

If missing, add the dependency and run `./gradlew :smartadmin-app:compileJava`.

---

## Critical Rules

**MUST enforce:**
- Service layer returns `Option<T>` NOT `Optional<T>` (ArchUnit violation)
- Use `Try.of()` for exceptions NOT `try-catch`
- No explicit null checks (`if (obj == null)`)
- Controller layer handles Option/Try (NOT Service)
- MyBatis integration: `Option.of(dao.selectById(id))` NOT `dao.selectById(id)`

---

## Refactoring Patterns (8 Patterns)

### Pattern 1: Optional -> Option
Change `Optional.ofNullable(x)` to `Option.of(x)`. Remove `import java.util.Optional`, add `import io.vavr.control.Option`.

### Pattern 2: try-catch -> Try.of()
Change `try { ... } catch (E e) { ... }` to `Try.of(() -> ...).onFailure(e -> log.error(...))`. Controller handles `.getOrElse("default")`.

### Pattern 3: Null checks -> Option chaining
Change `if (obj == null)` to `Option.of(obj).map(...)`. Use `.flatMap()` for nested nullable access.

### Pattern 4: MyBatis null handling
- MyBatis returns Optional: `Option.ofOptional(dao.findById(id))`
- MyBatis returns nullable: `Option.of(dao.selectById(id))`

### Pattern 5: Stream -> Vavr List (Optional)
Change `java.util.stream.collect(Collectors.toList())` to `io.vavr.collection.List.ofAll(...).filter(...).map(...)`.

### Pattern 6: Business validation -> Either
Chain validations with `Either<String, T>.flatMap()`. Controller handles with `.fold(error -> ResponseDTO.error(...), ok -> ResponseDTO.ok(...))`.

### Pattern 7: Multiple try-catch -> Try.flatMap() chain
Chain `Try.of(() -> step1).flatMap(r -> Try.of(() -> step2)).flatMap(...)` instead of nested try-catch.

### Pattern 8: @Transactional with Try return type
`@Transactional + Try<T>` is fully compatible. Try.Failure triggers rollback automatically.

See [vavr-patterns.md](knowledge/vavr-patterns.md) for complete BEFORE/AFTER code examples of all 8 patterns.

---

## Quick Reference Table

| Anti-Pattern | Vavr Pattern | When to Use |
|--------------|-------------|-------------|
| `Optional<T>` | `Option<T>` | Service return types (MANDATORY) |
| `Optional.ofNullable(x)` | `Option.of(x)` | Wrapping nullable values |
| `try-catch` | `Try.of(() -> ...)` | Single exception handling |
| Nested try-catch | `Try.flatMap()` chain | Multiple exception steps |
| `if (x == null)` | `Option.of(x).map(...)` | Null checks |
| Nested null checks | `.flatMap()` chaining | Chained nullable access |
| Multiple validations | `Either<String, T>.flatMap()` | Business rules |
| `@Transactional + throws` | `@Transactional + Try<T>` | Manager layer with exceptions |

---

## Refactoring Checklist

**Phase 1: Identify Anti-Patterns**
- [ ] Search for `java.util.Optional` in Service classes
- [ ] Find `try-catch` blocks in Service methods
- [ ] Locate explicit null checks (`if (obj == null)`)

**Phase 2: Update Imports**
- [ ] Remove `import java.util.Optional;`
- [ ] Add `import io.vavr.control.Option;`
- [ ] Add `import io.vavr.control.Try;` (if exceptions)
- [ ] Add `import io.vavr.control.Either;` (if business validation)

**Phase 3: Refactor Method Signatures**
- [ ] Change `Optional<T>` to `Option<T>`
- [ ] Change `T throws XException` to `Try<T>`
- [ ] Change validation methods to return `Either<Error, T>`

**Phase 4: Refactor Method Bodies**
- [ ] Replace `Optional.ofNullable()` with `Option.of()`
- [ ] Replace `try-catch` with `Try.of(() -> ...)`
- [ ] Replace `if (obj == null)` with `Option.of(obj).map(...)`
- [ ] Replace nested null checks with `.flatMap()` chaining

**Phase 5: Update Controller Layer**
- [ ] Add `.getOrElse()` for default values
- [ ] Add `.getOrElseThrow()` for required values
- [ ] Add `.fold()` for Either handling

**Phase 6: Verify**
- [ ] Run ArchitectureTest: `./gradlew :smartadmin-app:test --tests ArchitectureTest#serviceUsesVavrOption`
- [ ] Verify business logic correctness preserved
- [ ] Check imports are correct

---

## Common Mistakes

See [vavr-patterns.md](knowledge/vavr-patterns.md#common-mistakes--fixes) for detailed code examples of all 9 common mistakes. Summary:

| Mistake | Issue | Fix |
|---------|-------|-----|
| Import not updated | Left `java.util.Optional` import | Remove old, add `io.vavr.control.Option` |
| Return type only | Changed signature but kept `Optional.ofNullable()` | Also change method body |
| Missing .toOption() | Type mismatch Try<T> vs Option<T> | Add `.toOption()` or return `Try<T>` |
| Option in Service not Controller | Service does `.getOrElseThrow()` | Service returns `Option<T>`, Controller handles |
| .map() vs .flatMap() | `Option<Option<T>>` nesting | Use `.flatMap()` when function returns Option |
| Nested Try.of() | Kept try-catch inside Try.of() | Use `Try.flatMap()` chain |
| String errors only | No type safety for error cases | Use `Either<CustomError, T>` for complex domains |
| @Transactional worry | Kept `throws` clause | Try + @Transactional is fully compatible |

---

## Migration Strategy

### Phase 1: Setup and Planning (5 minutes)
1. Verify Vavr dependency
2. Identify target Service class
3. List methods with Optional/try-catch patterns

### Phase 2: Refactoring (2-5 minutes per method)
1. Update imports
2. Change method signatures
3. Refactor method bodies
4. Update Controller layer
5. Compile check: `./gradlew :smartadmin-app:compileJava`

### Phase 3: Validation (3-5 minutes)
1. Run tests: `./gradlew :smartadmin-app:test --tests {ServiceIntegrationTest}`
2. Run ArchUnit: `./gradlew :smartadmin-app:test --tests ArchitectureTest#serviceUsesVavrOption`

**Total Time**: 10-15 minutes per Service class.

**Recommended**: Migrate one Service class at a time (incremental, easy rollback).

---

## Validation

```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests ArchitectureTest#serviceUsesVavrOption
```

**Expected output:** `ArchitectureTest > serviceUsesVavrOption PASSED`

---

## Rationalization Table

| Excuse | Reality |
|--------|---------|
| "Just change the return type" | Must also change imports, method body, null handling |
| "Option.of() is same as Optional.of()" | Option.of() handles null, Optional.of() throws NPE |
| "Don't need to update Controller" | Controller MUST handle Option with .getOrElse()/.fold() |
| ".map() works for all cases" | Use .flatMap() when function returns Option/Try/Either |
| "try-catch is clearer than Try.of()" | ArchUnit enforces functional patterns in Service |
| "Conversion is optional" | ArchitectureTest FAILS if Service uses Optional |
| "Try might break @Transactional" | Try.Failure triggers rollback correctly, fully compatible |

---

## Related Rules

### Mandatory Requirements


### Reference Guidelines


---

## Reference Files

**Vavr rules:**
- `CLAUDE.md` - Option/Try basics
- `CLAUDE.md` - Either/Collections/Patterns
- `CLAUDE.md` - MyBatis + Vavr

**External:**
- [Vavr Option docs](https://docs.vavr.io/#_option)
- [Vavr Try docs](https://docs.vavr.io/#_try)
- [Vavr Either docs](https://docs.vavr.io/#_either)

---

**Last Updated:** 2026-02-06 (v1.3 - Patterns extracted to knowledge/)
