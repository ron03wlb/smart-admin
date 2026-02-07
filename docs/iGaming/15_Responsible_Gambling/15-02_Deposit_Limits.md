# 15-02 Deposit Limits (存款限額管理)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

存款限額 (Deposit Limits) 是幫助玩家控制博彩支出的核心工具。本文檔詳細說明存款限額的設計與實現，包括日/週/月限額、冷靜期機制、以及與支付系統的整合。

### 監管要求

| 監管機構 | 條款 | 限額類型 | 特殊要求 |
|---------|------|---------|---------|
| **UKGC** | LCCP SR 3.4.1 | 日/週/月 | 必須在首次存款前提示設定 |
| **MGA** | Player Protection Directive | 日/週/月 | 必須提供限額選項 |
| **PAGCOR** | Responsible Gaming Guidelines | 日/週/月 | 運營商自定義 |
| **Netherlands** | KOA Remote Gambling Act | 日/週/月 | 強制設定上限 |

---

## 限額類型

### 1. 時間維度限額

| 限額類型 | 計算週期 | 重置時間 | 說明 |
|---------|---------|---------|------|
| 日限額 | 24 小時 | UTC 00:00 | 限制每日存款總額 |
| 週限額 | 7 天 | 週一 UTC 00:00 | 限制每週存款總額 |
| 月限額 | 30 天 | 每月 1 日 UTC 00:00 | 限制每月存款總額 |

### 2. 層級關係

```
月限額 ≥ 週限額 × 4 (建議)
週限額 ≥ 日限額 × 7 (建議)

驗證順序: 日限額 → 週限額 → 月限額
任一限額達到 → 阻止存款
```

### 3. 預設限額（可選配置）

| 牌照 | 預設日限額 | 預設週限額 | 預設月限額 |
|------|-----------|-----------|-----------|
| UK | 無預設 | 無預設 | 無預設 |
| NL | €200 | €700 | €2,000 |
| DE | €1,000/月 (法定上限) | - | €1,000 |

---

## 業務規則

### 降低限額

```
玩家請求降低限額 → 立即生效 → 發送確認通知
```

**規則**:
- 立即生效，無需冷靜期
- 新限額立即適用於當期累計
- 如果當期已存款超過新限額，不追溯但阻止後續存款

### 提高限額

```
玩家請求提高限額 → 24-72 小時冷靜期 → 確認後生效
```

**規則**:
- 需要 24-72 小時冷靜期（依牌照要求）
- 冷靜期內玩家可取消申請
- 需要二次確認才能生效
- 記錄原因（可選）

### 移除限額

```
玩家請求移除限額 → 與提高限額相同流程 → 設為「無限額」
```

---

## 技術實現

### 資料庫設計

```sql
-- 存款限額設定表
CREATE TABLE t_deposit_limit_setting (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,

    -- 當前生效限額
    daily_limit         DECIMAL(18,2),      -- NULL = 無限額
    weekly_limit        DECIMAL(18,2),
    monthly_limit       DECIMAL(18,2),

    -- 待生效限額（提高限額冷靜期中）
    pending_daily_limit     DECIMAL(18,2),
    pending_weekly_limit    DECIMAL(18,2),
    pending_monthly_limit   DECIMAL(18,2),
    pending_effective_time  DATETIME,

    -- 限額來源
    limit_source        VARCHAR(20) DEFAULT 'PLAYER',  -- PLAYER, OPERATOR, REGULATOR, AFFORDABILITY

    -- 審計
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id)
);

-- 存款限額變更歷史
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

-- 存款累計表（用於限額檢查）
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

### 核心服務實現

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
     * 檢查存款是否超過限額
     *
     * @return Option.none() 表示可以存款，Option.some() 包含錯誤訊息
     */
    public Option<LimitBreachResult> checkDepositLimit(Long playerId, BigDecimal amount) {
        Option<DepositLimitSetting> settingOpt = limitSettingDao.findByPlayerId(playerId);

        if (settingOpt.isEmpty()) {
            // 無限額設定，允許存款
            return Option.none();
        }

        DepositLimitSetting setting = settingOpt.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 檢查日限額
        if (setting.getDailyLimit() != null) {
            BigDecimal dailyAccumulated = getAccumulatedAmount(playerId, PeriodType.DAILY, today);
            BigDecimal dailyRemaining = setting.getDailyLimit().subtract(dailyAccumulated);

            if (amount.compareTo(dailyRemaining) > 0) {
                return Option.some(LimitBreachResult.builder()
                    .limitType(LimitType.DAILY)
                    .limit(setting.getDailyLimit())
                    .accumulated(dailyAccumulated)
                    .remaining(dailyRemaining.max(BigDecimal.ZERO))
                    .message("超過每日存款限額")
                    .build());
            }
        }

        // 檢查週限額
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
                    .message("超過每週存款限額")
                    .build());
            }
        }

        // 檢查月限額
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
                    .message("超過每月存款限額")
                    .build());
            }
        }

        return Option.none();
    }

    /**
     * 設定存款限額
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<LimitUpdateResultVO> setDepositLimits(
            Long playerId,
            DepositLimitForm form) {

        DepositLimitSetting setting = limitSettingDao.findByPlayerId(playerId)
            .getOrElse(() -> createDefaultSetting(playerId));

        List<LimitChange> changes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 處理日限額變更
        if (form.getDailyLimit() != null) {
            changes.add(processLimitChange(
                setting, LimitType.DAILY,
                setting.getDailyLimit(), form.getDailyLimit(),
                now
            ));
        }

        // 處理週限額變更
        if (form.getWeeklyLimit() != null) {
            changes.add(processLimitChange(
                setting, LimitType.WEEKLY,
                setting.getWeeklyLimit(), form.getWeeklyLimit(),
                now
            ));
        }

        // 處理月限額變更
        if (form.getMonthlyLimit() != null) {
            changes.add(processLimitChange(
                setting, LimitType.MONTHLY,
                setting.getMonthlyLimit(), form.getMonthlyLimit(),
                now
            ));
        }

        // 驗證限額層級關係
        if (!validateLimitHierarchy(setting)) {
            return ResponseDTO.error(UserErrorCode.INVALID_LIMIT_HIERARCHY,
                "限額設定不合理：月限額應 ≥ 週限額，週限額應 ≥ 日限額");
        }

        limitSettingDao.saveOrUpdate(setting);
        recordLimitHistory(playerId, changes);

        // 發送通知
        notificationService.sendLimitUpdateConfirmation(playerId, changes);

        return ResponseDTO.ok(LimitUpdateResultVO.builder()
            .currentLimits(buildCurrentLimits(setting))
            .pendingChanges(buildPendingChanges(changes))
            .build());
    }

    /**
     * 處理單個限額變更
     */
    private LimitChange processLimitChange(
            DepositLimitSetting setting,
            LimitType limitType,
            BigDecimal oldValue,
            BigDecimal newValue,
            LocalDateTime now) {

        boolean isDecrease = newValue == null ||
            (oldValue != null && newValue.compareTo(oldValue) < 0);

        if (isDecrease) {
            // 降低限額：立即生效
            switch (limitType) {
                case DAILY -> setting.setDailyLimit(newValue);
                case WEEKLY -> setting.setWeeklyLimit(newValue);
                case MONTHLY -> setting.setMonthlyLimit(newValue);
            }
            return LimitChange.immediate(limitType, oldValue, newValue);
        } else {
            // 提高限額：設定待生效
            int coolingOffHours = getCoolingOffHours();
            LocalDateTime effectiveTime = now.plusHours(coolingOffHours);

            switch (limitType) {
                case DAILY -> {
                    setting.setPendingDailyLimit(newValue);
                    setting.setPendingEffectiveTime(effectiveTime);
                }
                case WEEKLY -> {
                    setting.setPendingWeeklyLimit(newValue);
                    setting.setPendingEffectiveTime(effectiveTime);
                }
                case MONTHLY -> {
                    setting.setPendingMonthlyLimit(newValue);
                    setting.setPendingEffectiveTime(effectiveTime);
                }
            }
            return LimitChange.pending(limitType, oldValue, newValue, effectiveTime);
        }
    }

    /**
     * 定時任務：處理待生效的限額提高
     */
    @Scheduled(fixedRate = 60000) // 每分鐘執行
    public void processPendingLimitIncreases() {
        LocalDateTime now = LocalDateTime.now();
        List<DepositLimitSetting> pendingSettings =
            limitSettingDao.findPendingEffective(now);

        for (DepositLimitSetting setting : pendingSettings) {
            applyPendingLimits(setting);
            limitSettingDao.updateById(setting);

            notificationService.sendLimitIncreaseEffective(setting.getPlayerId());

            log.info("Pending limit increase applied: playerId={}",
                setting.getPlayerId());
        }
    }

    /**
     * 取消待生效的限額提高
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> cancelPendingIncrease(Long playerId) {
        Option<DepositLimitSetting> settingOpt = limitSettingDao.findByPlayerId(playerId);

        if (settingOpt.isEmpty() || settingOpt.get().getPendingEffectiveTime() == null) {
            return ResponseDTO.error(UserErrorCode.NO_PENDING_LIMIT_CHANGE);
        }

        DepositLimitSetting setting = settingOpt.get();
        setting.setPendingDailyLimit(null);
        setting.setPendingWeeklyLimit(null);
        setting.setPendingMonthlyLimit(null);
        setting.setPendingEffectiveTime(null);
        limitSettingDao.updateById(setting);

        log.info("Pending limit increase cancelled: playerId={}", playerId);

        return ResponseDTO.ok();
    }

    /**
     * 記錄存款後更新累計
     */
    public void recordDeposit(Long playerId, BigDecimal amount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 更新日累計
        updateAccumulation(playerId, PeriodType.DAILY, today, amount);

        // 更新週累計
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        updateAccumulation(playerId, PeriodType.WEEKLY, weekStart, amount);

        // 更新月累計
        LocalDate monthStart = today.withDayOfMonth(1);
        updateAccumulation(playerId, PeriodType.MONTHLY, monthStart, amount);
    }

    /**
     * 獲取限額使用情況
     */
    public LimitUsageVO getLimitUsage(Long playerId) {
        Option<DepositLimitSetting> settingOpt = limitSettingDao.findByPlayerId(playerId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (settingOpt.isEmpty()) {
            return LimitUsageVO.noLimits();
        }

        DepositLimitSetting setting = settingOpt.get();

        return LimitUsageVO.builder()
            .daily(buildUsage(setting.getDailyLimit(),
                getAccumulatedAmount(playerId, PeriodType.DAILY, today)))
            .weekly(buildUsage(setting.getWeeklyLimit(),
                getAccumulatedAmount(playerId, PeriodType.WEEKLY, today.with(DayOfWeek.MONDAY))))
            .monthly(buildUsage(setting.getMonthlyLimit(),
                getAccumulatedAmount(playerId, PeriodType.MONTHLY, today.withDayOfMonth(1))))
            .pendingChanges(buildPendingChangesVO(setting))
            .build();
    }

    private int getCoolingOffHours() {
        // 可從牌照配置讀取
        return 24; // 預設 24 小時
    }
}
```

### 與支付系統整合

```java
@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositLimitService depositLimitService;
    private final PaymentGatewayService paymentGatewayService;
    private final WalletService walletService;

    /**
     * 存款流程 - 包含限額檢查
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<DepositResultVO> deposit(Long playerId, DepositForm form) {
        // 1. 檢查自我排除
        if (selfExclusionService.isExcluded(playerId)) {
            return ResponseDTO.error(UserErrorCode.PLAYER_EXCLUDED);
        }

        // 2. 檢查存款限額 ⭐
        Option<LimitBreachResult> limitBreachOpt =
            depositLimitService.checkDepositLimit(playerId, form.getAmount());

        if (limitBreachOpt.isDefined()) {
            LimitBreachResult breach = limitBreachOpt.get();
            return ResponseDTO.error(UserErrorCode.DEPOSIT_LIMIT_EXCEEDED,
                String.format("超過%s限額。限額: %s, 已存款: %s, 可存款: %s",
                    breach.getLimitType().getDisplayName(),
                    breach.getLimit(),
                    breach.getAccumulated(),
                    breach.getRemaining()
                ));
        }

        // 3. 執行存款
        PaymentResult paymentResult = paymentGatewayService.processDeposit(playerId, form);

        if (paymentResult.isSuccess()) {
            // 4. 更新錢包餘額
            walletService.credit(playerId, form.getAmount(), TransactionType.DEPOSIT);

            // 5. 記錄存款累計 ⭐
            depositLimitService.recordDeposit(playerId, form.getAmount());

            return ResponseDTO.ok(DepositResultVO.success(paymentResult));
        }

        return ResponseDTO.error(UserErrorCode.DEPOSIT_FAILED, paymentResult.getErrorMessage());
    }
}
```

---

## 前端整合

### 限額設定界面

```vue
<template>
  <a-card title="存款限額設定">
    <a-form
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
    >
      <!-- 日限額 -->
      <a-form-item label="每日限額" name="dailyLimit">
        <a-input-number
          v-model:value="form.dailyLimit"
          :min="0"
          :step="100"
          :precision="2"
          style="width: 200px"
        >
          <template #addonBefore>$</template>
        </a-input-number>
        <template #extra>
          <span v-if="currentUsage.daily">
            今日已存: {{ currentUsage.daily.accumulated }} / {{ currentUsage.daily.limit }}
            <a-progress
              :percent="currentUsage.daily.percentage"
              :status="currentUsage.daily.percentage >= 80 ? 'exception' : 'active'"
              size="small"
            />
          </span>
        </template>
      </a-form-item>

      <!-- 週限額 -->
      <a-form-item label="每週限額" name="weeklyLimit">
        <a-input-number
          v-model:value="form.weeklyLimit"
          :min="0"
          :step="100"
          :precision="2"
          style="width: 200px"
        >
          <template #addonBefore>$</template>
        </a-input-number>
        <template #extra>
          <span v-if="currentUsage.weekly">
            本週已存: {{ currentUsage.weekly.accumulated }} / {{ currentUsage.weekly.limit }}
          </span>
        </template>
      </a-form-item>

      <!-- 月限額 -->
      <a-form-item label="每月限額" name="monthlyLimit">
        <a-input-number
          v-model:value="form.monthlyLimit"
          :min="0"
          :step="100"
          :precision="2"
          style="width: 200px"
        >
          <template #addonBefore>$</template>
        </a-input-number>
        <template #extra>
          <span v-if="currentUsage.monthly">
            本月已存: {{ currentUsage.monthly.accumulated }} / {{ currentUsage.monthly.limit }}
          </span>
        </template>
      </a-form-item>

      <!-- 待生效變更提示 -->
      <a-alert
        v-if="pendingChanges.length > 0"
        type="info"
        show-icon
        class="mb-4"
      >
        <template #message>待生效的限額變更</template>
        <template #description>
          <ul>
            <li v-for="change in pendingChanges" :key="change.type">
              {{ change.type }}: {{ change.oldValue }} → {{ change.newValue }}
              <br>
              生效時間: {{ change.effectiveTime }}
              <a-button type="link" size="small" @click="cancelPending(change.type)">
                取消
              </a-button>
            </li>
          </ul>
        </template>
      </a-alert>

      <!-- 提高限額警告 -->
      <a-alert
        v-if="hasIncreases"
        type="warning"
        show-icon
        class="mb-4"
      >
        <template #message>提高限額需要 24 小時冷靜期</template>
        <template #description>
          您申請提高的限額將在 24 小時後生效，期間您可以取消此申請。
        </template>
      </a-alert>

      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">
          儲存設定
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { getLimitUsage, setDepositLimits, cancelPendingIncrease } from '@/api/player-protection';

const form = ref({
  dailyLimit: null,
  weeklyLimit: null,
  monthlyLimit: null,
});

const currentUsage = ref({});
const pendingChanges = ref([]);
const loading = ref(false);

const hasIncreases = computed(() => {
  // 檢查是否有任何限額提高
  return (form.value.dailyLimit > (currentUsage.value.daily?.limit || 0)) ||
         (form.value.weeklyLimit > (currentUsage.value.weekly?.limit || 0)) ||
         (form.value.monthlyLimit > (currentUsage.value.monthly?.limit || 0));
});

onMounted(async () => {
  const usage = await getLimitUsage();
  currentUsage.value = usage;
  pendingChanges.value = usage.pendingChanges || [];

  // 預填當前限額
  form.value.dailyLimit = usage.daily?.limit;
  form.value.weeklyLimit = usage.weekly?.limit;
  form.value.monthlyLimit = usage.monthly?.limit;
});

const handleSubmit = async () => {
  loading.value = true;
  try {
    await setDepositLimits(form.value);
    message.success('限額設定已更新');
    // 重新載入使用情況
    const usage = await getLimitUsage();
    currentUsage.value = usage;
    pendingChanges.value = usage.pendingChanges || [];
  } catch (error) {
    message.error(error.message);
  } finally {
    loading.value = false;
  }
};

const cancelPending = async (limitType: string) => {
  await cancelPendingIncrease(limitType);
  message.success('已取消待生效的限額變更');
  const usage = await getLimitUsage();
  pendingChanges.value = usage.pendingChanges || [];
};
</script>
```

---

## 監控與告警

### 關鍵指標

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 設定限額玩家比例 | `rg_deposit_limit_adoption_rate` | 有設定限額的玩家佔比 |
| 限額觸發次數 | `rg_deposit_limit_breaches_total` | 按限額類型分類 |
| 限額調整請求 | `rg_deposit_limit_changes_total` | 按調整方向分類 |
| 待生效限額數 | `rg_pending_limit_increases_gauge` | 等待冷靜期的數量 |

---

## 測試場景

| 場景 | 預期結果 |
|------|---------|
| 存款金額 ≤ 剩餘限額 | 存款成功 |
| 存款金額 > 剩餘限額 | 拒絕存款，顯示剩餘可存金額 |
| 降低限額 | 立即生效 |
| 提高限額 | 進入 24 小時冷靜期 |
| 冷靜期內取消 | 成功取消，維持原限額 |
| 冷靜期結束 | 新限額自動生效 |

---

## 相關文檔

- [15-01_Self_Exclusion.md](15-01_Self_Exclusion.md) - 自我排除
- [15-06_Loss_Limits.md](15-06_Loss_Limits.md) - 虧損限額
- [15-08_Affordability_Assessment.md](15-08_Affordability_Assessment.md) - 可負擔性評估
- [02-02_Payment_Gateway_Integration.md](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) - 支付閘道

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
