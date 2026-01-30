# 01-03 玩家分群與標籤 (Player Segmentation & Tagging)

> **版本**: 1.0.0
> **最後更新**: 2026-01-28
> **維護團隊**: Data Team & Product Team

---

## 📋 目錄

- [1. 系統概述](#1-系統概述-system-overview)
- [2. 玩家生命週期](#2-玩家生命週期-player-lifecycle)
- [3. RFM 模型](#3-rfm-模型-rfm-model)
- [4. 玩家標籤系統](#4-玩家標籤系統-player-tagging-system)
- [5. 自動化標籤規則](#5-自動化標籤規則-automated-tagging-rules)
- [6. 分群策略應用](#6-分群策略應用-segmentation-strategy-application)
- [7. 相關文檔](#7-相關文檔)

---

## 1. 系統概述 (System Overview)

玩家分群與標籤系統旨在通過**數據驅動的玩家畫像**,實現精準營銷、風險控制、個性化服務。系統基於玩家行為數據,自動計算分群指標並打標,為業務決策提供支持。

**核心功能**:
- **生命週期管理**: 識別新用戶、活躍用戶、沉睡用戶、流失用戶
- **RFM 分析**: 基於最近消費、消費頻率、消費金額的價值分層
- **標籤體系**: 高價值玩家、獎金獵人、對沖者、套利者等風控標籤
- **自動化規則**: 基於投注行為自動打標與更新
- **實時計算**: 玩家標籤與分群實時更新(5分鐘延遲)

---

## 2. 玩家生命週期 (Player Lifecycle)

### 2.1 生命週期階段定義

| 階段 | 定義 | 識別條件 | 業務策略 |
|------|------|---------|---------|
| **新用戶<br/>(New User)** | 註冊後 7 天內 | `DATEDIFF(NOW(), created_at) <= 7` | 首存紅利、新手引導 |
| **活躍用戶<br/>(Active User)** | 近 30 天有投注 | `last_bet_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)` | VIP 升級、專屬活動 |
| **沉睡用戶<br/>(Dormant User)** | 30-90 天未投注 | `last_bet_date BETWEEN DATE_SUB(NOW(), INTERVAL 90 DAY) AND DATE_SUB(NOW(), INTERVAL 30 DAY)` | 喚醒紅利、EDM 營銷 |
| **流失用戶<br/>(Churned User)** | 90 天以上未投注 | `last_bet_date < DATE_SUB(NOW(), INTERVAL 90 DAY)` | 大額回歸優惠、客服致電 |

### 2.2 生命週期轉化漏斗

```
註冊用戶 (100%)
    ↓ 首存轉化率 30-40%
首存用戶 (30-40%)
    ↓ 活躍留存率 40-50%
活躍用戶 (15-20%)
    ↓ VIP 轉化率 10-15%
VIP 玩家 (1.5-3%)
```

**關鍵指標**:
- **首存轉化率**: 註冊後 7 天內首存的比例
- **D7 留存率**: 註冊後第 7 天仍活躍的比例
- **D30 留存率**: 註冊後第 30 天仍活躍的比例
- **流失率**: 90 天未投注的玩家比例

---

## 3. RFM 模型 (RFM Model)

### 3.1 RFM 指標定義




### 3.2 RFM 分群矩陣

**重要客戶 (Champions)**: R=5, F=5, M=5
- **特徵**: 最近投注、高頻率、高金額
- **策略**: VIP 專屬活動、一對一服務、生日禮遇

**潛力客戶 (Potential Loyalists)**: R=4-5, F=3-4, M=3-4
- **特徵**: 近期活躍、中等頻率和金額
- **策略**: VIP 升級激勵、專屬紅利、提升頻率

**需要關注 (At Risk)**: R=2-3, F=3-5, M=3-5
- **特徵**: 曾經高價值,但最近不活躍
- **策略**: 喚醒活動、專屬回歸紅利、客服致電

**流失客戶 (Lost)**: R=1, F=1-2, M=1-3
- **特徵**: 長期未活躍、低頻率
- **策略**: 大額回歸優惠、重新激活廣告

---

## 4. 玩家標籤系統 (Player Tagging System)

### 4.1 標籤分類體系

**價值標籤 (Value Tags)**:
- `VIP_WHALE` - 鯨魚玩家 (月存款 > $50K)
- `HIGH_ROLLER` - 高額玩家 (月存款 $10K-$50K)
- `REGULAR` - 普通玩家 (月存款 $1K-$10K)
- `CASUAL` - 休閒玩家 (月存款 < $1K)

**風控標籤 (Risk Tags)**:
- `BONUS_HUNTER` - 獎金獵人 (僅玩高 RTP 遊戲,完成流水後立即提款)
- `ARBITRAGE` - 套利者 (在多平台對沖投注)
- `HEDGER` - 對沖者 (同一平台多賬戶對沖)
- `MULTI_ACCOUNT` - 多賬戶關聯 (設備指紋/IP/支付方式相同)

**行為標籤 (Behavior Tags)**:
- `SLOT_LOVER` - 老虎機愛好者 (90% 投注在老虎機)
- `LIVE_CASINO_FAN` - 真人娛樂愛好者
- `SPORTS_BETTOR` - 體育投注玩家
- `NIGHT_OWL` - 夜貓子 (投注時間集中在 22:00-06:00)

**生命週期標籤 (Lifecycle Tags)**:
- `FIRST_DEPOSIT_PENDING` - 待首存
- `ACTIVE_7D` - 7 天內活躍
- `DORMANT_30D` - 30 天沉睡
- `CHURNED_90D` - 90 天流失

---

## 5. 自動化標籤規則 (Automated Tagging Rules)

### 5.1 高價值玩家識別


### 5.2 獎金獵人檢測


### 5.3 對沖者檢測


---

## 6. 分群策略應用 (Segmentation Strategy Application)

### 6.1 營銷活動精準投放

| 玩家分群 | 活動類型 | 紅利金額 | 流水要求 | 預期 ROI |
|---------|---------|---------|---------|---------|
| VIP_WHALE | 專屬錦標賽 | $5,000+ | 10x | 150%+ |
| HIGH_ROLLER | 高額返水 | $1,000-$5,000 | 15x | 120% |
| REGULAR | 存款匹配 | $100-$1,000 | 20x | 80% |
| DORMANT_30D | 喚醒紅利 | $50-$200 | 5x | 50% |

### 6.2 風控策略分層

| 風控標籤 | 審核策略 | 提款限制 | 紅利限制 |
|---------|---------|---------|---------|
| BONUS_HUNTER | 人工審核 (100%) | 單日 $1K | 禁止參與新紅利 |
| ARBITRAGE | 凍結權益 | 單日 $500 | 禁止所有紅利 |
| MULTI_ACCOUNT | 永久封禁 | 禁止提款 | 清零紅利 |
| CLEAN | 自動審核 | 正常限額 | 正常參與 |

### 6.3 客服資源分配

| VIP 等級 | 客服類型 | 響應 SLA | 專屬經理 |
|---------|---------|---------|---------|
| Diamond | 1對1 VIP 經理 | 5 分鐘 | ✅ |
| Platinum | VIP 經理 | 15 分鐘 | ✅ |
| Gold | VIP 經理 | 30 分鐘 | ❌ |
| Silver/Bronze | 在線客服 | 2 小時 | ❌ |

---

## 7. 相關文檔

### 前置知識
- [00-03 數據模型總覽](../00_Concept_&_Analysis/00-03_Data_Model_Overview.md) - player_tags 表設計
- [00-02 行業術語表](../00_Concept_&_Analysis/00-02_Industry_Terminology.md) - RFM、生命週期等術語定義

### 核心依賴
- [01-02 VIP 與忠誠度系統](./01-02_VIP_&_Loyalty_System.md) - VIP 等級與玩家價值關聯
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 風控標籤與規則引擎

### 延伸閱讀
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - 基於分群的精準營銷
- [10-01 報表與 BI 架構](../10_Reporting_&_BI/10-01_Reporting_Architecture.md) - 玩家分群分析報表

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Data Team & Product Team
