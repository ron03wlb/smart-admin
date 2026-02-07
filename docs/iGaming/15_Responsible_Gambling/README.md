# 15 Responsible Gambling (負責任博彩)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 模塊概述

負責任博彩 (Responsible Gambling, RG) 模塊提供完整的玩家保護工具套件，確保平台符合全球主要監管機構（UKGC、MGA、PAGCOR、Brazil SPA）的合規要求。

### 核心目標

1. **玩家保護**: 幫助玩家控制博彩行為，預防問題性博彩
2. **監管合規**: 滿足 UK Gambling Commission、Malta Gaming Authority 等牌照要求
3. **風險管理**: 識別高風險玩家，主動介入保護
4. **透明報告**: 提供監管機構所需的合規報告

---

## 文檔結構

| 文檔 | 說明 | 優先級 |
|------|------|--------|
| [15-01_Self_Exclusion.md](15-01_Self_Exclusion.md) | 自我排除系統設計 | P0 |
| [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md) | 存款限額管理 | P0 |
| [15-03_Cooling_Off_Period.md](15-03_Cooling_Off_Period.md) | 冷靜期/暫停功能 | P0 |
| [15-04_Session_Management.md](15-04_Session_Management.md) | 會話時間控制 | P1 |
| [15-05_Reality_Checks.md](15-05_Reality_Checks.md) | 現實檢查彈窗 | P1 |
| [15-06_Loss_Limits.md](15-06_Loss_Limits.md) | 虧損限額 | P1 |
| [15-07_Player_Protection_API.md](15-07_Player_Protection_API.md) | 統一保護工具 API | P0 |
| [15-08_Affordability_Assessment.md](15-08_Affordability_Assessment.md) | 可負擔性評估 | P0 |

---

## 監管要求對照

### UK Gambling Commission (UKGC)

| 要求 | LCCP 條款 | 實現文檔 |
|------|----------|---------|
| 自我排除 | SR 3.5.1 | [15-01](15-01_Self_Exclusion.md) |
| 存款限額 | SR 3.4.1 | [15-02](15-02_Deposit_Limits.md) |
| 冷靜期 | SR 3.5.2 | [15-03](15-03_Cooling_Off_Period.md) |
| 現實檢查 | SR 3.4.2 | [15-05](15-05_Reality_Checks.md) |
| 可負擔性評估 | 2025 新規 | [15-08](15-08_Affordability_Assessment.md) |
| 即時 KYC | 2025 新規 | → [05-03_KYC_AML.md](../05_Risk_Control/05-03_KYC_AML.md) |

### Malta Gaming Authority (MGA)

| 要求 | 條款 | 實現文檔 |
|------|------|---------|
| 自我排除 | Player Protection Directive | [15-01](15-01_Self_Exclusion.md) |
| 存款限額 | Player Protection Directive | [15-02](15-02_Deposit_Limits.md) |
| 玩家保護基金 | License Conditions | → [06-09_MGA_Compliance.md](../06_Platform_Governance/06-09_MGA_Compliance.md) |

### Brazil SPA (2025-2026)

| 要求 | 條款 | 實現文檔 |
|------|------|---------|
| 自我排除 | Portaria SPA | [15-01](15-01_Self_Exclusion.md) |
| 存款限額 | Portaria SPA | [15-02](15-02_Deposit_Limits.md) |
| .bet.br 域名 | 2026 強制 | → [06-10_Brazil_SPA_Compliance.md](../06_Platform_Governance/06-10_Brazil_SPA_Compliance.md) |

---

## 架構概覽

```
┌─────────────────────────────────────────────────────────────────┐
│                    Player Protection Gateway                     │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│  │ Self-       │  │ Deposit     │  │ Session     │  │ Reality │ │
│  │ Exclusion   │  │ Limits      │  │ Manager     │  │ Checks  │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────┬────┘ │
│         │                │                │               │      │
│  ┌──────▼────────────────▼────────────────▼───────────────▼────┐│
│  │                 Protection Rules Engine                      ││
│  │            (LiteFlow / Drools Integration)                   ││
│  └──────────────────────────┬───────────────────────────────────┘│
│                             │                                    │
│  ┌──────────────────────────▼───────────────────────────────────┐│
│  │              Player Protection Database                       ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   ││
│  │  │ Exclusion   │  │ Limit       │  │ Session             │   ││
│  │  │ Records     │  │ Settings    │  │ Activity Logs       │   ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘   ││
│  └──────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              External Integration Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Gamstop    │  │ MGA         │  │ National Exclusion      │  │
│  │ (UK)       │  │ Self-Ban    │  │ Databases               │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心實體設計

### PlayerProtectionSettings

```java
@Data
@Entity
@Table(name = "t_player_protection_settings")
public class PlayerProtectionSettings {

    @Id
    private Long playerId;

    // 自我排除
    private Boolean selfExcluded;
    private LocalDateTime exclusionStartTime;
    private LocalDateTime exclusionEndTime;
    private String exclusionType; // TEMPORARY, PERMANENT

    // 存款限額
    private BigDecimal dailyDepositLimit;
    private BigDecimal weeklyDepositLimit;
    private BigDecimal monthlyDepositLimit;
    private LocalDateTime limitEffectiveDate;

    // 虧損限額
    private BigDecimal dailyLossLimit;
    private BigDecimal weeklyLossLimit;
    private BigDecimal monthlyLossLimit;

    // Session 限制
    private Integer sessionDurationMinutes;
    private Integer realityCheckIntervalMinutes;

    // 冷靜期
    private Boolean coolingOff;
    private LocalDateTime coolingOffEndTime;

    // 可負擔性評估
    private String affordabilityTier; // STANDARD, ENHANCED, RESTRICTED
    private LocalDateTime lastAffordabilityCheck;

    // 審計
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### ExclusionRecord

```java
@Data
@Entity
@Table(name = "t_exclusion_record")
public class ExclusionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long playerId;
    private String exclusionType; // SELF_EXCLUSION, GAMSTOP, OPERATOR_EXCLUSION
    private String duration; // 24H, 7D, 30D, 1Y, 5Y, PERMANENT
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String initiatedBy; // PLAYER, OPERATOR, REGULATOR
    private String externalReference; // Gamstop reference, etc.

    // 解除排除
    private Boolean revoked;
    private LocalDateTime revokedAt;
    private String revocationReason;

    // 審計
    private LocalDateTime createdAt;
}
```

---

## 關鍵業務流程

### 1. 自我排除流程

```
玩家請求排除 → 確認對話框 → 24h 冷靜期確認 → 立即生效 → 關閉所有活動會話
     │                                              │
     └─────────── 同步至 Gamstop (UK) ◄─────────────┘
```

**詳見**: [15-01_Self_Exclusion.md](15-01_Self_Exclusion.md)

### 2. 存款限額流程

```
設定限額 → 立即生效（降低）/ 24-72h 冷靜期（提高）
    │
    ├── 達到限額 → 阻止存款 → 顯示訊息
    │
    └── 嘗試提高 → 冷靜期倒數 → 確認後生效
```

**詳見**: [15-02_Deposit_Limits.md](15-02_Deposit_Limits.md)

### 3. 現實檢查流程

```
遊戲進行 → 達到設定時間（15/30/60分鐘）→ 彈窗顯示
    │                                        │
    │                                        ├── 盈虧狀態
    │                                        ├── 累計時間
    │                                        └── 繼續/停止選項
    │
    └── 選擇繼續 → 重置計時器 → 繼續遊戲
```

**詳見**: [15-05_Reality_Checks.md](15-05_Reality_Checks.md)

---

## 與其他模塊的整合

| 模塊 | 整合點 | 說明 |
|------|--------|------|
| **01_Player_Center** | 玩家資料 | 保護設定綁定玩家帳戶 |
| **02_Finance_Center** | 存款/出金 | 限額檢查、排除玩家阻止交易 |
| **03_Game_Center** | 遊戲進入 | 排除玩家阻止進入遊戲 |
| **05_Risk_Control** | 風控規則 | 高風險玩家自動建議限額 |
| **06_Platform_Governance** | 多牌照 | 按牌照載入不同規則 |
| **09_Technical_Infrastructure** | Sa-Token | 會話管理整合 |

---

## API 概覽

### 玩家端 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/v1/player/protection/settings` | GET | 獲取保護設定 |
| `/api/v1/player/protection/deposit-limits` | PUT | 設定存款限額 |
| `/api/v1/player/protection/self-exclusion` | POST | 申請自我排除 |
| `/api/v1/player/protection/cooling-off` | POST | 申請冷靜期 |
| `/api/v1/player/protection/session-limits` | PUT | 設定會話限制 |
| `/api/v1/player/protection/activity-history` | GET | 獲取活動歷史 |

### 管理端 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/v1/admin/protection/exclusions` | GET | 排除玩家列表 |
| `/api/v1/admin/protection/reports` | GET | 合規報告 |
| `/api/v1/admin/protection/gamstop/sync` | POST | Gamstop 同步 |

**詳見**: [15-07_Player_Protection_API.md](15-07_Player_Protection_API.md)

---

## 監控指標

| 指標 | 說明 | 告警閾值 |
|------|------|---------|
| `rg_self_exclusion_count` | 自我排除人數 | 日環比 > 50% |
| `rg_deposit_limit_breaches` | 存款限額觸發次數 | - |
| `rg_session_timeout_count` | 會話超時次數 | - |
| `rg_reality_check_continue_rate` | 現實檢查後繼續率 | > 95% 需關注 |
| `rg_gamstop_sync_failures` | Gamstop 同步失敗 | > 0 |

---

## 合規報告

### 必要報告清單

| 報告 | 頻率 | 監管機構 | 說明 |
|------|------|---------|------|
| 自我排除統計 | 月度 | UKGC, MGA | 排除/解除數量 |
| 存款限額使用率 | 月度 | UKGC | 設定限額玩家比例 |
| 可負擔性評估 | 季度 | UKGC | UK 2025 新規 |
| 問題博彩識別 | 季度 | 所有 | 高風險玩家處理 |

---

## 變更歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-02-07 | 初始版本，基於業界規範創建 |

---

**返回**: [iGaming 文檔首頁](../README.md) | [實作指南索引](../00_Foundation/00-04_Implementation_Index.md)
