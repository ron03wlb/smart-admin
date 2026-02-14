# iGaming 基礎設施缺口分析：SmartAdmin 需先完成的基建項目

> **文件類型**：設計文件 (Design Document)
> **版本**：1.0.0
> **日期**：2026-02-14
> **狀態**：已確認 (Confirmed)
> **前置依賴**：Phase 0-3 實作設計文件（9 份，11K+ 行）

## Context

iGaming 實作文件（Phase 0-3，共 9 份設計文件、11K+ 行）定義了一套完整的多租戶博弈平台架構。然而，這些設計大量依賴 SmartAdmin 尚未提供的基礎設施。本文件逐項比對「iGaming 需要什麼」vs「SmartAdmin 目前有什麼」，識別所有必須 **先在 SmartAdmin 層級建好** 的缺口，依優先級排序。

### 已確認的技術決策

| 決策項 | 選擇 | 影響 |
|--------|------|------|
| Multi-Tenant 策略 | **雙重保護同時上**（MyBatis-Plus interceptor + PostgreSQL RLS） | G1 + G11 合併為同一批次，複雜度提升但安全性最高 |
| OffsetDateTime 策略 | **全面遷移**（整個 SmartAdmin 遷移到 OffsetDateTime） | G2 複雜度從 M 升級為 **XL**，需改動所有現有 Entity 和 DB schema |
| DEK/KEK 金鑰管理 | **Config 簡化方案**（application.yml / DB 存放 DEK） | G5 先快速可用，未來可升級到 AWS KMS / Vault |
| Flyway 範圍 | **全專案引入**（baseline 現有 schema + 管理 iGaming 新表 + OffsetDateTime 遷移） | G3 複雜度升為 L，需 baseline 現有 DB，同時管理 G2 的 TIMESTAMPTZ 遷移 |

---

## 缺口總覽（依阻塞影響排序）

| # | 缺口項目 | 優先級 | 複雜度 | 阻塞 Phase | 建置位置 |
|---|---------|--------|--------|-----------|---------|
| G1 | Multi-Tenant 基礎設施 | P0 | XL | Phase 0 (全部) | smartadmin-common-mybatis + 新模組 |
| G2 | OffsetDateTime / TIMESTAMPTZ **全面遷移** | P0 | **XL** | Phase 0 (全部) | smartadmin-common-mybatis + 所有 Entity |
| G3 | Flyway 資料庫版本管理（全專案） | P0 | **L** | Phase 0 (DDL) | smartadmin-app + 新依賴 + baseline |
| G4 | DomainEvent + Kafka 冪等消費框架 | P0 | L | Phase 0 (事件驅動) | smartadmin-common-mq |
| G5 | AES-256-GCM 欄位加密 + Blind Index | P0 | L | Phase 2 (Player PII) | smartadmin-common-security 或新模組 |
| G6 | 財務精度工具 (DECIMAL 28,10 + HALF_EVEN) | P1 | S | Phase 1 (Wallet) | smartadmin-igaming-common |
| G7 | Resilience4j Circuit Breaker | P1 | S | Phase 1 (Payment) | libs.versions.toml + 新依賴 |
| G8 | LiteFlow 啟用 + SQL Rule 存儲 | P1 | M | Phase 1 (Wallet/VIP) | smartadmin-support-liteflow 配置 |
| G9 | 8 個新 Gradle Module 腳手架 | P1 | M | Phase 0 (模組結構) | settings.gradle.kts |
| G10 | ArchUnit iGaming 邊界規則 | P2 | S | Phase 0 (品質) | smartadmin-app test |
| G11 | PostgreSQL RLS 政策 + pg_partman | **P0** | L | Phase 0 (DB 層) | SQL migrations（併入 G1 同步建置） |
| G12 | Testcontainers Kafka/Redis 擴展 | P2 | S | Phase 0 (測試) | libs.versions.toml |

---

## 詳細缺口分析

### G1: Multi-Tenant 基礎設施 【P0-Critical / XL】

**iGaming 需要：**
- `TenantContext`（ThreadLocal 存儲當前 tenant_id）
- `TenantInterceptor`（從 HTTP Header/Token 提取 tenant_id 並設入 Context）
- MyBatis-Plus `TenantLineInnerInterceptor`（自動注入 `WHERE tenant_id = ?`）
- `@TenantIgnore` 注解（跳過租戶過濾，需 audit logging）
- PostgreSQL session 參數 `SET app.current_tenant_id`（for RLS）
- 4 層層級：Platform → Brand → Tenant → Player

**SmartAdmin 現況：**
- `MybatisPlusConfig.java` 只有 `PaginationInnerInterceptor`，**零租戶支援**
- 無 `TenantContext` 類
- 無 `TenantLineInnerInterceptor` 配置
- `smartadmin-common-datasource` 存在但僅做資料來源配置，不含租戶邏輯

**缺口影響：** 阻塞 **所有 iGaming Phase**（每張表都有 `tenant_id`）

**需要建置的項目：**
1. `TenantContext` (ThreadLocal holder)
2. `TenantInterceptor` (Web filter/interceptor)
3. `MybatisPlusConfig` 添加 `TenantLineInnerInterceptor`
4. `@TenantIgnore` 注解 + `TenantLineHandler` 忽略邏輯
5. PostgreSQL RLS session 參數注入（Connection hook）
6. MDC logging 注入 `tenantId`

---

### G2: OffsetDateTime / TIMESTAMPTZ 全面遷移 【P0-Critical / XL】

**決策：全面遷移**（不做條件共存，整個 SmartAdmin 統一改用 OffsetDateTime）

**iGaming 需要：**
- 所有時間欄位使用 `OffsetDateTime`（UTC）
- 資料庫使用 `TIMESTAMPTZ` 類型
- MyBatis-Plus 自動填充用 `OffsetDateTime.now(ZoneOffset.UTC)`

**SmartAdmin 現況：**
- `MybatisPlusFillHandler.java` 使用 `LocalDateTime.now(ZoneId.systemDefault())`
- 整個 codebase **零個** `OffsetDateTime` 引用
- 無 MyBatis TypeHandler 支援 `OffsetDateTime` ↔ `TIMESTAMPTZ`

**缺口影響：** 阻塞 **所有 iGaming Phase** + 影響所有現有 SmartAdmin Entity

**需要建置的項目：**
1. `MybatisPlusFillHandler` 改用 `OffsetDateTime.now(ZoneOffset.UTC)`
2. `OffsetDateTimeTypeHandler`（MyBatis TypeHandler for TIMESTAMPTZ）
3. 所有現有 Entity 的 `LocalDateTime` → `OffsetDateTime` 遷移
4. Jackson `JavaTimeModule` 確認 `OffsetDateTime` 序列化（ISO-8601 with offset）
5. 現有 DB schema 的 `TIMESTAMP` → `TIMESTAMPTZ` ALTER TABLE
6. 所有 Service/Manager 中使用 `LocalDateTime.now()` 的地方改為 `OffsetDateTime.now(ZoneOffset.UTC)`
7. API 層面：Form/VO 中的時間欄位統一更新

**風險警告：** 此為 **破壞性變更**，影響範圍涵蓋所有現有模組（system, business, oa）。需要：
- 完整的回歸測試
- DB migration script（ALTER COLUMN TYPE）
- 前端適配（時間格式可能需調整）

---

### G3: Flyway 資料庫版本管理（全專案）【P0-Critical / L】

**決策：全專案引入 Flyway**（baseline 現有 schema，同時管理 iGaming 新表 + OffsetDateTime 遷移）

**iGaming 需要：**
- Flyway 管理 32+ 張表的 DDL/DML migrations
- 分 Phase 版本化（V001__phase0_foundation.sql, V002__phase1_wallet.sql, etc.）
- 支持 PostgreSQL RLS 政策、partitioning DDL

**SmartAdmin 現況：**
- **完全無 Flyway**（未在 `libs.versions.toml` 或任何 `build.gradle` 中出現）
- 現有 schema 管理方式不明（可能手動 SQL）

**需要建置的項目：**
1. `libs.versions.toml` 添加 `flyway` 版本
2. `smartadmin-app/build.gradle` 添加 Flyway 依賴
3. `application.yaml` 配置 Flyway（migration location、baseline-on-migrate=true）
4. Migration 檔案目錄結構 `db/migration/`
5. **V001__baseline.sql**：空白佔位（baseline 現有 schema）
6. **V002__timestamp_to_timestamptz.sql**：所有現有表 `ALTER COLUMN ... TYPE TIMESTAMPTZ`（配合 G2）
7. **V003__phase0_igaming_foundation.sql**：iGaming Phase 0 DDL
8. 後續 iGaming Phase 的 migration scripts

---

### G4: DomainEvent + Kafka 冪等消費框架 【P0-Critical / L】

**iGaming 需要：**
- `DomainEvent` base class（eventId, eventType, tenantId, timestamp, payload）
- `Outbox` pattern（`t_outbox_event` 表 + 定時發布）
- ADR-015 三層冪等防護：
  1. Redis SETNX（5ms 快檢）
  2. DB UNIQUE constraint（`t_idempotent_key`）
  3. Redisson 分散式鎖（處理期間）
- `IdempotentKafkaListener` 抽象基類（擴展現有 `AbstractKafkaListener`）
- 5 個 Kafka topics 結構定義

**SmartAdmin 現況：**
- `AbstractKafkaListener` 已有基礎模板（handleMessage → doHandle → ack）
- 但 **無冪等檢查**、**無 DomainEvent**、**無 Outbox pattern**
- Kafka config 已存在但較基礎

**需要建置的項目：**
1. `DomainEvent` base class
2. `IdempotentService`（Redis + DB 雙層檢查）
3. `t_idempotent_key` 表 DDL
4. `IdempotentKafkaListener`（extends `AbstractKafkaListener` + 冪等檢查）
5. `OutboxEvent` entity + `OutboxPublisher` service
6. `t_outbox_event` 表 DDL
7. Kafka topic constants（igaming.wallet.transactions, igaming.risk.events, etc.）

---

### G5: AES-256-GCM 欄位加密 + Blind Index 【P0-Critical / L】

**iGaming 需要：**
- AES-256-GCM 加密個別欄位（email, phone, ID number）
- HMAC-SHA256 blind index（可搜尋但不需解密）
- Per-tenant DEK（Data Encryption Key）管理
- KEK（Key Encryption Key）層級保護
- Argon2id 密碼雜湊（m=65536, t=3, p=4）
- MyBatis TypeHandler 自動加解密

**SmartAdmin 現況：**
- `smartadmin-common-security`：有 `PasswordEncryptService`（Argon2id ✅）
- `smartadmin-common-api-encrypt`：有 API 層級加解密（request/response ✅）
- `smartadmin-common-data-masking`：有 JSON 脫敏（display masking ✅）
- Bouncy Castle 1.80 已在依賴中 ✅
- **但缺少**：欄位級 AES-256-GCM 加密、blind index 計算、DEK/KEK 管理

**需要建置的項目：**
1. `FieldEncryptService`（AES-256-GCM encrypt/decrypt）
2. `BlindIndexService`（HMAC-SHA256 計算）
3. `DekManager`（DEK 生成、加密、輪換）
4. `EncryptedFieldTypeHandler`（MyBatis TypeHandler，自動 encrypt/decrypt BYTEA）
5. `BlindIndexTypeHandler`（自動計算 blind index）

---

### G6: 財務精度工具 【P1-High / S】

**iGaming 需要：**
- `DECIMAL(28,10)` 資料庫精度
- `BigDecimal` + `RoundingMode.HALF_EVEN`（銀行家進位法）
- 標準化的 Money 運算工具（加、減、乘、除、比較）

**SmartAdmin 現況：**
- 無專門的財務計算工具類
- SmartAdmin 預設可能使用 `HALF_UP`

**需要建置的項目：**
1. `MoneyUtil` / `FinancialMathUtil`（scale=10, HALF_EVEN 強制）
2. iGaming BaseEntity 中金額欄位規範

---

### G7: Resilience4j Circuit Breaker 【P1-High / S】

**iGaming 需要：**
- PSP（Payment Service Provider）呼叫需 circuit breaker
- GP（Game Provider）呼叫需 circuit breaker
- 含 fallback、retry（exponential backoff）、rate limiter

**SmartAdmin 現況：**
- `libs.versions.toml` **無 Resilience4j**
- 完全無斷路器基礎設施

**需要建置的項目：**
1. `libs.versions.toml` 添加 `resilience4j` 版本
2. `build.gradle` 添加依賴（resilience4j-spring-boot3）
3. 配置 default circuit breaker 參數
4. （業務層面由 iGaming 各模組自行使用）

---

### G8: LiteFlow 啟用 + SQL Rule 存儲 【P1-High / M】

**iGaming 需要：**
- LiteFlow 啟用（VIP promotion rules, payment limits, risk scoring, activity workflows）
- SQL-based rule storage（PostgreSQL）
- QLExpress script execution

**SmartAdmin 現況：**
- `smartadmin-support-liteflow` 已存在 ✅
- `liteflow-rule-sql` 已在依賴中 ✅
- 但 **smart.liteflow.enabled=false**（目前停用）
- SQL XML parser 配置尚未完成

**需要建置的項目：**
1. 啟用 LiteFlow（`smart.liteflow.enabled=true`）
2. 完成 SQL rule storage 配置（table name, data source）
3. 驗證 QLExpress script 能正確執行
4. 建立 LiteFlow rule 管理 API（CRUD for rules in DB）

---

### G9: 8 個新 Gradle Module 腳手架 【P1-High / M】

**iGaming 需要 8 個新模組：**
1. `smartadmin-igaming-common`（共享 enums、base entities、constants）
2. `smartadmin-api-igaming`（Service interfaces、DTO、event contracts）
3. `smartadmin-igaming-wallet`（錢包、支付閘道）
4. `smartadmin-igaming-player`（玩家生命週期、KYC、VIP）
5. `smartadmin-igaming-game`（GP adapter、遊戲大廳）
6. `smartadmin-igaming-activity`（活動引擎、流水追蹤）
7. `smartadmin-igaming-risk`（風控引擎、Kafka 消費者）
8. `smartadmin-igaming-agent`（代理層級、信用網絡）

**SmartAdmin 現況：**
- `settings.gradle.kts` 目前 47 個模組
- 模組模板已有成熟範例（smartadmin-system, smartadmin-business 可參考）

**需要建置的項目：**
1. `settings.gradle.kts` 新增 8 個模組聲明
2. 每個模組的 `build.gradle.kts` + 依賴配置
3. Package 結構（`net.lab1024.sa.igaming.{module}.*`）
4. 每個模組的基礎目錄結構（controller/service/manager/dao/domain）

---

### G10: ArchUnit iGaming 邊界規則 【P2-Medium / S】

**iGaming 需要：**
- Wallet ⇎ Risk 不可直接 import（必須經 Kafka 事件）
- Cross-module 只能通過 `smartadmin-api-igaming` 的 interface
- iGaming 模組遵循現有 Controller → Service → Manager → Dao 規則

**SmartAdmin 現況：**
- ArchUnit 測試基礎設施已完備 ✅
- 但無 iGaming 模組邊界規則

**需要建置的項目：**
1. `IgamingArchitectureTest.java`（模組邊界規則）
2. 新增 package dependency rules for igaming modules

---

### G11: PostgreSQL RLS 政策 + pg_partman 分區 【P0-Critical / L】（併入 G1 同步建置）

**決策：與 G1 同步建置**（雙重保護同時上）

**iGaming 需要：**
- 30+ 張表的 RLS 政策（`CREATE POLICY ... USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT)`）
- `app_user` / `app_admin` 角色設置
- `pg_partman` 自動分區（wallet_transaction daily、round weekly）

**SmartAdmin 現況：**
- PostgreSQL 已在使用 ✅
- **無 RLS、無 pg_partman、無 partition 管理**

**需要建置的項目：**
1. RLS 啟用 SQL（`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`）
2. 30+ 張 RLS policy 定義（透過 Flyway migration）
3. DB 角色創建（`app_user`, `app_admin`）
4. JDBC Connection hook：每次取連線時 `SET app.current_tenant_id = ?`（與 G1 TenantContext 聯動）
5. `pg_partman` 安裝 + partition 配置
6. 自動 partition maintenance job（SmartJob）

---

### G12: Testcontainers Kafka/Redis 擴展 【P2-Medium / S】

**iGaming 需要：**
- Integration test 需 Kafka + Redis containers
- 已有 PostgreSQL Testcontainers ✅

**SmartAdmin 現況：**
- `testcontainers` 1.20.4 已在 BOM ✅
- `testcontainers-postgresql` 已配置 ✅
- **缺少** `testcontainers-kafka` 和 `testcontainers-redis`

**需要建置的項目：**
1. `libs.versions.toml` 添加 `testcontainers-kafka`、`testcontainers-redis`（或 GenericContainer）
2. Test base class 提供 Kafka/Redis 容器生命週期管理

---

## 建置順序（依賴鏈 Critical Path）

```
Sprint 1: 骨架 + 時間遷移（最先做，其他全依賴這兩個）
├── G9: Module 腳手架 ──────────────────── 程式碼的載體
└── G2: OffsetDateTime 全面遷移 ────────── 影響所有 Entity（XL 工程量）

Sprint 2: 租戶 + 資料庫基礎
├── G3: Flyway (全專案 baseline) ──────── DDL 管理 + TIMESTAMPTZ migration
├── G1: Multi-Tenant interceptor ────────── 應用層租戶隔離
└── G11: RLS + pg_partman ──────────────── DB 層租戶隔離（與 G1 同步）

Sprint 3: 事件 + 加密
├── G4: DomainEvent + 冪等框架 ────────── 事件驅動的基礎
└── G5: PII 加密 + Blind Index ────────── Config 簡化金鑰方案

Sprint 4: 金融 + 規則引擎
├── G6: 財務精度工具 ──────────────────── Wallet 前置
├── G7: Resilience4j ──────────────────── Payment 前置
└── G8: LiteFlow 啟用 ─────────────────── VIP/Risk 前置

Sprint 5: 品質門禁
├── G10: ArchUnit iGaming 規則
└── G12: Testcontainers Kafka/Redis
```

**Sprint 間不可跳過**：Sprint 2 依賴 Sprint 1，Sprint 3 依賴 Sprint 2（Flyway 管理 idempotent_key/outbox 表）。Sprint 4 可與 Sprint 3 部分平行。

---

## 已決策事項

| # | 問題 | 決策 | 備註 |
|---|------|------|------|
| 1 | Multi-Tenant 策略 | ✅ 雙重保護同時上 | G1 + G11 合併 Sprint 2 |
| 2 | OffsetDateTime 策略 | ✅ 全面遷移 | G2 升級為 XL，Sprint 1 |
| 3 | Flyway 範圍 | ✅ 全專案引入 | baseline 現有 schema + iGaming 新表 |
| 4 | DEK/KEK 管理 | ✅ Config 簡化方案 | 未來可升級 KMS/Vault |

## 仍需確認的事項（實作階段再議）

1. **LiteFlow 全局啟用**：G8 啟用 LiteFlow 是否影響現有 SmartAdmin 功能？需要 feature flag 隔離？
2. **OffsetDateTime 全面遷移的前端影響**：現有前端是否需要同步調整時間格式？

---

## 現有可直接複用的基礎設施（無需建置）

| 已有設施 | 模組 | iGaming 用途 |
|---------|------|-------------|
| Redisson 分散式鎖 | smartadmin-common-redis-lock | Wallet 並發控制 Layer 1 |
| JetCache L1+L2 | smartadmin-common-cache | Balance caching |
| Kafka 基礎整合 | smartadmin-common-mq | Event publishing/consuming |
| Sa-Token 認證 | smartadmin-common-token | Player/Admin 認證 |
| Argon2id 密碼雜湊 | smartadmin-common-security | Player 密碼 |
| Data Masking | smartadmin-common-data-masking | PII 顯示脫敏 |
| API Encrypt | smartadmin-common-api-encrypt | 傳輸層加密 |
| SmartJob 排程 | smartadmin-support-job | VIP demotion、partition cleanup |
| Operation Log | smartadmin-support-operatelog | 操作審計 |
| Serial Number | smartadmin-support-serialnumber | 訂單號生成 |
| IP Geolocation | smartadmin-common-ip-geolocation | 風控 IP 定位 |
| MyBatis-Plus 分頁 | smartadmin-common-mybatis | 列表查詢 |
| Bouncy Castle | 依賴已在 BOM | 加密演算法基礎 |
| ArchUnit | 測試依賴已配置 | 架構驗證 |
| Testcontainers PG | 測試依賴已配置 | Integration test |

---

## 總結

**12 個缺口項目**（根據決策調整後）：
- **P0 (Critical)**: 6 項 — G1, G2(XL), G3, G4, G5, G11(併入G1)
- **P1 (High)**: 3 項 — G6, G7, G8
- **P1 → Sprint 1**: G9（Module 腳手架，最先做）
- **P2 (Medium)**: 2 項 — G10, G12

**最大風險項**：
1. **G2 OffsetDateTime 全面遷移** — 影響所有現有 Entity + DB schema，是跨模組破壞性變更
2. **G1+G11 Multi-Tenant 雙重保護** — MyBatis interceptor + RLS 同時建置，工程量大
3. **G9 Module 腳手架** — 8 個新模組是所有業務程式碼的載體

**已有 15 項可直接複用**，SmartAdmin 的基礎設施覆蓋率約 55%，剩餘 45% 需要新建。

---

## 關鍵檔案索引

| 檔案 | 用途 | 受影響的缺口 |
|------|------|-------------|
| `smartadmin-common-mybatis/.../MybatisPlusConfig.java` | 需添加 TenantLineInnerInterceptor | G1 |
| `smartadmin-common-mybatis/.../MybatisPlusFillHandler.java` | LocalDateTime → OffsetDateTime | G2 |
| `gradle/libs.versions.toml` | 添加 Flyway, Resilience4j, testcontainers-kafka | G3, G7, G12 |
| `settings.gradle.kts` | 新增 8 個 iGaming 模組 | G9 |
| `smartadmin-common-mq/.../AbstractKafkaListener.java` | 擴展為 IdempotentKafkaListener 基類 | G4 |
| `smartadmin-common-security/` | 擴展 AES-256-GCM + blind index | G5 |
| `smartadmin-support-liteflow/` 配置 | 啟用 + SQL rule storage | G8 |
| `smartadmin-app/src/main/resources/dev/application.yaml` | Flyway + LiteFlow + tenant 配置 | G3, G8, G1 |
