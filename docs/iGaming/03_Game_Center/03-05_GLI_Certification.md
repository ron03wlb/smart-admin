# 03-05 GLI Certification (GLI 認證流程)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

Gaming Laboratories International (GLI) 是全球最大的遊戲測試和認證機構。本文檔說明如何準備和通過 GLI 認證，確保 iGaming 平台符合 RNG（隨機數生成器）和遊戲公平性標準。

### GLI 認證範圍

| 認證類型 | 標準 | 說明 |
|---------|------|------|
| RNG 認證 | GLI-19 | 互動博彩系統標準 |
| 遊戲認證 | GLI-11 | 電子遊戲機標準 |
| 系統認證 | GLI-19 | 平台系統安全 |
| 體育博彩 | GLI-33 | 體育博彩標準 |

---

## GLI-19 標準概述

GLI-19 是互動博彩系統的核心標準，涵蓋以下領域：

### 1. RNG 要求 (Section 2)

| 要求項 | 說明 | 驗證方法 |
|--------|------|---------|
| **統計隨機性** | 輸出必須通過 NIST SP 800-22 測試套件 | 數學分析 |
| **不可預測性** | 不能通過任何已知方法預測下一個輸出 | 安全審查 |
| **非重複性** | 週期長度必須足夠長（至少 2^32） | 算法分析 |
| **種子安全** | 種子必須安全生成和存儲 | 程式碼審查 |

### 2. 遊戲邏輯要求 (Section 3)

| 要求項 | 說明 |
|--------|------|
| 返還率 (RTP) | 必須符合聲明值，誤差 ≤ 0.5% |
| Hit Rate | 中獎頻率必須符合設計 |
| 最大賠付 | 不能超過聲明的最大賠付 |
| 遊戲規則 | 必須清晰顯示給玩家 |

### 3. 系統安全要求 (Section 4)

| 要求項 | 說明 |
|--------|------|
| 通訊安全 | TLS 1.2+ 加密 |
| 數據完整性 | 防篡改機制 |
| 訪問控制 | 角色權限管理 |
| 審計追蹤 | 完整操作日誌 |

---

## RNG 認證流程

### 階段 1: 準備

```
┌─────────────────────────────────────────────────────────────────┐
│                    RNG 認證準備階段                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 選擇 RNG 算法                                                │
│     ├── 軟體 RNG: Fortuna, ChaCha20, AES-CTR                    │
│     ├── 硬體 RNG: Intel RDRAND, TPM                             │
│     └── 混合: 軟體 + 硬體熵源                                    │
│                                                                 │
│  2. 準備文檔                                                     │
│     ├── RNG 設計文檔                                             │
│     ├── 算法說明                                                 │
│     ├── 種子管理流程                                             │
│     └── 源碼（供審查）                                           │
│                                                                 │
│  3. 收集測試數據                                                 │
│     ├── 至少 100MB 隨機數據樣本                                  │
│     └── 生成環境配置說明                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 階段 2: 測試

GLI 執行的測試項目：

#### NIST SP 800-22 測試套件

| 測試名稱 | 說明 | 通過標準 |
|---------|------|---------|
| Frequency Test | 0/1 分布均勻性 | p-value ≥ 0.01 |
| Block Frequency | 區塊內頻率 | p-value ≥ 0.01 |
| Runs Test | 連續相同位元 | p-value ≥ 0.01 |
| Longest Run | 最長連續序列 | p-value ≥ 0.01 |
| Binary Matrix Rank | 矩陣秩 | p-value ≥ 0.01 |
| DFT Test | 離散傅立葉變換 | p-value ≥ 0.01 |
| Non-overlapping Template | 非重疊模板 | p-value ≥ 0.01 |
| Overlapping Template | 重疊模板 | p-value ≥ 0.01 |
| Maurer's Universal | 通用統計 | p-value ≥ 0.01 |
| Linear Complexity | 線性複雜度 | p-value ≥ 0.01 |
| Serial Test | 序列測試 | p-value ≥ 0.01 |
| Approximate Entropy | 近似熵 | p-value ≥ 0.01 |
| Cumulative Sums | 累積和 | p-value ≥ 0.01 |
| Random Excursions | 隨機遊走 | p-value ≥ 0.01 |
| Random Excursions Variant | 隨機遊走變體 | p-value ≥ 0.01 |

### 階段 3: 程式碼審查

GLI 審查重點：

1. **種子生成**
   - 熵源質量
   - 種子更新機制
   - 種子存儲安全

2. **算法實現**
   - 正確實現標準算法
   - 無後門或弱點
   - 適當的狀態管理

3. **輸出處理**
   - 正確的數值映射
   - 無偏差縮放

---

## 技術實現

### 推薦 RNG 架構

```java
@Component
@Slf4j
public class CertifiedRNG implements RandomNumberGenerator {

    private static final int RESEED_INTERVAL = 1048576; // 每 1M 請求重新播種

    private final FortunaRNG fortuna;
    private final HardwareEntropySource hardwareEntropy;
    private final AtomicLong requestCount = new AtomicLong(0);

    @PostConstruct
    public void initialize() {
        // 初始化 Fortuna CSPRNG
        byte[] initialSeed = gatherEntropy();
        fortuna.initialize(initialSeed);
        log.info("RNG initialized with {} bytes of entropy", initialSeed.length);
    }

    /**
     * 生成隨機整數 [0, max)
     * GLI-19 要求：無偏差映射
     */
    public int nextInt(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("max must be positive");
        }

        // 使用拒絕採樣確保無偏差
        int threshold = Integer.MAX_VALUE - (Integer.MAX_VALUE % max);
        int result;
        do {
            result = next(31);
        } while (result >= threshold);

        // 定期重新播種
        if (requestCount.incrementAndGet() % RESEED_INTERVAL == 0) {
            reseed();
        }

        return result % max;
    }

    /**
     * 生成指定範圍的隨機整數 [min, max]
     */
    public int nextIntRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return min + nextInt(max - min + 1);
    }

    /**
     * 生成隨機 double [0.0, 1.0)
     */
    public double nextDouble() {
        return (next(53)) / (double) (1L << 53);
    }

    /**
     * 安全洗牌（Fisher-Yates 算法）
     */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    /**
     * 生成加權隨機選擇
     * 用於具有不同概率的遊戲結果
     */
    public int weightedChoice(double[] weights) {
        double totalWeight = Arrays.stream(weights).sum();
        double random = nextDouble() * totalWeight;

        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (random < cumulative) {
                return i;
            }
        }
        return weights.length - 1;
    }

    /**
     * 收集熵
     */
    private byte[] gatherEntropy() {
        ByteBuffer buffer = ByteBuffer.allocate(64);

        // 硬體熵源（如果可用）
        if (hardwareEntropy.isAvailable()) {
            buffer.put(hardwareEntropy.getBytes(32));
        }

        // 系統熵
        buffer.putLong(System.nanoTime());
        buffer.putLong(Runtime.getRuntime().freeMemory());
        buffer.putLong(Thread.currentThread().getId());

        // SecureRandom
        byte[] secureBytes = new byte[16];
        new SecureRandom().nextBytes(secureBytes);
        buffer.put(secureBytes);

        return buffer.array();
    }

    /**
     * 重新播種
     */
    private synchronized void reseed() {
        byte[] newEntropy = gatherEntropy();
        fortuna.reseed(newEntropy);
        log.debug("RNG reseeded with {} bytes of entropy", newEntropy.length);
    }

    private int next(int bits) {
        return fortuna.nextInt() >>> (32 - bits);
    }
}
```

### RNG 審計日誌

```java
@Service
@RequiredArgsConstructor
public class RNGAuditService {

    private final RNGAuditLogDao auditLogDao;

    /**
     * 記錄 RNG 調用（GLI-19 要求）
     */
    @Async
    public void logRNGCall(RNGAuditEvent event) {
        RNGAuditLog log = RNGAuditLog.builder()
            .gameSessionId(event.getGameSessionId())
            .gameType(event.getGameType())
            .roundId(event.getRoundId())
            .rngMethod(event.getRngMethod())
            .inputParameters(event.getInputParameters())
            .outputValue(event.getOutputValue())
            .timestamp(LocalDateTime.now())
            .serverInstanceId(getServerInstanceId())
            .build();

        auditLogDao.insert(log);
    }

    /**
     * 生成 GLI 合規報告
     */
    public RNGComplianceReport generateReport(LocalDate startDate, LocalDate endDate) {
        // 獲取期間內的 RNG 調用統計
        RNGStatistics stats = auditLogDao.getStatistics(startDate, endDate);

        // 計算分布
        Map<String, Double> distribution = calculateDistribution(startDate, endDate);

        // 檢查偏差
        List<BiasAlert> biasAlerts = checkForBias(distribution);

        return RNGComplianceReport.builder()
            .periodStart(startDate)
            .periodEnd(endDate)
            .totalCalls(stats.getTotalCalls())
            .callsByGameType(stats.getCallsByGameType())
            .distribution(distribution)
            .biasAlerts(biasAlerts)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
```

---

## 遊戲數學驗證

### RTP 計算

```java
@Service
@RequiredArgsConstructor
public class GameMathVerificationService {

    /**
     * 驗證遊戲 RTP
     *
     * GLI-19 要求：實際 RTP 與理論 RTP 偏差 ≤ 0.5%
     */
    public RTPVerificationResult verifyRTP(String gameId, LocalDate startDate, LocalDate endDate) {
        // 獲取理論 RTP
        GameMathConfig config = gameMathConfigDao.findByGameId(gameId);
        BigDecimal theoreticalRTP = config.getTheoreticalRTP();

        // 計算實際 RTP
        GameStatistics stats = gameStatsDao.getStatistics(gameId, startDate, endDate);
        BigDecimal totalWagered = stats.getTotalWagered();
        BigDecimal totalPaidOut = stats.getTotalPaidOut();

        if (totalWagered.compareTo(BigDecimal.ZERO) == 0) {
            return RTPVerificationResult.insufficientData();
        }

        BigDecimal actualRTP = totalPaidOut.divide(totalWagered, 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

        // 計算偏差
        BigDecimal deviation = actualRTP.subtract(theoreticalRTP).abs();
        boolean withinTolerance = deviation.compareTo(new BigDecimal("0.5")) <= 0;

        // 計算統計置信區間
        ConfidenceInterval ci = calculateConfidenceInterval(stats);

        return RTPVerificationResult.builder()
            .gameId(gameId)
            .periodStart(startDate)
            .periodEnd(endDate)
            .theoreticalRTP(theoreticalRTP)
            .actualRTP(actualRTP)
            .deviation(deviation)
            .withinTolerance(withinTolerance)
            .totalRounds(stats.getTotalRounds())
            .totalWagered(totalWagered)
            .totalPaidOut(totalPaidOut)
            .confidenceInterval(ci)
            .build();
    }

    /**
     * 計算 95% 置信區間
     */
    private ConfidenceInterval calculateConfidenceInterval(GameStatistics stats) {
        // 使用中心極限定理
        double n = stats.getTotalRounds().doubleValue();
        double p = stats.getTotalPaidOut().doubleValue() / stats.getTotalWagered().doubleValue();

        double se = Math.sqrt(p * (1 - p) / n);
        double z = 1.96; // 95% 置信度

        return ConfidenceInterval.builder()
            .lower(new BigDecimal(p - z * se).setScale(6, RoundingMode.HALF_UP))
            .upper(new BigDecimal(p + z * se).setScale(6, RoundingMode.HALF_UP))
            .confidence(0.95)
            .build();
    }
}
```

---

## 認證文件清單

### 必須提交的文件

| 文件類型 | 說明 | 格式 |
|---------|------|------|
| RNG 設計文檔 | 算法選擇、架構設計 | PDF |
| 源碼 | RNG 相關程式碼 | ZIP |
| 測試數據 | 100MB+ 隨機數據 | BIN |
| 遊戲數學文檔 | PAR sheet、RTP 計算 | PDF |
| 安全架構 | 系統安全設計 | PDF |
| 審計日誌格式 | 日誌結構說明 | PDF |

### PAR Sheet (Paytable and Return) 範例

```
遊戲名稱: Fortune Slots
遊戲類型: 5x3 Video Slots

符號支付表:
Symbol    | 3 Match | 4 Match | 5 Match
----------|---------|---------|--------
Wild      | 50x     | 200x    | 1000x
Scatter   | 5x      | 20x     | 100x
Symbol A  | 10x     | 50x     | 250x
Symbol B  | 5x      | 25x     | 100x
Symbol C  | 2x      | 10x     | 50x

RTP 計算:
Base Game RTP: 92.5%
Free Spins RTP: 3.5%
Bonus Feature RTP: 0.5%
Total Theoretical RTP: 96.5%

Hit Rate: 1 in 5.2 spins
Max Win: 5000x stake
Volatility: Medium-High
```

---

## 常見問題

### Q1: 認證週期多長？

**A**: 通常 4-8 週，取決於：
- 遊戲數量和複雜度
- 文檔準備充分程度
- 發現問題後的修復時間

### Q2: 認證費用？

**A**: 依項目複雜度，通常 $30,000 - $150,000 USD。

### Q3: 認證有效期？

**A**: 初始認證後，需要：
- 每次軟體更新需通知 GLI
- 重大更新需重新認證
- 年度審計（部分牌照要求）

---

## 相關文檔

- [03-06_Game_Audit_Trail.md](03-06_Game_Audit_Trail.md) - 遊戲審計追蹤
- [03-07_RTP_Monitoring.md](03-07_RTP_Monitoring.md) - RTP 監控
- [06-08_UKGC_Compliance.md](../06_Platform_Governance/06-08_UKGC_Compliance.md) - UK 合規

---

**返回**: [遊戲中心](README.md) | [iGaming 首頁](../README.md)
