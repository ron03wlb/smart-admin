# iGame PM Analyst - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-07
**Skill**: igame-pm-analyst (P1 - Extended/Domain)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| Ultrathink Analysis | First Principles deconstruction | ~5 min |
| JTBD Framework | Identify jobs-to-be-done | ~3 min |
| Risk Assessment | Three-tier risk analysis | ~5 min |
| PRD Generation | Generate Traditional Chinese PRD | ~10 min |
| Clarification Questions | Generate precise questions | ~2 min |

### Rapid Analysis Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Detect Keywords | Auto-trigger by iGaming keywords | ~1 min |
| 2. Ultrathink | Trust/Velocity/Friction analysis | ~5 min |
| 3. JTBD Analysis | Player/Merchant/Operations jobs | ~3 min |
| 4. Risk Assessment | Financial/Performance/Compliance | ~5 min |
| 5. Architecture Mapping | SmartAdmin layer mapping | ~5 min |
| 6. PRD Generation | Complete requirement document | ~10 min |

**Total**: ~30 minutes per requirement analysis

---

## Trigger Keyword Matrix (Decision Guide)

### Primary Keywords (High Confidence)

| Keyword | Context | Auto-Trigger |
|---------|---------|--------------|
| iGame | Platform requirement analysis | Yes |
| 包網 | Gaming platform turnkey solution | Yes |
| 博弈 | Gaming/gambling features | Yes |
| 遊戲平台 | Game platform development | Yes |

### Secondary Keywords (Medium Confidence)

| Keyword | Context | Auto-Trigger |
|---------|---------|--------------|
| 錢包 (wallet) | Wallet operations | Yes |
| 存款/提款 | Financial transactions | Yes |
| 風控 | Risk management | Yes |
| VIP | VIP tier system | Yes |
| 優惠/返水/傭金 | Bonus engine | Yes |
| 遊戲聚合/供應商 | Game provider integration | Yes |
| 多租戶 | Multi-tenant architecture | Yes |

---

## Ultrathink Framework (First Principles)

### Trust Layer (信任)
- Double-entry accounting
- Idempotency design
- Audit logging
- Data consistency guarantees

### Velocity Layer (速度)
- Concurrency requirements
- Response time targets
- Throughput expectations
- Cache strategies

### Friction Layer (摩擦)
- User operation steps
- Automation degree
- Zero manual intervention

---

## JTBD Analysis Template

### Player Jobs (玩家需求)
- 獲得特權 (Gain privileges)
- 成就滿足感 (Achievement satisfaction)
- 社交炫耀 (Social display)

### Merchant Jobs (商戶需求)
- 自動化管理 (Automated management)
- 提升留存率 (Retention improvement)
- 降低運營成本 (Reduce operations cost)

### Operations Jobs (運營需求)
- 減少人工成本 (Reduce labor cost)
- 提升效率 (Efficiency improvement)
- 合規監控 (Compliance monitoring)

---

## Risk Assessment Matrix

### 🔴 Financial Safety (Critical)
- Double-entry ledger implementation
- Transaction isolation
- Idempotency keys
- Audit trails

### 🟡 Performance (Major)
- Concurrency control
- Cache optimization
- Async processing
- Database sharding

### 🟡 Compliance (Major)
- KYC/AML integration
- Audit logging
- Data retention policy
- GDPR compliance

---

## SmartAdmin Architecture Mapping

### Layer Responsibilities

| Layer | iGame Responsibility | Example |
|-------|---------------------|---------|
| Controller | API + Sa-Token permission | `@SaCheckPermission("vip:upgrade")` |
| Service | Business orchestration | VipUpgradeService |
| Manager | Transaction + Cache | `@Transactional` VipManager |
| Dao | MyBatis Plus queries | VipDao.selectByPlayerId() |

### Foundation Module Dependencies

| Module | Usage |
|--------|-------|
| foundation.cache | Redis caching for VIP status |
| foundation.mq | Kafka events for level changes |
| foundation.redis-lock | Distributed locks for upgrades |
| foundation.security-protect | XSS/CSRF protection |

---

## PRD Output Template

### Standard Sections

1. **Why (業務價值)**
   - Business problem statement
   - Target users and jobs
   - Success metrics

2. **What (功能需求)**
   - Functional requirements
   - Acceptance criteria
   - Edge cases

3. **How (技術方案)**
   - SmartAdmin architecture design
   - Database schema
   - API contract
   - Risk mitigation

---

## Related Skills

| Skill | Relationship | When to Use |
|-------|--------------|-------------|
| business-analyst | Upstream | Triggers igame-pm-analyst |
| java-architect | Downstream | Receives PRD for implementation |
| igame-feature-builder | Downstream | Implements PRD features |
| fraud-detection-pattern-generator | Parallel | Risk control analysis |

---

## Quick Examples

### Example 1: VIP Auto-Upgrade
```
Input: "需要開發VIP自動升級功能"
Output: Complete PRD with Ultrathink analysis, JTBD, risk assessment
Time: ~30 minutes
```

### Example 2: Unclear Requirement
```
Input: "需要開發風控系統"
Output: Clarification questions (scope, trigger, level)
Time: ~5 minutes
```

---

## Knowledge Base References

- [igame-concepts.yaml](igame-concepts.yaml) - iGaming core concepts dictionary
- [pattern-mapping.yaml](pattern-mapping.yaml) - Business to architecture patterns
- [prd-template.md](prd-template.md) - PRD generation template

---

**Notes**:
- Language: All PRD outputs in Traditional Chinese (繁體中文)
- Execution: Single-shot analysis (no multi-phase)
- Timeout: 10 minutes per execution
