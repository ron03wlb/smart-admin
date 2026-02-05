---
trigger: always_on
description: PMD Code Quality Standards
tags: [static-analysis, pmd, code-quality, best-practices]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/F02-oop-principles.md
  - technology/patterns/P05-exception-logging.md
last_updated: 2026-01-22
---

# PMD Standards

**TL;DR**: PMD detects code smells, potential bugs, and best practice violations. Enable `bestpractices` and `errorprone` rulesets, all Priority 1-3 violations must be fixed.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When generating any Java code
- ✅ During Code Review to check code quality
- ✅ When `./gradlew pmdMain` fails
- ✅ User asks about code smell issues

### Mandatory Enforcement Checklist
- [ ] Use interface types instead of implementation types (LooseCoupling)
- [ ] No duplicate string literals (AvoidDuplicateLiterals)
- [ ] No unused variable assignments (UnusedAssignment)
- [ ] Log statements have guard (GuardLogStatement)
- [ ] Serializable classes have serialVersionUID

### AI Decision Tree
```
PMD Violation → Identify Rule Type
  ├─ LooseCoupling
  │   └─ Change HashMap/ArrayList to Map/List
  ├─ AvoidDuplicateLiterals
  │   └─ Extract as constant
  ├─ UnusedAssignment
  │   └─ Remove unused initialization
  ├─ GuardLogStatement
  │   └─ Add log level check or use placeholders
  └─ MissingSerialVersionUID
      └─ Add serialVersionUID field
```

### Error Pattern Detection and Auto-Fix

#### LooseCoupling - Loose Coupling
```java
// ❌ Violation - Using implementation type
public HashMap<String, Object> processData() {
    HashMap<String, Object> result = new HashMap<>();
    return result;
}

// ✅ Fix - Use interface type
public Map<String, Object> processData() {
    Map<String, Object> result = new HashMap<>();
    return result;
}
```

#### AvoidDuplicateLiterals - Avoid Duplicate Literals
```java
// ❌ Violation - String repeated 4+ times
@Select("SELECT * FROM t_user WHERE deleted_flag = 0")
User findOne();
@Select("SELECT * FROM t_user WHERE deleted_flag = 0 AND status = 1")
List<User> findActive();

// ✅ Fix - Extract constant
private static final String DELETED_FLAG = "deleted_flag";
private static final String NOT_DELETED = DELETED_FLAG + " = 0";
```

#### UnusedAssignment - Unused Assignment
```java
// ❌ Violation - Initialization value is overwritten
List<User> users = null;
if (condition) {
    users = findActiveUsers();
} else {
    users = findAllUsers();
}

// ✅ Fix - Remove useless initialization
List<User> users;
if (condition) {
    users = findActiveUsers();
} else {
    users = findAllUsers();
}
```

#### GuardLogStatement - Log Guard
```java
// ❌ Violation - Debug log without guard
log.debug("Processing user: " + user.toString());

// ✅ Fix Option 1 - Use placeholder
log.debug("Processing user: {}", user);

// ✅ Fix Option 2 - Add guard
if (log.isDebugEnabled()) {
    log.debug("Processing user: " + user.toString());
}
```

#### MissingSerialVersionUID
```java
// ❌ Violation - Missing serialVersionUID
public class MenuTreeVO implements Serializable {
    private Long id;
    private String name;
}

// ✅ Fix
public class MenuTreeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
}
```

#### CallSuperInConstructor - Constructor Not Calling super

**Scenario**: Exception classes like BusinessException provide no-arg constructor
**Violation Reason**: PMD requires all constructors to explicitly call super()
**SmartAdmin Decision**: Java defaults to calling parent no-arg constructor, explicit call is redundant
**Solution**:
```java
@SuppressWarnings("PMD.CallSuperInConstructor")
public BusinessException() {
    // empty - default calls parent no-arg constructor
}
```

#### AvoidReassigningParameters - Avoid Reassigning Parameters

**Scenario**: Method parameter needs modification before use
**Violation Reason**: Directly modifying parameter reduces readability
**SmartAdmin Decision**: Must fix, create local variable
**Solution**:
```java
// ❌ Violation
public void process(Integer pageNum) {
    pageNum = pageNum - 1;  // Modifying parameter
    query(pageNum);
}

// ✅ Fix
public void process(Integer pageNum) {
    int adjustedPage = pageNum - 1;  // Local variable
    query(adjustedPage);
}
```

#### ShortClassName - Class Name Too Short

**Scenario**: Static constant grouping classes in utility classes (Dict, Expire, Dept, Support)
**Violation Reason**: Class name less than 5 characters
**SmartAdmin Decision**: As internal constant organization structure is reasonable
**Solution**:
```java
@SuppressWarnings("PMD.ShortClassName")
public static final class Dict {
    public static final String DICT_DATA = "dict_data_cache";
    private Dict() {}
}
```

#### MissingStaticMethodInNonInstantiatableClass - Pure Constants Class

**Scenario**: CacheKeyConst only contains constant definitions, no static methods
**Violation Reason**: PMD expects utility class to have static methods
**SmartAdmin Decision**: Pure constants class is a legitimate design pattern
**Solution**:
```java
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class CacheKeyConst {
    private CacheKeyConst() {}
    // Only constant definitions
}
```

---

## [Mandatory] Enabled Rule Description

### Best Practices Ruleset

| Rule                    | Priority | Description                           |
| ----------------------- | -------- | ------------------------------------- |
| `LooseCoupling`         | P3       | Use interface types not implementation |
| `UnusedAssignment`      | P3       | Avoid unused variable assignments     |
| `GuardLogStatement`     | P2       | Log statements need guard             |
| `UnusedFormalParameter` | P3       | Avoid unused method parameters        |
| `UnusedLocalVariable`   | P3       | Avoid unused local variables          |
| `UnusedPrivateField`    | P3       | Avoid unused private fields           |
| `UnusedPrivateMethod`   | P3       | Avoid unused private methods          |

### Error Prone Ruleset

| Rule                                  | Priority | Description                                      |
| ------------------------------------- | -------- | ------------------------------------------------ |
| `AvoidDuplicateLiterals`              | P3       | String literal repeated ≥4 times needs extraction |
| `MissingSerialVersionUID`             | P3       | Serializable class needs serialVersionUID        |
| `CloseResource`                       | P3       | Resources need proper closing                    |
| `EmptyCatchBlock`                     | P3       | Prohibit empty catch blocks                      |
| `AvoidBranchingStatementAsLastInLoop` | P2       | Avoid break/continue/return at loop end          |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # PMD plugin configuration
```

### build.gradle.kts Configuration
```kotlin
configure<PmdExtension> {
    toolVersion = libs.findVersion("pmd").get().toString()
    isIgnoreFailures = false  // Fail on violation
    ruleSets = listOf(
        "category/java/errorprone.xml",
        "category/java/bestpractices.xml"
    )
}
```

### Custom Rule Exclusion (If Needed)
```kotlin
configure<PmdExtension> {
    ruleSets = emptyList()
    ruleSetFiles = files("config/pmd/custom-rules.xml")
}
```

---

## Common Violation Type Statistics

Based on actual project violation analysis:

| Violation Type                                | Count | Main Location              | Resolution Method    |
| --------------------------------------------- | ----- | -------------------------- | -------------------- |
| `LooseCoupling`                               | ~10   | Controller/Service         | Use interface type   |
| `AvoidDuplicateLiterals`                      | ~8    | DAO/Mapper                 | Extract constant     |
| `UnusedAssignment`                            | ~4    | Service                    | Remove unused init   |
| `GuardLogStatement`                           | ~2    | Manager                    | Use log placeholder  |
| `MissingSerialVersionUID`                     | ~1    | VO/DTO                     | Add serialVersionUID |
| `CallSuperInConstructor`                      | 3     | BusinessException.java     | @SuppressWarnings    |
| `AvoidReassigningParameters`                  | 11    | SmartPageUtil.java         | Create local variable |
| `ShortClassName`                              | 3     | CacheKeyConst.java         | @SuppressWarnings    |
| `MissingStaticMethodInNonInstantiatableClass` | 3     | CacheKeyConst.java         | @SuppressWarnings    |

---

## Verification Commands

```bash
# Gradle
./gradlew pmdMain pmdTest

# View report
open smartadmin-app/build/reports/pmd/main.html

# Check specific module only
./gradlew :smartadmin-app:pmdMain
```

---

## Related Specifications

- [11-checkstyle-rules.md](./11-checkstyle-rules.md) - Code style checking
- [13-spotbugs-rules.md](./13-spotbugs-rules.md) - Bug pattern detection
- [04-exception-logging.md](./04-exception-logging.md) - Logging standards
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
