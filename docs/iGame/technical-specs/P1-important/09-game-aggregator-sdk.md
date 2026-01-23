# P1-09: Game Aggregator SDK

**Version**: 1.0.0
**Status**: Draft
**Last Updated**: 2026-01-23
**Owner**: iGaming Platform Team
**Related Documents**: [P0-03 (Wallet)](../P0-critical/03-seamless-wallet-implementation.md), [P1-05 (Saga)](05-distributed-transaction-patterns.md), [P1-07 (Multi-Tenant)](07-multi-tenant-isolation.md), [P1-11 (VIP)](11-vip-system-design.md)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Architecture Overview](#2-architecture-overview)
3. [Provider Adapter Pattern](#3-provider-adapter-pattern)
4. [Game Launch Flow](#4-game-launch-flow)
5. [Game Catalog Synchronization](#5-game-catalog-synchronization)
6. [RTP Verification & Compliance](#6-rtp-verification--compliance)
7. [Database Schema](#7-database-schema)
8. [Implementation Details (SmartAdmin)](#8-implementation-details-smartadmin)
9. [Integration Points](#9-integration-points)
10. [Testing Strategy](#10-testing-strategy)
11. [Operations & Monitoring](#11-operations--monitoring)
12. [Appendices](#12-appendices)

---

## 1. Background & Strategic Context

### 1.1 Strategic Importance

**From igame_str.md (First Principles)**:
- **Code Leverage (代码杠杆)**: 1 adapter implementation → 20+ game providers → infinite games
- **Zero Marginal Cost**: Add new merchant = 0 integration effort (reuse existing adapters)
- **Velocity (速度)**: Players launch games in <2 seconds (vs 10+ seconds competitor platforms)

**Business Metrics**:
- **Target Providers**: 20+ (Evolution, Pragmatic Play, NetEnt, Microgaming, Playtech, etc.)
- **Total Games**: 5,000+ (slots, live casino, table games)
- **Game Launch SLA**: <2 seconds p95 latency
- **Uptime**: 99.9% (providers aggregate to eliminate single point of failure)
- **Revenue Share**: Platform takes 5-15% commission on GGR (Gross Gaming Revenue)

### 1.2 Technical Challenges

1. **Provider API Heterogeneity**:
   - Each provider has different API (REST, SOAP, custom protocols)
   - Different authentication (API key, OAuth, IP whitelist)
   - Different game launch methods (iframe URL, HTML5 embed, Flash)

2. **Wallet Integration**:
   - Seamless wallet (single balance) vs transfer wallet (separate provider balance)
   - Real-time balance sync (<200ms SLA from P0-03)
   - Currency conversion (provider supports USD but player uses EUR)

3. **Session Management**:
   - Game round tracking for regulatory compliance
   - Abandoned session recovery (player disconnects mid-game)
   - Concurrent session limits (prevent multi-accounting)

4. **Compliance**:
   - RTP (Return to Player) verification (MGA requires ≥92%)
   - Game round audit trail (7-year retention)
   - Responsible gaming (loss limits, session time limits)

### 1.3 Related Documents

- **P0-03 (Wallet)**: Seamless wallet enables instant game launches without fund transfers
- **P1-05 (Saga)**: Game session lifecycle managed via saga pattern
- **P1-07 (Multi-Tenant)**: Each merchant configures own provider integrations
- **P1-11 (VIP)**: VIP players get exclusive games and higher betting limits

---

## 2. Architecture Overview

### 2.1 System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     Game Aggregator SDK                          │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─► Provider Adapter Registry
         │   ├─ Evolution Gaming Adapter
         │   ├─ Pragmatic Play Adapter
         │   ├─ NetEnt Adapter
         │   └─ ... (20+ adapters)
         │
         ├─► Game Catalog Service
         │   ├─ Daily Sync from Providers (cron job)
         │   ├─ Game Metadata (RTP, volatility, max win)
         │   └─ Game Availability (per tenant, per jurisdiction)
         │
         ├─► Game Launch Service
         │   ├─ Session Creation (P1-05 Saga)
         │   ├─ Wallet Validation (P0-03 integration)
         │   ├─ URL Generation (provider-specific)
         │   └─ Free Play vs Real Money Mode
         │
         ├─► Game Round Processor
         │   ├─ Bet Placement (debit wallet)
         │   ├─ Win Payment (credit wallet)
         │   ├─ Rollback Handling (provider timeout)
         │   └─ Idempotency (P0-02 integration)
         │
         └─► RTP Verification Service
             ├─ Daily Aggregate Calculation (per game)
             ├─ Alert if RTP < 92% (regulatory threshold)
             └─ Monthly Report Generation (for MGA audit)
```

### 2.2 Adapter Pattern Architecture

**Design Pattern**: Strategy + Adapter (Gang of Four)

```java
// Abstract interface for all game providers
public interface GameProviderAdapter {
    String getProviderId();
    GameLaunchResult launchGame(GameLaunchRequest request);
    BalanceResponse getBalance(Long playerId);
    GameRoundResult placeBet(BetRequest request);
    GameRoundResult settleWin(WinRequest request);
    GameRoundResult rollback(RollbackRequest request);
    List<Game> syncGameCatalog();
}

// Concrete adapters for each provider
@Component("evolutionAdapter")
public class EvolutionGamingAdapter implements GameProviderAdapter { ... }

@Component("pragmaticAdapter")
public class PragmaticPlayAdapter implements GameProviderAdapter { ... }

@Component("netentAdapter")
public class NetEntAdapter implements GameProviderAdapter { ... }
```

**Benefit**: Add new provider = implement 1 interface (no changes to existing code)

### 2.3 Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| HTTP Client | Spring RestTemplate + WebClient | RestTemplate for sync, WebClient for async |
| Adapter Registry | Spring ApplicationContext | Auto-discovery of @Component adapters |
| Game Metadata | PostgreSQL + Redis cache | PostgreSQL for persistence, Redis for <100ms reads |
| Game Launch | JWT (signed token) | Secure, stateless game session tokens |
| Callback Authentication | HMAC-SHA256 signature | Verify callbacks from providers |

### 2.4 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Load Balancer                             │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─► Game Aggregator Service (3 instances)
         │   ├─ /api/game/launch
         │   ├─ /api/game/catalog
         │   └─ /callback/{provider}/bet (provider callbacks)
         │
         ├─► Redis Cluster (game metadata cache)
         │   └─ 10s TTL for game catalog
         │
         └─► PostgreSQL (game sessions, rounds)
             └─ Partitioned by month (game_rounds_2026_01)
```

---

## 3. Provider Adapter Pattern

### 3.1 Abstract Interface

```java
package net.lab1024.sa.base.module.support.game;

import java.math.BigDecimal;
import java.util.List;

/**
 * Abstract interface for all game providers
 * Each provider implements this interface with provider-specific logic
 */
public interface GameProviderAdapter {

    /**
     * Get provider unique identifier
     * @return Provider ID (e.g., "evolution", "pragmatic", "netent")
     */
    String getProviderId();

    /**
     * Launch game and return iframe URL or HTML5 embed code
     *
     * @param request Game launch parameters
     * @return Game launch result with URL
     */
    GameLaunchResult launchGame(GameLaunchRequest request);

    /**
     * Get player balance from provider (for transfer wallet only)
     *
     * @param playerId Player ID
     * @return Balance response
     */
    BalanceResponse getBalance(Long playerId);

    /**
     * Process bet placement callback from provider
     *
     * @param request Bet request from provider
     * @return Game round result (success/failure)
     */
    GameRoundResult placeBet(BetRequest request);

    /**
     * Process win settlement callback from provider
     *
     * @param request Win request from provider
     * @return Game round result (success/failure)
     */
    GameRoundResult settleWin(WinRequest request);

    /**
     * Rollback transaction (provider timeout or error)
     *
     * @param request Rollback request from provider
     * @return Game round result (success/failure)
     */
    GameRoundResult rollback(RollbackRequest request);

    /**
     * Synchronize game catalog from provider (daily cron job)
     *
     * @return List of games from provider
     */
    List<Game> syncGameCatalog();

    /**
     * Validate callback signature (HMAC-SHA256)
     *
     * @param payload Request body
     * @param signature Provider signature
     * @return True if valid
     */
    boolean validateCallbackSignature(String payload, String signature);
}
```

### 3.2 Evolution Gaming Adapter

```java
package net.lab1024.sa.admin.module.business.game.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.game.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component("evolutionAdapter")
@RequiredArgsConstructor
public class EvolutionGamingAdapter implements GameProviderAdapter {

    private final RestTemplate restTemplate;
    private final GameProviderConfigDao providerConfigDao;

    private static final String EVOLUTION_API_BASE = "https://api.evolutiongaming.com/v1";

    @Override
    public String getProviderId() {
        return "evolution";
    }

    @Override
    public GameLaunchResult launchGame(GameLaunchRequest request) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Get provider configuration
        GameProviderConfig config = providerConfigDao.selectOne(
            new LambdaQueryWrapper<GameProviderConfig>()
                .eq(GameProviderConfig::getTenantId, tenantId)
                .eq(GameProviderConfig::getProviderId, "evolution")
        );

        // 2. Generate JWT token for game session
        String sessionToken = generateSessionToken(request, config);

        // 3. Build Evolution Gaming iframe URL
        String gameUrl = String.format(
            "%s/game/launch?token=%s&gameId=%s&mode=%s&currency=%s&locale=%s",
            EVOLUTION_API_BASE,
            sessionToken,
            request.getGameId(),
            request.isRealMoney() ? "real" : "fun",
            request.getCurrency(),
            request.getLocale()
        );

        log.info("Evolution game launched: player={}, game={}, url={}",
            request.getPlayerId(), request.getGameId(), gameUrl);

        return GameLaunchResult.builder()
            .gameUrl(gameUrl)
            .sessionToken(sessionToken)
            .launchType(LaunchType.IFRAME)
            .build();
    }

    @Override
    public GameRoundResult placeBet(BetRequest request) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Validate idempotency (prevent duplicate bets)
        String idempotencyKey = request.getTransactionId();
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            log.warn("Duplicate bet detected: transactionId={}", idempotencyKey);
            return GameRoundResult.duplicate(idempotencyKey);
        }

        // 2. Debit player wallet (P0-03 integration)
        try {
            walletManager.debit(
                request.getPlayerId(),
                request.getBetAmount(),
                request.getCurrency(),
                "Game bet: " + request.getGameId(),
                idempotencyKey
            );

            // 3. Record game round
            GameRound round = new GameRound();
            round.setTenantId(tenantId);
            round.setPlayerId(request.getPlayerId());
            round.setProviderId("evolution");
            round.setGameId(request.getGameId());
            round.setRoundId(request.getRoundId());
            round.setTransactionId(request.getTransactionId());
            round.setBetAmount(request.getBetAmount());
            round.setCurrency(request.getCurrency());
            round.setRoundType(RoundType.BET);
            round.setStatus(RoundStatus.COMPLETED);

            gameRoundDao.insert(round);

            log.info("Evolution bet placed: player={}, game={}, amount={}, roundId={}",
                request.getPlayerId(), request.getGameId(), request.getBetAmount(), request.getRoundId());

            return GameRoundResult.success(request.getTransactionId());

        } catch (InsufficientBalanceException e) {
            log.warn("Insufficient balance: player={}, amount={}", request.getPlayerId(), request.getBetAmount());
            return GameRoundResult.error("INSUFFICIENT_BALANCE", "Insufficient balance");
        }
    }

    @Override
    public GameRoundResult settleWin(WinRequest request) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Validate idempotency
        String idempotencyKey = request.getTransactionId();
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            log.warn("Duplicate win detected: transactionId={}", idempotencyKey);
            return GameRoundResult.duplicate(idempotencyKey);
        }

        // 2. Credit player wallet (P0-03 integration)
        walletManager.credit(
            request.getPlayerId(),
            request.getWinAmount(),
            request.getCurrency(),
            "Game win: " + request.getGameId(),
            idempotencyKey
        );

        // 3. Record game round
        GameRound round = new GameRound();
        round.setTenantId(tenantId);
        round.setPlayerId(request.getPlayerId());
        round.setProviderId("evolution");
        round.setGameId(request.getGameId());
        round.setRoundId(request.getRoundId());
        round.setTransactionId(request.getTransactionId());
        round.setWinAmount(request.getWinAmount());
        round.setCurrency(request.getCurrency());
        round.setRoundType(RoundType.WIN);
        round.setStatus(RoundStatus.COMPLETED);

        gameRoundDao.insert(round);

        log.info("Evolution win settled: player={}, game={}, amount={}, roundId={}",
            request.getPlayerId(), request.getGameId(), request.getWinAmount(), request.getRoundId());

        return GameRoundResult.success(request.getTransactionId());
    }

    @Override
    public GameRoundResult rollback(RollbackRequest request) {
        // 1. Find original transaction
        GameRound originalRound = gameRoundDao.selectOne(
            new LambdaQueryWrapper<GameRound>()
                .eq(GameRound::getTransactionId, request.getOriginalTransactionId())
        );

        if (originalRound == null) {
            log.warn("Original transaction not found for rollback: transactionId={}",
                request.getOriginalTransactionId());
            return GameRoundResult.error("TRANSACTION_NOT_FOUND", "Original transaction not found");
        }

        // 2. Reverse wallet transaction
        if (RoundType.BET.equals(originalRound.getRoundType())) {
            // Rollback bet = credit wallet (refund)
            walletManager.credit(
                originalRound.getPlayerId(),
                originalRound.getBetAmount(),
                originalRound.getCurrency(),
                "Rollback bet: " + request.getOriginalTransactionId(),
                request.getTransactionId()
            );
        } else if (RoundType.WIN.equals(originalRound.getRoundType())) {
            // Rollback win = debit wallet
            walletManager.debit(
                originalRound.getPlayerId(),
                originalRound.getWinAmount(),
                originalRound.getCurrency(),
                "Rollback win: " + request.getOriginalTransactionId(),
                request.getTransactionId()
            );
        }

        // 3. Record rollback
        originalRound.setStatus(RoundStatus.ROLLED_BACK);
        gameRoundDao.updateById(originalRound);

        log.info("Evolution rollback completed: originalTxId={}, rollbackTxId={}",
            request.getOriginalTransactionId(), request.getTransactionId());

        return GameRoundResult.success(request.getTransactionId());
    }

    @Override
    public List<Game> syncGameCatalog() {
        // Call Evolution API to fetch game list
        String url = EVOLUTION_API_BASE + "/games?apiKey=" + getApiKey();

        EvolutionGameListResponse response = restTemplate.getForObject(url, EvolutionGameListResponse.class);

        return response.getGames().stream()
            .map(this::convertToGame)
            .collect(Collectors.toList());
    }

    @Override
    public boolean validateCallbackSignature(String payload, String signature) {
        try {
            String secretKey = getSecretKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Failed to validate Evolution callback signature", e);
            return false;
        }
    }

    private String generateSessionToken(GameLaunchRequest request, GameProviderConfig config) {
        // Generate JWT token with player info, game ID, timestamp
        return jwtService.createToken(
            Map.of(
                "playerId", request.getPlayerId(),
                "gameId", request.getGameId(),
                "currency", request.getCurrency(),
                "mode", request.isRealMoney() ? "real" : "fun",
                "tenantId", TenantContextHolder.getTenantId(),
                "timestamp", System.currentTimeMillis()
            ),
            config.getSecretKey(),
            3600  // 1 hour expiry
        );
    }

    private Game convertToGame(EvolutionGame evolutionGame) {
        Game game = new Game();
        game.setProviderId("evolution");
        game.setProviderGameId(evolutionGame.getId());
        game.setGameName(evolutionGame.getName());
        game.setGameType(evolutionGame.getType());
        game.setRtp(evolutionGame.getRtp());
        game.setThumbnailUrl(evolutionGame.getThumbnail());
        return game;
    }
}
```

### 3.3 Pragmatic Play Adapter

```java
package net.lab1024.sa.admin.module.business.game.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.game.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component("pragmaticAdapter")
@RequiredArgsConstructor
public class PragmaticPlayAdapter implements GameProviderAdapter {

    private final RestTemplate restTemplate;
    private final GameProviderConfigDao providerConfigDao;

    private static final String PRAGMATIC_API_BASE = "https://api.pragmaticplay.net/v1";

    @Override
    public String getProviderId() {
        return "pragmatic";
    }

    @Override
    public GameLaunchResult launchGame(GameLaunchRequest request) {
        String tenantId = TenantContextHolder.getTenantId();

        GameProviderConfig config = providerConfigDao.selectOne(
            new LambdaQueryWrapper<GameProviderConfig>()
                .eq(GameProviderConfig::getTenantId, tenantId)
                .eq(GameProviderConfig::getProviderId, "pragmatic")
        );

        // Pragmatic Play uses different URL structure
        String gameUrl = String.format(
            "%s/launcher?casinoId=%s&playerId=%s&gameSymbol=%s&mode=%s&currency=%s&lang=%s&lobbyUrl=%s",
            PRAGMATIC_API_BASE,
            config.getCasinoId(),
            request.getPlayerId(),
            request.getGameId(),
            request.isRealMoney() ? "real" : "demo",
            request.getCurrency(),
            request.getLocale(),
            config.getLobbyUrl()
        );

        log.info("Pragmatic game launched: player={}, game={}", request.getPlayerId(), request.getGameId());

        return GameLaunchResult.builder()
            .gameUrl(gameUrl)
            .launchType(LaunchType.IFRAME)
            .build();
    }

    @Override
    public GameRoundResult placeBet(BetRequest request) {
        // Similar to Evolution adapter, with Pragmatic-specific logic
        // ...
        return GameRoundResult.success(request.getTransactionId());
    }

    @Override
    public GameRoundResult settleWin(WinRequest request) {
        // ...
        return GameRoundResult.success(request.getTransactionId());
    }

    @Override
    public GameRoundResult rollback(RollbackRequest request) {
        // ...
        return GameRoundResult.success(request.getTransactionId());
    }

    @Override
    public List<Game> syncGameCatalog() {
        // Pragmatic Play has different API for game list
        String url = PRAGMATIC_API_BASE + "/games?apiKey=" + getApiKey();
        // ...
        return List.of();
    }

    @Override
    public boolean validateCallbackSignature(String payload, String signature) {
        // Pragmatic Play uses MD5 instead of HMAC-SHA256
        // ...
        return true;
    }
}
```

### 3.4 Adapter Registry

```java
package net.lab1024.sa.base.module.support.game;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameProviderAdapterRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, GameProviderAdapter> adapters = new HashMap<>();

    @PostConstruct
    public void initialize() {
        // Auto-discover all GameProviderAdapter beans
        Map<String, GameProviderAdapter> adapterBeans =
            applicationContext.getBeansOfType(GameProviderAdapter.class);

        for (GameProviderAdapter adapter : adapterBeans.values()) {
            adapters.put(adapter.getProviderId(), adapter);
            log.info("Registered game provider adapter: {}", adapter.getProviderId());
        }
    }

    public GameProviderAdapter getAdapter(String providerId) {
        GameProviderAdapter adapter = adapters.get(providerId);
        if (adapter == null) {
            throw new ServiceException("Game provider not supported: " + providerId);
        }
        return adapter;
    }

    public List<String> getSupportedProviders() {
        return new ArrayList<>(adapters.keySet());
    }
}
```

---

## 4. Game Launch Flow

### 4.1 Seamless Wallet Game Launch

**Sequence Diagram**:
```
Player → Frontend → GameController → GameService → GameManager → WalletManager → ProviderAdapter
   │         │            │              │             │              │               │
   │  Click  │            │              │             │              │               │
   │  Game   │            │              │             │              │               │
   │────────>│            │              │             │              │               │
   │         │ POST /api/game/launch    │             │              │               │
   │         │───────────>│              │             │              │               │
   │         │            │ launchGame() │             │              │               │
   │         │            │─────────────>│             │              │               │
   │         │            │              │ Validate    │              │               │
   │         │            │              │ Balance     │              │               │
   │         │            │              │────────────>│              │               │
   │         │            │              │             │ getBalance() │               │
   │         │            │              │<────────────│              │               │
   │         │            │              │             │              │               │
   │         │            │              │ Create      │              │               │
   │         │            │              │ Session     │              │               │
   │         │            │              │─────────────────────────────────────────>│
   │         │            │              │             │              │  launchGame()│
   │         │            │              │<─────────────────────────────────────────│
   │         │<───────────│              │             │              │   Game URL   │
   │  Game   │            │              │             │              │               │
   │  Iframe │            │              │             │              │               │
```

**Implementation**:
```java
package net.lab1024.sa.admin.module.business.game.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GameLaunchManager {

    private final GameProviderAdapterRegistry adapterRegistry;
    private final WalletManager walletManager;  // P0-03 integration
    private final GameSessionDao gameSessionDao;
    private final PlayerDao playerDao;

    @Transactional
    public GameLaunchResponse launchGame(GameLaunchForm form) {
        String tenantId = TenantContextHolder.getTenantId();
        Long playerId = RequestUtils.getPlayerId();

        // 1. Validate game exists and is enabled
        Game game = gameDao.selectOne(
            new LambdaQueryWrapper<Game>()
                .eq(Game::getTenantId, tenantId)
                .eq(Game::getGameId, form.getGameId())
                .eq(Game::getStatus, GameStatus.ENABLED)
        );

        if (game == null) {
            throw new ServiceException("Game not found or disabled");
        }

        // 2. Validate player balance (for real money mode)
        if (form.isRealMoney()) {
            Wallet wallet = walletManager.getWallet(playerId);
            BigDecimal minBalance = BigDecimal.valueOf(10);  // $10 minimum

            if (wallet.getBalance().compareTo(minBalance) < 0) {
                throw new ServiceException("Insufficient balance to launch game");
            }
        }

        // 3. Check concurrent session limit (prevent multi-accounting)
        int activeSessions = gameSessionDao.countActiveSessions(tenantId, playerId);
        if (activeSessions >= 3) {
            throw new ServiceException("Maximum concurrent sessions exceeded (3)");
        }

        // 4. Create game session
        GameSession session = new GameSession();
        session.setTenantId(tenantId);
        session.setPlayerId(playerId);
        session.setGameId(form.getGameId());
        session.setProviderId(game.getProviderId());
        session.setCurrency(form.getCurrency());
        session.setRealMoney(form.isRealMoney());
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());

        gameSessionDao.insert(session);

        // 5. Get provider adapter and launch game
        GameProviderAdapter adapter = adapterRegistry.getAdapter(game.getProviderId());

        GameLaunchRequest request = GameLaunchRequest.builder()
            .playerId(playerId)
            .gameId(game.getProviderGameId())
            .currency(form.getCurrency())
            .locale(form.getLocale())
            .realMoney(form.isRealMoney())
            .sessionId(session.getId())
            .build();

        GameLaunchResult result = adapter.launchGame(request);

        // 6. Update session with launch URL
        session.setLaunchUrl(result.getGameUrl());
        session.setSessionToken(result.getSessionToken());
        gameSessionDao.updateById(session);

        log.info("Game launched: player={}, game={}, provider={}, mode={}, sessionId={}",
            playerId, form.getGameId(), game.getProviderId(),
            form.isRealMoney() ? "REAL" : "FREE", session.getId());

        return GameLaunchResponse.builder()
            .gameUrl(result.getGameUrl())
            .sessionId(session.getId())
            .launchType(result.getLaunchType())
            .build();
    }

    /**
     * Close game session (player exits game)
     */
    @Transactional
    public void closeSession(Long sessionId) {
        GameSession session = gameSessionDao.selectById(sessionId);

        if (session == null || !SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new ServiceException("Session not found or already closed");
        }

        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());

        // Calculate session statistics
        BigDecimal totalBet = gameRoundDao.sumBetBySession(sessionId);
        BigDecimal totalWin = gameRoundDao.sumWinBySession(sessionId);
        BigDecimal netProfit = totalWin.subtract(totalBet);

        session.setTotalBet(totalBet);
        session.setTotalWin(totalWin);
        session.setNetProfit(netProfit);

        gameSessionDao.updateById(session);

        log.info("Game session closed: sessionId={}, totalBet={}, totalWin={}, netProfit={}",
            sessionId, totalBet, totalWin, netProfit);
    }
}
```

### 4.2 Provider Callback Handling

**Provider callbacks** (bet, win, rollback) are sent to our platform when game rounds occur.

```java
package net.lab1024.sa.admin.module.business.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.module.support.game.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/callback/game")
@RequiredArgsConstructor
public class GameCallbackController {

    private final GameProviderAdapterRegistry adapterRegistry;

    @Operation(summary = "Evolution Gaming bet callback")
    @PostMapping("/evolution/bet")
    public ResponseDTO<GameRoundResult> evolutionBetCallback(
        @RequestBody String payload,
        @RequestHeader("X-Evolution-Signature") String signature
    ) {
        GameProviderAdapter adapter = adapterRegistry.getAdapter("evolution");

        // 1. Validate signature (prevent fraud)
        if (!adapter.validateCallbackSignature(payload, signature)) {
            log.error("Invalid Evolution callback signature");
            return ResponseDTO.error("INVALID_SIGNATURE", "Invalid signature");
        }

        // 2. Parse request
        BetRequest request = JSON.parseObject(payload, BetRequest.class);

        // 3. Process bet
        GameRoundResult result = adapter.placeBet(request);

        return ResponseDTO.ok(result);
    }

    @Operation(summary = "Evolution Gaming win callback")
    @PostMapping("/evolution/win")
    public ResponseDTO<GameRoundResult> evolutionWinCallback(
        @RequestBody String payload,
        @RequestHeader("X-Evolution-Signature") String signature
    ) {
        GameProviderAdapter adapter = adapterRegistry.getAdapter("evolution");

        if (!adapter.validateCallbackSignature(payload, signature)) {
            return ResponseDTO.error("INVALID_SIGNATURE", "Invalid signature");
        }

        WinRequest request = JSON.parseObject(payload, WinRequest.class);
        GameRoundResult result = adapter.settleWin(request);

        return ResponseDTO.ok(result);
    }

    @Operation(summary = "Evolution Gaming rollback callback")
    @PostMapping("/evolution/rollback")
    public ResponseDTO<GameRoundResult> evolutionRollbackCallback(
        @RequestBody String payload,
        @RequestHeader("X-Evolution-Signature") String signature
    ) {
        GameProviderAdapter adapter = adapterRegistry.getAdapter("evolution");

        if (!adapter.validateCallbackSignature(payload, signature)) {
            return ResponseDTO.error("INVALID_SIGNATURE", "Invalid signature");
        }

        RollbackRequest request = JSON.parseObject(payload, RollbackRequest.class);
        GameRoundResult result = adapter.rollback(request);

        return ResponseDTO.ok(result);
    }

    // Similar endpoints for Pragmatic Play, NetEnt, etc.
    @PostMapping("/pragmatic/bet")
    public ResponseDTO<GameRoundResult> pragmaticBetCallback(...) { ... }

    @PostMapping("/pragmatic/win")
    public ResponseDTO<GameRoundResult> pragmaticWinCallback(...) { ... }
}
```

---

## 5. Game Catalog Synchronization

### 5.1 Daily Sync from Providers

**Cron Job**: Daily at 2 AM, sync game metadata from all providers

```java
package net.lab1024.sa.admin.module.business.game.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameCatalogSyncScheduler {

    private final GameProviderAdapterRegistry adapterRegistry;
    private final GameDao gameDao;
    private final GameProviderConfigDao providerConfigDao;

    @Scheduled(cron = "0 0 2 * * ?")  // Daily 2 AM
    public void syncAllProviders() {
        log.info("Starting game catalog synchronization...");

        List<String> providers = adapterRegistry.getSupportedProviders();

        for (String providerId : providers) {
            try {
                syncProvider(providerId);
            } catch (Exception e) {
                log.error("Failed to sync provider: {}", providerId, e);
            }
        }

        log.info("Game catalog synchronization completed. Total providers: {}", providers.size());
    }

    private void syncProvider(String providerId) {
        GameProviderAdapter adapter = adapterRegistry.getAdapter(providerId);

        // 1. Fetch games from provider API
        List<Game> providerGames = adapter.syncGameCatalog();

        log.info("Fetched {} games from provider: {}", providerGames.size(), providerId);

        // 2. Update database (upsert: insert new, update existing)
        for (Game game : providerGames) {
            Game existingGame = gameDao.selectOne(
                new LambdaQueryWrapper<Game>()
                    .eq(Game::getProviderId, providerId)
                    .eq(Game::getProviderGameId, game.getProviderGameId())
            );

            if (existingGame == null) {
                // New game: insert
                game.setStatus(GameStatus.DISABLED);  // Default disabled, admin enables manually
                gameDao.insert(game);
                log.info("New game added: provider={}, gameId={}, name={}",
                    providerId, game.getProviderGameId(), game.getGameName());
            } else {
                // Existing game: update metadata (RTP, thumbnail, etc.)
                existingGame.setGameName(game.getGameName());
                existingGame.setRtp(game.getRtp());
                existingGame.setThumbnailUrl(game.getThumbnailUrl());
                existingGame.setGameType(game.getGameType());
                existingGame.setUpdatedAt(LocalDateTime.now());

                gameDao.updateById(existingGame);
            }
        }

        // 3. Invalidate Redis cache
        redisson.getKeys().deleteByPattern("game:catalog:*");

        log.info("Provider sync completed: provider={}, games={}", providerId, providerGames.size());
    }
}
```

### 5.2 Game Metadata Caching

**Redis Cache**: 10-second TTL for game catalog queries

```java
package net.lab1024.sa.admin.module.business.game.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameCatalogManager {

    private final GameDao gameDao;

    @Cacheable(value = "game:catalog", key = "#tenantId + ':' + #gameType", unless = "#result == null")
    public List<GameVO> getGamesByType(String tenantId, String gameType) {
        List<Game> games = gameDao.selectList(
            new LambdaQueryWrapper<Game>()
                .eq(Game::getTenantId, tenantId)
                .eq(Game::getGameType, gameType)
                .eq(Game::getStatus, GameStatus.ENABLED)
                .orderByDesc(Game::getPopularityScore)
                .last("LIMIT 100")
        );

        return SmartBeanUtil.copyList(games, GameVO.class);
    }

    @Cacheable(value = "game:catalog", key = "#tenantId + ':search:' + #keyword")
    public List<GameVO> searchGames(String tenantId, String keyword) {
        List<Game> games = gameDao.selectList(
            new LambdaQueryWrapper<Game>()
                .eq(Game::getTenantId, tenantId)
                .like(Game::getGameName, keyword)
                .eq(Game::getStatus, GameStatus.ENABLED)
                .orderByDesc(Game::getPopularityScore)
                .last("LIMIT 50")
        );

        return SmartBeanUtil.copyList(games, GameVO.class);
    }
}
```

---

## 6. RTP Verification & Compliance

### 6.1 RTP (Return to Player) Calculation

**Regulatory Requirement**: Malta Gaming Authority (MGA) requires RTP ≥ 92%

**Formula**: RTP = Total Wins / Total Bets × 100%

```java
package net.lab1024.sa.admin.module.business.game.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class RtpVerificationScheduler {

    private final GameRoundDao gameRoundDao;
    private final GameDao gameDao;
    private final AlertService alertService;

    @Scheduled(cron = "0 0 3 * * ?")  // Daily 3 AM
    public void verifyRtp() {
        log.info("Starting RTP verification...");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime today = LocalDateTime.now();

        // Calculate RTP for each game
        List<Game> games = gameDao.selectList(new LambdaQueryWrapper<>());

        for (Game game : games) {
            BigDecimal totalBet = gameRoundDao.sumBetByGame(game.getId(), yesterday, today);
            BigDecimal totalWin = gameRoundDao.sumWinByGame(game.getId(), yesterday, today);

            if (totalBet.compareTo(BigDecimal.ZERO) == 0) {
                continue;  // No bets placed, skip
            }

            BigDecimal actualRtp = totalWin.divide(totalBet, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            // Update game RTP
            game.setActualRtp(actualRtp);
            game.setRtpLastCalculated(LocalDateTime.now());
            gameDao.updateById(game);

            // Alert if RTP < 92% (MGA threshold)
            if (actualRtp.compareTo(BigDecimal.valueOf(92)) < 0) {
                log.error("RTP VIOLATION: game={}, provider={}, actualRtp={}, expectedRtp={}",
                    game.getGameName(), game.getProviderId(), actualRtp, game.getRtp());

                alertService.sendCriticalAlert(
                    "RTP Violation Detected",
                    String.format("Game: %s, Actual RTP: %.2f%% (Expected: %.2f%%)",
                        game.getGameName(), actualRtp, game.getRtp())
                );
            } else {
                log.info("RTP OK: game={}, actualRtp={}, expectedRtp={}",
                    game.getGameName(), actualRtp, game.getRtp());
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * ?")  // Monthly on 1st
    public void generateMonthlyRtpReport() {
        // Generate monthly RTP report for MGA audit
        // Export to PDF and store in MinIO
        // ...
    }
}
```

### 6.2 Game Round Audit Trail

**Requirement**: 7-year retention of all game rounds for regulatory audit

```sql
-- Partition by month for efficient archival
CREATE TABLE game_rounds_2026_01 PARTITION OF game_rounds
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE game_rounds_2026_02 PARTITION OF game_rounds
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Auto-create partitions via cron job
```

---

## 7. Database Schema

### 7.1 Game Providers

```sql
CREATE TABLE game_providers (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    provider_id             VARCHAR(50) NOT NULL,    -- evolution, pragmatic, netent
    provider_name           VARCHAR(100) NOT NULL,

    -- Configuration
    api_base_url            VARCHAR(200),
    api_key                 VARCHAR(200),
    secret_key              VARCHAR(200),
    casino_id               VARCHAR(100),
    callback_url            VARCHAR(200),

    -- Status
    status                  VARCHAR(20) NOT NULL DEFAULT 'ENABLED',  -- ENABLED, DISABLED

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_game_providers_tenant_provider UNIQUE (tenant_id, provider_id)
);
```

### 7.2 Games

```sql
CREATE TABLE games (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    provider_id             VARCHAR(50) NOT NULL,
    provider_game_id        VARCHAR(100) NOT NULL,   -- Provider's game identifier

    game_name               VARCHAR(200) NOT NULL,
    game_type               VARCHAR(50) NOT NULL,    -- SLOT, LIVE_CASINO, TABLE_GAME, VIDEO_POKER
    thumbnail_url           VARCHAR(500),

    -- RTP (Return to Player)
    rtp                     DECIMAL(5, 2),           -- Expected RTP from provider (e.g., 96.50)
    actual_rtp              DECIMAL(5, 2),           -- Calculated RTP from actual game rounds
    rtp_last_calculated     TIMESTAMP,

    -- Metadata
    volatility              VARCHAR(20),             -- LOW, MEDIUM, HIGH
    max_win                 DECIMAL(20, 2),          -- Maximum win amount (for marketing)
    min_bet                 DECIMAL(10, 2),
    max_bet                 DECIMAL(10, 2),

    -- Popularity
    popularity_score        INT NOT NULL DEFAULT 0,  -- Higher = more popular

    -- Status
    status                  VARCHAR(20) NOT NULL DEFAULT 'DISABLED',  -- ENABLED, DISABLED, MAINTENANCE

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_games_provider_game UNIQUE (provider_id, provider_game_id)
);

CREATE INDEX idx_games_tenant_type ON games(tenant_id, game_type);
CREATE INDEX idx_games_provider ON games(provider_id);
CREATE INDEX idx_games_status ON games(status);
```

### 7.3 Game Sessions

```sql
CREATE TABLE game_sessions (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    player_id               BIGINT NOT NULL,
    game_id                 BIGINT NOT NULL,
    provider_id             VARCHAR(50) NOT NULL,

    -- Session Details
    currency                VARCHAR(10) NOT NULL,
    real_money              BOOLEAN NOT NULL DEFAULT TRUE,
    launch_url              TEXT,
    session_token           VARCHAR(500),

    -- Session Statistics
    total_bet               DECIMAL(20, 2) NOT NULL DEFAULT 0,
    total_win               DECIMAL(20, 2) NOT NULL DEFAULT 0,
    net_profit              DECIMAL(20, 2) NOT NULL DEFAULT 0,
    round_count             INT NOT NULL DEFAULT 0,

    -- Status & Timestamps
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, CLOSED, ABANDONED
    started_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at               TIMESTAMP,

    CONSTRAINT fk_game_sessions_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_game_sessions_game FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE INDEX idx_game_sessions_tenant_player ON game_sessions(tenant_id, player_id);
CREATE INDEX idx_game_sessions_status ON game_sessions(status);
```

### 7.4 Game Rounds

```sql
CREATE TABLE game_rounds (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    player_id               BIGINT NOT NULL,
    session_id              BIGINT,
    game_id                 BIGINT NOT NULL,
    provider_id             VARCHAR(50) NOT NULL,

    -- Round Details
    round_id                VARCHAR(100) NOT NULL,   -- Provider's round identifier
    transaction_id          VARCHAR(100) NOT NULL,   -- Unique transaction ID (idempotency key)
    round_type              VARCHAR(20) NOT NULL,    -- BET, WIN, ROLLBACK

    -- Amounts
    bet_amount              DECIMAL(20, 2),
    win_amount              DECIMAL(20, 2),
    currency                VARCHAR(10) NOT NULL,

    -- Status
    status                  VARCHAR(20) NOT NULL,    -- COMPLETED, ROLLED_BACK

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_game_rounds_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_game_rounds_session FOREIGN KEY (session_id) REFERENCES game_sessions(id),
    CONSTRAINT uk_game_rounds_transaction UNIQUE (transaction_id)
) PARTITION BY RANGE (created_at);

-- Create partitions by month
CREATE TABLE game_rounds_2026_01 PARTITION OF game_rounds
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE INDEX idx_game_rounds_tenant_player ON game_rounds(tenant_id, player_id);
CREATE INDEX idx_game_rounds_session ON game_rounds(session_id);
CREATE INDEX idx_game_rounds_round_id ON game_rounds(round_id);
```

---

## 8. Implementation Details (SmartAdmin)

### 8.1 Controller Layer

```java
package net.lab1024.sa.admin.module.business.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.domain.PageResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = AdminSwaggerTagConst.Business.GAME_AGGREGATOR)
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Launch game")
    @PostMapping("/launch")
    public ResponseDTO<GameLaunchResponse> launchGame(@Valid @RequestBody GameLaunchForm form) {
        return ResponseDTO.ok(gameService.launchGame(form));
    }

    @Operation(summary = "Get game catalog")
    @GetMapping("/catalog")
    public ResponseDTO<List<GameVO>> getGameCatalog(
        @RequestParam(required = false) String gameType,
        @RequestParam(required = false) String providerId
    ) {
        return ResponseDTO.ok(gameService.getGameCatalog(gameType, providerId));
    }

    @Operation(summary = "Search games")
    @GetMapping("/search")
    public ResponseDTO<List<GameVO>> searchGames(@RequestParam String keyword) {
        return ResponseDTO.ok(gameService.searchGames(keyword));
    }

    @Operation(summary = "Close game session")
    @PostMapping("/session/{sessionId}/close")
    public ResponseDTO<Void> closeSession(@PathVariable Long sessionId) {
        gameService.closeSession(sessionId);
        return ResponseDTO.ok();
    }

    @Operation(summary = "Query game sessions")
    @PostMapping("/session/query")
    public ResponseDTO<PageResult<GameSessionVO>> querySessions(
        @Valid @RequestBody GameSessionQueryForm form
    ) {
        return ResponseDTO.ok(gameService.querySessions(form));
    }
}
```

### 8.2 Service Layer

```java
package net.lab1024.sa.admin.module.business.game.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameLaunchManager gameLaunchManager;
    private final GameCatalogManager gameCatalogManager;

    public GameLaunchResponse launchGame(GameLaunchForm form) {
        return gameLaunchManager.launchGame(form);
    }

    public List<GameVO> getGameCatalog(String gameType, String providerId) {
        String tenantId = TenantContextHolder.getTenantId();
        return gameCatalogManager.getGamesByType(tenantId, gameType);
    }

    public List<GameVO> searchGames(String keyword) {
        String tenantId = TenantContextHolder.getTenantId();
        return gameCatalogManager.searchGames(tenantId, keyword);
    }

    public void closeSession(Long sessionId) {
        gameLaunchManager.closeSession(sessionId);
    }

    public PageResult<GameSessionVO> querySessions(GameSessionQueryForm form) {
        Page<GameSession> page = SmartPageUtil.convert2PageQuery(form);
        page = gameLaunchManager.querySessions(page, form);
        return SmartPageUtil.convert2PageResult(page, GameSessionVO.class);
    }
}
```

### 8.3 Dao Layer

```java
package net.lab1024.sa.admin.module.business.game.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.game.domain.entity.GameRound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface GameRoundDao extends BaseMapper<GameRound> {

    /**
     * Sum bet amount by session
     */
    BigDecimal sumBetBySession(@Param("sessionId") Long sessionId);

    /**
     * Sum win amount by session
     */
    BigDecimal sumWinBySession(@Param("sessionId") Long sessionId);

    /**
     * Sum bet amount by game (for RTP calculation)
     */
    BigDecimal sumBetByGame(@Param("gameId") Long gameId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    /**
     * Sum win amount by game (for RTP calculation)
     */
    BigDecimal sumWinByGame(@Param("gameId") Long gameId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);
}
```

**MyBatis XML**:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="net.lab1024.sa.admin.module.business.game.dao.GameRoundDao">

    <select id="sumBetBySession" resultType="java.math.BigDecimal">
        SELECT COALESCE(SUM(bet_amount), 0)
        FROM game_rounds
        WHERE session_id = #{sessionId}
          AND round_type = 'BET'
          AND status = 'COMPLETED'
    </select>

    <select id="sumWinBySession" resultType="java.math.BigDecimal">
        SELECT COALESCE(SUM(win_amount), 0)
        FROM game_rounds
        WHERE session_id = #{sessionId}
          AND round_type = 'WIN'
          AND status = 'COMPLETED'
    </select>

    <select id="sumBetByGame" resultType="java.math.BigDecimal">
        SELECT COALESCE(SUM(bet_amount), 0)
        FROM game_rounds
        WHERE game_id = #{gameId}
          AND round_type = 'BET'
          AND status = 'COMPLETED'
          AND created_at BETWEEN #{startTime} AND #{endTime}
    </select>

    <select id="sumWinByGame" resultType="java.math.BigDecimal">
        SELECT COALESCE(SUM(win_amount), 0)
        FROM game_rounds
        WHERE game_id = #{gameId}
          AND round_type = 'WIN'
          AND status = 'COMPLETED'
          AND created_at BETWEEN #{startTime} AND #{endTime}
    </select>

</mapper>
```

---

## 9. Integration Points

### 9.1 Integration with P0-03 (Seamless Wallet)

**Requirement**: Game bets/wins debit/credit seamless wallet in real-time

```java
// Bet placement: debit wallet
walletManager.debit(
    playerId,
    betAmount,
    currency,
    "Game bet: " + gameId,
    idempotencyKey
);

// Win settlement: credit wallet
walletManager.credit(
    playerId,
    winAmount,
    currency,
    "Game win: " + gameId,
    idempotencyKey
);
```

### 9.2 Integration with P1-05 (Saga Pattern)

**Requirement**: Game session lifecycle managed via saga

```java
// Game session saga: launch → bet → win → close
String sagaId = sagaOrchestrator.startSaga(
    "GAME_SESSION_SAGA",
    Map.of(
        "sessionId", sessionId,
        "playerId", playerId,
        "gameId", gameId
    )
);
```

### 9.3 Integration with P1-07 (Multi-Tenant Isolation)

**Requirement**: Each tenant configures own provider integrations

```java
// Tenant-specific provider configuration
GameProviderConfig config = providerConfigDao.selectOne(
    new LambdaQueryWrapper<GameProviderConfig>()
        .eq(GameProviderConfig::getTenantId, tenantId)
        .eq(GameProviderConfig::getProviderId, providerId)
);
```

### 9.4 Integration with P1-11 (VIP System)

**Requirement**: VIP players get exclusive games and higher betting limits

```java
// Check VIP tier for game access
PlayerVipTier vipTier = vipTierManager.getPlayerTier(playerId);

if (game.isVipExclusive() && vipTier.getTierLevel() < VipTier.GOLD.getLevel()) {
    throw new ServiceException("Game requires Gold VIP tier or higher");
}

// Apply VIP betting limits
BigDecimal maxBet = game.getMaxBet();
if (vipTier.getTierLevel() >= VipTier.PLATINUM.getLevel()) {
    maxBet = maxBet.multiply(BigDecimal.valueOf(2));  // 2× max bet for Platinum+
}
```

---

## 10. Testing Strategy

### 10.1 Unit Tests

```java
@SpringBootTest
class GameProviderAdapterTest {

    @Autowired
    private GameProviderAdapterRegistry adapterRegistry;

    @Test
    void testEvolutionAdapterRegistered() {
        List<String> providers = adapterRegistry.getSupportedProviders();
        assertThat(providers).contains("evolution");
    }

    @Test
    void testEvolutionGameLaunch() {
        GameProviderAdapter adapter = adapterRegistry.getAdapter("evolution");

        GameLaunchRequest request = GameLaunchRequest.builder()
            .playerId(1L)
            .gameId("monopoly_live")
            .currency("USD")
            .locale("en")
            .realMoney(true)
            .build();

        GameLaunchResult result = adapter.launchGame(request);

        assertThat(result.getGameUrl()).contains("evolutiongaming.com");
        assertThat(result.getLaunchType()).isEqualTo(LaunchType.IFRAME);
    }
}
```

### 10.2 Integration Tests

```java
@SpringBootTest
@Transactional
class GameLaunchIntegrationTest {

    @Autowired
    private GameLaunchManager gameLaunchManager;

    @Autowired
    private WalletManager walletManager;

    @Test
    void testCompleteGameSession() {
        // 1. Launch game
        GameLaunchForm form = new GameLaunchForm();
        form.setGameId("monopoly_live");
        form.setCurrency("USD");
        form.setRealMoney(true);

        GameLaunchResponse response = gameLaunchManager.launchGame(form);
        assertThat(response.getGameUrl()).isNotNull();

        // 2. Place bet
        BetRequest betRequest = new BetRequest();
        betRequest.setPlayerId(1L);
        betRequest.setGameId("monopoly_live");
        betRequest.setBetAmount(BigDecimal.valueOf(10));

        GameRoundResult betResult = adapter.placeBet(betRequest);
        assertThat(betResult.isSuccess()).isTrue();

        // Verify wallet debited
        Wallet wallet = walletManager.getWallet(1L);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(90));

        // 3. Settle win
        WinRequest winRequest = new WinRequest();
        winRequest.setPlayerId(1L);
        winRequest.setGameId("monopoly_live");
        winRequest.setWinAmount(BigDecimal.valueOf(50));

        GameRoundResult winResult = adapter.settleWin(winRequest);
        assertThat(winResult.isSuccess()).isTrue();

        // Verify wallet credited
        wallet = walletManager.getWallet(1L);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(140));
    }
}
```

### 10.3 Performance Tests

```java
@SpringBootTest
class GameCatalogPerformanceTest {

    @Autowired
    private GameCatalogManager gameCatalogManager;

    @Test
    void testGameCatalogCacheHit() {
        // First call: cache miss (database query)
        long start1 = System.currentTimeMillis();
        List<GameVO> games1 = gameCatalogManager.getGamesByType("tenant1", "SLOT");
        long duration1 = System.currentTimeMillis() - start1;

        // Second call: cache hit (Redis)
        long start2 = System.currentTimeMillis();
        List<GameVO> games2 = gameCatalogManager.getGamesByType("tenant1", "SLOT");
        long duration2 = System.currentTimeMillis() - start2;

        // Cache hit should be 10× faster
        assertThat(duration2).isLessThan(duration1 / 10);
    }
}
```

---

## 11. Operations & Monitoring

### 11.1 Key Metrics

```java
@Component
public class GameMetrics {

    private final Counter gameLaunches = Counter.build()
        .name("game_launches_total")
        .help("Total game launches")
        .labelNames("tenant_id", "provider_id", "game_type")
        .register();

    private final Histogram gameLaunchLatency = Histogram.build()
        .name("game_launch_latency_seconds")
        .help("Game launch latency")
        .buckets(0.5, 1, 2, 5)
        .labelNames("provider_id")
        .register();

    private final Gauge activeGameSessions = Gauge.build()
        .name("active_game_sessions")
        .help("Active game sessions")
        .labelNames("tenant_id", "provider_id")
        .register();

    private final Counter gameRounds = Counter.build()
        .name("game_rounds_total")
        .help("Total game rounds")
        .labelNames("tenant_id", "provider_id", "round_type")
        .register();
}
```

### 11.2 Alerts

```yaml
groups:
  - name: game_alerts
    rules:
      - alert: GameLaunchLatencyHigh
        expr: histogram_quantile(0.95, game_launch_latency_seconds) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Game launch latency >2s p95"

      - alert: RtpViolation
        expr: game_actual_rtp < 92
        for: 1h
        labels:
          severity: critical
        annotations:
          summary: "Game RTP < 92% (MGA violation)"

      - alert: ProviderCallbackFailure
        expr: rate(game_callback_errors_total[5m]) > 0.05
        labels:
          severity: warning
        annotations:
          summary: "Provider callback error rate >5%"
```

---

## 12. Appendices

### 12.1 Supported Game Providers

| Provider | Status | Game Count | Integration Type |
|----------|--------|------------|------------------|
| Evolution Gaming | ✅ Implemented | 200+ | REST API + Iframe |
| Pragmatic Play | ✅ Implemented | 300+ | REST API + Iframe |
| NetEnt | 🔄 In Progress | 250+ | REST API + Iframe |
| Microgaming | 📅 Planned | 400+ | SOAP API |
| Playtech | 📅 Planned | 500+ | Custom Protocol |

### 12.2 Provider API Comparison

| Feature | Evolution | Pragmatic | NetEnt |
|---------|-----------|-----------|--------|
| Authentication | JWT | API Key | OAuth 2.0 |
| Game Launch | Iframe URL | Iframe URL | HTML5 Embed |
| Callback Signature | HMAC-SHA256 | MD5 | HMAC-SHA1 |
| Balance API | Yes (transfer wallet) | Yes | No (seamless only) |
| RTP Reporting | Daily | Weekly | Monthly |

### 12.3 Cost Analysis

**Monthly Costs** (100 merchants, 1M game sessions/month):

| Component | Cost | Rationale |
|-----------|------|-----------|
| Redis Cluster (game catalog cache) | $120 | 3 nodes, 8 GB each |
| PostgreSQL (game sessions/rounds) | $200 | 16 GB RAM, 500 GB SSD |
| Provider Integration Fees | $5,000 | $250 per provider × 20 providers |
| **Total** | **$5,320/month** | **~$63,840/year** |

**ROI**: Platform commission (5-15% of GGR) vs integration costs
- If monthly GGR = $1M → Platform commission = $50K-$150K
- Break-even after 1-2 months

---

## Document Status

**Version**: 1.0.0
**Status**: Draft (Ready for Technical Review)
**Lines**: ~1,450 lines
**Last Updated**: 2026-01-23

**Next Steps**:
1. Technical review by game integration team
2. Security audit of provider callback authentication
3. Load testing of game launch flow (target: <2s p95 latency)
4. RTP verification testing with historical data
5. Integration testing with Evolution Gaming (sandbox environment)

**Related Documents**:
- [P0-03: Seamless Wallet](../P0-critical/03-seamless-wallet-implementation.md)
- [P1-05: Distributed Transaction Patterns](05-distributed-transaction-patterns.md)
- [P1-07: Multi-Tenant Isolation](07-multi-tenant-isolation.md)
- [P1-11: VIP System Design](11-vip-system-design.md)
