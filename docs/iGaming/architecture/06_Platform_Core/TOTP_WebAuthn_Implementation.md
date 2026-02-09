# TOTP 與 WebAuthn 技術實作

> **Canonical Source**: [06-06-02_TOTP_WebAuthn.md](../../source-archive/06_Platform_Governance/06-06-02_TOTP_WebAuthn.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: None (pure technical)
> **Last Synced**: 2026-02-08

---

## 1. TOTP 實施原理

### 1.1 RFC 6238 算法

**TOTP（Time-based One-Time Password）** 基於 [RFC 6238](https://tools.ietf.org/html/rfc6238) 標準。

**核心算法**：

```
TOTP = HOTP(K, T)

其中：
- K = 共享密鑰（Shared Secret，Base32 編碼）
- T = Floor(Current Unix Time / Time Step)
- Time Step = 30 秒（標準值）
- HOTP = HMAC-based One-Time Password（RFC 4226）
```

**HMAC-SHA1 計算過程**（Python 參考實現）：

```python
import hmac
import hashlib
import time
import base64

def generate_totp(secret_key: str, time_step: int = 30) -> str:
    """
    生成 6 位數 TOTP 驗證碼

    Args:
        secret_key: Base32 編碼的共享密鑰（例：JBSWY3DPEHPK3PXP）
        time_step: 時間步長（秒），預設 30 秒

    Returns:
        6 位數驗證碼（例：123456）
    """
    # Step 1: 計算時間計數器（T）
    current_time = int(time.time())
    time_counter = current_time // time_step

    # Step 2: 將計數器轉換為 8 字節大端序（Big-Endian）
    time_bytes = time_counter.to_bytes(8, byteorder='big')

    # Step 3: 解碼 Base32 密鑰
    key_bytes = base64.b32decode(secret_key)

    # Step 4: 計算 HMAC-SHA1
    hmac_hash = hmac.new(key_bytes, time_bytes, hashlib.sha1).digest()

    # Step 5: 動態截斷（Dynamic Truncation）
    offset = hmac_hash[-1] & 0x0F
    truncated_hash = hmac_hash[offset:offset+4]

    # Step 6: 轉換為整數並取模
    code = int.from_bytes(truncated_hash, byteorder='big') & 0x7FFFFFFF
    otp = code % 1000000  # 6 位數

    return f"{otp:06d}"  # 補齊前導零


# 範例使用
secret = "JBSWY3DPEHPK3PXP"  # Google Authenticator 測試密鑰
totp_code = generate_totp(secret)
print(f"當前 TOTP 驗證碼：{totp_code}")
```

**算法安全性分析**：

| 特性 | 說明 | 安全性評估 |
|------|------|-----------|
| **單向性** | HMAC-SHA1 不可逆，無法從驗證碼推導密鑰 | 5/5 |
| **時間敏感** | 每 30 秒更換一次驗證碼 | 5/5 |
| **抗暴力破解** | 6 位數（100 萬種可能）x 30 秒窗口 = 極低成功率 | 4/5 |
| **離線驗證** | 服務器和客戶端獨立計算，無需網絡 | 5/5 |

---

### 1.2 密鑰生成與共享

**密鑰生成實現**（Java）：

```java
import java.security.SecureRandom;
import java.util.Base64;

public class TotpSecretGenerator {

    /**
     * 生成 TOTP 共享密鑰（Base32 編碼）
     *
     * @return 20 字節隨機密鑰（例：JBSWY3DPEHPK3PXP）
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];  // 160 bits（RFC 6238 推薦）
        random.nextBytes(bytes);

        // 使用 Base32 編碼（A-Z 和 2-7，共 32 個字符）
        return Base32.encode(bytes);
    }

    /**
     * 生成 QR Code URL（用於 Google Authenticator 掃描）
     *
     * @param username 用戶名稱
     * @param secret Base32 編碼的密鑰
     * @param issuer 發行者（顯示在 App 中的名稱）
     * @return otpauth:// 協議 URL
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

// 範例使用
String secret = TotpSecretGenerator.generateSecret();
// 輸出：JBSWY3DPEHPK3PXP

String qrUrl = TotpSecretGenerator.generateQrCodeUrl(
    "admin@smartadmin.com",
    secret,
    "SmartAdmin iGaming"
);
// 輸出：otpauth://totp/SmartAdmin%20iGaming:admin@smartadmin.com?secret=JBSWY3DPEHPK3PXP&issuer=SmartAdmin%20iGaming&algorithm=SHA1&digits=6&period=30
```

**QR Code 顯示範例**（Google Authenticator 掃描後）：

```
SmartAdmin iGaming
admin@smartadmin.com
123 456
```

---

### 1.3 密鑰存儲設計

**資料庫 Schema**（PostgreSQL）：

```sql
-- t_admin_user_mfa 表結構
CREATE TABLE t_admin_user_mfa (
    user_id             BIGINT PRIMARY KEY REFERENCES t_admin_user(user_id),
    mfa_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    totp_secret         VARCHAR(255) NOT NULL,  -- AES-256-GCM 加密後的 Secret
    backup_codes        TEXT,                    -- JSON 數組（10 個備份碼）
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);
```

**密鑰加密實現**（Java AES-256-GCM）：

```java
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TotpSecretEncryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * 使用 AES-256-GCM 加密 TOTP Secret
     *
     * @param plainSecret 明文密鑰（例：JBSWY3DPEHPK3PXP）
     * @param masterKey 主密鑰（從 KMS 獲取，例：AWS KMS / HashiCorp Vault）
     * @return Base64 編碼的加密密文
     */
    public static String encryptSecret(String plainSecret, SecretKey masterKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey);

        byte[] iv = cipher.getIV();  // 初始化向量（IV）
        byte[] ciphertext = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));

        // 格式：IV + Ciphertext（Base64 編碼）
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 解密 TOTP Secret
     */
    public static String decryptSecret(String encryptedSecret, SecretKey masterKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedSecret);

        // 提取 IV（前 12 字節）
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);

        // 提取密文
        byte[] ciphertext = new byte[combined.length - 12];
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
```

**密鑰管理最佳實踐**：

| 階段 | 最佳實踐 | SmartAdmin 實施方案 |
|------|---------|-------------------|
| **生成** | 使用 `SecureRandom`（不可用 `Random`） | `SecureRandom` + 160 bits 熵 |
| **傳輸** | 通過 QR Code 傳輸（HTTPS + 一次性顯示） | QR Code 生成後僅顯示 1 次 |
| **存儲** | AES-256-GCM 加密 + KMS 管理主密鑰 | 集成 AWS KMS / Vault |
| **使用** | 解密後立即使用，不留內存痕跡 | `try-finally` 清除變量 |
| **銷毀** | 用戶禁用 MFA 時刪除 Secret | `CASCADE DELETE` |

---

## 2. 時間同步處理

### 2.1 問題描述

服務器時間與用戶設備時間可能不同步（時差 +/- 5 分鐘）。

**解決方案**：允許 +/- 1 個時間窗口（即驗證前後各 30 秒的驗證碼）。

### 2.2 驗證邏輯實現

```java
import java.time.Instant;

public class TotpValidator {
    private static final int TIME_STEP = 30;  // 30 秒
    private static final int WINDOW = 1;      // 允許 ±1 個窗口

    /**
     * 驗證 TOTP 驗證碼（允許時間偏移）
     *
     * @param userCode 用戶輸入的 6 位數驗證碼
     * @param secret Base32 編碼的共享密鑰
     * @return true 驗證成功，false 驗證失敗
     */
    public static boolean validate(String userCode, String secret) {
        long currentTime = Instant.now().getEpochSecond();

        // 檢查當前時間窗口 + 前後各 1 個窗口（共 3 個窗口）
        for (int i = -WINDOW; i <= WINDOW; i++) {
            long timeCounter = (currentTime / TIME_STEP) + i;
            String generatedCode = generateTotp(secret, timeCounter);

            if (userCode.equals(generatedCode)) {
                return true;
            }
        }

        return false;  // 所有窗口都不匹配
    }

    private static String generateTotp(String secret, long timeCounter) {
        // 使用 Section 1.1 的 HMAC-SHA1 算法
        // ...（省略實現細節）
    }
}
```

### 2.3 時間同步測試矩陣

| 服務器時間 | 用戶設備時間 | 時間差 | 驗證結果 | 說明 |
|-----------|------------|-------|---------|------|
| 10:00:00 | 10:00:00 | 0s | 成功 | 完全同步 |
| 10:00:00 | 10:00:25 | +25s | 成功 | 仍在當前窗口內 |
| 10:00:00 | 10:00:35 | +35s | 成功 | 進入下一個窗口（允許 +1） |
| 10:00:00 | 10:01:05 | +65s | 失敗 | 超出 +/- 1 窗口範圍 |
| 10:00:00 | 09:59:25 | -35s | 成功 | 前一個窗口（允許 -1） |

### 2.4 NTP 時間同步配置

```bash
# 服務器端配置 NTP 自動同步
sudo apt-get install ntp
sudo systemctl enable ntp
sudo systemctl start ntp

# 驗證時間同步狀態
ntpq -p

# 輸出範例（Stratum 2 表示與權威時間源僅差 2 跳）
     remote           refid      st t when poll reach   delay   offset  jitter
==============================================================================
*time.google.com .GOOG.           1 u   64   64  377    12.345  +0.123   0.456
```

---

## 相關文檔

- [MFA_Technical_Architecture.md](./MFA_Technical_Architecture.md) - MFA 系統架構設計
- [MFA_Compliance_Validation.md](./MFA_Compliance_Validation.md) - 合規驗證技術設計
- [MFA_Recovery_Implementation.md](./MFA_Recovery_Implementation.md) - 恢復流程技術實現

---

**End of Document**
