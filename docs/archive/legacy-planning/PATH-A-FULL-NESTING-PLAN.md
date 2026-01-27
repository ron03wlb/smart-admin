# 路徑 A：完全嵌套實施計畫（合併 sa-common）

**日期：** 2026-01-21
**決策者：** 用戶確認
**預估工作量：** 1-2 週
**風險等級：** 🔴 高（破壞性變更）

---

## 執行摘要

將 SmartAdmin 從混合結構（12 個根目錄）重構為完全嵌套結構（2 個根目錄），實現與 RuoYi-Vue-Plus 對齊的清晰架構。

**關鍵變更：**
- sa-common 合併到 sa-base/foundation/
- sa-base-* 移動到 sa-base/infrastructure/
- sa-base-support 移動到 sa-base/support/

**最終結構：**
```
smart-admin-api-java21-springboot3/
├── sa-base/                     # 唯一的基礎設施父模塊
│   ├── foundation/              # Layer 0（原 sa-common）
│   ├── infrastructure/          # Layer 1（原 sa-base-*）
│   └── support/                 # Layer 2（原 sa-base-support）
└── sa-admin/                    # 應用層
```

---

## 目標架構

### 完整目錄結構

```
smart-admin-api-java21-springboot3/
├── sa-base/
│   ├── build.gradle.kts                    # 聚合器
│   │
│   ├── foundation/                         # Layer 0: 跨領域關注點（原 sa-common）
│   │   ├── core/                           # :sa-base:foundation:core
│   │   ├── mq/                             # :sa-base:foundation:mq
│   │   ├── cache/                          # :sa-base:foundation:cache
│   │   ├── redis-lock/                     # :sa-base:foundation:redis-lock
│   │   ├── api-encrypt/                    # :sa-base:foundation:api-encrypt
│   │   ├── captcha/                        # :sa-base:foundation:captcha
│   │   ├── repeat-submit/                  # :sa-base:foundation:repeat-submit
│   │   ├── data-masking/                   # :sa-base:foundation:data-masking
│   │   └── security-protect/               # :sa-base:foundation:security-protect
│   │
│   ├── infrastructure/                     # Layer 1: 基礎設施（原 sa-base-*）
│   │   ├── core/                           # :sa-base:infrastructure:core
│   │   ├── web/                            # :sa-base:infrastructure:web
│   │   ├── mybatis/                        # :sa-base:infrastructure:mybatis
│   │   ├── redis/                          # :sa-base:infrastructure:redis
│   │   ├── token/                          # :sa-base:infrastructure:token
│   │   ├── datasource/                     # :sa-base:infrastructure:datasource
│   │   ├── swagger/                        # :sa-base:infrastructure:swagger
│   │   └── devtools/                       # :sa-base:infrastructure:devtools
│   │
│   └── support/                            # Layer 2: 業務支持（原 sa-base-support）
│       ├── config/                         # :sa-base:support:config
│       ├── dict/                           # :sa-base:support:dict
│       ├── reload/                         # :sa-base:support:reload
│       ├── file/                           # :sa-base:support:file
│       ├── helpdoc/                        # :sa-base:support:helpdoc
│       ├── job/                            # :sa-base:support:job
│       ├── heartbeat/                      # :sa-base:support:heartbeat
│       ├── loginlog/                       # :sa-base:support:loginlog
│       ├── operatelog/                     # :sa-base:support:operatelog
│       ├── datatracer/                     # :sa-base:support:datatracer
│       ├── feedback/                       # :sa-base:support:feedback
│       ├── message/                        # :sa-base:support:message
│       ├── changelog/                      # :sa-base:support:changelog
│       ├── table/                          # :sa-base:support:table
│       ├── mail/                           # :sa-base:support:mail
│       ├── serialnumber/                   # :sa-base:support:serialnumber
│       └── codegenerator/                  # :sa-base:support:codegenerator
│
└── sa-admin/                                # Application Layer
```

### 依賴關係（Gradle 兼容）

```gradle
// Layer 0: Foundation（無 project 依賴）
// sa-base/foundation/core/build.gradle.kts
dependencies {
    // ✅ 只有外部庫，無 project() 依賴
    api(libs.spring.boot.autoconfigure)
    api(libs.jackson.databind)
}

// Layer 1: Infrastructure（依賴 Layer 0）
// sa-base/infrastructure/core/build.gradle.kts
dependencies {
    api(project(":sa-base:foundation:core"))  // ✅ 同父模塊，Gradle OK
}

// sa-base/infrastructure/web/build.gradle.kts
dependencies {
    api(project(":sa-base:foundation:core"))  // ✅ 同父模塊，Gradle OK
}

// Layer 2: Support（依賴 Layer 0 + Layer 1）
// sa-base/support/dict/build.gradle.kts
dependencies {
    api(project(":sa-base:foundation:core"))         // ✅ 同父模塊，Gradle OK
    api(project(":sa-base:infrastructure:mybatis"))  // ✅ 同父模塊，Gradle OK
    api(project(":sa-base:infrastructure:web"))      // ✅ 同父模塊，Gradle OK
}
```

**關鍵：** 所有模塊都在同一父模塊 `sa-base` 下，所有依賴都是同父依賴，Gradle 完全兼容 ✅

---

## 實施計畫：五階段

### 📋 Phase 1: 準備工作（1-2 小時）

#### 1.1 創建備份與分支

```bash
cd smart-admin-api-java21-springboot3

# 確保當前狀態乾淨
git status

# 提交任何未提交的更改
git add .
git commit -m "chore: backup before major refactoring (Path A)"

# 創建備份分支
git branch backup/before-path-a-refactoring

# 創建工作分支
git checkout -b feature/ron/path-a-full-nesting
git push -u origin feature/ron/path-a-full-nesting
```

#### 1.2 記錄當前依賴關係

```bash
# 生成依賴快照（用於後續對比驗證）
./gradlew :sa-admin:dependencies --configuration runtimeClasspath > dependencies-before-path-a.txt
```

#### 1.3 統計影響範圍

```bash
# 統計所有 sa-common 引用
grep -r "project(\":sa-common:" --include="*.gradle.kts" | wc -l

# 預期：100+ 處引用需要更新
```

---

### 🏗️ Phase 2: 目錄重組（2-3 小時）

#### 2.1 創建新的目錄結構

```bash
# 創建 Layer 0: Foundation
mkdir -p sa-base/foundation

# 創建 Layer 1: Infrastructure
mkdir -p sa-base/infrastructure

# 創建 Layer 2: Support（已存在 sa-base/support，稍後移動）
```

#### 2.2 移動 sa-common 到 sa-base/foundation/

```bash
# 移動所有 sa-common 子模塊
git mv sa-common/core sa-base/foundation/core
git mv sa-common/mq sa-base/foundation/mq
git mv sa-common/cache sa-base/foundation/cache
git mv sa-common/redis-lock sa-base/foundation/redis-lock
git mv sa-common/api-encrypt sa-base/foundation/api-encrypt
git mv sa-common/captcha sa-base/foundation/captcha
git mv sa-common/repeat-submit sa-base/foundation/repeat-submit
git mv sa-common/data-masking sa-base/foundation/data-masking
git mv sa-common/security-protect sa-base/foundation/security-protect

# 保留 sa-common/build.gradle.kts（稍後移動或刪除）
# 暫時保留 sa-common/ 目錄作為父目錄（將在 Phase 2.4 處理）
```

#### 2.3 移動 sa-base-* 到 sa-base/infrastructure/

```bash
# 移動所有基礎設施模塊
git mv sa-base-core sa-base/infrastructure/core
git mv sa-base-web sa-base/infrastructure/web
git mv sa-base-mybatis sa-base/infrastructure/mybatis
git mv sa-base-redis sa-base/infrastructure/redis
git mv sa-base-token sa-base/infrastructure/token
git mv sa-base-datasource sa-base/infrastructure/datasource
git mv sa-base-swagger sa-base/infrastructure/swagger
git mv sa-base-devtools sa-base/infrastructure/devtools
```

#### 2.4 移動 sa-base-support 到 sa-base/support/

```bash
# 移動支持模塊組（如果當前在根目錄）
# 如果已經在 sa-base/support/，則跳過此步驟

# 檢查當前位置
ls sa-base/support/

# 如果在根目錄 sa-base-support/，則執行：
# git mv sa-base-support/* sa-base/support/
```

#### 2.5 處理 sa-common 父目錄

```bash
# 選項 1：刪除 sa-common/ 目錄（推薦）
rm -rf sa-common/

# 選項 2：保留作為過渡說明文件
echo "# sa-common has been merged into sa-base/foundation/
See docs/PATH-A-FULL-NESTING-PLAN.md for details." > sa-common/README.md
```

#### 2.6 驗證目錄結構

```bash
# 檢查最終結構
ls -la
# 預期：只有 sa-base/ 和 sa-admin/

# 檢查 sa-base 內部
ls -la sa-base/
# 預期：foundation/, infrastructure/, support/, build.gradle.kts

# 檢查各層子模塊數量
ls sa-base/foundation/ | wc -l        # 預期：9 個
ls sa-base/infrastructure/ | wc -l    # 預期：8 個
ls sa-base/support/ | wc -l           # 預期：17 個
```

---

### ⚙️ Phase 3: 配置更新（4-6 小時）

#### 3.1 更新 settings.gradle.kts

備份並完全替換 include 區塊：

```kotlin
rootProject.name = "sa-parent"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    // === Layer 0: Foundation - Cross-cutting concerns ===
    "sa-base",
    "sa-base:foundation:core",
    "sa-base:foundation:mq",
    "sa-base:foundation:cache",
    "sa-base:foundation:redis-lock",
    "sa-base:foundation:api-encrypt",
    "sa-base:foundation:captcha",
    "sa-base:foundation:repeat-submit",
    "sa-base:foundation:data-masking",
    "sa-base:foundation:security-protect",

    // === Layer 1: Infrastructure ===
    "sa-base:infrastructure:core",
    "sa-base:infrastructure:web",
    "sa-base:infrastructure:mybatis",
    "sa-base:infrastructure:redis",
    "sa-base:infrastructure:token",
    "sa-base:infrastructure:datasource",
    "sa-base:infrastructure:swagger",
    "sa-base:infrastructure:devtools",

    // === Layer 2: Business Support ===
    "sa-base:support:config",
    "sa-base:support:dict",
    "sa-base:support:reload",
    "sa-base:support:file",
    "sa-base:support:helpdoc",
    "sa-base:support:job",
    "sa-base:support:heartbeat",
    "sa-base:support:loginlog",
    "sa-base:support:operatelog",
    "sa-base:support:datatracer",
    "sa-base:support:feedback",
    "sa-base:support:message",
    "sa-base:support:changelog",
    "sa-base:support:table",
    "sa-base:support:mail",
    "sa-base:support:serialnumber",
    "sa-base:support:codegenerator",

    // === Application Layer ===
    "sa-admin"
)
```

#### 3.2 更新 sa-base/build.gradle.kts（聚合器）

完全重寫依賴區塊：

```kotlin
plugins {
    `java-library`
    id("io.spring.dependency-management")
}

// ... 保留現有 configurations 和環境配置 ...

dependencies {
    // ========== Layer 0: Foundation ==========
    api(project(":sa-base:foundation:core"))
    api(project(":sa-base:foundation:mq"))
    api(project(":sa-base:foundation:cache"))
    api(project(":sa-base:foundation:redis-lock"))
    api(project(":sa-base:foundation:api-encrypt"))
    api(project(":sa-base:foundation:captcha"))
    api(project(":sa-base:foundation:repeat-submit"))
    api(project(":sa-base:foundation:data-masking"))
    api(project(":sa-base:foundation:security-protect"))

    // ========== Layer 1: Infrastructure ==========
    api(project(":sa-base:infrastructure:core"))
    api(project(":sa-base:infrastructure:web"))
    api(project(":sa-base:infrastructure:mybatis"))
    api(project(":sa-base:infrastructure:redis"))
    api(project(":sa-base:infrastructure:token"))
    api(project(":sa-base:infrastructure:datasource"))
    api(project(":sa-base:infrastructure:swagger"))
    api(project(":sa-base:infrastructure:devtools"))

    // ========== Layer 2: Business Support ==========
    api(project(":sa-base:support:config"))
    api(project(":sa-base:support:dict"))
    api(project(":sa-base:support:reload"))
    api(project(":sa-base:support:file"))
    api(project(":sa-base:support:helpdoc"))
    api(project(":sa-base:support:job"))
    api(project(":sa-base:support:heartbeat"))
    api(project(":sa-base:support:loginlog"))
    api(project(":sa-base:support:operatelog"))
    api(project(":sa-base:support:datatracer"))
    api(project(":sa-base:support:feedback"))
    api(project(":sa-base:support:message"))
    api(project(":sa-base:support:changelog"))
    api(project(":sa-base:support:table"))
    api(project(":sa-base:support:mail"))
    api(project(":sa-base:support:serialnumber"))
    api(project(":sa-base:support:codegenerator"))

    // ... 保留現有的外部庫依賴 ...
}

// ... 保留現有的 tasks 配置 ...
```

#### 3.3 批量更新子模塊的 build.gradle.kts

**批量替換腳本（Windows PowerShell）：**

```powershell
# 替換所有 sa-common 引用
Get-ChildItem -Path "sa-base\" -Recurse -Filter "build.gradle.kts" | ForEach-Object {
    (Get-Content $_.FullName) `
        -replace ':sa-common:core', ':sa-base:foundation:core' `
        -replace ':sa-common:mq', ':sa-base:foundation:mq' `
        -replace ':sa-common:cache', ':sa-base:foundation:cache' `
        -replace ':sa-common:redis-lock', ':sa-base:foundation:redis-lock' `
        -replace ':sa-common:api-encrypt', ':sa-base:foundation:api-encrypt' `
        -replace ':sa-common:captcha', ':sa-base:foundation:captcha' `
        -replace ':sa-common:repeat-submit', ':sa-base:foundation:repeat-submit' `
        -replace ':sa-common:data-masking', ':sa-base:foundation:data-masking' `
        -replace ':sa-common:security-protect', ':sa-base:foundation:security-protect' |
    Set-Content $_.FullName
}

# 替換所有 sa-base-* 引用
Get-ChildItem -Path "sa-base\" -Recurse -Filter "build.gradle.kts" | ForEach-Object {
    (Get-Content $_.FullName) `
        -replace ':sa-base-core', ':sa-base:infrastructure:core' `
        -replace ':sa-base-web', ':sa-base:infrastructure:web' `
        -replace ':sa-base-mybatis', ':sa-base:infrastructure:mybatis' `
        -replace ':sa-base-redis', ':sa-base:infrastructure:redis' `
        -replace ':sa-base-token', ':sa-base:infrastructure:token' `
        -replace ':sa-base-datasource', ':sa-base:infrastructure:datasource' `
        -replace ':sa-base-swagger', ':sa-base:infrastructure:swagger' `
        -replace ':sa-base-devtools', ':sa-base:infrastructure:devtools' |
    Set-Content $_.FullName
}

# 替換所有 sa-base-support 引用
Get-ChildItem -Path "sa-base\" -Recurse -Filter "build.gradle.kts" | ForEach-Object {
    (Get-Content $_.FullName) `
        -replace ':sa-base-support:', ':sa-base:support:' |
    Set-Content $_.FullName
}
```

**Git Bash / Linux（替代）：**

```bash
# 替換 sa-common 引用
find sa-base/ -name "build.gradle.kts" -type f -exec sed -i '
    s/:sa-common:core/:sa-base:foundation:core/g;
    s/:sa-common:mq/:sa-base:foundation:mq/g;
    s/:sa-common:cache/:sa-base:foundation:cache/g;
    s/:sa-common:redis-lock/:sa-base:foundation:redis-lock/g;
    s/:sa-common:api-encrypt/:sa-base:foundation:api-encrypt/g;
    s/:sa-common:captcha/:sa-base:foundation:captcha/g;
    s/:sa-common:repeat-submit/:sa-base:foundation:repeat-submit/g;
    s/:sa-common:data-masking/:sa-base:foundation:data-masking/g;
    s/:sa-common:security-protect/:sa-base:foundation:security-protect/g;
' {} \;

# 替換 sa-base-* 引用
find sa-base/ -name "build.gradle.kts" -type f -exec sed -i '
    s/:sa-base-core/:sa-base:infrastructure:core/g;
    s/:sa-base-web/:sa-base:infrastructure:web/g;
    s/:sa-base-mybatis/:sa-base:infrastructure:mybatis/g;
    s/:sa-base-redis/:sa-base:infrastructure:redis/g;
    s/:sa-base-token/:sa-base:infrastructure:token/g;
    s/:sa-base-datasource/:sa-base:infrastructure:datasource/g;
    s/:sa-base-swagger/:sa-base:infrastructure:swagger/g;
    s/:sa-base-devtools/:sa-base:infrastructure:devtools/g;
    s/:sa-base-support:/:sa-base:support:/g;
' {} \;
```

#### 3.4 更新 sa-admin/build.gradle.kts

```bash
# 打開 sa-admin/build.gradle.kts
# 手動檢查所有 sa-common 和 sa-base 引用並更新

# 如果直接依賴了 sa-common:*，需要更新為 sa-base:foundation:*
# 如果依賴了 sa-base-*，需要更新為 sa-base:infrastructure:*
# 如果依賴了 :sa-base，保持不變（向後兼容）
```

---

### ✅ Phase 4: 構建驗證（2-3 小時）

#### 4.1 清理並重新構建

```bash
# 清理所有構建產物和緩存
./gradlew clean
rm -rf .gradle/
rm -rf */build/
rm -rf */*/build/
rm -rf */*/*/build/

# 重新構建（不執行測試）
./gradlew build -x test

# 預期結果：BUILD SUCCESSFUL
# 如果失敗：檢查錯誤信息，通常會指出哪個模塊找不到依賴
```

#### 4.2 驗證依賴解析

```bash
# 生成新的依賴樹
./gradlew :sa-admin:dependencies --configuration runtimeClasspath > dependencies-after-path-a.txt

# 對比前後依賴
diff dependencies-before-path-a.txt dependencies-after-path-a.txt

# 預期：依賴的類應該相同，只是路徑變化
# Before: sa-common:core, sa-base-core
# After:  sa-base:foundation:core, sa-base:infrastructure:core
```

#### 4.3 執行單元測試

```bash
# 執行 sa-admin 的所有測試
./gradlew :sa-admin:test

# 特別關注架構測試
./gradlew :sa-admin:test --tests ArchitectureTest

# 預期結果：所有測試通過
# 如果 ArchUnit 測試失敗：檢查測試規則是否硬編碼了 Gradle 模塊名稱
```

#### 4.4 啟動應用驗證

```bash
# 啟動應用
./gradlew :sa-admin:bootRun

# 等待啟動完成，訪問：
# http://localhost:1024/swagger-ui.html

# 檢查：
# - Swagger UI 是否正常顯示所有 API
# - 嘗試調用 2-3 個 API 驗證功能
# - 檢查日誌是否有錯誤
```

---

### 📝 Phase 5: 文檔更新與提交（1-2 小時）

#### 5.1 更新 CLAUDE.md

```markdown
## 模塊結構（全新：完全嵌套架構）

SmartAdmin v3.0.0 採用完全嵌套的模塊化架構，受 RuoYi-Vue-Plus 啟發。

### 根目錄結構

```
smart-admin-api-java21-springboot3/
├── sa-base/              # 統一的基礎設施父模塊
└── sa-admin/             # 業務應用層
```

### 三層架構

#### Layer 0: Foundation（基礎層）
- `sa-base:foundation:*` - 跨領域關注點（原 sa-common）
- 子模塊：core, cache, redis-lock, api-encrypt 等 9 個
- **特點：** 無 project 依賴，只有外部庫

#### Layer 1: Infrastructure（基礎設施層）
- `sa-base:infrastructure:*` - 基礎設施配置（原 sa-base-*）
- 子模塊：core, web, mybatis, redis, token, datasource, swagger, devtools
- **特點：** 依賴 Layer 0

#### Layer 2: Business Support（業務支持層）
- `sa-base:support:*` - 業務支持模塊（原 sa-base-support）
- 子模塊：config, dict, file, job, message 等 17 個
- **特點：** 依賴 Layer 0 + Layer 1

### 引用方式

**完整引用（推薦，向後兼容）：**
```kotlin
implementation(project(":sa-base"))  // 包含所有層的所有模塊
```

**精確引用：**
```kotlin
implementation(project(":sa-base:foundation:core"))
implementation(project(":sa-base:infrastructure:mybatis"))
implementation(project(":sa-base:support:dict"))
```

### 重大變更說明

**v3.0.0 Breaking Change：**
- `sa-common:*` 重命名為 `sa-base:foundation:*`
- `sa-base-*` 重命名為 `sa-base:infrastructure:*`
- `sa-base-support:*` 重命名為 `sa-base:support:*`

**遷移指南：** 查找替換所有 `project(":sa-common:XXX")` 為 `project(":sa-base:foundation:XXX")`
```

#### 5.2 創建遷移指南文檔

創建 `docs/MIGRATION-TO-V3.md`：

```markdown
# SmartAdmin v3.0.0 遷移指南

## 模塊路徑變更對照表

| v2.x 路徑 | v3.0.0 路徑 | 說明 |
|----------|-------------|------|
| `:sa-common:core` | `:sa-base:foundation:core` | Foundation layer |
| `:sa-common:cache` | `:sa-base:foundation:cache` | Foundation layer |
| `:sa-base-core` | `:sa-base:infrastructure:core` | Infrastructure layer |
| `:sa-base-web` | `:sa-base:infrastructure:web` | Infrastructure layer |
| `:sa-base-support:dict` | `:sa-base:support:dict` | Support layer |

## 自動遷移腳本

```powershell
# Windows PowerShell
Get-ChildItem -Recurse -Filter "*.gradle.kts" | ForEach-Object {
    (Get-Content $_.FullName) `
        -replace ':sa-common:', ':sa-base:foundation:' `
        -replace ':sa-base-([^:]+)"', ':sa-base:infrastructure:$1"' `
        -replace ':sa-base-support:', ':sa-base:support:' |
    Set-Content $_.FullName
}
```

## 驗證步驟

1. 執行自動遷移腳本
2. 清理構建緩存：`./gradlew clean`
3. 重新構建：`./gradlew build`
4. 運行測試：`./gradlew test`
```

#### 5.3 提交更改

```bash
# 添加所有更改
git add .

# 提交（遵循 Conventional Commits）
git commit -m "refactor(architecture): Path A - Full nesting with sa-common merge

BREAKING CHANGE: Module paths completely restructured

Major changes:
- Merge sa-common into sa-base/foundation/ (9 modules)
- Move sa-base-* to sa-base/infrastructure/ (8 modules)
- Move sa-base-support to sa-base/support/ (17 modules)
- Root directory cleaned: 12 → 2 directories

Module path mapping:
- :sa-common:* → :sa-base:foundation:*
- :sa-base-* → :sa-base:infrastructure:*
- :sa-base-support:* → :sa-base:support:*

Architecture alignment:
- Inspired by RuoYi-Vue-Plus three-layer architecture
- All modules under single parent (sa-base) for Gradle compatibility
- Clear layer boundaries: Foundation → Infrastructure → Support

Migration:
- See docs/MIGRATION-TO-V3.md for migration guide
- Backward compatibility maintained via :sa-base aggregator

Verified:
- ✅ Gradle build successful
- ✅ All unit tests passed
- ✅ ArchUnit tests passed
- ✅ Application starts normally
- ✅ Swagger UI accessible

Estimated effort: 1-2 weeks
Risk level: High (breaking change)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

# 推送到遠端
git push origin feature/ron/path-a-full-nesting
```

---

## 成功標準

### Phase 2 成功標準（目錄重組）
- ✅ 根目錄只有 2 個目錄：`sa-base/`, `sa-admin/`
- ✅ `sa-base/` 包含 3 個子目錄：`foundation/`, `infrastructure/`, `support/`
- ✅ Foundation 包含 9 個模塊
- ✅ Infrastructure 包含 8 個模塊
- ✅ Support 包含 17 個模塊

### Phase 3 成功標準（配置更新）
- ✅ `settings.gradle.kts` 聲明所有 34 個模塊（9 + 8 + 17）
- ✅ 所有模塊路徑使用 `:sa-base:*:*` 格式
- ✅ `sa-base/build.gradle.kts` 正確 `api()` 所有子模塊

### Phase 4 成功標準（構建驗證）
- ✅ `./gradlew build` 成功
- ✅ `./gradlew :sa-admin:test` 所有測試通過
- ✅ `./gradlew :sa-admin:test --tests ArchitectureTest` 通過
- ✅ 應用正常啟動：`./gradlew :sa-admin:bootRun`
- ✅ Swagger UI 可訪問：http://localhost:1024/swagger-ui.html
- ✅ API 調用正常

### Phase 5 成功標準（文檔更新）
- ✅ `CLAUDE.md` 更新完成
- ✅ `docs/MIGRATION-TO-V3.md` 創建完成
- ✅ Git commit 包含完整的變更說明
- ✅ 推送到遠端分支

---

## 風險管理

| 風險 | 可能性 | 影響 | 緩解措施 |
|-----|--------|------|---------|
| Gradle 構建失敗 | 🟡 中 | 🔴 高 | 分階段驗證，每階段後測試構建 |
| 依賴路徑遺漏更新 | 🟡 中 | 🟡 中 | 批量替換 + 手動檢查關鍵文件 |
| ArchUnit 測試失敗 | 🟢 低 | 🟡 中 | 更新測試規則，使用 Java 包路徑而非 Gradle 路徑 |
| 應用啟動失敗 | 🟡 中 | 🔴 高 | 檢查 AutoConfiguration 順序，啟用 DEBUG 日誌 |
| 循環依賴引入 | 🟢 低 | 🔴 高 | Gradle 自動檢測，依賴圖分析 |

**回滾計畫：**
- 備份分支：`backup/before-path-a-refactoring`
- 快速回滾：`git reset --hard backup/before-path-a-refactoring`
- 清理工作目錄：`git clean -fdx`

---

## 預期成果

### 架構清晰度
- ✅ 完全對齊 RuoYi-Vue-Plus 的三層架構理念
- ✅ 清晰的層次邊界：Foundation → Infrastructure → Support → Application
- ✅ 統一的命名規則：`sa-base:foundation:*`, `sa-base:infrastructure:*`, `sa-base:support:*`

### 根目錄清理
- ✅ 從 12 個目錄 → 2 個目錄
- ✅ 業務邊界一目了然
- ✅ IDE 項目視圖極度清晰

### Gradle 兼容性
- ✅ 所有依賴都是同父模塊依賴
- ✅ 無跨父模塊嵌套依賴
- ✅ Gradle 8.11 完全兼容

### 微服務遷移準備
- ✅ 未來可以將 `sa-base` 打包為共享庫發布到 Maven 私服
- ✅ 清晰的模塊邊界方便微服務拆分
- ✅ 統一的基礎設施引用簡化微服務開發

---

## 後續工作

### 短期（遷移完成後 1 個月）
1. 監控團隊反饋，解決遷移後的適應問題
2. 補充更多遷移案例和常見問題文檔
3. 培訓團隊成員理解新架構

### 中期（3-6 個月）
1. 評估架構優化效果（開發效率、代碼可維護性）
2. 考慮是否進一步優化模塊邊界
3. 準備 RuoYi-Vue-Plus 對比分析報告

### 長期（1 年+）
1. 基於新架構規劃微服務拆分策略
2. 將 sa-base 打包為獨立共享庫
3. 持續優化模塊組織和依賴關係

---

**計畫版本：** 1.0
**創建日期：** 2026-01-21
**狀態：** ✅ 已確認，等待執行
**預估完成時間：** 1-2 週
