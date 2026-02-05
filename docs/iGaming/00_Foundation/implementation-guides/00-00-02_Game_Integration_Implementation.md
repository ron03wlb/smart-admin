# 00-00-02 遊戲營運實作指南 (Game Integration Implementation Guide)

**版本**: 1.0.0
**創建日期**: 2026-02-04
**來源**: 從 00-00_IMPLEMENTATION_GUIDE.md 拆分（§5-7）
**狀態**: 🔄 部分完成（§5 ✅ 完整，§6-7 📝 PLANNED）

---

## 📋 文檔目的

本文檔涵蓋 iGaming 平台**遊戲營運**的實作指南，包括遊戲廠商對接、Seamless Wallet API 實作和流水計算邏輯設計。

**適用對象**：
- 後端開發工程師（遊戲模塊）
- 系統整合工程師
- 技術架構師

**使用方式**：
1. 按順序完成 3 個核心任務（遊戲對接 → Seamless Wallet → 流水計算）
2. 每個任務包含：閱讀順序、實作步驟、驗證清單、常見陷阱
3. 參考 SmartAdmin 架構模式（Entity、Manager、Service）

---

## 📚 目錄

5. [對接新遊戲廠商](#5-對接新遊戲廠商) - ✅ 完整內容
6. [實作 Seamless Wallet API](#6-實作-seamless-wallet-api) - 📝 PLANNED
7. [設計流水計算邏輯](#7-設計流水計算邏輯) - 📝 PLANNED

---

## ⚠️ 狀態說明

- **§5 對接新遊戲廠商**: 完整內容（包含實作步驟、代碼範例、驗證清單）
- **§6-7**: 內容開發中，預計 Phase 5 後續完成
- **臨時替代方案**: 參考相關文檔獲取詳細信息

---

## 5. 對接新遊戲廠商

### 📖 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [00-00 BUSINESS_FLOWS](./00-00_BUSINESS_FLOWS.md) | §2 遊戲對接流程 | 10 分鐘 | 完整業務流程 |
| 2 | [02-02 Seamless_Wallet_API](../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §4 Token 驗證 | 10 分鐘 | 安全機制 |
| 3 | [02-02 Seamless_Wallet_API](../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §4.3 冪等性設計 | 8 分鐘 | 請求去重 |
| 4 | [02-04 Game_Provider_Cases](../02_Game_Operations/02-04_Game_Provider_Cases.md) | 全文 | 15 分鐘 | 廠商案例 |

### 🎯 實作目標

完整對接新遊戲廠商，實作：
- Seamless Wallet API（下注/贏錢/取消）
- Token 驗證機制
- 冪等性處理（三層防護）
- 並發控制（Redis Lua）
- 錯誤恢復機制

### 📝 實作步驟

#### Step 1: 定義 Seamless Wallet API 接口

```java
/**
 * Seamless Wallet API 接口
 *
 * 標準接口：
 * 1. /api/game/debit - 下注扣款
 * 2. /api/game/credit - 贏錢加款
 * 3. /api/game/cancel - 取消交易
 * 4. /api/game/balance - 查詢餘額
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class SeamlessWalletController {

    private final SeamlessWalletService seamlessWalletService;
    private final TokenVerificationService tokenVerificationService;

    /**
     * 下注扣款
     *
     * @param request 下注請求
     * @return 交易結果
     */
    @PostMapping("/debit")
    public ResponseDTO<DebitResponse> debit(@RequestBody @Valid DebitRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行下注扣款
        DebitResponse response = seamlessWalletService.debit(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 贏錢加款
     *
     * @param request 贏錢請求
     * @return 交易結果
     */
    @PostMapping("/credit")
    public ResponseDTO<CreditResponse> credit(@RequestBody @Valid CreditRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行贏錢加款
        CreditResponse response = seamlessWalletService.credit(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 取消交易
     *
     * @param request 取消請求
     * @return 交易結果
     */
    @PostMapping("/cancel")
    public ResponseDTO<CancelResponse> cancel(@RequestBody @Valid CancelRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 執行取消
        CancelResponse response = seamlessWalletService.cancel(request);

        return ResponseDTO.ok(response);
    }

    /**
     * 查詢餘額
     *
     * @param request 餘額查詢請求
     * @return 餘額
     */
    @PostMapping("/balance")
    public ResponseDTO<BalanceResponse> getBalance(@RequestBody @Valid BalanceRequest request) {
        // 1. 驗證 Token
        tokenVerificationService.verifyToken(request.getToken());

        // 2. 查詢餘額
        BalanceResponse response = seamlessWalletService.getBalance(request);

        return ResponseDTO.ok(response);
    }
}
```

#### Step 2: 實作 Token 驗證機制

```java
/**
 * Token 驗證服務
 *
 * Token 組成：
 * player_id|tenant_id|timestamp|signature
 *
 * 簽名算法：
 * signature = HMAC-SHA256(player_id + tenant_id + timestamp, secret_key)
 */
@Service
@RequiredArgsConstructor
public class TokenVerificationService {

    @Value("${game.api.secret-key}")
    private String secretKey;

    @Value("${game.api.token-ttl:300}") // 5 分鐘
    private int tokenTtl;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 生成 Token
     *
     * @param playerId 玩家ID
     * @param tenantId 租戶ID
     * @return Token
     */
    public String generateToken(Long playerId, String tenantId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String data = playerId + "|" + tenantId + "|" + timestamp;

        // HMAC-SHA256 簽名
        String signature = HmacUtils.hmacSha256Hex(secretKey, data);

        // 組裝 Token
        String token = data + "|" + signature;

        // Base64 編碼
        return Base64.getEncoder().encodeToString(token.getBytes());
    }

    /**
     * 驗證 Token
     *
     * 檢查項目：
     * 1. 格式正確
     * 2. 簽名合法
     * 3. 未過期
     * 4. 未被重放（Redis 黑名單）
     *
     * @param token Token
     * @throws TokenInvalidException 驗證失敗
     */
    public void verifyToken(String token) {
        try {
            // 1. Base64 解碼
            String decoded = new String(Base64.getDecoder().decode(token));

            // 2. 解析 Token
            String[] parts = decoded.split("\\|");
            if (parts.length != 4) {
                throw new TokenInvalidException("Token 格式錯誤");
            }

            String playerId = parts[0];
            String tenantId = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String signature = parts[3];

            // 3. 驗證簽名
            String expectedSignature = HmacUtils.hmacSha256Hex(
                secretKey,
                playerId + "|" + tenantId + "|" + timestamp
            );

            if (!MessageDigest.isEqual(signature.getBytes(), expectedSignature.getBytes())) {
                throw new TokenInvalidException("Token 簽名無效");
            }

            // 4. 驗證有效期
            long now = System.currentTimeMillis() / 1000;
            if (now - timestamp > tokenTtl) {
                throw new TokenInvalidException("Token 已過期");
            }

            // 5. 防重放攻擊（Redis 黑名單）
            String blacklistKey = "token:blacklist:" + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                throw new TokenInvalidException("Token 已被使用");
            }

            // 6. 加入黑名單（TTL = Token 有效期）
            redisTemplate.opsForValue().set(
                blacklistKey,
                "1",
                tokenTtl,
                TimeUnit.SECONDS
            );

        } catch (IllegalArgumentException e) {
            throw new TokenInvalidException("Token 解碼失敗", e);
        }
    }
}
```

#### Step 3: 實作冪等性處理（三層防護）

```java
/**
 * Seamless Wallet 服務
 *
 * 冪等性三層防護：
 * 1. Redis 快速檢查（99% 場景）
 * 2. 資料庫檢查（Redis 失效時）
 * 3. 分佈式鎖（極端併發時）
 */
@Service
@RequiredArgsConstructor
public class SeamlessWalletService {

    private final WalletService walletService;
    private final GameTransactionDao gameTransactionDao;
    private final RedisTemplate<String, GameTransactionResult> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 下注扣款（冪等性保證）
     *
     * @param request 下注請求
     * @return 交易結果
     */
    public DebitResponse debit(DebitRequest request) {
        String requestId = request.getRequestId();

        // 第一層：Redis 快速檢查（99% 場景）
        String cacheKey = "game:tx:" + requestId;
        GameTransactionResult cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Request {} already processed (cached)", requestId);
            return DebitResponse.fromCachedResult(cached);
        }

        // 第二層：資料庫檢查（Redis 失效時）
        GameTransaction existing = gameTransactionDao.findByRequestId(requestId);

        if (existing != null) {
            log.info("Request {} already processed (db)", requestId);

            // 回寫 Redis
            GameTransactionResult result = GameTransactionResult.from(existing);
            redisTemplate.opsForValue().set(cacheKey, result, 15, TimeUnit.MINUTES);

            return DebitResponse.fromDbRecord(existing);
        }

        // 第三層：分佈式鎖（極端併發時）
        String lockKey = "game:lock:" + requestId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 嘗試獲取鎖（等待 3 秒，持有 10 秒）
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);

            if (!locked) {
                throw new ConcurrentRequestException("請求處理中，請稍後重試");
            }

            // 雙重檢查（獲得鎖後再次確認）
            GameTransaction doubleCheck = gameTransactionDao.findByRequestId(requestId);
            if (doubleCheck != null) {
                log.info("Request {} already processed (double check)", requestId);
                return DebitResponse.fromDbRecord(doubleCheck);
            }

            // 執行扣款
            walletService.debitWallet(
                request.getPlayerId(),
                request.getAmount(),
                "GAME_BET:" + requestId
            );

            // 記錄交易
            GameTransaction transaction = GameTransaction.builder()
                .requestId(requestId)
                .playerId(request.getPlayerId())
                .tenantId(request.getTenantId())
                .transactionType(TransactionType.DEBIT)
                .amount(request.getAmount())
                .gameId(request.getGameId())
                .roundId(request.getRoundId())
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

            gameTransactionDao.insert(transaction);

            // 快取結果（15 分鐘）
            GameTransactionResult result = GameTransactionResult.from(transaction);
            redisTemplate.opsForValue().set(cacheKey, result, 15, TimeUnit.MINUTES);

            return DebitResponse.fromTransaction(transaction);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException("鎖獲取被中斷", e);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 贏錢加款（冪等性保證）
     */
    public CreditResponse credit(CreditRequest request) {
        // 類似 debit()，省略重複邏輯
        // ...
    }

    /**
     * 取消交易（冪等性保證）
     */
    public CancelResponse cancel(CancelRequest request) {
        String originalRequestId = request.getOriginalRequestId();

        // 1. 查詢原始交易
        GameTransaction original = gameTransactionDao.findByRequestId(originalRequestId);

        if (original == null) {
            throw new TransactionNotFoundException("原始交易不存在：" + originalRequestId);
        }

        if (original.getStatus() == TransactionStatus.CANCELLED) {
            log.info("Transaction {} already cancelled", originalRequestId);
            return CancelResponse.alreadyCancelled();
        }

        // 2. 退款
        walletService.creditWallet(
            original.getPlayerId(),
            original.getAmount(),
            "GAME_CANCEL:" + request.getRequestId()
        );

        // 3. 更新原始交易狀態
        original.setStatus(TransactionStatus.CANCELLED);
        original.setCancelledAt(LocalDateTime.now());
        gameTransactionDao.update(original);

        // 4. 清除快取
        redisTemplate.delete("game:tx:" + originalRequestId);

        return CancelResponse.success();
    }
}
```

#### Step 4: 實作錯誤恢復機制

```java
/**
 * 遊戲交易恢復服務
 *
 * 處理場景：
 * 1. 網絡超時導致的懸掛交易
 * 2. 系統異常導致的不一致狀態
 * 3. 廠商回調失敗
 */
@Service
@RequiredArgsConstructor
public class GameTransactionRecoveryService {

    private final GameTransactionDao gameTransactionDao;
    private final WalletService walletService;
    private final GameProviderService gameProviderService;

    /**
     * 定時掃描懸掛交易
     *
     * 執行時間：每 5 分鐘
     * 懸掛交易定義：創建超過 10 分鐘且狀態為 PENDING
     */
    @Scheduled(fixedDelay = 300000)
    public void scanPendingTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        List<GameTransaction> pending = gameTransactionDao.findPendingBefore(threshold);

        for (GameTransaction tx : pending) {
            try {
                recoverTransaction(tx);
            } catch (Exception e) {
                log.error("Failed to recover transaction {}", tx.getRequestId(), e);
            }
        }
    }

    /**
     * 恢復單筆交易
     *
     * 恢復策略：
     * 1. 向遊戲廠商查詢交易狀態
     * 2. 根據廠商結果調整系統狀態
     * 3. 補償錢包餘額
     */
    private void recoverTransaction(GameTransaction tx) {
        // 1. 向遊戲廠商查詢
        GameProviderTransactionStatus providerStatus =
            gameProviderService.queryTransactionStatus(tx.getRoundId());

        // 2. 根據廠商狀態決定恢復動作
        switch (providerStatus) {
            case SUCCESS -> {
                // 廠商成功，系統更新為成功
                tx.setStatus(TransactionStatus.SUCCESS);
                gameTransactionDao.update(tx);
                log.info("Transaction {} recovered: SUCCESS", tx.getRequestId());
            }

            case FAILED -> {
                // 廠商失敗，退款
                if (tx.getTransactionType() == TransactionType.DEBIT) {
                    walletService.creditWallet(
                        tx.getPlayerId(),
                        tx.getAmount(),
                        "RECOVERY:" + tx.getRequestId()
                    );
                }

                tx.setStatus(TransactionStatus.FAILED);
                gameTransactionDao.update(tx);
                log.info("Transaction {} recovered: FAILED", tx.getRequestId());
            }

            case NOT_FOUND -> {
                // 廠商無記錄，視為失敗
                if (tx.getTransactionType() == TransactionType.DEBIT) {
                    walletService.creditWallet(
                        tx.getPlayerId(),
                        tx.getAmount(),
                        "RECOVERY:" + tx.getRequestId()
                    );
                }

                tx.setStatus(TransactionStatus.NOT_FOUND);
                gameTransactionDao.update(tx);
                log.warn("Transaction {} not found in provider", tx.getRequestId());
            }
        }

        // 3. 發送告警（如果是異常狀態）
        if (tx.getStatus() == TransactionStatus.FAILED ||
            tx.getStatus() == TransactionStatus.NOT_FOUND) {
            alertService.sendTransactionRecoveryAlert(tx);
        }
    }
}
```

### ✅ 驗證清單

- [ ] Token 驗證機制正確（簽名、有效期、防重放）
- [ ] 冪等性測試通過（重複請求返回相同結果）
- [ ] 並發測試通過（1000 TPS 無重複扣款）
- [ ] 取消交易正確退款
- [ ] 錯誤恢復機制正常運作
- [ ] 懸掛交易自動恢復
- [ ] 所有遊戲交易記錄到審計日誌

### ⚠️ 常見陷阱

1. **Token 重放攻擊**：必須使用 Redis 黑名單
2. **冪等性失效**：三層防護缺一不可
3. **分佈式鎖超時**：持有時間應大於業務執行時間
4. **懸掛交易未處理**：定時任務必須穩定運行

---


---

## 6. 實作 Seamless Wallet API

> **📝 STATUS**: PLANNED - 內容開發中  
> **預計完成**: Phase 5 後續  
> **涉及模塊**: 02_Game_Operations, 01_Core_Financial_Loop

### 實作目標
[待補充]

### 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [02-02 Seamless Wallet API](../../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §1 API 設計 | 15 分鐘 | Token 驗證、冪等性 |
| 2 | [02-02 Seamless Wallet API](../../02_Game_Operations/02-02_Seamless_Wallet_API.md) | §3 並發控制 | 10 分鐘 | Redis 分佈式鎖 |
| 3 | [01-02 Wallet Architecture](../../02_Finance_Center/02-06_Wallet_Architecture.md) | §4 錯誤恢復 | 12 分鐘 | Saga 模式 |

### 實作步驟
[待補充：包含 API 端點實作、Token 驗證機制、並發控制、錯誤恢復等]

### 驗證清單
- [ ] Token 驗證機制正確（簽名、有效期、防重放）
- [ ] 冪等性測試通過（重複請求返回相同結果）
- [ ] 並發測試通過（1000 TPS 無重複扣款）
- [ ] 錯誤恢復機制正常運作

### 常見陷阱
1. **Token 重放攻擊**：必須使用 Redis 黑名單
2. **冪等性失效**：三層防護缺一不可
3. **分佈式鎖超時**：持有時間應大於業務執行時間

---

## 7. 設計流水計算邏輯

> **📝 STATUS**: PLANNED - 內容開發中  
> **預計完成**: Phase 5 後續  
> **涉及模塊**: 02_Game_Operations, 04_Activity_Center

### 實作目標
[待補充]

### 閱讀順序

| 順序 | 文檔 | 章節 | 閱讀時間 | 重點內容 |
|------|------|------|---------|---------|
| 1 | [02-03 Turnover Calculation](../../03_Game_Center/03-04_Turnover_Calculation.md) | §1 三層驗證 | 20 分鐘 | Layer 1/2/3 架構 |
| 2 | [02-03 Turnover Calculation](../../03_Game_Center/03-04_Turnover_Calculation.md) | §5 遊戲權重 | 10 分鐘 | 免費旋轉處理 |
| 3 | [04-01 Activity System](../../04_Activity_Center/04-04_Activity_Bonus.md) | §5 流水要求 | 15 分鐘 | Wagering 計算 |

### 實作步驟
[待補充：包含三層驗證架構實作、有效投注計算、遊戲權重配置等]

### 驗證清單
- [ ] Layer 1/2/3 驗證規則正確執行
- [ ] 有效投注計算準確
- [ ] 遊戲權重配置生效
- [ ] 免費旋轉流水計算正確

### 常見陷阱
1. **忽略免費旋轉**：免費旋轉通常不計入流水
2. **權重配置錯誤**：體育博彩與老虎機權重差異大
3. **跨模組不一致**：錢包、活動、對帳必須使用同一算法

---

**文檔版本**: 1.0.0  
**最後更新**: 2026-02-04  
**維護團隊**: Game Integration Team & Backend Team

**📚 返回**: [實作指南總索引](../00-00_IMPLEMENTATION_GUIDE_INDEX.md)
