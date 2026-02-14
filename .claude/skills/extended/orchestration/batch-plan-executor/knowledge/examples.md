# Batch Plan Executor - Real-World Examples

**Version**: 1.0.0
**Last Updated**: 2026-02-02

This document provides complete, production-ready examples of batch plan execution for common scenarios in SmartAdmin projects.

---

## Example 1: LiteFlow Migration (8 Phases)

### Scenario

Migrate complete LiteFlow rule engine implementation across 8 phases:
- Phase 1: Database schema setup
- Phase 2: Domain model creation
- Phase 3: Rule DSL implementation
- Phase 4: Service layer integration
- Phase 5: Controller endpoints
- Phase 6: Frontend components
- Phase 7: Testing suite
- Phase 8: Documentation

### Pre-Execution Planning

**Step 1: Review Plan Structure**
```bash
ls -la docs/plans/liteflow/
# phase-1-database-schema.md
# phase-2-domain-model.md
# phase-3-rule-dsl.md
# phase-4-service-integration.md
# phase-5-controller-endpoints.md
# phase-6-frontend-components.md
# phase-7-testing-suite.md
# phase-8-documentation.md
```

**Step 2: Dry-run Analysis**
```bash
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/
```

**Dry-run Report Output**:
```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Dry-run Report
╠══════════════════════════════════════════════════════════════════
║ Scan Summary
║   Total Plans Detected: 8
║   Executable Plans: 8
║   Skipped Plans: 0
╠══════════════════════════════════════════════════════════════════
║ Skill Mapping
║   Phase 1 → db-migration-manager (confidence: 95%)
║   Phase 2 → smartadmin-crud-generator (confidence: 90%)
║   Phase 3 → liteflow-rule-builder (confidence: 100%)
║   Phase 4 → smartadmin-integration-test (confidence: 85%)
║   Phase 5 → smartadmin-crud-generator (confidence: 90%)
║   Phase 6 → smartadmin-crud-generator (confidence: 90%)
║   Phase 7 → smartadmin-testing-suite (confidence: 95%)
║   Phase 8 → documentation-engineer (confidence: 80%)
╠══════════════════════════════════════════════════════════════════
║ Conflict Analysis
║   File-level Conflicts: 3 (MEDIUM)
║     - LiteFlowService.java (Phase 3, Phase 4)
║     - liteflow-api.ts (Phase 5, Phase 6)
║     - application.yml (Phase 1, Phase 4)
║   Resolution: Serialize conflicting phases
╠══════════════════════════════════════════════════════════════════
║ Time Estimation
║   Phase 1: 8 min
║   Phase 2: 12 min
║   Phase 3: 15 min
║   Phase 4: 10 min
║   Phase 5: 8 min
║   Phase 6: 10 min
║   Phase 7: 12 min
║   Phase 8: 5 min
║   Total: 80 minutes (1h 20m)
╠══════════════════════════════════════════════════════════════════
║ Risk Assessment
║   Overall Risk: MEDIUM
║   Factors:
║     - File conflicts detected (3)
║     - Multi-phase dependencies
║     - Database migration included
║   Recommendation: Use interactive mode
╚══════════════════════════════════════════════════════════════════
```

### Execution Steps

**Step 3: Execute with Interactive Mode**
```bash
/batch-execute --mode=interactive --scan-dir=docs/plans/liteflow/
```

**Interactive Execution Flow**:
```
╔══════════════════════════════════════════════════════════════════
║ Interactive Execution - Plan #1/8
╠══════════════════════════════════════════════════════════════════
║ Plan: phase-1-database-schema.md
║ Type: migration
║ Skill: db-migration-manager
║ Risk: LOW (score: 2)
║   - No conflicts
║   - Dependencies: None
╠══════════════════════════════════════════════════════════════════
║ Actions:
║   [E] Execute now (Recommended)
║   [S] Skip this plan
║   [A] Abort batch execution
║   [V] View plan details
╚══════════════════════════════════════════════════════════════════
Your choice: E

✅ Phase 1 completed successfully (7 min 23 sec)

╔══════════════════════════════════════════════════════════════════
║ Interactive Execution - Plan #2/8
╠══════════════════════════════════════════════════════════════════
║ Plan: phase-2-domain-model.md
║ Type: crud
║ Skill: smartadmin-crud-generator
║ Risk: LOW (score: 2)
╠══════════════════════════════════════════════════════════════════
Your choice: E

✅ Phase 2 completed successfully (11 min 45 sec)

[Continue for all 8 phases...]
```

**Step 4: Review Execution Report**

After all phases complete:
```
╔══════════════════════════════════════════════════════════════════
║ Batch Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Batch ID: batch-exec-20260202-140530
║ Execution Time: 78 minutes 42 seconds
║ Status: ✅ SUCCESS
╠══════════════════════════════════════════════════════════════════
║ Plan Results
║   ✅ Phase 1: SUCCESS (7m 23s)
║   ✅ Phase 2: SUCCESS (11m 45s)
║   ✅ Phase 3: SUCCESS (14m 52s)
║   ✅ Phase 4: SUCCESS (9m 38s)
║   ✅ Phase 5: SUCCESS (8m 12s)
║   ✅ Phase 6: SUCCESS (9m 55s)
║   ✅ Phase 7: SUCCESS (12m 08s)
║   ✅ Phase 8: SUCCESS (4m 49s)
╠══════════════════════════════════════════════════════════════════
║ Quality Gates
║   ✅ ArchUnit Tests: PASSED
║   ✅ Unit Tests: PASSED (coverage: 87%)
║   ✅ Integration Tests: PASSED
╠══════════════════════════════════════════════════════════════════
║ Artifacts
║   - Execution log: ~/.claude/logs/batch-exec-20260202-140530.log
║   - Git commits: 8 commits created
║   - Modified files: 47 files
╚══════════════════════════════════════════════════════════════════
```

### Post-Execution Validation

```bash
# Verify all services running
./gradlew :smartadmin-app:bootRun

# Run full test suite
./gradlew :smartadmin-app:test

# Check git history
git log --oneline -8
```

**Time Comparison**:
- **Manual Execution**: ~120-150 minutes (2-2.5 hours)
- **Batch Execution**: 78 minutes (1h 18m)
- **Time Saved**: 42-72 minutes (35-48% improvement)

---

## Example 2: Batch CRUD Generation (5 Modules)

### Scenario

Generate 5 CRUD modules simultaneously:
- Product management
- Order management
- Inventory tracking
- Supplier management
- Category management

### Plan Files

Create plans in `~/.claude/plans/crud-batch/`:
```
product-crud.md
order-crud.md
inventory-crud.md
supplier-crud.md
category-crud.md
```

**Sample Plan Structure** (product-crud.md):
```markdown
---
skill: smartadmin-crud-generator
priority: high
---

# Product CRUD Implementation

## Entity Definition
- Product ID (Long)
- Product Name (String)
- Price (BigDecimal)
- Stock Quantity (Integer)
- Category ID (Long, FK)

## Features
- Full CRUD operations
- Pagination support
- Search by name
- Price range filter
- Stock alerts
```

### Execution Steps

**Step 1: Validate Skill Mappings**
```bash
/batch-execute --dry-run --validate-mappings --scan-dir=~/.claude/plans/crud-batch/
```

**Output**:
```
✅ All plans successfully mapped to smartadmin-crud-generator
Confidence: 100% (explicit skill annotation)
No conflicts detected (independent modules)
Estimated time: 15-25 minutes (5 modules × 3-5 min)
```

**Step 2: Execute Automatically**
```bash
/batch-execute --auto --scan-dir=~/.claude/plans/crud-batch/
```

**Execution Flow**:
```
Starting batch execution (auto mode)...

[1/5] Executing: product-crud
  ✅ ProductEntity created
  ✅ ProductController generated
  ✅ ProductService generated
  ✅ Product frontend components created
  ✅ Completed in 4m 12s

[2/5] Executing: order-crud
  ✅ OrderEntity created
  ✅ OrderController generated
  ✅ Completed in 4m 38s

[3/5] Executing: inventory-crud
  ✅ Completed in 3m 55s

[4/5] Executing: supplier-crud
  ✅ Completed in 4m 05s

[5/5] Executing: category-crud
  ✅ Completed in 3m 42s

╔══════════════════════════════════════════════════════════════════
║ Batch Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Total Time: 20 minutes 32 seconds
║ Status: ✅ ALL SUCCESS (5/5)
║ Files Created: 85 files
║ Git Commits: 5 commits
╚══════════════════════════════════════════════════════════════════
```

**Time Comparison**:
- **Manual Execution**: ~25-35 minutes (5 modules × 5-7 min)
- **Batch Execution**: 20 minutes 32 seconds
- **Time Saved**: 4-14 minutes (16-40% improvement)

---

## Example 3: Mixed Plan Types

### Scenario

Execute mixed plan types in one batch:
1. Product CRUD generation
2. Integration tests for Product module
3. Refactor Product Service to use Vavr

### Plan Files

```
~/.claude/plans/product-crud.md
.claude/skills/smartadmin-testing-suite/phases/phase-2-product-integration-tests.md
docs/plans/refactoring/product-service-vavr.md
```

### Conflict Analysis

**Step 1: Dry-run to Detect Conflicts**
```bash
/batch-execute --dry-run \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-product-integration-tests.md \
  docs/plans/refactoring/product-service-vavr.md
```

**Output**:
```
╔══════════════════════════════════════════════════════════════════
║ Conflict Analysis
╠══════════════════════════════════════════════════════════════════
║ File-level Conflicts: 1 (HIGH)
║   ProductService.java:
║     - Plan 1 (product-crud): Create service methods
║     - Plan 3 (vavr-refactoring): Refactor return types to Vavr
║
║ Resolution: Serialize conflicting plans
║   Group 1: [product-crud] → Execute first
║   Group 2: [product-integration-tests, vavr-refactoring] → Execute after
║
║ Recommended Execution Order:
║   1. product-crud (creates ProductService.java)
║   2. product-integration-tests (depends on CRUD completion)
║   3. vavr-refactoring (refactors existing service)
╚══════════════════════════════════════════════════════════════════
```

### Execution Steps

**Step 2: Execute with Recommended Order**
```bash
/batch-execute --mode=interactive \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-product-integration-tests.md \
  docs/plans/refactoring/product-service-vavr.md
```

**Execution Flow**:
```
[1/3] Executing: product-crud
  ✅ ProductEntity created
  ✅ ProductController generated
  ✅ ProductService generated (with Optional return types)
  ✅ Completed in 4m 18s

[2/3] Executing: product-integration-tests
  ✅ ProductControllerIntegrationTest created
  ✅ Test containers configured
  ✅ 12 integration tests passing
  ✅ Completed in 6m 42s

[3/3] Executing: vavr-refactoring
  ✅ ProductService methods refactored (Optional → Option)
  ✅ ArchUnit tests passing
  ✅ All tests passing (coverage: 92%)
  ✅ Completed in 7m 15s

╔══════════════════════════════════════════════════════════════════
║ Batch Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Total Time: 18 minutes 15 seconds
║ Status: ✅ ALL SUCCESS (3/3)
║ Quality Gates: ✅ PASSED
╚══════════════════════════════════════════════════════════════════
```

**Time Comparison**:
- **Manual Execution**: ~25-30 minutes (switching context between tasks)
- **Batch Execution**: 18 minutes 15 seconds
- **Time Saved**: 7-12 minutes (28-40% improvement)

---

## Example 4: High-Risk Batch with Validation

### Scenario

Execute high-risk batch involving:
- Database schema changes
- Security hardening updates
- Critical service refactoring

### Plan Files

```
docs/plans/migration/database-schema-v2.md
docs/plans/security/sm2-encryption.md
docs/plans/refactoring/critical-service-refactoring.md
```

### Safety-First Execution

**Step 1: Comprehensive Validation**
```bash
# Validate skill mappings
/batch-execute --dry-run --validate-mappings --scan-dir=docs/plans/

# Check for conflicts
/batch-execute --dry-run --show-conflicts --scan-dir=docs/plans/

# Review time estimates
/batch-execute --dry-run --show-time-estimates --scan-dir=docs/plans/
```

**Step 2: Backup Critical Files**
```bash
# Create backup before execution
git stash
cp -r src/ src.backup.$(date +%Y%m%d-%H%M%S)/
```

**Step 3: Execute with ABORT_ALL Strategy**
```bash
/batch-execute --mode=interactive --on-failure=ABORT_ALL --scan-dir=docs/plans/
```

**Execution Flow with Failure**:
```
[1/3] Executing: database-schema-v2
  ✅ Migration scripts generated
  ✅ Flyway migration executed
  ✅ Completed in 10m 35s

[2/3] Executing: sm2-encryption
  ✅ SM2 encryption service created
  ❌ FAILED: Unit tests failing (SM2 library not found)

╔══════════════════════════════════════════════════════════════════
║ Batch Execution Aborted
╠══════════════════════════════════════════════════════════════════
║ Reason: Plan #2 (sm2-encryption) failed
║ Failure Strategy: ABORT_ALL
║ Status: PARTIAL SUCCESS (1/3)
║
║ Completed Plans:
║   ✅ database-schema-v2
║
║ Failed Plans:
║   ❌ sm2-encryption (error: SM2 library dependency missing)
║
║ Skipped Plans:
║   ⏭️ critical-service-refactoring (aborted due to failure)
╚══════════════════════════════════════════════════════════════════
```

**Step 4: Fix Issue and Retry**
```bash
# Fix dependency issue
./gradlew :smartadmin-app:dependencies --add "org.bouncycastle:bcprov-jdk15on:1.70"

# Retry only failed plans
/batch-execute --retry-failed=batch-exec-20260202-153000
```

**Retry Execution**:
```
Retrying failed plans from batch-exec-20260202-153000...

[1/2] Executing: sm2-encryption (retry)
  ✅ SM2 library found
  ✅ Unit tests passing
  ✅ Completed in 5m 20s

[2/2] Executing: critical-service-refactoring
  ✅ Service refactored
  ✅ All tests passing
  ✅ Completed in 8m 42s

╔══════════════════════════════════════════════════════════════════
║ Retry Batch Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Status: ✅ ALL SUCCESS (2/2)
║ Original Batch: 1 success, 1 failed, 1 skipped
║ After Retry: 3/3 success
╚══════════════════════════════════════════════════════════════════
```

---

## Example 5: Parallel Execution Optimization (v1.1.0)

### Scenario (Future Feature)

Execute 10 independent CRUD modules with parallel execution.

### Expected Improvement

**Sequential Execution (v1.0.0)**:
```
Total Time: 10 modules × 4 min = 40 minutes
```

**Parallel Execution (v1.1.0)**:
```
Concurrency: 5 plans
Batch 1: Plans 1-5 (parallel) = 4 minutes
Batch 2: Plans 6-10 (parallel) = 4 minutes
Total Time: 8 minutes (80% time reduction)
```

**Command**:
```bash
# v1.1.0 feature (planned)
/batch-execute --max-concurrent=5 --auto --scan-dir=~/.claude/plans/crud-batch/
```

---

## Best Practices Summary

1. **Always Run Dry-run First**
   ```bash
   /batch-execute --dry-run --auto
   ```

2. **Choose Mode Based on Risk**
   - Low risk → Auto mode
   - High risk → Interactive mode

3. **Use ABORT_ALL for Critical Operations**
   ```bash
   --on-failure=ABORT_ALL
   ```

4. **Backup Before High-Risk Batches**
   ```bash
   git stash
   cp -r src/ src.backup/
   ```

5. **Retry Failed Plans**
   ```bash
   /batch-execute --retry-failed=<batch-id>
   ```

---

**See Also**:
- [Quick Reference](quick-reference.md) - Command reference and decision matrix
- [Troubleshooting Guide](troubleshooting.md) - Error resolution
- [Skill Mapping Reference](../references/skill-mapping.md) - Complete mapping rules
