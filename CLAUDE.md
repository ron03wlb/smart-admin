# CLAUDE.md

**Navigation**: See [README.md](README.md) for complete documentation index and "I want to..." guide.

**Quick Reference Card** for SmartAdmin development. This document provides essential patterns, commands, and conventions for rapid development.

**For detailed guidance**, see:
- [.claude/ Directory](.claude/README.md) - AI agent system, multi-agent workflows, and orchestration
- [.agent/rules/](.agent/rules/) - Comprehensive coding standards and technical rules
- [Architecture Documentation](docs/) - High-level architecture and design decisions

---

## Quick Reference Card

| Task | Pattern | Details |
|------|---------|---------|
| Return success | `ResponseDTO.ok(data)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#responsedto-pattern) |
| Paginated query | `SmartPageUtil.convert2PageQuery(form)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#pagination-pattern) |
| Bean copy | `SmartBeanUtil.copy(source, Target.class)` | [→](.claude/shared/knowledge/smartadmin-patterns.md#bean-conversion) |
| Transaction | `@Transactional` in Manager only | [→](.claude/shared/knowledge/smartadmin-patterns.md#transaction-management) |

**Complete Patterns**: [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md)

## Build Commands

**Location**: `smart-admin-api-java21-springboot3/`

```bash
./gradlew clean build          # Build
./gradlew :sa-admin:bootRun    # Run (http://localhost:1024)
./gradlew :sa-admin:test       # Test
./gradlew :sa-admin:test --tests ArchitectureTest  # Arch validation
```

→ **[All Build Commands](.claude/shared/knowledge/project-architecture.md#build-commands)**

## Architecture

Modular monolith with strict layered architecture enforced by ArchUnit:

```
Controller → Service → Manager → Dao → Entity
     ↓          ↓          ↓        ↓
@RestController  Business   Cache   BaseMapper
 @Valid         Logic      @Transactional
```

**Module structure:**
- `sa-base/` - Infrastructure, utilities, support modules
- `sa-admin/` - Business logic, system modules
- `sa-common/` - Shared services: api-encrypt, cache, mq, redis-lock

**Layer rules (enforced via ArchitectureTest.java):**
- Controller → Service ONLY (never directly to Manager/Dao)
- Service → Manager OR Dao
- Manager → Dao ONLY (never to Service or other Managers)
- `@Transactional` / `@Cacheable`: ONLY in Manager layer
- `@Autowired` field injection: FORBIDDEN

**See also**:
- [SmartAdmin Patterns](.claude/shared/knowledge/smartadmin-patterns.md) - Detailed implementation patterns
- [Project Architecture](.claude/shared/knowledge/project-architecture.md) - Module structure and build configuration
- [Quality Standards](.claude/shared/knowledge/quality-standards.md) - Code quality checklist

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
- Use `@RequiredArgsConstructor` + `private final` fields
- NEVER use `@Autowired` field injection

**Naming:**
- Classes: `UserController`, `UserService`, `UserManager`, `UserDao`, `UserEntity`
- Forms: `UserAddForm`, `UserUpdateForm`, `UserQueryForm`
- Boolean fields: `deleted` NOT `isDeleted`

**Commit messages:** Conventional Commits format
```
<type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
Scopes: sa-admin, sa-base, sa-common, smart-admin-web, smart-app, docker, docs
```

→ **[Complete Conventions](.agent/rules/01-naming-conventions.md)**

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

Detailed coding standards in `.agent/rules/`:
- `10-architecture-rules.md` - Layered architecture
- `09-manager-layer.md` - Manager layer constraints
- `01-naming-conventions.md` - Naming standards (Alibaba guidelines)
- `17-commit-message-conventions.md` - Git conventions
- `04-exception-logging.md` - Exception & logging standards

Run architecture validation before commits:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**See also**:
- [Quality Standards](.claude/shared/knowledge/quality-standards.md) - Code quality checklist
- [.claude/ Directory](.claude/README.md) - Agent system and orchestration

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

## Version

**Document Version**: 1.0.0
**Last Updated**: 2026-01-22
**Aligned with**: .claude/ v2.5.0, .agent/rules/ (latest)

**Change History**:
- 1.0.0 (2026-01-22): Initial versioned release with cross-references to .claude/ and improved navigation
