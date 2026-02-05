# iGaming 文檔歸檔索引

最後更新：2026-02-04
版本：**v4.0.0（激進簡化）**

---

## 歸檔結構總覽

本目錄包含 iGaming 文檔的關鍵歷史資料，採用 **極簡 3 文件結構**：

```
archive/
├── INDEX.md                                           # 本索引文件
├── v3.0.0/
│   └── migration/
│       ├── V3.0.0_FINAL_SUMMARY.md                   # 完整歷史記錄（733 行）
│       ├── MIGRATION_COMPLETION_REPORT.md            # Week 3-4 遷移完成報告
│       ├── PHASE3_COMPLETION_REPORT.md               # Phase 3 完成報告
│       ├── WEEK_3-4_COMPLETION_REPORT.md             # Week 3-4 完成報告
│       ├── WEEK_8_COMPLETION_REPORT.md               # Week 8 完成報告
│       ├── MIGRATION_MAPPING.md                      # 遷移映射表
│       └── SSOT_MAPPING.md                           # SSOT 映射表
└── legacy/
    └── analysis/seamless_wallet.md                   # 原始深度分析（560 行）
```

| 文件 | 大小 | 說明 |
|------|------|------|
| **V3.0.0_FINAL_SUMMARY.md** | 733 行 | 包含所有 Phase 3-6 報告、審計報告、遷移指南的完整總結 |
| **MIGRATION_COMPLETION_REPORT.md** | - | Week 3-4 遷移完成報告（已歸檔） |
| **PHASE3_COMPLETION_REPORT.md** | - | Phase 3 完成報告（已歸檔） |
| **WEEK_3-4_COMPLETION_REPORT.md** | - | Week 3-4 完成報告（已歸檔） |
| **WEEK_8_COMPLETION_REPORT.md** | - | Week 8 自動化維護完成報告（已歸檔） |
| **MIGRATION_MAPPING.md** | - | 文件遷移路徑映射表（已歸檔） |
| **SSOT_MAPPING.md** | - | 單一真實來源映射表（已歸檔） |
| **seamless_wallet.md** | 560 行 | 原始無縫錢包深度分析（已拆分為 10 個專題，保留供參考） |

---

## 查詢指南

### 快速查找

| 需求 | 查找位置 |
|------|---------|
| 了解 v3.0.0 完整歷史 | `v3.0.0/migration/V3.0.0_FINAL_SUMMARY.md` |
| 查看原始無縫錢包分析 | `legacy/analysis/seamless_wallet.md` |
| 查看已整合的無縫錢包專題 | [../02_Finance_Center/seamless-wallet/](../02_Finance_Center/seamless-wallet/) |
| 查看當前業務文檔 | [../00_Foundation/concepts/00-00_Document_Map.md](../00_Foundation/concepts/00-00_Document_Map.md) |

### V3.0.0_FINAL_SUMMARY.md 包含內容

V3.0.0_FINAL_SUMMARY.md 是完整歷史記錄，包含：

1. **階段報告**（Phase 3-6）：
   - Phase 3：錢包與交易系統
   - Phase 4：活動與獎金系統
   - Phase 5：風控與安全系統
   - Phase 6：平台與運維系統

2. **審計報告**（4 個）：
   - 初始文檔審計
   - 質量審計報告
   - v4.0.0/v5.0.0 實作完成報告

3. **遷移指南**（8 個）：
   - 修正總結（Phase 1-3）
   - 遷移指南（v2→v3）
   - 執行摘要

### seamless_wallet.md 整合狀態

原始 seamless_wallet.md (1,100+ 行) 已拆分為 10 個專題模塊：

| 原始章節 | 整合位置 |
|---------|---------|
| 核心安全設計 | [02_Finance_Center/seamless-wallet/core/01-security.md](../02_Finance_Center/seamless-wallet/core/01-security.md) |
| 並發控制 | [02_Finance_Center/seamless-wallet/core/02-concurrency.md](../02_Finance_Center/seamless-wallet/core/02-concurrency.md) |
| 錯誤恢復 | [02_Finance_Center/seamless-wallet/core/03-recovery.md](../02_Finance_Center/seamless-wallet/core/03-recovery.md) |
| 遊戲邏輯 | [02_Finance_Center/seamless-wallet/game-logic/](../02_Finance_Center/seamless-wallet/game-logic/) |
| 財務對帳 | [02_Finance_Center/seamless-wallet/finance/reconciliation.md](../02_Finance_Center/seamless-wallet/finance/reconciliation.md) |

---

## Week 9 新增歸檔（2026-02-04）

本次歸檔包含 6 個根目錄完成報告，這些報告記錄了 v3.0.0 文檔重組的關鍵里程碑：

| 文件 | 內容說明 |
|------|---------|
| **MIGRATION_COMPLETION_REPORT.md** | Week 3-4 模塊遷移完成報告，記錄 24 個模塊 → 8 個模塊的整合過程 |
| **PHASE3_COMPLETION_REPORT.md** | Phase 3 階段完成報告，涵蓋錢包架構、流水計算、數據安全的合併工作 |
| **WEEK_3-4_COMPLETION_REPORT.md** | Week 3-4 週期總結，包含內容去重與 SSOT 驗證結果 |
| **WEEK_8_COMPLETION_REPORT.md** | Week 8 自動化維護機制完成報告，涵蓋 CI/CD workflows、驗證腳本、Pre-commit hooks |
| **MIGRATION_MAPPING.md** | 舊路徑 → 新路徑映射表，用於追溯文件遷移歷史 |
| **SSOT_MAPPING.md** | 單一真實來源（Single Source of Truth）概念映射表，記錄 10 個核心概念的權威定義位置 |

**歸檔原因**: 這些報告屬於已完成的階段性里程碑文檔，其內容已整合至當前模塊文檔中，移至歸檔以保持根目錄整潔。

**Git 歷史**: 所有文件使用 `git mv` 移動，完整保留提交歷史，可通過 `git log --follow` 追溯。

---

## Week 10-11 新增歸檔（2026-02-04）

本次歸檔包含 Week 10-11 Phase 2 完成報告，記錄目錄命名規範化的執行過程：

| 文件 | 內容說明 |
|------|---------|
| **WEEK_10-11_PHASE2_COMPLETION_REPORT.md** | Week 10-11 Phase 2 執行報告，記錄移除 7 個 `_NEW` 後綴目錄 + 解決 `04_Risk_Control` 雙目錄問題 |

**關鍵成果**:
- ✅ 7 個目錄後綴移除（00/01/02/03/05/06/07）
- ✅ 274 行 `_NEW` 引用更新
- ✅ 127 個文件變更（Task 2.1）
- ✅ 11 個文件變更（Task 2.2）
- ✅ 0 個斷鏈（2 個計劃文件標記為待開發）
- ✅ Git 歷史 100% 保留

**Git Checkpoints**:
- `archive-week10-before` - Phase 2 開始前備份
- `archive-week10-phase2-complete` - Phase 2 完成標記（Commit: `a9e58a3f`）

**歸檔原因**: Phase 2 為階段性里程碑任務，完成後移至歸檔以保持根目錄整潔。

---

## 歸檔原則

本目錄遵循以下歸檔原則：

1. **完整性**：使用 `git mv` 移動文件，保留完整的 Git 歷史
2. **可追溯性**：關鍵歷史文件保留（V3.0.0_FINAL_SUMMARY.md），其他內容可通過 Git 歷史追溯
3. **極簡主義**：僅保留最關鍵的 2 個歷史文件（89% 文件減少率）

### 追溯 Git 歷史

所有已刪除文件可通過 Git 歷史追溯：

```bash
# 查看已刪除文件的歷史
git log --all --full-history -- "docs/iGaming/archive/v3.0.0/phase-reports/PHASE3_COMPLETION_REPORT.md"

# 恢復已刪除文件（如需要）
git checkout archive-phase3-before-cleanup -- \
  docs/iGaming/archive/v3.0.0/phase-reports/PHASE3_COMPLETION_REPORT.md
```

**Git Backup Tag**: `archive-phase3-before-cleanup`（2026-02-04 創建）

---

## 歸檔結構變更歷史

| 版本 | 日期 | 文件數 | 大小 | 說明 |
|------|------|--------|------|------|
| **v4.0.0** | 2026-02-04 | **3 個** | **~80 KB** | 激進簡化：35 → 3 文件（-89%），深度降低 80% |
| v3.0.0 | 2026-02-03 | 35 個 | 748 KB | 文檔重構：2 層結構（v3.0.0/ + legacy/） |
| v2.0.0 | 2026-02-02 | 46 個 | ~900 KB | v4.0.0 溫和整理：reports/ 和 technical-reports/ 分類 |
| v1.0.0 | 2026-01-31 | 20 個 | ~500 KB | 初始版本：analysis/、audit-reports/、corrections/ |

### v4.0.0 激進簡化亮點

- ✅ **文件數減少 89%**：35 個 → 3 個
- ✅ **存儲減少 89%**：748 KB → 80 KB
- ✅ **深度降低 80%**：5 層 → 1 層（扁平化）
- ✅ **完整歷史保留**：所有內容已合併至 V3.0.0_FINAL_SUMMARY.md，或可通過 Git 歷史追溯

**清理原則**：
- 所有階段報告（4 個）→ 合併至 FINAL_SUMMARY
- 所有審計報告（4 個）→ 合併至 FINAL_SUMMARY
- 所有遷移指南（8 個，除 FINAL_SUMMARY）→ 合併至 FINAL_SUMMARY
- 技術修復報告（8 個）→ 刪除（一次性修復，Git 歷史可追溯）
- 活動中心歷史（2 個）→ 刪除（已整合至 04-01_Activity_System_Design.md）
- 深度分析（7 個，除 seamless_wallet.md）→ 刪除（已整合至模塊文檔）

---

## 相關資源

- **當前業務文檔**：[../00_Foundation/concepts/00-00_Document_Map.md](../00_Foundation/concepts/00-00_Document_Map.md)
- **Seamless Wallet 專題**：[../02_Finance_Center/seamless-wallet/](../02_Finance_Center/seamless-wallet/)
- **架構決策記錄**：[../architecture-decisions/](../architecture-decisions/)
- **SmartAdmin 開發指南**：[../../../CLAUDE.md](../../../CLAUDE.md)
- **重組計劃**：[../../../.claude/plans/mutable-kindling-dijkstra.md](../../../.claude/plans/mutable-kindling-dijkstra.md)

---

**維護狀態**：只讀（僅用於歷史參考）
**歸檔原因**：iGame 文檔模塊化重組（v2.0.0 → v3.0.0 → v4.0.0 激進簡化）
**下次審閱**：2026-05-04（每季度審閱，評估是否需要進一步簡化）
