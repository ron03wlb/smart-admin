# iGaming 文檔規範標準 (Documentation Standards)

> **版本**: 1.1.0 | **生效日期**: 2026-04-02 | **狀態**: ACTIVE

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

### 3.4 等效 XREF Header 格式（v1.1.0 新增）

為避免大量格式統一導致的高風險批次修改，以下三種 header 格式**均視為合規**，不強制轉換現有文件：

| 格式 | 範例 | 使用場景 |
|------|------|---------|
| 標準格式 | `> **XREF（交叉引用）**: ...` | 新建文件必須使用 |
| Requirements 前向連結 | `> **Related Architecture**: [標題](路徑)` | requirements/ 舊有文件 |
| Architecture 後向連結 | `> **Business Requirements**: [標題](路徑)` | architecture/ 舊有文件 |

**規則**：
- 新建文件：必須使用標準格式 `> **XREF（交叉引用）**:`
- 現有文件：僅在需要修改 header 內容（如補充缺失連結）時才統一為標準格式，否則保留原格式
- 連結路徑有效性：所有格式下的連結路徑都必須有效（validate_links.sh 通過）

### 3.5 Mermaid 豁免清單（v1.1.0 新增）

下列文件類型**豁免** Mermaid 圖表要求，在計算覆蓋率時不計入分母：

- **Index / 導航文件**：`01_Seamless_Wallet_Index.md`、模組 README.md 等以表格為主的索引文件
- **API Listing 文件**：`07_Domain_APIs.md` 等純 API 清單文件
- **Migration Log / Sprint 報告**：`phase2-week1-*.md`、`sprint4-*.md` 等施工紀錄
- **Glossary / 術語文件**：`TRANSLATION_GLOSSARY.md`、各模組術語對照表

豁免文件應在 README.md 的文件列表中以「📋 索引」或「📝 紀錄」標示，與「✅ 架構文件」區分。

### 3.6 Implementation 層範圍邊界（v1.1.0 新增）

`implementation/` 目錄維持**平層結構**（不建立 phase 子目錄），原因：
- 現有 17 個文件已建立在平層，重組會導致大量 XREF path 失效
- SmartAdmin 為模組化單體架構，實作文件與架構文件一一對應，不需要 phase 分層

**範圍邊界**：
- Phase 0–3 模組（Database、Finance、Game Integration、Risk Engine）有獨立實作文件
- Phase 4–5 模組（Activity、Analytics、Security、Frontend）的實作細節**嵌入對應的 architecture 文件**（此為有意設計，非缺失）
- Sprint 報告類文件（`sprint4-*.md`、`phase2-week1-*.md`）歸類為歷史施工紀錄，**豁免 XREF 要求**

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
