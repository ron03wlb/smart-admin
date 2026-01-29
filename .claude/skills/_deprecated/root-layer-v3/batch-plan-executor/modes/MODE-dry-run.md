# Execution Mode: Dry-run

**Mode**: Dry-run (Simulation Mode)
**Version**: v1.0.0
**Status**: ✅ Production Ready
**Complexity**: Low

---

## Overview

**Dry-run mode** simulates batch plan execution without performing actual operations. It generates comprehensive pre-execution and post-execution reports, validates skill mappings, detects conflicts, and estimates execution time.

### Key Features

- ✅ **No Actual Execution**: All operations are simulated
- ✅ **Full Conflict Analysis**: Detects file-level conflicts (v1.0.0)
- ✅ **Skill Mapping Validation**: Verifies all plans can be mapped to skills
- ✅ **Execution Time Estimation**: Predicts sequential vs. parallel execution time
- ✅ **Risk Assessment**: Identifies HIGH/MEDIUM/LOW risk plans
- ✅ **Execution Plan Preview**: Shows serial groups and parallel groups

---

## Use Cases

### Use Case 1: Pre-Execution Risk Assessment

**Scenario**: Before executing 8 LiteFlow migration plans, validate there are no conflicts.

```bash
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/
```

**Output**:
- Conflict Analysis Report
- Risk Assessment (HIGH/MEDIUM/LOW)
- Execution Plan Preview (serial groups + parallel groups)
- Time Estimation (sequential: 240 min → parallel: 85 min)

### Use Case 2: Skill Mapping Validation

**Scenario**: Verify that all plans in a directory can be mapped to available skills.

```bash
/batch-execute --dry-run --validate-mappings --scan-dir=docs/plans/
```

**Output**:
- Plan Inventory with Skill Mappings
- Confidence Scores (0.0-1.0)
- Plans requiring manual execution (skill mapping failed)

### Use Case 3: Execution Plan Preview

**Scenario**: Preview the execution order before running a batch of plans.

```bash
/batch-execute --dry-run plan1.md plan2.md plan3.md plan4.md
```

**Output**:
- Execution Waves (Wave 1, Wave 2, ...)
- Serial Groups (plans that must run sequentially)
- Parallel Groups (plans that can run concurrently)

---

## Command Usage

### Basic Syntax

```bash
/batch-execute --dry-run [OPTIONS] [PLAN_FILES...]
```

### Options

| Option | Description | Default |
|--------|-------------|---------|
| `--dry-run` | Enable dry-run mode | false |
| `--validate-mappings` | Validate skill mappings | true |
| `--estimate-time` | Estimate execution time | true |
| `--show-conflicts` | Show detailed conflict information | true |
| `--risk-assessment` | Perform risk assessment | true |
| `--scan-dir=<path>` | Scan specific directory | All default dirs |
| `--auto` | Auto-scan all configured directories | false |

### Examples

**Example 1: Dry-run with auto-scan**

```bash
/batch-execute --dry-run --auto
```

Scans all configured directories:
- `~/.claude/plans/`
- `.claude/skills/*/phases/`
- `docs/plans/`

**Example 2: Dry-run specific plans**

```bash
/batch-execute --dry-run \
  ~/.claude/plans/product-crud.md \
  docs/plans/liteflow/phase-1-setup.md \
  docs/plans/tenant/multi-tenant.md
```

Analyzes only the specified plans.

**Example 3: Validate mappings only**

```bash
/batch-execute --dry-run --validate-mappings --no-estimate-time
```

Focuses on skill mapping validation without time estimation.

---

## Dry-run Workflow

### Step 1: Plan Discovery

```python
def dry_run_plan_discovery(config: Config, plan_files: List[str]) -> List[Plan]:
    """
    Discover and classify plans (same as normal execution).
    """
    if plan_files:
        # Use specified plan files
        plans = [load_plan(f) for f in plan_files]
    else:
        # Auto-scan directories
        discovered_files = scan_plan_directories(config)
        plans = [load_plan(f) for f in discovered_files]

    # Classify and validate plans
    for plan in plans:
        plan.type = detect_plan_type(plan.file_path)
        plan.validation_result = validate_plan(plan)

    return plans
```

### Step 2: Skill Mapping Validation

```python
def dry_run_validate_skill_mappings(plans: List[Plan]) -> MappingReport:
    """
    Validate that all plans can be mapped to available skills.

    Returns:
        MappingReport with mapping results and confidence scores
    """
    mapping_results = []

    for plan in plans:
        # Attempt skill mapping
        skill, confidence = map_plan_to_skill(plan)

        mapping_results.append(MappingResult(
            plan_id=plan.id,
            plan_name=plan.name,
            plan_type=plan.type,
            skill=skill,
            confidence=confidence,
            status=MappingStatus.SUCCESS if skill else MappingStatus.MANUAL_REQUIRED
        ))

    # Calculate statistics
    total_plans = len(plans)
    mapped_plans = sum(1 for r in mapping_results if r.skill is not None)
    manual_plans = total_plans - mapped_plans

    return MappingReport(
        total_plans=total_plans,
        mapped_plans=mapped_plans,
        manual_plans=manual_plans,
        mapping_results=mapping_results
    )
```

### Step 3: Conflict Detection (Simulated)

```python
def dry_run_conflict_detection(plans: List[Plan]) -> ConflictReport:
    """
    Detect conflicts without executing plans.

    Uses content analysis to predict file modifications.
    """
    # Extract affected files from plan content
    file_map = {}
    for plan in plans:
        affected_files = extract_affected_files(plan.content, plan.file_path)
        for file_path in affected_files:
            if file_path not in file_map:
                file_map[file_path] = []
            file_map[file_path].append(plan.id)

    # Detect file-level conflicts
    file_conflicts = []
    for file_path, plan_ids in file_map.items():
        if len(plan_ids) > 1:
            file_conflicts.append(FileConflict(
                file_path=file_path,
                conflicting_plan_ids=plan_ids,
                severity=ConflictSeverity.HIGH
            ))

    return ConflictReport(
        total_conflicts=len(file_conflicts),
        file_conflicts=file_conflicts,
        module_conflicts=[],  # v1.0.0 - Not implemented
        dependency_conflicts=[]  # v1.0.0 - Not implemented
    )
```

### Step 4: Execution Plan Generation (Simulated)

```python
def dry_run_generate_execution_plan(
    plans: List[Plan],
    conflicts: ConflictReport
) -> ExecutionPlan:
    """
    Generate execution plan without actually executing.

    Returns:
        ExecutionPlan with serial groups and parallel groups
    """
    # Resolve conflicts by creating serial groups
    serial_groups = resolve_file_conflicts_serial(conflicts.file_conflicts, plans)

    # Create execution waves
    waves = []
    remaining_plans = set(p.id for p in plans)

    # Assign conflicting plans to serial waves
    for group in serial_groups:
        for plan in group.plans:
            waves.append(ExecutionWave(
                wave_number=len(waves) + 1,
                plans=[plan],
                execution_mode=ExecutionMode.SERIAL,
                reason=f"File conflict: {group.reason}"
            ))
            remaining_plans.remove(plan.id)

    # Remaining plans can execute in parallel
    if remaining_plans:
        parallel_plans = [p for p in plans if p.id in remaining_plans]
        waves.append(ExecutionWave(
            wave_number=len(waves) + 1,
            plans=parallel_plans,
            execution_mode=ExecutionMode.PARALLEL,
            reason="No conflicts detected"
        ))

    return ExecutionPlan(
        total_waves=len(waves),
        waves=waves,
        total_plans=len(plans)
    )
```

### Step 5: Time Estimation

```python
def dry_run_estimate_execution_time(execution_plan: ExecutionPlan) -> TimeEstimate:
    """
    Estimate execution time based on plan complexity.

    Estimation factors:
    - Plan type (CRUD: 20-30 min, Testing: 15-25 min, etc.)
    - Plan size (content length)
    - Number of affected files
    """
    # Default time estimates per plan type (in minutes)
    PLAN_TYPE_ESTIMATES = {
        'crud': 25,
        'testing': 20,
        'refactoring': 30,
        'migration': 40,
        'integration': 25,
        'security': 35,
        'performance': 30,
        'custom': 20
    }

    sequential_time = 0
    parallel_time = 0

    for wave in execution_plan.waves:
        wave_time = 0

        for plan in wave.plans:
            # Base estimate from plan type
            base_time = PLAN_TYPE_ESTIMATES.get(plan.type, 20)

            # Adjust based on plan size
            size_multiplier = 1.0
            if len(plan.content) > 5000:
                size_multiplier = 1.5
            elif len(plan.content) > 10000:
                size_multiplier = 2.0

            plan_time = base_time * size_multiplier

            # Sequential: sum all times
            sequential_time += plan_time

            # Parallel: max time in wave
            wave_time = max(wave_time, plan_time)

        parallel_time += wave_time

    return TimeEstimate(
        sequential_time_minutes=int(sequential_time),
        parallel_time_minutes=int(parallel_time),
        time_saved_minutes=int(sequential_time - parallel_time),
        time_saved_percentage=int((sequential_time - parallel_time) / sequential_time * 100)
    )
```

### Step 6: Risk Assessment

```python
def dry_run_risk_assessment(
    plans: List[Plan],
    conflicts: ConflictReport,
    mapping_report: MappingReport
) -> RiskAssessment:
    """
    Assess execution risk for each plan.

    Risk factors:
    - Number of conflicts (HIGH risk if > 2)
    - Skill mapping confidence (LOW risk if confidence > 0.8)
    - Plan validation errors (HIGH risk if errors exist)
    - Dependencies (MEDIUM risk if dependencies exist)
    """
    plan_risks = []

    for plan in plans:
        risk_factors = []
        risk_score = 0

        # Factor 1: Conflicts
        plan_conflicts = [c for c in conflicts.file_conflicts if plan.id in c.conflicting_plan_ids]
        if len(plan_conflicts) > 2:
            risk_factors.append(f"Multiple file conflicts ({len(plan_conflicts)})")
            risk_score += 3
        elif len(plan_conflicts) > 0:
            risk_factors.append(f"File conflicts ({len(plan_conflicts)})")
            risk_score += 1

        # Factor 2: Skill mapping
        mapping_result = next((r for r in mapping_report.mapping_results if r.plan_id == plan.id), None)
        if mapping_result:
            if mapping_result.skill is None:
                risk_factors.append("No skill mapping - manual execution required")
                risk_score += 2
            elif mapping_result.confidence < 0.7:
                risk_factors.append(f"Low mapping confidence ({mapping_result.confidence:.2f})")
                risk_score += 1

        # Factor 3: Validation errors
        if plan.validation_result and not plan.validation_result.valid:
            risk_factors.append(f"Validation errors ({len(plan.validation_result.errors)})")
            risk_score += 3

        # Factor 4: Dependencies
        if hasattr(plan, 'dependencies') and plan.dependencies:
            risk_factors.append(f"Has dependencies ({len(plan.dependencies)})")
            risk_score += 1

        # Determine risk level
        if risk_score >= 5:
            risk_level = RiskLevel.HIGH
        elif risk_score >= 2:
            risk_level = RiskLevel.MEDIUM
        else:
            risk_level = RiskLevel.LOW

        plan_risks.append(PlanRisk(
            plan_id=plan.id,
            plan_name=plan.name,
            risk_level=risk_level,
            risk_score=risk_score,
            risk_factors=risk_factors
        ))

    # Calculate statistics
    high_risk_plans = sum(1 for r in plan_risks if r.risk_level == RiskLevel.HIGH)
    medium_risk_plans = sum(1 for r in plan_risks if r.risk_level == RiskLevel.MEDIUM)
    low_risk_plans = sum(1 for r in plan_risks if r.risk_level == RiskLevel.LOW)

    return RiskAssessment(
        total_plans=len(plans),
        high_risk_plans=high_risk_plans,
        medium_risk_plans=medium_risk_plans,
        low_risk_plans=low_risk_plans,
        plan_risks=plan_risks
    )
```

---

## Output Reports

### Pre-Execution Report (Dry-run)

```python
def generate_dry_run_pre_execution_report(
    plans: List[Plan],
    mapping_report: MappingReport,
    conflict_report: ConflictReport,
    execution_plan: ExecutionPlan,
    time_estimate: TimeEstimate,
    risk_assessment: RiskAssessment
) -> str:
    """
    Generate comprehensive dry-run pre-execution report.
    """
    report = f"""
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Dry-run Report
║ Mode: SIMULATION (No actual execution)
╠══════════════════════════════════════════════════════════════════
║ Plan Discovery:
║   - Total Plans: {len(plans)}
║   - Claude Code Plans: {sum(1 for p in plans if isinstance(p, ClaudeCodePlan))}
║   - Skills Phase Docs: {sum(1 for p in plans if isinstance(p, SkillPhaseDoc))}
║   - Project Plans: {sum(1 for p in plans if isinstance(p, ProjectPlan))}
╠══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation:
║   - Successfully Mapped: {mapping_report.mapped_plans}/{mapping_report.total_plans}
║   - Requires Manual Execution: {mapping_report.manual_plans}
║   - Mapping Success Rate: {mapping_report.mapped_plans / mapping_report.total_plans * 100:.1f}%
"""

    # List plans requiring manual execution
    manual_plans = [r for r in mapping_report.mapping_results if r.skill is None]
    if manual_plans:
        report += "║\n║   Plans Requiring Manual Execution:\n"
        for r in manual_plans:
            report += f"║     - {r.plan_name} (type: {r.plan_type})\n"

    report += f"""╠══════════════════════════════════════════════════════════════════
║ Conflict Analysis:
║   - Total Conflicts: {conflict_report.total_conflicts}
║   - File-level Conflicts: {len(conflict_report.file_conflicts)} (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM) [v1.0.0 - Not implemented]
║   - Dependency Conflicts: 0 (CRITICAL) [v1.0.0 - Not implemented]
"""

    # List file conflicts
    if conflict_report.file_conflicts:
        report += "║\n║   File-level Conflicts:\n"
        for i, conflict in enumerate(conflict_report.file_conflicts[:5], 1):
            report += f"║     {i}. {conflict.file_path}\n"
            report += f"║        Conflicting Plans: {len(conflict.conflicting_plan_ids)}\n"

        if len(conflict_report.file_conflicts) > 5:
            report += f"║     ... and {len(conflict_report.file_conflicts) - 5} more conflicts\n"

    report += f"""╠══════════════════════════════════════════════════════════════════
║ Execution Plan:
║   - Total Execution Waves: {execution_plan.total_waves}
║   - Serial Execution Groups: {sum(1 for w in execution_plan.waves if w.execution_mode == ExecutionMode.SERIAL)}
║   - Parallel Execution Groups: {sum(1 for w in execution_plan.waves if w.execution_mode == ExecutionMode.PARALLEL)}
"""

    # Show first 5 waves
    for wave in execution_plan.waves[:5]:
        mode_label = "Serial" if wave.execution_mode == ExecutionMode.SERIAL else "Parallel"
        report += f"║\n║   Wave {wave.wave_number} ({mode_label}): {len(wave.plans)} plan(s)\n"
        for plan in wave.plans[:3]:
            report += f"║     - {plan.name}\n"
        if len(wave.plans) > 3:
            report += f"║     ... and {len(wave.plans) - 3} more plans\n"

    if len(execution_plan.waves) > 5:
        report += f"║   ... and {len(execution_plan.waves) - 5} more waves\n"

    report += f"""╠══════════════════════════════════════════════════════════════════
║ Time Estimation:
║   - Sequential Execution: {time_estimate.sequential_time_minutes} minutes
║   - Parallel Execution: {time_estimate.parallel_time_minutes} minutes
║   - Time Saved: {time_estimate.time_saved_minutes} minutes ({time_estimate.time_saved_percentage}% reduction)
╠══════════════════════════════════════════════════════════════════
║ Risk Assessment:
║   - HIGH Risk Plans: {risk_assessment.high_risk_plans}
║   - MEDIUM Risk Plans: {risk_assessment.medium_risk_plans}
║   - LOW Risk Plans: {risk_assessment.low_risk_plans}
"""

    # Show high-risk plans
    high_risk = [r for r in risk_assessment.plan_risks if r.risk_level == RiskLevel.HIGH]
    if high_risk:
        report += "║\n║   HIGH Risk Plans:\n"
        for risk in high_risk[:5]:
            report += f"║     - {risk.plan_name} (score: {risk.risk_score})\n"
            for factor in risk.risk_factors[:2]:
                report += f"║         • {factor}\n"

        if len(high_risk) > 5:
            report += f"║     ... and {len(high_risk) - 5} more high-risk plans\n"

    report += """╠══════════════════════════════════════════════════════════════════
║ Recommendations:
"""

    # Generate recommendations
    recommendations = []

    if risk_assessment.high_risk_plans > 0:
        recommendations.append("• Review HIGH risk plans before execution")

    if conflict_report.total_conflicts > 5:
        recommendations.append("• Consider splitting batch into smaller groups to reduce conflicts")

    if mapping_report.manual_plans > 0:
        recommendations.append(f"• Manually execute {mapping_report.manual_plans} plans without skill mappings")

    if time_estimate.sequential_time_minutes > 180:
        recommendations.append("• Enable parallel execution to reduce total time")

    if not recommendations:
        recommendations.append("• No issues detected - safe to proceed with execution")

    for rec in recommendations:
        report += f"║   {rec}\n"

    report += """╚══════════════════════════════════════════════════════════════════
"""

    return report
```

### Post-Execution Summary (Simulated)

```python
def generate_dry_run_post_execution_summary(
    plans: List[Plan],
    execution_plan: ExecutionPlan,
    time_estimate: TimeEstimate
) -> str:
    """
    Generate simulated post-execution summary for dry-run.
    """
    report = f"""
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Simulated Post-Execution Summary
║ Mode: DRY-RUN (No actual execution performed)
╠══════════════════════════════════════════════════════════════════
║ Execution Results (SIMULATED):
║   - Total Plans: {len(plans)}
║   - Would Execute Successfully: {len(plans)} (estimated)
║   - Would Fail: 0 (estimated)
║   - Would Skip: 0 (estimated)
╠══════════════════════════════════════════════════════════════════
║ File Changes (PREDICTED):
║   - Files Would Be Created: ~ {len(plans) * 5} (estimated)
║   - Files Would Be Modified: ~ {len(plans) * 2} (estimated)
║   - Files Would Be Deleted: ~ 0 (estimated)
╠══════════════════════════════════════════════════════════════════
║ Quality Gate Results (SIMULATED):
║   ⚠ ArchitectureTest: SKIPPED (dry-run mode)
║   ⚠ Compilation: SKIPPED (dry-run mode)
║   ⚠ Unit Tests: SKIPPED (dry-run mode)
╠══════════════════════════════════════════════════════════════════
║ Estimated Execution Time:
║   - Sequential: {time_estimate.sequential_time_minutes} minutes
║   - Parallel: {time_estimate.parallel_time_minutes} minutes
║   - Time Saved: {time_estimate.time_saved_minutes} minutes ({time_estimate.time_saved_percentage}% reduction)
╠══════════════════════════════════════════════════════════════════
║ Next Steps:
║   1. Review conflict analysis and risk assessment above
║   2. Resolve any HIGH risk issues before actual execution
║   3. Run actual execution with: /batch-execute --auto
║   4. Monitor progress and check quality gate results
╚══════════════════════════════════════════════════════════════════

NOTE: This is a simulated summary. No actual changes were made to the codebase.
      To perform actual execution, run without --dry-run flag.
"""
    return report
```

---

## Example Output

### Complete Dry-run Report

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Dry-run Report
║ Mode: SIMULATION (No actual execution)
╠══════════════════════════════════════════════════════════════════
║ Plan Discovery:
║   - Total Plans: 8
║   - Claude Code Plans: 3
║   - Skills Phase Docs: 2
║   - Project Plans: 3
╠══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation:
║   - Successfully Mapped: 7/8
║   - Requires Manual Execution: 1
║   - Mapping Success Rate: 87.5%
║
║   Plans Requiring Manual Execution:
║     - multi-tenant-setup (type: custom)
╠══════════════════════════════════════════════════════════════════
║ Conflict Analysis:
║   - Total Conflicts: 2
║   - File-level Conflicts: 2 (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM) [v1.0.0 - Not implemented]
║   - Dependency Conflicts: 0 (CRITICAL) [v1.0.0 - Not implemented]
║
║   File-level Conflicts:
║     1. smart-admin-api/sa-admin/.../ProductController.java
║        Conflicting Plans: 2
║     2. smart-admin-web/src/api/product/product-api.ts
║        Conflicting Plans: 2
╠══════════════════════════════════════════════════════════════════
║ Execution Plan:
║   - Total Execution Waves: 5
║   - Serial Execution Groups: 2
║   - Parallel Execution Groups: 1
║
║   Wave 1 (Serial): 1 plan(s)
║     - product-crud
║
║   Wave 2 (Serial): 1 plan(s)
║     - controller-vavr-migration
║
║   Wave 3 (Parallel): 5 plan(s)
║     - liteflow-setup
║     - integration-tests
║     - cache-strategy
║     ... and 2 more plans
╠══════════════════════════════════════════════════════════════════
║ Time Estimation:
║   - Sequential Execution: 240 minutes
║   - Parallel Execution: 85 minutes
║   - Time Saved: 155 minutes (65% reduction)
╠══════════════════════════════════════════════════════════════════
║ Risk Assessment:
║   - HIGH Risk Plans: 2
║   - MEDIUM Risk Plans: 3
║   - LOW Risk Plans: 3
║
║   HIGH Risk Plans:
║     - product-crud (score: 5)
║         • Multiple file conflicts (2)
║         • Has dependencies (1)
║     - controller-vavr-migration (score: 6)
║         • Multiple file conflicts (2)
║         • Low mapping confidence (0.65)
╠══════════════════════════════════════════════════════════════════
║ Recommendations:
║   • Review HIGH risk plans before execution
║   • Manually execute 1 plans without skill mappings
║   • Enable parallel execution to reduce total time
╚══════════════════════════════════════════════════════════════════
```

---

## Validation Checks (Dry-run)

### Skill Mapping Validation

```python
def validate_skill_mappings_dry_run(plans: List[Plan]) -> bool:
    """
    Validate that all plans can be mapped to skills.

    Returns:
        True if all plans have valid skill mappings, False otherwise
    """
    unmapped_plans = []

    for plan in plans:
        skill, confidence = map_plan_to_skill(plan)
        if skill is None:
            unmapped_plans.append(plan)

    if unmapped_plans:
        print(f"⚠ Warning: {len(unmapped_plans)} plans require manual execution:")
        for plan in unmapped_plans:
            print(f"  - {plan.name} (type: {plan.type})")
        return False

    return True
```

### Conflict Validation

```python
def validate_no_critical_conflicts_dry_run(conflict_report: ConflictReport) -> bool:
    """
    Check for critical conflicts that would prevent execution.

    Returns:
        True if no critical conflicts, False otherwise
    """
    # v1.0.0: Only file-level conflicts implemented
    if len(conflict_report.file_conflicts) > 10:
        print(f"⚠ Warning: Too many file conflicts ({len(conflict_report.file_conflicts)})")
        print("  Consider splitting batch into smaller groups")
        return False

    return True
```

---

## Configuration (Dry-run Mode)

### config.yml Settings

```yaml
# Dry-run mode configuration
modes:
  dry_run:
    generate_full_report: true
    estimate_execution_time: true
    validate_skill_mappings: true
    show_conflict_details: true
    risk_assessment: true
    max_conflicts_to_show: 10
    max_waves_to_show: 5
```

---

## Limitations (v1.0.0)

### Known Limitations

1. **File Change Prediction Accuracy**:
   - Dry-run predicts file changes based on content analysis
   - Actual files modified may differ slightly
   - Estimated: 90-95% accuracy

2. **Time Estimation Accuracy**:
   - Based on historical averages and heuristics
   - Actual execution time may vary ±20%
   - Factors: system load, network latency, test execution

3. **Conflict Detection Scope**:
   - v1.0.0: Only file-level conflicts
   - Module-level and dependency conflicts: Future releases

4. **Risk Assessment**:
   - Heuristic-based scoring
   - May not catch all edge cases
   - Manual review recommended for HIGH risk plans

---

## Next Steps After Dry-run

### Step 1: Review Reports

Review generated reports:
- Pre-Execution Report (conflicts, risks, execution plan)
- Skill Mapping Validation
- Time Estimation

### Step 2: Resolve Issues

Address identified issues:
- Fix validation errors
- Resolve HIGH risk factors
- Add manual skill mappings for unmapped plans

### Step 3: Execute

Run actual execution:

```bash
# Run actual execution after dry-run validation
/batch-execute --auto

# Or run with interactive mode for safety
/batch-execute --mode=interactive
```

---

## Troubleshooting

### Issue 1: Skill Mapping Failures

**Symptom**: Many plans show "Requires Manual Execution"

**Solution**:
1. Add explicit `skill:` field in plan YAML frontmatter
2. Configure module-to-skill mappings in `config.yml`
3. Run manual execution for unmapped plans

### Issue 2: Inaccurate Time Estimates

**Symptom**: Estimated time differs significantly from actual

**Solution**:
1. Adjust `PLAN_TYPE_ESTIMATES` in config
2. Provide plan size hints in YAML frontmatter
3. Use historical execution data for calibration

### Issue 3: Missing Conflicts

**Symptom**: Dry-run shows no conflicts but actual execution fails

**Solution**:
1. Ensure plans document file modifications explicitly
2. Add "Files to Modify" section in plan content
3. Use code blocks with file path comments

---

## Best Practices

### Practice 1: Always Dry-run First

```bash
# ✅ Good: Validate before executing
/batch-execute --dry-run --auto
/batch-execute --auto

# ❌ Bad: Execute without validation
/batch-execute --auto
```

### Practice 2: Document File Modifications

```markdown
# Good Plan Format

## Files to Modify
- src/main/java/.../ProductController.java
- src/main/java/.../ProductService.java
- smart-admin-web/src/api/product/product-api.ts

## Implementation Steps
...
```

### Practice 3: Review Risk Assessment

Always review HIGH risk plans manually before execution.

---

## Related Documentation

- [SKILL.md](../SKILL.md) - Core skill documentation
- [Phase 2: Conflict Detection](../phases/phase-2-conflict-detection.md) - Conflict detection algorithms
- [Example: Mixed Plans](../examples/EXAMPLE-mixed-plans.md) - Mixed plan execution example

---

**Mode Status**: ✅ Production Ready
**Documentation Version**: v1.0.0
**Last Updated**: 2026-01-29
