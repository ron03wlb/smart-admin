# 合規與運營韌性 (Compliance & Operational Resilience)

**版本**: v0.1.0 (草稿)
**日期**: 2026-02-07
**狀態**: 📋 待前置依賴

---

## 前置依賴

> ⚠️ **暫緩執行**: 本文檔需先完成以下 Gap Analysis 文檔：
> - [000-05_Final_Gap_Analysis_Compliance_Infra.md](./000-05_Final_Gap_Analysis_Compliance_Infra.md)
>
> 待完成 Gap Analysis 後再實施本文檔內容。

---

## 問題陳述

現有文檔在 **合規性 (Regulatory)** 與 **穩定性 (Stability)** 方面存在致命缺失。

### 風控盲區

| 盲區類型 | 說明 | 影響 |
|---------|------|------|
| **合規紅線** | 缺乏責任博彩 (RG) 模組（如自我隔離、存款限制），無法通過 GLI/MGA 認證 | 市場准入受阻 |
| **流量欺詐** | 現金網缺乏對 Affiliate Traffic 的防作弊機制 (Click Injection)，導致廣告費被刷 | 運營成本浪費 |
| **單點故障** | 錢包服務缺乏明確的 "降級策略"（如 DB 掛掉時如何處理） | 服務中斷風險 |

---

## 擬新增模組

### 1. 責任博彩引擎 ([01-06_Responsible_Gaming_Module.md])

**功能**:
- **Deposit Limit**: 存款限額設定
- **Loss Limit**: 虧損限額設定
- **Self-Exclusion**: 自我隔離（不可撤銷）
- **Reality Check**: 遊戲時間提醒

**強制性**: 這是所有正規市場 (UK, US, EU) 的入場券。

**實現示例**:
```java
public class ResponsibleGamingService {

    public boolean checkDepositLimit(Long playerId, BigDecimal amount) {
        PlayerLimit limit = getPlayerLimit(playerId);
        BigDecimal totalToday = getTodayDeposits(playerId);

        return totalToday.add(amount).compareTo(limit.getDailyLimit()) <= 0;
    }

    public void triggerSelfExclusion(Long playerId, int months) {
        // 不可撤銷的自我隔離
        PlayerExclusion exclusion = new PlayerExclusion();
        exclusion.setPlayerId(playerId);
        exclusion.setEndDate(LocalDate.now().plusMonths(months));
        exclusion.setIrrevocable(true);  // 關鍵：不可撤銷

        exclusionRepository.save(exclusion);
        notifyPlayer(playerId, "SELF_EXCLUSION_ACTIVATED");
    }
}
```

### 2. 代理流量完整性 ([06-03_Affiliate_Tracking_Integrity.md])

**功能**:
- **CTIT 檢查**: Click-To-Install Time 檢查，防止歸因劫持
- **設備指紋**: 檢測同設備多次歸因

**防護規則**:
```
IF (install_time - click_time) < 10s THEN flag_as_suspicious
IF same_device_id AND multiple_affiliates THEN reject_attribution
```

### 3. 錢包高可用 ([07-05_High_Availability_Wallet.md])

**功能**:
- **降級模式**: 定義 "Read-Only Mode"
- **策略**: 在主庫宕機時允許遊玩（扣款記入 Redis 隊列），但禁止充提

**降級邏輯**:
```java
public enum WalletMode {
    NORMAL,      // 全功能
    READ_ONLY,   // 僅查詢
    GAME_ONLY,   // 僅遊戲（扣款進隊列）
    MAINTENANCE  // 完全不可用
}

@Scheduled(fixedDelay = 5000)
public void healthCheck() {
    if (!primaryDB.isHealthy()) {
        walletMode = WalletMode.GAME_ONLY;
        // 扣款記入 Redis 隊列，待主庫恢復後 replay
    }
}
```

---

## 實施優先級

| 優先級 | 模組 | 理由 |
|--------|------|------|
| **P1** | 責任博彩 (RG) | 合規剛需，優先級最高 |
| **P2** | 錢包高可用 (HA Wallet) | 防止重大事故 |
| **P3** | 代理流量防護 | 優化運營成本 |

---

## 相關文檔

- **主文檔**: [000-01_Risk_Control_Improvement.md](./000-01_Risk_Control_Improvement.md)
- **前置依賴**: [000-05_Final_Gap_Analysis_Compliance_Infra.md](./000-05_Final_Gap_Analysis_Compliance_Infra.md) (待建立)
- **系列文檔**:
  - [000-01-A 供應商風控](./000-01-A_Provider_Game_Risk.md)
  - [000-01-B 財務對帳](./000-01-B_Financial_Reconciliation.md)

---

## 變更日誌

| 版本 | 日期 | 變更 |
|------|------|------|
| v0.1.0 | 2026-02-07 | 從 000-01 §11 分離，建立獨立文檔框架 |
