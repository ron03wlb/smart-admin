# Naming Convention Checker - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: naming-convention-checker (P1 - Extended/Quality)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| Full Validation | Validate all Entity classes | ~2 min |
| Package Scan | Validate specific package | ~30s |
| Generate Report | Create detailed violation report | ~1 min |
| Auto-Fix | Generate corrected @TableName annotations | ~3 min |

### Rapid Validation Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Scan Entities | Detect @TableName annotations | ~30s |
| 2. Apply Rules | Check singular naming + exemptions | ~1 min |
| 3. Generate Report | List violations with suggestions | ~30s |
| 4. Fix Violations | Update Entity classes | ~5-10 min |
| 5. Re-validate | Verify fixes | ~1 min |

---

## Detection Pattern Matrix (Decision Guide)

### Pattern 1: Plural Table Names (VIOLATION)

**Trigger Keywords**: "players", "orders", "items", "categories"

**Severity**: ⚠️ BLOCKER

**Problem**:
```java
// ❌ WRONG - Plural table name
@TableName("t_players")
@Data
public class PlayerEntity {
    private Long id;
    private String username;
}
```

**Fix**:
```java
// ✅ CORRECT - Singular table name
@TableName("t_player")
@Data
public class PlayerEntity {
    private Long id;
    private String username;
}
```

**Detection Pattern**:
- Ends with `s` (players, orders, items)
- Irregular plurals (children, people, entries)
- Database table name != Entity class name pattern

**Time to Fix**: 2-5 minutes

---

### Pattern 2: Metrics Table (EXEMPTION)

**Trigger Keywords**: "metrics", "performance_metrics", "user_metrics"

**Severity**: ✅ PASS

**Pattern**:
```java
// ✅ EXEMPTION - Metrics are uncountable
@TableName("t_player_metrics")
@Data
public class PlayerMetricsEntity {
    private Long id;
    private Long playerId;
    private BigDecimal totalBet;
    private BigDecimal totalWin;
}
```

**Justification**:
- "Metrics" is an uncountable noun (like "data", "information")
- Aggregate statistics table pattern
- Industry standard for analytics tables

**Pattern**: `t_*_metrics` or `t_metrics_*`

---

### Pattern 3: Statistics Table (EXEMPTION)

**Trigger Keywords**: "statistics", "stats", "daily_statistics"

**Severity**: ✅ PASS

**Pattern**:
```java
// ✅ EXEMPTION - Statistics are uncountable
@TableName("t_game_statistics")
@Data
public class GameStatisticsEntity {
    private Long id;
    private Long gameId;
    private Integer totalPlayers;
    private BigDecimal totalRevenue;
}
```

**Justification**:
- "Statistics" is an uncountable noun
- Statistics domain convention
- Aggregated data pattern

**Pattern**: `t_*_statistics` or `t_statistics_*`

---

### Pattern 4: Analytics Table (EXEMPTION)

**Trigger Keywords**: "analytics", "user_analytics", "behavior_analytics"

**Severity**: ✅ PASS

**Pattern**:
```java
// ✅ EXEMPTION - Analytics are uncountable
@TableName("t_user_analytics")
@Data
public class UserAnalyticsEntity {
    private Long id;
    private Long userId;
    private String eventType;
    private LocalDateTime eventTime;
}
```

**Justification**:
- "Analytics" is an uncountable noun
- Business intelligence convention
- Event tracking pattern

**Pattern**: `t_*_analytics` or `t_analytics_*`

---

### Pattern 5: Irregular Plurals (VIOLATION)

**Trigger Keywords**: "children", "people", "entries", "categories"

**Severity**: ⚠️ BLOCKER

**Problem**:
```java
// ❌ WRONG - Irregular plural
@TableName("t_product_categories")
@Data
public class ProductCategoryEntity {
    private Long id;
    private String categoryName;
}
```

**Fix**:
```java
// ✅ CORRECT - Singular form
@TableName("t_product_category")
@Data
public class ProductCategoryEntity {
    private Long id;
    private String categoryName;
}
```

**Common Irregular Plurals**:
- categories → category
- entries → entry
- children → child
- people → person
- men/women → man/woman

**Time to Fix**: 2-5 minutes

---

## Common Errors and Quick Fixes

### Error 1: Plural Table Name (BLOCKER)

**Symptom**: `@TableName("t_players")` detected

**Quick Fix**:
```java
// Step 1: Update @TableName annotation
// Before:
@TableName("t_players")
@Data
public class PlayerEntity { }

// After:
@TableName("t_player")
@Data
public class PlayerEntity { }
```

**Database Migration** (if needed):
```sql
-- Rename table in database
ALTER TABLE t_players RENAME TO t_player;

-- Or keep old table and update application only
-- (No DB change needed if @TableName is already correct)
```

**Time to Fix**: 2-5 minutes (code only), 10-15 minutes (with DB migration)

---

### Error 2: False Positive - Metrics Table

**Symptom**: `@TableName("t_player_metrics")` flagged as plural

**Cause**: Detection pattern incorrectly flags "-metrics" suffix

**Fix**: Add exemption rule
```java
// ✅ No fix needed - Metrics is exempt
@TableName("t_player_metrics")
@Data
public class PlayerMetricsEntity { }
```

**Validation**:
```bash
# Verify exemption rule is applied
grep -r "t_.*_metrics" sa-admin/src/main/java/*/entity/
# Expected: No violations reported
```

**Time to Fix**: 0 minutes (no action needed)

---

### Error 3: Missing @TableName Annotation

**Symptom**: Entity class with no `@TableName` annotation

**Cause**: MyBatis-Plus default naming strategy may not match SmartAdmin standard

**Fix**:
```java
// Before (relies on default naming):
@Data
public class PlayerEntity {  // Assumes table "player_entity"
    private Long id;
}

// After (explicit @TableName):
@TableName("t_player")
@Data
public class PlayerEntity {
    private Long id;
}
```

**Time to Fix**: 1-2 minutes

---

### Error 4: Inconsistent Naming Between Entity and Table

**Symptom**: `PlayerEntity` with `@TableName("t_user_player")`

**Cause**: Entity class name doesn't match table name pattern

**Fix**:
```java
// Option 1: Rename Entity class (recommended)
@TableName("t_player")
@Data
public class PlayerEntity { }  // Entity name matches table

// Option 2: Keep Entity name, clarify with comment
@TableName("t_user_player")
@Data
public class UserPlayerEntity {  // Renamed to match table
    // User-Player relationship entity
}
```

**Time to Fix**: 5-10 minutes (includes refactoring references)

---

### Error 5: Database Table Already Plural

**Symptom**: Legacy database with `t_players` table

**Cause**: SmartAdmin standard applied to existing database

**Fix Options**:

**Option A: Rename Database Table (Recommended)**
```sql
-- Migration script
ALTER TABLE t_players RENAME TO t_player;

-- Update Entity
@TableName("t_player")
@Data
public class PlayerEntity { }
```

**Option B: Keep Database, Document Exception**
```java
// Temporary exemption (document reason)
@TableName("t_players")  // Legacy DB table - TODO: Migrate to singular
@Data
public class PlayerEntity { }

// Add suppression comment for naming-convention-checker
// @SuppressNamingConvention(reason = "Legacy database table")
```

**Time to Fix**: 15-30 minutes (Option A with testing), 5 minutes (Option B)

---

## Detection Flow

### Step-by-Step Validation Process

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Scan Entity Classes                                      │
│    - Find all classes with @TableName annotation            │
│    - Extract table name from annotation value               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Apply Exemption Rules (Pass if match)                   │
│    - t_*_metrics → PASS                                     │
│    - t_*_statistics → PASS                                  │
│    - t_*_analytics → PASS                                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Check Plural Patterns (Fail if match)                   │
│    - Ends with 's' (players, orders) → FAIL                │
│    - Irregular plurals (categories, entries) → FAIL         │
│    - Special cases (men, women, children) → FAIL            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Generate Report                                          │
│    - List violations with file path and line number         │
│    - Suggest singular form fix                              │
│    - Provide time estimate for fix                          │
└─────────────────────────────────────────────────────────────┘
```

### Validation Rules Priority

| Priority | Rule | Action |
|----------|------|--------|
| 1 | Exemption (metrics/statistics/analytics) | PASS - Skip validation |
| 2 | Plural pattern detected | FAIL - Report violation |
| 3 | Irregular plural detected | FAIL - Report violation |
| 4 | Singular form | PASS - Compliant |

---

## ArchUnit Integration

### Test Implementation

```java
package net.lab1024.sa.admin.test.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

public class NamingConventionTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("net.lab1024.sa");

    @Test
    public void tableName_shouldBeSingular() {
        ArchRule rule = classes()
            .that().areAnnotatedWith("com.baomidou.mybatisplus.annotation.TableName")
            .should(new TableNameSingularCondition());

        rule.check(classes);
    }
}

class TableNameSingularCondition extends ArchCondition<JavaClass> {
    @Override
    public void check(JavaClass javaClass, ConditionEvents events) {
        TableName annotation = javaClass.getAnnotation(TableName.class);
        String tableName = annotation.value();

        // Apply exemption rules
        if (isExempt(tableName)) {
            return;  // Pass
        }

        // Check plural patterns
        if (isPlural(tableName)) {
            String message = String.format(
                "Table name '%s' in class %s should be singular",
                tableName, javaClass.getName()
            );
            events.add(SimpleConditionEvent.violated(javaClass, message));
        }
    }

    private boolean isExempt(String tableName) {
        return tableName.matches("t_.*_metrics")
            || tableName.matches("t_.*_statistics")
            || tableName.matches("t_.*_analytics");
    }

    private boolean isPlural(String tableName) {
        // Ends with 's' (players, orders)
        if (tableName.endsWith("s") && !tableName.endsWith("ss")) {
            return true;
        }
        // Irregular plurals (categories, entries, children)
        if (tableName.matches(".*(categories|entries|children|people).*")) {
            return true;
        }
        return false;
    }
}
```

### Running Validation

```bash
# Run ArchUnit test
./gradlew :sa-admin:test --tests NamingConventionTest

# Expected output (compliant):
# NamingConventionTest > tableName_shouldBeSingular() PASSED

# Expected output (violations):
# NamingConventionTest > tableName_shouldBeSingular() FAILED
#   Table name 't_players' in class PlayerEntity should be singular
#   Table name 't_categories' in class CategoryEntity should be singular
```

---

## Time Estimates (Production Data)

| Violation Type | Detection Time | Fix Time | Verification Time | Total |
|---------------|---------------|----------|-------------------|-------|
| Plural table name | 1 min | 2-5 min | 1 min | 4-7 min |
| Irregular plural | 1 min | 2-5 min | 1 min | 4-7 min |
| Missing @TableName | 1 min | 1-2 min | 1 min | 3-4 min |
| Inconsistent naming | 2 min | 5-10 min | 2 min | 9-14 min |
| Database migration | 2 min | 15-30 min | 5 min | 22-37 min |

**Full Codebase Scan**: 2-5 minutes (depending on codebase size)

---

## Exemption Rules Summary

### Rule 1: Metrics Tables

**Pattern**: `t_*_metrics` or `t_metrics_*`

**Rationale**: "Metrics" is an uncountable noun representing aggregate statistics

**Examples**:
- ✅ `t_player_metrics`
- ✅ `t_game_metrics`
- ✅ `t_metrics_daily`

---

### Rule 2: Statistics Tables

**Pattern**: `t_*_statistics` or `t_statistics_*`

**Rationale**: "Statistics" is an uncountable noun in statistics domain

**Examples**:
- ✅ `t_game_statistics`
- ✅ `t_user_statistics`
- ✅ `t_statistics_hourly`

---

### Rule 3: Analytics Tables

**Pattern**: `t_*_analytics` or `t_analytics_*`

**Rationale**: "Analytics" is an uncountable noun in business intelligence

**Examples**:
- ✅ `t_user_analytics`
- ✅ `t_behavior_analytics`
- ✅ `t_analytics_events`

---

## Validation Checklist

Before committing naming convention changes:

- [ ] All Entity classes scanned for @TableName annotations
- [ ] Plural patterns detected and reported
- [ ] Exemption rules applied correctly (metrics/statistics/analytics)
- [ ] Irregular plurals identified (categories → category)
- [ ] Entity class names consistent with table names
- [ ] ArchUnit test passes (NamingConventionTest)
- [ ] Database migration script created (if needed)
- [ ] All references updated (Service/Manager/Dao)

---

**See Also**:
- [Naming Conventions](./../../../.agent/rules/foundation/01-naming-conventions.md) - Complete naming standards
- [Naming Exceptions](../references/naming-exceptions.md) - Detailed exemption rules
- [ArchUnit Test Generator](./../../../foundation/backend/archunit-test-generator/) - Generate ArchUnit tests
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - Domain object patterns
