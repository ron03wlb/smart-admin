# 遊戲整合標準（Game Integration Standards）

> **Canonical Source**: [03-01_Game_Integration_Standard.md](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md)
> **Audience**: 高階主管、產品經理、營運主管、合規官員
> **Related Architecture**: [Game Integration Protocols (Architecture)](../../architecture/03_Game_Integration/Game_Integration_Protocols.md)
> **Last Synced**: 2026-02-09
>
> **精煉說明**：技術細節（HMAC-SHA256、TLS、Type A/B/C 分類、HTTP 狀態碼）已移至架構層。本文件僅專注於業務需求。

---

## 業務價值（Business Value）

此遊戲整合標準提供關鍵業務價值：
- **玩家體驗**：無縫錢包架構消除遊戲錢包間的手動資金轉移，減少摩擦並提升留存率
- **風險保護**：RTP 自動暫停防止因遊戲供應商錯誤或賠率配置錯誤（例如必贏漏洞）造成的災難性損失
- **上市時間**：快速上線流程（< 5 個工作日）實現快速擴充遊戲目錄和競爭性遊戲組合
- **財務安全**：累積獎金驗證工作流程正確區分 GP 資助贏額與商戶資助贏額，防止商戶破產

---

## 驗收標準（Acceptance Criteria）

- [ ] 所有遊戲供應商整合通過認證檢查清單（API 合規、邊緣案例處理、幣別正規化、累積獎金處理）
- [ ] 熔斷器在 5 分鐘窗口內 RTP > 200% 且淨損失 > $10,000 時自動暫停遊戲
- [ ] API 正常運作時間達到 ≥ 99.9% SLA 目標，交易延遲 < 200ms P95
- [ ] 累積獎金贏額被正確識別並與商戶配額隔離（網路累積獎金不從商戶餘額扣除）
- [ ] 新遊戲供應商在 GP 認證後 5 個工作日內完成上線
- [ ] 重複交易 ID 回傳成功而不重新處理（冪等性保證）
- [ ] API 逾時場景將交易標記為「Pending」並透過 QueryStatus 查詢最終狀態（禁止直接回滾）
- [ ] 熔斷期間的玩家體驗：遊戲中玩家看到維護訊息，大廳玩家看到灰色遊戲圖示

---

## 1. 概述（Overview）

平台使用**無縫錢包（Seamless Wallet / Single Wallet）**架構與外部遊戲供應商（GP, Game Provider）整合。玩家可以存取任何整合的遊戲，無需在錢包之間手動轉移資金。本文件定義所有遊戲供應商整合的合作夥伴標準、認證要求和 SLA 期望。

---

## 2. 供應商合作夥伴標準（Provider Partnership Standards）

### 2.1 通訊協定要求

所有遊戲供應商整合必須滿足以下通訊標準：

| 需求 | 標準 |
|-------------|----------|
| API 格式 | 行業標準 RESTful API，使用 JSON 負載 |
| 傳輸安全 | 強制執行加密傳輸 |
| 網路存取控制 | IP 白名單強制執行 |
| 請求身份驗證 | 密碼學簽章驗證 |

→ **[技術實作細節](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#1-integration-architecture-overview)** - HTTPS/TLS 配置、HMAC-SHA256 簽章演算法

### 2.2 核心 API 合約

平台公開以下介面供遊戲供應商調用：

| API 端點 | 目的 | 關鍵需求 |
|--------------|---------|-----------------|
| **GetBalance** | 查詢玩家當前餘額 | 即時餘額準確性 |
| **Transaction (Bet/Win)** | 處理投注和派彩 | 保證一致性和防止重複 |
| **CheckToken** | 驗證玩家登入 Token | 會話有效性確認 |

**交易需求**：
- 投注和派彩必須一致處理並支援回滾
- 具有相同交易 ID 的重複請求絕不能導致重複扣款

→ **[交易保證](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#22-transaction-betwin)** - 原子性實作、冪等性機制

### 2.3 遊戲啟動流程

遊戲啟動流程遵循定義的順序：
1. 前端向平台後端請求啟動 URL
2. 後端從遊戲供應商取得 Token 和 URL
3. 前端透過 iframe 嵌入遊戲或開啟新視窗

**必要的啟動參數**：玩家 Token、語言、幣別和大廳返回 URL。

---

## 3. 無縫錢包邊緣案例處理（Seamless Wallet Edge Case Handling）

每個供應商整合必須為以下場景制定記錄的解決程序：

| 場景 | 說明 | 必要的解決方式 |
|----------|-------------|---------------------|
| **API 逾時** | 平台扣款成功但 GP 回應逾時 | 標記為「Pending」並透過 QueryStatus 查詢最終狀態。嚴格禁止直接回滾。 |
| **回滾 / 取消** | GP 發起投注取消（系統錯誤或事件取消） | 如果餘額足夠則退款；如果資金已提領則允許負餘額，標記為人工恢復。 |
| **競態條件** | 玩家發送並發投注請求 | 使用樂觀鎖定防止餘額變為負數。 |
| **冪等性** | GP 重試發送相同 webhook | 對於重複交易 ID 回傳成功而不重新處理。 |

---

## 4. 供應商適配需求（Provider Adaptation Requirements）

平台必須支援與使用不同 API 風格和資料格式的多個遊戲供應商整合。

### 資料正規化需求

- 所有供應商遊戲類型必須映射到平台標準類別：Live、Slot、Sport
- 所有幣別單位必須統一（例如，如果供應商使用分，平台轉換為基礎幣別單位）

→ **[供應商適配層](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#4-provider-adaptor-layer)** - Type A/B/C 技術分類、中間件適配策略

---

## 5. 動態配置和審批工作流程（Dynamic Configuration and Approval Workflows）

### 5.1 緊急供應商維護

當遊戲供應商遇到需要立即斷線的嚴重錯誤時：

| 步驟 | 執行者 | 動作 |
|------|-------|--------|
| 1 | 營運工程師 | 發起「GP 維護」請求 |
| 2 | CTO | 核准請求 |
| 3 | 系統 | 全平台隱藏該供應商的所有入口點 |

### 5.2 投注限額調整

- 投注限額（最小/最大）依幣別和商戶配置
- 所有投注限額變更需要風險管理部門核准

---

## 6. 累積獎金處理需求（Jackpot Handling Requirements）

### 6.1 風險場景

網路累積獎金（例如累積獎金池 USD 10M）由遊戲供應商資助，而非商戶。平台必須區分正常贏額（商戶資助）和累積獎金贏額（GP 資助），以防止商戶破產。

### 6.2 贏額類型分類

| 贏額類型 | 資金來源 | 餘額影響 |
|----------|---------------|----------------|
| **正常贏額** | 商戶 | 直接更新玩家餘額 |
| **累積獎金贏額（網路）** | 遊戲供應商 | 不從商戶配額扣除 |
| **累積獎金贏額（本地）** | 依協議而異 | 依供應商合約條款 |

### 6.3 累積獎金驗證流程

| 步驟 | 動作 | 責任方 |
|------|--------|-------------------|
| 1 | 識別累積獎金交易 | 系統（自動） |
| 2 | 在累積獎金錢包中凍結資金 | 系統（自動） |
| 3 | 警告風控和財務團隊 | 系統（自動通知） |
| 4 | 等待 GP 官方累積獎金驗證報告 | 遊戲供應商 |
| 5 | 確認 GP 資金轉入平台 | 財務團隊 |
| 6 | 解凍並入帳至玩家餘額 | 財務團隊（人工釋放） |

---

## 7. RTP 自動暫停 - 業務規則（RTP Automatic Suspension - Business Rules）

為防止 GP 錯誤（例如必贏漏洞）或錯誤賠率導致平台快速損失，整合層實作即時自動遊戲暫停。

### 7.1 監控指標

系統使用 5 分鐘滑動窗口監控每個遊戲供應商和遊戲 ID：

| 指標 | 定義 |
|--------|------------|
| 總投注額（Total Bet） | 窗口內所有投注總和 |
| 總贏額（Total Win） | 窗口內所有派彩總和 |
| RTP（Return To Player） | 總贏額 / 總投注額 x 100% |
| 淨損失（Net Loss） | 總贏額 - 總投注額 |

### 7.2 自動暫停門檻

| 警報級別 | 觸發條件（5 分鐘窗口） | 自動化動作 | 恢復方法 |
|-------------|----------------------------------|-------------------|-----------------|
| **警告** | RTP > 120% 且淨損失 > $5,000 | 發送警報至風控團隊（Slack/Telegram） | 自動（如果下一期正常化） |
| **關鍵** | RTP > 200% 且淨損失 > $10,000 | 自動停用遊戲/供應商 | 人工：需要 CTO/風控總監確認才能解鎖 |

→ **[暫停實作](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#6-rtp-circuit-breaker-implementation)** - 技術實作細節請見架構層

### 7.3 人工恢復流程

在關鍵自動暫停事件後：

| 步驟 | 動作 | 責任方 |
|------|--------|-------------------|
| 1 | 分析遊戲日誌以確定是錯誤還是玩家運氣 | 風控團隊 |
| 2 | 聯繫 GP 確認已知漏洞 | 整合團隊 |
| 3a | 如果誤報：管理員透過後台重置並恢復 | 營運 |
| 3b | 如果確認錯誤：維持阻擋直到 GP 提供修復和補丁說明 | 整合團隊 |

### 7.4 熔斷期間的玩家體驗

**遊戲中的玩家**：
- 下一個投注請求回傳服務不可用回應
- 前端顯示：「遊戲暫時維護中，請稍後再試」
- 餘額自動同步回主錢包

**大廳中的玩家**：
- 受影響的遊戲圖示變為灰色並標記「維護」
- 點擊顯示維護公告

---

## 8. SLA 期望（SLA Expectations）

### 8.1 供應商整合 SLA

| 指標 | 目標 |
|--------|--------|
| API 正常運作時間 | 99.9% |
| 交易處理延遲 | < 200ms P95 |
| 遊戲啟動成功率 | > 99.5% |
| Webhook 傳遞保證 | 至少一次並防止重複 |
| 新遊戲上線時間 | < 5 個工作日（GP 認證後） |

### 8.2 認證要求

遊戲供應商上線前，必須驗證以下項目：
- API 合約合規（所有核心端點功能正常）
- 邊緣案例處理（逾時、回滾、防止重複）
- 幣別和遊戲類型正規化
- 累積獎金交易處理（如適用）
- 熔斷器整合測試

→ **[技術認證檢查清單](../../architecture/03_Game_Integration/Game_Integration_Protocols.md)** - 詳細測試程序和驗證標準

---

## 相關文件（Related Documents）

### 核心依賴
- [Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) - 遊戲錢包轉帳邏輯
- [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP 邊緣案例處理

### 業務整合
- [Turnover and Reconciliation](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 遊戲對帳和投注額計算
- [Game Lobby Management](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md) - 遊戲元資料同步和大廳配置
- [Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) - 遊戲風險偵測和熔斷器

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: 遊戲整合團隊
