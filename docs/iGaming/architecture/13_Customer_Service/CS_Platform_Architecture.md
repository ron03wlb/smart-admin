# Customer Service Platform Architecture (客服平台技術架構)

> **Business Requirements**: [CS_Platform_Requirements.md](../../requirements/13_Customer_Service/CS_Platform_Requirements.md)
> **Canonical Source**: [13-01_CS_Platform_Design.md](../../source-archive/13_Customer_Service/13-01_CS_Platform_Design.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers

---

## 1. SmartAdmin Architecture Mapping

### 1.1 Entity Layer

```java
@Entity
@Table(name = "t_customer_service_ticket")
@Data
public class TicketEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    private Long playerId;

    @Enumerated(EnumType.STRING)
    private TicketType type; // DEPOSIT_ISSUE, WITHDRAWAL_QUERY, etc.

    @Enumerated(EnumType.STRING)
    private TicketPriority priority; // URGENT, HIGH, MEDIUM, LOW

    @Enumerated(EnumType.STRING)
    private TicketStatus status; // NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED

    private Long assignedAgentId;

    private String subject;

    private String description;

    private LocalDateTime firstResponseTime;

    private LocalDateTime resolvedTime;

    private Boolean deleted;
}
```

### 1.2 Manager Layer (Transaction Management)

```java
@Manager
@RequiredArgsConstructor
public class TicketAssignmentManager {
    private final TicketDao ticketDao;
    private final AgentDao agentDao;
    private final RedissonClient redissonClient;

    /**
     * Assign ticket to best available agent using distributed lock
     * to prevent concurrent assignment
     */
    @Transactional(rollbackFor = Throwable.class)
    public Long assignTicketToAgent(Long ticketId, Long playerId) {
        // Lock to prevent concurrent assignment
        RLock lock = redissonClient.getLock("ticket:assign:" + ticketId);
        try {
            lock.lock(10, TimeUnit.SECONDS);

            // Find best agent using weighted round-robin
            AgentEntity agent = findBestAvailableAgent(playerId);
            if (agent == null) {
                throw new BusinessException("No available agents");
            }

            // Assign ticket
            TicketEntity ticket = ticketDao.selectById(ticketId);
            ticket.setAssignedAgentId(agent.getAgentId());
            ticket.setStatus(TicketStatus.ASSIGNED);
            ticketDao.updateById(ticket);

            return agent.getAgentId();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Weighted round-robin agent selection algorithm
     *
     * Weight = (Agent Level Weight) x (Language Match Score) x (Current Ticket Load Inverse)
     */
    private AgentEntity findBestAvailableAgent(Long playerId) {
        List<AgentEntity> availableAgents = agentDao.findAvailableAgents();
        PlayerEntity player = playerDao.selectById(playerId);

        return availableAgents.stream()
            .map(agent -> {
                double levelWeight = getAgentLevelWeight(agent.getLevel());
                double languageScore = getLanguageMatchScore(agent, player.getLanguage());
                double loadInverse = 1.0 / (agent.getCurrentTicketCount() + 1);
                double totalWeight = levelWeight * languageScore * loadInverse;
                return Map.entry(agent, totalWeight);
            })
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private double getAgentLevelWeight(AgentLevel level) {
        return switch (level) {
            case SENIOR -> 1.5;
            case REGULAR -> 1.0;
            case JUNIOR -> 0.7;
        };
    }

    private double getLanguageMatchScore(AgentEntity agent, String playerLanguage) {
        if (agent.getNativeLanguage().equals(playerLanguage)) return 1.0;
        if (agent.getFluentLanguages().contains(playerLanguage)) return 0.8;
        if (agent.getBasicLanguages().contains(playerLanguage)) return 0.5;
        return 0.3;
    }
}
```

### 1.3 Service Layer (Vavr Option)

```java
@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketDao ticketDao;
    private final TicketAssignmentManager ticketAssignmentManager;

    /**
     * Create a new ticket with auto-assignment
     * Uses Vavr Option for safe null handling
     */
    public Option<TicketVO> createTicket(TicketCreateForm form) {
        return Try.of(() -> {
            // Validate form
            if (form.getPlayerId() == null || form.getSubject() == null) {
                throw new BusinessException("Invalid ticket form");
            }

            // Create ticket entity
            TicketEntity ticket = new TicketEntity();
            ticket.setPlayerId(form.getPlayerId());
            ticket.setType(form.getType());
            ticket.setPriority(calculatePriority(form));
            ticket.setStatus(TicketStatus.NEW);
            ticket.setSubject(form.getSubject());
            ticket.setDescription(form.getDescription());

            ticketDao.insert(ticket);

            // Auto-assign to agent
            ticketAssignmentManager.assignTicketToAgent(
                ticket.getTicketId(), form.getPlayerId());

            return SmartBeanUtil.copy(ticket, TicketVO.class);
        }).toOption();
    }

    /**
     * Calculate priority based on ticket type and player VIP level
     */
    private TicketPriority calculatePriority(TicketCreateForm form) {
        // VIP Diamond with deposit issue -> URGENT
        if (form.getVipLevel() == VipLevel.DIAMOND &&
            form.getType() == TicketType.DEPOSIT_ISSUE) {
            return TicketPriority.URGENT;
        }

        return switch (form.getType()) {
            case DEPOSIT_ISSUE, WITHDRAWAL_DELAY -> TicketPriority.URGENT;
            case BET_DISPUTE, LOGIN_ISSUE -> TicketPriority.HIGH;
            case GAME_LAG, BONUS_NOT_CREDITED -> TicketPriority.MEDIUM;
            case WAGERING_QUERY -> TicketPriority.LOW;
            default -> TicketPriority.MEDIUM;
        };
    }
}
```

---

## 2. Player 360-Degree View Data Integration

### 2.1 Data Source Architecture

```mermaid
flowchart TD
    A[CS Agent Opens Player Profile] --> B[Player 360 View Controller]

    B --> C[Player Service<br/>Basic info, KYC]
    B --> D[Wallet Service<br/>Balance, Transactions]
    B --> E[Bet Service<br/>History, Preferences]
    B --> F[Segmentation Service<br/>RFM, Lifecycle]
    B --> G[Risk Service<br/>Score, Tags]
    B --> H[Ticket Service<br/>History, Complaints]

    C --> I[Redis Cache<br/>5-min TTL]
    D --> I
    E --> I
    F --> I
    G --> I
    H --> I

    I --> J[Unified Player360VO]
    J --> K[CS Frontend Display]
```

### 2.2 Response Time Optimization

- **Redis cache**: 5-minute TTL for player profile data
- **Async loading**: Non-critical data (bet history, ticket history) loaded asynchronously
- **GraphQL**: On-demand query to avoid over-fetching

---

## 3. Knowledge Base Search Architecture

### 3.1 Elasticsearch Integration

```mermaid
flowchart LR
    A[CMS Backend] -->|Publish| B[Knowledge Article]
    B -->|Index| C[Elasticsearch]
    C -->|Search| D[Player Self-Service]
    C -->|Search| E[CS Agent Search]
    C -->|AI Match| F[Chatbot FAQ Match]

    G[Search Quality Tracker] -->|Click Rate| C
    G -->|Zero Results| H[Content Gap Report]
```

### 3.2 Search Configuration

```
Elasticsearch Index: knowledge_base
- Full-text search (synonym support, fuzzy matching)
- Autocomplete suggestions
- Search result highlighting
- Click-through rate tracking (ranking optimization)
```

**Quality Metrics**:
- Search success rate (found answer + closed ticket): > 70%
- Average search time: < 5 seconds
- Zero-result search rate: < 5%

---

## 4. AI Chatbot Architecture

### 4.1 NLU Pipeline

```mermaid
flowchart TD
    A[Player Message] --> B[NLU Engine]

    B --> C{Intent Classification<br/>13 categories}

    C -->|Confidence >= 0.8| D[Auto-Reply Engine]
    C -->|Confidence 0.7-0.8| E[Suggest + Confirm]
    C -->|Confidence < 0.7| F[Transfer to Human]

    D --> G[FAQ Match Rules]
    D --> H[Dynamic Data Query]
    D --> I[Operation Commands]

    G -->|Match > 85%| J[Return FAQ Article]
    H -->|Balance/Status| K[Query API + Reply]
    I -->|Password Reset| L[Execute Action]

    F --> M[Create Ticket + Attach History]
    M --> N[Assign to Agent]
    N --> O[Agent Takeover]
```

### 4.2 Auto-Transfer Conditions

```java
public boolean shouldTransferToHuman(ChatContext context) {
    // 1. Low confidence
    if (context.getIntentConfidence() < 0.7) return true;

    // 2. Explicit request
    if (containsHumanRequest(context.getMessage())) return true;

    // 3. Sensitive issue
    if (isSensitiveIntent(context.getIntent())) return true;

    // 4. Failed auto-replies
    if (context.getConsecutiveAutoReplyCount() >= 3) return true;

    // 5. VIP Diamond
    if (context.getPlayerVipLevel() == VipLevel.DIAMOND) return true;

    return false;
}
```

### 4.3 Training Data Pipeline

```
Historical Tickets (100K+) -> Agent Annotations -> Weekly Model Retraining -> Deploy
                                    |
                            FAQ Click Rates -> Ranking Optimization
```

---

## 5. SLA Monitoring Architecture

### 5.1 SLA Timer Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitorService {

    private final TicketDao ticketDao;
    private final SlaConfigDao slaConfigDao;
    private final NotificationService notificationService;

    /**
     * Scheduled task: check SLA compliance every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void checkSlaCompliance() {
        List<TicketEntity> openTickets = ticketDao.findOpenTickets();

        for (TicketEntity ticket : openTickets) {
            SlaConfig sla = slaConfigDao.findByPriorityAndVipLevel(
                ticket.getPriority(), ticket.getPlayerVipLevel());

            Duration elapsed = Duration.between(ticket.getCreatedAt(), LocalDateTime.now());
            Duration slaLimit = Duration.ofMinutes(sla.getFirstResponseMinutes());

            double percentageUsed = (double) elapsed.toMinutes() / sla.getFirstResponseMinutes() * 100;

            if (percentageUsed >= 100) {
                // SLA breached
                escalateTicket(ticket, EscalationLevel.LEVEL_2);
                notificationService.sendSlaBreachAlert(ticket);
            } else if (percentageUsed >= 75) {
                // SLA warning
                notificationService.sendSlaWarning(ticket, EscalationLevel.LEVEL_1);
            } else if (percentageUsed >= 50) {
                // SLA approaching
                notificationService.sendSlaApproaching(ticket);
            }
        }
    }
}
```

---

## 6. Multi-Channel Integration

### 6.1 WebSocket Live Chat

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LiveChatWebSocketHandler implements WebSocketHandler {

    private final Map<Long, WebSocketSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        String role = extractRole(session);

        if ("PLAYER".equals(role)) {
            playerSessions.put(userId, session);
        } else if ("AGENT".equals(role)) {
            agentSessions.put(userId, session);
        }
    }

    /**
     * Route message from player to assigned agent
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        ChatMessage chatMessage = parseMessage(message);

        if (chatMessage.isFromPlayer()) {
            // Forward to assigned agent
            Long agentId = ticketService.getAssignedAgentId(chatMessage.getTicketId());
            WebSocketSession agentSession = agentSessions.get(agentId);
            if (agentSession != null && agentSession.isOpen()) {
                sendMessage(agentSession, chatMessage);
            }
        } else {
            // Forward to player
            Long playerId = chatMessage.getPlayerId();
            WebSocketSession playerSession = playerSessions.get(playerId);
            if (playerSession != null && playerSession.isOpen()) {
                sendMessage(playerSession, chatMessage);
            }
        }
    }
}
```

### 6.2 Context Passing on Chat Init

```javascript
// Frontend: pass player context when initiating chat
const livechat = new LiveChatClient({
  playerId: currentUser.id,
  playerVIP: currentUser.vipLevel,
  currentPage: window.location.pathname,
  walletBalance: currentUser.wallet.playableBalance,
  wageringProgress: currentUser.bonusWageringProgress
});
```

---

## 7. Foundation Module Dependencies

| Module | Purpose |
|--------|---------|
| `foundation.redis-lock` | Ticket assignment concurrency control |
| `foundation.websocket` | Live Chat real-time communication |
| `foundation.mq` | SLA alert event publishing |
| `foundation.cache` | Player 360-degree view caching |
| `foundation.elasticsearch` | Knowledge base full-text search |

---

## 8. Performance KPI Dashboard

```mermaid
flowchart TD
    A[Ticket Events] --> B[Event Stream]
    B --> C[Metrics Calculator]

    C --> D[FCR Calculator<br/>First Contact Resolution]
    C --> E[AHT Calculator<br/>Average Handle Time]
    C --> F[CSAT Calculator<br/>Customer Satisfaction]
    C --> G[SLA Calculator<br/>Compliance Rate]

    D --> H[Dashboard Display]
    E --> H
    F --> H
    G --> H

    H --> I[Agent Rankings]
    H --> J[Team Overview]
    H --> K[SLA Alerts]
```

---

## Related Documents

- [CS_Platform_Requirements.md](../../requirements/13_Customer_Service/CS_Platform_Requirements.md) - Business requirements
- [CS_Operations_Architecture.md](CS_Operations_Architecture.md) - Operations architecture

---

## 9. Database Schema

```sql
-- Customer service ticket table
CREATE TABLE t_customer_service_ticket (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL UNIQUE,
    player_id       BIGINT NOT NULL,
    type            VARCHAR(50) NOT NULL,
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    assigned_agent_id BIGINT,
    subject         VARCHAR(200) NOT NULL,
    description     TEXT,
    first_response_time TIMESTAMP,
    resolved_time   TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_player ON t_customer_service_ticket(player_id, status);
CREATE INDEX idx_ticket_agent ON t_customer_service_ticket(assigned_agent_id, status);
CREATE INDEX idx_ticket_priority ON t_customer_service_ticket(priority, status, created_at);

-- Agent information table
CREATE TABLE t_cs_agent (
    id              BIGSERIAL PRIMARY KEY,
    agent_id        BIGINT NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    level           VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    native_language VARCHAR(10) NOT NULL,
    fluent_languages JSONB,
    basic_languages JSONB,
    current_ticket_count INTEGER NOT NULL DEFAULT 0,
    max_concurrent_tickets INTEGER NOT NULL DEFAULT 10,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_status ON t_cs_agent(status, level);

-- SLA configuration table
CREATE TABLE t_sla_config (
    id              BIGSERIAL PRIMARY KEY,
    priority        VARCHAR(20) NOT NULL,
    vip_level       VARCHAR(20) NOT NULL,
    first_response_minutes INTEGER NOT NULL,
    resolution_minutes INTEGER NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sla_config UNIQUE (priority, vip_level)
);

-- Chat message history
CREATE TABLE t_chat_message (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    sender_type     VARCHAR(20) NOT NULL,
    sender_id       BIGINT NOT NULL,
    message_content TEXT NOT NULL,
    attachments     JSONB,
    sent_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_ticket ON t_chat_message(ticket_id, sent_at);

-- Knowledge base article
CREATE TABLE t_knowledge_article (
    id              BIGSERIAL PRIMARY KEY,
    article_id      BIGINT NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL,
    tags            JSONB,
    view_count      INTEGER NOT NULL DEFAULT 0,
    helpful_count   INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_article_category ON t_knowledge_article(category, status);
```

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
