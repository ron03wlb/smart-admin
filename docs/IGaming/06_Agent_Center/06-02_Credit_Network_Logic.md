# 06-02 信用網與額度邏輯 (Credit Network Logic)

## 1. 系統概述
信用網 (Credit Network) 是亞洲市場特有的營運模式，與現金網 (Cash Market) 的核心差異在於 **「先遊玩、後結算」**。
系統需支援 **信用額度 (Credit Limit)** 的授予、消耗、歸還，以及代理體系內的 **佔成 (Position Taking)** 計算。

## 2. 核心邏輯模型

### 2.1 雙錢包體系 (Dual Wallet System)
為了兼容現金與信用模式，玩家帳戶應具備兩個獨立錢包：
- **Cash Wallet (現金錢包)**：透過金流充值，餘額 `Balance` 必須 > 0 才能下注。
- **Credit Wallet (信用錢包)**：
  - **Credit Limit (信用額度)**：由上級代理授予的最高可輸金額 (Max Loss)。
  - **Used Credit (已用額度)**：當前未結算的輸贏總和。
  - **Available Credit (可用額度)**：`Limit - Used`。
  - **邏輯**：下注時不扣除 Balance，而是增加 `Exposure` (曝險)。結算 (Settlement) 時才更新 `Used Credit`。

### 2.2 額度傳遞 (Credit Propagation)
額度由最高層級向下發放，形成樹狀結構：
`Platform` -> `Master Agent (總代)` -> `Agent L1` -> `Agent L2` -> `Player`

*   **授信 (Allocation)**：上級可隨時調整下級的 Limit。
*   **強平 (Liquidation)**：
    *   當下級 `Used / Limit > 90%` (警戒線)，發送通知。
    *   當 `Used >= Limit` (爆倉)，自動暫停下級所有玩家的下注權限。

## 3. 佔成機制 (Position Taking)

佔成決定了代理與上級如何分擔玩家的輸贏風險。

### 3.1 佔成公式
`Agent Win/Loss = (Player Win/Loss) * (Agent Position %)`

**範例情境**：
*   玩家輸 $10,000。
*   代理 A (直屬) 佔成 40%。
*   代理 B (上級) 佔成 30%。
*   總代 C (最高級) 佔成 20% (剩餘 10% 歸平台/公司)。

**結果**：
*   玩家：-10,000 (需支付給代理 A)
*   代理 A：+4,000 (從玩家收 10k，上繳 6k)
*   代理 B：+3,000
*   總代 C：+2,000
*   平台：+1,000

### 3.2 佔成限制 (Constraint)
*   **Max Position**：下級佔成比例不得超過上級設定的上限。
*   **Auto-Hedge**：若代理不想承擔風險，可將佔成設為 0% (全額上繳，只賺水錢)。

## 4. 結算流程 (Settlement Process)

### 4.1 週期性結算
信用網通常採用 **週結 (Weekly)** 或 **月結 (Monthly)**。

1.  **Freeze (凍結)**：每週一 12:00 凍結上一週的 Credit 帳目。
2.  **Statement Generation (報表生成)**：計算每位代理應收/應付金額 (AR/AP)。
3.  **Settlement (對帳/清算)**：
    *   代理透過銀行轉帳/USDT 結算差額。
    *   上級確認收到款項後，執行 `Reset Credit` (恢復額度)。

### 4.2 信用結算工單 (Settlement Agent)
系統需提供 "帳務工單" 功能：
*   `Type`: Deposit (上繳贏利) / Refill (補足虧損)。
*   `Status`: Pending -> Reviewed -> Completed。

## 5. 風控邏輯
*   **異常佔成監控**：若某代理突然將佔成調至 100% 且下線玩家大量贏錢，觸發 "聯合套利" 警報。
*   **信用評分**：依據代理歷史結算準時度，給予信用評分，影響可獲取的最大額度。
