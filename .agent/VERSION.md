# .agent/ System Version Tracking

**Current Version**: 1.0.0 (Target)
**Status**: 🚧 English Translation In Progress
**Last Updated**: 2026-01-24

---

## Version Information

### Current State

| Component | Version | Status | Language |
|-----------|---------|--------|----------|
| **Rules System** | 1.0.0-SNAPSHOT | Translation in progress | Mixed (Chinese → English) |
| **Workflows** | 1.0.0-SNAPSHOT | Translation in progress | Mixed (Chinese → English) |
| **Documentation** | 1.0.0-SNAPSHOT | Active | Chinese (Traditional) |
| **Configs** | 1.0.0 | Stable | N/A (YAML/Java) |

### Translation Progress

**Target**: v1.0.0 release after complete English translation

**Phase Status**:
- Wave 1: ✅ Content Deduplication Complete
- Wave 2: 🚧 In Progress (Translation Preparation)
- Wave 3: ⏳ Pending (Bulk Translation)

**Files to Translate**:
- `rules/*.md`: 25 files (including 00-ai-decision-matrix.md)
- `workflows/*.md`: 6 files
- `docs/*.md`: 2 files (coding-standards-summary.md, faq-troubleshooting.md remain Chinese for user reference)

---

## Component Details

### rules/ Directory

**Purpose**: Source of truth for all technical rules and coding standards

**Key Files**:
- `00-ai-decision-matrix.md` - AI decision tree and rule index (191 lines)
- `10-architecture-rules.md` - Layered architecture enforcement
- `01-naming-conventions.md` - Naming standards (Alibaba guidelines)
- `08-vavr-fundamentals.md` - Functional programming with Vavr
- `05-postgresql-*.md` - PostgreSQL best practices (3 files)

**Version History**:
- v1.0.0-SNAPSHOT (2026-01-24): Translation preparation, content deduplication
- Pre-1.0 (2025-12-15 to 2026-01-21): Initial creation, Chinese documentation

### workflows/ Directory

**Purpose**: Development workflows and processes

**Key Files**:
- `00-workflow-index.md` - Workflow navigation hub
- `init.md` - Environment setup (Java 21, PostgreSQL, Redis)
- `tdd-workflow.md` - Test-driven development process
- `quality-gates-local-ci.md` - Local Quality Gate, GitLab CI
- `github-actions-pipeline.md` - GitHub Actions CI/CD
- `java-failure-recovery.md` - Error recovery procedures

**Version History**:
- v1.0.0-SNAPSHOT (2026-01-24): Active development

### docs/ Directory

**Purpose**: High-level summaries and user-facing documentation

**Key Files**:
- `coding-standards-summary.md` - Development standards overview (Traditional Chinese, for user reference)
- `faq-troubleshooting.md` - Common issues and solutions (Traditional Chinese, for user reference)

**Language Strategy**: These docs remain in Traditional Chinese as they are primarily for human developers' quick reference, not AI consumption.

### configs/ Directory

**Purpose**: Configuration templates and examples

**Key Files**:
- `docker-compose.yml` - PostgreSQL + Redis development environment
- `ArchitectureTest.java` - ArchUnit test template

**Version**: v1.0.0 (Stable)

---

## Relationship with Other Systems

### Dependencies FROM .agent/

**Who reads .agent/**:
- `CLAUDE.md` - References .agent/rules/ for detailed technical rules
- `.claude/shared/knowledge/` - Aggregates and links to .agent/rules/ (source of truth)
- AI coding assistants (Claude, Antigravity, Gemini) - Read via CLAUDE.md entry point

### Dependencies TO .agent/

**What .agent/ depends on**:
- None - .agent/rules/ is the **source of truth** for technical rules
- All external documents reference .agent/ as authoritative source

---

## Update Protocol

### When to Bump Version

**Patch (1.0.x)**:
- Fix typos or minor clarifications in rules
- Add examples to existing rules
- Update code snippets for accuracy

**Minor (1.x.0)**:
- Add new rules files (e.g., new security guideline)
- Add new workflows
- Translate existing content to English
- Significant content additions to existing rules

**Major (x.0.0)**:
- Breaking changes to rule structure
- Removal of deprecated rules
- Major architectural rule changes that require code refactoring

### Versioning After Translation

**Target**: v1.0.0 upon completion of Wave 3 (Bulk Translation)

**Release Criteria**:
- ✅ All rules/*.md translated to English
- ✅ All workflows/*.md translated to English
- ✅ Cross-references validated
- ✅ AI decision matrix (00-ai-decision-matrix.md) fully functional in English
- ✅ Glossary complete (.agent/docs/translation-glossary.md)

---

## Version History

### 1.0.0-SNAPSHOT (Current)

**Date**: 2026-01-24
**Status**: Translation in progress

**Changes**:
- Wave 1 complete: Content deduplication across CLAUDE.md, .claude/, .agent/
- Deleted redundant files: docs/quick-reference.md, docs/tech-stack.md
- Fixed cross-references to deleted files
- Created CONTENT_MAP.md (Single Source of Truth hierarchy)
- Simplified CLAUDE.md from 241 → 234 lines
- Converted .claude/shared/knowledge/ to aggregator pattern

**Translation Progress**:
- 🚧 Wave 2 in progress: Translation glossary, README.md, 00-ai-decision-matrix.md
- ⏳ Wave 3 pending: Bulk translation of 23 rules + 6 workflows

### Pre-1.0 (2025-12-15 to 2026-01-21)

**Status**: Initial creation and evolution

**Major Milestones**:
- Created 25 rules covering architecture, naming, OOP, concurrency, PostgreSQL, security, Vavr, MyBatis Plus
- Created 6 workflows for init, TDD, quality gates, CI/CD, failure recovery
- Established AI decision matrix (00-ai-decision-matrix.md)
- Documented coding standards summary and FAQ
- All content in Traditional Chinese

---

## Roadmap

### Q1 2026

- ✅ Wave 1: Content deduplication (Complete)
- 🚧 Wave 2: Translation preparation (In Progress)
  - Translation glossary
  - Key files: README.md, 00-ai-decision-matrix.md
- ⏳ Wave 3: Bulk translation (Planned)
  - 23 rules files
  - 6 workflow files
- 🎯 Release v1.0.0 (Target: End of Q1)

### Q2 2026

- Continuous improvement based on usage feedback
- Add rule enforcement examples (ArchUnit tests)
- Expand PostgreSQL and Vavr guidelines based on project evolution

---

## Contact & Maintenance

**Maintained by**: SmartAdmin Development Team
**Coordination**: See [../.claude/META.md](../.claude/META.md) for cross-system version alignment
**Issues**: Report documentation issues via project issue tracker

---

**Document Version**: 1.0.0
**Last Updated**: 2026-01-24
**Next Review**: After Wave 3 completion (English translation)
