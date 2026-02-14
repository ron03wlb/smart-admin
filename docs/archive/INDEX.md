# SmartAdmin 文檔歸檔索引

**目的**：集中管理已完成、過時或替代的歷史文檔

**歸檔原則**：
- 已完成的階段性報告（審計、遷移等）
- 被新版本替代的技術文檔（Kafka v1 → v2）
- 已實施完成的計劃文檔
- 保留用於歷史追溯和參考

---

## 歸檔文檔清單

### 2026-01 架構審計報告
**路徑**：`archive/2026-01-audit/`
**狀態**：✅ 已完成並通過
**摘要**：v4.0.0 分支 refactor/atomic-foundation-migration 的架構合規性審計

| 文檔 | 版本 | 日期 | 說明 |
|------|------|------|------|
| ARCHITECTURE-AUDIT-REPORT.md | v1.0.0 | 2026-01-26 | 初始審計報告（發現測試同步問題） |
| ARCHITECTURE-AUDIT-REPORT-CORRECTED.md | v1.1.0 | 2026-01-27 | 修正版審計報告（修正誤判） |
| ARCHITECTURE-AUDIT-SUCCESS-REPORT.md | v2.0.0 | 2026-01-27 | 最終成功報告（14/14 測試通過） |

**關鍵成果**：
- ✅ ArchUnit 測試 14/14 全部通過
- ✅ 架構健康度評分 A+ (100/100)
- ✅ Foundation 遷移 100% 完成
- ✅ 測試同步問題已修復

**當前狀態**：架構規範由 ArchitectureTest.java 持續驗證

**相關文檔**：
- [.agent/rules/foundation/10-architecture-rules.md](../../.agent/rules/foundation/10-architecture-rules.md)
- [.agent/rules/ARCHITECTURE-RULES-CLARIFICATION.md](../../.agent/rules/ARCHITECTURE-RULES-CLARIFICATION.md)

---

### skills-migration-v3/ - Skills v3.0.0 遷移記錄

**歸檔日期**: 2026-01-30
**原因**: v3.0.0 階層式架構遷移完成

**內容**:
- `non-skill-items-v3/README.md` - 非技能項目清理記錄
- `root-layer-v3/README.md` - 根層級目錄重組記錄

**相關文檔**:
- [Skills README.md v3.0.0](../../.claude/skills/README.md)
- [VERSION.md v3.0.0](../../.claude/VERSION.md)

**查詢用途**: 了解 v3.0.0 目錄重組的歷史決策和遷移過程

---

### Legacy Kafka v1 文檔
**路徑**：`archive/legacy-kafka-v1/`
**狀態**：🗄️ 已替代
**替代版本**：（待填寫 Kafka v2 文檔路徑）
**歸檔原因**：Kafka 整合已升級，v1 文檔作為歷史參考保留

**文檔結構**：46 個文檔，包含架構、指南、範例、測試等
**歸檔日期**：2026-01-27

---

### Legacy 計劃文檔
**路徑**：`archive/legacy-planning/`
**狀態**：✅ 已實施完成
**歸檔原因**：階段性計劃已執行完成，保留用於歷史追溯

**包含文檔**：
- FEASIBILITY-ANALYSIS-CORRECTION.md
- gradle-nested-module-investigation.md
- IMPLEMENTATION_PROGRESS.md
- kafka-batch-requirements.md
- kafka-verification-framework.md
- PATH-A-FULL-NESTING-PLAN.md
- PHASE1_COMPLETE_SUMMARY.md
- sa-base-modularization-plan.md
- QUICK_START.md
- README.md
- index.md

**歸檔日期**：2026-01-27

---

### iGaming 文檔歸檔
**路徑**：`archive/iGaming/`
**狀態**：🗄️ 歷史版本
**歸檔原因**：v3.0.0 舊版文檔、中文舊版、已廢棄文檔

**目錄結構**：
- `v3.0.0/` - v3.0.0 版本完整備份
- `legacy-cn/` - 中文版舊文檔
- `deprecated/` - 已廢棄的設計文檔

**包含文檔**：14 個歸檔文件
**當前版本**：[docs/iGaming/](../iGaming/) (v4.0.0)
**歸檔日期**：2026-02-07

---

### Snail-Job 排程整合計劃
**路徑**：`archive/plans/job/`
**狀態**：✅ 已完成並發布
**版本**：v1.0.0 (2026-01-22)
**歸檔原因**：Snail-Job 整合已實施完成，所有8個文檔內容已完整

**包含文檔**：
- README.md - 計劃總覽與導航
- 01-quick-start.md - 快速開始指南
- 02-configuration.md - 配置說明
- 03-api-usage.md - API 使用指南
- 04-troubleshooting.md - 故障排除
- 05-architecture.md - 架構設計
- 06-monitoring.md - 監控方案
- 07-migration.md - 遷移指南
- 08-dag-workflows.md - DAG 工作流

**關鍵成果**：
- ✅ Snail-Job v1.6.0 整合完成
- ✅ Spring Boot 3.5.4 相容性驗證
- ✅ Redis/MySQL 雙重支援
- ✅ 管理後台整合完成
- ✅ 監控與告警機制建立

**當前狀態**：Snail-Job 已投入生產使用，計劃文檔歸檔以保持活躍計劃目錄整潔

**相關代碼**：
- [smartadmin-support-job](../../smart-admin-api-java21-springboot3/smartadmin-support/smartadmin-support-job/)

**歸檔日期**：2026-02-04

---

## 歸檔操作記錄

| 日期 | 操作 | 文件數 | 執行人 |
|------|------|--------|--------|
| 2026-01-27 | 創建 archive/ 目錄結構 | - | Claude Code |
| 2026-01-27 | 歸檔審計報告（3 個） | 3 | Claude Code |
| 2026-01-27 | 歸檔 Kafka v1 文檔 | 46 | Claude Code |
| 2026-01-27 | 歸檔舊計劃文檔 | 11 | Claude Code |
| 2026-01-27 | 刪除 docs/bak/ 目錄 | - | Claude Code |
| 2026-02-04 | 歸檔 Snail-Job 計劃（Week 9） | 8 | Claude Code |
| 2026-02-07 | 遷移 iGaming 歸檔至 archive/ | 14 | Claude Code |

---

## 訪問歸檔文檔

**瀏覽器訪問**：導航至對應子目錄
**命令行搜索**：
```bash
# 搜索歸檔文檔中的內容
grep -r "關鍵字" docs/archive/

# 查看特定審計報告
cat docs/archive/2026-01-audit/ARCHITECTURE-AUDIT-SUCCESS-REPORT.md
```

---

**維護責任**：SmartAdmin Documentation Team
**更新頻率**：每次歸檔操作後更新
**版本**：1.0.0
