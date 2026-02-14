# 15-03 Cooling-Off Period (冷靜期/暫停功能)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

冷靜期 (Cooling-Off Period / Time-Out) 是介於存款限額與自我排除之間的玩家保護工具，允許玩家暫時停止博彩活動而不需要提交正式的自我排除申請。

### 與自我排除的區別

| 特性 | 冷靜期 | 自我排除 |
|------|--------|---------|
| **目的** | 短期休息 | 長期/永久禁入 |
| **期限** | 24 小時 ~ 6 週 | 6 個月 ~ 終身 |
| **解除** | 自動解除 | 需申請 + 冷靜期 |
| **跨平台** | 僅本平台 | 可同步至 Gamstop |
| **嚴重程度** | 預防性 | 介入性 |

---

## 冷靜期選項

### 標準選項

| 期限 | 適用場景 | 自動解除 |
|------|---------|---------|
| 24 小時 | 當日冷靜 | ✅ |
| 48 小時 | 週末休息 | ✅ |
| 72 小時 | 三天冷靜 | ✅ |
| 7 天 | 一週休息 | ✅ |
| 14 天 | 兩週休息 | ✅ |
| 30 天 | 月度休息 | ✅ |
| 6 週 | 最長冷靜期 | ✅ |

### 客製化選項（可選）

允許玩家選擇具體結束日期：
- 最短：24 小時後
- 最長：6 週後
- 結束時間：固定為選擇日 UTC 23:59:59

---

## 業務規則

### 啟動冷靜期

```
玩家請求 → 確認對話框 → 立即生效 → 關閉活動會話
     │
     └── 不影響：餘額、未結投注、獎金進度
```

**規則**:
1. 立即生效，無需額外確認
2. 保留帳戶餘額
3. 保留未結投注（等待結果）
4. 保留獎金進度（但無法繼續累積）
5. 禁止存款、遊戲、投注
6. 可以申請出金

### 冷靜期期間

**允許操作**:
- 查看帳戶餘額
- 查看投注歷史
- 申請出金
- 聯繫客服
- 查看負責任博彩資源

**禁止操作**:
- 存款
- 遊戲
- 投注
- 領取獎金
- 參與促銷活動

### 解除冷靜期

```
冷靜期結束 → 系統自動解除 → 發送通知 → 帳戶恢復
```

**規則**:
1. 到期自動解除，無需操作
2. 發送解除通知（郵件/App 推送）
3. 帳戶完全恢復

### 提前解除（可選功能）

根據牌照要求，部分運營商允許提前解除：

```
玩家申請提前解除 → 24 小時確認期 → 確認後解除
```

**注意**: UK 牌照**不建議**提供提前解除功能。

---

## 技術實現

### 資料庫設計

```sql
-- 冷靜期記錄表
CREATE TABLE t_cooling_off_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    start_time          DATETIME NOT NULL,
    end_time            DATETIME NOT NULL,
    duration_type       VARCHAR(20) NOT NULL,  -- H24, H48, H72, D7, D14, D30, W6, CUSTOM
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, COMPLETED, CANCELLED

    -- 提前解除相關
    early_release_requested BOOLEAN DEFAULT FALSE,
    early_release_request_time DATETIME,
    early_release_effective_time DATETIME,

    -- 觸發來源
    trigger_source      VARCHAR(50),  -- PLAYER, OPERATOR, RISK_SYSTEM
    trigger_reason      VARCHAR(500),

    -- 審計
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_end_time (end_time),
    INDEX idx_status (status)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CoolingOffService {

    private final CoolingOffRecordDao coolingOffRecordDao;
    private final PlayerSessionManager sessionManager;
    private final NotificationService notificationService;

    /**
     * 啟動冷靜期
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<CoolingOffResultVO> startCoolingOff(
            Long playerId,
            CoolingOffForm form) {

        // 1. 檢查是否已在冷靜期或排除狀態
        if (isInCoolingOff(playerId)) {
            return ResponseDTO.error(UserErrorCode.ALREADY_IN_COOLING_OFF);
        }

        if (selfExclusionService.isExcluded(playerId)) {
            return ResponseDTO.error(UserErrorCode.ALREADY_EXCLUDED);
        }

        // 2. 計算結束時間
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = calculateEndTime(startTime, form.getDuration());

        // 3. 創建記錄
        CoolingOffRecord record = CoolingOffRecord.builder()
            .playerId(playerId)
            .startTime(startTime)
            .endTime(endTime)
            .durationType(form.getDuration())
            .status(CoolingOffStatus.ACTIVE)
            .triggerSource(TriggerSource.PLAYER)
            .triggerReason(form.getReason())
            .build();
        coolingOffRecordDao.insert(record);

        // 4. 關閉活動會話
        sessionManager.terminateAllSessions(playerId, "COOLING_OFF");

        // 5. 發送確認通知
        notificationService.sendCoolingOffStarted(playerId, record);

        log.info("Cooling-off started: playerId={}, duration={}, endTime={}",
            playerId, form.getDuration(), endTime);

        return ResponseDTO.ok(CoolingOffResultVO.builder()
            .status(CoolingOffStatus.ACTIVE)
            .startTime(startTime)
            .endTime(endTime)
            .message("冷靜期已啟動，將於 " + formatDateTime(endTime) + " 結束")
            .build());
    }

    /**
     * 檢查玩家是否在冷靜期
     */
    public boolean isInCoolingOff(Long playerId) {
        Option<CoolingOffRecord> recordOpt =
            coolingOffRecordDao.findActiveByPlayerId(playerId);
        return recordOpt.isDefined();
    }

    /**
     * 獲取冷靜期狀態
     */
    public Option<CoolingOffStatusVO> getCoolingOffStatus(Long playerId) {
        return coolingOffRecordDao.findActiveByPlayerId(playerId)
            .map(record -> CoolingOffStatusVO.builder()
                .active(true)
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .remainingHours(calculateRemainingHours(record.getEndTime()))
                .build());
    }

    /**
     * 定時任務：處理到期的冷靜期
     */
    @Scheduled(fixedRate = 60000) // 每分鐘執行
    public void processExpiredCoolingOff() {
        LocalDateTime now = LocalDateTime.now();
        List<CoolingOffRecord> expiredRecords =
            coolingOffRecordDao.findExpired(now);

        for (CoolingOffRecord record : expiredRecords) {
            record.setStatus(CoolingOffStatus.COMPLETED);
            coolingOffRecordDao.updateById(record);

            notificationService.sendCoolingOffEnded(record.getPlayerId());

            log.info("Cooling-off completed: playerId={}", record.getPlayerId());
        }
    }

    /**
     * 請求提前解除冷靜期（可選功能）
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<EarlyReleaseResultVO> requestEarlyRelease(Long playerId) {
        // 檢查牌照是否允許
        if (!jurisdictionService.allowsEarlyRelease(playerId)) {
            return ResponseDTO.error(UserErrorCode.EARLY_RELEASE_NOT_ALLOWED);
        }

        Option<CoolingOffRecord> recordOpt =
            coolingOffRecordDao.findActiveByPlayerId(playerId);

        if (recordOpt.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.NOT_IN_COOLING_OFF);
        }

        CoolingOffRecord record = recordOpt.get();
        LocalDateTime effectiveTime = LocalDateTime.now().plusHours(24);

        record.setEarlyReleaseRequested(true);
        record.setEarlyReleaseRequestTime(LocalDateTime.now());
        record.setEarlyReleaseEffectiveTime(effectiveTime);
        coolingOffRecordDao.updateById(record);

        return ResponseDTO.ok(EarlyReleaseResultVO.builder()
            .status("PENDING")
            .effectiveTime(effectiveTime)
            .message("提前解除申請已提交，將於 24 小時後生效")
            .build());
    }

    private LocalDateTime calculateEndTime(LocalDateTime start, DurationType duration) {
        return switch (duration) {
            case H24 -> start.plusHours(24);
            case H48 -> start.plusHours(48);
            case H72 -> start.plusHours(72);
            case D7 -> start.plusDays(7);
            case D14 -> start.plusDays(14);
            case D30 -> start.plusDays(30);
            case W6 -> start.plusWeeks(6);
            default -> start.plusHours(24);
        };
    }
}
```

### 登入攔截器整合

```java
@Component
@RequiredArgsConstructor
public class PlayerAccessInterceptor implements HandlerInterceptor {

    private final SelfExclusionService selfExclusionService;
    private final CoolingOffService coolingOffService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {

        Long playerId = getCurrentPlayerId(request);
        if (playerId == null) {
            return true; // 未登入，交給認證處理
        }

        String path = request.getRequestURI();

        // 檢查自我排除
        if (selfExclusionService.isExcluded(playerId)) {
            if (!isAllowedForExcludedPlayer(path)) {
                throw new PlayerExcludedException("帳戶已被排除");
            }
        }

        // 檢查冷靜期
        if (coolingOffService.isInCoolingOff(playerId)) {
            if (!isAllowedDuringCoolingOff(path)) {
                Option<CoolingOffStatusVO> status =
                    coolingOffService.getCoolingOffStatus(playerId);
                throw new CoolingOffActiveException(
                    "帳戶處於冷靜期，將於 " +
                    status.get().getEndTime() + " 結束"
                );
            }
        }

        return true;
    }

    /**
     * 冷靜期允許的路徑
     */
    private boolean isAllowedDuringCoolingOff(String path) {
        return path.startsWith("/api/v1/player/profile") ||
               path.startsWith("/api/v1/player/wallet/balance") ||
               path.startsWith("/api/v1/player/wallet/withdraw") ||
               path.startsWith("/api/v1/player/history") ||
               path.startsWith("/api/v1/player/protection") ||
               path.startsWith("/api/v1/support");
    }
}
```

---

## 前端整合

### 冷靜期設定界面

```vue
<template>
  <a-card title="暫停帳戶（冷靜期）">
    <a-alert
      type="info"
      show-icon
      class="mb-4"
    >
      <template #message>什麼是冷靜期？</template>
      <template #description>
        冷靜期允許您暫時停止博彩活動，期間您無法存款或遊戲，
        但可以查看帳戶和申請出金。期滿後帳戶將自動恢復。
      </template>
    </a-alert>

    <a-form
      :model="form"
      @finish="handleSubmit"
    >
      <a-form-item label="選擇冷靜期時長">
        <a-radio-group v-model:value="form.duration" button-style="solid">
          <a-radio-button value="H24">24 小時</a-radio-button>
          <a-radio-button value="H48">48 小時</a-radio-button>
          <a-radio-button value="H72">72 小時</a-radio-button>
          <a-radio-button value="D7">7 天</a-radio-button>
          <a-radio-button value="D14">14 天</a-radio-button>
          <a-radio-button value="D30">30 天</a-radio-button>
          <a-radio-button value="W6">6 週</a-radio-button>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="暫停原因（選填）">
        <a-textarea
          v-model:value="form.reason"
          :rows="3"
          placeholder="例如：需要休息一下..."
        />
      </a-form-item>

      <a-alert
        type="warning"
        show-icon
        class="mb-4"
      >
        <template #description>
          冷靜期將立即生效，您將被登出並無法登入直到期滿。
        </template>
      </a-alert>

      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">
          啟動冷靜期
        </a-button>
      </a-form-item>
    </a-form>

    <!-- 需要更長時間？引導至自我排除 -->
    <a-divider />
    <p>
      需要更長的休息時間？
      <a-button type="link" @click="goToSelfExclusion">
        了解自我排除
      </a-button>
    </p>
  </a-card>
</template>
```

---

## 監控與告警

### 關鍵指標

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 冷靜期啟動數 | `rg_cooling_off_started_total` | 按時長分類 |
| 當前冷靜期玩家數 | `rg_cooling_off_active_gauge` | 實時數量 |
| 冷靜期完成數 | `rg_cooling_off_completed_total` | 正常結束 |
| 提前解除數 | `rg_cooling_off_early_release_total` | 如果啟用 |

---

## 相關文檔

- [15-01_Self_Exclusion.md](15-01_Self_Exclusion.md) - 自我排除
- [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) - 存款限額
- [15-04_Session_Management.md](15-04_Session_Management.md) - 會話管理

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
