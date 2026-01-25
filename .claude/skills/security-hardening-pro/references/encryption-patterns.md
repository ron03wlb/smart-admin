# Encryption Patterns

Complete guide for implementing SM2/SM3/SM4 encryption and AES encryption in SmartAdmin.

---

## SM4 Encryption (Chinese National Standard)

### Overview
- **Algorithm**: SM4 (block cipher, 128-bit key)
- **Standard**: GB/T 32907-2016 (Chinese national cryptographic algorithm)
- **Use Cases**: Comply with PIPL (Personal Information Protection Law), encrypt sensitive data in China market

### Key Configuration

```java
// SM4 requires 16-byte (128-bit) key
// Key composition: Letters, numbers, special characters (16 chars total)

public class EncryptionConfig {
    // Development environment (NEVER use in production)
    private static final String SM4_KEY_DEV = "1024lab__1024lab";

    // Production: Load from AWS Secrets Manager
    private final String sm4KeyProd;

    @PostConstruct
    public void init() {
        sm4KeyProd = secretsManager.getSecret("igaming/prod/sm4-encryption-key");
    }
}
```

### API Request/Response Encryption

**Step 1: Enable in application.yml**

```yaml
# application.yml
api-encrypt:
  enabled: true
  type: SM4  # Options: SM4, AES
  key: ${SM4_ENCRYPTION_KEY}  # Load from environment variable
```

**Step 2: Add annotations to controller**

```java
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @ApiEncrypt  // Encrypt response
    @ApiDecrypt  // Decrypt request
    @PostMapping("/process")
    public ResponseDTO<PaymentResult> processPayment(@RequestBody PaymentForm form) {
        // Form data is automatically decrypted from request
        // Response data is automatically encrypted before sending
        return ResponseDTO.ok(paymentService.process(form));
    }
}
```

**Step 3: Frontend integration (Vue.js)**

```javascript
// Install sm-crypto library
// npm install --save sm-crypto

import { sm4 } from 'sm-crypto';

// Encrypt request payload
function encryptRequest(data) {
  const key = '1024lab__1024lab';  // Same key as backend
  const encrypted = sm4.encrypt(JSON.stringify(data), key);
  return btoa(encrypted);  // Base64 encode
}

// Decrypt response payload
function decryptResponse(encryptedData) {
  const key = '1024lab__1024lab';
  const base64Decoded = atob(encryptedData);
  const decrypted = sm4.decrypt(base64Decoded, key);
  return JSON.parse(decrypted);
}

// Usage with Axios
const response = await axios.post('/api/payment/process', {
  data: encryptRequest(paymentForm)
});
const result = decryptResponse(response.data.data);
```

**Step 4: Verify encryption**

```bash
# Test with curl
curl -X POST https://api.example.com/api/payment/process \
  -H "Content-Type: application/json" \
  -d '{"data":"YmFzZTY0X2VuY3J5cHRlZF9kYXRh..."}'

# Response will be encrypted:
{
  "code": 200,
  "data": "ZW5jcnlwdGVkX3Jlc3BvbnNlX2RhdGE..."
}
```

---

## Field-Level Encryption (Database Columns)

### Use Cases
- Encrypt SSN, passport numbers, bank accounts in database
- Automatic encryption/decryption with MyBatis type handler

### MyBatis Type Handler

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis type handler for automatic encryption/decryption
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    private final EncryptionService encryptionService;

    public EncryptedStringTypeHandler() {
        this.encryptionService = SpringContextHolder.getBean(EncryptionService.class);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            String encrypted = encryptionService.encrypt(parameter);
            ps.setString(i, encrypted);
        } catch (Exception e) {
            throw new SQLException("Failed to encrypt value", e);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return decrypt(encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return decrypt(encrypted);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return decrypt(encrypted);
    }

    private String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }

        try {
            return encryptionService.decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }
}
```

### Entity Configuration

```java
@Data
@TableName(value = "players", autoResultMap = true)  // IMPORTANT: autoResultMap = true
public class Player {
    private Long id;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String ssn;  // Automatically encrypted/decrypted

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String bankAccount;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String passportNumber;
}
```

### Usage

```java
// Insert (automatic encryption)
Player player = new Player();
player.setSsn("123-45-6789");  // Plain text
playerDao.insert(player);
// Database stores: "ZW5jcnlwdGVkX3Nzbl92YWx1ZQ==" (encrypted)

// Select (automatic decryption)
Player result = playerDao.selectById(playerId);
System.out.println(result.getSsn());  // "123-45-6789" (decrypted)
```

---

## AES-256-GCM Encryption

### Overview
- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Size**: 256-bit
- **Features**: Authenticated encryption (integrity + confidentiality)

### Implementation

```java
package net.lab1024.sa.base.module.support.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretsManagerClient secretsManager;

    /**
     * Encrypt sensitive data (e.g., KYC documents, payment details)
     * Uses AES-256-GCM (authenticated encryption)
     */
    public String encrypt(String plaintext) throws Exception {
        // 1. Get encryption key from AWS Secrets Manager
        SecretKey key = loadEncryptionKey();

        // 2. Generate random IV (nonce)
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 3. Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        // 4. Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 5. Combine IV + ciphertext for storage
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt sensitive data
     */
    public String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // 1. Extract IV and ciphertext
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

        // 2. Get decryption key
        SecretKey key = loadEncryptionKey();

        // 3. Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        // 4. Decrypt
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private SecretKey loadEncryptionKey() {
        // Load AES-256 key from AWS Secrets Manager
        GetSecretValueResponse response = secretsManager.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("igaming/encryption-key")
                .build()
        );

        byte[] keyBytes = Base64.getDecoder().decode(response.secretString());
        return new SecretKeySpec(keyBytes, "AES");
    }
}
```

---

## Key Management

### AWS Secrets Manager Integration

```java
@Service
@RequiredArgsConstructor
public class SecretsService {

    private final SecretsManagerClient secretsManager;
    private final RedissonClient redisson;

    /**
     * Get secret from AWS Secrets Manager (with caching)
     */
    public String getSecret(String secretName) {
        // 1. Check cache first
        String cacheKey = "secret:" + secretName;
        RBucket<String> bucket = redisson.getBucket(cacheKey);
        String cached = bucket.get();

        if (cached != null) {
            return cached;
        }

        // 2. Fetch from AWS Secrets Manager
        GetSecretValueResponse response = secretsManager.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()
        );

        String secretValue = response.secretString();

        // 3. Cache for 5 minutes
        bucket.set(secretValue, 5, TimeUnit.MINUTES);

        return secretValue;
    }

    /**
     * Rotate secret (part of automatic rotation lambda)
     */
    public void rotateSecret(String secretName, String newSecretValue) {
        // 1. Create new version
        PutSecretValueResponse response = secretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(newSecretValue)
                .build()
        );

        // 2. Invalidate cache
        redisson.getKeys().delete("secret:" + secretName);

        log.info("Secret rotated: name={}, version={}", secretName, response.versionId());
    }
}
```

### Environment-Specific Configuration

```yaml
# application-prod.yml
aws:
  secrets:
    database-password: "igaming/prod/database-password"
    encryption-key: "igaming/prod/encryption-key"
    sm4-encryption-key: "igaming/prod/sm4-encryption-key"
    jwt-secret: "igaming/prod/jwt-secret"
    payment-api-key: "igaming/prod/payment-api-key"

# NEVER commit actual secrets to Git!
# Use AWS Secrets Manager or environment variables
```

---

## TLS 1.3 Configuration

### Spring Boot Configuration

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: igaming
    protocol: TLS
    enabled-protocols: TLSv1.3  # Only TLS 1.3
    ciphers: TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
```

### Generate Keystore

```bash
# Generate self-signed certificate (development)
keytool -genkeypair \
  -alias igaming \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 3650 \
  -storepass changeit

# Production: Use Let's Encrypt or commercial CA
certbot certonly --standalone -d api.igaming.example.com
```

---

## PostgreSQL pgcrypto Extension

### Enable Extension

```sql
-- Enable pgcrypto extension for column-level encryption
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### Encrypt/Decrypt Columns

```sql
-- Encrypt SSN (Social Security Number)
INSERT INTO players (ssn_encrypted)
VALUES (pgp_sym_encrypt('123-45-6789', current_setting('app.encryption_key')));

-- Decrypt when reading
SELECT pgp_sym_decrypt(ssn_encrypted::bytea, current_setting('app.encryption_key'))
FROM players WHERE id = 123;

-- Transparent Data Encryption (TDE) setup
ALTER TABLE players ADD COLUMN ssn_encrypted BYTEA;

-- Migrate existing data
UPDATE players
SET ssn_encrypted = pgp_sym_encrypt(ssn, current_setting('app.encryption_key'));

-- Drop plain text column
ALTER TABLE players DROP COLUMN ssn;
```

---

## Best Practices

### 1. Key Rotation Schedule
- **Development**: No rotation (use static key)
- **Staging**: Rotate every 180 days
- **Production**: Rotate every 90 days

### 2. Key Storage
- NEVER hardcode keys in source code
- NEVER commit keys to Git
- Use AWS Secrets Manager or HashiCorp Vault
- Enable automatic key rotation

### 3. Encryption Key Length
- **SM4**: 128-bit (16 bytes)
- **AES**: 256-bit (32 bytes)
- **RSA**: 2048-bit minimum (4096-bit recommended)

### 4. IV (Initialization Vector)
- Generate random IV for each encryption
- Store IV with ciphertext (prepend)
- NEVER reuse IV with same key

### 5. Authenticated Encryption
- Use AES-GCM (provides integrity + confidentiality)
- Verify authentication tag before decryption
- Reject tampered ciphertext

---

## Security Checklist

- [ ] Encryption keys stored in AWS Secrets Manager
- [ ] Key rotation schedule documented (90 days)
- [ ] TLS 1.3 enabled for transport layer
- [ ] Database backups encrypted (S3 SSE-KMS)
- [ ] Column-level encryption for PII (SSN, passport)
- [ ] Random IV generated for each encryption
- [ ] Authenticated encryption used (AES-GCM)
- [ ] Key access logged and audited

---

## SmartAdmin Foundation Module

**Location**: `sa-base/foundation/api-encrypt/`

**Key Classes:**
- `ApiEncryptService` - Encryption interface
- `ApiEncryptServiceSmImpl` - SM4 implementation
- `ApiEncryptServiceAesImpl` - AES implementation
- `@ApiEncrypt` - Annotation for response encryption
- `@ApiDecrypt` - Annotation for request decryption

**Usage:**
```java
@ApiEncrypt  // Encrypt response
@ApiDecrypt  // Decrypt request
@PostMapping("/secure-endpoint")
public ResponseDTO<SecureData> secureOperation(@RequestBody SecureForm form) {
    return ResponseDTO.ok(service.process(form));
}
```
