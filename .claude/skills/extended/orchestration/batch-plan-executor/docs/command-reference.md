## Command Reference

### Basic Commands

```bash
# Auto-scan and execute all plans
/batch-execute --auto

# Execute specific plans
/batch-execute plan1.md plan2.md plan3.md

# Scan specific directory
/batch-execute --scan-dir=docs/plans/liteflow/

# Dry-run mode
/batch-execute --dry-run

# Interactive mode
/batch-execute --mode=interactive
```

### Command Options

| Option | Description | Default |
|--------|-------------|---------|
| `--auto` | Auto-scan all configured directories | false |
| `--mode=<mode>` | Execution mode (auto/interactive/dry-run) | auto |
| `--dry-run` | Simulation mode (no actual execution) | false |
| `--scan-dir=<path>` | Scan specific directory | All default dirs |
| `--max-concurrent=<n>` | Maximum concurrent executions | 5 |
| `--on-failure=<strategy>` | Failure strategy (CONTINUE/ABORT_ALL/PAUSE) | CONTINUE |
| `--validate-mappings` | Validate skill mappings | true (dry-run) |
| `--show-conflicts` | Show detailed conflict information | true (dry-run) |
| `--allow-reorder` | Allow plan reordering | false |
| `--retry-failed=<batch-id>` | Retry failed plans from previous batch | - |

### Advanced Commands

```bash
# Execute with custom concurrency
/batch-execute --max-concurrent=3 --auto

# Stop on first failure
/batch-execute --on-failure=ABORT_ALL --auto

# Retry failed plans from previous batch
/batch-execute --retry-failed=batch-exec-20260129-153000

# Validate mappings only (no execution)
/batch-execute --dry-run --validate-mappings --no-estimate-time

# Show detailed conflict analysis
/batch-execute --dry-run --show-conflicts --scan-dir=docs/plans/
```

### Command Examples

**Example 1: LiteFlow Migration (8 phases)**

```bash
# Dry-run to assess conflicts
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/

# Review report, then execute
/batch-execute --scan-dir=docs/plans/liteflow/ --mode=interactive
```

**Example 2: Mixed Plan Types**

```bash
# Execute CRUD + Testing + Migration plans
/batch-execute \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md
```

**Example 3: High-Risk Batch with Validation**

```bash
# Step 1: Validate skill mappings
/batch-execute --dry-run --validate-mappings --auto

# Step 2: Check for conflicts
/batch-execute --dry-run --show-conflicts --auto

# Step 3: Execute with interactive confirmation
/batch-execute --mode=interactive --auto
```

---

