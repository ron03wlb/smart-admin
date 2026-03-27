# 範例 1: 使用 SM3 加密用戶密碼

**技能**: security-hardening-pro
**難度**: ⭐⭐⭐☆☆（中高等）
**預估時間**: 20-30 分鐘

---

## 場景描述

實現符合中國密碼法規範的用戶密碼加密，使用國密算法 SM3（類似 SHA-256 但符合國家標準）。

**需求**:
- 用戶註冊時使用 SM3 加密密碼
- 登入時驗證 SM3 加密密碼
- 支援加鹽（salt）防止彩虹表攻擊
- 符合《中華人民共和國密碼法》要求

---

## 問題範例

### ❌ 不安全代碼（使用簡單 MD5）

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    public void registerUser(UserRegisterForm form) {
        // ❌ MD5 已被破解，不安全
        String encryptedPassword = DigestUtils.md5Hex(form.getPassword());

        UserEntity user = new UserEntity();
        user.setUsername(form.getUsername());
        user.setPassword(encryptedPassword); // ❌ 無加鹽
        userDao.insert(user);
    }

    public boolean login(String username, String password) {
        UserEntity user = userDao.selectByUsername(username);
        // ❌ 直接比較 MD5，容易被彩虹表攻擊
        return DigestUtils.md5Hex(password).equals(user.getPassword());
    }
}
```

**問題**:
1. MD5 演算法已被破解（2004 年）
2. 無加鹽（salt），容易被彩虹表攻擊
3. 不符合中國密碼法規範

---

## 解決方案

### 1. 引入國密依賴

**build.gradle.kts**:
```kotlin
dependencies {
    // 國密算法庫
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}
```

---

### 2. 實現 SM3 加密工具類

**SM3Util.java**:
```java
package net.lab1024.sa.foundation.security.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;

/**
 * SM3 加密工具類
 * 符合《GM/T 0004-2012 SM3密碼雜湊演算法》標準
 */
public class SM3Util {

    static {
        // 註冊 BouncyCastle 提供者
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final int SALT_LENGTH = 32; // 鹽長度 (bytes)

    /**
     * 生成隨機鹽
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Hex.toHexString(salt);
    }

    /**
     * SM3 加密（帶鹽）
     *
     * @param plaintext 明文
     * @param salt 鹽值（16進制字符串）
     * @return SM3(plaintext + salt) 的 16進制字符串
     */
    public static String encrypt(String plaintext, String salt) {
        SM3Digest digest = new SM3Digest();

        // plaintext + salt
        byte[] combined = (plaintext + salt).getBytes(StandardCharsets.UTF_8);
        digest.update(combined, 0, combined.length);

        // 計算 SM3 hash
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);

        return Hex.toHexString(hash);
    }

    /**
     * 驗證 SM3 加密結果
     */
    public static boolean verify(String plaintext, String salt, String encryptedText) {
        String computed = encrypt(plaintext, salt);
        return computed.equals(encryptedText);
    }
}
```

---

### 3. 更新用戶實體

**UserEntity.java**:
```java
@Data
@TableName("t_user")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    /**
     * SM3 加密後的密碼
     * 格式: SM3(plaintext + salt)
     */
    private String password;

    /**
     * 密碼鹽值（16進制字符串）
     * 每個用戶唯一，防止彩虹表攻擊
     */
    private String passwordSalt;

    // ... 其他欄位
}
```

---

### 4. 實現安全的用戶服務

**UserService.java**:
```java
package net.lab1024.sa.business.user.service;

import net.lab1024.sa.foundation.security.crypto.SM3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;

    /**
     * 註冊用戶（使用 SM3 加密密碼）
     */
    public ResponseDTO<Void> registerUser(UserRegisterForm form) {
        // 1. 生成隨機鹽
        String salt = SM3Util.generateSalt();

        // 2. SM3 加密密碼
        String encryptedPassword = SM3Util.encrypt(form.getPassword(), salt);

        // 3. 保存用戶
        UserEntity user = new UserEntity();
        user.setUsername(form.getUsername());
        user.setPassword(encryptedPassword);     // ✅ SM3 加密
        user.setPasswordSalt(salt);              // ✅ 保存鹽值

        userDao.insert(user);
        return ResponseDTO.ok();
    }

    /**
     * 登入驗證（驗證 SM3 密碼）
     */
    public ResponseDTO<LoginVO> login(UserLoginForm form) {
        // 1. 查詢用戶
        UserEntity user = userDao.selectByUsername(form.getUsername());
        if (user == null) {
            return ResponseDTO.error(UserErrorCode.LOGIN_FAIL);
        }

        // 2. 驗證 SM3 密碼
        boolean passwordMatch = SM3Util.verify(
            form.getPassword(),       // 用戶輸入的明文密碼
            user.getPasswordSalt(),   // 數據庫存儲的鹽
            user.getPassword()        // 數據庫存儲的 SM3 hash
        );

        if (!passwordMatch) {
            return ResponseDTO.error(UserErrorCode.LOGIN_FAIL);
        }

        // 3. 生成 Token（使用 Sa-Token）
        StpUtil.login(user.getUserId());
        String token = StpUtil.getTokenValue();

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(SmartBeanUtil.copy(user, UserVO.class));

        return ResponseDTO.ok(loginVO);
    }
}
```

---

## 測試驗證

### 單元測試

**UserServiceTest.java**:
```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testRegisterWithSM3() {
        // 註冊用戶
        UserRegisterForm form = new UserRegisterForm();
        form.setUsername("testuser");
        form.setPassword("SecureP@ss123");

        ResponseDTO<Void> response = userService.registerUser(form);
        Assertions.assertTrue(response.getOk());

        // 驗證密碼已加密
        UserEntity user = userDao.selectByUsername("testuser");
        Assertions.assertNotEquals("SecureP@ss123", user.getPassword()); // ✅ 密碼已加密
        Assertions.assertNotNull(user.getPasswordSalt()); // ✅ 鹽值已生成
        Assertions.assertEquals(64, user.getPassword().length()); // ✅ SM3 輸出 256 bits = 64 hex chars
    }

    @Test
    void testLoginWithSM3() {
        // 登入
        UserLoginForm form = new UserLoginForm();
        form.setUsername("testuser");
        form.setPassword("SecureP@ss123");

        ResponseDTO<LoginVO> response = userService.login(form);
        Assertions.assertTrue(response.getOk()); // ✅ 密碼驗證成功
    }

    @Test
    void testLoginWithWrongPassword() {
        UserLoginForm form = new UserLoginForm();
        form.setUsername("testuser");
        form.setPassword("WrongPassword");

        ResponseDTO<LoginVO> response = userService.login(form);
        Assertions.assertFalse(response.getOk()); // ✅ 密碼驗證失敗
    }
}
```

---

## 執行結果

**數據庫**:
```sql
SELECT user_id, username, password, password_salt FROM t_user WHERE username = 'testuser';

-- 結果:
-- user_id: 1
-- username: testuser
-- password: 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
--           ↑ SM3 hash (64 hex characters = 256 bits)
-- password_salt: a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456
--                ↑ 隨機鹽 (64 hex characters = 256 bits)
```

**安全性分析**:
- ✅ 使用國密 SM3 算法（符合密碼法）
- ✅ 每個用戶唯一鹽值（防止彩虹表）
- ✅ 密碼不可逆（無法從 hash 推導明文）
- ✅ 相同密碼不同用戶產生不同 hash

---

## 進階優化

### 1. 密碼強度驗證

```java
public class PasswordValidator {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public static boolean isStrongPassword(String password) {
        return STRONG_PASSWORD.matcher(password).matches();
    }
}
```

**使用**:
```java
if (!PasswordValidator.isStrongPassword(form.getPassword())) {
    return ResponseDTO.error(UserErrorCode.PASSWORD_TOO_WEAK);
}
```

---

### 2. 密碼歷史記錄（防止重複使用）

**t_user_password_history** 表:
```sql
CREATE TABLE t_user_password_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    password VARCHAR(128) NOT NULL,
    password_salt VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_user_id (user_id)
);
```

**驗證邏輯**:
```java
public boolean isPasswordReused(Long userId, String newPassword) {
    List<PasswordHistoryEntity> history = passwordHistoryDao.selectByUserId(userId);

    for (PasswordHistoryEntity record : history) {
        if (SM3Util.verify(newPassword, record.getPasswordSalt(), record.getPassword())) {
            return true; // 密碼已使用過
        }
    }

    return false;
}
```

---

## 相關規則

本範例直接關聯以下安全規範：

- **[中華人民共和國密碼法](https://www.gov.cn/xinwen/2019-10/26/content_5445331.htm)**
  - 第二十七條：商用密碼服務使用單位應當使用商用密碼進行保護

- **[GM/T 0004-2012 SM3密碼雜湊演算法](http://www.gmbz.org.cn/main/viewfile/2018011001400692565.html)**
  - SM3 演算法標準規範

---

## 總結

### 學到什麼

✅ **國密算法**:
- SM3 密碼雜湊（類似 SHA-256）
- BouncyCastle 函式庫使用

✅ **密碼安全**:
- 加鹽防止彩虹表攻擊
- 密碼不可逆存儲
- 每個用戶唯一鹽值

✅ **合規性**:
- 符合中國密碼法要求
- 滿足金融/政府行業標準

---

## 參考資料

- [BouncyCastle 官方文檔](https://www.bouncycastle.org/java.html)
- [SM3 演算法規範](http://www.gmbz.org.cn/main/viewfile/2018011001400692565.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
