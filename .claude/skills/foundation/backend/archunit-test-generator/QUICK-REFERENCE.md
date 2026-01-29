# Quick Reference Card: ArchUnit Test Generator

**1-Page Cheat Sheet for Agents**

---

## When to Use This Skill

✅ User says: "Add ArchUnit test for..."
✅ User says: "Generate test to enforce..."
✅ Adding new architecture rule in `.agent/rules/*.md`
✅ ArchitectureTest.java needs new constraint
✅ Keywords: "layer dependency", "annotation restriction", "naming convention", "field injection"

---

## 5-Step Workflow

```
1. Parse YAML → Check archunit_test field (exists? skip, else generate)
2. Select Pattern → Use mapping table (6 patterns)
3. Generate Test → Template + SmartAdmin style
4. Verify → Compile + run + violation example
5. Update YAML → Add archunit_test field
```

---

## Pattern Selection Matrix

| Rule Description | ArchUnit Entry Point |
|------------------|---------------------|
| "X layer cannot access Y layer" | `layeredArchitecture().whereLayer(X).mayNotAccessLayer(Y)` |
| "@Annotation only in X layer" | `methods().that().areAnnotatedWith(Annotation.class).should().beDeclaredInClassesThat()...` |
| "No field injection" | `fields().should().notBeAnnotatedWith(Autowired.class)` |
| "Classes must end with Suffix" | `classes().that().resideInAPackage("..pkg..").should().haveSimpleNameEndingWith("Suffix")` |
| "Cannot depend on package X" | `noClasses().should().dependOnClassesThat().resideInAPackage("..pkg..")` |
| "Package X deprecated" | `noClasses().that().resideOutsideOfPackage("allowed..").should().dependOnClassesThat()...` |
| "Field naming pattern" | `fields().that().haveRawType(...).should(new ArchCondition<JavaField>() {...})` |

---

## SmartAdmin Code Style

### Use Constants
```java
private static final String LAYER_CONTROLLER = "Controller";
private static final String LAYER_SERVICE = "Service";
```

### Specific Packages
```java
// ✅ Correct
"net.lab1024.sa.admin..service.."

// ❌ Wrong (catches framework)
"..service.."
```

### Mandatory .because() Clause
```java
.because("Manager layer prohibits calling Service (rule: 09-manager-layer.md)");
```

---

## Template

```java
/**
 * 【嚴格執行】{Rule Description}
 *
 * <p>{Detailed Explanation}
 *
 * <p>Exemptions:
 * <ul>
 *   <li>{Exemption 1} - {Reason}
 * </ul>
 *
 * <p>Rule Source: {rule-file}.md
 */
@ArchTest
static final ArchRule {ruleName} =
    {ArchUnitDSL}
        .because("{Summary} (rule: {rule-file}.md)");
```

---

## Verification Checklist

- [ ] Parse YAML frontmatter (check existing test)
- [ ] Select correct ArchUnit pattern
- [ ] Use class constants (`LAYER_CONTROLLER`)
- [ ] Specific packages (`net.lab1024.sa.admin..`)
- [ ] Include `.because()` clause with rule file
- [ ] Compile: `./gradlew :sa-admin:compileTestJava`
- [ ] Run: `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] Create violation example
- [ ] Verify test catches violation
- [ ] Update rule file YAML frontmatter

---

## Common Mistakes → Fixes

| ❌ Mistake | ✅ Fix |
|-----------|-------|
| `dependOnClassesThat()` for injection | Use `fields().that().areDeclaredInClassesThat()` |
| Hardcoded `"Controller"` | Use `LAYER_CONTROLLER` constant |
| Missing `.because()` | Always add (mandatory) |
| Broad `..service..` | Use `net.lab1024.sa.admin..service..` |
| No verification | Create violation example + run test |
| Comment-only exemptions | Use `.ignoreDependency()` or `.areNotAssignableTo()` |

---

## Example: Custom Predicate

```java
@ArchTest
static final ArchRule booleanFieldsNoIsPrefix =
    fields()
        .that(new DescribedPredicate<JavaField>("have boolean type") {
            @Override
            public boolean test(JavaField field) {
                return field.getRawType().getName().equals("boolean")
                    || field.getRawType().getName().equals("java.lang.Boolean");
            }
        })
        .should(new ArchCondition<JavaField>("not start with 'is'") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                if (field.getName().startsWith("is") && ...) {
                    events.add(SimpleConditionEvent.violated(field, message));
                }
            }
        });
```

---

## Framework Exemptions

```java
// MyBatis-Plus ServiceImpl
.ignoreDependency(simpleNameContaining("MyBatisPlugin"), alwaysTrue())

// Spring Data JPA
.and().areNotAssignableTo(JpaRepository.class)

// Interceptors
.ignoreDependency(resideInAPackage("..interceptor.."), alwaysTrue())
```

---

## Violation Example Pattern

```java
// Create violation
@RestController
public class TestController {
    @Autowired  // Should fail noFieldInjection test
    private UserRepository userRepository;
}

// Verify test catches it
./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection
// Expected: Test FAILS with violation message

// Fix violation
@RestController
@RequiredArgsConstructor
public class TestController {
    private final UserService userService;  // ✅ Correct
}

// Verify test passes
./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection
// Expected: BUILD SUCCESSFUL
```

---

## YAML Frontmatter Update

```yaml
---
# Before
archunit_test: ArchitectureTest#existingTest

# After
archunit_test:
  - ArchitectureTest#existingTest
  - ArchitectureTest#newTestName
archunit_generated: 2025-01-25
last_updated: 2025-01-25
---
```

---

## Time Budget

| Step | Expected Time |
|------|---------------|
| Parse rule + select pattern | 30s |
| Generate test method | 1 min |
| Verify compilation | 30s |
| Create violation example | 1 min |
| Verify test catches violation | 1 min |
| Update YAML frontmatter | 30s |
| **TOTAL** | **4-5 minutes** |

**If taking >5 minutes:** Re-read this reference card.

---

## Need More Details?

- **Basics:** README.md
- **Full Skill:** SKILL.md
- **Examples:** EXAMPLE-GENERATION.md, DEMONSTRATION.md
- **Edge Cases:** REFACTOR-PHASE-EDGE-CASES.md
- **Mistakes:** RATIONALIZATIONS-TABLE.md

---

**Remember:** Always verify with violation example. Test must FAIL before fix, PASS after fix.
