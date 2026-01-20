---
trigger: always_on
description: PMD 代碼質量規範
tags: [static-analysis, pmd, code-quality, best-practices]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/02-oop-principles.md
  - rules/04-exception-logging.md
last_updated: 2025-01-21
---

# PMD 規範

**TL;DR**: PMD 檢測代碼異味、潛在 Bug 和最佳實踐違規。啟用 `bestpractices` 和 `errorprone` 規則集，所有 Priority 1-3 違規必須修復。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 生成任何 Java 代碼時
- ✅ Code Review 時檢查代碼質量
- ✅ `./gradlew pmdMain` 或 `mvn pmd:check` 失敗時
- ✅ 用戶詢問代碼異味問題

### 強制執行檢查清單
- [ ] 使用接口類型而非實現類型（LooseCoupling）
- [ ] 無重複字符串字面量（AvoidDuplicateLiterals）
- [ ] 無未使用的變量賦值（UnusedAssignment）
- [ ] 日誌語句有 Guard（GuardLogStatement）
- [ ] Serializable 類有 serialVersionUID

### AI 決策樹
```
PMD 違規 → 識別規則類型
  ├─ LooseCoupling
  │   └─ 將 HashMap/ArrayList 改為 Map/List
  ├─ AvoidDuplicateLiterals
  │   └─ 提取為常量
  ├─ UnusedAssignment
  │   └─ 移除未使用的初始化
  ├─ GuardLogStatement
  │   └─ 添加日誌級別檢查或使用佔位符
  └─ MissingSerialVersionUID
      └─ 添加 serialVersionUID 字段
```

### 錯誤模式檢測與自動修正

#### LooseCoupling - 鬆耦合
```java
// ❌ 違規 - 使用實現類型
public HashMap<String, Object> processData() {
    HashMap<String, Object> result = new HashMap<>();
    return result;
}

// ✅ 修正 - 使用接口類型
public Map<String, Object> processData() {
    Map<String, Object> result = new HashMap<>();
    return result;
}
```

#### AvoidDuplicateLiterals - 避免重複字面量
```java
// ❌ 違規 - 字符串重複 4 次以上
@Select("SELECT * FROM t_user WHERE deleted_flag = 0")
User findOne();
@Select("SELECT * FROM t_user WHERE deleted_flag = 0 AND status = 1")
List<User> findActive();

// ✅ 修正 - 提取常量
private static final String DELETED_FLAG = "deleted_flag";
private static final String NOT_DELETED = DELETED_FLAG + " = 0";
```

#### UnusedAssignment - 未使用的賦值
```java
// ❌ 違規 - 初始化值被覆蓋
List<User> users = null;
if (condition) {
    users = findActiveUsers();
} else {
    users = findAllUsers();
}

// ✅ 修正 - 移除無用初始化
List<User> users;
if (condition) {
    users = findActiveUsers();
} else {
    users = findAllUsers();
}
```

#### GuardLogStatement - 日誌 Guard
```java
// ❌ 違規 - 無 Guard 的 debug 日誌
log.debug("Processing user: " + user.toString());

// ✅ 修正方案 1 - 使用佔位符
log.debug("Processing user: {}", user);

// ✅ 修正方案 2 - 添加 Guard
if (log.isDebugEnabled()) {
    log.debug("Processing user: " + user.toString());
}
```

#### MissingSerialVersionUID
```java
// ❌ 違規 - 缺少 serialVersionUID
public class MenuTreeVO implements Serializable {
    private Long id;
    private String name;
}

// ✅ 修正
public class MenuTreeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
}
```

---

## 【強制】啟用規則說明

### Best Practices 規則集

| 規則                    | 優先級 | 說明                   |
| ----------------------- | ------ | ---------------------- |
| `LooseCoupling`         | P3     | 使用接口類型而非實現類 |
| `UnusedAssignment`      | P3     | 避免未使用的變量賦值   |
| `GuardLogStatement`     | P2     | 日誌語句需要 Guard     |
| `UnusedFormalParameter` | P3     | 避免未使用的方法參數   |
| `UnusedLocalVariable`   | P3     | 避免未使用的局部變量   |
| `UnusedPrivateField`    | P3     | 避免未使用的私有字段   |
| `UnusedPrivateMethod`   | P3     | 避免未使用的私有方法   |

### Error Prone 規則集

| 規則                                  | 優先級 | 說明                                 |
| ------------------------------------- | ------ | ------------------------------------ |
| `AvoidDuplicateLiterals`              | P3     | 字符串字面量重複 ≥4 次需提取常量     |
| `MissingSerialVersionUID`             | P3     | Serializable 類需要 serialVersionUID |
| `CloseResource`                       | P3     | 資源需要正確關閉                     |
| `EmptyCatchBlock`                     | P3     | 禁止空 catch 塊                      |
| `AvoidBranchingStatementAsLastInLoop` | P2     | 循環末尾避免 break/continue/return   |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # PMD 插件配置
```

### build.gradle.kts 配置
```kotlin
configure<PmdExtension> {
    toolVersion = libs.findVersion("pmd").get().toString()
    isIgnoreFailures = false  // 發現違規即失敗
    ruleSets = listOf(
        "category/java/errorprone.xml",
        "category/java/bestpractices.xml"
    )
}
```

### 自定義規則排除（如需要）
```kotlin
configure<PmdExtension> {
    ruleSets = emptyList()
    ruleSetFiles = files("config/pmd/custom-rules.xml")
}
```

---

## 常見違規類型統計

基於專案實際違規分析：

| 違規類型                  | 數量 | 主要位置           |
| ------------------------- | ---- | ------------------ |
| `LooseCoupling`           | ~10  | Controller/Service |
| `AvoidDuplicateLiterals`  | ~8   | DAO/Mapper         |
| `UnusedAssignment`        | ~4   | Service            |
| `GuardLogStatement`       | ~2   | Manager            |
| `MissingSerialVersionUID` | ~1   | VO/DTO             |

---

## 驗證命令

```bash
# Gradle
./gradlew pmdMain pmdTest

# 查看報告
open sa-admin/build/reports/pmd/main.html

# Maven (如使用)
mvn pmd:check

# 僅檢查特定模塊
./gradlew :sa-admin:pmdMain
```

---

## 相關規範

- [11-checkstyle-rules.md](./11-checkstyle-rules.md) - 代碼風格檢查
- [13-spotbugs-rules.md](./13-spotbugs-rules.md) - Bug 模式檢測
- [04-exception-logging.md](./04-exception-logging.md) - 日誌規範
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
