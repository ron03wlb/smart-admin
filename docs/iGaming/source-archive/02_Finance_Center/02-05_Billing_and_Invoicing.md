# 02-05 商戶計費與發票系統 (Tenant Billing & Invoicing)

## 1. 系統概述
本模組專為 **B2B SaaS 平台方** 設計，用於自動化計算商戶 (Tenant) 的營運成本、營收分成 (Revenue Share) 並生成帳單，確保平台現金流穩定。

## 2. 計費模型 (Billing Model)

### 2.1 費用組成
商戶的月度帳單通常包含以下項目：

| 項目 | 計算方式 | 說明 |
| :--- | :--- | :--- |
| **Setup Fee (設定費)** | One-time | 開站時的一次性費用。 |
| **Monthly/License Fee (月費)** | Fixed | 平台授權費 (如 $5,000/月)。 |
| **Revenue Share (GGR 分成)** | GGR * Rate % | 依據階梯費率抽成 (如 10%~15%)。 |
| **Game Provider Fee (遊戲費)** | GGR * GP Rate % | 需支付給遊戲原廠的成本 (通常由平台代收代付)。 |
| **Infrastructure Cost** | Usage-based | 實報實銷：CDN 流量費、雲服務器費、簡訊費。 |
| **Adjustment (調整項)** | Manual | 針對上期爭議款項的調整。 |

### 2.2 階梯費率 (Tiered Pricing)
系統支援動態階梯配置：
- GGR $0 - $500k: **15%**
- GGR $500k - $1M: **12%**
- GGR > $1M: **10%**

## 3. 錢包與扣款邏輯

### 3.1 商戶錢包 (Merchant Wallet)
每個商戶在平台側擁有一個獨立資金帳戶：
- **Pre-paid Balance (預付餘額)**：商戶充值的金額。
- **Billing Wallet**：專用於扣除費用的子錢包。

### 3.2 扣款流程 (Deduction Process)
1.  **每日預扣 (Daily Provision)**：
    - 每日 04:00 估算昨日 GGR。
    - 凍結預估分成金額 (`Estimated Fee`)。
    - **目的**：防止商戶月底沒錢付賬。
2.  **月度結算 (Monthly Settlement)**：
    - 每月 1 號生成正式 Invoice。
    - `Final Fee` - `Total Provisions` = `Net Payable` (應付差額)。
    - 從 Merchant Wallet 自動扣除。

### 3.3 帳單爭議處置 (Billing Dispute)
若商戶對 GGR 計算或收費有異議：
1.  **提交爭議 (Contest)**：商戶 Admin 在收到 Invoice 3 日內，透過後台點擊 "Dispute Invoice"。
2.  **舉證 (Evidence)**：需上傳遊戲商後台報表 vs 平台報表的差異截圖。
3.  **鎖定 (Freeze)**：爭議期間，該筆 Invoice 暫停自動扣款 (不觸發欠費停權)。
4.  **裁決 (Adjudication)**：平台財務審核後，若屬實則發布 `Credit Note` 進行抵扣；若駁回則立即恢復扣款。

### 3.4 欠費處置 (Dunning Algorithm)
當商戶餘額不足以支付費用時，觸發自動化追債流程：

- **Stage 1 (T+1)**: Email/Telegram 催繳通知，發送 Low Balance Alert。
- **Stage 2 (T+3)**: **降級服務 (Graceful Degradation)** - 禁止開立新玩家帳號，停止 API 寫入操作 (只讀)。
- **Stage 3 (T+7)**: **後台鎖定** - 管理員無法登入後台。
- **Stage 4 (T+14)**: **玩家停權** - 前台顯示 "維護中"，停止所有遊戲服務。

## 4. 自助充值 (Self-Service Top-up)
商戶後台需提供充值入口：
- 支援 **USDT/USDC** 支付 (針對加密友好商戶)。
- 支援 **Wire Transfer** (針對合規商戶，需人工上傳憑證核銷)。
- 充值成功後，自動觸發 `Unsuspend` 恢復服務。

## 5. 報表與審計
- **Invoice PDF 生成**：包含詳細費用明細 (Breakdown)。
- **Reconciliation Report**：展示 平台數據 vs 遊戲商數據 的差異調整。

---

**文檔版本**: 4.0.0
**最後更新**: 2026-01-28
**維護團隊**: Finance Team & Backend Team

---

## 📚 相關文檔

### 前置依賴
- [02-06 統一錢包模型](./02-06_Wallet_Architecture.md) - 錢包架構
- [07-01 租戶層級架構](../06_Platform_Governance/06-01_Multi_Tenant.md) - 多租戶模型
