# Example 1: Multi-Account Fraud Detection

## Scenario
檢測同一用戶註冊多個帳號套利（設備指紋 + IP + 註冊時間）

## Input
```bash
User: "Detect multi-account registration fraud"
```

## Generated Output
```java
@Service
public class MultiAccountDetector {
    public FraudRiskLevel detectMultiAccount(RegisterForm form) {
        // 1. 設備指紋檢查
        long deviceCount = userRepository.countByDeviceFingerprint(
            form.getDeviceId()
        );

        // 2. IP 地址檢查
        long ipCount = userRepository.countByIpAddress(
            form.getIpAddress(),
            LocalDateTime.now().minusHours(24)
        );

        // 3. 風險評分
        int riskScore = 0;
        if (deviceCount > 3) riskScore += 40;
        if (ipCount > 5) riskScore += 60;

        return riskScore > 70 ? FraudRiskLevel.HIGH : FraudRiskLevel.LOW;
    }
}
```

## Expected Result
- 實時檢測多帳號註冊
- 自動標記高風險用戶
- 支持人工審核
