---
trigger: on_demand
description: Java error diagnosis and recovery procedures
tags: [troubleshooting, debugging, error-recovery, archunit, quality-gate]
required_rules:
  - rules/foundation/F04-architecture-rules.md
  - rules/quality-tools/Q01-checkstyle-rules.md
  - rules/quality-tools/Q02-pmd-rules.md
  - rules/quality-tools/Q03-spotbugs-rules.md
  - rules/quality-tools/Q04-spotless-rules.md
  - rules/quality-tools/Q05-error-prone-rules.md
  - rules/quality-tools/Q06-jacoco-coverage-rules.md

execution_order:
  - step: identify_error_type
    description: Identify error type
    validation: Clear error classification (compilation/test/ArchUnit/QualityGate)

  - step: locate_root_cause
    description: Locate root cause
    validation: Specific file and line number

  - step: apply_fix
    description: Apply fix solution
    validation: Code modified

  - step: verify_fix
    description: Verify fix
    commands:
      - mvn verify
    validation: BUILD SUCCESS

last_updated: 2025-01-13
---

# Java Error Diagnosis and Recovery Procedures

> **Purpose**: Systematically diagnose and fix compilation errors, test failures, ArchUnit violations, Quality Gate failures, and other issues

---

## 🤖 AI Execution Guide

### When to Apply This Workflow
- ✅ Compilation failures (compilation errors)
- ✅ Test failures (test failures)
- ✅ ArchUnit test failures (architecture violations)
- ✅ Quality Gate failures (coverage/sonar/checkstyle)
- ✅ Runtime errors (NPE, exceptions)
- ✅ Dependency conflicts

### Error Classification Decision Tree
```
Error detected → Identify error type
  ├─ Compilation failure
  │   ├─ Java version error → Section 1.1
  │   ├─ Missing dependency → Section 1.2
  │   └─ Syntax error → Section 1.3
  │
  ├─ ArchUnit test failure
  │   ├─ Architecture violation (Controller → Repository) → Section 2.1
  │   ├─ Optional vs Option → Section 2.2
  │   ├─ Field injection → Section 2.3
  │   └─ @Transactional placement → Section 2.4
  │
  ├─ Quality Gate failure
  │   ├─ Insufficient coverage → Section 3.1
  │   ├─ Checkstyle errors → Section 3.2
  │   └─ SonarQube issues → Section 3.3
  │
  └─ Runtime error
      └─ NullPointerException → Section 4.1
```

---

## Section 1: Compilation Error Diagnosis

### 1.1 Java Version Error

#### Symptoms
```
[ERROR] Failed to execute goal maven-compiler-plugin
[ERROR] Source option 21 is no longer supported
```

#### Diagnosis Commands
```bash
java -version    # Expected: openjdk version "21.x.x"
mvn -version     # Expected: Java version: 21.x.x
echo $JAVA_HOME  # Expected: /path/to/jdk-21
```

#### AI Auto-Fix (macOS)
```bash
# Install Java 21
brew install openjdk@21

# Set environment variables
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

# Verify
java -version && mvn clean compile
```

---

### 1.2 Missing Dependency

#### Symptoms
```
[ERROR] cannot find symbol: class Option
  location: package io.vavr.control
```

#### Diagnosis and Fix
```bash
# Check dependency tree
mvn dependency:tree | grep vavr

# Clean and re-download
mvn clean install -U

# Verify
mvn clean compile
```

---

## Section 2: ArchUnit Test Failure Diagnosis

### 2.1 Architecture Violation: Controller Directly Accessing Repository

#### AI Auto-Diagnosis Process
```bash
# 1. Run test to see detailed violations
mvn test -Dtest=ArchitectureTest#controllerNotAccessRepository -X

# 2. Locate violating code
grep -r "Repository" --include="*Controller.java" src/
```

#### AI Auto-Fix
```java
// ❌ Violating code
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository; // ❌

    @GetMapping("/{id}")
    public ResponseDTO<User> getUser(@PathVariable Long id) {
        return ResponseDTO.ok(userRepository.findById(id)); // ❌
    }
}

// ✅ AI auto-corrected to
// Step 1: Create UserService
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}

// Step 2: Modify Controller
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService; // ✅

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(UserVO::from)
            .map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error("User not found"));
    }
}
```

---

### 2.2 Vavr Violation: Service Using Optional

#### AI Auto-Fix
```java
// ❌ Violation
public Optional<User> findById(Long id) {
    return userMapper.findById(id);
}

// ✅ Auto-corrected to
public Option<User> findById(Long id) {
    return Option.ofOptional(userMapper.findById(id));
}
```

---

### 2.3 Field Injection Violation

#### AI Auto-Fix
```java
// ❌ Violation
@Service
public class UserService {
    @Autowired  // ❌ Field injection
    private UserRepository userRepository;
}

// ✅ Auto-corrected to
@Service
@RequiredArgsConstructor  // ✅ Constructor injection
public class UserService {
    private final UserMapper userMapper;  // ✅
}
```

---

## Section 3: Quality Gate Failure Diagnosis

### 3.1 Insufficient Test Coverage

#### Diagnosis
```bash
# Generate coverage report
mvn clean test jacoco:report

# Open report
open target/site/jacoco/index.html
```

#### Fix: Add Tests
```java
// Find untested methods and add tests
@Test
void shouldReturnNone_whenUserIsInactive() {
    User inactiveUser = User.builder().id(1L).active(false).build();
    when(userMapper.selectById(1L)).thenReturn(inactiveUser);

    Option<User> result = userService.findActiveById(1L);

    assertThat(result.isEmpty()).isTrue();
}
```

---

### 3.2 Checkstyle Errors

#### AI Batch Fix
```bash
# View all errors
mvn checkstyle:check -Dcheckstyle.console=true

# Auto-format
mvn spotless:apply
```

---

### 3.3 SonarQube Issue Fix

#### Cognitive Complexity Too High
```java
// ❌ Complexity 15
public void processOrder(Order order) {
    if (order != null) {
        if (order.getStatus() == Status.PENDING) {
            // Deeply nested...
        }
    }
}

// ✅ Simplify using Vavr Option
public void processOrder(Order order) {
    Option.of(order)
        .filter(o -> o.getStatus() == Status.PENDING)
        .forEach(this::doProcess);
}
```

---

## Section 4: Runtime Error Diagnosis

### 4.1 NullPointerException

#### AI Auto-Refactor
```java
// ❌ Prone to NPE
public String getUserCity(Long id) {
    User user = userRepository.findById(id);  // Might be null
    return user.getAddress().getCity();  // NPE!
}

// ✅ Use Vavr Option
public String getUserCity(Long id) {
    return Option.of(userRepository.findById(id))
        .flatMap(user -> Option.of(user.getAddress()))
        .map(Address::getCity)
        .getOrElse("Unknown");
}
```

---

## Quick Diagnosis Commands

```bash
# Complete diagnosis process
mvn clean verify                              # Full build
mvn test -Dtest=ArchitectureTest              # Architecture tests
mvn jacoco:report && open target/site/jacoco/index.html  # Coverage
mvn checkstyle:check                          # Code style
```

---

**Last Updated**: 2025-01-13
**Sprint 2 Complete**: Detailed error diagnosis procedures
