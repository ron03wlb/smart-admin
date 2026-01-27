# IGaming 流水計算流程圖與時序圖

> **文檔目標**: 可視化展示 IGaming 平台流水計算的完整流程，包含三層驗證架構的詳細交互。
> **創建日期**: 2026-01-27
> **參考文檔**:
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

```mermaid
graph TB
    subgraph "玩家投注"
        A[玩家下注<br/>Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95]
    end

    subgraph "Layer 1: 風控引擎 (Risk Engine - 05-01)"
        B[對沖檢測<br/>Hedge Detection]
        C[套利檢測<br/>Arbitrage Detection]
        D[低賠率過濾<br/>Low Odds Filter<br/>閾值: 1.5]
        E[輸出: effective_turnover_base<br/>基礎有效流水]
    end

    subgraph "Layer 2: 財務中心 (Finance Center - 02-04)"
        F[注單結算<br/>Bet Settlement]
        G[狀態因子應用<br/>Status Factor]
        H{注單狀態?}
        I[WIN/LOSS<br/>Factor: 1.0]
        J[DRAW/TIE<br/>Factor: 0.0]
        K[VOID/CANCEL<br/>Factor: 0.0]
        L[HALF_WIN/LOSS<br/>Factor: 0.5]
        M[輸出: valid_turnover_finance<br/>財務有效流水]
    end

    subgraph "Layer 3: 活動系統 (Activity System - 04-01)"
        N[遊戲權重應用<br/>Game Weight]
        O{遊戲類型?}
        P[Slots/Sports<br/>Weight: 1.0]
        Q[Baccarat<br/>Weight: 0.15]
        R[Blackjack<br/>Weight: 0.1]
        S[Roulette<br/>Weight: 0.2]
        T[輸出: activity_valid_turnover<br/>活動有效流水]
    end

    subgraph "應用場景"
        U[返水計算<br/>Rebate Calculation]
        V[流水進度追蹤<br/>Wagering Progress]
        W[VIP升級<br/>VIP Upgrade]
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
    participant Game as 🎮 遊戲提供商<br/>(Game Provider)
    participant Platform as 🖥️ 平台核心<br/>(Platform Core)
    participant Risk as 🛡️ 風控引擎<br/>(Risk Engine)<br/>05-01
    participant Finance as 💰 財務中心<br/>(Finance Center)<br/>02-04
    participant Activity as 🎁 活動系統<br/>(Activity System)<br/>04-01
    participant Wallet as 💳 錢包系統<br/>(Wallet System)<br/>02-06
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

    Risk-->>Platform: 15. 驗證通過<br/>{is_valid: true,<br/>effective_turnover_base: 100,<br/>risk_code: "VALID"}

    rect rgb(255, 250, 250)
        Note over Finance,Activity: ===== 階段4: 狀態因子應用 (Layer 2 - Finance) =====
    end

    Platform->>Finance: 16. applyStatusFactor(bet_id)<br/>{effective_turnover_base: 100,<br/>status: WIN}

    Finance->>Finance: 17. 計算狀態因子<br/>status_factor = getStatusFactor("WIN")
    Note over Finance: WIN → factor = 1.0<br/>LOSS → factor = 1.0<br/>DRAW → factor = 0.0

    Finance->>Finance: 18. 計算財務有效流水<br/>valid_turnover_finance =<br/>100 × 1.0 = $100

    Finance->>DB: 19. 更新注單流水記錄<br/>UPDATE bets SET<br/>valid_turnover_finance=100

    Finance-->>Platform: 20. 返回財務流水<br/>{valid_turnover_finance: 100}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== 階段5: 遊戲權重應用 (Layer 3 - Activity) =====
    end

    Platform->>Activity: 21. calculateActivityTurnover()<br/>{valid_turnover_finance: 100,<br/>game_type: BACCARAT}

    Activity->>DB: 22. 查詢玩家參與的活動<br/>SELECT * FROM player_bonuses<br/>WHERE player_id=xxx<br/>AND status='active'
    DB-->>Activity: 23. 返回活動列表<br/>[{bonus_id: B001,<br/>bonus_type: "DEPOSIT",<br/>wagering_requirement: 5000}]

    Activity->>Activity: 24. 獲取遊戲權重<br/>game_weight = getGameWeight("BACCARAT")
    Note over Activity: Baccarat → 0.15 (15%)<br/>Slots → 1.0 (100%)<br/>Blackjack → 0.1 (10%)

    Activity->>Activity: 25. 計算活動有效流水<br/>activity_valid_turnover =<br/>100 × 0.15 = $15

    Activity->>DB: 26. 更新流水進度<br/>UPDATE player_bonuses SET<br/>wagering_completed += 15,<br/>wagering_progress = 15/5000

    DB-->>Activity: 27. 更新成功

    Activity-->>Platform: 28. 返回活動流水<br/>{activity_valid_turnover: 15,<br/>wagering_progress: "0.3%",<br/>remaining: 4985}

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

```mermaid
flowchart TD
    Start([開始: 注單結算])

    subgraph Input["輸入數據"]
        A[注單信息<br/>bet_id, player_id<br/>bet_amount: $100<br/>odds: 1.95<br/>game_type: BACCARAT<br/>status: WIN]
    end

    subgraph Layer1["Layer 1: 風控引擎驗證 (05-01)"]
        B{是否對沖投注?}
        C{是否套利投注?}
        D{賠率是否≥1.5?}
        E[有效流水基數<br/>effective_turnover_base<br/>= $100]
        F[拒絕<br/>effective_turnover_base<br/>= $0]
    end

    subgraph Layer2["Layer 2: 財務狀態驗證 (02-04)"]
        G{注單狀態?}
        H[WIN/LOSS<br/>status_factor = 1.0]
        I[DRAW/TIE<br/>status_factor = 0.0]
        J[VOID/CANCEL<br/>status_factor = 0.0]
        K[HALF_WIN/HALF_LOSS<br/>status_factor = 0.5]
        L[計算財務流水<br/>valid_turnover_finance<br/>= base × factor<br/>= $100 × 1.0 = $100]
    end

    subgraph Layer3["Layer 3: 活動權重驗證 (04-01)"]
        M{遊戲類型?}
        N[Slots/Sports<br/>game_weight = 1.0]
        O[Baccarat<br/>game_weight = 0.15]
        P[Blackjack<br/>game_weight = 0.1]
        Q[Roulette<br/>game_weight = 0.2]
        R[計算活動流水<br/>activity_valid_turnover<br/>= finance × weight<br/>= $100 × 0.15 = $15]
    end

    subgraph Update["更新流水進度"]
        S[更新數據庫]
        T[計算流水進度<br/>progress = 15/5000<br/>= 0.3%]
        U[檢查是否完成流水]
        V{流水是否達標?}
        W[解鎖提款權限<br/>可提現餘額增加]
        X[保持流水鎖定<br/>繼續追蹤進度]
    end

    End([結束])

    Start --> Input
    Input --> A
    A --> B

    B -->|是| F
    B -->|否| C
    C -->|是| F
    C -->|否| D
    D -->|否<br/>低賠率| F
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

    V -->|是<br/>completed ≥ required| W
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
    B[獲取注單信息<br/>player_id, round_id<br/>selection, amount]
    C[查詢同一Round<br/>該玩家的所有注單]
    D{是否存在<br/>相反投注?}

    subgraph Example["範例: 百家樂"]
        E1[投注 Banker $1000]
        E2[投注 Player $950]
        E3[對沖檢測: ✓<br/>相反投注]
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
    B[讀取配置<br/>MIN_ODDS_THRESHOLD = 1.5]
    C[獲取注單賠率<br/>odds = 1.95]
    D{odds ≥ threshold?}
    E[賠率合格<br/>通過驗證]
    F[低賠率投注<br/>risk_code = LOW_ODDS]
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

## 5. Layer 2: 財務驗證流程

### 5.1 狀態因子應用流程 (Status Factor Application)

```mermaid
flowchart TD
    A[開始: 財務層驗證]
    B[輸入: effective_turnover_base<br/>= $100]
    C[獲取注單狀態<br/>bet.status]
    D{注單狀態}

    E[WIN<br/>玩家贏]
    F[status_factor = 1.0<br/>100% 計入流水]

    G[LOSS<br/>玩家輸]
    H[status_factor = 1.0<br/>100% 計入流水]

    I[DRAW/TIE<br/>和局]
    J[status_factor = 0.0<br/>無風險,不計流水]

    K[VOID/CANCEL<br/>注單作廢]
    L[status_factor = 0.0<br/>不計流水]

    M[HALF_WIN/HALF_LOSS<br/>半贏半輸]
    N[status_factor = 0.5<br/>50% 計入流水]

    O[計算財務流水<br/>valid_turnover_finance<br/>= base × factor]

    P[更新數據庫<br/>bets.valid_turnover_finance]

    End([返回: valid_turnover_finance])

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

    style F fill:#d4edda
    style H fill:#d4edda
    style J fill:#fff3cd
    style L fill:#fff3cd
    style N fill:#d1ecf1
```

### 5.2 狀態因子對照表

```mermaid
graph LR
    subgraph "注單狀態 (Bet Status)"
        A1[WIN<br/>玩家贏]
        A2[LOSS<br/>玩家輸]
        A3[DRAW<br/>和局]
        A4[TIE<br/>平局]
        A5[VOID<br/>作廢]
        A6[CANCEL<br/>取消]
        A7[HALF_WIN<br/>半贏]
        A8[HALF_LOSS<br/>半輸]
        A9[RUNNING<br/>進行中]
    end

    subgraph "狀態因子 (Status Factor)"
        B1[1.0]
        B2[1.0]
        B3[0.0]
        B4[0.0]
        B5[0.0]
        B6[0.0]
        B7[0.5]
        B8[0.5]
        B9[0.0]
    end

    subgraph "計入流水比例"
        C1[100%]
        C2[100%]
        C3[0%]
        C4[0%]
        C5[0%]
        C6[0%]
        C7[50%]
        C8[50%]
        C9[0%]
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
    style B3 fill:#f8d7da
    style B4 fill:#f8d7da
    style B5 fill:#f8d7da
    style B6 fill:#f8d7da
    style B7 fill:#fff3cd
    style B8 fill:#fff3cd
    style B9 fill:#f8d7da
```

---

## 6. Layer 3: 活動驗證流程

### 6.1 遊戲權重應用流程 (Game Weight Application)

```mermaid
flowchart TD
    A[開始: 活動層驗證]
    B[輸入: valid_turnover_finance<br/>= $100]
    C[獲取遊戲類型<br/>game_type]
    D{遊戲類型}

    E[Slots<br/>老虎機]
    F[game_weight = 1.0<br/>100% 貢獻]

    G[Sports<br/>體育博彩]
    H[game_weight = 1.0<br/>100% 貢獻]

    I[Baccarat<br/>百家樂]
    J[game_weight = 0.15<br/>15% 貢獻]

    K[Blackjack<br/>二十一點]
    L[game_weight = 0.1<br/>10% 貢獻]

    M[Roulette<br/>輪盤]
    N[game_weight = 0.2<br/>20% 貢獻]

    O[Live Casino<br/>真人娛樂場]
    P[game_weight = 0.15<br/>15% 貢獻]

    Q[計算活動流水<br/>activity_valid_turnover<br/>= finance × weight]

    R[查詢玩家活動<br/>player_bonuses]
    S[更新流水進度<br/>wagering_completed += activity_valid_turnover]
    T[計算完成百分比<br/>progress = completed / required]

    U{流水是否達標?}
    V[標記活動完成<br/>status = 'completed']
    W[解鎖提款<br/>可提現餘額更新]
    X[保持追蹤<br/>status = 'active']

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

    U -->|是<br/>completed ≥ required| V
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
        A1["🎰 Slots<br/>老虎機<br/>Weight: 1.0"]
        A2["⚽ Sports<br/>體育博彩<br/>Weight: 1.0"]
        A3["🎲 Dice<br/>骰寶<br/>Weight: 1.0"]
    end

    subgraph "中貢獻遊戲 (Medium Contribution)"
        B1["🎡 Roulette<br/>輪盤<br/>Weight: 0.2"]
        B2["🃏 Baccarat<br/>百家樂<br/>Weight: 0.15"]
        B3["👨‍💼 Live Casino<br/>真人娛樂<br/>Weight: 0.15"]
    end

    subgraph "低貢獻遊戲 (Low Contribution)"
        C1["🂡 Blackjack<br/>二十一點<br/>Weight: 0.1"]
        C2["🎴 Poker<br/>撲克<br/>Weight: 0.05"]
        C3["🎯 Video Poker<br/>視訊撲克<br/>Weight: 0.05"]
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
    note right of S_Pending
        Status: PENDING
        Turnover: 0
        Reason: 等待GP確認
    end note

    S_Pending --> S_Running : GP確認接受\n(遊戲開始)

    %% 2. 進行階段
    note right of S_Running
        Status: RUNNING
        Turnover: 0
        Reason: 賽果未出
    end note

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
        A["投注金額: $100<br/>遊戲: Baccarat<br/>賠率: 1.95<br/>結果: WIN"]
    end

    subgraph "Layer 1: 風控 (05-01)"
        B1["對沖檢測: ✓ 通過"]
        B2["套利檢測: ✓ 通過"]
        B3["賠率檢測: 1.95 ≥ 1.5 ✓"]
        B4["effective_turnover_base<br/>= $100"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["狀態: WIN"]
        C2["status_factor = 1.0"]
        C3["valid_turnover_finance<br/>= $100 × 1.0<br/>= $100"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["遊戲: Baccarat"]
        D2["game_weight = 0.15"]
        D3["activity_valid_turnover<br/>= $100 × 0.15<br/>= $15"]
    end

    subgraph "流水進度更新"
        E1["累計流水: $15"]
        E2["流水進度: 15 / 10,000<br/>= 0.15%"]
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
        A["投注金額: $100<br/>遊戲: Slots<br/>結果: LOSS"]
    end

    subgraph "Layer 1: 風控 (05-01)"
        B1["老虎機無對沖風險 ✓"]
        B2["無需賠率檢測 ✓"]
        B3["effective_turnover_base<br/>= $100"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["狀態: LOSS"]
        C2["status_factor = 1.0<br/>(輸也計流水)"]
        C3["valid_turnover_finance<br/>= $100 × 1.0<br/>= $100"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["遊戲: Slots"]
        D2["game_weight = 1.0<br/>(100%貢獻)"]
        D3["activity_valid_turnover<br/>= $100 × 1.0<br/>= $100"]
    end

    subgraph "流水進度更新"
        E1["本次流水: $100"]
        E2["累計流水: $15 + $100<br/>= $115"]
        E3["流水進度: 115 / 10,000<br/>= 1.15%"]
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
        B6["effective_turnover_base<br/>= $0"]
    end

    subgraph "Layer 2: 財務 (02-04)"
        C1["接收 base = $0"]
        C2["valid_turnover_finance<br/>= $0"]
    end

    subgraph "Layer 3: 活動 (04-01)"
        D1["接收 finance = $0"]
        D2["activity_valid_turnover<br/>= $0"]
    end

    subgraph "流水進度"
        E1["本次流水: $0"]
        E2["流水進度: 無變化"]
        E3["系統警告:<br/>檢測到對沖投注"]
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

**風控邏輯**:
```python
# 偽代碼: 對沖檢測
def detect_hedge_bet(bet: Bet) -> bool:
    # 查詢同一局的其他投注
    other_bets = query_bets_by_round(bet.round_id, bet.player_id)

    for other_bet in other_bets:
        if is_opposite_selection(bet.selection, other_bet.selection):
            # 檢測到相反投注
            if is_hedge_pattern(bet.amount, other_bet.amount, bet.odds, other_bet.odds):
                return True  # 對沖投注

    return False  # 無對沖

def is_hedge_pattern(amount1, amount2, odds1, odds2) -> bool:
    # 檢查是否為無風險套利
    expected_return_1 = amount1 * odds1
    expected_return_2 = amount2 * odds2
    total_stake = amount1 + amount2

    # 如果任一結果都能保本或盈利,則為對沖
    if expected_return_1 >= total_stake and expected_return_2 >= total_stake:
        return True

    # 或者損失極小 (< 5%)
    min_return = min(expected_return_1, expected_return_2)
    loss_rate = (total_stake - min_return) / total_stake

    return loss_rate < 0.05  # 損失小於5%視為對沖
```

**結論**:
- ❌ 對沖投注被風控拒絕
- ❌ 兩筆投注流水均為 $0
- ⚠️ 可能觸發風控標籤 (Risk Tag: HEDGE_BETTOR)

---

## 9. 總結

### 9.1 核心設計原則

1. **三層驗證架構** (Three-Layer Architecture)
   - Layer 1 (風控): 過濾惡意行為
   - Layer 2 (財務): 確保財務準確性
   - Layer 3 (活動): 應用業務規則

2. **單一數據源** (Single Source of Truth)
   - 05-01 風控系統是風險驗證的權威來源
   - 其他模塊通過 API 調用,不重複實現邏輯

3. **關注點分離** (Separation of Concerns)
   - 風控關注: 對沖、套利、賠率
   - 財務關注: 注單狀態
   - 活動關注: 遊戲權重

### 9.2 關鍵指標

| 指標 | 定義 | 計算公式 |
|------|------|---------|
| **effective_turnover_base** | 風控有效流水 | 通過風控驗證的投注金額 |
| **valid_turnover_finance** | 財務有效流水 | base × status_factor |
| **activity_valid_turnover** | 活動有效流水 | finance × game_weight |
| **wagering_progress** | 流水完成進度 | completed / required × 100% |

### 9.3 常見問題 (FAQ)

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
