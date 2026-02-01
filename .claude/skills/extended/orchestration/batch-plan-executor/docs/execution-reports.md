## Execution Reports

Batch Plan Executor 生成三種類型的報告。

### 1. Pre-Execution Report

**生成時機**: 執行開始前

**內容**:
- Plan Inventory (方案清單)
- Skill Mapping Validation (Skill 映射驗證)
- Conflict Analysis (衝突分析)
- Execution Plan (執行計劃)
- Time Estimation (時間估算)
- Risk Assessment (風險評估)

**示例**:

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Pre-Execution Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: 8
║ Plan Types:
║   - Claude Code Plans: 3
║   - Skills Phase Docs: 2
║   - Project Plans: 3
╠══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation:
║   - Successfully Mapped: 7/8
║   - Requires Manual Execution: 1
╠══════════════════════════════════════════════════════════════════
║ Conflict Analysis:
║   - File-level Conflicts: 2 (HIGH)
║   - Module-level Conflicts: 0 (MEDIUM)
║   - Dependency Conflicts: 0 (CRITICAL)
╠══════════════════════════════════════════════════════════════════
║ Execution Plan:
║   - Serial Groups: 2
║   - Parallel Groups: 1 (v1.1.0+)
║   - Total Execution Waves: 3
╠══════════════════════════════════════════════════════════════════
║ Time Estimation:
║   - Sequential Execution: 240 minutes
║   - Parallel Execution: 85 minutes (v1.1.0+)
║   - Time Saved: 155 minutes (65% reduction)
╠══════════════════════════════════════════════════════════════════
║ Risk Assessment:
║   - HIGH Risk Plans: 2
║   - MEDIUM Risk Plans: 3
║   - LOW Risk Plans: 3
╚══════════════════════════════════════════════════════════════════
```

### 2. Progress Report (v1.3.0 ⏳)

**生成時機**: 執行過程中（每分鐘）

**內容**:
- Current Status (當前狀態)
- Completed Plans (已完成方案)
- Running Plans (運行中方案)
- Pending Plans (待執行方案)
- Elapsed Time (已用時間)
- Estimated Remaining Time (預計剩餘時間)

**示例** (Future Release):

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Progress Report
╠══════════════════════════════════════════════════════════════════
║ Current Status: Wave 2/5 (Serial Execution)
╠══════════════════════════════════════════════════════════════════
║ Completed Plans: 3/8 (37.5%)
║   ✓ product-crud (25 min)
║   ✓ liteflow-setup (30 min)
║   ✓ integration-tests (18 min)
╠══════════════════════════════════════════════════════════════════
║ Running Plans: 1/5 slots
║   ⚙ controller-vavr-migration (12/30 min)
╠══════════════════════════════════════════════════════════════════
║ Pending Plans: 4
║   ○ cache-strategy (Waiting: Wave 3)
║   ○ tenant-migration (Waiting: Wave 4)
║   ○ security-hardening (Waiting: Wave 5)
║   ○ api-docs (Waiting: Wave 5)
╠══════════════════════════════════════════════════════════════════
║ Elapsed Time: 73 minutes
║ Estimated Remaining: 52 minutes
╚══════════════════════════════════════════════════════════════════
```

### 3. Post-Execution Summary

**生成時機**: 執行完成後

**內容**:
- Execution Results (執行結果統計)
- File Changes (文件變更統計)
- Quality Gate Results (質量門檢查結果)
- Total Execution Time (總執行時間)
- Recommendations (建議)

**示例**:

```
╔══════════════════════════════════════════════════════════════════
║ Batch Plan Executor - Post-Execution Summary
╠══════════════════════════════════════════════════════════════════
║ Execution Results:
║   - Total Plans: 8
║   - Successful: 7 (87.5%)
║   - Failed: 1 (12.5%)
║   - Skipped: 0
╠══════════════════════════════════════════════════════════════════
║ File Changes:
║   - Files Created: 42
║   - Files Modified: 18
║   - Files Deleted: 3
╠══════════════════════════════════════════════════════════════════
║ Quality Gate Results:
║   ✓ ArchitectureTest: PASSED
║   ✓ Compilation: PASSED
║   ✗ Unit Tests: FAILED (2 tests failed)
╠══════════════════════════════════════════════════════════════════
║ Failed Plans:
║   ✗ security-hardening.md
║     - Error: Skill execution timeout (60 min limit exceeded)
║     - Recommendation: Increase per_plan_timeout or split into phases
╠══════════════════════════════════════════════════════════════════
║ Total Execution Time: 125 minutes (v1.0.0 - Serial)
║   - Planned: 240 minutes (sequential)
║   - Actual: 125 minutes
║   - Efficiency: 52% (due to failures and skips)
╠══════════════════════════════════════════════════════════════════
║ Recommendations:
║   - Fix failing unit tests in ProductServiceTest
║   - Manually retry security-hardening.md with extended timeout
║   - Consider adding integration tests for tenant module
╚══════════════════════════════════════════════════════════════════
```

### Report Storage

```yaml
reporting:
  logs:
    save_to_file: true
    log_directory: ".claude/metrics/batch-executions"
    log_format: "batch-exec-{timestamp}.log"
```

**生成的文件**:
```
.claude/metrics/batch-executions/
├── batch-exec-20260129-153000.log      # Full execution log
├── batch-exec-20260129-153000-pre.md   # Pre-execution report
├── batch-exec-20260129-153000-post.md  # Post-execution summary
└── batch-exec-20260129-153000.json     # Machine-readable data
```

---

