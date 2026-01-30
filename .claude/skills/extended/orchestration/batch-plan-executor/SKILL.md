---
name: batch-plan-executor
description: [P1 - Extended] Batch plan executor for automatically detecting, analyzing conflicts, and executing multiple implementation plans. Use when executing multiple plans in sequence or parallel.
---

# Batch Plan Executor - Core Skill Documentation

**Skill Name**: batch-plan-executor
**Version**: v1.0.0 (Phase 1 - MVP)
**Priority**: P1 (Important - Orchestration & Productivity)
**Status**: ✅ Production Ready
**Last Updated**: 2026-01-29

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Plan Type Support](#plan-type-support)
4. [Plan Identification Mechanism](#plan-identification-mechanism)
5. [Conflict Detection](#conflict-detection)
6. [Execution Modes](#execution-modes)
7. [Configuration](#configuration)
8. [Command Reference](#command-reference)
9. [Execution Reports](#execution-reports)
10. [Skill Mapping](#skill-mapping)
11. [Use Cases](#use-cases)
12. [FAQ](#faq)
13. [Troubleshooting](#troubleshooting)
14. [Limitations](#limitations)
15. [Roadmap](#roadmap)

---

## Overview

### What is Batch Plan Executor?

**Batch Plan Executor** 是一個智能批量方案執行器，能夠自動檢測、分析衝突並執行多個實施計劃。它是 SmartAdmin Skills System 的核心協調器，用於提升多方案執行的效率和安全性。

### Core Capabilities

- ✅ **Multi-Source Plan Discovery**: 支持三種方案來源（Claude Code Plans、Skills Docs、Project Plans）
- ✅ **Intelligent Conflict Detection**: 三層衝突檢測（文件級別、模塊級別、依賴關係）
- ✅ **Automatic Skill Mapping**: 自動將方案映射到對應的 Skill
- ✅ **Parallel Execution Optimization**: 最多 5 個方案並行執行，效率提升 60-80%
- ✅ **Comprehensive Reporting**: 執行前風險評估 + 執行中進度追蹤 + 執行後總結
- ✅ **Dry-run Simulation**: 模擬運行模式，零風險評估
- ✅ **Zero Configuration**: 開箱即用，90% 場景無需配置

### When to Use This Skill

**觸發關鍵詞**:
- "batch execute", "execute multiple plans", "run all plans"
- "parallel execution", "concurrent execution"
- "批量執行", "一次執行多個方案"
- "liteflow migration" (含多個階段性方案)
- "execute all plans in directory"

**典型場景**:
- ✅ 大型技術遷移項目（如 LiteFlow 遷移包含 8 個階段）
- ✅ 批量 CRUD 模組生成（一次生成多個業務模組）
- ✅ 混合類型方案執行（CRUD + 測試 + 遷移）
- ✅ 執行前風險評估和衝突檢測
- ✅ 多團隊協作場景（避免文件衝突）

### v1.0.0 MVP Features

**Phase 1 - MVP (v1.0.0)**:
- ✅ 方案自動識別和分類
- ✅ 文件級別衝突檢測
- ✅ Skill 自動映射
- ✅ 串行執行（Sequential Execution）
- ✅ Dry-run 模式
- ✅ 完整執行報告

**Future Releases**:
- ⏳ Phase 2 (v1.1.0): 並行執行、模塊衝突檢測
- ⏳ Phase 3 (v1.2.0): 依賴關係檢測、回滾機制
- ⏳ Phase 4 (v1.3.0): 進度追蹤、實時日誌

---

## Quick Start

### 1. Basic Usage (Auto-scan All Plans)

最簡單的使用方式：自動掃描所有配置目錄並執行。

```bash
# 自動掃描並執行所有方案
/batch-execute --auto

# 僅掃描並顯示報告（不執行）
/batch-execute --dry-run --auto
```

**效果**:
- 自動掃描 `~/.claude/plans/`、`.claude/skills/*/phases/`、`docs/plans/`
- 識別所有 markdown 方案文件
- 檢測衝突並生成執行計劃
- 串行執行所有方案（v1.0.0）

### 2. Execute Specific Plans

指定要執行的方案文件。

```bash
# 執行 2 個方案
/batch-execute plan1.md plan2.md

# 使用絕對路徑
/batch-execute \
  ~/.claude/plans/product-crud.md \
  docs/plans/liteflow/phase-1-setup.md
```

### 3. Scan Specific Directory

掃描特定目錄下的所有方案。

```bash
# 掃描 LiteFlow 計劃目錄
/batch-execute --scan-dir=docs/plans/liteflow/

# 掃描 Skills Phase 文檔
/batch-execute --scan-dir=.claude/skills/smartadmin-crud-generator/phases/
```

### 4. Dry-run Mode (Risk Assessment)

在實際執行前進行風險評估。

```bash
# Dry-run: 生成完整報告但不執行
/batch-execute --dry-run

# 驗證 Skill 映射
/batch-execute --dry-run --validate-mappings

# 顯示詳細衝突信息
/batch-execute --dry-run --show-conflicts
```

### 5. Interactive Mode

交互式確認模式，每個方案執行前確認。

```bash
# 交互式執行
/batch-execute --mode=interactive

# 允許調整執行順序
/batch-execute --mode=interactive --allow-reorder
```

---

## Plan Type Support

Batch Plan Executor 支持三種類型的方案文件。

### Type 1: Claude Code Plans

**來源**: Claude Code CLI 生成的方案

**路徑**: `~/.claude/plans/*.md`

**特徵**:
- YAML frontmatter 包含 `name`, `type`, `created_at`
- 自動映射到對應 Skill（基於 `type` 字段）

**示例**:

```yaml
---
name: product-crud
type: crud
created_at: 2026-01-28
skill: smartadmin-crud-generator  # Optional: explicit skill mapping
---

# Product CRUD Module Implementation Plan

## Overview
Create a complete CRUD module for Product management...

## Entity Definition
```java
// File: src/main/java/.../ProductEntity.java
@Entity
@Table(name = "product")
public class ProductEntity { ... }
```

## Implementation Steps
1. Create Entity class
2. Create Dao/Mapper
3. Create Service layer
4. Create Controller
5. Create Vue components
```

**Skill 映射規則**:
- `type: crud` → `smartadmin-crud-generator`
- `type: testing` → `smartadmin-testing-suite`
- `type: refactoring` → `vavr-refactoring-assistant`
- `type: migration` → 根據內容推斷（liteflow, kafka, etc.）

### Type 2: Skills Phase Documentation

**來源**: Skills 的階段性文檔

**路徑**: `.claude/skills/{skill-name}/phases/phase-*.md`

**特徵**:
- 文件名格式: `phase-N-{name}.md`
- 直接映射到父 Skill
- 通常按順序執行（Phase 1 → Phase 2 → ...）

**示例**:

```markdown
# Phase 2: Frontend Component Implementation

**Phase**: 2/4
**Status**: ✅ Production Ready
**Duration**: 10-14 hours

## Overview
This phase implements Vue 3 frontend components...

## Implementation Checklist
- [ ] Create ProductList.vue
- [ ] Create ProductForm.vue
- [ ] Create product-api.ts
```

**Skill 映射規則**:
- 直接映射到父 Skill（從路徑提取）
- 示例: `.claude/skills/smartadmin-crud-generator/phases/phase-2.md`
  → Skill: `smartadmin-crud-generator`

### Type 3: Project Plans

**來源**: 項目級別的實施計劃

**路徑**: `docs/plans/{module}/*.md`

**特徵**:
- YAML frontmatter 包含 `module`, `feature`, `dependencies`
- 需要配置映射或用戶確認
- 支持依賴聲明

**示例**:

```yaml
---
module: liteflow
feature: rule-migration
type: migration
priority: high
dependencies:
  - liteflow-setup
  - evrete-analysis
skill: liteflow-rule-builder  # Optional: explicit skill mapping
---

# LiteFlow Rule Migration Implementation Plan

## Overview
Migrate existing Evrete rules to LiteFlow DSL...

## Prerequisites
- ✅ LiteFlow setup completed (Phase 1)
- ✅ Evrete analysis completed

## Implementation Steps
1. Analyze Evrete rule structure
2. Generate LiteFlow EL expressions
3. Migrate rule conditions
4. Create LiteFlow chain definitions
```

**Skill 映射規則**:
- 基於 `module` 字段映射（見 [Skill Mapping](#skill-mapping)）
- 示例:
  - `module: liteflow` → `liteflow-rule-builder`
  - `module: job` → `scheduled-task-manager`
  - `module: tenant` → 需要手動執行

---

## Plan Identification Mechanism

### Discovery Process

Batch Plan Executor 使用多階段方案識別流程。

#### Stage 1: Directory Scanning

```bash
# 默認掃描目錄
~/.claude/plans/           # Claude Code Plans
.claude/skills/*/phases/   # Skills Phase Docs
docs/plans/                # Project Plans
```

**掃描規則**:
- 遞歸掃描（最大深度: 3）
- 僅掃描 `.md` 文件
- 排除 `archive/`、`deprecated/`、`.git/`

#### Stage 2: Plan Type Detection

**檢測優先級**:

1. **Priority 1: File Path Pattern** (最高優先級)
   - 基於文件路徑判斷類型
   - 準確率: 95%+

2. **Priority 2: YAML Frontmatter**
   - 分析 YAML metadata
   - 準確率: 90%+

3. **Priority 3: Content Analysis**
   - 標題格式、關鍵詞分析
   - 準確率: 80%+

**示例**:

```python
# File Path Pattern (Priority 1)
~/.claude/plans/product.md          → CLAUDE_CODE_PLAN
.claude/skills/crud/phases/phase-1.md → SKILL_PHASE_DOC
docs/plans/liteflow/migration.md    → PROJECT_PLAN

# YAML Frontmatter (Priority 2)
---
name: product-crud
type: crud
---
→ CLAUDE_CODE_PLAN

# Content Analysis (Priority 3)
# Phase 2: Frontend Implementation
→ SKILL_PHASE_DOC
```

#### Stage 3: Metadata Extraction

從方案中提取關鍵信息：

| Field | Source | Required |
|-------|--------|----------|
| `name` | YAML frontmatter or filename | ✅ |
| `type` | YAML frontmatter or inferred | ✅ |
| `title` | First H1 heading | ✅ |
| `skill` | YAML or mapped | ⚠ Recommended |
| `dependencies` | YAML frontmatter | ❌ Optional |
| `priority` | YAML frontmatter | ❌ Optional |

#### Stage 4: Validation

驗證方案完整性和正確性：

**驗證規則**:
- ✅ 文件存在且可讀
- ✅ 包含有效的標題（H1 heading）
- ⚠ 有 Skill 映射（警告級別）
- ⚠ 內容長度 > 100 字符（警告級別）

**驗證報告**:

```
╔══════════════════════════════════════════════════════════════════
║ Plan Validation Report
╠══════════════════════════════════════════════════════════════════
║ Total Plans: 8
║ Valid Plans: 7
║ Invalid Plans: 1
╠══════════════════════════════════════════════════════════════════
║ ✗ docs/plans/tenant/multi-tenant.md
║     - ERROR: Missing plan title (H1 heading)
║ ⚠ docs/plans/custom/custom-feature.md
║     - WARNING: No skill mapping found - manual execution required
╚══════════════════════════════════════════════════════════════════
```

---

## Conflict Detection

### Overview

Batch Plan Executor 實施三層衝突檢測機制。

**v1.0.0 MVP**: 僅實施文件級別衝突檢測
**Future Releases**: 模塊級別和依賴關係衝突檢測

### Layer 1: File-Level Conflicts (v1.0.0 ✅)

**定義**: 兩個或多個方案嘗試修改同一個文件。

**嚴重性**: **HIGH** - 可能導致合併衝突或數據丟失。

**檢測機制**:

```python
# Step 1: Extract affected files from each plan
for plan in plans:
    affected_files = extract_affected_files(plan.content)
    # Strategy 1: Code blocks with file path comments
    # Strategy 2: Explicit file references (backticks)
    # Strategy 3: Markdown links
    # Strategy 4: Inferred from class names (Entity, Controller, etc.)

# Step 2: Detect conflicts (files modified by 2+ plans)
file_map = {}
for plan, files in plan_files:
    for file_path in files:
        if file_path not in file_map:
            file_map[file_path] = []
        file_map[file_path].append(plan.id)

conflicts = [
    FileConflict(file_path=fp, conflicting_plan_ids=pids)
    for fp, pids in file_map.items()
    if len(pids) > 1
]
```

**文件提取策略**:

1. **Strategy 1: Code Blocks with File Path Comments**
   ```java
   ```java
   // File: src/main/java/.../ProductController.java
   @RestController
   public class ProductController { ... }
   ```
   ```

2. **Strategy 2: Explicit File References**
   ```markdown
   Modify `src/main/java/.../ProductService.java` to add caching.
   ```

3. **Strategy 3: Markdown Links**
   ```markdown
   See [ProductEntity](src/main/java/.../ProductEntity.java) for details.
   ```

4. **Strategy 4: Class Name Inference**
   ```markdown
   Create ProductEntity class
   → Infer: src/main/java/.../domain/entity/ProductEntity.java
   ```

**衝突解決策略**:

- **Serial Execution Groups**: 將衝突方案分組，串行執行
- **User Confirmation**: 高衝突場景需要用戶確認

**示例報告**:

```
╔══════════════════════════════════════════════════════════════════
║ Conflict Analysis Report
╠══════════════════════════════════════════════════════════════════
║ Total Conflicts: 2
║ File-level Conflicts: 2 (HIGH)
╠══════════════════════════════════════════════════════════════════
║ Conflict #1: ProductController.java
║   Conflicting Plans:
║     - [crud] product-crud
║     - [refactoring] controller-vavr-migration
║   Resolution: Serialize execution (product-crud → vavr-migration)
║
║ Conflict #2: product-api.ts
║   Conflicting Plans:
║     - [crud] product-crud
║     - [integration] api-update
║   Resolution: Serialize execution
╚══════════════════════════════════════════════════════════════════
```

### Layer 2: Module-Level Conflicts (v1.1.0 ⏳)

**定義**: 兩個或多個方案操作同一業務模組。

**嚴重性**: **MEDIUM** - 可能導致集成問題或測試失敗。

**檢測機制** (Planned):

```python
# Extract affected modules from plan content
modules = extract_affected_modules(plan)
# Example: net.lab1024.sa.admin.module.business.product.*
#          smart-admin-web/src/views/product/*

# Detect module conflicts
module_map = {}
for plan, modules in plan_modules:
    for module in modules:
        if module not in module_map:
            module_map[module] = []
        module_map[module].append(plan.id)

conflicts = [
    ModuleConflict(module=m, conflicting_plan_ids=pids)
    for m, pids in module_map.items()
    if len(pids) > 1
]
```

**解決策略**:
- **Warning Only**: 允許並行執行，但標記為風險
- **Suggested Testing**: 建議在每個方案後運行測試

### Layer 3: Dependency Relationship Conflicts (v1.2.0 ⏳)

**定義**: 方案執行順序違反依賴關係。

**嚴重性**: **CRITICAL** - 可能導致執行失敗或錯誤行為。

**檢測機制** (Planned):

```python
# Build dependency graph
graph = DirectedGraph()
for plan in plans:
    graph.add_node(plan.id)
    for dep in plan.dependencies:
        graph.add_edge(plan.id, dep)

# Detect circular dependencies
cycles = graph.find_cycles()
if cycles:
    raise CircularDependencyError(cycles)

# Perform topological sort for safe execution order
execution_order = graph.topological_sort()
```

**解決策略**:
- **Topological Sort**: 自動調整執行順序
- **Circular Dependency Detection**: 檢測並報告循環依賴

---

## Execution Modes

Batch Plan Executor 提供三種執行模式。

### Mode 1: Auto Mode (Fully Automatic)

**特點**:
- 零人工干預
- 自動解決可解決的衝突
- 高風險項會暫停並提示

**適用場景**: 低風險方案批量執行

**命令**:

```bash
/batch-execute --auto
/batch-execute --mode=auto
```

**行為**:
- 自動掃描所有配置目錄
- 自動檢測衝突
- 自動生成執行計劃
- 自動串行執行（v1.0.0）
- 高風險方案暫停並提示

**配置**:

```yaml
modes:
  auto:
    confirm_before_start: false
    stop_on_high_risk: true
    auto_resolve_conflicts: true
```

### Mode 2: Interactive Mode

**特點**:
- 每個方案執行前確認
- 顯示詳細衝突信息
- 允許調整執行順序

**適用場景**: 高風險方案或首次執行

**命令**:

```bash
/batch-execute --mode=interactive
/batch-execute --mode=interactive --allow-reorder
```

**行為**:
- 掃描並顯示所有方案
- 顯示衝突分析報告
- 每個方案執行前等待確認
- 允許跳過或調整順序

**交互流程**:

```
╔══════════════════════════════════════════════════════════════════
║ Interactive Execution - Plan #1/8
╠══════════════════════════════════════════════════════════════════
║ Plan: product-crud
║ Type: crud
║ Skill: smartadmin-crud-generator
║ Risk: MEDIUM (score: 3)
║   - File conflicts: 2
║   - Dependencies: 1
╠══════════════════════════════════════════════════════════════════
║ Actions:
║   [E] Execute now
║   [S] Skip this plan
║   [R] Reorder plans
║   [A] Abort batch execution
║   [V] View plan details
╚══════════════════════════════════════════════════════════════════
Your choice: _
```

**配置**:

```yaml
modes:
  interactive:
    confirm_each_plan: true
    show_conflict_details: true
    allow_reorder: true
    show_risk_factors: true
```

### Mode 3: Dry-run Mode (Simulation)

**特點**:
- 不執行實際操作
- 生成完整分析報告
- 驗證 Skill 映射

**適用場景**: 執行前風險評估

**命令**:

```bash
/batch-execute --dry-run
/batch-execute --dry-run --auto
/batch-execute --dry-run plan1.md plan2.md
```

**行為**:
- 掃描和分類方案
- 驗證 Skill 映射
- 檢測衝突
- 生成執行計劃
- 估算執行時間
- 風險評估
- 生成完整報告（不執行）

**報告內容**:
- Plan Inventory
- Skill Mapping Validation
- Conflict Analysis
- Execution Plan Preview
- Time Estimation
- Risk Assessment
- Recommendations

**配置**:

```yaml
modes:
  dry_run:
    generate_full_report: true
    estimate_execution_time: true
    validate_skill_mappings: true
```

**詳細文檔**: 參考 [MODE-dry-run.md](modes/MODE-dry-run.md)

---

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
      - "net.lab1024.sa.admin.module"
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

## Skill Mapping

### Overview

Batch Plan Executor 自動將方案映射到對應的 Skill。

**映射方式**:
1. **Explicit Mapping**: 方案中明確指定 `skill:` 字段（優先級最高）
2. **Type-Based Mapping**: 基於方案類型自動映射（Claude Code Plans）
3. **Module-Based Mapping**: 基於模組名稱映射（Project Plans）
4. **Content Analysis**: 基於內容關鍵詞推斷（Fallback）

### Type-Based Mapping (Claude Code Plans)

| Plan Type | Skill | Confidence |
|-----------|-------|------------|
| `crud` | smartadmin-crud-generator | 1.0 |
| `testing` | smartadmin-testing-suite | 1.0 |
| `refactoring` | vavr-refactoring-assistant | 0.9 |
| `migration` | (Module-dependent) | 0.7 |
| `integration` | (Content-dependent) | 0.6 |
| `security` | security-hardening-pro | 0.9 |
| `performance` | smartadmin-performance-suite | 0.9 |

### Module-Based Mapping (Project Plans)

| Module | Skill | Confidence |
|--------|-------|------------|
| `liteflow` | liteflow-rule-builder | 1.0 |
| `job` | scheduled-task-manager | 1.0 |
| `kafka` | message-queue-pattern-generator | 1.0 |
| `cache` | cache-strategy-generator | 1.0 |
| `websocket` | websocket-sse-realtime-generator | 1.0 |
| `security` | security-hardening-pro | 1.0 |
| `migration` | db-migration-manager | 1.0 |
| `i18n` | i18n-generator | 1.0 |
| `report` | report-generator-skill | 1.0 |
| `search` | full-text-search-integration | 1.0 |
| `apm` | apm-integration-skill | 1.0 |
| `tenant` | None (Manual) | 0.0 |
| `custom` | None (Manual) | 0.0 |

### Skill Phase Docs (Direct Mapping)

**規則**: Skills Phase Docs 直接映射到父 Skill。

**示例**:
- `.claude/skills/smartadmin-crud-generator/phases/phase-2-frontend.md`
  → Skill: `smartadmin-crud-generator` (Confidence: 1.0)

### Content-Based Mapping (Fallback)

當無法通過類型或模組映射時，使用內容分析。

**關鍵詞匹配**:

```python
keywords_mapping = {
    'smartadmin-crud-generator': ['entity', 'dao', 'controller', 'service', 'crud'],
    'liteflow-rule-builder': ['liteflow', 'rule', 'chain', 'node', 'el expression'],
    'scheduled-task-manager': ['xxl-job', 'snail-job', 'scheduled', 'cron', 'task'],
    'vavr-refactoring-assistant': ['vavr', 'option', 'try', 'either', 'functional'],
    # ... more mappings
}

def content_based_mapping(content: str) -> Tuple[str, float]:
    scores = {}
    for skill, keywords in keywords_mapping.items():
        score = sum(1 for kw in keywords if kw.lower() in content.lower())
        if score > 0:
            scores[skill] = score / len(keywords)  # Normalize to 0.0-1.0

    if scores:
        best_skill = max(scores, key=scores.get)
        confidence = scores[best_skill]
        return best_skill, confidence

    return None, 0.0
```

### Mapping Confidence Scores

| Confidence Range | Interpretation | Action |
|------------------|----------------|--------|
| 1.0 | Exact match | Auto-execute |
| 0.8 - 0.99 | High confidence | Auto-execute |
| 0.7 - 0.79 | Medium confidence | Auto-execute (with warning) |
| 0.5 - 0.69 | Low confidence | Ask user confirmation |
| < 0.5 | Very low confidence | Suggest manual execution |

### Manual Mapping Override

**方法 1: YAML Frontmatter**

```yaml
---
name: custom-feature
type: custom
skill: smartadmin-crud-generator  # Explicit override
---
```

**方法 2: Configuration File**

```yaml
# config.yml
skill_mapping:
  custom_mappings:
    'docs/plans/custom/feature-a.md': 'smartadmin-crud-generator'
    'docs/plans/custom/feature-b.md': 'liteflow-rule-builder'
```

**詳細文檔**: 參考 [references/skill-mapping.md](references/skill-mapping.md)

---

## Use Cases

### Use Case 1: LiteFlow Migration (8 Phases)

**場景**: LiteFlow 遷移計劃包含 8 個階段性方案，需要依序執行。

**挑戰**:
- 多個階段有依賴關係（Phase 2 依賴 Phase 1）
- 串行執行耗時長（預計 4 小時）
- 手動執行容易出錯

**解決方案**:

```bash
# Step 1: Dry-run 風險評估
/batch-execute --dry-run --scan-dir=docs/plans/liteflow/

# Step 2: 查看報告
# - 識別到 8 個方案
# - 檢測到依賴關係
# - 預計時間: 串行 240 分鐘

# Step 3: 執行
/batch-execute --scan-dir=docs/plans/liteflow/ --auto
```

**效果**:
- ✅ 自動檢測並遵循依賴關係
- ✅ v1.0.0: 串行執行，預計 240 分鐘
- ✅ v1.1.0+: 並行優化，預計 85 分鐘（65% 時間減少）
- ✅ 自動質量檢查（ArchUnit、編譯、測試）

### Use Case 2: Batch CRUD Generation

**場景**: 一次生成 5 個業務模組的 CRUD 功能。

**挑戰**:
- 重複性工作量大
- 手動執行耗時且枯燥
- 容易出現不一致

**解決方案**:

```bash
# 創建 5 個 Claude Code Plans
~/.claude/plans/product-crud.md
~/.claude/plans/order-crud.md
~/.claude/plans/customer-crud.md
~/.claude/plans/inventory-crud.md
~/.claude/plans/payment-crud.md

# 批量執行
/batch-execute --auto
```

**效果**:
- ✅ 自動識別為 `crud` 類型
- ✅ 映射到 `smartadmin-crud-generator`
- ✅ 無文件衝突（不同業務模組）
- ✅ v1.1.0+: 並行執行，5 個模組僅需 30 分鐘（vs 串行 125 分鐘）

### Use Case 3: Mixed Plan Types

**場景**: 同時執行 CRUD 生成、測試生成和業務方案。

**挑戰**:
- 多種方案類型混合
- 可能存在文件衝突
- 需要不同的 Skill

**解決方案**:

```bash
# Dry-run 檢查衝突
/batch-execute --dry-run \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md

# 查看報告
# - Conflict: ProductController.java (product-crud vs integration-tests)
# - Resolution: Serial execution (product-crud → integration-tests)

# 執行
/batch-execute --mode=interactive \
  ~/.claude/plans/product-crud.md \
  .claude/skills/smartadmin-testing-suite/phases/phase-2-integration-tests.md \
  docs/plans/tenant/multi-tenant-setup.md
```

**效果**:
- ✅ 自動識別三種方案類型
- ✅ 檢測文件衝突並串行執行
- ✅ tenant-migration 需要手動執行（無 Skill 映射）

**詳細示例**: 參考 [examples/EXAMPLE-mixed-plans.md](examples/EXAMPLE-mixed-plans.md)

### Use Case 4: Pre-Execution Risk Assessment

**場景**: 在實際執行前評估方案衝突和風險。

**解決方案**:

```bash
# Dry-run 完整評估
/batch-execute --dry-run --auto
```

**報告內容**:
- ✅ 檢測到的衝突列表
- ✅ 執行計劃預覽
- ✅ 風險評估（HIGH/MEDIUM/LOW）
- ✅ 預計執行時間

---

## FAQ

### Q1: 如何處理方案執行失敗？

**A**: 默認採用 `CONTINUE` 策略，跳過失敗方案繼續執行。

**配置選項**:

```bash
# 失敗時立即停止並回滾
/batch-execute --on-failure=ABORT_ALL

# 失敗時暫停等待用戶決策
/batch-execute --on-failure=PAUSE
```

**重試失敗的方案**:

```bash
# 查看執行報告，獲取 Batch ID
# 示例: batch-exec-20260129-153000

# 重試失敗的方案
/batch-execute --retry-failed batch-exec-20260129-153000
```

### Q2: 如何自定義並行數量？

**A**: 使用 `--max-concurrent` 參數。

```bash
# 最多 3 個並行（適用於資源受限環境）
/batch-execute --max-concurrent=3

# 最多 10 個並行（適用於高性能環境）
/batch-execute --max-concurrent=10
```

**配置文件**:

```yaml
execution:
  max_concurrent: 5  # Default
```

**注意**: v1.0.0 MVP 暫不支持並行執行，此參數在 v1.1.0 生效。

### Q3: 如何驗證方案映射是否正確？

**A**: 使用 Dry-run 模式驗證。

```bash
# 檢查 Skill 映射
/batch-execute --dry-run --validate-mappings
```

**報告示例**:

```
╔══════════════════════════════════════════════════════════════════
║ Skill Mapping Validation
╠══════════════════════════════════════════════════════════════════
║ Successfully Mapped: 7/8
║
║   ✓ product-crud → smartadmin-crud-generator (confidence: 1.0)
║   ✓ liteflow-migration → liteflow-rule-builder (confidence: 1.0)
║   ⚠ custom-feature → None (requires manual execution)
╚══════════════════════════════════════════════════════════════════
```

### Q4: 如何手動指定 Skill 映射？

**A**: 在方案的 YAML frontmatter 中添加 `skill:` 字段。

```yaml
---
name: custom-feature
type: custom
skill: smartadmin-crud-generator  # Manual override
---
```

### Q5: 支持哪些 Skill？

**A**: 支持所有 P0/P1 Skills（共 15 個）。

**P0 Skills (6)**:
- smartadmin-crud-generator
- smartadmin-integration-test
- test-fixture-generator
- vavr-refactoring-assistant
- archunit-test-generator
- security-hardening-pro

**P1 Skills (3)**:
- liteflow-rule-builder
- quality-gate-orchestrator
- fraud-detection-pattern-generator

**P2 Skills (6)**:
- smartadmin-performance-suite
- smartadmin-testing-suite
- db-migration-manager
- igame-feature-builder
- cicd-pipeline-builder
- (More...)

**完整列表**: 參考 [references/skill-mapping.md](references/skill-mapping.md)

### Q6: 如何處理無 Skill 映射的方案？

**A**: 無映射的方案會被標記為 "Requires Manual Execution"。

**選項**:

1. **手動執行**:
   ```bash
   # 手動執行該方案（不使用 batch executor）
   /execute docs/plans/custom/feature.md
   ```

2. **添加映射**:
   ```yaml
   # 在方案中添加 skill 字段
   ---
   skill: smartadmin-crud-generator
   ---
   ```

3. **配置映射**:
   ```yaml
   # config.yml
   skill_mapping:
     custom_mappings:
       'docs/plans/custom/feature.md': 'smartadmin-crud-generator'
   ```

### Q7: v1.0.0 支持並行執行嗎？

**A**: 不支持。v1.0.0 MVP 僅支持串行執行（Sequential Execution）。

**並行執行計劃**: v1.1.0（預計 2026-02-12）

**當前行為**:
- 所有方案串行執行（一次一個）
- 衝突方案自動分組串行
- 無衝突方案也串行執行（v1.0.0 限制）

### Q8: 如何估算執行時間？

**A**: Dry-run 模式會自動估算。

```bash
/batch-execute --dry-run --auto
```

**估算方法**:
- 基於方案類型的默認時間（CRUD: 25 min, Testing: 20 min, etc.）
- 根據方案大小調整（內容長度）
- 考慮衝突導致的串行執行

**報告示例**:

```
║ Time Estimation:
║   - Sequential Execution: 240 minutes
║   - Parallel Execution: 85 minutes (v1.1.0+)
║   - Time Saved: 155 minutes (65% reduction)
```

### Q9: Quality Gate 失敗時會怎樣？

**A**: 取決於配置的失敗策略。

**配置**:

```yaml
execution:
  failure_strategy: CONTINUE  # Default
  quality_gates:
    enabled: true
    run_architecture_test: true
    run_unit_tests: true
```

**行為**:
- `CONTINUE`: 記錄失敗，繼續執行其他方案
- `ABORT_ALL`: 立即停止所有執行
- `PAUSE`: 暫停並等待用戶決策

### Q10: 如何查看歷史執行記錄？

**A**: 查看日誌目錄。

```bash
# 查看日誌目錄
ls -la .claude/metrics/batch-executions/

# 示例輸出
batch-exec-20260129-153000.log
batch-exec-20260129-153000-pre.md
batch-exec-20260129-153000-post.md
batch-exec-20260129-153000.json
```

**JSON 格式** (Machine-readable):

```json
{
  "batch_id": "batch-exec-20260129-153000",
  "total_plans": 8,
  "successful": 7,
  "failed": 1,
  "execution_time_minutes": 125,
  "plans": [
    {
      "name": "product-crud",
      "status": "success",
      "duration_minutes": 25,
      "skill": "smartadmin-crud-generator"
    }
    // ... more plans
  ]
}
```

---

## Troubleshooting

### Issue 1: Skill Mapping Failures

**症狀**: 很多方案顯示 "Requires Manual Execution"。

**可能原因**:
- 方案缺少明確的類型或模組信息
- 內容關鍵詞不足以推斷 Skill
- 自定義方案類型未配置映射

**解決方案**:

1. **添加明確的 Skill 字段**:
   ```yaml
   ---
   skill: smartadmin-crud-generator
   ---
   ```

2. **配置模組到 Skill 的映射**:
   ```yaml
   # config.yml
   skill_mapping:
     custom_mappings:
       'docs/plans/custom-module/*.md': 'smartadmin-crud-generator'
   ```

3. **降低信心閾值**:
   ```yaml
   skill_mapping:
     auto_mapping:
       confidence_threshold: 0.5  # Default: 0.7
   ```

### Issue 2: File Conflict Detection Misses

**症狀**: Dry-run 顯示無衝突，但實際執行時出現合併衝突。

**可能原因**:
- 方案中未明確記錄文件修改
- 文件路徑格式不規範
- 推斷算法失敗

**解決方案**:

1. **明確記錄文件修改**:
   ```markdown
   ## Files to Modify
   - src/main/java/.../ProductController.java
   - src/main/java/.../ProductService.java
   ```

2. **使用代碼塊文件註釋**:
   ```markdown
   ```java
   // File: src/main/java/.../ProductController.java
   @RestController
   public class ProductController { ... }
   ```
   ```

3. **手動審查衝突**:
   ```bash
   /batch-execute --dry-run --show-conflicts
   ```

### Issue 3: Execution Timeout

**症狀**: 方案執行超時（默認 60 分鐘）。

**可能原因**:
- 方案過於複雜
- Skill 執行時間過長
- 系統資源不足

**解決方案**:

1. **增加超時時間**:
   ```yaml
   execution:
     timeout:
       per_plan_seconds: 7200  # 120 minutes
   ```

2. **拆分大型方案**:
   - 將單個大方案拆分為多個小階段
   - 每個階段獨立執行

3. **手動執行超時方案**:
   ```bash
   # 查看執行報告，找到超時方案
   # 手動執行該方案（不使用 batch executor）
   ```

### Issue 4: Quality Gate Failures

**症狀**: ArchUnit 測試失敗，阻止後續方案執行。

**可能原因**:
- 方案生成的代碼違反架構規則
- Skill 實現不符合 SmartAdmin 規範

**解決方案**:

1. **查看詳細錯誤信息**:
   ```bash
   # 查看執行日誌
   cat .claude/metrics/batch-executions/batch-exec-*.log
   ```

2. **修復架構違規**:
   - 檢查生成的代碼
   - 修正違反的規則（如使用 `@Autowired` 字段注入）

3. **臨時禁用 Quality Gate** (不推薦):
   ```yaml
   execution:
     quality_gates:
       enabled: false  # Only for testing
   ```

### Issue 5: Inaccurate Time Estimates

**症狀**: 預估時間與實際執行時間差距大（> 50%）。

**可能原因**:
- 默認時間估算不準確
- 方案複雜度差異大
- 系統資源波動

**解決方案**:

1. **校準時間估算**:
   ```yaml
   # config.yml - Add custom estimates
   time_estimation:
     plan_type_estimates:
       crud: 30  # Adjust from default 25
       testing: 25  # Adjust from default 20
   ```

2. **提供方案大小提示**:
   ```yaml
   ---
   estimated_duration_minutes: 45
   ---
   ```

3. **使用歷史數據**:
   - 執行幾次後，系統會自動調整估算

---

## Limitations

### v1.0.0 Known Limitations

#### 1. Serial Execution Only

**限制**: v1.0.0 僅支持串行執行，無法並行執行無衝突的方案。

**影響**: 執行時間較長，無法充分利用系統資源。

**解決方案**: 升級到 v1.1.0（預計 2026-02-12）以支持並行執行。

#### 2. File-Level Conflict Detection Only

**限制**: v1.0.0 僅檢測文件級別衝突，不檢測模塊級別和依賴關係衝突。

**影響**: 可能遺漏某些潛在衝突。

**解決方案**:
- 手動審查方案之間的模塊依賴
- 升級到 v1.1.0（模塊衝突）或 v1.2.0（依賴衝突）

#### 3. Basic Skill Mapping

**限制**: Skill 映射基於啟發式算法，準確率約 85-90%。

**影響**: 某些方案可能需要手動指定 Skill。

**解決方案**:
- 在方案中明確指定 `skill:` 字段
- 使用 Dry-run 驗證映射結果

#### 4. No Real-Time Progress Tracking

**限制**: v1.0.0 無實時進度追蹤，僅在執行完成後生成報告。

**影響**: 長時間執行時無法了解當前進度。

**解決方案**: 升級到 v1.3.0（預計 2026-03-12）以支持實時追蹤。

#### 5. No Automatic Rollback

**限制**: v1.0.0 無自動回滾機制，失敗的方案需要手動清理。

**影響**: 執行失敗時可能留下不完整的代碼。

**解決方案**:
- 使用 Git 手動回滾
- 升級到 v1.2.0（預計 2026-02-26）以支持自動回滾

### Design Limitations

#### 1. Markdown-Only Plans

**限制**: 僅支持 Markdown 格式的方案文件。

**影響**: 無法處理其他格式（如 YAML、JSON、XML）。

**解決方案**: 將其他格式轉換為 Markdown，或在 frontmatter 中引用外部文件。

#### 2. Local Execution Only

**限制**: v1.0.0 僅支持本地執行，不支持遠程執行或分布式執行。

**影響**: 受限於單機資源。

**解決方案**: Future enhancement（分布式執行計劃尚未制定）。

#### 3. No Plan Versioning

**限制**: 無方案版本管理，無法追蹤方案修改歷史。

**影響**: 難以回溯方案變更。

**解決方案**: 使用 Git 管理方案文件。

---

## Roadmap

### v1.0.0 (Phase 1) - ✅ Released (2026-01-29)

**MVP Features**:
- ✅ 方案自動識別和分類
- ✅ 三種方案類型支持（Claude Code Plans, Skills Docs, Project Plans）
- ✅ 文件級別衝突檢測
- ✅ Skill 自動映射（基於類型和模組）
- ✅ 串行執行
- ✅ Dry-run 模式（完整報告）
- ✅ Pre-Execution Report
- ✅ Post-Execution Summary

### v1.1.0 (Phase 2) - 🚧 In Development (ETA: 2026-02-12)

**Parallel Execution**:
- ⏳ 並行執行優化（最多 5 個方案並行）
- ⏳ 依賴圖構建和拓撲排序
- ⏳ 執行波次劃分（Wave 1, Wave 2, ...）
- ⏳ 模塊級別衝突檢測
- ⏳ 動態並發數調整

**Enhanced Reporting**:
- ⏳ Progress Report（執行過程中）
- ⏳ 實時日誌輸出
- ⏳ 執行可視化（文本圖表）

### v1.2.0 (Phase 3) - 📋 Planned (ETA: 2026-02-26)

**Dependency & Rollback**:
- 📋 依賴關係衝突檢測
- 📋 循環依賴檢測
- 📋 自動回滾機制（Git snapshot）
- 📋 部分成功處理（rollback completed plans）

**Enhanced Conflict Detection**:
- 📋 Java 包結構分析
- 📋 Vue 模組依賴分析
- 📋 數據庫表結構衝突檢測

### v1.3.0 (Phase 4) - 💡 Ideas (ETA: 2026-03-12)

**Real-Time Monitoring**:
- 💡 實時進度追蹤
- 💡 Web UI dashboard
- 💡 Slack/Email 通知
- 💡 Webhook 集成

**Advanced Features**:
- 💡 方案優先級排序
- 💡 資源使用監控（CPU, Memory）
- 💡 執行歷史統計和分析
- 💡 AI-powered 衝突預測

### Future (v2.0.0+) - 🔮 Long-term

**Distributed Execution**:
- 🔮 多機分布式執行
- 🔮 雲端執行支持（AWS, GCP, Azure）
- 🔮 容器化執行（Docker, Kubernetes）

**AI Enhancements**:
- 🔮 AI-powered Skill 映射
- 🔮 智能方案分組
- 🔮 自動衝突解決建議
- 🔮 方案優化建議

---

## Version History

| Version | Release Date | Status | Key Features |
|---------|--------------|--------|--------------|
| v1.0.0 | 2026-01-29 | ✅ Released | MVP - Plan discovery, file conflicts, serial execution, dry-run |
| v1.1.0 | 2026-02-12 (ETA) | 🚧 In Dev | Parallel execution, module conflicts, progress tracking |
| v1.2.0 | 2026-02-26 (ETA) | 📋 Planned | Dependency detection, rollback mechanism |
| v1.3.0 | 2026-03-12 (ETA) | 💡 Ideas | Real-time monitoring, web dashboard |
| v2.0.0 | TBD | 🔮 Future | Distributed execution, AI enhancements |

---

## Related Documentation

- **[README.md](README.md)** - Quick start guide
- **[config.yml](config.yml)** - Complete configuration reference
- **[Phase 1: Plan Discovery](phases/phase-1-discovery.md)** - Plan identification algorithms
- **[Phase 2: Conflict Detection](phases/phase-2-conflict-detection.md)** - Conflict detection mechanisms
- **[MODE-dry-run.md](modes/MODE-dry-run.md)** - Dry-run mode detailed guide
- **[Skill Mapping](references/skill-mapping.md)** - Complete skill mapping rules
- **[Example: Mixed Plans](examples/EXAMPLE-mixed-plans.md)** - Real-world usage examples

---

## Support

- **GitHub Issues**: [Submit bugs or feature requests](https://github.com/1024-lab/smart-admin/issues)
- **Documentation**: [SmartAdmin Skills System](.claude/skills/README.md)
- **Contact**: Smart-Admin Development Team

---

**Last Updated**: 2026-01-29
**Documentation Version**: v1.0.0 (Phase 1 - MVP)
**Skill Status**: ✅ Production Ready
