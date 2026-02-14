# Data Masking Guide

Complete guide for implementing PII (Personally Identifiable Information) masking in SmartAdmin.

---

## Overview

**Purpose**: Protect sensitive data by masking it in API responses, logs, and UI displays

**Use Cases:**
- Hide phone numbers from customer support agents
- Mask credit card numbers in transaction history
- Protect email addresses in player lists
- Anonymize sensitive data in logs

---

## SmartAdmin Data Masking Module

**Location**: `smartadmin-support/src/main/java/net/lab1024/sa/support/datamasking/`

**Key Classes:**
- `@DataMasking` - Annotation for field-level masking
- `DataMaskingTypeEnum` - Pre-defined masking patterns
- `SmartDataMaskingUtil` - Programmatic masking utility
- `DataMaskingSerializer` - Jackson serializer for automatic masking

---

## Masking Types

### PHONE (Mobile Phone)

**Pattern**: 138****5678

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.PHONE)
    private String phone;  // 13812345678 → 138****5678
}
```

### EMAIL

**Pattern**: j***@example.com

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.EMAIL)
    private String email;  // john@example.com → j***@example.com
}
```

### ID_CARD (Identity Card Number)

**Pattern**: 110101******1234 (show first 6 and last 2 digits)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.ID_CARD)
    private String idCard;  // 11010119900101123X → 110101******3X
}
```

### BANK_CARD (Bank Card Number)

**Pattern**: 6222 **** **** 1234

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.BANK_CARD)
    private String bankCard;  // 6222600260001234567 → 6222 **** **** 4567
}
```

### CHINESE_NAME

**Pattern**: 张* (show first character)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.CHINESE_NAME)
    private String realName;  // 张三 → 张*
}
```

### ADDRESS

**Pattern**: 北京市******小区 (show first 6 chars, mask middle)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.ADDRESS)
    private String address;  // 北京市朝阳区建国路88号SOHO现代城 → 北京市朝阳******城
}
```

### PASSWORD

**Pattern**: ****** (completely hidden)

```java
@Data
public class UserVO {
    @DataMasking(type = DataMaskingTypeEnum.PASSWORD)
    private String password;  // Abc123!@ → ******
}
```

### CAR_LICENSE (Car License Plate)

**Pattern**: 京A****8 (show province/city code and last digit)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.CAR_LICENSE)
    private String carLicense;  // 京A12345 → 京A****5
}
```

### FIXED_PHONE (Fixed-line Phone)

**Pattern**: 010****1234 (mask middle digits)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.FIXED_PHONE)
    private String fixedPhone;  // 01012345678 → 010****5678
}
```

### USER_ID (User ID)

**Pattern**: Random UUID (completely anonymized)

```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.USER_ID)
    private String userId;  // 12345 → 8f7d6e5c-4b3a-2f1e-0d9c-8b7a6f5e4d3c
}
```

---

## Implementation Patterns

### Pattern 1: Automatic JSON Serialization

**Use Case**: Mask data in API responses

```java
@Data
public class PlayerVO {
    private Long id;
    private String nickname;

    @DataMasking(type = DataMaskingTypeEnum.PHONE)
    private String phone;

    @DataMasking(type = DataMaskingTypeEnum.EMAIL)
    private String email;

    @DataMasking(type = DataMaskingTypeEnum.ID_CARD)
    private String idCard;
}

// Controller
@GetMapping("/list")
public ResponseDTO<List<PlayerVO>> listPlayers() {
    List<Player> players = playerDao.selectList(null);
    List<PlayerVO> voList = SmartBeanUtil.copyList(players, PlayerVO.class);
    return ResponseDTO.ok(voList);
    // All fields with @DataMasking are automatically masked in JSON response
}
```

**JSON Response:**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "nickname": "Player001",
      "phone": "138****5678",
      "email": "j***@example.com",
      "idCard": "110101******3X"
    }
  ]
}
```

### Pattern 2: Programmatic Masking

**Use Case**: Mask data in logs or custom logic

```java
@Service
public class PlayerService {

    public void registerPlayer(PlayerAddForm form) {
        // Mask sensitive data in logs
        log.info("Player registered: phone={}, email={}",
            SmartDataMaskingUtil.dataMasking(form.getPhone(), DataMaskingTypeEnum.PHONE),
            SmartDataMaskingUtil.dataMasking(form.getEmail(), DataMaskingTypeEnum.EMAIL)
        );

        Player player = SmartBeanUtil.copy(form, Player.class);
        playerDao.insert(player);
    }
}
```

### Pattern 3: Batch Masking

**Use Case**: Mask multiple objects at once

```java
@Service
public class PlayerService {

    public List<PlayerVO> listPlayers() throws Exception {
        List<Player> players = playerDao.selectList(null);
        List<PlayerVO> voList = SmartBeanUtil.copyList(players, PlayerVO.class);

        // Batch masking (modifies objects in-place)
        SmartDataMaskingUtil.dataMasking(voList);

        return voList;
    }
}
```

---

## Custom Masking Logic

### Custom Masking Function

```java
public class CustomDataMaskingUtil {

    /**
     * Mask cryptocurrency wallet address
     * Pattern: 0x1234...5678 (show first 6 and last 4 chars)
     */
    public static String maskWalletAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    /**
     * Mask player balance
     * Pattern: $***.**
     */
    public static String maskBalance(BigDecimal balance) {
        if (balance == null) {
            return null;
        }
        int intLength = balance.toBigInteger().toString().length();
        return "$" + "*".repeat(intLength) + ".**";
    }
}
```

### Custom Jackson Serializer

```java
public class WalletAddressMaskSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeNull();
            return;
        }

        String masked = CustomDataMaskingUtil.maskWalletAddress(value);
        gen.writeString(masked);
    }
}

// Usage in VO
@Data
public class CryptoPaymentVO {
    private Long id;

    @JsonSerialize(using = WalletAddressMaskSerializer.class)
    private String walletAddress;  // 0x1234567890abcdef → 0x1234...cdef
}
```

---

## Field-Level Encryption vs. Masking

### When to Use Encryption

**Use Cases:**
- Data must remain confidential in database (SSN, passport, bank account)
- Compliance requirements (GDPR, PIPL)
- Need to decrypt data later for processing

**Implementation:**
```java
@Data
@TableName(value = "players", autoResultMap = true)
public class Player {
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String ssn;  // Encrypted in database

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String bankAccount;
}
```

### When to Use Masking

**Use Cases:**
- Data visible in UI but should be partially hidden
- Logs should not contain full PII
- Customer support agents should see limited info

**Implementation:**
```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.PHONE)
    private String phone;  // Masked in JSON response
}
```

### Combined Approach

**Recommended**: Encrypt in database + mask in response

```java
// Entity (database)
@Data
@TableName(value = "players", autoResultMap = true)
public class Player {
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String ssn;  // Encrypted: "ZW5jcnlwdGVkX3Nzbl92YWx1ZQ=="
}

// VO (API response)
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.ID_CARD)
    private String ssn;  // Masked: "110101******3X"
}

// Service (decryption + masking)
@Service
public class PlayerService {
    public PlayerVO getPlayer(Long id) {
        Player player = playerDao.selectById(id);  // Decrypted: "11010119900101123X"
        return SmartBeanUtil.copy(player, PlayerVO.class);  // Masked: "110101******3X"
    }
}
```

---

## Permission-Based Masking

### Use Case: Show full data to admins, masked to operators

```java
@Data
public class PlayerVO {
    private Long id;
    private String nickname;
    private String phone;  // Full or masked based on permission
    private String email;
}

@Service
@RequiredArgsConstructor
public class PlayerService {

    public PlayerVO getPlayer(Long id) {
        Player player = playerDao.selectById(id);
        PlayerVO vo = SmartBeanUtil.copy(player, PlayerVO.class);

        // Mask data unless user has "player:view:sensitive" permission
        if (!StpUtil.hasPermission("player:view:sensitive")) {
            vo.setPhone(SmartDataMaskingUtil.dataMasking(vo.getPhone(), DataMaskingTypeEnum.PHONE));
            vo.setEmail(SmartDataMaskingUtil.dataMasking(vo.getEmail(), DataMaskingTypeEnum.EMAIL));
        }

        return vo;
    }
}
```

---

## Logging Best Practices

### NEVER Log Unmasked Sensitive Data

```java
// ❌ BAD: Logs unmasked phone number
log.info("Player registered: phone={}", player.getPhone());

// ✅ GOOD: Logs masked phone number
log.info("Player registered: phone={}",
    SmartDataMaskingUtil.dataMasking(player.getPhone(), DataMaskingTypeEnum.PHONE));
```

### Structured Logging with Masking

```java
@Slf4j
@Service
public class PlayerService {

    public void registerPlayer(PlayerAddForm form) {
        // Structured log with masked PII
        Map<String, Object> logData = Map.of(
            "action", "player_register",
            "phone", SmartDataMaskingUtil.dataMasking(form.getPhone(), DataMaskingTypeEnum.PHONE),
            "email", SmartDataMaskingUtil.dataMasking(form.getEmail(), DataMaskingTypeEnum.EMAIL),
            "timestamp", LocalDateTime.now()
        );

        log.info("Player registration: {}", JSON.toJSONString(logData));
    }
}
```

---

## GDPR Compliance

### Right to Erasure (Data Deletion)

```java
@Service
@RequiredArgsConstructor
public class GdprService {

    @Transactional
    public void deletePlayerData(Long playerId) {
        Player player = playerDao.selectById(playerId);

        // Anonymize player data (retain for regulatory compliance)
        player.setEmail("deleted-" + UUID.randomUUID() + "@anonymized.com");
        player.setNickname("Deleted User");
        player.setPhone(null);
        player.setSsn(null);
        player.setDeleted(true);
        player.setDeletedAt(LocalDateTime.now());

        playerDao.updateById(player);

        log.info("Player data anonymized (GDPR): playerId={}", playerId);
    }
}
```

### Data Portability (Export Player Data)

```java
@Service
@RequiredArgsConstructor
public class GdprService {

    public PlayerDataExportVO exportPlayerData(Long playerId) {
        Player player = playerDao.selectById(playerId);

        // Export full data (no masking) for GDPR data portability
        PlayerDataExportVO export = new PlayerDataExportVO();
        export.setPersonalInfo(SmartBeanUtil.copy(player, PersonalInfoVO.class));
        export.setTransactions(transactionDao.selectByPlayerId(playerId));
        export.setGameHistory(gameRoundDao.selectByPlayerId(playerId));

        log.info("Player data exported (GDPR): playerId={}", playerId);

        return export;
    }
}
```

---

## Testing Masking Rules

### Unit Test

```java
@Test
public void testPhoneMasking() {
    String phone = "13812345678";
    String masked = SmartDataMaskingUtil.dataMasking(phone, DataMaskingTypeEnum.PHONE);

    assertEquals("138****5678", masked);
}

@Test
public void testEmailMasking() {
    String email = "john.doe@example.com";
    String masked = SmartDataMaskingUtil.dataMasking(email, DataMaskingTypeEnum.EMAIL);

    assertEquals("j***@example.com", masked);
}

@Test
public void testIdCardMasking() {
    String idCard = "11010119900101123X";
    String masked = SmartDataMaskingUtil.dataMasking(idCard, DataMaskingTypeEnum.ID_CARD);

    assertEquals("110101******3X", masked);
}
```

---

## Security Checklist

- [ ] All PII fields in VO have `@DataMasking` annotation
- [ ] Logs use `SmartDataMaskingUtil` to mask sensitive data
- [ ] Admin panel shows masked data unless user has `view:sensitive` permission
- [ ] GDPR data export returns full data (no masking)
- [ ] Production logs reviewed for leaked PII
- [ ] Masking rules tested in unit tests

---

## SmartAdmin Foundation Module

**Location**: `smartadmin-support/src/main/java/net/lab1024/sa/support/datamasking/`

**Key Classes:**
- `@DataMasking` - Annotation for field-level masking
- `DataMaskingTypeEnum` - Pre-defined masking patterns (PHONE, EMAIL, ID_CARD, etc.)
- `SmartDataMaskingUtil` - Programmatic masking utility
- `DataMaskingSerializer` - Jackson serializer for automatic JSON masking

**Usage:**
```java
@Data
public class PlayerVO {
    @DataMasking(type = DataMaskingTypeEnum.PHONE)
    private String phone;  // Automatically masked in JSON response
}
```
