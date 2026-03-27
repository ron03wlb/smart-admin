---
title: "ADR-013: Manager 邊界 (@Transactional / @Cacheable)"
status: accepted
date: 2026-02-15
deciders: Tech Lead, Architecture Team
---

# ADR-013: Manager 邊界 (@Transactional / @Cacheable)

## 狀態

已接受 (Accepted)

## 背景

SmartAdmin 四層架構中，事務管理 (`@Transactional`) 和快取管理 (`@Cached`) 的使用位置不統一。部分開發者在 Service 層使用 `@Transactional`，導致：
1. 事務範圍過大（Service 方法包含遠程呼叫）
2. 快取失效時機不明確
3. 分散式鎖與事務交互問題

## 決策

### 規則

**Manager 層是唯一允許使用以下註解的層級：**

| 註解 | 層級限制 | 說明 |
|------|---------|------|
| `@Transactional` | Manager only | 資料庫事務 |
| `@Cached` / `@CacheUpdate` / `@CacheInvalidate` | Manager only | JetCache 快取 |
| `@DistributedLock` | Manager only | Redisson 分散式鎖 |

### 其他層級的責任

| 層級 | 責任 | 禁止 |
|------|------|------|
| Controller | 參數驗證、回應格式 | 任何業務邏輯 |
| Service | 業務編排、遠程呼叫、Try/Either 錯誤處理 | @Transactional, @Cached |
| Manager | 事務、快取、鎖、單一聚合操作 | 遠程服務呼叫 |
| Dao | MyBatis-Plus 資料存取 | 業務邏輯 |

### 程式碼範例

```java
// ✅ 正確：Manager 層持有事務
@Component
@RequiredArgsConstructor
public class WalletManager {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;

    @Transactional(rollbackFor = Throwable.class)
    @Cached(name = "wallet:balance:", key = "#tenantId + ':' + #playerId",
            cacheType = CacheType.BOTH)
    public Option<WalletBalanceVO> getBalance(Long tenantId, Long playerId) {
        return Option.of(walletDao.selectBalance(tenantId, playerId))
                .map(WalletBalanceVO::fromEntity);
    }

    @Transactional(rollbackFor = Throwable.class)
    @CacheUpdate(name = "wallet:balance:", key = "#tenantId + ':' + #playerId",
                 value = "#result")
    public WalletBalanceVO debit(Long tenantId, Long playerId, BigDecimal amount,
                                  String transactionId) {
        // 1. 樂觀鎖更新餘額
        int rows = walletDao.debit(tenantId, playerId, amount);
        if (rows == 0) {
            throw new InsufficientFundsException(tenantId, playerId, amount);
        }
        // 2. 寫入交易記錄
        transactionDao.insert(TransactionEntity.debit(tenantId, playerId, amount, transactionId));
        // 3. 返回新餘額
        return getBalance(tenantId, playerId).getOrElseThrow(
            () -> new WalletNotFoundException(tenantId, playerId));
    }
}

// ✅ 正確：Service 層編排業務，不持有事務
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletManager walletManager;
    private final RiskService riskService;     // 遠程呼叫
    private final EventPublisher eventPublisher;

    public Either<ErrorCode, DebitResultVO> processDebit(DebitForm form) {
        // 1. 風控檢查 (可能遠程呼叫)
        RiskResult risk = riskService.evaluate(form);
        if (risk.isBlocked()) {
            return Either.left(ErrorCode.RISK_BLOCKED);
        }

        // 2. 執行扣款 (Manager 管理事務)
        return Try.of(() -> walletManager.debit(
                form.getTenantId(), form.getPlayerId(),
                form.getAmount(), form.getTransactionId()))
            .map(balance -> {
                // 3. 發布事件 (事務外)
                eventPublisher.publish(WalletDebited.of(form));
                return Either.<ErrorCode, DebitResultVO>right(
                    DebitResultVO.of(balance));
            })
            .getOrElseGet(e -> Either.left(ErrorCode.DEBIT_FAILED));
    }
}

// ❌ 錯誤：Service 層不應使用 @Transactional
@Service
public class BadWalletService {
    @Transactional  // ❌ 違反 ADR-013
    public void processDebit(DebitForm form) {
        riskService.evaluate(form);  // ❌ 事務內包含遠程呼叫
        walletDao.debit(...);
    }
}
```

## ArchUnit 驗證

```java
@ArchTest
static ArchRule transactionalOnlyInManager =
    noClasses().that()
        .resideInAnyPackage("..service..", "..controller..", "..dao..")
        .should().beAnnotatedWith(Transactional.class)
        .orShould().beAnnotatedWith(Cached.class)
        .because("ADR-013: @Transactional and @Cached are only allowed in Manager layer");

@ArchTest
static ArchRule managerShouldNotCallRemoteService =
    noClasses().that().resideInAPackage("..manager..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..feign..", "..client..")
        .because("ADR-013: Manager should not make remote service calls");
```

## 後果

- 正向：事務範圍精確，不包含遠程呼叫
- 正向：快取操作與事務綁定，一致性有保障
- 正向：ArchUnit 自動驗證合規
- 負向：增加一個層級的間接呼叫
- 負向：新成員需理解四層職責分界
