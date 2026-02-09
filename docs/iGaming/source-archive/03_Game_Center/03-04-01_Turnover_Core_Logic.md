# 03-04-01 流水計算核心邏輯 (Turnover Core Logic)

<!-- SSOT: Authoritative definition of Turnover Core Logic and Layer 1 Risk Validation -->

> **父文檔**: [03-04 流水計算與遊戲對帳](./03-04_Turnover_Calculation.md)
>
> **三層風控架構定位**: 本子文檔定義流水計算的核心概念與 Layer 1 風控驗證邏輯。
>
> **創建日期**: 2026-02-07
> **最後更新**: 2026-02-07
> **版本**: 4.0.0

---

## 1. 系統概述

### 1.1 核心原則

**有效流水定義**: 僅計算「產生輸贏結果」、「具備風險」且「通過風控驗證」的注單。

**公式**:
```
ValidTurnover = BetAmount × GameWeight × OddsFactor × StatusFactor × RiskFactor
```

其中：
- **RiskFactor**: `1` (Pass) 或 `0` (Reject/Flag)
- **StatusFactor**: Layer 2財務層確定 (WIN/LOSS=1.0, DRAW=0)
- **GameWeight**: Layer 3活動層應用 (Slots=1.0, Baccarat=0.15)

### 1.2 三層驗證架構總覽

**架構圖**:

```mermaid
graph TB
    subgraph "玩家投注"
        A["玩家下注<br/>Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95"]
    end

    subgraph "Layer 1: 風控引擎 (Risk Engine - 05-01)"
        B["對沖檢測<br/>Hedge Detection"]
        C["套利檢測<br/>Arbitrage Detection"]
        D["低賠率過濾<br/>Low Odds Filter<br/>閾值: 1.5"]
        E["輸出: valid_bet<br/>有效投注額<br/>+ action_type<br/>(BLOCK/FLAG/PASS)"]
    end

    subgraph "Layer 2: 財務中心 (Finance Center - 02-04)"
        F["注單結算<br/>Bet Settlement"]
        G["記錄結算狀態<br/>Settlement Status"]
        H{注單狀態?}
        I["WIN/LOSS<br/>記錄狀態"]
        J["DRAW/TIE<br/>記錄狀態"]
        K["VOID/CANCEL<br/>記錄狀態"]
        L["HALF_WIN/LOSS<br/>記錄狀態"]
        M["輸出: valid_bet<br/>有效投注額 (不變)"]
    end

    subgraph "Layer 3: 活動系統 (Activity System - 04-01)"
        N["遊戲權重應用<br/>Game Weight"]
        O{遊戲類型?}
        P["Slots/Sports<br/>Weight: 1.0"]
        Q["Baccarat<br/>Weight: 0.15"]
        R["Blackjack<br/>Weight: 0.1"]
        S["Roulette<br/>Weight: 0.2"]
        T["輸出: activity_valid_turnover<br/>活動有效流水"]
    end

    subgraph "應用場景"
        U["返水計算<br/>Rebate Calculation"]
        V["流水進度追蹤<br/>Wagering Progress"]
        W["VIP升級<br/>VIP Upgrade"]
    end

    A --> B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G --> H
    H --> I
    H --> J
    H --> K
    H --> L
    I --> M
    J --> M
    K --> M
    L --> M
    M --> N
    N --> O
    O --> P
    O --> Q
    O --> R
    O --> S
    P --> T
    Q --> T
    R --> T
    S --> T
    T --> U
    T --> V
    T --> W

    style A fill:#e1f5ff
    style E fill:#fff3cd
    style M fill:#d4edda
    style T fill:#d1ecf1
```

### 1.3 關鍵術語定義

<!-- SSOT: Authoritative terminology definitions -->

| 中文 | 英文 | 定義 | 單位 | 用途 |
|------|------|------|------|------|
| **投注額** | Bet Amount | 單筆原始投注金額 | 單筆 | API 交互、資金扣除 |
| **流水** | Turnover | 投注額的時間累積總和 | 累積 | GGR 計算、財務報表 |
| **有效投注額** | Valid Bet | 經風控過濾的單筆金額 | 單筆 | 流水要求、返水、VIP |
| **流水要求** | Wagering Requirement | 必須達成的有效投注總額 | 累積 | 活動驗證、取款限制 |

**詳細定義**: [術語標準化文檔](../00_Foundation/concepts/00-03_Terminology_Standards.md)

---

## 2. Layer 1: 風控驗證 (Risk Engine Validation)

### 2.1 風控引擎職責

<!-- SSOT: Layer 1 is the ONLY layer responsible for rejection decisions -->

**核心原則**: Layer 1是唯一負責拒絕決策的層級，後續層級(Layer 2/3)僅做數值調整。

**職責矩陣**:

| 職責類型 | Layer 1 (Risk Engine) | Layer 2 (Finance) | Layer 3 (Activity) |
|---------|----------------------|-------------------|-------------------|
| **拒絕決策** | ✅ 唯一負責 | ❌ 不參與 | ❌ 不參與 |
| **狀態因子調整** | ❌ 不參與 | ✅ 唯一負責 | ❌ 不參與 |
| **遊戲權重應用** | ❌ 不參與 | ❌ 不參與 | ✅ 唯一負責 |
| **短路返回** | ✅ BLOCK規則直接返回0 | ❌ 信任 Layer 1 結果 | ❌ 信任 Layer 2 結果 |

### 2.2 配置驅動風控 (v2.1.0)

**新特性**: 支援 `action_type` 配置驅動 (BLOCK/FLAG/PASS)

**返回結構**:
```typescript
interface RiskValidationResult {
  is_valid: boolean;
  action_type: 'BLOCK' | 'FLAG' | 'PASS';
  matched_rules: string[];
  risk_proposal_id: string | null;
  effective_turnover_base: number;
}
```

**三種Action類型**:

| Action Type | 說明 | valid_bet | 流水計算 | 風控提案 |
|-------------|------|-----------|---------|---------|
| **BLOCK** | 實時阻斷 | 0 | 不計入 | 不生成 |
| **FLAG** | 標記但允許 | bet_amount | 正常計入 | ✅ 生成 |
| **PASS** | 正常通過 | bet_amount | 正常計入 | 不生成 |

### 2.3 風控檢測流程

**對沖檢測流程圖**:

```mermaid
flowchart TD
    A[開始: 對沖檢測]
    B["獲取注單信息<br/>player_id, round_id<br/>selection, amount"]
    C["查詢同一Round<br/>該玩家的所有注單"]
    D{"是否存在<br/>相反投注?"}

    subgraph Example["範例: 百家樂"]
        E1[投注 Banker $1000]
        E2[投注 Player $950]
        E3["對沖檢測: ✓<br/>相反投注"]
    end

    F[標記為對沖投注]
    G["action_type = BLOCK<br/>(或 FLAG，依配置)"]
    H[effective_turnover = 0]
    I[無對沖投注]
    J[繼續下一步驗證]

    End([結束: 返回驗證結果])

    A --> B
    B --> C
    C --> D
    D -->|是| F
    D -->|否| I

    F --> G
    G --> H
    H --> End

    I --> J
    J --> End

    style F fill:#f8d7da
    style H fill:#f8d7da
    style I fill:#d4edda
```

**賠率閾值檢測**:

```mermaid
flowchart TD
    A[開始: 賠率驗證]
    B["讀取配置<br/>MIN_ODDS_THRESHOLD = 1.5"]
    C["獲取注單賠率<br/>odds = 1.95"]
    D{odds ≥ threshold?}
    E["賠率合格<br/>通過驗證"]
    F["低賠率投注<br/>action_type = BLOCK"]
    G[effective_turnover = 0]

    subgraph Examples["賠率範例"]
        EX1[1.95 → 通過 ✓]
        EX2[1.50 → 通過 ✓]
        EX3[1.30 → 拒絕 ✗]
        EX4[1.01 → 拒絕 ✗]
    end

    End([返回驗證結果])

    A --> B
    B --> C
    C --> D
    D -->|是| E
    D -->|否| F

    E --> End
    F --> G
    G --> End

    style E fill:#d4edda
    style F fill:#f8d7da
    style G fill:#f8d7da
```

### 2.4 Layer 1 處理流程 (v2.1.0)

**調用層次**:

```typescript
// ✅ 正確方式 (v2.1.0): Layer 1 拒絕後直接短路
const riskValidation = await RiskEngine.validateTurnover({...});

if (!riskValidation.is_valid && riskValidation.action_type === 'BLOCK') {
  // BLOCK規則直接返回0，不進入Layer 2/3
  return {
    effective_turnover_base: 0,
    valid_turnover_finance: 0,
    activity_valid_turnover: 0,
    rejected_by: 'RISK_ENGINE',
    action_type: 'BLOCK',
    matched_rules: riskValidation.matched_rules
  };
}

// FLAG規則標記但繼續 (v2.1.0)
if (riskValidation.action_type === 'FLAG') {
  await recordRiskFlag(bet.id, riskValidation.risk_proposal_id);
}

// Layer 2僅負責狀態因子調整 (信任Layer 1已通過驗證)
const valid_turnover_finance = calculateFinanceTurnover(
  riskValidation.effective_turnover_base,
  bet.status
);
```

**性能優化效果 (v2.0.0→v2.1.0)**:
- Layer 1 拒絕後直接短路返回
- 節省性能: ~5% CPU 與 DB 查詢
- BLOCK vs FLAG分離: FLAG規則允許流水正常計算 + 生成風控提案

---

## 相關文檔

### 子文檔導航
- **下一篇**: [03-04-02 三層驗證架構](./03-04-02_Three_Layer_Validation.md) - Layer 2 財務狀態 + Layer 3 活動權重
- [03-04-03 對帳模型](./03-04-03_Reconciliation_Model.md) - 有效投注計算 + 免費旋轉流水 + 遊戲對帳
- [03-04-04 SmartAdmin 架構映射](./03-04-04_SmartAdmin_Mapping.md) - 投注要求追蹤 + 代碼實現 + 監控

### 外部依賴
- [00-03 術語標準化](../00_Foundation/concepts/00-03_Terminology_Standards.md) - **必讀**
- [05-01 風控系統](../05_Risk_Control/05-01_Risk_Framework.md) - Layer 1 風控引擎

---

**文檔版本**: 4.0.0
**最後更新**: 2026-02-07
**維護團隊**: Finance Team & Backend Team & Risk Team
