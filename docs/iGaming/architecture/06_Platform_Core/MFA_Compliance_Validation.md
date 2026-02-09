# MFA Compliance Validation - Technical Architecture

> **Canonical Source**: [06-06-04_Compliance_Audit.md](../../source-archive/06_Platform_Governance/06-06-04_Compliance_Audit.md)
> **Audience**: Architects, Backend Developers, DevOps
> **Business Requirements**: [MFA Compliance Requirements](../../requirements/06_Governance_Licensing/MFA_Compliance_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

This document covers the technical implementation of MFA compliance validation, including backup code generation algorithms, audit log database schemas, automated anomaly detection queries, API specifications, and integration test patterns for the SmartAdmin iGaming platform.

---

## 2. Backup Code Implementation

### 2.1 Generation Algorithm

```java
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BackupCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate backup codes (8-digit, format: 1234-5678)
     *
     * @param count Number of codes to generate (recommended: 10)
     * @return List of backup codes
     */
    public static List<String> generate(int count) {
        List<String> codes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            // Generate 8-digit number (10000000 ~ 99999999)
            int code = RANDOM.nextInt(90000000) + 10000000;

            // Format as 1234-5678 (human-readable)
            String formatted = String.format("%04d-%04d",
                code / 10000,
                code % 10000
            );

            codes.add(formatted);
        }

        return codes;
    }
}

// Example output
// List<String> backupCodes = BackupCodeGenerator.generate(10);
// [1234-5678, 9012-3456, 7890-1234, ...]
```

### 2.2 Storage Schema (JSON)

```json
{
  "codes": [
    {
      "code": "1234-5678",
      "used": false,
      "usedAt": null
    },
    {
      "code": "9012-3456",
      "used": false,
      "usedAt": null
    },
    {
      "code": "7890-1234",
      "used": true,
      "usedAt": "2026-02-05T10:30:00Z"
    }
  ],
  "generatedAt": "2026-02-01T10:00:00Z"
}
```

### 2.3 Backup Code Verification Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant C as Controller
    participant M as MfaManager
    participant D as Database

    U->>C: POST /admin/auth/mfa/verify<br/>{backupCode: "1234-5678"}
    C->>M: verifyBackupCode(userId, code)
    M->>D: Query backup codes (decrypt)
    D-->>M: JSON backup code array

    M->>M: Iterate and check backup code match

    alt Backup code valid and unused
        M->>D: Mark backup code as used<br/>used = true
        M->>D: Record audit log (BACKUP_CODE_USED)

        alt Remaining backup codes <= 2
            M->>U: WARNING: Only X codes remaining<br/>Recommend regeneration
        end

        M-->>C: Verification success
        C-->>U: Issue Tokens
    else Backup code invalid or already used
        M->>D: Record audit log (BACKUP_CODE_INVALID)
        M-->>C: HTTP 401<br/>{error: "INVALID_BACKUP_CODE"}
    end
```

---

## 3. Backup Code Management API

### 3.1 View Remaining Backup Code Status

```http
GET /admin/mfa/backup-codes/status
Authorization: Bearer {accessToken}
```

**Response**:

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

### 3.2 Regenerate Backup Codes

```http
POST /admin/mfa/backup-codes/regenerate
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "totpCode": "123456"
}
```

**Response**:

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

## 4. Device Loss Recovery Implementation

### 4.1 Recovery Request Flow

```mermaid
graph TD
    A[User Submits Recovery Request] --> B[Verify Identity Document<br/>Passport / License / ID Card]
    B --> C[Verify Email<br/>Send OTP to Registered Email]
    C --> D[Verify Phone<br/>Send SMS OTP]
    D --> E[Security Question Verification<br/>3 Pre-configured Questions]
    E --> F{All Verifications Passed?}

    F -->|PASS| G[Human Review<br/>Security Team Approval]
    F -->|FAIL| H[Reject Recovery Request<br/>Contact Customer Service]

    G --> I[Security Team Login<br/>Force Reset MFA]
    I --> J[Generate New TOTP Secret<br/>Send to User]
    J --> K[User Scans New QR Code<br/>Re-activate MFA]
```

### 4.2 Recovery Request API

```http
POST /admin/mfa/recovery/request
Content-Type: multipart/form-data

{
  "userId": 12345,
  "idCardPhoto": <File>,
  "reason": "Phone lost, cannot use Google Authenticator"
}
```

### 4.3 Recovery Request Database Schema

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

### 4.4 Security Team Review Interface

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

### 4.5 Force Reset MFA Implementation

```java
@Transactional(rollbackFor = Throwable.class)
public ResponseDTO<Void> forceResetMfa(Long userId, Long reviewerUserId) {
    // Step 1: Delete old MFA records
    mfaRepository.deleteByUserId(userId);

    // Step 2: Generate new TOTP Secret
    String newSecret = TotpSecretGenerator.generateSecret();
    String qrCode = QrCodeGenerator.generateQrCode(
        getUserEmail(userId),
        newSecret,
        "SmartAdmin iGaming"
    );

    // Step 3: Send email to user
    emailService.sendMfaResetEmail(userId, qrCode, newSecret);

    // Step 4: Record audit log (CRITICAL level)
    auditLogService.log(userId, "MFA_FORCE_RESET", String.format(
        "Security Team force reset MFA (reviewer: %d)", reviewerUserId
    ));

    return ResponseDTO.ok();
}
```

---

## 5. Emergency Contact Database Schema

```sql
-- t_mfa_emergency_contacts table
CREATE TABLE t_mfa_emergency_contacts (
    user_id             BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_user_id     BIGINT NOT NULL REFERENCES t_admin_user(user_id),
    contact_type        VARCHAR(20) NOT NULL,   -- COLLEAGUE / MANAGER
    verified            BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, contact_user_id)
);
```

### 5.1 Verification Email Template

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

## 6. Role-Based MFA Policy Implementation

### 6.1 MFA Policy Service

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

### 6.2 First Login Mandatory Setup Flow

```mermaid
graph TD
    A[User Login] --> B{Password Verification}
    B -->|PASS| C{Check Role}

    C -->|High-Risk Role| D{MFA Enabled?}
    C -->|Low-Risk Role| G[Direct Login]

    D -->|Yes| E[Require MFA Verification]
    D -->|No| F[Force Redirect to MFA Setup Page<br/>Cannot Skip]

    F --> H[Scan QR Code]
    H --> I[Verify Activation]
    I --> E

    E --> J{TOTP Verification}
    J -->|PASS| G
    J -->|FAIL| K[Re-enter Code]
```

### 6.3 Optional MFA Recommendation (Low-Risk Roles)

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

### 6.4 Frontend MFA Recommendation Banner

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

### 6.5 Decision Framework Diagram

```mermaid
graph TD
    A[Role MFA Strategy] --> B{Dimension 1: Risk Exposure}
    A --> C{Dimension 2: Operation Reversibility}
    A --> D{Dimension 3: Compliance Requirements}

    B --> B1[Super Admin: 5/5<br/>Can modify system config]
    B --> B2[Finance: 5/5<br/>Can adjust player balances]
    B --> B3[Customer Service: 3/5<br/>Query-only permissions]

    C --> C1[Super Admin: NO<br/>Irreversible]
    C --> C2[Finance: NO<br/>Irreversible]
    C --> C3[Customer Service: YES<br/>Auditable]

    D --> D1[Super Admin: REQUIRED<br/>PCI DSS mandate]
    D --> D2[Finance: REQUIRED<br/>PCI DSS mandate]
    D --> D3[Customer Service: RECOMMENDED<br/>GDPR suggestion]
```

---

## 7. Audit Log Database Schema

### 7.1 MFA Audit Log Table

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

### 7.2 Audit Log Entry Example

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

## 8. Automated Anomaly Detection Queries

### 8.1 Rule 1: Multiple MFA Failures in Short Period

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

**Triggered actions**:
- Lock account for 15 minutes
- Send alert to Security Team
- Send email notification to user

### 8.2 Rule 2: Abnormal Geographic Location Login

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

**Triggered actions**:
- Alert Security Team for manual review
- Disable trusted device, require full MFA on next login

### 8.3 Rule 3: Frequent Backup Code Usage

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

**Triggered actions**:
- Warn user to reconfigure TOTP
- Prompt user about possible device loss

### 8.4 Rule 4: MFA Disabled for High-Risk Role

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

**Triggered actions**:
- Immediately alert CTO / CISO
- Manual review required
- If unauthorized, lock account as security incident

---

## 9. Integration Test Examples

```java
@SpringBootTest
@AutoConfigureMockMvc
class MfaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MfaRepository mfaRepository;

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

## 10. Related Documents

- [06-06-01 MFA Architecture Design](../../source-archive/06_Platform_Governance/06-06-01_MFA_Architecture.md) - Business requirements and method selection
- [06-06-02 TOTP & WebAuthn Implementation](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md) - TOTP algorithm details
- [06-06-03 Login & Recovery Flow](../../source-archive/06_Platform_Governance/06-06-03_Recovery_Flow.md) - Authentication flow design
