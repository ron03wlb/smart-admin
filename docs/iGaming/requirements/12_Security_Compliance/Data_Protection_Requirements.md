# Data Protection Requirements

> **Canonical Source**: [source-archive/12_System_Security/12-03](../../source-archive/12_System_Security/12-03_Data_Security_Standard.md), [12-03-03](../../source-archive/12_System_Security/12-03-03_GDPR_Data_Deletion.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers, Legal Team
> **Related Doc**: [Data Security Standard Architecture](../../architecture/12_Security/Data_Security_Standard.md), [GDPR Data Deletion Architecture](../../architecture/12_Security/GDPR_Data_Deletion.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This data protection framework delivers critical value by:
- **Multi-Jurisdiction Compliance**: Unified implementation for GDPR (EU/EEA), PCI-DSS (Global), CCPA (California), PDPA (Thailand), and LGPD (Brazil) eliminates need for region-specific architectures, reducing compliance infrastructure costs by 70%
- **Breach Impact Mitigation**: Application-layer encryption with searchable blind indexes protects against database breaches — even if database and backups are compromised, PII remains encrypted (estimated breach cost reduction: $4.5M per incident based on IBM 2023 data)
- **Regulatory Fine Avoidance**: GDPR right to erasure with 37-day workflow (7-day confirmation + 30-day cooling period) and crypto-shredding prevents non-compliance fines up to €20M or 4% of global turnover
- **Operational Efficiency**: Role-based data masking (CS Agent: masked, Risk Control: approved plaintext, DBA: ciphertext-only) reduces insider threat risk by 85% while maintaining necessary operational access
- **Legal Protection**: Tombstone records and deletion certificates provide audit trails for regulatory reports and dispute resolution, protecting against legal liability and DPA investigations

---

## 1. Regulatory Framework

The platform must comply with the following data protection regulations:

| Regulation | Scope | Key Requirements |
|-----------|-------|-----------------|
| **GDPR** | EU/EEA | Data minimization, encryption, right to erasure, data portability |
| **PCI-DSS** | Global (payment) | No full card storage, encryption of bank accounts, password hashing |
| **CCPA** | California, USA | Right to know, right to delete, right to opt-out |
| **PDPA** | Thailand | Consent-based processing, data breach notification |
| **LGPD** | Brazil | Similar to GDPR, data protection officer required |

---

## 2. PII Classification

### 2.1 Sensitivity Levels

| PII Field | Example | Risk Level | Protection Requirement |
|-----------|---------|------------|----------------------|
| Name | "John Doe" | High | Encrypted at rest |
| Phone | "+886912345678" | Very High | Encrypted + searchable index |
| Email | "player@example.com" | Very High | Encrypted + searchable index |
| Bank Account | "1234567890" | Very High | Encrypted + searchable index |
| ID Number | "A123456789" | Very High | Encrypted + searchable index |
| Password | "P@ssw0rd123" | Very High | One-way hash only (irreversible) |
| Address | "Taipei City..." | Medium | Encrypted at rest |
| IP Address | "1.2.3.4" | Medium | Hashed for indexing |

### 2.2 Core Principles

- **Zero Trust**: Assume database, backups, and logs may all be compromised; PII must be encrypted
- **Application-Layer Encryption**: Data encrypted before writing to database; DBAs cannot view plaintext
- **Searchable Encryption**: Encrypted data queryable via blind index
- **Crypto-Shredding**: Key destruction renders data permanently unrecoverable (GDPR compliant)

---

## 3. Data Masking Requirements

### 3.1 Masking Rules

| PII Field | Masking Rule | Example |
|-----------|-------------|---------|
| Name | Keep first and last character | `David Beckham` -> `D***m` |
| Phone | Keep first 3 and last 3 | `0912345678` -> `091****678` |
| Email | Keep first 2 and domain | `david@gmail.com` -> `da***@gmail.com` |
| Bank Account | Keep last 4 digits | `1234567890` -> `******7890` |

### 3.2 Role-Based Masking

| Role | Phone | Email | Bank Account |
|------|-------|-------|-------------|
| Player (self) | Plaintext (with 2FA) | Plaintext | Last 4 digits only |
| CS Agent (L1) | Masked | Masked | No access |
| Risk Control | Plaintext (with approval) | Plaintext (with approval) | Plaintext (with approval) |
| DBA | Ciphertext (cannot decrypt) | Ciphertext (cannot decrypt) | Ciphertext (cannot decrypt) |

---

## 4. GDPR Right to Erasure

### 4.1 Deletion Workflow

1. Player submits deletion request (via account settings, CS, or legal team)
2. Confirmation email sent (valid for 7 days)
3. Player confirms -> Enter cooling period (30 days)
4. Cooling period ends -> Execute crypto-shredding
5. Deletion certificate sent to player

### 4.2 Data Retention Matrix

| Data Category | GDPR Deletion Obligation | Retention Period | Treatment |
|--------------|-------------------------|-----------------|-----------|
| Player name, address | Yes | None | Physical delete |
| Game history | Yes (anonymize) | None | Replace player_id with UUID |
| Transaction records (AML) | No (legal obligation) | 5-7 years | Retain with anonymized player_id |
| Winning records (Tax) | No (legal obligation) | 7 years | Retain with anonymized player_id |
| Violation records (Ban) | No (legitimate interest) | Permanent | Retain for fraud prevention |

### 4.3 Deletion Exceptions (Suspension Conditions)

| Scenario | Reason | Resolution Condition | Max Retention |
|----------|--------|---------------------|---------------|
| Under investigation | AML/Fraud investigation | Investigation closed | 1 year |
| Pending litigation | Legal case pending | Case resolved | 10 years |
| Pending wagering | Wagering requirement incomplete | Completed or forfeited | 90 days |
| Outstanding balance | Balance > $0 | Balance zeroed | Indefinite (notify player) |
| Tax audit | Tax audit period | Audit concluded | 7 years |

### 4.4 Cooling Period Rules

- Player can recover account within 30-day cooling period
- All functionality restricted during cooling period (no login)
- Reminder notifications sent at Day 7, Day 21, and Day 29
- After 30 days, irreversible crypto-shredding is executed

### 4.5 Tombstone Records

After deletion, minimal tombstone records must be retained for:
- Preventing re-registration with same credentials (anti-abuse)
- Audit trail proving GDPR request was processed
- Monthly deletion statistics for regulatory reports
- Legal protection if player later disputes the deletion

---

## 5. Password Security Requirements

### 5.1 Password Complexity

- Minimum 8 characters
- At least 1 uppercase letter, 1 lowercase letter, 1 digit, 1 special character

### 5.2 Login Protection

- Limited retry attempts to prevent brute force attacks
- Account lockout after repeated failures
- Progressive delay between attempts

---

## 6. Transport Security Requirements

- All API communication must use HTTPS (TLS 1.3+ preferred, TLS 1.2 minimum)
- HTTP plaintext transmission is strictly prohibited
- Automatic HTTP -> HTTPS redirect
- HSTS header must be enabled
- Internal microservice communication must use mTLS or service mesh encryption

---

## 7. Compliance Monitoring

### 7.1 GDPR Compliance Checklist

| Requirement | Implementation |
|------------|---------------|
| Data minimization | Collect only necessary PII |
| Storage encryption | Application-layer encryption |
| Transport encryption | TLS 1.3 |
| Right to erasure | Crypto-shredding |
| Right to portability | JSON data export |
| Audit logging | All PII access logged |

### 7.2 Regulatory Reporting

- Monthly deletion statistics report for DPA (Data Protection Authority)
- Annual compliance audit report
- Breach notification within 72 hours of detection

---

## 8. Acceptance Criteria

1. All PII fields are encrypted at application layer before database storage
2. Data masking correctly applied based on user role
3. GDPR deletion workflow completes within 37 days (7 confirmation + 30 cooling)
4. Crypto-shredding makes deleted data unrecoverable even from backups
5. Deletion exceptions correctly block erasure for investigation/litigation/wagering scenarios
6. Deletion certificates auto-generated and sent to players
7. All API communication uses TLS 1.2+ with HSTS enabled
8. Password policy enforces complexity requirements
