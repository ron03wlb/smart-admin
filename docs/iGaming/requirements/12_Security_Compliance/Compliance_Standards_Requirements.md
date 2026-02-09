# Compliance Standards Requirements

> **Canonical Source**: [source-archive/12_System_Security/12-04](../../source-archive/12_System_Security/12-04_ISO27001_2022_Mapping.md), [12-05](../../source-archive/12_System_Security/12-05_UK_RTS_Security.md)
> **View Type**: Business Requirements
> **Target Audience**: Compliance Officers, Security Managers, Auditors
> **Related Doc**: [ISO27001 Mapping Architecture](../../architecture/12_Security/ISO27001_Mapping.md), [UK RTS Security Architecture](../../architecture/12_Security/UK_RTS_Security.md)
> **Last Synced**: 2026-02-09

---

## 1. ISO 27001:2022 Requirements

### 1.1 Standard Overview

ISO/IEC 27001:2022 is the international standard for Information Security Management Systems (ISMS). The UK Gambling Commission RTS security requirements now reference the 2022 version.

| Version | Control Count | Key Change |
|---------|--------------|------------|
| ISO 27001:2013 | 114 controls | Legacy version |
| ISO 27001:2022 | 93 controls | Consolidated into 4 categories |

### 1.2 Annex A Control Categories

| Category | Count | Description |
|----------|-------|-------------|
| 5. Organizational | 37 | Organizational controls |
| 6. People | 8 | Personnel controls |
| 7. Physical | 14 | Physical controls |
| 8. Technological | 34 | Technical controls |

### 1.3 Key Organizational Controls

| Control | Requirement | Priority |
|---------|------------|----------|
| 5.1 | Information security policy documented and maintained | P0 |
| 5.2 | Information security roles and responsibilities defined | P0 |
| 5.3 | Segregation of duties enforced | P0 |
| 5.7 | Threat intelligence subscription and integration | P2 |
| 5.15 | Access control policies implemented | P0 |
| 5.23 | Cloud service security assessment | P2 |
| 5.30 | ICT business continuity planning | P1 |

### 1.4 Key Technical Controls

| Control | Requirement | Priority |
|---------|------------|----------|
| 8.2 | Privileged access management | P0 |
| 8.3 | Information access restriction (row-level security) | P0 |
| 8.5 | Secure authentication (multi-factor) | P0 |
| 8.9 | Configuration management (Git version control) | P0 |
| 8.12 | Data leakage prevention | P0 |
| 8.15 | Comprehensive logging | P0 |
| 8.16 | System monitoring and alerting | P0 |
| 8.20 | Network security (TLS 1.3, API Gateway) | P0 |
| 8.24 | Cryptography usage standards | P0 |
| 8.25 | Secure development lifecycle | P0 |
| 8.28 | Secure coding guidelines (OWASP) | P0 |

### 1.5 Gap Analysis Actions

| Control | Gap | Recommended Action | Priority |
|---------|-----|-------------------|----------|
| 5.7 | No threat intelligence feed | Integrate threat intelligence subscription | P2 |
| 5.23 | No cloud security assessment | Conduct cloud security evaluation | P2 |
| 6.3 | No security awareness training | Establish training program | P1 |
| 8.1 | No endpoint management | Implement MDM solution | P2 |

### 1.6 Certification Timeline

| Phase | Duration | Goal |
|-------|----------|------|
| Phase 1 | 1 month | Complete gap assessment |
| Phase 2 | 2 months | Implement critical controls |
| Phase 3 | 1 month | Internal audit |
| Phase 4 | Ongoing | Certification audit preparation |

---

## 2. UK RTS Security Requirements

### 2.1 RTS Section 4 Overview

UK Gambling Commission Remote Technical Standards (RTS) Section 4 defines security requirements for remote gambling systems.

### 2.2 RTS 4.1 - Information Security Management

| Requirement | Description |
|------------|-------------|
| 4.1.1 | ISO 27001 compliance required |
| 4.1.2 | Regular risk assessments must be conducted |
| 4.1.3 | Documented security policies must exist |

### 2.3 RTS 4.2 - Clock Synchronization

All systems must use synchronized time sources:
- NTP server synchronization mandatory
- Maximum time deviation < 1 second
- All logs must use UTC timezone

### 2.4 RTS 4.3 - Environment Separation

| Environment | Purpose | Isolation Requirement |
|------------|---------|----------------------|
| Development (DEV) | Development and testing | Fully isolated |
| Testing (UAT) | Acceptance testing | Isolated from production |
| Production (PROD) | Live operations | Strictest controls |

### 2.5 RTS 4.4 - Access Control

| Requirement | Implementation |
|------------|---------------|
| Authentication | Secure token-based system |
| Multi-Factor Authentication | TOTP/WebAuthn required |
| Role-Based Access | RBAC with least privilege |
| Audit Logging | Complete operation records |

### 2.6 RTS 4.5 - Outsourced Development Control

When using third-party development:
- Security requirements in contract terms
- Code review mandatory
- Vulnerability scanning before acceptance
- Security testing sign-off required

### 2.7 RTS 4.6 - Privileged Tool Control

Administrative tools require:
- Independent authentication
- Operation auditing
- Usage restrictions and monitoring

### 2.8 Communication Security

| Requirement | Standard |
|------------|---------|
| Minimum TLS version | TLS 1.2 (TLS 1.3 recommended) |
| Cipher suites | Strong encryption only |
| Certificates | Valid CA-signed certificates |

---

## 3. Combined Compliance Calendar

| Activity | Frequency | Responsible Party |
|----------|-----------|-------------------|
| Security risk assessment | Annual | Security Team |
| Penetration testing | Semi-annual | External Auditor |
| Access control review | Quarterly | IT Security |
| Security awareness training | Annual (+ onboarding) | HR + Security |
| Policy review and update | Annual | Compliance Team |
| Internal audit | Annual | Internal Audit |
| Regulatory filing | Annual | Legal Team |

---

## 4. Acceptance Criteria

1. ISO 27001:2022 gap analysis completed and documented
2. All P0 controls implemented and evidenced
3. UK RTS Section 4 requirements fully mapped and addressed
4. Environment separation enforced (DEV/UAT/PROD)
5. Clock synchronization within 1 second across all systems
6. Administrative tools have independent authentication and full audit trails
7. TLS 1.2+ enforced on all communications
8. Compliance calendar established with regular cadence
