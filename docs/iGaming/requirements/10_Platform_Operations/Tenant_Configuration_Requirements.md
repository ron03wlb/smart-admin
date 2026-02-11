# Tenant Configuration Business Requirements

> **Canonical Source**: [source-archive/10_Platform_Management/10-02_Tenant_Configuration.md](../../source-archive/10_Platform_Management/10-02_Tenant_Configuration.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Architecture**: [Tenant Configuration Architecture](../../architecture/10_Platform_Management/Tenant_Configuration_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Self-Service Enablement**: Provides "out-of-the-box" tenant configuration enabling operators to manage site properties without platform intervention
- **Market Agility**: Defines 4 market templates (Asia VN/TH, Europe EU, Latin America) with pre-configured currencies, payment methods, and KYC levels for rapid market entry
- **Risk Control**: Establishes dual-review approval workflow for high-risk operations (IP restrictions, domain binding, maintenance mode) with comprehensive audit logging
- **Operational Safety**: Specifies real-time health checks (config sync every 30s, DNS every 5m, balance every hour) with severity-based alerting

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Configuration Propagation Time | < 1 minute | Time from change to all nodes synchronized |
| Zero-Downtime Deployment | 100% | No service restarts required for config changes |
| Audit Log Coverage | 100% | All configuration changes recorded with required fields |
| Config Health Check Pass Rate | > 99% | Successful health checks / Total checks |
| New Tenant Onboarding Time | < 1 hour | Time from template selection to site live |
| Dual-Review Compliance | 100% | High-risk operations with proper approval / Total high-risk ops |

---

## 1. Business Overview

The Tenant Configuration system provides "out-of-the-box" site configuration capabilities, enabling tenants to self-manage their site properties. All configuration changes must take effect dynamically without requiring service restarts.

---

## 2. Core Configuration Items

### 2.1 Basic Information

| Configuration | Description | Example |
|--------------|-------------|---------|
| **Site Name** | Displayed in browser title bar | "Casino VN" |
| **Primary Domain** | Main site domain | www.abc.com |
| **API Domain** | Backend API domain | api.abc.com |
| **CDN Domain** | Static asset domain | img.abc.com |
| **Base Currency** | Primary operating currency | VND |
| **Supported Currencies** | Additional wallet currencies | USD, VND, THB |
| **Timezone** | Operational timezone | Asia/Ho_Chi_Minh |

### 2.2 Operational Switches

| Switch | Description | Impact |
|--------|-------------|--------|
| **Maintenance Mode** | One-click full site or frontend maintenance | Blocks all player access |
| **Registration Switch** | Pause new user registration | Prevents new signups |
| **Deposit Switch** | Enable/disable deposits | Controls fund inflow |
| **Withdrawal Switch** | Enable/disable withdrawals | Controls fund outflow |

### 2.3 IP & Geo Restrictions

| Restriction Type | Description | Example |
|-----------------|-------------|---------|
| **Country Allowlist** | Permitted access countries (GeoIP) | VN, TH, ID |
| **Country Blacklist** | Blocked access countries | US, UK, FR |
| **Office IP Whitelist** | Authorized admin portal IPs | 192.168.1.0/24 |

### 2.4 Game Credit Quota (Transfer Mode)

| Rule | Description |
|------|-------------|
| **Pre-paid Model** | Tenant must purchase credit from platform before players can play |
| **Low Balance Alert** | When tenant balance falls below threshold, alert is triggered |
| **Auto-block** | When balance is insufficient, players are blocked from transferring into games |

### 2.5 Game Management

| Feature | Description | Granularity |
|---------|-------------|------------|
| **Provider Blocklist** | Block entire game provider | e.g., Block all PG Soft games |
| **Game Blocklist** | Block specific games | e.g., Block "Mahjong Ways 2" |
| **Maintenance Mode** | Tenant-level game maintenance page | Independent of platform maintenance |

---

## 3. Configuration Templates

### 3.1 Market Templates

| Template | Default Currency | Payment Methods | KYC Level | Withdrawal Limit |
|----------|-----------------|----------------|-----------|-----------------|
| **Asia (VN)** | VND | VietQR, MoMo, ZaloPay | BASIC | 50M VND/day |
| **Asia (TH)** | THB | PromptPay, TrueMoney | BASIC | 200k THB/day |
| **Europe (EU)** | EUR | SEPA, Trustly | FULL (GDPR) | 10k EUR/day |
| **Latin America** | BRL/MXN | PIX, Mercado Pago | BASIC | 50k BRL/day |

### 3.2 Template Auto-Configuration

When creating a new tenant using a template, the following are automatically configured:

| Item | Description |
|------|-------------|
| Currency & timezone | Based on market region |
| Payment methods | Pre-selected for the market |
| Game providers | Popular providers for the region |
| KYC requirements | Based on regulatory environment |
| Auto-approval thresholds | Withdrawal auto-approval limits |
| Language support | Primary + secondary languages |

---

## 4. Dynamic Configuration & Approval

### 4.1 Dynamic Configuration Principles

| Principle | Requirement |
|-----------|-------------|
| **Real-time Effect** | Changes must propagate to all nodes within 1 minute |
| **Zero Downtime** | Configuration changes must never cause service restart |
| **Version Tracking** | Every change creates a new version |

### 4.2 Approval Requirements

#### High-Risk Operations (Dual Review Required)

| Operation | Risk | Approval Flow |
|-----------|------|--------------|
| **Modify IP restrictions** | Risk of blocking legitimate traffic or opening backdoors | Tenant Admin submits > Platform Ops reviews > Takes effect |
| **Modify domain binding** | Risk of domain hijacking | Tenant Admin submits > Platform Ops reviews > Takes effect |
| **Disable maintenance mode** | Premature restoration risk | Tenant Admin submits > Platform Ops reviews > Takes effect |

#### Standard Operations (Audit Trail Only)

| Operation | Requirement |
|-----------|-------------|
| Modify site name/logo | No approval needed, but must record audit log |
| Adjust deposit/withdrawal limits | Record audit log with old/new values |
| Toggle game provider status | Record audit log |

### 4.3 Audit Log Requirements

Every configuration change must be recorded with:

| Field | Description |
|-------|-------------|
| **Who** | User ID and role |
| **What** | Changed field and old/new values |
| **When** | Timestamp of change |
| **Why** | Reason for change |
| **Approval** | Approval status and approver (if applicable) |
| **Source IP** | IP address of the requester |

---

## 5. Configuration Priority Rules

Configuration values are resolved in this order (highest to lowest priority):

| Priority | Source | Description | Example |
|----------|--------|-------------|---------|
| 1 (Highest) | **Runtime Override** | Operations team real-time adjustment | Promo period: min deposit = $20 |
| 2 | **Tenant Config** | Tenant's custom settings | VIP tenant: min deposit = $1 |
| 3 | **Template Config** | Market template defaults | Asia template: min deposit = $5 |
| 4 (Lowest) | **System Default** | Platform-wide global defaults | System default: min deposit = $10 |

---

## 6. Monitoring & Health Check Requirements

### 6.1 Configuration Health Checks

| Check Item | Frequency | Action on Failure |
|-----------|-----------|-------------------|
| All gateway nodes have consistent config version | Every 30 seconds | Alert operations team |
| Domain DNS resolution is valid | Every 5 minutes | Alert tenant admin |
| No IP whitelist/blacklist conflicts | On every change | Block the change |
| Tenant balance above minimum threshold | Every hour | Alert tenant + platform |

### 6.2 Alert Rules

| Condition | Severity | Notification |
|-----------|----------|-------------|
| Config sync failed (version mismatch > 1 minute) | Warning | Operations team |
| Tenant balance below threshold (< $1,000) | Warning | Tenant + Platform |
| Maintenance mode activated outside planned window | Critical | All stakeholders |
| Domain binding changed without approval | Critical | Security team |

---

## 7. Validation Rules

### 7.1 Input Validation

| Field | Validation Rule |
|-------|----------------|
| Domain name | Valid domain format (regex check) |
| IP address | Valid IPv4 or IPv6 format |
| Currency code | ISO 4217 standard |
| Timezone | IANA timezone database |
| Amount limits | Positive numbers within configured min/max |
| Country codes | ISO 3166-1 alpha-2 |

### 7.2 Business Rule Validation

| Rule | Description |
|------|-------------|
| Cannot be on both whitelist and blacklist | IP/Country cannot appear in both lists |
| Withdrawal limit >= Minimum withdrawal | Withdrawal limit must be at least the minimum amount |
| At least one payment method active | Cannot disable all payment methods |
| At least one language configured | Must have at least one active language |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Platform Team
