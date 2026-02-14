# 06-10 Brazil SPA Compliance (巴西 SPA 合規)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

巴西博彩市場於 2025 年 1 月正式開放，由 Secretariat of Prizes and Bets (SPA) 監管。2026 年將進入運營成熟期，有更嚴格的合規要求。

### 時間線

| 日期 | 里程碑 |
|------|--------|
| 2025-01-01 | 正式開放，牌照生效 |
| 2025-06-30 | 非法運營商封禁 |
| 2026-01-01 | .bet.br 域名強制 |
| 2026-06-30 | 完整 AML 報告上線 |

---

## 主要合規要求

### 域名要求

**2026 年強制**: 所有 B2C 運營商必須使用 `.bet.br` 域名

```java
@Configuration
public class BrazilDomainConfig {

    @Value("${brazil.domain}")
    private String brazilDomain; // example.bet.br

    /**
     * 巴西玩家必須使用 .bet.br 域名
     */
    public String getBrazilPlayerUrl() {
        return "https://" + brazilDomain;
    }
}
```

### 稅務要求

| 稅種 | 稅率 | 說明 |
|------|------|------|
| GGR 稅 | 12% | 毛博彩收入 |
| 玩家獎金稅 | 15% | 扣繳稅（>R$2,112） |

### 本地化要求

| 要求 | 說明 |
|------|------|
| 語言 | 葡萄牙語 (pt-BR) |
| 貨幣 | BRL (巴西雷亞爾) |
| 客服 | 本地時間 08:00-22:00 |
| 支付 | PIX 必須支援 |

---

## 技術實現

### 巴西合規服務

```java
@Service
@RequiredArgsConstructor
public class BrazilComplianceService {

    /**
     * 計算玩家獎金扣繳稅
     */
    public BigDecimal calculateWithholdingTax(BigDecimal winAmount) {
        BigDecimal threshold = new BigDecimal("2112");

        if (winAmount.compareTo(threshold) <= 0) {
            return BigDecimal.ZERO;
        }

        // 15% 扣繳稅
        return winAmount.multiply(new BigDecimal("0.15"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成 SPA 報告
     */
    public SPAReport generateReport(YearMonth month) {
        return SPAReport.builder()
            .reportMonth(month)
            .totalBets(countTotalBets(month))
            .grossGamingRevenue(calculateGGR(month))
            .taxDue(calculateTax(month))
            .withholdingTaxCollected(calculateWithholdingTax(month))
            .build();
    }

    /**
     * PIX 支付整合
     */
    public PIXPaymentResult processPIXDeposit(Long playerId, PIXDepositForm form) {
        // PIX 是巴西即時支付系統
        // 必須支援 PIX 作為主要存款方式
        return pixClient.processDeposit(form);
    }
}
```

---

## 報告要求

| 報告 | 頻率 | 提交期限 |
|------|------|---------|
| 月度 GGR | 每月 | 次月 15 日前 |
| 年度審計 | 每年 | 財年結束後 90 天 |
| SAR | 即時 | 24 小時內 |

---

## 相關文檔

- [06-07_Multi_Jurisdiction_Framework.md](06-07_Multi_Jurisdiction_Framework.md) - 多牌照框架
- [11-07_i18n_Localization.md](../11_Frontend_CMS/11-07_i18n_Localization.md) - 國際化

---

**返回**: [平台治理](README.md) | [iGaming 首頁](../README.md)
