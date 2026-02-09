# P1-08: 流水驗證方案 (Turnover Validation Scheme)

**優先級**: P1 重要
**預估工作量**: 2 人天
**風險等級**: 🟡 中
**版本**: v4.0.0
**日期**: 2026-02-05
**關聯文檔**:
- [05-01_Risk_Framework.md](../../05_Risk_Control/05-01_Risk_Framework.md)
- [05-04_Risk_Proposal_Workflow.md](../05_Risk_Control/05-05_Risk_Proposal_Workflow.md)
- [07-withdrawal-risk-correlation.md](./05-06_Withdrawal_Risk_Correlation.md)

---

## 1. 問題陳述

### 1.1 長週期流水驗證的效能瓶頸

當玩家多年未取款（如 3-5 年），取款時需計算自上次取款以來的所有流水。傳統方式需掃描數百萬筆注單，導致：

| 問題 | 影響 |
|------|------|
| 查詢耗時 | 數年數據掃描可能超過 30 秒 |
| 資料庫壓力 | 全表掃描導致 I/O 飆升 |
| 超時風險 | 取款請求可能因查詢超時而失敗 |

### 1.2 活動風控的缺口

現有系統缺乏對活動獎金（Bonus）的投注行為防護：
- 玩家利用低賠率投注完成流水要求（低風險套利）
- 對沖投注（Hedge Betting）消除風險後提取獎金
- 缺乏無效流水的標記機制

---

## 2. 方案設計：存取款快照法 (Checkpoint Snapshot)

### 2.1 核心機制

在每次取款成功時，記錄當前總流水快照。下次取款驗證時，只需計算差值。

**驗證公式**：

```
本次有效流水 = 當前總流水(實時) - 上次快照總流水
```

**效能優勢**：驗證耗時 **O(1)**，與時間跨度無關。

### 2.2 快照資料表設計

```sql
CREATE TABLE t_player_turnover_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT NOT NULL,
    tenant_id       BIGINT NOT NULL,
    -- 快照數據
    total_bet       NUMERIC(18, 2) NOT NULL DEFAULT 0,   -- 累計總投注
    total_valid_bet NUMERIC(18, 2) NOT NULL DEFAULT 0,   -- 累計有效流水
    total_win       NUMERIC(18, 2) NOT NULL DEFAULT 0,   -- 累計總獲獎
    -- 快照觸發資訊
    snapshot_type   VARCHAR(30) NOT NULL,                 -- WITHDRAWAL / MANUAL / SCHEDULED
    trigger_id      BIGINT,                               -- 觸發的取款單 ID
    -- 時間戳記
    snapshot_time   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_player_snapshot UNIQUE (player_id, snapshot_time)
);

CREATE INDEX idx_snapshot_player_latest
    ON t_player_turnover_snapshot (player_id, snapshot_time DESC);

COMMENT ON TABLE t_player_turnover_snapshot IS '玩家流水快照表 — 每次成功取款時記錄';
COMMENT ON COLUMN t_player_turnover_snapshot.total_valid_bet IS '累計有效流水（排除無效投注）';
```

### 2.3 驗證服務

```java
public class TurnoverValidationService {

    /**
     * 驗證玩家流水是否滿足取款要求
     * 效能: O(1) — 僅查詢最新快照 + 實時聚合
     */
    public TurnoverResult validateTurnover(Long playerId, BigDecimal requiredMultiplier) {
        // 1. 取得上次快照
        TurnoverSnapshot lastSnapshot = snapshotDao.findLatest(playerId)
            .orElse(TurnoverSnapshot.ZERO);  // 首次取款：快照為 0

        // 2. 取得當前實時流水
        BigDecimal currentValidBet = betRecordDao.sumValidBet(playerId);

        // 3. 計算本期有效流水
        BigDecimal periodValidBet = currentValidBet.subtract(lastSnapshot.getTotalValidBet());

        // 4. 計算所需流水
        BigDecimal requiredTurnover = calculateRequired(playerId, requiredMultiplier);

        // 5. 判定結果
        boolean passed = periodValidBet.compareTo(requiredTurnover) >= 0;

        return TurnoverResult.builder()
            .playerId(playerId)
            .periodValidBet(periodValidBet)
            .requiredTurnover(requiredTurnover)
            .passed(passed)
            .lastSnapshotTime(lastSnapshot.getSnapshotTime())
            .build();
    }

    /**
     * 取款成功後建立新快照
     */
    @Transactional(rollbackFor = Throwable.class)
    public void createSnapshot(Long playerId, Long withdrawalId) {
        BigDecimal currentTotalBet = betRecordDao.sumTotalBet(playerId);
        BigDecimal currentValidBet = betRecordDao.sumValidBet(playerId);
        BigDecimal currentTotalWin = betRecordDao.sumTotalWin(playerId);

        TurnoverSnapshot snapshot = TurnoverSnapshot.builder()
            .playerId(playerId)
            .totalBet(currentTotalBet)
            .totalValidBet(currentValidBet)
            .totalWin(currentTotalWin)
            .snapshotType("WITHDRAWAL")
            .triggerId(withdrawalId)
            .build();

        snapshotDao.insert(snapshot);
    }
}
```

### 2.4 與風控提案的時間範圍對齊

根據「全量一致性 (Full Consistency)」原則：

| 維度 | 掃描範圍 | 說明 |
|------|---------|------|
| 風控提案查詢 | `Last Snapshot Time` → 現在 | 所有未結案提案 |
| 流水驗證 | `Last Snapshot Time` → 現在 | 差值計算 |
| 長週期處理 | 即使 5 年，也查詢快照後所有 URGENT/HIGH 提案 | 不設固定天數上限 |

---

## 3. 活動風控整合 (Activity Risk Integration)

### 3.1 雙層防護架構

```
┌─────────────────────────────────────────────┐
│              活動風控雙層架構                    │
├──────────────────┬──────────────────────────┤
│  Prevention      │  Detection               │
│  (前置攔截)       │  (後置偵測)                │
├──────────────────┼──────────────────────────┤
│  • 領獎攔截       │  • 取款前掃描              │
│    - IP 檢查      │    - 異常行為提案           │
│    - 設備指紋      │    - 風控規則觸發           │
│  • 投注攔截       │  • 報表分析                │
│    - 低賠率投注    │    - 活動 ROI 監控         │
│      → 流水為 0   │    - 濫用者名單             │
│    - 對沖投注      │                           │
│      → 流水為 0   │                           │
└──────────────────┴──────────────────────────┘
```

### 3.2 無效流水規則

以下投注行為的流水貢獻為 0（不計入有效流水）：

| 規則代碼 | 條件 | 說明 |
|---------|------|------|
| `LOW_ODDS` | 賠率 < 1.3 | 低賠率投注（套利風險） |
| `HEDGE_BET` | 偵測到對沖投注組合 | 無風險套利 |
| `MIN_BET_BONUS` | 最小投注額 + 活動流水 | Bonus Chasing |
| `SAME_EVENT_OPPOSITE` | 同一賽事兩面下注 | 確保型投注 |

```java
public BigDecimal calculateValidBet(BetRecord bet) {
    // 低賠率檢查
    if (bet.getOdds().compareTo(new BigDecimal("1.3")) < 0) {
        return BigDecimal.ZERO;  // 流水貢獻為 0
    }

    // 對沖投注檢查
    if (hedgeDetector.isHedgeBet(bet)) {
        return BigDecimal.ZERO;
    }

    // 正常投注：全額計入有效流水
    return bet.getBetAmount();
}
```

### 3.3 UI 警示設計

**審核頁 (Proposal Review)**:
- 高亮標籤：`關聯風險`、`活動套利`、`Bonus Chasing`
- 顯示關聯的活動名稱與流水完成進度

**注單明細頁 (Bet Details)**:
- 標記無效流水注單：`[低賠率] 流水: 0.00`
- 標記對沖注單：`[對沖] 流水: 0.00`
- 提供有效流水佔比統計

---

## 4. 驗收標準

- [ ] 快照機制：取款成功後自動建立流水快照
- [ ] O(1) 驗證：流水驗證耗時 < 50ms（P95），與時間跨度無關
- [ ] 無效流水：低賠率 / 對沖 / Bonus Chasing 投注流水計為 0
- [ ] 時間對齊：風控提案查詢範圍 = 流水驗證範圍 = 上次快照至今
- [ ] UI 警示：審核頁顯示活動風險標籤，注單頁標記無效流水

---

## 5. 變更日誌

### v1.0.0 (2026-02-05)
- 初始版本
- 定義存取款快照法（Checkpoint Snapshot）
- 定義活動風控雙層防護架構
- 定義無效流水規則（低賠率、對沖、Bonus Chasing）
- 統一風控提案與流水驗證的時間範圍

---
