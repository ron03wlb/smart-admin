# Multi-Tenant Requirements

> **Canonical Source**: [`docs/iGaming/source/06_Platform_Governance/06-01_Multi_Tenant.md`](../../source/06_Platform_Governance/06-01_Multi_Tenant.md)
>
> **Related Architecture**: [`architecture/06_Platform_Core/Multi_Tenant_Architecture.md`](../../architecture/06_Platform_Core/Multi_Tenant_Architecture.md)

---

## 1. Tenant Hierarchy Definition

The system adopts a **SaaS (Software as a Service)** multi-tenant architecture with the following hierarchy:

**Super Admin (Platform) -> Brand (Group/Conglomerate) -> Tenant (Merchant/Site) -> Agent**

---

## 2. Hierarchy Requirements

### 2.1 Super Admin (Platform Administrator)

- **Authority**: God-view perspective with visibility into all Brand and Tenant data
- **Functions**:
  - Create new Brands
  - Global game switches (emergency kill-switch for Game Providers)
  - Global risk control rule settings

### 2.2 Brand (Group/Conglomerate)

- **Definition**: Represents an operating group that can own multiple sites (Tenants) with different domains
- **Shared Resources**:
  - Tenants under a Brand may optionally share "Player Blacklist"
  - Financial Quota typically managed at Brand level

### 2.3 Tenant (Merchant/Site)

- **Definition**: Actual operating website with independent domain, logo, and frontend styling
- **Data Isolation**: Tenant A players cannot log into Tenant B (unless group-wide SSO is configured)
- **Configuration Independence**:
  - Independent payment gateway merchant IDs
  - Independent game selection
  - Independent promotional activities

---

## 3. Cross-Hierarchy Management Requirements

### 3.1 Perspective Switching

- Super Admin can "Impersonate" (simulate) entry into any Tenant's backend for operations
- Visual indicator required when in impersonation mode

### 3.2 Report Aggregation

- **Tenant-level reports**: Single site profit/loss
- **Brand-level reports**: Group total profit/loss (aggregating all Tenants)

---

## 4. Permission Inheritance Rules

### 4.1 Top-Down Inheritance

| Hierarchy | Visible Data Scope | Operable Scope | Example Role |
|-----------|-------------------|----------------|--------------|
| **Super Admin** | All global data | Create Brand, global configuration | Platform CTO |
| **Brand Admin** | All Tenants under owned Brand | Create Tenant, Brand-level configuration | Group CEO |
| **Tenant Admin** | Owned Tenant data | Manage players, agents, activities | Site Operations Manager |
| **Agent** | Self and subordinate agent data | View subordinate players, commissions | Agent Partner |

### 4.2 Permission Scope Matrix

| Operation | Super Admin | Brand Admin | Tenant Admin | Agent |
|-----------|:-----------:|:-----------:|:------------:|:-----:|
| Create Brand | Yes | No | No | No |
| Create Tenant | Yes | Yes | No | No |
| Global Game Switch | Yes | No | No | No |
| Brand Quota Adjustment | Yes | Yes | No | No |
| Player Management | Yes | Yes | Yes | No |
| View Commission Reports | Yes | Yes | Yes | Yes |

---

## 5. Data Segregation Policies

### 5.1 Isolation Requirements

1. **Strict Tenant Isolation**: Players, transactions, and game sessions must be isolated by tenant_id
2. **Brand-Level Aggregation**: Brand Admin can view aggregated data across Tenants but cannot modify individual Tenant records without impersonation
3. **No Cross-Brand Access**: Brand A cannot access any data from Brand B

### 5.2 Shared Data Considerations

| Data Type | Sharing Level | Requirement |
|-----------|--------------|-------------|
| Player Blacklist | Optional (Brand) | Configurable per Brand |
| Game Provider List | Global | Managed by Super Admin |
| Payment Providers | Tenant-specific | Each Tenant has own merchant ID |
| Risk Rules | Hierarchical | Global defaults, Tenant overrides allowed |

---

## 6. Branding and Customization Rules

### 6.1 Tenant-Level Customization

Each Tenant must support:
- **Domain Configuration**: Independent FQDN
- **Visual Identity**: Logo, favicon, color scheme
- **Email Templates**: Branded transactional emails
- **SMS Sender ID**: Localized sender identification
- **Currency Settings**: Primary currency and exchange rates

### 6.2 White-Label Requirements

| Component | Customizable | Notes |
|-----------|:------------:|-------|
| Domain | Yes | SSL certificate required |
| Logo | Yes | Multiple sizes (favicon, header, footer) |
| Color Theme | Yes | Primary, secondary, accent colors |
| Footer Text | Yes | Copyright, legal disclaimers |
| Email From Address | Yes | SPF/DKIM configuration needed |
| App Icon (if PWA) | Yes | iOS/Android specific sizes |

---

## 7. Licensing Per Tenant

### 7.1 Billing Models

Three billing modes supported:

1. **Fixed Monthly Fee (Subscription)**:
   - Fixed monthly charge per Tenant (e.g., $1,000/month)
   - Suitable for small sites

2. **Revenue Share**:
   - Platform takes percentage of player effective turnover (2-5%)
   - Suitable for large sites

3. **Hybrid Model**:
   - Base monthly fee + revenue share (e.g., $500/month + 1% turnover)
   - Balanced risk and reward

### 7.2 Billing Cycle Requirements

| Requirement | Specification |
|------------|---------------|
| Billing Period | Monthly (1st of each month) |
| Grace Period | 7 days before service suspension |
| Auto-Renewal | Default enabled |
| Invoice Format | PDF with VAT/Tax details |
| Payment Methods | Wire transfer, Credit card, Crypto |

### 7.3 Overdue Handling

| Days Overdue | Action |
|--------------|--------|
| 0-7 | Warning email sent daily |
| 8-14 | Account marked as "At Risk" |
| 15-30 | New player registration disabled |
| 31+ | Full service suspension |

---

## 8. Data Migration and Tenant Transfer

### 8.1 Cross-Brand Transfer Requirements

**Scenario**: Tenant transfers from Brand A to Brand B (e.g., acquisition)

**Migration Checklist**:
- [ ] All player data integrity verified
- [ ] Transaction history complete
- [ ] Wallet balances reconciled
- [ ] Pending withdrawals processed
- [ ] Audit trail preserved
- [ ] Regulatory notification (if required)

### 8.2 Data Portability (GDPR Compliance)

Tenants must be able to export:
- Player registration data
- Transaction history
- Game session records
- Communication logs
- Marketing consent records

**Export Format**: JSON or CSV with schema documentation

---

## 9. Group-Wide SSO Requirements

### 9.1 Brand-Wide Single Sign-On

**Requirement**: Players can use one account to log into multiple Tenants under the same Brand.

**Considerations**:
- Wallet balances remain Tenant-specific (no automatic transfer)
- VIP status may differ per Tenant
- Bonus eligibility checked per Tenant

### 9.2 SSO Configuration Options

| Option | Description |
|--------|-------------|
| Disabled | Each Tenant has completely separate player pools |
| Enabled (Same Wallet) | Shared wallet across Tenants |
| Enabled (Separate Wallets) | Same login, different wallet per Tenant |

---

## Related Documents

### Business Logic References
- [Billing & Invoicing](../02_Financial_Operations/Billing_Invoicing_Requirements.md) - Multi-tenant billing model details
- [Agent System](../07_Agent_Center/Agent_System_Requirements.md) - Hierarchy structure extension

### Technical Architecture References
- [Multi-Tenant Architecture](../../architecture/06_Platform_Core/Multi_Tenant_Architecture.md) - Technical implementation
- [RBAC Permissions](RBAC_Requirements.md) - Hierarchical permission implementation
- [Audit Logging](Audit_Log_Requirements.md) - Tenant operation auditing

### Technical Implementation

→ **[Multi-Tenant Architecture](../../architecture/06_Platform_Core/Multi_Tenant_Architecture.md)** - Tenant isolation mechanisms (shared-nothing pattern), dynamic tenant routing, hierarchical data partitioning (tenantId propagation), database sharding strategies, and SSO configuration options

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Product Team
