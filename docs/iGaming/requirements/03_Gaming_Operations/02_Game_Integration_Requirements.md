# 遊戲整合需求（Game Integration Requirements）

> **Canonical Source**: [source-archive/00_Foundation/guides/00-12_Game_Integration_Implementation.md](../../source-archive/00_Foundation/guides/00-12_Game_Integration_Implementation.md)
> **Audience**: 高階主管、產品經理
> **Related Architecture**: [Game_Integration_Security.md](../../architecture/03_Game_Integration/03_Game_Integration_Security.md)
> **Last Synced**: 2026-02-09
>
> **精煉說明**：技術細節（Token 生成 Java 程式碼、HMAC-SHA256 簽章演算法、Base64 編碼、Redis 防重放黑名單、三層冪等防禦、停滯交易恢復作業、速率限制 Redisson 實作）已移至架構層。本文件僅專注於業務需求。

---

## 業務價值（Business Value）

此遊戲供應商整合框架提供關鍵價值：

- **加速市場進入**：標準化的無縫錢包 API 和基於 Token 的身份驗證能快速上線新遊戲供應商，縮短新遊戲內容的上市時間並提升競爭力
- **保護收入完整性**：三層冪等防禦防止網路重試期間的重複扣款/入帳，自動錯誤恢復在 10 分鐘內解決停滯交易，完整審計追蹤確保零未記錄交易
- **建立玩家信任**：所有遊戲供應商的單一統一錢包消除玩家資金轉移摩擦，即時餘額同步確保準確的「可下注餘額（Playable Balance）」顯示，失敗交易的自動退款保持玩家信心
- **最小化安全風險**：基於 Token 的身份驗證（5 分鐘有效期）和防重放保護防止未授權遊戲存取，IP 白名單和 TLS 1.2+ 強制執行阻擋惡意回呼，速率限制保護 API 免於濫用

---

## 1. 概述（Overview）

本文件定義將新遊戲供應商（GP, Game Provider）整合到 iGaming 平台的業務需求。涵蓋必須支援的核心功能區域、合作夥伴標準，以及每個整合里程碑的驗收標準。

---

## 2. 核心整合能力（Core Integration Capabilities）

平台在上線新遊戲供應商時必須支援以下能力：

| 能力 | 說明 | 業務優先級 |
|-----------|-------------|-------------------|
| Seamless Wallet API（無縫錢包 API） | 統一錢包，允許玩家在任何 GP 的所有遊戲中使用單一餘額 | 關鍵 |
| Token-Based Authentication（基於 Token 的身份驗證） | 平台與 GP 系統之間的安全會話驗證 | 關鍵 |
| Idempotent Transaction Processing（冪等交易處理） | 保證重複請求（例如網路重試）不會導致重複扣款或入帳 | 關鍵 |
| Transaction Cancellation（交易取消） | 在 GP 請求時能夠撤銷已完成的投注交易 | 高 |
| Error Recovery（錯誤恢復） | 自動偵測和解決停滯或不一致的交易 | 高 |
| Real-Time Balance Query（即時餘額查詢） | GP 可隨時查詢玩家當前的可下注餘額 | 關鍵 |

---

## 3. 無縫錢包功能需求（Seamless Wallet Functional Requirements）

### 3.1 支援的操作

無縫錢包整合必須支援四個主要操作：

| 操作 | 業務規則 | 預期結果 |
|-----------|--------------|------------------|
| **Debit（扣款 - 下注）** | 當下注時從玩家錢包扣除投注金額 | 玩家餘額減少；交易被記錄 |
| **Credit（入帳 - 派彩）** | 當回合結算時將贏額加入玩家錢包 | 玩家餘額增加；交易被記錄 |
| **Cancel（取消 - 交易撤銷）** | 根據 GP 請求撤銷先前完成的扣款或入帳 | 原始交易被撤銷；餘額恢復 |
| **Balance Inquiry（餘額查詢）** | 回傳玩家當前的可下注餘額 | GP 接收最新餘額以供顯示 |

### 3.2 交易完整性規則

| 規則 | 說明 |
|------|-------------|
| 單次使用請求識別碼 | 來自 GP 的每個交易請求必須攜帶唯一的請求 ID |
| 冪等回應 | 使用相同請求 ID 的重複請求必須回傳相同結果而不重新處理 |
| 原子性餘額更新 | 每次扣款或入帳必須完全成功或完全失敗；不允許部分更新 |
| 審計追蹤 | 每筆交易必須記錄時間戳記、玩家 ID、GP ID、回合 ID 和金額 |

---

## 4. 安全需求（Security Requirements）

### 4.1 Token 身份驗證標準

| 需求 | 規格 |
|------------|---------------|
| Token 組成 | 必須包含玩家 ID、租戶 ID、時間戳記和密碼學簽章 |
| 簽章演算法 | 行業標準密碼學簽章 |
| Token 有效期窗口 | 最長 5 分鐘（可配置） |
| 防重放保護 | 每個 Token 必須單次使用；已使用的 Token 必須被拒絕 |
| 編碼 | 用於傳輸的安全編碼 |

→ **[Token 身份驗證實作](../../architecture/03_Game_Integration/03_Game_Integration_Security.md#token-authentication)** - HMAC-SHA256 演算法、Base64 編碼、Java generateToken/validateToken 方法

### 4.2 GP 上線安全檢查清單

| 項目 | 需求 |
|------|------------|
| IP 白名單 | GP 回呼端點必須在 IP 白名單中 |
| HTTPS 強制執行 | 所有 API 通訊必須使用 TLS 1.2 或更高版本 |
| API 金鑰輪換 | GP API 金鑰必須支援定期輪換而不停機 |
| 速率限制 | API 端點必須對每個 GP 執行速率限制以防止濫用 |

→ **[安全實作細節](../../architecture/03_Game_Integration/03_Game_Integration_Security.md#security-checklist)** - IP 白名單配置、TLS 1.2+ 設定、速率限制（Redisson）、Redis 防重放黑名單

---

## 5. 錯誤恢復需求（Error Recovery Requirements）

### 5.1 停滯交易政策

| 場景 | 業務規則 | 最大解決時間 |
|----------|--------------|------------------------|
| 扣款期間網路逾時 | 系統必須自動偵測和恢復停滯交易 | 10 分鐘 |
| GP 回呼失敗 | 系統必須重試或查詢 GP 以取得權威狀態 | 10 分鐘 |
| 不一致狀態（扣款已記錄，GP 說失敗） | 系統必須自動退款給玩家 | 10 分鐘 |
| GP 端未知交易 | 系統必須視為失敗並退款給玩家 | 10 分鐘 |

### 5.2 恢復驗證

每次自動恢復後，系統必須：
- 在審計日誌中記錄恢復動作
- 如果交易狀態為 FAILED 或 NOT_FOUND，發送警報通知
- 確保玩家餘額與解決的狀態一致

→ **[停滯交易自動恢復](../../architecture/03_Game_Integration/03_Game_Integration_Security.md#auto-recovery)** - 排程作業配置、SQL 查詢、GP 狀態查詢 API 整合、退款自動化

---

## 6. 規劃功能（階段 5+）（Planned Capabilities - Phase 5+）

### 6.1 Seamless Wallet API 進階功能

| 功能 | 狀態 | 說明 |
|---------|--------|-------------|
| Free Spin Support（免費旋轉支援） | 規劃中 | GP 發起的免費旋轉，由平台資助的紅利餘額 |
| Jackpot Contribution（累積獎金貢獻） | 規劃中 | 每筆投注扣除累積獎金貢獻 |
| Multi-Currency Support（多幣別支援） | 規劃中 | 單一錢包支援多種幣別投注並即時轉換 |

### 6.2 投注額計算邏輯

| 功能 | 狀態 | 說明 |
|---------|--------|-------------|
| Three-Layer Validation（三層驗證） | 規劃中 | Layer 1（風控）、Layer 2（財務）、Layer 3（活動）投注額驗證 |
| Game Weight Configuration（遊戲權重配置） | 規劃中 | 不同遊戲類型對投注要求的貢獻率不同 |
| Free Spin Turnover Exclusion（免費旋轉投注額排除） | 規劃中 | 免費旋轉投注通常不計入投注要求計算 |

---

## 7. 驗收標準（Acceptance Criteria）

### 7.1 整合驗證檢查清單

| 標準 | 驗證方法 |
|-----------|------------------|
| Token 身份驗證正確運作（簽章、過期、防重放） | 自動化測試套件 |
| 冪等性已驗證（重複請求回傳相同結果） | 注入重複請求的負載測試 |
| 並發性已驗證（1,000 TPS 無重複扣款） | 並發負載測試 |
| 取消交易正確退款給玩家 | 功能測試 |
| 錯誤恢復機制在 SLA 內運作 | 混沌工程測試（網路分區模擬） |
| 停滯交易自動恢復 | 排程任務驗證 |
| 所有遊戲交易記錄在審計日誌中 | 審計日誌檢查 |

### 7.2 常見整合陷阱

| 陷阱 | 緩解措施 |
|---------|-----------|
| Token 重放攻擊 | 強制執行單次使用 Token 並進行伺服器端追蹤 |
| 冪等層失敗 | 多層防禦機制必須運作 |
| 鎖定逾時過短 | 鎖定持有時間必須超過最大業務執行時間 |
| 停滯交易累積 | 自動恢復任務必須監控以確保持續運作 |

→ **[冪等性實作](../../architecture/03_Game_Integration/03_Game_Integration_Security.md#idempotency-defense)** - 三層防禦（Redis 快取 + DB 唯一約束 + 分散式鎖）、逾時配置、備援機制

---

## 相關文件（Related Documents）

### 業務參考
- [Turnover Business Rules](./01_Turnover_Business_Rules.md) - 投注額和投注要求規則

### 技術實作

→ **[遊戲整合實作](../../architecture/03_Game_Integration/02_Game_Integration_Implementation.md)** - 完整遊戲供應商整合模式、API 規格、Token 生成/驗證演算法、錯誤處理和測試策略

**額外技術參考**：
- [投注額計算邏輯（架構）](../../architecture/03_Game_Integration/04_Turnover_Calculation_Logic.md) - 投注額計算技術設計

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-08
**Maintainers**: 產品團隊與遊戲整合團隊
