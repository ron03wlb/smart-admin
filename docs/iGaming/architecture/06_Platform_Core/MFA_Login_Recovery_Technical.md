# MFA Login and Recovery Technical Implementation

> **Business Requirements**: [MFA_Recovery_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Recovery_Requirements.md)
> **Audience**: Backend Developers, Security Engineers, DevOps Engineers
> **Last Synced**: 2026-02-09

---

## 1. Two-Phase Login Flow

### 1.1 Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant LoginAPI as LoginController
    participant MFAService
    participant Redis
    participant UserService
    participant TokenService
    participant DB as PostgreSQL

    Note over User,DB: Phase 1: Password Verification

    User->>Frontend: Enter username + password
    Frontend->>LoginAPI: POST /api/auth/login<br/>(username, password)
    LoginAPI->>UserService: validatePassword(username, password)
    UserService->>DB: SELECT * FROM t_user WHERE username = ?
    DB-->>UserService: User record
    UserService->>UserService: BCrypt.checkpw(password, hashedPassword)
    UserService-->>LoginAPI: Password valid

    LoginAPI->>LoginAPI: Check if MFA enabled
    alt MFA Enabled
        LoginAPI->>MFAService: createMFASession(userId)
        MFAService->>MFAService: Generate sessionToken (UUID)
        MFAService->>Redis: SET mfa:session:token<br/>TTL 300s (5 min)
        Redis-->>MFAService: OK
        MFAService-->>LoginAPI: MFASession(sessionToken)
        LoginAPI-->>Frontend: HTTP 200 { requireMFA: true, mfaSessionToken }

        Note over User,DB: Phase 2: MFA Verification

        User->>Frontend: Enter 6-digit TOTP code
        Frontend->>LoginAPI: POST /api/auth/mfa/verify<br/>(mfaSessionToken, totpCode)
        LoginAPI->>Redis: GET mfa:session:token
        Redis-->>LoginAPI: MFASession(userId, createdAt)

        alt Session Expired
            LoginAPI-->>Frontend: HTTP 401 { error: "MFA_SESSION_EXPIRED" }
        end

        LoginAPI->>MFAService: verifyTOTP(userId, totpCode)
        MFAService->>DB: SELECT encrypted_secret FROM t_user_mfa WHERE user_id = ?
        DB-->>MFAService: Encrypted TOTP secret
        MFAService->>MFAService: Decrypt secret (AES-256-GCM)
        MFAService->>MFAService: TOTPGenerator.verify(secret, code)

        alt TOTP Invalid
            MFAService-->>LoginAPI: TOTP verification failed
            LoginAPI->>Redis: INCR mfa:failed:userId
            LoginAPI->>Redis: Check failed count >= 3
            alt Account Locked
                LoginAPI->>DB: UPDATE t_user SET locked_until = NOW() + INTERVAL '15 minutes'
                LoginAPI-->>Frontend: HTTP 423 { error: "MFA_ACCOUNT_LOCKED" }
            else
                LoginAPI-->>Frontend: HTTP 401 { error: "INVALID_TOTP", attemptsRemaining: 2 }
            end
        end

        MFAService-->>LoginAPI: TOTP valid
        LoginAPI->>Redis: DEL mfa:session:token (consume session)
        LoginAPI->>Redis: DEL mfa:failed:userId (reset counter)

        LoginAPI->>TokenService: generateTokens(userId)
        TokenService-->>LoginAPI: AccessToken + RefreshToken

        alt Trust Device Requested
            LoginAPI->>MFAService: createTrustedDeviceToken(userId, deviceFingerprint, ip, userAgent)
            MFAService-->>LoginAPI: Trusted device cookie (30 days)
        end

        LoginAPI-->>Frontend: HTTP 200 { accessToken, refreshToken, trustedDeviceCookie }

    else MFA Not Enabled
        LoginAPI->>TokenService: generateTokens(userId)
        TokenService-->>LoginAPI: AccessToken + RefreshToken
        LoginAPI-->>Frontend: HTTP 200 { accessToken, refreshToken }
    end
```

---

## 2. MFA Session Storage (Redis)

### 2.1 Session Data Structure

**Redis Key Format**:
```
mfa:session:{sessionToken}
```

**Value (JSON)**:
```json
{
  "userId": 12345,
  "username": "john.doe@example.com",
  "createdAt": 1612345678000,
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}
```

**TTL**: 300 seconds (5 minutes)

### 2.2 MFASessionService Implementation

```java
@Service
@RequiredArgsConstructor
public class MFASessionService {

    private final StringRedisTemplate redisTemplate;
    private static final int SESSION_TTL_SECONDS = 300; // 5 minutes
    private static final String SESSION_KEY_PREFIX = "mfa:session:";

    /**
     * Create MFA session after successful password verification
     *
     * @param userId User identifier
     * @param ipAddress Client IP address
     * @param userAgent Client User-Agent
     * @return MFA session token (UUID)
     */
    public String createMFASession(Long userId, String ipAddress, String userAgent) {
        String sessionToken = UUID.randomUUID().toString();

        MFASessionData sessionData = MFASessionData.builder()
            .userId(userId)
            .createdAt(System.currentTimeMillis())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        String key = SESSION_KEY_PREFIX + sessionToken;
        String value = JsonUtil.toJson(sessionData);

        redisTemplate.opsForValue().set(key, value, SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("[MFASession] Created session {} for user {}", sessionToken, userId);

        return sessionToken;
    }

    /**
     * Verify MFA session token is valid
     *
     * @param sessionToken MFA session token
     * @return MFASessionData if valid
     * @throws MFASessionExpiredException if session expired or invalid
     */
    public MFASessionData verifySession(String sessionToken) {
        String key = SESSION_KEY_PREFIX + sessionToken;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new MFASessionExpiredException("MFA session expired or invalid");
        }

        return JsonUtil.fromJson(value, MFASessionData.class);
    }

    /**
     * Consume MFA session (delete after successful verification)
     *
     * @param sessionToken MFA session token
     */
    public void consumeSession(String sessionToken) {
        String key = SESSION_KEY_PREFIX + sessionToken;
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("[MFASession] Consumed session {}", sessionToken);
        }
    }

    /**
     * Extend session TTL (e.g., for slow networks)
     *
     * @param sessionToken MFA session token
     * @param additionalSeconds Additional TTL in seconds
     */
    public void extendSession(String sessionToken, int additionalSeconds) {
        String key = SESSION_KEY_PREFIX + sessionToken;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttl != null && ttl > 0) {
            redisTemplate.expire(key, ttl + additionalSeconds, TimeUnit.SECONDS);
        }
    }
}
```

### 2.3 Failed Attempt Tracking

**Redis Key Format**:
```
mfa:failed:{userId}
```

**Value**: Integer (failed attempt count)
**TTL**: 15 minutes (auto-unlock)

```java
@Service
@RequiredArgsConstructor
public class MFALockoutService {

    private final StringRedisTemplate redisTemplate;
    private static final String FAILED_KEY_PREFIX = "mfa:failed:";
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_SECONDS = 900; // 15 minutes

    public void recordFailedAttempt(Long userId) {
        String key = FAILED_KEY_PREFIX + userId;

        Long failedCount = redisTemplate.opsForValue().increment(key);

        if (failedCount == 1) {
            // Set TTL on first failure
            redisTemplate.expire(key, LOCKOUT_DURATION_SECONDS, TimeUnit.SECONDS);
        }

        if (failedCount >= MAX_ATTEMPTS) {
            lockAccount(userId);
        }
    }

    public int getRemainingAttempts(Long userId) {
        String key = FAILED_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);

        int failedCount = value != null ? Integer.parseInt(value) : 0;
        return Math.max(0, MAX_ATTEMPTS - failedCount);
    }

    public void resetFailedAttempts(Long userId) {
        String key = FAILED_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    private void lockAccount(Long userId) {
        // Update database to lock account for 15 minutes
        // (This ensures lock persists even if Redis is flushed)
        userDao.updateLockedUntil(userId, LocalDateTime.now().plusMinutes(15));

        log.warn("[MFALockout] Account {} locked for 15 minutes", userId);
    }
}
```

---

## 3. Trusted Device Token

### 3.1 Token Generation (SHA256)

**Token Composition**:
```
TrustedDeviceToken = SHA256(userId + deviceFingerprint + ipAddress + userAgent + secret)
```

**Cookie Name**: `trusted_device`
**Attributes**:
- `HttpOnly`: true (prevent JavaScript access)
- `Secure`: true (HTTPS only)
- `SameSite`: Strict (CSRF protection)
- `Max-Age`: 2592000 seconds (30 days)

### 3.2 TrustedDeviceService Implementation

```java
@Service
@RequiredArgsConstructor
public class TrustedDeviceService {

    private final VaultKeyManager vaultKeyManager;

    private static final String COOKIE_NAME = "trusted_device";
    private static final int COOKIE_MAX_AGE_SECONDS = 2592000; // 30 days

    /**
     * Generate trusted device token
     *
     * @param userId User identifier
     * @param deviceFingerprint Browser fingerprint (from FingerprintJS)
     * @param ipAddress Client IP address
     * @param userAgent Client User-Agent
     * @return Base64-encoded SHA256 token
     */
    public String generateToken(Long userId, String deviceFingerprint, String ipAddress, String userAgent) {
        try {
            // Step 1: Retrieve secret from Vault
            String secret = vaultKeyManager.getTrustedDeviceSecret();

            // Step 2: Compose data string
            String data = String.format("%d|%s|%s|%s|%s",
                userId,
                deviceFingerprint,
                ipAddress,
                userAgent,
                secret
            );

            // Step 3: Compute SHA256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Step 4: Base64 encode
            String token = Base64.getEncoder().encodeToString(hash);

            // Step 5: Store metadata in database
            saveTrustedDevice(userId, deviceFingerprint, ipAddress, token);

            return token;

        } catch (NoSuchAlgorithmException e) {
            throw new TrustedDeviceException("Failed to generate trusted device token", e);
        }
    }

    /**
     * Verify trusted device token
     *
     * @param token Token from cookie
     * @param userId User identifier
     * @param deviceFingerprint Browser fingerprint
     * @param ipAddress Client IP address
     * @param userAgent Client User-Agent
     * @return true if token is valid
     */
    public boolean verifyToken(String token, Long userId, String deviceFingerprint, String ipAddress, String userAgent) {
        // Step 1: Re-generate expected token
        String expectedToken = generateToken(userId, deviceFingerprint, ipAddress, userAgent);

        // Step 2: Constant-time comparison
        boolean valid = MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8),
            expectedToken.getBytes(StandardCharsets.UTF_8)
        );

        // Step 3: Additional validation: Check database record exists and not expired
        if (valid) {
            TrustedDeviceEntity device = trustedDeviceDao.findByToken(token);
            if (device == null || device.isExpired()) {
                return false;
            }
        }

        return valid;
    }

    /**
     * Create HTTP cookie for trusted device
     *
     * @param token Trusted device token
     * @param response HttpServletResponse
     */
    public void setCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS only
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }

    /**
     * Remove trusted device cookie (logout)
     *
     * @param response HttpServletResponse
     */
    public void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Immediate expiry

        response.addCookie(cookie);
    }

    private void saveTrustedDevice(Long userId, String deviceFingerprint, String ipAddress, String token) {
        TrustedDeviceEntity device = TrustedDeviceEntity.builder()
            .userId(userId)
            .deviceFingerprint(deviceFingerprint)
            .ipAddress(ipAddress)
            .token(token)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();

        trustedDeviceDao.insert(device);
    }
}
```

### 3.3 Database Schema

```sql
CREATE TABLE t_trusted_device (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    user_agent TEXT,
    token VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- Index for lookup by user
CREATE INDEX idx_trusted_device_user_id ON t_trusted_device(user_id) WHERE deleted = FALSE;

-- Index for token verification
CREATE INDEX idx_trusted_device_token ON t_trusted_device(token) WHERE deleted = FALSE;
```

---

## 4. TOTP QR Code Generation

### 4.1 QR Code Encoding

**QR Code Content** (otpauth URI format):
```
otpauth://totp/SmartAdmin:john.doe@example.com?secret=JBSWY3DPEHPK3PXP&issuer=SmartAdmin&algorithm=SHA1&digits=6&period=30
```

**URI Components**:
- `totp`: TOTP protocol
- `SmartAdmin`: Issuer name
- `john.doe@example.com`: Account name (email or username)
- `secret`: Base32-encoded TOTP secret
- `algorithm`: Hash algorithm (SHA1, SHA256, or SHA512)
- `digits`: Code length (6 or 8)
- `period`: Time step in seconds (30)

### 4.2 QRCodeService Implementation

```java
@Service
public class MFAQRCodeService {

    /**
     * Generate QR code image for TOTP setup
     *
     * @param secret Base32-encoded TOTP secret
     * @param issuer Platform name
     * @param accountName User email or username
     * @return Base64-encoded PNG image
     */
    public String generateQRCodeImage(String secret, String issuer, String accountName) {
        try {
            // Step 1: Generate otpauth URI
            String uri = generateOtpauthURI(secret, issuer, accountName);

            // Step 2: Generate QR code using ZXing library
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                uri,
                BarcodeFormat.QR_CODE,
                300, // width
                300  // height
            );

            // Step 3: Convert BitMatrix to BufferedImage
            BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            // Step 4: Convert BufferedImage to Base64 PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            throw new QRCodeGenerationException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate otpauth URI for TOTP
     *
     * @param secret Base32-encoded TOTP secret
     * @param issuer Platform name
     * @param accountName User email or username
     * @return otpauth URI
     */
    public String generateOtpauthURI(String secret, String issuer, String accountName) {
        try {
            String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
            String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

            return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer,
                encodedAccountName,
                secret,
                encodedIssuer
            );
        } catch (Exception e) {
            throw new QRCodeGenerationException("Failed to generate otpauth URI", e);
        }
    }
}
```

### 4.3 MFA Setup Flow

```java
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MFASetupController {

    private final MFASecretGenerator secretGenerator;
    private final MFAQRCodeService qrCodeService;
    private final MFASecretEncryptor secretEncryptor;
    private final UserMFADao userMFADao;

    /**
     * Step 1: Initiate MFA setup (generate secret and QR code)
     *
     * GET /api/mfa/setup/init
     *
     * @return MFASetupResponse (QR code image, secret backup)
     */
    @GetMapping("/setup/init")
    public ResponseDTO<MFASetupResponse> initSetup(@LoginUser UserDTO user) {
        // Step 1: Generate secret
        String secret = secretGenerator.generateSecret();

        // Step 2: Generate QR code
        String qrCodeImage = qrCodeService.generateQRCodeImage(
            secret,
            "SmartAdmin iGaming",
            user.getUsername()
        );

        // Step 3: Temporarily store secret in Redis (10-minute TTL)
        String tempKey = "mfa:setup:temp:" + user.getUserId();
        redisTemplate.opsForValue().set(tempKey, secret, 600, TimeUnit.SECONDS);

        // Step 4: Return QR code and secret (for manual entry)
        return ResponseDTO.ok(MFASetupResponse.builder()
            .qrCodeImage(qrCodeImage)
            .secretBackup(secret)
            .build());
    }

    /**
     * Step 2: Verify TOTP code and activate MFA
     *
     * POST /api/mfa/setup/verify
     *
     * @param request VerifyTOTPRequest (code)
     * @return Success response with backup codes
     */
    @PostMapping("/setup/verify")
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<MFAActivationResponse> verifySetup(
            @LoginUser UserDTO user,
            @RequestBody VerifyTOTPRequest request) {

        // Step 1: Retrieve temporary secret from Redis
        String tempKey = "mfa:setup:temp:" + user.getUserId();
        String secret = redisTemplate.opsForValue().get(tempKey);

        if (secret == null) {
            throw new BusinessException("MFA setup expired, please restart");
        }

        // Step 2: Verify TOTP code
        boolean valid = totpGenerator.verifyTOTP(secret, request.getCode(), 1);

        if (!valid) {
            throw new BusinessException("Invalid TOTP code, please try again");
        }

        // Step 3: Encrypt secret and store in database
        String encryptedSecret = secretEncryptor.encryptSecret(secret, user.getUserId().toString());

        UserMFAEntity mfa = UserMFAEntity.builder()
            .userId(user.getUserId())
            .encryptedSecret(encryptedSecret)
            .enabled(true)
            .verifiedAt(LocalDateTime.now())
            .build();

        userMFADao.insert(mfa);

        // Step 4: Generate backup codes
        List<String> backupCodes = generateBackupCodes(user.getUserId());

        // Step 5: Delete temporary secret
        redisTemplate.delete(tempKey);

        log.info("[MFASetup] User {} successfully enabled MFA", user.getUserId());

        return ResponseDTO.ok(MFAActivationResponse.builder()
            .success(true)
            .backupCodes(backupCodes)
            .build());
    }

    private List<String> generateBackupCodes(Long userId) {
        // Generate 10 backup codes (8-digit numeric)
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 10; i++) {
            int code = 10000000 + random.nextInt(90000000); // 8-digit
            codes.add(String.valueOf(code));
        }

        // Store encrypted backup codes in database
        // (Implementation in next section)

        return codes;
    }
}
```

---

## 5. Backup Code Implementation

### 5.1 Backup Code Storage

**Database Schema**:

```sql
CREATE TABLE t_mfa_backup_code (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,  -- SHA256 hash of code
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

CREATE INDEX idx_backup_code_user_id ON t_mfa_backup_code(user_id);
```

### 5.2 BackupCodeService Implementation

```java
@Service
@RequiredArgsConstructor
public class MFABackupCodeService {

    private final MFABackupCodeDao backupCodeDao;

    /**
     * Generate and store backup codes
     *
     * @param userId User identifier
     * @return List of plain backup codes (show to user ONCE)
     */
    public List<String> generateBackupCodes(Long userId) {
        List<String> plainCodes = new ArrayList<>();
        List<MFABackupCodeEntity> entities = new ArrayList<>();

        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 10; i++) {
            // Generate 8-digit code
            String code = String.format("%08d", random.nextInt(100000000));
            plainCodes.add(code);

            // Hash code with SHA256
            String codeHash = hashCode(code);

            entities.add(MFABackupCodeEntity.builder()
                .userId(userId)
                .codeHash(codeHash)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build());
        }

        // Batch insert
        backupCodeDao.insertBatch(entities);

        return plainCodes;
    }

    /**
     * Verify backup code (single-use)
     *
     * @param userId User identifier
     * @param code Plain backup code from user
     * @return true if code is valid and unused
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean verifyBackupCode(Long userId, String code) {
        String codeHash = hashCode(code);

        // Find matching code
        MFABackupCodeEntity backupCode = backupCodeDao.findByUserIdAndHash(userId, codeHash);

        if (backupCode == null || backupCode.isUsed()) {
            return false;
        }

        // Mark as used (single-use)
        backupCode.setUsed(true);
        backupCode.setUsedAt(LocalDateTime.now());
        backupCodeDao.updateById(backupCode);

        log.info("[BackupCode] User {} used backup code", userId);

        return true;
    }

    /**
     * Count remaining backup codes
     *
     * @param userId User identifier
     * @return Number of unused codes
     */
    public int getRemainingCount(Long userId) {
        return backupCodeDao.countUnusedCodes(userId);
    }

    /**
     * Hash backup code with SHA256
     *
     * @param code Plain backup code
     * @return SHA256 hash (hex string)
     */
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashingException("Failed to hash backup code", e);
        }
    }
}
```

---

## 6. Device Fingerprinting

### 6.1 FingerprintJS Integration

**Frontend Implementation** (using FingerprintJS):

```javascript
// Install: npm install @fingerprintjs/fingerprintjs

import FingerprintJS from '@fingerprintjs/fingerprintjs';

async function getDeviceFingerprint() {
  const fp = await FingerprintJS.load();
  const result = await fp.get();

  return result.visitorId; // Unique device identifier
}

// Usage during login
async function handleLogin(username, password, trustDevice) {
  const deviceFingerprint = await getDeviceFingerprint();

  const response = await axios.post('/api/auth/login', {
    username,
    password,
    deviceFingerprint,
    trustDevice
  });

  return response.data;
}
```

### 6.2 Device Fingerprint Validation

```java
@Service
public class DeviceFingerprintValidator {

    /**
     * Validate device fingerprint consistency
     *
     * @param storedFingerprint Fingerprint from trusted device record
     * @param currentFingerprint Fingerprint from current request
     * @return true if fingerprints match
     */
    public boolean validate(String storedFingerprint, String currentFingerprint) {
        // Exact match required
        return MessageDigest.isEqual(
            storedFingerprint.getBytes(StandardCharsets.UTF_8),
            currentFingerprint.getBytes(StandardCharsets.UTF_8)
        );
    }
}
```

---

## 7. Configuration Reference

### 7.1 Redis Configuration

**application.yml**:

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
        max-wait: 3000ms

mfa:
  session:
    ttl-seconds: 300  # 5 minutes
  lockout:
    max-attempts: 3
    duration-seconds: 900  # 15 minutes
  trusted-device:
    max-age-days: 30
```

### 7.2 Cookie Security Configuration

**Spring Security Cookie Configuration**:

```java
@Configuration
public class CookieSecurityConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("trusted_device");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(true); // HTTPS only
        serializer.setSameSite("Strict");    // CSRF protection
        serializer.setCookieMaxAge(2592000); // 30 days
        return serializer;
    }
}
```

---

## 8. Related Documents

### Business Requirements
- [MFA_Recovery_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Recovery_Requirements.md) - MFA session rules, trusted device policy, lockout rules

### Technical Implementation
- [MFA_Technical_Evaluation.md](MFA_Technical_Evaluation.md) - TOTP algorithm, secret encryption, security analysis
- [MFA_Compliance_Technical.md](MFA_Compliance_Technical.md) - Audit logs, compliance validation

### Security Standards
- **RFC 6238**: TOTP Specification
- **OWASP Session Management Cheat Sheet**
- **SameSite Cookie Specification**

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-09
**Maintainer**: Security Team, Backend Team
