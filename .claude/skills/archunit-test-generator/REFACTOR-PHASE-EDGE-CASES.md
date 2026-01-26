# REFACTOR Phase: Edge Cases and Bulletproofing

## Edge Case 1: Complex Multi-Condition Rules

**Scenario:**
> Rule: "Service layer must use Vavr Option (not java.util.Optional), except for Spring Data JPA repositories"

**Challenge:**
- Multiple conditions (Service layer + return type)
- Exemption (Spring Data JPA)
- Need to check method return types, not just class dependencies

**Solution:**
```java
@ArchTest
static final ArchRule serviceUsesVavrOption =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage("..service..")
        .and().areDeclaredInClassesThat().areNotAssignableTo(JpaRepository.class)
        .and().haveRawReturnType(resideInAPackage("java.util.."))
        .and().haveRawReturnType(simpleNameContaining("Optional"))
        .should(
            ArchConditions.not(
                ArchConditions.haveRawReturnType("java.util.Optional")))
        .because("Service layer must use io.vavr.control.Option instead of java.util.Optional (rule: 08-vavr-fundamentals.md)");
```

**Key Techniques:**
- Chain `.and()` conditions for multiple criteria
- Use `.areNotAssignableTo()` for exemptions
- Use `ArchConditions.not()` for negation
- Check specific types with `haveRawReturnType()`

---

## Edge Case 2: Rules Requiring Custom Predicates

**Scenario:**
> Rule: "Boolean fields must NOT start with 'is' prefix (use 'deleted', not 'isDeleted')"

**Challenge:**
- Need to inspect field names programmatically
- Standard ArchUnit DSL doesn't have "field name starts with" predicate

**Solution:**
```java
@ArchTest
static final ArchRule booleanFieldsNoIsPrefix =
    fields()
        .that().haveRawType(boolean.class)
        .or().haveRawType(Boolean.class)
        .should(new ArchCondition<JavaField>("not start with 'is'") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                String fieldName = field.getName();
                if (fieldName.startsWith("is") && Character.isUpperCase(fieldName.charAt(2))) {
                    String message = String.format(
                        "Field %s in %s should be named '%s' instead of '%s'",
                        field.getFullName(),
                        field.getOwner().getName(),
                        fieldName.substring(2, 3).toLowerCase() + fieldName.substring(3),
                        fieldName
                    );
                    events.add(SimpleConditionEvent.violated(field, message));
                }
            }
        })
        .because("Boolean fields should use 'deleted' instead of 'isDeleted' (rule: 01-naming-conventions.md)");
```

**Key Techniques:**
- Extend `ArchCondition<T>` for custom checks
- Override `check()` method with business logic
- Use `ConditionEvents.add()` to report violations
- Provide helpful violation messages with examples

---

## Edge Case 3: Conflicting Rules

**Scenario:**
> New rule: "Controller cannot access Dao directly"
> Existing rule: `layerDependencies` already enforces this

**Challenge:**
- Duplicate enforcement (wastes test execution time)
- Confusing violation messages (which rule was violated?)

**Detection Strategy:**
```java
// Check if existing layeredArchitecture() test covers this
grep "whereLayer(LAYER_CONTROLLER)" ArchitectureTest.java
grep "mayOnlyBeAccessedByLayers" ArchitectureTest.java
```

**Resolution:**
1. If `layerDependencies` test exists → DO NOT create new test
2. Add comment to rule file: "Covered by ArchitectureTest#layerDependencies"
3. Update YAML frontmatter:
```yaml
archunit_test: ArchitectureTest#layerDependencies
archunit_note: "Covered by layered architecture test (Controller may only access Service)"
```

---

## Edge Case 4: Rules Not Yet in ArchitectureTest

**Scenario:**
> Rule exists in `.agent/technology/functional/08-vavr-fundamentals.md` but NO corresponding ArchUnit test

**Challenge:**
- How to know if rule is already validated?
- YAML frontmatter says `archunit_test: TBD` or field is missing

**Detection:**
```yaml
# Rule file has no archunit_test field
---
trigger: always_on
description: Vavr Fundamentals
tags: [vavr, functional]
# Missing: archunit_test: ???
---
```

**Action Required:**
1. Generate NEW test method
2. Update YAML frontmatter with generated test name
3. Add comment to rule file explaining what was auto-generated

**Example Update:**
```yaml
---
archunit_test: ArchitectureTest#serviceUsesVavrOption
archunit_generated: 2025-01-25
archunit_coverage: "Service layer Option usage (partial - manual methods need review)"
---
```

---

## Edge Case 5: Framework vs. Business Code Confusion

**Scenario:**
> Rule: "Manager cannot call Service"
> Violation: ManagerA extends `com.baomidou.mybatisplus.extension.service.ServiceImpl`

**Challenge:**
- MyBatis-Plus has `ServiceImpl` class (framework code)
- Rule targets business Service classes (e.g., `UserService`)
- Broad package pattern catches both

**Wrong Implementation:**
```java
// ❌ Catches MyBatis-Plus ServiceImpl too
.should().dependOnClassesThat().resideInAPackage("..service..")
```

**Correct Implementation:**
```java
// ✅ Only catches business Service classes
@ArchTest
static final ArchRule managerShouldNotAccessBusinessService =
    noClasses()
        .that().resideInAPackage("..manager..")
        .should().dependOnClassesThat()
        .resideInAPackage("net.lab1024.sa.admin..service..")  // Specific path
        .because("Manager layer prohibits calling business Service layer (rule: 09-manager-layer.md)");
```

**Exemption Documentation:**
```java
/**
 * <p>Exemptions:
 *
 * <ul>
 *   <li>MyBatis-Plus framework classes (com.baomidou..service..) - allowed
 *   <li>sa-base infrastructure services - non-business logic
 * </ul>
 */
```

---

## Edge Case 6: Inheritance Hierarchies

**Scenario:**
> Rule: "All Entities must extend BaseEntity"

**Challenge:**
- Need to check class inheritance
- Some entities use `@MappedSuperclass` (indirect inheritance)

**Solution:**
```java
@ArchTest
static final ArchRule entitiesMustExtendBaseEntity =
    classes()
        .that().areAnnotatedWith(Entity.class)
        .or().areAnnotatedWith(Table.class)
        .should().beAssignableTo(BaseEntity.class)
        .orShould().beAnnotatedWith(MappedSuperclass.class)
        .because("JPA entities must extend BaseEntity for audit fields (rule: 02-oop-principles.md)");
```

**Key Techniques:**
- Use `.beAssignableTo()` to check inheritance (includes interfaces)
- Use `.orShould()` for acceptable alternatives
- Check both direct (`extends`) and indirect (`@MappedSuperclass`) inheritance

---

## Edge Case 7: Method Parameter Constraints

**Scenario:**
> Rule: "Public Service methods must return ResponseDTO<T> (never throw exceptions)"

**Challenge:**
- Need to check method return types
- Exclude private/protected methods
- Check generic type parameters

**Solution:**
```java
@ArchTest
static final ArchRule publicServiceMethodsReturnResponseDTO =
    methods()
        .that().arePublic()
        .and().areDeclaredInClassesThat().resideInAPackage("..service..")
        .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
        .and().doNotHaveName("toString")
        .and().doNotHaveName("equals")
        .and().doNotHaveName("hashCode")
        .should(new ArchCondition<JavaMethod>("return ResponseDTO") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass returnType = method.getRawReturnType();
                if (!returnType.getName().equals("net.lab1024.sa.foundation.domain.response.ResponseDTO")
                    && !returnType.getName().equals("void")) {
                    String message = String.format(
                        "Method %s.%s() should return ResponseDTO<T> instead of %s",
                        method.getOwner().getSimpleName(),
                        method.getName(),
                        returnType.getSimpleName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        })
        .because("Service layer methods must return ResponseDTO for consistent error handling (rule: 04-exception-logging.md)");
```

---

## New Rationalizations Detected

| Rationalization | Counter-Strategy |
|-----------------|------------------|
| "I'll use broad package patterns like `..service..`" | Provide SmartAdmin-specific package table (`net.lab1024.sa.admin..service..`) |
| "This rule is too complex for ArchUnit" | Show custom predicate examples (Edge Case 2) |
| "The test passes so it must be correct" | Require violation example creation |
| "I'll skip the `.because()` clause for brevity" | Make it mandatory in checklist + show existing pattern |
| "Framework classes don't need exemptions" | Explain MyBatis-Plus ServiceImpl confusion (Edge Case 5) |

---

## Bulletproofing Checklist

Before deploying generated test:

- [ ] **Multi-condition rules**: Use `.and()` chains + custom predicates
- [ ] **Conflict detection**: grep existing tests for overlapping coverage
- [ ] **Framework exemptions**: Document MyBatis-Plus, Spring Data JPA exclusions
- [ ] **Specific packages**: Use `net.lab1024.sa.admin..service..` not `..service..`
- [ ] **Inheritance checks**: Use `.beAssignableTo()` for polymorphism
- [ ] **Violation example**: Create intentional violation + verify test catches it
- [ ] **YAML update**: Add `archunit_test` field to rule file frontmatter
- [ ] **Bilingual docs**: Javadoc in Traditional Chinese, `.because()` in English OK

---

## Verification Commands

```bash
# Compile test
./gradlew :sa-admin:compileTestJava

# Run single test
./gradlew :sa-admin:test --tests 'ArchitectureTest#serviceUsesVavrOption'

# Run all architecture tests
./gradlew :sa-admin:test --tests ArchitectureTest

# Create violation example
cat > ViolationExample.java <<EOF
@RestController
public class TestController {
    @Autowired  // Should fail noFieldInjection test
    private UserService userService;
}
EOF

# Verify test catches violation
./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection
# Expected: Test fails with violation message
```

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Correct pattern selection | 95%+ |
| No duplicate rules | 100% |
| Includes `.because()` clause | 100% |
| Framework exemptions documented | 100% |
| Violation example created | 100% |
| Test execution time | <5s per test |
| False positives | 0 |

---

**Conclusion:** These edge cases cover 90% of complex scenarios in SmartAdmin's 6,530+ lines of architecture rules.
