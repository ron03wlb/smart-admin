# iGaming 文檔整理問題清單 (Audit Issues)

> **建立日期**: 2026-03-31 | **更新日期**: 2026-04-02 | **目的**: 記錄整理過程中發現的問題及解決狀態
> v1.0 任務（[01]–[25]）為 2026-03-31 重整的歷史記錄（已完成）。
> v2.0 任務（[B01]–[B25]）為 2026-04-02 品質稽核發現的待處理問題（進行中）。

---

## 問題統計（v2.0 稽核 — 2026-04-02）

| 類型 | 總數 | 已解決 | 待處理 |
|------|------|--------|--------|
| Broken links | 28 | 0 | 28 |
| 缺失文件（DOCS_SPEC 宣稱存在） | 17 | 0 | 17 |
| XREF 前向連結缺失（requirements） | 4 | 0 | 4 |
| XREF 後向連結缺失（architecture） | 16 | 0 | 16 |
| Mermaid 缺失（architecture） | 7 | 0 | 7 |
| DOCS_SPEC.md 不存在 | 1 | 0 | 1 |
| **合計** | **73** | **0** | **73** |

## 問題統計（v1.0 歷史 — 2026-03-31 重整完成）

| 類型 | 總數 | 已解決 | 待處理 |
|------|------|--------|--------|
| 命名衝突 | 1 | 1 | 0 |
| 內容重複/分散 | 4 | 4 | 0 |
| 邏輯矛盾 | 5 | 5 | 0 |
| 不完整模組 | 1 | 1 | 0 |
| 文件錯位 | 2 | 2 | 0 |
| 缺失文件 | 2 | 2 | 0 |
| **合計** | **15** | **15** | **0** |

---

## 問題詳細清單

### 🔴 類型 A：命名衝突

#### A-01: requirements/ 07 號重複
- **狀態**: ⏳ 待解決 → Task [03]
- **問題**: `requirements/07_Agent_Operations/` 和 `requirements/07_Metrics_KPIs/` 編號相同
- **解決方案**: 將 `07_Metrics_KPIs` 重命名為 `16_Metrics_KPIs`（接在 15 後）
- **影響文件**: `requirements/README.md`（需更新導航）

---

### 🔴 類型 B：不完整模組

#### B-01: 07_Metrics_KPIs 只有空 README
- **狀態**: ⏳ 待解決 → Task [04]（依賴 A-01 完成後）
- **問題**: `requirements/07_Metrics_KPIs/` 只有 `README.md`，無實際 KPI 內容
- **解決方案**: 建立 `01_KPI_Definitions.md`、`02_Business_Metrics.md`，從 analytics 模組提取 KPI 定義
- **影響文件**: `requirements/16_Metrics_KPIs/`（重命名後）

---

### 🟡 類型 C：文件錯位

#### C-01: database-boolean-migration-solution.md 位於 iGaming 根目錄
- **狀態**: ⏳ 待解決 → Task [05]
- **問題**: `docs/iGaming/database-boolean-migration-solution.md` 應在 `implementation/` 下
- **解決方案**: 移至 `implementation/database-boolean-migration.md`
- **影響文件**: `docs/iGaming/README.md`（需更新引用）

#### C-02: seamless-wallet-api-spec.md 位於 architecture 根目錄
- **狀態**: ⏳ 待解決 → Task [05]
- **問題**: `docs/iGaming/architecture/seamless-wallet-api-spec.md` 應在 `02_Finance_Service/` 下
- **解決方案**: 移至 `architecture/02_Finance_Service/seamless-wallet-api-spec.md`
- **影響文件**: `docs/iGaming/architecture/README.md`（需更新引用）

---

### 🟡 類型 D：內容重複/分散

#### D-01: MFA 文件過度分散（8 個文件 → 4 個）
- **狀態**: ⏳ 待解決 → Task [08-11]
- **問題**: `architecture/06_Platform_Core/` 有 8 個 MFA 相關文件（03-10）
- **文件清單**:
  - `03_MFA_Technical_Architecture.md` + `05_MFA_Technical_Evaluation.md` → `03_MFA_Architecture.md`
  - `04_MFA_Technical.md` + `06_TOTP_WebAuthn_Implementation.md` → `04_MFA_Implementation.md`
  - `07_MFA_Compliance_Technical.md` + `08_MFA_Compliance_Validation.md` → `05_MFA_Compliance.md`
  - `09_MFA_Recovery_Implementation.md` + `10_MFA_Login_Recovery_Technical.md` → `06_MFA_Recovery.md`
- **注意**: 合併後需調整 `11_Jurisdiction_Routing_Architecture.md` 改為 `07_Jurisdiction_Routing_Architecture.md`

#### D-02: Token 文件過度分散（6 個文件 → 2 個）
- **狀態**: ⏳ 待解決 → Task [12-13]
- **問題**: `architecture/09_Infrastructure/` 有 6 個 Token 相關文件（17-22）
- **文件清單**:
  - `17_Multi_Actor_Token_Security.md` + `18_OAuth_Refresh_Token.md` + `19_Token_Validation_Architecture.md` → `17_Token_Security_Architecture.md`
  - `20_Token_Validation_Service.md` + `21_Token_Cache_Performance.md` + `22_Token_Edge_Deployment.md` → `18_Token_Operations.md`
- **注意**: 合併後 `23_Capacity_Planning_Analysis.md` → `19_Capacity_Planning_Analysis.md`

#### D-03: Turnover 計算邏輯分散（4 個文件 → 2 個）
- **狀態**: ⏳ 待解決 → Task [14-15]
- **問題**: `architecture/02_Finance_Service/` 有 4 個 Turnover 文件（08-11）
- **文件清單**:
  - `08_Turnover_Calculation_Architecture.md` + `09_Turnover_Calculation_Logic_Detail.md` → `08_Turnover_Architecture.md`
  - `10_Turnover_Implementation.md` + `11_Turnover_Flowcharts.md` → `09_Turnover_Implementation.md`
- **注意**: `architecture/03_Game_Integration/04_Turnover_Calculation_Logic.md` 加 XREF

#### D-04: source-archive 命名與現行不一致
- **狀態**: ⏳ 待解決 → Task [06]
- **問題**: `source-archive/` 使用 "Center" 命名（01_Player_Center），現行使用 "Service"（01_Player_Service）
- **解決方案**: 更新 `source-archive/README.md` 說明命名演進歷史

---

### 🔴 類型 E：邏輯矛盾

#### E-01: Affordability 需求在兩處定義
- **狀態**: ⏳ 待解決 → Task [16]
- **SSOT**: `requirements/05_Risk_Compliance/09_Affordability_Requirements.md`（風控角度）
- **重複**: `requirements/15_Responsible_Gambling/04_Affordability_Requirements.md`（RG 角度）
- **解決方案**: RG 版本縮減為摘要 + XREF 指向 Risk_Compliance 版本

#### E-02: Player Protection API 在兩處定義
- **狀態**: ⏳ 待解決 → Task [17]
- **文件 A**: `architecture/05_Risk_Engine/08_Player_Protection_API.md`（觸發機制）
- **文件 B**: `architecture/15_Responsible_Gambling/04_Player_Protection_API.md`（執行策略）
- **解決方案**: 各自明確職責，互相 XREF

#### E-03: Turnover 業務規則一致性待驗證
- **狀態**: ⏳ 待解決 → Task [18]
- **文件 A**: `requirements/03_Gaming_Operations/01_Turnover_Business_Rules.md`
- **文件 B**: `architecture/02_Finance_Service/08_Turnover_Architecture.md`（合併後）
- **解決方案**: 比對公式，以 requirements 為準修正 architecture

#### E-04: KYC 等級定義一致性待驗證
- **狀態**: ⏳ 待解決 → Task [19]
- **文件 A**: `requirements/05_Risk_Compliance/03_KYC_AML_Requirements.md`
- **文件 B**: `architecture/05_Risk_Engine/06_KYC_Verification_API.md`

#### E-05: Player Lifecycle 狀態一致性待驗證
- **狀態**: ⏳ 待解決 → Task [20]
- **文件 A**: `requirements/01_Player_Experience/04_Player_Lifecycle.md`
- **文件 B**: `architecture/01_Player_Service/01_Player_Lifecycle_Implementation.md`

---

### 🟢 類型 F：缺失文件

#### F-01: 缺少跨模組整合指南
- **狀態**: ⏳ 待解決 → Task [07]
- **問題**: 無文件描述 Risk Engine ↔ Finance Service ↔ Activity Engine 互動
- **解決方案**: 建立 `architecture/00_Overview/06_Cross_Module_Integration.md`

#### F-02: 缺少文件規範（STANDARDS.md）
- **狀態**: ✅ 已解決（Task [01]）
- **解決**: `docs/iGaming/STANDARDS.md` 已建立

---

## 整理進度追蹤

| Task | 描述 | 狀態 |
|------|------|------|
| [01] | 建立 STANDARDS.md | ✅ 已完成 |
| [02] | 建立 AUDIT_ISSUES.md（本文件） | ✅ 已完成 |
| [03] | 修復 07 命名衝突 | ✅ 已完成 |
| [04] | 補完 16_Metrics_KPIs 內容 | ✅ 已完成 |
| [05] | 移動錯位文件 | ✅ 已完成 |
| [06] | 更新 source-archive README | ✅ 已完成 |
| [07] | 建立跨模組整合指南 | ✅ 已完成 |
| [08] | 合併 MFA 架構文件 | ✅ 已完成 |
| [09] | 合併 MFA 實作文件 | ✅ 已完成 |
| [10] | 合併 MFA 合規文件 | ✅ 已完成 |
| [11] | 合併 MFA 恢復文件 | ✅ 已完成 |
| [12] | 合併 Token 安全文件 | ✅ 已完成 |
| [13] | 合併 Token 運維文件 | ✅ 已完成 |
| [14] | 合併 Turnover 計算文件 | ✅ 已完成 |
| [15] | 合併 Turnover 實作文件 | ✅ 已完成 |
| [16] | 解決 Affordability 矛盾 | ✅ 已完成 |
| [17] | 解決 Player Protection 重複 | ✅ 已完成 |
| [18] | 驗證 Turnover 一致性 | ✅ 已完成（一致，無矛盾）|
| [19] | 驗證 KYC 一致性 | ✅ 已完成（一致，無矛盾）|
| [20] | 驗證 Player Lifecycle 一致性 | ✅ 已完成（一致，無矛盾）|
| [21] | 更新所有 README.md | ✅ 已完成 |
| [22] | 更新主 README.md | ✅ 已完成 |
| [23] | 產出品質報告 | ✅ 已完成 |
| [24] | 最終驗收 | ✅ 已完成（所有 STANDARDS.md 項目通過）|
| [25] | 最終提交 | ✅ 已完成 |

---

## v2.0 Live Tracker（2026-04-02 稽核發現）

### Broken Links（28 條）

| ID | 來源文件 | 損壞連結目標 | 根本原因 | 修復 Iteration |
|----|---------|------------|---------|--------------|
| BL-01 | `requirements/15_Responsible_Gambling/04_Affordability_Requirements.md` | `../../05_Risk_Compliance/09_Affordability_Requirements.md`（×4） | 路徑層級錯誤（應為 `../`） | Iter-1 |
| BL-02 | `requirements/05_Risk_Compliance/11_Jurisdiction_Framework_Requirements.md` | `06_Platform_Core/11_Jurisdiction_Routing_Architecture.md`（×2） | 文件重新編號後 refs 未更新 | Iter-1 |
| BL-03 | `implementation/01-wallet-design.md` | `08_Turnover_Calculation_Architecture.md`（×3） | 合併後重命名 | Iter-1 |
| BL-04 | `implementation/02-game-integration.md` | `08_Turnover_Calculation_Architecture.md`（×1） | 合併後重命名 | Iter-1 |
| BL-05 | `architecture/02_Finance_Service/04_Financial_Implementation.md` | `../../../../.agent/rules/foundation/F04-architecture-rules.md`（×1） | 外部 agent rules 路徑 | Iter-1 |
| BL-06 | `architecture/03_Game_Integration/04_Turnover_Calculation_Logic.md` | `../../../../.agent/rules/foundation/F04-architecture-rules.md`（×1） | 外部 agent rules 路徑 | Iter-1 |
| BL-07 | `architecture/06_Platform_Core/03_MFA_Architecture.md` | `07_MFA_Compliance_Technical.md`（×1）、`10_MFA_Login_Recovery_Technical.md`（×1） | MFA 8→4 合併後舊檔名 | Iter-2 |
| BL-08 | `architecture/06_Platform_Core/04_MFA_Implementation.md` | `03_MFA_Technical_Architecture.md`（×1）、`08_MFA_Compliance_Validation.md`（×1）、`09_MFA_Recovery_Implementation.md`（×1） | 同上 | Iter-2 |
| BL-09 | `architecture/06_Platform_Core/05_MFA_Compliance.md` | `05_MFA_Technical_Evaluation.md`（×1）、`10_MFA_Login_Recovery_Technical.md`（×1） | 同上 | Iter-2 |
| BL-10 | `architecture/05_Risk_Engine/08_Player_Protection_API.md` | `../../15_Responsible_Gambling/04_Player_Protection_API.md`（×1） | 同層目錄路徑深度錯誤（應為 `../`） | Iter-3 |
| BL-11 | `architecture/15_Responsible_Gambling/04_Player_Protection_API.md` | `../../05_Risk_Engine/08_Player_Protection_API.md`（×1） | 同上 | Iter-3 |
| BL-12 | `architecture/02_Finance_Service/seamless-wallet-api-spec.md` | 4 條 root-relative 舊路徑 | 文件移入子目錄後 paths 未更新 | Iter-4 |

### 缺失文件（17 個）

| ID | 缺失文件路徑 | 類型 | 建立 Iteration |
|----|------------|------|--------------|
| MF-01 | `architecture/01_Player_Service/02_KYC_Progression_State_Machine.md` | architecture | Iter-9 |
| MF-02 | `architecture/01_Player_Service/03_Account_Lifecycle_Management.md` | architecture | Iter-10 |
| MF-03 | `architecture/02_Finance_Service/11_Multi_Currency_Exchange_Management.md` | architecture | Iter-11 |
| MF-04 | `architecture/03_Game_Integration/07_Provider_Error_Recovery.md` | architecture | Iter-12 |
| MF-05 | `architecture/09_Infrastructure/24_Database_Failover_Recovery.md` | architecture | Iter-13 |
| MF-06 | `architecture/09_Infrastructure/25_Cost_Allocation_Per_Tenant.md` | architecture | Iter-14 |
| MF-07 | `architecture/09_Infrastructure/26_Tenant_Migration_Procedures.md` | architecture | Iter-15 |
| MF-08 | `requirements/02_Financial_Operations/05_Dunning_Retry_Management.md` | requirements | Iter-16 |
| MF-09 | `requirements/02_Financial_Operations/06_Chargeback_Dispute_Resolution.md` | requirements | Iter-16 |
| MF-10 | `requirements/02_Financial_Operations/07_Affiliate_Payout_Audit.md` | requirements | Iter-16 |
| MF-11 | `requirements/01_Player_Experience/07_Account_Closure_Deletion.md` | requirements | Iter-16 |
| MF-12 | `requirements/03_Gaming_Operations/05_Game_Provider_Error_Handling.md` | requirements | Iter-17 |
| MF-13 | `requirements/03_Gaming_Operations/06_Seamless_Wallet_Callback_Recovery.md` | requirements | Iter-17 |
| MF-14 | `requirements/05_Risk_Compliance/12_Affiliate_Fraud_Detection.md` | requirements | Iter-17 |
| MF-15 | `requirements/06_Governance_Licensing/07_Regional_Jurisdiction_Rules.md` | requirements | Iter-17 |
| MF-16 | `requirements/15_Responsible_Gambling/05_Cross_Operator_Self_Exclusion.md` | requirements | Iter-17 |
| MF-17 | `requirements/15_Responsible_Gambling/06_Reality_Check_UX_Patterns.md` | requirements | Iter-17 |

### XREF 連結缺失

| ID | 模組 | 類型 | 修復 Iteration |
|----|------|------|--------------|
| XF-01 | `requirements/01_Player_Experience/` 部分文件 | 缺少 architecture 前向連結 | Iter-5 |
| XF-02 | `requirements/03_Gaming_Operations/` 全部 4 個文件 | 缺少前向連結 | Iter-6 |
| XF-03 | `requirements/04_Promotions_VIP/` 全部 3 個文件 | 缺少前向連結 | Iter-6 |
| XF-04 | `requirements/06–09` 部分文件 | 缺少前向連結 | Iter-7 |
| XA-01 | `architecture/09_Infrastructure/` 16 個文件 | 缺少 requirements 後向連結 | Iter-18/19 |
| XA-02 | `architecture/00_Overview/03, 04` | 缺少 requirements 後向連結 | Iter-19 |
| XA-03 | `architecture/02_Finance_Service/09_Turnover_Implementation.md` | 缺少後向連結 | Iter-20 |

### Mermaid 缺失（7 個文件）

| ID | 文件路徑 | 修復 Iteration |
|----|---------|--------------|
| MD-01 | `architecture/00_Overview/06_Cross_Module_Integration.md` | Iter-21 |
| MD-02 | `architecture/02_Finance_Service/01_Seamless_Wallet_Index.md` | Iter-21 |
| MD-03 | `architecture/09_Infrastructure/06_Common_Patterns.md` | Iter-21 |
| MD-04 | `architecture/09_Infrastructure/09_Infrastructure_Implementation.md` | Iter-21 |
| MD-05 | `architecture/09_Infrastructure/07_Domain_APIs.md` | Iter-21 |
| MD-06 | `architecture/09_Infrastructure/04_API_Design_Principles.md` | Iter-21 |
| MD-07 | `architecture/05_Risk_Engine/08_Player_Protection_API.md` | Iter-21 |

### v2.0 進度追蹤

| Task | 描述 | 狀態 |
|------|------|------|
| [B00] | 更新 STANDARDS.md（Section 3.4–3.6）+ 重建 AUDIT_ISSUES.md | ✅ 已完成 |
| [B01] | Broken Link 快速修復（BL-01 to BL-06，6 個已知目標） | ✅ 已完成 |
| [B02] | MFA Orphaned Refs 清理（BL-07 to BL-09） | ✅ 已完成 |
| [B03] | E-02 Player Protection API 雙向 XREF 修復（BL-10, BL-11） | ✅ 已完成 |
| [B04] | Seamless Wallet API Spec Path 修復（BL-12） | ✅ 已完成 |
| [B05] | Requirements XREF Audit — 01, 02 模組 | ⏳ 待處理 |
| [B06] | Requirements XREF Audit — 03, 04, 05 模組 | ⏳ 待處理 |
| [B07] | Requirements XREF Audit — 06, 07, 08, 09 模組 | ⏳ 待處理 |
| [B08] | Requirements XREF Audit — 10, 11, 12, 13, 14, 15 模組 | ⏳ 待處理 |
| [B09] | 建立 MF-01 KYC 狀態機架構文件 | ⏳ 待處理 |
| [B10] | 建立 MF-02 帳戶生命週期管理架構文件 | ⏳ 待處理 |
| [B11] | 建立 MF-03 多幣種兌換管理架構文件 | ⏳ 待處理 |
| [B12] | 建立 MF-04 GP 錯誤恢復架構文件 | ⏳ 待處理 |
| [B13] | 建立 MF-05 資料庫故障恢復架構文件 | ⏳ 待處理 |
| [B14] | 建立 MF-06 租戶成本分配架構文件 | ⏳ 待處理 |
| [B15] | 建立 MF-07 租戶遷移程序架構文件 | ⏳ 待處理 |
| [B16] | 建立 MF-08 to MF-11（金融+玩家 requirements）| ⏳ 待處理 |
| [B17] | 建立 MF-12 to MF-17（Gaming+Risk+RG requirements）| ⏳ 待處理 |
| [B18] | Architecture 後向 XREF — 09_Infrastructure 前 8 個 | ⏳ 待處理 |
| [B19] | Architecture 後向 XREF — 09_Infrastructure 後 8 個 + 00_Overview | ⏳ 待處理 |
| [B20] | Architecture 後向 XREF — 02_Finance + 03_Game_Integration | ⏳ 待處理 |
| [B21] | 補齊 7 個 Mermaid 圖（MD-01 to MD-07） | ⏳ 待處理 |
| [B22] | Implementation 層 XREF Headers + README 修正 | ⏳ 待處理 |
| [B23] | 新增 ADR-016 + ADR-017 + 更新 ADR INDEX | ⏳ 待處理 |
| [B24] | 建立 DOCS_SPEC.md + 品質閘道測量 | ⏳ 待處理 |
| [B25] | 最終品質審查 + 矛盾稽核 + Q2 品質報告 | ⏳ 待處理 |
