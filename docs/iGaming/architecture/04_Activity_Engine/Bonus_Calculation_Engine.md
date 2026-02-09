# 獎金計算引擎技術架構 (Bonus Calculation Engine Architecture)

> **Canonical Source**: [source/04_Activity_Center/04-02_Bonus_Calculation_Engine.md](../../source-archive/04_Activity_Center/04-02_Bonus_Calculation_Engine.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: [Bonus_Calculation_Requirements.md](../../requirements/04_Promotions_VIP/Bonus_Calculation_Requirements.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 1.0.0

---

## 概述

本文檔詳述活動系統獎金計算引擎的技術實現，包括三層流水計算架構、事件驅動處理流程、API 契約定義，以及跨模組一致性保障機制。這是確保活動系統與財務系統數據一致性的關鍵技術模塊。

---

## 三層流水計算架構 (Three-Layer Turnover Calculation)

### 架構概覽

```
┌─────────────────────────────────────────────────────────────────┐
│              Unified Turnover Validation Stack                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: Base Validation (Single Source of Truth)             │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  RiskEngine.validateTurnover() (05-01 Risk Control)       │ │
│  │  - Filter: Hedge Detection (對沖投注檢測)                  │ │
│  │  - Filter: Arbitrage Detection (套利投注檢測)              │ │
│  │  - Filter: Low Odds (<1.5, configurable)                  │ │
│  │  - Output: effective_turnover_base (基礎有效流水)          │ │
│  └───────────────────────────────────────────────────────────┘ │
│                           ↓                                     │
│  Layer 2: Finance Layer (02-04 Finance Center)                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  effective_turnover_base × status_factor                  │ │
│  │  - WIN: 1.0 (玩家贏,計入流水)                              │ │
│  │  - LOSS: 1.0 (玩家輸,計入流水)                             │ │
│  │  - DRAW/TIE: 0 (和局,無風險,不計流水)                      │ │
│  │  - VOID/CANCEL: 0 (注單作廢)                               │ │
│  │  - HALF_WIN/HALF_LOSS: 0.5 (半贏半輸)                      │ │
│  │  → Output: valid_turnover_finance (財務有效流水)           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                           ↓                                     │
│  Layer 3: Activity Layer (04-01 Activity Center)               │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  valid_turnover_finance × game_weight                     │ │
│  │  - Slots (老虎機): 1.0 (100% contribution)                 │ │
│  │  - Sports (體育): 1.0                                      │ │
│  │  - Baccarat (百家樂): 0.1-0.15 (10-15%)                    │ │
│  │  - Blackjack (二十一點): 0.05-0.1 (5-10%)                  │ │
│  │  - Roulette (輪盤): 0.1-0.2                                │ │
│  │  → Output: activity_valid_turnover (活動有效流水)          │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 計算公式

```
ValidTurnover = BetAmount
                × Layer1_RiskFactor     (05-01: 0 or 1)
                × Layer2_StatusFactor   (02-04: 0%, 50%, 100%)
                × Layer3_GameWeight     (本節: 5%-100%)
```

### 計算鏈路示例

假設玩家在百家樂 (Baccarat) 投注 $100，賠率 1.95 (歐洲盤)，最終結果 WIN:

```
Step 1 (Risk Engine - 05-01):
  Input: bet_amount = $100, odds = 1.95, game = BACCARAT
  Check: odds >= 1.5 ✓, no hedge detected ✓
  Output: effective_turnover_base = $100

Step 2 (Finance Layer - 02-04):
  Input: effective_turnover_base = $100, status = WIN
  Apply: status_factor = 1.0 (WIN counts as valid)
  Output: valid_turnover_finance = $100 × 1.0 = $100

Step 3 (Activity Layer - 04-01):
  Input: valid_turnover_finance = $100, game = BACCARAT
  Apply: game_weight = 0.15 (15% contribution for baccarat)
  Output: activity_valid_turnover = $100 × 0.15 = $15

Result:
  - Finance System records: $100 valid turnover (for VIP/rebate)
  - Activity System records: $15 wagering progress (for bonus clearing)
```

---

## 事件驅動架構 (Event-Driven Architecture)

### 處理流程

```
玩家動作 → 遊戲服務 → Kafka Topic →
  → 活動觸發消費者
    → 資格檢查服務
    → 獎勵計算服務
    → 錢包服務（發放）
    → 通知服務（推播/站內信）
```

### Kafka Topic 結構

```
Topics:
├── player.activity.casino     # 娛樂場遊戲活動
├── player.activity.sports     # 體育投注活動
├── player.activity.poker      # 撲克活動
├── player.deposits            # 存款事件
├── player.registrations       # 註冊事件
├── promotion.triggers         # 活動觸發
├── promotion.claims           # 活動領取
├── reward.distributions       # 獎勵發放
└── wagering.updates           # 流水更新
```

### 玩家活動追蹤事件結構

```json
{
  "eventType": "GAME_ROUND_COMPLETED",
  "timestamp": "2026-01-24T15:30:00+08:00",
  "playerId": "player-uuid",
  "tenantId": "operator-uuid",
  "vertical": "CASINO",
  "gameType": "SLOTS",
  "gameId": "game-uuid",
  "providerId": "provider-uuid",
  "data": {
    "betAmount": 10.00,
    "winAmount": 25.00,
    "currency": "USD",
    "roundId": "round-uuid"
  },
  "bonusContext": {
    "activeBonusId": "bonus-uuid",
    "contributionRate": 100,
    "effectiveContribution": 10.00,
    "progressBefore": 500.00,
    "progressAfter": 510.00,
    "requiredTotal": 3000.00
  }
}
```

---

## 流水驗證流程 (Turnover Validation Flow)

### 處理時序

```mermaid
sequenceDiagram
    GameService->>Kafka: 1. Pub GameRound
    PromotionService->>Kafka: 2. Sub GameRound
    PromotionService->>RiskEngine: 3. Async Validate (Batch)
    RiskEngine-->>PromotionService: 4. Callback / Pub Result
    alt is Valid
        PromotionService->>BonusWallet: 5. Commit Progress
    else is Invalid
        PromotionService->>AuditLog: 5. Log "Risk Rejected"
        PromotionService->>Player: 6. Notify (Optional)
    end
```

### 驗證狀態流轉

| 初始狀態 | 風控結果 | 最終狀態 | 後續動作 |
|---------|---------|---------|---------|
| PENDING | Valid | COMPLETED | 累積流水進度 |
| PENDING | Hedge Detected | REJECTED | 記錄原因、若已發放則 Rollback |
| PENDING | Arbitrage | REJECTED | 記錄原因、觸發風控警報 |
| PENDING | Low Odds | REJECTED | 記錄原因 |

---

## API 契約定義 (API Contract)

### Base Validation API (Risk Engine)

```typescript
// Risk Engine provides this API for cross-module consumption
interface ValidateTurnoverRequest {
  bet_id: string;
  player_id: string;
  game_type: GameType;  // SLOTS, SPORTS, BACCARAT, etc.
  bet_amount: number;
  odds: number;
  odds_type: OddsType;  // EUR, HK, MY, ID
  bet_pattern?: BetPattern[];  // For multi-bet analysis
}

interface ValidateTurnoverResponse {
  is_valid: boolean;
  effective_turnover_base: number;  // Base valid turnover after risk checks
  risk_code: RiskCode;  // VALID, HEDGE_DETECTED, ARBITRAGE, LOW_ODDS
  rejection_reason?: string;
}

// Example usage:
const result = await RiskEngine.validateTurnover({
  bet_id: "bet-uuid-001",
  player_id: "player-uuid-123",
  game_type: GameType.BACCARAT,
  bet_amount: 100,
  odds: 1.95,
  odds_type: OddsType.EUR
});
// Returns: { is_valid: true, effective_turnover_base: 100, risk_code: "VALID" }
```

### 衝突處理配置結構

```json
{
  "conflict_resolution_config": {
    "default_policy": "TYPE_EXCLUSIVE",
    "rules": [
      {
        "conflict_group": "first_deposit_bonuses",
        "type": "MUTUALLY_EXCLUSIVE",
        "resolution_strategy": "MAX_REWARD",
        "members": ["promo-first-deposit-100", "promo-first-deposit-200", "promo-vip-first-deposit"],
        "reason": "首存活動互斥，自動選擇獎勵最高的"
      },
      {
        "conflict_group": "cashback_programs",
        "type": "STACKABLE",
        "resolution_strategy": "STACK_ALL",
        "max_total_percentage": 5.0,
        "members": ["daily-cashback", "vip-cashback", "game-specific-cashback"],
        "reason": "返水可疊加但總計不超過 5%"
      },
      {
        "conflict_group": "free_spins_offers",
        "type": "PLAYER_CHOICE",
        "resolution_strategy": "PLAYER_SELECT",
        "max_selections": 1,
        "members": ["free-spins-50", "free-spins-100-wagered", "mega-spins-10"],
        "reason": "免費旋轉類型差異大，讓玩家選擇"
      }
    ],
    "wagering_mode": {
      "default": "ISOLATED",
      "cross_category_shared": false,
      "completion_order": "PRIORITY_ASC"
    },
    "game_contribution_policy": {
      "default": "UNIFIED",
      "allow_activity_override": true,
      "conflict_resolution": "MIN_CONTRIBUTION"
    },
    "global_limits": {
      "max_active_bonuses_per_player": 5,
      "max_total_bonus_balance": 10000,
      "max_daily_claim_count": 3
    }
  }
}
```

### 遊戲權重配置

```json
{
  "game_weights": {
    "SLOTS": 1.0,
    "SPORTS": 1.0,
    "BACCARAT": 0.15,
    "BLACKJACK": 0.10,
    "ROULETTE": 0.20
  },
  "odds_thresholds": {
    "EUR": 1.5,
    "HK": 0.5,
    "MY": 0.5,
    "ID": 1.2
  }
}
```

---

## 跨模組一致性機制 (Cross-Module Consistency)

### 統一事件源

所有注單結算事件 `GameRound.Settled` 必須同時發送至:
- Finance Turnover Service (財務流水服務)
- Activity Wagering Service (活動流水服務)

兩者均訂閱相同的 Kafka Topic: `game.rounds.settled`

### 同步驗證調用

Finance 與 Activity 模組均需調用 `RiskEngine.validateTurnover()` 作為第一步，不可各自實作風控邏輯。

### 配置集中管理

遊戲權重表 (Game Weight Table) 必須存放於統一配置服務 (Config Service)，Finance 與 Activity 模組均需從此服務讀取，禁止硬編碼。

---

## 技術棧推薦

| 層級 | 技術選型 | 用途 |
|------|---------|------|
| API 閘道 | Kong / AWS API Gateway | 限流、認證、路由 |
| 服務通訊 | gRPC（內部）/ REST（外部）| 高效內部調用 |
| 事件串流 | Apache Kafka + Kafka Streams | 即時事件處理 |
| 主數據庫 | PostgreSQL + JSONB | 活動配置、ACID 事務 |
| 高併發寫入 | ScyllaDB / CockroachDB | 流水記錄、交易日誌 |
| 緩存 | Redis Cluster | 玩家狀態、活動快取 |
| 搜尋 | Elasticsearch | 活動搜尋、玩家查詢 |

---

## SmartAdmin 層級對應

### 架構映射

| 獎金引擎組件 | SmartAdmin 層級 | 說明 |
|-------------|----------------|------|
| Promotion API | Controller | REST 端點定義 |
| Bonus Calculation Service | Service | 業務邏輯編排 |
| Turnover Validation Manager | Manager | @Transactional 流水驗證 |
| Bonus Progress Dao | Dao | 流水進度持久化 |
| Risk Engine Client | Service | 調用風控 API |

### 關鍵類別設計

```java
// Controller - API 端點
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping("/claim")
    public ResponseDTO<ClaimResultVO> claimPromotion(@RequestBody ClaimPromotionForm form) {
        return ResponseDTO.ok(promotionService.claimPromotion(form));
    }
}

// Service - 業務編排
@Service
@RequiredArgsConstructor
public class PromotionService {
    private final TurnoverValidationManager turnoverValidationManager;
    private final BonusProgressDao bonusProgressDao;

    public Option<BonusProgressVO> calculateProgress(TurnoverEventForm event) {
        // Layer 1: 風控驗證
        return turnoverValidationManager.validateAndCalculate(event);
    }
}

// Manager - 事務管理
@Component
@RequiredArgsConstructor
public class TurnoverValidationManager {
    private final RiskEngineClient riskEngineClient;
    private final BonusProgressDao bonusProgressDao;

    @Transactional(rollbackFor = Throwable.class)
    public Option<BonusProgressVO> validateAndCalculate(TurnoverEventForm event) {
        // 三層計算邏輯
        // Layer 1: RiskEngine validation
        // Layer 2: Status factor
        // Layer 3: Game weight
        return Option.of(progressVO);
    }
}
```

---

## 相關文檔

### 技術架構
- [活動引擎架構索引](README.md)
- [風控系統架構](../05_Risk_Engine/Risk_System_Architecture.md)

### 業務需求
- [獎金計算業務需求](../../requirements/04_Promotions_VIP/Bonus_Calculation_Requirements.md)

---

**文檔版本**: 1.0.0
**創建日期**: 2026-02-08
**維護團隊**: Backend Team
