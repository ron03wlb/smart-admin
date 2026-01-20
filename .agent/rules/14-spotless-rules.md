---
trigger: always_on
description: Spotless 自動格式化規範
tags: [static-analysis, spotless, code-formatting]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/01-naming-conventions.md
  - rules/11-checkstyle-rules.md
last_updated: 2025-01-21
---

# Spotless 規範

**TL;DR**: Spotless 自動格式化代碼，使用 Google Java Format 標準。編譯前自動執行 `spotlessApply`，確保代碼風格一致。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 生成任何 Java 代碼時
- ✅ 提交代碼前
- ✅ `./gradlew spotlessCheck` 失敗時
- ✅ 用戶詢問代碼格式化問題

### 強制執行檢查清單
- [ ] 使用 Google Java Format 風格
- [ ] 移除未使用的 import
- [ ] 行尾無空白
- [ ] 文件末尾有換行符
- [ ] 提交前執行 `spotlessApply`

### AI 決策樹
```
Spotless 問題 → 識別問題類型
  ├─ 格式不符合 Google Style
  │   └─ 執行 ./gradlew spotlessApply
  ├─ 未使用的 import
  │   └─ 自動移除（removeUnusedImports）
  ├─ 行尾空白
  │   └─ 自動移除（trimTrailingWhitespace）
  └─ 文件末尾無換行
      └─ 自動添加（endWithNewline）
```

### 格式化規則

#### Google Java Format 風格
```java
// ✅ Google Java Format
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }
}
```

**特點**:
- 縮進: 2 空格
- 行寬: 100 字符
- 大括號: K&R 風格
- 空格: 操作符周圍有空格

---

## 【強制】功能說明

| 功能                       | 說明                         |
| -------------------------- | ---------------------------- |
| `googleJavaFormat()`       | 應用 Google Java Format 風格 |
| `removeUnusedImports()`    | 移除未使用的 import 語句     |
| `trimTrailingWhitespace()` | 移除行尾空白                 |
| `endWithNewline()`         | 確保文件末尾有換行符         |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # Spotless 插件配置
```

### build.gradle.kts 配置
```kotlin
// 插件引入
plugins {
    alias(libs.plugins.spotless) apply false
}

// 子項目配置
subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()              // Google Java Format
            removeUnusedImports()           // 移除未使用 import
            trimTrailingWhitespace()        // 移除行尾空白
            endWithNewline()                // 文件末尾換行
        }
    }
}

// 編譯前自動執行格式化
tasks.withType<JavaCompile> {
    dependsOn("spotlessApply")
}
```

### 版本配置（gradle/libs.versions.toml）
```toml
[versions]
spotless = "6.25.0"

[plugins]
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

---

## Git Hooks 整合

### Pre-commit Hook
```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "Running Spotless in smart-admin-api-java21-springboot3..."
cd smart-admin-api-java21-springboot3 && ./gradlew spotlessApply

# 檢查是否有變更
if ! git diff --quiet; then
    echo "Spotless made changes. Please review and re-commit."
    exit 1
fi
```

### 自動安裝 Git Hooks
build.gradle.kts 中已配置：
```kotlin
tasks.register("installGitHooks") {
    doLast {
        // 自動安裝 pre-commit hook
    }
}

tasks.named("build") {
    dependsOn("installGitHooks")
}
```

---

## 驗證命令

```bash
# 檢查格式（不修改文件）
./gradlew spotlessCheck

# 自動格式化
./gradlew spotlessApply

# 僅格式化 Java 文件
./gradlew spotlessJavaApply

# 查看哪些文件會被格式化
./gradlew spotlessJavaCheck --info
```

---

## Spotless vs Checkstyle

| 功能         | Spotless         | Checkstyle     |
| ------------ | ---------------- | -------------- |
| **作用**     | 自動格式化       | 風格檢查       |
| **修復**     | 自動修復         | 僅報告         |
| **執行時機** | 編譯前           | 編譯後         |
| **範圍**     | 格式/空白/import | 命名/邏輯/風格 |

**建議**:
- 先執行 `spotlessApply` 自動修復格式問題
- 再執行 `checkstyleMain` 檢查其他風格問題

---

## 相關規範

- [11-checkstyle-rules.md](./11-checkstyle-rules.md) - Checkstyle 風格檢查
- [01-naming-conventions.md](./01-naming-conventions.md) - 命名規範
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
