# 06-09 MGA Compliance (Malta Gaming Authority 合規)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

Malta Gaming Authority (MGA) 是歐盟最受認可的博彩監管機構之一，提供 B2B 和 B2C 牌照。

### 牌照類型

| 類型 | 說明 | 年費 |
|------|------|------|
| **B2C** | 直接面向玩家 | €25,000 起 |
| **B2B** | 軟體供應商 | €25,000 起 |

---

## 主要合規要求

### 玩家保護

| 要求 | 說明 |
|------|------|
| 自我排除 | 6個月 - 終身 |
| 存款限額 | 日/週/月選項 |
| 冷靜期 | 24小時 - 6週 |
| 現實檢查 | 建議 60 分鐘 |

### KYC/AML

| 階段 | 要求 |
|------|------|
| 註冊 | 基本資料驗證 |
| €2,000 累計存款 | 身份驗證 |
| €10,000 累計存款 | 資金來源驗證 |

### 玩家保護基金

運營商必須為玩家資金提供保護：
- 分離帳戶
- 銀行擔保
- 保險

---

## 技術實現

### MGA 合規服務

```java
@Service
@RequiredArgsConstructor
public class MGAComplianceService {

    /**
     * MGA KYC 階段檢查
     */
    public KYCRequirement checkKYCRequirement(Long playerId) {
        BigDecimal totalDeposits = depositService.getTotalDeposits(playerId);

        if (totalDeposits.compareTo(new BigDecimal("10000")) >= 0) {
            return KYCRequirement.SOURCE_OF_FUNDS;
        } else if (totalDeposits.compareTo(new BigDecimal("2000")) >= 0) {
            return KYCRequirement.IDENTITY_VERIFICATION;
        }
        return KYCRequirement.BASIC;
    }

    /**
     * 生成 MGA 月度報告
     */
    public MGAMonthlyReport generateMonthlyReport(YearMonth month) {
        LocalDateTime startTime = month.atDay(1).atStartOfDay();
        LocalDateTime endTime = month.plusMonths(1).atDay(1).atStartOfDay();

        return MGAMonthlyReport.builder()
            .reportMonth(month)
            .totalPlayers(countTotalPlayers(endTime))
            .newRegistrations(countNewRegistrations(startTime, endTime))
            .selfExclusions(countSelfExclusions(startTime, endTime))
            .grossGamingRevenue(calculateGGR(startTime, endTime))
            .playerLiabilities(calculatePlayerLiabilities(endTime))
            .build();
    }
}
```

---

## 報告要求

| 報告 | 頻率 | 內容 |
|------|------|------|
| 月度報告 | 每月 | 玩家統計、GGR、合規事項 |
| 年度審計 | 每年 | 財務、系統、合規 |
| SAR | 即時 | 可疑活動報告 |

---

## 相關文檔

- [06-07_Multi_Jurisdiction_Framework.md](06-07_Multi_Jurisdiction_Framework.md) - 多牌照框架
- [05-03_KYC_AML.md](../05_Risk_Control/05-03_KYC_AML.md) - KYC/AML

---

**返回**: [平台治理](README.md) | [iGaming 首頁](../README.md)
