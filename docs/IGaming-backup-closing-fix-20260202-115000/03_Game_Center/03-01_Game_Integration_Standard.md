# 03-01 遊戲整合標準 (Game Integration Standard)

## 1. 系統概述
定義平台與外部遊戲供應商 (Game Provider - GP) 的標準對接協議。
主流採用 **單一錢包 (Seamless Wallet/Single Wallet)** 架構，確保玩佳無需手動轉帳即可遊玩。

## 2. 整合架構

### 2.1 對接協議 (Integration Protocol)
- **API 通訊**：RESTful API (JSON Body)
- **安全性**：
  - HTTPS 強制
  - IP 白名單限制
  - 簽名驗證 (HMAC-SHA256)

### 2.2 核心 API 接口 (Core APIs)
平台需提供以下接口供 GP 調用：
1. **GetBalance**：查詢玩家當前餘額。
2. **Transaction (Bet/Win)**：
   - **原子性 (Atomicity)**：Bet 與 Win 必須在同一事務中處理 (針對部分 GP)，或支援回滾 (Rollback)。
   - **冪等性 (Idempotency)**：同一 transaction_id 重複請求不可重複扣款。
3. **CheckToken**：驗證玩家登入 Token 有效性。

### 2.3 遊戲啟動 (Game Launch)
- **流程**：前端請求 Launch URL -> 後端向 GP 獲取 Token 與 URL -> 前端 iframe 嵌入或新視窗打開。
- **參數**：需包含 `token`, `language`, `currency`, `lobby_url` (返回大廳)。

## 3. 無縫錢包極端場景矩陣 (Seamless Wallet Edge Cases)

| 場景 | 描述 | 處置策略 |
|---|---|---|
| **API Timeout** | 平台扣款成功但 GP 回傳超時 | **Pending 機制**：標記為 "Pending"，發起 QueryStatus 查詢最終狀態。嚴禁直接回滾。 |
| **Rollback / Cancel** | GP 發起取消注單 (因系統錯誤或賽事取消) | **餘額檢查**：若玩家餘額足夠則退款；若餘額不足 (已提款)，允許 **負餘額 (Negative Balance)** 並標記為異常，人工追討。 |
| **Race Condition** | 用戶同時發送多筆 Bet 請求 (並發攻擊) | **樂觀鎖 (Optimistic Locking)**：使用 `version` 欄位或 `Redis Lua` 腳本扣款，確保餘額不被扣成負數。 |
| **Idempotency** | GP 重試發送相同的 Webhook | **唯一鍵約束**：以 `transaction_id` 為 Key，若已存在則直接返回 Success，不重複扣款。 |

## 4. 對接適配層 (Provider Adaptor Layer)
由於不同 GP 的 API 風格迥異，需建立統一適配層 (Middleware)：

- **風格差異處理**：
  - **Type A (PG-like)**：TransferWallet (單一接口含 Bet & Win)。 -> **適配**：直接處理。
  - **Type B (Evo-like)**：Debit (Bet) 與 Credit (Win) 分離。 -> **適配**：需維護 Round 狀態。
  - **Type C (Seamless)**：僅提供 GetBalance，變更由 GP 端發起。 -> **適配**：需實作 Webhook 接收。

- **數據標準化 (Normalization)**：
  - 將所有 GP 的 `GameType` 映射為平台標準 (Live, Slot, Sport)。
  - 將所有貨幣單位統一 (如 GP 單位為分，平台單位為元)。

## 5. 動態配置與審批 (Dynamic Config & Approval)
- **GP 維護審批**：
  - **場景**：某 GP 瘋狂報錯，需緊急切斷。
  - **流程**：運維工程師發起 "GP 維護" -> CTO 批准 -> 全平台該 GP 入口隱藏。
- **限紅調整 (Bet Limits)**：
  - **配置**：針對不同幣種/商戶設定 Min/Max Bet。
  - **審批**：修改限紅需經由風險管理部審核。

## 6. 特殊獎項處理 (Jackpot Handling)

### 6.1 風險場景
若玩家在小型商戶中得 **Network Jackpot** (如累積彩池 $10M)，直接扣除商戶餘額會導致商戶瞬間破產。實際上，Network Jackpot 應由 GP (遊戲供應商) 支付。

### 6.2 交易協議擴展
Transaction API 需區分獎金類型：

- **Normal Win**: 商戶承擔，直接 update balance。
- **Jackpot Win**: GP 承擔，不扣商戶額度。

**Payload 範例**：
```json
{
  "transaction_id": "tx_123",
  "type": "WIN",
  "amount": 1000000.00,
  "is_jackpot": true,
  "jackpot_type": "NETWORK", // 或 LOCAL
  "currency": "USD"
}
```markdown

### 6.3 處理流程
1. **識別 Jackpot**：收到 `is_jackpot: true`。
2. **資金凍結**：不直接加到玩家 `Cash Wallet`，而是寫入 `Jackpot Wallet (Frozen)`。
3. **人工核驗**：
   - 觸發 Alert 通知風控與財務。
   - 等待 GP 發送官方 `Jackpot Verification Report`。
4. **派發**：
   - 確認 GP 已將資金轉撥給平台。
   - 解凍資金，轉入玩家餘額。

## 7. 業務熔斷機制 (RTP Circuit Breaker)

為了防範 GP 端出現 Bug (如：必中 Bug) 或 賠率設定錯誤導致平台資金快速流失，需在 **Integration Adaptor** 層實作即時熔斷。

### 7.1 監控指標 (Metrics)
系統對每個 `GP (Provider)` 和 `GameID` 進行 5 分鐘滑動窗口監控：
- **Total Bet**: 總投注額
- **Total Win**: 總派彩額
- **RTP (Return To Player)**: `Total Win / Total Bet * 100%`
- **Net Loss**: `Total Win - Total Bet`

### 7.2 觸發條件 (Thresholds)
| 等級 | 條件 (5min window) | 動作 (Action) | 解除方式 |
|---|---|---|---|
| **Warning** | RTP > 120% AND Net Loss > $5,000 | 發送 Alert 至 Slack/Telegram 風控群組 | 自動 (下一週期若正常) |
| **Critical** | RTP > 200% AND Net Loss > $10,000 | **自動停用** 該遊戲/該 GP (HTTP 503) | **手動**：需 CTO/風控主管確認後解鎖 |

### 7.3 實作邏輯


### 7.4 人工介入與恢復 (Manual Recovery)
熔斷觸發後，必須經過嚴格的人工審核流程才能恢復：
1.  **事故調查**：風控團隊分析 `Game Logs`，確認是否為 Bug 或單純玩家運氣好。
2.  **供應商確認**：聯繫 GP 確認該遊戲是否存在已知漏洞。
3.  **解鎖操作**：
    - 若為誤判：Admin 後台點擊 "Reset & Resume" 解除封鎖。
    - 若確有 Bug：保持封鎖，直到 GP 修復並提供 Patch Note。

### 7.5 告警 Payload 範例 (Alert Payload)
當熔斷觸發時，系統發送至 Slack/Telegram 的 JSON 結構：
```json
{
  "alert_level": "CRITICAL",
  "event": "CIRCUIT_BREAKER_TRIGGERED",
  "data": {
    "provider": "PGSoft",
    "game_id": "mahjong-ways-2",
    "window_minutes": 5,
    "total_bet": 15000.00,
    "total_win": 45000.00,
    "rtp": 300.0,
    "net_loss": 30000.00
  },
  "action_taken": "AUTO_DISABLE_GAME",
  "timestamp": "2026-01-27T12:00:00Z"
}
```

### 7.6 玩家體驗處理 (User Experience)
當遊戲被熔斷或平台進入維護模式 (參見 `12-04_Maintenance_Procedure`) 時：
- **正在遊玩者**：
  - 下一次 Spin/Bet 請求返回 `HTTP 503 Service Unavailable`。
  - 前端彈窗提示：「遊戲臨時維護中，請稍後再試 (Error: G-503)」。
  - 餘額自動同步回主錢包。
- **在大廳者**：
  - 該遊戲 Icon 變灰或添加 "維護中" 標籤。
  - 點擊時彈出維護公告。

---

## 📚 相關文檔

### 核心依賴
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 遊戲錢包轉入轉出邏輯
- [03-03 無縫錢包對接分析](./03-03_Seamless_Wallet_Analysis.md) - GP 對接極端場景處理

### 業務整合
- [02-04 流水計算與對帳](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 遊戲對帳、流水計算
- [03-02 遊戲大廳管理](./03-02_Game_Lobby_Management.md) - 遊戲元數據同步、大廳配置
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 遊戲風控檢測、熔斷機制

### 技術參考
- [12-03 網關架構](../12_Technical_Operations/12-03_Gateway_Architecture.md) - API 安全、HMAC 簽名驗證
- [12-04 維護程序](../12_Technical_Operations/12-04_Maintenance_Procedure.md) - 遊戲維護流程

### 延伸閱讀
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 敏感數據加密規範
- [07-03 通知架構](../07_Platform_Management/07-03_Notification_Architecture.md) - 遊戲維護通知

---

**最後更新**: 2026-01-27
**維護團隊**: Game Integration Team

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Integration Team & Backend Team
