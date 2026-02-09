# Governance Implementation

> **Canonical Source**: [00-15_Governance_Implementation.md](../../source/00_Foundation/guides/00-15_Governance_Implementation.md)
> **Audience**: Architects, Backend Engineers, Security Engineers
> **Business Requirements**: [Governance_Requirements.md](../../requirements/06_Governance_Licensing/Governance_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

This document covers the technical implementation of the iGaming platform governance system, including multi-tenant architecture, RBAC permission system, audit logging, and data encryption strategy. All implementations follow SmartAdmin layered architecture patterns.

---

## 2. Multi-Tenant Architecture

**Status**: PLANNED (Phase 5+)
**Modules**: 10_Platform_Management, all business modules

### Implementation Goal

Design and implement tenant isolation using schema-based separation, data sharding, and tenant configuration management.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [06-01 Multi-Tenant](../../source/06_Platform_Governance/06-01_Multi_Tenant.md) | S2 Tenant Model | Schema isolation |
| 2 | [06-01 Multi-Tenant](../../source/06_Platform_Governance/06-01_Multi_Tenant.md) | S3 Data Isolation | Sharding strategy |
| 3 | [02-05 Billing](../../source/02_Finance_Center/02-05_Billing_and_Invoicing.md) | S2 Tenant Billing | Merchant management |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | Tenant context extraction from request headers |
| Service | Business logic with tenant-scoped queries (Vavr Option) |
| Manager | @Transactional tenant data operations, @Cacheable tenant config |
| Dao | tenant_id filtering on all queries via MyBatis interceptor |

### Verification Checklist

- [ ] Tenant data is fully isolated
- [ ] Cross-tenant queries are blocked
- [ ] Tenant configurations load correctly
- [ ] Tenant billing is accurate

### Common Pitfalls

1. **Data Leakage**: Missing `tenant_id` filter allows cross-tenant access -- enforce via MyBatis interceptor
2. **Performance**: Multi-tenant queries not using shard keys -- mandate shard-key in all Dao queries
3. **Configuration Error**: Tenant-specific config not isolated -- use separate config stores with @Cacheable in Manager

---

## 3. RBAC Permission System

**Status**: PLANNED (Phase 5+)
**Modules**: 12_System_Security, 10_Platform_Management

### Implementation Goal

Build role definitions, permission matrices, and dynamic authorization integrated with Sa-Token.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [06-02 RBAC](../../source/06_Platform_Governance/06-02_RBAC_Permissions.md) | S2 Permission Model | RBAC design |
| 2 | [06-02 RBAC](../../source/06_Platform_Governance/06-02_RBAC_Permissions.md) | S3 Role Management | Role inheritance |
| 3 | [06-02 RBAC](../../source/06_Platform_Governance/06-02_RBAC_Permissions.md) | S4 Permission Verification | Sa-Token integration |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | `@SaCheckPermission` annotation for endpoint authorization |
| Service | Permission logic, role resolution (Vavr Option for lookups) |
| Manager | @Cacheable permission cache, @Transactional role mutations |
| Dao | Role/permission CRUD via MyBatis Plus |

### Key Integration Points

- **Sa-Token**: Permission verification via `@SaCheckPermission` annotations
- **Redis Cache**: Permission data cached via Redisson, invalidated on role change
- **Manager Layer**: All permission cache operations use `@Cacheable` in Manager (never in Service)

### Verification Checklist

- [ ] Role permissions are correctly configured
- [ ] Permission verification is accurate
- [ ] Dynamic authorization takes effect
- [ ] Permission inheritance is correct

### Common Pitfalls

1. **Permission Explosion**: Too many permissions -- group into categories with hierarchical structure
2. **Circular Dependency**: Role inheritance cycles -- validate DAG structure on save
3. **Cache Inconsistency**: Permission changes not reflected -- implement cache eviction in Manager layer

---

## 4. Audit Log System

**Status**: PLANNED (Phase 5+)
**Modules**: 12_System_Security, all business modules

### Implementation Goal

Build operation logging, change tracking, and compliance reporting using AOP-based interception.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [06-03 Audit Log](../../source/06_Platform_Governance/06-03_Audit_Log.md) | S2 Log Model | Event definition |
| 2 | [06-03 Audit Log](../../source/06_Platform_Governance/06-03_Audit_Log.md) | S3 AOP Interception | Automatic recording |
| 3 | [06-03 Audit Log](../../source/06_Platform_Governance/06-03_Audit_Log.md) | S4 Query & Analysis | Audit reporting |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | `@AuditLog` annotation marks auditable endpoints |
| AOP Aspect | Intercepts annotated methods, captures before/after state |
| Service | Audit query logic (Vavr Option for optional audit fields) |
| Manager | @Transactional async audit log writes |
| Dao | Append-only audit log table (no UPDATE/DELETE) |

### Technical Considerations

- **Async Writing**: Use `@Async` with virtual threads (Java 21) for non-blocking audit writes
- **Immutability**: Audit log table must be append-only -- no UPDATE or DELETE operations
- **Archival**: Implement scheduled archival via Snail-Job for logs older than retention period
- **Performance**: Asynchronous writing prevents audit overhead from impacting request latency

### Verification Checklist

- [ ] Critical operations are logged
- [ ] Before/after comparisons are accurate
- [ ] Audit logs are tamper-proof
- [ ] Compliance reports are complete

### Common Pitfalls

1. **Missing Critical Operations**: Not all sensitive operations covered -- maintain operation registry
2. **Performance Impact**: Synchronous writes degrade performance -- use async with virtual threads
3. **Storage Bloat**: Logs not archived -- schedule periodic archival with retention policies

---

## 5. Data Encryption Strategy

**Status**: PLANNED (Phase 5+)
**Modules**: 12_System_Security, all business modules

### Implementation Goal

Implement field-level encryption, KMS integration, and key rotation using MyBatis interceptors.

### Implementation Reading Order

| Order | Document | Section | Focus |
|-------|----------|---------|-------|
| 1 | [12-03 Data Security](../../source/12_System_Security/12-03_Data_Security_Standard.md) | S2 Encryption Standard | AES-256-GCM |
| 2 | [12-03-01 Encryption](../../source/12_System_Security/12-03-01_Encryption_Strategy.md) | S3 Field Encryption | MyBatis interceptor |
| 3 | [12-03-02 Blind Index](../../source/12_System_Security/12-03-02_Blind_Index_Architecture.md) | S2 Blind Index | Searchable encryption |

### SmartAdmin Layer Mapping

| Layer | Responsibility |
|-------|---------------|
| Controller | No encryption awareness (transparent to API consumers) |
| Service | Business logic operates on plaintext (decrypted by interceptor) |
| Manager | @Cacheable for encrypted field cache, @Transactional for key rotation |
| Dao/Interceptor | MyBatis interceptor handles encrypt-on-write, decrypt-on-read |
| KMS | External key management service for key storage and rotation |

### Encryption Architecture

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Field Encryption | AES-256-GCM | Encrypt PII and financial data at column level |
| Blind Index | HMAC-SHA256 | Enable searching on encrypted fields |
| Key Management | External KMS | Centralized key storage with access controls |
| Key Rotation | Scheduled task | Periodic re-encryption without downtime |
| MyBatis Interceptor | Custom plugin | Transparent encrypt/decrypt at persistence layer |

### Verification Checklist

- [ ] Sensitive fields are encrypted
- [ ] KMS integration is successful
- [ ] Key rotation mechanism works
- [ ] Encryption performance is acceptable

### Common Pitfalls

1. **Key Management Chaos**: Hardcoded keys or leakage -- use centralized KMS, never embed keys in code
2. **Weak Algorithms**: Outdated encryption -- enforce AES-256-GCM minimum
3. **Blind Index Collision**: Hash collisions cause query errors -- use high-entropy hash with sufficient output length

---

## 6. Reference Documents

| Area | Document |
|------|----------|
| Multi-Tenant | [06-01 Multi-Tenant](../../source/06_Platform_Governance/06-01_Multi_Tenant.md) |
| RBAC | [06-02 RBAC Permissions](../../source/06_Platform_Governance/06-02_RBAC_Permissions.md) |
| Audit Log | [06-03 Audit Log](../../source/06_Platform_Governance/06-03_Audit_Log.md) |
| Data Security | [12-03 Data Security Standard](../../source/12_System_Security/12-03_Data_Security_Standard.md) |
| Encryption | [12-03-01 Encryption Strategy](../../source/12_System_Security/12-03-01_Encryption_Strategy.md) |
| Blind Index | [12-03-02 Blind Index Architecture](../../source/12_System_Security/12-03-02_Blind_Index_Architecture.md) |

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Source Version**: 4.0.0
