# 15-01 Self-Exclusion (自我排除系統)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

自我排除 (Self-Exclusion) 是最嚴格的玩家保護工具，允許玩家自願禁止自己進入博彩平台。本文檔詳細說明自我排除系統的設計與實現。

### 監管要求

| 監管機構 | 條款 | 最短期限 | 最長期限 | 特殊要求 |
|---------|------|---------|---------|---------|
| **UKGC** | LCCP SR 3.5.1 | 6 個月 | 5 年+ | 必須對接 Gamstop |
| **MGA** | Player Protection Directive | 6 個月 | 終身 | 國家排除庫 |
| **PAGCOR** | Responsible Gaming Guidelines | 6 個月 | 1 年 | 運營商管理 |
| **Brazil SPA** | Portaria SPA | 3 個月 | 5 年 | 2026 國家庫上線 |

---

## 排除類型

### 1. 暫時排除 (Temporary Exclusion)

| 期限選項 | 適用場景 | 解除條件 |
|---------|---------|---------|
| 24 小時 | 短期冷靜 | 自動解除 |
| 7 天 | 短期控制 | 自動解除 |
| 30 天 | 月度休息 | 自動解除 |
| 6 個月 | 中期排除 | 需申請 + 24h 冷靜期 |
| 1 年 | 長期排除 | 需申請 + 7 天冷靜期 |
| 5 年 | 長期排除 | 需申請 + 7 天冷靜期 |

### 2. 永久排除 (Permanent Exclusion)

- **不可撤銷**: 終身禁止進入平台
- **不可轉讓**: 不能轉移到其他帳戶
- **跨平台**: 通過 Gamstop 同步到所有持牌運營商 (UK)

### 3. 運營商排除 (Operator-Initiated Exclusion)

當風控系統識別到高風險行為時，運營商可主動排除玩家：
- 疑似問題博彩行為
- 異常存款模式
- 客服要求協助

---

## 業務流程

### 玩家自我排除流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     Self-Exclusion Flow                          │
└─────────────────────────────────────────────────────────────────┘

┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  玩家    │     │  選擇期限    │     │  確認對話框  │
│  請求    │────▶│  (24h-永久)  │────▶│  (顯示後果)  │
└──────────┘     └──────────────┘     └──────┬───────┘
                                              │
                      ┌───────────────────────┴───────────────────┐
                      │                                           │
                      ▼                                           ▼
               ┌──────────────┐                           ┌──────────────┐
               │  24h 冷靜期  │ (期限 > 6 個月)           │  立即生效   │
               │  確認視窗    │                           │ (期限 ≤ 30天)│
               └──────┬───────┘                           └──────┬───────┘
                      │                                           │
                      ▼                                           │
               ┌──────────────┐                                   │
               │  確認排除    │                                   │
               │  (24h 後)    │                                   │
               └──────┬───────┘                                   │
                      │                                           │
                      └────────────────┬──────────────────────────┘
                                       │
                                       ▼
                               ┌──────────────┐
                               │  執行排除    │
                               │  動作清單    │
                               └──────┬───────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
              ▼                        ▼                        ▼
       ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
       │ 關閉活動     │        │ 清算未結    │        │ 同步至       │
       │ 會話/遊戲    │        │ 投注/獎金   │        │ Gamstop (UK) │
       └──────────────┘        └──────────────┘        └──────────────┘
```

### 解除排除流程

```
排除期滿 → 24h/7d 冷靜期 → 確認解除 → 恢復帳戶
    │            │
    │            └── 期間不可登入
    │
    └── 永久排除: 不可解除
```

---

## 技術實現

### 資料庫設計

```sql
-- 排除記錄表
CREATE TABLE t_exclusion_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    exclusion_type      VARCHAR(50) NOT NULL,  -- SELF, GAMSTOP, OPERATOR
    duration_type       VARCHAR(20) NOT NULL,  -- 24H, 7D, 30D, 6M, 1Y, 5Y, PERMANENT
    start_time          DATETIME NOT NULL,
    end_time            DATETIME,              -- NULL = 永久
    reason              VARCHAR(500),
    initiated_by        VARCHAR(50) NOT NULL,  -- PLAYER, OPERATOR, REGULATOR
    external_reference  VARCHAR(100),          -- Gamstop reference

    -- 解除相關
    revocation_status   VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, PENDING_REVOCATION, REVOKED
    revocation_request_time  DATETIME,
    revocation_effective_time DATETIME,
    revocation_reason   VARCHAR(500),

    -- 審計
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_exclusion_type (exclusion_type),
    INDEX idx_end_time (end_time)
);

-- Gamstop 同步記錄
CREATE TABLE t_gamstop_sync_log (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    sync_type           VARCHAR(20) NOT NULL,  -- CHECK, REGISTER, REVOKE
    request_payload     JSON,
    response_payload    JSON,
    gamstop_reference   VARCHAR(100),
    status              VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, PENDING
    error_message       VARCHAR(500),
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_created_at (created_at)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SelfExclusionService {

    private final ExclusionRecordDao exclusionRecordDao;
    private final PlayerSessionManager sessionManager;
    private final GamstopClient gamstopClient;
    private final BetSettlementService betSettlementService;
    private final NotificationService notificationService;

    /**
     * 玩家申請自我排除
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<ExclusionResultVO> requestSelfExclusion(
            Long playerId,
            SelfExclusionForm form) {

        // 1. 驗證玩家狀態
        Option<ExclusionRecord> existingExclusion =
            exclusionRecordDao.findActiveExclusion(playerId);
        if (existingExclusion.isDefined()) {
            return ResponseDTO.error(UserErrorCode.ALREADY_EXCLUDED);
        }

        // 2. 計算排除期限
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = calculateEndTime(startTime, form.getDuration());

        // 3. 判斷是否需要冷靜期確認
        boolean requiresCoolingOff = requiresCoolingOffPeriod(form.getDuration());

        if (requiresCoolingOff && !form.isCoolingOffConfirmed()) {
            // 返回需要冷靜期確認的狀態
            return ResponseDTO.ok(ExclusionResultVO.builder()
                .status(ExclusionStatus.PENDING_CONFIRMATION)
                .confirmationDeadline(startTime.plusHours(24))
                .message("請在 24 小時後確認排除申請")
                .build());
        }

        // 4. 創建排除記錄
        ExclusionRecord record = ExclusionRecord.builder()
            .playerId(playerId)
            .exclusionType(ExclusionType.SELF)
            .durationType(form.getDuration())
            .startTime(startTime)
            .endTime(endTime)
            .reason(form.getReason())
            .initiatedBy(InitiatedBy.PLAYER)
            .revocationStatus(RevocationStatus.ACTIVE)
            .build();
        exclusionRecordDao.insert(record);

        // 5. 執行排除動作
        executeExclusionActions(playerId, record);

        // 6. UK 牌照: 同步至 Gamstop
        if (isUkJurisdiction(playerId)) {
            syncToGamstop(playerId, record);
        }

        // 7. 發送確認通知
        notificationService.sendExclusionConfirmation(playerId, record);

        log.info("Self-exclusion activated: playerId={}, duration={}",
            playerId, form.getDuration());

        return ResponseDTO.ok(ExclusionResultVO.builder()
            .status(ExclusionStatus.ACTIVE)
            .exclusionId(record.getId())
            .startTime(startTime)
            .endTime(endTime)
            .message("自我排除已生效")
            .build());
    }

    /**
     * 執行排除動作清單
     */
    private void executeExclusionActions(Long playerId, ExclusionRecord record) {
        // 1. 關閉所有活動會話
        sessionManager.terminateAllSessions(playerId, "SELF_EXCLUSION");

        // 2. 清算未結投注
        betSettlementService.settleOpenBets(playerId, SettlementType.EXCLUSION);

        // 3. 處理未完成獎金
        // 根據牌照規則: 已達標獎金可提取，未達標獎金取消
        bonusService.processExclusionBonuses(playerId);

        // 4. 阻止行銷通訊
        marketingService.optOutAll(playerId, "SELF_EXCLUSION");

        // 5. 記錄審計日誌
        auditLogService.logExclusion(playerId, record);
    }

    /**
     * 同步至 Gamstop (UK)
     */
    private void syncToGamstop(Long playerId, ExclusionRecord record) {
        try {
            GamstopRegistrationRequest request = buildGamstopRequest(playerId, record);
            GamstopResponse response = gamstopClient.registerExclusion(request);

            record.setExternalReference(response.getReference());
            exclusionRecordDao.updateById(record);

            log.info("Gamstop sync successful: playerId={}, ref={}",
                playerId, response.getReference());
        } catch (Exception e) {
            log.error("Gamstop sync failed: playerId={}", playerId, e);
            // 記錄失敗，後續重試
            gamstopSyncLogDao.insert(GamstopSyncLog.failed(playerId, e.getMessage()));
        }
    }

    /**
     * 檢查玩家是否被排除
     */
    public boolean isExcluded(Long playerId) {
        return exclusionRecordDao.findActiveExclusion(playerId).isDefined();
    }

    /**
     * 解除排除申請
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<RevocationResultVO> requestRevocation(
            Long playerId,
            RevocationForm form) {

        // 1. 查找活動排除記錄
        Option<ExclusionRecord> exclusionOpt =
            exclusionRecordDao.findActiveExclusion(playerId);

        if (exclusionOpt.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.NO_ACTIVE_EXCLUSION);
        }

        ExclusionRecord record = exclusionOpt.get();

        // 2. 永久排除不可解除
        if (DurationType.PERMANENT.equals(record.getDurationType())) {
            return ResponseDTO.error(UserErrorCode.PERMANENT_EXCLUSION_CANNOT_REVOKE);
        }

        // 3. 排除期未滿不可解除
        if (LocalDateTime.now().isBefore(record.getEndTime())) {
            return ResponseDTO.error(UserErrorCode.EXCLUSION_PERIOD_NOT_ENDED,
                "排除期將於 " + record.getEndTime() + " 結束");
        }

        // 4. 設定冷靜期
        int coolingOffDays = getCoolingOffDays(record.getDurationType());
        LocalDateTime effectiveTime = LocalDateTime.now().plusDays(coolingOffDays);

        record.setRevocationStatus(RevocationStatus.PENDING_REVOCATION);
        record.setRevocationRequestTime(LocalDateTime.now());
        record.setRevocationEffectiveTime(effectiveTime);
        record.setRevocationReason(form.getReason());
        exclusionRecordDao.updateById(record);

        // 5. 發送通知
        notificationService.sendRevocationPending(playerId, effectiveTime);

        return ResponseDTO.ok(RevocationResultVO.builder()
            .status(RevocationStatus.PENDING_REVOCATION)
            .effectiveTime(effectiveTime)
            .coolingOffDays(coolingOffDays)
            .message("解除申請已提交，將於 " + effectiveTime + " 生效")
            .build());
    }

    /**
     * 判斷是否需要冷靜期
     */
    private boolean requiresCoolingOffPeriod(DurationType duration) {
        return switch (duration) {
            case H24, D7, D30 -> false;  // 短期排除立即生效
            case M6, Y1, Y5, PERMANENT -> true;  // 長期排除需要確認
        };
    }

    /**
     * 獲取解除冷靜期天數
     */
    private int getCoolingOffDays(DurationType duration) {
        return switch (duration) {
            case M6 -> 1;      // 6 個月排除: 1 天冷靜期
            case Y1, Y5 -> 7;  // 1-5 年排除: 7 天冷靜期
            default -> 0;
        };
    }
}
```

### Gamstop 整合

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class GamstopClient {

    private final RestTemplate restTemplate;

    @Value("${gamstop.api.url}")
    private String apiUrl;

    @Value("${gamstop.api.key}")
    private String apiKey;

    /**
     * 檢查玩家是否在 Gamstop 排除名單
     *
     * 必須在玩家註冊時調用 (UK 2025 即時 KYC 要求)
     */
    public GamstopCheckResult checkExclusion(GamstopCheckRequest request) {
        HttpHeaders headers = createHeaders();
        HttpEntity<GamstopCheckRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GamstopCheckResponse> response = restTemplate.exchange(
                apiUrl + "/exclusion/check",
                HttpMethod.POST,
                entity,
                GamstopCheckResponse.class
            );

            return GamstopCheckResult.builder()
                .excluded(response.getBody().isExcluded())
                .exclusionEndDate(response.getBody().getExclusionEndDate())
                .reference(response.getBody().getReference())
                .build();
        } catch (Exception e) {
            log.error("Gamstop check failed", e);
            throw new GamstopIntegrationException("Gamstop 檢查失敗", e);
        }
    }

    /**
     * 註冊排除至 Gamstop
     */
    public GamstopResponse registerExclusion(GamstopRegistrationRequest request) {
        HttpHeaders headers = createHeaders();
        HttpEntity<GamstopRegistrationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<GamstopResponse> response = restTemplate.exchange(
            apiUrl + "/exclusion/register",
            HttpMethod.POST,
            entity,
            GamstopResponse.class
        );

        return response.getBody();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
    }
}
```

---

## 前端整合

### 自我排除對話框

```vue
<template>
  <a-modal
    v-model:visible="visible"
    title="自我排除"
    :footer="null"
    width="600px"
  >
    <a-alert
      type="warning"
      show-icon
      class="mb-4"
    >
      <template #message>
        <strong>重要提醒</strong>
      </template>
      <template #description>
        自我排除後，您將無法登入帳戶、進行遊戲或存款。
        所有未結投注將被清算，未完成的獎金可能被取消。
      </template>
    </a-alert>

    <a-form
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
    >
      <a-form-item label="排除期限" name="duration">
        <a-radio-group v-model:value="form.duration">
          <a-radio-button value="24H">24 小時</a-radio-button>
          <a-radio-button value="7D">7 天</a-radio-button>
          <a-radio-button value="30D">30 天</a-radio-button>
          <a-radio-button value="6M">6 個月</a-radio-button>
          <a-radio-button value="1Y">1 年</a-radio-button>
          <a-radio-button value="5Y">5 年</a-radio-button>
          <a-radio-button value="PERMANENT">永久</a-radio-button>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="排除原因" name="reason">
        <a-textarea
          v-model:value="form.reason"
          :rows="3"
          placeholder="請說明申請自我排除的原因（選填）"
        />
      </a-form-item>

      <a-form-item v-if="requiresConfirmation">
        <a-checkbox v-model:checked="form.coolingOffConfirmed">
          我了解此申請需要 24 小時冷靜期確認，確認後將無法撤銷
        </a-checkbox>
      </a-form-item>

      <a-form-item>
        <a-button
          type="primary"
          danger
          html-type="submit"
          :loading="loading"
          block
        >
          確認自我排除
        </a-button>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { requestSelfExclusion } from '@/api/player-protection';

const visible = ref(false);
const loading = ref(false);

const form = ref({
  duration: '',
  reason: '',
  coolingOffConfirmed: false,
});

const requiresConfirmation = computed(() => {
  return ['6M', '1Y', '5Y', 'PERMANENT'].includes(form.value.duration);
});

const handleSubmit = async () => {
  loading.value = true;
  try {
    const result = await requestSelfExclusion(form.value);
    if (result.status === 'PENDING_CONFIRMATION') {
      message.info('請在 24 小時後確認排除申請');
    } else {
      message.success('自我排除已生效');
      // 強制登出
      window.location.href = '/logout';
    }
  } catch (error) {
    message.error('申請失敗，請稍後重試');
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 監控與告警

### 關鍵指標

| 指標 | Prometheus 名稱 | 告警閾值 |
|------|----------------|---------|
| 自我排除申請數 | `rg_self_exclusion_requests_total` | 日環比 > 100% |
| 排除生效數 | `rg_self_exclusion_activated_total` | - |
| Gamstop 同步延遲 | `gamstop_sync_latency_seconds` | > 30s |
| Gamstop 同步失敗 | `gamstop_sync_failures_total` | > 0 |

### 審計日誌

所有排除操作必須記錄：
- 操作時間
- 玩家 ID
- 操作類型（申請/確認/解除）
- 操作來源（玩家/運營商/監管機構）
- IP 地址
- 設備資訊

---

## 測試場景

### 功能測試

| 場景 | 預期結果 |
|------|---------|
| 玩家申請 24 小時排除 | 立即生效，24 小時後自動解除 |
| 玩家申請 6 個月排除 | 顯示 24 小時確認視窗 |
| 排除期間嘗試登入 | 顯示排除訊息，禁止登入 |
| 永久排除後申請解除 | 拒絕，顯示永久排除不可解除 |
| Gamstop 同步失敗 | 記錄失敗，排程重試 |

### 整合測試

```java
@SpringBootTest
@Transactional
class SelfExclusionServiceTest {

    @Autowired
    private SelfExclusionService selfExclusionService;

    @Test
    void testShortTermExclusionImmediateEffect() {
        // Given
        Long playerId = 1001L;
        SelfExclusionForm form = new SelfExclusionForm();
        form.setDuration(DurationType.H24);

        // When
        ResponseDTO<ExclusionResultVO> result =
            selfExclusionService.requestSelfExclusion(playerId, form);

        // Then
        assertThat(result.getData().getStatus())
            .isEqualTo(ExclusionStatus.ACTIVE);
        assertThat(selfExclusionService.isExcluded(playerId)).isTrue();
    }

    @Test
    void testLongTermExclusionRequiresCoolingOff() {
        // Given
        Long playerId = 1002L;
        SelfExclusionForm form = new SelfExclusionForm();
        form.setDuration(DurationType.Y1);
        form.setCoolingOffConfirmed(false);

        // When
        ResponseDTO<ExclusionResultVO> result =
            selfExclusionService.requestSelfExclusion(playerId, form);

        // Then
        assertThat(result.getData().getStatus())
            .isEqualTo(ExclusionStatus.PENDING_CONFIRMATION);
    }
}
```

---

## 常見問題

### Q1: 排除期間可以提取餘額嗎？

**A**: 可以。根據大多數牌照要求，排除期間玩家仍可申請提款。系統應保留出金功能，但禁止存款和遊戲。

### Q2: 排除期間的未結投注如何處理？

**A**:
- 已開始的遊戲回合：等待結束後正常結算
- 未開始的預約投注：取消並退款
- 體育投注：依牌照規則，可能等待結果或提前結算

### Q3: 如何防止玩家建立新帳戶繞過排除？

**A**:
- 設備指紋識別
- 支付資訊比對
- Gamstop 跨平台同步 (UK)
- 人臉識別驗證（進階）

---

## 相關文檔

- [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) - 存款限額
- [15-03_Cooling_Off_Period.md](15-03_Cooling_Off_Period.md) - 冷靜期
- [06-08_UKGC_Compliance.md](../06_Platform_Governance/06-08_UKGC_Compliance.md) - UK 合規
- [05-03_KYC_AML.md](../05_Risk_Control/05-03_KYC_AML.md) - KYC/AML

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
