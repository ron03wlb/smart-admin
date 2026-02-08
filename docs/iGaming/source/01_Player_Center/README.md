# 01_Player_Center - 玩家中心

> **模塊定位**: 玩家全生命週期管理
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

玩家註冊、身份驗證、分群管理、VIP 體系、提款風控。

**核心功能**：
- 玩家生命週期管理（註冊→激活→留存→流失）
- 玩家分群與標籤系統
- VIP 忠誠度體系
- 提款風控與審核

**職責邊界**：
- ✅ 包含：玩家數據、VIP、提款審核
- ❌ 不包含：支付通道（02_Finance_Center）、風控規則引擎（05_Risk_Control）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 |
|------|---------|------|--------|
| 01-01 | [Player_Lifecycle.md](01-01_Player_Lifecycle.md) | 玩家生命週期與狀態機 | P0 |
| 01-03 | [Player_Segmentation.md](01-03_Player_Segmentation.md) | 玩家分群與 RFM 模型 | P1 |
| 01-05 | [Withdrawal_Risk.md](01-05_Withdrawal_Risk.md) | 提款風控與流水驗證 | P0 |
| 01-06 | [VIP_Loyalty.md](01-06_VIP_Loyalty.md) | VIP 等級與權益體系 | P1 |

---

## 🔗 核心依賴

```text
01_Player_Center
    ↓
    ├─► 02_Finance_Center (02-06) - 錢包餘額查詢
    ├─► 04_Activity_Center (04-03) - VIP 專屬活動
    ├─► 05_Risk_Control (05-01) - 風控驗證
    └─► 06_Platform_Governance (06-02) - 權限驗證
```

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 玩家狀態機 | 01-01 Player_Lifecycle | §2.1 |
| VIP 等級計算 | 01-06 VIP_Loyalty | §3.2 |
| 提款流水驗證 | 01-05 Withdrawal_Risk | §4.1 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Player Team
