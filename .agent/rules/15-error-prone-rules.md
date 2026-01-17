# Error Prone 規範

**TL;DR**: Error Prone 是 Google 開發的 Java 編譯器插件，在編譯時檢測常見 Bug 模式並提供修復建議。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 生成任何 Java 代碼時
- ✅ 編譯失敗且錯誤來自 Error Prone 時
- ✅ 用戶詢問編譯時 Bug 檢測

### 強制執行檢查清單
- [ ] 無 `==` 比較字符串（使用 `equals()`）
- [ ] 無忽略返回值的方法調用
- [ ] 無格式化字符串參數數量不匹配
- [ ] 無未使用的變量/參數
- [ ] 無不安全的類型轉換

### AI 決策樹
```
Error Prone 錯誤 → 識別 Bug 類型
  ├─ StringEquality
  │   └─ 使用 equals() 替代 ==
  ├─ ReturnValueIgnored
  │   └─ 處理或賦值返回結果
  ├─ FormatString
  │   └─ 修正格式化參數數量
  └─ UnusedVariable
      └─ 移除或使用該變量
```

### 錯誤模式檢測與自動修正

#### StringEquality - 字符串比較
```java
// ❌ 違規 - 編譯錯誤
if (status == "ACTIVE") {
    // ...
}

// ✅ 修正
if ("ACTIVE".equals(status)) {
    // ...
}

// ✅ 或使用 Objects.equals
if (Objects.equals(status, "ACTIVE")) {
    // ...
}
```

#### ReturnValueIgnored - 忽略返回值
```java
// ❌ 違規 - 編譯警告/錯誤
stringBuilder.append("text");  // StringBuilder.append 返回 this
file.delete();                  // File.delete 返回 boolean

// ✅ 修正
stringBuilder = stringBuilder.append("text");
// 或鏈式調用
stringBuilder.append("text").append("more");

// 對於 delete
if (!file.delete()) {
    log.warn("Failed to delete file: {}", file);
}
```

#### FormatString - 格式化字符串
```java
// ❌ 違規 - 參數數量不匹配
String.format("User: %s, Age: %d", name);  // 缺少 age 參數
log.info("Processing {} with status {}", id);  // 缺少 status 參數

// ✅ 修正
String.format("User: %s, Age: %d", name, age);
log.info("Processing {} with status {}", id, status);
```

#### UnusedVariable - 未使用變量
```java
// ❌ 違規
public void process(String data) {
    int count = 0;  // 未使用
    // ...
}

// ✅ 修正 - 移除未使用變量
public void process(String data) {
    // ...
}

// ✅ 或者使用 @SuppressWarnings
@SuppressWarnings("UnusedVariable")
public void process(String data) {
    int count = 0;  // 有意保留
    // ...
}
```

---

## 【強制】啟用規則說明

### 默認錯誤級別

| Bug Pattern          | 說明                   | 級別    |
| -------------------- | ---------------------- | ------- |
| `StringEquality`     | 使用 == 比較字符串     | ERROR   |
| `ReturnValueIgnored` | 忽略重要返回值         | WARNING |
| `FormatString`       | 格式化參數不匹配       | ERROR   |
| `MissingOverride`    | 缺少 @Override         | WARNING |
| `UnusedVariable`     | 未使用的變量           | WARNING |
| `UnusedMethod`       | 未使用的私有方法       | WARNING |
| `FallThrough`        | switch 語句穿透        | WARNING |
| `EqualsHashCode`     | equals/hashCode 不匹配 | ERROR   |

### 安全相關規則

| Bug Pattern          | 說明              | 級別    |
| -------------------- | ----------------- | ------- |
| `InsecureCipherMode` | 不安全的加密模式  | ERROR   |
| `BadShiftAmount`     | 錯誤的位移量      | ERROR   |
| `ArrayToString`      | 數組直接 toString | WARNING |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # Error Prone 插件配置
```

### build.gradle.kts 配置
```kotlin
plugins {
    alias(libs.plugins.errorprone) apply false
}

subprojects {
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        "errorprone"(libs.findLibrary("error-prone-core").get())
    }
}
```

### 版本配置（gradle/libs.versions.toml）
```toml
[versions]
errorprone = "2.36.0"
errorprone-plugin = "4.1.0"

[libraries]
error-prone-core = { module = "com.google.errorprone:error_prone_core", version.ref = "errorprone" }

[plugins]
errorprone = { id = "net.ltgt.errorprone", version.ref = "errorprone-plugin" }
```

### 自定義錯誤級別
```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // 將警告升級為錯誤
        error("ReturnValueIgnored")
        
        // 將錯誤降級為警告
        warn("UnusedVariable")
        
        // 禁用特定檢查
        disable("MissingSummary")
    }
}
```

---

## 抑制警告

### 方法級別
```java
@SuppressWarnings("ReturnValueIgnored")
public void processFile(File file) {
    file.delete();  // 有意忽略返回值
}
```

### 行級別
```java
@SuppressWarnings("StringEquality")
public boolean isSameInstance(String a, String b) {
    return a == b;  // 故意比較引用
}
```

---

## Error Prone vs SpotBugs

| 功能         | Error Prone  | SpotBugs         |
| ------------ | ------------ | ---------------- |
| **執行時機** | 編譯時       | 編譯後           |
| **分析方式** | 源碼分析     | 字節碼分析       |
| **速度**     | 快（編譯時） | 慢（單獨步驟）   |
| **規則數量** | ~500         | ~450             |
| **安全規則** | 較少         | FindSecBugs 豐富 |

**建議**: 兩者互補使用
- Error Prone: 編譯時快速反饋
- SpotBugs: 更深入的安全和 Bug 分析

---

## 驗證命令

```bash
# Error Prone 在編譯時自動執行
./gradlew compileJava

# 查看編譯輸出中的 Error Prone 警告
./gradlew compileJava --warning-mode all

# 如需單獨運行（通常不需要）
./gradlew check
```

---

## 相關規範

- [13-spotbugs-rules.md](./13-spotbugs-rules.md) - SpotBugs Bug 檢測
- [02-oop-principles.md](./02-oop-principles.md) - OOP 規範
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
