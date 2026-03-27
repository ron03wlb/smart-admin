---
title: "Ch8: 代理營運技術架構"
part: technical
module: agent-operations
version: v2.2
created: 2026-03-24
---

# 第 8 章：代理營運技術架構

## 8.1 模組概述

代理營運模組提供完整的多層級代理體系、雙錢包管理、佣金計算引擎及信用額度管理功能。支持 10 層代理層級架構，實現複雜的傭金分配、實時信用控制及自動化結算流程。

**核心功能：**
- 10 層代理層級樹狀結構
- 雙錢包模型（現金錢包 + 信用錢包）
- 4 種佣金計算模式
- 實時信用額度管理
- 周期性結算與自動支付
- 代理自助門戶
- 詐欺監測與風控

---

## 8.2 資料模型

### t_agent
主代理表，存儲代理基本信息及配置

| 欄位 | 類型 | 描述 |
|------|------|------|
| agent_id | UUID | 代理唯一識別碼 |
| tenant_id | UUID | 租戶 ID |
| parent_agent_id | UUID | 上級代理 ID（NULL 表示頂級代理） |
| level | INT | 代理層級（1-10） |
| username | VARCHAR(255) | 代理用戶名 |
| email | VARCHAR(255) | 代理郵箱 |
| status | VARCHAR(20) | 代理狀態（ACTIVE/INACTIVE/SUSPENDED/FROZEN） |
| commission_model | VARCHAR(20) | 佣金模式（REVENUE_SHARE/TURNOVER_REBATE/CPA/HYBRID） |
| credit_limit | DECIMAL(15,2) | 信用額度上限 |
| credit_used | DECIMAL(15,2) | 已使用信用額度 |
| tier_config | JSONB | 分層配置 |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

**索引：** (tenant_id, status), (parent_agent_id), (email, tenant_id)

### t_agent_wallet
代理錢包表，支持現金和信用兩種錢包類型

| 欄位 | 類型 | 描述 |
|------|------|------|
| wallet_id | UUID | 錢包唯一識別碼 |
| agent_id | UUID | 代理 ID |
| wallet_type | VARCHAR(20) | 錢包類型（CASH/CREDIT） |
| balance | DECIMAL(15,2) | 錢包餘額 |
| locked_amount | DECIMAL(15,2) | 鎖定金額（待結算） |
| version | INT | 樂觀鎖版本號 |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

**索引：** (agent_id, wallet_type), (agent_id)

### t_commission_record
佣金記錄表，詳細記錄每個佣金計算週期的佣金

| 欄位 | 類型 | 描述 |
|------|------|------|
| record_id | UUID | 記錄唯一識別碼 |
| agent_id | UUID | 代理 ID |
| period | VARCHAR(20) | 計算週期（例：2026-03-W12） |
| model | VARCHAR(20) | 佣金模式 |
| amount | DECIMAL(15,2) | 佣金金額 |
| status | VARCHAR(20) | 記錄狀態（PENDING/APPROVED/PAID） |
| breakdown | JSONB | 佣金明細（分層、具體計算） |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

**索引：** (agent_id, period), (status, created_at)

### t_agent_settlement
結算記錄表，週期性結算統計

| 欄位 | 類型 | 描述 |
|------|------|------|
| settlement_id | UUID | 結算唯一識別碼 |
| agent_id | UUID | 代理 ID |
| period_start | DATE | 結算週期開始日期 |
| period_end | DATE | 結算週期結束日期 |
| total_commission | DECIMAL(15,2) | 總佣金 |
| deductions | DECIMAL(15,2) | 扣減金額 |
| net_amount | DECIMAL(15,2) | 結算淨額 |
| status | VARCHAR(20) | 結算狀態（PENDING/APPROVED/PAID） |
| payment_method | VARCHAR(20) | 支付方式（WALLET/BANK_TRANSFER） |
| bank_reference | VARCHAR(100) | 銀行參考編號 |
| paid_at | TIMESTAMP | 支付時間 |
| created_at | TIMESTAMP | 建立時間 |

**索引：** (agent_id, period_start), (status), (paid_at)

### t_agent_player
代理玩家映射表，記錄代理下屬玩家

| 欄位 | 類型 | 描述 |
|------|------|------|
| id | UUID | 主鍵 |
| agent_id | UUID | 代理 ID |
| player_id | UUID | 玩家 ID |
| joined_date | TIMESTAMP | 玩家加入時間 |
| status | VARCHAR(20) | 關係狀態（ACTIVE/INACTIVE） |

**索引：** (agent_id), (player_id), (agent_id, status)

---

## 8.3 代理層級樹

### 層級架構設計

代理採用樹狀結構，支持最多 10 層層級關係。使用 **鄰接表模型** 存儲父子關係。

**層級定義：**
- **L1**: 頂級代理（parent_agent_id = NULL）
- **L2-L10**: 子代理（parent_agent_id 指向上級代理）

### 樹遍歷實現

使用遞迴 CTE（Common Table Expression）實現高效樹遍歷：

```sql
WITH RECURSIVE agent_tree AS (
    -- 基礎案例：從指定根節點開始
    SELECT agent_id, parent_agent_id, level, username, 1 AS depth
    FROM t_agent
    WHERE agent_id = :root_id AND tenant_id = :tenant_id

    UNION ALL

    -- 遞迴案例：找到所有子節點
    SELECT a.agent_id, a.parent_agent_id, a.level, a.username, at.depth + 1
    FROM t_agent a
    JOIN agent_tree at ON a.parent_agent_id = at.agent_id
    WHERE at.depth < 10 AND a.tenant_id = :tenant_id
)
SELECT * FROM agent_tree
ORDER BY depth, agent_id;
```

### 效能優化

**Redis 快取層：**
- 完整樹結構快取（JSON 格式）
- TTL：5 分鐘
- 快取鍵：`agent:tree:{tenant_id}:{root_agent_id}`
- 快取失效觸發器：代理添加/刪除、狀態變更

**快取預熱策略：**
```sql
-- 後台任務：每 3 分鐘更新頂級代理樹快取
SELECT agent_id, parent_agent_id, level FROM t_agent
WHERE parent_agent_id IS NULL AND status = 'ACTIVE'
ORDER BY created_at;
```

### 層級限制與驗證

```java
// Java 驗證邏輯
public class AgentHierarchyValidator {
    private static final int MAX_LEVEL = 10;

    public void validateNewAgent(UUID parentAgentId)
            throws HierarchyException {
        Agent parent = agentRepository.findById(parentAgentId);
        if (parent.getLevel() >= MAX_LEVEL) {
            throw new HierarchyException("Cannot create agent below level 10");
        }
        if (parent.getStatus() != AgentStatus.ACTIVE) {
            throw new HierarchyException("Parent agent must be ACTIVE");
        }
    }

    public int getDepth(UUID agentId) {
        // 使用快取樹計算深度
        return redisCache.getAgentTree()
            .findNode(agentId)
            .getDepth();
    }
}
```

---

## 8.4 信用額度管理

### 信用額度概念

信用額度用於限制代理的行為風險。每個代理都有設定的信用上限，系統實時追蹤使用情況。

**信用額度類型：**
- **淨值信用**：基於代理的結算淨額與績效
- **風險調整信用**：根據詐欺檢測結果動態調整
- **臨時信用增長**：基於代理的業績達成

### 信用額度閾值

| 閾值 | 觸發條件 | 動作 |
|------|---------|------|
| **正常** | 使用率 ≤ 80% | 正常營運 |
| **警告** | 80% < 使用率 ≤ 90% | 發送警告提示 |
| **高風險** | 90% < 使用率 ≤ 100% | 發送警告，限制新玩家 |
| **凍結** | 使用率 > 100% | 暫停所有操作，需手動審查 |

### 實時信用追蹤

使用 Redis 實現低延遲的信用檢查與更新：

```java
// Java 信用檢查邀請
public class CreditManager {
    private static final String CREDIT_KEY_TEMPLATE = "agent:credit:{agentId}";

    /**
     * 原子性檢查信用額度，失敗返回 -1
     */
    public long checkAndDeductCredit(String agentId, long amount) {
        String key = CREDIT_KEY_TEMPLATE.replace("{agentId}", agentId);
        String luaScript = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local limit = tonumber(ARGV[1])
            local amount = tonumber(ARGV[2])
            if current + amount > limit then
                return -1  -- 信用額度不足
            end
            return redis.call('INCRBY', KEYS[1], amount)
        """;

        Object result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            List.of(key),
            String.valueOf(creditLimit),
            String.valueOf(amount)
        );

        return (long) result;
    }

    /**
     * 監測並發佈閾值超越事件
     */
    public void monitorCreditThresholds(String agentId) {
        long used = Long.parseLong(redisTemplate.opsForValue()
            .get(CREDIT_KEY_TEMPLATE.replace("{agentId}", agentId)));
        long limit = getAgentCreditLimit(agentId);

        if (used > limit * 0.9) {
            kafkaTemplate.send("agent-credit-alerts",
                new CreditAlertEvent(agentId, used, limit));
        }
    }
}
```

### 定期同步至數據庫

```sql
-- 後台任務：每分鐘同步 Redis 信用至 PostgreSQL
UPDATE t_agent_wallet
SET balance = :redis_balance,
    updated_at = NOW()
WHERE agent_id = :agent_id
  AND wallet_type = 'CREDIT'
  AND balance != :redis_balance;
```

### 警告與警報

信用額度閾值超越時，發佈 Kafka 事件供警報系統處理：

```java
public class CreditAlertEvent {
    public UUID agentId;
    public long creditUsed;
    public long creditLimit;
    public double usagePercentage;
    public String alertLevel; // WARNING, HIGH_RISK, FROZEN
    public long timestamp;
}
```

---

## 8.5 佣金計算引擎

### 4 種佣金模式

#### 1. 收入分成 (Revenue Share)

代理按玩家產生的淨收益比例獲得佣金。支持多層分級，根據總 GGR 自動調整佣金比例。

**分級標準（預設）：**
| GGR 範圍 | 佣金比例 |
|----------|---------|
| < $50,000 | 30% |
| $50,000 - $200,000 | 35% |
| $200,000 - $500,000 | 40% |
| ≥ $500,000 | 45% |

**計算公式：**
```
commission = agent_net_revenue × tier_percentage
```

#### 2. 流量返佣 (Turnover Rebate)

代理按玩家總投注額比例獲得佣金，與盈虧無關。適合高流量場景。

**計算公式：**
```
commission = total_turnover × rebate_rate
```

**預設返佣率：** 2% - 5%（可配置）

#### 3. 新玩家獲取 (CPA - Cost Per Acquisition)

代理按首次充值的新玩家數量獲得固定佣金。

**計算公式：**
```
commission = new_depositing_players_count × fixed_cpa_amount
```

**預設 CPA 金額：** $5 - $50 per player（可配置）

#### 4. 混合模式 (Hybrid)

結合多種模式，例如基礎收入分成 + CPA 獎金。

**計算公式：**
```
commission = (revenue_share_commission) + (new_players_count × cpa_bonus)
```

### 佣金計算任務

使用 **Flink SQL 批量計算** 實現高效的週期性佣金計算：

```sql
-- 主佣金計算查詢
SELECT
    a.agent_id,
    a.username,
    a.commission_model,
    SUM(CASE
        WHEN a.commission_model = 'REVENUE_SHARE'
        THEN calculate_revenue_share_commission(
            aps.net_revenue,
            get_tier(aps.net_revenue)
        )
        WHEN a.commission_model = 'TURNOVER_REBATE'
        THEN calculate_turnover_rebate_commission(
            aps.total_turnover,
            a.rebate_rate
        )
        WHEN a.commission_model = 'CPA'
        THEN aps.new_depositing_players * a.cpa_amount
        WHEN a.commission_model = 'HYBRID'
        THEN (
            calculate_revenue_share_commission(aps.net_revenue, get_tier(aps.net_revenue)) +
            aps.new_depositing_players * a.cpa_bonus
        )
        ELSE 0
    END) AS total_commission,
    ARRAY_AGG(JSON_BUILD_OBJECT(
        'player_id', aps.player_id,
        'net_revenue', aps.net_revenue,
        'commission', calculate_commission_for_player(...)
    )) AS breakdown
FROM agent_player_summary aps
JOIN t_agent a ON aps.agent_id = a.agent_id
WHERE aps.period = :calculation_period
  AND a.status = 'ACTIVE'
GROUP BY a.agent_id, a.username, a.commission_model;
```

### 持倉模型 (Position Holding)

代理對玩家投注持倉，贏虧直接影響代理帳戶：

```sql
-- 代理持倉摘要表
CREATE TABLE t_agent_position (
    position_id UUID PRIMARY KEY,
    agent_id UUID REFERENCES t_agent,
    player_id UUID,
    bet_amount DECIMAL(15,2),
    pnl DECIMAL(15,2),  -- 正數表示獲利，負數表示虧損
    status VARCHAR(20),  -- OPEN, SETTLED
    created_at TIMESTAMP,
    settled_at TIMESTAMP
);

-- 代理結算時聚合持倉 P&L
SELECT
    agent_id,
    SUM(pnl) AS position_pnl
FROM t_agent_position
WHERE agent_id = :agent_id
  AND period = :period
  AND status = 'SETTLED'
GROUP BY agent_id;
```

### 佣金計算流水線

```java
@Component
public class CommissionCalculationJob {

    /**
     * 定時任務：每週計算一次佣金
     */
    @Scheduled(cron = "0 0 0 ? * MON") // 每週一 00:00
    public void calculateWeeklyCommissions() {
        LocalDate startOfWeek = LocalDate.now().minusDays(7);
        LocalDate endOfWeek = LocalDate.now();

        List<Commission> commissions = flinkSqlService.executeCommissionCalculation(
            startOfWeek, endOfWeek
        );

        for (Commission commission : commissions) {
            commissionRepository.save(new CommissionRecord(
                commission.getAgentId(),
                commission.getPeriod(),
                commission.getModel(),
                commission.getAmount(),
                CommissionStatus.PENDING,
                commission.getBreakdown()
            ));

            // 發佈佣金計算完成事件
            eventPublisher.publishEvent(new CommissionCalculatedEvent(
                commission.getAgentId(),
                commission.getAmount()
            ));
        }
    }
}
```

---

## 8.6 結算流程

### 結算週期

支持週期性結算（預設週結，可改月結）：

**結算配置：**
```json
{
    "settlement_cycle": "WEEKLY",
    "settlement_day": "MONDAY",
    "settlement_time": "00:00",
    "approval_threshold": 10000,
    "auto_pay_enabled": true,
    "payment_method": "WALLET",
    "bank_transfer_batch_size": 100
}
```

### 4 階段結算流程

#### 第 1 階段：計算 (Calculate)

聚合結算週期內的佣金記錄，計算代理應得佣金：

```sql
-- 聚合佣金
SELECT
    a.agent_id,
    SUM(cr.amount) AS total_commission,
    ARRAY_AGG(cr.record_id) AS commission_record_ids,
    MAX(cr.created_at) AS last_commission_date
FROM t_agent a
LEFT JOIN t_commission_record cr ON a.agent_id = cr.agent_id
WHERE cr.period >= :period_start
  AND cr.period <= :period_end
  AND cr.status IN ('APPROVED', 'PENDING')
GROUP BY a.agent_id;
```

#### 第 2 階段：扣減 (Deduct)

應用各類扣減規則：

```java
public class SettlementDeductionService {

    public BigDecimal calculateDeductions(UUID agentId, BigDecimal grossAmount) {
        BigDecimal deductions = BigDecimal.ZERO;

        // 1. 平台服務費
        deductions = deductions.add(
            grossAmount.multiply(PLATFORM_FEE_RATE)
        );

        // 2. 結轉負餘額
        BigDecimal previousNegativeBalance =
            getLastSettlementNegativeBalance(agentId);
        if (previousNegativeBalance.compareTo(BigDecimal.ZERO) < 0) {
            deductions = deductions.add(
                previousNegativeBalance.abs()
            );
        }

        // 3. 欠款或罰款
        BigDecimal penalties = getPenalties(agentId);
        deductions = deductions.add(penalties);

        return deductions;
    }
}
```

#### 第 3 階段：審批 (Approve)

手動審批超過閾值的結算：

```java
@Component
public class SettlementApprovalService {

    private static final BigDecimal APPROVAL_THRESHOLD =
        new BigDecimal("10000");

    public void submitForApproval(SettlementRecord settlement) {
        if (settlement.getNetAmount().compareTo(APPROVAL_THRESHOLD) > 0) {
            settlement.setStatus(SettlementStatus.PENDING_APPROVAL);

            // 發佈審批事件
            approvalEventPublisher.publishEvent(
                new SettlementApprovalRequiredEvent(
                    settlement.getSettlementId(),
                    settlement.getAgentId(),
                    settlement.getNetAmount()
                )
            );
        } else {
            settlement.setStatus(SettlementStatus.APPROVED);
        }

        settlementRepository.save(settlement);
    }

    @Transactional
    public void approveSettlement(UUID settlementId, UUID approverId,
                                   String remarks) {
        SettlementRecord settlement =
            settlementRepository.findById(settlementId);
        settlement.setStatus(SettlementStatus.APPROVED);
        settlement.setApprovedBy(approverId);
        settlement.setRemarks(remarks);
        settlement.setApprovedAt(LocalDateTime.now());

        settlementRepository.save(settlement);

        // 移至支付階段
        paymentScheduler.schedulePayment(settlement);
    }
}
```

#### 第 4 階段：支付 (Pay)

將結算淨額支付至代理：

```java
@Component
public class SettlementPaymentService {

    @Transactional
    public void executePayment(UUID settlementId) {
        SettlementRecord settlement =
            settlementRepository.findById(settlementId);

        if (settlement.getPaymentMethod().equals(PaymentMethod.WALLET)) {
            // 方式 1：直接轉入 CASH 錢包
            creditAgentWallet(
                settlement.getAgentId(),
                settlement.getNetAmount(),
                WalletType.CASH
            );
        } else if (settlement.getPaymentMethod()
                      .equals(PaymentMethod.BANK_TRANSFER)) {
            // 方式 2：銀行轉帳
            BankTransferRequest transfer = new BankTransferRequest(
                settlement.getAgentId(),
                settlement.getNetAmount(),
                settlement.getAgentBankAccount()
            );
            bankIntegrationService.initiateTransfer(transfer);
        }

        settlement.setStatus(SettlementStatus.PAID);
        settlement.setPaidAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        // 發佈支付完成事件
        eventPublisher.publishEvent(
            new SettlementPaidEvent(settlement.getSettlementId())
        );
    }
}
```

### 負餘額結轉規則

處理結算后負餘額的規則：

```java
@Component
public class NegativeBalanceHandler {

    private static final BigDecimal NEGATIVE_RESET_THRESHOLD =
        new BigDecimal("1000000"); // $1M

    public void handleNegativeBalance(UUID agentId,
                                      BigDecimal netAmount) {
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            // 1. 結轉至下一週期
            Agent agent = agentRepository.findById(agentId);
            agent.setCarriedOverNegativeBalance(
                agent.getCarriedOverNegativeBalance().add(netAmount.abs())
            );

            // 2. 監測連續負值
            int consecutiveNegativePeriods =
                getConsecutiveNegativePeriods(agentId);

            if (consecutiveNegativePeriods >= 3) {
                // 觸發人工審查
                triggerAgentReview(agentId,
                    "3 consecutive negative settlements");
            }

            // 3. 檢查是否超過重置閾值
            if (agent.getCarriedOverNegativeBalance()
                    .compareTo(NEGATIVE_RESET_THRESHOLD) > 0) {
                // 負餘額重置（需管理員確認）
                agent.setCarriedOverNegativeBalance(BigDecimal.ZERO);
                notifyAdminOfNegativeReset(agentId);
            }

            agentRepository.save(agent);
        }
    }
}
```

---

## 8.7 代理自助門戶

### 門戶功能概覽

代理自助門戶提供完整的營運管理工具，支持玩家管理、佣金查詢、信用監測及子代理配置。

### 1. 儀表板 (Dashboard)

實時顯示代理的關鍵業績指標：

```java
@RestController
@RequestMapping("/api/v1/agents/me")
public class AgentDashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard(
            @AuthenticationPrincipal AgentDetails agent) {

        DashboardDTO dashboard = new DashboardDTO();

        // 玩家統計
        dashboard.setTotalPlayers(
            playerRepository.countByAgentId(agent.getId())
        );
        dashboard.setActivePlayersThisMonth(
            playerRepository.countActiveByAgentIdInMonth(
                agent.getId(), LocalDate.now()
            )
        );

        // GGR 與收入
        BigDecimal monthlyGGR = reportService.calculateMonthlyGGR(
            agent.getId(), YearMonth.now()
        );
        dashboard.setMonthlyGGR(monthlyGGR);
        dashboard.setMonthlyCommission(
            commissionRepository.sumByAgentIdAndMonth(
                agent.getId(), YearMonth.now()
            )
        );

        // 信用狀態
        Wallet creditWallet = walletRepository.findByAgentIdAndType(
            agent.getId(), WalletType.CREDIT
        );
        dashboard.setCreditUsage(new CreditUsageDTO(
            creditWallet.getBalance(),
            agent.getCreditLimit(),
            calculateUsagePercentage(creditWallet, agent)
        ));

        return ResponseEntity.ok(dashboard);
    }
}

public class DashboardDTO {
    public int totalPlayers;
    public int activePlayersThisMonth;
    public BigDecimal monthlyGGR;
    public BigDecimal monthlyCommission;
    public CreditUsageDTO creditUsage;
    public List<RecentActivityDTO> recentActivities;
}
```

### 2. 玩家管理 (Player Management)

查看、分析及管理代理的玩家：

```java
@GetMapping("/players")
public ResponseEntity<PagedResponse<PlayerDTO>> listPlayers(
        @AuthenticationPrincipal AgentDetails agent,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status) {

    Pageable pageable = PageRequest.of(page, size);
    Page<Player> players;

    if (status != null) {
        players = playerRepository.findByAgentIdAndStatus(
            agent.getId(), PlayerStatus.valueOf(status), pageable
        );
    } else {
        players = playerRepository.findByAgentId(
            agent.getId(), pageable
        );
    }

    Page<PlayerDTO> dtos = players.map(p -> new PlayerDTO(
        p.getId(),
        p.getUsername(),
        p.getEmail(),
        p.getStatus(),
        p.getJoinedDate(),
        reportService.getPlayerStats(p.getId())
    ));

    return ResponseEntity.ok(new PagedResponse<>(dtos));
}

@GetMapping("/players/{playerId}/report")
public ResponseEntity<PlayerActivityReportDTO> getPlayerReport(
        @PathVariable UUID playerId,
        @RequestParam(name = "period") String period) {

    // 計算玩家在指定週期的活動統計
    PlayerActivityReportDTO report = new PlayerActivityReportDTO(
        reportService.getPlayerDeposits(playerId, period),
        reportService.getPlayerWithdrawals(playerId, period),
        reportService.getPlayerTurnover(playerId, period),
        reportService.getPlayerGGR(playerId, period),
        reportService.getPlayerAverageSessionDuration(playerId, period)
    );

    return ResponseEntity.ok(report);
}
```

### 3. 佣金報告 (Commission Reports)

詳細的佣金明細及歷史查詢：

```java
@GetMapping("/commissions")
public ResponseEntity<PagedResponse<CommissionDTO>> getCommissionHistory(
        @AuthenticationPrincipal AgentDetails agent,
        @RequestParam(required = false) String periodFrom,
        @RequestParam(required = false) String periodTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CommissionRecord> records = commissionRepository
        .findByAgentIdAndPeriodBetween(
            agent.getId(),
            periodFrom != null ? periodFrom : "0",
            periodTo != null ? periodTo : "9999",
            pageable
        );

    Page<CommissionDTO> dtos = records.map(record ->
        new CommissionDTO(
            record.getRecordId(),
            record.getAgent().getUsername(),
            record.getPeriod(),
            record.getModel(),
            record.getAmount(),
            record.getStatus(),
            parseBreakdown(record.getBreakdown()),
            record.getCreatedAt()
        )
    );

    return ResponseEntity.ok(new PagedResponse<>(dtos));
}

@GetMapping("/commissions/{recordId}")
public ResponseEntity<CommissionDetailDTO> getCommissionDetail(
        @PathVariable UUID recordId,
        @AuthenticationPrincipal AgentDetails agent) {

    CommissionRecord record = commissionRepository.findById(recordId)
        .orElseThrow(NotFoundException::new);

    if (!record.getAgentId().equals(agent.getId())) {
        throw new ForbiddenException("Access denied");
    }

    CommissionDetailDTO detail = new CommissionDetailDTO(
        record.getRecordId(),
        record.getPeriod(),
        record.getModel(),
        record.getAmount(),
        record.getStatus(),
        record.getBreakdown(), // JSONB，含每個玩家的佣金
        record.getCreatedAt()
    );

    return ResponseEntity.ok(detail);
}
```

### 4. 信用狀態 (Credit Status)

監測信用額度使用及警告：

```java
@GetMapping("/credit")
public ResponseEntity<CreditStatusDTO> getCreditStatus(
        @AuthenticationPrincipal AgentDetails agent) {

    Agent agentEntity = agentRepository.findById(agent.getId())
        .orElseThrow();

    Wallet creditWallet = walletRepository.findByAgentIdAndType(
        agent.getId(), WalletType.CREDIT
    );

    double usagePercentage = creditWallet.getBalance().doubleValue()
        / agentEntity.getCreditLimit().doubleValue() * 100;

    CreditAlertLevel alertLevel;
    if (usagePercentage > 100) {
        alertLevel = CreditAlertLevel.FROZEN;
    } else if (usagePercentage > 90) {
        alertLevel = CreditAlertLevel.HIGH_RISK;
    } else if (usagePercentage > 80) {
        alertLevel = CreditAlertLevel.WARNING;
    } else {
        alertLevel = CreditAlertLevel.NORMAL;
    }

    CreditStatusDTO status = new CreditStatusDTO(
        agentEntity.getCreditLimit(),
        creditWallet.getBalance(),
        creditWallet.getLockedAmount(),
        usagePercentage,
        alertLevel,
        getCreditAlertHistory(agent.getId(), 30) // 最近 30 天
    );

    return ResponseEntity.ok(status);
}

public class CreditStatusDTO {
    public BigDecimal creditLimit;
    public BigDecimal creditUsed;
    public BigDecimal creditLocked;
    public double usagePercentage;
    public CreditAlertLevel alertLevel;
    public List<CreditAlertHistoryDTO> alertHistory;
}
```

### 5. 子代理管理 (Sub-Agent Management)

創建、配置及管理子代理：

```java
@PostMapping("/sub-agents")
public ResponseEntity<SubAgentDTO> createSubAgent(
        @RequestBody CreateSubAgentRequest request,
        @AuthenticationPrincipal AgentDetails agent) {

    // 驗證權限：只有 L1-L9 的代理可以創建子代理
    Agent parentAgent = agentRepository.findById(agent.getId())
        .orElseThrow();
    if (parentAgent.getLevel() >= 10) {
        throw new ValidationException("Cannot create sub-agent below level 10");
    }

    // 創建子代理
    Agent subAgent = new Agent();
    subAgent.setId(UUID.randomUUID());
    subAgent.setParentAgentId(parentAgent.getId());
    subAgent.setLevel(parentAgent.getLevel() + 1);
    subAgent.setUsername(request.getUsername());
    subAgent.setEmail(request.getEmail());
    subAgent.setCommissionModel(request.getCommissionModel());
    subAgent.setCreditLimit(request.getCreditLimit());
    subAgent.setStatus(AgentStatus.ACTIVE);
    subAgent.setTenantId(parentAgent.getTenantId());
    subAgent.setCreatedAt(LocalDateTime.now());

    Agent savedAgent = agentRepository.save(subAgent);

    // 初始化子代理錢包
    for (WalletType type : WalletType.values()) {
        Wallet wallet = new Wallet();
        wallet.setAgentId(savedAgent.getId());
        wallet.setWalletType(type);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
    }

    // 清除代理樹快取
    cacheService.evictAgentTree(parentAgent.getTenantId());

    return ResponseEntity.created(null).body(new SubAgentDTO(savedAgent));
}

@GetMapping("/sub-agents")
public ResponseEntity<List<SubAgentDTO>> listSubAgents(
        @AuthenticationPrincipal AgentDetails agent) {

    List<Agent> subAgents = agentRepository.findByParentAgentId(agent.getId());
    List<SubAgentDTO> dtos = subAgents.stream()
        .map(SubAgentDTO::new)
        .collect(Collectors.toList());

    return ResponseEntity.ok(dtos);
}

@PutMapping("/sub-agents/{subAgentId}")
public ResponseEntity<SubAgentDTO> updateSubAgent(
        @PathVariable UUID subAgentId,
        @RequestBody UpdateSubAgentRequest request,
        @AuthenticationPrincipal AgentDetails agent) {

    Agent subAgent = agentRepository.findById(subAgentId)
        .orElseThrow();

    // 驗證所有權：子代理必須屬於當前代理
    if (!subAgent.getParentAgentId().equals(agent.getId())) {
        throw new ForbiddenException("Not your sub-agent");
    }

    // 更新配置
    subAgent.setCommissionModel(request.getCommissionModel());
    subAgent.setCreditLimit(request.getCreditLimit());
    subAgent.setStatus(request.getStatus());

    Agent updated = agentRepository.save(subAgent);
    return ResponseEntity.ok(new SubAgentDTO(updated));
}
```

### 門戶 API 端點總結

| 方法 | 路徑 | 描述 |
|------|------|------|
| GET | `/api/v1/agents/me/dashboard` | 獲取代理儀表板 |
| GET | `/api/v1/agents/me/players` | 獲取代理的玩家列表（分頁） |
| GET | `/api/v1/agents/me/players/{playerId}/report` | 獲取單個玩家活動報告 |
| GET | `/api/v1/agents/me/commissions` | 獲取佣金歷史（分頁） |
| GET | `/api/v1/agents/me/commissions/{recordId}` | 獲取佣金詳情 |
| GET | `/api/v1/agents/me/credit` | 獲取信用額度狀態 |
| GET | `/api/v1/agents/me/sub-agents` | 列出所有子代理 |
| POST | `/api/v1/agents/me/sub-agents` | 創建新子代理 |
| PUT | `/api/v1/agents/me/sub-agents/{subAgentId}` | 更新子代理配置 |

---

## 8.8 代理詐欺偵測

### 監測模式

系統持續監測代理行為異常，自動標記並上報高風險活動：

#### 1. 自我投注 (Self-Play Detection)

檢測代理或其關聯人員投注自己經營的玩家：

```sql
-- 檢測潛在自我投注
SELECT
    a.agent_id,
    p.player_id,
    p.email,
    COUNT(*) as bet_count,
    SUM(b.amount) as total_wager
FROM t_agent a
JOIN t_agent_player ap ON a.agent_id = ap.agent_id
JOIN t_player p ON ap.player_id = p.player_id
JOIN t_bet b ON p.player_id = b.player_id
WHERE p.email ILIKE CONCAT('%', a.email, '%')
   OR p.registered_ip = a.last_login_ip
   OR p.device_fingerprint = a.device_fingerprint
GROUP BY a.agent_id, p.player_id
HAVING SUM(b.amount) > 10000  -- 大於 $10K
ORDER BY total_wager DESC;
```

#### 2. 虛假玩家註冊 (Fake Player Registration)

檢測異常的玩家註冊模式：

```java
@Component
public class FakePlayerDetector {

    public void analyzePlayerRegistration(UUID playerId) {
        Player player = playerRepository.findById(playerId);
        Agent agent = agentRepository.findById(
            player.getAgentId()
        );

        // 檢查 1：短時間內大量註冊
        long recentRegistrations = playerRepository
            .countByAgentIdAndCreatedAfter(
                agent.getId(),
                LocalDateTime.now().minusMinutes(60)
            );

        if (recentRegistrations > 50) {
            flagAsAnomalous("Bulk registration detected",
                            agent.getId(), "HIGH");
        }

        // 檢查 2：相同 IP 或設備的玩家
        long samIPPlayers = playerRepository.countByRegisteredIp(
            player.getRegisteredIp()
        );

        if (sameIpPlayers > 100) {
            flagAsAnomalous("Multiple accounts from same IP",
                            agent.getId(), "MEDIUM");
        }

        // 檢查 3：沒有實際活動
        BetActivity activity = reportService.getPlayerActivity(playerId);
        if (activity.getBetCount() == 0
                && activity.getDaysSinceCreation() > 7) {
            flagAsAnomalous("Inactive dummy account",
                            agent.getId(), "LOW");
        }
    }
}
```

#### 3. 佣金操縱 (Commission Manipulation)

檢測異常的佣金計算模式：

```sql
-- 檢測異常佣金模式
WITH agent_commissions AS (
    SELECT
        agent_id,
        period,
        amount,
        LAG(amount) OVER (PARTITION BY agent_id ORDER BY period)
            as prev_amount,
        LAG(amount, 12) OVER (PARTITION BY agent_id ORDER BY period)
            as amount_12_months_ago
    FROM t_commission_record
    WHERE status = 'APPROVED'
)
SELECT
    agent_id,
    period,
    amount,
    ABS((amount - prev_amount) / NULLIF(prev_amount, 0)) as pct_change,
    CASE
        WHEN ABS((amount - prev_amount) / NULLIF(prev_amount, 0)) > 3
        THEN 'SPIKE_DETECTED'
        ELSE 'NORMAL'
    END as anomaly_type
FROM agent_commissions
WHERE ABS((amount - prev_amount) / NULLIF(prev_amount, 0)) > 3
   OR amount < 0;
```

#### 4. 異常信用模式 (Unusual Credit Patterns)

檢測信用額度的異常波動：

```java
@Component
public class CreditAnomalyDetector {

    @Scheduled(fixedDelay = 300000) // 每 5 分鐘
    public void detectCreditAnomalies() {
        List<Agent> agents = agentRepository.findAllActive();

        for (Agent agent : agents) {
            List<CreditSnapshot> snapshots =
                creditSnapshotRepository.findLast24Hours(agent.getId());

            if (snapshots.size() < 2) continue;

            // 計算信用變化速率
            for (int i = 1; i < snapshots.size(); i++) {
                long timeDiffMs = snapshots.get(i).getTimestamp()
                    .getTime() - snapshots.get(i-1).getTimestamp()
                    .getTime();

                BigDecimal creditChange = snapshots.get(i)
                    .getBalance()
                    .subtract(snapshots.get(i-1).getBalance());

                // 如果 1 小時內信用變化超過 $50K
                if (timeDiffMs < 3600000 &&
                    creditChange.abs().compareTo(
                        new BigDecimal("50000")) > 0) {

                    flagAsAnomalous(
                        "Rapid credit depletion",
                        agent.getId(),
                        "HIGH"
                    );
                }
            }
        }
    }
}
```

#### 5. 代理勾結 (Collusion Between Agents)

檢測多個代理之間的協作欺詐：

```sql
-- 檢測可能的代理勾結
WITH shared_players AS (
    SELECT
        ap1.agent_id as agent1,
        ap2.agent_id as agent2,
        COUNT(DISTINCT ap1.player_id) as shared_player_count
    FROM t_agent_player ap1
    JOIN t_agent_player ap2
        ON ap1.player_id = ap2.player_id
        AND ap1.agent_id != ap2.agent_id
    GROUP BY ap1.agent_id, ap2.agent_id
    HAVING COUNT(DISTINCT ap1.player_id) > 50
)
SELECT
    sp.agent1,
    sp.agent2,
    sp.shared_player_count,
    a1.username,
    a2.username
FROM shared_players sp
JOIN t_agent a1 ON sp.agent1 = a1.agent_id
JOIN t_agent a2 ON sp.agent2 = a2.agent_id
WHERE sp.shared_player_count > 50
ORDER BY shared_player_count DESC;
```

### 詐欺標記與上報

```java
@Component
public class FraudFlaggingService {

    public void flagAsAnomalous(String reason, UUID agentId,
                                String severity) {
        FraudAlert alert = new FraudAlert();
        alert.setAgentId(agentId);
        alert.setReason(reason);
        alert.setSeverity(FraudSeverity.valueOf(severity));
        alert.setStatus(FraudAlertStatus.OPEN);
        alert.setCreatedAt(LocalDateTime.now());

        fraudAlertRepository.save(alert);

        // 發佈 Kafka 事件供風控系統消費
        kafkaTemplate.send("fraud-alerts", new FraudAlertEvent(
            alert.getId(),
            alert.getAgentId(),
            alert.getReason(),
            alert.getSeverity(),
            LocalDateTime.now()
        ));

        // 如果是高風險，自動觸發信用凍結
        if (severity.equals("HIGH")) {
            agentService.freezeAgent(agentId, "Fraud alert: " + reason);
        }
    }
}
```

---

## 8.9 自我排除信用管道封鎖 (Self-Exclusion Credit Block)

> **業務規則來源**: Ch15 需求 §15.2 步驟 6 — 代理信用投注管道同步封鎖

### AgentCreditBetFilter

所有代理信用投注 API 入口必須先經過自我排除檢查。這確保「排除檢查覆蓋所有投注管道」(Ch15 合規要求)。

```java
@Component
@RequiredArgsConstructor
@Order(0) // Highest priority — run before credit limit check
public class SelfExclusionCreditFilter {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Check self-exclusion before ANY agent credit operation.
     * Covers: direct credit bet, agent-placed bet, agent-operated bet.
     *
     * Redis key: se:excluded:{tenantId} → SET of excluded player IDs
     * This key is maintained by Ch15 SelfExclusionActivationHandler Step 5.
     */
    public void validateNotExcluded(Long tenantId, Long playerId) {
        Boolean excluded = redisTemplate.opsForSet().isMember(
            "se:excluded:" + tenantId, playerId.toString());

        if (Boolean.TRUE.equals(excluded)) {
            throw new PlayerExcludedException(
                "Player " + playerId + " is self-excluded. " +
                "All betting channels blocked including agent credit.");
        }
    }
}
```

### Credit API Middleware Integration

```java
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentCreditBetController {

    private final SelfExclusionCreditFilter exclusionFilter;
    private final CreditManager creditManager;
    private final WalletService walletService;

    /**
     * Place bet on behalf of player using agent credit.
     * Self-exclusion check is the FIRST validation step.
     */
    @PostMapping("/{agentId}/credit-bet")
    public ResponseEntity<CreditBetResponse> placeCreditBet(
            @PathVariable UUID agentId,
            @RequestBody CreditBetRequest request) {

        // Step 0: Self-exclusion check (Ch15 compliance)
        exclusionFilter.validateNotExcluded(
            request.getTenantId(), request.getPlayerId());

        // Step 1: Verify agent credit limit
        long result = creditManager.checkAndDeductCredit(
            agentId.toString(), request.getAmount().longValue());
        if (result == -1) {
            return ResponseEntity.badRequest()
                .body(CreditBetResponse.insufficientCredit());
        }

        // Step 2: Place bet via wallet service
        // ... (existing logic)

        return ResponseEntity.ok(CreditBetResponse.success());
    }
}
```

### Kafka Listener: Agent Credit Sync Block

```java
/**
 * Listen for self-exclusion events and ensure agent credit channel is blocked.
 * This is the receiving end of Ch15 SelfExclusionActivationHandler Step 6.
 */
@Component
@RequiredArgsConstructor
public class AgentCreditExclusionListener {

    @KafkaListener(topics = "responsible_gaming.self_exclusion_activated",
                   groupId = "agent-credit-exclusion")
    public void onSelfExclusionActivated(SelfExclusionActivatedEvent event) {
        Long tenantId = event.getTenantId();
        Long playerId = event.getPlayerId();

        // Find all agents who have this player
        List<UUID> agentIds = agentPlayerRepo
            .findAgentIdsByPlayerId(tenantId, playerId);

        for (UUID agentId : agentIds) {
            // Notify agent that player is excluded
            notificationService.notifyAgent(agentId,
                "玩家 " + playerId + " 已啟動自我排除，所有信用投注管道已封鎖");

            // Cancel any pending credit bets for this player
            creditBetRepo.cancelPendingBets(tenantId, agentId, playerId,
                "SELF_EXCLUSION");
        }

        auditLog.log(tenantId, playerId, "AGENT_CREDIT_CHANNEL_BLOCKED",
            Map.of("affected_agents", agentIds.size()));
    }
}
```

---

## 8.10 多幣種佣金結算 (Multi-Currency Commission Settlement)

> **業務規則來源**: Ch2 需求 FX 策略 (C-05) + Ch8 代理跨幣種營運

### 佣金幣種轉換規則

| 場景 | 規則 |
|------|------|
| 玩家與代理同幣種 | 直接計算，無需轉換 |
| 玩家與代理不同幣種 | 按結算日 Cold Path 匯率轉換 (T+1 精確匯率) |
| 多幣種玩家聚合 | 各幣種分別計算佣金，統一轉換為代理結算幣種 |
| 加密貨幣玩家 | 按結算日收盤價轉換 |

### t_commission_record 欄位擴充

```sql
ALTER TABLE t_commission_record
    ADD COLUMN IF NOT EXISTS original_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS original_amount DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS settlement_currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS settlement_amount DECIMAL(19,4),
    ADD COLUMN IF NOT EXISTS fx_rate DECIMAL(12,6),
    ADD COLUMN IF NOT EXISTS fx_rate_date DATE;
```

### Multi-Currency Commission Calculator

```java
@Service
@RequiredArgsConstructor
public class MultiCurrencyCommissionService {

    private final FxRateService fxRateService;
    private final CommissionCalculationJob baseCalculator;

    /**
     * Calculate commission for agents with multi-currency player base.
     * Uses Cold Path (T+1) FX rates for accuracy (aligned with Ch2 §2.3.7).
     */
    public List<CommissionRecord> calculateWithFxConversion(
            UUID agentId, LocalDate periodStart, LocalDate periodEnd) {

        Agent agent = agentRepository.findById(agentId).orElseThrow();
        String settlementCurrency = agent.getSettlementCurrency(); // e.g., "USD"

        // Group players by currency
        Map<String, List<AgentPlayerSummary>> byCurrency =
            playerSummaryRepo.findByAgentAndPeriod(agentId, periodStart, periodEnd)
                .stream()
                .collect(Collectors.groupingBy(AgentPlayerSummary::getCurrency));

        List<CommissionRecord> records = new ArrayList<>();

        for (Map.Entry<String, List<AgentPlayerSummary>> entry : byCurrency.entrySet()) {
            String playerCurrency = entry.getKey();
            List<AgentPlayerSummary> summaries = entry.getValue();

            // Calculate commission in player's original currency
            BigDecimal originalCommission = baseCalculator
                .calculateForSummaries(agent, summaries);

            // Convert to agent's settlement currency
            BigDecimal fxRate;
            BigDecimal settlementAmount;

            if (playerCurrency.equals(settlementCurrency)) {
                fxRate = BigDecimal.ONE;
                settlementAmount = originalCommission;
            } else {
                // Use Cold Path rate (T+1, 100% accuracy)
                fxRate = fxRateService.getColdPathRate(
                    playerCurrency, settlementCurrency, periodEnd);
                settlementAmount = originalCommission.multiply(fxRate)
                    .setScale(2, RoundingMode.HALF_UP);
            }

            CommissionRecord record = CommissionRecord.builder()
                .agentId(agentId)
                .period(formatPeriod(periodStart, periodEnd))
                .model(agent.getCommissionModel())
                .originalCurrency(playerCurrency)
                .originalAmount(originalCommission)
                .settlementCurrency(settlementCurrency)
                .settlementAmount(settlementAmount)
                .fxRate(fxRate)
                .fxRateDate(periodEnd)
                .amount(settlementAmount) // backward compat: amount = settlement amount
                .status(CommissionStatus.PENDING)
                .build();

            records.add(record);
        }

        return commissionRepository.saveAll(records);
    }
}
```

---

## 8.11 信用錢包樂觀鎖實作 (Optimistic Lock on Agent Wallet)

> t_agent_wallet 已定義 `version` 欄位，此節定義具體的樂觀鎖使用方式

### JPA Entity

```java
@Entity
@Table(name = "t_agent_wallet")
public class AgentWallet {

    @Id
    @Column(name = "wallet_id")
    private UUID walletId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type")
    private WalletType walletType;

    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "locked_amount", precision = 15, scale = 2)
    private BigDecimal lockedAmount;

    @Version // JPA 樂觀鎖
    @Column(name = "version")
    private Integer version;

    // getters/setters...
}
```

### Optimistic Lock Retry Pattern

```java
@Service
@RequiredArgsConstructor
public class AgentWalletService {

    private static final int MAX_RETRY = 3;

    /**
     * Debit agent wallet with optimistic lock + retry.
     * On concurrent modification, retries up to 3 times.
     */
    @Retryable(
        value = OptimisticLockingFailureException.class,
        maxAttempts = MAX_RETRY,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void debit(UUID agentId, WalletType type, BigDecimal amount,
                      String referenceId) {

        AgentWallet wallet = walletRepository
            .findByAgentIdAndWalletType(agentId, type)
            .orElseThrow(() -> new WalletNotFoundException(agentId, type));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(agentId, amount, wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        // JPA @Version auto-increments on save; throws OptimisticLockingFailureException
        // if another transaction modified the row since read
        walletRepository.save(wallet);

        // Audit trail
        walletTransactionRepo.save(AgentWalletTransaction.builder()
            .walletId(wallet.getWalletId())
            .agentId(agentId)
            .type(WalletTransactionType.DEBIT)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .referenceId(referenceId)
            .build());
    }

    /**
     * Credit agent wallet with optimistic lock + retry.
     */
    @Retryable(
        value = OptimisticLockingFailureException.class,
        maxAttempts = MAX_RETRY,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public void credit(UUID agentId, WalletType type, BigDecimal amount,
                       String referenceId) {

        AgentWallet wallet = walletRepository
            .findByAgentIdAndWalletType(agentId, type)
            .orElseThrow(() -> new WalletNotFoundException(agentId, type));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        walletTransactionRepo.save(AgentWalletTransaction.builder()
            .walletId(wallet.getWalletId())
            .agentId(agentId)
            .type(WalletTransactionType.CREDIT)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .referenceId(referenceId)
            .build());
    }

    /**
     * Fallback when all retries exhausted.
     */
    @Recover
    public void debitRecovery(OptimisticLockingFailureException e,
                               UUID agentId, WalletType type,
                               BigDecimal amount, String referenceId) {
        log.error("Agent wallet debit failed after {} retries: agent={}, amount={}",
            MAX_RETRY, agentId, amount);
        throw new WalletConcurrencyException(
            "Concurrent modification on agent wallet after " + MAX_RETRY + " retries", e);
    }
}
```

### Redis + DB 雙層一致性

```java
/**
 * After DB wallet update succeeds, sync to Redis for real-time credit checks.
 * Redis is the "fast path" for credit limit checks (§8.4 CreditManager).
 * DB is the source of truth.
 */
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void syncWalletToRedis(AgentWalletUpdatedEvent event) {
    String key = "agent:credit:" + event.getAgentId();
    redisTemplate.opsForValue().set(key,
        String.valueOf(event.getNewBalance().longValue()));
}
```

---

## 8.12 大額結算審批門檻 (Settlement Approval Tiers)

> v2.2 新增 — GAP-3（大額結算審批門檻量化）
> ✅ **已決策 (2026-03-25)** — Ron 確認所有門檻為 DB 可配置，不硬編碼。
> 📎 **配置 param_keys**：
> - `ch8.settlement.auto_max` (預設 10000)
> - `ch8.settlement.manager_approval_max` (預設 50000)
> - `ch8.settlement.cfo_approval_max` (預設 100000)
> - `ch8.settlement.monthly_escalation` (預設 500000)
> 詳見 [可配置參數註冊表](./PRD_Configurable_Parameters_Registry_可配置參數註冊表.md)

### 審批分層結構

結算分層制度確保大額結算（≥$10K）符合內控要求，同時保持高效率的自動化流程。

| 單筆結算金額 | 審批層級 | 審批人數 | SLA | 說明 |
|------------|---------|--------|-----|------|
| < $10,000 | **自動發放** | — | 即時 | 系統計算 + 財務審核通過後自動轉帳 |
| $10,000 – $49,999 | **經理級** (Manager) | 1 | 4 小時 | 代理主管核准；可批量簽核 |
| $50,000 – $99,999 | **CFO 級** | 1 | 24 小時 | 財務長核准；額度風控檢查 |
| ≥ $100,000 | **CFO + CEO** | 2 | 48 小時 | 雙重簽核；提交合規審查隊 |

### 累計監控 & 升級規則

| 條件 | 觸發動作 | 說明 |
|------|--------|------|
| 月度累計 ≥ $500K | **升級合規審查** | 代理月度結算超過月度升級閾值時自動轉向合規隊審查 |
| 週度增長 > 200% | **自動 FLAG** | 相比前 4 週均值增長超過 200% 觸發風控人工審核 |
| 新代理首月 ≥ $50K | **自動 FLAG** | 新代理首結算月份金額 ≥ $50K 標記異常 |

**跨章節參照**：
- 📌 **BS-04** (Self-Exclusion × Agent Credit)：結算時須檢查玩家自我排除狀態（§8.3）
- 📌 **BS-09** (Agent Hierarchy Change × Settlement)：代理層級變更時結轉規則（§8.6）

### 實現細節

#### 1. 配置存儲 (t_approval_threshold)

所有審批門檻存儲在資料庫，支持按品牌/司法管轄區覆蓋：

```sql
CREATE TABLE t_approval_threshold (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    brand_id BIGINT,
    jurisdiction_code VARCHAR(10),
    tier INT NOT NULL,
    min_amount DECIMAL(19,4) NOT NULL,
    max_amount DECIMAL(19,4),  -- null = 無上限
    approval_level VARCHAR(50),  -- AUTO, MANAGER, CFO, DUAL_CFO_CEO
    required_approvers INT DEFAULT 1,
    sla_hours INT DEFAULT 24,
    active BOOLEAN DEFAULT TRUE,
    version INT DEFAULT 1,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    change_reason TEXT,
    UNIQUE (tenant_id, brand_id, jurisdiction_code, tier, effective_date),
    INDEX idx_threshold_lookup (brand_id, jurisdiction_code, effective_date)
);
```

#### 2. 結算審批流 (t_settlement_approval_request)

```sql
CREATE TABLE t_settlement_approval_request (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL UNIQUE,
    agent_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    approval_tier INT NOT NULL,
    approval_level VARCHAR(50),  -- AUTO, MANAGER, CFO, DUAL_CFO_CEO
    required_approvers INT NOT NULL,
    status VARCHAR(30),  -- PENDING, IN_PROGRESS, APPROVED, REJECTED, ESCALATED
    escalation_count INT DEFAULT 0,
    escalated_to_tier INT,
    escalation_reason TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sla_expires_at TIMESTAMP NOT NULL,
    decision_at TIMESTAMP,
    notes TEXT,
    supporting_documents JSON,
    CONSTRAINT fk_approval_settlement FOREIGN KEY (settlement_id)
        REFERENCES t_settlement(id),
    CONSTRAINT fk_approval_agent FOREIGN KEY (agent_id)
        REFERENCES t_agent(id),
    INDEX idx_approval_status_sla (status, sla_expires_at),
    INDEX idx_approval_tier (approval_tier)
);
```

#### 3. 門檻變更 (Maker-Checker 審批)

門檻配置變更須通過雙人審批防止誤操作：

```java
@Service
@Transactional
public class ApprovalThresholdService {

    // 提案者提交變更
    public ApprovalThresholdDraft proposeThresholdChange(
        SettlementThresholdChangeRequest request, String makerUserId) {
        // 1. 驗證新門檻邏輯合理性
        validateThresholdLogic(request);

        // 2. 保存為草稿，等待審核
        ApprovalThresholdDraft draft = ApprovalThresholdDraft.builder()
            .makerUserId(makerUserId)
            .proposedAt(LocalDateTime.now())
            .changeReason(request.getReason())
            .newThresholds(request.getThresholds())
            .status(DraftStatus.PENDING_REVIEW)
            .build();

        return draftRepository.save(draft);
    }

    // 審核者確認變更
    public void approveThresholdChange(
        Long draftId, String checkerUserId, String approvalRemarks) {
        ApprovalThresholdDraft draft = draftRepository.findById(draftId);

        // 1. 記錄審批歷史
        draft.setCheckerUserId(checkerUserId);
        draft.setApprovedAt(LocalDateTime.now());
        draft.setApprovalRemarks(approvalRemarks);
        draft.setStatus(DraftStatus.APPROVED);

        // 2. 激活新門檻配置（effective_date = 次日）
        activateThresholds(draft.getNewThresholds());

        draftRepository.save(draft);
    }
}
```

#### 4. Kafka 事件驅動

結算狀態變更發佈事件，下游系統（通知、審計、合規隊）訂閱：

```java
@Component
public class SettlementApprovalEventListener {

    @KafkaListener(topics = "settlement.approval.submitted")
    public void onApprovalSubmitted(SettlementApprovalSubmittedEvent event) {
        // 通知相應審批隊
        String approvalQueue = event.getApprovalLevel(); // MANAGER, CFO, etc
        notificationService.notifyApprovalQueue(approvalQueue, event);
    }

    @KafkaListener(topics = "settlement.escalation.triggered")
    public void onEscalation(SettlementEscalationEvent event) {
        // 月度累計超過 $500K → 轉向合規隊
        complianceTeamService.submitForReview(event.getSettlementId());
        auditService.logEscalation(event);
    }
}
```

---

## 8.13 對應業務文檔

詳細的需求與業務規則，請參考：
> [requirements/08_Agent_Operations_代理營運.md](../requirements/08_Agent_Operations_代理營運.md)

**v2.2 新增/變更清單**:
- §8.12 大額結算審批門檻 (GAP-3): t_approval_threshold + t_settlement_approval_request + Maker-Checker 機制 + Kafka 事件驅動

**v2.1 新增/變更清單**:
- §8.9 自我排除信用管道封鎖: SelfExclusionCreditFilter + Kafka 監聽器 + Credit API 中間件
- §8.10 多幣種佣金結算: FX 轉換規則 + t_commission_record 欄位擴充 + MultiCurrencyCommissionService
- §8.11 信用錢包樂觀鎖: JPA @Version + @Retryable 重試模式 + Redis 同步

---

**版本履歷：**
- v2.2 (2026-03-25)：新增大額結算審批門檻量化 (GAP-3)、BS-04/BS-09 交叉參照
- v2.1 (2026-03-24)：新增排除檢查、多幣種佣金、樂觀鎖實作
- v2.0 (2026-03-24)：初版，涵蓋完整的代理營運技術架構
