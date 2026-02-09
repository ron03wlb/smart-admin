# Game Lobby System

> **Canonical Source**: [03-02_Game_Lobby_Management.md](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md)
> **Audience**: Architects, Backend Engineers, Frontend Engineers, Search Engineers
> **Business Requirements**: [Game Lobby Requirements](../../requirements/03_Gaming_Operations/Game_Lobby_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. System Overview

The Game Lobby Service manages the player-facing game catalog, including metadata management, categorization, search, personalized recommendations, and multi-tenant game filtering. The system is designed for high-read, low-write workloads with aggressive caching.

---

## 2. Game Metadata Sync Architecture

### 2.1 Auto-Sync Job

A scheduled `GameDiscoveryJob` runs every 4 hours to synchronize game catalogs from all integrated providers:

```
GameDiscoveryJob (every 4 hours)
        │
        ▼
┌──────────────────────────┐
│ 1. Call GP GetGameList    │
│    API for each provider  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ 2. Diff against local DB │
│    Identify [NEW] games   │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────────┐
│ 3. Download game assets       │
│    Upload to platform CDN     │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│ 4. Insert with status =       │
│    DISABLED (pending review)  │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│ 5. Send "New Game Detected"  │
│    notification to ops team   │
└──────────────────────────────┘
```

### 2.2 Game Metadata Schema

**Core attributes per game record**:
- `game_id` (String, unique) - Provider-prefixed identifier (e.g., `pg_fortune_tiger`)
- `game_name` (JSON) - Multi-language map: `{"zh": "招財虎", "en": "Fortune Tiger"}`
- `provider` (String) - Provider identifier (e.g., `PG`, `PRAGMATIC`, `EVOLUTION`)
- `game_type` (Enum) - `SLOT`, `LIVE`, `SPORT`, `LOTTERY`
- `rtp` (Decimal) - Return to Player percentage (e.g., 96.81)
- `volatility` (Enum) - `LOW`, `MEDIUM`, `HIGH`
- `min_bet` / `max_bet` (Decimal) - Bet range
- `thumbnail` (URL) - CDN URL for game icon
- `tags` (Array[String]) - System and operations tags
- `status` (Enum) - `ENABLED`, `DISABLED`, `MAINTENANCE`
- `supported_devices` (Array[Enum]) - `DESKTOP`, `MOBILE`

---

## 3. Multi-Level Caching Architecture

```
┌─────────────────────────────────────────────┐
│  L1: CDN Cache (game icons, banners)        │
│  TTL: 7 days                                │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  L2: Redis Cache (game lists, sort results) │
│  TTL: 15 minutes                            │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  L3: Application Cache (JVM Caffeine)       │
│  TTL: 5 minutes                             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Database (MySQL - game metadata master)    │
└─────────────────────────────────────────────┘
```

### 3.1 Cache Invalidation Strategy

| Event | Invalidation Scope | Mechanism |
|-------|--------------------|-----------|
| Game enable/disable | Clear all caches for affected categories | Redis Pub/Sub broadcast |
| Popularity re-sort | Replace Redis L2 cache for sorted lists | Background job every 15 minutes |
| New game sync | Incremental update, clear related category caches only | Selective invalidation |

---

## 4. Search and Filter Implementation

### 4.1 Filter Dimensions

Supported filter parameters for the game list API:

| Parameter | Type | Values |
|-----------|------|--------|
| `category` | Enum | `HOT`, `NEW`, `JACKPOT`, custom tags |
| `provider` | String | `PG`, `PRAGMATIC`, `EVOLUTION`, etc. |
| `game_type` | Enum | `SLOT`, `LIVE`, `SPORT`, `LOTTERY` |
| `rtp_range` | Range | e.g., `95-96`, `96-97`, `97+` |
| `volatility` | Enum | `LOW`, `MEDIUM`, `HIGH` |
| `bet_range` | Range | Min/max bet amount |

### 4.2 Full-Text Search (Elasticsearch)

```json
{
  "query": {
    "multi_match": {
      "query": "水果機",
      "fields": ["game_name_zh^3", "game_name_en", "tags^2", "provider"],
      "fuzziness": "AUTO"
    }
  }
}
```

**Field boost weights**:
- `game_name_zh`: 3x (highest priority for Chinese name match)
- `tags`: 2x (secondary priority for tag match)
- `game_name_en`, `provider`: 1x (default weight)

### 4.3 Search Optimization

| Feature | Implementation |
|---------|---------------|
| Pinyin search | Custom Elasticsearch analyzer maps pinyin input (e.g., "shuiguoji") to Chinese characters |
| Synonym matching | Synonym filter: "Slots" = "Fruit Machine" = regional equivalents |
| Search history | Per-user search term logging for hot search recommendations |

---

## 5. Personalized Recommendation Engine

### 5.1 Strategy Matrix

| Player Segment | Algorithm | Weight Distribution |
|----------------|-----------|---------------------|
| New Players | Popularity + High RTP | 60% popularity + 40% RTP |
| Active Players | Collaborative filtering + Similar games | 70% CF + 30% popularity |
| VIP Players | High-stakes + Exclusive games | 50% high-bet + 30% exclusive + 20% new |
| Churning Players | Historical preferences + New promotions | 60% history + 40% new activities |

### 5.2 Behavioral Scoring Model

Real-time player interaction tracking:

| Action | Score Weight | Collection Method |
|--------|-------------|-------------------|
| Click on game | +1 | Frontend event |
| Demo play | +3 | Frontend event |
| Real-money bet | +10 | Transaction webhook |
| Add to favorites | +5 | API call |

---

## 6. API Endpoints

### 6.1 Game List API

```http
GET /api/v1/game-lobby/games?category={category}&provider={provider}&page={page}

Headers:
  Authorization: Bearer {token}
  X-Tenant-ID: {tenantId}

Query Parameters:
  - category (optional): 'HOT', 'NEW', 'JACKPOT'
  - provider (optional): 'PG', 'PRAGMATIC', 'EVOLUTION'
  - search (optional): Search keyword
  - page (required): Page number
  - size (optional): Items per page (default 30)

Response (200 OK):
{
  "code": 0,
  "msg": "success",
  "data": {
    "games": [
      {
        "game_id": "pg_fortune_tiger",
        "game_name": "Fortune Tiger",
        "provider": "PG Soft",
        "rtp": 96.81,
        "thumbnail": "https://cdn.example.com/games/fortune_tiger.jpg",
        "tags": ["HOT", "HIGH_RTP"],
        "popularity_score": 9.2
      }
    ],
    "pagination": {
      "current_page": 1,
      "total_pages": 15,
      "total_count": 450
    }
  }
}
```

### 6.2 Game Detail API

```http
GET /api/v1/game-lobby/games/{gameId}

Response (200 OK):
{
  "code": 0,
  "data": {
    "game_id": "pg_fortune_tiger",
    "game_name": "Fortune Tiger (招財虎)",
    "provider": "PG Soft",
    "rtp": 96.81,
    "volatility": "MEDIUM",
    "min_bet": 0.10,
    "max_bet": 250.00,
    "description": "Asian-themed slot machine...",
    "features": ["Free Spins", "Multipliers", "Re-Spins"],
    "stats": {
      "total_players_today": 1234,
      "total_bets_today": 45678,
      "avg_rating": 4.7,
      "favorite_count": 890
    }
  }
}
```

### 6.3 Multi-Tenant Header

All lobby APIs require the `X-Tenant-ID` header. Tenant-specific rules:
- **Game blocking**: Merchant A can hide games with RTP > 98%; Merchant B can retain them
- **Exclusive games**: Certain games visible only to specific merchants

---

## 7. Sorting Implementation

### 7.1 Sorting Priority

```
┌─────────────────────────────────────┐
│  Priority 1: Manual Pin (is_pinned) │
│  ──────────────────────────────────  │
│  Priority 2: Popularity Score       │
│  (calculated from bet count +       │
│   bet volume in last 24h)           │
│  ──────────────────────────────────  │
│  Priority 3: Default Weight         │
│  (assigned per game)                │
└─────────────────────────────────────┘
```

### 7.2 Popularity Calculation

Popularity scores are recalculated by a background job every 15 minutes using:
- Unique player count in the last 24 hours
- Total betting volume in the last 24 hours

---

## 8. Tag System Architecture

### 8.1 System Tags (Automated)

| Tag | Calculation Rule |
|-----|-----------------|
| `NEW` | `created_at` within last 7 days |
| `HOT` | Unique bettors in last 24h > 100 |
| `JACKPOT` | `has_progressive_pool = true` |
| `HIGH_RTP` | `rtp >= 97.0` |
| `EXCLUSIVE` | `exclusive_tenant_ids IS NOT NULL` |

### 8.2 Operations Tags (Manual)

Stored as a JSON array on the game record, managed via admin back-office.

### 8.3 Player Tags (User-Generated)

- Favorite count: aggregated from `user_favorites` table
- Rating: average from `user_ratings` table

---

## 9. Frontend Virtual Scrolling

For rendering large game catalogs (1000+ games):

- Use `react-window` or `react-virtualized` for virtual scrolling
- Render only visible viewport + upper/lower buffer zones
- Pagination API returns 30 items per page by default
- Infinite scroll triggers next page load at 80% scroll depth

---

## 10. Monitoring and Alerting

### 10.1 Technical SLA Targets

| Metric | Target |
|--------|--------|
| API response time (P50) | < 100ms |
| API response time (P99) | < 500ms |
| Redis cache hit rate | > 95% |
| Elasticsearch query latency | < 50ms |
| CDN image hit rate | > 98% |

### 10.2 Alert Rules

| Condition | Severity | Action |
|-----------|----------|--------|
| Game RTP anomaly (> 105% or < 90%) | Warning | Alert risk team |
| Game sync failure (3 consecutive) | Warning | Alert ops team |
| API response P99 > 1s | Warning | Alert engineering |
| Cache hit rate < 80% | Warning | Alert engineering |

---

## 11. Dynamic Configuration

All lobby elements (banners, game grids, menus, tags) are driven by JSON configuration:

- **No frontend hardcoding** of any lobby layout or content
- **Hot reload**: Changes pushed via polling or WebSocket; players see updates without page refresh
- Configuration changes trigger selective cache invalidation

---

## Related Documents

### Core Dependencies
- [Game Integration Standards](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) - GP API specifications
- [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - Game launch flow

### Technical Architecture
- [Frontend Layout Engine](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md) - Lobby page design patterns
- [Gateway Architecture](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) - API rate limiting

### Business Integration
- [Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) - Activity-based game recommendations
- [VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) - VIP exclusive game access

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Integration Team & Backend Team
