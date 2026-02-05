# CLAUDE.md

**For AI Assistants**: This documentation is designed for **Claude, Antigravity, Gemini, and other AI coding assistants**. All AI tools should read this as the primary entry point to SmartAdmin development guidelines.

**For Developers**: Quick Reference Card for SmartAdmin development patterns, conventions, and commands.

**Navigation**:
- [README.md](README.md) - Project overview and "I want to..." guide
- [.claude/README.md](.claude/README.md) - AI agent system and orchestration
- [.agent/README.md](.agent/README.md) - Comprehensive technical rules
- [Architecture Documentation](docs/) - High-level system design

---

## Quick Reference Card

| Task | Pattern | Details |
|------|---------|---------|
| Return success | `ResponseDTO.ok(data)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#responsedto-pattern) |
| Paginated query | `SmartPageUtil.convert2PageQuery(form)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#pagination-pattern) |
| Bean copy | `SmartBeanUtil.copy(source, Target.class)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#bean-conversion) |
| Transaction | `@Transactional` in Manager only | [→](.claude/shared/knowledge/smartadmin-patterns.md#transaction-management) |

**Complete Patterns**: [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)

---

## For AI Coding Assistants

### Reading Priority (AI Assistants)

When working with SmartAdmin codebase, read documentation in this order:

1. **CLAUDE.md** (this file) - Quick reference and navigation hub
2. **[.agent/rules/00-INDEX.md](.agent/rules/00-INDEX.md)** - Unified decision center (630 lines: rules routing, skill selection, agent orchestration) ⭐
3. **[.agent/rules/foundation/10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md)** - Mandatory architectural constraints (enforced by ArchUnit)
4. **[.claude/shared/knowledge/](.claude/shared/knowledge/)** - SmartAdmin implementation patterns
5. **[.claude/skills/](.claude/skills/)** - Specialized skills for complex tasks (optional, for Claude Code)
6. **[.claude/agents/](.claude/agents/)** - Specialized agent definitions (optional, for Claude Code)

**Note**: For rule-specific questions (e.g., "How to name Service classes?"), skip directly to relevant rule files in step 3-4. The 00-INDEX.md in step 2 is primarily for AI decision-making (choosing rules/skills/agents).

### Interaction Language

- **Documentation**: All AI instruction documents are in English
- **User Communication**: Respond to users in **Traditional Chinese (繁體中文)**
- **Code Comments**: English (SmartAdmin standard - no Chinese in code comments)

### Key Constraints (Always Apply)

**CRITICAL** - These rules are enforced by ArchitectureTest and must NEVER be violated:

- ✅ Service layer MUST use `io.vavr.control.Option` (NOT `java.util.Optional`)
- ✅ Controller NEVER directly accesses Repository/Dao (must go through Service)
- ✅ **Service CAN directly call Dao/Mapper** (for single-table CRUD without @Transactional)
- ✅ `@Transactional` / `@Cacheable` annotations ONLY in Manager layer (NEVER in Service)
- ✅ **When Service needs @Transactional or @Cacheable → Extract to Manager layer**
- ✅ Constructor injection via `@RequiredArgsConstructor` + `private final` (NEVER `@Autowired` field injection)
- ✅ Boolean fields: `deleted` NOT `isDeleted`
- ✅ Use `ResponseDTO.ok(data)` for all API responses
- ✅ Transaction annotation: `@Transactional(rollbackFor = Throwable.class)`

### When in Doubt

- **Rule/Skill/Agent selection**: Consult [.agent/rules/00-INDEX.md](.agent/rules/00-INDEX.md) for unified decision center
- **Architectural violations**: Will fail ArchUnit tests - check [.agent/configs/ArchitectureTest.java](.agent/configs/ArchitectureTest.java)
- **Pattern implementation**: See [.claude/shared/knowledge/smartadmin-patterns.md](.claude/shared/knowledge/smartadmin-patterns.md)

---

## Build Commands

**Location**: `smart-admin-api-java21-springboot3/`

```bash
./gradlew :sa-admin:bootRun    # Run (http://localhost:1024)
./gradlew :sa-admin:test       # Test
```

→ **[All Build Commands](.claude/shared/knowledge/project-architecture.md#build-commands)**

## Architecture

Modular monolith with strict layered architecture:

```
Controller → Service → Manager → Dao → Entity
              ↓         ↓
            (Can call Dao directly for single-table CRUD)
            (Delegate to Manager when @Transactional needed)
```

**Key Rules** (enforced by ArchUnit):
- Controller → Service ONLY (Controller CANNOT call Dao/Manager directly)
- **Service → Dao is ALLOWED** (for single-table CRUD without @Transactional)
- **Service → Manager is REQUIRED** (when @Transactional or @Cacheable needed)
- `@Transactional` / `@Cacheable`: Manager layer ONLY (NEVER in Service/Controller)
- `@Autowired` field injection: FORBIDDEN

→ **[Complete Architecture Rules](.agent/rules/foundation/10-architecture-rules.md)**
→ **[SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)**
→ **[Project Architecture](.claude/shared/knowledge/project-architecture.md)**

## Foundation Package Naming

**v3.6.0+ Standard Pattern:**
- Foundation: `net.lab1024.sa.foundation.{module}.*`
- Infrastructure: `net.lab1024.sa.base.{module}.*`
- Support: `net.lab1024.sa.base.module.support.{module}.*`

**v4.0.0 Breaking Change:**
- ❌ **REMOVED**: `net.lab1024.sa.common.core.*` bridge classes
- ✅ **Use instead**: `net.lab1024.sa.foundation.domain.*`
- ℹ️ **Exception**: `SmartBeanUtil` remains in `net.lab1024.sa.common.core.util.*`

→ **[Complete Package Naming Guide](docs/archive/migration/foundation-packages.md)** (archived)
→ **[v4.0.0 Breaking Changes](#v40-breaking-changes)**

## SmartAdmin Patterns

**Core Patterns:**
- [ResponseDTO Pattern](.claude/shared/knowledge/smartadmin-patterns.md#responsedto-pattern) - API responses (ok, error, exceptions)
- [Domain Objects](.claude/shared/knowledge/smartadmin-patterns.md#domain-object-pattern) - Entity, Form, VO, QueryForm
- [Pagination](.claude/shared/knowledge/smartadmin-patterns.md#pagination-pattern) - SmartPageUtil usage
- [Bean Conversion](.claude/shared/knowledge/smartadmin-patterns.md#bean-conversion) - SmartBeanUtil patterns
- [Authentication](.claude/shared/knowledge/smartadmin-patterns.md#authentication-sa-token) - Sa-Token (@NoNeedLogin, @SaCheckPermission)
- [Dependency Injection](.claude/shared/knowledge/smartadmin-patterns.md#dependency-injection) - Constructor injection (MANDATORY)
- [Transaction Management](.claude/shared/knowledge/smartadmin-patterns.md#transaction-management) - Manager layer only

**See**: [Complete SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)

## Key Conventions

**Dependency Injection (MANDATORY):**
- `@RequiredArgsConstructor` + `private final` fields
- NEVER `@Autowired` field injection

**Naming Examples:**
- Classes: `UserController`, `UserService`, `UserManager`, `UserDao`
- Boolean: `deleted` NOT `isDeleted`

**Commit Format:** `<type>(<scope>): <subject>`

→ **[Complete Naming Conventions](.agent/rules/foundation/01-naming-conventions.md)**
→ **[Commit Message Guide](.agent/rules/workflows/17-commit-message-conventions.md)**

## Anti-Patterns to Avoid

**Top 3 Critical:**

| ❌ Never | ✅ Always |
|---------|----------|
| `@Transactional` in Service | Manager layer only |
| Field injection | Constructor injection |
| Controller → Dao | Controller → Service → Dao |

→ **[Complete Anti-Patterns List](.claude/shared/knowledge/quality-standards.md#anti-patterns-to-avoid)**

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.5.4 |
| MyBatis Plus | 3.5.12 |
| Sa-Token | 1.44.0 |
| Redisson | 3.50.0 |
| Knife4j | 4.6.0 |

**See also**: [Project Architecture](.claude/shared/knowledge/project-architecture.md) for detailed version compatibility and configuration.

## Java 21 Features

SmartAdmin v4.0.0+ leverages Java 21 features for improved type safety and performance:

**Sealed Classes (Type Safety):**
- `ErrorCode` interface uses sealed classes to restrict implementations
- Compiler-enforced exhaustiveness in switch expressions
- Pattern: `sealed interface ErrorCode permits SystemErrorCode, UserErrorCode, UnexpectedErrorCode`

**Virtual Threads (Performance):**
- Enabled for `@Async` and `@Scheduled` tasks
- 30-50% throughput improvement for I/O-intensive operations
- Configuration: `spring.threads.virtual.enabled=true`
- Low memory footprint: ~1KB per virtual thread (vs ~1MB for platform threads)

**Implementation Details:**
- Virtual Threads: [VirtualThreadsConfig.java](smart-admin-api-java21-springboot3/sa-admin/src/main/java/net/lab1024/sa/admin/config/VirtualThreadsConfig.java)
- Sealed ErrorCode: [ErrorCode.java](smart-admin-api-java21-springboot3/sa-base/foundation/domain/src/main/java/net/lab1024/sa/foundation/domain/code/ErrorCode.java)

→ **[Complete Java 21 Features Guide](docs/architecture/java21-features.md)**

## Development Guidelines

**Essential Rules** (see `.agent/rules/`):
- Architecture: [`foundation/10-architecture-rules.md`](.agent/rules/foundation/10-architecture-rules.md)
- Manager Layer: [`foundation/09-manager-layer.md`](.agent/rules/foundation/09-manager-layer.md)
- Naming: [`foundation/01-naming-conventions.md`](.agent/rules/foundation/01-naming-conventions.md)
- Exceptions: [`technology/patterns/04-exception-logging.md`](.agent/rules/technology/patterns/04-exception-logging.md)

**Validation**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

→ **[Unified Decision Center](.agent/rules/00-INDEX.md)** - Rules, Skills, and Agent Routing
→ **[Quality Standards](.claude/shared/knowledge/quality-standards.md)**

### Mermaid Diagram Standards

**CRITICAL RULE**: All Mermaid diagrams in SmartAdmin project must use `<br/>` HTML tags for line breaks, NOT `\n` escape sequences.

**SmartAdmin Environment Requirement**:
- ✅ Use `<br/>` tags: `A[Line 1<br/>Line 2]`
- ❌ Do NOT use `\n`: `A["Line 1\nLine 2"]` (not supported in our rendering environment)

```mermaid
# ❌ 錯誤
graph TD
    A["Line 1\nLine 2"]

# ✅ 正確
graph TD
    A[Line 1<br/>Line 2]
```

**Note**: This differs from standard Mermaid specification, which recommends `\n`. SmartAdmin's rendering environment requires HTML tags.

**⚠️ IMPORTANT EXCEPTION**: stateDiagram-v2 does NOT support `<br/>` tags.

### stateDiagram-v2 Specific Rules

**CRITICAL**: stateDiagram-v2 has different syntax requirements than other Mermaid diagram types.

**❌ Do NOT use `<br/>` in stateDiagram-v2:**
- ❌ Transition labels: `A --> B: Event<br/>Action` (will fail)
- ❌ Note blocks: `note right of A : Text<br/>More` (will fail)

**✅ Correct stateDiagram-v2 syntax:**
```mermaid
# ✅ Correct - Simplified transition labels
stateDiagram-v2
    A --> B: Event

# ✅ Correct - Multi-line note blocks
stateDiagram-v2
    note right of A
        Event Triggered
        Action Executed
        Result Achieved
    end note
```

**When to use which approach:**
- **P0/P1 core documents**: Use multi-line note blocks to preserve detailed information
- **P2 supporting documents**: Simplify transition labels for quick fixes

**Automatic fix tools:**
- Detection: `./scripts/detect-statediagram-br.sh docs/iGaming/`
- Fix: `./scripts/batch-fix-statediagram-br.sh --verify`
- Validation: `./scripts/validate-mermaid.sh docs/iGaming/`

**Pre-commit Hook**: Automatically validates Mermaid syntax before commit (detects stateDiagram `<br/>` errors).

→ **[Complete Mermaid Best Practices](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/knowledge/mermaid-best-practices.md)** (v1.1.0 - Added stateDiagram error guide)

## Specialized Skills

**Quick Overview**: 35 skills in hierarchical structure (see [Complete Catalog](.claude/skills/README.md) for full details)

**Skills Organization (v4.0.0)**:
```
.claude/skills/
├── foundation/      (P0 - 6 skills: Critical foundation)
│   ├── backend/     (3 skills: ArchUnit, Security, Vavr)
│   ├── full-stack/  (2 skills: CRUD, Integration Test)
│   └── testing/     (1 skill: Test Fixture)
├── extended/        (P1 - 10 skills: Domain, Orchestration, Quality)
│   ├── domain/      (5 skills: iGaming + LiteFlow)
│   ├── orchestration/ (2 skills: batch-plan, quality-gate)
│   └── quality/     (3 skills: concurrency, spring-pattern, naming-checker) ⭐
├── productivity/    (P2 - 17 skills: DevOps, Integration, Composite, Analysis, Refactoring)
│   ├── devops/      (5 skills: APM, CI/CD, DB migration, scheduling, WebSocket)
│   ├── integration/ (6 skills: Cache, Search, i18n, MQ, Reports, PostgreSQL) ⭐ +1
│   ├── composite/   (2 skills: Performance suite, Testing suite)
│   ├── analysis/    (1 skill: Java performance profiler)
│   └── refactoring/ (3 skills: Vavr refactoring, Manager extractor, Markdown quality) ⭐ +1
└── lifecycle/       (Deprecated - 3 skills with migration guides)
```

**P0 Skills (Foundation)** - 6 skills:
- **Backend** (3 skills):
  - **[archunit-test-generator](.claude/skills/foundation/backend/archunit-test-generator/)** - Generate ArchUnit tests for architecture enforcement
  - **[security-hardening-pro](.claude/skills/foundation/backend/security-hardening-pro/)** - SM2/SM3/SM4 encryption, data masking, XSS/CSRF protection, audit logging
  - **[vavr-refactoring-assistant](.claude/skills/foundation/backend/vavr-refactoring-assistant/)** - Refactor Service layer to use Vavr Option/Try/Either patterns (moved from productivity/)
- **Full-stack** (2 skills):
  - **[smartadmin-crud-generator](.claude/skills/foundation/full-stack/smartadmin-crud-generator/)** - Composite full-stack CRUD (Backend + Frontend + API Docs + Tests) with phase-based execution
  - **[smartadmin-integration-test](.claude/skills/foundation/full-stack/smartadmin-integration-test/)** - Spring Boot integration tests with Testcontainers
- **Testing** (1 skill):
  - **[test-fixture-generator](.claude/skills/foundation/testing/test-fixture-generator/)** - Test data builders for complex domain objects

**P1 Skills (Extended)** - 10 skills:
- Domain (5 skills):
  - **[fraud-detection-pattern-generator](.claude/skills/extended/domain/fraud-detection-pattern-generator/)** - iGaming fraud detection, risk control, KYC/AML compliance
  - **[igame-feature-builder](.claude/skills/extended/domain/igame-feature-builder/)** - iGaming domain features (VIP system, Wallet API, Bonus engine)
  - **[igame-pm-analyst](.claude/skills/extended/domain/igame-pm-analyst/)** - iGaming產品經理分析助手 (Ultrathink深度分析、PRD生成)
  - **[igaming-multi-tenant-wallet-pm](.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/)** - Multi-tenant architecture & seamless wallet design (phase-based)
  - **[liteflow-rule-builder](.claude/skills/extended/domain/liteflow-rule-builder/)** - Generate LiteFlow DSL (EL expressions, QLExpress scripts) for business workflows
- Orchestration (2 skills):
  - **[batch-plan-executor](.claude/skills/extended/orchestration/batch-plan-executor/)** - Batch plan execution orchestrator with conflict detection
  - **[quality-gate-orchestrator](.claude/skills/extended/orchestration/quality-gate-orchestrator/)** - Multi-tool quality gate orchestration (Checkstyle, PMD, SpotBugs, ArchUnit)
- Quality (3 skills):
  - **[concurrency-safety-auditor](.claude/skills/extended/quality/concurrency-safety-auditor/)** - Concurrency safety audit with ⭐⭐⭐⭐⭐ risk rating, SpotBugs custom detectors, check-then-act pattern detection
  - **[spring-pattern-checker](.claude/skills/extended/quality/spring-pattern-checker/)** - Validate Spring patterns: @Transactional placement, dependency injection, layered architecture compliance
  - **[naming-convention-checker](.claude/skills/extended/quality/naming-convention-checker/)** - Validate SmartAdmin naming conventions (singular table names, class naming, field naming)

**P2 Skills (Productivity)** - 17 skills:
- DevOps (5): APM integration, CI/CD pipeline, DB migration, scheduled tasks, WebSocket/SSE
- Integration (6): Cache strategy, Elasticsearch, i18n, message queue, report export, **PostgreSQL best practices** ⭐
- Composite (2): smartadmin-performance-suite, smartadmin-testing-suite
- Analysis (1): java-performance-pro
- Refactoring (3): vavr-refactoring-assistant, smartadmin-manager-extractor, **markdown-quality-checker** ⭐

→ **[Complete Skills Catalog](.claude/skills/README.md)** - Full hierarchical structure, trigger keywords, and execution modes

### Week 4-5 New Skills (v3.0.0)

**1. postgresql-best-practices (P2 - Integration)**
- **Path**: `.claude/skills/productivity/integration/postgresql-best-practices/`
- **Purpose**: PostgreSQL performance analysis and optimization
- **Capabilities**:
  - HikariCP connection pool analysis (utilization formula: `connections = (core_count × 2) + effective_spindle_count`)
  - N+1 query detection via P6Spy log parsing
  - EXPLAIN ANALYZE automation for slow queries
  - Index recommendations from `pg_stat_user_tables`
- **Triggers**: "database performance", "PostgreSQL optimization", "HikariCP tuning", "N+1 query"
- **Output**: Comprehensive performance report (Markdown) with actionable recommendations
- **Time Saving**: Identifies 97.6% timeout error reductions in production cases

**2. smartadmin-manager-extractor (P2 - Refactoring)**
- **Path**: `.claude/skills/productivity/refactoring/smartadmin-manager-extractor/`
- **Purpose**: Auto-extract `@Transactional` methods from Service to Manager layer
- **Capabilities**:
  - JavaParser AST manipulation for code refactoring
  - ArchUnit test integration for violation detection
  - Triple validation: Compile + ArchUnit + Tests
  - Git-safe rollback mechanism
- **Triggers**: "extract to Manager", "transactionalMustUseRollbackForThrowable fails", "refactor transaction"
- **Time Saving**: 83% reduction (30 minutes → 5 minutes per refactoring)
- **Safety**: Creates Git stash before execution, auto-rollback on failure

**3. concurrency-safety-auditor (P1 - Quality)**
- **Path**: `.claude/skills/extended/quality/concurrency-safety-auditor/`
- **Purpose**: Concurrency safety audit with risk rating system
- **Capabilities**:
  - SpotBugs custom detectors for 8 concurrency patterns
  - ⭐⭐⭐⭐⭐ risk rating (Probability × Impact × Actual Harm)
  - Check-then-act pattern detection (non-atomic operations)
  - Double-checked locking detection
  - ConcurrentHashMap misuse detection
- **Triggers**: "concurrency", "thread safety", "race condition", "deadlock detection"
- **Output**: Concurrency audit report with risk assessment and fix recommendations
- **Risk Formula**: `Risk Score = (Probability × 0.4) + (Impact × 0.35) + (Actual Harm × 0.25)`

## Quality Tool Patterns

Common quality tool violations and approved solutions:

### PMD Suppressions
- **CallSuperInConstructor**: Empty constructors (default behavior is acceptable)
- **AvoidReassigningParameters**: Create local variable copy instead
- **ShortClassName**: Use `@SuppressWarnings` for utility inner classes (Dict, Expire)
- **MissingStaticMethodInNonInstantiatableClass**: Constant-only classes are valid

### SpotBugs Exclusions
- **EI_EXPOSE_REP/EI_EXPOSE_REP2**: DTO/VO/Form/Config classes don't need defensive copying
- **NP_NULL_ON_SOME_PATH**: CompletableFuture.getNow(null) and Kafka null keys are valid
- **ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD**: @PostConstruct static field initialization pattern
- **CT_CONSTRUCTOR_THROW**: Constructor validation pattern is safe for internal classes

Detailed rules: See [.agent/rules/quality-tools/12-pmd-rules.md](.agent/rules/quality-tools/12-pmd-rules.md) and [.agent/rules/quality-tools/13-spotbugs-rules.md](.agent/rules/quality-tools/13-spotbugs-rules.md)

---

## v4.0.0 Breaking Changes

**Bridge Class Removal**: All `net.lab1024.sa.common.core.*` bridge classes permanently removed.

**Key Changes:**
- ❌ OLD: `net.lab1024.sa.common.core.domain.ResponseDTO`
- ✅ NEW: `net.lab1024.sa.foundation.domain.response.ResponseDTO`
- ℹ️ **Exception**: `SmartBeanUtil` remains in `common.core.util.*`

**Migration Tool:**
```bash
cd smart-admin-api-java21-springboot3
./gradlew migrateToFoundation
```

→ **[Complete Package Migration Guide](docs/archive/migration/foundation-packages.md)** (archived)

---

## Documentation Structure

SmartAdmin 採用分層文檔組織結構，確保當前文檔和歷史資料清晰分離。

### 📁 Active Documentation (當前有效文檔)

```
docs/
├── migration/              # 遷移指南（當前有效）
│   └── foundation-packages.md  # v4.0.0 包命名遷移
├── testing/                # 測試文檔（當前有效）
│   ├── testing-strategy.md
│   ├── integration-testing-quick-reference.md
│   └── architecture/
├── plans/                  # 技術計畫（當前活躍）
│   ├── job/               # Snail-Job 排程計畫
│   ├── liteflow/          # LiteFlow 規則引擎計畫
│   ├── minio/             # MinIO 對象存儲計畫
│   └── tenant/            # 多租戶計畫
├── iGame/                  # iGaming 業務模塊
│   ├── architecture-decisions/  # ADR 記錄
│   └── technical-specs/         # 技術規格
└── audit/                  # 審計歷史追蹤
    └── AUDIT_HISTORY.md    # 架構審計記錄
```

### 🗄️ Archived Documentation (已歸檔文檔)

```
docs/archive/
├── INDEX.md                # 歸檔總索引
├── 2026-01-audit/          # 2026-01 架構審計報告
│   ├── README.md           # 審計報告版本說明
│   ├── ARCHITECTURE-AUDIT-REPORT.md (v1.0.0)
│   ├── ARCHITECTURE-AUDIT-REPORT-CORRECTED.md (v1.1.0)
│   └── ARCHITECTURE-AUDIT-SUCCESS-REPORT.md (v2.0.0)
├── legacy-kafka-v1/        # Kafka v1 文檔（46 個文件）
│   ├── INDEX.md
│   └── kafka/              # 架構、指南、範例、測試等
├── legacy-planning/        # 舊計劃文檔（11 個文件）
│   ├── INDEX.md
│   ├── FEASIBILITY-ANALYSIS-CORRECTION.md
│   ├── IMPLEMENTATION_PROGRESS.md
│   └── ...
├── evrete/                 # Evrete 規則引擎（已替換）
├── migration/              # 舊遷移報告（已完成）
└── testing/                # 舊測試文檔（已更新）
```

### 📝 Documentation Principles

**歸檔原則**：
- ✅ 已完成的階段性報告（審計、遷移等）
- ✅ 被新版本替代的技術文檔
- ✅ 已實施完成的計劃文檔
- ✅ 保留完整 Git 歷史用於追溯

**查找文檔**：
- **當前開發**：直接查看 `docs/` 對應子目錄
- **歷史追溯**：參考 [docs/archive/INDEX.md](docs/archive/INDEX.md)
- **審計記錄**：查看 [docs/audit/AUDIT_HISTORY.md](docs/audit/AUDIT_HISTORY.md)

→ **[Complete Archive Index](docs/archive/INDEX.md)** - 所有歸檔文檔的完整導航

---

## Documentation System Version

| Component | Version | Status | Metadata |
|-----------|---------|--------|----------|
| **This Document** | 3.4.0 | ✅ Universal AI Support + Java 21 | - |
| **AI Doc System** | 3.0.2 | ✅ Optimized | [.claude/META.md](.claude/META.md) |
| **.claude/** | 3.0.2 | ✅ Optimized | [.claude/README.md](.claude/README.md) |
| **.agent/** | 1.0.0 | ✅ Production Ready | [.agent/VERSION.md](.agent/VERSION.md) |
| **SmartAdmin** | v4.0.0 | ✅ Production | - |

**System Metadata**: [.claude/META.md](.claude/META.md) - Unified version tracking and content ownership

**Last Updated**: 2026-01-31

**Change History**:
- 3.4.0 (2026-01-31): Java 21 features documentation - Added dedicated Java 21 section covering Sealed Classes and Virtual Threads implementations, Phase 4 Manager layer testing completion (100% coverage, 8 test classes, 92 test cases)
- 3.3.0 (2026-01-27): Documentation structure update - Added "Documentation Structure" section with active/archived documentation organization, updated archive navigation with INDEX.md references
- 3.2.0 (2026-01-27): P1 improvements - Enhanced reading priority guidance with decision-making note, clarified code comment language standard, expanded skills catalog from 6 to 15 (P0: 6, P1: 3, P2: 6)
- 3.1.0 (2026-01-27): P0 critical fixes - Version synchronization (.agent/ v1.0.0 production release), fixed broken migration links
- 3.0.0 (2026-01-24): Content deduplication, universal AI support, unified version management
- 2.0.0 (2026-01-23): v4.0.0 breaking changes, foundation package documentation
- 1.0.0 (2026-01-22): Initial versioned release
