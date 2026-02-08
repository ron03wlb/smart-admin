# 06-11 PAGCOR & Curacao Compliance (亞洲/離岸牌照)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

本文檔涵蓋亞洲市場（PAGCOR）和離岸牌照（Curacao）的合規要求。

---

## PAGCOR (菲律賓)

### 概述

Philippine Amusement and Gaming Corporation (PAGCOR) 是菲律賓的博彩監管機構，提供離岸和本地牌照。

### 牌照類型

| 類型 | 說明 | 適用市場 |
|------|------|---------|
| **POGO** | Philippine Offshore Gaming Operator | 海外（排除菲律賓本地） |
| **CEZA** | Cagayan Economic Zone Authority | 海外 |

### 主要要求

| 要求 | 說明 |
|------|------|
| 最低年齡 | 21 歲 |
| KYC | 必須在存款前完成 |
| 負責任博彩 | 自我排除、限額必須 |
| 資金保護 | 分離帳戶 |

### 報告要求

| 報告 | 頻率 |
|------|------|
| 月度報告 | 每月 |
| 年度審計 | 每年 |
| 財務報表 | 每季 |

---

## Curacao eGaming

### 概述

Curacao 提供相對寬鬆的離岸牌照，適合新進入市場的運營商。

### 牌照特點

| 特點 | 說明 |
|------|------|
| 申請週期 | 4-6 週 |
| 費用 | 相對較低 |
| 監管 | 較寬鬆 |
| 適用市場 | 全球（除特定限制國家） |

### 限制國家

以下國家玩家不得接受：
- 美國
- 英國
- 荷蘭
- 法國
- 澳大利亞
- 以及其他已有本地牌照要求的國家

### 基本合規要求

| 要求 | 說明 |
|------|------|
| KYC | 基本身份驗證 |
| AML | 可疑活動監控 |
| 負責任博彩 | 自我排除選項 |
| 公平遊戲 | RNG 認證 |

---

## 技術實現

```java
@Service
public class OffshoreComplianceService {

    /**
     * 檢查 Curacao 牌照限制國家
     */
    public boolean isCountryAllowed(String countryCode) {
        Set<String> blockedCountries = Set.of(
            "US", "GB", "NL", "FR", "AU", "IT", "ES", "DE"
        );
        return !blockedCountries.contains(countryCode);
    }

    /**
     * PAGCOR 年齡驗證
     */
    public boolean validatePAGCORAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= 21; // PAGCOR 要求 21 歲
    }
}
```

---

## Isle of Man

### 概述

Isle of Man 是另一個受認可的離岸牌照，監管標準介於 UK 和 Curacao 之間。

### 主要要求

| 要求 | 說明 |
|------|------|
| 玩家保護 | 類似 UK 標準 |
| RNG | 必須認證 |
| AML | 完整 AML 計劃 |
| 報告 | 月度/年度 |

---

## 牌照選擇建議

| 場景 | 建議牌照 | 原因 |
|------|---------|------|
| 進入歐洲市場 | MGA | EU 認可度高 |
| 進入英國市場 | UKGC | 必須 |
| 亞洲市場 | PAGCOR | 區域認可 |
| 快速上線 | Curacao | 審批快 |
| 多市場運營 | MGA + UKGC | 覆蓋主要市場 |

---

## 相關文檔

- [06-07_Multi_Jurisdiction_Framework.md](06-07_Multi_Jurisdiction_Framework.md) - 多牌照框架
- [06-08_UKGC_Compliance.md](06-08_UKGC_Compliance.md) - UK 合規
- [06-09_MGA_Compliance.md](06-09_MGA_Compliance.md) - Malta 合規

---

**返回**: [平台治理](README.md) | [iGaming 首頁](../README.md)
