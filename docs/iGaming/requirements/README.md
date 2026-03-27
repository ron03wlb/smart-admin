---
title: "Part 1: 業務需求文檔索引"
part: requirements
version: v2.0
created: 2026-03-24
---

# Part 1: 業務需求文檔

> 本目錄包含 iGaming 平台的**純業務需求**文檔，描述系統「做什麼」與「為什麼」。
> 技術實作 (「怎麼做」) 請見 [Part 2: 技術文檔](../technical/README.md)。

---

## 文檔總覽

| # | 章節 | 文件 | 核心內容 |
|---|------|------|---------|
| 0 | 平台總覽 | [00_Overview_總覽.md](./00_Overview_總覽.md) | 商業模式、市場規模、五大核心領域、玩家旅程、術語表 |
| 1 | 玩家管理 | [01_Player_Management_玩家管理.md](./01_Player_Management_玩家管理.md) | 註冊、KYC 四級、VIP、RFM 分群、問題博彩、AML |
| 2 | 錢包系統 | [02_Wallet_System_錢包系統.md](./02_Wallet_System_錢包系統.md) | CASH/BONUS/CREDIT、可下注餘額、九大交易情境、回合管理 |
| 3 | 支付系統 | [03_Payment_System_支付系統.md](./03_Payment_System_支付系統.md) | 存提款流程、PSP 路由、對帳、管轄區限制 |
| 4 | 遊戲整合 | [04_Game_Integration_遊戲整合.md](./04_Game_Integration_遊戲整合.md) | GP 接入、Token 驗證、遊戲大廳、RTP 監控 |
| 5 | 促銷與 VIP | [05_Promotions_VIP_促銷與VIP.md](./05_Promotions_VIP_促銷與VIP.md) | 紅利類型、流水要求、衝突策略、濫用偵測 |
| 6 | 風控與合規 | [06_Risk_Compliance_風控與合規.md](./06_Risk_Compliance_風控與合規.md) | 五層偵測、七維評分、風險邊界、詐欺偵測、AML |
| 7 | 治理與牌照 | [07_Governance_Licensing_治理與牌照.md](./07_Governance_Licensing_治理與牌照.md) | 多租戶四層層級、RBAC、MFA、白標、計費 |
| 8 | 代理營運 | [08_Agent_Operations_代理營運.md](./08_Agent_Operations_代理營運.md) | 信用網路、佣金模型、結算、代理門戶 |
| 9 | 分析與報表 | [09_Analytics_Reporting_分析與報表.md](./09_Analytics_Reporting_分析與報表.md) | 角色化儀表板、三路數據、報表分類、匯出 |
| 10 | 基礎設施需求 | [10_Infrastructure_基礎設施需求.md](./10_Infrastructure_基礎設施需求.md) | SLA 三級、DR 三級、效能目標、容量規劃 |
| 11 | 前端體驗 | [11_Frontend_Experience_前端體驗.md](./11_Frontend_Experience_前端體驗.md) | 多語言、行動 App、SEO、響應式、可訪問性 |
| 12 | 客戶服務 | [12_Customer_Service_客戶服務.md](./12_Customer_Service_客戶服務.md) | 360° 視圖、AI Chatbot、VIP SLA、工單管理 |
| 13 | 安全合規 | [13_Security_Compliance_安全合規.md](./13_Security_Compliance_安全合規.md) | GDPR、PCI-DSS、加密、ISO 27001 |
| 14 | 第三方整合 | [14_Third_Party_Integration_第三方整合.md](./14_Third_Party_Integration_第三方整合.md) | 供應商 SLA、Webhook 重試、API 金鑰、降級 |
| 15 | 負責任博彩 | [15_Responsible_Gambling_負責任博彩.md](./15_Responsible_Gambling_負責任博彩.md) | 自我排除、限額、可負擔性、Session 保護 |
| 16 | 事件回應 | [16_Incident_Response_事件回應.md](./16_Incident_Response_事件回應.md) | 事件分級、War Room、升級矩陣、事後檢討、韌性演練 **(v2.2 新增)** |

---

## 閱讀指南

### 按角色

| 角色 | 建議閱讀順序 |
|------|------------|
| **產品經理** | Ch0 → Ch1 → Ch2 → Ch5 → Ch6 |
| **營運經理** | Ch0 → Ch5 → Ch8 → Ch9 → Ch15 |
| **合規官** | Ch0 → Ch6 → Ch13 → Ch15 → Ch1 (KYC/AML) |
| **技術主管** | Ch0 → Ch10 → Ch2 → Ch4 → Ch14 |
| **客服主管** | Ch0 → Ch12 → Ch1 → Ch15 |
| **財務主管** | Ch0 → Ch2 → Ch3 → Ch8 → Ch9 |

### 文檔原則

- **純業務需求**: 0% 技術實作代碼
- **What/Why**: 描述系統做什麼、為什麼這樣做
- **可測試**: 每章包含成功指標與 KPI
- **跨引用**: 每章末尾指向對應的技術文檔

---

## 附錄

| # | 附錄 | 文件 | 內容 |
|---|------|------|------|
| A | 統一術語表 | [Appendix_A_Glossary_術語表.md](./Appendix_A_Glossary_術語表.md) | iGaming 業務、合規、技術全領域術語定義 |
| B | 需求追溯矩陣 | [Appendix_B_Traceability_Matrix_需求追溯矩陣.md](./Appendix_B_Traceability_Matrix_需求追溯矩陣.md) | 需求 ↔ 監管法規雙向追溯 |
| C | 效能需求彙總 | [Appendix_C_Performance_Requirements_效能需求彙總.md](./Appendix_C_Performance_Requirements_效能需求彙總.md) | 全平台回應時間、吞吐量、SLA、DR 統一視圖 |
| D | 司法管轄權衝突矩陣 | [Appendix_D_Jurisdictional_Conflict_Matrix_司法管轄權衝突矩陣.md](./Appendix_D_Jurisdictional_Conflict_Matrix_司法管轄權衝突矩陣.md) | 多牌照規則衝突決策優先級（⏳ 待法律填充） |
| E | 跨模組邊界情境矩陣 | [Appendix_E_Cross_Module_Boundary_Scenarios_跨模組邊界情境矩陣.md](./Appendix_E_Cross_Module_Boundary_Scenarios_跨模組邊界情境矩陣.md) | 模組交界處邊緣情境決策規則（✅ BS-01~12 全部已決策） |

---

## 審查報告

| 報告 | 文件 | 說明 |
|------|------|------|
| 第一性原理審查 | [AUDIT_First_Principles_Review_第一性原理審查.md](./AUDIT_First_Principles_Review_第一性原理審查.md) | v2.1 — 42 findings 全部 RESOLVED |
| 綜合審查 | [SYNTHESIS_First_Principles_Requirements_Review_第一性原理需求綜合審查.md](./SYNTHESIS_First_Principles_Requirements_Review_第一性原理需求綜合審查.md) | v2.2 — 8F + 5N + 10D + 15GAP |
| 缺漏補充規格書 | [PRD_Sprint_v2.2_Gap_Supplements_缺漏補充規格書.md](./PRD_Sprint_v2.2_Gap_Supplements_缺漏補充規格書.md) | v2.2 — 13 項 GAP/N/F 完整 PRD |
| 可配置參數註冊表 | [PRD_Configurable_Parameters_Registry_可配置參數註冊表.md](./PRD_Configurable_Parameters_Registry_可配置參數註冊表.md) | v2.2 — 717+ 參數三層覆蓋架構 + DB Schema |

---

## 文檔版本控制策略

### 版本命名規則

| 版本格式 | 觸發條件 | 範例 |
|---------|---------|------|
| `vX.0` (主版本) | 大規模重整、新增/刪除章節 | v2.0 → v3.0 |
| `vX.Y` (次版本) | 新增功能需求、跨章節修正 | v2.0 → v2.1 |
| `vX.Y.Z` (修訂) | 筆誤修正、格式調整、表格更新 | v2.1 → v2.1.1 |

### 變更管理流程

| 步驟 | 說明 | 負責人 |
|------|------|--------|
| 1 | 提出變更請求 (含影響範圍、跨章節依賴) | 任何人 |
| 2 | 影響評估 (哪些章節受影響、是否涉及 SSOT 變更) | 產品負責人 |
| 3 | 審批 (主版本需 CTO 審批、次版本需產品負責人) | 審批人 |
| 4 | 修改文檔 + 更新 YAML frontmatter 的 version 欄位 | 執行者 |
| 5 | 更新 README 版本歷史 | 執行者 |
| 6 | 通知受影響的技術文檔負責人同步更新 | 執行者 |

### SSOT 變更特殊規則

跨章節引用的 SSOT 值（如遊戲權重、風險分數邊界、Token TTL 等）變更時:
1. **必須**先修改 SSOT 所在章節
2. 所有引用章節須同步更新引用值或確認引用方式為「見 ChX §X.X」
3. 變更記錄須列出所有受影響章節

---

## 版本歷史

| 版本 | 日期 | 變更 |
|------|------|------|
| v2.0 | 2026-03-24 | 從 414 份源文檔重整為 16 份獨立業務需求文檔 |
| v2.1 | 2026-03-24 | 第一性原理審查修正 — 42 項 findings (6C+18H+12M+6L)；新增附錄 A/B/C |
| v2.2 | 2026-03-25 | 綜合審查修正 — SSOT 引用標記 (D-02~D-10)；新增附錄 D 司法管轄權衝突矩陣、附錄 E 跨模組邊界情境矩陣 |
| v2.3 | 2026-03-25 | 缺漏補充 — GAP-1~10 全部寫入章節；新增 Ch16 事件回應；F-04 FX 分攤矩陣；N-02 術語確認；PRD 規格書 |
| v2.4 | 2026-03-25 | 可配置參數 — 717+ 硬編碼參數提取為 DB 配置；三層覆蓋架構 (Global→Brand→Jurisdiction)；Ch0 §0.10 SSOT |
| v2.5 | 2026-03-25 | 邊界情境全數決策 — BS-05~12 填入決策規則；F-04/GAP-3 確認 DB 可配置；BS-01/BS-02 寫入 Ch5 |
| v2.6 | 2026-03-25 | 技術文檔同步 — 全部 17 章技術文檔升級至 v2.2；Ch0/Ch2/Ch4/Ch5/Ch6/Ch8/Ch15 補充 BS 交叉引用及 param_key；Sprint v2.2 實作指南 param_key 命名統一 |
