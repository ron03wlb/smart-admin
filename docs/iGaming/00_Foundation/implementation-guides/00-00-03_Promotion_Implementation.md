# 00-00-03 活動系統實作指南 (Promotion Implementation Guide)

**版本**: 1.0.0
**創建日期**: 2026-02-04
**來源**: 從 00-00_IMPLEMENTATION_GUIDE.md 拆分（§8-10）
**狀態**: 📝 PLANNED - 內容開發中

---

## 📋 文檔目的

本文檔涵蓋 iGaming 平台**活動系統**的實作指南，包括 Bonus 發放引擎、流水要求追蹤和 VIP 等級系統。

**適用對象**：
- 後端開發工程師（活動模塊）
- 產品經理（活動規則設計）
- 運營人員（活動配置）

---

## 📚 目錄

8. [建立 Bonus 發放引擎](#8-建立-bonus-發放引擎) - 📝 PLANNED
9. [設計流水要求追蹤](#9-設計流水要求追蹤) - 📝 PLANNED
10. [實作 VIP 等級系統](#10-實作-vip-等級系統) - 📝 PLANNED

---

## 8. 建立 Bonus 發放引擎

> **📝 STATUS**: PLANNED
> **預計完成**: Phase 5 後續
> **涉及模塊**: 04_Activity_Center, 01_Core_Financial_Loop

### 實作目標
[待補充：包含 Bonus 類型設計、發放規則引擎、觸發條件配置等]

### 閱讀順序

| 順序 | 文檔 | 章節 | 重點內容 |
|------|------|------|----------|
| 1 | [04-01 Activity System](../../03_Player_Journey/03-03_Activity_Bonus.md) | §3 規則引擎 | Bonus 發放邏輯 |
| 2 | [04-02 Bonus Calculation](../../03_Player_Journey/03-03_Activity_Bonus.md) | 全文 | 計算公式 |
| 3 | [01-02 Wallet Architecture](../../01_Core_Financial_Loop/01-02_Wallet_Architecture.md) | §2 Bonus 錢包 | 錢包整合 |

### 驗證清單
- [ ] Bonus 類型配置正確（首存、流水、活動等）
- [ ] 發放規則引擎運作正常
- [ ] 防重複領取機制有效
- [ ] Bonus 錢包餘額正確

### 常見陷阱
1. **重複領取**：需要分佈式鎖防並發領取
2. **流水未清零**：新 Bonus 發放時檢查未完成流水
3. **過期處理**：定時任務清理過期 Bonus

---

## 9. 設計流水要求追蹤

> **📝 STATUS**: PLANNED
> **預計完成**: Phase 5 後續
> **涉及模塊**: 04_Activity_Center, 02_Game_Operations

### 實作目標
[待補充：包含流水計算邏輯、進度追蹤、完成通知等]

### 閱讀順序

| 順序 | 文檔 | 章節 | 重點內容 |
|------|------|------|----------|
| 1 | [02-03 Turnover Calculation](../../02_Game_Operations/02-03_Turnover_Calculation.md) | §1 三層驗證 | 有效投注算法 |
| 2 | [04-01 Activity System](../../03_Player_Journey/03-03_Activity_Bonus.md) | §5 流水要求 | Wagering 計算 |
| 3 | [04-02 Bonus Calculation](../../03_Player_Journey/03-03_Activity_Bonus.md) | §4 流水追蹤 | 進度記錄 |

### 驗證清單
- [ ] 有效投注計算準確
- [ ] 流水進度實時更新
- [ ] 達成通知及時發送
- [ ] 歷史流水可追溯

### 常見陷阱
1. **遊戲權重錯誤**：不同遊戲權重不同
2. **取消投注處理**：需扣減已計算的流水
3. **跨日累計**：流水需跨天累計直到達成

---

## 10. 實作 VIP 等級系統

> **📝 STATUS**: PLANNED
> **預計完成**: Phase 5 後續
> **涉及模塊**: 01_Player_Center, 04_Activity_Center

### 實作目標
[待補充：包含等級定義、升級規則、專屬權益等]

### 閱讀順序

| 順序 | 文檔 | 章節 | 重點內容 |
|------|------|------|----------|
| 1 | [01-02 VIP & Loyalty](../../03_Player_Journey/03-02_VIP_Loyalty.md) | §2 等級體系 | VIP 定義 |
| 2 | [01-02 VIP & Loyalty](../../03_Player_Journey/03-02_VIP_Loyalty.md) | §3 升級規則 | 積分計算 |
| 3 | [01-02 VIP & Loyalty](../../03_Player_Journey/03-02_VIP_Loyalty.md) | §4 權益配置 | 專屬福利 |

### 驗證清單
- [ ] VIP 等級正確計算
- [ ] 升級觸發準確
- [ ] 降級機制運作正常
- [ ] 專屬權益生效

### 常見陷阱
1. **降級規則**：需定義保級條件和降級緩衝期
2. **權益失效**：降級時移除專屬權益
3. **積分過期**：定期清理過期積分

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-04
**維護團隊**: Product Team & Backend Team

**📚 返回**: [實作指南總索引](../00-00_IMPLEMENTATION_GUIDE_INDEX.md)
