# SmartAdmin Skills Catalog

**Version**: 4.1.0
**Last Updated**: 2026-03-29
**Total Skills**: 16 (P0: 7, P1: 9)

---

## Overview

Skills are organized in a **Two-Layer Architecture** combining priority (top level) and function (second level):

```
.claude/skills/
├── foundation/          (P0 - 7 skills: Critical Foundation)
│   ├── backend/         (3: ArchUnit, Security, Vavr)
│   ├── frontend/        (1: React CRUD)
│   ├── full-stack/      (2: CRUD Generator, Integration Test)
│   └── testing/         (1: Test Fixtures)
└── extended/            (P1 - 9 skills: Domain & Quality)
    ├── domain/          (3: iGaming PM, Feature Builder, LiteFlow)
    └── quality/         (6: Concurrency, Spring, Naming, Manager Extractor, PostgreSQL, Java21+PG Migration)
```

## Quick Navigation

| Priority | Count | Categories | Purpose |
|----------|-------|------------|---------|
| **P0** (Foundation) | 7 skills | Backend, Frontend, Full-stack, Testing | Critical foundation patterns, essential workflows |
| **P1** (Extended) | 9 skills | Domain, Quality | Business features, quality validation |

---

## P0 Skills (Foundation)

### Backend (3 skills)

#### [archunit-test-generator](foundation/backend/archunit-test-generator/)
Generate ArchUnit tests to enforce architecture rules (layer dependencies, annotation restrictions, naming conventions).

**Trigger Keywords**: "add architecture rule", "generate ArchUnit test", "enforce layer dependency"

---

#### [security-hardening-pro](foundation/backend/security-hardening-pro/)
Implement security best practices (SM2/SM3/SM4 encryption, data masking, XSS/CSRF protection, audit logging).

**Trigger Keywords**: "security", "encryption", "data masking", "SQL injection", "XSS", "CSRF", "audit log"

---

#### [vavr-refactoring-assistant](foundation/backend/vavr-refactoring-assistant/)
Refactor Service layer methods to use Vavr Option/Try/Either instead of java.util.Optional/checked exceptions.

**Trigger Keywords**: "refactor to Vavr", "convert Optional", "use Try", "serviceUsesVavrOption fails"

---

### Frontend (1 skill)

#### [smartadmin-react-crud](foundation/frontend/smartadmin-react-crud/)
Generate React 19 CRUD modules (List + Form + API + Tests) with TypeScript, Ant Design 5.

**Trigger Keywords**: "react CRUD", "frontend component", "Ant Design table", "React form"

---

### Full-stack (2 skills)

#### [smartadmin-crud-generator](foundation/full-stack/smartadmin-crud-generator/)
**Composite Skill**: Complete full-stack CRUD module generation (Backend + Frontend + API Docs + Tests).

**Trigger Keywords**: "create CRUD module", "generate CRUD", "scaffold module", "implement CRUD operations"

**Phase-Based Execution**:
```bash
# Complete CRUD (all phases, ~20 minutes)
/crud Employee --all-phases

# Backend only (~10 minutes)
/crud Order --backend-only

# Frontend only (~8 minutes)
/crud Brand --frontend-only

# API docs only (~3 minutes)
/crud Product --docs-only
```

---

#### [smartadmin-integration-test](foundation/full-stack/smartadmin-integration-test/)
Auto-generate Spring Boot integration tests with Testcontainers for SmartAdmin's layered architecture.

**Trigger Keywords**: "create integration tests", "test database operations", "test Redis cache", "Testcontainers"

---

### Testing (1 skill)

#### [test-fixture-generator](foundation/testing/test-fixture-generator/)
Generate test data builders for complex domain objects (Entity, Form, VO) using AtomicInteger pattern.

**Trigger Keywords**: "create test fixture", "test data builder", "fixture for Entity"

---

## P1 Skills (Extended)

### Domain (3 skills)

#### [liteflow-rule-builder](extended/domain/liteflow-rule-builder/)
Generate LiteFlow rule DSL (EL expressions, QLExpress scripts) from natural language for business workflows.

**Trigger Keywords**: "create LiteFlow chain", "business workflow", "approval flow", "sequential execution", "parallel processing"

**Related**: [docs/plans/liteflow/](../../../docs/plans/liteflow/)

---

#### [igame-feature-builder](extended/domain/igame-feature-builder/)
Implement iGaming domain features (VIP system, Wallet API, Bonus engine, Risk control, Reporting).

**Trigger Keywords**: "VIP tier", "wallet deposit", "bonus distribution", "financial operations", "iGaming business logic"

---

#### [igame-pm-analyst](extended/domain/igame-pm-analyst/)
iGaming 產品經理分析助手 - Ultrathink 深度分析、JTBD 框架、SmartAdmin 架構映射、iGame 風險識別、繁體中文 PRD 生成。

**Trigger Keywords**: "iGame 需求分析", "產品經理", "PRD", "需求澄清", "VIP 功能", "錢包功能", "風控功能"

---

### Quality (5 skills)

#### [concurrency-safety-auditor](extended/quality/concurrency-safety-auditor/)
Concurrency safety audit with 8 detection patterns and risk rating system.

**Trigger Keywords**: "thread safety", "race condition", "concurrency audit", "deadlock"

---

#### [spring-pattern-checker](extended/quality/spring-pattern-checker/)
Validate Spring patterns: @Transactional placement, dependency injection, layered architecture.

**Trigger Keywords**: "@Transactional placement", "Spring patterns", "DI check"

---

#### [naming-convention-checker](extended/quality/naming-convention-checker/)
Validate SmartAdmin naming conventions (singular table names, class naming, field naming).

**Trigger Keywords**: "naming conventions", "class naming", "table naming"

---

#### [smartadmin-manager-extractor](extended/quality/smartadmin-manager-extractor/)
Auto-extract `@Transactional` methods from Service to Manager layer with AST manipulation and ArchUnit validation.

**Trigger Keywords**: "extract to Manager", "transactionalMustUseRollbackForThrowable fails", "refactor transaction"

---

#### [postgresql-best-practices](extended/quality/postgresql-best-practices/)
PostgreSQL performance analysis (HikariCP tuning, N+1 detection, EXPLAIN ANALYZE, index recommendations).

**Trigger Keywords**: "database performance", "HikariCP", "N+1 detection", "EXPLAIN ANALYZE"

---

#### [java21-postgresql-migration](extended/quality/java21-postgresql-migration/)
Java 17→21 與 MySQL→PostgreSQL 遷移規範。涵蓋 toolchain 配置、Virtual Threads、Jakarta EE、BooleanToSmallintTypeHandler、Flyway、SQL 語法差異，以及完整驗收 checklist。

**Trigger Keywords**: "java migration", "java 21", "postgresql migration", "mysql to postgres", "BooleanToSmallintTypeHandler", "virtual threads", "validate migration"

---

## Skill Selection Guide

### By Task Type

| Task Type | Recommended Skill |
|-----------|-------------------|
| **New CRUD Module** | `foundation/full-stack/smartadmin-crud-generator --all-phases` |
| **Backend Only** | `foundation/full-stack/smartadmin-crud-generator --backend-only` |
| **Frontend Only** | `foundation/frontend/smartadmin-react-crud` |
| **Business Workflow** | `extended/domain/liteflow-rule-builder` |
| **Code Quality** | `extended/quality/spring-pattern-checker` |
| **Refactoring** | `foundation/backend/vavr-refactoring-assistant` |
| **Integration Tests** | `foundation/full-stack/smartadmin-integration-test` |
| **Test Fixtures** | `foundation/testing/test-fixture-generator` |
| **Architecture Tests** | `foundation/backend/archunit-test-generator` |
| **Security Audit** | `foundation/backend/security-hardening-pro` |
| **Database Performance** | `extended/quality/postgresql-best-practices` |

### By User Request

| User Says | Use Skill |
|-----------|-----------|
| "Create a player management module" | `smartadmin-crud-generator` |
| "Add approval workflow for withdrawals" | `liteflow-rule-builder` |
| "Refactor Optional to Vavr" | `vavr-refactoring-assistant` |
| "Generate integration tests" | `smartadmin-integration-test` |
| "Check naming conventions" | `naming-convention-checker` |
| "Extract @Transactional to Manager" | `smartadmin-manager-extractor` |
| "Analyze slow queries" | `postgresql-best-practices`CLAUDE.md`productivity/`, `lifecycle/deprecated/` directory references (never existed in v4.0.0)
- Synchronized README with skill-registry.yml v4.0.0 and VERSIONS.yml
- Fixed all broken rule path references (e.g., `10-architecture-rules` → `F04-architecture-rules`)

### 3.2.0 (2026-02-07) - Knowledge Base Complete
- 100% knowledge directory coverage (32/32 skills at the time)

### 3.0.0 (2026-01-29) - Hierarchical Architecture Migration
- Migrated all skills to foundation/extended/productivity/lifecycle structure
- Added skill-registry.yml as Single Source of Truth

### 2.0.0 (2026-01-27) - Skills Consolidation
- CRUD Pipeline: 4 skills → 1 composite (smartadmin-crud-generator)
- Testing Suite: 2 skills → 1 composite
- Performance Suite: 3 skills → 1 composite

### 1.0.0 (2026-01-25) - Initial Catalog

---

**Last Validated**: 2026-03-08
**Validation Scope**: All 15 skill SKILL.md files, trigger keywords, execution modes
**Skill Inventory**:
- Foundation (P0): 7 skills
- Extended (P1): 8 skills
