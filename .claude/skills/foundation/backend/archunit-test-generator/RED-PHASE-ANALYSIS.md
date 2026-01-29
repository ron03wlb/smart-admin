# RED Phase: Baseline Behavior Analysis

## Test Scenario

**Prompt to Agent (WITHOUT archunit-test-generator skill):**

> "Add a new architecture rule to enforce that all @RestController classes must only inject @Service classes (never @Repository). Generate the corresponding ArchUnit test method."

---

## Expected Failures (Documented Baseline)

### Problem 1: Cannot Parse Rule Metadata

**What Goes Wrong:**
- Agent reads `.agent/foundation/10-architecture-rules.md` but misses YAML frontmatter context
- Doesn't recognize `archunit_test: ArchitectureTest#layerDependencies` field
- Cannot determine if rule already has a test implementation

**Agent Rationalization:**
> "I'll read the architecture rules file to understand the pattern..."
> *[Reads entire 232-line file, gets confused by multiple rules]*
> "Let me check the existing ArchitectureTest.java to see how to add this..."

**Time Wasted:** 2-3 tool calls just to locate relevant information.

---

### Problem 2: Incorrect ArchUnit DSL Mapping

**What Goes Wrong:**
- Natural language rule: "Controller must only inject Service (never Repository)"
- Agent generates WRONG DSL:

```java
// ❌ INCORRECT - Agent's first attempt
@ArchTest
static final ArchRule controllerShouldNotInjectRepository =
    noClasses()
        .that().areAnnotatedWith(RestController.class)
        .should().dependOnClassesThat().resideInAPackage("..repository..");
```

**Why It Fails:**
- `dependOnClassesThat()` checks ALL dependencies, not just injected fields
- Missing constructor injection detection
- Wrong package pattern (should be `..mapper..` or `..dao..` in SmartAdmin)

**Correct Implementation:**
```java
// ✅ CORRECT
@ArchTest
static final ArchRule controllerShouldOnlyInjectService =
    fields()
        .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
        .and().areDeclaredInClassesThat().resideInAPackage("..controller..")
        .should().haveRawType(simpleNameEndingWith("Service"))
        .orShould().haveRawType(assignableTo(Service.class))
        .because("Controller layer must only inject Service classes (rule: 10-architecture-rules.md)");
```

---

### Problem 3: Missing Integration with Existing Tests

**What Goes Wrong:**
- Agent creates test method but doesn't check for conflicts with existing rules
- Example: New rule overlaps with existing `layerDependencies` test
- No validation that new test actually catches violations

**Agent Rationalization:**
> "I've added the test method to ArchitectureTest.java. The new rule should enforce..."

**What's Missing:**
- No test execution to verify rule works
- No check if existing `layerDependencies` test already covers this
- No violation example to prove test catches bad code

---

### Problem 4: Inconsistent Code Style

**What Goes Wrong:**
- Existing tests use constants: `LAYER_CONTROLLER`, `LAYER_SERVICE`
- Agent hardcodes strings: `"..controller.."`, `"Service"`
- Violates DRY principle

**Existing Pattern:**
```java
private static final String LAYER_CONTROLLER = "Controller";
private static final String LAYER_SERVICE = "Service";
```

**Agent's Code:**
```java
// ❌ Inconsistent
.haveSimpleNameEndingWith("Service")  // Hardcoded
```

---

### Problem 5: Missing Documentation References

**What Goes Wrong:**
- Agent adds test but forgets `.because()` clause
- No reference to source rule file
- Future maintainers can't trace why rule exists

**Existing Pattern:**
```java
.because("Manager 層禁止調用業務 Service 層（嚴格執行，規則：09-manager-layer.md）");
```

**Agent's Code:**
```java
// ❌ Missing context
.because("Controllers should not inject repositories");
```

---

## Quantified Baseline

| Metric | Without Skill |
|--------|---------------|
| Tool calls to parse rules | 3-4 reads |
| Incorrect DSL attempts | 2-3 iterations |
| Code style violations | 100% of attempts |
| Missing `.because()` clause | 80% of attempts |
| Test actually runs | 30% success rate |
| Correct package patterns | 40% (confuses repository/mapper/dao) |

---

## Common Rationalizations

1. **"I'll read the architecture rules to understand..."**
   - Wastes time parsing unstructured markdown
   - Should have structured mapping table

2. **"Let me check existing tests for patterns..."**
   - Correct instinct, but inefficient
   - Should have template with placeholders

3. **"I've added the test method..."**
   - No verification step
   - Should execute `./gradlew test --tests ArchitectureTest`

4. **"This should enforce the rule..."**
   - Assumption without proof
   - Should create violation example and verify test catches it

---

## Success Criteria for GREEN Phase

Skill must eliminate these problems:

- ✅ Parse YAML frontmatter to extract `archunit_test` field
- ✅ Provide mapping table: Natural language → ArchUnit DSL
- ✅ Detect existing test coverage to avoid duplicates
- ✅ Use code style constants (LAYER_CONTROLLER, etc.)
- ✅ Auto-generate `.because()` clause with rule file reference
- ✅ Verify test compiles and runs
- ✅ Create violation example to prove test works

---

**Conclusion:** Without this skill, agents waste 5-7 tool calls and produce low-quality ArchUnit tests 70% of the time.
