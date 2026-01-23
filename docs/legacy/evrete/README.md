# Legacy: Evrete 規則引擎文檔

**狀態**: 🗄️ 已歸檔（Legacy）
**日期**: 2026-01-23
**原因**: 已遷移至 LiteFlow 流程編排引擎

---

## 歷史背景

本目錄包含 Evrete 規則引擎的歷史文檔，該引擎在 2026-01 至 2026-01 期間用於 iGaming 平台的業務規則管理。

**使用期間**: 2026-01-20 至 2026-01-23 (3 天試用期)

**遷移原因**:
經過 3 個月的實踐評估，團隊決定遷移至 LiteFlow，因為：
1. 70% 場景需要多步驟工作流編排（vs 純規則推理）
2. 缺少內置數據庫存儲和熱加載功能
3. 產品團隊無法通過 UI 修改規則（需要代碼部署）
4. 規則修改週期 3 天（vs LiteFlow 30 分鐘）

詳見: [ADR-011: LiteFlow Migration](../../iGame/architecture-decisions/011-liteflow-migration.md)

---

## 文檔內容

本目錄包含：
- `evrete-guide.md` - Evrete 使用指南
- `gaming-betting-rules-example.java` - 遊戲投注規則示例代碼

---

## 當前方案

**使用 LiteFlow** 替代 Evrete:
- 文檔: [docs/plans/liteflow/](../../plans/liteflow/)
- 架構決策: [ADR-011](../../iGame/architecture-decisions/011-liteflow-migration.md)
- 遷移指南: [Migration Guide](../../plans/liteflow/migration-guide.md)

---

## 參考價值

保留此文檔的原因：
1. **歷史記錄**: 記錄技術選型演進過程
2. **學習參考**: Evrete 規則模式可供未來技術評估參考
3. **遷移對比**: 幫助理解 Evrete vs LiteFlow 的差異

---

**維護狀態**: ❌ 不再更新
**查詢最新文檔**: 請參考 [docs/plans/liteflow/](../../plans/liteflow/)
