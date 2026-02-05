## Configuration

### Configuration File

配置文件位置: `.claude/skills/batch-plan-executor/config.yml`

### Key Configuration Sections

#### 1. Execution Settings

```yaml
execution:
  # Maximum number of concurrent plan executions (v1.1.0+)
  max_concurrent: 5

  # Failure strategy when a plan execution fails
  # Options: CONTINUE | ABORT_ALL | PAUSE
  failure_strategy: CONTINUE

  # Retry configuration
  retry:
    enabled: true
    max_attempts: 3
    backoff: exponential  # exponential | linear | fixed
    initial_delay_seconds: 1

  # Timeout settings
  timeout:
    per_plan_seconds: 3600   # 60 minutes per plan
    total_seconds: 14400     # 4 hours total

  # Quality gate checks after execution
  quality_gates:
    enabled: true
    run_architecture_test: true
    run_compilation_check: true
    run_unit_tests: true
```

#### 2. Conflict Detection Settings

```yaml
conflict_detection:
  # File-level conflict detection
  file_level:
    enabled: true
    severity: HIGH
    ignore_patterns:
      - "**/*Test.java"
      - "**/target/**"
      - "**/build/**"

  # Module-level conflict detection (v1.1.0+)
  module_level:
    enabled: true
    severity: MEDIUM
    java_package_prefixes:
      - "net.lab1024.sa"
    vue_module_paths:
      - "smart-admin-web/src/views"

  # Dependency conflict detection (v1.2.0+)
  dependency_level:
    enabled: true
    severity: CRITICAL
    auto_resolve_cycles: false
```

#### 3. Reporting Settings

```yaml
reporting:
  # Pre-execution report
  pre_execution:
    enabled: true
    include_conflict_analysis: true
    include_execution_plan: true
    include_risk_assessment: true
    include_time_estimation: true

  # Progress updates during execution
  progress_updates:
    enabled: true
    interval_seconds: 60
    show_real_time_logs: true

  # Post-execution summary
  post_execution:
    enabled: true
    include_execution_stats: true
    include_file_changes: true
    include_quality_gate_results: true

  # Log storage
  logs:
    save_to_file: true
    log_directory: ".claude/metrics/batch-executions"
    log_format: "batch-exec-{timestamp}.log"
```

#### 4. Plan Discovery Settings

```yaml
plan_discovery:
  # Directories to scan
  directories:
    claude_code_plans: "~/.claude/plans/"
    skill_phase_docs: ".claude/skills/*/phases/"
    project_plans: "docs/plans/"

  # File patterns
  file_patterns:
    claude_code_plans: "*.md"
    skill_phase_docs: "phase-*.md"
    project_plans: "*.md"

  # Scan depth
  max_depth: 3

  # Exclude patterns
  exclude_patterns:
    - "**/archive/**"
    - "**/deprecated/**"
    - "**/.git/**"
```

#### 5. Skill Mapping Settings

```yaml
skill_mapping:
  # Auto-mapping
  auto_mapping:
    enabled: true
    confidence_threshold: 0.7  # 0.0-1.0

  # Fallback behavior
  fallback:
    ask_user: true
    suggest_manual: true

  # Skill aliases
  aliases:
    crud: smartadmin-crud-generator
    test: smartadmin-testing-suite
    refactor: vavr-refactoring-assistant
    liteflow: liteflow-rule-builder
    job: scheduled-task-manager
```

#### 6. Execution Modes

```yaml
modes:
  # Default mode
  default: auto

  # Auto mode settings
  auto:
    confirm_before_start: false
    stop_on_high_risk: true

  # Interactive mode settings
  interactive:
    confirm_each_plan: true
    show_conflict_details: true
    allow_reorder: true

  # Dry-run mode settings
  dry_run:
    generate_full_report: true
    estimate_execution_time: true
    validate_skill_mappings: true
```

### Environment-Specific Configuration

```bash
# Development environment
export BATCH_EXECUTOR_ENV=development
export BATCH_EXECUTOR_MAX_CONCURRENT=3

# Production environment
export BATCH_EXECUTOR_ENV=production
export BATCH_EXECUTOR_MAX_CONCURRENT=5
export BATCH_EXECUTOR_FAILURE_STRATEGY=ABORT_ALL
```

---

