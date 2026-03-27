# 無縫錢包技術架構

> **規範來源**: [03-03_Seamless_Wallet_Analysis.md](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md)
> **目標讀者**: 架構師、後端開發者、DevOps
> **業務需求**: [無縫錢包需求](../../requirements/02_Financial_Operations/01_Seamless_Wallet_Requirements.md)
> **同步時間**: 2026-02-08

---

## 1. 概述

本文檔涵蓋無縫錢包 (Seamless Wallet) 整合系統的技術架構和實作細節。包括 API 規格、併發控制機制、狀態機、資料庫結構、監控配置以及 SmartAdmin 架構對應。

---

## 2. 基於回合的狀態機

### 2.1 狀態圖

```mermaid
stateDiagram-v2
    [*] --> IDLE: No Active Round

    IDLE --> OPEN: Bet Request Received

    state OPEN {
        [*] --> WaitingWin: Bet Success
        WaitingWin --> WaitingWin: Additional Bet (Same Round)
    }

    OPEN --> CLOSED: Win Request Received
    OPEN --> CANCELLED: Rollback Request
    OPEN --> TIMEOUT: No Win After 2 Hours

    state TIMEOUT {
        [*] --> QueryGP: Query GP API
        QueryGP --> GPClosed: GP Status = CLOSED
        QueryGP --> GPPending: GP Status = PENDING
        QueryGP --> GPError: GP API Error
    }

    GPClosed --> CLOSED: Auto Close Round
    GPPending --> PENDING_REVIEW: Manual Review Required
    GPError --> PENDING_REVIEW

    state PENDING_REVIEW {
        [*] --> CSReview: CS Agent Investigation
        CSReview --> ManualClose: Approve Close
        CSReview --> ManualCancel: Approve Cancel
    }

    ManualClose --> CLOSED
    ManualCancel --> CANCELLED
    CLOSED --> ADJUSTED: Resettlement Request

    ADJUSTED --> [*]
    CLOSED --> [*]
    CANCELLED --> [*]

    note right of OPEN
        收到投注請求
        動作：扣除餘額
        建立回合紀錄
        狀態 = OPEN
        追加投注：累加投注金額
    end note

    note right of CLOSED
        收到派彩請求
        動作：增加餘額
        更新回合紀錄
        狀態 = CLOSED
        自動關閉：增加派彩金額
        來源：GP 查詢
    end note

    note right of CANCELLED
        收到回滾請求
        動作：退還投注
        狀態 = CANCELLED
    end note

    note right of TIMEOUT
        2 小時內無派彩
        孤立回合
        觸發：排程任務
        每 15 分鐘執行
        檢查 status=OPEN
        且 created_at < NOW() - 2 hours
        查詢 GP API 取得最終狀態
        GET /round/{id}/status
    end note

    note right of PENDING_REVIEW
        需要人工審核
        通知客服團隊
        金額 > $1000 升級處理
        HIGH 優先級：金額 > $1000
        MEDIUM 優先級：金額 <= $1000
        SLA：24 小時內回應
    end note

    note right of ADJUSTED
        重新結算請求
        動作：調整餘額
        狀態 = ADJUSTED
    end note
```

---

## 3. 冪等性實作

> **交叉引用 — 冪等策略三文件導航**:
> - [Seamless_Wallet_Technical.md](03_Seamless_Wallet_Technical.md#4-冪等三層防禦) — 三層架構圖（Redis → DB UNIQUE → Fallback Query）+ Java IdempotencyGuard 實作
> - [Financial_Implementation.md](04_Financial_Implementation.md#23-並發安全的餘額扣減redis-lua) — WalletManager 中 Redis 快取層的實際扣款應用
> - **本文件** (Seamless_Wallet_Analysis.md) — 逾時重試時序圖 + 組件設計分析

### 3.1 逾時與重試時序圖

```mermaid
sequenceDiagram
    participant GP as 遊戲供應商
    participant GW as API Gateway
    participant Platform as Wallet Service
    participant Redis as Redis Cache
    participant DB as Database

    Note over GP,DB: 情境 C：逾時與冪等重試

    rect rgb(255, 230, 230)
        Note over GP,DB: 第一次嘗試 - 發生逾時

        GP->>GW: POST /debit<br/>{txId: "TX001", amount: 100}
        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX001
        Redis-->>Platform: null (不存在)

        Platform->>DB: BEGIN TRANSACTION

        Platform->>DB: SELECT balance FROM wallet<br/>WHERE player_id = ? FOR UPDATE
        DB-->>Platform: balance = 500

        Platform->>DB: UPDATE wallet<br/>SET balance = 400<br/>WHERE player_id = ?
        DB-->>Platform: OK (1 row affected)

        Platform->>DB: INSERT INTO transactions<br/>(tx_id, type, amount, balance_after)<br/>VALUES ('TX001', 'DEBIT', 100, 400)
        DB-->>Platform: OK

        Platform->>DB: COMMIT

        Platform->>Redis: SETEX idempotency:TX001<br/>value: {status: SUCCESS, balance: 400}<br/>TTL: 3600
        Redis-->>Platform: OK

        Platform->>GW: Response: {status: SUCCESS, balance: 400}

        Note over GW,GP: 網路錯誤 - 回應遺失！
        GW-xGP: TCP Connection Reset / Timeout

        Note over GP: GP 收到 TIMEOUT<br/>狀態未知<br/>決定 RETRY
    end

    rect rgb(230, 255, 230)
        Note over GP,DB: 第二次嘗試 - 冪等重試（相同 txId）

        GP->>GW: POST /debit<br/>{txId: "TX001", amount: 100}<br/>相同 txId

        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX001
        Redis-->>Platform: {status: SUCCESS, balance: 400}<br/>存在！

        Note over Platform: 冪等檢查通過<br/>回傳快取回應<br/>無資料庫操作！

        Platform-->>GW: Response: {status: SUCCESS, balance: 400}<br/>來源：CACHED

        GW-->>GP: Response: {status: SUCCESS, balance: 400}

        Note over GP: 重試成功<br/>收到回應<br/>玩家餘額：400
    end

    rect rgb(230, 230, 255)
        Note over GP,DB: 第三次嘗試 - 不同 txId（新交易）

        GP->>GW: POST /debit<br/>{txId: "TX002", amount: 50}<br/>新 txId

        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX002
        Redis-->>Platform: null (不存在)

        Platform->>DB: BEGIN TRANSACTION

        Platform->>DB: SELECT balance FROM wallet<br/>WHERE player_id = ? FOR UPDATE
        DB-->>Platform: balance = 400

        Platform->>DB: UPDATE wallet<br/>SET balance = 350<br/>WHERE player_id = ?
        DB-->>Platform: OK

        Platform->>DB: INSERT INTO transactions<br/>(tx_id, type, amount, balance_after)<br/>VALUES ('TX002', 'DEBIT', 50, 350)
        DB-->>Platform: OK

        Platform->>DB: COMMIT

        Platform->>Redis: SETEX idempotency:TX002<br/>value: {status: SUCCESS, balance: 350}<br/>TTL: 3600
        Redis-->>Platform: OK

        Platform->>GW: Response: {status: SUCCESS, balance: 350}
        GW-->>GP: Response: {status: SUCCESS, balance: 350}

        Note over GP: 新交易成功<br/>玩家餘額：350
    end

    style Platform fill:#E6E6FA
    style Redis fill:#FFE4B5
    style DB fill:#ADD8E6
```

### 3.2 冪等性組件設計

| 組件 | 策略 | 關鍵參數 |
|-----------|----------|---------------|
| **唯一性保證** | txId 作為 Redis Key + DB 唯一約束 | `UNIQUE INDEX (tx_id)` |
| **快取 TTL** | Redis SETEX 避免永久佔用記憶體 | 3600 秒（1 小時） |
| **回應一致性** | 快取完整回應（餘額、狀態） | JSON 格式儲存 |
| **併發保護** | Redis SETNX 或 Lua Script | 原子操作 |
| **清理策略** | TTL 自動過期 + 定期批次清理 | 每日清理超過 24 小時的紀錄 |

### 3.3 錯誤處理

1. **Redis 不可用**：降級為僅檢查 DB 唯一約束（效能降低但正確性保持）
2. **DB 唯一約束衝突**：捕獲例外，查詢原始紀錄，回傳原始結果
3. **txId 格式錯誤**：400 Bad Request，拒絕請求

---

## 4. 亂序處理決策樹

### 4.1 策略決策流程圖

```mermaid
flowchart TD
    START[收到派彩請求] --> CHECK{"檢查 refTxId<br/>SELECT * FROM transactions<br/>WHERE tx_id = refTxId"}

    CHECK -->|找到投注| NORMAL["正常流程<br/>增加派彩金額<br/>關聯投注<br/>回傳 SUCCESS"]

    CHECK -->|投注不存在| STRATEGY{選擇策略}

    STRATEGY -->|策略 1<br/>立即拒絕| S1["回傳錯誤：<br/>BET_NOT_FOUND<br/>HTTP 400<br/>Error Code: 1001"]

    S1 --> S1A{GP 重試派彩？}
    S1A -->|是 - 投注到達後| S1B["下次重試：<br/>投注存在則 SUCCESS"]
    S1A -->|否 - GP 放棄| S1C["玩家遺失派彩<br/>需要人工調查"]

    STRATEGY -->|策略 2<br/>允許孤立派彩| S2["無投注也允許派彩<br/>增加金額<br/>標記：ORPHAN_WIN<br/>標記待審核"]

    S2 --> S2A{投注稍後到達？}
    S2A -->|是| S2B["問題：<br/>雙重增額風險<br/>玩家獲得兩次派彩"]
    S2A -->|否| S2C["對帳不符<br/>GP 有投注，平台只有派彩"]

    STRATEGY -->|策略 3<br/>待處理佇列| S3["將派彩存入待處理佇列<br/>INSERT INTO pending_wins<br/>(ref_tx_id, amount, expires_at)<br/>TTL: 30 分鐘"]

    S3 --> S3A{投注在 30 分鐘內到達？}
    S3A -->|是| S3B["自動處理：<br/>1. 處理投注扣款<br/>2. 處理待處理派彩增額<br/>3. 從佇列移除"]
    S3A -->|否 - 逾時| S3C["升級至人工審核<br/>建立工單<br/>優先級：HIGH"]

    S3B --> SUCCESS1["完成<br/>投注與派彩皆已處理"]
    S1B --> SUCCESS2["完成<br/>重試後處理"]
    S3C --> REVIEW[需要人工審核]
    S1C --> REVIEW
    S2B --> REVIEW
    S2C --> REVIEW

    NORMAL --> END1[結束 - 正常]
    SUCCESS1 --> END2[結束 - 成功]
    SUCCESS2 --> END2
    REVIEW --> END3[結束 - 人工介入]

    style NORMAL fill:#90EE90
    style S1 fill:#FFB6C1
    style S2 fill:#FF6B6B
    style S3 fill:#FFD93D
    style SUCCESS1 fill:#90EE90
    style SUCCESS2 fill:#90EE90
    style REVIEW fill:#DDA0DD
```

### 4.2 策略切換決策樹

```mermaid
flowchart TD
    START[收到派彩請求] --> CHECK_STRATEGY{目前策略？}

    CHECK_STRATEGY -->|策略 3 啟用| CHECK_CONDITIONS{檢查系統健康}

    CHECK_CONDITIONS -->|佇列大小 > 500| DEGRADE_S1["降級至策略 1<br/>發送告警<br/>回傳 BET_NOT_FOUND"]
    CHECK_CONDITIONS -->|Redis 掛掉| DEGRADE_S1
    CHECK_CONDITIONS -->|DB 延遲 > 5s| DEGRADE_S1
    CHECK_CONDITIONS -->|CPU > 90%| DEGRADE_S1

    CHECK_CONDITIONS -->|全部健康| EXECUTE_S3["執行策略 3<br/>存入待處理佇列<br/>TTL: 30 min"]

    CHECK_STRATEGY -->|策略 1 啟用| CHECK_RECOVERY{檢查恢復條件}

    CHECK_RECOVERY -->|佇列 < 100<br/>持續 10 分鐘| UPGRADE_S3["升級至策略 3<br/>發送通知<br/>重啟待處理佇列"]
    CHECK_RECOVERY -->|Redis 健康<br/>+ 穩定 5 分鐘| UPGRADE_S3
    CHECK_RECOVERY -->|DB 延遲 < 1s<br/>持續 10 分鐘| UPGRADE_S3
    CHECK_RECOVERY -->|CPU < 70%<br/>持續 10 分鐘| UPGRADE_S3

    CHECK_RECOVERY -->|條件未達| EXECUTE_S1["執行策略 1<br/>回傳 BET_NOT_FOUND<br/>依賴 GP 重試"]

    DEGRADE_S1 --> NOTIFY_OPS["告警通知<br/>Slack: #game-ops<br/>PagerDuty: On-Call"]

    EXECUTE_S3 --> CHECK_GP_RELIABILITY{"GP 重試率<br/>< 80%？"}
    CHECK_GP_RELIABILITY -->|是 - GP 不可靠| EMERGENCY_S2["緊急切換至策略 2<br/>允許孤立派彩<br/>標記待人工審核"]
    CHECK_GP_RELIABILITY -->|否 - GP 可靠| END_S3[結束 - S3 處理]

    UPGRADE_S3 --> NOTIFY_RECOVERY["恢復通知<br/>Slack: #game-ops"]

    NOTIFY_OPS --> END_S1[結束 - S1 處理]
    EXECUTE_S1 --> END_S1
    EMERGENCY_S2 --> END_S2[結束 - S2 處理]
    NOTIFY_RECOVERY --> END_S3

    style DEGRADE_S1 fill:#FFB6C1
    style UPGRADE_S3 fill:#90EE90
    style EMERGENCY_S2 fill:#FF6B6B
    style EXECUTE_S3 fill:#FFD93D
    style EXECUTE_S1 fill:#E0E0E0
    style NOTIFY_OPS fill:#FFA07A
    style NOTIFY_RECOVERY fill:#98FB98
```

---

## 5. 完整極端情境決策矩陣

### 5.1 統一例外處理流程圖

```mermaid
flowchart TD
    START[收到 GP 請求] --> TYPE{請求類型？}

    TYPE -->|投注請求| SCENARIO_CHECK_BET{"情境檢測"}

    SCENARIO_CHECK_BET -->|情境 G：免費旋轉| FREE_SPIN["檢測到免費旋轉<br/>amount = 0"]
    FREE_SPIN --> VALIDATE_FREE[驗證免費旋轉配額]
    VALIDATE_FREE -->|配額有效| ACCEPT_FREE["接受 amount=0 交易<br/>記錄免費旋轉標記"]
    VALIDATE_FREE -->|配額無效| REJECT_FREE[拒絕：免費旋轉配額已用盡]

    SCENARIO_CHECK_BET -->|情境 H：獎金錢包| BONUS_WALLET[獎金錢包檢查]
    BONUS_WALLET --> GAME_TYPE{遊戲支援獎金？}
    GAME_TYPE -->|支援| DUAL_WALLET["雙錢包扣款<br/>1. 先扣現金<br/>2. 再扣獎金"]
    GAME_TYPE -->|不支援| CASH_ONLY["僅現金錢包<br/>忽略獎金餘額"]

    SCENARIO_CHECK_BET -->|情境 A：正常投注| NORMAL_BET[正常投注流程]
    NORMAL_BET --> CONCURRENCY_CHECK{情境 B：併發檢測}

    CONCURRENCY_CHECK -->|檢測到併發| RACE_CONDITION[檢測到競爭條件]
    RACE_CONDITION --> ACQUIRE_LOCK["取得分佈式鎖<br/>Redis: SETNX lock:player:{id}"]
    ACQUIRE_LOCK -->|成功| BALANCE_CHECK
    ACQUIRE_LOCK -->|失敗 - 重試 3 次| RETRY_LOCK["指數退避重試<br/>50ms, 100ms, 200ms"]
    RETRY_LOCK -->|仍然失敗| REJECT_CONCURRENCY[拒絕：系統繁忙 - 稍後重試]

    CONCURRENCY_CHECK -->|無併發| BALANCE_CHECK{情境 A：餘額檢查}

    BALANCE_CHECK -->|不足| INSUFFICIENT_FUNDS[檢測到餘額不足]
    INSUFFICIENT_FUNDS --> CHECK_BONUS_ELIGIBLE{有可用獎金？}
    CHECK_BONUS_ELIGIBLE -->|有| DUAL_WALLET
    CHECK_BONUS_ELIGIBLE -->|無| REJECT_INSUFFICIENT["拒絕：餘額不足<br/>errorCode: INSUFFICIENT_FUNDS"]

    BALANCE_CHECK -->|充足| OPTIMISTIC_LOCK["樂觀鎖扣款<br/>UPDATE balance WHERE version={v}"]
    OPTIMISTIC_LOCK -->|成功| BET_SUCCESS[回傳 Success + txId]
    OPTIMISTIC_LOCK -->|版本衝突| RETRY_LOCK

    DUAL_WALLET --> OPTIMISTIC_LOCK
    CASH_ONLY --> BALANCE_CHECK
    ACCEPT_FREE --> BET_SUCCESS

    TYPE -->|派彩請求| SCENARIO_CHECK_WIN{"情境檢測"}

    SCENARIO_CHECK_WIN -->|情境 I：頭獎| JACKPOT["檢測到頭獎<br/>amount > $10,000"]
    JACKPOT --> JACKPOT_MODE{GP 協議模式？}
    JACKPOT_MODE -->|人工審批| JACKPOT_NOTIFY["發送 NotifyWin<br/>待人工審核"]
    JACKPOT_NOTIFY --> JACKPOT_PENDING["狀態：PENDING_APPROVAL<br/>暫不增額"]
    JACKPOT_MODE -->|自動增額| JACKPOT_AUTO["自動增額 + 凍結帳戶<br/>觸發風控審核"]

    SCENARIO_CHECK_WIN -->|情境 D：亂序| OUT_OF_ORDER["檢測到亂序<br/>派彩早於投注"]
    OUT_OF_ORDER --> PENDING_QUEUE["存入待處理派彩佇列<br/>等待投注請求<br/>TTL: 2 小時"]

    SCENARIO_CHECK_WIN -->|情境 C：正常派彩| NORMAL_WIN[正常派彩流程]
    NORMAL_WIN --> IDEMPOTENCY_CHECK{"冪等檢查<br/>txId 已處理？"}
    IDEMPOTENCY_CHECK -->|已處理| IDEMPOTENT_RESPONSE["回傳快取結果<br/>Redis: idempotency:{txId}"]
    IDEMPOTENCY_CHECK -->|未處理| CREDIT_BALANCE["增額至錢包<br/>UPDATE balance += amount"]
    CREDIT_BALANCE --> WIN_SUCCESS["回傳 Success<br/>快取結果 TTL=1h"]

    TYPE -->|回滾/退款請求| SCENARIO_E[情境 E：回滾補償]
    SCENARIO_E --> FIND_ORIGINAL_TX{依 txId 查找原始交易}
    FIND_ORIGINAL_TX -->|找到| ROLLBACK_EXECUTE["執行反向操作<br/>Debit 變 Credit<br/>Credit 變 Debit"]
    ROLLBACK_EXECUTE --> MARK_ROLLED_BACK["標記原始交易 ROLLED_BACK<br/>記錄審計日誌"]
    MARK_ROLLED_BACK --> ROLLBACK_SUCCESS[回傳 Success]

    FIND_ORIGINAL_TX -->|找不到| ROLLBACK_NOT_FOUND["原始交易不存在<br/>投注請求從未到達"]
    ROLLBACK_NOT_FOUND --> ROLLBACK_SUCCESS_ANYWAY["回傳 Success<br/>目標達成：未扣款"]

    TYPE -->|調整/重新結算請求| SCENARIO_F[情境 F：重新結算]
    SCENARIO_F --> ADJUST_TYPE{調整類型？}
    ADJUST_TYPE -->|多付 - 回收| NEGATIVE_ADJUST["執行負向增額<br/>balance -= adjustment_amount"]
    NEGATIVE_ADJUST --> CHECK_NEGATIVE{扣除後餘額 < 0？}
    CHECK_NEGATIVE -->|是| ALLOW_NEGATIVE["允許負餘額<br/>觸發風控告警<br/>標記：MANUAL_RECOVERY"]
    CHECK_NEGATIVE -->|否| ADJUST_SUCCESS[回傳 Success]

    ADJUST_TYPE -->|少付 - 補發| POSITIVE_ADJUST["執行正向增額<br/>balance += adjustment_amount"]
    POSITIVE_ADJUST --> ADJUST_SUCCESS

    ALLOW_NEGATIVE --> MANUAL_REVIEW["人工介入<br/>1. 凍結提款<br/>2. 聯繫玩家<br/>3. 分期回收"]

    BET_SUCCESS --> TIMEOUT_MONITOR{情境 C：逾時監控}
    TIMEOUT_MONITOR -->|GP 無回應 2h| TIMEOUT_DETECTED[檢測到逾時]
    TIMEOUT_DETECTED --> QUERY_GP["主動查詢 GP 狀態<br/>GET /query?roundId={id}"]
    QUERY_GP -->|GP 已結算| COMPENSATE_WIN["補償性增額<br/>防止資金卡住"]
    QUERY_GP -->|GP 仍在處理| EXTEND_TIMEOUT["延長逾時<br/>繼續等待"]
    QUERY_GP -->|GP 無紀錄| ROUND_CANCELLED["標記回合 CANCELLED<br/>退款給玩家"]

    style FREE_SPIN fill:#E8F5E9
    style BONUS_WALLET fill:#E3F2FD
    style RACE_CONDITION fill:#FFF3E0
    style INSUFFICIENT_FUNDS fill:#FFEBEE
    style JACKPOT fill:#F3E5F5
    style OUT_OF_ORDER fill:#E0F2F1
    style SCENARIO_E fill:#FFF9C4
    style SCENARIO_F fill:#FCE4EC
    style TIMEOUT_DETECTED fill:#ECEFF1

    style REJECT_FREE fill:#FFCDD2
    style REJECT_CONCURRENCY fill:#FFCDD2
    style REJECT_INSUFFICIENT fill:#FFCDD2
    style ALLOW_NEGATIVE fill:#FF9800

    style BET_SUCCESS fill:#C8E6C9
    style WIN_SUCCESS fill:#C8E6C9
    style ROLLBACK_SUCCESS fill:#C8E6C9
    style ADJUST_SUCCESS fill:#C8E6C9
```

---

## 6. 負餘額狀態機

```mermaid
stateDiagram-v2
    [*] --> Normal
    Normal --> Deduction : GP Request (Debit/Rollback)
    Deduction --> Normal : Balance >= Amount
    Deduction --> NegativeLocked : Balance < Amount (Force Deduct)

    state NegativeLocked {
        [*] --> UpdateBalance : Set Balance < 0
        UpdateBalance --> LockAccount : Status = SUSPENDED_RISK
        LockAccount --> CreateTicket : Notify Risk Team
    }

    NegativeLocked --> Normal : Manual Deposit / Debt Cleared (Admin Unlock)
```

---

## 7. 整合交易流程

```mermaid
flowchart TD

    Req["GP 請求 (Debit/Credit)"] --> CheckID{"有 TransactionId？"}

    CheckID -->|否| Err400["Error 400: Missing ID"]

    CheckID -->|是| CheckCache{"冪等檢查<br/>(Redis Key 存在？)"}

    CheckCache -->|是| ReturnCache["回傳快取回應"]

    CheckCache -->|否| LoadConfig["載入遊戲配置<br/>(取得錢包優先順序)"]

    LoadConfig --> TypeCheck{"請求類型？"}

    TypeCheck -->|扣款 (投注)| CalcDeduction["計算扣款順序<br/>(例如 獎金優先於現金)"]

    CalcDeduction --> CheckBal{"餘額充足？"}

    CheckBal -->|否| ErrFund["Error: Insufficient Funds"]

    CheckBal -->|是| ExecDebit["對錢包執行扣款"]

    TypeCheck -->|增額 (派彩/回滾)| ExecCredit["執行增額/調整"]

    ExecCredit --> CheckNeg{"結果餘額 < 0？"}

    CheckNeg -->|是| LockAcc["更新餘額並<br/>SET STATUS = LOCKED"]

    LockAcc --> AlertRisk["觸發風控告警"]

    CheckNeg -->|否| NormalUpdate["更新餘額"]

    ExecDebit --> SaveTx["儲存交易日誌"]

    ErrFund --> SaveTx

    LockAcc --> SaveTx

    NormalUpdate --> SaveTx

    SaveTx --> CacheRes["快取回應至 Redis"]

    CacheRes --> Resp["回傳 API 回應"]
```

---

## 8. 錢包扣款順序配置

### 8.1 遊戲配置結構

```json
{
  "game_code": "slot_001",
  "wallet_priority": ["BONUS_WALLET", "CASH_WALLET"],
  "use_system_default": false
}
```

### 8.2 執行流程

1. 收到投注請求
2. 檢查 `game_config.wallet_priority` 是否存在
3. 若存在且 `use_system_default = false`：使用遊戲配置
4. 若不存在：使用系統預設（獎金 -> 現金 -> 信用額度）
5. 依優先順序扣除餘額
6. 若首選錢包餘額不足，允許混合扣款（記錄分割明細）

---

## 9. 監控與告警配置

### 9.1 Prometheus AlertManager 規則

```yaml
# Prometheus AlertManager Rules
groups:
  - name: seamless_wallet_alerts
    rules:
      # 情境 A：異常餘額不足率
      - alert: HighInsufficientFundsRate
        expr: (rate(bet_rejected_insufficient_funds_total[5m]) / rate(bet_requests_total[5m])) > 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "餘額不足拒絕率 > 30%，可能玩家資金耗盡或存款異常"

      # 情境 B：高鎖競爭
      - alert: HighLockContentionRate
        expr: rate(redis_lock_timeout_total[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Redis 鎖逾時頻率過高，系統併發壓力過大"

      # 情境 C：異常 GP 逾時率
      - alert: HighGPTimeoutRate
        expr: (rate(gp_timeout_total[1h]) / rate(bet_requests_total[1h])) > 0.01
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "GP 逾時率 > 1%，檢查 GP 服務健康狀態"

      # 情境 D：亂序積壓
      - alert: PendingWinQueueBacklog
        expr: pending_win_queue_size > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "待處理派彩佇列積壓 > 100，可能投注請求延遲或遺失"

      # 情境 F：負餘額累積
      - alert: NegativeBalanceAccumulation
        expr: sum(player_negative_balance) < -10000
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "負餘額累積超過 $10,000，觸發財務風控審核"

      # 情境 I：異常頭獎頻率
      - alert: UnusualJackpotFrequency
        expr: rate(jackpot_win_total[1h]) > 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "頭獎觸發 > 1 小時 5 次，懷疑遊戲邏輯異常或詐騙"
```

### 9.2 策略切換告警

```yaml
# Prometheus AlertManager Rules
alerts:
  - name: out_of_order_strategy_degraded
    expr: out_of_order_strategy != 3
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "亂序處理策略降級至策略 {{ $value }}"
      description: "目前策略：{{ if eq $value 1 }}立即拒絕{{ else if eq $value 2 }}孤立派彩（緊急）{{ end }}"

  - name: out_of_order_emergency_mode
    expr: out_of_order_strategy == 2
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "亂序處理進入緊急模式（策略 2 - 孤立派彩）"
      description: "GP 重試率過低，允許孤立派彩，需立即人工介入"
```

---

## 10. SmartAdmin 架構對應

### 10.1 五層模組設計

| 層級 | 類別模式 | 職責 | 註解限制 |
|-------|--------------|---------------|----------------------|
| **Controller** | `SeamlessWalletController` | 接收 GP HTTP 請求，參數驗證，回傳 ResponseDTO | 禁用 @Transactional |
| **Service** | `SeamlessWalletService` | 業務協調，冪等檢查，呼叫 Manager | 禁用 @Transactional |
| **Manager** | `SeamlessWalletTransactionManager` | 交易管理，分佈式鎖，錢包扣款/增額 | **僅此層允許** @Transactional |
| **Dao** | `GameTransactionRecordDao` | 資料庫 CRUD，MyBatis Mapper | 無業務邏輯 |
| **Entity** | `GameTransactionRecordEntity` | 資料模型，對應表結構 | 無業務邏輯 |

### 10.2 依賴規則（由 ArchitectureTest 強制）

```text
Controller -> Service (允許)
Service -> Dao      (允許 - 用於冪等紀錄查詢)
Service -> Manager  (允許 - 需要 @Transactional 時)
Manager -> Dao      (允許)

Controller -> Dao   (禁止 - 違反分層)
Controller -> Manager (禁止 - 違反分層)
```

### 10.3 Entity 層

**GameTransactionRecordEntity.java** - 對應遊戲交易紀錄表，追蹤所有 Debit/Credit/Rollback/Adjust 操作，包含 txId、roundId、type、amount、balance_after 和 status 欄位。

**RoundStateEntity.java** - 對應回合狀態表，用於基於回合的 GP 整合，追蹤 roundId、status (OPEN/CLOSED/CANCELLED/ADJUSTED/TIMEOUT/PENDING_REVIEW)、總投注金額、總派彩金額和時間戳記。

### 10.3.1 資料庫結構

```sql
-- 無縫錢包操作的遊戲交易紀錄表
CREATE TABLE t_game_transaction_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    player_id       BIGINT NOT NULL,
    tx_id           VARCHAR(100) NOT NULL,
    round_id        VARCHAR(100),
    tx_type         VARCHAR(20) NOT NULL,
    amount          DECIMAL(18, 4) NOT NULL,
    balance_after   DECIMAL(18, 4) NOT NULL,
    status          SMALLINT NOT NULL DEFAULT 1,
    game_code       VARCHAR(50),
    provider_code   VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_game_tx_id UNIQUE (tenant_id, tx_id)
);

CREATE INDEX idx_game_tx_player ON t_game_transaction_record(tenant_id, player_id, created_at DESC);
CREATE INDEX idx_game_tx_round ON t_game_transaction_record(tenant_id, round_id) WHERE round_id IS NOT NULL;
CREATE INDEX idx_game_tx_status ON t_game_transaction_record(tenant_id, status, created_at) WHERE status != 1;

-- 基於回合的遊戲供應商整合回合狀態表
CREATE TABLE t_round_state (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    player_id       BIGINT NOT NULL,
    round_id        VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    total_bet       DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_win       DECIMAL(18, 4) NOT NULL DEFAULT 0,
    game_code       VARCHAR(50),
    provider_code   VARCHAR(50),
    opened_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP,
    timeout_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_round_id UNIQUE (tenant_id, round_id)
);

CREATE INDEX idx_round_player ON t_round_state(tenant_id, player_id, opened_at DESC);
CREATE INDEX idx_round_status ON t_round_state(tenant_id, status, opened_at) WHERE status = 'OPEN';
CREATE INDEX idx_round_timeout ON t_round_state(tenant_id, timeout_at) WHERE status = 'OPEN' AND timeout_at IS NOT NULL;
```

### 10.4 DAO 層

**GameTransactionRecordDao.java** - 遊戲交易 CRUD 操作的 MyBatis Mapper，包含依 txId 查詢的冪等檢查和對帳批次查詢。

**RoundStateDao.java** - 回合狀態管理的 MyBatis Mapper，包含查詢孤立回合（status=OPEN 且 created_at 超過閾值）。

### 10.5 Manager 層

**SeamlessWalletTransactionManager.java** - 處理所有交易操作：
- `processBet()`：分佈式鎖取得、餘額檢查、在 @Transactional 內執行扣款
- `processWin()`：增額執行、在 @Transactional 內關閉回合
- `processRollback()`：在 @Transactional 內執行反向操作
- `processAdjustment()`：支援負餘額的重新結算處理，在 @Transactional 內

```java
@Component
@RequiredArgsConstructor
public class SeamlessWalletTransactionManager {

    private final PlayerWalletDao walletDao;
    private final GameTransactionRecordDao transactionDao;
    private final RoundStateDao roundStateDao;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "seamless:player:";

    /**
     * 使用分佈式鎖和交易管理處理投注。
     * 依 SmartAdmin 架構，@Transactional 僅允許在 Manager 層使用。
     */
    @Transactional(rollbackFor = Throwable.class)
    public TransactionResult processBet(BetRequestForm form) {
        String lockKey = LOCK_KEY_PREFIX + form.getPlayerId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 取得帶逾時的分佈式鎖
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY);
            }

            // 使用 SELECT FOR UPDATE 檢查餘額
            PlayerWalletEntity wallet = walletDao.selectForUpdate(form.getPlayerId());
            if (wallet.getBalance().compareTo(form.getAmount()) < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
            }

            // 扣除餘額
            BigDecimal newBalance = wallet.getBalance().subtract(form.getAmount());
            walletDao.updateBalance(form.getPlayerId(), newBalance, wallet.getVersion());

            // 記錄交易
            GameTransactionRecordEntity record = new GameTransactionRecordEntity();
            record.setTxId(form.getTxId());
            record.setPlayerId(form.getPlayerId());
            record.setType(TransactionType.DEBIT);
            record.setAmount(form.getAmount());
            record.setBalanceAfter(newBalance);
            transactionDao.insert(record);

            return TransactionResult.success(newBalance);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 10.6 Service 層

**SeamlessWalletService.java** - 協調業務邏輯：
- 透過 Redis 快取查詢進行冪等檢查
- 委派 Manager 處理交易操作
- 處理亂序待處理佇列邏輯
- 與 Risk Engine 協調異常偵測

### 10.7 Controller 層

**SeamlessWalletController.java** - HTTP 介面：
- 接收 GP 請求（Debit、Credit、Rollback、Adjust、GetBalance）
- 參數驗證
- 以標準化錯誤碼回傳 ResponseDTO

### 10.8 基礎模組依賴

| 基礎模組 | 用途 | 參考位置 |
|------------------|---------|-------------------|
| **foundation.redis-lock** | 分佈式鎖防止併發扣款（情境 B） | SeamlessWalletTransactionManager.processBet/processWin/processRollback() |
| **foundation.cache** | Caffeine + Redis 快取用於冪等 | SeamlessWalletService.checkIdempotency() |
| **foundation.audit-log** | 審計日誌記錄 | 所有交易後自動記錄 |
| **foundation.retry** | 失敗重試策略 | GP API 呼叫失敗重試 |

### 10.9 冪等 TTL 標準化

從 v2.0.0 起，統一冪等 TTL 為 **3600 秒（1 小時）**，與有效投注額計算模組一致，便於營運和監控對齊。

---

## 11. 架構影響分析

1. **遊戲配置服務**：結構需擴展以支援 `wallet_priority` 配置
2. **錢包服務**：
   - `debit()` 方法須支援接受 `priority_list` 參數
   - 須處理「分割交易」（例如 20 來自獎金，80 來自現金）
3. **風控引擎**：須監聽 `BALANCE_CHANGE` 事件，當 `NewBalance < 0` 時觸發帳戶鎖定

---

## 12. 關鍵實作注意事項

1. **情境優先順序**：決策樹依 P0 -> P1 -> P2 順序處理，確保高優先情境優先處理
2. **冪等保護**：所有 Credit/Debit 操作須通過 `idempotency:{txId}` 檢查
3. **分佈式鎖**：併發情境使用 Redis SETNX + TTL=5s 防止永久佔鎖
4. **降級策略**：當 Redis/GP 不可用時，優先資金安全（拒絕而非錯誤扣款）
5. **審計日誌**：所有例外情境（負餘額、頭獎、回滾）須記錄完整審計日誌
6. **人工介入**：明確預定義人工介入閾值，防止自動決策失控

---

## 13. 相關文檔

### 上游架構
- **統一錢包模型** - 整體錢包架構，可下注餘額公式
- **交易處理流程** - 事件驅動架構，Outbox Pattern

### 深度技術分析
詳細的無縫錢包主題分析（13 個主題），提供細粒度實作指引：

- **無縫錢包主題索引** - 完整導航和學習路徑
  - Token 驗證 - API 認證決策樹（投注 vs 結果 API 驗證策略）
  - 冪等設計 - 三層保護（Redis -> DB -> 分佈式鎖），防重複扣款
  - 體育投注邏輯 - 有效投注額 (Valid Turnover) 計算，HALF WIN/LOSS 處理
  - 有效投注額併發累加 - Lua script 原子性，TOCTOU 攻擊防護
  - 錯誤恢復 - 例外處理，補償交易，回滾策略

### 風控整合
- **風控框架** - 交易風險檢查，對沖偵測

---

## 14. 變更日誌

### v2.0.0 (2026-01-29)

**重大變更**：
1. 新增 SmartAdmin 架構對應（第 10 節）
   - 完整五層架構（Entity/Dao/Manager/Service/Controller）
   - 無縫錢包核心邏輯實作（投注/派彩/回滾）
   - 分佈式鎖（RedisLock）和冪等（Redis Cache）整合
   - 回合狀態管理（基於回合的 GP 支援）
   - 基礎模組依賴文檔（redis-lock, cache, audit-log, retry）
   - ArchitectureTest 驗證規則

2. 統一冪等 TTL 為 3600 秒（1 小時）
   - 與有效投注額計算模組一致
   - 移除不一致的描述（1-2h）

### v1.0.0 (2026-01-28)

**初始版本**：
- 無縫錢包整合情境分析（第 1-2 節）
- 9 種極端情境綜合決策矩陣（第 5 節）
- 冪等、併發控制、亂序處理（第 3-4 節）
- 基於回合的狀態機（第 2 節）
- 負餘額處理、錢包扣款順序（第 6-8 節）
