# Game Lobby Requirements

> **Canonical Source**: [03-02_Game_Lobby_Management.md](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md)
> **Audience**: Executives, Product Managers, Operations Leads, UX Designers
> **Related Architecture**: [Game Lobby System (Architecture)](../../architecture/03_Game_Integration/Game_Lobby_System.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

The Game Lobby is responsible for managing the player-facing game list, including categorization, sorting, search, and personalized recommendations. The goal is to deliver a **personalized lobby experience** tailored to individual player preferences and behavior ("thousands of faces for thousands of players").

---

## 2. Game Metadata Management

### 2.1 Required Game Attributes

Each game in the lobby must maintain the following metadata:

| Attribute Category | Fields |
|--------------------|--------|
| **Basic Information** | Game name (multi-language), icon/thumbnail, game type (Slot, Live, Sport), provider name |
| **Technical Properties** | RTP (Return to Player percentage), volatility (Low/Medium/High), supported devices (Desktop/Mobile) |

### 2.2 Auto-Sync Process

New games from providers are synchronized automatically every 4 hours:

| Step | Action | Detail |
|------|--------|--------|
| 1 | Fetch game list | Call provider's GetGameList API |
| 2 | Identify new games | Compare with local database to detect new entries |
| 3 | Download assets | Retrieve game images and upload to platform CDN |
| 4 | Set initial status | Default to **DISABLED** (pending review) to prevent untranslated content from appearing |
| 5 | Notify operations | Send "New Game Detected" notification to the operations team |

---

## 3. Lobby Organization and Display Rules

### 3.1 Game Categorization

**System Categories** (predefined):
- Hot (popular games)
- New (recently launched)
- Jackpot (progressive prize pool games)

**Custom Categories** (merchant-configurable):
- Merchants can create custom labels such as "Weekly Picks", "Christmas Special", "Lunar New Year" etc.
- Custom categories are fully managed by the operations team per merchant

### 3.2 Sorting Rules

Games within each category are sorted using the following priority system:

| Priority | Sorting Method | Description |
|----------|---------------|-------------|
| 1 (Highest) | Manual Pin | Operations team can pin specific games to the top |
| 2 | Popularity-based | Automatic sorting by player count and betting volume |
| 3 (Default) | Weight-based | Default weight assigned to each game |

### 3.3 Dynamic Configuration

- **All lobby elements** (banners, game grids, menus, tags) must be configuration-driven. No frontend hardcoding is permitted.
- **Hot updates**: Configuration changes are pushed via polling or WebSocket, visible to players without page refresh.

---

## 4. Game Tags and Classification

### 4.1 Multi-Dimensional Tag System

**System Tags** (automatically generated):

| Tag | Trigger Condition |
|-----|-------------------|
| NEW | Launched within the last 7 days |
| HOT | More than 100 unique bettors in the last 24 hours |
| JACKPOT | Game is connected to a progressive prize pool |
| HIGH_RTP | RTP is 97% or higher |
| EXCLUSIVE | Platform-exclusive game |

**Operations Tags** (manually configured):

| Tag | Example Use |
|-----|-------------|
| Weekly Recommendation | Curated picks updated weekly |
| Seasonal Promotion | "Christmas Special", "Lunar New Year" |
| Quick Game | Single round duration under 30 seconds |
| High Stakes | Maximum bet exceeds $1,000 per wager |

**Player Tags** (user-generated):
- Favorite count (e.g., 1,200 players have favorited)
- Player rating (e.g., 4.8 out of 5.0)

---

## 5. Search and Filter Requirements

### 5.1 Filter Dimensions

Players must be able to filter games by the following criteria:

| Filter | Options |
|--------|---------|
| Game Provider | PG Soft, Pragmatic Play, Evolution, etc. |
| Game Type | Slots, Live Casino, Sports, Lottery |
| RTP Range | 95-96%, 96-97%, 97%+ |
| Volatility | Low, Medium, High |
| Bet Range | Minimum and maximum bet amount |

### 5.2 Search Capabilities

| Feature | Description |
|---------|-------------|
| Full-text search | Search across game name (multi-language), tags, and provider name |
| Pinyin support | Typing "shuiguoji" matches the Chinese game name for "Fruit Machine" |
| Synonym matching | "Slots" = "Fruit Machine" = other regional equivalents |
| Search history | Record player search terms for personalized hot search recommendations |
| Weighted results | Game name matches are prioritized over tag matches |

---

## 6. Personalized Recommendation Strategy

### 6.1 Recommendation Matrix by Player Segment

| Player Segment | Recommendation Strategy | Weight Distribution |
|----------------|------------------------|---------------------|
| **New Players** | Popular games + high RTP games | 60% popularity + 40% RTP |
| **Active Players** | Historical preferences + similar games | 70% collaborative filtering + 30% popularity |
| **VIP Players** | High-stakes games + exclusive games | 50% high-bet + 30% exclusive + 20% new games |
| **Churning Players** | Previously enjoyed games + new promotions | 60% historical preference + 40% new activities |

### 6.2 Behavioral Scoring

Player interactions with games are scored to drive recommendations:

| Action | Score |
|--------|-------|
| Click on a game | +1 |
| Play in demo mode | +3 |
| Place a real-money bet | +10 |
| Add to favorites | +5 |

---

## 7. Merchant Differentiation

| Feature | Description |
|---------|-------------|
| **Game Blocking** | Merchant A can hide all games with RTP above 98%; Merchant B can retain them |
| **Exclusive Games** | Certain games can be restricted to specific merchants only |

---

## 8. Approval Workflows

### 8.1 Emergency Game Takedown

| Step | Actor | Action |
|------|-------|--------|
| 1 | Operations | Initiates "Maintenance Request" (e.g., abnormal RTP detected) |
| 2 | Risk Control Manager | Approves the request |
| 3 | System | Immediately takes effect -- game is hidden or disabled |

### 8.2 New Game Launch Approval

| Step | Actor | Action |
|------|-------|--------|
| 1 | System | Auto-syncs new game from provider |
| 2 | Operations | Configures images, translations, and tags |
| 3 | Operations | Submits for review |
| 4 | Reviewer | Approves for launch |

**Purpose**: Prevents untested or partially translated games from being exposed to players.

---

## 9. UX Requirements

### 9.1 Performance Targets

| Metric | Target |
|--------|--------|
| API response time (P50) | Under 100ms |
| API response time (P99) | Under 500ms |
| Search query latency | Under 50ms |
| Image CDN hit rate | Above 98% |

### 9.2 Pagination and Scrolling

- Games are loaded in pages of 30 items by default
- Frontend uses virtual scrolling to render only visible game cards plus a buffer zone, ensuring smooth scroll performance even with thousands of games

---

## 10. Business KPIs

### 10.1 Core Metrics

| KPI | Formula |
|-----|---------|
| Game Click-Through Rate (CTR) | (Game clicks / Impressions) x 100% |
| Game Conversion Rate | (Actual bets / Game clicks) x 100% |
| Average Session Duration | AVG(session_duration) per game |
| Top 10 Popular Games | Ranked by total betting volume |

### 10.2 Alert Rules

| Alert Condition | Severity |
|-----------------|----------|
| Game RTP anomaly (above 105% or below 90%) | Warning |
| Game sync failure (3 consecutive failures) | Warning |
| API response time P99 exceeds 1 second | Warning |
| Cache hit rate drops below 80% | Warning |

---

## 11. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Game Click-Through Rate (CTR) | ≥15% | (Game clicks / Impressions) × 100% |
| Game Conversion Rate | ≥40% | (Actual bets / Game clicks) × 100% |
| API Response Time (P99) | <500ms | Latency monitoring at gateway level |
| Search Query Latency | <50ms | Elasticsearch query time tracking |
| Image CDN Hit Rate | ≥98% | CDN cache hit ratio logs |
| New Game Sync Success Rate | ≥99% | Automated sync job success/failure ratio |
| Player Personalization Match Rate | ≥70% | A/B test conversion lift from recommended vs random games |

---

## Related Documents

### Core Dependencies
- [Game Integration Standards](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md) - GP API specifications
- [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - Game launch flow

### Business Integration
- [Activity Bonus](../../source-archive/04_Activity_Center/04-04_Activity_Bonus.md) - Activity-based game recommendations
- [VIP Loyalty](../../source-archive/01_Player_Center/01-06_VIP_Loyalty.md) - VIP exclusive games

### UX and Design
- [Frontend Layout Engine](../../source-archive/11_Frontend_CMS/11-01_Frontend_Layout_Engine.md) - Lobby page design
- [Gateway Architecture](../../source-archive/09_Technical_Infrastructure/09-02-01_Gateway_Core.md) - API rate limiting

### Technical Implementation

→ **[Game Lobby System Architecture](../../architecture/03_Game_Integration/Game_Lobby_System.md)** - Game catalog management, filter algorithms, lazy loading implementation, CDN asset optimization, and personalization engine

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Game Team
