# sa-base 模組拆分計畫

## 目標
將 `sa-base` 單體模組拆分成細粒度子模組，讓其他專案可以按需引用特定功能，減少不必要的依賴。

---

## 現狀分析

### 當前 sa-base 結構（約 317 個 Java 檔案）
```
sa-base/
├── common/           # 基礎設施（12+ util 類、domain、exception、json）
├── config/           # 16 個配置類
├── module/support/   # 18 個功能模組（220+ 檔案）
└── listener/handler/ # 監聽器和處理器
```

### 問題
1. 引用 sa-base 會帶入所有依賴（Excel、S3、Mail、模板引擎等）
2. 無法選擇性引用特定功能
3. 啟動時間和記憶體佔用較大

---

## 拆分方案

### 模組層級設計

```
┌─────────────────────────────────────────────────────────────┐
│                    Tier 1: Core (必選)                       │
├─────────────────────────────────────────────────────────────┤
│  sa-base:core         基礎 domain、exception、enum、util     │
│  sa-base:mybatis-plus SmartPageUtil、MyBatis 配置           │
│  sa-base:web          JSON 序列化、CORS、AsyncConfig         │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Tier 2: Infrastructure (常用)                │
├─────────────────────────────────────────────────────────────┤
│  sa-base:swagger      Knife4j、Swagger 配置                  │
│  sa-base:token        Sa-Token 整合                         │
│  sa-base:redis        Redisson、Redis 配置                   │
│  sa-base:datasource   PostgreSQL、HikariCP、P6Spy           │
│  sa-base:ip-region    IP 地理位置解析                        │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Tier 3: Features (可選)                     │
├─────────────────────────────────────────────────────────────┤
│  sa-base:support-file          檔案上傳（S3 可選）           │
│  sa-base:support-dict          資料字典 + 快取               │
│  sa-base:support-job           定時任務調度                  │
│  sa-base:support-datatracer    資料變更追蹤                  │
│  sa-base:support-serialnumber  序號產生器                    │
│  sa-base:support-reload        動態配置重載                  │
│  sa-base:support-config        系統配置管理                  │
│  sa-base:support-helpdoc       幫助文檔                      │
│  sa-base:support-message       站內訊息                      │
│  sa-base:support-changelog     更新日誌                      │
│  sa-base:support-feedback      意見回饋                      │
│  sa-base:support-operatelog    操作日誌                      │
│  sa-base:support-loginlog      登入日誌                      │
│  sa-base:support-table         表格元數據                    │
│  sa-base:support-heartbeat     心跳監控                      │
│  sa-base:support-mail          郵件服務                      │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Tier 4: DevTools (開發工具)                  │
├─────────────────────────────────────────────────────────────┤
│  sa-base:support-codegenerator  程式碼產生器（Velocity）     │
│  sa-base:support-excel          Excel 匯入匯出               │
└─────────────────────────────────────────────────────────────┘
```

---

## 模組依賴圖

```
sa-base:core (無依賴)
    │
    ├── sa-base:mybatis-plus
    │       └── sa-base:support-* (大部分)
    │
    ├── sa-base:web
    │       └── sa-base:swagger
    │
    ├── sa-base:token
    ├── sa-base:redis
    ├── sa-base:datasource
    └── sa-base:ip-region
            └── sa-base:support-datatracer
```

---

## 目錄結構

```
sa-base/
├── build.gradle.kts              # 聚合模組（向後相容）
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/net/lab1024/sa/base/core/
│       ├── domain/               # ResponseDTO, PageParam, PageResult
│       ├── code/                 # ErrorCode, SystemErrorCode
│       ├── exception/            # BusinessException
│       ├── enumeration/          # BaseEnum, GenderEnum
│       ├── annotation/           # @NoNeedLogin
│       ├── constant/             # StringConst
│       ├── util/                 # SmartBeanUtil, SmartEnumUtil
│       └── validator/            # CheckEnum, EnumValidator
├── mybatis-plus/
│   └── src/.../mybatisplus/
│       ├── util/                 # SmartPageUtil
│       └── config/               # MybatisPlusConfig
├── web/
│   └── src/.../web/
│       ├── json/                 # 所有序列化器
│       ├── util/                 # SmartRequestUtil
│       └── config/               # JsonConfig, CorsConfig
├── support-file/
│   └── src/.../support/file/     # 完整 file 模組
├── support-dict/
│   └── src/.../support/dict/     # 完整 dict 模組
└── ... (其他 support-* 模組)
```

---

## Gradle 配置範例

### settings.gradle.kts
```kotlin
include(
    "sa-base",
    "sa-base:core",
    "sa-base:mybatis-plus",
    "sa-base:web",
    "sa-base:swagger",
    "sa-base:token",
    "sa-base:redis",
    "sa-base:datasource",
    "sa-base:ip-region",
    "sa-base:support-file",
    "sa-base:support-dict",
    // ... 其他模組
)
```

### sa-base:core/build.gradle.kts
```kotlin
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    api(libs.spring.boot.starter.validation)
    api(libs.commons.lang3)
    api(libs.guava)
    api(libs.lombok)
    annotationProcessor(libs.lombok)
}
```

### sa-base:support-dict/build.gradle.kts
```kotlin
dependencies {
    api(project(":sa-base:core"))
    api(project(":sa-base:mybatis-plus"))
    api(project(":sa-base:web"))
    api(project(":sa-common:cache"))  // JetCache
}
```

### sa-base/build.gradle.kts（向後相容聚合器）
```kotlin
dependencies {
    // 匯出所有子模組，保持向後相容
    api(project(":sa-base:core"))
    api(project(":sa-base:mybatis-plus"))
    api(project(":sa-base:web"))
    api(project(":sa-base:swagger"))
    api(project(":sa-base:support-file"))
    api(project(":sa-base:support-dict"))
    // ... 其他所有模組
}
```

---

## AutoConfiguration 模式

每個模組需要：
```
src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

內容範例：
```
net.lab1024.sa.base.support.dict.config.DictAutoConfiguration
```

---

## 使用範例

### 完整引用（向後相容）
```kotlin
// 與現有行為相同
implementation(project(":sa-base"))
```

### 最小化引用
```kotlin
// 只需要核心功能
implementation(project(":sa-base:core"))
implementation(project(":sa-base:mybatis-plus"))
```

### 選擇性功能
```kotlin
// 核心 + 字典 + 檔案上傳
implementation(project(":sa-base:core"))
implementation(project(":sa-base:mybatis-plus"))
implementation(project(":sa-base:web"))
implementation(project(":sa-base:support-dict"))
implementation(project(":sa-base:support-file"))
```

---

## 驗證方式

### 1. 編譯驗證
```bash
./gradlew clean build -x test
```

### 2. 單元測試
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

### 3. 依賴檢查
```bash
# 檢查各模組依賴樹
./gradlew :sa-base:core:dependencies --configuration compileClasspath
./gradlew :sa-base:support-dict:dependencies --configuration compileClasspath
```

### 4. 循環依賴檢測
```bash
# Gradle 會自動檢測並報錯
./gradlew build
```

### 5. 新增 ArchUnit 模組邊界測試
```java
// ModuleBoundaryTest.java
@ArchTest
static final ArchRule coreModuleShouldNotDependOnSupportModules =
    noClasses()
        .that().resideInAPackage("net.lab1024.sa.base.core..")
        .should().dependOnClassesThat()
        .resideInAPackage("net.lab1024.sa.base.support..");

@ArchTest
static final ArchRule supportModulesShouldNotCrossDepend =
    slices()
        .matching("net.lab1024.sa.base.support.(*)..")
        .should().notDependOnEachOther();
```

### 6. 整合測試
```bash
# 啟動應用驗證
./gradlew :sa-admin:bootRun

# 訪問 Swagger 確認功能正常
# http://localhost:1024/swagger-ui.html
```

---

## 決策記錄

| 決策項 | 選擇 |
|--------|------|
| sa-common:core 處理 | 合併到 sa-base:core，sa-common:core 作為 alias |
| 命名風格 | support-file（與 sa-common 風格一致） |
| minimal 聚合模組 | 不需要，使用者自行組合 |

---

## 實施階段

### Phase 1: Core 提取與合併
1. 建立 `sa-base:core` 模組
2. 移動 sa-base 的 domain、exception、enum、基礎 util
3. **合併 sa-common:core 內容到 sa-base:core**
4. 將 sa-common:core 改為 alias（僅依賴 sa-base:core）

### Phase 2: Infrastructure 提取
1. 建立 `sa-base:mybatis-plus`、`sa-base:web`、`sa-base:swagger`
2. 建立 `sa-base:redis`、`sa-base:token`、`sa-base:datasource`
3. 移動對應配置類

### Phase 3: Support 模組提取
1. 從獨立模組開始：mail、changelog、feedback
2. 提取有單一依賴的：loginlog、operatelog
3. 提取有複雜依賴的：file、dict、datatracer

### Phase 4: DevTools 提取
1. 建立 `sa-base:support-codegenerator`
2. 建立 `sa-base:support-excel`

### Phase 5: 聚合器 + 清理
1. 保持 `sa-base` 作為聚合器（向後相容）
2. 更新所有文檔
3. 執行完整驗證

---

## 關鍵檔案

| 檔案 | 用途 |
|------|------|
| settings.gradle.kts | 新增所有子模組 |
| sa-base/build.gradle.kts | 改為聚合器 |
| ArchitectureTest.java | 新增模組邊界規則 |

---

## 風險與注意事項

1. **向後相容**：保持 `sa-base` 聚合器，現有 `implementation(project(":sa-base"))` 繼續有效
2. **循環依賴**：support 模組之間禁止互相依賴
3. **AutoConfiguration 順序**：需要正確設定 `@AutoConfigureAfter`
4. **測試覆蓋**：每個模組需要獨立的單元測試
