---
title: "Ch4: 遊戲整合技術架構"
part: technical
module: game-integration
version: v2.2
created: 2026-03-24
---

# 第 4 章：遊戲整合技術架構

## 4.1 模組概述

Game Provider adapter pattern with unified callback handling, token-based authentication, and real-time RTP monitoring.

Key responsibilities:
- Multi-provider game adapter pattern for seamless integration
- Unified callback handling for all game provider events
- Token-based authentication and session management
- Real-time RTP (Return to Player) monitoring and circuit breaking
- Seamless and transfer wallet integration
- Game session lifecycle management
- Health monitoring and auto-failover

## 4.2 資料模型

### t_game_provider
Primary storage for game provider configurations and credentials.

| Column | Type | Description |
|--------|------|-------------|
| provider_code | VARCHAR(50) | Unique provider identifier (e.g., "pgsoft", "pragmatic", "evolution") |
| name | VARCHAR(255) | Display name of the provider |
| api_base_url | VARCHAR(500) | Base URL for provider API endpoints |
| api_key | VARCHAR(500) | Encrypted API key for authentication |
| secret_key | VARCHAR(500) | Encrypted secret key for HMAC signature |
| ip_whitelist | TEXT | Comma-separated IP addresses allowed for callbacks |
| status | ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'SUSPENDED') | Current operational status |
| wallet_type | ENUM('SEAMLESS', 'TRANSFER') | Integration wallet model |
| supported_currencies | JSON | Array of supported currency codes |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### t_game
Master catalog of available games.

| Column | Type | Description |
|--------|------|-------------|
| game_code | VARCHAR(100) | Unique game identifier within provider |
| provider_code | VARCHAR(50) | Foreign key to t_game_provider |
| game_type | ENUM('SLOT', 'LIVE', 'TABLE', 'SPORTS', 'FISHING', 'LOTTERY', 'ESPORTS') | Game category |
| name | VARCHAR(255) | Game display name |
| rtp_theoretical | DECIMAL(5,2) | Theoretical RTP percentage (e.g., 96.50) |
| status | ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'SUSPENDED') | Game availability status |
| metadata | JSONB | Additional properties: theme, volatility, max_win, min_bet, max_bet, languages |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### t_game_session
Track player game sessions for token management and lifecycle.

| Column | Type | Description |
|--------|------|-------------|
| session_id | VARCHAR(100) | Unique session identifier |
| player_id | BIGINT | Foreign key to player |
| game_code | VARCHAR(100) | Game being played |
| provider_code | VARCHAR(50) | Provider of the game |
| token | VARCHAR(500) | Session token for game provider |
| status | ENUM('ACTIVE', 'SUSPENDED', 'CLOSED') | Session state |
| started_at | TIMESTAMP | Session start time |
| ended_at | TIMESTAMP | Session end time |
| ip_address | VARCHAR(50) | Player IP at session start |
| user_agent | TEXT | Player browser user agent |
| created_at | TIMESTAMP | Creation timestamp |

### t_game_round
Individual game rounds/bets for financial reconciliation.

| Column | Type | Description |
|--------|------|-------------|
| round_id | VARCHAR(100) | Unique round identifier from provider |
| session_id | VARCHAR(100) | Associated game session |
| player_id | BIGINT | Player placing the bet |
| provider_code | VARCHAR(50) | Provider code |
| status | ENUM('OPEN', 'CLOSED', 'TIMEOUT', 'CANCELLED', 'PENDING_REVIEW', 'ADJUSTED') | Round state |
| total_bet | DECIMAL(19,2) | Total amount wagered |
| total_win | DECIMAL(19,2) | Total amount won |
| net_result | DECIMAL(19,2) | Net outcome (win - bet) |
| currency | VARCHAR(3) | ISO 4217 currency code |
| created_at | TIMESTAMP | Round creation time |
| updated_at | TIMESTAMP | Last state change |
| settled_at | TIMESTAMP | When round was settled |

## 4.3 GP 適配器模式

### GameProviderAdapter Interface

```java
public interface GameProviderAdapter {
    /**
     * Authenticate player and generate game session token
     * @param playerId player identifier
     * @param tenantId tenant identifier
     * @return Try containing session token
     */
    Try<String> authenticate(Long playerId, Long tenantId);

    /**
     * Process bet request
     * @param request BetRequest with game_code, amount, round_id
     * @return Try containing BetResult with balance after debit
     */
    Try<BetResult> bet(BetRequest request);

    /**
     * Settle game round (credit winnings)
     * @param request SettleRequest with round_id, amount, game_code
     * @return Try containing SettleResult with new balance
     */
    Try<SettleResult> settle(SettleRequest request);

    /**
     * Rollback bet for failed/cancelled round
     * @param request RollbackRequest with round_id, original_bet_amount
     * @return Try containing RollbackResult
     */
    Try<RollbackResult> rollback(RollbackRequest request);

    /**
     * Query current player balance
     * @param playerId player identifier
     * @param tenantId tenant identifier
     * @return Try containing balance as BigDecimal
     */
    Try<BigDecimal> getBalance(Long playerId, Long tenantId);

    /**
     * Query historical game round details
     * @param roundId round identifier
     * @return Try containing GameRoundDetail with all round info
     */
    Try<GameRoundDetail> queryRound(String roundId);

    /**
     * Get provider identifier
     * @return provider code (e.g., "pgsoft", "pragmatic", "evolution", "mock")
     */
    String getProviderCode();
}
```

### GPAdapterFactory

Single-entry point factory for adapter instantiation:

```java
@Component
public class GPAdapterFactory {
    private final Map<String, GameProviderAdapter> adapters;

    // Constructor uses Spring @Autowired to auto-discover all GameProviderAdapter implementations
    public GPAdapterFactory(List<GameProviderAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(
                GameProviderAdapter::getProviderCode,
                Function.identity()
            ));
    }

    /**
     * Lookup adapter by provider code - O(1) complexity
     * @param providerCode provider identifier
     * @return GameProviderAdapter implementation
     * @throws GameProviderNotFoundException if provider not found
     */
    public GameProviderAdapter getAdapter(String providerCode) {
        GameProviderAdapter adapter = adapters.get(providerCode);
        if (adapter == null) {
            throw new GameProviderNotFoundException(providerCode);
        }
        return adapter;
    }
}
```

### Provider Implementations

- **PGSoftAdapter**: Integrates with PG Soft provider
- **PragmaticAdapter**: Integrates with Pragmatic Play provider
- **EvolutionAdapter**: Integrates with Evolution Gaming provider
- **MockGameProviderAdapter**: For POC and testing environments
  - Returns valid tokens
  - Integrates with real WalletService
  - No actual game execution
  - Supports all callback scenarios

## 4.4 Token 驗證機制

### Seamless Wallet Authentication

Two-layer authentication for secure provider integration:

#### Layer 1: X-Api-Token Header
- Contains encrypted API token
- Identifies calling provider
- Refreshed on each request

#### Layer 2: HMAC-SHA256 Signature
- Request body is hashed with secret key
- Timestamp included to prevent replay attacks
- Signature embedded in X-Signature header

```
Request Headers:
X-Api-Token: {encrypted_api_token}
X-Signature: hmac_sha256(request_body + timestamp, secret_key)
X-Timestamp: {milliseconds_since_epoch}

Validation:
1. Extract timestamp from X-Timestamp header
2. Verify timestamp is within 5-minute tolerance
3. Reconstruct HMAC using same request_body, timestamp, secret_key
4. Compare reconstructed HMAC with X-Signature header
5. Reject if any validation fails with 401 Unauthorized
```

### Game Session Token

Generated when player launches game:
- HMAC-SHA256 signed token
- Payload: player_id, game_code, session_id, tenant_id, timestamp
- Expiry: 5 minutes from generation
- One-time use (invalidated after first game load)
- **STRICT Expiry Policy** (aligned with Ch2 §2.3.6):
  - **All money operations (Debit/Credit/Rollback/Adjust): expired token → REJECT**
  - **GetBalance only: lenient validation (signature-only, expiry ignored)**
  - No grace period. GP must complete settlement within token TTL.
  - Late GP settlements (token expired) → Resettlement flow (see Ch2 §2.3.6 `t_resettlement`)

Token format:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
{base64_encoded_payload}.
{hmac_sha256_signature}
```

## 4.5 遊戲啟動序列

### Sequence Diagram

```
Player          Frontend         GameService      TokenService      GameProvider
  |                 |                 |                 |                 |
  |  Click Play     |                 |                 |                 |
  |---------------->|                 |                 |                 |
  |                 | GET /game/:id    |                 |                 |
  |                 |----------------->|                 |                 |
  |                 |                 | generateToken() |                 |
  |                 |                 |----------------->|                 |
  |                 |                 |<----------------|                 |
  |                 |                 | getProviderAdapter()              |
  |                 |                 | .authenticate()                   |
  |                 |                 |---------------------------------->|
  |                 |                 |<----------------------------------|
  |                 | gameUrl + token |                 |                 |
  |                 |<-----------------|                 |                 |
  |<----------------|                 |                 |                 |
  | Open iframe/redirect with token   |                 |                 |
  |--------------------------------------------->|                      |
  |                 |                 |                 | Validate token  |
  |                 |                 |                 |<----------------|
  |                 |                 |                 | Game session active
  |                 |                 |                 |                 |
  | Player plays game                 |                 |                 |
  |                                   |                 |                 |
  |                 |     Bet callback |                 |                 |
  |                 |<-----------------------------------                 |
  |                 | Verify signature, exec bet       |                 |
  |                 | Debit wallet, return balance    |                 |
  |                 |----------------------------------->|                 |
  |                 |                 |                 | Balance updated |
  |                 |                 |                 |                 |
  |                 |     Win callback |                 |                 |
  |                 |<-----------------------------------                 |
  |                 | Verify signature, exec settle   |                 |
  |                 | Credit wallet, return balance   |                 |
  |                 |----------------------------------->|                 |
```

### Implementation Flow

1. **Player initiates game launch**
   - Frontend calls `/api/games/{gameCode}/launch`
   - Includes tenantId, playerId, currencyCode

2. **GameService authenticates player**
   - Verify player is authenticated
   - Check player status (active, not self-excluded)
   - Check balance >= minimum bet

3. **TokenService generates session token**
   - Create unique session_id
   - Build token payload (playerId, gameCode, sessionId, tenantId, timestamp)
   - Sign with HMAC-SHA256
   - Store in t_game_session with status='ACTIVE'

4. **GameService calls GP adapter**
   - Call adapter.authenticate(playerId, tenantId)
   - Receive game launch URL from provider

5. **Combine token with game URL**
   - Append token as query parameter or header
   - Return full game URL to frontend

6. **Frontend opens game in iframe or redirect**
   - Game client receives token
   - Token validated on first load
   - Game session becomes active

## 4.6 回調處理

### Unified Callback Handler

All game provider callbacks flow through single handler for consistency and safety:

```java
@RestController
@RequestMapping("/api/callbacks/game")
public class GameCallbackController {

    @PostMapping("/bet")
    public ResponseEntity<CallbackResponse> handleBetCallback(
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Provider-Code") String providerCode,
            @RequestBody BetCallbackRequest request) {

        return processCallback(signature, timestamp, providerCode, request,
            CallbackType.BET);
    }

    @PostMapping("/settle")
    public ResponseEntity<CallbackResponse> handleSettleCallback(
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Provider-Code") String providerCode,
            @RequestBody SettleCallbackRequest request) {

        return processCallback(signature, timestamp, providerCode, request,
            CallbackType.SETTLE);
    }

    @PostMapping("/rollback")
    public ResponseEntity<CallbackResponse> handleRollbackCallback(
            @RequestHeader("X-Signature") String signature,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Provider-Code") String providerCode,
            @RequestBody RollbackCallbackRequest request) {

        return processCallback(signature, timestamp, providerCode, request,
            CallbackType.ROLLBACK);
    }
}
```

### Callback Processing Pipeline

```
1. Signature Verification
   ├─ Verify X-Signature using X-Timestamp + request_body
   ├─ Verify IP is in provider whitelist
   └─ Reject if invalid: 401 Unauthorized

1.5 Token Validation (STRICT — aligned with Ch2 §2.3.6)
   ├─ For BET/SETTLE/ROLLBACK: signatureService.verifyStrict(form)
   │   ├─ Check HMAC-SHA256 signature
   │   ├─ Check timestamp within TOKEN_TTL_MS (5 min)
   │   └─ Reject if expired: {"error":"TOKEN_EXPIRED","code":"TOKEN_EXPIRED"}
   ├─ For GET_BALANCE: signatureService.verifyLenient(form)
   │   └─ Check HMAC-SHA256 signature only (no expiry check)
   └─ Expired SETTLE → Insert t_resettlement record, return TOKEN_EXPIRED
       └─ GP or CS workflow triggers resettlement via Ch2 resettlement API

2. Idempotency Check (ADR-015)
   ├─ Query idempotency_key_cache
   ├─ If exists: return cached response
   └─ Otherwise: continue to processing

3. Round State Validation
   ├─ Fetch game round from database
   ├─ Verify round status allows this operation
   ├─ Check amount against expected values
   └─ Reject if invalid state: 400 Bad Request

4. Wallet Operation
   ├─ Call WalletManager.debit() for BET
   ├─ Call WalletManager.credit() for SETTLE
   ├─ Call WalletManager.rollback() for ROLLBACK
   ├─ Retry with exponential backoff on transient failure
   └─ Return new balance

5. Database State Update
   ├─ Update t_game_round with final status
   ├─ Update settled_at timestamp
   └─ Store in idempotency cache

6. Event Publishing
   ├─ Publish GameBetEvent to Kafka topic
   ├─ Publish GameSettleEvent to Kafka topic
   └─ Allows downstream analytics/reporting

7. Response Transmission
   ├─ Return CallbackResponse with balance
   └─ 200 OK if successful
```

### Error Handling

| Scenario | HTTP Status | Action | Response |
|----------|-------------|--------|----------|
| Invalid signature | 401 | Reject immediately | `{"error": "Invalid signature"}` |
| Timestamp out of tolerance | 401 | Reject immediately | `{"error": "Timestamp expired"}` |
| Token expired (money op) | 401 | Insert t_resettlement record | `{"error": "TOKEN_EXPIRED", "code": "TOKEN_EXPIRED", "resettlementId": "..."}` |
| Idempotent duplicate | 200 | Return cached response | Cached response data |
| Round not found | 404 | Log and reject | `{"error": "Round not found"}` |
| Invalid round state | 400 | Log and reject | `{"error": "Invalid round state"}` |
| Insufficient balance | 400 | Decline bet, no wallet update | `{"error": "Insufficient balance", "code": "INSUFFICIENT_FUNDS"}` |
| Wallet service unavailable | 503 | Retry with backoff | `{"error": "Service temporarily unavailable"}` |
| Round state violation | 400 | Reject with error | `{"error": "Cannot settle open round"}` |

### Callback Response Format

```json
{
  "status": "success",
  "code": 0,
  "message": "OK",
  "data": {
    "playerId": 12345,
    "balance": "1500.50",
    "balanceAfter": "1450.50",
    "transactionId": "txn_abc123def456",
    "roundId": "round_xyz789",
    "timestamp": 1711270800000
  }
}
```

## 4.7 RTP 監控

### Real-Time RTP Calculation

Flink streaming job processes game round events:

```sql
SELECT game_code,
       provider_code,
       ROUND(SUM(total_win) / NULLIF(SUM(total_bet), 0) * 100, 2) AS actual_rtp,
       COUNT(*) AS round_count,
       SUM(total_bet) AS total_wagered,
       SUM(total_win) AS total_paid,
       TUMBLE_START(event_time, INTERVAL '1' HOUR) AS window_start,
       TUMBLE_END(event_time, INTERVAL '1' HOUR) AS window_end
FROM game_round_events
WHERE status IN ('CLOSED', 'SETTLED')
GROUP BY game_code, provider_code, TUMBLE(event_time, INTERVAL '1' HOUR)
HAVING COUNT(*) >= 10
```

### Alert Thresholds and Actions

| Condition | Threshold | Action | Duration |
|-----------|-----------|--------|----------|
| Warning | actual_rtp > 120% of theoretical | Alert to ops team | 1-hour window |
| Critical | actual_rtp > 200% of theoretical | Immediate alert + escalation | Consecutive 2 windows |
| High Volume Anomaly | CRITICAL + volume > $10,000 | Trigger circuit breaker | Until manual review |

### Circuit Breaker Pattern

```java
public class GameCircuitBreaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

    /**
     * Monitor RTP and auto-suspend games with anomalies
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void monitorRTP() {
        List<RTrPAlert> criticalAlerts = getRTPAlertsAboveThreshold();

        for (RTpAlert alert : criticalAlerts) {
            if (alert.getActualRtp() > 200 &&
                alert.getTotalWagered() > 10000 &&
                alert.isConsecutiveWindow()) {

                // Auto-suspend game
                gameService.suspendGame(alert.getGameCode());

                // Notify compliance team
                notificationService.sendAlert(
                    "Critical RTP anomaly: " + alert.getGameCode(),
                    AlertLevel.CRITICAL,
                    alert
                );

                // Log for investigation
                auditLog.recordGameSuspension(
                    alert.getGameCode(),
                    "Automatic RTP circuit breaker"
                );
            }
        }
    }
}
```

### Metrics Storage

RTP metrics stored in time-series database (InfluxDB/Prometheus):
- Bucketed by game_code, provider_code
- 1-hour granularity
- Retention: 90 days detailed, 2 years aggregated
- Used for dashboards, compliance reporting, player fairness verification

## 4.8 遊戲大廳服務

### Game Catalog Management

```java
@RestController
@RequestMapping("/api/games")
public class GameLobbyController {

    /**
     * List all games with pagination
     */
    @GetMapping
    public Page<GameDTO> listGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        GameFilter filter = GameFilter.builder()
            .category(category)
            .searchTerm(search)
            .status(GameStatus.ACTIVE)
            .build();

        return gameService.searchGames(filter, PageRequest.of(page, size));
    }

    /**
     * Get personalized game recommendations
     */
    @GetMapping("/recommendations")
    public List<GameDTO> getRecommendations(
            @RequestParam int limit) {

        // Combine player preference + popular games
        List<GameDTO> playerPref = getPlayerPreferredGames(limit / 2);
        List<GameDTO> popular = getPopularGames(limit / 2);

        return Stream.concat(playerPref.stream(), popular.stream())
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

### Categories

Available game categories:
- **SLOT**: Slot machines and video slots
- **LIVE**: Live dealer games (blackjack, roulette, baccarat)
- **TABLE**: Table games (poker, dice games)
- **SPORTS**: Sports betting and prediction
- **FISHING**: Fish hunting/shooting games
- **LOTTERY**: Lottery-style games
- **ESPORTS**: Esports competitions and betting

### Caching Strategy

```java
@Service
public class GameCacheService {

    @Cacheable(value = "games", key = "#category")
    public List<Game> getGamesByCategory(String category) {
        // Cached in Redis
        // TTL: 1 hour
        return gameRepository.findByCategory(category);
    }

    @CachePut(value = "games:metadata", key = "#gameCode")
    public GameMetadata refreshMetadata(String gameCode) {
        // Refresh metadata from provider
        return gameService.fetchMetadata(gameCode);
    }
}
```

### Search Implementation

Elasticsearch for full-text search:
- Indexed fields: game_name, provider_name, game_type, description
- Query: game name/provider/type fuzzy matching
- Facets: Category, Provider, RTP range
- Sorting: Relevance, Popularity, RTP, Date added

### Pagination & Infinite Scroll

```javascript
// Frontend infinite scroll implementation
let page = 0;
const pageSize = 20;

window.addEventListener('scroll', () => {
    if (isNearBottom()) {
        fetchGames(page++, pageSize);
    }
});

async function fetchGames(pageNum, size) {
    const response = await fetch(
        `/api/games?page=${pageNum}&size=${size}&category=${selectedCategory}`
    );
    const games = await response.json();
    appendToDOM(games);
}
```

## 4.9 GP 健康監控

### Health Check Configuration

```java
@Service
public class GPHealthMonitorService {

    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds
    private static final int FAILURE_THRESHOLD = 3;
    private static final int SUCCESS_THRESHOLD = 3;

    /**
     * Perform periodic health checks on all providers
     */
    @Scheduled(fixedDelay = HEALTH_CHECK_INTERVAL_MS)
    public void checkAllProvidersHealth() {
        List<GameProvider> providers = gameProviderRepository.findAll();

        for (GameProvider provider : providers) {
            HealthCheckResult result = performHealthCheck(provider);
            handleHealthCheckResult(provider, result);
        }
    }

    /**
     * Individual health check for a provider
     */
    private HealthCheckResult performHealthCheck(GameProvider provider) {
        long startTime = System.currentTimeMillis();

        try {
            GameProviderAdapter adapter = adapterFactory.getAdapter(
                provider.getProviderCode()
            );

            // Test connectivity
            adapter.authenticate(TEST_PLAYER_ID, TEST_TENANT_ID);

            long responseTime = System.currentTimeMillis() - startTime;

            return HealthCheckResult.success(responseTime);

        } catch (Exception e) {
            return HealthCheckResult.failure(e.getMessage());
        }
    }

    /**
     * Handle health check result and take action if needed
     */
    private void handleHealthCheckResult(GameProvider provider,
                                        HealthCheckResult result) {
        String providerCode = provider.getProviderCode();

        if (result.isSuccess()) {
            incrementSuccessCount(providerCode);
            if (getSuccessCount(providerCode) >= SUCCESS_THRESHOLD) {
                enableProvider(provider);
                resetCounters(providerCode);
            }
        } else {
            incrementFailureCount(providerCode);
            if (getFailureCount(providerCode) >= FAILURE_THRESHOLD) {
                disableProvider(provider);
                notifyOpsTeam(provider, "Provider disabled after health check failures");
            }
        }
    }
}
```

### Health Metrics

Tracked for each game provider:
- **Availability %**: Percentage of successful health checks (5-minute window)
- **P99 Response Time**: 99th percentile response latency in milliseconds
- **Error Rate %**: Percentage of failed requests
- **Uptime**: Total cumulative uptime
- **Last Health Check**: Timestamp of most recent check

### Auto-disable/Re-enable Logic

**Auto-disable** (ON)
- Trigger: 3 consecutive failed health checks
- Action: Set provider status to SUSPENDED
- Prevent: No new games launched from this provider
- Existing: In-progress game sessions continue

**Auto-re-enable** (ON)
- Trigger: 3 consecutive successful health checks AND availability > 95%
- Action: Set provider status back to ACTIVE
- Games: Can be launched again

**Manual Escalation**
- Team notified after auto-disable
- Manual review before re-enabling
- Can force disable/enable regardless of metrics

### Dashboard Metrics

```json
{
  "providerCode": "pragmatic",
  "name": "Pragmatic Play",
  "status": "ACTIVE",
  "availability": 99.8,
  "p99ResponseTime": 245,
  "errorRate": 0.2,
  "consecutiveSuccesses": 3,
  "lastHealthCheck": "2026-03-24T10:45:30Z",
  "mttr": 300,
  "lastIncident": "2026-03-23T14:20:00Z"
}
```

## 4.10 Demo/Free Play

### Demo Mode Implementation

```java
@Service
public class DemoGameService {

    /**
     * Launch game in demo mode
     */
    public GameLaunchResponse launchDemoGame(String gameCode,
                                            String tenantId) {
        Game game = gameService.getGame(gameCode);

        // Generate demo token with special prefix
        String demoToken = generateDemoToken(gameCode);

        // Get provider adapter
        GameProviderAdapter adapter = adapterFactory.getAdapter(
            game.getProviderCode()
        );

        // Provider must support demo mode
        if (!adapter.supportsDemoMode()) {
            throw new GameProviderException("Demo mode not supported");
        }

        // Get game URL for demo
        String gameUrl = adapter.getDemoGameUrl(gameCode, demoToken);

        return GameLaunchResponse.builder()
            .gameUrl(gameUrl)
            .token(demoToken)
            .mode("DEMO")
            .balanceOverride("1000.00") // Virtual balance
            .build();
    }

    /**
     * Generate demo token with recognizable prefix
     */
    private String generateDemoToken(String gameCode) {
        String payload = String.format("DEMO_%s_%d", gameCode,
            System.currentTimeMillis());
        return TokenUtil.sign(payload, DEMO_TOKEN_SECRET);
    }
}
```

### Key Characteristics

- **No wallet interaction**: Demo play does not debit/credit real wallet
- **Demo token prefix**: Begins with "DEMO_" for easy identification
- **Provider support**: Game provider must explicitly support demo mode
- **Virtual balance**: Displayed balance is virtual and not persisted
- **Game state reset**: Each session starts fresh with default virtual balance

### Rate Limiting

```java
@Component
public class DemoPlayRateLimiter {

    /**
     * Rate limit non-authenticated demo play
     */
    @Before("execution(* DemoGameService.launchDemoGame(..))")
    public void rateLimitDemoPlay(JoinPoint joinPoint) {
        String clientIP = RequestContextHolder.getClientIP();

        int requestCount = demoPlayCache.get(clientIP, 0);

        if (requestCount >= DEMO_PLAY_LIMIT_PER_HOUR) {
            throw new RateLimitException(
                "Demo play limit exceeded. Max 50 launches per hour."
            );
        }

        demoPlayCache.put(clientIP, requestCount + 1);
    }
}
```

Rate limits for non-authenticated demo play:
- **Limit**: 50 game launches per hour per IP address
- **Enforcement**: IP-based tracking
- **Reset**: Hourly rolling window
- **Authenticated players**: No limit on demo play

## 4.11 GP 違約自動化處理

> **業務規則來源**: Ch4 需求 §4.8 GP 違約處理

### t_gp_violation

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-increment |
| provider_code | VARCHAR(50) | FK → t_game_provider |
| violation_level | ENUM('WARNING', 'THROTTLE', 'SUSPEND', 'DELIST') | Current escalation level |
| trigger_type | ENUM('RTP_DEVIATION', 'API_AVAILABILITY', 'SETTLEMENT_DELAY', 'SECURITY_INCIDENT', 'COMPLIANCE_VIOLATION') | What triggered the violation |
| trigger_detail | JSONB | Metrics snapshot: `{"rtp_deviation": 1.5, "period": "2026-03"}` |
| previous_level | VARCHAR(20) | Previous violation level (NULL for first) |
| escalated_at | TIMESTAMP | When this level was applied |
| review_deadline | TIMESTAMP | Deadline for GP to remediate |
| resolved_at | TIMESTAMP | When violation was resolved (NULL if active) |
| resolved_by | VARCHAR(100) | Admin who resolved |
| created_at | TIMESTAMP | Record creation |

```sql
CREATE INDEX idx_gp_violation_provider ON t_gp_violation(provider_code, violation_level)
    WHERE resolved_at IS NULL;
```

### GPViolationService

```java
@Service
public class GPViolationService {

    /**
     * Scheduled monthly evaluation of GP compliance metrics.
     * Runs on 1st of each month at 06:00.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void evaluateMonthlyCompliance() {
        List<GameProvider> activeProviders = providerRepository.findByStatus(ACTIVE);

        for (GameProvider provider : activeProviders) {
            GPMetricsSnapshot metrics = collectMetrics(provider.getProviderCode());
            evaluateAndEscalate(provider, metrics);
        }
    }

    /**
     * Collect GP performance metrics for evaluation period.
     */
    private GPMetricsSnapshot collectMetrics(String providerCode) {
        // 30-day sliding window
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        return GPMetricsSnapshot.builder()
            .rtpDeviation(rtpService.getAverageDeviation(providerCode, start, end))
            .apiAvailability(healthService.getAvailability(providerCode, start, end))
            .avgSettlementDelay(roundService.getAvgSettlementDelay(providerCode, start, end))
            .maxSettlementDelay(roundService.getMaxSettlementDelay(providerCode, start, end))
            .build();
    }

    /**
     * 4-level escalation logic (aligned with requirements §4.8):
     *
     * Level 1 — WARNING:
     *   Trigger: RTP deviation > ±1% OR API availability < 99%
     *   Action: Written notice + enhanced monitoring
     *   Deadline: 7 days to remediate
     *
     * Level 2 — THROTTLE:
     *   Trigger: 2 consecutive months unresolved WARNING, OR settlement delay > 24h
     *   Action: Reduce lobby sort weight (weight *= 0.3)
     *   Deadline: 14 days observation
     *
     * Level 3 — SUSPEND:
     *   Trigger: 3 consecutive months unresolved, OR suspected RTP fraud
     *   Action: Stop new bets; existing rounds continue settlement
     *   Effect: Immediate
     *
     * Level 4 — DELIST:
     *   Trigger: Security incident OR compliance violation
     *   Action: Remove all games, notify affected players, settle pending rounds
     *   Effect: Immediate
     */
    public void evaluateAndEscalate(GameProvider provider, GPMetricsSnapshot metrics) {
        String code = provider.getProviderCode();
        ViolationLevel currentLevel = getCurrentViolationLevel(code);

        // Level 4: Immediate delist (checked by separate security event handler)
        // Level 3: Suspend check
        if (currentLevel == THROTTLE && getConsecutiveViolationMonths(code) >= 3) {
            escalateTo(provider, SUSPEND, "3 consecutive months unresolved", metrics);
            return;
        }

        // Level 2: Throttle check
        if (currentLevel == WARNING && getConsecutiveViolationMonths(code) >= 2) {
            escalateTo(provider, THROTTLE, "2 consecutive months unresolved", metrics);
            return;
        }
        if (metrics.getMaxSettlementDelay().toHours() > 24) {
            escalateTo(provider, THROTTLE, "Settlement delay > 24h", metrics);
            return;
        }

        // Level 1: Warning check
        if (Math.abs(metrics.getRtpDeviation()) > 1.0) {
            escalateTo(provider, WARNING, "RTP deviation > ±1%", metrics);
            return;
        }
        if (metrics.getApiAvailability() < 99.0) {
            escalateTo(provider, WARNING, "API availability < 99%", metrics);
            return;
        }

        // All clear — resolve existing violations
        if (currentLevel != null) {
            resolveViolation(code, "Metrics within acceptable range");
        }
    }

    /**
     * Execute escalation side effects.
     */
    private void escalateTo(GameProvider provider, ViolationLevel level,
                            String reason, GPMetricsSnapshot metrics) {
        String code = provider.getProviderCode();

        // Persist violation record
        GPViolation violation = GPViolation.builder()
            .providerCode(code)
            .violationLevel(level)
            .triggerDetail(JsonUtil.toJson(metrics))
            .previousLevel(getCurrentViolationLevel(code))
            .reviewDeadline(calculateDeadline(level))
            .build();
        violationRepository.save(violation);

        // Execute level-specific actions
        switch (level) {
            case WARNING:
                notificationService.notifyGP(code, "Compliance Warning", reason);
                monitoringService.enableEnhancedMonitoring(code);
                break;

            case THROTTLE:
                gameLobbyService.reduceWeight(code, 0.3); // weight *= 0.3
                notificationService.notifyGP(code, "Service Throttled", reason);
                notificationService.notifyOps("GP throttled: " + code, reason);
                break;

            case SUSPEND:
                provider.setStatus(ProviderStatus.SUSPENDED);
                providerRepository.save(provider);
                notificationService.notifyAffectedPlayers(code,
                    "Game provider temporarily suspended");
                orphanRoundService.scheduleSettlement(code); // settle existing rounds
                break;

            case DELIST:
                provider.setStatus(ProviderStatus.INACTIVE);
                providerRepository.save(provider);
                gameLobbyService.removeAllGames(code);
                notificationService.notifyAffectedPlayers(code,
                    "Games from this provider have been removed");
                orphanRoundService.forceSettleAll(code);
                freeSpinService.refundUnusedFreeSpins(code);
                break;
        }

        auditLog.record("GP_VIOLATION_ESCALATION", code, level, reason);
    }

    /**
     * Deadline calculation per business rules.
     */
    private Instant calculateDeadline(ViolationLevel level) {
        return switch (level) {
            case WARNING -> Instant.now().plus(7, ChronoUnit.DAYS);
            case THROTTLE -> Instant.now().plus(14, ChronoUnit.DAYS);
            case SUSPEND, DELIST -> Instant.now(); // immediate
        };
    }
}
```

### Security Event Handler (Immediate Delist)

```java
@Component
public class GPSecurityEventHandler {

    @KafkaListener(topics = "security.incident")
    public void handleSecurityIncident(SecurityIncidentEvent event) {
        if (event.getTargetType() == TargetType.GAME_PROVIDER) {
            gpViolationService.escalateTo(
                providerRepository.findByCode(event.getTargetCode()),
                ViolationLevel.DELIST,
                "Security incident: " + event.getDescription(),
                null
            );
        }
    }
}
```

---

## 4.12 Partial Cashout (部分結算)

> **業務規則來源**: Ch4 需求 §4.8 部分結算

### 適用範圍

- **僅適用於體育投注** (game_type = 'SPORTS')
- 紅利資金投注**不允許** Partial Cashout (須完整結算)
- 玩家可在賽事進行中選擇提前結算部分投注金額

### Settlement Amount 計算公式

```
settlement_amount = bet_amount × cashout_ratio × current_odds / original_odds
```

其中:
- `bet_amount`: 原始投注金額
- `cashout_ratio`: 玩家選擇結算的比例 (0.01 ~ 1.00)
- `current_odds`: 當前即時賠率 (由 Odds Feed 提供)
- `original_odds`: 下注時鎖定的賠率

**Margin 扣除** (平台保護):
```
final_settlement = settlement_amount × (1 - cashout_margin)
```
- `cashout_margin`: 可配置，預設 5% (即玩家拿到 95%)

### 流水計算規則

| 部分 | Valid Bet 計入時機 |
|------|-------------------|
| 已結算部分 | `bet_amount × cashout_ratio` → Partial Cashout 完成時立即計入 |
| 未結算部分 | `bet_amount × (1 - cashout_ratio)` → 賽事最終結算時計入 |

### API Endpoint

```java
@RestController
@RequestMapping("/api/v1/games/sports")
public class PartialCashoutController {

    /**
     * Get available cashout offer for a bet.
     * Returns current settlement amount based on live odds.
     */
    @GetMapping("/bets/{roundId}/cashout-offer")
    public ResponseEntity<CashoutOfferResponse> getCashoutOffer(
            @PathVariable String roundId,
            @RequestParam BigDecimal cashoutRatio) {

        // Validate ratio range
        if (cashoutRatio.compareTo(BigDecimal.ZERO) <= 0 ||
            cashoutRatio.compareTo(BigDecimal.ONE) > 0) {
            return ResponseEntity.badRequest().build();
        }

        GameRound round = roundService.findByRoundId(roundId);

        // Validation checks
        validateCashoutEligibility(round);

        // Calculate offer (valid for 10 seconds)
        BigDecimal currentOdds = oddsFeedService.getCurrentOdds(
            round.getEventId(), round.getMarketId());
        BigDecimal settlementAmount = calculateSettlement(
            round.getTotalBet(), cashoutRatio, currentOdds, round.getOriginalOdds());

        String offerId = UUID.randomUUID().toString();
        // Cache offer with 10s TTL
        redisTemplate.opsForValue().set(
            "cashout:offer:" + offerId, settlementAmount, Duration.ofSeconds(10));

        return ResponseEntity.ok(CashoutOfferResponse.builder()
            .offerId(offerId)
            .roundId(roundId)
            .cashoutRatio(cashoutRatio)
            .originalBet(round.getTotalBet())
            .currentOdds(currentOdds)
            .originalOdds(round.getOriginalOdds())
            .settlementAmount(settlementAmount)
            .cashoutMargin(cashoutMarginConfig.getMargin())
            .expiresInSeconds(10)
            .build());
    }

    /**
     * Execute partial cashout with a previously obtained offer.
     */
    @PostMapping("/bets/{roundId}/cashout")
    public ResponseEntity<CashoutResult> executePartialCashout(
            @PathVariable String roundId,
            @RequestBody PartialCashoutRequest request) {

        // Verify offer still valid (10s TTL)
        BigDecimal lockedAmount = redisTemplate.opsForValue().get(
            "cashout:offer:" + request.getOfferId());
        if (lockedAmount == null) {
            return ResponseEntity.status(410).body(
                CashoutResult.expired("Cashout offer expired, please request new offer"));
        }

        GameRound round = roundService.findByRoundId(roundId);
        validateCashoutEligibility(round);

        // Execute within transaction
        CashoutResult result = cashoutService.executeCashout(round, request, lockedAmount);

        return ResponseEntity.ok(result);
    }

    private void validateCashoutEligibility(GameRound round) {
        // Must be sports bet
        if (round.getGameType() != GameType.SPORTS) {
            throw new CashoutNotAllowedException("Partial cashout only for sports bets");
        }
        // Must be open round
        if (round.getStatus() != RoundStatus.OPEN) {
            throw new CashoutNotAllowedException("Round not open for cashout");
        }
        // Bonus funds not allowed
        if (round.getFundingSource() == FundingSource.BONUS) {
            throw new CashoutNotAllowedException("Bonus bets cannot be partially cashed out");
        }
    }

    private BigDecimal calculateSettlement(BigDecimal betAmount, BigDecimal ratio,
                                           BigDecimal currentOdds, BigDecimal originalOdds) {
        // settlement = bet × ratio × currentOdds / originalOdds × (1 - margin)
        BigDecimal gross = betAmount
            .multiply(ratio)
            .multiply(currentOdds)
            .divide(originalOdds, 4, RoundingMode.HALF_UP);
        BigDecimal margin = cashoutMarginConfig.getMargin(); // default 0.05
        return gross.multiply(BigDecimal.ONE.subtract(margin))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
```

### CashoutService 交易處理

```java
@Service
public class CashoutService {

    @Transactional
    public CashoutResult executeCashout(GameRound round, PartialCashoutRequest request,
                                        BigDecimal lockedAmount) {
        BigDecimal cashoutRatio = request.getCashoutRatio();

        // 1. Split the round
        BigDecimal cashedOutBet = round.getTotalBet().multiply(cashoutRatio);
        BigDecimal remainingBet = round.getTotalBet().subtract(cashedOutBet);

        // 2. Credit settlement to player wallet
        walletService.credit(WalletCreditRequest.builder()
            .playerId(round.getPlayerId())
            .amount(lockedAmount)
            .transactionType(TransactionType.PARTIAL_CASHOUT)
            .referenceId("PCO_" + round.getRoundId())
            .build());

        // 3. Record partial cashout in game round
        round.setPartialCashoutAmount(lockedAmount);
        round.setPartialCashoutRatio(cashoutRatio);
        round.setRemainingBet(remainingBet);
        round.setStatus(RoundStatus.PARTIALLY_SETTLED);
        roundRepository.save(round);

        // 4. Record valid bet for cashed-out portion
        validBetService.record(round.getPlayerId(), cashedOutBet, round.getRoundId());

        // 5. Publish event
        eventPublisher.publish(new PartialCashoutEvent(
            round.getRoundId(), round.getPlayerId(), lockedAmount,
            cashoutRatio, remainingBet));

        // 6. Delete used offer
        redisTemplate.delete("cashout:offer:" + request.getOfferId());

        return CashoutResult.success(lockedAmount, remainingBet);
    }
}
```

### t_game_round 欄位擴充

```sql
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS original_odds DECIMAL(10,4);
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS current_odds_at_cashout DECIMAL(10,4);
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS partial_cashout_ratio DECIMAL(5,4);
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS partial_cashout_amount DECIMAL(19,2);
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS remaining_bet DECIMAL(19,2);
ALTER TABLE t_game_round ADD COLUMN IF NOT EXISTS funding_source ENUM('CASH', 'BONUS') DEFAULT 'CASH';

-- New status value for partially settled rounds
-- Status ENUM updated: ('OPEN','CLOSED','TIMEOUT','CANCELLED','PENDING_REVIEW','ADJUSTED','PARTIALLY_SETTLED')
```

---

## 4.13 跨模組邊界情境 (Cross-Module Boundary Scenarios)

The following boundary scenarios describe interactions between Game Integration and other modules when edge cases occur. For complete boundary scenario specifications, refer to [Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md).

| BS ID | Scenario | Decision |
|-------|----------|----------|
| BS-01 | Token 過期 × 流水進度 | Active wagering proceeds uninterrupted; expired token → new token issued; win settles to CASH wallet via Resettlement |
| BS-03 | GP 維護 × 超時 | System waits for settlement with configurable timeout (default 5min); unsettled rounds auto-rollback after timeout |
| BS-07 | KYC 升級 × 進行中交易 | Don't interrupt active game round; block new bets after round ends until KYC verification complete |

**Cross-references**: [§BS-01](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-01-token-過期--流水進度), [§BS-03](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-03-gp-維護--超時), [§BS-07](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-07-kyc-升級--進行中交易)

---

## 4.14 對應業務文檔

Link to requirements specification: `requirements/04_Game_Integration_遊戲整合.md`

**v2.1 新增/變更清單**:
- §4.4 Token 驗證: 修正為 STRICT 過期政策 (移除 grace period)
- §4.6 回調處理 Pipeline: 新增 Step 1.5 Token 嚴格驗證 + TOKEN_EXPIRED 錯誤處理
- §4.11 GP 違約自動化處理: 新增 4 級升級服務 + t_gp_violation schema
- §4.12 Partial Cashout: 新增計算公式、API 端點、CashoutService、t_game_round 欄位擴充

---

**Document Version**: 2.1
**Last Updated**: 2026-03-24
**Module Owner**: Game Integration Team
**Related Modules**: Chapter 3 (Wallet Integration), Chapter 5 (Reporting & Analytics)
