# iGaming Architecture Documentation

> **Audience**: Architects, Backend Developers, DevOps Engineers
> **Focus**: HOW - Technical implementation, APIs, system design
> **Last Updated**: 2026-02-09
> **Phase**: 9 Complete (117 documents across 18 categories)

---

## Category Navigation

| Category | Description | Documents |
|----------|-------------|-----------|
| [00_Overview](00_Overview/) | Technology stack, data model, platform architecture | 5 |
| [01_Player_Service](01_Player_Service/) | Player lifecycle implementation | 1 |
| [02_Finance_Service](02_Finance_Service/) | Wallet, payment, transactions, turnover, seamless wallet API spec | 9 |
| [03_Game_Integration](03_Game_Integration/) | GP API, turnover calculation, lobby system | 4 |
| [04_Activity_Engine](04_Activity_Engine/) | Bonus calculation, activity risk, promotions | 3 |
| [05_Risk_Engine](05_Risk_Engine/) | Fraud detection, ML integration, risk proposals | 10 |
| [06_Platform_Core](06_Platform_Core/) | Multi-tenant, RBAC, MFA, governance | 8 |
| [07_Agent_Service](07_Agent_Service/) | Credit network, agent system | 2 |
| [08_Analytics_Service](08_Analytics_Service/) | Reporting, BI dashboards | 2 |
| [09_Infrastructure](09_Infrastructure/) | API Gateway, caching, streaming, token validation | 23 |
| [10_Platform_Management](10_Platform_Management/) | Tenant config, notifications, data pipeline | 3 |
| [11_Frontend](11_Frontend/) | Layout engine, i18n, mobile, A/B testing | 11 |
| [12_Security](12_Security/) | Encryption, GDPR, blind index, ISO 27001 | 10 |
| [13_Customer_Service](13_Customer_Service/) | CS platform, operations | 2 |
| [14_Third_Party](14_Third_Party/) | Third-party integration | 1 |
| [15_Responsible_Gambling](15_Responsible_Gambling/) | Self-exclusion, deposit limits, player protection API | 4 |
| [adr](adr/) | Architecture Decision Records | 3 |
| [quality-reports](quality-reports/) | Quality gate reports | 2 |

---

## New Modules (Phase 8-11)

### 07 Agent Service (2 documents)

| Document | Key Topics |
|----------|------------|
| [Credit Network Architecture](07_Agent_Service/01_Credit_Network_Architecture.md) | Credit line management, settlement, database schema |
| [Agent System Architecture](07_Agent_Service/02_Agent_System_Architecture.md) | Agent hierarchy, commission calculation, API specs |

### 08 Analytics Service (2 documents)

| Document | Key Topics |
|----------|------------|
| [Reporting Architecture](08_Analytics_Service/01_Reporting_Architecture.md) | Report generation, data warehouse, ETL pipeline |
| [BI Dashboard Architecture](08_Analytics_Service/02_BI_Dashboard_Architecture.md) | Dashboard framework, real-time metrics, OLAP |

### 09 Infrastructure (23 documents)

| Document | Key Topics |
|----------|------------|
| [README](09_Infrastructure/README.md) | Module overview and navigation |
| [Infrastructure Implementation](09_Infrastructure/09_Infrastructure_Implementation.md) | API Gateway, Blue-Green K8s/Istio, rate limiting |
| [Deployment Architecture](09_Infrastructure/08_Deployment_Architecture.md) | K8s config, CI/CD pipeline |
| [Gateway Core](09_Infrastructure/01_Gateway_Core.md) | Kong configuration, routing |
| [Gateway Rate Limiting](09_Infrastructure/02_Gateway_Rate_Limiting.md) | Token Bucket, sliding window |
| [Gateway Security](09_Infrastructure/03_Gateway_Security.md) | mTLS, WAF rules, IP filtering |
| [API Design Principles](09_Infrastructure/04_API_Design_Principles.md) | REST API standards, versioning |
| [Authentication Architecture](09_Infrastructure/05_Authentication_Architecture.md) | OAuth2, JWT, multi-actor token |
| [Common Patterns](09_Infrastructure/06_Common_Patterns.md) | Shared API patterns, error handling |
| [Domain APIs](09_Infrastructure/07_Domain_APIs.md) | Domain-specific API specs |
| [QA Standards](09_Infrastructure/11_QA_Standards.md) | Test pyramid, coverage, automation |
| [Maintenance Architecture](09_Infrastructure/10_Maintenance_Architecture.md) | Zero-downtime, blue-green, canary |
| [Performance Monitoring](09_Infrastructure/12_Performance_Monitoring.md) | APM, Prometheus/Grafana, alerting |
| [Performance Optimization](09_Infrastructure/13_Performance_Optimization.md) | Caching, query optimization |
| [Stream Processing Architecture](09_Infrastructure/15_Stream_Processing_Architecture.md) | Kafka/Flink, event sourcing, CQRS |
| [Caching Strategy](09_Infrastructure/14_Caching_Strategy.md) | L1/L2/L3 cache, Redis, Caffeine |
| [Cost Optimization Architecture](09_Infrastructure/16_Cost_Optimization_Architecture.md) | Auto-scaling, infrastructure cost |
| [OAuth Refresh Token](09_Infrastructure/18_OAuth_Refresh_Token.md) | Token rotation, security |
| [Multi Actor Token Security](09_Infrastructure/17_Multi_Actor_Token_Security.md) | Multi-actor token design |
| [Token Validation Service](09_Infrastructure/20_Token_Validation_Service.md) | Centralized token validation |
| [Token Validation Architecture](09_Infrastructure/19_Token_Validation_Architecture.md) | Validation flow, circuit breaker |
| [Token Cache Performance](09_Infrastructure/21_Token_Cache_Performance.md) | Token cache hierarchy, TTL |
| [Token Edge Deployment](09_Infrastructure/22_Token_Edge_Deployment.md) | Edge deployment, CDN |

### 10 Platform Management (3 documents)

| Document | Key Topics |
|----------|------------|
| [Tenant Configuration Architecture](10_Platform_Management/01_Tenant_Configuration_Architecture.md) | Tenant routing, white-label config |
| [Notification Architecture](10_Platform_Management/02_Notification_Architecture.md) | Multi-channel notifications, templates |
| [Data Pipeline Architecture](10_Platform_Management/03_Data_Pipeline_Architecture.md) | ETL, data warehouse, real-time sync |

### 11 Frontend (11 documents)

| Document | Key Topics |
|----------|------------|
| [README](11_Frontend/README.md) | Module overview |
| [Frontend Layout Engine](11_Frontend/01_Frontend_Layout_Engine.md) | Layout system, component architecture |
| [Banner Announcement](11_Frontend/02_Banner_Announcement.md) | Banner management, scheduling |
| [SEO Performance](11_Frontend/04_SEO_Performance.md) | SEO optimization, Core Web Vitals |
| [Mobile App Architecture](11_Frontend/03_Mobile_App_Architecture.md) | Hybrid/native, platform support |
| [Marketing Compliance](11_Frontend/05_Marketing_Compliance.md) | Ad compliance, ASA/UKGC rules |
| [AB Testing Framework](11_Frontend/06_AB_Testing_Framework.md) | A/B testing, feature flags |
| [i18n Localization](11_Frontend/07_i18n_Localization.md) | Internationalization architecture |
| [Dynamic Content Localization](11_Frontend/08_Dynamic_Content_Localization.md) | Dynamic content translation |
| [Localization Workflow](11_Frontend/09_Localization_Workflow.md) | Translation pipeline |
| [Localization API](11_Frontend/10_Localization_API.md) | i18n API specifications |

### 12 Security (10 documents)

| Document | Key Topics |
|----------|------------|
| [README](12_Security/README.md) | Module overview |
| [Data Security Standard](12_Security/01_Data_Security_Standard.md) | PII classification, encryption |
| [Encryption Strategy](12_Security/02_Encryption_Strategy.md) | AES-256-GCM, Argon2id |
| [Blind Index Architecture](12_Security/03_Blind_Index_Architecture.md) | HMAC-SHA256 searchable encryption |
| [GDPR Data Deletion](12_Security/04_GDPR_Data_Deletion.md) | Crypto-shredding, right to erasure |
| [ISO27001 Mapping](12_Security/06_ISO27001_Mapping.md) | ISO 27001 control mapping |
| [UK RTS Security](12_Security/08_UK_RTS_Security.md) | UK Gambling Commission requirements |
| [Payment Restrictions](12_Security/09_Payment_Restrictions.md) | Payment limit enforcement |
| [Data Portability SAR](12_Security/05_Data_Portability_SAR.md) | Subject access requests |
| [MITM Detection](12_Security/10_MITM_Detection.md) | Man-in-the-middle detection |

### 13 Customer Service (2 documents)

| Document | Key Topics |
|----------|------------|
| [CS Platform Architecture](13_Customer_Service/01_CS_Platform_Architecture.md) | CS system design, Player 360, ticket routing |
| [CS Operations Architecture](13_Customer_Service/02_CS_Operations_Architecture.md) | CS operations, performance monitoring |

### 14 Third Party (1 document)

| Document | Key Topics |
|----------|------------|
| [Third Party Integration Architecture](14_Third_Party/01_Third_Party_Integration_Architecture.md) | Vendor integration standards, API specs |

### 15 Responsible Gambling (4 documents)

| Document | Key Topics |
|----------|------------|
| [Self-Exclusion Architecture](15_Responsible_Gambling/01_Self_Exclusion_Architecture.md) | Self-exclusion service, GAMSTOP integration |
| [Deposit Loss Limits Architecture](15_Responsible_Gambling/02_Deposit_Loss_Limits_Architecture.md) | Limit enforcement, database schema |
| [Session Protection Architecture](15_Responsible_Gambling/03_Session_Protection_Architecture.md) | Session tracking, reality checks |
| [Player Protection API](15_Responsible_Gambling/04_Player_Protection_API.md) | Protection API specs, affordability |

---

## Existing Modules (Phase 6)

### 00 Overview (5 documents)

| Document | Key Topics |
|----------|------------|
| [Technology Stack](00_Overview/03_Technology_Stack.md) | Libraries, versions, frameworks |
| [Business Logic Flows](00_Overview/05_Business_Logic_Flows.md) | Technical flow diagrams |
| [Data Model](00_Overview/04_Data_Model.md) | Entity relationships |
| [Platform Architecture](00_Overview/02_Platform_Architecture.md) | Module architecture |
| [System Overview](00_Overview/01_System_Overview.md) | Technical formulas, token verification |

### 01-06 (Core Modules - 42 documents)

See individual module READMEs for full listings:
- [01_Player_Service](01_Player_Service/) (1 doc)
- [02_Finance_Service](02_Finance_Service/) (8 docs)
- [03_Game_Integration](03_Game_Integration/) (4 docs)
- [04_Activity_Engine](04_Activity_Engine/) (3 docs)
- [05_Risk_Engine](05_Risk_Engine/) (10 docs)
- [06_Platform_Core](06_Platform_Core/) (8 docs)

---

## Quick Links by Technology

### Event-Driven Architecture
- [Risk System Architecture](05_Risk_Engine/01_Risk_System_Architecture.md) - Kafka/Flink event processing
- [Stream Processing Architecture](09_Infrastructure/15_Stream_Processing_Architecture.md) - Flink CDC, CQRS

### API Design
- [API Design Principles](09_Infrastructure/04_API_Design_Principles.md) - REST API standards
- [Authentication Architecture](09_Infrastructure/05_Authentication_Architecture.md) - OAuth2, JWT
- [Token Validation Service](09_Infrastructure/20_Token_Validation_Service.md) - Centralized validation

### Data Layer
- [Multi-Tenant Architecture](06_Platform_Core/01_Multi_Tenant_Architecture.md) - RLS, tenant isolation
- [Caching Strategy](09_Infrastructure/14_Caching_Strategy.md) - JetCache, Redis, Caffeine

### Security & Compliance
- [Data Security Standard](12_Security/01_Data_Security_Standard.md) - PII encryption
- [GDPR Data Deletion](12_Security/04_GDPR_Data_Deletion.md) - Crypto-shredding
- [ISO27001 Mapping](12_Security/06_ISO27001_Mapping.md) - Compliance mapping

### Infrastructure
- [Deployment Architecture](09_Infrastructure/08_Deployment_Architecture.md) - K8s, CI/CD
- [Performance Monitoring](09_Infrastructure/12_Performance_Monitoring.md) - APM, Prometheus
- [Cost Optimization Architecture](09_Infrastructure/16_Cost_Optimization_Architecture.md) - Cloud costs

---

## Related Documentation

- **Business Requirements**: [../requirements/](../requirements/) - For executives and product managers
- **Archived Source**: [../source-archive/](../source-archive/) - Original SSOT documents (read-only)
- **Main Index**: [../README.md](../README.md) - Navigation hub

---

**Status**: Phase 9 Complete - 117 documents across 18 categories
**Validation**: All documents contain technical implementation details (code, APIs, schemas)
