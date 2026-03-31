# iGaming 文檔規範標準 (Documentation Standards)

> **版本**: 1.0.0 | **生效日期**: 2026-03-31 | **狀態**: ACTIVE

本文件是 `docs/iGaming/` 所有文件的規範 SSOT（Single Source of Truth）。
所有文件整理工作必須遵循以下規則。

---

## 1. 核心原則

### 1.1 SSOT（Single Source of Truth）原則

> **每個業務概念、技術規格只能在一個地方被「完整定義」。**

- 其他引用該概念的文件，必須使用 **XREF 標記**，不可複製貼上內容
- XREF 格式：`> 詳見：[文件標題](相對路徑)` 放置於文件頂部或相關段落前
- 違反 SSOT 原則的重複內容必須在整理過程中消除

### 1.2 受眾分層（Audience Separation）原則

| 目錄 | 受眾 | 回答問題 | 語言風格 |
|------|------|---------|---------|
| `requirements/` | PM、執行主管、合規專員 | **WHAT**（要做什麼）+ **WHY**（為何要做）| 業務語言、無程式碼 |
| `architecture/` | 系統架構師、資深工程師 | **HOW**（如何設計）| 技術語言、可含時序圖 |
| `implementation/` | 開發工程師 | **CODE**（如何實作）| 程式碼為主 |
| `source-archive/` | 歷史查閱（只讀）| 原始版本參照 | 不修改 |

**規則**：
- `requirements/` 不應含架構設計細節（class 名稱、SQL DDL、程式碼）
- `architecture/` 不應重複業務規則定義，而應引用 `requirements/` 對應文件
- `implementation/` 以程式碼和 SmartAdmin 模式為主，業務邏輯引用 `requirements/`

### 1.3 矛盾解決優先順序

當 `requirements/` 與 `architecture/` 描述不一致時：

```
requirements/ 業務規則 > architecture/ 技術設計 > implementation/ 程式碼
```

**處理步驟**：
1. 以 `requirements/` 版本為權威（業務優先）
2. 更新 `architecture/` 使其與 `requirements/` 一致
3. 在矛盾解決後，於文件頂部加上：`> ⚠️ 已於 [日期] 依需求文件修訂，詳見 [requirements/ 路徑]`

---

## 2. 命名規範

### 2.1 目錄命名

```
XX_Module_Name/    （數字前綴 + 底線 + 英文模組名，使用底線分隔單字）
```

範例：
- ✅ `01_Player_Service/`
- ✅ `09_Infrastructure/`
- ❌ `01_player-center/`（不用橫線，不用小寫）
- ❌ `01_Player Center/`（不用空格）

**編號規則**：
- 每個父目錄下，子目錄編號從 `01` 開始，不跳號
- `requirements/` 和 `architecture/` 的模組編號 **對應相同業務領域**（01 = Player, 02 = Finance, ...）
- 若需新增模組，取目前最大號 + 1

### 2.2 文件命名

```
NN_Feature_Description.md    （數字前綴 + 底線 + 功能描述）
```

範例：
- ✅ `01_Player_Lifecycle.md`
- ✅ `08_Turnover_Architecture.md`
- ❌ `player-lifecycle.md`（無數字前綴）
- ❌ `01_PlayerLifecycle.md`（應用底線分隔）

**特殊文件**（無數字前綴）：
- `README.md` — 每個目錄必須有，作為導航索引
- `STANDARDS.md` — 本文件
- `TRANSLATION_GLOSSARY.md` — 術語表

### 2.3 模組對應表

| 號碼 | 業務領域 | requirements/ | architecture/ |
|------|---------|---------------|---------------|
| 00 | 概覽 | （無） | `00_Overview/` |
| 01 | 玩家服務 | `01_Player_Experience/` | `01_Player_Service/` |
| 02 | 財務/錢包 | `02_Financial_Operations/` | `02_Finance_Service/` |
| 03 | 遊戲整合 | `03_Gaming_Operations/` | `03_Game_Integration/` |
| 04 | 活動/促銷 | `04_Promotions_VIP/` | `04_Activity_Engine/` |
| 05 | 風控/合規 | `05_Risk_Compliance/` | `05_Risk_Engine/` |
| 06 | 治理/授權 | `06_Governance_Licensing/` | `06_Platform_Core/` |
| 07 | 代理系統 | `07_Agent_Operations/` | `07_Agent_Service/` |
| 08 | 分析/BI | `08_Analytics_Operations/` | `08_Analytics_Service/` |
| 09 | 基礎設施 | `09_Infrastructure_Requirements/` | `09_Infrastructure/` |
| 10 | 平台管理 | `10_Platform_Operations/` | `10_Platform_Management/` |
| 11 | 前端 | `11_Frontend_Experience/` | `11_Frontend/` |
| 12 | 安全合規 | `12_Security_Compliance/` | `12_Security/` |
| 13 | 客戶服務 | `13_Customer_Service/` | `13_Customer_Service/` |
| 14 | 第三方整合 | `14_Integration_Standards/` | `14_Third_Party/` |
| 15 | 負責任賭博 | `15_Responsible_Gambling/` | `15_Responsible_Gambling/` |
| 16 | 指標/KPI | `16_Metrics_KPIs/` | （無獨立模組） |

---

## 3. XREF 交叉引用規範

### 3.1 格式

```markdown
> **XREF（交叉引用）**: 完整定義見 [文件標題](相對路徑)
```

### 3.2 放置位置

- **文件頂部**（H1 標題下方）：當整份文件是對另一文件的摘要或補充時
- **段落前**：當該段落討論的概念在另一文件有完整定義時

### 3.3 範例

```markdown
# Affordability 評估需求

> **XREF（交叉引用）**: 完整業務規則定義見 [Affordability 需求](../../requirements/05_Risk_Compliance/09_Affordability_Requirements.md)

本文件僅涵蓋負責任賭博脈絡下的 Affordability 執行面，技術觸發邏輯詳見上方 XREF。
```

---

## 4. README.md 規範

每個目錄的 `README.md` 必須包含：

```markdown
# [模組名稱]

## 概覽
（1-2 句描述本模組職責）

## 受眾
（本目錄文件的目標讀者）

## 文件列表

| 文件 | 描述 | 狀態 |
|------|------|------|
| [01_xxx.md](01_xxx.md) | 描述 | ✅ 最新 |

## 相關模組
（列出相關的 requirements/architecture 對應文件路徑）
```

---

## 5. 矛盾識別 Checklist

整理文件時，必須比對以下已知矛盾點：

| 矛盾類型 | 文件 A | 文件 B | 解決方式 |
|---------|--------|--------|---------|
| Affordability 規則重複 | `requirements/05_Risk_Compliance/09_Affordability_Requirements.md` | `requirements/15_Responsible_Gambling/04_Affordability_Requirements.md` | A 為 SSOT，B 改為 XREF |
| Player Protection API 重複 | `architecture/05_Risk_Engine/08_Player_Protection_API.md` | `architecture/15_Responsible_Gambling/04_Player_Protection_API.md` | 各自定義職責邊界，互相 XREF |
| Turnover 計算邏輯 | `architecture/02_Finance_Service/08_Turnover_Architecture.md` | `architecture/03_Game_Integration/04_Turnover_Calculation_Logic.md` | Finance 為 SSOT，Game Integration 加 XREF |
| KYC 等級定義 | `requirements/05_Risk_Compliance/03_KYC_AML_Requirements.md` | `architecture/05_Risk_Engine/06_KYC_Verification_API.md` | Requirements 為 SSOT |
| Player 狀態機 | `requirements/01_Player_Experience/04_Player_Lifecycle.md` | `architecture/01_Player_Service/01_Player_Lifecycle_Implementation.md` | Requirements 為 SSOT |

---

## 6. 整理禁止事項

- ❌ 不修改 `source-archive/` 下的任何文件（除 README.md）
- ❌ 不刪除業務規則（只重新組織，合併時保留所有重要內容）
- ❌ 不在 `requirements/` 中加入程式碼片段
- ❌ 不在沒有 XREF 的情況下截斷文件內容
- ❌ 不重新編號既有文件（除非解決衝突）

---

## 7. 驗收 Checklist

整理完成後，必須逐項確認：

```
[ ] STANDARDS.md 已建立（本文件）
[ ] requirements/ 無重複編號（07 衝突已修復 → 16_Metrics_KPIs）
[ ] 16_Metrics_KPIs/ 有實際內容（非空 README）
[ ] root-level 無雜亂文件（boolean migration 已移動）
[ ] source-archive/README.md 說明歸檔政策
[ ] architecture/00_Overview/06_Cross_Module_Integration.md 已建立
[ ] architecture/06_Platform_Core/ MFA 文件：8 → 4
[ ] architecture/09_Infrastructure/ Token 文件：6 → 2
[ ] architecture/02_Finance_Service/ Turnover 文件：4 → 2
[ ] Affordability 矛盾已解決（XREF 到 05_Risk_Compliance）
[ ] Player Protection 職責邊界已釐清
[ ] 所有 README.md 文件列表與實際文件一致
[ ] docs/iGaming/README.md 主導航已更新
[ ] 品質報告已產出
```
