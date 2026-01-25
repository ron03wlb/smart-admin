---
trigger: always_on
description: Spotless Auto-Formatting Standards
tags: [static-analysis, spotless, code-formatting]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/01-naming-conventions.md
  - rules/11-checkstyle-rules.md
last_updated: 2025-01-21
---

# Spotless Standards

**TL;DR**: Spotless auto-formats code using Google Java Format standard. `spotlessApply` runs automatically before compilation, ensuring code style consistency.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When generating any Java code
- ✅ Before committing code
- ✅ When `./gradlew spotlessCheck` fails
- ✅ User asks about code formatting issues

### Mandatory Enforcement Checklist
- [ ] Use Google Java Format style
- [ ] Remove unused imports
- [ ] No trailing whitespace at line end
- [ ] File ends with newline
- [ ] Run `spotlessApply` before committing

### AI Decision Tree
```
Spotless Issue → Identify Problem Type
  ├─ Format not compliant with Google Style
  │   └─ Run ./gradlew spotlessApply
  ├─ Unused imports
  │   └─ Auto-remove (removeUnusedImports)
  ├─ Trailing whitespace
  │   └─ Auto-remove (trimTrailingWhitespace)
  └─ Missing newline at end of file
      └─ Auto-add (endWithNewline)
```

### Formatting Rules

#### Google Java Format Style
```java
// ✅ Google Java Format
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}
```

**Characteristics**:
- Indentation: 2 spaces
- Line width: 100 characters
- Braces: K&R style
- Spaces: Space around operators

---

## [Mandatory] Feature Description

| Feature                    | Description                        |
| -------------------------- | ---------------------------------- |
| `googleJavaFormat()`       | Apply Google Java Format style     |
| `removeUnusedImports()`    | Remove unused import statements    |
| `trimTrailingWhitespace()` | Remove trailing whitespace         |
| `endWithNewline()`         | Ensure file ends with newline      |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # Spotless plugin configuration
```

### build.gradle.kts Configuration
```kotlin
// Plugin import
plugins {
    alias(libs.plugins.spotless) apply false
}

// Subproject configuration
subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()              // Google Java Format
            removeUnusedImports()           // Remove unused imports
            trimTrailingWhitespace()        // Remove trailing whitespace
            endWithNewline()                // End file with newline
        }
    }
}

// Auto-format before compilation
tasks.withType<JavaCompile> {
    dependsOn("spotlessApply")
}
```

### Version Configuration (gradle/libs.versions.toml)
```toml
[versions]
spotless = "6.25.0"

[plugins]
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

---

## Git Hooks Integration

### Pre-commit Hook
```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "Running Spotless in smart-admin-api-java21-springboot3..."
cd smart-admin-api-java21-springboot3 && ./gradlew spotlessApply

# Check for changes
if ! git diff --quiet; then
    echo "Spotless made changes. Please review and re-commit."
    exit 1
fi
```

### Auto-Install Git Hooks
Configured in build.gradle.kts:
```kotlin
tasks.register("installGitHooks") {
    doLast {
        // Auto-install pre-commit hook
    }
}

tasks.named("build") {
    dependsOn("installGitHooks")
}
```

---

## Verification Commands

```bash
# Check format (does not modify files)
./gradlew spotlessCheck

# Auto-format
./gradlew spotlessApply

# Format Java files only
./gradlew spotlessJavaApply

# See which files will be formatted
./gradlew spotlessJavaCheck --info
```

---

## Spotless vs Checkstyle

| Feature          | Spotless          | Checkstyle      |
| ---------------- | ----------------- | --------------- |
| **Purpose**      | Auto-formatting   | Style checking  |
| **Fix**          | Auto-fix          | Report only     |
| **Timing**       | Before compilation | After compilation |
| **Scope**        | Format/whitespace/imports | Naming/logic/style |

**Recommendation**:
- Run `spotlessApply` first to auto-fix format issues
- Then run `checkstyleMain` to check other style issues

---

## Related Specifications

- [11-checkstyle-rules.md](./11-checkstyle-rules.md) - Checkstyle style checking
- [01-naming-conventions.md](./01-naming-conventions.md) - Naming conventions
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
