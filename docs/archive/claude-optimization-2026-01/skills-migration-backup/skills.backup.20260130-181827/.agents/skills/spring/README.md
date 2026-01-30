# SmartAdmin Spring Pattern Checker

> Validates code compliance with SmartAdmin's Spring-specific conventions

## Quick Start

```bash
# Check current git changes
/spring

# Check specific module
/spring sa-admin/src/main/java/net/lab1024/sa/admin/module/system/

# Check specific file
/spring path/to/UserService.java
```

## What It Checks

### 🚨 CRITICAL Rules
1. **@Transactional Placement** - Only in Manager layer (`*Manager.java`)
2. **Dependency Injection** - Constructor injection only (no `@Autowired` fields)
3. **Layered Architecture** - Strict `Controller → Service → Manager → Dao` hierarchy

### ⚠️ HIGH Priority
4. **Spring Bean Naming** - Classes must have proper suffixes (`*Controller`, `*Service`, etc.)

## Example Output

```markdown
## 🔍 SmartAdmin Spring Pattern Check Report

**Scan Summary:**
- Files Scanned: 42
- Violations Found: 8
- CRITICAL: 3 | HIGH: 4 | MEDIUM: 1

### 🚨 CRITICAL - @Transactional in Service Layer

**File**: `EmployeeService.java:87`

**Current**:
```java
@Service
public class EmployeeService {
    @Transactional  // ❌ VIOLATION
    public void updateEmployee() { }
}
```

**Fix**:
```java
// Move to EmployeeManager.java
@Service
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public void updateEmployee() { }
}
```
```

## When It Triggers

**Automatically:**
- When creating/modifying Spring Bean classes (`@Service`, `@Controller`, etc.)
- After `code-reviewer` agent completes
- When `ArchitectureTest.java` fails

**Manually:**
- Run `/spring` command anytime

## Integration with ArchUnit

This skill complements `ArchitectureTest.java`:

| Check | ArchUnit (Compile-time) | /spring (Runtime) |
|-------|-------------------------|-------------------|
| @Transactional placement | ✅ | ✅ |
| Constructor injection | ✅ | ✅ |
| Layer dependencies | ✅ | ✅ |
| Bean naming | ✅ | ✅ |
| Detailed fix suggestions | ❌ | ✅ |
| Before/after code examples | ❌ | ✅ |
| Documentation links | ❌ | ✅ |

**Best practice**: Run both!
```bash
# 1. Run ArchUnit tests
./gradlew :sa-admin:test --tests ArchitectureTest

# 2. If failures, run /spring for detailed fixes
/spring
```

## Architecture Rules

### Rule 1: @Transactional Placement
```java
// ❌ WRONG
@Service
public class UserService {
    @Transactional
    public void update() { }
}

// ✅ CORRECT
@Service
public class UserManager {
    @Transactional(rollbackFor = Throwable.class)
    public void update() { }
}
```

### Rule 2: Dependency Injection
```java
// ❌ WRONG
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}

// ✅ CORRECT
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
}
```

### Rule 3: Layered Calls
```java
// ❌ WRONG - Controller → Manager (skipping Service)
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;  // VIOLATION
}

// ✅ CORRECT - Controller → Service
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // CORRECT
}
```

### Rule 4: Bean Naming
```java
// ❌ WRONG
@RestController
public class User { }

// ✅ CORRECT
@RestController
public class UserController { }
```

## Documentation References

- **[SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)** - Core patterns and conventions
- **[Manager Layer Rules](.agent/foundation/09-manager-layer.md)** - Transaction and cache management
- **[Architecture Rules](.agent/foundation/10-architecture-rules.md)** - Layered architecture enforcement
- **[Naming Conventions](.agent/foundation/01-naming-conventions.md)** - Naming standards

## FAQ

**Q: Why can't I use @Transactional in Service layer?**
A: SmartAdmin enforces explicit transaction boundaries in the Manager layer to prevent hidden transaction nesting and ensure proper rollback scope. See [Manager Layer Rules](.agent/foundation/09-manager-layer.md).

**Q: Why constructor injection instead of field injection?**
A: Constructor injection ensures immutability, makes dependencies explicit, enables easier testing, and prevents circular dependency issues. See [Dependency Injection](.claude/shared/knowledge/smartadmin-patterns.md#dependency-injection).

**Q: Can Service call another Service?**
A: No. SmartAdmin prohibits Service→Service calls to prevent tight coupling. If multiple Services are needed, create a Manager to orchestrate them. See [Architecture Rules](.agent/foundation/10-architecture-rules.md).

**Q: Does this skill modify my code?**
A: No. This skill is **read-only** - it only provides violation reports and fix suggestions. You review and apply fixes manually.

**Q: How is this different from ArchitectureTest?**
A: ArchUnit provides compile-time validation with pass/fail results. This skill provides runtime analysis with detailed fix suggestions, before/after code examples, and documentation links.

## Version

- **Version**: 1.0.0
- **Last Updated**: 2026-01-23
- **Compatible with**: SmartAdmin v4.0.0+
- **ArchUnit Version**: Latest (enforced by ArchitectureTest.java)

## License

Part of SmartAdmin framework - see project LICENSE for details.
