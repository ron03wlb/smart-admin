# AI Documentation Content Ownership Map

**Purpose**: Define Single Source of Truth for all content across CLAUDE.md, .claude/, and .agent/

**Last Updated**: 2026-01-23
**Status**: Active - Use this map to determine content ownership

---

## Content Ownership Matrix

| Content Category | Source of Truth | Quick Reference | Related Docs | Notes |
|------------------|----------------|-----------------|--------------|-------|
| **Build Commands** | `.claude/shared/knowledge/project-architecture.md` | `CLAUDE.md` (top 2 + link) | ~~.agent/docs/quick-reference.md~~ (removed) | Full list in source |
| **Architecture Rules** | `.agent/rules/10-architecture-rules.md` | `CLAUDE.md` (summary + link) | `.claude/shared/knowledge/smartadmin-patterns.md` (references source) | Enforced by ArchUnit |
| **Naming Conventions** | `.agent/rules/01-naming-conventions.md` | `CLAUDE.md` (examples + link) | `.claude/shared/knowledge/smartadmin-patterns.md` (references source) | Alibaba guidelines |
| **SmartAdmin Implementation Patterns** | `.claude/shared/knowledge/smartadmin-patterns.md` | `CLAUDE.md` (links only) | Multiple `.agent/rules/*.md` (cross-reference) | AI-optimized patterns |
| **ResponseDTO Pattern** | `.claude/shared/knowledge/smartadmin-patterns.md` | `CLAUDE.md` (example + link) | - | API response pattern |
| **Pagination Pattern** | `.claude/shared/knowledge/smartadmin-patterns.md` | `CLAUDE.md` (example + link) | - | SmartPageUtil usage |
| **Bean Conversion** | `.claude/shared/knowledge/smartadmin-patterns.md` | `CLAUDE.md` (example + link) | - | SmartBeanUtil |
| **Transaction Management** | `.agent/rules/09-manager-layer.md` | `CLAUDE.md` (rule + link) | `.claude/shared/knowledge/smartadmin-patterns.md` (references) | Manager layer only |
| **Quality Standards** | `.claude/shared/knowledge/quality-standards.md` | `CLAUDE.md` (top 3 + link) | `.agent/rules/11-16*.md` (tool-specific) | Anti-patterns, checklist |
| **Quality Tool Rules (PMD, SpotBugs, etc.)** | `.agent/rules/11-16*.md` | `.claude/shared/knowledge/quality-standards.md` (summary) | - | Tool-specific details |
| **Technology Stack** | `.claude/shared/knowledge/project-architecture.md` | `CLAUDE.md` (table + link) | ~~.agent/docs/tech-stack.md~~ (consolidated) | Versions, compatibility |
| **Foundation Package Naming** | `CLAUDE.md` (unique content) | `CLAUDE.md` | `docs/migration/foundation-package-naming-standardization.md` | v4.0.0 breaking changes |
| **AI Decision Matrix** | `.agent/rules/00-ai-decision-matrix.md` | - | - | Scenario → rules mapping |
| **PostgreSQL Rules** | `.agent/rules/05-postgresql-*.md` | - | `.claude/shared/knowledge/smartadmin-patterns.md` (references) | Database best practices |
| **Vavr Functional Programming** | `.agent/rules/08-vavr-*.md` | - | `.claude/shared/knowledge/smartadmin-patterns.md` (references) | Option, Try, Either |
| **MyBatis Plus Rules** | `.agent/rules/09-mybatis-plus-*.md` | - | `.claude/shared/knowledge/smartadmin-patterns.md` (references) | LambdaQueryWrapper |
| **OWASP Security** | `.agent/rules/07-owasp-top10-*.md` | - | - | Security guidelines |
| **Commit Conventions** | `.agent/rules/17-commit-message-conventions.md` | `CLAUDE.md` (format + link) | - | Conventional Commits |
| **Agent Definitions** | `.claude/agents/*.md` | - | - | Specialized AI agents |
| **Orchestration Patterns** | `.claude/shared/orchestration/*.md` | - | - | Multi-agent workflows |
| **Development Workflows** | `.agent/workflows/*.md` | - | - | TDD, CI/CD, init |

---

## Duplication Rules

### ✅ Allowed Patterns

1. **Quick Reference in CLAUDE.md**:
   - Show 1-3 most common examples
   - MUST include link to source of truth
   - Keep examples minimal (1-2 lines)

2. **Cross-References**:
   - Files can reference each other
   - Use relative links
   - Always link to source, never duplicate full content

3. **Aggregators**:
   - `.claude/shared/knowledge/quality-standards.md` aggregates from `.agent/rules/11-16*.md`
   - Must use "See [file] for details" pattern
   - Can provide brief summary (2-3 sentences)

### ❌ Prohibited Patterns

1. **Full Content Duplication**:
   - Never copy full sections across files
   - Never duplicate code blocks >5 lines
   - Never duplicate entire tables

2. **Partial Duplication Without Links**:
   - If showing example, MUST link to source
   - If summarizing, MUST link to full content

3. **Conflicting Sources**:
   - Only ONE file can be source of truth per content
   - If conflict found, use hierarchy: `.agent/rules/` > `.claude/shared/knowledge/` > `CLAUDE.md`

---

## Update Protocols

### Scenario 1: Architecture Rule Changes

**Trigger**: `.agent/rules/10-architecture-rules.md` updated

**Impact Chain**:
1. Update `.agent/rules/10-architecture-rules.md` (source of truth)
2. Verify `.claude/shared/knowledge/smartadmin-patterns.md` reference still accurate
3. Verify `CLAUDE.md` quick reference still accurate (usually no change needed)
4. Update `.agent/configs/ArchitectureTest.java` if enforcement rules changed

**Validation**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
./.claude/scripts/validate-cross-references.sh
```

### Scenario 2: New SmartAdmin Pattern Added

**Trigger**: `.claude/shared/knowledge/smartadmin-patterns.md` updated

**Impact Chain**:
1. Add pattern to `.claude/shared/knowledge/smartadmin-patterns.md`
2. If pattern is top 10 most common, add to `CLAUDE.md` Quick Reference Table (1 line + link)
3. Add cross-reference in related `.agent/rules/*.md` if applicable

**Validation**:
```bash
./.claude/scripts/validate-cross-references.sh
```

### Scenario 3: Quality Tool Rule Update

**Trigger**: `.agent/rules/12-pmd-rules.md` updated (example)

**Impact Chain**:
1. Update `.agent/rules/12-pmd-rules.md` (source of truth)
2. Update `.claude/shared/knowledge/quality-standards.md` if summary affected
3. `CLAUDE.md` usually unaffected (shows top 3 anti-patterns only)

### Scenario 4: CLAUDE.md Quick Reference Update

**Trigger**: `CLAUDE.md` quick reference needs update

**Impact Chain**:
1. Verify change is appropriate (should link, not duplicate)
2. Update `CLAUDE.md`
3. Verify links still point to correct sources
4. NO changes to source docs (CLAUDE.md never the source of truth except Foundation Package Naming)

**Validation**:
```bash
./.claude/scripts/validate-cross-references.sh
```

---

## File Status After Phase 4

### Modified Files

| File | Status | Changes |
|------|--------|---------|
| `CLAUDE.md` | ✅ Optimized | Removed duplication, added links |
| `.claude/shared/knowledge/smartadmin-patterns.md` | ✅ Updated | Now aggregator, references `.agent/rules/` |
| `.claude/shared/knowledge/quality-standards.md` | ✅ Updated | References `.agent/rules/11-16*.md` |
| `.claude/shared/knowledge/project-architecture.md` | ✅ Expanded | Merged tech stack from `.agent/docs/` |

### Removed Files

| File | Status | Reason |
|------|--------|--------|
| `.agent/docs/quick-reference.md` | 🗑️ Deleted | Merged into `CLAUDE.md` |
| `.agent/docs/tech-stack.md` | 🗑️ Deleted | Merged into `.claude/shared/knowledge/project-architecture.md` |

### Unchanged Files (Source of Truth)

| File | Status | Notes |
|------|--------|-------|
| `.agent/rules/*.md` (25 files) | ✅ Unchanged | Source of truth for technical rules |
| `.agent/workflows/*.md` (6 files) | ✅ Unchanged | Development workflows |
| `.claude/agents/*.md` (9 files) | ✅ Unchanged | Agent definitions |
| `.claude/shared/orchestration/*.md` (3 files) | ✅ Unchanged | Orchestration patterns |

---

## Maintenance Schedule

- **Weekly**: Run `.claude/scripts/validate-cross-references.sh`
- **Monthly**: Review this CONTENT_MAP.md for new duplication
- **Quarterly**: Full audit of all cross-references
- **On major change**: Update this map immediately

---

**Version**: 1.0.0
**Created**: 2026-01-23
**Next Review**: 2026-02-23
