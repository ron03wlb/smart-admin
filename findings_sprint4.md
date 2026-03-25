# Findings: Sprint 4 - Agent Commission, VIP Auto-upgrade, Self-Exclusion

**專案**: SmartAdmin iGaming Integration
**日期**: 2026-03-25
**目的**: 記錄 Sprint 4 架構設計決策、技術研究和重要發現

---

## 架構設計發現

### 1. 代理佣金系統架構

**業務需求分析**:
- 支持多層級代理結構（最多 5 層）
- 佣金基於負盈利計算（玩家輸的錢 - 平台成本）
- 防止重複結算（分佈式環境）
- 支持佣金凍結/解凍（風險管理）
- 月度/週度結算批次

**技術選型**:
| 技術組件 | 選擇方案 | 理由 |
|---------|---------|------|
| 任務調度 | Snail Job | 分佈式任務調度，支持失敗重試 |
| 防重複結算 | Redis + DB 唯一約束 | 雙重保障，高可靠性 |
| 代理樹查詢 | PostgreSQL Recursive CTE | 原生支持遞歸查詢，性能優異 |
| 佣金計算 | LiteFlow 規則引擎 | 支持複雜規則配置，可視化編排 |

**關鍵技術點**:
1. **代理樹構建**: 使用 PostgreSQL Recursive CTE 查詢多層級代理關係
   ```sql
   WITH RECURSIVE agent_tree AS (
     SELECT player_id, agent_id, 1 AS level
     FROM t_agent_relationship
     WHERE player_id = ?
     UNION ALL
     SELECT r.player_id, r.agent_id, t.level + 1
     FROM t_agent_relationship r
     JOIN agent_tree t ON r.player_id = t.agent_id
     WHERE t.level < 5
   )
   SELECT * FROM agent_tree;
   ```

2. **防重複結算**: 使用 Redis 分佈式鎖 + 資料庫唯一約束
   ```java
   String lockKey = "commission:settlement:" + settlementDate;
   RLock lock = redissonClient.getLock(lockKey);
   try {
     lock.lock(30, TimeUnit.SECONDS);
     // 檢查資料庫是否已結算
     // 執行結算邏輯
   } finally {
     lock.unlock();
   }
   ```

3. **佣金計算規則**: 使用 LiteFlow 動態配置
   - 節點 1: 計算玩家負盈利（投注額 - 贏金）
   - 節點 2: 按代理層級分配佣金（L1: 30%, L2: 15%, L3: 10%, L4: 5%, L5: 5%）
   - 節點 3: 扣除平台成本（5%）
   - 節點 4: 檢查佣金凍結狀態

**數據模型設計**:
```
t_agent_relationship
├── relationship_id (PK)
├── player_id (FK → t_player)
├── agent_id (FK → t_player)
├── level (1-5)
├── bind_time
└── status (ACTIVE, SUSPENDED, TERMINATED)

t_agent_commission_config
├── config_id (PK)
├── agent_level (1-5)
├── commission_rate (DECIMAL)
├── product_type (SPORTS, CASINO, LIVE, POKER)
└── effective_from / effective_to

t_agent_commission_record
├── record_id (PK)
├── agent_id (FK → t_player)
├── settlement_date
├── total_valid_turnover
├── total_negative_profit
├── commission_amount
├── status (PENDING, SETTLED, FROZEN, CANCELLED)
└── settlement_batch_id

t_agent_commission_settlement
├── batch_id (PK)
├── settlement_date (UNIQUE)
├── total_agents_count
├── total_commission_amount
├── status (RUNNING, COMPLETED, FAILED)
└── executed_at
```

---

### 2. VIP 等級自動升級架構

**業務需求分析**:
- 10 個 VIP 等級（Bronze → Silver → Gold → Platinum → Diamond → ...）
- 升級條件: 累計投注額、累計存款、活躍天數
- 自動升級任務（每日執行）
- 升級獎勵自動發放（獎金、返水比例提升）
- 升級通知（Email + SMS + 站內信）

**技術選型**:
| 技術組件 | 選擇方案 | 理由 |
|---------|---------|------|
| 任務調度 | Snail Job | 每日自動執行升級檢查 |
| 升級獎勵 | Wallet Transaction | 使用現有錢包系統發放獎金 |
| 升級通知 | Kafka Event | 異步發送通知，解耦業務邏輯 |
| 升級歷史 | Audit Log | 記錄所有升級操作，支持追溯 |

**關鍵技術點**:
1. **累計值計算**: 使用 Redis 緩存 + 定期同步資料庫
   ```java
   // Redis 緩存累計投注額
   String key = "vip:turnover:" + playerId;
   redisTemplate.opsForValue().increment(key, betAmount);

   // 每小時同步到資料庫
   @Scheduled(cron = "0 0 * * * *")
   public void syncTurnoverToDatabase() {
     // 批量更新 t_player 的 cumulative_turnover 字段
   }
   ```

2. **升級條件檢查**: 使用 SQL 查詢符合條件的玩家
   ```sql
   SELECT player_id, current_vip_level, cumulative_turnover, cumulative_deposit
   FROM t_player
   WHERE cumulative_turnover >= (
     SELECT min_turnover FROM t_vip_level_config WHERE level = current_vip_level + 1
   )
   AND cumulative_deposit >= (
     SELECT min_deposit FROM t_vip_level_config WHERE level = current_vip_level + 1
   )
   AND active_days >= (
     SELECT min_active_days FROM t_vip_level_config WHERE level = current_vip_level + 1
   );
   ```

3. **升級獎勵發放**: 使用事務確保原子性
   ```java
   @Transactional(rollbackFor = Throwable.class)
   public void upgradeVipLevel(Long playerId, Integer newLevel) {
     // 1. 更新玩家 VIP 等級
     playerService.updateVipLevel(playerId, newLevel);

     // 2. 發放升級獎金
     VipLevelConfig config = vipLevelConfigService.getByLevel(newLevel);
     walletService.addBonus(playerId, config.getUpgradeBonus());

     // 3. 記錄升級歷史
     vipHistoryService.recordUpgrade(playerId, newLevel);

     // 4. 發送升級通知事件
     kafkaPublisher.publishVipUpgradeEvent(playerId, newLevel);
   }
   ```

**數據模型設計**:
```
t_vip_level_config
├── level_id (PK)
├── level (1-10)
├── level_name (Bronze, Silver, Gold, ...)
├── min_turnover (NUMERIC)
├── min_deposit (NUMERIC)
├── min_active_days (INT)
├── upgrade_bonus (NUMERIC)
├── cashback_rate (DECIMAL) -- 返水比例
├── birthday_bonus (NUMERIC)
└── monthly_bonus (NUMERIC)

t_player_vip_history
├── history_id (PK)
├── player_id (FK → t_player)
├── old_level
├── new_level
├── upgrade_time
├── upgrade_reason (AUTO, MANUAL, PROMOTION)
└── reward_amount

t_vip_reward_record
├── reward_id (PK)
├── player_id (FK → t_player)
├── vip_level
├── reward_type (UPGRADE_BONUS, BIRTHDAY, MONTHLY, CASHBACK)
├── reward_amount
├── status (PENDING, ISSUED, CANCELLED)
└── issued_at
```

---

### 3. 自我排除（自我限制）架構

**業務需求分析**:
- 限制類型: 存款限制、投注限制、登入限制、完全封鎖
- 冷靜期: 24小時、7天、30天、永久
- 解除審核流程（合規團隊審核）
- 監管合規性（符合博彩監管要求）
- 審計記錄（所有操作可追溯）

**技術選型**:
| 技術組件 | 選擇方案 | 理由 |
|---------|---------|------|
| 限制執行 | Spring Interceptor | 統一攔截所有操作，易於維護 |
| 冷靜期管理 | Database + Scheduled Task | 自動檢查並更新限制狀態 |
| 審核流程 | Workflow Engine (LiteFlow) | 支持複雜審核流程配置 |
| 審計記錄 | Audit Log + JSONB | 記錄所有操作詳情，支持查詢 |

**關鍵技術點**:
1. **限制執行攔截器**: 在 Controller 層統一攔截
   ```java
   @Component
   public class SelfExclusionInterceptor implements HandlerInterceptor {
     @Override
     public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) {
       Long playerId = getCurrentPlayerId();

       // 檢查玩家是否有自我排除限制
       SelfExclusionRequest exclusion =
         selfExclusionService.getActiveExclusion(playerId);

       if (exclusion != null) {
         // 根據限制類型阻止操作
         if (isDepositOperation(request) && exclusion.hasDepositLimit()) {
           throw new SelfExclusionException("Deposit is restricted");
         }
         if (isBettingOperation(request) && exclusion.hasBettingLimit()) {
           throw new SelfExclusionException("Betting is restricted");
         }
       }

       return true;
     }
   }
   ```

2. **冷靜期管理**: 使用定時任務檢查
   ```java
   @Scheduled(cron = "0 0 * * * *") // 每小時執行一次
   public void checkCoolingPeriod() {
     List<SelfExclusionRequest> expiredRequests =
       selfExclusionRepository.findExpiredCoolingPeriod(OffsetDateTime.now());

     for (SelfExclusionRequest request : expiredRequests) {
       // 更新狀態為 COOLING_PERIOD_EXPIRED
       request.setStatus(SelfExclusionStatus.COOLING_PERIOD_EXPIRED);
       selfExclusionRepository.save(request);

       // 發送通知給玩家（可申請解除）
       notificationService.sendCoolingPeriodExpiredNotice(request.getPlayerId());
     }
   }
   ```

3. **解除審核流程**: 使用 LiteFlow 配置
   ```yaml
   # LiteFlow Chain: self_exclusion_review
   THEN(
     validateReviewRequest,      # 驗證解除請求合規性
     checkCoolingPeriod,          # 檢查冷靜期是否結束
     riskAssessment,              # 風險評估（是否有異常行為）
     manualReview,                # 合規團隊手動審核
     notifyPlayer                 # 通知玩家審核結果
   )
   ```

**數據模型設計**:
```
t_self_exclusion_request
├── request_id (PK)
├── player_id (FK → t_player)
├── exclusion_type (DEPOSIT, BETTING, LOGIN, FULL_BLOCK)
├── cooling_period (HOURS_24, DAYS_7, DAYS_30, PERMANENT)
├── cooling_period_end_time
├── reason (TEXT) -- 玩家申請原因
├── status (ACTIVE, COOLING_PERIOD_EXPIRED, REVIEW_PENDING, RELEASED)
├── created_at
└── released_at

t_self_exclusion_history
├── history_id (PK)
├── request_id (FK → t_self_exclusion_request)
├── player_id (FK → t_player)
├── operation_type (CREATE, UPDATE, RELEASE, REJECT)
├── old_status
├── new_status
├── reviewer_id (FK → t_employee) -- 審核人員
├── review_comment (TEXT)
├── operated_at
└── audit_log (JSONB) -- 完整審計記錄
```

---

## 技術研究發現

### 1. Snail Job 排程系統配置

**研究目的**: 確認 SmartAdmin 是否已集成 Snail Job

**發現**:
- ✅ SmartAdmin v4.1.0 已經集成 Snail Job
- ✅ 配置文件位置: `smartadmin-support/smartadmin-support-job/`
- ✅ 支持分佈式任務調度、失敗重試、監控告警

**使用方式**:
```java
@Component
public class CommissionSettlementJob implements SnailJobHandler {

  @Override
  @SnailJob(name = "commission_settlement",
            cron = "0 0 1 * * MON") // 每週一凌晨 1 點執行
  public void execute(JobContext context) {
    // 執行佣金結算邏輯
    agentCommissionSettlementService.settleWeeklyCommission();
  }
}
```

**配置參考**:
- [smartadmin-support-job README](../smart-admin-api-java21-springboot3/smartadmin-support/smartadmin-support-job/README.md)

---

### 2. Redis 分佈式鎖最佳實踐

**研究目的**: 確認 Redisson 分佈式鎖的使用方式

**發現**:
- ✅ SmartAdmin 已集成 Redisson 3.50.0
- ✅ 支持可重入鎖、讀寫鎖、信號量等多種鎖類型
- ✅ 支持自動續期（Watchdog 機制）

**最佳實踐**:
```java
@Service
public class CommissionSettlementService {

  private final RedissonClient redissonClient;

  public void settleCommission(LocalDate settlementDate) {
    String lockKey = "commission:settlement:" + settlementDate;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      // 等待最多 10 秒獲取鎖，鎖自動釋放時間 30 秒
      boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

      if (!acquired) {
        throw new BusinessException("Settlement job is already running");
      }

      // 執行結算邏輯
      performSettlement(settlementDate);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BusinessException("Settlement interrupted", e);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}
```

**注意事項**:
- ⚠️ 必須在 finally 塊中釋放鎖
- ⚠️ 使用 `lock.isHeldByCurrentThread()` 檢查避免誤解鎖
- ⚠️ 設置合理的鎖超時時間（避免死鎖）

---

### 3. LiteFlow 規則引擎使用

**研究目的**: 確認 LiteFlow 在 Sprint 3 中的實際使用情況

**發現**:
- ✅ Sprint 3 已成功集成 LiteFlow（Turnover Calculation）
- ✅ 支持資料庫動態配置規則（t_liteflow_chain, t_liteflow_script）
- ✅ 支持 Groovy 腳本編寫節點邏輯

**已有實現參考**:
- Chain: `turnover_calculation_main`
- Nodes: `riskFilterNode`, `statusFactorNode`, `gameWeightNode`, `turnoverAggregateNode`
- Migration: V010__seed_liteflow_turnover_chain.sql

**應用場景**:
- ✅ 代理佣金計算（多層級分配規則）
- ✅ VIP 升級條件判斷（多條件組合）
- ✅ 自我排除審核流程（多步驟審核）

**配置示例** (佣金計算 Chain):
```sql
INSERT INTO t_liteflow_chain (chain_code, chain_name, chain_data, enable) VALUES
('agent_commission_calculation', 'Agent Commission Calculation Chain',
 'THEN(calculateNegativeProfit, allocateCommissionByLevel, deductPlatformCost, checkFrozenStatus)',
 true);
```

---

## 架構決策記錄 (ADR)

### ADR-001: 代理佣金使用負盈利計算而非有效投注額

**上下文**:
- 有效投注額: 玩家投注的總金額（不考慮輸贏）
- 負盈利: 玩家輸的錢 - 平台成本（考慮輸贏）

**決策**:
使用負盈利計算佣金

**理由**:
1. ✅ 更公平: 代理僅在玩家虧損時獲得佣金
2. ✅ 風險共擔: 玩家贏錢時代理無佣金，符合利益共享原則
3. ✅ 行業標準: 大多數博彩平台採用負盈利模式
4. ✅ 防止刷量: 避免代理通過虛假投注刷佣金

**後果**:
- ⚠️ 計算複雜度增加（需計算每個玩家的輸贏）
- ⚠️ 數據依賴增加（需要完整的投注結算數據）

---

### ADR-002: VIP 升級使用累計值而非當期值

**上下文**:
- 累計值: 玩家自註冊以來的總投注額、總存款
- 當期值: 玩家當月/當週的投注額、存款

**決策**:
使用累計值作為 VIP 升級條件

**理由**:
1. ✅ 避免降級: 使用累計值確保玩家不會因短期不活躍而降級
2. ✅ 提升體驗: 玩家對 VIP 等級有更強的獲得感和安全感
3. ✅ 簡化邏輯: 不需要實現複雜的降級機制
4. ✅ 行業慣例: 大多數平台採用累計值升級，不降級

**後果**:
- ⚠️ 可能導致高級 VIP 玩家不活躍（已達到最高等級）
- ✅ 可通過其他機制激勵高級 VIP（月度返水、專屬活動）

---

### ADR-003: 自我排除使用攔截器而非 AOP

**上下文**:
- 攔截器 (Interceptor): 在 Spring MVC 層攔截 HTTP 請求
- AOP: 在 Service 層攔截方法調用

**決策**:
使用 Spring Interceptor 實現自我排除限制

**理由**:
1. ✅ 統一入口: 所有 HTTP 請求都經過攔截器，不會遺漏
2. ✅ 性能優異: 在請求早期攔截，避免不必要的業務邏輯執行
3. ✅ 易於測試: 攔截器可以獨立測試，不依賴 Service 層
4. ✅ 符合職責: 限制檢查屬於 Web 層職責

**後果**:
- ⚠️ 無法攔截內部 Service 調用（但自我排除僅針對玩家操作）
- ✅ 可通過添加 `@SkipSelfExclusionCheck` 註解繞過檢查（用於管理員操作）

---

## 性能優化建議

### 1. 代理佣金系統

**潛在瓶頸**:
- 多層級代理樹遞歸查詢（N+1 問題）
- 大批量玩家佣金計算（CPU 密集）

**優化方案**:
1. ✅ 使用 PostgreSQL Recursive CTE（原生支持，性能優異）
2. ✅ 使用 Redis 緩存代理樹結構（24 小時 TTL）
3. ✅ 批量計算佣金（使用 CompletableFuture 並行計算）
4. ✅ 添加資料庫索引（agent_id, settlement_date）

**預期性能**:
- 代理樹查詢: < 50ms (5 層, 1000 個代理)
- 佣金計算: < 5 秒 (10,000 個玩家)

---

### 2. VIP 升級系統

**潛在瓶頸**:
- 大批量玩家升級條件檢查（全表掃描）
- 累計值實時更新（高頻寫入）

**優化方案**:
1. ✅ 使用 Redis 緩存累計值（定期同步資料庫）
2. ✅ 添加資料庫索引（cumulative_turnover, cumulative_deposit）
3. ✅ 使用分頁查詢（避免一次載入所有玩家）
4. ✅ 使用任務調度錯峰執行（凌晨低峰期）

**預期性能**:
- 升級條件檢查: < 10 秒 (100,000 個玩家)
- 累計值更新: < 5ms (Redis 寫入)

---

### 3. 自我排除系統

**潛在瓶頸**:
- 每次請求都查詢限制狀態（高頻查詢）

**優化方案**:
1. ✅ 使用 Redis 緩存限制狀態（10 分鐘 TTL）
2. ✅ 使用 ThreadLocal 緩存單次請求的限制狀態
3. ✅ 添加資料庫索引（player_id, status）

**預期性能**:
- 限制檢查: < 1ms (Redis 緩存命中)
- 資料庫查詢: < 10ms (索引查詢)

---

## 下一步行動

**Phase 0 待辦事項**:
1. [ ] 讀取 iGaming 相關文檔（代理、VIP、自我排除需求）
2. [ ] 確認技術選型（Snail Job、Redis、LiteFlow）
3. [ ] 設計完整的 Database Schema（8-10 張表）
4. [ ] 創建 ERD 圖表（表關係圖）
5. [ ] 創建新分支 `feature/igaming-p1-features`
6. [ ] 準備進入 Phase 1（代理佣金 Database Schema）

**預計完成時間**: 2026-03-25 17:00（約 3 小時）
