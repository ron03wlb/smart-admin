# MFA Compliance and Audit Technical Implementation

> **Business Requirements**: [MFA_Compliance_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Compliance_Requirements.md)
> **Audience**: Compliance Officers, Security Engineers, Backend Developers
> **Last Synced**: 2026-02-09

---

## 1. Backup Code Storage and Encryption

### 1.1 AES-256-GCM Encryption

**Algorithm**: AES-256-GCM (Galois/Counter Mode)
**Key Size**: 256 bits
**IV Size**: 96 bits (12 bytes)
**Tag Size**: 128 bits (16 bytes)

**Why Encrypt Backup Codes**:
- Backup codes are as sensitive as passwords
- If stolen, attacker can bypass MFA
- Encryption prevents theft even if database is compromised

### 1.2 Backup Code Encryptor Implementation

```java
@Component
@RequiredArgsConstructor
public class BackupCodeEncryptor {

    private final VaultKeyManager vaultKeyManager;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    /**
     * Encrypt backup code using AES-256-GCM
     *
     * @param plainCode Plain backup code (8-digit numeric)
     * @param userId User identifier (for key derivation)
     * @return Encrypted code (Base64-encoded: IV + ciphertext + tag)
     */
    public String encryptCode(String plainCode, String userId) {
        try {
            // Step 1: Retrieve master encryption key from Vault
            byte[] masterKey = vaultKeyManager.getMasterEncryptionKey();

            // Step 2: Derive user-specific key using HKDF
            byte[] userKey = deriveUserKey(masterKey, userId);

            // Step 3: Generate random IV
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            // Step 4: Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            SecretKeySpec keySpec = new SecretKeySpec(userKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Step 5: Encrypt code
            byte[] plainBytes = plainCode.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            // Step 6: Combine IV + ciphertext + tag
            byte[] combined = new byte[IV_SIZE + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(cipherBytes, 0, combined, IV_SIZE, cipherBytes.length);

            // Step 7: Base64 encode for storage
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt backup code", e);
        }
    }

    /**
     * Decrypt backup code using AES-256-GCM
     *
     * @param encryptedCode Base64-encoded encrypted code
     * @param userId User identifier
     * @return Plain backup code
     */
    public String decryptCode(String encryptedCode, String userId) {
        try {
            // Step 1: Base64 decode
            byte[] combined = Base64.getDecoder().decode(encryptedCode);

            // Step 2: Extract IV and ciphertext
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE);
            byte[] cipherBytes = Arrays.copyOfRange(combined, IV_SIZE, combined.length);

            // Step 3: Retrieve and derive key
            byte[] masterKey = vaultKeyManager.getMasterEncryptionKey();
            byte[] userKey = deriveUserKey(masterKey, userId);

            // Step 4: Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            SecretKeySpec keySpec = new SecretKeySpec(userKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Step 5: Decrypt and verify authentication tag
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (AEADBadTagException e) {
            throw new DecryptionException("Backup code authentication failed (possible tampering)", e);
        } catch (Exception e) {
            throw new DecryptionException("Failed to decrypt backup code", e);
        }
    }

    private byte[] deriveUserKey(byte[] masterKey, String userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(masterKey, "HmacSHA256");
            mac.init(keySpec);

            byte[] info = ("backup-code-" + userId).getBytes(StandardCharsets.UTF_8);
            byte[] hash = mac.doFinal(info);

            return Arrays.copyOf(hash, 32); // 256 bits
        } catch (Exception e) {
            throw new KeyDerivationException("Failed to derive user key", e);
        }
    }
}
```

### 1.3 Database Storage Schema

```sql
CREATE TABLE t_mfa_backup_code (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_encrypted VARCHAR(500) NOT NULL,  -- Base64-encoded (IV + ciphertext + tag)
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encryption_version INT NOT NULL DEFAULT 1,  -- For key rotation
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

CREATE INDEX idx_backup_code_user_id ON t_mfa_backup_code(user_id) WHERE used = FALSE;
```

---

## 2. Identity Document Upload (Account Recovery)

### 2.1 Document Upload Flow

**Supported Document Types**:
- Passport
- Driver's License
- National ID Card
- Utility Bill (proof of address)

**File Format**:
- Accepted: JPG, PNG, PDF
- Max Size: 10 MB
- Resolution: Minimum 1200 x 900 pixels

### 2.2 S3 Upload Implementation

```java
@Service
@RequiredArgsConstructor
public class IdentityDocumentService {

    private final AmazonS3 s3Client;
    private final UserMFADao userMFADao;

    private static final String BUCKET_NAME = "smartadmin-mfa-recovery";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Upload identity document to S3 (for account recovery)
     *
     * @param userId User identifier
     * @param file Uploaded file
     * @param documentType Document type (passport, license, etc.)
     * @return S3 object key
     */
    @Transactional(rollbackFor = Throwable.class)
    public String uploadDocument(Long userId, MultipartFile file, DocumentType documentType) {
        // Step 1: Validate file
        validateFile(file);

        // Step 2: Generate unique S3 key
        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        String s3Key = String.format(
            "identity-docs/%d/%s_%s.%s",
            userId,
            documentType.name().toLowerCase(),
            UUID.randomUUID().toString(),
            fileExtension
        );

        try {
            // Step 3: Upload to S3 with encryption
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);

            s3Client.putObject(
                BUCKET_NAME,
                s3Key,
                file.getInputStream(),
                metadata
            );

            // Step 4: Store reference in database
            IdentityDocumentEntity doc = IdentityDocumentEntity.builder()
                .userId(userId)
                .documentType(documentType)
                .s3Bucket(BUCKET_NAME)
                .s3Key(s3Key)
                .fileSize(file.getSize())
                .fileName(file.getOriginalFilename())
                .uploadedAt(LocalDateTime.now())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

            identityDocumentDao.insert(doc);

            log.info("[IdentityDoc] User {} uploaded {} document: {}",
                userId, documentType, s3Key);

            return s3Key;

        } catch (IOException e) {
            throw new DocumentUploadException("Failed to upload identity document", e);
        }
    }

    /**
     * Retrieve document URL (pre-signed URL with 5-minute expiry)
     *
     * @param userId User identifier
     * @param documentId Document identifier
     * @return Pre-signed S3 URL
     */
    public String getDocumentURL(Long userId, Long documentId) {
        IdentityDocumentEntity doc = identityDocumentDao.selectById(documentId);

        if (doc == null || !doc.getUserId().equals(userId)) {
            throw new BusinessException("Document not found or access denied");
        }

        // Generate pre-signed URL (expires in 5 minutes)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + (5 * 60 * 1000); // 5 minutes
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
            doc.getS3Bucket(),
            doc.getS3Key()
        ).withMethod(HttpMethod.GET)
         .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(request);

        return url.toString();
    }

    private void validateFile(MultipartFile file) {
        // Validate size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds 10 MB limit");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "application/pdf").contains(contentType)) {
            throw new BusinessException("Invalid file type (accepted: JPG, PNG, PDF)");
        }

        // Validate file extension
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!Arrays.asList("jpg", "jpeg", "png", "pdf").contains(extension.toLowerCase())) {
            throw new BusinessException("Invalid file extension");
        }
    }
}
```

### 2.3 Document Verification Workflow

```java
@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private final IdentityDocumentDao documentDao;
    private final NotificationService notificationService;

    /**
     * Admin review and approve/reject identity document
     *
     * @param documentId Document identifier
     * @param status APPROVED or REJECTED
     * @param reviewNote Admin review note
     */
    @Transactional(rollbackFor = Throwable.class)
    public void reviewDocument(Long documentId, VerificationStatus status, String reviewNote) {
        IdentityDocumentEntity doc = documentDao.selectById(documentId);

        if (doc == null) {
            throw new BusinessException("Document not found");
        }

        doc.setVerificationStatus(status);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(reviewNote);

        documentDao.updateById(doc);

        // Send notification to user
        if (status == VerificationStatus.APPROVED) {
            notificationService.sendDocumentApproved(doc.getUserId());
        } else {
            notificationService.sendDocumentRejected(doc.getUserId(), reviewNote);
        }

        log.info("[DocumentVerification] Document {} reviewed: {}", documentId, status);
    }
}
```

---

## 3. Audit Log Implementation

### 3.1 JSONB Storage (PostgreSQL)

**Database Schema**:

```sql
CREATE TABLE t_mfa_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,  -- MFA_ENABLED, MFA_LOGIN_SUCCESS, MFA_LOGIN_FAILED, etc.
    event_data JSONB NOT NULL,        -- Flexible event-specific data
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- Index for user lookup
CREATE INDEX idx_mfa_audit_log_user_id ON t_mfa_audit_log(user_id);

-- Index for event type filtering
CREATE INDEX idx_mfa_audit_log_event_type ON t_mfa_audit_log(event_type);

-- Index for time-based queries
CREATE INDEX idx_mfa_audit_log_created_at ON t_mfa_audit_log(created_at DESC);

-- GIN index for JSONB queries
CREATE INDEX idx_mfa_audit_log_event_data ON t_mfa_audit_log USING GIN (event_data);
```

### 3.2 Audit Event Types

| Event Type | Description | Retention Period |
|-----------|-------------|------------------|
| MFA_ENABLED | User enabled MFA | Permanent |
| MFA_DISABLED | User disabled MFA | Permanent |
| MFA_LOGIN_SUCCESS | Successful MFA verification | 90 days |
| MFA_LOGIN_FAILED | Failed MFA verification | 90 days |
| MFA_ACCOUNT_LOCKED | Account locked after 3 failed attempts | 365 days |
| MFA_BACKUP_CODE_USED | Backup code used for login | 365 days |
| MFA_TRUSTED_DEVICE_ADDED | New trusted device added | 90 days |
| MFA_RECOVERY_INITIATED | Account recovery initiated | 365 days |
| MFA_SECURITY_ALERT | Suspicious activity detected | 365 days |

### 3.3 Audit Logger Implementation

```java
@Service
@RequiredArgsConstructor
public class MFAAuditLogger {

    private final MFAAuditLogDao auditLogDao;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String KAFKA_TOPIC = "mfa-audit-events";

    /**
     * Log MFA event to PostgreSQL + Kafka
     *
     * @param event MFAAuditEvent
     */
    public void log(MFAAuditEvent event) {
        // Step 1: Store in PostgreSQL (immediate persistence)
        MFAAuditLogEntity logEntity = MFAAuditLogEntity.builder()
            .userId(event.getUserId())
            .eventType(event.getEventType())
            .eventData(JsonUtil.toJson(event.getData())) // Convert to JSONB
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .createdAt(LocalDateTime.now())
            .build();

        auditLogDao.insert(logEntity);

        // Step 2: Send to Kafka (for real-time monitoring and SIEM integration)
        try {
            String kafkaMessage = JsonUtil.toJson(event);
            kafkaTemplate.send(KAFKA_TOPIC, event.getUserId().toString(), kafkaMessage);
        } catch (Exception e) {
            log.error("[AuditLog] Failed to send Kafka message for event {}", event.getEventType(), e);
            // Continue execution (Kafka failure should not break audit logging)
        }

        log.info("[AuditLog] Logged event {} for user {}", event.getEventType(), event.getUserId());
    }

    /**
     * Query audit logs for a user
     *
     * @param userId User identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of audit logs
     */
    public List<MFAAuditLogEntity> getUserAuditLogs(Long userId, LocalDate startDate, LocalDate endDate) {
        return auditLogDao.findByUserIdAndDateRange(
            userId,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );
    }

    /**
     * Query audit logs with JSONB filtering
     *
     * @param eventType Event type
     * @param jsonQuery JSONB query (e.g., "event_data @> '{\"failureReason\": \"INVALID_TOTP\"}'")
     * @return List of audit logs
     */
    public List<MFAAuditLogEntity> queryByEventData(String eventType, String jsonQuery) {
        return auditLogDao.findByEventTypeAndJsonQuery(eventType, jsonQuery);
    }
}
```

### 3.4 Kafka Configuration

**application.yml**:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all  # Ensure message is replicated before returning
      retries: 3
    consumer:
      group-id: mfa-audit-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### 3.5 Example Audit Log Entries

**MFA Login Success**:
```json
{
  "userId": 12345,
  "eventType": "MFA_LOGIN_SUCCESS",
  "eventData": {
    "method": "TOTP",
    "deviceFingerprint": "abc123xyz",
    "trustedDevice": true,
    "loginTimestamp": "2026-02-09T10:30:15Z"
  },
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
  "createdAt": "2026-02-09T10:30:15Z"
}
```

**MFA Account Locked**:
```json
{
  "userId": 12345,
  "eventType": "MFA_ACCOUNT_LOCKED",
  "eventData": {
    "reason": "MAX_ATTEMPTS_EXCEEDED",
    "failedAttempts": 3,
    "lockedUntil": "2026-02-09T10:45:00Z",
    "failureDetails": [
      {"timestamp": "2026-02-09T10:28:00Z", "code": "123456", "result": "INVALID"},
      {"timestamp": "2026-02-09T10:29:00Z", "code": "789012", "result": "INVALID"},
      {"timestamp": "2026-02-09T10:30:00Z", "code": "345678", "result": "INVALID"}
    ]
  },
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0",
  "createdAt": "2026-02-09T10:30:15Z"
}
```

---

## 4. Anomaly Detection Implementation

### 4.1 Detection Rules

**Rule 1: Multiple Failed Attempts from New IP**
- Trigger: ≥ 2 failed MFA attempts from an IP address never seen before
- Action: Send security alert + require email verification

**Rule 2: Geographic Anomaly**
- Trigger: Successful MFA login from country different from user's typical location
- Action: Send security alert + optional account freeze

**Rule 3: Excessive Backup Code Usage**
- Trigger: > 3 backup codes used within 7 days
- Action: Force MFA device re-registration

**Rule 4: Trusted Device Token Reuse**
- Trigger: Same trusted device token used from different IP addresses
- Action: Invalidate trusted device token + send security alert

### 4.2 Anomaly Detector Implementation

```java
@Service
@RequiredArgsConstructor
public class MFAAnomalyDetector {

    private final MFAAuditLogDao auditLogDao;
    private final SecurityAlertService alertService;

    /**
     * Rule 1: Multiple failed attempts from new IP
     *
     * @param userId User identifier
     * @param ipAddress Client IP address
     */
    public void detectFailedAttemptsFromNewIP(Long userId, String ipAddress) {
        // Step 1: Check if IP address is known
        boolean isKnownIP = auditLogDao.existsByUserIdAndIP(userId, ipAddress);

        if (!isKnownIP) {
            // Step 2: Count failed attempts from this IP in last 10 minutes
            long failedCount = auditLogDao.countFailedAttempts(
                userId,
                ipAddress,
                LocalDateTime.now().minusMinutes(10)
            );

            if (failedCount >= 2) {
                // Step 3: Trigger security alert
                alertService.sendAlert(SecurityAlert.builder()
                    .userId(userId)
                    .alertType(AlertType.MFA_ANOMALY)
                    .severity(AlertSeverity.HIGH)
                    .title("Multiple MFA failures from unknown IP")
                    .message(String.format(
                        "User %d had %d failed MFA attempts from new IP %s",
                        userId, failedCount, ipAddress
                    ))
                    .build());

                log.warn("[AnomalyDetection] Rule 1 triggered for user {}: {} failed attempts from new IP {}",
                    userId, failedCount, ipAddress);
            }
        }
    }

    /**
     * Rule 2: Geographic anomaly
     *
     * @param userId User identifier
     * @param ipAddress Client IP address
     */
    public void detectGeographicAnomaly(Long userId, String ipAddress) {
        // Step 1: Get user's typical country from historical logins
        String typicalCountry = getUserTypicalCountry(userId);

        // Step 2: Resolve current IP to country
        String currentCountry = geoIPService.resolveCountry(ipAddress);

        if (!currentCountry.equals(typicalCountry)) {
            // Step 3: Check if this is a high-risk country
            boolean isHighRisk = SecurityConfig.HIGH_RISK_COUNTRIES.contains(currentCountry);

            alertService.sendAlert(SecurityAlert.builder()
                .userId(userId)
                .alertType(AlertType.GEOGRAPHIC_ANOMALY)
                .severity(isHighRisk ? AlertSeverity.CRITICAL : AlertSeverity.MEDIUM)
                .title("Login from unusual location")
                .message(String.format(
                    "User %d logged in from %s (typical: %s)",
                    userId, currentCountry, typicalCountry
                ))
                .build());

            log.warn("[AnomalyDetection] Rule 2 triggered for user {}: login from {} (typical: {})",
                userId, currentCountry, typicalCountry);
        }
    }

    /**
     * Rule 3: Excessive backup code usage
     *
     * @param userId User identifier
     */
    public void detectExcessiveBackupCodeUsage(Long userId) {
        // Count backup codes used in last 7 days
        long usedCount = auditLogDao.countBackupCodeUsage(
            userId,
            LocalDateTime.now().minusDays(7)
        );

        if (usedCount > 3) {
            alertService.sendAlert(SecurityAlert.builder()
                .userId(userId)
                .alertType(AlertType.EXCESSIVE_BACKUP_CODE_USAGE)
                .severity(AlertSeverity.HIGH)
                .title("Excessive backup code usage")
                .message(String.format(
                    "User %d used %d backup codes in 7 days (threshold: 3)",
                    userId, usedCount
                ))
                .build());

            // Force MFA device re-registration
            mfaService.disableMFA(userId, "Excessive backup code usage detected");

            log.warn("[AnomalyDetection] Rule 3 triggered for user {}: {} backup codes used in 7 days",
                userId, usedCount);
        }
    }

    /**
     * Rule 4: Trusted device token reuse
     *
     * @param userId User identifier
     * @param deviceToken Trusted device token
     * @param ipAddress Client IP address
     */
    public void detectTrustedDeviceTokenReuse(Long userId, String deviceToken, String ipAddress) {
        // Find all recent uses of this device token
        List<MFAAuditLogEntity> recentUses = auditLogDao.findRecentTrustedDeviceUses(
            userId,
            deviceToken,
            LocalDateTime.now().minusHours(24)
        );

        // Extract distinct IP addresses
        Set<String> ipAddresses = recentUses.stream()
            .map(MFAAuditLogEntity::getIpAddress)
            .collect(Collectors.toSet());

        if (ipAddresses.size() > 1) {
            // Same device token used from multiple IPs
            alertService.sendAlert(SecurityAlert.builder()
                .userId(userId)
                .alertType(AlertType.TRUSTED_DEVICE_TOKEN_REUSE)
                .severity(AlertSeverity.CRITICAL)
                .title("Trusted device token reused from multiple IPs")
                .message(String.format(
                    "User %d's trusted device token was used from %d different IPs: %s",
                    userId, ipAddresses.size(), ipAddresses
                ))
                .build());

            // Invalidate trusted device token
            trustedDeviceService.revokeToken(userId, deviceToken);

            log.error("[AnomalyDetection] Rule 4 triggered for user {}: device token reused from {} IPs",
                userId, ipAddresses.size());
        }
    }

    private String getUserTypicalCountry(Long userId) {
        // Get most frequent country from last 30 days of logins
        return auditLogDao.getMostFrequentCountry(userId, LocalDateTime.now().minusDays(30));
    }
}
```

---

## 5. MFA Enforcement Policy

### 5.1 Role-Based Configuration

**Database Schema**:

```sql
CREATE TABLE t_role_mfa_config (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    mfa_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    allowed_methods TEXT[] NOT NULL,  -- Array: TOTP, SMS, BACKUP_CODES, HARDWARE_TOKEN
    grace_period_days INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Example data
INSERT INTO t_role_mfa_config (role_code, mfa_mandatory, allowed_methods, grace_period_days) VALUES
('SUPER_ADMIN', TRUE, ARRAY['TOTP', 'HARDWARE_TOKEN'], 0),
('FINANCE_MANAGER', TRUE, ARRAY['TOTP', 'HARDWARE_TOKEN'], 7),
('RISK_CONTROL', TRUE, ARRAY['TOTP', 'SMS', 'BACKUP_CODES'], 14),
('DEVELOPER', TRUE, ARRAY['TOTP', 'SMS'], 14),
('CS_AGENT', FALSE, ARRAY['TOTP', 'SMS', 'BACKUP_CODES'], 0),
('MARKETING', FALSE, ARRAY['TOTP', 'SMS'], 0);
```

### 5.2 MFAEnforcementService Implementation

```java
@Service
@RequiredArgsConstructor
public class MFAEnforcementService {

    private final RoleMFAConfigDao roleMFAConfigDao;
    private final UserMFADao userMFADao;

    /**
     * Check if MFA is mandatory for user's role
     *
     * @param role User role
     * @return true if MFA is mandatory
     */
    public boolean isMFAMandatory(Role role) {
        RoleMFAConfig config = roleMFAConfigDao.findByRoleCode(role.getCode());

        return config != null && config.isMfaMandatory();
    }

    /**
     * Get allowed MFA methods for user's role
     *
     * @param role User role
     * @return List of allowed MFA methods
     */
    public List<MFAMethod> getAllowedMethods(Role role) {
        RoleMFAConfig config = roleMFAConfigDao.findByRoleCode(role.getCode());

        if (config == null) {
            return List.of(MFAMethod.TOTP, MFAMethod.SMS); // Default
        }

        return Arrays.stream(config.getAllowedMethods())
            .map(MFAMethod::valueOf)
            .collect(Collectors.toList());
    }

    /**
     * Enforce MFA at login (block if required but not enabled)
     *
     * @param userId User identifier
     * @param role User role
     * @throws MFARequiredException if MFA is mandatory but not enabled
     */
    public void enforceAtLogin(Long userId, Role role) {
        if (isMFAMandatory(role)) {
            UserMFAEntity mfa = userMFADao.findByUserId(userId);

            if (mfa == null || !mfa.isEnabled()) {
                // Check grace period
                RoleMFAConfig config = roleMFAConfigDao.findByRoleCode(role.getCode());
                LocalDateTime gracePeriodEnd = calculateGracePeriodEnd(userId, config.getGracePeriodDays());

                if (LocalDateTime.now().isAfter(gracePeriodEnd)) {
                    throw new MFARequiredException(
                        "MFA is mandatory for your role. Please enable MFA to continue."
                    );
                }
            }
        }
    }

    private LocalDateTime calculateGracePeriodEnd(Long userId, int gracePeriodDays) {
        // Grace period starts from user's role assignment date
        UserRoleEntity userRole = userRoleDao.findByUserId(userId);
        return userRole.getAssignedAt().plusDays(gracePeriodDays);
    }
}
```

---

## 6. Related Documents

### Business Requirements
- [MFA_Compliance_Requirements.md](../../requirements/06_Governance_Licensing/MFA_Compliance_Requirements.md) - Backup code specs, recovery flow, audit requirements

### Technical Implementation
- [MFA_Technical_Evaluation.md](MFA_Technical_Evaluation.md) - TOTP algorithm, AES-256-GCM encryption
- [MFA_Login_Recovery_Technical.md](MFA_Login_Recovery_Technical.md) - Two-phase login, session storage

### Security Standards
- **NIST SP 800-63B**: Digital Identity Guidelines (backup authenticators, audit requirements)
- **GDPR Article 32**: Encryption, logging, and security measures
- **ISO 27001 A.12.4**: Logging and monitoring

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-09
**Maintainer**: Compliance Team, Security Team
