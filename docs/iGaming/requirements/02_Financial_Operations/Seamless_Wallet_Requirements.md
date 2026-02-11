# Seamless Wallet 業務需求文檔（Business Requirements）

> **Canonical Source**: [03-03_Seamless_Wallet_Analysis.md](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md)
> **Audience**: 高層主管、產品經理、合規官員
> **Related Architecture**: [Seamless Wallet 技術實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md)
> **Last Synced**: 2026-02-09
>
> **精煉說明**：技術細節（排程任務、Redis 緩存、資料庫約束、分散式鎖、待處理佇列實現、冪等性機制）已移至架構層。本文檔僅聚焦業務規則。

---

## 驗收標準（Acceptance Criteria）

- [ ] 平台支援所有四種交易模型（Transaction-Based, Round-Based, Transfer-Based, Result-Only）
- [ ] Round 生命週期管理正確處理所有狀態（OPEN, CLOSED, TIMEOUT, PENDING_REVIEW, CANCELLED, ADJUSTED）
- [ ] 孤兒 Round 檢測每 15 分鐘執行一次，並在 > 2 小時後升級處理
- [ ] 冪等性強制防止相同 txId 重試導致的重複加額（100% 準確率）
- [ ] 亂序處理使用策略 3（臨時存儲），30 分鐘過期，壓力下自動降級
- [ ] 並發下注透過分散式鎖防止超額扣款，響應時間 < 200ms
- [ ] 餘額不足返回錯誤，響應時間 < 100ms（P0 優先級）
- [ ] 負餘額處理立即鎖定帳戶並觸發 P2 風控警報
- [ ] Free Spin 配額驗證拒絕過期配額，返回「Free Spin 配額已耗盡」錯誤
- [ ] 雙錢包扣款遵循配置優先級（預設：Bonus → Cash → Credit），支援混合扣款
- [ ] 玩家 Token 過期的 Win 請求被接受（玩家合法資產不受結算時間影響）
- [ ] Jackpot 獎金 > $10,000 觸發人工審批模式或自動加額並凍結帳戶（依 GP 協議）
- [ ] 每日對帳生成自動化差異報告（Diff Reports），標記不匹配的交易 ID 和金額
- [ ] 所有 GP 整合強制要求 TransactionId（拒絕缺少必填欄位的請求）
- [ ] 監控 KPI 達標（亂序 < 0.1%，待處理 < 100，超時升級 < 1%，餘額不足警報 > 30%）

---

## 1. 目的（Purpose）

本文檔定義與遊戲供應商（GP, Game Provider）進行 Seamless Wallet（單一錢包）整合的業務規則、政策和操作要求。涵蓋交易類型、結算規則、玩家面向政策、風險場景和合規要求。

---

## 2. 遊戲供應商整合類型（Game Provider Integration Types）

### 2.1 交易模型分類（Transaction Model Classification）

| 類型 | 說明 | 代表供應商 | 業務影響 |
|:-----|:------------|:------------------------|:---------------------|
| **Transaction-Based** | 每個請求為獨立交易（Debit/Credit），帶有 Amount 或 Type | PG Soft, JILI | 最簡單模型。TransactionId 必須全局唯一。 |
| **Round-Based** | 請求綁定至 RoundId。Bet 開啟 Round，Win 關閉 Round。 | Evolution, Pragmatic Play | 存在「孤兒 Round」風險（Bet 成功但 Win 遺失）。需排程監控。 |
| **Transfer-Based** | 舊式架構模擬 TransferIn / TransferOut | 特定舊式體育博彩 | 語義混淆（與「轉帳錢包」概念衝突）。必須確保動作為系統觸發，非人工。 |
| **Result-Only** | 僅發送遊戲結果（Win/Loss Amount），無 Bet/Win 區分 | 特定彩票/牌類遊戲 | 風控困難：無法在下注時驗證餘額，可能導致負餘額。 |

### 2.2 錢包行為分類（Wallet Behavior Classification）

| 行為 | 說明 | 風險與政策 |
|:---------|:------------|:-------------|
| **GetBalance Only** | GP 僅查詢餘額；餘額變更由 GP 內部記錄，定期結算 | **極高風險**。不推薦，除非使用信用模式運營。 |
| **Async Callback** | 平台立即確認請求接收；實際交易結果透過回調通知異步交付 | 需雙向狀態機；流程複雜度翻倍。 |
| **Batch Processing** | GP 將多個玩家交易合併為單一 HTTP 請求 | 必須支援批次交易處理。業務需決定：全有全無 vs. 部分成功。 |

→ **[Async Response Protocol 技術實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#async-response-protocol)**

---

## 3. Round 生命週期管理（Round Lifecycle Management）（Round-Based 模型）

### 3.1 Round 狀態定義（Round State Definitions）

| 狀態 | 觸發條件 | 自動化動作 | 人工介入 |
|-------|------------------|-----------------|-------------------|
| **OPEN** | Bet 請求成功 | 扣除餘額 + 建立 Round 記錄 | 無 |
| **CLOSED** | Win 請求成功 | 加額餘額 + 關閉 Round | 無 |
| **TIMEOUT** | 開啟超過 2 小時無 Win | 查詢 GP API 最終狀態 | 若 GP API 失敗，升級至 PENDING_REVIEW |
| **PENDING_REVIEW** | GP 狀態未知 | 建立 CS 支援工單 | 必須：CS 人員手動關閉或取消 |
| **CANCELLED** | 收到 Rollback 請求 | 退款 + 標記為已取消 | 無 |
| **ADJUSTED** | 收到 Resettlement 請求 | 調整餘額（可能為負） | 若產生負餘額，觸發風控鎖定 |

### 3.2 孤兒 Round 政策（Orphaned Round Policy）

孤兒 Round 發生於 Bet 成功但 Win 永遠不到達的情況。

- **檢測**：系統每 15 分鐘檢查開啟超過 2 小時的 Round
- **解決**：查詢 GP API 最終狀態；若確認則自動關閉，或升級至人工審核
- **升級標準**：
  - Round 金額 > $1,000：HIGH 優先級，24 小時 SLA
  - Round 金額 ≤ $1,000：MEDIUM 優先級，24 小時 SLA

→ **[孤兒 Round 檢測實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#orphaned-round-detection)** - 排程任務配置、SQL 查詢、GP API 整合

### 3.3 重複 Win 政策（Duplicate Win Policy）

- 重複 Win 請求（相同 roundId + txId）必須返回原始執行結果
- 不得重複加額
- 透過重複防禦機制強制執行

→ **[冪等性實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#idempotency-defense)** - 三層防禦（Redis + DB 唯一約束 + Fallback 查詢）

### 3.4 重新結算負餘額政策（Resettlement Negative Balance Policy）

當 GP 收回多付的獎金且玩家餘額不足時：

- 平台允許餘額為負
- 玩家帳戶自動鎖定
- 觸發風控警報
- 恢復流程遵循負餘額處理政策（見第 7 節）

---

## 4. 交易場景與業務規則（Transaction Scenarios & Business Rules）

### 4.1 正常交易流程（Normal Transaction Flow）（Happy Path）

**Bet（扣款）**：
1. GP 發送扣款請求，包含 user, amount, roundId, txId
2. 平台驗證餘額充足
3. 平台扣除餘額、記錄交易、返回新餘額

**Win（加額）**：
1. GP 發送加額請求，包含 user, amount, roundId, txId, refTxId
2. 平台加額餘額、記錄交易、返回新餘額
3. refTxId 將 Win 連結回原始 Bet

### 4.2 餘額不足（Insufficient Funds）（場景 A）

- 當玩家餘額 < 下注金額時，平台返回 INSUFFICIENT_FUNDS 錯誤
- GP 必須顯示餘額不足訊息並阻止遊戲進行
- 若有 Bonus 錢包可用且遊戲支援，則應用雙錢包扣款（見第 6 節）

### 4.3 並發下注（Concurrent Betting）（場景 B）

- 當同一玩家的多個下注請求同時到達時，平台必須防止超額扣款
- 業務規則：每個玩家同一時間僅能處理一個下注
- 若檢測到競爭且重試限制內無法解決，返回「系統繁忙 - 請稍後重試」

### 4.4 超時與重試（Timeout & Retry）（場景 C）

- 當 GP 等待響應超時時，可能使用相同 txId 重試
- 業務規則：相同 txId 的重試必須返回存儲的原始結果，不重新執行
- 無 txId 的交易必須拒絕並返回錯誤

→ **[冪等性緩存實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#timeout-retry-handling)** - Redis 緩存、1 小時 TTL、Fallback 查詢

### 4.5 亂序請求（Out-of-Order Requests）（場景 D）

當 Win 請求在對應 Bet 之前到達時：

| 策略 | 說明 | 優點 | 缺點 | 建議 |
|----------|------------|------|------|---------------|
| **策略 1：立即拒絕** | 返回 BET_NOT_FOUND 錯誤 | 簡單、無狀態 | 若 GP 不重試，Win 可能遺失 | 適用於有重試機制的可靠 GP |
| **策略 2：允許孤兒 Win** | 無匹配 Bet 仍加額 Win | 玩家不會失去獎金 | 高風險重複加額；對帳困難 | **不推薦**，僅作緊急備案 |
| **策略 3：臨時存儲** | 臨時存儲 Win（30 分鐘過期） | 自動化解決；完整審計追蹤 | 實現複雜度較高 | **推薦用於生產** |

**生產建議**：策略 3 為預設，系統壓力下自動降級至策略 1。

→ **[亂序處理實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#out-of-order-handling)** - 待處理佇列架構、Redis 存儲、TTL 配置、降級觸發器

### 4.6 回滾 / 退款（Rollback / Refund）（場景 E）

- 當遊戲 Round 被取消（例如：體育賽事中斷、系統故障）時，GP 發送 Rollback 請求
- 平台查找原始交易並執行反向操作（退款）
- 若找不到原始交易（Bet 從未到達），平台返回 Success（目標已達成：無扣款發生）

### 4.7 重新結算 / 調整（Resettlement / Adjustment）（場景 F）

- 當 GP 發現錯誤賠付（例如：賠率錯誤）時，發送 Adjust 請求
- 平台支援對同一 Round 多次加額，包括負額加額（回收）
- 若回收導致負餘額，平台允許並鎖定帳戶，觸發風控警報
- 人工恢復流程：凍結提款、聯繫玩家、安排分期回收

### 4.8 Jackpot 與大獎（Jackpot & Big Win）（場景 I）

- 當玩家贏得 > $10,000 或中累積 Jackpot 時觸發
- **人工審批模式**：GP 發送 NotifyWin；資金暫扣待人工審核後加額
- **自動加額模式**：資金立即加額，帳戶凍結，觸發風控審核
- 協議模式依 GP 協議而定

---

## 5. 促銷與獎金規則（Promotion & Bonus Rules）

### 5.1 Free Spin / Free Round（場景 G）

- GP 通知平台 Bet 金額為 0，實際 Win 金額不為零
- 平台必須接受 Amount=0 交易
- 平台必須在接受前驗證玩家的 Free Spin 配額
- 若配額耗盡，拒絕並返回「Free Spin 配額已耗盡」

### 5.2 Bonus Wallet（獎金錢包）（場景 H）

- 玩家可能同時擁有 Cash 錢包和 Bonus 錢包（例如：Cash 100 + Bonus 50 僅限老虎機）
- 當遊戲不支援 Bonus 遊玩時，僅使用 Cash 錢包
- 若 Cash 餘額不足（例如：下注 120 但 Cash 僅 100），即使總資產為 150，平台仍返回餘額不足
- 當遊戲支援 Bonus 遊玩時，應用雙錢包扣款（Cash 優先，不足時使用 Bonus）

---

## 6. 錢包扣款順序政策（Wallet Deduction Order Policy）

### 6.1 預設扣款順序（Default Deduction Sequence）

系統預設扣款順序：**Bonus → Cash → Credit**

### 6.2 遊戲層級覆寫（Game-Level Override）

- 每個遊戲或 GP 可配置自訂 `wallet_priority` 以覆寫系統預設
- 配置必須明確標記是否覆寫預設

### 6.3 扣款執行規則（Deduction Execution Rules）

1. 接收 Bet 請求
2. 檢查遊戲是否有自訂 wallet_priority 配置
3. 若自訂配置存在且啟用覆寫，使用遊戲配置
4. 若無自訂配置，使用系統預設
5. 按優先級順序扣除餘額
6. 允許混合扣款（第一優先錢包不足時，記錄拆分交易明細）

---

## 7. 負餘額處理政策（Negative Balance Handling Policy）

### 7.1 決策（Decision）

允許負餘額，自動鎖定並人工介入。

### 7.2 觸發場景（Trigger Scenarios）

GP 發起的 Rollback 或 Resettlement，扣除金額超過玩家當前餘額。

### 7.3 處理程序（Handling Procedure）

1. 強制執行扣款，設定餘額為負
2. 立即將帳戶狀態改為 LOCKED 或 SUSPENDED
3. 阻止所有新登入、下注和提款
4. 向風控團隊發送高優先級警報
5. 僅具有 RISK_MANAGER 權限的管理員可解鎖帳戶

---

## 8. 身份驗證與 Session 政策（Authentication & Session Policy）

### 8.1 Session 過期規則（Session Expiry Rules）

| 請求類型 | 玩家 Token 過期 | 業務規則 |
|-------------|---------------------|--------------|
| **Bet** | 拒絕（錯誤：Token Expired） | 玩家無法在 Session 過期後發起新下注 |
| **Win** | **必須接受** | Bet 發生於有效 Session 期間；獎金為玩家合法資產，不受結算時間影響 |

---

## 9. 貨幣與匯兌政策（Currency & Exchange Policy）

### 9.1 精度要求（Precision Requirement）

- 所有貨幣計算必須支援 4 位小數
- 必須防止 0.0001 級別累積差異產生的捨入誤差

### 9.2 多幣種政策（Multi-Currency Policy）

- 若 GP 不支援玩家幣種，需中間匯兌層
- **建議**：配置 GP 直接使用玩家幣種運營，避免匯兌風險
- 單位轉換（例如：美分 vs. 美元）必須按 GP 整合明確定義

---

## 10. 對帳要求（Reconciliation Requirements）

### 10.1 每日對帳（Daily Reconciliation）

- 平台必須每日下載各 GP 的交易報告（Transaction Report）並與平台記錄比對
- 自動化差異報告（Diff Reports）必須生成：不匹配的交易 ID、不匹配的金額

### 10.2 極端案例政策（Extreme Case Policy）

即使有重試，邊緣案例仍可能發生 GP 認為交易成功而平台認為失敗（例如：成功響應後內部處理失敗）。每日對帳是這些場景的安全網。

---

## 11. 場景優先級矩陣（Scenario Priority Matrix）

| 場景 | 檢測點 | 優先級 | 響應時間 | 風險級別 | 自動化水平 |
|----------|----------------|----------|--------------|------------|-----------------|
| A - 餘額不足 | Bet Request | P0 | < 100ms | LOW | 100% 自動化 |
| B - 並發競爭 | Bet Request | P0 | < 200ms | MEDIUM | 100% 自動化（並發控制） |
| C - 超時重試 | Win Timeout | P1 | 2 hours | MEDIUM | 90% 自動化（主動查詢） |
| D - 亂序 | Win Request | P1 | < 2 hours | MEDIUM | 95% 自動化（臨時存儲） |
| E - 回滾 / 退款 | Rollback Request | P1 | < 500ms | MEDIUM | 100% 自動化 |
| F - 重新結算 | Adjust Request | P2 | < 1s | HIGH | 50% 自動化（負餘額需人工） |
| G - Free Spin | Bet Request | P0 | < 100ms | LOW | 100% 自動化 |
| H - Bonus Wallet | Bet Request | P0 | < 150ms | LOW | 100% 自動化 |
| I - Jackpot | Win Request | P0 | 人工審核 | HIGH | 0-50% 自動化（依 GP 協議） |

→ **[並發控制與佇列管理](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#concurrency-management)** - 分散式鎖（Redisson）、待處理佇列實現

---

## 12. 異常處理決策表（Exception Handling Decision Table）

| 異常類型 | 檢測方法 | 降級策略 | 補償機制 | 人工介入閾值 |
|---------------|-----------------|---------------------|----------------------|------------------------------|
| 鎖超時 | 3 次重試失敗 | 返回「系統繁忙」 | 玩家重試 | 鎖等待超過 10s |
| GP 超時 | 2 小時無響應 | 主動查詢 GP | 補償性加額或退款 | 查詢連續 3 次失敗 |
| 亂序積壓 | 臨時存儲超過 100 | 警報 + 擴容 | 延長保留至 4 小時 | 存儲超過 500 |
| 負餘額 | 扣款後餘額 < 0 | 允許負餘額 | 凍結提款 + 人工回收 | 負餘額 < -$1,000 |
| Jackpot 異常 | 金額超過 $50k | 強制人工審核 | 暫扣加額 | 所有 Jackpot |
| 交易歷史不可用 | 存儲結果過期 | 查詢交易記錄 | 重建存儲結果（1 小時保留） | 系統無記錄 |

→ **[異常處理實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#exception-handling)** - Redis 緩存過期、資料庫 Fallback 查詢、待處理佇列擴容、鎖超時處理

---

## 13. 策略切換政策（Strategy Switching Policy）（亂序處理）

### 13.1 降級觸發器（Degradation Triggers）

系統根據運營條件自動切換亂序處理策略：

| 觸發條件 | 切換至 | 恢復條件 |
|------------------|-----------|-------------------|
| 臨時存儲積壓超過閾值 | 策略 3 → 策略 1 | 積壓恢復正常 10 分鐘 |
| 存儲服務不可用 | 策略 3 → 策略 1 | 服務恢復 + 5 分鐘穩定 |
| 交易處理時間過長 | 策略 3 → 策略 1 | 處理時間正常化 10 分鐘 |
| 系統資源壓力過高 | 策略 3 → 策略 1 | 資源使用正常化 10 分鐘 |
| GP 重試率過低 | 策略 1 → 策略 2（緊急） | 僅人工恢復 |

→ **[策略切換實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#strategy-switching)** - 具體閾值（Redis 連線、DB 延遲 P99、CPU 使用率、佇列大小）、監控指標、降級自動化

### 13.2 實施指南（Implementation Guidelines）

1. **預設策略**：策略 3（臨時存儲）
2. **自動降級**：系統壓力超過閾值時，降級至策略 1
3. **緊急模式**：GP 重試率 < 80% 時，切換至策略 2（需人工恢復）
4. **自動恢復**：系統指標穩定後，自動升級至策略 3
5. **警報**：所有策略切換必須觸發警報和審計日誌條目

---

## 14. 監控 KPI（Monitoring KPIs）

| KPI | 公式 | 目標 |
|-----|---------|--------|
| 亂序頻率 | (pending_wins_count / total_wins) x 100% | < 0.1% |
| 待處理交易數 | 即時監控 | 警報閾值 > 100 |
| 超時升級率 | (escalated_to_manual / pending_wins) x 100% | < 1% |
| 餘額不足拒絕率 | (rejected_insufficient / total_bets) x 100% | 警報若 > 30% |
| 負餘額累積 | sum(player_negative_balance) | 警報若 < -$10,000 |
| Jackpot 頻率 | jackpot_win_count per hour | 警報若 > 5 per hour |

---

## 15. 冪等性要求（Idempotency Requirements）

### 15.1 強制 TransactionId（Mandatory TransactionId）

- 所有 GP 整合**必須**在請求標頭或主體中提供全局唯一的 `transaction_id`（或 `request_id`）
- 無 TransactionId 的請求將被拒絕，錯誤訊息指示缺少必填欄位

→ **[API 錯誤響應碼](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#error-responses)**

### 15.2 重複處理（Duplicate Handling）

- 重複的 TransactionId 必須返回先前存儲的響應
- 業務邏輯對相同 TransactionId 絕不執行兩次
- 響應保留：1 小時，所有錢包模組統一

→ **[冪等性存儲實現](../../architecture/02_Finance_Service/Seamless_Wallet_Technical.md#idempotency-storage)** - Redis 緩存、TTL 配置（3,600 秒）、Fallback 機制

---

## 16. 驗收標準（Acceptance Criteria）

- [ ] 所有 GP 整合每個請求都需要全局唯一的 `transaction_id`
- [ ] 重複的 TransactionId 返回存儲響應，不重新執行
- [ ] 餘額不足在 100ms 響應時間內返回錯誤（場景 A）
- [ ] 同一玩家的並發下注請求原子化處理，無超額扣款
- [ ] 孤兒 Round 在 15 分鐘檢查週期內檢測，2 小時後升級
- [ ] 亂序 Win 請求臨時存儲，30 分鐘過期（策略 3）
- [ ] 重新結算支援負餘額，自動鎖定帳戶並觸發風控警報
- [ ] Jackpot 獎金 > $10,000 觸發人工審批或自動凍結（依 GP 協議）
- [ ] 每日對帳比對平台記錄與 GP 交易報告
- [ ] 亂序頻率維持 < 0.1%，超時升級率 < 1%

---

## 17. 相關文檔（Related Documents）

### 核心依賴（Core Dependencies）
- Unified Wallet Model - 錢包餘額更新、鎖定機制
- Game Integration Standard - GP API 規格、安全設計

### 業務整合（Business Integration）
- Turnover Calculation & Reconciliation - 遊戲對帳、下注驗證
- Risk Framework - 異常下注檢測、負餘額警報

### 延伸閱讀（Extended Reading）
- Game Lobby Management - 遊戲入口管理
- Maintenance Procedures - 遊戲維護與餘額同步

### 技術實現（Technical Implementation）

→ **[Seamless Wallet 技術架構](../../architecture/02_Finance_Service/Seamless_Wallet_Analysis.md)** - Round-based 狀態機、孤兒 Round 檢測、冪等性防禦層、並發處理模式、負餘額處理、異常恢復策略
