# iGame Feature Builder

> iGaming 業務功能生成器：VIP 系統 + 錢包 API + 獎金引擎

## 🚀 快速開始

```bash
# 場景：需要開發 VIP 自動升級功能
User: "Create VIP auto-upgrade feature based on deposit and bet amounts"

# 自動生成：
# 1. VIP 等級管理（等級、權益、升級規則）
# 2. 升級計算邏輯（存款額 + 投注額）
# 3. 獎勵發放（升級禮金、專屬優惠）
# 4. 事件通知（Kafka 消息）
```

## 核心功能

### 1. VIP 系統

**VIP 等級設計**：
- Bronze（青銅）：存款 1,000 + 投注 5,000
- Silver（白銀）：存款 10,000 + 投注 50,000
- Gold（黃金）：存款 100,000 + 投注 500,000
- Platinum（鉑金）：存款 1,000,000 + 投注 5,000,000

**升級規則**：
```java
@Service
@RequiredArgsConstructor
public class VipManager {
    @Transactional(rollbackFor = Throwable.class)
    public void checkAndUpgrade(Long playerId) {
        // 計算玩家累計存款和投注
        BigDecimal totalDeposit = calculateTotalDeposit(playerId);
        BigDecimal totalBet = calculateTotalBet(playerId);

        // 判斷升級條件
        VipLevel newLevel = determineVipLevel(totalDeposit, totalBet);

        // 升級並發放獎勵
        if (newLevel.ordinal() > currentLevel.ordinal()) {
            upgradeVipLevel(playerId, newLevel);
            grantUpgradeBonus(playerId, newLevel);
            publishVipUpgradeEvent(playerId, newLevel);
        }
    }
}
```

---

### 2. 錢包 API

**錢包操作**：
- 充值（Deposit）：銀行轉賬、第三方支付
- 提款（Withdrawal）：到銀行卡、電子錢包
- 轉賬（Transfer）：主錢包 ↔ 遊戲錢包

**雙式記賬**：
```java
@Transactional(rollbackFor = Throwable.class)
public void deposit(Long playerId, BigDecimal amount) {
    // 借：玩家錢包（資產增加）
    walletDao.increaseBalance(playerId, amount);

    // 貸：平台錢包（負債增加）
    platformWalletDao.increaseBalance(amount);

    // 記錄流水
    transactionDao.insert(new TransactionEntity(playerId, amount, "DEPOSIT"));
}
```

---

### 3. 獎金引擎

**獎金類型**：
- 紅利（Bonus）：註冊紅利、首存紅利
- 返水（Rebate）：每日返水、每週返水
- 優惠（Promotion）：充值優惠、特殊活動

**獎金計算**：
```java
public BigDecimal calculateRebate(Long playerId, LocalDate date) {
    // 計算當日有效投注
    BigDecimal validBet = calculateValidBet(playerId, date);

    // 根據 VIP 等級確定返水比例
    BigDecimal rebateRate = getVipRebateRate(playerId);

    // 計算返水金額
    return validBet.multiply(rebateRate);
}
```

---

## 使用場景

### ✅ When to Use

1. **新遊戲平台搭建** - 完整 VIP/錢包/獎金系統
2. **現有平台功能擴展** - 新增 VIP 權益、獎金類型
3. **VIP 系統設計** - 等級體系、升級邏輯

### ❌ When NOT to Use

- 非 iGaming 業務
- 簡單的積分系統（過度設計）

## 常見問題

### Q1: 如何設計 VIP 等級體系？

**A**: 考慮 3 個維度：
- 門檻：存款 + 投注（雙重條件）
- 權益：返水率、專屬客服、生日禮金
- 保級：寬限期機制（3 個月）

### Q2: 如何處理錢包並發？

**A**: 使用樂觀鎖 + Redis 分佈式鎖：
```java
@Transactional(rollbackFor = Throwable.class)
public void withdraw(Long playerId, BigDecimal amount) {
    String lockKey = "wallet:lock:" + playerId;
    redisLock.lock(lockKey);
    try {
        // 檢查餘額（樂觀鎖）
        WalletEntity wallet = walletDao.selectByIdWithVersion(playerId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("餘額不足");
        }
        // 扣減餘額
        walletDao.decreaseBalance(playerId, amount, wallet.getVersion());
    } finally {
        redisLock.unlock(lockKey);
    }
}
```

### Q3: 如何防止獎金濫用？

**A**: 3 種策略：
- 流水要求（紅利需滿足 3 倍流水）
- 最大獎金限制（單日最高 10,000）
- 異常檢測（機器人刷獎金）

---

## 相關資源

- [SKILL.md](SKILL.md)
- [iGame Technical Specs](../../../../docs/iGame/technical-specs/)
- [smartadmin-crud-generator](../../foundation/full-stack/smartadmin-crud-generator/README.md)

---

**Version**: 1.0.0
**Last Updated**: 2026-01-30
