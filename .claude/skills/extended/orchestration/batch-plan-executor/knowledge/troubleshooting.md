# Batch Plan Executor - Troubleshooting Guide

**Version**: 1.0.0
**Last Updated**: 2026-02-02

This document provides detailed troubleshooting guidance for common issues encountered during batch plan execution.

---

## Skill Mapping Issues

### Problem 1: Automatic Mapping Failed

**Symptoms**:
```
ERROR: Cannot map plan [customer-management.md] to skill
No matching skill found for plan type: [unknown]
```

**Root Causes**:
1. Plan content doesn't contain recognizable keywords
2. Plan file is malformed or empty
3. Custom plan type not in mapping rules

**Diagnosis Steps**:
```bash
# Step 1: Run dry-run with validation
/batch-execute --dry-run --validate-mappings customer-management.md

# Step 2: Check plan content
cat customer-management.md | head -50

# Step 3: Verify mapping confidence
/batch-execute --dry-run --show-mapping-confidence
```

**Solutions**:

**Solution 1: Add Explicit Skill Mapping**
```markdown
<!-- skill: smartadmin-crud-generator -->
# Customer Management CRUD Plan
```

**Solution 2: Add YAML Frontmatter**
```yaml
---
skill: smartadmin-crud-generator
confidence: 100
---
# Customer Management CRUD Plan
```

**Solution 3: Increase Keyword Density**
```markdown
# Customer Management CRUD Plan

## Objective
Generate full CRUD operations for Customer entity using smartadmin-crud-generator.

## Tasks
- Create CustomerEntity
- Generate CRUD endpoints
- Build frontend components
```

---

### Problem 2: Wrong Skill Mapped

**Symptoms**:
```
WARNING: Low confidence mapping (65%)
Plan: refactoring-service-layer.md
Mapped to: smartadmin-crud-generator (expected: vavr-refactoring-assistant)
```

**Root Causes**:
1. Conflicting keywords (e.g., "create" vs "refactor")
2. Plan description too generic
3. Mapping confidence threshold too low

**Solutions**:

**Solution 1: Override Automatic Mapping**
```markdown
<!-- skill: vavr-refactoring-assistant -->
# Refactor Service Layer to Use Vavr Option
```

**Solution 2: Improve Plan Description**
```markdown
# Service Layer Vavr Refactoring Plan

**Goal**: Refactor Service layer methods from java.util.Optional to io.vavr.control.Option

**NOT**: Create new CRUD operations (use vavr-refactoring-assistant, not CRUD generator)
```

---

## Conflict Detection Issues

### Problem 3: False Positive File Conflicts

**Symptoms**:
```
WARNING: File conflict detected
File: ProductService.java
Conflicting Plans: [product-crud, product-refactoring]
Recommendation: Serialize execution
```

**Actual Situation**: Plans modify different methods in the same file (no real conflict)

**Root Causes**:
1. File-level conflict detection too coarse (v1.0.0 limitation)
2. Cannot detect method-level changes

**Solutions**:

**Solution 1: Ignore Warning (Safe if Different Methods)**
```bash
# Proceed with auto mode
/batch-execute --auto --ignore-file-conflicts
```

**Solution 2: Use Interactive Mode for Safety**
```bash
# Manual review before execution
/batch-execute --mode=interactive
```

**Solution 3: Wait for v1.1.0 (Method-Level Conflict Detection)**
- Planned feature: Detect conflicts at method/function level
- ETA: v1.1.0

---

### Problem 4: Missed Conflict (False Negative)

**Symptoms**:
```
SUCCESS: Batch execution completed
Plan 1 (product-crud): ✅ SUCCESS
Plan 2 (inventory-update): ✅ SUCCESS

[Later] ERROR: Test failed - ProductController method conflict
```

**Root Causes**:
1. File path extraction failed (Strategy 1-4 didn't detect file)
2. Implicit file modification not detected
3. Dependency conflict not detected (v1.2.0 feature)

**Solutions**:

**Solution 1: Add Explicit File References**
```markdown
# Plan: product-crud

## Affected Files
- `src/main/java/.../ProductController.java`
- `src/main/java/.../ProductService.java`
```

**Solution 2: Run Tests After Each Plan**
```bash
# Use interactive mode with test verification
/batch-execute --mode=interactive --verify-after-each
```

**Solution 3: Use Dry-run First**
```bash
# Always run dry-run before real execution
/batch-execute --dry-run --show-conflicts --auto

# Review report, then execute
/batch-execute --auto
```

---

## Execution Failures

### Problem 5: Plan Execution Timeout

**Symptoms**:
```
ERROR: Plan execution timeout
Plan: liteflow-phase-3 (database-schema-migration)
Timeout: 600 seconds
```

**Root Causes**:
1. Long-running operations (database migration, large code generation)
2. Waiting for user input (interactive prompts)
3. Skill execution hang (infinite loop)

**Solutions**:

**Solution 1: Increase Timeout**
```bash
# Custom timeout (in seconds)
/batch-execute --plan-timeout=1800 --scan-dir=docs/plans/
```

**Solution 2: Execute Plan Individually**
```bash
# Execute timeout plan separately
/liteflow-rule-builder docs/plans/liteflow/phase-3-database-schema.md
```

**Solution 3: Check for Interactive Prompts**
```markdown
# Review plan for blocking prompts
grep -r "AskUserQuestion" docs/plans/liteflow/phase-3-database-schema.md
```

---

### Problem 6: Partial Batch Failure

**Symptoms**:
```
PARTIAL SUCCESS: Batch execution completed with errors
✅ Plan 1 (product-crud): SUCCESS
✅ Plan 2 (order-crud): SUCCESS
❌ Plan 3 (inventory-crud): FAILED
⏭️ Plan 4 (supplier-crud): SKIPPED
⏭️ Plan 5 (category-crud): SKIPPED
```

**Root Causes**:
1. Plan 3 execution error (compilation, test failure, etc.)
2. Default failure strategy: `CONTINUE` (continues execution)
3. Plans 4-5 skipped due to dependency on Plan 3

**Solutions**:

**Solution 1: Retry Failed Plans**
```bash
# Retry only failed plans from previous batch
/batch-execute --retry-failed=batch-exec-20260129-153000
```

**Solution 2: Fix Plan 3 and Re-execute**
```bash
# Debug Plan 3
cat ~/.claude/plans/inventory-crud.md

# Fix issues, then re-execute
/batch-execute --scan-dir=~/.claude/plans/ --mode=interactive
```

**Solution 3: Use ABORT_ALL Strategy**
```bash
# Stop on first failure
/batch-execute --on-failure=ABORT_ALL --auto
```

---

### Problem 7: Skill Execution Error

**Symptoms**:
```
ERROR: Skill execution failed
Plan: security-hardening-phase-1
Skill: security-hardening-pro
Error: Cannot find SM2 encryption library
```

**Root Causes**:
1. Skill dependency missing (library, tool, service)
2. Skill configuration incomplete
3. Environment setup incorrect

**Solutions**:

**Solution 1: Check Skill Dependencies**
```bash
# Read skill requirements
cat .claude/skills/foundation/backend/security-hardening-pro/README.md

# Verify dependencies
./gradlew :smartadmin-app:dependencies | grep -i "sm2\|sm3\|sm4"
```

**Solution 2: Execute Skill Manually for Debugging**
```bash
# Run skill directly to see detailed error
/security-hardening-pro docs/plans/security/phase-1-encryption.md
```

**Solution 3: Check Skill Configuration**
```yaml
# Verify skill config
cat .claude/skills/foundation/backend/security-hardening-pro/config.yml
```

---

## Performance Issues

### Problem 8: Slow Batch Execution

**Symptoms**:
```
Batch execution time: 45 minutes
Expected time (from dry-run): 20 minutes
```

**Root Causes**:
1. Sequential execution (v1.0.0 limitation)
2. Network-bound operations (API calls, external services)
3. Large file operations (code generation, compilation)

**Solutions**:

**Solution 1: Upgrade to v1.1.0 (Parallel Execution)**
- Planned feature: Parallel execution (up to 5 plans)
- Expected speedup: 60-80%

**Solution 2: Split Batch into Smaller Groups**
```bash
# Execute in phases
/batch-execute --scan-dir=docs/plans/phase-1/
/batch-execute --scan-dir=docs/plans/phase-2/
```

**Solution 3: Optimize Individual Plans**
```bash
# Profile slow plans
/batch-execute --dry-run --show-time-estimates --auto

# Optimize bottleneck plans
```

---

### Problem 9: High Memory Usage

**Symptoms**:
```
WARNING: High memory usage detected
Current: 8.2GB
Threshold: 8GB
Plan: batch-invoice-processing
```

**Root Causes**:
1. Large plan file (100+ MB)
2. Many concurrent skills (v1.1.0+)
3. Memory leak in skill execution

**Solutions**:

**Solution 1: Reduce Concurrent Plans**
```bash
# Limit parallel execution (v1.1.0+)
/batch-execute --max-concurrent=2 --auto
```

**Solution 2: Split Large Plans**
```bash
# Split into smaller batches
split -l 1000 large-plan.md small-plan-
```

**Solution 3: Increase JVM Heap**
```bash
# Increase Claude Code memory
export CLAUDE_CODE_MAX_HEAP=12g
```

---

## Report Interpretation

### Problem 10: Confusing Dry-run Report

**Symptoms**:
```
Dry-run Report shows:
- Total Plans: 8
- Executable Plans: 5
- Skipped Plans: 3

Question: Why are 3 plans skipped?
```

**Common Reasons for Skipped Plans**:
1. **No Skill Mapping**: Plan type unknown, confidence < 80%
2. **Duplicate Plans**: Same plan detected multiple times
3. **Invalid Plan Format**: Markdown parsing failed
4. **Dependency Not Met**: Required plan not in batch (v1.2.0)

**Solutions**:

**Solution 1: Check Skipped Plan Details**
```bash
# Show detailed skip reasons
/batch-execute --dry-run --show-skipped-details --auto
```

**Solution 2: Add Missing Skill Mappings**
```markdown
<!-- skill: appropriate-skill-name -->
```

**Solution 3: Fix Invalid Format**
```bash
# Validate plan format
cat skipped-plan.md | head -50
```

---

## Best Practices to Avoid Issues

### Pre-Execution Checklist

- [ ] **Always run dry-run first**
  ```bash
  /batch-execute --dry-run --auto
  ```

- [ ] **Review conflict analysis**
  - Check file-level conflicts
  - Review risk assessment

- [ ] **Validate skill mappings**
  - Ensure all plans mapped correctly
  - Confidence ≥ 80%

- [ ] **Choose appropriate mode**
  - Low risk → Auto mode
  - High risk → Interactive mode

- [ ] **Set failure strategy**
  - Critical operations → `ABORT_ALL`
  - Routine operations → `CONTINUE`

- [ ] **Clean git state**
  ```bash
  git status  # Ensure no uncommitted changes
  ```

- [ ] **Backup critical files** (if high-risk conflicts detected)
  ```bash
  cp -r src/ src.backup/
  ```

---

## Quality Gate Failures

### Problem 11: ArchUnit Test Failure After Execution

**Symptoms**:
```
ERROR: ArchUnit test failed after plan execution
Plan: product-crud
Test: transactionalMustUseRollbackForThrowable
```

**Root Causes**:
1. Generated code violates SmartAdmin architecture rules
2. Skill implementation does not conform to SmartAdmin conventions
3. `@Transactional` placed in Service instead of Manager layer

**Solutions**:

**Solution 1: Check Detailed Error Information**
```bash
# View execution logs
cat .claude/metrics/batch-executions/batch-exec-*.log
```

**Solution 2: Fix Architecture Violations**
- Review generated code
- Fix violations (e.g., replace `@Autowired` field injection with constructor injection)
- Move `@Transactional` annotations from Service to Manager layer

**Solution 3: Temporarily Disable Quality Gate** (Not recommended):
```yaml
execution:
  quality_gates:
    enabled: false  # Only for testing
```

---

### Problem 12: Inaccurate Time Estimates

**Symptoms**:
```
WARNING: Actual execution time differs from estimate by > 50%
Estimated: 20 minutes
Actual: 45 minutes
```

**Root Causes**:
1. Default time estimates not calibrated
2. Large variance in plan complexity
3. System resource fluctuations

**Solutions**:

**Solution 1: Calibrate Time Estimates**
```yaml
# config.yml - Add custom estimates
time_estimation:
  plan_type_estimates:
    crud: 30  # Adjust from default 25
    testing: 25  # Adjust from default 20
```

**Solution 2: Provide Plan Size Hints**
```yaml
---
estimated_duration_minutes: 45
---
```

**Solution 3: Use Historical Data**
- After several executions, the system will auto-calibrate estimates

---

## Getting Help

### Debug Mode

Enable debug logging for detailed information:

```bash
# Enable debug mode (future feature)
/batch-execute --debug --verbose --auto
```

### Contact Support

If issues persist:
1. Export execution logs: `~/.claude/logs/batch-executor/`
2. Include dry-run report
3. Provide plan files
4. Report GitHub issue: [smartadmin-issues](https://github.com/smart-admin/issues)

---

**See Also**:
- [Quick Reference](quick-reference.md) - Command reference and decision matrix
- [Examples](examples.md) - Real-world batch execution cases
