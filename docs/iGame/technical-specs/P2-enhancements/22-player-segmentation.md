# P2-22: Player Segmentation & Targeting

**Version**: 1.0
**Last Updated**: 2026-01-23
**Status**: Draft
**Priority**: P2 (Enhancement)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Requirements](#2-requirements)
3. [Architecture Design](#3-architecture-design)
4. [RFM Analysis](#4-rfm-analysis)
5. [Player Lifecycle Segmentation](#5-player-lifecycle-segmentation)
6. [Behavioral Segmentation](#6-behavioral-segmentation)
7. [Database Schema Design](#7-database-schema-design)
8. [Real-Time Segment Updates](#8-real-time-segment-updates)
9. [Integration Points](#9-integration-points)
10. [SmartAdmin Implementation](#10-smartadmin-implementation)
11. [Testing Strategy](#11-testing-strategy)
12. [Operations & Monitoring](#12-operations--monitoring)
13. [Appendices](#13-appendices)

---

## 1. Background & Strategic Context

### 1.1 Business Context

**From [igame_str.md](../../igame_str.md) - Data Leverage**:
> "Every player interaction generates data that compounds into competitive moats. Data leverage enables intelligent decisions at scale."

**From [backend_project.md](../../backend_project.md) Section 3.7**:
> "VIP system with automated tier progression based on player lifetime value. Personalized bonus offers maximize engagement and retention."

### 1.2 Problem Statement

**Current Limitation**:
- Treat all players the same (mass marketing approach)
- Cannot identify high-value players early (miss retention opportunities)
- No automated churn prediction (reactive instead of proactive)
- Bonus offers not personalized (15% lower conversion than targeted offers)

**Business Impact**:
- **Revenue Loss**: $120K annual revenue loss from churned high-value players (could have been prevented)
- **Marketing Waste**: 60% of bonus offers sent to low-engagement players (low ROI)
- **Missed LTV**: High-value players not identified until month 3 (lost 2 months of VIP benefits)

### 1.3 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Segment Coverage** | 100% of players | All active players assigned to segments |
| **Segment Update Latency** | <5 minutes | Real-time segment recalculation via Flink |
| **Churn Prediction Accuracy** | >75% precision | Machine learning model evaluation |
| **Targeted Campaign Conversion** | +30% vs broadcast | A/B test targeted vs mass campaigns |
| **Query Performance** | <200ms p95 | Segment membership lookup |

---

## 2. Requirements

### 2.1 Functional Requirements

**FR-SEG-001: RFM Analysis**
- Calculate Recency (days since last activity), Frequency (activity count), Monetary (total deposits)
- Segment players into 125 RFM buckets (5 × 5 × 5)
- Update RFM scores daily via batch job

**FR-SEG-002: Lifecycle Segmentation**
- New: 0-7 days since registration
- Active: Played in last 7 days
- At-Risk: No activity in 8-30 days
- Churned: No activity in 30+ days
- Real-time lifecycle updates (move to "at-risk" immediately after 8 days)

**FR-SEG-003: Behavioral Segmentation**
- Slot Enthusiasts: >80% of play on slot games
- Sports Bettors: >50% of wagers on sports betting
- High Rollers: Average bet size >$100
- Bonus Hunters: Wagering ratio <1.5x (only play with bonuses)

**FR-SEG-004: Demographic Segmentation**
- Country-based: Brazil, Mexico, Spain, etc.
- Platform: Mobile (iOS/Android), Desktop (Web)
- Age Group: 18-24, 25-34, 35-44, 45+

**FR-SEG-005: Dynamic Segment Membership**
- Players automatically enter/exit segments based on behavior
- Segment history tracking (player was "high_roller" from Jan 1-15, then moved to "medium_roller")

### 2.2 Non-Functional Requirements

**NFR-SEG-001: Performance**
- Segment membership lookup: <50ms p95 latency
- Segment recalculation: <5 minutes from trigger event
- Campaign targeting query: <2 seconds for 100K players

**NFR-SEG-002: Scalability**
- Support 1M+ players across all tenants
- Handle 100+ custom segments per tenant

**NFR-SEG-003: Flexibility**
- Allow merchants to define custom segments via UI
- No-code segment builder (drag-and-drop filters)

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Player Activity Events                        │
│  Kafka: player-events topic                                     │
│  - Login, Deposit, Bet, Win, Withdrawal                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              Flink Stream Processing                             │
│  - Calculate rolling metrics (last 7d activity, total deposits)  │
│  - Evaluate segment membership rules                            │
│  - Emit segment change events                                   │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              PostgreSQL + Redis Cache                            │
│                                                                  │
│  PostgreSQL: t_player_segment (segment membership history)      │
│  Redis: segment:{tenant}:{player_id} → ["vip", "slot_lover"]   │
│                                                                  │
│  Cache hit rate: >95%                                            │
│  Cache TTL: 24 hours (refreshed on segment change)              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Application Layer                                │
│                                                                  │
│  if (segmentService.isPlayerInSegment(playerId, "high_roller")) {    │
│      offerVipBonus(playerId);                                   │
│  } else if (segmentService.isPlayerInSegment(playerId, "at_risk")) { │
│      sendRetentionOffer(playerId);                              │
│  }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Key Design Decisions

**Decision 1: Flink for Real-Time Segment Updates**
- **Pattern**: Events → Flink → Segment recalculation → Database + Redis
- **Rationale**: <5-minute latency requirement, 10K events/sec throughput
- **Alternative Rejected**: Batch processing (30-minute delay too slow)

**Decision 2: Redis Cache for Segment Membership**
- **Pattern**: Check Redis first (fast path), fallback to PostgreSQL
- **Rationale**: <50ms lookup requirement, 95%+ cache hit rate
- **Cache Invalidation**: Expire on segment change event

**Decision 3: Segment History Table**
- **Pattern**: Store segment membership changes with timestamps
- **Rationale**: Analytics (how long was player in "high_roller" segment?), audit trail
- **Trade-off**: Storage overhead (~1KB per player per month)

**Decision 4: Multi-Tenancy via tenant_id**
- **Pattern**: Each tenant defines custom segments, isolated data
- **Rationale**: Merchant A's "high_roller" definition differs from Merchant B

---

## 4. RFM Analysis

### 4.1 RFM Scoring Algorithm

**Recency (R)**: Days since last activity (login, deposit, bet)
- Score 5: 0-1 days (very recent)
- Score 4: 2-7 days
- Score 3: 8-14 days
- Score 2: 15-30 days
- Score 1: 31+ days (inactive)

**Frequency (F)**: Number of sessions in last 30 days
- Score 5: 20+ sessions
- Score 4: 10-19 sessions
- Score 3: 5-9 sessions
- Score 2: 2-4 sessions
- Score 1: 0-1 sessions

**Monetary (M)**: Total deposits in last 30 days (USD equivalent)
- Score 5: $1,000+
- Score 4: $500-999
- Score 3: $200-499
- Score 2: $50-199
- Score 1: $0-49

**RFM Segment**: Concatenate scores → "555" (best), "111" (worst)

### 4.2 RFM Calculation (SQL)

```sql
WITH player_metrics AS (
    SELECT
        player_id,
        tenant_id,
        -- Recency: days since last activity
        EXTRACT(EPOCH FROM (NOW() - MAX(last_activity_at))) / 86400 AS recency_days,
        -- Frequency: session count in last 30 days
        COUNT(DISTINCT session_id) FILTER (WHERE session_started_at >= NOW() - INTERVAL '30 days') AS frequency_30d,
        -- Monetary: total deposits in last 30 days
        COALESCE(SUM(deposit_amount) FILTER (WHERE deposited_at >= NOW() - INTERVAL '30 days'), 0) AS monetary_30d
    FROM t_player_activity
    WHERE tenant_id = :tenantId
    GROUP BY player_id, tenant_id
),
rfm_scores AS (
    SELECT
        player_id,
        tenant_id,
        -- Recency score (reverse: lower days = higher score)
        CASE
            WHEN recency_days <= 1 THEN 5
            WHEN recency_days <= 7 THEN 4
            WHEN recency_days <= 14 THEN 3
            WHEN recency_days <= 30 THEN 2
            ELSE 1
        END AS r_score,
        -- Frequency score
        CASE
            WHEN frequency_30d >= 20 THEN 5
            WHEN frequency_30d >= 10 THEN 4
            WHEN frequency_30d >= 5 THEN 3
            WHEN frequency_30d >= 2 THEN 2
            ELSE 1
        END AS f_score,
        -- Monetary score
        CASE
            WHEN monetary_30d >= 1000 THEN 5
            WHEN monetary_30d >= 500 THEN 4
            WHEN monetary_30d >= 200 THEN 3
            WHEN monetary_30d >= 50 THEN 2
            ELSE 1
        END AS m_score
    FROM player_metrics
)
SELECT
    player_id,
    tenant_id,
    r_score,
    f_score,
    m_score,
    CONCAT(r_score, f_score, m_score) AS rfm_segment  -- e.g., "555", "341"
FROM rfm_scores;
```

### 4.3 RFM Segment Interpretation

**Champions (RFM 555, 554, 544, 545)**:
- Best customers: Recent, frequent, high spenders
- Action: Exclusive VIP rewards, early access to new games

**Loyal Customers (RFM 543, 444, 435, 355)**:
- Regular players with good value
- Action: Loyalty bonuses, referral incentives

**At-Risk (RFM 244, 334, 343, 245)**:
- Were good customers, declining activity
- Action: Win-back campaigns, personalized offers

**Churned (RFM 111, 112, 121, 131)**:
- Inactive, low value
- Action: Reactivation email (last attempt), then archive

---

## 5. Player Lifecycle Segmentation

### 5.1 Lifecycle Stages

```java
public enum PlayerLifecycleStage {
    NEW("0-7 days since registration"),
    ACTIVE("Played in last 7 days"),
    AT_RISK("No activity in 8-30 days"),
    CHURNED("No activity in 30+ days"),
    REACTIVATED("Was churned, returned within 90 days");

    private final String description;
}
```

### 5.2 Lifecycle Transition Rules

```
NEW (0-7d)
    │
    ├─> First deposit + bet → ACTIVE
    └─> 7 days, no deposit → AT_RISK

ACTIVE
    │
    ├─> Activity in last 7d → Stay ACTIVE
    └─> No activity for 8d → AT_RISK

AT_RISK (8-30d)
    │
    ├─> Returns → ACTIVE
    └─> 30 days → CHURNED

CHURNED (30d+)
    │
    └─> Returns within 90d → REACTIVATED
```

### 5.3 Flink Lifecycle Monitoring

```java
public class PlayerLifecycleMonitoringJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka source: player-events
        DataStream<PlayerEvent> events = env
            .addSource(new FlinkKafkaConsumer<>("player-events", deserializer, props));

        // Key by player_id
        DataStream<PlayerLifecycleUpdate> lifecycleUpdates = events
            .keyBy(PlayerEvent::getPlayerId)
            .process(new LifecycleEvaluator());

        // Sink to Kafka: segment-change-events
        lifecycleUpdates.addSink(new FlinkKafkaProducer<>("segment-change-events", serializer, props));

        env.execute("Player Lifecycle Monitoring");
    }

    static class LifecycleEvaluator extends KeyedProcessFunction<Long, PlayerEvent, PlayerLifecycleUpdate> {

        private transient ValueState<Long> lastActivityTimestamp;
        private transient ValueState<PlayerLifecycleStage> currentStage;

        @Override
        public void processElement(PlayerEvent event, Context ctx, Collector<PlayerLifecycleUpdate> out) {
            Long lastActivity = lastActivityTimestamp.value();
            PlayerLifecycleStage stage = currentStage.value();

            // Update last activity
            lastActivityTimestamp.update(event.getTimestamp());

            // Evaluate lifecycle stage
            if (lastActivity == null) {
                // First event → NEW
                stage = PlayerLifecycleStage.NEW;
            } else {
                long daysSinceActivity = (event.getTimestamp() - lastActivity) / 86400000L;

                if (daysSinceActivity <= 7) {
                    stage = PlayerLifecycleStage.ACTIVE;
                } else if (daysSinceActivity <= 30) {
                    stage = PlayerLifecycleStage.AT_RISK;
                } else {
                    stage = PlayerLifecycleStage.CHURNED;
                }
            }

            // Emit lifecycle update
            PlayerLifecycleUpdate update = new PlayerLifecycleUpdate();
            update.setPlayerId(event.getPlayerId());
            update.setNewStage(stage);
            update.setTimestamp(event.getTimestamp());
            out.collect(update);

            currentStage.update(stage);

            // Schedule timer to check for churn (8 days from now)
            ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + 8 * 86400000L);
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<PlayerLifecycleUpdate> out) {
            // Timer fired → check if player is now AT_RISK or CHURNED
            Long lastActivity = lastActivityTimestamp.value();
            if (lastActivity != null) {
                long daysSinceActivity = (System.currentTimeMillis() - lastActivity) / 86400000L;

                if (daysSinceActivity > 7 && daysSinceActivity <= 30) {
                    // Transition to AT_RISK
                    PlayerLifecycleUpdate update = new PlayerLifecycleUpdate();
                    update.setPlayerId(ctx.getCurrentKey());
                    update.setNewStage(PlayerLifecycleStage.AT_RISK);
                    update.setTimestamp(System.currentTimeMillis());
                    out.collect(update);

                    currentStage.update(PlayerLifecycleStage.AT_RISK);

                    // Schedule next check for churn (22 days from now)
                    ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + 22 * 86400000L);
                } else if (daysSinceActivity > 30) {
                    // Transition to CHURNED
                    PlayerLifecycleUpdate update = new PlayerLifecycleUpdate();
                    update.setPlayerId(ctx.getCurrentKey());
                    update.setNewStage(PlayerLifecycleStage.CHURNED);
                    update.setTimestamp(System.currentTimeMillis());
                    out.collect(update);

                    currentStage.update(PlayerLifecycleStage.CHURNED);
                }
            }
        }
    }
}
```

---

## 6. Behavioral Segmentation

### 6.1 Game Preference Segmentation

**Segment Definitions**:

```java
@Service
@RequiredArgsConstructor
public class BehavioralSegmentService {

    /**
     * Segment players by game preference
     *
     * @param playerId Player ID
     * @return List of behavioral segments (can belong to multiple)
     */
    public List<String> calculateGamePreferenceSegments(Long playerId) {
        // Fetch player's game activity breakdown (last 30 days)
        GameActivityBreakdown breakdown = getGameActivityBreakdown(playerId);

        List<String> segments = new ArrayList<>();

        // Slot Enthusiast: >80% of wagers on slots
        if (breakdown.getSlotWagerPercentage() > 0.80) {
            segments.add("slot_enthusiast");
        }

        // Sports Bettor: >50% of wagers on sports
        if (breakdown.getSportsWagerPercentage() > 0.50) {
            segments.add("sports_bettor");
        }

        // Live Casino Player: >30% of wagers on live casino
        if (breakdown.getLiveCasinoWagerPercentage() > 0.30) {
            segments.add("live_casino_player");
        }

        // Table Games Fan: >40% of wagers on blackjack/roulette/baccarat
        if (breakdown.getTableGamesWagerPercentage() > 0.40) {
            segments.add("table_games_fan");
        }

        return segments;
    }

    /**
     * Segment players by bet sizing
     */
    public List<String> calculateBetSizingSegments(Long playerId) {
        BettingStats stats = getBettingStats(playerId);

        List<String> segments = new ArrayList<>();

        // High Roller: average bet >$100
        if (stats.getAverageBetSize().compareTo(BigDecimal.valueOf(100)) > 0) {
            segments.add("high_roller");
        }
        // Medium Roller: average bet $10-100
        else if (stats.getAverageBetSize().compareTo(BigDecimal.valueOf(10)) >= 0) {
            segments.add("medium_roller");
        }
        // Low Roller: average bet <$10
        else {
            segments.add("low_roller");
        }

        // Bonus Hunter: wagering ratio <1.5x (only plays with bonuses)
        if (stats.getWageringRatio().compareTo(BigDecimal.valueOf(1.5)) < 0) {
            segments.add("bonus_hunter");
        }

        return segments;
    }
}
```

---

## 7. Database Schema Design

### 7.1 Player Segment Table

```sql
CREATE TABLE t_player_segment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    player_id BIGINT NOT NULL,
    segment_id VARCHAR(128) NOT NULL,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exited_at TIMESTAMP,  -- NULL if currently in segment

    INDEX idx_player_segment_current (tenant_id, player_id, segment_id, exited_at)
);

CREATE INDEX idx_player_segment_history
    ON t_player_segment(tenant_id, segment_id, entered_at, exited_at);

COMMENT ON COLUMN t_player_segment.exited_at IS 'NULL = currently in segment, timestamp = exited segment';
```

### 7.2 Segment Definition Table

```sql
CREATE TABLE t_segment_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    segment_id VARCHAR(128) NOT NULL,
    segment_name VARCHAR(255) NOT NULL,
    segment_type VARCHAR(50) NOT NULL,  -- RFM, LIFECYCLE, BEHAVIORAL, CUSTOM
    description TEXT,
    filter_rules JSONB,  -- JSON rules for segment membership
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(tenant_id, segment_id)
);

CREATE INDEX idx_segment_definition_type
    ON t_segment_definition(tenant_id, segment_type, is_active);
```

**Example filter_rules (JSONB)**:

```json
{
  "type": "AND",
  "conditions": [
    {
      "field": "total_deposits_30d",
      "operator": ">=",
      "value": 500
    },
    {
      "field": "sessions_30d",
      "operator": ">=",
      "value": 10
    },
    {
      "field": "vip_tier",
      "operator": "IN",
      "value": ["GOLD", "PLATINUM", "DIAMOND"]
    }
  ]
}
```

### 7.3 Efficient Segment Membership Query

```java
/**
 * Check if player is currently in segment (fast Redis lookup)
 */
public boolean isPlayerInSegment(Long playerId, String segmentId) {
    String tenantId = TenantContextHolder.getTenantId();

    // Check Redis cache first
    String cacheKey = String.format("segment:%s:%d", tenantId, playerId);
    Set<String> segments = redisService.sMembers(cacheKey);

    if (segments != null) {
        return segments.contains(segmentId);
    }

    // Cache miss → query database
    List<PlayerSegment> activeSegments = playerSegmentDao.selectList(
        new LambdaQueryWrapper<PlayerSegment>()
            .eq(PlayerSegment::getTenantId, tenantId)
            .eq(PlayerSegment::getPlayerId, playerId)
            .isNull(PlayerSegment::getExitedAt)  // Currently in segment
    );

    segments = activeSegments.stream()
        .map(PlayerSegment::getSegmentId)
        .collect(Collectors.toSet());

    // Cache for 24 hours
    redisService.sAddAll(cacheKey, segments.toArray(new String[0]));
    redisService.expire(cacheKey, Duration.ofHours(24));

    return segments.contains(segmentId);
}
```

---

## 8. Real-Time Segment Updates

### 8.1 Segment Change Consumer

```java
@Component
@RequiredArgsConstructor
public class SegmentChangeConsumer {

    private final PlayerSegmentDao playerSegmentDao;
    private final RedisService redisService;

    @KafkaListener(topics = "segment-change-events", groupId = "segment-consumer")
    public void handleSegmentChange(SegmentChangeEvent event) {
        String tenantId = event.getTenantId();
        Long playerId = event.getPlayerId();
        String segmentId = event.getSegmentId();
        SegmentChangeType changeType = event.getChangeType();

        if (changeType == SegmentChangeType.ENTER) {
            // Player entered segment
            PlayerSegment segment = new PlayerSegment();
            segment.setTenantId(tenantId);
            segment.setPlayerId(playerId);
            segment.setSegmentId(segmentId);
            segment.setEnteredAt(LocalDateTime.now());
            playerSegmentDao.insert(segment);

            // Update Redis cache
            String cacheKey = String.format("segment:%s:%d", tenantId, playerId);
            redisService.sAdd(cacheKey, segmentId);

        } else if (changeType == SegmentChangeType.EXIT) {
            // Player exited segment
            PlayerSegment segment = playerSegmentDao.selectOne(
                new LambdaQueryWrapper<PlayerSegment>()
                    .eq(PlayerSegment::getTenantId, tenantId)
                    .eq(PlayerSegment::getPlayerId, playerId)
                    .eq(PlayerSegment::getSegmentId, segmentId)
                    .isNull(PlayerSegment::getExitedAt)
            );

            if (segment != null) {
                segment.setExitedAt(LocalDateTime.now());
                playerSegmentDao.updateById(segment);

                // Update Redis cache
                String cacheKey = String.format("segment:%s:%d", tenantId, playerId);
                redisService.sRem(cacheKey, segmentId);
            }
        }

        log.info("Segment change processed: player={}, segment={}, type={}",
                 playerId, segmentId, changeType);
    }
}
```

---

## 9. Integration Points

### 9.1 A/B Testing (P2-21)

**Use Case**: Run A/B test only on "high_roller" segment

```java
@Service
@RequiredArgsConstructor
public class ABTestService {

    private final PlayerSegmentService segmentService;

    public ABTestVariant getVariant(String experimentId, Long playerId) {
        ABTestExperiment experiment = getExperiment(experimentId);

        // Check targeting rules
        if (experiment.getTargetingRules() != null) {
            String requiredSegment = experiment.getTargetingRules().getSegmentId();
            if (!segmentService.isPlayerInSegment(playerId, requiredSegment)) {
                return null;  // Player not in target segment
            }
        }

        // Assign variant
        return assignVariant(experimentId, playerId);
    }
}
```

### 9.2 Bonus Targeting (P1-12)

**Use Case**: Offer 200% bonus only to "at_risk" players

```java
@Service
@RequiredArgsConstructor
public class BonusOfferService {

    private final PlayerSegmentService segmentService;

    public List<BonusOffer> getAvailableOffers(Long playerId) {
        List<BonusOffer> offers = new ArrayList<>();

        // Retention offer for at-risk players
        if (segmentService.isPlayerInSegment(playerId, "at_risk")) {
            offers.add(new BonusOffer("retention_200", "200% Deposit Bonus", 2.00));
        }

        // Exclusive offer for VIP players
        if (segmentService.isPlayerInSegment(playerId, "rfm_555")) {
            offers.add(new BonusOffer("vip_exclusive", "VIP Cashback 10%", 0.10));
        }

        return offers;
    }
}
```

### 9.3 Notification Targeting (P2-17)

**Use Case**: Send push notification only to "slot_enthusiast" when new slot game launches

```java
@Service
@RequiredArgsConstructor
public class CampaignManager {

    private final PlayerSegmentService segmentService;
    private final NotificationManager notificationManager;

    @Transactional(rollbackFor = Exception.class)
    public void sendSegmentedCampaign(String segmentId, String templateCode) {
        String tenantId = TenantContextHolder.getTenantId();

        // Query all players in segment
        List<Long> playerIds = playerSegmentDao.selectPlayerIdsInSegment(tenantId, segmentId);

        log.info("Sending campaign to {} players in segment {}", playerIds.size(), segmentId);

        // Send notifications in batches (1000 per batch)
        for (List<Long> batch : Lists.partition(playerIds, 1000)) {
            batch.parallelStream().forEach(playerId -> {
                try {
                    notificationManager.sendNotification(
                        playerId,
                        templateCode,
                        NotificationChannel.PUSH,
                        Map.of("segment", segmentId)
                    );
                } catch (Exception e) {
                    log.error("Failed to send notification to player={}", playerId, e);
                }
            });
        }
    }
}
```

---

## 10. SmartAdmin Implementation

### 10.1 Controller Layer

```java
@RestController
@RequestMapping("/segment")
@RequiredArgsConstructor
@Tag(name = "Player Segmentation")
public class PlayerSegmentController {

    private final PlayerSegmentService segmentService;

    @GetMapping("/player/{playerId}")
    @SaCheckPermission("segment:player:view")
    public ResponseDTO<List<String>> getPlayerSegments(@PathVariable Long playerId) {
        List<String> segments = segmentService.getPlayerSegments(playerId);
        return ResponseDTO.ok(segments);
    }

    @GetMapping("/{segmentId}/players")
    @SaCheckPermission("segment:view")
    public ResponseDTO<PageResult<PlayerVO>> getPlayersInSegment(
            @PathVariable String segmentId,
            @Valid SegmentPlayerQueryForm form) {
        PageResult<PlayerVO> players = segmentService.getPlayersInSegment(segmentId, form);
        return ResponseDTO.ok(players);
    }

    @PostMapping("/calculate-rfm")
    @SaCheckPermission("segment:manage")
    public ResponseDTO<Void> recalculateRfmSegments() {
        segmentService.recalculateRfmSegments();
        return ResponseDTO.ok();
    }
}
```

---

## 11. Testing Strategy

### 11.1 Segment Assignment Tests

```java
@SpringBootTest
class PlayerSegmentServiceTest {

    @Autowired
    private PlayerSegmentService segmentService;

    @Test
    void testRfmSegmentCalculation() {
        Long playerId = 12345L;

        // Mock player with: last activity 2 days ago, 15 sessions, $600 deposits
        // Expected RFM: 4 (recency) + 4 (frequency) + 4 (monetary) = "444"

        String rfmSegment = segmentService.calculateRfmSegment(playerId);

        assertThat(rfmSegment).isEqualTo("444");
    }

    @Test
    void testLifecycleTransition() {
        Long playerId = 67890L;

        // Player starts as NEW
        assertThat(segmentService.isPlayerInSegment(playerId, "lifecycle_new")).isTrue();

        // Simulate 8 days of inactivity
        clock.advance(Duration.ofDays(8));

        // Should transition to AT_RISK
        assertThat(segmentService.isPlayerInSegment(playerId, "lifecycle_at_risk")).isTrue();
        assertThat(segmentService.isPlayerInSegment(playerId, "lifecycle_new")).isFalse();
    }
}
```

---

## 12. Operations & Monitoring

### 12.1 Metrics

```promql
# Segment distribution
sum by (segment_id) (player_segment_membership_total)

# Segment change rate
rate(player_segment_transitions_total[5m])

# Churn rate by segment
sum(player_lifecycle_churned_total) / sum(player_lifecycle_active_total) by (segment_id)

# Campaign conversion by segment
sum(campaign_conversions_total) / sum(campaign_impressions_total) by (segment_id)
```

### 12.2 Alerts

```yaml
- alert: HighChurnRate
  expr: sum(player_lifecycle_churned_total[7d]) / sum(player_lifecycle_active_total[7d]) > 0.20
  annotations:
    summary: "Churn rate exceeded 20% in last 7 days"

- alert: SegmentDriftDetected
  expr: abs(player_segment_membership_total - player_segment_membership_total offset 7d) > 1000
  annotations:
    summary: "Segment {{ $labels.segment_id }} membership changed by >1000 players"
```

---

## 13. Appendices

### 13.1 RFM Segment Action Matrix

| RFM Score | Segment Name | Action |
|-----------|--------------|--------|
| 555, 554, 544 | Champions | Exclusive rewards, early access |
| 543, 444, 435 | Loyal | Loyalty bonuses, referral |
| 525, 524, 523 | Potential Loyalists | Engagement campaigns |
| 512, 511 | Recent Customers | Onboarding, tutorials |
| 244, 334, 343 | At-Risk | Win-back offers |
| 111, 112, 121 | Churned | Reactivation (last attempt) |

### 13.2 Cross-References

- [P2-21: A/B Testing Framework](./21-ab-testing-framework.md) - Targeted experiments
- [P1-12: Bonus Engine](../P1-important/12-bonus-engine.md) - Personalized bonus offers
- [P1-11: VIP System Design](../P1-important/11-vip-system-design.md) - VIP tier progression
- [P2-17: Notification System](./17-notification-system.md) - Segmented campaigns

---

**Document End**
