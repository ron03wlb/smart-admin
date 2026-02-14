# iGaming Multi-Tenant Wallet PM - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-07
**Skill**: igaming-multi-tenant-wallet-pm (P1 - Extended/Domain)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| Full Flow | All 4 phases execution | ~60 min |
| Phase 1 | Requirement Gathering | ~15 min |
| Phase 2 | Multi-Tenant Design | ~20 min |
| Phase 3 | Seamless Wallet Design | ~15 min |
| Phase 4 | Regional Compliance | ~10 min |

### Phase Execution Options

```bash
# Full flow (all phases)
"Design multi-tenant VIP system with white-label, European license"

# Single phase execution
"Seamless wallet integration with Evolution Gaming" -> Phase 3

# Regional compliance only
"European market KYC compliance requirements" -> Phase 4
```

---

## Trigger Keyword Matrix (Decision Guide)

### Primary Keywords (High Confidence)

| Keyword | Context | Phase Triggered |
|---------|---------|-----------------|
| multi-tenant | Architecture design | Phase 2 |
| white-label | Platform customization | Phase 2 |
| seamless wallet | Wallet integration | Phase 3 |

### Secondary Keywords (Medium Confidence)

| Keyword | Context | Phase Triggered |
|---------|---------|-----------------|
| tenant isolation | Data isolation | Phase 2 |
| wallet integration | Provider integration | Phase 3 |
| MGA / PAGCOR | Gaming license | Phase 4 |
| KYC / AML | Compliance requirements | Phase 4 |
| VIP customization | White-label features | Phase 2 |

---

## Phase Execution Guide

### Phase 1: Requirement Gathering (~15 min)

**Input**: Business requirement description
**Output**: Structured requirement analysis

| Step | Action | Time |
|------|--------|------|
| 1 | Identify stakeholders | ~3 min |
| 2 | Document functional requirements | ~5 min |
| 3 | List non-functional requirements | ~4 min |
| 4 | Define acceptance criteria | ~3 min |

### Phase 2: Multi-Tenant Design (~20 min)

**Input**: Tenant isolation requirements
**Output**: Architecture design document

| Isolation Level | Use Case | Complexity |
|----------------|----------|------------|
| Schema | Enterprise SaaS | High |
| Row-level | Multi-brand platform | Medium |
| Full | Dedicated instances | Low |

**White-Label Customization Options**:
- Logo / Theme / Domain
- VIP tier configuration
- Payment method selection
- Regional settings

### Phase 3: Seamless Wallet Design (~15 min)

**Input**: Game provider integration requirements
**Output**: Wallet API specification

**12 Core Patterns**:

| Pattern | Priority | Purpose |
|---------|----------|---------|
| Token Validation | P0 | Unified validation |
| Idempotency | P0 | Duplicate prevention |
| Error Recovery | P0 | Failure handling |
| Turnover Accumulation | P0 | Atomic Lua scripts |
| Sports Betting | P1 | Win-half/Lose-half |
| Free Spin | P1 | Turnover calculation |
| Roulette Hedge | P1 | Coverage detection |
| Accounting Correction | P1 | IFRS 15 compliance |
| Reconciliation | P1 | Transaction vs deposit |
| Wagering Tracking | P1 | Withdrawal verification |
| Bonus Transfer | P1 | Wallet transfer logic |
| Baccarat Tie | P2 | Valid bet calculation |

### Phase 4: Regional Compliance (~10 min)

**Input**: Target market regions
**Output**: Compliance comparison table

| Region | License | KYC Level | Tax Rate |
|--------|---------|-----------|----------|
| Europe (MGA) | Strict | L3 | 15-20% |
| Philippines (PAGCOR) | Moderate | L2 | 5% |
| US (Nevada) | Very Strict | L3 | 6.75% |
| Macau | Strict | L3 | 39% |
| Curacao | Relaxed | L1 | 2% |

---

## Risk Assessment Framework

### 🔴 Fund Safety (Critical)
- Double-entry ledger
- Transaction atomicity
- Idempotency keys
- Audit trails

### 🟡 Performance (Major)
- High concurrency wallet operations
- Cache invalidation strategy
- Database sharding plan
- API response time targets

### 🟡 Compliance (Major)
- Regional KYC requirements
- AML monitoring
- Data residency requirements
- License obligations

---

## SmartAdmin Architecture Mapping

### Layer Responsibilities

| Layer | Wallet Responsibility | Example |
|-------|----------------------|---------|
| Controller | API + Sa-Token | `@SaCheckPermission("wallet:balance")` |
| Service | Business orchestration | WalletService |
| Manager | Transaction boundary | `@Transactional` WalletManager |
| Dao | MyBatis Plus queries | WalletDao.selectBalance() |

### Multi-Tenant Implementation

```java
// MyBatis Interceptor for tenant isolation
@Component
public class TenantInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) {
        // Auto-inject tenant_id condition
        String sql = addTenantFilter(originalSql, tenantId);
        return invocation.proceed();
    }
}
```

---

## Output Deliverables

### PRD Document Structure (6 Sections)

1. **Background & Goals** - Business context
2. **User Stories** - JTBD analysis
3. **Functional Requirements** - Feature specifications
4. **Non-Functional Requirements** - Performance, security
5. **Technical Design** - SmartAdmin architecture
6. **Risk Assessment** - 3-dimensional analysis

### Diagram Types

| Diagram | Use Case | Mermaid Type |
|---------|----------|--------------|
| Business Flow | User journey | flowchart |
| API Sequence | Wallet operations | sequenceDiagram |
| Data Model | Entity relationships | erDiagram |
| Architecture | System components | C4/architecture |
| State Machine | Transaction states | stateDiagram |

---

## Related Skills

| Skill | Relationship | When to Use |
|-------|--------------|-------------|
| igame-pm-analyst | Complementary | General iGaming PRD |
| java-architect | Downstream | Implementation |
| igame-feature-builder | Downstream | Feature development |
| fraud-detection-pattern-generator | Parallel | Risk control |

---

## Knowledge Base References

- [wallet-patterns.md](wallet-patterns.md) - 12 seamless wallet patterns
- [extracted-patterns.md](extracted-patterns.md) - 7 core PM patterns
- [prd-template.md](prd-template.md) - PRD generation template
- [mermaid-best-practices.md](mermaid-best-practices.md) - Diagram guidelines

---

## Quick Examples

### Example 1: Full Multi-Tenant Design
```
Input: "Design multi-tenant VIP system with white-label customization"
Phases: 1 -> 2 -> 3 -> 4
Output: Complete PRD with architecture, wallet design, compliance
Time: ~60 minutes
```

### Example 2: Wallet Integration Only
```
Input: "Seamless wallet integrate Evolution Gaming"
Phases: 3 only
Output: Wallet API specification document
Time: ~15 minutes
```

### Example 3: Compliance Comparison
```
Input: "European vs Asian market KYC requirements"
Phases: 4 only
Output: Regional compliance comparison table
Time: ~10 minutes
```

---

**Notes**:
- Language: All PRD outputs in Traditional Chinese (繁體中文)
- Execution: Phase-based (can run single phase or all 4)
- Timeout: 10 minutes per phase
