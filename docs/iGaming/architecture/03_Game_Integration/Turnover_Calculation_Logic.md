# Turnover Calculation Logic

> **Canonical Source**: [source-archive/03_Game_Center/03-04_Turnover_Calculation.md](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## 1. Core Calculation Formula

```
ValidTurnover = BetAmount × GameWeight × OddsFactor × StatusFactor × RiskFactor
```

Where:
- **RiskFactor**: `1` (Pass) or `0` (Block/Flag) - Determined by Layer 1
- **StatusFactor**: `1.0` (WIN/LOSS) or `0` (DRAW/VOID) - Determined by Layer 2
- **GameWeight**: `1.0` (Slots) to `0.05` (Poker) - Applied by Layer 3

---

## 2. Three-Layer Validation Architecture

### 2.1 Architecture Overview

```mermaid
graph TB
    subgraph "Player Bet"
        A["Player Bet<br/>Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95"]
    end

    subgraph "Layer 1: Risk Engine (05-01)"
        B["Hedge Detection"]
        C["Arbitrage Detection"]
        D["Low Odds Filter<br/>Threshold: 1.5"]
        E["Output: valid_bet<br/>+ action_type<br/>(BLOCK/FLAG/PASS)"]
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
        N["Apply Game Weight"]
        O{Game Type?}
        P["Slots/Sports<br/>Weight: 1.0"]
        Q["Baccarat<br/>Weight: 0.15"]
        R["Blackjack<br/>Weight: 0.1"]
        S["Roulette<br/>Weight: 0.2"]
        T["Output: activity_valid_turnover"]
    end

    subgraph "Applications"
        U["Rebate Calculation"]
        V["Wagering Progress"]
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
```

---

## 3. Layer 1: Risk Engine Validation

### 3.1 Risk Validation Result Interface

```typescript
interface RiskValidationResult {
  is_valid: boolean;
  action_type: 'BLOCK' | 'FLAG' | 'PASS';
  matched_rules: string[];
  risk_proposal_id: string | null;
  effective_turnover_base: number;
}
```

### 3.2 Hedge Detection Flow

```mermaid
flowchart TD
    A[Start: Hedge Detection]
    B["Get bet info<br/>player_id, round_id<br/>selection, amount"]
    C["Query same Round<br/>all bets from player"]
    D{"Opposite bets<br/>exist?"}

    subgraph Example["Example: Baccarat"]
        E1[Bet Banker $1000]
        E2[Bet Player $950]
        E3["Hedge Detected<br/>Opposite bets"]
    end

    F[Mark as hedge bet]
    G["action_type = BLOCK<br/>(or FLAG per config)"]
    H[effective_turnover = 0]
    I[No hedge detected]
    J[Continue validation]

    End([End: Return result])

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

### 3.3 Odds Threshold Validation

```mermaid
flowchart TD
    A[Start: Odds Validation]
    B["Read config<br/>MIN_ODDS_THRESHOLD = 1.5"]
    C["Get bet odds<br/>odds = 1.95"]
    D{odds >= threshold?}
    E["Odds valid<br/>Pass validation"]
    F["Low odds bet<br/>action_type = BLOCK"]
    G[effective_turnover = 0]

    subgraph Examples["Odds Examples"]
        EX1[1.95 -> Pass]
        EX2[1.50 -> Pass]
        EX3[1.30 -> Reject]
        EX4[1.01 -> Reject]
    end

    End([Return result])

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

### 3.4 Layer 1 Processing Logic (v2.1.0)

```typescript
// Correct approach (v2.1.0): Short-circuit on Layer 1 rejection
const riskValidation = await RiskEngine.validateTurnover({...});

if (!riskValidation.is_valid && riskValidation.action_type === 'BLOCK') {
  // BLOCK rule returns 0 immediately, skip Layer 2/3
  return {
    effective_turnover_base: 0,
    valid_turnover_finance: 0,
    activity_valid_turnover: 0,
    rejected_by: 'RISK_ENGINE',
    action_type: 'BLOCK',
    matched_rules: riskValidation.matched_rules
  };
}

// FLAG rule marks but continues (v2.1.0)
if (riskValidation.action_type === 'FLAG') {
  await recordRiskFlag(bet.id, riskValidation.risk_proposal_id);
}

// Layer 2 only handles status factor adjustment (trusts Layer 1 result)
const valid_turnover_finance = calculateFinanceTurnover(
  riskValidation.effective_turnover_base,
  bet.status
);
```

---

## 4. Layer 2: Finance Status Recording

### 4.1 Status Factor Mapping

```typescript
/**
 * Layer 2: Finance Layer status factor adjustment
 * Responsibility: WIN/LOSS/DRAW/CANCEL status factor application
 * Precondition: Layer 1 validation passed (is_valid = true)
 *
 * Warning: This layer does NOT make rejection decisions, trusts Layer 1 result
 */
function getStatusFactor(status: BetStatus): number {
  const STATUS_FACTORS = {
    'WIN': 1.0,        // Player wins - full turnover
    'LOSS': 1.0,       // Player loses - full turnover
    'DRAW': 0.0,       // Draw - no risk, no turnover
    'TIE': 0.0,        // Same as draw
    'VOID': 0.0,       // Voided - invalid bet
    'CANCEL': 0.0,     // Cancelled - invalid bet
    'HALF_WIN': 1.0,   // v2.0.0: Half win - full turnover (Standard Principal)
    'HALF_LOSS': 1.0,  // v2.0.0: Half loss - full turnover (Standard Principal)
    'RUNNING': 0.0     // In progress - not settled
  };
  return STATUS_FACTORS[status] ?? 0.0;
}
```

### 4.2 Finance Layer Flow

```mermaid
flowchart TD
    A[Start: Finance Layer]
    B["Input: valid_bet<br/>= $100<br/>from Layer 1, immutable"]
    C["Get bet status<br/>bet.status"]
    D{Bet Status}

    E["WIN<br/>Player wins"]
    F["Record: settlement_status = WIN<br/>Calculate payout"]

    G["LOSS<br/>Player loses"]
    H["Record: settlement_status = LOSS<br/>Calculate payout"]

    I["DRAW/TIE<br/>Draw"]
    J["Record: settlement_status = DRAW<br/>Return stake"]

    K["VOID/CANCEL<br/>Voided"]
    L["Record: settlement_status = VOID<br/>Return stake"]

    M["HALF_WIN/HALF_LOSS<br/>Partial result"]
    N["Record: settlement_status<br/>Calculate partial payout"]

    O["Update database<br/>settlement_status<br/>payout_amount"]

    P["valid_bet unchanged<br/>= $100<br/>not affected by status"]

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
```

---

## 5. Layer 3: Activity Weight Application

### 5.1 Game Weight Configuration

```typescript
/**
 * Layer 3: Activity Layer game weight application
 * Responsibility: Apply game weight to finance turnover
 */
function getGameWeight(gameType: GameType): number {
  const GAME_WEIGHTS = {
    'SLOTS': 1.0,
    'SPORTS': 1.0,
    'E_SPORTS': 1.0,
    'ROULETTE': 0.2,
    'BACCARAT': 0.15,
    'LIVE_CASINO': 0.15,
    'BLACKJACK': 0.1,
    'VIDEO_POKER': 0.05,
    'POKER': 0.05,
    'LOTTERY': 0.1,
    'PVP': 0.0
  };
  return GAME_WEIGHTS[gameType] ?? 1.0; // Default 100%
}
```

### 5.2 Activity Layer Flow

```mermaid
flowchart TD
    A[Start: Activity Layer]
    B["Input: valid_turnover_finance<br/>= $100"]
    C["Get game type<br/>game_type"]
    D{Game Type}

    E["Slots<br/>Slot Machine"]
    F["game_weight = 1.0<br/>100% contribution"]

    G["Sports<br/>Sports Betting"]
    H["game_weight = 1.0<br/>100% contribution"]

    I["Baccarat<br/>Baccarat"]
    J["game_weight = 0.15<br/>15% contribution"]

    K["Blackjack<br/>Blackjack"]
    L["game_weight = 0.1<br/>10% contribution"]

    M["Roulette<br/>Roulette"]
    N["game_weight = 0.2<br/>20% contribution"]

    O["Live Casino<br/>Live Dealer"]
    P["game_weight = 0.15<br/>15% contribution"]

    Q["Calculate activity turnover<br/>activity_valid_turnover<br/>= finance x weight"]

    R["Query player bonuses<br/>player_bonuses"]
    S["Update wagering progress<br/>wagering_completed += activity_valid_turnover"]
    T["Calculate completion percentage<br/>progress = completed / required"]

    U{Wagering complete?}
    V["Mark activity complete<br/>status = 'completed'"]
    W["Unlock withdrawal<br/>Update withdrawable balance"]
    X["Keep tracking<br/>status = 'active'"]

    End([Return: Wagering Progress])

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

    U -->|Yes| V
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

---

## 6. Valid Bet Calculation by Game Type

### 6.1 Calculation Formulas

| Game Type | Condition | effectiveStake Formula |
|-----------|-----------|------------------------|
| **SPORTS / E-SPORTS** | - | `|winAmount + lossAmount|` |
| **CASINO** | Draw (payout == betAmount) | `0` |
| **CASINO** | Win (winAmount > 0) | `min(winAmount, betAmount)` |
| **CASINO** | Loss (winAmount == 0) | `betAmount` |
| **Other Types** | - | `betAmount` |

### 6.2 Implementation

```typescript
/**
 * Calculate Effective Stake
 * Location: GridAbstractService.java:171-193
 */
function getEffectiveStake(bet: Transaction): number {
  const gameType = bet.gameType;
  const betAmount = bet.betAmount;
  const payout = bet.payout;
  const winAmount = payout - betAmount;
  const lossAmount = betAmount - payout;

  // Sports: absolute value (winAmount + lossAmount)
  if (gameType === 'SPORTS' || gameType === 'E_SPORTS') {
    return Math.abs(winAmount + lossAmount);
  }

  // Casino games
  if (gameType === 'CASINO') {
    // Draw: 0
    if (payout === betAmount) {
      return 0;
    }
    // Win: min(winAmount, betAmount)
    if (winAmount > 0) {
      return Math.min(winAmount, betAmount);
    }
    // Loss: betAmount
    return betAmount;
  }

  // Other types: betAmount
  return betAmount;
}
```

### 6.3 effectiveStake and lockAmount Relationship

```typescript
/**
 * Add effective stake and adjust lockAmount
 * Location: WalletTransaction.java:119-125
 */
function addEffectiveStake(amount: number): void {
  this.effectiveStake += amount;
  this.addedLockAmount -= amount; // lockAmount decreases

  log.info(`[Wallet] effectiveStake=${this.effectiveStake}, lockAmount=${this.lockAmount}`);
}
```

**Example**:
- Before: lockAmount=500, effectiveStake=100
- Bet settlement produces effectiveStake=200
- After: lockAmount=300, effectiveStake=300

---

## 7. Free Spins Turnover Calculation

### 7.1 GGR Calculation Implementation

```typescript
/**
 * Calculate GGR (including free spins cost)
 */
public calculateGGR(date: LocalDate): GgrReport {
    const transactions = this.transactionRepository.findByDate(date);

    let totalTurnover = 0;
    let totalPayout = 0;
    let freespinTurnover = 0;  // Promotion cost tracking

    for (const tx of transactions) {
        // Turnover calculation (includes free spin face value)
        if (tx.transactionType.endsWith('_BET')) {
            totalTurnover += tx.turnover;

            if (tx.isFreeSpin) {
                freespinTurnover += tx.turnover;  // Record promotion cost
            }
        }

        // Payout calculation
        if (tx.transactionType.endsWith('_WIN')) {
            totalPayout += tx.amount;
        }
    }

    // GGR = Turnover - Payout
    const ggr = totalTurnover - totalPayout;

    return {
        date,
        totalTurnover,
        freespinTurnover,      // Promotion cost
        cashTurnover: totalTurnover - freespinTurnover,
        totalPayout,
        ggr
    };
}
```

---

## 8. Database Schema

### 8.1 Wagering Details Table

```sql
-- wagering_details table (valid bet records)
CREATE TABLE wagering_details (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bet_id VARCHAR(64) NOT NULL UNIQUE,
  player_id BIGINT NOT NULL,
  promotion_id BIGINT,

  -- Original data (immutable)
  bet_amount DECIMAL(19,4) NOT NULL,
  game_type VARCHAR(32) NOT NULL,
  odds DECIMAL(10,4),
  status VARCHAR(32) NOT NULL,

  -- Calculation results (recalculable)
  valid_bet DECIMAL(19,4) NOT NULL,
  game_weight DECIMAL(5,4) NOT NULL,
  contributed_amount DECIMAL(19,4) NOT NULL,

  -- Recalculation support
  calculation_version VARCHAR(16) NOT NULL DEFAULT 'v1.0.0',

  -- Audit fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_player_promotion (player_id, promotion_id),
  INDEX idx_calculation_version (calculation_version)
);
```

### 8.2 Wagering Progress Table

```sql
-- wagering_progress table (aggregated view)
CREATE TABLE wagering_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  player_id BIGINT NOT NULL,
  promotion_id BIGINT NOT NULL,

  total_requirement DECIMAL(19,4) NOT NULL,
  completed_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
  remaining_amount DECIMAL(19,4) AS (total_requirement - completed_amount) STORED,

  is_completed BOOLEAN AS (completed_amount >= total_requirement) STORED,

  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_player_promotion (player_id, promotion_id)
);
```

### 8.3 Recalculation Task Table

```sql
-- Recalculation task table
CREATE TABLE t_turnover_recalculation_task (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id             VARCHAR(64) NOT NULL UNIQUE,
    trigger_type        VARCHAR(50) NOT NULL,  -- CONFIG_CHANGE, GP_DIFF, MANUAL
    trigger_reason      VARCHAR(500),

    -- Recalculation scope
    player_id           BIGINT,                -- NULL = full recalculation
    game_type           VARCHAR(50),
    date_range_start    DATETIME,
    date_range_end      DATETIME,
    affected_count      INT,

    -- New configuration
    new_config          JSON,                  -- New status_factor / game_weight

    -- Execution status
    status              VARCHAR(20) DEFAULT 'PENDING',
    started_at          DATETIME,
    completed_at        DATETIME,
    error_message       TEXT,

    -- Audit
    created_by          VARCHAR(100) NOT NULL,
    approved_by         VARCHAR(100),
    approved_at         DATETIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_trigger_type (trigger_type),
    INDEX idx_date_range (date_range_start, date_range_end)
);
```

---

## 9. SmartAdmin Layer Mapping

### 9.1 Layer Responsibilities

| Layer | Class Pattern | Responsibility | Annotation Restrictions |
|-------|---------------|----------------|------------------------|
| **Controller** | `TurnoverController` | HTTP requests, parameter validation, return ResponseDTO | No @Transactional |
| **Service** | `TurnoverService` | Business coordination, call Manager/Dao, return Option/Try | No @Transactional |
| **Manager** | `TurnoverCalculationManager` | Transaction management, cross-table operations, cache control | @Transactional ONLY here |
| **Dao** | `BetTurnoverRecordDao` | Database CRUD, MyBatis Mapper | No business logic |
| **Entity** | `BetTurnoverRecordEntity` | Data model, 1:1 table mapping | No business logic |

### 9.2 Entity Layer

```java
@Data
@TableName("t_bet_turnover_record")
public class BetTurnoverRecordEntity extends SmartBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Bet ID */
    private String betId;

    /** Player ID */
    private Long playerId;

    /** Game type */
    private String gameType;

    // ========== Layer 1 Results (v2.1.0 update) ==========
    /** Effective turnover base (Layer 1 risk engine output) */
    private BigDecimal effectiveTurnoverBase;

    /** Risk action type: BLOCK/FLAG/PASS (v2.1.0) */
    private String actionType;

    /** Matched rules list (v2.1.0) */
    private String matchedRules; // JSON array

    /** Risk proposal ID (v2.1.0) */
    private String riskProposalId;

    // ========== Layer 2 Results ==========
    /** Bet status */
    private String status;

    /** Status factor */
    private BigDecimal statusFactor;

    /** Finance valid turnover (Layer 2 output) */
    private BigDecimal validTurnoverFinance;

    // ========== Layer 3 Results ==========
    /** Activity valid turnover (Layer 3 output, if applicable) */
    private BigDecimal activityValidTurnover;

    /** Game weight */
    private BigDecimal gameWeight;

    // ========== Audit Fields ==========
    /** Calculation timestamp */
    private LocalDateTime calculatedAt;

    /** Three-layer calculation breakdown (JSON) */
    private String layerBreakdown;
}
```

### 9.3 Manager Layer

```java
@Component  // SmartAdmin Pattern: Manager uses @Component, not @Service
@RequiredArgsConstructor
public class TurnoverCalculationManager {

    private final BetTurnoverRecordDao betTurnoverRecordDao;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Calculate and record turnover (with transaction)
     * v2.1.0: Support config-driven risk control (BLOCK/FLAG/PASS)
     */
    @Transactional(rollbackFor = Throwable.class)
    public BetTurnoverRecordEntity calculateAndRecord(
        String betId,
        Long playerId,
        String gameType,
        BigDecimal betAmount,
        String status,
        BigDecimal odds
    ) {
        // Step 1: Distributed lock to prevent duplicate calculation
        RLock lock = redissonClient.getLock("turnover:calc:" + betId);
        if (!lock.tryLock()) {
            throw new BusinessException("Duplicate turnover calculation");
        }

        try {
            // Step 2: Call Layer 1 risk engine
            RiskValidationResult riskResult = riskEngineClient.validateTurnover(
                betId, playerId, gameType, betAmount, odds
            );

            // Step 2.1: BLOCK rule short-circuit return
            if (!riskResult.isValid() && "BLOCK".equals(riskResult.getActionType())) {
                return createBlockedRecord(betId, playerId, riskResult);
            }

            // Step 2.2: FLAG rule records risk mark
            if ("FLAG".equals(riskResult.getActionType())) {
                recordRiskFlag(betId, riskResult.getRiskProposalId());
            }

            // Step 3: Layer 2 finance status factor
            BigDecimal statusFactor = getStatusFactor(status);
            BigDecimal validTurnoverFinance = riskResult.getEffectiveTurnoverBase()
                .multiply(statusFactor);

            // Step 4: Layer 3 activity weight (if applicable)
            BigDecimal gameWeight = getGameWeight(gameType);
            BigDecimal activityValidTurnover = validTurnoverFinance.multiply(gameWeight);

            // Step 5: Record turnover (three-layer results)
            BetTurnoverRecordEntity record = new BetTurnoverRecordEntity();
            record.setBetId(betId);
            record.setPlayerId(playerId);
            record.setGameType(gameType);

            // Layer 1 results
            record.setEffectiveTurnoverBase(riskResult.getEffectiveTurnoverBase());
            record.setActionType(riskResult.getActionType());
            record.setMatchedRules(JSON.toJSONString(riskResult.getMatchedRules()));
            record.setRiskProposalId(riskResult.getRiskProposalId());

            // Layer 2 results
            record.setStatus(status);
            record.setStatusFactor(statusFactor);
            record.setValidTurnoverFinance(validTurnoverFinance);

            // Layer 3 results
            record.setGameWeight(gameWeight);
            record.setActivityValidTurnover(activityValidTurnover);

            record.setCalculatedAt(LocalDateTime.now());

            // Insert to database
            betTurnoverRecordDao.insert(record);

            // Step 6: Publish event to Kafka
            kafkaTemplate.send("finance.turnover.calculated", betId, JSON.toJSONString(record));

            return record;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Query turnover record (with cache)
     */
    @Cacheable(value = "turnover", key = "#betId")
    public Option<BetTurnoverRecordEntity> getTurnoverByBetId(String betId) {
        return Option.of(betTurnoverRecordDao.selectByBetId(betId));
    }
}
```

### 9.4 Service Layer

```java
@Service
@RequiredArgsConstructor
public class TurnoverService {

    private final TurnoverCalculationManager turnoverCalculationManager;
    private final BetTurnoverRecordDao betTurnoverRecordDao;

    /**
     * Calculate turnover (business coordination)
     */
    public Option<BetTurnoverRecordEntity> calculateTurnover(
        String betId,
        Long playerId,
        String gameType,
        BigDecimal betAmount,
        String status,
        BigDecimal odds
    ) {
        // Parameter validation
        if (StringUtils.isBlank(betId)) {
            return Option.none();
        }

        // Check if already calculated
        Option<BetTurnoverRecordEntity> existing =
            turnoverCalculationManager.getTurnoverByBetId(betId);
        if (existing.isDefined()) {
            return existing;
        }

        // Call Manager for calculation
        try {
            BetTurnoverRecordEntity record = turnoverCalculationManager.calculateAndRecord(
                betId, playerId, gameType, betAmount, status, odds
            );
            return Option.of(record);
        } catch (Exception e) {
            log.error("Failed to calculate turnover: betId={}", betId, e);
            return Option.none();
        }
    }
}
```

### 9.5 Controller Layer

```java
@RestController
@Api(tags = "Turnover Calculation")
@RequiredArgsConstructor
public class TurnoverController {

    private final TurnoverService turnoverService;

    /**
     * Query player turnover records
     */
    @GetMapping("/api/turnover/player/{playerId}")
    @ApiOperation("Query player turnover")
    public ResponseDTO<List<BetTurnoverRecordEntity>> getPlayerTurnover(
        @PathVariable Long playerId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        List<BetTurnoverRecordEntity> records =
            turnoverService.getPlayerTurnover(playerId, date);
        return ResponseDTO.ok(records);
    }

    /**
     * Query single bet turnover
     */
    @GetMapping("/api/turnover/bet/{betId}")
    @ApiOperation("Query bet turnover")
    public ResponseDTO<BetTurnoverRecordEntity> getTurnoverByBet(@PathVariable String betId) {
        Option<BetTurnoverRecordEntity> record =
            turnoverService.calculateTurnover(betId, null, null, null, null, null);

        return record.map(ResponseDTO::ok)
            .getOrElse(() -> ResponseDTO.error(ErrorCodeEnum.DATA_NOT_EXIST));
    }
}
```

---

## 10. End-to-End Sequence Diagram

```mermaid
sequenceDiagram
    autonumber

    participant Player as Player
    participant Game as Game Provider
    participant Platform as Platform Core
    participant Risk as Risk Engine<br/>(05-01)
    participant Finance as Finance Center<br/>(02-04)
    participant Activity as Activity System<br/>(04-01)
    participant Wallet as Wallet System<br/>(02-06)
    participant DB as Database

    rect rgb(240, 248, 255)
        Note over Player,Game: ===== Phase 1: Betting Phase =====
    end

    Player->>Game: 1. Place Bet<br/>Amount: $100, Game: Baccarat, Odds: 1.95
    Game->>Platform: 2. Debit Request
    Platform->>Wallet: 3. Lock player funds
    Wallet->>DB: 4. Update wallet<br/>playable_balance -= 100
    DB-->>Wallet: 5. Confirm deduction
    Wallet-->>Platform: 6. Return Transaction ID
    Platform-->>Game: 7. Debit Success
    Game-->>Player: 8. Bet Confirmed<br/>Round ID: round_12345

    rect rgb(255, 250, 240)
        Note over Player,DB: ===== Phase 2: Settlement Phase =====
    end

    Note over Game: Result: Player wins $195
    Game->>Platform: 9. Credit Request<br/>Amount: $195, Status: WIN
    Platform->>DB: 10. Record bet result<br/>bet_id, status=WIN, win_amount=195

    rect rgb(240, 255, 240)
        Note over Risk,Activity: ===== Phase 3: Layer 1 Risk Validation =====
    end

    Platform->>Risk: 11. validateTurnover(bet_id)
    Risk->>Risk: 12a. Hedge Detection
    Risk->>DB: 12b. Query same-round bets
    DB-->>Risk: 12c. No hedge detected
    Risk->>Risk: 13a. Arbitrage Detection
    Risk->>Risk: 14a. Odds Validation<br/>odds=1.95 >= 1.5
    Risk-->>Platform: 15. Validation passed<br/>{is_valid: true, valid_bet: 100}

    rect rgb(255, 250, 250)
        Note over Finance,Activity: ===== Phase 4: Layer 2 Finance Recording =====
    end

    Platform->>Finance: 16. recordSettlement(bet_id)
    Finance->>Finance: 17. Record status<br/>settlement_status = "WIN"
    Finance->>DB: 19. Update bet record
    Finance-->>Platform: 20. Return result<br/>{valid_bet: 100, status: 'WIN'}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== Phase 5: Layer 3 Game Weight =====
    end

    Platform->>Activity: 21. applyGameWeight()
    Activity->>DB: 22. Query player bonuses
    DB-->>Activity: 23. Return bonus list
    Activity->>Activity: 24. Get weight<br/>Baccarat = 0.15
    Activity->>Activity: 25. Calculate contribution<br/>$100 x 0.15 = $15
    Activity->>DB: 26. Update progress<br/>wagering_completed += 15
    Activity-->>Platform: 28. Return progress<br/>{contributed: 15, progress: 0.3%}

    rect rgb(255, 245, 240)
        Note over Platform,Player: ===== Phase 6: Payout & Notification =====
    end

    Platform->>Wallet: 29. Credit $195 to player
    Wallet->>DB: 30. Update wallet balance
    Wallet-->>Platform: 31. Payout success
    Platform->>Player: 32. Push notification<br/>Won $195, Progress: +$15
```

---

## 11. Reconciliation System

### 11.1 Three-Layer Reconciliation

#### Layer 1: Real-time Stream Check
- **Timing**: 1-5 minutes after `GameEnd` or `Settlement` webhook
- **Mechanism**: Query GP API for transaction status comparison
- **Purpose**: Quick fix for latency issues

#### Layer 2: Near Real-time Batch
- **Timing**: Every 10-30 minutes
- **Mechanism**: Fetch GP history API, Anti-Join with DB
- **Purpose**: Self-healing for lost callbacks

#### Layer 3: T+1 Daily Settlement
- **Timing**: Daily at 02:00 after GP produces settlement files
- **Mechanism**: Full Outer Join comparison
- **Purpose**: Final settlement reconciliation

### 11.2 Reconciliation Data Flow

```mermaid
graph TD
    classDef database fill:#003366,stroke:#00ccff,stroke-width:2px,color:#fff;
    classDef process fill:#333333,stroke:#fff,stroke-width:2px,color:#fff;
    classDef decision fill:#800080,stroke:#ff00ff,stroke-width:2px,color:#fff;
    classDef alert fill:#990000,stroke:#ff3333,stroke-width:2px,color:#fff;
    classDef success fill:#006600,stroke:#00ff00,stroke-width:2px,color:#fff;

    GP_API[Game Provider API/File]:::process -->|1. Fetch/Download| Staging[Staging Area<br/>Raw Data]:::process

    Platform_DB[(Platform Ledger)]:::database -->|2. Extract| Reconciliation_Engine[Reconciliation Engine]:::process

    Staging --> Reconciliation_Engine

    Reconciliation_Engine -->|3. Compare Logic| Logic{Match?}:::decision

    Logic -- Yes --> Mark_Verified[Mark as Verified]:::success

    Logic -- No: Missing --> Action_Recover[Create Missing Transaction]:::process

    Logic -- No: Diff --> Action_Adjust[Create Adjustment Record]:::process

    Logic -- No: Ghost --> Alert_Risk[Trigger Risk Alert]:::alert

    Action_Recover --> SaveTx[Save Transaction]:::database

    Action_Adjust --> SaveTx

    Mark_Verified --> End((Process End)):::process

    SaveTx --> Platform_DB

    SaveTx --> CacheRes[Update Redis Cache]:::process

    CacheRes --> Resp[Generate Admin Report/API]:::process
```

---

## 12. Monitoring & Alerting

### 12.1 Prometheus Metrics

```yaml
metrics:
  # Turnover calculation performance
  - name: finance.turnover.calculation.latency_p99
    type: histogram
    description: Turnover calculation latency (P99)
    unit: milliseconds
    target: "< 100ms"

  # Risk engine call success rate
  - name: finance.turnover.risk_engine.call.success_rate
    type: gauge
    description: Risk engine call success rate
    target: "> 99.9%"

  # Daily reconciliation deviation rate
  - name: finance.turnover.daily_reconciliation.deviation_rate
    type: gauge
    description: Daily reconciliation deviation rate
    target: "< 0.01%"

  # Event publish success rate
  - name: finance.turnover.event_publish.success_rate
    type: gauge
    description: Kafka event publish success rate
    target: "> 99.99%"
```

### 12.2 Alert Rules

```yaml
alerts:
  - name: turnover_calculation_latency_high
    condition: finance.turnover.calculation.latency_p99 > 500ms
    severity: WARNING
    notify: slack:#finance-ops

  - name: risk_engine_call_failure
    condition: finance.turnover.risk_engine.call.success_rate < 99%
    severity: CRITICAL
    notify: pagerduty:finance-oncall

  - name: daily_reconciliation_deviation
    condition: finance.turnover.daily_reconciliation.deviation_rate > 0.01
    severity: WARNING
    notify: slack:#finance-ops, email:finance-team@company.com

  - name: event_publish_failure
    condition: finance.turnover.event_publish.success_rate < 99.9
    severity: CRITICAL
    notify: pagerduty:finance-oncall
```

---

## 13. ArchUnit Validation Rules

```java
@AnalyzeClasses(packages = "net.lab1024.sa.business.module.finance.turnover")
public class TurnoverModuleArchitectureTest {

    /**
     * Rule 1: Controller must not directly access Dao
     */
    @ArchTest
    static final ArchRule controllers_should_not_access_daos =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..dao..");

    /**
     * Rule 2: @Transactional only allowed in Manager layer
     */
    @ArchTest
    static final ArchRule transactional_only_in_manager =
        methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..manager..");

    /**
     * Rule 3: Service must return Option/Try (no null)
     */
    @ArchTest
    static final ArchRule service_should_return_option_or_try =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..service..")
            .and().arePublic()
            .should().haveRawReturnType(Option.class)
            .orShould().haveRawReturnType(Try.class);

    /**
     * Rule 4: Controller must use @RequiredArgsConstructor (no @Autowired)
     */
    @ArchTest
    static final ArchRule controller_should_use_constructor_injection =
        noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
            .should().beAnnotatedWith(Autowired.class);
}
```

---

## 14. Performance Considerations

### 14.1 Optimization Strategies

| Strategy | Implementation | Impact |
|----------|----------------|--------|
| **Layer 1 Short-circuit** | BLOCK rule returns immediately, skips Layer 2/3 | ~5% CPU savings |
| **Distributed Lock** | Redisson lock per bet_id | Prevents duplicate calculation |
| **Caching** | @Cacheable for turnover records | Reduces DB queries |
| **Batch Insert** | Async batch write to DB (every 10s or 1000 records) | Reduces write pressure |
| **Event-driven** | Kafka events for downstream systems | Decoupled architecture |

### 14.2 Hybrid Accumulation Architecture

```yaml
Bet Placement:
  1. Real-time calculate valid bet
  2. Write to Redis (millisecond level, player can query immediately)
  3. Async batch write to DB (every 10s or 1000 records)

Withdrawal:
  1. Read wagering_progress table directly (millisecond level)
  2. If target met, allow withdrawal

Background Reconciliation (Flink):
  1. Hourly/daily Flink job
  2. Recalculate wagering progress
  3. Compare with wagering_progress table
  4. Alert + auto-correct on discrepancy
```

---

## 15. Related Documents

### Sub-documents
- [03-04-01 Turnover Core Logic](../../source-archive/03_Game_Center/03-04-01_Turnover_Core_Logic.md)
- [03-04-02 Three Layer Validation](../../source-archive/03_Game_Center/03-04-02_Three_Layer_Validation.md)
- [03-04-03 Reconciliation Model](../../source-archive/03_Game_Center/03-04-03_Reconciliation_Model.md)
- [03-04-04 SmartAdmin Mapping](../../source-archive/03_Game_Center/03-04-04_SmartAdmin_Mapping.md)

### Business Rules
- [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)

### Architecture Dependencies
- [SmartAdmin Architecture Rules](../../../../.agent/rules/foundation/F04-architecture-rules.md)
- [02-06 Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md)

---

**Document Version**: 1.0.0 (derived from source v4.0.0)
**Last Updated**: 2026-02-08
**Maintainers**: Backend Team, Architecture Team
