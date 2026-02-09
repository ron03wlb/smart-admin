# Payment Security Requirements

> **Canonical Source**: [source-archive/12_System_Security/12-06](../../source-archive/12_System_Security/12-06_Payment_Restrictions.md), [12-07](../../source-archive/12_System_Security/12-07_Data_Portability_SAR.md), [12-08](../../source-archive/12_System_Security/12-08_MITM_Detection.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers, Payment Operations
> **Related Doc**: [Payment Restrictions Architecture](../../architecture/12_Security/Payment_Restrictions.md), [Data Portability SAR Architecture](../../architecture/12_Security/Data_Portability_SAR.md), [MITM Detection Architecture](../../architecture/12_Security/MITM_Detection.md)
> **Last Synced**: 2026-02-09

---

## 1. Credit Card Ban Requirements

### 1.1 Jurisdictional Restrictions

| Region | Effective Date | Scope | Status |
|--------|---------------|-------|--------|
| **UK** | April 2020 | All gambling | Complete ban |
| **Australia** | April 2026 | Online gambling | Upcoming |
| **Sweden** | 2025+ | Online brands | Expanding |
| **Germany** | 2021 | All gambling | Complete ban |

### 1.2 Business Rules

- System must automatically detect and block credit card deposits based on player jurisdiction
- Debit cards remain allowed in all jurisdictions
- Cryptocurrency restrictions vary by jurisdiction and must be configurable

### 1.3 Payment Method Whitelist by Jurisdiction

**UK Allowed Methods**:

| Payment Type | Allowed | Notes |
|-------------|---------|-------|
| Debit Card | Yes | Visa/Mastercard debit |
| Bank Transfer | Yes | Bank wire |
| e-Wallet | Yes | PayPal, Skrill, Neteller |
| Prepaid Card | Yes | Paysafecard |
| Credit Card | **No** | Prohibited since April 2020 |

**Brazil Required Methods**:

| Payment Type | Requirement | Notes |
|-------------|-------------|-------|
| PIX | **Mandatory** | Brazil instant payment |
| Bank Transfer | Recommended | Bank wire |
| Boleto | Recommended | Cash payment |
| Credit Card | Allowed | Currently permitted |

### 1.4 Cryptocurrency Compliance

| Region | Stance | Requirements |
|--------|--------|-------------|
| UK | Cautious | Full AML required |
| Malta | Allowed | Regulatory framework needed |
| Curacao | Allowed | Less strict |

**Crypto AML Requirements**:
- Wallet address risk scoring before accepting deposits
- Transaction tracing to detect blacklisted addresses
- All crypto AML checks must be logged for audit

---

## 2. Data Portability and Subject Access Requests (SAR)

### 2.1 Regulatory Deadlines

| Regulation | Right | Response Deadline | Extension |
|-----------|-------|-------------------|-----------|
| GDPR Art. 15 | Access right | 30 days | Up to 90 days (complex) |
| GDPR Art. 20 | Portability right | 30 days | N/A |
| UK GDPR | Same as GDPR | 30 days | Same |
| CCPA | Right to know | 45 days | N/A |

### 2.2 Request Types

| Type | Code | Description |
|------|------|-------------|
| Access Request (SAR) | ACCESS | Obtain copy of personal data |
| Portability Request | PORTABILITY | Export in machine-readable format |
| Erasure Request | ERASURE | Right to be forgotten |
| Rectification Request | RECTIFICATION | Correct inaccurate data |
| Processing Restriction | RESTRICTION | Suspend data processing |

### 2.3 SAR Processing Requirements

1. **Identity Verification**: Standard (email + OTP) or Enhanced (government ID + transaction verification) within 5 business days
2. **Request Assessment**: Determine clarity, scope, and any exceptions
3. **Data Collection**: Gather from all sources (database, logs, third-party systems, backups) within 15 business days
4. **Data Review**: Redact third-party data, trade secrets, legal privileged info, and ongoing investigation details
5. **Package Delivery**: Encrypted files via secure download link (7-day validity)

### 2.4 Data Export Scope

**Must Include**:
- Account information (username, email, phone, name, address, dates)
- KYC verification status and dates
- Financial data (deposit/withdrawal history, balances)
- Gaming data (bet history, bonus history, game time statistics)
- Responsible gambling settings (deposit limits, self-exclusion history)
- Communication records (CS conversations, notifications)
- Technical data (login history, device info)

**Must Exclude**:
- Other players' personal data
- CS agent names (use IDs instead)
- Risk control rule configurations
- Fraud detection scores
- Internal investigation notes
- Ongoing legal proceedings
- Aggregated/anonymized data

### 2.5 Portability Format Requirements

- Machine-readable formats required: JSON (structured), CSV (tabular), XML (compatibility)
- PDF alone is not acceptable (not machine-readable)
- Must include data dictionary/schema documentation
- Direct transfer to third party supported upon request (GDPR Art. 20(2))

### 2.6 Exception Handling

Requests may be refused if:
- Manifestly unfounded or excessive
- Duplicate request within 12 months
- Would adversely affect others' rights

---

## 3. MITM Attack Protection Requirements

### 3.1 Threat Overview

| Attack Type | Severity | Description |
|------------|----------|-------------|
| TLS Downgrade | Critical | Forcing weak encryption protocols |
| SSL Stripping | Critical | Downgrading HTTPS to HTTP |
| Certificate Forgery | Critical | Using forged CA certificates |
| DNS Spoofing | High | Tampering DNS resolution results |
| ARP Spoofing | High | LAN traffic interception |
| Proxy Injection | High | Injecting malicious transparent proxy |

### 3.2 Detection Requirements

| Capability | Description | Detection Latency |
|-----------|-------------|-------------------|
| TLS downgrade detection | Detect forced TLS version downgrade | Real-time |
| Certificate pinning | Verify server certificate legitimacy | Real-time |
| Session hijack detection | Identify stolen session tokens | Near real-time (<1s) |
| DNS spoofing detection | Detect DNS resolution anomalies | Real-time |
| Proxy injection detection | Identify malicious proxy interception | Real-time |

### 3.3 Response Actions

| Alert Type | Trigger Condition | Severity | Action |
|-----------|-------------------|----------|--------|
| TLS Downgrade Attempt | TLS 1.0/1.1 request detected | Critical | Block + Log |
| Certificate Pin Failure | Certificate mismatch | Critical | Block + Investigate |
| Session Hijack | Device + IP simultaneous change | Critical | Terminate session |
| Suspicious Proxy | Multiple proxy signals | High | Step-up authentication |
| DNS Anomaly | Inconsistent resolution results | High | Verify + Log |

### 3.4 Session Binding Rules

- Each session must be bound to device fingerprint and IP address
- Device fingerprint change triggers re-authentication
- "Impossible travel" detection: If IP geolocations change faster than physically possible, flag as potential hijack
- Risk levels: CRITICAL (device + impossible travel), HIGH (device mismatch only), MEDIUM (IP change only)

---

## 4. Monitoring KPIs

### 4.1 SAR Metrics

| Metric | Target | Alert |
|--------|--------|-------|
| Pending request count | < 10 | > 10 |
| Overdue request count | 0 | > 0 |
| Average processing days | < 20 | > 20 |
| Completion rate | > 95% | < 95% |

### 4.2 MITM Metrics

| Metric | Target | Alert |
|--------|--------|-------|
| TLS 1.3 adoption rate | > 90% | < 80% |
| Certificate pin failure rate | < 0.01% | > 0.1% |
| Daily session hijack detections | < 5 | > 20 |
| Proxy detection rate | Monitor trend | Abnormal increase |

---

## 5. Acceptance Criteria

1. Credit card deposits blocked automatically in UK, Germany, and other restricted jurisdictions
2. Payment method whitelists enforced per jurisdiction
3. Crypto deposits undergo wallet risk scoring and transaction tracing
4. SAR requests processed within 30-day deadline with secure delivery
5. Data portability exports include all required data in machine-readable format
6. MITM detection covers TLS downgrade, certificate pinning, session hijack, DNS spoofing, and proxy detection
7. Session binding with device fingerprint and IP properly enforced
8. All security events logged and alerting thresholds configured
