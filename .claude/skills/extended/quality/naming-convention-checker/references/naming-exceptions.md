# Naming Convention Exemptions

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Maintainer**: SmartAdmin Architecture Team

---

## Overview

本文檔定義了 SmartAdmin 單數命名標準的豁免清單。某些表名因語言慣例或業務語義，允許使用複數或特殊形式。

---

## 豁免規則

### 1. Uncountable Nouns (不可數名詞)

#### Rule: `t_*_metrics`

**原因**: `metrics` 本身是複數形式的不可數名詞，表示聚合統計數據，無對應單數形式。

**適用場景**:
- 性能指標表
- 執行統計表
- 監控數據表

**範例**:
```java
@TableName("t_liteflow_execution_metrics")  // ✅ 允許
public class LiteFlowExecutionMetricsEntity {
    private Long metricsId;
    private String chainName;
    private Long executionCount;
    private Long averageDuration;
    // ...
}
```

**已知使用**:
- `t_liteflow_execution_metrics` - LiteFlow 規則引擎執行指標

**參考文檔**:
- [.agent/rules/foundation/01-naming-conventions.md](../../../../.agent/rules/foundation/01-naming-conventions.md) - Section 7
- [ADR-001: Naming Convention Singular Standard](../../../../docs/IGaming/architecture-decisions/ADR-001-Naming-Convention-Singular-Standard.md)

---

### 2. Statistics Tables

#### Rule: `t_*_statistics`

**原因**: `statistics` 在統計學和數據分析領域慣用複數形式，語義上表示多個統計數據的集合。

**適用場景**:
- 用戶統計表
- 遊戲統計表
- 財務統計表
- 業務指標彙總表

**範例**:
```java
@TableName("t_user_statistics")  // ✅ 允許
public class UserStatisticsEntity {
    private Long statisticsId;
    private Long userId;
    private Integer loginCount;
    private BigDecimal totalDeposit;
    private BigDecimal totalWithdrawal;
    // ...
}

@TableName("t_game_statistics")  // ✅ 允許
public class GameStatisticsEntity {
    private Long statisticsId;
    private Long gameId;
    private Integer playCount;
    private BigDecimal totalBet;
    private BigDecimal totalWin;
    // ...
}
```

**語言慣例**:
- 英文中 "statistics" 通常不使用單數形式 "statistic"
- 業界常見命名：`user_statistics`, `daily_statistics`

**參考標準**:
- ISO/IEC 11179 數據元素命名標準
- Database Design Best Practices

---

### 3. Analytics Tables

#### Rule: `t_*_analytics`

**原因**: `analytics` 在商業智能和數據分析領域慣用複數形式，表示多維度分析數據。

**適用場景**:
- 玩家行為分析表
- 業務分析表
- Web 分析表
- 預測分析表

**範例**:
```java
@TableName("t_player_analytics")  // ✅ 允許
public class PlayerAnalyticsEntity {
    private Long analyticsId;
    private Long playerId;
    private String behaviorPattern;
    private BigDecimal riskScore;
    private BigDecimal ltv;  // Lifetime Value
    // ...
}

@TableName("t_behavior_analytics")  // ✅ 允許
public class BehaviorAnalyticsEntity {
    private Long analyticsId;
    private Long userId;
    private String eventType;
    private String sessionId;
    private LocalDateTime eventTime;
    // ...
}
```

**業界慣例**:
- Google Analytics, Adobe Analytics 等工具使用複數形式
- 數據倉庫命名標準傾向使用 `analytics` 而非 `analytic`

**參考標準**:
- Data Warehousing Naming Conventions
- Business Intelligence Best Practices

---

## 添加新豁免規則

### Process

如需添加新的豁免規則，請遵循以下流程：

#### Step 1: 評估合理性

提出豁免申請時，需提供以下證明：
1. **語言慣例證明**: 該詞彙在英文中是否慣用複數形式
2. **業界標準證明**: 是否有權威標準或行業慣例支持
3. **業務語義證明**: 使用複數形式是否更符合業務語義

#### Step 2: 更新文檔

在以下文件中添加豁免規則：
1. `.agent/rules/foundation/01-naming-conventions.md` (Section 7)
2. `patterns/table-name-patterns.json` (exemptions 數組)
3. `references/naming-exceptions.md` (本文件)
4. `docs/IGaming/architecture-decisions/ADR-001-Naming-Convention-Singular-Standard.md`

#### Step 3: 更新代碼

在 `ArchitectureTest.java` 的 `tableNameMustBeSingular` 測試中添加豁免：

```java
// 豁免清單
if (tableName.endsWith("_metrics") ||
    tableName.endsWith("_statistics") ||
    tableName.endsWith("_analytics") ||
    tableName.endsWith("_new_exemption")) {  // ✅ 添加新豁免
    return;
}
```

#### Step 4: 審核與批准

豁免申請需經過以下審核：
- [ ] Architecture Team Review
- [ ] Technical Lead Approval
- [ ] ADR 文檔更新完成
- [ ] ArchUnit 測試驗證通過

---

## 非豁免案例

以下情況**不應**被列入豁免清單：

### ❌ 錯誤範例 1: 普通業務實體複數

```java
@TableName("t_users")  // ❌ 不允許，應使用 t_user
public class UserEntity { }

@TableName("t_orders")  // ❌ 不允許，應使用 t_order
public class OrderEntity { }
```

**原因**: user 和 order 有明確單數形式，無特殊業務語義需求。

### ❌ 錯誤範例 2: 關聯表複數

```java
@TableName("t_user_roles")  // ❌ 不允許，應使用 t_user_role
public class UserRoleEntity { }
```

**原因**: 即使是多對多關聯表，也應使用單數形式表示單條關聯記錄。

### ❌ 錯誤範例 3: 歷史表複數

```java
@TableName("t_order_histories")  // ❌ 不允許，應使用 t_order_history
public class OrderHistoryEntity { }
```

**原因**: history 本身已是單數形式，表示「一條歷史記錄」。

---

## 豁免清單總結

| 序號 | 表名模式 | 原因 | 範例 |
|-----|---------|------|------|
| 1 | `t_*_metrics` | 不可數名詞，聚合統計數據 | `t_liteflow_execution_metrics` |
| 2 | `t_*_statistics` | 統計學慣用複數 | `t_user_statistics`, `t_game_statistics` |
| 3 | `t_*_analytics` | 商業智能慣用複數 | `t_player_analytics`, `t_behavior_analytics` |

**當前豁免總數**: 3 個規則

---

## ArchUnit 測試集成

豁免規則在 ArchUnit 測試中的實現：

```java
@ArchTest
static final ArchRule tableNameMustBeSingular = classes()
    .that().areAnnotatedWith(TableName.class)
    .should(new ArchCondition<JavaClass>("use singular table names") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            com.baomidou.mybatisplus.annotation.TableName annotation =
                javaClass.tryGetAnnotationOfType(
                    com.baomidou.mybatisplus.annotation.TableName.class
                ).orElse(null);

            if (annotation == null) return;

            String tableName = annotation.value();

            // 豁免清單檢查
            if (tableName.endsWith("_metrics") ||
                tableName.endsWith("_statistics") ||
                tableName.endsWith("_analytics")) {
                return;  // 通過檢查
            }

            // 檢查複數形式...
        }
    });
```

---

## 版本歷史

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-02 | 初始版本：定義 metrics、statistics、analytics 三個豁免規則 |

---

**維護團隊**: SmartAdmin Architecture Team
**下次審閱**: 2026-05-02（每季度審閱）
