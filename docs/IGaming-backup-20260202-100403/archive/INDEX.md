# IGaming 文檔歸檔索引

## 歸檔原則

本目錄存放已完成的審計/實作報告、已整合到模塊文檔的分析報告，保留用於歷史追溯和參考。

所有歸檔文件的 Git 歷史完整保留，可使用 `git log --follow <file>` 追溯。

---

## 📋 審計報告 (Audit Reports)

| 文件 | 日期 | 描述 |
|------|------|------|
| [DOCUMENTATION_AUDIT_REPORT.md](./audit-reports/DOCUMENTATION_AUDIT_REPORT.md) | 2026-01-28 | 初始文檔審計報告 |
| [FINAL_DOCUMENTATION_REVIEW_REPORT.md](./audit-reports/FINAL_DOCUMENTATION_REVIEW_REPORT.md) | 2026-01-28 | 最終文檔審查報告 |
| [IMPLEMENTATION_COMPLETE_v4.0.0.md](./audit-reports/IMPLEMENTATION_COMPLETE_v4.0.0.md) | v4.0.0 | 實作完成報告 v4.0.0 |
| [IMPLEMENTATION_COMPLETE_v5.0.0.md](./audit-reports/IMPLEMENTATION_COMPLETE_v5.0.0.md) | v5.0.0 | 實作完成報告 v5.0.0 |

---

## 🔬 技術分析 (Technical Analysis)

| 文件 | 主題 | 整合狀態 |
|------|------|----------|
| [seamless_wallet.md](./analysis/seamless_wallet.md) | 無縫錢包原始分析 | ✅ 已拆分為 14 個專題（[02_Finance_Center/seamless-wallet/](../02_Finance_Center/seamless-wallet/)） |
| [lockAmount_betting_calculation_logic.md](./analysis/lockAmount_betting_calculation_logic.md) | 鎖定金額與投注計算 | ✅ 已整合至 02-06 統一錢包模型 |
| [turnover_calculation_logic.md](./analysis/turnover_calculation_logic.md) | 流水計算邏輯 | ✅ 已整合至 02-04 流水與對帳 |
| [LOGIC_ANALYSIS_REPORT.md](./analysis/LOGIC_ANALYSIS_REPORT.md) | 邏輯分析報告 | ✅ 已整合至相關模塊 |
| [LOGIC_ERROR_ANALYSIS_REPORT_v3.0.0.md](./analysis/LOGIC_ERROR_ANALYSIS_REPORT_v3.0.0.md) | 邏輯錯誤分析 v3.0.0 | ✅ 已修正並整合 |
| [LOGIC_ERROR_REVIEW_v6.0.0.md](./analysis/LOGIC_ERROR_REVIEW_v6.0.0.md) | 邏輯錯誤審查 v6.0.0 | ✅ 已修正並整合 |
| [P0_ERROR_ULTRATHINK_ANALYSIS.md](./analysis/P0_ERROR_ULTRATHINK_ANALYSIS.md) | P0 錯誤深度分析 | ✅ 已修正 |
| [P1_ERROR_ULTRATHINK_ANALYSIS.md](./analysis/P1_ERROR_ULTRATHINK_ANALYSIS.md) | P1 錯誤深度分析 | ✅ 已修正 |

---

## ✏️ 階段修正 (Phase Corrections)

| 文件 | 階段 | 描述 |
|------|------|------|
| [EXECUTIVE_SUMMARY_zh-TW.md](./corrections/EXECUTIVE_SUMMARY_zh-TW.md) | - | 執行摘要（繁體中文） |
| [PHASE1_CORRECTIONS_SUMMARY.md](./corrections/PHASE1_CORRECTIONS_SUMMARY.md) | Phase 1 | 第一階段修正總結 |
| [PHASE2_CORRECTIONS_SUMMARY.md](./corrections/PHASE2_CORRECTIONS_SUMMARY.md) | Phase 2 | 第二階段修正總結 |
| [PHASE3_CORRECTIONS_SUMMARY.md](./corrections/PHASE3_CORRECTIONS_SUMMARY.md) | Phase 3 | 第三階段修正總結 |

---

## 🔍 如何使用歸檔文件

### 追溯 Git 歷史

所有文件使用 `git mv` 移動，完整保留歷史：

```bash
# 查看歸檔文件的完整歷史
git log --follow docs/IGaming/archive/analysis/seamless_wallet.md

# 查看文件在某個時間點的內容
git show <commit-hash>:docs/IGaming/seamless_wallet.md
```

### 尋找已整合內容

如果您需要查找原始分析報告中的特定內容，請參考：

- **Seamless Wallet 專題**：[02_Finance_Center/seamless-wallet/00_INDEX.md](../02_Finance_Center/seamless-wallet/00_INDEX.md)
- **財務中心文檔**：[02_Finance_Center/](../02_Finance_Center/)
- **風控系統文檔**：[05_Risk_Management/](../05_Risk_Management/)

---

## 📚 相關文檔

- [遷移指南](../MIGRATION_GUIDE_v2.0.0.md) - 完整的舊→新路徑映射
- [文檔地圖](../00_Concept_&_Analysis/00-00_Document_Map.md) - IGaming 文檔導航中心
- [CLAUDE.md](../../../CLAUDE.md) - SmartAdmin 開發指南

---

**歸檔日期**：2026-01-30
**歸檔原因**：iGame 文檔模塊化重組 v2.0.0
**維護狀態**：只讀（僅用於歷史參考）
