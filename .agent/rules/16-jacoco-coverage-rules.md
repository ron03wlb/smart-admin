# JaCoCo 測試覆蓋率規範

**TL;DR**: JaCoCo 測量測試覆蓋率，強制要求行覆蓋率 ≥80%、分支覆蓋率 ≥70%。新代碼必須有對應測試。

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ 創建新的 Service/Manager 類時
- ✅ `./gradlew jacocoTestCoverageVerification` 失敗時
- ✅ Code Review 時檢查測試覆蓋
- ✅ 用戶詢問測試覆蓋率問題

### 強制執行檢查清單
- [ ] 行覆蓋率 ≥ 80%
- [ ] 分支覆蓋率 ≥ 70%
- [ ] 新代碼有對應單元測試
- [ ] 異常分支有測試覆蓋
- [ ] Service 層方法全覆蓋

### AI 決策樹
```
覆蓋率不足 → 識別未覆蓋區域
  ├─ Service 方法未測試
  │   └─ 創建對應單元測試
  ├─ 異常分支未覆蓋
  │   └─ 添加異常場景測試
  ├─ 條件分支未覆蓋
  │   └─ 添加邊界條件測試
  └─ 不需要測試的代碼
      └─ 添加到排除配置
```

### 測試模板

#### Service 單元測試模板
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // Given
        User user = User.builder().id(1L).name("Test").build();
        when(userMapper.selectById(1L)).thenReturn(user);

        // When
        Option<User> result = userService.findById(1L);

        // Then
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().getName()).isEqualTo("Test");
    }

    @Test
    void findById_shouldReturnNone_whenUserNotExists() {
        // Given
        when(userMapper.selectById(1L)).thenReturn(null);

        // When
        Option<User> result = userService.findById(1L);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void findById_shouldThrow_whenIdIsNull() {
        // Then
        assertThatThrownBy(() -> userService.findById(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

## 【強制】覆蓋率標準

| 指標                | 最低要求 | 目標  |
| ------------------- | -------- | ----- |
| 行覆蓋率 (Line)     | ≥ 80%    | ≥ 85% |
| 分支覆蓋率 (Branch) | ≥ 70%    | ≥ 75% |
| 方法覆蓋率 (Method) | ≥ 75%    | ≥ 80% |
| 類覆蓋率 (Class)    | ≥ 90%    | ≥ 95% |

### 分層覆蓋率要求

| 層級          | 行覆蓋率 | 說明                |
| ------------- | -------- | ------------------- |
| Service 層    | ≥ 85%    | 核心業務邏輯        |
| Manager 層    | ≥ 80%    | 事務/緩存邏輯       |
| Controller 層 | ≥ 60%    | HTTP 處理（可較低） |
| DTO/VO/Entity | 排除     | 純數據類            |
| Config        | 排除     | 配置類              |

---

## 配置說明

### Gradle 配置位置
```
smart-admin-api-java21-springboot3/
└── build.gradle.kts          # JaCoCo 插件配置
```

### build.gradle.kts 配置
```kotlin
subprojects {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)   // SonarQube 需要
            html.required.set(true)  // 人工查看
        }
    }

    tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn("test")
        
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn("jacocoTestCoverageVerification")
    }
}
```

### 排除配置
```kotlin
tasks.withType<JacocoReport> {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/domain/**",      // Entity
                    "**/dto/**",         // DTO
                    "**/vo/**",          // VO
                    "**/config/**",      // 配置類
                    "**/constant/**",    // 常量類
                    "**/*Application*",  // 啟動類
                    "**/*Config*"        // 配置類
                )
            }
        }))
    }
}
```

---

## 報告解讀

### 報告位置
```
sa-admin/build/reports/jacoco/test/html/index.html
```

### 顏色含義
- 🟢 **綠色**: 已覆蓋
- 🔴 **紅色**: 未覆蓋
- 🟡 **黃色**: 部分覆蓋（分支）

### 常見問題

#### 分支覆蓋不足
```java
// 原代碼 - 只測試了 true 分支
public String getStatus(boolean active) {
    if (active) {        // 🟡 部分覆蓋
        return "ACTIVE";
    }
    return "INACTIVE";   // 🔴 未覆蓋
}

// 補充測試
@Test
void getStatus_shouldReturnInactive_whenNotActive() {
    assertThat(service.getStatus(false)).isEqualTo("INACTIVE");
}
```

#### 異常分支未覆蓋
```java
// 原代碼
public User findById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {       // 🔴 異常分支未覆蓋
        throw new NotFoundException("用戶不存在");
    }
    return user;
}

// 補充測試
@Test
void findById_shouldThrow_whenUserNotFound() {
    when(userMapper.selectById(1L)).thenReturn(null);
    
    assertThatThrownBy(() -> service.findById(1L))
        .isInstanceOf(NotFoundException.class);
}
```

---

## 驗證命令

```bash
# 運行測試並生成覆蓋率報告
./gradlew test jacocoTestReport

# 查看覆蓋率報告
open sa-admin/build/reports/jacoco/test/html/index.html

# 驗證覆蓋率門檻
./gradlew jacocoTestCoverageVerification

# 完整驗證
./gradlew check

# Maven (如使用)
mvn clean test jacoco:report
mvn jacoco:check -Djacoco.minimum=0.80
```

---

## SonarQube 整合

JaCoCo 報告可以上傳到 SonarQube：

```kotlin
sonarqube {
    properties {
        property("sonar.coverage.jacoco.xmlReportPaths", 
            "${buildDir}/reports/jacoco/test/jacocoTestReport.xml")
    }
}
```

---

## 相關規範

- [06-sonarqube-rules.md](./06-sonarqube-rules.md) - SonarQube 質量門禁
- [workflows/tdd-workflow.md](../workflows/tdd-workflow.md) - TDD 工作流程
- [workflows/quality-gates-local-ci.md](../workflows/quality-gates-local-ci.md) - 質量門禁流程
