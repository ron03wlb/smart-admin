---
name: java21-postgresql-migration
description: [P1 - Quality] Java 17→21 與 MySQL→PostgreSQL 遷移規範。定義完整的遷移規則、SmartAdmin 特有 patterns、以及驗收 checklist。使用時機：新增模組、升級依賴、或驗證遷移完整性。
---

# Java 21 + PostgreSQL 遷移規範

**Version**: 1.1.0
**Priority**: P1 (Quality)
**Category**: Migration / Infrastructure
**Status**: Stable
**Last Updated**: 2026-03-30

---

## 概述

本技能定義 SmartAdmin 從 Java 17 升級至 Java 21，以及從 MySQL 遷移至 PostgreSQL 的完整規範。
遷移已於 2026-03 完成，本文件作為：

1. **規範參考**：新功能開發必須遵守的技術標準
2. **驗收 Checklist**：確認任何新模組或依賴升級是否符合規範
3. **故障排除指引**：常見遷移錯誤的解決方案

---

## Part 1：Java 17 → 21 遷移規範

### 1.1 Toolchain 配置

```kotlin
// build.gradle.kts - 根模組
configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))  // ✅ Java 21
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

```toml
# gradle/libs.versions.toml
[versions]
java = "21"
springBoot = "3.5.4"  # Spring Boot 3.x = Jakarta EE 10
```

### 1.2 Virtual Threads（Java 21 核心特性）

**強制要求**：所有環境配置（包括生產）必須啟用 Virtual Threads：

```yaml
# application.yml / application-{profile}.yml
spring:
  threads:
    virtual:
      enabled: true   # ✅ Java 21 Virtual Threads
```

**效果**：
- `@Async` 任務自動使用 Virtual Threads
- `@Scheduled` 任務自動使用 Virtual Threads
- I/O 密集型任務吞吐量提升 30-50%
- 每個 Virtual Thread 記憶體佔用 ~1KB（vs 平台執行緒 ~1MB）

**注意**：`AsyncConfig.java` 中如使用 `ThreadPoolTaskExecutor`，Spring Boot 3.2+ 在啟用 Virtual Threads 後會自動包裝為 Virtual Thread 執行器，無需手動修改。

### 1.3 Java 21 語言特性使用規範

#### Sealed Classes（已使用）
```java
// ✅ 正確：ErrorCode 使用 sealed interface
public sealed interface ErrorCode permits SystemErrorCode, UserErrorCode, UnexpectedErrorCode {
    int getCode();
    String getMessage();
}
```

#### Record Types（新模組建議使用）
```java
// ✅ 適合用於不可變 VO / DTO
public record PlayerSummaryVO(Long playerId, String username, BigDecimal balance) {}

// ❌ 不適合：Entity 類（MyBatis Plus 需要 setter）
```

#### Pattern Matching for instanceof（建議使用）
```java
// ✅ Java 21 模式匹配
if (errorCode instanceof UserErrorCode userError) {
    log.warn("User error: {}", userError.getMessage());
}

// ❌ 舊方式
if (errorCode instanceof UserErrorCode) {
    UserErrorCode userError = (UserErrorCode) errorCode;
}
```

#### Switch Expressions（建議使用）
```java
// ✅ Switch Expression（Java 14+，在 21 中穩定）
String result = switch (status) {
    case ACTIVE -> "active";
    case INACTIVE -> "inactive";
    default -> throw new IllegalArgumentException("Unknown status: " + status);
};
```

### 1.4 javax vs jakarta 規範

**Jakarta EE 遷移規則**（Spring Boot 3.x 強制要求）：

| ❌ 舊 javax（Jakarta EE，已遷移） | ✅ 新 jakarta |
|----------------------------------|---------------|
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.annotation.*` | `jakarta.annotation.*` |
| `javax.transaction.*` | `jakarta.transaction.*` |

**保留 javax 的例外**（JDK 標準庫，不需遷移）：

| javax 套件 | 說明 | 是否保留 |
|-----------|------|---------|
| `javax.crypto.*` | JDK 加密 API | ✅ 保留 |
| `javax.sql.DataSource` | JDK SQL DataSource | ✅ 保留 |
| `javax.imageio.*` | JDK 圖片 IO | ✅ 保留 |
| `javax.swing.*` | JDK Swing（測試工具） | ✅ 保留 |
| `javax.management.*` | JDK JMX | ✅ 保留 |

---

## Part 2：MySQL → PostgreSQL 遷移規範

### 2.1 依賴配置

```toml
# gradle/libs.versions.toml - ✅ 只使用 PostgreSQL
[versions]
postgresql = "42.7.5"

[libraries]
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgresql" }
flyway-core = { module = "org.flywaydb:flyway-core" }
flyway-database-postgresql = { module = "org.flywaydb:flyway-database-postgresql" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql" }

# ❌ 嚴禁：mysql-connector-java、mariadb-java-client
```

### 2.2 JDBC URL 格式

```yaml
# ✅ PostgreSQL
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smartadmin
    driver-class-name: org.postgresql.Driver

# ✅ P6Spy（開發/測試用 SQL logging）
    url: jdbc:p6spy:postgresql://localhost:5432/smartadmin
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver

# ❌ 嚴禁：jdbc:mysql://
```

### 2.3 Boolean 欄位處理（最關鍵規則）

**背景**：PostgreSQL 的 BOOLEAN 與 MyBatis 的 boolean 不相容，SmartAdmin 使用 `SMALLINT` 存儲布林值（0/1）。

**強制規則**：所有 XML mapper 中涉及 boolean 參數的位置，必須加 `typeHandler`：

```xml
<!-- ✅ 正確：顯式指定 typeHandler -->
<if test="deleted != null">
    AND deleted = #{deleted, typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
</if>

<!-- ✅ 正確：INSERT 語句 -->
<insert id="insert">
    INSERT INTO t_user (username, deleted)
    VALUES (#{username}, #{deleted, typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler})
</insert>

<!-- ❌ 錯誤：直接使用 boolean，在 PostgreSQL 上會報 "operator does not exist: smallint = boolean" -->
<if test="deleted != null">
    AND deleted = #{deleted}
</if>
```

**驗證命令**：
```bash
# 執行 Gradle 內建驗證（pre-commit hook 也會自動執行）
./gradlew validateMyBatisBooleanParams
```

**SQL Schema 規範**：
```sql
-- ✅ 正確：SMALLINT NOT NULL DEFAULT 0 (0=false, 1=true)
deleted SMALLINT NOT NULL DEFAULT 0

-- ❌ 錯誤：BOOLEAN 型別（與 typeHandler 不相容）
deleted BOOLEAN NOT NULL DEFAULT FALSE
```

### 2.4 SQL 語法差異：MySQL → PostgreSQL

#### 自增主鍵
```sql
-- ❌ MySQL
id BIGINT AUTO_INCREMENT PRIMARY KEY

-- ✅ PostgreSQL
id BIGSERIAL PRIMARY KEY
-- 或
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

#### 字符串函數
```sql
-- ❌ MySQL
IFNULL(column, 'default')

-- ✅ PostgreSQL
COALESCE(column, 'default')
```

```sql
-- ❌ MySQL
STR_TO_DATE('2026-01-01', '%Y-%m-%d')

-- ✅ PostgreSQL
TO_DATE('2026-01-01', 'YYYY-MM-DD')
TO_TIMESTAMP('2026-01-01 10:00:00', 'YYYY-MM-DD HH24:MI:SS')
```

#### 全文搜尋（INSTR → ILIKE）⚠️ 最常見遷移錯誤

**背景**：`INSTR()` 是 MySQL 專屬函數，PostgreSQL 不支援。H2（測試環境）支援 `INSTR()` 導致測試通過但生產環境失敗。

```xml
<!-- ❌ MySQL 語法（INSTR）- 在 PostgreSQL 報 function instr() does not exist -->
<if test="query.searchWord != null and query.searchWord != ''">
    AND ( INSTR(username, #{query.searchWord})
       OR INSTR(email, #{query.searchWord})
    )
</if>

<!-- ✅ PostgreSQL 語法（ILIKE） -->
<if test="query.searchWord != null and query.searchWord != ''">
    AND ( username ILIKE CONCAT('%', #{query.searchWord}, '%')
       OR email ILIKE CONCAT('%', #{query.searchWord}, '%')
    )
</if>
```

**替換規則**：
| 場景 | MySQL（❌） | PostgreSQL（✅） |
|------|-----------|----------------|
| 搜尋功能（不區分大小寫） | `INSTR(col, #{p})` | `col ILIKE CONCAT('%', #{p}, '%')` |
| 精確位置判斷（需回傳位置） | `INSTR(col, #{p}) > 0` | `strpos(col, #{p}) > 0` |

**注意**：`ILIKE` 等同 MySQL `INSTR()` + 不區分大小寫，為搜尋功能的首選方案。

**驗證**：
```bash
# 確認 XML mapper 中無殘留 INSTR()
grep -r "INSTR(" --include="*.xml" smart-admin-api-java21-springboot3 | grep -v build
# 預期：無輸出
```

#### 大小寫敏感
```sql
-- PostgreSQL 字串比較區分大小寫（MySQL 預設不區分）
-- ✅ 不區分大小寫搜尋
WHERE LOWER(username) = LOWER(#{username})
-- 或
WHERE username ILIKE #{username}   -- PostgreSQL 特有（不區分大小寫 LIKE）
```

#### 時間函數
```sql
-- ✅ PostgreSQL（NOW() 在兩者都支援）
update_time = NOW()

-- ✅ PostgreSQL 時間間隔語法
WHERE create_time >= NOW() - INTERVAL '#{days} days'

-- ❌ MySQL 語法（不支援）
WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
```

#### LIMIT / OFFSET
```sql
-- ❌ MySQL style（MyBatis Plus 已處理，勿手動使用）
LIMIT #{offset}, #{limit}

-- ✅ PostgreSQL / ANSI SQL style
LIMIT #{limit} OFFSET #{offset}
```

#### 資料庫元數據查詢（Information Schema）

```xml
<!-- ❌ MySQL 語法 -->
where table_schema = (select database())
    `tables`.table_name, `tables`.table_comment,

<!-- ✅ PostgreSQL 語法 -->
where tables.table_schema = current_schema() and tables.table_type = 'BASE TABLE'
    tables.table_name,
    obj_description(
        (quote_ident(tables.table_schema) || '.' || quote_ident(tables.table_name))::regclass
    ) as table_comment,
```

**說明**：
- `select database()` → `current_schema()`（當前 schema）
- MySQL backtick 識別符引號（`` ` ``）→ 去除（PostgreSQL 使用雙引號 `"`，通常不需要）
- `table_comment`（MySQL information_schema 欄位）→ `obj_description(regclass)` PostgreSQL 系統函數

#### JSON 支援
```sql
-- ✅ PostgreSQL JSONB（推薦）
config JSONB DEFAULT '{}'::jsonb

-- 查詢
WHERE config->>'key' = 'value'
WHERE config @> '{"status": "active"}'::jsonb
```

### 2.5 Flyway 遷移規範

**檔案命名規則**：
```
src/main/resources/db/migration/
├── V1__core_infrastructure.sql    # V{版本}__{描述}.sql
├── V2__igaming_domain_tables.sql
├── V3__igaming_features.sql
└── ...
```

**SQL 文件規範**：
```sql
-- ✅ Flyway migration 標準格式
-- 1. 建立表
CREATE TABLE IF NOT EXISTS t_player (
    player_id    BIGSERIAL PRIMARY KEY,
    username     VARCHAR(64) NOT NULL,
    deleted      SMALLINT NOT NULL DEFAULT 0,
    create_time  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    update_time  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    tenant_id    BIGINT NOT NULL DEFAULT 1
);

-- 2. 建立索引
CREATE INDEX IF NOT EXISTS idx_player_tenant_id ON t_player(tenant_id);
CREATE INDEX IF NOT EXISTS idx_player_username ON t_player(username);

-- 3. 欄位備註（必須）
COMMENT ON TABLE t_player IS '玩家基本資訊';
COMMENT ON COLUMN t_player.player_id IS '玩家 ID';
COMMENT ON COLUMN t_player.deleted IS '刪除標誌（0=未刪除，1=已刪除）';
```

**Spring Boot 配置**：
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    baseline-version: 0
```

### 2.6 MyBatis Plus 配置（PostgreSQL 特有）

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: net.lab1024.sa.**.domain.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    type-handlers-package: net.lab1024.sa.common.mybatis.typehandler  # ✅ 必須
  global-config:
    banner: false
    db-config:
      logic-delete-field: deleted          # ✅ 與 Entity 欄位名一致
      logic-delete-value: 1                # ✅ SMALLINT 1=已刪除
      logic-not-delete-value: 0            # ✅ SMALLINT 0=未刪除
```

### 2.7 HikariCP 連接池配置

```yaml
spring:
  datasource:
    hikari:
      pool-name: SmartAdminHikariPool
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000         # 30 秒
      idle-timeout: 600000              # 10 分鐘
      max-lifetime: 1800000             # 30 分鐘
      connection-test-query: SELECT 1   # PostgreSQL 健康檢查
      register-mbeans: true             # 啟用 JMX 監控
```

### 2.8 多租戶 Row-Level Security (RLS)

**生產環境**（使用 PostgreSQL RLS）：
```sql
-- 啟用 RLS
ALTER TABLE t_player ENABLE ROW LEVEL SECURITY;

-- 建立 Policy
CREATE POLICY player_tenant_isolation ON t_player
    USING (tenant_id = current_setting('app.tenant_id')::BIGINT);
```

**測試環境**（使用 MyBatis Plus interceptor）：
```yaml
# application-test.yml
tenant:
  enabled: true
  rls:
    enabled: false  # 測試環境用 MyBatis Plus interceptor 代替 PostgreSQL RLS
```

---

## Part 3：Testcontainers 整合測試規範

```kotlin
// build.gradle.kts（模組）
dependencies {
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
```

```java
// ✅ 標準整合測試結構
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PlayerServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## Part 4：驗收 Checklist

執行以下所有檢查，確認遷移完整性（0 個失敗 = 遷移完成）：

### A. 依賴檢查

```bash
# A1: 確認無 MySQL 依賴
grep -r "mysql\|mariadb" --include="*.toml" --include="*.kts" | grep -v build | grep -v "#"
# 預期：無輸出

# A2: 確認無 MySQL JDBC URL
grep -r "jdbc:mysql" --include="*.yml" --include="*.properties" --include="*.java" | grep -v build
# 預期：無輸出

# A3: 確認 PostgreSQL 驅動存在
grep "postgresql" gradle/libs.versions.toml
# 預期：postgresql = "42.7.5"
```

### B. Java 21 配置

```bash
# B1: 確認 Java 21 toolchain
grep "JavaLanguageVersion.of(21)\|VERSION_21" build.gradle.kts
# 預期：找到 3 行

# B2: 確認 Virtual Threads 啟用
grep -r "virtual.enabled: true\|virtual:\n.*enabled: true" --include="*.yml" | grep -v build
# 預期：至少 1 個配置文件

# B3: 確認 Jakarta EE（非 javax.servlet）
grep -r "import javax.servlet\|import javax.validation\|import javax.persistence" --include="*.java" | grep -v build
# 預期：無輸出
```

### C. PostgreSQL 特有規則

```bash
# C1: BooleanToSmallintTypeHandler 驗證（關鍵！）
./gradlew validateMyBatisBooleanParams
# 預期：BUILD SUCCESS

# C2: 確認無 MySQL 方言函數
grep -r "IFNULL\|STR_TO_DATE\|DATE_SUB\|AUTO_INCREMENT" --include="*.xml" --include="*.sql" | grep -v build
# 預期：無輸出

# C2b: 確認無 INSTR()（MySQL 專屬函數）
grep -r "INSTR(" --include="*.xml" smart-admin-api-java21-springboot3 | grep -v build
# 預期：無輸出

# C2c: 確認無 MySQL 資料庫元數據函數
grep -r "select database()" --include="*.xml" | grep -v build
# 預期：無輸出

# C3: 確認 Flyway migrations 存在
find . -path "*/db/migration/V*.sql" -not -path "*/build/*" | wc -l
# 預期：>= 5 個文件

# C4: 完整架構驗證
./gradlew :smartadmin-app:test --tests ArchitectureTest
# 預期：BUILD SUCCESS
```

### D. 全測試套件

```bash
# D1: 執行完整測試
./gradlew :smartadmin-app:test
# 預期：BUILD SUCCESS

# D2: 質量門禁
./gradlew qualityGateSequential
# 預期：✅ Quality Gate PASSED
```

---

## Part 5：常見錯誤與解決方案

### 錯誤 1：`operator does not exist: smallint = boolean`

**原因**：XML mapper 中 boolean 參數未指定 `typeHandler`

**解決**：
```xml
<!-- 加入 typeHandler -->
#{deleted, typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
```

### 錯誤 2：`PSQLException: ERROR: syntax error at or near "AUTO_INCREMENT"`

**原因**：SQL migration 文件使用 MySQL 語法

**解決**：
```sql
-- 將 AUTO_INCREMENT 替換為 BIGSERIAL
id BIGSERIAL PRIMARY KEY
```

### 錯誤 3：`cannot cast type text to smallint`

**原因**：Boolean 欄位在 PostgreSQL 中型別為 `SMALLINT`，但 MyBatis 試圖插入 `text` 值

**解決**：確認 `mybatis-plus.global-config.db-config.logic-delete-value: 1`（integer，非 string）

### 錯誤 4：`Caused by: javax.servlet.ServletException`

**原因**：使用了 javax 命名空間（Spring Boot 3.x 需要 jakarta）

**解決**：
```java
// 將 import javax.servlet.* 替換為：
import jakarta.servlet.*;
```

### 錯誤 5：`FlywayException: Validate failed: Migration checksum mismatch`

**原因**：已執行的 migration 文件被修改

**解決**：**絕對不能修改已執行的 migration 文件**，建立新版本的 migration：
```sql
-- 建立新文件 V{N+1}__fix_description.sql
ALTER TABLE t_player ADD COLUMN ...;
```

---

## Part 6：版本歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.1.0 | 2026-03-30 | 補充 INSTR()→ILIKE 規範（Part 2.4）、資料庫元數據查詢、Checklist C2b/C2c |
| 1.0.0 | 2026-03-29 | 初始版本：Java 17→21 + MySQL→PostgreSQL 完整遷移規範 |

---

**維護者**: SmartAdmin Infrastructure Team
**遷移完成日期**: 2026-03（Java 21 + PostgreSQL 16 + Spring Boot 3.5.4）
