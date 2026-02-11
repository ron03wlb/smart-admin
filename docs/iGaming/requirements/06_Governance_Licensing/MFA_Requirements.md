# MFA Requirements (Multi-Factor Authentication)

> **Canonical Source**: [06-06 MFA Implementation](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Architecture**: [MFA Technical Architecture](../../architecture/06_Platform_Core/MFA_Technical.md)
> **Last Synced**: 2026-02-09

**Related Source Documents**:
- [06-06-01 MFA Architecture Design](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md)
- [06-06-04 Compliance & Audit](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md)

---

## Business Value

This requirements document delivers strategic value by:
- **Risk Reduction**: MFA reduces account takeover risk by 47% (CVSS 8.1 → 4.3), protecting high-privilege accounts from phishing attacks
- **Financial Protection**: Prevents credential-based fraud that caused $237K USD direct loss and 120K player data exposure in recent industry incidents
- **Regulatory Compliance**: Satisfies mandatory MFA requirements for MGA, PCI DSS 4.0, and GDPR, avoiding fines up to EUR 20M or 4% global revenue
- **Operational Efficiency**: Differentiates mandatory MFA (5 high-risk roles) from optional MFA (3 low-risk roles) to balance security with user experience

---

## Acceptance Criteria

- [ ] TOTP (Google Authenticator) functions as primary MFA method for 90% of users
- [ ] SMS OTP functions as backup method when TOTP is unavailable
- [ ] Backup codes (10 one-time use, 8-digit format) are generated and displayed during MFA setup
- [ ] Super Admin, Finance Manager, Risk Control, Database Admin, and DevOps have mandatory MFA enforcement
- [ ] Customer Service, Marketing, and Content Editor have optional MFA with recommendation banner
- [ ] Account locks for 15 minutes after 3 consecutive MFA failures
- [ ] Device trust feature allows 30-day MFA skip for verified devices
- [ ] Device loss recovery completes within 24-48 business hours with human review
- [ ] All 10 audit event types (MFA_SETUP_INIT through DEVICE_TRUSTED) are logged with required fields
- [ ] 4 anomaly detection rules trigger appropriate alerts (multiple failures, geographic anomaly, backup code abuse, high-risk MFA disabled)

---

## 1. Executive Summary

This document outlines the **Multi-Factor Authentication (MFA)** requirements for the SmartAdmin iGaming platform backend administration system. MFA is critical for protecting high-privilege accounts from unauthorized access, preventing financial fraud, and meeting regulatory compliance requirements.

**Key Statistics**:
- MFA reduces account takeover risk by **47%** (CVSS 8.1 → 4.3)
- Protects against phishing attacks (primary attack vector in iGaming)
- Required for regulatory compliance (MGA, PCI DSS, GDPR)

---

## 2. Business Requirements

### 2.1 Why Backend Users Need MFA

Backend administrators have high-privilege access that, if compromised, can result in severe losses:

| Role | High-Risk Operations | Potential Loss |
|------|---------------------|----------------|
| **Super Admin** | Modify system config, delete users, change permissions | System outage, data breach |
| **Finance Manager** | Adjust player balances, approve withdrawals, modify transactions | Direct financial loss ($10K-$1M+) |
| **Risk Control** | Modify risk rules, whitelist/blacklist management | Fraud losses, compliance violations |
| **Customer Service** | View player PII, modify player information | Privacy breach, GDPR fines |

**Real-World Cases (iGaming Industry)**:
- **Case A (2023)**: Finance Manager account phished, attacker approved 47 fraudulent withdrawals, **$237,000 USD** loss
- **Case B (2024)**: Super Admin account compromised via credential stuffing, resulted in **120,000 player records exposed**

### 2.2 Risk Analysis (STRIDE Framework)

| Threat Type | Attack Scenario | Password Only | Password + MFA |
|------------|-----------------|---------------|----------------|
| **Spoofing** | Phishing website steals password | Compromised | Attacker cannot obtain TOTP |
| **Tampering** | Session Hijacking | Session can be hijacked | MFA binds to device fingerprint |
| **Repudiation** | Insider malicious action denial | Difficult to prove | MFA audit trail |
| **Information Disclosure** | Password leak (database breach) | Hash can be brute-forced | TOTP secret separately encrypted |
| **Denial of Service** | Brute force login | Mitigated by IP throttling | MFA increases attack difficulty |
| **Elevation of Privilege** | Lateral movement attack | Password may be guessed | High-privilege roles require MFA |

**Risk Score (CVSS 3.1)**:
- Without MFA: **8.1 (High)**
- With MFA: **4.3 (Medium)**
- Risk reduction: **47%**

---

## 3. Supported Authentication Methods

### 3.1 Method Comparison

| Method | Principle | Security | UX | Cost | Dependency |
|--------|-----------|----------|-----|------|------------|
| **TOTP** | Time-based OTP (Google Authenticator) | 5/5 | 4/5 | 5/5 | None (offline) |
| **SMS OTP** | SMS verification code | 3/5 | 5/5 | 3/5 | SMS gateway |
| **Email OTP** | Email verification code | 3/5 | 3/5 | 4/5 | Email service |
| **Hardware Token (YubiKey)** | Physical device key generation | 5/5 | 2/5 | 2/5 | Hardware purchase |

### 3.2 Recommended Three-Tier Architecture

| Priority | Method | Use Case | Security Level |
|----------|--------|----------|----------------|
| **P0** | TOTP | Daily login (90% of users) | 5/5 |
| **P1** | SMS OTP | Device lost / TOTP unavailable | 3/5 |
| **P2** | Email OTP | SMS unavailable (international roaming) | 3/5 |
| **P3** | Backup Codes | All methods unavailable (offline recovery) | 4/5 |

### 3.3 Method Selection Rationale

**Why TOTP as Primary Method**:
- **Highest Security**: RFC 6238 standard, HMAC-SHA1 algorithm
- **Zero Dependencies**: Works offline, no third-party services required
- **Lowest Cost**: Free apps (Google Authenticator, Authy, 1Password)
- **Widely Supported**: Industry standard for backend systems

**Why SMS OTP as Backup Only**:
- Security concerns: Vulnerable to SIM Swap attacks, SS7 hijacking
- NIST SP 800-63B deprecated SMS OTP in 2016
- Cost: $0.05-$0.10 per message
- Use only when TOTP unavailable

---

## 4. MFA Requirements by Jurisdiction

### 4.1 Regulatory Compliance Matrix

| Regulator | Standard | MFA Requirement | Penalty |
|-----------|----------|-----------------|---------|
| **MGA (Malta)** | MGA/B2C/183/2010 | **Mandatory** for high-privilege accounts | License revocation, EUR 50K-500K fine |
| **UKGC (UK)** | LCCP 10.1.1 | **Recommended** (Risk-based Authentication) | License suspension, GBP 100K-2M fine |
| **Curacao eGaming** | Gaming Control Board | Not explicitly required but audited | Audit failure, license renewal issues |
| **GDPR (EU)** | Art. 32 | Requires "appropriate technical measures" | EUR 20M or 4% global revenue |
| **PCI DSS 4.0** | Requirement 8.3.1 | **Mandatory** for all administrators | Cannot process card transactions |

### 4.2 MGA Compliance Checklist

1. All Super Admin, Finance Manager, Risk Control must enable MFA
2. MFA Secret must be encrypted using NIST-approved 256-bit encryption standards

→ **[MFA Secret Encryption](../../architecture/06_Platform_Core/MFA_Technical.md#secret-encryption)** — See architecture layer for approved algorithm details
3. Audit logs must record all MFA events (setup, verify, failure)
4. Recovery mechanism must require secondary verification (no self-service)
5. MFA implementation must pass penetration testing

---

## 5. Role-Based MFA Policy

### 5.1 Mandatory MFA Roles (5 Categories)

| Role | High-Risk Operations | MFA Requirement | Notes |
|------|---------------------|-----------------|-------|
| **Super Admin** | Modify system config, delete users, change permissions | **Mandatory** | No exemptions |
| **Finance Manager** | Adjust player balances, approve withdrawals | **Mandatory** | No exemptions |
| **Risk Control** | Modify risk rules, whitelist/blacklist | **Mandatory** | No exemptions |
| **Database Admin** | Direct production database access | **Mandatory** | Additional YubiKey recommended |
| **DevOps** | Deploy code, modify server config | **Mandatory** | Additional YubiKey recommended |

### 5.2 Optional MFA Roles (3 Categories)

| Role | Primary Responsibilities | MFA Requirement | Recommendation |
|------|-------------------------|-----------------|----------------|
| **Customer Service** | Query player data, respond to tickets | Optional | **Recommended** |
| **Marketing** | View statistics, edit campaign pages | Optional | **Suggested** |
| **Content Editor** | Edit announcements, news, help docs | Optional | May skip |

### 5.3 Policy Decision Rationale

**Why Differentiated Policy (Mandatory vs Optional)**:

| Criteria | Super Admin | Finance | Customer Service |
|----------|-------------|---------|------------------|
| Risk Exposure | 5/5 (system config) | 5/5 (player balance) | 3/5 (view only) |
| Operation Reversibility | No | No | Yes (audit trail) |
| Compliance Requirement | PCI DSS Required | PCI DSS Required | GDPR Recommended |

**Decision Matrix**:

| Option | Security | UX | Compliance | Score (40%+30%+30%) |
|--------|----------|-----|------------|---------------------|
| A (All Mandatory) | 5 | 2 | 5 | 4.1 |
| **B (Differentiated)** | 5 | 4 | 5 | **4.8** |
| C (All Optional) | 2 | 5 | 2 | 2.8 |

**Conclusion**: Option B (High-risk mandatory + Low-risk optional) selected.

---

## 6. Recovery Procedures

### 6.1 Backup Codes

**Purpose**: Allow login when TOTP device is unavailable (phone lost, app deleted, device damaged).

**Specifications**:
- **Quantity**: 10 one-time use codes
- **Format**: 8 digits (e.g., 1234-5678)
- **Generation**: SecureRandom, cryptographically secure
- **Storage**: NIST-approved 256-bit encryption, stored with user MFA record

→ **[Encryption Implementation Details](../../architecture/06_Platform_Core/MFA_Technical.md#secret-encryption)** — See architecture layer for approved algorithm details
- **Usage**: Each code can only be used once

**User Guidance**:
1. Download backup codes immediately after MFA setup
2. Store in secure location (password manager, printed in safe)
3. When remaining codes <= 2, regenerate full set
4. Regeneration requires current TOTP verification

### 6.2 Device Lost Recovery Flow

**Scenario**: User loses phone, cannot use TOTP, has no backup codes.

**Recovery Process (Multi-Factor Verification)**:

1. **Submit Recovery Request**
   - Upload identity document (passport/driver's license/ID card)
   - Provide reason for request

2. **Identity Verification**
   - Email OTP verification (to registered email)
   - SMS OTP verification (to registered phone)
   - Security questions verification (3 preset questions)

3. **Human Review**
   - Security Team reviews request
   - Verifies identity document against user record
   - Approves or rejects within 24 hours

4. **MFA Reset**
   - Security Team force resets MFA
   - New TOTP Secret generated
   - Sent to user via secure email
   - User scans new QR code to reactivate

**SLA**: Recovery completed within 24-48 business hours.

### 6.3 Emergency Contact Verification

**Enhanced Security Option**: Users can designate 1-2 emergency contacts (colleagues/managers) during MFA setup.

**Flow**:
1. User submits recovery request
2. System sends verification email to emergency contacts
3. Emergency contact clicks verification link (confirms legitimate request)
4. Security Team receives notification, expedites review

---

## 7. User Experience Requirements

### 7.1 Trust Device Feature

**Purpose**: Reduce MFA friction for frequent logins from same device.

**Specifications**:
- **Trust Duration**: 30 days
- **Mechanism**: Device fingerprint + HttpOnly secure cookie
- **Opt-in**: User chooses "Trust this device" during MFA verification
- **Revocation**: User can revoke trusted devices from settings

**Security Measures**:
- HttpOnly cookie (cannot be read by JavaScript)
- SameSite=Strict (prevents CSRF)
- Bound to IP + User-Agent + Fingerprint triplet
- Automatically revoked on password change

### 7.2 Failure Handling

| Scenario | Behavior | User Message |
|----------|----------|--------------|
| TOTP code incorrect | Increment failure count | "Verification code incorrect. X attempts remaining." |
| 3 consecutive failures | Lock account 15 minutes | "Too many attempts. Account locked for 15 minutes." |
| MFA session expired | Redirect to password login | "Session expired. Please log in again." |
| Server time sync issue | Internal error | "System error. Please try again in 30 seconds." |

### 7.3 First-Time Setup Flow

**Trigger Conditions**:
- First login for mandatory MFA role
- User enables MFA from personal settings
- Admin forces MFA enable (security policy)

**Setup Steps**:
1. Display QR code for Google Authenticator
2. Show plaintext secret for manual entry
3. Generate and display 10 backup codes
4. Require user to enter current TOTP to verify setup
5. Activate MFA upon successful verification

---

## 8. Audit Requirements

### 8.1 Event Types to Record

| Event Type | Level | Trigger | Retention |
|------------|-------|---------|-----------|
| `MFA_SETUP_INIT` | INFO | User starts MFA setup | Permanent |
| `MFA_ENABLED` | INFO | MFA activated successfully | Permanent |
| `MFA_DISABLED` | CRITICAL | MFA disabled by user/admin | Permanent |
| `MFA_LOGIN_SUCCESS` | INFO | MFA verification successful | 90 days |
| `MFA_LOGIN_FAILED` | WARNING | TOTP verification failed | Permanent |
| `MFA_LOCKED` | CRITICAL | Account locked after 3 failures | Permanent |
| `BACKUP_CODE_USED` | WARNING | Login with backup code | Permanent |
| `BACKUP_CODE_REGENERATE` | INFO | Backup codes regenerated | Permanent |
| `MFA_FORCE_RESET` | CRITICAL | Security Team force reset MFA | Permanent |
| `DEVICE_TRUSTED` | INFO | User trusted device | Permanent |

### 8.2 Audit Log Fields

Each audit log entry must include:
- User ID
- Event type and level
- IP address
- User agent
- Device fingerprint
- Timestamp (UTC)
- Additional details (JSON format)

### 8.3 Anomaly Detection Rules

| Rule | Detection Logic | Action |
|------|-----------------|--------|
| Multiple MFA failures | >= 3 failures in 5 minutes | Lock 15 min, alert Security Team |
| Geographic anomaly | Login from different countries within 1 hour | Alert Security Team, require re-verification |
| Frequent backup code use | >= 3 backup code uses in 7 days | Warn user, suggest TOTP reset |
| High-risk MFA disabled | MFA disabled for Super Admin/Finance/Risk | **Immediate alert to CTO/CISO** |

---

## 9. Compliance Reporting Requirements

### 9.1 Required Reports

| Report | Frequency | Audience | Content |
|--------|-----------|----------|---------|
| MFA Adoption Report | Monthly | Security Team | Adoption rate by role, pending setups |
| MFA Event Summary | Weekly | Security Team | Login success/failure ratio, anomalies |
| High-Risk Action Audit | On-demand | Compliance Team | All MFA_DISABLED and MFA_FORCE_RESET events |
| Penetration Test Results | Annually | Regulators | MFA bypass attempt results |

### 9.2 Regulatory Submission

For MGA/UKGC audits, the following must be demonstrable:
1. All mandatory MFA roles have active MFA
2. MFA secrets are encrypted at rest
3. Audit logs are retained per policy
4. Recovery procedures require human verification
5. Penetration test passed with no High/Critical findings

---

## 10. Implementation Roadmap

| Phase | Timeline | Goals | Deliverables |
|-------|----------|-------|--------------|
| **Phase 1** | Week 1-2 | Core TOTP functionality | TOTP login, mandatory role check, audit logs |
| **Phase 2** | Week 3 | Backup & recovery | Backup codes, device trust, recovery flow |
| **Phase 3** | Week 4 | Advanced security | SMS OTP backup, anomaly detection, compliance reports |

---

## 11. FAQ (Frequently Asked Questions)

**Q1: User reports "TOTP code always incorrect"?**
- 99% caused by time sync issues
- Check server NTP status
- Have user verify device auto-time setting
- Temporarily increase validation window (+-2 instead of +-1)

**Q2: User lost phone with no backup codes?**
- Execute device lost recovery flow (Section 6.2)
- Requires identity verification + Security Team approval
- SLA: 24-48 business hours

**Q3: Why not use SMS OTP as primary method?**
- Vulnerable to SIM Swap attacks
- Vulnerable to SS7 protocol hijacking
- NIST deprecated in 2016
- Use only as backup when TOTP unavailable

**Q4: TOTP code expires during entry (30-second window)?**
- System allows +-1 window (90 seconds effective)
- Previous window code valid
- Next window code valid
- No user action required

**Q5: Should we support hardware tokens (YubiKey)?**
- Phase 3 optional feature
- Recommended only for Super Admin, CTO, CFO
- Cost: $50 per device
- Provides highest security (anti-phishing, anti-MITM)

---

## Related Documentation

→ **[TOTP & WebAuthn Implementation](../../architecture/06_Platform_Core/TOTP_WebAuthn_Implementation.md)** - TOTP algorithm implementation (RFC 6238), secret generation, QR code rendering, backup code encryption (AES-256-GCM), trusted device fingerprinting, and audit log schemas

---

**End of Document**
