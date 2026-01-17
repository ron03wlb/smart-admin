# SpotBugs 規範

**TL;DR**: SpotBugs 檢測潛在 Bug 和安全漏洞。配置 MAX effort + LOW confidence，所有檢測到的問題必須修復。結合 FindSecBugs 插件進行安全掃描。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 生成任何 Java 代碼時
- ✅ Code Review 時檢查潛在 Bug
- ✅ `./gradlew spotbugsMain` 失敗時
- ✅ 用戶詢問 Null 安全或安全漏洞問題

### 強制執行檢查清單
- [ ] 無潛在的 NullPointerException
- [ ] 資源正確關閉（try-with-resources）
- [ ] 無 SQL 注入風險
- [ ] 無硬編碼密碼/密鑰
- [ ] equals/hashCode 正確實現

### AI 決策樹
```
SpotBugs 違規 → 識別 Bug 類型
  ├─ NP_* (Null Pointer)
  │   └─ 使用 Vavr Option 或添加 null 檢查
  ├─ RCN_* (Redundant Null Check)
  │   └─ 移除冗餘檢查
  ├─ OBL_* (Obligatory Resource Close)
  │   └─ 使用 try-with-resources
  ├─ SQL_* (SQL Injection)
  │   └─ 使用 LambdaQueryWrapper 或參數綁定
  └─ HARD_CODE_* (Hardcoded Secrets)
      └─ 移至配置文件或環境變量
```

### 錯誤模式檢測與自動修正

#### NP_NULL_ON_SOME_PATH - 可能的空指針
```java
// ❌ 違規
public String getUserEmail(Long id) {
    User user = userMapper.selectById(id);
    return user.getEmail();  // user 可能為 null
}

// ✅ 修正 - 使用 Vavr Option
public Option<String> getUserEmail(Long id) {
    return Option.of(userMapper.selectById(id))
        .map(User::getEmail);
}

// ✅ 修正 - 傳統方式
public String getUserEmail(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new NotFoundException("用戶不存在");
    }
    return user.getEmail();
}
```

#### OBL_UNSATISFIED_OBLIGATION - 資源未關閉
```java
// ❌ 違規
public String readFile(String path) throws IOException {
    InputStream is = new FileInputStream(path);
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    return reader.readLine();  // 資源未關閉
}

// ✅ 修正 - try-with-resources
public String readFile(String path) throws IOException {
    try (InputStream is = new FileInputStream(path);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        return reader.readLine();
    }
}
```

#### SQL_INJECTION - SQL 注入
```java
// ❌ 違規
public List<User> findByName(String name) {
    String sql = "SELECT * FROM t_user WHERE name = '" + name + "'";
    return jdbcTemplate.query(sql, rowMapper);
}

// ✅ 修正 - 使用 LambdaQueryWrapper
public List<User> findByName(String name) {
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .eq(User::getName, name)
    );
}

// ✅ 修正 - 使用參數綁定
public List<User> findByName(String name) {
    String sql = "SELECT * FROM t_user WHERE name = ?";
    return jdbcTemplate.query(sql, rowMapper, name);
}
```

#### HARD_CODE_PASSWORD - 硬編碼密碼
```java
// ❌ 違規
private static final String DB_PASSWORD = "mypassword123";

// ✅ 修正 - 使用配置
@Value("${spring.datasource.password}")
private String dbPassword;
```

---

## 【強制】啟用規則說明

### Correctness（正確性）

| Bug Pattern             | 說明                    | 嚴重程度 |
| ----------------------- | ----------------------- | -------- |
| `NP_NULL_ON_SOME_PATH`  | 可能的空指針解引用      | High     |
| `NP_NULL_PARAM_DEREF`   | 將 null 傳給非空參數    | High     |
| `EC_UNRELATED_TYPES`    | equals() 參數類型不匹配 | High     |
| `HE_EQUALS_NO_HASHCODE` | equals 無對應 hashCode  | High     |

### Bad Practice（不良實踐）

| Bug Pattern                  | 說明                  | 嚴重程度 |
| ---------------------------- | --------------------- | -------- |
| `OBL_UNSATISFIED_OBLIGATION` | 資源未正確關閉        | High     |
| `RCN_REDUNDANT_NULLCHECK`    | 冗餘的 null 檢查      | Medium   |
| `SE_NO_SERIALVERSIONID`      | 缺少 serialVersionUID | Medium   |

### Security（安全 - FindSecBugs）

| Bug Pattern               | 說明         | 嚴重程度 |
| ------------------------- | ------------ | -------- |
| `SQL_INJECTION`           | SQL 注入漏洞 | Critical |
| `COMMAND_INJECTION`       | 命令注入漏洞 | Critical |
| `PATH_TRAVERSAL_IN`       | 路徑遍歷漏洞 | Critical |
| `XXE_DOCUMENT`            | XXE 攻擊漏洞 | Critical |
| `XSS_REQUEST_WRAPPER`     | XSS 漏洞     | High     |
| `HARD_CODE_PASSWORD`      | 硬編碼密碼   | High     |
| `WEAK_MESSAGE_DIGEST_MD5` | 弱哈希算法   | High     |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # SpotBugs 插件配置
```

### build.gradle.kts 配置
```kotlin
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    toolVersion.set(libs.findVersion("spotbugs").get().toString())
    ignoreFailures.set(false)           // 發現 Bug 即失敗
    effort.set(com.github.spotbugs.snom.Effort.MAX)           // 最大努力
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW) // 低置信度也報告
}
```

### 添加 FindSecBugs 插件
```kotlin
dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}
```

### 排除特定規則（如需要）
創建 `config/spotbugs/exclude.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <Match>
        <Package name="~.*\.generated\..*"/>
    </Match>
    <Match>
        <Bug pattern="SE_NO_SERIALVERSIONID"/>
        <Class name="~.*DTO"/>
    </Match>
</FindBugsFilter>
```

---

## 驗證命令

```bash
# Gradle
./gradlew spotbugsMain spotbugsTest

# 查看報告
open sa-admin/build/reports/spotbugs/main.html

# Maven (如使用)
mvn spotbugs:check

# 僅檢查特定模塊
./gradlew :sa-admin:spotbugsMain
```

---

## 相關規範

- [07-owasp-top10-part1.md](./07-owasp-top10-part1.md) - 安全規範 Part 1
- [07-owasp-top10-part2.md](./07-owasp-top10-part2.md) - 安全規範 Part 2
- [08-vavr-fundamentals.md](./08-vavr-fundamentals.md) - 使用 Option 處理 null
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
