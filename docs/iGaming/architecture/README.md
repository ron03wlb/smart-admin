# iGaming Architecture Documentation

> **Audience**: Architects, Backend Developers, DevOps Engineers
> **Focus**: HOW - Technical implementation, APIs, system design
> **Last Updated**: 2026-02-08
> **Phase**: 6 Complete (40 documents split from source)

---

## Category Navigation

| Category | Description | Split Documents |
|----------|-------------|-----------------|
| [00_Overview](00_Overview/) | Technology stack, data model, platform architecture | 5 |
| [01_Player_Service](01_Player_Service/) | Player lifecycle implementation | 1 |
| [02_Finance_Service](02_Finance_Service/) | Wallet, payment, transactions, turnover | 8 + source index |
| [03_Game_Integration](03_Game_Integration/) | GP API, turnover calculation, lobby system | 4 + source index |
| [04_Activity_Engine](04_Activity_Engine/) | Bonus calculation, activity risk, promotions | 3 |
| [05_Risk_Engine](05_Risk_Engine/) | Fraud detection, ML integration, risk proposals | 10 |
| [06_Platform_Core](06_Platform_Core/) | Multi-tenant, RBAC, MFA, governance | 8 |
| [09_Infrastructure](09_Infrastructure/) | API Gateway, caching, streaming | 1 + source index |
| [11_Frontend](11_Frontend/) | Layout engine, i18n, mobile | 0 (source index) |
| [12_Security](12_Security/) | Encryption, GDPR, blind index | 0 (source index) |

---

## All Split Documents

### 00 Overview (5 documents)

| Document | Key Topics |
|----------|------------|
| [Technology Stack](00_Overview/Technology_Stack.md) | Libraries, versions, frameworks |
| [Business Logic Flows](00_Overview/Business_Logic_Flows.md) | Technical flow diagrams, sequence charts |
| [Data Model](00_Overview/Data_Model.md) | Entity relationships, field mappings |
| [Platform Architecture](00_Overview/Platform_Architecture.md) | Module architecture, system topology |
| [System Overview](00_Overview/System_Overview.md) | Technical formulas, token verification, idempotency |

### 01 Player Service (1 document)

| Document | Key Topics |
|----------|------------|
| [Player Lifecycle Implementation](01_Player_Service/Player_Lifecycle_Implementation.md) | State machine, database design, API specs |

### 02 Finance Service (8 documents)

| Document | Key Topics |
|----------|------------|
| [Payment Gateway API](02_Finance_Service/Payment_Gateway_API.md) | API specs, webhooks, provider integration |
| [Reconciliation Technical](02_Finance_Service/Reconciliation_Technical.md) | Three-way matching, scheduled jobs, data models |
| [Financial Implementation](02_Finance_Service/Financial_Implementation.md) | Wallet architecture, SAGA orchestrator, risk scoring engine |
| [Turnover Flowcharts](02_Finance_Service/Turnover_Flowcharts.md) | Turnover validation flow diagrams, bet lifecycle state machine |
| [Turnover Calculation Architecture](02_Finance_Service/Turnover_Calculation_Architecture.md) | Three-layer validation, event-driven data exchange |
| [Turnover Implementation](02_Finance_Service/Turnover_Implementation.md) | Wallet deduction algorithm, effective stake calculation |
| [Seamless Wallet Analysis](02_Finance_Service/Seamless_Wallet_Analysis.md) | Seamless wallet API specs, concurrency control, state machines |
| [Turnover Calculation Logic Detail](02_Finance_Service/Turnover_Calculation_Logic_Detail.md) | effectiveStake formulas, lockAmount lifecycle, rebate calculation |

### 03 Game Integration (4 documents)

| Document | Key Topics |
|----------|------------|
| [Turnover Calculation Logic](03_Game_Integration/Turnover_Calculation_Logic.md) | Three-layer validation, SmartAdmin mapping |
| [Game Integration Implementation](03_Game_Integration/Game_Integration_Implementation.md) | Seamless Wallet API controller, token verification, idempotency |
| [Game Integration Protocols](03_Game_Integration/Game_Integration_Protocols.md) | API protocols, provider adaptor layer, webhook specs |
| [Game Lobby System](03_Game_Integration/Game_Lobby_System.md) | Multi-level caching, Elasticsearch search, sorting |

### 04 Activity Engine (3 documents)

| Document | Key Topics |
|----------|------------|
| [Bonus Calculation Engine](04_Activity_Engine/Bonus_Calculation_Engine.md) | Calculation pipeline, game weight factors |
| [Activity Risk System](04_Activity_Engine/Activity_Risk_System.md) | Multi-layer risk assessment, matched betting detection |
| [Promotion Implementation](04_Activity_Engine/Promotion_Implementation.md) | Redisson distributed lock, wagering calculation, VIP tier change |

### 05 Risk Engine (10 documents)

| Document | Key Topics |
|----------|------------|
| [Risk System Architecture](05_Risk_Engine/Risk_System_Architecture.md) | Event-driven pipeline, Kafka/Flink, ML model serving |
| [KYC Verification API](05_Risk_Engine/KYC_Verification_API.md) | Identity verification service, database schema, API endpoints |
| [Fraud Detection System](05_Risk_Engine/Fraud_Detection_System.md) | ML detection models, rule engine, SpotBugs integration |
| [Risk Proposal Implementation](05_Risk_Engine/Risk_Proposal_Implementation.md) | Priority calculation, SLA monitoring, Camunda BPMN workflow |
| [Affordability Implementation](05_Risk_Engine/Affordability_Implementation.md) | Affordability scoring algorithms, API endpoints, monitoring |
| [Player Protection API](05_Risk_Engine/Player_Protection_API.md) | Self-exclusion service, limit management, GAMSTOP integration |
| [ML Integration Architecture](05_Risk_Engine/ML_Integration_Architecture.md) | ML training pipeline, model deployment, A/B testing |
| [Detection Model Implementation](05_Risk_Engine/Detection_Model_Implementation.md) | Five-layer detection architecture, Kafka pipeline, TCC transactions |
| [Turnover Validation Architecture](05_Risk_Engine/Turnover_Validation_Architecture.md) | Snapshot SQL schema, validation service, index strategy |
| [Risk Implementation](05_Risk_Engine/Risk_Implementation.md) | Rule engine (Drools/LiteFlow), risk score calculation, ML integration |

### 06 Platform Core (8 documents)

| Document | Key Topics |
|----------|------------|
| [Multi-Tenant Architecture](06_Platform_Core/Multi_Tenant_Architecture.md) | RLS, MyBatis-Plus plugin, tenant routing |
| [MFA Technical](06_Platform_Core/MFA_Technical.md) | TOTP/WebAuthn implementation, recovery |
| [MFA Recovery Implementation](06_Platform_Core/MFA_Recovery_Implementation.md) | Recovery flow, backup codes, identity verification |
| [MFA Compliance Validation](06_Platform_Core/MFA_Compliance_Validation.md) | Compliance testing framework, audit validation |
| [MFA Technical Architecture](06_Platform_Core/MFA_Technical_Architecture.md) | Authentication service design, factor management |
| [TOTP WebAuthn Implementation](06_Platform_Core/TOTP_WebAuthn_Implementation.md) | TOTP algorithm, WebAuthn registration/authentication |
| [Jurisdiction Routing Architecture](06_Platform_Core/Jurisdiction_Routing_Architecture.md) | Multi-jurisdiction routing, regulatory data isolation |
| [Governance Implementation](06_Platform_Core/Governance_Implementation.md) | SmartAdmin layer mapping, Sa-Token, MyBatis encryption |

### 09 Infrastructure (1 document)

| Document | Key Topics |
|----------|------------|
| [Infrastructure Implementation](09_Infrastructure/Infrastructure_Implementation.md) | API Gateway config, Blue-Green K8s/Istio, Redisson rate limiting |

---

## Quick Links by Technology

### Event-Driven Architecture
- [Risk System Architecture](05_Risk_Engine/Risk_System_Architecture.md) - Kafka/Flink event processing pipeline
- [Detection Model Implementation](05_Risk_Engine/Detection_Model_Implementation.md) - Five-layer detection with Kafka
- Stream Processing - See [source index](09_Infrastructure/)

### API Design
- [Payment Gateway API](02_Finance_Service/Payment_Gateway_API.md) - Payment provider integration
- [KYC Verification API](05_Risk_Engine/KYC_Verification_API.md) - Identity verification service
- [Player Protection API](05_Risk_Engine/Player_Protection_API.md) - Self-exclusion and limits
- [Game Integration Protocols](03_Game_Integration/Game_Integration_Protocols.md) - Game provider API
- API Design Principles - See [source index](09_Infrastructure/)

### Data Layer
- [Multi-Tenant Architecture](06_Platform_Core/Multi_Tenant_Architecture.md) - Row-level security, tenant isolation
- [Reconciliation Technical](02_Finance_Service/Reconciliation_Technical.md) - Three-way matching engine
- [Turnover Calculation Architecture](02_Finance_Service/Turnover_Calculation_Architecture.md) - Three-layer validation
- Wallet Architecture - See [source index](02_Finance_Service/)

### Security & Authentication
- [MFA Technical](06_Platform_Core/MFA_Technical.md) - TOTP/WebAuthn implementation
- [Fraud Detection System](05_Risk_Engine/Fraud_Detection_System.md) - ML-based fraud detection
- [ML Integration Architecture](05_Risk_Engine/ML_Integration_Architecture.md) - ML pipeline and A/B testing

### Infrastructure
- [Infrastructure Implementation](09_Infrastructure/Infrastructure_Implementation.md) - API Gateway, K8s, rate limiting
- [Game Lobby System](03_Game_Integration/Game_Lobby_System.md) - Multi-level caching, Elasticsearch

---

## Related Documentation

- **Business Requirements**: [../requirements/](../requirements/) - For executives and product managers
- **Complete Source**: [../source/](../source/) - Full documentation (SSOT)
- **Main Index**: [../README.md](../README.md) - Navigation hub

---

**Status**: Phase 6 Complete - 40 documents split from source
**Validation**: All documents contain technical implementation details (code, APIs, schemas)
