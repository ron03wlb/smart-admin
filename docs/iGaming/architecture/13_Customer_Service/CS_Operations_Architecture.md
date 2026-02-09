# Customer Service Operations Architecture (客服營運技術架構)

> **Business Requirements**: [CS_Operations_Requirements.md](../../requirements/13_Customer_Service/CS_Operations_Requirements.md)
> **Canonical Source**: [13-02_Customer_Service_Operations.md](../../source-archive/13_Customer_Service/13-02_Customer_Service_Operations.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers

---

## 1. Database Schema

### 1.1 Agent Entity Table

```sql
CREATE TABLE t_cs_agent (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id            BIGINT NOT NULL UNIQUE,
    agent_name          VARCHAR(100) NOT NULL,
    agent_level         VARCHAR(20) NOT NULL,  -- JUNIOR, REGULAR, SENIOR, TEAM_LEAD, MANAGER

    -- Language capabilities
    native_language     VARCHAR(10) NOT NULL,
    fluent_languages    VARCHAR(100),          -- Comma-separated: en,zh-TW,th
    basic_languages     VARCHAR(100),

    -- Skills
    skill_tags          VARCHAR(200),          -- Comma-separated: payment_expert,game_integration

    -- Status
    online_status       VARCHAR(20) DEFAULT 'OFFLINE',  -- ONLINE, BUSY, AWAY, OFFLINE
    current_ticket_count INT DEFAULT 0,
    max_ticket_count    INT DEFAULT 15,

    -- Performance
    fcr_rate            DECIMAL(5,2),
    csat_score          DECIMAL(3,2),
    aht_seconds         INT,

    -- Shift
    shift_start         TIME,
    shift_end           TIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_online_status (online_status),
    INDEX idx_agent_level (agent_level)
);
```

### 1.2 SLA Configuration Table

```sql
CREATE TABLE t_sla_config (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_priority     VARCHAR(20) NOT NULL,  -- URGENT, HIGH, MEDIUM, LOW
    vip_level           VARCHAR(20),           -- DIAMOND, PLATINUM, GOLD, SILVER, BRONZE

    -- SLA thresholds (minutes)
    first_response_minutes  INT NOT NULL,
    resolution_minutes      INT NOT NULL,

    -- Escalation rules
    escalation_50_target    VARCHAR(50),  -- team_lead, senior, manager
    escalation_75_target    VARCHAR(50),
    escalation_100_target   VARCHAR(50),

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_priority_vip (ticket_priority, vip_level)
);
```

### 1.3 SLA Tracking Table

```sql
CREATE TABLE t_sla_tracking (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id           BIGINT NOT NULL,
    sla_type            VARCHAR(20) NOT NULL,  -- FIRST_RESPONSE, RESOLUTION

    -- SLA targets
    sla_deadline        DATETIME NOT NULL,

    -- Actual performance
    met                 BOOLEAN,
    actual_time         DATETIME,

    -- Escalation
    escalated           BOOLEAN DEFAULT FALSE,
    escalation_level    INT DEFAULT 0,
    escalated_to        VARCHAR(100),
    escalated_at        DATETIME,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_ticket_id (ticket_id),
    INDEX idx_sla_deadline (sla_deadline),
    INDEX idx_met (met)
);
```

### 1.4 Chat Session Table

```sql
CREATE TABLE t_chat_session (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id           BIGINT NOT NULL,
    player_id           BIGINT NOT NULL,
    agent_id            BIGINT,

    -- Channel
    channel             VARCHAR(20) NOT NULL,  -- LIVE_CHAT, TELEGRAM, WHATSAPP, EMAIL

    -- Status
    status              VARCHAR(20) NOT NULL,  -- ACTIVE, TRANSFERRED, CLOSED

    -- Timing
    started_at          DATETIME NOT NULL,
    ended_at            DATETIME,
    first_response_at   DATETIME,

    -- Stats
    message_count       INT DEFAULT 0,
    agent_message_count INT DEFAULT 0,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_ticket_id (ticket_id),
    INDEX idx_player_id (player_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_channel (channel)
);
```

### 1.5 Chat Message Table

```sql
CREATE TABLE t_chat_message (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_session_id     BIGINT NOT NULL,
    sender_type         VARCHAR(20) NOT NULL,  -- PLAYER, AGENT, BOT
    sender_id           BIGINT NOT NULL,

    -- Content
    message_type        VARCHAR(20) NOT NULL,  -- TEXT, IMAGE, FILE, SYSTEM
    content             TEXT NOT NULL,
    file_url            VARCHAR(500),

    -- AI analysis
    detected_intent     VARCHAR(50),
    intent_confidence   DECIMAL(3,2),
    sentiment           VARCHAR(20),  -- POSITIVE, NEUTRAL, NEGATIVE

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_chat_session_id (chat_session_id),
    INDEX idx_created_at (created_at)
);
```

### 1.6 Agent Performance Table

```sql
CREATE TABLE t_agent_daily_performance (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id            BIGINT NOT NULL,
    performance_date    DATE NOT NULL,

    -- Volume
    tickets_handled     INT DEFAULT 0,
    chats_handled       INT DEFAULT 0,

    -- Quality
    fcr_count           INT DEFAULT 0,
    total_resolved      INT DEFAULT 0,
    csat_sum            DECIMAL(10,2) DEFAULT 0,
    csat_count          INT DEFAULT 0,

    -- Timing
    total_handle_time_seconds   INT DEFAULT 0,
    total_first_response_seconds INT DEFAULT 0,

    -- SLA
    sla_met_count       INT DEFAULT 0,
    sla_breached_count  INT DEFAULT 0,

    -- Reopens
    reopen_count        INT DEFAULT 0,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_agent_date (agent_id, performance_date),
    INDEX idx_performance_date (performance_date)
);
```

---

## 2. Ticket Routing Service

### 2.1 Weighted Round-Robin Algorithm

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingService {

    private final AgentDao agentDao;
    private final PlayerDao playerDao;
    private final VipConfigDao vipConfigDao;

    /**
     * Find best agent for a ticket based on:
     * 1. VIP priority routing
     * 2. Skill matching
     * 3. Language matching
     * 4. Workload balancing
     */
    public Option<AgentEntity> findBestAgent(TicketEntity ticket) {
        PlayerEntity player = playerDao.selectById(ticket.getPlayerId());

        // Step 1: VIP priority routing
        if (isVipPriorityRoute(player.getVipLevel())) {
            Option<AgentEntity> vipAgent = findVipManager(player.getVipLevel());
            if (vipAgent.isDefined()) return vipAgent;
        }

        // Step 2: Get skill-matched agents
        String requiredSkill = getRequiredSkill(ticket.getType());
        List<AgentEntity> candidates = agentDao.findAvailableAgentsBySkill(requiredSkill);

        if (candidates.isEmpty()) {
            candidates = agentDao.findAvailableAgents();
        }

        // Step 3: Calculate weighted score for each agent
        return Option.ofOptional(candidates.stream()
            .filter(a -> a.getCurrentTicketCount() < a.getMaxTicketCount())
            .map(agent -> {
                double weight = calculateWeight(agent, player);
                return Map.entry(agent, weight);
            })
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey));
    }

    private double calculateWeight(AgentEntity agent, PlayerEntity player) {
        double levelWeight = switch (agent.getAgentLevel()) {
            case "SENIOR" -> 1.5;
            case "REGULAR" -> 1.0;
            case "JUNIOR" -> 0.7;
            default -> 1.0;
        };

        double languageScore = getLanguageMatchScore(agent, player.getLanguage());
        double loadInverse = 1.0 / (agent.getCurrentTicketCount() + 1);

        return levelWeight * languageScore * loadInverse;
    }

    private String getRequiredSkill(TicketType type) {
        return switch (type) {
            case DEPOSIT_ISSUE, WITHDRAWAL_DELAY -> "payment_expert";
            case GAME_LAG, BET_DISPUTE -> "game_integration";
            case BONUS_NOT_CREDITED, WAGERING_QUERY -> "bonus_specialist";
            case SERVICE_COMPLAINT, FRAUD_ACCUSATION -> "compliance_officer";
            default -> null;
        };
    }
}
```

---

## 3. SLA Automated Monitoring

### 3.1 SLA Monitor Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorService {

    private final TicketDao ticketDao;
    private final SlaTrackingDao slaTrackingDao;
    private final SlaConfigDao slaConfigDao;
    private final NotificationService notificationService;

    /**
     * Scheduled: check SLA compliance every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void checkSlaCompliance() {
        List<SlaTracking> openTrackings = slaTrackingDao.findPendingSla();

        for (SlaTracking tracking : openTrackings) {
            Duration remaining = Duration.between(
                LocalDateTime.now(), tracking.getSlaDeadline());

            double percentageUsed = calculatePercentageUsed(tracking);

            if (remaining.isNegative()) {
                // SLA breached
                handleSlaBreach(tracking);
            } else if (percentageUsed >= 75) {
                escalate(tracking, 2);
            } else if (percentageUsed >= 50) {
                escalate(tracking, 1);
            }
        }
    }

    private void handleSlaBreach(SlaTracking tracking) {
        tracking.setMet(false);
        slaTrackingDao.updateById(tracking);

        TicketEntity ticket = ticketDao.selectById(tracking.getTicketId());

        // Level 2 escalation
        escalate(tracking, 2);

        // Send breach alert
        notificationService.sendSlaBreachAlert(ticket, tracking);

        log.warn("SLA breached: ticketId={}, type={}", ticket.getTicketId(), tracking.getSlaType());
    }

    private void escalate(SlaTracking tracking, int level) {
        if (tracking.getEscalationLevel() >= level) return;

        SlaConfig config = slaConfigDao.findByTicket(tracking.getTicketId());
        String target = switch (level) {
            case 1 -> config.getEscalation50Target();
            case 2 -> config.getEscalation75Target();
            case 3 -> config.getEscalation100Target();
            default -> "team_lead";
        };

        tracking.setEscalated(true);
        tracking.setEscalationLevel(level);
        tracking.setEscalatedTo(target);
        tracking.setEscalatedAt(LocalDateTime.now());
        slaTrackingDao.updateById(tracking);

        notificationService.sendEscalationNotification(tracking, target);
    }
}
```

---

## 4. Multi-Channel Message Bus

### 4.1 Channel Adapter Architecture

```mermaid
flowchart TD
    A[Player] -->|Live Chat| B[WebSocket Handler]
    A -->|Telegram| C[Telegram Bot API]
    A -->|WhatsApp| D[WhatsApp Business API]
    A -->|Email| E[Email Receiver]

    B --> F[Channel Message Adapter]
    C --> F
    D --> F
    E --> F

    F --> G[Unified Message Bus]

    G --> H{AI Classification}

    H -->|Auto-resolve| I[Auto Reply Engine]
    H -->|Need human| J[Ticket Router]

    J --> K[Agent Assignment]
    K --> L[Agent Desktop]

    L -->|Reply| M[Channel Message Adapter]
    M --> A
```

### 4.2 Unified Message Format

```java
@Data
@Builder
public class UnifiedMessage {
    private Long messageId;
    private Long playerId;
    private Long ticketId;
    private String channel;           // LIVE_CHAT, TELEGRAM, WHATSAPP, EMAIL
    private String senderType;        // PLAYER, AGENT, BOT
    private Long senderId;
    private String messageType;       // TEXT, IMAGE, FILE, SYSTEM
    private String content;
    private String fileUrl;
    private LocalDateTime timestamp;

    // AI enrichment
    private String detectedIntent;
    private Double intentConfidence;
    private String sentiment;
}
```

---

## 5. Performance Analytics Pipeline

### 5.1 Metrics Calculation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPerformanceService {

    private final AgentDailyPerformanceDao performanceDao;
    private final TicketDao ticketDao;

    /**
     * Calculate daily performance for all agents
     * Runs at end of day
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateDailyPerformance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Long> agentIds = agentDao.findAllAgentIds();

        for (Long agentId : agentIds) {
            List<TicketEntity> tickets = ticketDao.findResolvedByAgentAndDate(agentId, yesterday);

            int totalResolved = tickets.size();
            int fcrCount = (int) tickets.stream()
                .filter(t -> t.getResolvedOnFirstContact())
                .count();

            double avgHandleTime = tickets.stream()
                .mapToLong(t -> Duration.between(t.getAssignedAt(), t.getResolvedTime()).getSeconds())
                .average()
                .orElse(0);

            AgentDailyPerformance perf = AgentDailyPerformance.builder()
                .agentId(agentId)
                .performanceDate(yesterday)
                .ticketsHandled(totalResolved)
                .fcrCount(fcrCount)
                .totalResolved(totalResolved)
                .totalHandleTimeSeconds((int) avgHandleTime * totalResolved)
                .build();

            performanceDao.saveOrUpdate(perf);
        }
    }

    /**
     * Get weekly performance ranking
     */
    public List<AgentRankingVO> getWeeklyRanking() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        return performanceDao.getAggregatePerfByDateRange(weekStart, weekEnd).stream()
            .map(perf -> AgentRankingVO.builder()
                .agentId(perf.getAgentId())
                .fcrRate(perf.getFcrCount() * 100.0 / perf.getTotalResolved())
                .csatScore(perf.getCsatSum() / perf.getCsatCount())
                .ticketVolume(perf.getTicketsHandled())
                .build())
            .sorted(Comparator.comparingDouble(AgentRankingVO::getFcrRate).reversed())
            .collect(Collectors.toList());
    }
}
```

---

## 6. Quality Assurance Automation

```java
@Service
@RequiredArgsConstructor
public class QualityAssuranceService {

    private final TicketDao ticketDao;
    private final QaReviewDao qaReviewDao;

    /**
     * Select 10% of tickets for daily QA review
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void selectTicketsForReview() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<TicketEntity> resolvedTickets = ticketDao.findResolvedByDate(yesterday);

        int sampleSize = Math.max(1, resolvedTickets.size() / 10);
        Collections.shuffle(resolvedTickets);

        List<TicketEntity> selectedTickets = resolvedTickets.subList(0, sampleSize);

        for (TicketEntity ticket : selectedTickets) {
            QaReview review = QaReview.builder()
                .ticketId(ticket.getTicketId())
                .agentId(ticket.getAssignedAgentId())
                .reviewDate(yesterday)
                .status("PENDING")
                .build();
            qaReviewDao.insert(review);
        }

        log.info("QA review: selected {} tickets from {} total",
            sampleSize, resolvedTickets.size());
    }
}
```

---

## 7. Monitoring

| Metric | Prometheus Name | Description |
|--------|----------------|-------------|
| Open tickets by priority | `cs_open_tickets_gauge{priority}` | Real-time count |
| SLA achievement rate | `cs_sla_achievement_rate` | Rolling 24h |
| First response time avg | `cs_first_response_seconds_avg` | Average |
| FCR rate | `cs_fcr_rate` | First Contact Resolution |
| CSAT score | `cs_csat_score_avg` | Average satisfaction |
| AI automation rate | `cs_ai_automation_rate` | Auto-resolved percentage |
| Agent utilization | `cs_agent_utilization_rate` | Busy/Total time |
| Ticket reopen rate | `cs_ticket_reopen_rate` | Quality indicator |

---

## Related Documents

- [CS_Operations_Requirements.md](../../requirements/13_Customer_Service/CS_Operations_Requirements.md) - Business requirements
- [CS_Platform_Architecture.md](CS_Platform_Architecture.md) - Platform architecture

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
