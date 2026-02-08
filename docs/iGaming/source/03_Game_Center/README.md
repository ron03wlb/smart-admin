# 03_Game_Center - 遊戲中心

> **模塊定位**: 遊戲整合與大廳管理
> **最後更新**: 2026-02-07

---

## 📋 模塊職責

遊戲供應商整合、大廳管理、Seamless Wallet、流水計算。

**核心功能**：
- 遊戲供應商 API 整合標準
- 遊戲大廳與分類管理
- Seamless Wallet 無縫錢包整合
- 遊戲流水計算細則

**職責邊界**：
- ✅ 包含：遊戲整合、大廳管理、遊戲流水
- ❌ 不包含：活動系統（04_Activity_Center）、財務對帳（02_Finance_Center）

---

## 📂 文檔清單

| 編號 | 文檔名稱 | 主題 | 優先級 | 狀態 |
|------|---------|------|--------|------|
| 03-01 | [Game_Integration_Standard.md](03-01_Game_Integration_Standard.md) | 遊戲整合標準 | P0 | ✅ |
| 03-02 | [Game_Lobby_Management.md](03-02_Game_Lobby_Management.md) | 遊戲大廳管理 | P1 | ✅ |
| 03-03 | [Seamless_Wallet_Analysis.md](03-03_Seamless_Wallet_Analysis.md) | Seamless Wallet 分析 | P0 | ✅ |
| 03-04 | [Turnover_Calculation.md](03-04_Turnover_Calculation.md) | 遊戲流水計算 | P0 | ✅ ⚠️ |
| 03-05 | [GLI_Certification.md](03-05_GLI_Certification.md) | GLI RNG 認證流程 | P0 | 🆕 ✅ |
| 03-06 | [Game_Audit_Trail.md](03-06_Game_Audit_Trail.md) | 遊戲審計追蹤 | P0 | 🆕 ✅ |
| 03-07 | [RTP_Monitoring.md](03-07_RTP_Monitoring.md) | RTP 實時監控 | P1 | 🆕 ✅ |
| 03-09 | [Jackpot_Pool_Reconciliation.md](03-09_Jackpot_Pool_Reconciliation.md) | Jackpot 累積獎池對帳 | P0 | 🆕 ✅ |
| 03-10 | [Live_Dealer_Reconciliation.md](03-10_Live_Dealer_Reconciliation.md) | Live Dealer 對帳 | P1 | 🆕 ✅ |

> ⚠️ **03-04 超大文檔**：建議未來拆分為 4 個子文檔

---

## 🔗 核心依賴

```text
03_Game_Center
    ↓
    ├─► 02_Finance_Center (02-06) - 錢包餘額同步
    ├─► 05_Risk_Control (05-01) - 風控驗證
    ├─► 14_Third_Party_Integration - 供應商 API
    └─► 04_Activity_Center (04-02) - 遊戲權重配置
```

---

## 📁 子目錄結構

```text
03_Game_Center/
├── README.md                          # 本索引
├── 03-01_Game_Integration_Standard.md
├── 03-02_Game_Lobby_Management.md
├── 03-03_Seamless_Wallet_Analysis.md
├── 03-04_Turnover_Calculation.md
└── seamless-wallet/                   # Seamless Wallet v3.0.0 詳細設計
    ├── README.md
    ├── core/
    ├── finance/
    └── game-logic/
```

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| 遊戲 API 標準 | 03-01 Game_Integration | §2.1 |
| Seamless Wallet 協議 | 03-03 Seamless_Wallet | §3.1 |
| 遊戲流水規則 | 03-04 Turnover_Calculation | §4.1 |
| **GLI-19 RNG 標準** | 03-05 GLI_Certification | §2.1 | 🆕 |
| **遊戲記錄 Hash Chain** | 03-06 Game_Audit_Trail | §3.1 | 🆕 |
| **RTP 偏差閾值** | 03-07 RTP_Monitoring | §4.1 | 🆕 |

---

**索引版本**: 1.0.0
**創建日期**: 2026-02-07
**維護團隊**: Game Team
