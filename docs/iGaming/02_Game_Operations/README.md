# 02_Game_Operations - 遊戲營運

**狀態**: 🚧 重組中（v2.0 新結構）
**創建日期**: 2026-02-03

---

## 📋 模塊職責

本模塊涵蓋遊戲提供商（GP）對接的技術實現與遊戲營運邏輯。

**核心內容**：
- 遊戲集成標準
- Seamless Wallet API（**SSOT: Token 驗證**）
- 流水計算算法（**SSOT: 有效投注算法**）
- 遊戲提供商案例

**職責邊界**：
- ✅ 包含：GP API 對接、流水計算、遊戲技術細節
- ❌ 不包含：活動規則（在 03_Promotion_System）、風控邏輯（在 04_Risk_Control）

---

## 📂 規劃文檔列表

| 文檔編號 | 文檔名稱 | 來源 | 合併說明 |
|---------|---------|------|---------|
| 02-01 | Game_Integration.md | 03-01/03-02 | 合併遊戲集成 |
| 02-02 | **Seamless_Wallet_API.md** | 03-03 + seamless-wallet/core/* | **關鍵合併**（SSOT）|
| 02-03 | **Turnover_Calculation.md** | 02-04 + 02-04-diagrams/* | **關鍵合併**（最長文件）|
| 02-04 | Game_Provider_Cases.md | seamless-wallet/game-logic/* | 遊戲案例整合 |

---

## 🔑 SSOT 定義

| 概念 | 文檔 | 章節 |
|------|------|------|
| **Token 驗證流程** | 02-02 Seamless_Wallet_API | §4.2 |
| **有效投注算法** | 02-03 Turnover_Calculation | §3.1 |
| **流水三層驗證架構** | 02-03 Turnover_Calculation | §2 |

---

## 🔗 與其他模塊的關係

- **對外提供**: 遊戲 API、流水計算接口
- **依賴模塊**: 01_Core_Financial_Loop（錢包）、04_Risk_Control（風控）
- **被依賴**: 03_Promotion_System（活動流水）

**預計完成日期**: Week 3-4 (2026-02-28)
