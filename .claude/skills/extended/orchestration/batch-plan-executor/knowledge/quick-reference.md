# Batch Plan Executor - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: batch-plan-executor (P1 - Extended/Orchestration)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Example |
|---------|---------|---------|
| Auto Scan & Execute | Auto-detect and execute all plans | /batch-execute --auto |
| Execute Specific Plans | Execute selected plans | /batch-execute plan1.md plan2.md plan3.md |
| Scan Directory | Scan specific directory | /batch-execute --scan-dir=docs/plans/liteflow/ |
| Dry-run Simulation | Risk assessment without execution | /batch-execute --dry-run |
| Interactive Mode | Manual confirmation for each plan | /batch-execute --mode=interactive |
| Retry Failed Plans | Retry failed plans from previous batch | /batch-execute --retry-failed=batch-exec-20260129 |

### Rapid Development Workflow

Time estimate: 10-30 minutes per batch (depending on plan count)

| Step | Action | Time |
|------|--------|------|
| 1. Dry-run Analysis | Run dry-run to assess conflicts | ~2 min |
| 2. Review Report | Check conflict analysis and risk assessment | ~3 min |
| 3. Decide Mode | Choose auto/interactive based on risk | ~1 min |
| 4. Execute Batch | Execute plans in chosen mode | 5-20 min |
| 5. Verify Results | Check execution report and logs | ~2 min |

---

## Execution Mode Selection (Decision Matrix)

### Mode 1: Auto Mode (Fully Automatic)

**Trigger Keywords**: "low risk", "trusted plans", "routine execution", "batch generate CRUD"

**Use When**:
- Low-risk plans (no file conflicts)
- Trusted plan sources
- Routine batch operations (CRUD generation)

**Command**:
```bash
/batch-execute --auto
/batch-execute --mode=auto --scan-dir=docs/plans/
```

**Behavior**:
- Zero manual intervention
- Auto-resolves conflicts
- Stops on high-risk conflicts
- Sequential execution (v1.0.0)

**Time Estimate**: 5-15 min for 5-10 plans

**Risk Level**: ⚠️ MEDIUM (use dry-run first for unknown plans)

---

### Mode 2: Interactive Mode

**Trigger Keywords**: "high risk", "first time", "review before execute", "manual confirmation"

**Use When**:
- High-risk plans (file conflicts detected)
- First-time execution
- Unknown plan sources
- Multi-team collaboration

**Command**:
```bash
/batch-execute --mode=interactive
/batch-execute --mode=interactive --allow-reorder
```

**Behavior**:
- Manual confirmation for each plan
- Display conflict details
- Allow skip/reorder plans
- Safe for production changes

**Time Estimate**: 15-30 min for 5-10 plans (includes review time)

**Risk Level**: ✅ LOW (manual control at each step)

---

### Mode 3: Dry-run Mode (Simulation)

**Trigger Keywords**: "assess risk", "check conflicts", "validate plans", "no execution"

**Use When**:
- Pre-execution risk assessment
- Validating plan mappings
- Analyzing conflicts before real execution
- Estimating execution time

**Command**:
```bash
/batch-execute --dry-run
/batch-execute --dry-run --show-conflicts --auto
```

**Behavior**:
- No actual execution
- Generate full analysis report
- Validate skill mappings
- Detect conflicts
- Estimate execution time

**Report Includes**:
- Plan inventory
- Skill mapping validation
- Conflict analysis
- Execution plan preview
- Time estimation
- Risk assessment

**Time Estimate**: 2-5 min

**Risk Level**: ✅ ZERO RISK (simulation only)

---

## Conflict Detection Quick Guide

### Layer 1: File-Level Conflicts (v1.0.0 ✅)

**Definition**: Two or more plans modify the same file

**Severity**: 🔴 HIGH - May cause merge conflicts or data loss

**Detection Strategy**:
1. Code blocks with file path comments
2. Explicit file references (backticks)
3. Markdown links
4. Class name inference (Entity, Controller, etc.)

**Resolution**:
- Serialize execution (Plan A → Plan B)
- User confirmation (interactive mode)

**Example Report**:
```
╔══════════════════════════════════════════════════════════════════
║ Conflict #1: ProductController.java
║   Conflicting Plans:
║     - [crud] product-crud
║     - [refactoring] controller-vavr-migration
║   Resolution: Serialize execution (product-crud → vavr-migration)
╚══════════════════════════════════════════════════════════════════
```

---

### Layer 2: Module-Level Conflicts (v1.1.0 ⏳)

**Definition**: Two or more plans operate on the same business module

**Severity**: ⚠️ MEDIUM - May cause integration issues

**Status**: Planned for v1.1.0

---

### Layer 3: Dependency Conflicts (v1.2.0 ⏳)

**Definition**: Plan execution order violates dependencies

**Severity**: 🔴 CRITICAL - May cause execution failure

**Status**: Planned for v1.2.0

---

## Skill Mapping Quick Reference

### Automatic Mapping Rules

| Plan Type | Keyword Patterns | Mapped Skill | Confidence |
|-----------|------------------|--------------|------------|
| CRUD | "CRUD", "generate entity", "add module" | smartadmin-crud-generator | 100% |
| Testing | "test", "integration test", "unit test" | smartadmin-integration-test | 95% |
| Refactoring | "refactor", "Vavr", "extract Manager" | vavr-refactoring-assistant, smartadmin-manager-extractor | 90% |
| Migration | "migrate", "LiteFlow", "database schema" | liteflow-rule-builder, db-migration-manager | 85% |
| Security | "security", "encryption", "SM2/SM3" | security-hardening-pro | 95% |
| ArchUnit | "ArchUnit", "architecture test" | archunit-test-generator | 100% |

### Manual Mapping Override

If automatic mapping fails or confidence < 80%:

```markdown
<!-- skill: smartadmin-crud-generator -->
# Product CRUD Implementation Plan
```

Add YAML frontmatter:
```yaml
---
skill: smartadmin-crud-generator
priority: high
---
```

---

## Command Options Reference

### Execution Control

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--mode=<mode>` | Execution mode (auto/interactive/dry-run) | auto | --mode=interactive |
| `--max-concurrent=<n>` | Max parallel executions | 5 | --max-concurrent=3 |
| `--on-failure=<strategy>` | Failure strategy (CONTINUE/ABORT_ALL/PAUSE) | CONTINUE | --on-failure=ABORT_ALL |

### Scanning & Discovery

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--auto` | Auto-scan all configured directories | false | --auto |
| `--scan-dir=<path>` | Scan specific directory | All default dirs | --scan-dir=docs/plans/ |

### Analysis & Validation

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--dry-run` | Simulation mode (no execution) | false | --dry-run |
| `--validate-mappings` | Validate skill mappings | true (dry-run) | --validate-mappings |
| `--show-conflicts` | Show detailed conflict info | true (dry-run) | --show-conflicts |
| `--allow-reorder` | Allow plan reordering | false | --allow-reorder |

### Retry & Recovery

| Option | Description | Default | Example |
|--------|-------------|---------|---------|
| `--retry-failed=<batch-id>` | Retry failed plans from previous batch | - | --retry-failed=batch-exec-20260129 |

---

## Common Errors and Quick Fixes

### Error 1: Skill Mapping Failed

**Error Message**: "Cannot map plan [product-crud.md] to skill: No matching skill found"

**Cause**: Plan content doesn't match any automatic mapping rules

**Fix**:
Add explicit skill mapping in plan:
```markdown
<!-- skill: smartadmin-crud-generator -->
# Product CRUD Implementation Plan
```

---

### Error 2: File Conflict Detected

**Error Message**: "File conflict detected: ProductController.java modified by 2 plans"

**Cause**: Multiple plans attempt to modify the same file

**Fix**:
1. Use interactive mode to review conflicts
2. Serialize execution: Execute plans in sequence
3. Manually merge changes if necessary

```bash
# Step 1: Review conflicts
/batch-execute --dry-run --show-conflicts

# Step 2: Execute with manual confirmation
/batch-execute --mode=interactive
```

---

### Error 3: Plan Directory Not Found

**Error Message**: "Scan directory not found: docs/plans/nonexistent/"

**Cause**: Specified directory doesn't exist

**Fix**:
1. Verify directory path
2. Use `--auto` to scan default directories
3. Check spelling and case sensitivity

```bash
# Verify directory exists
ls docs/plans/

# Use auto-scan instead
/batch-execute --auto
```

---

### Error 4: Circular Dependency Detected

**Error Message**: "Circular dependency detected: plan-a → plan-b → plan-a"

**Cause**: Plans have circular dependencies (v1.2.0 feature)

**Fix**:
1. Review plan dependencies
2. Break circular reference
3. Reorder plans manually

**Status**: Will be detected in v1.2.0

---

### Error 5: Batch Execution Timeout

**Error Message**: "Batch execution timeout: exceeded 60 minutes"

**Cause**: Too many plans or long-running operations

**Fix**:
1. Split batch into smaller groups
2. Increase timeout (if available)
3. Use `--max-concurrent` to control parallel execution

```bash
# Split into smaller batches
/batch-execute --scan-dir=docs/plans/phase1/
/batch-execute --scan-dir=docs/plans/phase2/

# Control concurrency
/batch-execute --max-concurrent=3 --auto
```

---

## Use Case Quick Templates

### Use Case 1: LiteFlow Migration (8 Phases)

**Scenario**: Migrate 8-phase LiteFlow implementation plans

**Command Sequence**:
```bash
# Step 1: Dry-run to assess conflicts
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/

# Step 2: Review report (conflict analysis, time estimate)

# Step 3: Execute with interactive confirmation
/batch-execute --mode=interactive --scan-dir=docs/plans/liteflow/
```

**Time Estimate**: 45-60 min (8 phases × 5-8 min/phase)

---

### Use Case 2: Batch CRUD Generation

**Scenario**: Generate 5 CRUD modules (Product, Order, Inventory, Supplier, Category)

**Command Sequence**:
```bash
# Step 1: Validate skill mappings
/batch-execute --dry-run --validate-mappings ~/.claude/plans/crud-batch/

# Step 2: Execute automatically (low risk)
/batch-execute --auto --scan-dir=~/.claude/plans/crud-batch/
```

**Time Estimate**: 15-25 min (5 modules × 3-5 min/module)

---

### Use Case 3: Mixed Plan Types

**Scenario**: Execute CRUD + Testing + Refactoring plans together

**Command Sequence**:
```bash
# Explicit plan list
/batch-execute \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/refactoring/vavr-migration-phase1.md

# Or use auto-scan
/batch-execute --auto --mode=interactive
```

**Time Estimate**: 30-45 min (3 plans × 10-15 min/plan)

---

### Use Case 4: High-Risk Batch Validation

**Scenario**: Validate high-risk plans before production execution

**Command Sequence**:
```bash
# Step 1: Validate skill mappings
/batch-execute --dry-run --validate-mappings --auto

# Step 2: Check for conflicts
/batch-execute --dry-run --show-conflicts --auto

# Step 3: Review dry-run report (5-10 min)

# Step 4: Execute with manual confirmation
/batch-execute --mode=interactive --on-failure=ABORT_ALL --auto
```

**Time Estimate**: 40-60 min (includes 10 min review)

---

## Time Estimates (Production Data)

| Batch Size | Dry-run | Auto Mode | Interactive Mode |
|------------|---------|-----------|------------------|
| 1-3 plans | 1-2 min | 5-10 min | 10-15 min |
| 4-7 plans | 2-3 min | 10-20 min | 20-30 min |
| 8-12 plans | 3-5 min | 20-35 min | 35-50 min |
| 13+ plans | 5-8 min | 35-60 min | 50-90 min |

**Breakdown**:
- Dry-run: 20-30 seconds per plan
- Auto Mode: 2-5 min per plan (sequential execution)
- Interactive Mode: 3-7 min per plan (includes review time)

**Parallel Execution (v1.1.0)**:
- Expected speedup: 60-80% for independent plans
- Max concurrent: 5 plans

---

## Plan Directory Defaults

Batch Plan Executor scans these directories by default:

| Directory | Description | Priority |
|-----------|-------------|----------|
| `~/.claude/plans/` | Claude Code plans | HIGH |
| `.claude/skills/*/phases/` | Skill phase plans | MEDIUM |
| `docs/plans/` | Project implementation plans | MEDIUM |

**Override**:
```bash
# Scan specific directory only
/batch-execute --scan-dir=custom/path/
```

---

## Validation Checklist (Copy-Paste)

Before executing batch:

- [ ] Run dry-run mode: `--dry-run`
- [ ] Review conflict analysis report
- [ ] Validate skill mappings: `--validate-mappings`
- [ ] Check time estimates (align with available time)
- [ ] Assess risk level (LOW/MEDIUM/HIGH)
- [ ] Choose appropriate execution mode (auto/interactive)
- [ ] Set failure strategy: `--on-failure=CONTINUE/ABORT_ALL`
- [ ] Backup critical files (if high-risk conflicts detected)
- [ ] Ensure clean git state (commit or stash changes)
- [ ] Verify all plan dependencies are met

---

**See Also**:
- [Troubleshooting Guide](../docs/troubleshooting.md) - Detailed error resolution
- [Examples](examples.md) - Real-world batch execution cases
- [Skill Mapping Guide](../docs/skill-mapping.md) - Automatic mapping rules
- [Configuration](../docs/configuration.md) - Advanced configuration options
- [Execution Reports](../docs/execution-reports.md) - Report format and interpretation
