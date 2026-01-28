# 錯誤恢復場景設計

## 問題來源
文檔第 2.3 節提到超時處理，但缺少具體的複雜場景處理邏輯。

## 缺失的場景

### 場景 1: 亂序請求（Out-of-Order Requests）

**問題描述**:
```
正常順序:
1. Bet Request (tx_id = "bet_123")
2. Bet Response
3. Result Request (tx_id = "result_456", refer_id = "bet_123")
4. Result Response

異常順序（網路延遲導致）:
1. Result Request 先到達（referencing bet_123）
2. Bet Request 後到達

問題: 營運商如何處理？
❌ 直接拒絕 → 玩家贏錢丟失
✅ 暫存 Result，等待 Bet
```

**解決方案**:
```java
public ResultResponse handleResultRequest(ResultRequest request) {
    String betTxId = request.getReferTransactionId();

    // 檢查 Bet 是否存在
    Option<WalletTransaction> betTx = transactionRepository
        .findByTransactionId(betTxId);

    if (betTx.isEmpty()) {
        // Bet 還沒到達 → 暫存 Result
        String key = "pending_result:" + request.getTransactionId();

        // 檢查是否已暫存
        if (redisTemplate.hasKey(key)) {
            return ResultResponse.pending("Waiting for bet");
        }

        // 首次遇到 → 暫存（TTL = 60 秒）
        redisTemplate.opsForValue().set(key,
            json(request),
            Duration.ofSeconds(60)
        );

        return ResultResponse.retry("Bet not found, retry after 1s");
    }

    // Bet 存在 → 正常處理
    return processResult(request, betTx.get());
}
```

### 場景 2: 預回滾（Pre-Rollback）

**問題描述**:
```
異常時序:
1. GP 發送 Bet Request
2. GP 超時（認為失敗）
3. GP 立即發送 Rollback Request
4. Rollback 先到達營運商
5. Bet Request 延遲 5 秒後才到達

問題: 如何處理這個 Rollback？
```

**解決方案**:
```java
public RollbackResponse handleRollback(RollbackRequest request) {
    String betTxId = request.getOriginalTransactionId();

    Option<WalletTransaction> betTx = transactionRepository
        .findByTransactionId(betTxId);

    if (betTx.isEmpty()) {
        // Bet 還沒到達 → 標記為「預回滾」
        String key = "pre_rollback:" + betTxId;
        redisTemplate.opsForValue().set(key,
            json(request),
            Duration.ofMinutes(5)
        );

        log.warn("Pre-rollback for bet: {}", betTxId);

        return RollbackResponse.accepted(
            "Will be applied when Bet arrives"
        );
    }

    // Bet 已存在 → 正常回滾
    return executeRollback(request, betTx.get());
}

public BetResponse handleBet(BetRequest request) {
    String txId = request.getTransactionId();

    // 檢查是否有預回滾標記
    String key = "pre_rollback:" + txId;
    if (redisTemplate.hasKey(key)) {
        log.info("Bet cancelled due to pre-rollback: {}", txId);

        // 記錄為 CANCELLED 狀態
        transactionRepository.save(
            WalletTransaction.cancelled(txId)
        );

        // 清除標記
        redisTemplate.delete(key);

        return BetResponse.cancelled(
            "Bet was rolled back before processing"
        );
    }

    // 正常處理
    return processBet(request);
}
```

### 場景 3: 部分失敗恢復（Two-Phase Commit）

**問題描述**:
```
場景: Bet 扣款成功，但在記錄注單時數據庫崩潰

T1: Bet Request 到達
T2: 扣款成功（玩家餘額 -100）
T3: 開始插入 bet_details 表
T4: 數據庫崩潰（主從切換）
T5: API 超時，未返回響應
T6: GP 重試（相同 transaction_id）

問題:
- wallet_transactions 表有記錄（扣款成功）
- bet_details 表沒有記錄（注單丟失）
- 返回「已處理」→ GP 認為成功
- 但實際上數據不完整
```

**解決方案**:
```java
@Transactional(rollbackFor = Throwable.class)
public BetResponse processBet(BetRequest request) {
    String txId = request.getTransactionId();

    // 階段 1: Prepare（預留資源）
    transactionRepository.save(
        WalletTransaction.builder()
            .transactionId(txId)
            .status(TransactionStatus.PREPARED)  // 關鍵狀態
            .build()
    );

    // 階段 2: 執行業務邏輯（全部完成或全部回滾）
    BigDecimal newBalance = walletService.deductBalance(...);
    betDetailsRepository.save(BetDetails.of(request));

    // 階段 3: Commit（標記為成功）
    transactionRepository.updateStatus(txId, TransactionStatus.SUCCESS);

    // 如果上面任何一步失敗，整個事務回滾
    // 確保要麼全部成功，要麼全部失敗

    return BetResponse.success(newBalance);
}
```

## 監控與告警

```yaml
metrics:
  # 亂序請求率
  - out_of_order_request_rate
    target: < 1%
    alert: > 5%

  # 預回滾發生率
  - pre_rollback_rate
    target: < 0.1%
    alert: > 1%

  # 部分失敗率
  - partial_failure_rate
    target: 0
    alert: > 0
```

## 決策總結

✅ **推薦方案**:
- 亂序請求: 暫存機制（TTL = 60 秒）
- 預回滾: 標記機制（TTL = 5 分鐘）
- 部分失敗: 兩階段提交（數據庫事務）

❌ **錯誤方案**:
- 直接拒絕亂序請求 → 玩家贏錢丟失
- 忽略預回滾 → 可能重複扣款
- 不使用事務 → 數據不一致
