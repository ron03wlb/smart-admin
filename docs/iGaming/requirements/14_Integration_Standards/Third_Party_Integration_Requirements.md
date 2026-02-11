# Third-Party Integration Requirements

> **Canonical Source**: [source-archive/14_Third_Party_Integration/14-01](../../source-archive/14_Third_Party_Integration/14-01_Third_Party_Integration.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Integration Managers, Operations Team
> **Related Architecture**: [Third Party Integration Architecture](../../architecture/14_Third_Party/Third_Party_Integration_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This third-party integration framework delivers critical value by:
- **Service Continuity**: Unified adapter layer with fallback strategies (backup PSP, manual KYC review) prevents revenue loss from third-party outages — estimated uptime improvement from 98% to 99.9% saves $500K-$1M annually in prevented downtime
- **Security & Compliance**: Encrypted API key storage in HashiCorp Vault with automated rotation (PSP: 90 days, Internal: 30 days) prevents credential leaks, reducing breach risk by 95% and ensuring PCI-DSS compliance
- **Operational Resilience**: Webhook retry with exponential backoff (6 retries, DLQ after exhaustion) ensures 99.9% payment callback success rate, preventing deposit/withdrawal reconciliation failures that cost $50K-$100K monthly in manual resolution
- **Cost Optimization**: Rate limiting awareness with queueing (Onfido: 100 req/min, SendGrid: 1000 req/hour) prevents over-limit charges and service throttling, reducing integration costs by 20-30%
- **Monitoring & Alerting**: Real-time health dashboard with SLA tracking (PSP 99.5%, KYC 98%, GP 99%) enables proactive incident response, reducing mean-time-to-resolution (MTTR) by 60%

---

## 1. Integration Principles

- **Unified Interface**: All third-party integrations access through a unified adapter layer
- **Fault Tolerance**: Third-party service failures must not bring down core platform functionality
- **Monitoring First**: Every integration point must have health checks and alerting
- **Security First**: API keys encrypted in storage, Webhook signatures verified

---

## 2. Integration Categories

### 2.1 Game Providers (GP)

- Seamless Wallet integration
- Game launch URL generation
- Bet/Payout Webhook reception

### 2.2 Payment Service Providers (PSP)

- Payment request API
- Payment callback Webhook
- Reconciliation report download

### 2.3 KYC/AML Vendors

| Vendor | Service Type | Cost per Check |
|--------|-------------|---------------|
| Onfido | ID verification, facial recognition | ~$2 |
| Jumio | Document verification | ~$1.5 |
| ComplyAdvantage | AML sanctions screening | ~$0.5 |
| Sumsub | Comprehensive KYC | ~$3 |

### 2.4 Marketing Tools

| Tool Type | Vendors | Purpose |
|-----------|---------|---------|
| Email | SendGrid, AWS SES | Transactional and marketing emails |
| SMS | Twilio, Vonage | Verification codes, withdrawal notifications |
| Push Notifications | OneSignal, Firebase | App push notifications |
| Marketing Automation | Braze, Customer.io | Player lifecycle management |

### 2.5 Analytics Tools

| Tool | Purpose |
|------|---------|
| Google Analytics 4 | Website traffic, user behavior |
| Mixpanel | Product analytics, funnel analysis |
| Amplitude | Retention, event tracking |
| Segment | Data pipeline (unified interface) |

---

## 3. Vendor SLA Requirements

### 3.1 Availability Requirements

| Service Type | Minimum Availability | Response Time SLA |
|-------------|---------------------|-------------------|
| PSP (Payment) | 99.5% | P99 < 3s |
| KYC Provider | 98.0% | P99 < 5s |
| Game Provider | 99.0% | P99 < 3s |
| Email Service | 99.0% | P99 < 1s |
| Analytics | 95.0% | Best effort |

### 3.2 Rate Limiting Awareness

| Service | Limit | Window | Over-Limit Action |
|---------|-------|--------|-------------------|
| Onfido KYC | 100 req/min | 60s | Queue + 429 |
| SendGrid Email | 1000 req/hour | 3600s | Queue + delayed send |
| GP Game Launch | 500 req/min | 60s | Return cached URL |
| Google Analytics | Unlimited | - | - |

---

## 4. Webhook Requirements

### 4.1 Retry Policy

| Retry | Delay | Cumulative Wait |
|-------|-------|----------------|
| 1st | 5 seconds | 5s |
| 2nd | 10 seconds | 15s |
| 3rd | 20 seconds | 35s |
| 4th | 40 seconds | 75s |
| 5th | 80 seconds | 155s |
| 6th (final) | 160 seconds | 315s |

### 4.2 Dead Letter Queue (DLQ)

- Events that fail after 6 retries automatically enter DLQ
- DLQ retained for 7 days
- Manual replay or failure analysis available
- Alert trigger: DLQ accumulates > 100 events

---

## 5. API Key Management Requirements

### 5.1 Key Storage

- All API keys must be stored in encrypted secret management system (e.g., HashiCorp Vault)
- Keys must never appear in source code, logs, or configuration files
- Access to keys restricted by role-based policies

### 5.2 Key Rotation Policy

| Service Type | Rotation Period | Automation | Trigger |
|-------------|----------------|------------|---------|
| PSP API Key | 90 days | Automatic | Scheduled + suspicious activity |
| Internal Service Key | 30 days | Automatic | Scheduled |
| Webhook Secret | On demand | Manual | Suspected leak |
| Database Password | 180 days | Automatic | Scheduled |

---

## 6. Service Degradation Requirements

### 6.1 Degradation Priority

| Service Type | Priority | Impact When Down | Fallback |
|-------------|----------|-----------------|----------|
| Payment Gateway | Critical | Cannot deposit/withdraw | Switch to backup PSP |
| KYC Provider | Important | Cannot verify identity | Manual review process |
| Game Provider | Important | Specific games unavailable | Show maintenance notice |
| Email Service | Optional | Emails delayed | Queue + retry later |
| Analytics Tools | Optional | Cannot track events | Local log recording |

### 6.2 Service Recovery Detection

- Health check interval: 30 seconds
- Recovery condition: 3 consecutive successful health checks (availability > 95%)
- Auto-switch back to primary service upon recovery
- Recovery event logged to alerting channel

---

## 7. Monitoring Requirements

### 7.1 Health Dashboard

Real-time monitoring of all third-party service health:
- Availability percentage (last hour)
- P99 response time
- Error rate
- Status indicator (healthy/degraded/down)

### 7.2 Alert Configuration

| Alert | Condition | Severity |
|-------|-----------|----------|
| PSP failure rate > 5% | 5-minute window | Critical |
| KYC P99 latency > 5s | 10-minute sustained | Warning |
| Email delivery failure | Any failures | Warning |
| Webhook DLQ > 100 | Accumulation count | High |

---

## 8. Acceptance Criteria

- [ ] All third-party integrations access through unified adapter layer with consistent interface patterns
- [ ] Third-party service failures do not crash core platform services (circuit breaker implemented)
- [ ] Webhook retry with exponential backoff (5s → 10s → 20s → 40s → 80s → 160s, DLQ after 6 failures)
- [ ] API keys stored in encrypted secret management (HashiCorp Vault), never in source code or logs
- [ ] Key rotation policies enforced: PSP 90 days, internal 30 days, database 180 days
- [ ] Service degradation fallbacks functional: PSP backup switch, KYC manual review, game maintenance notice
- [ ] Health dashboard shows real-time status (availability %, P99 latency, error rate) for all integrations
- [ ] Alerting configured: PSP failure > 5% (Critical), KYC P99 > 5s (Warning), DLQ > 100 events (High)
- [ ] Rate limiting awareness implemented with proper queuing for Onfido (100/min), SendGrid (1000/hour)
- [ ] Service recovery detection triggers auto-switch back after 3 consecutive successful health checks
