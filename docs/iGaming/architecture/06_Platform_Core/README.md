# 06 Platform Core

> **Audience**: Architects, Backend Developers, DevOps
> **Status**: Phase 6 Complete - 8 split documents + source index

---

## Split Documents

| Document | Description | Source |
|----------|-------------|--------|
| [Multi-Tenant Architecture](Multi_Tenant_Architecture.md) | Row-level security, MyBatis-Plus tenant plugin, data isolation design | [source](../../source-archive/06_Platform_Governance/06-01_Multi_Tenant.md) |
| [MFA Technical](MFA_Technical.md) | TOTP/WebAuthn implementation, recovery flow, compliance validation | [source](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md) |
| [MFA Compliance Validation](MFA_Compliance_Validation.md) | Backup code algorithms, audit log schemas, anomaly detection queries | [source](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md) |
| [MFA Recovery Implementation](MFA_Recovery_Implementation.md) | Recovery flow state machine, two-phase login, trusted device management | [source](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) |
| [Jurisdiction Routing Architecture](Jurisdiction_Routing_Architecture.md) | Multi-jurisdiction routing, geo-based restrictions, license-aware request processing | [source](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) |
| [MFA Technical Architecture](MFA_Technical_Architecture.md) | MFA system architecture, STRIDE threat modeling, CVSS scoring | [source](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) |
| [TOTP WebAuthn Implementation](TOTP_WebAuthn_Implementation.md) | TOTP RFC 6238 algorithm, AES-256-GCM encryption, WebAuthn attestation | [source](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md) |
| [Governance Implementation](Governance_Implementation.md) | SmartAdmin layer mapping, Sa-Token integration, MyBatis interceptor encryption, async audit | [source](../../source-archive/00_Foundation/guides/00-15_Governance_Implementation.md) |

## Source Index

| Document | Description | Source |
|----------|-------------|--------|
| RBAC Implementation | Role-based access control system design | [source](../../source-archive/06_Platform_Governance/06-02_RBAC_Permissions.md) |
| Audit Log | System audit logging architecture | [source](../../source-archive/06_Platform_Governance/06-03_Audit_Log.md) |
| Approval Workflow | Multi-level approval system | [source](../../source-archive/06_Platform_Governance/06-04_Approval_Workflow.md) |
| Tenant Configuration | Tenant configuration management | [source](../../source-archive/10_Platform_Management/10-02_Tenant_Configuration.md) |

---

**Last Updated**: 2026-02-08
