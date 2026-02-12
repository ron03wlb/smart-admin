# 有效投注額計算邏輯（Turnover Calculation Logic）

> **規範來源**: [source-archive/03_Game_Center/03-04_Turnover_Calculation.md](../../source-archive/03_Game_Center/03-04_Turnover_Calculation.md)
> **目標讀者**: 架構師、後端開發
> **業務需求**: [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)
> **最後同步**: 2026-02-08
> **來源版本**: 4.0.0

---

## 1. 核心計算公式（Core Calculation Formula）

```
ValidTurnover = BetAmount × GameWeight × OddsFactor × StatusFactor × RiskFactor
```

其中：
- **RiskFactor**: `1`（通過）或 `0`（封鎖/標記）- 由第一層決定
- **StatusFactor**: `1.0`（WIN/LOSS）或 `0`（DRAW/VOID）- 由第二層決定
- **GameWeight**: `1.0`（老虎機）至 `0.05`（撲克）- 由第三層應用

---

## 2. 三層驗證架構（Three-Layer Validation Architecture）

### 2.1 架構總覽（Architecture Overview）

```mermaid
graph TB
    subgraph "玩家投注"
        A["玩家投注<br/>Amount: $100<br/>Game: Baccarat<br/>Odds: 1.95"]
    end

    subgraph "第一層：風控引擎 (05-01)"
        B["對沖檢測"]
        C["套利檢測"]
        D["低賠率過濾<br/>Threshold: 1.5"]
        E["Output: valid_bet<br/>+ action_type<br/>(BLOCK/FLAG/PASS)"]
    end

    subgraph "第二層：財務中心 (02-04)"
        F["注單結算"]
        G["記錄結算狀態"]
        H{Bet Status?}
        I["WIN/LOSS<br/>Record Status"]
        J["DRAW/TIE<br/>Record Status"]
        K["VOID/CANCEL<br/>Record Status"]
        L["HALF_WIN/LOSS<br/>Record Status"]
        M["Output: valid_bet<br/>(unchanged)"]
    end

    subgraph "第三層：活動系統 (04-01)"
        N["應用遊戲權重"]
        O{Game Type?}
        P["Slots/Sports<br/>Weight: 1.0"]
        Q["Baccarat<br/>Weight: 0.15"]
        R["Blackjack<br/>Weight: 0.1"]
        S["Roulette<br/>Weight: 0.2"]
        T["Output: activity_valid_turnover"]
    end

    subgraph "應用場景"
        U["返水計算"]
        V["有效投注額進度"]
        W["VIP 升級"]
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

## 3. 第一層：風控引擎驗證（Layer 1: Risk Engine Validation）

### 3.1 風控驗證結果介面（Risk Validation Result Interface）

```typescript
interface RiskValidationResult {
  is_valid: boolean;
  action_type: 'BLOCK' | 'FLAG' | 'PASS';
  matched_rules: string[];
  risk_proposal_id: string | null;
  effective_turnover_base: number;
}
```

### 3.2 對沖檢測流程（Hedge Detection Flow）

```mermaid
flowchart TD
    A[開始：對沖檢測]
    B["取得投注資訊<br/>player_id, round_id<br/>selection, amount"]
    C["查詢同一回合<br/>玩家所有投注"]
    D{"是否存在<br/>反向投注？"}

    subgraph Example["範例：百家樂"]
        E1[投注莊家 $1000]
        E2[投注閒家 $950]
        E3["對沖檢測<br/>反向投注"]
    end

    F[標記為對沖投注]
    G["action_type = BLOCK<br/>(或依配置 FLAG)"]
    H[effective_turnover = 0]
    I[未檢測到對沖]
    J[繼續驗證]

    End([結束：返回結果])

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

### 3.3 賠率門檻驗證（Odds Threshold Validation）

```mermaid
flowchart TD
    A[開始：賠率驗證]
    B["讀取配置<br/>MIN_ODDS_THRESHOLD = 1.5"]
    C["取得投注賠率<br/>odds = 1.95"]
    D{odds >= threshold?}
    E["賠率有效<br/>通過驗證"]
    F["低賠率投注<br/>action_type = BLOCK"]
    G[effective_turnover = 0]

    subgraph Examples["賠率範例"]
        EX1[1.95 -> 通過]
        EX2[1.50 -> 通過]
        EX3[1.30 -> 拒絕]
        EX4[1.01 -> 拒絕]
    end

    End([返回結果])

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

### 3.4 第一層處理邏輯（Layer 1 Processing Logic）（v2.1.0）

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

## 4. 第二層：財務狀態記錄（Layer 2: Finance Status Recording）

### 4.1 狀態因子對應（Status Factor Mapping）

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

### 4.2 財務層流程（Finance Layer Flow）

```mermaid
flowchart TD
    A[開始：財務層]
    B["輸入：valid_bet<br/>= $100<br/>from Layer 1, immutable"]
    C["取得投注狀態<br/>bet.status"]
    D{Bet Status}

    E["WIN<br/>玩家獲勝"]
    F["Record: settlement_status = WIN<br/>Calculate payout"]

    G["LOSS<br/>玩家輸"]
    H["Record: settlement_status = LOSS<br/>Calculate payout"]

    I["DRAW/TIE<br/>平局"]
    J["Record: settlement_status = DRAW<br/>Return stake"]

    K["VOID/CANCEL<br/>作廢"]
    L["Record: settlement_status = VOID<br/>Return stake"]

    M["HALF_WIN/HALF_LOSS<br/>部分結果"]
    N["Record: settlement_status<br/>Calculate partial payout"]

    O["更新資料庫<br/>settlement_status<br/>payout_amount"]

    P["valid_bet 不變<br/>= $100<br/>不受狀態影響"]

    End([返回：valid_bet $100<br/>+ settlement_status])

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

## 5. 第三層：活動權重應用（Layer 3: Activity Weight Application）

### 5.1 遊戲權重配置（Game Weight Configuration）

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

### 5.2 活動層流程（Activity Layer Flow）

```mermaid
flowchart TD
    A[開始：活動層]
    B["輸入：valid_turnover_finance<br/>= $100"]
    C["取得遊戲類型<br/>game_type"]
    D{Game Type}

    E["老虎機<br/>Slot Machine"]
    F["game_weight = 1.0<br/>100% 貢獻"]

    G["體育博彩<br/>Sports Betting"]
    H["game_weight = 1.0<br/>100% 貢獻"]

    I["百家樂<br/>Baccarat"]
    J["game_weight = 0.15<br/>15% 貢獻"]

    K["21點<br/>Blackjack"]
    L["game_weight = 0.1<br/>10% 貢獻"]

    M["輪盤<br/>Roulette"]
    N["game_weight = 0.2<br/>20% 貢獻"]

    O["真人娛樂場<br/>Live Dealer"]
    P["game_weight = 0.15<br/>15% 貢獻"]

    Q["計算活動有效投注額<br/>activity_valid_turnover<br/>= finance x weight"]

    R["查詢玩家紅利<br/>player_bonuses"]
    S["更新流水進度<br/>wagering_completed += activity_valid_turnover"]
    T["計算完成百分比<br/>progress = completed / required"]

    U{流水完成？}
    V["標記活動完成<br/>status = 'completed'"]
    W["解鎖提款<br/>Update withdrawable balance"]
    X["繼續追蹤<br/>status = 'active'"]

    End([返回：流水進度])

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

    U -->|是| V
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

---

## 6. 依遊戲類型計算有效投注（Valid Bet Calculation by Game Type）

### 6.1 計算公式（Calculation Formulas）

| 遊戲類型 | 條件 | effectiveStake 公式 |
|---------|------|-------------------|
| **SPORTS / E-SPORTS** | - | `|winAmount + lossAmount|` |
| **CASINO** | 平局（payout == betAmount）| `0` |
| **CASINO** | 獲勝（winAmount > 0）| `min(winAmount, betAmount)` |
| **CASINO** | 輸（winAmount == 0）| `betAmount` |
| **其他類型** | - | `betAmount` |

### 6.2 實作（Implementation）

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

### 6.3 effectiveStake 與 lockAmount 關係

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

**範例**：
- 之前：lockAmount=500, effectiveStake=100
- 注單結算產生 effectiveStake=200
- 之後：lockAmount=300, effectiveStake=300

---

## 7. 免費旋轉有效投注額計算（Free Spins Turnover Calculation）

### 7.1 GGR 計算實作（GGR Calculation Implementation）

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

## 8. 資料庫結構（Database Schema）

### 8.1 流水詳情表（Wagering Details Table）

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

### 8.2 流水進度表（Wagering Progress Table）

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

### 8.3 重算任務表（Recalculation Task Table）

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

## 9. SmartAdmin 層級對應（SmartAdmin Layer Mapping）

### 9.1 層級職責（Layer Responsibilities）

| 層級 | 類別模式 | 職責 | 註解限制 |
|------|---------|------|---------|
| **Controller** | `TurnoverController` | HTTP 請求、參數驗證、返回 ResponseDTO | 不可使用 @Transactional |
| **Service** | `TurnoverService` | 業務協調、調用 Manager/Dao、返回 Option/Try | 不可使用 @Transactional |
| **Manager** | `TurnoverCalculationManager` | 交易管理、跨表操作、快取控制 | 僅此層可使用 @Transactional |
| **Dao** | `BetTurnoverRecordDao` | 資料庫 CRUD、MyBatis Mapper | 無業務邏輯 |
| **Entity** | `BetTurnoverRecordEntity` | 資料模型、1:1 表格映射 | 無業務邏輯 |

### 9.2 Entity 層

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

### 9.3 Manager 層

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

### 9.4 Service 層

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

### 9.5 Controller 層

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

## 10. 端到端時序圖（End-to-End Sequence Diagram）

```mermaid
sequenceDiagram
    autonumber

    participant Player as 玩家
    participant Game as 遊戲供應商
    participant Platform as 平台核心
    participant Risk as 風控引擎<br/>(05-01)
    participant Finance as 財務中心<br/>(02-04)
    participant Activity as 活動系統<br/>(04-01)
    participant Wallet as 錢包系統<br/>(02-06)
    participant DB as 資料庫

    rect rgb(240, 248, 255)
        Note over Player,Game: ===== 階段 1：投注階段 =====
    end

    Player->>Game: 1. 下注<br/>Amount: $100, Game: Baccarat, Odds: 1.95
    Game->>Platform: 2. Debit Request
    Platform->>Wallet: 3. 鎖定玩家資金
    Wallet->>DB: 4. 更新錢包<br/>playable_balance -= 100
    DB-->>Wallet: 5. 確認扣除
    Wallet-->>Platform: 6. Return Transaction ID
    Platform-->>Game: 7. Debit Success
    Game-->>Player: 8. 投注確認<br/>Round ID: round_12345

    rect rgb(255, 250, 240)
        Note over Player,DB: ===== 階段 2：結算階段 =====
    end

    Note over Game: 結果：玩家獲勝 $195
    Game->>Platform: 9. Credit Request<br/>Amount: $195, Status: WIN
    Platform->>DB: 10. 記錄投注結果<br/>bet_id, status=WIN, win_amount=195

    rect rgb(240, 255, 240)
        Note over Risk,Activity: ===== 階段 3：第一層風控驗證 =====
    end

    Platform->>Risk: 11. validateTurnover(bet_id)
    Risk->>Risk: 12a. 對沖檢測
    Risk->>DB: 12b. 查詢同回合投注
    DB-->>Risk: 12c. 未檢測到對沖
    Risk->>Risk: 13a. 套利檢測
    Risk->>Risk: 14a. 賠率驗證<br/>odds=1.95 >= 1.5
    Risk-->>Platform: 15. 驗證通過<br/>{is_valid: true, valid_bet: 100}

    rect rgb(255, 250, 250)
        Note over Finance,Activity: ===== 階段 4：第二層財務記錄 =====
    end

    Platform->>Finance: 16. recordSettlement(bet_id)
    Finance->>Finance: 17. 記錄狀態<br/>settlement_status = "WIN"
    Finance->>DB: 19. 更新投注記錄
    Finance-->>Platform: 20. 返回結果<br/>{valid_bet: 100, status: 'WIN'}

    rect rgb(248, 240, 255)
        Note over Activity,Wallet: ===== 階段 5：第三層遊戲權重 =====
    end

    Platform->>Activity: 21. applyGameWeight()
    Activity->>DB: 22. 查詢玩家紅利
    DB-->>Activity: 23. 返回紅利列表
    Activity->>Activity: 24. 取得權重<br/>Baccarat = 0.15
    Activity->>Activity: 25. 計算貢獻<br/>$100 x 0.15 = $15
    Activity->>DB: 26. 更新進度<br/>wagering_completed += 15
    Activity-->>Platform: 28. 返回進度<br/>{contributed: 15, progress: 0.3%}

    rect rgb(255, 245, 240)
        Note over Platform,Player: ===== 階段 6：支付與通知 =====
    end

    Platform->>Wallet: 29. 派彩 $195 給玩家
    Wallet->>DB: 30. 更新錢包餘額
    Wallet-->>Platform: 31. 支付成功
    Platform->>Player: 32. 推送通知<br/>獲勝 $195，流水進度：+$15
```

---

## 11. 對帳系統（Reconciliation System）

### 11.1 三層對帳（Three-Layer Reconciliation）

#### 第一層：即時流式檢查（Layer 1: Real-time Stream Check）
- **時機**：`GameEnd` 或 `Settlement` webhook 後 1-5 分鐘
- **機制**：查詢 GP API 進行交易狀態比對
- **目的**：快速修復延遲問題

#### 第二層：準即時批次（Layer 2: Near Real-time Batch）
- **時機**：每 10-30 分鐘
- **機制**：抓取 GP 歷史 API，與資料庫進行 Anti-Join
- **目的**：自我修復丟失的回調

#### 第三層：T+1 每日結算（Layer 3: T+1 Daily Settlement）
- **時機**：每日 02:00（GP 產生結算檔案後）
- **機制**：完整外部連接比對
- **目的**：最終結算對帳

### 11.2 對帳資料流（Reconciliation Data Flow）

```mermaid
graph TD
    classDef database fill:#003366,stroke:#00ccff,stroke-width:2px,color:#fff;
    classDef process fill:#333333,stroke:#fff,stroke-width:2px,color:#fff;
    classDef decision fill:#800080,stroke:#ff00ff,stroke-width:2px,color:#fff;
    classDef alert fill:#990000,stroke:#ff3333,stroke-width:2px,color:#fff;
    classDef success fill:#006600,stroke:#00ff00,stroke-width:2px,color:#fff;

    GP_API[Game Provider API/檔案]:::process -->|1. 抓取/下載| Staging[暫存區<br/>原始資料]:::process

    Platform_DB[(平台帳本)]:::database -->|2. 提取| Reconciliation_Engine[對帳引擎]:::process

    Staging --> Reconciliation_Engine

    Reconciliation_Engine -->|3. 比對邏輯| Logic{匹配？}:::decision

    Logic -->|是| Mark_Verified[標記已驗證]:::success

    Logic -->|否：缺失| Action_Recover[建立缺失交易]:::process

    Logic -->|否：差異| Action_Adjust[建立調整記錄]:::process

    Logic -->|否：幽靈| Alert_Risk[觸發風險告警]:::alert

    Action_Recover --> SaveTx[儲存交易]:::database

    Action_Adjust --> SaveTx

    Mark_Verified --> End((流程結束)):::process

    SaveTx --> Platform_DB

    SaveTx --> CacheRes[更新 Redis 快取]:::process

    CacheRes --> Resp[產生管理報告/API]:::process
```

---

## 12. 監控與告警（Monitoring & Alerting）

### 12.1 Prometheus 指標

```yaml
metrics:
  # Turnover calculation performance
  - name: finance.turnover.calculation.latency_p99
    type: histogram
    description: 有效投注額計算延遲（P99）
    unit: milliseconds
    target: "< 100ms"

  # Risk engine call success rate
  - name: finance.turnover.risk_engine.call.success_rate
    type: gauge
    description: 風控引擎調用成功率
    target: "> 99.9%"

  # Daily reconciliation deviation rate
  - name: finance.turnover.daily_reconciliation.deviation_rate
    type: gauge
    description: 每日對帳偏差率
    target: "< 0.01%"

  # Event publish success rate
  - name: finance.turnover.event_publish.success_rate
    type: gauge
    description: Kafka 事件發布成功率
    target: "> 99.99%"
```

### 12.2 告警規則（Alert Rules）

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

## 13. ArchUnit 驗證規則（ArchUnit Validation Rules）

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

## 14. 效能考量（Performance Considerations）

### 14.1 優化策略（Optimization Strategies）

| 策略 | 實作 | 影響 |
|-----|------|-----|
| **第一層短路** | BLOCK 規則立即返回，跳過第二/三層 | ~5% CPU 節省 |
| **分散式鎖** | Redisson 鎖（每個 bet_id）| 防止重複計算 |
| **快取** | @Cacheable 用於有效投注額記錄 | 減少資料庫查詢 |
| **批次插入** | 非同步批次寫入資料庫（每 10 秒或 1000 筆）| 減少寫入壓力 |
| **事件驅動** | Kafka 事件用於下游系統 | 解耦架構 |

### 14.2 混合累積架構（Hybrid Accumulation Architecture）

```yaml
下注階段：
  1. 即時計算有效投注額
  2. 寫入 Redis（毫秒級，玩家可立即查詢）
  3. 非同步批次寫入資料庫（每 10 秒或 1000 筆）

提款：
  1. 直接讀取 wagering_progress 表（毫秒級）
  2. 若達標，允許提款

背景對帳 (Flink)：
  1. 每小時/每日 Flink 作業
  2. 重新計算流水進度
  3. 與 wagering_progress 表比對
  4. 發現差異時告警 + 自動修正
```

---

## 15. 相關文檔

### 子文檔
- [03-04-01 Turnover Core Logic](../../source-archive/03_Game_Center/03-04-01_Turnover_Core_Logic.md)
- [03-04-02 Three Layer Validation](../../source-archive/03_Game_Center/03-04-02_Three_Layer_Validation.md)
- [03-04-03 Reconciliation Model](../../source-archive/03_Game_Center/03-04-03_Reconciliation_Model.md)
- [03-04-04 SmartAdmin Mapping](../../source-archive/03_Game_Center/03-04-04_SmartAdmin_Mapping.md)

### 業務規則
- [Turnover_Business_Rules.md](../../requirements/03_Gaming_Operations/Turnover_Business_Rules.md)

### 架構依賴
- [SmartAdmin Architecture Rules](../../../../.agent/rules/foundation/F04-architecture-rules.md)
- [02-06 Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md)

---

**文檔版本**: 1.0.0 (derived from source v4.0.0)
**最後更新**: 2026-02-08
**維護團隊**: Backend Team, Architecture Team
