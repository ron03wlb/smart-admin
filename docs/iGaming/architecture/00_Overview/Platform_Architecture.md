# iGaming Platform Architecture Overview

> **Canonical Source**: [00-06_Solution_Overview.md](../../source/00_Foundation/guides/00-06_Solution_Overview.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: [Solution_Overview.md](../../requirements/01_Player_Experience/Solution_Overview.md)
> **Last Synced**: 2026-02-08

---

## 1. Architecture Paradigm

Modern iGaming platforms have transitioned from monolithic to microservices architecture, the key technical choice for supporting **200,000+** concurrent users.

```mermaid
graph TB
    subgraph Client["Client Layer"]
        WEB[Web App - React]
        MOB[Mobile App - React Native]
        API_EXT[External API Consumers]
    end

    subgraph Edge["Edge Layer"]
        CDN[CDN<br/>Akamai / Continent 8]
        WAF[WAF + DDoS Protection]
        LB[Global DNS Load Balancer]
    end

    subgraph Gateway["API Gateway"]
        GW[API Gateway<br/>Rate Limiting, Auth, Routing]
    end

    subgraph Services["Microservices Layer"]
        PAM[Player Account<br/>Management]
        GAME[Game Aggregation<br/>Service]
        SPORTS[Sportsbook<br/>Engine]
        PAY[Payment<br/>Orchestration]
        CRM[CRM &<br/>Analytics]
        BONUS[Bonus<br/>Engine]
        RISK[Risk &<br/>Fraud Detection]
    end

    subgraph Data["Data Layer"]
        PG[(PostgreSQL<br/>Financial Txns)]
        REDIS[(Redis<br/>Sessions, Leaderboards)]
        MONGO[(MongoDB<br/>Activity Logs, Analytics)]
        KAFKA[Apache Kafka<br/>Event Streaming]
    end

    Client --> Edge --> Gateway --> Services
    Services --> Data
    PAM --> PG
    PAM --> REDIS
    GAME --> KAFKA
    PAY --> PG
    CRM --> MONGO
    RISK --> REDIS
```

---

## 2. Multi-Tenant Database Isolation Strategies

Database architecture directly impacts security, cost, and operational efficiency. The industry employs three primary patterns:

| Pattern | Description | Isolation Level | Cost | Best For |
|---|---|---|---|---|
| **Shared Database & Schema** | Single DB, `tenant_id` column discriminator | Low | Lowest | Small operators, cost-sensitive |
| **Shared Database, Separate Schema** | One DB, each tenant gets own schema | Medium | Moderate | Mid-size platforms |
| **Fully Separate Database** | Dedicated DB per tenant | Highest | Highest | High-end clients, strict regulatory |

### Recommended Polyglot Persistence Stack

```mermaid
graph LR
    subgraph Transactional["OLTP - ACID"]
        PG[PostgreSQL<br/>Financial Transactions<br/>Row-Level Security]
    end

    subgraph Cache["In-Memory"]
        REDIS[Redis<br/>Sessions<br/>Leaderboards<br/>Real-time State]
    end

    subgraph Analytics["Document Store"]
        MONGO[MongoDB<br/>Player Activity Logs<br/>Analytics Data]
    end

    subgraph Streaming["Event Streaming"]
        KAFKA[Apache Kafka<br/>Event Bus<br/>Millions of Concurrent Connections]
    end

    PG -.-> KAFKA
    REDIS -.-> KAFKA
    MONGO -.-> KAFKA
```

**PostgreSQL Row-Level Security (RLS)** is the standard implementation for tenant isolation:

```sql
-- Enable RLS on tenant-scoped table
ALTER TABLE player_wallet ENABLE ROW LEVEL SECURITY;

-- Create policy: each tenant only sees own data
CREATE POLICY tenant_isolation ON player_wallet
    USING (tenant_id = current_setting('app.current_tenant')::bigint);

-- Set tenant context per request
SET app.current_tenant = '12345';
SELECT * FROM player_wallet; -- Only returns tenant 12345 data
```

---

## 3. Technology Stack Selection

### Backend

| Technology | Use Case | Strengths |
|---|---|---|
| **Java / Spring Boot** | Enterprise core services | High availability, mature ecosystem, strong typing |
| **Node.js** | WebSocket services, real-time features | Event-driven, optimal for WebSocket |
| **Go** | High-performance microservices | Efficient concurrency, low memory footprint |

### Frontend

| Technology | Use Case |
|---|---|
| **React** | Complex UI development (dominant) |
| **Angular** | Enterprise management applications |

### Real-Time Communication

WebSocket protocol ensures:
- Sports betting odds updates: **< 500ms** latency
- Live dealer games: **< 100ms** latency

Combined with **Apache Kafka** for supporting millions of concurrent connections (reference: Disney+ Hotstar handled **25.3 million** simultaneous viewers with Kafka-based architecture).

---

## 4. Cloud Deployment and High Availability

### Container Orchestration

**AWS EKS** is the industry-preferred container orchestration platform, with multi-availability-zone deployment for high availability.

```mermaid
graph TB
    subgraph Global["Global Traffic Management"]
        DNS[DNS-Level Global<br/>Load Balancing]
    end

    subgraph EU["Europe Cluster"]
        EU_EKS[AWS EKS<br/>Multi-AZ]
        EU_DB[(PostgreSQL<br/>Primary)]
    end

    subgraph US["Americas Cluster"]
        US_EKS[AWS EKS<br/>Multi-AZ]
        US_DB[(PostgreSQL<br/>Primary)]
    end

    subgraph AP["Asia Cluster"]
        AP_EKS[AWS EKS<br/>Multi-AZ]
        AP_DB[(PostgreSQL<br/>Primary)]
    end

    DNS --> EU_EKS
    DNS --> US_EKS
    DNS --> AP_EKS

    EU_DB <-.->|Cross-Region<br/>Replication| US_DB
    US_DB <-.->|Cross-Region<br/>Replication| AP_DB
```

### Active-Active Architecture

Leading platforms deploy independent clusters across Europe, Americas, and Asia with DNS-level global load balancing.

**Disaster Recovery Targets**:

| Metric | Target |
|---|---|
| RTO (Recovery Time Objective) | 5-15 minutes |
| RPO (Recovery Point Objective) | Near-zero |

### CDN and Edge Computing

| Provider | Specialization |
|---|---|
| **Akamai** | iGaming-specific solutions with DDoS protection |
| **Continent 8** | Private network designed exclusively for gaming industry |

Edge computing enables bet processing logic closer to users while satisfying data localization regulatory requirements.

---

## 5. Wallet Integration Architecture

### Seamless Wallet (Industry Standard)

```mermaid
sequenceDiagram
    participant P as Player
    participant OP as Operator Platform
    participant GP as Game Provider

    P->>OP: Launch Game
    OP->>GP: Authenticate Player + Session Token
    GP->>OP: GET /balance (real-time)
    OP-->>GP: {balance: 1000.00}
    P->>GP: Place Bet ($10)
    GP->>OP: POST /debit {amount: 10.00, txId: "abc123"}
    OP-->>GP: {balance: 990.00, status: "OK"}
    Note over GP: Game Round Resolves
    GP->>OP: POST /credit {amount: 25.00, txId: "abc124"}
    OP-->>GP: {balance: 1015.00, status: "OK"}
```

**Characteristics**:
- Player balance remains on operator platform
- Real-time processing per bet/win
- Supports multi-game simultaneous play
- Integration time: ~10 days

### Transfer Wallet (Legacy)

```mermaid
sequenceDiagram
    participant P as Player
    participant OP as Operator Platform
    participant GP as Game Provider

    P->>OP: Launch Game
    OP->>OP: Transfer $500 to Provider Wallet
    OP->>GP: {providerBalance: 500.00}
    Note over GP: Multiple Game Rounds
    GP->>OP: Session End - Transfer Back
    OP->>OP: Credit remaining balance
```

**Characteristics**:
- Funds transferred to provider-specific wallet
- Simpler integration (~2 days)
- Poorer player experience
- Risk of fund isolation on disconnect

---

## 6. Three-Party Reconciliation Architecture

```mermaid
flowchart TB
    subgraph L1["L1: Real-Time"]
        CB[Game Provider Callback] --> LOCAL_DB[(Local DB)]
    end

    subgraph L2["L2: Compensatory (Every 5 min)"]
        POLL[API Poll<br/>GetTransactionHistory] --> COMPARE[Compare with<br/>Local DB]
        COMPARE --> PATCH[Patch Missing<br/>Transactions]
    end

    subgraph L3["L3: Daily Settlement"]
        IMPORT[Import Provider<br/>Settlement Report] --> DIFF[Final Difference<br/>Comparison]
        DIFF --> REPORT[Discrepancy Report<br/>for Manual Reconciliation]
    end

    L1 --> L2 --> L3
```

### Circuit Breaker Mechanism

```mermaid
flowchart LR
    MONITOR[RTP Monitor] --> CHECK{1hr RTP > 200%<br/>AND<br/>Bet Volume > $10K?}
    CHECK -->|Yes| SUSPEND[Suspend Game Entry<br/>for Tenant/Provider]
    CHECK -->|No| CONTINUE[Continue Normal<br/>Operations]
    SUSPEND --> ALERT[Send Alert to<br/>Operations Team]
```

**Trigger Conditions**: When a single tenant or game provider's **RTP** exceeds threshold within a short window (e.g., 1-hour RTP > 200% with bet volume > $10,000), the system automatically suspends the game entry and dispatches alerts.

---

## 7. SaaS Tenant Billing Architecture

### Dynamic Cost Allocation

```mermaid
graph LR
    subgraph Resources["Cloud Resources"]
        API_CALL[API Calls]
        CDN_BW[CDN Bandwidth]
        STORAGE[Cloud Storage]
        COMPUTE[Compute]
    end

    subgraph Tagging["Cost Tagging Engine"]
        TAG[Tenant ID<br/>Tag Injection]
    end

    subgraph Billing["Billing System"]
        CALC[Tiered Rate<br/>Calculator]
        INVOICE[Invoice<br/>Generator]
    end

    Resources --> TAG --> CALC --> INVOICE
```

### Tiered Pricing Implementation

```python
# Tiered pricing calculation
def calculate_tenant_fee(base_fee: float, ggr: float) -> float:
    """
    Calculate monthly tenant fee using tiered GGR share.

    Tiers:
      GGR < $500K    -> 15%
      $500K - $1M    -> 12%
      > $1M          -> 10%
    """
    if ggr <= 500_000:
        share = ggr * 0.15
    elif ggr <= 1_000_000:
        share = 500_000 * 0.15 + (ggr - 500_000) * 0.12
    else:
        share = 500_000 * 0.15 + 500_000 * 0.12 + (ggr - 1_000_000) * 0.10
    return base_fee + share
```

### Non-Payment Suspension Escalation

```mermaid
stateDiagram-v2
    [*] --> Active: Payment Current
    Active --> Warning: T+1 Overdue
    Warning --> Restricted: T+3 Overdue
    Restricted --> Frozen: T+7 Overdue
    Frozen --> Shutdown: T+30 Overdue

    note right of Warning
        Send payment reminder
    end note

    note right of Restricted
        Block new player registration
        Existing players unaffected
    end note

    note right of Frozen
        Freeze back-office access
        Players can still withdraw
    end note

    note right of Shutdown
        Full shutdown
        Data archival
    end note
```

---

## 8. Payment Processing Architecture

### Multi-Acquirer Routing

```mermaid
flowchart LR
    subgraph Operator["Operator Platform"]
        PAY_ORCH[Payment Orchestration<br/>e.g. PaymentIQ]
    end

    subgraph Acquirers["Acquirer Pool"]
        ACQ1[Nuvei<br/>50 markets]
        ACQ2[Worldpay<br/>145+ countries]
        ACQ3[CoinsPaid<br/>Crypto]
        ACQ4[Local PSP<br/>Regional]
    end

    PAY_ORCH -->|Route by geo,<br/>currency, risk| ACQ1
    PAY_ORCH -->|Failover| ACQ2
    PAY_ORCH -->|Crypto txns| ACQ3
    PAY_ORCH -->|Local methods| ACQ4
```

**Routing Strategies**:
- **Redundancy**: Avoid single point of failure
- **Load Balancing**: Distribute transaction volume across processors
- **Geographic Optimization**: Use local acquirers for higher approval rates
- **Risk Distribution**: Prevent total shutdown from single account termination

Target: **99%+ transaction success rate**

### Crypto Payment Integration

```json
{
  "provider": "CoinsPaid",
  "supported_currencies": ["BTC", "ETH", "USDT", "USDC", "LTC"],
  "fee_structure": {
    "crypto_to_crypto": "0.8%",
    "crypto_to_fiat": "1.5%"
  },
  "settlement": "real-time",
  "monthly_volume": "EUR 1B+",
  "integration": "REST API + Webhook callbacks"
}
```

---

## 9. Fraud Detection Technology Stack

```mermaid
flowchart TB
    subgraph Input["Data Collection"]
        DEV[Device Fingerprint<br/>Browser, OS, Hardware]
        IP[IP Intelligence<br/>Geolocation, VPN Detection]
        BEH[Behavioral Signals<br/>Betting Patterns, Session Data]
        PAY_SIG[Payment Signals<br/>Card BIN, Velocity]
    end

    subgraph Engine["Risk Engine"]
        RULES[Rule-Based Engine<br/>Velocity Checks, Thresholds]
        ML[ML Models<br/>Random Forest, AUC 0.729]
        GRAPH[Graph Analysis<br/>Multi-Account Networks]
    end

    subgraph Action["Response"]
        ALLOW[Allow]
        REVIEW[Manual Review]
        BLOCK[Block + Alert]
    end

    Input --> Engine --> Action
```

**Provider Capabilities**:

| Provider | Signals | Specialization |
|---|---|---|
| **SEON** | 900+ first-party data signals | iGaming fraud prevention, AML compliance |
| **Sift** | Cross-industry ML models | 100% fraud guarantee with financial backing |

---

## 10. Data Security Architecture

### PCI-DSS 4.0 Compliance

```mermaid
flowchart LR
    subgraph CDE["Cardholder Data Environment"]
        ENCRYPT[Encryption at Rest<br/>AES-256]
        TOKENIZE[Tokenization<br/>PAN Replacement]
        MFA[MFA Required<br/>for All Access]
    end

    subgraph Protection["Perimeter"]
        WAF_PCI[WAF<br/>Mandatory]
        SCAN[Quarterly<br/>Vulnerability Scan]
        AUDIT[Annual On-Site<br/>Assessment L1]
    end

    CDE --> Protection
```

### GDPR Compliance Matrix

| Player Right | Implementation | Exception |
|---|---|---|
| Access | Export player data on request | None |
| Rectification | Update personal information | None |
| Erasure ("Right to be Forgotten") | Delete personal data | AML records retained 5-7 years |
| Data Portability | Machine-readable export | None |
| Objection | Opt-out of processing | Self-exclusion records maintained for full period |

**Violation Penalties**: Up to **4% of global annual turnover** or **EUR 20M** (whichever is greater).

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Architecture Team
