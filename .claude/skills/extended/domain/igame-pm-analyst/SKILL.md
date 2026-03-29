---
name: igaming-pm-analyst
description: "[P1 - Extended] iGaming Product Manager Assistant - Requirement analysis, PRD generation, multi-tenant architecture design, seamless wallet specification, and regional compliance. Uses Ultrathink (First Principles) deep analysis with SmartAdmin architecture mapping."
---

# iGaming PM Analyst

**Priority**: P1 | **Category**: Domain | **Language**: Traditional Chinese (繁體中文)

PM assistant for iGaming platform requirement analysis, combining First Principles thinking with SmartAdmin architectural patterns. Includes multi-tenant architecture design and seamless wallet specification.

## Trigger Keywords

**Primary Keywords** (High confidence):
- "iGame" / "包網" / "博弈" / "遊戲平台"
- "multi-tenant" / "white-label" / "seamless wallet"

**Secondary Keywords** (Medium confidence):
- "錢包" / "存款" / "提款" / "風控" / "VIP"
- "優惠" / "返水" / "傭金" / "多租戶"
- "tenant isolation" / "regional compliance" / "KYC" / "AML"

## Core Capabilities

### 1. Ultrathink Deep Analysis (First Principles)
- **Trust Layer**: Double-entry accounting, idempotency, audit logging
- **Velocity Layer**: Concurrency requirements, response time, throughput
- **Friction Layer**: User operation steps, automation degree

### 2. JTBD Framework
- Player jobs: Gain privileges, achievement satisfaction
- Merchant jobs: Automated management, retention improvement
- Operations jobs: Cost reduction, efficiency improvement

### 3. SmartAdmin Architecture Mapping
Maps business requirements to `Controller → Service → Manager → Dao` with Foundation module dependencies (cache, mq, redis-lock, security-protect).

### 4. Multi-Tenant Architecture Design
Three isolation strategies: Schema isolation, Row-level isolation, Full isolation. White-label customization (Logo, theme, domain). MyBatis interceptor auto-injects tenant_id.

### 5. Seamless Wallet Patterns (12 Core Patterns)
| Pattern | Priority |
|---------|----------|
| Token Validation Decision Tree | P0 |
| Idempotency Layered Design (Redis + DB + Lock) | P0 |
| Error Recovery Scenarios | P0 |
| Turnover Concurrent Accumulation (Lua atomic) | P0 |
| Sports Betting Logic | P1 |
| Free Spin Turnover | P1 |
| Roulette Hedge Detection | P1 |
| Accounting Entry Correction (IFRS 15) | P1 |
| Reconciliation Model Separation | P1 |
| Wagering Requirement Tracking | P1 |
| Bonus Wallet Transfer | P1 |
| Baccarat Tie Logic | P2 |

### 6. Regional Compliance Mapping
Auto-generate comparison tables for Europe (MGA, Curacao), Asia (PAGCOR), Americas (US Nevada), China (Macau).

### 7. Risk Assessment Framework
Three-dimensional: Fund Safety / Performance / Compliance.

### 8. PRD Document Generation
Standardized Traditional Chinese PRD with 6-section structure: Executive Summary, Requirements Analysis, Technical Solution, Deep Analysis, Implementation Plan, Risk & Compliance.

## Phase-Based Execution

| Phase | Goal | Time |
|-------|------|------|
| Phase 1: Requirement Gathering | Collect requirements, clarify scope | 10-15 min |
| Phase 2: Multi-Tenant Design | Isolation strategy, white-label customization | 15-20 min |
| Phase 3: Seamless Wallet Design | Wallet API spec, turnover calculation | 20-25 min |
| Phase 4: Regional Compliance | Compliance tables, risk assessment | 10-15 min |

## Related Rules

- [Architecture Rules](CLAUDE.md) - SmartAdmin layered design
- [Manager Layer Rules](CLAUDE.md) - @Transactional, distributed lock
- [Naming Conventions](CLAUDE.md) - Entity/Service/Manager naming

## Related Skills

- **[igaming-feature-builder](../igame-feature-builder/SKILL.md)** - Feature implementation (developer perspective)
- **[liteflow-rule-builder](../liteflow-rule-builder/SKILL.md)** - Tenant config-driven workflows

---
**Version**: 2.0.0 (Merged from igame-pm-analyst + igaming-multi-tenant-wallet-pm)
**Last Updated**: 2026-03-07
