# Fraud Detection - Examples

## 範例: 多帳號檢測規則

```java
@LiteflowComponent("multiAccountCheck")
public class MultiAccountCheckCmp extends NodeComponent {

    @Resource
    private DeviceFingerprintService fingerprintService;

    @Override
    public void process() throws Exception {
        RiskContext ctx = this.getContextBean(RiskContext.class);
        String fingerprint = ctx.getDeviceFingerprint();

        List<Long> relatedAccounts = fingerprintService.findByFingerprint(fingerprint);

        if (relatedAccounts.size() > 3) {
            ctx.addRiskFlag(RiskFlag.MULTI_ACCOUNT);
            ctx.setRiskScore(ctx.getRiskScore() + 50);
        }
    }
}
```
