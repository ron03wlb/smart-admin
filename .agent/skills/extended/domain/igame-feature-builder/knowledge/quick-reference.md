# iGame Feature Builder - Quick Reference

## VIP 等級系統

```java
public enum VipTier {
    BRONZE(1, 0, 1.0),
    SILVER(2, 1000, 1.2),
    GOLD(3, 5000, 1.5),
    PLATINUM(4, 20000, 2.0),
    DIAMOND(5, 100000, 3.0);

    private final int level;
    private final int pointsRequired;
    private final double bonusMultiplier;
}
```

## 錢包 API

```java
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @PostMapping("/deposit")
    public ResponseDTO<TransactionVO> deposit(@RequestBody DepositForm form);

    @PostMapping("/withdraw")
    public ResponseDTO<TransactionVO> withdraw(@RequestBody WithdrawForm form);

    @GetMapping("/balance")
    public ResponseDTO<BalanceVO> getBalance();

    @GetMapping("/transactions")
    public ResponseDTO<PageResult<TransactionVO>> getTransactions(QueryForm form);
}
```

## 獎金引擎

```java
@Service
public class BonusService {

    public ResponseDTO<BonusVO> claimWelcomeBonus(Long userId);
    public ResponseDTO<BonusVO> processReloadBonus(DepositForm deposit);
    public ResponseDTO<BigDecimal> calculateCashback(Long userId, LocalDate date);
}
```
