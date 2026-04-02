# ADR-017: Implementation 層範圍邊界——維持平層結構，不建立 Phase 子目錄

## 狀態

✅ **已接受** (Accepted) — 2026-03-31

---

## 背景 (Context)

`docs/iGaming/implementation/` 目錄目前包含 17 個文件，涵蓋 Phase 0-3 的基礎設施設計、Sprint 4 衝刺報告、缺口分析（gap analysis）、遷移驗證報告等多種類型。

隨著文件數量增加，有團隊成員提出建立 `phase0/`、`phase1/` 等子目錄的方案，以提升可瀏覽性。

---

## 決策 (Decision)

**維持 `implementation/` 的平層（flat）結構，不建立 phase 子目錄。**

文件依以下命名前綴隱式分類：

| 前綴 | 類別 | 範例 |
|------|------|------|
| `00-` | Phase 0 基礎設施 | `00-database-schema.md`、`00-module-structure.md` |
| `01-` | Phase 1 核心金融 | `01-wallet-design.md`、`01-payment-design.md` |
| `02-` | Phase 2 玩家與遊戲 | `02-player-design.md`、`02-game-integration.md` |
| `03-` | Phase 3 風控合規 | `03-risk-engine-design.md` |
| `sprint4-` / `phase2-week1-` | Sprint/Phase 施工報告 | `sprint4-database-schema-design.md` |
| `v2x-` / `V2x-` | 版本驗證報告 | `v24-migration-verification-report.md` |
| 無前綴 | 專項技術文件 | `database-boolean-migration.md`、`g2.3-frontend-adaptation-spec.md` |

**Sprint 報告與遷移驗證報告**（`sprint4-*.md`、`phase2-week1-*.md`、`v2x-*.md`）歸類為**歷史施工紀錄**：
- 豁免 Mermaid 圖表要求
- 豁免強制 XREF header 要求
- 在 `README.md` 中以獨立區段 "Phase 4+ Sprint Reports" 列出

---

## 理由 (Rationale)

1. **現有 17 個文件不足以因數量問題建立子目錄**（業界慣例：<30 個文件可維持平層）
2. **命名前綴已提供足夠的視覺分類**，目錄列表按字母順序自然分組
3. **避免相對路徑複雜化**：建立子目錄後所有文件的 XREF 路徑需要多一層 `../`，增加 broken link 風險
4. **arc42 實踐**：arc42 的 Chapter 11（Risks）建議 implementation notes 以單層目錄維護，便於 diff 和 code review

---

## 後果 (Consequences)

**正面影響**：
- 路徑穩定，不因重組而產生 broken links
- 新文件加入只需在 `README.md` 追加一行
- CI/CD 的 link validator 腳本無需修改

**負面影響**：
- 目錄超過 20+ 文件時可瀏覽性略降（屆時可重新評估此決策）

---

## 觸發重新評估的條件

若以下任一條件成立，應重新評估此 ADR：
- `implementation/` 文件數量超過 30 個
- 有 3 個以上的 Phase 5+ 設計文件加入

---

**文檔版本**: 1.0.0
**決策日期**: 2026-03-31
**維護團隊**: SmartAdmin Architecture Team

---

**Navigation**: [ADR Index](INDEX.md) | [iGaming Architecture](../) | [STANDARDS.md](../../STANDARDS.md)
