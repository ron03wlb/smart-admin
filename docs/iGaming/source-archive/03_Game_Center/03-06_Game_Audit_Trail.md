# 03-06 Game Audit Trail (遊戲審計追蹤)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

遊戲審計追蹤 (Game Audit Trail) 是記錄所有遊戲活動的不可篡改日誌系統，用於監管合規、爭議解決和遊戲公平性驗證。

### 監管要求

| 監管機構 | 保留期限 | 關鍵要求 |
|---------|---------|---------|
| **UKGC** | 5 年 | 完整遊戲歷史、可重播 |
| **MGA** | 10 年 | 時間戳、不可篡改 |
| **PAGCOR** | 5 年 | 玩家可查詢 |
| **GLI-19** | 依牌照 | 實時記錄、安全存儲 |

---

## 審計日誌內容

### 必須記錄的資訊

| 類別 | 欄位 | 說明 |
|------|------|------|
| **識別** | gameSessionId | 遊戲會話唯一識別碼 |
| | roundId | 遊戲回合唯一識別碼 |
| | playerId | 玩家識別碼 |
| | gameId | 遊戲識別碼 |
| **時間** | startTime | 回合開始時間（UTC） |
| | endTime | 回合結束時間（UTC） |
| **金額** | stakeAmount | 投注金額 |
| | winAmount | 獎金金額 |
| | currency | 貨幣代碼 |
| **遊戲狀態** | gameType | 遊戲類型 |
| | gameResult | 遊戲結果 |
| | rngSeed | RNG 種子（加密） |
| | rngOutput | RNG 輸出值 |
| **環境** | serverInstanceId | 伺服器實例 |
| | clientIp | 客戶端 IP |
| | deviceType | 設備類型 |

### 遊戲類型特定資料

#### Slots

```json
{
  "gameSpecificData": {
    "reels": [[1,2,3], [4,5,6], [7,8,9], [10,11,12], [13,14,15]],
    "paylines": [1, 3, 5, 7, 9],
    "winningLines": [
      {"line": 3, "symbol": "WILD", "count": 4, "payout": 200}
    ],
    "freeSpinsTriggered": true,
    "freeSpinsRemaining": 10,
    "multiplier": 2
  }
}
```

#### Blackjack

```json
{
  "gameSpecificData": {
    "dealerCards": ["AS", "7H"],
    "playerHands": [
      {"cards": ["KD", "QC"], "value": 20, "action": "STAND"}
    ],
    "dealerFinalValue": 18,
    "playerFinalValue": 20,
    "outcome": "PLAYER_WIN",
    "blackjack": false,
    "insurance": false,
    "split": false,
    "doubleDown": false
  }
}
```

#### Roulette

```json
{
  "gameSpecificData": {
    "wheelType": "EUROPEAN",
    "spinResult": 17,
    "resultColor": "BLACK",
    "bets": [
      {"type": "STRAIGHT", "numbers": [17], "amount": 10, "payout": 350},
      {"type": "RED", "amount": 50, "payout": 0}
    ],
    "totalBet": 60,
    "totalWin": 350
  }
}
```

---

## 技術實現

### 資料庫設計

```sql
-- 遊戲回合審計日誌（主表）
CREATE TABLE t_game_round_audit (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    round_id            VARCHAR(64) NOT NULL UNIQUE,
    game_session_id     VARCHAR(64) NOT NULL,
    player_id           BIGINT NOT NULL,
    game_id             VARCHAR(50) NOT NULL,
    game_type           VARCHAR(30) NOT NULL,

    -- 時間
    start_time          DATETIME(3) NOT NULL,  -- 毫秒精度
    end_time            DATETIME(3),

    -- 金額
    stake_amount        DECIMAL(18,4) NOT NULL,
    win_amount          DECIMAL(18,4) NOT NULL DEFAULT 0,
    currency            VARCHAR(3) NOT NULL,

    -- 遊戲數據
    game_result         VARCHAR(50),
    game_specific_data  JSON,

    -- RNG 數據（加密存儲）
    rng_seed_encrypted  VARBINARY(256),
    rng_output_hash     VARCHAR(64),  -- SHA-256 hash

    -- 環境
    server_instance_id  VARCHAR(50),
    client_ip           VARCHAR(45),
    device_type         VARCHAR(20),
    user_agent          VARCHAR(500),

    -- 完整性
    record_hash         VARCHAR(64) NOT NULL,  -- 記錄 hash
    previous_hash       VARCHAR(64),            -- 鏈接前一筆記錄
    signature           VARBINARY(512),         -- 數位簽章

    -- 審計
    created_at          DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_player_id (player_id),
    INDEX idx_game_session_id (game_session_id),
    INDEX idx_start_time (start_time),
    INDEX idx_game_id (game_id)
) ENGINE=InnoDB;

-- 遊戲動作審計日誌（詳細）
CREATE TABLE t_game_action_audit (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    round_id            VARCHAR(64) NOT NULL,
    action_sequence     INT NOT NULL,
    action_type         VARCHAR(30) NOT NULL,  -- BET, HIT, STAND, SPIN, etc.
    action_data         JSON,
    action_time         DATETIME(3) NOT NULL,
    balance_before      DECIMAL(18,4),
    balance_after       DECIMAL(18,4),

    INDEX idx_round_id (round_id),
    INDEX idx_action_time (action_time)
) ENGINE=InnoDB;

-- 審計日誌歸檔表（長期存儲）
CREATE TABLE t_game_round_audit_archive (
    -- 與主表結構相同
    -- 分區按月
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(start_time) * 100 + MONTH(start_time)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    -- ...
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### 審計服務實現

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class GameAuditService {

    private final GameRoundAuditDao auditDao;
    private final GameActionAuditDao actionAuditDao;
    private final EncryptionService encryptionService;
    private final SignatureService signatureService;

    // 用於 hash 鏈
    private volatile String lastRecordHash = "";
    private final Object hashLock = new Object();

    /**
     * 記錄遊戲回合
     */
    @Transactional(rollbackFor = Throwable.class)
    public void recordGameRound(GameRoundEvent event) {
        // 1. 加密 RNG 種子
        byte[] encryptedSeed = encryptionService.encrypt(event.getRngSeed());

        // 2. 計算 RNG 輸出 hash
        String rngOutputHash = calculateHash(event.getRngOutput());

        // 3. 構建審計記錄
        GameRoundAudit audit = GameRoundAudit.builder()
            .roundId(event.getRoundId())
            .gameSessionId(event.getGameSessionId())
            .playerId(event.getPlayerId())
            .gameId(event.getGameId())
            .gameType(event.getGameType())
            .startTime(event.getStartTime())
            .endTime(event.getEndTime())
            .stakeAmount(event.getStakeAmount())
            .winAmount(event.getWinAmount())
            .currency(event.getCurrency())
            .gameResult(event.getGameResult())
            .gameSpecificData(JsonUtil.toJson(event.getGameSpecificData()))
            .rngSeedEncrypted(encryptedSeed)
            .rngOutputHash(rngOutputHash)
            .serverInstanceId(getServerInstanceId())
            .clientIp(event.getClientIp())
            .deviceType(event.getDeviceType())
            .userAgent(event.getUserAgent())
            .build();

        // 4. 計算記錄 hash（包含所有關鍵欄位）
        String recordHash = calculateRecordHash(audit);
        audit.setRecordHash(recordHash);

        // 5. 鏈接前一筆記錄 hash（區塊鏈式完整性）
        synchronized (hashLock) {
            audit.setPreviousHash(lastRecordHash);
            lastRecordHash = recordHash;
        }

        // 6. 數位簽章
        byte[] signature = signatureService.sign(recordHash);
        audit.setSignature(signature);

        // 7. 保存
        auditDao.insert(audit);

        log.debug("Game round recorded: roundId={}, hash={}",
            event.getRoundId(), recordHash.substring(0, 16));
    }

    /**
     * 記錄遊戲動作
     */
    public void recordGameAction(GameActionEvent event) {
        GameActionAudit action = GameActionAudit.builder()
            .roundId(event.getRoundId())
            .actionSequence(event.getActionSequence())
            .actionType(event.getActionType())
            .actionData(JsonUtil.toJson(event.getActionData()))
            .actionTime(LocalDateTime.now())
            .balanceBefore(event.getBalanceBefore())
            .balanceAfter(event.getBalanceAfter())
            .build();

        actionAuditDao.insert(action);
    }

    /**
     * 驗證審計記錄完整性
     */
    public IntegrityVerificationResult verifyIntegrity(String roundId) {
        GameRoundAudit audit = auditDao.findByRoundId(roundId);
        if (audit == null) {
            return IntegrityVerificationResult.notFound(roundId);
        }

        List<String> issues = new ArrayList<>();

        // 1. 驗證記錄 hash
        String recalculatedHash = calculateRecordHash(audit);
        if (!recalculatedHash.equals(audit.getRecordHash())) {
            issues.add("Record hash mismatch");
        }

        // 2. 驗證簽章
        boolean signatureValid = signatureService.verify(
            audit.getRecordHash(),
            audit.getSignature()
        );
        if (!signatureValid) {
            issues.add("Invalid signature");
        }

        // 3. 驗證 hash 鏈
        GameRoundAudit previousAudit = auditDao.findPreviousRecord(audit.getId());
        if (previousAudit != null &&
            !previousAudit.getRecordHash().equals(audit.getPreviousHash())) {
            issues.add("Hash chain broken");
        }

        return IntegrityVerificationResult.builder()
            .roundId(roundId)
            .verified(issues.isEmpty())
            .issues(issues)
            .verifiedAt(LocalDateTime.now())
            .build();
    }

    /**
     * 重播遊戲回合（用於爭議解決）
     */
    public GameReplayResult replayGameRound(String roundId) {
        GameRoundAudit audit = auditDao.findByRoundId(roundId);
        if (audit == null) {
            throw new GameNotFoundException(roundId);
        }

        // 解密 RNG 種子
        byte[] rngSeed = encryptionService.decrypt(audit.getRngSeedEncrypted());

        // 獲取遊戲動作
        List<GameActionAudit> actions = actionAuditDao.findByRoundId(roundId);

        // 使用相同種子和動作重播遊戲
        GameReplayEngine engine = gameEngineFactory.createReplayEngine(audit.getGameType());
        GameReplayResult result = engine.replay(rngSeed, actions);

        // 驗證結果一致
        if (!result.getGameResult().equals(audit.getGameResult()) ||
            result.getWinAmount().compareTo(audit.getWinAmount()) != 0) {
            log.error("Game replay mismatch: roundId={}", roundId);
            throw new GameReplayMismatchException(roundId);
        }

        return result;
    }

    /**
     * 查詢玩家遊戲歷史
     */
    public PageResult<GameHistoryVO> getPlayerGameHistory(
            Long playerId,
            GameHistoryQueryForm form) {

        Page<GameRoundAudit> page = SmartPageUtil.convert2PageQuery(form);
        IPage<GameRoundAudit> result = auditDao.selectPage(page,
            new LambdaQueryWrapper<GameRoundAudit>()
                .eq(GameRoundAudit::getPlayerId, playerId)
                .ge(form.getStartDate() != null, GameRoundAudit::getStartTime, form.getStartDate())
                .le(form.getEndDate() != null, GameRoundAudit::getStartTime, form.getEndDate())
                .eq(StringUtils.hasText(form.getGameId()), GameRoundAudit::getGameId, form.getGameId())
                .orderByDesc(GameRoundAudit::getStartTime)
        );

        List<GameHistoryVO> records = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        return PageResult.build(records, result.getTotal(), form);
    }

    private String calculateRecordHash(GameRoundAudit audit) {
        String content = String.join("|",
            audit.getRoundId(),
            audit.getGameSessionId(),
            String.valueOf(audit.getPlayerId()),
            audit.getGameId(),
            audit.getStartTime().toString(),
            audit.getStakeAmount().toString(),
            audit.getWinAmount().toString(),
            audit.getRngOutputHash(),
            audit.getGameSpecificData()
        );
        return DigestUtils.sha256Hex(content);
    }
}
```

### 審計日誌歸檔

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditArchiveService {

    private final GameRoundAuditDao auditDao;
    private final GameRoundAuditArchiveDao archiveDao;
    private final CloudStorageClient cloudStorage;

    /**
     * 定時歸檔舊記錄
     * 保留最近 90 天在主表，其餘歸檔
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 點
    public void archiveOldRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);

        // 批量歸檔
        int archived = 0;
        int batchSize = 10000;

        while (true) {
            List<GameRoundAudit> records = auditDao.findOldRecords(cutoffTime, batchSize);
            if (records.isEmpty()) {
                break;
            }

            // 轉移到歸檔表
            archiveDao.batchInsert(records);

            // 從主表刪除
            List<Long> ids = records.stream()
                .map(GameRoundAudit::getId)
                .collect(Collectors.toList());
            auditDao.deleteBatchIds(ids);

            archived += records.size();
            log.info("Archived {} records, total: {}", records.size(), archived);
        }

        log.info("Archive completed, total records: {}", archived);
    }

    /**
     * 導出審計記錄到雲存儲（長期保存）
     */
    @Scheduled(cron = "0 0 3 1 * ?") // 每月 1 日凌晨 3 點
    public void exportToCloudStorage() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String filename = String.format("game_audit_%d_%02d.parquet",
            lastMonth.getYear(), lastMonth.getMonthValue());

        // 導出為 Parquet 格式（壓縮 + 高效查詢）
        Path tempFile = exportToParquet(lastMonth);

        // 上傳到雲存儲
        cloudStorage.upload(
            "audit-archive/" + filename,
            tempFile,
            Map.of("retention", "10-years")
        );

        log.info("Exported audit records to cloud: {}", filename);
    }
}
```

---

## 前端整合

### 玩家遊戲歷史

```vue
<template>
  <a-card title="遊戲歷史">
    <a-table
      :columns="columns"
      :data-source="history"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
    >
      <template #gameType="{ record }">
        <a-tag :color="getGameTypeColor(record.gameType)">
          {{ record.gameType }}
        </a-tag>
      </template>

      <template #result="{ record }">
        <span :class="getResultClass(record)">
          {{ record.winAmount > 0 ? '+' : '' }}{{ formatMoney(record.winAmount - record.stakeAmount) }}
        </span>
      </template>

      <template #action="{ record }">
        <a-button type="link" @click="viewDetails(record)">
          查看詳情
        </a-button>
      </template>
    </a-table>

    <!-- 詳情彈窗 -->
    <a-modal
      v-model:visible="detailVisible"
      title="遊戲詳情"
      width="700px"
    >
      <game-detail-view :round-id="selectedRoundId" />
    </a-modal>
  </a-card>
</template>
```

---

## 監控指標

| 指標 | Prometheus 名稱 | 說明 |
|------|----------------|------|
| 審計記錄數 | `game_audit_records_total` | 按遊戲類型分類 |
| 記錄延遲 | `game_audit_latency_seconds` | 記錄處理時間 |
| 完整性驗證失敗 | `game_audit_integrity_failures_total` | 需立即告警 |
| 歸檔記錄數 | `game_audit_archived_total` | 歸檔統計 |

---

## 相關文檔

- [03-05_GLI_Certification.md](03-05_GLI_Certification.md) - GLI 認證
- [03-07_RTP_Monitoring.md](03-07_RTP_Monitoring.md) - RTP 監控
- [06-03_Audit_Log.md](../06_Platform_Governance/06-03_Audit_Log.md) - 審計日誌

---

**返回**: [遊戲中心](README.md) | [iGaming 首頁](../README.md)
