# SmartAdmin Skills Catalog

**Version**: 2.1.0
**Last Updated**: 2026-01-29
**Total Skills**: 16 (P0: 6, P1: 4, P2: 6)

**Recent Changes** (v2.1.0):
- ✅ **NEW**: batch-plan-executor added to P1 Skills (orchestration & productivity)
- ✅ CRUD Pipeline Consolidated: 4 skills → 1 composite skill
- ✅ Backward compatibility: Old commands route to new skill with deprecation warnings
- ✅ Phase-based execution: `--backend-only`, `--frontend-only`, `--docs-only`, `--all-phases`

## Overview

This directory contains specialized skills for SmartAdmin development. Skills are organized by priority level and domain expertise.

## Quick Navigation

| Priority | Skills | Purpose |
|----------|--------|---------|
| **P0** (Critical) | 6 skills | Foundation, core patterns, essential workflows |
| **P1** (Important) | 4 skills | Business logic, quality gates, orchestration |
| **P2** (Nice-to-have) | 6 skills | Productivity, testing, documentation |

---

## P0 Skills (Critical - Foundation)

### smartadmin-crud-generator
**Description**: Composite skill for complete full-stack CRUD module generation. Consolidates backend (MyBatis/Dao), frontend (Vue 3/Ant Design), API documentation (Knife4j), and integration tests into a unified phase-based workflow.

**Trigger Keywords**:
- "create/generate CRUD module/feature"
- "scaffold complete module"
- "implement CRUD operations"
- "generate full-stack code"

**Phase-Based Execution**:
```bash
# Complete CRUD (all phases)
/crud Employee --all-phases
# Generates: Backend + Frontend + API Docs + Tests

# Backend only (Entity/Dao/Manager/Service/Controller)
/crud Order --backend-only
# Consolidates former: /mybatis

# Frontend only (Vue components + API client + TypeScript types)
/crud Brand --frontend-only
# Consolidates former: /vue-crud

# API documentation only (Swagger/Knife4j annotations)
/crud Product --docs-only
# Consolidates former: /api-docs
```

**Use Cases**:
- Creating new business modules (20 minutes vs 45 minutes with separate skills)
- Implementing CRUD features with consistent patterns
- Scaffolding complete modules (frontend + backend + tests)

**Documentation**: [smartadmin-crud-generator/](smartadmin-crud-generator/)

**Consolidates**:
- Phase 1: Backend generation (formerly smartadmin-mybatis)
- Phase 2: Frontend generation (formerly smartadmin-vue-crud)
- Phase 3: API documentation (formerly smartadmin-api-docs)
- Phase 4: Integration tests (smartadmin-integration-test patterns)

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
- [.agent/foundation/10-architecture-rules.md](../../.agent/foundation/10-architecture-rules.md)
- [.agent/rules/quality-tools/11-checkstyle-rules.md](../../.agent/rules/quality-tools/11-checkstyle-rules.md)
- [.agent/rules/quality-tools/12-pmd-rules.md](../../.agent/rules/quality-tools/12-pmd-rules.md)

---

### igaming-multi-tenant-wallet-pm
**Description**: 資深 iGaming 產品經理專家，專注多商戶架構和無縫錢包設計，具備歐洲/亞洲/美洲/中國市場經驗。生成標準化 PRD 文檔，整合 13 個無縫錢包專題和多商戶技術指南，使用 Mermaid 生成流程圖、架構圖、時序圖，並進行 step by step 深度思考（Ultrathink 方法論）。

**Trigger Keywords**:
- "multi-tenant", "white-label", "多商戶", "白標", "包網"
- "seamless wallet", "無縫錢包", "錢包整合", "遊戲錢包"
- "歐洲牌照", "亞洲市場", "美洲合規", "中國博弈"
- "KYC", "AML", "地區合規"
- "產品需求文檔", "PRD 文檔"

**Phase-Based Execution**:
```bash
# 完整流程（所有階段，55-75 分鐘）
"設計多商戶 VIP 系統，支持白標定制，歐洲牌照合規"
# 執行：需求收集 → 多商戶設計 → 無縫錢包設計 → 地區合規

# 單階段執行（20-25 分鐘）
"無縫錢包對接 Evolution Gaming"
# 僅執行：無縫錢包設計階段（Phase 3）

# 地區合規查詢（10-15 分鐘）
"歐洲市場 KYC 合規要求對比表"
# 僅執行：地區合規階段（Phase 4）
```

**Use Cases**:
- 設計多商戶架構（Schema/行級/完全隔離策略）
- 無縫錢包第三方遊戲對接（13 個核心模式整合）
- 跨地區合規方案（歐洲/亞洲/美洲/中國）
- 生成標準化繁體中文 PRD 文檔（6 章節結構 + Mermaid 圖表）

**Documentation**: [igaming-multi-tenant-wallet-pm/](igaming-multi-tenant-wallet-pm/)

**與 igame-pm-analyst 的關係**: **擴展（Extension）**
- igame-pm-analyst: 通用 iGaming 需求分析
- igaming-multi-tenant-wallet-pm: 專精多商戶 + 無縫錢包 + 跨地區合規
- 自動觸發：檢測到 "multi-tenant" 或 "seamless wallet" 關鍵詞時優先使用此 Skill

**Related Specs**:
- [docs/IGaming/seamless_wallet_analysis/](../../docs/IGaming/seamless_wallet_analysis/) (13 個專題)
- [docs/plans/tenant/](../../docs/plans/tenant/) (多商戶技術指南)
- [docs/IGaming/02_Finance_Center/](../../docs/IGaming/02_Finance_Center/) (錢包模型)

---

### batch-plan-executor
**Description**: Batch plan executor for automatically detecting, analyzing conflicts, and executing multiple implementation plans in parallel. Supports three plan types (Claude Code Plans, Skills Phase Docs, Project Plans) with intelligent conflict detection and execution orchestration.

**Trigger Keywords**:
- "batch execute plans"
- "批量執行方案"
- "execute multiple plans"
- "並行執行計劃"
- "run plans in parallel"
- "orchestrate plan execution"

**Use Cases**:
- Executing multiple CRUD generation plans
- Batch migration workflows (e.g., LiteFlow 8-phase migration)
- Mixed plan type execution (backend + frontend + testing)
- Pre-execution risk assessment with dry-run mode

**Key Features** (v1.0.0 MVP):
- ✅ Auto-detect plan types from multiple sources
- ✅ File-level conflict detection (95%+ accuracy)
- ✅ Serial execution groups for conflicting plans
- ✅ Dry-run mode with comprehensive reports
- ⏳ Parallel execution (v1.1.0 - planned)
- ⏳ Module/dependency conflict detection (v1.2.0 - planned)

**Execution Modes**:
```bash
# Auto mode (fully automatic)
/batch-execute --auto

# Interactive mode (confirm each plan)
/batch-execute --mode=interactive plan1.md plan2.md

# Dry-run mode (simulation only)
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/
```

**Documentation**: [batch-plan-executor/](batch-plan-executor/)

**Configuration**: [batch-plan-executor/config.yml](batch-plan-executor/config.yml)

---

## P2 Skills (Nice-to-have - Productivity)

### db-migration-manager
**Description**: Generate and manage Flyway/Liquibase database migrations for SmartAdmin.

**Documentation**: [db-migration-manager/](db-migration-manager/)

---

### igame-feature-builder
**Description**: Implement iGaming domain features following technical specs (VIP system, Wallet API, Bonus engine, Risk control, Reporting).

**Documentation**: [igame-feature-builder/](igame-feature-builder/)

---

### smartadmin-performance-suite
**Description**: Comprehensive performance suite with integrated diagnose → optimize → monitor workflow. Consolidates java-performance-pro (N+1 detection, JVM tuning), cache-strategy-generator (multi-level caching), and apm-integration-skill (Skywalking, Micrometer, Grafana) into a unified performance orchestrator.

**Trigger Keywords**:
- "slow", "performance", "optimize"
- "N+1", "memory leak", "CPU usage"
- "caching", "cache warming", "cache invalidation"
- "monitoring", "APM", "Grafana dashboard"
- "distributed tracing", "Skywalking", "Micrometer"

**Mode-Based Execution**:
```bash
# Complete workflow (default)
/performance /api/employees/list --workflow
# Runs: Diagnose → Optimize → Monitor (~30 minutes)

# Diagnose only (profile and identify bottlenecks)
/performance /api/orders/query --mode=diagnose
# Consolidates former: /java-performance-pro

# Optimize only (implement caching)
/performance ProductService.getById --mode=optimize
# Consolidates former: /cache-strategy-generator

# Monitor only (setup APM)
/performance OrderService --mode=monitor
# Consolidates former: /apm-integration-skill
```

**Use Cases**:
- Investigating slow endpoints and performance bottlenecks (30 minutes vs 60 minutes with separate skills)
- Implementing multi-level caching (Caffeine L1 + Redis L2)
- Setting up production APM with Skywalking + Grafana
- Complete performance optimization pipeline

**Documentation**: [smartadmin-performance-suite/](smartadmin-performance-suite/)

**Consolidates**:
- Mode 1: Diagnose (formerly java-performance-pro)
- Mode 2: Optimize (formerly cache-strategy-generator)
- Mode 3: Monitor (formerly apm-integration-skill)

---

### smartadmin-testing-suite
**Description**: Comprehensive testing suite with multiple test modes (integration tests with Testcontainers, test fixtures, unit tests, E2E tests). Consolidates smartadmin-integration-test and test-fixture-generator into a unified test orchestrator.

**Trigger Keywords**:
- "create/generate tests"
- "integration test"
- "test fixture"
- "TDD"
- "E2E test"

**Mode-Based Execution**:
```bash
# Integration tests (default)
/test EmployeeService --mode=integration
# Generates: IntegrationTest + Test fixtures + Testcontainers setup

# Test fixtures only
/test Employee --mode=fixtures
# Generates: TestFixture with AtomicInteger builders

# All test types
/test EmployeeService --mode=all
# Generates: Integration + Unit + Fixtures
```

**Use Cases**:
- Creating integration tests for Service/Manager/Controller (10 minutes vs 18 minutes with separate skills)
- Generating test data builders for complex domain objects
- Implementing TDD workflows with RED-GREEN-REFACTOR
- Setting up E2E tests with Cypress/Playwright

**Documentation**: [smartadmin-testing-suite/](smartadmin-testing-suite/)

**Consolidates**:
- Mode 1: Integration Tests (formerly smartadmin-integration-test)
- Mode 2: Test Fixtures (formerly test-fixture-generator)
- Mode 3: Unit Tests (TDD workflow) - future
- Mode 4: E2E Tests - future

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
| **New CRUD Module** | smartadmin-crud-generator --all-phases |
| **Backend Only** | smartadmin-crud-generator --backend-only |
| **Frontend Only** | smartadmin-crud-generator --frontend-only |
| **API Docs Only** | smartadmin-crud-generator --docs-only |
| **Business Workflow** | liteflow-rule-builder → igame-feature-builder |
| **Fraud Detection** | fraud-detection-pattern-generator → security-hardening-pro |
| **Code Quality** | quality-gate-orchestrator → archunit-test-generator |
| **Refactoring** | vavr-refactoring-assistant → java-performance-pro |
| **Database Changes** | db-migration-manager |
| **Integration Tests** | smartadmin-testing-suite --mode=integration |
| **Test Fixtures** | smartadmin-testing-suite --mode=fixtures |
| **TDD Workflow** | smartadmin-testing-suite --mode=unit (future) |
| **Performance Diagnosis** | smartadmin-performance-suite --mode=diagnose |
| **Cache Optimization** | smartadmin-performance-suite --mode=optimize |
| **APM Monitoring** | smartadmin-performance-suite --mode=monitor |
| **Complete Performance Pipeline** | smartadmin-performance-suite --workflow |

### By User Request

| User Says | Use Skill |
|-----------|-----------|
| "Create a player management module" | smartadmin-crud-generator |
| "Add approval workflow for withdrawals" | liteflow-rule-builder |
| "Detect bonus abuse" | fraud-detection-pattern-generator |
| "Set up quality checks for PR" | quality-gate-orchestrator |
| "Refactor Optional to Vavr" | vavr-refactoring-assistant |
| "Add encryption for PII" | security-hardening-pro |
| "Generate integration tests" | smartadmin-testing-suite --mode=integration |
| "Create test fixtures" | smartadmin-testing-suite --mode=fixtures |
| "TDD for new service" | smartadmin-testing-suite --mode=unit |
| "Slow endpoint, need profiling" | smartadmin-performance-suite --mode=diagnose |
| "Add caching to ProductService" | smartadmin-performance-suite --mode=optimize |
| "Setup Grafana dashboard" | smartadmin-performance-suite --mode=monitor |
| "Optimize /api/orders performance" | smartadmin-performance-suite --workflow |

---

## Deprecated Skills (Backward Compatibility)

The following skills have been consolidated into **smartadmin-crud-generator** but remain accessible via command aliases for backward compatibility:

### Migration Timeline

**Soft Deprecation (Weeks 1-12, until 2026-06-30)**:
- ⚠️ Old commands display deprecation warnings
- ✅ Commands still work, auto-route to new skill
- 📚 Migration guide provided in warning message

**Hard Deprecation (Weeks 13-24, until 2026-09-30)**:
- ❌ Old commands display error + migration guide
- ✅ Can still access via explicit flags
- 📚 Documentation shows new patterns only

**Complete Removal (Week 25+, after 2026-10-01)**:
- ❌ Old commands removed
- ✅ Only new consolidated skill remains

### Deprecated Command Mapping

| Old Command | New Command | Status |
|-------------|-------------|--------|
| `/mybatis generate Employee` | `/crud Employee --backend-only` | ⚠️ Soft Deprecated |
| `/vue-crud Brand` | `/crud Brand --frontend-only` | ⚠️ Soft Deprecated |
| `/api-docs ProductController` | `/crud Product --docs-only` | ⚠️ Soft Deprecated |
| `/integration-test EmployeeService` | `/test EmployeeService --mode=integration` | ⚠️ Soft Deprecated |
| `/test-fixture Employee` | `/test Employee --mode=fixtures` | ⚠️ Soft Deprecated |
| `/java-performance-pro analyze /api/orders` | `/performance /api/orders --mode=diagnose` | ⚠️ Soft Deprecated |
| `/cache-strategy ProductService.getById` | `/performance ProductService.getById --mode=optimize` | ⚠️ Soft Deprecated |
| `/apm-integration OrderService` | `/performance OrderService --mode=monitor` | ⚠️ Soft Deprecated |

**Migration Guide**: [skill-aliases.json](skill-aliases.json)

**Example Migration**:
```bash
# CRUD Pipeline - Before (4 separate commands, ~45 minutes)
/mybatis generate Employee
/vue-crud Employee
/api-docs EmployeeController
/integration-test EmployeeService

# CRUD Pipeline - After (1 command, ~20 minutes)
/crud Employee --all-phases

# Testing Suite - Before (2 separate commands, ~18 minutes)
/integration-test EmployeeService
/test-fixture Employee

# Testing Suite - After (1 command, ~10 minutes)
/test EmployeeService --mode=all

# Performance Suite - Before (3 separate commands, ~60 minutes)
/java-performance-pro analyze /api/orders/list
/cache-strategy OrderService
/apm-integration OrderService

# Performance Suite - After (1 command, ~30 minutes)
/performance /api/orders/list --workflow
```

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
- [.agent/rules/00-INDEX.md](../../.agent/rules/00-INDEX.md) - Rule selection logic
- [CLAUDE.md](../../CLAUDE.md) - Primary AI assistant entry point

---

## Version History

- **2.0.0** (2026-01-27): Skills Consolidation (CRUD + Testing + Performance)
  - **CRUD Pipeline Consolidation**:
    - Merged 4 skills into 1: smartadmin-crud-generator
    - Deprecated: smartadmin-mybatis, smartadmin-vue-crud, smartadmin-api-docs
    - Added phase-based execution: --backend-only, --frontend-only, --docs-only, --all-phases
    - Reduced CRUD generation time: 45 min → 20 min (55% improvement)
  - **Testing Suite Consolidation**:
    - Merged 2 skills into 1: smartadmin-testing-suite
    - Deprecated: smartadmin-integration-test, test-fixture-generator
    - Added mode-based execution: --mode=integration, --mode=fixtures, --mode=unit (future), --mode=e2e (future)
    - Reduced test setup time: 18 min → 10 min (44% improvement)
  - **Performance Suite Consolidation**:
    - Merged 3 skills into 1: smartadmin-performance-suite
    - Deprecated: java-performance-pro, cache-strategy-generator, apm-integration-skill
    - Added integrated workflow: --mode=diagnose, --mode=optimize, --mode=monitor, --workflow
    - Reduced performance investigation time: 60 min → 30 min (50% improvement)
  - **Overall Impact**:
    - Skill count: 18 → 12 (9 skills deprecated + consolidated into 3)
    - Backward compatibility via skill-aliases.json (12-week soft deprecation)
    - Development efficiency improved by 40-55% across all workflows
- **1.0.0** (2026-01-25): Initial skills catalog with P0/P1/P2 classification
