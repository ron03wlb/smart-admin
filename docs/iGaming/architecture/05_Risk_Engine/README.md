# 05 Risk Engine

> **Audience**: Architects, Backend Developers, DevOps
> **Status**: Phase 6 Complete - 10 split documents + source index

---

## Split Documents

| Document | Description | Source |
|----------|-------------|--------|
| [Risk System Architecture](Risk_System_Architecture.md) | Event-driven risk pipeline, Kafka/Flink integration, ML model serving | [source](../../source/05_Risk_Control/05-01_Risk_Framework.md) |
| [KYC Verification API](KYC_Verification_API.md) | Identity verification service, database schema, API endpoints | [source](../../source/05_Risk_Control/05-03_KYC_AML.md) |
| [Fraud Detection System](Fraud_Detection_System.md) | ML detection models, rule engine, SpotBugs integration | [source](../../source/05_Risk_Control/05-02_Fraud_Detection.md) |
| [Risk Proposal Implementation](Risk_Proposal_Implementation.md) | Priority calculation service, SLA monitoring, Camunda BPMN workflow, Sa-Token permissions | [source](../../source/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| [Affordability Implementation](Affordability_Implementation.md) | Affordability scoring algorithms, API endpoints, data integration, monitoring | [source](../../source/15_Responsible_Gambling/15-08_Affordability_Assessment.md) |
| [Player Protection API](Player_Protection_API.md) | Self-exclusion service, limit management API, session tracking, GAMSTOP integration | [source](../../source/15_Responsible_Gambling/15-07_Player_Protection_API.md) |
| [ML Integration Architecture](ML_Integration_Architecture.md) | ML training pipeline, model deployment, A/B testing, drift monitoring | [source](../../source/05_Risk_Control/05-02-03_ML_Integration.md) |
| [Detection Model Implementation](Detection_Model_Implementation.md) | Five-layer detection architecture, Kafka pipeline, TCC transactions, rule execution | [source](../../source/05_Risk_Control/05-02-01_Detection_Model.md) |
| [Turnover Validation Architecture](Turnover_Validation_Architecture.md) | Snapshot SQL schema, validation service, dual-layer architecture, index strategy | [source](../../source/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |
| [Risk Implementation](Risk_Implementation.md) | Rule engine architecture (Drools/LiteFlow), risk score calculation, ML integration | [source](../../source/00_Foundation/guides/00-14_Risk_Implementation.md) |

## Source Index

| Document | Description | Source |
|----------|-------------|--------|
| Detection Model | ML detection model architecture | [source](../../source/05_Risk_Control/05-02-01_Detection_Model.md) |
| Rule Configuration | Risk rule engine configuration | [source](../../source/05_Risk_Control/05-02-02_Rule_Configuration.md) |
| ML Integration | ML model training and deployment pipeline | [source](../../source/05_Risk_Control/05-02-03_ML_Integration.md) |
| Operations Tools | Risk operations tooling | [source](../../source/05_Risk_Control/05-02-04_Operations_Tools.md) |
| Risk Proposal Workflow | Risk proposal approval system | [source](../../source/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) |
| Turnover Validation | Turnover validation architecture | [source](../../source/05_Risk_Control/05-07_Turnover_Validation_Scheme.md) |

---

**Last Updated**: 2026-02-08
