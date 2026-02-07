# 00-00 iGaming 實作指南總索引 (Implementation Guide Index)

**版本**: 4.0.0
**創建日期**: 2026-02-04
**重組說明**: 原 IMPLEMENTATION_GUIDE.md (2,379 行) 拆分為 6 個主題文件
**狀態**: ✅ 索引完成 | 📝 內容開發中（5/20 章節完成）

---

## 📋 文檔目的

本索引為 iGaming 平台實作指南的導航中心，將 20 個實作任務按 **6 大主題** 組織，幫助開發者快速找到所需的實作步驟。

**適用對象**：
- 後端開發工程師（各模塊實作）
- 架構師（系統設計驗證）
- 產品經理（理解技術實現）

---

## 📚 六大主題文件導航

### 財務流程實作（§1-4）✅ 完整內容

**文件**: [00-00-01_Financial_Implementation.md](./implementation-guides/00-00-01_Financial_Implementation.md) (1,817 行)

**包含任務**：
1. 實作錢包系統（可下注餘額公式、扣款優先級）
2. 對接支付閘道（第三方 API、異步通知處理）
3. 實作出金風控流程（三級風控、KYC 升級）
4. 建立對帳系統（三層對帳、差異調整）

**關鍵模塊**: 01_Core_Financial_Loop, 02_Finance_Center

---

### 遊戲營運實作（§5-7）⚠️ 部分完成

**文件**: [00-00-02_Game_Integration_Implementation.md](./implementation-guides/00-00-02_Game_Integration_Implementation.md) (613 行)

**包含任務**：
5. ✅ 對接新遊戲廠商（完整內容）
6. 📝 實作 Seamless Wallet API（PLANNED）
7. 📝 設計流水計算邏輯（PLANNED）

**關鍵模塊**: 02_Game_Operations, 03_Game_Management

---

### 活動系統實作（§8-10）📝 PLANNED

**文件**: [00-00-03_Promotion_Implementation.md](./implementation-guides/00-00-03_Promotion_Implementation.md)

**包含任務**：
8. 建立 Bonus 發放引擎（Bonus 類型、發放規則、防重複領取）
9. 設計流水要求追蹤（有效投注、進度追蹤、達成通知）
10. 實作 VIP 等級系統（等級定義、升級規則、專屬權益）

**關鍵模塊**: 04_Activity_Center, 01_Player_Center

---

### 風控系統實作（§11-13）📝 PLANNED

**文件**: [00-00-04_Risk_Implementation.md](./implementation-guides/00-00-04_Risk_Implementation.md)

**包含任務**：
11. 建立風控規則引擎（Drools/LiteFlow、動態規則配置）
12. 實作欺詐檢測算法（設備指紋、行為分析、機器學習）
13. 設計代理信用管理（信用額度、風險預警、佔成模式）

**關鍵模塊**: 05_Risk_Control, 07_Agent_Center

---

### 平台治理實作（§14-17）📝 PLANNED

**文件**: [00-00-05_Governance_Implementation.md](./implementation-guides/00-00-05_Governance_Implementation.md)

**包含任務**：
14. 實作多租戶架構（租戶隔離、數據分片、租戶配置）
15. 設計 RBAC 權限系統（角色定義、權限矩陣、動態授權）
16. 建立審計日誌系統（操作日誌、變更追蹤、合規報表）
17. 實作數據加密策略（字段加密、KMS 整合、密鑰輪換）

**關鍵模塊**: 10_Platform_Management, 12_System_Security

---

### 技術基礎設施實作（§18-20）📝 PLANNED

**文件**: [00-00-06_Infrastructure_Implementation.md](./implementation-guides/00-00-06_Infrastructure_Implementation.md)

**包含任務**：
18. 設計 API 閘道（Spring Cloud Gateway、路由規則、熔斷器）
19. 建立 Blue-Green 部署（Kubernetes 配置、流量切換、回滾）
20. 實作 API 限流機制（令牌桶算法、Redis 限流、分層限流）

**關鍵模塊**: 12_Technical_Operations, Foundation 模塊

---

## 🎯 按角色推薦閱讀路徑

### 後端開發工程師（新手）

**建議閱讀順序**：
1. **財務流程** (§1-4) → 理解核心業務邏輯
2. **遊戲營運** (§5) → 學習第三方整合模式
3. **活動系統** (§8-10, 待開發) → 掌握規則引擎設計

**預估時間**：40-50 小時

---

### 架構師

**建議閱讀順序**：
1. **平台治理** (§14-17, 待開發) → 多租戶、權限、加密架構
2. **技術基礎設施** (§18-20, 待開發) → 閘道、部署、限流設計
3. **風控系統** (§11-13, 待開發) → 規則引擎、欺詐檢測架構

**預估時間**：20-30 小時（架構驗證）

---

### 產品經理

**建議閱讀順序**：
1. **財務流程** (§1, §3) → 錢包、風控業務流程
2. **活動系統** (§8-10, 待開發) → Bonus、VIP 系統邏輯
3. **遊戲營運** (§5) → 遊戲對接流程

**預估時間**：15-20 小時（跳過技術細節）

---

## 📊 實作任務總覽表

| 任務編號 | 任務名稱 | 主題文件 | 狀態 | 預估時間 |
|---------|---------|---------|------|---------|
| §1 | 實作錢包系統 | 財務流程 | ✅ 完整 | 8-10 小時 |
| §2 | 對接支付閘道 | 財務流程 | ✅ 完整 | 6-8 小時 |
| §3 | 實作出金風控流程 | 財務流程 | ✅ 完整 | 10-12 小時 |
| §4 | 建立對帳系統 | 財務流程 | ✅ 完整 | 12-15 小時 |
| §5 | 對接新遊戲廠商 | 遊戲營運 | ✅ 完整 | 8-10 小時 |
| §6 | 實作 Seamless Wallet API | 遊戲營運 | 📝 PLANNED | 10-12 小時 |
| §7 | 設計流水計算邏輯 | 遊戲營運 | 📝 PLANNED | 12-15 小時 |
| §8 | 建立 Bonus 發放引擎 | 活動系統 | 📝 PLANNED | 10-12 小時 |
| §9 | 設計流水要求追蹤 | 活動系統 | 📝 PLANNED | 8-10 小時 |
| §10 | 實作 VIP 等級系統 | 活動系統 | 📝 PLANNED | 6-8 小時 |
| §11 | 建立風控規則引擎 | 風控系統 | 📝 PLANNED | 12-15 小時 |
| §12 | 實作欺詐檢測算法 | 風控系統 | 📝 PLANNED | 15-20 小時 |
| §13 | 設計代理信用管理 | 風控系統 | 📝 PLANNED | 8-10 小時 |
| §14 | 實作多租戶架構 | 平台治理 | 📝 PLANNED | 15-20 小時 |
| §15 | 設計 RBAC 權限系統 | 平台治理 | 📝 PLANNED | 10-12 小時 |
| §16 | 建立審計日誌系統 | 平台治理 | 📝 PLANNED | 8-10 小時 |
| §17 | 實作數據加密策略 | 平台治理 | 📝 PLANNED | 12-15 小時 |
| §18 | 設計 API 閘道 | 技術基礎設施 | 📝 PLANNED | 10-12 小時 |
| §19 | 建立 Blue-Green 部署 | 技術基礎設施 | 📝 PLANNED | 12-15 小時 |
| §20 | 實作 API 限流機制 | 技術基礎設施 | 📝 PLANNED | 6-8 小時 |

**總計**：5 個完整任務（§1-5），15 個待開發任務（§6-20）

**總預估時間**：200-260 小時

---

## 📝 文檔狀態說明

### ✅ 完整內容（5 個任務）
- 包含完整的實作步驟、代碼範例、驗證清單、常見陷阱
- 可直接參考實作

### 📝 PLANNED（15 個任務）
- 包含實作目標、閱讀順序、驗證清單、常見陷阱
- 缺少詳細實作步驟和代碼範例
- 待 Phase 5 後續補充

---

## 🔗 相關文檔

### 核心參考
- [00-00_Document_Map.md](concepts/00-00_Document_Map.md) - 完整文檔地圖
- [00-01_Solution_Overview.md](concepts/00-01_Solution_Overview.md) - 解決方案總覽
- [00-04_Technology_Stack.md](concepts/00-04_Technology_Stack.md) - 技術棧說明

### 架構文檔
- [10-architecture-rules.md](../../.agent/rules/foundation/F04-architecture-rules.md) - SmartAdmin 架構規則
- [smartadmin-patterns.md](../../.claude/shared/knowledge/smartadmin-patterns.md) - SmartAdmin 設計模式

---

## 📌 使用建議

1. **新手開發者**：從財務流程（§1-4）開始，按順序閱讀
2. **有經驗開發者**：直接跳到感興趣的主題文件
3. **架構師**：重點閱讀平台治理和技術基礎設施
4. **產品經理**：閱讀業務邏輯相關章節，跳過技術細節

**遇到問題**：
- 查看原始文件 [00-00_IMPLEMENTATION_GUIDE.md](./00-00_IMPLEMENTATION_GUIDE.md)（已棄用，建議使用拆分後的文件）
- 參考對應模塊的詳細文檔（見各章節的「閱讀順序」）
- 查詢 SmartAdmin 設計模式文檔

---

**文檔版本**: 4.0.0
**最後更新**: 2026-02-04
**維護團隊**: Product Team & Backend Team

**📚 返回**: [Foundation 首頁](../README.md) | [文檔地圖](concepts/00-00_Document_Map.md)
