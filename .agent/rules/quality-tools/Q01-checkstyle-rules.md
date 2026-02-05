---
trigger: always_on
description: Checkstyle Code Style Standards
tags: [static-analysis, checkstyle, code-style]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/F01-naming-conventions.md
checkstyle_rule: TypeName,MethodName,ConstantName,AvoidStarImport
last_updated: 2025-01-21
---

# Checkstyle Standards

**TL;DR**: Checkstyle enforces code style consistency, including naming conventions, whitespace, braces, and import rules. All violations must be fixed before committing.

---

## 🤖 AI Directive Block

### When to Apply This Rule
- ✅ When generating any Java code
- ✅ During Code Review to check code style
- ✅ When `./gradlew checkstyleMain` fails
- ✅ User asks about code formatting issues

### Mandatory Enforcement Checklist
- [ ] Prohibit `import *` star imports
- [ ] One top-level class per file
- [ ] Opening brace `{` does not break line (K&R style)
- [ ] Whitespace around operators
- [ ] Class names UpperCamelCase, method names lowerCamelCase
- [ ] Package names all lowercase

### AI Decision Tree
```
Checkstyle Error → Identify Rule Type
  ├─ AvoidStarImport
  │   └─ Expand to specific class imports
  ├─ WhitespaceAround
  │   └─ Add spaces around operators/keywords
  ├─ NeedBraces
  │   └─ Add braces (even for single line)
  ├─ LeftCurly / RightCurly
  │   └─ Adjust brace position
  └─ Naming Rules (TypeName/MethodName/...)
      └─ Rename according to specification
```

### Error Pattern Detection and Auto-Fix

#### AvoidStarImport
```java
// ❌ Violation
import java.util.*;
import org.springframework.web.bind.annotation.*;

// ✅ Fix
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
```

#### WhitespaceAround
```java
// ❌ Violation
if(condition){
int x=1+2;
}

// ✅ Fix
if (condition) {
    int x = 1 + 2;
}
```

#### NeedBraces
```java
// ❌ Violation
if (condition)
    doSomething();

// ✅ Fix
if (condition) {
    doSomething();
}
```

#### LeftCurly (K&R Style)
```java
// ❌ Violation
public void method()
{
    // ...
}

// ✅ Fix
public void method() {
    // ...
}
```

---

## [Mandatory] Enabled Rule Description

| Rule                           | Description                         | Example                               |
| ------------------------------ | ----------------------------------- | ------------------------------------- |
| `AvoidStarImport`              | Prohibit star imports               | `import java.util.*` ❌                |
| `OneTopLevelClass`             | One top-level class per file        | -                                     |
| `NoLineWrap`                   | Prohibit package/import line break  | -                                     |
| `NeedBraces`                   | Mandatory braces                    | `if (x) y;` ❌                         |
| `LeftCurly`                    | Opening brace does not break line   | K&R style                             |
| `RightCurly`                   | Closing brace on own line or with else | -                                  |
| `WhitespaceAround`             | Whitespace around operators         | `x=1` ❌ → `x = 1` ✅                   |
| `OneStatementPerLine`          | One statement per line              | -                                     |
| `MultipleVariableDeclarations` | One variable declaration per line   | `int a, b;` ❌                         |
| `ArrayTypeStyle`               | Array type style                    | `String args[]` ❌ → `String[] args` ✅ |
| `UpperEll`                     | Long literal uses uppercase L       | `100l` ❌ → `100L` ✅                   |
| `ModifierOrder`                | Modifier order                      | `public static final`                 |
| `PackageName`                  | Package name all lowercase          | `com.example.myapp`                   |
| `TypeName`                     | Class name UpperCamelCase           | `UserService`                         |
| `MemberName`                   | Member name lowerCamelCase          | `userName`                            |
| `ParameterName`                | Parameter name lowerCamelCase       | `userId`                              |
| `LocalVariableName`            | Local variable lowerCamelCase       | `itemCount`                           |
| `MethodName`                   | Method name lowerCamelCase          | `getUserById`                         |
| `NoFinalizer`                  | Prohibit finalize() method          | -                                     |

---

## Configuration Description

### Gradle Configuration Location
```
smart-admin-api-java21-springboot3/
├── build.gradle.kts          # Checkstyle plugin configuration
└── config/checkstyle/
    └── checkstyle.xml        # Rule configuration file
```

### build.gradle.kts Configuration
```kotlin
configure<CheckstyleExtension> {
    toolVersion = libs.findVersion("checkstyle").get().toString()
    isIgnoreFailures = false  // Fail on violation
    maxWarnings = 0           // Warnings also treated as errors
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
```

### checkstyle.xml Configuration
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>
    <property name="fileExtensions" value="java, properties, xml"/>

    <module name="TreeWalker">
        <module name="AvoidStarImport"/>
        <module name="OneTopLevelClass"/>
        <module name="NoLineWrap"/>
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAround">
            <property name="allowEmptyConstructors" value="true"/>
            <property name="allowEmptyMethods" value="true"/>
            <property name="allowEmptyTypes" value="true"/>
            <property name="allowEmptyLoops" value="true"/>
            <property name="allowEmptyLambdas" value="true"/>
            <property name="allowEmptyCatches" value="true"/>
        </module>
        <module name="OneStatementPerLine"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="ArrayTypeStyle"/>
        <module name="UpperEll"/>
        <module name="ModifierOrder"/>
        <module name="PackageName">
            <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
        </module>
        <module name="TypeName"/>
        <module name="MemberName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="MethodName"/>
        <module name="NoFinalizer"/>
    </module>
</module>
```

---

## Verification Commands

```bash
# Gradle
./gradlew checkstyleMain checkstyleTest

# View report
open smartadmin-app/build/reports/checkstyle/main.html
```

---

## Related Specifications

- [01-naming-conventions.md](./01-naming-conventions.md) - Detailed naming conventions
- [14-spotless-rules.md](./14-spotless-rules.md) - Auto-formatting (can fix some Checkstyle issues)
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - Quality gates workflow
