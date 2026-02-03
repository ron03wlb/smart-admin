# IGaming 流水計算流程圖與時序圖

> **文檔目標**: 可視化展示 IGaming 平台流水計算的完整流程，包含三層驗證架構的詳細交互。
>
> **重要更新 (v2.0.0 - 2026-01-28)**:
> - ✅ 修正 Layer 2 邏輯: 移除 status_factor 動態修改 valid_bet 的錯誤設計
> - ✅ 採用「標準本金法」: valid_bet = bet_amount (不論結算狀態)
> - ✅ 增加風控標記機制: risk_status, filter_reason, risk_rules_applied
> - ✅ 增加審計追溯支持: calculation_version, 回推重算機制
> - ✅ 術語標準化: 統一使用 Bet Amount, Valid Bet, Wagering Requirement
>
> **創建日期**: 2026-01-27
> **最後更新**: 2026-01-28
> **版本**: 2.0.0
>
> **參考文檔**:
> - [00-03 術語標準化定義](../../00_Concept_&_Analysis/00-03_Terminology_Standards.md) - **必讀**
> - [05-01 風控系統](IGaming需求框架/05_Risk_Management/05-01_Risk_Control_System.md)
> - [02-04 流水與對帳](IGaming需求框架/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md)
> - [04-01 活動系統](IGaming需求框架/04_Activity_Center/04-01_Activity_System_Design.md)

---

## 📊 目錄

1. [架構概覽圖](#1-架構概覽圖)
2. [完整時序圖](#2-完整時序圖)
3. [詳細流程圖](#3-詳細流程圖)
4. [Layer 1: 風控驗證流程](#4-layer-1-風控驗證流程)
5. [Layer 2: 財務驗證流程](#5-layer-2-財務驗證流程)
6. [Layer 3: 活動驗證流程](#6-layer-3-活動驗證流程)
7. [注單狀態機](#7-注單狀態機)
8. [實際計算範例](#8-實際計算範例)

---

## 1. 架構概覽圖

### 1.1 三層驗證架構 (Three-Layer Validation Architecture)

> **圖表複雜度**: 24 個節點（已使用 subgraph 分組優化）
> **閱讀建議**: 按數據流順序閱讀（玩家投注 → Layer 1 → Layer 2 → Layer 3 → 應用場景）
> **關鍵要點**:
> - Layer 1 風控驗證決定 valid_bet
> - Layer 2 僅記錄狀態，不修改 valid_bet
> - Layer 3 應用遊戲權重計算活動貢獻

```mermaid
graph TB
    subgraph "玩家投注"
        A["玩家下注\nAmount: $100\nGame: Baccarat\nOdds: 1.95"]
    end

    subgraph "Layer 1: 風控引擎 (Risk Engine - 05-01)"
        B["對沖檢測\nHedge Detection"]
        C["套利檢測\nArbitrage Detection"]
        D["低賠率過濾\nLow Odds Filter\n閾值: 1.5"]
        E["輸出: valid_bet\n有效投注額\n+ risk_status\n+ filter_reason"]
    end

    subgraph "Layer 2: 財務中心 (Finance Center - 02-04)"
        F["注單結算\nBet Settlement"]
        G["記錄結算狀態\nSettlement Status"]
        H{注單狀態?}
        I["WIN/LOSS\n記錄狀態"]
        J["DRAW/TIE\n記錄狀態"]
        K["VOID/CANCEL\n記錄狀態"]
        L["HALF_WIN/LOSS\n記錄狀態"]
        M["輸出: valid_bet\n有效投注額 (不變)"]
    end

    subgraph "Layer 3: 活動系統 (Activity System - 04-01)"
        N["遊戲權重應用\nGame Weight"]
        O{遊戲類型?}
        P["Slots/Sports\nWeight: 1.0"]
        Q["Baccarat\nWeight: 0.15"]
        R["Blackjack\nWeight: 0.1"]
        S["Roulette\nWeight: 0.2"]
        T["輸出: activity_valid_turnover\n活動有效流水"]
    end

    subgraph "應用場景"
        U["返水計算\nRebate Calculation"]
        V["流水進度追蹤\nWagering Progress"]
        W["VIP升級\nVIP Upgrade"]
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
    style U fill:#f8d7da
    style V fill:#f8d7da
    style W fill:#f8d7da
```

---

## 2. 完整時序圖

### 2.1 流水計算完整時序 (End-to-End Turnover Calculation Sequence)

```mermaid
sequenceDiagram
    autonumber

    participant Player as 👤 玩家
    participant Game as 🎮 遊戲提供商\n(Game Provider)
    participant Platform as 🖥️ 平台核心\n(Platform Core)
    participant Risk as 🛡️ 風控引擎\n(Risk Engine)\n05-01
    participant Finance as 💰 財務中心\n(Finance Center)\n02-04
    participant Activity as 🎁 活動系統\n(Activity System)\n04-01
    participant Wallet as 💳 錢包系統\n(Wallet System)\n02-06
    participant DB as 💾 數據庫

    rect rgb(240, 248, 255)
        Note over Player,Game: ===== 階段1: 投注階段 (Betting Phase) =====
    end

    Player->>Game: 1. 發起投注 (Place Bet)<br/>Amount: $100, Game: Baccarat, Odds: 1.95
    Game->>Platform: 2. Debit 請求 (扣除餘額)
    Platform->>Wallet: 3. Lock 玩家資金
    Wallet->>DB: 4. 更新錢包狀態<br/>playable_balance -= 100
    DB-->>Wallet: 5. 確認扣款成功
    Wallet-->>Platform: 6. 返回 Transaction ID
    Platform-->>Game: 7. Debit Success
    Game-->>Player: 8. 投注確認 (Bet Confirmed)<br/>Round ID: round_12345

    rect rgb(255, 250, 240)
        Note over Player,DB: ===== 階段2: 遊戲結算階段 (Settlement Phase) =====
    end

    Note over Game: 遊戲結果：玩家贏 $195
    Game->>Platform: 9. Credit 請求 (派彩)<br/>Amount: $195, Status: WIN
    Platform->>DB: 10. 記錄注單結果<br/>bet_id, status=WIN, win_amount=195

    rect rgb(240, 255, 240)
        Note over Risk,Activity: ===== 階段3: 流水驗證 (Layer 1 - Risk Engine) =====
    end

    Platform->>Risk: 11. validateTurnover(bet_id)<br/>{bet_amount: 100, odds: 1.95, game: BACCARAT}

    Risk->>Risk: 12a. 檢查對沖投注<br/>(Hedge Detection)
    Note over Risk: 查詢同一玩家、同一Round<br/>是否有相反投注
    Risk->>DB: 12b. Query同局對沖注單
    DB-->>Risk: 12c. 無對沖注單

    Risk->>Risk: 13a. 檢查套利投注<br/>(Arbitrage Detection)
    Note over Risk: 檢查跨平台/跨市場<br/>是否存在套利機會

    Risk->>Risk: 14a. 檢查賠率閾值<br/>(Odds Validation)
    Note over Risk: odds=1.95 >= 1.5 ✓

    Risk-->>Platform: 15. 驗證通過<br/>{is_valid: true,<br/>valid_bet: 100,<br/>risk_status: "PASSED",<br/>filter_reason: null,<br/>risk_rules_applied: []}

    rect rgb(255, 250, 250)
        Note over Finance,Activity: ===== 階段4: 財務狀態記錄 (Layer 2 - Finance) =====
    end

    Platform->>Finance: 16. recordSettlement(bet_id)<br/>{valid_bet: 100,<br/>status: WIN}

    Finance->>Finance: 17. 記錄結算狀態<br/>settlement_status = "WIN"
    Note over Finance: 僅記錄狀態<br/>不修改 valid_bet

    Finance->>Finance: 18. 計算賠付金額<br/>payout_amount = calculatePayout()

    Finance->>DB: 19. 更新注單記錄<br/>UPDATE bets SET<br/>settlement_status='WIN',<br/>payout_amount=195

    Finance-->>Platform: 20. 返回結算結果<br/>{valid_bet: 100 (不變),<br/>settlement_status: 'WIN',<br/>payout_amount: 195}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== 階段5: 遊戲權重應用 (Layer 3 - Activity) =====
    end

    Platform->>Activity: 21. applyGameWeight()<br/>{valid_bet: 100,<br/>game_type: BACCARAT}

    Activity->>DB: 22. 查詢玩家參與的活動<br/>SELECT * FROM player_bonuses<br/>WHERE player_id=xxx<br/>AND status='active'
    DB-->>Activity: 23. 返回活動列表<br/>[{bonus_id: B001,<br/>bonus_type: "DEPOSIT",<br/>wagering_requirement: 5000}]

    Activity->>Activity: 24. 獲取遊戲權重<br/>game_weight = getGameWeight("BACCARAT")
    Note over Activity: Baccarat → 0.15 (15%)<br/>Slots → 1.0 (100%)<br/>Blackjack → 0.1 (10%)

    Activity->>Activity: 25. 計算活動貢獻金額<br/>contributed_amount =<br/>valid_bet × weight<br/>= 100 × 0.15 = $15

    Activity->>DB: 26. 更新流水進度<br/>UPDATE player_bonuses SET<br/>wagering_completed += 15,<br/>wagering_progress = 15/5000

    DB-->>Activity: 27. 更新成功

    Activity-->>Platform: 28. 返回活動貢獻<br/>{contributed_amount: 15,<br/>wagering_progress: "0.3%",<br/>remaining: 4985}

    rect rgb(255, 245, 240)
        Note over Platform,Player: ===== 階段6: 派彩與通知 =====
    end

    Platform->>Wallet: 29. 派彩到錢包<br/>Credit $195 to player
    Wallet->>DB: 30. 更新錢包餘額<br/>playable_balance += 195
    Wallet-->>Platform: 31. 派彩成功

    Platform->>Player: 32. 推送通知<br/>✅ 贏得 $195<br/>💰 流水進度: +$15 (0.3%)

    rect rgb(245, 245, 245)
        Note over Platform,DB: ===== 階段7: 審計日誌 =====
    end

    Platform->>DB: 33. 記錄審計日誌<br/>AuditLog.create({<br/>action: "TURNOVER_CALCULATED",<br/>details: {...}<br/>})
```

---

## 3. 詳細流程圖

### 3.1 流水計算主流程 (Main Turnover Calculation Flow)

> **圖表複雜度**: 26 個節點（已使用 subgraph 分組優化）
> **閱讀建議**: 按照三層架構順序閱讀（Input → Layer 1 → Layer 2 → Layer 3 → Update）
> **關鍵決策點**:
> - Layer 1: 是否對沖/套利/低賠率 → 決定 valid_bet 是否為 0
> - Layer 2: 注單狀態分支（WIN/LOSS/DRAW/VOID/HALF）→ 僅記錄狀態
> - Layer 3: 遊戲類型分支（Slots/Baccarat/Blackjack/Roulette）→ 應用對應權重
> - Update: 流水是否達標 → 決定是否解鎖提款

```mermaid
flowchart TD
    Start([開始: 注單結算])

    subgraph Input["輸入數據"]
        A[注單信息\nbet_id, player_id\nbet_amount: $100\nodds: 1.95\ngame_type: BACCARAT\nstatus: WIN]
    end

    subgraph Layer1["Layer 1: 風控引擎驗證 (05-01)"]
        B{是否對沖投注?}
        C{是否套利投注?}
        D{賠率是否≥1.5?}
        E[有效流水基數\neffective_turnover_base\n= $100]
        F[拒絕\neffective_turnover_base\n= $0]
    end

    subgraph Layer2["Layer 2: 財務狀態記錄 (02-04)"]
        G{注單狀態?}
        H[WIN/LOSS\n記錄狀態]
        I[DRAW/TIE\n記錄狀態]
        J[VOID/CANCEL\n記錄狀態]
        K[HALF_WIN/HALF_LOSS\n記錄狀態]
        L[valid_bet 保持不變\n= Layer 1 輸出\n= $100\n僅記錄 settlement_status]
    end

    subgraph Layer3["Layer 3: 活動權重應用 (04-01)"]
        M{遊戲類型?}
        N[Slots/Sports\ngame_weight = 1.0]
        O[Baccarat\ngame_weight = 0.15]
        P[Blackjack\ngame_weight = 0.1]
        Q[Roulette\ngame_weight = 0.2]
        R[計算活動貢獻\ncontributed_amount\n= valid_bet × weight\n= $100 × 0.15 = $15]
    end

    subgraph Update["更新流水進度"]
        S[更新數據庫]
        T[計算流水進度\nprogress = 15/5000\n= 0.3%]
        U[檢查是否完成流水]
        V{流水是否達標?}
        W[解鎖提款權限\n可提現餘額增加]
        X[保持流水鎖定\n繼續追蹤進度]
    end

    End([結束])

    Start --> Input
    Input --> A
    A --> B

    B -->|是| F
    B -->|否| C
    C -->|是| F
    C -->|否| D
    D -->|否\n低賠率| F
    D -->|是| E

    F --> End
    E --> G

    G -->|WIN/LOSS| H
    G -->|DRAW/TIE| I
    G -->|VOID/CANCEL| J
    G -->|HALF| K

    H --> L
    I --> L
    J --> L
    K --> L

    L --> M

    M -->|Slots/Sports| N
    M -->|Baccarat| O
    M -->|Blackjack| P
    M -->|Roulette| Q

    N --> R
    O --> R
    P --> R
    Q --> R

    R --> S
    S --> T
    T --> U
    U --> V

    V -->|是\ncompleted ≥ required| W
    V -->|否| X

    W --> End
    X --> End

    style Start fill:#e1f5ff
    style End fill:#e1f5ff
    style E fill:#d4edda
    style F fill:#f8d7da
    style L fill:#d4edda
    style R fill:#d1ecf1
    style W fill:#d4edda
    style X fill:#fff3cd
```

---

## 4. Layer 1: 風控驗證流程

### 4.1 對沖檢測流程 (Hedge Detection Flow)

```mermaid
flowchart TD
    A[開始: 對沖檢測]
    B["獲取注單信息\nplayer_id, round_id\nselection, amount"]
    C["查詢同一Round\n該玩家的所有注單"]
    D{"是否存在\n相反投注?"}

    subgraph Example["範例: 百家樂"]
        E1[投注 Banker $1000]
        E2[投注 Player $950]
        E3["對沖檢測: ✓\n相反投注"]
    end

    F[標記為對沖投注]
    G[risk_code = HEDGE_BET]
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

### 4.2 賠率閾值檢測 (Odds Threshold Check)

```mermaid
flowchart TD
    A[開始: 賠率驗證]
    B["讀取配置\nMIN_ODDS_THRESHOLD = 1.5"]
    C["獲取注單賠率\nodds = 1.95"]
    D{odds ≥ threshold?}
    E["賠率合格\n通過驗證"]
    F["低賠率投注\nrisk_code = LOW_ODDS"]
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

---

## 5. Layer 2: 財務狀態記錄流程

### 5.1 結算狀態記錄流程 (Settlement Status Recording)

```mermaid
flowchart TD
    A[開始: 財務層記錄]
    B["輸入: valid_bet\n= $100\n來自 Layer 1,不可變"]
    C["獲取注單狀態\nbet.status"]
    D{注單狀態}

    E["WIN\n玩家贏"]
    F["記錄: settlement_status = WIN\n計算賠付金額"]

    G["LOSS\n玩家輸"]
    H["記錄: settlement_status = LOSS\n計算賠付金額"]

    I["DRAW/TIE\n和局"]
    J["記錄: settlement_status = DRAW\n退還本金"]

    K["VOID/CANCEL\n注單作廢"]
    L["記錄: settlement_status = VOID\n退還本金"]

    M["HALF_WIN/HALF_LOSS\n半贏半輸"]
    N["記錄: settlement_status = HALF_WIN/HALF_LOSS\n計算部分賠付"]

    O["更新數據庫\nsettlement_status\npayout_amount"]

    P["valid_bet 保持不變\n= $100\n不受結算狀態影響"]

    End([返回: valid_bet $100\n+ settlement_status])

    A --> B
    B --> C
    C --> D

    D -->|WIN| E
    D -->|LOSS| G
    D -->|DRAW/TIE| I
    D -->|VOID/CANCEL| K
    D -->|HALF| M

    E --> F
    G --> H
    I --> J
    K --> L
    M --> N

    F --> O
    H --> O
    J --> O
    L --> O
    N --> O

    O --> P
    P --> End

    style P fill:#d4edda
    style F fill:#fff3cd
    style H fill:#fff3cd
    style J fill:#fff3cd
    style L fill:#fff3cd
    style N fill:#fff3cd
```

### 5.2 結算狀態與 Valid Bet 關係對照表

> **重要原則**: Valid Bet 由 Layer 1 風控引擎一次性判定,Layer 2 僅記錄結算狀態,**不修改** Valid Bet 值。

```mermaid
graph LR
    subgraph "注單狀態 (Bet Status)"
        A1["WIN\n玩家贏"]
        A2["LOSS\n玩家輸"]
        A3["DRAW\n和局"]
        A4["TIE\n平局"]
        A5["VOID\n作廢"]
        A6["CANCEL\n取消"]
        A7["HALF_WIN\n半贏"]
        A8["HALF_LOSS\n半輸"]
        A9["RUNNING\n進行中"]
    end

    subgraph "Valid Bet 處理"
        B1[保持不變]
        B2[保持不變]
        B3[保持不變]
        B4[保持不變]
        B5[保持不變]
        B6[保持不變]
        B7[保持不變]
        B8[保持不變]
        B9[保持不變]
    end

    subgraph "說明"
        C1["僅記錄狀態\n計算賠付"]
        C2["僅記錄狀態\n計算賠付"]
        C3["僅記錄狀態\n退還本金"]
        C4["僅記錄狀態\n退還本金"]
        C5["僅記錄狀態\n退還本金"]
        C6["僅記錄狀態\n退還本金"]
        C7["僅記錄狀態\n計算部分賠付"]
        C8["僅記錄狀態\n計算部分賠付"]
        C9["等待結算\n暫不處理"]
    end

    A1 --> B1 --> C1
    A2 --> B2 --> C2
    A3 --> B3 --> C3
    A4 --> B4 --> C4
    A5 --> B5 --> C5
    A6 --> B6 --> C6
    A7 --> B7 --> C7
    A8 --> B8 --> C8
    A9 --> B9 --> C9

    style B1 fill:#d4edda
    style B2 fill:#d4edda
    style B3 fill:#d4edda
    style B4 fill:#d4edda
    style B5 fill:#d4edda
    style B6 fill:#d4edda
    style B7 fill:#d4edda
    style B8 fill:#d4edda
    style B9 fill:#fff3cd
```

---

## 6. Layer 3: 活動驗證流程

### 6.1 遊戲權重應用流程 (Game Weight Application)

```mermaid
flowchart TD
    A[開始: 活動層驗證]
    B["輸入: valid_turnover_finance\n= $100"]
    C["獲取遊戲類型\ngame_type"]
    D{遊戲類型}

    E["Slots\n老虎機"]
    F["game_weight = 1.0\n100% 貢獻"]

    G["Sports\n體育博彩"]
    H["game_weight = 1.0\n100% 貢獻"]

    I["Baccarat\n百家樂"]
    J["game_weight = 0.15\n15% 貢獻"]

    K["Blackjack\n二十一點"]
    L["game_weight = 0.1\n10% 貢獻"]

    M["Roulette\n輪盤"]
    N["game_weight = 0.2\n20% 貢獻"]

    O["Live Casino\n真人娛樂場"]
    P["game_weight = 0.15\n15% 貢獻"]

    Q["計算活動流水\nactivity_valid_turnover\n= finance × weight"]

    R["查詢玩家活動\nplayer_bonuses"]
    S["更新流水進度\nwagering_completed += activity_valid_turnover"]
    T["計算完成百分比\nprogress = completed / required"]

    U{流水是否達標?}
    V["標記活動完成\nstatus = 'completed'"]
    W["解鎖提款\n可提現餘額更新"]
    X["保持追蹤\nstatus = 'active'"]

    End([返回: 流水進度])

    A --> B
    B --> C
    C --> D

    D -->|Slots| E
    D -->|Sports| G
    D -->|Baccarat| I
    D -->|Blackjack| K
    D -->|Roulette| M
    D -->|Live Casino| O

    E --> F
    G --> H
    I --> J
    K --> L
    M --> N
    O --> P

    F --> Q
    H --> Q
    J --> Q
    L --> Q
    N --> Q
    P --> Q

    Q --> R
    R --> S
    S --> T
    T --> U

    U -->|是\ncompleted ≥ required| V
    U -->|否| X

    V --> W
    W --> End
    X --> End

    style F fill:#d4edda
    style H fill:#d4edda
    style J fill:#fff3cd
    style L fill:#fff3cd
    style N fill:#fff3cd
    style P fill:#fff3cd
    style V fill:#d4edda
    style W fill:#d4edda
```

### 6.2 遊戲權重配置表

```mermaid
graph TD
    subgraph "高貢獻遊戲 (High Contribution)"
        A1["🎰 Slots\n老虎機\nWeight: 1.0"]
        A2["⚽ Sports\n體育博彩\nWeight: 1.0"]
        A3["🎲 Dice\n骰寶\nWeight: 1.0"]
    end

    subgraph "中貢獻遊戲 (Medium Contribution)"
        B1["🎡 Roulette\n輪盤\nWeight: 0.2"]
        B2["🃏 Baccarat\n百家樂\nWeight: 0.15"]
        B3["👨‍💼 Live Casino\n真人娛樂\nWeight: 0.15"]
    end

    subgraph "低貢獻遊戲 (Low Contribution)"
        C1["🂡 Blackjack\n二十一點\nWeight: 0.1"]
        C2["🎴 Poker\n撲克\nWeight: 0.05"]
        C3["🎯 Video Poker\n視訊撲克\nWeight: 0.05"]
    end

    style A1 fill:#d4edda
    style A2 fill:#d4edda
    style A3 fill:#d4edda
    style B1 fill:#fff3cd
    style B2 fill:#fff3cd
    style B3 fill:#fff3cd
    style C1 fill:#f8d7da
    style C2 fill:#f8d7da
    style C3 fill:#f8d7da
```

---

## 7. 注單狀態機

### 7.1 注單生命週期 (Bet Lifecycle State Machine)

```mermaid
%%{init: {
  "theme": "base",
  "themeVariables": {
    "primaryColor": "#2d2d2d",
    "primaryTextColor": "#fff",
    "primaryBorderColor": "#00d4ff",
    "lineColor": "#00ff00",
    "secondaryColor": "#006100",
    "tertiaryColor": "#fff",
    "darkMode": true,
    "background": "#1e1e1e"
  }
}}%%

stateDiagram-v2
    direction TB

    %% 狀態定義：使用別名 (Alias) %%
    state "Pending (待處理)" as S_Pending
    state "Running (進行中)" as S_Running
    state "Settlement (結算中心)" as S_Settlement
    state "Turnover Calc (流水計算)" as S_Turnover

    %% 1. 初始階段
    [*] --> S_Pending : 玩家下注\n(扣除餘額)

    %% 修復點：將筆記移到 state 定義之外，並明確指向 S_Pending
    note right of S_Pending : Status: PENDING\nTurnover: 0\nReason: 等待GP確認

    S_Pending --> S_Running : GP確認接受\n(遊戲開始)

    %% 2. 進行階段
    note right of S_Running : Status: RUNNING\nTurnover: 0\nReason: 賽果未出

    S_Running --> S_Settlement : 接收賽果

    %% 3. 結算複合狀態
    state S_Settlement {
        direction TB
        state "WIN (贏)" as Res_Win
        state "LOSS (輸)" as Res_Loss
        state "DRAW (和)" as Res_Draw
        state "VOID (作廢)" as Res_Void
        state "CANCEL (取消)" as Res_Cancel

        [*] --> Res_Win
        [*] --> Res_Loss
        [*] --> Res_Draw
        [*] --> Res_Void
        [*] --> Res_Cancel
        
        %% 內部狀態的筆記
        note right of Res_Win : 賠付因子 > 1.0\n流水 100%
        note right of Res_Draw : 賠付因子 1.0\n流水 0%
    }

    %% 4. 資金流向與流水計算
    S_Settlement --> Paid : WIN (派彩)
    S_Settlement --> Completed : LOSS (結算)
    S_Settlement --> Refunded : DRAW/VOID (退款)

    state S_Turnover {
        state "Valid Turnover (有效流水)" as TO_Yes
        state "Ignored Turnover (無效流水)" as TO_No
        
        Paid --> TO_Yes : 贏單計入
        Completed --> TO_Yes : 輸單計入
        Refunded --> TO_No : 退款不計
    }

    TO_Yes --> [*]
    TO_No --> [*]
```

---

## 8. 實際計算範例

### 8.1 範例1: 百家樂投注 (Baccarat Bet)

**場景**: 玩家參與「首存100%紅利」活動，需完成 5x 流水要求 (Wagering Requirement)。

**投注詳情**:
- **存款金額**: $1,000
- **紅利金額**: $1,000 (100% Match)
- **流水要求**: ($1,000 + $1,000) × 5 = **$10,000**
- **當前投注**: 百家樂投注 $100，賠率 1.95，結果 WIN

**計算流程**:

```mermaid
graph TB
    subgraph "輸入"
        A["投注金額: $100\n遊戲: Baccarat\n賠率: 1.95\n結果: WIN"]
    end

    subgraph "Layer 1: 風控 (05-01)"
        B1["對沖檢測: ✓ 通過"]
        B2["套利檢測: ✓ 通過"]
        B3["賠率檢測: 1.95 ≥ 1.5 ✓"]
        B4["effective_turnover_base\n= $100"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["狀態: WIN"]
        C2["status_factor = 1.0"]
        C3["valid_turnover_finance\n= $100 × 1.0\n= $100"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["遊戲: Baccarat"]
        D2["game_weight = 0.15"]
        D3["activity_valid_turnover\n= $100 × 0.15\n= $15"]
    end

    subgraph "流水進度更新"
        E1["累計流水: $15"]
        E2["流水進度: 15 / 10,000\n= 0.15%"]
        E3["還需流水: $9,985"]
    end

    A --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> E1
    E1 --> E2
    E2 --> E3

    style B4 fill:#fff3cd
    style C3 fill:#d4edda
    style D3 fill:#d1ecf1
    style E2 fill:#f8d7da
```

**結論**:
- ✅ 風控驗證通過
- ✅ 財務流水: $100
- ⚠️ 活動流水: $15 (僅15%貢獻)
- ⚠️ 需要更多投注才能完成流水要求

---

### 8.2 範例2: 老虎機投注 (Slots Bet)

**場景**: 同一玩家改玩老虎機，期望更快完成流水。

**投注詳情**:
- **當前流水進度**: $15 / $10,000 (0.15%)
- **當前投注**: 老虎機投注 $100，結果 LOSS

**計算流程**:

```mermaid
graph TB
    subgraph "輸入"
        A["投注金額: $100\n遊戲: Slots\n結果: LOSS"]
    end

    subgraph "Layer 1: 風控 (05-01)"
        B1["老虎機無對沖風險 ✓"]
        B2["無需賠率檢測 ✓"]
        B3["effective_turnover_base\n= $100"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["狀態: LOSS"]
        C2["status_factor = 1.0\n(輸也計流水)"]
        C3["valid_turnover_finance\n= $100 × 1.0\n= $100"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["遊戲: Slots"]
        D2["game_weight = 1.0\n(100%貢獻)"]
        D3["activity_valid_turnover\n= $100 × 1.0\n= $100"]
    end

    subgraph "流水進度更新"
        E1["本次流水: $100"]
        E2["累計流水: $15 + $100\n= $115"]
        E3["流水進度: 115 / 10,000\n= 1.15%"]
        E4["還需流水: $9,885"]
    end

    A --> B1
    B1 --> B2
    B2 --> B3
    B3 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> E1
    E1 --> E2
    E2 --> E3
    E3 --> E4

    style B3 fill:#fff3cd
    style C3 fill:#d4edda
    style D3 fill:#d4edda
    style E3 fill:#d1ecf1
```

**對比分析**:

| 維度 | 百家樂 (Baccarat) | 老虎機 (Slots) |
|------|------------------|---------------|
| 投注金額 | $100 | $100 |
| 風控驗證 | 需檢查對沖/賠率 | 簡化驗證 |
| 財務流水 | $100 | $100 |
| 遊戲權重 | 0.15 (15%) | 1.0 (100%) |
| **活動流水** | **$15** | **$100** |
| 流水效率 | 低 (6.67倍投注才完成) | 高 (1倍投注完成) |

**結論**:
- ✅ 老虎機流水貢獻率高 (100%)
- ✅ 適合快速完成流水要求
- ⚠️ 但老虎機 RTP 較高，玩家期望值更好

---

### 8.3 範例3: 對沖投注被拒 (Hedge Bet Rejected)

**場景**: 玩家嘗試在同一局百家樂同時投注 Banker 和 Player。

**投注詳情**:
- **投注1**: Banker $1,000 (賠率 1.95)
- **投注2**: Player $950 (賠率 2.00)
- **遊戲結果**: Banker 贏

**計算流程**:

```mermaid
graph TB
    subgraph "輸入"
        A1["注單1: Banker $1,000"]
        A2["注單2: Player $950"]
        A3["同一Round ID"]
    end

    subgraph "Layer 1: 風控 (05-01)"
        B1["檢測到對沖投注"]
        B2["round_id 相同 ✓"]
        B3["selection 相反 ✓"]
        B4["對沖檢測觸發"]
        B5["risk_code = HEDGE_BET"]
        B6["effective_turnover_base\n= $0"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["接收 base = $0"]
        C2["valid_turnover_finance\n= $0"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["接收 finance = $0"]
        D2["activity_valid_turnover\n= $0"]
    end

    subgraph "流水進度"
        E1["本次流水: $0"]
        E2["流水進度: 無變化"]
        E3["系統警告:\n檢測到對沖投注"]
    end

    A1 --> B1
    A2 --> B1
    A3 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> B5
    B5 --> B6
    B6 --> C1
    C1 --> C2
    C2 --> D1
    D1 --> D2
    D2 --> E1
    E1 --> E2
    E2 --> E3

    style B5 fill:#f8d7da
    style B6 fill:#f8d7da
    style C2 fill:#f8d7da
    style D2 fill:#f8d7da
    style E3 fill:#f8d7da
```


**結論**:
- ❌ 對沖投注被風控拒絕
- ❌ 兩筆投注流水均為 $0
- ⚠️ 可能觸發風控標籤 (Risk Tag: HEDGE_BETTOR)

---

## 9. Layer 2 不修改 Valid Bet 的設計原則

### 9.1 核心原則

> **關鍵設計決策**: Layer 2 (財務中心) 僅記錄結算狀態,**不修改** Layer 1 (風控引擎) 確定的 Valid Bet 值。

### 9.2 為什麼財務層不應該修改 Valid Bet?

#### 9.2.1 違反「風控完全獨立」原則

**問題**:
- 如果 Layer 1 已經確定了 valid_bet,為何 Layer 2 還要根據結算狀態動態修改?
- 這意味著風控判定並非最終決策,存在邏輯矛盾

**正確做法**:
```
Layer 1 (風控引擎): 一次性判定 valid_bet
  ├─ 通過 → valid_bet = bet_amount, risk_status = "PASSED"
  └─ 拒絕 → valid_bet = 0, risk_status = "FILTERED", reason = "對沖投注"

Layer 2 (財務中心): 僅記錄結算狀態,不修改 valid_bet
  ├─ settlement_status = "WIN/LOSS/HALF_WIN/HALF_LOSS/DRAW"
  └─ payout_amount = calculatePayout(bet, status)
```

#### 9.2.2 違反公平性原則 (Error #3 - 實際風險法)

**問題場景**:
```
兩位玩家都投注 100 元在體育博彩「讓 -0.25」:
- 玩家 A 的比賽結果: 全贏 → valid_bet = 100 元 ✓
- 玩家 B 的比賽結果: 平局(輸半) → valid_bet = 50 元 ❌ (錯誤)

矛盾:
- 相同的投注行為
- 相同的風險暴露 (100 元)
- 但 valid_bet 不同 → 違反公平性原則
```

**正確做法 (標準本金法 - 業界標準)**:
```
玩家 A: 投注 100 元 → 全贏 → valid_bet = 100 元
玩家 B: 投注 100 元 → 輸半 → valid_bet = 100 元 (不是 50!)

理由:
- 玩家下注時承擔的風險都是 100 元
- Valid Bet 應該反映投注行為,而非結算結果
- 簡化計算,不需要等結算才知道 valid_bet
```

#### 9.2.3 業界標準對照

| 營運商 | Valid Bet 計算方法 | 結算狀態影響 | 說明 |
|-------|------------------|------------|------|
| **Pinnacle** | 標準本金法 | 無影響 | Valid Bet = 投注本金 |
| **Betfair** | 標準本金法 | 無影響 | 不論輸贏,全額計入 |
| **Pragmatic Play** | 標準本金法 | 無影響 | 業界遊戲提供商標準 |
| **Evolution Gaming** | 標準本金法 | 無影響 | 真人娛樂業界標準 |
| **❌ 實際風險法** | 動態調整 | 受影響 | 已廢棄,違反公平性 |

### 9.3 Settlement Status 與 Valid Bet 的分離

```mermaid
graph LR
    subgraph "Layer 1 輸出"
        A[valid_bet = $100]
        B[risk_status = PASSED]
        C[filter_reason = null]
    end

    subgraph "Layer 2 記錄"
        D[settlement_status = HALF_WIN]
        E[payout_amount = $145]
        F[valid_bet 保持 $100]
    end

    subgraph "Layer 3 應用"
        G[contributed_amount]
        H[= valid_bet × weight]
        I[= $100 × 0.15 = $15]
    end

    A --> F
    F --> G
    D -.僅用於計算賠付.-> E
    G --> H --> I

    style F fill:#d4edda
    style A fill:#fff3cd
```

### 9.4 實施建議


---

## 10. 風控標記機制

### 10.1 為什麼需要風控標記?

**問題**:
- 當前設計僅記錄最終的 valid_bet 值
- 缺失: 沒有記錄「為什麼」流水是這個值
- **影響**: 無法審計追溯、無法回推重算

**解決方案**:
增加四個風控標記字段:
1. `risk_status`: 風控檢查結果 (PASSED/FILTERED/PENDING)
2. `filter_reason`: 過濾原因文字說明
3. `risk_rules_applied`: 應用的風控規則 (JSON數組)
4. `calculation_version`: 計算規則版本號

### 10.2 風控標記字段定義

#### 10.2.1 risk_status (風控狀態)

| 狀態 | 說明 | valid_bet | 使用場景 |
|------|------|-----------|---------|
| **PASSED** | 風控檢查通過 | = bet_amount | 正常投注 |
| **FILTERED** | 風控過濾拒絕 | = 0 | 對沖、套利、低賠率 |
| **PENDING** | 等待人工審核 | = 0 (暫不計入) | 可疑交易、高額投注 |

#### 10.2.2 filter_reason (過濾原因)

| risk_status | filter_reason 範例 | 說明 |
|-------------|------------------|------|
| PASSED | null | 無過濾 |
| FILTERED | "對沖投注: 同時投注 Banker/Player" | 具體原因 |
| FILTERED | "低賠率投注: odds=1.30 < 閾值 1.50" | 具體原因 |
| FILTERED | "套利投注: 跨平台賠率差異 > 5%" | 具體原因 |
| PENDING | "高額投注需人工審核: 金額 > $10,000" | 等待審核 |

#### 10.2.3 risk_rules_applied (應用的規則)

**JSON 格式範例**:
```json
{
  "risk_rules_applied": [
    {
      "rule_id": "HEDGE_001",
      "rule_name": "百家樂對沖檢測",
      "action": "FILTER",
      "confidence": 0.95
    },
    {
      "rule_id": "ODDS_001",
      "rule_name": "賠率閾值檢查",
      "action": "PASS",
      "threshold": 1.5,
      "actual_odds": 1.95
    }
  ]
}
```

#### 10.2.4 calculation_version (計算版本)

**用途**: 支持規則調整後的回推重算

| 版本 | 變更內容 | 生效日期 |
|------|---------|---------|
| v1.0.0 | 初始版本,使用實際風險法 | 2026-01-01 |
| v1.1.0 | 修正為標準本金法 | 2026-01-28 |
| v1.2.0 | 調整賠率閾值 1.3 → 1.5 | 2026-02-01 |

### 10.3 審計追溯示例

**查詢: 為什麼這筆投注 valid_bet = 0?**


**結果**:
```
bet_id: BET_12345
bet_amount: 100.00
valid_bet: 0.00
risk_status: FILTERED
filter_reason: "對沖投注: 同時投注 Banker (1000元) 和 Player (950元)"
risk_rules_applied: [{"rule_id": "HEDGE_001", "action": "FILTER"}]
```

**結論**: 該投注被風控系統識別為對沖投注,因此 valid_bet = 0。

### 10.4 回推重算支持

**場景**: 風控規則調整後,需要重新計算歷史數據


---

## 11. 總結

### 11.1 核心設計原則

1. **三層驗證架構** (Three-Layer Architecture)
   - Layer 1 (風控引擎): 一次性判定 valid_bet,標記 risk_status
   - Layer 2 (財務中心): 僅記錄結算狀態,**不修改** valid_bet
   - Layer 3 (活動系統): 應用遊戲權重,計算活動貢獻

2. **風控完全獨立原則**
   - Layer 1 確定 valid_bet 後,後續層級不再修改
   - 採用「標準本金法」: valid_bet = bet_amount (不論結果)
   - 結算狀態僅用於計算賠付金額,與流水計算分離

3. **關注點分離** (Separation of Concerns)
   - 風控關注: 對沖、套利、賠率過濾
   - 財務關注: 結算狀態、賠付金額計算
   - 活動關注: 遊戲權重應用

4. **審計追溯支持**
   - 記錄原始數據 + 計算結果 + 版本號
   - 提供回推重算接口
   - 完整的風控標記 (risk_status, filter_reason)

### 11.2 關鍵術語定義

> **術語標準化**: 見 [術語標準化定義](../../00_Concept_&_Analysis/00-03_Terminology_Standards.md)

| 中文 | 英文 | 定義 | 單位 | 用途 |
|------|------|------|------|------|
| **投注額** | Bet Amount | 單筆原始投注金額 | 單筆 | API 交互、資金扣除 |
| **流水** | Turnover | 投注額的時間累積總和 | 累積 | GGR 計算、財務報表 |
| **有效投注額** | Valid Bet | 經風控過濾的單筆金額 | 單筆 | 流水要求、返水、VIP |
| **流水要求** | Wagering Requirement | 必須達成的有效投注總額 | 累積 | 活動驗證、取款限制 |

### 11.3 數據流關鍵指標

| 階段 | 指標 | 計算公式 | 說明 |
|------|------|---------|------|
| **Layer 1 輸出** | valid_bet | 風控判定 (bet_amount or 0) | 一次性確定,不可變 |
| **Layer 2 記錄** | settlement_status | 記錄結算狀態 | 不影響 valid_bet |
| **Layer 3 輸出** | contributed_amount | valid_bet × game_weight | 活動貢獻金額 |
| **進度追蹤** | wagering_progress | Σ contributed / requirement × 100% | 流水完成百分比 |

### 11.4 常見問題 (FAQ)

**Q1: 為什麼 DRAW/TIE 不計流水?**
- A: 和局時玩家本金返還,無實際風險,因此不計入流水要求。

**Q2: 為什麼百家樂權重只有15%?**
- A: 百家樂 RTP 高 (98.94%),平台風險低,為防止流水濫用,降低權重。

**Q3: 對沖投注如何檢測?**
- A: 系統檢查同一玩家在同一局遊戲中是否有相反投注 (如同時投注 Banker/Player)。

**Q4: 流水計算是實時的嗎?**
- A: 是的,每筆注單結算後立即計算流水並更新進度。

---

## 📚 相關文檔

### 核心參考
- [05-01 風控系統](IGaming需求框架/05_Risk_Management/05-01_Risk_Control_System.md) - Layer 1 基礎驗證
- [02-04 流水與對帳](IGaming需求框架/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - Layer 2 狀態因子
- [04-01 活動系統](IGaming需求框架/04_Activity_Center/04-01_Activity_System_Design.md) - Layer 3 遊戲權重

### 相關系統
- [02-06 統一錢包](IGaming需求框架/02_Finance_Center/02-06_Unified_Wallet_Model.md) - 流水鎖定機制
- [03-01 遊戲整合](IGaming需求框架/03_Game_Center/03-01_Game_Integration_Standard.md) - 注單結算流程

---

**文檔版本**: 1.0.0
**創建日期**: 2026-01-27
**維護團隊**: Product Team & Tech Architecture Team
