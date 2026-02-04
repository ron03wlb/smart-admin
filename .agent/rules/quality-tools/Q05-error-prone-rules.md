---
trigger: always_on
description: Error Prone Compile-Time Check Standards
tags: [static-analysis, error-prone, compile-time-check]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/F02-oop-principles.md
  - technology/patterns/P04-concurrency-rules.md
last_updated: 2025-01-21
---

# Error Prone Standards

**TL;DR**: Error Prone is a Google-developed Java compiler plugin that detects common bug patterns at compile time and provides fix suggestions.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When generating any Java code
- ✅ When compilation fails with Error Prone errors
- ✅ User asks about compile-time bug detection

### Mandatory Enforcement Checklist
- [ ] No `==` for string comparison (use `equals()`)
- [ ] No ignored return values from method calls
- [ ] No format string parameter count mismatch
- [ ] No unused variables/parameters
- [ ] No unsafe type casts

### AI Decision Tree
```
Error Prone Error → Identify Bug Type
  ├─ StringEquality
  │   └─ Use equals() instead of ==
  ├─ ReturnValueIgnored
  │   └─ Handle or assign return result
  ├─ FormatString
  │   └─ Fix format parameter count
  └─ UnusedVariable
      └─ Remove or use the variable
```

### Error Pattern Detection and Auto-Fix

#### StringEquality - String Comparison
```java
// ❌ Violation - Compilation error
if (status == "ACTIVE") {
    // ...
}

// ✅ Fix
if ("ACTIVE".equals(status)) {
    // ...
}

// ✅ Or use Objects.equals
if (Objects.equals(status, "ACTIVE")) {
    // ...
}
```

#### ReturnValueIgnored - Ignored Return Value
```java
// ❌ Violation - Compilation warning/error
stringBuilder.append("text");  // StringBuilder.append returns this
file.delete();                  // File.delete returns boolean

// ✅ Fix
stringBuilder = stringBuilder.append("text");
// Or method chaining
stringBuilder.append("text").append("more");

// For delete
if (!file.delete()) {
    log.warn("Failed to delete file: {}", file);
}
```

#### FormatString - Format String
```java
// ❌ Violation - Parameter count mismatch
String.format("User: %s, Age: %d", name);  // Missing age parameter
log.info("Processing {} with status {}", id);  // Missing status parameter

// ✅ Fix
String.format("User: %s, Age: %d", name, age);
log.info("Processing {} with status {}", id, status);
```

#### UnusedVariable - Unused Variable
```java
// ❌ Violation
public void process(String data) {
    int count = 0;  // Unused
    // ...
}

// ✅ Fix - Remove unused variable
public void process(String data) {
    // ...
}

// ✅ Or use @SuppressWarnings
@SuppressWarnings("UnusedVariable")
public void process(String data) {
    int count = 0;  // Intentionally kept
    // ...
}
```

---

## [Mandatory] Enabled Rule Description

### Default Error Level

| Bug Pattern          | Description                | Level   |
| -------------------- | -------------------------- | ------- |
| `StringEquality`     | Using == to compare strings | ERROR   |
| `ReturnValueIgnored` | Ignoring important return values | WARNING |
| `FormatString`       | Format parameter mismatch   | ERROR   |
| `MissingOverride`    | Missing @Override           | WARNING |
| `UnusedVariable`     | Unused variable             | WARNING |
| `UnusedMethod`       | Unused private method       | WARNING |
| `FallThrough`        | Switch statement fall-through | WARNING |
| `EqualsHashCode`     | equals/hashCode mismatch    | ERROR   |

### Security-Related Rules

| Bug Pattern          | Description             | Level   |
| -------------------- | ----------------------- | ------- |
| `InsecureCipherMode` | Insecure cipher mode    | ERROR   |
| `BadShiftAmount`     | Incorrect shift amount  | ERROR   |
| `ArrayToString`      | Array direct toString   | WARNING |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # Error Prone plugin configuration
```

### build.gradle.kts Configuration
```kotlin
plugins {
    alias(libs.plugins.errorprone) apply false
}

subprojects {
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        "errorprone"(libs.findLibrary("error-prone-core").get())
    }
}
```

### Version Configuration (gradle/libs.versions.toml)
```toml
[versions]
errorprone = "2.36.0"
errorprone-plugin = "4.1.0"

[libraries]
error-prone-core = { module = "com.google.errorprone:error_prone_core", version.ref = "errorprone" }

[plugins]
errorprone = { id = "net.ltgt.errorprone", version.ref = "errorprone-plugin" }
```

### Custom Error Level
```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // Upgrade warning to error
        error("ReturnValueIgnored")

        // Downgrade error to warning
        warn("UnusedVariable")

        // Disable specific check
        disable("MissingSummary")
    }
}
```

---

## Suppressing Warnings

### Method Level
```java
@SuppressWarnings("ReturnValueIgnored")
public void processFile(File file) {
    file.delete();  // Intentionally ignore return value
}
```

### Line Level
```java
@SuppressWarnings("StringEquality")
public boolean isSameInstance(String a, String b) {
    return a == b;  // Intentionally compare reference
}
```

---

## Error Prone vs SpotBugs

| Feature         | Error Prone    | SpotBugs           |
| --------------- | -------------- | ------------------ |
| **Timing**      | Compile-time   | Post-compilation   |
| **Analysis**    | Source analysis | Bytecode analysis  |
| **Speed**       | Fast (compile) | Slow (separate step) |
| **Rule Count**  | ~500           | ~450               |
| **Security**    | Fewer          | FindSecBugs rich   |

**Recommendation**: Use both complementarily
- Error Prone: Quick compile-time feedback
- SpotBugs: Deeper security and bug analysis

---

## Verification Commands

```bash
# Error Prone runs automatically during compilation
./gradlew compileJava

# View Error Prone warnings in compilation output
./gradlew compileJava --warning-mode all

# If separate run needed (usually not necessary)
./gradlew check
```

---

## Related Specifications

- [13-spotbugs-rules.md](./13-spotbugs-rules.md) - SpotBugs bug detection
- [02-oop-principles.md](./02-oop-principles.md) - OOP standards
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
