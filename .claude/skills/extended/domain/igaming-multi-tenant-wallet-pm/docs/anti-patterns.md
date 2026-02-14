## 🚫 常見錯誤與反模式

### 反模式 1: 多租戶數據洩漏

❌ **錯誤做法**：未在 WHERE 子句添加 tenant_id 過濾

```sql
-- 危險！可能查詢到其他租戶的數據
SELECT * FROM t_wallet_transaction WHERE player_id = 12345;
```

✅ **正確做法**：MyBatis 攔截器自動注入 tenant_id

```sql
-- 自動注入（由 TenantInterceptor 處理）
SELECT * FROM t_wallet_transaction
WHERE player_id = 12345 AND tenant_id = 1 AND deleted_flag = 0;
```

**SmartAdmin 實現**：

```java
// MyBatis 攔截器會自動注入，開發者無需手動添加
@Mapper
public interface WalletTransactionDao extends BaseMapper<WalletTransactionEntity> {
    // 自動注入 tenant_id
    List<WalletTransactionEntity> selectByPlayerId(Long playerId);
}
```

**ArchitectureTest 驗證**：

```java
@Test
void allDaoMethodsShouldNotManuallyFilterByTenantId() {
    // 驗證 Dao 方法不應手動添加 tenant_id 過濾（由攔截器自動處理）
}
```

---

### 反模式 2: 無縫錢包非冪等

❌ **錯誤做法**：transaction_id 未緩存，可能重複扣款

```java
// 危險！無冪等性保證
public void debitBalance(String transactionId, BigDecimal amount) {
    // 直接扣款，未檢查 transactionId 是否已處理
    walletDao.updateBalance(playerId, amount);
}
```

✅ **正確做法**：三層冪等性防護

```java
// Layer 1: Redis 快取（1-5ms）
if (redisTemplate.hasKey("idempotency:" + transactionId)) {
    return ResponseDTO.error("重複請求");
}

// Layer 2: 資料庫唯一索引（10-50ms）
try {
    transactionDao.insert(tx); // transaction_id UNIQUE
} catch (DuplicateKeyException e) {
    return ResponseDTO.error("重複請求");
}

// Layer 3: 分散式鎖（5-20ms）
RLock lock = redissonClient.getLock("wallet:lock:" + playerId);
try {
    lock.lock(5, TimeUnit.SECONDS);
    // 扣款邏輯
} finally {
    lock.unlock();
}
```

**詳細分析**：[knowledge/wallet-patterns.md §2](knowledge/wallet-patterns.md)

---

### 反模式 3: Token 驗證邏輯混淆

❌ **錯誤做法**：Bet API 和 Result API 使用相同的 Token 驗證邏輯

```java
// 危險！體育博彩 Result API 可能在 3 天後結算，Token 已過期
public void validateToken(String token) {
    if (isTokenExpired(token)) {
        throw new BusinessException("Token 已過期");
    }
}
```

✅ **正確做法**：根據 API 類型和遊戲類型採用不同策略

```java
public void validateTokenForBet(String token) {
    // Bet API: STRICT（嚴格驗證有效期）
    if (isTokenExpired(token)) {
        throw new BusinessException("TOKEN_EXPIRED");
    }
}

public void validateTokenForResult(String token, String gameType) {
    // Result API: CONDITIONAL
    if (isShortLifecycleGame(gameType)) {
        // 老虎機、桌遊：< 5 分鐘，重用 Bet Token 驗證
        validateTokenForBet(token);
    } else {
        // 體育博彩：> 1 小時，Fallback 到注單驗證
        if (isTokenExpired(token)) {
            // Fallback: 驗證注單是否存在
            if (!betRecordExists(token)) {
                throw new BusinessException("INVALID_BET");
            }
        }
    }
}
```

**詳細分析**：[knowledge/wallet-patterns.md §1](knowledge/wallet-patterns.md)

---

### 反模式 4: 流水要求驗證時機錯誤

❌ **錯誤做法**：投注時自動解鎖紅利錢包

```java
// 危險！玩家達標後繼續遊戲虧損，紅利已解鎖無法追回
public void onBetPlaced(BetEvent event) {
    accumulateTurnover(event.getValidBet());

    if (getRemainingTurnover() <= 0) {
        // 錯誤：自動解鎖
        unlockBonusWallet();
    }
}
```

✅ **正確做法**：取款時驗證達標才解鎖

```java
// 投注時：僅累積流水進度
public void onBetPlaced(BetEvent event) {
    accumulateTurnover(event.getValidBet());
    // 不自動解鎖
}

// 取款時：驗證達標才允許取款
public ResponseDTO<Void> requestWithdrawal(WithdrawalRequest request) {
    if (getRemainingTurnover() > 0) {
        return ResponseDTO.error("流水要求未達標，無法取款");
    }

    // 達標後解鎖紅利錢包
    unlockBonusWallet();
    processWithdrawal(request);
    return ResponseDTO.ok();
}
```

**業界標準**：
- Pragmatic Play: "Wagering requirement is only cleared when player initiates withdrawal"
- Evolution Gaming: "Bonus balance remains locked until requirement met AND withdrawal requested"

**詳細分析**：[knowledge/wallet-patterns.md §11](knowledge/wallet-patterns.md)

---

### 反模式 5: 併發衝突處理不足

❌ **錯誤做法**：無樂觀鎖，可能重複發放獎勵

```java
// 危險！TOCTOU 競爭條件
public void accumulateTurnover(BigDecimal validBet) {
    BigDecimal current = redis.get("turnover:" + playerId);
    BigDecimal newValue = current.add(validBet);
    redis.set("turnover:" + playerId, newValue);

    if (newValue >= requirement) {
        // 可能重複觸發
        grantReward();
    }
}
```

✅ **正確做法**：Lua 腳本原子性累積

```lua
-- Lua 腳本（原子性執行）
local key = KEYS[1]
local increment = tonumber(ARGV[1])
local requirement = tonumber(ARGV[2])
local lock_key = KEYS[2]

local new_value = redis.call('INCRBYFLOAT', key, increment)

if new_value >= requirement then
    -- 原子性標記（防止重複發放）
    local locked = redis.call('SETNX', lock_key, '1')
    if locked == 1 then
        redis.call('EXPIRE', lock_key, 3600)
        return {new_value, 1}  -- 返回 [累積值, 需發放獎勵]
    end
end

return {new_value, 0}  -- 返回 [累積值, 無需發放獎勵]
```

**Java 調用**：

```java
@Service
@RequiredArgsConstructor
public class TurnoverAccumulationManager {
    private final RedisScript<List> luaScript;
    private final StringRedisTemplate redisTemplate;

    @Transactional(rollbackFor = Throwable.class)
    public void accumulateTurnover(Long playerId, BigDecimal validBet, BigDecimal requirement) {
        List<Object> keys = Arrays.asList(
            "turnover:" + playerId,
            "turnover:lock:" + playerId
        );

        List result = redisTemplate.execute(
            luaScript,
            keys,
            validBet.toString(),
            requirement.toString()
        );

        BigDecimal newValue = new BigDecimal(result.get(0).toString());
        int shouldGrantReward = (int) result.get(1);

        if (shouldGrantReward == 1) {
            // 原子性保證，僅執行一次
            grantReward(playerId);
        }
    }
}
```

**詳細分析**：[knowledge/wallet-patterns.md §7](knowledge/wallet-patterns.md)

---

## 📚 版本資訊

- **Version**: 1.0.0
- **Last Updated**: 2026-01-29
- **Author**: SmartAdmin Team
- **Maintained By**: Claude Code Skills Team

### 變更歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2026-01-29 | 初始版本發布 |

### 相關文檔

- [README.md](README.md) - 快速參考與使用指南
- [knowledge/wallet-patterns.md](knowledge/wallet-patterns.md) - 無縫錢包 13 個專題整合
- [knowledge/multi-tenant-patterns.md](knowledge/multi-tenant-patterns.md) - 多商戶模式整合
- [knowledge/regional-requirements.md](knowledge/regional-requirements.md) - 地區合規要求
- [knowledge/prd-template.md](knowledge/prd-template.md) - PRD 文檔模板
- [knowledge/mermaid-best-practices.md](knowledge/mermaid-best-practices.md) - Mermaid 圖表策略

### 依賴知識庫

- [docs/IGaming/02_Finance_Center/seamless-wallet/](../../docs/IGaming/02_Finance_Center/seamless-wallet/) - 13 個無縫錢包專題
- [docs/plans/tenant/](../../docs/plans/tenant/) - 多商戶技術指南
- [docs/IGaming/02_Finance_Center/](../../docs/IGaming/02_Finance_Center/) - 錢包模型

### 擴展閱讀

- [.claude/shared/knowledge/smartadmin-patterns.md](../../.claude/shared/knowledge/smartadmin-patterns.md) - SmartAdmin 核心模式
- [.agent/rules/foundation/10-architecture-rules.md](../../.agent/rules/foundation/10-architecture-rules.md) - 架構規則

---

**文檔結束**

**下一步**: 閱讀 [README.md](README.md) 快速上手，或直接開始使用 Phase 1 收集需求。
