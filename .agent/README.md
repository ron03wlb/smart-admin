# SmartAdmin Development Standards and Navigation

> **Target Architecture**: Java 21 + Spring Boot 3.5.4 + PostgreSQL 16 + Vavr 0.10.4 + MyBatis Plus 3.5.12

---

## 1. Quick Start

### Main Development Directories

**Primary development directories:**
- **Backend**: `smart-admin-api-java21-springboot3/` - Java 21 + Spring Boot 3 version
- **Frontend**: `smart-admin-web/` - TypeScript + Vue 3 version

### New Developer Onboarding

1. **Environment Setup**: Start with [workflows/01-environment-setup.md](workflows/01-environment-setup.md) to complete Java 21 + PostgreSQL + Redis environment configuration
2. **Architecture Understanding**: Read [rules/foundation/F04-architecture-rules.md](rules/foundation/F04-architecture-rules.md) to understand layered architecture
3. **Learn Vavr**: Begin functional programming with [rules/technology/functional/P01-vavr-fundamentals.md](rules/technology/functional/P01-vavr-fundamentals.md)
4. **PostgreSQL Features**: Refer to [rules/technology/database/D02-postgresql-advanced.md](rules/technology/database/D02-postgresql-advanced.md) to learn JSONB and arrays
5. **Development Practices**: Read [Coding Standards Summary](docs/coding-standards-summary.md) for specific coding standards

### Environment Setup Verification

```bash
# Verify prerequisites
java -version          # Expected: Java 21+
./gradlew --version    # Expected: Gradle 8.x+, Java 21
docker --version       # Expected: Docker installed

# Start development environment
cd .agent/configs && docker-compose up -d postgres redis

# Verify architecture tests
cd smart-admin-api-java21-springboot3
./gradlew :smartadmin-app:test --tests ArchitectureTest
```

---

## 2. Standards Navigation

### 🏗️ Core Coding Standards
Fundamental coding standards (always apply):
- [01-naming-conventions.md](rules/foundation/F01-naming-conventions.md) - Naming conventions
- [02-oop-principles.md](rules/foundation/F02-oop-principles.md) - OOP principles
- [03-concurrency-rules.md](rules/technology/patterns/P04-concurrency-rules.md) - Concurrency rules
- [04-exception-logging.md](rules/technology/patterns/P05-exception-logging.md) - Exception and logging
- [06-sonarqube-rules.md](rules/workflows/W01-sonarqube-rules.md) - SonarQube rules
- [10-architecture-rules.md](rules/foundation/F04-architecture-rules.md) - Layered architecture

### 🗄️ PostgreSQL Database Standards (Ideal Architecture)
- [D01-postgresql-basics.md](rules/technology/database/D01-postgresql-basics.md) - Table creation and indexing
- [D02-postgresql-advanced.md](rules/technology/database/D02-postgresql-advanced.md) - JSONB, arrays, CTE, window functions
- [D03-postgresql-mybatis.md](rules/technology/database/D03-postgresql-mybatis.md) - Complete PostgreSQL + MyBatis Plus Integration

### 🔒 Security Standards (OWASP Top 10)
- [07-owasp-top10-part1.md](rules/security/S01-owasp-top10-part1.md) - A01-A04
- [07-owasp-top10-part2.md](rules/security/S02-owasp-top10-part2.md) - A05-A10

### 🎯 Vavr Functional Programming (Ideal Architecture)
- [08-vavr-fundamentals.md](rules/technology/functional/P01-vavr-fundamentals.md) - Option, Try fundamentals
- [08-vavr-advanced.md](rules/technology/functional/P02-vavr-advanced.md) - Either, collections, pattern matching
- [08-vavr-mybatis-integration.md](rules/technology/functional/P03-vavr-mybatis-integration.md) - Vavr + MyBatis Plus

### 💾 MyBatis Plus Persistence Layer (Ideal Architecture: LambdaQueryWrapper)
- [09-mybatis-plus-core.md](rules/technology/database/09-mybatis-plus-core.md) - LambdaQueryWrapper, pagination, IEnum
- [D03-postgresql-mybatis.md](rules/technology/database/D03-postgresql-mybatis.md) - PostgreSQL + MyBatis Plus Integration (TypeHandlers, SQL Optimization, Migration)
- [09-manager-layer.md](rules/foundation/F03-manager-layer.md) - Manager layer standards

### 🔧 Static Analysis Tool Standards
- [11-checkstyle-rules.md](rules/quality-tools/Q01-checkstyle-rules.md) - Checkstyle code style
- [12-pmd-rules.md](rules/quality-tools/Q02-pmd-rules.md) - PMD code quality
- [13-spotbugs-rules.md](rules/quality-tools/Q03-spotbugs-rules.md) - SpotBugs bug detection
- [14-spotless-rules.md](rules/quality-tools/Q04-spotless-rules.md) - Spotless formatting
- [15-error-prone-rules.md](rules/quality-tools/Q05-error-prone-rules.md) - Error Prone compile-time checks
- [16-jacoco-coverage-rules.md](rules/quality-tools/Q06-jacoco-coverage-rules.md) - JaCoCo test coverage

### 📝 Git Standards
- [17-commit-message-conventions.md](rules/workflows/W02-commit-message-conventions.md) - Commit message conventions

### 🔄 Development Workflows
- [init.md](workflows/01-environment-setup.md) - Environment initialization
- [github-actions-pipeline.md](workflows/github-actions-pipeline.md) - GitHub Actions CI/CD
- [quality-gates-local-ci.md](workflows/quality-gates-local-ci.md) - Local Quality Gate, GitLab CI
- [tdd-workflow.md](workflows/tdd-workflow.md) - Test-Driven Development
- [java-failure-recovery.md](workflows/java-failure-recovery.md) - Error recovery workflow

### ⚙️ Configuration Reference
- [docker-compose.yml](configs/docker-compose.yml) - PostgreSQL + Redis environment
- [ArchitectureTest.java](configs/ArchitectureTest.java) - ArchUnit test template

---

## 3. Project Overview

### Backend Structure
```
smart-admin-api-java21-springboot3/
├── smartadmin-common/                # Public Foundation (21 modules)
│   ├── foundation/domain/           # Base DTOs (ResponseDTO, PageParam, etc.)
│   ├── foundation/util/             # Utility classes (SmartBeanUtil, SmartPageUtil, etc.)
│   └── foundation/config/           # Core framework configurations
│
├── smartadmin-support/               # Business Support (17 modules)
│   └── module/support/              # Support modules (file, dict, login, etc.)
│
├── smartadmin-modules/               # Business Domain (3 modules)
│   ├── smartadmin-system/           # System modules (employee, role, etc.)
│   ├── smartadmin-business/         # Business modules (your code goes here)
│   └── smartadmin-oa/               # OA modules (notice, enterprise, etc.)
│
├── smartadmin-api/                   # API Contract Layer (3 modules)
│
├── smartadmin-starter/               # Starter Combinations (2 modules)
│
└── smartadmin-app/                   # Unified Application Entry (1 module)
```

### Frontend Structure
```
smart-admin-web/
├── src/
│   ├── api/                         # API interface definitions
│   ├── components/                  # Shared components
│   ├── views/                       # Page views
│   └── store/                       # Pinia state management
└── package.json                     # Project configuration
```

---

## 4. Technology Stack and Development Standards

### 📚 Detailed Documentation
- **Technology Stack Details**: [../.claude/shared/knowledge/project-architecture.md](../.claude/shared/knowledge/project-architecture.md#technology-stack)
- **Coding Standards Summary**: [docs/coding-standards-summary.md](docs/coding-standards-summary.md)
- **Quick Reference**: [../CLAUDE.md](../CLAUDE.md)
- **FAQ**: [docs/faq-troubleshooting.md](docs/faq-troubleshooting.md)

### Core Technical Points

**Layered Architecture (Mandatory)**:
```
Controller → Service → Manager → Dao → Database
```

**Vavr Functional Programming**:
```java
// Option replaces null
Option<User> user = Option.of(userMapper.selectById(id));

// Try replaces try-catch
Try<User> result = Try.of(() -> userService.create(dto));
```

**ResponseDTO Pattern**:
```java
return ResponseDTO.ok(data);
return ResponseDTO.error(ErrorCode.XXX);
```

---

## 5. AI Rule System

### Positioning Explanation

`.agent` documentation defines the **ideal target architecture** to guide project technical evolution:

- **Ideal Architecture** (`positioning: ideal`): PostgreSQL + Vavr + LambdaQueryWrapper
- **Current Implementation**: MySQL + Traditional Java + XML Mapper (see actual code)
- **Migration Path** (`positioning: migration`): Guidance for migrating from current implementation to ideal architecture

### File Organization Principles

1. **Character Limit**: All rules/*.md and workflows/*.md ≤ 11000 characters (except special index files)
2. **Flat Structure**: rules/ and workflows/ do not use subdirectories
3. **Prerequisites**: Declared via `prerequisites` field in front matter
4. **Cross-References**: Linked via `related_rules` and `related_workflows` fields
5. **Positioning Tags**: `ideal` (ideal architecture) / `migration` (migration support) / `current-standard` (must follow now)

### AI Decision Matrix

Entry point for all AI rules: [rules/00-INDEX.md](rules/00-INDEX.md)

**Core Checklist**:
- ✅ Service layer must use `io.vavr.control.Option` instead of `java.util.Optional`
- ✅ Controller cannot directly access Repository
- ✅ Field injection forbidden (use constructor injection)
- ✅ @Transactional/@Cacheable only in Manager layer
- ✅ Naming conventions (Controller/Service/Dao suffix)
- ✅ Test coverage ≥ 80%

---

## 6. Quality Assurance

### ArchUnit Tests

```bash
# Run complete architecture tests
./gradlew :smartadmin-app:test --tests ArchitectureTest

# Run specific tests
./gradlew :smartadmin-app:test --tests "ArchitectureTest.serviceUsesVavrOption"
./gradlew :smartadmin-app:test --tests "ArchitectureTest.layerDependencies"
```

**Mandatory Rules (PR Blocking)**:
- ✅ Service layer uses Vavr Option
- ✅ Controller does not directly access Repository
- ✅ Constructor injection
- ✅ @Transactional only in Manager layer

### Quality Gate Standards

```yaml
Quality Gate Pass Criteria:
  ✅ ArchUnit:        100% pass (zero tolerance)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ Test Coverage:   ≥ 80% (Line), ≥ 70% (Branch)
```

**Detailed Standards**: [workflows/quality-gates-local-ci.md](workflows/quality-gates-local-ci.md)

---

## 7. Quick Navigation

### Common Resources
- **Quick Reference**: [../CLAUDE.md](../CLAUDE.md) - Command and pattern cheat sheet
- **FAQ**: [docs/faq-troubleshooting.md](docs/faq-troubleshooting.md) - Troubleshooting
- **Official Documentation**: https://smartadmin.vip
- **Live Preview**: https://preview.smartadmin.vip

### Workflow Index
- **Workflow Overview**: [workflows/00-workflow-index.md](workflows/00-workflow-index.md)
- **AI Decision Matrix**: [rules/00-INDEX.md](rules/00-INDEX.md)

---

**Last Updated**: 2025-01-21
**Version**: v3.0 (Refactored Edition)

**Remember**: We advocate for high-quality code. As developers, code is our sword! ⚔️
