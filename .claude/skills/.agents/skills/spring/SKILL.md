---
name: spring
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
- `.agent/rules/09-manager-layer.md`
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
- `.agent/rules/01-naming-conventions.md`

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
- `.agent/rules/10-architecture-rules.md`

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
- `.agent/rules/01-naming-conventions.md`
- `.claude/shared/knowledge/smartadmin-patterns.md#layer-responsibilities`

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
**File**: `sa-admin/.../EmployeeService.java:87`
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
- [Manager Layer Rules](.agent/rules/09-manager-layer.md)
- [Transaction Management](.claude/shared/knowledge/smartadmin-patterns.md#transaction-management)

---

## 📚 Quick Reference

**Run ArchitectureTest:**
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Documentation:**
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)
- [Manager Layer Rules](.agent/rules/09-manager-layer.md)
- [Architecture Rules](.agent/rules/10-architecture-rules.md)
- [Naming Conventions](.agent/rules/01-naming-conventions.md)

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
