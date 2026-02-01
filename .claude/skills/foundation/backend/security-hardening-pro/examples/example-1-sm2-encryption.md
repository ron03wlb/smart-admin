# Example 1: SM2 Encryption Implementation

## Scenario
實現 SM2 國密加密用於敏感數據保護（用戶身份證號、銀行卡號）

## Input
```bash
User: "Add SM2 encryption for user ID card numbers"
```

## Generated Output
```java
@Service
@RequiredArgsConstructor
public class UserEncryptionService {
    private final SM2Util sm2Util;

    public String encryptIdCard(String idCard) {
        return sm2Util.encrypt(idCard);
    }

    public String decryptIdCard(String encryptedIdCard) {
        return sm2Util.decrypt(encryptedIdCard);
    }
}
```

## Expected Result
- 身份證號加密存儲在數據庫
- 查詢時自動解密顯示
- 符合國密 SM2 標準
