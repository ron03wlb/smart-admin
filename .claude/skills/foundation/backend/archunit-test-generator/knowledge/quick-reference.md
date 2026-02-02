# ArchUnit Test Generator - Quick Reference

**Version**: 1.0.0  
**Last Updated**: 2026-02-02  
**Skill**: archunit-test-generator (P0 - Critical)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Example |
|---------|---------|---------|
| Compile Tests | Verify syntax without running | ./gradlew :sa-admin:compileTestJava |
| Run All Tests | Execute ArchitectureTest suite | ./gradlew :sa-admin:test --tests ArchitectureTest |
| Run Specific Test | Execute single test method | ./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection |
| Search Tests | Find existing test methods | grep -n "static final ArchRule" ArchitectureTest.java |
| Check Rule Coverage | List archunit_test fields | grep -r "archunit_test:" .agent/rules/ |

### Rapid Development Workflow

Time estimate: 10-15 minutes per rule

| Step | Command | Time |
|------|---------|------|
| 1. Parse rule | Read .agent/rules/{rule-file}.md frontmatter | ~1 min |
| 2. Map DSL | Select ArchUnit pattern from decision matrix | ~2 min |
| 3. Check existing | grep "static final ArchRule" ArchitectureTest.java | ~1 min |
| 4. Generate test | Write method with correct style | ~3 min |
| 5. Verify | ./gradlew test --tests ArchitectureTest | ~3 min |

---

## 7 ArchUnit DSL Patterns (Decision Matrix)

### Pattern 1: Layer Dependencies

**Trigger**: "Controller → Service", "may only access"

**Quick Template**:
layeredArchitecture()
    .whereLayer(X).mayOnlyBeAccessedByLayers(Y)

**Example**:
Manager layer only accessed by Service layer

---

### Pattern 2: Annotation Restrictions

**Trigger**: "@Transactional only in", "@Cacheable"

**Quick Template**:
methods()
    .that().areAnnotatedWith(Annotation.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")

**Example**:
@Transactional only in Manager layer

---

### Pattern 3: Annotation Parameters

**Trigger**: "rollbackFor = Throwable.class", "specific parameter value"

**Quick Template**:
methods()
    .that().areAnnotatedWith(Transactional.class)
    .should(new ArchCondition<JavaMethod>("use rollbackFor = Throwable.class") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            Transactional annotation = method.getAnnotationOfType(Transactional.class);
            // Validate parameter values
        }
    })

**Example**:
@Transactional must use rollbackFor = Throwable.class

---

### Pattern 4: Package Prohibitions

**Trigger**: "cannot call", "prohibit", "should not access"

**Quick Template**:
noClasses()
    .that().resideInAPackage("..manager..")
    .should().dependOnClassesThat().resideInAPackage("..service..")

**Example**:
Manager cannot call Service layer

---

### Pattern 5: Field Injection Prohibition

**Trigger**: "field injection", "@Autowired"

**Quick Template**:
fields()
    .that().areDeclaredInClassesThat().resideInAnyPackage("..controller..", "..service..")
    .should().notBeAnnotatedWith(Autowired.class)

**Example**:
No @Autowired field injection (use constructor injection)

---

### Pattern 6: Naming Conventions

**Trigger**: "must end with", "naming pattern"

**Quick Template**:
classes()
    .that().resideInAPackage("..controller..")
    .should().haveSimpleNameEndingWith("Controller")

**Example**:
Controller classes must end with "Controller"

---

### Pattern 7: Package Migration

**Trigger**: "deprecated package", "use instead"

**Quick Template**:
noClasses()
    .that().resideOutsideOfPackage("allowed..")
    .should().dependOnClassesThat().resideInAnyPackage("deprecated..")

**Example**:
No new code should use legacy common.* packages

---

## SmartAdmin Constants (Always Use These)

### Layer Constants

private static final String LAYER_CONTROLLER = "Controller";
private static final String LAYER_SERVICE = "Service";
private static final String LAYER_MANAGER = "Manager";
private static final String LAYER_DAO = "Dao";

### Package Patterns

// Business code (admin module)
"net.lab1024.sa.admin.."

// Foundation modules
"net.lab1024.sa.foundation.."

// Legacy packages (being deprecated)
"net.lab1024.sa.common.."

---

## Common Errors and Quick Fixes

### Error 1: Compilation Failure

**Error Message**:
error: cannot find symbol: method beDeclaredInClassesThat()

**Cause**: Missing ArchUnit import
**Fix**:
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

---

### Error 2: Test Catches Too Much

**Error Message**:
Architecture Violation: Class <com.baomidou.mybatisplus.extension.service.IService> is accessed by ...

**Cause**: Package pattern too broad
**Fix**: Use specific package path
// ❌ BAD
.resideInAnyPackage("..service..")

// ✅ GOOD
.resideInAnyPackage("net.lab1024.sa.admin..service..")

---

### Error 3: Test Doesn't Catch Violation

**Error Message**: Test passes but violation exists

**Cause**: Wrong DSL method
**Fix**: Check decision matrix for correct pattern
// For checking field injection
fields().that().areDeclaredInClassesThat()...  // ✅ CORRECT

// NOT
noClasses().should().dependOnClassesThat()...  // ❌ WRONG (transitive)

---

### Error 4: Missing .because() Clause

**Error Message**: (No error, but violates SmartAdmin standards)

**Fix**: ALWAYS add .because() with rule file reference
.because("@Transactional must only be used in Manager layer (rule: 09-manager-layer.md)")

---

### Error 5: Hardcoded Strings

**Error Message**: (No error, but violates DRY principle)

**Fix**: Use existing constants
// ❌ BAD
.haveSimpleNameEndingWith("Controller")

// ✅ GOOD
.haveSimpleNameEndingWith(LAYER_CONTROLLER)

---

## Test Method Template (Copy-Paste Ready)

/**
 * 【嚴格執行】{Rule Description}
 *
 * <p>{Detailed Explanation}
 *
 * <p>Exemptions (if any):
 * <ul>
 *   <li>{Exemption 1} - {Reason}
 * </ul>
 *
 * <p>Rule Source: {rule-file}.md
 */
@ArchTest
static final ArchRule {ruleName} =
    {ArchUnitDSL}
        .because("{Rule summary} (rule: {rule-file}.md)");

---

## Validation Checklist (Copy-Paste)

Before completing test generation:

- [ ] Parse YAML frontmatter to check archunit_test field
- [ ] No duplicate test for same rule
- [ ] Correct ArchUnit pattern selected (see decision matrix)
- [ ] Uses class constants where applicable
- [ ] .because() clause references rule file
- [ ] Javadoc explains rule and exemptions
- [ ] Test compiles: ./gradlew :sa-admin:compileTestJava
- [ ] Test runs: ./gradlew :sa-admin:test --tests ArchitectureTest
- [ ] Created violation example to verify test catches bad code
- [ ] Updated rule file frontmatter with archunit_test field

---

## Rule File Frontmatter Template

---
trigger: always_on
description: {Rule Description}
tags: [architecture, {category}, {subcategory}]
archunit_test: ArchitectureTest#{testMethodName}
last_updated: 2026-02-02
---

---

## Time Estimates (Production Data)

| Task | Time | Breakdown |
|------|------|-----------|
| Parse rule file | 1 min | Read YAML frontmatter |
| Map to DSL | 2 min | Decision matrix lookup |
| Check existing tests | 1 min | grep for duplicates |
| Generate test method | 3 min | Write code + docs |
| Verify compilation | 1 min | ./gradlew compileTestJava |
| Create violation example | 2 min | Write bad code |
| Verify test catches violation | 2 min | Run test, confirm failure |
| **Total** | **10-12 min** | **Per rule** |

**Workflow < 3 min** (steps 1-4)  
**Verification ~7 min** (steps 5-7)

---

**See Also**:
- [Patterns Guide](patterns.md) - Detailed pattern explanations
- [Examples Guide](examples.md) - Real SmartAdmin test cases
- [Best Practices](best-practices.md) - Advanced techniques
- [Troubleshooting](troubleshooting.md) - Deep-dive error resolution
