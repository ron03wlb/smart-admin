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

### 2026-03 Sprint 歷史記錄
**路徑**：`archive/2026-03-sprint-history/`
**狀態**：✅ 已歸檔
**歸檔原因**：Sprint 3-4 階段性進度文件歸檔，保持根目錄整潔

**時間範圍**：2026-03-10 至 2026-03-25

**文件統計**：
- **Sprint 3**: 4 個文件（PR 描述、進度、發現、任務計劃）
- **Sprint 4 進度**: 5 個文件（各模塊階段性進度）
- **React 遷移**: 10 個文件（進度、發現、測試、任務計劃）
- **總計**: 19 個進度/發現/任務文件（約 7,257 行）

**子目錄結構**：
- `sprint-3/` - Sprint 3 完成總結
  - PR_DESCRIPTION.md, progress_sprint4.md, findings_sprint4.md, task_plan_sprint3.md
- `sprint-4-progress/` - Sprint 4 各階段進度
  - progress_update_2026-03-15.md, progress_job_module_2026-03-16.md, progress_home_module_2026-03-17.md, progress_typescript_fixes_2026-03-18.md, progress_vue_to_react_2026-03-18.md
- `react-migration/` - Vue→React 遷移專案
  - progress.md, findings.md, findings_react_2026-03-18.md, REACT_MIGRATION_FINAL_SUMMARY.md, M1-MILESTONE-CHECKLIST.md, react-test-findings.md, react-test-fix-plan.md, REACT-TEST-FIX-SUMMARY.md, task_plan_react_next.md, test_coverage_findings.md

**關鍵成果**：
- ✅ Sprint 3 完成：iGaming 基礎設施（代理佣金、VIP 升級、自我排除）
- ✅ Sprint 4 進行中：玩家治理模塊開發
- ✅ React 遷移 M1：核心框架與第一批模塊完成
- ✅ 測試覆蓋率：單元測試 75%+，整合測試核心流程 100%

**當前狀態**：Sprint 4 進行中，當前任務計劃保留於根目錄 `/task_plan.md`

**參考文檔**：
- [Sprint 3 README](2026-03-sprint-history/sprint-3/README.md)
- [Sprint 4 進度 README](2026-03-sprint-history/sprint-4-progress/README.md)
- [React 遷移 README](2026-03-sprint-history/react-migration/README.md)

**歸檔日期**：2026-03-27

---

### K6 效能測試 - 2026-03-11 有效投注額測試
**路徑**：`archive/performance-tests/2026-03-11-k6-turnover/`
**狀態**：✅ 測試完成，優化已實施
**歸檔原因**：效能測試完成，測試日誌和結果歸檔供未來參考

**測試日期**：2026-03-11
**測試目標**：驗證有效投注額（Valid Turnover）計算系統在高並發場景下的性能

**文件清單**：
- k6-quick-test.js (4.2K) - K6 測試腳本（可重用）
- k6-test-output.log (451B) - 初始基準測試
- k6-quick-test-output.log (5.4M) - 快速壓力測試
- k6-test-final-output.log (5.7M) - 最終完整測試
- k6-test-optimistic-lock-fix.log (67K) - 樂觀鎖修復驗證
- README.md - 測試背景與結果摘要

**關鍵指標**：
- **響應時間 (P95)**: 800ms → 180ms ✅
- **吞吐量**: 600 TPS → 1200 TPS ✅
- **錯誤率**: 3.2% → 0.05% ✅
- **數據庫連接池使用率**: 80% → 60% ✅

**優化措施**：
- Manager 層事務範圍優化（減少樂觀鎖衝突）
- 批次處理策略（批次大小：100）
- Redis 緩存熱點數據（TTL: 5 分鐘）
- 數據庫索引優化（複合索引 + 分區表）

**相關 Issues**：
- #123: High concurrency turnover calculation optimization
- #124: Optimistic lock conflict reduction

**當前狀態**：系統滿足生產環境性能需求

**參考文檔**：
- [K6 測試 README](performance-tests/2026-03-11-k6-turnover/README.md)

**歸檔日期**：2026-03-27

---

### LiteFlow 臨時 SQL 文件
**路徑**：`archive/migration/liteflow/`
**狀態**：🗄️ 歷史遷移文件
**歸檔原因**：v3→v4 遷移所需的臨時表結構，保留供歷史參考

**文件**：
- temp-liteflow-tables.sql (6.6K) - LiteFlow 表結構（v3→v4 遷移用）

**說明**：
此文件包含 v3→v4 遷移過程中所需的 LiteFlow 表結構定義，用於在 Flyway V2 遷移之前創建臨時表。遷移完成後保留作為歷史記錄。

**歸檔日期**：2026-03-27

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
| 2026-03-27 | 歸檔 Sprint 3-4 進度文件 | 19 | Claude Code |
| 2026-03-27 | 歸檔 React 遷移專案文件 | 10 | Claude Code |
| 2026-03-27 | 歸檔 K6 效能測試日誌 | 6 | Claude Code |
| 2026-03-27 | 歸檔 LiteFlow 臨時 SQL | 1 | Claude Code |

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
