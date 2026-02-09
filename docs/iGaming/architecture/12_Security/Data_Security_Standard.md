# Data Security Standard Architecture

> **Business Requirements**: [Data Protection Requirements](../../requirements/12_Security_Compliance/Data_Protection_Requirements.md)
> **Canonical Source**: [source-archive/12_System_Security/12-03](../../source-archive/12_System_Security/12-03_Data_Security_Standard.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Security Engineers, Backend Developers

---

## 1. Encryption Architecture Overview

```text
+-----------------------------------------------------+
|  Application Layer                                    |
|  - AES-256-GCM Column-Level Encryption               |
|  - Argon2id Password Hashing                         |
|  - HMAC-SHA256 Blind Index Generation                |
+----------------+------------------------------------+
                 |
+----------------v------------------------------------+
|  Key Management Layer                                |
|  - AWS KMS: Master Key (KEK)                         |
|  - Per-Player DEK (Data Encryption Key)              |
|  - HashiCorp Vault: Blind Index Key                  |
+----------------+------------------------------------+
                 |
+----------------v------------------------------------+
|  Storage Layer                                       |
|  - encrypted_* columns (VARBINARY)                   |
|  - *_index columns (CHAR(64) - Blind Index)          |
|  - TLS 1.3 in transit                               |
+-----------------------------------------------------+
```

## 2. PII Classification Matrix

| PII Field | Example | Risk Level | Encryption |
|-----------|---------|-----------|------------|
| **Real Name** | "John Doe" | High | AES-256-GCM |
| **Phone** | "+886912345678" | Critical | AES-256-GCM + Blind Index |
| **Email** | "player@example.com" | Critical | AES-256-GCM + Blind Index |
| **Bank Account** | "1234567890" | Critical | AES-256-GCM + Blind Index |
| **ID Number** | "A123456789" | Critical | AES-256-GCM + Blind Index |
| **Password** | "P@ssw0rd123" | Critical | **Argon2id Hash** (irreversible) |
| **Address** | "Taipei..." | Medium | AES-256-GCM |
| **IP Address** | "1.2.3.4" | Medium | HMAC-SHA256 (Blind Index) |

## 3. Storage Encryption Format

```text
[Version]:[IV]:[Ciphertext]:[AuthTag]

Example:
v1:a3f8d9e2c1b4:Y3J5cHRvZ3JhcGh5==:4a7b8c9d
```

| Field | Length | Description |
|-------|--------|-------------|
| Version | 2 bytes | Encryption version (for key rotation) |
| IV | 12 bytes | Random per encryption |
| Ciphertext | Variable | AES-GCM encrypted data |
| AuthTag | 16 bytes | GCM authentication tag |

## 4. Blind Index Query Flow

```sql
-- Write Path:
-- 1. Input: +886912345678
-- 2. AES-256-GCM(phone) -> encrypted_phone
-- 3. HMAC-SHA256(phone, blind_key) -> phone_index
-- 4. Store: encrypted_phone + phone_index

-- Query Path:
-- 1. Input: +886912345678
-- 2. HMAC-SHA256(phone, blind_key) -> computed_index
-- 3. WHERE phone_index = computed_index
-- 4. Decrypt encrypted_phone for display
```

## 5. Crypto-Shredding (GDPR Deletion)

```text
Dual-Layer Encryption Architecture:
- Master Key (KEK) stored in AWS KMS
  -> Encrypts
- Per-Player DEK (Data Encryption Key)
  -> Encrypts
- Player PII

Deletion Flow:
DELETE FROM user_keys WHERE player_id = ?
-> DEK destroyed
-> All PII permanently unrecoverable (even with backups)
```

## 6. Transport Security

```nginx
server {
    listen 443 ssl http2;
    ssl_protocols TLSv1.3 TLSv1.2;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
}
```

## 7. Data Masking Rules

| PII Field | Masking Rule | Example |
|-----------|-------------|---------|
| **Name** | Keep first/last char | `David Beckham` -> `D***m` |
| **Phone** | Keep first 3, last 3 | `0912345678` -> `091****678` |
| **Email** | Keep first 2 + domain | `david@gmail.com` -> `da***@gmail.com` |
| **Bank Account** | Keep last 4 | `1234567890` -> `******7890` |

### Role-Based Masking

| Role | Phone | Email | Bank Account |
|------|-------|-------|-------------|
| **Player (self)** | Full (2FA required) | Full | Last 4 digits |
| **CS Level 1** | `091****678` | `da***@gmail.com` | No access |
| **Risk Control** | Full (approval needed) | Full (approval needed) | Full (approval needed) |
| **DBA** | Ciphertext (cannot decrypt) | Ciphertext | Ciphertext |

## 8. Compliance Checklist

### GDPR

| Requirement | Status | Implementation |
|-------------|--------|---------------|
| Data Minimization | Compliant | Collect only necessary PII |
| Storage Encryption | Compliant | AES-256-GCM |
| Transport Encryption | Compliant | TLS 1.3 |
| Right to Erasure | Compliant | Crypto-Shredding |
| Data Portability | Compliant | JSON export |
| Audit Logging | Compliant | All PII access logged |

### PCI-DSS

| Requirement | Status | Implementation |
|-------------|--------|---------------|
| No full card storage | Compliant | PSP Token only |
| Bank account encrypted | Compliant | AES-256-GCM + Blind Index |
| Password hashing | Compliant | Argon2id |
| Access control | Compliant | RBAC + IP whitelist |
