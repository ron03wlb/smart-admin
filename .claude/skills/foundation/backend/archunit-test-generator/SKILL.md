---
name: archunit-test-generator
description: [P0 - Critical] Use when adding new architecture rules in .agent/rules/*.md or when ArchitectureTest.java needs to enforce new constraints (layer dependencies, annotation restrictions, naming conventions, field injection patterns)
---

# ArchUnit Test Generator

Auto-generate ArchUnit test methods from SmartAdmin architecture rules in `.agent/rules/*.md`.

## Quick Start

**Most common usage:**
```
User: "Add ArchUnit test to enforce that Manager layer cannot call Service layer"
User: "Generate test for the @Transactional restriction in Manager layer"
User: "Create ArchUnit rule to prevent field injection"
```

You will:
1. Parse `.agent/rules/{rule-file}.md` YAML frontmatter for `archunit_test` field (~1 min)
2. Map natural language rule to ArchUnit DSL using conversion table (~2 min)
3. Check existing `ArchitectureTest.java` for conflicts/duplicates (~1 min)
4. Generate test method with correct style and documentation (~3 min)
5. Verify test compiles and catches violations (~3 min)

**Total Time Estimate**: 10-15 minutes per rule (workflow < 3 min, verification ~7 min)

## Core Pattern: Rule → DSL Mapping

SmartAdmin has **6 ArchUnit test patterns** based on rule types:

### Pattern 1: Layer Dependency Rules

**Natural Language:**
> "Controller → Service → Manager → Dao (no cross-layer access)"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule layerDependencies =
    layeredArchitecture()
        .consideringAllDependencies()
        .layer(LAYER_CONTROLLER).definedBy("..controller..")
        .layer(LAYER_SERVICE).definedBy("..service..")
        .layer(LAYER_MANAGER).definedBy("..manager..")
        .layer(LAYER_DAO).definedBy("..dao..")
        .whereLayer(LAYER_CONTROLLER).mayNotBeAccessedByAnyLayer()
        .whereLayer(LAYER_SERVICE).mayOnlyBeAccessedByLayers(LAYER_CONTROLLER)
        .whereLayer(LAYER_MANAGER).mayOnlyBeAccessedByLayers(LAYER_SERVICE)
        .whereLayer(LAYER_DAO).mayOnlyBeAccessedByLayers(LAYER_MANAGER, LAYER_SERVICE);
```

**Key Characteristics:**
- Use `layeredArchitecture()` API
- Define layers with `.definedBy()` package patterns
- Specify access rules with `.whereLayer().mayOnlyBeAccessedByLayers()`
- Use class constants: `LAYER_CONTROLLER`, `LAYER_SERVICE`

---

### Pattern 2: Annotation Restrictions

**Natural Language:**
> "@Transactional only in Manager layer (never in Service)"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule transactionalOnlyInManager =
    methods()
        .that().areAnnotatedWith(Transactional.class)
        .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
        .because("@Transactional must only be used in Manager layer (rule: 09-manager-layer.md)");
```

**Key Characteristics:**
- Use `methods().that().areAnnotatedWith()`
- Check declaring class with `.beDeclaredInClassesThat()`
- Always add `.because()` clause with rule file reference

---

### Pattern 3: Package Dependency Prohibition

**Natural Language:**
> "Manager layer cannot call Service layer"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule managerShouldNotAccessBusinessService =
    noClasses()
        .that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("net.lab1024.sa.admin..service..")
        .because("Manager 層禁止調用業務 Service 層（嚴格執行，規則：09-manager-layer.md）");
```

**Key Characteristics:**
- Use `noClasses()` for prohibition rules
- Specify exact package patterns (avoid wildcards for business code)
- Include exemptions in comments (e.g., MyBatis-Plus framework classes)

---

### Pattern 4: Field Injection Prohibition

**Natural Language:**
> "No field injection - use constructor injection only"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule noFieldInjection =
    fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..controller..", "..service..", "..manager..")
        .should().notBeAnnotatedWith(Autowired.class)
        .because("Use constructor injection with @RequiredArgsConstructor (rule: 10-architecture-rules.md)");
```

**Key Characteristics:**
- Use `fields().that().areDeclaredInClassesThat()`
- Apply to specific packages where injection is expected
- Reference Spring best practices in `.because()` clause

---

### Pattern 5: Naming Convention Rules

**Natural Language:**
> "Classes in controller package must end with 'Controller'"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule controllerNaming =
    classes()
        .that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith(LAYER_CONTROLLER)
        .orShould().haveSimpleNameEndingWith("Interceptor");
```

**Key Characteristics:**
- Use `.haveSimpleNameEndingWith()` for suffix checks
- Use `.orShould()` for exemptions
- Use class constants for reusable strings

---

### Pattern 6: Package Migration Rules

**Natural Language:**
> "No code should use deprecated common.* packages (use foundation.* instead)"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule noNewCodeShouldUseLegacyCommonPackages =
    noClasses()
        .that().resideOutsideOfPackage("net.lab1024.sa.common.core..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "net.lab1024.sa.common.apiencrypt..",
            "net.lab1024.sa.common.cache..",
            "net.lab1024.sa.common.captcha..")
        .because("Legacy common.* package naming is deprecated, use foundation.* instead");
```

**Key Characteristics:**
- Use `resideOutsideOfPackage()` to exclude allowed usage
- List specific deprecated packages (avoid broad wildcards)
- Explain migration context in `.because()` clause

---

### Pattern 7: Annotation Parameter Verification

**Natural Language:**
> "@Transactional must use rollbackFor = Throwable.class (not Exception.class)"

**ArchUnit DSL:**
```java
@ArchTest
static final ArchRule transactionalRollbackForThrowable =
    methods()
        .that().areAnnotatedWith(Transactional.class)
        .should(new ArchCondition<JavaMethod>("use rollbackFor = Throwable.class") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Transactional annotation = method.getAnnotationOfType(Transactional.class);
                Class<?>[] rollbackFor = annotation.rollbackFor();

                boolean hasThrowable = Arrays.stream(rollbackFor)
                    .anyMatch(clazz -> clazz.equals(Throwable.class));

                if (!hasThrowable || rollbackFor.length != 1) {
                    String message = String.format(
                        "Method %s.%s() should use @Transactional(rollbackFor = Throwable.class), " +
                        "but uses rollbackFor = %s",
                        method.getOwner().getSimpleName(),
                        method.getName(),
                        Arrays.toString(rollbackFor)
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        })
        .because("@Transactional must rollback on all throwables including errors (rule: 09-manager-layer.md)");
```

**Key Characteristics:**
- Use custom `ArchCondition` for annotation parameter checks
- Access annotation via `.getAnnotationOfType()`
- Validate parameter values (arrays, classes, primitives)
- Build descriptive violation messages with actual vs expected values
- Use `.check()` method with `ConditionEvents` to report violations

**Common Annotation Parameter Checks:**
| Annotation | Parameter | Expected Value | Pattern |
|------------|-----------|----------------|---------|
| `@Transactional` | `rollbackFor` | `Throwable.class` | Array check with `.anyMatch()` |
| `@Cacheable` | `key` | Non-empty string | String validation |
| `@RequestMapping` | `method` | Specific HTTP method | Enum array check |
| `@Column` | `nullable` | `false` for required fields | Boolean check |

---

## Test Generation Workflow

### Step 1: Parse Rule File Frontmatter

Read YAML frontmatter to extract metadata:

```yaml
---
trigger: always_on
description: Manager Layer Architecture Rules
tags: [architecture, manager, transaction, cache]
archunit_test: ArchitectureTest#managerShouldNotAccessBusinessService
last_updated: 2025-01-17
---
```

**Key Fields:**
- `archunit_test`: Existing test method name (if already implemented)
- `tags`: Rule categories (helps determine pattern type)
- `description`: Natural language rule summary

**Parsing Code:**
```java
// Extract frontmatter
String frontmatter = ruleContent.split("---")[1];
Map<String, String> metadata = parseFrontmatter(frontmatter);
String existingTest = metadata.get("archunit_test");
```

---

### Step 2: Check Existing Test Coverage

Read `ArchitectureTest.java` to avoid duplicates:

```bash
grep -n "static final ArchRule" ArchitectureTest.java
```

**Conflict Detection:**
- Rule already has test → Update existing test instead of creating new one
- Similar rule exists → Consider merging or clarifying distinction
- No coverage → Proceed with generation

---

### Step 3: Select ArchUnit DSL Pattern

Use this decision matrix:

| Rule Type | Keyword Triggers | ArchUnit Pattern |
|-----------|------------------|------------------|
| Layer dependencies | "Controller → Service", "may only access" | `layeredArchitecture()` |
| Annotation restrictions | "@Transactional only in", "@Cacheable" | `methods().that().areAnnotatedWith()` |
| Annotation parameters | "rollbackFor = Throwable.class", "specific parameter value" | `methods().should(new ArchCondition<>())` |
| Package prohibitions | "cannot call", "prohibit", "should not access" | `noClasses().that().resideInAPackage()` |
| Field injection | "field injection", "@Autowired" | `fields().that().areDeclaredInClassesThat()` |
| Naming conventions | "must end with", "naming pattern" | `classes().that().resideInAPackage().should().haveSimpleNameEndingWith()` |
| Package migration | "deprecated package", "use instead" | `noClasses().should().dependOnClassesThat().resideInAnyPackage()` |

---

### Step 4: Generate Test Method

**Template Structure:**
```java
/**
 * 【{Enforcement Level}】{Rule Description}
 *
 * <p>{Detailed Explanation}
 *
 * <p>Exemptions (if any):
 *
 * <ul>
 *   <li>{Exemption 1} - {Reason}
 *   <li>{Exemption 2} - {Reason}
 * </ul>
 *
 * <p>Rule Source: {rule-file}.md
 */
@ArchTest
static final ArchRule {ruleName} =
    {ArchUnitDSL}
        .because("{Rule summary} (rule: {rule-file}.md)");
```

**Enforcement Levels:**
- `【嚴格執行】` (Strictly Enforced) - No exemptions
- `【建議遵循】` (Recommended) - Has documented exemptions

---

### Step 5: Add Integration Metadata

Update rule file frontmatter with test reference:

```yaml
---
archunit_test: ArchitectureTest#newRuleName
last_updated: 2025-01-25
---
```

---

## Common Mistakes and Fixes

### Mistake 1: Wrong Dependency Check Method

**❌ WRONG:**
```java
// Checks ALL dependencies (including transitive)
.should().dependOnClassesThat().resideInAPackage("..repository..")
```

**✅ CORRECT:**
```java
// For field injection check
fields()
    .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
    .should().haveRawType(simpleNameEndingWith("Service"))
```

**Why:** `dependOnClassesThat()` includes transitive dependencies (Controller → DTO → Entity). Use `fields()` for direct injection checks.

---

### Mistake 2: Hardcoded Strings Instead of Constants

**❌ WRONG:**
```java
.haveSimpleNameEndingWith("Controller")  // Hardcoded
.definedBy("..service..")                 // Hardcoded
```

**✅ CORRECT:**
```java
// Reuse existing constants
private static final String LAYER_CONTROLLER = "Controller";
private static final String LAYER_SERVICE = "Service";

.haveSimpleNameEndingWith(LAYER_CONTROLLER)
.definedBy("..service..")  // OK for package patterns
```

**Why:** Maintains consistency with existing tests and follows DRY principle.

---

### Mistake 3: Missing `.because()` Clause

**❌ WRONG:**
```java
@ArchTest
static final ArchRule managerNoService =
    noClasses().that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("..service..");
// Missing .because()
```

**✅ CORRECT:**
```java
@ArchTest
static final ArchRule managerNoService =
    noClasses().that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("..service..")
        .because("Manager layer prohibits calling Service layer (rule: 09-manager-layer.md)");
```

**Why:** `.because()` clause documents WHY the rule exists and traces back to source documentation.

---

### Mistake 4: Overly Broad Package Patterns

**❌ WRONG:**
```java
// Catches framework classes too
.resideInAnyPackage("..service..")
```

**✅ CORRECT:**
```java
// Specific to business code
.resideInAnyPackage("net.lab1024.sa.admin..service..")
```

**Why:** Broad patterns catch framework classes (e.g., MyBatis-Plus ServiceImpl). Use full package path for business rules.

---

### Mistake 5: No Validation Step

**❌ WRONG:**
```java
// Generate test and assume it works
agent: "I've added the test method to ArchitectureTest.java"
```

**✅ CORRECT:**
```java
// Generate test → Verify compilation → Create violation example → Confirm test catches it
./gradlew test --tests ArchitectureTest

// Create intentional violation
@RestController
public class TestController {
    @Autowired  // Should be caught by noFieldInjection test
    private UserRepository userRepository;
}

// Verify test fails
./gradlew test --tests ArchitectureTest#noFieldInjection
```

**Why:** Untested rules may have syntax errors or fail to catch violations.

---

## SmartAdmin-Specific Patterns

### 1. Use Existing Layer Constants

```java
private static final String LAYER_CONTROLLER = "Controller";
private static final String LAYER_SERVICE = "Service";
private static final String LAYER_MANAGER = "Manager";
private static final String LAYER_DAO = "Dao";
```

### 2. Standard Package Patterns

```java
// Business code (admin module)
"net.lab1024.sa.admin.."

// Foundation modules
"net.lab1024.sa.foundation.."

// Legacy packages (being deprecated)
"net.lab1024.sa.common.."
```

### 3. Exemption Patterns

Use `.ignoreDependency()` for framework exemptions:

```java
layeredArchitecture()
    .consideringAllDependencies()
    .ignoreDependency(resideInAPackage("..interceptor.."), alwaysTrue())
    .ignoreDependency(simpleNameContaining("MyBatisPlugin"), alwaysTrue())
```

### 4. Bilingual Documentation

SmartAdmin uses Traditional Chinese for comments:

```java
/**
 * 【嚴格執行】Manager 層禁止調用業務 Service 層
 *
 * <p>Manager 只能向下調用 DAO/Mapper，不能向上調用業務 Service
 *
 * <p>規則來源：09-manager-layer.md
 */
```

**English `.because()` clauses are acceptable** for readability in test output.

---

## Validation Checklist

Before completing test generation:

- [ ] Parse YAML frontmatter to check `archunit_test` field
- [ ] No duplicate test for same rule
- [ ] Correct ArchUnit pattern selected (see decision matrix)
- [ ] Uses class constants where applicable
- [ ] `.because()` clause references rule file (e.g., "rule: 09-manager-layer.md")
- [ ] Javadoc explains rule and exemptions
- [ ] Test compiles: `./gradlew :sa-admin:compileTestJava`
- [ ] Test runs: `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] Created violation example to verify test catches bad code
- [ ] Updated rule file frontmatter with `archunit_test` field

---

## Quick Reference: Rule → DSL Mapping

| Natural Language Rule | ArchUnit DSL Entry Point |
|-----------------------|--------------------------|
| "X layer cannot access Y layer" | `layeredArchitecture().whereLayer(X).mayNotAccessLayer(Y)` |
| "@Annotation only in X layer" | `methods().that().areAnnotatedWith(Annotation.class).should().beDeclaredInClassesThat()...` |
| "@Annotation must use parameter=value" | `methods().should(new ArchCondition<>() { check annotation params })` |
| "No field injection" | `fields().should().notBeAnnotatedWith(Autowired.class)` |
| "Classes must end with Suffix" | `classes().that().resideInAPackage("..pkg..").should().haveSimpleNameEndingWith("Suffix")` |
| "Cannot depend on package X" | `noClasses().should().dependOnClassesThat().resideInAPackage("..pkg..")` |
| "Package X deprecated" | `noClasses().that().resideOutsideOfPackage("allowed..").should().dependOnClassesThat().resideInAPackage("deprecated..")` |

---

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "ArchUnit" - ArchUnit test generation or architecture enforcement
- "ArchitectureTest" - Direct reference to ArchitectureTest.java
- "architecture test" - Generate tests for architecture validation
- "add ArchUnit test" - Explicit skill invocation request
- "enforce architecture rule" - Architecture constraint enforcement

**Secondary Keywords** (Medium confidence):
- "layer dependency" - Context: validating layered architecture (Controller → Service → Manager → Dao)
- "@Transactional restriction" - Context: annotation placement validation
- "field injection" - Context: detecting prohibited @Autowired field injection
- "package migration" - Context: deprecated package detection rules
- "naming convention" - Context: class/method naming validation

**Phrase Patterns**:
- "add ArchUnit test for [module/rule]" - Example: "add ArchUnit test for Manager layer"
- "generate test to enforce [constraint]" - Example: "generate test to enforce @Transactional placement"
- "create ArchUnit rule for [pattern]" - Example: "create ArchUnit rule for constructor injection"

**Example User Requests**:
```
User: "Add ArchUnit test to enforce that Manager layer cannot call Service layer"
User: "Generate test for the @Transactional restriction in Manager layer"
User: "I need to validate layered architecture dependencies for the Employee module"
```

**Note**: This skill can also be manually invoked via `/archunit-test-generator` command.

---

**Last Updated:** 2026-01-25 (v1.1 - Added Pattern 7 for annotation parameter verification and time estimates based on real-world testing)

---

## 相關規則

本技能直接關聯以下 SmartAdmin 架構規則：

### 強制要求

- **[Architecture Rules - Complete](./../../../.agent/rules/foundation/10-architecture-rules.md)**
  - 本技能為所有 ArchUnit 測試的生成器
  - 涵蓋全部 21 條架構規則的測試生成
  - 關鍵規則包含：
    - `layeredArchitecture()` - Controller → Service → Manager → Dao 分層架構
    - `transactionalMustBeInManagerLayer()` - @Transactional 僅允許在 Manager 層
    - `serviceUsesVavrOption()` - Service 層必須使用 Vavr Option
    - `noFieldInjection()` - 禁止使用 @Autowired 欄位注入

- **[Manager Layer Rules](./../../../.agent/rules/foundation/09-manager-layer.md)**
  - Manager 層事務管理規則
  - @Transactional 必須包含 `rollbackFor = Throwable.class`
  - Manager 層命名規範（XXXManager 而非 XXXManagerImpl）

- **[Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md)**
  - 類別命名驗證（Controller, Service, Manager, Dao 後綴）
  - 布林欄位命名（`deleted` 不是 `isDeleted`）
  - 包名結構驗證

### 參考指引

- **[Dependency Injection Rules](./../../../.agent/rules/foundation/07-dependency-injection.md)**
  - 構造器注入強制要求（@RequiredArgsConstructor + private final）
  - 禁止欄位注入的 ArchUnit 驗證

- **[Exception Handling](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - 異常處理層級驗證
  - 禁止在 Controller 層使用 try-catch

---

## 參考資料

- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html) - 官方文檔
- [ArchitectureTest.java](./../../../.agent/configs/ArchitectureTest.java) - SmartAdmin 架構測試基準
- [SmartAdmin Architecture Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md)
