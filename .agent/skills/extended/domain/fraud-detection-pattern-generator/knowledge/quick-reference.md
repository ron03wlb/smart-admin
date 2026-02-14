# Fraud Detection - Quick Reference

## 風控規則類型

| 類型 | 檢測項目 | 觸發條件 |
|------|----------|----------|
| 多帳號 | 設備指紋、IP | 相同指紋 > 3 帳號 |
| 獎金濫用 | 投注模式 | 低風險套利行為 |
| 支付欺詐 | 交易速度 | 5分鐘內 > 3筆 |
| 洗錢 | 拆分交易 | 大額分拆多筆 |

## 風控服務範例

```java
@Service
public class RiskControlService {

    public RiskResult evaluate(Transaction tx) {
        List<RiskRule> rules = riskRuleDao.findActiveRules();
        int score = 0;

        for (RiskRule rule : rules) {
            if (rule.matches(tx)) {
                score += rule.getWeight();
            }
        }

        return new RiskResult(score, determineAction(score));
    }
}
```
