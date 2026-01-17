# Checkstyle 規範

**TL;DR**: Checkstyle 強制執行代碼風格一致性，包括命名規範、空白符、大括號、import 規則。所有違規必須在提交前修復。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 生成任何 Java 代碼時
- ✅ Code Review 時檢查代碼風格
- ✅ `./gradlew checkstyleMain` 或 `mvn checkstyle:check` 失敗時
- ✅ 用戶詢問代碼格式問題

### 強制執行檢查清單
- [ ] 禁止 `import *` 星號導入
- [ ] 每個文件只有一個頂層類
- [ ] 大括號 `{` 不換行（K&R 風格）
- [ ] 操作符周圍有空白符
- [ ] 類名 UpperCamelCase、方法名 lowerCamelCase
- [ ] 包名全小寫

### AI 決策樹
```
Checkstyle 錯誤 → 識別規則類型
  ├─ AvoidStarImport
  │   └─ 展開為具體類導入
  ├─ WhitespaceAround
  │   └─ 在操作符/關鍵字周圍添加空格
  ├─ NeedBraces
  │   └─ 添加大括號（即使單行）
  ├─ LeftCurly / RightCurly
  │   └─ 調整大括號位置
  └─ 命名規則 (TypeName/MethodName/...)
      └─ 按規範重命名
```

### 錯誤模式檢測與自動修正

#### AvoidStarImport
```java
// ❌ 違規
import java.util.*;
import org.springframework.web.bind.annotation.*;

// ✅ 修正
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
```

#### WhitespaceAround
```java
// ❌ 違規
if(condition){
int x=1+2;
}

// ✅ 修正
if (condition) {
    int x = 1 + 2;
}
```

#### NeedBraces
```java
// ❌ 違規
if (condition)
    doSomething();

// ✅ 修正
if (condition) {
    doSomething();
}
```

#### LeftCurly (K&R 風格)
```java
// ❌ 違規
public void method()
{
    // ...
}

// ✅ 修正
public void method() {
    // ...
}
```

---

## 【強制】啟用規則說明

| 規則                           | 說明                           | 示例                                  |
| ------------------------------ | ------------------------------ | ------------------------------------- |
| `AvoidStarImport`              | 禁止星號導入                   | `import java.util.*` ❌                |
| `OneTopLevelClass`             | 每個文件只有一個頂層類         | -                                     |
| `NoLineWrap`                   | 禁止 package/import 換行       | -                                     |
| `NeedBraces`                   | 強制大括號                     | `if (x) y;` ❌                         |
| `LeftCurly`                    | 左大括號不換行                 | K&R 風格                              |
| `RightCurly`                   | 右大括號獨立一行或與 else 同行 | -                                     |
| `WhitespaceAround`             | 操作符周圍空白                 | `x=1` ❌ → `x = 1` ✅                   |
| `OneStatementPerLine`          | 每行一個語句                   | -                                     |
| `MultipleVariableDeclarations` | 每行一個變量聲明               | `int a, b;` ❌                         |
| `ArrayTypeStyle`               | 數組類型風格                   | `String args[]` ❌ → `String[] args` ✅ |
| `UpperEll`                     | 長整型用大寫 L                 | `100l` ❌ → `100L` ✅                   |
| `ModifierOrder`                | 修飾符順序                     | `public static final`                 |
| `PackageName`                  | 包名全小寫                     | `com.example.myapp`                   |
| `TypeName`                     | 類名 UpperCamelCase            | `UserService`                         |
| `MemberName`                   | 成員名 lowerCamelCase          | `userName`                            |
| `ParameterName`                | 參數名 lowerCamelCase          | `userId`                              |
| `LocalVariableName`            | 局部變量 lowerCamelCase        | `itemCount`                           |
| `MethodName`                   | 方法名 lowerCamelCase          | `getUserById`                         |
| `NoFinalizer`                  | 禁止 finalize() 方法           | -                                     |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
├── build.gradle.kts          # Checkstyle 插件配置
└── config/checkstyle/
    └── checkstyle.xml        # 規則配置文件
```

### build.gradle.kts 配置
```kotlin
configure<CheckstyleExtension> {
    toolVersion = libs.findVersion("checkstyle").get().toString()
    isIgnoreFailures = false  // 發現違規即失敗
    maxWarnings = 0           // 警告也視為錯誤
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
}
```

### checkstyle.xml 配置
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>
    <property name="fileExtensions" value="java, properties, xml"/>

    <module name="TreeWalker">
        <module name="AvoidStarImport"/>
        <module name="OneTopLevelClass"/>
        <module name="NoLineWrap"/>
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAround">
            <property name="allowEmptyConstructors" value="true"/>
            <property name="allowEmptyMethods" value="true"/>
            <property name="allowEmptyTypes" value="true"/>
            <property name="allowEmptyLoops" value="true"/>
            <property name="allowEmptyLambdas" value="true"/>
            <property name="allowEmptyCatches" value="true"/>
        </module>
        <module name="OneStatementPerLine"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="ArrayTypeStyle"/>
        <module name="UpperEll"/>
        <module name="ModifierOrder"/>
        <module name="PackageName">
            <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
        </module>
        <module name="TypeName"/>
        <module name="MemberName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="MethodName"/>
        <module name="NoFinalizer"/>
    </module>
</module>
```

---

## 驗證命令

```bash
# Gradle
./gradlew checkstyleMain checkstyleTest

# 查看報告
open sa-admin/build/reports/checkstyle/main.html

# Maven (如使用)
mvn checkstyle:check
```

---

## 相關規範

- [01-naming-conventions.md](./01-naming-conventions.md) - 命名規範詳細說明
- [14-spotless-rules.md](./14-spotless-rules.md) - 自動格式化（可修復部分 Checkstyle 問題）
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
