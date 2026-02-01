# AI Documentation System Metadata

**Purpose**: Unified version tracking and metadata for entire SmartAdmin AI documentation system (CLAUDE.md, .claude/, .agent/)

**Last Updated**: 2026-01-31
**System Version**: 3.0.2
**Current .claude/ Version**: 3.0.2

---

## Version Information

### Component Versions

| Component | Version | Last Updated | Status | Owner |
|-----------|---------|-------------|--------|-------|
| **CLAUDE.md** | 3.4.0 | 2026-01-31 | ✅ Universal AI Support + Java 21 | Root |
| **.claude/ System** | 3.0.2 | 2026-01-30 | ✅ Optimized | .claude/VERSION.md |
| **.agent/ Rules** | 1.0.0 | 2026-01-27 | ✅ Production Ready | .agent/VERSION.md |
| **.agents/skills/** | (external) | N/A | ✅ Active | Claude Code |

**Version Notes**:
- CLAUDE.md v3.3.0: Skills catalog update (16 → 29 skills), hierarchical structure documentation
- .claude/ v3.0.2: Directory optimization - 3.2MB archived, 35 test files archived, Git status cleaned, comprehensive archive structure
- .claude/ v3.0.0: Skills architecture v3.0.0 - Hybrid layered structure (foundation/extended/productivity/lifecycle), 100% config.yml coverage (1/29 → 29/29), skill-registry.yml SSOT
- .agent/ v1.0.0: Production release - Rules classification, PostgreSQL consolidation, unified decision center
- .agents/skills/: External Claude Code skills, managed independently

**Detailed Version History**: See [.claude/VERSION.md](VERSION.md) for complete version history and release notes.

---

## Cross-System Dependencies

**CLAUDE.md → Dependencies**:
- `.claude/shared/knowledge/smartadmin-patterns.md` (implementation patterns)
- `.claude/shared/knowledge/project-architecture.md` (build commands, tech stack)
- `.claude/shared/knowledge/quality-standards.md` (quality checklist)
- `.agent/rules/foundation/01-naming-conventions.md` (naming conventions)
- `.agent/rules/foundation/10-architecture-rules.md` (architecture enforcement)
- `.agent/rules/00-INDEX.md` (AI decision tree)
- `.agent/rules/workflows/17-commit-message-conventions.md` (commit conventions)

**.claude/shared/knowledge/ → Dependencies**:
- `.agent/rules/*.md` (source of truth for technical rules)
- References .agent/rules/ for detailed rule enforcement
- Aggregates and links, does not duplicate content

**.agent/rules/ → Dependencies**:
- None (source of truth, no external dependencies)
- All technical rules originate here
- Other documents reference these as authoritative source

---

## Content Ownership Map

See [../../CONTENT_MAP.md](../../CONTENT_MAP.md) for complete content ownership tracking.

**Quick Reference**:
- **Architecture Rules**: `.agent/rules/foundation/10-architecture-rules.md` (source of truth)
- **Naming Conventions**: `.agent/rules/foundation/01-naming-conventions.md` (source of truth)
- **SmartAdmin Patterns**: `.claude/shared/knowledge/smartadmin-patterns.md` (source of truth for implementation)
- **Build Commands**: `.claude/shared/knowledge/project-architecture.md` (source of truth)
- **Quick Reference**: `CLAUDE.md` (never duplicates, only links + 1-3 examples)

**Key Principle**: Each piece of content has exactly ONE source of truth. All other references link to that source.

---

## Update Protocols (Simplified)

**Full protocols**: See [.claude/docs/maintenance-guide.md](docs/maintenance-guide.md) for detailed maintenance procedures.

### Quick Update Guide

| Change Type | Primary File | Time | Cross-Ref Updates |
|------------|--------------|------|-------------------|
| **SmartAdmin Pattern** | `.claude/shared/knowledge/smartadmin-patterns.md` | ~5 min | Verify CLAUDE.md links, update version |
| **Architecture Rule** | `.agent/rules/foundation/10-architecture-rules.md` | ~15 min | Update ArchitectureTest.java, verify shared knowledge |
| **Build Command** | `.claude/shared/knowledge/project-architecture.md` | ~10 min | Update CLAUDE.md top 4 commands |
| **New Agent** | `.claude/agents/[name].md` | 30-45 min | Update 00-INDEX.md, agent-capability-matrix.md |
| **Permission** | `.claude/settings.local.json` | ~5 min | Update version if significant |

### Update Principle

**Always update the source of truth first, then propagate changes to references.**

**Impact Chain Example** (Architecture Rule Change):
1. Update `.agent/rules/foundation/10-architecture-rules.md` (source)
2. Verify `.claude/shared/knowledge/smartadmin-patterns.md` references
3. Update `.agent/configs/ArchitectureTest.java` enforcement
4. Bump `.agent/` version in this META.md

---

## File Structure (Essential)

```
.claude/
├── META.md                              # ← This file (system metadata)
├── VERSION.md                           # Version history
├── settings.local.json                  # Permissions (12 core patterns)
├── agents/                              # 9 specialized agents
├── skills/                              # 29 specialized skills (v3.0.0)
│   ├── README.md                        # Skills catalog (auto-generated)
│   ├── skill-registry.yml               # SSOT for skill metadata
│   ├── skill-aliases.json               # Backward compatibility
│   ├── foundation/                      # P0 - 6 skills (Backend, Full-stack, Testing)
│   ├── extended/                        # P1 - 7 skills (Business Logic, Domain, Orchestration, Quality)
│   ├── productivity/                    # P2 - 15 skills (Infrastructure, Composite, Analysis)
│   ├── lifecycle/                       # Deprecated & Experimental skills
│   └── _shared/                         # Templates, scripts, references
├── shared/
│   ├── knowledge/                       # Source of truth (4 files)
│   │   ├── smartadmin-patterns.md
│   │   ├── smartadmin-frontend-patterns.md
│   │   ├── project-architecture.md
│   │   └── quality-standards.md
│   ├── templates/                       # Agent base templates
│   └── orchestration/
│       └── orchestration-playbook.md    # Unified: workflows + dependencies + handoffs (2,296 lines)
├── docs/                                # Documentation
│   ├── maintenance-guide.md             # Maintenance procedures
│   ├── changelog.md                     # Detailed change history
│   ├── quick-start-guide.md
│   └── agent-capability-matrix.md
└── scripts/
    └── validate-links.sh

.agent/rules/
├── 00-INDEX.md                          # Unified decision center (630 lines)
├── foundation/                          # Core architecture rules (4 files)
├── technology/                          # Tech-specific rules (10 files)
├── security/                            # Security rules (2 files)
├── quality-tools/                       # Tool enforcement (6 files)
└── workflows/                           # Process rules (2 files)
```

---

## Success Metrics

| Category | Metric | Target | Current | Status |
|----------|--------|--------|---------|--------|
| **Documentation** | Entry points | 1 | 1 (README.md) | ✅ |
| | Broken links | 0 | 0 | ✅ |
| | Duplicated examples | 0 | 0 | ✅ |
| **Maintenance** | Pattern update time | <10 min | 5 min | ✅ |
| | Add new agent | <60 min | 30-45 min | ✅ |
| **Quality** | Agent duplication | <10% | <10% | ✅ |
| | Permission patterns | ~12 | 12 + loops | ✅ |
| | Orchestration files | 1 | 1 | ✅ |

---

## Maintenance Schedule

- **Weekly**: Run link validation script (`.claude/scripts/validate-links.sh`)
- **Monthly**: Review metrics, update if needed
- **Quarterly**: Full cross-reference review (see [maintenance-guide.md](docs/maintenance-guide.md#protocol-5-quarterly-review))
- **On major change**: Update all dependent docs synchronously
- **On version bump**: Update this META.md Component Versions table

**Next Quarterly Review**: 2026-04-27 (Q2)

---

## Related Documentation

### Core Documentation
- **[CLAUDE.md](../../CLAUDE.md)** - AI assistant quick reference card
- **[README.md](../../README.md)** - Project overview and "I want to..." guide
- **[.claude/README.md](README.md)** - AI agent system overview

### Version & History
- **[.claude/VERSION.md](VERSION.md)** - Complete .claude/ version history
- **[.agent/VERSION.md](../.agent/VERSION.md)** - .agent/ rules version history
- **[.claude/docs/changelog.md](docs/changelog.md)** - Detailed change log with optimization history

### Maintenance & Support
- **[.claude/docs/maintenance-guide.md](docs/maintenance-guide.md)** - Complete maintenance procedures, protocols, rollback information
- **[.claude/docs/troubleshooting-guide.md](docs/troubleshooting-guide.md)** - Problem resolution
- **[.claude/docs/quick-start-guide.md](docs/quick-start-guide.md)** - Getting started

### Agent System
- **[.agent/rules/00-INDEX.md](../.agent/rules/00-INDEX.md)** - Unified decision center (rules routing, skill selection, agent orchestration)
- **[.claude/shared/orchestration/orchestration-playbook.md](shared/orchestration/orchestration-playbook.md)** - Complete workflows, dependencies, collaboration protocols
- **[.claude/docs/agent-capability-matrix.md](docs/agent-capability-matrix.md)** - Agent comparison

---

## Contact & Support

For questions or issues with the .claude/ configuration:

1. **Maintenance**: [docs/maintenance-guide.md](docs/maintenance-guide.md) - Procedures and protocols
2. **Troubleshooting**: [docs/troubleshooting-guide.md](docs/troubleshooting-guide.md) - Common issues
3. **Version History**: This META.md + [VERSION.md](VERSION.md)
4. **Git History**: `git log .claude/` - Complete change history

---

## Skill Version Management

**Central Version Registry**: `.claude/skills/VERSIONS.yml`

**Purpose**: Single source of truth for all 33 skill versions across foundation/extended/productivity/lifecycle categories.

**Usage**:
```bash
# Check version synchronization (dry-run)
.claude/scripts/sync-skill-versions.sh --dry-run

# Sync all config.yml versions with VERSIONS.yml
.claude/scripts/sync-skill-versions.sh --update
```

**Version Sync Tool**: `.claude/scripts/sync-skill-versions.sh`
- Automatically syncs versions between VERSIONS.yml and individual config.yml files
- Supports dry-run mode for safe checking
- Color-coded output for easy identification of sync status

**Maintenance**:
- Update VERSIONS.yml when releasing new skill versions
- Run sync script after bulk version updates
- VERSIONS.yml is the canonical source for all version numbers

---

## Maintenance Protocol

### 何時更新 META.md

**觸發事件**:
- 新增/移除 skill → 更新 "Content Inventory" 計數
- CLAUDE.md 版本升級 → 更新版本表 + last updated 日期
- `.claude/` 結構變更 → 更新 "Content Inventory" 路徑
- 重大 skill 重構 → 更新變更歷史

### 更新檢查清單

1. **版本表** (Lines 13-16):
   - 如有破壞性變更，升級版本
   - 更新 "Last Updated" 日期以匹配最新變更
   - 如有交叉引用，與 CLAUDE.md 同步日期

2. **內容清單** (Lines 24-49):
   - 驗證 skill 計數符合 `find .claude/skills -name SKILL.md | wc -l`
   - 如結構變更，更新文件計數

3. **變更歷史** (Lines 51-76):
   - 添加新條目（日期、版本、摘要）
   - 保持時間順序（最新在前）

### 驗證命令

```bash
# 驗證 skill 計數
find .claude/skills/foundation -name SKILL.md | wc -l  # 應符合 P0 計數
find .claude/skills/extended -name SKILL.md | wc -l    # 應符合 P1 計數
find .claude/skills/productivity -name SKILL.md | wc -l # 應符合 P2 計數

# 檢查孤立的 backup
find .claude -name "*.backup" -o -name "*~" -o -name "*.swp"

# 驗證時間戳一致性
grep -h "Last Updated\|2026-" CLAUDE.md .claude/META.md | sort -u
```

**時間戳**: 2026-01-31
**驗證者**: Architecture review process
**下次審查**: 2026-02-28（或當 CLAUDE.md 達到 v3.5.0）

---

**Document Version**: 2.2.0
**Last Updated**: 2026-01-31
**Next Review**: 2026-04-27 (Quarterly)
