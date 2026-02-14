---
name: igaming-multi-tenant-wallet-pm
description: [P1 - Extended] iGaming PM expert for multi-tenant architecture and seamless wallet design with phase-based approach (Requirements -> Multi-tenant -> Seamless Wallet -> Regional Compliance). Use when planning iGaming platform architecture, wallet system design, or regional compliance requirements.
trigger_keywords:
  - multi-tenant
  - white-label
  - seamless wallet
  - multi-merchant
  - white-label platform
  - wallet integration
  - gaming wallet
  - European license
  - Asian market
  - Americas compliance
  - China gaming
  - KYC
  - AML
  - regional compliance
version: 1.0.0
priority: P1
phase_based: true
phases:
  - phase-1-requirement-gathering
  - phase-2-multi-tenant-design
  - phase-3-seamless-wallet-design
  - phase-4-regional-compliance
---

# iGaming Multi-Tenant Wallet PM Expert

## Quick Start

### Most Common Usage Examples

```bash
# Full flow (all phases)
"Design multi-tenant VIP system with white-label customization, European license compliance"
-> Execute: Phase 1-4 (Requirements -> Multi-tenant -> Seamless Wallet -> Regional Compliance)

# Single phase execution
"Seamless wallet integration with Evolution Gaming"
-> Execute: Phase 3 (Seamless Wallet Design)

# Regional compliance query
"European market KYC compliance requirements comparison"
-> Execute: Phase 4 (Regional Compliance)
```

### Output Deliverables

- Standardized Traditional Chinese PRD documents (6-section structure)
- Mermaid diagrams (flowchart, sequenceDiagram, erDiagram, architecture)
- SmartAdmin layered design (Controller -> Service -> Manager -> Dao)
- Regional compliance comparison tables (Europe/Asia/Americas/China)
- Risk assessment matrix (Fund Safety / Performance / Compliance)

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "multi-tenant" - Multi-tenant architecture design
- "white-label" - White-label platform solution
- "seamless wallet" - Seamless wallet integration

**Secondary Keywords** (Medium confidence):
- "tenant isolation" - Data isolation strategy
- "wallet integration" - Game provider wallet integration
- "regional compliance" - KYC/AML compliance requirements
- "MGA" / "PAGCOR" - European/Asian gaming license compliance
- "VIP customization" - White-label VIP tier customization

**Phrase Patterns**:
- "Design [multi-tenant feature]" - Example: "Design multi-tenant VIP system"
- "[seamless wallet] integrate [provider]" - Example: "Seamless wallet integrate Evolution Gaming"
- "[region] compliance requirements" - Example: "European market KYC compliance comparison"

**Note**: This skill can also be manually invoked via `/igaming-multi-tenant-wallet-pm` command. Supports phase-based execution: `--phase=1` (requirements), `--phase=2` (multi-tenant), `--phase=3` (wallet), `--phase=4` (compliance).

---

## Why This Skill Exists

### Problems Solved

1. **igame-pm-analyst insufficient**: Lacks deep multi-tenant and seamless wallet expertise
2. **Cross-regional compliance complexity**: Europe MGA, Philippines PAGCOR, US Nevada, Macau -- each with different licenses, KYC, tax rates
3. **Seamless wallet expertise fragmented**: 12 core patterns scattered across multiple documents
4. **Lack of standardized PRD template**: Inconsistent document structure
5. **Insufficient deep analysis**: Needs Ultrathink methodology (first principles analysis)

### Core Capabilities

| Capability | Description | Reference |
|-----------|------------|-----------|
| **Regional Compliance Mapping** | Auto-generate license, KYC, tax rate comparison tables | [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-1-regional-compliance-mapping---implementation-details) |
| **Multi-Tenant Architecture** | Isolation strategies (Schema/Row-level/Full) & white-label customization | [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-2-multi-tenant-architecture-design---implementation-details) |
| **Seamless Wallet Patterns** | 12 core wallet patterns integrated | [wallet-patterns.md](knowledge/wallet-patterns.md) |
| **Ultrathink Methodology** | First principles deep analysis framework | [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-4-ultrathink-methodology---example-analysis) |
| **PRD Document Generation** | Standardized Traditional Chinese PRD (6-section structure) | [prd-template.md](knowledge/prd-template.md) |
| **Mermaid Diagram Strategy** | 5 diagram types for scenario-based application | [mermaid-best-practices.md](knowledge/mermaid-best-practices.md) |
| **Risk Assessment Framework** | Fund Safety / Performance / Compliance 3-dimensional risk assessment | [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-7-risk-assessment-framework---full-matrix) |

---

## Core Patterns (7 Patterns)

### Pattern 1: Regional Compliance Mapping

Auto-generate multi-region license and compliance comparison tables for Europe (MGA, Curacao), Asia (Philippines PAGCOR, Singapore), Americas (US Nevada, Costa Rica), and China (Macau).

**Use Cases**: Cross-regional deployment compliance assessment, multi-tenant regional configuration, risk assessment & legal review, operations team compliance training.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-1-regional-compliance-mapping---implementation-details) for decision tree, comparison matrix, database schema, and Service layer implementation.

### Pattern 2: Multi-Tenant Architecture Design

Design multi-tenant isolation strategy with three isolation levels: Schema isolation, Row-level isolation, and Full isolation. Includes white-label customization (Logo, theme, domain).

**Use Cases**: SaaS platform multi-enterprise deployment, white-label customization, group enterprise multi-subsidiary management, agent/distributor systems.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-2-multi-tenant-architecture-design---implementation-details) for decision tree, strategy comparison, MyBatis interceptor, and ArchitectureTest verification.

### Pattern 3: Seamless Wallet Pattern Generation

Integrate 12 seamless wallet core patterns for complete wallet API specification documents.

**Core Pattern Index**:

| Pattern | Priority | Description |
|---------|----------|-------------|
| Token Validation Decision Tree | P0 | Unified token validation supporting multiple formats |
| Idempotency Layered Design | P0 | Three-layer protection (Redis + DB + Distributed Lock) |
| Error Recovery Scenarios | P0 | Out-of-order requests, pre-rollback, partial failure recovery |
| Turnover Concurrent Accumulation | P0 | Lua script atomic turnover accumulation (prevent TOCTOU) |
| Sports Betting Logic | P1 | Win-half/Lose-half Valid Bet calculation |
| Free Spin Turnover | P1 | Free spin Turnover calculation (Turnover != Valid Bet) |
| Roulette Hedge Detection | P1 | Roulette coverage rate detection algorithm |
| Accounting Entry Correction | P1 | Financial report accounting design (IFRS 15 compliance) |
| Reconciliation Model Separation | P1 | Game transaction vs deposit/withdrawal reconciliation |
| Wagering Requirement Tracking | P1 | Verify wagering requirements on withdrawal (not on bet) |
| Bonus Wallet Transfer | P1 | Bonus wallet and cash wallet transfer logic |
| Baccarat Tie Logic | P2 | Baccarat tie Valid Bet calculation |

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-3-seamless-wallet---implementation-details) for Bet API sequence diagram, Manager layer transaction code, and wallet transaction DDL.

### Pattern 4: Ultrathink Methodology (First Principles Deep Analysis)

Six-step analysis framework: Problem Decomposition -> First Principles -> Scenario Enumeration -> Risk Assessment -> Solution Comparison -> Decision Reasoning.

**Use Cases**: Complex business logic design, architecture selection decisions, risk assessment and mitigation, compliance requirements analysis.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-4-ultrathink-methodology---example-analysis) for the wagering requirement verification timing analysis example.

### Pattern 5: PRD Document Generation

Generate standardized Traditional Chinese PRD documents with 6-section structure: Executive Summary, Requirements Analysis, Technical Solution, Deep Analysis, Implementation Plan, Risk & Compliance.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-5-prd-document-template-structure) for the complete template structure and [prd-template.md](knowledge/prd-template.md) for the full template.

### Pattern 6: Mermaid Diagram Strategy

Select appropriate Mermaid diagram types for different scenarios: flowchart, sequenceDiagram, erDiagram, architecture (C4), and stateDiagram.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-6-mermaid-diagram-strategy---c4-architecture-example) for C4 architecture example and diagram type selection guide. See [mermaid-best-practices.md](knowledge/mermaid-best-practices.md) for complete Mermaid guidelines.

### Pattern 7: Risk Assessment Framework

Three-dimensional risk assessment matrix: Fund Safety, Performance, Compliance. Includes risk level definitions and escalation procedures.

See [extracted-patterns.md](knowledge/extracted-patterns.md#pattern-7-risk-assessment-framework---full-matrix) for the complete risk matrix and risk level definitions.

---

## Phase-Based Execution (4 Phases)

### Phase 1: Requirement Gathering

**Goal**: Collect user requirements, clarify functional scope and constraints.
**Input**: Natural language requirements, reference documents, business scenarios.
**Output**: Requirements analysis document (PRD Sections 1+2), user role definitions, functional/non-functional requirements.
**Time Estimate**: 10-15 minutes.

### Phase 2: Multi-Tenant Design

**Goal**: Design multi-tenant isolation strategy and white-label customization.
**Input**: Phase 1 output, tenant count, security requirements, cost considerations.
**Output**: Architecture design (PRD Section 3.1), tenant data model, tenant config DDL, MyBatis interceptor code, ArchitectureTest verification code.
**Time Estimate**: 15-20 minutes.

### Phase 3: Seamless Wallet Design

**Goal**: Design seamless wallet API specification and turnover calculation logic.
**Input**: Phase 2 output, game provider list, wallet type, wagering rules.
**Output**: Wallet API specification (Bet/Settle/Rollback), Token validation decision tree, Bet API sequence diagram, idempotency design, balance formulas, DDL.
**Time Estimate**: 20-25 minutes.

### Phase 4: Regional Compliance

**Goal**: Generate regional compliance comparison tables and risk assessment.
**Input**: Phase 3 output, target regions, KYC/AML requirements.
**Output**: Regional comparison table (PRD Section 6.1), compliance risk assessment, mitigation plans, regional selection decision tree.
**Time Estimate**: 10-15 minutes.

---

## Verification Checklist

### PRD Quality Check

- [ ] Contains Ultrathink deep analysis (Section 4)
- [ ] Contains at least 2 Mermaid diagram types (flowchart + sequenceDiagram)
- [ ] Specifies SmartAdmin layered design (Section 3.2)
- [ ] Assesses three-dimensional risk (Fund/Performance/Compliance, Section 6)
- [ ] Lists Foundation module dependencies
- [ ] Contains database DDL (Section 3.3)
- [ ] Contains ArchitectureTest verification code

### Multi-Tenant Design Check

- [ ] Isolation strategy specified (Schema/Row-level/Full)
- [ ] Tenant configuration items listed (feature flags, quotas)
- [ ] Tenant identification mechanism designed (JWT Token / HTTP Header / Subdomain)
- [ ] MyBatis interceptor auto-injects tenant_id
- [ ] All business tables include tenant_id column
- [ ] Redis cache includes tenant isolation (key prefix)
- [ ] Distributed locks include tenant isolation (lock key prefix)

### Seamless Wallet Design Check

- [ ] Token validation strategy specified (Bet API vs Result API)
- [ ] Idempotency mechanism complete (Redis + DB + Distributed Lock)
- [ ] Balance calculation formula correct (Playable Balance)
- [ ] Turnover accumulation logic correct (Lua script atomicity)
- [ ] Bet/Settle/Rollback APIs complete
- [ ] Sequence diagrams clear (at least 2)
- [ ] Database DDL includes tenant_id
- [ ] Audit logs complete

### Regional Compliance Check

- [ ] Regional comparison table complete (Europe/Asia/Americas/China)
- [ ] KYC requirements specified (levels: 1-5)
- [ ] AML requirements specified (large transaction thresholds)
- [ ] Tax rate calculations correct (GGR tax rate)
- [ ] Compliance risk assessment complete (risk level + mitigation)
- [ ] Legal team review approved

---

## Detailed Documentation

**[Anti-Patterns and Fixes](docs/anti-patterns.md)** - Complete anti-pattern list and correction recommendations.

---

## Cross-References & Skill Collaboration

### Related Rule Files

- **[Architecture Rules](../../../.agent/rules/foundation/F04-architecture-rules.md)** - Multi-tenant isolation, Controller -> Service -> Manager -> Dao layering, @Transactional Manager-only
- **[Naming Conventions](../../../.agent/rules/foundation/F01-naming-conventions.md)** - Entity naming: TenantEntity, WalletEntity; Manager naming: TenantManager (not XXXManagerImpl)
- **[Manager Layer Rules](../../../.agent/rules/foundation/F03-manager-layer.md)** - Cross-tenant queries, @Transactional isolation, distributed lock + optimistic lock
- **[Concurrency Safety Rules](../../../.agent/rules/technology/patterns/05-concurrency-safety.md)** - Lua script atomicity, Redisson RLock, tenant-isolated Redis keys

### Related Skills

- **Pre-requisite**: `igame-pm-analyst` - PRD requirements analysis (auto-triggers this skill)
- **Downstream**: **[igame-feature-builder](../igame-feature-builder/SKILL.md)** - Multi-tenant wallet feature implementation
- **Parallel**: **[fraud-detection-pattern-generator](../fraud-detection-pattern-generator/SKILL.md)** - Cross-tenant risk control isolation
- **Parallel**: **[liteflow-rule-builder](../liteflow-rule-builder/SKILL.md)** - Tenant config-driven workflows

### Skill Positioning

This is a **PM Expert skill** (product manager perspective), focusing on:
- Multi-tenant architecture design
- Seamless wallet design
- Regional compliance requirements
- PRD document generation

**Difference from igame-feature-builder**: This skill outputs PRD documents (PM perspective); igame-feature-builder outputs code implementations (developer perspective).

---

## Mermaid Diagram Standards

**CRITICAL**: All Mermaid diagrams must follow SmartAdmin syntax rules:

- Use `<br/>` for line breaks in graph/flowchart nodes: `[Line 1<br/>Line 2]`
- Use `<br/>` for line breaks in sequenceDiagram notes: `Note over A: Line 1<br/>Line 2`
- **Exception**: stateDiagram-v2 does NOT support `<br/>` tags

**Reference**: See [mermaid-best-practices.md](knowledge/mermaid-best-practices.md) for complete guidelines.

---

**Version**: 1.1.0 (Optimized - patterns extracted to knowledge/)
**Last Updated**: 2026-02-06
**Documentation Structure**: Main + Extracted Patterns + Anti-Patterns Doc
