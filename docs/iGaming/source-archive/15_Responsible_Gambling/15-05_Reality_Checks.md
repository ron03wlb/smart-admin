# 15-05 Reality Checks (現實檢查)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

現實檢查 (Reality Checks) 是定時彈窗提醒玩家其博彩活動狀態的工具，幫助玩家意識到已花費的時間和金錢。

### 監管要求

| 監管機構 | 條款 | 預設間隔 | 顯示內容 |
|---------|------|---------|---------|
| **UKGC** | LCCP SR 3.4.2 | 玩家可選 | 時間、盈虧、選項 |
| **Sweden** | Gambling Act | 60 分鐘 | 時間、盈虧 |
| **Germany** | GlüStV 2021 | 60 分鐘 | 強制顯示 |

---

## 現實檢查配置

### 間隔選項

| 選項 | 適用場景 | 說明 |
|------|---------|------|
| 15 分鐘 | 高風險玩家 | 頻繁提醒 |
| 30 分鐘 | 建議設定 | 適度提醒 |
| 60 分鐘 | 預設設定 | 標準間隔 |
| 不設定 | 玩家選擇 | 部分牌照不允許 |

### 顯示內容

**必須顯示**:
1. 本次會話遊戲時間
2. 本次會話淨盈虧（清晰顯示虧損為負數）

**建議顯示**:
3. 今日總存款金額
4. 今日總投注金額
5. 帳戶餘額

**操作選項**:
- 繼續遊戲
- 結束遊戲（登出）
- 設定存款限額（快捷入口）
- 查看帳戶歷史

---

## 業務流程

### 現實檢查觸發流程

```
會話開始 → 計時器啟動 → 達到設定間隔 → 彈窗顯示
    │                                      │
    │                                      ├── 顯示遊戲統計
    │                                      ├── 顯示盈虧狀態
    │                                      └── 等待玩家響應
    │                                                │
    │          ┌─────────────────────────────────────┤
    │          │                                     │
    │          ▼                                     ▼
    │   ┌──────────────┐                    ┌──────────────┐
    │   │  繼續遊戲    │                    │  結束遊戲    │
    │   │ (重置計時器) │                    │  (登出)      │
    │   └──────┬───────┘                    └──────────────┘
    │          │
    └──────────┘
```

### 彈窗阻斷規則

**遊戲回合進行中**:
- 不立即彈出，等待回合結束
- 回合結束後立即彈出
- 保證遊戲完整性

**自動遊戲模式**:
- 暫停自動遊戲
- 彈出現實檢查
- 玩家確認後可恢復

---

## 技術實現

### 資料庫設計

```sql
-- 現實檢查設定（與 Session 設定合併）
ALTER TABLE t_session_setting
ADD COLUMN reality_check_interval INT DEFAULT 60;  -- 分鐘，NULL = 不設定

-- 現實檢查記錄
CREATE TABLE t_reality_check_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    session_id          BIGINT NOT NULL,
    check_time          DATETIME NOT NULL,

    -- 顯示的數據
    session_duration_minutes    INT NOT NULL,
    net_result                  DECIMAL(18,2) NOT NULL,
    total_stake                 DECIMAL(18,2) NOT NULL,
    today_deposit               DECIMAL(18,2),
    account_balance             DECIMAL(18,2),

    -- 玩家響應
    response                    VARCHAR(20),  -- CONTINUE, STOP, TIMEOUT
    response_time               DATETIME,
    response_duration_seconds   INT,          -- 思考時間

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_session_id (session_id),
    INDEX idx_check_time (check_time)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RealityCheckService {

    private final RealityCheckRecordDao realityCheckRecordDao;
    private final SessionRecordDao sessionRecordDao;
    private final WalletService walletService;
    private final BetHistoryService betHistoryService;
    private final DepositService depositService;

    /**
     * 生成現實檢查數據
     */
    public RealityCheckDataVO generateRealityCheckData(Long playerId, Long sessionId) {
        SessionRecord session = sessionRecordDao.selectById(sessionId);
        LocalDateTime now = LocalDateTime.now();

        // 計算會話時長
        long sessionMinutes = Duration.between(session.getStartTime(), now).toMinutes();

        // 計算本會話盈虧
        BigDecimal netResult = betHistoryService.calculateSessionNetResult(
            playerId, session.getStartTime(), now);

        // 本會話總投注
        BigDecimal totalStake = betHistoryService.calculateSessionTotalStake(
            playerId, session.getStartTime(), now);

        // 今日存款
        BigDecimal todayDeposit = depositService.getTodayDepositTotal(playerId);

        // 當前餘額
        BigDecimal balance = walletService.getBalance(playerId);

        return RealityCheckDataVO.builder()
            .sessionDurationMinutes((int) sessionMinutes)
            .netResult(netResult)
            .totalStake(totalStake)
            .todayDeposit(todayDeposit)
            .accountBalance(balance)
            .checkTime(now)
            .build();
    }

    /**
     * 記錄現實檢查
     */
    @Transactional(rollbackFor = Throwable.class)
    public void recordRealityCheck(Long playerId, Long sessionId, RealityCheckDataVO data) {
        RealityCheckRecord record = RealityCheckRecord.builder()
            .playerId(playerId)
            .sessionId(sessionId)
            .checkTime(data.getCheckTime())
            .sessionDurationMinutes(data.getSessionDurationMinutes())
            .netResult(data.getNetResult())
            .totalStake(data.getTotalStake())
            .todayDeposit(data.getTodayDeposit())
            .accountBalance(data.getAccountBalance())
            .build();

        realityCheckRecordDao.insert(record);

        log.info("Reality check recorded: playerId={}, sessionId={}, netResult={}",
            playerId, sessionId, data.getNetResult());
    }

    /**
     * 記錄玩家響應
     */
    @Transactional(rollbackFor = Throwable.class)
    public void recordResponse(Long recordId, RealityCheckResponse response) {
        RealityCheckRecord record = realityCheckRecordDao.selectById(recordId);
        LocalDateTime now = LocalDateTime.now();

        record.setResponse(response.name());
        record.setResponseTime(now);
        record.setResponseDurationSeconds(
            (int) Duration.between(record.getCheckTime(), now).getSeconds()
        );

        realityCheckRecordDao.updateById(record);

        // 分析玩家行為
        analyzePlayerBehavior(record);
    }

    /**
     * 分析玩家行為（用於風險識別）
     */
    private void analyzePlayerBehavior(RealityCheckRecord record) {
        // 如果玩家總是立即點擊繼續（<5 秒），可能需要關注
        if (record.getResponseDurationSeconds() < 5 &&
            record.getResponse().equals("CONTINUE")) {

            // 檢查歷史模式
            int fastContinueCount = realityCheckRecordDao.countFastContinue(
                record.getPlayerId(),
                LocalDateTime.now().minusDays(7),
                5
            );

            if (fastContinueCount >= 10) {
                // 標記為潛在風險玩家
                riskService.flagPotentialProblemGambler(record.getPlayerId(),
                    "快速跳過現實檢查次數過多");
            }
        }
    }

    /**
     * 設定現實檢查間隔
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> setRealityCheckInterval(Long playerId, Integer minutes) {
        // 驗證間隔（依牌照可能有最小值限制）
        if (minutes != null && minutes < 15) {
            return ResponseDTO.error(UserErrorCode.INVALID_INTERVAL,
                "現實檢查間隔不能少於 15 分鐘");
        }

        SessionSetting setting = sessionSettingDao.findByPlayerId(playerId)
            .getOrElse(() -> {
                SessionSetting newSetting = new SessionSetting();
                newSetting.setPlayerId(playerId);
                return newSetting;
            });

        setting.setRealityCheckInterval(minutes);
        sessionSettingDao.saveOrUpdate(setting);

        log.info("Reality check interval updated: playerId={}, interval={}min",
            playerId, minutes);

        return ResponseDTO.ok();
    }
}
```

### 前端彈窗實現

```vue
<template>
  <a-modal
    v-model:visible="visible"
    title="遊戲時間提醒"
    :closable="false"
    :maskClosable="false"
    :keyboard="false"
    width="450px"
    centered
  >
    <div class="reality-check-content">
      <!-- 遊戲時間 -->
      <div class="stat-item">
        <ClockCircleOutlined class="icon" />
        <div class="stat-content">
          <span class="label">本次遊戲時間</span>
          <span class="value">{{ formatDuration(data.sessionDurationMinutes) }}</span>
        </div>
      </div>

      <!-- 盈虧狀態 -->
      <div class="stat-item" :class="netResultClass">
        <DollarOutlined class="icon" />
        <div class="stat-content">
          <span class="label">本次會話盈虧</span>
          <span class="value">
            {{ data.netResult >= 0 ? '+' : '' }}{{ formatMoney(data.netResult) }}
          </span>
        </div>
      </div>

      <!-- 總投注 -->
      <div class="stat-item">
        <ThunderboltOutlined class="icon" />
        <div class="stat-content">
          <span class="label">本次總投注</span>
          <span class="value">{{ formatMoney(data.totalStake) }}</span>
        </div>
      </div>

      <!-- 今日存款 -->
      <div class="stat-item">
        <WalletOutlined class="icon" />
        <div class="stat-content">
          <span class="label">今日存款</span>
          <span class="value">{{ formatMoney(data.todayDeposit) }}</span>
        </div>
      </div>

      <!-- 帳戶餘額 -->
      <div class="stat-item highlight">
        <BankOutlined class="icon" />
        <div class="stat-content">
          <span class="label">帳戶餘額</span>
          <span class="value">{{ formatMoney(data.accountBalance) }}</span>
        </div>
      </div>

      <!-- 負責任博彩提示 -->
      <a-divider />
      <p class="rg-message">
        請負責任地進行遊戲。如果您感到困擾，請考慮休息或尋求幫助。
      </p>
    </div>

    <template #footer>
      <div class="button-group">
        <a-button
          type="default"
          @click="handleSetLimit"
        >
          設定限額
        </a-button>
        <a-button
          type="default"
          danger
          @click="handleStop"
        >
          結束遊戲
        </a-button>
        <a-button
          type="primary"
          @click="handleContinue"
          :loading="loading"
        >
          繼續遊戲
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  ClockCircleOutlined,
  DollarOutlined,
  ThunderboltOutlined,
  WalletOutlined,
  BankOutlined,
} from '@ant-design/icons-vue';
import { recordRealityCheckResponse } from '@/api/player-protection';

interface RealityCheckData {
  sessionDurationMinutes: number;
  netResult: number;
  totalStake: number;
  todayDeposit: number;
  accountBalance: number;
  recordId: number;
}

const props = defineProps<{
  visible: boolean;
  data: RealityCheckData;
}>();

const emit = defineEmits(['continue', 'stop', 'setLimit']);

const loading = ref(false);

const netResultClass = computed(() => ({
  'positive': props.data.netResult >= 0,
  'negative': props.data.netResult < 0,
}));

const handleContinue = async () => {
  loading.value = true;
  try {
    await recordRealityCheckResponse(props.data.recordId, 'CONTINUE');
    emit('continue');
  } finally {
    loading.value = false;
  }
};

const handleStop = async () => {
  await recordRealityCheckResponse(props.data.recordId, 'STOP');
  emit('stop');
};

const handleSetLimit = () => {
  emit('setLimit');
};

const formatDuration = (minutes: number) => {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours > 0) {
    return `${hours} 小時 ${mins} 分鐘`;
  }
  return `${mins} 分鐘`;
};

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};
</script>

<style scoped lang="scss">
.reality-check-content {
  .stat-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-of-type {
      border-bottom: none;
    }

    &.positive {
      color: #52c41a;
    }

    &.negative {
      color: #ff4d4f;
    }

    &.highlight {
      background: #fafafa;
      margin: 0 -24px;
      padding: 12px 24px;
    }

    .icon {
      font-size: 24px;
      margin-right: 16px;
      color: #1890ff;
    }

    .stat-content {
      flex: 1;
      display: flex;
      justify-content: space-between;

      .label {
        color: #666;
      }

      .value {
        font-weight: 600;
        font-size: 16px;
      }
    }
  }

  .rg-message {
    color: #999;
    font-size: 12px;
    text-align: center;
    margin: 0;
  }
}

.button-group {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
</style>
```

---

## 遊戲回合處理

### 等待回合結束

```java
@Service
@RequiredArgsConstructor
public class GameRoundAwareRealityCheck {

    private final GameSessionService gameSessionService;

    /**
     * 檢查是否可以顯示現實檢查
     */
    public boolean canShowRealityCheck(Long playerId) {
        // 檢查是否有進行中的遊戲回合
        Option<GameRound> activeRoundOpt = gameSessionService.getActiveRound(playerId);

        if (activeRoundOpt.isEmpty()) {
            return true; // 沒有進行中的回合，可以顯示
        }

        GameRound round = activeRoundOpt.get();

        // 判斷遊戲類型
        return switch (round.getGameType()) {
            case SLOTS -> round.isSpinComplete();
            case BLACKJACK -> !round.isPlayerTurn();
            case ROULETTE -> round.isBettingClosed() && round.isResultDetermined();
            case BACCARAT -> round.isResultDetermined();
            case LIVE_CASINO -> !round.isActive();
            default -> true;
        };
    }

    /**
     * 等待回合結束後觸發
     */
    public void scheduleRealityCheckAfterRound(Long playerId, Long roundId) {
        // 監聽回合結束事件
        gameEventListener.onRoundComplete(roundId, () -> {
            realityCheckService.triggerRealityCheck(playerId);
        });
    }
}
```

---

## 監控與分析

### 關鍵指標

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 現實檢查觸發數 | `reality_check_triggered_total` | 總觸發次數 |
| 繼續遊戲率 | `reality_check_continue_rate` | 選擇繼續的比例 |
| 平均響應時間 | `reality_check_response_seconds_avg` | 玩家思考時間 |
| 快速跳過率 | `reality_check_fast_skip_rate` | <5秒響應的比例 |

### 風險識別規則

| 行為模式 | 風險等級 | 建議動作 |
|---------|---------|---------|
| 連續 10 次 <5秒繼續 | 中 | 增加現實檢查頻率 |
| 虧損 >$1000 仍繼續 | 中 | 發送關懷訊息 |
| 遊戲時間 >4 小時 | 高 | 建議休息 |
| 存款後立即虧損 >50% | 高 | 觸發可負擔性評估 |

---

## 相關文檔

- [15-04_Session_Management.md](15-04_Session_Management.md) - 會話管理
- [15-06_Loss_Limits.md](15-06_Loss_Limits.md) - 虧損限額
- [15-08_Affordability_Assessment.md](15-08_Affordability_Assessment.md) - 可負擔性評估

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
