# Sprint 4 Service Layer Architecture

**專案**: SmartAdmin iGaming Integration - P1 Features
**日期**: 2026-03-25
**版本**: 1.0.0
**設計者**: iGaming Team

---

## 架構總覽

### SmartAdmin 分層架構規範

```
Controller → Service → Manager → Dao → Entity
              ↓         ↓
            (可直接調用 Dao 進行單表 CRUD)
            (需要 @Transactional 時委託給 Manager)
```

**關鍵規則**（由 ArchUnit 強制執行）:
- ✅ Controller → Service ONLY（Controller 不能直接調用 Dao/Manager）
- ✅ Service → Dao 允許（單表 CRUD，無需事務）
- ✅ Service → Manager 必需（需要 @Transactional 或 @Cacheable）
- ✅ `@Transactional` / `@Cacheable` 只能在 Manager 層
- ✅ 使用 `io.vavr.control.Option`（不是 `java.util.Optional`）
- ✅ 構造器注入（`@RequiredArgsConstructor` + `private final`）

---

## 1. 代理佣金系統（Agent Commission System）

### 1.1 模塊結構

```
smartadmin-igaming-agent/
├── src/main/java/net/lab1024/sa/igaming/agent/
│   ├── controller/
│   │   ├── AgentRelationshipController.java
│   │   ├── AgentCommissionController.java
│   │   └── AgentPerformanceController.java
│   ├── service/
│   │   ├── AgentRelationshipService.java
│   │   ├── AgentCommissionCalculationService.java
│   │   ├── AgentCommissionQueryService.java
│   │   └── AgentPerformanceService.java
│   ├── manager/
│   │   ├── AgentCommissionSettlementManager.java
│   │   └── AgentRelationshipManager.java
│   ├── dao/
│   │   ├── AgentRelationshipDao.java
│   │   ├── AgentCommissionConfigDao.java
│   │   ├── AgentCommissionRecordDao.java
│   │   ├── AgentCommissionSettlementDao.java
│   │   └── AgentPerformanceSnapshotDao.java
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── AgentRelationshipEntity.java
│   │   │   ├── AgentCommissionConfigEntity.java
│   │   │   ├── AgentCommissionRecordEntity.java
│   │   │   ├── AgentCommissionSettlementEntity.java
│   │   │   └── AgentPerformanceSnapshotEntity.java
│   │   ├── form/
│   │   │   ├── AgentRelationshipBindForm.java
│   │   │   ├── AgentCommissionConfigForm.java
│   │   │   └── AgentCommissionQueryForm.java
│   │   └── vo/
│   │       ├── AgentRelationshipVO.java
│   │       ├── AgentCommissionRecordVO.java
│   │       ├── AgentTreeVO.java
│   │       └── AgentPerformanceVO.java
│   └── job/
│       └── AgentCommissionSettlementJob.java
└── src/main/resources/mapper/
    ├── AgentRelationshipMapper.xml
    ├── AgentCommissionRecordMapper.xml
    └── AgentPerformanceSnapshotMapper.xml
```

---

### 1.2 核心 Service 設計

#### 1.2.1 AgentRelationshipService

**職責**: 管理代理關係樹（綁定、查詢、解除）

```java
package net.lab1024.sa.igaming.agent.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.dao.AgentRelationshipDao;
import net.lab1024.sa.igaming.agent.domain.entity.AgentRelationshipEntity;
import net.lab1024.sa.igaming.agent.domain.form.AgentRelationshipBindForm;
import net.lab1024.sa.igaming.agent.domain.vo.AgentTreeVO;
import net.lab1024.sa.igaming.agent.manager.AgentRelationshipManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent Relationship Service - Manages agent relationship tree.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRelationshipService {

  private final AgentRelationshipDao agentRelationshipDao;
  private final AgentRelationshipManager agentRelationshipManager;

  /**
   * Bind player to agent (uses Manager for @Transactional).
   *
   * @param form bind form
   * @return success response
   */
  public ResponseDTO<Void> bindPlayerToAgent(AgentRelationshipBindForm form) {
    return agentRelationshipManager.bindPlayerToAgent(form);
  }

  /**
   * Get agent tree (recursive query using PostgreSQL CTE).
   *
   * @param agentId agent ID
   * @param tenantId tenant ID
   * @return agent tree
   */
  public Option<AgentTreeVO> getAgentTree(Long agentId, Long tenantId) {
    // Query agent tree using PostgreSQL Recursive CTE
    List<AgentRelationshipEntity> relationships =
        agentRelationshipDao.selectAgentTree(agentId, tenantId);

    if (relationships.isEmpty()) {
      return Option.none();
    }

    // Build tree VO
    AgentTreeVO tree = buildAgentTree(relationships);
    return Option.of(tree);
  }

  /**
   * Check if player already has agent.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @return true if player has agent
   */
  public boolean hasAgent(Long playerId, Long tenantId) {
    return agentRelationshipDao.existsByPlayerIdAndTenantId(playerId, tenantId);
  }

  private AgentTreeVO buildAgentTree(List<AgentRelationshipEntity> relationships) {
    // Tree building logic
    // ...
    return new AgentTreeVO();
  }
}
```

---

#### 1.2.2 AgentCommissionCalculationService

**職責**: 計算代理佣金（使用 LiteFlow 規則引擎）

```java
package net.lab1024.sa.igaming.agent.service;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.agent.dao.AgentCommissionConfigDao;
import net.lab1024.sa.igaming.agent.domain.entity.AgentCommissionConfigEntity;
import net.lab1024.sa.igaming.agent.domain.entity.AgentCommissionRecordEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Agent Commission Calculation Service - Calculates commission using LiteFlow.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionCalculationService {

  private static final String LITEFLOW_CHAIN_NAME = "agent_commission_calculation";

  private final FlowExecutor flowExecutor;
  private final AgentCommissionConfigDao commissionConfigDao;

  /**
   * Calculate commission for agent (multi-level distribution).
   *
   * @param agentId agent ID
   * @param settlementDate settlement date
   * @param tenantId tenant ID
   * @return commission records for all levels
   */
  public Try<List<AgentCommissionRecordEntity>> calculateCommission(
      Long agentId, LocalDate settlementDate, Long tenantId) {

    return Try.of(() -> {
      // 1. Get active commission config
      List<AgentCommissionConfigEntity> configs =
          commissionConfigDao.selectActiveConfigs(tenantId, settlementDate);

      if (configs.isEmpty()) {
        log.warn("[AGENT_COMMISSION] No active commission config found for tenantId={}, date={}",
            tenantId, settlementDate);
        return List.of();
      }

      // 2. Build LiteFlow context
      AgentCommissionContext context = buildContext(agentId, settlementDate, tenantId, configs);

      // 3. Execute LiteFlow commission calculation chain
      LiteflowResponse response = flowExecutor.execute2Resp(LITEFLOW_CHAIN_NAME, null, context);

      if (!response.isSuccess()) {
        throw new RuntimeException("LiteFlow commission calculation failed: " + response.getCause());
      }

      // 4. Extract calculated commission records from context
      List<AgentCommissionRecordEntity> records = context.getCommissionRecords();

      log.info("[AGENT_COMMISSION] Calculated commission for agentId={}, recordCount={}",
          agentId, records.size());

      return records;
    });
  }

  /**
   * Calculate platform cost (5% of negative profit).
   *
   * @param negativeProfit negative profit
   * @return platform cost
   */
  public BigDecimal calculatePlatformCost(BigDecimal negativeProfit) {
    return negativeProfit.multiply(new BigDecimal("0.05"));
  }

  private AgentCommissionContext buildContext(
      Long agentId, LocalDate settlementDate, Long tenantId,
      List<AgentCommissionConfigEntity> configs) {
    // Context building logic
    // ...
    return new AgentCommissionContext();
  }
}
```

---

#### 1.2.3 AgentCommissionQueryService

**職責**: 查詢代理佣金記錄和統計

```java
package net.lab1024.sa.igaming.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.agent.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.domain.form.AgentCommissionQueryForm;
import net.lab1024.sa.igaming.agent.domain.vo.AgentCommissionRecordVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Agent Commission Query Service - Query commission records and statistics.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionQueryService {

  private final AgentCommissionRecordDao commissionRecordDao;

  /**
   * Query commission records (paginated).
   *
   * @param form query form
   * @return paginated commission records
   */
  public PageResult<AgentCommissionRecordVO> queryCommissionRecords(AgentCommissionQueryForm form) {
    return SmartPageUtil.convert2PageQuery(
        form,
        () -> commissionRecordDao.queryCommissionRecords(form),
        AgentCommissionRecordVO.class
    );
  }

  /**
   * Get total commission for agent (within date range).
   *
   * @param agentId agent ID
   * @param startDate start date
   * @param endDate end date
   * @param tenantId tenant ID
   * @return total commission amount
   */
  public BigDecimal getTotalCommission(
      Long agentId, LocalDate startDate, LocalDate endDate, Long tenantId) {

    List<AgentCommissionRecordEntity> records =
        commissionRecordDao.selectList(
            Wrappers.<AgentCommissionRecordEntity>lambdaQuery()
                .eq(AgentCommissionRecordEntity::getAgentId, agentId)
                .eq(AgentCommissionRecordEntity::getTenantId, tenantId)
                .between(AgentCommissionRecordEntity::getSettlementDate, startDate, endDate)
                .eq(AgentCommissionRecordEntity::getStatus, "SETTLED")
                .eq(AgentCommissionRecordEntity::getDeleted, false)
        );

    return records.stream()
        .map(AgentCommissionRecordEntity::getNetCommission)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
```

---

### 1.3 Manager 層設計

#### AgentCommissionSettlementManager

**職責**: 執行佣金結算（@Transactional + 分佈式鎖）

```java
package net.lab1024.sa.igaming.agent.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.dao.AgentCommissionSettlementDao;
import net.lab1024.sa.igaming.agent.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.domain.entity.AgentCommissionSettlementEntity;
import net.lab1024.sa.igaming.agent.service.AgentCommissionCalculationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agent Commission Settlement Manager - Execute commission settlement with distributed lock.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCommissionSettlementManager {

  private final RedissonClient redissonClient;
  private final AgentCommissionCalculationService calculationService;
  private final AgentCommissionRecordDao commissionRecordDao;
  private final AgentCommissionSettlementDao settlementDao;

  /**
   * Execute weekly commission settlement (with Redis distributed lock).
   *
   * @param settlementDate settlement date (e.g., 2026-03-24 for week 2026-03-18~2026-03-24)
   * @param tenantId tenant ID
   * @return settlement result
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<Void> settleWeeklyCommission(LocalDate settlementDate, Long tenantId) {
    String lockKey = "commission:settlement:" + settlementDate + ":" + tenantId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      // Acquire distributed lock (wait 10s, auto-release after 30s)
      boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

      if (!acquired) {
        return ResponseDTO.userErrorParam("Settlement job is already running for date: " + settlementDate);
      }

      // 1. Check if settlement already exists (防重複結算)
      if (settlementDao.existsBySettlementDateAndTenantId(settlementDate, tenantId)) {
        return ResponseDTO.userErrorParam("Settlement already completed for date: " + settlementDate);
      }

      // 2. Create settlement batch
      AgentCommissionSettlementEntity settlement = createSettlementBatch(settlementDate, tenantId);
      settlementDao.insert(settlement);

      // 3. Calculate commission for all agents
      List<AgentCommissionRecordEntity> records = calculateAllAgentsCommission(settlementDate, tenantId);

      // 4. Save commission records
      commissionRecordDao.batchInsert(records);

      // 5. Update settlement batch status
      settlement.setStatus("COMPLETED");
      settlement.setTotalAgentsCount(records.size());
      settlement.setTotalCommissionAmount(calculateTotalCommission(records));
      settlementDao.updateById(settlement);

      log.info("[AGENT_COMMISSION] Weekly settlement completed: date={}, agents={}, total={}",
          settlementDate, records.size(), settlement.getTotalCommissionAmount());

      return ResponseDTO.ok();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return ResponseDTO.error("Settlement interrupted");
    } catch (Exception e) {
      log.error("[AGENT_COMMISSION] Settlement failed for date={}", settlementDate, e);
      return ResponseDTO.error("Settlement failed: " + e.getMessage());
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private AgentCommissionSettlementEntity createSettlementBatch(LocalDate settlementDate, Long tenantId) {
    // Create settlement batch logic
    // ...
    return new AgentCommissionSettlementEntity();
  }

  private List<AgentCommissionRecordEntity> calculateAllAgentsCommission(LocalDate settlementDate, Long tenantId) {
    // Calculate commission for all agents
    // ...
    return List.of();
  }

  private java.math.BigDecimal calculateTotalCommission(List<AgentCommissionRecordEntity> records) {
    // Calculate total commission
    // ...
    return java.math.BigDecimal.ZERO;
  }
}
```

---

### 1.4 Snail Job 定時任務

```java
package net.lab1024.sa.igaming.agent.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.agent.manager.AgentCommissionSettlementManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Agent Commission Settlement Job - Weekly commission settlement task.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCommissionSettlementJob {

  private final AgentCommissionSettlementManager settlementManager;

  /**
   * Execute weekly commission settlement.
   * Scheduled: Every Monday at 1:00 AM (cron = "0 0 1 * * MON")
   */
  public void executeWeeklySettlement() {
    // Settlement for last week (Monday to Sunday)
    LocalDate settlementDate = LocalDate.now().minusWeeks(1).with(java.time.DayOfWeek.SUNDAY);
    Long tenantId = 1L; // TODO: Get all active tenants

    log.info("[AGENT_COMMISSION_JOB] Starting weekly settlement for date={}", settlementDate);

    settlementManager.settleWeeklyCommission(settlementDate, tenantId);

    log.info("[AGENT_COMMISSION_JOB] Weekly settlement completed for date={}", settlementDate);
  }
}
```

---

## 2. VIP 等級系統（VIP Tier System）

### 2.1 模塊結構

```
smartadmin-igaming-vip/
├── src/main/java/net/lab1024/sa/igaming/vip/
│   ├── controller/
│   │   ├── VipLevelConfigController.java
│   │   └── VipRewardController.java
│   ├── service/
│   │   ├── VipLevelConfigService.java
│   │   ├── VipAutoUpgradeService.java
│   │   └── VipRewardDistributionService.java
│   ├── manager/
│   │   └── VipUpgradeManager.java
│   ├── dao/
│   │   ├── VipLevelConfigDao.java
│   │   ├── PlayerVipHistoryDao.java
│   │   └── VipRewardRecordDao.java
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── VipLevelConfigEntity.java
│   │   │   ├── PlayerVipHistoryEntity.java
│   │   │   └── VipRewardRecordEntity.java
│   │   ├── form/
│   │   │   ├── VipLevelConfigForm.java
│   │   │   └── VipRewardQueryForm.java
│   │   └── vo/
│   │       ├── VipLevelConfigVO.java
│   │       ├── PlayerVipStatusVO.java
│   │       └── VipRewardRecordVO.java
│   └── job/
│       └── VipAutoUpgradeJob.java
└── src/main/resources/mapper/
    ├── PlayerVipHistoryMapper.xml
    └── VipRewardRecordMapper.xml
```

---

### 2.2 核心 Service 設計

#### VipAutoUpgradeService

**職責**: 自動檢查並升級 VIP 等級

```java
package net.lab1024.sa.igaming.vip.service;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.vip.dao.VipLevelConfigDao;
import net.lab1024.sa.igaming.vip.domain.entity.VipLevelConfigEntity;
import net.lab1024.sa.igaming.vip.manager.VipUpgradeManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * VIP Auto Upgrade Service - Automatically check and upgrade VIP levels.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipAutoUpgradeService {

  private final PlayerDao playerDao;
  private final VipLevelConfigDao vipLevelConfigDao;
  private final VipUpgradeManager vipUpgradeManager;

  /**
   * Check and upgrade all eligible players (called by daily scheduled job).
   *
   * @param tenantId tenant ID
   * @return number of upgraded players
   */
  public Try<Integer> checkAndUpgradeAllPlayers(Long tenantId) {
    return Try.of(() -> {
      // 1. Get all VIP level configs (sorted by level)
      List<VipLevelConfigEntity> vipConfigs = vipLevelConfigDao.selectAllLevelsSorted(tenantId);

      if (vipConfigs.isEmpty()) {
        log.warn("[VIP_AUTO_UPGRADE] No VIP configs found for tenantId={}", tenantId);
        return 0;
      }

      int upgradedCount = 0;

      // 2. For each VIP level (starting from lowest), check eligible players
      for (VipLevelConfigEntity config : vipConfigs) {
        List<PlayerEntity> eligiblePlayers = findEligiblePlayers(config, tenantId);

        for (PlayerEntity player : eligiblePlayers) {
          // 3. Upgrade player using Manager (with @Transactional)
          vipUpgradeManager.upgradeVipLevel(player.getPlayerId(), config.getLevel(), "AUTO", tenantId);
          upgradedCount++;
        }
      }

      log.info("[VIP_AUTO_UPGRADE] Total upgraded players: {}", upgradedCount);
      return upgradedCount;
    });
  }

  /**
   * Find players eligible for VIP upgrade.
   *
   * @param config VIP level config
   * @param tenantId tenant ID
   * @return eligible players
   */
  private List<PlayerEntity> findEligiblePlayers(VipLevelConfigEntity config, Long tenantId) {
    // SQL query to find eligible players:
    // WHERE cumulative_turnover >= config.min_cumulative_turnover
    //   AND cumulative_deposit >= config.min_cumulative_deposit
    //   AND active_days >= config.min_active_days
    //   AND current_vip_level < config.level
    return playerDao.selectEligibleForVipUpgrade(config, tenantId);
  }
}
```

---

## 3. 自我排除系統（Self-Exclusion System）

### 3.1 模塊結構

```
smartadmin-igaming-selfexclusion/
├── src/main/java/net/lab1024/sa/igaming/selfexclusion/
│   ├── controller/
│   │   └── SelfExclusionController.java
│   ├── service/
│   │   ├── SelfExclusionRequestService.java
│   │   ├── SelfExclusionEnforcementService.java
│   │   └── SelfExclusionReviewService.java
│   ├── manager/
│   │   └── SelfExclusionManager.java
│   ├── dao/
│   │   ├── SelfExclusionRequestDao.java
│   │   └── SelfExclusionHistoryDao.java
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── SelfExclusionRequestEntity.java
│   │   │   └── SelfExclusionHistoryEntity.java
│   │   ├── form/
│   │   │   ├── SelfExclusionRequestForm.java
│   │   │   └── SelfExclusionReviewForm.java
│   │   └── vo/
│   │       ├── SelfExclusionRequestVO.java
│   │       └── SelfExclusionHistoryVO.java
│   ├── interceptor/
│   │   └── SelfExclusionInterceptor.java
│   └── job/
│       └── SelfExclusionCoolingPeriodCheckJob.java
└── src/main/resources/mapper/
    └── SelfExclusionRequestMapper.xml
```

---

### 3.2 Spring Interceptor 設計

```java
package net.lab1024.sa.igaming.selfexclusion.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.selfexclusion.service.SelfExclusionEnforcementService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Self-Exclusion Interceptor - Intercept all player operations and enforce restrictions.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfExclusionInterceptor implements HandlerInterceptor {

  private final SelfExclusionEnforcementService enforcementService;

  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler) throws Exception {

    Long playerId = getCurrentPlayerId(request);
    if (playerId == null) {
      return true; // Not a player request, allow
    }

    // Check if player has active self-exclusion
    boolean isDepositOperation = isDepositOperation(request);
    boolean isBettingOperation = isBettingOperation(request);
    boolean isLoginOperation = isLoginOperation(request);

    if (isDepositOperation) {
      enforcementService.checkDepositRestriction(playerId);
    }

    if (isBettingOperation) {
      enforcementService.checkBettingRestriction(playerId);
    }

    if (isLoginOperation) {
      enforcementService.checkLoginRestriction(playerId);
    }

    return true;
  }

  private Long getCurrentPlayerId(HttpServletRequest request) {
    // Extract player ID from session/token
    return null;
  }

  private boolean isDepositOperation(HttpServletRequest request) {
    return request.getRequestURI().contains("/payment/deposit");
  }

  private boolean isBettingOperation(HttpServletRequest request) {
    return request.getRequestURI().contains("/game/bet");
  }

  private boolean isLoginOperation(HttpServletRequest request) {
    return request.getRequestURI().contains("/auth/login");
  }
}
```

---

## 4. 技術選型確認

### 4.1 已選定技術組件

| 組件 | 版本 | 用途 | 狀態 |
|------|------|------|------|
| **Snail Job** | 1.x | 分佈式任務調度 | ✅ SmartAdmin 已集成 |
| **Redisson** | 3.50.0 | 分佈式鎖（Redis） | ✅ SmartAdmin 已集成 |
| **LiteFlow** | 2.12.x | 規則引擎 | ✅ Sprint 3 已集成 |
| **PostgreSQL Recursive CTE** | - | 代理樹查詢 | ✅ 原生支持 |
| **Spring Interceptor** | - | 自我排除攔截 | ✅ Spring MVC 內建 |
| **Kafka** | - | 事件通知 | ✅ SmartAdmin 已集成 |

### 4.2 技術組件使用場景

**Snail Job**:
- 代理佣金週度結算（每週一凌晨 1 點）
- VIP 等級自動升級（每日凌晨 2 點）
- 自我排除冷靜期檢查（每小時）

**Redisson 分佈式鎖**:
- 佣金結算防重複（lockKey: `commission:settlement:{date}:{tenantId}`）
- VIP 升級防並發（lockKey: `vip:upgrade:{playerId}`）

**LiteFlow 規則引擎**:
- 代理佣金計算（多層級分配規則）
- 自我排除審核流程（多步驟審核）

**PostgreSQL Recursive CTE**:
- 代理樹遞歸查詢（5 層代理關係）

**Spring Interceptor**:
- 自我排除限制執行（統一攔截所有操作）

**Kafka 事件通知**:
- VIP 升級通知（topic: `vip.upgrade.events`）
- 自我排除狀態變更通知（topic: `self-exclusion.events`）

---

## 5. 下一步行動

1. ✅ 完成 Service 層架構設計文檔
2. [ ] 創建新分支 `feature/igaming-p1-features`
3. [ ] 開始實施 Phase 1：代理佣金系統 - Database Schema（V013 migration）
4. [ ] 實施 Phase 2：代理佣金系統 - Business Logic
5. [ ] 實施 Phase 3：代理佣金系統 - Integration Tests

---

**文檔版本**: 1.0.0
**最後更新**: 2026-03-25 17:45
**審核狀態**: ✅ 設計完成，待實施
