---
title: "ADR-012: 非同步風控提案系統 (Flag Mode)"
status: accepted
date: 2026-02-10
deciders: Risk Team Lead, Architecture Team
---

# ADR-012: 非同步風控提案系統 (Flag Mode)

## 狀態

已接受 (Accepted)

## 背景

五層偵測管線中，Layer 3 (非同步規則引擎) 在偵測到可疑行為時，若直接阻斷交易會造成：
1. 誤殺合法玩家（ML 模型 AUC 0.729，存在誤報）
2. 影響玩家體驗（投注被中斷）
3. 無法累積足夠證據進行人工審核

需要一種機制允許交易繼續但標記為待審。

## 決策

Layer 3 規則引擎偵測結果分為兩種動作：

### BLOCK（即時阻斷）
- 觸發條件：高確信度惡意行為
- 效果：交易被拒絕，即時生效
- 範例：已確認的多帳號、黑名單匹配

### FLAG（非同步提案）
- 觸發條件：中等確信度可疑行為
- 效果：交易繼續，但在 t_risk_flag 建立提案記錄
- 後續：Layer 4 人工審核團隊處理

### Flag 資料模型

```sql
CREATE TABLE t_risk_flag (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    player_id       BIGINT NOT NULL,
    flag_type       VARCHAR(50) NOT NULL,   -- MULTI_ACCOUNT, BONUS_ABUSE, AML_SUSPICIOUS, etc.
    severity        VARCHAR(10) NOT NULL,   -- LOW, MEDIUM, HIGH
    trigger_rule    VARCHAR(100) NOT NULL,  -- 觸發規則 ID
    evidence        JSONB NOT NULL,         -- 觸發證據
    related_txn_ids BIGINT[],              -- 相關交易 ID
    status          VARCHAR(20) NOT NULL,   -- OPEN, REVIEWING, CONFIRMED, DISMISSED
    assigned_to     BIGINT,
    resolution      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP
);
```

### 規則獨立觸發原則

每條規則獨立產生 FLAG/BLOCK 結果，**不進行分數累加**。例如：
- 規則 A 偵測到「深夜高頻投注」→ FLAG (獨立)
- 規則 B 偵測到「IP 地理異常」→ FLAG (獨立)
- 兩個 FLAG 各自進入人工審核佇列，審核員看到完整上下文

### 為什麼不累加分數

1. 獨立規則語意清晰，審核員能精確知道哪條規則觸發
2. 避免多個弱信號累加導致誤殺
3. 規則新增/刪除不影響其他規則的閾值
4. 便於個別規則的準確率追蹤與調優

## 後果

- 正向：玩家體驗不受中等風險偵測影響
- 正向：人工審核有充分證據和上下文
- 正向：可追蹤每條規則的 precision/recall
- 負向：增加人工審核工作量
- 負向：FLAG 到人工處理之間存在時間窗口

## 相關

- Layer 1 (同步黑名單) 不受此 ADR 影響
- Layer 5 (提款延遲檢查) 會參考 FLAG 狀態
- 風險分數邊界 [0,30)/[30,70)/[70,100] 用於 7 維度綜合評估，非規則觸發
