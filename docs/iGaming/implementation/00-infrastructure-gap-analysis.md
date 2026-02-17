# iGaming 基礎設施缺口分析：SmartAdmin 需先完成的基建項目

> **文件類型**：設計文件 (Design Document)
> **版本**：1.13.0
> **日期**：2026-02-17
> **狀態**：已確認 (Confirmed) — 所有技術決策已完成（D1-D11），**20/20 缺口全部完成**
> **前置依賴**：Phase 0-3 實作設計文件（9 份，11K+ 行）
> **變更紀錄**：v1.13.0 — G2.3 前端 ISO-8601 + 多租戶 UI 適配完成（datetime-util.ts + tenant Pinia store + axios headers + 24 list pages formatDateTime），**20/20 gaps 全部完成 (100%)** / v1.12.0 — Sprint 2 全部完成（G1 Multi-Tenant + G3 Flyway + G11 RLS + G13 Sa-Token + G14 Virtual Thread + G15 UNIQUE + G1.5 數據遷移），後端 18/20 gaps 完成 / v1.11.0 — G12 Testcontainers 完成 / v1.10.0 — G10 ArchUnit 完成 / v1.9.0 — G16+Sprint 4 完成 / v1.8.0 — G8 / v1.7.0 — G7 / v1.6.0 — G6 / v1.5.0 — G5+Sprint 3 完成 / v1.4.0 — G4 / v1.3.x — G9+G2+G2.5+G11 / v1.2.x — D8-D11+5 新缺口 / v1.1.0 — 原始碼驗證+D1-D7

## Context

iGaming 實作文件（Phase 0-3，共 9 份設計文件、11K+ 行）定義了一套完整的多租戶博弈平台架構。然而，這些設計大量依賴 SmartAdmin 尚未提供的基礎設施。本文件逐項比對「iGaming 需要什麼」vs「SmartAdmin 目前有什麼」，識別所有必須 **先在 SmartAdmin 層級建好** 的缺口，依優先級排序。

### 已確認的技術決策

| # | 決策項 | 選擇 | 影響 |
|---|--------|------|------|
| D1 | Multi-Tenant 策略 | **雙重保護同時上**（MyBatis-Plus interceptor + PostgreSQL RLS） | G1 + G11 合併為同一批次，複雜度提升但安全性最高 |
| D2 | OffsetDateTime 策略 | **全面遷移** + **Big-bang 一次性遷移** | 131 檔案 467 變更點一次完成，不維護 LocalDateTime/OffsetDateTime 共存期 |
| D3 | API 時間格式 | **ISO-8601 新格式**（`2026-02-14T10:30:00+00:00`） | 前端必須同步適配，Jackson 輸出標準 ISO-8601 格式 |
| D4 | Tenant 範圍 | **全域加 tenant_id**（所有現有表 + iGaming 新表） | G1 範圍從 XL 升級為 **XXL**，所有現有 Entity 需加 `tenantId`，G11 RLS 覆蓋 77+ 表 |
| D5 | DEK/KEK 金鑰管理 | **Config 簡化方案**（application.yml / DB 存放 DEK） | G5 先快速可用，未來可升級到 AWS KMS / Vault |
| D6 | Flyway 範圍 | **全專案引入**（baseline 現有 schema + 管理 iGaming 新表 + TIMESTAMPTZ + tenant_id 遷移） | G3 複雜度升為 L+，需 baseline 現有 DB，管理 G2 + G1 的 DDL 遷移 |
| D7 | MySQL DDL 處理 | **重寫為 PostgreSQL**（保留設計意圖，納入 Flyway baseline） | `sa-admin/build/` 中 4 個 MySQL 語法 SQL 需重寫 |
| D8 | 主鍵策略 | **iGaming 新表改用 Snowflake**（`IdType.ASSIGN_ID`），現有表保留 `IdType.AUTO` | G15 主鍵部分確認，iGaming Entity 使用 Snowflake ID，避免跨 tenant ID 語意洩漏 |
| D9 | Sa-Token 多帳戶體系 | **獨立 StpLogic**（`StpAdminUtil` / `StpPlayerUtil` 各自獨立） | G13 認證架構確認，Admin 和 Player 完全隔離的 Token 體系，各有獨立的 token-name 和登入態 |
| D10 | Argon2id 密碼遷移 | **Lazy migration**（用戶下次登入時重新 hash） | G16 遷移策略確認，無需批次遷移，驗證成功後用新參數重新 encode 並更新 DB |
| D11 | LiteFlow 啟用策略 | **直接啟用 + 空規則**（不需 feature flag） | G8 確認直接在 `application.yaml` 啟用，空規則不影響現有功能 |

### D4 決策的重大影響

「全域加 tenant_id」將 G1 的範圍從「僅 iGaming」擴展到**整個 SmartAdmin**：

| 影響面 | 原始範圍（僅 iGaming） | 新範圍（全域） |
|--------|----------------------|---------------|
| Entity 需加 `tenantId` 欄位 | iGaming 32+ 表 | **ALL 45+ 現有表 + 32+ 新表** |
| DB ALTER TABLE | 無（全新表） | **所有現有表 ADD COLUMN tenant_id** |
| RLS 政策 | 32+ iGaming 表 | **77+ 所有表** |
| 數據遷移 | 無 | **現有數據需設定 default tenant_id** |
| TenantLineInnerInterceptor 排除清單 | 大（所有非 iGaming 表） | **小（僅少數系統表如 flyway_schema_history）** |

---

## 缺口總覽（依阻塞影響排序）

| # | 缺口項目 | 優先級 | 複雜度 | 阻塞 Phase | 建置位置 |
|---|---------|--------|--------|-----------|---------|
| **G0** | **BaseEntity 共用基類提取** ✅ DONE | **P0** | **M** | Phase 0 (G1+G2 前置) | SmartAdminBaseEntity (tenantId + OffsetDateTime + deleted) |
| G1 | Multi-Tenant 基礎設施（**全域版**） ✅ DONE | P0 | **XXL** | Phase 0 (全部) | TenantContext + SmartTenantLineHandler + BaseEntity tenantId + MybatisPlusFillHandler |
| G2 | OffsetDateTime / TIMESTAMPTZ **全面遷移** ✅ DONE | P0 | **XXL→M** | Phase 0 (全部) | App 層已完成(G0)，DB TIMESTAMPTZ + 清理 |
| G3 | Flyway 資料庫版本管理（全專案） ✅ DONE | P0 | **L+** | Phase 0 (DDL) | V1-V7 migrations (baseline, tenant_id, RLS, TIMESTAMPTZ, idempotent) |
| G4 | DomainEvent + Kafka 冪等消費框架 ✅ DONE | P0 | L | Phase 0 (事件驅動) | smartadmin-common-mq |
| G5 | AES-256-GCM 欄位加密 + Blind Index ✅ DONE | P0 | L | Phase 2 (Player PII) | smartadmin-common-security |
| G6 | 財務精度工具 (DECIMAL 19,4 + HALF_EVEN) ✅ DONE | P1 | S | Phase 1 (Wallet) | smartadmin-igaming-common |
| G7 | Resilience4j Circuit Breaker ✅ DONE | P1 | S | Phase 1 (Payment) | libs.versions.toml + application.yaml |
| G8 | LiteFlow 配置整合 + SQL Rule 存儲 ✅ DONE | P1 | **M-** | Phase 1 (Wallet/VIP) | smartadmin-app 配置啟用 + SQL 規則存儲 |
| G9 | 8 個新 Gradle Module 腳手架 ✅ DONE | P1 | M | Phase 0 (模組結構) | settings.gradle.kts |
| G10 | ArchUnit iGaming 邊界規則 ✅ DONE | P2 | S | Phase 0 (品質) | smartadmin-app IgamingArchitectureTest (13 rules) |
| G11 | PostgreSQL RLS 政策 + pg_partman ✅ DONE | **P0** | **XL** | Phase 0 (DB 層) | V5__rls_policies.sql（77+ 表全域 RLS，併入 G1） |
| G12 | Testcontainers Kafka/Redis 擴展 ✅ DONE | P2 | S | Phase 0 (測試) | AbstractIntegrationTestBase (PostgreSQL + Kafka + Redis) |
| **G13** | **Sa-Token 多租戶認證適配** ✅ DONE | **P0** | **M** | Phase 0 (認證) | StpAdminUtil + StpPlayerUtil + LoginService tenant binding |
| **G14** | **Virtual Thread + TenantContext 相容性** ✅ DONE | **P0** | **S** | Phase 0 (G1 前置) | TenantTaskDecorator + 設計決策完成 |
| **G15** | **UNIQUE Constraint + tenant_id 衝突改造** ✅ DONE | **P0** | **M** | Phase 0 (G1.5 聯動) | V4__unique_constraint_tenant.sql |
| **G16** | **Argon2id 參數強化** ✅ DONE | **P1** | **S** | Phase 2 (Player) | smartadmin-common-security + LoginService lazy migration |
| **G1.5** | **全域 tenant_id 數據遷移** ✅ DONE | **P0** | **M** | Phase 0 (數據) | V2__add_tenant_id.sql (default tenant + NOT NULL) |
| **G2.3** | **前端 ISO-8601 + 多租戶 UI 適配** ✅ DONE | **P1** | **M+** | Phase 0 (前端) | datetime-util.ts + tenant store + axios headers + 24 list pages |
| **G2.5** | **MySQL DDL → PostgreSQL 重寫** ✅ DONE | **P1** | **S** | Phase 0 (DDL) | `sa-admin/build/` 殘留清理（D7 衍生） |

---

## 詳細缺口分析

### G0: BaseEntity 共用基類提取【P0-Critical / M】（NEW v1.2.0）

> **v1.2.0 新增**：原始碼驗證發現 45+ Entity 無共用基類，每個獨立定義 `createTime`/`updateTime`/`deleted`。G1 加 `tenantId` 和 G2 改 `OffsetDateTime` 如果不先提取 BaseEntity，同樣的 45+ 檔案要改兩輪，提取後可減少約 50% Entity 修改工作量。

**iGaming 需要：**
- 統一的 Entity 基類，包含多租戶 + 時間審計 + 軟刪除共用欄位
- G1（tenantId）和 G2（OffsetDateTime）的共同前置條件

**SmartAdmin 現況（原始碼驗證）：**
- `Glob("**/*BaseEntity*.java")` → **零結果**，整個 codebase 無 BaseEntity
- `EmployeeEntity.java` 直接定義 `@TableField(fill = FieldFill.INSERT) private LocalDateTime createTime`
- 所有 Entity（45+ 檔案）重複定義相同欄位（`createTime`, `updateTime`, `deleted`）
- `MybatisPlusFillHandler.java` 目前只填充 `createTime` 和 `updateTime`，無 `tenantId` 填充邏輯

**缺口影響：** 阻塞 **G1** 和 **G2**，是 Sprint 0.5 的唯一任務

**需要建置的項目：**
1. `SmartAdminBaseEntity`（abstract class，含 `tenantId`, `createTime`/`updateTime` as `OffsetDateTime`, `deleted`, `createBy`/`updateBy`）
2. 所有現有 Entity 改繼承 `SmartAdminBaseEntity`，移除重複欄位定義（45+ 檔案）
3. `MybatisPlusFillHandler` 擴展：新增 `tenantId` 自動填充（INSERT 時從 `TenantContext` 取值）
4. 確保 MyBatis-Plus `@TableField(fill = FieldFill.INSERT)` 等注解移至 BaseEntity
5. 回歸測試：確認現有 CRUD 功能不受影響

**關鍵檔案：**
- `smartadmin-common-mybatis/.../MybatisPlusFillHandler.java` — 填充邏輯擴展
- `smartadmin-modules/smartadmin-system/.../EmployeeEntity.java` — 範例 Entity 重構

---

### G1: Multi-Tenant 基礎設施（全域版）【P0-Critical / XXL】✅ DONE

> **v1.1.0 更新**：D4 決策確認**全域加 tenant_id**（所有現有表 + iGaming 新表），範圍從 XL 升級為 XXL。

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
- **原始碼驗證**：整個 codebase 零個 `TenantContext`/`tenant_id`/`tenantId` 引用

**缺口影響：** 阻塞 **所有 iGaming Phase** + **所有現有模組**（D4 決策：全域 tenant_id）

**需要建置的項目：**
1. `TenantContext` (ThreadLocal holder, Virtual Threads 安全)
2. `TenantInterceptor` (Web filter/interceptor)
3. `MybatisPlusConfig` 添加 `TenantLineInnerInterceptor`（全域，僅排除系統表如 `flyway_schema_history`）
4. `@TenantIgnore` 注解 + `TenantLineHandler` 忽略邏輯
5. PostgreSQL RLS session 參數注入（Connection hook）
6. MDC logging 注入 `tenantId`
7. **所有現有 Entity 類加入 `tenantId` 欄位**（45+ Entity 檔案）
8. **現有數據 default tenant_id 填充 + NOT NULL 約束**（參見 G1.5）
9. **含 tenant_id 的複合索引設計**（部分表需要）— 詳見 G15 UNIQUE constraint 改造
10. **`MybatisPlusFillHandler` 新增 tenantId 自動填充**（INSERT 時從 `TenantContext` 取值）(v1.2.0 補充)
11. **`TenantContextTaskDecorator`** for @Async 跨線程傳遞（與 G14 聯動）(v1.2.0 補充)
12. **前置依賴 G0**（BaseEntity 提取），45+ Entity 改繼承 BaseEntity 而非逐檔加 tenantId (v1.2.0 補充)

---

### G2: OffsetDateTime / TIMESTAMPTZ 全面遷移 【P0-Critical / XXL→M】✅ DONE

> **v1.1.0 更新**：原始碼驗證後，範圍從 XL 升級為 **XXL**。D2 確認 Big-bang 一次性遷移，D3 確認 API 輸出 ISO-8601 格式。

> **v1.3.1 實作完成**：原估 XXL（131 檔、467 變更）大幅縮減為 M（5 檔），因 G0（BaseEntity 提取）已完成應用層 OffsetDateTime 遷移。剩餘工作：V6 TIMESTAMPTZ migration + Code Generator 模板修正 + DataTracer 死碼清理 + application.yaml UTC 修正。

**決策：全面遷移 + Big-bang**（不做條件共存，整個 SmartAdmin 統一改用 OffsetDateTime，一次完成）

**實作摘要 (Sprint 1)：**
- **範圍縮減**：原估 XXL（131 檔 467 變更）→ 實際 M（5 檔），G0 BaseEntity 提取已完成 ~95% 應用層遷移
- `V6__timestamptz_migration.sql`：ALTER ~100 TIMESTAMP columns → TIMESTAMPTZ（48 tables），PostgreSQL 隱式轉換無資料損失
- `CodeGenerateBaseVariableService.java`：`LocalDateTime` → `OffsetDateTime` 常數 + import string，新生成程式碼自動使用 OffsetDateTime
- `CodeGeneratorTemplateService.java`：移除 `.toLocalDateTime()` 呼叫，直接用 OffsetDateTime.format()
- `DataTracerChangeContentService.java`：移除死 `instanceof LocalDateTime` 分支 + 未使用 import
- `application.yaml`：`time-zone: GMT+8` → `time-zone: UTC`（TenantTimezoneSerializer 在序列化時轉換為租戶時區）
- **應用層已完成（G0 階段）**：SmartAdminBaseEntity 使用 OffsetDateTime、MybatisPlusFillHandler 使用 OffsetDateTime.now(ZoneOffset.UTC)、TenantTimezoneSerializer 已註冊、零 `private LocalDateTime` 欄位、零 `@JsonFormat` 註解

**iGaming 需要：**
- 所有時間欄位使用 `OffsetDateTime`（UTC）
- 資料庫使用 `TIMESTAMPTZ` 類型
- MyBatis-Plus 自動填充用 `OffsetDateTime.now(ZoneOffset.UTC)`

**SmartAdmin 現況（原始碼驗證）：**
- `MybatisPlusFillHandler.java` 使用 `LocalDateTime.now(ZoneId.systemDefault())`
- 整個 codebase **零個** `OffsetDateTime` 引用
- 無 MyBatis TypeHandler 支援 `OffsetDateTime` ↔ `TIMESTAMPTZ`

**影響範圍量化（原始碼層級掃描）：**

| 類別 | 受影響檔案 | `LocalDateTime` 出現次數 |
|------|-----------|------------------------|
| 基礎設施 (common) | 5 檔 | 32 次 |
| 支援模組 (support) | 78 檔 | 296 次 |
| 業務模組 (modules) | 35 檔 | 105 次 |
| API 層 (DTOs/VOs/Forms) | 10 檔 | 29 次 |
| 測試程式碼 | 3 檔 | 5 次 |
| **合計** | **131 檔** | **467 次** |

**4 個必須最先修改的關鍵檔案：**
1. `smartadmin-common-mybatis/.../MybatisPlusFillHandler.java` — 所有 Entity createTime/updateTime 的填充源頭
2. `smartadmin-common-core/.../SmartLocalDateUtil.java` — 日期工具類，需建立 OffsetDateTime 等效方法
3. `smartadmin-common-web/.../JsonConfig.java` — Jackson 序列化配置，需加 OffsetDateTime ISO-8601 serializer/deserializer
4. 各 Entity 的 `createTime`/`updateTime` 欄位類型（131 檔遍歷修改）

**缺口影響：** 阻塞 **所有 iGaming Phase** + 影響所有現有 SmartAdmin Entity + **前端 API 契約變更**

**需要建置的項目：**
1. `MybatisPlusFillHandler` 改用 `OffsetDateTime.now(ZoneOffset.UTC)`
2. `OffsetDateTimeTypeHandler`（MyBatis TypeHandler for TIMESTAMPTZ）
3. 所有現有 Entity 的 `LocalDateTime` → `OffsetDateTime` 遷移（45+ Entity 檔案）
4. 所有 VO/Form/QueryForm 的 `LocalDateTime` → `OffsetDateTime` 遷移（30+ 檔案）
5. Jackson `JsonConfig.java` 加入 `OffsetDateTime` ISO-8601 序列化（D3 決策）
6. `SmartLocalDateUtil` → 建立 `SmartOffsetDateTimeUtil` 或重構現有工具類
7. 現有 DB schema 的 `TIMESTAMP` → `TIMESTAMPTZ` ALTER TABLE
8. 所有 Service/Manager 中使用 `LocalDateTime.now()` 的地方改為 `OffsetDateTime.now(ZoneOffset.UTC)`（20+ 檔案）
9. 測試程式碼更新（3+ 檔案）
10. **前置依賴 G0**（BaseEntity 中 createTime/updateTime 一次改為 OffsetDateTime，減少逐檔修改量）(v1.2.0 補充)
11. **`SmartDateFormatterEnum.java` 新增 ISO-8601 formatter**：現有 8 種日期格式全部無時區資訊（`yyyy-MM-dd HH:mm:ss`），需新增 `ISO_OFFSET_DATE_TIME` (v1.2.0 補充)

**v1.2.0 補充 — `JsonConfig.java` 完整影響：**

> 原文件僅提及「Jackson JsonConfig.java 加入 OffsetDateTime 序列化」一行，實際影響遠大於此。

`JsonConfig.java`（`smartadmin-common-web`）包含 **6 個需要改造的元件**：
1. `LocalDateDeserializer` → 需新增 `OffsetDateTimeDeserializer`
2. `LocalDateTimeDeserializer` → 需替換為 `OffsetDateTimeDeserializer`（ISO-8601 格式）
3. `LocalDateSerializer` → 保留（日期無時區）
4. `LocalDateTimeSerializer` → 需替換為 `OffsetDateTimeSerializer`（ISO-8601 格式）
5. `StringToLocalDateTime` 內部 Converter 類 → 需新增 `StringToOffsetDateTime` Converter
6. `StringToLocalDate` 內部 Converter 類 → 保留（日期無時區）

此外，`OperateLogVO.java` 有硬編碼 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")` → 需改為 ISO-8601 格式（`timezone = "UTC"` 或移除讓全域配置處理）。

**風險警告：** 此為 **跨層破壞性變更**（Entity → VO → Form → Service → Test → 前端 → DB schema），影響範圍涵蓋所有現有模組（system, business, oa, support）。需要：
- 完整的回歸測試
- DB migration script（ALTER COLUMN TYPE）— 透過 Flyway G3 管理
- **前端同步適配 ISO-8601 格式**（參見 G2.3）— D3 已確認，需前端團隊配合

---

### G3: Flyway 資料庫版本管理（全專案）【P0-Critical / L+】✅ DONE

> **v1.1.0 更新**：發現 `sa-admin/build/` 中存在 4 個 MySQL 語法 DDL 殘留。D6 決策全專案引入 Flyway，D7 決策重寫 MySQL DDL 為 PostgreSQL。複雜度升為 L+。

**決策：全專案引入 Flyway**（baseline 現有 schema + 管理 iGaming 新表 + TIMESTAMPTZ + 全域 tenant_id 遷移）

**iGaming 需要：**
- Flyway 管理 77+ 張表的 DDL/DML migrations（D4 全域 tenant_id 後）
- 分 Phase 版本化（V001__baseline.sql, V002__timestamptz.sql, V003__tenant_id.sql, etc.）
- 支持 PostgreSQL RLS 政策、partitioning DDL

**SmartAdmin 現況：**
- `libs.versions.toml` 和 `build.gradle` 中**無 Flyway 依賴**
- **但 `sa-admin/build/resources/main/db/migration/` 中存在 4 個過期的 DDL 檔案**：
  - `V1.0.11__create_wagering_tracking_tables.sql` — 3 張流水追蹤表（**MySQL 語法**：AUTO_INCREMENT, ENGINE=InnoDB, TINYINT, DATETIME）
  - `V1.1__liteflow.sql` — LiteFlow 表
  - `V1.2__rename_goods_table.sql` — 改名腳本
  - `V999__ai_system_tables.sql` — AI 系統表
- 這些檔案位於 build output 目錄（非 source），使用 **MySQL 語法但系統運行在 PostgreSQL** 上，為過期原型殘留

**需要建置的項目：**
1. `libs.versions.toml` 添加 `flyway` 版本
2. `smartadmin-app/build.gradle` 添加 Flyway 依賴
3. `application.yaml` 配置 Flyway（migration location、baseline-on-migrate=true）
4. Migration 檔案目錄結構 `db/migration/`（位於 `src/main/resources/`）
5. **V001__baseline.sql**：空白佔位（baseline 現有 schema）
6. **V002__timestamp_to_timestamptz.sql**：所有現有表 `ALTER COLUMN ... TYPE TIMESTAMPTZ`（配合 G2）
7. **V003__add_tenant_id.sql**：所有現有表 `ADD COLUMN tenant_id` + default 填充 + NOT NULL 約束（配合 G1/D4）
8. **V004__phase0_igaming_foundation.sql**：iGaming Phase 0 DDL
9. **清理/重寫 MySQL DDL 殘留**：`sa-admin/build/` 中的 4 個 SQL 檔案重寫為 PostgreSQL 語法（D7 決策），納入 Flyway 管理（參見 G2.5）
10. 後續 iGaming Phase 的 migration scripts

---

### G4: DomainEvent + Kafka 冪等消費框架 【P0-Critical / L】 ✅ DONE

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

### G5: AES-256-GCM 欄位加密 + Blind Index ✅ DONE 【P0-Critical / L】

**狀態**：✅ 已完成（v1.5.0, 2026-02-16）

**已建置項目：**
1. `AesGcmFieldEncryptService` — AES-256-GCM encrypt/decrypt（格式：`v1:{iv}:{ciphertext}`）
2. `BlindIndexService` — HMAC-SHA256 計算（64-char hex）
3. `EncryptedFieldTypeHandler` — MyBatis TypeHandler 自動加解密
4. `FieldEncryptProperties` — `@ConfigurationProperties("smart.field-encrypt")`
5. `FieldEncryptAutoConfiguration` — 條件式 Bean 註冊（`smart.field-encrypt.enabled=true`）

**建置位置**：`smartadmin-common-security` (`net.lab1024.sa.common.security.encrypt.*`)

**設計決策**：
- D2: 純 JDK Crypto（無需 BouncyCastle），Java 21 原生支援 AES/GCM/NoPadding + HmacSHA256
- D3: 3-part 儲存格式（Java GCM 自動附加 AuthTag）
- D4: Static setter 模式讓 TypeHandler 存取 Spring Bean
- D5: Blind Index 在 Service/Manager 層計算（非 TypeHandler）

---

### G6: 財務精度工具 ✅ DONE 【P1-High / S】

**狀態**：✅ 已完成（v1.6.0, 2026-02-16）

**已建置項目：**
1. `IgamingMoneyUtil` — BigDecimal 運算工具（scale=4, `RoundingMode.HALF_EVEN`）
   - 四則運算：add, subtract, multiply, divide（全部強制 scale=4 + HALF_EVEN）
   - 比較：isGreaterThan, isLessThan, isGreaterOrEqual, isLessOrEqual, equals
   - 驗證：requireNonNegative, isPositive, isNegative, isZero
   - Null-safe：null 視為 ZERO

**建置位置**：`smartadmin-igaming-common` (`net.lab1024.sa.igaming.common.util.IgamingMoneyUtil`)

**設計決策**：
- D1: 使用 DECIMAL(19,4)（匹配 Wallet DDL 設計，非原始的 28,10）
- D2: 新建 `IgamingMoneyUtil`，不修改現有 `SmartBigDecimalUtil`（避免影響 SmartAdmin 既有邏輯）

---

### G7: Resilience4j Circuit Breaker ✅ DONE 【P1-High / S】

**狀態**：✅ 已完成（v1.7.0, 2026-02-16）

**已建置項目：**
1. `resilience4j-spring-boot3` v2.2.0 依賴（`libs.versions.toml` + `smartadmin-app/build.gradle.kts`）
2. `pspDefault` circuit breaker 配置：COUNT_BASED sliding window (10), 50% failure threshold, 30s open wait, 5s slow call
3. `gpDefault` circuit breaker 配置：COUNT_BASED sliding window (10), 50% failure threshold, 60s open wait, 10s slow call
4. `pspDefault` retry 配置：max 3 attempts, exponential backoff (1s base, 2x multiplier), IOException + TimeoutException

**建置位置**：`libs.versions.toml` + `smartadmin-app/build.gradle.kts` + `application.yaml`

**設計決策**：
- D1: resilience4j-spring-boot3 v2.2.0（Spring Boot 3.5.4 compatible）
- D2: 依賴放在 `smartadmin-app` 層（Spring Boot starter auto-configuration）
- D3: Config-only approach（業務層 `@CircuitBreaker` 由 iGaming 各模組自行使用）

---

### G8: LiteFlow 配置整合 + SQL Rule 存儲 ✅ DONE 【P1-High / M-】

**狀態**：✅ 已完成（v1.8.0, 2026-02-16）

**已建置項目：**
1. 移除 `smartadmin-app/build.gradle.kts` 中的 LiteFlow exclusion（原 `exclude(group = "net.lab1024", module = "smartadmin-support-liteflow")`）
2. 啟用 `smart.liteflow.enabled: true`（D11 決策：直接啟用 + 空規則）
3. 新增 `liteflow:` 根級 SQL 規則存儲配置（`rule-source: sql`，映射 `t_liteflow_chain` + `t_liteflow_script` 表）
4. 禁用 LiteFlow 內建輪詢（`pollingEnabled: false`），使用 SmartReload 手動觸發規則重載

**建置位置**：`smartadmin-app/build.gradle.kts` + `application.yaml`

**設計決策**：
- D1: 純配置變更（模組程式碼 15+ Java 檔案早已就緒）
- D2: 複用 Spring datasource（同一 PostgreSQL 實例）
- D3: 禁用 polling，使用 SmartReload（`/reload/execute?tag=liteflow`）

**已就緒但未修改的元件**：
- `LiteFlowAutoConfiguration`（`@ConditionalOnProperty("smart.liteflow.enabled")`）
- `LiteFlowChainService/Controller` + `LiteFlowScriptService/Controller`（CRUD API 完整）
- `SmartFlowExecutor`（統一執行入口）
- `LiteFlowCacheManager`（JetCache L1+L2 緩存）
- 4 張 DB 表（V2 tenant_id + V5 RLS + V6 TIMESTAMPTZ）

---

### G9: 8 個新 Gradle Module 腳手架 【P1-High / M】✅ DONE

> **v1.3.3 實作完成**：Sprint 1 G9 已完成 8 個 iGaming Gradle 模組腳手架建立。

**實作摘要 (Sprint 1)：**
- 7 個模組於 `smartadmin-igaming/` 頂層群組（common + 6 業務模組）
- 1 個 API 契約模組於 `smartadmin-api/smartadmin-api-igaming/`
- `settings.gradle.kts` 新增 8 個模組（47 → 55 模組）
- `smartadmin-app/build.gradle.kts` 新增 6 個 iGaming 業務模組依賴
- 套件結構：`net.lab1024.sa.igaming.{module}.*`（業務）/ `net.lab1024.sa.api.igaming`（API）
- igaming-common：輕量依賴（BOM + core + mybatis + vavr + jackson）
- api-igaming：完全遵循 `smartadmin-api-business` 模式（BOM + vavr + validation + jackson）
- 6 個業務模組：遵循 `smartadmin-business` 模式，依賴 api-igaming + igaming-common
- risk 模組額外依賴 `smartadmin-common-mq`（Kafka 消費者）
- 空腳手架（僅 package-info.java），BUILD SUCCESSFUL（172 tasks compile / 253 tasks test）

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
- `settings.gradle.kts` 目前 55 個模組（原 47 + 8 新增）✅
- 模組模板已有成熟範例（smartadmin-system, smartadmin-business 可參考）

**已建置項目：**
1. ✅ `settings.gradle.kts` 新增 8 個模組聲明
2. ✅ 每個模組的 `build.gradle.kts` + 依賴配置（8 個 build 檔案）
3. ✅ Package 結構（`net.lab1024.sa.igaming.{module}.*`）— 8 個 `package-info.java`
4. ⏳ 每個模組的業務目錄結構（controller/service/manager/dao/domain）— 待業務開發時建立

---

### G10: ArchUnit iGaming 邊界規則 ✅ DONE 【P2-Medium / S】

**狀態**：✅ 已完成（v1.10.0, 2026-02-17）

**已建置項目：**
1. `IgamingArchitectureTest.java`（13 個 iGaming 邊界規則）：
   - 分層架構：Controller → Service → Manager → Dao（1 rule）
   - 循環依賴檢測：slices 無循環（1 rule）
   - Wallet ⇎ Risk Kafka 隔離：雙向禁止直接 import（4 rules）
   - 跨模組服務隔離：6 個業務模組各自隔離（6 rules）
   - 循環依賴檢測（1 rule）
2. `ArchitectureTest.java` BUSINESS_CLASSES 過濾器加入 igaming

**建置位置**：`smartadmin-app/src/test/java/net/lab1024/sa/app/IgamingArchitectureTest.java`

**設計決策**：
- D1: 測試放在 smartadmin-app（跨模組可見性 + ArchUnit 依賴已存在）
- D2: `@AnalyzeClasses` 風格（效率 + 與 system/business/oa 一致）
- D3: API 契約層 `net.lab1024.sa.api.igaming` 不匹配 `..igaming.X..` pattern，合法跨模組調用不被攔截

---

### G11: PostgreSQL RLS 政策 + pg_partman 分區 【P0-Critical / XL】（併入 G1 同步建置）✅ DONE

> **v1.1.0 更新**：D4 決策（全域 tenant_id）後，RLS 覆蓋範圍從 30+ iGaming 表擴展為 **77+ 所有表**，複雜度從 L 升級為 XL。

> **v1.3.0 實作完成**：Sprint 2 G11 已完成 RLS 政策部分。pg_partman 延後至 iGaming DDL 階段（iGaming 表尚不存在）。

**決策：與 G1 同步建置**（雙重保護同時上）

**實作摘要 (Sprint 2)：**
- `V5__rls_policies.sql`：建立 `smartadmin_app` 非超級用戶角色 + ENABLE RLS on 48 tables + CREATE POLICY tenant_isolation（session variable 驅動）
- `RlsSessionInterceptor.java`：MyBatis Interceptor，每次 SQL 前執行 `SET LOCAL app.current_tenant_id`（transaction-scoped，無連線池洩漏）
- `TenantProperties.Rls`：新增 `tenant.rls.enabled` 配置（預設 false）
- `TenantAutoConfiguration`：`@ConditionalOnProperty` 註冊 interceptor bean
- Flyway user 分離：`spring.flyway.user: postgres`（DDL 用超級用戶）
- **關鍵設計**：PostgreSQL 超級用戶永遠繞過 RLS，故建立 `smartadmin_app` LOGIN 角色（非超級用戶）供應用 DML
- **延後**：pg_partman 延後至 iGaming DDL 階段

**iGaming 需要：**
- **77+ 張表**的 RLS 政策（D4 全域化：45+ 現有表 + 32+ iGaming 新表）
- `CREATE POLICY ... USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT)`
- `app_user` / `app_admin` 角色設置
- `pg_partman` 自動分區（wallet_transaction daily、round weekly）

**SmartAdmin 現況：**
- PostgreSQL 已在使用 ✅
- RLS 已啟用於 48 張表 ✅（V5 migration）
- `smartadmin_app` 角色已建立 ✅
- `RlsSessionInterceptor` 已實作 ✅（gated by `tenant.rls.enabled`）
- **pg_partman 延後至 iGaming DDL 階段**

**已建置項目：**
1. ✅ RLS 啟用 SQL（`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`）— 48 表
2. ✅ 48 張 RLS policy 定義（`V5__rls_policies.sql`）
3. ✅ DB 角色創建（`smartadmin_app` — 非超級用戶）
4. ✅ MyBatis Interceptor：每次 query/update 前 `SET LOCAL app.current_tenant_id`（transaction-scoped）
5. ⏳ `pg_partman` 安裝 + partition 配置（延後至 iGaming DDL 階段）
6. ⏳ 自動 partition maintenance job（延後至 iGaming DDL 階段）

---

### G12: Testcontainers Kafka/Redis 擴展 ✅ DONE

**完成摘要**（v1.11.0）：

| 項目 | 內容 |
|------|------|
| **依賴** | `testcontainers-kafka` 添加至 `libs.versions.toml` + `build.gradle.kts`；`testcontainers-postgresql` + `awaitility` 同步添加 |
| **基礎類** | `AbstractIntegrationTestBase`：PostgreSQL 16 + Kafka (cp-kafka:7.6.1) + Redis 7 三容器；`@DynamicPropertySource` 自動注入 `smart.kafka.*` + `spring.data.redis.*` |
| **驗證** | `ContainerSmokeTest`（3 tests）：JDBC 連接 PostgreSQL、container port mapping Redis、bootstrap servers Kafka |
| **設計決策** | D1: Redis 用 `GenericContainer`（BOM 無一等公民模組）；D2: Kafka 用 `KafkaContainer`（Testcontainers 官方模組） |

**新增檔案**：
- `smartadmin-app/src/test/java/net/lab1024/sa/app/support/AbstractIntegrationTestBase.java`
- `smartadmin-app/src/test/java/net/lab1024/sa/app/support/ContainerSmokeTest.java`

---

## 新增缺口（v1.1.0 原始碼驗證發現）

### G1.5: 全域 tenant_id 數據遷移 【P0-Critical / M】✅ DONE

> 因 D4 決策（全域加 tenant_id）衍生的新缺口。

**問題**：所有現有表需新增 `tenant_id` 欄位，但現有數據沒有此欄位值。

**需要建置的項目：**
1. Flyway migration: `ALTER TABLE ... ADD COLUMN tenant_id BIGINT`（所有現有表）
2. 數據填充: `UPDATE ... SET tenant_id = 1`（default tenant_id for existing data）
3. 約束添加: `ALTER TABLE ... ALTER COLUMN tenant_id SET NOT NULL`（先填充後加約束）
4. 複合索引: 部分表需含 `tenant_id` 的複合索引（如 unique constraints 需加入 tenant_id）
5. BaseEntity 改造: 共用 Entity 基類加入 `tenantId` 欄位 — **已由 G0（Sprint 0.5）完成基類提取**
6. **UNIQUE constraint 改造**（與 G15 聯動）：需在 `tenant_id ADD COLUMN` 之後、`NOT NULL` 約束之前完成 (v1.2.0 補充)

**注意**：此缺口與 G1（Multi-Tenant interceptor）、G3（Flyway）、**G15（UNIQUE 改造）** 緊密耦合，應在 Sprint 2 同步完成。

---

### G2.3: 前端 ISO-8601 + 多租戶 UI 適配 【P1-High / M+】✅ DONE

> 因 D3 決策（API 時間格式改為 ISO-8601）+ G13（Sa-Token 多租戶）衍生的綜合前端需求。v1.2.0 擴展範圍，複雜度從 M 升為 M+。

**狀態**：✅ 已完成（v1.13.0, 2026-02-17）

**已建置項目：**
1. `src/lib/datetime-util.ts` — ISO-8601 解析/格式化工具（dayjs utc/timezone/customParseFormat plugins）
   - `formatDateTime(isoString)` → `"2026-02-14 18:30:00"`（顯示用）
   - `formatDate(isoString)` → `"2026-02-14"`
   - `toApiDateTime(dayjsValue)` → ISO-8601 字串（提交 API 用）
   - `parseApiDateTime(isoString)` → dayjs 實例（兼容舊格式 fallback）
2. `src/store/modules/system/tenant.ts` — 租戶 Pinia store（tenantId, timezone, tenantCode）
   - `setTenantInfo(data)` — 登入後設定租戶上下文 + localStorage 持久化
   - `clearTenantInfo()` — 登出時清除
3. `src/constants/local-storage-key-const.ts` — +3 tenant keys（TENANT_ID, TENANT_CODE, TENANT_TIMEZONE）
4. `src/lib/axios.ts` — request interceptor 注入 `X-Tenant-Id` + `X-Timezone` headers
5. `src/views/system/login/login.vue` — 登入成功後存儲租戶上下文（tenantId, timezone, tenantCode）
6. `src/store/modules/system/user.ts` — logout 時 `clearTenantInfo()`
7. `src/lib/default-time-ranges.ts` — +timezone-aware `createTimeRanges(tz)` composable
8. **24 個 list 頁面** — `#bodyCell` slot 加入 `formatDateTime(text)` 格式化時間欄位
   - support 模組（15 頁面）：operate-log, login-log, login-fail, change-log, feedback, file, heart-beat, config, job, deleted-job, message, reload, reload-result, help-doc-list, help-doc-view-record
   - business 模組（6 頁面）：goods, enterprise, enterprise-bank, enterprise-invoice, notice, notice-employee, notice-view-record
   - system 模組（3 頁面）：department, position, serial-number

**設計決策**：
- D1: axios interceptor 使用 `localRead` 而非 `useTenantStore()` 避免 Pinia 循環依賴
- D2: 保留原 `defaultTimeRanges` export 向後兼容，新增 `createTimeRanges(tz)` composable
- D3: DatePicker `@change` handlers（22 files）暫不修改，依賴後端 fallback 解析舊格式

**規格文件**：`docs/iGaming/implementation/g2.3-frontend-adaptation-spec.md`（906 行）

**原始需求（保留紀錄）：**

**問題 1（原 v1.1.0）**：API 時間格式從 `"2026-02-14 10:30:00"` 變為 `"2026-02-14T10:30:00+00:00"`，前端需全面適配。

**問題 2（v1.2.0 新增）**：多租戶認證需前端配合傳遞 tenant 資訊。

**影響範圍：**
1. 所有時間顯示元件的格式化邏輯 ✅
2. 表單提交的時間格式（DatePicker 等元件）— P2 deferred（後端 fallback 可解析）
3. 時區轉換邏輯（UTC → 用戶本地時區顯示）✅（後端 TenantTimezoneSerializer 已轉換）
4. 時間比較和計算邏輯 ✅（dayjs 原生支援 ISO-8601）
5. **前端 Login API 需傳 `tenantId` 參數**（G13 衍生）✅
6. **API 請求 Header 攜帶 tenant 資訊** ✅（X-Tenant-Id + X-Timezone）
7. **租戶切換 UI 邏輯**（如有多租戶管理需求）— P2 deferred

---

### G2.5: MySQL DDL → PostgreSQL 重寫 【P1-High / S】✅ DONE

> 原始碼驗證發現 `sa-admin/build/` 中存在 MySQL 語法 DDL 殘留。D7 決策重寫為 PostgreSQL。

> **v1.3.2 實作完成**：探索後發現 `sa-admin/` 是 v3.x 遺留目錄（不在 `settings.gradle.kts`），`build/` 內的 4 個 SQL 檔案為未追蹤的 build artifacts（`git ls-files` 回傳空，`.gitignore` 已覆蓋）。無需 PostgreSQL 重寫，直接刪除整個 `sa-admin/` 目錄。

**實作摘要：**
- `sa-admin/` 已刪除（v4.1.0 重構後的遺留目錄，僅含 `build/` 無 `src/`）
- 4 個 SQL 檔案分析：V1.0.11 wagering（無 Entity）、V1.1 liteflow（DB 已存在）、V1.2 rename（衝突）、V999 AI（無 Entity）
- LiteFlow tables 已由外部 DDL 建立，V2/V5/V6 Flyway migrations 正常引用
- Wagering/AI tables 將在 iGaming 開發時建立新的 Flyway migration

---

## 新增缺口（v1.2.0 深度驗證發現）

### G13: Sa-Token 多租戶認證適配【P0-Critical / M】✅ DONE

> **v1.2.0 新增**：原始碼驗證發現 Sa-Token 為單租戶模式，整個 codebase 僅 5 個檔案使用 `StpUtil`，無任何 tenant-aware 邏輯。多租戶環境下 Token 必須攜帶 `tenantId`，否則無法區分不同租戶用戶。
> **v1.2.1 更新**：D9 決策確認使用**獨立 StpLogic** — Admin（`StpAdminUtil`）和 Player（`StpPlayerUtil`）各自擁有完全獨立的 Token 體系。

**iGaming 需要：**
- 登入時 Token/Session 攜帶 `tenantId`
- Admin（後台管理）和 Player（玩家端）雙帳戶體系
- Tenant-scoped permission check（同一 permission 跨 tenant 獨立）
- 前端登入需傳 `tenantId` 參數

**SmartAdmin 現況（原始碼驗證）：**
- `TokenConfig.java`（smartadmin-common-token）：僅配置 `activeTimeout`，**零 tenant 相關邏輯**
- `LoginService.java`、`LoginController.java`：單一登入流程，無 tenant 區分
- `StpUtil` 使用僅限 5 個檔案（`LoginService`, `LoginController`, `EmployeeService`, `Level3ProtectConfigService`, `LoginServiceTest`）
- Sa-Token `application.yaml` 配置：**單帳戶體系**，無多帳號認證（`StpLogic`）

**決策 D9**：獨立 StpLogic（`StpAdminUtil` / `StpPlayerUtil` 各自獨立）

**缺口影響：** 阻塞 **Phase 0**（Multi-Tenant 無法獨立於認證體系運作）

**需要建置的項目：**
1. 登入流程注入 `tenantId`：`StpAdminUtil.getSession().set("tenantId", tenantId)` / `StpPlayerUtil.getSession().set("tenantId", tenantId)`
2. `TenantInterceptor` 從 Token/Session 提取 `tenantId` 設入 `TenantContext`（與 G1 聯動）
3. **獨立 StpLogic 體系**（D9 決策）：
   - `StpAdminUtil` + `StpAdminLogic extends StpLogic("admin")` — 後台管理端
   - `StpPlayerUtil` + `StpPlayerLogic extends StpLogic("player")` — 玩家端
   - 各自獨立的 token-name（如 `sa-admin-token` / `sa-player-token`）
4. 前端 Login API 增加 `tenantId` 參數
5. `@SaCheckPermission` 確認 tenant-scoped 行為（評估是否需自訂 `SaPermissionHandler`）
6. 現有 `StpUtil` 引用（5 個檔案）遷移到 `StpAdminUtil`

**關鍵檔案：**
- `smartadmin-common-token/.../TokenConfig.java` — 多帳號體系配置
- `smartadmin-system/.../LoginService.java` — 登入流程注入 tenantId
- `smartadmin-system/.../LoginController.java` — API 參數擴展

---

### G14: Virtual Thread + TenantContext 相容性策略【P0-Critical / S】✅ DONE

> **v1.2.0 新增**：SmartAdmin 已啟用 `spring.threads.virtual.enabled: true`（Java 21 Virtual Threads）。G1 計畫用 ThreadLocal 實現 TenantContext，需要明確跨線程傳遞策略。

**iGaming 需要：**
- TenantContext 在 @Async 任務、CompletableFuture、Kafka Consumer 中正確傳遞
- Virtual Thread 環境下 ThreadLocal 的正確使用策略

**SmartAdmin 現況（原始碼驗證）：**
- `application.yaml:34-35`：`spring.threads.virtual.enabled: true` ✅ **已啟用**
- 搜尋 `@Async` → 零結果（目前未使用 @Async，但 iGaming 將大量使用）
- 搜尋 `CompletableFuture` → 僅 `KafkaProducerService`/`KafkaProducerServiceImpl`（2 個檔案）
- 搜尋 `TaskDecorator`/`InheritableThreadLocal`/`ScopedValue` → **零結果**

**技術分析：**
- ✅ ThreadLocal 在 Virtual Thread 中**可正常使用**（每個 VT 有獨立 TL 實例，這是正確行為）
- ❌ `@Async` 和 `CompletableFuture` 的子任務**不會自動繼承** ThreadLocal → 需要 `TaskDecorator`
- ❌ Kafka Consumer 線程與 HTTP 請求線程不同 → 需要 Kafka Header 傳遞 tenantId
- ⚠️ Java 21 `ScopedValue` 仍是 Preview API，**不建議生產使用**

**缺口影響：** 阻塞 **G1**（TenantContext 設計決策）

**需要建置的項目：**
1. `TenantContext` 使用 `ThreadLocal`（確認可行，Virtual Thread 安全）
2. `TenantContextTaskDecorator` implements `TaskDecorator`（@Async 上下文傳遞）
3. `AsyncConfig` 註冊 TaskDecorator 到 `ThreadPoolTaskExecutor`（或 Virtual Thread executor）
4. Kafka `ProducerInterceptor` 注入 `tenantId` 到 Kafka Header
5. Kafka `ConsumerInterceptor` 從 Header 恢復 `TenantContext`
6. MDC logging 注入 `tenantId`（確保跨線程日誌可追蹤）

**關鍵檔案：**
- `smartadmin-app/.../application.yaml` — Virtual Thread 配置確認
- `smartadmin-common-mq/.../KafkaProducerServiceImpl.java` — Kafka Header 注入
- `smartadmin-common-mq/.../AbstractKafkaListener.java` — Kafka Header 恢復

---

### G15: UNIQUE Constraint + tenant_id 衝突改造【P0-Critical / M】（NEW v1.2.0）✅ DONE

> **實施完成** (2026-02-14)：`V4__unique_constraint_tenant.sql` — 11 個 UNIQUE 約束改為 `(tenant_id, ...)` 複合形式，3 個遺漏表補充 `tenant_id`（t_notice_visible_range, t_notice_view_record, t_help_doc_view_record），11 個冗餘索引清理。無 Java 程式碼變更。

> **v1.2.0 新增**：D4 決策全域加 `tenant_id` 後，現有表的 UNIQUE constraint 需要改為包含 `tenant_id` 的複合唯一索引。此外，`@TableId(type = IdType.AUTO)` 在多租戶環境下可能產生 ID 語意問題。
> **v1.2.1 更新**：D8 決策確認 **iGaming 新表改用 Snowflake**（`IdType.ASSIGN_ID`），現有表保留 `IdType.AUTO`。

**問題 1：UNIQUE Constraint 衝突**
- 例如 `t_employee.login_name` 如果是 UNIQUE → 多租戶下不同 tenant 的員工可能有相同 login_name
- 需要改為 `UNIQUE(tenant_id, login_name)` 複合唯一索引

**問題 2：主鍵策略 ~~評估~~ → 已決策 D8**
- 現有 Entity 使用 `@TableId(type = IdType.AUTO)`（DB auto-increment）→ **保留不變**
- **iGaming 新表**使用 `@TableId(type = IdType.ASSIGN_ID)`（MyBatis-Plus Snowflake）→ **D8 決策**
- 理由：Snowflake ID 避免跨 tenant ID 語意洩漏，且 iGaming 新表無歷史遷移負擔

**SmartAdmin 現況（原始碼驗證）：**
- SQL DDL 僅存在於 `sa-admin/build/`（MySQL 殘留），**非正式 schema**
- UNIQUE constraints 定義在實際 PostgreSQL DB 中，需 DBA 級別審查
- `@TableId(type = IdType.AUTO)` 確認在 `EmployeeEntity.java:21`

**缺口影響：** 阻塞 **G1.5**（數據遷移），UNIQUE constraint 改造必須在 `tenant_id ADD COLUMN` 之後、`NOT NULL` 約束之前

**需要建置的項目：**
1. 全量審查現有 PostgreSQL DB schema 的 UNIQUE constraints（`\d+ table_name` 或 `pg_indexes` 查詢）
2. 編製需要改造為 `(tenant_id, original_column)` 複合唯一索引的清單
3. Flyway migration script 重建 UNIQUE constraints（DROP + CREATE）
4. ~~評估 `IdType.AUTO` → `IdType.ASSIGN_ID`（Snowflake）的必要性~~ → **已決策 D8：iGaming 新表用 Snowflake，現有表保留 AUTO**
5. 編製受影響的 Service 層業務邏輯（如重複檢查 SQL 需加 `AND tenant_id = ?`）
6. iGaming BaseEntity 配置 `@TableId(type = IdType.ASSIGN_ID)`（D8 決策實施）

---

### G16: Argon2id 參數強化（iGaming 合規）✅ DONE 【P1-High / S】

**狀態**：✅ 已完成（v1.9.0, 2026-02-17）

**已建置項目：**
1. `Argon2Properties`（`@ConfigurationProperties(prefix = "smart.security.argon2")`）— YAML 可配置參數，預設值匹配 Spring Security 5.8
2. `PasswordEncryptService` 參數化改造：constructor injection `Argon2Properties`，取代硬編碼 `defaultsForSpringSecurity_v5_8()`
3. `PasswordEncryptService.needsUpgrade()` — 解析 Argon2 hash header (`$argon2id$v=19$m=...,t=...,p=...$`) 比對當前配置參數
4. `SecurityPasswordService.needsPasswordHashUpgrade()` — 業務層便捷方法
5. `LoginService` lazy migration（D10 決策）：登入成功後自動偵測舊參數 hash 並重新編碼
6. `application.yaml` 配置：`smart.security.argon2` with `m=65536, t=3, p=4`

**建置位置**：`smartadmin-common-security` + `smartadmin-support-securityprotect` + `smartadmin-system` + `application.yaml`

**設計決策**：
- D1: YAML 可配置（`Argon2Properties`），預設值匹配 Spring Security 5.8 向後兼容
- D2: 保留 `@Service` 在 `PasswordEncryptService`（向後兼容 component scanning）
- D3: 解析 Argon2 hash header 判斷參數版本（無需額外 DB 欄位）
- D4: Lazy migration 整合在 `LoginService`（iGaming `PlayerLoginService` 於 Phase 2 沿用同模式）

---

## 建置順序（依賴鏈 Critical Path）

```
Sprint 0.5: 基礎提取（G1+G2 的必要前置）✅ ALL DONE
└── G0: BaseEntity 共用基類提取 ✅ ──── SmartAdminBaseEntity (tenantId + OffsetDateTime + deleted)
    ├── 定義 SmartAdminBaseEntity（tenantId + createTime/updateTime as OffsetDateTime + deleted）
    └── MybatisPlusFillHandler 擴展（tenantId 自動填充準備）

Sprint 1: 骨架 + 時間遷移（依賴 Sprint 0.5 BaseEntity）✅ ALL DONE
├── G9: 8 個 iGaming Module 腳手架 ✅ ── 8 模組、55 模組總計
├── G2: OffsetDateTime Big-bang 遷移 ✅ ── XXL→M（G0 已完成 95%）
│   ├── Phase 1: 基礎設施層（5 檔：FillHandler + DateUtil + JsonConfig 6 元件改造）
│   ├── Phase 2: Support 模組（78 檔）
│   ├── Phase 3: Business 模組（35 檔）
│   └── Phase 4: API 層 + 測試驗證（13 檔）+ SmartDateFormatterEnum 補充
├── G14: Virtual Thread + TenantContext 策略確認 ✅ ── 設計決策完成
└── G2.5: MySQL DDL → PostgreSQL 重寫 ✅ ── sa-admin/ 刪除（build artifacts）

Sprint 2: 租戶全域化 + 認證 + 資料庫基礎（D4 全域 tenant_id）✅ ALL DONE
├── G3: Flyway baseline ✅ ──────────────── V1-V7 migrations (baseline → idempotent_key)
├── G1: Multi-Tenant interceptor ✅ ────────── TenantContext + SmartTenantLineHandler + tenantId 自動填充
│   ├── TenantContext + TenantInterceptor + TaskDecorator（G14 實現）
│   ├── TenantLineInnerInterceptor
│   └── Kafka Header 注入/恢復（G14 實現）
├── G13: Sa-Token 多租戶認證適配 ✅ ────── StpAdminUtil/StpPlayerUtil + session tenant binding
├── G1.5: 現有數據 tenant_id 填充 ✅ ────── V2__add_tenant_id.sql default 填充 + NOT NULL
├── G15: UNIQUE Constraint 改造 ✅ ────── V4__unique_constraint_tenant.sql 複合唯一索引
└── G11: RLS（77+ 表全域）✅ ────────── V5__rls_policies.sql DB 層租戶隔離

Sprint 3: 事件 + 加密 ✅ DONE
├── G4: DomainEvent + 冪等框架 ✅ ────── 事件驅動的基礎
└── G5: PII 加密 + Blind Index ✅ ────── AES-256-GCM + HMAC-SHA256 Blind Index

Sprint 4: 金融 + 規則引擎 ✅ ALL DONE
├── G6: 財務精度工具 ✅ ────────────────── DECIMAL(19,4) + HALF_EVEN
├── G7: Resilience4j ✅ ──────────────────── resilience4j-spring-boot3 v2.2.0 + PSP/GP config
├── G8: LiteFlow 配置整合 ✅ ────────────── 啟用 + SQL 規則存儲配置
└── G16: Argon2id 參數強化 ✅ ────────────── m=65536, t=3, p=4 + lazy migration

Sprint 5: 品質門禁
├── G10: ArchUnit iGaming 規則 ✅ ────────── 13 boundary rules + BUSINESS_CLASSES patch
├── G12: Testcontainers Kafka/Redis ✅ ─────── PostgreSQL + Kafka + Redis 三容器基礎類
└── G2.3: 前端 ISO-8601 + 多租戶 UI 適配 ✅ ── datetime-util.ts + tenant store + 24 list pages
```

**Sprint 間不可跳過**：Sprint 0.5 → Sprint 1 → Sprint 2 為嚴格順序依賴。Sprint 3 依賴 Sprint 2（Flyway 管理 idempotent_key/outbox 表）。Sprint 4 可與 Sprint 3 部分平行。
**注意**：Sprint 0.5 為 v1.2.0 新增的前置 Sprint，工作量約 1-2 天。Sprint 1-2 工程量因 D4 決策（全域 tenant_id）大幅增加，Sprint 2 含新增的 G13/G15，可能需要拉長或拆分為 2a/2b。

---

## 已決策事項

| # | 問題 | 決策 | 備註 |
|---|------|------|------|
| D1 | Multi-Tenant 策略 | ✅ 雙重保護同時上 | G1 + G11 合併 Sprint 2 |
| D2 | OffsetDateTime 遷移策略 | ✅ Big-bang 一次性遷移 | 131 檔 467 次一次完成，不維護共存期 |
| D3 | API 時間格式 | ✅ ISO-8601 新格式 | 前端必須同步適配 `2026-02-14T10:30:00+00:00` |
| D4 | Tenant 範圍 | ✅ 全域加 tenant_id | 所有現有表 + iGaming 新表，G1 升為 XXL |
| D5 | DEK/KEK 管理 | ✅ Config 簡化方案 | 未來可升級 KMS/Vault |
| D6 | Flyway 範圍 | ✅ 全專案引入 | baseline + TIMESTAMPTZ + tenant_id + iGaming DDL |
| D7 | MySQL DDL 處理 | ✅ 重寫為 PostgreSQL | 保留設計意圖，納入 Flyway baseline |
| D8 | 主鍵策略 | ✅ iGaming 新表 Snowflake | 現有表保留 `IdType.AUTO`，iGaming 用 `IdType.ASSIGN_ID` |
| D9 | Sa-Token 多帳戶體系 | ✅ 獨立 StpLogic | `StpAdminUtil` / `StpPlayerUtil` 完全隔離 |
| D10 | Argon2id 密碼遷移 | ✅ Lazy migration | 下次登入時重新 hash，無需批次遷移 |
| D11 | LiteFlow 啟用策略 | ✅ 直接啟用 + 空規則 | 不需 feature flag，空規則不影響現有功能 |

## 仍需確認的事項（實作階段再議）

1. ~~LiteFlow 全局啟用~~ → **已決策 D11：直接啟用 + 空規則，不需 feature flag 隔離**
2. ~~OffsetDateTime 全面遷移的前端影響~~ → **已決策 D3：ISO-8601 新格式，前端必須適配**
3. ~~Sa-Token 多帳戶體系~~ → **已決策 D9：獨立 StpLogic**（`StpAdminUtil` / `StpPlayerUtil` 各自獨立，Admin 和 Player 完全隔離的 Token 體系）
4. ~~主鍵策略~~ → **已決策 D8：iGaming 新表改用 Snowflake**（`IdType.ASSIGN_ID`），現有表保留 `IdType.AUTO`
5. ~~Argon2id 密碼遷移策略~~ → **已決策 D10：Lazy migration**（用戶下次登入時重新 hash，無需批次遷移）

> **v1.2.1 更新**：所有待確認事項均已決策完畢（D1-D11），無剩餘未決項目。

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

**20 個缺口項目** — **20/20 全部完成（100%）** :
- **P0 (Critical)**: 11 項 — 全部 ✅ DONE（G0, G1, G1.5, G2, G3, G4, G5, G11, G13, G14, G15）
- **P1 (High)**: 7 項 — 全部 ✅ DONE（G2.3, G6, G7, G8, G9, G16, G2.5）
- **P2 (Medium)**: 2 項 — 全部 ✅ DONE（G10, G12）

### 風險矩陣（v1.2.0 更新）

| Gap | 項目 | v1.0.0 評估 | v1.1.0 評估 | v1.2.0 評估 | 變化原因 |
|-----|------|------------|------------|------------|---------|
| **G1** | Multi-Tenant | XL | **XXL** | **XXL** | D4 全域 tenant_id，所有現有表 + Entity |
| **G2** | OffsetDateTime | XL | **XXL** | **XXL** | 131 檔 467 次 + JsonConfig 6 元件 + SmartDateFormatterEnum |
| **G11** | RLS | L | **XL** | **XL** | 77+ 表全域 RLS（原 30+） |
| **G3** | Flyway | L | **L+** | **L+** | 額外清理 MySQL DDL + 全域 tenant_id migration |
| **G8** | LiteFlow | M | **M-** | **M-** | 程式碼已就緒，只需配置整合 |
| G1.5 | 數據遷移 | — | **M** | **M** | D4 衍生：現有數據 default tenant_id + UNIQUE 改造聯動 |
| G2.3 | 前端適配 | — | **M** | **M+** | D3+G13 衍生：ISO-8601 + 多租戶 UI（v1.2.0 範圍擴展） |
| G2.5 | MySQL DDL | — | **S** | **S** | 原始碼驗證發現 MySQL 殘留 |
| **G0** | BaseEntity 提取 | — | — | **M** (NEW) | v1.2.0：G1+G2 前置，減少 ~50% Entity 修改量 |
| **G13** | Sa-Token 多租戶 | — | — | **M** (NEW) | v1.2.0：認證體系無 tenant-aware = 多租戶無法登入 |
| **G14** | Virtual Thread 相容 | — | — | **S** (NEW) | v1.2.0：設計決策，影響 G1 TenantContext + @Async 傳遞 |
| **G15** | UNIQUE 改造 | — | — | **M** (NEW) | v1.2.0：DB schema 級別 UNIQUE constraint 審查 + 改造 |
| **G16** | Argon2id 強化 | — | — | **S** (NEW) | v1.2.0：安全參數差異（m=16K vs m=64K） |

**最大風險項**（v1.2.0 排序）：
1. **G2 + G1 雙 XXL 疊加** — 131 檔 OffsetDateTime 遷移 + 45+ Entity 加 tenant_id = 接近整個 codebase 的重寫（G0 BaseEntity 提取可降低約 50% 重複工作）
2. **G1+G11+G13+G15 租戶全鏈路** — MyBatis interceptor + 77+ 表 RLS + Sa-Token 多帳戶 + UNIQUE constraint 改造 + 現有數據遷移
3. **前端適配** — ISO-8601 + 全域 tenant context + 多租戶 UI 需前端團隊同步（跨團隊依賴，v1.2.0 範圍擴展）
4. **G14 Virtual Thread + ThreadLocal** — 策略決策影響 G1 TenantContext 實現和所有 @Async/Kafka 場景
5. **G9 Module 腳手架** — 8 個新模組是所有業務程式碼的載體

**已有 15 項可直接複用**（全部經原始碼驗證確認），SmartAdmin 的基礎設施覆蓋率約 43%（15 可用 / 35 總需求），剩餘 57% 需要新建。

### 需要跨團隊協調的事項

1. ~~**前端團隊**：ISO-8601 時間格式適配 + 多租戶 UI 適配~~ → ✅ G2.3 已完成（datetime-util.ts + tenant store + 24 list pages）
2. ~~**DBA/運維**：全域 tenant_id DDL 遷移策略 + UNIQUE constraint 審查~~ → ✅ G1.5 + G15 已完成
3. **QA 團隊**：Big-bang 遷移的回歸測試計畫（D2 決策）— 待 QA 排程
4. ~~**安全團隊**：Argon2id 參數合規確認 + PII 加密範圍確認~~ → ✅ G16 + G5 已完成

---

## 關鍵檔案索引

| 檔案 | 用途 | 受影響的缺口 |
|------|------|-------------|
| `smartadmin-common-mybatis/.../MybatisPlusConfig.java` | 需添加 TenantLineInnerInterceptor（全域） | G1 |
| `smartadmin-common-mybatis/.../MybatisPlusFillHandler.java` | LocalDateTime → OffsetDateTime（填充源頭） | G2 |
| `smartadmin-common-core/.../SmartLocalDateUtil.java` | 日期工具類重構 / 建立 OffsetDateTime 等效 | G2 |
| `smartadmin-common-web/.../JsonConfig.java` | Jackson OffsetDateTime ISO-8601 序列化 | G2, D3 |
| `gradle/libs.versions.toml` | 添加 Flyway, Resilience4j, testcontainers-kafka | G3, G7, G12 |
| `settings.gradle.kts` | 新增 8 個 iGaming 模組 | G9 |
| `smartadmin-common-mq/.../AbstractKafkaListener.java` | 擴展為 IdempotentKafkaListener 基類 | G4 |
| `smartadmin-common-security/` | 擴展 AES-256-GCM + blind index | G5 |
| `smartadmin-support-liteflow/` 配置 | 建立 application.yaml 配置區塊 | G8 |
| `smartadmin-app/src/main/resources/dev/application.yaml` | Flyway + LiteFlow + tenant 配置 | G3, G8, G1 |
| `sa-admin/build/resources/main/db/migration/*.sql` | MySQL DDL 殘留，需重寫或清理 | G2.5 |
| 所有 `*Entity.java`（45+ 檔案） | 改繼承 BaseEntity + LocalDateTime → OffsetDateTime | G0, G1, G2 |
| 所有 `*VO.java` / `*Form.java`（30+ 檔案） | LocalDateTime → OffsetDateTime | G2 |
| `smartadmin-common-token/.../TokenConfig.java` | 多帳號體系配置 + tenant session 注入 | G13 |
| `smartadmin-system/.../LoginService.java` | 登入流程注入 tenantId 到 SaSession | G13 |
| `smartadmin-common-mq/.../KafkaProducerServiceImpl.java` | Kafka Header 注入 tenantId | G14 |
| `smartadmin-common-security/.../PasswordEncryptService.java` | Argon2id 參數化改造 | G16 |
| `smartadmin-common-core/.../SmartDateFormatterEnum.java` | 新增 ISO-8601 formatter | G2 |
| `smartadmin-support-operatelog/.../OperateLogVO.java` | @JsonFormat timezone 修正 | G2 |
