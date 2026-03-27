# Task Plan: Sprint 4 - Agent Commission, VIP Auto-upgrade, Self-Exclusion

**專案**: SmartAdmin iGaming Integration
**任務**: 實現 P1 核心業務功能（代理佣金、VIP 升級、自我排除）
**分支**: `feature/igaming-p1-features`
**日期**: 2026-03-26
**狀態**: 進行中（Phase 1-7 完成，Phase 8 準備開始）

**進度摘要**:
- ✅ Phase 1-3: 代理佣金系統 100% 完成（Database + Business Logic + Tests）
- ✅ Phase 4-6: VIP 升級系統 100% 完成（Database + Business Logic + Tests）
- ✅ Phase 7: 自我排除 Database Schema 100% 完成
- 📋 Phase 8: 自我排除 Business Logic 準備開始

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

**Phase 8: 自我排除 - Business Logic**
- Service Design: 📋 READY TO START
- Service Implementation: ⏳ PENDING
- Unit Tests: ⏳ PENDING

**上一個完成階段**: Phase 7 - Self-Exclusion Database Schema ✅ (2026-03-26)

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

### Phase 1: 代理佣金系統 - Database Schema + Entity + Dao ✅ COMPLETED (2026-03-25)

**目標**: 創建代理佣金系統的完整數據層（Database + Entity + Dao）

#### 1.1 Database Schema ✅ COMPLETED

**已完成表**: 5 張表
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

**Git 提交**: `ce54e63b` - feat(igaming-agent): implement Phase 1 - Database Schema

---

#### 1.2 Entity 類創建 ✅ COMPLETED

**已完成 Entity 類**: 5 個
- [x] `AgentRelationshipEntity` - 代理關係實體
- [x] `AgentCommissionConfigEntity` - 佣金配置實體
- [x] `AgentCommissionRecordEntity` - 佣金記錄實體
- [x] `AgentCommissionSettlementEntity` - 結算批次實體
- [x] `AgentPerformanceSnapshotEntity` - 業績快照實體

---

#### 1.3 Dao 接口創建 ✅ COMPLETED

**已完成 Dao 接口**: 5 個
- [x] `AgentRelationshipDao` extends `BaseMapper<AgentRelationshipEntity>`
- [x] `AgentCommissionConfigDao` extends `BaseMapper<AgentCommissionConfigEntity>`
- [x] `AgentCommissionRecordDao` extends `BaseMapper<AgentCommissionRecordEntity>`
- [x] `AgentCommissionSettlementDao` extends `BaseMapper<AgentCommissionSettlementEntity>`
- [x] `AgentPerformanceSnapshotDao` extends `BaseMapper<AgentPerformanceSnapshotEntity>`

**Git 提交**: `ce54e63b` - feat(igaming-agent): implement Phase 1 - Database Schema

**Phase 1 Complete**: ✅ All requirements met

---

### Phase 2: 代理佣金系統 - Business Logic ✅ COMPLETED (2026-03-25)

**目標**: 實現代理佣金計算與結算業務邏輯

**已完成 Service 類**: 4 個
1. ✅ `AgentRelationshipService` - 代理樹管理（綁定玩家、查詢代理）
2. ✅ `AgentCommissionCalculationService` - 佣金計算引擎（負盈利抽佣、平台成本）
3. ✅ `AgentCommissionSettlementService` - 分佈式結算（批次管理、防重複）
4. ✅ `AgentCommissionQueryService` - 佣金查詢統計（日期範圍、總額統計）

**已完成任務**:
- [x] 實現代理樹構建與查詢（Level 1 ACTIVE 關係）
- [x] 實現單層級佣金計算（負盈利 × 比例 - 平台成本 5%）
- [x] 實現結算防重複機制（資料庫唯一約束 + 狀態檢查）
- [x] 添加佣金凍結/解凍邏輯（FROZEN 狀態 + 原因記錄）
- [x] 實現結算批次取消功能（SETTLED → PENDING 回滾）

**Git 提交**: `51d11580` - feat(igaming-agent): implement Phase 2 - Business Logic Services

**Phase 2 Complete**: ✅ All core business logic implemented

---

### Phase 3: 代理佣金系統 - Integration Tests ✅ COMPLETED (2026-03-25)

**目標**: 創建端到端集成測試，驗證代理佣金完整流程

**已完成測試**: 12 個測試（100% 通過率）
1. ✅ testBindPlayerToAgent - 玩家綁定代理
2. ✅ testPreventDuplicateBinding - 防止重複綁定
3. ✅ testCalculateCommissionForPlayerLosses - 佣金計算（$1000 → $285 淨佣金）
4. ✅ testNoCommissionForPlayerWins - 玩家贏錢無佣金
5. ✅ testExecuteSettlement - 批次結算（PENDING → SETTLED）
6. ✅ testPreventDuplicateSettlement - 防止重複結算
7. ✅ testFreezeCommissionRecord - 佣金凍結（PENDING → FROZEN）
8. ✅ testUnfreezeCommissionRecord - 佣金解凍（FROZEN → PENDING）
9. ✅ testCancelSettlementBatch - 結算批次取消（SETTLED → PENDING）
10. ✅ testQueryCommissionRecordsByDateRange - 日期範圍查詢
11. ✅ testCalculateTotalCommission - 總額統計（$570）
12. ✅ testPreventSettlementOfFrozenCommission - 凍結佣金不納入結算

**已完成任務**:
- [x] 創建 `AgentCommissionJourneyIntegrationTest`（433 行）
- [x] 測試單層代理佣金計算（30% 比例 - 5% 平台成本）
- [x] 測試防重複結算機制（資料庫唯一約束）
- [x] 測試佣金凍結/解凍（NULL 值更新修復）
- [x] 測試結算批次取消（LambdaUpdateWrapper 修復）

**技術修復**:
- [x] 修復 PostgreSQL BOOLEAN @TableLogic 配置（5 個 Entity）
- [x] 修復 MyBatis Plus NULL 值更新問題（LambdaUpdateWrapper）
- [x] 修復測試數據清理順序（JdbcTemplate 物理刪除）

**Git 提交**: `72b02dc1` - feat(igaming-agent): implement Phase 3 - Integration Tests

**Phase 3 Complete**: ✅ 12/12 tests passed (100%)

---

### Phase 4: VIP 等級自動升級 - Database Schema ✅ COMPLETED (2026-03-26)

**目標**: 創建 VIP 系統的數據庫表結構

**已完成表**: 3 張表
1. ✅ `t_vip_level_config` - VIP 等級配置（升級條件、權益、JSON 額外權益）
2. ✅ `t_player_vip_history` - 玩家 VIP 升級歷史（AUTO_UPGRADE, MANUAL_UPGRADE, DOWNGRADE）
3. ✅ `t_vip_reward_record` - VIP 獎勵記錄（LEVEL_UP_BONUS, BIRTHDAY_BONUS, MONTHLY_REBATE）

**已完成任務**:
- [x] 創建 V014__create_vip_tables.sql（3 張表，15 個索引）
- [x] 創建 V015__seed_vip_level_config.sql（10 個 VIP 等級種子數據）
- [x] 定義 VIP 等級（Bronze → Supreme, 10 個等級）
- [x] 設計升級條件（累計投注額、累計存款、活躍天數）
- [x] 添加 VIP 權益配置（提款限額、返水比例、生日禮金、升級獎金、JSON 額外權益）
- [x] 更新 FlywayMigrationIntegrationTest（8/8 tests passed）

**VIP 等級體系** (10 tiers):
- Level 1: Bronze (entry) - $0 deposit, 0.10% rebate, $50 birthday
- Level 2: Silver - $1K deposit, $5K turnover, 0.15% rebate
- Level 3: Gold - $5K deposit, $25K turnover, 0.20% rebate, priority support
- Level 4: Platinum - $20K deposit, $100K turnover, 0.30% rebate, account manager
- Level 5: Diamond - $50K deposit, $250K turnover, 0.40% rebate, unlimited withdrawal
- Level 6: Master - $100K deposit, $500K turnover, 0.50% rebate, VIP concierge
- Level 7: Grandmaster - $250K deposit, $1.25M turnover, 0.60% rebate, luxury gifts
- Level 8: Elite - $500K deposit, $2.5M turnover, 0.70% rebate, private jets
- Level 9: Legend - $1M deposit, $5M turnover, 0.80% rebate, yacht rentals
- Level 10: Supreme - $5M deposit, $25M turnover, 1.00% rebate, custom benefits

**Git 提交**: `15b438e8` - feat(igaming-vip): implement Phase 4 - VIP Level System Database Schema

**Phase 4 Complete**: ✅ All database schema created and tested

---

### Phase 5: VIP 等級自動升級 - Business Logic ✅ COMPLETED (2026-03-26)

**目標**: 實現 VIP 自動升級業務邏輯

**已完成 Service 類**: 3 個
1. ✅ `VipLevelConfigService` - VIP 等級配置管理（查詢、有效期驗證、升級資格計算）
2. ✅ `VipAutoUpgradeService` - 自動升級引擎（自動升級、手動升級、歷史記錄）
3. ✅ `VipRewardDistributionService` - 升級獎勵發放（升級獎金、生日獎金、月度返水）

**已完成任務**:
- [x] 實現多層級跳躍升級邏輯（calculateNextEligibleLevel 計算最高符合等級）
- [x] 實現升級資格檢查（累計存款、累計投注額、活躍天數）
- [x] 實現自動升級引擎（checkAndUpgradePlayer 自動檢查並升級）
- [x] 實現手動升級邏輯（manualUpgrade 管理員強制升級）
- [x] 實現兩階段獎勵發放（PENDING → ISSUED 狀態流轉）
- [x] 實現獎勵過期管理（升級獎金 30 天、生日獎金 7 天、月度返水 30 天）
- [x] 實現升級歷史記錄（AUTO_UPGRADE、MANUAL_UPGRADE、玩家統計數據快照）
- [x] 添加時區感知日期操作（ZoneId.systemDefault()）
- [x] 使用 io.vavr.Option 函數式編程模式
- [x] 更新 IntegrationModuleTestConfig（添加 VIP 包掃描）
- [x] 編譯驗證通過（集成模組 100% 編譯成功）

**核心實現亮點**:
- **Multi-level Jump**: 玩家可一次跳過多個 VIP 等級（符合最高等級即升級）
- **Two-phase Reward**: 獎勵先創建 PENDING 記錄，錢包服務發放後標記 ISSUED
- **Reward Expiration**: 不同獎勵類型有不同過期時間（7-30 天）
- **Admin Override**: 支持管理員手動升級（特殊促銷、客服升級）
- **Audit Trail**: 完整記錄升級歷史（玩家統計快照、升級原因、操作人員）

**Git 提交**: `26efff6c` - feat(igaming-vip): implement Phase 5 - VIP Level Auto-Upgrade Business Logic

**Phase 5 Complete**: ✅ All business logic implemented (9 files, 915 lines)

---

### Phase 6: VIP 等級自動升級 - Integration Tests ✅ COMPLETED (2026-03-26)

**目標**: 創建端到端集成測試，驗證 VIP 升級完整流程

**已完成測試**: 10 個測試（100% 通過率）
1. ✅ testPlayerReachesUpgradeConditionSingleCondition - 單一條件升級（投注額）
2. ✅ testPlayerReachesUpgradeConditionMultiCondition - 多條件升級（投注額 + 存款 + 活躍天數）
3. ✅ testAutoUpgradeSuccess - 自動升級成功（Bronze → Silver）
4. ✅ testMultiLevelJumpUpgrade - 多級跳躍升級（Bronze → Gold, skip Silver）
5. ✅ testManualUpgrade - 手動管理員升級（Bronze → Platinum）
6. ✅ testUpgradeRewardDistribution - 升級獎勵發放（PENDING → ISSUED）
7. ✅ testUpgradeHistoryVerification - 升級歷史驗證（AUTO_UPGRADE/MANUAL_UPGRADE）
8. ✅ testIneligiblePlayerDoesNotUpgrade - 不符合條件玩家不升級
9. ✅ testMaxLevelPlayerDoesNotUpgrade - 最高等級玩家無法繼續升級
10. ✅ testVipLevelConfigValidityPeriod - VIP 配置有效期驗證

**已完成任務**:
- [x] 創建 `VipAutoUpgradeJourneyIntegrationTest`（464 行）
- [x] 測試單一條件升級（累計投注額）
- [x] 測試多條件升級（投注額 + 存款 + 活躍天數）
- [x] 測試多級跳躍升級邏輯（Bronze → Gold）
- [x] 測試手動管理員升級
- [x] 測試升級獎勵發放生命週期（PENDING → ISSUED）
- [x] 測試升級歷史記錄審計
- [x] 測試邊界條件（不符合條件、最高等級、配置有效期）

**技術修復**:
- [x] 修復 LambdaQueryWrapper 實例化模式（11 處修正）
- [x] 修復測試數據清理順序（VIP 表物理刪除）
- [x] 修復外鍵約束違規（t_player_vip_history、t_vip_reward_record）
- [x] 修復測試斷言金額（匹配 V015 種子數據）

**核心測試覆蓋**:
- **Eligibility Check**: 單一/多條件升級資格驗證
- **Auto-Upgrade**: 自動升級引擎端到端流程
- **Multi-level Jump**: 跨等級直接升級（最高符合等級）
- **Manual Override**: 管理員手動強制升級
- **Reward Distribution**: 兩階段獎勵發放（PENDING → ISSUED）
- **Audit Trail**: 完整升級歷史記錄（玩家統計快照）
- **Edge Cases**: 不符合條件、最高等級、配置有效期

**Git 提交**: `bf02f339` - feat(igaming-vip): implement Phase 6 - VIP Auto-Upgrade Integration Tests

**Phase 6 Complete**: ✅ 10/10 tests passed (100%)

---

### Phase 7: 自我排除 - Database Schema ✅ COMPLETED (2026-03-26)

**目標**: 創建自我排除系統的數據庫表結構

**已完成表**: 2 張表
1. ✅ `t_self_exclusion_request` - 自我排除請求（類型、期限、狀態、冷靜期、審核流程）
2. ✅ `t_self_exclusion_history` - 限制歷史記錄（完整審計追蹤）

**已完成任務**:
- [x] 創建 V016__create_self_exclusion_tables.sql（142 行 SQL）
- [x] 定義限制類型枚舉（DEPOSIT, BETTING, LOGIN, FULL_BLOCK）
- [x] 設計冷靜期規則（24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT）
- [x] 設計解除審核流程（removal_request → approval workflow）
- [x] 添加合規性審計字段（IP address, user agent, compliance acknowledgement）
- [x] 創建 6 個索引（player_id, status, cooling-off period, end date）
- [x] 添加 2 個外鍵約束（player_id → t_player, request_id → t_self_exclusion_request）
- [x] 添加 CHECK 約束（exclusion_type, duration_type, status, permanent end date）
- [x] 添加完整的表和欄位 COMMENT
- [x] 更新 FlywayMigrationIntegrationTest（17 migrations, 33 tables）
- [x] 測試通過（8/8 tests, 100%）

**核心功能亮點**:
- **Exclusion Types**: 4 種限制類型（存款、投注、登入、完全封鎖）
- **Duration Types**: 5 種期限（24小時、7天、30天、6個月、永久）
- **Cooling-off Period**: 防止衝動解除限制的冷靜期機制
- **Removal Workflow**: 完整的解除請求→審核→批准流程
- **Compliance Audit**: IP地址、瀏覽器資訊、政策確認記錄
- **History Tracking**: 完整的狀態變更審計追蹤（7 種 action types）
- **Metadata JSON**: 靈活的擴展字段支持

**Git 提交**: Pending (Phase 7 completed)

**Phase 7 Complete**: ✅ All database schema created and tested

---

### Phase 8: 自我排除 - Business Logic

**目標**: 實現自我排除業務邏輯

**核心 Service 類**:
1. `SelfExclusionRequestService` - 自我排除請求管理
2. `SelfExclusionEnforcementService` - 限制執行引擎
3. `SelfExclusionReviewService` - 解除審核服務

**任務清單**:
- [x] 實現自我排除請求創建 (SelfExclusionRequestService) ✅ 2026-03-26
- [x] 實現限制執行邏輯（攔截器）(SelfExclusionEnforcementService) ✅ 2026-03-26
- [x] 實現冷靜期管理 (cooling-off period calculation) ✅ 2026-03-26
- [x] 實現解除審核流程 (SelfExclusionReviewService) ✅ 2026-03-26
- [ ] 添加限制通知（Email + SMS）⏸️ Deferred to P1
- [ ] 創建 Unit Tests（80%+ 覆蓋率）⏸️ Deferred to Phase 9

**完成標準**:
- [x] 所有 Service 類實現完成 ✅ 2026-03-26
  - SelfExclusionRequestService (269 lines)
  - SelfExclusionEnforcementService (207 lines)
  - SelfExclusionReviewService (286 lines)
- [ ] Unit Tests 通過（80%+ 覆蓋率）⏸️ Deferred to testing phase
- [ ] 限制執行驗證 ⏸️ Deferred to Phase 9 (Integration Tests)

**預計時間**: 6-8 小時

**實際時間**: ~4 小時

**Status**: ✅ completed (core business logic) - 2026-03-26

**備註**:
- Entity classes: SelfExclusionRequestEntity (145 lines), SelfExclusionHistoryEntity (108 lines)
- DAO interfaces: SelfExclusionRequestDao, SelfExclusionHistoryDao
- IntegrationModuleTestConfig updated with self-exclusion package scanning
- 編譯驗證通過（BUILD SUCCESSFUL）

---

### Phase 9: 自我排除 - Integration Tests ✅ COMPLETED (2026-03-27)

**目標**: 創建端到端集成測試，驗證自我排除完整流程

**核心測試場景** (10 個測試 - 100% 通過率):
1. ✅ 玩家創建自我排除請求 (shouldCreateSelfExclusionRequest)
2. ✅ 存款限制執行 (shouldBlockDepositDuringActiveExclusion)
3. ✅ 投注限制執行 (shouldBlockBettingDuringActiveExclusion)
4. ✅ 登入限制執行 (shouldBlockLoginDuringActiveExclusion)
5. ✅ 冷靜期內無法解除 (shouldPreventRemovalBeforeCoolingOffPeriodEnds)
6. ✅ 冷靜期後申請解除 (shouldAllowRemovalRequestAfterCoolingOff)
7. ✅ 管理員批准解除 (shouldApproveRemovalRequest)
8. ✅ 管理員拒絕解除 (shouldRejectRemovalRequest)
9. ✅ 管理員取消請求 (shouldCancelActiveExclusionRequest)
10. ✅ 自動過期處理 (shouldAutomaticallyExpireCompletedRequests)

**前置條件**:
- [x] Phase 8 業務邏輯完成 ✅ 2026-03-26
- [x] V016__create_self_exclusion_tables.sql 遷移腳本創建 ✅ 2026-03-27
- [x] PlayerRegistrationTestFixture 擴展（添加 createSelfExclusionRequest 方法）✅ 2026-03-27
- [x] BaseIntegrationTest 清理邏輯更新（包含自我排除表）✅ 2026-03-27

**任務清單**:
- [x] **Priority 1**: 創建 V016 遷移腳本 ✅ 2026-03-27
  - t_self_exclusion_request 表（17 列，7 個索引）
  - t_self_exclusion_history 表（13 列，4 個索引）
  - 外鍵約束（CASCADE DELETE）
  - 使用 BOOLEAN 類型（統一項目標準，非 SMALLINT）

- [x] **Priority 2**: 擴展 PlayerRegistrationTestFixture ✅ 2026-03-27
  - createSelfExclusionRequest() 方法
  - 冷靜期計算邏輯（24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT）
  - 時間戳操作輔助方法

- [x] **Priority 3**: 創建 SelfExclusionJourneyIntegrationTest ✅ 2026-03-27
  - 10 個測試方法（600+ 行）
  - 審計追蹤驗證邏輯（7 種操作類型）
  - 時間操作策略（數據庫 UPDATE 修改 coolingOffPeriodEnd）

- [x] **Priority 4**: 調試與修復 ✅ 2026-03-27
  - 修復 BOOLEAN vs SMALLINT 類型衝突（統一使用 BOOLEAN）
  - 修復 PostgreSQL JSONB TypeHandler 問題（創建 PostgresJsonbTypeHandler）
  - 修復 MyBatis Plus null 更新問題（使用 LambdaUpdateWrapper.set()）
  - 修復外鍵約束違規（添加 self-exclusion 表清理）
  - 所有 10 個測試通過 ✅

- [x] **Priority 5**: 覆蓋率驗證 ✅ 2026-03-27
  - 生成 JaCoCo 覆蓋率報告
  - Player integration package: 85% 覆蓋率 ✅ (超過 80% 要求)
  - 審計追蹤完整性驗證（7 種操作類型記錄）

**完成標準**:
- [x] 所有 10 個測試通過（100% 通過率）✅
- [x] JaCoCo 覆蓋率 >= 80% ✅ (實際 85%)
  - SelfExclusionRequestService: 3/3 方法
  - SelfExclusionEnforcementService: 5/5 方法
  - SelfExclusionReviewService: 4/4 方法
- [x] 審計追蹤驗證（7 種操作類型完整記錄）✅
- [x] 無數據庫約束違規 ✅
- [x] 測試執行時間 < 60 秒 ✅ (實際 ~7.5 秒)

**技術挑戰與解決方案**:
1. **時間模擬**: ✅ 使用數據庫時間戳操作（UPDATE t_self_exclusion_request SET cooling_off_period_end = ...）
2. **冷靜期測試**: ✅ 快進 coolingOffPeriodEnd 到過去時間（模擬冷靜期結束）
3. **審計追蹤**: ✅ 驗證每次狀態轉換都有對應的 history 記錄（7 種操作類型）
4. **BOOLEAN vs SMALLINT**: ✅ 統一使用 PostgreSQL BOOLEAN 類型（修正 D03 規則）
5. **JSONB TypeHandler**: ✅ 創建 PostgresJsonbTypeHandler（使用 PGobject）
6. **MyBatis Plus null 更新**: ✅ 使用 LambdaUpdateWrapper.set(field, null) 明確設置 null
7. **FK 約束清理**: ✅ 添加 self-exclusion 表清理到 BaseIntegrationTest

**預計時間**: 4 小時

**實際時間**: ~6 小時（包含型別系統修正和 TypeHandler 創建）

**Status**: ✅ completed - 2026-03-27

**Git 提交**: 待提交 (Phase 9 完成後)

**Phase 9 Complete**: ✅ All tests passed (10/10), coverage 85% (exceeds 80% requirement)

**備註**:
- 測試文件：`SelfExclusionJourneyIntegrationTest.java` (600+ 行)
- TypeHandler：`PostgresJsonbTypeHandler.java` (68 行，支持 JSONB 序列化/反序列化)
- 遷移腳本：`V016__create_self_exclusion_tables.sql` (192 行)
- 測試工具擴展：`PlayerRegistrationTestFixture.createSelfExclusionRequest()` (38 行)
- 清理邏輯：`BaseIntegrationTest` 添加 self-exclusion 表清理（8 行）
- 型別標準修正：統一項目使用 BOOLEAN（非 SMALLINT）

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

**2026-03-26 19:20** - Phase 7 完成（Self-Exclusion Database Schema）
- 創建 V016__create_self_exclusion_tables.sql（2 張表，142 行 SQL）
- 定義 4 種限制類型、5 種期限類型
- 實現冷靜期和解除審核流程
- 添加合規性審計字段（IP、user agent、政策確認）
- 更新 FlywayMigrationIntegrationTest（17 migrations, 33 tables）
- 所有測試通過（8/8, 100%）
- Git 提交: Pending

**2026-03-26 17:45** - Phase 6 完成（VIP Integration Tests）
- 創建 VipAutoUpgradeJourneyIntegrationTest（464 行，10 個測試）
- 修復 LambdaQueryWrapper 實例化模式（11 處修正）
- 修復 VIP 表測試清理邏輯（BaseIntegrationTest）
- 所有測試通過（10/10, 100%）
- Git 提交: bf02f339

**2026-03-26 14:30** - Phase 5 完成（VIP Business Logic）
- 實現 3 個 Service 類（VipLevelConfigService、VipAutoUpgradeService、VipRewardDistributionService）
- 多級跳躍升級邏輯實現
- 兩階段獎勵發放實現
- Git 提交: 26efff6c

**2026-03-26 11:20** - Phase 4 完成（VIP Database Schema）
- 創建 3 張表（t_vip_level_config、t_player_vip_history、t_vip_reward_record）
- 10 個 VIP 等級種子數據（Bronze → Supreme）
- FlywayMigrationIntegrationTest 通過（8/8）
- Git 提交: 15b438e8

**2026-03-25 18:40** - Phase 3 完成（Agent Integration Tests）
- 創建 AgentCommissionJourneyIntegrationTest（433 行，12 個測試）
- 修復 PostgreSQL BOOLEAN @TableLogic 配置
- 修復 MyBatis Plus NULL 值更新問題
- Git 提交: 72b02dc1

**2026-03-25 16:15** - Phase 2 完成（Agent Business Logic）
- 實現 4 個 Service 類（AgentRelationshipService、AgentCommissionCalculationService 等）
- 佣金計算引擎實現（負盈利抽佣、平台成本）
- 分佈式結算防重複機制
- Git 提交: 51d11580

**2026-03-25 14:50** - Phase 1 完成（Agent Database Schema）
- 創建 5 張表（t_agent_relationship、t_agent_commission_config 等）
- 16 個索引，5 個外鍵約束
- FlywayMigrationIntegrationTest 通過（8/8）
- Git 提交: ce54e63b

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
