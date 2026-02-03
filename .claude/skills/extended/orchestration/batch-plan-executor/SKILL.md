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

---

## Version

**Skill Version**: 1.0.0
**Last Updated**: 2026-01-31
**Compatible With**: SmartAdmin v4.0.0+, .claude/ system v3.0.2+

---

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

## Skill Dependencies

此編排 skill 協調多個 foundation/productivity skills：

**Foundation Skills**:
- `smartadmin-crud-generator` - CRUD 生成任務
- `archunit-test-generator` - 測試生成任務

**Productivity Skills**:
- `cache-strategy-generator` - 快取實現任務
- `db-migration-manager` - 資料庫遷移任務
- 其他 skills 根據任務類型動態調用

**依賴處理**:
- Skills 根據 plan task 類型動態調用
- 依賴 skill 失敗會觸發整個 batch 回滾
- 完整映射見 `docs/skill-mapping.md`

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

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "batch execute" - Batch execution of multiple plans
- "batch-execute" - Direct command invocation
- "execute multiple plans" - Multi-plan execution request
- "plan orchestration" - Orchestrate multiple implementation plans
- "conflict detection" - Detect conflicts between plans

**Secondary Keywords** (Medium confidence):
- "dry-run" - Context: risk assessment before execution
- "execute plans" - Context: batch execution context
- "scan plans" - Context: auto-discovery of plan files
- "interactive mode" - Context: interactive plan execution
- "skill mapping" - Context: validate skill invocations in plans

**Phrase Patterns**:
- "Execute [number] plans together" - Example: "Execute 5 plans together with conflict detection"
- "Batch execute plans in [directory]" - Example: "Batch execute plans in docs/plans/liteflow/"
- "Run dry-run for [plans]" - Example: "Run dry-run for all LiteFlow migration plans"

**Example User Requests**:
```
User: "Execute all plans in ~/.claude/plans/ directory"
User: "Batch execute plan1.md and plan2.md with conflict detection"
User: "Run dry-run to assess conflicts between Employee and Department CRUD plans"
User: "Execute all LiteFlow migration phases interactively"
User: "Scan and execute all skill phase documents"
```

**Note**: This skill can also be manually invoked via `/batch-execute` command. Supports execution modes: `--auto`, `--dry-run`, `--mode=interactive`, `--mode=sequential`, `--mode=parallel`.

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


---

## 📚 詳細文檔

以下章節已提取至單獨文檔以提高可讀性：

### 核心功能

1. **[Plan Identification Mechanism](docs/plan-identification.md)** - 方案識別機制
   - 4 階段識別流程：Directory Scanning → Type Detection → Metadata Extraction → Validation
   - 支持 3 種計劃類型：Claude Code Plans, Skills Phase Docs, Project Plans
   
2. **[Conflict Detection](docs/conflict-detection.md)** - 衝突檢測
   - File-level 檢測（HIGH 嚴重性）
   - Module-level 檢測（MEDIUM 嚴重性）
   - Skill-level 檢測（LOW 嚴重性）
   
3. **[Execution Modes](docs/execution-modes.md)** - 執行模式
   - Sequential Mode（串行執行，fail-fast）
   - Parallel Mode（並行執行，未來支持）
   - Interactive Mode（交互式確認）
   - Dry-run Mode（風險評估）

### 配置和命令

4. **[Configuration](docs/configuration.md)** - 配置說明
   - Execution settings（max_concurrent, failure_strategy, retry）
   - Conflict detection settings
   - Notification settings
   - Logging settings

5. **[Command Reference](docs/command-reference.md)** - 命令參考
   - 完整命令語法和參數說明
   - 常用命令示例

### 執行和監控

6. **[Execution Reports](docs/execution-reports.md)** - 執行報告
   - Summary Report（執行摘要）
   - Detailed Report（詳細報告）
   - Conflict Report（衝突報告）
   - Performance Metrics（性能指標）

7. **[Skill Mapping](docs/skill-mapping.md)** - Skill 映射
   - 自動 Skill 映射規則
   - Type → Skill 映射表
   - 手動 Skill 指定

### 使用指南

8. **[Use Cases](docs/use-cases.md)** - 使用案例
   - LiteFlow 遷移（8 階段）
   - 批量 CRUD 生成
   - 混合類型執行

9. **[FAQ](docs/faq.md)** - 常見問題
   - 執行相關問題
   - 衝突檢測問題
   - Skill 映射問題

10. **[Troubleshooting](docs/troubleshooting.md)** - 故障排除
    - 常見錯誤和解決方案
    - 調試技巧

### 參考信息

11. **[Limitations](docs/limitations.md)** - 限制
    - 當前版本限制
    - 不支持的場景

12. **[Roadmap](docs/roadmap.md)** - 開發路線圖
    - v1.1.0 計劃功能
    - v2.0.0 長期規劃

---

## 相關規則與協調技能

本技能作為批量計劃執行協調器，整合以下 SmartAdmin 技能與規範：

### 協調的核心技能（Foundation）

- **[smartadmin-crud-generator](./../../foundation/full-stack/smartadmin-crud-generator/SKILL.md)** - 批量 CRUD 生成（最常見使用場景）
- **[archunit-test-generator](./../../foundation/backend/archunit-test-generator/SKILL.md)** - 架構測試生成（驗證階段）
- **[test-fixture-generator](./../../foundation/testing/test-fixture-generator/SKILL.md)** - 測試數據生成（測試階段）
- **[smartadmin-integration-test](./../../foundation/full-stack/smartadmin-integration-test/SKILL.md)** - 整合測試（驗證階段）

### 協調的擴展技能（Extended）

- **[igame-feature-builder](./../../extended/domain/igame-feature-builder/SKILL.md)** - iGaming 功能批量生成
- **[liteflow-rule-builder](./../../extended/domain/liteflow-rule-builder/SKILL.md)** - LiteFlow 規則鏈批量生成
- **[quality-gate-orchestrator](./../../extended/orchestration/quality-gate-orchestrator/SKILL.md)** - 質量門檢查（執行後驗證）

### 架構規範遵循

- **[Architecture Rules](./../../../.agent/rules/foundation/10-architecture-rules.md)** - 確保生成的代碼符合分層架構
- **[Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md)** - 確保批量生成的類名一致

### 協調模式

- **衝突檢測**: 自動檢測批量計劃間的資源衝突（文件、數據庫表）
- **依賴排序**: 根據技能依賴關係自動排序執行順序
- **並行執行**: 無衝突的計劃並行執行（提升效率）
- **失敗回滾**: 單個計劃失敗不影響其他計劃（隔離性）

**技能定位**: 本技能是**協調器技能**（Orchestrator），不生成代碼，僅協調其他技能執行。

---

**Version**: 2.0.0 (Restructured)
**Last Updated**: 2026-02-01
**Documentation Structure**: Main + Detailed Docs

