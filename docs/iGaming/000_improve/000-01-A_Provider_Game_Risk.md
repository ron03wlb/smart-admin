# 供應商與遊戲風控 (Provider & Game Risk)

**版本**: v0.1.0 (草稿)
**日期**: 2026-02-07
**狀態**: 📋 待前置依賴

---

## 前置依賴

> ⚠️ **暫緩執行**: 本文檔需先完成以下 Gap Analysis 文檔：
> - [000-03_Gap_Analysis_Provider_Game_Risk.md](./000-03_Gap_Analysis_Provider_Game_Risk.md)
>
> 待完成 Gap Analysis 後再實施本文檔內容。

---

## 問題陳述

現有系統缺乏對 **遊戲商異常 (Provider Anomalies)** 和 **遊戲漏洞 (Game Glitches)** 的防禦能力。

### 風控盲區

| 盲區類型 | 說明 | 潛在損失 |
|---------|------|---------|
| **RTP 異常** | 若某款 Slot 遊戲因 Bug 導致 RTP > 100%，系統目前無法自動熔斷 | 高 |
| **延遲套利** | 玩家利用不同平台間的 API 延遲 (Latency) 進行無風險套利 | 中 |
| **數據完整性** | 缺乏 Provider 端的 Round ID 連續性檢查，易被 "吃單" | 高 |

---

## 擬新增模組

### 1. 遊戲異常監控 ([04-05_Game_Anomaly_Monitor.md])

**功能**:
- **RTP 熔斷**: 實時監控 Lobby/Game 維度的 RTP，異常自動下架
- **特徵檢測**: `WinAmount > BetAmount * 10` 且 `Frequency > 5/min`

**監控指標**:
```
RTP_realtime = SUM(WinAmount) / SUM(BetAmount) over sliding_window(1h)
IF RTP_realtime > 1.05 THEN trigger_circuit_breaker(game_id)
```

### 2. 遊戲商對帳 ([02-08_Provider_Reconciliation.md])

**功能**:
- **完整性檢查**: `Platform.TotalBet` vs `Provider.TotalBet`
- **延遲監控**: 報警若 `BetTime - ReceiveTime > 2s`

**對帳規則**:
```sql
SELECT
    provider_id,
    ABS(platform_total_bet - provider_total_bet) AS diff
FROM reconciliation_summary
WHERE diff > 100  -- 容忍閾值
```

---

## 實施優先級

| 優先級 | 模組 | 理由 |
|--------|------|------|
| **P1** | 遊戲異常監控 (RTP Monitor) | 直接防止巨額虧損 |
| **P2** | 延遲套利檢測 (Latency Arbitrage) | 提升平台公平性 |
| **P3** | 遊戲商對帳 (Provider Recon) | 財務合規 |

---

## 相關文檔

- **主文檔**: [000-01_Risk_Control_Improvement.md](./000-01_Risk_Control_Improvement.md)
- **前置依賴**: [000-03_Gap_Analysis_Provider_Game_Risk.md](./000-03_Gap_Analysis_Provider_Game_Risk.md) (待建立)
- **系列文檔**:
  - [000-01-B 財務對帳](./000-01-B_Financial_Reconciliation.md)
  - [000-01-C 合規韌性](./000-01-C_Compliance_Resilience.md)

---

## 變更日誌

| 版本 | 日期 | 變更 |
|------|------|------|
| v0.1.0 | 2026-02-07 | 從 000-01 §9 分離，建立獨立文檔框架 |
