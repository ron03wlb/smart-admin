# 有效投注額計算流程圖與時序圖

> **Canonical Source**: [source-archive/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md)
> **Audience**: 架構師、後端開發人員、數據工程師
> **Business Requirements**: [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)
> **Last Synced**: 2026-02-08

---

## 文件目的

本文件提供 iGaming 平台有效投注額 (Valid Turnover) 計算系統的技術視覺化呈現，包括三層驗證架構、詳細時序圖、各層流程圖，以及投注生命週期狀態機。所有圖表均供實作或維護有效投注額計算管線的工程團隊使用。

**版本歷史**:
- v4.0.0 (2026-01-28): 修正 Layer 2 邏輯——移除錯誤的 `status_factor` 動態修改 `valid_bet`；採用標準本金法 (Standard Principal Method)；新增風險標記機制與稽核追溯支援
- v3.0.0 (2026-01-27): 初始建立

**關鍵設計決策**:
- Layer 1 (風控引擎 Risk Engine) 一次性決定 `valid_bet`
- Layer 2 (財務中心 Finance Center) 僅記錄結算狀態——**不修改** `valid_bet`
- Layer 3 (活動系統 Activity System) 應用遊戲權重計算活動貢獻

**參考文件**:
- 術語標準 *(規劃中 - source-archive/00_Foundation/00-03_Terminology_Standards)*
- [Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md)
- [Turnover and Reconciliation Analysis](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md)
- [Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md)

---

## 目錄

1. [架構概覽](#1-架構概覽)
2. [端對端時序圖](#2-端對端時序圖)
3. [主流程圖](#3-主流程圖)
4. [Layer 1: 風控引擎驗證](#4-layer-1-風控引擎驗證)
5. [Layer 2: 財務結算記錄](#5-layer-2-財務結算記錄)
6. [Layer 3: 活動權重應用](#6-layer-3-活動權重應用)
7. [投注生命週期狀態機](#7-投注生命週期狀態機)
8. [計算範例](#8-計算範例)
9. [風險標記機制](#9-風險標記機制)
10. [設計原則：Layer 2 不修改 Valid Bet](#10-設計原則layer-2-不修改-valid-bet)

---

## 1. 架構概覽

### 1.1 三層驗證架構

有效投注額計算系統採用三層驗證架構。資料從玩家投注開始，依序流經 Layer 1 (風控)、Layer 2 (財務)、Layer 3 (活動)，最後到達應用場景。

> **圖表複雜度**: 24 個節點（透過 subgraph 分組優化）
> **閱讀指南**: 按資料流順序閱讀（玩家投注 -> Layer 1 -> Layer 2 -> Layer 3 -> 應用場景）
> **關鍵要點**:
> - Layer 1 風控驗證決定 `valid_bet`
> - Layer 2 僅記錄狀態，**不修改** `valid_bet`
> - Layer 3 應用遊戲權重計算活動貢獻

```mermaid
graph TB
    subgraph "Player Bet"
        A["Player Places Bet<br/>Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95"]
    end

    subgraph "Layer 1: Risk Engine (05-01)"
        B["Hedge Detection"]
        C["Arbitrage Detection"]
        D["Low Odds Filter<br/>Threshold: 1.5"]
        E["Output: valid_bet<br/>+ risk_status<br/>+ filter_reason"]
    end

    subgraph "Layer 2: Finance Center (02-04)"
        F["Bet Settlement"]
        G["Record Settlement Status"]
        H{Bet Status?}
        I["WIN/LOSS<br/>Record Status"]
        J["DRAW/TIE<br/>Record Status"]
        K["VOID/CANCEL<br/>Record Status"]
        L["HALF_WIN/LOSS<br/>Record Status"]
        M["Output: valid_bet<br/>(unchanged)"]
    end

    subgraph "Layer 3: Activity System (04-01)"
        N["Game Weight<br/>Application"]
        O{Game Type?}
        P["Slots/Sports<br/>Weight: 1.0"]
        Q["Baccarat<br/>Weight: 0.15"]
        R["Blackjack<br/>Weight: 0.1"]
        S["Roulette<br/>Weight: 0.2"]
        T["Output: activity_valid_turnover"]
    end

    subgraph "Application Scenarios"
        U["Rebate Calculation"]
        V["Wagering Progress<br/>Tracking"]
        W["VIP Upgrade"]
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

## 2. 端對端時序圖

### 2.1 完整有效投注額計算時序

此時序圖展示投注從下注到結算、三層驗證、派彩及稽核日誌的完整生命週期。

```mermaid
sequenceDiagram
    autonumber

    participant Player as Player
    participant Game as Game Provider<br/>(GP)
    participant Platform as Platform Core
    participant Risk as Risk Engine<br/>(Layer 1)
    participant Finance as Finance Center<br/>(Layer 2)
    participant Activity as Activity System<br/>(Layer 3)
    participant Wallet as Wallet System
    participant DB as Database

    rect rgb(240, 248, 255)
        Note over Player,Game: ===== 階段 1: 投注階段 =====
    end

    Player->>Game: 1. Place Bet<br/>Amount: $100, Game: Baccarat, Odds: 1.95
    Game->>Platform: 2. Debit Request (deduct balance)
    Platform->>Wallet: 3. Lock player funds
    Wallet->>DB: 4. Update wallet state<br/>playable_balance -= 100
    DB-->>Wallet: 5. Deduction confirmed
    Wallet-->>Platform: 6. Return Transaction ID
    Platform-->>Game: 7. Debit Success
    Game-->>Player: 8. Bet Confirmed<br/>Round ID: round_12345

    rect rgb(255, 250, 240)
        Note over Player,DB: ===== 階段 2: 遊戲結算階段 =====
    end

    Note over Game: Game Result: Player wins $195
    Game->>Platform: 9. Credit Request (payout)<br/>Amount: $195, Status: WIN
    Platform->>DB: 10. Record bet result<br/>bet_id, status=WIN, win_amount=195

    rect rgb(240, 255, 240)
        Note over Risk,Activity: ===== 階段 3: 有效投注額驗證 (Layer 1 - Risk Engine) =====
    end

    Platform->>Risk: 11. validateTurnover(bet_id)<br/>{bet_amount: 100, odds: 1.95, game: BACCARAT}

    Risk->>Risk: 12a. Hedge Detection
    Note over Risk: Query same player, same round<br/>for opposing bets
    Risk->>DB: 12b. Query hedging bets in same round
    DB-->>Risk: 12c. No hedging bets found

    Risk->>Risk: 13a. Arbitrage Detection
    Note over Risk: Check cross-platform/cross-market<br/>arbitrage opportunities

    Risk->>Risk: 14a. Odds Threshold Validation
    Note over Risk: odds=1.95 >= 1.5 PASS

    Risk-->>Platform: 15. Validation Passed<br/>{is_valid: true,<br/>valid_bet: 100,<br/>risk_status: "PASSED",<br/>filter_reason: null,<br/>risk_rules_applied: []}

    rect rgb(255, 250, 250)
        Note over Finance,Activity: ===== 階段 4: 財務狀態記錄 (Layer 2) =====
    end

    Platform->>Finance: 16. recordSettlement(bet_id)<br/>{valid_bet: 100,<br/>status: WIN}

    Finance->>Finance: 17. Record settlement status<br/>settlement_status = "WIN"
    Note over Finance: Only records status<br/>Does NOT modify valid_bet

    Finance->>Finance: 18. Calculate payout amount<br/>payout_amount = calculatePayout()

    Finance->>DB: 19. Update bet record<br/>UPDATE bets SET<br/>settlement_status='WIN',<br/>payout_amount=195

    Finance-->>Platform: 20. Return settlement result<br/>{valid_bet: 100 (unchanged),<br/>settlement_status: 'WIN',<br/>payout_amount: 195}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== 階段 5: 遊戲權重應用 (Layer 3) =====
    end

    Platform->>Activity: 21. applyGameWeight()<br/>{valid_bet: 100,<br/>game_type: BACCARAT}

    Activity->>DB: 22. Query player's active bonuses<br/>SELECT * FROM player_bonuses<br/>WHERE player_id=xxx<br/>AND status='active'
    DB-->>Activity: 23. Return bonus list<br/>[{bonus_id: B001,<br/>bonus_type: "DEPOSIT",<br/>wagering_requirement: 5000}]

    Activity->>Activity: 24. Get game weight<br/>game_weight = getGameWeight("BACCARAT")
    Note over Activity: Baccarat = 0.15 (15%)<br/>Slots = 1.0 (100%)<br/>Blackjack = 0.1 (10%)

    Activity->>Activity: 25. Calculate activity contribution<br/>contributed_amount =<br/>valid_bet x weight<br/>= 100 x 0.15 = $15

    Activity->>DB: 26. Update wagering progress<br/>UPDATE player_bonuses SET<br/>wagering_completed += 15,<br/>wagering_progress = 15/5000

    DB-->>Activity: 27. Update successful

    Activity-->>Platform: 28. Return activity contribution<br/>{contributed_amount: 15,<br/>wagering_progress: "0.3%",<br/>remaining: 4985}

    rect rgb(255, 245, 240)
        Note over Platform,Player: ===== 階段 6: 派彩與通知 =====
    end

    Platform->>Wallet: 29. Credit payout to wallet<br/>Credit $195 to player
    Wallet->>DB: 30. Update wallet balance<br/>playable_balance += 195
    Wallet-->>Platform: 31. Payout successful

    Platform->>Player: 32. Push notification<br/>Won $195<br/>Wagering progress: +$15 (0.3%)

    rect rgb(245, 245, 245)
        Note over Platform,DB: ===== 階段 7: 稽核日誌 =====
    end

    Platform->>DB: 33. Record audit log<br/>AuditLog.create({<br/>action: "TURNOVER_CALCULATED",<br/>details: {...}<br/>})
```

---

## 3. 主流程圖

### 3.1 主流程

> **圖表複雜度**: 26 個節點（透過 subgraph 分組優化）
> **閱讀指南**: 按三層架構順序閱讀（輸入 -> Layer 1 -> Layer 2 -> Layer 3 -> 更新）
> **關鍵決策點**:
> - Layer 1: 對沖/套利/低賠率 -> 決定 `valid_bet` 是否為 0
> - Layer 2: 投注狀態分支 (WIN/LOSS/DRAW/VOID/HALF) -> 僅記錄狀態
> - Layer 3: 遊戲類型分支 (Slots/Baccarat/Blackjack/Roulette) -> 應用對應權重
> - 更新: 是否達成流水要求？ -> 決定是否解鎖提款

```mermaid
flowchart TD
    Start([Start: Bet Settlement])

    subgraph Input["Input Data"]
        A[Bet Information<br/>bet_id, player_id<br/>bet_amount: $100<br/>odds: 1.95<br/>game_type: BACCARAT<br/>status: WIN]
    end

    subgraph Layer1["Layer 1: Risk Engine Validation (05-01)"]
        B{Hedge Bet?}
        C{Arbitrage Bet?}
        D{Odds >= 1.5?}
        E[effective_turnover_base<br/>= $100]
        F[Rejected<br/>effective_turnover_base<br/>= $0]
    end

    subgraph Layer2["Layer 2: Finance Status Recording (02-04)"]
        G{Bet Status?}
        H[WIN/LOSS<br/>Record Status]
        I[DRAW/TIE<br/>Record Status]
        J[VOID/CANCEL<br/>Record Status]
        K[HALF_WIN/HALF_LOSS<br/>Record Status]
        L[valid_bet unchanged<br/>= Layer 1 output<br/>= $100<br/>Only records settlement_status]
    end

    subgraph Layer3["Layer 3: Activity Weight Application (04-01)"]
        M{Game Type?}
        N[Slots/Sports<br/>game_weight = 1.0]
        O[Baccarat<br/>game_weight = 0.15]
        P[Blackjack<br/>game_weight = 0.1]
        Q[Roulette<br/>game_weight = 0.2]
        R[Calculate activity contribution<br/>contributed_amount<br/>= valid_bet x weight<br/>= $100 x 0.15 = $15]
    end

    subgraph Update["Update Wagering Progress"]
        S[Update Database]
        T[Calculate wagering progress<br/>progress = 15/5000<br/>= 0.3%]
        U[Check if wagering complete]
        V{Wagering Met?}
        W[Unlock withdrawal<br/>Withdrawable balance updated]
        X[Keep tracking<br/>Continue tracking progress]
    end

    End([End])

    Start --> Input
    Input --> A
    A --> B

    B -->|Yes| F
    B -->|No| C
    C -->|Yes| F
    C -->|No| D
    D -->|No<br/>Low Odds| F
    D -->|Yes| E

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

    V -->|Yes<br/>completed >= required| W
    V -->|No| X

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

## 4. Layer 1: 風控引擎驗證

### 4.1 對沖偵測流程

對沖偵測流程識別玩家在同一遊戲局中下注對立選項的情況（例如：在百家樂中同時投注莊家和閒家）。

```mermaid
flowchart TD
    A[Start: Hedge Detection]
    B["Get bet info<br/>player_id, round_id<br/>selection, amount"]
    C["Query all bets by this player<br/>in the same round"]
    D{"Opposing bets<br/>found?"}

    subgraph Example["Example: Baccarat"]
        E1[Bet Banker $1000]
        E2[Bet Player $950]
        E3["Hedge detected: YES<br/>Opposing bets"]
    end

    F[Mark as hedge bet]
    G[risk_code = HEDGE_BET]
    H[effective_turnover = 0]
    I[No hedge detected]
    J[Proceed to next validation]

    End([End: Return validation result])

    A --> B
    B --> C
    C --> D
    D -->|Yes| F
    D -->|No| I

    F --> G
    G --> H
    H --> End

    I --> J
    J --> End

    style F fill:#f8d7da
    style H fill:#f8d7da
    style I fill:#d4edda
```

### 4.2 賠率門檻檢查

低賠率投注被過濾，以防止玩家利用幾乎必贏的結果來完成流水要求 (Wagering Requirement)。

```mermaid
flowchart TD
    A[Start: Odds Validation]
    B["Read config<br/>MIN_ODDS_THRESHOLD = 1.5"]
    C["Get bet odds<br/>odds = 1.95"]
    D{odds >= threshold?}
    E["Odds qualified<br/>Validation passed"]
    F["Low odds bet<br/>risk_code = LOW_ODDS"]
    G[effective_turnover = 0]

    subgraph Examples["Odds Examples"]
        EX1[1.95 -> Pass]
        EX2[1.50 -> Pass]
        EX3[1.30 -> Reject]
        EX4[1.01 -> Reject]
    end

    End([Return validation result])

    A --> B
    B --> C
    C --> D
    D -->|Yes| E
    D -->|No| F

    E --> End
    F --> G
    G --> End

    style E fill:#d4edda
    style F fill:#f8d7da
    style G fill:#f8d7da
```

---

## 5. Layer 2: 財務結算記錄

### 5.1 結算狀態記錄流程

Layer 2 接收 Layer 1 輸出的 `valid_bet` 值作為不可變輸入。它記錄結算狀態並計算派彩金額，但**永不修改** `valid_bet` 值。

```mermaid
flowchart TD
    A[Start: Finance Layer Recording]
    B["Input: valid_bet<br/>= $100<br/>From Layer 1, immutable"]
    C["Get bet status<br/>bet.status"]
    D{Bet Status}

    E["WIN<br/>Player wins"]
    F["Record: settlement_status = WIN<br/>Calculate payout"]

    G["LOSS<br/>Player loses"]
    H["Record: settlement_status = LOSS<br/>Calculate payout"]

    I["DRAW/TIE"]
    J["Record: settlement_status = DRAW<br/>Return principal"]

    K["VOID/CANCEL"]
    L["Record: settlement_status = VOID<br/>Return principal"]

    M["HALF_WIN/HALF_LOSS"]
    N["Record: settlement_status = HALF_WIN/HALF_LOSS<br/>Calculate partial payout"]

    O["Update database<br/>settlement_status<br/>payout_amount"]

    P["valid_bet unchanged<br/>= $100<br/>Not affected by settlement status"]

    End([Return: valid_bet $100<br/>+ settlement_status])

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

### 5.2 結算狀態與 Valid Bet 對照表

**核心原則**: `valid_bet` 由 Layer 1 (風控引擎) 一次性決定。Layer 2 僅記錄結算狀態，**不修改** `valid_bet` 值。

```mermaid
graph LR
    subgraph "Bet Status"
        A1["WIN"]
        A2["LOSS"]
        A3["DRAW"]
        A4["TIE"]
        A5["VOID"]
        A6["CANCEL"]
        A7["HALF_WIN"]
        A8["HALF_LOSS"]
        A9["RUNNING"]
    end

    subgraph "Valid Bet Treatment"
        B1[Unchanged]
        B2[Unchanged]
        B3[Unchanged]
        B4[Unchanged]
        B5[Unchanged]
        B6[Unchanged]
        B7[Unchanged]
        B8[Unchanged]
        B9[Unchanged]
    end

    subgraph "Action"
        C1["Record status<br/>Calculate payout"]
        C2["Record status<br/>Calculate payout"]
        C3["Record status<br/>Return principal"]
        C4["Record status<br/>Return principal"]
        C5["Record status<br/>Return principal"]
        C6["Record status<br/>Return principal"]
        C7["Record status<br/>Partial payout"]
        C8["Record status<br/>Partial payout"]
        C9["Await settlement<br/>No action yet"]
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

## 6. Layer 3: 活動權重應用

### 6.1 遊戲權重應用流程

Layer 3 接收管線驗證後的 `valid_bet`，並應用遊戲特定權重計算活動貢獻金額。

```mermaid
flowchart TD
    A[Start: Activity Layer Validation]
    B["Input: valid_turnover_finance<br/>= $100"]
    C["Get game_type"]
    D{Game Type}

    E["Slots"]
    F["game_weight = 1.0<br/>100% contribution"]

    G["Sports"]
    H["game_weight = 1.0<br/>100% contribution"]

    I["Baccarat"]
    J["game_weight = 0.15<br/>15% contribution"]

    K["Blackjack"]
    L["game_weight = 0.1<br/>10% contribution"]

    M["Roulette"]
    N["game_weight = 0.2<br/>20% contribution"]

    O["Live Casino"]
    P["game_weight = 0.15<br/>15% contribution"]

    Q["Calculate activity turnover<br/>activity_valid_turnover<br/>= finance x weight"]

    R["Query player bonuses<br/>player_bonuses"]
    S["Update wagering progress<br/>wagering_completed += activity_valid_turnover"]
    T["Calculate completion percentage<br/>progress = completed / required"]

    U{Wagering Met?}
    V["Mark activity completed<br/>status = 'completed'"]
    W["Unlock withdrawal<br/>Withdrawable balance updated"]
    X["Keep tracking<br/>status = 'active'"]

    End([Return: wagering progress])

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

    U -->|Yes<br/>completed >= required| V
    U -->|No| X

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
    subgraph "High Contribution Games"
        A1["Slots<br/>Weight: 1.0"]
        A2["Sports<br/>Weight: 1.0"]
        A3["Dice<br/>Weight: 1.0"]
    end

    subgraph "Medium Contribution Games"
        B1["Roulette<br/>Weight: 0.2"]
        B2["Baccarat<br/>Weight: 0.15"]
        B3["Live Casino<br/>Weight: 0.15"]
    end

    subgraph "Low Contribution Games"
        C1["Blackjack<br/>Weight: 0.1"]
        C2["Poker<br/>Weight: 0.05"]
        C3["Video Poker<br/>Weight: 0.05"]
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

## 7. 投注生命週期狀態機

### 7.1 投注生命週期（狀態機）

此狀態圖模擬投注從下注到結算及有效投注額計算的完整生命週期。

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

    state "Pending" as S_Pending
    state "Running" as S_Running
    state "Settlement" as S_Settlement
    state "Turnover Calc" as S_Turnover

    [*] --> S_Pending : Player places bet

    note right of S_Pending
        Status: PENDING
        Turnover: 0
        Reason: Awaiting GP confirmation
    end note

    S_Pending --> S_Running : GP confirms acceptance

    note right of S_Running
        Status: RUNNING
        Turnover: 0
        Reason: Game result pending
    end note

    S_Running --> S_Settlement : Game result received

    state S_Settlement {
        direction TB
        state "WIN" as Res_Win
        state "LOSS" as Res_Loss
        state "DRAW" as Res_Draw
        state "VOID" as Res_Void
        state "CANCEL" as Res_Cancel

        [*] --> Res_Win
        [*] --> Res_Loss
        [*] --> Res_Draw
        [*] --> Res_Void
        [*] --> Res_Cancel

        note right of Res_Win
            Payout factor > 1.0
            Turnover: 100%
        end note
        note right of Res_Draw
            Payout factor 1.0
            Turnover: 0%
        end note
    }

    S_Settlement --> Paid : WIN (payout)
    S_Settlement --> Completed : LOSS (settled)
    S_Settlement --> Refunded : DRAW/VOID (refund)

    state S_Turnover {
        state "Valid Turnover" as TO_Yes
        state "Ignored Turnover" as TO_No

        Paid --> TO_Yes : Win counts toward turnover
        Completed --> TO_Yes : Loss counts toward turnover
        Refunded --> TO_No : Refund does not count
    }

    TO_Yes --> [*]
    TO_No --> [*]
```

---

## 8. 計算範例

### 8.1 範例 1：百家樂投注

**情境**: 玩家參與「100% 首存優惠」活動，流水要求 (Wagering Requirement) 為 5 倍。

**投注詳情**:
- 存款: $1,000
- 獎金: $1,000（100% 匹配）
- 流水要求: ($1,000 + $1,000) x 5 = **$10,000**
- 本次投注: 百家樂 $100，賠率 1.95，結果：贏

```mermaid
graph TB
    subgraph "Input"
        A["Bet Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95<br/>Result: WIN"]
    end

    subgraph "Layer 1: Risk (05-01)"
        B1["Hedge Detection: PASS"]
        B2["Arbitrage Detection: PASS"]
        B3["Odds Check: 1.95 >= 1.5 PASS"]
        B4["effective_turnover_base<br/>= $100"]
    end

    subgraph "Layer 2: Finance (02-04)"
        C1["Status: WIN"]
        C2["status_factor = 1.0"]
        C3["valid_turnover_finance<br/>= $100 x 1.0<br/>= $100"]
    end

    subgraph "Layer 3: Activity (04-01)"
        D1["Game: Baccarat"]
        D2["game_weight = 0.15"]
        D3["activity_valid_turnover<br/>= $100 x 0.15<br/>= $15"]
    end

    subgraph "Wagering Progress Update"
        E1["Cumulative turnover: $15"]
        E2["Wagering progress: 15 / 10,000<br/>= 0.15%"]
        E3["Remaining: $9,985"]
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
- 風控驗證：通過
- 財務有效投注額：$100
- 活動有效投注額：$15（因百家樂權重僅 15%）
- 需要更多投注才能達成流水要求

### 8.2 範例 2：老虎機投注

**情境**: 同一玩家改玩老虎機以更快完成流水。

**投注詳情**:
- 目前流水進度: $15 / $10,000 (0.15%)
- 本次投注: 老虎機 $100，結果：輸

```mermaid
graph TB
    subgraph "Input"
        A["Bet Amount: $100<br/>Game: Slots<br/>Result: LOSS"]
    end

    subgraph "Layer 1: Risk (05-01)"
        B1["Slots: no hedge risk PASS"]
        B2["No odds check needed PASS"]
        B3["effective_turnover_base<br/>= $100"]
    end

    subgraph "Layer 2: Finance (02-04)"
        C1["Status: LOSS"]
        C2["status_factor = 1.0<br/>(losses count toward turnover)"]
        C3["valid_turnover_finance<br/>= $100 x 1.0<br/>= $100"]
    end

    subgraph "Layer 3: Activity (04-01)"
        D1["Game: Slots"]
        D2["game_weight = 1.0<br/>(100% contribution)"]
        D3["activity_valid_turnover<br/>= $100 x 1.0<br/>= $100"]
    end

    subgraph "Wagering Progress Update"
        E1["This bet: $100"]
        E2["Cumulative: $15 + $100<br/>= $115"]
        E3["Progress: 115 / 10,000<br/>= 1.15%"]
        E4["Remaining: $9,885"]
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

**比較分析**:

| 維度 | 百家樂 | 老虎機 |
|------|--------|--------|
| 投注金額 | $100 | $100 |
| 風控驗證 | 需對沖/賠率檢查 | 簡化驗證 |
| 財務有效投注額 | $100 | $100 |
| 遊戲權重 | 0.15 (15%) | 1.0 (100%) |
| **活動有效投注額** | **$15** | **$100** |
| 流水效率 | 低（需 6.67 倍投注才能完成） | 高（1 倍投注即完成） |

### 8.3 範例 3：對沖投注被拒絕

**情境**: 玩家嘗試在同一百家樂局中同時投注莊家和閒家。

**投注詳情**:
- 投注 1: 莊家 $1,000（賠率 1.95）
- 投注 2: 閒家 $950（賠率 2.00）
- 遊戲結果：莊家贏

```mermaid
graph TB
    subgraph "Input"
        A1["Bet 1: Banker $1,000"]
        A2["Bet 2: Player $950"]
        A3["Same Round ID"]
    end

    subgraph "Layer 1: Risk (05-01)"
        B1["Hedge bet detected"]
        B2["Same round_id: YES"]
        B3["Opposing selection: YES"]
        B4["Hedge detection triggered"]
        B5["risk_code = HEDGE_BET"]
        B6["effective_turnover_base<br/>= $0"]
    end

    subgraph "Layer 2: Finance (02-04)"
        C1["Receives base = $0"]
        C2["valid_turnover_finance<br/>= $0"]
    end

    subgraph "Layer 3: Activity (04-01)"
        D1["Receives finance = $0"]
        D2["activity_valid_turnover<br/>= $0"]
    end

    subgraph "Wagering Progress"
        E1["This bet: $0"]
        E2["Progress: no change"]
        E3["System alert:<br/>Hedge bet detected"]
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
- 對沖投注被風控引擎拒絕
- 兩筆投注的有效投注額均為 $0
- 可能觸發風險標籤：`HEDGE_BETTOR`

---

## 9. 風險標記機制

### 9.1 為何需要風險標記

風險標記機制為每個有效投注額計算決策提供稽核追溯。若無此機制，將無法記錄特定 `valid_bet` 值為何被指派。

**解決方案**: 為每筆投注記錄新增四個風險標記欄位。

### 9.2 風險標記欄位定義

#### 9.2.1 risk_status

| 狀態 | 說明 | valid_bet | 使用情境 |
|------|------|-----------|----------|
| **PASSED** | 風控檢查通過 | = bet_amount | 正常投注 |
| **FILTERED** | 被風控過濾器拒絕 | = 0 | 對沖、套利、低賠率 |
| **PENDING** | 等待人工審核 | = 0（尚未計算） | 可疑交易、高額投注 |

#### 9.2.2 filter_reason

| risk_status | filter_reason 範例 | 說明 |
|-------------|-------------------|------|
| PASSED | null | 無過濾 |
| FILTERED | "Hedge bet: simultaneous Banker/Player bet" | 具體原因 |
| FILTERED | "Low odds bet: odds=1.30 < threshold 1.50" | 具體原因 |
| FILTERED | "Arbitrage bet: cross-platform odds difference > 5%" | 具體原因 |
| PENDING | "High-value bet requires manual review: amount > $10,000" | 等待審核 |

#### 9.2.3 risk_rules_applied (JSON 陣列)

```json
{
  "risk_rules_applied": [
    {
      "rule_id": "HEDGE_001",
      "rule_name": "Baccarat Hedge Detection",
      "action": "FILTER",
      "confidence": 0.95
    },
    {
      "rule_id": "ODDS_001",
      "rule_name": "Odds Threshold Check",
      "action": "PASS",
      "threshold": 1.5,
      "actual_odds": 1.95
    }
  ]
}
```

#### 9.2.4 calculation_version

支援規則調整後的重新計算。

| 版本 | 變更 | 生效日期 |
|------|------|----------|
| v1.0.0 | 初始版本，使用實際風險法 | 2026-01-01 |
| v1.1.0 | 修正為標準本金法 | 2026-01-28 |
| v1.2.0 | 調整賠率門檻 1.3 -> 1.5 | 2026-02-01 |

### 9.3 稽核追蹤範例

**查詢：為何這筆投注的 valid_bet = 0？**

```
bet_id: BET_12345
bet_amount: 100.00
valid_bet: 0.00
risk_status: FILTERED
filter_reason: "Hedge bet: simultaneous Banker ($1000) and Player ($950) bet"
risk_rules_applied: [{"rule_id": "HEDGE_001", "action": "FILTER"}]
```

**結論**：該投注被風控引擎識別為對沖投注，因此 `valid_bet = 0`。

### 9.4 重新計算支援

當風控規則調整時，`calculation_version` 欄位支援歷史資料重新計算。系統可查詢所有使用特定版本計算的投注，並以更新的規則重新處理。

---

## 10. 設計原則：Layer 2 不修改 Valid Bet

### 10.1 核心原則

> **關鍵設計決策**: Layer 2 (財務中心) 僅記錄結算狀態，**不修改** Layer 1 (風控引擎) 決定的 `valid_bet` 值。

### 10.2 為何財務層不應修改 Valid Bet

#### 10.2.1 違反「風控獨立性」原則

如果 Layer 1 已決定 `valid_bet`，那麼 Layer 2 根據結算狀態動態修改它，就意味著風控決定不是最終決策——這造成邏輯矛盾。

**正確做法**:
```
Layer 1 (Risk Engine): 一次性決定 valid_bet
  +-- 通過 -> valid_bet = bet_amount, risk_status = "PASSED"
  +-- 拒絕 -> valid_bet = 0, risk_status = "FILTERED", reason = "hedge bet"

Layer 2 (Finance Center): 僅記錄結算狀態，不修改 valid_bet
  +-- settlement_status = "WIN/LOSS/HALF_WIN/HALF_LOSS/DRAW"
  +-- payout_amount = calculatePayout(bet, status)
```

#### 10.2.2 違反公平性原則

**問題情境**:
```
兩位玩家同樣投注 $100 於體育博彩「讓分 -0.25」:
- 玩家 A 結果：全贏 -> valid_bet = $100（正確）
- 玩家 B 結果：和局（半輸）-> valid_bet = $50（舊方法下錯誤）

矛盾：
- 相同投注行為
- 相同風險敞口（$100）
- 但不同 valid_bet -> 違反公平性原則
```

**正確做法（標準本金法 - 行業標準）**:
```
玩家 A: 投注 $100 -> 全贏 -> valid_bet = $100
玩家 B: 投注 $100 -> 半輸 -> valid_bet = $100（不是 $50！）

理由：
- 兩位玩家在投注時都承擔 $100 風險
- Valid Bet 應反映投注行為，而非結算結果
- 簡化計算——無需等待結算即可知道 valid_bet
```

#### 10.2.3 行業標準比較

| 營運商 | Valid Bet 方法 | 結算影響 | 備註 |
|--------|---------------|---------|------|
| **Pinnacle** | 標準本金法 | 無 | valid_bet = 投注本金 |
| **Betfair** | 標準本金法 | 無 | 全額計算不論結果 |
| **Pragmatic Play** | 標準本金法 | 無 | 行業遊戲供應商標準 |
| **Evolution Gaming** | 標準本金法 | 無 | 真人賭場行業標準 |
| **實際風險法** | 動態調整 | 受影響 | 已棄用，違反公平性 |

### 10.3 結算狀態與 Valid Bet 分離

```mermaid
graph LR
    subgraph "Layer 1 Output"
        A[valid_bet = $100]
        B[risk_status = PASSED]
        C[filter_reason = null]
    end

    subgraph "Layer 2 Records"
        D[settlement_status = HALF_WIN]
        E[payout_amount = $145]
        F[valid_bet remains $100]
    end

    subgraph "Layer 3 Application"
        G[contributed_amount]
        H[= valid_bet x weight]
        I[= $100 x 0.15 = $15]
    end

    A --> F
    F --> G
    D -.used only for payout calculation.-> E
    G --> H --> I

    style F fill:#d4edda
    style A fill:#fff3cd
```

---

## 11. SmartAdmin 實作

### 11.1 Service 層實作

```java
@Service
@RequiredArgsConstructor
public class TurnoverValidationService {

    private final TurnoverRecordDao turnoverRecordDao;
    private final TurnoverValidationManager validationManager;
    private final RiskEngineClient riskEngineClient;

    /**
     * Execute three-layer validation for a settled bet.
     * Uses Vavr Option for null-safety per SmartAdmin patterns.
     */
    public ResponseDTO<TurnoverValidationResult> validateTurnover(BetSettleEvent event) {
        // Layer 1: Risk Engine validation
        RiskValidationResult riskResult = riskEngineClient.validate(event);
        if (riskResult.getActionType() == ActionType.BLOCK) {
            return ResponseDTO.ok(TurnoverValidationResult.rejected(riskResult));
        }

        // Layer 2 & 3: Delegate to Manager for transactional operations
        return validationManager.processFinanceAndActivity(event, riskResult);
    }

    /**
     * Query turnover record by bet ID.
     */
    public Option<TurnoverRecordVO> getTurnoverByBetId(String betId) {
        return Option.of(turnoverRecordDao.selectByBetId(betId))
            .map(entity -> SmartBeanUtil.copy(entity, TurnoverRecordVO.class));
    }
}
```

### 11.2 Manager 層實作

```java
@Component
@RequiredArgsConstructor
public class TurnoverValidationManager {

    private final TurnoverRecordDao turnoverRecordDao;
    private final WageringProgressDao wageringProgressDao;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Process Layer 2 (Finance) and Layer 3 (Activity) with transaction support.
     * @Transactional only allowed in Manager layer per SmartAdmin architecture.
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<TurnoverValidationResult> processFinanceAndActivity(
            BetSettleEvent event, RiskValidationResult riskResult) {

        BigDecimal effectiveBase = riskResult.getEffectiveTurnoverBase();

        // Layer 2: Record settlement status (does NOT modify valid_bet)
        BigDecimal validTurnoverFinance = effectiveBase;

        // Layer 3: Apply game weight for activity contribution
        BigDecimal gameWeight = getGameWeight(event.getGameType());
        BigDecimal activityContribution = validTurnoverFinance.multiply(gameWeight);

        // Save turnover record
        TurnoverRecordEntity record = buildTurnoverRecord(event, riskResult,
            validTurnoverFinance, gameWeight, activityContribution);
        turnoverRecordDao.insert(record);

        // Update wagering progress if player has active bonus
        updateWageringProgress(event.getPlayerId(), activityContribution);

        return ResponseDTO.ok(TurnoverValidationResult.success(record));
    }

    private BigDecimal getGameWeight(String gameType) {
        return switch (gameType) {
            case "SLOTS", "SPORTS" -> BigDecimal.ONE;
            case "BACCARAT", "LIVE_CASINO" -> new BigDecimal("0.15");
            case "BLACKJACK" -> new BigDecimal("0.10");
            case "ROULETTE" -> new BigDecimal("0.20");
            default -> BigDecimal.ONE;
        };
    }
}
```

### 11.3 資料庫結構

```sql
-- Wagering progress table for bonus tracking
CREATE TABLE t_wagering_progress (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL,
    player_id               BIGINT NOT NULL,
    bonus_id                BIGINT NOT NULL,
    wagering_requirement    DECIMAL(18, 4) NOT NULL,
    wagering_completed      DECIMAL(18, 4) NOT NULL DEFAULT 0,
    progress_percentage     DECIMAL(5, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wagering_progress UNIQUE (tenant_id, player_id, bonus_id)
);

CREATE INDEX idx_wagering_player ON t_wagering_progress(tenant_id, player_id, status);
CREATE INDEX idx_wagering_status ON t_wagering_progress(tenant_id, status, updated_at DESC)
    WHERE status = 'ACTIVE';
```

---

## 摘要

### 核心設計原則

1. **三層驗證架構**
   - Layer 1 (風控引擎)：一次性決定 `valid_bet`，標記 `risk_status`
   - Layer 2 (財務中心)：僅記錄結算狀態，**不修改** `valid_bet`
   - Layer 3 (活動系統)：應用遊戲權重，計算活動貢獻

2. **風控獨立性原則**
   - Layer 1 決定 `valid_bet` 後，後續層級不修改
   - 採用「標準本金法」：`valid_bet = bet_amount`（不論結果）
   - 結算狀態僅用於派彩計算，與有效投注額計算分離

3. **關注點分離**
   - 風控專注於：對沖偵測、套利偵測、賠率過濾
   - 財務專注於：結算狀態、派彩金額計算
   - 活動專注於：遊戲權重應用

4. **稽核追溯**
   - 記錄原始資料 + 計算結果 + 版本號
   - 提供重新計算介面
   - 完整風險標記（`risk_status`、`filter_reason`）

### 關鍵術語

| 術語 | 定義 | 單位 | 用途 |
|------|------|------|------|
| **Bet Amount** (投注額) | 單筆原始投注金額 | 每筆 | API 互動、資金扣款 |
| **Turnover** (流水) | 一段時間內投注金額的累計總和 | 累計 | GGR 計算、財務報表 |
| **Valid Bet** (有效投注額) | 經風控過濾後的單筆投注金額 | 每筆 | 流水要求、返水、VIP |
| **Wagering Requirement** (流水要求) | 所需的有效投注額累計總額 | 累計 | 活動驗證、提款限制 |

### 資料流關鍵指標

| 階段 | 指標 | 公式 | 說明 |
|------|------|------|------|
| **Layer 1 輸出** | valid_bet | 風控決定（bet_amount 或 0） | 一次性、不可變 |
| **Layer 2 記錄** | settlement_status | 記錄結算狀態 | 不影響 valid_bet |
| **Layer 3 輸出** | contributed_amount | valid_bet x game_weight | 活動貢獻金額 |
| **進度追蹤** | wagering_progress | sum(contributed) / requirement x 100% | 流水完成百分比 |

### 常見問答

**Q1: 為何 DRAW/TIE 不計入流水？**
- A: 和局時玩家本金歸還，無實際風險，因此不計入流水要求。

**Q2: 為何百家樂權重僅 15%？**
- A: 百家樂 RTP 高（98.94%），平台風險低。較低權重防止玩家利用它來滿足流水要求。

**Q3: 如何偵測對沖投注？**
- A: 系統檢查同一玩家在同一遊戲局中是否有對立投注（例如同時投注莊家和閒家）。

**Q4: 有效投注額計算是即時的嗎？**
- A: 是的，每筆投注結算後立即計算有效投注額並更新進度。

---

**文件版本**: 4.0.0
**建立日期**: 2026-01-27
**維護團隊**: 產品團隊 & 技術架構團隊
