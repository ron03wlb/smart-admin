# MFA 合規技術（MFA Compliance Technical）

> **目標讀者**: Security Engineers, Compliance Officers
> **業務需求**: [MFA 合規需求](../../requirements/06_Governance_Licensing/05_MFA_Compliance_Requirements.md)
> **XREF 架構設計**: [03_MFA_Architecture.md](03_MFA_Architecture.md)
> **最後更新**: 2026-03-31（合併自 07_MFA_Compliance_Technical.md + 08_MFA_Compliance_Validation.md）

---

## 1. 備份碼儲存與加密（Backup Code Storage and Encryption）

### 1.1 AES-256-GCM 加密（AES-256-GCM Encryption）

**演算法**: AES-256-GCM (Galois/Counter Mode)
**金鑰大小**: 256 bits
**初始向量大小**: 96 bits (12 bytes)
**標籤大小**: 128 bits (16 bytes)

**為何需要加密備份碼**:
- 備份碼與密碼同等敏感
- 若被竊取,攻擊者可繞過 MFA
- 加密可防止資料庫被入侵時備份碼洩露

### 1.2 備份碼加密器實現（Backup Code Encryptor Implementation）

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

### 1.3 資料庫儲存架構（Database Storage Schema）

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

## 2. 身份證件上傳（帳戶恢復）（Identity Document Upload - Account Recovery）

### 2.1 證件上傳流程（Document Upload Flow）

**支援的證件類型**:
- 護照（Passport）
- 駕照（Driver's License）
- 國民身份證（National ID Card）
- 水電帳單（地址證明）（Utility Bill - proof of address）

**檔案格式**:
- 接受格式: JPG, PNG, PDF
- 最大大小: 10 MB
- 解析度: 最小 1200 x 900 像素

### 2.2 S3 上傳實現（S3 Upload Implementation）

```java
/**
 * Manager class for identity document persistence operations.
 * SmartAdmin Pattern: @Transactional only in Manager layer.
 */
@Component
@RequiredArgsConstructor
public class IdentityDocumentManager {

    private final IdentityDocumentDao documentDao;

    /**
     * Persist document reference in database (transactional).
     */
    @Transactional(rollbackFor = Throwable.class)
    public void saveDocumentReference(IdentityDocumentEntity doc) {
        documentDao.insert(doc);
    }
}

/**
 * Service class for identity document orchestration.
 * Delegates transactional operations to IdentityDocumentManager.
 */
@Service
@RequiredArgsConstructor
public class IdentityDocumentService {

    private final AmazonS3 s3Client;
    private final UserMFADao userMFADao;
    private final IdentityDocumentManager documentManager;

    private static final String BUCKET_NAME = "smartadmin-mfa-recovery";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Upload identity document to S3 (for account recovery).
     * Delegates persistence to IdentityDocumentManager.
     *
     * @param userId User identifier
     * @param file Uploaded file
     * @param documentType Document type (passport, license, etc.)
     * @return S3 object key
     */
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

            // Step 4: Store reference in database (delegate to Manager)
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

            documentManager.saveDocumentReference(doc);

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

### 2.3 證件驗證工作流程（Document Verification Workflow）

```java
/**
 * Manager class for document verification persistence.
 * SmartAdmin Pattern: @Transactional only in Manager layer.
 */
@Component
@RequiredArgsConstructor
public class DocumentVerificationManager {

    private final IdentityDocumentDao documentDao;

    /**
     * Update document verification status (transactional).
     */
    @Transactional(rollbackFor = Throwable.class)
    public IdentityDocumentEntity updateVerificationStatus(
            Long documentId,
            VerificationStatus status,
            String reviewNote) {
        IdentityDocumentEntity doc = documentDao.selectById(documentId);

        if (doc == null) {
            throw new BusinessException("Document not found");
        }

        doc.setVerificationStatus(status);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(reviewNote);

        documentDao.updateById(doc);
        return doc;
    }
}

/**
 * Service class for document verification orchestration.
 * Delegates transactional operations to DocumentVerificationManager.
 */
@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private final DocumentVerificationManager verificationManager;
    private final NotificationService notificationService;

    /**
     * Admin review and approve/reject identity document.
     * Delegates persistence to DocumentVerificationManager.
     *
     * @param documentId Document identifier
     * @param status APPROVED or REJECTED
     * @param reviewNote Admin review note
     */
    public void reviewDocument(Long documentId, VerificationStatus status, String reviewNote) {
        // Delegate transactional operation to Manager
        IdentityDocumentEntity doc = verificationManager.updateVerificationStatus(
            documentId, status, reviewNote);

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

## 3. 審計日誌實現（Audit Log Implementation）

### 3.1 JSONB 儲存（PostgreSQL）

**資料庫架構**:

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

### 3.2 審計事件類型（Audit Event Types）

| 事件類型 | 描述 | 保留期限 |
|---------|------|---------|
| MFA_ENABLED | 使用者啟用 MFA | 永久保留 |
| MFA_DISABLED | 使用者停用 MFA | 永久保留 |
| MFA_LOGIN_SUCCESS | MFA 驗證成功 | 90 天 |
| MFA_LOGIN_FAILED | MFA 驗證失敗 | 90 天 |
| MFA_ACCOUNT_LOCKED | 3 次失敗後帳戶鎖定 | 365 天 |
| MFA_BACKUP_CODE_USED | 使用備份碼登入 | 365 天 |
| MFA_TRUSTED_DEVICE_ADDED | 新增受信任裝置 | 90 天 |
| MFA_RECOVERY_INITIATED | 啟動帳戶恢復 | 365 天 |
| MFA_SECURITY_ALERT | 偵測到可疑活動 | 365 天 |

### 3.3 審計記錄器實現（Audit Logger Implementation）

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

### 3.4 Kafka 配置（Kafka Configuration）

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

### 3.5 審計日誌條目範例（Example Audit Log Entries）

**MFA 登入成功**:
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

**MFA 帳戶鎖定**:
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

## 4. 異常偵測實現（Anomaly Detection Implementation）

### 4.1 偵測規則（Detection Rules）

**規則 1: 新 IP 地址多次失敗嘗試**
- 觸發條件: 從未見過的 IP 地址進行 ≥ 2 次 MFA 失敗嘗試
- 處理動作: 發送安全警報 + 要求電子郵件驗證

**規則 2: 地理位置異常**
- 觸發條件: 從與使用者慣用地點不同的國家成功 MFA 登入
- 處理動作: 發送安全警報 + 可選帳戶凍結

**規則 3: 備份碼過度使用**
- 觸發條件: 7 天內使用 > 3 個備份碼
- 處理動作: 強制 MFA 裝置重新註冊

**規則 4: 受信任裝置權杖重複使用**
- 觸發條件: 同一受信任裝置權杖從不同 IP 地址使用
- 處理動作: 撤銷受信任裝置權杖 + 發送安全警報

### 4.2 異常偵測器實現（Anomaly Detector Implementation）

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

## 5. MFA 強制執行策略（MFA Enforcement Policy）

### 5.1 基於角色的配置（Role-Based Configuration）

**資料庫架構**:

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

### 5.2 MFAEnforcementService 實現（MFAEnforcementService Implementation）

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

## 6. MFA 挑戰-回應流程（MFA Challenge-Response Flow）

### 6.1 端到端 MFA 驗證序列（End-to-End MFA Verification Sequence）

以下圖表說明從初始登入到成功驗證的完整 MFA 挑戰-回應流程:

```mermaid
sequenceDiagram
    participant U as 使用者
    participant F as 前端
    participant A as Auth Controller
    participant M as MFA Service
    participant L as MFA Audit Logger
    participant DB as PostgreSQL

    Note over U,DB: 階段 1: 主要驗證
    U->>F: 使用帳號 + 密碼登入
    F->>A: POST /api/auth/login
    A->>A: 驗證憑證

    alt 憑證無效
        A-->>F: 401 Unauthorized
        F-->>U: 顯示錯誤
    end

    Note over U,DB: 階段 2: MFA 挑戰
    A->>M: checkMFARequired(userId)
    M->>DB: SELECT * FROM t_user_mfa WHERE user_id = ?
    DB-->>M: MFA 配置 (enabled, method)

    alt 需要 MFA
        M->>A: MFA Required: TOTP
        A-->>F: 200 OK + requiresMFA: true + sessionToken
        F-->>U: 顯示 MFA 輸入表單

        Note over U,DB: 階段 3: MFA 代碼提交
        U->>F: 輸入 TOTP 代碼 (6 位數)
        F->>A: POST /api/auth/mfa/verify<br/>{sessionToken, code}

        A->>M: verifyMFACode(userId, code)
        M->>DB: SELECT totp_secret FROM t_user_mfa
        DB-->>M: 加密的 TOTP 密鑰
        M->>M: 解密密鑰 (AES-256-GCM)
        M->>M: 生成預期的 TOTP<br/>(時間窗口 ±1)

        alt 代碼有效
            M->>L: log(MFA_LOGIN_SUCCESS)
            L->>DB: INSERT INTO t_mfa_audit_log
            M-->>A: 驗證成功
            A->>A: 生成 JWT access token
            A-->>F: 200 OK + accessToken + refreshToken
            F-->>U: 重導向至儀表板
        else 代碼無效
            M->>L: log(MFA_LOGIN_FAILED)
            L->>DB: INSERT INTO t_mfa_audit_log
            M->>M: 增加 failedAttempts

            alt 失敗次數 >= 3
                M->>M: 鎖定帳戶 (15 分鐘)
                M->>L: log(MFA_ACCOUNT_LOCKED)
                M-->>A: 帳戶已鎖定
                A-->>F: 423 Locked + lockedUntil
                F-->>U: 帳戶鎖定訊息
            else 失敗次數 < 3
                M-->>A: 代碼無效 (attemptsRemaining)
                A-->>F: 401 Unauthorized + attemptsRemaining
                F-->>U: 顯示錯誤 + 重試
            end
        end
    else 不需要 MFA
        A->>A: 生成 JWT access token
        A-->>F: 200 OK + accessToken
        F-->>U: 重導向至儀表板
    end

    Note over U,DB: 可選: 備份碼回退
    U->>F: 點擊「使用備份碼」
    F->>A: POST /api/auth/mfa/backup-code<br/>{sessionToken, backupCode}
    A->>M: verifyBackupCode(userId, backupCode)
    M->>DB: SELECT * FROM t_mfa_backup_code<br/>WHERE user_id = ? AND used = FALSE
    DB-->>M: 備份碼清單 (已加密)
    M->>M: 解密每個代碼 (AES-256-GCM)
    M->>M: 與輸入比對

    alt 備份碼有效
        M->>DB: UPDATE t_mfa_backup_code<br/>SET used = TRUE, used_at = NOW()
        M->>L: log(MFA_BACKUP_CODE_USED)
        M-->>A: 驗證成功
        A-->>F: 200 OK + accessToken
        F-->>U: 重導向至儀表板
    else 備份碼無效
        M->>L: log(MFA_LOGIN_FAILED)
        M-->>A: 備份碼無效
        A-->>F: 401 Unauthorized
        F-->>U: 顯示錯誤
    end
```

### 6.2 關鍵安全措施（Key Security Measures）

**基於時間的窗口（TOTP）**:
- 接受 T-30s 到 T+30s 的代碼（±1 時間步驟）
- 透過基於時間的失效防止重放攻擊
- 30 秒窗口 = 任何時間有 3 個有效代碼（T-1, T, T+1）

**帳戶鎖定**:
- 3 次失敗嘗試 → 15 分鐘鎖定
- 鎖定時長儲存在 Redis（自動過期）
- 防止暴力破解攻擊

**備份碼一次性使用**:
- 每個備份碼僅能使用一次
- 驗證後在資料庫中將 `used` 標記設為 TRUE
- 使用備份碼時向使用者發送通知

**審計軌跡**:
- 每次 MFA 嘗試都記錄到 PostgreSQL + Kafka
- JSONB 儲存提供靈活的事件資料
- 登入事件保留 90 天，安全警報保留 365 天

---

## 7. 相關文件（Related Documents）

### 業務需求（Business Requirements）
- [MFA_Compliance_Requirements.md](../../requirements/06_Governance_Licensing/05_MFA_Compliance_Requirements.md) - 備份碼規格、恢復流程、審計需求

### 技術實現（Technical Implementation）
- [MFA_Technical_Evaluation.md](05_MFA_Technical_Evaluation.md) - TOTP 演算法、AES-256-GCM 加密
- [MFA_Login_Recovery_Technical.md](10_MFA_Login_Recovery_Technical.md) - 兩階段登入、會話儲存

### 安全標準（Security Standards）
- **NIST SP 800-63B**: 數位身份指南（備份驗證器、審計需求）
- **GDPR Article 32**: 加密、日誌記錄和安全措施
- **ISO 27001 A.12.4**: 日誌記錄與監控

---

**文件版本**: 1.0.0
**最後更新**: 2026-02-09
**維護者**: 合規團隊、安全團隊

---

## 附錄 B：備份碼管理 API 與設備恢復（Backup Code Management API & Device Recovery）

## 3. 備用碼管理 API（Backup Code Management API）

### 3.1 查看剩餘備用碼狀態（View Remaining Backup Code Status）

```http
GET /admin/mfa/backup-codes/status
Authorization: Bearer {accessToken}
```

**回應（Response）**：

```json
{
  "code": 200,
  "msg": "Success",
  "data": {
    "totalCodes": 10,
    "usedCodes": 3,
    "remainingCodes": 7,
    "lastUsedAt": "2026-02-05T10:30:00Z"
  }
}
```

### 3.2 重新生成備用碼（Regenerate Backup Codes）

```http
POST /admin/mfa/backup-codes/regenerate
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "totpCode": "123456"
}
```

**回應（Response）**：

```json
{
  "code": 200,
  "msg": "Backup codes regenerated successfully.",
  "data": {
    "backupCodes": [
      "1111-2222",
      "3333-4444",
      "5555-6666"
    ]
  }
}
```

---

## 4. 設備遺失恢復實現（Device Loss Recovery Implementation）

### 4.1 恢復請求流程（Recovery Request Flow）

```mermaid
graph TD
    A[使用者提交恢復請求] --> B[驗證身份證件<br/>護照 / 駕照 / 身份證]
    B --> C[驗證電子郵件<br/>發送 OTP 至註冊郵箱]
    C --> D[驗證手機<br/>發送 SMS OTP]
    D --> E[安全問題驗證<br/>3 個預先配置的問題]
    E --> F{所有驗證通過？}

    F -->|通過| G[人工審核<br/>安全團隊批准]
    F -->|失敗| H[拒絕恢復請求<br/>聯繫客服]

    G --> I[安全團隊登入<br/>強制重置 MFA]
    I --> J[生成新的 TOTP Secret<br/>發送給使用者]
    J --> K[使用者掃描新 QR Code<br/>重新啟用 MFA]
```

### 4.2 恢復請求 API（Recovery Request API）

```http
POST /admin/mfa/recovery/request
Content-Type: multipart/form-data

{
  "userId": 12345,
  "idCardPhoto": <File>,
  "reason": "Phone lost, cannot use Google Authenticator"
}
```

### 4.3 恢復請求數據庫架構（Recovery Request Database Schema）

```sql
-- t_mfa_recovery_request table
CREATE TABLE t_mfa_recovery_request (
    request_id      BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    id_card_photo   TEXT,                       -- ID document photo URL
    reason          TEXT,
    status          VARCHAR(20) NOT NULL,       -- PENDING / APPROVED / REJECTED
    reviewed_by     BIGINT REFERENCES t_admin_user(user_id),
    reviewed_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### 4.4 安全團隊審核介面（Security Team Review Interface）

```vue
<template>
  <a-table :dataSource="recoveryRequests" :columns="columns">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'action'">
        <a-space>
          <a-button type="primary" @click="approveRequest(record)">
            Approve
          </a-button>
          <a-button danger @click="rejectRequest(record)">
            Reject
          </a-button>
        </a-space>
      </template>
    </template>
  </a-table>
</template>

<script setup>
const recoveryRequests = ref([]);

async function approveRequest(record) {
  await fetch(`/admin/mfa/recovery/${record.requestId}/approve`, {
    method: 'POST'
  });

  message.success('Recovery request approved');
  loadRequests();
}
</script>
```

### 4.5 強制重置 MFA 實現（Force Reset MFA Implementation）

```java
/**
 * Manager class for MFA force reset operations.
 * SmartAdmin Pattern: @Transactional only in Manager layer with @Component.
 */
@Component
@RequiredArgsConstructor
public class MfaResetManager {

    private final MfaRepository mfaRepository;
    private final AuditLogService auditLogService;

    /**
     * Force reset MFA for a user (transactional).
     * Deletes old MFA records and prepares for re-enrollment.
     */
    @Transactional(rollbackFor = Throwable.class)
    public void forceResetMfaRecords(Long userId, Long reviewerUserId) {
        // Step 1: Delete old MFA records
        mfaRepository.deleteByUserId(userId);

        // Step 2: Record audit log (CRITICAL level)
        auditLogService.log(userId, "MFA_FORCE_RESET", String.format(
            "Security Team force reset MFA (reviewer: %d)", reviewerUserId
        ));
    }
}

/**
 * Service class for MFA reset orchestration.
 * Delegates transactional operations to MfaResetManager.
 */
@Service
@RequiredArgsConstructor
public class MfaResetService {

    private final MfaResetManager mfaResetManager;
    private final EmailService emailService;

    /**
     * Force reset MFA for a user.
     * Orchestrates the reset flow and email notification.
     */
    public ResponseDTO<Void> forceResetMfa(Long userId, Long reviewerUserId) {
        // Delegate transactional operation to Manager
        mfaResetManager.forceResetMfaRecords(userId, reviewerUserId);

        // Generate new TOTP Secret
        String newSecret = TotpSecretGenerator.generateSecret();
        String qrCode = QrCodeGenerator.generateQrCode(
            getUserEmail(userId),
            newSecret,
            "SmartAdmin iGaming"
        );

        // Send email to user
        emailService.sendMfaResetEmail(userId, qrCode, newSecret);

        return ResponseDTO.ok();
    }
}
```

---

## 5. 緊急聯絡人數據庫架構（Emergency Contact Database Schema）

```sql
-- t_mfa_emergency_contact table
CREATE TABLE t_mfa_emergency_contact (
    user_id             BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_user_id     BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_type        VARCHAR(20) NOT NULL,   -- COLLEAGUE / MANAGER
    verified            BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, contact_user_id)
);
```

### 5.1 驗證電子郵件範本（Verification Email Template）

```html
<p>Hello {{contactName}},</p>

<p>{{userName}} ({{userEmail}}) has submitted an MFA recovery request, claiming phone loss.</p>

<p>Please click the link below to confirm this is a legitimate request:</p>

<a href="https://admin.smartadmin.com/mfa/recovery/verify?token={{verificationToken}}">
  Confirm Recovery Request
</a>

<p>If this is not a legitimate request, please contact Security Team immediately.</p>
```

---

## 6. 基於角色的 MFA 策略實現（Role-Based MFA Policy Implementation）

### 6.1 MFA 策略服務（MFA Policy Service）

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
     * @return true if mandatory MFA required, false if optional
     */
    public boolean requiresMandatoryMfa(Long userId) {
        List<String> roles = adminUserService.getUserRoles(userId);

        // Mandatory MFA role list
        Set<String> mandatoryMfaRoles = Set.of(
            "SUPER_ADMIN",
            "FINANCE_MANAGER",
            "RISK_CONTROL",
            "DATABASE_ADMIN",
            "DEVOPS"
        );

        // Check if user belongs to a mandatory MFA role
        return roles.stream().anyMatch(mandatoryMfaRoles::contains);
    }

    /**
     * Pre-login MFA status validation
     *
     * @param userId User ID
     * @throws MfaNotEnabledException if user requires MFA but has not enabled it
     */
    public void validateMfaStatusBeforeLogin(Long userId) {
        if (requiresMandatoryMfa(userId)) {
            boolean mfaEnabled = mfaRepository.isMfaEnabled(userId);

            if (!mfaEnabled) {
                throw new MfaNotEnabledException(
                    "Your role requires MFA to be enabled before login. Please contact an administrator."
                );
            }
        }
    }
}
```

### 6.2 首次登入強制設定流程（First Login Mandatory Setup Flow）

```mermaid
graph TD
    A[使用者登入] --> B{密碼驗證}
    B -->|通過| C{檢查角色}

    C -->|高風險角色| D{MFA 已啟用？}
    C -->|低風險角色| G[直接登入]

    D -->|是| E[要求 MFA 驗證]
    D -->|否| F[強制重定向至 MFA 設定頁面<br/>無法跳過]

    F --> H[掃描 QR Code]
    H --> I[驗證啟用]
    I --> E

    E --> J{TOTP 驗證}
    J -->|通過| G
    J -->|失敗| K[重新輸入代碼]
```

### 6.3 可選 MFA 建議（Optional MFA Recommendation）（低風險角色）

```java
/**
 * Post-login MFA recommendation prompt
 */
public void showMfaRecommendation(Long userId) {
    List<String> roles = adminUserService.getUserRoles(userId);
    boolean mfaEnabled = mfaRepository.isMfaEnabled(userId);

    if (!mfaEnabled && roles.contains("CUSTOMER_SERVICE")) {
        // Display recommendation banner (non-blocking)
        return ResponseDTO.ok().withWarning(
            "We recommend enabling MFA to enhance account security. Click to set up."
        );
    }
}
```

### 6.4 前端 MFA 建議橫幅（Frontend MFA Recommendation Banner）

```vue
<template>
  <a-alert
    v-if="showMfaBanner"
    type="warning"
    message="Enable Multi-Factor Authentication (MFA)"
    description="Your role has access to player privacy data. Enabling MFA effectively prevents account compromise."
    closable
    @close="dismissBanner"
  >
    <template #action>
      <a-button type="primary" @click="navigateToMfaSetup">
        Set Up Now
      </a-button>
    </template>
  </a-alert>
</template>
```

### 6.5 決策框架圖（Decision Framework Diagram）

```mermaid
graph TD
    A[角色 MFA 策略] --> B{維度 1：風險暴露}
    A --> C{維度 2：操作可逆性}
    A --> D{維度 3：合規要求}

    B --> B1[Super Admin：5/5<br/>可修改系統配置]
    B --> B2[Finance：5/5<br/>可調整玩家餘額]
    B --> B3[Customer Service：3/5<br/>僅查詢權限]

    C --> C1[Super Admin：否<br/>不可逆]
    C --> C2[Finance：否<br/>不可逆]
    C --> C3[Customer Service：是<br/>可審計]

    D --> D1[Super Admin：必須<br/>PCI DSS 強制要求]
    D --> D2[Finance：必須<br/>PCI DSS 強制要求]
    D --> D3[Customer Service：建議<br/>GDPR 建議]
```

---

## 7. 審計日誌數據庫架構（Audit Log Database Schema）

### 7.1 MFA 審計日誌表（MFA Audit Log Table）

```sql
CREATE TABLE t_audit_log_mfa (
    log_id              BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    event_type          VARCHAR(50) NOT NULL,
    event_level         VARCHAR(20) NOT NULL,   -- INFO / WARNING / CRITICAL
    ip_address          INET,
    user_agent          TEXT,
    device_fingerprint  VARCHAR(64),
    details             JSONB,                   -- Additional details (JSON format)
    created_at          TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_mfa_log_user_id ON t_audit_log_mfa(user_id);
CREATE INDEX idx_mfa_log_event_type ON t_audit_log_mfa(event_type);
CREATE INDEX idx_mfa_log_event_level ON t_audit_log_mfa(event_level);
CREATE INDEX idx_mfa_log_created_at ON t_audit_log_mfa(created_at);
```

### 7.2 審計日誌條目範例（Audit Log Entry Example）

```json
{
  "logId": 12345,
  "userId": 67890,
  "eventType": "MFA_LOGIN_FAILED",
  "eventLevel": "WARNING",
  "ipAddress": "203.0.113.42",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...",
  "deviceFingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "details": {
    "reason": "INVALID_TOTP",
    "attemptCount": 2,
    "remainingAttempts": 1
  },
  "createdAt": "2026-02-05T10:30:15Z"
}
```

---

## 8. 自動化異常檢測查詢（Automated Anomaly Detection Queries）

### 8.1 規則 1（Rule 1）：短時間內多次 MFA 失敗（Multiple MFA Failures in Short Period）

```sql
-- Detection: >= 3 MFA failures within 5 minutes
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

**觸發動作（Triggered actions）**：
- 鎖定帳戶 15 分鐘
- 向安全團隊發送警報
- 向使用者發送電子郵件通知

### 8.2 規則 2（Rule 2）：異常地理位置登入（Abnormal Geographic Location Login）

```sql
-- Detection: Logins from different countries within 1 hour
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

**觸發動作（Triggered actions）**：
- 警報安全團隊進行人工審核
- 停用受信任設備，下次登入要求完整 MFA

### 8.3 規則 3（Rule 3）：頻繁使用備用碼（Frequent Backup Code Usage）

```sql
-- Detection: >= 3 backup code uses within 7 days
SELECT
    user_id,
    COUNT(*) AS backup_code_usage
FROM t_audit_log_mfa
WHERE event_type = 'BACKUP_CODE_USED'
  AND created_at >= NOW() - INTERVAL '7 days'
GROUP BY user_id
HAVING COUNT(*) >= 3;
```

**觸發動作（Triggered actions）**：
- 警告使用者重新配置 TOTP
- 提示使用者可能設備遺失

### 8.4 規則 4（Rule 4）：高風險角色停用 MFA（MFA Disabled for High-Risk Role）

```sql
-- Detection: MFA disabled for mandatory MFA roles
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

**觸發動作（Triggered actions）**：
- 立即警報 CTO / CISO
- 需要人工審核
- 如果未經授權，作為安全事件鎖定帳戶

---

## 9. 集成測試範例（Integration Test Examples）

```java
@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor
class MfaIntegrationTest {

    private final MockMvc mockMvc;

    private final MfaRepository mfaRepository;

    @Test
    @DisplayName("TC-MFA-001: User first-time TOTP setup")
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
        assertThat(mfa.getTotpSecret()).isNotBlank();  // Encrypted Secret
    }

    @Test
    @DisplayName("TC-MFA-004: Account locked after 3 consecutive TOTP failures")
    void testMfaLockAfterThreeFailures() throws Exception {
        // Given
        Long userId = 12345L;
        String mfaSessionToken = "mfa_sess_test123";

        // When: 3 incorrect attempts
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

## 10. 相關文檔（Related Documents）

- [06-06-01 MFA Architecture Design](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) - 業務需求和方法選擇
- [06-06-02 TOTP & WebAuthn Implementation](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md) - TOTP 算法細節
- [06-06-03 Login & Recovery Flow](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) - 認證流程設計
