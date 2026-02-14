# Naming Convention Checker

**版本**: 1.0.0
**類別**: Quality (P1 - Extended)
**狀態**: ✅ Stable

## 快速開始

### 使用場景

自動檢查 SmartAdmin 代碼庫中的 Entity 類 `@TableName` 註解是否遵循單數命名標準。

```bash
# 使用 Claude Code 調用技能（推薦）
> /naming-convention-checker

# 或通過關鍵詞觸發
> 檢查命名規範
> 驗證表名是否符合單數標準
```

### 主要功能

1. **掃描 Entity 類**
   自動查找所有帶 `@TableName` 註解的 Entity 類

2. **檢測複數形式**
   識別常見複數模式：`t_users`, `t_orders`, `t_goods`, `t_players` 等

3. **豁免驗證**
   支持豁免清單：`t_liteflow_execution_metrics`, `t_*_statistics`, `t_*_analytics`

4. **生成報告**
   生成詳細違規報告：`docs/quality/NAMING-VIOLATIONS-REPORT.md`

## 檢查規則

### ✅ 正確範例

```java
@TableName("t_good")     // ✅ 使用單數
@TableName("t_user")     // ✅ 使用單數
@TableName("t_player")   // ✅ 使用單數
@TableName("t_wallet")   // ✅ 使用單數
```

### ❌ 錯誤範例

```java
@TableName("t_goods")       // ❌ 複數形式
@TableName("t_users")       // ❌ 複數形式
@TableName("t_players")     // ❌ 複數形式
@TableName("t_wallets")     // ❌ 複數形式
```

### 🔓 豁免清單

以下表名允許使用複數或特殊形式：

| 表名 | 原因 | 規則 |
|------|------|------|
| `t_liteflow_execution_metrics` | metrics 是不可數名詞 | 保持複數 |
| `t_*_statistics` | statistics 慣用複數 | 保持複數 |
| `t_*_analytics` | analytics 慣用複數 | 保持複數 |

## 輸出報告

技能執行後會生成以下報告：

```markdown
docs/quality/NAMING-VIOLATIONS-REPORT.md
```

報告包含：
- 違規 Entity 類列表
- 當前表名 vs 推薦表名
- 文件路徑和行號
- 修正建議

## 集成

### ArchUnit 測試

技能檢查邏輯與 ArchUnit 測試同步：

```java
@ArchTest
static final ArchRule tableNameMustBeSingular = classes()
    .that().areAnnotatedWith(TableName.class)
    .should(new ArchCondition<JavaClass>("use singular table names") {
        // 檢查邏輯
    });
```

運行測試：
```bash
./gradlew :smartadmin-app:test --tests ArchitectureTest#tableNameMustBeSingular
```

### CI/CD 集成

參考：`.github/workflows/naming-check.yml`

## 相關文檔

- [01-naming-conventions.md](.agent/rules/foundation/01-naming-conventions.md) - 第 7 節
- [ADR-001: Naming Convention Singular Standard](../../../docs/IGaming/architecture-decisions/ADR-001-Naming-Convention-Singular-Standard.md)
- [SKILL.md](./SKILL.md) - 詳細技能文檔

## 版本歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-02-02 | 初始版本，支持單數命名檢查和豁免清單 |

---

**維護團隊**: SmartAdmin Architecture Team
**最後更新**: 2026-02-02
