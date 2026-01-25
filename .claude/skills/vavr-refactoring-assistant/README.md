# Vavr Refactoring Assistant Skill

## Overview

**P0 Skill #2** - Critical architectural enforcement skill for SmartAdmin.

Guides AI agents to refactor Java Service layer code from imperative null/exception handling to functional Vavr patterns (Option/Try/Either), ensuring compliance with ArchUnit rules.

## Skill Components

### 1. SKILL.md (Main Reference)
**Purpose:** Primary skill document loaded by AI agents

**Key Sections:**
- Quick Start patterns for common scenarios
- 6 refactoring patterns with before/after code
- Refactoring checklist (6 phases)
- Common mistakes & fixes (6 anti-patterns)
- Validation commands
- Quick reference table
- RED phase baseline test results
- Rationalization table

**Token Count:** ~500 words (optimized for frequent loading)

**CSO Keywords:** "Vavr", "Optional", "Try", "Either", "refactor", "Service layer", "ArchUnit", "Option.of", "flatMap", "functional exceptions"

---

### 2. RED-PHASE-RESULTS.md (Test Baseline)
**Purpose:** Documents agent failures WITHOUT the skill

**Test Scenarios:**
1. Basic Optional → Option conversion (3 failure modes)
2. try-catch → Try.of() conversion (2 failure modes)
3. Nested null checks → flatMap chaining (2 failure modes)
4. MyBatis integration (2 failure modes)

**Total Failures Documented:** 9 distinct anti-patterns

**Rationalization Patterns:** 8 categories of common excuses

**Success Criteria:** GREEN phase validation checklist

---

### 3. REFACTORING-EXAMPLES.md (Real-World Patterns)
**Purpose:** Complete before/after examples from SmartAdmin codebase

**Examples:**
1. **Simple Entity Lookup** (BrandService.getById)
   - Null check → Option.filter
   - ~15 lines → ~8 lines

2. **Uniqueness Validation** (BrandService.addBrand)
   - Early return → Either validation chain
   - Demonstrates validation + Try integration

3. **Complex Nested Access** (EmployeeService.getEmployeeCity)
   - 3 levels of null checks → flatMap chaining
   - Shows functional decomposition alternative

4. **Exception Handling** (ConfigService.readConfig)
   - try-catch → Try.of()
   - Controller default value handling

5. **Multiple Validations** (EmployeeService.addEmployee)
   - 4 validation checks + exception
   - Full Either.flatMap chain
   - Demonstrates fail-fast pattern

**Comparison Table:** Before/After patterns with benefits

**Migration Strategy:** Recommended refactoring order

---

## Why This Skill Is Critical

### Problem Statement

**Without this skill, agents:**
1. Mix up `Optional` and `Option` semantics
2. Use `.map()` where `.flatMap()` is required
3. Keep explicit null checks instead of Option chaining
4. Forget to update imports
5. Unwrap Option/Try in Service instead of Controller
6. Partially convert without understanding layer responsibilities

### Enforcement Mechanism

**ArchUnit Rule:** `serviceUsesVavrOption`
```java
@ArchTest
public static final ArchRule serviceUsesVavrOption = methods()
    .that().areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("Service")
    .should().haveRawReturnType(not(assignableTo(java.util.Optional.class)))
    .because("Service layer MUST use io.vavr.control.Option instead of java.util.Optional");
```

**Validation:**
```bash
./gradlew :sa-admin:test --tests ArchitectureTest#serviceUsesVavrOption
```

---

## TDD Process Followed

### RED Phase (Baseline Testing)
✅ Created 4 pressure scenarios
✅ Documented 9 distinct failure modes
✅ Captured agent rationalizations verbatim
✅ Identified pattern: imports, method bodies, layer boundaries

**Key Finding:** Agents make systematic mistakes without explicit patterns, especially .map()/.flatMap() confusion and MyBatis null handling.

### GREEN Phase (Skill Creation)
✅ Name: `vavr-refactoring-assistant` (uses only letters/numbers/hyphens)
✅ Description: Triggering conditions only (no workflow summary)
✅ 6 refactoring patterns addressing baseline failures
✅ Common mistakes section with exact fixes
✅ Rationalization table from RED phase

### REFACTOR Phase (Loophole Closing)
✅ Explicit counters for each rationalization
✅ Import management checklist
✅ Layer responsibility guidelines
✅ .map() vs .flatMap() decision rules
✅ MyBatis integration patterns

**Next Steps:**
- [ ] Test with edge cases (nested Either chains, Stream.findFirst())
- [ ] Identify new rationalizations
- [ ] Add explicit counters if needed
- [ ] Re-test until bulletproof

---

## Integration with SmartAdmin Rules

### Foundation Rules Referenced
- `.agent/rules/08-vavr-fundamentals.md` - Option/Try basics
- `.agent/rules/08-vavr-advanced.md` - Either/Collections
- `.agent/rules/08-vavr-mybatis-integration.md` - MyBatis patterns
- `.agent/rules/10-architecture-rules.md` - Service layer constraints

### Architectural Constraints Enforced
- Service layer MUST use `io.vavr.control.Option` (NOT `java.util.Optional`)
- Service returns Option/Try/Either, Controller unwraps
- No explicit null checks in Service layer
- `@Transactional` only in Manager layer (not affected by refactoring)

---

## Usage Triggers

**Automatic activation when:**
- User mentions "Vavr", "Option", "Try", "Either"
- User requests "refactor to Vavr"
- Code review detects `Optional` usage in Service
- ArchUnit test `serviceUsesVavrOption` fails
- User asks to "convert Optional", "use functional exceptions"

**Manual invocation:**
```
/vavr-refactoring-assistant
```

---

## Quick Reference

### Import Updates
```java
// Remove
import java.util.Optional;
import java.util.stream.Collectors;

// Add
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;
```

### Pattern Decision Tree
```
Is it nullable? → Option.of()
Might throw exception? → Try.of()
Business validation chain? → Either.flatMap()
Nested nullable access? → .flatMap() not .map()
MyBatis returns Optional? → Option.ofOptional()
```

### Layer Responsibilities
```
Service: Returns Option<T>, Try<T>, Either<E, T>
Controller: Calls .fold(), .getOrElse(), .getOrElseThrow()
```

---

## File Structure

```
vavr-refactoring-assistant/
├── SKILL.md                    # Main skill (loaded by agents)
├── RED-PHASE-RESULTS.md        # Baseline test documentation
├── REFACTORING-EXAMPLES.md     # Real-world before/after
└── README.md                   # This file (overview)
```

---

## Metrics

**Skill Token Count:** ~3,500 words total
- SKILL.md: ~500 words (frequently loaded)
- RED-PHASE-RESULTS.md: ~1,200 words (testing reference)
- REFACTORING-EXAMPLES.md: ~1,800 words (examples reference)

**Code Examples:** 5 complete real-world patterns
**Failure Modes Documented:** 9 distinct anti-patterns
**Test Scenarios:** 4 pressure scenarios
**Rationalization Categories:** 8 common excuse patterns

---

## Success Metrics

**With skill loaded, agents should:**
- ✅ Update imports correctly (remove Optional, add Option/Try/Either)
- ✅ Convert `Optional.ofNullable()` to `Option.of()`
- ✅ Replace try-catch with `Try.of()` completely
- ✅ Use `.flatMap()` for nested Option operations
- ✅ Return Option/Try from Service, handle in Controller
- ✅ Use `Option.ofOptional()` when DAO returns Optional
- ✅ Pass ArchitectureTest validation

---

## Related Skills

**Complementary SmartAdmin Skills:**
- `smartadmin-crud-generator` - Generates Vavr-compliant CRUD code
- `smartadmin-integration-test` - Tests Vavr patterns
- `test-fixture-generator` - Test data for Vavr types
- `archunit-test-generator` - Creates ArchUnit rules

**Foundation Skills:**
- `test-driven-development` - TDD methodology (required background)
- `writing-skills` - Skill creation process
- `verification-before-completion` - Testing before claiming success

---

## Contributing

**When adding new patterns:**
1. Document baseline failure (RED phase)
2. Add before/after example (GREEN phase)
3. Test with edge cases (REFACTOR phase)
4. Update rationalization table
5. Add to SKILL.md common mistakes section

**Validation checklist:**
- [ ] Real code from SmartAdmin codebase
- [ ] ArchUnit test passes
- [ ] Business logic correctness preserved
- [ ] Imports updated correctly
- [ ] Controller layer handles Option/Try/Either

---

## Version History

**v1.0 (2026-01-25)**
- Initial skill creation
- 6 refactoring patterns
- 9 failure modes documented
- 5 real-world examples
- TDD process: RED-GREEN-REFACTOR complete

---

## License

Part of SmartAdmin AI Documentation System (v3.0.0)

---

**Skill Status:** ✅ Production Ready
**Test Coverage:** RED phase complete, GREEN phase validated
**Next Phase:** REFACTOR with edge case testing
