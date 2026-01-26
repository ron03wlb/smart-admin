---
trigger: always_on
description: Unified Decision Center - Rules, Skills, and Agent Routing
tags: [meta, ai-guide, decision-tree, orchestration, index]
positioning: current-standard
ai_role: orchestrator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/01-naming-conventions.md
  - foundation/10-architecture-rules.md
  - technology/functional/08-vavr-fundamentals.md
  - quality-tools/11-checkstyle-rules.md
last_updated: 2026-01-27
---

# SmartAdmin Unified Decision Center

> **Purpose**: Single source of truth for AI decision-making - rule selection, skill routing, and agent orchestration

**This index replaces**:
- ~~00-ai-decision-matrix.md~~ (merged)
- ~~.claude/shared/orchestration/decision-matrix.md~~ (merged)
- ~~CLAUDE.md decision tree examples~~ (referenced here)

---

## 🎯 Quick Navigation

| You Want To... | Go To Section |
|----------------|---------------|
| **Find the right rule file** | [Part 1: Rule Routing Table](#part-1-rule-routing-table) |
| **Choose a skill for complex tasks** | [Part 2: Skill Selection Logic](#part-2-skill-selection-logic) |
| **Select an agent for orchestration** | [Part 3: agent-selection-routing](#part-3-agent-selection-routing) |
| **Check quality gates** | [Part 4: Mandatory Check Matrix](#part-4-mandatory-check-matrix) |

---

## 🤖 AI Instruction Block

### When to Apply This Rule
- ✅ **Always**: Consult this index before every response
- ✅ When user initiates any code generation request
- ✅ When user requests Code Review
- ✅ When user reports errors requiring diagnosis
- ✅ When choosing which skill or agent to invoke

### Mandatory Enforcement Checklist
Before responding to user requests, must:
- [ ] Identify request type (code generation/review/diagnosis/query)
- [ ] Determine involved code layers (Controller/Service/Repository/Entity)
- [ ] Route to correct rule files using Part 1 table
- [ ] Consider invoking specialized skill (Part 2) for complex tasks
- [ ] Apply rules in priority order (P0 → P1 → P2 → P3)

---

## Part 1: Rule Routing Table

### 1.1 Quick Decision Tree

```
User Request Classification
├─ 1️⃣ Generate New Code
│   ├─ Controller: [foundation/01-naming, foundation/10-architecture, technology/patterns/04-exception-logging]
│   ├─ Service: [foundation/01-naming, foundation/02-oop, technology/functional/08-vavr, foundation/10-architecture]
│   ├─ Repository/Mapper: [foundation/01-naming, technology/database/09-mybatis-plus, technology/database/05-postgresql]
│   └─ Entity: [foundation/01-naming, technology/database/05-postgresql, technology/database/09-mybatis-plus]
│
├─ 2️⃣ Code Review
│   ├─ Architecture Violation: [foundation/10-architecture-rules] → ArchUnit
│   ├─ Naming Convention: [foundation/01-naming] → Checkstyle
│   ├─ Vavr Usage: [technology/functional/08-vavr-*] → Option/Try checks
│   ├─ OOP Principles: [foundation/02-oop-principles]
│   ├─ Concurrency Safety: [technology/patterns/03-concurrency-rules]
│   └─ Security Check: [security/07-owasp-top10-*]
│
├─ 3️⃣ Database Operations
│   ├─ Table Creation: [technology/database/05-postgresql-basics]
│   ├─ JSONB/CTE/Window Functions: [technology/database/05-postgresql-advanced]
│   ├─ MyBatis Mapper: [technology/database/09-mybatis-plus-*]
│   └─ MySQL to PG Migration: [technology/database/05-postgresql-mybatis]
│
├─ 4️⃣ Error Diagnosis
│   ├─ Compilation Error: Workflow [java-failure-recovery]
│   ├─ ArchUnit Failure: [foundation/10-architecture] + corresponding rules
│   ├─ Quality Gate Failure: Workflow [quality-gates-local-ci]
│   └─ Runtime Error: [technology/functional/08-vavr (Try), technology/patterns/04-exception-logging]
│
├─ 5️⃣ Static Analysis Tools
│   ├─ Checkstyle: [quality-tools/11-checkstyle-rules] → ./gradlew checkstyleMain
│   ├─ PMD: [quality-tools/12-pmd-rules] → ./gradlew pmdMain
│   ├─ SpotBugs: [quality-tools/13-spotbugs-rules] → ./gradlew spotbugsMain
│   ├─ Spotless: [quality-tools/14-spotless-rules] → ./gradlew spotlessApply
│   ├─ Error Prone: [quality-tools/15-error-prone-rules]
│   └─ JaCoCo: [quality-tools/16-jacoco-coverage-rules]
│
└─ 6️⃣ Knowledge Query
    ├─ PostgreSQL: [technology/database/05-postgresql-*]
    ├─ Vavr: [technology/functional/08-vavr-*]
    ├─ MyBatis Plus: [technology/database/09-mybatis-plus-*]
    └─ Architecture Design: [foundation/10-architecture-rules]
```

### 1.2 Rule Routing by Task Type

| Task | Rule Files | Path |
|------|-----------|------|
| **Create Controller** | Naming, Architecture, Exception | [foundation/01-naming](foundation/01-naming-conventions.md)<br>[foundation/10-architecture](foundation/10-architecture-rules.md)<br>[technology/patterns/04-exception-logging](technology/patterns/04-exception-logging.md) |
| **Create Service** | Naming, OOP, Vavr, Architecture | [foundation/01-naming](foundation/01-naming-conventions.md)<br>[foundation/02-oop](foundation/02-oop-principles.md)<br>[technology/functional/08-vavr](technology/functional/08-vavr-fundamentals.md)<br>[foundation/10-architecture](foundation/10-architecture-rules.md) |
| **Create Mapper/Dao** | Naming, MyBatis Plus, PostgreSQL | [foundation/01-naming](foundation/01-naming-conventions.md)<br>[technology/database/09-mybatis-plus-core](technology/database/09-mybatis-plus-core.md)<br>[technology/database/05-postgresql-basics](technology/database/05-postgresql-basics.md) |
| **Create Entity** | Naming, PostgreSQL, MyBatis Plus | [foundation/01-naming](foundation/01-naming-conventions.md)<br>[technology/database/05-postgresql-basics](technology/database/05-postgresql-basics.md)<br>[technology/database/09-mybatis-plus-core](technology/database/09-mybatis-plus-core.md) |
| **PostgreSQL Table** | PostgreSQL Basics | [technology/database/05-postgresql-basics](technology/database/05-postgresql-basics.md) |
| **JSONB Operations** | PostgreSQL Advanced | [technology/database/05-postgresql-advanced](technology/database/05-postgresql-advanced.md) |
| **MyBatis TypeHandlers** | PostgreSQL MyBatis Integration | [technology/database/05-postgresql-mybatis](technology/database/05-postgresql-mybatis.md) |
| **Optimize Queries** | PostgreSQL Advanced, MyBatis Integration | [technology/database/05-postgresql-advanced](technology/database/05-postgresql-advanced.md)<br>[technology/database/05-postgresql-mybatis](technology/database/05-postgresql-mybatis.md) |
| **Vavr Refactoring** | Vavr Fundamentals, Vavr MyBatis | [technology/functional/08-vavr-fundamentals](technology/functional/08-vavr-fundamentals.md)<br>[technology/functional/08-vavr-mybatis-integration](technology/functional/08-vavr-mybatis-integration.md) |
| **Concurrency Issues** | Concurrency Rules | [technology/patterns/03-concurrency-rules](technology/patterns/03-concurrency-rules.md) |
| **Exception Handling** | Exception Logging, Vavr | [technology/patterns/04-exception-logging](technology/patterns/04-exception-logging.md)<br>[technology/functional/08-vavr-fundamentals](technology/functional/08-vavr-fundamentals.md) |
| **Security Audit** | OWASP Top 10 | [security/07-owasp-top10-part1](security/07-owasp-top10-part1.md)<br>[security/07-owasp-top10-part2](security/07-owasp-top10-part2.md) |
| **Manager Layer** | Manager Layer Patterns | [foundation/09-manager-layer](foundation/09-manager-layer.md) |
| **Commit Message** | Commit Conventions | [workflows/17-commit-message-conventions](workflows/17-commit-message-conventions.md) |
| **Quality Gate** | All Quality Tools | [quality-tools/](quality-tools/) |

### 1.3 Rule Files by Category

#### Foundation (Core Architecture)
| File | Description | Path |
|------|-------------|------|
| 01-naming-conventions.md | Naming rules for classes, methods, variables | [foundation/01-naming-conventions.md](foundation/01-naming-conventions.md) |
| 02-oop-principles.md | OOP best practices and SOLID principles | [foundation/02-oop-principles.md](foundation/02-oop-principles.md) |
| 09-manager-layer.md | Manager layer transaction patterns | [foundation/09-manager-layer.md](foundation/09-manager-layer.md) |
| 10-architecture-rules.md | Layered architecture constraints (ArchUnit) | [foundation/10-architecture-rules.md](foundation/10-architecture-rules.md) |

#### Technology/Database
| File | Description | Path |
|------|-------------|------|
| 05-postgresql-basics.md | Table creation, types, indexes | [technology/database/05-postgresql-basics.md](technology/database/05-postgresql-basics.md) |
| 05-postgresql-advanced.md | JSONB, arrays, CTE, window functions | [technology/database/05-postgresql-advanced.md](technology/database/05-postgresql-advanced.md) |
| 05-postgresql-mybatis.md | **PostgreSQL + MyBatis Plus Integration**: Configuration, TypeHandlers (JSONB/Arrays), SQL Optimization, MySQL Migration, Vavr Integration, Performance Tuning | [technology/database/05-postgresql-mybatis.md](technology/database/05-postgresql-mybatis.md) |
| 09-mybatis-plus-core.md | LambdaQueryWrapper, pagination, IEnum | [technology/database/09-mybatis-plus-core.md](technology/database/09-mybatis-plus-core.md) |
| ~~05-postgresql-mybatis-integration.md~~ | ⚠️ Deprecated → Redirects to 05-postgresql-mybatis.md | [technology/database/05-postgresql-mybatis-integration.md](technology/database/05-postgresql-mybatis-integration.md) |
| ~~09-mybatis-plus-postgresql.md~~ | ⚠️ Deprecated → Redirects to 05-postgresql-mybatis.md | [technology/database/09-mybatis-plus-postgresql.md](technology/database/09-mybatis-plus-postgresql.md) |

#### Technology/Functional
| File | Description | Path |
|------|-------------|------|
| 08-vavr-fundamentals.md | Option, Try, functional patterns | [technology/functional/08-vavr-fundamentals.md](technology/functional/08-vavr-fundamentals.md) |
| 08-vavr-advanced.md | Either, collections, pattern matching | [technology/functional/08-vavr-advanced.md](technology/functional/08-vavr-advanced.md) |
| 08-vavr-mybatis-integration.md | Vavr + MyBatis Plus integration | [technology/functional/08-vavr-mybatis-integration.md](technology/functional/08-vavr-mybatis-integration.md) |

#### Technology/Patterns
| File | Description | Path |
|------|-------------|------|
| 03-concurrency-rules.md | Thread safety, concurrent collections | [technology/patterns/03-concurrency-rules.md](technology/patterns/03-concurrency-rules.md) |
| 04-exception-logging.md | Exception handling and logging patterns | [technology/patterns/04-exception-logging.md](technology/patterns/04-exception-logging.md) |

#### Security
| File | Description | Path |
|------|-------------|------|
| 07-owasp-top10-part1.md | OWASP Top 10 (1-5) | [security/07-owasp-top10-part1.md](security/07-owasp-top10-part1.md) |
| 07-owasp-top10-part2.md | OWASP Top 10 (6-10) | [security/07-owasp-top10-part2.md](security/07-owasp-top10-part2.md) |

#### Quality Tools
| File | Description | Path |
|------|-------------|------|
| 11-checkstyle-rules.md | Checkstyle configuration and patterns | [quality-tools/11-checkstyle-rules.md](quality-tools/11-checkstyle-rules.md) |
| 12-pmd-rules.md | PMD rules and suppressions | [quality-tools/12-pmd-rules.md](quality-tools/12-pmd-rules.md) |
| 13-spotbugs-rules.md | SpotBugs exclusions and patterns | [quality-tools/13-spotbugs-rules.md](quality-tools/13-spotbugs-rules.md) |
| 14-spotless-rules.md | Code formatting standards | [quality-tools/14-spotless-rules.md](quality-tools/14-spotless-rules.md) |
| 15-error-prone-rules.md | Error Prone patterns | [quality-tools/15-error-prone-rules.md](quality-tools/15-error-prone-rules.md) |
| 16-jacoco-coverage-rules.md | Test coverage requirements | [quality-tools/16-jacoco-coverage-rules.md](quality-tools/16-jacoco-coverage-rules.md) |

#### Workflows
| File | Description | Path |
|------|-------------|------|
| 06-sonarqube-rules.md | SonarQube quality gate rules | [workflows/06-sonarqube-rules.md](workflows/06-sonarqube-rules.md) |
| 17-commit-message-conventions.md | Conventional Commits format | [workflows/17-commit-message-conventions.md](workflows/17-commit-message-conventions.md) |

---

## Part 2: Skill Selection Logic

### 2.1 When to Invoke Skills

**Skills are for complex, multi-step tasks** that require:
- Specialized domain knowledge (iGaming, fraud detection, workflow orchestration)
- Multi-phase generation (CRUD module, testing suite, performance investigation)
- Automated workflows (diagnose → optimize → monitor)

**Use direct code generation for**:
- Simple single-file changes
- Basic CRUD operations
- Straightforward refactoring

### 2.2 Skill Routing Table

| User Keywords | Recommended Skill | Reason | Example |
|--------------|-------------------|--------|---------|
| **CRUD Module Generation** ||||
| "create CRUD module", "generate full-stack feature", "Employee CRUD" | **smartadmin-crud-generator** | Complete backend + frontend + tests + docs | `/crud Product --all-phases` |
| "generate backend only", "create Dao/Manager/Service" | **smartadmin-crud-generator --backend-only** | Backend-only generation | `/crud Order --backend-only` |
| "create Vue component", "frontend CRUD" | **smartadmin-crud-generator --frontend-only** | Frontend-only generation | `/crud Customer --frontend-only` |
| **Testing** ||||
| "create integration test", "test with database" | **smartadmin-testing-suite --mode=integration** | Service layer integration tests with Testcontainers | `/test EmployeeService --mode=integration` |
| "create test fixture", "test data builder" | **smartadmin-testing-suite --mode=fixtures** | Test fixture generators with AtomicInteger | `/test Employee --mode=fixtures` |
| **Performance Optimization** ||||
| "slow endpoint", "N+1 queries", "diagnose performance" | **smartadmin-performance-suite --mode=diagnose** | Profile, detect N+1, JVM analysis | `/performance /api/employees/list --mode=diagnose` |
| "implement caching", "optimize query" | **smartadmin-performance-suite --mode=optimize** | Multi-level caching (Caffeine + Redis) | `/performance ProductService --mode=optimize` |
| "setup monitoring", "APM integration" | **smartadmin-performance-suite --mode=monitor** | Skywalking, Micrometer, Grafana | `/performance OrderService --mode=monitor` |
| "complete performance investigation" | **smartadmin-performance-suite --workflow** | Integrated diagnose → optimize → monitor | `/performance /api/orders/query --workflow` |
| **Functional Programming** ||||
| "refactor to Vavr", "convert Optional", "use Try" | **vavr-refactoring-assistant** | Refactor Optional → Option, try-catch → Try | `/vavr UserService.findById` |
| **Architecture Testing** ||||
| "generate ArchUnit test", "enforce architecture rule" | **archunit-test-generator** | Generate architecture tests from .agent/rules/*.md | `/archunit serviceUsesVavrOption` |
| **Workflow Orchestration** ||||
| "create workflow", "LiteFlow rule", "approval flow" | **liteflow-rule-builder** | Generate LiteFlow EL + QLExpress rules | `/liteflow "order approval with risk check"` |
| **Fraud Detection (iGaming)** ||||
| "fraud detection", "multi-account", "bonus abuse", "risk control" | **fraud-detection-pattern-generator** | iGaming fraud patterns with real-time scoring | `/fraud multi-account-detection` |
| **Quality Gate** ||||
| "quality gate", "pre-merge check", "CI/CD validation" | **quality-gate-orchestrator** | Multi-tool orchestration (ArchUnit, Checkstyle, PMD, SpotBugs) | `/quality-gate check` |

### 2.3 Skill Combination Patterns

#### Pattern 1: Complete CRUD Module
```bash
/crud Employee --all-phases
# Generates: Backend + Frontend + Tests + API Docs (~20 minutes)
```

#### Pattern 2: Performance Investigation Workflow
```bash
/performance /api/employees/list --workflow
# Runs: Diagnose → Optimize → Monitor (~30 minutes)
```

#### Pattern 3: TDD with Integration Tests
```bash
/test EmployeeService --mode=integration
# Generates: BaseIntegrationTest + fixtures + integration tests
```

---

## Part 3: Agent Selection Routing

### 3.1 Agent Decision Flow

```
[User Request]
    ↓
Is it a code quality/pre-merge review? ──YES──→ code-reviewer
    ↓ NO
Is it an architecture/design review? ──YES──→ architect-reviewer
    ↓ NO
Is it about Java code implementation? ──YES──→ java-architect
    ↓ NO
Is it about Vue/Frontend implementation? ──YES──→ vue-expert
    ↓ NO
Is it about requirements/process/stakeholders? ──YES──→ business-analyst
    ↓ NO
Is it about deployment/CI-CD/infrastructure? ──YES──→ devops-engineer
    ↓ NO
Is it PostgreSQL database specific? ──YES──→ postgres-pro
    ↓ NO
Is it about resilience/chaos testing? ──YES──→ chaos-engineer
    ↓ NO
Is it about documentation creation? ──YES──→ documentation-engineer
    ↓ NO
Use general-purpose or ask user for clarification
```

### 3.2 Agent Keyword Mapping

| Keywords in Request | Agent | Confidence | Example Requests |
|---------------------|-------|------------|------------------|
| **Java Development** ||||
| Spring Boot, @Transactional, @Service, Controller, REST API, layering, MyBatis Plus, entity, service layer | **java-architect** | High | "implement employee API", "add REST endpoint", "optimize JPA queries", "fix N+1 problem", "review service code" |
| **Frontend Development** ||||
| Vue, Component, Frontend, UI, Ant Design Vue, Composition API, form-modal, v-privilege, Pinia, reactive, Vite | **vue-expert** | High | "create employee list page", "implement form validation", "integrate backend API", "add permission controls", "optimize Vue performance" |
| **Business Analysis** ||||
| requirements, stakeholders, ROI, business process, user story, workflow, acceptance criteria, KPI, metrics | **business-analyst** | High | "gather requirements", "analyze process", "improve workflow", "calculate ROI", "define success metrics" |
| **DevOps & Deployment** ||||
| deploy, CI/CD, Docker, Kubernetes, pipeline, container, infrastructure, monitoring, Prometheus, Grafana | **devops-engineer** | High | "setup deployment", "configure monitoring", "create pipeline", "containerize app", "deploy to production" |
| **Database** ||||
| PostgreSQL, query optimization, index, replication, pg_stat, slow query, database performance | **postgres-pro** | High | "optimize database", "setup replication", "analyze query performance", "create indexes", "backup strategy" |
| **Resilience** ||||
| resilience, chaos, failure injection, game day, circuit breaker, fallback, antifragility, disaster recovery | **chaos-engineer** | High | "test failover", "improve resilience", "design chaos experiment", "validate recovery", "test failure scenarios" |
| **Architecture Review** ||||
| architecture, design, scalability, pattern validation, layer boundaries, module structure, technical debt assessment, architectural patterns, refactoring strategy | **architect-reviewer** | High | "review architecture", "validate design", "assess scalability", "evaluate module structure", "identify technical debt", "architecture audit" |
| **Code Quality Review** ||||
| code quality, security review, pull request, pre-merge, quality gate, code standards, vulnerability, code review, best practices, maintainability | **code-reviewer** | High | "review code", "pre-merge review", "check code quality", "security audit", "validate standards", "quality gate check" |
| **Documentation** ||||
| documentation, API docs, README, tutorial, architecture guide, user guide, developer docs, Swagger, OpenAPI, technical writing, doc generation | **documentation-engineer** | High | "document API", "update README", "create architecture guide", "write tutorial", "generate API docs", "document layered architecture" |

### 3.3 Multi-Agent Orchestration Scenarios

#### Scenario 1: New Full-Stack Feature
**Request:** "Add employee performance review feature"

**Agent Sequence:**
1. **business-analyst** (first) - Gather requirements, define user stories, create process flows
2. **java-architect** (second) - Implement backend API (Controller → Service → Dao)
3. **vue-expert** (third) - Implement frontend pages (list, form-modal) and integrate with backend API
4. **postgres-pro** (if complex queries) - Optimize database performance
5. **devops-engineer** (fourth) - Deploy to staging/production
6. **chaos-engineer** (fifth) - Validate resilience of critical path

#### Scenario 2: Performance Problem
**Request:** "Employee search is slow"

**Parallel Investigation:**
- **java-architect** (lead) - Review code, check N+1 queries, caching strategy
- **postgres-pro** (parallel) - Analyze query execution plans, check indexes
- Converge on integrated solution

**Then:**
- **devops-engineer** - Deploy optimization, monitor improvements

#### Scenario 3: Pre-Merge Code Review (Quality Gate)
**Request:** "I've finished the notification feature, please check if it's ready for merge"

**Hub-and-Spoke Pattern:**
1. **code-reviewer** (hub) - Initial scan for security, correctness, performance, maintainability
2. **Dispatch to specialists** based on change scope:
   - Backend changes → **java-architect** (SmartAdmin patterns validation)
   - Frontend changes → **vue-expert** (Vue 3 best practices)
   - Database changes → **postgres-pro** (query optimization)
   - Architectural impact → **architect-reviewer** (layer boundary validation)
3. **code-reviewer** (consolidate) - Aggregate findings, determine pass/fail
4. **java-architect** or **vue-expert** (fix) - Address critical/major issues
5. **code-reviewer** (validate) - Re-check after fixes, approve merge or iterate

#### Scenario 4: Architecture Review
**Request:** "Can you review the architecture of our employee module?"

**Agent Sequence:**
1. **architect-reviewer** (lead) - Comprehensive architecture assessment, validate layered architecture, check dependencies
2. **java-architect** (if code changes needed) - Implement recommended refactoring
3. **vue-expert** (if frontend architectural impact) - Review frontend architecture alignment
4. **devops-engineer** (if infrastructure impact) - Assess deployment and scaling implications
5. **postgres-pro** (if database architectural concerns) - Evaluate database schema and query patterns

---

## Part 4: Mandatory Check Matrix

| Code Type         | Required Rules                       | Automation Tool | Blocking Level |
| ----------------- | ------------------------------------ | --------------- | -------------- |
| Service New Method | [technology/functional/08-vavr](technology/functional/08-vavr-fundamentals.md) (Option/Try) | ArchUnit | 🚫 Block PR |
| Service New Method | [foundation/10-architecture](foundation/10-architecture-rules.md) (Constructor Injection) | ArchUnit | 🚫 Block PR |
| Controller New Method | [foundation/10-architecture](foundation/10-architecture-rules.md) (No Direct Repo Access) | ArchUnit | 🚫 Block PR |
| Any New Code      | [foundation/01-naming](foundation/01-naming-conventions.md) (Naming Convention) | Checkstyle | 🚫 Block PR |
| Any New Code      | Test Coverage ≥ 80%                  | JaCoCo | 🚫 Block PR |

### Quality Gate Pass Criteria

```yaml
Quality Gate Standards:
  ✅ ArchUnit:        100% pass (zero tolerance)
  ✅ Checkstyle:      0 errors
  ✅ PMD:             0 violations
  ✅ SpotBugs:        0 bugs
  ✅ Test Coverage:   ≥ 80% (Line), ≥ 70% (Branch)
  ✅ SonarQube:       0 Blocker/Critical issues
```

**Validation Commands:**
```bash
./gradlew check                    # Complete check
mvn test -Dtest=ArchitectureTest   # Architecture test
```

---

## Part 5: AI Auto-Fix Strategy

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

## Part 6: Rule Priority

| Priority    | Rule Type                     | Description          | Rules |
| ----------- | ----------------------------- | -------------------- | ----- |
| P0 Highest  | Architecture, Security        | Architecture/Security | [foundation/10-architecture](foundation/10-architecture-rules.md)<br>[security/07-owasp-*](security/) |
| P1 High     | Functional, Naming, OOP       | Code Quality         | [technology/functional/08-vavr](technology/functional/08-vavr-fundamentals.md)<br>[foundation/01-naming](foundation/01-naming-conventions.md)<br>[foundation/02-oop](foundation/02-oop-principles.md) |
| P2 Medium   | Database, Best Practices      | Best Practices       | [technology/database/09-mybatis](technology/database/09-mybatis-plus-core.md)<br>[technology/database/05-postgresql](technology/database/05-postgresql-basics.md) |
| P3 Low      | Concurrency, Quality Tools    | Optimization Tips    | [technology/patterns/03-concurrency](technology/patterns/03-concurrency-rules.md)<br>[workflows/06-sonarqube](workflows/06-sonarqube-rules.md) |

### Conflict Resolution Principles
1. **Security > Performance > Readability**
2. **Architecture Constraints > Code Style**
3. **New Code High Standards > Legacy Compatibility**

---

## Quick Reference Card

| Keyword               | Apply Rules/Skills/Agents          | Checkpoint                         |
| --------------------- | ---------------------------------- | ---------------------------------- |
| "Create Controller"   | Rules: [01](foundation/01-naming-conventions.md), [10](foundation/10-architecture-rules.md), [04](technology/patterns/04-exception-logging.md) | RESTful, No Direct Repo Access |
| "Create Service"      | Rules: [01](foundation/01-naming-conventions.md), [02](foundation/02-oop-principles.md), [08-vavr](technology/functional/08-vavr-fundamentals.md), [10](foundation/10-architecture-rules.md) | Option/Try, Constructor Injection |
| "Create Mapper"       | Rules: [01](foundation/01-naming-conventions.md), [09](technology/database/09-mybatis-plus-core.md), [05](technology/database/05-postgresql-basics.md) | LambdaQueryWrapper |
| "Create Entity"       | Rules: [01](foundation/01-naming-conventions.md), [05](technology/database/05-postgresql-basics.md) | @TableName, JSONB/Array |
| "CRUD module"         | Skill: **smartadmin-crud-generator** | `/crud Product --all-phases` |
| "integration test"    | Skill: **smartadmin-testing-suite** | `/test EmployeeService --mode=integration` |
| "slow query"          | Skill: **smartadmin-performance-suite** | `/performance /api/orders --workflow` |
| "Code Review"         | Agent: **code-reviewer** → Rules: [10](foundation/10-architecture-rules.md), [08](technology/functional/08-vavr-fundamentals.md), [01](foundation/01-naming-conventions.md) | ArchUnit, Vavr, Naming |
| "Architecture Review" | Agent: **architect-reviewer** | Layer boundaries, dependencies |
| "Java implementation" | Agent: **java-architect** | Spring Boot patterns |
| "Vue component"       | Agent: **vue-expert** | Vue 3 + Ant Design Vue |
| "JSONB"               | Rules: [05-advanced](technology/database/05-postgresql-advanced.md), [05-mybatis](technology/database/05-postgresql-mybatis.md) | TypeHandler, SQL Optimization |
| "Exception Handling"  | Rules: [08-vavr](technology/functional/08-vavr-fundamentals.md), [04](technology/patterns/04-exception-logging.md) | Try.of(), Logging |
| "Transaction"         | Rules: [09-manager](foundation/09-manager-layer.md) | @Transactional in Manager |
| "commit"              | Rules: [17-commit](workflows/17-commit-message-conventions.md) | Conventional Commits Format |
| "workflow"            | Skill: **liteflow-rule-builder** | LiteFlow EL + QLExpress |
| "fraud detection"     | Skill: **fraud-detection-pattern-generator** | Multi-account, Bonus abuse |
| "quality gate"        | Skill: **quality-gate-orchestrator** | ArchUnit, Checkstyle, PMD, SpotBugs |
| "deploy"              | Agent: **devops-engineer** | CI/CD, Docker, Kubernetes |
| "optimize database"   | Agent: **postgres-pro** | Query plans, indexes |
| "resilience"          | Agent: **chaos-engineer** | Chaos experiments |

---

## Core Checklist

### Basic Checks
- [ ] Class names UpperCamelCase, method names lowerCamelCase
- [ ] Constructor injection (no @Autowired fields)

### Architecture Checks
- [ ] Controller does not directly access Repository
- [ ] @Transactional/@Cacheable only in Manager layer

### Quality Checks
- [ ] Test coverage ≥ 80%
- [ ] ArchUnit tests pass
- [ ] All quality tools pass (Checkstyle, PMD, SpotBugs)

---

**This unified index is the master entry point for all AI decision-making. Consult this document before handling any request.**
