# Turnover Calculation Flowcharts and Sequence Diagrams

> **Canonical Source**: [source-archive/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md)
> **Audience**: Architects, Backend Developers, Data Engineers
> **Business Requirements**: [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)
> **Last Synced**: 2026-02-08

---

## Document Purpose

This document provides the technical visualization of the iGaming platform's turnover calculation system, including the Three-Layer Validation Architecture, detailed sequence diagrams, flowcharts for each layer, and the bet lifecycle state machine. All diagrams are intended for engineering teams implementing or maintaining the turnover calculation pipeline.

**Version History**:
- v4.0.0 (2026-01-28): Corrected Layer 2 logic -- removed erroneous `status_factor` dynamic modification of `valid_bet`; adopted Standard Principal Method; added risk marking mechanism and audit traceability support
- v3.0.0 (2026-01-27): Initial creation

**Key Design Decisions**:
- Layer 1 (Risk Engine) makes the one-time determination of `valid_bet`
- Layer 2 (Finance Center) only records settlement status -- it does NOT modify `valid_bet`
- Layer 3 (Activity System) applies game weights for activity contribution calculation

**Reference Documents**:
- Terminology Standards *(planned - source-archive/00_Foundation/00-03_Terminology_Standards)*
- [Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md)
- [Turnover and Reconciliation Analysis](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md)
- [Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [End-to-End Sequence Diagram](#2-end-to-end-sequence-diagram)
3. [Main Turnover Calculation Flowchart](#3-main-turnover-calculation-flowchart)
4. [Layer 1: Risk Engine Validation](#4-layer-1-risk-engine-validation)
5. [Layer 2: Finance Settlement Recording](#5-layer-2-finance-settlement-recording)
6. [Layer 3: Activity Weight Application](#6-layer-3-activity-weight-application)
7. [Bet Lifecycle State Machine](#7-bet-lifecycle-state-machine)
8. [Calculation Examples](#8-calculation-examples)
9. [Risk Marking Mechanism](#9-risk-marking-mechanism)
10. [Design Principles: Layer 2 Does Not Modify Valid Bet](#10-design-principles-layer-2-does-not-modify-valid-bet)

---

## 1. Architecture Overview

### 1.1 Three-Layer Validation Architecture

The turnover calculation system uses a three-layer validation architecture. Data flows sequentially from player bet through Layer 1 (Risk), Layer 2 (Finance), and Layer 3 (Activity) before reaching application scenarios.

> **Diagram Complexity**: 24 nodes (optimized with subgraph grouping)
> **Reading Guide**: Follow the data flow order (Player Bet -> Layer 1 -> Layer 2 -> Layer 3 -> Application Scenarios)
> **Key Points**:
> - Layer 1 Risk Validation determines `valid_bet`
> - Layer 2 only records status, does NOT modify `valid_bet`
> - Layer 3 applies game weights for activity contribution

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

## 2. End-to-End Sequence Diagram

### 2.1 Complete Turnover Calculation Sequence

This sequence diagram shows the full lifecycle of a bet from placement through settlement, three-layer validation, payout, and audit logging.

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
        Note over Player,Game: ===== Phase 1: Betting Phase =====
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
        Note over Player,DB: ===== Phase 2: Game Settlement Phase =====
    end

    Note over Game: Game Result: Player wins $195
    Game->>Platform: 9. Credit Request (payout)<br/>Amount: $195, Status: WIN
    Platform->>DB: 10. Record bet result<br/>bet_id, status=WIN, win_amount=195

    rect rgb(240, 255, 240)
        Note over Risk,Activity: ===== Phase 3: Turnover Validation (Layer 1 - Risk Engine) =====
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
        Note over Finance,Activity: ===== Phase 4: Finance Status Recording (Layer 2) =====
    end

    Platform->>Finance: 16. recordSettlement(bet_id)<br/>{valid_bet: 100,<br/>status: WIN}

    Finance->>Finance: 17. Record settlement status<br/>settlement_status = "WIN"
    Note over Finance: Only records status<br/>Does NOT modify valid_bet

    Finance->>Finance: 18. Calculate payout amount<br/>payout_amount = calculatePayout()

    Finance->>DB: 19. Update bet record<br/>UPDATE bets SET<br/>settlement_status='WIN',<br/>payout_amount=195

    Finance-->>Platform: 20. Return settlement result<br/>{valid_bet: 100 (unchanged),<br/>settlement_status: 'WIN',<br/>payout_amount: 195}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== Phase 5: Game Weight Application (Layer 3) =====
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
        Note over Platform,Player: ===== Phase 6: Payout and Notification =====
    end

    Platform->>Wallet: 29. Credit payout to wallet<br/>Credit $195 to player
    Wallet->>DB: 30. Update wallet balance<br/>playable_balance += 195
    Wallet-->>Platform: 31. Payout successful

    Platform->>Player: 32. Push notification<br/>Won $195<br/>Wagering progress: +$15 (0.3%)

    rect rgb(245, 245, 245)
        Note over Platform,DB: ===== Phase 7: Audit Log =====
    end

    Platform->>DB: 33. Record audit log<br/>AuditLog.create({<br/>action: "TURNOVER_CALCULATED",<br/>details: {...}<br/>})
```

---

## 3. Main Turnover Calculation Flowchart

### 3.1 Main Flow

> **Diagram Complexity**: 26 nodes (optimized with subgraph grouping)
> **Reading Guide**: Follow the three-layer architecture order (Input -> Layer 1 -> Layer 2 -> Layer 3 -> Update)
> **Key Decision Points**:
> - Layer 1: Hedge/Arbitrage/Low Odds -> determines whether `valid_bet` is 0
> - Layer 2: Bet status branch (WIN/LOSS/DRAW/VOID/HALF) -> records status only
> - Layer 3: Game type branch (Slots/Baccarat/Blackjack/Roulette) -> applies corresponding weight
> - Update: Wagering met? -> determines whether to unlock withdrawal

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

## 4. Layer 1: Risk Engine Validation

### 4.1 Hedge Detection Flow

The hedge detection process identifies when a player places opposing bets within the same game round (e.g., betting on both Banker and Player in Baccarat).

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

### 4.2 Odds Threshold Check

Low-odds bets are filtered to prevent exploitation of near-certain outcomes for wagering requirement completion.

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

## 5. Layer 2: Finance Settlement Recording

### 5.1 Settlement Status Recording Flow

Layer 2 receives the `valid_bet` value from Layer 1 as an immutable input. It records the settlement status and calculates payout amounts, but **never modifies** the `valid_bet` value.

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

### 5.2 Settlement Status vs Valid Bet Reference Table

**Core Principle**: `valid_bet` is determined once by Layer 1 (Risk Engine). Layer 2 only records settlement status and **does not modify** the `valid_bet` value.

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

## 6. Layer 3: Activity Weight Application

### 6.1 Game Weight Application Flow

Layer 3 receives the validated `valid_bet` from the pipeline and applies game-specific weights to calculate the activity contribution amount.

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

### 6.2 Game Weight Configuration Table

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

## 7. Bet Lifecycle State Machine

### 7.1 Bet Lifecycle (State Machine)

This state diagram models the full lifecycle of a bet from placement through settlement and turnover calculation.

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

## 8. Calculation Examples

### 8.1 Example 1: Baccarat Bet

**Scenario**: Player participates in a "100% First Deposit Bonus" activity with a 5x wagering requirement.

**Bet Details**:
- Deposit: $1,000
- Bonus: $1,000 (100% Match)
- Wagering Requirement: ($1,000 + $1,000) x 5 = **$10,000**
- Current Bet: Baccarat $100, odds 1.95, result: WIN

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

**Conclusion**:
- Risk validation: PASSED
- Finance turnover: $100
- Activity turnover: $15 (only 15% contribution due to Baccarat weight)
- More bets needed to meet wagering requirement

### 8.2 Example 2: Slots Bet

**Scenario**: Same player switches to Slots for faster wagering completion.

**Bet Details**:
- Current wagering progress: $15 / $10,000 (0.15%)
- Current bet: Slots $100, result: LOSS

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

**Comparison Analysis**:

| Dimension | Baccarat | Slots |
|-----------|----------|-------|
| Bet Amount | $100 | $100 |
| Risk Validation | Hedge/Odds checks required | Simplified validation |
| Finance Turnover | $100 | $100 |
| Game Weight | 0.15 (15%) | 1.0 (100%) |
| **Activity Turnover** | **$15** | **$100** |
| Wagering Efficiency | Low (6.67x bets to complete) | High (1x bet to complete) |

### 8.3 Example 3: Hedge Bet Rejected

**Scenario**: Player attempts to bet on both Banker and Player in the same Baccarat round.

**Bet Details**:
- Bet 1: Banker $1,000 (odds 1.95)
- Bet 2: Player $950 (odds 2.00)
- Game Result: Banker wins

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

**Conclusion**:
- Hedge bet rejected by risk engine
- Both bets receive $0 turnover
- May trigger risk tag: `HEDGE_BETTOR`

---

## 9. Risk Marking Mechanism

### 9.1 Why Risk Marking is Needed

The risk marking mechanism provides audit traceability for every turnover calculation decision. Without it, there is no record of _why_ a particular `valid_bet` value was assigned.

**Solution**: Four risk marking fields are added to each bet record.

### 9.2 Risk Marking Field Definitions

#### 9.2.1 risk_status

| Status | Description | valid_bet | Use Case |
|--------|-------------|-----------|----------|
| **PASSED** | Risk check passed | = bet_amount | Normal bet |
| **FILTERED** | Risk filter rejected | = 0 | Hedge, arbitrage, low odds |
| **PENDING** | Awaiting manual review | = 0 (not counted yet) | Suspicious transaction, high-value bet |

#### 9.2.2 filter_reason

| risk_status | filter_reason Example | Description |
|-------------|----------------------|-------------|
| PASSED | null | No filtering |
| FILTERED | "Hedge bet: simultaneous Banker/Player bet" | Specific reason |
| FILTERED | "Low odds bet: odds=1.30 < threshold 1.50" | Specific reason |
| FILTERED | "Arbitrage bet: cross-platform odds difference > 5%" | Specific reason |
| PENDING | "High-value bet requires manual review: amount > $10,000" | Awaiting review |

#### 9.2.3 risk_rules_applied (JSON Array)

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

Supports recalculation when rules are adjusted.

| Version | Change | Effective Date |
|---------|--------|---------------|
| v1.0.0 | Initial version, using actual risk method | 2026-01-01 |
| v1.1.0 | Corrected to Standard Principal Method | 2026-01-28 |
| v1.2.0 | Adjusted odds threshold 1.3 -> 1.5 | 2026-02-01 |

### 9.3 Audit Trail Example

**Query: Why is this bet's valid_bet = 0?**

```
bet_id: BET_12345
bet_amount: 100.00
valid_bet: 0.00
risk_status: FILTERED
filter_reason: "Hedge bet: simultaneous Banker ($1000) and Player ($950) bet"
risk_rules_applied: [{"rule_id": "HEDGE_001", "action": "FILTER"}]
```

**Conclusion**: The bet was identified as a hedge bet by the risk engine, resulting in `valid_bet = 0`.

### 9.4 Recalculation Support

When risk rules are adjusted, the `calculation_version` field enables historical data recalculation. The system can query all bets calculated under a specific version and reprocess them with updated rules.

---

## 10. Design Principles: Layer 2 Does Not Modify Valid Bet

### 10.1 Core Principle

> **Key Design Decision**: Layer 2 (Finance Center) only records settlement status and **does not modify** the `valid_bet` value determined by Layer 1 (Risk Engine).

### 10.2 Why Finance Layer Should Not Modify Valid Bet

#### 10.2.1 Violation of "Risk Independence" Principle

If Layer 1 has already determined `valid_bet`, then Layer 2 dynamically modifying it based on settlement status would mean the risk determination is not the final decision -- creating a logical contradiction.

**Correct Approach**:
```
Layer 1 (Risk Engine): One-time determination of valid_bet
  +-- Pass -> valid_bet = bet_amount, risk_status = "PASSED"
  +-- Reject -> valid_bet = 0, risk_status = "FILTERED", reason = "hedge bet"

Layer 2 (Finance Center): Only records settlement status, does NOT modify valid_bet
  +-- settlement_status = "WIN/LOSS/HALF_WIN/HALF_LOSS/DRAW"
  +-- payout_amount = calculatePayout(bet, status)
```

#### 10.2.2 Violation of Fairness Principle

**Problem Scenario**:
```
Two players both bet $100 on sports betting "Handicap -0.25":
- Player A result: Full win -> valid_bet = $100 (correct)
- Player B result: Draw (half loss) -> valid_bet = $50 (INCORRECT under old method)

Contradiction:
- Same betting behavior
- Same risk exposure ($100)
- But different valid_bet -> violates fairness principle
```

**Correct Approach (Standard Principal Method - Industry Standard)**:
```
Player A: Bet $100 -> Full win -> valid_bet = $100
Player B: Bet $100 -> Half loss -> valid_bet = $100 (NOT $50!)

Rationale:
- Both players assumed $100 risk at the time of betting
- Valid Bet should reflect betting behavior, not settlement outcome
- Simplifies calculation -- no need to wait for settlement to know valid_bet
```

#### 10.2.3 Industry Standard Comparison

| Operator | Valid Bet Method | Settlement Impact | Notes |
|----------|-----------------|-------------------|-------|
| **Pinnacle** | Standard Principal | None | valid_bet = bet principal |
| **Betfair** | Standard Principal | None | Full amount regardless of outcome |
| **Pragmatic Play** | Standard Principal | None | Industry game provider standard |
| **Evolution Gaming** | Standard Principal | None | Live casino industry standard |
| **Actual Risk Method** | Dynamic adjustment | Affected | Deprecated, violates fairness |

### 10.3 Settlement Status and Valid Bet Separation

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

## Summary

### Core Design Principles

1. **Three-Layer Validation Architecture**
   - Layer 1 (Risk Engine): One-time determination of `valid_bet`, marks `risk_status`
   - Layer 2 (Finance Center): Only records settlement status, **does not modify** `valid_bet`
   - Layer 3 (Activity System): Applies game weights, calculates activity contribution

2. **Risk Independence Principle**
   - After Layer 1 determines `valid_bet`, subsequent layers do not modify it
   - Uses "Standard Principal Method": `valid_bet = bet_amount` (regardless of outcome)
   - Settlement status is only used for payout calculation, separated from turnover calculation

3. **Separation of Concerns**
   - Risk focuses on: hedge detection, arbitrage detection, odds filtering
   - Finance focuses on: settlement status, payout amount calculation
   - Activity focuses on: game weight application

4. **Audit Traceability**
   - Records original data + calculation results + version number
   - Provides recalculation interface
   - Complete risk marking (`risk_status`, `filter_reason`)

### Key Terminology

| Term | Definition | Unit | Usage |
|------|-----------|------|-------|
| **Bet Amount** | Single original bet amount | Per bet | API interaction, fund deduction |
| **Turnover** | Time-accumulated sum of bet amounts | Cumulative | GGR calculation, financial reports |
| **Valid Bet** | Risk-filtered single bet amount | Per bet | Wagering requirements, rebates, VIP |
| **Wagering Requirement** | Required total valid bet amount | Cumulative | Activity verification, withdrawal limits |

### Data Flow Key Metrics

| Phase | Metric | Formula | Description |
|-------|--------|---------|-------------|
| **Layer 1 Output** | valid_bet | Risk determination (bet_amount or 0) | One-time, immutable |
| **Layer 2 Record** | settlement_status | Records settlement status | Does not affect valid_bet |
| **Layer 3 Output** | contributed_amount | valid_bet x game_weight | Activity contribution amount |
| **Progress Tracking** | wagering_progress | sum(contributed) / requirement x 100% | Wagering completion percentage |

### FAQ

**Q1: Why doesn't DRAW/TIE count toward turnover?**
- A: In a draw, the player's principal is returned with no actual risk, so it does not count toward wagering requirements.

**Q2: Why is Baccarat weight only 15%?**
- A: Baccarat has a high RTP (98.94%), low platform risk. The lower weight prevents wagering requirement exploitation.

**Q3: How is hedge betting detected?**
- A: The system checks whether the same player has opposing bets in the same game round (e.g., betting on both Banker and Player simultaneously).

**Q4: Is turnover calculation real-time?**
- A: Yes, turnover is calculated and progress updated immediately after each bet settlement.

---

**Document Version**: 4.0.0
**Created**: 2026-01-27
**Maintained by**: Product Team & Tech Architecture Team
