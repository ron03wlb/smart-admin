# Batch Plan Executor

**Priority**: P1 (Extended - Orchestration)
**Version**: 1.0.0
**Status**: Stable

---

## Overview

Intelligent batch plan executor for detecting conflicts and executing multiple implementation plans. Core orchestrator of the SmartAdmin Skills System.

---

## When to Use

- Executing multiple implementation plans in sequence or parallel
- Large migration projects (e.g., LiteFlow migration with multiple phases)
- Batch CRUD module generation (multiple business modules at once)
- Pre-execution risk assessment and conflict detection
- Multi-team collaboration scenarios (avoiding file conflicts)

---

## Core Capabilities

| Capability | Description |
|-----------|-------------|
| **Multi-Source Discovery** | Claude Code Plans, Skills Docs, Project Plans |
| **Conflict Detection** | File-level, Module-level, Dependency relations |
| **Skill Mapping** | Auto-map plans to corresponding skills |
| **Parallel Execution** | Up to 5 plans in parallel (60-80% efficiency gain) |
| **Comprehensive Reporting** | Pre-run risk + Progress tracking + Post-run summary |
| **Dry-run Mode** | Simulate execution with zero risk |

---

## Skill Dependencies

This orchestrator coordinates multiple skills:

| Skill | Purpose |
|-------|---------|
| `smartadmin-crud-generator` | CRUD generation tasks |
| `liteflow-rule-builder` | Rule DSL generation |
| `smartadmin-testing-suite` | Testing execution |
| `smartadmin-performance-suite` | Performance optimization |

---

## Execution Modes

### Sequential Mode (Default)
Plans execute one after another. Safer for dependent plans.

### Parallel Mode
Independent plans execute simultaneously. Maximum 5 concurrent.

### Dry-run Mode
Simulates execution without making changes. Use for risk assessment.

---

## Usage Examples

```
# Execute all plans in directory
"Execute all plans in docs/plans/liteflow/"

# Dry-run assessment
"Dry-run batch execution for CRUD modules"

# Parallel execution
"Execute CRUD plans in parallel"
```

---

## Conflict Types Detected

| Type | Detection | Resolution |
|------|-----------|------------|
| **File-level** | Same file modified by multiple plans | Sequential execution |
| **Module-level** | Same module touched | Merge or manual review |
| **Dependency** | Plan A depends on Plan B | Topological sort |

---

## Related Rules

- [F04-architecture-rules.md](../../../../rules/foundation/F04-architecture-rules.md) - SmartAdmin layered architecture

---

**Maintainer**: SmartAdmin Skills Team
**Last Updated**: 2026-02-07
