---
name: naming-convention-checker
description: SmartAdmin Naming Convention Checker - Validates code compliance with SmartAdmin's singular naming standards for database tables (@TableName annotations), class naming (XXXController, XXXService, XXXManager, XXXDao), and field naming (deleted not isDeleted). Use when creating Entity classes, during code review, or when ArchitectureTest tableNameMustBeSingular fails.
---

# Naming Convention Checker - Detailed Skill Documentation

**Version**: 1.0.0
**Priority**: P1 (Extended/Quality)
**Type**: Atomic
**Status**: ✅ Stable

---

## Table of Contents

1. [Overview](#overview)
2. [Skill Architecture](#skill-architecture)
3. [Execution Phases](#execution-phases)
4. [Detection Patterns](#detection-patterns)
5. [Exemption Rules](#exemption-rules)
6. [Report Generation](#report-generation)
7. [Integration Points](#integration-points)
8. [Usage Examples](#usage-examples)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

Enforces SmartAdmin's singular naming standard for database tables by scanning Entity classes and validating `@TableName` annotations.

### Background

SmartAdmin v4.0+ adopts a strict singular naming convention for consistency:
- **Table Names**: `t_player`, `t_wallet`, `t_transaction` (NOT `t_players`, `t_wallets`)
- **Entity Classes**: `PlayerEntity`, `WalletEntity` (NOT `PlayersEntity`)
- **API Paths**: `/api/v1/players`, `/api/v1/wallets` (RESTful plural - exception)

This skill prevents accidental introduction of plural table names through automated scanning.

### Key Benefits

✅ **Early Detection**: Catches violations before code review
✅ **Automated Enforcement**: Runs in CI/CD pipeline
✅ **Consistent Standards**: Aligns with ArchUnit tests
✅ **Exemption Support**: Allows intentional exceptions (metrics, statistics)

---

## Skill Architecture

### Component Diagram

```
naming-convention-checker/
├── config.yml                      # Skill configuration
├── SKILL.md                        # This document
├── README.md                       # Quick reference
├── patterns/
│   └── table-name-patterns.json   # Plural patterns to detect
└── references/
    └── naming-exceptions.md        # Exemption list documentation
```

### Detection Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. Scan Codebase                                        │
│    - Find all Java files in entity packages             │
│    - Use Glob pattern: **/*Entity.java                  │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│ 2. Parse @TableName Annotations                         │
│    - Use JavaParser or Grep                             │
│    - Extract table name values                          │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│ 3. Apply Detection Patterns                             │
│    - Check against plural patterns                      │
│    - Validate exemption list                            │
└─────────────────┬───────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────┐
│ 4. Generate Report                                       │
│    - List violations with file paths                    │
│    - Suggest singular form alternatives                 │
│    - Output: docs/quality/NAMING-VIOLATIONS-REPORT.md   │
└─────────────────────────────────────────────────────────┘
```

---

## Execution Phases

### Phase 1: Discovery (30 seconds)

**Goal**: Find all Entity classes with `@TableName` annotations

**Actions**:
```bash
# Glob search for Entity files
Glob("**/*Entity.java")

# Grep for @TableName annotations
Grep("@TableName", output_mode="files_with_matches", glob="**/*Entity.java")
```

**Output**: List of Entity file paths

### Phase 2: Validation (60 seconds)

**Goal**: Check each `@TableName` value against plural patterns

**Actions**:
- Read Entity class content
- Extract `@TableName("xxx")` value using regex: `@TableName\\("([^"]+)"\\)`
- Match against plural patterns (see [Detection Patterns](#detection-patterns))
- Check exemption list

**Output**: List of violations with metadata

### Phase 3: Reporting (30 seconds)

**Goal**: Generate comprehensive violation report

**Actions**:
- Format violations into Markdown table
- Include file paths, line numbers, current name, suggested name
- Add summary statistics
- Write to `docs/quality/NAMING-VIOLATIONS-REPORT.md`

**Output**: Markdown report file

---

## Detection Patterns

### Plural Pattern Configuration

File: `patterns/table-name-patterns.json`

```json
{
  "plural_patterns": [
    {
      "pattern": "_goods$",
      "description": "Common irregular plural",
      "singular_rule": "remove_s",
      "example": {
        "plural": "t_goods",
        "singular": "t_good"
      }
    },
    {
      "pattern": "_users$",
      "description": "Regular plural (s)",
      "singular_rule": "remove_s",
      "example": {
        "plural": "t_users",
        "singular": "t_user"
      }
    },
    {
      "pattern": "_bonuses$",
      "description": "Regular plural (es)",
      "singular_rule": "remove_es",
      "example": {
        "plural": "t_bonuses",
        "singular": "t_bonus"
      }
    },
    {
      "pattern": "_activities$",
      "description": "Irregular plural (ies)",
      "singular_rule": "replace_ies_with_y",
      "example": {
        "plural": "t_activities",
        "singular": "t_activity"
      }
    }
  ],
  "exemptions": [
    {
      "pattern": "_metrics$",
      "reason": "metrics is uncountable noun (aggregated data)"
    },
    {
      "pattern": "_statistics$",
      "reason": "statistics is conventionally plural"
    },
    {
      "pattern": "_analytics$",
      "reason": "analytics is conventionally plural"
    }
  ]
}
```

### Detection Algorithm

```java
boolean isPlural(String tableName, String[] patterns, String[] exemptions) {
    // 1. Check exemptions first
    for (String exemption : exemptions) {
        if (tableName.matches(".*" + exemption)) {
            return false;  // Allowed exception
        }
    }

    // 2. Check plural patterns
    for (String pattern : patterns) {
        if (tableName.matches(".*" + pattern)) {
            return true;  // Violation detected
        }
    }

    return false;  // No violation
}
```

---

## Exemption Rules

### Exemption List

File: `references/naming-exceptions.md`

| Table Pattern | Reason | Example |
|--------------|--------|---------|
| `t_*_metrics` | Uncountable noun (aggregated statistical data) | `t_liteflow_execution_metrics` |
| `t_*_statistics` | Conventionally used in plural form | `t_user_statistics`, `t_game_statistics` |
| `t_*_analytics` | Conventionally used in plural form | `t_player_analytics`, `t_behavior_analytics` |

### Adding New Exemptions

**Process**:
1. Update `patterns/table-name-patterns.json` (add to `exemptions` array)
2. Update `references/naming-exceptions.md` (document reason)
3. Update `.agent/rules/foundation/01-naming-conventions.md` (Section 7)
4. Update `ArchitectureTest.java` (add to exemption check)

**Example**:
```java
// In ArchitectureTest.java
if (tableName.endsWith("_metrics") ||
    tableName.endsWith("_statistics") ||
    tableName.endsWith("_analytics") ||
    tableName.endsWith("_new_exemption")) {  // ✅ Add here
    return;
}
```

---

## Report Generation

### Report Template

```markdown
# Naming Convention Violation Report

**Generated**: 2026-02-02 14:35:22
**Scanned Entities**: 42
**Violations Found**: 2
**Exemptions Applied**: 1

---

## Violations Summary

| # | Entity Class | Current Table Name | Suggested Name | File Path | Line |
|---|--------------|-------------------|----------------|-----------|------|
| 1 | `GoodsEntity` | `t_goods` | `t_good` | `smartadmin-modules/smartadmin-business/.../GoodsEntity.java` | 17 |
| 2 | `PlayerEntity` | `t_players` | `t_player` | `smartadmin-modules/smartadmin-business/.../PlayerEntity.java` | 23 |

---

## Detailed Violations

### 1. GoodsEntity

**File**: `smart-admin-api-java21-springboot3/smartadmin-modules/smartadmin-business/src/main/java/net/lab1024/sa/business/goods/domain/entity/GoodsEntity.java:17`

**Current Code**:
```java
@TableName("t_goods")
public class GoodsEntity {
    // ...
}
```

**Recommended Fix**:
```java
@TableName("t_good")
public class GoodsEntity {
    // ...
}
```

**Migration Required**:
- Database migration script needed
- Update all SQL queries in Mapper XML files
- Run ArchUnit tests to verify

---

## Exemptions Applied

| Entity Class | Table Name | Exemption Reason |
|--------------|------------|------------------|
| `LiteFlowExecutionMetricsEntity` | `t_liteflow_execution_metrics` | metrics is uncountable noun |

---

## Next Steps

1. Review violations listed above
2. Create database migration scripts if needed
3. Update Entity @TableName annotations
4. Update Mapper XML files
5. Run ArchUnit tests: `./gradlew :smartadmin-app:test --tests ArchitectureTest#tableNameMustBeSingular`

---

**Generated by**: naming-convention-checker skill v1.0.0
**Rule Source**: .agent/rules/foundation/01-naming-conventions.md (Section 7)
```

---

## Integration Points

### 1. ArchUnit Integration

技能檢查邏輯與 ArchUnit 測試保持一致：

```java
// .agent/configs/ArchitectureTest.java
@ArchTest
static final ArchRule tableNameMustBeSingular = classes()
    .that().areAnnotatedWith(TableName.class)
    .should(new ArchCondition<JavaClass>("use singular table names") {
        // 同步檢查邏輯
    });
```

### 2. CI/CD Integration

`.github/workflows/naming-check.yml`:
```yaml
name: Naming Convention Check
on:
  pull_request:
    paths:
      - '**/entity/*.java'
jobs:
  naming-check:
    runs-on: ubuntu-latest
    steps:
      - name: Run Naming Convention Checker
        run: |
          claude-code skills run naming-convention-checker
      - name: Run ArchUnit Tests
        run: |
          ./gradlew :smartadmin-app:test --tests ArchitectureTest#tableNameMustBeSingular
```

### 3. Pre-commit Hook Integration

`.githooks/pre-commit`:
```bash
#!/bin/bash
# Check @TableName annotations before commit

echo "Running naming convention checker..."

# Find staged Entity files
staged_entities=$(git diff --cached --name-only | grep 'Entity\.java$')

if [ -n "$staged_entities" ]; then
    # Run quick validation
    for file in $staged_entities; do
        if grep -q '@TableName.*s"' "$file"; then
            echo "❌ Potential plural table name detected in $file"
            echo "Run: claude-code skills run naming-convention-checker"
            exit 1
        fi
    done
fi

echo "✅ Naming convention check passed"
```

---

## Usage Examples

### Example 1: Basic Scan

```bash
> /naming-convention-checker

# Output:
> Scanning codebase for @TableName annotations...
> Found 42 Entity classes
> Checking against plural patterns...
> Detected 2 violations
> Report generated: docs/quality/NAMING-VIOLATIONS-REPORT.md
```

### Example 2: After Entity Creation

```bash
# Scenario: Developer just created PlayerEntity.java with @TableName("t_players")

> 檢查命名規範

# Skill detects violation:
> ❌ Violation found in PlayerEntity.java
>    Current: @TableName("t_players")
>    Suggested: @TableName("t_player")
>
> Fix required before commit
```

### Example 3: Validation After Merge

```bash
# After merging a branch with new entities

> validate table names

# Skill runs and outputs:
> ✅ No violations found
> All 45 Entity classes follow singular naming standard
> 1 exemption applied: LiteFlowExecutionMetricsEntity
```

---

## Troubleshooting

### Issue 1: False Positives

**Problem**: Skill reports valid table names as violations

**Solution**:
1. Check if table should be in exemption list
2. Update `patterns/table-name-patterns.json` to add exemption
3. Update `ArchitectureTest.java` to match

### Issue 2: Pattern Not Detected

**Problem**: Plural table name not caught by skill

**Solution**:
1. Add new pattern to `patterns/table-name-patterns.json`
2. Update detection regex in skill logic
3. Add test case to verify

### Issue 3: Report Not Generated

**Problem**: Skill runs but no report file created

**Solution**:
1. Check `docs/quality/` directory exists
2. Verify write permissions
3. Check skill execution logs for errors

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-02 | Initial release with singular naming check and exemption support |

---

**Maintainer**: SmartAdmin Architecture Team
**Last Updated**: 2026-02-02

---

## 相關規則

本技能驗證以下 SmartAdmin 命名規範：

### 強制要求

- **[Naming Conventions - Table Names](./../../../.agent/rules/foundation/01-naming-conventions.md#table-naming)**
  - 表名必須使用單數形式（`t_employee` 不是 `t_employees`）
  - 本技能檢測所有 @TableName 註解中的複數命名
  - 豁免清單：特殊複數名詞（LiteFlowExecutionMetrics）

- **[Naming Conventions - Class Names](./../../../.agent/rules/foundation/01-naming-conventions.md#class-naming)**
  - Entity 命名：XXXEntity（不是 XXXPO, XXXDO）
  - Service 命名：XXXService（不是 XXXServiceImpl）
  - Manager 命名：XXXManager（不是 XXXManagerImpl）
  - Dao 命名：XXXDao（不是 XXXMapper）

- **[Naming Conventions - Field Names](./../../../.agent/rules/foundation/01-naming-conventions.md#field-naming)**
  - 布林欄位：`deleted` 不是 `isDeleted`
  - 本技能檢測欄位命名違規（實驗性功能）

### 參考指引

- **[Architecture Rules - Entity Layer](./../../../.agent/rules/foundation/10-architecture-rules.md)**
  - Entity 類必須使用 @TableName 註解
  - Entity 包結構：`..domain.entity`

- **[Database Schema Design](./../../../../docs/database/schema-design.md)**
  - 表命名規範（如適用）
  - 外鍵命名規範

---

## 參考資料

- [ArchitectureTest.java](./../../../.agent/configs/ArchitectureTest.java) - 命名規範的 ArchUnit 驗證
- [Table Name Patterns](patterns/table-name-patterns.json) - 複數模式檢測規則
- [Naming Conventions Complete Guide](./../../../.agent/rules/foundation/01-naming-conventions.md) - 完整命名規範
