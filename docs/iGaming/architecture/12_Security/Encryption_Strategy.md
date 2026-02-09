# Encryption Strategy Architecture

> **Business Requirements**: [Data Protection Requirements](../../requirements/12_Security_Compliance/Data_Protection_Requirements.md)
> **Canonical Source**: [source-archive/12_System_Security/12-03-01](../../source-archive/12_System_Security/12-03-01_Encryption_Strategy.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Security Engineers, Backend Developers

---

## 1. AES-256-GCM Implementation

### Storage Format (Base64 Encoded)

```text
[Version]:[IV]:[Ciphertext]:[AuthTag]

Example:
v1:a3f8d9e2c1b4:Y3J5cHRvZ3JhcGh5==:4a7b8c9d
```

| Field | Length | Description |
|-------|--------|-------------|
| Version | 2 bytes | Encryption version (for key rotation) |
| IV (Initialization Vector) | 12 bytes | Random, unique per encryption |
| Ciphertext | Variable | AES-GCM encrypted data |
| AuthTag | 16 bytes | GCM authentication tag |

### Why AES-256-GCM

- **AES-256**: NIST certified, industry standard, quantum-resistant (current)
- **GCM Mode**: AEAD (Authenticated Encryption with Associated Data) - prevents ciphertext tampering
- **Performance**: Hardware acceleration (AES-NI) support

## 2. TLS Configuration

```nginx
server {
    listen 443 ssl http2;
    ssl_protocols TLSv1.3 TLSv1.2;  # Disable TLSv1.0/1.1
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers on;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
}
```

## 3. Password Hashing: Argon2id

### Algorithm Comparison

| Algorithm | Year | Strength | Weakness |
|-----------|------|----------|----------|
| **MD5** | 1992 | Fast | Broken, prohibited |
| **bcrypt** | 1999 | CPU-resistant | GPU vulnerable |
| **PBKDF2** | 2000 | NIST certified | GPU/ASIC vulnerable |
| **Argon2id** | 2015 | **CPU/GPU/ASIC resistant** | High compute cost (a feature) |

### Recommended Parameters (OWASP)

| Parameter | Value | Description |
|-----------|-------|-------------|
| `time_cost` | 3 | Iterations (target: 0.5-1s) |
| `memory_cost` | 65536 (64 MB) | Memory consumption, prevents GPU parallelism |
| `parallelism` | 2 | CPU cores |
| `salt_len` | 16 bytes | Unique random salt per user |

## 4. Data Masking Implementation

### Masking Rules

| PII Field | Masking Rule | Example | Use Case |
|-----------|-------------|---------|----------|
| **Name** | Keep first/last char | `David Beckham` -> `D***m` | CS query, VIP management |
| **Phone** | Keep first 3, last 3 | `0912345678` -> `091****678` | CS query, withdrawal review |
| **Email** | Keep first 2 + domain | `david@gmail.com` -> `da***@gmail.com` | CS query, account settings |
| **Bank Account** | Keep last 4 | `1234567890` -> `******7890` | Withdrawal review, reports |
| **ID Number** | Keep first 2, last 2 | `A123456789` -> `A1*****89` | KYC verification |
| **IP Address** | Keep first 2 octets | `192.168.1.100` -> `192.168.*.*` | Risk analysis, logs |

### Implementation Layer

Masking MUST be implemented at the Backend DTO Converter / Serializer layer. Frontend-only masking is prohibited (API response would still contain plaintext).

## 5. Key Management Architecture

### Dual-Key Hierarchy

```text
+--------------------------------------------+
|  Master Key (CMK - Customer Master Key)    |
|  - Stored in AWS KMS / HashiCorp Vault     |
|  - Never leaves HSM                        |
|  - Rotated every 365 days                  |
+-------------------+------------------------+
                    | Encrypts
                    v
+--------------------------------------------+
|  Data Encryption Keys (DEK)                |
|  - Generated per encryption operation       |
|  - Cached in application memory (ephemeral) |
|  - Encrypted by CMK before storage         |
+--------------------------------------------+
```

### IAM Policy (Least Privilege)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "arn:aws:kms:us-east-1:123456789:key/abc-123",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "rds.us-east-1.amazonaws.com"
        }
      }
    }
  ]
}
```

## 6. Key Rotation

- **Frequency**: Auto-rotate CMK every 365 days (AWS KMS managed)
- **Transition**: Old and new keys coexist for 90 days (graceful migration)
- **Emergency**: Manual rotation triggered on suspected key compromise
