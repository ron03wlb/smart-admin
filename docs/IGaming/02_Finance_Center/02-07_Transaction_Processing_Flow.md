# 02-07 交易處理流程與鎖機制 (Transaction Processing Flow & Locking Strategy)

## 1. 系統概述 (System Overview)

在統一錢包 (Unified Wallet) 架構下，單筆交易可能同時涉及 **Cash (資產)** 扣除與 **Credit (負債)** 增加。
本模組定義了在高併發 (High Concurrency) 場景下，如何保證 **交易原子性 (Atomicity)** 與 **數據一致性 (Consistency)**，防止「扣了錢卻沒增加額度」或「超額透支」的情況發生。

---

## 2. 併發控制策略 (Concurrency Control)

### 2.1 樂觀鎖 (Optimistic Locking)
適用於絕大多數的 **Wallet Balance Update** 場景。

*   **機制**：在 `player_wallet` 表中增加 `version` 欄位。
*   **SQL 範例**：
    ```sql
    UPDATE player_wallet 
    SET cash_balance = cash_balance - 100, 
        version = version + 1 
    WHERE player_id = 'uuid' 
      AND version = 5 
      AND cash_balance >= 100; -- 確保餘額足夠
    ```
*   **重試機制**：若 `Affected Rows = 0` (表示餘額不足或版本號過期)，Service 層應進行 **Backoff Retry** (最多 3 次)。

### 2.2 悲觀鎖 (Pessimistic Locking)
僅用於 **"結算歸零 (Settlement Reset)"** 或 **"強制平倉 (Liquidation)"** 等高風險管理操作。

*   **機制**：使用 `SELECT ... FOR UPDATE` 鎖定該行記錄。
*   **注意**：務必依序鎖定 (如按 `player_id` 排序)，避免 Deadlock。

---

## 3. 混合支付原子性設計 (Hybrid Payment Atomicity)

當一筆 $100 的下注需要由 $20 Bonus + $30 Cash + $50 Credit 組成時，交易流程如下：

### 3.1 兩階段提交 (2PC / TCC) - 微服務內部
若 Wallet Service 是單體或模組化單體，可透過 DB Transaction 解決。但若跨服務 (如 Game Service -> Wallet Service)，建議採用 **TCC (Try-Confirm-Cancel)** 模式。

#### Phase 1: Try (預扣)
*   檢查 Bonus >= 20, Cash >= 30, (Limit - Outstanding) >= 50。
*   **Action**: 凍結資源 (Frozen Balance)。
    *   `Frozen_Bonus += 20`
    *   `Frozen_Cash += 30`
    *   `Frozen_Credit += 50`

#### Phase 2: Confirm (實扣)
*   遊戲回合確認成立。
*   **Action**: 
    *   `Bonus -= 20`, `Frozen_Bonus -= 20`
    *   `Cash -= 30`, `Frozen_Cash -= 30`
    *   `Outstanding += 50`, `Frozen_Credit -= 50`
    *   寫入 `Transaction Log`。

#### Phase 3: Cancel (回滾)
*   若遊戲回合失敗或 Timeout。
*   **Action**: 釋放凍結資源。

### 3.2 冪等性設計 (Idempotency)
所有涉及資金變動的 API 必須支援冪等性，防止網絡重發導致重複扣款。

*   **Idempotency Key**: 使用 `Manage_Transaction_UUID` (由遊戲商提供或平台生成)。
*   **Check**: 執行前查詢 `wallet_transaction` 表，若 `reference_id` 已存在，直接返回上次結果。

---

## 4. 異常處理與補單 (Exception Handling)

### 4.1 超時未決 (Timeout / Pending)
若 Phase 1 成功但 Phase 2 超時：
*   **Job**: 每分鐘掃描 `Pending TCC Transactions`。
*   **Action**: 向 Game Service 查詢回合狀態。
    *   若回合存在：重試 Confirm。
    *   若回合不存在：執行 Cancel。

### 4.2 餘額負值保護 (Negative Balance Protection)
*   **硬性限制**：`Cash` 與 `Bonus` 絕對不可小於 0。
*   **彈性限制**：`Outstanding` 允許在極端併發下微幅超過 `Credit Limit` (如 < 0.1%)，但需立即觸發風控警報。

---

## 5. 數據一致性檢核
*   **每日校驗 Job**：
    `Sum(Transaction Logic) == Current Balance`
    若不一致，自動標記帳號為 `Risk Locked` 並通知技術團隊。
    若不一致，自動標記帳號為 `Risk Locked` 並通知技術團隊。

---

## 6. 非同步事件廣播 (Async Event Publishing)

為了驅動 **活動系統 (04)** 與 **風控系統 (05)**，Wallet Service 必須在交易完成後發出 Domain Event。
採用 **Transactional Outbox Pattern** 確保資金變動與事件發送的 "最終一致性"。

### 6.1 Outbox Table 設計
在與 `wallet_transaction` 同一個 DB Transaction 中寫入：
```sql
INSERT INTO outreach_event_outbox (id, aggregate_id, type, payload, status)
VALUES (uuid(), player_id, 'WALLET_DEBITED', '{"amount": 100, "game": "slot"}', 'PENDING');
```

### 6.2 關鍵事件列表
*   `WALLET_DEBITED`: 用於計算流水 (Turnover Contribution)、觸發 "投注任務"。
*   `WALLET_CREDITED`: 用於觸發 "贏分任務"、更新排行榜。
*   `DEPOSIT_SUCCESS`: 用於觸發 "首存紅利"。
*   `WITHDRAW_REQUESTED`: 用於觸發 "提款審核"。

### 6.3 消息不丟失保證
*   **CDC / Polling Publisher**: 獨立進程讀取 `outreach_event_outbox` -> 發送至 Kafka -> 更新 Status='SENT'。
*   **Consumer Idempotency**: 下游服務 (Activity Service) 必須利用 `event_id` 實現冪等處理。
