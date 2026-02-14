# iGaming 文檔重組基準報告 (Baseline Report)

**生成日期**: 2026-02-05
**目的**: Phase 2 基準建立，用於追蹤 Phase 3-7 改善進度

---

## 1. 編號檢查結果

| 指標 | 數值 |
|------|------|
| 總文件數 | 121 |
| 模組目錄數 | 23 |
| 編號違規 (Violations) | 16 |
| 警告 (Warnings) | 4 |

### 違規明細

**深度超過 3 層 (6 個)**:
- `09-11_OAuth_Refresh_Token_Implementation.md`
- `09-12_Multi_Actor_Token_Security.md`
- `ADR-012_Async_Risk_Proposal_System.md`
- `ADR-001_Naming_Convention_Singular_Standard.md`
- `07-withdrawal-risk-correlation.md`
- `08-turnover-validation-scheme.md`

**模組編號衝突 (10 組)**:
- `00`: Concept_&_Analysis / Foundation / Navigation
- `01`: Core_Financial_Loop / Player_Center
- `02`: Finance_Center / Game_Operations
- `03`: Game_Center / Player_Journey / Promotion_System
- `04`: Activity_Center / Risk_Control
- `05`: Platform_Governance / Risk_Management
- `06`: Agent_Center / Analytics_Operations
- `07`: Platform_Management / Technical_Infrastructure

**非 ASCII 文件名 (2 個)**:
- `風控系統架構.md`
- `風控系統處置.md`

---

## 2. 版本分佈

| 版本 | 文件數 | 佔比 |
|------|--------|------|
| v1.0.0 | 61 | 63.5% |
| v1.1.0 | 10 | 10.4% |
| v1.3.0 | 1 | 1.0% |
| v1.10.0 | 1 | 1.0% |
| v2.0.0 | 15 | 15.6% |
| v2.1.0 | 3 | 3.1% |
| v2.2.0 | 1 | 1.0% |
| v3.0.0 | 4 | 4.2% |
| **合計** | **96** | 8 個不同版本 |

**目標**: Phase 7 統一為 v4.0.0

---

## 3. 連結驗證

| 指標 | 數值 |
|------|------|
| 斷裂連結 | 386 |

**主要原因**:
- 跨模組引用（模組重組後路徑改變）
- 不存在的文件引用（如 `11-07_i18n_Localization.md`）
- 歸檔文件引用（已移動到 archive/）
- 外部文件引用（`.agent/`, `.claude/`）

---

## 4. 工具清單

| 腳本 | 版本 | 功能 | 狀態 |
|------|------|------|------|
| `check_file_numbering.sh` | v2.0.0 | 編號格式 + 深度 + 重複 + 衝突 | ✅ 增強完成 |
| `validate_links.sh` | v2.0.0 | 斷裂連結掃描 | ✅ macOS 兼容修復 |
| `detect_ssot_violations.sh` | v1.0.0 | SSOT 概念重複偵測 | ✅ 已有 |
| `update-links.sh` | v1.0.0 | 批量連結替換 | ✅ 新建 |
| `normalize-versions.sh` | v1.0.0 | 版本號掃描/統一 | ✅ 新建 |

---

## 5. 預期改善目標

| 指標 | 基準 | Phase 7 目標 |
|------|------|-------------|
| 模組編號衝突 | 10 組 | 0 |
| 編號深度違規 | 6 個 | 0 |
| 非 ASCII 文件名 | 2 個 | 0 |
| 版本不一致 | 8 個版本 | 1 個 (v4.0.0) |
| 斷裂連結 | 386 個 | < 10 |

---
