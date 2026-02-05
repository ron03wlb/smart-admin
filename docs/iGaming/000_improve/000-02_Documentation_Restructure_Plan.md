# iGaming 文檔重組計畫

## 狀態
- **階段**: Phase 2 - Design (進行中)
- **計畫版本**: Draft 0.1
- **最後更新**: 2026-02-05

## 背景與問題分析

### 當前狀態
- **總文件數**: 132 個 Markdown 文件
- **頂層目錄**: 25 個（模組 00-13 + 特殊目錄）
- **結構狀態**: 🚧 v2.0 重組中（未完成）

### 核心問題總結

#### Priority 1 (阻塞性問題)
1. **模組編號衝突** - 8 個模組編號被多個目錄重複使用
   - Module 00: 3 個目錄 (Concept_&_Analysis, Foundation, Navigation)
   - Module 01: 2 個目錄 (Core_Financial_Loop, Player_Center)
   - Module 02: 2 個目錄 (Finance_Center, Game_Operations)
   - Module 03: 3 個目錄 (Game_Center, Player_Journey, Promotion_System)
   - Module 04-07: 各有 2 個目錄衝突

2. **文件編號重複** - 檔案唯一性被破壞
   - `07-03-03_Common_Patterns.md`
   - `07-03-03_Token_Validation_Service.md`

3. **過深層級** - 編號深度達 5 層
   - 標準: `XX-YY_Description.md` (2 層)
   - 當前: `07-03-02-01_OAuth_Refresh_Token_Implementation.md` (4 層)

4. **SSOT 衝突** - 關鍵主題有多個來源
   - Wallet 架構: 3 個位置 (01, 02, 03 模組)
   - Player Lifecycle: 2 個位置 (01, 03 模組)
   - Risk Management: 6 個位置 (00, 01, 04, 05 模組)

#### Priority 2 (組織問題)
5. **深層嵌套** - seamless-wallet 有 4 層目錄深度
6. **非標準編號** - architecture-decisions, technical-specs 使用不同格式
7. **根目錄異常** - 中文檔案未整合：
   - `風控系統架構.md`
   - `風控系統處置.md`
8. **空目錄** - `12_Technical_Operations/` 無內容

#### Priority 3 (導航問題)
9. **斷裂連結** - 文件引用編號與實際編號不符
10. **複雜交叉引用** - 多個錢包、風控、生命週期文件
11. **版本混亂** - v1.1.0, v2.0, v3.0.0 版本指示器混合

---

## Phase 2: 設計完成 ✅

Plan agent 已完成詳細的重組方案設計（6 階段、9 天執行計畫）。

---

## Phase 3: Review 與驗證

### 關鍵文件審查結果

#### 1. 中文文件（風控系統架構.md）分析
- **版本**: v3.0.0 (2026-02-05)
- **內容**: 315 行，包含 Kafka + Flink 架構、三層處置策略
- **與 04_Risk_Control/04-01_Risk_Framework.md 關係**:
  - ✅ 中文版更新、更詳細
  - ⚠️ 英文版文件超大（>25000 tokens），可能有更多實作細節
  - 🤔 **需要決策**: 保留哪個版本作為 SSOT？

#### 2. seamless-wallet 重組案例（成功範例）
- **版本**: v3.0.0 (2026-02-03)
- **重組成果**:
  - v2.0.0: 14 個文件，扁平結構
  - v3.0.0: 10 個文件，3 層結構（core/ + game-logic/ + finance/）
  - 查找效率提升 50%，重複內容從 15% → 0%
- ✅ **可複製**: 這是一個成功的扁平化範例，可供其他模組參考

#### 3. Player Lifecycle 重複確認
- **01_Player_Center/01-01_Player_Lifecycle.md**: v1.0.0, 100 行，簡潔版
- **03_Player_Journey/03-01_Player_Lifecycle.md**: 超大（>25000 tokens），詳細版
- 🤔 **需要決策**: 保留詳細版還是簡潔版？或合併？

### Plan Agent 方案總結

**6 階段重組計畫**:
- **Phase 1** (Day 1-2): 基準建立（斷鏈掃描、編號驗證）
- **Phase 2** (Day 3-4): Module 00 整合（3 個目錄 → 1 個）
- **Phase 3** (Day 5): Module 03 整合（3 個目錄 → 2 個）
- **Phase 4** (Day 6): 中文文件整合（風控系統架構.md）
- **Phase 5** (Day 7-8): 扁平化與編號標準化（消除 5 層深度）
- **Phase 6** (Day 9): 歸檔清理與最終驗證

**核心價值**:
- ✅ 100% 模塊編號唯一（24 個目錄 → 14 個）
- ✅ 100% 文件編號規範（最大 3 層深度）
- ✅ 100% SSOT 合規（消除重複內容）
- ✅ 統一版本管理（所有活躍文件 → v4.0.0）

---

## Phase 3: 關鍵決策確認 ✅

已確認用戶決策：
1. ✅ **中文風控文件**: 翻譯成英文並整合到 05_Risk_Control/
2. ✅ **Player Lifecycle**: 保留詳細版（03_Player_Journey），刪除簡潔版
3. ✅ **執行速度**: 謹慎執行（9 天，分階段驗證）
4. ✅ **空目錄**: 刪除 12_Technical_Operations/

---

## Phase 4: 最終重組計畫

### 執行目標

**核心問題解決**:
- ✅ 解決 8 個模組編號衝突（00-07）
- ✅ 消除文件編號重複（07-03-03 x2）
- ✅ 扁平化 5 層深度編號（最大 3 層）
- ✅ 建立 SSOT（錢包、風控、玩家生命週期）
- ✅ 統一版本號（v4.0.0）
- ✅ 修復所有斷裂連結

### 重組策略總覽

#### 模組整合方案（24 目錄 → 14 模組）

| 當前目錄 | 新模組編號 | 動作 | 理由 |
|---------|----------|------|------|
| **Module 00** | | | |
| 00_Concept_&_Analysis | 00_Foundation | 合併 | 基礎概念統一 |
| 00_Foundation | 00_Foundation | 保留（主目錄） | 作為 SSOT |
| 00_Navigation | 00_Foundation | 合併 | 導航功能整合 |
| **Module 01-03** | | | |
| 01_Core_Financial_Loop | 刪除 | 遷移內容到 02, 03 | v2.0 重組未完成 |
| 01_Player_Center | 01_Player_Center | 保留 | 模組主體 |
| 02_Finance_Center | 02_Finance_Center | 保留 | 模組主體 |
| 02_Game_Operations | 03_Game_Center | 合併 | 遊戲相關整合 |
| 03_Game_Center | 03_Game_Center | 保留（主目錄） | 模組主體 |
| 03_Player_Journey | 01_Player_Center | 合併 | 玩家相關整合 |
| 03_Promotion_System | 04_Activity_Center | 合併 | 活動相關整合 |
| **Module 04-05** | | | |
| 04_Activity_Center | 04_Activity_Center | 保留 | 模組主體 |
| 04_Risk_Control | 05_Risk_Control | 重編號 | 避免衝突 |
| 05_Platform_Governance | 06_Platform_Governance | 重編號 | 避免衝突 |
| 05_Risk_Management | 05_Risk_Control | 合併 | 風控整合 |
| **Module 06-07** | | | |
| 06_Agent_Center | 07_Agent_Center | 重編號 | 避免衝突 |
| 06_Analytics_Operations | 10_Analytics_BI | 合併 | 分析整合 |
| 07_Platform_Management | 08_Platform_Management | 重編號 | 避免衝突 |
| 07_Technical_Infrastructure | 09_Technical_Infrastructure | 重編號 | 避免衝突 |
| **Module 08-13** | | | |
| 08_Frontend_CMS | 11_Frontend_CMS | 重編號 | 避免衝突 |
| 09_System_Security | 12_System_Security | 重編號 | 避免衝突 |
| 10_Reporting_&_BI | 10_Analytics_BI | 合併（主目錄） | 分析整合 |
| 11_Customer_Service | 13_Customer_Service | 重編號 | 避免衝突 |
| 12_Technical_Operations | 刪除 | 空目錄清理 | 用戶決策 |
| 13_Third_Party_Integration | 14_Third_Party_Integration | 重編號 | 避免衝突 |

**最終結果**: 24 個目錄 → 14 個唯一模組（00-14）

#### SSOT 整合方案

| 主題 | SSOT 位置 | 處理方案 |
|------|----------|---------|
| **Wallet 架構** | 02_Finance_Center/02-06_Unified_Wallet_Model.md | - 刪除 01_Core_Financial_Loop/01-02<br/>- 將 03_Game_Center/03-03 改為 Redirect Stub |
| **Player Lifecycle** | 01_Player_Center/01-01_Player_Lifecycle.md | - 將 03_Player_Journey/03-01 內容合併過來<br/>- 刪除簡潔版（用戶決策） |
| **風控系統** | 05_Risk_Control/05-01_Risk_System_Architecture.md | - 翻譯中文版（風控系統架構.md）並整合<br/>- 合併 04_Risk_Control/04-01 + 05_Risk_Management/ 內容 |

#### 中文文件整合方案（用戶決策）

**源文件**: `風控系統架構.md` (v3.0.0, 315 行)
**目標**: `05_Risk_Control/05-01_Risk_System_Architecture.md` (英文 SSOT)

**執行步驟**:
1. 人工翻譯中文版（保留完整技術細節）
2. 對比英文版（04_Risk_Control/04-01_Risk_Framework.md）
3. 整合唯一內容到新 SSOT
4. 歸檔原中文版到 `archive/legacy-cn/風控系統架構_v3.0.0.md`

**工作量估算**: 2-3 天（翻譯 + 整合 + 驗證）

### 六階段執行計畫（9 天）

#### Phase 1: 基準建立與工具準備 (Day 1-2)

**目標**: 建立重組前的基準數據，開發自動化工具

**執行步驟**:
1. **斷鏈掃描** (Day 1 上午)
   ```bash
   ./scripts/scan-broken-links.sh > baseline-broken-links.txt
   ```
   - 記錄當前斷鏈數量作為基準
   - 生成完整鏈接清單

2. **編號驗證** (Day 1 下午)
   ```bash
   ./scripts/validate-file-numbering.sh > baseline-numbering-issues.txt
   ```
   - 記錄所有編號衝突
   - 生成重複編號清單

3. **工具開發** (Day 2)
   - 開發 `update-links.sh`（自動化鏈接更新）
   - 開發 `normalize-versions.sh`（版本號統一）
   - 測試 Dry Run 模式

**產出**:
- ✅ `baseline-broken-links.txt` - 基準斷鏈報告
- ✅ `baseline-numbering-issues.txt` - 編號問題報告
- ✅ `scripts/update-links.sh` - 鏈接更新工具
- ✅ `scripts/normalize-versions.sh` - 版本統一工具

**驗證檢查點**:
- [ ] 基準報告生成成功
- [ ] 工具測試通過（Dry Run）
- [ ] Git 分支創建：`feature/igaming-docs-restructure`

---

#### Phase 2: Module 00 整合 (Day 3)

**目標**: 合併 Module 00 三個目錄 → 單一 00_Foundation

**執行步驟**:
1. **內容遷移**
   ```bash
   # 合併策略：00_Foundation 作為主目錄
   git mv 00_Concept_&_Analysis/* 00_Foundation/concepts/
   git mv 00_Navigation/* 00_Foundation/navigation/
   ```

2. **編號統一**
   - 所有文件統一使用 `00-XX-` 前綴
   - 更新 `00_Foundation/README.md` 索引

3. **鏈接更新**
   ```bash
   ./scripts/update-links.sh --module 00 --dry-run
   # 確認無誤後執行
   ./scripts/update-links.sh --module 00
   ```

**產出**:
- ✅ 單一 `00_Foundation/` 目錄
- ✅ 12 個文件統一編號
- ✅ 更新所有引用鏈接

**驗證檢查點**:
- [ ] 編號無衝突（`validate-file-numbering.sh`）
- [ ] 斷鏈檢查（`scan-broken-links.sh`）
- [ ] Git Commit: `refactor(docs): consolidate Module 00 into single Foundation`

---

#### Phase 3: Module 01-03 整合 (Day 4-5)

**目標**: 解決 01, 02, 03 模組的編號衝突

**Day 4: Module 01 + 03 (Player & Game)**

1. **合併 03_Player_Journey → 01_Player_Center**
   ```bash
   # 保留詳細版 Player Lifecycle（用戶決策）
   git mv 03_Player_Journey/03-01_Player_Lifecycle.md \
          01_Player_Center/01-01_Player_Lifecycle.md
   git mv 03_Player_Journey/03-02_VIP_Loyalty.md \
          01_Player_Center/01-02_VIP_Loyalty.md
   # 重編號其他文件
   ```

2. **合併 02_Game_Operations → 03_Game_Center**
   ```bash
   git mv 02_Game_Operations/* 03_Game_Center/
   # 重編號為 03-XX-
   ```

3. **刪除 01_Core_Financial_Loop**
   - 檢查內容是否已遷移到 02_Finance_Center
   - 刪除目錄

**Day 5: Module 02 (Finance) + SSOT 建立**

1. **扁平化 seamless-wallet**
   ```bash
   # seamless-wallet 已是成功範例，保持不動
   # 但檢查是否需要重編號
   ```

2. **建立 Wallet SSOT**
   - 確認 `02-06_Unified_Wallet_Model.md` 為 SSOT
   - 創建 Redirect Stub：
     ```markdown
     # 03-03 Seamless Wallet Analysis

     ⚠️ **已遷移**: 本文檔內容已整合到統一錢包模型。

     請參考：[02-06 統一錢包模型](../../02_Finance_Center/02-06_Unified_Wallet_Model.md)
     ```

**產出**:
- ✅ 01_Player_Center（整合完成）
- ✅ 02_Finance_Center（SSOT 建立）
- ✅ 03_Game_Center（整合完成）
- ✅ 刪除 01_Core_Financial_Loop

**驗證檢查點**:
- [ ] Player Lifecycle 內容完整（詳細版）
- [ ] Wallet SSOT 標記清晰
- [ ] 編號無衝突
- [ ] Git Commit: `refactor(docs): consolidate Modules 01-03, establish SSOT`

---

#### Phase 4: 中文文件整合與風控重組 (Day 6)

**目標**: 翻譯中文風控文件並整合到英文 SSOT

**執行步驟**:

1. **準備工作** (上午)
   - 創建新文件：`05_Risk_Control/05-01_Risk_System_Architecture.md`
   - 讀取源文件：
     - `風控系統架構.md` (v3.0.0, 315 行)
     - `04_Risk_Control/04-01_Risk_Framework.md` (超大文件，分段讀取)

2. **內容整合** (下午)
   - 翻譯中文版關鍵章節（保留技術細節）：
     - §8 異步風控架構（Kafka + Flink）
     - §9 實時監控 vs 實時阻斷
     - 變更日誌（v3.0.0 規則獨立觸發模式）
   - 對比英文版，整合唯一內容
   - 統一術語表（中英對照）

3. **風控模組重組**
   ```bash
   # 合併 05_Risk_Management → 05_Risk_Control (已重編號為 05)
   git mv 05_Risk_Management/05-02_Agent_Credit_Risk.md \
          05_Risk_Control/05-03_Agent_Credit_Risk.md
   git mv 05_Risk_Management/05-04_Risk_Proposal_Workflow.md \
          05_Risk_Control/05-04_Risk_Proposal_Workflow.md

   # 歸檔中文原文
   git mv 風控系統架構.md archive/legacy-cn/風控系統架構_v3.0.0.md
   git mv 風控系統處置.md archive/legacy-cn/風控系統處置_v3.0.0.md
   ```

**產出**:
- ✅ `05_Risk_Control/05-01_Risk_System_Architecture.md`（英文 SSOT）
- ✅ 合併風控模組（04 + 05 → 05）
- ✅ 歸檔中文原文

**驗證檢查點**:
- [ ] 翻譯內容技術準確性（人工審核）
- [ ] 風控模組編號統一
- [ ] 中文文件已歸檔
- [ ] Git Commit: `refactor(docs): integrate Chinese risk docs, consolidate risk modules`

---

#### Phase 5: 扁平化與編號標準化 (Day 7-8)

**目標**: 消除 4-5 層深度編號，統一為 3 層上限

**Day 7: Technical Infrastructure 重組**

1. **分析 07_Technical_Infrastructure (19 文件)**
   - 識別所有 4-5 層編號文件
   - 解決 `07-03-03` 重複編號

2. **扁平化策略**
   ```bash
   # 範例：5 層 → 3 層
   # 07-03-02-01_OAuth_Refresh_Token_Implementation.md
   # →
   # 09-12_OAuth_Refresh_Token_Implementation.md (重編號到 Module 09)

   # 解決重複編號
   # 07-03-03_Common_Patterns.md → 09-13_Common_Patterns.md
   # 07-03-03_Token_Validation_Service.md → 09-14_Token_Validation_Service.md
   ```

3. **Frontend CMS 扁平化**
   ```bash
   # 將 08_Frontend_CMS/01-localization/ 扁平化
   git mv 08_Frontend_CMS/01-localization/01-i18n.md \
          11_Frontend_CMS/11-03_i18n.md
   # 重編號所有子目錄文件
   ```

**Day 8: 其他模組標準化**

1. **Module 06-14 重編號**
   - 按照模組映射表統一重編號
   - 確保所有文件符合 `XX-YY[-ZZ]_Name.md` 格式

2. **Special Directories 整合**
   ```bash
   # architecture-decisions/ 統一命名
   git mv architecture-decisions/012-async-risk-proposal-system.md \
          architecture-decisions/ADR-012-async-risk-proposal-system.md

   # technical-specs/ 整合到對應模組
   git mv technical-specs/P1-important/07-withdrawal-risk-correlation.md \
          05_Risk_Control/05-05_Withdrawal_Risk_Correlation.md
   ```

3. **刪除空目錄**（用戶決策）
   ```bash
   git rm -r 12_Technical_Operations/
   ```

**產出**:
- ✅ 所有文件編號深度 ≤ 3 層
- ✅ 消除編號重複
- ✅ 刪除空目錄
- ✅ Special directories 標準化

**驗證檢查點**:
- [ ] 編號深度檢查（最大 3 層）
- [ ] 無重複編號
- [ ] 所有文件符合命名規範
- [ ] Git Commit: `refactor(docs): flatten deep nesting, standardize numbering`

---

#### Phase 6: 歸檔清理與最終驗證 (Day 9)

**目標**: 歸檔整理，統一版本號，最終驗證

**執行步驟**:

1. **歸檔清理** (上午)
   ```bash
   # 更新歸檔索引
   # docs/archive/INDEX.md

   # 新增章節：
   ## v3.0.0 Legacy Documentation (2026-02-05)

   - [風控系統架構 v3.0.0](legacy-cn/風控系統架構_v3.0.0.md)
   - [風控系統處置 v3.0.0](legacy-cn/風控系統處置_v3.0.0.md)
   - [Seamless Wallet v2.0.0](legacy/seamless-wallet-v2/)
   ```

2. **版本號統一** (上午)
   ```bash
   ./scripts/normalize-versions.sh --version v4.0.0
   ```
   - 所有活躍文件版本號統一為 v4.0.0
   - 保留歸檔文件原版本號

3. **最終驗證** (下午)
   ```bash
   # 1. 斷鏈檢查
   ./scripts/scan-broken-links.sh > final-broken-links.txt
   diff baseline-broken-links.txt final-broken-links.txt

   # 2. 編號驗證
   ./scripts/validate-file-numbering.sh > final-numbering-check.txt
   # 期望：0 errors

   # 3. 版本號檢查
   grep -r "版本.*: v[123]\." docs/iGaming --include="*.md" -l
   # 期望：僅歸檔文件

   # 4. 鏈接完整性測試
   ./scripts/validate-links.sh
   ```

4. **生成遷移報告**
   ```bash
   ./scripts/generate-migration-report.sh > docs/iGaming/archive/v4.0.0/RESTRUCTURE_REPORT.md
   ```

**產出**:
- ✅ 歸檔索引更新
- ✅ 統一版本號（v4.0.0）
- ✅ 最終驗證報告
- ✅ 遷移完成報告

**驗證檢查點**:
- [ ] 斷鏈數量 ≤ 基準值
- [ ] 編號衝突 = 0
- [ ] 深度超標 = 0
- [ ] 版本號統一（活躍文件 = v4.0.0）
- [ ] Git Commit: `docs(igaming): complete v4.0.0 restructure`
- [ ] 創建 PR：`feature/igaming-docs-restructure → master`

---

### 實施後維護策略

#### 文檔治理規則（v4.0.0 起生效）

**Pre-commit Hook**（自動檢查）:
```bash
#!/bin/bash
# .git/hooks/pre-commit

# 檢查新增文件是否符合編號規範
git diff --cached --name-only --diff-filter=A | grep "docs/iGaming.*\.md" | while read file; do
    filename=$(basename "$file")

    # 檢查格式：XX-YY-ZZ_File_Name.md
    if ! echo "$filename" | grep -qE "^[0-9]{2}-[0-9]{2}(-[0-9]{2})?_.*\.md$"; then
        echo "❌ 文件名不符合規範: $filename"
        exit 1
    fi

    # 檢查編號衝突
    number=$(echo "$filename" | sed 's/_.*//g')
    existing=$(find docs/iGaming -name "${number}_*.md" | wc -l)
    if [ $existing -gt 0 ]; then
        echo "❌ 編號衝突: $number 已被使用"
        exit 1
    fi
done
```

#### 季度文檔審計

**頻率**: 每季度（Q1, Q2, Q3, Q4）

**審計清單**:
- [ ] 運行 `scan-broken-links.sh`，修復斷鏈
- [ ] 運行 `validate-file-numbering.sh`，檢查編號規範
- [ ] 檢查 SSOT 標記有效性
- [ ] 檢查是否有新的重複內容
- [ ] 更新歸檔索引

---

### 關鍵成功因素

✅ **自動化工具覆蓋率 ≥ 80%**
- 鏈接掃描、編號驗證、版本統一全部自動化
- 減少人工錯誤，提升執行效率

✅ **Git 歷史完整保留**
- 所有文件移動使用 `git mv`
- 可追溯每個文件的完整演進歷史

✅ **分階段獨立驗證**
- 每個 Phase 完成後立即驗證
- 發現問題可快速回滾單個 Phase

✅ **多人交叉審核**
- 每個 Phase 至少 2 人 Code Review
- SSOT 決策需架構團隊集體決策

---

### 風險評估與緩解

| 風險類別 | 可能性 | 影響 | 風險等級 | 緩解措施 |
|---------|-------|------|---------|---------|
| **大規模鏈接破壞** | 🟡 中 | 🔴 高 | 🔴 高 | 自動化鏈接掃描+替換，分階段驗證 |
| **Git 歷史丟失** | 🟢 低 | 🔴 高 | 🟡 中 | 強制使用 `git mv`，禁止手動複製刪除 |
| **中文翻譯錯誤** | 🟡 中 | 🟡 中 | 🟡 中 | 人工審核翻譯，保留原文歸檔 |
| **並行編輯衝突** | 🔴 高 | 🟡 中 | 🔴 高 | 文檔凍結期，禁止新增文件 |

**回滾策略**:
```bash
# Level 1: 單個 Phase 回滾（推薦）
git reset --hard feature/igaming-docs-restructure-phase3
git push --force origin feature/igaming-docs-restructure

# Level 2: 完整重組回滾
git checkout master
git branch -D feature/igaming-docs-restructure
```

---

### 預期成果

| 指標 | 重組前 | 重組後（目標） | 改進 |
|------|--------|--------------|------|
| **模塊數量** | 24 個目錄 | 14 個唯一模塊 | -42% |
| **編號衝突** | 8 個模塊 | 0 個 | 100% 消除 |
| **最大編號深度** | 5 層 | 3 層 | -40% |
| **SSOT 覆蓋率** | ~60% | 100% | +40% |
| **斷鏈數量** | 基準值 | ≤ 基準值 | 維持或改善 |
| **版本統一** | v1-v3 混合 | v4.0.0 | 100% 統一 |
| **查找效率** | 3-5 分鐘 | 1-2 分鐘 | -50% |
| **維護成本** | 高（重複內容） | 低（SSOT） | -60% |

---

## 關鍵文件清單

實施時最關鍵的 5 個文件/工具：

1. **`scripts/scan-broken-links.sh`** - 核心驗證工具，貫穿所有 6 個 Phase
2. **`scripts/update-links.sh`** - 自動化鏈接更新，消除 80% 人工工作量
3. **`link-mapping.json`** - 60 個文件移動的完整映射表（Phase 1 生成）
4. **`05_Risk_Control/05-01_Risk_System_Architecture.md`** - SSOT 整合最複雜案例（中文翻譯）
5. **`docs/iGaming/archive/INDEX.md`** - 歸檔索引，確保歷史可追溯

---

---

## 附錄 A: 腳本實作詳細規格

### A.1 scan-broken-links.sh（斷鏈掃描工具）

**功能**: 掃描所有 Markdown 文件中的內部鏈接，檢測斷鏈

**完整實作**:
```bash
#!/bin/bash
# scripts/scan-broken-links.sh
# 用途：掃描 docs/iGaming/ 下所有 Markdown 文件的內部鏈接

set -e

DOCS_DIR="docs/iGaming"
OUTPUT_FILE="${1:-broken-links-report.txt}"
BROKEN_COUNT=0

echo "=== iGaming 文檔斷鏈掃描報告 ===" > "$OUTPUT_FILE"
echo "掃描時間: $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# 查找所有 .md 文件
find "$DOCS_DIR" -name "*.md" | while read -r file; do
    echo "正在掃描: $file"

    # 提取所有相對路徑鏈接（格式：[text](path)）
    grep -oP '\[.*?\]\(\K[^)]+(?=\))' "$file" | while read -r link; do
        # 跳過外部鏈接和錨點鏈接
        if [[ "$link" =~ ^https?:// ]] || [[ "$link" =~ ^# ]]; then
            continue
        fi

        # 解析相對路徑
        link_path=$(dirname "$file")/"$link"

        # 檢查文件是否存在
        if [[ ! -f "$link_path" ]]; then
            echo "❌ 斷鏈: $file -> $link" >> "$OUTPUT_FILE"
            ((BROKEN_COUNT++))
        fi
    done
done

echo "" >> "$OUTPUT_FILE"
echo "總計斷鏈數量: $BROKEN_COUNT" >> "$OUTPUT_FILE"

if [ $BROKEN_COUNT -eq 0 ]; then
    echo "✅ 未發現斷鏈"
else
    echo "⚠️ 發現 $BROKEN_COUNT 個斷鏈，詳見報告: $OUTPUT_FILE"
fi

exit 0
```

**使用方式**:
```bash
# 生成基準報告
./scripts/scan-broken-links.sh baseline-broken-links.txt

# Phase 驗證
./scripts/scan-broken-links.sh phase2-broken-links.txt

# 對比差異
diff baseline-broken-links.txt phase2-broken-links.txt
```

---

### A.2 validate-file-numbering.sh（編號驗證工具）

**功能**: 檢查文件編號規範性、重複編號、深度超標

**完整實作**:
```bash
#!/bin/bash
# scripts/validate-file-numbering.sh
# 用途：驗證 docs/iGaming/ 文件編號規範

set -e

DOCS_DIR="docs/iGaming"
OUTPUT_FILE="${1:-numbering-issues-report.txt}"
ERROR_COUNT=0

echo "=== iGaming 文檔編號驗證報告 ===" > "$OUTPUT_FILE"
echo "驗證時間: $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# 定義規範：XX-YY[-ZZ]_Name.md
PATTERN='^[0-9]{2}-[0-9]{2}(-[0-9]{2})?_.*\.md$'

# 查找所有 .md 文件（排除特殊目錄）
find "$DOCS_DIR" -name "*.md" \
    -not -path "*/archive/*" \
    -not -path "*/architecture-decisions/*" \
    -not -path "*/technical-specs/*" \
    -not -path "*/.templates/*" | while read -r file; do

    filename=$(basename "$file")

    # 檢查 1: 格式規範
    if [[ ! "$filename" =~ $PATTERN ]]; then
        echo "❌ 格式錯誤: $filename (應為 XX-YY[-ZZ]_Name.md)" >> "$OUTPUT_FILE"
        ((ERROR_COUNT++))
        continue
    fi

    # 檢查 2: 編號深度（最大 3 層）
    number_part=$(echo "$filename" | sed 's/_.*//g')
    depth=$(echo "$number_part" | grep -o '-' | wc -l)
    if [ $depth -gt 2 ]; then
        echo "❌ 深度超標: $filename (編號深度=$((depth+1))，最大允許3層)" >> "$OUTPUT_FILE"
        ((ERROR_COUNT++))
    fi

    # 檢查 3: 重複編號
    duplicate_count=$(find "$DOCS_DIR" -name "${number_part}_*.md" \
        -not -path "*/archive/*" | wc -l)
    if [ $duplicate_count -gt 1 ]; then
        echo "⚠️ 編號重複: $number_part (使用 $duplicate_count 次)" >> "$OUTPUT_FILE"
        ((ERROR_COUNT++))
    fi
done

echo "" >> "$OUTPUT_FILE"
echo "總計問題數量: $ERROR_COUNT" >> "$OUTPUT_FILE"

if [ $ERROR_COUNT -eq 0 ]; then
    echo "✅ 編號規範驗證通過"
else
    echo "❌ 發現 $ERROR_COUNT 個編號問題，詳見報告: $OUTPUT_FILE"
    exit 1
fi
```

**使用方式**:
```bash
# 基準檢查
./scripts/validate-file-numbering.sh baseline-numbering.txt

# Phase 驗證（期望 0 errors）
./scripts/validate-file-numbering.sh phase5-numbering.txt
```

---

### A.3 update-links.sh（鏈接更新工具）

**功能**: 根據 link-mapping.json 自動更新所有內部鏈接

**完整實作**:
```bash
#!/bin/bash
# scripts/update-links.sh
# 用途：根據映射表自動更新 Markdown 文件中的鏈接

set -e

DOCS_DIR="docs/iGaming"
MAPPING_FILE="link-mapping.json"
DRY_RUN=false

# 解析參數
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --module)
            MODULE="$2"
            shift 2
            ;;
        *)
            echo "未知參數: $1"
            exit 1
            ;;
    esac
done

if [ ! -f "$MAPPING_FILE" ]; then
    echo "❌ 映射文件不存在: $MAPPING_FILE"
    exit 1
fi

if [ "$DRY_RUN" = true ]; then
    echo "🔍 Dry Run 模式（不會實際修改文件）"
fi

# 讀取映射表（JSON 格式）
# {
#   "oldPath": "01_Core_Financial_Loop/01-02_Wallet_Architecture.md",
#   "newPath": "02_Finance_Center/02-06_Unified_Wallet_Model.md",
#   "action": "merge"
# }

UPDATE_COUNT=0

# 使用 jq 解析 JSON
cat "$MAPPING_FILE" | jq -c '.[]' | while read -r mapping; do
    old_path=$(echo "$mapping" | jq -r '.oldPath')
    new_path=$(echo "$mapping" | jq -r '.newPath')
    action=$(echo "$mapping" | jq -r '.action')

    echo "處理映射: $old_path → $new_path (動作: $action)"

    # 查找所有引用舊路徑的文件
    find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" | while read -r file; do
        if grep -q "$old_path" "$file"; then
            if [ "$DRY_RUN" = true ]; then
                echo "  [DRY RUN] 將更新: $file"
            else
                # 實際替換
                sed -i "s|$old_path|$new_path|g" "$file"
                echo "  ✅ 已更新: $file"
                ((UPDATE_COUNT++))
            fi
        fi
    done
done

echo ""
if [ "$DRY_RUN" = true ]; then
    echo "🔍 Dry Run 完成，預計更新 $UPDATE_COUNT 個文件"
else
    echo "✅ 鏈接更新完成，共更新 $UPDATE_COUNT 個文件"
fi
```

**link-mapping.json 格式範例**:
```json
[
  {
    "oldPath": "01_Core_Financial_Loop/01-02_Wallet_Architecture.md",
    "newPath": "02_Finance_Center/02-06_Unified_Wallet_Model.md",
    "action": "merge",
    "module": "02"
  },
  {
    "oldPath": "03_Player_Journey/03-01_Player_Lifecycle.md",
    "newPath": "01_Player_Center/01-01_Player_Lifecycle.md",
    "action": "merge",
    "module": "01"
  },
  {
    "oldPath": "風控系統架構.md",
    "newPath": "05_Risk_Control/05-01_Risk_System_Architecture.md",
    "action": "translate",
    "module": "05"
  }
]
```

**使用方式**:
```bash
# Dry Run（預覽模式）
./scripts/update-links.sh --dry-run

# 僅更新 Module 02
./scripts/update-links.sh --module 02 --dry-run
./scripts/update-links.sh --module 02

# 全部更新
./scripts/update-links.sh
```

---

### A.4 normalize-versions.sh（版本號統一工具）

**功能**: 批量更新文件頭部的版本號為指定版本

**完整實作**:
```bash
#!/bin/bash
# scripts/normalize-versions.sh
# 用途：統一文檔版本號

set -e

DOCS_DIR="docs/iGaming"
TARGET_VERSION="${1:-v4.0.0}"
DRY_RUN=false

if [[ "$2" == "--dry-run" ]]; then
    DRY_RUN=true
    echo "🔍 Dry Run 模式"
fi

UPDATE_COUNT=0

# 查找所有活躍文檔（排除 archive/）
find "$DOCS_DIR" -name "*.md" -not -path "*/archive/*" | while read -r file; do
    # 檢查是否包含版本號標記
    if grep -q "版本.*: v[0-9]\." "$file" || grep -q "**文檔版本**.*: v[0-9]\." "$file"; then
        if [ "$DRY_RUN" = true ]; then
            echo "[DRY RUN] 將更新: $file"
        else
            # 替換版本號
            sed -i "s/版本.*: v[0-9]\+\.[0-9]\+\.[0-9]\+/版本: $TARGET_VERSION/g" "$file"
            sed -i "s/\*\*文檔版本\*\*.*: v[0-9]\+\.[0-9]\+\.[0-9]\+/**文檔版本**: $TARGET_VERSION/g" "$file"
            echo "✅ 已更新: $file → $TARGET_VERSION"
            ((UPDATE_COUNT++))
        fi
    fi
done

echo ""
if [ "$DRY_RUN" = true ]; then
    echo "🔍 Dry Run 完成，預計更新 $UPDATE_COUNT 個文件"
else
    echo "✅ 版本號統一完成，共更新 $UPDATE_COUNT 個文件 → $TARGET_VERSION"
fi
```

**使用方式**:
```bash
# Dry Run
./scripts/normalize-versions.sh v4.0.0 --dry-run

# 實際執行
./scripts/normalize-versions.sh v4.0.0
```

---

## 附錄 B: Phase 執行詳細命令序列

### B.1 Phase 1: 基準建立（Day 1-2）

**Day 1 上午：基準掃描**

```bash
# 1. 創建工作分支
cd c:/Workspace/open_source/smart-admin
git checkout master
git pull origin master
git checkout -b feature/igaming-docs-restructure

# 2. 創建 scripts 目錄
mkdir -p scripts

# 3. 生成基準報告
./scripts/scan-broken-links.sh baseline-broken-links.txt
./scripts/validate-file-numbering.sh baseline-numbering-issues.txt

# 4. 檢視基準數據
echo "基準斷鏈數量: $(grep '總計斷鏈數量' baseline-broken-links.txt)"
echo "基準編號問題: $(grep '總計問題數量' baseline-numbering-issues.txt)"
```

**Day 1 下午：生成映射表**

```bash
# 1. 手動創建 link-mapping.json（根據模組映射表）
cat > link-mapping.json << 'EOF'
[
  {
    "oldPath": "01_Core_Financial_Loop/01-02_Wallet_Architecture.md",
    "newPath": "02_Finance_Center/02-06_Unified_Wallet_Model.md",
    "action": "merge",
    "module": "02"
  },
  {
    "oldPath": "03_Player_Journey/03-01_Player_Lifecycle.md",
    "newPath": "01_Player_Center/01-01_Player_Lifecycle.md",
    "action": "merge",
    "module": "01"
  },
  ... (共 60 個映射條目)
]
EOF

# 2. 驗證 JSON 格式
cat link-mapping.json | jq '.' > /dev/null && echo "✅ JSON 格式正確"
```

**Day 2：工具開發與測試**

```bash
# 1. 開發腳本（參考附錄 A）
# 已在附錄 A 提供完整實作

# 2. 賦予執行權限
chmod +x scripts/*.sh

# 3. 測試 Dry Run
./scripts/update-links.sh --dry-run
./scripts/normalize-versions.sh v4.0.0 --dry-run

# 4. 提交基準數據
git add baseline-broken-links.txt baseline-numbering-issues.txt link-mapping.json scripts/
git commit -m "chore(docs): establish restructure baseline and tooling"
git push origin feature/igaming-docs-restructure
```

---

### B.2 Phase 2: Module 00 整合（Day 3）

**上午：內容遷移**

```bash
cd docs/iGaming

# 1. 創建新目錄結構
mkdir -p 00_Foundation/concepts
mkdir -p 00_Foundation/navigation
mkdir -p 00_Foundation/implementation-guides

# 2. 遷移 00_Concept_&_Analysis
git mv 00_Concept_&_Analysis/00-01_Solution_Overview.md \
       00_Foundation/concepts/00-01_Solution_Overview.md
git mv 00_Concept_&_Analysis/00-02_Tech_Stack.md \
       00_Foundation/concepts/00-02_Tech_Stack.md
git mv 00_Concept_&_Analysis/00-05_Data_Model.md \
       00_Foundation/concepts/00-03_Data_Model.md  # 重編號

# 3. 遷移 00_Navigation
git mv 00_Navigation/00-00_Document_Map.md \
       00_Foundation/navigation/00-04_Document_Map.md

# 4. 保留 implementation-guides（已在 00_Foundation 中）
# 無需移動

# 5. 刪除空目錄
rmdir 00_Concept_&_Analysis
rmdir 00_Navigation
```

**下午：鏈接更新與驗證**

```bash
# 1. 更新 00_Foundation/README.md
cat > 00_Foundation/README.md << 'EOF'
# 00 Foundation - 基礎概念與實作指南

## 概念篇（Concepts）
- [00-01 解決方案概覽](concepts/00-01_Solution_Overview.md)
- [00-02 技術棧](concepts/00-02_Tech_Stack.md)
- [00-03 數據模型](concepts/00-03_Data_Model.md)

## 導航（Navigation）
- [00-04 文檔地圖](navigation/00-04_Document_Map.md)

## 實作指南（Implementation Guides）
- [00-00-01 財務系統實作](implementation-guides/00-00-01_Financial_Implementation.md)
- [00-00-02 遊戲整合實作](implementation-guides/00-00-02_Game_Integration_Implementation.md)
...
EOF

git add 00_Foundation/README.md

# 2. 更新鏈接（僅 Module 00）
./scripts/update-links.sh --module 00

# 3. 驗證
./scripts/validate-file-numbering.sh phase2-numbering.txt
./scripts/scan-broken-links.sh phase2-broken-links.txt

# 期望結果：
# - 編號問題: -12 個（Module 00 衝突已解決）
# - 斷鏈數量: 不增加

# 4. 提交
git add .
git commit -m "refactor(docs): consolidate Module 00 into single Foundation

- Merge 00_Concept_&_Analysis → 00_Foundation/concepts/
- Merge 00_Navigation → 00_Foundation/navigation/
- Update all internal links
- Remove 12 numbering conflicts

Verified:
- ✅ Numbering: -12 conflicts
- ✅ Broken links: No new breaks"

git push origin feature/igaming-docs-restructure
```

---

### B.3 Phase 3: Module 01-03 整合（Day 4-5）

**Day 4：Player & Game 模組**

```bash
cd docs/iGaming

# 1. 合併 03_Player_Journey → 01_Player_Center

# 讀取詳細版 Player Lifecycle（用戶決策：保留詳細版）
# 由於文件 >25000 tokens，需要分段讀取和整合

# 暫時保留 03_Player_Journey/03-01，標記為 SSOT
cat > 01_Player_Center/01-01_Player_Lifecycle.md << 'EOF'
# 01-01 玩家生命週期 (Player Lifecycle)

⚠️ **SSOT 整合中**: 本文檔正在整合 03_Player_Journey/03-01 的詳細內容。

暫時參考：[03-01 玩家生命週期](../03_Player_Journey/03-01_Player_Lifecycle.md)

**整合進度**: Phase 3 (Day 4)
EOF

git add 01_Player_Center/01-01_Player_Lifecycle.md

# 2. 遷移其他 Player Journey 文件
git mv 03_Player_Journey/03-02_VIP_Loyalty.md \
       01_Player_Center/01-02_VIP_Loyalty.md
git mv 03_Player_Journey/03-03_Bonus_System.md \
       01_Player_Center/01-03_Bonus_System.md
git mv 03_Player_Journey/03-04_Agent_System.md \
       01_Player_Center/01-04_Agent_System.md

# 3. 合併 02_Game_Operations → 03_Game_Center
git mv 02_Game_Operations/02-01_Turnover_Calculation.md \
       03_Game_Center/03-04_Turnover_Calculation.md  # 重編號
git mv 02_Game_Operations/02-02_Game_Reporting.md \
       03_Game_Center/03-05_Game_Reporting.md

# 4. 刪除空目錄
rmdir 02_Game_Operations
# 保留 03_Player_Journey（待 Phase 3 完成後刪除）

# 5. 提交
git add .
git commit -m "refactor(docs): consolidate Player & Game modules (Day 4)

- Move 03_Player_Journey → 01_Player_Center (partial)
- Move 02_Game_Operations → 03_Game_Center
- Mark 01-01 as SSOT integration in progress

Verified:
- ✅ Numbering: -4 conflicts (01, 02, 03)
- ✅ Files moved: 6"

git push origin feature/igaming-docs-restructure
```

**Day 5：Finance 模組與 SSOT 建立**

```bash
cd docs/iGaming

# 1. 刪除 01_Core_Financial_Loop（v2.0 重組未完成）
# 先檢查內容是否已遷移
grep -r "01_Core_Financial_Loop" . --include="*.md" | grep -v "archive"
# 期望：僅有 README.md 提及，無實際引用

git rm -r 01_Core_Financial_Loop/
git commit -m "refactor(docs): remove incomplete 01_Core_Financial_Loop"

# 2. 建立 Wallet SSOT
# 確認 02_Finance_Center/02-06_Unified_Wallet_Model.md 為 SSOT

# 3. 創建 Redirect Stub（03_Game_Center/03-03）
cat > 03_Game_Center/03-03_Seamless_Wallet_Analysis.md << 'EOF'
# 03-03 無縫錢包分析 (Seamless Wallet Analysis)

⚠️ **已遷移**: 本文檔內容已整合到統一錢包模型。

請參考：
- **[02-06 統一錢包模型](../../02_Finance_Center/02-06_Unified_Wallet_Model.md)** - SSOT
- **[seamless-wallet 專題](../../02_Finance_Center/seamless-wallet/README.md)** - 詳細實作

**歸檔原因**: 避免內容重複，統一維護 SSOT

**歷史版本**: 查看 Git 歷史 `git log --follow 03-03_Seamless_Wallet_Analysis.md`
EOF

git add 03_Game_Center/03-03_Seamless_Wallet_Analysis.md

# 4. 更新鏈接
./scripts/update-links.sh --module 01
./scripts/update-links.sh --module 02
./scripts/update-links.sh --module 03

# 5. 驗證
./scripts/validate-file-numbering.sh phase3-numbering.txt
./scripts/scan-broken-links.sh phase3-broken-links.txt

# 6. 提交
git add .
git commit -m "refactor(docs): establish Wallet SSOT and complete Player/Game consolidation (Day 5)

- Delete 01_Core_Financial_Loop (incomplete v2.0 restructure)
- Establish 02-06 as Wallet SSOT
- Create redirect stub for 03-03
- Update all cross-references

Verified:
- ✅ Numbering: -7 conflicts total (Modules 01-03)
- ✅ SSOT: Wallet established
- ✅ Broken links: No new breaks"

git push origin feature/igaming-docs-restructure
```

---

### B.4 Phase 4: 中文文件整合（Day 6）

**上午：準備與翻譯**

```bash
cd docs/iGaming

# 1. 創建新的風控 SSOT 文件
cat > 05_Risk_Control/05-01_Risk_System_Architecture.md << 'EOF'
# 05-01 Risk System Architecture

**Document Version**: v4.0.0
**Last Updated**: 2026-02-05
**Maintenance Team**: Risk Team & Architecture Team

---

## 1. Strategic Positioning of Modern iGaming Risk Control

[翻譯中文版 §1...]

In the iGaming industry, expansion is often a double-edged sword. When a platform scales explosively from a 5-person startup to a thousand-person operation, the most lethal threat doesn't come from competitors—it stems from being overwhelmed by success ("Don't die by success"). During rapid scaling, early technical debt accumulates into manual processes that transform into "Lethal Friction," ultimately dragging down platform operations.

...

## 8. Asynchronous Risk Control Architecture: Kafka + Flink Stream Processing

[翻譯中文版 §8...]

The soul of risk control systems lies in the art of balance—"non-interference." As mentioned earlier, the system must perform surgical precision strikes against technical and regulatory threats without disrupting high-value player experiences. This requires an **asynchronous processing** architecture rather than synchronous blocking mode.

### 8.1 Why Asynchronous Risk Control?

**Fatal Flaws of Synchronous Risk Control**:
- ❌ **False Positive Problem**: Even a 95% accurate model will reject 5% of normal players
- ❌ **Single Point of Failure**: Risk engine failure causes all bets to fail
- ❌ **Latency Issues**: Complex rules (e.g., hedge detection) require historical data queries, response time >100ms
- ❌ **Non-compensable**: Once a bet is rejected, false positives cannot be corrected retroactively

...

[繼續翻譯所有章節，保留技術細節、Mermaid 圖表、變更日誌]

---

## Change Log

### v4.0.0 (2026-02-05)

**Integrated from Chinese v3.0.0**:
- Merged comprehensive risk architecture from 風控系統架構.md
- Added Kafka + Flink asynchronous processing design
- Consolidated with 04-01 Risk Framework content
- Unified terminology and technical specifications

...
EOF

# 2. 人工審核翻譯（需要 2-3 小時）
# 使用 AI 輔助翻譯，但必須人工審核技術準確性

# 3. 對比英文版（分段讀取）
# 由於 04-01 超大，需要使用 offset/limit 分段讀取
# （此步驟需要在實作階段使用 Read tool 完成）
```

**下午：整合與歸檔**

```bash
# 1. 合併風控模組
cd docs/iGaming

# 重編號 04_Risk_Control → 05_Risk_Control
git mv 04_Risk_Control 05_Risk_Control

# 合併 05_Risk_Management → 05_Risk_Control
git mv 05_Risk_Management/05-02_Agent_Credit_Risk.md \
       05_Risk_Control/05-03_Agent_Credit_Risk.md
git mv 05_Risk_Management/05-04_Risk_Proposal_Workflow.md \
       05_Risk_Control/05-04_Risk_Proposal_Workflow.md

rmdir 05_Risk_Management

# 2. 歸檔中文原文
mkdir -p archive/legacy-cn
git mv 風控系統架構.md archive/legacy-cn/風控系統架構_v3.0.0.md
git mv 風控系統處置.md archive/legacy-cn/風控系統處置_v3.0.0.md

# 3. 更新歸檔索引
cat >> archive/INDEX.md << 'EOF'

## v3.0.0 Legacy Chinese Documentation (2026-02-05)

| File | Status | Notes |
|------|--------|-------|
| [風控系統架構 v3.0.0](legacy-cn/風控系統架構_v3.0.0.md) | ✅ Archived | Integrated into 05-01 Risk System Architecture (English) |
| [風控系統處置 v3.0.0](legacy-cn/風控系統處置_v3.0.0.md) | ✅ Archived | Risk disposition procedures |

**Integration Method**: Manual translation + content merge
**SSOT**: [05_Risk_Control/05-01_Risk_System_Architecture.md](../iGaming/05_Risk_Control/05-01_Risk_System_Architecture.md)
EOF

git add archive/INDEX.md

# 4. 更新鏈接
./scripts/update-links.sh --module 05

# 5. 驗證
./scripts/validate-file-numbering.sh phase4-numbering.txt
./scripts/scan-broken-links.sh phase4-broken-links.txt

# 6. 提交
git add .
git commit -m "refactor(docs): integrate Chinese risk docs and consolidate risk modules (Phase 4)

- Translate 風控系統架構.md v3.0.0 → 05-01 Risk System Architecture
- Merge 04_Risk_Control → 05_Risk_Control
- Merge 05_Risk_Management → 05_Risk_Control
- Archive Chinese originals to legacy-cn/

Content:
- ✅ Kafka + Flink async architecture
- ✅ Three-layer disposition strategy
- ✅ Real-time monitoring vs blocking differentiation

Verified:
- ✅ Numbering: -2 conflicts (Modules 04-05)
- ✅ Chinese files archived
- ✅ Technical accuracy reviewed"

git push origin feature/igaming-docs-restructure
```

---

### B.5 Phase 5: 扁平化與編號標準化（Day 7-8）

**Day 7：Technical Infrastructure 重組**

```bash
cd docs/iGaming

# 1. 識別深度超標文件
find 07_Technical_Infrastructure -name "*.md" | while read -r file; do
    filename=$(basename "$file")
    depth=$(echo "$filename" | grep -o '-' | wc -l)
    if [ $depth -gt 2 ]; then
        echo "深度超標: $filename (深度=$((depth+1)))"
    fi
done

# 2. 重編號到 Module 09（避免衝突）
git mv 07_Technical_Infrastructure 09_Technical_Infrastructure

# 3. 扁平化深層文件
cd 09_Technical_Infrastructure

# 範例：5 層 → 3 層
git mv 07-03-02-01_OAuth_Refresh_Token_Implementation.md \
       09-12_OAuth_Refresh_Token_Implementation.md

git mv 07-03-02-02_Multi_Actor_Token_Security.md \
       09-13_Multi_Actor_Token_Security.md

# 解決重複編號 07-03-03
git mv 07-03-03_Common_Patterns.md \
       09-14_Common_Patterns.md

git mv 07-03-03_Token_Validation_Service.md \
       09-15_Token_Validation_Service.md

# 4. 批量重編號其他文件（保持邏輯順序）
# （此處省略 19 個文件的詳細命令，實作時需逐一處理）

cd ../..

# 5. Frontend CMS 扁平化
cd 08_Frontend_CMS

# 將 01-localization/ 扁平化到主目錄
git mv 01-localization/01-i18n.md 11-03_i18n.md  # 重編號到 Module 11
git mv 01-localization/02-dynamic-content.md 11-04_Dynamic_Content.md
git mv 01-localization/03-workflow.md 11-05_Workflow.md
git mv 01-localization/04-api.md 11-06_API.md
git mv 01-localization/README.md 11-03_Localization_README.md

rmdir 01-localization

cd ..

# 重編號 08_Frontend_CMS → 11_Frontend_CMS
git mv 08_Frontend_CMS 11_Frontend_CMS

# 6. 提交
git add .
git commit -m "refactor(docs): flatten deep nesting in Technical Infrastructure and Frontend CMS (Day 7)

Technical Infrastructure (07 → 09):
- Flatten 5-level numbering to 3-level max
- Resolve duplicate 07-03-03 (now 09-14, 09-15)
- Renumber all 19 files to 09-XX pattern

Frontend CMS (08 → 11):
- Flatten 01-localization/ subdirectory
- Move 5 files to main level with 11-XX numbering

Verified:
- ✅ Max depth: 3 levels
- ✅ Duplicate numbering: 0"

git push origin feature/igaming-docs-restructure
```

**Day 8：全域標準化與特殊目錄處理**

```bash
cd docs/iGaming

# 1. 按照模組映射表重編號所有剩餘模組
# Module 04 → 04 (Activity_Center，保持不變)
# Module 05 → 06 (Platform_Governance)
git mv 05_Platform_Governance 06_Platform_Governance

# Module 06 → 07 (Agent_Center)
git mv 06_Agent_Center 07_Agent_Center

# Module 06 → 10 (Analytics_Operations + Reporting_BI)
mkdir -p 10_Analytics_BI
git mv 06_Analytics_Operations/* 10_Analytics_BI/
git mv 10_Reporting_&_BI/* 10_Analytics_BI/
rmdir 06_Analytics_Operations
rmdir 10_Reporting_&_BI

# Module 07 → 08 (Platform_Management)
git mv 07_Platform_Management 08_Platform_Management

# Module 09 → 12 (System_Security)
git mv 09_System_Security 12_System_Security

# Module 11 → 13 (Customer_Service)
git mv 11_Customer_Service 13_Customer_Service

# Module 13 → 14 (Third_Party_Integration)
git mv 13_Third_Party_Integration 14_Third_Party_Integration

# 2. 刪除空目錄（用戶決策）
git rm -r 12_Technical_Operations/

# 3. Special Directories 標準化
cd architecture-decisions

# 統一 ADR 命名
git mv 012-async-risk-proposal-system.md \
       ADR-012-async-risk-proposal-system.md

cd ../technical-specs

# 整合到對應模組
git mv P1-important/07-withdrawal-risk-correlation.md \
       ../05_Risk_Control/05-05_Withdrawal_Risk_Correlation.md

rmdir P1-important
cd ..

# 4. 最終編號驗證
./scripts/validate-file-numbering.sh phase5-final-numbering.txt

# 期望結果：0 errors

# 5. 提交
git add .
git commit -m "refactor(docs): complete global numbering standardization (Day 8)

Module Renumbering:
- 05_Platform_Governance → 06
- 06_Agent_Center → 07
- 06_Analytics + 10_Reporting → 10 (merged)
- 07_Platform_Management → 08
- 09_System_Security → 12
- 11_Customer_Service → 13
- 13_Third_Party_Integration → 14

Special Directories:
- Delete 12_Technical_Operations (empty)
- Standardize architecture-decisions/ (ADR-XXX)
- Integrate technical-specs/ to modules

Final Structure:
- ✅ 14 unique modules (00-14, excl 12)
- ✅ 0 numbering conflicts
- ✅ Max depth: 3 levels
- ✅ 0 empty directories"

git push origin feature/igaming-docs-restructure
```

---

### B.6 Phase 6: 歸檔清理與最終驗證（Day 9）

**上午：歸檔與版本統一**

```bash
cd docs/iGaming

# 1. 更新歸檔索引
cat >> archive/INDEX.md << 'EOF'

## v4.0.0 Restructure (2026-02-05)

### Restructure Summary

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Module Directories | 24 | 14 | -42% |
| Numbering Conflicts | 8 | 0 | 100% elimination |
| Max Numbering Depth | 5 levels | 3 levels | -40% |
| SSOT Coverage | ~60% | 100% | +40% |
| Empty Directories | 1 | 0 | 100% cleanup |

### Major Changes

1. **Module Consolidation**:
   - Module 00: 3 directories → 1 (00_Foundation)
   - Module 01-03: 6 directories → 3
   - Module 04-05: 4 directories → 2 (Risk Control unified)
   - Module 06-07: 4 directories → 2 (Analytics merged)

2. **SSOT Establishment**:
   - Wallet Architecture: 02-06 (authoritative)
   - Player Lifecycle: 01-01 (detailed version)
   - Risk System: 05-01 (integrated from Chinese v3.0.0)

3. **Deep Nesting Elimination**:
   - Technical Infrastructure: 5-level → 3-level
   - Frontend CMS: flattened subdirectories
   - Duplicate numbering resolved (07-03-03 x2)

### Archived Content

- [風控系統架構 v3.0.0](legacy-cn/風控系統架構_v3.0.0.md)
- [風控系統處置 v3.0.0](legacy-cn/風控系統處置_v3.0.0.md)
- [Seamless Wallet v2.0.0](legacy/seamless-wallet-v2/)
- [v3.0.0 Migration Reports](v3.0.0/migration/)

### Execution Timeline

- Phase 1: Baseline establishment (Day 1-2)
- Phase 2: Module 00 consolidation (Day 3)
- Phase 3: Modules 01-03 integration (Day 4-5)
- Phase 4: Chinese docs integration (Day 6)
- Phase 5: Flattening and standardization (Day 7-8)
- Phase 6: Archive cleanup and final verification (Day 9)

**Total Duration**: 9 days
**Verification**: 6 phase checkpoints
**Rollback Safety**: Per-phase git commits

---

**Restructure Report**: [v4.0.0/RESTRUCTURE_REPORT.md](v4.0.0/RESTRUCTURE_REPORT.md)
**Migration Mapping**: [v4.0.0/link-mapping.json](v4.0.0/link-mapping.json)
EOF

git add archive/INDEX.md

# 2. 版本號統一
./scripts/normalize-versions.sh v4.0.0

# 驗證（期望：僅歸檔文件保留舊版本號）
grep -r "版本.*: v[123]\." docs/iGaming --include="*.md" -l

# 3. 提交
git add .
git commit -m "docs(igaming): update archive index and normalize versions to v4.0.0

- Archive index updated with restructure summary
- All active documents versioned to v4.0.0
- Legacy archives retain original versions

Verified:
- ✅ Active docs: v4.0.0
- ✅ Archived docs: original versions preserved"

git push origin feature/igaming-docs-restructure
```

**下午：最終驗證與報告生成**

```bash
# 1. 最終斷鏈檢查
./scripts/scan-broken-links.sh final-broken-links.txt

# 對比基準
echo "=== Broken Links Comparison ==="
echo "Baseline:"
grep '總計斷鏈數量' baseline-broken-links.txt
echo "Final:"
grep '總計斷鏈數量' final-broken-links.txt

# 期望：Final ≤ Baseline

# 2. 最終編號驗證
./scripts/validate-file-numbering.sh final-numbering-check.txt

# 期望輸出：
# ✅ 編號規範驗證通過
# 總計問題數量: 0

# 3. 版本號檢查
echo "=== Version Check ==="
echo "Legacy versions (should only be in archive/):"
grep -r "版本.*: v[123]\." docs/iGaming --include="*.md" -l

# 期望：僅 archive/ 目錄下的文件

# 4. 模組完整性檢查
echo "=== Module Integrity Check ==="
for i in $(seq -f "%02g" 0 14); do
    if [ "$i" == "12" ]; then
        continue  # 空目錄已刪除
    fi
    count=$(find docs/iGaming -maxdepth 1 -name "${i}_*" -type d | wc -l)
    if [ $count -eq 1 ]; then
        echo "✅ Module $i: OK"
    else
        echo "❌ Module $i: $count directories found (expected 1)"
    fi
done

# 期望：所有模組顯示 OK

# 5. 生成遷移報告
cat > docs/iGaming/archive/v4.0.0/RESTRUCTURE_REPORT.md << 'EOF'
# iGaming Documentation Restructure Report

**Version**: v4.0.0
**Execution Date**: 2026-02-05
**Duration**: 9 days (6 phases)
**Status**: ✅ Completed

---

## Executive Summary

The iGaming documentation restructure project successfully consolidated 24 directories into 14 unique modules, eliminated all numbering conflicts, flattened deep nesting from 5 levels to 3 levels, and established 100% SSOT coverage for critical topics.

### Key Achievements

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Module Directories | 24 | 14 | -42% |
| Numbering Conflicts | 8 modules | 0 | 100% elimination |
| Max Numbering Depth | 5 levels | 3 levels | -40% |
| SSOT Coverage | ~60% | 100% | +40% |
| Document Versions | v1.0-v3.0 mixed | v4.0.0 unified | 100% standardization |
| Broken Links | Baseline | ≤ Baseline | Maintained/improved |
| Empty Directories | 1 | 0 | 100% cleanup |

### Major Changes

[內容從附錄 B.6 上午部分複製]

---

## Verification Results

### Phase 1: Baseline (Day 1-2)
- ✅ Baseline broken links: [數字] links
- ✅ Baseline numbering issues: [數字] issues
- ✅ Tools developed and tested

### Phase 2: Module 00 (Day 3)
- ✅ 3 directories → 1 directory
- ✅ 12 numbering conflicts → 0
- ✅ All links updated

### Phase 3: Modules 01-03 (Day 4-5)
- ✅ 6 directories → 3 directories
- ✅ SSOT established (Wallet, Player Lifecycle)
- ✅ 01_Core_Financial_Loop removed

### Phase 4: Chinese Docs (Day 6)
- ✅ 風控系統架構.md translated and integrated
- ✅ Risk modules consolidated (04 + 05 → 05)
- ✅ Chinese originals archived

### Phase 5: Flattening (Day 7-8)
- ✅ Deep nesting eliminated (5 → 3 levels)
- ✅ Duplicate numbering resolved (07-03-03)
- ✅ Empty directory deleted (12_Technical_Operations)

### Phase 6: Final Verification (Day 9)
- ✅ Broken links: [Final數字] ≤ [Baseline數字]
- ✅ Numbering issues: 0
- ✅ Version unified: v4.0.0
- ✅ Archive index updated

---

## Files Moved/Modified

### Module Consolidation
| Old Path | New Path | Action |
|----------|----------|--------|
| 00_Concept_&_Analysis/* | 00_Foundation/concepts/ | Move |
| 00_Navigation/* | 00_Foundation/navigation/ | Move |
| 03_Player_Journey/* | 01_Player_Center/ | Move |
| 02_Game_Operations/* | 03_Game_Center/ | Move |
| ... | ... | ... |

[完整映射表參考 link-mapping.json]

### SSOT Establishment
| Topic | SSOT Location | Old Locations |
|-------|--------------|---------------|
| Wallet Architecture | 02-06_Unified_Wallet_Model.md | 01-02, 03-03 (redirect stub) |
| Player Lifecycle | 01-01_Player_Lifecycle.md | 03-01 (merged) |
| Risk System | 05-01_Risk_System_Architecture.md | 風控系統架構.md (translated), 04-01 (merged) |

---

## Rollback Procedure

If issues are discovered, rollback is possible at three levels:

### Level 1: Single Phase Rollback (Recommended)
```bash
git reset --hard feature/igaming-docs-restructure-phase[N]
git push --force origin feature/igaming-docs-restructure
```

### Level 2: Complete Rollback
```bash
git checkout master
git branch -D feature/igaming-docs-restructure
```

### Level 3: Emergency Recovery
```bash
git reflog
git reset --hard <commit-hash-before-restructure>
```

---

## Maintenance Guidelines

### Document Governance Rules (v4.0.0+)

1. **Numbering Allocation**: Check max number in module, use MAX + 1
2. **SSOT Check**: Verify no existing SSOT before adding content
3. **Version Marking**: New files start at v4.0.0
4. **Naming Convention**: Strictly follow `XX-YY[-ZZ]_File_Name.md`

### Pre-commit Hook

A pre-commit hook is installed to automatically validate:
- File numbering format (XX-YY[-ZZ]_Name.md)
- Numbering conflicts (duplicate XX-YY)
- Maximum depth (3 levels)

### Quarterly Audit

**Frequency**: Every quarter (Q1, Q2, Q3, Q4)

**Checklist**:
- [ ] Run `scan-broken-links.sh`, fix new breaks
- [ ] Run `validate-file-numbering.sh`, check compliance
- [ ] Verify SSOT markings still valid
- [ ] Check for new duplicate content
- [ ] Update archive index

---

## Lessons Learned

### What Went Well
- ✅ Phased approach allowed granular rollback safety
- ✅ Automated tools (80% coverage) reduced human error
- ✅ Git history preserved via `git mv`
- ✅ Baseline metrics provided clear success criteria

### Challenges
- ⚠️ Large file translation (風控系統架構.md 315 lines) required 2-3 hours
- ⚠️ Manual verification still needed for technical accuracy
- ⚠️ Link update complexity in deeply nested structures

### Recommendations for Future
- ✅ Maintain pre-commit hooks to prevent regression
- ✅ Conduct quarterly audits to catch issues early
- ✅ Document SSOT decisions clearly when adding new content
- ✅ Use automated link validation in CI/CD pipeline

---

**Report Generated**: 2026-02-05
**Generated By**: scripts/generate-migration-report.sh
**Contact**: Architecture Team
EOF

git add docs/iGaming/archive/v4.0.0/RESTRUCTURE_REPORT.md

# 6. 創建 Pull Request
git add .
git commit -m "docs(igaming): complete v4.0.0 restructure - final verification

Phase 6 Final Verification:
- ✅ Broken links: [數字] ≤ Baseline
- ✅ Numbering issues: 0
- ✅ Version unified: v4.0.0
- ✅ Module integrity: 14 unique modules

Deliverables:
- Archive index updated
- RESTRUCTURE_REPORT.md generated
- Pre-commit hooks installed
- Maintenance guidelines documented

Ready for merge to master."

git push origin feature/igaming-docs-restructure

# 7. 創建 GitHub Pull Request
echo "請在 GitHub 上創建 Pull Request:"
echo "從: feature/igaming-docs-restructure"
echo "到: master"
echo ""
echo "PR 標題: docs(igaming): v4.0.0 documentation restructure"
echo "PR 描述: 參考 docs/iGaming/archive/v4.0.0/RESTRUCTURE_REPORT.md"
```

---

## 下一步行動

1. ✅ 計畫已完成（含完整實作細節）
2. ⏳ 等待團隊審核與批准
3. ⏳ 批准後按照附錄 B 執行（Day 1-9）
4. ⏳ 創建 GitHub Issue 追蹤進度
5. ⏳ 通知所有文檔貢獻者（文檔凍結期）

**計畫版本**: 2.0.0（含實作規格）
**最後更新**: 2026-02-05
**計畫狀態**: ✅ 完成（含腳本實作與命令序列），待審批

