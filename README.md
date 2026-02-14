# SmartAdmin - Enterprise Management System

**Java 21 + Spring Boot 3 + Vue 3** | Modular Monolith Architecture

---

## 🚀 Quick Start

Choose your role to get started:

- **👨‍💻 Developer (Quick Reference)** → [CLAUDE.md](CLAUDE.md) - Commands, patterns, conventions
- **🤖 AI-Assisted Development** → [.claude/](.claude/README.md) - Multi-agent workflows and orchestration
- **🏗️ Architecture & Standards** → [.agent/rules/](.agent/rules/) - Comprehensive coding standards
- **📚 Detailed Patterns** → [.claude/shared/knowledge/](.claude/shared/knowledge/) - SmartAdmin implementation patterns
- **📖 Infrastructure Guides** → [docs/](docs/README.md) - Kafka, MinIO, multi-tenant, testing

---

## 📖 Documentation Navigator

**"I want to..."** → **Go to:**

| Goal | Documentation |
|------|---------------|
| **Find a quick command** | [CLAUDE.md](CLAUDE.md) - Build, test, run commands |
| **Learn SmartAdmin patterns** | [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md) - ResponseDTO, pagination, transactions |
| **Understand the architecture** | [Project Architecture](.claude/shared/knowledge/project-architecture.md) - Module structure, build system |
| **Fix code quality issues** | [Quality Standards](.claude/shared/knowledge/quality-standards.md) - Anti-patterns, checklist |
| **Use an AI agent** | [.claude/README.md](.claude/README.md) - Agent selection guide |
| **Follow coding standards** | [.agent/rules/](.agent/rules/) - Architecture, naming, PMD, SpotBugs rules |
| **Set up Kafka integration** | [docs/kafka/](docs/kafka/getting-started/quick-start.md) - Complete Kafka guide |
| **Configure MinIO storage** | [docs/minio/](docs/minio/01-quick-start.md) - Object storage setup |
| **Learn iGaming platform** | [docs/iGaming/](docs/iGaming/README.md) - Player, Finance, Risk, Activity modules |
| **Run architecture tests** | [CLAUDE.md](CLAUDE.md#test-commands) - `./gradlew :sa-admin:test --tests ArchitectureTest` |
| **Commit code properly** | [Commit Conventions](.agent/rules/workflows/17-commit-message-conventions.md) - Conventional Commits format |

---

## 🏗️ Project Structure

```
smart-admin/
├── smart-admin-api-java21-springboot3/    # Backend (Java 21 + Spring Boot 3)
│   ├── sa-base/                           # Infrastructure, utilities, support
│   ├── sa-admin/                          # Business logic, system modules
│   └── sa-common/                         # Shared services (cache, MQ, redis-lock)
├── smart-admin-web/                       # Frontend (Vue 3 + Vite + TypeScript)
├── docs/                                  # Infrastructure documentation
│   ├── iGaming/                           # iGaming platform docs (133+ files)
│   ├── kafka/                             # Kafka integration guide (46 docs)
│   ├── minio/                             # MinIO object storage guide (8 docs)
│   ├── multi-tenant/                      # Multi-tenant architecture (5 docs)
│   └── testing/                           # Testing strategies (5 docs)
├── .claude/                               # AI agent system
│   ├── agents/                            # Specialized agents
│   ├── shared/knowledge/                  # SmartAdmin patterns, architecture, quality
│   └── scripts/                           # Automation scripts
├── .agent/rules/                          # Coding standards (architecture, naming, PMD)
├── CLAUDE.md                              # Quick reference for developers
└── README.md                              # ← You are here (primary navigation)
```

---

## ⚡ Quick Commands

```bash
# Build backend
cd smart-admin-api-java21-springboot3
./gradlew clean build

# Run backend (http://localhost:1024)
./gradlew :sa-admin:bootRun

# Run unit tests (CI default - no Redis required)
./gradlew :sa-admin:test

# Run integration tests (requires Docker for Redis/PostgreSQL)
./gradlew :sa-admin:integrationTest

# Run architecture tests
./gradlew :sa-admin:test --tests ArchitectureTest

# Start frontend (http://localhost:5173)
cd smart-admin-web
npm run dev
```

→ **[All Build Commands](.claude/shared/knowledge/project-architecture.md#build-commands)**

---

## 🤖 AI-Assisted Development

SmartAdmin includes a multi-agent system to accelerate development:

| Agent | Use Case | Documentation |
|-------|----------|---------------|
| **java-architect** | Java/Spring Boot implementation, layered architecture | [.claude/agents/java-architect/](.claude/agents/java-architect/) |
| **code-reviewer** | Code quality review, best practices validation | [.claude/agents/code-reviewer/](.claude/agents/code-reviewer/) |
| **vue-expert** | Vue 3 components, Ant Design integration | [.claude/agents/vue-expert/](.claude/agents/vue-expert/) |
| **documentation-engineer** | API docs, tutorials, architecture guides | [.claude/agents/documentation-engineer/](.claude/agents/documentation-engineer/) |
| **devops-engineer** | CI/CD, deployment, monitoring | [.claude/agents/devops-engineer/](.claude/agents/devops-engineer/) |
| **business-analyst** | Requirements analysis, process optimization | [.claude/agents/business-analyst/](.claude/agents/business-analyst/) |
| **architect-reviewer** | Architecture evaluation, design decisions | [.claude/agents/architect-reviewer/](.claude/agents/architect-reviewer/) |

→ **[Complete Agent Guide](.claude/README.md)**

---

## 📚 Documentation Index

| Category | Files | Purpose |
|----------|-------|---------|
| **Quick Reference** | [CLAUDE.md](CLAUDE.md) | Developer quick reference card |
| **AI Agents** | [.claude/README.md](.claude/README.md) | Multi-agent system overview |
| **SmartAdmin Patterns** | [.claude/shared/knowledge/smartadmin-patterns.md](.claude/shared/knowledge/smartadmin-patterns.md) | ResponseDTO, pagination, transactions, validation |
| **Project Architecture** | [.claude/shared/knowledge/project-architecture.md](.claude/shared/knowledge/project-architecture.md) | Module structure, build system, Gradle configuration |
| **Quality Standards** | [.claude/shared/knowledge/quality-standards.md](.claude/shared/knowledge/quality-standards.md) | Anti-patterns, code quality checklist |
| **Coding Standards** | [.agent/rules/](.agent/rules/) | Architecture, naming, PMD, SpotBugs rules |
| **Architecture Rules** | [.agent/rules/foundation/10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md) | Layered architecture enforcement |
| **Manager Layer** | [.agent/rules/foundation/09-manager-layer.md](.agent/rules/foundation/09-manager-layer.md) | Transaction, caching constraints |
| **Naming Conventions** | [.agent/rules/foundation/01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md) | Alibaba naming guidelines |
| **Exception & Logging** | [.agent/rules/technology/patterns/04-exception-logging.md](.agent/rules/technology/patterns/04-exception-logging.md) | Exception handling, logging standards |
| **Commit Messages** | [.agent/rules/workflows/17-commit-message-conventions.md](.agent/rules/workflows/17-commit-message-conventions.md) | Conventional Commits format |
| **Kafka Guide** | [docs/kafka/](docs/kafka/) | Complete Kafka integration (46 documents) |
| **MinIO Guide** | [docs/minio/](docs/minio/) | Object storage setup (8 documents) |
| **Multi-Tenant** | [docs/multi-tenant/](docs/multi-tenant/) | Multi-tenant architecture (5 documents) |
| **Testing** | [docs/testing/](docs/testing/) | Testing strategies (5 documents) |

---

## 🛠️ Technology Stack

| Component | Version | Documentation |
|-----------|---------|---------------|
| Java | 21 | [Oracle Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) |
| Spring Boot | 3.5.4 | [Backend README](./smart-admin-api-java21-springboot3/README.md) |
| MyBatis Plus | 3.5.12 | [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md) |
| Sa-Token | 1.44.0 | [CLAUDE.md](CLAUDE.md#authentication-sa-token) |
| Vue | 3 | [Frontend README](./smart-admin-web/README.md) |
| Vite | Latest | [Frontend README](./smart-admin-web/README.md) |
| Ant Design Vue | Latest | [Frontend README](./smart-admin-web/README.md) |

---

## 🏃 Getting Started

### Backend Setup

```bash
# Navigate to backend
cd smart-admin-api-java21-springboot3

# Build project
./gradlew clean build

# Run application (http://localhost:1024/swagger-ui.html)
./gradlew :sa-admin:bootRun
```

**Detailed instructions**: [Backend README](./smart-admin-api-java21-springboot3/README.md)

### Frontend Setup

```bash
# Navigate to frontend
cd smart-admin-web

# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev
```

**Detailed instructions**: [Frontend README](./smart-admin-web/README.md)

---

## 🧪 Architecture Validation

SmartAdmin enforces layered architecture through ArchUnit tests:

```bash
# Run all architecture tests
./gradlew :sa-admin:test --tests ArchitectureTest

# Run specific test
./gradlew :sa-admin:test --tests ArchitectureTest.testControllerShouldOnlyDependOnService
```

**Architecture rules**:
- Controller → Service ONLY (never Manager/Dao)
- Service → Manager OR Dao
- Manager → Dao ONLY (never Service/other Managers)
- `@Transactional` / `@Cacheable`: Manager layer ONLY
- `@Autowired` field injection: FORBIDDEN (use constructor injection)

→ **[Complete Architecture Rules](.agent/rules/foundation/10-architecture-rules.md)**

---

## 📝 Git Sparse Checkout (Optional)

To focus only on Java 21 + Vue 3 directories:

```bash
git sparse-checkout init --cone
git sparse-checkout set .agent .claude .github docker smart-admin-api-java21-springboot3 smart-admin-web smart-app docs 数据库SQL脚本
```

---

## 📄 License

Please refer to the project's license file for details.

---

## 🔗 Quick Links

- **[CLAUDE.md](CLAUDE.md)** - Developer quick reference
- **[.claude/README.md](.claude/README.md)** - AI agent system
- **[.agent/rules/](.agent/rules/)** - Coding standards
- **[docs/](docs/README.md)** - Infrastructure documentation
- **[Backend README](./smart-admin-api-java21-springboot3/README.md)** - Java 21 + Spring Boot 3
- **[Frontend README](./smart-admin-web/README.md)** - Vue 3 + Vite

---

**Version**: 1.0.0 | **Last Updated**: 2026-01-22
