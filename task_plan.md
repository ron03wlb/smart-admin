# Task Plan: Sprint 4 - Agent Commission, VIP Auto-upgrade, Self-Exclusion

**專案**: SmartAdmin iGaming Integration
**任務**: 實現 P1 核心業務功能（代理佣金、VIP 升級、自我排除）
**分支**: `feature/igaming-p1-features`
**日期**: 2026-03-25
**狀態**: 準備開始（Sprint 3 已完成 100%）

**進度摘要**: Sprint 3 ✅ → Sprint 4 🚀

---

## Goal

實現 iGaming 平台的三個 P1 核心業務功能：
1. 代理佣金計算與分佈式結算系統
2. VIP 等級自動升級引擎
3. 玩家自我排除（自我限制）功能

**成功標準**:
- Database schema 完整（新增 8-10 張表）
- 業務邏輯實現並通過測試（80%+ 覆蓋率）
- Integration tests 通過（預估 40+ 測試）
- 文檔完整（ADR + API 文檔）

---

## Current Phase

**Phase 1: 代理佣金系統 - Database Schema + Entity + Dao**
- Database Schema: ✅ COMPLETED
- Entity 類創建: ⏳ IN PROGRESS
- Dao 接口創建: ⏳ PENDING

---

## Sprint 3 成果回顧

**已完成工作** (100%):
- ✅ Database Schema V001-V012 (23 張表，2,400+ 行 SQL)
- ✅ LiteFlow 規則引擎集成（Turnover Calculation）
- ✅ Withdrawal Risk Check Service
- ✅ Integration Tests: 33/33 通過 (100%)
- ✅ Boolean Type Migration ADR 文檔

**測試覆蓋率**:
- FlywayMigrationIntegrationTest: 8/8 (100%)
- PlayerRegistrationJourneyIntegrationTest: 10/10 (100%)
- GameBettingJourneyIntegrationTest: 15/15 (100%)

**Git 提交**: 分支領先遠端 1 個提交（待推送）

---

## Phases

### Phase 0: Sprint 準備與架構設計 ✓ in_progress

**目標**: 完成 Sprint 4 啟動準備，設計三個功能的架構方案

**任務清單**:
- [x] 創建 Sprint 4 task_plan.md
- [ ] 讀取 iGaming 文檔（代理、VIP、自我排除需求）
- [ ] 設計 Database Schema（8-10 張新表）
- [ ] 設計 Service 層架構
- [ ] 確定技術選型（Snail Job、Redis、LiteFlow）
- [ ] 創建新分支 `feature/igaming-p1-features`

**完成標準**:
- [ ] Database Schema 設計文檔（表結構、關係圖）
- [ ] 架構決策記錄（ADR）
- [ ] 技術選型確認
- [ ] 開發順序規劃

**預計時間**: 2-3 小時

**Status**: in_progress

---

### Phase 1: 代理佣金系統 - Database Schema + Entity + Dao ✓ in_progress

**目標**: 創建代理佣金系統的完整數據層（Database + Entity + Dao）

#### 1.1 Database Schema ✅ COMPLETED (2026-03-25 18:30)

**預估表數量**: 5 張表
1. ✅ `t_agent_relationship` - 代理關係樹（玩家-代理多對一）
2. ✅ `t_agent_commission_config` - 佣金配置（層級、比例、產品類型）
3. ✅ `t_agent_commission_record` - 佣金記錄（結算週期、金額、狀態）
4. ✅ `t_agent_commission_settlement` - 結算批次（防重複結算）
5. ✅ `t_agent_performance_snapshot` - 代理業績快照（月度/週度）

**已完成任務**:
- [x] 創建 V013__create_agent_tables.sql
- [x] 定義代理層級結構（最多 5 層）
- [x] 設計佣金計算規則（有效投注額、負盈利抽佣）
- [x] 添加防重複結算機制（分佈式鎖）
- [x] 創建必要的索引和約束（16 個索引，5 個外鍵）
- [x] FlywayMigrationIntegrationTest 更新並通過（8/8 tests）

**Git 提交**: `b44fced3` - feat(igaming): add V013 migration for Agent Commission System

---

#### 1.2 Entity 類創建 ⏳ IN PROGRESS

**目標**: 創建 5 個 Entity 類對應 5 張資料庫表

**Entity 類清單**:
- [ ] `AgentRelationshipEntity` - 代理關係實體
- [ ] `AgentCommissionConfigEntity` - 佣金配置實體
- [ ] `AgentCommissionRecordEntity` - 佣金記錄實體
- [ ] `AgentCommissionSettlementEntity` - 結算批次實體
- [ ] `AgentPerformanceSnapshotEntity` - 業績快照實體

**技術要求**:
- MyBatis Plus 註解：`@TableName`, `@TableId`, `@TableField`
- Lombok 註解：`@Data`, `@EqualsAndHashCode(callSuper = true)`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- 字段類型：`BigDecimal` for NUMERIC(19,4), `OffsetDateTime` for TIMESTAMP WITH TIME ZONE
- Boolean 字段：使用 `Boolean` 類型（PostgreSQL 原生 BOOLEAN）
- 包位置：`net.lab1024.sa.igaming.agent.commission.domain.entity`
- 繼承：`extends SmartAdminBaseEntity`（提供 tenantId, deleted, createTime, updateTime）
- 主鍵：`@TableId(type = IdType.AUTO)` for BIGSERIAL

**完成標準**:
- [ ] 所有 5 個 Entity 類創建完成
- [ ] 字段映射正確（與資料庫表一致）
- [ ] Spotless 格式檢查通過
- [ ] 編譯無錯誤

---

#### 1.3 Dao 接口創建 ⏳ PENDING

**目標**: 創建 5 個 Dao 接口繼承 BaseMapper

**Dao 接口清單**:
- [ ] `AgentRelationshipDao` extends `BaseMapper<AgentRelationshipEntity>`
- [ ] `AgentCommissionConfigDao` extends `BaseMapper<AgentCommissionConfigEntity>`
- [ ] `AgentCommissionRecordDao` extends `BaseMapper<AgentCommissionRecordEntity>`
- [ ] `AgentCommissionSettlementDao` extends `BaseMapper<AgentCommissionSettlementEntity>`
- [ ] `AgentPerformanceSnapshotDao` extends `BaseMapper<AgentPerformanceSnapshotEntity>`

**技術要求**:
- MyBatis Plus BaseMapper 繼承
- `@Mapper` 註解
- 包位置：`net.lab1024.sa.igaming.agent.commission.dao`

**完成標準**:
- [ ] 所有 5 個 Dao 接口創建完成
- [ ] MyBatis Plus 自動配置生效
- [ ] 編譯無錯誤

---

**Phase 1 整體完成標準**:
- [x] Flyway migration 通過
- [x] 所有表創建成功
- [x] 外鍵約束正確
- [x] 索引優化完成
- [ ] 所有 Entity 類創建完成
- [ ] 所有 Dao 接口創建完成
- [ ] 編譯通過，無錯誤

**預計時間**: 6-8 小時（已用 2 小時，剩餘 4-6 小時）

**Status**: in_progress (Database Schema ✅, Entity ⏳, Dao ⏳)

---

### Phase 2: 代理佣金系統 - Business Logic

**目標**: 實現代理佣金計算與結算業務邏輯

**核心 Service 類**:
1. `AgentRelationshipService` - 代理樹管理
2. `AgentCommissionCalculationService` - 佣金計算引擎
3. `AgentCommissionSettlementService` - 分佈式結算
4. `AgentCommissionQueryService` - 佣金查詢統計

**任務清單**:
- [ ] 實現代理樹構建與查詢
- [ ] 實現多層級佣金計算（遞歸計算）
- [ ] 實現結算防重複機制（Redis + DB）
- [ ] 實現結算任務調度（Snail Job）
- [ ] 添加佣金凍結/解凍邏輯
- [ ] 創建 Unit Tests（80%+ 覆蓋率）

**完成標準**:
- [ ] 所有 Service 類實現完成
- [ ] Unit Tests 通過（80%+ 覆蓋率）
- [ ] 佣金計算準確性驗證
- [ ] 防重複結算驗證

**預計時間**: 8-12 小時

**Status**: pending

---

### Phase 3: 代理佣金系統 - Integration Tests

**目標**: 創建端到端集成測試，驗證代理佣金完整流程

**測試場景**:
1. 玩家註冊並綁定代理
2. 玩家投注產生有效流水
3. 觸發佣金計算（多層級）
4. 執行結算並發放佣金
5. 驗證防重複結算

**任務清單**:
- [ ] 創建 `AgentCommissionJourneyIntegrationTest`
- [ ] 測試單層代理佣金計算
- [ ] 測試多層代理佣金計算（3 層）
- [ ] 測試防重複結算機制
- [ ] 測試佣金凍結/解凍
- [ ] 測試結算任務調度

**完成標準**:
- [ ] Integration Tests 通過（預估 10-12 個測試）
- [ ] 佣金計算準確性驗證
- [ ] 性能測試通過（1000+ 玩家）

**預計時間**: 4-6 小時

**Status**: pending

---

### Phase 4: VIP 等級自動升級 - Database Schema

**目標**: 創建 VIP 系統的數據庫表結構

**預估表數量**: 2-3 張表
1. `t_vip_level_config` - VIP 等級配置（升級條件、權益）
2. `t_player_vip_history` - 玩家 VIP 升級歷史
3. `t_vip_reward_record` - VIP 升級獎勵記錄

**任務清單**:
- [ ] 創建 V014__create_vip_tables.sql
- [ ] 定義 VIP 等級（Bronze → Diamond, 10 個等級）
- [ ] 設計升級條件（累計投注額、累計存款、活躍天數）
- [ ] 設計降級規則（可選）
- [ ] 添加 VIP 權益配置（返水比例、生日禮金）

**完成標準**:
- [ ] Flyway migration 通過
- [ ] 所有表創建成功
- [ ] VIP 等級配置種子數據

**預計時間**: 2-3 小時

**Status**: pending

---

### Phase 5: VIP 等級自動升級 - Business Logic

**目標**: 實現 VIP 自動升級業務邏輯

**核心 Service 類**:
1. `VipLevelConfigService` - VIP 等級配置管理
2. `VipAutoUpgradeService` - 自動升級引擎
3. `VipRewardDistributionService` - 升級獎勵發放

**任務清單**:
- [ ] 實現 VIP 升級條件檢查（累計投注額、存款）
- [ ] 實現自動升級任務（Snail Job 每日執行）
- [ ] 實現升級獎勵發放（獎金、返水）
- [ ] 實現降級邏輯（可選）
- [ ] 添加升級通知（Kafka Event）
- [ ] 創建 Unit Tests（80%+ 覆蓋率）

**完成標準**:
- [ ] 所有 Service 類實現完成
- [ ] Unit Tests 通過（80%+ 覆蓋率）
- [ ] 升級條件準確性驗證

**預計時間**: 6-8 小時

**Status**: pending

---

### Phase 6: VIP 等級自動升級 - Integration Tests

**目標**: 創建端到端集成測試，驗證 VIP 升級完整流程

**測試場景**:
1. 玩家達到升級條件
2. 自動升級任務執行
3. VIP 等級升級成功
4. 升級獎勵自動發放
5. 驗證升級歷史記錄

**任務清單**:
- [ ] 創建 `VipAutoUpgradeJourneyIntegrationTest`
- [ ] 測試單一條件升級（累計投注額）
- [ ] 測試多條件升級（投注額 + 存款）
- [ ] 測試升級獎勵發放
- [ ] 測試降級邏輯（如果實現）
- [ ] 測試升級通知事件

**完成標準**:
- [ ] Integration Tests 通過（預估 8-10 個測試）
- [ ] 升級邏輯準確性驗證

**預計時間**: 3-4 小時

**Status**: pending

---

### Phase 7: 自我排除 - Database Schema

**目標**: 創建自我排除系統的數據庫表結構

**預估表數量**: 2 張表
1. `t_self_exclusion_request` - 自我排除請求（類型、期限、狀態）
2. `t_self_exclusion_history` - 限制歷史記錄（啟用、解除）

**任務清單**:
- [ ] 創建 V015__create_self_exclusion_tables.sql
- [ ] 定義限制類型（存款、投注、登入、完全封鎖）
- [ ] 設計冷靜期規則（24小時、7天、30天、永久）
- [ ] 設計解除審核流程
- [ ] 添加合規性審計字段

**完成標準**:
- [ ] Flyway migration 通過
- [ ] 所有表創建成功
- [ ] 限制類型種子數據

**預計時間**: 2-3 小時

**Status**: pending

---

### Phase 8: 自我排除 - Business Logic

**目標**: 實現自我排除業務邏輯

**核心 Service 類**:
1. `SelfExclusionRequestService` - 自我排除請求管理
2. `SelfExclusionEnforcementService` - 限制執行引擎
3. `SelfExclusionReviewService` - 解除審核服務

**任務清單**:
- [ ] 實現自我排除請求創建
- [ ] 實現限制執行邏輯（攔截器）
- [ ] 實現冷靜期管理
- [ ] 實現解除審核流程
- [ ] 添加限制通知（Email + SMS）
- [ ] 創建 Unit Tests（80%+ 覆蓋率）

**完成標準**:
- [ ] 所有 Service 類實現完成
- [ ] Unit Tests 通過（80%+ 覆蓋率）
- [ ] 限制執行驗證

**預計時間**: 6-8 小時

**Status**: pending

---

### Phase 9: 自我排除 - Integration Tests

**目標**: 創建端到端集成測試，驗證自我排除完整流程

**測試場景**:
1. 玩家創建自我排除請求
2. 限制立即生效（存款、投注、登入）
3. 冷靜期內無法解除
4. 冷靜期後申請解除審核
5. 審核通過後限制解除

**任務清單**:
- [ ] 創建 `SelfExclusionJourneyIntegrationTest`
- [ ] 測試存款限制
- [ ] 測試投注限制
- [ ] 測試登入限制
- [ ] 測試冷靜期管理
- [ ] 測試解除審核流程

**完成標準**:
- [ ] Integration Tests 通過（預估 8-10 個測試）
- [ ] 限制執行準確性驗證
- [ ] 合規性驗證

**預計時間**: 3-4 小時

**Status**: pending

---

### Phase 10: 文檔與提交

**目標**: 完成所有文檔並提交代碼

**任務清單**:
- [ ] 創建 ADR 文檔（3 個功能）
- [ ] 創建 API 文檔（Knife4j）
- [ ] 更新 README.md
- [ ] 創建 PR 描述
- [ ] 代碼審查準備
- [ ] 提交所有代碼

**完成標準**:
- [ ] 所有文檔完整
- [ ] PR 創建成功
- [ ] CI/CD 測試通過

**預計時間**: 2-3 小時

**Status**: pending

---

## Key Questions

### 代理佣金系統
1. **代理層級限制**: 最多支持幾層代理？（建議 5 層）
2. **佣金計算基礎**: 使用有效投注額還是負盈利？（建議負盈利抽佣）
3. **結算週期**: 每日、每週、每月？（建議每週）
4. **防重複結算**: 使用分佈式鎖還是資料庫唯一約束？（建議 Redis + DB）

### VIP 等級系統
5. **VIP 等級數量**: 多少個等級？（建議 10 個: Bronze → Diamond）
6. **升級條件**: 僅投注額還是多條件？（建議多條件: 投注額 + 存款 + 活躍天數）
7. **降級機制**: 是否實現降級？（可選）
8. **升級獎勵**: 即時發放還是延遲發放？（建議即時發放）

### 自我排除系統
9. **限制類型**: 支持哪些類型？（建議: 存款、投注、登入、完全封鎖）
10. **冷靜期**: 支持哪些時長？（建議: 24小時、7天、30天、永久）
11. **解除審核**: 誰負責審核？（建議: 合規團隊）
12. **合規要求**: 是否需要符合特定監管要求？（需確認）

---

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| 使用 Snail Job 調度佣金結算 | 分佈式任務調度，支持失敗重試和監控 |
| 使用 Redis 防重複結算 | 高性能分佈式鎖，配合資料庫唯一約束雙重保障 |
| VIP 升級使用累計值而非當期值 | 避免降級/升級頻繁切換，提升玩家體驗 |
| 自我排除使用攔截器執行限制 | 統一限制邏輯，易於維護和審計 |

---

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| （暫無錯誤） | - | - |

---

## Progress Log

**2026-03-25 14:10** - Sprint 4 啟動
- 創建 task_plan.md（Sprint 4）
- 歸檔 Sprint 3 規劃文件（task_plan_sprint3.md）
- 準備開始 Phase 0: 架構設計

---

## Notes

**Sprint 4 總預估時間**: 10-14 天（80-112 小時）
- 代理佣金系統: 4-5 天
- VIP 等級系統: 3-4 天
- 自我排除系統: 3-4 天
- 文檔與提交: 0.5 天

**技術債務**:
- Phase 1-3: 代理佣金系統需要考慮性能優化（代理樹遞歸計算）
- Phase 4-6: VIP 升級需要考慮大批量玩家升級的性能問題
- Phase 7-9: 自我排除需要考慮監管合規性（不同地區要求不同）

**依賴項**:
- Snail Job 排程系統（需配置）
- Redis 分佈式鎖（已有 Redisson）
- Kafka 事件通知（已有配置）

**文件組織**:
- Planning files: 專案根目錄（task_plan.md, findings.md, progress.md）
- Migration scripts: `smartadmin-igaming-integration/src/main/resources/db/migration/`
- Service classes: `smartadmin-igaming-{module}/src/main/java/net/lab1024/sa/igaming/{module}/`
- Test files: `smartadmin-igaming-integration/src/test/java/`

**Git 策略**:
- 創建新分支 `feature/igaming-p1-features`
- 每個 Phase 完成後提交一次
- 最後創建 PR 合併到主分支
