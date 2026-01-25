# SmartAdmin Skills Catalog

**Version**: 1.0.0
**Last Updated**: 2026-01-25
**Total Skills**: 18 (P0: 6, P1: 3, P2: 9)

## Overview

This directory contains specialized skills for SmartAdmin development. Skills are organized by priority level and domain expertise.

## Quick Navigation

| Priority | Skills | Purpose |
|----------|--------|---------|
| **P0** (Critical) | 6 skills | Foundation, core patterns, essential workflows |
| **P1** (Important) | 3 skills | Business logic, quality gates, fraud detection |
| **P2** (Nice-to-have) | 9 skills | Productivity, testing, documentation |

---

## P0 Skills (Critical - Foundation)

### smartadmin-crud-generator
**Description**: Generate complete full-stack CRUD module (Backend Entity/Dao/Manager/Service/Controller + Frontend Vue components + API client + Tests + Documentation).

**Trigger Keywords**:
- "create/generate CRUD module/feature"
- "scaffold complete module"
- "implement CRUD operations"
- "generate full-stack code"

**Use Cases**:
- Creating new business modules
- Implementing CRUD features
- Scaffolding complete modules (frontend + backend)

**Documentation**: [smartadmin-crud-generator/](smartadmin-crud-generator/)

---

### smartadmin-mybatis
**Description**: MyBatis/MyBatis Plus AI code generator for SmartAdmin framework. Optimizes queries and generates Dao layer code.

**Trigger Keywords**:
- "create Dao/Entity/Mapper"
- "MyBatis query optimization"
- "database layer code generation"

**Use Cases**:
- Creating new Dao layer code
- Optimizing MyBatis queries
- Reviewing MyBatis implementations

**Documentation**: [smartadmin-mybatis/](smartadmin-mybatis/)

---

### vavr-refactoring-assistant
**Description**: Refactor Service layer methods to use Vavr Option/Try/Either instead of java.util.Optional/checked exceptions.

**Trigger Keywords**:
- "refactor to Vavr"
- "convert Optional"
- "use Try"
- "functional exceptions"
- "serviceUsesVavrOption fails"

**Use Cases**:
- Refactoring Service layer to Vavr patterns
- Fixing ArchitectureTest violations
- Eliminating checked exceptions

**Documentation**: [vavr-refactoring-assistant/](vavr-refactoring-assistant/)

---

### archunit-test-generator
**Description**: Generate ArchUnit tests to enforce architecture rules (layer dependencies, annotation restrictions, naming conventions, field injection patterns).

**Trigger Keywords**:
- "add architecture rule"
- "generate ArchUnit test"
- "enforce layer dependency"
- "validate architecture"

**Use Cases**:
- Adding new architecture rules from `.agent/rules/*.md`
- Enforcing layer dependencies
- Validating naming conventions

**Documentation**: [archunit-test-generator/](archunit-test-generator/)

---

### security-hardening-pro
**Description**: Implement security best practices (SM2/SM3/SM4 encryption, data masking for PII, SQL injection prevention, XSS/CSRF protection, rate limiting, audit logging).

**Trigger Keywords**:
- "security", "encryption", "data masking"
- "SQL injection", "XSS", "CSRF"
- "audit log", "compliance", "hardening"
- "KYC/AML compliance"

**Use Cases**:
- Securing APIs
- Implementing encryption
- Masking sensitive data
- Meeting compliance requirements

**Documentation**: [security-hardening-pro/](security-hardening-pro/)

---

### smartadmin-integration-test
**Description**: Auto-generate Spring Boot integration tests with Testcontainers for SmartAdmin's layered architecture.

**Trigger Keywords**:
- "create/generate integration tests"
- "test database operations"
- "test Redis cache"
- "Testcontainers setup"

**Use Cases**:
- Creating integration tests for Service/Manager/Controller
- Testing database persistence
- Validating @Transactional behavior
- Testing cross-layer integration

**Documentation**: [smartadmin-integration-test/](smartadmin-integration-test/)

---

## P1 Skills (Important - Business Logic & Quality)

### liteflow-rule-builder
**Description**: Generate LiteFlow rule DSL (EL expressions and QLExpress scripts) from natural language descriptions for complex business workflows.

**Trigger Keywords**:
- "create LiteFlow chain/rule"
- "business workflow", "approval flow", "validation chain"
- "sequential execution", "parallel processing", "conditional logic"
- "orchestration", "routing", "branching"
- "migrate from Evrete"

**Use Cases**:
- Implementing business rule orchestration
- Creating approval workflows
- Building validation chains
- Migrating from Evrete rules

**Documentation**: [liteflow-rule-builder/](liteflow-rule-builder/)

**Related Specs**:
- [docs/plans/liteflow/README.md](../../docs/plans/liteflow/README.md)
- [docs/plans/liteflow/architecture.md](../../docs/plans/liteflow/architecture.md)

---

### fraud-detection-pattern-generator
**Description**: Generate complete fraud detection and risk control systems for iGaming platforms (multi-account detection, bonus abuse prevention, suspicious betting patterns, payment fraud, KYC verification automation).

**Trigger Keywords**:
- "fraud", "risk control", "bonus abuse"
- "multi-account", "suspicious transactions"
- "KYC automation", "AML screening"
- "iGaming compliance"

**Use Cases**:
- Implementing multi-account detection
- Preventing bonus abuse
- Detecting suspicious betting patterns
- Automating KYC verification triggers
- Real-time risk scoring

**Documentation**: [fraud-detection-pattern-generator/](fraud-detection-pattern-generator/)

**Related Specs**:
- [docs/iGame/technical-specs/P0-critical/04-kyc-aml-automation.md](../../docs/iGame/technical-specs/P0-critical/04-kyc-aml-automation.md)
- [docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md](../../docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md)

---

### quality-gate-orchestrator
**Description**: Automate quality gates orchestrating ArchUnit, Checkstyle, PMD, SpotBugs, JaCoCo, and SonarQube. Generate pre-merge quality checks, CI/CD pipelines, and violation reports.

**Trigger Keywords**:
- "quality gate"
- "pre-commit checks"
- "ArchUnit integration"
- "Checkstyle pipeline"
- "PMD automation"
- "SpotBugs workflow"
- "SonarQube integration"
- "code quality checks"
- "static analysis pipeline"

**Use Cases**:
- Setting up pre-merge quality checks
- Creating CI/CD quality pipelines
- Generating aggregated quality reports
- Enforcing quality thresholds

**Documentation**: [quality-gate-orchestrator/](quality-gate-orchestrator/)

**Related Rules**:
- [.agent/rules/10-architecture-rules.md](../../.agent/rules/10-architecture-rules.md)
- [.agent/rules/11-checkstyle-rules.md](../../.agent/rules/11-checkstyle-rules.md)
- [.agent/rules/12-pmd-rules.md](../../.agent/rules/12-pmd-rules.md)

---

## P2 Skills (Nice-to-have - Productivity)

### smartadmin-vue-crud
**Description**: Generate Vue 3 + Ant Design CRUD components for SmartAdmin frontend (list views, form modals, API clients, TypeScript types).

**Documentation**: [smartadmin-vue-crud/](smartadmin-vue-crud/)

---

### smartadmin-api-docs
**Description**: Auto-generate and maintain Knife4j/OpenAPI API documentation for SmartAdmin.

**Documentation**: [smartadmin-api-docs/](smartadmin-api-docs/)

---

### db-migration-manager
**Description**: Generate and manage Flyway/Liquibase database migrations for SmartAdmin.

**Documentation**: [db-migration-manager/](db-migration-manager/)

---

### igame-feature-builder
**Description**: Implement iGaming domain features following technical specs (VIP system, Wallet API, Bonus engine, Risk control, Reporting).

**Documentation**: [igame-feature-builder/](igame-feature-builder/)

---

### java-performance-pro
**Description**: Profile and optimize Java application performance (N+1 query detection, JVM memory tuning, CPU hotspot analysis, Redis cache optimization).

**Documentation**: [java-performance-pro/](java-performance-pro/)

---

### test-fixture-generator
**Description**: Generate test data builders for complex domain objects (Entity, Form, VO) for integration tests.

**Documentation**: [test-fixture-generator/](test-fixture-generator/)

---

### cicd-pipeline-builder
**Description**: Automate CI/CD pipeline setup for SmartAdmin with GitHub Actions or GitLab CI.

**Documentation**: [cicd-pipeline-builder/](cicd-pipeline-builder/)

---

### semgrep-rule-creator
**Description**: Create custom Semgrep rules for detecting bug patterns and security vulnerabilities.

**Documentation**: Symlinked to `.agents/skills/semgrep-rule-creator`

---

### git-pushing
**Description**: Stage, commit, and push git changes with conventional commit messages.

**Documentation**: Symlinked to `.agents/skills/git-pushing`

---

## Skill Selection Guide

### By Task Type

| Task Type | Recommended Skills |
|-----------|-------------------|
| **New CRUD Module** | smartadmin-crud-generator → smartadmin-mybatis → smartadmin-integration-test |
| **Business Workflow** | liteflow-rule-builder → igame-feature-builder |
| **Fraud Detection** | fraud-detection-pattern-generator → security-hardening-pro |
| **Code Quality** | quality-gate-orchestrator → archunit-test-generator |
| **Refactoring** | vavr-refactoring-assistant → java-performance-pro |
| **Frontend Development** | smartadmin-vue-crud → smartadmin-api-docs |
| **Database Changes** | db-migration-manager → smartadmin-mybatis |
| **Testing** | smartadmin-integration-test → test-fixture-generator |

### By User Request

| User Says | Use Skill |
|-----------|-----------|
| "Create a player management module" | smartadmin-crud-generator |
| "Add approval workflow for withdrawals" | liteflow-rule-builder |
| "Detect bonus abuse" | fraud-detection-pattern-generator |
| "Set up quality checks for PR" | quality-gate-orchestrator |
| "Refactor Optional to Vavr" | vavr-refactoring-assistant |
| "Add encryption for PII" | security-hardening-pro |
| "Generate integration tests" | smartadmin-integration-test |

---

## Skill Development Guidelines

### Creating New Skills

1. **Identify Need**: Clear use case requiring specialized knowledge
2. **Define Scope**: Specific, well-bounded problem domain
3. **Write Skill**: Follow [skill-creator](../../../.agents/skills/skill-creator/) guidelines
4. **Add Tests**: RED-GREEN-REFACTOR pattern examples
5. **Document**: Clear trigger keywords and use cases
6. **Update Catalog**: Add to this README with appropriate priority

### Skill Priority Levels

- **P0 (Critical)**: Foundation patterns, mandatory for all development
- **P1 (Important)**: Business logic, quality gates, domain-specific features
- **P2 (Nice-to-have)**: Productivity tools, optional enhancements
- **P3 (Experimental)**: Proof-of-concept, under evaluation

### Skill Maintenance

- Review and update skills quarterly
- Deprecate skills with < 10% usage
- Merge overlapping skills
- Update trigger keywords based on user feedback

---

## Related Documentation

- [.claude/README.md](../README.md) - AI agent system overview
- [.agent/rules/00-ai-decision-matrix.md](../../.agent/rules/00-ai-decision-matrix.md) - Rule selection logic
- [CLAUDE.md](../../CLAUDE.md) - Primary AI assistant entry point

---

## Version History

- **1.0.0** (2026-01-25): Initial skills catalog with P0/P1/P2 classification
