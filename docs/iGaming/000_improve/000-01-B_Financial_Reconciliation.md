# 財務整合與對帳深化 (Financial Integrity & Reconciliation)

**版本**: v0.1.0 (草稿)
**日期**: 2026-02-07
**狀態**: 📋 待前置依賴

---

## 前置依賴

> ⚠️ **暫緩執行**: 本文檔需先完成以下 Gap Analysis 文檔：
> - [000-04_Gap_Analysis_Reconciliation.md](./000-04_Gap_Analysis_Reconciliation.md)
>
> 待完成 Gap Analysis 後再實施本文檔內容。

---

## 問題陳述

現有對帳側重於 **外部 (PSP/Game)**，缺乏 **內部審計 (Internal Audit)** 與 **佣金核算 (Commission)** 的監督機制。

### 風控盲區

| 盲區類型 | 說明 | 潛在風險 |
|---------|------|---------|
| **內部總帳風險** | 若數據庫被 DB Admin 篡改或系統出現並發扣款 Bug，缺乏每日總帳試算 (Trial Balance) 難以發現 | 資金異常 |
| **佣金超發風險** | NGR 計算缺乏獨立檢核，可能因未扣除 Bonus Cost 導致代理佣金超發 | 資金流失 |
| **對帳精度問題** | 缺乏時區與幣種精度的明確標準 | 對帳誤差 |

---

## 擬新增模組

### 1. 內部錢包審計 ([02-09_Internal_Wallet_Audit.md])

**功能**:
- **日結試算**: `Total Deposits - Withdrawals + Wins - Losses == Sum(User Balances)`
- **監控**: 餘額不平時觸發 P0 告警

**審計公式**:
```sql
SELECT
    SUM(deposits) - SUM(withdrawals) + SUM(wins) - SUM(losses) AS expected_balance,
    SUM(user_balance) AS actual_balance,
    ABS(expected_balance - actual_balance) AS diff
FROM daily_wallet_audit
WHERE diff > 0.01  -- 容忍閾值 0.01
```

### 2. 代理佣金對帳 ([06-05_Affiliate_Commission_Reconciliation.md])

**功能**:
- **NGR 審計**: 獨立驗證 `NGR = Bet - Win - Bonus - Tax` 的計算過程

**對帳邏輯**:
```java
// 佣金計算公式驗證
BigDecimal expectedNGR = totalBet
    .subtract(totalWin)
    .subtract(totalBonus)
    .subtract(totalTax);

if (!expectedNGR.equals(reportedNGR)) {
    alertService.trigger("NGR_MISMATCH", affiliateId);
}
```

### 3. 遊戲商時區與精度優化 ([02-04] 增強)

**補充內容**:
- Provider 時區處理 (UTC/UTC+8)
- 幣種精度處理邏輯（小數位數）

---

## 實施優先級

| 優先級 | 模組 | 理由 |
|--------|------|------|
| **P1** | 內部錢包審計 (Internal Audit) | 防止內部欺詐與系統崩潰 |
| **P2** | 代理佣金對帳 (Commission Recon) | 防止資金流失 |
| **P3** | 遊戲商時區精度優化 | 提升對帳效率 |

---

## 相關文檔

- **主文檔**: [000-01_Risk_Control_Improvement.md](./000-01_Risk_Control_Improvement.md)
- **前置依賴**: [000-04_Gap_Analysis_Reconciliation.md](./000-04_Gap_Analysis_Reconciliation.md) (待建立)
- **系列文檔**:
  - [000-01-A 供應商風控](./000-01-A_Provider_Game_Risk.md)
  - [000-01-C 合規韌性](./000-01-C_Compliance_Resilience.md)

---

## 變更日誌

| 版本 | 日期 | 變更 |
|------|------|------|
| v0.1.0 | 2026-02-07 | 從 000-01 §10 分離，建立獨立文檔框架 |
