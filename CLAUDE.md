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

### Reading Priority

When working with SmartAdmin codebase, read documentation in this order:

1. **CLAUDE.md** (this file) - Quick reference and navigation hub
2. **[.agent/rules/00-ai-decision-matrix.md](.agent/rules/00-ai-decision-matrix.md)** - Scenario-based rule selection
3. **[.agent/rules/10-architecture-rules.md](.agent/rules/10-architecture-rules.md)** - Mandatory architectural constraints
4. **[.claude/shared/knowledge/](. claude/shared/knowledge/)** - SmartAdmin implementation patterns
5. **[.claude/agents/](.claude/agents/)** - Specialized agent definitions (optional, for Claude Code)

### Interaction Language

- **Documentation**: All AI instruction documents are in English
- **User Communication**: Respond to users in **Traditional Chinese (繁體中文)**
- **Code Comments**: Follow project conventions (typically English)

### Key Constraints (Always Apply)

**CRITICAL** - These rules are enforced by ArchitectureTest and must NEVER be violated:

- ✅ Service layer MUST use `io.vavr.control.Option` (NOT `java.util.Optional`)
- ✅ Controller NEVER directly accesses Repository/Dao (must go through Service)
- ✅ `@Transactional` / `@Cacheable` annotations ONLY in Manager layer
- ✅ Constructor injection via `@RequiredArgsConstructor` + `private final` (NEVER `@Autowired` field injection)
- ✅ Boolean fields: `deleted` NOT `isDeleted`
- ✅ Use `ResponseDTO.ok(data)` for all API responses
- ✅ Transaction annotation: `@Transactional(rollbackFor = Throwable.class)`

### When in Doubt

- **Architecture questions**: Consult [.agent/rules/00-ai-decision-matrix.md](.agent/rules/00-ai-decision-matrix.md) for scenario-based guidance
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
```

**Key Rules** (enforced by ArchUnit):
- Controller → Service ONLY
- `@Transactional` / `@Cacheable`: Manager layer ONLY
- `@Autowired` field injection: FORBIDDEN

→ **[Complete Architecture Rules](.agent/rules/10-architecture-rules.md)**
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

→ **[Complete Package Naming Guide](docs/migration/foundation-package-naming-standardization.md)**
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

→ **[Complete Naming Conventions](.agent/rules/01-naming-conventions.md)**
→ **[Commit Message Guide](.agent/rules/17-commit-message-conventions.md)**

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

## Development Guidelines

**Essential Rules** (see `.agent/rules/`):
- Architecture: `10-architecture-rules.md`
- Manager Layer: `09-manager-layer.md`
- Naming: `01-naming-conventions.md`
- Exceptions: `04-exception-logging.md`

**Validation**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

→ **[All Rules Index](.agent/rules/00-ai-decision-matrix.md)**
→ **[Quality Standards](.claude/shared/knowledge/quality-standards.md)**

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

Detailed rules: See [.agent/rules/12-pmd-rules.md](.agent/rules/12-pmd-rules.md) and [.agent/rules/13-spotbugs-rules.md](.agent/rules/13-spotbugs-rules.md)

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

→ **[Complete Package Migration Guide](docs/migration/foundation-package-naming-standardization.md)**

---

## Documentation System Version

| Component | Version | Status | Metadata |
|-----------|---------|--------|----------|
| **This Document** | 3.0.0 | ✅ Universal AI Support | - |
| **AI Doc System** | 3.0.0 | ✅ Unified | [.claude/META.md](.claude/META.md) |
| **.claude/** | 2.6.0 | ✅ Agent System | [.claude/README.md](.claude/README.md) |
| **.agent/** | (unversioned) | ✅ Technical Rules | [.agent/README.md](.agent/README.md) |
| **SmartAdmin** | v4.0.0 | ✅ Production | - |

**System Metadata**: [.claude/META.md](.claude/META.md) - Unified version tracking and content ownership

**Last Updated**: 2026-01-24

**Change History**:
- 3.0.0 (2026-01-24): Content deduplication, universal AI support, unified version management
- 2.0.0 (2026-01-23): v4.0.0 breaking changes, foundation package documentation
- 1.0.0 (2026-01-22): Initial versioned release
