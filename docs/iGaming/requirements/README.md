# iGaming Requirements Documentation

> **Audience**: Executives, Product Managers, Compliance Officers, Business Analysts
> **Focus**: WHAT and WHY - Business requirements, policies, and compliance
> **Last Updated**: 2026-02-08
> **Phase**: 6 Complete (35 documents split from source)

---

## Category Navigation

| Category | Description | Split Documents |
|----------|-------------|-----------------|
| [01_Player_Experience](01_Player_Experience/) | Player lifecycle, VIP, segmentation | 6 |
| [02_Financial_Operations](02_Financial_Operations/) | Payment, reconciliation, wallet rules | 5 |
| [03_Gaming_Operations](03_Gaming_Operations/) | Game integration, turnover rules | 4 |
| [04_Promotions_VIP](04_Promotions_VIP/) | Bonus rules, activity policies | 3 |
| [05_Risk_Compliance](05_Risk_Compliance/) | KYC/AML, fraud detection, licensing | 11 |
| [06_Governance_Licensing](06_Governance_Licensing/) | RBAC, audit, multi-jurisdiction, MFA | 6 |
| [07_Metrics_KPIs](07_Metrics_KPIs/) | Reporting, analytics, CS operations | 0 (source index) |

---

## All Split Documents

### 01 Player Experience (6 documents)

| Document | Key Topics |
|----------|------------|
| [Player Lifecycle](01_Player_Experience/Player_Lifecycle.md) | Player stages, KYC levels, status transitions |
| [Business Flows](01_Player_Experience/Business_Flows.md) | Core business processes, player journey maps |
| [Platform Overview](01_Player_Experience/Platform_Overview.md) | Platform capabilities, 5 core business concepts |
| [Solution Overview](01_Player_Experience/Solution_Overview.md) | Platform positioning, module relationships |
| [Industry Glossary](01_Player_Experience/Industry_Glossary.md) | iGaming terminology reference |
| [Terminology Standards](01_Player_Experience/Terminology_Standards.md) | Naming conventions, term standardization |

### 02 Financial Operations (5 documents)

| Document | Key Topics |
|----------|------------|
| [Payment Operations](02_Financial_Operations/Payment_Operations.md) | Payment methods, costs, SLAs, compliance |
| [Reconciliation Requirements](02_Financial_Operations/Reconciliation_Requirements.md) | Three-tier matching, discrepancy handling |
| [Financial Implementation Requirements](02_Financial_Operations/Financial_Implementation_Requirements.md) | Wallet business rules, payment processing policies |
| [Turnover Reconciliation Requirements](02_Financial_Operations/Turnover_Reconciliation_Requirements.md) | Turnover validation policies, game reconciliation |
| [Seamless Wallet Requirements](02_Financial_Operations/Seamless_Wallet_Requirements.md) | Seamless wallet business rules, state transitions |

### 03 Gaming Operations (4 documents)

| Document | Key Topics |
|----------|------------|
| [Turnover Business Rules](03_Gaming_Operations/Turnover_Business_Rules.md) | Wagering requirements, game weights |
| [Game Integration Requirements](03_Gaming_Operations/Game_Integration_Requirements.md) | Provider standards, certification checklist |
| [Game Integration Standards](03_Gaming_Operations/Game_Integration_Standards.md) | Provider API standards, integration policies |
| [Game Lobby Requirements](03_Gaming_Operations/Game_Lobby_Requirements.md) | Lobby organization, categorization, recommendations |

### 04 Promotions & VIP (3 documents)

| Document | Key Topics |
|----------|------------|
| [Bonus Calculation Requirements](04_Promotions_VIP/Bonus_Calculation_Requirements.md) | Bonus types, eligibility, terms |
| [Activity Risk Requirements](04_Promotions_VIP/Activity_Risk_Requirements.md) | Bonus abuse policies, matched betting detection |
| [Promotion Requirements](04_Promotions_VIP/Promotion_Requirements.md) | Wagering game weights, VIP tier rules |

### 05 Risk & Compliance (11 documents)

| Document | Key Topics |
|----------|------------|
| [Risk Strategy Overview](05_Risk_Compliance/Risk_Strategy_Overview.md) | Fraud types, KPIs, industry context |
| [KYC/AML Requirements](05_Risk_Compliance/KYC_AML_Requirements.md) | Verification levels, regulations |
| [Fraud Detection Requirements](05_Risk_Compliance/Fraud_Detection_Requirements.md) | Detection policies, priority levels |
| [Risk Proposal Requirements](05_Risk_Compliance/Risk_Proposal_Requirements.md) | Approval workflows, SLA policies |
| [Affordability Requirements](05_Risk_Compliance/Affordability_Requirements.md) | Affordability scoring, income assessment |
| [Player Protection Requirements](05_Risk_Compliance/Player_Protection_Requirements.md) | Self-exclusion, limit management, GAMSTOP |
| [ML Requirements](05_Risk_Compliance/ML_Requirements.md) | ML model policies, drift monitoring |
| [Jurisdiction Framework Requirements](05_Risk_Compliance/Jurisdiction_Framework_Requirements.md) | Multi-jurisdiction policies, regulatory routing |
| [Detection Model Spec](05_Risk_Compliance/Detection_Model_Spec.md) | Detection categories, rule thresholds |
| [Risk Requirements Summary](05_Risk_Compliance/Risk_Requirements_Summary.md) | Rule engine policies, escalation procedures |
| [Turnover Validation Requirements](05_Risk_Compliance/Turnover_Validation_Requirements.md) | Checkpoint snapshot rules, dual-layer protection |

### 06 Governance & Licensing (6 documents)

| Document | Key Topics |
|----------|------------|
| [Multi-Tenant Requirements](06_Governance_Licensing/Multi_Tenant_Requirements.md) | Hierarchy, data isolation, billing |
| [MFA Requirements](06_Governance_Licensing/MFA_Requirements.md) | Authentication policies, compliance |
| [MFA Recovery Requirements](06_Governance_Licensing/MFA_Recovery_Requirements.md) | Recovery procedures, backup authentication |
| [MFA Compliance Requirements](06_Governance_Licensing/MFA_Compliance_Requirements.md) | Audit trails, compliance testing |
| [MFA Architecture Spec](06_Governance_Licensing/MFA_Architecture_Spec.md) | MFA design requirements, authenticator policies |
| [Governance Requirements](06_Governance_Licensing/Governance_Requirements.md) | RBAC policies, audit logging, multi-tenant governance |

---

## Quick Links by Role

### For Executives
- [Risk Strategy Overview](05_Risk_Compliance/Risk_Strategy_Overview.md) - Fraud costs, KPIs, vendor comparison
- [Player Lifecycle](01_Player_Experience/Player_Lifecycle.md) - Player journey and engagement
- [Platform Overview](01_Player_Experience/Platform_Overview.md) - Platform capabilities overview
- [Solution Overview](01_Player_Experience/Solution_Overview.md) - Platform positioning

### For Compliance Officers
- [KYC/AML Requirements](05_Risk_Compliance/KYC_AML_Requirements.md) - Verification procedures
- [Fraud Detection Requirements](05_Risk_Compliance/Fraud_Detection_Requirements.md) - Detection policies
- [MFA Requirements](06_Governance_Licensing/MFA_Requirements.md) - Authentication compliance
- [Jurisdiction Framework Requirements](05_Risk_Compliance/Jurisdiction_Framework_Requirements.md) - Multi-jurisdiction compliance
- [Player Protection Requirements](05_Risk_Compliance/Player_Protection_Requirements.md) - Responsible gambling
- [Affordability Requirements](05_Risk_Compliance/Affordability_Requirements.md) - Player affordability

### For Product Managers
- [Bonus Calculation Requirements](04_Promotions_VIP/Bonus_Calculation_Requirements.md) - Bonus rules
- [Payment Operations](02_Financial_Operations/Payment_Operations.md) - Payment methods
- [Multi-Tenant Requirements](06_Governance_Licensing/Multi_Tenant_Requirements.md) - Platform hierarchy
- [Game Lobby Requirements](03_Gaming_Operations/Game_Lobby_Requirements.md) - Game lobby organization
- [Promotion Requirements](04_Promotions_VIP/Promotion_Requirements.md) - Promotion policies

---

## Related Documentation

- **Technical Implementation**: [../architecture/](../architecture/) - For architects and developers
- **Complete Source**: [../source/](../source/) - Full documentation (SSOT)
- **Main Index**: [../README.md](../README.md) - Navigation hub

---

**Status**: Phase 6 Complete - 35 documents split from source
**Validation**: No code blocks in requirements documents (verified)
