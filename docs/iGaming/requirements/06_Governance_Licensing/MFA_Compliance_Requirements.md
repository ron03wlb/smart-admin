# MFA Compliance & Audit Requirements

> **Canonical Source**: [06-06-04_Compliance_Audit.md](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md)
> **Audience**: Executives, Compliance Officers
> **Related Doc**: [MFA_Compliance_Technical.md](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (Backup Code AES-256-GCM encryption, Identity Document S3 upload implementation, Audit Log JSONB format + Kafka integration, Anomaly Detection rule implementations, HTTP status codes, Redis cache configuration) moved to Architecture layer. This document focuses on business policies, compliance mandates, and operational procedures only.

---

## Business Value

MFA Compliance & Audit delivers critical business value by:
- **Regulatory Compliance**: Meets PCI DSS mandatory requirements (administrative accounts MFA), GDPR Art. 30 (audit logging), MGA security measures, and NIST SP 800-63B (TOTP primary, SMS backup only) to avoid license suspension and fines
- **Risk-Based Access Control**: Differentiated MFA strategy (Option B) achieves 4.8/5.0 weighted score by balancing security (5/5) for high-risk roles with user experience (4/5) for low-risk roles, avoiding disruption to 80%+ of staff
- **Fraud Prevention**: Anomaly detection rules (geographic location, multiple failures, frequent backup code usage) trigger automatic account lockouts and security alerts within minutes of suspicious activity
- **Operational Continuity**: Multi-layer backup strategy (10 backup codes, device trust 30 days, SMS OTP fallback, emergency contact verification) ensures authorized users maintain access while blocking attackers

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| High-Risk Role MFA Coverage | 100% enforcement | Super Admin, Finance Manager, Risk Control, Database Admin, DevOps all enabled (no exemptions) |
| Regulatory Compliance Rate | 100% | Pass all PCI DSS, GDPR Art. 30, MGA, NIST SP 800-63B audit requirements |
| Anomaly Detection Response Time | Alert within 5 minutes | Time from triggering condition to Security Team notification |
| Device Loss Recovery SLA | ≤24 hours for approval | Time from recovery request submission to Security Team review completion |
| Backup Code Availability | 90% of users maintain ≥3 unused codes | Percentage of active MFA users with sufficient backup codes remaining |
| Audit Log Retention Compliance | 100% | All CRITICAL and WARNING events retained permanently, INFO events 90 days |
| False Positive Rate (Geo-Anomaly) | ≤5% | Geographic location alerts that are legitimate user travel, not fraud |
| MFA Lockout Incident Rate | ≤2% per month | Percentage of users locked out due to 3 consecutive MFA failures |

---

## 1. Purpose

This document defines the business compliance requirements, audit policies, regulatory mandates, and role-based MFA policies for the SmartAdmin iGaming platform. It covers backup and recovery procedures, role-level enforcement rules, audit event tracking, and implementation roadmap from a business perspective.

---

## 2. Backup Code Policy

### 2.1 Problem Statement

Users may lose access to TOTP-based MFA due to phone loss, Google Authenticator uninstallation, or device damage.

### 2.2 Backup Code Specifications

| Attribute | Requirement |
|-----------|------------|
| **Quantity** | 10 one-time backup codes per user |
| **Format** | 8-digit numeric, displayed as XXXX-XXXX (e.g., 1234-5678) |
| **Usage** | Each code is single-use; once consumed, it cannot be reused |
| **Storage** | Codes stored securely with used/unused status tracking |
| **Low Code Warning** | When remaining unused codes drops to 2 or fewer, prompt user to regenerate |
| **Regeneration** | Requires active TOTP verification before new codes can be generated |

→ **[Backup Code Storage & Encryption](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#backup-code-encryption)** - AES-256-GCM encryption algorithm, secure generation, database schema, status tracking implementation

### 2.3 Backup Code Login Policy

1. User submits a backup code in place of TOTP during MFA verification
2. System checks the code against the stored list
3. If the code is valid and unused:
   - Mark it as used with a timestamp
   - Grant access
   - If remaining codes are 2 or fewer, display a warning recommending regeneration
4. If the code is invalid or already used:
   - Deny access
   - Record an audit log entry (BACKUP_CODE_INVALID)

### 2.4 Backup Code Management

**View remaining codes**: Users can view how many backup codes remain (total, used, remaining, last used date) without seeing the actual codes.

**Regenerate codes**: Users can regenerate a full set of new backup codes, which invalidates all previous codes. This action requires active TOTP verification.

---

## 3. Device Loss Recovery Policy

### 3.1 Scenario

User has lost their phone, cannot use TOTP, and has no remaining backup codes.

### 3.2 Multi-Step Recovery Process

The recovery process requires passing all of the following verification steps:

| Step | Verification Method | Description |
|------|-------------------|-------------|
| 1 | **Identity Document** | User submits passport, driver's license, or national ID card photo |
| 2 | **Email Verification** | OTP sent to registered email address |
| 3 | **Phone Verification** | SMS OTP sent to registered phone number |
| 4 | **Security Questions** | Answer 3 pre-configured security questions |
| 5 | **Human Review** | Security Team reviews and approves the request |

### 3.3 Recovery Outcome

- If all verifications pass and Security Team approves:
  - Old MFA configuration is deleted
  - A new TOTP secret is generated and sent to the user
  - User scans a new QR code to re-activate MFA
- If any verification fails:
  - Recovery request is rejected
  - User is directed to contact customer service

### 3.4 Recovery Request Tracking

Each recovery request must be tracked with:
- User ID and reason for recovery
- Identity document upload
- Review status (PENDING / APPROVED / REJECTED)
- Reviewer ID and review timestamp
- Full audit trail

→ **[Identity Document Upload Implementation](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#identity-document-upload)** - S3 storage configuration, file validation, secure upload API, document verification workflow

---

## 4. Emergency Contact Verification

### 4.1 Enhanced Security Measure

Users may designate 1-2 emergency contacts (colleagues or managers) during MFA setup.

### 4.2 Emergency Contact Recovery Flow

1. User submits a recovery request
2. System sends a verification email to designated emergency contacts
3. Emergency contacts click a verification link to confirm the request is legitimate
4. Security Team receives notification and proceeds with accelerated review

### 4.3 Contact Types

| Type | Description |
|------|------------|
| **COLLEAGUE** | Same-level team member who can verify identity |
| **MANAGER** | Direct supervisor with authority to confirm |

---

## 5. Role-Based MFA Strategy

### 5.1 Mandatory MFA Roles (High Risk)

The following roles **must** have MFA enabled. No exemptions are permitted.

| Role | High-Risk Operations | MFA Requirement | Notes |
|------|---------------------|-----------------|-------|
| **Super Admin** | Modify system configuration, delete users, change permissions | Mandatory | No exemption |
| **Finance Manager** | Adjust player balances, approve withdrawals | Mandatory | No exemption |
| **Risk Control** | Modify risk rules, whitelist/blacklist management | Mandatory | No exemption |
| **Database Admin** | Direct production database access | Mandatory | Additional YubiKey required |
| **DevOps** | Deploy code, modify server configuration | Mandatory | Additional YubiKey required |

**Enforcement**: Users assigned to mandatory MFA roles who have not enabled MFA will be forced to the MFA setup page upon login. They cannot bypass this step.

### 5.2 Optional MFA Roles (Low Risk)

The following roles may optionally enable MFA.

| Role | Primary Duties | MFA Requirement | Recommendation |
|------|---------------|-----------------|---------------|
| **Customer Service** | Query player data, respond to tickets | Optional | Recommended |
| **Marketing** | View statistics, edit campaign pages | Optional | Suggested |
| **Content Editor** | Edit announcements, news, help documents | Optional | May skip |

**Recommendation**: Users with optional MFA will see a non-blocking banner recommending MFA setup upon login. The banner is dismissible.

### 5.3 Decision Rationale

The MFA strategy was chosen based on a weighted decision matrix across three dimensions:

**Dimension 1: Risk Exposure**
- Super Admin / Finance: 5/5 (can modify system configuration / adjust balances)
- Customer Service: 3/5 (query-only permissions)

**Dimension 2: Operation Reversibility**
- Super Admin / Finance: NOT reversible
- Customer Service: Reversible (auditable actions)

**Dimension 3: Regulatory Compliance**
- Super Admin / Finance: REQUIRED by PCI DSS
- Customer Service: RECOMMENDED by GDPR

**Options Evaluated**:

| Option | Security | User Experience | Compliance | Weighted Score (40%+30%+30%) |
|--------|---------|----------------|-----------|------------------------------|
| A - All roles mandatory MFA | 5 | 2 | 5 | **4.1** |
| **B - Differentiated (high-risk mandatory, low-risk optional)** | **5** | **4** | **5** | **4.8** |
| C - All roles optional MFA | 2 | 5 | 2 | **2.8** |

**Conclusion**: Option B selected. This balances security protection for high-value targets without disrupting daily operations for lower-risk roles.

### 5.4 Implementation Guidelines

**Mandatory MFA (5 role categories)**:
- Super Admin
- Finance Manager
- Risk Control
- Database Admin
- DevOps

**Optional MFA (3 role categories)**:
- Customer Service (recommended)
- Marketing (suggested)
- Content Editor (may skip)

---

## 6. Audit Event Requirements

### 6.1 Compliance Mandate

All MFA-related operations must be recorded in an audit log in compliance with GDPR Article 30 requirements.

### 6.2 Event Type Catalog

| Event Type | Severity | Trigger | Retention Period |
|-----------|----------|---------|-----------------|
| `MFA_SETUP_INIT` | INFO | User begins MFA setup | Permanent |
| `MFA_ENABLED` | INFO | MFA activation successful | Permanent |
| `MFA_DISABLED` | CRITICAL | User or admin disables MFA | Permanent |
| `MFA_LOGIN_SUCCESS` | INFO | MFA verification successful at login | 90 days |
| `MFA_LOGIN_FAILED` | WARNING | TOTP verification failed | Permanent |
| `MFA_LOCKED` | CRITICAL | Account locked after 3 consecutive failures | Permanent |
| `BACKUP_CODE_USED` | WARNING | Login using backup code | Permanent |
| `BACKUP_CODE_REGENERATE` | INFO | Backup codes regenerated | Permanent |
| `MFA_FORCE_RESET` | CRITICAL | Security Team force-resets user MFA | Permanent |
| `DEVICE_TRUSTED` | INFO | User trusts a device | Permanent |

### 6.3 Audit Log Content Requirements

Each audit entry must capture:
- User ID
- Event type and severity level
- IP address
- User agent string
- Device fingerprint
- Additional details in structured format (e.g., reason, attempt count, remaining attempts)
- Timestamp

→ **[Audit Log Implementation](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#audit-log-implementation)** - JSONB format specification, Kafka event streaming integration, PostgreSQL storage schema, retention policy automation

---

## 7. Anomaly Detection Rules

### 7.1 Rule 1: Multiple MFA Failures in Short Period

- **Condition**: 3 or more MFA failures within 5 minutes for the same user
- **Actions**:
  - Lock account for 15 minutes
  - Send alert to Security Team
  - Send email notification to user about abnormal login attempts

### 7.2 Rule 2: Abnormal Geographic Location Login

- **Condition**: Successful MFA logins from different countries within 1 hour for the same user
- **Actions**:
  - Send alert to Security Team for manual review
  - Disable trusted device status, requiring full MFA on next login

### 7.3 Rule 3: Frequent Backup Code Usage

- **Condition**: 3 or more backup code uses within 7 days for the same user
- **Actions**:
  - Warn user: "You are frequently using backup codes; please reconfigure TOTP"
  - Prompt: "Has your device been lost? Click here to recover MFA"

### 7.4 Rule 4: MFA Disabled for High-Risk Role

- **Condition**: MFA disabled for any user in a mandatory MFA role (Super Admin, Finance Manager, Risk Control)
- **Actions**:
  - **Immediately alert CTO / CISO**
  - Manual review: "Why was MFA disabled for a high-risk account?"
  - If not initiated by the account holder, treat as a security incident and lock the account immediately

→ **[Anomaly Detection Implementation](../../architecture/06_Platform_Core/MFA_Compliance_Technical.md#anomaly-detection-rules)** - Java rule engine implementation, threshold configuration, alert triggering logic, geographic location detection algorithm

---

## 8. Implementation Roadmap

### 8.1 Phase 1: Core TOTP Functionality (Week 1-2, 10 working days)

**Objective**: Implement TOTP authentication (Google Authenticator) with mandatory MFA for high-risk roles.

**Task Summary**:

| Task | Owner | Duration | Dependencies |
|------|-------|----------|-------------|
| Design database table structures | Backend Dev | 0.5 day | - |
| Implement TOTP generation and verification logic | Backend Dev | 1 day | - |
| Implement QR code generation | Backend Dev | 0.5 day | TOTP logic |
| Implement MFA setup flow | Backend Dev | 2 days | TOTP + QR |
| Implement MFA login flow (two-stage auth) | Backend Dev | 2 days | MFA setup |
| Implement audit log recording | Backend Dev | 1 day | Setup + login flows |
| Frontend MFA setup page | Frontend Dev | 2 days | MFA setup API |
| Frontend MFA login page | Frontend Dev | 1.5 days | MFA login API |
| Unit tests + integration tests | QA | 2 days | All above |
| Deploy to test environment + verify | DevOps | 0.5 day | All above |

**Deliverables**:
- TOTP authentication (Google Authenticator)
- Mandatory MFA check for high-risk roles
- Audit log recording (INFO / WARNING / CRITICAL)
- Unit test coverage >= 80%

### 8.2 Phase 2: Backup & Recovery Mechanisms (Week 3, 5 working days)

**Objective**: Implement backup codes, device trust, and device loss recovery flow.

**Task Summary**:

| Task | Owner | Duration | Dependencies |
|------|-------|----------|-------------|
| Implement backup code generation and verification | Backend Dev | 1 day | Phase 1 |
| Implement device trust mechanism (fingerprint-based) | Backend Dev | 1.5 days | Phase 1 |
| Implement device loss recovery flow (human review) | Backend Dev | 1.5 days | Phase 1 |
| Frontend backup code management page | Frontend Dev | 1 day | Backup code API |
| Testing and verification | QA | 1 day | All above |

**Deliverables**:
- 10 backup codes (8-digit, single-use)
- Device trust for 30 days (skip MFA)
- Device loss recovery flow (Security Team manual review)

### 8.3 Phase 3: Advanced Security Features (Week 4, 5 working days)

**Objective**: Implement SMS OTP backup, anomaly detection, and compliance audit reports.

**Task Summary**:

| Task | Owner | Duration | Dependencies |
|------|-------|----------|-------------|
| Integrate SMS OTP gateway (Twilio / AWS SNS) | Backend Dev | 1 day | Phase 1 |
| Implement anomaly detection rules (4 rules) | Backend Dev | 1.5 days | Phase 1 + 2 |
| Implement compliance audit report generation (GDPR / PCI DSS) | Backend Dev | 1 day | Phase 1 + 2 |
| Frontend SMS OTP login flow | Frontend Dev | 0.5 day | SMS OTP API |
| Penetration testing | Security Team | 2 days | All above |

**Deliverables**:
- SMS OTP backup method
- Anomaly detection and automatic alerts
- Compliance audit reports (exportable PDF)
- Pass penetration testing (no High / Critical vulnerabilities)

---

## 9. Frequently Asked Questions

### Q1: Users report "TOTP code always incorrect"

**Root Cause**: 99% of cases are time synchronization issues.

**Resolution**:
1. Verify server NTP status
2. Have user check device time settings (enable automatic time)
3. Temporarily widen verification window from +/-1 to +/-2 (allowing +/-60 second offset)

### Q2: User lost phone and has no backup codes

**Resolution**: Follow the Device Loss Recovery Process (Section 3):
1. Submit recovery request with identity documents
2. Verify email + phone + security questions
3. Security Team manual review
4. Force reset MFA (generate new secret)

### Q3: Why not use SMS OTP as the primary method?

**Security concerns**:
- Vulnerable to SIM Swap attacks
- Vulnerable to SS7 hijacking (telecom protocol vulnerability)
- NIST deprecated SMS OTP as primary factor (2016)
- SMS OTP is used only as a backup method in this system

### Q4: What if TOTP code expires while user is typing?

**Mitigation**: The system allows a +/-1 window (90-second effective validity):
- Previous window code (-30s): valid
- Current window code: valid
- Next window code (+30s): valid

### Q5: Is hardware token (YubiKey) support needed?

**Phase 3 optional feature** (Super Admin only):
- Highest security (phishing-resistant, MITM-resistant)
- High cost ($50/device)
- Logistics complexity (remote employees need shipping)
- Recommendation: Purchase only for Super Admin, CTO, CFO

---

## 10. Test Scenarios

The following test scenarios must be verified for compliance acceptance:

| Test ID | Scenario | Expected Result |
|---------|---------|----------------|
| TC-MFA-001 | User first-time TOTP setup (scan QR code) | Secret securely stored, Status = PENDING |
| TC-MFA-002 | User enters correct TOTP code to activate MFA | Status changes to ACTIVE, audit log recorded |
| TC-MFA-003 | User enters incorrect TOTP code | Access denied, error counter incremented |
| TC-MFA-004 | User fails TOTP 3 consecutive times | Account locked 15 minutes, security alert triggered |
| TC-MFA-005 | User logs in with backup code | Login succeeds, backup code marked as used |
| TC-MFA-006 | User reuses an already-consumed backup code | Access denied, audit log recorded |
| TC-MFA-007 | User selects "Trust this device for 30 days" | Trust token stored (TTL 30 days) |
| TC-MFA-008 | Login from trusted device (MFA skipped) | Token issued directly without TOTP prompt |
| TC-MFA-009 | Server time offset +25 seconds | Verification succeeds (+/-1 window) |
| TC-MFA-010 | Server time offset +65 seconds | Verification fails (exceeds +/-1 window) |
| TC-MFA-011 | Security Team force-resets user MFA | Old secret deleted, new secret generated |
| TC-MFA-012 | User logs in from different countries within 1 hour | Anomaly detection triggered, alert sent |

---

## 11. Regulatory Compliance Mapping

| Regulation | Requirement | Coverage |
|-----------|------------|---------|
| **PCI DSS** | Administrative accounts must use MFA | Covered by mandatory MFA for high-risk roles (Section 5.1) |
| **GDPR Art. 30** | All processing activities must be logged | Covered by audit log requirements (Section 6) |
| **MGA** | Appropriate security measures for admin access | Covered by differentiated MFA strategy (Section 5) |
| **NIST SP 800-63B** | SMS OTP not recommended as primary factor | Covered - TOTP as primary, SMS as backup only (Section 9, Q3) |

---

## 12. Related Documents

- MFA Architecture Design - Business requirements and method selection
- TOTP & WebAuthn Implementation - TOTP algorithm details
- Login & Recovery Flow - Authentication flow design

### Technical Implementation

→ **[MFA Compliance Validation](../../architecture/06_Platform_Core/MFA_Compliance_Validation.md)** - Backup code storage encryption, identity document upload workflows, audit log implementation (JSONB + Kafka), anomaly detection algorithms (failed attempts, geolocation, backup code abuse)
