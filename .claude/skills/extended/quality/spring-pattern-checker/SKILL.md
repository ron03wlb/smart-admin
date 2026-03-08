---
name: spring-pattern-checker
description: SmartAdmin Spring Pattern Checker - Validates code compliance with SmartAdmin's Spring-specific conventions (@Transactional placement, dependency injection, layered architecture calls, Spring Bean naming). Use when creating/modifying Spring Bean classes, during code review, or when ArchitectureTest fails. Triggers on @Component, @Service, @Controller, @RestController annotations or explicit /spring command.
---

# SmartAdmin Spring Pattern Checker

Validates code compliance with SmartAdmin's strict Spring Framework conventions enforced by `ArchitectureTest.java`.

## When to Use

**Automatic triggers:**
- Creating/modifying classes with `@Component`, `@Service`, `@Controller`, `@RestController`, `@Repository`
- After code-reviewer agent completes (as Spring-specific follow-up)
- When `ArchitectureTest.java` execution fails
- Before committing changes to Spring Bean classes

**Manual trigger:**
```bash
/spring                    # Check current changes
/spring path/to/files      # Check specific files
```

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "spring pattern" - Spring pattern validation
- "spring validation" - Validate Spring code compliance
- "transactional placement" - Validate @Transactional placement rules
- "dependency injection" - Check dependency injection patterns
- "architecture compliance" - SmartAdmin architecture compliance

**Secondary Keywords** (Medium confidence):
- "field injection" - Context: detect field injection anti-patterns
- "constructor injection" - Context: validate constructor injection usage
- "@Transactional" - Context: annotation placement validation
- "@Cacheable" - Context: cacheable annotation placement
- "layered architecture" - Context: layer dependency validation
- "Spring Bean naming" - Context: bean naming convention validation

**Phrase Patterns**:
- "Check Spring patterns in [component]" - Example: "Check Spring patterns in Manager layer"
- "Validate [Spring rule]" - Example: "Validate @Transactional placement"
- "Detect [anti-pattern]" - Example: "Detect field injection in Service layer"

**Example User Requests**:
```
User: "Check Spring patterns in the Employee module"
User: "Validate @Transactional placement in Manager layer"
User: "Detect field injection anti-patterns"
User: "Verify layered architecture compliance"
User: "Check dependency injection patterns"
```

**Automatic Activation** (Agent-triggered):
- After `code-reviewer` agent completes (Spring-specific follow-up)
- When `ArchitectureTest.java` execution fails
- When modifying Spring Bean classes (`@Component`, `@Service`, `@Controller`, `@Repository`)

**Note**: This skill can also be manually invoked via `/spring-pattern-checker` or `/spring` command.

---

## Related Skills & Boundaries

**互補 Skills**:
- `concurrency-safety-auditor` - 驗證執行緒安全和並發模式
- `archunit-test-generator` - 生成架構強制測試

**邊界澄清**:
- **本 skill 專注**: Spring 框架合規性（分層、註解、依賴注入）
- **concurrency-safety-auditor 專注**: 並發原語和執行緒安全
- **無重疊**: 這些 skills 處理正交關注點，應同時使用以進行全面質量檢查

---

## Core Rules

SmartAdmin enforces 4 critical Spring patterns validated by this skill:

### Rule 1: @Transactional Placement
**CRITICAL**: `@Transactional` ONLY in Manager layer

```java
// ❌ WRONG - Service layer
@Service
public class UserService {
    @Transactional  // VIOLATION
    public void updateUser() { }
}

// ❌ WRONG - Controller layer
@RestController
public class UserController {
    @Transactional  // VIOLATION
    public void deleteUser() { }
}

// ✅ CORRECT - Manager layer only
@Service
public class UserManager {
    @Transactional(rollbackFor = Throwable.class)  // CORRECT
    public void updateUser() { }
}
```

**Why**: Transaction boundaries must be explicit and managed in a dedicated layer to prevent hidden transaction nesting and ensure proper rollback scope.

**References**:
- `.agent/foundation/F03-manager-layer.md`
- `.claude/shared/knowledge/smartadmin-patterns.md#transaction-management`

### Rule 2: Dependency Injection Style
**CRITICAL**: Constructor injection ONLY via `@RequiredArgsConstructor`

```java
// ❌ WRONG - Field injection
@Service
public class UserService {
    @Autowired  // VIOLATION
    private UserDao userDao;
}

// ❌ WRONG - Setter injection
@Service
public class UserService {
    private UserDao userDao;

    @Autowired  // VIOLATION
    public void setUserDao(UserDao dao) {
        this.userDao = dao;
    }
}

// ✅ CORRECT - Constructor injection
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class UserService {
    private final UserDao userDao;  // CORRECT
    private final UserManager userManager;
}
```

**Why**: Constructor injection ensures immutability, makes dependencies explicit, enables easier testing, and prevents circular dependency issues.

**References**:
- `.claude/shared/knowledge/smartadmin-patterns.md#dependency-injection`
- `.agent/foundation/F01-naming-conventions.md`

### Rule 3: Layered Architecture Calls
**CRITICAL**: Strict call hierarchy enforcement

\`\`\`
Controller → Service → Manager → Dao
\`\`\`

**Violations:**

```java
// ❌ VIOLATION 1: Controller → Manager (skipping Service)
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;  // VIOLATION

    @PostMapping("/users")
    public ResponseDTO<Void> add() {
        return userManager.createUser();  // Must call Service
    }
}

// ❌ VIOLATION 2: Service → Service
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;  // VIOLATION
    private final ProductService productService;  // VIOLATION
}

// ❌ VIOLATION 3: Manager → Service
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserService userService;  // VIOLATION - Manager cannot call Service
}
```

**Correct patterns:**

```java
// ✅ CORRECT: Controller → Service
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // CORRECT
}

// ✅ CORRECT: Service → Manager OR Dao
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;  // CORRECT
    private final UserDao userDao;  // CORRECT (if no Manager needed)
}

// ✅ CORRECT: Manager → Dao ONLY
@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserDao userDao;  // CORRECT
    private final RoleDao roleDao;  // CORRECT
}
```

**Why**: Enforces separation of concerns, prevents circular dependencies, ensures transaction boundaries are clear, and maintains testability.

**References**:
- `.claude/shared/knowledge/smartadmin-patterns.md#mandatory-layered-architecture`
- `.agent/foundation/F04-architecture-rules.md`

### Rule 4: Spring Bean Naming Conventions
**HIGH**: Class names must match annotation type

```java
// ❌ WRONG - Missing suffix
@RestController
public class User { }  // VIOLATION: Must be UserController

@Service
public class Employee { }  // VIOLATION: Must be EmployeeService or EmployeeManager

// ✅ CORRECT - Proper suffixes
@RestController
public class UserController { }  // CORRECT

@Service
public class EmployeeService { }  // CORRECT

@Service
public class EmployeeManager { }  // CORRECT
```

**Naming rules:**
- `@RestController` → `*Controller`
- `@Service` (business logic) → `*Service`
- `@Service` (transaction/cache) → `*Manager`
- `@Repository` / MyBatis Plus Mapper → `*Dao`

**References**:
- `.agent/foundation/F01-naming-conventions.md`
- `.claude/shared/knowledge/smartadmin-patterns.md#layer-responsibilities`

### Rule 5: Logging Best Practices (v4.1.0)
**MEDIUM**: SLF4J placeholder style, no guard checks

```java
// ✅ CORRECT - SLF4J placeholder (lazy evaluation)
log.info("Processing user: id={}, name={}", user.getId(), user.getName());
log.error("Operation failed: context={}", context, exception);

// ❌ WRONG - Guard check (unnecessary with SLF4J)
if (log.isInfoEnabled()) {
    log.info("Processing user: {}", user);
}

// ❌ WRONG - String concatenation
log.info("Processing user: " + user.toString());
```

**Why**: SLF4J placeholders provide lazy evaluation - string formatting only occurs when the log level is enabled. Guard checks are redundant and add unnecessary code complexity.

**SmartAdmin Decision**: PMD `GuardLogStatement` rule is **excluded** in `config/pmd/ruleset.xml`

**References**:
- `.agent/rules/technology/patterns/P05-exception-logging.md`
- `.agent/rules/quality-tools/Q02-pmd-rules.md`

## Implementation Steps

When `/spring` is invoked:

**Step 1: Detect Scope**
```
If user provides path → Check that path
Else → Check git modified files (git diff --name-only)
```

**Step 2: Find Spring Beans**
```
Grep for @Service, @Controller, @RestController, @Repository, @Component
Filter to *.java files only
```

**Step 3: Analyze Files**
```
For each file:
  Read file content
  Determine layer (by class name suffix)
  Check Rule 1: @Transactional placement
  Check Rule 2: @Autowired usage
  Check Rule 3: Injected dependencies match layer rules
  Check Rule 4: Class naming conventions
  Collect violations
```

**Step 4: Generate Report**
```
Sort violations by severity (CRITICAL > HIGH > MEDIUM > LOW)
For each violation:
  - Show file path and line number
  - Display current code snippet
  - Provide fix suggestion with before/after
  - Link to SmartAdmin documentation
```

**Step 5: Summary**
```
Provide actionable next steps:
  - Immediate fixes (CRITICAL)
  - Before merge (HIGH)
  - Optional improvements (MEDIUM/LOW)
  - Command to run ArchitectureTest
```

## Report Format

```markdown
## 🔍 SmartAdmin Spring Pattern Check Report

**Scan Summary:**
- Files Scanned: 42
- Violations Found: 8
- CRITICAL: 3 | HIGH: 4 | MEDIUM: 1

---

### 🚨 CRITICAL Violations

#### 1. @Transactional in Service Layer
**File**: `smartadmin-modules/smartadmin-business/.../EmployeeService.java:87`
**Severity**: CRITICAL
**Rule**: @Transactional ONLY in Manager layer

**Current Code**:
\`\`\`java
@Service
public class EmployeeService {
    @Transactional  // ❌ VIOLATION
    public void updateEmployee() { }
}
\`\`\`

**Fix**:
\`\`\`java
// Move to EmployeeManager.java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public void updateEmployee() { }
}
\`\`\`

**References**:
- [Manager Layer Rules](.agent/foundation/F03-manager-layer.md)
- [Transaction Management](.claude/shared/knowledge/smartadmin-patterns.md#transaction-management)

---

## 📚 Quick Reference

**Run ArchitectureTest:**
```bash
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

**Documentation:**
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)
- [Manager Layer Rules](.agent/foundation/F03-manager-layer.md)
- [Architecture Rules](.agent/foundation/F04-architecture-rules.md)
- [Naming Conventions](.agent/foundation/F01-naming-conventions.md)

**Detailed References** (read when needed):
- `references/spring-rules-detailed.md` - Complete rule specifications
- `references/quick-fix-guide.md` - Fast fix templates
```

## Notes

- **Safe by default**: This skill NEVER modifies code - only provides suggestions
- **Complementary to ArchUnit**: Works alongside `ArchitectureTest.java`
- **Context-aware**: Understands SmartAdmin-specific patterns
- **Actionable**: Every violation includes concrete fix examples
- **Educational**: Links to detailed documentation

## Version

**Skill Version**: 1.0.0
**Last Updated**: 2026-01-23
**Compatible with**: SmartAdmin v4.0.0+

---

## 相關規則

本技能驗證以下 SmartAdmin 架構規則的實際代碼實現：

### 強制要求

- **[Architecture Rules - Transaction Management](./../../../.agent/rules/foundation/F04-architecture-rules.md#transactionalMustBeInManagerLayer)**
  - @Transactional 僅允許在 Manager 層
  - 本技能檢測 Service 層的 @Transactional 違規（ArchUnit 補充）
  - 檢測缺少 `rollbackFor = Throwable.class` 參數

- **[Manager Layer Rules](./../../../.agent/rules/foundation/F03-manager-layer.md)**
  - Manager 層事務管理職責
  - @Transactional 必須包含 rollbackFor 參數
  - Service → Manager 重構指引

- **[Dependency Injection Rules](./../../../.agent/rules/foundation/F04-architecture-rules.md)**
  - 禁止 @Autowired 欄位注入
  - 強制使用構造器注入（@RequiredArgsConstructor）
  - 本技能檢測欄位注入違規

### 參考指引

- **[Exception Handling](./../../../.agent/rules/technology/patterns/04-exception-logging.md)**
  - Controller 層禁止 try-catch（應由全局異常處理器）
  - 本技能檢測 Controller 異常處理違規

- **[SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md)**
  - Transaction Management Pattern
  - Dependency Injection Pattern

---

## 參考資料

- [ArchitectureTest.java](./../../../.agent/configs/ArchitectureTest.java) - 補充 ArchUnit 靜態測試
- [Manager Layer Extraction Guide](./../../productivity/refactoring/smartadmin-manager-extractor/) - 自動重構工具
- [Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction.html) - 事務管理官方文檔
