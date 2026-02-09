# Notification System Business Requirements

> **Canonical Source**: [source-archive/10_Platform_Management/10-03_Notification_Architecture.md](../../source-archive/10_Platform_Management/10-03_Notification_Architecture.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Notification Architecture](../../architecture/10_Platform_Management/Notification_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Business Overview

The Notification Service is the platform's **single outbound communication channel**, responsible for managing all user-facing messages. It addresses three key pain points: **channel fragmentation**, **template maintenance difficulty**, and **cost overruns**.

---

## 2. Channel Categories

### 2.1 Transactional Messages (High Priority)

| Channel | Use Cases | Expected Delivery |
|---------|----------|-------------------|
| **SMS** | OTP verification codes, password reset | < 2 seconds |
| **Email** | Registration confirmation, password reset, account alerts | < 5 seconds |
| **Telegram/WhatsApp** | OTP codes (cost-free alternative to SMS) | < 1 second |

### 2.2 Marketing Messages (Low Priority)

| Channel | Use Cases | Expected Delivery |
|---------|----------|-------------------|
| **App Push** | Promotions, bonus notifications, game announcements | < 5 seconds |
| **In-App Message** | Site inbox messages, news | Real-time |
| **IM Integration** | Telegram Bot, Line OA, WhatsApp Business | < 5 seconds |
| **Email** | Newsletter, promotional campaigns | < 10 seconds |

---

## 3. Smart Routing Business Rules

The system automatically selects the optimal channel based on **cost** and **delivery rate**.

### 3.1 Transactional Routing Priority

| Priority | Channel | Cost per Message | Delivery Rate | Fallback Condition |
|----------|---------|-----------------|---------------|-------------------|
| 1st | Telegram/WhatsApp | $0 (free) | 95-98% | User not found, timeout |
| 2nd | SMS | $0.05 | 99.5% | Invalid phone, provider outage |
| Final | Manual Review | - | - | All channels failed |

**Business Rule**: Always attempt free channels first. Only fall back to paid SMS when free channels are unavailable or fail.

### 3.2 Marketing Routing Priority

| Priority | Channel | Cost per Message | Delivery Rate | Fallback Condition |
|----------|---------|-----------------|---------------|-------------------|
| 1st | App Push (FCM) | $0 (free) | 85% | Token invalid, timeout |
| 2nd | Email | $0.001 | 92% | Invalid email, quota exceeded |
| Persistent | In-App Inbox | $0 | 100% (if viewed) | Always saved |

**Business Rule**: Marketing messages are always persisted to the inbox regardless of push/email delivery.

### 3.3 24-Hour Read Check (Marketing Only)

If a push notification is not read within 24 hours, a follow-up email reminder is automatically sent.

---

## 4. Cost Optimization Rules

### 4.1 Channel Cost Comparison

| Channel | Cost per Message | Cost per 100,000 Messages | Delivery Rate | Effective Cost per Delivered |
|---------|-----------------|--------------------------|---------------|----------------------------|
| **Telegram/WhatsApp** | $0 | $0 | 95-98% | $0 (optimal) |
| **App Push (FCM)** | $0 | $0 | 85% | $0 (free but lower delivery) |
| **Email** | $0.001 | $100 | 92% | $0.00109 |
| **SMS** | $0.05 | $5,000 | 99.5% | $0.05025 (most expensive) |

### 4.2 Cost Optimization Strategies

| Strategy | Description | Savings |
|----------|-------------|---------|
| **Prioritize free channels** | Telegram/WhatsApp before SMS | Up to $0.05 per message |
| **Batch sending** | Merge same-template messages | Reduce provider API calls |
| **Smart degradation** | Delay non-critical messages during peak | Reduce load costs |
| **Provider negotiation** | Volume discounts at > 100k messages/month | 20-40% discount |
| **Self-hosted SMTP** | For email volume > 1M messages/month | 80% email cost reduction |

---

## 5. Template Management Rules

### 5.1 Template Requirements

| Rule | Description |
|------|-------------|
| **No Hardcoded Content** | All message content must use template codes |
| **Multi-language Support** | Templates reference translation keys, not hardcoded text |
| **Fallback Language** | If translation service fails, use English default |
| **Version Control** | Template changes tracked with versioning |

### 5.2 Template Example

| Template Code | Purpose | Variables |
|--------------|---------|-----------|
| `OTP_REGISTER` | Registration verification code | `code`, `expire_minutes` |
| `OTP_LOGIN` | Login verification code | `code`, `expire_minutes` |
| `PASSWORD_RESET` | Password reset link | `reset_link`, `expire_hours` |
| `PROMOTION_BONUS` | Bonus promotion notification | `bonus_amount`, `wagering_requirement` |
| `WITHDRAWAL_APPROVED` | Withdrawal approval notice | `amount`, `account_last4` |

---

## 6. Rate Limiting & Anti-Abuse

### 6.1 Rate Limits

| Limit Type | Threshold | Purpose |
|-----------|-----------|---------|
| **Global OTP Cap** | Max 5 OTP messages per phone per hour | Prevent malicious SMS cost abuse |
| **Cool-down Period** | 60-second minimum between sends | Prevent rapid-fire requests |
| **Daily Marketing Cap** | Max 3 marketing messages per user per day | Prevent user annoyance |

### 6.2 Do Not Disturb (DND) Rules

| Rule | Description |
|------|-------------|
| **Quiet Hours** | Marketing messages are not sent between 22:00 - 08:00 local time |
| **Queue Behavior** | Messages during quiet hours are queued and sent at 08:00 |
| **Exception** | Transactional messages (OTP, security alerts) ignore DND |

### 6.3 Deduplication

| Rule | Description |
|------|-------------|
| **Window** | Same user + same template within 60 seconds = duplicate |
| **Action** | Return error with cool-down remaining time |
| **Purpose** | Prevent accidental double-sends |

---

## 7. Inbox (Message Center) Requirements

### 7.1 Inbox Features

| Feature | Description |
|---------|-------------|
| **Message List** | Chronological list of all marketing messages |
| **Read/Unread Status** | Visual indicator for unread messages |
| **Deep Link** | Each message can link to a specific page/promotion |
| **Auto-Expiry** | Messages automatically removed after 30 days |

### 7.2 Inbox Business Rules

| Rule | Description |
|------|-------------|
| Only marketing messages saved to inbox | Transactional (OTP) messages are not persisted |
| Messages expire after 30 days | Automatic cleanup |
| Maximum 100 messages per user | Oldest messages removed when exceeded |

---

## 8. Security Requirements

| Requirement | Description |
|-------------|-------------|
| **Encrypted Storage** | Phone numbers and emails in notification logs must be encrypted |
| **Content Filtering** | Automatic scan for prohibited marketing terms (e.g., "guaranteed win") |
| **Audit Trail** | Complete log of all sent notifications with status and cost |
| **PII Masking** | Phone/email masked in operational reports |

---

## 9. Failure Handling Business Rules

| Scenario | Business Impact | Expected Behavior |
|----------|----------------|-------------------|
| **Primary channel fails** | Message not delivered | Automatic fallback to next channel |
| **All channels fail** | Critical message lost | Alert operations team + queue for manual review |
| **Provider quota exceeded** | Messages delayed | Queue with delay until quota resets |
| **User contact invalid** | Permanent delivery failure | Mark user for contact verification |

---

## 10. Business KPIs

| KPI | Target | Measurement |
|-----|--------|-------------|
| **OTP Delivery Rate** | > 99% | Successful deliveries / total sends |
| **Marketing Open Rate** | > 15% | Opened / delivered |
| **Average Delivery Time** | < 3 seconds (transactional) | Send to delivery confirmation |
| **Monthly Communication Cost** | Optimize 30% vs pure SMS | Actual cost / theoretical SMS-only cost |
| **Channel Fallback Rate** | < 5% | Fallback events / total sends |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Platform Team
