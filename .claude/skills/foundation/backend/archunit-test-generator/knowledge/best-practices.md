# ArchUnit Test Generator - Best Practices

## Best Practice 1: Always Use .because() Clause

**Why**: Documents the reason for the rule and provides traceability

```java
// ❌ BAD - No explanation
@ArchTest
static final ArchRule managerNoService =
    noClasses().that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("..service..");

// ✅ GOOD - Clear explanation with source
@ArchTest
static final ArchRule managerNoService =
    noClasses().that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("..service..")
        .because("Manager layer prohibits calling Service layer (rule: F03-manager-layer.md)");
```

---

## Best Practice 2: Use Class Constants

**Why**: Maintains consistency and follows DRY principle

```java
// ❌ BAD - Hardcoded strings
@ArchTest
static final ArchRule controllerNaming =
    classes().that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith("Controller");

// ✅ GOOD - Reusable constants
private static final String LAYER_CONTROLLER = "Controller";

@ArchTest
static final ArchRule controllerNaming =
    classes().that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith(LAYER_CONTROLLER);
```

---

## Best Practice 3: Use Specific Package Patterns

**Why**: Avoids false positives from framework classes

```java
// ❌ BAD - Too broad, catches framework classes
@ArchTest
static final ArchRule noServiceInController =
    noClasses().that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..service..");
// Catches: com.baomidou.mybatisplus.extension.service.IService

// ✅ GOOD - Specific to business code
@ArchTest
static final ArchRule noServiceInController =
    noClasses().that().resideInAPackage("..controller..")
        .should().dependOnClassesThat()
        .resideInAPackage("net.lab1024.sa..service..");
```

---

## Best Practice 4: Always Verify Tests

**Why**: Ensures tests actually catch violations

**Verification Workflow**:
1. Write test
2. Compile: `./gradlew :smartadmin-app:compileTestJava`
3. Run test: `./gradlew :smartadmin-app:test --tests ArchitectureTest#yourTest`
4. Create intentional violation
5. Verify test fails
6. Remove violation
7. Verify test passes

```java
// Step 4: Create violation
@RestController
public class TestController {
    @Autowired  // Intentional violation
    private UserRepository userRepository;
}

// Step 5: Run test
./gradlew :smartadmin-app:test --tests ArchitectureTest#noFieldInjection
// Expected: FAIL

// Step 6: Fix violation (remove @Autowired)

// Step 7: Run test again
./gradlew :smartadmin-app:test --tests ArchitectureTest#noFieldInjection
// Expected: PASS
```

---

## Best Practice 5: Update Rule File Frontmatter

**Why**: Maintains bidirectional traceability

```CLAUDE.md```

**After Creating Test**:
1. Add `archunit_test` field with test method name
2. Update `last_updated` timestamp
3. Commit both rule file and ArchitectureTest.java together

---

## Best Practice 6: Use Framework Exemptions Carefully

**When to Exempt**:
- Framework base classes (e.g., MyBatis-Plus IService)
- Interceptors and filters
- Configuration classes

**How to Exempt**:
```java
layeredArchitecture()
    .ignoreDependency(
        resideInAPackage("..interceptor.."),
        alwaysTrue()
    )
    .ignoreDependency(
        simpleNameContaining("MyBatisPlugin"),
        alwaysTrue()
    )
```

**When NOT to Exempt**:
- Business code violations
- Architectural violations in admin module

---

## Best Practice 7: Write Comprehensive Javadoc

**Template**:
```java
/**
 * 【嚴格執行】{Enforcement Level + Rule Summary}
 *
 * <p>{Detailed Explanation}
 *
 * <p>Exemptions (if any):
 * <ul>
 *   <li>{Exemption 1} - {Reason}
 *   <li>{Exemption 2} - {Reason}
 * </ul>
 *
 * <p>Rule Source: {rule-file}.md
 */
@ArchTest
static final ArchRule {ruleName} = ...
```

**Enforcement Levels**:
- 【嚴格執行】(Strictly Enforced) - No exemptions
- 【建議遵循】(Recommended) - Has documented exemptions

---

## Best Practice 8: Check for Duplicate Tests

**Before Creating New Test**:
```bash
# Search existing tests
grep -n "static final ArchRule" ArchitectureTest.java

# Search for similar rules
grep -i "transactional" ArchitectureTest.java
grep -i "manager.*service" ArchitectureTest.java
```

**If Duplicate Exists**:
- Update existing test instead of creating new one
- Consider merging similar rules
- Document why multiple tests are needed

---

## Best Practice 9: Use Custom ArchCondition for Complex Validations

**When to Use**:
- Annotation parameter validation
- Complex multi-condition checks
- Custom business logic

**Template**:
```java
@ArchTest
static final ArchRule customRule =
    methods()
        .that().areAnnotatedWith(CustomAnnotation.class)
        .should(new ArchCondition<JavaMethod>("custom validation") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                // Custom validation logic
                if (!isValid(method)) {
                    String message = "Violation description";
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        })
        .because("Rule explanation");
```

---

## Best Practice 10: Time-Box Test Generation

**Workflow Time Budget**:
- Parse rule file: 1 min
- Map to DSL: 2 min
- Check existing tests: 1 min
- Generate test: 3 min
- Verify: 3 min
- **Total**: 10 min per rule

**If Exceeding Time**:
- Simplify the rule
- Split into multiple simpler rules
- Consult team for guidance

---

## Checklist (Copy-Paste)

Before completing test generation:

- [ ] Parse YAML frontmatter
- [ ] No duplicate test exists
- [ ] Correct ArchUnit pattern selected
- [ ] Uses class constants
- [ ] .because() clause with rule file reference
- [ ] Comprehensive Javadoc
- [ ] Test compiles
- [ ] Test runs successfully
- [ ] Created violation example
- [ ] Verified test catches violation
- [ ] Updated rule file frontmatter

---

See SKILL.md for detailed workflows and patterns.
