# 資料安全標準架構

> **業務需求**: [Data Protection Requirements](../../requirements/12_Security_Compliance/Data_Protection_Requirements.md)
> **規範來源**: [source-archive/12_System_Security/12-03](../../source-archive/12_System_Security/12-03_Data_Security_Standard.md)
> **文件類型**: 技術架構
> **目標讀者**: 架構師、安全工程師、後端開發人員

---

## 1. 加密架構總覽

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

## 2. PII 分類矩陣

| PII 欄位 | 範例 | 風險等級 | 加密方式 |
|-----------|---------|-----------|------------|
| **真實姓名** | "John Doe" | 高 | AES-256-GCM |
| **電話** | "+886912345678" | 極高 | AES-256-GCM + Blind Index |
| **電子郵件** | "player@example.com" | 極高 | AES-256-GCM + Blind Index |
| **銀行帳號** | "1234567890" | 極高 | AES-256-GCM + Blind Index |
| **身分證號** | "A123456789" | 極高 | AES-256-GCM + Blind Index |
| **密碼** | "P@ssw0rd123" | 極高 | **Argon2id Hash**（不可逆） |
| **地址** | "Taipei..." | 中 | AES-256-GCM |
| **IP 位址** | "1.2.3.4" | 中 | HMAC-SHA256 (Blind Index) |

## 3. 儲存加密格式

```text
[Version]:[IV]:[Ciphertext]:[AuthTag]

Example:
v1:a3f8d9e2c1b4:Y3J5cHRvZ3JhcGh5==:4a7b8c9d
```

| 欄位 | 長度 | 說明 |
|-------|--------|-------------|
| Version | 2 bytes | 加密版本（用於金鑰輪換） |
| IV | 12 bytes | 每次加密隨機產生 |
| Ciphertext | 可變 | AES-GCM 加密資料 |
| AuthTag | 16 bytes | GCM 驗證標籤 |

## 4. Blind Index 查詢流程

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

## 5. 密碼學銷毀（GDPR 刪除）

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

## 6. 傳輸安全

```nginx
server {
    listen 443 ssl http2;
    ssl_protocols TLSv1.3 TLSv1.2;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
}
```

## 7. 資料遮罩規則

| PII 欄位 | 遮罩規則 | 範例 |
|-----------|-------------|---------|
| **姓名** | 保留首末字元 | `David Beckham` -> `D***m` |
| **電話** | 保留前 3 後 3 | `0912345678` -> `091****678` |
| **電子郵件** | 保留前 2 + 域名 | `david@gmail.com` -> `da***@gmail.com` |
| **銀行帳號** | 保留末 4 碼 | `1234567890` -> `******7890` |

### 角色層級遮罩

| 角色 | 電話 | 電子郵件 | 銀行帳號 |
|------|-------|-------|-------------|
| **玩家（本人）** | 完整（需 2FA） | 完整 | 末 4 碼 |
| **客服 Level 1** | `091****678` | `da***@gmail.com` | 無權限 |
| **風控** | 完整（需審批） | 完整（需審批） | 完整（需審批） |
| **DBA** | 密文（無法解密） | 密文 | 密文 |

## 8. 合規檢查清單

### GDPR

| 要求 | 狀態 | 實作方式 |
|-------------|--------|---------------|
| 資料最小化 | 合規 | 僅收集必要 PII |
| 儲存加密 | 合規 | AES-256-GCM |
| 傳輸加密 | 合規 | TLS 1.3 |
| 被遺忘權 | 合規 | Crypto-Shredding |
| 資料可攜性 | 合規 | JSON 匯出 |
| 稽核日誌 | 合規 | 所有 PII 存取皆記錄 |

### PCI-DSS

| 要求 | 狀態 | 實作方式 |
|-------------|--------|---------------|
| 禁止儲存完整卡號 | 合規 | 僅存 PSP Token |
| 銀行帳號加密 | 合規 | AES-256-GCM + Blind Index |
| 密碼雜湊 | 合規 | Argon2id |
| 存取控制 | 合規 | RBAC + IP 白名單 |

## 9. 資料分類服務

```mermaid
flowchart TD
    A[Incoming Data Field] --> B{Classify Field}
    B -->|Critical| C[AES-256-GCM + Blind Index<br/>Bank Account, ID Number,<br/>Email, Phone]
    B -->|High| D[AES-256-GCM Only<br/>Real Name, Date of Birth]
    B -->|Medium| E[HMAC-SHA256 Pseudonymization<br/>IP Address, Device ID]
    B -->|Low| F[No Encryption<br/>Preferred Language, Timezone]

    C --> G[Encrypted Storage<br/>encrypted_* columns]
    D --> G
    E --> H[Pseudonymized Storage<br/>*_hash columns]
    F --> I[Plaintext Storage]

    G --> J[Access Audit Log]
    H --> J
    I --> J

    style C fill:#FF5252,color:#fff
    style D fill:#FF9800,color:#fff
    style E fill:#FFC107
    style F fill:#4CAF50,color:#fff
```

```java
@Service
@RequiredArgsConstructor
public class DataClassificationService {

    private final EncryptionManager encryptionManager;
    private final AuditLogDao auditLogDao;

    /**
     * Classification levels aligned with ISO 27001 Annex A.8
     */
    public enum Classification {
        CRITICAL,  // PCI-DSS scope: bank accounts, card tokens, ID numbers
        HIGH,      // PII requiring encryption: real name, DOB
        MEDIUM,    // Pseudonymizable: IP address, device fingerprint
        LOW        // Non-sensitive: language preference, timezone
    }

    private static final Map<String, Classification> FIELD_CLASSIFICATION = Map.ofEntries(
        Map.entry("bank_account", Classification.CRITICAL),
        Map.entry("id_number", Classification.CRITICAL),
        Map.entry("email", Classification.CRITICAL),
        Map.entry("phone", Classification.CRITICAL),
        Map.entry("real_name", Classification.HIGH),
        Map.entry("date_of_birth", Classification.HIGH),
        Map.entry("ip_address", Classification.MEDIUM),
        Map.entry("device_id", Classification.MEDIUM),
        Map.entry("language", Classification.LOW),
        Map.entry("timezone", Classification.LOW)
    );

    public Classification classifyField(String fieldName) {
        return FIELD_CLASSIFICATION.getOrDefault(fieldName, Classification.HIGH);
    }
}
```

## 10. 欄位層級加密模式

```java
@Component
@RequiredArgsConstructor
public class FieldEncryptionHandler implements TypeHandler<String> {

    private final KeyManagementManager keyManager;

    /**
     * Encrypt sensitive field before database write.
     * Format: v1:{iv}:{ciphertext}:{authTag}
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     String value, JdbcType jdbcType) throws SQLException {
        DataEncryptionKey dek = keyManager.getCurrentDEK();
        byte[] iv = SecureRandom.getInstanceStrong().generateSeed(12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, dek.getSecretKey(), spec);

        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String encoded = String.format("v1:%s:%s:%s",
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(Arrays.copyOf(ciphertext, ciphertext.length - 16)),
            Base64.getEncoder().encodeToString(Arrays.copyOfRange(ciphertext, ciphertext.length - 16, ciphertext.length))
        );
        ps.setString(i, encoded);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        if (encrypted == null || !encrypted.startsWith("v1:")) {
            return encrypted;
        }
        return decrypt(encrypted);
    }

    private String decrypt(String encoded) {
        String[] parts = encoded.split(":");
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
        byte[] authTag = Base64.getDecoder().decode(parts[3]);

        DataEncryptionKey dek = keyManager.getDEKForVersion(parts[0]);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, dek.getSecretKey(), new GCMParameterSpec(128, iv));

        byte[] combined = new byte[ciphertext.length + authTag.length];
        System.arraycopy(ciphertext, 0, combined, 0, ciphertext.length);
        System.arraycopy(authTag, 0, combined, ciphertext.length, authTag.length);

        return new String(cipher.doFinal(combined), StandardCharsets.UTF_8);
    }
}
```

## 11. 敏感資料存取控制矩陣

| 資料類別 | 玩家（本人） | 客服 Level 1 | 客服 Level 2 | 風控 | 財務 | DBA | 系統管理員 |
|---------------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **銀行帳號** | 末 4 碼 | 無權限 | 末 4 碼 | 完整（需審批） | 完整（需審批） | 密文 | 無權限 |
| **身分證號** | 遮罩 | 無權限 | 末 4 碼 | 完整（需審批） | 無權限 | 密文 | 無權限 |
| **電話** | 完整（2FA） | 遮罩 | 完整（需審批） | 完整（需審批） | 無權限 | 密文 | 無權限 |
| **電子郵件** | 完整 | 遮罩 | 完整 | 完整 | 無權限 | 密文 | 無權限 |
| **真實姓名** | 完整 | 遮罩 | 完整 | 完整 | 完整 | 密文 | 無權限 |
| **IP 位址** | 無權限 | 無權限 | 雜湊 | 完整 | 無權限 | 雜湊 | 雜湊 |
| **交易紀錄** | 僅本人 | 唯讀（遮罩） | 唯讀 | 完整 | 完整 | 無權限 | 無權限 |

### 透過註解強制存取控制

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDataAccess {
    DataClassificationService.Classification level();
    boolean requiresApproval() default false;
    boolean auditLog() default true;
}

// Usage in Controller
@GetMapping("/player/{id}/bank-account")
@RequiresDataAccess(level = Classification.CRITICAL, requiresApproval = true)
@SaCheckPermission("player:pii:bank-account")
public ResponseDTO<BankAccountVO> getBankAccount(@PathVariable Long id) {
    return ResponseDTO.ok(playerService.getBankAccount(id));
}
```

---

<!-- End of Document -->
