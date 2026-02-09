# MFA Technical Architecture

> **Business Requirements**: [MFA_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Requirements.md)
> **Audience**: Backend Developers, Security Engineers, Compliance Officers
> **Last Synced**: 2026-02-09

**Document Metadata**:
- Version: 1.0.0
- Created: 2026-02-08
- Last Updated: 2026-02-08
- Status: Production Ready
- Priority: P1 (High)
- Owner: Security Team + Backend Team
- View Type: Technical Architecture

**Canonical Source**: [docs/iGaming/source-archive/06_Platform_Governance/06-06_MFA_Implementation.md](../../source-archive/06_Platform_Governance/06-06_MFA_Implementation.md)

**Related Documents**:
- Business Requirements: [MFA_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Requirements.md)
- Source Sub-documents:
  - [06-06-02 TOTP & WebAuthn Implementation](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md)
  - [06-06-03 Login & Recovery Flow](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md)

---

## 1. Architecture Overview

### 1.1 System Flow

```mermaid
graph TD
    A[User Login] --> B{Password Verification}
    B -->|PASS| C{MFA Enabled?}
    B -->|FAIL| Z[Return Error]

    C -->|Yes| D{Device Trusted?}
    C -->|No| G[Issue Token Directly]

    D -->|Yes| G
    D -->|No| E[Require MFA Verification]

    E --> F{Select Verification Method}
    F --> F1[TOTP - Primary]
    F --> F2[SMS OTP - Backup 1]
    F --> F3[Backup Code - Backup 2]

    F1 --> H{Verification Success?}
    F2 --> H
    F3 --> H

    H -->|PASS| G
    H -->|FAIL| I[Error Count +1]
    I --> J{Errors >= 3?}
    J -->|Yes| K[Lock 15 Minutes]
    J -->|No| E
```

### 1.2 SmartAdmin Layer Mapping

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Controller** | `MfaController` | HTTP endpoints, request validation |
| **Service** | `AdminAuthService` | Login orchestration, token issuance |
| **Manager** | `MfaManager` | TOTP verification, backup codes (contains `@Transactional`) |
| **Dao** | `MfaDao` | MFA record persistence |
| **Entity** | `MfaEntity` | Database mapping |

---

## 2. TOTP Implementation

### 2.1 RFC 6238 Algorithm

**TOTP (Time-based One-Time Password)** is based on [RFC 6238](https://tools.ietf.org/html/rfc6238) standard.

**Core Algorithm**:

```
TOTP = HOTP(K, T)

Where:
- K = Shared Secret (Base32 encoded)
- T = Floor(Current Unix Time / Time Step)
- Time Step = 30 seconds (standard value)
- HOTP = HMAC-based One-Time Password (RFC 4226)
```

**Python Implementation Reference**:

```python
import hmac
import hashlib
import time
import base64

def generate_totp(secret_key: str, time_step: int = 30) -> str:
    """
    Generate 6-digit TOTP verification code

    Args:
        secret_key: Base32 encoded shared secret (e.g., JBSWY3DPEHPK3PXP)
        time_step: Time step in seconds, default 30

    Returns:
        6-digit verification code (e.g., 123456)
    """
    # Step 1: Calculate time counter (T)
    current_time = int(time.time())
    time_counter = current_time // time_step

    # Step 2: Convert counter to 8-byte big-endian
    time_bytes = time_counter.to_bytes(8, byteorder='big')

    # Step 3: Decode Base32 secret
    key_bytes = base64.b32decode(secret_key)

    # Step 4: Calculate HMAC-SHA1
    hmac_hash = hmac.new(key_bytes, time_bytes, hashlib.sha1).digest()

    # Step 5: Dynamic Truncation
    offset = hmac_hash[-1] & 0x0F
    truncated_hash = hmac_hash[offset:offset+4]

    # Step 6: Convert to integer and modulo
    code = int.from_bytes(truncated_hash, byteorder='big') & 0x7FFFFFFF
    otp = code % 1000000  # 6 digits

    return f"{otp:06d}"  # Pad leading zeros
```

**Algorithm Security Analysis**:

| Property | Description | Security Rating |
|----------|-------------|-----------------|
| **One-way** | HMAC-SHA1 irreversible, cannot derive secret from code | 5/5 |
| **Time-sensitive** | Code changes every 30 seconds | 5/5 |
| **Brute-force resistant** | 6 digits (1M possibilities) x 30s window = very low success rate | 4/5 |
| **Offline verification** | Server and client calculate independently, no network required | 5/5 |

### 2.2 Secret Key Generation & Sharing

**Java Implementation**:

```java
import java.security.SecureRandom;
import java.util.Base64;

public class TotpSecretGenerator {

    /**
     * Generate TOTP shared secret (Base32 encoded)
     *
     * @return 20-byte random secret (e.g., JBSWY3DPEHPK3PXP)
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];  // 160 bits (RFC 6238 recommended)
        random.nextBytes(bytes);

        // Use Base32 encoding (A-Z and 2-7, 32 characters)
        return Base32.encode(bytes);
    }

    /**
     * Generate QR Code URL (for Google Authenticator scanning)
     *
     * @param username User name
     * @param secret Base32 encoded secret
     * @param issuer Issuer name (displayed in app)
     * @return otpauth:// protocol URL
     */
    public static String generateQrCodeUrl(String username, String secret, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            issuer,
            username,
            secret,
            issuer
        );
    }
}
```

**Secret Encryption (AES-256-GCM)**:

```java
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TotpSecretEncryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Encrypt TOTP Secret using AES-256-GCM
     *
     * @param plainSecret Plain text secret (e.g., JBSWY3DPEHPK3PXP)
     * @param masterKey Master key (from KMS, e.g., AWS KMS / HashiCorp Vault)
     * @return Base64 encoded ciphertext
     */
    public static String encryptSecret(String plainSecret, SecretKey masterKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey);

        byte[] iv = cipher.getIV();  // Initialization Vector
        byte[] ciphertext = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));

        // Format: IV + Ciphertext (Base64 encoded)
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt TOTP Secret
     */
    public static String decryptSecret(String encryptedSecret, SecretKey masterKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedSecret);

        // Extract IV (first 12 bytes)
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);

        // Extract ciphertext
        byte[] ciphertext = new byte[combined.length - 12];
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
```

**Key Management Best Practices**:

| Phase | Best Practice | SmartAdmin Implementation |
|-------|---------------|---------------------------|
| **Generation** | Use `SecureRandom` (NOT `Random`) | `SecureRandom` + 160 bits entropy |
| **Transmission** | Via QR Code (HTTPS + one-time display) | QR Code shown only once |
| **Storage** | AES-256-GCM encrypted + KMS managed master key | Integrate AWS KMS / Vault |
| **Usage** | Decrypt immediately before use, no memory traces | `try-finally` to clear variables |
| **Destruction** | Delete secret when user disables MFA | `CASCADE DELETE` |

### 2.3 Time Synchronization

**Problem**: Server time may differ from user device time (+-5 minutes).

**Solution**: Allow +-1 time window (verify codes from previous, current, and next 30-second windows).

**Java Implementation**:

```java
import java.time.Instant;

public class TotpValidator {
    private static final int TIME_STEP = 30;  // 30 seconds
    private static final int WINDOW = 1;      // Allow +-1 window

    /**
     * Validate TOTP code (with time drift tolerance)
     *
     * @param userCode User-entered 6-digit code
     * @param secret Base32 encoded shared secret
     * @return true if valid, false otherwise
     */
    public static boolean validate(String userCode, String secret) {
        long currentTime = Instant.now().getEpochSecond();

        // Check current window + previous and next windows (3 total)
        for (int i = -WINDOW; i <= WINDOW; i++) {
            long timeCounter = (currentTime / TIME_STEP) + i;
            String generatedCode = generateTotp(secret, timeCounter);

            if (userCode.equals(generatedCode)) {
                return true;
            }
        }

        return false;  // No window matches
    }

    private static String generateTotp(String secret, long timeCounter) {
        // Use HMAC-SHA1 algorithm from Section 2.1
        // ... (implementation omitted)
    }
}
```

**Time Synchronization Test Matrix**:

| Server Time | Device Time | Drift | Result | Notes |
|-------------|-------------|-------|--------|-------|
| 10:00:00 | 10:00:00 | 0s | SUCCESS | Perfectly synced |
| 10:00:00 | 10:00:25 | +25s | SUCCESS | Within current window |
| 10:00:00 | 10:00:35 | +35s | SUCCESS | Next window (allowed +1) |
| 10:00:00 | 10:01:05 | +65s | FAIL | Exceeds +-1 window range |
| 10:00:00 | 09:59:25 | -35s | SUCCESS | Previous window (allowed -1) |

**NTP Configuration**:

```bash
# Server-side NTP auto-sync
sudo apt-get install ntp
sudo systemctl enable ntp
sudo systemctl start ntp

# Verify sync status
ntpq -p
```

---

## 3. API Specifications

### 3.1 Password Login (Phase 1)

```http
POST /admin/auth/login
Content-Type: application/json

{
  "username": "admin@smartadmin.com",
  "password": "SecurePassword123!",
  "deviceFingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

**Response (MFA Enabled)**:

```json
{
  "code": 200,
  "msg": "Password verified. MFA required.",
  "data": {
    "needMfa": true,
    "mfaSessionToken": "mfa_sess_abc123xyz...",
    "expiresAt": "2026-02-05T10:05:00Z",
    "availableMethods": ["totp", "sms", "backup_code"]
  }
}
```

**Response (MFA Not Enabled)**:

```json
{
  "code": 200,
  "msg": "Login successful.",
  "data": {
    "needMfa": false,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "rt_abc123xyz...",
    "user": {
      "userId": 12345,
      "username": "admin@smartadmin.com",
      "roles": ["SUPER_ADMIN"]
    }
  }
}
```

### 3.2 MFA Verification (Phase 2)

```http
POST /admin/auth/mfa/verify
Content-Type: application/json

{
  "mfaSessionToken": "mfa_sess_abc123xyz...",
  "totpCode": "123456",
  "trustDevice": true
}
```

**Response (Success)**:

```json
{
  "code": 200,
  "msg": "MFA verification successful.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "rt_abc123xyz...",
    "user": {
      "userId": 12345,
      "username": "admin@smartadmin.com",
      "roles": ["SUPER_ADMIN"]
    }
  }
}
```

**Response (Failure)**:

```json
{
  "code": 401,
  "msg": "Invalid TOTP code.",
  "data": {
    "remainingAttempts": 2,
    "lockoutDuration": null
  }
}
```

**Response (Account Locked)**:

```json
{
  "code": 429,
  "msg": "Too many failed MFA attempts. Account locked.",
  "data": {
    "remainingAttempts": 0,
    "lockoutDuration": 900,
    "unlockAt": "2026-02-05T10:15:00Z"
  }
}
```

### 3.3 MFA Setup Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/admin/mfa/setup/init` | POST | Initialize MFA setup (generate secret + QR code) |
| `/admin/mfa/setup/verify` | POST | Verify and activate MFA |
| `/admin/mfa/backup-codes/status` | GET | Check remaining backup codes |
| `/admin/mfa/backup-codes/regenerate` | POST | Regenerate backup codes (requires TOTP) |

---

## 4. Database Schema

### 4.1 MFA User Table

```sql
CREATE TABLE t_admin_user_mfa (
    user_id             BIGINT PRIMARY KEY REFERENCES t_admin_user(user_id),
    mfa_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    totp_secret         VARCHAR(255) NOT NULL,          -- AES-256-GCM encrypted
    backup_codes        TEXT,                           -- JSON array (AES encrypted)
    status              VARCHAR(20) NOT NULL,           -- PENDING / ACTIVE / DISABLED
    activated_at        TIMESTAMP,                      -- Activation time
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_mfa_status ON t_admin_user_mfa(status);
CREATE INDEX idx_mfa_activated_at ON t_admin_user_mfa(activated_at);
```

### 4.2 MFA Audit Log Table

```sql
CREATE TABLE t_audit_log_mfa (
    log_id              BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    event_type          VARCHAR(50) NOT NULL,
    event_level         VARCHAR(20) NOT NULL,           -- INFO / WARNING / CRITICAL
    ip_address          INET,
    user_agent          TEXT,
    device_fingerprint  VARCHAR(64),
    details             JSONB,                          -- Additional details (JSON)
    created_at          TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_mfa_log_user_id ON t_audit_log_mfa(user_id);
CREATE INDEX idx_mfa_log_event_type ON t_audit_log_mfa(event_type);
CREATE INDEX idx_mfa_log_event_level ON t_audit_log_mfa(event_level);
CREATE INDEX idx_mfa_log_created_at ON t_audit_log_mfa(created_at);
```

### 4.3 MFA Recovery Request Table

```sql
CREATE TABLE t_mfa_recovery_request (
    request_id      BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    id_card_photo   TEXT,                               -- ID card photo URL
    reason          TEXT,
    status          VARCHAR(20) NOT NULL,               -- PENDING / APPROVED / REJECTED
    reviewed_by     BIGINT REFERENCES t_admin_user(user_id),
    reviewed_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### 4.4 Emergency Contacts Table

```sql
CREATE TABLE t_mfa_emergency_contacts (
    user_id             BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_user_id     BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_type        VARCHAR(20) NOT NULL,           -- COLLEAGUE / MANAGER
    verified            BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, contact_user_id)
);
```

### 4.5 Redis Structures

**MFA Session**:

```
Key:   mfa:session:{userId}
Value: {
  "userId": 12345,
  "createdAt": "2026-02-05T10:00:00Z"
}
TTL:   300 seconds (5 minutes)
```

**Trusted Device**:

```
Key:   trusted_device:{userId}:{deviceFingerprint}
Value: {
  "userId": 12345,
  "deviceFingerprint": "e3b0c442...",
  "trustedAt": "2026-02-05T10:00:00Z",
  "expiresAt": "2026-03-07T10:00:00Z",
  "ipAddress": "203.0.113.42",
  "userAgent": "Mozilla/5.0..."
}
TTL:   2592000 seconds (30 days)
```

**MFA Failure Counter**:

```
Key:   mfa:fail:{userId}
Value: 3
TTL:   900 seconds (15 minutes)
```

---

## 5. Sequence Diagrams

### 5.1 Two-Phase Authentication Flow

```mermaid
sequenceDiagram
    participant U as User Browser
    participant C as Controller<br/>(AuthController)
    participant S as Service<br/>(AdminAuthService)
    participant M as Manager<br/>(MfaManager)
    participant R as Redis<br/>(MFA Session)
    participant D as Database<br/>(PostgreSQL)

    Note over U,D: Phase 1: Password Verification
    U->>C: POST /admin/auth/login<br/>{username, password}
    C->>S: login(username, password)
    S->>D: Query user + verify password
    D-->>S: User data + mfa_enabled = true

    alt MFA Enabled
        S->>R: Create MFA Session<br/>Key: mfa:session:{userId}<br/>TTL: 5 minutes
        R-->>S: Session Token
        S-->>C: Return needMfa = true<br/>+ mfaSessionToken
        C-->>U: HTTP 200<br/>{needMfa: true, mfaSessionToken}

        Note over U: User opens Google Authenticator<br/>Enters 6-digit code

        Note over U,D: Phase 2: MFA Verification
        U->>C: POST /admin/auth/mfa/verify<br/>{mfaSessionToken, totpCode}
        C->>M: verifyTotp(mfaSessionToken, totpCode)
        M->>R: Check session validity
        R-->>M: userId = 12345
        M->>D: Get encrypted TOTP Secret
        D-->>M: encrypted_secret
        M->>M: Decrypt Secret<br/>Validate TOTP (+-1 window)

        alt TOTP Verification Success
            M->>R: Delete MFA Session
            M->>D: Record audit log (MFA_LOGIN_SUCCESS)
            M-->>S: Verification success + userId
            S->>S: Issue Access Token + Refresh Token
            S-->>C: JWT Tokens
            C-->>U: HTTP 200<br/>{accessToken, refreshToken}
        else TOTP Verification Failure
            M->>R: mfa:fail:{userId} += 1<br/>TTL: 15 minutes
            M->>D: Record audit log (MFA_LOGIN_FAILED)

            alt Failure count >= 3
                M->>D: Lock account 15 minutes
                M->>R: Send security alert
                M-->>C: HTTP 429<br/>{error: "MFA_LOCKED"}
            else Failure count < 3
                M-->>C: HTTP 401<br/>{error: "INVALID_TOTP"}
            end
        end
    else MFA Not Enabled
        S->>S: Issue Tokens directly
        S-->>C: JWT Tokens
        C-->>U: HTTP 200<br/>{accessToken, refreshToken}
    end
```

### 5.2 MFA Registration Flow

```mermaid
sequenceDiagram
    participant U as User Browser
    participant C as Controller<br/>(MfaController)
    participant M as Manager<br/>(MfaManager)
    participant D as Database<br/>(PostgreSQL)
    participant Q as QR Service<br/>(ZXing)

    Note over U: User clicks "Enable MFA"
    U->>C: POST /admin/mfa/setup/init
    C->>M: initMfaSetup(userId)
    M->>M: Generate TOTP Secret<br/>(SecureRandom + Base32)
    M->>Q: Generate QR Code PNG<br/>otpauth://totp/...
    Q-->>M: Base64 QR Code Image
    M->>D: Save encrypted Secret<br/>Status = PENDING
    D-->>M: Save successful
    M-->>C: QR Code + Secret (plaintext)
    C-->>U: HTTP 200<br/>{qrCode, secret, backupCodes}

    Note over U: User scans QR Code<br/>Adds to Google Authenticator<br/>Enters first verification code

    U->>C: POST /admin/mfa/setup/verify<br/>{totpCode}
    C->>M: verifyAndActivate(userId, totpCode)
    M->>D: Get PENDING status Secret
    D-->>M: encrypted_secret
    M->>M: Decrypt Secret<br/>Validate TOTP

    alt TOTP Verification Success
        M->>D: Update Status = ACTIVE<br/>mfa_enabled = true
        M->>D: Record audit log (MFA_ENABLED)
        M-->>C: Activation successful
        C-->>U: HTTP 200<br/>{success: true}

        Note over U: Show success message<br/>Download backup codes
    else TOTP Verification Failure
        M-->>C: HTTP 401<br/>{error: "INVALID_TOTP"}
        C-->>U: Prompt to retry
    end
```

### 5.3 Backup Code Login Flow

```mermaid
sequenceDiagram
    participant U as User
    participant C as Controller
    participant M as MfaManager
    participant D as Database

    U->>C: POST /admin/auth/mfa/verify<br/>{backupCode: "1234-5678"}
    C->>M: verifyBackupCode(userId, code)
    M->>D: Query backup code list (decrypt)
    D-->>M: JSON backup code array

    M->>M: Iterate and check backup code match

    alt Backup code valid and unused
        M->>D: Mark backup code as used<br/>used = true
        M->>D: Record audit log (BACKUP_CODE_USED)

        alt Remaining codes <= 2
            M->>U: WARNING: Only X codes remaining<br/>Recommend regeneration
        end

        M-->>C: Verification success
        C-->>U: Issue Tokens
    else Backup code invalid or already used
        M->>D: Record audit log (BACKUP_CODE_INVALID)
        M-->>C: HTTP 401<br/>{error: "INVALID_BACKUP_CODE"}
    end
```

### 5.4 Trust Device Flow

```mermaid
graph TD
    A[User Login] --> B{Password Verification}
    B -->|PASS| C{Check Device Trusted}
    B -->|FAIL| Z[Return Error]

    C -->|Yes| D[Skip MFA, Issue Token Directly]
    C -->|No| E{MFA Enabled?}

    E -->|Yes| F[Require MFA Verification]
    E -->|No| D

    F --> G{User Selects "Trust Device"?}
    G -->|Yes| H[Generate Device Trust Token<br/>Store to Redis<br/>TTL = 30 days]
    G -->|No| I[No trust info stored]

    H --> J[Set-Cookie: device_trust_token<br/>HttpOnly + Secure + SameSite=Strict]
    I --> J
    J --> D
```

---

## 6. Code Examples

### 6.1 QR Code Generation (ZXing)

```java
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class QrCodeGenerator {

    /**
     * Generate TOTP QR Code (Base64 PNG image)
     *
     * @param username User name
     * @param secret Base32 encoded TOTP Secret
     * @param issuer Issuer name (displayed in App)
     * @return Base64 encoded PNG image
     */
    public static String generateQrCode(String username, String secret, String issuer) throws Exception {
        // Step 1: Construct otpauth:// URL
        String otpauthUrl = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            issuer,
            username,
            secret,
            issuer
        );

        // Step 2: Use ZXing to generate QR Code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUrl, BarcodeFormat.QR_CODE, 300, 300);

        // Step 3: Convert to BufferedImage
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Step 4: Convert to Base64 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
```

### 6.2 Backup Code Generator

```java
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BackupCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate backup codes (8 digits, format: 1234-5678)
     *
     * @param count Number to generate (recommend 10)
     * @return List of backup codes
     */
    public static List<String> generate(int count) {
        List<String> codes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            // Generate 8-digit number (10000000 ~ 99999999)
            int code = RANDOM.nextInt(90000000) + 10000000;

            // Format as 1234-5678 (easier to read)
            String formatted = String.format("%04d-%04d",
                code / 10000,
                code % 10000
            );

            codes.add(formatted);
        }

        return codes;
    }
}
```

### 6.3 MFA Setup Service

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfaSetupService {

    private final MfaRepository mfaRepository;
    private final TotpValidator totpValidator;
    private final AuditLogService auditLogService;

    /**
     * Initialize MFA setup (generate Secret + QR Code)
     *
     * @param userId User ID
     * @return MFA setup info (QR Code + Secret + Backup Codes)
     */
    @Transactional(rollbackFor = Throwable.class)
    public MfaSetupVO initMfaSetup(Long userId) {
        // Step 1: Check if user already has MFA enabled
        if (mfaRepository.isMfaEnabled(userId)) {
            throw new BusinessException("MFA already enabled, cannot setup again");
        }

        // Step 2: Generate TOTP Secret (Base32 encoded)
        String totpSecret = TotpSecretGenerator.generateSecret();

        // Step 3: Generate QR Code
        String username = getUserEmail(userId);
        String qrCodeBase64 = QrCodeGenerator.generateQrCode(
            username,
            totpSecret,
            "SmartAdmin iGaming"
        );

        // Step 4: Generate 10 backup codes (8-digit)
        List<String> backupCodes = BackupCodeGenerator.generate(10);

        // Step 5: Encrypt and save to database (Status = PENDING)
        String encryptedSecret = encryptSecret(totpSecret);
        String encryptedBackupCodes = encryptBackupCodes(backupCodes);

        MfaEntity mfa = MfaEntity.builder()
            .userId(userId)
            .totpSecret(encryptedSecret)
            .backupCodes(encryptedBackupCodes)
            .status(MfaStatus.PENDING)
            .build();

        mfaRepository.save(mfa);

        // Step 6: Record audit log
        auditLogService.log(userId, "MFA_SETUP_INIT", "User started MFA setup");

        // Step 7: Return to frontend (plaintext secret shown only once)
        return MfaSetupVO.builder()
            .qrCode(qrCodeBase64)
            .secret(totpSecret)  // Plaintext (only once)
            .backupCodes(backupCodes)
            .build();
    }

    /**
     * Verify and activate MFA
     *
     * @param userId User ID
     * @param totpCode User-entered 6-digit code
     * @return Activation result
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> verifyAndActivate(Long userId, String totpCode) {
        // Step 1: Get PENDING status MFA record
        MfaEntity mfa = mfaRepository.findByUserIdAndStatus(userId, MfaStatus.PENDING)
            .orElseThrow(() -> new BusinessException("No pending MFA setup found"));

        // Step 2: Decrypt TOTP Secret
        String decryptedSecret = decryptSecret(mfa.getTotpSecret());

        // Step 3: Validate TOTP code
        boolean isValid = totpValidator.validate(totpCode, decryptedSecret);

        if (!isValid) {
            auditLogService.log(userId, "MFA_SETUP_VERIFY_FAILED", "TOTP verification failed");
            return ResponseDTO.error(ErrorCode.INVALID_TOTP);
        }

        // Step 4: Activate MFA
        mfa.setStatus(MfaStatus.ACTIVE);
        mfa.setActivatedAt(Instant.now());
        mfaRepository.save(mfa);

        // Step 5: Record audit log
        auditLogService.log(userId, "MFA_ENABLED", "MFA successfully enabled");

        return ResponseDTO.ok();
    }
}
```

### 6.4 MFA Policy Service

```java
@Service
@RequiredArgsConstructor
public class MfaPolicyService {

    private final AdminUserService adminUserService;
    private final MfaRepository mfaRepository;

    /**
     * Check if user requires mandatory MFA
     *
     * @param userId User ID
     * @return true if mandatory, false if optional
     */
    public boolean requiresMandatoryMfa(Long userId) {
        List<String> roles = adminUserService.getUserRoles(userId);

        // Mandatory MFA roles list
        Set<String> mandatoryMfaRoles = Set.of(
            "SUPER_ADMIN",
            "FINANCE_MANAGER",
            "RISK_CONTROL",
            "DATABASE_ADMIN",
            "DEVOPS"
        );

        // Check if user belongs to mandatory MFA role
        return roles.stream().anyMatch(mandatoryMfaRoles::contains);
    }

    /**
     * Validate MFA status before login
     *
     * @param userId User ID
     * @throws MfaNotEnabledException If user requires MFA but hasn't enabled
     */
    public void validateMfaStatusBeforeLogin(Long userId) {
        if (requiresMandatoryMfa(userId)) {
            boolean mfaEnabled = mfaRepository.isMfaEnabled(userId);

            if (!mfaEnabled) {
                throw new MfaNotEnabledException(
                    "Your role requires MFA to be enabled before login. Please contact administrator."
                );
            }
        }
    }
}
```

### 6.5 Device Fingerprint (JavaScript)

```javascript
import FingerprintJS from '@fingerprintjs/fingerprintjs';

async function getDeviceFingerprint() {
  // Initialize FingerprintJS
  const fp = await FingerprintJS.load();
  const result = await fp.get();

  // Return device unique identifier (99.5% accuracy)
  return result.visitorId;
}

// Attach Device Fingerprint during login
const deviceFingerprint = await getDeviceFingerprint();
const response = await fetch('/admin/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin@smartadmin.com',
    password: 'SecurePassword123!',
    deviceFingerprint: deviceFingerprint
  })
});
```

---

## 7. Anomaly Detection Rules (SQL)

### 7.1 Multiple MFA Failures in Short Time

```sql
-- Detection: >= 3 MFA failures in 5 minutes
SELECT
    user_id,
    COUNT(*) AS fail_count,
    ARRAY_AGG(ip_address) AS ip_list
FROM t_audit_log_mfa
WHERE event_type = 'MFA_LOGIN_FAILED'
  AND created_at >= NOW() - INTERVAL '5 minutes'
GROUP BY user_id
HAVING COUNT(*) >= 3;
```

### 7.2 Geographic Anomaly Detection

```sql
-- Detection: Login from different countries within 1 hour
SELECT
    a.user_id,
    a.ip_address AS ip_1,
    b.ip_address AS ip_2,
    a.created_at AS time_1,
    b.created_at AS time_2
FROM t_audit_log_mfa a
JOIN t_audit_log_mfa b ON a.user_id = b.user_id
WHERE a.event_type = 'MFA_LOGIN_SUCCESS'
  AND b.event_type = 'MFA_LOGIN_SUCCESS'
  AND a.created_at < b.created_at
  AND b.created_at - a.created_at <= INTERVAL '1 hour'
  AND get_country(a.ip_address) != get_country(b.ip_address);
```

### 7.3 Frequent Backup Code Usage

```sql
-- Detection: >= 3 backup code uses in 7 days
SELECT
    user_id,
    COUNT(*) AS backup_code_usage
FROM t_audit_log_mfa
WHERE event_type = 'BACKUP_CODE_USED'
  AND created_at >= NOW() - INTERVAL '7 days'
GROUP BY user_id
HAVING COUNT(*) >= 3;
```

### 7.4 High-Risk MFA Disabled

```sql
-- Detection: MFA disabled for high-risk roles
SELECT
    l.user_id,
    u.username,
    l.created_at,
    l.details->>'disabledBy' AS disabled_by
FROM t_audit_log_mfa l
JOIN t_admin_user u ON l.user_id = u.user_id
WHERE l.event_type = 'MFA_DISABLED'
  AND u.role_code IN ('SUPER_ADMIN', 'FINANCE_MANAGER', 'RISK_CONTROL');
```

---

## 8. Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class MfaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MfaRepository mfaRepository;

    @Test
    @DisplayName("TC-MFA-001: User first time TOTP setup")
    void testInitMfaSetup() throws Exception {
        // Given
        Long userId = 12345L;

        // When
        MvcResult result = mockMvc.perform(post("/admin/mfa/setup/init")
                .header("X-User-Id", userId))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        assertThat(json.get("data").get("qrCode").asText()).startsWith("data:image/png;base64,");
        assertThat(json.get("data").get("secret").asText()).hasSize(32);  // Base32 = 32 chars

        // Verify database
        MfaEntity mfa = mfaRepository.findByUserId(userId).orElseThrow();
        assertThat(mfa.getStatus()).isEqualTo(MfaStatus.PENDING);
        assertThat(mfa.getTotpSecret()).isNotBlank();  // Encrypted secret
    }

    @Test
    @DisplayName("TC-MFA-004: Account locked after 3 consecutive TOTP failures")
    void testMfaLockAfterThreeFailures() throws Exception {
        // Given
        Long userId = 12345L;
        String mfaSessionToken = "mfa_sess_test123";

        // When: 3 failed attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/admin/auth/mfa/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mfaSessionToken\":\"" + mfaSessionToken + "\",\"totpCode\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
        }

        // Then: 4th attempt should return 429
        mockMvc.perform(post("/admin/auth/mfa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mfaSessionToken\":\"" + mfaSessionToken + "\",\"totpCode\":\"000000\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.msg").value(containsString("locked")));
    }
}
```

---

## 9. Error Codes

| Error Code | HTTP Status | Description | Resolution |
|------------|-------------|-------------|------------|
| `INVALID_MFA_SESSION` | 401 | MFA Session Token expired or invalid | Re-execute Phase 1 (password login) |
| `INVALID_TOTP` | 401 | TOTP verification code incorrect | Prompt user to retry (show remaining attempts) |
| `MFA_LOCKED` | 429 | Account locked after 3 failures | Display unlock countdown (15 minutes) |
| `MFA_NOT_ENABLED` | 400 | User called verify without MFA enabled | Guide user to enable MFA |
| `TIME_SYNC_ERROR` | 500 | Server time not synced with NTP | Trigger ops alert, restart NTP service |

---

## 10. Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| TOTP Algorithm | RFC 6238 (HMAC-SHA1) | - |
| QR Code Generation | ZXing | 3.5.x |
| Secret Encryption | AES-256-GCM | - |
| Key Management | AWS KMS / HashiCorp Vault | - |
| Device Fingerprint | FingerprintJS | 4.x |
| Session Storage | Redis | 7.x |
| Database | PostgreSQL | 16.x |

---

**End of Document**
