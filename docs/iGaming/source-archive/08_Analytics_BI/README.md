# 08_Analytics_BI - 數據分析與商業智能

> **模塊定位**: 數據驅動決策支持
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

報表系統、BI 分析、數據倉庫、指標體系。

**核心功能**：
- 營運報表（玩家、財務、遊戲）
- 商業智能儀表板
- 報表架構與數據管道
- KPI 指標體系

**職責邊界**：
- ✅ 包含：報表、BI、數據分析
- ❌ 不包含：數據管道基礎設施（10_Platform_Management）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 |
|------|---------|------|--------|
| 08-01 | [Reporting_BI.md](08-01_Reporting_BI.md) | 報表與 BI 系統 | P0 |
| 08-04 | [Reporting_Architecture.md](08-04_Reporting_Architecture.md) | 報表架構設計 | P1 |

---

## 🔗 核心依賴

```text
08_Analytics_BI
    ↓
    ├─► 10_Platform_Management (10-04) - 數據管道
    ├─► 01_Player_Center - 玩家數據
    ├─► 02_Finance_Center - 財務數據
    └─► 03_Game_Center - 遊戲數據
```

---

## 📊 核心 KPI 指標

| 指標分類 | 關鍵指標 | 計算來源 |
|---------|---------|---------|
| 玩家 | DAU, MAU, Retention | 01_Player_Center |
| 財務 | NGR, GGR, Deposits | 02_Finance_Center |
| 遊戲 | Rounds, Bets, RTP | 03_Game_Center |
| 活動 | Bonus ROI, Conversion | 04_Activity_Center |

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 報表類型定義 | 08-01 Reporting_BI | §2.1 |
| 報表架構設計 | 08-04 Reporting_Architecture | §3.1 |
| 指標計算公式 | 08-01 Reporting_BI | §4.2 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: BI Team
