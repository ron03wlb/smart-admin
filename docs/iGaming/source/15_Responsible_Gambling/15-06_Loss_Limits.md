# 15-06 Loss Limits (虧損限額)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

虧損限額 (Loss Limits) 允許玩家設定在特定時間內的最大虧損金額，達到限額後將阻止繼續投注。

### 與存款限額的區別

| 特性 | 存款限額 | 虧損限額 |
|------|---------|---------|
| **計算基礎** | 存款金額 | 淨虧損金額 |
| **目的** | 控制投入資金 | 控制實際損失 |
| **計算方式** | 累加存款 | 投注 - 獎金 |
| **贏錢影響** | 不受影響 | 降低累計虧損 |

---

## 虧損計算邏輯

### 淨虧損公式

```
淨虧損 = 總投注金額 - 總贏得金額 - 已提取金額

其中：
- 總投注金額：玩家在計算期間的所有投注
- 總贏得金額：玩家在計算期間的所有獎金（含 Jackpot）
- 已提取金額：不影響虧損計算（可選配置）
```

### 計算範例

```
情境：玩家設定日虧損限額 $500

時間線：
09:00 - 存款 $1,000
10:00 - 投注 $300，贏 $100 → 淨虧損: $200
11:00 - 投注 $200，輸 → 淨虧損: $400
12:00 - 投注 $100，輸 → 淨虧損: $500 ⚠️ 達到限額
12:01 - 嘗試投注 → ❌ 被阻止
13:00 - 贏得 $150（從未結投注）→ 淨虧損: $350 ✅ 可繼續
```

---

## 限額類型

### 時間維度

| 限額類型 | 計算週期 | 重置時間 |
|---------|---------|---------|
| 日虧損限額 | 24 小時 | UTC 00:00 |
| 週虧損限額 | 7 天 | 週一 UTC 00:00 |
| 月虧損限額 | 30 天 | 每月 1 日 UTC 00:00 |

### 行為選項

達到限額後的行為（可配置）：

| 選項 | 說明 | 適用場景 |
|------|------|---------|
| 阻止投注 | 禁止新投注 | 預設行為 |
| 警告繼續 | 顯示警告，玩家可選擇繼續 | 某些牌照允許 |
| 冷靜期 | 強制暫停 1 小時 | 嚴格模式 |

---

## 技術實現

### 資料庫設計

```sql
-- 虧損限額設定表
CREATE TABLE t_loss_limit_setting (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,

    -- 限額設定
    daily_loss_limit    DECIMAL(18,2),
    weekly_loss_limit   DECIMAL(18,2),
    monthly_loss_limit  DECIMAL(18,2),

    -- 達到限額行為
    breach_action       VARCHAR(20) DEFAULT 'BLOCK',  -- BLOCK, WARN, COOLING_OFF

    -- 待生效限額（提高需冷靜期）
    pending_daily       DECIMAL(18,2),
    pending_weekly      DECIMAL(18,2),
    pending_monthly     DECIMAL(18,2),
    pending_effective_time DATETIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 虧損累計表
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

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LossLimitService {

    private final LossLimitSettingDao lossLimitSettingDao;
    private final LossAccumulationDao lossAccumulationDao;

    /**
     * 檢查投注是否超過虧損限額
     */
    public Option<LossLimitBreachResult> checkLossLimit(Long playerId, BigDecimal stakeAmount) {
        Option<LossLimitSetting> settingOpt = lossLimitSettingDao.findByPlayerId(playerId);

        if (settingOpt.isEmpty()) {
            return Option.none(); // 無限額設定
        }

        LossLimitSetting setting = settingOpt.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 檢查日虧損限額
        if (setting.getDailyLossLimit() != null) {
            BigDecimal dailyLoss = getCurrentLoss(playerId, PeriodType.DAILY, today);
            // 預測本次投注後的潛在虧損
            BigDecimal projectedLoss = dailyLoss.add(stakeAmount);

            if (projectedLoss.compareTo(setting.getDailyLossLimit()) > 0) {
                BigDecimal remaining = setting.getDailyLossLimit().subtract(dailyLoss)
                    .max(BigDecimal.ZERO);
                return Option.some(LossLimitBreachResult.builder()
                    .limitType(LimitType.DAILY)
                    .limit(setting.getDailyLossLimit())
                    .currentLoss(dailyLoss)
                    .remainingAllowedLoss(remaining)
                    .message("超過每日虧損限額")
                    .build());
            }
        }

        // 檢查週虧損限額
        if (setting.getWeeklyLossLimit() != null) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            BigDecimal weeklyLoss = getCurrentLoss(playerId, PeriodType.WEEKLY, weekStart);
            BigDecimal projectedLoss = weeklyLoss.add(stakeAmount);

            if (projectedLoss.compareTo(setting.getWeeklyLossLimit()) > 0) {
                BigDecimal remaining = setting.getWeeklyLossLimit().subtract(weeklyLoss)
                    .max(BigDecimal.ZERO);
                return Option.some(LossLimitBreachResult.builder()
                    .limitType(LimitType.WEEKLY)
                    .limit(setting.getWeeklyLossLimit())
                    .currentLoss(weeklyLoss)
                    .remainingAllowedLoss(remaining)
                    .message("超過每週虧損限額")
                    .build());
            }
        }

        // 檢查月虧損限額
        if (setting.getMonthlyLossLimit() != null) {
            LocalDate monthStart = today.withDayOfMonth(1);
            BigDecimal monthlyLoss = getCurrentLoss(playerId, PeriodType.MONTHLY, monthStart);
            BigDecimal projectedLoss = monthlyLoss.add(stakeAmount);

            if (projectedLoss.compareTo(setting.getMonthlyLossLimit()) > 0) {
                BigDecimal remaining = setting.getMonthlyLossLimit().subtract(monthlyLoss)
                    .max(BigDecimal.ZERO);
                return Option.some(LossLimitBreachResult.builder()
                    .limitType(LimitType.MONTHLY)
                    .limit(setting.getMonthlyLossLimit())
                    .currentLoss(monthlyLoss)
                    .remainingAllowedLoss(remaining)
                    .message("超過每月虧損限額")
                    .build());
            }
        }

        return Option.none();
    }

    /**
     * 記錄投注結果（更新虧損累計）
     */
    @Transactional(rollbackFor = Throwable.class)
    public void recordBetResult(Long playerId, BigDecimal stakeAmount, BigDecimal winAmount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 更新日累計
        updateAccumulation(playerId, PeriodType.DAILY, today, stakeAmount, winAmount);

        // 更新週累計
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        updateAccumulation(playerId, PeriodType.WEEKLY, weekStart, stakeAmount, winAmount);

        // 更新月累計
        LocalDate monthStart = today.withDayOfMonth(1);
        updateAccumulation(playerId, PeriodType.MONTHLY, monthStart, stakeAmount, winAmount);
    }

    private void updateAccumulation(Long playerId, PeriodType periodType,
                                   LocalDate periodStart,
                                   BigDecimal stakeAmount, BigDecimal winAmount) {
        LossAccumulation accumulation = lossAccumulationDao
            .findByPlayerAndPeriod(playerId, periodType, periodStart)
            .getOrElse(() -> {
                LossAccumulation newAcc = new LossAccumulation();
                newAcc.setPlayerId(playerId);
                newAcc.setPeriodType(periodType);
                newAcc.setPeriodStart(periodStart);
                newAcc.setTotalStake(BigDecimal.ZERO);
                newAcc.setTotalWin(BigDecimal.ZERO);
                newAcc.setNetLoss(BigDecimal.ZERO);
                return newAcc;
            });

        accumulation.setTotalStake(accumulation.getTotalStake().add(stakeAmount));
        accumulation.setTotalWin(accumulation.getTotalWin().add(winAmount));
        accumulation.setNetLoss(
            accumulation.getTotalStake().subtract(accumulation.getTotalWin())
        );

        lossAccumulationDao.saveOrUpdate(accumulation);
    }

    /**
     * 獲取當前虧損
     */
    private BigDecimal getCurrentLoss(Long playerId, PeriodType periodType, LocalDate periodStart) {
        return lossAccumulationDao
            .findByPlayerAndPeriod(playerId, periodType, periodStart)
            .map(LossAccumulation::getNetLoss)
            .getOrElse(BigDecimal.ZERO);
    }

    /**
     * 設定虧損限額
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<LimitUpdateResultVO> setLossLimits(Long playerId, LossLimitForm form) {
        LossLimitSetting setting = lossLimitSettingDao.findByPlayerId(playerId)
            .getOrElse(() -> {
                LossLimitSetting newSetting = new LossLimitSetting();
                newSetting.setPlayerId(playerId);
                return newSetting;
            });

        List<LimitChange> changes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 處理各限額變更（與存款限額類似的冷靜期邏輯）
        if (form.getDailyLossLimit() != null) {
            changes.add(processLimitChange(setting, LimitType.DAILY,
                setting.getDailyLossLimit(), form.getDailyLossLimit(), now));
        }

        // ... 週/月限額處理

        lossLimitSettingDao.saveOrUpdate(setting);

        return ResponseDTO.ok(LimitUpdateResultVO.builder()
            .currentLimits(buildCurrentLimits(setting))
            .pendingChanges(buildPendingChanges(changes))
            .build());
    }

    /**
     * 獲取虧損限額使用情況
     */
    public LossLimitUsageVO getLossLimitUsage(Long playerId) {
        Option<LossLimitSetting> settingOpt = lossLimitSettingDao.findByPlayerId(playerId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (settingOpt.isEmpty()) {
            return LossLimitUsageVO.noLimits();
        }

        LossLimitSetting setting = settingOpt.get();

        return LossLimitUsageVO.builder()
            .daily(buildUsage(
                setting.getDailyLossLimit(),
                getCurrentLoss(playerId, PeriodType.DAILY, today)
            ))
            .weekly(buildUsage(
                setting.getWeeklyLossLimit(),
                getCurrentLoss(playerId, PeriodType.WEEKLY, today.with(DayOfWeek.MONDAY))
            ))
            .monthly(buildUsage(
                setting.getMonthlyLossLimit(),
                getCurrentLoss(playerId, PeriodType.MONTHLY, today.withDayOfMonth(1))
            ))
            .build();
    }
}
```

### 與投注服務整合

```java
@Service
@RequiredArgsConstructor
public class BetService {

    private final LossLimitService lossLimitService;
    private final DepositLimitService depositLimitService;
    private final WalletService walletService;

    /**
     * 下注前檢查
     */
    public ResponseDTO<BetPlacementResult> placeBet(Long playerId, BetForm form) {
        // 1. 檢查自我排除
        if (selfExclusionService.isExcluded(playerId)) {
            return ResponseDTO.error(UserErrorCode.PLAYER_EXCLUDED);
        }

        // 2. 檢查餘額
        BigDecimal balance = walletService.getBalance(playerId);
        if (balance.compareTo(form.getStakeAmount()) < 0) {
            return ResponseDTO.error(UserErrorCode.INSUFFICIENT_BALANCE);
        }

        // 3. 檢查虧損限額 ⭐
        Option<LossLimitBreachResult> lossBreachOpt =
            lossLimitService.checkLossLimit(playerId, form.getStakeAmount());

        if (lossBreachOpt.isDefined()) {
            LossLimitBreachResult breach = lossBreachOpt.get();
            return ResponseDTO.error(UserErrorCode.LOSS_LIMIT_EXCEEDED,
                String.format("超過%s虧損限額。當前虧損: %s, 限額: %s, 剩餘可虧損: %s",
                    breach.getLimitType().getDisplayName(),
                    breach.getCurrentLoss(),
                    breach.getLimit(),
                    breach.getRemainingAllowedLoss()
                ));
        }

        // 4. 執行下注
        BetResult result = executeBet(playerId, form);

        // 5. 記錄投注結果（異步更新虧損累計）
        CompletableFuture.runAsync(() -> {
            lossLimitService.recordBetResult(
                playerId,
                form.getStakeAmount(),
                result.getWinAmount()
            );
        });

        return ResponseDTO.ok(result);
    }
}
```

---

## 前端整合

### 虧損限額設定

```vue
<template>
  <a-card title="虧損限額設定">
    <a-alert
      type="info"
      show-icon
      class="mb-4"
    >
      <template #description>
        虧損限額限制您在特定時間內的淨虧損金額。
        當您贏錢時，虧損累計會減少；當您輸錢時，虧損累計會增加。
      </template>
    </a-alert>

    <a-form :model="form" @finish="handleSubmit">
      <!-- 日虧損限額 -->
      <a-form-item label="每日虧損限額">
        <a-input-number
          v-model:value="form.dailyLossLimit"
          :min="0"
          :precision="2"
          style="width: 200px"
        >
          <template #addonBefore>$</template>
        </a-input-number>
        <template #extra v-if="usage.daily">
          <a-progress
            :percent="usage.daily.percentage"
            :status="getProgressStatus(usage.daily.percentage)"
            size="small"
          />
          <span>
            今日淨虧損: {{ formatMoney(usage.daily.current) }} /
            {{ formatMoney(usage.daily.limit) }}
          </span>
        </template>
      </a-form-item>

      <!-- 週/月虧損限額... -->

      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">
          儲存設定
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>
```

---

## 監控與告警

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 虧損限額觸發次數 | `loss_limit_breaches_total` | 按限額類型分類 |
| 玩家虧損分布 | `player_loss_distribution` | 分析虧損集中度 |
| 接近限額警告 | `loss_limit_warning_total` | 達到 80% 時的警告 |

---

## 相關文檔

- [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) - 存款限額
- [15-05_Reality_Checks.md](15-05_Reality_Checks.md) - 現實檢查
- [15-08_Affordability_Assessment.md](15-08_Affordability_Assessment.md) - 可負擔性評估

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
