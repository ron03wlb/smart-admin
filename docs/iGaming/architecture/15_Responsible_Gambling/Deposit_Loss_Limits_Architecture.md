# Deposit & Loss Limits Architecture (存款及虧損限額技術架構)

> **Business Requirements**: [Deposit_Limits_Requirements.md](../../requirements/15_Responsible_Gambling/Deposit_Limits_Requirements.md)
> **Canonical Source**: [15-02_Deposit_Limits.md](../../source-archive/15_Responsible_Gambling/15-02_Deposit_Limits.md), [15-06_Loss_Limits.md](../../source-archive/15_Responsible_Gambling/15-06_Loss_Limits.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers

---

## 1. Database Schema

### 1.1 Deposit Limit Setting Table

```sql
CREATE TABLE t_deposit_limit_setting (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,

    -- Current effective limits
    daily_limit         DECIMAL(18,2),      -- NULL = no limit
    weekly_limit        DECIMAL(18,2),
    monthly_limit       DECIMAL(18,2),

    -- Pending limits (during cooling-off for increases)
    pending_daily_limit     DECIMAL(18,2),
    pending_weekly_limit    DECIMAL(18,2),
    pending_monthly_limit   DECIMAL(18,2),
    pending_effective_time  DATETIME,

    -- Limit source
    limit_source        VARCHAR(20) DEFAULT 'PLAYER',  -- PLAYER, OPERATOR, REGULATOR, AFFORDABILITY

    -- Audit
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id)
);
```

### 1.2 Deposit Limit History Table

```sql
CREATE TABLE t_deposit_limit_history (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    change_type         VARCHAR(20) NOT NULL,  -- DECREASE, INCREASE, REMOVE, SET
    limit_type          VARCHAR(20) NOT NULL,  -- DAILY, WEEKLY, MONTHLY
    old_value           DECIMAL(18,2),
    new_value           DECIMAL(18,2),
    effective_time      DATETIME NOT NULL,
    reason              VARCHAR(500),
    initiated_by        VARCHAR(50) NOT NULL,  -- PLAYER, OPERATOR, SYSTEM

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_created_at (created_at)
);
```

### 1.3 Deposit Accumulation Table

```sql
CREATE TABLE t_deposit_accumulation (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    period_type         VARCHAR(20) NOT NULL,  -- DAILY, WEEKLY, MONTHLY
    period_start        DATE NOT NULL,
    accumulated_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,

    UNIQUE KEY uk_player_period (player_id, period_type, period_start),
    INDEX idx_period_start (period_start)
);
```

### 1.4 Loss Limit Setting Table

```sql
CREATE TABLE t_loss_limit_setting (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,

    -- Limit settings
    daily_loss_limit    DECIMAL(18,2),
    weekly_loss_limit   DECIMAL(18,2),
    monthly_loss_limit  DECIMAL(18,2),

    -- Breach action
    breach_action       VARCHAR(20) DEFAULT 'BLOCK',  -- BLOCK, WARN, COOLING_OFF

    -- Pending limits (increase requires cooling-off)
    pending_daily       DECIMAL(18,2),
    pending_weekly      DECIMAL(18,2),
    pending_monthly     DECIMAL(18,2),
    pending_effective_time DATETIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 1.5 Loss Accumulation Table

```sql
CREATE TABLE t_loss_accumulation (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    period_type         VARCHAR(20) NOT NULL,  -- DAILY, WEEKLY, MONTHLY
    period_start        DATE NOT NULL,

    total_stake         DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_win           DECIMAL(18,2) NOT NULL DEFAULT 0,
    net_loss            DECIMAL(18,2) NOT NULL DEFAULT 0,  -- = stake - win

    last_updated        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_player_period (player_id, period_type, period_start),
    INDEX idx_period_start (period_start)
);
```

### 1.6 Pre-Deposit Limit Setup Table (UKGC 2025-10-31)

```sql
ALTER TABLE t_player_protection_settings
ADD COLUMN pre_deposit_limit_set BOOLEAN DEFAULT FALSE
COMMENT 'Pre-deposit limit setup completed flag (UKGC 2025-10-31)';

CREATE TABLE t_pre_deposit_limit_setup (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,
    jurisdiction        VARCHAR(20) NOT NULL,
    setup_completed_at  DATETIME,
    initial_daily_limit     DECIMAL(18,2),
    initial_weekly_limit    DECIMAL(18,2),
    initial_monthly_limit   DECIMAL(18,2),
    acknowledgment_text     VARCHAR(500),
    ip_address              VARCHAR(45),
    user_agent              VARCHAR(500),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_jurisdiction (jurisdiction)
);
```

### 1.7 Deposit Limit Reconciliation Table

```sql
CREATE TABLE t_deposit_limit_reconciliation (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    reconciliation_date DATE NOT NULL,
    player_id           BIGINT NOT NULL,
    jurisdiction        VARCHAR(20) NOT NULL,

    -- Configured limits
    configured_daily    DECIMAL(18,2),
    configured_weekly   DECIMAL(18,2),
    configured_monthly  DECIMAL(18,2),

    -- Actual deposits
    actual_daily        DECIMAL(18,2),
    actual_weekly       DECIMAL(18,2),
    actual_monthly      DECIMAL(18,2),

    -- Status
    daily_status        VARCHAR(20),  -- OK, WARNING, BREACH
    weekly_status       VARCHAR(20),
    monthly_status      VARCHAR(20),

    -- Breach details
    breach_amount       DECIMAL(18,2),
    resolution_action   VARCHAR(100),
    resolved_at         DATETIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_date_player (reconciliation_date, player_id),
    INDEX idx_status (daily_status, weekly_status, monthly_status)
);
```

---

## 2. Service Implementation

### 2.1 DepositLimitService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DepositLimitService {

    private final DepositLimitSettingDao limitSettingDao;
    private final DepositLimitHistoryDao limitHistoryDao;
    private final DepositAccumulationDao accumulationDao;
    private final NotificationService notificationService;

    /**
     * Check if deposit exceeds limit
     * @return Option.none() = can deposit, Option.some() = limit breach
     */
    public Option<LimitBreachResult> checkDepositLimit(Long playerId, BigDecimal amount) {
        Option<DepositLimitSetting> settingOpt = limitSettingDao.findByPlayerId(playerId);

        if (settingOpt.isEmpty()) {
            return Option.none();
        }

        DepositLimitSetting setting = settingOpt.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Check daily limit
        if (setting.getDailyLimit() != null) {
            BigDecimal dailyAccumulated = getAccumulatedAmount(playerId, PeriodType.DAILY, today);
            BigDecimal dailyRemaining = setting.getDailyLimit().subtract(dailyAccumulated);

            if (amount.compareTo(dailyRemaining) > 0) {
                return Option.some(LimitBreachResult.builder()
                    .limitType(LimitType.DAILY)
                    .limit(setting.getDailyLimit())
                    .accumulated(dailyAccumulated)
                    .remaining(dailyRemaining.max(BigDecimal.ZERO))
                    .message("Daily deposit limit exceeded")
                    .build());
            }
        }

        // Check weekly limit
        if (setting.getWeeklyLimit() != null) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            BigDecimal weeklyAccumulated = getAccumulatedAmount(playerId, PeriodType.WEEKLY, weekStart);
            BigDecimal weeklyRemaining = setting.getWeeklyLimit().subtract(weeklyAccumulated);

            if (amount.compareTo(weeklyRemaining) > 0) {
                return Option.some(LimitBreachResult.builder()
                    .limitType(LimitType.WEEKLY)
                    .limit(setting.getWeeklyLimit())
                    .accumulated(weeklyAccumulated)
                    .remaining(weeklyRemaining.max(BigDecimal.ZERO))
                    .build());
            }
        }

        // Check monthly limit
        if (setting.getMonthlyLimit() != null) {
            LocalDate monthStart = today.withDayOfMonth(1);
            BigDecimal monthlyAccumulated = getAccumulatedAmount(playerId, PeriodType.MONTHLY, monthStart);
            BigDecimal monthlyRemaining = setting.getMonthlyLimit().subtract(monthlyAccumulated);

            if (amount.compareTo(monthlyRemaining) > 0) {
                return Option.some(LimitBreachResult.builder()
                    .limitType(LimitType.MONTHLY)
                    .limit(setting.getMonthlyLimit())
                    .accumulated(monthlyAccumulated)
                    .remaining(monthlyRemaining.max(BigDecimal.ZERO))
                    .build());
            }
        }

        return Option.none();
    }

    /**
     * Scheduled task: process pending limit increases
     */
    @Scheduled(fixedRate = 60000)
    public void processPendingLimitIncreases() {
        LocalDateTime now = LocalDateTime.now();
        List<DepositLimitSetting> pendingSettings =
            limitSettingDao.findPendingEffective(now);

        for (DepositLimitSetting setting : pendingSettings) {
            applyPendingLimits(setting);
            limitSettingDao.updateById(setting);
            notificationService.sendLimitIncreaseEffective(setting.getPlayerId());
            log.info("Pending limit increase applied: playerId={}", setting.getPlayerId());
        }
    }
}
```

### 2.2 LossLimitService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LossLimitService {

    private final LossLimitSettingDao lossLimitSettingDao;
    private final LossAccumulationDao lossAccumulationDao;

    /**
     * Check if bet exceeds loss limit
     */
    public Option<LossLimitBreachResult> checkLossLimit(Long playerId, BigDecimal stakeAmount) {
        Option<LossLimitSetting> settingOpt = lossLimitSettingDao.findByPlayerId(playerId);

        if (settingOpt.isEmpty()) {
            return Option.none();
        }

        LossLimitSetting setting = settingOpt.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // Check daily loss limit
        if (setting.getDailyLossLimit() != null) {
            BigDecimal dailyLoss = getCurrentLoss(playerId, PeriodType.DAILY, today);
            BigDecimal projectedLoss = dailyLoss.add(stakeAmount);

            if (projectedLoss.compareTo(setting.getDailyLossLimit()) > 0) {
                BigDecimal remaining = setting.getDailyLossLimit().subtract(dailyLoss)
                    .max(BigDecimal.ZERO);
                return Option.some(LossLimitBreachResult.builder()
                    .limitType(LimitType.DAILY)
                    .limit(setting.getDailyLossLimit())
                    .currentLoss(dailyLoss)
                    .remainingAllowedLoss(remaining)
                    .build());
            }
        }

        // Weekly and monthly checks follow same pattern...
        return Option.none();
    }

    /**
     * Record bet result (update loss accumulation)
     */
    @Transactional(rollbackFor = Throwable.class)
    public void recordBetResult(Long playerId, BigDecimal stakeAmount, BigDecimal winAmount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        updateAccumulation(playerId, PeriodType.DAILY, today, stakeAmount, winAmount);
        updateAccumulation(playerId, PeriodType.WEEKLY, today.with(DayOfWeek.MONDAY), stakeAmount, winAmount);
        updateAccumulation(playerId, PeriodType.MONTHLY, today.withDayOfMonth(1), stakeAmount, winAmount);
    }
}
```

### 2.3 Integration with Payment and Bet Services

```java
@Service
@RequiredArgsConstructor
public class DepositService {

    private final PreDepositLimitService preDepositLimitService;
    private final DepositLimitService depositLimitService;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<DepositResultVO> deposit(Long playerId, DepositForm form) {

        // 0. Pre-deposit limit check (UKGC 2025-10-31)
        String jurisdiction = playerService.getJurisdiction(playerId);
        if (!preDepositLimitService.hasCompletedPreDepositLimitSetup(playerId, jurisdiction)) {
            return ResponseDTO.error(UserErrorCode.PRE_DEPOSIT_LIMIT_REQUIRED,
                "Please set deposit limits before depositing");
        }

        // 1. Check self-exclusion
        if (selfExclusionService.isExcluded(playerId)) {
            return ResponseDTO.error(UserErrorCode.PLAYER_EXCLUDED);
        }

        // 2. Check deposit limit
        Option<LimitBreachResult> limitBreachOpt =
            depositLimitService.checkDepositLimit(playerId, form.getAmount());

        if (limitBreachOpt.isDefined()) {
            LimitBreachResult breach = limitBreachOpt.get();
            return ResponseDTO.error(UserErrorCode.DEPOSIT_LIMIT_EXCEEDED,
                String.format("Exceeded %s limit. Limit: %s, Deposited: %s, Remaining: %s",
                    breach.getLimitType().getDisplayName(),
                    breach.getLimit(), breach.getAccumulated(), breach.getRemaining()));
        }

        // 3. Execute deposit... (continued)
        // 4. Record deposit accumulation
        depositLimitService.recordDeposit(playerId, form.getAmount());

        return ResponseDTO.ok(DepositResultVO.success(paymentResult));
    }
}
```

---

## 3. Reconciliation SQL

```sql
-- Daily deposit limit reconciliation
SELECT
    p.id AS player_id,
    ps.daily_deposit_limit AS configured_limit,
    COALESCE(SUM(pt.amount), 0) AS actual_deposits,
    CASE
        WHEN SUM(pt.amount) > ps.daily_deposit_limit THEN 'BREACH'
        WHEN SUM(pt.amount) > ps.daily_deposit_limit * 0.9 THEN 'WARNING'
        ELSE 'OK'
    END AS status,
    SUM(pt.amount) - ps.daily_deposit_limit AS breach_amount
FROM t_player p
JOIN t_player_protection_settings ps ON p.id = ps.player_id
LEFT JOIN t_payment_transaction pt ON p.id = pt.player_id
    AND pt.type = 'DEPOSIT'
    AND pt.status = 'SUCCESS'
    AND DATE(pt.created_at) = CURDATE() - INTERVAL 1 DAY
WHERE ps.daily_deposit_limit IS NOT NULL
  AND p.jurisdiction = 'UKGC'
GROUP BY p.id, ps.daily_deposit_limit
HAVING status = 'BREACH';
```

---

## 4. Deposit/Loss Limit Enforcement Pipeline

### 4.1 End-to-End Enforcement Flow

The following diagram illustrates the complete enforcement pipeline for deposit and loss limits:

```mermaid
flowchart TD
    Start[Player Action] --> Decision{Action Type?}

    Decision -->|Deposit| Deposit[Deposit Request]
    Decision -->|Bet| Bet[Bet Placement Request]

    %% Deposit Flow
    Deposit --> PreCheck{Pre-deposit<br/>Limit Setup?}
    PreCheck -->|No| BlockPreDeposit[Block: Setup Required<br/>UKGC 2025-10-31]
    PreCheck -->|Yes| SelfExcl{Self-Exclusion<br/>Active?}

    SelfExcl -->|Yes| BlockExcl[Block: Player Excluded]
    SelfExcl -->|No| DailyCheck{Daily Limit<br/>Exceeded?}

    DailyCheck -->|Yes| BlockDaily[Block: Daily Limit Reached<br/>Show remaining amount]
    DailyCheck -->|No| WeeklyCheck{Weekly Limit<br/>Exceeded?}

    WeeklyCheck -->|Yes| BlockWeekly[Block: Weekly Limit Reached]
    WeeklyCheck -->|No| MonthlyCheck{Monthly Limit<br/>Exceeded?}

    MonthlyCheck -->|Yes| BlockMonthly[Block: Monthly Limit Reached]
    MonthlyCheck -->|No| ExecuteDeposit[Execute Deposit Transaction]

    ExecuteDeposit --> RecordDeposit[Record Deposit Accumulation<br/>Update t_deposit_accumulation]
    RecordDeposit --> DepositSuccess[Return Success + Receipt]

    %% Bet Flow
    Bet --> LossLimitCheck{Loss Limit<br/>Configured?}
    LossLimitCheck -->|No| ExecuteBet[Execute Bet Transaction]
    LossLimitCheck -->|Yes| DailyLossCheck{Daily Loss<br/>+ Stake > Limit?}

    DailyLossCheck -->|Yes| LossAction{Breach Action?}
    LossAction -->|BLOCK| BlockLoss[Block: Daily Loss Limit<br/>Show remaining allowance]
    LossAction -->|WARN| WarnLoss[Warn + Allow Bet<br/>Send notification]
    LossAction -->|COOLING_OFF| CoolingOff[Enter Cooling-Off Period<br/>24h restriction]

    DailyLossCheck -->|No| WeeklyLossCheck{Weekly Loss<br/>+ Stake > Limit?}
    WeeklyLossCheck -->|Yes| LossAction
    WeeklyLossCheck -->|No| MonthlyLossCheck{Monthly Loss<br/>+ Stake > Limit?}

    MonthlyLossCheck -->|Yes| LossAction
    MonthlyLossCheck -->|No| ExecuteBet

    WarnLoss --> ExecuteBet
    ExecuteBet --> RecordBet[Record Bet Result<br/>Update t_loss_accumulation]
    RecordBet --> BetSuccess[Return Bet Result]

    %% Error Paths
    BlockPreDeposit --> ErrorResponse[Return Error Response<br/>Status Code 403]
    BlockExcl --> ErrorResponse
    BlockDaily --> ErrorResponse
    BlockWeekly --> ErrorResponse
    BlockMonthly --> ErrorResponse
    BlockLoss --> ErrorResponse
    CoolingOff --> ErrorResponse

    %% Success Paths
    DepositSuccess --> End[End]
    BetSuccess --> End
    ErrorResponse --> End

    %% Styling
    classDef blockStyle fill:#ff6b6b,stroke:#c92a2a,color:#fff
    classDef successStyle fill:#51cf66,stroke:#2f9e44,color:#fff
    classDef warningStyle fill:#ffd43b,stroke:#fab005,color:#000
    classDef processStyle fill:#339af0,stroke:#1971c2,color:#fff

    class BlockPreDeposit,BlockExcl,BlockDaily,BlockWeekly,BlockMonthly,BlockLoss,CoolingOff blockStyle
    class DepositSuccess,BetSuccess,ExecuteDeposit,ExecuteBet successStyle
    class WarnLoss warningStyle
    class RecordDeposit,RecordBet,DailyCheck,WeeklyCheck,MonthlyCheck,DailyLossCheck,WeeklyLossCheck,MonthlyLossCheck processStyle
```

### 4.2 Enforcement Rules Summary

**Deposit Limits (Hierarchical Enforcement)**:
1. **Pre-deposit Setup** (P0): Block if UKGC player without limit setup (2025-10-31)
2. **Self-Exclusion** (P0): Block all deposits during exclusion period
3. **Daily Limit** (P1): Check accumulated deposits for current day (UTC 00:00 reset)
4. **Weekly Limit** (P2): Check accumulated deposits for current week (Monday reset)
5. **Monthly Limit** (P3): Check accumulated deposits for current calendar month

**Loss Limits (Configurable Actions)**:
- **BLOCK**: Prevent bet placement when limit is exceeded (default)
- **WARN**: Allow bet but send notification to player (high rollers)
- **COOLING_OFF**: Trigger 24-hour cooling-off period (regulatory requirement)

**Limit Application Order**:
```
Pre-deposit Setup (UKGC) → Self-Exclusion → Daily → Weekly → Monthly
```

If ANY check fails, transaction is blocked immediately (fail-fast pattern).

### 4.3 Accumulation Calculation

**Deposit Accumulation**:
```java
// Period boundaries (UTC)
LocalDate today = LocalDate.now(ZoneOffset.UTC);
LocalDate weekStart = today.with(DayOfWeek.MONDAY);
LocalDate monthStart = today.withDayOfMonth(1);

// Query accumulated amount
BigDecimal accumulated = depositAccumulationDao
    .selectOne(Wrappers.lambdaQuery(DepositAccumulation.class)
        .eq(DepositAccumulation::getPlayerId, playerId)
        .eq(DepositAccumulation::getPeriodType, periodType)
        .eq(DepositAccumulation::getPeriodStart, periodStart))
    .map(DepositAccumulation::getAccumulatedAmount)
    .getOrElse(BigDecimal.ZERO);
```

**Loss Accumulation**:
```java
// Net loss calculation
BigDecimal netLoss = totalStake.subtract(totalWin);

// Update atomic operation
lossAccumulationDao.updateAccumulation(
    playerId,
    periodType,
    periodStart,
    stakeAmount,
    winAmount
);
```

### 4.4 Error Response Format

```json
{
  "code": 40301,
  "msg": "Deposit limit exceeded",
  "data": {
    "limitType": "DAILY",
    "limit": "500.00",
    "accumulated": "450.00",
    "remaining": "50.00",
    "requestedAmount": "100.00",
    "excessAmount": "50.00",
    "resetTime": "2026-02-11T00:00:00Z"
  }
}
```

---

## 5. Monitoring

| Metric | Prometheus Name | Description |
|--------|----------------|-------------|
| Limit adoption rate | `rg_deposit_limit_adoption_rate` | Players with limits / total players |
| Limit breaches | `rg_deposit_limit_breaches_total` | By limit type |
| Limit changes | `rg_deposit_limit_changes_total` | By direction (increase/decrease) |
| Pending increases | `rg_pending_limit_increases_gauge` | In cooling-off period |
| Loss limit breaches | `loss_limit_breaches_total` | By limit type |
| Player loss distribution | `player_loss_distribution` | Loss concentration analysis |

---

## Related Documents

- [Deposit_Limits_Requirements.md](../../requirements/15_Responsible_Gambling/Deposit_Limits_Requirements.md) - Business requirements
- [Self_Exclusion_Architecture.md](Self_Exclusion_Architecture.md) - Self-exclusion architecture
- [Player_Protection_API.md](Player_Protection_API.md) - Unified API architecture

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
