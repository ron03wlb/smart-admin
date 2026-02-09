# 15-04 Session Management (會話時間控制)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

會話時間控制 (Session Management) 是幫助玩家管理遊戲時長的保護工具，包括 Session 時間限制、強制休息和活動超時自動登出。

### 監管要求

| 監管機構 | 條款 | 要求 |
|---------|------|------|
| **UKGC** | LCCP SR 3.4.2 | 10 次存款/24h 後強制 60 分鐘休息 |
| **Sweden** | Gambling Act | 必須提供 Session 限制選項 |
| **Germany** | GlüStV 2021 | 強制 60 分鐘休息後暫停 |

---

## Session 限制類型

### 1. 時間限制 (Duration Limit)

玩家設定每次遊戲會話的最長時間：

| 選項 | 說明 | 到期動作 |
|------|------|---------|
| 15 分鐘 | 短暫遊戲 | 提示 + 可繼續 |
| 30 分鐘 | 標準時段 | 提示 + 可繼續 |
| 60 分鐘 | 較長時段 | 提示 + 可繼續 |
| 120 分鐘 | 延長時段 | 提示 + 可繼續 |
| 無限制 | 不限時 | 依現實檢查設定 |

### 2. 強制休息 (Mandatory Break)

根據監管要求，達到特定條件後強制休息：

**UK 規則**:
- 24 小時內存款 10 次以上 → 強制 60 分鐘休息
- 休息期間無法繼續遊戲

**Germany 規則**:
- 連續遊戲 60 分鐘 → 強制 5 分鐘休息
- 休息後才能繼續

### 3. 閒置超時 (Idle Timeout)

無活動自動登出：
- 預設：30 分鐘無操作
- 可配置：15-60 分鐘
- 遊戲進行中不計入閒置時間

---

## 業務流程

### Session 開始

```
玩家登入/開始遊戲 → 建立 Session → 啟動計時器
         │
         ├── 檢查 Session 限制設定
         ├── 檢查強制休息狀態
         └── 初始化活動追蹤
```

### Session 進行中

```
┌─────────────────────────────────────────────────────────────┐
│                     Session 監控                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │ Duration    │    │ Activity    │    │ Mandatory   │     │
│  │ Timer       │    │ Tracker     │    │ Break Check │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
│         │                  │                   │            │
│         ▼                  ▼                   ▼            │
│  ┌──────────────────────────────────────────────────┐      │
│  │            Session State Manager                  │      │
│  └─────────────────────┬────────────────────────────┘      │
│                        │                                    │
│         ┌──────────────┼──────────────┐                    │
│         ▼              ▼              ▼                    │
│    ┌─────────┐   ┌─────────┐   ┌─────────┐                │
│    │ Warning │   │ Timeout │   │ Force   │                │
│    │ Popup   │   │ Logout  │   │ Break   │                │
│    └─────────┘   └─────────┘   └─────────┘                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Session 結束

```
到達時間限制 / 閒置超時 / 玩家登出
         │
         ├── 保存會話統計
         ├── 記錄活動歷史
         └── 清理會話資源
```

---

## 技術實現

### 資料庫設計

```sql
-- Session 設定表
CREATE TABLE t_session_setting (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL UNIQUE,

    -- 時間限制
    session_duration_minutes    INT,          -- NULL = 無限制
    idle_timeout_minutes        INT DEFAULT 30,

    -- 現實檢查間隔（分鐘）
    reality_check_interval      INT DEFAULT 60,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Session 記錄表
CREATE TABLE t_session_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    session_token       VARCHAR(64) NOT NULL UNIQUE,

    -- 時間
    start_time          DATETIME NOT NULL,
    end_time            DATETIME,
    last_activity_time  DATETIME NOT NULL,

    -- 狀態
    status              VARCHAR(20) NOT NULL,  -- ACTIVE, ENDED, TIMEOUT, FORCED_BREAK
    end_reason          VARCHAR(50),           -- LOGOUT, DURATION_LIMIT, IDLE_TIMEOUT, FORCED_BREAK

    -- 統計
    total_duration_seconds  INT,
    game_time_seconds       INT,
    total_bets              INT DEFAULT 0,
    total_stake             DECIMAL(18,2) DEFAULT 0,
    net_result              DECIMAL(18,2) DEFAULT 0,

    -- 設備資訊
    device_type         VARCHAR(20),
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
);

-- 強制休息記錄表
CREATE TABLE t_mandatory_break_record (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id           BIGINT NOT NULL,
    trigger_reason      VARCHAR(50) NOT NULL,  -- DEPOSIT_COUNT, DURATION_LIMIT, REGULATOR
    break_duration_minutes  INT NOT NULL,
    start_time          DATETIME NOT NULL,
    end_time            DATETIME NOT NULL,
    status              VARCHAR(20) NOT NULL,  -- ACTIVE, COMPLETED

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player_id (player_id),
    INDEX idx_end_time (end_time)
);
```

### 核心服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final SessionRecordDao sessionRecordDao;
    private final SessionSettingDao sessionSettingDao;
    private final MandatoryBreakRecordDao mandatoryBreakDao;
    private final WebSocketSessionManager wsSessionManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String ACTIVITY_KEY_PREFIX = "session:activity:";

    /**
     * 開始會話
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<SessionStartResultVO> startSession(
            Long playerId,
            SessionStartForm form) {

        // 1. 檢查是否有強制休息
        Option<MandatoryBreakRecord> breakOpt = checkMandatoryBreak(playerId);
        if (breakOpt.isDefined()) {
            MandatoryBreakRecord breakRecord = breakOpt.get();
            long remainingMinutes = calculateRemainingMinutes(breakRecord.getEndTime());
            return ResponseDTO.error(UserErrorCode.MANDATORY_BREAK_ACTIVE,
                String.format("您需要休息 %d 分鐘後才能繼續遊戲", remainingMinutes));
        }

        // 2. 終止現有會話
        terminateExistingSessions(playerId);

        // 3. 獲取 Session 設定
        SessionSetting setting = sessionSettingDao.findByPlayerId(playerId)
            .getOrElse(SessionSetting::defaultSetting);

        // 4. 創建新會話
        String sessionToken = generateSessionToken();
        LocalDateTime now = LocalDateTime.now();

        SessionRecord session = SessionRecord.builder()
            .playerId(playerId)
            .sessionToken(sessionToken)
            .startTime(now)
            .lastActivityTime(now)
            .status(SessionStatus.ACTIVE)
            .deviceType(form.getDeviceType())
            .ipAddress(form.getIpAddress())
            .userAgent(form.getUserAgent())
            .build();
        sessionRecordDao.insert(session);

        // 5. 快取 Session 狀態
        cacheSessionState(playerId, session, setting);

        // 6. 安排提醒和超時任務
        scheduleSessionTasks(playerId, session, setting);

        log.info("Session started: playerId={}, sessionToken={}",
            playerId, sessionToken);

        return ResponseDTO.ok(SessionStartResultVO.builder()
            .sessionToken(sessionToken)
            .durationLimit(setting.getSessionDurationMinutes())
            .realityCheckInterval(setting.getRealityCheckInterval())
            .build());
    }

    /**
     * 更新活動時間
     */
    public void updateActivity(Long playerId, String sessionToken) {
        String activityKey = ACTIVITY_KEY_PREFIX + playerId;
        LocalDateTime now = LocalDateTime.now();

        // 更新 Redis 快取
        redisTemplate.opsForValue().set(activityKey, now,
            Duration.ofMinutes(60));

        // 異步更新資料庫
        CompletableFuture.runAsync(() -> {
            sessionRecordDao.updateLastActivityTime(sessionToken, now);
        });
    }

    /**
     * 檢查 Session 狀態
     */
    public SessionStatusVO checkSessionStatus(Long playerId) {
        String sessionKey = SESSION_KEY_PREFIX + playerId;
        SessionState state = (SessionState) redisTemplate.opsForValue().get(sessionKey);

        if (state == null) {
            return SessionStatusVO.noActiveSession();
        }

        LocalDateTime now = LocalDateTime.now();

        // 檢查時間限制
        if (state.getDurationLimit() != null) {
            long elapsedMinutes = Duration.between(state.getStartTime(), now).toMinutes();
            if (elapsedMinutes >= state.getDurationLimit()) {
                return SessionStatusVO.durationLimitReached(state.getDurationLimit());
            }
        }

        // 檢查閒置超時
        long idleMinutes = Duration.between(state.getLastActivityTime(), now).toMinutes();
        if (idleMinutes >= state.getIdleTimeout()) {
            return SessionStatusVO.idleTimeout(state.getIdleTimeout());
        }

        // 檢查現實檢查
        if (state.getRealityCheckInterval() != null) {
            long sinceLastCheck = Duration.between(
                state.getLastRealityCheck(), now).toMinutes();
            if (sinceLastCheck >= state.getRealityCheckInterval()) {
                return SessionStatusVO.realityCheckDue(buildRealityCheckData(playerId, state));
            }
        }

        return SessionStatusVO.active(state);
    }

    /**
     * 觸發強制休息
     */
    @Transactional(rollbackFor = Throwable.class)
    public void triggerMandatoryBreak(Long playerId, MandatoryBreakReason reason) {
        int breakDuration = getBreakDuration(reason);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(breakDuration);

        // 記錄強制休息
        MandatoryBreakRecord breakRecord = MandatoryBreakRecord.builder()
            .playerId(playerId)
            .triggerReason(reason.name())
            .breakDurationMinutes(breakDuration)
            .startTime(now)
            .endTime(endTime)
            .status(MandatoryBreakStatus.ACTIVE)
            .build();
        mandatoryBreakDao.insert(breakRecord);

        // 終止當前會話
        terminateSession(playerId, EndReason.FORCED_BREAK);

        // 通過 WebSocket 通知前端
        wsSessionManager.sendMessage(playerId,
            WebSocketMessage.mandatoryBreak(breakDuration));

        log.info("Mandatory break triggered: playerId={}, reason={}, duration={}min",
            playerId, reason, breakDuration);
    }

    /**
     * UK 規則：檢查 24 小時內存款次數
     */
    public void checkDepositCountTrigger(Long playerId) {
        int depositCount = depositService.getDepositCountLast24Hours(playerId);
        if (depositCount >= 10) {
            triggerMandatoryBreak(playerId, MandatoryBreakReason.DEPOSIT_COUNT);
        }
    }

    /**
     * 終止會話
     */
    @Transactional(rollbackFor = Throwable.class)
    public void terminateSession(Long playerId, EndReason reason) {
        Option<SessionRecord> sessionOpt =
            sessionRecordDao.findActiveByPlayerId(playerId);

        if (sessionOpt.isEmpty()) {
            return;
        }

        SessionRecord session = sessionOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 計算統計
        long totalDuration = Duration.between(session.getStartTime(), now).getSeconds();

        session.setEndTime(now);
        session.setStatus(SessionStatus.ENDED);
        session.setEndReason(reason.name());
        session.setTotalDurationSeconds((int) totalDuration);
        sessionRecordDao.updateById(session);

        // 清理快取
        String sessionKey = SESSION_KEY_PREFIX + playerId;
        String activityKey = ACTIVITY_KEY_PREFIX + playerId;
        redisTemplate.delete(sessionKey);
        redisTemplate.delete(activityKey);

        // 通知 WebSocket 斷開
        wsSessionManager.closeSession(playerId);

        log.info("Session terminated: playerId={}, reason={}, duration={}s",
            playerId, reason, totalDuration);
    }

    /**
     * 設定 Session 時間限制
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> setSessionDurationLimit(Long playerId, Integer minutes) {
        SessionSetting setting = sessionSettingDao.findByPlayerId(playerId)
            .getOrElse(() -> {
                SessionSetting newSetting = new SessionSetting();
                newSetting.setPlayerId(playerId);
                return newSetting;
            });

        setting.setSessionDurationMinutes(minutes);
        sessionSettingDao.saveOrUpdate(setting);

        log.info("Session duration limit updated: playerId={}, limit={}min",
            playerId, minutes);

        return ResponseDTO.ok();
    }

    private int getBreakDuration(MandatoryBreakReason reason) {
        return switch (reason) {
            case DEPOSIT_COUNT -> 60;     // UK: 60 分鐘
            case DURATION_LIMIT -> 5;      // DE: 5 分鐘
            case REGULATOR -> 60;          // 預設 60 分鐘
        };
    }
}
```

### WebSocket 整合

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionWebSocketHandler implements WebSocketHandler {

    private final SessionManagementService sessionService;
    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long playerId = extractPlayerId(session);
        activeSessions.put(playerId, session);

        // 啟動心跳監測
        startHeartbeatMonitor(playerId, session);
    }

    /**
     * 處理心跳訊息
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        Long playerId = extractPlayerId(session);

        if (message instanceof PingMessage) {
            // 更新活動時間
            sessionService.updateActivity(playerId, session.getId());

            // 檢查 Session 狀態
            SessionStatusVO status = sessionService.checkSessionStatus(playerId);

            switch (status.getType()) {
                case REALITY_CHECK_DUE -> {
                    sendMessage(session, new RealityCheckMessage(status.getRealityCheckData()));
                }
                case DURATION_LIMIT_REACHED -> {
                    sendMessage(session, new SessionLimitMessage("已達到您設定的遊戲時間限制"));
                }
                case IDLE_TIMEOUT -> {
                    sendMessage(session, new SessionTimeoutMessage("由於閒置過久，會話已結束"));
                    sessionService.terminateSession(playerId, EndReason.IDLE_TIMEOUT);
                }
            }
        }
    }

    /**
     * 發送強制休息通知
     */
    public void sendMandatoryBreakNotification(Long playerId, int durationMinutes) {
        WebSocketSession session = activeSessions.get(playerId);
        if (session != null && session.isOpen()) {
            MandatoryBreakMessage message = new MandatoryBreakMessage(
                durationMinutes,
                "根據監管要求，您需要休息 " + durationMinutes + " 分鐘後才能繼續遊戲"
            );
            sendMessage(session, message);

            // 關閉連線
            try {
                session.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                log.error("Failed to close WebSocket session", e);
            }
        }
    }
}
```

---

## 前端整合

### Session 狀態監控

```vue
<template>
  <div class="session-monitor">
    <!-- Session 時間顯示 -->
    <div class="session-timer" v-if="sessionActive">
      <ClockCircleOutlined />
      <span>遊戲時間: {{ formatDuration(elapsedTime) }}</span>
      <span v-if="durationLimit">/ {{ formatDuration(durationLimit * 60) }}</span>
    </div>

    <!-- 現實檢查彈窗 -->
    <a-modal
      v-model:visible="realityCheckVisible"
      title="遊戲時間提醒"
      :closable="false"
      :maskClosable="false"
    >
      <div class="reality-check-content">
        <p>您已遊戲 <strong>{{ formatDuration(elapsedTime) }}</strong></p>
        <p>本次會話淨輸贏: <strong :class="netResultClass">{{ formatMoney(netResult) }}</strong></p>
        <p>今日總存款: <strong>{{ formatMoney(todayDeposit) }}</strong></p>
      </div>

      <template #footer>
        <a-button @click="handleContinue">繼續遊戲</a-button>
        <a-button type="primary" @click="handleStopPlaying">
          結束遊戲
        </a-button>
      </template>
    </a-modal>

    <!-- 強制休息彈窗 -->
    <a-modal
      v-model:visible="mandatoryBreakVisible"
      title="強制休息"
      :closable="false"
      :maskClosable="false"
      :footer="null"
    >
      <a-result
        status="info"
        title="您需要休息一下"
        :sub-title="`根據監管要求，請休息 ${breakRemainingMinutes} 分鐘後再繼續遊戲`"
      >
        <template #extra>
          <a-button type="primary" @click="goToHome">
            返回首頁
          </a-button>
        </template>
      </a-result>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useWebSocket } from '@/composables/useWebSocket';

const { connect, onMessage, sendPing } = useWebSocket();

const sessionActive = ref(true);
const elapsedTime = ref(0);
const durationLimit = ref<number | null>(null);
const realityCheckVisible = ref(false);
const mandatoryBreakVisible = ref(false);
const breakRemainingMinutes = ref(0);
const netResult = ref(0);
const todayDeposit = ref(0);

let heartbeatInterval: number;
let elapsedInterval: number;

onMounted(() => {
  connect();

  // 定時發送心跳
  heartbeatInterval = setInterval(() => {
    sendPing();
  }, 10000); // 每 10 秒

  // 更新經過時間
  elapsedInterval = setInterval(() => {
    elapsedTime.value += 1;
  }, 1000);

  // 監聽 WebSocket 訊息
  onMessage((message) => {
    switch (message.type) {
      case 'REALITY_CHECK':
        netResult.value = message.data.netResult;
        todayDeposit.value = message.data.todayDeposit;
        realityCheckVisible.value = true;
        break;

      case 'MANDATORY_BREAK':
        breakRemainingMinutes.value = message.data.durationMinutes;
        mandatoryBreakVisible.value = true;
        break;

      case 'SESSION_TIMEOUT':
        handleSessionEnd();
        break;
    }
  });
});

onUnmounted(() => {
  clearInterval(heartbeatInterval);
  clearInterval(elapsedInterval);
});

const handleContinue = () => {
  realityCheckVisible.value = false;
  // 重置現實檢查計時器（後端處理）
};

const handleStopPlaying = () => {
  // 結束遊戲，登出
  window.location.href = '/logout';
};
</script>
```

---

## 監控與告警

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 平均 Session 時長 | `session_duration_seconds_avg` | 所有 Session 平均 |
| Session 超時率 | `session_timeout_rate` | 閒置超時佔比 |
| 強制休息觸發數 | `mandatory_break_total` | 按原因分類 |
| 現實檢查繼續率 | `reality_check_continue_rate` | 選擇繼續的比例 |

---

## 相關文檔

- [15-05_Reality_Checks.md](15-05_Reality_Checks.md) - 現實檢查
- [15-03_Cooling_Off_Period.md](15-03_Cooling_Off_Period.md) - 冷靜期
- [09-11_OAuth_Refresh_Token_Implementation.md](../09_Technical_Infrastructure/09-11_OAuth_Refresh_Token_Implementation.md) - Token 管理

---

**返回**: [負責任博彩模塊](README.md) | [iGaming 首頁](../README.md)
