# 02-14 Currency 匯率對帳 (Currency Reconciliation)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: P2 - 優化項

---

## 1. 概述

Currency (多幣種) 匯率對帳確保平台在多幣種環境下的匯率應用一致，並正確處理跨幣種的 GGR 計算和財務報表合併。

### 1.1 對帳目的

- **匯率一致性**: 確保所有系統使用統一的匯率來源
- **結算準確性**: 驗證跨幣種結算金額正確
- **GGR 合併**: 確保多幣種 GGR 正確換算至申報幣種
- **審計追蹤**: 提供完整的匯率應用記錄

### 1.2 業界標準

| 標準 | 適用範圍 | 關鍵條款 |
|------|---------|---------|
| **PCI DSS** | 支付安全 | Currency Handling Requirements |
| **IFRS 21** | 財務報告 | Foreign Exchange Accounting |
| **ISO 4217** | 幣種代碼 | Currency Codes Standard |

---

## 2. 匯率來源管理

### 2.1 匯率來源優先級

```yaml
匯率來源優先級:
  1. 央行官方匯率 (Central Bank Rate)
     用途: 監管申報、稅務計算
     更新頻率: 每日

  2. 銀行間市場匯率 (Interbank Rate)
     用途: 大額交易
     更新頻率: 實時

  3. 商業匯率 (Commercial Rate)
     用途: 玩家存提款
     更新頻率: 每小時

  4. 平台固定匯率 (Platform Fixed Rate)
     用途: 遊戲內換算
     更新頻率: 每日鎖定
```

### 2.2 匯率快照管理

```java
/**
 * 匯率快照服務
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateSnapshotService {

    private final ExchangeRateDao rateDao;
    private final ExchangeRateProvider rateProvider;

    /**
     * 獲取指定時間點的匯率快照
     */
    public ExchangeRateSnapshot getSnapshot(
            String fromCurrency,
            String toCurrency,
            LocalDateTime timestamp) {

        // 1. 查找最近的快照
        return rateDao.findNearestSnapshot(fromCurrency, toCurrency, timestamp)
            .getOrElse(() -> {
                // 2. 如果沒有快照，從 Provider 獲取
                BigDecimal rate = rateProvider.getHistoricalRate(
                    fromCurrency, toCurrency, timestamp);

                return ExchangeRateSnapshot.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(rate)
                    .snapshotTime(timestamp)
                    .source("PROVIDER")
                    .build();
            });
    }

    /**
     * 每日匯率快照
     */
    @Scheduled(cron = "0 0 0 * * ?")  // 每日 00:00
    public void createDailySnapshot() {
        LocalDateTime now = LocalDateTime.now();
        List<String> currencies = getCurrencyList();

        for (String from : currencies) {
            for (String to : currencies) {
                if (!from.equals(to)) {
                    BigDecimal rate = rateProvider.getCurrentRate(from, to);

                    ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.builder()
                        .fromCurrency(from)
                        .toCurrency(to)
                        .rate(rate)
                        .snapshotTime(now)
                        .source("DAILY_SNAPSHOT")
                        .build();

                    rateDao.insert(snapshot);
                }
            }
        }
    }
}
```

---

## 3. 遊戲內匯率對帳

### 3.1 遊戲匯率鎖定

```yaml
遊戲匯率鎖定規則:
  時機: 玩家進入遊戲時鎖定匯率
  有效期: 遊戲回合結束
  用途: 確保遊戲過程中匯率不變

  對帳要點:
    - 驗證鎖定時匯率來源正確
    - 驗證遊戲過程中匯率未變
    - 驗證結算使用鎖定匯率
```

### 3.2 遊戲匯率對帳查詢

```sql
-- 遊戲匯率對帳
SELECT
    gt.round_id,
    gt.player_id,
    gt.player_currency,
    gt.game_currency,

    -- 鎖定匯率
    gt.locked_rate,
    gt.locked_at,

    -- 交易金額 (玩家幣種)
    gt.bet_amount_player,
    gt.payout_amount_player,

    -- 交易金額 (遊戲幣種)
    gt.bet_amount_game,
    gt.payout_amount_game,

    -- 驗證計算
    gt.bet_amount_player / gt.bet_amount_game AS calculated_rate,
    ABS(gt.locked_rate - (gt.bet_amount_player / gt.bet_amount_game)) AS rate_variance,

    -- 對帳狀態
    CASE
        WHEN ABS(gt.locked_rate - (gt.bet_amount_player / gt.bet_amount_game)) < 0.0001
            THEN 'OK'
        ELSE 'VARIANCE'
    END AS reconciliation_status

FROM t_game_transaction gt
WHERE gt.player_currency != gt.game_currency
  AND gt.created_at >= DATE_SUB(CURDATE(), INTERVAL 1 DAY)
  AND gt.created_at < CURDATE()
HAVING reconciliation_status = 'VARIANCE';
```

---

## 4. 存提款匯率對帳

### 4.1 存提款匯率規則

```yaml
存提款匯率:

  存款:
    - 使用存款時的實時商業匯率
    - 記錄匯率來源和時間戳
    - 允許小額點差 (通常 1-2%)

  提款:
    - 使用提款申請時的匯率
    - 或使用處理時的匯率 (依配置)
    - 記錄應用的匯率

  對帳要點:
    - 驗證使用的匯率與記錄一致
    - 驗證點差在允許範圍內
    - 追蹤匯率波動對營收的影響
```

### 4.2 存提款匯率對帳服務

```java
/**
 * 存提款匯率對帳服務
 */
@Service
@RequiredArgsConstructor
public class PaymentExchangeReconciliationService {

    /**
     * 對帳單筆交易的匯率
     */
    public PaymentExchangeReconciliation reconcilePayment(Long paymentId) {
        Payment payment = paymentDao.selectById(paymentId);

        // 1. 獲取交易時的市場匯率
        ExchangeRateSnapshot marketRate = rateService.getSnapshot(
            payment.getPaymentCurrency(),
            payment.getAccountCurrency(),
            payment.getCreatedAt()
        );

        // 2. 計算實際應用的匯率
        BigDecimal appliedRate = payment.getAccountAmount()
            .divide(payment.getPaymentAmount(), 6, RoundingMode.HALF_UP);

        // 3. 計算點差
        BigDecimal spread = appliedRate.subtract(marketRate.getRate())
            .abs()
            .divide(marketRate.getRate(), 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

        // 4. 驗證點差是否在允許範圍
        BigDecimal allowedSpread = getSpreadConfig(payment.getType());
        boolean isValid = spread.compareTo(allowedSpread) <= 0;

        return PaymentExchangeReconciliation.builder()
            .paymentId(paymentId)
            .paymentCurrency(payment.getPaymentCurrency())
            .accountCurrency(payment.getAccountCurrency())
            .marketRate(marketRate.getRate())
            .appliedRate(appliedRate)
            .spreadPercent(spread)
            .allowedSpread(allowedSpread)
            .isValid(isValid)
            .build();
    }
}
```

---

## 5. GGR 跨幣種合併

### 5.1 GGR 換算規則

```yaml
GGR 跨幣種合併:

  申報幣種: 依牌照要求 (UKGC: GBP, MGA: EUR)

  換算時機:
    - 日結: 使用當日收盤匯率
    - 月結: 使用月平均匯率或月末匯率

  換算公式:
    GGR_Reporting = Σ (GGR_Currency × ExchangeRate)

  對帳要點:
    - 驗證使用的匯率來源正確
    - 驗證換算計算正確
    - 追蹤匯率波動對 GGR 的影響
```

### 5.2 GGR 匯率對帳查詢

```sql
-- GGR 跨幣種合併對帳
SELECT
    g.report_date,
    g.jurisdiction,
    g.reporting_currency,

    -- 各幣種 GGR
    g.ggr_currency,
    g.ggr_original,

    -- 換算匯率
    g.exchange_rate,
    g.rate_source,
    g.rate_date,

    -- 換算後 GGR
    g.ggr_converted,

    -- 驗證計算
    g.ggr_original * g.exchange_rate AS expected_converted,
    ABS(g.ggr_converted - (g.ggr_original * g.exchange_rate)) AS variance,

    -- 市場匯率對照
    m.rate AS market_rate,
    ABS(g.exchange_rate - m.rate) / m.rate * 100 AS rate_deviation_pct

FROM t_ggr_currency_breakdown g
LEFT JOIN t_exchange_rate_snapshot m
    ON g.ggr_currency = m.from_currency
    AND g.reporting_currency = m.to_currency
    AND DATE(m.snapshot_time) = g.rate_date
WHERE g.report_date = CURDATE() - INTERVAL 1 DAY
ORDER BY g.ggr_currency;
```

---

## 6. 匯率差異處理

### 6.1 差異分類

| 差異類型 | 原因 | 處理方式 |
|---------|------|---------|
| **來源差異** | 不同系統使用不同匯率來源 | 統一匯率來源 |
| **時間差異** | 匯率更新時間不同步 | 使用統一快照 |
| **計算差異** | 精度/四捨五入差異 | 統一計算規則 |
| **配置差異** | 點差配置不一致 | 同步配置 |

### 6.2 差異對帳表

```sql
-- 匯率差異對帳表
CREATE TABLE t_exchange_rate_reconciliation (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    reconciliation_date DATE NOT NULL,

    -- 幣種對
    from_currency       VARCHAR(3) NOT NULL,
    to_currency         VARCHAR(3) NOT NULL,

    -- 各來源匯率
    central_bank_rate   DECIMAL(18,8),
    interbank_rate      DECIMAL(18,8),
    commercial_rate     DECIMAL(18,8),
    platform_rate       DECIMAL(18,8),

    -- 差異分析
    max_deviation       DECIMAL(18,8),
    max_deviation_pct   DECIMAL(8,4),

    -- 影響評估
    affected_transactions INT,
    financial_impact    DECIMAL(18,2),

    -- 狀態
    status              VARCHAR(20) DEFAULT 'OK',
    resolution_notes    TEXT,

    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_date_pair (reconciliation_date, from_currency, to_currency),
    INDEX idx_status (status)
);
```

---

## 7. 監控與告警

### 7.1 關鍵指標

| 指標 | Prometheus 名稱 | 告警閾值 |
|------|----------------|---------|
| 匯率來源差異 | `exchange_rate_source_deviation` | > 1% |
| 匯率更新延遲 | `exchange_rate_update_delay_minutes` | > 30 分鐘 |
| 點差超限交易 | `exchange_spread_exceeded_count` | > 0 |
| GGR 換算差異 | `ggr_currency_conversion_variance` | > 0.1% |

---

## 8. 相關文檔

- [02-03 對帳系統](02-03_Reconciliation_System.md) - 核心對帳架構
- [02-11 GGR 稅務對帳](02-11_GGR_Tax_Reconciliation.md) - GGR 計算
- [02-02 支付閘道](02-02_Payment_Gateway_Integration.md) - 存提款處理

---

**返回**: [財務中心](README.md) | [iGaming 首頁](../README.md)
