---
trigger: always_on
description: AI Decision Matrix - Scenario to Rule Mapping
tags: [meta, ai-guide, decision-tree, orchestration]
positioning: current-standard
ai_role: orchestrator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/01-naming-conventions.md
  - rules/10-architecture-rules.md
  - rules/08-vavr-fundamentals.md
  - rules/11-checkstyle-rules.md
  - rules/12-pmd-rules.md
  - rules/13-spotbugs-rules.md
  - rules/14-spotless-rules.md
  - rules/15-error-prone-rules.md
  - rules/16-jacoco-coverage-rules.md
  - rules/17-commit-message-conventions.md
last_updated: 2025-01-17
---

# AI Decision Matrix

> **Purpose**: Help AI quickly identify user request scenarios and automatically apply the correct rule combinations

## 🤖 AI Instruction Block

### When to Apply This Rule
- ✅ **Always**: This rule should be consulted before every response
- ✅ When user initiates any code generation request
- ✅ When user requests Code Review
- ✅ When user reports errors requiring diagnosis

### Mandatory Enforcement Checklist
Before responding to user requests, must:
- [ ] Identify request type (code generation/review/diagnosis/query)
- [ ] Determine involved code layers (Controller/Service/Repository/Entity)
- [ ] List all rules that need to be applied
- [ ] Apply rules in priority order

---

## AI Decision Tree

```
User Request Classification
├─ 1️⃣ Generate New Code
│   ├─ Controller: [01-naming, 10-architecture, 04-exception-logging]
│   ├─ Service: [01-naming, 02-oop, 08-vavr, 10-architecture]
│   ├─ Repository/Mapper: [01-naming, 09-mybatis-plus, 05-postgresql]
│   └─ Entity: [01-naming, 05-postgresql, 09-mybatis-plus]
│
├─ 2️⃣ Code Review
│   ├─ Architecture Violation: [10-architecture-rules] → ArchUnit
│   ├─ Naming Convention: [01-naming] → Checkstyle
│   ├─ Vavr Usage: [08-vavr-*] → Option/Try checks
│   ├─ OOP Principles: [02-oop-principles]
│   ├─ Concurrency Safety: [03-concurrency-rules]
│   └─ Security Check: [07-owasp-top10-*]
│
├─ 3️⃣ Database Operations
│   ├─ Table Creation: [05-postgresql-basics]
│   ├─ JSONB/CTE/Window Functions: [05-postgresql-advanced]
│   ├─ MyBatis Mapper: [09-mybatis-plus-*]
│   └─ MySQL to PG Migration: [05-postgresql-mybatis-integration]
│
├─ 4️⃣ Error Diagnosis
│   ├─ Compilation Error: Workflow [java-failure-recovery]
│   ├─ ArchUnit Failure: [10-architecture] + corresponding rules
│   ├─ Quality Gate Failure: Workflow [quality-gates-local-ci]
│   └─ Runtime Error: [08-vavr (Try), 04-exception-logging]
│
├─ 5️⃣ Static Analysis Tools
│   ├─ Checkstyle: [11-checkstyle-rules] → ./gradlew checkstyleMain
│   ├─ PMD: [12-pmd-rules] → ./gradlew pmdMain
│   ├─ SpotBugs: [13-spotbugs-rules] → ./gradlew spotbugsMain
│   ├─ Spotless: [14-spotless-rules] → ./gradlew spotlessApply
│   ├─ Error Prone: [15-error-prone-rules]
│   └─ JaCoCo: [16-jacoco-coverage-rules]
│
├─ 6️⃣ Knowledge Query
│   ├─ PostgreSQL: [05-postgresql-*]
│   ├─ Vavr: [08-vavr-*]
│   ├─ MyBatis Plus: [09-mybatis-plus-*]
│   └─ Architecture Design: [10-architecture-rules]
│
└─ 7️⃣ Git Operations
    └─ Commit Message: [17-commit-message-conventions]
```

---

## 📋 Mandatory Check Matrix

| Code Type         | Required Rules                       | Automation Tool | Blocking Level |
| ----------------- | ------------------------------------ | --------------- | -------------- |
| Service New Method | 08-vavr (Option/Try)                | ArchUnit        | 🚫 Block PR    |
| Service New Method | 10-architecture (Constructor Injection) | ArchUnit    | 🚫 Block PR    |
| Controller New Method | 10-architecture (No Direct Repo Access) | ArchUnit | 🚫 Block PR    |
| Any New Code      | 01-naming (Naming Convention)        | Checkstyle      | 🚫 Block PR    |
| Any New Code      | Test Coverage ≥ 80%                  | JaCoCo          | 🚫 Block PR    |

### Quality Gate Standards

```yaml
Quality Gate Pass Criteria:
  ✅ ArchUnit:        100% pass (zero tolerance)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ Test Coverage:   ≥ 80% (Line), ≥ 70% (Branch)
  ✅ SonarQube:       0 Blocker/Critical issues
```

---

## 🔧 AI Auto-Fix Strategy

### Can Auto-Fix (auto_apply: true)
- Optional → Option (Vavr)
- try-catch → Try.of()
- @Autowired field injection → @RequiredArgsConstructor
- Non-compliant naming (class name/method name/constant)

### Ask User Before Fix (ask_before_fix: true)
- Service method missing @Transactional
- Controller directly accessing Repository
- Entity missing JSONB TypeHandler
- Complex business logic in Controller

### Forbidden to Auto-Fix
- 🚫 Business logic errors
- 🚫 Security vulnerabilities
- 🚫 Database migration
- 🚫 Code deletion

---

## 📊 Rule Priority

| Priority    | Rule Type                     | Description          |
| ----------- | ----------------------------- | -------------------- |
| P0 Highest  | 10-architecture, 07-owasp-*   | Architecture/Security|
| P1 High     | 08-vavr, 01-naming, 02-oop    | Code Quality         |
| P2 Medium   | 09-mybatis, 05-postgresql     | Best Practices       |
| P3 Low      | 03-concurrency, 06-sonarqube  | Optimization Tips    |

### Conflict Resolution Principles
1. **Security > Performance > Readability**
2. **Architecture Constraints > Code Style**
3. **New Code High Standards > Legacy Compatibility**

---

## 📚 Quick Reference Card

| Keyword               | Immediately Apply Rules    | Checkpoint                         |
| --------------------- | -------------------------- | ---------------------------------- |
| "Create Controller"   | 01, 10, 04                 | RESTful, No Direct Repo Access     |
| "Create Service"      | 01, 02, 08, 10             | Option/Try, Constructor Injection  |
| "Create Mapper"       | 01, 09, 05                 | LambdaQueryWrapper                 |
| "Create Entity"       | 01, 05                     | @TableName, JSONB/Array            |
| "Code Review"         | 10, 08, 01                 | ArchUnit, Vavr, Naming             |
| "JSONB"               | 05-advanced, 09-postgresql | TypeHandler                        |
| "Exception Handling"  | 08-vavr, 04                | Try.of(), Logging                  |
| "Transaction"         | 09-manager-layer           | @Transactional in Manager          |
| "commit"              | 17-commit-message          | Conventional Commits Format        |

---

## ✅ Core Checklist

### Basic Checks
- [ ] Class names UpperCamelCase, method names lowerCamelCase
- [ ] Constructor injection (no @Autowired fields)

### Architecture Checks
- [ ] Controller does not directly access Repository
- [ ] @Transactional/@Cacheable only in Manager layer

### Validation Commands
```bash
./gradlew check                    # Complete check
mvn test -Dtest=ArchitectureTest   # Architecture test
```

---

**This decision matrix is the master entry point for all rules. AI should consult this document before handling any request.**
